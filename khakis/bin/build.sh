#!/bin/bash

# Include common functionality
SCRIPT_PATH="$0"
SCRIPT_DIR=$(dirname ${SCRIPT_PATH})
. "${SCRIPT_DIR}/common.sh"

# Build all of the images if none are specified
IMAGES="$@"
if [ -z "${IMAGES}" ]; then
    IMAGES=""
    IMAGES="${IMAGES} arcus-java"
    IMAGES="${IMAGES} arcus-zookeeper"
    IMAGES="${IMAGES} arcus-kafka"
    IMAGES="${IMAGES} arcus-cassandra"
#    IMAGES="${IMAGES} arcus-kairosdb"
fi

# Build the requested images
for image in ${IMAGES}; do
    docker_build "${image}"
done

