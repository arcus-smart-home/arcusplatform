#!/bin/bash
set -e
ROOT=$(git rev-parse --show-toplevel)
GRADLE=$ROOT/gradlew

if [[ "${GITHUB_REPOSITORY}" != 'wl-net/arcusplatform' ]]; then
  exit 0  # skip due to not being on a known repo
fi

echo "Building and publishing containers to '${REGISTRY_NAME}'"

$GRADLE :khakis:distDocker

echo "tagging"
$GRADLE :khakis:tagDocker
echo "pushing"
$GRADLE :khakis:pushDocker

$GRADLE pushDocker
