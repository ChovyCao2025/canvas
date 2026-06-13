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

TMP_FILES=()
cleanup() {
  if [ "${#TMP_FILES[@]}" -gt 0 ]; then
    rm -f "${TMP_FILES[@]}"
  fi
}
trap cleanup EXIT

write_expected_paths() {
  local out
  out="$(mktemp)"
  TMP_FILES+=("$out")
  find "$@" -name '*.java' 2>/dev/null | sort > "$out" || true
  echo "$out"
}

write_matching_paths() {
  local out
  local root="$1"
  local pattern="$2"
  out="$(mktemp)"
  TMP_FILES+=("$out")
  if [ -d "$root" ]; then
    find "$root" -name "$pattern" | sort > "$out"
  else
    : > "$out"
  fi
  echo "$out"
}

write_combined_matching_paths() {
  local out
  out="$(mktemp)"
  TMP_FILES+=("$out")
  shift 0
  : > "$out"
  while [ "$#" -gt 0 ]; do
    local root="$1"
    local pattern="$2"
    shift 2
    if [ -d "$root" ]; then
      find "$root" -name "$pattern" >> "$out"
    fi
  done
  sort -o "$out" "$out"
  echo "$out"
}

require_inventory_rows() {
  local file="$1"
  local row_label="$2"
  local expected_count="$3"
  local require_tests="$4"
  local expected_paths_file="${5:-}"

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

  local parsed_paths
  parsed_paths="$(mktemp)"
  TMP_FILES+=("$parsed_paths")

  awk -v file="$file" -v label="$row_label" -v require_tests="$require_tests" -v parsed_paths="$parsed_paths" '
    function trim(value) {
      gsub(/^[[:space:]]+|[[:space:]]+$/, "", value)
      return value
    }
    function reset_block() {
      source_value = current_value = module_value = target_value = tests_value = ""
      owner_count = 0
      waiting = ""
    }
    function set_field(field, value) {
      value = trim(value)
      if (field == "source") {
        source_value = value
      } else if (field == "current") {
        current_value = value
      } else if (field == "module") {
        module_value = value
      } else if (field == "target") {
        target_value = value
      } else if (field == "tests") {
        tests_value = value
      } else if (field == "owner" && value != "") {
        owner_count++
      }
      waiting = value == "" ? field : ""
    }
    function validate_block() {
      if (!in_block) {
        return
      }
      if (source_value == "") {
        printf("FAIL: %s row %d for %s has empty source value\n", file, row_start, label)
        failures++
      }
      if (current_value == "") {
        printf("FAIL: %s row %d for %s is missing current path:\n", file, row_start, label)
        failures++
      }
      if (module_value == "") {
        printf("FAIL: %s row %d for %s has empty target module:\n", file, row_start, label)
        failures++
      }
      if (target_value == "") {
        printf("FAIL: %s row %d for %s is missing target package: or target role:\n", file, row_start, label)
        failures++
      }
      if (owner_count != 1) {
        printf("FAIL: %s row %d for %s must contain exactly one of owning worker: or coordinator decision:\n", file, row_start, label)
        failures++
      }
      if (require_tests == "yes" && tests_value == "") {
        printf("FAIL: %s row %d for %s is missing required tests:\n", file, row_start, label)
        failures++
      }
      if (current_value != "") {
        print current_value >> parsed_paths
      }
    }
    /^(old class|old file|source path):/ {
      validate_block()
      in_block = 1
      row_start = NR
      reset_block()
      sub(/^[^:]+:/, "", $0)
      set_field("source", $0)
      next
    }
    in_block && /^[[:space:]]+/ && waiting != "" {
      set_field(waiting, $0)
      next
    }
    in_block && /^current path:/ {
      sub(/^current path:/, "", $0)
      set_field("current", $0)
      next
    }
    in_block && /^target module:/ {
      sub(/^target module:/, "", $0)
      set_field("module", $0)
      next
    }
    in_block && /^(target package|target role):/ {
      sub(/^[^:]+:/, "", $0)
      set_field("target", $0)
      next
    }
    in_block && /^(owning worker|coordinator decision):/ {
      sub(/^[^:]+:/, "", $0)
      set_field("owner", $0)
      next
    }
    in_block && /^required tests:/ {
      sub(/^required tests:/, "", $0)
      set_field("tests", $0)
      next
    }
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

  local duplicate_current
  duplicate_current="$(sort "$parsed_paths" | uniq -d)"
  if [ -n "$duplicate_current" ]; then
    while IFS= read -r duplicate; do
      [ -n "$duplicate" ] && fail "$file has duplicate current path for $row_label: $duplicate"
    done <<<"$duplicate_current"
  fi

  local duplicate_source
  duplicate_source="$(awk '
    function trim(value) {
      gsub(/^[[:space:]]+|[[:space:]]+$/, "", value)
      return value
    }
    /^(old class|old file|source path):/ {
      value = $0
      sub(/^[^:]+:/, "", value)
      value = trim(value)
      if (value == "") {
        getline value
        value = trim(value)
      }
      if (value != "") {
        print value
      }
    }
  ' "$file" | sort | uniq -d)"
  if [ -n "$duplicate_source" ]; then
    while IFS= read -r duplicate; do
      [ -n "$duplicate" ] && fail "$file has duplicate source row for $row_label: $duplicate"
    done <<<"$duplicate_source"
  fi

  if [ -n "$expected_paths_file" ]; then
    while IFS= read -r expected_path; do
      [ -z "$expected_path" ] && continue
      if ! grep -Fxq "$expected_path" "$parsed_paths"; then
        fail "$file is missing current path for $row_label: $expected_path"
      fi
    done < "$expected_paths_file"

    while IFS= read -r actual_path; do
      [ -z "$actual_path" ] && continue
      if ! grep -Fxq "$actual_path" "$expected_paths_file"; then
        fail "$file has unexpected current path for $row_label: $actual_path"
      fi
    done < "$parsed_paths"
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

controller_paths="$(write_matching_paths backend/canvas-engine/src/main/java/org/chovy/canvas/web '*Controller.java')"
data_object_paths="$(write_matching_paths backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject '*DO.java')"
mapper_paths="$(write_matching_paths backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper '*Mapper.java')"
service_paths="$(write_expected_paths \
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain \
  backend/canvas-engine/src/main/java/org/chovy/canvas/engine \
  backend/canvas-engine/src/main/java/org/chovy/canvas/platform \
  backend/canvas-engine/src/main/java/org/chovy/canvas/architecture \
  backend/canvas-engine/src/main/java/org/chovy/canvas/strategy/architecture)"
test_paths="$(write_matching_paths backend/canvas-engine/src/test/java '*Test.java')"
persistence_paths="$(mktemp)"
TMP_FILES+=("$persistence_paths")
cat "$data_object_paths" "$mapper_paths" | sort > "$persistence_paths"

controller_count="$(wc -l < "$controller_paths" | tr -d ' ')"
data_object_count="$(wc -l < "$data_object_paths" | tr -d ' ')"
mapper_count="$(wc -l < "$mapper_paths" | tr -d ' ')"
service_count="$(count_files \
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain \
  backend/canvas-engine/src/main/java/org/chovy/canvas/engine \
  backend/canvas-engine/src/main/java/org/chovy/canvas/platform \
  backend/canvas-engine/src/main/java/org/chovy/canvas/architecture \
  backend/canvas-engine/src/main/java/org/chovy/canvas/strategy/architecture)"
test_count="$(wc -l < "$test_paths" | tr -d ' ')"
persistence_count=$((data_object_count + mapper_count))

require_inventory_rows docs/ddd-rewrite/inventory/http-api-inventory.md "HTTP APIs" "$controller_count" yes "$controller_paths"
require_inventory_rows docs/ddd-rewrite/inventory/persistence-ownership.md "persistence" "$persistence_count" yes "$persistence_paths"
require_inventory_rows docs/ddd-rewrite/inventory/service-ownership.md "services" "$service_count" yes "$service_paths"
require_inventory_rows docs/ddd-rewrite/inventory/test-ownership.md "tests" "$test_count" yes "$test_paths"
require_inventory_rows docs/ddd-rewrite/inventory/cross-context-dependencies.md "cross-context dependencies" 1 no
require_dispatch_rows

if [ "$failures" -gt 0 ]; then
  echo "Inventory readiness failed: $failures"
  exit 1
fi

echo "Inventory readiness passed."
