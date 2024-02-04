import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.stream.Collectors;

/**
 * The NetworkConfigParser class is designed for parsing and processing network configuration data.
 * This class reads a JSON file that defines the network topology, including P2P connections and superpeers.
 * It provides functionalities to extract and organize various aspects of the
 * network configuration such as:
 * - The names of superpeers.
 * - P2P connection details.
 * - Mapping superpeers to their connected peers.
 * - Generating a list of distinct peer names, excluding superpeers and the lectureStudioServer.
 *
 * Usage: This class is used to parse network configuration data from a JSON file, facilitating the
 * understanding and analysis of the network topology. It is particularly useful in scenarios where
 * network configurations need to be dynamically read and interpreted for simulation or analysis purposes.
 * 
 * @author Özcan Karaca
 */
public class NetworkConfigParser {

    /**
     * Represents a single P2P connection in the network.
     * It stores the names of the source and target peers involved in the connection.
     */
    static class PeerConnection {
        private String targetName; // Name of the target peer
        private String sourceName; // Name of the source peer

        // Returns the name of the target peer.
        public String getTargetName() {
            return targetName;
        }

        // Returns the name of the source peer.
        public String getSourceName() {
            return sourceName;
        }

        // Default constructor for use with data binding libraries or for manual instantiation.
        public PeerConnection() {
        }

        // Constructs a PeerConnection with specified source and target peers.
        public PeerConnection(String sourceName, String targetName) {
            this.sourceName = sourceName;
            this.targetName = targetName;
        }
    }

    /**
     * Represents a superpeer in the network, which may act as a central node
     * for a subset of peers, facilitating efficient data distribution.
     */
    static class Superpeer {
        private String name; // Name of the superpeer

        // Returns the name of the superpeer.
        public String getName() {
            return name;
        }

        // Default constructor for use with data binding libraries or for manual instantiation.
        public Superpeer() {
        }

        // Constructs a Superpeer with a specified name.
        public Superpeer(String name) {
            this.name = name;
        }
    }

    /**
     * Encapsulates the entire network configuration, including peer-to-peer connections
     * and superpeers. This class provides a top-level view of the network's structure.
     */
    static class NetworkConfig {
        private List<PeerConnection> peer2peer; // List of peer-to-peer connections
        private List<Superpeer> superpeers; // List of superpeers in the network

        // Returns the list of peer-to-peer connections.
        public List<PeerConnection> getPeer2peer() {
            return peer2peer;
        }

        // Returns the list of superpeers.
        public List<Superpeer> getSuperpeers() {
            return superpeers;
        }
    }

    private NetworkConfig config;

    /**
     * Constructor for the NetworkConfigParser class.
     * Initializes the parser with the provided configuration file path.
     * It reads the JSON configuration file and loads it into the NetworkConfig object.
     *
     * @param configFilePath The path to the JSON file containing network configuration data.
     * @throws IOException If there is an issue reading the file.
     */
    public NetworkConfigParser(String configFilePath) throws IOException {
        System.out.println("Info: Initializing NetworkConfigParser with file: " + configFilePath);
        ObjectMapper mapper = new ObjectMapper();

        // Reads the network configuration from the given file path into a NetworkConfig
        // object
        config = mapper.readValue(new File(configFilePath), new TypeReference<NetworkConfig>() {
        });
        System.out.println("Success: Network configuration successfully loaded.");
    }

    /**
     * Retrieves the names of all superpeers defined in the network configuration.
     * If there are no superpeers, it returns an empty list.
     *
     * @return A list of names of superpeers, or an empty list if none exist.
     */
    public List<String> getSuperpeerNames() {
        System.out.println("Info: Extracting superpeer names.");

        // Check if the superpeers list is null or empty
        if (config.getSuperpeers() == null || config.getSuperpeers().isEmpty()) {
            System.out.println("Info: No superpeers found in the configuration.");
            return new ArrayList<>(); // Return an empty list
        }

        // Extracts and returns a list of names of superpeers from the network configuration
        return config.getSuperpeers().stream()
                .map(Superpeer::getName)
                .collect(Collectors.toList());
    }

    /**
     * Retrieves all P2P connections defined in the network configuration.
     * This method returns a list of PeerConnection objects, each representing a connection between two peers.
     *
     * @return A list of PeerConnection objects representing peer-to-peer
     *         connections.
     */
    public List<PeerConnection> getPeerConnections() {
        System.out.println("Info: Retrieving peer-to-peer connections.");

        // Returns a list of PeerConnection representing all P2P connections in the network configuration.
        return config.getPeer2peer();
    }

    /**
     * Generates a mapping of superpeers to their directly connected peers.
     * If there are no superpeers, it returns an empty map.
     *
     * @return A map where each key is a superpeer name and the value is a list of
     *         connected peers, or an empty map if there are no superpeers.
     */
    public Map<String, List<String>> getSuperpeerConnections() {
        System.out.println("Info: Mapping superpeers to their connected peers.");
        Map<String, List<String>> superpeerConnections = new HashMap<>();

        // Check if the superpeers list is null or empty
        if (config.getSuperpeers() == null || config.getSuperpeers().isEmpty()) {
            System.out.println("Info: No superpeers found in the configuration for mapping connections.");
            return superpeerConnections; // Return an empty map
        }

        // Maps each superpeer to a list of peers it is directly connected to
        for (Superpeer superpeer : config.getSuperpeers()) {
            List<String> connectedPeers = config.getPeer2peer().stream()
                    .filter(connection -> superpeer.getName().equals(connection.getSourceName()))
                    .map(PeerConnection::getTargetName)
                    .collect(Collectors.toList());
            superpeerConnections.put(superpeer.getName(), connectedPeers);
        }
        return superpeerConnections;
    }

    /**
     * Gathers a list of distinct peer names, excluding superpeers and the lectureStudioServer.
     * This method processes the configuration data to compile a list of unique peer
     * names that are not superpeers.
     *
     * @return A list of peer names excluding superpeers and the
     *         lectureStudioServer.
     */
    public List<String> getPeers() {
        System.out.println("Info: Gathering distinct peer names, excluding superpeers and the lectureStudioServer.");
        List<String> superpeerNames = getSuperpeerNames();

        // Compiles a list of unique peer names that are not superpeers or the lectureStudioServer
        return config.getPeer2peer().stream()
                .map(PeerConnection::getTargetName)
                .filter(peerName -> !superpeerNames.contains(peerName) && !"lectureStudioServer".equals(peerName))
                .distinct()
                .collect(Collectors.toList());
    }

    private static int numberOfPeers = 10;
    private static boolean useSuperPeers = true;

    /**
     * Reads network configuration data, and displays information about peers and connections.
     *
     * @param args Command-line arguments.
     * @throws IOException If there is an issue reading the network configuration file.
     */
    public static void main(String[] args) throws IOException {

        if (args.length > 0) {
            try {
                numberOfPeers = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.err.println("Error: Argument must be an integer. The default value of 10 is used.");
            }
        }

        if (args.length > 1) {
            useSuperPeers = Boolean.parseBoolean(args[1]);
        }

        // Get the user's home directory path
        String homeDirectory = System.getProperty("user.home");
        // Define the base path for the master thesis's directory
        String basePath = homeDirectory + "/Desktop/master-thesis-ozcankaraca";
        // Constructs the path to the output data file based on the number of peers
        String pathToOutputData;
        if (useSuperPeers) {
            pathToOutputData = basePath + "/data-for-testbed/outputs-with-superpeer/output-data-" + numberOfPeers
                    + ".json";
        } else {
            pathToOutputData = basePath + "/data-for-testbed/outputs-without-superpeer/output-data-" + numberOfPeers
                    + ".json";
        }

        System.out.println("\nStep: Integrating P2P algorithm\n");

        // Initializes the NetworkConfigParser with the path to the output data
        NetworkConfigParser parser = new NetworkConfigParser(pathToOutputData);

        System.out.println("Info: Processing superpeers.");
        List<String> superpeerNames = parser.getSuperpeerNames();
        System.out.println("\n--List of Superpeers:--\n");
        superpeerNames.forEach(System.out::println);
        System.out.println(
                "\n---------------------------------------------------------------------------------------------------------------------------------------------\n");

        System.out.println("\nInfo: Retrieving connections originating from lectureStudioServer.");
        // Filters connections that originate from the lectureStudioServer
        List<PeerConnection> connectionsFromServer = parser.getPeerConnections().stream()
                .filter(connection -> "lectureStudioServer".equals(connection.getSourceName()))
                .collect(Collectors.toList());
        System.out.println("\n--List of Connections from lectureStudioServer to Superpeers or Peers:--\n");
        connectionsFromServer
                .forEach(connection -> System.out.println("lectureStudioServer -> " + connection.getTargetName()));
        System.out.println(
                "\n---------------------------------------------------------------------------------------------------------------------------------------------\n");
        System.out.println("\nInfo: Analyzing connections from Superpeers to Peers.");
        // Retrieves the mapping of superpeers to their connected peers
        Map<String, List<String>> superpeerConnections = parser.getSuperpeerConnections();
        System.out.println("\n--List of Connections from Superpeers to Peers:--\n");
        superpeerConnections.forEach((superpeer, peers) -> System.out.println(superpeer + " -> " + peers));
        System.out.println(
                "\n---------------------------------------------------------------------------------------------------------------------------------------------\n");

        System.out.println("\nStep Done: Integrating P2P algorithm is done.\n");
    }
}