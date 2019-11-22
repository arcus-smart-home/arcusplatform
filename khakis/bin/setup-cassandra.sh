#!/bin/bash

INSTANCE=$(docker ps -f ancestor=arcus/cassandra:latest --latest --format '{{.ID}}')
echo "Found instance $INSTANCE"
docker exec -e 'CASSANDRA_KEYSPACE=dev' -e 'CASSANDRA_REPLICATION=1' $INSTANCE /usr/bin/cassandra-provision
