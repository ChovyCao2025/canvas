#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
MIGRATION_DIR="$ROOT_DIR/backend/canvas-engine/src/main/resources/db/migration"
NOTES_DIR="$ROOT_DIR/docs/architecture/evidence/migrations"
BASELINE_FILE="$NOTES_DIR/released-baseline.version"

fail() {
  echo "ERROR: $*" >&2
  exit 1
}

usage() {
  cat <<'EOF'
Usage: scripts/release/check-flyway-migration.sh [--from-version VERSION]

Checks Flyway migration policy:
  - migration files must match V<integer>__<description>.sql
  - version numbers must be unique and strictly increasing when sorted
  - new high-risk migrations after the released baseline require notes in
    docs/architecture/evidence/migrations/<migration-file>.md

Set CANVAS_MIGRATION_BASE_VERSION or pass --from-version to override the
released baseline for a specific release.
EOF
}

FROM_VERSION="${CANVAS_MIGRATION_BASE_VERSION:-}"

while [[ $# -gt 0 ]]; do
  case "$1" in
    --from-version)
      [[ $# -ge 2 ]] || fail "--from-version requires a value"
      FROM_VERSION="$2"
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

[[ -d "$MIGRATION_DIR" ]] || fail "migration directory is missing: $MIGRATION_DIR"

if git -C "$ROOT_DIR" rev-parse --is-inside-work-tree >/dev/null 2>&1; then
  missing_tracked_migrations=()
  while IFS= read -r tracked_migration; do
    [[ -f "$ROOT_DIR/$tracked_migration" ]] || missing_tracked_migrations+=("$tracked_migration")
  done < <(git -C "$ROOT_DIR" ls-files 'backend/canvas-engine/src/main/resources/db/migration/V*.sql')

  if [[ ${#missing_tracked_migrations[@]} -gt 0 ]]; then
    printf 'ERROR: tracked Flyway migration files are missing from the worktree:\n' >&2
    printf '  - %s\n' "${missing_tracked_migrations[@]}" >&2
    fail "do not delete or renumber versioned migrations that may have been applied"
  fi
fi

if [[ -z "$FROM_VERSION" && -f "$BASELINE_FILE" ]]; then
  FROM_VERSION="$(tr -d '[:space:]' < "$BASELINE_FILE")"
fi
FROM_VERSION="${FROM_VERSION#V}"
FROM_VERSION="${FROM_VERSION:-0}"
[[ "$FROM_VERSION" =~ ^[0-9]+$ ]] || fail "baseline version must be numeric or V-prefixed numeric: $FROM_VERSION"

files=()
while IFS= read -r file; do
  files+=("$file")
done < <(find "$MIGRATION_DIR" -maxdepth 1 -type f -name 'V*.sql' | sort -V)

[[ ${#files[@]} -gt 0 ]] || fail "no Flyway migrations found in $MIGRATION_DIR"

versions=()
for file in "${files[@]}"; do
  base="$(basename "$file")"
  if [[ ! "$base" =~ ^V([0-9]+)__[A-Za-z0-9][A-Za-z0-9_]*\.sql$ ]]; then
    fail "invalid Flyway migration filename: $base"
  fi
  versions+=("${BASH_REMATCH[1]}")
done

duplicates="$(printf '%s\n' "${versions[@]}" | sort -n | uniq -d | tr '\n' ' ')"
[[ -z "${duplicates// }" ]] || fail "duplicate Flyway migration versions: $duplicates"

previous=0
highest=0
while IFS= read -r version; do
  [[ "$version" -gt "$previous" ]] || fail "migration versions are not strictly increasing near V$version"
  previous="$version"
  highest="$version"
done < <(printf '%s\n' "${versions[@]}" | sort -n)

HIGH_RISK_PATTERN='(^|[^A-Z_])(DROP|TRUNCATE|DELETE[[:space:]]+FROM|ALTER[[:space:]]+TABLE|RENAME[[:space:]]+TABLE|MODIFY[[:space:]]|CHANGE[[:space:]]|UPDATE[[:space:]].*SET|CREATE[[:space:]]+(UNIQUE[[:space:]]+)?INDEX|UNIQUE[[:space:]]+KEY)([^A-Z_]|$)'

new_migrations=()
high_risk_migrations=()
for file in "${files[@]}"; do
  base="$(basename "$file")"
  [[ "$base" =~ ^V([0-9]+)__ ]] || fail "invalid Flyway migration filename: $base"
  version="${BASH_REMATCH[1]}"
  if [[ "$version" -gt "$FROM_VERSION" ]]; then
    new_migrations+=("$base")
    if grep -Eiq "$HIGH_RISK_PATTERN" "$file"; then
      high_risk_migrations+=("$base")
      note="$NOTES_DIR/${base}.md"
      [[ -f "$note" ]] || fail "missing backup notes for high-risk migration: $note"
      grep -Eiq '^##[[:space:]]+Backup' "$note" || fail "backup notes must include a Backup section: $note"
      grep -Eiq '^##[[:space:]]+Restore' "$note" || fail "backup notes must include a Restore section: $note"
      grep -Eiq '^##[[:space:]]+Dry run' "$note" || fail "backup notes must include a Dry run section: $note"
      grep -Eiq '^##[[:space:]]+Rollback owner' "$note" || fail "backup notes must include a Rollback owner section: $note"
    fi
  fi
done

echo "flyway migration policy passed"
echo "migration_dir: $MIGRATION_DIR"
echo "released_baseline: V$FROM_VERSION"
echo "highest_version: V$highest"
echo "migration_count: ${#files[@]}"
echo "new_migration_count: ${#new_migrations[@]}"

if [[ ${#new_migrations[@]} -gt 0 ]]; then
  printf 'new_migrations:\n'
  printf '  - %s\n' "${new_migrations[@]}"
fi

if [[ ${#high_risk_migrations[@]} -gt 0 ]]; then
  printf 'high_risk_migrations_with_notes:\n'
  printf '  - %s\n' "${high_risk_migrations[@]}"
fi
