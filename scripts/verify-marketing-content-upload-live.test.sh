#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SCRIPT="$ROOT_DIR/scripts/verify-marketing-content-upload-live.sh"

require_pattern() {
  local pattern="$1"
  local message="$2"
  if ! grep -Eq "$pattern" "$SCRIPT"; then
    echo "Missing live verifier production probe: $message" >&2
    exit 1
  fi
}

require_pattern 'Access-Control-Request-Method: PUT' 'S3/browser CORS preflight'
require_pattern 'invalid READY callback signature' 'bad HMAC rejection check'
require_pattern 'stale READY callback timestamp' 'replay-window rejection check'
require_pattern 'Verifying READY DAM asset' 'DAM asset READY row verification'
require_pattern 'marketing/content/assets.*asset_query' 'authenticated DAM asset lookup after callback'
require_pattern '!asset\.checksumSha256' 'strict DAM checksum presence check'
require_pattern 'checksum mismatch' 'strict DAM checksum equality check'

echo "verify-marketing-content-upload-live tests passed"
