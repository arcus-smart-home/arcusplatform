# Releasing Arcus

## Version Properties

Each subproject has a `version.properties` file with `major`, `minor`, `patch`, and `qualifier` fields. These are used by Gradle to tag Docker images and artifacts during builds.

## Release Workflow

### 1. Create a release branch

From the branch you want to release (typically `master`), run:

```
./gradlew branchRelease
```

This will:
- Create a `release-{major}.{minor}` branch at the current commit with the qualifier removed
- Bump the minor version on the original branch

For example, if `master` is at `2026.2.0-SNAPSHOT`:
- Creates branch `release-2026.2` at version `2026.2.0`
- Bumps `master` to `2026.3.0-SNAPSHOT`

### 2. Tag a release

From a `release-*` branch, run:

```
./gradlew tagRelease
```

This will:
- Tag the current commit as `v{major}.{minor}.{patch}`
- Bump the patch version for the next build

For example, on `release-2026.2` at version `2026.2.0`:
- Creates tag `v2026.2.0`
- Bumps the branch to `2026.2.1`

Both targets auto-push when running on the build server.

## Building Docker Images

```
./khakis/bin/build.sh
```

Images are tagged as `latest`. To build with a versioned tag from `version.properties`:

```
./gradlew :khakis:distDocker
```

## Tagging and Pushing Docker Images

`./khakis/bin/tag.sh` and `./khakis/bin/push.sh` accept two environment variables:

* `REGISTRY_SEPARATOR` - Separator between image name segments. Use `/` for gcr, `-` for DockerHub.
* `REGISTRY_NAME` - Registry to push to, e.g. `gcr.io/YOURPROJECT` or a DockerHub account name.

Tag then push:

```
REGISTRY_SEPARATOR='/' REGISTRY_NAME=gcr.io/arcus-238802 ./khakis/bin/tag.sh
REGISTRY_SEPARATOR='/' REGISTRY_NAME=gcr.io/arcus-238802 ./khakis/bin/push.sh
```
