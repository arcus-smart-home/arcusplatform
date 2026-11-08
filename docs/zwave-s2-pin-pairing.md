# Z-Wave S2 PIN Entry

A Z-Wave device that asks for S2 Authenticated or S2 Access Control can only
get those keys if the user types in the 5 digit PIN printed on it. This page
is the contract between the hub agent, the platform and clients for collecting
that PIN during pairing.

The platform side is in `PairingSubsystem` (`S2PinPairing`) and the hub bridge,
and Oculus can enter a PIN. The hub side belongs to the hub's Z-Wave controller;
[What the hub's Z-Wave controller must do](#what-the-hubs-z-wave-controller-must-do)
lists its part. The iris2 controller and the Z-Wave controller on master don't
take part: they ignore `s2PinEntry` and never ask for a PIN, so devices pair on
them as before. Mobile and web clients live in other repos;
[What a client must do](#what-a-client-must-do) is their part.

## Background

The DSK is the first 16 bytes of the joining node's public key, written as
eight groups of five decimal digits. The first group is the PIN. The node
leaves those two bytes blank when it sends its key over the air, so the hub
sees a DSK starting `00000-` and needs the PIN to fill them in.

The PIN on its own is only 16 bits. What protects the key exchange from a man
in the middle is the user checking the other seven groups against the label on
the device. That is why the client is shown the DSK, not just a PIN box.

There is one attempt per inclusion. If the PIN is wrong the key exchange fails
and the device stays on the network without S2. It has to be excluded or
factory reset and paired again.

## Overview

```
Client                  Pairing subsystem                Hub                     Node
  |  Search {s2PinEntry}    |                              |                       |
  |------------------------>|  hub:PairingRequest          |                       |
  |                         |  {START_PAIRING, s2PinEntry} |                       |
  |                         |----------------------------->|   inclusion, KEX      |
  |                         |                              |<--------------------->|
  |                         |                              |<-- Public Key Report  |
  |                         |                              |--- Public Key Report->|  (within TB3, 10 s)
  |                         |  hubzwave:S2PinRequired      |                       |
  |                         |<-----------------------------|  (TAI2 starts)        |
  |  ValueChange            |                              |                       |
  |  s2PinRequestId         |                              |                       |
  |<------------------------|                              |                       |
  |  GetS2PinRequest        |                              |                       |
  |------------------------>|                              |                       |
  |  {requestId, dsk, ...}  |                              |                       |
  |<------------------------|                              |                       |
  |  SubmitS2Pin            |                              |                       |
  |------------------------>|  hubzwave:SubmitS2Pin        |                       |
  |                         |----------------------------->|<-- KEX Set echo, keys>|
  |  SubmitS2PinResponse    |  hubzwave:SubmitS2PinResponse|                       |
  |<------------------------|<-----------------------------|                       |
  |                         |  hubzwave:S2PinFinished      |                       |
  |                         |<-----------------------------|                       |
  |  PairingFailed (unless  |                              |                       |
  |  GRANTED)               |                              |                       |
  |<------------------------|                              |                       |
```

## Timing

The S2 timers are taken from zwave-js
(`packages/cc/src/lib/Security2/shared.ts`, `inclusionTimeouts`).

| Timer | Value | Who | What it covers |
|---|---|---|---|
| TB3 | 10 s | joining node | After sending its public key, the node waits this long for the controller's. The hub must send its Public Key Report **before** asking for the PIN. |
| TAI2 | 240 s | hub | From receiving the node's public key to receiving the node's KEX Set echo. PIN entry has to fit inside it. |
| `timeoutMs` | 200 s | hub | How long the hub waits for an answer, sent in `S2PinRequired`. The platform accepts 30 to 240 s and cancels anything else. |
| Grace | 5 s | platform | The platform stops taking answers at `timeoutMs` minus 5 s, counted from when the hub bridge received `S2PinRequired`, so an accepted PIN still reaches the hub however long the request sat in a queue. This is `subpairing:s2PinExpires`. |
| Echo wait | rest of TAI2, at least 40 s | hub | After the PIN arrives. The node repeats its KEX Set every 10 s while it waits. |
| Request timeout | 30 s | platform | Wait for the hub's `SubmitS2PinResponse` (`pairing.request.timeoutMs`). |
| Answered wait | 150 s | platform | After passing a PIN on, how long the platform waits for `S2PinFinished` before reporting a failure. This covers the echo wait plus every key's get, report and verify. |
| Search hold | `s2PinExpires` + 150 s | platform | The pairing search is not allowed to time out before this, whatever the product's pairing timeout. |

With these numbers the user has about 195 s from the prompt appearing, and the
hub still has at least 40 s of TAI2 left when the latest accepted PIN arrives.

### If nobody answers, or pairing is cancelled

| What happens | Platform | Hub | Client sees |
|---|---|---|---|
| Nobody answers by `s2PinExpires` | Clears the request, sends `CancelS2Pin`, broadcasts `PairingFailed` `zwave.s2.pin.timeout` | Ends the key exchange about 5 s before its own timeout, sends `S2PinFinished` CANCELLED (ignored) | `s2PinRequestId` cleared, `PairingFailed` |
| The hub times out first | `S2PinFinished` TIMEOUT: clears, broadcasts `zwave.s2.pin.timeout` | | the same |
| User declines (`CancelS2Pin`) | Clears, sends `CancelS2Pin` to the hub | `S2PinFinished` CANCELLED (ignored) | `CancelS2PinResponse` |
| Pairing ends: `StopSearching`, `DismissAll`, search timeout, `FactoryReset` | Clears, sends `CancelS2Pin` unless a PIN was already given | | no event |
| The node gives up while waiting (KEX Fail) | `S2PinFinished` CANCELLED or FAILED: broadcasts `zwave.s2.pin.failed` | Must notice the KEX Fail during the wait | `PairingFailed` |
| Wrong PIN | `S2PinFinished` FAILED: broadcasts `zwave.s2.pin.failed` | | `PairingFailed` |
| PIN given, then the hub goes quiet | After 150 s broadcasts `zwave.s2.pin.failed` | | `PairingFailed` |
| The hub refuses the PIN (stale) or is offline | Clears, passes the error on, broadcasts `zwave.s2.pin.failed` unless `S2PinFinished` already ended the request | | error response to `SubmitS2Pin`, `PairingFailed` |
| The hub does not answer `SubmitS2Pin` in 30 s | Replies `hub.offline`, waits for `S2PinFinished` or the 150 s | | error response |
| Subsystem service restarts while waiting | The DSK is gone, so it clears, sends `CancelS2Pin` and broadcasts `zwave.s2.pin.failed` | | `PairingFailed` |
| The hub restarts | Nothing more arrives; the timers above end it | The wait is lost | `zwave.s2.pin.timeout` |

The hub leaving pairing mode on its own timer does **not** end the wait. Only
`CancelS2Pin`, its own `timeoutMs`, or a KEX Fail from the node does.

## Messages

### Client and platform (`subpairing`)

**Opting in.** `subpairing:StartPairing` and `subpairing:Search` take an
optional boolean `s2PinEntry`. A client that can show a DSK and collect a PIN
sends `true` on both. The platform copies it into `hub:PairingRequest`. Without
it the hub does not grant S2 Authenticated or Access Control, and a device
asking for them joins with S2 Unauthenticated or S0 instead of waiting for a
PIN nobody can type.

**Attributes.** While a device waits:

| Attribute | Type | |
|---|---|---|
| `subpairing:s2PinRequestId` | string | Set while a device waits for its PIN, empty once answered, declined or expired |
| `subpairing:s2PinExpires` | timestamp | When answers stop being accepted, 0 once the request has ended |

Neither is a secret. They arrive in the usual `base:ValueChange`, and are in
the subsystem model when a client reconnects. Both are absent until the first
request. A request that ends sets them empty rather than removing them: an
attribute set to null is dropped when the message is read off the platform bus,
so a client would never see it go.

**`subpairing:GetS2PinRequest`** returns `requestId`, `dsk` and `expires`. This
is the only way a client gets the DSK: it is never in an attribute or an
event, only in the response to the session that asked. Errors:
`request.state.invalid` if nothing is waiting.

**`subpairing:SubmitS2Pin`** `{requestId, pin}`. `pin` is a string of exactly
five ASCII digits from `00000` to `65535`. The success response is an empty
message, sent once the hub has taken the PIN. Errors:

| Code | Meaning | Retry? |
|---|---|---|
| `request.param.invalid` | The PIN is malformed. Nothing was sent. | Yes, the request is still open |
| `request.state.invalid` | Not waiting: expired, answered, cancelled, or a different `requestId` | No |
| `hub.offline` or another hub error | The hub did not take the PIN | No, the attempt is spent |

Any well formed PIN uses up the request, whatever happens next. A second
answer is refused.

**`subpairing:CancelS2Pin`** `{requestId}` declines. Errors:
`request.state.invalid` as above.

**Outcome.** On success nothing extra happens and the device shows up as a
pairing device in the usual way. Otherwise the subsystem broadcasts
`subpairing:PairingFailed` with:

| Code | Meaning |
|---|---|
| `zwave.s2.pin.timeout` | Nobody entered the PIN in time |
| `zwave.s2.pin.failed` | A PIN was entered and secure pairing still failed, usually a wrong PIN |

In both cases the device is on the network without S2 and has to be removed and
paired again.

**Who may answer.** The same people who may pair:

- The request's place header must be this subsystem's place. Subsystem requests
  are routed by destination, so this is checked in the subsystem.
- The actor must be a person at the place (in the place's model store), or an
  API key. The api-bridge only lets a key through if it holds
  `subpairing:x:<placeId>` (or a wildcard covering it) on its one place, the
  same permission `Search` needs.
- Anything else, including a hub, another service or a missing actor, is
  refused as `Error` `NotFound`, which is how unauthorized requests are
  reported elsewhere.

These rules apply to `GetS2PinRequest`, `SubmitS2Pin` and `CancelS2Pin`.

### Platform and hub (`hubzwave`)

All four messages are point-to-point. None of them is ever a broadcast.

**`hubzwave:S2PinRequired`**, event, hub to `SERV:subpairing:<placeId>`:

| Field | Type | |
|---|---|---|
| `requestId` | string | New for every wait, `[A-Za-z0-9-]{1,64}`. A random UUID is fine. |
| `node` | int | 1 to 232 |
| `dsk` | string | Exactly what `S2Crypto.dsk()` gives for the key as received: eight dash separated groups of five digits, the first `00000` |
| `timeoutMs` | long | 200000. Also set the message's time to live to this. |

The platform ignores it unless it comes from this place's hub (source hub id
and place header). It answers with `CancelS2Pin` if the DSK is malformed, the
first group isn't `00000`, `timeoutMs` is outside 30 s to 240 s, or the place
isn't in hub pairing mode. A repeat with the same `requestId` is ignored. A
late copy of the request that ended last gets `CancelS2Pin` again, in case
the first was lost, and no prompt. One that reaches the subsystem after its own
window has passed (by the hub bridge's timestamp) is ignored. One with a new
`requestId` replaces whatever was waiting, since inclusion handles one node at
a time.

**`hubzwave:SubmitS2Pin`**, request, from `SERV:subpairing:<placeId>` to the hub
address (`HubController` passes `hubzwave:*` on to the Z-Wave controller):
`{requestId, node, pin}`, time to live 30 s.

- If `requestId` and `node` match the wait in progress and `pin` is valid,
  answer `hubzwave:SubmitS2PinResponse` and carry on with the key exchange.
- Otherwise answer `Error` `request.state.invalid`, or `request.param.invalid`
  for a bad PIN. The hub must check the format itself (`S2Pin.isValidPin`):
  5 ASCII digits, value at most 65535.
- Accept at most one PIN per `requestId`.

**`hubzwave:CancelS2Pin`**, request, from the same address: `{requestId, node}`.
End the wait as if no PIN was given (KEX Fail, Cancel). Always answer
`hubzwave:CancelS2PinResponse`, even for an unknown or finished request.

**`hubzwave:S2PinFinished`**, event, hub to `SERV:subpairing:<placeId>`:
`{requestId, node, result}`. Send exactly one per `S2PinRequired`, as soon as
the outcome is known: when the node holds its keys (before asking which
command classes are secure), or when the wait or the exchange fails.

| `result` | When |
|---|---|
| `GRANTED` | The node holds its S2 keys |
| `FAILED` | A PIN was given and the exchange still failed: the echo never decrypted, KEX Fail, or a later step failed |
| `TIMEOUT` | No answer within `timeoutMs` |
| `CANCELLED` | `CancelS2Pin` arrived, or the node sent KEX Fail before any PIN arrived |

**`hub:PairingRequest`** gains an optional boolean `s2PinEntry`. Take it from
every `START_PAIRING`. `HubController` passes one that arrives while the hub
is already pairing on to the Z-Wave controller alone, leaving the window's
timer and ZigBee as they were. Clear it on `STOP_PAIRING` and when pairing
mode ends.

### Hub bridge rules

`DefaultHubMessageFilterImpl` enforces two things before any other check,
including the admin address bypass:

- `hubzwave:SubmitS2Pin` and `hubzwave:CancelS2Pin` reach a hub only from
  `SERV:subpairing:<the hub's place>` with that place in the header. Anything
  else is dropped, and a request gets an unauthorized error back. A client
  can't send a PIN straight to the hub and skip the checks above.
- `hubzwave:S2PinRequired` from a hub is dropped unless it is addressed to
  `SERV:subpairing:<the hub's place>`. A hub bug can't put a DSK on a broadcast.

## Device security attributes

The hub's add device request for a Z-Wave device carries two protocol
attributes, defined in `protocol/zwave.xml`. Clients see them in
`devadv:protocolAttrs`.

| Attribute | Value |
|---|---|
| `ZWAV:Security` | The highest class the hub granted: `S2_ACCESS_CONTROL`, `S2_AUTHENTICATED`, `S2_UNAUTHENTICATED`, `S0` or `NONE` |
| `ZWAV:SecurityRequested` | The highest class the device asked for, in the same terms |

When `ZWAV:SecurityRequested` is higher than `ZWAV:Security`, the device
joined with less than it asked for, usually because no client could take its
PIN. Pairing it again from a client that sends `s2PinEntry` gets it the rest.

- An S2 device says what it wants in its KEX Report, which the hub keeps. A
  device without S2 can only ask for S0, if it has the Security command class,
  or for nothing.
- `ZWAV:SecurityRequested` is left out when the hub doesn't know: an S2 device
  paired before the hub kept KEX Reports, or one that stopped before sending
  its KEX Report.
- Hubs running the iris2 Z-Wave controller send neither. A missing
  `ZWAV:Security` means unknown, not `NONE`.
- The platform takes protocol attributes only when a device is added, so a
  device paired before hubs sent these doesn't have them until it is paired
  again.

## Security

The PIN and the DSK are secrets.

| Where | PIN | DSK |
|---|---|---|
| Client to bridge to subsystem | `subpairing:SubmitS2Pin` | |
| Subsystem to hub | `hubzwave:SubmitS2Pin` | |
| Hub to subsystem | | `hubzwave:S2PinRequired` |
| Subsystem memory | | Until answered, declined or expired, never more than 240 s |
| Subsystem to client | | `subpairing:GetS2PinRequestResponse`, to the asking session only |

Neither one is in an attribute, a subsystem variable, Cassandra, a broadcast,
history, `PairingFailed`, an error message or a log. The subsystem saves only
`S2PinRequest`: request id, node, hub address, expiry and whether it was
answered.

Logging:

- `MessageBody.toString()` prints `<redacted>` for the attributes of
  `hubzwave:S2PinRequired`, `hubzwave:SubmitS2Pin`, `subpairing:SubmitS2Pin`
  and `subpairing:GetS2PinRequestResponse`. Every log line that prints a
  `MessageBody`, a `PlatformMessage` or the bridges' `com.iris.messages.ClientMessage`
  goes through it, on the platform and on the hub. The JVM client library
  (`com.iris.client`, used by Oculus) has its own message classes and is not
  covered. `MessageBodySecretsTest` builds each one through
  its generated class, so renaming one in the capability XML fails the build
  instead of quietly logging it.
- The client bridge and api-bridge used to log each raw frame at debug, and the
  hub bridge each raw hub frame at trace. They now log the parsed message.
- Code that handles these values must not log `getAttributes()` directly or
  quote a value in an exception message.

Kafka: every hop above goes over the `platform` topic, which stores messages
in cleartext until its retention runs out. Neither khakis nor arcus-k8s sets a
retention, so the broker default applies (seven days in stock Kafka). Anyone
with access to the brokers can read PINs and DSKs from that window. The PIN
only protects one inclusion against a man in the middle, but it is printed on
the device and is the same when it is paired again. A shorter `retention.ms`
on `platform` would narrow this.

## What the hub's Z-Wave controller must do

The messages above are the contract. These are the parts that are easy to get
wrong:

1. **Send the hub's Public Key Report before asking for the PIN.** The node
   waits only TB3 (10 s) for it. Derive the temporary keys once the PIN
   arrives; the node's KEX Set echoes can't be decrypted before then.
2. **Grant S2 Authenticated and Access Control only when a client can answer**,
   that is when the latest `START_PAIRING` had `s2PinEntry: true`. Decide when
   the key exchange starts. Otherwise grant only the classes that need no PIN.
3. **Ask for one PIN at a time.** Send `S2PinFinished` for one request before
   sending the next `S2PinRequired`, since the platform takes a new request as
   the end of the last.
4. **Send `S2PinRequired` point to point** to
   `Address.platformService(placeId, "subpairing")`, not as an event, which
   broadcasts. If the hub isn't connected, don't wait for an answer that can't
   come.
5. **Bound the wait** at `timeoutMs`, and end it early on `CancelS2Pin` or a KEX
   Fail from the node.
6. **Wait for the KEX Set echo for the rest of TAI2**, at least 40 s, once a PIN
   is used.
7. **Leaving pairing mode doesn't end a key exchange.** The hub's pairing window
   keeps its own timer while the platform holds the search open for the PIN, so
   a device that joins near the end of the window is still waiting when the
   window closes.
8. **Check the PIN with `S2Pin.isValidPin`.** Five digits alone lets through
   65536 to 99999, which don't fit in the two bytes the PIN fills.
9. **Keep the PIN and DSK out of logs.** The hub ships its logs to the
   platform's `irisLog` topic. Don't log the DSK, and mask Security 2 payloads
   in frame dumps, since the node's Public Key Report carries it. Keep these
   four messages out of anything that records platform messages, such as the
   hub's spy service, and never quote either value in an exception message.

## What a client must do

- Send `s2PinEntry: true` on `StartPairing` and `Search`.
- When `subpairing:s2PinRequestId` is set and not empty, call `GetS2PinRequest`. Show groups
  two to eight of the DSK and ask the user to check them against the device
  label, then ask for the first group, the PIN. Count down to `expires`.
- Call `SubmitS2Pin` once, or `CancelS2Pin` if the DSK doesn't match or the user
  gives up. On `request.param.invalid`, let the user fix the PIN and send again.
- `s2PinRequestId` goes empty as soon as a PIN goes to the hub. Keep the prompt
  until the `SubmitS2Pin` response arrives, since that carries the hub's error.
  Any other prompt for the request should close when it goes empty.
- Don't store, log or send the DSK or PIN anywhere else, and drop them once
  answered.
- On `PairingFailed` with a `zwave.s2.pin.*` code, tell the user to remove the
  device and pair it again.
