#!/usr/bin/env bash
set -euo pipefail

ROOT="${1:-.}"
cd "$ROOT"

failures=0

# Required row fields:
# old class:
# current path:
# target module:
# target package:
# target role:
# owning worker:
# required tests:
# coordinator decision:

fail() {
  echo "FAIL: $1"
  failures=$((failures + 1))
}

require_file() {
  local file="$1"
  if [ ! -s "$file" ]; then
    fail "$file is required and must not be empty"
  fi
}

require_inventory_rows() {
  local file="$1"
  local row_label="$2"
  local expected_count="$3"
  local require_tests="$4"

  require_file "$file"
  if [ ! -f "$file" ]; then
    return
  fi

  local row_count
  row_count="$(rg -n "^(old class|old file|source path):" "$file" | wc -l | tr -d ' ')"
  if [ "$row_count" -eq 0 ]; then
    fail "$file must contain exact source rows for $row_label"
  fi
  if [ "$expected_count" -gt 0 ] && [ "$row_count" -lt "$expected_count" ]; then
    fail "$file has $row_count rows for $row_label but expected at least $expected_count source files"
  fi

  awk -v file="$file" -v label="$row_label" -v require_tests="$require_tests" '
    function reset_block() {
      source = current = module = target = owner = tests = 0
    }
    function validate_block() {
      if (!in_block) {
        return
      }
      if (!current) {
        printf("FAIL: %s row %d for %s is missing current path:\n", file, row_start, label)
        failures++
      }
      if (!module) {
        printf("FAIL: %s row %d for %s is missing target module:\n", file, row_start, label)
        failures++
      }
      if (!target) {
        printf("FAIL: %s row %d for %s is missing target package: or target role:\n", file, row_start, label)
        failures++
      }
      if (!owner) {
        printf("FAIL: %s row %d for %s is missing owning worker: or coordinator decision:\n", file, row_start, label)
        failures++
      }
      if (require_tests == "yes" && !tests) {
        printf("FAIL: %s row %d for %s is missing required tests:\n", file, row_start, label)
        failures++
      }
    }
    /^(old class|old file|source path):/ {
      validate_block()
      in_block = 1
      row_start = NR
      reset_block()
      source = 1
      next
    }
    in_block && /^current path:/ { current = 1 }
    in_block && /^target module:/ { module = 1 }
    in_block && /^(target package|target role):/ { target = 1 }
    in_block && /^(owning worker|coordinator decision):/ { owner = 1 }
    in_block && /^required tests:/ { tests = 1 }
    END {
      validate_block()
      if (failures > 0) {
        exit 1
      }
    }
  ' "$file" || failures=$((failures + 1))

  if rg -n "unassigned|unknown|ambiguous without coordinator decision|待定|待补|TBD|TODO" "$file"; then
    fail "$file contains unresolved ownership for $row_label"
  fi
}

count_files() {
  local count=0
  local path
  for path in "$@"; do
    if [ -d "$path" ]; then
      count=$((count + $(find "$path" -name '*.java' | wc -l | tr -d ' ')))
    fi
  done
  echo "$count"
}

count_matching_files() {
  local path="$1"
  local pattern="$2"
  if [ ! -d "$path" ]; then
    echo 0
    return
  fi
  find "$path" -name "$pattern" | wc -l | tr -d ' '
}

require_dispatch_rows() {
  local packets="docs/program-coordination/subagent-worker-packets.md"
  require_file "$packets"
  if [ ! -f "$packets" ]; then
    return
  fi
  for task in DDD-W01 DDD-W02 DDD-W03 DDD-W04 DDD-W05 DDD-W06 DDD-W07 DDD-W08; do
    if ! awk -v task="^### ${task}:" '
      $0 ~ task { in_section = 1 }
      in_section && /^### / && $0 !~ task { exit }
      in_section { print }
    ' "$packets" | rg -n "Inventory rows required:" >/dev/null; then
      fail "$packets must require pasted inventory rows for $task"
    fi
  done
}

controller_count="$(count_matching_files backend/canvas-engine/src/main/java/org/chovy/canvas/web '*Controller.java')"
data_object_count="$(count_matching_files backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject '*DO.java')"
mapper_count="$(count_matching_files backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper '*Mapper.java')"
service_count="$(count_files \
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain \
  backend/canvas-engine/src/main/java/org/chovy/canvas/engine \
  backend/canvas-engine/src/main/java/org/chovy/canvas/platform \
  backend/canvas-engine/src/main/java/org/chovy/canvas/architecture \
  backend/canvas-engine/src/main/java/org/chovy/canvas/strategy/architecture)"
test_count="$(count_matching_files backend/canvas-engine/src/test/java '*Test.java')"
persistence_count=$((data_object_count + mapper_count))

require_inventory_rows docs/ddd-rewrite/inventory/http-api-inventory.md "HTTP APIs" "$controller_count" yes
require_inventory_rows docs/ddd-rewrite/inventory/persistence-ownership.md "persistence" "$persistence_count" yes
require_inventory_rows docs/ddd-rewrite/inventory/service-ownership.md "services" "$service_count" yes
require_inventory_rows docs/ddd-rewrite/inventory/test-ownership.md "tests" "$test_count" yes
require_inventory_rows docs/ddd-rewrite/inventory/cross-context-dependencies.md "cross-context dependencies" 1 no
require_dispatch_rows

if [ "$failures" -gt 0 ]; then
  echo "Inventory readiness failed: $failures"
  exit 1
fi

echo "Inventory readiness passed."
