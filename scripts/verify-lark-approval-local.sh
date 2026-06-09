#!/usr/bin/env bash
# Runs a local Lark approval integration smoke check against a developer backend.
#
# The script validates request/response behavior without requiring the live Lark
# environment, so it is suitable for pre-merge verification and local debugging.
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
BACKEND_DIR="$ROOT_DIR/backend"
MODULE_DIR="$BACKEND_DIR/canvas-engine"
DEFAULT_JAVA_HOME="/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home"
JAVA_HOME="${CANVAS_JAVA_HOME:-$DEFAULT_JAVA_HOME}"
ALLOW_TARGETED_TEST_COMPILE="${ALLOW_TARGETED_TEST_COMPILE:-false}"
TESTS="${TESTS:-ApprovalLarkUserIdentityResolverTest,LarkApprovalProviderTest,HttpLarkApprovalClientTest,ApprovalWorkflowServiceTest,ApprovalControllerTest,LarkApprovalSyncSchedulerTest,CanvasPublishApprovalServiceTest}"

export JAVA_HOME
export PATH="$JAVA_HOME/bin:$PATH"

usage() {
  cat <<'USAGE'
Verify local Canvas Lark approval regression tests.

Default mode runs Maven's normal test lifecycle for the focused approval tests.
If unrelated dirty-tree test sources fail testCompile, rerun with:

  ALLOW_TARGETED_TEST_COMPILE=true scripts/verify-lark-approval-local.sh

That fallback compiles only the approval test sources named by TESTS and then
runs surefire:test for the same list.

The script defaults to the repository-required Java 21 runtime. Override with
CANVAS_JAVA_HOME when needed.
USAGE
}

if [[ "${1:-}" == "-h" || "${1:-}" == "--help" ]]; then
  usage
  exit 0
fi

if [[ ! -x "$JAVA_HOME/bin/java" || ! -x "$JAVA_HOME/bin/javac" ]]; then
  echo "JAVA_HOME must point to a JDK with java and javac: $JAVA_HOME" >&2
  exit 2
fi

echo "Running focused approval tests with Maven test lifecycle"
if mvn -f "$MODULE_DIR/pom.xml" test -Dtest="$TESTS"; then
  exit 0
fi

if [[ "$ALLOW_TARGETED_TEST_COMPILE" != "true" ]]; then
  cat >&2 <<'MSG'
Focused approval tests did not complete through Maven test lifecycle.
If the failure is caused by unrelated testCompile errors outside approval,
rerun with ALLOW_TARGETED_TEST_COMPILE=true.
MSG
  exit 1
fi

echo "Falling back to targeted approval test compilation"
classpath_file="$MODULE_DIR/target/approval-test-classpath.txt"
mvn -q -f "$MODULE_DIR/pom.xml" dependency:build-classpath \
  -Dmdep.outputFile="$classpath_file" \
  -Dmdep.includeScope=test

main_sources=()
while IFS= read -r source_path; do
  main_sources+=("$source_path")
done < <(find "$MODULE_DIR/src/main/java/org/chovy/canvas/domain/approval" -name '*.java' -print)
while IFS= read -r source_path; do
  main_sources+=("$source_path")
done < <(find "$MODULE_DIR/src/main/java/org/chovy/canvas/dal" \( -name '*Approval*.java' -o -name '*LarkUserIdentity*.java' \) -print)
main_sources+=("$MODULE_DIR/src/main/java/org/chovy/canvas/web/ApprovalController.java")

echo "Compiling targeted approval main sources"
"$JAVA_HOME/bin/javac" \
  --release 21 \
  -encoding UTF-8 \
  -sourcepath "$MODULE_DIR/src/main/java" \
  -cp "$MODULE_DIR/target/classes:$(cat "$classpath_file")" \
  -d "$MODULE_DIR/target/classes" \
  "${main_sources[@]}"

IFS=',' read -r -a test_names <<< "$TESTS"
test_sources=()
for test_name in "${test_names[@]}"; do
  while IFS= read -r source_path; do
    test_sources+=("$source_path")
  done < <(find "$MODULE_DIR/src/test/java" -name "${test_name}.java" -print)
done

if [[ "${#test_sources[@]}" -eq 0 ]]; then
  echo "No approval test sources found for TESTS=$TESTS" >&2
  exit 1
fi

"$JAVA_HOME/bin/javac" \
  --release 21 \
  -encoding UTF-8 \
  -sourcepath "$MODULE_DIR/src/test/java:$MODULE_DIR/src/main/java" \
  -cp "$MODULE_DIR/target/test-classes:$MODULE_DIR/target/classes:$(cat "$classpath_file")" \
  -d "$MODULE_DIR/target/test-classes" \
  "${test_sources[@]}"

mvn -f "$MODULE_DIR/pom.xml" surefire:test -Dtest="$TESTS"
