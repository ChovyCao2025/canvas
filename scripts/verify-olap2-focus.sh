#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
BACKEND_DIR="$ROOT_DIR/backend"
ENGINE_DIR="$BACKEND_DIR/canvas-engine"

TEST_SELECTOR="CdpOlapAudienceSchemaTest,StableUserIndexServiceTest,VersionedAudienceBitmapStoreTest,BehaviorAudienceRuleCompilerTest,AudienceMaterializationServiceTest,AudienceQualityServiceTest"
DRY_RUN=false

FOCUSED_TEST_SOURCES=(
  "canvas-engine/src/test/java/org/chovy/canvas/testsupport/MigrationTestSupport.java"
  "canvas-engine/src/test/java/org/chovy/canvas/domain/analytics/CdpOlapAudienceSchemaTest.java"
  "canvas-engine/src/test/java/org/chovy/canvas/engine/audience/StableUserIndexServiceTest.java"
  "canvas-engine/src/test/java/org/chovy/canvas/engine/audience/VersionedAudienceBitmapStoreTest.java"
  "canvas-engine/src/test/java/org/chovy/canvas/domain/analytics/BehaviorAudienceRuleCompilerTest.java"
  "canvas-engine/src/test/java/org/chovy/canvas/domain/analytics/AudienceMaterializationServiceTest.java"
  "canvas-engine/src/test/java/org/chovy/canvas/domain/analytics/AudienceQualityServiceTest.java"
)

fail() {
  echo "ERROR: $*" >&2
  exit 1
}

usage() {
  cat <<'EOF'
Usage: scripts/verify-olap2-focus.sh [--dry-run]

Runs the focused olap2/P2-021 backend verification slice with Java 21:
  - CDP OLAP audience schema and Doris DDL checks
  - stable user index allocation
  - versioned audience bitmap storage
  - bounded behavior audience rule compilation
  - audience materialization orchestration
  - audience quality verdicts
EOF
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --dry-run)
      DRY_RUN=true
      shift
      ;;
    --help|-h)
      usage
      exit 0
      ;;
    *)
      fail "unknown argument: $1"
      ;;
  esac
done

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

java_spec=""
if [[ -n "${JAVA_HOME:-}" && -x "$JAVA_HOME/bin/java" ]]; then
  java_spec="$(java_spec_for "$JAVA_HOME/bin/java")"
fi

if ! is_java_21_or_newer "$java_spec"; then
  if command -v /usr/libexec/java_home >/dev/null 2>&1; then
    candidate_java_home="$(/usr/libexec/java_home -v 21 2>/dev/null || true)"
    if [[ -n "$candidate_java_home" && -x "$candidate_java_home/bin/java" ]]; then
      candidate_java_spec="$(java_spec_for "$candidate_java_home/bin/java")"
      if is_java_21_or_newer "$candidate_java_spec"; then
        JAVA_HOME="$candidate_java_home"
        java_spec="$candidate_java_spec"
        export JAVA_HOME
      fi
    fi
  fi
fi

if ! is_java_21_or_newer "$java_spec"; then
  if command -v java >/dev/null 2>&1; then
    java_spec="$(java_spec_for "$(command -v java)")"
  else
    fail "Java 21+ is required and no java command was found"
  fi
fi

case "$java_spec" in
  21|2[2-9]|[3-9][0-9])
    ;;
  *)
    fail "Java 21+ is required for olap2 verification; current java.specification.version=${java_spec:-unknown}"
    ;;
esac

if [[ -n "${JAVA_HOME:-}" ]]; then
  export PATH="$JAVA_HOME/bin:$PATH"
fi

[[ -d "$BACKEND_DIR" ]] || fail "backend directory is missing: $BACKEND_DIR"

COMPILE_CMD=(mvn -pl canvas-engine -DskipTests compile)
CLASSPATH_CMD=(mvn -pl canvas-engine -DincludeScope=test "-Dmdep.outputFile=$ENGINE_DIR/target/test-classpath.txt" dependency:build-classpath)
RUN_CMD=(mvn -pl canvas-engine "-Dtest=$TEST_SELECTOR" -DfailIfNoTests=false surefire:test)

if [[ "$DRY_RUN" == "true" ]]; then
  echo "olap2 focus verification dry-run passed"
  echo "java_specification_version: $java_spec"
  if [[ -n "${JAVA_HOME:-}" ]]; then
    echo "JAVA_HOME: $JAVA_HOME"
  fi
  printf 'compile: cd backend &&'
  printf ' %q' "${COMPILE_CMD[@]}"
  printf '\n'
  printf 'classpath: cd backend &&'
  printf ' %q' "${CLASSPATH_CMD[@]}"
  printf '\n'
  printf 'test-compile: cd backend && javac --release 21 -cp <test-classpath> -d canvas-engine/target/test-classes'
  printf ' %q' "${FOCUSED_TEST_SOURCES[@]}"
  printf '\n'
  printf 'run: cd backend &&'
  printf ' %q' "${RUN_CMD[@]}"
  printf '\n'
  exit 0
fi

echo "Running olap2 focus verification with java.specification.version=$java_spec"
(
  cd "$BACKEND_DIR"
  "${COMPILE_CMD[@]}"
  "${CLASSPATH_CMD[@]}"

  mkdir -p "$ENGINE_DIR/target/test-classes"
  TEST_CP="$(cat "$ENGINE_DIR/target/test-classpath.txt"):$ENGINE_DIR/target/classes:$ENGINE_DIR/target/test-classes"
  "$JAVA_HOME/bin/javac" --release 21 -cp "$TEST_CP" -d "$ENGINE_DIR/target/test-classes" "${FOCUSED_TEST_SOURCES[@]}"

  "${RUN_CMD[@]}"
)
