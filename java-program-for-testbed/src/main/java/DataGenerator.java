import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.distribution.RealDistribution;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * The DataGenerator class is designed to simulate network data for a specified number of peers,
 * including technology type, upload/download speeds, latency, and packet loss.
 * It generates statistical distributions for each network technology and produces random network statistics.
 * 
 * @author Özcan Karaca
 */
public class DataGenerator {

    private Random random = new Random();

    // Defined percentages for each technology type
    private final double adslPercentage = 77.30;
    private final double cablePercentage = 19.70;
    private final double fiberPercentage = 3.00;

    // Normal distributions for various network characteristics for each technology
    private NormalDistribution adslUploadDistribution;
    private NormalDistribution cableUploadDistribution;
    private NormalDistribution fttcUploadDistribution;

    private NormalDistribution adslDownloadDistribution;
    private NormalDistribution cableDownloadDistribution;
    private NormalDistribution fttcDownloadDistribution;

    private NormalDistribution adslLatencyDistribution;
    private NormalDistribution cableLatencyDistribution;
    private NormalDistribution fttcLatencyDistribution;

    private NormalDistribution adslLossDistribution;
    private NormalDistribution cableLossDistribution;
    private NormalDistribution fttcLossDistribution;

    /**
     * Constructs a DataGenerator instance for the specified number of peers.
     * Initializes normal distributions for network characteristics based on statistical data.
     *
     * @param numberOfPeers The number of peers for which network data is to be  generated.
     */
    public DataGenerator(int numberOfPeers) {
        DataGenerator.numberOfPeers = numberOfPeers;

        // Initialization of normal distributions with mean and standard deviation values
        adslUploadDistribution = new NormalDistribution(0.8241263021582734, 0.21124587974728493);
        cableUploadDistribution = new NormalDistribution(18.612462057142857, 11.386316445471635);
        fttcUploadDistribution = new NormalDistribution(13.7526504, 5.233485819565032);

        adslDownloadDistribution = new NormalDistribution(9.489131670827337, 5.811595717123024);
        cableDownloadDistribution = new NormalDistribution(211.760197609, 106.11755346760694);
        fttcDownloadDistribution = new NormalDistribution(52.611914328, 17.76856566435048);

        adslLatencyDistribution = new NormalDistribution(25.5033015573741, 9.71303335021941);
        cableLatencyDistribution = new NormalDistribution(17.643558222285716, 2.341692489398925);
        fttcLatencyDistribution = new NormalDistribution(12.959799725, 5.467801480564891);

        adslLossDistribution = new NormalDistribution(0.001967985611510791, 0.0047487657799690644);
        cableLossDistribution = new NormalDistribution(0.0026428571428571425, 0.010508706830750317);
        fttcLossDistribution = new NormalDistribution(5.0E-4, 7.378647873726219E-4);
    }

    /**
     * Randomly selects a technology type based on predefined percentages.
     *
     * @return The name of the selected technology.
     */
    private String selectTechnology() {
        double techRoll = random.nextDouble() * 100;

        // Logic to select technology based on random roll and predefined percentages
        if (techRoll <= adslPercentage) {
            return "ADSL";
        } else if (techRoll <= adslPercentage + cablePercentage) {
            return "Cable";
        } else if (techRoll <= adslPercentage + cablePercentage + fiberPercentage) {
            return "FTTC";
        } else {
            throw new IllegalStateException("Error: Technology roll out of range");
        }
    }

    /**
     * Represents the network statistics for a peer.
     */
    public static class PeerStats {
        String peerId;
        int maxUpload;
        int maxDownload;
        double latency;
        double packetLoss;

        /**
         * Constructs an instance of PeerStats with specified network characteristics.
         * This constructor initializes the PeerStats object with network parameters for a peer,
         * including its identifier, maximum upload and download speeds, latency, and packet loss.
         * Speeds are converted from Mbps to Kbps during initialization.
         *
         * @param peerId      The identifier of the peer, which can be a numeric ID or a
         *                    special identifier like "lectureStudioServer".
         * @param maxUpload   The maximum upload speed in Mbps. This value is converted to Kbps.
         * @param maxDownload The maximum download speed in Mbps. This value is also converted to Kbps.
         * @param latency     The network latency in milliseconds.
         * @param packetLoss  The packet loss rate as a percentage.
         */
        public PeerStats(String peerId, double maxUpload, double maxDownload, double latency, double packetLoss) {
            this.peerId = peerId;
            this.maxUpload = (int) (maxUpload * 1000);
            this.maxDownload = (int) (maxDownload * 1000);
            this.latency = latency;
            this.packetLoss = packetLoss;
        }

        public String getPeerId() {
            return peerId;
        }

        public int getMaxUpload() {
            return maxUpload;
        }

        public int getMaxDownload() {
            return maxDownload;
        }

        public double getLatency() {
            return latency;
        }

        public double getPacketLoss() {
            return packetLoss;
        }

        @Override
        public String toString() {
            return String.format(
                    "%s: Max Upload: %d Kbps, Max Download: %d Kbps, Latency: %.2f ms, Packet Loss: %.4f%%",
                    peerId, maxUpload, maxDownload, latency, packetLoss);
        }
    }

    /**
     * Generates simulated network data for each peer based on statistical distributions.
     *
     * @return A list of PeerStats objects containing simulated network statistics for each peer.
     */
    public List<PeerStats> generateNetworkData() {
        List<PeerStats> peerStatsList = new ArrayList<>();
        for (int i = 1; i <= numberOfPeers; i++) { // Aktualisiert um nur für die Anzahl der Peers zu iterieren
            String peerId = String.valueOf(i);
            String technology = selectTechnology();

            // Logic to assign distributions based on the selected technology
            RealDistribution uploadDistribution;
            RealDistribution downloadDistribution;
            RealDistribution latencyDistribution;
            RealDistribution lossDistribution;

            switch (technology) {
                case "ADSL":
                    uploadDistribution = adslUploadDistribution;
                    downloadDistribution = adslDownloadDistribution;
                    latencyDistribution = adslLatencyDistribution;
                    lossDistribution = adslLossDistribution;
                    break;

                case "Cable":
                    uploadDistribution = cableUploadDistribution;
                    downloadDistribution = cableDownloadDistribution;
                    latencyDistribution = cableLatencyDistribution;
                    lossDistribution = cableLossDistribution;
                    break;

                case "FTTC":
                    uploadDistribution = fttcUploadDistribution;
                    downloadDistribution = fttcDownloadDistribution;
                    latencyDistribution = fttcLatencyDistribution;
                    lossDistribution = fttcLossDistribution;
                    break;

                default:
                    throw new IllegalStateException("Error: Unexpected technology: " + technology);
            }

            // Sampling network characteristics from the assigned distributions
            double uploadSpeed, downloadSpeed, latency, packetLoss;

            do {
                uploadSpeed = Math.max(uploadDistribution.sample(), 0);
            } while (uploadSpeed == 0);

            do {
                downloadSpeed = Math.max(downloadDistribution.sample(), 0);
            } while (downloadSpeed == 0);

            latency = Math.max(latencyDistribution.sample(), 0);
            packetLoss = Math.max(lossDistribution.sample(), 0);

            PeerStats stats = new PeerStats(peerId, uploadSpeed, downloadSpeed, latency, packetLoss);
            peerStatsList.add(stats);
        }
        return peerStatsList;
    }

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
        
        DataGenerator dataGenerator = new DataGenerator(numberOfPeers);
        List<DataGenerator.PeerStats> generatedData = dataGenerator.generateNetworkData();

        // Get the user's home directory path
        String homeDirectory = System.getProperty("user.home");
        // Define the base path for the master thesis's directory
        String basePath = homeDirectory + "/Desktop/master-thesis-ozcankaraca";
        // Specify the path to the network statistics for number of peers
        String pathToOutput = basePath + "/data-for-testbed/results/network-statistics/network-statistics-" + numberOfPeers + ".txt";

        // Initialize BufferedWriter for writing files
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(pathToOutput))) {
            System.out.println("Info: Generate simulated network data for " + numberOfPeers + " peers.");
            for (DataGenerator.PeerStats stats : generatedData) {
                String dataRow = stats.toString();
                writer.write(dataRow + "\n");
                System.out.println(dataRow);
            }
            System.out.println("Success: Simulated network data was written to the file: " + pathToOutput);
        } catch (IOException e) {
            System.err.println("Unsuccess: Error while writing the file: " + e.getMessage());
        }
    }
}