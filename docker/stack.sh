#!/bin/bash

# Script useful for manage stack saps services life-cycle.
# See usage for commands & options.

set -o errexit;

readonly ARCHIVER_REPO=wesleymonte/archiver
readonly CATALOG_REPO=wesleymonte/catalog
readonly DISPATCHER_REPO=wesleymonte/dispatcher
readonly SCHEDULER_REPO=wesleymonte/scheduler

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
  mvn clean install -DskipTests
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
  mvn clean install -DskipTests
  docker build --tag "${DISPATCHER_REPO}":"${TAG}" \
            --file "${DOCKERFILE_DIR}" .
}

build_scheduler() {
  local DOCKERFILE_DIR=docker/dockerfiles/scheduler
  local TAG="${1}"
  mvn clean install -DskipTests
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

restart() {
  while [[ $# -ne 0 ]]
  do
    case $1 in
      archiver) shift
        tag=$(docker inspect -f '{{.Config.Image}}' ${ARCHIVER_CONTAINER} | cut -d':' -f2)
        docker stop ${ARCHIVER_CONTAINER}
        docker rm ${ARCHIVER_CONTAINER}
        run archiver ${tag}
        ;;
      catalog) shift
        tag=$(docker inspect -f '{{.Config.Image}}' ${CATALOG_CONTAINER} | cut -d':' -f2)
        docker stop ${CATALOG_CONTAINER}
        docker rm ${CATALOG_CONTAINER}
        run catalog ${tag}
        ;;
      dispatcher) shift
        tag=$(docker inspect -f '{{.Config.Image}}' ${DISPATCHER_CONTAINER} | cut -d':' -f2)
        docker stop ${DISPATCHER_CONTAINER}
        docker rm ${DISPATCHER_CONTAINER}
        run dispatcher ${tag}
        ;;
      scheduler) shift
        tag=$(docker inspect -f '{{.Config.Image}}' ${SCHEDULER_CONTAINER} | cut -d':' -f2)
        docker stop ${SCHEDULER_CONTAINER}
        docker rm ${SCHEDULER_CONTAINER}
        run scheduler ${tag}
        ;;
      *) shift
        ;;
    esac
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
    publish) shift
      publish "$@"
      ;;
  esac
}

define_params "$@"