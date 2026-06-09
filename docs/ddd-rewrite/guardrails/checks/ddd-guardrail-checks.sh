#!/usr/bin/env bash
set -euo pipefail

ROOT="${1:-.}"
cd "$ROOT"

TARGETS=()
for path in backend/canvas-context-* backend/canvas-platform backend/canvas-web backend/canvas-common backend/canvas-boot; do
  if [ -d "$path/src/main/java" ]; then
    TARGETS+=("$path/src/main/java")
  fi
done

if [ "${#TARGETS[@]}" -eq 0 ]; then
  echo "No DDD rewrite module source directories found; nothing to scan yet."
  exit 0
fi

failures=0

check_no_matches() {
  local name="$1"
  local pattern="$2"
  shift 2
  echo "== $name"
  if rg -n "$pattern" "$@"; then
    echo "FAIL: $name"
    failures=$((failures + 1))
  else
    echo "PASS: $name"
  fi
  echo
}

check_advisory() {
  local name="$1"
  local pattern="$2"
  shift 2
  echo "== $name"
  if rg -n "$pattern" "$@"; then
    echo "ADVISORY: inspect matches manually"
  else
    echo "PASS: no advisory matches"
  fi
  echo
}

check_package_prefix() {
  local module="$1"
  local prefix="$2"

  if [ ! -d "$module/src/main/java" ]; then
    return
  fi

  echo "== $(basename "$module") declares only $prefix packages"
  local prefix_regex="${prefix//./\\.}"
  local bad_matches
  bad_matches="$(rg -n "^package " "$module/src/main/java" -g "*.java" | grep -Ev "package ${prefix_regex}(;|\\.)" || true)"
  if [ -n "$bad_matches" ]; then
    printf '%s\n' "$bad_matches"
    echo "FAIL: $(basename "$module") declares only $prefix packages"
    failures=$((failures + 1))
  else
    echo "PASS: $(basename "$module") package declarations match"
  fi
  echo
}

check_package_prefix backend/canvas-common org.chovy.canvas.common
check_package_prefix backend/canvas-web org.chovy.canvas.web
check_package_prefix backend/canvas-boot org.chovy.canvas.boot
check_package_prefix backend/canvas-platform org.chovy.canvas.platform
check_package_prefix backend/canvas-context-canvas org.chovy.canvas.canvas
check_package_prefix backend/canvas-context-execution org.chovy.canvas.execution
check_package_prefix backend/canvas-context-marketing org.chovy.canvas.marketing
check_package_prefix backend/canvas-context-cdp org.chovy.canvas.cdp
check_package_prefix backend/canvas-context-bi org.chovy.canvas.bi
check_package_prefix backend/canvas-context-risk org.chovy.canvas.risk
check_package_prefix backend/canvas-context-conversation org.chovy.canvas.conversation

check_no_matches \
  "domain has no infrastructure imports" \
  "^import (com\\.baomidou|org\\.springframework\\.web|org\\.springframework\\.data\\.redis|org\\.apache\\.rocketmq|org\\.springframework\\.web\\.reactive\\.function\\.client|com\\.fasterxml\\.jackson\\.databind\\.ObjectMapper)" \
  "${TARGETS[@]}" -g "**/domain/**/*.java"

check_no_matches \
  "non-persistence code does not import DO classes" \
  "^import .*DO;" \
  "${TARGETS[@]}" -g "*.java" -g "!**/adapter/persistence/**/*.java"

check_no_matches \
  "non-persistence code does not import Mapper classes" \
  "^import .*Mapper;" \
  "${TARGETS[@]}" -g "*.java" -g "!**/adapter/persistence/**/*.java"

if [ -d "backend/canvas-web/src/main/java" ]; then
  check_no_matches \
    "web does not import persistence adapters" \
    "^import .*\\.adapter\\.persistence\\.|^import .*Mapper;|^import .*DO;" \
    backend/canvas-web/src/main/java -g "*.java"
fi

check_no_matches \
  "modules do not import adapter packages directly outside adapters" \
  "^import org\\.chovy\\.canvas\\..*\\.adapter\\.(persistence|messaging|external)\\." \
  "${TARGETS[@]}" -g "*.java" -g "!**/adapter/**/*.java"

echo "== contexts do not import other context adapter packages"
cross_context_failures=0
for module in backend/canvas-context-* backend/canvas-platform; do
  if [ ! -d "$module/src/main/java" ]; then
    continue
  fi
  base_name="$(basename "$module")"
  if [[ "$base_name" == canvas-context-* ]]; then
    context="${base_name#canvas-context-}"
  else
    context="platform"
  fi
  matches="$(rg -n "^import org\\.chovy\\.canvas\\..*\\.(adapter\\.(persistence|messaging|external)|config)\\." "$module/src/main/java" -g "*.java" || true)"
  if [ -n "$matches" ]; then
    bad_matches="$(printf '%s\n' "$matches" | grep -Ev "import org\\.chovy\\.canvas\\.${context}\\.(adapter\\.|config\\.)" || true)"
    if [ -n "$bad_matches" ]; then
      printf '%s\n' "$bad_matches"
      cross_context_failures=$((cross_context_failures + 1))
    fi
  fi
done
if [ "$cross_context_failures" -gt 0 ]; then
  echo "FAIL: contexts do not import other context adapter packages"
  failures=$((failures + cross_context_failures))
else
  echo "PASS: contexts do not import other context adapter packages"
fi
echo

check_no_matches \
  "new modules do not import old canvas-engine internals" \
  "^import org\\.chovy\\.canvas\\.(domain|engine|dal|infrastructure)\\." \
  "${TARGETS[@]}" -g "*.java"

POM_TARGETS=()
for pom in backend/canvas-context-*/pom.xml backend/canvas-platform/pom.xml backend/canvas-web/pom.xml backend/canvas-common/pom.xml backend/canvas-boot/pom.xml; do
  if [ -f "$pom" ]; then
    POM_TARGETS+=("$pom")
  fi
done
if [ "${#POM_TARGETS[@]}" -gt 0 ]; then
  check_no_matches \
    "new module poms do not depend on canvas-engine" \
    "<artifactId>canvas-engine</artifactId>" \
    "${POM_TARGETS[@]}"
fi

if [ -d "backend/canvas-common/src/main/java" ]; then
  check_no_matches \
    "common has no business enum names" \
    "(enum|class|interface) .*(CanvasStatus|NodeType|CampaignStatus|RiskStatus|ApprovalStatus|BiResourceType|Cdp.*Status)" \
    backend/canvas-common/src/main/java -g "*.java"
fi

check_no_matches \
  "no premature generic base layer" \
  "(class|interface) (BaseCrudService|GenericRepository|AbstractDomainService|BaseMapperHelper|GenericCrudService)" \
  "${TARGETS[@]}" -g "*.java"

check_advisory \
  "temporary bridge names require manual removal gate review" \
  "(Legacy|Compatibility|Bridge)" \
  "${TARGETS[@]}" -g "*.java"

if [ "$failures" -gt 0 ]; then
  echo "Guardrail checks failed: $failures"
  exit 1
fi

echo "Guardrail checks passed."
