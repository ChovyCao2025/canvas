# 方向㉓：对话式营销与社交渠道 — 功能清单

> 定位：从"单向推送"升级为"双向对话"——对话流编排+WhatsApp+社交DM+网站聊天+对话AI集成
> 策略评估：对话式营销是2026核心趋势，Canvas仅有企微/短信/邮件/Push/InApp，无对话式渠道
> 竞品对标：Insider One(对话式营销全渠道)、Braze Content Cards+WhatsApp、Iterable WhatsApp+Social
> 建议：**P2建议做**，企微已部分覆盖对话场景，WhatsApp/社交DM是国际化需求

---

## 参考出处

| 出处 | 核心观点 | 链接 |
|------|---------|------|
| Insider One: Conversational Marketing 2026 | 对话式营销是2026增长引擎，5大渠道(网站聊天/SMS+WhatsApp/社交DM/语音/InApp) | https://insiderone.com/conversational-marketing-strategy/ |
| Klaviyo 8 Marketing Automation Trends 2026 | AI copilots改变营销，实时对话成为标配 | https://www.klaviyo.com/blog/marketing-automation-trends |
| 2026中国社交媒体营销趋势报告 | 生态协同化——平台边界正在消失，社交+电商+私域融合 | https://www.miaozhen.com/all-reports/socialmediamarketing/14014.html |
| Iterable Nova AI Agent | AI Agent对话式交互，定义目标→自动编排多渠道活动 | https://www.businesswire.com/news/home/20250402556728/en/Iterable-Unveils-Iterable-Nova-A-New-AI-Agent-to-Power-Moments-Based-Marketing |
| 2026年优化客户体验必掌握的9大电商营销趋势 | AI重塑全链路体验，社交平台反哺自有渠道 | https://www.effilink.co/resources/show/4084 |
| 2026商家全链路企微营销白皮书 | 企微是私域主阵地，但对话自动化能力不足 | https://pdf.dfcfw.com/pdf/H3_AP202604241821576069_1.pdf |

---

## 现状盘点

| 功能 | 实现程度 | 核心现有代码 | 差距 |
|------|----------|-------------|------|
| 企微消息 | **完整** | WeworkHandler/WeworkDeliveryService | 企微消息发送完整 |
| 短信 | **完整** | SmsHandler/SmsDeliveryService | 短信发送完整 |
| 邮件 | **完整** | EmailHandler/EmailDeliveryService | 邮件发送完整 |
| Push | **完整** | PushHandler/PushDeliveryService | Push发送完整 |
| InApp | **部分** | InAppNotificationHandler(Stub) | 仅存根，无实际InApp消息 |
| WhatsApp | **不存在** | — | 完全缺失 |
| 网站聊天 | **不存在** | — | 完全缺失 |
| 社交DM | **不存在** | — | Instagram/Facebook Messenger未接入 |
| 对话流编排 | **不存在** | — | 无法编排对话式交互流程 |
| 对话AI | **不存在** | — | 无AI对话能力 |
| RCS | **不存在** | — | 富媒体短信未接入 |

### 关键洞察

Canvas当前触达模型是"Fire-and-Forget"：
1. **单向推送**：所有Handler都是"发送消息→记录结果→走下一步"
2. **无回复处理**：无法接收用户回复并据此决策
3. **无对话上下文**：无法维护多轮对话状态
4. **企微是唯一双向渠道**：但企微的对话能力未被编排进画布

对话式营销与当前画布的核心差异：
- 当前画布：触发→筛选→推送→结束
- 对话式画布：触发→对话→接收回复→分支→继续对话→...

---

## 功能清单

### P0 — 对话式渠道接入

---

#### 1. WhatsApp Business API [中复杂度 | 2.0人月]

**现状**：无WhatsApp

**需补齐**：

| 子功能 | 描述 |
|--------|------|
| WhatsApp发送 | 模板消息+富媒体消息发送 |
| WhatsApp接收 | Webhook接收用户回复 |
| 模板管理 | WhatsApp模板注册+审批状态 |
| 会话管理 | 24小时会话窗口管理 |
| 富媒体消息 | 图片/视频/文档/位置/交互按钮 |
| 消息类型 | 营销模板/服务会话/验证码 |

**WhatsApp Handler设计**：

```java
@NodeHandlerType("WHATSAPP")
public class WhatsAppHandler implements NodeHandler {
    // 与现有SmsHandler/EmailHandler同构
    // 额外支持：interactive buttons / list messages / flow messages
    // 返回NodeResult包含messageId，用于追踪回复
}
```

**数据库DDL**：

```sql
CREATE TABLE whatsapp_template (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(200) NOT NULL COMMENT '模板名称',
    template_id VARCHAR(100) NOT NULL COMMENT 'WhatsApp模板ID',
    category VARCHAR(30) NOT NULL COMMENT 'MARKETING/UTILITY/AUTHENTICATION',
    language VARCHAR(10) NOT NULL DEFAULT 'zh_CN',
    body_text TEXT NOT NULL COMMENT '模板正文',
    header_type VARCHAR(20) COMMENT 'TEXT/IMAGE/VIDEO/DOCUMENT',
    buttons JSON COMMENT '按钮配置',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/APPROVED/REJECTED',
    tenant_id BIGINT NOT NULL DEFAULT 0,
    INDEX idx_status (status),
    INDEX idx_tenant (tenant_id)
) COMMENT 'WhatsApp消息模板';
```

---

#### 2. 对话式画布节点 [高复杂度 | 3.0人月]

**现状**：画布仅支持单向推送节点

**需补齐**：

| 节点类型 | 描述 |
|---------|------|
| 发送对话消息 | 发送消息并等待回复(区别于单向发送) |
| 接收回复 | 接收用户回复并解析意图 |
| 条件分支 | 根据回复内容/意图分支 |
| 多轮对话 | 维护对话状态，支持N轮交互 |
| 转人工 | 条件触发转接人工客服 |
| 快速回复 | 提供选项按钮供用户选择 |

**对话式画布示例**：

```
[触发: 企微消息] → [发送对话: "您好，想了解哪类产品？"]
    ↓ (等待回复)
[接收回复] → [意图识别]
    ├── 产品咨询 → [发送对话: "我们有A/B/C三款..."]
    ├── 投诉反馈 → [转人工客服]
    └── 超时未回复 → [发送对话: "还在吗？需要帮助吗？"]
```

**对话状态管理**：

```sql
CREATE TABLE conversation_session (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    canvas_id BIGINT NOT NULL,
    execution_id VARCHAR(64) NOT NULL,
    user_id VARCHAR(64) NOT NULL,
    channel VARCHAR(20) NOT NULL COMMENT 'WEWORK/WHATSAPP/SOCIAL_DM/WEB_CHAT',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/COMPLETED/EXPIRED/TRANSFERRED',
    turn_count INT NOT NULL DEFAULT 0 COMMENT '对话轮次',
    context JSON COMMENT '对话上下文(提取的意图/实体)',
    last_message_at DATETIME NOT NULL,
    expires_at DATETIME COMMENT '会话过期时间',
    tenant_id BIGINT NOT NULL DEFAULT 0,
    INDEX idx_user (user_id),
    INDEX idx_canvas (canvas_id),
    INDEX idx_status (status),
    INDEX idx_tenant (tenant_id)
) COMMENT '对话会话';

CREATE TABLE conversation_message (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    session_id BIGINT NOT NULL,
    direction VARCHAR(10) NOT NULL COMMENT 'OUTBOUND/INBOUND',
    message_type VARCHAR(20) NOT NULL COMMENT 'TEXT/IMAGE/INTERACTIVE',
    content JSON NOT NULL COMMENT '消息内容',
    intent VARCHAR(50) COMMENT '识别的意图(INBOUND)',
    processed TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否已被画布处理',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_session (session_id),
    INDEX idx_processed (processed)
) COMMENT '对话消息';
```

---

### P1 — 社交渠道与网站聊天

---

#### 3. 社交DM渠道 [中复杂度 | 1.5人月]

**现状**：无社交DM

**需补齐**：

| 渠道 | 描述 | 参考出处 |
|------|------|---------|
| Instagram DM | Instagram私信营销 | Insider One: Social DMs |
| Facebook Messenger | Messenger消息营销 | Insider One: Social DMs |
| 小红书私信 | 小红书私信(中国市场) | 2026社媒趋势报告 |
| 抖音私信 | 抖音私信(中国市场) | 2026社媒趋势报告 |

---

#### 4. 网站聊天组件 [中复杂度 | 1.5人月]

**现状**：无网站聊天

**需补齐**：

| 子功能 | 描述 |
|--------|------|
| 聊天组件 | 嵌入网站的聊天Widget |
| 主动触发 | 根据用户行为主动发起对话 |
| 离线消息 | 客服离线时留言+邮件通知 |
| 预聊天表单 | 对话前收集用户信息 |
| 对话路由 | 按意图路由到不同画布/人工 |

---

### P2 — 对话AI与高级能力

---

#### 5. 对话AI集成 [中复杂度 | 1.5人月]

**描述**：AI驱动的对话能力

| 子功能 | 描述 |
|--------|------|
| 意图识别 | 基于NLP识别用户意图 |
| 实体提取 | 从对话中提取关键实体(产品/订单号/日期) |
| 情感分析 | 分析用户情感(正面/负面/中性) |
| 对话生成 | AI生成回复(接入LLM) |
| 知识库 | 产品FAQ/常见问题知识库 |
| 人机协作 | AI无法处理时无缝转人工 |

---

#### 6. RCS富媒体消息 [低复杂度 | 0.5人月]

**描述**：RCS(Rich Communication Services)富媒体短信

| 子功能 | 描述 |
|--------|------|
| RCS模板 | 富媒体短信模板(图片/按钮/轮播) |
| RCS发送 | 通过运营商RCS通道发送 |
| 回复处理 | 接收RCS回复 |

---

## 工作量估算

| 优先级 | 功能 | 后端人月 | 前端人月 | 测试人月 | 总计 |
|--------|------|---------|---------|---------|------|
| P0 | WhatsApp Business API | 1.5 | 0.5 | 0.2 | 2.2 |
| P0 | 对话式画布节点 | 2.5 | 0.5 | 0.3 | 3.3 |
| P1 | 社交DM渠道 | 1.0 | 0.5 | 0.2 | 1.7 |
| P1 | 网站聊天组件 | 1.0 | 0.5 | 0.2 | 1.7 |
| P2 | 对话AI集成 | 1.0 | 0.5 | 0.2 | 1.7 |
| P2 | RCS富媒体消息 | 0.3 | 0.2 | 0.1 | 0.6 |
| | **合计** | **7.3** | **2.7** | **1.2** | **11.2** |

---

## 执行顺序

```
Sprint 1 (P0-WhatsApp): WhatsApp接入 — 2.2人月
  → 产出：WhatsAppHandler+模板管理+Webhook接收

Sprint 2 (P0-对话): 对话式画布节点 — 3.3人月
  → 产出：对话会话+多轮对话+转人工

Sprint 3 (P1-社交): 社交DM渠道 — 1.7人月
  → 产出：Instagram/Messenger/小红书私信

Sprint 4 (P1-网站): 网站聊天组件 — 1.7人月
  → 产出：聊天Widget+主动触发+离线留言

Sprint 5 (P2-AI): 对话AI+RCS — 2.3人月
  → 产出：意图识别+情感分析+AI回复+RCS
```

---

## 风险评估

| 风险 | 影响 | 缓解措施 |
|------|------|---------|
| WhatsApp审核 | 模板审核周期长(1-3天) | 预审批模板库+提前提交 |
| 对话超时 | 用户不回复导致画布挂起 | 超时自动跳转+会话过期 |
| 渠道政策 | 社交平台API限制变更 | 多渠道降级+官方API优先 |
| AI回复质量 | AI生成不当回复 | 人工审核+敏感词过滤+回复模板限制 |
| 数据合规 | 对话内容含敏感信息 | 加密存储+脱敏展示+合规审查 |

---

## 与其他方向的关系

| 方向 | 与㉓的关系 |
|------|----------|
| ① 营销自动化深度 | 对话式是触达渠道的重要扩展 |
| ② 私域运营中台 | 企微对话是私域核心交互方式 |
| ⑦ 合规渠道护城河 | WhatsApp/社交DM合规要求 |
| ⑪ 开放平台 | 对话式渠道通过开放API对接 |
| ④ AI原生平台 | 对话AI是AI平台的重要应用场景 |
| ㉔ 客户生命周期智能 | 对话意图识别→生命周期阶段判断 |
