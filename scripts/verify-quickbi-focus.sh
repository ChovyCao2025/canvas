#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
BACKEND_DIR="$ROOT_DIR/backend"
ENGINE_DIR="$BACKEND_DIR/canvas-engine"
FRONTEND_DIR="$ROOT_DIR/frontend"

CORE_BACKEND_TEST_SOURCES=(
  "src/test/java/org/chovy/canvas/domain/bi/datasource/BiDatasourceOnboardingServiceTest.java"
  "src/test/java/org/chovy/canvas/domain/bi/datasource/BiDatasourceApiPreviewRuntimeServiceTest.java"
  "src/test/java/org/chovy/canvas/web/bi/BiDatasourceControllerTest.java"
  "src/test/java/org/chovy/canvas/domain/bi/dataset/BiDatasetFromDatasourceServiceTest.java"
  "src/test/java/org/chovy/canvas/domain/bi/dataset/BiDatasetResourceServiceTest.java"
  "src/test/java/org/chovy/canvas/domain/bi/dataset/BiDatasetAccelerationServiceTest.java"
  "src/test/java/org/chovy/canvas/infrastructure/bi/JdbcBiDatasetExtractMaterializerTest.java"
  "src/test/java/org/chovy/canvas/domain/bi/query/BiQueryCompilerTest.java"
  "src/test/java/org/chovy/canvas/web/bi/BiQueryControllerTest.java"
  "src/test/java/org/chovy/canvas/domain/bi/query/BiQueryCachePolicyServiceTest.java"
  "src/test/java/org/chovy/canvas/domain/bi/query/BiQueryGovernancePolicyServiceTest.java"
  "src/test/java/org/chovy/canvas/web/bi/BiSelfServiceControllerTest.java"
  "src/test/java/org/chovy/canvas/domain/bi/permission/BiPermissionServiceTest.java"
  "src/test/java/org/chovy/canvas/domain/bi/permission/BiPermissionAdminServiceTest.java"
  "src/test/java/org/chovy/canvas/domain/bi/permission/BiPermissionRequestServiceTest.java"
  "src/test/java/org/chovy/canvas/web/bi/BiPermissionControllerTest.java"
  "src/test/java/org/chovy/canvas/domain/bi/dataset/BiQuickEngineCapacityServiceTest.java"
  "src/test/java/org/chovy/canvas/domain/bi/dataset/BiQuickEngineQueueServiceTest.java"
  "src/test/java/org/chovy/canvas/domain/bi/query/BiQuickEngineQueryAdmissionQueueWiringTest.java"
  "src/test/java/org/chovy/canvas/web/bi/BiCapacityControllerTest.java"
)

API_EXTRACT_BACKEND_TEST_SOURCES=(
  "src/test/java/org/chovy/canvas/domain/bi/dataset/BiDatasetResourceServiceTest.java"
  "src/test/java/org/chovy/canvas/domain/bi/dataset/BiDatasetAccelerationServiceTest.java"
  "src/test/java/org/chovy/canvas/infrastructure/bi/JdbcBiDatasetExtractMaterializerTest.java"
)

FRONTEND_TESTS=(
  "src/pages/bi/index.test.tsx"
  "src/pages/bi/biWorkbench.test.ts"
  "src/services/biApi.test.ts"
)

RUN_BACKEND=true
RUN_FRONTEND=true
RUN_FRONTEND_BUILD=false
RUN_API_EXTRACT_ONLY=false
RUN_BACKEND_ALL=false
DRY_RUN=false

fail() {
  echo "ERROR: $*" >&2
  exit 1
}

usage() {
  cat <<'EOF'
Usage: scripts/verify-quickbi-focus.sh [options]

Runs the focused QuickBI verification gate with Java 21 and the frontend Node/npm path.

Options:
  --backend-only       Run only backend focused verification
  --frontend-only      Run only frontend focused verification
  --skip-frontend      Run backend verification and skip frontend verification
  --with-frontend-build
                       Also run npm run build after focused frontend tests
  --api-extract-only   Run the narrow API datasource EXTRACT materialization backend gate
  --backend-all        Discover and run all backend BI test classes
  --dry-run            Print resolved commands without running them
  --help, -h           Show this help
EOF
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --backend-only)
      RUN_BACKEND=true
      RUN_FRONTEND=false
      shift
      ;;
    --frontend-only)
      RUN_BACKEND=false
      RUN_FRONTEND=true
      shift
      ;;
    --skip-frontend)
      RUN_FRONTEND=false
      shift
      ;;
    --with-frontend-build)
      RUN_FRONTEND_BUILD=true
      shift
      ;;
    --api-extract-only)
      RUN_API_EXTRACT_ONLY=true
      RUN_BACKEND=true
      RUN_FRONTEND=false
      shift
      ;;
    --backend-all)
      RUN_BACKEND_ALL=true
      shift
      ;;
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

[[ "$RUN_BACKEND" == "true" || "$RUN_FRONTEND" == "true" ]] || fail "nothing to verify"
[[ "$RUN_API_EXTRACT_ONLY" == "false" || "$RUN_BACKEND_ALL" == "false" ]] \
  || fail "--api-extract-only and --backend-all cannot be combined"

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
      return 0
    fi
  fi
  return 1
}

resolve_java_21() {
  if [[ -n "${JAVA_HOME:-}" && -x "$JAVA_HOME/bin/java" ]]; then
    is_java_21_or_newer "$(java_spec_for "$JAVA_HOME/bin/java")" && return 0
  fi
  if command -v /usr/libexec/java_home >/dev/null 2>&1; then
    try_java_home "$(/usr/libexec/java_home -v 21 2>/dev/null || true)" && return 0
  fi
  fail "Java 21+ is required for QuickBI backend verification; set JAVA_HOME to a Java 21+ JDK"
}

node_version_for() {
  "$1" -v 2>/dev/null | sed 's/^v//'
}

resolve_frontend_node() {
  if [[ -x /opt/homebrew/bin/node ]]; then
    PATH="/opt/homebrew/bin:$PATH"
    export PATH
  fi
  command -v node >/dev/null 2>&1 || fail "node is required for QuickBI frontend verification"
  command -v npm >/dev/null 2>&1 || fail "npm is required for QuickBI frontend verification"
}

quote_cmd() {
  printf ' %q' "$@"
  printf '\n'
}

backend_test_sources() {
  if [[ "$RUN_BACKEND_ALL" == "true" ]]; then
    find "$ENGINE_DIR/src/test/java/org/chovy/canvas/domain/bi" \
      "$ENGINE_DIR/src/test/java/org/chovy/canvas/infrastructure/bi" \
      "$ENGINE_DIR/src/test/java/org/chovy/canvas/web/bi" \
      -type f -name '*Test.java' 2>/dev/null |
      sed "s#^$ENGINE_DIR/##" |
      sort
    return 0
  fi

  if [[ "$RUN_API_EXTRACT_ONLY" == "true" ]]; then
    printf '%s\n' "${API_EXTRACT_BACKEND_TEST_SOURCES[@]}"
    return 0
  fi

  printf '%s\n' "${CORE_BACKEND_TEST_SOURCES[@]}"
}

backend_selector_from_sources() {
  local source class_name
  local test_classes=()
  for source in "$@"; do
    class_name="$(basename "$source" .java)"
    [[ "$class_name" == *Test ]] && test_classes+=("$class_name")
  done
  [[ "${#test_classes[@]}" -gt 0 ]] || fail "backend QuickBI verification has no test classes"
  local IFS=,
  printf '%s' "${test_classes[*]}"
}

assert_backend_sources_exist() {
  local source
  local missing=()
  for source in "$@"; do
    [[ -f "$ENGINE_DIR/$source" ]] || missing+=("$source")
  done
  if [[ "${#missing[@]}" -gt 0 ]]; then
    printf 'ERROR: backend QuickBI test source is missing: %s\n' "${missing[@]}" >&2
    exit 1
  fi
}

assert_frontend_tests_exist() {
  local source
  local missing=()
  for source in "$@"; do
    [[ -f "$FRONTEND_DIR/$source" ]] || missing+=("$source")
  done
  if [[ "${#missing[@]}" -gt 0 ]]; then
    printf 'ERROR: frontend QuickBI test is missing: %s\n' "${missing[@]}" >&2
    exit 1
  fi
}

BACKEND_TEST_SOURCES=()
BACKEND_SELECTOR=""
BACKEND_COMPILE_CMD=(mvn -q -f "$ENGINE_DIR/pom.xml" -DskipTests \
  -Dmaven.compiler.useIncrementalCompilation=false \
  -Dmaven.compiler.forceJavacCompilerUse=true \
  compile)
BACKEND_TEST_CMD=()
FRONTEND_TEST_CMD=()
FRONTEND_BUILD_CMD=()

if [[ "$RUN_BACKEND" == "true" ]]; then
  [[ -d "$ENGINE_DIR" ]] || fail "backend canvas-engine directory is missing: $ENGINE_DIR"
  resolve_java_21
  PATH="$JAVA_HOME/bin:$PATH"
  export PATH
  while IFS= read -r source; do
    BACKEND_TEST_SOURCES+=("$source")
  done < <(backend_test_sources)
  assert_backend_sources_exist "${BACKEND_TEST_SOURCES[@]}"
  BACKEND_SELECTOR="$(backend_selector_from_sources "${BACKEND_TEST_SOURCES[@]}")"
  BACKEND_TEST_CMD=(mvn -q -f "$ENGINE_DIR/pom.xml" "-Dtest=$BACKEND_SELECTOR" \
    -Dmaven.compiler.useIncrementalCompilation=false \
    -Dmaven.compiler.forceJavacCompilerUse=true \
    test)
fi

if [[ "$RUN_FRONTEND" == "true" ]]; then
  [[ -d "$FRONTEND_DIR" ]] || fail "frontend directory is missing: $FRONTEND_DIR"
  resolve_frontend_node
  assert_frontend_tests_exist "${FRONTEND_TESTS[@]}"
  FRONTEND_TEST_CMD=(npm run test -- "${FRONTEND_TESTS[@]}")
  FRONTEND_BUILD_CMD=(npm run build)
fi

if [[ "$DRY_RUN" == "true" ]]; then
  echo "quickbi focus verification dry-run passed"
  if [[ "$RUN_BACKEND" == "true" ]]; then
    echo "JAVA_HOME: $JAVA_HOME"
    echo "java_specification_version: $(java_spec_for "$JAVA_HOME/bin/java")"
    printf 'backend-compile:'
    quote_cmd "${BACKEND_COMPILE_CMD[@]}"
    printf 'backend-tests:'
    quote_cmd "${BACKEND_TEST_CMD[@]}"
  fi
  if [[ "$RUN_FRONTEND" == "true" ]]; then
    echo "NODE_BIN: $(command -v node)"
    echo "node_version: $(node_version_for "$(command -v node)")"
    echo "NPM_BIN: $(command -v npm)"
    printf 'frontend-tests: cd frontend &&'
    quote_cmd "${FRONTEND_TEST_CMD[@]}"
    if [[ "$RUN_FRONTEND_BUILD" == "true" ]]; then
      printf 'frontend-build: cd frontend &&'
      quote_cmd "${FRONTEND_BUILD_CMD[@]}"
    fi
  fi
  exit 0
fi

if [[ "$RUN_BACKEND" == "true" ]]; then
  echo "Running QuickBI backend verification with java.specification.version=$(java_spec_for "$JAVA_HOME/bin/java")"
  "${BACKEND_COMPILE_CMD[@]}"
  "${BACKEND_TEST_CMD[@]}"
fi

if [[ "$RUN_FRONTEND" == "true" ]]; then
  echo "Running QuickBI frontend verification with node $(node -v)"
  (
    cd "$FRONTEND_DIR"
    "${FRONTEND_TEST_CMD[@]}"
    if [[ "$RUN_FRONTEND_BUILD" == "true" ]]; then
      "${FRONTEND_BUILD_CMD[@]}"
    fi
  )
fi
