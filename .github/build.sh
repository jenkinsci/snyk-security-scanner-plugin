#!/bin/bash
set -e
SCRIPT_RELATIVE_DIR=$(dirname "${BASH_SOURCE[0]}")
pushd "$SCRIPT_RELATIVE_DIR"/.. || exit 1
  # use local .m2 repo to speed up build
  rsync -crlDv ~/.m2 ./.m2
  docker build -t jenkins-w3security -f .github/Dockerfile .
  rm -rf ./.m2
popd || exit
