#!/usr/bin/env bash

set -euo pipefail

main() {
  fly -t runway sync

  pushd "$(dirname $0)/.." > /dev/null
    echo "Setting starters pipeline..."
    fly -t runway set-pipeline -p starters-3.1.x -c pipeline.yml -l config-concourse.yml
  popd > /dev/null
}

main
