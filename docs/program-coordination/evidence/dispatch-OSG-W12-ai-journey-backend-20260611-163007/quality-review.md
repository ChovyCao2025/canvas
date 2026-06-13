# OSG-W12 Quality Review

review status: PASS_WITH_CONCERNS

review scope:
Read-only code quality review for OSG-W12 only; no file edits and no
ledger/state writes.

files reviewed:
- Worker return and spec review evidence
- OSG-W12 packet and reviewer contract
- Six returned Java/test files
- Context: `AiJourneyDraftProposal.java`, `ExecutionTraceView.java`
- Contracts: `ai-operator-contract.md`, `canvas-dsl-v1.md`, execution child spec

requirements checked:
- Null handling, defensive copies, deterministic mock output, proposal ID shape,
  DSL content
- Meaningful test coverage and edge-case gaps
- DDD boundaries: no persistence leakage, no hidden publish side effects, no old
  engine imports
- API shape, validation behavior, maintainability
- Whether accepted concerns are non-blocking

commands inspected or run:
- `rg` scoped forbidden-reference checks: only intentional publish-field test
  assertion found
- `git status --short` scoped to six files and `backend/canvas-engine`: six
  assigned untracked files only
- `JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test -pl canvas-context-canvas -Dtest=JourneyGenerationServiceTest`:
  passed, 2 tests
- `JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test -pl canvas-context-marketing -Dtest=JourneyRiskAuditServiceTest`:
  passed, 2 tests
- `JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test -pl canvas-context-execution -Dtest=TraceExplanationFacadeTest`:
  passed, 2 tests
- `node tools/open-source-growth/guardrail-verifier.mjs`: passed with
  `{ "ok": true }`

findings:
- Non-blocking: `proposalId` uses slug plus epoch millis only, so duplicate IDs
  are possible for same-prompt generations in the same millisecond. Because this
  is preview/draft-only and not persisted here, this is not a closure blocker.
  Owner: canvas AI integration before durable proposal storage or concurrent UI
  caching.
- Non-blocking: risk audit's `MISSING_FAILURE_OR_EXIT_PATH` rule only checks
  for any edge into an `end` node; it does not prove reachability,
  all-terminal-branch coverage, or a distinct failure branch. Acceptable as a
  first preview heuristic, but should be hardened before it becomes a publish
  gate. Owner: marketing/risk follow-up.
- Non-blocking: tests cover the main happy paths and required minimum findings,
  but do not exercise invalid requests, repeated ID generation, disconnected end
  nodes, or immutable result mutation for the new service records. Existing
  boundary tests cover some immutability for shared API records.

required fixes:
None before closure.

residual risks:
- Java 21 remains required for Maven verification.
- The six files are untracked additions; attribution should stay tied to this
  dispatch evidence during integration.
- The OSG-W12 packet rollback text says frontend AI files, but the worker return
  has the correct backend rollback list.

ledger update:
Record OSG-W12 code quality review as `PASS_WITH_CONCERNS`; no required fixes.
Owner for residual concerns: coordinator/integration plus future
canvas/marketing AI hardening.
