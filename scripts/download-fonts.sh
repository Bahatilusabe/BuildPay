#!/usr/bin/env bash
# downloads open-source fonts (Inter, Poppins, Montserrat) from the GoogleFonts GitHub repo
# and places them into src/resources/fonts/ so the launcher can load them.
# Usage:
#   ./scripts/download-fonts.sh
# or (safer) run it interactively and inspect the files.

set -euo pipefail
ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
DEST_DIR="$ROOT_DIR/src/resources/fonts"

mkdir -p "$DEST_DIR"
cd "$DEST_DIR"

declare -A URLS
URLS[
  "Poppins-Regular.ttf"]="https://github.com/google/fonts/raw/main/ofl/poppins/Poppins-Regular.ttf"
URLS[
  "Poppins-SemiBold.ttf"]="https://github.com/google/fonts/raw/main/ofl/poppins/Poppins-SemiBold.ttf"
URLS[
  "Inter-Regular.ttf"]="https://github.com/google/fonts/raw/main/ofl/inter/Inter-Regular.ttf"
URLS[
  "Inter-SemiBold.ttf"]="https://github.com/google/fonts/raw/main/ofl/inter/Inter-SemiBold.ttf"
URLS[
  "Inter-Bold.ttf"]="https://github.com/google/fonts/raw/main/ofl/inter/Inter-Bold.ttf"
URLS[
  "Montserrat-ExtraBold.ttf"]="https://github.com/google/fonts/raw/main/ofl/montserrat/Montserrat-ExtraBold.ttf"
URLS[
  "Montserrat-Medium.ttf"]="https://github.com/google/fonts/raw/main/ofl/montserrat/Montserrat-Medium.ttf"

)

echo "Downloading fonts into $DEST_DIR ..."

for name in "${!URLS[@]}"; do
  url="${URLS[$name]}"
  echo -n "- $name ... "
  if command -v curl >/dev/null 2>&1; then
    if curl -fSL -o "$name" "$url"; then
      echo "OK"
    else
      echo "FAILED"
    fi
  elif command -v wget >/dev/null 2>&1; then
    if wget -q -O "$name" "$url"; then
      echo "OK"
    else
      echo "FAILED"
    fi
  else
    echo "ERROR: curl or wget required to download fonts."
    exit 1
  fi
done

# Create copies with names expected by LoginLauncher
# The launcher looks for Inter-Medium.ttf; many repos supply Inter-Regular, so copy it.
if [[ -f "Inter-Regular.ttf" && ! -f "Inter-Medium.ttf" ]]; then
  cp -v Inter-Regular.ttf Inter-Medium.ttf || true
fi

# Print a summary
echo
ls -lh "$DEST_DIR" | sed -n '1,200p'

echo
cat <<EOF
Done. Fonts placed into $DEST_DIR.
Run your launcher (LoginLauncher) again; it will detect these fonts and the runtime font warnings should disappear.
EOF
