#!/usr/bin/env bash

set -euo pipefail

readonly FLY_TARGET=scs

function main() {
	fly -t "$FLY_TARGET" sync

	pushd "$(dirname "$0")/../ci" >/dev/null
	echo "Setting starters pipeline..."
	fly --target "$FLY_TARGET" set-pipeline --pipeline starters \
		--config pipeline.yml \
		--instance-var "branch=main" \
		--var branch="main"
	popd >/dev/null
}

main
