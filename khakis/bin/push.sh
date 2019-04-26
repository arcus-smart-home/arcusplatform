#!/bin/bash

# Include common functionality
SCRIPT_PATH="$0"
SCRIPT_DIR=$(dirname ${SCRIPT_PATH})
. "${SCRIPT_DIR}/common.sh"

# Push all of the images if none are specified
IMAGES="$@"
if [ -z "${IMAGES}" ]; then
    IMAGES=""
    IMAGES="${IMAGES} eyeris-java"
    IMAGES="${IMAGES} eyeris-zookeeper"
    IMAGES="${IMAGES} eyeris-kafka"
    IMAGES="${IMAGES} eyeris-cassandra"
fi

docker_push_to_cloud() {
    local DOCKER_PATH="$1"
    local DOCKER_NAME="${2:-$(basename ${DOCKER_PATH})}"
    local DOCKER_TAG=$(echo "${DOCKER_NAME}" |tr '-' '/')
    docker_push "gcr.io/${GCP_PROJECT_ID}/${DOCKER_TAG}:latest"
}

# Build the requested images
for image in ${IMAGES}; do
    docker_push_to_cloud "${image}"
done

