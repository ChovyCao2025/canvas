#!/usr/bin/env bash
# Classifies Flyway validation failures from a boot or migration log without mutating the database.
set -euo pipefail

INPUT_FILE=""

usage() {
  cat <<'USAGE'
Classify Flyway validate failures from a log.

Options:
  --input-file <path>  Read a boot/Flyway log from a file. Defaults to stdin.
  -h, --help           Show this help.

Exit codes:
  0  no Flyway validation failure pattern was found
  1  Flyway validation failed and needs operator action
  2  invalid usage
USAGE
}

fail_usage() {
  echo "ERROR: $*" >&2
  usage >&2
  exit 2
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --input-file)
      [[ $# -ge 2 ]] || fail_usage "--input-file requires a path"
      INPUT_FILE="$2"
      shift 2
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      fail_usage "unknown argument: $1"
      ;;
  esac
done

if [[ -n "$INPUT_FILE" ]]; then
  [[ -f "$INPUT_FILE" ]] || fail_usage "input file does not exist: $INPUT_FILE"
  LOG_CONTENT="$(cat "$INPUT_FILE")"
else
  LOG_CONTENT="$(cat)"
fi

versions_from_pattern() {
  local pattern="$1"
  printf '%s\n' "$LOG_CONTENT" \
    | sed -nE "s/.*${pattern}[^0-9]*([0-9]+).*/V\\1/p" \
    | sort -uV \
    | awk 'BEGIN { first = 1 } { if (!first) { printf ", " } printf "%s", $0; first = 0 } END { if (!first) { printf "\n" } }'
}

checksum_versions="$(versions_from_pattern 'Migration checksum mismatch for migration version')"
missing_versions="$(versions_from_pattern 'Detected applied migration not resolved locally')"
duplicate_versions="$(versions_from_pattern 'Found more than one migration with version')"

if [[ -n "$checksum_versions" ]]; then
  echo "FAIL: Flyway validate found schema-history checksum drift."
  echo "Versions: $checksum_versions"
  echo "Classification: the database has applied migrations whose recorded checksums differ from the current migration files."
  echo "Action: do not run flyway repair automatically. Use a disposable database for boot wiring checks, or run an operator-led repair only after comparing the applied SQL with the current migration files and taking a backup."
  exit 1
fi

if [[ -n "$missing_versions" ]]; then
  echo "FAIL: Flyway validate found applied migrations that are missing from the runtime artifact."
  echo "Versions: $missing_versions"
  echo "Classification: this environment applied migrations that the current canvas-boot package no longer resolves."
  echo "Action: stop the rollout and reconcile migration history with the release owner; prefer a forward repair migration over editing applied files."
  exit 1
fi

if [[ -n "$duplicate_versions" ]]; then
  echo "FAIL: Flyway validate found duplicate migration versions in the runtime artifact."
  echo "Versions: $duplicate_versions"
  echo "Classification: the migration directory is invalid and must be fixed before startup."
  echo "Action: renumber the unapplied migration in source control; do not repair database history for duplicate files."
  exit 1
fi

if printf '%s\n' "$LOG_CONTENT" | grep -qiE 'Validate failed|FlywayValidateException|Migrations have failed validation'; then
  echo "FAIL: Flyway validation failed, but this classifier did not match a known safe category."
  echo "Action: inspect the full log manually before changing database history."
  exit 1
fi

echo "PASS: no Flyway validation failure pattern found"
