#!/bin/bash
set -ex
SCRIPT_RELATIVE_DIR=$(dirname "${BASH_SOURCE[0]}")
pushd "$SCRIPT_RELATIVE_DIR"/.. || exit 1
cp -rp ~/.m2 ./.m2
docker build -t jenkins-snyk -f .github/Dockerfile "$PWD"
rm -rf ./.m2
popd || exit
