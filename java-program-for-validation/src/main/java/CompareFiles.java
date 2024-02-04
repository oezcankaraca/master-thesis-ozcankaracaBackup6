import java.io.*;
import java.security.*;
import java.util.ArrayList;
import java.util.List;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.core.DockerClientBuilder;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

/**
 * The CompareFiles class is designed to verify the integrity of files across multiple Docker containers.
 * It achieves this by comparing the SHA-256 hash values of files located in different containers against
 * a reference hash value of an original file. This class is useful in scenarios where consistency and
 * data integrity of files distributed across multiple containers need to be ensured.
 *
 * @author Özcan Karaca
 */

public class CompareFiles {

    // Static initializer block for configuring logging settings.
    static {
        BasicConfigurator.configure();
        Logger.getRootLogger().setLevel(Level.ERROR);
    }

    private static int numberOfPeers = 10;

    private static DockerClient dockerClient;

    /*
     * The main method initializes the process by defining container names, file paths,
     * and calculating the hash of an original file. It then iterates through each specified container,
     * checking if the files exist and comparing their hash values against the  original file's hash.
     * This provides detailed output about the hash comparison results, 
     * including whether the hashes match or not.
     */

    public static void main(String[] args) {

        System.out.println("\nStep Started: Comparing hash values.\n");

        // Parsing command line arguments to set the number of peers
        if (args.length > 0) {
            try {
                numberOfPeers = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.err.println("Error: Argument must be an integer. The default value of 10 is used.");
            }
        }

        // Generating file paths based on the number of peers
        List<String> containerPathsList = new ArrayList<>();
        containerPathsList.add("/app/mydocument.pdf");
        containerPathsList.add("/app/receivedMydocumentFromLectureStudioServer.pdf");

        // Add file paths for each peer
        for (int i = 1; i <= numberOfPeers; i++) {
            containerPathsList.add("/app/receivedMydocumentFrom-" + i + ".pdf");
        }

        String[] containerPaths = containerPathsList.toArray(new String[0]);

        String homeDirectory = System.getProperty("user.home");
        String basePath = homeDirectory + "/Desktop/master-thesis-ozcankaraca";

        // Local path of the source file, adjust as needed
        String SOURCE_FILE_DIR = basePath + "/data-for-testbed/mydocument.pdf";

        // Flag to track if all file hashes match
        boolean allHashesMatch = true;

        try {
            // Initialize Docker client and retrieve container names
            dockerClient = DockerClientBuilder.getInstance().build();
            List<String> containerNames = retrieveContainerNames();

            // Calculate the hash of the original file
            String originalHash = calculateFileHash(SOURCE_FILE_DIR);
            System.out.println("Info: Original File Hash: " + originalHash + "\n");

            // Check and compare hashes for each container
            for (String containerName : containerNames) {
                if (!containerName.equals("p2p-containerlab-topology-trackerPeer")
                        && !containerName.equals("p2p-containerlab-topology-prometheus")
                        && !containerName.equals("p2p-containerlab-topology-grafana")
                        && !containerName.equals("p2p-containerlab-topology-cadvisor")) {
                    allHashesMatch &= checkAndCompareHashes(containerName, containerPaths, originalHash);
                }
            }

            // Output the result of hash comparison
            if (allHashesMatch) {
                System.out.println("Info: All containers have the same file based on the hash values." + "\n");
            } else {
                System.out.println("Error: Not all containers have the same file based on the hash values." + "\n");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println("\nStep Done: Comparing hash values is done.\n");
    }

    /**
     * Retrieves names of all currently running containers.
     */
    private static List<String> retrieveContainerNames() {
        List<String> containerNames = new ArrayList<>();
        List<Container> containers = dockerClient.listContainersCmd().withShowAll(true).exec();
        for (Container container : containers) {
            containerNames.add(container.getNames()[0].substring(1));
        }
        return containerNames;
    }

    /**
     * Checks and compares the hash of files within a container against an original hash.
     * Iterates through file paths within the container, checks if each file exists,
     * then compares its hash with the original hash, stopping after finding the first existing file.
     *
     * @param containerName  The name of the container where the files are located.
     * @param containerPaths An array of paths to the files within the container.
     * @param originalHash   The hash value of the original file for comparison.
     * @return True if all found files have matching hash values; False if any file
     *         is not found or hashes do not match.
     * @throws InterruptedException
     * @throws IOException
     */
    private static boolean checkAndCompareHashes(String containerName, String[] containerPaths, String originalHash)
            throws IOException, InterruptedException {

        if (containerName.equals("p2p-containerlab-topology-trackerPeer")
                && containerName.equals("p2p-containerlab-topology-prometheus")
                && containerName.equals("p2p-containerlab-topology-grafana")
                && containerName.equals("p2p-containerlab-topology-cadvisor")) {
            return true;
        }
        boolean fileExists = false;
        boolean containerHashMatches = true;

        // Loop through each file path in the container
        for (String containerPath : containerPaths) {
            // Check if the current file exists in the container
            if (doesFileExistInContainer(containerName, containerPath)) {
                fileExists = true;
                // Get the hash of the file from the container
                System.out.println("--Container --> " + containerName + "--\n");
                String containerHash = getContainerFileHash(containerName, containerPath);
                System.out
                        .println("Info: Hash for the file " + containerPath + " in container " + containerName + ": "
                                + containerHash);

                // Compare the container file hash with the original file hash
                if (originalHash.equals(containerHash)) {
                    System.out.println("Success: The hash values match for original file: " + containerPath + "\n");
                } else {
                    System.out.println("Unsuccess: Hash values do not match for: " + containerPath + "\n");
                    containerHashMatches = false;
                }
                // Stop after finding the first file that exists
                break;
            }
        }
        System.out.println(
                "---------------------------------------------------------------------------------------------------------------------------------------------\n\n");

        // If no file was found in the container, return false
        if (!fileExists) {
            System.out.println("Error: None of the files were found in the container " + containerName);
            return false;
        }
        // Return true if all found files have matching hash values
        return containerHashMatches;
    }

    /**
     * Checks if a file exists within a Docker container.
     * Executes a command in the Docker container to check if a specified file path exists.
     *
     * @param containerName     The name of the Docker container.
     * @param containerFilePath The file path within the container to be checked.
     * @return True if the file is found; False otherwise.
     * @throws IOException
     * @throws InterruptedException
     */
    private static boolean doesFileExistInContainer(String containerName, String containerFilePath)
            throws IOException, InterruptedException {

        if (containerName.equals("p2p-containerlab-topology-centralManagementServer")) {
            return true;
        }

        String[] checkCommand = {
                "docker", "exec", containerName, "sh", "-c",
                "[ -f " + containerFilePath + " ] && echo found || echo not found"
        };

        // Execute the command to check if the file exists in the container
        Process checkProcess = Runtime.getRuntime().exec(checkCommand);
        BufferedReader checkReader = new BufferedReader(new InputStreamReader(checkProcess.getInputStream()));

        // Read the output of the command
        String checkResult = checkReader.readLine();
        checkProcess.waitFor();

        // Return true if 'found' is in the command output, indicating the file exists
        return "found".equals(checkResult);
    }

    /**
     * Calculates the SHA-256 hash of a file within a Docker container.
     * Executes a command in the container to generate the hash of the specified file.
     *
     * @param containerName     The name of the Docker container.
     * @param containerFilePath The file path within the container to be hashed.
     * @return The SHA-256 hash of the file as a String, or an error message if the process fails.
     */
    private static String getContainerFileHash(String containerName, String containerFilePath)
            throws IOException, InterruptedException {
        String[] command = {
                "docker", "exec", containerName, "sh", "-c",
                "sha256sum " + containerFilePath + " | awk '{print $1}'"
        };

        // Execute the command to calculate the file's hash
        Process process = Runtime.getRuntime().exec(command);
        BufferedReader stdInput = new BufferedReader(new InputStreamReader(process.getInputStream()));
        BufferedReader stdError = new BufferedReader(new InputStreamReader(process.getErrorStream()));

        // Read the command output to get the hash
        String line;
        StringBuilder output = new StringBuilder();

        while ((line = stdInput.readLine()) != null) {
            output.append(line).append("\n");
        }

        // Read any error messages from the command execution
        while ((line = stdError.readLine()) != null) {
            System.out.println("ERROR: " + line);
        }

        process.waitFor();
        return output.length() > 0 ? output.toString().trim() : "Error: Error in hash calculation";
    }

    /**
     * Calculates the SHA-256 hash of a local file, and reads the file and computes its SHA-256 hash.
     *
     * @param filePath The path of the file to be hashed.
     * @return The SHA-256 hash of the file as a String.
     */
    public static String calculateFileHash(String filePath) throws Exception {
        MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
        try (FileInputStream fis = new FileInputStream(filePath);
                DigestInputStream dis = new DigestInputStream(fis, sha256)) {
            byte[] buffer = new byte[8192];

            // Read the file and update the message digest
            while (dis.read(buffer) != -1) {
                // Continuously reading file for hashing
            }
            byte[] digest = sha256.digest();
            return bytesToHex(digest);
        }
    }

    /**
     * Converts a byte array into a hexadecimal string. 
     * Useful for representing hash values in a readable format.
     *
     * @param bytes The byte array to be converted.
     * @return A hexadecimal string representation of the byte array.
     */
    public static String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder(); // StringBuilder to accumulate the hex values
        
        for (byte b : bytes) { // Iterate over each byte in the array

            // Convert byte to hex, masking with 0xff to keep it positive
            String hex = Integer.toHexString(0xff & b); 
            
            if (hex.length() == 1) { // If hex string is a single digit
                hexString.append('0'); // Append '0' to make it two digits
            }
            
            hexString.append(hex); // Append the hex string for the current byte
        }
        
        return hexString.toString(); // Return the complete hex string
    }
}