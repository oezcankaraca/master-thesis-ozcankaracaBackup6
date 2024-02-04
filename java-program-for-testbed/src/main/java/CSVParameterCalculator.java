import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * The CSVParameterCalculator class is designed to read network performance data
 * from a CSV file and calculate statistical parameters.
 * It processes data for different network technologies and computes descriptive
 * statistics for various network characteristics.
 * 
 * @author Özcan Karaca
 */
public class CSVParameterCalculator {

    /**
     * Reads network performance data from a CSV file and calculates descriptive
     * statistics for each technology and characteristic.
     *
     * @param pathToCSV The file path to the CSV file containing the real network data.
     * @return A map with technology as the key and another map as the value, which
     *         contains network characteristics and their statistics.
     */
    public static Map<String, Map<String, DescriptiveStatistics>> calculateParameters(String pathToCSV) {
        Map<String, Map<String, DescriptiveStatistics>> statsMap = new HashMap<>();

        try (CSVReader reader = new CSVReader(new FileReader(pathToCSV))) {
            String[] headers = reader.readNext(); // Read the column headers from the CSV

            // Initialize maps to hold statistical data for different technologies
            for (String tech : new String[] { "ADSL", "CABLE", "FTTC" }) { // Use uppercase for uniformity
                Map<String, DescriptiveStatistics> techStats = new HashMap<>();
                for (String characteristic : new String[] { "maxUpload", "maxDownload", "latency", "loss" }) {
                    techStats.put(characteristic, new DescriptiveStatistics());
                }
                statsMap.put(tech, techStats);
            }

            String[] nextLine;

            // Process each row and add to the statistics
            while ((nextLine = reader.readNext()) != null) {
                // Convert to uppercase
                String technology = nextLine[findColumnIndex(headers, "Technology")].toUpperCase();

                // Skip rows with unrecognized technology
                if (!statsMap.containsKey(technology)) {
                    System.out.println("Error: Technology not recognized - " + technology);
                    continue; // Skip this row
                }

                // Add data to statistics for each characteristic
                statsMap.get(technology).get("maxUpload").addValue(
                        Double.parseDouble(nextLine[findColumnIndex(headers, "Peak average maximum upload speed")]));
                statsMap.get(technology).get("maxDownload").addValue(
                        Double.parseDouble(nextLine[findColumnIndex(headers, "Peak average maximum download speed")]));
                statsMap.get(technology).get("latency")
                        .addValue(Double.parseDouble(nextLine[findColumnIndex(headers, "24 hour Latency")]));
                statsMap.get(technology).get("loss")
                        .addValue(Double.parseDouble(nextLine[findColumnIndex(headers, "24 hour packet loss")]));
            }
        } catch (IOException | CsvValidationException e) {
            e.printStackTrace();
        }
        return statsMap;
    }

    /**
     * Finds the index of a specific column in the CSV file headers.
     * 
     * @param headers    Array of header names.
     * @param columnName Name of the column to find.
     * @return The index of the column in the headers array.
     */
    private static int findColumnIndex(String[] headers, String columnName) {
        for (int i = 0; i < headers.length; i++) {
            if (headers[i].equalsIgnoreCase(columnName)) {
                return i;
            }
        }
        throw new IllegalArgumentException("Error: Column '" + columnName + "' not found in the headers.");
    }

    /**
     * The main method to execute the CSVParameterCalculator.
     * It reads the CSV file, processes the data, and prints out calculated statistical parameters.
     * 
     * @param args Command-line arguments (not used).
     */
    public static void main(String[] args) {

        // Get the user's home directory path
        String homeDirectory = System.getProperty("user.home");
        // Define the base path for the master thesis's directory
        String basePath = homeDirectory + "/Desktop/master-thesis-ozcankaraca";
        // Specify the path to the CSV file to be sampled
        String pathToCSV = basePath + "/data-for-realnetwork/reduced-sample.csv";
        
        //Get the calculated parameters
        Map<String, Map<String, DescriptiveStatistics>> parameters = calculateParameters(pathToCSV);

        // Print out the parameters for each technology in a specific order
        for (Map.Entry<String, Map<String, DescriptiveStatistics>> entry : parameters.entrySet()) {
            System.out.println("--Technology: " + entry.getKey() + "--");
           
            System.out.println("Characteristic: Max Upload Speed");
            System.out.println("Mean: " + entry.getValue().get("maxUpload").getMean());
            System.out.println("Standard Deviation: " + entry.getValue().get("maxUpload").getStandardDeviation() + "\n");

            System.out.println("Characteristic: Max Download Speed");
            System.out.println("Mean: " + entry.getValue().get("maxDownload").getMean());
            System.out.println("Standard Deviation: " + entry.getValue().get("maxDownload").getStandardDeviation() + "\n");

            System.out.println("Characteristic: Latency");
            System.out.println("Mean: " + entry.getValue().get("latency").getMean());
            System.out.println("Standard Deviation: " + entry.getValue().get("latency").getStandardDeviation() + "\n");

            System.out.println("Characteristic: Packet Loss");
            System.out.println("Mean: " + entry.getValue().get("loss").getMean());
            System.out.println("Standard Deviation: " + entry.getValue().get("loss").getStandardDeviation() + "\n");
        }
    }
}