#!/bin/bash

# Script useful for manage stack saps services life-cycle.
# See usage for commands & options.

set -o errexit;

readonly ARCHIVER_REPO=ufcgsaps/archiver
readonly CATALOG_REPO=ufcgsaps/catalog
readonly DISPATCHER_REPO=ufcgsaps/dispatcher
readonly SCHEDULER_REPO=ufcgsaps/scheduler

readonly ARCHIVER_CONTAINER=saps-archiver
readonly CATALOG_CONTAINER=saps-catalog
readonly DISPATCHER_CONTAINER=saps-dispatcher
readonly SCHEDULER_CONTAINER=saps-scheduler
readonly SAPS_NETWORK=saps-network

readonly CATALOG_USER=admin
readonly CATALOG_PASSWORD=admin
readonly CATALOG_DB=saps

readonly CATALOG_PORT=5432
readonly DISPATCHER_PORT=8091

readonly TEMP_STORAGE_DIR=/nfs

build_archiver() {
  local DOCKERFILE_DIR=docker/dockerfiles/archiver
  local TAG="${1}"
  docker build --tag "${ARCHIVER_REPO}":"${TAG}" \
            --file "${DOCKERFILE_DIR}" .
}

build_catalog() {
  local DOCKERFILE_DIR=docker/dockerfiles/catalog
  local TAG="${1}"
  docker build --tag "${CATALOG_REPO}":"${TAG}" \
            --file "${DOCKERFILE_DIR}" .
}

build_dispatcher() {
  local DOCKERFILE_DIR=docker/dockerfiles/dispatcher
  local TAG="${1}"
  docker build --tag "${DISPATCHER_REPO}":"${TAG}" \
            --file "${DOCKERFILE_DIR}" .
}

build_scheduler() {
  local DOCKERFILE_DIR=docker/dockerfiles/scheduler
  local TAG="${1}"
  docker build --tag "${SCHEDULER_REPO}":"${TAG}" \
            --file "${DOCKERFILE_DIR}" .
}

check_network() {
  local exists="$(docker network ls --format "{{.Name}}" --filter name=${SAPS_NETWORK})"
  if [ ! "${exists}" ]; then
    echo "Creating saps network..."
    docker network create "${SAPS_NETWORK}"
  fi
}

run_archiver() {
  local TAG="${1}"
  docker run -dit \
    --name "${ARCHIVER_CONTAINER}" \
    --net="${SAPS_NETWORK}" --net-alias=archiver \
    -v "$(pwd)"/config/archiver.conf:/archiver/archiver.conf \
    -v "$(pwd)"/config/log4j.properties:/archiver/log4j.properties \
    -v "${TEMP_STORAGE_DIR}":/archiver/nfs \
    "${ARCHIVER_REPO}":"${TAG}"
}

run_catalog() {
  local TAG="${1}"
  docker run -dit \
    --name "${CATALOG_CONTAINER}" \
    -p "${CATALOG_PORT}":5432 \
    --net="${SAPS_NETWORK}" --net-alias=catalog \
    -v catalogdata:/var/lib/postgresql/data \
    -e POSTGRES_USER="${CATALOG_USER}" \
    -e POSTGRES_PASSWORD="${CATALOG_PASSWORD}" \
    -e POSTGRES_DB="${CATALOG_DB}" \
    "${CATALOG_REPO}":"${TAG}"
}

run_dispatcher() {
  local TAG="${1}"
  local CONF_FILE="dispatcher.conf"
  docker run -dit \
    --name "${DISPATCHER_CONTAINER}" \
    -p "${DISPATCHER_PORT}":8091 \
    --net="${SAPS_NETWORK}" --net-alias=dispatcher \
    -v "$(pwd)"/config/"${CONF_FILE}":/dispatcher/"${CONF_FILE}" \
    -v "$(pwd)"/config/log4j.properties:/dispatcher/log4j.properties \
    -v "$(pwd)"/resources/execution_script_tags.json:/dispatcher/resources/execution_script_tags.json \
    -v "${TEMP_STORAGE_DIR}":/dispatcher/nfs \
    "${DISPATCHER_REPO}":"${TAG}"
}

run_scheduler() {
  local TAG="${1}"
  local CONF_FILE="scheduler.conf"
  docker run -dit \
    --name "${SCHEDULER_CONTAINER}" \
    --net="${SAPS_NETWORK}" --net-alias=scheduler \
    -v "$(pwd)"/config/"${CONF_FILE}":/scheduler/"${CONF_FILE}" \
    -v "$(pwd)"/config/log4j.properties:/scheduler/log4j.properties \
    -v "$(pwd)"/resources/execution_script_tags.json:/dispatcher/resources/execution_script_tags.json \
    "${SCHEDULER_REPO}":"${TAG}"
}

access_catalog() {
  local CATALOG_IP=$(docker inspect -f '{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}' ${CATALOG_CONTAINER})
  psql -h "${CATALOG_IP}" -p ${CATALOG_PORT} ${CATALOG_DB} ${CATALOG_USER}
}

access() {
  local service="${1}"
  case $service in
    catalog)
      access_catalog
      ;;
  esac
}

build() {
  local service="${1}"
  local tag="${2-latest}"
  case $service in
    archiver)
      build_archiver ${tag}
      ;;
    catalog)
      build_catalog ${tag}
      ;;
    dispatcher)
      build_dispatcher ${tag}
      ;;
    scheduler)
      build_scheduler ${tag}
  esac
}

run() {
  local service="${1}"
  local tag="${2:-latest}"
  check_network
  case $service in
    archiver)
      run_archiver ${tag}
      ;;
    catalog)
      run_catalog ${tag}
      ;;
    dispatcher)
      run_dispatcher ${tag}
      ;;
    scheduler)
      run_scheduler ${tag}
  esac
}

restart_template() {
  local CONTAINER_NAME="$1"
  local SERVICE="$2"
  local TAG=$(docker inspect -f '{{.Config.Image}}' ${CONTAINER_NAME} | cut -d':' -f2)
  docker stop "${CONTAINER_NAME}"
  docker rm "${CONTAINER_NAME}"
  run ${SERVICE} ${TAG}
}

restart() {
  while [[ $# -ne 0 ]]
  do
    case $1 in
      archiver) shift
        restart_template ${ARCHIVER_CONTAINER} archiver
        ;;
      catalog) shift
        restart_template ${CATALOG_CONTAINER} catalog
        ;;
      dispatcher) shift
        restart_template ${DISPATCHER_CONTAINER} dispatcher
        ;;
      scheduler) shift
        restart_template ${SCHEDULER_CONTAINER} scheduler
        ;;
      *) shift
        ;;
    esac
  done
}

# Stop and remove all containers
stop() {
  for service in ${ARCHIVER_CONTAINER} ${CATALOG_CONTAINER} ${DISPATCHER_CONTAINER} ${SCHEDULER_CONTAINER};
  do
    docker stop "${service}"
    docker rm "${service}"
  done
}

publish()
{
  local service="${1}"
  local tag="${2:-latest}"
  case $service in
    archiver)
      docker push "${ARCHIVER_REPO}":"${tag}"
      ;;
    catalog)
      docker push "${CATALOG_REPO}":"${tag}"
      ;;
    dispatcher)
      docker push "${DISPATCHER_REPO}":"${tag}"
      ;;
    scheduler)
      docker push "${SCHEDULER_REPO}":"${tag}"
      ;;
  esac
}

define_params() {
  case $1 in 
    access) shift
      access "$@"
      ;;
    build) shift
      build "$@"
      ;;
    run) shift
      run "$@"
      ;;
    restart) shift
      restart "$@"
      ;;
    stop) shift
      stop "$@"
      ;;
    publish) shift
      publish "$@"
      ;;
  esac
}

define_params "$@"