# P2-080 Staging Diff Review - 2026-06-07

Purpose: review the P2-080 core pathspec before any staging or implementation continues, so this session does not duplicate other active sessions.

Inputs:

- `docs/product-evolution/evidence/active-session-coordination-2026-06-07.md`
- `docs/product-evolution/evidence/p2-080-conversation-handoff-boundary.md`
- `docs/product-evolution/evidence/p2-080-staging-pathspec.txt`
- `docs/product-evolution/evidence/p2-080-deferred-overlap-pathspec.txt`
- `docs/product-evolution/evidence/p2-080-clean-core-candidate-pathspec.txt`
- `docs/product-evolution/evidence/p2-080-shared-split-required-pathspec.txt`

No P2-080 files were staged or committed during this review.

Current index warning:

- A later `git diff --cached --name-only` check showed 441 already-staged files in the shared repository index.
- A P2-080/conversation path filter over the staged file list produced no matches.
- The staged files appear to belong to other active BI/OLAP/Flink-style work and must not be used as evidence for P2-080 readiness.
- Do not run broad `git commit`, `git restore --staged`, or other index-wide commands from this session.

## Summary

The P2-080 core package is not ready for blind staging.

Core foundation files exist and the focused conversation verifier has passed in the current handoff history, but the proposed staging pathspec includes shared files with mixed concerns:

1. Product-evolution index files include P2-081 through P2-089 entries, not just P2-080.
2. Frontend conversation files still include SCRM workspace, SOP task, and AI reply suggestion behavior that belongs to P2-082D and P2-082L.
3. `ConversationIngressService` previously depended on `ConversationWorkspaceService`, which is deferred as P2-082D; that direct coupling has now been removed from the P2-080 core service and covered by a boundary test.

The safe next action is to split or narrow the P2-080 staging set before any `git add`.

Follow-up split artifacts:

- `p2-080-clean-core-candidate-pathspec.txt` lists files that appear safe for P2-080 core review after removing known mixed frontend/index files. `ConversationIngressService` and its test are included again after removing direct P2-082D workspace coupling, `conversationCoreApi` / `conversationCorePresentation` / `ConversationCoreInspectionPanel` provide P2-080-only frontend service, presentation, and inspection surfaces, and `p2-080-product-index-hunks.md` records the P2-080-only product index rows without staging mixed index files wholesale.
- `p2-080-shared-split-required-pathspec.txt` lists shared files that still need hunk-level splitting, refactoring, or an explicit ownership decision before staging.
- The clean-core candidate pathspec is still review-only, not approval for blind staging, because frontend and product index shared files remain split-required.

## Pathspec Validation

Validated on 2026-06-07:

- `p2-080-staging-pathspec.txt`: 81 entries, no missing paths.
- `p2-080-deferred-overlap-pathspec.txt`: 81 entries, no missing paths.
- `p2-080-clean-core-candidate-pathspec.txt`: 74 entries, no missing paths; review candidate only.
- `p2-080-shared-split-required-pathspec.txt`: 11 entries, no missing paths; split-required list.

Commands used:

```bash
cat docs/product-evolution/evidence/p2-080-staging-pathspec.txt |
  while IFS= read -r item; do
    [ -e "$item" ] || printf 'STAGING_MISSING %s\n' "$item"
  done

cat docs/product-evolution/evidence/p2-080-deferred-overlap-pathspec.txt |
  while IFS= read -r item; do
    [ -e "$item" ] || printf 'DEFERRED_MISSING %s\n' "$item"
  done
```

## Findings

### 1. Index files are mixed with non-P2-080 product slices

Files:

- `docs/product-evolution/IMPLEMENTATION_ORDER.md`
- `docs/product-evolution/specs/INDEX.md`
- `docs/product-evolution/plans/INDEX.md`

Evidence:

- The diff adds P2-080, but also P2-081, P2-082, P2-082D/D2/E/F/G/H/I/J/K/L/M/N/O/P/Q/R/S/T/U/V/W/X/Y/Z/AA/AB/AC/AD, and P2-083 through P2-089.
- Example lines after the current diff:
  - `docs/product-evolution/IMPLEMENTATION_ORDER.md:131` starts P2-081.
  - `docs/product-evolution/IMPLEMENTATION_ORDER.md:133` starts P2-082D.
  - `docs/product-evolution/IMPLEMENTATION_ORDER.md:141` starts P2-082K.
  - `docs/product-evolution/IMPLEMENTATION_ORDER.md:142` starts P2-082L.
  - `docs/product-evolution/specs/INDEX.md:133` starts non-P2-080 entries.
  - `docs/product-evolution/plans/INDEX.md:133` starts non-P2-080 entries.

Impact:

- These index hunks cannot be staged as a clean P2-080 core package.
- Staging them would pull unrelated product slices into the P2-080 change and repeat work owned by other active sessions.

Required split:

- For P2-080 core staging, include only the P2-080 index rows.
- Keep P2-081 through P2-089 rows for their owning sessions or a separate product-evolution index maintenance change.

Follow-up split result:

- `docs/product-evolution/evidence/p2-080-product-index-hunks.md` records the exact P2-080-only rows for `IMPLEMENTATION_ORDER.md`, `specs/INDEX.md`, and `plans/INDEX.md`.
- The three product index files remain split-required because their current content still includes P2-081 through P2-089 rows.

### 2. Frontend conversation service is mixed with P2-082D and P2-082L behavior

File:

- `frontend/src/services/conversationApi.ts`

Evidence:

- Lines 33-52 are P2-080-compatible session ingress and inspection helpers.
- Lines 54-93 add workspace/work-item/SOP/timeline APIs, which belong to P2-082D.
- Lines 95-121 add AI reply suggestion APIs, which belong to P2-082L.

Impact:

- The file cannot be staged wholesale as P2-080 core.
- A P2-080-only package should include only ingress, adapter ingress, recent sessions, and messages unless the frontend workspace slice is explicitly included.

Required split:

- P2-080: `ingestConversationReply`, `ingestConversationAdapterReply`, `listConversationSessions`, `listConversationMessages`.
- P2-082D: work item, inbox, assignment, status, SOP task, and timeline APIs.
- P2-082L: AI reply suggestion generate/list/review APIs.

Follow-up split result:

- `frontend/src/services/conversationCoreApi.ts` and `frontend/src/services/conversationCoreApi.test.ts` now provide the P2-080-only service surface for ingress, adapter ingress, recent sessions, and messages.
- `frontend/src/services/conversationApi.ts` remains split-required because it still contains P2-082D workspace and P2-082L AI reply functions.

### 3. Frontend conversation presentation/page files are mixed with P2-082D and P2-082L UI

Files:

- `frontend/src/pages/conversations/conversationPresentation.ts`
- `frontend/src/pages/conversations/index.tsx`
- related tests under `frontend/src/pages/conversations/`

Evidence:

- `conversationPresentation.ts:56-130` defines work items, contact profile, SOP tasks, audits, and workspace timeline types.
- `conversationPresentation.ts:132-215` defines AI reply suggestion types and payloads.
- `index.tsx:31-40` imports assignment, SOP, timeline, and AI suggestion service functions.
- `index.tsx:72-83` stores selected work item, workspace timeline, and AI suggestions as primary page state.
- `index.tsx:230-260` generates and reviews AI suggestions.
- `index.tsx:540-605` renders AI suggestion controls.
- `index.tsx:622-630` begins SOP task rendering.

Impact:

- The frontend `/conversations` page is already a P2-082D/P2-082L workspace surface, not only a P2-080 session inspection page.
- It must not be staged as P2-080 core without hunk-level splitting or explicit scope expansion.

Required split:

- P2-080 frontend should remain limited to session/message inspection, adapter test ingress, and the conversation WAIT authoring preset.
- P2-082D owns work-item inbox, assignment, SOP tasks, timeline, audits, and contact profile UI.
- P2-082L owns AI suggestion generation, status filtering, accept/reject review, and risk flag UI.

Follow-up split result:

- `frontend/src/pages/conversations/conversationCorePresentation.ts` and `frontend/src/pages/conversations/conversationCorePresentation.test.ts` now provide the P2-080-only presentation helper surface for conversation status, duplicate/resume outcome, message summary, and date-time formatting.
- `frontend/src/pages/conversations/ConversationCoreInspectionPanel.tsx` and `frontend/src/pages/conversations/ConversationCoreInspectionPanel.test.tsx` now provide the P2-080-only session/message inspection component that calls only `conversationCoreApi`.
- `frontend/src/pages/conversations/conversationPresentation.ts` re-exports the P2-080 helpers from the core module but remains split-required because it still contains P2-082D workspace and P2-082L AI reply types/helpers.

### 4. `ConversationIngressService` P2-082D coupling was removed

File:

- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/conversation/ConversationIngressService.java`
- `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/conversation/ConversationIngressServiceTest.java`

Evidence:

- The service no longer declares `ConversationWorkspaceService`, `ObjectProvider`, or `recordInboundMessage` references.
- `ConversationIngressServiceTest.ingressServiceDoesNotDependOnWorkspaceSlice` asserts that ingress constructors do not expose `ConversationWorkspaceService` or Spring `ObjectProvider` parameters.
- The test checks parameter type names so the P2-080 test itself does not compile against the P2-082D workspace class.

Impact:

- P2-080 ingress is limited to session/message persistence, context update, idempotency handling, and `WAIT` resume.
- P2-082D remains responsible for workspace work item creation and timeline side effects.

Staging decision:

- `ConversationIngressService.java` and `ConversationIngressServiceTest.java` have moved from split-required back into the P2-080 clean-core review candidate.

### 5. WAIT authoring preset looks P2-080-compatible

Files:

- `frontend/src/components/node-panel/nodeLibrary.ts`
- `frontend/src/components/node-panel/nodeLibrary.test.ts`
- `frontend/src/pages/canvas-editor/insertNode.test.ts`
- `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/wait/WaitEventFilterTest.java`

Evidence:

- `WaitEventFilterTest` adds a `CONVERSATION_REPLY` filter test.
- `nodeLibrary.ts` adds a `conversation-reply-wait` preset that still creates backend node type `WAIT`.
- `insertNode.test.ts` proves the preset display name and default `bizConfig` are preserved.

Impact:

- These changes align with P2-080 core because they expose a product-facing WAIT preset without adding a new backend node type.

Staging decision:

- These files can remain in the P2-080 core review set after normal diff review.

## Clean-Core Candidate And Split-Required Lists

The first staging pathspec was too broad for direct use. Two narrower lists now exist:

### Clean-core candidate

File:

- `docs/product-evolution/evidence/p2-080-clean-core-candidate-pathspec.txt`

Use this only for review, not as proof of a complete package. It intentionally excludes:

- product-evolution index files that contain P2-081 through P2-089 rows;
- P2-080 spec/plan files that still contain mixed handoff language and broad verification notes;
- frontend `/conversations` and `conversationApi` files, because they contain P2-082D/P2-082L behavior.

### Split-required

File:

- `docs/product-evolution/evidence/p2-080-shared-split-required-pathspec.txt`

Every file in this list needs one of these before P2-080 core staging:

- hunk-level split so only P2-080 content remains;
- small refactor to remove P2-082 coupling from P2-080 core;
- explicit decision to move the whole file into a later P2-082 slice.

## Recommended Next Action

Do not run `git add` against `p2-080-staging-pathspec.txt` yet.

First split the staging plan into three buckets:

1. P2-080 clean core:
   - session/message schema, DAL, ingress service without P2-082 workspace coupling, adapter harness/fixtures, WhatsApp acceleration templates, WAIT resume coverage, authoring preset.
2. Shared files requiring hunk-level split:
   - product-evolution index files;
   - `frontend/src/services/conversationApi.ts`;
   - `frontend/src/pages/conversations/*`.
3. Deferred P2-082 files:
   - everything listed in `p2-080-deferred-overlap-pathspec.txt`.

Use these helper checks while splitting:

```bash
cat docs/product-evolution/evidence/p2-080-clean-core-candidate-pathspec.txt |
  while IFS= read -r item; do
    [ -e "$item" ] || printf 'CLEAN_CORE_MISSING %s\n' "$item"
  done

cat docs/product-evolution/evidence/p2-080-shared-split-required-pathspec.txt |
  while IFS= read -r item; do
    [ -e "$item" ] || printf 'SPLIT_REQUIRED_MISSING %s\n' "$item"
  done
```

After splitting, rerun:

```bash
scripts/verify-conversation-focus.sh
```

Latest rerun after removing the ingress/workspace coupling:

- `cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test -pl canvas-engine -Dtest=ConversationIngressServiceTest`
  - 6 tests run, 0 failures, 0 errors, 0 skipped.
- `scripts/verify-conversation-focus.sh`
  - backend Surefire: 191 tests run, 0 failures, 0 errors, 0 skipped.
  - frontend Vitest: 8 files passed, 34 tests passed before the `conversationCoreApi` split.
- `scripts/verify-conversation-focus.sh --frontend-logic-only`
  - frontend Vitest: 5 files passed, 17 tests passed after adding the P2-080-only `conversationCoreApi` split.
- `scripts/verify-conversation-focus.sh`
  - backend Surefire: 191 tests run, 0 failures, 0 errors, 0 skipped.
  - frontend Vitest: 9 files passed, 35 tests passed after adding the P2-080-only `conversationCoreApi` split.
- `scripts/verify-conversation-focus.sh --frontend-logic-only`
  - frontend Vitest: 6 files passed, 21 tests passed after adding the P2-080-only `conversationCorePresentation` split.
- `scripts/verify-conversation-focus.sh --frontend-conversation-only`
  - frontend Vitest: 9 files passed, 27 tests passed after adding the P2-080-only `ConversationCoreInspectionPanel` split.
- `scripts/verify-conversation-focus.sh`
  - backend Surefire: 191 tests run, 0 failures, 0 errors, 0 skipped.
  - frontend Vitest: 11 files passed, 40 tests passed after adding the P2-080-only `ConversationCoreInspectionPanel` split.
- `scripts/verify-p2-080-handoff-boundary.sh`
  - staging pathspec entries: 80.
  - deferred pathspec entries: 81.
  - staged files: 0.
- Product index hunk check:
  - `rg -n '^\\| P2-080 \\|' docs/product-evolution/IMPLEMENTATION_ORDER.md docs/product-evolution/specs/INDEX.md docs/product-evolution/plans/INDEX.md docs/product-evolution/evidence/p2-080-product-index-hunks.md` found the same three P2-080 rows in the live index files and the hunk evidence file.
- `scripts/verify-p2-080-handoff-boundary.sh`
  - staging pathspec entries: 81.
  - deferred pathspec entries: 81.
  - staged files: 0.
- `scripts/verify-conversation-focus.sh`
  - backend Surefire: 191 tests run, 0 failures, 0 errors, 0 skipped.
  - frontend Vitest: 10 files passed, 39 tests passed after adding the P2-080-only `conversationCorePresentation` split.
- `scripts/verify-p2-080-handoff-boundary.sh`
  - staging pathspec entries: 78.
  - deferred pathspec entries: 81.
  - staged files: 0.

Then perform a staged diff review:

```bash
git diff --cached --name-only | rg 'p2-080|conversation|Conversation|verify-conversation|scaffold-conversation|nodeLibrary|insertNode|WaitEventFilter|V270'
git diff --cached -- docs/product-evolution/evidence/p2-080-clean-core-candidate-pathspec.txt
```

Because the shared repository index currently contains unrelated staged files, a broad `git diff --cached` is not a reliable P2-080 review command until those unrelated staged files are handled by their owning sessions.
