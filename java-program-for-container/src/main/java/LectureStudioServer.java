import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;

/**
 * 
 * The LectureStudioServer class is designed to act as a server in a distributed
 * system,
 * primarily for conducting network performance tests. This class is capable of
 * running server instances
 * on multiple IP addresses simultaneously, handling connections and file
 * transfers to multiple clients.
 * 
 * It uses the Netty framework for efficient network communication, setting up
 * server bootstrap and
 * channel handlers to manage incoming network traffic. The class is capable of
 * binding to different
 * IP addresses, specified during its initialization, and listens on a
 * designated port for incoming
 * connections. Key functionalities include starting server instances
 * onspecified IP addresses,
 * handling file sending operations, and logging critical network performance
 * metrics such as file send duration.
 * 
 * The server's operation is highly dependent on environment variables that
 * dictate the network topology
 * and parameters, making it adaptable for various testing scenarios. The
 * ability to extract IP addresses
 * from environment settings and to run multiple instances makes this class
 * suitable for complex network
 * environments where simultaneous connections from different network nodes are
 * required.
 * 
 * @author Özcan Karaca
 */
public class LectureStudioServer {

    private boolean hasSentFirstConfirmation = false;
    private final Object confirmationLock = new Object();

    // Static block for configuring logging settings.
    static {
        BasicConfigurator.configure();
        Logger.getRootLogger().setLevel(Level.ERROR);
    }
    private final int port;
    private final List<String> ipAddresses;

    private long fileTransferDuration;

    /**
     * 
     * - Constructs a LectureStudioServer instance with a specified port and a list
     * of
     * - IP addresses.
     * - This constructor initializes the server with necessary network parameters
     * and
     * - logs its creation.
     * -
     * - @param port The port number on which the server will listen.
     * - @param ipAddresses A list of IP addresses on which the server will operate.
     */
    public LectureStudioServer(int port, List<String> ipAddresses) {
        this.port = port;
        this.ipAddresses = ipAddresses;
        System.out.println("\nInfo: LectureStudioServer constructor called with Port: " + port);
    }

    /**
     * 
     * - Starts the server on all specified IP addresses.
     * - This method launches a new thread for each IP address to start individual
     * - server instances.
     * -
     * - @throws Exception If an error occurs during the server startup.
     */
    public void start() throws Exception {
        for (String ipAddress : ipAddresses) {
            new Thread(() -> {
                try {
                    startServerOnAddress(ipAddress);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();
        }
    }

    /**
     * 
     * - Starts a server instance on a specified IP address.
     * - This method sets up the server's network infrastructure and attempts to
     * bind
     * - it to the given IP address and port.
     * - It also logs the binding process and total duration of the server
     * operation.
     * -
     * - @param ipAddress The IP address to bind the server.
     * - @throws Exception If an error occurs during the server setup or binding.
     */
    private void startServerOnAddress(String ipAddress) throws Exception {
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        LectureStudioServer self = this;

        // Sending a confirmation message to the tracker peer
        synchronized (confirmationLock) {
            if (!hasSentFirstConfirmation) {
                sendConfirmationToTrackerPeer();
                hasSentFirstConfirmation = true;
            }
        }

        final int maxAttempts = 100000; // Maximale Anzahl der Versuche
        final long waitBetweenAttempts = 1000; // Wartezeit zwischen den Versuchen in Millisekunden
        boolean isBound = false;
        int attempt = 0;

        while (!isBound && attempt < maxAttempts) {
            try {
                ServerBootstrap b = new ServerBootstrap();
                b.group(bossGroup, workerGroup)
                        .channel(NioServerSocketChannel.class)
                        .childHandler(new ChannelInitializer<Channel>() {
                            @Override
                            protected void initChannel(Channel ch) {
                                // Adding a file sender handler to the pipeline.
                                ch.pipeline().addLast(new FileSenderHandler("/app/mydocument.pdf", self));
                            }
                        })
                        .option(ChannelOption.SO_BACKLOG, 128)
                        .childOption(ChannelOption.SO_KEEPALIVE, true);

                ChannelFuture f = b.bind(InetAddress.getByName(ipAddress), port).sync();
                f.channel().closeFuture().sync();
                isBound = true; // Wenn das Binden erfolgreich war
            } catch (Exception e) {
                System.err.println(
                        "Bind attempt " + (attempt + 1) + " failed for address " + ipAddress + ": " + e.getMessage());
                attempt++;
                if (attempt < maxAttempts) {
                    try {
                        Thread.sleep(waitBetweenAttempts);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new Exception("Interrupted while waiting to retry bind", ie);
                    }
                }
            } finally {
                if (!isBound) {
                    // Graceful shutdown in case of unsuccessful binding after all attempts
                    workerGroup.shutdownGracefully();
                    bossGroup.shutdownGracefully();
                }
            }
        }

        if (!isBound) {
            throw new Exception("Unable to bind to address " + ipAddress + " after " + maxAttempts + " attempts.");
        }

        // Wait for the completion of file transfer
        synchronized (this) {
            while (this.fileTransferDuration == 0) {
                this.wait();
            }
        }
    }

    /**
     * 
     * - Sends a confirmation message to tracker-peer after successful file
     * transfer.
     * - This method attempts to establish a socket connection to the server and
     * sends
     * - a predefined confirmation message.
     * - It retries the connection a specified number of times in case of failures.
     */
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
     * 
     * - Sets the duration of a file transfer for this peer. This method is used to
     * - track the time taken for file transfers, which is a critical metric in
     * - evaluating network performance.
     * -
     * - @param duration The duration of the file transfer in milliseconds.
     */
    public void setFileTransferDuration(long duration) {
        this.fileTransferDuration = duration;
    }

    /**
     * 
     * - Extracts the list of IP addresses from environment variables.
     * - This method searches the environment variables for entries starting with
     * - "CONNECTION_" and extracts the first IP address from each.
     * -
     * - @return A list of extracted IP addresses.
     */
    private static List<String> extractIPAddressesFromEnv() {
        List<String> ipAddresses = new ArrayList<>();
        Map<String, String> env = System.getenv();

        for (String envKey : env.keySet()) {
            if (envKey.startsWith("CONNECTION_")) {
                String connectionValue = env.get(envKey);
                String firstIPAddress = extractFirstIPAddress(connectionValue);
                if (firstIPAddress != null) {
                    ipAddresses.add(firstIPAddress);
                }
            }
        }
        return ipAddresses;
    }

    /**
     * 
     * - Extracts the first IP address from a connection information string.
     * -
     * - @param connectionInfo The connection information string.
     * - @return The extracted IP address or null if no IP address is found.
     */
    private static String extractFirstIPAddress(String connectionInfo) {
        String[] parts = connectionInfo.split(",");
        if (parts.length > 0) {
            String[] subParts = parts[0].split(":");
            return subParts[1];
        }
        return null;
    }

    /**
     * 
     * - The main method for the LectureStudioServer application.
     * - This method initializes the server with network parameters based on
     * - environment variables and starts the server.
     * -
     * - @param args Command-line arguments (not used).
     * - @throws Exception If an error occurs during execution.
     */
    public static void main(String[] args) throws Exception {
        System.out.println("\n*Main Method of lectureStudioServer*\n");

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
        Thread.sleep(numberOfPeers * 30000);
        System.out.println("Info: Number Of Total Containers in the Testbed: " + numberOfPeers);

        List<String> formattedPeers = new ArrayList<>();
        for (String peer : myPeers) {
            formattedPeers.add(prefixOfContainer + "-" + peer);
        }
        String joinedPeers = String.join(", ", formattedPeers);
        System.out.println("Info: The containers that will receive the data from lectureStudioServer:");
        System.out.println(joinedPeers);

        int port = 7070;
        List<String> ipAddresses = extractIPAddressesFromEnv();
        System.out.println("\nInfo: IP-Adressen from lectureStudioServer: " + ipAddresses);

        new LectureStudioServer(port, ipAddresses).start();

        Thread.sleep(5000000);
    }
}