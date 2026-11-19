#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"

if ! command -v pwsh >/dev/null 2>&1; then
  cat >&2 <<'EOF'
ThermaBio necesita PowerShell 7 (pwsh) en Ubuntu/WSL.
Instálalo una vez con las instrucciones oficiales de Microsoft:
https://learn.microsoft.com/powershell/scripting/install/install-ubuntu
Después vuelve a ejecutar: ./console/run-ubuntu.sh
EOF
  exit 127
fi

exec pwsh -NoLogo -NoProfile -File "$SCRIPT_DIR/thermabio.ps1" "$@"
