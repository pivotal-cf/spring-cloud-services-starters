#!/bin/bash

set -euo pipefail

# shellcheck source=common.sh
source "$(dirname "$0")/common.sh"
repository=$(pwd)/distribution-repository


pushd git-repo >/dev/null
./gradlew --parallel clean build publish \
	-PpublicationRepository="${repository}"
popd >/dev/null