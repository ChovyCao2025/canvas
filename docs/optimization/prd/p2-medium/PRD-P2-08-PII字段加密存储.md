# PRD-P2-08-PII字段加密存储

> 本文档为营销画布平台个人信息字段加密需求文档。

---

## 基本信息

| 字段 | 内容 |
|------|------|
| **PRD编号** | PRD-P2-08 |
| **需求名称** | PII字段加密存储 |
| **优先级** | P2 |
| **所属类别** | 安全合规 HIGH 转 P2 |
| **提出日期** | 2026-05-31 |
| **来源** | BMAD 产品设计审查报告 |

---

## 1. 问题描述

### 1.1 现状

用户表中的敏感字段（如 `real_name`, `id_card_number`, `phone_number`）仅通过明文或用户手动掩码存储。

### 1.2 法律依据

- **GDPR Art.32(1)(a)**：采用加密存储技术保护 PII
- **PIPL Art.51**：个人信息应采取加密、去标识化等保护措施

### 1.3 竞品对标

| 竞品 | 策略 |
|------|------|
| Braze | PII 字段字段级加密（AES-256-ECB） |
| Iterable | 数据库字段级加密，Key 在 Vault |

---

## 2. 功能需求

### 2.1 核心功能

1. **加密字段映射**：定义哪些表哪些字段需要加密
2. **Jasypt 插值器**：MyBatis-Plus + Jasypt 插值器自动加密/解密
3. **密钥管理**：密钥存储在 Vault，定时轮换
4. **审计追踪**：记录加密字段被读取的操作（不可见原始值）

### 2.2 实现要点

```sql
CREATE TABLE user (
  user_id BIGINT PRIMARY KEY,
  phone_number VARCHAR(20) NOT NULL, -- 加密字段
  real_name VARCHAR(50),             -- 加密字段
  email VARCHAR(100)                 -- 加密字段
);

-- 存储格式: ENC(base64(encrypted_data))
```

```kotlin
@TableName("user")
data class User(
  @TableId
  @EncryptField(algorithm = "AES-GCM", encryptionKeyEnv = "JASYPT_USER_DB_KEY")
  val phoneNumber: String,

  @EncryptField(algorithm = "AES-GCM", encryptionKeyEnv = "JASYPT_USER_DB_KEY")
  val realName: String?
)
```

---

## 3. 工作量

| 任务 | 工作量（人天） |
|------|--------------|
| 字段加密配置 | 3 |
| MyBatis-Plus 插值器开发 | 4 |
| 密钥轮换任务 | 2 |
| 测试 + 文档 | 1 |
| **总计** | **10** |

---

## 4. 参考资料

- BMIAD 产品设计审查报告 (2026-05-31) - 安全合规 HIGH 转 P2

---

**（文档已保存到 `/Users/photonpay/project/canvas/docs/optimization/prd/p2-medium/PRD-P2-08-PII字段加密存储.md`）**
