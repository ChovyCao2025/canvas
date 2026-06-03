# 方向㉚：多品牌与代理平台 — 功能清单

> 定位：从"单品牌SaaS"升级为"多品牌代理平台"——品牌管理+白标定制+代理层级+客户门户+品牌隔离+跨品牌用户
> 策略评估：SaaS代理模式(White-Label/Agency)是2026营销自动化重要商业模式，Vendasta/HubSpot/GoHighLevel均已标配
> 竞品对标：Vendasta(Agency Platform+白标CRM)、GoHighLevel(White-Label Agency CRM)、Bird(Agency Workflow)、HubSpot(Multi-Brand)
> 建议：**P2建议做**，⑫多租户基础完善后再做，代理模式是营收倍增器但非刚需

---

## 参考出处

| 出处 | 核心观点 | 链接 |
|------|---------|------|
| Vendasta: 21 Best White Label Marketing Software 2026 | Agency Platform核心：白标CRM+自动化+计费+客户门户，一站式代理解决方案 | https://www.perspective.co/article/white-label-marketing-software |
| Improvado: White Label Marketing Reports 2026 | 白标报告：数据采集架构→模板设计→自动化，Agency向客户交付品牌化报告 | https://improvado.io/blog/white-label-marketing-reports |
| Bird: Agency Workflow Automation | 智能工作流+多步审批链+并行审核+跨渠道协调，Agency专用 | https://bird.com/en/automate/workflows |
| 7 Best Marketing Automation for Agencies 2026 | 代理模式关键：多客户管理+白标+独立报表+批量操作+客户门户 | https://ustechautomations.com/resources/blog/best-marketing-automation-software-agencies-2026 |
| SlickText: Marketing Automation for Agencies 2026 | Agency工具选型：CRM+社交+自动化+白标，HubSpot Agency首选 | https://www.slicktext.com/blog/2026/01/marketing-automation-agencies-top-tools-2026/ |
| 2026私域增长新范式 | 经销商多级私域的专业工具成细分赛道核心增长 | https://www.sohu.com/a/1024688650_120701308 |

---

## 现状盘点

| 功能 | 实现程度 | 核心现有代码 | 差距 |
|------|----------|-------------|------|
| 租户管理 | **部分** | TenantDO(tenantKey/name/status/planCode) | 单租户结构，无品牌/代理层级 |
| 用户权限 | **部分** | RBAC(ADMIN/OPERATOR) | 角色简单，无品牌级权限 |
| 白标定制 | **不存在** | — | 无Logo/颜色/域名/邮件模板定制 |
| 品牌管理 | **不存在** | — | 无品牌实体，无法多品牌运营 |
| 代理层级 | **不存在** | — | 无代理/经销商层级管理 |
| 客户门户 | **不存在** | — | 无面向终端客户的自助门户 |
| 跨品牌用户 | **不存在** | — | 同一用户不能属于多个品牌 |
| 品牌隔离 | **不存在** | — | 数据/配置/模板按品牌隔离 |

### 关键洞察

TenantDO vs 品牌的关系：
- **TenantDO**：面向SaaS运营(哪个客户租用系统)
- **Brand**：面向终端用户(哪个品牌在触达)
- 当前：1个Tenant = 1个品牌 = 1套配置
- 目标：1个Tenant可管理多个品牌，每个品牌独立配置+数据隔离

代理模式的三层架构：
```
平台(Platform) → 代理(Agency) → 品牌(Brand) → 终端客户(End Customer)
   ↓                ↓               ↓
  运营管理         客户管理        营销执行
```

中国市场特殊性(私域多级经销商)：
- 品牌方→一级经销商→二级经销商→终端门店
- 每一级都需要独立的营销画布+用户池
- 但品牌资产(Logo/素材/合规)需统一管控

---

## 功能清单

### P0 — 多品牌管理

---

#### 1. 品牌管理 [中复杂度 | 1.5人月]

**现状**：无品牌实体

**需补齐**：

| 子功能 | 描述 |
|--------|------|
| 品牌CRUD | 创建/编辑/归档品牌 |
| 品牌配置 | Logo/主色/辅色/字体/品牌名/品牌描述 |
| 品牌域名 | 每个品牌可绑定独立域名 |
| 品牌邮箱 | 发件人名称/邮箱按品牌配置 |
| 品牌模板 | 各渠道消息模板按品牌管理 |
| 数据隔离 | 品牌间用户/画布/数据严格隔离 |
| 用户归属 | 用户可属于一个或多个品牌 |

**数据库DDL**：

```sql
CREATE TABLE brand (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL COMMENT '所属租户',
    brand_key VARCHAR(50) NOT NULL COMMENT '品牌标识',
    name VARCHAR(200) NOT NULL COMMENT '品牌名称',
    description VARCHAR(500),
    logo_url VARCHAR(500),
    primary_color VARCHAR(10) COMMENT '主色#RRGGBB',
    secondary_color VARCHAR(10) COMMENT '辅色',
    font_family VARCHAR(100),
    domain VARCHAR(200) COMMENT '品牌域名',
    from_name VARCHAR(100) COMMENT '发件人名称',
    from_email VARCHAR(200) COMMENT '发件人邮箱',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/INACTIVE/ARCHIVED',
    settings JSON COMMENT '品牌级配置',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE INDEX uk_tenant_key (tenant_id, brand_key),
    INDEX idx_tenant (tenant_id)
) COMMENT '品牌';

-- 用户-品牌关联
CREATE TABLE user_brand (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id VARCHAR(64) NOT NULL,
    brand_id BIGINT NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'VIEWER' COMMENT 'OWNER/ADMIN/EDITOR/VIEWER',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE INDEX uk_user_brand (user_id, brand_id),
    INDEX idx_brand (brand_id)
) COMMENT '用户-品牌关联';
```

---

#### 2. 白标定制 [中复杂度 | 1.5人月]

**现状**：无白标

**需补齐**：

| 子功能 | 描述 |
|--------|------|
| 登录页白标 | 品牌化登录页(Logo/背景/颜色) |
| 导航白标 | 品牌化导航栏(品牌名/Logo) |
| 邮件白标 | 发件人/邮件模板/退订页品牌化 |
| 报表白标 | 数据报表/仪表盘品牌化(Logo/颜色) |
| 域名绑定 | CNAME绑定品牌域名 |
| 隐藏平台标识 | 隐藏"Powered by Canvas"等平台标识 |

---

### P1 — 代理层级与客户门户

---

#### 3. 代理层级管理 [高复杂度 | 2.0人月]

**现状**：无代理层级

**需补齐**：

| 子功能 | 描述 |
|--------|------|
| 代理注册 | 代理商/经销商注册+审核 |
| 层级关系 | 平台→代理→子代理→品牌(最多5级) |
| 客户分配 | 代理可管理其下属品牌/客户 |
| 代理仪表盘 | 代理查看下属客户概览+收入+活跃度 |
| 代理佣金 | 代理推荐客户产生的佣金计算 |
| 批量操作 | 代理批量创建/管理客户 |
| 经销商模式 | 中国私域多级经销商场景(品牌→一级→二级→门店) |

**数据库DDL**：

```sql
CREATE TABLE agency (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL,
    agency_key VARCHAR(50) NOT NULL,
    name VARCHAR(200) NOT NULL,
    parent_agency_id BIGINT COMMENT '上级代理(多级经销)',
    level INT NOT NULL DEFAULT 1 COMMENT '代理层级(1-5)',
    contact_name VARCHAR(100),
    contact_email VARCHAR(200),
    contact_phone VARCHAR(20),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    commission_rate DECIMAL(5,4) COMMENT '佣金比例',
    settings JSON,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_parent (parent_agency_id),
    INDEX idx_tenant (tenant_id)
) COMMENT '代理';

CREATE TABLE agency_brand (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    agency_id BIGINT NOT NULL,
    brand_id BIGINT NOT NULL,
    relationship VARCHAR(20) NOT NULL COMMENT 'MANAGED/REFERRAL/RESELLER',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE INDEX uk_agency_brand (agency_id, brand_id)
) COMMENT '代理-品牌关联';
```

---

#### 4. 客户门户 [中复杂度 | 1.5人月]

**现状**：无客户门户

**需补齐**：

| 子功能 | 描述 |
|--------|------|
| 自助门户 | 终端客户自助查看画布/报表/用量 |
| 消息预览 | 客户预览将要发送的消息 |
| 审批功能 | 客户审批画布上线(代理→客户审批流) |
| 报表查看 | 客户查看自己品牌的营销效果报表 |
| 用量查看 | 客户查看当前用量+配额 |
| 独立登录 | 客户有独立登录入口(品牌化) |

---

### P2 — 高级代理能力

---

#### 5. 代理市场 [中复杂度 | 1.0人月]

**描述**：代理可上架营销模板/画布模板到市场

| 子功能 | 描述 |
|--------|------|
| 模板市场 | 代理上架/下架画布模板 |
| 模板定价 | 模板免费/付费/订阅 |
| 模板评分 | 终端客户对模板评分+评论 |
| 收益分成 | 模板销售收入平台与代理分成 |

---

#### 6. 多品牌分析 [低复杂度 | 0.5人月]

**描述**：跨品牌聚合分析+对比

| 子功能 | 描述 |
|--------|------|
| 跨品牌汇总 | 代理/平台层汇总所有品牌数据 |
| 品牌对比 | 不同品牌的关键指标对比 |
| 排行榜 | 品牌间效果排行 |
| 异常告警 | 品牌级指标异常告警 |

---

## 工作量估算

| 优先级 | 功能 | 后端人月 | 前端人月 | 测试人月 | 总计 |
|--------|------|---------|---------|---------|------|
| P0 | 品牌管理 | 1.0 | 0.5 | 0.2 | 1.7 |
| P0 | 白标定制 | 1.0 | 0.5 | 0.2 | 1.7 |
| P1 | 代理层级管理 | 1.5 | 0.5 | 0.2 | 2.2 |
| P1 | 客户门户 | 1.0 | 0.5 | 0.2 | 1.7 |
| P2 | 代理市场 | 0.7 | 0.3 | 0.1 | 1.1 |
| P2 | 多品牌分析 | 0.3 | 0.2 | 0.1 | 0.6 |
| | **合计** | **5.5** | **2.5** | **1.0** | **9.0** |

---

## 执行顺序

```
Sprint 1 (P0-品牌): 品牌管理 — 1.7人月
  → 产出：品牌CRUD+配置+数据隔离+用户归属

Sprint 2 (P0-白标): 白标定制 — 1.7人月
  → 产出：登录/导航/邮件/报表品牌化+域名绑定

Sprint 3 (P1-代理): 代理层级管理 — 2.2人月
  → 产出：代理注册+层级关系+仪表盘+佣金+经销商模式

Sprint 4 (P1-门户): 客户门户 — 1.7人月
  → 产出：自助门户+审批+报表+用量查看

Sprint 5 (P2-市场): 代理市场+多品牌分析 — 1.7人月
  → 产出：模板市场+收益分成+跨品牌汇总+排行
```

---

## 风险评估

| 风险 | 影响 | 缓解措施 |
|------|------|---------|
| 数据泄露 | 品牌间数据隔离不当导致数据泄露 | 行级安全策略+租户上下文+定期渗透测试 |
| 白标维护 | 多品牌白标导致前端维护成本高 | 主题系统+CSS变量+配置驱动 |
| 代理冲突 | 代理间客户归属冲突 | 划区规则+客户归属锁定+仲裁机制 |
| 经销商层级深 | 多级经销商管理复杂度高 | 最多5级+佣金级联计算+层级权限 |
| 品牌一致性 | 多品牌运营导致品牌规范失控 | 品牌规范模板+锁定规则+审批流 |

---

## 与其他方向的关系

| 方向 | 与㉚的关系 |
|------|----------|
| ⑫ 多租户SaaS化 | 品牌是租户下的子级隔离单元 |
| ㉕ 计费与用量计量 | 代理模式需要多级计费+佣金计算 |
| ⑯ 协作与权限管理 | 品牌级权限+代理级权限 |
| ⑮ 营销资源中心 | 品牌资产(Logo/素材)按品牌管理 |
| ② 私域运营中台 | 经销商多级私域是私域中台的重要场景 |
| ⑪ 开放平台 | 客户门户API+代理管理API |
| ㉘ 动态内容引擎 | 品牌化内容模板+品牌变量 |
