#!/bin/bash

DOCKER_BIN="docker"
DOCKER_PROJECT="arcus"

docker_build() {
    local DOCKER_PATH="$1"
    local DOCKER_NAME="${2:-$(basename ${DOCKER_PATH})}"
    local DOCKER_TAG=$(echo "${DOCKER_NAME}" |tr '-' '/')
    if [ $DOCKER_VERSION ]; then
        local DOCKER_VERSION=":${DOCKER_VERSION}"
    fi

    if [ -z "${is_build_server}" ]; then
        "${DOCKER_BIN}" build -t "${DOCKER_TAG}${DOCKER_VERSION}" -t "${DOCKER_TAG}:latest" "${DOCKER_PATH}"
    else
        echo "running on build server, forcing clean rebuild..."
        "${DOCKER_BIN}" build --no-cache=true -t "${DOCKER_TAG}" "${DOCKER_PATH}"
    fi
}

docker_create() {
    "${DOCKER_BIN}" create "$@"
}

docker_delete() {
    "${DOCKER_BIN}" rm "$@"
}

docker_delete_image() {
    "${DOCKER_BIN}" rmi "$@"
}

docker_run() {
    echo "Executing: ${DOCKER_BIN} run $@"
    "${DOCKER_BIN}" run "$@"
}

docker_tag() {
    "${DOCKER_BIN}" tag "$@"
}

docker_push() {
    local COUNT="0"
    local SUCCESS="false"
    
    while [ $COUNT -lt 4 ]; do
        if "${DOCKER_BIN}" push "$@"; then
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
    "${DOCKER_BIN}" stop "$@"
}

docker_exec() {
    "${DOCKER_BIN}" exec "$@"
}

findroot() {
    local  __resultvar=$1
    result=$(git rev-parse --show-toplevel)
    eval $__resultvar="'$result'"
}

function prompt() {
  local  __resultvar=$1
  echo -n "${2} "
  local  myresult=''
  read myresult
  eval $__resultvar="'$myresult'"
}
