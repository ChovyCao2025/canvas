# PRD-P2-06-事件上报密钥管理

> 本文档为营销画布平台事件上报 API 密钥加密需求文档。

---

## 基本信息

| 字段 | 内容 |
|------|------|
| **PRD编号** | PRD-P2-06 |
| **需求名称** | 事件上报密钥管理 |
| **优先级** | P2 |
| **所属类别** | 安全合规 MEDIUM |
| **提出日期** | 2026-05-31 |
| **来源** | BMAD 产品设计审查报告 |

---

## 1. 问题描述

### 1.1 现状

配置文件中硬编码事件上报密钥：

```yaml
canvas:
  event:
    report:
      secret: canvas-event-report-secret-2026!!  # 硬编码，无加密
      api-key: xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
```

### 1.2 痛点

1. **密钥泄露**：配置文件提交到 Git，所有协作者可获取
2. **密钥轮换困难**：无法在不升级应用的情况下更换密钥
3. **密钥滥用**：泄露后任何人可伪造事件数据上报
4. **多租户风险**：不同租户共用密钥，无法区分来源租户

### 1.3 竞品对标

| 竞品 | 密钥管理策略 |
|------|------------|
| Braze | 使用 App Secret + API Key，密钥存储在 Dashboard |
| Iterable | OAuth Token，存储在环境变量 |

---

## 2. 目标与价值

### 2.1 用户故事

- **产品经理**：作为平台负责人，我希望加密事件上报密钥，防止未授权者伪造事件数据影响分析。

### 2.2 成功指标

- 配置文件中的 API 密钥加密存储（使用 Jasypt `ENC()` 格式）
- 所有事件上报请求携带签名 `Authorization: Bearer {secret}`，服务端验证签名
- 支持密钥冷热存储：主密钥存储在 Vault，应用启动时解密

### 2.3 不做会怎样

- 泄露的密钥被用于伪造事件，导致 CDP 数据污染
- 无法区分联名账户的子账号事件上报来源

---

## 3. 功能需求

### 3.1 核心功能

1. **密钥加密存储**：`application.yml` 只存储 `ENC()` 加密后的密文
2. **签名验证机制**：
   - 请求头：`X-Canvas-Secret: {encoded_secret}`
   - 签名字段：`timestamp`, `nonce`, `payload_hash`
   - 算法：HMAC-SHA256
3. **密钥版本管理**：支持多版本密钥（如 `secret-v1`, `secret-v2`），切换平滑
4. **密钥轮换计划**：通过后台「事件上报密钥管理」页面轮换密钥

#### 3.1.1 签名验证代码示例

```kotlin
@Component
class EventReportInterceptor : HandlerInterceptor() {
    override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {
        val secret = getDecryptedSecret()
        val timestamp = request.getHeader("X-Canvas-Timestamp")
        val nonce = request.getHeader("X-Canvas-Nonce")
        val payload = request.reader.readText()

        val calculatedHash = generateHmacSha256Secret(secret, "$timestamp.$nonce.$payload")

        val clientHash = request.getHeader("X-Canvas-Signature")
        if (!MessageDigest.isEqual(calculatedHash.encodeToByteArray(), clientHash.encodeToByteArray())) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid signature")
            return false
        }

        return true
    }
}
```

### 3.2 详细描述

**密钥轮换流程**：

1. 管理员进入「设置 → 事件上报密钥管理」
2. 点击「生成新密钥」，系统生成 32 字符随机密钥
3. 指定生效时间（如 T+24h）
4. 旧密钥标记为「待废弃」，新密钥标记为「活跃」
5. 验证新密钥验证成功后，废弃旧密钥

**多租户密钥隔离**：

提供 `api-key-{tenantId}` 格式，每个租户独立密钥：

```yaml
canvas:
  event:
    report:
      tenant-key:
        tenant_1: ENC(3xZ9mN2pR4sT6vW8xY0zA2bC4dE6fG8hJ)
        tenant_2: ENC(4kL9mN5qR6sT8wY1zA3bC5dE7fG9hJ2kL)
```

---

## 4. 非功能需求

### 4.1 性能要求

- 签名验证延迟：< 50ms（支持 QPS > 1000）

### 4.2 安全要求

- 支持红黑名单密钥：主动封禁恶意租户的密钥
- 签名有效期：timestamp + 300 秒（防重放攻击）

### 4.3 可用性要求

- 密钥轮换期间允许「双密钥并行验证」，无缝切换

---

## 5. 验收标准

- [ ] 事件上报接口 `/canvas/event/report/*` 需要签名验证
- [ ] 明文密钥提交到 Git 时，DevOps 钩子返回失败
- [ ] 测试签名验证（未签名返回 401，错误签名返回 401）
- [ ] 测试密钥轮换：新密钥生效后，旧密钥立即失效

---

## 6. 技术建议

### 6.1 涉及模块

- **后端**：
  - `canvas-api` - 事件上报接口 + 签名拦截器
  - `canvas-common` - 密钥加密配置

### 6.2 技术要点

1. 使用 HMAC-SHA256 而非简单 Token 进行签名
2. 支持多版本密钥灰度启用（JSON 格式：`{"current": "v2", "pending": "v1"}`）

### 6.3 预估工作量

| 任务 | 工作量（人天） |
|------|--------------|
| 签名机制开发 | 3 |
| 密钥加密存储 | 2 |
| 密钥轮换管理 UI | 2 |
| 测试 + 文档 | 1 |
| **总计** | **8** |

---

## 7. 依赖与风险

### 7.1 前置依赖

- 确认现有事件上报客户端（营销 sprinkle、移动端 App）能升级到新签名协议
- 需要区分内部事件上报（内部服务调用）和外部租户上报

### 7.2 风险

1. **向后兼容性**：旧版客户端无法通过新签名协议验证
   - 缓解：支持「旧密钥 + 新密钥」双签名验证 1 周后废弃旧密钥

2. **签名伪造攻击**：攻击者拦截用户请求并替换 `X-Canvas-Signature`
   - 缓解：添加随机 `nonce` 字段防重放

---

## 8. 参考资料

- BMAD 产品设计审查报告 (2026-05-31) - 安全合规 MEDIUM
- OAuth 2.0 HMAC 签名指南
- AWS SigV4 签名算法参考

---

**（文档已保存到 `/Users/photonpay/project/canvas/docs/optimization/prd/p2-medium/PRD-P2-06-事件上报密钥管理.md`）**
