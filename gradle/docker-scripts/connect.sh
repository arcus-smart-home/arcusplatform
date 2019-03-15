#!/bin/bash

# Include common functionality
SCRIPT_PATH="$0"
SCRIPT_DIR=$(dirname ${SCRIPT_PATH})
. "${SCRIPT_DIR}/common.sh"

CONTAINER_NAME="$1"
shift

# Set the default command to bash if none exists
CMD=${@:-bash}

# Define the development docker container using 1 cpu share and 64 MB of memory
docker_exec -i -t "${CONTAINER_NAME}" ${CMD}
