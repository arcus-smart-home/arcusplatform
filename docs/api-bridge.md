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
    "permissions": ["place:x:*", "base:r:*", "dev:r:*", "swit:r:*", "swit:w:*"],
    "expiresAt": 1735689600000
  }
}
```

| Field | Required | Description |
|-------|----------|-------------|
| `label` | yes | Human-readable name, max 64 characters |
| `permissions` | yes | Non-empty set of permission strings, see [Permissions](#permissions) |
| `expiresAt` | no | When the key stops working, in milliseconds since the epoch (UTC), as a number or a string of digits. Must be in the future. Any other value is rejected; leave it out for a key that doesn't expire |

The response contains the raw key — **this is the only time the full key is returned**. Platform logs print `<redacted>` in place of its attributes:

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

Revoking a key that has already expired or been revoked succeeds and leaves its `expiresAt` as it was. It sends the revocation again, so it also ends any session still using the key, for example one that missed the first revocation.

### Delete

Send `apikey:Delete` with the key's `id`. This permanently removes the key record and disconnects any session still using it. The key must be revoked before it can be deleted — attempting to delete an active key returns an error.

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

A key's permissions use the same format as a person's grants (see [Security](security.md)): [Shiro wildcard](https://shiro.apache.org/permissions.html) strings with three parts, `<namespace>:<operation>:<object id>`.

- **namespace**: a capability or service namespace, the part before the colon in an attribute or message name, such as `dev`, `swit`, `scene` or `subalarm`.
- **operation**: `r` (read), `w` (write) or `x` (execute), or `c` or `d`.
- **object id**: the id of the object the message goes to or comes from, such as a device or the place.

Any part can be `*`, or a comma-separated list such as `r,w`. A shorter string covers everything after it, so `swit:w` means `swit:w:*` and `*` means `*:*:*`.

Each message is checked like this:

| Message | Needs |
|---------|-------|
| `base:GetAttributes` | `<namespace>:r:<object>` for the namespace of each attribute asked for |
| `base:SetAttributes` | `<namespace>:w:<object>` for the namespace of each attribute set |
| Any other request, such as `scene:Fire` | `<namespace of the request>:x:<object>`, such as `scene:x:*` |
| Events and responses sent to the key | `<namespace>:r:<source>` for each attribute; attributes the key can't read are removed, and `place:ListDevices` leaves out a device the key can read nothing of |

Examples:

| Permissions | Allow |
|-------------|-------|
| `*:*:*` | Everything the place's full-access people can do, still limited to the key's place and never account-owner-only requests. A Home Assistant bridge such as arcus-mqtt-server, which lists and controls devices, scenes, the alarm and people, needs this |
| `place:x:*`, `base:r:*`, `dev:r:*`, `swit:r:*` | List the devices and read their names and switch state |
| `swit:w:*` | Turn switches on and off (`base:SetAttributes` with `swit:state`) |
| `scene:x:*` | Fire scenes (`scene:Fire`) |
| `subalarm:x:*` | Arm and disarm the alarm (`subalarm:Arm`, `subalarm:Disarm`) |

Oculus's API key dialog builds `<namespace>:r:*`, `<namespace>:w:*` and `<namespace>:x:*` from the list of capabilities and services; enter anything else, such as `*:*:*`, with **Add Custom**.

`apikey:Create` rejects a string Shiro can't parse, and one whose operation is none of `*`, `r`, `w`, `x`, `c` or `d` (such as `device:get`), since it could never allow anything. It doesn't check the namespace: `device:*` is accepted but allows nothing, because devices are `dev`.

Attributes without a namespace are checked with their own name in place of one. `scene:ListScenesResponse` returns its list in `scenes`, which needs `scenes:r:*`, and `subs:ListSubsystemsResponse` needs `subsystems:r:*`. Error responses carry `code` and `message`, and a key that can't read those gets no error response at all, so a key needs `*` as its first part to see errors: `*:*:*`, or `*:r:*` next to narrower write and execute permissions.

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
| `kafka.offsets.transient` | `true` | After a restart, start at the latest message instead of replaying the ones missed, like the other bridges |

## Security Notes

- **One-time key display** — the raw key is returned only in the `CreateResponse`. It is never stored or retrievable afterward.
- **SHA-256 hashed storage** — only a SHA-256 hash of the key is persisted. Authentication works by hashing the presented key and looking up the hash.
- **Key format** — keys are prefixed with `arcus_sk_` followed by 32 hex characters (128 bits of entropy from `SecureRandom`).
- **Place isolation** — a key can only access resources within its assigned place. The active place is locked at session creation and cannot be changed.
- **Immediate revocation** — revoking or deleting a key disconnects all active sessions using it across all api-bridge instances (via a platform bus event from the key's place). Each session also checks its key again once it is registered, so a revocation made while it was connecting isn't missed, and a session ends when its key's `expiresAt` passes.
- **Per-place limits** — maximum 10 keys per place to prevent abuse.
- **Account owner only** — only the account owner can create, list, revoke, and delete API keys.
