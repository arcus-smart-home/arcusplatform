# Cassandra Schema

## Keyspaces

All keyspaces use `SimpleStrategy` with a configurable replication factor (default 3).

| Keyspace | Purpose |
|----------|---------|
| `dev` (configurable via `$CASSANDRA_KEYSPACE`) | Primary application data — all core entities |
| `history` | Event history / audit log |
| `video` | Video recording data |
| `support` | Support tooling |
| `analytics` | Analytics (currently empty) |

## Schema Management

Schema is managed by **arcus-modelmanager**, a Liquibase-inspired tool using XML changelogs.

- **Keyspace creation:** `khakis/arcus-cassandra/cassandra-provision` (shell script with inline CQL)
- **Primary keyspace changelogs:** `platform/arcus-modelmanager/src/main/resources/changelogs/`
- **History changelogs:** `platform/arcus-modelmanager/src/dist/history-resources/changelogs/`
- **Video changelogs:** `platform/arcus-modelmanager/src/dist/video-resources/changelogs/`

The modelmanager tracks applied migrations in `changeset` and `versionhistory` tables (created automatically in each managed keyspace).

## Primary Keyspace Tables

### Account / Person / Login

**`account`** — billing account

| Column | Type | Notes |
|--------|------|-------|
| `id` | uuid | **PK** |
| `state` | varchar | Account state |
| `owner` | uuid | Person ID of account owner |
| `placeIDs` | set\<uuid\> | All place IDs for this account |
| `billable` | boolean | |
| `taxExempt` | boolean | |
| `billingFirstName`, `billingLastName`, `billingCCType`, `billingCCLast4`, billing address fields | varchar | |
| `created`, `modified` | timestamp | |

**`person`** — user entity (also used for service accounts with `hasLogin=false`)

| Column | Type | Notes |
|--------|------|-------|
| `id` | uuid | **PK** |
| `accountId` | uuid | |
| `firstName`, `lastName` | varchar | |
| `email` | varchar | |
| `mobileNumber` | varchar | |
| `hasLogin` | boolean | `false` for service accounts/guests |
| `pinPerPlace` | map\<varchar,varchar\> | placeId → AES-encrypted PIN |
| `securityAnswers` | map\<varchar,varchar\> | AES-encrypted |
| `currPlace` | uuid | Last-active place |
| `created`, `modified` | timestamp | |

**`login`** — email/password authentication index

| Column | Type | Notes |
|--------|------|-------|
| `domain` | varchar | **Partition key** — email domain (e.g. `gmail.com`) |
| `user_0_3` | varchar | **Partition key** — first 3 chars of local part |
| `user` | varchar | **Clustering key** — full email local part |
| `password` | varchar | SHA-256 hash (1024 iterations), Base64 |
| `password_salt` | varchar | Random salt, Base64 |
| `personId` | uuid | FK to person |
| `reset_token` | varchar | TTL'd (15 min default) |

Lookup: split email into domain + prefix to compute partition key, then filter by full `user`.

### Place

**`place`** — physical location / home

| Column | Type | Notes |
|--------|------|-------|
| `id` | uuid | **PK** |
| `accountId` | uuid | |
| `name` | varchar | |
| `serviceLevel` | varchar | `PREMIUM`, `BASIC` |
| `tzName` | varchar | e.g. `America/Chicago` |
| `addrLatitude`, `addrLongitude` | double | |
| `partitionId` | int | For partitioned platform routing (secondary index) |
| `ruleSequence` | int | Auto-increment counter for rule IDs |
| `actionSequence` | int | Auto-increment counter for scene IDs |
| Address fields | varchar | Street, city, state, zip |

### Device

**`device`** — device entity

| Column | Type | Notes |
|--------|------|-------|
| `id` | uuid | **PK** |
| `accountId` | uuid | Secondary index |
| `placeId` | uuid | |
| `hubId` | varchar | Hub serial number |
| `protocolName` | varchar | `ZIGBEE`, `ZWAVE`, `IPCD` |
| `protocolAddress` | varchar | |
| `driverName` | varchar | |
| `driverVersion` | varchar | |
| `caps` | set\<varchar\> | Capability namespaces |
| `name` | varchar | User-assigned name |
| `attributes` | map\<varchar,varchar\> | All device attributes (JSON-encoded values) |
| `protocolattrs` | blob | Protocol-specific attributes |
| `variables` | blob | Driver variable state |

Lookup indexes (separate tables with matching partition/clustering keys):

| Table | Partition Key | Clustering Key | Returns |
|-------|--------------|----------------|---------|
| `device_placeid` | `placeId` | `devId` | Devices at a place |
| `device_hubid` | `hubId` | `devId` | Devices on a hub |
| `device_protocoladdress` | `protocolAddress` | — | Device by protocol address |

### Hub

**`hub`** — hub entity

| Column | Type | Notes |
|--------|------|-------|
| `id` | varchar | **PK** — e.g. `ABC-1234` |
| `accountId` | uuid | |
| `placeId` | uuid | |
| `state` | varchar | |
| `registrationState` | varchar | |
| `osVer`, `agentVer`, `firmwareGroup` | varchar | |
| `partitionId` | int | Secondary index |
| `attributes` | map\<varchar,varchar\> | Hub capability attributes |

Lookup indexes:

| Table | Partition Key | Clustering Key | Returns |
|-------|--------------|----------------|---------|
| `hub_placeid` | `placeId` | `hubId` | Hub at a place |
| `hub_accountid` | `accountId` | `hubId` | Hubs for an account |
| `hub_macaddr` | `macaddr_0_7` | `macaddr` | Hub by MAC address |

**`hub_registration`** — firmware upgrade state (PK: hub ID).

**`hub_blacklist`** — blacklisted hub certificates (PK: certificate serial number).

### Authorization

**`authorization_grant`** — per-person-per-place access grants

| Column | Type | Notes |
|--------|------|-------|
| `entityId` | uuid | **Partition key** — person UUID |
| `placeId` | uuid | **Clustering key** |
| `accountId` | uuid | |
| `accountOwner` | boolean | |
| `permissions` | set\<varchar\> | Shiro wildcard permission strings |

**`authorization_grant_by_place`** — same columns, reversed keys (partition: `placeId`, clustering: `entityId`). Both tables updated atomically in a `BatchStatement`.

### Sessions / Auth

**`sessions`** — Shiro web sessions (PK: `timeuuid`). Stores serialized session data with `gc_grace_seconds = 86400`.

**`app_handoff_token`** — short-lived tokens for app-to-app session transfer (PK: `handoffToken`).

### OAuth

**`oauth`** — token store

| Column | Type | Notes |
|--------|------|-------|
| `appid` | varchar | **Partition key** |
| `tok_0_2` | varchar | **Partition key** — first 3 chars of token |
| `tok` | varchar | **Clustering key** — full token |
| `type` | varchar | **Clustering key** — `CODE`, `ACCESS`, `REFRESH` |
| `person` | uuid | |

TTL set per token type at insert time.

**`person_oauth`** — per-person per-app relationship (PK: `(person, appid)`). Stores current access/refresh tokens.

### Rules / Scenes

**`ruleenvironment`** — unified table for rules and scenes

| Column | Type | Notes |
|--------|------|-------|
| `placeId` | uuid | **Partition key** |
| `type` | varchar | **Clustering key** — `rule` or `scene` |
| `id` | int | **Clustering key** — auto-incrementing within place+type |
| `name`, `description` | varchar | |
| `ruleTemplate` | varchar | Template ID (rules only) |
| `ruleDisabled`, `ruleSuspended` | boolean | Rules only |
| `action` | blob | Scene actions |

IDs are generated by incrementing `place.ruleSequence` / `place.actionSequence` with lightweight transactions.

### Subsystem State

**`subsystem`** — per-place per-subsystem persisted state

| Column | Type | Notes |
|--------|------|-------|
| `placeId` | uuid | **Partition key** |
| `namespace` | varchar | **Clustering key** — e.g. `subsecurity`, `subcare` |
| `attributes` | map\<varchar,varchar\> | All subsystem model attributes, JSON-encoded |

All subsystem state (alarm state, device sets, mode, etc.) is packed into the `attributes` map.

### Scheduler

**`scheduler`** — scheduler model entities (PK: `id` uuid). One per scheduled device at a place.

**`scheduled_event`** — time-bucketed queue of upcoming events

| Column | Type | Notes |
|--------|------|-------|
| `partitionId` | int | **Partition key** |
| `timeBucket` | timestamp | **Partition key** |
| `scheduledTime` | timestamp | **Clustering key** (ASC) |
| `scheduler` | varchar | **Clustering key** — scheduler address |

### Alarm Incidents

**`alarmincident`** — alarm incident records

| Column | Type | Notes |
|--------|------|-------|
| `placeid` | uuid | **Partition key** |
| `incidentid` | timeuuid | **Clustering key** (DESC) |
| `alertState`, `platformState`, `hubState` | varchar | |
| `monitoringState` | text | |
| `tracker` | list\<text\> | Event timeline |

### Invitations

**`invitation`** — place invitation to a guest (PK: `code` varchar, TTL 7 days).

Indexes: `invitation_place_idx` (place → codes), `invitation_person_idx` (invitee → codes).

### Other Tables

| Table | Purpose | PK |
|-------|---------|-----|
| `mobiledevices` | Push notification endpoints per person | `(personId, deviceIndex)` |
| `notificationtoken_mobiledevice` | Reverse index: token → person | `notificationToken` |
| `preferences` | Per-person per-place UI preferences | `(personId, placeId)` |
| `ipcd_device` | WiFi/IP device registration | `protocolAddress` |
| `pairing_device` | In-progress pairing state | `(placeId, protocolAddress)` |
| `device_driver` | Groovy driver scripts stored in DB | `(name, version)` |
| `service` | Cluster membership / leader election | `(service, clusterId)` |
| `notification_audit` | Notification delivery audit log | `(id, time)` |
| `cellbackup_time` | Cellular backup usage tracking | `(dayhour, minute, hubId)` |
| `resource_bundle` | Localized strings | `((bundle, locale), key)` |

## History Keyspace

All history tables use `DateTieredCompactionStrategy` with `gc_grace_seconds = 86400`. TTLs are set at write time.

Each table shares a common structure: partition key scopes the entity, `time` (timeuuid DESC) is the clustering key, with `subjectAddress`, `messageKey`, and `params` columns.

| Table | Partition Key | Purpose |
|-------|--------------|---------|
| `histlog_place_detailed` | `placeId` | Full event log per place |
| `histlog_place_critical` | `placeId` | Critical-only events (alarms) |
| `histlog_person_detailed` | `personId` | Events per person |
| `histlog_device_detailed` | `deviceId` | Events per device |
| `histlog_hub_detailed` | `hubId` | Events per hub |
| `histlog_rule_detailed` | `(placeId, ruleId)` | Events per rule |
| `histlog_subsys_detailed` | `(placeId, subsystem)` | Events per subsystem |
| `histlog_alarm_detailed` | `incidentId` | Events per alarm incident |
| `histlog_care_activity` | `placeId` | Care subsystem sensor activity |

## Video Keyspace

Video uses V2 tables (V1 tables were dropped in 2018.9.0). Recordings are stored as time-series segments with `TimeWindowCompactionStrategy`.

| Table | Purpose | PK |
|-------|---------|-----|
| `recording_v2` | Video segment data | `(recordingid, expiration, ts, bo)` |
| `recording_metadata_v2` | Metadata fields per recording | `(recordingid, expiration, field)` |
| `place_recording_index_v2` | Recordings indexed by place | `((placeid, field), value, recordingid)` |
| `recording_v2_favorite` | Favorited recording data (no TTL) | `(recordingid, ts, bo)` |
| `recording_metadata_v2_favorite` | Favorite metadata | `(recordingid, field)` |
| `place_recording_index_v2_favorite` | Favorite recordings by place | `((placeid, field), value, recordingid)` |
| `purge_recordings_v2` | Scheduled deletion queue | `((deletetime, partitionid), recordingid, placeid)` |
| `place_purge_recording` | Places with recordings to purge | `(deletetime, placeid)` |

## Key Source Files

| Path | Purpose |
|------|---------|
| `platform/arcus-modelmanager/src/main/resources/changelogs/` | Primary keyspace schema changelogs |
| `platform/arcus-modelmanager/src/dist/history-resources/changelogs/` | History keyspace changelogs |
| `platform/arcus-modelmanager/src/dist/video-resources/changelogs/` | Video keyspace changelogs |
| `khakis/arcus-cassandra/cassandra-provision` | Keyspace creation script |
| `platform/arcus-lib/src/main/java/com/iris/core/dao/cassandra/` | Core DAO implementations |
| `platform/arcus-lib/src/main/java/com/iris/platform/history/cassandra/` | History DAO and table definitions |
| `platform/arcus-lib/src/main/java/com/iris/platform/scheduler/cassandra/` | Scheduler tables |
| `platform/arcus-oauth/src/main/java/com/iris/oauth/dao/CassandraOAuthDAO.java` | OAuth token DAO |
| `platform/arcus-video/src/main/java/com/iris/video/cql/v2/` | Video table Java classes |
