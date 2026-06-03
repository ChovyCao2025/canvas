# Context Memory Limit + Key Namespace Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development or superpowers:executing-plans.

**Goal:** Enforce isOversized() check at putNodeOutput entry, add namespace prefix to flatContext keys to prevent output collisions, improve size estimation.

**Architecture:** ExecutionContext.putNodeOutput becomes the single enforcement point: check isOversized() before accepting data, throw ContextOverflowException if over limit. flatContext keys change from raw field name to namespaced "nodeId.fieldName" format, with putNodeOutput/getNodeOutput managing the namespace transparently.

**Tech Stack:** Java 21, ConcurrentHashMap, AtomicInteger

---

### Task 1: Add Memory Limit Enforcement + Eviction to ExecutionContext

**Files:**
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/context/ExecutionContext.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/context/ContextOverflowException.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/context/ExecutionContextMemoryLimitTest.java`

- [ ] **Step 1: Write failing test for memory limit enforcement**

```java
package org.chovy.canvas.engine.context;

import org.chovy.canvas.engine.context.ContextOverflowException;
import org.chovy.canvas.engine.context.NodeStatus;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ExecutionContextMemoryLimitTest {

    @Test
    void testPutNodeOutput_rejectsWhenOversized() {
        ExecutionContext ctx = new ExecutionContext();
        ctx.setExecutionId("exec-001");

        // Fill context to exceed the 1MB limit (5 x 200KB = 1MB+)
        String largeValue = "x".repeat(200_000);
        for (int i = 0; i < 5; i++) {
            ctx.putNodeOutput("node-" + i, Map.of("data", largeValue));
        }
        // Now ctx should be over 1MB
        assertThat(ctx.isOversized()).isTrue();

        // Next putNodeOutput should throw ContextOverflowException
        assertThatThrownBy(() -> ctx.putNodeOutput("node-overflow", Map.of("key", "value")))
                .isInstanceOf(ContextOverflowException.class)
                .hasMessageContaining("exceeds 1MB limit");
    }

    @Test
    void testPutNodeOutput_acceptsWhenUnderLimit() {
        ExecutionContext ctx = new ExecutionContext();
        ctx.setExecutionId("exec-001");

        // Small output should be accepted
        ctx.putNodeOutput("node-A", Map.of("result", "ok"));
        assertThat(ctx.isOversized()).isFalse();
        assertThat(ctx.getContextValue("node-A.result")).isEqualTo("ok");
    }

    @Test
    void testPutNodeOutput_evictsOldestNodeWhenOverLimit() {
        ExecutionContext ctx = new ExecutionContext();
        ctx.setExecutionId("exec-001");

        // Fill with 5 x 200KB nodes to approach the 1MB limit
        String mediumValue = "y".repeat(200_000);
        for (int i = 0; i < 5; i++) {
            ctx.putNodeOutput("node-" + i, Map.of("data", mediumValue));
        }

        // Add one more 200KB node to trigger eviction (6 x 200KB = 1.2MB > 1MB)
        ctx.putNodeOutput("node-overflow", Map.of("bigdata", mediumValue));

        // After eviction, oldest node outputs should be removed from flatContext
        // but nodeOutputs map retains the per-node snapshot
        assertThat(ctx.isOversized()).isFalse();
    }

    @Test
    void testApproxSizeBytes_increasesOnPut() {
        ExecutionContext ctx = new ExecutionContext();
        ctx.setExecutionId("exec-001");

        int sizeBefore = ctx.getApproxSizeBytes();
        ctx.putNodeOutput("node-A", Map.of("key1", "value1"));
        int sizeAfter = ctx.getApproxSizeBytes();

        assertThat(sizeAfter).isGreaterThan(sizeBefore);
        // "key1" (4) + "value1" (6) = 10 bytes minimum
        assertThat(sizeAfter - sizeBefore).isGreaterThanOrEqualTo(10);
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

```bash
cd backend && mvn test -pl canvas-engine -Dtest=ExecutionContextMemoryLimitTest -DfailIfNoTests=false 2>&1 | tail -30
```

Expected output:
```
[ERROR] Tests run: 4, Failures: 4, Errors: 0, Skipped: 0
[ERROR] ... ContextOverflowException class not found
```

- [ ] **Step 3: Create ContextOverflowException**

Create `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/context/ContextOverflowException.java`:

```java
package org.chovy.canvas.engine.context;

/**
 * Thrown when ExecutionContext exceeds the 1MB memory limit.
 * Prevents OOM by rejecting writes that would push the context over size.
 */
public class ContextOverflowException extends RuntimeException {

    private final int currentSizeBytes;

    public ContextOverflowException(String message, int currentSizeBytes) {
        super(message);
        this.currentSizeBytes = currentSizeBytes;
    }

    public int getCurrentSizeBytes() {
        return currentSizeBytes;
    }
}
```

- [ ] **Step 4: Modify putNodeOutput in ExecutionContext to enforce limit and add eviction**

Replace the existing `putNodeOutput` method in `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/context/ExecutionContext.java` with:

```java
    /** 写入或记录 put Node Output 相关的业务数据。
     *
     * <p>Enforces memory limit: if context is already oversized, throws ContextOverflowException.
     * If writing this output would push context over limit, evicts oldest node outputs first.
     * Keys are namespaced as "nodeId.fieldName" in flatContext to prevent collisions.
     *
     * @param nodeId nodeId 对应的业务主键或标识
     * @param output output 方法执行所需的业务参数
     * @throws ContextOverflowException if context is already oversized and eviction cannot free enough space
     */
    public void putNodeOutput(String nodeId, Map<String, Object> output) {
        // Enforcement: reject writes when already oversized
        if (isOversized()) {
            // Attempt eviction before rejecting
            evictOldestNodes();
            if (isOversized()) {
                throw new ContextOverflowException(
                        "Context exceeds 1MB limit: " + approxSizeBytes.get() + " bytes. " +
                        "Node output rejected for nodeId=" + nodeId,
                        approxSizeBytes.get());
            }
        }

        // Estimate size of incoming output
        int incomingSize = 0;
        for (Map.Entry<String, Object> entry : output.entrySet()) {
            String namespacedKey = nodeId + "." + entry.getKey();
            incomingSize += namespacedKey.length() + (entry.getValue() != null ? entry.getValue().toString().length() : 4);
        }

        // Check if adding this output would push over limit
        if (approxSizeBytes.get() + incomingSize > MAX_SIZE_BYTES) {
            evictOldestNodes();
        }

        // nodeOutputs retains per-node snapshot (not namespaced)
        nodeOutputs.put(nodeId, output);

        // flatContext uses namespaced keys to prevent collisions
        output.forEach((k, v) -> {
            String namespacedKey = nodeId + "." + k;
            Object previous = flatContext.put(namespacedKey, v);
            // If key already existed from same node, subtract old value size
            if (previous != null) {
                approxSizeBytes.addAndGet(-(namespacedKey.length() + previous.toString().length()));
            }
        });

        // Size monitoring: accumulate estimated bytes
        output.forEach((k, v) ->
            approxSizeBytes.addAndGet((nodeId.length() + 1 + k.length()) + (v != null ? v.toString().length() : 4)));

        if (approxSizeBytes.get() > MAX_SIZE_BYTES) {
            // After write, if still over limit, evict
            evictOldestNodes();
        } else if (approxSizeBytes.get() > WARN_SIZE_BYTES) {
            // over 512KB warning threshold
        }
    }

    /**
     * Evict oldest node outputs when context exceeds memory limit.
     * Removes entries from both nodeOutputs and flatContext, starting from the first inserted node.
     */
    private void evictOldestNodes() {
        java.util.Iterator<Map.Entry<String, Map<String, Object>>> it = nodeOutputs.entrySet().iterator();
        while (it.hasNext() && isOversized()) {
            Map.Entry<String, Map<String, Object>> entry = it.next();
            String nodeId = entry.getKey();
            Map<String, Object> output = entry.getValue();
            // Remove namespaced keys from flatContext
            output.forEach((k, v) -> {
                String namespacedKey = nodeId + "." + k;
                flatContext.remove(namespacedKey);
                approxSizeBytes.addAndGet(-(namespacedKey.length() + (v != null ? v.toString().length() : 4)));
            });
            it.remove();
        }
    }
```

- [ ] **Step 5: Run test to verify it passes**

```bash
cd backend && mvn test -pl canvas-engine -Dtest=ExecutionContextMemoryLimitTest -DfailIfNoTests=false 2>&1 | tail -20
```

Expected output:
```
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

- [ ] **Step 6: Commit**

```bash
git add backend/canvas-engine/src/main/java/org/chovy/canvas/engine/context/ExecutionContext.java backend/canvas-engine/src/main/java/org/chovy/canvas/engine/context/ContextOverflowException.java backend/canvas-engine/src/test/java/org/chovy/canvas/engine/context/ExecutionContextMemoryLimitTest.java && git commit -m "feat: enforce isOversized() at putNodeOutput with eviction and ContextOverflowException"
```

---

### Task 2: Add Key Namespace to Prevent Output Collisions

**Files:**
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/context/ExecutionContext.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/context/ExecutionContextNamespaceTest.java`

- [ ] **Step 1: Write failing test for key namespace isolation**

```java
package org.chovy.canvas.engine.context;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ExecutionContextNamespaceTest {

    @Test
    void testPutNodeOutput_namespacedKeysPreventCollision() {
        ExecutionContext ctx = new ExecutionContext();
        ctx.setExecutionId("exec-001");

        // Two nodes output the same key "result" with different values
        ctx.putNodeOutput("node-A", Map.of("result", "value-from-A"));
        ctx.putNodeOutput("node-B", Map.of("result", "value-from-B"));

        // Both values should be retrievable via namespaced access
        assertThat(ctx.getNodeOutput("node-A", "result")).isEqualTo("value-from-A");
        assertThat(ctx.getNodeOutput("node-B", "result")).isEqualTo("value-from-B");

        // flatContext should have both namespaced keys
        assertThat(ctx.getFlatContext().get("node-A.result")).isEqualTo("value-from-A");
        assertThat(ctx.getFlatContext().get("node-B.result")).isEqualTo("value-from-B");
    }

    @Test
    void testGetNodeOutput_returnsLatestNodeValueForSameKey() {
        ExecutionContext ctx = new ExecutionContext();
        ctx.setExecutionId("exec-001");

        ctx.putNodeOutput("node-A", Map.of("status", "pending"));
        ctx.putNodeOutput("node-B", Map.of("status", "completed"));

        // getNodeOutput(nodeId, key) returns the specific node's output
        assertThat(ctx.getNodeOutput("node-A", "status")).isEqualTo("pending");
        assertThat(ctx.getNodeOutput("node-B", "status")).isEqualTo("completed");

        // getContextValue returns the last-written value (backward compat)
        Object latest = ctx.getContextValue("status");
        assertThat(latest).isEqualTo("completed");
    }

    @Test
    void testNodeOutput_overwriteSameNodeUpdatesBothMaps() {
        ExecutionContext ctx = new ExecutionContext();
        ctx.setExecutionId("exec-001");

        ctx.putNodeOutput("node-A", Map.of("count", "1"));
        ctx.putNodeOutput("node-A", Map.of("count", "2"));

        // nodeOutputs map holds the latest for node-A
        assertThat(ctx.getNodeOutput("node-A", "count")).isEqualTo("2");
        // flatContext also updated
        assertThat(ctx.getFlatContext().get("node-A.count")).isEqualTo("2");
    }

    @Test
    void testNamespacedKey_formatIsNodeIdDotFieldName() {
        ExecutionContext ctx = new ExecutionContext();
        ctx.setExecutionId("exec-001");

        ctx.putNodeOutput("send-sms-1", Map.of("phone", "13800138000", "status", "sent"));

        // Verify namespaced keys exist in flatContext
        assertThat(ctx.getFlatContext()).containsKey("send-sms-1.phone");
        assertThat(ctx.getFlatContext()).containsKey("send-sms-1.status");

        // Original bare keys should NOT exist (collision prevented)
        assertThat(ctx.getFlatContext()).doesNotContainKey("phone");
        assertThat(ctx.getFlatContext()).doesNotContainKey("status");
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

```bash
cd backend && mvn test -pl canvas-engine -Dtest=ExecutionContextNamespaceTest -DfailIfNoTests=false 2>&1 | tail -30
```

Expected output:
```
[ERROR] Tests run: 4, Failures: 4, Errors: 0, Skipped: 0
[ERROR] ... getNodeOutput(String, String) method not found, flatContext does not contain namespaced keys
```

- [ ] **Step 3: Add getNodeOutput method to ExecutionContext**

Add the following method to `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/context/ExecutionContext.java`:

```java
    /**
     * Get a specific output field from a specific node (namespaced access).
     * This is the preferred way to read node outputs — no collision risk.
     *
     * @param nodeId  the node that produced the output
     * @param fieldKey the output field key
     * @return the value, or null if not found
     */
    public Object getNodeOutput(String nodeId, String fieldKey) {
        String namespacedKey = nodeId + "." + fieldKey;
        return flatContext.get(namespacedKey);
    }
```

Note: `getFlatContext()` already exists via Lombok @Getter on the `flatContext` field — no need to add it.
```

- [ ] **Step 4: Update getContextValue to support namespaced lookup with backward compatibility**

Replace the existing `getContextValue` method in ExecutionContext:

```java
    /**
     * Read a context value by field key.
     *
     * <p>Lookup order:
     * 1. Exact namespaced key match in flatContext (e.g., "node-A.result")
     * 2. Bare key match across all node outputs (latest wins, backward compat)
     * 3. Trigger payload fallback
     *
     * @param fieldKey the field key to look up (can be namespaced "nodeId.key" or bare "key")
     * @return the value, or null if not found
     */
    public Object getContextValue(String fieldKey) {
        // 1. Try exact match first (handles both namespaced and bare keys)
        Object val = flatContext.get(fieldKey);
        if (val != null) return val;

        // 2. Bare key: scan all node outputs for the latest value (backward compat)
        //    This handles legacy code that calls getContextValue("result") without nodeId prefix
        if (!fieldKey.contains(".")) {
            Object latest = null;
            for (Map.Entry<String, Map<String, Object>> entry : nodeOutputs.entrySet()) {
                Object candidate = entry.getValue().get(fieldKey);
                if (candidate != null) {
                    latest = candidate;
                }
            }
            if (latest != null) return latest;
        }

        // 3. Trigger payload fallback
        return triggerPayload.get(fieldKey);
    }
```

- [ ] **Step 5: Run test to verify it passes**

```bash
cd backend && mvn test -pl canvas-engine -Dtest=ExecutionContextNamespaceTest -DfailIfNoTests=false 2>&1 | tail -20
```

Expected output:
```
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

- [ ] **Step 6: Run existing concurrency test to ensure no regression**

```bash
cd backend && mvn test -pl canvas-engine -Dtest=ExecutionContextConcurrencyTest -DfailIfNoTests=false 2>&1 | tail -20
```

Expected output:
```
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

Note: The existing concurrency test uses `ctx.getContextValue("key-" + i)` with bare keys. With namespaced keys, flatContext now stores "node-i.key-i" format. The backward-compatible `getContextValue` bare key scan will find these values. However, the test also does `ctx.putNodeOutput("node-" + idx, Map.of("key-" + idx, "val-" + idx))` which now stores "node-idx.key-idx" in flatContext. The backward compat scan iterates nodeOutputs entries to find bare key matches, so this should pass.

- [ ] **Step 7: Commit**

```bash
git add backend/canvas-engine/src/main/java/org/chovy/canvas/engine/context/ExecutionContext.java backend/canvas-engine/src/test/java/org/chovy/canvas/engine/context/ExecutionContextNamespaceTest.java && git commit -m "feat: add namespaced flatContext keys (nodeId.fieldName) and getNodeOutput(nodeId, fieldKey) to prevent output collisions"
```
