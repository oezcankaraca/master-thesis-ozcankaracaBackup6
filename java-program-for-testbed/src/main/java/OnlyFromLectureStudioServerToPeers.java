import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.GsonBuilder;

import java.io.FileWriter;
import java.io.IOException;

/**
 * Generates a JSON file representing connections from a single source (lectureStudioServer) 
 * to multiple peer nodes in a network simulation.
 * 
 * @author Özcan Karaca
 */
public class OnlyFromLectureStudioServerToPeers {

    private static int numberOfPeers = 10; // Default number of peers

    /**
     * Entry point of the program. Generates output data for connections.
     * 
     * @param args Command-line arguments, optionally specifying the number of peers.
     * @throws IOException If file operations fail.
     */
    public static void main(String[] args) throws IOException {
        System.out.println("The file is going to be sent from lectureStudio-server to all peers");
        
        // Parse number of peers from command-line if provided
        if (args.length > 0) {
            try {
                numberOfPeers = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.err.println("Error: Argument must be an integer. The default value of 10 is used.");
            }
        }
        
        // Get the user's home directory path
        String homeDirectory = System.getProperty("user.home");
        // Define the base path for the master thesis's directory
        String basePath = homeDirectory + "/Desktop/master-thesis-ozcankaraca";
        // Define the path for the output data with only lectureStudioServer
        String pathToJsonOutput = basePath + "/data-for-testbed/outputs-without-superpeer/output-data-" + numberOfPeers + ".json";

        // Prepare JSON data representing connections
        JsonArray peer2peerArray = new JsonArray();
        for (int i = 1; i <= numberOfPeers; i++) {
            JsonObject connection = new JsonObject();
            connection.addProperty("sourceName", "lectureStudioServer");
            connection.addProperty("targetName", String.valueOf(i));
            peer2peerArray.add(connection);
        }

        // Final JSON object
        JsonObject finalJson = new JsonObject();
        finalJson.add("peer2peer", peer2peerArray);
  
        // Write JSON data to file
        try (FileWriter file = new FileWriter(pathToJsonOutput)) {
            GsonBuilder gsonBuilder = new GsonBuilder();
            gsonBuilder.setPrettyPrinting();
            gsonBuilder.create().toJson(finalJson, file);
            System.out.println("\nSuccess: Peer information has been saved to the file: " + pathToJsonOutput);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}