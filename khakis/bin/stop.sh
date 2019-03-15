#!/bin/bash

# Include common functionality
SCRIPT_PATH="$0"
SCRIPT_DIR=$(dirname ${SCRIPT_PATH})
. "${SCRIPT_DIR}/common.sh"

PROCESSES="$@"
if [ -z "${PROCESSES}" ]; then
   PROCESSES=""
   PROCESSES="${PROCESSES} eyeris-cassandra"
   PROCESSES="${PROCESSES} eyeris-kafka"
   PROCESSES="${PROCESSES} eyeris-zookeeper"

fi

################################################################################
# Stop the each container and then delete its definition
################################################################################

for process in ${PROCESSES}; do
   echo "Stopping process ${process}..."
   docker_stop "${process}" &
done

wait

for process in ${PROCESSES}; do
   echo "Deleting process ${process}..."
   docker_delete "${process}" &
done

wait
