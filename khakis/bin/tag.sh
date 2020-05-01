#!/bin/bash

# Include common functionality
SCRIPT_PATH="$0"
SCRIPT_DIR=$(dirname ${SCRIPT_PATH})
. "${SCRIPT_DIR}/common.sh"

# Tag all of the images if none are specified
IMAGES="$@"
if [ -z "${IMAGES}" ]; then
    IMAGES=""
    IMAGES="${IMAGES} arcus-java"
    IMAGES="${IMAGES} arcus-zookeeper"
    IMAGES="${IMAGES} arcus-kafka"
    IMAGES="${IMAGES} arcus-cassandra"
#    IMAGES="${IMAGES} arcus-kairosdb"
fi

docker_tag_for_registry() {
    local DOCKER_PATH="$1"
    local DOCKER_NAME="${2:-$(basename ${DOCKER_PATH})}"
    local seperator=${REGISTRY_SEPERATOR:-/}
    local DOCKER_TAG=$(echo "${DOCKER_NAME}" |tr '-' "${seperator}")
    local DOCKER_SRC=$(echo "${DOCKER_NAME}" |tr '-' "/")

    if [ "$DOCKER_PREFIX_OVERRIDE" ]; then
        DOCKER_TAG=$(echo "${DOCKER_TAG}" | sed "s%arcus%${DOCKER_PREFIX_OVERRIDE}%")
    fi

    if [ "$DOCKER_VERSION" ]; then
        local DOCKER_VERSION=":${DOCKER_VERSION}"
    fi

    if [ "$REGISTRY_NAME" ]; then
        docker_tag "${DOCKER_SRC}" "${REGISTRY_NAME}/${DOCKER_TAG}:latest"
        docker_tag "${DOCKER_SRC}" "${REGISTRY_NAME}/${DOCKER_TAG}${DOCKER_VERSION}"
    else
        docker_tag "${DOCKER_SRC}" "${DOCKER_TAG}:latest"
        docker_tag "${DOCKER_SRC}" "${DOCKER_TAG}${DOCKER_VERSION}"
    fi
}

# Build the requested images
for image in ${IMAGES}; do
    docker_tag_for_registry "${image}"
done

