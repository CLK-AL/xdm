#!/usr/bin/env bash
# restore-redacted.sh
#
# Downloads files that were redacted for GitHub push protection from their
# upstream repositories and patches the local copies.
#
# Usage: ./restore-redacted.sh
#
# Currently restores:
#   - src/py/yt-dlp/yt_dlp/extractor/shahid.py (AWS API keys for Shahid.net extractor)

set -euo pipefail

UPSTREAM_RAW="https://raw.githubusercontent.com/yt-dlp/yt-dlp/master/yt_dlp/extractor/shahid.py"
LOCAL_FILE="src/py/yt-dlp/yt_dlp/extractor/shahid.py"

echo "Restoring redacted files from upstream repositories..."

if [ ! -f "$LOCAL_FILE" ]; then
    echo "ERROR: $LOCAL_FILE not found. Run from the repository root."
    exit 1
fi

echo "  Fetching: $UPSTREAM_RAW"
if command -v curl &>/dev/null; then
    curl -fsSL "$UPSTREAM_RAW" -o "$LOCAL_FILE"
elif command -v wget &>/dev/null; then
    wget -qO "$LOCAL_FILE" "$UPSTREAM_RAW"
else
    echo "ERROR: Neither curl nor wget found."
    exit 1
fi

echo "  Restored: $LOCAL_FILE"
echo ""
echo "Done. Redacted files have been restored from upstream."
echo "NOTE: These files contain API keys that trigger GitHub push protection."
echo "      Do not commit them back — they are for local use only."
