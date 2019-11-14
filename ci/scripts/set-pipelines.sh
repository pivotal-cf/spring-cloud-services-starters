#!/usr/bin/env bash

set -euo pipefail

readonly SCS_SECRETS_LAST_PASS_ID="7602408477377037219"

secrets_file=$(mktemp).yml

fetch_secrets() {
  lpass show --notes "${SCS_SECRETS_LAST_PASS_ID}" > "${secrets_file}"
}

set_starters_pipeline() {
  echo "Setting starters pipeline..."
  fly -t scs set-pipeline -p starters -c pipeline.yml -l config-concourse.yml -l "${secrets_file}"
}

cleanup() {
  rm "${secrets_file}"
}

trap "cleanup" EXIT

main() {
  fly -t scs sync

  pushd "$(dirname $0)/.." > /dev/null
    fetch_secrets
    set_starters_pipeline
  popd > /dev/null
}

main
