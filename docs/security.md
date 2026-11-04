# Security Documentation

## Access Control

### Roles

Roles are derived dynamically from the state of an `AuthorizationGrant` (defined in `platform/arcus-security/.../authz/AuthorizationGrant.java`). There are four roles:

| Role | Condition | Description |
|------|-----------|-------------|
| `OWNER` | `accountOwner = true` | Account owner — full access plus billing and account management |
| `FULL_ACCESS` | Non-owner with non-empty permissions set | Can control devices and access all place features |
| `HOBBIT` | Non-owner with empty permissions set | Limited access — no device control permissions |
| `OTHER` | Defined but never returned by `getRole()` | Vestigial; unused in practice |

These values are declared in the capability type XMLs (`placeaccessdescriptor.xml`, `personaccessdescriptor.xml`).

### Permissions

Permissions use Shiro wildcard syntax with three colon-separated parts:

```
capability:operation:instanceId
```

Operations are defined in `PermissionCode`:

| Code | Meaning |
|------|---------|
| `r` | Read |
| `w` | Write |
| `x` | Execute |
| `c` | Create |
| `d` | Delete |

Examples:

- `*:*:*` — full access to everything (default for new accounts and invitees)
- `dev:*:*` — full access to the `dev` capability namespace
- `swit:x:*` — execute commands on any switch device
- `dev:r:<deviceId>` — read-only on a specific device

Instance-specific permissions (where the third segment is a concrete ID) are evaluated before wildcard permissions.

### Authorization Grants

An `AuthorizationGrant` is a per-person-per-place access record stored in Cassandra. Each grant contains:

- **entityId** — the person being granted access
- **placeId** — the place they can access
- **accountId** — the account that owns the place
- **accountOwner** — whether this person is the account owner
- **permissions** — set of Shiro wildcard permission strings

Grants are stored in two Cassandra tables kept in sync via batch writes:

- `authorization_grant` — keyed by `entityId` (lookup by person)
- `authorization_grant_by_place` — keyed by `placeId` (lookup by place)

When an account is created, the owner gets `accountOwner=true` with `permissions=["*:*:*"]`. When an invitation is accepted, the invitee gets `accountOwner=false` with `permissions=["*:*:*"]` (FULL_ACCESS).

### Client Message Authorization

Each bridge binds one `Authorizer`, chosen by `authz.algorithm`. A client chooses the destination of every message, so authorization has to decide whether that destination is within the session's scope as well as whether the message is allowed.

| Bridge | `authz.algorithm` | Runs |
|---|---|---|
| client-bridge | `role` | RoleAuthorizer |
| api-bridge | `layered` | RoleAuthorizer, then PermissionsAuthorizer |

**RoleAuthorizer** (`platform/arcus-security/.../authz/RoleAuthorizer.java`):

Scope checks, using only the session's grants:

- Certain messages (e.g., `IssueInvoiceRefundRequest`, population changes) are always denied to clients (reserved for internal support)
- Account-mutating messages (billing, place deletion) require `accountOwner=true`, on the addressed account when the destination is an account
- "Self" methods (add mobile device, delete login, invitations) require the message actor to match the destination
- Place, account and mobile device destinations require a grant on that place or account, or for a mobile device to be the actor's own
- Place-scoped service requests verify the user has at least one permission for the target place

**PermissionsAuthorizer** (`platform/arcus-security/.../authz/PermissionsAuthorizer.java`):

Fine-grained Shiro permission checking:

1. Extracts required permissions from the message via `PermissionExtractorRegistry`
2. Checks instance-specific permissions first (per-device overrides), then wildcard permissions

It doesn't look at where the destination belongs, so it isn't enough by itself: a key with `*:*:*` on one place would pass for any object.

**Services** check whatever needs a lookup. A service that loads an object from a client-chosen address must compare the object's place to the message's place header with `Errors.assertPlaceMatches`, or refuse it some other way. RoleAuthorizer can't do this, so a new handler on one of these paths has to:

| Destination | Checked by |
|---|---|
| Devices | `MessageHandler`; remove, force remove, lost, upgrade and history have their own handlers, which each compare the device's place |
| Subsystems | `SubsystemExecutorResolver` |
| Alarm incidents | Looked up by place, `ListIncidentHistoryEntriesHandler` |
| Hubs | hub-bridge `acceptFromPlatform`, `OfflineHubRequestHandler`, `HubDeleteHandler` |
| Rules | `RuleService` — a rule address names its place, which must match the message |
| Scenes, schedulers, pairing devices | Their handlers |
| IPCD devices | `IpcdService` |
| People | `PersonAccess` (see below) |

**People:** someone with a login manages their own record, because their email is their username. Anyone sharing a place may maintain someone without a login. The web, iOS and Android clients all follow the same rule. Reading a person needs a shared place. A stranger gets `not found`, so these can't be used to test person ids.

### Disabling Operations

Instances can switch off what they don't use. Both settings are read at startup and turn nothing off by default. Each disabled entry logs a warning at startup, so confirm the lines after a change. A misspelled variable name is ignored like any unknown variable.

**`client.disabled.handlers`** (env `CLIENT_DISABLED_HANDLERS`, client-bridge) removes REST handlers by class name, and the path then returns 404. A mistyped handler is ignored, so check the log.

| To disable | Handlers |
|---|---|
| Account creation | `CreateAccountRESTHandler` |
| Password reset | `SendPasswordResetRESTHandler,ResetPasswordRESTHandler` |

**`authz.disabled.risky.operations`** (env `AUTHZ_DISABLED_RISKY_OPERATIONS`, client-bridge and api-bridge) refuses risky operations from clients, comma separated, or `all`. A mistyped operation stops the bridge from starting, and so does combining the switch with `authz.algorithm=none`, which couldn't enforce it. Only client requests are affected, so deleting a place still removes its hubs. Both bridges run RoleAuthorizer, so refusals on either count toward `bridge.authz.unauthorized.disabled`, and both log them.

| Name | Refuses |
|---|---|
| `email.change` | `base:SetAttributes` with `person:email`, for anyone including the person themselves |
| `phone.change` | `base:SetAttributes` with `person:mobileNumber`, for anyone including the person themselves |
| `account.delete` | `account:Delete` |
| `place.delete` | `place:Delete` |
| `hub.delete` | `hub:Delete` |
| `hub.factoryreset` | `FactoryReset` on the hub, ZigBee and Z-Wave, plus `hubzigbee:Reset`, `hubzwave:Reset` and `hubbackup:Restore`, which tear down or rewind the networks |
| `person.delete` | `person:Delete`, `person:RemoveFromPlace` |
| `login.delete` | `person:DeleteLogin` |

Alarm and device control aren't on this list, deliberately.

The mobile apps send name, phone and email together on every contact save, so `email.change` or `phone.change` refuses the whole save even if that field didn't change. The web client only sends what changed. `phone.change` also stops anyone adding or correcting a number, and the alarm call tree only lists people who have one.

Whatever this setting says, the scheduler never sends these operations or anything addressed to a hub. `FireCommand` returns an error, and a timer logs a warning and moves on to the next fire time.

### Runtime Authorization Context

At WebSocket session establishment, `SubscriberAuthorizationContextLoader` builds an `AuthorizationContext` by loading all `AuthorizationGrant`s for the authenticated user from Cassandra. This context caches parsed permissions per place:

- Instance permissions (device-specific grants)
- Non-instance permissions (namespace-wide grants)

## Authentication

### Password Login

Authentication uses Apache Shiro with `GuicedIrisRealm` as the primary realm:

1. Client submits email + password
2. `AuthenticationDAO.findLogin(email)` retrieves the stored hash and salt from the `login` Cassandra table
3. Shiro's `HashedCredentialsMatcher` re-hashes the submitted password with the stored salt and compares
4. On success, a `DefaultPrincipal(username, userId)` is created
5. A Shiro session is persisted to Cassandra via `GuicedCassandraSessionDAO`

### App Handoff

A secondary `AppHandoffRealm` handles app-to-app handoff (e.g., mobile app to web). It accepts an `AppHandoffToken` validated against the `app_handoff_token` Cassandra table rather than by credential matching.

### Passkeys

client-bridge supports passkey (WebAuthn) login alongside passwords, using Yubico's `webauthn-server-core`. Passkeys are discoverable and require user verification, so a login needs no username and the passkey covers both factors. Password login and password reset are unchanged and remain the recovery path.

Passkeys are off unless `passkey.rp.id` is set:

| Property | Default | Purpose |
|----------|---------|---------|
| `passkey.rp.id` | (unset: disabled) | Domain passkeys are bound to, e.g. `example.com`. The web app and client-bridge must be served from it or a subdomain. |
| `passkey.rp.name` | `Arcus` | Name shown by the browser and authenticator |
| `passkey.origins` | `https://{rp.id}` | Comma-separated origins allowed to use passkeys |
| `passkey.request.ttl.secs` | `300` | How long the browser has to answer a request (1 to 3600). The prompt timeout sent to the browser is shorter by a tenth of this, but by no more than 30 seconds (270 seconds for the default). |
| `passkey.ratelimit.capacity` | `20` | Login requests a client address can make in a burst; `0` turns limiting off |
| `passkey.ratelimit.rate` | `0.2` | Login requests per second a client address earns back |

Each ceremony is two POSTs with JSON bodies. The `*Options` responses are `{"requestId": ..., "publicKey": {...}}`: pass `publicKey` to `PublicKeyCredential.parseCreationOptionsFromJSON()` or `parseRequestOptionsFromJSON()`, then send the resulting credential's `toJSON()` back as `credential` with the `requestId`. Requests are single-use and expire.

| Endpoint | Session | Body | Result |
|----------|---------|------|--------|
| `/passkey/LoginOptions` | no | `{}` | login options |
| `/passkey/Login` | no | `{requestId, credential, public}` | session cookie, as for `/login` |
| `/passkey/RegisterOptions` | yes | `{}` | registration options for the logged-in person |
| `/passkey/Register` | yes | `{requestId, credential, name}` | `{id, name, created}` |
| `/passkey/List` | yes | `{}` | `{passkeys: [{id, name, created, lastUsed}]}` |
| `/passkey/Delete` | yes | `{id}` | `{}`, or 404 for an unknown id |

`PasskeyRealm` verifies the WebAuthn response itself, so a `PasskeyToken` only authenticates with a valid signature. Credentials are stored in the `passkey` and `passkey_by_credential` tables; in-progress requests in `passkey_request` with a TTL. Every write to `passkey` is a lightweight transaction, so Cassandra orders them itself rather than by each writer's clock. A login whose passkey is deleted while it's being checked fails, rather than bringing back part of the deleted row.

Deleting a person deletes their passkeys too (in `PersonDAO.delete`, whichever service does it), after the person. That never fails the delete, since callers have already removed the person's grants. A passkey left behind can't log in, because login needs its person; an error naming the person is logged so the rows can be removed by hand. The passkey statements are prepared on first use, so services start without these tables. If they're missing, the passkey endpoints fail, and deleting a person skips them.

A person can have up to 20 passkeys; past that, `/passkey/RegisterOptions` and `/passkey/Register` answer `409`. Registrations that finish at the same moment can each pass the check, so the limit is approximate. Names are shown to people in the household, so a few things are tightened up. Control characters (line breaks among them) and every kind of space become one space, and the ends are trimmed. Formatting characters, which are drawn as nothing, are dropped: direction overrides, zero-width spaces, soft hyphens, tag text. The zero-width joiner and non-joiner stay, since emoji and scripts such as Persian need them; England's, Scotland's and Wales's flags show as a plain black flag. Names are cut to 64 code points without splitting an emoji (Java 20 and later know where a joined emoji ends), and a blank name becomes `Passkey`. Otherwise a name is kept as typed.

Adding or removing a passkey emails the person (`account.passkey.added` and `account.passkey.removed`, LOW priority, so email only), naming the passkey. A passkey keeps working after a password change or reset, so this notice is how someone finds out that another person added one to their account.

Anyone can call `/passkey/LoginOptions` and `/passkey/Login`, and each call writes to Cassandra, so they share a token bucket per client address and answer `429` with `Retry-After` when it's empty. IPv6 addresses count per /64. The address is the socket's unless `allow.forwardedfor` is set, in which case it's the last `X-Forwarded-For` hop. Behind a proxy such as Traefik, set `allow.forwardedfor=true`, or every client shares the proxy's bucket. Only do that if clients can't reach client-bridge without going through the proxy, since a client that connects directly can send any `X-Forwarded-For` it likes. `passkey_request` keeps tombstones for an hour rather than 10 days and uses time-window compaction, so a flood of requests is gone from disk soon after it stops.

## Password Storage

Passwords are hashed using **SHA-256 with a random salt and 1024 iterations**, Base64-encoded.

Implementation: `platform/arcus-security/.../credentials/Sha256CredentialsHashingStrategy.java`

```java
// Hashing
new Sha256Hash(credentials, salt, 1024).toBase64()

// Salt generation
RandomNumberGenerator rng = new SecureRandomNumberGenerator();
return rng.nextBytes();
```

The `login` Cassandra table stores:

| Column | Description |
|--------|-------------|
| `domain` | Email domain (e.g., `gmail.com`) |
| `user_0_3` | First 4 chars of local part (bucketing) |
| `user` | Full local part of email |
| `password` | SHA-256 hash, Base64-encoded |
| `password_salt` | Random salt, Base64-encoded |
| `personid` | UUID foreign key to person table |
| `reset_token` | Temporary reset token (TTL, default 15 min) |
| `lastPassChange` | Timestamp of last password change |

### PIN Storage

PINs are stored per-place in `person.pinPerPlace` (a map of `placeId` to encrypted PIN), encrypted using AES. Security question answers are also AES-encrypted.

## Key Files

| File | Purpose |
|------|---------|
| `platform/arcus-security/.../authz/AuthorizationGrant.java` | Role derivation, permission storage |
| `platform/arcus-security/.../credentials/Sha256CredentialsHashingStrategy.java` | Password hashing (SHA-256, 1024 iterations) |
| `platform/arcus-security/.../GuicedIrisRealm.java` | Primary Shiro realm (username + password) |
| `platform/arcus-security/.../handoff/AppHandoffRealm.java` | Secondary Shiro realm (app handoff) |
| `platform/arcus-containers/client-bridge/.../passkey/PasskeyService.java` | Passkey registration and login (WebAuthn) |
| `platform/arcus-containers/client-bridge/.../passkey/PasskeyRealm.java` | Shiro realm for passkey login |
| `platform/arcus-security/.../SecurityModule.java` | Guice wiring of Shiro components |
| `platform/arcus-security/.../authz/AuthorizationContext.java` | Per-session permission cache |
| `platform/arcus-security/.../authz/RoleAuthorizer.java` | Coarse-grained role/ownership checks |
| `platform/arcus-security/.../authz/PermissionsAuthorizer.java` | Fine-grained Shiro wildcard permission checks |
| `platform/arcus-security/.../authz/permission/PermissionCode.java` | Permission codes: r, w, x, c, d |
| `platform/arcus-lib/.../PersonDAOImpl.java` | Password hash/salt storage in Cassandra |
| `platform/arcus-lib/.../AuthorizationGrantDAOImpl.java` | Grant persistence (two Cassandra tables) |

## Running the OWASP Dependency Scanner

```
./gradlew -Peyeris_owasp=1 :platform:arcus-containers:client-bridge:dependencyCheckAnalyze
```
