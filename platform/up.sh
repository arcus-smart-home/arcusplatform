#!/bin/bash

# Colors
GREEN='\e[0;32m'
LIGHT_GREY='\e[0;37m'
RED='\e[0;31m'
END_COLOR='\e[m'

function echo_green {
    echo -e "${GREEN}$1${END_COLOR}"
}

# Hop in boot2docker for OSX.
if hash boot2docker 2>/dev/null && boot2docker status == "poweroff"; then
    echo_green "Starting boot2docker"
    boot2docker start; $(boot2docker shellinit)
fi

# Fire up the platform.
echo_green "Starting platform"
gradle startService

# Add test data and user.
CONTAINER_ID=$(docker inspect -f '{{.Config.Hostname}}' eyeris-cassandra)
DIR=$(cd "$(dirname $0)" && pwd)
DATA="${DIR}/arcus-modelmanager/src/test/data/test_data.cql"
DOCKER_EXEC="docker exec -i eyeris-cassandra /bin/bash -c"

echo_green "Loading test data into cassandra"

if [[ $(uname) == Darwin ]]; then
    $DOCKER_EXEC "echo $(base64 ${DATA}) > testdata_base64"
else
    $DOCKER_EXEC "echo $(base64 --wrap=0 ${DATA}) > testdata_base64"
fi

$DOCKER_EXEC "base64 --decode testdata_base64 > test_data.cql"
$DOCKER_EXEC "cqlsh -f test_data.cql $CONTAINER_ID"

echo_green "You now need to create user via either oculus or a mobile app"

