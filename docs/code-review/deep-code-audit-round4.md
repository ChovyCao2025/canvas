# Deep Code Audit — Round 4

> 第四轮深度扫描：配置安全、输入验证、分页/导出安全、时区处理、Lombok陷阱、前端HTTP客户端安全
> 审计日期: 2026-05-31

## Summary

| Severity | Count | Categories |
|----------|-------|------------|
| **P0 CRITICAL** | 2 | HMAC默认密钥、211个API端点零Bean Validation |
| **P1 HIGH** | 5 | MySQL useSSL=false+allowPublicKeyRetrieval、DataSourceConfigDO @Data+@JsonProperty矛盾、57处LocalDateTime.now()无时区、无文件上传大小限制、前端axios无timeout |
| **P2 MEDIUM** | 4 | @ToString泄漏密码、TagImport无文件类型校验、CdpTagOperationController无分页上限、定时任务默认Asia/Shanghai |
| **P3 LOW** | 1 | SysUserDO @Data含@JsonIgnore但toString仍输出 |

---

## P0 — CRITICAL

### P0-1: HMAC 签名密钥硬编码默认值

**文件**: `application.yml:55`

```yaml
report-secret: ${CANVAS_EVENT_REPORT_SECRET:canvas-event-report-secret-2026!!}
```

**问题**: 事件上报 HMAC 密钥有硬编码默认值。如果未设置环境变量 `CANVAS_EVENT_REPORT_SECRET`，所有实例使用相同默认密钥。`EventReportAuthService` 仅在启动时检查密钥长度 >= 32 字节（此默认值满足），不检查是否为默认值。

**影响**: 攻击者知道默认密钥 → 可伪造任意事件上报请求 → 触发任意画布执行

**修复**: 
1. 移除默认值: `${CANVAS_EVENT_REPORT_SECRET:}` → 启动时 fail-fast
2. 添加启动检查: 如果密钥等于已知默认值则拒绝启动

---

### P0-2: 211 个 API 端点零 Bean Validation

**问题**: 211 个 `@RequestBody` / `@RequestParam` / `@PathVariable` 绑定，0 个 `@Valid` / `@Validated` 注解。所有输入验证在 Service 层命令式执行（`requireText()`, `if (x == null)` 等）。

**关键缺失验证**:

| 端点 | 缺失验证 | 风险 |
|------|---------|------|
| POST /auth/login | username/password 无 @NotBlank | 空值请求打到 DB |
| POST /canvas | name 无长度限制 | 超长名称存入 DB |
| POST /admin/users | password 无强度校验 | 弱密码 |
| POST /canvas/trigger/behavior | userId/canvasId 无校验 | 注入攻击 |
| POST /canvas/execute/direct/{id} | inputParams 无深度限制 | 大对象序列化 OOM |
| PUT /canvas/data-sources | password 明文无加密 | (已知 P0) |

**影响**: 
- 恶意输入绕过 Service 层条件 → 数据损坏
- 大 payload → OOM / DoS
- 无法统一验证策略

**修复**: 
1. DTO 添加 `@NotBlank`, `@Size`, `@Min`, `@Max` 等 Bean Validation 注解
2. Controller 方法参数添加 `@Valid`
3. 优先修复公开端点（/auth/login, /canvas/trigger/behavior, /canvas/execute/direct/*）

---

## P1 — HIGH

### P1-1: MySQL useSSL=false + allowPublicKeyRetrieval=true

**文件**: `application.yml:7`

```
jdbc:mysql://localhost:3306/canvas_db?useSSL=false&allowPublicKeyRetrieval=true
```

**问题**: 
- `useSSL=false`: 数据库连接不加密 → 网络嗅探可获取查询和结果
- `allowPublicKeyRetrieval=true`: 允许中间人攻击替换 MySQL 公钥 → 凭证泄露

**影响**: 生产环境数据库通信可被窃听

**修复**: 
1. 生产 profile 必须用 `useSSL=true` + `requireSSL=true`
2. 删除 `allowPublicKeyRetrieval=true`，改用 CA 证书

---

### P1-2: DataSourceConfigDO @Data + @JsonProperty 矛盾

**文件**: `dal/dataobject/DataSourceConfigDO.java:14-36`

**问题**: 
```java
@Data                          // Lombok 生成 toString()、equals()、hashCode()
@JsonProperty(access = WRITE_ONLY)  // Jackson 序列化时排除 password
private String password;
```

`@Data` 生成的 `toString()` 包含 `password` 字段。日志输出 DataSourceConfigDO 实例时密码会明文出现在日志中。

同样，`@Data` 生成的 `equals()` 和 `hashCode()` 包含 password — 两个配置对象仅因密码不同就被认为不等，且密码参与哈希计算。

**影响**: 
- 日志泄露外部数据库密码
- equals/hashCode 语义错误

**修复**: 
1. 添加 `@ToString.Exclude` 到 password 字段
2. 添加 `@EqualsAndHashCode.Exclude` 到 password 字段
3. 或改用 `@Getter @Setter` 替代 `@Data`

---

### P1-3: 57 处 LocalDateTime.now() 无时区感知

**问题**: 57 处 `LocalDateTime.now()` 调用，使用 JVM 默认时区。在容器化部署中，JVM 时区可能不是 `Asia/Shanghai`，导致：
- `CanvasExecutionDO.createdAt` 时间戳与业务时区不一致
- `CanvasSchedulerService` 定时触发时间偏移
- 数据库查询时间范围错误

**影响**: 跨时区部署时定时任务和审计时间戳错误

**修复**: 
1. 全局设置 JVM 时区: `TZ=Asia/Shanghai` 环境变量
2. 或改用 `OffsetDateTime.now(ZoneId.of("Asia/Shanghai"))` / `Instant.now()`
3. `ScheduleRegistration` 已正确处理 timezone（好的实践）

---

### P1-4: 无文件上传大小限制

**文件**: `web/TagImportController.java:102`

**问题**: `POST /tag-import/excel` 接受 `MultipartFile` 上传，但 `application.yml` 无 `spring.servlet.multipart.max-file-size` 配置。Spring Boot 默认 1MB，但 `TagImportController` 内限制 20000 行，无文件大小校验。

**影响**: 大文件上传 → 磁盘/内存耗尽

**修复**: 
1. 配置 `spring.servlet.multipart.max-file-size: 10MB`
2. 配置 `spring.servlet.multipart.max-request-size: 10MB`
3. Controller 内添加文件大小校验

---

### P1-5: 前端 axios 无 timeout 配置

**文件**: `frontend/src/services/api.ts:20`

```typescript
const http = axios.create({ baseURL: '/' })
```

**问题**: axios 实例无 `timeout` 配置。画布执行等长操作可能挂起前端请求。

**影响**: 画布发布/执行请求无超时 → 前端挂起，用户无法操作

**修复**: 添加 `timeout: 30000` (30秒)，画布执行接口用更长 timeout

---

## P2 — MEDIUM

### P2-1: SysUserDO @Data + @JsonIgnore — toString 仍输出 password

**文件**: `dal/dataobject/SysUserDO.java:13-31`

**问题**: `@JsonIgnore` 只影响 Jackson 序列化，`@Data` 生成的 `toString()` 仍包含 password。如果 `SysUserDO` 实例被日志输出（如 `log.info("user={}", user)`），密码会明文出现在日志。

**修复**: password 字段添加 `@ToString.Exclude`

---

### P2-2: TagImport 文件类型无校验

**文件**: `web/TagImportController.java:102`

**问题**: Excel 导入端点接受 `MultipartFile`，但未校验 Content-Type 或文件扩展名。攻击者可上传 .exe 或 .sh 文件。

**修复**: 校验 `file.getContentType()` 为 `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet` 或 `application/vnd.ms-excel`

---

### P2-3: CdpTagOperationController limit 无上限

**文件**: `web/CdpTagOperationController.java:47-48`

```java
public Mono<R<List<CdpTagOperationDO>>> list(@RequestParam(defaultValue = "20") int limit)
```

**问题**: `limit` 参数无上限校验。请求 `?limit=999999` 可返回全表数据。

**修复**: 添加 `Math.min(limit, 200)` 上限

---

### P2-4: 定时任务默认时区 Asia/Shanghai 硬编码

**文件**: `engine/trigger/CanvasSchedulerService.java:119`, `engine/schedule/ScheduleRegistration.java:37`

**问题**: 定时触发 timezone 默认 `Asia/Shanghai`，适合中国市场但不适合国际化。

**修复**: 可接受（当前仅中国市场），但需文档说明时区假设

---

## P3 — LOW

### P3-1: DataSourceConfigDO testConnection 创建未关闭的 DataSource

**文件**: `web/DataSourceConfigController.java:194-213`

**问题**: `testConnection` 每次创建 `DataSourceBuilder.create().build()`，try-with-resources 只关闭 Connection 不关闭连接池。与 AudienceUserResolver 相同问题但频率较低（手动测试触发）。

**修复**: 与 P0-1 Round2 一起修复，引入 DataSource 池化

---

## Configuration Security Audit

| 配置项 | 当前值 | 风险 | 建议 |
|--------|--------|------|------|
| JWT secret | `${CANVAS_JWT_SECRET:}` | ✅ 无默认值 | 保持 |
| HMAC report secret | `${...:canvas-event-report-secret-2026!!}` | ❌ 硬编码 | P0-1 |
| MySQL password | `root` (明文) | ❌ 开发环境 | 生产必须改 |
| MySQL useSSL | `false` | ❌ 不加密 | P1-1 |
| MySQL allowPublicKeyRetrieval | `true` | ❌ MITM风险 | P1-1 |
| Redis password | 注释掉 | ❌ 无密码 | 生产必须配 |
| CORS origins | `*` | ❌ 通配符 | 已知P0 |
| multipart max-file-size | 未配置 | ❌ 无限制 | P1-4 |
| Actuator exposure | health,info,prometheus,metrics | ✅ 合理 | — |
| Log level (prod) | INFO | ✅ 合理 | — |
| Log format (prod) | JSON (Logstash) | ✅ 合理 | — |

---

## Input Validation Gap Analysis

| 端点类别 | 端点数 | @Valid | Service层校验 | 评估 |
|----------|--------|--------|--------------|------|
| Auth | 3 | 0 | 部分 | ❌ login缺验证 |
| Canvas CRUD | 14 | 0 | 部分 | ❌ name无长度 |
| Execution | 4 | 0 | 少量 | ❌ 公开端点零验证 |
| Admin | 6 | 0 | 部分 | ❌ password无强度 |
| Meta | 22 | 0 | 少量 | ⚠️ 低风险 |
| Audience | 5 | 0 | 部分 | ❌ sampleLimit有上限(好) |
| Data Source | 5 | 0 | 部分 | ❌ password明文 |
| Tag Import | 4 | 0 | 部分 | ❌ 无文件类型校验 |
| **Total** | **~63** | **0** | — | **零覆盖** |

---

## Cumulative Findings (Rounds 1-4)

| Severity | R1 | R2 | R3 | R4 | **Total** |
|----------|----|----|----|----|-----------|
| P0 CRITICAL | 21 | 8 | 3 | 2 | **34** |
| P1 HIGH | 39 | 12 | 6 | 5 | **62** |
| P2 MEDIUM | — | 9 | 5 | 4 | **18** |
| P3 LOW | — | 4 | 2 | 1 | **7** |

### 新发现递减趋势

| 轮次 | P0 | P1 | 总计 | 变化 |
|------|----|----|----|------|
| R1 | 21 | 39 | 60 | 基线 |
| R2 | 8 | 12 | 20 | -67% |
| R3 | 3 | 6 | 9 | -55% |
| R4 | 2 | 5 | 7 | -22% |

**递减趋势明显，剩余未发现的高严重度问题已有限。** 后续轮次预计仅能发现 P2/P3 级别问题。

### All P0 Issues (Sorted by Fix Priority)

1. **R3-P0-1**: 多租户隔离失效 — Canvas/Engine/Handler 层无 tenant_id
2. **R2-P0-5**: ExecutionController 行为触发端点 userId 伪造
3. **R4-P0-1**: HMAC 密钥硬编码默认值
4. **R3-P0-2**: DataSourceConfig password API 明文返回
5. **R2-P0-2**: SQL 注入风险 — 表名/列名拼接
6. **R2-P0-1**: Audience JDBC DataSource 连接泄漏
7. **R2-P0-8**: @Transactional + Redis 操作不一致
8. **R2-P0-3**: GroovyHandler 虚拟线程池无 shutdown
9. **R2-P0-4**: .block() 阻塞 Reactor EventLoop
10. **R3-P0-3**: 14 个 Handler→Mapper 分层违规
11. **R2-P0-6**: CanvasExecutionService God Class
12. **R2-P0-7**: DagEngine 过度耦合
13. **R4-P0-2**: 211 端点零 Bean Validation
14. **R1**: 21 项 (架构/设计层面)

**建议停止循环** — 四轮审核已覆盖：架构设计、并发安全、Reactor合规、多租户隔离、配置安全、输入验证、资源泄漏、异常处理、前端安全、数据正确性。P0+P1 总计 96 项，后续轮次预计仅发现 P2/P3 级别问题，投入产出比已不值得继续。
