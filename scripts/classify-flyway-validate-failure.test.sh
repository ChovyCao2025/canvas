#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SCRIPT="$ROOT_DIR/scripts/classify-flyway-validate-failure.sh"
TMP_DIR="$(mktemp -d)"
trap 'rm -rf "$TMP_DIR"' EXIT

assert_contains() {
  local file="$1"
  local expected="$2"
  if ! grep -Fq "$expected" "$file"; then
    echo "Expected $file to contain: $expected" >&2
    cat "$file" >&2
    exit 1
  fi
}

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

clean="$TMP_DIR/clean.log"
checksum="$TMP_DIR/checksum.log"
missing="$TMP_DIR/missing.log"
duplicate="$TMP_DIR/duplicate.log"

cat > "$clean" <<'EOF'
Successfully validated 243 migrations (execution time 00:00.231s)
Creating Schema History table `canvas`.`flyway_schema_history` ...
Started CanvasBootApplication in 12.345 seconds
EOF

cat > "$checksum" <<'EOF'
org.flywaydb.core.api.exception.FlywayValidateException: Validate failed: Migrations have failed validation
Migration checksum mismatch for migration version 2
-> Applied to database : 123
-> Resolved locally    : 456
Migration checksum mismatch for migration version 89
-> Applied to database : 789
-> Resolved locally    : 987
EOF

cat > "$missing" <<'EOF'
Validate failed: Migrations have failed validation
Detected applied migration not resolved locally: 91.
EOF

cat > "$duplicate" <<'EOF'
org.flywaydb.core.api.FlywayException: Found more than one migration with version 92
EOF

run_ok "clean" "$clean"
run_fail "checksum" "$checksum"
assert_contains "$TMP_DIR/checksum.out" "schema-history checksum drift"
assert_contains "$TMP_DIR/checksum.out" "V2, V89"
assert_contains "$TMP_DIR/checksum.out" "do not run flyway repair automatically"

run_fail "missing" "$missing"
assert_contains "$TMP_DIR/missing.out" "missing from the runtime artifact"

run_fail "duplicate" "$duplicate"
assert_contains "$TMP_DIR/duplicate.out" "duplicate migration versions"

echo "classify-flyway-validate-failure tests passed"
