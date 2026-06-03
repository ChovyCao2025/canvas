# 方向⑦：合规+渠道双护城河 — 功能清单

> 定位：不追功能广度，在PIPL合规+中国渠道上做到极致，成为中国营销的"合规基础设施"
> 策略评估：合规是准入条件不是卖点，天花板低；但作为方向②的底座是必须的
> 竞品对标：海外竞品结构性无法满足PIPL+中国渠道（天然壁垒）
> 建议：**不宜独立成方向**，作为方向②私域中台的必要底座

---

## 现状盘点

| 功能 | 实现程度 | 核心现有代码 | 差距 |
|------|----------|-------------|------|
| PIPL合规 | **部分** | MarketingConsentDO+MarketingSuppressionDO+MarketingPolicyService+SuppressionCheckHandler+FrequencyCapHandler+ChannelAvailabilityHandler+QuietHoursHandler+DataMaskingUtil | 营销合规完整，但缺PIPL专项（目的声明/数据主体权利/DPIA/隐私评估） |
| 国密算法 | **不存在** | 仅BCrypt+HMAC-SHA256+SHA-256 | 完全缺失 |
| 信创兼容 | **不存在** | 仅MySQL | 完全缺失 |
| 数据出境 | **不存在** | — | 完全缺失 |
| 抖音/小红书/视频号 | **不存在** | 仅WECHAT/SMS/PUSH/EMAIL/IN_APP | 完全缺失 |
| 通信短信息合规 | **部分** | FrequencyCapHandler+QuietHoursHandler | 缺"退订回T"等运营商要求 |
| 退订/黑名单管理 | **部分** | MarketingSuppressionDO | 缺退订入口页面+渠道级退订 |

---

## 功能清单

### P0 — 合规底线

---

#### 1. PIPL合规引擎 [中复杂度 | 4.0人月]

**现状**：营销合规层较完整（同意/抑制/频率/静默/脱敏），但缺PIPL专项框架

**需补齐**：

| 子功能 | 描述 | 后端 | 前端 |
|--------|------|------|------|
| 数据处理目的声明 | 每个数据使用场景声明处理目的 | ProcessingPurpose表+声明管理 | 目的声明配置页 |
| 明示同意管理 | 用户主动勾选同意（非默认勾选） | ConsentRecord表（含同意时间/IP/版本） | 同意收集弹窗组件 |
| 数据主体权利请求 | 用户可请求访问/更正/删除/可携带自己的数据 | DataSubjectRequest表+处理工作流 | 用户自助页面 |
| 数据删除 | 收到删除请求后在所有系统中删除用户数据 | 数据删除编排服务（依次调用各子系统删除API） | 删除进度查看 |
| DPIA评估 | 高风险数据处理前进行隐私影响评估 | DPIA模板+评估记录 | DPIA评估表单 |
| 隐私政策版本管理 | 隐私政策更新后用户需重新同意 | PrivacyPolicy版本表 | 隐私政策管理页 |

**数据主体权利请求工作流**：
```
1. 用户提交请求（访问/更正/删除/可携带）→ DataSubjectRequest表
2. 系统验证身份（手机验证码/邮箱验证）
3. 自动处理或人工审核：
   - 访问：查询所有相关数据 → 生成数据包 → 发送下载链接
   - 更新：修改指定字段
   - 删除：调用删除编排服务 → 依次删除
   - 可携带：导出用户数据为JSON/CSV
4. 法律要求保留的数据标记为"法律保留"不可删除
5. 记录处理日志
```

**数据库DDL**：
```sql
CREATE TABLE processing_purpose (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL COMMENT '处理目的名称',
    description VARCHAR(500) NOT NULL COMMENT '目的描述',
    legal_basis VARCHAR(30) NOT NULL COMMENT '法律依据 CONSENT/CONTRACT/LEGAL_OBLIGATION/VITAL_INTEREST/PUBLIC_TASK/LEGITIMATE_INTEREST',
    data_categories JSON NOT NULL COMMENT '涉及数据类别',
    retention_days INT NOT NULL COMMENT '数据保留天数',
    is_active TINYINT(1) NOT NULL DEFAULT 1,
    tenant_id BIGINT NOT NULL DEFAULT 0,
    UNIQUE INDEX uk_name (name, tenant_id),
    INDEX idx_tenant (tenant_id)
) COMMENT '数据处理目的声明';

CREATE TABLE consent_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id VARCHAR(64) NOT NULL,
    purpose_id BIGINT NOT NULL,
    consent_type VARCHAR(20) NOT NULL COMMENT 'EXPLICIT/IMPLIED',
    status VARCHAR(20) NOT NULL DEFAULT 'GRANTED' COMMENT 'GRANTED/WITHDRAWN/EXPIRED',
    consent_time DATETIME NOT NULL,
    consent_ip VARCHAR(45) COMMENT '同意时IP',
    policy_version VARCHAR(20) COMMENT '隐私政策版本',
    withdraw_time DATETIME COMMENT '撤回时间',
    tenant_id BIGINT NOT NULL DEFAULT 0,
    INDEX idx_user (user_id),
    INDEX idx_purpose (purpose_id),
    INDEX idx_status (status),
    INDEX idx_tenant (tenant_id)
) COMMENT '用户同意记录';

CREATE TABLE data_subject_request (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id VARCHAR(64) NOT NULL,
    request_type VARCHAR(20) NOT NULL COMMENT 'ACCESS/CORRECT/DELETE/PORTABILITY',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/VERIFIED/PROCESSING/COMPLETED/REJECTED',
    detail JSON COMMENT '请求详情',
    verification_method VARCHAR(20) COMMENT 'SMS/EMAIL',
    verified TINYINT(1) NOT NULL DEFAULT 0,
    result_detail JSON COMMENT '处理结果',
    processed_by VARCHAR(64) COMMENT '处理人',
    tenant_id BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at DATETIME,
    INDEX idx_user (user_id),
    INDEX idx_type_status (request_type, status),
    INDEX idx_created (created_at),
    INDEX idx_tenant (tenant_id)
) COMMENT '数据主体权利请求';

CREATE TABLE privacy_policy_version (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    version VARCHAR(20) NOT NULL COMMENT '版本号',
    content TEXT NOT NULL COMMENT '政策内容(Markdown)',
    effective_date DATE NOT NULL COMMENT '生效日期',
    is_current TINYINT(1) NOT NULL DEFAULT 0,
    tenant_id BIGINT NOT NULL DEFAULT 0,
    INDEX idx_version (version),
    INDEX idx_tenant (tenant_id)
) COMMENT '隐私政策版本';
```

---

#### 2. 通信短信息合规频控 [低复杂度 | 1.0人月]

**现状**：FrequencyCapHandler+QuietHoursHandler已有基础频控

**需补齐（运营商硬性要求）**：

| 合规项 | 描述 | 工信部要求 |
|--------|------|-----------|
| 退订方式 | 短信必须包含"退订回T" | 强制 |
| 签名管理 | 短信必须包含签名【XXX】 | 强制 |
| 模板审核 | 短信内容需经运营商模板审核 | 强制 |
| 频次限制 | 同一用户同一签名同一天最多5条 | 强制 |
| 黑名单同步 | 退订用户同步到运营商黑名单 | 强制 |
| DND名单 | 12321投诉号码自动加入黑名单 | 强制 |

---

#### 3. 退订/黑名单管理增强 [低复杂度 | 1.0人月]

**现状**：MarketingSuppressionDO存在，缺退订入口页面+渠道级退订

**需补齐**：
- 退订页面（方向①已规划，此处不重复）
- 渠道级退订（只退某渠道，保留其他渠道）
- 退订原因收集
- 退订后冷却期（30天后可重新订阅）
- 黑名单批量导入/导出

---

### P1 — 渠道扩展

---

#### 4. 抖音/视频号/小红书渠道集成 [中复杂度 | 6.0人月]

**现状**：仅5个传统渠道，新兴社交平台完全缺失

**需新增**：

| 渠道 | 集成方式 | 核心能力 | 优先级 |
|------|---------|---------|--------|
| 抖音私信 | 抖音开放平台API | 私信触达+订单回传+粉丝事件 | P1 |
| 抖音小程序 | 抖音小程序订阅消息 | 订阅消息+事件追踪 | P1 |
| 视频号 | 微信视频号API | 直播预约+商品卡片 | P2 |
| 小红书 | 小红书开放平台API | 笔记互动+私信+店铺消息 | P2 |

**抖音私信集成**：
```
1. 申请抖音开放平台应用 → 获取app_id/app_secret
2. OAuth2授权 → 获取商家access_token
3. 发送私信API：POST /api/douyin/im/send
4. 事件回调：粉丝关注/取关/私信/订单
5. NodeHandler: SEND_DOUYIN — 抖音私信发送节点
```

**数据库DDL**：
```sql
CREATE TABLE channel_config (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    channel VARCHAR(30) NOT NULL COMMENT '渠道标识 DOUYIN/VIDEO_ACCOUNT/XIAOHONGSHU',
    name VARCHAR(100) NOT NULL COMMENT '渠道名称',
    config JSON NOT NULL COMMENT '渠道配置(app_id/secret/callback_url等)',
    auth_data JSON COMMENT '认证数据(加密存储)',
    status VARCHAR(20) NOT NULL DEFAULT 'INACTIVE' COMMENT 'ACTIVE/INACTIVE/EXPIRED',
    tenant_id BIGINT NOT NULL DEFAULT 0,
    UNIQUE INDEX uk_channel (channel, tenant_id),
    INDEX idx_tenant (tenant_id)
) COMMENT '渠道配置';
```

---

### P2 — 信创与国密

---

#### 5. 国密算法(SM2/SM3/SM4) [中复杂度 | 2.0人月]

**现状**：仅BCrypt+HMAC-SHA256+SHA-256

**需补齐**：

| 算法 | 用途 | 替代 |
|------|------|------|
| SM3 | 哈希（数字摘要） | SHA-256 |
| SM4 | 对称加密（数据加密） | AES-256 |
| SM2 | 非对称加密（签名/验签） | RSA/ECDSA |

**集成方案**：
- 使用Bouncy Castle或Hutol-crypto的SM2/SM3/SM4实现
- 配置化切换：`canvas.crypto.algorithm=SM4/AES256`
- 应用场景：用户PII加密、API签名、数据传输加密

---

#### 6. 信创兼容 [中复杂度 | 3.0人月]

**现状**：仅MySQL

**需适配**：

| 组件 | 国产替代 | 适配方式 |
|------|---------|---------|
| 数据库 | OceanBase/TiDB/GaussDB | MySQL协议兼容（基本零改动） |
| 中间件 | 东方通/宝兰德 | Tomcat替换 |
| 操作系统 | 麒麟/UOS | Java跨平台 |
| CPU | 鲲鹏/飞腾 | JDK+aarch64 |

**关键验证点**：
- MyBatis-Plus SQL兼容性（TiDB/OceanBase的MySQL模式基本兼容）
- Flyway迁移脚本兼容性
- Redis兼容性（Pika/Kvrocks等国产Redis替代）

---

#### 7. 数据出境安全评估 [低复杂度 | 1.0人月]

**现状**：不存在

**需补齐**：

| 子功能 | 描述 |
|--------|------|
| 数据分类分级 | 自动标记数据敏感级别（L1公开/L2内部/L3敏感/L4机密） |
| 出境审批流程 | L3+数据出境需审批 |
| 出境日志 | 记录所有跨境数据传输 |
| 数据本地化 | 敏感数据强制存储在中国境内 |

---

## 工作量估算

| 优先级 | 功能 | 后端人月 | 前端人月 | 测试人月 | 总计 |
|--------|------|---------|---------|---------|------|
| P0 | PIPL合规引擎 | 2.5 | 1.5 | 0.5 | 4.5 |
| P0 | 通信短信息合规 | 0.8 | 0.2 | 0.2 | 1.2 |
| P0 | 退订/黑名单增强 | 0.5 | 0.5 | 0.2 | 1.2 |
| P1 | 抖音/视频号/小红书 | 4.0 | 2.0 | 0.5 | 6.5 |
| P2 | 国密算法 | 1.5 | 0.5 | 0.3 | 2.3 |
| P2 | 信创兼容 | 2.0 | 0.5 | 0.5 | 3.0 |
| P2 | 数据出境 | 0.7 | 0.3 | 0.2 | 1.2 |
| | **合计** | **12.0** | **5.5** | **2.4** | **19.9** |

---

## 不推荐独立成方向的原因

| 风险 | 说明 |
|------|------|
| 合规是准入条件不是卖点 | 客户不会"因为合规好"而购买，只会"因为不合规则不买" |
| 天花板低 | 纯合规+渠道方向收入有限 |
| 渠道维护成本高 | 抖音/小红书API频繁更新，维护负担大 |

**建议**：作为方向②私域中台的必要底座，P0合规功能必须与方向②Phase 0同步完成。

---

## 与方向②的融合方式

| 方向⑦功能 | 方向②阶段 | 融合方式 |
|-----------|-----------|---------|
| PIPL合规引擎 | Phase 0 | 企微触达前强制合规检查 |
| 通信短信息合规 | Phase 0 | SMS触达满足运营商要求 |
| 退订/黑名单 | Phase 0 | 所有渠道退订统一管理 |
| 抖音/小红书渠道 | Phase 2 | 社交渠道触达扩展 |
| 国密/信创 | Phase 2+ | 政企/金融客户要求 |
| 数据出境 | Phase 2+ | 多区域部署场景 |
