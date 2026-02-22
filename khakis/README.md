# Khakis - Arcus Infrastructure

Arcus utilizes Cassandra, Kafka, and Zookeeper in order to support arcusplatform. This sub-project contains the necessary Dockerfiles and scripts to build and run the Arcus containers.

For a production deployment, it is recommended to use some form of container orchestration, e.g. Docker Compose or Kubernetes.

## Building the Arcus docker images

Running the following command on the console will build all of the docker
images required for the Arcus Platform.

```
./bin/build.sh
```

To build a specific image, pass it as an argument:

```
./bin/build.sh arcus-cassandra
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
./bin/start.sh
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
./bin/stop.sh
```

To stop a specific container:

```
./bin/stop.sh eyeris-cassandra
```

## Connecting to a running container

```
./bin/connect.sh <container-name> [command]
```

If a command is not given then the script will start a bash shell by default. For example:

```
./bin/connect.sh eyeris-cassandra cqlsh
```

## Versioning

If you invoke `./bin/build.sh` to build your containers, the version will be `latest`, however if you utilize `./gradlew :khakis:distDocker`, the version will be set based on `version.properties`, e.g. `2019.10.0`.

The version can be adjusted by changing `version.properties`, but you should probably use the gradle `branchRelease` and `tagRelease` targets instead.

## Pushing docker containers

`./khakis/bin/tag.sh` and `./khakis/bin/push.sh` accept two environment variables which change the path of the image to tag or push.

* `REGISTRY_SEPARATOR` - Controls the separator between image names, e.g. `arcus/java` or `arcus-java`. For Google Container Registry (gcr) this can be `/`, but for DockerHub this will need to be `-`.
* `REGISTRY_NAME` - Specifies where to push the container to, e.g. `gcr.io/YOURPROJECT` or your DockerHub account name.

First tag:
```
REGISTRY_SEPARATOR='/' REGISTRY_NAME=gcr.io/arcus-238802 ./khakis/bin/tag.sh
```
Then push:
```
REGISTRY_SEPARATOR='/' REGISTRY_NAME=gcr.io/arcus-238802 ./khakis/bin/push.sh
```
