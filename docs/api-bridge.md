# API Bridge

The api-bridge provides programmatic access to the Arcus platform via API keys and WebSocket connections. It is built on the same bridge-common framework as the client-bridge but uses bearer token authentication instead of session cookies, making it suitable for automation, integrations, and machine-to-machine communication.

Each API key is scoped to a single place and carries a fixed set of permissions. There are no interactive login flows — authentication happens inline during the WebSocket handshake.

## API Key Management

API keys are managed by sending messages to the place service (`base:place`). Only the account owner can create, list, revoke, and delete keys.

### Create

Send `apikey:Create` to the place address:

```json
{
  "type": "apikey:Create",
  "destination": "SERV:place:<placeId>",
  "attributes": {
    "label": "my-integration",
    "permissions": ["device:*", "scene:*"],
    "expiresAt": 1735689600000
  }
}
```

| Field | Required | Description |
|-------|----------|-------------|
| `label` | yes | Human-readable name, max 64 characters |
| `permissions` | yes | Non-empty set of Shiro wildcard permission strings |
| `expiresAt` | no | Expiration timestamp in milliseconds (must be in the future) |

The response contains the raw key — **this is the only time the full key is returned**:

```json
{
  "type": "apikey:CreateResponse",
  "attributes": {
    "id": "a1b2c3d4-...",
    "key": "arcus_sk_0a1b2c3d4e5f6a7b8c9d0e1f2a3b4c5d"
  }
}
```

Limits: maximum 10 keys per place.

### List

Send `apikey:ListKeys` to the place address. Returns all keys for the place with their metadata but never the full key — only a `keyPrefix` (e.g. `arcus_sk_0a1b2c3d`) for identification.

### Revoke

Send `apikey:Revoke` with the key's `id`. This sets `expiresAt` to the current time, immediately expiring the key. All active WebSocket sessions using the revoked key are disconnected. The key record is preserved for auditing.

### Delete

Send `apikey:Delete` with the key's `id`. This permanently removes the key record. The key must be revoked before it can be deleted — attempting to delete an active key returns an error.

```json
{
  "type": "apikey:Delete",
  "destination": "SERV:place:<placeId>",
  "attributes": {
    "id": "a1b2c3d4-..."
  }
}
```

## Authentication

The api-bridge authenticates via bearer token during the WebSocket upgrade handshake. No separate login step is needed.

Connect to the WebSocket endpoint with an `Authorization` header:

```
GET /apibus HTTP/1.1
Upgrade: websocket
Authorization: Bearer arcus_sk_0a1b2c3d4e5f6a7b8c9d0e1f2a3b4c5d
```

On success the server completes the WebSocket upgrade and emits a `client:session:created` message:

```json
{
  "type": "client:session:created",
  "attributes": {
    "personId": "<uuid>",
    "placeId": "<uuid>",
    "keyId": "<uuid>",
    "label": "my-integration",
    "places": [{"placeId": "<uuid>", "name": "Home", "accountId": "<uuid>", "role": "OWNER"}]
  }
}
```

On failure the server responds with HTTP 401.

## Permissions

Permissions use [Apache Shiro wildcard format](https://shiro.apache.org/permissions.html). Examples:

| Permission | Grants |
|-----------|--------|
| `device:*` | All device operations |
| `device:get` | Read device state only |
| `scene:execute` | Execute scenes |
| `subsystem:*` | All subsystem operations |

Each permission string is validated at key creation time. Invalid formats are rejected.

The key's place is set automatically and cannot be changed — `SetActivePlace` and other session-service messages are rejected. ListPlaces responses are filtered to return only the key's place.

## Connection Flow

1. **Create an API key** — send `apikey:Create` via an authenticated client-bridge session (account owner only)
2. **Store the raw key** — it is only returned once at creation time
3. **Open a WebSocket** — connect to `ws://<host>:8085/apibus` with `Authorization: Bearer <key>`
4. **Receive session message** — server sends `client:session:created` with place info
5. **Send and receive messages** — use standard `ClientMessage` JSON format, same as client-bridge

### Message Format

```json
{
  "type": "<message_type>",
  "destination": "<address>",
  "correlationId": "<optional_id>",
  "attributes": { ... }
}
```

Messages are authorized against the key's permission set and routed to the platform bus. The actor address for API key sessions is `SERV:<keyId>/apikey`.

## Configuration

Default configuration (`api-bridge.properties`):

| Property | Default | Description |
|----------|---------|-------------|
| `port` | `8085` | WebSocket server port |
| `web.socket.path` | `apibus` | WebSocket upgrade path |
| `healthcheck.http.port` | `9081` | HTTP health check port |
| `bridge.name` | `api` | Bridge identifier |
| `kafka.group` | `api-bridge` | Kafka consumer group |

## Security Notes

- **One-time key display** — the raw key is returned only in the `CreateResponse`. It is never stored or retrievable afterward.
- **SHA-256 hashed storage** — only a SHA-256 hash of the key is persisted. Authentication works by hashing the presented key and looking up the hash.
- **Key format** — keys are prefixed with `arcus_sk_` followed by 32 hex characters (128 bits of entropy from `SecureRandom`).
- **Place isolation** — a key can only access resources within its assigned place. The active place is locked at session creation and cannot be changed.
- **Immediate revocation** — revoking a key disconnects all active sessions using it across all api-bridge instances (via platform bus event).
- **Per-place limits** — maximum 10 keys per place to prevent abuse.
- **Account owner only** — only the account owner can create, list, revoke, and delete API keys.
