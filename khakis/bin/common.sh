#!/bin/bash

DOCKER_BIN="docker"
DOCKER_PROJECT="eyeris"

docker_build() {
    local DOCKER_PATH="$1"
    local DOCKER_NAME="${2:-$(basename ${DOCKER_PATH})}"
    local DOCKER_TAG=$(echo "${DOCKER_NAME}" |tr '-' '/')

    if [ -z "${bamboo_bitbucket_user}" ]; then
        "${DOCKER_BIN}" build -t "${DOCKER_TAG}" "${DOCKER_PATH}"
    else
        echo "running on build server, forcing clean rebuild..."
        "${DOCKER_BIN}" build --no-cache=true -t "${DOCKER_TAG}" "${DOCKER_PATH}"
    fi
}

docker_create() {
    "${DOCKER_BIN}" create $@
}

docker_delete() {
    "${DOCKER_BIN}" rm $@
}

docker_delete_image() {
    "${DOCKER_BIN}" rmi $@
}

docker_run() {
    echo "Executing: ${DOCKER_BIN} run $@"
    "${DOCKER_BIN}" run $@
}

docker_tag() {
    "${DOCKER_BIN}" tag $@
}

docker_push() {
    local COUNT="0"
    local SUCCESS="false"
    
    while [ $COUNT -lt 4 ]; do
        if "${DOCKER_BIN}" push $@; then
            echo "Push succeeded."
            COUNT="5"
            SUCCESS="true"
        else
            echo "Push failed."
            COUNT=$[$COUNT+1]
        fi
    done

    if [ "$SUCCESS" == "false" ]; then 
        echo "Push failed after 5 attempts."
        exit 1
    fi
}

docker_stop() {
    "${DOCKER_BIN}" stop $@
}

docker_exec() {
    "${DOCKER_BIN}" exec $@
}
