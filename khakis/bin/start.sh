#!/bin/bash

set -x

# Include common functionality
SCRIPT_PATH="$0"
SCRIPT_DIR=$(dirname ${SCRIPT_PATH})
. "${SCRIPT_DIR}/common.sh"

# Images default to local
ZOOKEEPER_IMAGE=${ZOOKEEPER_IMAGE:-"arcus/zookeeper"}
KAFKA_IMAGE=${KAFKA_IMAGE:-"arcus/kafka"}
CASSANDRA_IMAGE=${CASSANDRA_IMAGE:-"arcus/cassandra"}

# Container configuration
EYERIS_PLATFORM_DIRECT_PORTS=${EYERIS_PLATFORM_DIRECT_PORTS:-1}
EYERIS_PLATFORM_PORTS=${EYERIS_PLATFORM_PORTS:--P}
EYERIS_DEVELOPER_MODE=${EYERIS_DEVELOPER_MODE:-true}

ZOOKEEPER_CPUSHARES=${ZOOKEEPER_CPUSHARES:-4}
ZOOKEEPER_MEMORY=${ZOOKEEPER_MEMORY:-512m}
KAFKA_CPUSHARES=${KAFKA_CPUSHARES:-4}
KAFKA_MEMORY=${KAFKA_MEMORY:-768m}
CASSANDRA_CPUSHARES=${CASSANDRA_CPUSHARES:-4}
CASSANDRA_MEMORY=${CASSANDRA_MEMORY:-768m}


ZOOKEEPER_CMD="--name=eyeris-zookeeper --cpu-shares=$ZOOKEEPER_CPUSHARES --memory=$ZOOKEEPER_MEMORY $ZOOKEEPER_IMAGE"
KAFKA_CMD="--name=eyeris-kafka --cpu-shares=$KAFKA_CPUSHARES --memory=$KAFKA_MEMORY  -e KAFKA_REPLICATION=1 -e KAFKAOPS_REPLICATION=1 -e KAFKA_HSIZE=512 -e ADVERTISED_HSTN=kafka.eyeris -e ZOOKEEPER=zookeeper.eyeris:2181 -e ZOOKEEPEROPS=zookeeper.eyeris:2181 --link eyeris-zookeeper:zookeeper.eyeris $KAFKA_IMAGE"
CASSANDRA_CMD="--name=eyeris-cassandra --cpu-shares=$CASSANDRA_CPUSHARES --memory=$CASSANDRA_MEMORY -e CASSANDRA_REPLICATION=1 -e CASSANDRA_HEAPSIZE=512 $CASSANDRA_IMAGE"


if [ -n "${EYERIS_PLATFORM_DIRECT_PORTS}" ] && [ "${EYERIS_PLATFORM_PORTS}" == "-P" ]; then
    ZOOKEEPER_CMD="-P -p 2181:2181 -p 2888:2888 -p 3888:3888 $ZOOKEEPER_CMD"
    KAFKA_CMD="-P -p 9092:9092 $KAFKA_CMD"
    CASSANDRA_CMD="-P -p 7000:7000 -p 7001:7001 -p 7199:7199 -p 9042:9042 -p 9160:9160 $CASSANDRA_CMD"
fi

if [ "${EYERIS_DEVELOPER_MODE}" == "true" ]; then
    CASSANDRA_CMD="-e CASSANDRA_SINGLE_NODE=true $CASSANDRA_CMD"
fi

################################################################################
# Launch the base services
################################################################################

# Define the zookeeper docker container
docker_run -d $ZOOKEEPER_CMD

# Define the cassandra docker container
docker_run -d $CASSANDRA_CMD

# Define the kafka docker container
docker_run -d $KAFKA_CMD

sleep 2

# Provision Kafka & Cassandra
#  these processes now wait for kafka/cassandra to startup, might consider running them in the background
#  of the main process
docker_exec "eyeris-cassandra" cassandra-cmd setup &
docker_exec "eyeris-kafka" kafka-cmd setup &

wait

sleep 2
