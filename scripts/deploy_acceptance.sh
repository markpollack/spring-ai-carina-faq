#!/usr/bin/env bash

export SCRIPT_DIR="$(dirname "$(stat -f "$0")")"
pushd $SCRIPT_DIR/.. # cd to project root

./mvnw clean package
cf target -s acceptance
cf push

popd # return to previous dir
