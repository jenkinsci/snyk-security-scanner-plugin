#!/bin/bash
set -e
SCRIPT_RELATIVE_DIR=$(dirname "${BASH_SOURCE[0]}")
pushd "$SCRIPT_RELATIVE_DIR"/.. || exit 1
HOST="http://$(echo "$DOCKER_HOST" | cut -f3 -d "/" | cut -f1 -d":"):8080"
echo "Please connect to IP address $HOST"
# --platform linux/amd64 is required for M1 Macs
# to persist data:
  # create a volume using docker `volume create <VOLUME_NAME>`
  # mount the volume via docker run command using `-v <VOLUME_NAME>:/var/jenkins_home`
docker run --rm --platform linux/amd64 -p 8080:8080 -p 50000:50000 --name=jenkins-snyk jenkins-snyk
popd || exit
