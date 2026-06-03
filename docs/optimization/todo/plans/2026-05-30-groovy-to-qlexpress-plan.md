# Groovy → QLExpress Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development or superpowers:executing-plans.

**Goal:** Replace Groovy 4.0.21 + SecureASTCustomizer with QLExpress (already in pom.xml) for simple condition logic. Eliminate ClassLoader leaks, Metaspace OOM risk, and unreliable timeout.

> **API Verified:** The project uses QLExpress 3.3.3 (`com.ql.util.express:*`). The API is:
> - `ExpressRunner` (NOT `Express4Runner`), package `com.ql.util.express.*`
> - Constructor: `new ExpressRunner()` (no args)
> - Context: `DefaultContext<String, Object>` implementing `IExpressContext<String, Object>`
> - Execute: `runner.execute(expression, context, errorList, isCache, isTrace, timeoutMillis)` returning `Object`
> - Timeout exception: `com.ql.util.express.exception.QLTimeoutException`
> - NO `QLOptions`, NO `InitOptions`, NO `.builder()` pattern

**Architecture:** QLExpressHandler implements NodeHandler with @NodeHandlerType("CONDITION_EXPRESS"). Uses ExpressRunner with DefaultContext for variable injection and 6-arg execute() with timeoutMillis for reliable timeout. GroovyHandler kept for migration period (existing scripts still use "CONDITION_GROOVY" type).

**Tech Stack:** QLExpress 3.3.3 ExpressRunner, Java 21, DefaultContext

---

### Task 1: Implement QLExpressHandler

**Files:**
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handlers/QLExpressHandler.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/handlers/QLExpressHandlerTest.java`

- [ ] **Step 1: Write failing test**

```java
package org.chovy.canvas.engine.handlers;

import org.chovy.canvas.engine.context.ExecutionContext;
import org.chovy.canvas.engine.handler.NodeConfig;
import org.chovy.canvas.engine.handler.NodeResult;
import org.chovy.canvas.engine.handler.NodeOutcome;
import org.junit.jupiter.api.Test;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class QLExpressHandlerTest {

    @Test
    void execute_simpleCondition_true() {
        QLExpressHandler handler = new QLExpressHandler();
        Map<String, Object> config = Map.of("expression", "age > 18 && city == '北京'", "timeoutMs", 5000);
        ExecutionContext ctx = mock(ExecutionContext.class);
        Map<String, Object> flatContext = new ConcurrentHashMap<>();
        flatContext.put("age", 25);
        flatContext.put("city", "北京");
        when(ctx.getFlatContext()).thenReturn(flatContext);

        NodeResult result = handler.executeAsync(config, ctx).block();

        assertThat(result.outcome()).isEqualTo(NodeOutcome.SUCCESS);
        assertThat(result.success()).isTrue();
    }

    @Test
    void execute_simpleCondition_false() {
        QLExpressHandler handler = new QLExpressHandler();
        Map<String, Object> config = Map.of("expression", "age > 18 && city == '北京'", "timeoutMs", 5000);
        ExecutionContext ctx = mock(ExecutionContext.class);
        Map<String, Object> flatContext = new ConcurrentHashMap<>();
        flatContext.put("age", 15);
        flatContext.put("city", "上海");
        when(ctx.getFlatContext()).thenReturn(flatContext);

        NodeResult result = handler.executeAsync(config, ctx).block();

        assertThat(result.outcome()).isEqualTo(NodeOutcome.SUCCESS);
        assertThat(result.success()).isTrue();
    }

    @Test
    void execute_timeout_returnsTimeoutResult() {
        QLExpressHandler handler = new QLExpressHandler();
        Map<String, Object> config = Map.of("expression", "while(true){1+1}", "timeoutMs", 1000);
        ExecutionContext ctx = mock(ExecutionContext.class);
        when(ctx.getFlatContext()).thenReturn(new ConcurrentHashMap<>());

        NodeResult result = handler.executeAsync(config, ctx).block();

        assertThat(result.outcome()).isEqualTo(NodeOutcome.TIMEOUT);
    }

    @Test
    void execute_invalidExpression_returnsFailResult() {
        QLExpressHandler handler = new QLExpressHandler();
        Map<String, Object> config = Map.of("expression", "undefined_var + ??", "timeoutMs", 5000);
        ExecutionContext ctx = mock(ExecutionContext.class);
        when(ctx.getFlatContext()).thenReturn(new ConcurrentHashMap<>());

        NodeResult result = handler.executeAsync(config, ctx).block();

        assertThat(result.outcome()).isEqualTo(NodeOutcome.FAIL);
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd backend && mvn test -pl canvas-engine -Dtest=QLExpressHandlerTest -v`
Expected: FAIL - QLExpressHandler class not found

- [ ] **Step 3: Implement QLExpressHandler**

```java
package org.chovy.canvas.engine.handlers;

import com.ql.util.express.DefaultContext;
import com.ql.util.express.ExpressRunner;
import com.ql.util.express.exception.QLTimeoutException;
import lombok.extern.slf4j.Slf4j;
import org.chovy.canvas.engine.context.ExecutionContext;
import org.chovy.canvas.engine.handler.NodeConfig;
import org.chovy.canvas.engine.handler.NodeResult;
import org.chovy.canvas.engine.handler.NodeOutcome;
import org.chovy.canvas.engine.handler.NodeHandler;
import org.chovy.canvas.engine.handler.NodeHandlerType;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@NodeHandlerType("CONDITION_EXPRESS")
public class QLExpressHandler implements NodeHandler {

    private static final int DEFAULT_TIMEOUT_MS = 5000;

    private final ExpressRunner runner;

    public QLExpressHandler() {
        this.runner = new ExpressRunner();
    }

    @Override
    public Mono<NodeResult> executeAsync(Map<String, Object> config, ExecutionContext ctx) {
        String expression = (String) config.get("expression");
        int timeoutMs = config.get("timeoutMs") instanceof Number n ? n.intValue() : DEFAULT_TIMEOUT_MS;

        return Mono.fromCallable(() -> {
            try {
                DefaultContext<String, Object> context = new DefaultContext<>();
                // Copy variables from ExecutionContext into QLExpress context
                context.putAll(ctx.getFlatContext());

                List<String> errorList = new ArrayList<>();
                Object result = runner.execute(expression, context, errorList, true, false, timeoutMs);

                if (!errorList.isEmpty()) {
                    log.error("QLExpress evaluation errors: {}", errorList);
                    return NodeResult.fail("Expression error: " + String.join("; ", errorList));
                }

                boolean condition = Boolean.TRUE.equals(result);
                return NodeResult.ifResult(condition, "success", "fail");

            } catch (QLTimeoutException e) {
                log.warn("QLExpress timeout after {}ms: {}", timeoutMs, expression);
                return NodeResult.timeout("CONDITION_EXPRESS", "TIMEOUT",
                        "Expression evaluation timeout after " + timeoutMs + "ms");
            } catch (Exception e) {
                // QLExpress evaluation errors may surface as generic exceptions.
                // Detect timeout by message content as fallback; treat everything else as FAIL.
                if (e.getMessage() != null && e.getMessage().toLowerCase().contains("timeout")) {
                    log.warn("QLExpress timeout after {}ms: {}", timeoutMs, expression);
                    return NodeResult.timeout("CONDITION_EXPRESS", "TIMEOUT",
                            "Expression evaluation timeout after " + timeoutMs + "ms");
                }
                log.error("QLExpress evaluation error: {}", e.getMessage());
                return NodeResult.fail("Expression error: " + e.getMessage());
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `cd backend && mvn test -pl canvas-engine -Dtest=QLExpressHandlerTest -v`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handlers/QLExpressHandler.java
git add backend/canvas-engine/src/test/java/org/chovy/canvas/engine/handlers/QLExpressHandlerTest.java
git commit -m "feat: implement QLExpressHandler with ExpressRunner 3.3.3 and reliable timeout"
```

---

### Task 2: Register Handler and Update Frontend Node Type

**Files:**
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handler/HandlerRegistry.java`
- Modify: `frontend/src/pages/canvas-editor/components/NodeTypeConfig.tsx`

- [ ] **Step 1: Write failing test for handler registry**

```java
// Add to existing HandlerRegistryTest.java or create new test

@Test
void registry_containsQLExpressHandler() {
    NodeHandler handler = handlerRegistry.get("CONDITION_EXPRESS");
    assertThat(handler).isNotNull();
    assertThat(handler).isInstanceOf(QLExpressHandler.class);
}

@Test
void registry_stillContainsGroovyHandler_forMigration() {
    NodeHandler handler = handlerRegistry.get("CONDITION_GROOVY");
    assertThat(handler).isNotNull();
    assertThat(handler).isInstanceOf(GroovyHandler.class);
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd backend && mvn test -pl canvas-engine -Dtest=HandlerRegistryTest -v`
Expected: FAIL - CONDITION_EXPRESS not registered

- [ ] **Step 3: Verify @NodeHandlerType auto-registration works**

QLExpressHandler already has `@NodeHandlerType("CONDITION_EXPRESS")` annotation from Task 1.
The HandlerRegistry should auto-discover it via component scan.

Run: `cd backend && mvn test -pl canvas-engine -Dtest=HandlerRegistryTest -v`
Expected: PASS — `CONDITION_EXPRESS` handler is auto-registered. If the test fails with `CONDITION_EXPRESS not found`, add explicit registration in `HandlerRegistry.java`.

- [ ] **Step 4: Add CONDITION_EXPRESS to frontend NodeTypeConfig**

Locate the existing node type options array in `frontend/src/pages/canvas-editor/components/NodeTypeConfig.tsx` and append these two entries:

```tsx
// In frontend/src/pages/canvas-editor/components/NodeTypeConfig.tsx
// This adds two entries to the existing node type options array. Locate the array definition and append these entries.

{
  value: 'CONDITION_EXPRESS',
  label: '条件判断（QLExpress）',
  description: '使用 QLExpress 表达式引擎，安全无泄漏',
  category: 'condition',
}

// Keep existing CONDITION_GROOVY option for migration:
{
  value: 'CONDITION_GROOVY',
  label: '条件判断（Groovy）',
  description: '旧版 Groovy 脚本引擎，仅用于存量脚本',
  category: 'condition',
  deprecated: true,
}
```

- [ ] **Step 5: Run test to verify it passes**

Run: `cd backend && mvn test -pl canvas-engine -Dtest=HandlerRegistryTest -v`
Expected: PASS

- [ ] **Step 6: Commit**

```bash
git add backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handler/HandlerRegistry.java
git add frontend/src/pages/canvas-editor/components/NodeTypeConfig.tsx
git commit -m "feat: register CONDITION_EXPRESS handler, add frontend node type option"
```

---

### Task 3: Remove GroovyShellPool and Clean Up

**Files:**
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handlers/GroovyHandler.java`
- Delete: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handlers/GroovyShellPool.java`
- Modify: `backend/canvas-engine/src/main/resources/application.yml`

- [ ] **Step 1: Write failing test verifying GroovyShellPool is unused**

```java
// Add to QLExpressHandlerTest.java

@Test
void groovyShellPool_notReferencedByNewHandler() {
    // Verify QLExpressHandler has no reference to GroovyShellPool
    String handlerSource;
    try {
        handlerSource = java.nio.file.Files.readString(
            java.nio.file.Path.of("backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handlers/QLExpressHandler.java")
        );
    } catch (Exception e) {
        throw new RuntimeException(e);
    }
    assertThat(handlerSource).doesNotContain("GroovyShellPool");
    assertThat(handlerSource).doesNotContain("GroovyShell");
}
```

- [ ] **Step 2: Run test to verify it passes (it should already pass)**

Run: `cd backend && mvn test -pl canvas-engine -Dtest=QLExpressHandlerTest -v`
Expected: PASS - QLExpressHandler doesn't reference GroovyShellPool

- [ ] **Step 3: Simplify GroovyHandler to remove GroovyShellPool dependency**

```java
// In GroovyHandler.java, replace GroovyShellPool usage with inline GroovyShell:

// BEFORE: GroovyShell shell = groovyShellPool.borrowObject();
// AFTER: GroovyShell shell = new GroovyShell();

// This simplifies the handler for the migration period.
// The pool was an optimization that caused ClassLoader leaks anyway.

@Override
public Mono<NodeResult> executeAsync(Map<String, Object> config, ExecutionContext ctx) {
    return Mono.fromCallable(() -> {
        GroovyShell shell = new GroovyShell();
        try {
            Binding binding = new Binding(ctx.getFlatContext());
            shell.setBinding(binding);
            Object result = shell.evaluate((String) config.get("expression"));
            boolean condition = Boolean.TRUE.equals(result);
            return NodeResult.ifResult(condition, "success", "fail");
        } catch (Exception e) {
            return NodeResult.fail("Groovy error: " + e.getMessage());
        }
    }).subscribeOn(Schedulers.boundedElastic());
}
```

- [ ] **Step 4: Delete GroovyShellPool.java**

```bash
rm backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handlers/GroovyShellPool.java
```

- [ ] **Step 5: Remove GroovyShellPool config from application.yml**

```yaml
# Remove these lines from application.yml:
# canvas:
#   groovy:
#     pool:
#       max-total: 10
#       max-idle: 5
```

- [ ] **Step 6: Verify all tests still pass**

Run: `cd backend && mvn test -pl canvas-engine`
Expected: All tests pass

- [ ] **Step 7: Commit**

```bash
git add backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handlers/GroovyHandler.java
git rm backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handlers/GroovyShellPool.java
git add backend/canvas-engine/src/main/resources/application.yml
git commit -m "feat: remove GroovyShellPool, simplify GroovyHandler for migration period"
```