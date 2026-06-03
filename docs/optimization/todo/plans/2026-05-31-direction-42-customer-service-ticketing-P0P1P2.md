# 方向㊷：客户服务与工单系统 — 功能清单

> 定位：从"单向营销触达"升级为"营销+服务双向闭环"——工单系统+知识库+服务触发画布+客服工作台+服务分析
> 策略评估：Emplifi/CX Today 2026报告明确：营销、电商、客服在社交渠道上正在融合，客户不区分"这是营销消息还是服务消息"。Braze+Zendesk/Intercom集成已成标配
> 竞品对标：Zendesk(工单+知识库+AI Agent)、Intercom(客服+产品导览+AI chatbot)、Salesforce Service Cloud(CRM+工单统一)
> 建议：**P1建议做**，依赖①营销深度+㉓对话式营销成熟后启动，服务闭环是客户体验的"另一半"

---

## 参考出处

| 出处 | 核心观点 | 链接 |
|------|---------|------|
| Emplifi: Experience Orchestration 2026 | 营销+电商+客服在社交渠道上融合，客户不区分功能边界，需统一协调 | https://emplifi.io/resources/experience-orchestration/ |
| CX Today: 9 Customer Community Platforms 2026 | 社区+支持+倡导融合，支持案例偏转和自助解决是ROI关键 | https://www.cxtoday.com/community-social-engagement/customer-community-platforms-2026/ |
| CMSCWire: CX Platform Shift 2026 | 从集成(Integration)到编排(Orchestration)，打通营销/电商/客服 | https://www.cmswire.com/customer-experience/the-cx-stack-is-breaking-are-end-to-end-platforms-the-fix/ |

---

## 现状盘点

| 功能 | 实现程度 | 差距 |
|------|----------|------|
| 工单系统 | **不存在** | 无工单创建/分配/SLA/升级/关闭流程 |
| 知识库 | **不存在** | 无FAQ/帮助中心/自助服务 |
| 客服触发 | **不存在** | 无法根据用户行为自动创建客服工单 |
| 客服工作台 | **不存在** | 无统一客服视图(对话/工单/用户画像) |

---

## 功能清单

### P0 — 工单与知识库

#### 1. 智能工单系统 [1.5人月]
工单创建(多渠道:邮件/表单/API/社交私信)→自动分类+优先级+分配→SLA追踪+升级→状态流转→关闭+满意度回访

#### 2. 知识库与自助服务 [1.0人月]
FAQ文章/帮助中心/AI搜索/按用户画像推荐相关文章/文章效果统计

```sql
CREATE TABLE support_ticket (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id VARCHAR(64) NOT NULL,
    subject VARCHAR(500) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN' COMMENT 'OPEN/IN_PROGRESS/WAITING_CUSTOMER/RESOLVED/CLOSED',
    priority VARCHAR(10) NOT NULL DEFAULT 'MEDIUM',
    assigned_to VARCHAR(64), sla_deadline DATETIME,
    source VARCHAR(20) COMMENT 'EMAIL/PORTAL/API/SOCIAL/WEBHOOK',
    resolution TEXT, satisfaction_score INT,
    tenant_id BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user (user_id), INDEX idx_status (status), INDEX idx_tenant (tenant_id)
) COMMENT '客服工单';
```

### P1 — 服务+营销联动

#### 3. 服务-营销桥接 [1.0人月]
工单创建/关闭→触发画布(投诉→恢复画布/问题解决→NPS调研)→工单数据写入画像→服务触达通道(InApp客服消息)

#### 4. 客服工作台 [1.0人月]
统一视图(对话+工单+用户画像+历史互动)→快捷回复+知识库侧边栏→工单关联画布执行记录

### P2 — AI客服
#### 5. AI客服Agent [0.8人月] — AI初筛+自动回答(知识库匹配)+升级人工+情感路由
#### 6. 服务分析 [0.5人月] — 工单量/平均解决时间/SLA达成率/满意度趋势/常见问题排行

---

## 工作量: 5.8人月 | 依赖: ①营销深度+㉓对话式营销+⑬画像+㊱反馈调研
