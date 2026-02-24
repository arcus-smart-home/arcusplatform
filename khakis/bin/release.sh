#!/bin/bash
set -e
ROOT=$(git rev-parse --show-toplevel)
GRADLE=$ROOT/gradlew

case "${GITHUB_REPOSITORY}" in
  wl-net/arcusplatform|arcus-smart-home/arcusplatform) ;;
  *) exit 0  # skip due to not being on a known repo ;;
esac

echo "Building and publishing containers to '${REGISTRY_NAME}'"

$GRADLE :khakis:distDocker

echo "tagging"
$GRADLE :khakis:tagDocker
echo "pushing"
$GRADLE :khakis:pushDocker

$GRADLE pushDocker
