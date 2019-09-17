platform-client is used by various programs (e.g. oculus and the android app) to connect to the platform.

# Building


## As a fatjar (for use in other projects)

`./gradlew :common:arcus-model:platform-client:shadowJar`

The resulting jar will be located in ./common/arcus-model/platform-client/build/libs/platform-client-all.jar

## As part of the platform

`./gradlew :common:arcus-model:platform-client:jar`

The resulting jar will be located in ./common/arcus-model/platform-client/build/libs/platform-client.jar
