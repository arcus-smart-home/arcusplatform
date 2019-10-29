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

cd $ROOT/khakis
$ROOT/khakis/bin/build.sh
$ROOT/khakis/bin/tag.sh
$ROOT/khakis/bin/push.sh
cd -
$GRADLE pushDocker

