#!/bin/bash
set -ex
SCRIPT_RELATIVE_DIR=$(dirname "${BASH_SOURCE[0]}")
pushd "$SCRIPT_RELATIVE_DIR"/.. || exit 1
docker run --rm -p 8080:8080 -p 50000:50000 --name=jenkins-snyk jenkins-snyk
popd || exit
