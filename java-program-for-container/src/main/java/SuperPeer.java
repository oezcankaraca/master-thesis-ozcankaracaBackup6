import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

/**
 * The SuperPeer class represents a key component in a distributed network system,
 * providing both server and client functionalities. This class is responsible for
 * sending and receiving files among peers in a network. It implements both the  server side, 
 * which sends files to other peers, and the client side, which receives files from the lecutreStudio-server.
 *
 * Utilizing the Netty library for network operations, this class is configured to
 * listen on specific ports for server and client operations. It manages the network
 * connection duration and file transfer duration to measure and analyze network performance.
 * Additionally, it has mechanisms for managing connection status, including retrying
 * connection attempts and synchronizing file transfer processes.
 * 
 * @author Özcan Karaca
 */

public class SuperPeer {
    private long connectionDuration;
    private long fileTransferDuration;

    // Static block for configuring logging settings.
    static {
        BasicConfigurator.configure();
        Logger.getRootLogger().setLevel(Level.ERROR);
    }

    private final int serverPort;
    private final int clientPort;
    private final String filePathToSend;
    private final String filePathToReceive;
    private volatile boolean fileReceived = false;

    /**
     * Constructs a SuperPeer instance with specified server and client ports, as
     * well as file paths for sending and receiving files.
     * This constructor initializes the SuperPeer with necessary network parameters and logs its creation.
     * 
     * @param serverPort        The port number on which the server will run.
     * @param clientPort        The port number on which the client will connect.
     * @param filePathToSend    The file path of the file to be sent by this peer.
     * @param filePathToReceive The file path where received files will be stored.
     */
    public SuperPeer(int serverPort, int clientPort, String filePathToSend, String filePathToReceive) {
        this.serverPort = serverPort;
        this.clientPort = clientPort;
        this.filePathToSend = filePathToSend;
        this.filePathToReceive = filePathToReceive;

        System.out
                .println("\nInfo: SuperPeer created with Server Port: " + serverPort + " and Client Port: "
                        + clientPort);
    }

    /**
     * Sets the duration of the file transfer process for this peer.
     * This method is used to record the time taken to transfer a file, which is a
     * crucial metric for network performance.
     * 
     * @param duration The duration of the file transfer in milliseconds.
     */
    public void setFileTransferDuration(long duration) {
        this.fileTransferDuration = duration;
    }

    /**
     * Starts the server component of the SuperPeer.
     * This method waits for the file to be received before starting the server.
     * It sets up the network infrastructure to send files and waits for the completion of the file transfer.
     * 
     * @throws Exception If an error occurs during the server setup or operation.
     */
    public void startServer() throws Exception {
        int maxAttempts = 100000;
        int attempts = 0;
        SuperPeer self = this;

        // Wait for the file to be received or until the maximum attempts are reached
        while (!fileReceived && attempts < maxAttempts) {
            System.out.println("Info: Waiting for the file to be received. Attempt: " + (attempts + 1));
            Thread.sleep(1000); // Wait until the file is received
            attempts++;
        }

        if (fileReceived) {
            // Starting the server after successful file reception
            System.out.println("\nInfo: Starting server on Port: " + serverPort);

            EventLoopGroup bossGroup = new NioEventLoopGroup();
            EventLoopGroup workerGroup = new NioEventLoopGroup();

            try {
                ServerBootstrap b = new ServerBootstrap();
                b.group(bossGroup, workerGroup)
                        .channel(NioServerSocketChannel.class)
                        .childHandler(new ChannelInitializer<Channel>() {
                            @Override
                            protected void initChannel(Channel ch) {
                                // Adding a file sender handler to the pipeline.
                                ch.pipeline().addLast(new FileSenderHandler(filePathToSend, self));
                            }
                        })
                        .option(ChannelOption.SO_BACKLOG, 128)
                        .childOption(ChannelOption.SO_KEEPALIVE, true);

                ChannelFuture f = b.bind(serverPort).sync(); // Bind and start to accept incoming connections
                f.channel().closeFuture().sync();
            } finally {
                // Shutdown the server gracefully
                workerGroup.shutdownGracefully();
                bossGroup.shutdownGracefully();
            }

            // Wait for the completion of file transfer
            synchronized (this) {
                while (this.fileTransferDuration == 0) {
                    this.wait();
                }
            }
        } else {
            System.out.println("Error: File was not received after " + maxAttempts + " attempts. Server not started");
        }
    }

    /**
     * Initiates the client component of the SuperPeer to establish a network connection and receive files.
     * This method sets up the network client, attempts to connect to a specified
     * server, and handles file reception.
     * 
     * @throws Exception If an error occurs during the client setup or operation.
     */
    public void startClient() throws Exception {
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        String superPeerIP = System.getenv("SUPER_PEER_IP_ADDRES");
        SuperPeer self = this;
        long connectionStartTime = System.currentTimeMillis();

        try {
            Bootstrap b = new Bootstrap();
            b.group(workerGroup)
                    .channel(NioSocketChannel.class)
                    .handler(new ChannelInitializer<Channel>() {
                        @Override
                        protected void initChannel(Channel ch) {
                            // Adding a file receiver handler to the pipeline.
                            ch.pipeline().addLast(new FileReceiverHandler(filePathToReceive, self));
                        }
                    })
                    .option(ChannelOption.SO_KEEPALIVE, true);

            int maxAttempts = 100000;
            int attempts = 0;
            boolean connected = false;

            // Attempting to establish a connection to the server
            while (!connected && attempts < maxAttempts) {
                try {
                    ChannelFuture f = b.connect(superPeerIP, clientPort).sync();
                    f.channel().closeFuture().sync();
                    connected = true;
                } catch (Exception e) {
                    attempts++;
                    if (attempts < maxAttempts) {
                        Thread.sleep(3000);
                    }
                }
            }

            if (connected) {
                // Waiting for file transfer to complete
                synchronized (this) {
                    while (this.fileTransferDuration == 0) {
                        this.wait();
                    }
                }

                // Calculating and logging connection duration
                this.connectionDuration = System.currentTimeMillis() - connectionStartTime - fileTransferDuration;
                System.out.println("Info: Connection successfully established to Port: " + clientPort
                        + " and with lectureStudioServer, with IP Address: " + superPeerIP + " after " + attempts
                        + " attempts");

                // Calculating total duration and logging results
                long totalDuration = connectionDuration + fileTransferDuration;
                Thread.sleep(2000);

                System.out.println("Info: Conection Time: " + connectionDuration + " ms");
                System.out.println("\nTotal Time (Connection + Transfer): " + totalDuration + " ms");

                // Send a confirmation message to the tracker peer
                sendConfirmationToTrackerPeer();

            } else {
                System.out.println("Error: Connection could not be established after "
                        + maxAttempts + " attempts");
            }
        } finally {
            // Gracefully shutting down the worker group
            workerGroup.shutdownGracefully();
        }
    }

    /**
     * Sets the flag indicating whether a file has been received by this SuperPeer.
     * 
     * @param received A boolean indicating if the file has been received.
     */
    public void setFileReceived(boolean received) {
        this.fileReceived = received;
    }

    private void sendConfirmationToTrackerPeer() {
        String trackerPeerHost = "172.100.100.11"; // Host address of the tracker peer
        int trackerPeerPort = 5050; // Port of the tracker peer
        int maxAttempts = 100000;
        int attempt = 0;
    
        while (attempt < maxAttempts) {
            try (Socket socket = new Socket(trackerPeerHost, trackerPeerPort);
                    OutputStream out = socket.getOutputStream()) {
    
                String confirmationMessage = "CONFIRMATION\n";
                out.write(confirmationMessage.getBytes());
                out.flush(); // Stellen Sie sicher, dass die Daten gesendet werden.
                System.out.println("Info: Confirmation sent to TrackerPeer");
                return;
            } catch (IOException e) {
                System.err.println("Error: Error connecting to TrackerPeer (Attempt " + (attempt + 1)
                        + "): " + e.getMessage());
                e.printStackTrace();
            }
    
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                System.err.println("Info: Waiting period interrupted: " + ie.getMessage());
                return;
            }
    
            attempt++;
        }
        // Log a message if connection could not be established after maximum attempts
        System.err.println("Error: Connection to TrackerPeer could not be established after " + maxAttempts
                + " attempts.");
    }
    
    

    /**
     * The main method for the SuperPeer application.
     * This method initializes the SuperPeer with network parameters, starts client and server threads,
     * and handles file transfer operations between peers.
     * 
     * @param args Command-line arguments (not used).
     * @throws Exception If an error occurs during execution.
     */
    public static void main(String[] args) throws Exception {
        System.out.println("\n*Main Method of a super-peer*\n");

        // Environment variable processing and initial setup
        String envNumberOfPeers = System.getenv("NUMBER_OF_TOTAL_PEERS");
        int numberOfPeers = Integer.parseInt(envNumberOfPeers);

        String peersEnvVar = System.getenv("TARGET_PEERS");
        List<String> myPeers = peersEnvVar != null ? Arrays.asList(peersEnvVar.split(",")) : new ArrayList<>();

        String prefixOfContainer = System.getenv("PREFIX_NAME_OF_CONTAINER");

        // Adding connection details into to conections
        switch (numberOfPeers) {
            case 6:
                Thread.sleep(50000);
                break;
            case 11:
                Thread.sleep(80000);
                break;
            case 21:
                Thread.sleep(150000);
                break;
            case 36:
                Thread.sleep(250000);
                break;
            case 51:
                Thread.sleep(350000);
                break;
            case 76:
                Thread.sleep(500000);
                break;
            case 101:
                Thread.sleep(800000);
                break;
            case 151:
                Thread.sleep(1200000);
                break;
            default:
                // Handle any other number of peers that doesn't match above cases
                break;
        }        
 
        // Validation of he Network Characteristics
        //Thread.sleep(numberOfPeers * 30000);
        System.out.println("Info: Number Of Total Containern in Testbed: " + numberOfPeers);

        int serverPort = 9090;
        int clientPort = 7070;

        String filePathToSend = "/app/receivedMydocumentFromLectureStudioServer.pdf";
        String filePathToReceive = "/app/receivedMydocumentFromLectureStudioServer.pdf";

        List<String> formattedPeers = new ArrayList<>();
        for (String peer : myPeers) {
            formattedPeers.add(prefixOfContainer + "-" + peer);
        }
        String joinedPeers = String.join(", ", formattedPeers);
        System.out.println("The containers that will receive the data from lectureStudioServer:");
        System.out.println(joinedPeers);

        SuperPeer superPeer = new SuperPeer(serverPort, clientPort, filePathToSend, filePathToReceive);

        Thread clientThread = new Thread(() -> {
            try {
                superPeer.startClient();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        clientThread.start();
        clientThread.join(); // Wait for the client thread to finish

        new Thread(() -> {
            try {
                superPeer.startServer();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();

        Thread.sleep(5000000);
    }
}
