# Security Considerations

## Overview

Marketing Canvas 实现了多层安全机制，但存在 3 个 CRITICAL 级别缺口。

## Strengths

### Authentication (认证)

- **JWT + HS256**: 最小32字节密钥，24小时过期，启动时 fail-fast 校验
- **Redis 黑名单**: 登出后 token SHA-256 hash 存入 `canvas:jwt:revoked:<hash>`
- **暴力破解防护**: 5次失败锁定15分钟 (Redis `canvas:login:locked:<username>`)
- **DB 重新验证**: 每次请求从 DB 重新加载用户状态 (enabled + role)

### Authorization (授权)

- **RBAC 4角色**: SUPER_ADMIN, TENANT_ADMIN, OPERATOR, ADMIN(legacy)
- **多租户隔离**: tenant_id 列级别隔离
- **路由级控制**: SecurityConfig 定义公开/TENANT_ADMIN/SUPER_ADMIN/认证路由

### API Security

- **SSRF 防护** (OutboundUrlValidator): 阻止 localhost/RFC1918/link-local/multicast/IPv6-unique-local，含 DNS 解析检查
- **API 限流**: 三层 (per-API Redis fixed-window + replay rate limit + login brute-force)
- **HMAC 签名**: EventReportAuthService — HmacSHA256 + 5分钟时间窗口 + 常量时间比较

### Groovy Sandbox

- **Import 白名单**: 仅允许基础类型/集合/时间/数学/正则
- **禁止反射**: Runtime, Process, ProcessBuilder, Thread, ClassLoader, Class, reflect.*
- **间接导入检查**: 防止 `new java.lang.ProcessBuilder()` 绕过
- **5秒超时**: 虚拟线程 Future.get() + cancel
- **64KB 输出限制**: 防止内存耗尽

### Data Masking

- **DataMaskingUtil**: 手机号 `138****8888`，身份证 `110***********1234`
- **默认敏感键**: phone, mobile, idCard, bankCard, password, token, secret, apiKey, credential 等20+
- **递归处理**: Map/List/String 深度脱敏

## Critical Gaps

### GAP-1: data_source_config.password 明文存储 (CRITICAL)

**位置**: V71 迁移, data_source_config 表
**影响**: 数据库泄露 = 外部数据库凭证泄露
**修复**: 引入 Jasypt 或 Vault → 见 brownfield-architecture.md

### GAP-2: 公开端点无认证 (CRITICAL)

**位置**: SecurityConfig
**受影响端点**:
- `POST /canvas/events/report` — HMAC签名已实现但需确保密钥已配置
- `POST /canvas/execute/direct/*` — 无认证
- `POST /canvas/trigger/behavior` — 无认证
- `POST /ops/**` — 内网专用但无网络层保障

**修复**: 添加 API Key 或 HMAC 签名认证

### GAP-3: CORS wildcard + allowCredentials (CRITICAL)

**位置**: application.yml `canvas.cors.allowed-origins: "*"`
**影响**: 任意域可携带凭证跨域请求
**修复**: 配置具体域名白名单

## High Priority Gaps

### GAP-4: 无加密静态数据

- 无 Jasypt/Vault 配置加密
- 敏感环境变量 (CANVAS_JWT_SECRET) 走 env — 好
- data_source_config.password 明文 — 坏

### GAP-5: Redis/MySQL 无密码 (dev)

- docker-compose: Redis 无密码, MySQL root/root
- application.yml: Redis password 注释掉
- 仅影响开发环境，但需生产环境配置文档

### GAP-6: 无分布式追踪

- 无 Sleuth/Brave/Micrometer Tracing
- 攻击溯源困难：无法关联跨节点执行链路

## Medium Priority Gaps

### GAP-7: LoginReq 无 Bean Validation

- `@NotBlank` / `@Size` 注解缺失
- 验证在 Service 层命令式执行
- 建议: 添加 `@Valid` + Bean Validation 注解

### GAP-8: 无 XSS 过滤器

- JSON API 无服务端渲染 — 低风险
- 存储型内容 (画布名称/描述) 需注意
- GlobalExceptionHandler 不泄露堆栈 — 好

### GAP-9: V6 表分区未强制

- 仅占位迁移，生产由 DBA 手动执行
- 需分区策略文档

## Security Testing Coverage

| Area | Test Exists | Test Needed |
|------|-------------|------------|
| JWT authentication | ✅ JwtAuthFilterTest | Token expiry, forgery, replay |
| RBAC authorization | ✅ SecurityConfigRoleTest | Cross-role denial |
| HMAC event signing | ❌ | EventReportAuthService unit test |
| SSRF protection | ❌ | OutboundUrlValidator boundary test |
| SQL injection | ❌ (MyBatis-Plus parameterized) | Explicit injection attempt |
| XSS | ❌ | Stored XSS in canvas name/desc |
| Groovy sandbox | ✅ GroovyHandlerValidationTest | Escape attempt tests |
| Data masking | ❌ | DataMaskingUtil edge cases |