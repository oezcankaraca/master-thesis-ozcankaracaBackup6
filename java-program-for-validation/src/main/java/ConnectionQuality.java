import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.yaml.snakeyaml.Yaml;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.ExecCreateCmdResponse;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.core.DockerClientBuilder;

/**
 * The ConnectionQuality class is designed for assessing network performance characteristics
 * within a containerized environment. It facilitates the execution of network tests,
 * such as latency and bandwidth measurements, between different Docker containers.
 * The class integrates functionalities for parsing YAML files to extract network topology and
 * connection details, executing network tests using tools like ping and iperf3, and analyzing the test results.
 *
 * The class primarily works with Docker containers, identifying them by name and executing
 * commands within them to measure network performance metrics. Additionally, it parses JSON data
 * to compare the expected network parameters with the measured ones, calculates error percentages,
 * and determines if the network's performance is within acceptable limits.
 *
 * @author Özcan Karaca
 */

public class ConnectionQuality extends YMLParserForConnectionQuality {

    // Static initializer block for configuring logging settings.
    static {
        BasicConfigurator.configure();
        Logger.getRootLogger().setLevel(Level.ERROR);
    }

    private DockerClient dockerClient;
    private List<String> containerNames;
    private static boolean allTestsSuccessful = true;

    private static List<Double> latencyErrorRates = new ArrayList<>();
    private static List<Double> bandwidthErrorRates = new ArrayList<>();

    // Static variables for directory paths.
    static String homeDirectory = System.getProperty("user.home");
    static String basePath = homeDirectory + "/Desktop/master-thesis-ozcankaraca";

    private static int numberOfPeers = 5;

    /**
     * Main method to initiate network validation.
     * 
     * @param args Command line arguments, expects number of peers as an optional argument.
     */
    public static void main(String[] args) throws InterruptedException {

        System.out.println("\nStep Started: Validation of the network characteristics.\n");

        // Parsing command-line arguments for number of peers.
        if (args.length > 0) {
            try {
                numberOfPeers = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.err.println("Error: Argument must be an integer. Defaulting to 10.");
            }
        }

        // Setting up connection information file path.
        String CONNECTION_INFOS_FILE_DIR = basePath
                + "/java-program-for-container/src/main/java/containerlab-topology.yml";

        // Running tests for network characteristics.
        ConnectionQuality tests = new ConnectionQuality();
        List<ConnectionInfo> connectionInfos = tests.getConnectionInfos(CONNECTION_INFOS_FILE_DIR);
        tests.runTests(connectionInfos);

        displayFinalResults();

        System.out.println("\nStep Done: Validation of the network characteristics is done.\n");

        // Final test results
        if (allTestsSuccessful) {
            System.out.println("Success: All tests were successful.");
            System.exit(0);
        } else {
            System.out.println("Unsuccess: Some tests need to be repeated.");
            System.exit(1);
        }
    }

    /**
     * Retrieves names of Docker containers.
     */
    private void retrieveContainerNames() {
        containerNames = new ArrayList<>();
        List<Container> containers = dockerClient.listContainersCmd().withShowAll(true).exec();
        for (Container container : containers) {
            containerNames.add(container.getNames()[0].substring(1));
        }
    }

    /**
     * Finds container name by its number.
     * 
     * @param number The specific number part of the container name.
     * @return The full container name matching the provided number.
     * @throws IllegalArgumentException if no container matches the provided number.
     */
    private String findContainerNameByNumber(String number) {
        for (String name : containerNames) {
            if (name.matches(".*\\-" + number + "$")) {
                return name;
            }
        }
        throw new IllegalArgumentException("Error: No container found with number: " + number);
    }

    /**
     * Constructor for ConnectionQuality class.
     * Initializes Docker client and retrieves container names.
     */
    public ConnectionQuality() {
        dockerClient = DockerClientBuilder.getInstance().build();
        retrieveContainerNames();
    }

    /**
     * Retrieves connection information from a YAML file.
     * 
     * @param yamlFilePath The path to the YAML file containing connection details.
     * @return A list of ConnectionInfo objects extracted from the YAML file.
     */
    public List<ConnectionInfo> getConnectionInfos(String yamlFilePath) {
        List<ConnectionInfo> connectionInfos = new ArrayList<>();
        Yaml yaml = new Yaml();

        try (FileInputStream fis = new FileInputStream(yamlFilePath)) {
            // Parsing the YAML file
            Map<String, Object> data = yaml.load(fis);
            Map<String, Object> topology = safeCastMap(data.get("topology"));
            Map<String, Object> nodes = safeCastMap(topology.get("nodes"));

            // Extracting connection details
            for (Object nodeKey : nodes.keySet()) {
                Map<String, Object> node = safeCastMap(nodes.get(nodeKey));

                if (node.containsKey("env")) {
                    Map<String, Object> env = safeCastMap(node.get("env"));

                    for (Map.Entry<String, Object> entry : env.entrySet()) {
                        if (entry.getKey().startsWith("CONNECTION_")) {
                            // Processing connection details
                            String connectionValue = String.valueOf(entry.getValue());
                            String[] parts = connectionValue.split(",");
                            String targetPeer = parts[1].split(":")[0].trim();
                            String targetPeerIp = parts[1].split(":")[1].trim();
                            connectionInfos.add(new ConnectionInfo(nodeKey.toString(), targetPeer, targetPeerIp));
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return connectionInfos;
    }

    /**
     * Casts an object to a map if possible.
     * 
     * @param obj The object to be cast.
     * @return The object cast to a map.
     * @throws IllegalArgumentException if the object cannot be cast to a map.
     */
    @SuppressWarnings("unchecked")
    private static Map<String, Object> safeCastMap(Object obj) {
        if (obj instanceof Map) {
            return (Map<String, Object>) obj;
        }
        throw new IllegalArgumentException("Error: Object is not a Map");
    }

    /**
     * Performs iperf and ping tests on a list of connection infos.
     * 
     * @param connectionInfos List of ConnectionInfo objects to test.
     * @throws InterruptedException If thread interruption occurs during the tests.
     */
    private void iperfAndPingTests(List<ConnectionInfo> connectionInfos) throws InterruptedException {

        // Iterating through connections to perform tests
        for (ConnectionInfo info : connectionInfos) {
            int attempts = 0;
            boolean testSuccessful;

            // Extracting container names
            String targetPeerNumber = info.getTargetPeer();
            String targetPeer = findContainerNameByNumber(targetPeerNumber);
            String sourcePeerNumber = info.getSourcePeer();
            String sourcePeer = findContainerNameByNumber(sourcePeerNumber);

            do {
                // Performing connection tests
                System.out.println("\n--Latency and Bandwidth Tests from " + sourcePeer + " to " + targetPeer + "--");
                System.out.println("\nTesting connection from " + sourcePeer + " to " + targetPeer
                        + " (Attempt " + (attempts + 1) + "):");
                testSuccessful = performConnectionTest(info);
                attempts++;
            } while (!testSuccessful && attempts < 3);

            if (!testSuccessful) {
                allTestsSuccessful = false;
            }
            System.out.println(
                    "\n---------------------------------------------------------------------------------------------------------------------------------------------\n");

        }
    }

    /**
     * Performs a connection test between two peers using ping for latency measurement
     * and iperf3 for bandwidth measurement. The method retries up to three times if
     * the error thresholds for bandwidth or latency are not met.
     * 
     * @param info ConnectionInfo object containing details for the test.
     * @return True if the test is successful, false otherwise.
     * @throws InterruptedException If thread interruption occurs during the tests.
     */
    private boolean performConnectionTest(ConnectionInfo info) throws InterruptedException {
        // Extract peer and IP information from the ConnectionInfo object.
        String targetPeerNumber = info.getTargetPeer();
        String targetPeer = findContainerNameByNumber(targetPeerNumber);
        String sourcePeerNumber = info.getSourcePeer();
        String sourcePeer = findContainerNameByNumber(sourcePeerNumber);
        String targetPeerIp = info.getTargetPeerIp();

        int attempts = 0;
        boolean testSuccessful = false;

        // Loop up to three attempts or until the test is successful.
        while (attempts < 3 && !testSuccessful) {

            // Perform ping and iperf tests.
            String pingResult = executeCommand(sourcePeer, "ping -c 4 " + targetPeerIp);
            executeCommandInBackground(targetPeer, "iperf3 -s");
            Thread.sleep(5000); // Wait for the server to be ready.
            String iperfResult = executeCommand(sourcePeer, "iperf3 -c " + targetPeerIp);

            // Extract measured values from the test results.
            double measuredBandwidth = extractAndPrintIperfBandwidth(iperfResult);
            double measuredLatency = extractAndPrintPingLatency(pingResult);

            // Load applied (expected) values from a configuration file.
            String CONNECTION_DETAILS_FILE_DIR = basePath + "/data-for-testbed/connection-details/connection-details-"
                    + numberOfPeers + ".json";
            Object[] appliedValues = extractData(sourcePeerNumber, targetPeerNumber, CONNECTION_DETAILS_FILE_DIR);

            int appliedBandwidth = (int) ((Integer) appliedValues[0]).doubleValue();
            double appliedLatency = Double.parseDouble((String) appliedValues[1]);
            double appliedLoss = Double.parseDouble((String) appliedValues[2]);

            // Analyze results and print a comparison.
            analyzeAndPrintResults(measuredBandwidth, appliedBandwidth, measuredLatency, appliedLatency, appliedLoss,
                    sourcePeer, targetPeer);

            // Calculate error percentages.
            double bandwidthError = calculateErrorPercentage(measuredBandwidth, appliedBandwidth);
            double latencyError = calculateErrorPercentage(measuredLatency, appliedLatency);

            // Check if the test is successful based on error thresholds.
            if (bandwidthError <= 5 && latencyError <= determineAcceptableLatencyErrorRate(measuredBandwidth)) {
                testSuccessful = true;

                latencyErrorRates.add(latencyError);
                bandwidthErrorRates.add(bandwidthError);

            } else {
                attempts++;
                if (attempts < 3) {
                    System.out.println("\nRetry: Test failed, retrying... (Attempt " + (attempts + 1) + ")");
                }
            }
        }

        // Final decision based on test success or failure after all attempts.
        if (testSuccessful) {
            System.out.println("\nInfo: Test from " + sourcePeer + " to " + targetPeer + " successful after "
                    + (attempts + 1) + " attempts.");
            return true;
        } else {
            System.out.println("\nError: Test from " + sourcePeer + " to " + targetPeer + " failed after 3 attempts.");
            return false;
        }
    }

    /**
     * Determines the acceptable latency error rate based on the measured bandwidth.
     * Higher bandwidths allow for tighter error margins.
     * 
     * @param measuredBandwidth The bandwidth measured during the connection test.
     * @return The acceptable latency error rate percentage.
     */
    private double determineAcceptableLatencyErrorRate(double measuredBandwidth) {
        // Define acceptable latency error rates based on different bandwidth ranges.
        if (measuredBandwidth < 100) {
            return 35;
        } else if (measuredBandwidth >= 100 && measuredBandwidth <= 200) {
            return 30;
        } else if (measuredBandwidth >= 200 && measuredBandwidth <= 500) {
            return 25;
        } else if (measuredBandwidth >= 500 && measuredBandwidth <= 1000) {
            return 20;
        } else if (measuredBandwidth >= 1000 && measuredBandwidth <= 3000) {
            return 15;
        } else {
            return 10; // Tightest error margin for the highest bandwidths.
        }
    }

    /**
     * Calculates the average of a list of double values.
     * Returns 0.0 if the list is empty.
     *
     * @param rates The list of double values.
     * @return The average of the values in the list.
     */
    private static double calculateAverage(List<Double> rates) {
        if (rates.isEmpty()) {
            return 0.0; // Return default value if list is empty
        }
        double sum = 0.0;
        for (Double rate : rates) {
            sum += rate; // Sum up all the rates
        }
        return sum / rates.size(); // Calculate average
    }

    /**
     * Calculates the maximum value in a list of double values.
     * Returns 0.0 if the list is empty.
     *
     * @param rates The list of double values.
     * @return The maximum value in the list.
     */
    private static double calculateMax(List<Double> rates) {
        if (rates.isEmpty()) {
            return 0.0; // Return default value if list is empty
        }
        double max = rates.get(0); // Initialize max with the first value
        for (Double rate : rates) {
            if (rate > max) {
                max = rate; // Update max if current rate is greater
            }
        }
        return max; // Return the max value found
    }

    /**
     * Calculates the minimum value in a list of double values.
     * Returns 0.0 if the list is empty.
     *
     * @param rates The list of double values.
     * @return The minimum value in the list.
     */
    private static double calculateMin(List<Double> rates) {
        if (rates.isEmpty()) {
            return 0.0; // Return default value if list is empty
        }
        double min = rates.get(0); // Initialize min with the first value
        for (Double rate : rates) {
            if (rate < min) {
                min = rate; // Update min if current rate is lower
            }
        }
        return min; // Return the min value found
    }

    /**
     * Displays the final results of latency and bandwidth error rates,
     * including the minimum, average, and maximum error rates calculated from the provided lists.
     */
    private static void displayFinalResults() {
        System.out.printf("**Final Results for Latency and Bandwidth Error Rates**%n");
        System.out.printf("Min Latency Error Rate: %.2f%%%n", calculateMin(latencyErrorRates));
        System.out.printf("Average Latency Error Rate: %.2f%%%n", calculateAverage(latencyErrorRates));
        System.out.printf("Max Latency Error Rate: %.2f%%%n", calculateMax(latencyErrorRates));
        System.out.printf("Min Bandwidth Error Rate: %.2f%%%n", calculateMin(bandwidthErrorRates));
        System.out.printf("Average Bandwidth Error Rate: %.2f%%%n", calculateAverage(bandwidthErrorRates));
        System.out.printf("Max Bandwidth Error Rate: %.2f%%%n", calculateMax(bandwidthErrorRates));
    }

    /**
     * Extracts and prints the average latency from a ping command output.
     * 
     * @param pingOutput The output string from a ping command.
     * @return The calculated average latency in milliseconds.
     */
    private double extractAndPrintPingLatency(String pingOutput) {
        double latency = 0.0;
        String[] lines = pingOutput.split("\n");
        for (String line : lines) {
            if (line.contains("avg")) {
                String[] parts = line.split("/");
                String latencyString = parts[4].replace(',', '.');
                latency = Double.parseDouble(latencyString);
            }
        }
        return latency;
    }

    /**
     * Extracts and prints the bandwidth from an iperf command output.
     * 
     * @param iperfOutput The output string from an iperf command.
     * @return The calculated bandwidth in Kbits/sec.
     */
    private int extractAndPrintIperfBandwidth(String iperfOutput) {
        double bandwidthAsDouble = 0.0;
        String[] lines = iperfOutput.split("\n");
        for (String line : lines) {
            if (line.contains("sec")
                    && (line.contains("Gbits/sec") || line.contains("Mbits/sec") || line.contains("Kbits/sec"))
                    && line.contains("receiver")) {
                String[] parts = line.split("\\s+");
                for (int i = 0; i < parts.length; i++) {
                    if ("Gbits/sec".equals(parts[i]) || "Mbits/sec".equals(parts[i]) || "Kbits/sec".equals(parts[i])) {
                        bandwidthAsDouble = Double.parseDouble(parts[i - 1]);
                        
                        // Convert bandwidth to Kbits/sec
                        if ("Gbits/sec".equals(parts[i])) {
                            bandwidthAsDouble *= 1000000;
                        } else if ("Mbits/sec".equals(parts[i])) {
                            bandwidthAsDouble *= 1000;
                        }
                        break;
                    }
                }
            }
        }

        int bandwidth = (int) bandwidthAsDouble;

        return bandwidth;
    }

    /**
     * Extracts data from a JSON file for given source and target names.
     * 
     * @param sourceName The name of the source node.
     * @param targetName The name of the target node.
     * @param filePath   The file path to the JSON file.
     * @return An array of Objects containing extracted data.
     */
    public Object[] extractData(String sourceName, String targetName, String filePath) {
        // Initialize an array to hold bandwidth, latency, and loss values
        Object[] appliedValues = new Object[3];

        try (FileInputStream fis = new FileInputStream(new File(filePath))) {
            String data = new String(fis.readAllBytes(), StandardCharsets.UTF_8);
            JSONArray jsonArray = new JSONArray(data); // Converts the String into a JSONArray

            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject obj = jsonArray.getJSONObject(i); // Retrieves the JSONObject at position i
                
                // Checks if sourceName and targetName match the provided parameters
                if (obj.getString("sourceName").equals(sourceName) && obj.getString("targetName").equals(targetName)) {
                    // Stores bandwidth, latency, and loss values in the result array
                    appliedValues[0] = obj.getInt("bandwidth");
                    appliedValues[1] = obj.getString("latency");
                    appliedValues[2] = obj.getString("loss");

                    return appliedValues;
                }
            }
            
            // Message if no data was found for the specified sourceName and targetName
            System.out.println("Error: No data found for the specified sourceName and targetName.");
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Error: Error reading the JSON file.");
        }

        return appliedValues; // Returns the array, even if no specific data was found
    }

    /**
     * Executes a command in a specified Docker container and returns the output.
     * 
     * @param containerName The name of the Docker container.
     * @param command       The command to be executed inside the container.
     * @return The output of the executed command.
     */
    private String executeCommand(String containerName, String command) {
        final StringBuilder output = new StringBuilder();
        try {
            // Wraps the command in a shell call
            String[] commandWithShell = { "/bin/sh", "-c", command };
            // Creates a command execution request for the specified container
            ExecCreateCmdResponse execCreateCmdResponse = dockerClient.execCreateCmd(containerName)
                    .withAttachStdout(true) // Attach to stdout to capture command output
                    .withAttachStderr(true) // Also attach stderr to capture any errors
                    .withCmd(commandWithShell) // Command to execute
                    .exec();

            // Starts the command execution and processes the output asynchronously
            dockerClient.execStartCmd(execCreateCmdResponse.getId())
                    .exec(new ResultCallback.Adapter<Frame>() {
                        @Override
                        public void onNext(Frame frame) {
                            String payload = new String(frame.getPayload()); // Convert frame payload to string
                            output.append(payload); // Append the output to the StringBuilder
                        }
                    })
                    .awaitCompletion(30, TimeUnit.SECONDS); // Wait for command completion or timeout
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Error: Error while executing command in " + containerName);
        }
        return output.toString(); // Return the accumulated output as a string
    }

    /**
     * Executes a command in a Docker container without waiting for the output (runs in background).
     * 
     * @param containerName The name of the Docker container.
     * @param command       The command to be executed inside the container.
     */
    private void executeCommandInBackground(String containerName, String command) {
        try {
            // Wraps the command in a shell call for execution
            String[] commandWithShell = { "/bin/sh", "-c", command };
            // Creates a command execution request for the specified container
            ExecCreateCmdResponse execCreateCmdResponse = dockerClient.execCreateCmd(containerName)
                    .withAttachStdout(false) // No need to attach stdout for background execution
                    .withAttachStderr(true) // Attach stderr to capture any errors
                    .withCmd(commandWithShell) // Command to execute
                    .exec();

            // Starts the command execution without waiting for completion
            dockerClient.execStartCmd(execCreateCmdResponse.getId()).start();
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Error: Error while executing background command in " + containerName);
        }
    }

    /**
     * Calculates the error percentage between a measured value and an applied value.
     * 
     * @param measuredValue The measured value from the test.
     * @param appliedValue  The expected or applied value.
     * @return The error percentage.
     */
    private double calculateErrorPercentage(double measuredValue, double appliedValue) {
        return Math.abs(measuredValue - appliedValue) / appliedValue * 100;
    }

    /**
     * Analyzes test results, calculates error percentages, and prints the outcomes.
     * 
     * @param measuredBandwidth The measured bandwidth from the test.
     * @param appliedBandwidth  The applied bandwidth.
     * @param measuredLatency   The measured latency from the test.
     * @param appliedLatency    The applied latency.
     * @param sourcePeer        The source peer of the test.
     * @param targetPeer        The target peer of the test.
     * @throws InterruptedException If thread interruption occurs during the process.
     */
    private void analyzeAndPrintResults(double measuredBandwidth, int appliedBandwidth, double measuredLatency,
            double appliedLatency, double appliedLoss, String sourcePeer, String targetPeer)
            throws InterruptedException {
        
        // Calculate error percentages for bandwidth and latency against applied (expected) values.
        double bandwidthError = calculateErrorPercentage(measuredBandwidth, appliedBandwidth);
        double latencyError = calculateErrorPercentage(measuredLatency, appliedLatency);
        
        // Determine the acceptable latency error rate based on measured bandwidth.
        double acceptableLatencyErrorRate;

        // Set acceptable error rates based on bandwidth ranges.
        if (measuredBandwidth < 100) {
            acceptableLatencyErrorRate = 35;
        } else if (measuredBandwidth >= 100 && measuredBandwidth <= 200) {
            acceptableLatencyErrorRate = 30;
        } else if (measuredBandwidth >= 200 && measuredBandwidth <= 500) {
            acceptableLatencyErrorRate = 25;
        } else if (measuredBandwidth >= 500 && measuredBandwidth <= 1000) {
            acceptableLatencyErrorRate = 20;
        } else if (measuredBandwidth >= 1000 && measuredBandwidth <= 3000) {
            acceptableLatencyErrorRate = 15;
        } else {
            acceptableLatencyErrorRate = 10;
        }

        // Print measurement results.
        System.out.println("\n**Results of the Measurements:**");
        System.out.printf("Measured Bandwidth: %.0f Kbits/sec%n", measuredBandwidth);
        System.out.printf(Locale.US, "Measured Latency: %.2f ms%n", measuredLatency);
        System.out.println("Applied Bandwidth: " + appliedBandwidth + " Kbits/sec");
        System.out.println("Applied Latency: " + appliedLatency + " ms");
        System.out.println("Applied Packet Loss: " + appliedLoss + " %");

        // Print error rates and compare them against acceptable thresholds.
        System.out.println("\n**Results of the Error Rate:**");
        System.out.printf("Error Rate for Bandwidth: %.2f%%%n", bandwidthError);
        System.out.printf("Error Rate for Latency: %.2f%%%n", latencyError);

        // Evaluate if the error rates are within acceptable limits and print the conclusion.
        if (bandwidthError > 5 || latencyError > acceptableLatencyErrorRate) {
            System.out.println(
                    "\nError: Error rate exceeds " + acceptableLatencyErrorRate + "% for Latency or 5% for Bandwidth.");
            System.out.println("Error: The test from " + sourcePeer + " to " + targetPeer + " should be repeated.");
        } else {
            System.out.println("\nInfo: Error rate is below " + acceptableLatencyErrorRate
                    + "% for Latency and 5% for Bandwidth.");
            System.out.println("Info: The test from " + sourcePeer + " to " + targetPeer + " is successful.");
        }

        // Pause to ensure the message is read before proceeding
        Thread.sleep(2000);
    }

    /**
     * Runs a series of network tests using connection information provided.
     * 
     * @param connectionInfos A list of ConnectionInfo objects representing the connections to be tested.
     * @throws InterruptedException If thread interruption occurs during the tests.
     */
    public void runTests(List<ConnectionInfo> connectionInfos) throws InterruptedException {
        iperfAndPingTests(connectionInfos);
    }
}
