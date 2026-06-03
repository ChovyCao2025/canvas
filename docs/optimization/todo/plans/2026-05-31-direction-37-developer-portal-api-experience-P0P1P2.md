# 方向㊲：开发者门户与API体验 — 功能清单

> 定位：从"有API"升级为"开发者愿意用的API平台"——开发者门户+交互式API文档+SDK分发+沙箱环境+API分析+MCP/AI Agent友好
> 策略评估：2026年API体验直接决定平台生态成败，Mintlify/Theneo/DigitalAPI均验证"开发者从注册到首次API调用<10分钟"是黄金标准，MCP/AI Agent就绪成为新门槛
> 竞品对标：Mintlify(AI-Agent-Ready文档+自动维护+语义搜索)、Theneo(AI原生开发者门户+Agent友好)、DigitalAPI(多网关+Persona-based+自服务)、ReadMe(开发者Hub+互动探索器)
> 建议：**P1建议做**，依赖⑪开放平台/Webhook成熟后启动，开发者门户是开放平台的"门面"，直接影响API采用率

---

## 参考出处

| 出处 | 核心观点 | 链接 |
|------|---------|------|
| Mintlify: API Developer Portals for Enterprise 2026 | 7维度评估：品牌/治理/API参考/文档维护/AI-Agent就绪/分析/上手体验；llms.txt+MCP Server+Markdown→Agent友好；Workflows自动检测代码变更→PR文档更新 | https://www.mintlify.com/library/api-developer-portals-for-enterprise |
| Theneo: Top API Documentation Tools 2026 | AI原生：AI搜索+AI Q&A+完整开发者门户(指南+参考+教程+上手)；Agent-friendly文档是2026新门槛 | https://dev.to/arobakid/top-api-documentation-tools-in-2026-the-shortlist-for-modern-api-teams-29bj |
| DigitalAPI: Must-Have Features of API Developer Portal 2026 | 4级成熟度：静态→文档化→交互式→自服务；Persona-based导航+多网关+In-Portal测试+自服务=80-90%集成自助完成 | https://www.digitalapi.ai/blogs/top-crucial-features-of-an-high-impact-api-developer-portal |
| Zuplo: What Is a Developer Portal 2026 | 开发者门户>API文档——它是数字店面+支持中心+社区中心+运营中枢 | https://zuplo.com/learning-center/what-is-a-developer-portal |
| DevPortal Awards: Best Onboarding Experience | 透明注册步骤+快速授权+展示API能力+交互式体验 | https://devportalawards.org/categories/developer-experience/best-onboarding-experience-developer-portal |
| Postman/Redocly/Stoplight/SwaggerHub | API工具生态：Postman(测试集合)、Redocly(规范优先)、Stoplight(治理)、SwaggerHub(设计协作) | Theneo comparison |
| ReadMe: Developer Hub Platform | 开发者Hub：指南+互动API浏览器+开发者互动分析+社区论坛+Changelog | https://www.mintlify.com/library/api-developer-portals-for-enterprise |
| Klaviyo: API-first Marketing Automation | API-first营销平台标杆：完整开发者门户+SDK+Webhook+实时事件API | https://www.klaviyo.com/ |

---

## 现状盘点

| 功能 | 实现程度 | 核心现有代码 | 差距 |
|------|----------|-------------|------|
| Swagger UI | **部分** | SecurityConfig: permitAll /swagger-ui/**, /v3/api-docs/** | 仅SpringDoc默认Swagger UI，无定制门户 |
| REST API | **部分** | 多个Controller暴露API | API分散，无统一网关/版本管理/限流 |
| Webhook | **部分** | ㉑方向规划中 | 无Webhook管理界面/重试/事件目录 |
| 开发者门户 | **不存在** | — | 无独立开发者门户(注册/API Key管理/文档/SDK) |
| 交互式API文档 | **不存在** | — | Swagger UI默认界面，无API Playground/代码示例/预填凭证 |
| SDK分发 | **不存在** | — | 无官方SDK(Java/JS/Python/Go)+包管理器分发 |
| API分析 | **不存在** | — | 无API使用量/错误率/延迟/P99分析 |
| 沙箱环境 | **不存在** | — | 无独立沙箱环境(Sandbox API endpoint+测试数据) |
| API Key管理 | **不存在** | — | 无自服务API Key创建/轮转/权限管理 |
| MCP/AI Agent就绪 | **不存在** | — | 无llms.txt/MCP Server/AI Agent可发现API |

### 关键洞察

DigitalAPI 4级开发者门户成熟度模型：
```
Level 1 (静态): PDF文档+开发者手动编辑+扁平API列表+无自服务
Level 2 (文档化): 在线Spec查看器+基础搜索+OpenAPI下载+订阅管理
Level 3 (交互式): 内嵌测试+响应示例+多语言代码样例+预填Payload
Level 4 (自服务): ★ Persona导航+多层网关+非开发者可编辑+分析治理

Canvas当前: Level 1.5（有Swagger UI，但无自服务/无Key管理/无SDK/无分析）
```

2026年API体验新标准：
- **Agent-friendly是新的SEO**：API必须对AI Agent可发现+可理解+可调用（llms.txt + MCP Server + 结构化Markdown）
- **自服务是新的入门**：开发者从注册到成功API调用<30分钟(Level 4) vs 需要人工支持数天(Level 1)
- **文档自动维护是新的CI/CD**：代码变更→自动检测→自动PR文档更新（Mintlify Workflows模式）

---

## 功能清单

### P0 — 开发者门户与交互式文档

---

#### 1. 开发者门户 [中复杂度 | 2.0人月]

**现状**：仅Swagger UI，无独立门户

**需补齐**：

| 子功能 | 描述 | 参考出处 |
|--------|------|---------|
| 独立开发者门户 | 品牌化开发者门户(自定义域名+Logo+颜色+导航) | Mintlify: white-labeling |
| 开发者注册与入驻 | 自服务注册→邮箱验证→自动生成API Key→快速入门引导 | DigitalAPI: Level 4 self-service |
| API Key管理 | 自助创建/轮转/禁用/删除API Key+权限范围+用量配额 | DigitalAPI: instant subscriptions |
| Persona导航 | 不同角色(外部开发者/合作伙伴/内部团队)看到不同的文档和权限 | DigitalAPI: persona-based navigation |
| API目录 | 搜索+分类+收藏+版本管理→统一API发现 | DigitalAPI: multi-gateway catalog |
| 代码样例库 | 多语言代码样例(Java/Go/Python/Node.js/curl) | Theneo: multi-language code samples |
| 快速入门指南 | 5分钟快速入门教程→引导完成首次API调用 | Mintlify: time-to-first-call <10min |
| 认证流程文档 | OAuth2.0/JWT/API Key认证流程详解+交互式演示 | 开发者体验 |

**数据库DDL**：

```sql
CREATE TABLE developer (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL,
    email VARCHAR(200) NOT NULL,
    name VARCHAR(100),
    company VARCHAR(200),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/SUSPENDED/DELETED',
    registered_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_active_at DATETIME,
    UNIQUE INDEX uk_tenant_email (tenant_id, email),
    INDEX idx_tenant (tenant_id)
) COMMENT '开发者';

CREATE TABLE api_key (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    developer_id BIGINT NOT NULL,
    key_hash VARCHAR(128) NOT NULL COMMENT 'API Key哈希(仅存哈希，不存明文)',
    key_prefix VARCHAR(10) NOT NULL COMMENT 'Key前缀(展示用, cv_live_xxx)',
    name VARCHAR(100) COMMENT 'Key名称(开发者自定义)',
    scopes JSON COMMENT '权限范围(["canvas:read","canvas:write","events:write"])',
    rate_limit INT COMMENT '速率限制(req/s)',
    quota_limit INT COMMENT '月配额上限(请求数)',
    quota_used INT NOT NULL DEFAULT 0 COMMENT '当月已用配额',
    expires_at DATETIME,
    last_used_at DATETIME,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/REVOKED/EXPIRED',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_developer (developer_id),
    INDEX idx_hash (key_hash),
    INDEX idx_status (status)
) COMMENT 'API密钥';
```

---

#### 2. 交互式API文档与Playground [中复杂度 | 1.5人月]

**现状**：SpringDoc默认Swagger UI

**需补齐**：

| 子功能 | 描述 | 参考出处 |
|--------|------|---------|
| 交互式API浏览器 | Explore→Test→See Response→Copy Code，全流程在浏览器完成 | DigitalAPI: in-portal testing |
| API Playground | 浏览器端API测试：自动注入凭证+预填Payload+多语言代码生成 | Theneo: built-in playground |
| 按错误码展示 | 每个API端点展示所有可能的错误响应+示例+解决方案 | DigitalAPI: per-error response examples |
| 实时同步 | 从OpenAPI Spec自动生成文档→代码变更→文档自动更新 | Mintlify: CI/CD integration |
| 变更日志 | API版本变更日志+Breaking Change标注+迁移指南 | ReadMe: changelog |
| 搜索 | 语义搜索+AI Q&A（开发者用自然语言问"如何创建用户标签？"） | Theneo: AI search + Ask the docs |
| 非开发者编辑 | 产品/市场/技术写作人员通过Draft→Review→Publish工作流编辑文档 | DigitalAPI: non-developer editing |

---

### P1 — SDK分发与API分析

---

#### 3. SDK管理与分发 [中复杂度 | 1.0人月]

**现状**：零官方SDK

**需补齐**：

| 子功能 | 描述 |
|--------|------|
| 官方SDK生成 | OpenAPI Spec→自动生成Java/Python/Go/Node.js SDK |
| 包管理器分发 | Maven Central(npm/pip/go get)发布 |
| SDK文档 | 每个SDK独立的API参考+快速入门+示例项目 |
| SDK版本管理 | SDK版本与API版本对应+兼容性矩阵 |
| SDK下载统计 | SDK各语言下载量+版本分布+活跃度 |
| SDK社区 | GitHub讨论+Issue跟踪+贡献指南 |

---

#### 4. API分析与监控 [中复杂度 | 1.0人月]

**现状**：零API分析

**需补齐**：

| 子功能 | 描述 |
|--------|------|
| API用量仪表盘 | 总请求量+按API端点+按开发者+按时间(日/周/月) |
| 错误分析 | 4xx/5xx错误率+按端点+按开发者+错误趋势 |
| 延迟分析 | P50/P90/P99延迟+按端点+按时间段 |
| API Key用量 | 每个Key的请求量+配额使用率+活跃度 |
| 异常检测 | 异常流量(突发激增/异常调用模式)→自动告警 |
| 开发者互动分析 | 文档页面访问+搜索关键词+API测试频率→改进方向 |

---

### P2 — 高级开发者体验

---

#### 5. 沙箱与测试环境 [低复杂度 | 0.5人月]

**描述**：独立沙箱环境+测试数据+一键重置

| 子功能 | 描述 |
|--------|------|
| 沙箱API Endpoint | 独立沙箱域名(sandbox.api.xxx.com)+完全隔离 |
| 测试数据 | 预置测试数据+可自定义+一键重置 |
| 沙箱配额 | 沙箱环境独立配额(免费但有限) |
| Webhook测试 | Webhook事件模拟发送+请求日志查看 |
| 沙箱文档 | 沙箱与生产差异说明+数据限制+使用规范 |

---

#### 6. AI Agent与MCP就绪 [低复杂度 | 0.5人月]

**描述**：2026年API新标准——让AI Agent能够发现和理解你的API

| 子功能 | 描述 | 参考出处 |
|--------|------|---------|
| llms.txt | 自动生成llms.txt+llms-full.txt→LLM可读取的API文档索引 | Mintlify: auto-generate llms.txt |
| MCP Server | 自动托管MCP Server→AI Agent可通过MCP协议调用API | Mintlify: auto-host MCP server |
| Agent流量分析 | 区分Human vs AI Agent访问+各自的行为分析 | Mintlify: AI traffic analytics |
| OpenAPI增强 | OpenAPI Spec中增加AI Agent专用元数据(使用场景/约束/示例) | Theneo: agent-friendly documentation |
| 自然语言API | 开发者用自然语言查询API→AI路由到正确端点 | AI-to-API gateway |

---

## 工作量估算

| 优先级 | 功能 | 后端人月 | 前端人月 | 测试人月 | 总计 |
|--------|------|---------|---------|---------|------|
| P0 | 开发者门户 | 1.2 | 0.8 | 0.2 | 2.2 |
| P0 | 交互式API文档与Playground | 0.7 | 0.5 | 0.2 | 1.4 |
| P1 | SDK管理与分发 | 0.5 | 0.3 | 0.2 | 1.0 |
| P1 | API分析与监控 | 0.7 | 0.3 | 0.1 | 1.1 |
| P2 | 沙箱与测试环境 | 0.3 | 0.2 | 0.1 | 0.6 |
| P2 | AI Agent与MCP就绪 | 0.3 | 0.2 | 0.1 | 0.6 |
| | **合计** | **3.7** | **2.3** | **0.9** | **6.9** |

---

## 执行顺序

```
Sprint 1 (P0-Portal): 开发者门户 — 2.2人月
  → 产出：品牌化门户+注册入驻+API Key管理+Persona导航+多语言代码样例

Sprint 2 (P0-Docs): 交互式API文档 — 1.4人月
  → 产出：API Playground+按错误码展示+语义搜索+AI Q&A+自动同步

Sprint 3 (P1): SDK+API分析 — 2.1人月
  → 产出：官方SDK(Java/Python/Go/Node.js)+包管理器分发+API用量/错误/延迟仪表盘

Sprint 4 (P2): 沙箱+MCP就绪 — 1.2人月
  → 产出：沙箱环境+llms.txt+MCP Server+AI Agent流量分析
```

---

## 风险评估

| 风险 | 影响 | 缓解措施 |
|------|------|---------|
| API Key泄露 | Key泄露→恶意调用→数据泄露/资源滥用 | Key仅存哈希+自动轮转提醒+异常检测+即时吊销 |
| 文档与代码不同步 | 文档过时→开发者信任下降→采用率低 | CI/CD自动同步+定期审计+版本标记+变更日志 |
| 沙箱数据泄露 | 沙箱测试数据混入生产→数据污染 | 完全隔离+沙箱数据标记+定期清理+一键重置 |
| AI Agent误用 | AI Agent误调用API→非预期行为 | MCP Server鉴权+Agent调用策略限制+操作日志+人工审核 |
| 多版本兼容 | API版本演进→旧版本维护成本高 | 版本策略(Sunset date)+弃用通知+迁移工具+兼容性矩阵 |

---

## 与其他方向的关系

| 方向 | 与㊲的关系 |
|------|----------|
| ⑪ 开放平台/Webhook | 开发者门户是开放平台的"门面"和体验层 |
| ⑫ 多租户SaaS化 | 租户隔离+API Key按租户管理+开发者按租户分组 |
| ㉕ 计费与用量计量 | API用量数据是计费的基础+API Key级配额 |
| ⑲ 沙箱与测试环境 | 沙箱API endpoint是沙箱环境的一部分 |
| ㉜ 事件追踪SDK | SDK分发+文档与开发者门户统一管理 |
| ⑨ 营销数据中台 | API分析数据纳入平台级监控 |
| ⑯ 协作与权限管理 | 开发者门户访问权限+API Key权限粒度 |
