# Template Renderer And Variable Picker Implementation Plan

Status: Current implementation and focused verification passed on 2026-06-09; commit and merge status remain unverified in this audit.

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add one runtime-safe template renderer and a graph-aware variable picker.

**Architecture:** Keep rendering in backend `TemplateRenderService`; keep frontend variable availability in a pure helper consumed by `VariablePicker`.

**Tech Stack:** Java 21, Spring Boot, JUnit 5, React 18, TypeScript, Ant Design, Vitest.

---

## Spec Reference

- `docs/product-evolution/specs/p2-017-template-renderer-and-variable-picker.md`

## Current Status Note

The implementation files are present in the current worktree and fresh focused verification passed on 2026-06-09:

- `JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-engine test -Dtest=TemplateRenderServiceTest,UserInputHandlerTest,WaitEventFilterTest,WaitHandlerTest,ConnectedContentHandlerTest,ExecutionRerunControllerTest,CanvasBatchOperationControllerTest,RuntimeMigrationEvidenceTest` (covered `TemplateRenderServiceTest`; 48 total selected backend tests passed).
- `npm test -- variableAvailability.test.ts variablePicker.test.tsx executionTimeline.test.tsx` (covered `variableAvailability.test.ts` and `variablePicker.test.tsx`; 12 frontend tests passed).

Historical RED-state checks were not reproduced because the current worktree already contains the implementation. No commit or merge was created in this audit, so commit and merge status remain unverified.

## File Structure

- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/template/TemplateRenderService.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/template/TemplateRenderServiceTest.java`
- Create: `frontend/src/components/config-panel/variableAvailability.ts`
- Create: `frontend/src/components/config-panel/variableAvailability.test.ts`
- Create: `frontend/src/components/config-panel/VariablePicker.tsx`
- Create: `frontend/src/components/config-panel/variablePicker.test.tsx`
- Modify: `frontend/src/components/config-panel/index.tsx`

### Task 1: Template Renderer

**Files:**
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/template/TemplateRenderServiceTest.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/template/TemplateRenderService.java`

- [x] **Step 1: Write renderer tests**

Create tests:

```java
@Test
void rendersVariablesEscapesHtmlAndReportsMissingFields() {
    TemplateRenderService service = new TemplateRenderService(1000);

    TemplateRenderService.RenderResult result = service.render("Hi {{profile.name}}, {{missing}}", Map.of(
            "profile", Map.of("name", "<Ada>")));

    assertThat(result.output()).isEqualTo("Hi &lt;Ada&gt;, ");
    assertThat(result.errors()).extracting(TemplateRenderService.RenderError::code).contains("MISSING_VARIABLE");
}

@Test
void supportsDateFormattingConditionalsAndRepeatedLists() {
    TemplateRenderService service = new TemplateRenderService(1000);

    TemplateRenderService.RenderResult result = service.render(
            "{{#if paid}}paid{{/if}} {{formatDate createdAt 'yyyy-MM-dd'}} {{#each items}}{{name}};{{/each}}",
            Map.of("paid", true, "createdAt", "2026-06-03T10:00:00Z", "items", List.of(Map.of("name", "A"), Map.of("name", "B"))));

    assertThat(result.output()).contains("paid").contains("2026-06-03").contains("A;B;");
}
```

Historical RED-state boundary: not reproduced in this audit because the current worktree already contains the implementation.

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=TemplateRenderServiceTest
```

Expected: FAIL because renderer does not exist.

- [x] **Step 3: Implement renderer**

Implement `render(String template, Map<String,Object> context)` with interpolation, HTML escaping by default, missing-variable errors, max rendered length, `formatDate`, `#if`, and `#each` support.

- [x] **Step 4: Run renderer tests**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=TemplateRenderServiceTest
```

Expected: PASS.

### Task 2: Variable Availability And Picker

**Files:**
- Create: `frontend/src/components/config-panel/variableAvailability.ts`
- Create: `frontend/src/components/config-panel/variableAvailability.test.ts`
- Create: `frontend/src/components/config-panel/VariablePicker.tsx`
- Create: `frontend/src/components/config-panel/variablePicker.test.tsx`
- Modify: `frontend/src/components/config-panel/index.tsx`

- [x] **Step 1: Write availability tests**

Create `variableAvailability.test.ts`:

```ts
it('returns trigger profile and upstream variables but hides downstream output', () => {
  const result = availableVariables({
    selectedNodeId: 'send',
    nodes: [
      { id: 'start', outputs: ['trigger.eventCode'] },
      { id: 'api', outputs: ['api_user.name'] },
      { id: 'send', outputs: [] },
      { id: 'later', outputs: ['later.value'] },
    ],
    upstreamNodeIds: ['start', 'api'],
    profileFields: ['profile.email'],
    computedFields: ['computed.churnRisk'],
  })

  expect(result.map(item => item.token)).toContain('{{api_user.name}}')
  expect(result.map(item => item.token)).toContain('{{profile.email}}')
  expect(result.map(item => item.token)).not.toContain('{{later.value}}')
})
```

- [x] **Step 2: Write picker tests**

Create `variablePicker.test.tsx` with render, search, keyboard select, and insertion callback assertions.

- [x] **Step 3: Implement helper and picker**

`availableVariables` returns `{ token, label, source }[]`. `VariablePicker` renders searchable options and calls `onInsert(token)` without mutating other config fields.

- [x] **Step 4: Run frontend tests**

Run:

```bash
cd frontend && npm test -- variableAvailability.test.ts variablePicker.test.tsx
```

Expected: PASS.

### Task 3: Verification And Commit

**Files:**
- Modify: `docs/product-evolution/specs/p2-017-template-renderer-and-variable-picker.md`
- Modify: `docs/product-evolution/plans/p2-017-template-renderer-and-variable-picker-plan.md`

- [x] **Step 1: Run focused verification**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=TemplateRenderServiceTest
cd frontend && npm test -- variableAvailability.test.ts variablePicker.test.tsx
```

Expected: PASS.

Commit boundary: no commit was created in this audit; commit and merge status remain unverified.

Run:

```bash
git add backend/canvas-engine/src/main/java/org/chovy/canvas/engine/template backend/canvas-engine/src/test/java/org/chovy/canvas/engine/template frontend/src/components/config-panel docs/product-evolution/specs/p2-017-template-renderer-and-variable-picker.md docs/product-evolution/plans/p2-017-template-renderer-and-variable-picker-plan.md
git commit -m "feat: add template renderer and variable picker"
```

Expected: commit contains only renderer, variable picker, tests, and related docs.
