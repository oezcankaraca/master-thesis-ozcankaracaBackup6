import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.apache.log4j.Level;

/**
 * The Peer class represents a node in a distributed network system, capable of
 * both client and server functionalities. Designed to work in environments with
 * varying network conditions, this class plays a pivotal role in testing and
 * evaluating
 * network performance, especially in containerized settings.
 *
 * The class leverages the Netty library for efficient network communication,
 * handling file transfers,
 * and maintaining connection states.
 * It manages critical metrics such as file transfer duration and connection
 * duration,
 * providing insights into network efficiency and reliability.
 *
 * Key functionalities include initiating connections to other peers, receiving
 * and
 * sending files, and dynamically adapting to the role of server or client based
 * on
 * environmental settings. This adaptability makes it suitable for complex
 * network
 * setups requiring robust testing and performance analysis.
 * 
 * @author Özcan Karaca
 */

public class Peer {
    private long fileTransferDuration;
    private long connectionDuration;

    // Static block for configuring logging settings.
    static {
        BasicConfigurator.configure();
        Logger.getRootLogger().setLevel(Level.ERROR);
    }

    private final int port;

    /**
     * Constructs a Peer instance with a specified port and super peer identifier.
     * It logs the construction of peer instances, particularly for specific ports
     * and super peer conditions.
     * 
     * @param port      The port number associated with this peer.
     * @param superPeer The identifier of the super peer.
     */
    public Peer(int port, String superPeer) {
        this.port = port;
        if (port == 7070 && superPeer.equals("lectureStudioServer")) {
            System.out.println("Info: Peer constructor called with lectureStudioServer and Port: " + port);
        } else if (port == 9090) {
            System.out.println("Info: Peer constructor called with " + superPeer + " and Port: " + port);
        }
    }

    /**
     * Sets the duration of a file transfer for this peer. This method is used to
     * track the time taken for file transfers, which is a critical metric in
     * evaluating network performance.
     * 
     * @param fileTransferDuration The duration of the file transfer in
     *                             milliseconds.
     */
    public void setFileTransferDuration(long fileTransferDuration) {
        this.fileTransferDuration = fileTransferDuration;
    }

    /**
     * Starts the Lecture Studio Server and attempts to establish a connection to a
     * super peer.
     * This method handles the initialization of network components, connection
     * attempts, and
     * file transfer handling. It also calculates and logs the duration of the
     * connection and file transfer.
     * 
     * @throws Exception If an error occurs during the server startup or connection
     *                   process.
     */
    public void startLectureStudioServer() throws Exception {
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        String superPeerIP = System.getenv("SUPER_PEER_IP_ADDRES");
        Peer self = this;
        long connectionStartTime = System.currentTimeMillis();

        try {
            Bootstrap b = new Bootstrap();
            b.group(workerGroup)
                    .channel(NioSocketChannel.class)
                    .handler(new ChannelInitializer<Channel>() {
                        @Override
                        protected void initChannel(Channel ch) {
                            ch.pipeline().addLast(
                                    new FileReceiverHandler("/app/receivedMydocumentFromLectureStudioServer.pdf",
                                            self));
                        }
                    })
                    .option(ChannelOption.SO_KEEPALIVE, true);

            int maxAttempts = 100000; // Maximum number of connection attempts
            int attempts = 0; // Current number of attempts
            boolean connected = false;

            while (!connected && attempts < maxAttempts) {
                try {
                    ChannelFuture f = b.connect(superPeerIP, port).sync();
                    f.channel().closeFuture().sync();
                    connected = true; // Connection successful
                } catch (Exception e) {
                    attempts++;
                    if (attempts < maxAttempts) {
                        Thread.sleep(3000); // Waiting time between attempts
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

                // Calculating and logging the connection duration
                this.connectionDuration = System.currentTimeMillis() - connectionStartTime - fileTransferDuration;
                System.out.println("Info: Connection successfully established to Port: " + port
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
                // Logging the failure to connect after maximum attempts
                System.out.println("Error: Connection could not be established after " + maxAttempts + " attempts");
            }
        } finally {
            // Properly shutting down the worker group
            workerGroup.shutdownGracefully();
        }
    }

    /**
     * Initiates the Super Peer setup and establishes a connection.
     * This method configures networking components and attempts to connect to the
     * specified super peer.
     * It handles file reception and logs connection and file transfer durations.
     * 
     * @param superPeerHost The hostname of the super peer.
     * @throws Exception If an error occurs during the startup or connection
     *                   process.
     */
    public void startSuperPeer(String superPeerHost) throws Exception {
        String superPeerIP = System.getenv("SUPER_PEER_IP_ADDRES");
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        Peer self = this;
        long connectionStartTime = System.currentTimeMillis();

        try {
            Bootstrap b = new Bootstrap();
            b.group(workerGroup)
                    .channel(NioSocketChannel.class)
                    .handler(new ChannelInitializer<Channel>() {
                        @Override
                        protected void initChannel(Channel ch) {
                            ch.pipeline().addLast(new FileReceiverHandler(
                                    "/app/receivedMydocumentFrom-" + superPeerHost + ".pdf", self));
                        }
                    })
                    .option(ChannelOption.SO_KEEPALIVE, true);

            int maxAttempts = 100000; // Maximum number of connection attempts
            int attempts = 0; // Current number of attempts
            boolean connected = false;

            while (!connected && attempts < maxAttempts) {
                try {
                    ChannelFuture f = b.connect(superPeerIP, port).sync();
                    f.channel().closeFuture().sync();
                    connected = true; // Connection successful
                } catch (Exception e) {
                    attempts++;
                    if (attempts < maxAttempts) {
                        Thread.sleep(3000); // Waiting time between attempts
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

                // Calculating and logging the connection duration
                this.connectionDuration = System.currentTimeMillis() - connectionStartTime - fileTransferDuration;
                System.out
                        .println("Info: Connection successfully established to Port: " + port + " and with super peer "
                                + superPeerHost + ", with IP Address: " + superPeerIP + " after "
                                + attempts + " attempts.");

                // Calculating total duration and logging results
                long totalDuration = connectionDuration + fileTransferDuration;
                Thread.sleep(2000);

                System.out.println("Info: Conection Time: " + connectionDuration + " ms");
                System.out.println("\nTotal Time (Connection + Transfer): " + totalDuration + " ms");

                // Send a confirmation message to the tracker peer
                sendConfirmationToTrackerPeer();

            } else {
                System.out.println("Error: Connection could not be established after " + maxAttempts + " attempts.");
            }
        } finally {
            // Properly shutting down the worker group
            workerGroup.shutdownGracefully();
        }
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
     * The main method that initializes the Peer instance and starts the connection
     * process.
     * It determines the role of the peer (either Lecture Studio Server or a super
     * peer) based on
     * environment variables and initiates the corresponding network connection.
     * 
     * @param args Command-line arguments (not used).
     * @throws Exception If an error occurs during execution.
     */
    public static void main(String[] args) throws Exception {
        System.out.println("\n*Main Method of a peer*\n");

        // Environment variable processing and initial setup
        String envNumberOfPeers = System.getenv("NUMBER_OF_TOTAL_PEERS");
        int numberOfPeers = Integer.parseInt(envNumberOfPeers);

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

        // Validation
        Thread.sleep(numberOfPeers * 30000);
        System.out.println("Info: Number Of Total Containers in the Testbed: " + numberOfPeers);

        int portLectureStudioServer = 7070;
        int portSuperPeer = 9090;
        String superPeerHost = System.getenv("SUPER_PEER");

        if (superPeerHost.equals("lectureStudioServer")) {
            System.out.println("Info: Super-peer of this peer is lectureStudioServer");
            new Peer(portLectureStudioServer, "lectureStudioServer").startLectureStudioServer();
        } else {

            System.out.println("Info: Super-peer of this peer is " + superPeerHost);
            new Peer(portSuperPeer, superPeerHost).startSuperPeer(superPeerHost);
        }

        Thread.sleep(5000000);
    }
}
