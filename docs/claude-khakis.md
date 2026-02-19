# Khakis — Docker Infrastructure (`/khakis`)

Khakis provides Dockerized infrastructure services for local development and deployment.

---

## Infrastructure Containers

| Container | Version | Ports | Purpose |
|-----------|---------|-------|---------|
| `arcus-java` | Temurin JDK 8 | — | Base image (Debian Bullseye) for all other containers |
| `arcus-zookeeper` | 3.8.4 | 2181, 2888, 3888 | Distributed coordination |
| `arcus-kafka` | 2.6.0 (Scala 2.12) | 9092 | Async message bus |
| `arcus-cassandra` | 3.11.11 | 9042, 9160, 7000, 7199 | NoSQL database |
| `arcus-kairosdb` | — | 8080, 4242 | Time-series metrics |

---

## Gradle Tasks

```bash
./gradlew :platform:arcus-khakis:distDocker       # Build all Docker images
./gradlew :platform:arcus-khakis:startPlatform    # Start infra + model manager
./gradlew :platform:arcus-khakis:stopPlatform     # Stop containers
./gradlew :platform:arcus-khakis:tagDocker        # Tag images for registry
./gradlew :platform:arcus-khakis:pushDocker       # Push to container registry
```

---

## Shell Scripts (`khakis/bin/`)

- `start.sh` — Starts ZooKeeper → Kafka → Cassandra in sequence; controlled by env vars:
  - `EYERIS_PLATFORM_MEMORY` (default 1 GB per container)
  - `EYERIS_PLATFORM_CPUSHARES`
  - `EYERIS_PLATFORM_DIRECT_PORTS`
- `stop.sh` — Graceful shutdown; respects `EYERIS_PLATFORM_NAME` for multi-instance setups
- `setup-cassandra.sh` — Cassandra schema initialization
- `common.sh` — Shared helpers (`docker_build`, registry config, tag/push)

---

## Docker Compose

`khakis/docker-compose/docker-compose.yml` defines the full local dev stack including KairosDB and a UI server. Cassandra runs in single-node mode (`CASSANDRA_SINGLE_NODE=true`) with persistent volumes.

---

## Notable: Cassandra Extras

Includes a custom `TimeWindowCompactionStrategy-2.2.5.jar` plugin for efficient time-series data compaction.
