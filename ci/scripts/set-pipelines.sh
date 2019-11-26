#!/usr/bin/env bash

set -euo pipefail

main() {
  fly -t scs sync

  pushd "$(dirname $0)/.." > /dev/null
    echo "Setting starters pipeline..."
    fly -t scs set-pipeline -p starters -c pipeline.yml -l config-concourse.yml
  popd > /dev/null
}

main
