#!/usr/bin/env bash

set -euo pipefail

readonly FLY_TARGET=scs

main() {
  fly -t "$FLY_TARGET" sync

  pushd "$(dirname $0)/../ci" > /dev/null
    echo "Setting starters pipeline..."
    fly --target "$FLY_TARGET" set-pipeline \
      --pipeline starters-2.4.x \
      --config pipeline.yml \
      --load-vars-from config-concourse.yml \
      --var branch="asalgado+gareth/concourse-migration-for-2_4_0-209"
  popd > /dev/null
}

main
