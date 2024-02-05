#!/bin/bash

printf "\nStarting Testbed.\n\n"

# run-testbed-automation.sh
export has_superpeer_values="true false"
export number_of_peers_values="5 10 20 35 50 75"
export choice_of_pdf_mb_values="2 4 8 16 32 64 128"

# Base directory path where all project-related files are located
export BASE_PATH="$HOME/Desktop"

./configuration-testbed.sh

printf "\nStopping Testbed.\n\n"
