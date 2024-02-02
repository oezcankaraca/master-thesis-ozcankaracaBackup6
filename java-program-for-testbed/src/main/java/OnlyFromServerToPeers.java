import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.GsonBuilder;

import java.io.FileWriter;
import java.io.IOException;

public class OnlyFromServerToPeers {

    private static int numberOfPeers = 150;

    public static void main(String[] args) {

        System.out.println("The file is going to be sent from lectureStudio-server to all peers");
        // Check if the number of peers is provided as an argument
        if (args.length > 0) {
            try {
                numberOfPeers = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.err.println("Error: Argument must be an integer. The default value of 10 is used.");
            }
        }
        String homeDirectory = System.getProperty("user.home");
        String basePath = homeDirectory + "/Desktop/master-thesis-ozcankaraca";
        String pathToJsonOutput = basePath + "/data-for-testbed/outputs-without-superpeer/output-data-" + numberOfPeers + ".json";

        JsonArray peer2peerArray = new JsonArray();

        for (int i = 1; i <= numberOfPeers; i++) {
            JsonObject connection = new JsonObject();
            connection.addProperty("sourceName", "lectureStudioServer");
            connection.addProperty("targetName", String.valueOf(i));
            peer2peerArray.add(connection);
        }

        JsonObject finalJson = new JsonObject();
        finalJson.add("peer2peer", peer2peerArray);

        try (FileWriter file = new FileWriter(pathToJsonOutput)) {
            GsonBuilder gsonBuilder = new GsonBuilder();
            gsonBuilder.setPrettyPrinting();
            gsonBuilder.create().toJson(finalJson, file);
            System.out.println("\nInfo: Peer information has been saved to the file: " + pathToJsonOutput);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
