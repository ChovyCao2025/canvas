#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SCRIPT="$ROOT_DIR/scripts/verify-marketing-content-release-live.sh"

require_pattern() {
  local pattern="$1"
  local message="$2"
  if ! grep -Eq "$pattern" "$SCRIPT"; then
    echo "Missing live release verifier production probe: $message" >&2
    exit 1
  fi
}

require_pattern 'Existing READY DAM asset' 'READY asset prerequisite check'
require_pattern '/marketing/content/releases/validate' 'release validation gate'
require_pattern '/marketing/content/releases/publish' 'release publish call'
require_pattern 'release_key/resolve' 'runtime release resolve call'
require_pattern 'exactly one ACTIVE release' 'single active release verification'
require_pattern 'release_key/rollback' 'release rollback call'
require_pattern 'RELEASE_PUBLISHED' 'published audit verification'
require_pattern 'RELEASE_SUPERSEDED' 'superseded audit verification'
require_pattern 'RELEASE_ROLLED_BACK' 'rollback audit verification'
require_pattern 'RELEASE_RESTORED' 'restore audit verification'

echo "verify-marketing-content-release-live tests passed"
