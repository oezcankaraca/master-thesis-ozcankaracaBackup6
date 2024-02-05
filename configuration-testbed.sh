#!/bin/bash

# Read values from run-testbed.sh
IFS=' ' read -r -a has_superpeer_values <<< "$has_superpeer_values"
IFS=' ' read -r -a number_of_peers_values <<< "$number_of_peers_values"
IFS=' ' read -r -a choice_of_pdf_mb_values <<< "$choice_of_pdf_mb_values"
IFS=' ' read -r -a BASE_PATH <<< "$BASE_PATH"

# Class names of various Java programs used in the testbed
java_program_for_testbed_class1="GeneratorOfNetworkTopology"
java_program_for_testbed_class2="ConnectionAnalysis"
java_program_for_testbed_class3="NetworkConfigParser"
java_program_for_testbed_class4="ConnectionDetails"
java_program_for_testbed_class5="YMLGenerator"
java_program_for_testbed_class6="OnlyFromLectureStudioServerToPeers"

# Class names for the Java programs used in validation
java_program_for_validation_class1="ConnectionQuality"
java_program_for_validation_class2="CompareFiles"

# Variables for calculating and measuring data transfer time
declare -A calculated_times
declare -A measured_times

# Clean for Docker images
enable_cleanup_for_image="false"
run_generator_network_topology="false"

# Paths to various components of the Java programs used in the testbed
JAVA_PROGRAM_FOR_TESTBED_PATH="$BASE_PATH/master-thesis-ozcankaraca/java-program-for-testbed/"
JAVA_PROGRAM_FOR_VALIDATION_PATH="$BASE_PATH/master-thesis-ozcankaraca/java-program-for-validation/"
JAVA_PROGRAM_FOR_CONTAINER_PATH="$BASE_PATH/master-thesis-ozcankaraca/java-program-for-container/"

# Path to the YAML file for containerlab topology configuration
CONTAINERLAB_YML="$BASE_PATH/master-thesis-ozcankaraca/java-program-for-container/src/main/java/containerlab-topology.yml"

# Paths for Docker images related to the testbed, tracker-peer, and monitoring tools
IMAGE_TESTBED_PATH="$BASE_PATH/master-thesis-ozcankaraca/java-program-for-container/"
IMAGE_TRACKER_PATH="$BASE_PATH/master-thesis-ozcankaraca/java-program-for-validation/src/main/java"
IMAGE_ANALYSING_MONITORING_PATH="$BASE_PATH/master-thesis-ozcankaraca/data-for-testbed/data-for-analysing-monitoring/"

# Path to the PDF files for data transfer
PDF_FILES_PATH="$BASE_PATH/master-thesis-ozcankaraca/data-for-testbed/PDF/"
DESTINATION_PATH="$BASE_PATH/master-thesis-ozcankaraca/data-for-testbed/"
DELETE_FILE_PATH="$BASE_PATH/master-thesis-ozcankaraca/data-for-testbed/mydocument.pdf"

# Iterate over each combination of variable values
for has_superpeer in "${has_superpeer_values[@]}"; do
    for number_of_peers in "${number_of_peers_values[@]}"; do
        for choice_of_pdf_mb in "${choice_of_pdf_mb_values[@]}"; do
            echo "Info: Running test with has_superpeer=$has_superpeer, number_of_peers=$number_of_peers, choice_of_pdf_mb=$choice_of_pdf_mb"

printf "\nStep Started: Choosing test case and moving data file.\n"

# Delete the last pdf file for testing and give information about the pdf file 
rm -f "$DELETE_FILE_PATH"

file_name="${choice_of_pdf_mb}MB.pdf"
FULL_PATH="${PDF_FILES_PATH}/${file_name}"

if [ -f "$FULL_PATH" ]; then
  
    cp "$FULL_PATH" "$DESTINATION_PATH/mydocument.pdf"
    printf "\nSuccess: File '$file_name' was copied as 'mydocument.pdf' to '$DESTINATION_PATH'.\n"
else
    printf "\nUnsucess: File '$file_name' was not found.\n"
fi

sleep 30
printf "\nStep Done: Choosing test case and moving data file are done.\n\n"

testbed_and_containerlab() {

    # Navigating to the directory containing the Java program for testbed
    cd "$JAVA_PROGRAM_FOR_TESTBED_PATH"
    
    # Executing specific Java classes based on the configuration of super-peers 
    if [ "$has_superpeer" = "false" ]; then
        echo "Info: Executing OnlyFromLectureStudioServerToPeers class as has_superpeer is set to false."
        mvn -q exec:java -Dexec.mainClass="$java_program_for_testbed_class6" -Dexec.args="$number_of_peers"
    fi
    
    if [ "$run_generator_network_topology" == "true" ]; then
        mvn -q exec:java -Dexec.mainClass="$JAVA_PROGRAM_FOR_TESTBED_CLASS1" -Dexec.args="$number_of_peers"
        sleep 5
    fi
    
    mvn -q exec:java -Dexec.mainClass="$java_program_for_testbed_class2" -Dexec.args="$number_of_peers"
    sleep 5
    
    mvn -q exec:java -Dexec.mainClass="$java_program_for_testbed_class3" -Dexec.args="$number_of_peers $has_superpeer"
    sleep 5
    
JAVA_OUTPUT_FILE_PATH=$(mktemp)
mvn -q exec:java -Dexec.mainClass="$java_program_for_testbed_class4" -Dexec.args="$number_of_peers $has_superpeer $choice_of_pdf_mb" | tee "$JAVA_OUTPUT_FILE_PATH"


while IFS= read -r line; do
    if [[ "$line" =~ ([0-9]+):[[:space:]]+([0-9]+)[[:space:]]ms ]]; then
        container_id="${BASH_REMATCH[1]}"
        time_ms="${BASH_REMATCH[2]}"
        calculated_times["p2p-containerlab-topology-$container_id"]=$time_ms
    fi
done < "$JAVA_OUTPUT_FILE_PATH"

rm "$JAVA_OUTPUT_FILE_PATH"

printf "\n--Calculated Transfer Times for Container--\n"

for container_name in "${!calculated_times[@]}"; do
    echo "Container $container_name: ${calculated_times[$container_name]} ms"
done

printf "\nStep Done: Combining connection details is done.\n"
     
    mvn -q exec:java -Dexec.mainClass="$java_program_for_testbed_class5" -Dexec.args="$number_of_peers $has_superpeer"
    sleep 5

    printf "Step Started: Generating Docker image.\n"

    # Building Docker images for the testbed, tracker-peer, and monitoring tools
    cd "$IMAGE_TESTBED_PATH"
    printf "\nInfo: Creating Docker image for testbed.\n"
    #docker build -f dockerfile.testbed -t image-testbed .
    #sleep 5
    
    cd "$IMAGE_TRACKER_PATH"
    printf "\nInfo: Creating Docker image for tracker-peer.\n"
    #docker build -f dockerfile.tracker -t image-tracker .
    #sleep 5
    
    cd "$IMAGE_ANALYSING_MONITORING_PATH"
    printf "\nInfo: Creating Docker image for analysing and monitoring.\n"
    #docker build -f dockerfile.cadvisor -t image-cadvisor .
    #sleep 5
    #docker build -f dockerfile.grafana -t image-grafana .
    #sleep 5
    #docker build -f dockerfile.prometheus -t image-prometheus .
    
    printf "\nStep Done: Genereating Docker image is done.\n"

    # Starting the deployment of Containerlab
    printf "\nStep Started: Creating Containerlab file.\n"
    printf "\nInfo: Starting Containerlab.\n\n"
    sudo containerlab deploy -t "$CONTAINERLAB_YML"
    sleep 5 
    printf "\nStep Done: Creating Containerlab file is done.\n"
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
    done < <(mvn -q exec:java -Dexec.mainClass="$java_program_for_validation_class1" -Dexec.args="$number_of_peers")
}

# Executing the testbed setup and validation process
testbed_and_containerlab
run_validation

# Check if the previous command was successful
if [ $? -eq 0 ]; then
    printf "\nSuccess: Proceeding\n"
else
    # If the validation failed, restart the testbed and run validation 
    printf "Unsuccess: Some tests need to be repeated. Restarting the testbed and containerlab."

    printf "Info: Destroying Containerlab and cleaning up the environment."
    sudo containerlab destroy -t "$CONTAINERLAB_YML" --cleanup
   
    # Executing the testbed setup and validation process again
    testbed_and_containerlab
    run_validation
fi

printf "\nInfo: Validation is done.\n"

printf "\nStep Started: Checking Container logs.\n\n"

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
    container_name=$(docker inspect --format '{{.Name}}' "$id" | sed 's/^\/\+//') 
    
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
    
    printf "\n-----------------------------------------------------------------------------------------------------------------------------------------------"
    echo ""
done
    
    if [[ -n "$lectureStudioServerLog" ]]; then
    echo "--Logs for Container p2p-containerlab-topology-lectureStudioServer:--"
    echo "$lectureStudioServerLog"
    printf "\n-----------------------------------------------------------------------------------------------------------------------------------------------"
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
    
    if [ ! -z "$transfer_time" ]; then
        measured_times["$container_name"]=$transfer_time
    fi
    
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
        echo "Error: No valid Connection Time, File Transfer Time, or Total Time found in logs for $container_name."
        all_containers_processed=false
    fi
    
       if [[ "$total_time" =~ ^[0-9]+$ ]]; then
        total_total_time=$((total_total_time + total_time))
    fi
done

min_error_rate=99999999  
max_error_rate=0
total_error_rate=0
count_error_rates=0

printf "\n--Results of Error Rates for Transfer Time--\n\n"

for container_name in "${!measured_times[@]}"; do
    measured_time=${measured_times[$container_name]}
    
    if [[ -n ${calculated_times[$container_name]+_} ]]; then
        calculated_time=${calculated_times[$container_name]}
        
        if [ -n "$calculated_time" ] && [ -n "$measured_time" ]; then
            error_rate=$(echo "scale=4; (($measured_time - $calculated_time) / $calculated_time) * 100" | bc)
            error_rate=$(printf "%.2f" "$error_rate") 
            echo "$container_name"
            echo "Measured Time: $measured_time"
            echo "Calculated Time: $calculated_time"
            echo "Transfer Time Fehler Rate: $error_rate %"
            echo ""
            
            if (( $(echo "$error_rate < $min_error_rate" | bc -l) )); then
                min_error_rate=$error_rate
            fi
            
            if (( $(echo "$error_rate > $max_error_rate" | bc -l) )); then
                max_error_rate=$error_rate
            fi
            
            total_error_rate=$(echo "scale=2; $total_error_rate + $error_rate" | bc)
            count_error_rates=$((count_error_rates + 1))
        else
            echo "Error: No data found for container $container_name."
        fi
    else
        echo "Error: No calculated data found for container $container_name."
    fi
done

avg_error_rate=0
if [ "$count_error_rates" -gt 0 ]; then
    avg_error_rate=$(echo "scale=2; $total_error_rate / $count_error_rates" | bc)
    avg_error_rate=$(printf "%.2f" "$avg_error_rate")
fi

echo "-----------------------------------------------------------------------------------------------------------------------------------------------"

if [[ -n "$trackerPeerId" ]]; then
    printf "\n--Logs for Container p2p-containerlab-topology-trackerPeer:--\n\n"
    tracker_peer_logs=$(docker logs "$trackerPeerId")
    echo "$tracker_peer_logs" 

    total_duration=$(echo "$tracker_peer_logs" | grep "Result: Total duration" | awk '{print $4}')
    
    if [[ -n "$total_duration" ]]; then
        echo "Info: Total Duration from Tracker Peer: $total_duration ms"
    else
        echo "Error: Total Duration not found in Tracker Peer Logs."
    fi

    echo "----------------------------------------------------------------------------------------------------------------------------------------"
fi

if [ $count_containers -gt 0 ]; then
    avg_connection_time=$((total_connection_time / count_containers))
    avg_transfer_time=$((total_transfer_time / count_containers))
    avg_total_time=$(( (total_total_time / count_containers) / 2 ))
else
    printf "\nError: No valid data for average time calculations.\n"
fi

if $all_containers_processed; then
    printf "\n\n--Results related to Connection, File Transfer and Total Time:--\n\n"
    
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
printf "\nStep Done: Checking Container logs is done.\n"

sleep 20 

cd "$JAVA_PROGRAM_FOR_VALIDATION_PATH"
all_containers_have_file=false

while IFS= read -r line; do
    echo "$line" 
    if [[ "$line" == "Info: All containers have the same file based on the hash values." ]]; then
        all_containers_have_file=true
    fi
done < <(mvn -q exec:java -Dexec.mainClass="$java_program_for_validation_class2" -Dexec.args="$NUMBER_OF_PEERS_ARG")
sleep 5

printf "\nStep Started: Cleaning up the testbed.\n"

# Destroying the Containerlab setup and cleaning up the environment
printf "\nInfo: Destroying Containerlab and cleaning up the environment.\n\n"
sudo containerlab destroy -t "$CONTAINERLAB_YML" --cleanup

# Waiting for a short period to ensure all containers are stopped
printf "\nInfo: Waiting for all Containers to stop.\n"
sleep 5

if [ "$enable_cleanup_for_image" == "true" ]; then
    # Checking if any containers using the 'image-testbed' image are still running
    if [ -z "$(docker ps -q --filter ancestor=image-testbed)" ]; then
        printf "Info: All Containers have stopped.\n\n"
        printf "Deleting Docker image:\n"

        docker image rm image-testbed
        docker image rm image-tracker
        docker image rm image-cadvisor
        docker image rm image-grafana
        docker image rm image-prometheus
        printf "\nInfo: Docker image successfully deleted."
    else
        echo "Error: There are still running Containers. Cannot delete Docker image."
    fi
fi

printf "\nStep Done: Cleaning up the Testbed is done.\n"

printf "\nStep Started: Calculating all results.\n"

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
TEST_ID_FILE_PATH="$BASE_PATH/master-thesis-ozcankaraca/data-for-testbed/results/test_id_counter.txt"

# Incrementing the test ID for each run, or starting at 1 if the file doesn't exist
if [ -f "$TEST_ID_FILE_PATH" ]; then
    test_id=$(<"$TEST_ID_FILE_PATH")
    test_id=$((test_id+1)) 
else
    test_id=1  
fi

# Updating the test ID counter file
echo "$test_id" > "$TEST_ID_FILE_PATH"

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
echo "Number of Peers: $(($number_of_peers + 1))"
echo "With Super-Peers: $has_superpeer"
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

echo "Minimum Transfer Time Error Rate: $min_error_rate %"
echo "Average Transfer Time Error Rate: $avg_error_rate %"
echo "Maximum Transfer Time Error Rate: $max_error_rate %"

printf "\nTotal Duration: $total_duration_sec s\n"

printf "\nStep Done: Calculating all results is done.\n"

printf "\nStep Started: Writing all results into CSV file.\n"

# Path for the CSV file to store the results
CSV_PATH="$BASE_PATH/master-thesis-ozcankaraca/data-for-testbed/results/results-testbed.csv"

# Create a CSV file with headers if it doesn't already exist
if [ ! -f "$CSV_PATH" ]; then
    echo "TestID;Number of Peers;With Super-Peers;Total Duration [s];Total Received Bytes;Minimum Transfer Time Error Rate [%];Average Transfer Time Error Rate [%];Maximum Transfer Time Error Rate [%];Same PDF File;Maximum Connection Time [s];Minimum Connection Time [s];Average Connection Time [s];Maximum Transfer Time [s];Minimum Transfer Time [s];Average Transfer Time [s];Maximum Total Time [s];Minimum Total Time [s];Average Total Time [s];Maximum Latency Error Rate [%];Minimum Latency Error Rate [%];Average Latency Error Rate [%];Maximum Bandwidth Error Rate [%];Minimum Bandwidth Error Rate [%];Average Bandwidth Error Rate [%]" > "$CSV_PATH"
fi

# Append the current test results to the CSV file along with the new fields for error rates and total duration
echo "Test$test_id;$number_of_peers;$has_superpeer;$total_duration_sec;$total_received_bytes;$min_error_rate;$avg_error_rate;$max_error_rate;$all_containers_have_file;$max_connection_time_sec;$min_connection_time_sec;$avg_connection_time_sec;$max_transfer_time_sec;$min_transfer_time_sec;$avg_transfer_time_sec;$max_total_time_sec;$min_total_time_sec;$avg_total_time_sec;$max_latency_error_rate;$min_latency_error_rate;$avg_latency_error_rate;$max_bandwidth_error_rate;$min_bandwidth_error_rate;$avg_bandwidth_error_rate" >> "$CSV_PATH"

printf "\nStep Done: Writing all results into CSV file is done.\n\n"

        done
    done
done
