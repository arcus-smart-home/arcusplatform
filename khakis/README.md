# Khakis - Arcus Infrastructure

Arcus utilizes Cassandra, Kafka, and Zookeeper in order to support arcusplatform. This sub-project contains the necessary Dockerfiles and scripts to build and run the Arcus containers.

For a production deployment, it is recommended to use some form of container orchestration, e.g. Docker Compose or Kubernetes.

## Building the Arcus docker images

Running the following command on the console will build all of the docker
images required for the Arcus Platform.

```
./khakis/bin/build.sh
```

To build a specific image, pass it as an argument:

```
./khakis/bin/build.sh arcus-cassandra
```

Docker caches intermediate images to speed up image creation, so running this
command after making small changes will not take as long as the first run.

You can also build via gradle, which will tag the images with the version from `version.properties`:

```
./gradlew :khakis:distDocker
```

## Starting the Arcus infrastructure containers

Running the following command will start the Zookeeper, Kafka, and Cassandra containers:

```
./khakis/bin/start.sh
```

There are several environment variables that control container configuration:

* `ZOOKEEPER_CPUSHARES` (default: 4) - CPU shares for Zookeeper
* `ZOOKEEPER_MEMORY` (default: 512m) - Memory limit for Zookeeper
* `KAFKA_CPUSHARES` (default: 4) - CPU shares for Kafka
* `KAFKA_MEMORY` (default: 768m) - Memory limit for Kafka
* `CASSANDRA_CPUSHARES` (default: 4) - CPU shares for Cassandra
* `CASSANDRA_MEMORY` (default: 768m) - Memory limit for Cassandra
* `EYERIS_PLATFORM_DIRECT_PORTS` (default: 1) - Maps container ports to the same host ports
* `EYERIS_DEVELOPER_MODE` (default: true) - Runs Cassandra in single-node mode

## Stopping the Arcus infrastructure containers

```
./khakis/bin/stop.sh
```

To stop a specific container:

```
./khakis/bin/stop.sh eyeris-cassandra
```

## Connecting to a running container

```
./khakis/bin/connect.sh <container-name> [command]
```

If a command is not given then the script will start a bash shell by default. For example:

```
./khakis/bin/connect.sh eyeris-cassandra cqlsh
```

## Versioning, Tagging, and Pushing

See [docs/RELEASING.md](../docs/RELEASING.md) for the full release workflow, including how to build, tag, and push Docker images.
