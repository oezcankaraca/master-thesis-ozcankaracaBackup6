-----------------------------------------Generating Real Network Data------------------------------------------

The combination of the three Java classes, CSVSampler, CSVParameterCalculator, and DataGenerator, forms a comprehensive process for analyzing and generating network data. Initially, CSVSampler utilizes real network data from a CSV file to create a representative sample based on predefined percentages for various technologies. CSVParameterCalculator then analyzes these data to calculate statistical parameters such as means and standard deviations for various network characteristics. Finally, DataGenerator utilizes these statistical parameters to simulate realistic network data for a specified number of peers. This integrated approach enables precise analysis and effective simulation of network performance, which is crucial for planning and optimizing network topologies.

**1 - CSVSampler**
Path: java-program-for-testbed/src/main/java
The Java class called CSVSampler is designed to extract data from a CSV file based on specific criteria and save this data in a new file. Its main functions and processes are as follows:

Data Sampling: This class includes sampleCsvData, which reads a CSV file and creates a data sample based on predefined percentages for different technologies (ADSL, cable, fiber). The method takes the path to the CSV source file, the path to the output file, the total sample size, and the percentages for each technology as parameters.

Data Processing: It reads all rows of the CSV file, counts the availability of different technologies, and then selects a representative sample of rows based on the specified percentages. This involves finding the relevant column indices and using a random generator to obtain a random sample.

Data Output: After the samples are drawn, the method writes the selected data to a new CSV file, including relevant information such as technology type, upload speed, download speed, latency, and packet loss for each row.

Utility Methods: The class also includes additional utility methods like sampleRows, which draws a sample for a specific technology from the total data, and sampleRowsTechnology, which selects a specific number of rows for a given technology. Another method, findColumnIndex, is used to find the index of a specific column in the header rows of the CSV file.

Main Method: The main method of the class serves as the entry point for running the program. It sets the paths for the input and output files, determines the sample size, and specifies the percentages for each technology from 2019. It then calls the sampleCsvData method to initiate the sampling process.

**2 - CSVParameterCalculator**
Path: java-program-for-testbed/src/main/java
The Java class CSVParameterCalculator is designed to read network performance data from a reduced CSV file (generated from the CSVSampler class) and calculate statistical parameters for various network technologies. Here is a detailed description of how it works and its main components:

Calculation of Statistical Parameters: The main functionality of the class resides in the calculateParameters method. This method reads network performance data from a given CSV file. It computes descriptive statistics (such as mean and standard deviation) for various network features (e.g., upload speed, download speed, latency, packet loss) for each network technology (ADSL, CABLE, FTTC).

Data Processing and Analysis: The method uses a loop to process each row when reading data from the CSV file. It extracts relevant data points for each technology and adds them to the respective DescriptiveStatistics objects. These objects collect statistical information such as sums, counts, means, standard deviations, etc.

Flexibility and Extensibility: The class uses a combination of maps and arrays to enable a flexible and extensible structure. This allows for the easy addition of additional technologies or network features by adding new entries to the arrays or maps.

Error Handling and Validation: The findColumnIndex method is used to find the index of a specific column in the header rows of the CSV file. If a column is not found, an exception is thrown. This ensures that the data is correctly read and processed.

Output of Results: The path to the CSV file is set in the main method, and the calculateParameters method is called to compute the statistics. Subsequently, the results for each technology and feature are output in a structured and readable manner.

**3 - DataGenerator**
Path: java-program-for-testbed/src/main/java
The Java class DataGenerator is designed to simulate network data for a specific number of peers. The class allows for generating various network characteristics such as technology type, upload/download speeds, latency, and packet loss for each peer. This functionality is particularly useful for creating an extensive set of simulated network data without the need to read data from a CSV file every time. Instead, the class uses means and standard deviations previously calculated from real data (e.g., using the CSVParameterCalculator class) to simulate realistic network statistics. Here is a detailed description of how it works and its main components:

Initialization and Parameter Setting: In the class constructor, the total number of peers is defined, and normal distributions for various network characteristics of each technology (ADSL, Cable, FTTC) are initialized. These distributions are set with specific means and standard deviations for upload/download speeds, latency, and packet loss.

Technology Selection: The selectTechnology method randomly selects a technology based on predefined percentages. This ensures that the generated data reflects the distribution of technologies in the real world.

Generation of Network Statistics: The generateNetworkData method generates simulated network data for each peer. For each peer, a technology is randomly chosen. Then, network characteristics such as upload/download speeds, latency, and packet loss are generated based on the corresponding normal distributions for the selected technology.

Representation of Peer Statistics: The inner class PeerStats represents the network statistics of a peer. It stores information such as peer ID, maximum upload/download speeds, latency, and packet loss. Speeds are converted from Mbps to Kbps.

Output of Generated Data: In the main method, an instance of DataGenerator is created, and simulated network data is generated for a defined number of peers. This data is then written to a file and additionally displayed in the console.


----------------------------------Generating and Analyzing Network Topology------------------------------------

The NetworkTopologyGenerator class generates a simulated network topology in a JSON file, including details like upload/download speeds, latency, and packet loss for a specified number of peers. The ConnectionAnalysis class complements it by analyzing the JSON file to ensure the network exhibits a complete mesh structure, which is crucial for network integrity and performance.

**4 - GeneratorOfNetworkTopology**
Path: java-program-for-testbed/src/main/java
The Java class GeneratorOfNetworkTopology is specifically designed to create a simulated network topology in JSON format based on data generated by the DataGenerator class. Here is an explanation of how it works:

Data Utilization from DataGenerator: The class receives network data for a specific number of peers from the DataGenerator class. This data includes network parameters such as upload and download speeds, latency, and packet loss for each peer, including a special peer named 'lectureStudioServer'.

Creation of Network Topology: GeneratorOfNetworkTopology uses this data to create a detailed topology of the network. This topology describes the connections between the peers as well as their respective network parameters.

JSON Formatting: The generated network topology is represented in a JSON document. This document contains information about individual peers and their connections, including network characteristics such as bandwidth, latency, and packet loss.

Flexibility in Application: The number of peers in the topology can be dynamically set through command-line arguments, providing flexibility in the class's application.

Creation and Storage of JSON File: After creating the network topology, the JSON document is saved in a file. This file can then be used for further analysis, simulations, or visualization of the network topology.

**5 - ConnectionAnalysis**
Path: java-program-for-testbed/src/main/java
The Java class ConnectionAnalysis is developed to analyze network connections in a network topology. Its primary purpose is to read and process network configuration data generated in a JSON file by the GeneratorOfNetworkTopology class. Here is a detailed explanation of how it operates and its role in the overall process:

Processing Data from JSON File: ConnectionAnalysis begins by reading the JSON file created by GeneratorOfNetworkTopology. This file contains detailed information about network peers and their connections, including bandwidth, latency, and packet loss.

Extraction and Segmentation of Information: The class extracts relevant data from the JSON file and divides it into manageable components. This includes bandwidth information for each peer and specific details of each network connection.

Analysis of Network Completeness: An essential part of ConnectionAnalysis functionality is to check whether the network exhibits a complete mesh structure. This means that every peer in the network should be directly connected to every other peer. The class identifies missing connections that would be required for a complete mesh structure.

Output of Analysis Results: The results of the analysis, including missing connections and other relevant information, are presented both on the console and in an output file. This provides a clear overview of the current state of the network.

Interaction with the YMLGenerator Class: While ConnectionAnalysis is primarily responsible for analyzing the network topology, the obtained information serves as a basis for later use by another class, the YMLGenerator. The YMLGenerator will use these analyzed data to create a YAML file that can be used for further network configurations in the Containerlab.


-----------------------------------------Integrating of P2P Algorithm------------------------------------------

**6 - NetworkConfigParser**
Path: java-program-for-testbed/src/main/java
The NetworkConfigParser class is designed to analyze an optimized network topology calculated by a P2P algorithm. The process unfolds as follows:

Generation of Original Data: Initially, the GeneratorOfNetworkTopology class creates a JSON file representing the original network topology. This file contains information about peers and their connections.

Optimization by P2P Algorithm: A P2P algorithm takes this JSON file generated by GeneratorOfNetworkTopology and calculates an optimized network topology. The result is a new, optimized JSON file that demonstrates a more efficient structuring and distribution of network nodes.

Analysis of the Optimized Topology: The NetworkConfigParser class reads the optimized JSON file, which is the result of the P2P algorithm. This file contains detailed information about improved peer-to-peer connections as well as super-peers.

Processing and Extraction of Network Information: Within the NetworkConfigParser class, specific network topology elements are extracted and analyzed. This includes identifying super-peers, details of peer-to-peer connections, and mapping super-peers to their connected peers.

Utilization for Further Analysis and Simulations: These processed and extracted data are valuable for scenarios where the network topology needs to be dynamically interpreted and prepared for various applications, such as simulations, and for creating Containerlab YAML files.


-----------------------------------------Creating a Containerlab File------------------------------------------
The combination of the ConnectionDetails and YMLGenerator classes forms a comprehensive approach to creating a testbed.

**7 - Connection Details**
Path: java-program-for-testbed/src/main/java
The ConnectionDetails class is designed to process network data and generate detailed connection information for each peer-to-peer connection. The process unfolds as follows:

Generation of Source Data: Firstly, the GeneratorOfNetworkTopology class generates a JSON file representing the original network topology. This file contains information about peers and their connections.

Processing by the P2P Algorithm: The P2P algorithm processes this JSON file generated by GeneratorOfNetworkTopology and calculates an optimized network topology. The result is a new, optimized JSON file that demonstrates a more efficient structuring and distribution of network nodes.

Analysis of the Optimized Topology: The ConnectionDetails class reads the optimized JSON file, the result of the P2P algorithm. This file contains detailed information about improved peer-to-peer connections as well as super-peers.

Creation of Detailed Connection Properties: The class then writes detailed connection properties to a file based on the processed data. It combines information from the P2P-optimized topology and network statistics to provide a comprehensive view of the connections between peers.

**8 - YAMLGenerator**
Path: java-program-for-testbed/src/main/java
The YMLGenerator class in Java is designed to create a YAML file that defines the network topology for a container-based network testbed. This class reads network configuration details from a JSON file and translates them into a YAML format suitable for container orchestration tools like ContainerLab. Here is a detailed explanation of the class and its functions:

Generation of Network Topologies: The YMLGenerator class leverages all the previously generated information about the network topology and the performance of network nodes. This information can come from previous steps of network data generation and optimization, such as from classes like GeneratorOfNetworkTopology or NetworkConfigParser.

Reading Configuration Details: The class reads network configuration details from a JSON file generated by previous processes. This file contains information about peers, super-peers, and the network structure.

Translation into YAML Format: The read data is translated into a YAML format suitable for use with ContainerLab. This includes assigning network interfaces, IP addresses, and environment variables for each node in the network.

Generating a Coherent Network Configuration: The class ensures that each node in the network is correctly configured to provide a coherent and functional network environment for testing and experiments.

Incorporating Additional Monitoring and Visualization Tools: There is the possibility of including additional nodes for monitoring and visualization tools like Prometheus, cAdvisor, and Grafana in the topology.

Creation of the YAML File: The YMLGenerator class generates the topology file in YAML format. This includes configurations for peers, super-peers, and the central management server. Additionally, options for additional monitoring and visualization tools can be included in the YAML configuration.
The class handles the assignment of network interfaces and IP addresses, as well as the setting of environment variables for each node in the network.

Use Case: The generated YAML file can be used in ContainerLab to build a network environment that reflects the defined topology and configuration. This is particularly useful for simulating and testing network topologies in a controlled environment.
