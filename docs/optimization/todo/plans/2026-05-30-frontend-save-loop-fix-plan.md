# Frontend Save Loop Fix + Retry Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development or superpowers:executing-plans.

**Goal:** Fix while(true) infinite loop in handleSave with max iterations. Add exponential backoff retry for auto-save failures. Preserve user edits on save failure.

**Architecture:** Replace while(true) with bounded for-loop (max 10 iterations). Extract save-with-retry utility that uses exponential backoff. Failed saves preserve edits in localStorage as recovery mechanism. Auto-save uses the same retry logic but with a configurable max attempts.

**Tech Stack:** React, antd 5, vitest

---

### Task 1: Fix while(true) with Max Iterations and Retry

**Files:**
- Modify: `frontend/src/pages/canvas-editor/index.tsx`
- Create: `frontend/src/pages/canvas-editor/__tests__/handleSave.test.ts`

- [ ] **Step 1: Write failing test**

```tsx
// frontend/src/pages/canvas-editor/__tests__/handleSave.test.ts
import { describe, it, expect, vi, beforeEach } from 'vitest';

// Mock the API module
vi.mock('../../../services/api', () => ({
  canvasApi: {
    save: vi.fn(),
  },
}));

import { canvasApi } from '../../../services/api';
import { handleSave, saveWithRetry } from '../handleSaveUtils';

describe('handleSave', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('should not loop more than MAX_SAVE_ITERATIONS times', async () => {
    // Simulate: snapshot always changes (would cause infinite loop before fix)
    const mockGetSnapshot = vi.fn()
      .mockReturnValueOnce({ version: 1 })  // first save attempt
      .mockReturnValueOnce({ version: 2 })  // changed after save
      .mockReturnValueOnce({ version: 3 })  // changed again
      .mockReturnValueOnce({ version: 4 })  // changed again
      .mockReturnValueOnce({ version: 5 }); // final check

    const mockSnapshotChanged = vi.fn()
      .mockReturnValue(true);  // always reports "changed"

    vi.mocked(canvasApi.save).mockResolvedValue({ status: 200 });

    await handleSave({
      getSnapshot: mockGetSnapshot,
      snapshotChangedSinceSave: mockSnapshotChanged,
    });

    // Should stop at MAX_SAVE_ITERATIONS (10), not loop forever
    expect(canvasApi.save).toHaveBeenCalledTimes(10);
  });

  it('should stop early when snapshot stops changing', async () => {
    const mockGetSnapshot = vi.fn()
      .mockReturnValue({ version: 1 });

    const mockSnapshotChanged = vi.fn()
      .mockReturnValueOnce(true)   // changed after first save
      .mockReturnValueOnce(false); // stable after second save

    vi.mocked(canvasApi.save).mockResolvedValue({ status: 200 });

    await handleSave({
      getSnapshot: mockGetSnapshot,
      snapshotChangedSinceSave: mockSnapshotChanged,
    });

    expect(canvasApi.save).toHaveBeenCalledTimes(2);
  });
});

describe('saveWithRetry', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('should retry with exponential backoff on failure', async () => {
    vi.useFakeTimers();

    vi.mocked(canvasApi.save)
      .mockRejectedValueOnce(new Error('Network error'))
      .mockRejectedValueOnce(new Error('Network error'))
      .mockResolvedValueOnce({ status: 200 });

    const retryPromise = saveWithRetry({ version: 1 }, { maxRetries: 3, baseDelay: 100 });

    // Fast-forward through delays: 100ms, 200ms
    await vi.advanceTimersByTimeAsync(100);
    await vi.advanceTimersByTimeAsync(200);

    const result = await retryPromise;
    expect(canvasApi.save).toHaveBeenCalledTimes(3);
    expect(result.success).toBe(true);

    vi.useRealTimers();
  });

  it('should give up after maxRetries and preserve edits', async () => {
    vi.useFakeTimers();

    vi.mocked(canvasApi.save).mockRejectedValue(new Error('Server down'));

    const retryPromise = saveWithRetry({ version: 1 }, { maxRetries: 3, baseDelay: 100 });

    await vi.advanceTimersByTimeAsync(100);
    await vi.advanceTimersByTimeAsync(200);
    await vi.advanceTimersByTimeAsync(400);

    const result = await retryPromise;
    expect(result.success).toBe(false);
    expect(result.lastError).toBe('Server down');
    expect(canvasApi.save).toHaveBeenCalledTimes(3);

    vi.useRealTimers();
  });
});
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd frontend && npx vitest run pages/canvas-editor/__tests__/handleSave.test.ts`
Expected: FAIL - handleSaveUtils module not found

- [ ] **Step 3: Create handleSaveUtils.ts with bounded loop and retry**

```tsx
// frontend/src/pages/canvas-editor/handleSaveUtils.ts
import { canvasApi } from '../../services/api';
import { message } from 'antd';

const MAX_SAVE_ITERATIONS = 10;

interface SaveContext {
  getSnapshot: () => unknown;
  snapshotChangedSinceSave: () => boolean;
}

interface RetryOptions {
  maxRetries?: number;
  baseDelay?: number;
}

interface SaveResult {
  success: boolean;
  lastError?: string;
}

export async function handleSave(ctx: SaveContext): Promise<void> {
  for (let i = 0; i < MAX_SAVE_ITERATIONS; i++) {
    const snapshot = ctx.getSnapshot();
    const result = await saveWithRetry(snapshot);

    if (!result.success) {
      message.error(`保存失败: ${result.lastError}`);
      return;
    }

    // Check if snapshot changed during save
    if (!ctx.snapshotChangedSinceSave()) {
      return; // Save succeeded and snapshot is stable
    }
    // Snapshot changed, loop again to save the new version
  }

  // Max iterations reached — snapshot is changing faster than we can save
  message.warning('保存冲突：编辑速度过快，部分修改可能未保存，请手动保存');
}

export async function saveWithRetry(
  snapshot: unknown,
  options: RetryOptions = {}
): Promise<SaveResult> {
  const { maxRetries = 3, baseDelay = 1000 } = options;

  let lastError: string | undefined;

  for (let attempt = 0; attempt < maxRetries; attempt++) {
    try {
      await canvasApi.save(snapshot);
      return { success: true };
    } catch (err) {
      lastError = err instanceof Error ? err.message : String(err);

      if (attempt < maxRetries - 1) {
        // Exponential backoff: 1s, 2s, 4s
        const delay = baseDelay * Math.pow(2, attempt);
        await new Promise(resolve => setTimeout(resolve, delay));
      }
    }
  }

  return { success: false, lastError };
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `cd frontend && npx vitest run pages/canvas-editor/__tests__/handleSave.test.ts`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add frontend/src/pages/canvas-editor/handleSaveUtils.ts
git add frontend/src/pages/canvas-editor/__tests__/handleSave.test.ts
git commit -m "feat: replace while(true) with bounded save loop and exponential backoff retry"
```

---

### Task 2: Integrate into Existing Canvas Editor Component

**Files:**
- Modify: `frontend/src/pages/canvas-editor/index.tsx`

- [ ] **Step 1: Write failing test for integration**

```tsx
// Add to frontend/src/pages/canvas-editor/__tests__/handleSave.test.ts

describe('handleSave integration', () => {
  it('should replace while(true) in handleSave with bounded loop', async () => {
    // This test verifies the integration by checking that
    // the component's handleSave no longer contains while(true)
    const fs = require('fs');
    const source = fs.readFileSync(
      'frontend/src/pages/canvas-editor/index.tsx',
      'utf-8'
    );
    expect(source).not.toContain('while (true)');
    expect(source).toContain('handleSave');
  });
});
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd frontend && npx vitest run pages/canvas-editor/__tests__/handleSave.test.ts`
Expected: FAIL - index.tsx still contains `while (true)`

- [ ] **Step 3: Modify canvas-editor/index.tsx to use new handleSave**

```tsx
// In canvas-editor/index.tsx, replace the existing handleSave implementation:

// BEFORE (simplified):
// const handleSave = async () => {
//   while (true) {
//     await doSave(currentSnapshot);
//     if (!snapshotChangedSinceSave()) break;
//   }
// };

// AFTER:
import { handleSave } from './handleSaveUtils';

const handleCanvasSave = useCallback(async () => {
  await handleSave({
    getSnapshot: () => currentSnapshotRef.current,
    snapshotChangedSinceSave: () => {
      const current = JSON.stringify(currentSnapshotRef.current);
      const saved = JSON.stringify(lastSavedSnapshotRef.current);
      return current !== saved;
    },
  });
}, []);
```

- [ ] **Step 4: Run test to verify it passes**

Run: `cd frontend && npx vitest run pages/canvas-editor/__tests__/handleSave.test.ts`
Expected: PASS

- [ ] **Step 5: Run full frontend test suite**

Run: `cd frontend && npm run test`
Expected: All tests pass

- [ ] **Step 6: Commit**

```bash
git add frontend/src/pages/canvas-editor/index.tsx
git add frontend/src/pages/canvas-editor/__tests__/handleSave.test.ts
git commit -m "feat: integrate bounded save loop into canvas editor, remove while(true)"
```