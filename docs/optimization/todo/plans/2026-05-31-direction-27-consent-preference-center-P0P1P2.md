# 方向㉗：用户偏好与同意管理中心 — 功能清单

> 定位：从"二元OPT_IN/OPT_OUT"升级为"全渠道偏好中心"——同意管理+偏好中心+退订管理+合规审计+数据主体权利
> 策略评估：GDPR/个保法/PIPL合规是2026营销平台底线，无偏好中心=无合规触达；OneTrust/Klaviyo/OneSignal均已标配
> 竞品对标：OneTrust(Consent+Preference一体化)、Usercentrics、HYPERS(国内CPM平台)、OneSignal Preference Center、Optimove Preference Center
> 建议：**P0必须做**，⑦合规渠道护城河的核心组成部分，无同意管理=营销触达不合规

---

## 参考出处

| 出处 | 核心观点 | 链接 |
|------|---------|------|
| OneTrust: Scalable Preference Centers for Marketing Operations | 偏好中心从合规机制→营销运营核心，统一同意+偏好+隐私请求，跨渠道同步执行 | https://www.onetrust.com/blog/how-scalable-preference-centers-support-better-marketing-operations/ |
| Usercentrics: Consent and Preference Management 2026 | Universal Consent Management，12种数据采集意图预置，实时下游调用支持 | https://usercentrics.com/knowledge-hub/what-is-universal-consent-and-preference-management/ |
| HYPERS: 用户同意与偏好管理平台 | 企业级CPM平台，预置12种数据采集意图+10种请求类型，个保法合规一站式 | https://www.hypers.com/products/marketing-cloud/consent-manager |
| OneSignal: 偏好设置中心 | 主题/类别/频率控制+渠道管理+数据合规+删除用户数据API | https://documentation.onesignal.com/docs/cn/preference-center |
| emfluence: Email Preference Center Best Practices | 偏好中心减少20-30%退订率，频率/主题/渠道三级控制 | https://emarketingplatform.com/blog/preference-center-best-practices/ |
| 2026 SaaS出海合规红线 | GDPR执行进入自动化阶段，营销工具需"数据来源合法性证明"，AI客服需EU AI Act标注 | https://www.knitpeople.com.cn/blog/2026-saas-global-expansion-compliance-redlines-eor |
| 纷享销客: 2026智能营销数据安全合规指南 | 算法解释权+动态授权+原子化权限+合规防火墙+销毁闭环管理 | https://www.fxiaoke.com/crm/information-87779.html |

---

## 现状盘点

| 功能 | 实现程度 | 核心现有代码 | 差距 |
|------|----------|-------------|------|
| 渠道级授权 | **完整** | MarketingConsentDO(userId/channel/consentStatus/source) | 仅OPT_IN/OPT_OUT二元，无偏好粒度 |
| 抑制检查 | **完整** | SuppressionCheckHandler + MarketingPolicyService | 抑制名单完整，但无偏好中心UI |
| 频率控制 | **完整** | MarketingPolicyService.consumeFrequency() | Redis频控完整，但无用户自选频率 |
| 静默时段 | **完整** | MarketingPolicyService.quietHoursAllowed() | 默认22:00-08:00，但无用户自设 |
| 渠道可用性 | **完整** | MarketingPolicyService.channelAvailable() | 检查渠道可达，但无偏好中心 |
| 偏好中心UI | **不存在** | — | 完全缺失 |
| 主题级同意 | **不存在** | — | 无营销主题/类别级同意 |
| 频率偏好 | **不存在** | — | 用户无法自选接收频率 |
| 渠道偏好 | **不存在** | — | 用户无法自选接收渠道 |
| 同意审计 | **不存在** | — | 无同意变更日志 |
| 数据主体权利 | **不存在** | — | 无访问/删除/导出请求 |
| 个保法合规 | **不存在** | — | 无PIPL/GDPR合规框架 |

### 关键洞察

MarketingConsentDO现状：
1. **粒度粗**：仅userId+channel+OPT_IN/OPT_OUT，无主题/类别/频率维度
2. **无来源追踪**：source字段存在但无结构化记录(哪个表单/哪个页面/哪个时间)
3. **无过期机制**：同意无有效期，无法满足"条款变更需重新获取同意"
4. **无偏好中心**：用户无法自助管理接收偏好，只能二元退订

MarketingPolicyService已具备的能力：
- **consentAllowed()**：检查渠道级授权，可复用
- **suppressionAllowed()**：检查抑制名单，可复用
- **consumeFrequency()**：频控消耗，可复用
- **quietHoursAllowed()**：静默时段，可复用
- **channelAvailable()**：渠道可用性，可复用

缺失的核心环节：
1. **偏好中心**：用户自助管理接收偏好的UI+API
2. **主题级同意**：按营销主题(促销/资讯/活动/服务)分别授权
3. **同意审计**：所有同意变更的可追溯记录
4. **数据主体权利**：访问/纠正/删除/导出/限制处理

---

## 功能清单

### P0 — 同意管理与偏好中心

---

#### 1. 同意管理增强 [中复杂度 | 2.0人月]

**现状**：MarketingConsentDO仅OPT_IN/OPT_OUT

**需补齐**：

| 子功能 | 描述 |
|--------|------|
| 主题级同意 | 按营销主题(促销/资讯/活动/服务通知/调研)分别授权 |
| 渠道-主题矩阵 | 每个主题可独立选择渠道(如：促销仅邮件+Push，资讯仅邮件) |
| 同意来源追踪 | 记录同意来源(表单/偏好中心/导入/API/条款变更) |
| 同意有效期 | 条款变更时需重新获取同意，过期同意自动失效 |
| 双重确认(Double Opt-In) | 邮件/短信确认订阅，防止误操作 |
| 合规框架 | GDPR/PIPL/CAN-SPAM/CASL多框架适配 |

**同意模型升级**：

```
当前: userId + channel → OPT_IN/OPT_OUT
升级: userId + topic + channel → consent_status + source + version + expires_at
```

**数据库DDL**：

```sql
-- 营销主题定义
CREATE TABLE marketing_topic (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    topic_key VARCHAR(50) NOT NULL COMMENT 'PROMO/NEWS/EVENT/SERVICE/SURVEY',
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    available_channels VARCHAR(200) NOT NULL COMMENT 'EMAIL,SMS,PUSH,WEWORK,WHATSAPP',
    required TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否必须(服务通知不可退)',
    sort_order INT NOT NULL DEFAULT 0,
    tenant_id BIGINT NOT NULL DEFAULT 0,
    UNIQUE INDEX uk_key (topic_key),
    INDEX idx_tenant (tenant_id)
) COMMENT '营销主题定义';

-- 同意记录(替代原marketing_consent)
CREATE TABLE consent_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id VARCHAR(64) NOT NULL,
    topic_key VARCHAR(50) NOT NULL COMMENT '营销主题',
    channel VARCHAR(20) NOT NULL COMMENT '渠道',
    consent_status VARCHAR(20) NOT NULL COMMENT 'OPT_IN/OPT_OUT/PENDING',
    source VARCHAR(30) NOT NULL COMMENT 'PREFERENCE_CENTER/FORM/IMPORT/API/TERMS_CHANGE',
    source_ref VARCHAR(100) COMMENT '来源引用(表单ID/导入批次)',
    consent_version INT NOT NULL DEFAULT 1 COMMENT '同意版本(条款变更递增)',
    double_opt_in_confirmed TINYINT(1) NOT NULL DEFAULT 0,
    expires_at DATETIME COMMENT '同意过期时间',
    ip_address VARCHAR(45) COMMENT '授权IP',
    user_agent VARCHAR(500) COMMENT '授权UA',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    tenant_id BIGINT NOT NULL DEFAULT 0,
    UNIQUE INDEX uk_user_topic_ch (user_id, topic_key, channel),
    INDEX idx_user (user_id),
    INDEX idx_status (consent_status),
    INDEX idx_tenant (tenant_id)
) COMMENT '同意记录';

-- 同意变更审计
CREATE TABLE consent_audit (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id VARCHAR(64) NOT NULL,
    topic_key VARCHAR(50) NOT NULL,
    channel VARCHAR(20) NOT NULL,
    old_status VARCHAR(20),
    new_status VARCHAR(20) NOT NULL,
    change_source VARCHAR(30) NOT NULL,
    change_reason VARCHAR(200),
    changed_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    tenant_id BIGINT NOT NULL DEFAULT 0,
    INDEX idx_user (user_id),
    INDEX idx_time (changed_at),
    INDEX idx_tenant (tenant_id)
) COMMENT '同意变更审计';

-- 隐私条款版本管理
CREATE TABLE privacy_policy_version (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    policy_type VARCHAR(20) NOT NULL COMMENT 'PRIVACY_POLICY/TERMS_OF_SERVICE/CONSENT_POLICY',
    version INT NOT NULL,
    content_url VARCHAR(500) NOT NULL,
    effective_at DATETIME NOT NULL,
    reconsent_required TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否需要重新获取同意',
    tenant_id BIGINT NOT NULL DEFAULT 0,
    INDEX idx_type_version (policy_type, version),
    INDEX idx_tenant (tenant_id)
) COMMENT '隐私条款版本';
```

---

#### 2. 偏好中心 [中复杂度 | 2.0人月]

**现状**：无偏好中心

**需补齐**：

| 子功能 | 描述 |
|--------|------|
| 偏好中心页面 | 用户自助管理接收偏好的Web页面 |
| 主题开关 | 每个营销主题的开关(促销/资讯/活动/服务) |
| 渠道选择 | 每个主题可选渠道(邮件/短信/Push/企微) |
| 频率偏好 | 选择接收频率(实时/每日摘要/每周摘要/每月摘要) |
| 暂停接收 | 暂停接收N天(如假期模式) |
| 全局退订 | 一键退订所有营销 |
| 嵌入式组件 | 偏好中心可嵌入网站/App(iframe/SDK) |
| 条件展示 | 根据用户状态/地区条件展示不同选项 |

**偏好中心API设计**：

```
GET  /api/v1/preferences/{userId}         → 获取用户偏好
PUT  /api/v1/preferences/{userId}         → 更新用户偏好
GET  /api/v1/preferences/{userId}/topics  → 获取主题列表+当前状态
PUT  /api/v1/preferences/{userId}/topics/{topicKey}/channels → 更新主题渠道
POST /api/v1/preferences/{userId}/pause   → 暂停接收(days=N)
POST /api/v1/preferences/{userId}/unsubscribe-all → 全局退订
```

**数据库DDL**：

```sql
CREATE TABLE user_preference (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id VARCHAR(64) NOT NULL,
    frequency_preference VARCHAR(20) NOT NULL DEFAULT 'REALTIME' COMMENT 'REALTIME/DAILY_DIGEST/WEEKLY_DIGEST/MONTHLY_DIGEST',
    pause_until DATETIME COMMENT '暂停接收截止日',
    language VARCHAR(10) COMMENT '偏好语言',
    timezone VARCHAR(50) COMMENT '偏好时区',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    tenant_id BIGINT NOT NULL DEFAULT 0,
    UNIQUE INDEX uk_user (user_id),
    INDEX idx_tenant (tenant_id)
) COMMENT '用户偏好';
```

---

### P1 — 数据主体权利与合规

---

#### 3. 数据主体权利 [中复杂度 | 1.5人月]

**现状**：无数据主体权利

**需补齐**：

| 权利 | 描述 | 对标法规 |
|------|------|---------|
| 访问权 | 用户请求查看其所有个人数据 | GDPR Art.15 / PIPL Art.44 |
| 纠正权 | 用户请求修正错误数据 | GDPR Art.16 / PIPL Art.46 |
| 删除权 | 用户请求删除其个人数据(被遗忘权) | GDPR Art.17 / PIPL Art.47 |
| 导出权 | 用户请求导出其数据(数据可携带) | GDPR Art.20 / PIPL Art.45 |
| 限制处理权 | 用户请求限制其数据处理 | GDPR Art.18 |
| 反对权 | 用户反对自动化决策(含画像) | GDPR Art.21 / PIPL Art.24 |

**数据库DDL**：

```sql
CREATE TABLE data_subject_request (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id VARCHAR(64) NOT NULL,
    request_type VARCHAR(20) NOT NULL COMMENT 'ACCESS/CORRECT/DELETE/EXPORT/RESTRICT/OBJECT',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/PROCESSING/COMPLETED/REJECTED',
    request_detail JSON COMMENT '请求详情(纠正字段/导出格式)',
    result_url VARCHAR(500) COMMENT '结果下载URL(导出)',
    completed_at DATETIME,
    expires_at DATETIME COMMENT '结果过期时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    tenant_id BIGINT NOT NULL DEFAULT 0,
    INDEX idx_user (user_id),
    INDEX idx_status (status),
    INDEX idx_tenant (tenant_id)
) COMMENT '数据主体请求';
```

---

#### 4. 合规仪表盘 [低复杂度 | 0.8人月]

**现状**：无合规可视化

**需补齐**：

| 子功能 | 描述 |
|--------|------|
| 同意率看板 | 各主题/渠道的同意率趋势 |
| 退订率看板 | 各主题/渠道的退订率趋势 |
| 数据请求统计 | 数据主体请求统计+处理时效 |
| 条款版本管理 | 隐私条款版本管理+生效状态 |
| 合规扫描 | 定期扫描未获同意的触达记录 |
| 风险告警 | 同意率骤降/退订率飙升告警 |

---

### P2 — 高级合规能力

---

#### 5. 合规防火墙 [中复杂度 | 1.0人月]

**描述**：系统间数据传输的合规拦截层

| 子功能 | 描述 |
|--------|------|
| 数据流审计 | 审计所有跨系统数据调用 |
| 敏感字段拦截 | 自动拦截明文手机号/身份证等查询 |
| 动态脱敏 | 返回数据实时脱敏(手机号→138****1234) |
| 合规中间件 | 系统间数据传输的合规检查层 |

---

#### 6. 智能同意优化 [低复杂度 | 0.5人月]

**描述**：AI驱动的同意率优化

| 子功能 | 描述 |
|--------|------|
| 同意率预测 | 预测不同授权时机/文案的同意率 |
| 最佳授权时机 | 在用户最可能同意的时刻弹出授权 |
| A/B授权测试 | 不同授权文案/样式的A/B测试 |
| 退订挽回 | 退订后触发偏好中心(降级而非离开) |

---

## 工作量估算

| 优先级 | 功能 | 后端人月 | 前端人月 | 测试人月 | 总计 |
|--------|------|---------|---------|---------|------|
| P0 | 同意管理增强 | 1.5 | 0.5 | 0.2 | 2.2 |
| P0 | 偏好中心 | 1.2 | 0.8 | 0.2 | 2.2 |
| P1 | 数据主体权利 | 1.0 | 0.5 | 0.2 | 1.7 |
| P1 | 合规仪表盘 | 0.5 | 0.3 | 0.1 | 0.9 |
| P2 | 合规防火墙 | 0.7 | 0.3 | 0.1 | 1.1 |
| P2 | 智能同意优化 | 0.3 | 0.2 | 0.1 | 0.6 |
| | **合计** | **5.2** | **2.6** | **0.9** | **8.7** |

---

## 执行顺序

```
Sprint 1 (P0-同意): 同意管理增强 — 2.2人月
  → 产出：主题级同意+渠道矩阵+审计+条款版本+双重确认

Sprint 2 (P0-偏好): 偏好中心 — 2.2人月
  → 产出：偏好中心UI+API+嵌入式组件+暂停+全局退订

Sprint 3 (P1-权利): 数据主体权利 — 1.7人月
  → 产出：访问/删除/导出/纠正请求+自动化处理

Sprint 4 (P1-仪表盘): 合规仪表盘 — 0.9人月
  → 产出：同意率+退订率+数据请求统计+风险告警

Sprint 5 (P2-高级): 合规防火墙+智能优化 — 1.7人月
  → 产出：数据流审计+脱敏+同意率优化
```

---

## 风险评估

| 风险 | 影响 | 缓解措施 |
|------|------|---------|
| 同意数据迁移 | 原MarketingConsentDO数据需迁移到新模型 | 兼容读取+渐进迁移+双写过渡 |
| 偏好中心滥用 | 恶意批量修改他人偏好 | 身份验证+CSRF防护+操作审计 |
| 删除级联 | 用户请求删除数据时级联删除范围 | 数据分类+保留期限+法律例外 |
| 跨渠道同步 | 偏好变更未及时同步到所有触达渠道 | 事件驱动+最终一致性+定期校验 |
| 条款变更风暴 | 条款变更需重新获取同意，可能触发大规模退订 | 渐进式通知+偏好中心引导+降级选项 |

---

## 与其他方向的关系

| 方向 | 与㉗的关系 |
|------|----------|
| ⑦ 合规渠道护城河 | 同意管理是合规的核心组成部分 |
| ⑫ 多租户SaaS化 | 不同租户/地区需不同合规框架(GDPR/PIPL) |
| ① 营销自动化深度 | 偏好中心数据驱动个性化触达 |
| ㉓ 对话式营销 | 对话中获取同意(如WhatsApp opt-in) |
| ⑨ 营销数据中台 | 同意状态影响数据可用性和触达效果统计 |
| ㉕ 计费与用量计量 | 合规功能可作为套餐功能权限(高级合规=付费) |
| ⑯ 协作与权限管理 | 合规审计需要权限管理支撑 |
