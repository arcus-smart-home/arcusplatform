# libs/ — Flat Directory Repository

This directory is a Gradle `flatDir` repository for artifacts not available on Maven Central.

## Contents

| File | Purpose |
|------|---------|
| `ipcd-lib-1.0.jar` | IPCD (IP-Connected Device) protocol library — built from [arcusipcd](https://github.com/arcus-smart-home/arcusipcd) |
| `netty-transport-native-epoll-{version}-linux-arm_32.jar` | ARM32 epoll native for Iris hub hardware |

## Rebuilding the Netty ARM32 Native

The epoll jar is a build output from the ARM32 cross-compilation Docker build.
To rebuild (e.g. after bumping the Netty version in `agent/gradle.properties`):

```bash
./gradlew :agent:arcus-hal:arcus-hal-hub-v2:buildNettyArm32
```

See `agent/arcus-hal/hub-v2/docker/Dockerfile.netty-arm32` for details.
