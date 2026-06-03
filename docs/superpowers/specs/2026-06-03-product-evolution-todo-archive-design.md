# Product Evolution Todo and Archive Design

Date: 2026-06-03

## Purpose

`docs/product-evolution` currently mixes product strategy, competitor research, technical selection, interaction scans, and implementation candidates. The folder needs a clean working system so actionable product work is separated from original research material.

The implementation will create a dedicated priority-based todo area under `docs/product-evolution/todo/` and move each processed source document into `docs/product-evolution/archive/`. The todo area must contain only filtered, rewritten, execution-oriented documents.

## Scope

Process all current Markdown files directly under `docs/product-evolution`:

- `product-audit-report-2026-05-31.md`
- `product-best-practice-roadmap-2026-05-31.md`
- `product-strategy-supplementary-dimensions-2026-05-31.md`
- `product-evolution-directions-2026-05-31.md`
- `product-evolution-directions-ext-2026-05-31.md`
- `product-strategy-dual-track-2026-05-31.md`
- `product-interaction-directions-2026-06-01.md`
- `product-interaction-directions-2026-06-02.md`
- `mautic-comparison-2026-06.md`
- `mautic-capabilities-to-adopt.md`
- `mautic-plugin-feasibility-analysis.md`
- `plugin-candidate-list.md`
- `tech-selection-whitepaper.md`

In scope:

- read and assess each source file;
- verify obvious external version or product claims where feasible;
- identify unreasonable scope, duplicated strategy, stale facts, and overbroad "all in" recommendations;
- extract only actionable items into rewritten todo documents;
- assign each rewritten document to `p0`, `p1`, `p2`, or `p3`;
- build a `todo/INDEX.md` that explains the priority model, source mapping, and filtered-out material;
- move every processed original file into `archive/2026-06-03/` after its actionable content is represented or intentionally filtered out.

Out of scope:

- implementing product or code changes;
- preserving original long-form research inside todo;
- generating one PRD per bullet point;
- deleting source files;
- moving unrelated documentation outside `docs/product-evolution`.

## Directory Design

The target structure is:

```text
docs/product-evolution/
  todo/
    INDEX.md
    p0/
    p1/
    p2/
    p3/
  archive/
    2026-06-03/
```

Priority folders have these meanings:

- `p0/`: production blockers, compliance or security red lines, white-screen or navigation-breaking UX defects, tenant isolation risks, and high-ROI already-built capabilities that only need exposure.
- `p1/`: near-term operating loop and usability work, including templates, preview/dry-run, version visibility, runtime visibility, table/list improvements, and basic event or attribution foundations.
- `p2/`: medium-term platform capability, analytics depth, collaboration, integration, plugin groundwork, channel expansion, and data infrastructure work.
- `p3/`: long-term strategy, AI agent evolution, ecosystem commercialization, internationalization, mobile/PWA, advanced automation, and exploratory trend bets.

## Todo Document Format

Each generated todo document should be short enough to execute from. It should include:

- title and priority;
- source files;
- rationale for the priority;
- filtered scope, including what was removed or deferred;
- concrete work items;
- dependencies;
- acceptance criteria;
- notes about fact corrections or uncertainty.

Long research content stays in archive and is referenced by source filename rather than copied into todo.

## Filtering Rules

Do not copy "all in", "all configuration", or broad strategy language directly into todo. Rewrite it into staged, bounded work.

When a source document contains many independent themes, split the actionable content into separate todo documents. When multiple source files repeat the same theme, merge the content into one todo document and list all sources.

When a source is mostly research, create a concise adoption or decision todo only if it leads to a concrete action. Otherwise, record it in `todo/INDEX.md` as archived reference.

When a claim is stale or too uncertain to execute, keep it out of active priority folders unless the todo is specifically a validation or decision task.

## Initial Priority Mapping

Expected `p0` topics:

- production and compliance stopgaps from the audit report;
- ErrorBoundary, 404/403, request timeout/cancellation, offline detection, and basic accessibility stopgaps;
- tenant isolation and `/ops` endpoint authentication;
- stub AI/recommendation node handling;
- circuit breaker visibility and already-built runtime visibility surfaces;
- audience estimate and dry-run/preview foundations when the backend is already mostly present.

Expected `p1` topics:

- template browse/clone and message template management;
- version history, diff, canary UI, and execution request management;
- table filtering, row selection, export, and list usability;
- global fatigue control, consent/suppression management, and preference basics;
- lightweight attribution, conversion event intake, channel receipts, and basic dashboard/command-center improvements.

Expected `p2` topics:

- plugin and integration foundations;
- canvas import/export, projects, webhook/API key/SSO work;
- data platform and technical migration candidates;
- collaboration, comments, sharing, approval expansion, and personalization infrastructure;
- report builder, richer analytics, RFM/lifecycle grid, WeCom/RCS/channel expansion.

Expected `p3` topics:

- AI agent strategy and advanced AI optimization;
- outcome-based pricing, ISV marketplace, partner ecosystem, and managed-service strategy;
- internationalization, mobile/PWA, long-term self-serve growth journey, and advanced privacy computing;
- full plugin marketplace and commercial revenue sharing.

## Known Corrections

Several source documents need correction or downgrading before their content enters todo:

- Broad "全做" plans across 10, 15, or 29 directions are not executable as written and must be converted into staged priorities.
- Large configuration inventories such as 61, 93, or 154 configuration items should not become todo scope by themselves.
- `product-interaction-directions-2026-06-01.md` has a dependency inconsistency: user preference infrastructure is listed in phase 2 but later referenced as a phase 0 dependency.
- Mautic version statements should reflect that the 7.x line is active in 2026 and that 8.0 alpha is expected later in 2026.
- Apache Doris version references should be checked against current official releases; 4.1.0 exists by 2026-04, so any "Doris 4.0 latest" wording should be softened.
- Technical selection items should become migration or validation todos, not product feature todos, unless they directly unblock a product outcome.

## Processing Workflow

For each source file:

1. Read the file and classify it as audit, roadmap, strategy, interaction scan, competitor research, plugin research, or technical whitepaper.
2. Extract actionable items and merge duplicates with already extracted topics.
3. Rewrite the extracted content into one or more priority todo documents.
4. Update `todo/INDEX.md` with source mapping and filtering notes.
5. Move the original file into `archive/2026-06-03/`.

The move should happen only after the actionable content from that source has either been represented in todo or explicitly recorded as filtered/reference material.

## Error Handling

If a source claim cannot be verified locally or externally in reasonable time, mark the generated todo as a validation task or record uncertainty in the notes.

If two files conflict, prefer the newer, more specific, or better evidenced file, and record the older claim as superseded in the index.

If a source contains no actionable item after filtering, archive it and add a short explanation in `todo/INDEX.md`.

If the destination archive already has a file with the same name, keep the existing file intact and choose a non-conflicting filename for the moved source.

## Validation

The documentation restructuring should be validated with:

- `git status --short` to inspect all additions and moves;
- `find docs/product-evolution -maxdepth 3 -type f` to inspect final layout;
- `rg --files docs/product-evolution/todo docs/product-evolution/archive` to confirm todo and archive contents;
- spot checks of generated `todo/INDEX.md` and representative `p0`, `p1`, `p2`, and `p3` documents.

Application tests are not required because this pass only restructures and rewrites documentation.

## Success Criteria

The task is complete when:

- `docs/product-evolution/todo/p0`, `p1`, `p2`, and `p3` exist;
- `docs/product-evolution/todo/INDEX.md` explains the priorities and source mapping;
- todo documents contain only filtered, execution-oriented content;
- every original product-evolution source file has been processed and moved into `archive/2026-06-03/`;
- unreasonable broad scope has been downgraded, split, or filtered out;
- no original research source is deleted.
