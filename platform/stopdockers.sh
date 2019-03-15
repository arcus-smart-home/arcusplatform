#!/bin/bash

docker stop platform-services.eyeris
docker rm platform-services.eyeris
docker stop notification-services.eyeris
docker rm notification-services.eyeris
docker stop ipcd-bridge.eyeris
docker rm ipcd-bridge.eyeris
docker stop hub-bridge.eyeris
docker rm hub-bridge.eyeris
docker stop driver-services.eyeris
docker rm driver-services.eyeris
docker stop driver-bridge.eyeris
docker rm driver-bridge.eyeris
docker stop client-bridge.eyeris
docker rm client-bridge.eyeris
docker stop eyeris-kafka
docker rm eyeris-kafka
docker stop eyeris-cassandra
docker rm eyeris-cassandra
docker stop eyeris-zookeeper
docker rm eyeris-zookeeper
