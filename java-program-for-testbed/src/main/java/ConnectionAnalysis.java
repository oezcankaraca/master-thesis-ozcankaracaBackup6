import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * The ConnectionAnalysis class is designed to analyze network connection details in a distributed system.
 * It reads and processes network configuration data, evaluates the completeness
 * of the network mesh, and reports any missing connections.
 * This class is particularly useful in scenarios where ensuring full mesh connectivity between network nodes is critical.
 * 
 * @author Özcan Karaca
 */
public class ConnectionAnalysis {

    private static PrintStream fileOutput; // Output stream for writing analysis results to a file

    /**
     * Prints a message to both the console and a file.
     * 
     * @param message The message to be printed.
     */
    private static void printAndWrite(String message) {
        System.out.println(message);
        fileOutput.println(message);
    }

    /**
     * Checks for missing connections in a full mesh network configuration.
     * 
     * @param allConnections A set of all existing connections.
     * @param peerNames      A set of all peer names in the network.
     * @return A set of missing connections that are required for a full mesh.
     */
    private static Set<String> checkFullMesh(Set<String> allConnections, Set<String> peerNames) {
        // Create a set to store any missing connections
        Set<String> missingConnections = new HashSet<>();
        
        // Log the start of the check
        printAndWrite("Info: Checking for missing connections in the full mesh network.");
        
        // Check each possible connection for its presence in the set of all connections
        for (String source : peerNames) {
            for (String target : peerNames) {
                if (!source.equals(target)) {
                    String connectionKey = source + "-" + target;
                    if (!allConnections.contains(connectionKey)) {
                        // Add missing connections to the set
                        missingConnections.add(connectionKey);
                    }
                }
            }
        }
        return missingConnections;
    }

    // Default number of peers
    private static int numberOfPeers = 10;

    /**
     * Main method to start the connection analysis.
     * 
     * @param args Command-line arguments, which can include the number of peers.
     * @throws IOException If there is an error in reading the input data or writing to the output file.
     */
    public static void main(String[] args) throws IOException {

        // Read the number of peers from the environment variable or use the default value
        if (args.length > 0) {
            try {
                numberOfPeers = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.err.println("Error: Argument must be an integer. The default value of 10 is used.");
            }
        }

        System.out.println("Step: Analysing connection details\n");

        // Get the user's home directory path
        String homeDirectory = System.getProperty("user.home");
        // Define the base path for the master thesis's directory
        String basePath = homeDirectory + "/Desktop/master-thesis-ozcankaraca";
        // Define the path for the network topology
        String pathToInputData = basePath + "/data-for-testbed/inputs/input-data-" + numberOfPeers + ".json";
       
        fileOutput = new PrintStream(basePath + "/data-for-testbed/results/input-info/input-info-" + numberOfPeers + ".txt");

        // Read input data from JSON file
        printAndWrite("Info: Reading input data from: " + pathToInputData);
        String content = new String(Files.readAllBytes(Paths.get(pathToInputData)));
        JSONObject json = new JSONObject(content);

        // Parse JSON arrays containing peer and connection information
        JSONArray peers = json.getJSONArray("peers");
        JSONArray connections = json.getJSONArray("connections");

        // Maps to store bandwidth information for each peer
        Map<String, Long> uploadBandwidthMap = new HashMap<>();
        Map<String, Long> downloadBandwidthMap = new HashMap<>();

        // Process peer data to populate the bandwidth maps
        printAndWrite("Info: Processing peer data.");
        for (int i = 0; i < peers.length(); i++) {
            JSONObject peer = peers.getJSONObject(i);
            String name = peer.getString("name");
            long maxDownload = peer.getLong("maxDownload");
            long maxUpload = peer.getLong("maxUpload");
            downloadBandwidthMap.put(name, maxDownload);
            uploadBandwidthMap.put(name, maxUpload);
        }

        // Process connection data and checking for full mesh connectivity
        StringBuilder connectionDetails = new StringBuilder();
        Set<String> allConnections = new HashSet<>();
        Set<String> peerNames = new HashSet<>();

        // Log the start of connection data processing
        printAndWrite("Info: Processing connection data.\n");
        for (int i = 0; i < connections.length(); i++) {
            JSONObject connection = connections.getJSONObject(i);
            String source = connection.getString("sourceName");
            String target = connection.getString("targetName");
            double latency = connection.getDouble("latency");
            double loss = connection.getDouble("loss");

            // Add peer names to the set
            peerNames.add(source);
            peerNames.add(target);

            // Get upload and download bandwidth for the connection
            long sourceUploadBandwidth = uploadBandwidthMap.getOrDefault(source, 0L);
            long targetDownloadBandwidth = downloadBandwidthMap.getOrDefault(target, 0L);

            // Determine the bandwidth used in the connection
            long usedBandwidth = Math.min(sourceUploadBandwidth, targetDownloadBandwidth);

            // Format connection information string
            String connectionInfo = String.format(
                    "Source: %s (maxUpload: %d Kbps) - Target: %s (maxDownload: %d Kbps) - Bandwidth: %d Kbps - Latency: %.2f ms - Loss: %.2f%%\n",
                    source, sourceUploadBandwidth, target, targetDownloadBandwidth, usedBandwidth, latency, loss);

            // Append connection information to the details string
            connectionDetails.append(connectionInfo);
            // Add the connection to the set of all connections
            allConnections.add(source + "-" + target);
        }

        /** 
        // Print all connection details
        printAndWrite("--Connection Analysis--\n");
        printAndWrite(connectionDetails.toString());
        System.out.println(
                    "\n---------------------------------------------------------------------------------------------------------------------------------------------\n");
        */
        
        // Perform analysis to check mesh completeness
        printAndWrite("Info: Analyzing mesh completeness.");
        Set<String> missingConnections = checkFullMesh(allConnections, peerNames);

        // Print analysis results
        if (missingConnections.isEmpty()) {
            printAndWrite("Success: There is a full mesh among all peers including the lectureStudioServer.");
        } else {
            printAndWrite("Unsuccess: There is NOT a full mesh among all peers including the lectureStudioServer.");
            printAndWrite("Error: Missing connections:");
            missingConnections.forEach(connection -> printAndWrite(connection));
        }
        
        System.out.println("\nStep: Analysing connection details is done.\n"); 

        // Write analysis results to the file and close the stream
        fileOutput.close();
    }
}