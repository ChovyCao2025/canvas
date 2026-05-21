# 接口 Map 魔法字符串重构 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将控制器中内联拼装 `Map<String, Object>` 的接口全部替换为强类型 DTO，消除魔法字符串，不改变 API 字段名和路径。

**Architecture:** 在 `dto` 包新建 Java record DTO → 逐控制器替换 → 改 Mapper 返回类型（funnel）。引擎内部动态 Map 不动。

**Tech Stack:** Java 17 records, MyBatis-Plus, Spring WebFlux

---

## File Map

| Action | File |
|--------|------|
| Create | `dto/TraceItemDTO.java` |
| Create | `dto/ExecutionSummaryDTO.java` |
| Create | `dto/CanvasStatsDTO.java` |
| Create | `dto/TrendPointDTO.java` |
| Create | `dto/NodeFunnelDTO.java` |
| Create | `dto/MqDefinitionDTO.java` |
| Create | `dto/ApiDefinitionDTO.java` |
| Create | `dto/EventReportResponseDTO.java` |
| Modify | `controller/CanvasStatsController.java` |
| Modify | `controller/MetaController.java` |
| Modify | `controller/EventDefinitionController.java` |
| Modify | `domain/execution/CanvasExecutionTraceMapper.java` |
| Modify | `resources/mapper/CanvasExecutionTraceMapper.xml` |

所有路径以 `backend/canvas-engine/src/main/java/org/chovy/canvas/` 为前缀，XML 在 `src/main/resources/mapper/`。

---

## Task 1：创建全部 DTO record

**Files:** 8 个新文件，全部在 `dto/` 包

- [ ] **Step 1：创建 TraceItemDTO.java**

```java
package org.chovy.canvas.dto;

public record TraceItemDTO(
        String nodeId,
        String nodeType,
        String nodeName,
        Integer status,
        String errorMsg,
        String outputData,
        Long durationMs
) {}
```

- [ ] **Step 2：创建 ExecutionSummaryDTO.java**

```java
package org.chovy.canvas.dto;

public record ExecutionSummaryDTO(
        String id,
        String triggerType,
        Integer status,
        String userId,
        String createdAt
) {}
```

- [ ] **Step 3：创建 CanvasStatsDTO.java**

```java
package org.chovy.canvas.dto;

public record CanvasStatsDTO(
        long total,
        long success,
        long failed,
        long paused,
        String successRate,
        long uniqueUsers
) {}
```

- [ ] **Step 4：创建 TrendPointDTO.java**

```java
package org.chovy.canvas.dto;

public record TrendPointDTO(String date, long count) {}
```

- [ ] **Step 5：创建 NodeFunnelDTO.java**

字段名与 Mapper SQL 的 AS 别名严格对齐：

```java
package org.chovy.canvas.dto;

public record NodeFunnelDTO(
        String nodeId,
        String nodeType,
        String nodeName,
        long totalEntered,
        long totalSuccess,
        long totalFailed,
        long totalSkipped,
        Double avgDurationMs
) {}
```

- [ ] **Step 6：创建 MqDefinitionDTO.java**

```java
package org.chovy.canvas.dto;

public record MqDefinitionDTO(String value, String label, String requestSchema) {}
```

- [ ] **Step 7：创建 ApiDefinitionDTO.java**

```java
package org.chovy.canvas.dto;

public record ApiDefinitionDTO(String value, String label, String requestSchema) {}
```

- [ ] **Step 8：创建 EventReportResponseDTO.java**

```java
package org.chovy.canvas.dto;

public record EventReportResponseDTO(String executionId, String userId, String status) {}
```

- [ ] **Step 9：编译验证**

```bash
cd backend && mvn compile -pl canvas-engine -q
```

Expected: `BUILD SUCCESS`

- [ ] **Step 10：Commit**

```bash
git add backend/canvas-engine/src/main/java/org/chovy/canvas/dto/
git commit -m "feat: add DTO records to replace Map<String,Object> in controllers"
```

---

## Task 2：重构 CanvasStatsController — getTrace / recentExecutions / stats / trend

**Files:**
- Modify: `controller/CanvasStatsController.java`

- [ ] **Step 1：替换 getTrace 方法**

将方法签名和 Map 拼装替换为：

```java
@GetMapping("/execution/{executionId}/trace")
public Mono<R<List<TraceItemDTO>>> getTrace(@PathVariable String executionId) {
    return Mono.fromCallable(() -> {
        List<CanvasExecutionTrace> all =
                traceMapper.selectList(
                        new LambdaQueryWrapper<CanvasExecutionTrace>()
                                .eq(CanvasExecutionTrace::getExecutionId, executionId)
                                .orderByAsc(CanvasExecutionTrace::getStartedAt));

        Map<String, CanvasExecutionTrace> best = new LinkedHashMap<>();
        for (var t : all) {
            best.merge(t.getNodeId(), t,
                    (a, b) -> b.getStatus() > a.getStatus() ? b : a);
        }

        return best.values().stream().map(t -> {
            Long durationMs = t.getDurationMs();
            if (durationMs == null && t.getStartedAt() != null && t.getFinishedAt() != null) {
                durationMs = Duration.between(t.getStartedAt(), t.getFinishedAt()).toMillis();
            }
            return new TraceItemDTO(
                    t.getNodeId(), t.getNodeType(), t.getNodeName(),
                    t.getStatus(), t.getErrorMsg(), t.getOutputData(), durationMs);
        }).collect(Collectors.toList());
    }).subscribeOn(Schedulers.boundedElastic()).map(R::ok);
}
```

添加 import：`import org.chovy.canvas.dto.TraceItemDTO;`

- [ ] **Step 2：替换 recentExecutions 方法**

```java
@GetMapping("/executions")
public Mono<R<List<ExecutionSummaryDTO>>> recentExecutions(
        @PathVariable Long id,
        @RequestParam(defaultValue = "20") int size) {
    return Mono.fromCallable(() -> {
        List<CanvasExecution> execs = executionMapper.selectList(
                new LambdaQueryWrapper<CanvasExecution>()
                        .eq(CanvasExecution::getCanvasId, id)
                        .orderByDesc(CanvasExecution::getCreatedAt)
                        .last("LIMIT " + Math.min(size, 100)));
        return execs.stream()
                .map(e -> new ExecutionSummaryDTO(
                        e.getId(),
                        e.getTriggerType(),
                        e.getStatus(),
                        e.getUserId(),
                        e.getCreatedAt() != null ? e.getCreatedAt().toString() : null))
                .collect(Collectors.toList());
    }).subscribeOn(Schedulers.boundedElastic()).map(R::ok);
}
```

添加 import：`import org.chovy.canvas.dto.ExecutionSummaryDTO;`

- [ ] **Step 3：替换 stats 方法**

```java
@GetMapping("/stats")
public Mono<R<CanvasStatsDTO>> stats(
        @PathVariable Long id,
        @RequestParam(defaultValue = "7") int days,
        @RequestParam(required = false) String since,
        @RequestParam(required = false) String until) {
    return Mono.fromCallable(() -> {
        LocalDate sinceDate = since != null ? LocalDate.parse(since) : LocalDate.now().minusDays(days);
        LocalDate untilDate = until != null ? LocalDate.parse(until) : LocalDate.now();

        List<CanvasExecution> executions = executionMapper.selectList(
                new LambdaQueryWrapper<CanvasExecution>()
                        .eq(CanvasExecution::getCanvasId, id)
                        .ge(CanvasExecution::getCreatedAt, sinceDate.atStartOfDay())
                        .le(CanvasExecution::getCreatedAt, untilDate.plusDays(1).atStartOfDay()));

        long total = executions.size();
        long success = executions.stream().filter(e -> e.getStatus() == 2).count();
        long failed  = executions.stream().filter(e -> e.getStatus() == 3).count();
        long paused  = executions.stream().filter(e -> e.getStatus() == 1).count();
        long uniqueUsers = executions.stream()
                .filter(e -> e.getUserId() != null)
                .map(CanvasExecution::getUserId)
                .distinct().count();
        String successRate = total > 0
                ? String.format("%.1f%%", success * 100.0 / total) : "0%";

        return new CanvasStatsDTO(total, success, failed, paused, successRate, uniqueUsers);
    }).subscribeOn(Schedulers.boundedElastic()).map(R::ok);
}
```

添加 import：`import org.chovy.canvas.dto.CanvasStatsDTO;`

- [ ] **Step 4：替换 trend 方法**

```java
@GetMapping("/trend")
public Mono<R<List<TrendPointDTO>>> trend(
        @PathVariable Long id,
        @RequestParam(defaultValue = "30") int days,
        @RequestParam(required = false) String since,
        @RequestParam(required = false) String until) {
    return Mono.fromCallable(() -> {
        LocalDate sinceDate = since != null ? LocalDate.parse(since) : LocalDate.now().minusDays(days);
        LocalDate untilDate = until != null ? LocalDate.parse(until) : LocalDate.now();

        List<CanvasExecution> executions = executionMapper.selectList(
                new LambdaQueryWrapper<CanvasExecution>()
                        .eq(CanvasExecution::getCanvasId, id)
                        .ge(CanvasExecution::getCreatedAt, sinceDate.atStartOfDay())
                        .le(CanvasExecution::getCreatedAt, untilDate.plusDays(1).atStartOfDay())
                        .orderByAsc(CanvasExecution::getCreatedAt));

        Map<String, Long> byDate = new LinkedHashMap<>();
        for (CanvasExecution e : executions) {
            if (e.getCreatedAt() == null) continue;
            String date = e.getCreatedAt().toLocalDate().toString();
            byDate.merge(date, 1L, Long::sum);
        }

        return byDate.entrySet().stream()
                .map(entry -> new TrendPointDTO(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
    }).subscribeOn(Schedulers.boundedElastic()).map(R::ok);
}
```

添加 import：`import org.chovy.canvas.dto.TrendPointDTO;`

- [ ] **Step 5：编译验证**

```bash
cd backend && mvn compile -pl canvas-engine -q
```

Expected: `BUILD SUCCESS`

- [ ] **Step 6：Commit**

```bash
git add backend/canvas-engine/src/main/java/org/chovy/canvas/controller/CanvasStatsController.java
git commit -m "refactor: replace Map<String,Object> with DTOs in CanvasStatsController (trace/executions/stats/trend)"
```

---

## Task 3：重构 CanvasStatsController.funnel + Mapper

**Files:**
- Modify: `domain/execution/CanvasExecutionTraceMapper.java`
- Modify: `resources/mapper/CanvasExecutionTraceMapper.xml`
- Modify: `controller/CanvasStatsController.java`

- [ ] **Step 1：修改 Mapper Java 接口方法返回类型**

在 `CanvasExecutionTraceMapper.java` 中，将：
```java
List<Map<String, Object>> selectFunnelByCanvasId(@Param("canvasId") Long canvasId);
```
改为：
```java
List<NodeFunnelDTO> selectFunnelByCanvasId(@Param("canvasId") Long canvasId);
```

添加 import：`import org.chovy.canvas.dto.NodeFunnelDTO;`

- [ ] **Step 2：修改 Mapper XML resultType**

在 `CanvasExecutionTraceMapper.xml` 中，将：
```xml
<select id="selectFunnelByCanvasId" parameterType="long" resultType="java.util.Map">
```
改为：
```xml
<select id="selectFunnelByCanvasId" parameterType="long" resultType="org.chovy.canvas.dto.NodeFunnelDTO">
```

SQL 语句本身不变（AS 别名与 record 字段名已对齐）。

- [ ] **Step 3：修改控制器 funnel 方法签名**

在 `CanvasStatsController.java` 中，将：
```java
public Mono<R<List<Map<String, Object>>>> funnel(@PathVariable Long id) {
```
改为：
```java
public Mono<R<List<NodeFunnelDTO>>> funnel(@PathVariable Long id) {
```

添加 import：`import org.chovy.canvas.dto.NodeFunnelDTO;`

- [ ] **Step 4：编译验证**

```bash
cd backend && mvn compile -pl canvas-engine -q
```

Expected: `BUILD SUCCESS`

- [ ] **Step 5：Commit**

```bash
git add backend/canvas-engine/src/main/java/org/chovy/canvas/domain/execution/CanvasExecutionTraceMapper.java \
        backend/canvas-engine/src/main/resources/mapper/CanvasExecutionTraceMapper.xml \
        backend/canvas-engine/src/main/java/org/chovy/canvas/controller/CanvasStatsController.java
git commit -m "refactor: replace Map<String,Object> with NodeFunnelDTO in funnel endpoint"
```

---

## Task 4：重构 MetaController — getMqDefinitions / getApiDefinitions

**Files:**
- Modify: `controller/MetaController.java`

- [ ] **Step 1：替换 getMqDefinitions**

将：
```java
public Mono<R<List<Map<String, Object>>>> getMqDefinitions() {
    ...
    return defs.stream().map(d -> {
        Map<String, Object> m = new java.util.LinkedHashMap<>();
        m.put("value", d.getMessageCode());
        m.put("label", d.getName());
        m.put("requestSchema", d.getRequestSchema() != null ? d.getRequestSchema() : "[]");
        return m;
    }).collect(Collectors.toList());
```
改为：
```java
public Mono<R<List<MqDefinitionDTO>>> getMqDefinitions() {
    ...
    return defs.stream()
            .map(d -> new MqDefinitionDTO(
                    d.getMessageCode(),
                    d.getName(),
                    d.getRequestSchema() != null ? d.getRequestSchema() : "[]"))
            .collect(Collectors.toList());
```

添加 import：`import org.chovy.canvas.dto.MqDefinitionDTO;`

- [ ] **Step 2：替换 getApiDefinitions**

将：
```java
public Mono<R<List<Map<String, Object>>>> getApiDefinitions() {
    ...
    return defs.stream().map(def -> {
        Map<String, Object> m = new java.util.LinkedHashMap<>();
        m.put("value", def.getApiKey());
        m.put("label", def.getName());
        m.put("requestSchema", def.getRequestSchema() != null ? def.getRequestSchema() : "[]");
        return m;
    }).collect(Collectors.toList());
```
改为：
```java
public Mono<R<List<ApiDefinitionDTO>>> getApiDefinitions() {
    ...
    return defs.stream()
            .map(def -> new ApiDefinitionDTO(
                    def.getApiKey(),
                    def.getName(),
                    def.getRequestSchema() != null ? def.getRequestSchema() : "[]"))
            .collect(Collectors.toList());
```

添加 import：`import org.chovy.canvas.dto.ApiDefinitionDTO;`

- [ ] **Step 3：编译验证**

```bash
cd backend && mvn compile -pl canvas-engine -q
```

Expected: `BUILD SUCCESS`

- [ ] **Step 4：Commit**

```bash
git add backend/canvas-engine/src/main/java/org/chovy/canvas/controller/MetaController.java
git commit -m "refactor: replace Map<String,Object> with DTOs in MetaController"
```

---

## Task 5：重构 EventDefinitionController.reportEvent

**Files:**
- Modify: `controller/EventDefinitionController.java`

- [ ] **Step 1：找到 reportEvent 中的 Map 拼装（约 L152-158）并替换**

将：
```java
Map<String, Object> resp = new java.util.LinkedHashMap<>();
resp.put("executionId", ...);
resp.put("userId", req.getUserId());
resp.put("status", "ACCEPTED");
return resp;
```
改为：
```java
return new EventReportResponseDTO(/* executionId */, req.getUserId(), "ACCEPTED");
```

同时将方法返回类型从 `Mono<R<Map<String, Object>>>` 改为 `Mono<R<EventReportResponseDTO>>`。

添加 import：`import org.chovy.canvas.dto.EventReportResponseDTO;`

- [ ] **Step 2：编译验证**

```bash
cd backend && mvn compile -pl canvas-engine -q
```

Expected: `BUILD SUCCESS`

- [ ] **Step 3：运行全量测试，确认无回归**

```bash
cd backend && mvn test -pl canvas-engine -q
```

Expected: `BUILD SUCCESS`，所有现有测试通过

- [ ] **Step 4：Commit**

```bash
git add backend/canvas-engine/src/main/java/org/chovy/canvas/controller/EventDefinitionController.java
git commit -m "refactor: replace Map<String,Object> with EventReportResponseDTO in reportEvent"
```
