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

### Two-Layer Authorization

Every platform message passes through two authorization layers:

**Layer 1 — RoleAuthorizer** (`platform/arcus-security/.../authz/RoleAuthorizer.java`):

Coarse-grained checks based on message type:

- Certain messages (e.g., `IssueInvoiceRefundRequest`) are always denied to end users (reserved for internal support)
- Account-mutating messages (billing, place deletion) require `accountOwner=true`
- "Self" methods (add mobile device, delete login) require the message actor to match the destination
- Place-scoped service requests verify the user has at least one permission for the target place

**Layer 2 — PermissionsAuthorizer** (`platform/arcus-security/.../authz/PermissionsAuthorizer.java`):

Fine-grained Shiro permission checking:

1. Extracts required permissions from the message via `PermissionExtractorRegistry`
2. Checks instance-specific permissions first (per-device overrides), then wildcard permissions

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
