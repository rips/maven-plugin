#!/usr/bin/env bash
set -e

SCRIPT_PATH=$( cd "$(dirname "${BASH_SOURCE[0]}")" ; pwd -P )
cd "${SCRIPT_PATH}/.."

if [[ "$1" == "master" ]]; then
    mvn deploy -Dmaven.test.skip=true -Dbamboo.planRepository.branchName=master
fi

if [[ "$1" == "dev" ]]; then
    mvn deploy -Dmaven.test.skip=true -Dbamboo.planRepository.branchName=dev
fi
