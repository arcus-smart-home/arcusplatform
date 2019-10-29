#!/bin/bash
set -e
ROOT=$(git rev-parse --show-toplevel)
GRADLE=$ROOT/gradlew

if [[ "${TRAVIS_REPO_SLUG}" != 'wl-net/arcusplatform' ]]; then

fi

REGISTRY_SEPERATOR='/'
REGISTRY_NAME=docker.pkg.github.com/$TRAVIS_REPO_SLUG

cd $ROOT/khakis
$ROOT/khakis/bin/build.sh
$ROOT/khakis/bin/tag.sh
$ROOT/khakis/bin/push.sh
cd -
$GRADLE pushDocker

