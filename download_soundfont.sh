#!/usr/bin/env bash
set -euo pipefail

# where to put the downloaded ZIP and extracted SF2
SF2_DIR=src/main/resources/soundfonts
ZIP_FILE=$SF2_DIR/FluidR3_GM.zip
SF2_FILE=$SF2_DIR/FluidR3_GM.sf2

mkdir -p "$SF2_DIR"

echo "Downloading FluidR3_GM.zip SoundFont..."
if curl -fSL https://keymusician01.s3.amazonaws.com/FluidR3_GM.zip -o "$ZIP_FILE"; then
  echo "Extracting FluidR3_GM.sf2 from ZIP..."
  unzip -p "$ZIP_FILE" FluidR3_GM.sf2 > "$SF2_FILE"
  echo "✔ Saved SF2 to $SF2_FILE"
else
  echo "✖ Failed to download. Please fetch manually and place at $SF2_FILE"
  exit 1
fi
