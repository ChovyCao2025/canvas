#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
BACKEND_DIR="$ROOT_DIR/backend"
DRY_RUN=false

fail() {
  echo "ERROR: $*" >&2
  exit 1
}

usage() {
  cat <<'USAGE'
Usage: scripts/compile-canvas-engine.sh [--dry-run]

Compiles the canvas-engine Maven module and required backend modules with Java 21+.

Options:
  --dry-run   Print the resolved compile command without running it.
  -h, --help  Show this help.
USAGE
}

java_spec_for() {
  "$1" -XshowSettings:properties -version 2>&1 |
    awk -F'= ' '/java.specification.version/ {print $2; exit}'
}

is_java_21_or_newer() {
  case "$1" in
    21|2[2-9]|[3-9][0-9])
      return 0
      ;;
    *)
      return 1
      ;;
  esac
}

try_java_home() {
  local candidate="$1"
  if [[ -n "$candidate" && -x "$candidate/bin/java" ]]; then
    local spec
    spec="$(java_spec_for "$candidate/bin/java")"
    if is_java_21_or_newer "$spec"; then
      JAVA_HOME="$candidate"
      export JAVA_HOME
      PATH="$JAVA_HOME/bin:$PATH"
      export PATH
      return 0
    fi
  fi
  return 1
}

resolve_java_21() {
  if [[ -n "${CANVAS_JAVA_HOME:-}" ]]; then
    try_java_home "$CANVAS_JAVA_HOME" || fail "CANVAS_JAVA_HOME must point to a Java 21+ JDK: $CANVAS_JAVA_HOME"
    return 0
  fi

  if [[ -n "${JAVA_HOME:-}" ]]; then
    try_java_home "$JAVA_HOME" && return 0
  fi

  if command -v /usr/libexec/java_home >/dev/null 2>&1; then
    try_java_home "$(/usr/libexec/java_home -v 21 2>/dev/null || true)" && return 0
  fi

  if command -v java >/dev/null 2>&1; then
    local java_bin
    java_bin="$(command -v java)"
    if is_java_21_or_newer "$(java_spec_for "$java_bin")"; then
      return 0
    fi
  fi

  fail "Java 21+ is required; set JAVA_HOME or CANVAS_JAVA_HOME to a Java 21+ JDK"
}

quote_cmd() {
  printf ' %q' "$@"
  printf '\n'
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --dry-run)
      DRY_RUN=true
      shift
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      fail "Unknown argument: $1"
      ;;
  esac
done

[[ -d "$BACKEND_DIR" ]] || fail "backend directory is missing: $BACKEND_DIR"
command -v mvn >/dev/null 2>&1 || fail "mvn is required"
resolve_java_21

COMPILE_CMD=(
  mvn
  -q
  -f "$BACKEND_DIR/pom.xml"
  -pl canvas-engine
  -am
  -DskipTests
  -Dmaven.compiler.useIncrementalCompilation=false
  -Dmaven.compiler.forceJavacCompilerUse=true
  compile
)

echo "JAVA_HOME: ${JAVA_HOME:-"(using java from PATH)"}"
echo "java.specification.version: $(java_spec_for "$(command -v java)")"
echo "Compile command:$(quote_cmd "${COMPILE_CMD[@]}")"

if [[ "$DRY_RUN" == "true" ]]; then
  exit 0
fi

"${COMPILE_CMD[@]}"
