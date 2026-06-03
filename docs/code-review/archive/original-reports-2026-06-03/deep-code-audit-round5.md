# Deep Code Audit — Round 5

> 第五轮深度扫描：HTTP客户端安全、连接池管理、Actuator暴露、数据脱敏覆盖率、乐观锁实现、加密缺失
> 审计日期: 2026-05-31

## Summary

| Severity | Count | Categories |
|----------|-------|------------|
| **P0 CRITICAL** | 1 | WebClient每次请求重建实例(3处) |
| **P1 HIGH** | 5 | 连接池无idle eviction、HikariCP无leak detection、health show-details=always、DataMasking仅3处调用(覆盖率极低)、无应用层加密 |
| **P2 MEDIUM** | 5 | CSRF全局禁用(无替代)、ApiCallHandler响应体无大小限制、TaggerOfflineHandler/CouponHandler/ReachPlatformHandler响应体无大小限制、CircuitBreaker无per-node配置、WebClient response-timeout 3s可能不够 |
| **P3 LOW** | 2 | SafeUpdateReq无@Valid、CanvasOpsService乐观锁无重试 |

---

## P0 — CRITICAL

### P0-1: WebClient 每次请求重建实例 — 连接池失效

**文件**: `engine/handlers/ApiCallHandler.java:186,188`, `domain/meta/TagImportSourceService.java:134`

**问题**: 
```java
webClientBuilder.build().get().uri(url)  // 每次调用 build()
```
`WebClientConfig` 配置了连接池（`ConnectionProvider` maxConnections=500），但 `webClientBuilder.build()` 每次创建新 `WebClient` 实例。新实例不共享连接池 — 每个实例创建自己的 `ConnectionProvider`，导致：
1. 连接池配置完全失效 — 实际无界建连
2. 连接不复用 — 每次请求新建 TCP 连接
3. 连接不关闭 — 无 idle eviction，连接泄漏

**影响**: 高频 API_CALL 节点 → 大量 TCP 连接 → 外部服务连接耗尽或本机 fd 耗尽

**修复**: 
1. `WebClientConfig` 创建单例 `WebClient` Bean
2. Handler 注入 `WebClient` 而非 `WebClient.Builder`
3. 如需不同配置，创建有限个 WebClient Bean（如 default、longTimeout）

---

## P1 — HIGH

### P1-1: WebClient ConnectionProvider 无 idle eviction

**文件**: `config/WebClientConfig.java:36-40`

**问题**: 
```java
ConnectionProvider.builder("canvas-http")
    .maxConnections(maxConnections)
    .pendingAcquireMaxCount(pendingAcquireMaxCount)
    .pendingAcquireTimeout(Duration.ofMillis(pendingAcquireTimeoutMs))
    .build();
```
无 `maxIdleTime` / `maxLifeTime` 配置。空闲连接永不回收，长时间运行后连接池中积累大量僵死连接。

**影响**: 外部服务重启后，池中旧连接不可用但未被回收 → 后续请求失败

**修复**: 添加 `.maxIdleTime(Duration.ofSeconds(30)).maxLifeTime(Duration.ofMinutes(5))`

---

### P1-2: HikariCP 无 leak detection

**文件**: `application.yml` hikari 配置

**问题**: HikariCP 配置了 pool size、timeout、idle timeout 等，但未配置 `leak-detection-threshold`。连接泄漏时无告警。

**影响**: 连接泄漏（如 AudienceUserResolver DataSource 未关闭）无法及时发现

**修复**: 添加 `leak-detection-threshold: 60000` (60秒)

---

### P1-3: Actuator health show-details=always — 信息泄露

**文件**: `application.yml:166`

```yaml
management:
  endpoint:
    health:
      show-details: always
```

**问题**: `show-details: always` 对所有请求者暴露健康检查详情，包括：
- 数据库连接状态（URL、driver）
- Redis 连接状态
- 磁盘空间信息

**影响**: 未认证用户可通过 `/actuator/health` 获取基础设施信息

**修复**: 改为 `show-details: when-authorized` 或 `never`（生产环境）

---

### P1-4: DataMaskingUtil 仅 3 处调用 — 脱敏覆盖率极低

**问题**: `DataMaskingUtil` 定义了手机号、身份证、递归 Map/List 脱敏能力，但仅 3 处调用：

| 调用位置 | 脱敏对象 |
|---------|---------|
| DagEngine:1235 | 节点执行输出写入 trace 时 |
| DagEngine:1247 | 错误消息写入 trace 时 |
| CdpUserService:126 | CDP 用户手机号 |

**未脱敏的关键路径**:
- `CanvasController` 返回画布列表/详情 — 无脱敏
- `AudienceController` 返回人群预览用户ID — 无脱敏
- `ExecutionController` 返回执行结果 — 无脱敏（DagEngine trace 有脱敏，但 API 响应无）
- `CdpUserController` 返回 CDP 用户列表 — 仅 phone 脱敏，其他敏感字段未脱敏
- `CanvasExecutionDO` 的 `triggerPayload` — 可能包含手机号/身份证

**影响**: API 响应中敏感数据（手机号、身份证）可能以明文返回前端

**修复**: 
1. API 响应层添加统一脱敏拦截器
2. `ExecutionContext.getFlatContext()` 返回前脱敏
3. `CanvasExecutionDO.triggerPayload` 序列化时脱敏

---

### P1-5: 无应用层加密 — data_source_config.password 明文存储

**问题**: 项目无 Jasypt、Vault、KMS 等加密集成。`data_source_config.password` 明文存储在 MySQL 中。`SysUserDO.password` 使用 BCrypt（正确），但其他敏感配置无加密。

**影响**: 数据库泄露 = 外部数据库凭证泄露

**修复**: 引入 Jasypt + `ENC()` 包装，或使用 Vault/KMS

---

## P2 — MEDIUM

### P2-1: CSRF 全局禁用且无替代保护

**文件**: `config/SecurityConfig.java:44`

```java
.csrf(ServerHttpSecurity.CsrfSpec::disable)
```

**问题**: CSRF 完全禁用。对于纯 JSON API + JWT 认证，CSRF 风险较低（浏览器不会自动附加 Authorization header）。但如果有 cookie-based session 或表单提交，CSRF 攻击可行。

**评估**: 当前架构（JWT Bearer + JSON API）下可接受，但需文档说明

**修复**: 添加注释说明禁用原因，或对 cookie 路径启用 CSRF

---

### P2-2: ApiCallHandler 响应体无大小限制

**文件**: `engine/handlers/ApiCallHandler.java:187,190`

**问题**: 
```java
.retrieve().bodyToMono(String.class)
```
`bodyToMono(String.class)` 将整个响应体加载到内存。如果外部 API 返回超大响应（如导出文件），会 OOM。

**影响**: 恶意或异常的外部 API 响应 → OOM

**修复**: 
1. 使用 `bodyToFlux(DataBuffer.class)` + 限制最大字节数
2. 或在 WebClient 配置中添加 `maxInboundContentLength`

---

### P2-3: TaggerOfflineHandler/CouponHandler/ReachPlatformHandler 同样无响应体大小限制

**文件**: `engine/handlers/TaggerOfflineHandler.java:54`, `CouponHandler.java:65`, `ReachPlatformHandler.java:61`

**问题**: 同样使用 `.bodyToMono(Map.class)` 无大小限制。

**修复**: 同 P2-2

---

### P2-4: CircuitBreaker 无 per-node 配置

**文件**: `engine/scheduler/CircuitBreakerRegistry.java`

**问题**: 熔断器配置为全局默认值（failure-threshold=5, open-duration=30s），所有节点类型共享。不同节点类型的失败特征不同（API_CALL 可能因网络抖动频繁失败，GROOVY 不应频繁失败）。

**修复**: 支持按 `nodeType` 配置不同阈值

---

### P2-5: WebClient response-timeout 3s 可能不够

**文件**: `config/WebClientConfig.java:32`

```java
@Value("${canvas.http-client.response-timeout-ms:3000}")
```

**问题**: 3 秒响应超时对某些外部 API（如 AI 推荐、批量查询）可能不够。

**修复**: 支持按 API 定义配置不同超时（`ApiDefinitionDO.responseTimeoutMs`）

---

## P3 — LOW

### P3-1: SafeUpdateReq 无 @Valid 注解

**文件**: `dto/SafeUpdateReq.java`, `web/CanvasController.java:326-327`

**问题**: `safeUpdate` 端点接受 `SafeUpdateReq` 但无 `@Valid`，`editVersion` 无 `@Min(0)` 校验。

**修复**: 添加 Bean Validation

---

### P3-2: CanvasOpsService 乐观锁无重试

**文件**: `domain/canvas/CanvasOpsService.java:61`

**问题**: `updateEditVersion` 返回 `updated` 行数，0 表示版本冲突。当前直接抛异常，无自动重试。

**修复**: 可接受（前端应刷新后重试），但需在 API 响应中明确返回冲突状态码（409）

---

## Infrastructure Security Audit

| 组件 | 配置 | 评估 |
|------|------|------|
| HikariCP pool size | 33 | ✅ 合理 |
| HikariCP connection-timeout | 3000ms | ✅ |
| HikariCP idle-timeout | 600000ms | ✅ |
| HikariCP max-lifetime | 1800000ms | ✅ |
| HikariCP leak-detection | **未配置** | ❌ P1-2 |
| WebClient max-connections | 500 | ✅ 但因 P0-1 失效 |
| WebClient connect-timeout | 1000ms | ✅ |
| WebClient response-timeout | 3000ms | ⚠️ P2-5 |
| WebClient idle eviction | **未配置** | ❌ P1-1 |
| Actuator exposure | health,info,prometheus,metrics | ✅ |
| Actuator health show-details | always | ❌ P1-3 |
| CSRF | disabled | ⚠️ P2-1 |
| Log format (prod) | JSON | ✅ |
| Log level (prod) | INFO | ✅ |

---

## Data Masking Coverage Matrix

| 数据路径 | 是否脱敏 | 风险 |
|---------|---------|------|
| DagEngine → trace 写入 | ✅ maskObject | — |
| DagEngine → error 写入 | ✅ maskText | — |
| CdpUserService → phone | ✅ maskPhone | — |
| CanvasController → API 响应 | ❌ | 手机号可能泄露 |
| AudienceController → 预览 | ❌ | userId 明文 |
| ExecutionController → 结果 | ❌ | 上下文含敏感数据 |
| CdpUserController → 列表 | ❌ (仅phone) | 身份证等未脱敏 |
| CanvasExecutionDO.triggerPayload | ❌ | 含用户输入敏感数据 |
| AuthController → LoginResp | ❌ | token 已通过 JSON 返回(设计如此) |
| DataSourceConfigController | ❌ | password WRITE_ONLY(部分保护) |

**脱敏覆盖率**: 3 / ~10 关键路径 = **30%**

---

## Cumulative Findings (Rounds 1-5)

| Severity | R1 | R2 | R3 | R4 | R5 | **Total** |
|----------|----|----|----|----|----|-----------| 
| P0 CRITICAL | 21 | 8 | 3 | 2 | 1 | **35** |
| P1 HIGH | 39 | 12 | 6 | 5 | 5 | **67** |
| P2 MEDIUM | — | 9 | 5 | 4 | 5 | **23** |
| P3 LOW | — | 4 | 2 | 1 | 2 | **9** |

### 新发现递减趋势

| 轮次 | P0 | P1 | 总新发现 | 变化 |
|------|----|----|---------|------|
| R1 | 21 | 39 | 60 | 基线 |
| R2 | 8 | 12 | 20 | -67% |
| R3 | 3 | 6 | 9 | -55% |
| R4 | 2 | 5 | 7 | -22% |
| R5 | 1 | 5 | 6 | -14% |

**P0 新发现已降至 1 项，P1 趋于稳定。后续轮次预计仅能发现 P2/P3 级别问题。**

### All P0 Issues — Complete List (35 items)

**Round 1 (21 items — 架构/设计层面)**: 已在 `docs/architect-checklist-report.md` 和 `docs/deep-code-audit-round2.md` 记录

**Round 2-5 新增 P0 (14 items)**:
1. R5-P0-1: WebClient 每次重建实例 → 连接池失效
2. R4-P0-1: HMAC 密钥硬编码默认值
3. R4-P0-2: 211 端点零 Bean Validation
4. R3-P0-1: 多租户隔离失效
5. R3-P0-2: DataSourceConfig password API 明文返回
6. R3-P0-3: 14 个 Handler→Mapper 分层违规
7. R2-P0-1: Audience JDBC DataSource 连接泄漏
8. R2-P0-2: SQL 注入风险
9. R2-P0-3: GroovyHandler 虚拟线程池无 shutdown
10. R2-P0-4: .block() 阻塞 Reactor EventLoop
11. R2-P0-5: ExecutionController userId 伪造
12. R2-P0-6: CanvasExecutionService God Class
13. R2-P0-7: DagEngine 过度耦合
14. R2-P0-8: @Transactional + Redis 不一致

**建议停止循环。** 五轮审核已全面覆盖架构、并发、Reactor、多租户、配置、验证、资源、异常、前端、数据正确性、HTTP客户端、脱敏、加密等维度。P0+P1 累计 102 项，后续轮次投入产出比极低。
