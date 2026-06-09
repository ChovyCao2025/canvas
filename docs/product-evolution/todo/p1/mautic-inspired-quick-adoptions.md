# P1 - Mautic Inspired Quick Adoptions

## Sources

- `mautic-comparison-2026-06.md`
- `mautic-capabilities-to-adopt.md`

## Why P1

These are practical capabilities borrowed from Mautic that can improve operator safety and migration without adopting Mautic's full ecosystem model.

## Work Items

### Audience Send Semantics

- Add segment sending mode: static locked audience vs dynamic refreshing audience.

### Safe Preview

- Improve message preview across text, web push, and selected user contexts.

### Import Export

- Add canvas import/export for environment migration and template reuse.

### Project Folder

- Add basic project or folder grouping only if it does not duplicate existing canvas grouping work.

### AI Policy

- Publish an internal AI capability policy and roadmap if AI features are exposed to operators.

## Fact Corrections

- Mautic version claims should be softened to current 7.x active line and 8.0 alpha expectation in 2026.
- Canvas AI claims in the sources are inconsistent: some files describe AI nodes as stubs while others say AI is implemented. Treat production AI as unverified until code and behavior are confirmed.

## Deferred From Sources

- Mautic partner ecosystem and public community strategy are P3.
- Full AI marketplace is P3.

## Dependencies

- Dynamic audience refresh depends on audience resolver performance and snapshot semantics.
- Import/export depends on graph schema versioning and safe secret handling.

## Acceptance Criteria

- Operators can explicitly choose whether a scheduled segment send locks or refreshes the audience.
- Canvas export/import preserves graph, node config, edge config, and version metadata without leaking environment secrets.
- Preview output masks sensitive data and cannot accidentally send real messages.
