# Voice Bridges (Removed)

Alexa, Google Assistant, and the shared voice bridge were removed — they aren't
needed for self-hosted deployments and depended on third-party cloud services.
The code was behind the `enableVoice` feature flag and had zero dependencies from
other modules, so removal is clean.

## What the voice bridge did

The voice bridges followed a pattern built on top of `bridge-common` and `arcus-oauth`:

1. Each bridge ran a Netty HTTP server that accepted requests from the voice
   assistant cloud (Alexa POST to `/alexa/shs`, Google POST to `/ha`).
2. The bridge resolved the user's place from the OAuth token attached to the
   request (via `OAuthDAO`).
3. Requests were translated into `PlatformMessage`s and sent to `voice-service`
   over Kafka.
4. `voice-service` maintained a per-place in-memory cache of device models
   (`VoiceContext`), executed commands, and returned responses.
5. `voice-service` also handled proactive state reporting — pushing device
   changes to Alexa (Event Gateway) and Google (Home Graph gRPC API).

The shared `arcus-voice-bridge` module provided the glue between `arcus-oauth`
and the platform bus: when a user authorized a place via OAuth, the
`VoicePlaceSelectionHandler` (implementing `PlaceSelectionHandler` from
`arcus-oauth`) sent a `VoiceService.StartPlaceRequest` to activate the place
in the voice service cache. Deauthorization sent `StopPlaceRequest`.

## arcus-oauth (retained)

`platform/arcus-oauth` is a general-purpose OAuth2 server library. It provides:

- `AuthorizeHandler`, `TokenHandler`, `RevokeHandler` — standard OAuth2 endpoints
- `OAuthDAO` / `CassandraOAuthDAO` — token + attribute persistence in Cassandra
- `BearerAuth` — bearer token validation for stateless requests
- `PlaceSelectionHandler` interface — hook for reacting to place auth/deauth
- `AppRegistry` / `Application` — registered OAuth application definitions

Any future integration needing OAuth (e.g., an MQTT bridge) can reuse this module
by depending on `project(':platform:arcus-oauth')` and binding a
`PlaceSelectionHandler` implementation.

## Restoring from git

```bash
git log --all --oneline -- platform/arcus-alexa | head -1
# then: git checkout <commit> -- platform/arcus-alexa platform/arcus-google ...
```

You'd also need to restore the service XMLs (`alexaservice.xml`, `googleservice.xml`,
`voiceservice.xml`) and re-add the `enableVoice` block to `settings.gradle`.
