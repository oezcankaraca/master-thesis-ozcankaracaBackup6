#!/bin/bash

printf "\nStarting Testbed.\n\n"

# run-testbed-automation.sh
export has_superpeer_values="true false"
export number_of_peers_values="75 10 20 35 50 75 100 150"
export choice_of_pdf_mb_values="1 3 5 10 15 20 30"

# Base directory path where all project-related files are located
export BASE_PATH="$HOME/Desktop"

./configuration-testbed.sh

printf "\nStopping Testbed.\n\n"
