#!/bin/bash

# This script is designed to configure a network interface on a Linux system with a specific IP address. It uses environment variables to dynamically set the IP address and source peer node.

# Environment variables for IP address and interface
IP_ADDRESS=$IP_ADDRES # Retrieves the IP address from an environment variable
INTERFACE="eth1" # Sets the network interface to be configured, here it's eth1
SOURCE_PEER=$SOURCE_PEER # Retrieves the source node information from an environment variable

# Prints the source peer information
printf "\n--SOURCE_PEER: $SOURCE_PEER--\n"

# Function to configure a network interface with an IP address
configure_interface() {
    local interface=$1
    local ip_address=$2

    # Adds the specified IP address to the specified network interface
    ip addr add ${ip_address}/24 dev $interface
    echo "Info: New IP address "$ip_address" added for interface "$interface""
}

# Calls the function to configure the network interface with the IP address
printf "\nReceiving Configuration:\n"
echo "Info: Configuring the network interface with the following details for" $SOURCE_PEER
echo "Network Details--> Interface: $INTERFACE" "IP Address: $IP_ADDRESS"
configure_interface "$INTERFACE" "$IP_ADDRESS"
echo ""
echo "---------------------------------------------------------------------------------------------------------------------------------------------"

