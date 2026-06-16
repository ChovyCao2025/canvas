#!/usr/bin/env bash
# Verifies the known Flyway history repair window without mutating the database.
#
# The checker compares applied V91/V92/V93/V354/V356 rows with the expected
# post-merge migration sequence and returns repair guidance through failures.
set -euo pipefail

INPUT_FILE=""
DB_HOST="${DB_HOST:-127.0.0.1}"
DB_PORT="${DB_PORT:-3306}"
DB_NAME="${DB_NAME:-${MYSQL_DATABASE:-canvas}}"
DB_USER="${DB_USER:-${MYSQL_USER:-root}}"
DB_PASSWORD="${DB_PASSWORD:-${MYSQL_PASSWORD:-}}"

usage() {
  cat <<'USAGE'
Verify Flyway history for the V91/V92/V354/V356 migration merge repair.

By default this connects to MySQL and reads flyway_schema_history.

Environment:
  DB_HOST       MySQL host, default 127.0.0.1
  DB_PORT       MySQL port, default 3306
  DB_NAME       Database name, default canvas
  DB_USER       MySQL user, default root
  DB_PASSWORD   MySQL password, optional

Options:
  --input-file <path>  Read tab-separated rows instead of querying MySQL.
                       Row format: version<TAB>description<TAB>success
  -h, --help           Show this help.

Exit codes:
  0  history is deployable for the resolved migration sequence
  1  conflict or failed/unknown history found
  2  invalid verifier usage or missing dependency
USAGE
}

# Print usage together with an invalid invocation error.
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

normalize_description() {
  echo "$1" \
    | tr '[:upper:]' '[:lower:]' \
    | tr '_' ' ' \
    | sed -E 's/[[:space:]]+/ /g; s/^ //; s/ $//'
}

# Read applied Flyway history from either a fixture file or the configured MySQL schema.
history_rows() {
  if [[ -n "$INPUT_FILE" ]]; then
    [[ -f "$INPUT_FILE" ]] || fail_usage "input file does not exist: $INPUT_FILE"
    cat "$INPUT_FILE"
    return
  fi

  if ! command -v mysql >/dev/null 2>&1; then
    echo "ERROR: mysql client is required unless --input-file is used" >&2
    exit 2
  fi

  local query
  query="SELECT version, description, success FROM flyway_schema_history WHERE version IN ('91','92','93','354','356') ORDER BY installed_rank;"
  MYSQL_PWD="$DB_PASSWORD" mysql \
    --batch \
    --raw \
    --skip-column-names \
    --host="$DB_HOST" \
    --port="$DB_PORT" \
    --user="$DB_USER" \
    "$DB_NAME" \
    --execute "$query"
}

# Validate applied rows against the repaired migration sequence and flag old conflicting entries.
verify_rows() {
  local rows="$1"
  local failures=()
  local seen_count=0
  local seen_91=""
  local seen_92=""
  local seen_93=""
  local seen_354=""
  local seen_356=""

  while IFS=$'\t' read -r version description success _rest; do
    [[ -n "${version:-}" ]] || continue
    seen_count=$((seen_count + 1))
    local normalized
    normalized="$(normalize_description "${description:-}")"
    local succeeded
    succeeded="$(echo "${success:-}" | tr '[:upper:]' '[:lower:]')"
    if [[ "$succeeded" != "1" && "$succeeded" != "true" ]]; then
      failures+=("V${version} ${description:-<missing description>} is not successful")
    fi
    case "$version" in
      91)
        seen_91="$normalized"
        if [[ "$normalized" == "sanitize demo datasource credentials" ]]; then
          failures+=("V91 contains old conflicting sanitize migration")
        elif [[ "$normalized" != "data security and tenant isolation" ]]; then
          failures+=("V91 has unexpected description: ${description:-<missing>}")
        fi
        ;;
      92)
        seen_92="$normalized"
        if [[ "$normalized" == "enforce core tenant not null" ]]; then
          failures+=("V92 contains old conflicting tenant-not-null migration")
        elif [[ "$normalized" != "execution context cold backup" ]]; then
          failures+=("V92 has unexpected description: ${description:-<missing>}")
        fi
        ;;
      93)
        seen_93="$normalized"
        if [[ "$normalized" != "tenant scope datasources and execution requests" ]]; then
          failures+=("V93 has unexpected description: ${description:-<missing>}")
        fi
        ;;
      354)
        seen_354="$normalized"
        if [[ "$normalized" != "sanitize demo datasource credentials" ]]; then
          failures+=("V354 has unexpected description: ${description:-<missing>}")
        fi
        ;;
      356)
        seen_356="$normalized"
        if [[ "$normalized" != "enforce core tenant not null" ]]; then
          failures+=("V356 has unexpected description: ${description:-<missing>}")
        fi
        ;;
      *)
        failures+=("unexpected version returned by history query: V${version}")
        ;;
    esac
  done <<< "$rows"

  if [[ "$seen_count" -eq 0 ]]; then
    echo "PASS: no V91/V92/V93/V354/V356 Flyway history rows exist yet"
    return 0
  fi

  if [[ -n "$seen_354" && -z "$seen_91" ]]; then
    failures+=("V354 is present but V91 is absent; inspect migration history ordering")
  fi
  if [[ -n "$seen_356" && -z "$seen_92" ]]; then
    failures+=("V356 is present but V92 is absent; inspect migration history ordering")
  fi
  if [[ -n "$seen_93" && ( -z "$seen_91" || -z "$seen_92" ) ]]; then
    failures+=("V93 is present without both resolved V91 and V92 rows")
  fi

  if [[ "${#failures[@]}" -gt 0 ]]; then
    echo "FAIL: Flyway history is not safe for automatic deployment" >&2
    printf ' - %s\n' "${failures[@]}" >&2
    return 1
  fi

  echo "PASS: Flyway history matches the resolved V91/V92 repair sequence"
}

rows="$(history_rows)"
verify_rows "$rows"
