#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
# Exercises the actual shipped menu runtime and structural bytecode resolver.
"${GRADLE:-gradle}" -p tests/fingerprints test --no-daemon "$@"
