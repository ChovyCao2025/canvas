# Plan: Release And Deployment Governance

1. Inventory current deployable assets versus checklist-only material.
2. Add or update environment profile files and startup validation.
3. Create migration backup/rollback conventions for new Flyway migrations.
4. Add CI pipeline checks for backend tests, frontend tests, lint/build, migration validation, and container build.
5. Convert deployment checklist into runbook steps with commands.
6. Add post-deploy verification and rollback drills.
