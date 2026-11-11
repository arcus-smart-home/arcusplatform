#!/usr/bin/env bash
#
# Starts ZooKeeper, Kafka and Cassandra, sets up the schema, then starts each
# service a deployment runs and waits for its health check. Fails if any
# service exits, or is not healthy within the timeout.
#
# Usage: khakis/smoke-test/run.sh [--build] [--keep] [service ...]
#
#   --build   build the images first (khakis, modelmanager, the services)
#   --keep    leave the containers running afterwards, for poking at
#   service   only start these services (default: all of them)
#
# Environment:
#   SMOKE_TIMEOUT  seconds to wait for every service to be healthy (default 420)
#   SMOKE_LOGS     where to write each container's log (default build/smoke-test)
#
# Written for bash 3.2 too, so it runs as-is on macOS.

set -euo pipefail

HERE=$(cd "$(dirname "$0")" && pwd)
ROOT=$(cd "${HERE}/../.." && pwd)

ALL_SERVICES="platform-services driver-services subsystem-service rule-service
scheduler-service history-service notification-services client-bridge
hub-bridge api-bridge ivr-callback-server"

BUILD=false
KEEP=false
SERVICES=""
for arg in "$@"; do
   case "${arg}" in
      --build) BUILD=true ;;
      --keep) KEEP=true ;;
      -*) echo "Unknown option: ${arg}" >&2; exit 2 ;;
      *) SERVICES="${SERVICES} ${arg}" ;;
   esac
done
SERVICES=$(echo ${SERVICES:-${ALL_SERVICES}})

TIMEOUT=${SMOKE_TIMEOUT:-420}
LOGS=${SMOKE_LOGS:-${ROOT}/build/smoke-test}

# The images are tagged with the version being worked toward, without the
# qualifier (see platform/build.gradle).
prop() { grep -m1 "^$1=" "${ROOT}/platform/version.properties" | cut -d= -f2- | tr -d '[:space:]'; }
export ARCUS_VERSION="$(prop major).$(prop minor).$(prop patch)"

compose() { docker compose -f "${HERE}/docker-compose.yml" "$@"; }

if ${BUILD}; then
   # Two runs: every service image builds FROM arcus/java, which the first
   # one makes.
   (cd "${ROOT}" && ./gradlew --no-daemon :khakis:distDocker)
   TASKS=":platform:arcus-modelmanager:distDocker"
   for s in ${SERVICES}; do
      TASKS="${TASKS} :platform:arcus-containers:${s}:distDocker"
   done
   (cd "${ROOT}" && ./gradlew --no-daemon ${TASKS})
fi

# Where modelmanager is installed in its image, e.g.
# /modelmanager-2026.11.1-SNAPSHOT/. Compose needs it for the history schema.
MODELMANAGER_INSTALL=$(docker run --rm --entrypoint printenv \
   "arcus/modelmanager:${ARCUS_VERSION}" MODELMANAGER_HOME)
export MODELMANAGER_INSTALL

# hub-bridge runs mutual TLS: give it a self-signed certificate and key, and
# trust that same certificate for client certificates. Readable by the
# container's own user.
SMOKE_CERTS=${ROOT}/build/smoke-test-certs
export SMOKE_CERTS
KEYTOOL=${JAVA_HOME:+${JAVA_HOME}/bin/}keytool
rm -rf "${SMOKE_CERTS}"
mkdir -p "${SMOKE_CERTS}"
openssl req -x509 -newkey rsa:2048 -nodes -days 7 -subj /CN=hub-bridge \
   -keyout "${SMOKE_CERTS}/tls.key" -out "${SMOKE_CERTS}/tls.crt" 2>/dev/null
"${KEYTOOL}" -importcert -noprompt -alias smoke-test -storetype JKS -storepass smoke-test \
   -file "${SMOKE_CERTS}/tls.crt" -keystore "${SMOKE_CERTS}/truststore.jks" >/dev/null
chmod a+rx "${SMOKE_CERTS}"
chmod a+r "${SMOKE_CERTS}"/*

save_logs() {
   mkdir -p "${LOGS}"
   for s in zookeeper kafka cassandra ${SERVICES}; do
      compose logs --no-color --timestamps "${s}" > "${LOGS}/${s}.log" 2>&1 || true
   done
}

cleanup() {
   local status=$?
   save_logs
   if ${KEEP}; then
      echo "Leaving the containers running (--keep). Stop them with:"
      echo "  ARCUS_VERSION=${ARCUS_VERSION} MODELMANAGER_INSTALL=${MODELMANAGER_INSTALL} SMOKE_CERTS=${SMOKE_CERTS} docker compose -f ${HERE}/docker-compose.yml down -v"
   else
      compose --profile setup down -v --remove-orphans >/dev/null 2>&1 || true
   fi
   echo "Container logs are in ${LOGS}"
   exit ${status}
}

# Start clean, in case an earlier run was killed before it could tidy up.
compose --profile setup down -v --remove-orphans >/dev/null 2>&1 || true
rm -rf "${LOGS}"
trap cleanup EXIT

state() {
   local id
   id=$(compose ps -a -q "$1")
   if [ -z "${id}" ]; then
      echo "missing - -"
      return
   fi
   docker inspect -f '{{.State.Status}} {{if .State.Health}}{{.State.Health.Status}}{{else}}none{{end}} {{.State.ExitCode}}' "${id}"
}

echo "==> Starting ZooKeeper, Kafka and Cassandra"
compose up -d zookeeper kafka cassandra

echo "==> Waiting for Cassandra"
deadline=$((SECONDS + 300))
until [ "$(state cassandra | cut -d' ' -f2)" = "healthy" ]; do
   if [ ${SECONDS} -ge ${deadline} ]; then
      echo "Cassandra did not come up within 300s" >&2
      compose logs --no-color --tail 80 cassandra >&2
      exit 1
   fi
   sleep 3
done

echo "==> Creating keyspaces and topics"
compose exec -T cassandra cassandra-cmd setup
compose exec -T kafka kafka-cmd setup

echo "==> Applying the schema"
compose run --rm -T modelmanager-platform
compose run --rm -T modelmanager-history

echo "==> Starting:" ${SERVICES}
started=${SECONDS}
compose up -d ${SERVICES}

pending="${SERVICES}"
healthy=""
failed=""
deadline=$((SECONDS + TIMEOUT))
while [ -n "${pending}" ]; do
   still=""
   for s in ${pending}; do
      set -- $(state "${s}")
      case "$1/$2" in
         running/healthy)
            echo "    ok    ${s} ($((SECONDS - started))s)"
            healthy="${healthy} ${s}"
            ;;
         exited/*|dead/*|missing/*)
            echo "    FAIL  ${s} exited with status $3"
            failed="${failed} ${s}"
            ;;
         *)
            still="${still} ${s}"
            ;;
      esac
   done
   pending=$(echo ${still})
   if [ -n "${pending}" ] && [ ${SECONDS} -ge ${deadline} ]; then
      for s in ${pending}; do
         echo "    FAIL  ${s} not healthy after ${TIMEOUT}s"
         failed="${failed} ${s}"
      done
      break
   fi
   [ -n "${pending}" ] && sleep 2
done

# A service that answers its health check and then falls over a moment later
# is not started either.
if [ -n "${healthy}" ]; then
   sleep 15
   for s in ${healthy}; do
      set -- $(state "${s}")
      if [ "$1" != "running" ]; then
         echo "    FAIL  ${s} exited with status $3 after becoming healthy"
         failed="${failed} ${s}"
      fi
   done
fi

failed=$(echo ${failed})
if [ -n "${failed}" ]; then
   for s in ${failed}; do
      echo
      echo "==> Last lines from ${s}"
      compose logs --no-color --tail 60 "${s}" 2>&1 || true
   done
   echo
   echo "Smoke test FAILED:" ${failed}
   exit 1
fi

echo
echo "Smoke test passed: every service started ($((SECONDS - started))s)."
