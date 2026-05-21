# 接口 Map 魔法字符串重构设计（优化点 #3）

## 背景

多个控制器直接返回 `Map<String, Object>`，键为魔法字符串（如 `"nodeId"`、`"status"`、`"total"`），导致：
- IDE 无法跳转、自动补全、重命名
- Swagger 无法生成字段文档
- 调用方无法知道返回结构

## 范围

### 需 DTO 化（本次处理）

| 控制器 | 方法 | 新 DTO |
|--------|------|--------|
| `CanvasStatsController` | `getTrace` | `TraceItemDTO` |
| `CanvasStatsController` | `recentExecutions` | `ExecutionSummaryDTO` |
| `CanvasStatsController` | `stats` | `CanvasStatsDTO` |
| `CanvasStatsController` | `trend` | `TrendPointDTO` |
| `CanvasStatsController` | `funnel` | `NodeFunnelDTO`（需改 Mapper） |
| `MetaController` | `getMqDefinitions` | 复用已有 `StubOption`，加 `requestSchema` 字段 → `MqDefinitionDTO` |
| `MetaController` | `getApiDefinitions` | 类似 → `ApiDefinitionDTO` |
| `EventDefinitionController` | `reportEvent` | `EventReportResponseDTO` |

### 保留 Map（合理，不动）

- `ExecutionController.directCall` / `dryRun`：画布执行输出天然动态，类型无法静态化
- 引擎内部 `ExecutionContext`：节点 config 和运行时数据均为动态结构

## DTO 设计

所有 DTO 使用 Java record（不可变、无样板代码）。

```java
// TraceItemDTO
public record TraceItemDTO(
    String nodeId, String nodeType, String nodeName,
    Integer status, String errorMsg, String outputData, Long durationMs
) {}

// ExecutionSummaryDTO
public record ExecutionSummaryDTO(
    String id, String triggerType, Integer status, String userId, String createdAt
) {}

// CanvasStatsDTO
public record CanvasStatsDTO(
    long total, long success, long failed, long paused,
    String successRate, long uniqueUsers
) {}

// TrendPointDTO
public record TrendPointDTO(String date, long count) {}

// NodeFunnelDTO（替换 Mapper 返回的 Map）
public record NodeFunnelDTO(
    String nodeId, String nodeType, long entered, long succeeded, long failed, long skipped
) {}

// MqDefinitionDTO
public record MqDefinitionDTO(String value, String label, String requestSchema) {}

// ApiDefinitionDTO
public record ApiDefinitionDTO(String value, String label, String requestSchema) {}

// EventReportResponseDTO
public record EventReportResponseDTO(String executionId, String userId, String status) {}
```

## 存放路径

所有 DTO 放入 `backend/canvas-engine/src/main/java/org/chovy/canvas/dto/` 包。

## 改动边界

- **不改接口路径**
- **不改字段名**（原 Map 键即为 DTO 字段名，API 契约不变）
- **不改业务逻辑**（只替换 Map 拼装为 DTO 构造）
- `NodeFunnelDTO` 需同步修改 `CanvasExecutionTraceMapper.selectFunnelByCanvasId` 返回类型

## 不在范围内

- 引擎内部节点 config / 执行上下文的 Map 使用
- `ExecutionController` 的动态输出
- Swagger 注解补充（属于优化点 #api-doc，单独处理）
