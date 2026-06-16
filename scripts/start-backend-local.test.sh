#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SCRIPT="$ROOT_DIR/scripts/start-backend-local.sh"
TMP_DIR="$(mktemp -d)"
trap 'rm -rf "$TMP_DIR"' EXIT

assert_contains() {
  local file="$1"
  local expected="$2"
  if ! grep -Fq -- "$expected" "$file"; then
    echo "Expected $file to contain: $expected" >&2
    cat "$file" >&2
    exit 1
  fi
}

run_ok() {
  local name="$1"
  shift
  if ! "$SCRIPT" "$@" >"$TMP_DIR/${name}.out" 2>"$TMP_DIR/${name}.err"; then
    echo "Expected success for $name" >&2
    cat "$TMP_DIR/${name}.out" "$TMP_DIR/${name}.err" >&2
    exit 1
  fi
}

run_ok "default" --dry-run
assert_contains "$TMP_DIR/default.out" "Target database: canvas_db"
assert_contains "$TMP_DIR/default.out" "mvn -pl canvas-boot -am -DskipTests install"
assert_contains "$TMP_DIR/default.out" "mvn -f canvas-boot/pom.xml -Dmaven.test.skip=true spring-boot:run"

run_ok "fresh-default-name" --dry-run --fresh-db
assert_contains "$TMP_DIR/fresh-default-name.out" "Target database: canvas_boot_local"
assert_contains "$TMP_DIR/fresh-default-name.out" "DROP DATABASE IF EXISTS \`canvas_boot_local\`"
assert_contains "$TMP_DIR/fresh-default-name.out" "SPRING_DATASOURCE_URL=jdbc:mysql://localhost:3306/canvas_boot_local?"

run_ok "fresh-custom-name-port" --dry-run --fresh-db --db-name canvas_boot_tmp --server-port 18080
assert_contains "$TMP_DIR/fresh-custom-name-port.out" "Target database: canvas_boot_tmp"
assert_contains "$TMP_DIR/fresh-custom-name-port.out" "--server.port=18080"
assert_contains "$TMP_DIR/fresh-custom-name-port.out" "DROP DATABASE IF EXISTS \`canvas_boot_tmp\`"

echo "start-backend-local tests passed"
