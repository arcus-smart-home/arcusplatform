#!/bin/bash

# Include common functionality
SCRIPT_PATH="$0"
SCRIPT_DIR=$(dirname ${SCRIPT_PATH})
. "${SCRIPT_DIR}/common.sh"

# Build all of the images if none are specified
IMAGES="$@"
if [ -z "${IMAGES}" ]; then
    IMAGES=""
#    IMAGES="${IMAGES} eyeris-base"
#    IMAGES="${IMAGES} eyeris-java"
#    IMAGES="${IMAGES} eyeris-zookeeper"
#    IMAGES="${IMAGES} eyeris-kafka"
#    IMAGES="${IMAGES} eyeris-cassandra"
#    IMAGES="${IMAGES} eyeris-kairosdb"
#    IMAGES="${IMAGES} eyeris-grafana"
#    IMAGES="${IMAGES} eyeris-git"
#    IMAGES="${IMAGES} eyeris-elasticsearch"
#    IMAGES="${IMAGES} eyeris-jessie"
#    IMAGES="${IMAGES} eyeris-logstash"
    #IMAGES="${IMAGES} eyeris-hbase"
    # having issues getting lek to build
#    IMAGES="${IMAGES} eyeris-lek"
fi


# Build the requested images
for image in ${IMAGES}; do
    docker_build "${image}"
done
