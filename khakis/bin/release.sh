#!/bin/bash
set -e
ROOT=$(git rev-parse --show-toplevel)
GRADLE=$ROOT/gradlew

if [[ "${TRAVIS_REPO_SLUG}" != 'wl-net/arcusplatform' ]]; then
  exit 0  # skip due to not being on a known repo
fi

REGISTRY_SEPERATOR='/'
REGISTRY_NAME=docker.pkg.github.com/$TRAVIS_REPO_SLUG

echo "$GITHUB_SECRET" | docker login docker.pkg.github.com -u "$GITHUB_USERNAME" --password-stdin

echo "Building and publishing containers to ${REGISTRY_NAME}"

cd $ROOT/khakis
$ROOT/khakis/bin/build.sh
echo "tagging"
$ROOT/khakis/bin/tag.sh
echo "pushing"
$ROOT/khakis/bin/push.sh
cd -
$GRADLE pushDocker

