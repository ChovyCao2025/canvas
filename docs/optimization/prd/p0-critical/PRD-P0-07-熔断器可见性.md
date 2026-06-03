# PRD-P0-07-熔断器可见性

## 基本信息

| 字段 | 内容 |
|------|------|
| **PRD编号** | PRD-P0-07 |
| **需求名称** | 熔断器可见性 |
| **优先级** | P0 |
| **所属类别** | 错误恢复UX |
| **提出日期** | 2026-05-31 |
| **来源** | BMAD 产品设计审查报告 |

## 1. 问题描述

### 1.1 现状

熔断器机制已实现，但**操作员无法查看熔断器状态**（打开/关闭/半开），导致：
- 全局性失败无法定位原因
- 无法诊断触发的熔断规则
- 无法提前扩展人群或调整策略

### 1.2 痛点

- **故障诊断困难**：操作员不知道哪些下游服务熔断导致画布全链路失败
- **监控盲区**：熔断器日志无前端可视化，依赖后台日志分析
- **恢复依赖开发**：操作员无法主动恢复，需等待开发团队处理

### 1.3 竞品对标

| 竞品 | 能力描述 |
|------|----------|
| Netflix Resilience4j | 熔断器状态可视化（Dashboard + Metrics） |
| Hystrix Dashboard | Circuit Breaker Status + 故障统计 |

---

## 2. 目标与价值

### 2.1 用户故事

> 作为运营操作员，当遇到画布执行失败时，我希望看到触发的熔断器状态和原因，以便快速诊断全局性故障并通知技术团队。

### 2.2 成功指标

- **熔断器状态可见性** 100%（所有熔断器状态可通过 UI 查询）
- **故障诊断时间** < 10 分钟（从发现异常到定位熔断器）
- **操作员响应率** > 70%（看到熔断器状态后 1 小时内通知技术团队）

### 2.3 不做会怎样

- 无法定位全局性故障原因，修复依赖开发团队深入排查
- 全链路并发下降无预警（熔断后请求全部失败）
- 操作员无法主动恢复熔断器状态

---

## 3. 功能需求

### 3.1 核心功能

1. **熔断器状态监控**
   - 按熔断器名称查看状态（OPEN/CLOSED/HALF_OPEN）
   - 按触发条件查看熔断器列表（超时/失败率/并发）
   - 按画布ID 查询触发的熔断器

2. **熔断器详情页面**
   - 熔断器名称、状态、触发条件
   - 故障统计（失败次数、失败率、QPS）
   - 熔断时间线（打开时间、关闭时间、当前状态）
   - 影响画布列表（使用该熔断器的画布）

3. **熔断器操作**
   - 手动强制关闭熔断器（WARNING: 仅紧急情况）
   - 查看熔断器配置（阈值/超时）
   - 通知技术团队（RT/飞书）

### 3.2 详细描述

#### 3.2.1 熔断器状态模型

```java
// 熔断器状态枚举
enum CircuitBreakerState {
    CLOSED,      // 关闭（正常）
    OPEN,        // 打开（熔断）
    HALF_OPEN    // 半开（尝试恢复）
}

// 熔断器表（Redis Hash 缓存）
canvas_circuit_breaker:
  - breaker_name: "ExternalAPI_Timeout"
  - state: "OPEN"
  - failure_count: 15
  - success_count: 0
  - open_time: 1709251200000  -- 2024-03-01 00:00:00
  - variables: {"max_calls": 10, "percent_failure": 50.0}
```

#### 3.2.2 熔断器监控接口

```java
@RestController
@RequestMapping("/api/circuit/breakers")
public class CircuitBreakerController {

    @GetMapping("/list")
    public ResponseEntity<List<CircuitBreaker>> listBreakers(
        @RequestParam(required = false) String canvasId
    ) {
        List<CircuitBreaker> breakers = circuitBreakerService.listBreakers(canvasId);
        return ResponseEntity.ok(breakers);
    }

    @GetMapping("/{breakerName}")
    public ResponseEntity<CircuitBreakerDetail> getBreakerDetail(
        @PathVariable String breakerName
    ) {
        CircuitBreakerDetail detail = circuitBreakerService.getDetail(breakerName);
        return ResponseEntity.ok(detail);
    }

    @PostMapping("/{breakerName}/close")
    public ResponseEntity<Void> closeBreaker(
        @PathVariable String breakerName,
        @RequestBody CloseRequest request
    ) {
        circuitBreakerService.close(breakerName, request.getReason());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/affected/canvases")
    public ResponseEntity<List<String>> getAffectedCanvases() {
        List<String> canvasIds = circuitBreakerService.getAffectedCanvases();
        return ResponseEntity.ok(canvasIds);
    }
}

@Data
class CircuitBreaker {
    private String name;
    private String state;
    private int failureCount;
    private int successCount;
    private long openTime;
    private Map<String, Object> variables;
}
```

#### 3.2.3 熔断器时间线（前端可视化）

- 使用 ECharts Timeline 展示熔断器状态变化
- 打开时间 ++ 失败次数 +++

### 3.3 交互流程

**流程 1：操作员查看熔断器状态**

1. 进入"运行监控" → 选择"熔断器监控"
2. 查看熔断器列表（按状态筛选：OPEN 关键告警）
3. 点击熔断器 → 查看详情（触发条件、故障统计、影响画布）
4. 点击"通知技术团队" → 发送飞书通知
5. 如需手动关闭熔断器 → 点击"强制关闭"（输入原因）

---

## 4. 非功能需求

- **性能要求**：
  - 熔断器状态查询 P95 < 200ms（Redis Hash 直接查询）
  - 影响画布列表查询 P95 < 500ms

- **安全要求**：
  - 手动关闭熔断器需 RBAC 控制（仅技术团队）
  - 熔断器配置脱敏（不暴露内部变量）

- **可用性要求**：
  - 定时刷新熔断器状态（5 秒轮询或 WebSocket 推送）

---

## 5. 验收标准

- [ ] 后端熔断器监控接口实现
- [ ] 熔断器状态以 JSON 格式返回（state/open_time/failure_count）
- [ ] 运营端"运行监控"页面可查看熔断器列表
- [ ] 熔断器详情页面展示故障统计
- [ ] 列表支持按状态筛选（OPEN/其他）
- [ ] 影响画布列表查询接口实现
- [ ] 手动关闭熔断器接口实现
- [ ] 通知技术团队功能集成（飞书 API）
- [ ] 熔断器状态实时刷新（5 秒轮询）

---

## 6. 技术建议

### 6.1 涉及模块

- **前端**：
  - `frontend/src/pages/OperationalControl/CircuitBreakerMonitor.tsx`
  - Ant Design Table + ECharts 时间线

- **后端**：
  - `backend/canvas-engine/src/main/java/com/canvas/executor/circuit/BreakerMonitorService.java`
  - `backend/canvas-engine/src/main/java/com/canvas/model/CircuitBreaker.java`
  - Redis Key: `canvas_circuit_breaker:{breaker_name}`

- **数据库**：
  - 无需新表（状态以 Redis Hash 存储）

### 6.2 技术要点

1. **Redis Hash 缓存**
   - 按熔断器名称 Hash 存储（HSET/HGET/HMGET）
   - 过期时间设置为 24 小时（自动清理）

2. **影响画布列表查询**
   - 代码扫描：查询 CodeBase 中所有使用 `@CircuitBreaker` 注解的节点
   - 结果缓存（Redis List，TTL=1 小时）

3. **手动关闭熔断器**
   - 允许技术团队强制关闭（记录操作日志写入 `canvas_audit_log` 表）
   - 警告：仅紧急情况使用，避免雪崩

---

## 7. 依赖与风险

### 7.1 前置依赖

- 熔断器基础设施（Spring Cloud Circuit Breaker + Redis）- 已存在
- 飞书 API 集成 - 已存在

### 7.2 风险

- **手动误操作**：非紧急情况下手动关闭熔断器可能导致雪崩（需二次确认弹窗）

---

## 8. 参考资料

- BMAD 产品设计审查报告 (2026-05-31) — 错误恢复 UX 层缺项
- Spring Cloud Circuit Breaker Feign 文档
