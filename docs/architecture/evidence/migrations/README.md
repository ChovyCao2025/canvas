# Flyway Migration Release Evidence

`released-baseline.version` records the highest Flyway migration version that the release governance gate treats as already reviewed.

For every new migration above that baseline, `scripts/release/check-flyway-migration.sh` validates the filename and duplicate-version policy. If a new migration contains destructive or high-risk SQL, add a sibling evidence file named:

```text
docs/architecture/evidence/migrations/<migration-file>.md
```

The evidence file must include:

- `## Backup`
- `## Restore`
- `## Dry run`
- `## Rollback owner`

Do not bump `released-baseline.version` until the release evidence confirms the migration has been deployed and rollback evidence has been captured.
