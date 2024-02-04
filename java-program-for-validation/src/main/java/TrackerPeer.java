import java.net.ServerSocket;
import java.net.Socket;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The TrackerPeer class acts as a server in a distributed system to track and confirm
 * peer connections. It listens for incoming connections on a specified port and expects
 * to receive confirmation messages from client peers. Upon receiving a confirmation,
 * it increments a counter. The class is designed to measure the time taken to receive
 * confirmations from all expected peers in the network.
 * 
 * It starts counting time from the moment the first confirmation is received until the last
 * expected confirmation, allowing for the calculation of the total duration needed for all
 * peers to successfully send their confirmation. 
 * The number of expected confirmations (peers) is defined by an environment variable,
 * allowing for dynamic adjustment based on the deployment setup.
 * 
 * @author Özcan Karaca
 */

public class TrackerPeer {

    // Port number on which the server listens
    private static final int PORT = 5050; 
    // Counter for received confirmations
    private static AtomicInteger confirmationsReceived = new AtomicInteger(0); 
    // Time when the first confirmation is received
    private static long startTime = 0; 

    public static void main(String[] args) throws IOException {
        String envNumberOfPeers = System.getenv("NUMBER_OF_TOTAL_PEERS");
        int numberOfPeers = Integer.parseInt(envNumberOfPeers);
        int expectedNumberOfConfirmations = numberOfPeers; // Expected number of confirmations

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Info: Tracker peer is listening on port " + PORT);

            // Continuously listen for incoming connections
            while (true) {
                try (Socket socket = serverSocket.accept(); // Accept incoming connections
                        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

                    String line = in.readLine(); // Read a line from the client
                    if ("CONFIRMATION".equals(line)) { // Check if the line is a confirmation message
                        if (confirmationsReceived.get() == 0) {
                            // Record start time at the first confirmation
                            startTime = System.currentTimeMillis(); 
                        }

                        int received = confirmationsReceived.incrementAndGet();
                        System.out.println("Info: Received confirmation: " + received);

                        // Check if all confirmations are received
                        if (received == expectedNumberOfConfirmations) {
                            long duration = System.currentTimeMillis() - startTime;
                            System.out.println("Info: All confirmations received\n");
                            System.out.println("Result: Total duration: " + duration + " ms");
                            break;
                        }
                    }
                } catch (IOException e) {
                    System.out.println("Error: Server exception: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }
        try {
            Thread.sleep(500000000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}