# Design System And Guided Experience Implementation Plan

Status: Open execution plan; implementation is not complete in this docs-only audit because the plan retains unchecked execution tasks.

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the first guided-experience slice: design tokens, consistent empty-state copy, product guide step helpers, accessible form option lookup, and reduced-motion support.

**Architecture:** Reuse existing `SystemOptionService` for content style and accessible form option categories, with no schema migration in this slice. Keep frontend design primitives small and testable: CSS tokens, an `EmptyState` component with exported copy helpers, and a `ProductGuide` component with exported guide-step helpers.

**Tech Stack:** Java 21, JUnit 5, Mockito, AssertJ, React 18, TypeScript, Ant Design, Vitest, CSS custom properties.

---

## Spec Reference

- `docs/product-evolution/specs/p2-014-design-system-guided-experience.md`
- Source item: `docs/product-evolution/todo/p2/product-opportunities-from-filtered-scope.md#design-system-and-guided-experience`

## File Structure

**Backend**
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/meta/SystemOptionService.java` - add design-system option grouping from existing system-option storage.
- Modify: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/meta/SystemOptionServiceTest.java`

**Frontend**
- Create: `frontend/src/design/tokens.css` - CSS custom properties and reduced-motion defaults.
- Create: `frontend/src/components/empty/EmptyState.tsx` - unified empty state component and copy helper.
- Create: `frontend/src/components/empty/EmptyState.test.tsx`
- Create: `frontend/src/components/guides/ProductGuide.tsx` - guided tour wrapper and guide-step helpers.
- Create: `frontend/src/components/guides/ProductGuide.test.tsx`

**Data And Config**
- No Flyway migration for this slice. Design tokens are static CSS, and content/form guidance reads existing `system_option` rows through `SystemOptionService`.

### Task 1: Design-System System Options

**Files:**
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/meta/SystemOptionService.java`
- Modify: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/meta/SystemOptionServiceTest.java`

- [ ] **Step 1: Write backend tests**

Append these tests to `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/meta/SystemOptionServiceTest.java`:

```java
@Test
void designSystemOptionsReturnsContentStyleAndFormPatterns() {
    SystemOptionMapper mapper = mock(SystemOptionMapper.class);
    SystemOptionDO tone = new SystemOptionDO();
    tone.setCategory("content_style_rule");
    tone.setOptionKey("concise");
    tone.setLabel("Concise");
    SystemOptionDO form = new SystemOptionDO();
    form.setCategory("accessible_form_pattern");
    form.setOptionKey("required_hint");
    form.setLabel("Required hint");
    when(mapper.selectList(any())).thenReturn(List.of(tone), List.of(form));
    SystemOptionService service = new SystemOptionService(mapper);

    SystemOptionService.DesignSystemOptions options = service.designSystemOptions(8L);

    assertThat(options.contentStyleRules()).extracting(StubOption::getKey).containsExactly("concise");
    assertThat(options.accessibleFormPatterns()).extracting(StubOption::getKey).containsExactly("required_hint");
}

@Test
void designSystemOptionsRequiresTenantForTenantScopedLookup() {
    SystemOptionMapper mapper = mock(SystemOptionMapper.class);
    SystemOptionService service = new SystemOptionService(mapper);

    assertThatThrownBy(() -> service.designSystemOptions(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("tenantId is required for design system options");
}
```

- [ ] **Step 2: Run backend tests and confirm red state**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=SystemOptionServiceTest
```

Expected: FAIL because `designSystemOptions` and `DesignSystemOptions` do not exist.

- [ ] **Step 3: Implement design-system option grouping**

Add this record and method to `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/meta/SystemOptionService.java`:

```java
public DesignSystemOptions designSystemOptions(Long tenantId) {
    if (tenantId == null) {
        throw new IllegalArgumentException("tenantId is required for design system options");
    }
    return new DesignSystemOptions(
            activeOptions("content_style_rule", tenantId),
            activeOptions("accessible_form_pattern", tenantId),
            activeOptions("motion_guideline", tenantId));
}

public record DesignSystemOptions(
        List<StubOption> contentStyleRules,
        List<StubOption> accessibleFormPatterns,
        List<StubOption> motionGuidelines) {}
```

- [ ] **Step 4: Run backend tests**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=SystemOptionServiceTest
```

Expected: PASS.

### Task 2: Design Tokens And Empty State

**Files:**
- Create: `frontend/src/design/tokens.css`
- Create: `frontend/src/components/empty/EmptyState.tsx`
- Create: `frontend/src/components/empty/EmptyState.test.tsx`

- [ ] **Step 1: Write empty-state tests**

Create `frontend/src/components/empty/EmptyState.test.tsx`:

```ts
import { describe, expect, it } from 'vitest'
import {
  buildEmptyStateCopy,
  getEmptyStateTone,
  shouldReduceMotion,
} from './EmptyState'

describe('EmptyState design primitive', () => {
  it('builds consistent copy for permission, empty, and error states', () => {
    expect(buildEmptyStateCopy('NO_DATA')).toEqual({
      title: 'No data yet',
      description: 'Create or adjust filters to populate this view.',
    })
    expect(buildEmptyStateCopy('NO_PERMISSION')).toEqual({
      title: 'Permission required',
      description: 'Ask an administrator for access to this workflow.',
    })
    expect(buildEmptyStateCopy('ERROR')).toEqual({
      title: 'Unable to load',
      description: 'Retry after checking the current filters and connection.',
    })
  })

  it('maps empty-state kinds to stable visual tones', () => {
    expect(getEmptyStateTone('NO_DATA')).toBe('neutral')
    expect(getEmptyStateTone('NO_PERMISSION')).toBe('warning')
    expect(getEmptyStateTone('ERROR')).toBe('danger')
  })

  it('honors reduced-motion preference', () => {
    expect(shouldReduceMotion(true)).toBe(true)
    expect(shouldReduceMotion(false)).toBe(false)
  })
})
```

- [ ] **Step 2: Run empty-state tests and confirm red state**

Run:

```bash
cd frontend && npm test -- EmptyState.test.tsx
```

Expected: FAIL because `EmptyState.tsx` does not exist.

- [ ] **Step 3: Add design tokens**

Create `frontend/src/design/tokens.css`:

```css
:root {
  --canvas-color-surface: #ffffff;
  --canvas-color-surface-muted: #f6f8fb;
  --canvas-color-text: #1f2937;
  --canvas-color-text-muted: #667085;
  --canvas-color-border: #d0d5dd;
  --canvas-color-focus: #1677ff;
  --canvas-space-sm: 8px;
  --canvas-space-md: 16px;
  --canvas-space-lg: 24px;
  --canvas-radius-card: 8px;
  --canvas-motion-fast: 120ms;
}

@media (prefers-reduced-motion: reduce) {
  :root {
    --canvas-motion-fast: 0ms;
  }
}
```

- [ ] **Step 4: Implement empty-state component**

Create `frontend/src/components/empty/EmptyState.tsx`:

```tsx
import { Empty, Button, type ButtonProps } from 'antd'
import '../../design/tokens.css'

export type EmptyStateKind = 'NO_DATA' | 'NO_PERMISSION' | 'ERROR'
export type EmptyStateTone = 'neutral' | 'warning' | 'danger'

export interface EmptyStateCopy {
  title: string
  description: string
}

export function buildEmptyStateCopy(kind: EmptyStateKind): EmptyStateCopy {
  if (kind === 'NO_PERMISSION') {
    return {
      title: 'Permission required',
      description: 'Ask an administrator for access to this workflow.',
    }
  }
  if (kind === 'ERROR') {
    return {
      title: 'Unable to load',
      description: 'Retry after checking the current filters and connection.',
    }
  }
  return {
    title: 'No data yet',
    description: 'Create or adjust filters to populate this view.',
  }
}

export function getEmptyStateTone(kind: EmptyStateKind): EmptyStateTone {
  if (kind === 'NO_PERMISSION') return 'warning'
  if (kind === 'ERROR') return 'danger'
  return 'neutral'
}

export function shouldReduceMotion(prefersReducedMotion: boolean): boolean {
  return prefersReducedMotion
}

export function EmptyState({ kind, action }: { kind: EmptyStateKind; action?: ButtonProps & { label: string } }) {
  const copy = buildEmptyStateCopy(kind)
  return (
    <div data-tone={getEmptyStateTone(kind)} style={{ padding: 'var(--canvas-space-lg)' }}>
      <Empty description={copy.description}>
        <strong>{copy.title}</strong>
        {action ? <Button {...action}>{action.label}</Button> : null}
      </Empty>
    </div>
  )
}
```

- [ ] **Step 5: Run empty-state tests**

Run:

```bash
cd frontend && npm test -- EmptyState.test.tsx
```

Expected: PASS.

### Task 3: Product Guide Primitive

**Files:**
- Create: `frontend/src/components/guides/ProductGuide.tsx`
- Create: `frontend/src/components/guides/ProductGuide.test.tsx`

- [ ] **Step 1: Write product-guide tests**

Create `frontend/src/components/guides/ProductGuide.test.tsx`:

```ts
import { describe, expect, it } from 'vitest'
import {
  buildGuideProgressLabel,
  getNextGuideStep,
  normalizeGuideSteps,
} from './ProductGuide'

describe('ProductGuide helpers', () => {
  it('normalizes guide steps by order and removes disabled steps', () => {
    expect(normalizeGuideSteps([
      { key: 'finish', title: 'Finish', order: 3, enabled: true },
      { key: 'skip', title: 'Skip', order: 2, enabled: false },
      { key: 'start', title: 'Start', order: 1, enabled: true },
    ])).toEqual([
      { key: 'start', title: 'Start', order: 1, enabled: true },
      { key: 'finish', title: 'Finish', order: 3, enabled: true },
    ])
  })

  it('builds progress labels', () => {
    expect(buildGuideProgressLabel(0, 3)).toBe('Step 1 of 3')
  })

  it('returns the next step or undefined at the end', () => {
    const steps = normalizeGuideSteps([
      { key: 'start', title: 'Start', order: 1, enabled: true },
      { key: 'finish', title: 'Finish', order: 2, enabled: true },
    ])
    expect(getNextGuideStep(steps, 'start')?.key).toBe('finish')
    expect(getNextGuideStep(steps, 'finish')).toBeUndefined()
  })
})
```

- [ ] **Step 2: Run product-guide tests and confirm red state**

Run:

```bash
cd frontend && npm test -- ProductGuide.test.tsx
```

Expected: FAIL because `ProductGuide.tsx` does not exist.

- [ ] **Step 3: Implement product guide component**

Create `frontend/src/components/guides/ProductGuide.tsx`:

```tsx
import { Tour, type TourProps } from 'antd'

export interface ProductGuideStep {
  key: string
  title: string
  order: number
  enabled: boolean
  description?: string
}

export function normalizeGuideSteps(steps: ProductGuideStep[]): ProductGuideStep[] {
  return steps
    .filter((step) => step.enabled)
    .sort((left, right) => left.order - right.order)
}

export function buildGuideProgressLabel(currentIndex: number, total: number): string {
  return `Step ${currentIndex + 1} of ${total}`
}

export function getNextGuideStep(steps: ProductGuideStep[], currentKey: string): ProductGuideStep | undefined {
  const currentIndex = steps.findIndex((step) => step.key === currentKey)
  return currentIndex >= 0 ? steps[currentIndex + 1] : undefined
}

export function ProductGuide({ steps, open, onClose }: { steps: ProductGuideStep[]; open: boolean; onClose: () => void }) {
  const normalized = normalizeGuideSteps(steps)
  const tourSteps: TourProps['steps'] = normalized.map((step, index) => ({
    title: step.title,
    description: `${buildGuideProgressLabel(index, normalized.length)}. ${step.description || ''}`.trim(),
  }))
  return <Tour open={open} steps={tourSteps} onClose={onClose} />
}
```

- [ ] **Step 4: Run product-guide tests**

Run:

```bash
cd frontend && npm test -- ProductGuide.test.tsx
```

Expected: PASS.

### Task 4: Verification, Rollout Notes, And Commit

**Files:**
- Modify: `docs/product-evolution/specs/p2-014-design-system-guided-experience.md`
- Modify: `docs/product-evolution/plans/p2-014-design-system-guided-experience-plan.md`

- [ ] **Step 1: Run focused backend verification**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=SystemOptionServiceTest
```

Expected: PASS.

- [ ] **Step 2: Run focused frontend verification**

Run:

```bash
cd frontend && npm test -- EmptyState.test.tsx ProductGuide.test.tsx
```

Expected: PASS.

- [ ] **Step 3: Run broad regression gates**

Run:

```bash
(cd backend && mvn -pl canvas-engine test)
(cd frontend && npm test -- --run)
(cd frontend && npm run build)
```

Expected: PASS for backend module tests, Vitest, and Vite build.

- [ ] **Step 4: Add rollout notes to the implementation PR**

Use this text in the PR:

```markdown
Rollout notes:
- Feature flag: enable new empty-state and product-guide components per route after visual smoke checks.
- Migration: no Flyway migration in this slice; design tokens are CSS and backend options use existing `system_option` storage.
- Tenant and role impact: content style and form pattern options are read with tenant-scoped `SystemOptionService.activeOptions`.
- Manual verification: open an empty list, a permission-denied workflow, and one guided tour with reduced-motion enabled at OS level.
- Rollback: remove component imports or route-level flags; no data rollback is required.
```

- [ ] **Step 5: Commit this slice**

Run:

```bash
git add backend/canvas-engine/src/main/java/org/chovy/canvas/domain/meta/SystemOptionService.java \
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/meta/SystemOptionServiceTest.java \
  frontend/src/design/tokens.css \
  frontend/src/components/empty/EmptyState.tsx \
  frontend/src/components/empty/EmptyState.test.tsx \
  frontend/src/components/guides/ProductGuide.tsx \
  frontend/src/components/guides/ProductGuide.test.tsx \
  docs/product-evolution/specs/p2-014-design-system-guided-experience.md \
  docs/product-evolution/plans/p2-014-design-system-guided-experience-plan.md
git commit -m "feat: add design system guided experience plan"
```

Expected: commit contains only P2-014 implementation files and matching spec/plan documentation.
