#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SCRIPT="$ROOT_DIR/scripts/verify-flyway-history.sh"
TMP_DIR="$(mktemp -d)"
trap 'rm -rf "$TMP_DIR"' EXIT

run_ok() {
  local name="$1"
  local file="$2"
  if ! "$SCRIPT" --input-file "$file" >"$TMP_DIR/${name}.out" 2>"$TMP_DIR/${name}.err"; then
    echo "Expected success for $name" >&2
    cat "$TMP_DIR/${name}.out" "$TMP_DIR/${name}.err" >&2
    exit 1
  fi
}

run_fail() {
  local name="$1"
  local file="$2"
  if "$SCRIPT" --input-file "$file" >"$TMP_DIR/${name}.out" 2>"$TMP_DIR/${name}.err"; then
    echo "Expected failure for $name" >&2
    cat "$TMP_DIR/${name}.out" "$TMP_DIR/${name}.err" >&2
    exit 1
  fi
}

empty="$TMP_DIR/empty.tsv"
resolved="$TMP_DIR/resolved.tsv"
old_conflict="$TMP_DIR/old-conflict.tsv"
failed_row="$TMP_DIR/failed-row.tsv"

: > "$empty"
cat > "$resolved" <<'EOF'
91	data security and tenant isolation	1
92	execution context cold backup	1
93	tenant scope datasources and execution requests	1
272	github oauth integration	1
273	add filesystem read capability	1
354	sanitize demo datasource credentials	1
355	enforce core tenant not null	1
EOF
cat > "$old_conflict" <<'EOF'
91	sanitize demo datasource credentials	1
92	enforce core tenant not null	1
EOF
cat > "$failed_row" <<'EOF'
91	data security and tenant isolation	1
92	execution context cold backup	0
EOF

run_ok "empty" "$empty"
run_ok "resolved" "$resolved"
run_fail "old-conflict" "$old_conflict"
run_fail "failed-row" "$failed_row"

echo "verify-flyway-history tests passed"
