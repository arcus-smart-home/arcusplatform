#!/bin/bash
set -e
ROOT=$(git rev-parse --show-toplevel)
GRADLE=$ROOT/gradlew

if [[ "${TRAVIS_REPO_SLUG}" != 'wl-net/arcusplatform' ]]; then
  exit 0  # skip due to not being on a known repo
fi

if [ -z ${DOCKERHUB_USER+x} ]; then
  export REGISTRY_NAME=docker.pkg.github.com/$TRAVIS_REPO_SLUG
  echo "$GITHUB_SECRET" | docker login docker.pkg.github.com -u "$GITHUB_USERNAME" --password-stdin
else
  export DOCKER_PREFIX_OVERRIDE=$DOCKERHUB_USER
  echo "$DOCKERHUB_TOKEN" | docker login -u "$DOCKERHUB_USER" --password-stdin
fi

echo "Building and publishing containers to '${REGISTRY_NAME}'"

$GRADLE :khakis:distDocker

echo "tagging"
$GRADLE :khakis:tagDocker
echo "pushing"
$GRADLE :khakis:pushDocker

$GRADLE pushDocker
