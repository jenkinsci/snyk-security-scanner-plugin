#!/bin/bash
set -e
SCRIPT_RELATIVE_DIR=$(dirname "${BASH_SOURCE[0]}")
pushd "$SCRIPT_RELATIVE_DIR"/.. || exit 1
HOST="http://$(echo "$DOCKER_HOST" | cut -f3 -d "/" | cut -f1 -d":"):8080"
echo "Please connect to IP address $HOST"
docker run --rm -p 8080:8080 -p 50000:50000 --name=jenkins-w3security jenkins-w3security
popd || exit
