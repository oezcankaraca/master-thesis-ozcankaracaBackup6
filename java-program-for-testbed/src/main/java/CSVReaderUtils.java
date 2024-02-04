import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * The PeerStats class stores network performance statistics for a peer.
 * It includes maximum upload and download speeds, latency, and packet loss.
 * 
 * @author Özcan Karaca
 */
class PeerStats {
    int maxUpload;
    int maxDownload;
    double latency;
    double packetLoss;

    /**
     * Constructs PeerStats with given network parameters.
     *
     * @param maxUpload   Maximum upload speed in Mbps, converted to Kbps.
     * @param maxDownload Maximum download speed in Mbps, converted to Kbps.
     * @param latency     Latency in milliseconds.
     * @param packetLoss  Packet loss percentage.
     */
    public PeerStats(double maxUpload, double maxDownload, double latency, double packetLoss) {
        // Convert Mbps to Kbps and cast to int
        this.maxUpload = (int) (maxUpload * 1000);
        this.maxDownload = (int) (maxDownload * 1000);
        this.latency = latency;
        this.packetLoss = packetLoss;
    }

    @Override
    public String toString() {
        return String.format("Max Upload: %d Kbps, Max Download: %d Kbps, Latency: %.2f ms, Packet Loss: %.4f%%",
                maxUpload, maxDownload, latency, packetLoss);
    }
}

/**
 * The CSVReaderUtils class processes a CSV file containing network performance
 * data and extracts statistics for each peer.
 * It supports reading data, calculating statistics, and writing them to a file.
 */
public class CSVReaderUtils {

    private static final Map<String, PeerStats> peerStatsMap = new HashMap<>();

    /**
     * Reads network data from a CSV file, calculates statistics for each peer, and
     * writes them to an output file.
     * 
     * @param pathToCSV     Path to the input CSV file.
     * @param pathToOutput  Path to the output file for writing statistics.
     * @param numberOfPeers Number of peers to process.
     */
    public static void readCsvDataAndWriteToFile(String pathToCSV, String pathToOutput, int numberOfPeers) {
        System.out.println(
                "Info: Extracting real network data for " + numberOfPeers + " peers and the lectureStudioServer.");
        try (CSVReader reader = new CSVReader(new FileReader(pathToCSV));
                BufferedWriter writer = new BufferedWriter(new FileWriter(pathToOutput))) {

            String[] nextLine;
            String[] headers = reader.readNext();
            System.out.println("Info: Reading CSV headers");

            // Find column indices for required headers
            int indexMaximumDownloadSpeed = -1;
            int indexMaximumUploadSpeed = -1;
            int indexLatency = -1;
            int indexPacketLoss = -1;

            for (int i = 0; i < headers.length; i++) {
                String header = headers[i].trim();
                switch (header) {
                    case "Peak average maximum upload speed":
                        indexMaximumUploadSpeed = i;
                        break;
                    case "Peak average maximum download speed":
                        indexMaximumDownloadSpeed = i;
                        break;
                    case "24 hour Latency":
                        indexLatency = i;
                        break;
                    case "24 hour packet loss":
                        indexPacketLoss = i;
                        break;
                }
            }

            if (indexMaximumUploadSpeed == -1 || indexMaximumDownloadSpeed == -1 ||
                    indexLatency == -1 || indexPacketLoss == -1) {
                throw new IllegalArgumentException(
                        "Error: One or more required columns were not found in the header.");
            }

            System.out.println("Info: Column indices identified. Starting to process each row");

            // Process each row and create PeerStats objects
            for (int peerIndex = 1; peerIndex <= numberOfPeers + 1; peerIndex++) {
                if ((nextLine = reader.readNext()) == null)
                    break;

                try {
                    Double maximumUploadSpeed = Double.parseDouble(nextLine[indexMaximumUploadSpeed]);
                    Double maximumDownloadSpeed = Double.parseDouble(nextLine[indexMaximumDownloadSpeed]);
                    Double latency = Double.parseDouble(nextLine[indexLatency]);
                    Double packetLoss = Double.parseDouble(nextLine[indexPacketLoss]);

                    PeerStats stats = new PeerStats(maximumUploadSpeed, maximumDownloadSpeed, latency, packetLoss);
                    String peerId = peerIndex <= numberOfPeers ? String.valueOf(peerIndex) : "lectureStudioServer";

                    peerStatsMap.put(peerId, stats);

                    String statsString = peerId + ": " + stats.toString() + "\n";
                    writer.write(statsString);

                    // Output to console
                    System.out.print(statsString);

                } catch (NumberFormatException e) {
                    System.out.println("Error: Error parsing numeric data: " + e.getMessage());
                }
            }

            writer.flush();
            System.out.println("Info: All data successfully written to " + pathToOutput);

        } catch (IOException | CsvValidationException e) {
            System.out.println("Error: Exception encountered - " + e.getMessage());
            e.printStackTrace();
        }

    }

    /**
     * Retrieves the PeerStats for a given peer ID.
     * 
     * @param peerId The ID of the peer whose statistics are to be retrieved.
     * @return PeerStats object containing the network statistics.
     */
    public static PeerStats getPeerStats(String peerId) {
        return peerStatsMap.get(peerId);
    }

    private static int numberOfPeers = 10;

    /**
     * Main method to start the connection analysis.
     * 
     * @param args Command-line arguments, which can include the number of peers.
     * @throws IOException If there is an error in reading the input data or writing
     *                     to the output file.
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

        // Get the user's home directory path
        String homeDirectory = System.getProperty("user.home");
        // Define the base path for the master thesis's directory
        String basePath = homeDirectory + "/Desktop/master-thesis-ozcankaraca";
        // Specify the path to the CSV file
        String pathToCSV = basePath + "/data-for-testbed/data-for-realnetwork/fixed-broadband-speeds-august-2019-data-25.csv";
        // Define the path to store the network statistics
        String pathToNetworkStatistics = basePath + "/data-for-testbed/results/network-statistics/network-statistics-50.txt";

        readCsvDataAndWriteToFile(pathToCSV, pathToNetworkStatistics, numberOfPeers);
        System.out.println("Success: Network statistics have been written to the file: " + pathToNetworkStatistics + "\n");

        // Retrieve and display statistics for a specific peer
        String peerToRetrieve = "lectureStudioServer";
        PeerStats stats = getPeerStats(peerToRetrieve);

        if (stats != null) {
            System.out.println("Info: Data for Peer " + peerToRetrieve + ": " + stats);
        } else {
            System.out.println("Error: No data found for Peer " + peerToRetrieve);
        }

    }
}