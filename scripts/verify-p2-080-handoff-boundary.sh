#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

STAGING_PATHSPEC="docs/product-evolution/evidence/p2-080-staging-pathspec.txt"
DEFERRED_PATHSPEC="docs/product-evolution/evidence/p2-080-deferred-overlap-pathspec.txt"

fail() {
  printf 'ERROR: %s\n' "$*" >&2
  exit 1
}

count_lines() {
  local file="$1"
  wc -l < "$file" | tr -d '[:space:]'
}

assert_file_exists() {
  local file="$1"
  [[ -f "$file" ]] || fail "missing required file: $file"
}

assert_no_blank_lines() {
  local file="$1"
  local blanks
  blanks="$(awk 'NF == 0 { print NR }' "$file")"
  [[ -z "$blanks" ]] || fail "$file contains blank pathspec lines at: $blanks"
}

assert_no_duplicate_lines() {
  local file="$1"
  local duplicates
  duplicates="$(sort "$file" | uniq -d)"
  [[ -z "$duplicates" ]] || fail "$file contains duplicate pathspec entries: $duplicates"
}

assert_all_paths_exist() {
  local file="$1"
  local missing=()
  local item

  while IFS= read -r item; do
    [[ -e "$item" ]] || missing+=("$item")
  done < "$file"

  if [[ "${#missing[@]}" -gt 0 ]]; then
    printf 'ERROR: %s contains missing paths:\n' "$file" >&2
    printf '  %s\n' "${missing[@]}" >&2
    exit 1
  fi
}

assert_no_pathspec_overlap() {
  local overlap
  overlap="$(comm -12 <(sort "$STAGING_PATHSPEC") <(sort "$DEFERRED_PATHSPEC"))"
  [[ -z "$overlap" ]] || fail "P2-080 staging and deferred pathspecs overlap: $overlap"
}

assert_no_staged_handoff_paths() {
  local staged
  local overlap

  staged="$(git diff --cached --name-only | sort)"
  [[ -n "$staged" ]] || return 0

  overlap="$(comm -12 <(printf '%s\n' "$staged") <(sort "$STAGING_PATHSPEC"))"
  [[ -z "$overlap" ]] || fail "staged files overlap P2-080 staging pathspec: $overlap"

  overlap="$(comm -12 <(printf '%s\n' "$staged") <(sort "$DEFERRED_PATHSPEC"))"
  [[ -z "$overlap" ]] || fail "staged files overlap P2-080 deferred pathspec: $overlap"
}

assert_file_exists "$STAGING_PATHSPEC"
assert_file_exists "$DEFERRED_PATHSPEC"

for pathspec in "$STAGING_PATHSPEC" "$DEFERRED_PATHSPEC"; do
  assert_no_blank_lines "$pathspec"
  assert_no_duplicate_lines "$pathspec"
  assert_all_paths_exist "$pathspec"
done

assert_no_pathspec_overlap
assert_no_staged_handoff_paths

printf 'P2-080 handoff boundary verification passed\n'
printf 'staging pathspec entries: %s\n' "$(count_lines "$STAGING_PATHSPEC")"
printf 'deferred pathspec entries: %s\n' "$(count_lines "$DEFERRED_PATHSPEC")"
printf 'staged files: %s\n' "$(git diff --cached --name-only | wc -l | tr -d '[:space:]')"
