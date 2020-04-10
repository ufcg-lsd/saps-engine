#!/bin/bash

# Script useful for manage stack saps services life-cycle.
# See usage for commands & options.

readonly ARCHIVER_REPO=wesleymonte/archiver
readonly CATALOG_REPO=wesleymonte/catalog
readonly DISPATCHER_REPO=wesleymonte/dispatcher
readonly SCHEDULER_REPO=wesleymonte/scheduler

readonly DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"

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
  docker build --tag "${DISPATCHER_REPO}":"${TAG}" \
            --file "${DOCKERFILE_DIR}" .
}

build_scheduler() {
  local DOCKERFILE_DIR=docker/dockerfiles/scheduler
  local TAG="${1}"
  docker build --tag "${SCHEDULER_REPO}":"${TAG}" \
            --file "${DOCKERFILE_DIR}" .
}

run_archiver() {
  local TAG="${1}"
  local CONTAINER_NAME="saps-archiver"
  docker run -dit \
    --name "${CONTAINER_NAME}" \
    -v "$(pwd)"/config/archiver.conf:/archiver/archiver.conf \
    -v "$(pwd)"/config/log4j.properties:/archiver/log4j.properties \
    "${ARCHIVER_REPO}":"${TAG}"
}

run_catalog() {
  local TAG="${1}"
  local PORT="5432"
  local CONTAINER_NAME="saps-catalog"
  docker run -dit \
    --name "${CONTAINER_NAME}" \
    -p "${PORT}":"${PORT}" \
    -e POSTGRES_USER="admin" \
    "${CATALOG_REPO}":"${TAG}"
}

run_dispatcher() {
  local TAG="${1}"
  local CONTAINER_NAME="saps-dispatcher"
  local CONF_FILE="dispatcher.conf"
  local PORT="8091"
  docker run -dit \
    --name "${CONTAINER_NAME}" \
    -p "${PORT}":"${PORT}" \
    -v "$(pwd)"/config/"${CONF_FILE}":/dispatcher/"${CONF_FILE}" \
    -v "$(pwd)"/config/log4j.properties:/dispatcher/log4j.properties \
    "${DISPATCHER_REPO}":"${TAG}"
}

run_scheduler() {
  local TAG="${1}"
  local CONTAINER_NAME="saps-scheduler"
  local CONF_FILE="scheduler.conf"
  docker run -dit \
    --name "${CONTAINER_NAME}" \
    -v "$(pwd)"/config/"${CONF_FILE}":/scheduler/"${CONF_FILE}" \
    -v "$(pwd)"/config/log4j.properties:/scheduler/log4j.properties \
    "${SCHEDULER_REPO}":"${TAG}"
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
    build)
      build $2 $3
      ;;
    run)
      run $2 $3
      ;;
    publish)
      publish $2 $3
      ;;
  esac
}

define_params "$@"