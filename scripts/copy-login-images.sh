#!/usr/bin/env bash
# Copies the attached design images into the login images folder.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
SRC="$ROOT/src/ui/login/images"
if [ ! -d "$SRC" ]; then
  echo "Login images directory not found: $SRC" >&2
  exit 1
fi
# This script expects you to place the image files next to the script and name them buildpay-logo.png and silhouette.png
if [ -f "$ROOT/buildpay-logo.png" ]; then
  cp "$ROOT/buildpay-logo.png" "$SRC/buildpay-logo.png"
  echo "Copied buildpay-logo.png -> $SRC/buildpay-logo.png"
fi
if [ -f "$ROOT/silhouette.png" ]; then
  cp "$ROOT/silhouette.png" "$SRC/silhouette.png"
  echo "Copied silhouette.png -> $SRC/silhouette.png"
fi

echo "Done. Re-run: export OPENJFX_LIB=\"/Users/mac/Downloads/javafx-sdk-26.0.1/lib\" && ./run-login.sh" 
