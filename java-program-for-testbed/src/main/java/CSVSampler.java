import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * The CSVSampler class samples data from a CSV file based on specified criteria
 * and writes the sampled data to a new file.
 * It is designed to selectively sample data for different technologies based on given percentages.
 * 
 * @author Özcan Karaca
 */
public class CSVSampler {

    /**
     * Samples data from a CSV file based on the specified criteria and writes the
     * sampled data to an output file.
     * 
     * @param pathToCSV       The path to the source CSV file.
     * @param pathToCSVReduced    The path to the output file for the sampled data.
     * @param sampleSize      The total number of samples to be taken.
     * @param adslPercentage  The percentage of samples to be taken for ADSL technology.
     * @param cablePercentage The percentage of samples to be taken for Cable technology.
     * @param fiberPercentage The percentage of samples to be taken for Fiber technology.
     */
    public static void sampleCsvData(String pathToCSV, String pathToCSVReduced, int sampleSize,
            double adslPercentage, double cablePercentage, double fiberPercentage) {
        // Initialize CSVReader and BufferedWriter for reading and writing files
        try (CSVReader reader = new CSVReader(new FileReader(pathToCSV));
                BufferedWriter writer = new BufferedWriter(new FileWriter(pathToCSVReduced))) {

            // Lists to store all rows and sampled rows from the CSV file
            List<String[]> allRows = new ArrayList<>();
            List<String[]> sampledRows = new ArrayList<>();
            String[] nextLine;
            String[] headers = reader.readNext(); // Read headers

            // Finding column indices for technology and other relevant columns
            int indexTechnology = findColumnIndex(headers, "Technology");

            // Read all rows from the CSV file
            while ((nextLine = reader.readNext()) != null) {
                allRows.add(nextLine);
            }

            // Print the total number of rows read
            System.out.println("Info: Total rows read: " + allRows.size());

            // Calculate the count of each technology
            int adslCount = 0;
            int cableCount = 0;
            int fiberCount = 0;

            for (String[] row : allRows) {
                String tech = row[indexTechnology];
                if ("ADSL".equalsIgnoreCase(tech))
                    adslCount++;
                else if ("Cable".equalsIgnoreCase(tech))
                    cableCount++;
                else if ("FTTC".equalsIgnoreCase(tech))
                    fiberCount++;
            }

            // Print counts of each technology type
            System.out.println("Available ADSL: " + adslCount);
            System.out.println("Available Cable: " + cableCount);
            System.out.println("Available FTTC: " + fiberCount);

            // Find the indices for the required columns
            int indexUploadSpeed = findColumnIndex(headers, "Peak average maximum upload speed");
            int indexDownloadSpeed = findColumnIndex(headers, "Peak average maximum download speed");
            int indexLatency = findColumnIndex(headers, "24 hour Latency");
            int indexPacketLoss = findColumnIndex(headers, "24 hour packet loss");

            // Initialize random generator for sampling
            Random random = new Random();

            // Add samples to the sampledRows list
            sampledRows.addAll(sampleRows(allRows, random, sampleSize, adslPercentage, "ADSL", indexTechnology));
            sampledRows.addAll(sampleRows(allRows, random, sampleSize, cablePercentage, "Cable", indexTechnology));
            sampledRows.addAll(sampleRows(allRows, random, sampleSize, fiberPercentage, "FTTC", indexTechnology));

            // Calculate number of samples for each technology type
            int adslSamples = (int) (sampleSize * adslPercentage / 100.0);
            int cableSamples = (int) (sampleSize * cablePercentage / 100.0);
            int fiberSamples = sampleSize - adslSamples - cableSamples; // Remaining samples for fiber

            // Print info about number of samples for each technology
            System.out.println("Info: Samples needed - ADSL: " + adslSamples + ", Cable: " + cableSamples + ", FTTC: "
                    + fiberSamples);

            // Sample rows for each technology type
            sampleRowsTechnology(allRows, sampledRows, random, adslSamples, "ADSL", indexTechnology);
            sampleRowsTechnology(allRows, sampledRows, random, cableSamples, "Cable", indexTechnology);
            sampleRowsTechnology(allRows, sampledRows, random, fiberSamples, "FTTC", indexTechnology);

            // Writing headers for the selected columns
            writer.write(String.join(",",
                    headers[indexTechnology],
                    headers[indexUploadSpeed],
                    headers[indexDownloadSpeed],
                    headers[indexLatency],
                    headers[indexPacketLoss]));
            writer.newLine();

            // Writing sampled data to the output file and console
            for (String[] row : sampledRows) {
                String line = String.join(",",
                        row[indexTechnology],
                        row[indexUploadSpeed],
                        row[indexDownloadSpeed],
                        row[indexLatency],
                        row[indexPacketLoss]);

                writer.write(line);
                writer.newLine();

                // Output the line to the console
                System.out.println(line);
            }

            writer.flush();
            System.out.println("Success: Sampling completed and data written to " + pathToCSVReduced);

        } catch (IOException | CsvValidationException e) {
            e.printStackTrace();
        }
    }

    /**
     * Samples a specified number of rows for a given technology.
     * 
     * @param allRows               The list of all rows from the CSV file.
     * @param random                An instance of Random for generating random numbers.
     * @param sampleSize            The total number of samples to be taken.
     * @param percentage            The percentage of samples for the specific technology.
     * @param technology            The technology for which sampling is to be done.
     * @param technologyColumnIndex The column index for the technology in the CSV file.
     * @return A list of sampled rows for the specified technology.
     */
    private static List<String[]> sampleRows(List<String[]> allRows, Random random, int sampleSize,
            double percentage, String technology, int technologyColumnIndex) {
        List<String[]> rowsForTechnology = new ArrayList<>();
        List<String[]> sampled = new ArrayList<>();

        // Loop through all rows and add those matching the specified technology to a list
        for (String[] row : allRows) {
            if (row[technologyColumnIndex].equalsIgnoreCase(technology)) {
                rowsForTechnology.add(row);
            }
        }

        // Calculate the number of samples needed based on the total sample size and the percentage
        int samplesNeeded = (int) (sampleSize * percentage / 100.0);

        // Randomly select rows from the list until the required number of samples is reached
        for (int i = 0; i < samplesNeeded && !rowsForTechnology.isEmpty(); i++) {
            int randomIndex = random.nextInt(rowsForTechnology.size());
            sampled.add(rowsForTechnology.remove(randomIndex));
        }

        return sampled;
    }

    /**
     * Samples a specific number of rows from the dataset for a given technology.
     * This method selects random rows corresponding to the specified technology and
     * adds them to the list of sampled rows.
     *
     * @param allRows               The list of all rows read from the CSV file.
     * @param sampledRows           The list to store the sampled rows.
     * @param random                An instance of Random for generating random indices.
     * @param sampleCount           The number of samples needed for the specified technology.
     * @param technology            The name of the technology for which samples are being taken.
     * @param technologyColumnIndex The index of the column that contains the technology information.
     */
    private static void sampleRowsTechnology(List<String[]> allRows, List<String[]> sampledRows,
            Random random, int sampleCount, String technology, int technologyColumnIndex) {
        List<String[]> rowsForTechnology = new ArrayList<>();

        // Loop through all rows and add those matching the specified technology to a list
        for (String[] row : allRows) {
            if (row[technologyColumnIndex].equalsIgnoreCase(technology)) {
                rowsForTechnology.add(row);
            }
        }

        // Check if there is enough data; adjust the number of samples if necessary
        if (rowsForTechnology.size() < sampleCount) {
            System.out.println("Error: Not enough data for " + technology + ". Only " + rowsForTechnology.size()
                    + " samples will be used.");
            sampleCount = rowsForTechnology.size();
        }

        // Add randomly selected rows to the sampledRows list until the desired number of samples is reached
        while (sampledRows.size() < sampleCount && !rowsForTechnology.isEmpty()) {
            int randomIndex = random.nextInt(rowsForTechnology.size());
            sampledRows.add(rowsForTechnology.remove(randomIndex));
        }

        System.out.println("Info: Sampled " + technology + ": " + sampleCount);
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
        throw new IllegalArgumentException("Error: Column '" + columnName + "' not found");
    }

    /**
     * Main method to execute the CSV sampling process.
     * 
     * @param args Command-line arguments (not used).
     */
    public static void main(String[] args) {

        // Get the user's home directory path
        String homeDirectory = System.getProperty("user.home");
        // Define the base path for the master thesis's directory
        String basePath = homeDirectory + "/Desktop/master-thesis-ozcankaraca";
        // Specify the path to the CSV file to be sampled
        String pathToCSV = basePath + "/data-for-testbed/data-for-realnetwork/fixed-broadband-speeds-august-2019-data-25.csv";
        // Specify the path for the output CSV file with reduced sample
        String pathToCSVReduced = basePath + "/data-for-testbed/data-for-realnetwork/reduced-sample.csv";

        // Set the sample size and the percentage for each technology type for 2019
        int sampleSize = 360;
        double adslPercentage = 77.30;
        double cablePercentage = 19.70;
        double fiberPercentage = 3.00;

        // Call the sampleCsvData method with the specified parameters
        sampleCsvData(pathToCSV, pathToCSVReduced, sampleSize, adslPercentage, cablePercentage, fiberPercentage);
    }
}