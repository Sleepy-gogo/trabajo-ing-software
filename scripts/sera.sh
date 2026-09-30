#!/usr/bin/env sh
# En Linux/macOS: sh scripts/sera.sh setup
set -eu
exec python3 "$(dirname "$0")/sera.py" "$@"
