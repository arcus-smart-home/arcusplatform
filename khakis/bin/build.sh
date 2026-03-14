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

# Build arcus-java first, including the JDK 11 variant needed by Cassandra/Kafka
if [ -z "${1}" ] || echo "${IMAGES}" | grep -q "arcus-java"; then
    docker_build "arcus-java"
    echo "Building arcus/java:jdk11 for infra containers..."
    docker build --build-arg JAVA_VERSION=11 -t arcus/java:jdk11 arcus-java
fi

# Build the remaining images
for image in ${IMAGES}; do
    [ "${image}" = "arcus-java" ] && continue
    docker_build "${image}"
done

