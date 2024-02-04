import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.GsonBuilder;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/**
 * The GeneratorOfNetworkTopology class is responsible for creating a simulated network topology.
 * This class generates a JSON representation of a network topology, including various
 * network parameters and connections between peers.
 * It includes functionalities to collect and analyze data for each peer 
 * (like upload/download speeds, latency, packet loss),
 * establish connections between peers, and output the final network topology in a JSON format.
 * The class is designed to simulate a realistic network environment for testing purposes.
 *
 * Usage: This class is used in a process that involves collecting simulated network data for a specified
 * number of peers, including a special peer named lectureStudioServer.
 * It then generates a JSON file that describes the network topology,
 * including connections between these peers and their respective network parameters.
 * 
 * @author Özcan Karaca
 */
public class GeneratorOfNetworkTopology {
    private static int numberOfPeers = 10;
    // Create a JSON array to hold peer information
    static JsonArray peersArray = new JsonArray();

    /**
     * The main method to initiate the generation of network topology data.
     * It begins by collecting simulated network data for a specified number of peers, 
     * including lectureStudioServer.
     * Then, it proceeds to create a network topology in JSON format, detailing  onnections between peers.
     *
     * @param args Command line arguments, expects the number of peers as an optional argument.
     */
    public static void main(String[] args) {

        // Check if the number of peers is provided as an argument
        if (args.length > 0) {
            try {
                numberOfPeers = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.err.println("Error: Argument must be an integer. The default value of 10 is used.");
            }
        }

        System.out.println("\nStep Started: Collecting and analyzing real network data.\n");

        // Create an instance of DataGenerator to generate network data
        DataGenerator dataGenerator = new DataGenerator(numberOfPeers);
        // Generate network data for the specified number of peers
        List<DataGenerator.PeerStats> peerStatsList = dataGenerator.generateNetworkData();

        JsonObject lectureStudioServerObject = new JsonObject();
        Random random = new Random();

        int randomMaxUpload = 25000 + random.nextInt(30000 - 25000 + 1);
        int randomMaxDownload = 78000 + random.nextInt(80000 - 78000 + 1);

        lectureStudioServerObject.addProperty("name", "lectureStudioServer");
        lectureStudioServerObject.addProperty("maxUpload", randomMaxUpload);
        lectureStudioServerObject.addProperty("maxDownload", randomMaxDownload);

        DataGenerator.PeerStats lectureStudioServerStats = new DataGenerator.PeerStats(
                "lectureStudioServer",
                randomMaxUpload / 1000.0,
                randomMaxDownload / 1000.0,
                40.20,
                0.0024);

        peerStatsList.add(lectureStudioServerStats);

        // Add the special peer lectureStudioServer to the JSON array
        peersArray.add(lectureStudioServerObject);

        // Get the user's home directory path
        String homeDirectory = System.getProperty("user.home");
        // Define the base path for the master thesis's directory
        String basePath = homeDirectory + "/Desktop/master-thesis-ozcankaraca";
        // Specify the path to the network statistics for number of peers
        String pathToOutput = basePath + "/data-for-testbed/results/network-statistics/network-statistics-" + numberOfPeers + ".txt";

        // Initialize BufferedWriter for writing files
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(pathToOutput))) {
            System.out.println("Info: Generate simulated network data for " + numberOfPeers
                    + " peers and the lectureStudioServer.\n");

            // Print detailed information about the generated network data
            System.out.println("--Details of Collecting and Analyzing Real Network Data:--\n");
            for (DataGenerator.PeerStats stats : peerStatsList) {
                String dataRow = stats.toString();
                // Write each peer's data to the output file
                writer.write(dataRow + "\n");
                // Also print the data to the console
                System.out.println(dataRow);
            }
            System.out.println(
                    "\n---------------------------------------------------------------------------------------------------------------------------------------------\n");
            System.out.println("\nSuccess: Simulated network data was written to the file: " + pathToOutput);
        } catch (IOException e) {
            System.err.println("Unsuccess: Error while writing the file: " + e.getMessage());
        }

        System.out.println("\nStep Done: Collecting and analyzing real network data.\n");

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Define the path for the JSON file that will represent the network topology
        String pathToInputData = basePath + "/data-for-testbed/inputs/input-10/input-data-" + numberOfPeers + ".json";

        System.out.println("Step Started: Generating network topology.\n");

        // Generate a JSON object representing the input data for network topology
        JsonObject inputDataObject = generateInputDataObject(peerStatsList);

        // Notify the user about the generation of the JSON file
        System.out.println("Info: Generating input data JSON file.");

        // Call the method to write the generated JSON object to a file
        generateInputDataJsonFile(inputDataObject, pathToInputData);
    }

    /**
     * Generates a JSON object representing the input data for the network topology.
     * This method creates a JSON object with details of each peer, including the lectureStudioServer,
     * and their respective network parameters like maximum upload and download  speeds.
     *
     * @param peerStatsList A list of PeerStats objects containing network data for each peer.
     * @return A JsonObject representing the network topology's input data.
     */
    private static JsonObject generateInputDataObject(List<DataGenerator.PeerStats> peerStatsList) {
        System.out.println("Info: Creating input data JSON object.");

        // Create a new JSON object to hold the input data
        JsonObject inputDataObject = new JsonObject();
        // Add file properties to the JSON object (e.g., filename and filesize)
        inputDataObject.addProperty("filename", "test.pdf");
        inputDataObject.addProperty("filesize", 5000);

        // Add remaining peers to the JSON array
        for (DataGenerator.PeerStats stats : peerStatsList) {
            if (!stats.peerId.equals("lectureStudioServer")) {
                JsonObject peerObject = new JsonObject();
                peerObject.addProperty("name", stats.peerId);
                peerObject.addProperty("maxDownload", stats.maxDownload);
                peerObject.addProperty("maxUpload", stats.maxUpload);
                peersArray.add(peerObject);
            }
        }

        // Add the array of peers to the input data object
        inputDataObject.add("peers", peersArray);
        System.out.println("Info: Creating connections array.");

        // Create and add the connections array to the input data object
        JsonArray connectionsArray = createConnectionsArray(peerStatsList);
        inputDataObject.add("connections", connectionsArray);

        return inputDataObject;
    }

    /**
     * Creates a JSON array of connections between peers in the network.
     * Each connection object includes the source and target peer names, bandwidth, latency, and packet loss.
     * Connections are shuffled to randomize the network topology.
     *
     * @param peerStatsList A list of PeerStats objects containing network data for each peer.
     * @return A JsonArray of connection objects representing the network connections.
     */
    private static JsonArray createConnectionsArray(List<DataGenerator.PeerStats> peerStatsList) {
        System.out.println("Info: Generating connection data for peers.");

        // Initialize a new JSON array to store connection data
        JsonArray connectionsArray = new JsonArray();

        // Loop through each peer to create connection data
        for (int sourceIndex = 0; sourceIndex < peerStatsList.size(); sourceIndex++) {
            DataGenerator.PeerStats sourceStats = peerStatsList.get(sourceIndex);
            for (int targetIndex = 0; targetIndex < peerStatsList.size(); targetIndex++) {
                if (sourceIndex != targetIndex) {
                    DataGenerator.PeerStats targetStats = peerStatsList.get(targetIndex);

                    // Create a JSON object for each connection
                    JsonObject connection = new JsonObject();
                    connection.addProperty("sourceName", sourceStats.peerId);
                    connection.addProperty("targetName", targetStats.peerId);

                    // Calculate bandwidth, latency, and packet loss for the connection
                    int connectionBandwidthKbps = (int) (Math.min(sourceStats.maxUpload, targetStats.maxDownload));
                    double connectionLatency = sourceStats.latency + targetStats.latency;
                    double connectionLoss = Math.max(sourceStats.packetLoss, targetStats.packetLoss);

                    // Format and add connection properties to the JSON object
                    String formattedLatency = String.format(Locale.US, "%.2f", connectionLatency);
                    String formattedLoss = String.format(Locale.US, "%.4f", connectionLoss);

                    connection.addProperty("bandwidth", connectionBandwidthKbps);
                    connection.addProperty("latency", formattedLatency);
                    connection.addProperty("loss", formattedLoss);

                    // Add the connection object to the array
                    connectionsArray.add(connection);
                }
            }
        }

        // Notify about shuffling of connections to randomize the network topology
        System.out.println("Info: Shuffling connections array.");

        List<JsonElement> connectionList = new ArrayList<>();
        // Convert the JSON array to a list for shuffling
        for (JsonElement element : connectionsArray) {
            connectionList.add(element);
        }
        Collections.shuffle(connectionList);

        // Create a new JSON array for the shuffled connections
        JsonArray shuffledArray = new JsonArray();
        for (JsonElement connectionElement : connectionList) {
            shuffledArray.add(connectionElement);
        }

        return shuffledArray;
    }

    /**
     * Writes the generated network topology data to a JSON file.
     * This method saves the generated JSON object to a specified file path.
     *
     * @param jsonObject The JsonObject representing the network topology.
     * @param filePath   The file path where the JSON data will be saved.
     */
    private static void generateInputDataJsonFile(JsonObject jsonObject, String filePath) {
        System.out.println("Success: Writing input data to JSON file: " + filePath);

        try (FileWriter file = new FileWriter(filePath)) {

            // Create a GsonBuilder instance for pretty printing
            new GsonBuilder().setPrettyPrinting().create().toJson(jsonObject, file);

            System.out.println("Info: Input data JSON has been saved to the file: " + filePath);
            System.out.println("\nStep Done: Generating the network topology is done.\n");

        } catch (IOException e) {
            System.err.println("Error: Error encountered while writing to JSON file.");
            e.printStackTrace();
        }
    }
}