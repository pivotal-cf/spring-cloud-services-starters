#!/usr/bin/env bash

set -euo pipefail

readonly FLY_TARGET=scs

function main() {
	fly -t "$FLY_TARGET" sync

	pushd "$(dirname "$0")/../ci" >/dev/null
	echo "Setting starters pipeline..."
	fly --target "$FLY_TARGET" set-pipeline --pipeline starters-4.3.x \
		--config pipeline.yml \
		--var branch="4.3.x"
	popd >/dev/null
}

main
