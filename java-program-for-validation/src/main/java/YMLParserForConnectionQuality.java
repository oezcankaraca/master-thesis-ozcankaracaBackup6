import org.yaml.snakeyaml.Yaml;
import java.io.FileInputStream;
import java.util.*;

/**
 * YMLParserForConnectionQuality class is designed to parse YAML files that define
 * network topologies and connection settings in a containerized environment.
 * It reads YAML configuration, extracts connection details between nodes,
 * and represents them in a structured format. 
 * This class is particularly useful for network analysis and diagnostics in containerized network setups.
 *
 * @author Özcan Karaca
 */

public class YMLParserForConnectionQuality {

    static String homeDirectory = System.getProperty("user.home");
    static String basePath = homeDirectory + "/Desktop/master-thesis-ozcankaraca";

    public static void main(String[] args) throws Exception {
        Yaml yaml = new Yaml();

        String CONNECTION_INFOS_FILE_DIR = basePath
                + "/java-program-for-container/src/main/java/containerlab-topology.yml";

        // Load YAML file containing topology configuration
        try (FileInputStream fis = new FileInputStream(CONNECTION_INFOS_FILE_DIR)) {

            Map<String, Object> data = yaml.load(fis);
            Map<String, Object> topology = safeCastMap(data.get("topology"));
            Map<String, Object> nodes = safeCastMap(topology.get("nodes"));

            List<ConnectionInfo> connections = new ArrayList<>();

            // Iterate through each node to extract connection details
            for (Object nodeKey : nodes.keySet()) {
                Map<String, Object> node = safeCastMap(nodes.get(nodeKey));

                // Iterate through each node to extract connection details
                if (node.containsKey("env")) {
                    Map<String, Object> env = safeCastMap(node.get("env"));

                    for (Map.Entry<String, Object> entry : env.entrySet()) {
                        if (entry.getKey().startsWith("CONNECTION_")) {
                            String connectionValue = String.valueOf(entry.getValue());
                            String[] parts = connectionValue.split(",");
                            String targetPeer = parts[1].split(":")[0].trim();
                            String targetPeerIp = parts[1].split(":")[1].trim();
                            connections.add(new ConnectionInfo(nodeKey.toString(), targetPeer, targetPeerIp));
                        }
                    }
                }
            }

            // Display the connection information
            for (ConnectionInfo ci : connections) {
                System.out.println(ci);
            }
        }
    }

    /**
     * Safely casts an object to a Map<String, Object> if possible.
     * Throws an IllegalArgumentException if the object is not a map.
     *
     * @param obj The object to be cast.
     * @return The object cast to a Map<String, Object>.
     */
    @SuppressWarnings("unchecked")
    private static Map<String, Object> safeCastMap(Object obj) {
        if (obj instanceof Map) {
            return (Map<String, Object>) obj;
        }
        throw new IllegalArgumentException("Error: Object is not a Map");
    }

    /**
     * The ConnectionInfo class encapsulates the details of a connection between two peers in the network, 
     * including the source peer, target peer, and the IP address of the target peer.
     */
    public static class ConnectionInfo {
        private String sourcePeer;
        private String targetPeer;
        private String targetPeerIp;

        public ConnectionInfo(String sourcePeer, String targetPeer, String targetPeerIp) {
            this.sourcePeer = sourcePeer;
            this.targetPeer = targetPeer;
            this.targetPeerIp = targetPeerIp;
        }

        public String getSourcePeer() {
            return sourcePeer;
        }

        public String getTargetPeer() {
            return targetPeer;
        }

        public String getTargetPeerIp() {
            return targetPeerIp;
        }

        @Override
        public String toString() {
            return "Source Peer: " + sourcePeer + ", Target Peer: " + targetPeer + ", Target Peer IP: " + targetPeerIp;
        }
    }
}
