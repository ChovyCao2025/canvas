#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"

DRY_RUN=false
PUSH=false
SKIP_PACKAGE="${CANVAS_RELEASE_SKIP_PACKAGE:-false}"
IMAGE_NAME="${CANVAS_IMAGE_NAME:-canvas-engine}"
IMAGE_TAG="${CANVAS_IMAGE_TAG:-local}"
DOCKERFILE="${CANVAS_DOCKERFILE:-backend/canvas-engine/Dockerfile.perf}"
CONTEXT="${CANVAS_DOCKER_CONTEXT:-.}"

fail() {
  echo "ERROR: $*" >&2
  exit 1
}

usage() {
  cat <<'EOF'
Usage: scripts/release/build-image.sh [--dry-run] [--push] [--skip-package] [--tag TAG] [--image NAME]

Builds the canvas-engine release image from a clean checkout. The default
Dockerfile expects backend/canvas-engine/target/canvas-engine-*.jar, so the
script packages the backend before docker build unless --skip-package is used.
EOF
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --dry-run)
      DRY_RUN=true
      shift
      ;;
    --push)
      PUSH=true
      shift
      ;;
    --skip-package)
      SKIP_PACKAGE=true
      shift
      ;;
    --tag)
      [[ $# -ge 2 ]] || fail "--tag requires a value"
      IMAGE_TAG="$2"
      shift 2
      ;;
    --image)
      [[ $# -ge 2 ]] || fail "--image requires a value"
      IMAGE_NAME="$2"
      shift 2
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

[[ -f "$ROOT_DIR/$DOCKERFILE" ]] || fail "Dockerfile is missing: $ROOT_DIR/$DOCKERFILE"
[[ -d "$ROOT_DIR/$CONTEXT" ]] || fail "Docker context is missing: $ROOT_DIR/$CONTEXT"
[[ "$IMAGE_TAG" != "latest" ]] || fail "release image tag must not be latest"

FULL_IMAGE="$IMAGE_NAME:$IMAGE_TAG"
PACKAGE_CMD="cd backend && mvn -pl canvas-engine -am package -DskipTests"
BUILD_CMD="docker build -f $DOCKERFILE -t $FULL_IMAGE $CONTEXT"
PUSH_CMD="docker push $FULL_IMAGE"

if [[ "$DRY_RUN" == "true" ]]; then
  echo "image build dry-run passed"
  echo "package: $PACKAGE_CMD"
  echo "build: $BUILD_CMD"
  if [[ "$PUSH" == "true" ]]; then
    echo "push: $PUSH_CMD"
  fi
  exit 0
fi

command -v docker >/dev/null 2>&1 || fail "docker is required"
command -v java >/dev/null 2>&1 || fail "Java 21+ is required; set JAVA_HOME"

java_spec="$(java -XshowSettings:properties -version 2>&1 | awk -F'= ' '/java.specification.version/ {print $2; exit}')"
case "$java_spec" in
  21|2[2-9]|[3-9][0-9])
    ;;
  *)
    fail "Java 21+ is required for Maven packaging; current java.specification.version=${java_spec:-unknown}"
    ;;
esac

if [[ "$SKIP_PACKAGE" != "true" ]]; then
  (cd "$ROOT_DIR/backend" && mvn -pl canvas-engine -am package -DskipTests)
fi

shopt -s nullglob
jars=("$ROOT_DIR"/backend/canvas-engine/target/canvas-engine-*.jar)
shopt -u nullglob
[[ ${#jars[@]} -gt 0 ]] || fail "missing packaged canvas-engine jar; run backend Maven package first"

(cd "$ROOT_DIR" && docker build -f "$DOCKERFILE" -t "$FULL_IMAGE" "$CONTEXT")

if [[ "$PUSH" == "true" ]]; then
  docker push "$FULL_IMAGE"
fi

echo "image build passed: $FULL_IMAGE"
