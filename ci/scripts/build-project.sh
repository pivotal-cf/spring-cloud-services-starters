#!/bin/bash
set -euo pipefail

# shellcheck source=scripts/common.sh
source "$(dirname "$0")/common.sh"
repository=$(pwd)/distribution-repository

pushd git-repo >/dev/null
./gradlew --no-daemon clean build publishToMavenLocal -Dmaven.repo.local="${repository}" -PbuildmasterName="${ARTIFACTORY_USERNAME}" -PbuildmasterPass="${ARTIFACTORY_PASSWORD}"
popd >/dev/null
