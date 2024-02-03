#!/bin/bash

# Define arrays for the variable values
declare -a HAS_SUPERPEER_VALUES=("true" "false")
#declare -a NUMBER_OF_PEERS_VALUES=("5" "10" "20" "35" "50" "75")
declare -a NUMBER_OF_PEERS_VALUES=("10" "20" "35" "50" "75")
declare -a CHOICE_OF_PDF_MB_VALUES=("1" "3" "5" "10" "15" "20" "30")

# Iterate over each combination of variable values
for HAS_SUPERPEER in "${HAS_SUPERPEER_VALUES[@]}"; do
    for NUMBER_OF_PEERS_ARG in "${NUMBER_OF_PEERS_VALUES[@]}"; do
        for CHOICE_OF_PDF_MB in "${CHOICE_OF_PDF_MB_VALUES[@]}"; do
            echo "Running test with HAS_SUPERPEER=$HAS_SUPERPEER, NUMBER_OF_PEERS_ARG=$NUMBER_OF_PEERS_ARG, CHOICE_OF_PDF_MB=$CHOICE_OF_PDF_MB"

# Base directory path where all project-related files are located
BASISPFAD="$HOME/Desktop"

# Paths to various components of the Java programs used in the testbed
JAVA_PROGRAM_FOR_TESTBED_PATH="$BASISPFAD/master-thesis-ozcankaraca/java-program-for-testbed/"
JAVA_PROGRAM_FOR_VALIDATION_PATH="$BASISPFAD/master-thesis-ozcankaraca/java-program-for-validation/"
JAVA_PROGRAM_FOR_CONTAINER_PATH="$BASISPFAD/master-thesis-ozcankaraca/java-program-for-container/"

# Path to the YAML file for containerlab topology configuration
CONTAINERLAB_YML="$BASISPFAD/master-thesis-ozcankaraca/java-program-for-container/src/main/java/containerlab-topology.yml"

# Paths for Docker images related to the testbed, tracker-peer, and monitoring tools
IMAGE_TESTBED_PATH="$BASISPFAD/master-thesis-ozcankaraca/java-program-for-container/"
IMAGE_TRACKER_PATH="$BASISPFAD/master-thesis-ozcankaraca/java-program-for-validation/src/main/java"
IMAGE_ANALYSING_MONITORING_PATH="$BASISPFAD/master-thesis-ozcankaraca/data-for-testbed/data-for-analysing-monitoring/"

# Class names of various Java programs used in the testbed
JAVA_PROGRAM_FOR_TESTBED_CLASS1="GeneratorOfNetworkTopology"
JAVA_PROGRAM_FOR_TESTBED_CLASS2="ConnectionAnalysis"
JAVA_PROGRAM_FOR_TESTBED_CLASS3="NetworkConfigParser"
JAVA_PROGRAM_FOR_TESTBED_CLASS4="ConnectionDetails"
JAVA_PROGRAM_FOR_TESTBED_CLASS5="YMLGenerator"
JAVA_PROGRAM_FOR_TESTBED_CLASS6="OnlyFromServerToPeers"

# Class names for the Java programs used in validation
JAVA_PROGRAM_FOR_VALIDATION_CLASS1="ConnectionQuality"
JAVA_PROGRAM_FOR_VALIDATION_CLASS2="CompareFiles"

base_path="$BASISPFAD/master-thesis-ozcankaraca/data-for-testbed/PDF/"

destination="$BASISPFAD/master-thesis-ozcankaraca/data-for-testbed/"

delete_file="$BASISPFAD/master-thesis-ozcankaraca/data-for-testbed/mydocument.pdf"

rm -f "$delete_file"

file_name="${CHOICE_OF_PDF_MB}MB.pdf"
full_path_to_file="${base_path}/${file_name}"

if [ -f "$full_path_to_file" ]; then
  
    cp "$full_path_to_file" "$destination/mydocument.pdf"
    echo "File '$file_name' was copied as 'mydocument.pdf' to '$destination'."
else
    echo "File '$file_name' was not found."
fi

sleep 30

printf "\nStarting Testbed\n\n"

testbed_and_containerlab() {

    # Navigating to the directory containing the Java program for testbed
    cd "$JAVA_PROGRAM_FOR_TESTBED_PATH"
    
    # Executing specific Java classes based on the configuration of super-peers 
    if [ "$HAS_SUPERPEER" = "false" ]; then
        echo "Executing OnlyFromServerToPeers class as HAS_SUPERPEER is set to false"
        mvn -q exec:java -Dexec.mainClass="$JAVA_PROGRAM_FOR_TESTBED_CLASS6" -Dexec.args="$NUMBER_OF_PEERS_ARG"
    fi
    
    # Additional Java classes executed as part of the testbed setup process
    #mvn -q exec:java -Dexec.mainClass="$JAVA_PROGRAM_FOR_TESTBED_CLASS1" -Dexec.args="$NUMBER_OF_PEERS_ARG"
    #sleep 5
    
    mvn -q exec:java -Dexec.mainClass="$JAVA_PROGRAM_FOR_TESTBED_CLASS2" -Dexec.args="$NUMBER_OF_PEERS_ARG"
    sleep 5
    
    mvn -q exec:java -Dexec.mainClass="$JAVA_PROGRAM_FOR_TESTBED_CLASS3" -Dexec.args="$NUMBER_OF_PEERS_ARG $HAS_SUPERPEER"
    sleep 5
    
    mvn -q exec:java -Dexec.mainClass="$JAVA_PROGRAM_FOR_TESTBED_CLASS4" -Dexec.args="$NUMBER_OF_PEERS_ARG $HAS_SUPERPEER $CHOICE_OF_PDF_MB"
    sleep 5
     
    mvn -q exec:java -Dexec.mainClass="$JAVA_PROGRAM_FOR_TESTBED_CLASS5" -Dexec.args="$NUMBER_OF_PEERS_ARG $HAS_SUPERPEER"
    sleep 5

    printf "\nStep: Generating Docker image\n"

    # Building Docker images for the testbed, tracker-peer, and monitoring tools
    cd "$IMAGE_TESTBED_PATH"
    printf "\nInfo: Creating Docker Image for Testbed\n"
    #docker build -f dockerfile.testbed -t image-testbed .
    #sleep 5
    
    cd "$IMAGE_TRACKER_PATH"
    printf "\nInfo: Creating Docker Image for tracker-peer\n"
    #docker build -f dockerfile.tracker -t image-tracker .
    #sleep 5
    
    cd "$IMAGE_ANALYSING_MONITORING_PATH"
    printf "\nInfo: Creating Docker Image for analysing and monitoring\n"
    #docker build -f dockerfile.cadvisor -t image-cadvisor .
    #sleep 5
    #docker build -f dockerfile.grafana -t image-grafana .
    #sleep 5
    #docker build -f dockerfile.prometheus -t image-prometheus .
    
    printf "\nStep: Genereating Docker image is done.\n"

    # Starting the deployment of Containerlab
    printf "\nStep: Creating Containerlab file\n"
    printf "\nInfo: Starting Containerlab\n\n"
    sudo containerlab deploy -t "$CONTAINERLAB_YML"
    sleep 5 
    printf "\nStep: Creating Containerlab file is done.\n"
}

# Function to run validation tests
run_validation() {

    # Navigate to the directory containing the Java program for validation
    cd "$JAVA_PROGRAM_FOR_VALIDATION_PATH"

    # Reading output line by line and extracting error rate metrics
    while IFS= read -r line; do
        echo "$line"
        case "$line" in
            # Extracting latency and bandwidth error rates using pattern matching
            *"Average Latency Error Rate"*)
                avg_latency_error_rate=$(echo "$line" | awk '{print $5}')
                ;;
            *"Max Latency Error Rate"*)
                max_latency_error_rate=$(echo "$line" | awk '{print $5}')
                ;;
            *"Min Latency Error Rate"*)
                min_latency_error_rate=$(echo "$line" | awk '{print $5}')
                ;;
            *"Average Bandwidth Error Rate"*)
                avg_bandwidth_error_rate=$(echo "$line" | awk '{print $5}')
                ;;
            *"Max Bandwidth Error Rate"*)
                max_bandwidth_error_rate=$(echo "$line" | awk '{print $5}')
                ;;
            *"Min Bandwidth Error Rate"*)
                min_bandwidth_error_rate=$(echo "$line" | awk '{print $5}')
                ;;
        esac
    done < <(mvn -q exec:java -Dexec.mainClass="$JAVA_PROGRAM_FOR_VALIDATION_CLASS1" -Dexec.args="$NUMBER_OF_PEERS_ARG")
}

# Executing the testbed setup and validation process
testbed_and_containerlab
#run_validation

# Check if the previous command was successful
if [ $? -eq 0 ]; then
    printf "Info: Proceeding...\n"
else
    # If the validation failed, restart the testbed and run validation 
    printf "Error: Some tests need to be repeated. Restarting the testbed and containerlab..."

    printf "Info: Destroying Containerlab and cleaning up the environment:"
    sudo containerlab destroy -t "$CONTAINERLAB_YML" --cleanup
   
    # Executing the testbed setup and validation process again
    testbed_and_containerlab
    run_validation
fi

printf "\nInfo: Validation is done.\n"

printf "\nStep: Checking Container logs\n\n"

lectureStudioServerLog=""

container_ids=$(docker ps -q)

trackerPeerId=""

max_time=0
min_time=99999999
max_time_container=""
min_time_container=""

max_connection_time=0
min_connection_time=99999999
max_connection_time_container=""
min_connection_time_container=""

max_transfer_time=0
min_transfer_time=99999999
max_transfer_time_container=""
min_transfer_time_container=""

total_connection_time=0
total_transfer_time=0
total_total_time=0

count_containers=0

all_containers_processed=true

for id in $container_ids; do
    container_name=$(sudo docker inspect --format '{{.Name}}' "$id" | sed 's/^\/\+//')  # Remove leading slashes from container name 
    
    if [[ "$container_name" == "p2p-containerlab-topology-trackerPeer" ]]; then
            trackerPeerId="$id"
            continue 
        fi
        
    if [[ "$container_name" == "p2p-containerlab-topology-grafana" || \
          "$container_name" == "p2p-containerlab-topology-prometheus" || \
          "$container_name" == "p2p-containerlab-topology-cadvisor" ]]; then
        continue 
    fi
    
    if [[ "$container_name" == "p2p-containerlab-topology-lectureStudioServer" ]]; then
        lectureStudioServerLog=$(docker logs "$id")
        continue
    fi

    while :; do
        container_logs=$(docker logs "$id")

        if echo "$container_logs" | grep -q "Total Time"; then
            echo "--Logs for Container $container_name:--"
            echo "$container_logs"
            break 
        else
            sleep 5
        fi
    done
    
    echo "-----------------------------------------------------------------------------------------------------------------------------------------------"
    echo ""
done
    
    if [[ -n "$lectureStudioServerLog" ]]; then
    echo "--Logs for Container p2p-containerlab-topology-lectureStudioServer:--"
    echo "$lectureStudioServerLog"
    echo "-----------------------------------------------------------------------------------------------------------------------------------------------"
    fi

 for id in $container_ids; do
    container_name=$(docker inspect --format '{{.Name}}' "$id" | sed 's/^\/\+//')

    if [[ "$container_name" == "p2p-containerlab-topology-lectureStudioServer" || \
          "$container_name" == "p2p-containerlab-topology-trackerPeer" || \
          "$container_name" == "p2p-containerlab-topology-cadvisor" || \
          "$container_name" == "p2p-containerlab-topology-grafana" || \
          "$container_name" == "p2p-containerlab-topology-prometheus" ]]; then
        continue
    fi

    container_logs=$(docker logs "$id")

    connection_time_line=$(echo "$container_logs" | grep "Conection Time")
    connection_time=$(echo "$connection_time_line" | grep -oP '(?<=Conection Time: )\d+')

    transfer_time_line=$(echo "$container_logs" | grep "File Transfer Time")
    transfer_time=$(echo "$transfer_time_line" | grep -oP '(?<=File Transfer Time: )\d+')

    total_time_line=$(echo "$container_logs" | grep "Total Time")
    total_time=$(echo "$total_time_line" | grep -oP '(?<=: )\d+')
    
    received_bytes_line=$(echo "$container_logs" | grep "Info: Total received bytes")

    valid_time_found=false
    
    if [[ $received_bytes_line ]]; then
        total_received_bytes=$(echo "$received_bytes_line" | grep -oP '(?<=Info: Total received bytes: )\d+')
    fi
    
    if [[ "$connection_time" =~ ^[0-9]+$ ]]; then
    total_connection_time=$((total_connection_time + connection_time))
    count_containers=$((count_containers + 1))
    fi

    if [[ "$transfer_time" =~ ^[0-9]+$ ]]; then
    total_transfer_time=$((total_transfer_time + transfer_time))
    fi

    if [[ "$total_time" =~ ^[0-9]+$ ]]; then
    total_total_time=$((total_total_time + total_time))
    fi

    if [[ "$connection_time" =~ ^[0-9]+$ ]]; then
        valid_time_found=true
        if [[ "$connection_time" -gt "$max_connection_time" ]]; then
            max_connection_time=$connection_time
            max_connection_time_container=$container_name
        fi
        if [[ "$connection_time" -lt "$min_connection_time" ]]; then
            min_connection_time=$connection_time
            min_connection_time_container=$container_name
        fi
    fi

    if [[ "$transfer_time" =~ ^[0-9]+$ ]]; then
    	valid_time_found=true
        if [[ "$transfer_time" -gt "$max_transfer_time" ]]; then
            max_transfer_time=$transfer_time
            max_transfer_time_container=$container_name
        fi
        if [[ "$transfer_time" -lt "$min_transfer_time" ]]; then
            min_transfer_time=$transfer_time
            min_transfer_time_container=$container_name
        fi
    fi

    if [[ "$total_time" =~ ^[0-9]+$ ]]; then
    	valid_time_found=true
        if [[ "$total_time" -gt "$max_time" ]]; then
            max_time=$total_time
            max_time_container=$container_name
        fi
        if [[ "$total_time" -lt "$min_time" ]]; then
            min_time=$total_time
            min_time_container=$container_name
        fi
    fi
    
    if ! $valid_time_found; then
        echo "Error: No valid Connection Time, File Transfer Time, or Total Time found in logs for $container_name"
        all_containers_processed=false
    fi
    
       if [[ "$total_time" =~ ^[0-9]+$ ]]; then
        total_total_time=$((total_total_time + total_time))
    fi
done

if [[ -n "$trackerPeerId" ]]; then
    printf "\n--Logs for Container p2p-containerlab-topology-trackerPeer:--\n\n"
    tracker_peer_logs=$(docker logs "$trackerPeerId")
    echo "$tracker_peer_logs" 

    total_duration=$(echo "$tracker_peer_logs" | grep "Result: Total duration" | awk '{print $4}')
    
    if [[ -n "$total_duration" ]]; then
        echo "Total Duration from Tracker Peer: $total_duration ms"
    else
        echo "Total Duration not found in Tracker Peer Logs"
    fi

    printf "\n----------------------------------------------------------------------------------------------------------------------------------------"
fi

if [ $count_containers -gt 0 ]; then
    avg_connection_time=$((total_connection_time / count_containers))
    avg_transfer_time=$((total_transfer_time / count_containers))
    avg_total_time=$(( (total_total_time / count_containers) / 2 ))
else
    printf "\nNo valid data for average time calculations.\n"
fi

if $all_containers_processed; then
    printf "\n--Results related to Connection, File Transfer and Total Time:--\n\n"
    
    printf "\nMax Values:\n"
    printf "Max Connection Time: $max_connection_time ms in Container $max_connection_time_container\n"
    printf "Max File Transfer Time: $max_transfer_time ms in Container $max_transfer_time_container\n"
    printf "Max Total Time: $max_time ms in Container $max_time_container\n"
    
    printf "\nMin Values:\n"
    printf "Min Connection Time: $min_connection_time ms in Container $min_connection_time_container\n"
    printf "Min File Transfer Time: $min_transfer_time ms in Container $min_transfer_time_container\n"
    printf "Min Total Time: $min_time ms in Container $min_time_container\n" 
    
    printf "\nAverage Values:\n"
    printf "Average Connection Time: $avg_connection_time ms\n"
    printf "Average File Transfer Time: $avg_transfer_time ms\n"
    printf "Average Total Time: $avg_total_time ms\n"
else
    echo "Error: Not all containers were successfully processed"
fi

printf "\n----------------------------------------------------------------------------------------------------------------------------------------\n"

printf "\nInfo: Container check completed. Cleaning up the environment.\n"
printf "\n**10.STEP IS DONE.**\n"
#sleep 20 

#cd "$JAVA_PROGRAM_FOR_VALIDATION_PATH"
#all_containers_have_file=false

#while IFS= read -r line; do
#    echo "$line" 
#    if [[ "$line" == "Info: All containers have the same file based on the hash values." ]]; then
#        all_containers_have_file=true
#    fi
#done < <(mvn -q exec:java -Dexec.mainClass="$JAVA_PROGRAM_FOR_VALIDATION_CLASS2" -Dexec.args="$NUMBER_OF_PEERS_ARG")
#sleep 5

printf "\nStep: Cleaning up the Testbed\n"

# Destroying the Containerlab setup and cleaning up the environment
printf "\nInfo: Destroying Containerlab and cleaning up the environment.\n\n"
sudo containerlab destroy -t "$CONTAINERLAB_YML" --cleanup

# Waiting for a short period to ensure all containers are stopped
printf "\nInfo: Waiting for all Containers to stop.\n"
sleep 5

# Checking if any containers using the 'image-testbed' image are still running
if [ -z "$(docker ps -q --filter ancestor=image-testbed)" ]; then
    printf "Info: All Containers have stopped.\n"
    echo ""
    echo "Deleting Docker image:"
    echo ""
    #docker image rm image-testbed
    #docker image rm image-tracker
    #docker image rm image-cadvisor
    #docker image rm image-grafana
    #docker image rm image-prometheus
    printf "\nInfo: Docker image successfully deleted."
else
    echo "Info: There are still running Containers. Cannot delete Docker Image."
fi

# Removing percentage signs and replacing commas with dots for error rates
max_latency_error_rate=$(echo "$max_latency_error_rate" | tr -d '%')
min_latency_error_rate=$(echo "$min_latency_error_rate" | tr -d '%')
avg_latency_error_rate=$(echo "$avg_latency_error_rate" | tr -d '%')
max_bandwidth_error_rate=$(echo "$max_bandwidth_error_rate" | tr -d '%')
min_bandwidth_error_rate=$(echo "$min_bandwidth_error_rate" | tr -d '%')
avg_bandwidth_error_rate=$(echo "$avg_bandwidth_error_rate" | tr -d '%')

# Formatting error rates for consistency
max_latency_error_rate=${max_latency_error_rate//,/\.}
min_latency_error_rate=${min_latency_error_rate//,/\.}
avg_latency_error_rate=${avg_latency_error_rate//,/\.}
max_bandwidth_error_rate=${max_bandwidth_error_rate//,/\.}
min_bandwidth_error_rate=${min_bandwidth_error_rate//,/\.}
avg_bandwidth_error_rate=${avg_bandwidth_error_rate//,/\.}

# Path for the file storing the test ID counter
TEST_ID_FILE="$BASISPFAD/master-thesis-ozcankaraca/data-for-testbed/results/test_id_counter3.txt"

# Incrementing the test ID for each run, or starting at 1 if the file doesn't exist
if [ -f "$TEST_ID_FILE" ]; then
    test_id=$(<"$TEST_ID_FILE")
    test_id=$((test_id+1)) 
else
    test_id=1  
fi

# Updating the test ID counter file
echo "$test_id" > "$TEST_ID_FILE"

# Defining function to format time values
format_time() {
    local time_value=$1
    # If the time is less than 1 second, prepend a '0' to maintain decimal format
    if (( $(bc <<< "$time_value < 1") )); then
        echo "0$time_value"
    else
        echo "$time_value"
    fi
}

# Function to calculate and format time from milliseconds to seconds
calculate_and_format_time() {
    local time_ms=$1
    local time_sec
    # Check if the time in milliseconds is provided
    if [[ -n "$time_ms" ]]; then
        # Convert time from milliseconds to seconds and format it
        time_sec=$(bc <<< "scale=2; $time_ms/1000")
        format_time "$time_sec"
    else
        echo "N/A" # Return 'N/A' if no time value is provided
    fi
}

# Calculating and formatting all time measurements
max_connection_time_sec=$(calculate_and_format_time "$max_connection_time")
min_connection_time_sec=$(calculate_and_format_time "$min_connection_time")
avg_connection_time_sec=$(calculate_and_format_time "$avg_connection_time")
max_transfer_time_sec=$(calculate_and_format_time "$max_transfer_time")
min_transfer_time_sec=$(calculate_and_format_time "$min_transfer_time")
avg_transfer_time_sec=$(calculate_and_format_time "$avg_transfer_time")
max_total_time_sec=$(calculate_and_format_time "$max_time")
min_total_time_sec=$(calculate_and_format_time "$min_time")
avg_total_time_sec=$(calculate_and_format_time "$avg_total_time")
total_duration_sec=$(calculate_and_format_time "$total_duration")

# Displaying all calculated results
printf "\nAll Results:\n"

echo "TestID: Test$test_id"
echo "Number of Peers: $(($NUMBER_OF_PEERS_ARG + 1))"
echo "With Super-Peers: $HAS_SUPERPEER"
echo "All containers have the same PDF file: $all_containers_have_file"
echo "Total Received Bytes: $total_received_bytes Bytes"

echo "Minimum Connection Time: $min_connection_time_sec s"
echo "Avarage Connection Time: $avg_connection_time_sec s"
echo "Maximum Connection Time: $max_connection_time_sec s"

echo "Minimum Transfer Time: $min_transfer_time_sec s"
echo "Avarage Transfer Time: $avg_transfer_time_sec s"
echo "Maximum Transfer Time: $max_transfer_time_sec s"

echo "Minimum Total Time: $min_total_time_sec s"
echo "Avarage Total Time: $avg_total_time_sec s"
echo "Maximum Total Time: $max_total_time_sec s"

echo "Minimum Latency Error Rate: $min_latency_error_rate %"
echo "Avarage Latency Error Rate: $avg_latency_error_rate %"
echo "Maximum Latency Error Rate: $max_latency_error_rate %"

echo "Minimum Bandwidth Error Rate: $min_bandwidth_error_rate %"
echo "Avarage Bandwidth Error Rate: $avg_bandwidth_error_rate %"
echo "Maximum Bandwidth Error Rate: $max_bandwidth_error_rate %"

printf "\nTotal Duration: $total_duration_sec s\n"

# Path for the CSV file to store the results
CSV_PATH="$BASISPFAD/master-thesis-ozcankaraca/data-for-testbed/results/results-testbed3.csv"

# Create a CSV file with headers if it doesn't already exist
if [ ! -f "$CSV_PATH" ]; then
    echo "TestID;Number of Peers;With Super-Peers;Total Duration from tracker-peer [s];Same PDF File;Total Received Bytes;Maximum Connection Time [s];Minimum Connection Time [s];Average Connection Time [s];Maximum Transfer Time [s];Minimum Transfer Time [s];Average Transfer Time [s];Maximum Total Time [s];Minimum Total Time [s];Average Total Time [s];Maximum Latency Error Rate [%];Minimum Latency Error Rate [%];Average Latency Error Rate [%];Maximum Bandwidth Error Rate [%];Minimum Bandwidth Error Rate [%];Average Bandwidth Error Rate [%]" > "$CSV_PATH"
fi

# Append the current test results to the CSV file
echo "Test$test_id;$(($NUMBER_OF_PEERS_ARG + 1));$HAS_SUPERPEER;$total_duration_sec;$all_containers_have_file;$total_received_bytes;$max_connection_time_sec;$min_connection_time_sec;$avg_connection_time_sec;$max_transfer_time_sec;$min_transfer_time_sec;$avg_transfer_time_sec;$max_total_time_sec;$min_total_time_sec;$avg_total_time_sec;$max_latency_error_rate;$min_latency_error_rate;$avg_latency_error_rate;$max_bandwidth_error_rate;$min_bandwidth_error_rate;$avg_bandwidth_error_rate" >> "$CSV_PATH"

printf "\nStep: Cleaning up the Testbed is done.\n"
printf "\nStopping Testbed\n\n"

        done
    done
done
