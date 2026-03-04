# Client-Bridge WebSocket Protocol

This document describes what a consumer of the client-bridge WebSocket API needs to do to connect, authenticate, and interact with the Arcus platform.

The [api-bridge](api-bridge.md) is a separate service built on the same `bridge-common` framework. It shares the same wire format, message routing, and event model, but differs in authentication (bearer token instead of session cookie), connection flow (no login step), and authorization (API key permissions instead of user roles). Much of this document — particularly the wire format (Section 5), error codes (Section 6), broadcast events (Section 7), and session lifecycle mechanics (Section 8) — applies to both bridges.

## 1. Authenticate via HTTP

POST credentials to obtain a session cookie before opening the WebSocket.

**Endpoint:** `POST /login` (also accepts `POST /`)

The server accepts two content types, tried in order:

### Form-encoded (`application/x-www-form-urlencoded`)

| Field | Required | Description |
|---|---|---|
| `user` | yes* | Username (email address) |
| `password` | yes* | Password |
| `token` | yes* | App handoff token (alternative to user/password) |
| `public` | no | `"true"` for public/shared devices — uses a shorter session timeout |

\* Provide either `user` + `password`, or `token`.

```
POST /login HTTP/1.1
Content-Type: application/x-www-form-urlencoded

user=alice%40example.com&password=secret
```

### JSON (`application/json`)

Falls back to JSON parsing if form decoding yields no credentials. Note two differences from form-encoded:
- The username field is `username`, not `user`
- The `public` field is **non-functional** due to a bug (`"true".equalsIgnoreCase("public")` — always false)

| Field | Required | Description |
|---|---|---|
| `username` | yes* | Username (email address) |
| `password` | yes* | Password |
| `token` | yes* | App handoff token (alternative to username/password) |
| `public` | no | `"true"` for public/shared devices — uses a shorter session timeout |

```
POST /login HTTP/1.1
Content-Type: application/json

{"username": "alice@example.com", "password": "secret"}
```

### Response

**Success (200):**

```
HTTP/1.1 200 OK
Set-Cookie: irisAuthToken=<session-id>; Max-Age=1209600; Path=/; HttpOnly; Secure

{"status":"success"}
```

The cookie name is configurable via `auth.cookie.name`. The cookie is `HttpOnly`, scoped to `/`, and `Secure` by default (configurable via `auth.cookie.secure`). If `domain.name` is set, the cookie includes a `Domain` attribute — this is important for cross-origin browser deployments where the web app and client-bridge are on different subdomains. See [CORS and Browser Integration](#cors-and-browser-integration).

**Bad request (400):** No credentials could be extracted from the request body.

**Unauthorized (401):** Credentials were invalid. The response expires the auth cookie and includes `Connection: close`.

**Service unavailable (503):** Could not establish a client session (internal error).

## 2. Open the WebSocket

Connect to `ws(s)://host:8081/websocket` with the session cookie included in the HTTP upgrade request:

```
GET /websocket HTTP/1.1
Host: <host>:8081
Upgrade: websocket
Connection: Upgrade
Sec-WebSocket-Key: <key>
Sec-WebSocket-Version: 13
Cookie: irisAuthToken=<value>
```

The server validates the cookie via `SessionAuth` during the upgrade handshake. If authentication fails the upgrade is rejected with HTTP 401 and the auth cookie is expired.

### WebSocket Path

The configured path is `androidbus` (set via `web.socket.path` in `client-bridge.properties`), a legacy name from the original Android app. However, the server's `WebSocketUpgradeMatcher` only checks for the `Upgrade: websocket` header — **the path is not validated**. Any path will be accepted for the WebSocket upgrade as long as it doesn't match a more specific HTTP handler first.

The configured path is used server-side only in the `Sec-WebSocket-Location` response header during the handshake. In practice:

| Client | Path Used | Notes |
|---|---|---|
| Android/iOS apps | `/androidbus` | Matches the configured `web.socket.path` |
| Oculus (desktop test tool) | `/websocket` | Uses its own hardcoded path — works because the path is not enforced |
| API bridge | `/apibus` | Separate bridge service on port 8085 |

Since the path is not enforced, new clients should use `/websocket` (the path used by Oculus) rather than `/androidbus`, which is specific to the Android app.

## 3. Receive `SessionCreated`

Immediately after the handshake completes, the server pushes a `SessionCreated` message containing session context and configuration URLs:

```json
{
  "type": "SessionCreated",
  "headers": {},
  "payload": {
    "messageType": "SessionCreated",
    "attributes": {
      "personId": "<uuid>",
      "places": [
        {
          "placeId": "<uuid>",
          "placeName": "Home",
          "accountId": "<uuid>",
          "role": "OWNER"
        }
      ],
      "requiresTermsAndConditionsConsent": false,
      "requiresPrivacyPolicyConsent": false,
      "smartyAuthID": "<smarty-streets-id>",
      "smartyAuthToken": "<smarty-streets-token>",
      "tokenURL": "<recurly-token-url>",
      "publicKey": "<recurly-public-api-key>",
      "cameraPreviewBaseUrl": "<url>",
      "staticResourceBaseUrl": "<url>",
      "secureStaticResourceBaseUrl": "<url>",
      "redirectBaseUrl": "<url>",
      "androidLaunchUrl": "<url>/android/launch",
      "iosLaunchUrl": "<url>/ios/launch",
      "webLaunchUrl": "<url>/web/launch"
    }
  }
}
```

| Field | Description |
|---|---|
| `personId` | UUID of the authenticated user |
| `places` | List of places the user can access, each with `placeId`, `placeName`, `accountId`, and `role` |
| `requiresTermsAndConditionsConsent` | Whether the user must accept updated terms (always `false` for non-owners) |
| `requiresPrivacyPolicyConsent` | Whether the user must accept updated privacy policy (always `false` for non-owners) |
| `smartyAuthID` | SmartyStreets address validation auth ID |
| `smartyAuthToken` | SmartyStreets address validation auth token |
| `tokenURL` | Recurly billing token URL |
| `publicKey` | Recurly public API key |
| `cameraPreviewBaseUrl` | Base URL for camera preview images |
| `staticResourceBaseUrl` | Base URL for static resources |
| `secureStaticResourceBaseUrl` | HTTPS base URL for static resources |
| `redirectBaseUrl` | Base URL for redirects |
| `androidLaunchUrl` | Android app deep-link launch URL |
| `iosLaunchUrl` | iOS app deep-link launch URL |
| `webLaunchUrl` | Web app launch URL |

Receiving this message confirms that the session is authenticated. There is no separate REST endpoint to check session validity.

## 4. Set Active Place

Before interacting with devices or services at a place, the client must set the active place:

**Request:**

```json
{
  "type": "sess:SetActivePlaceRequest",
  "headers": {
    "destination": "SERV:sess:",
    "correlationId": "<unique-id>",
    "isRequest": true
  },
  "payload": {
    "messageType": "sess:SetActivePlaceRequest",
    "attributes": {
      "placeId": "<place-uuid>"
    }
  }
}
```

**Response:**

```json
{
  "type": "sess:SetActivePlaceResponse",
  "headers": {
    "source": "SERV:sess:",
    "correlationId": "<unique-id>"
  },
  "payload": {
    "messageType": "sess:SetActivePlaceResponse",
    "attributes": {
      "placeId": "<place-uuid>",
      "preferences": {}
    }
  }
}
```

## 5. Send and Receive Platform Messages

### Wire Format

All messages are JSON `ClientMessage` objects serialized with this structure:

```json
{
  "type": "<message-type>",
  "headers": {
    "source": "<sender-address>",
    "destination": "<target-address>",
    "correlationId": "<uuid>",
    "isRequest": true
  },
  "payload": {
    "messageType": "<message-type>",
    "attributes": { }
  }
}
```

| Field | Location | Description |
|---|---|---|
| `type` | top-level | Message type, mirrors `payload.messageType` |
| `source` | `headers` | Address of the sender (set by server on responses/events) |
| `destination` | `headers` | Target address (set by client on requests) |
| `correlationId` | `headers` | UUID for request/response pairing |
| `isRequest` | `headers` | `true` if this message expects a response |
| `messageType` | `payload` | Same as top-level `type` |
| `attributes` | `payload` | Message-type-specific key/value pairs |

### Device Commands

Send commands directly to a device address:

```json
{
  "type": "base:SetAttributes",
  "headers": {
    "destination": "DRIV:dev:abc-123",
    "correlationId": "cmd-1",
    "isRequest": true
  },
  "payload": {
    "messageType": "base:SetAttributes",
    "attributes": {
      "swit:state": "ON"
    }
  }
}
```

### Service Requests

Send requests to service addresses such as `SERV:sess:`, `SERV:place:`, etc.

### Message Routing

- Messages addressed to `SERV:sess:` are handled locally by `ClientRequestDispatcher`
- All other messages are forwarded to the platform bus for device/service handling
- Each message is authorization-checked; the user must have appropriate permissions for the target place
- For API key sessions, the actor namespace is `apikey` instead of `person`
- `video:ListRecordingsRequest` is intercepted and always returns an empty recordings list without forwarding to the platform bus

### Session Messages Reference

These messages are handled locally by the client-bridge (addressed to `SERV:sess:`).

#### `sess:SetActivePlace`

Set the working place. Required before interacting with devices or services. See [Section 4](#4-set-active-place) for request/response examples.

**Request attributes:**

| Attribute | Type | Required | Description |
|---|---|---|---|
| `placeId` | string | yes | UUID of the place to activate |

**Response attributes (`sess:SetActivePlaceResponse`):**

| Attribute | Type | Description |
|---|---|---|
| `placeId` | string | The activated place ID |
| `preferences` | map | User preferences at the place (may be null) |

Errors: `missing.attribute` if `placeId` is absent, `error.unauthorized` if the user has no permissions on the place.

#### `sess:ListAvailablePlaces`

List all places accessible to the authenticated user. No request attributes.

**Response attributes (`sess:ListAvailablePlacesResponse`):**

| Attribute | Type | Description |
|---|---|---|
| `places` | list | List of `PlaceAccessDescriptor` objects |

Each `PlaceAccessDescriptor`:

| Field | Type | Description |
|---|---|---|
| `placeId` | string | UUID |
| `name` | string | Place name |
| `streetAddress1` | string | Street address line 1 |
| `streetAddress2` | string | Street address line 2 |
| `city` | string | City |
| `state` | string | State/province |
| `zipCode` | string | ZIP/postal code |
| `role` | string | `OWNER`, `FULL_ACCESS`, `HOBBIT`, or `OTHER` |
| `primary` | boolean | True if account owner's primary place |
| `promonAd` | boolean | True if promo ad should be shown |

#### `sess:GetPreferences`

Get the user's preferences at the active place. No request attributes. Returns empty body if no active place is set.

**Response attributes (`sess:GetPreferencesResponse`):**

| Attribute | Type | Description |
|---|---|---|
| `prefs` | map | Preferences object (see [Preferences](#preferences-type) below) |

#### `sess:SetPreferences`

Merge preferences at the active place. Uses merge semantics — only the provided keys are updated.

**Request attributes:**

| Attribute | Type | Required | Description |
|---|---|---|---|
| `prefs` | map | yes | Partial or complete preferences to merge (see [Preferences](#preferences-type) below) |

**Response:** Empty body on success. Emits `sess:PreferencesChanged` event to other sessions.

Errors: `place.active.notSet` if no active place.

#### `sess:ResetPreference`

Delete a single preference key, reverting it to its default.

**Request attributes:**

| Attribute | Type | Required | Description |
|---|---|---|---|
| `prefKey` | string | yes | The preference key to reset (e.g. `"hideTutorials"`, `"dashboardCards"`) |

**Response:** Empty body on success.

Errors: `place.active.notSet` if no active place.

#### `sess:Log`

Submit a client log entry. Logged server-side at INFO level. Works before and after setting an active place.

**Request attributes:**

| Attribute | Type | Required | Description |
|---|---|---|---|
| `category` | string | no | Log category (defaults to `[notset]`) |
| `code` | string | no | Event code (defaults to `[notset]`) |
| `message` | string | no | Context message (defaults to `[none]`) |

**Response:** Empty body.

#### `sess:Tag`

Submit an analytics tag. Emits a `sess:Tagged` event to the analytics bus. Works before and after setting an active place.

**Request attributes:**

| Attribute | Type | Required | Description |
|---|---|---|---|
| `name` | string | yes | Tag name |
| `context` | map&lt;string, string&gt; | no | Additional key-value context. A `service.level` key, if present, is extracted into the event's `serviceLevel` field. |

**Response:** Empty body.

#### Preferences Type

Preferences are a map with two defined keys:

| Key | Type | Default | Description |
|---|---|---|---|
| `hideTutorials` | boolean | `false` | Whether to suppress tutorial screens |
| `dashboardCards` | list&lt;CardPreference&gt; | All 12 cards, visible | Ordered list of dashboard cards with visibility |

Each `CardPreference`:

| Field | Type | Required | Description |
|---|---|---|---|
| `serviceName` | string | yes | One of: `FAVORITES`, `HISTORY`, `LIGHTS_N_SWITCHES`, `ALARMS`, `CLIMATE`, `DOORS_N_LOCKS`, `CAMERAS`, `CARE`, `HOME_N_FAMILY`, `LAWN_N_GARDEN`, `WATER`, `SANTA_TRACKER` |
| `hideCard` | boolean | no | Whether this card is hidden (default `false`) |

Default card order: `SANTA_TRACKER`, `FAVORITES`, `HISTORY`, `LIGHTS_N_SWITCHES`, `ALARMS`, `CLIMATE`, `DOORS_N_LOCKS`, `CAMERAS`, `CARE`, `HOME_N_FAMILY`, `LAWN_N_GARDEN`, `WATER`. Duplicate `serviceName` values in a single `SetPreferences` call are rejected.

## 6. Error Responses

When a request fails, the server returns an error message with `type: "Error"`. The `correlationId` from the original request is echoed back so the client can match the error to its pending request.

```json
{
  "type": "Error",
  "headers": {
    "source": "SERV:sess:",
    "destination": "<client-address>",
    "correlationId": "<echoed-from-request>"
  },
  "payload": {
    "messageType": "Error",
    "attributes": {
      "code": "request.unsupported",
      "message": "Unsupported message type sess:BadRequest"
    }
  }
}
```

### Error Code Reference

**Framework errors** (from `Errors.java`):

| Code | Description |
|---|---|
| `error` | Generic uncaught exception ("Oops. I'm not sure what happened, but you might want to try again.") |
| `request.invalid` | Bad request format or structure |
| `request.param.missing` | Required parameter missing |
| `request.param.invalid` | Invalid parameter value |
| `request.timeout` | Request processing timed out |
| `request.cancelled` | Request was cancelled |
| `request.destination.notfound` | No object at the addressed destination. Also returned for authorization failures (to avoid leaking information). |
| `unsupported.message` | Unknown message type sent to `ClientRequestDispatcher` |
| `unknown.destination` | Service address not found |
| `service.unavailable` | Handler thread pool rejected the request (`RejectedExecutionException`) |

**Session handler errors:**

| Code | Description |
|---|---|
| `missing.attribute` | `placeId` missing from `SetActivePlace` request |
| `error.unauthorized` | User lacks permission on the target place |
| `place.active.notSet` | `SetPreferences` or `ResetPreference` called before `SetActivePlace` |

**Device/attribute errors:**

| Code | Description |
|---|---|
| `UnsupportedAttribute` | Attempted to set a non-writable attribute |
| `NoSuchAttribute` | Attribute does not exist on the device |
| `ReadOnlyAttribute` | Attribute is read-only |
| `UnknownDevice` | Hub is not currently connected |

**Person/PIN errors:**

| Code | Description |
|---|---|
| `person.notFound` | Person record not found |
| `pin.notUniqueAtPlace` | PIN is already in use at this place |
| `pin.invalid` | Invalid PIN format |
| `MismatchedPins` | PIN verification mismatch |
| `token.invalid` | Invalid password reset token |

**Exception class hierarchy** — uncaught exceptions are converted to error codes via `Errors.fromException()`:

| Exception | Resulting Code |
|---|---|
| `InvalidRequestException` | `request.invalid` |
| `MissingParameterException` | `request.param.missing` |
| `NotFoundException` | `request.destination.notfound` |
| `UnauthorizedRequestException` | `request.destination.notfound` (intentionally obscured) |
| Any other `Throwable` | `error` |

## 7. Server-Pushed Events

The server pushes unsolicited messages to connected clients. These have no `correlationId` and `isRequest` is absent or `false`.

### Broadcast Events

Sent to all clients whose active place matches the event's place. These are platform messages filtered through the user's authorization context.

#### `base:ValueChange`

Attributes of an object changed. The payload contains only the changed attributes and their new values.

```json
{
  "type": "base:ValueChange",
  "headers": { "source": "DRIV:dev:abc-123" },
  "payload": {
    "messageType": "base:ValueChange",
    "attributes": {
      "swit:state": "ON",
      "dev:lastchange": 1677000000000
    }
  }
}
```

#### `base:Added`

A new object was added at the place. The payload contains the complete object state.

```json
{
  "type": "base:Added",
  "headers": { "source": "DRIV:dev:abc-123" },
  "payload": {
    "messageType": "base:Added",
    "attributes": {
      "base:id": "<uuid>",
      "base:address": "DRIV:dev:abc-123",
      "base:type": "device",
      "base:caps": ["base", "dev", "swit"],
      "dev:vendor": "Iris",
      "dev:model": "Smart Switch",
      "swit:state": "OFF"
    }
  }
}
```

#### `base:Deleted`

An object was removed from the place. The payload contains the complete object state at deletion time.

```json
{
  "type": "base:Deleted",
  "headers": { "source": "DRIV:dev:abc-123" },
  "payload": {
    "messageType": "base:Deleted",
    "attributes": { "...complete object state..." }
  }
}
```

### Targeted Session Events

#### `sess:SessionExpired`

Sent immediately before the WebSocket is closed with status 4001. Triggers include: Shiro session expiry, per-message auth check failure, password change, person deletion.

```json
{
  "type": "sess:SessionExpired",
  "headers": { "source": "SERV:sess:" },
  "payload": {
    "messageType": "sess:SessionExpired",
    "attributes": {}
  }
}
```

#### `sess:PreferencesChanged`

User preferences were updated via `SetPreferences`. Sent only to sessions matching the same person **and** active place.

```json
{
  "type": "sess:PreferencesChanged",
  "headers": { "source": "SERV:sess:" },
  "payload": {
    "messageType": "sess:PreferencesChanged",
    "attributes": {
      "prefs": {
        "hideTutorials": false,
        "dashboardCards": [
          { "serviceName": "FAVORITES", "hideCard": false },
          { "serviceName": "LIGHTS_N_SWITCHES", "hideCard": true }
        ]
      }
    }
  }
}
```

| Attribute | Type | Description |
|---|---|---|
| `prefs` | map | Complete merged preferences after the update (same structure as [Preferences type](#preferences-type)) |

#### `sess:ActivePlaceCleared`

The client's active place was forcibly cleared. The client should prompt the user to select a new place.

```json
{
  "type": "sess:ActivePlaceCleared",
  "headers": { "source": "SERV:sess:" },
  "payload": {
    "messageType": "sess:ActivePlaceCleared",
    "attributes": {
      "placeId": "550e8400-e29b-41d4-a716-446655440000",
      "reason": "Place removed"
    }
  }
}
```

| Attribute | Type | Description |
|---|---|---|
| `placeId` | string | UUID of the cleared place |
| `reason` | string | `"Place removed"` (place was deleted) or `"Access to place removed"` (user's authorization revoked) |

#### `person:PasswordChanged`

The user's password was changed. All **other** sessions for this person are logged out — the session that initiated the change (identified by `session`) is preserved. This event is not forwarded to the client as a message; instead, matching sessions are silently disconnected.

## 8. Session Lifecycle

### Authentication Check

The server re-validates the session on **every incoming WebSocket message**, not just during the upgrade handshake. The check in `ShiroClient.isAuthenticated()` is a three-part test:

1. **Shiro subject is authenticated** — the principal is valid
2. **Session is not expired** — `expirationTime` has not passed
3. **Session started after last password change** — prevents use of sessions created before a password reset

If any check fails, the server immediately disconnects the client.

### Session Expiry

When a session expires — whether detected by the Shiro session manager, by the per-message authentication check, or via a platform `SessionExpiredEvent` — the server:

1. Sends a `sess:SessionExpired` text frame
2. Sends a WebSocket `CloseFrame` with **status code 4001**
3. Closes the channel

### Determining Login State

There is no dedicated REST endpoint for session validation. Instead:

- **On connect** — receiving the `SessionCreated` message after WebSocket upgrade confirms authentication
- **On failure** — the WebSocket upgrade is rejected with HTTP 401
- **On expiry** — the server sends `sess:SessionExpired` + close code 4001
- **Passively** — if a message is sent on an expired session, the connection is closed with code 4001 instead of returning a response

### Keepalive

The server sends WebSocket `PingFrame`s on idle (configurable via `web.socket.ping.rate`, default 30 seconds). If `web.socket.pong.timeout` is non-zero, the server tracks outstanding pings and closes the connection if a pong is not received in time. The `web.socket.read.idle.close` option, when enabled, closes the connection immediately on read idle instead of sending a ping.

## 9. REST Endpoints

In addition to the WebSocket API, the client-bridge serves HTTP REST endpoints on the same port. Most accept `POST` with a JSON `ClientMessage` body and return a JSON `ClientMessage` response. Unless noted, all REST endpoints use `AlwaysAllow` authorization (no session required).

### Authentication

| Method | Path | Auth | Description |
|---|---|---|---|
| POST | `/login` | No | Authenticate with username/password or handoff token. Also matches `POST /`. Accepts form-encoded (fields: `user`, `password`, `token`, `public`) or JSON (fields: `username`, `password`, `token`, `public`). Sets session cookie on success. See [Section 1](#1-authenticate-via-http) for full details. |
| POST | `/logout` | No | Destroy the current session and expire the cookie. |

### Health Check

| Method | Path | Auth | Description |
|---|---|---|---|
| GET | `/check` | No | Returns 204 if Cassandra is healthy, 503 otherwise. |

### Account

| Method | Path | Auth | Description |
|---|---|---|---|
| POST | `/account/CreateAccount` | No | Create a new account and person. Sets session cookie on success. |

### Person

| Method | Path | Auth | Description |
|---|---|---|---|
| POST | `/person/ChangePassword` | No | Change password (requires `emailAddress`, `currentPassword`, `newPassword`). Broadcasts `PasswordChangedEvent` to boot other sessions. |
| POST | `/person/SendPasswordReset` | No | Send a password reset link via email or SMS (field: `method`). |
| POST | `/person/ResetPassword` | No | Reset password using a token (fields: `email`, `password`, `token`). Auto-authenticates and sets session cookie. |
| POST | `/person/ChangePin` | Session | Change the user's PIN for the active place. |
| POST | `/person/ChangePinV2` | Session | Change the user's PIN for a specific place (fields: `pin`, `placeId`). |
| POST | `/person/VerifyPin` | Session | Verify a PIN for a place (fields: `pin`, `placeId`). |
| POST | `/person/SendVerificationEmail` | Session | Send an email verification link. |
| POST | `/person/VerifyEmail` | Session | Verify email with a token (fields: `token`, `email`). |

### Invitations

| Method | Path | Auth | Description |
|---|---|---|---|
| POST | `/invite/GetInvitation` | No | Look up an invitation (fields: `code`, `inviteeEmail`). |
| POST | `/invite/AcceptInvitationCreateLogin` | No | Accept an invitation and create a new login (fields: `code`, `inviteeEmail`, `password`, `person`). |

### Product Catalog

| Method | Path | Auth | Description |
|---|---|---|---|
| POST | `/prodcat/GetProductCatalog` | No | Get the full product catalog for a place. |
| POST | `/prodcat/GetProducts` | No | List products (fields: `place`, `include`, `hubrequired`). |
| POST | `/prodcat/FindProducts` | No | Search products (fields: `query`, `place`). |
| POST | `/prodcat/GetProduct` | No | Get a single product (fields: `productId`, `place`). |
| POST | `/prodcat/GetBrands` | No | List product brands for a place. |
| POST | `/prodcat/GetCategories` | No | List product categories for a place. |
| POST | `/prodcat/GetProductsByBrand` | No | List products by brand (fields: `brandId`, `place`). |
| POST | `/prodcat/GetProductsByCategory` | No | List products by category (fields: `categoryId`, `place`). |

### Location & Weather Codes

| Method | Path | Auth | Description |
|---|---|---|---|
| POST | `/place/ListTimezones` | No | List available timezones. |
| POST | `/nwssame/GetSameCode` | No | Get a SAME code (fields: `stateCode`, `countyFips`). |
| POST | `/nwssame/ListSameStates` | No | List SAME state codes. |
| POST | `/nwssame/ListSameCounties` | No | List SAME counties for a state (field: `stateCode`). |
| POST | `/emerg/ListEasCodes` | No | List EAS event codes. |

### Internationalization

| Method | Path | Auth | Description |
|---|---|---|---|
| POST | `/i18n/LoadLocalizedStrings` | No | Load localized strings (fields: `locale`, `keys`). |

### Session & Device

| Method | Path | Auth | Description |
|---|---|---|---|
| POST | `/sess/Log` | No | Submit a client log entry. |
| POST | `/sess/LockDevice` | Session | Lock a device and expire the session (fields: `deviceIdentifier`, `reason`). |

### Billing

| Method | Path | Auth | Description |
|---|---|---|---|
| GET | `/invoice/GetInvoice?in=<number>&ac=<accountId>` | Session | Render an invoice as HTML. Account owner only. 30-second timeout. |

### App Launch / Deep Links

| Method | Path | Auth | Description |
|---|---|---|---|
| GET | `/apple-app-site-association` | No | Apple universal links association file. |
| GET | `/app/launch[?*][/*]` | No | Mobile app launch redirect (to app store or auth server). |
| GET | `/web/launch[?*][/*]` | No | Web app launch redirect. Generates handoff token for authenticated users. |
| GET | `/web/run[?*][/*]` | No | Web app execution/redirect endpoint. |
| GET | `/(android\|ios\|other)/run[/**]` | No | App fallback — redirects to app store or help page if app is not installed. |

### Static Resources

| Method | Path | Auth | Description |
|---|---|---|---|
| GET | `/` | No | Redirects to `/index.html`. |
| GET | `/index.html` | No | Index page. |
| GET | `/login` | No | Login page (GET renders the HTML form). |
| GET | `/*` | No | Catch-all static file handler (serves from `./webapp` directory). Bound last due to greedy matching. |

## Configuration Reference

| Property | Default | Description |
|---|---|---|
| `port` | `8081` | Server listen port |
| `web.socket.path` | `androidbus` | WebSocket endpoint path |
| `auth.cookie.name` | `irisAuthToken` | Session cookie name |
| `auth.cookie.secure` | `true` | Only send cookie over HTTPS |
| `domain.name` | _(none)_ | Cookie domain (`"none"` to disable) |
| `web.socket.ping.rate` | `30` | Seconds between server pings |
| `web.socket.pong.timeout` | `0` | Seconds before pong timeout (0 = disabled) |
| `web.socket.read.idle.close` | `false` | Close connection on read idle |
| `web.socket.maxFrameSizeBytes` | `65535` | Maximum WebSocket frame size |
| `auth.timeout` | `1209600` | Session timeout in seconds (~2 weeks) |
| `message.prefix` | `CLNT:android:` | Prefix for client addresses |
| `client.background.threads` | `100` | Background thread pool size for async handlers |
| `client.background.threadKeepAliveMs` | `10000` | Thread keep-alive timeout in milliseconds |

## CORS and Browser Integration

The client-bridge includes a Netty CORS handler for browser-based clients. This is important because the client-bridge does not need to be hosted at the same origin as the web application — it is typically deployed on a separate subdomain or port.

### Cross-Origin Setup

A typical deployment looks like:

| Service | URL |
|---|---|
| Web application | `https://app.example.com` |
| Client-bridge | `https://client.example.com:8081` |

The web app at `app.example.com` makes cross-origin requests to `client.example.com` for both the REST login endpoint and the WebSocket connection. This works because:

1. **CORS** — the client-bridge's `cors.origins` is configured to include the web app's origin, and `Access-Control-Allow-Credentials: true` is set so the browser will send cookies cross-origin.
2. **Cookie domain scoping** — the `domain.name` property is set to a shared parent domain (e.g. `.example.com`) so the `irisAuthToken` cookie set by the client-bridge is also sent on requests to any `*.example.com` subdomain. Without this, the browser would scope the cookie to `client.example.com` only and it would not be reusable across services on sibling subdomains.
3. **WebSocket upgrade** — browsers include cookies matching the target origin when performing the WebSocket handshake, so the `irisAuthToken` cookie flows automatically to `wss://client.example.com:8081/androidbus` as long as the domain scope covers it.

### Browser Client Flow

1. Web app at `https://app.example.com` sends `POST https://client.example.com:8081/login` with `credentials: "include"` (fetch) or `withCredentials: true` (XHR)
2. Browser receives `Set-Cookie: irisAuthToken=...; Domain=.example.com; Path=/; HttpOnly; Secure`
3. Web app opens `new WebSocket("wss://client.example.com:8081/websocket")` — the browser automatically attaches the cookie
4. The WebSocket upgrade succeeds and the server pushes `SessionCreated`

### CORS Configuration

| Property | Default | Description |
|---|---|---|
| `cors.origins` | _(configured per environment)_ | Comma-separated list of allowed origins |
| `cors.allow.any` | `false` | Allow any origin (development only) |
| `cors.allow.request.methods` | `GET, POST, OPTIONS` | Allowed HTTP methods |
| `cors.allow.request.headers` | _(extensive list)_ | Includes `content-type`, `x-client-version`, etc. |

The CORS preflight response sets `Access-Control-Max-Age: 1209600` (2 weeks) and `Access-Control-Allow-Credentials: true`.

Note: because the cookie is `HttpOnly`, JavaScript cannot read it directly. The browser manages cookie attachment transparently. This also means the cookie is not vulnerable to XSS exfiltration.

## Known Issues

- **JSON login `public` field is broken** — `ShiroAuthenticator.extractToken()` line 237 compares `"true".equalsIgnoreCase("public")` which is always `false`. The `public` flag only works with form-encoded login. See [Section 1](#1-authenticate-via-http).
- **`video:ListRecordingsRequest` returns empty** — `IrisNettyMessageHandler` intercepts this message type and always returns an empty list without forwarding to the platform.

## Key Source Files

| File | Purpose |
|---|---|
| `platform/arcus-containers/client-bridge/src/main/java/com/iris/client/server/ClientServerModule.java` | Guice module — binds all handlers |
| `platform/arcus-containers/client-bridge/src/main/java/com/iris/client/server/ClientServer.java` | Main server entry point |
| `platform/arcus-containers/client-bridge/src/main/java/com/iris/client/server/session/HandshakeSessionListener.java` | Sends `SessionCreated` on connect |
| `platform/bridge-common/src/main/java/com/iris/netty/server/message/SetActivePlaceHandler.java` | Handles `SetActivePlace` |
| `platform/bridge-common/src/main/java/com/iris/netty/server/message/IrisNettyMessageHandler.java` | Main message router to platform bus |
| `platform/bridge-common/src/main/java/com/iris/netty/server/message/ClientRequestDispatcher.java` | Routes session service messages |
| `platform/bridge-common/src/main/java/com/iris/netty/bus/IrisNettyPlatformBusListener.java` | Dispatches server-pushed events to clients |
| `platform/bridge-common/src/main/java/com/iris/bridge/server/netty/BaseWebSocketServerHandler.java` | WebSocket upgrade, frame handling, and ping/pong |
| `platform/bridge-common/src/main/java/com/iris/bridge/server/http/impl/auth/SessionAuth.java` | WebSocket upgrade and REST authorization |
| `platform/bridge-common/src/main/java/com/iris/bridge/server/shiro/ShiroClient.java` | Session authentication logic (expiry, password change check) |
| `platform/bridge-common/src/main/java/com/iris/bridge/server/session/DefaultSessionImpl.java` | Session disconnect and close frame handling |
| `common/arcus-client/src/main/java/com/iris/messages/ClientMessage.java` | Message structure |
| `common/arcus-client/src/main/java/com/iris/gson/ClientMessageTypeAdapter.java` | JSON serialization |
| `common/arcus-client/src/main/java/com/iris/messages/ErrorEvent.java` | Error response format |
