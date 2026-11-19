#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
if ! command -v python3 >/dev/null 2>&1; then
  echo 'ThermaBio necesita Python 3 en Ubuntu 26.04.' >&2
  echo 'Instálalo con: sudo apt update && sudo apt install python3' >&2
  exit 127
fi
exec python3 "$SCRIPT_DIR/thermabio.py" "$@"
