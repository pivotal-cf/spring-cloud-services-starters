#!/bin/bash
set -euo pipefail

readonly BUILD_INFO_LOCATION="$(pwd)/artifactory/build-info.json"
readonly CONFIG_DIR="$(pwd)/git-repo/ci/config"

java -jar /opt/concourse-release-scripts*.jar \
	--spring.config.location="${CONFIG_DIR}/release-scripts.yml" \
	publishToCentral 'RELEASE' "$BUILD_INFO_LOCATION" "artifactory"

echo "Sync complete"
