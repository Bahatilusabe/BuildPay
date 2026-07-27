#!/usr/bin/env bash
# Copy known font files from a source directory into src/resources/fonts/
# Usage: ./scripts/install-fonts.sh /path/to/fonts

set -euo pipefail
SRC_DIR="$1"
DEST_DIR="src/resources/fonts"

if [[ -z "$SRC_DIR" || ! -d "$SRC_DIR" ]]; then
  echo "Usage: $0 /path/to/fonts_dir"
  echo "Place Inter/Poppins (or Montserrat) TTF files in that directory and run this script."
  exit 1
fi

mkdir -p "$DEST_DIR"

FILES=(
  "Montserrat-ExtraBold.ttf"
  "Montserrat-Medium.ttf"
  "Inter-Medium.ttf"
  "Inter-SemiBold.ttf"
  "Inter-Bold.ttf"
  "Poppins-Regular.ttf"
  "Poppins-SemiBold.ttf"
)

for f in "${FILES[@]}"; do
  if [[ -f "$SRC_DIR/$f" ]]; then
    cp -v "$SRC_DIR/$f" "$DEST_DIR/"
  else
    echo "Skipping $f (not found in $SRC_DIR)"
  fi
done

echo "Done. Fonts copied to $DEST_DIR (where LoginLauncher will attempt to load them)."