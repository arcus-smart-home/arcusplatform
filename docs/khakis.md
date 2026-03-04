# Khakis — Docker Infrastructure (`/khakis`)

Khakis provides Dockerized infrastructure services (ZooKeeper, Kafka, Cassandra) for local development and deployment of the Arcus platform. All platform services depend on these infrastructure containers.

---

## Quick Start

```bash
# Build infrastructure Docker images
./gradlew :platform:arcus-khakis:distDocker

# Start infrastructure + run Cassandra schema migrations
./gradlew :platform:arcus-khakis:startPlatform

# Stop infrastructure
./gradlew :platform:arcus-khakis:stopPlatform
```

---

## Infrastructure Containers

All containers are built from `arcus/java`, the base image.

### arcus/java (Base Image)

| Property | Value |
|----------|-------|
| Base | Debian Bullseye |
| JDK | Temurin 8 JRE (Adoptium) |
| User | `arcus` (UID 999) |
| Locale | `en_US.UTF-8` |
| Includes | procps, less, tcpdump, vim |

### arcus/zookeeper

| Property | Value |
|----------|-------|
| Version | 3.8.6 |
| Ports | 2181 (client), 2888 (peer), 3888 (leader election) |
| Entry point | `zookeeper-cmd entry` → `zookeeper-cmd start` |
| Data dir | `/data` |

### arcus/kafka

| Property | Value |
|----------|-------|
| Version | 2.8.2 (Scala 2.12) |
| Port | 9092 |
| Entry point | `kafka-cmd entry` → `kafka-cmd start` |
| Data dir | `/data` |
| Host aliases | `kafka.eyeris`, `kafkaops.eyeris` → 127.0.0.1 |
| Scripts | `kafka-cmd`, `kafka-provision`, `kafka-operations-provision`, `kafka-console-consumer`, `kafka-console-producer` |

### arcus/cassandra

| Property | Value |
|----------|-------|
| Version | 4.0.15 |
| Ports | 9042 (CQL), 7000 (intra-node), 7001 (TLS intra-node), 7199 (JMX) |
| Entry point | `cassandra-cmd entry` → `cassandra-cmd start` |
| Data dir | `/data` |
| Extras | `cqlsh` and `nodetool` symlinked to `/usr/bin`, Python 3 for CQL shell |
| Scripts | `cassandra-cmd`, `cassandra-provision` |

### arcus/kairosdb (optional)

| Property | Value |
|----------|-------|
| Ports | 8080, 4242 |
| Purpose | Time-series metrics storage |
| Status | Disabled in default build (`build.sh` comments it out) |

---

## Provisioning

### Kafka Topics

`kafka-provision` creates these topics on first startup (32 partitions each, replication factor configurable via `KAFKA_REPLICATION`):

| Topic | Purpose |
|-------|---------|
| `platform` | Platform messages (inter-service commands and events) |
| `protocol_todriver` | Protocol messages routed to driver-services |
| `protocol_tohub` | Protocol messages routed to hubs |
| `protocol_ipcdtodevice` | IPCD protocol messages to IP-connected devices |
| `test` | Test topic |

Provisioning is idempotent — skipped if `/data/install/.eyeris-kafka` exists (use `-f` to force re-run).

### Cassandra Keyspaces

`cassandra-provision` creates these keyspaces with `SimpleStrategy` replication:

| Keyspace | Purpose |
|----------|---------|
| `dev` (configurable via `CASSANDRA_KEYSPACE`) | Primary platform data (places, people, devices, rules, subsystems) |
| `support` | Support/operational data |
| `video` | Recording metadata, streaming sessions, previews |
| `history` | Device attribute change history, partitioned by time |
| `analytics` | Analytics events |

Tables within these keyspaces are created by `arcus-modelmanager` (schema migration tool) which runs as part of `startPlatform`.

---

## Startup Sequence

`bin/start.sh` launches containers in this order:

```
1. ZooKeeper (eyeris-zookeeper)
2. Cassandra (eyeris-cassandra)      [parallel with ZooKeeper]
3. Kafka (eyeris-kafka)              [linked to eyeris-zookeeper]
4. sleep 2
5. cassandra-provision (background)  [waits for Cassandra, creates keyspaces]
6. kafka-provision (background)      [waits for Kafka, creates topics]
7. wait for provisioning to complete
```

Then `startPlatform` runs `arcus-modelmanager` to apply schema migrations to Cassandra.

### Container Resource Defaults

| Container | CPU Shares | Memory |
|-----------|-----------|--------|
| ZooKeeper | 4 | 512m |
| Kafka | 4 | 768m |
| Cassandra | 4 | 768m |

### Container Linking

Kafka links to ZooKeeper via `--link eyeris-zookeeper:zookeeper.eyeris`.

In developer mode (`EYERIS_DEVELOPER_MODE=true`, the default), Cassandra runs in single-node mode (`CASSANDRA_SINGLE_NODE=true`).

---

## Gradle Tasks

| Task | Description |
|------|-------------|
| `distDocker` | Build all infrastructure Docker images (calls `bin/build.sh`) |
| `startPlatform` | Start infrastructure + run schema migrations (`startPlatformContainers` + `arcus-modelmanager:run`) |
| `startProdPlatform` | Start production infrastructure (calls `bin/start-prod.sh`) |
| `stopPlatform` | Stop development containers (calls `bin/stop.sh`) |
| `stopProdPlatform` | Stop production containers (calls `bin/stop-prod.sh`) |
| `tagDocker` | Tag images for registry (calls `bin/tag.sh`) |
| `pushDocker` | Push images to registry (depends on `tagDocker`, calls `bin/push.sh`) |

---

## Shell Scripts (`khakis/bin/`)

| Script | Purpose |
|--------|---------|
| `build.sh` | Build Docker images for java, zookeeper, kafka, cassandra |
| `start.sh` | Start containers with resource limits and port mapping |
| `stop.sh` | Stop and delete containers (accepts specific names or defaults to all) |
| `connect.sh` | Exec into a running container: `./connect.sh eyeris-cassandra [cmd]` (defaults to bash) |
| `tag.sh` | Tag images for a Docker registry |
| `push.sh` | Push tagged images to registry (retries up to 4 times on failure) |
| `release.sh` | CI release script — builds, tags, and pushes all images (only runs on `wl-net/arcusplatform` repo) |
| `setup-cassandra.sh` | Manual Cassandra schema setup |
| `common.sh` | Shared helpers: `docker_build`, `docker_run`, `docker_tag`, `docker_push`, `docker_stop` |
| `delete-eyeris-images.sh` | Delete all eyeris Docker images |
| `delete-untagged-images.sh` | Clean up untagged Docker images |

---

## Docker Compose

`khakis/docker-compose/docker-compose.yml` defines a full local dev stack:

```yaml
services:
  ui-server:     # Web UI (pulled from GCR)
  zookeeper:     # arcus/zookeeper, port 2181
  cassandra:     # arcus/cassandra, single-node mode, persistent volume
  kafka:         # arcus/kafka, port 9092, persistent volume
```

Volumes `cassandra-storage` and `kafka-storage` persist data across container restarts.

---

## Environment Variables

### Container Resource Control

| Variable | Default | Description |
|----------|---------|-------------|
| `EYERIS_PLATFORM_NAME` | `eyeris` | Container name prefix (allows multiple independent stacks) |
| `EYERIS_PLATFORM_MEMORY` | `1g` | Max memory per container |
| `EYERIS_PLATFORM_CPUSHARES` | `1` | CPU shares (relative weight) |
| `EYERIS_PLATFORM_DIRECT_PORTS` | `1` | Map container ports to host directly (set empty for random) |
| `EYERIS_DEVELOPER_MODE` | `true` | Single-node Cassandra mode |
| `ZOOKEEPER_MEMORY` | `512m` | ZooKeeper memory limit |
| `KAFKA_MEMORY` | `768m` | Kafka memory limit |
| `CASSANDRA_MEMORY` | `768m` | Cassandra memory limit |

### Image Overrides

| Variable | Default | Description |
|----------|---------|-------------|
| `ZOOKEEPER_IMAGE` | `arcus/zookeeper` | Custom ZooKeeper image |
| `KAFKA_IMAGE` | `arcus/kafka` | Custom Kafka image |
| `CASSANDRA_IMAGE` | `arcus/cassandra` | Custom Cassandra image |

### Kafka Configuration

| Variable | Default | Description |
|----------|---------|-------------|
| `KAFKA_REPLICATION` | `1` (dev) / `3` (prod) | Topic replication factor |
| `KAFKAOPS_REPLICATION` | `1` | Operations topic replication |
| `KAFKA_HSIZE` | `512` | Kafka heap size (MB) |
| `ADVERTISED_HSTN` | `kafka.eyeris` | Kafka advertised hostname |
| `ZOOKEEPER` | `zookeeper.eyeris:2181` | ZooKeeper connection string |

### Cassandra Configuration

| Variable | Default | Description |
|----------|---------|-------------|
| `CASSANDRA_REPLICATION` | `1` (dev) / `3` (prod) | Keyspace replication factor |
| `CASSANDRA_HEAPSIZE` | `512` | Cassandra heap size (MB) |
| `CASSANDRA_SINGLE_NODE` | `true` (dev) | Skip cluster discovery |
| `CASSANDRA_KEYSPACE` | `dev` | Primary keyspace name |
| `CASSANDRA_HOSTNAME` | `$HOSTNAME` | Cassandra listen address |

### Registry Configuration

| Variable | Default | Description |
|----------|---------|-------------|
| `REGISTRY_NAME` | (empty) | Registry hostname (e.g., `gcr.io/arcus-238802`) |
| `REGISTRY_SEPERATOR` | `/` | Path separator (`/` for GCR, `--` for GHCR) |
| `DOCKER_PREFIX_OVERRIDE` | (empty) | Replace `arcus` prefix in image names |
| `DOCKER_VERSION` | (from gradle) | Image version tag |

### Registry Examples

```bash
# Google Container Registry
REGISTRY_SEPERATOR='/' REGISTRY_NAME=gcr.io/arcus-238802 ./khakis/bin/tag.sh
REGISTRY_SEPERATOR='/' REGISTRY_NAME=gcr.io/arcus-238802 ./khakis/bin/push.sh

# GitHub Container Registry
REGISTRY_SEPERATOR='--' REGISTRY_NAME=ghcr.io/wl-net ./khakis/bin/tag.sh
REGISTRY_SEPERATOR='--' REGISTRY_NAME=ghcr.io/wl-net ./khakis/bin/push.sh
```

---

## Versioning

- `version.properties` in `khakis/` controls container image versions
- `./bin/build.sh` tags images as `latest`
- `./gradlew :khakis:distDocker` tags images with the version from `version.properties` (e.g., `2026.2.1`)
- Use `./gradlew branchRelease` and `tagRelease` for formal version bumps

---

## Troubleshooting

```bash
# Check running containers
docker ps

# Check container logs
docker logs eyeris-cassandra
docker logs eyeris-kafka

# Shell into a container
./khakis/bin/connect.sh eyeris-cassandra

# Force re-provision Kafka topics
docker exec eyeris-kafka kafka-provision -f

# Force re-provision Cassandra keyspaces
docker exec eyeris-cassandra cassandra-provision -f

# CQL shell
docker exec -it eyeris-cassandra cqlsh

# Check Kafka topics
docker exec eyeris-kafka kafka-cmd list-topics
```
