import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * The YMLGenerator class is designed for creating a YAML (YML) file that defines the network topology
 * for a container-based network testbed. This class reads network configuration details from a JSON file
 * and translates them into a YML format suitable for container orchestration tools like ContainerLab.
 * It supports generating configurations for a lectureStudioServer, multiple peers, super-peers and
 * trackerPeer, includes options for additional monitoring and visualization tools like
 * Prometheus, cAdvisor, and Grafana.
 * The class handles the assignment of network interfaces, IP addresses, and environment variables 
 * for each node in the network, ensuring a cohesive and functional network setup for testing
 * and experimentation purposes.
 * 
 * @author Özcan Karaca
 */
public class YMLGenerator {
    // Get the user's home directory path
    private static final String homeDirectory = System.getProperty("user.home");
    // Define the base path for the master thesis's directory
    private static final String basePath = homeDirectory + "/Desktop/master-thesis-ozcankaraca";
    // Specify the path to the YML File for Containerlab
    private static final String pathToYMLFile = basePath
            + "/java-program-for-container/src/main/java/containerlab-topology.yml";

    private static HashMap<String, Set<String>> superPeerToPeersMap = new HashMap<>();
    private static Map<String, Integer> interfaceCounter = new HashMap<>();
    private static List<String> linkInformation = new ArrayList<>();
    private static Map<String, String> lectureStudioServerEnvVariables = new HashMap<>();
    private static Map<String, List<String>> superPeerEnvVariables = new HashMap<>();
    private static Map<String, String> peerEnvVariables = new HashMap<>();
    private static Map<String, String> peerEnvVariablesSuperPeerIP = new HashMap<>();
    private static int subnetCounter = 21;

    private static int numberOfPeers = 10;
    private static boolean useSuperPeers = true;

    /**
     * The main method for generating a YML file to configure a network testbed.
     * This method reads the network configuration from a JSON file and uses it to
     * create a YML file
     * that describes the network topology for a testbed environment.
     *
     * @param args Command-line arguments. The first argument (optional) specifies
     *             the number of peers.
     */
    public static void main(String[] args) {
        // Check if a command-line argument is provided for the number of peers.
        if (args.length > 0) {
            try {
                // Try parsing the first argument as the number of peers.
                numberOfPeers = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                // If parsing fails, log an error and use the default value for the number of
                // peers.
                System.err.println("Error: Argument must be an integer. The default value of 10 is used.");
            }
        }

        if (args.length > 1) {
            useSuperPeers = Boolean.parseBoolean(args[1]);
        }

        // Print the beginning of the YML file generation step.
        System.out.println("\nStep Started: Creating YML file for testbed.\n");

        try {

            String pathToOutputData;
            if (useSuperPeers) {
                pathToOutputData = basePath + "/data-for-testbed/outputs-with-superpeer/output-data-" + numberOfPeers + ".json";
            } else {
                pathToOutputData = basePath + "/data-for-testbed/outputs-without-superpeer/output-data-" + numberOfPeers
                        + ".json";
            }

            // Create a YMLGenerator instance with the path to the output data.
            YMLGenerator generator = new YMLGenerator(pathToOutputData);

            // Flag to determine whether to include extra nodes in the topology.
            boolean includeExtraNodes = true;

            // Generate link information based on the output data.
            generator.generateLinkInformation();

            // Process the generated link information (this could involve additional logic or steps).
            processLinkInformation();

            // Generate the topology file in YML format, including any extra nodes if required.
            generator.generateTopologyFile(includeExtraNodes);

            // Log the successful generation of the YML topology file.
            System.out.println("\nSuccess: YML topology file generated successfully.");

        } catch (IOException e) {
            // Log any IOExceptions that occur during the file generation process.
            e.printStackTrace();
            System.out.println("Unsuccess: An error occurred.");
        }

        // Print the completion of the YML file generation step.
        System.out.println("\nStep Done: Creating YML file for testbed is done.\n");
    }

    /**
     * Constructor for the YMLGenerator class. Initializes the YMLGenerator with a specific configuration file.
     * It reads and processes the output file containing network configuration details.
     *
     * @param configFilePath The path to the configuration file.
     * @throws IOException If there is an error in reading the configuration file.
     */
    public YMLGenerator(String configFilePath) throws IOException {
        System.out.println("Info: Initializing YMLGenerator with config file path: " + configFilePath);
        readAndProcessOutputFile(); // Reads the configuration file and processes the data.
        System.out.println("Info: Output file read and processed.");
    }

    /**
     * Generates a topology file in YML format for network configuration.
     * This file includes configurations for peers, super-peers, and trackerPeer.
     *
     * @param includeExtraNodes Flag indicating whether to include additional nodes like Grafana, Prometheus.
     * @throws IOException If there is an error writing to the YML file.
     */
    public void generateTopologyFile(boolean includeExtraNodes) throws IOException {
        System.out.println("Info: Generating topology file.");

        // Create a set to collect all peer names including supe-rpeers.
        Set<String> allPeers = new HashSet<>();

        // Get the names of super-peers.
        Set<String> superPeerNames = superPeerToPeersMap.keySet();

        // Define topology parameters.
        String nameOfTopology = "containerlab-topology";
        String prefixOfTopology = "p2p";
        String subnetOfTopology = "172.100.100.0/24";

        // Collect all peer names including super-peers.
        for (String superPeer : superPeerToPeersMap.keySet()) {
            allPeers.add(superPeer);
            allPeers.addAll(superPeerToPeersMap.get(superPeer));
        }

        try (FileWriter fw = new FileWriter(pathToYMLFile)) {
            System.out.println("Info: Writing topology to file: " + pathToYMLFile);

            // Start writing the topology configuration to the YML file.
            fw.write("name: " + nameOfTopology + "\n");
            fw.write("prefix: " + prefixOfTopology + "\n\n");
            fw.write("mgmt:\n");
            fw.write("  network: fixedips\n");
            fw.write("  ipv4-subnet: " + subnetOfTopology + "\n");
            fw.write("topology:\n");
            fw.write("  nodes:\n");

            // Write node configurations for trackerPeer
            fw.write("    trackerPeer:\n");
            fw.write("      kind: linux\n");
            fw.write("      mgmt-ipv4: 172.100.100.11\n");
            fw.write("      image: image-tracker\n");
            fw.write("      env:\n");
            fw.write("        NUMBER_OF_TOTAL_PEERS: " + allPeers.size() + "\n");
            fw.write("      exec:\n");
            fw.write("        - sleep 5\n");
            fw.write("      cmd: \"java -cp /app TrackerPeer\"\n");
            fw.write("      ports:\n");
            fw.write("        - \"5050:5050\"\n\n");

            // Write node configurations for lectureStudioServer
            fw.write("    lectureStudioServer:\n");
            fw.write("      kind: linux\n");
            fw.write("      mgmt-ipv4: 172.100.100.12\n");
            fw.write("      image: image-testbed\n");
            fw.write("      env:\n");
            fw.write("        NUMBER_OF_TOTAL_PEERS: " + allPeers.size() + "\n");
            fw.write("        PREFIX_NAME_OF_CONTAINER: " + prefixOfTopology + "-" + nameOfTopology + "\n");
            Set<String> lectureStudioPeers = superPeerToPeersMap.getOrDefault("lectureStudioServer", new HashSet<>());
            fw.write("        SOURCE_PEER: lectureStudioServer\n");
            fw.write("        TARGET_PEERS: " + String.join(",", lectureStudioPeers) + "\n");

            // Write configurations for lectureStudioServer
            for (Map.Entry<String, String> entry : lectureStudioServerEnvVariables.entrySet()) {
                fw.write("        " + entry.getKey() + ": " + entry.getValue() + "\n");
            }

            fw.write("        MAIN_CLASS: LectureStudioServer\n");
            fw.write("      labels:\n");
            fw.write("        role: sender\n");
            fw.write("        group: server\n");
            fw.write("      binds:\n");
            fw.write(
                    "        - " + basePath + "/data-for-testbed/mydocument.pdf:/app/mydocument.pdf\n");
            fw.write(
                    "        - " + basePath + "/data-for-testbed/connection-details/connection-details-"
                            + numberOfPeers + ".json:/app/connection-details-" + numberOfPeers + ".json\n");
            fw.write(
                    "        - " + basePath
                            + "/data-for-testbed/script-for-connection/connection-details-superpeer.sh:/app/connection-details-superpeer.sh\n");
            fw.write("      exec:\n");
            fw.write("        - sleep 5\n");
            fw.write("        - chmod +x /app/connection-details-superpeer.sh\n");
            fw.write("        - ./connection-details-superpeer.sh\n");
            fw.write("      ports:\n");
            fw.write("        - \"7070:7070\"\n\n");

            // Iterate through all peers and write node configurations
            for (String peerName : allPeers) {
                if (peerName.equals("lectureStudioServer")) {
                    continue;
                }

                boolean isNormalPeer = !superPeerNames.contains(peerName);
                String superPeer = determineSuperPeerForPeer(peerName);
                String mainClass = superPeerNames.contains(peerName) ? "SuperPeer" : "Peer";
                String role = superPeerNames.contains(peerName) ? "receiver/sender" : "receiver";

                // Write configurations for peers
                fw.write("    " + peerName + ":\n");
                fw.write("      kind: linux\n");
                fw.write("      image: image-testbed\n");
                fw.write("      env:\n");
                fw.write("        NUMBER_OF_TOTAL_PEERS: " + allPeers.size() + "\n");
                fw.write("        PREFIX_NAME_OF_CONTAINER: " + prefixOfTopology + "-" + nameOfTopology + "\n");

                if (mainClass.equals("Peer")) {
                    fw.write("        SOURCE_PEER: " + peerName + "\n");
                }

                // Write configurations for IP address
                for (Map.Entry<String, String> entry : peerEnvVariables.entrySet()) {
                    if (entry.getKey().equals(peerName)) {
                        fw.write("        IP_ADDRES: " + entry.getValue() + "\n");
                    }
                }

                fw.write("        SUPER_PEER: " + superPeer + "\n");

                // Write configurations for IP address
                for (Map.Entry<String, String> entry : peerEnvVariablesSuperPeerIP.entrySet()) {
                    if (entry.getKey().equals(peerName)) {
                        fw.write("        SUPER_PEER_IP_ADDRES: " + entry.getValue() + "\n");
                    }
                }

                // Write configurations for source and target peers
                if (mainClass.equals("SuperPeer")) {
                    Set<String> targetPeers = superPeerToPeersMap.getOrDefault(peerName, new HashSet<>());
                    fw.write("        SOURCE_PEER: " + peerName + "\n");
                    fw.write("        TARGET_PEERS: " + String.join(",", targetPeers) + "\n");
                }

                // Write additional environment variables for super peers
                if (superPeerEnvVariables.containsKey(peerName)) {
                    List<String> envVars = superPeerEnvVariables.get(peerName);
                    for (String envVar : envVars) {
                        fw.write("        " + envVar + "\n");
                    }
                }

                fw.write("        MAIN_CLASS: " + mainClass + "\n");
                fw.write("      labels:\n");
                fw.write("        role: " + role + "\n");
                fw.write("        group: " + (mainClass.equals("SuperPeer") ? "superpeer" : "peer") + "\n");

                appendBindsAndExec(fw, isNormalPeer);
            }

            // Append extra nodes if required.
            if (!includeExtraNodes) {
                System.out.println("Info: Appending extra nodes: Grafana, Prometheus and cAdvisor");
                appendExtraNodes(fw);
            }

            // Write link configurations.
            fw.write("  links:\n");
            for (String link : linkInformation) {
                fw.write("    - endpoints: [" + link + "]\n");
            }

            System.out.println("Info: YML file successfully written.");
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Error: An error occurred while generating the topology YML file.");
        }
    }

    /**
     * Appends additional monitoring and visualization tools to the YML configuration.
     * These tools are used for monitoring the network and visualizing metrics.
     *
     * @param fw FileWriter instance used to write to the YML file.
     * @throws IOException If an error occurs while writing to the file.
     */
    private void appendExtraNodes(FileWriter fw) throws IOException {
        // Configuration for the prometheus tool
        fw.write("\n    prometheus:\n");
        fw.write("      kind: linux\n");
        fw.write("      image: image-prometheus\n");
        fw.write("      binds:\n");
        fw.write(
                "       - " + basePath
                        + "/data-for-testbed/data-for-analysing-monitoring/prometheus.yml:/etc/prometheus/prometheus.yml\n");
        fw.write("      ports:\n");
        fw.write("        - \"9090:9090\"\n");

        // Configuration for the cadvisor tool
        fw.write("\n    cadvisor:\n");
        fw.write("      kind: linux\n");
        fw.write("      image: image-cadvisor\n");
        fw.write("      binds:\n");
        fw.write("        - /:/rootfs:ro\n");
        fw.write("        - /var/run:/var/run:ro\n");
        fw.write("        - /sys:/sys:ro\n");
        fw.write("        - /var/snap/docker/common/var-lib-docker/:/var/lib/docker:ro\n");
        fw.write("      ports:\n");
        fw.write("        - \"8080:8080\"\n");

        // Configuration for the grafana tool
        fw.write("\n    grafana:\n");
        fw.write("      kind: linux\n");
        fw.write("      image: image-grafana\n");
        fw.write("      ports:\n");
        fw.write("       - \"3000:3000\"\n");
    }

    /**
     * Appends binding and execution commands to the YML file for either super-peer or peer nodes.
     *
     * @param fw          FileWriter instance used to write to the YML file.
     * @param isSuperPeer Boolean flag to indicate if the node is a super-peer.
     * @throws IOException If an error occurs while writing to the file.
     */
    private void appendBindsAndExec(FileWriter fw, boolean isSuperPeer) throws IOException {
        if (!isSuperPeer) {
            // Bind and execution commands for normal peers
            fw.write("      binds:\n");
            fw.write(
                    "        - " + basePath + "/data-for-testbed/connection-details/connection-details-" + numberOfPeers
                            + ".json:/app/connection-details-" + numberOfPeers + ".json\n");
            fw.write(
                    "        - " + basePath
                            + "/data-for-testbed/script-for-connection/connection-details-superpeer.sh:/app/connection-details-superpeer.sh\n");
            fw.write("      exec:\n");
            fw.write("        - sleep 5\n");
            fw.write("        - /bin/sh -c 'while ! ping -c 1 172.100.100.12 > /dev/null; do echo \"Waiting for lectureStudioServer\"; sleep 1; done'\n");
            fw.write("        - chmod +x /app/connection-details-superpeer.sh\n");
            fw.write("        - ./connection-details-superpeer.sh\n");
        } else {
            // Bind and execution commands for super-peers
            fw.write("      binds:\n");
            fw.write(
                    "        - " + basePath
                            + "/data-for-testbed/script-for-connection/connection-details-peer.sh:/app/connection-details-peer.sh\n");
            fw.write("      exec:\n");
            fw.write("        - sleep 5\n");
            fw.write("        - /bin/sh -c 'while ! ping -c 1 172.100.100.12 > /dev/null; do echo \"Waiting for lectureStudioServer\"; sleep 1; done'\n");
            fw.write("        - chmod +x /app/connection-details-peer.sh\n");
            fw.write("        - ./connection-details-peer.sh\n");
        }
    }

    /**
     * Generates an IP address for a node within the network.
     * The IP address is generated based on the connection counter and the node's position.
     *
     * @param connectionCounter The connection number in the sequence of generated connections.
     * @param isFirstNode       Boolean indicating if the node is the first in the connection pair.
     * @return Generated IP address as a String.
     */
    private static String generateIpAddress(int connectionCounter, boolean isFirstNode) {
        String baseIp = "172.20." + (subnetCounter + connectionCounter - 1) + ".";
        int lastOctet = isFirstNode ? 2 : 3; // Determines the last octet based on whether it's the first or second node
        return baseIp + lastOctet; // Returns the complete IP address
    }

    /**
     * Processes link information to assign IP addresses and environment variables for each connection.
     * This method generates IP addresses for each link and updates environment
     * variables for peers and super-peers.
     */
    public static void processLinkInformation() {
        int connectionCounter = 1;
        for (String link : linkInformation) {
            // Splitting the link into two endpoint details
            String[] endpoints = link.split(", ");
            String[] node1Details = endpoints[0].split(":");
            String[] node2Details = endpoints[1].split(":");

            // Generating IP addresses for both nodes in the link
            String node1Ip = generateIpAddress(connectionCounter, true);
            String node2Ip = generateIpAddress(connectionCounter, false);

            // Creating and storing environment variable values for nodes
            String envVariableValue = node1Details[1] + ":" + node1Ip + ", " + node2Details[0] + ":" + node2Ip;
            peerEnvVariables.put(node2Details[0], node2Ip);
            peerEnvVariablesSuperPeerIP.put(node2Details[0], node1Ip);

            // Storing connection details in the node environment variables
            if (node1Details[0].equals("lectureStudioServer")) {
                lectureStudioServerEnvVariables.put("CONNECTION_" + connectionCounter, envVariableValue);
            } else {
                superPeerEnvVariables.computeIfAbsent(node1Details[0], k -> new ArrayList<>())
                        .add("CONNECTION_" + connectionCounter + ":" + " " + envVariableValue);
            }
            connectionCounter++;
        }

    }

    /**
     * Assigns an interface name (e.g., eth1, eth2) for a given node.
     * The interface name is generated based on the number of interfaces already assigned to the node.
     *
     * @param nodeName The name of the node for which the interface is to be assigned.
     * @return The interface name as a String.
     */
    private String assignInterface(String nodeName) {
        // Retrieve the current count of interfaces for the node, defaulting to 1
        int count = interfaceCounter.getOrDefault(nodeName, 1);
        // Increment and update the counter for the node
        interfaceCounter.put(nodeName, count + 1);

        return "eth" + count;
    }

    /**
     * Determines the super-peer responsible for a given peer.
     * It iterates through the map of super-peers to their connected peers to find the corresponding super-peer.
     *
     * @param peerName The name of the peer whose super-peer is to be determined.
     * @return The name of the super-peer.
     */
    private String determineSuperPeerForPeer(String peerName) {
        // Iterate over each super-peer and their connected peers
        for (Map.Entry<String, Set<String>> entry : superPeerToPeersMap.entrySet()) {
            // Check if the current super-peer has the given peer in its set of connected peers
            if (entry.getValue().contains(peerName)) {
                return entry.getKey();
            }
        }

        return "lectureStudioServer";
    }

    /**
     * Reads and processes the output JSON file to extract network connections.
     * This method constructs a map of super-peers and their connected peers based on the output file.
     */
    private void readAndProcessOutputFile() {
        System.out.println("Info: Reading and processing the output file.");
        ObjectMapper mapper = new ObjectMapper();

        String pathToOutputFile;
        if (useSuperPeers) {
            pathToOutputFile = basePath + "/data-for-testbed/outputs-with-superpeer/output-data-" + numberOfPeers + ".json";
        } else {
            pathToOutputFile = basePath + "/data-for-testbed/outputs-without-superpeer/output-data-" + numberOfPeers
                    + ".json";
        }

        try {
            // Read the entire JSON tree from the output file
            JsonNode rootNode = mapper.readTree(new File(pathToOutputFile));
            JsonNode peer2peerNode = rootNode.path("peer2peer");

            // Iterate over each connection in the peer-to-peer array
            for (JsonNode connection : peer2peerNode) {
                String sourceName = connection.path("sourceName").asText();
                String targetName = connection.path("targetName").asText();

                // Add the source (super-peer) and target (peer) relationship to the map
                superPeerToPeersMap.putIfAbsent(sourceName, new HashSet<>());
                superPeerToPeersMap.get(sourceName).add(targetName);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Generates information about the links between nodes in the network.
     * This method creates a set of unique connections between super-peers and their
     * peers and between lectureStudioServer and its peers.
     */
    public void generateLinkInformation() {
        System.out.println("Info: Generating link information.");
        Set<String> uniqueConnections = new HashSet<>();

        // Retrieve the set of peers connected to the lectureStudioServer
        Set<String> lectureStudioPeers = superPeerToPeersMap.getOrDefault("lectureStudioServer", new HashSet<>());
        for (String peer : lectureStudioPeers) {
            // Check to ensure the connection hasn't already been added
            if (!uniqueConnections.contains(peer)) {
                String lectureStudioInterface = assignInterface("lectureStudioServer");
                String peerInterface = assignInterface(peer);

                // Create a formatted string representing the link information for lectureStudioServer
                String linkInfo = "lectureStudioServer:" + lectureStudioInterface + ", " + peer + ":" + peerInterface;
                linkInformation.add(linkInfo);
                uniqueConnections.add(peer);
            }
        }

        // Iterate over each super-peer and their connected peers
        for (String superPeer : superPeerToPeersMap.keySet()) {
            Set<String> connectedPeers = superPeerToPeersMap.getOrDefault(superPeer, new HashSet<>());
            for (String peer : connectedPeers) {
                if (!uniqueConnections.contains(peer)) {
                    String superPeerInterface = assignInterface(superPeer);
                    String peerInterface = assignInterface(peer);

                    // Create a formatted string representing the link information for super-peer
                    String linkInfo = superPeer + ":" + superPeerInterface + ", " + peer + ":" + peerInterface;
                    linkInformation.add(linkInfo);
                    uniqueConnections.add(peer);
                }
            }
        }
    }
}