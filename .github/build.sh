#!/bin/bash
set -e
SCRIPT_RELATIVE_DIR=$(dirname "${BASH_SOURCE[0]}")
pushd "$SCRIPT_RELATIVE_DIR"/.. || exit 1
  # use local .m2 repo to speed up build
  rsync -crlDv ~/.m2 ./.m2
  # --platform linux/amd64 is required for M1 Macs
  docker build --platform linux/amd64 -t jenkins-snyk -f .github/Dockerfile .
  rm -rf ./.m2
popd || exit
