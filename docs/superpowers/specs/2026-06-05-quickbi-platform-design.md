# QuickBI-Like Analytics Platform Design

## 1. 背景与目标

目标是在 Marketing Canvas 平台中建设一套完整的通用 BI 能力包，并优先服务画布业务。它参考阿里云 Quick BI 的能力模型，但不照搬阿里云实现；系统应成为平台内的 `BI Platform` 子系统，支持从画布运营分析逐步扩展为跨业务域的数据消费平台。

Quick BI 官方资料显示，其主链路覆盖数据源、数据集建模、仪表板、电子表格、数据大屏、即席分析、自助取数、数据门户、小Q问数/解读/报告/搭建、订阅推送、权限治理和报表嵌入。参考资料：

- Quick BI 概述：`https://help.aliyun.com/zh/quick-bi/product-overview/introduction-to-quick-bi-1`
- 数据源支持能力：`https://help.aliyun.com/zh/quick-bi/user-guide/features-supported-by-different-data-sources`
- 数据集创建与管理：`https://help.aliyun.com/zh/quick-bi/user-guide/create-and-manage-datasets`
- 数据集字段管理：`https://help.aliyun.com/zh/quick-bi/user-guide/detail-processing`
- 数据集管理：`https://help.aliyun.com/zh/quick-bi/user-guide/manage-dataset`
- 自定义 SQL 数据集：`https://help.aliyun.com/zh/quick-bi/user-guide/use-ad-hoc-queries-for-data-modeling`
- 仪表板：`https://help.aliyun.com/zh/quick-bi/user-guide/dashboard/`
- 仪表板制作：`https://help.aliyun.com/zh/quick-bi/user-guide/create-a-dashboard-2`
- 图表工具栏区：`https://help.aliyun.com/zh/quick-bi/user-guide/configure-the-toolbar-in-a-chart`
- 仪表板交互分析：`https://help.aliyun.com/zh/quick-bi/user-guide/interactive-analysis-overview`
- 仪表板管理：`https://help.aliyun.com/zh/quick-bi/user-guide/manage-dashboards`
- 资源包导入导出：`https://help.aliyun.com/zh/quick-bi/user-guide/overview-of-resource-plans`
- 导出资源包：`https://help.aliyun.com/zh/quick-bi/user-guide/export-and-import-a-resource-plan`
- 查询控件：`https://help.aliyun.com/zh/quick-bi/user-guide/overview-of-the-filter-bar-widget`
- 钻取、联动、跳转：`https://help.aliyun.com/zh/quick-bi/user-guide/drilling-filter-interaction-and-hyperlink-1`
- 图表跳转：`https://help.aliyun.com/zh/quick-bi/user-guide/hyperlink`
- 自助取数创建：`https://help.aliyun.com/zh/quick-bi/user-guide/create-a-download-task`
- 自助取数配置：`https://help.aliyun.com/zh/quick-bi/user-guide/configure-a-download-task`
- 自助取数下载概览：`https://help.aliyun.com/zh/quick-bi/user-guide/overview`
- 组织级导出控制：`https://help.aliyun.com/zh/quick-bi/user-guide/configure-the-export-feature`
- 工作空间导出控制：`https://help.aliyun.com/zh/quick-bi/user-guide/configure-the-export-feature-in-a-workspace`
- 企业级导出管控：`https://help.aliyun.com/zh/quick-bi/use-cases/enterprise-level-export-control`
- PC 端数据门户：`https://help.aliyun.com/zh/quick-bi/user-guide/create-a-pc-bi-portal-2`
- 数据门户菜单权限：`https://help.aliyun.com/zh/quick-bi/user-guide/bi-portal-menu-permissions`
- 工作空间成员角色：`https://help.aliyun.com/zh/quick-bi/user-guide/manage-workspace-members`
- 资源协同授权：`https://help.aliyun.com/zh/quick-bi/user-guide/overview-of-collaborative-authorization`
- 数据集协同授权：`https://help.aliyun.com/zh/quick-bi/user-guide/grant-users-the-permissions-on-datasets`
- 行列权限：`https://help.aliyun.com/zh/quick-bi/user-guide/row-level-and-column-level-permissions/`
- 条件组合行权限：`https://help.aliyun.com/zh/quick-bi/user-guide/mode-1-authorization-based-on-combined-conditions`
- 组织级列权限：`https://help.aliyun.com/zh/quick-bi/user-guide/column-level-permissions-at-the-organization-level`
- 订阅推送：`https://help.aliyun.com/zh/quick-bi/user-guide/subscriptions/`
- 指标监控告警：`https://help.aliyun.com/zh/quick-bi/user-guide/configure-alert-rules`
- 报表嵌入：`https://help.aliyun.com/zh/quick-bi/embed-a-report-into-a-third-party-system`
- 嵌入 ticket API：`https://help.aliyun.com/zh/quick-bi/developer-reference/api-quickbi-public-2022-01-01-createticket`
- 智能小Q：`https://help.aliyun.com/zh/quick-bi/user-guide/smartq`
- 小Q问数：`https://help.aliyun.com/zh/quick-bi/user-guide/chat-bi-overview`
- 小Q搭建：`https://help.aliyun.com/zh/quick-bi/user-guide/intelligent-assistant-of-quick-bi`
- 小Q解读：`https://help.aliyun.com/zh/quick-bi/user-guide/overview-of-small-q-interpretation`

### 1.1 Quick BI 能力映射

| Quick BI 官方能力 | Canvas BI 对应设计 | 第一阶段落点 |
| --- | --- | --- |
| 数据源、数据集、字段建模 | `bi_data_source_ref`、`bi_dataset`、`bi_dataset_field`、`bi_metric` 和内置数据集注册表 | 已有 `canvas_daily_stats` 语义模型和元数据表 |
| 仪表板、查询控件、组件拖拽 | BI 工作台左侧资源区、中间 20 栅格画布、右侧属性面板、顶部工具栏 | 已有 `canvas-effect` 预置看板和 QuickBI-like 设计器 |
| 钻取、联动、跳转 | `BiDashboardInteraction` 保存组件关系和跳转目标 | 已在预置看板中表达联动、钻取、超链接 |
| 数据门户、自助取数、电子表格、大屏 | 门户菜单、导出任务、表格式分析和大屏资源类型 | 门户资源 lifecycle 和自助取数/导出 foundation 已完成；电子表格和大屏仍待实现 |
| 订阅推送、指标告警 | `bi_subscription`、`bi_alert_rule`、`bi_delivery_log`、`bi_delivery_attachment`、多渠道推送任务 | 订阅/告警管理、手动运行、阈值检测、投递日志、周期 due-check 调度、分布式调度租约、SMTP Email、Webhook/Lark/飞书/钉钉/企业微信 HTTP 投递、带退避和上限的重试、服务端快照/附件和可配置浏览器截图 renderer foundation 已完成；内置截图执行集群仍待部署 |
| 报表嵌入、CreateTicket | 短期 HMAC ticket、匿名 verify endpoint、嵌入渲染页 | 已有 ticket 签发、校验和基础渲染页 |
| 工作空间角色、行列权限 | 工作空间成员、资源权限、行权限、列权限和审计 | 查询链路已接入资源/行/列权限、脱敏和拒绝审计；资源/行/列权限已有管理 API 和工作台基础 UI；导出/订阅/嵌入仍需复用同一路径 |
| 智能小Q/问数/解读/搭建 | 基于语义层的问数、解读、报告、搭建、洞察 Agent | 仍在设计阶段 |

## 2. 产品定位

系统定位为“通用 BI 内核 + 营销场景预置包”：

1. 通用 BI 内核提供数据源、数据集、语义层、查询引擎、图表、仪表板、门户、订阅、嵌入、权限、AI 分析。
2. 营销场景预置包提供画布执行、节点漏斗、渠道触达、用户行为、转化归因、人群分析等标准数据集和仪表板。
3. Canvas 平台通过内部路由和嵌入 token 使用 BI 资源，而不是把每个报表写死在业务页面。

## 3. 用户角色

### 3.1 平台管理员

管理组织、工作空间、数据源、全局角色、嵌入策略、推送渠道和安全策略。

### 3.2 空间管理员

管理某个工作空间的数据集、报表、成员权限、行列权限和发布流程。

### 3.3 数据开发者

创建数据源、数据集、字段、计算字段、指标字典、数据权限规则。

### 3.4 分析师

基于已授权数据集创建图表、仪表板、即席分析、自助取数、订阅任务。

### 3.5 查看者

查看已发布报表、导出已授权数据、触发筛选、钻取、联动和问数。

### 3.6 Canvas 运营

在画布、渠道、人群、归因等营销数据集上完成每日运营分析。

## 4. 能力范围

### 4.1 数据源中心

支持数据源注册、凭证加密、连接测试、表结构同步、使用权限和健康状态：

- 第一批：Doris、MySQL、现有 `data_source_config` JDBC。
- 第二批：PostgreSQL、ClickHouse、CSV/Excel 上传、API 数据源。
- 第三批：第三方 SaaS 连接器和实时流式数据源。

### 4.2 数据集建模

数据集是报表和问数的唯一数据入口。支持：

- 单表数据集。
- SQL 数据集。
- 视图数据集。
- 多表关联数据集。
- 字段元数据：字段名、展示名、数据类型、语义类型、维度/度量、默认聚合、格式、单位、可见性、脱敏策略。
- 计算字段：表达式白名单、依赖字段、返回类型。
- 默认筛选：如 `tenant_id`、业务状态、日期范围。
- 模型预览：字段详情、样例数据、模型结构。

Quick BI 数据建模参考落点：

- 数据集创建入口来自工作台、空间资源列表、数据集列表、数据源列表和数据集编辑页。
- 建模方式先支持可视化表/视图建模，后续再支持 SQL 数据集；生产阶段 SQL 数据集必须使用服务端白名单、参数绑定、SQL lint 和审批。
- 数据集编辑页需要包含字段大纲、数据预览、字段详情、字段批量配置、维度/度量角色切换、展示格式、语义类型、默认聚合、文件夹分组和数据过滤。
- 数据集管理页需要支持仅看我创建、名称搜索、创建者/创建时间筛选、授权、删除、重命名、转让、申请编辑/使用权限、行列权限配置状态标识。
- 数据集编辑需要有抢锁机制；未获得编辑锁时不能保存，抢锁后前一个编辑者的页面进入锁定状态。
- 删除或批量删除数据集前，需要提示受影响的仪表板、自助取数、电子表格、数据大屏、即席分析、数据填报和探索分析，并展示资源名称、所有者、修改人和修改时间。
- 数据集批量管理需要覆盖批量转让、批量移动、批量清除缓存和批量删除。
- Quick BI 官方限制单个数据集中的数据表/自定义 SQL 数量不能超过 100 个；Canvas BI 初期只开放单表/视图资源，后续多表模型按同等上限治理。

当前数据集资源 lifecycle foundation：

- `bi_dataset`、`bi_dataset_field`、`bi_metric` 已映射为资源模型。
- `bi_dataset_version` 表用于持久化数据集发布快照，保留数据集、字段、指标和模型 JSON，支持语义模型回滚。
- `GET /canvas/bi/datasets/resources` 读取当前租户可管理的数据集资源。
- `GET /canvas/bi/datasets/resources/{datasetKey}` 读取数据集、字段和指标定义。
- `POST /canvas/bi/datasets/resources/{datasetKey}/draft` 保存数据集草稿，字段和指标跟随数据集整包保存。
- `POST /canvas/bi/datasets/resources/{datasetKey}/publish` 发布数据集，使其进入可分析状态，并写入 `bi_dataset_version` 发布快照。
- `GET /canvas/bi/datasets/resources/{datasetKey}/versions` 读取数据集发布历史，返回版本号、发布人、发布时间和完整数据集快照。
- `POST /canvas/bi/datasets/resources/{datasetKey}/versions/{version}/restore` 将指定数据集发布快照恢复为当前数据集草稿，重新走字段、指标和表达式校验路径。
- `DELETE /canvas/bi/datasets/resources/{datasetKey}` 归档数据集。
- 保存时校验 dataset key、表表达式、tenant column、字段表达式、指标表达式、字段重复、指标重复和指标允许维度。
- 已有数据集保存草稿需通过 `EDIT` 资源权限，并在保存 endpoint 携带当前用户有效的 `X-BI-LOCK-TOKEN` 编辑锁 token；发布需通过 `PUBLISH` 资源权限，非管理员发布还需存在通过评审且覆盖当前资源更新时间的发布审批。
- 前端工作台“数据集资产”表支持选择数据集资产，并在“数据集发布历史”表格查看发布版本、发布人、发布时间和恢复入口。
- `BiDatasetSpecResolver` 将持久化数据集转换为查询语义模型；查询编译、查询执行和图表保存优先使用 resolver，找不到时回退内置营销数据集。
- 前端工作台底部展示“数据集资产”，并保留营销预置数据集展示。

后续仍需要补齐：数据源连接器 UI、可视化建模画布、多表关联、SQL 数据集审批、字段文件夹、样例预览、血缘影响分析、批量移动/批量转让、复制、权限申请和权限管理 UI。资源移动/转让/收藏/评论/编辑锁 foundation、保存与版本恢复写入链路编辑锁强制校验、发布审批、保存/发布权限检查与行列权限的查询期执行基础已接入。

### 4.3 语义层与指标字典

指标统一管理，避免不同报表口径分裂。每个指标包含：

- `metric_key`：稳定编码。
- `display_name`：业务名称。
- `expression`：聚合表达式。
- `aggregation`：SUM、COUNT、COUNT_DISTINCT、AVG、MIN、MAX、RATIO。
- `unit`：次数、人数、百分比、金额、毫秒。
- `format_pattern`：前端展示格式。
- `allowed_dimensions`：可搭配维度。
- `owner`：指标负责人。
- `description`：口径说明。

首批营销指标：

- 画布执行数、成功数、失败数、挂起数。
- 执行成功率、平均耗时。
- 触达人数、发送量、送达率、打开率、点击率。
- 转化数、转化率、收入、ROI。
- 节点进入数、节点成功率、节点失败率、节点平均耗时。
- 人群规模、增量、流失风险、质量评分。

### 4.4 查询引擎

前端只提交结构化查询，不直接传 SQL：

```json
{
  "datasetKey": "canvas_daily_stats",
  "dimensions": ["stat_date", "canvas_name"],
  "metrics": ["total_executions", "success_rate"],
  "filters": [
    { "field": "stat_date", "operator": "BETWEEN", "value": ["2026-06-01", "2026-06-05"] }
  ],
  "sorts": [{ "field": "stat_date", "direction": "ASC" }],
  "limit": 500
}
```

当前后端接口为 `POST /canvas/bi/query/compile` 和 `POST /canvas/bi/query/execute`，并提供 `GET /canvas/bi/datasets`、`GET /canvas/bi/datasets/{datasetKey}` 给前端图表编辑器和自助取数加载字段/指标清单。`sorts` 是正式字段名，后端兼容旧版 `sort` 入参。

后端负责：

1. 校验数据集、字段、指标、筛选、排序是否合法。
2. 注入租户隔离条件。
3. 注入行级权限和列级权限。
4. 将指标表达式编译成安全 SQL。
5. 使用参数绑定，避免 SQL 注入。
6. 限制查询时间范围、结果行数和执行超时。
7. 写入查询历史，支持性能诊断和审计。

当前查询执行 foundation：

- `BiQueryExecutionService` 复用安全编译器，只执行白名单结构化查询生成的参数化 SQL。
- `JdbcBiQueryExecutor` 根据数据集表表达式路由 JDBC 数据源，`canvas_dws.*` 和 `canvas_ods.*` 走可选 Doris JDBC，其他数据集可走主库 JDBC。
- `JdbcBiQueryHistoryRecorder` 将请求 JSON、SQL hash、行数、耗时、状态和错误摘要写入 `bi_query_history`；历史写入失败不影响报表读取。
- `JdbcBiQueryHistoryReader` 提供租户内最近查询历史读取，前端展示数据集、执行人、状态、行数、耗时和 SQL hash 摘要。
- `BiDatasourceHealthProvider` 暴露主库和 Doris 的健康状态，前端展示数据源可用性，帮助解释实时数据和预览降级。
- `BiQueryResultCache` 提供按 SQL hash 的结果缓存，默认本地内存缓存、TTL 300 秒、最大 500 条，可通过 `canvas.bi.query.cache.*` 配置开关、TTL 和容量。
- 查询命中缓存时返回 `cached=true`，并在查询历史中记录 `CACHE_HIT`，前端 widget 展示“缓存/实时”状态、行数和耗时。
- 返回结果包含列元数据、行数据、行数、耗时和 SQL hash，供图表组件和后续慢查询诊断使用。
- 本地未启用 Doris 时，执行接口会明确报数据源不可用；前端工作台保留预置预览，避免设计器空白。

### 4.5 图表编辑器

参考 Quick BI 仪表板交互，编辑器由四个区域组成：

- 左侧资源树：工作空间、数据集、字段、指标、图表模板。
- 中间画布：栅格布局，拖拽摆放组件。
- 右侧配置面板：数据、样式、交互、权限、订阅。
- 顶部操作栏：撤销、重做、预览、保存、发布、主题、移动端布局。

首批图表：

- 指标卡。
- 明细表、交叉表。
- 折线图、面积图。
- 柱状图、堆叠柱状图。
- 饼图、环图。
- 漏斗图。
- 散点图。
- 热力图。

后续图表：

- 地图。
- 桑基图。
- 水波图。
- 甘特图。
- 自定义 ECharts 插件。

### 4.6 仪表板交互

支持 Quick BI 常见交互：

- Story Builder：围绕图表串成故事线，表达分析路径。
- 查询控件：日期、文本、枚举、数字区间、树形选择。
- 全局筛选器。
- 图表联动：点击一个图表过滤其他图表。
- 层级钻取：如渠道 -> 子渠道 -> 素材。
- 跳转：跳到另一个仪表板、Canvas 详情、用户 360、外部链接。
- 圈选：框选多个数据点后触发只看、标注、联动或钻取。
- 事件：面向表格类组件保留填报、待办和外部事件扩展点。
- 查看明细数据。
- 组件级订阅和导出。
- 移动端布局。

展示设计参考 Quick BI 的工具型工作台，而非营销落地页：

- 资源管理页要支持搜索、筛选、排序、仅看我的、状态筛选和创建时间筛选。
- 编辑态采用顶部操作栏、左侧资源/组件区、中间画布、右侧配置面板。
- 预览态支持多报表页签、收藏、评论、组件级评论入口、深浅主题切换。
- 分享/发布能力要与发布状态绑定，已发布后才能进入私密链接、公开分享、发布渠道和嵌入配置。
- 删除发布态资源前需要下线，并提示会影响的数据门户、订阅、告警和跳转引用。

当前前端工作台按 Quick BI 仪表板制作体验落地为四块区域：

- 顶部工具栏：保存、预览、发布、订阅、嵌入。
- 左侧资源区：数据字段、图表资产、图表组件库、查询控件和布局组件，并支持按字段、图表、控件关键字搜索。
- 中央画布：查询控件栏 + 20 栅格仪表板布局；组件卡片支持选中、查看 SQL、复制、删除和上下左右布局微调。
- 右侧属性区：数据、样式、交互配置面板；数据面板展示查询结果、SQL hash、明细预览，并可调用后端语义层编译参数化 SQL。

首个后端预置看板为 `canvas-effect`，包含 KPI、折线、柱状、明细表、日期查询控件、画布搜索控件、触发方式控件、联动、钻取、跳转、订阅 channel 和嵌入 scope。

当前仪表板资源 lifecycle foundation：

- `GET /canvas/bi/dashboards/resources` 读取当前租户持久化仪表板资源。
- `GET /canvas/bi/dashboards/resources/{dashboardKey}` 优先读取持久化资源，未保存时降级为预置看板。
- `POST /canvas/bi/dashboards/resources/{dashboardKey}/draft` 保存当前看板草稿，持久化仪表板基础信息、筛选控件、交互、订阅 channel、嵌入 scope 和 widget 布局/查询配置。
- `POST /canvas/bi/dashboards/resources/{dashboardKey}/publish` 发布草稿并递增版本，同时写入 `bi_dashboard_version` 发布快照。
- `POST /canvas/bi/dashboards/resources/{dashboardKey}/clone` 从持久化或内置看板复制出新的草稿看板，拒绝覆盖已有 dashboard key。
- `GET /canvas/bi/dashboards/resources/{dashboardKey}/export` 导出已发布看板为 JSON 资源包，包含资源类型、包版本、源 dashboard key、源版本、完整看板 preset、导出人和导出时间。
- `GET /canvas/bi/dashboards/resources/{dashboardKey}/export-file` 下载已发布看板资源包文件，文件名为 `{dashboardKey}-v{version}.bi-dashboard.json`。
- `POST /canvas/bi/dashboards/resources/import` 导入 dashboard 资源包为新草稿；默认拒绝覆盖已有 key，可显式 overwrite。
- `POST /canvas/bi/dashboards/resources/import-file` 以 multipart 文件上传方式导入 dashboard 资源包，并通过 query 参数指定目标 key、标题和 overwrite 策略。
- 前端工作台导出资源包时会下载 `.bi-dashboard.json` 文件；导入按钮支持上传本地 JSON 资源包文件，先做包结构校验，再调用 multipart 导入 API。
- `DELETE /canvas/bi/dashboards/resources/{dashboardKey}` 将看板置为 `ARCHIVED`，资源列表默认隐藏归档看板。
- `GET /canvas/bi/dashboards/resources/{dashboardKey}/versions` 读取当前租户仪表板发布历史，返回版本号、发布人、发布时间和完整看板快照。
- `POST /canvas/bi/dashboards/resources/{dashboardKey}/versions/{version}/restore` 将指定发布快照恢复为当前看板草稿，保留当前资源版本号并重写 widget 布局/查询配置。
- 前端工作台优先加载持久化资源，顶部展示来源、状态和版本，保存、发布、复制、导出、导入、归档按钮已接入上述 API；复制/导入后跳转到新看板，导出后显示当前资源包版本，资源包文件导出/导入使用后端文件端点，画布下方展示发布历史表格，发布成功后自动刷新快照列表，并可从历史版本恢复草稿。
- 已有仪表板保存草稿需通过 `EDIT` 资源权限，并在保存 endpoint 携带当前用户有效的 `X-BI-LOCK-TOKEN` 编辑锁 token；发布需通过 `PUBLISH` 资源权限，版本恢复回到草稿保存路径并继承相同编辑权限校验。

发布审批门禁已接入数据集、仪表板、图表和门户发布链路：非 `ADMIN`、`SUPER_ADMIN`、`TENANT_ADMIN` 角色发布时必须存在同资源、同工作空间的已通过审批，且审批时间不能早于资源最后更新时间；管理员角色可跳过审批。

后续仍需要补齐：更细的设计器键盘操作。资源移动/转让/收藏/评论/编辑锁 foundation、保存与版本恢复写入链路编辑锁强制校验、发布审批和保存/发布资源权限校验已接入统一资源管理与 lifecycle 路径，工作台可对当前仪表板、选中图表、选中数据集或选中门户执行移动、转让、收藏、评论、锁操作和发布审批请求/审核操作；仪表板 helper 已支持布局缩放与撤销/重做历史，画布卡片已提供标题区拖拽移动和右下角拖拽缩放手柄，顶部工具栏已接入撤销/重做、多选左/中/右/顶/垂直居中/底对齐以及桌面/平板/手机布局预览，前端渲染已使用真实 20 栅格 `gridX/gridY/gridW/gridH` 定位，并可把同一看板映射到 12 栅格平板布局或 1 栅格手机单列布局；移动/缩放时做基础碰撞避让；拖拽移动会在阈值内吸附到其它 widget 的边线或中心线，并返回 guide 元数据供后续视觉辅助线渲染。

当前图表资源 lifecycle foundation：

- `bi_chart` 表用于持久化独立图表资产，内容包含图表类型、数据集、结构化查询、样式、交互和状态。
- `bi_chart_version` 表用于持久化图表发布快照，保留图表类型、数据集、查询、样式和交互 JSON，支持按版本回滚。
- `GET /canvas/bi/charts/resources` 读取当前租户已保存且未归档图表。
- `GET /canvas/bi/charts/resources/{chartKey}` 读取单个图表资源。
- `POST /canvas/bi/charts/resources/{chartKey}/draft` 保存图表草稿，保存前复用 `BiQueryCompiler` 校验数据集、字段、指标、筛选、排序和 limit；已有图表保存需携带当前用户有效的 `X-BI-LOCK-TOKEN` 编辑锁 token。
- `POST /canvas/bi/charts/resources/{chartKey}/publish` 发布图表并写入 `bi_chart_version` 发布快照。
- `GET /canvas/bi/charts/resources/{chartKey}/versions` 读取当前租户图表发布历史，返回版本号、发布人、发布时间和完整图表快照。
- `POST /canvas/bi/charts/resources/{chartKey}/versions/{version}/restore` 将指定图表发布快照恢复为当前图表草稿，重新走草稿保存和查询校验路径。
- `DELETE /canvas/bi/charts/resources/{chartKey}` 归档图表。
- 已有图表保存草稿需通过 `EDIT` 资源权限；发布需通过 `PUBLISH` 资源权限，非管理员发布还需通过发布审批；版本恢复回到草稿保存路径并继承相同编辑权限校验。
- 前端工作台左侧“图表”页签区分“已保存图表资产”和“可新建图表类型”，可选择图表资产并在“图表发布历史”表格查看发布版本、发布人、发布时间和恢复入口。

后续仍需要补齐：图表编辑器表单、字段拖拽生成查询、图表引用影响分析和复制。图表资源可通过统一资源管理 band 进行移动/转让/收藏/评论/锁定，并通过统一 lifecycle guard 执行保存/发布权限、保存与版本恢复写入链路编辑锁与发布审批。

### 4.7 数据大屏

数据大屏用于值班室、战情室、活动大促。支持：

- 全屏展示。
- 大屏尺寸和背景。
- 自由定位组件。
- 自动刷新。
- 图表联动、钻取、跳转。
- 运营播报组件。

### 4.8 电子表格与自助取数

电子表格用于复杂格式报表，自助取数用于业务导出：

- 类 Excel 的行列布局。
- 交叉表。
- 公式单元格。
- 自助字段拖拽。
- 查询预览。
- CSV/Excel/PDF 导出。
- 大数据量异步导出。
- 导出审批、脱敏、审计。

Quick BI 自助取数参考落点：

- 自助取数可以从工作台、自助取数列表、数据集操作入口和数据集编辑页开始创建。
- 配置页需要先选择数据集，再配置行列字段、过滤器、查询控件和取数结果。
- 下载格式需要覆盖 CSV/Excel；大数据量场景需要异步任务、行数/文件大小限制和下载任务列表。
- 自助取数结果必须处在行级权限和列级权限控制范围内，导出还需要单独的导出权限。
- 企业导出治理需要覆盖事前导出开关/权限/审批、事中任务状态和下载渠道控制、事后下载审计和过期清理。

当前自助取数 foundation：

- `POST /canvas/bi/self-service/preview` 基于结构化 `BiQueryRequest` 进行预览，复用查询执行链路，因此自动继承租户隔离、行权限、列权限、脱敏、字段治理和查询历史。
- `POST /canvas/bi/self-service/exports` 创建导出任务，当前实现为同步执行并通过 `BiFileStorage` 写入可下载对象；默认 provider 是本地文件实现，表模型保存 `storage_provider`/`storage_key`，后续可直接替换为 S3/OSS/MinIO provider；敏感导出、显式 `approvalRequired` 或超过 `canvas.bi.export.approval.row-threshold` 的任务会进入 `PENDING_APPROVAL`，不会执行查询或生成文件。
- `GET /canvas/bi/self-service/exports` 返回最近导出任务。
- `POST /canvas/bi/self-service/exports/{id}/review` 由管理员角色审批或驳回待审批导出；批准后恢复原始结构化查询并生成文件，驳回后标记 `REJECTED`。
- `GET /canvas/bi/self-service/exports/{id}/download` 下载导出文件。
- `POST /canvas/bi/self-service/exports/cleanup` 清理当前租户已过期导出任务，并返回检查数、过期数、删除文件数和失败数。
- 导出格式支持 `CSV`、`JSON` 和 `XLSX`；导出前会校验数据集 `EXPORT` 资源权限，查询过程继续执行行列权限和脱敏。
- 默认本地 storage 根目录为 `${java.io.tmpdir}/canvas-bi-exports`，可通过 `canvas.bi.export.dir` 配置；导出任务默认保留 7 天，可通过 `canvas.bi.export.retention-days` 配置。
- 下载会累计 `download_count` 和 `last_downloaded_at`；已过期导出会拒绝下载并标记为 `EXPIRED`，清理接口会通过 `BiFileStorage` 删除对象，同时保留旧本地路径行的兼容清理。
- 前端 BI 工作台展示自助取数预览、普通 CSV 导出、敏感导出申请、导出任务列表、审批状态、批准/驳回操作、保留期、下载次数、过期状态、清理结果和下载入口。

后续仍需要补齐：真正异步队列、外部对象存储 provider、百万行分片、进度查询、失败重试、导出审计详情、Excel 样式和 PDF 导出。

### 4.9 数据门户

门户把多个分析资产组织成完整数据产品：

- 门户菜单。
- 仪表板、电子表格、大屏、自助取数、外部链接。
- 角色可见性。
- 主题样式。
- 默认首页。

Quick BI 数据门户参考落点：

- 数据门户是以菜单形式组织的数据作品和外部链接的集合，支持仪表板、电子表格、数据大屏、即席分析、数据填报、自助取数和外部链接。
- PC 端门户需要支持顶部导航、左导航、双导航布局；主题支持浅色、深色和跟随系统。
- 门户配置需要覆盖 LOGO、主标题、副标题、查看方式、页脚、菜单层级面包屑、菜单缓存、目录节点点击行为、门户别名。
- 菜单配置需要支持主菜单、二级及以下菜单、设置主页、删除菜单、空节点、内容节点、菜单搜索、页面全屏、查看方式、菜单展示样式和图标。
- 菜单权限使用协同授权，授权对象为用户或用户组；仅授权可见时只有被授权对象可读，未启用时所有有门户阅览权限的用户可读。

当前数据门户资源 lifecycle foundation：

- `bi_portal`、`bi_portal_menu` 已映射为资源模型。
- `bi_portal_version` 表用于持久化数据门户发布快照，保留门户主题、菜单树、资源引用和可见性 JSON，支持门户配置回滚。
- `GET /canvas/bi/portals/resources` 读取当前租户可管理的数据门户。
- `GET /canvas/bi/portals/resources/{portalKey}` 读取门户和菜单树。
- `POST /canvas/bi/portals/resources/{portalKey}/draft` 保存门户草稿，菜单跟随门户整包保存。
- `POST /canvas/bi/portals/resources/{portalKey}/publish` 发布门户并写入 `bi_portal_version` 发布快照。
- `GET /canvas/bi/portals/resources/{portalKey}/versions` 读取门户发布历史，返回版本号、发布人、发布时间和完整门户快照。
- `POST /canvas/bi/portals/resources/{portalKey}/versions/{version}/restore` 将指定门户发布快照恢复为当前门户草稿，重新走菜单资源引用校验路径。
- `DELETE /canvas/bi/portals/resources/{portalKey}` 归档门户。
- 已有门户保存草稿需通过 `EDIT` 资源权限；发布需通过 `PUBLISH` 资源权限，非管理员发布还需通过发布审批；版本恢复回到草稿保存路径并继承相同编辑权限校验。
- 菜单支持 `DASHBOARD`、`CHART`、`SELF_SERVICE`、`SPREADSHEET`、`BIG_SCREEN`、`EXTERNAL_LINK` 类型；`DASHBOARD` 和 `CHART` 通过 key 解析为资源 ID，外链限制为 `http(s)` 或站内路径。
- `visibility_json` 保留菜单级角色、用户和用户组授权配置；后端运行态门户 API 已复用菜单可见性过滤，只返回当前用户可见菜单。
- `GET /canvas/bi/portals/runtime` 读取当前租户已发布门户，并按当前用户/角色过滤菜单。
- `GET /canvas/bi/portals/runtime/{portalKey}` 读取单个已发布门户；草稿或归档门户不会作为查看态资源返回。
- 前端工作台同时展示“门户查看态”和“数据门户资产”，前者体现当前身份可见菜单，后者体现管理态资源全量；数据门户资产表支持选择门户，并在“门户发布历史”表格查看发布版本、发布人、发布时间和恢复入口。

后续仍需要补齐：门户编辑器、导航布局配置 UI、菜单树拖拽、默认主页、菜单搜索、全屏、移动端门户、门户预览页和嵌入渲染。

Canvas 预置门户：

- 经营总览。
- 画布分析。
- 渠道分析。
- 人群分析。
- 归因 ROI。
- 异常监控。

### 4.10 分享、订阅、告警

支持：

- 站内分享。
- 邮件、飞书、企业微信、钉钉、自定义 Webhook。
- 截图、链接、Excel/PDF 附件。
- 日报、周报、月报。
- 组件级订阅。
- 指标阈值告警。
- 同比/环比异常告警。
- 小Q解读订阅。

当前订阅/告警 foundation：

- `bi_subscription` 已映射为订阅任务模型，保存资源、周期、接收人和投递配置。
- `bi_alert_rule` 已映射为指标告警模型，保存数据集、指标、条件和接收人配置。
- `bi_delivery_log` 已映射为订阅/告警运行流水，保存任务类型、渠道、接收人、payload、指标值、状态、错误、重试次数、下次重试时间、重试耗尽时间和触发人。
- `bi_delivery_attachment` 已映射为投递附件模型，保存快照/附件类型、文件名、下载 URL、storage provider/key、大小、留存天数、过期时间、下载次数、最后下载时间、状态和错误。
- `GET /canvas/bi/subscriptions`、`POST /canvas/bi/subscriptions`、`DELETE /canvas/bi/subscriptions/{id}` 提供订阅任务管理。
- `GET /canvas/bi/alerts`、`POST /canvas/bi/alerts`、`DELETE /canvas/bi/alerts/{id}` 提供指标告警管理。
- `POST /canvas/bi/subscriptions/{id}/run` 手动执行订阅，生成每个渠道的投递日志；站内渠道会写入通知中心，Email 在 SMTP 配置后会真实邮件投递，Webhook/Lark/飞书/钉钉/企业微信类渠道在配置 URL 后会真实 HTTP 投递，未配置渠道进入 `PENDING_ADAPTER`，HTTP/SMTP 或网络失败进入 `FAILED`。
- `POST /canvas/bi/alerts/{id}/run` 手动检测告警，执行语义层指标查询；静态阈值按 `GT/GTE/LT/LTE/EQ/NEQ` 判断，异常型条件支持 `ANOMALY_DROP`、`ANOMALY_RISE` 或 `mode/type=ANOMALY`，基于同一告警最近 `EVALUATION` 指标值计算基线均值、标准差、偏离值和偏离百分比，样本不足时只写 `SKIPPED` 检测日志，命中后生成渠道投递日志。
- `GET /canvas/bi/delivery-logs` 查询订阅和告警投递流水。
- `GET /canvas/bi/delivery-audit` 按 jobType、status、channel、jobId 和 limit 返回投递审计窗口，汇总 total、delivered、triggered、skipped、pending、failed、retryable 和 retryExhausted，并返回对应明细。
- `GET /canvas/bi/delivery-attachments` 查询订阅投递附件，`GET /canvas/bi/delivery-attachments/{id}/download` 下载已生成文件，`POST /canvas/bi/delivery-attachments/cleanup` 清理过期附件对象并兼容旧本地路径。
- 订阅运行时会根据 `delivery.content` 和 `delivery.attachment(s)` 生成服务端快照/附件：`SNAPSHOT_LINK`/`SNAPSHOT` 默认生成 HTML 快照，`snapshotFormat`/`screenshotFormat` 设置为 `PNG` 或 `JPEG` 时会调用可配置 HTTP browser renderer 生成图片截图，`CSV`/`JSON`/`XLSX`/`PDF` 生成投递摘要附件，其中 PDF 会按摘要内容自动分页并生成多页 Page/Contents 对象，附件元数据会写入 payload；附件通过 `BiFileStorage` 写入并记录 `storage_provider`/`storage_key`，默认本地 storage 根目录为 `${java.io.tmpdir}/canvas-bi-delivery-attachments`；附件默认留存 7 天，可通过 `canvas.bi.delivery.attachment.retention-days` 调整；下载会累计下载次数和最后下载时间，过期附件拒绝下载并可被清理任务标记为 `EXPIRED` 且删除存储对象；邮件渠道会读取生成文件并作为 MIME 附件发送，飞书、钉钉、企微和 Webhook 等非邮件渠道使用文本下载链接；失败邮件重试会从历史 payload 中的附件 ID 重新下载文件并回放为 MIME 附件。
- `BiSnapshotRenderer` 定义浏览器截图渲染 SPI；`HttpBiSnapshotRenderer` 默认关闭，通过 `canvas.bi.delivery.snapshot.renderer.enabled:true` 和 `canvas.bi.delivery.snapshot.renderer.url` 接入内部 Playwright/Browserless 渲染服务，协议为 JSON 请求和 base64 图片响应，支持 PNG/JPEG、宽高和缩放倍率。
- `BiDeliveryAdapterService` 提供外部投递 adapter foundation，支持 `EMAIL`、`WEBHOOK`、`LARK`、`FEISHU`、`DINGTALK`、`DING`、`WECOM`、`WECHAT_WORK` 和 `ENTERPRISE_WECHAT` 等渠道；Lark/飞书、钉钉和企业微信机器人采用文本消息 payload，通用 Webhook 采用 `BI_DELIVERY` 结构化 payload。
- `BiSmtpEmailDeliveryClient` 提供无第三方依赖的 SMTP 邮件 foundation，默认关闭，可通过 `canvas.bi.delivery.email.*` 配置启用，支持基本 SMTP、SSL、STARTTLS、AUTH LOGIN、纯文本正文和 `multipart/mixed` 附件。
- `POST /canvas/bi/delivery-logs/retry` 对到达 `next_retry_at` 且未耗尽的 `PENDING_ADAPTER` 和 `FAILED` 非检测日志进行一次性重试，复用同一 adapter 路径并写入新的重试日志；重试策略默认最多 4 次、初始延迟 30 分钟、指数退避倍率 2、最大延迟 1440 分钟，可通过 `canvas.bi.delivery.retry.*` 配置。
- `BiDeliverySchedulerService` 提供订阅/告警 due-check 调度能力，自动调度默认通过 `canvas.bi.delivery.scheduler.enabled:false` 关闭；支持 `HOURLY`、`DAILY`、`WEEKLY`、`MONTHLY`、`intervalMinutes`、`checkIntervalMinutes` 和 `cronExpression`，并基于 `bi_delivery_log` 最近运行时间避免同一周期重复投递；自动调度路径通过 `bi_delivery_scheduler_lease` 抢占租约，避免多实例重复触发同一租户的订阅/告警检查。
- `POST /canvas/bi/delivery-scheduler/run` 提供运营侧一次性调度检查，使用当前租户、用户和角色上下文，便于在后台调度关闭时手动补跑 due 任务。
- 创建订阅和告警时复用 `BiPermissionService.ACTION_SUBSCRIBE`，把订阅/告警纳入同一资源权限路径。
- 前端工作台新增“订阅推送”和“指标告警”区域，支持查看已有任务、创建当前看板日报订阅、为当前选中指标创建阈值告警或基线异常告警、手动执行/检测、触发一次调度检查、重试失败/待配置投递、查看投递记录与审计汇总、查看重试次数/下次重试/耗尽状态、下载投递附件、查看附件过期/下载次数并手动清理过期附件。

后续仍需要补齐：内置浏览器截图执行集群、对象存储保留策略、告警静默和更完整的日历窗口同比/环比异常模型。

### 4.11 嵌入分析

支持将仪表板、电子表格、大屏、自助取数和问数嵌入 Canvas 或第三方系统：

- 内部嵌入：登录态透传，使用当前用户权限。
- 外部嵌入：短期 embed token。
- token 绑定 tenant、user、resource、scope、过期时间、nonce。
- 支持 URL 参数传入筛选条件。
- token 不能暴露数据源凭证或 SQL。

当前实现：

- `POST /canvas/bi/embed-tickets` 签发 HMAC-SHA256 短期 ticket。
- `POST /canvas/bi/embed-tickets/verify` 匿名校验 ticket，用于第三方 iframe 打开嵌入报表。
- ticket payload 绑定 `tenantId`、`username`、`resourceType`、`resourceKey`、`scope`、`filters`、`nonce`、`issuedAt`、`expiresAt`。
- TTL 默认 10 分钟，最短 1 分钟，最长 30 分钟；外部 ticket 的前端默认申请 15 分钟。
- 前端 BI 设计器顶部“嵌入”按钮调用 ticket API，并在右侧交互面板展示短期 `embedUrl` 与过期时间。
- 匿名嵌入页 `/bi/embed/:resourceType/:resourceKey` 已具备基础路由、ticket 校验、资源匹配校验和预置看板降级展示。
- 当前 ticket 尚未持久化到 `bi_embed_token` 表，生产阶段需要补 nonce 回收、一次性消费、域名白名单、访问次数限制、审计和真实数据查询渲染。

### 4.12 权限治理

权限分层：

1. 组织角色：平台管理员、权限管理员、普通成员。
2. 工作空间角色：管理员、开发者、分析师、查看者。
3. 资源权限：数据源、数据集、图表、仪表板、门户、订阅。
4. 数据权限：行级权限、列级权限、脱敏规则。
5. 操作权限：查看、使用、编辑、发布、分享、导出、订阅、嵌入。

安全原则：

- 默认拒绝。
- 租户条件强制注入。
- 敏感字段默认不可导出。
- 外部嵌入必须短期 token。
- 所有查询、导出、订阅、权限变更写审计日志。

当前权限执行 foundation：

- `BiPermissionService` 统一读取 `bi_resource_permission`、`bi_row_permission`、`bi_column_permission` 和 `bi_audit_log`。
- 查询编译和查询执行都会先经过权限准备：资源权限 `DENY` 会阻断数据集使用，行权限会追加为结构化 `BiFilter`，列权限 `DENY` 会在 SQL 生成前拒绝。
- 列权限 `MASK` 会在查询执行后对返回行做字段级脱敏，并把脱敏配置纳入查询缓存签名，避免不同权限用户共享错误缓存。
- 菜单 `visibility_json` 支持 `roles/users/denyRoles/denyUsers` 的服务级过滤方法，供后续门户查看态 API 复用。
- 权限拒绝会写入 `bi_audit_log`；现有 CDP 字段治理仍在权限准备之后执行，形成“BI 权限 + 字段治理”的叠加校验。
- `BiPermissionAdminService` 和 `BiPermissionController` 已提供资源授权、行权限、列权限的 list/upsert/delete 管理 API，支持通过数据集、仪表板、图表和门户 key 解析资源 ID。
- 前端 BI 工作台已展示资源授权、行权限、列权限三类规则，并提供数据集使用授权、画布行权限和字段脱敏的模板化新增动作。
- `BiResourcePermissionGuard` 已接入数据集、仪表板、图表和门户 lifecycle：已有资源保存/恢复草稿执行 `EDIT`，发布执行 `PUBLISH`，首创资源因尚无资源 ID 暂不阻断创建。
- `BiPublishApprovalService.requireApprovedApproval(...)` 已接入数据集、仪表板、图表和门户发布路径，非管理员发布必须有覆盖当前资源更新时间的已批准发布审批；`ADMIN`、`SUPER_ADMIN` 和 `TENANT_ADMIN` 可绕过发布审批。
- 当前尚未完成完整权限编辑器、权限申请、工作空间成员角色表执行、嵌入/真实外部投递/AI agents 统一鉴权和权限变更审计。

### 4.13 AI 分析

参考 Quick BI 智能小Q，拆为五类 Agent：

- 问数 Agent：自然语言转结构化查询。
- 解读 Agent：解释图表、仪表板、异常波动。
- 报告 Agent：生成日报、周报、月报和管理层摘要。
- 搭建 Agent：根据用户意图生成图表和仪表板草稿。
- 洞察 Agent：自动发现趋势、异常、归因和机会。

AI 必须只使用数据集语义层，不直接访问数据库。生成查询前要走字段权限和行级权限校验。

## 5. Canvas 业务预置包

### 5.1 预置数据集

- `canvas_daily_stats`：画布每日统计。
- `node_daily_stats`：节点每日统计。
- `canvas_execution_trace`：执行轨迹。
- `message_delivery_stats`：消息发送与回执。
- `channel_performance`：渠道效果。
- `conversion_attribution`：转化归因。
- `audience_stats`：人群统计。
- `user_event_timeline`：用户行为时间线。

### 5.2 预置仪表板

- 运营驾驶舱。
- 单画布效果分析。
- 节点转化漏斗。
- 渠道效果分析。
- 画布对比。
- 人群效果分析。
- ROI 排行。
- 异常失败分析。

### 5.3 Canvas 嵌入点

- 首页：嵌入运营驾驶舱。
- 画布列表：嵌入画布排行榜组件。
- 画布详情：嵌入单画布效果仪表板。
- 节点详情：嵌入节点漏斗和节点耗时图。
- 人群详情：嵌入人群趋势和质量分析。
- 消息触达：嵌入渠道回执和失败原因分析。

### 5.4 当前实现状态

截至 2026-06-05，第一阶段已落地：

- BI 元数据 Flyway 基础表：工作空间、数据源引用、数据集、字段、指标、图表、仪表板、门户、权限、订阅、告警、嵌入 token、审计等。
- 结构化查询编译器：白名单数据集、字段、指标、筛选、排序和租户条件注入，只生成参数化 SQL，不接受前端 SQL。
- 查询执行 foundation：`POST /canvas/bi/query/execute` 通过 JDBC/Doris 执行已编译查询，返回列、行、行数、耗时和 SQL hash，并基础写入 `bi_query_history`。
- 查询治理可见性：`GET /canvas/bi/query/history` 返回最近查询历史，`GET /canvas/bi/datasources/health` 返回主库/Doris 健康状态。
- 查询结果缓存：同一租户、同一结构化查询编译出的 SQL hash 会复用短期结果缓存，降低 QuickBI-like 看板多组件刷新对 Doris 的压力。
- 权限执行与管理 foundation：查询编译和查询执行接入 `BiPermissionService`，支持数据集资源拒绝、行权限过滤追加、列权限拒绝、列脱敏、权限缓存签名和拒绝审计；`/canvas/bi/permissions/*` 提供资源、行、列权限管理 API；工作台展示并可创建常见授权规则；门户 runtime API 已接入菜单可见性过滤。
- 自助取数/导出 foundation：`/canvas/bi/self-service/preview`、`/canvas/bi/self-service/exports`、`/canvas/bi/self-service/exports/{id}/review`、`/canvas/bi/self-service/exports/{id}/download` 和 `/canvas/bi/self-service/exports/cleanup` 支持授权数据集预览、CSV/JSON/XLSX 导出任务、敏感/大批量导出审批、下载、下载审计、过期拒绝和本地过期文件清理，前端工作台已提供预览、普通导出、敏感导出、任务列表、待审批通过/驳回、过期/下载状态和清理入口。
- 订阅/告警运行 foundation：`/canvas/bi/subscriptions` 和 `/canvas/bi/alerts` 支持任务列表、创建/更新和删除；`/canvas/bi/subscriptions/{id}/run`、`/canvas/bi/alerts/{id}/run`、`/canvas/bi/delivery-scheduler/run`、`/canvas/bi/delivery-logs/retry`、`/canvas/bi/delivery-logs`、`/canvas/bi/delivery-audit`、`/canvas/bi/delivery-attachments`、`/canvas/bi/delivery-attachments/{id}/download` 和 `/canvas/bi/delivery-attachments/cleanup` 支持手动运行、告警检测、一次性调度检查、到期失败/待配置重试、投递流水、投递审计摘要、服务端快照/附件生成、下载审计、过期拒绝和本地附件清理；自动调度具备 `bi_delivery_scheduler_lease` 分布式租约，避免多实例重复触发；创建时校验资源、数据集、指标、接收渠道，并执行 `SUBSCRIBE` 权限检查；投递 adapter 支持 SMTP Email、站内通知、Webhook、Lark/飞书、钉钉和企业微信，邮件渠道发送真实 MIME 附件，失败邮件重试会回放历史附件，失败/待配置投递按可配置指数退避写入 `next_retry_at`、`retry_count` 和 `retry_exhausted_at`，非邮件渠道在文本消息中带附件下载链接；截图格式支持 HTML 默认快照和可配置 HTTP renderer 的 PNG/JPEG；前端工作台展示订阅任务、指标告警、运行按钮、调度按钮、重试按钮、重试状态、过期附件清理、投递记录、投递审计摘要、附件过期/下载状态和附件下载入口。
- 营销画布内置数据集：`canvas_daily_stats`。
- 持久化数据集资源：支持数据集、字段、指标整包保存、发布、归档、发布快照历史和历史版本恢复，并能转换为查询语义模型供图表和查询引擎使用。
- 营销画布预置看板：`canvas-effect`。
- 持久化仪表板资源：支持看板资源列表、详情、保存草稿、发布、复制、JSON 资源包导入导出、资源包文件下载上传、归档、版本递增、发布快照历史和历史版本恢复；未保存时可从预置看板降级加载。
- 持久化图表资源：支持图表资源列表、详情、保存草稿、发布、归档、发布快照历史和历史版本恢复；保存时校验结构化查询，防止图表绕过语义层。
- 持久化门户资源：支持门户资源列表、详情、保存草稿、发布、归档、发布快照历史、历史版本恢复和按当前用户/角色过滤的 runtime 查看态。
- BI 资源移动 lifecycle：`bi_resource_location` 记录数据集、仪表板、图表和门户的租户/工作空间内文件夹位置与排序；移动接口会校验资源存在、拒绝归档资源、校验安全 folder key，并保留操作人；前端工作台可读取位置列表，选择当前仪表板、选中图表、选中数据集或选中门户并移动到目标文件夹，位置表展示 folder、sort、movedBy 和 movedAt。
- BI 资源转让 lifecycle：`bi_resource_ownership` 记录数据集、仪表板、图表和门户的当前负责人以及 transferredBy/transferredAt；转让接口会校验资源类型、资源 key、负责人、租户/工作空间内资源存在性并拒绝归档资源；前端工作台读取负责人列表，选择当前仪表板、选中图表、选中数据集或选中门户并转让给目标账号，负责人表展示 ownerUser、transferredBy 和 transferredAt。
- BI 资源协作 lifecycle：`bi_resource_favorite` 记录当前用户收藏，`bi_resource_comment` 记录资源级和 widget 级评论并支持创建者软删除，`bi_resource_lock` 记录资源编辑锁、锁持有人、token 和过期时间；收藏和评论会拒绝归档资源，锁获取通过资源唯一键条件 upsert 避免并发编辑冲突，释放锁要求 token 与当前用户匹配；保存和版本恢复 dataset/dashboard/chart/portal 写入链路时会用 `X-BI-LOCK-TOKEN` 强制校验当前用户持有的未过期锁，管理员角色可跳过该编辑锁门禁；前端工作台在统一资源管理 band 中展示当前资源收藏状态、评论流和编辑锁状态，并可执行收藏/取消收藏、发送/删除评论、获取/释放锁。
- BI 发布审批与 lifecycle 权限：`bi_publish_approval` 记录数据集、仪表板、图表和门户的发布审批请求、状态、申请人、审批人和审批意见；请求接口会校验资源类型/key、默认工作空间和资源存在性并拒绝归档资源；审核接口只允许 `PENDING` 审批流转到 `APPROVED` 或 `REJECTED`；非管理员发布需通过同资源、同工作空间且覆盖资源最后更新时间的已批准审批，管理员角色可跳过审批；已有资源保存/恢复草稿执行 `EDIT` 权限，发布执行 `PUBLISH` 权限；前端工作台在统一资源管理 band 中展示当前资源审批列表，并可创建审批、批准和驳回待审批请求。
- 后端接口：`GET /canvas/bi/datasets`、`GET /canvas/bi/datasets/{datasetKey}`、`GET /canvas/bi/datasets/resources`、`GET /canvas/bi/datasets/resources/{datasetKey}`、`POST /canvas/bi/datasets/resources/{datasetKey}/draft`、`POST /canvas/bi/datasets/resources/{datasetKey}/publish`、`GET /canvas/bi/datasets/resources/{datasetKey}/versions`、`POST /canvas/bi/datasets/resources/{datasetKey}/versions/{version}/restore`、`DELETE /canvas/bi/datasets/resources/{datasetKey}`、`GET /canvas/bi/dashboards/presets`、`GET /canvas/bi/dashboards/presets/{dashboardKey}`、`GET /canvas/bi/dashboards/resources`、`GET /canvas/bi/dashboards/resources/{dashboardKey}`、`POST /canvas/bi/dashboards/resources/{dashboardKey}/draft`、`POST /canvas/bi/dashboards/resources/{dashboardKey}/publish`、`POST /canvas/bi/dashboards/resources/{dashboardKey}/clone`、`GET /canvas/bi/dashboards/resources/{dashboardKey}/export`、`GET /canvas/bi/dashboards/resources/{dashboardKey}/export-file`、`POST /canvas/bi/dashboards/resources/import`、`POST /canvas/bi/dashboards/resources/import-file`、`DELETE /canvas/bi/dashboards/resources/{dashboardKey}`、`GET /canvas/bi/dashboards/resources/{dashboardKey}/versions`、`POST /canvas/bi/dashboards/resources/{dashboardKey}/versions/{version}/restore`、`GET /canvas/bi/charts/resources`、`GET /canvas/bi/charts/resources/{chartKey}`、`POST /canvas/bi/charts/resources/{chartKey}/draft`、`POST /canvas/bi/charts/resources/{chartKey}/publish`、`GET /canvas/bi/charts/resources/{chartKey}/versions`、`POST /canvas/bi/charts/resources/{chartKey}/versions/{version}/restore`、`DELETE /canvas/bi/charts/resources/{chartKey}`、`GET /canvas/bi/portals/resources`、`GET /canvas/bi/portals/resources/{portalKey}`、`POST /canvas/bi/portals/resources/{portalKey}/draft`、`POST /canvas/bi/portals/resources/{portalKey}/publish`、`GET /canvas/bi/portals/resources/{portalKey}/versions`、`POST /canvas/bi/portals/resources/{portalKey}/versions/{version}/restore`、`DELETE /canvas/bi/portals/resources/{portalKey}`、`GET /canvas/bi/portals/runtime`、`GET /canvas/bi/portals/runtime/{portalKey}`、`GET /canvas/bi/resources/locations`、`POST /canvas/bi/resources/move`、`POST /canvas/bi/resources/locations`、`GET /canvas/bi/resources/ownerships`、`POST /canvas/bi/resources/transfer`、`GET /canvas/bi/resources/favorites`、`POST /canvas/bi/resources/favorites`、`DELETE /canvas/bi/resources/favorites/{resourceType}/{resourceKey}`、`GET /canvas/bi/resources/comments`、`POST /canvas/bi/resources/comments`、`DELETE /canvas/bi/resources/comments/{commentId}`、`GET /canvas/bi/resources/locks`、`POST /canvas/bi/resources/locks/acquire`、`POST /canvas/bi/resources/locks/release`、`GET /canvas/bi/resources/publish-approvals`、`POST /canvas/bi/resources/publish-approvals`、`POST /canvas/bi/resources/publish-approvals/{approvalId}/review`、`GET /canvas/bi/subscriptions`、`POST /canvas/bi/subscriptions`、`DELETE /canvas/bi/subscriptions/{id}`、`POST /canvas/bi/subscriptions/{id}/run`、`GET /canvas/bi/alerts`、`POST /canvas/bi/alerts`、`DELETE /canvas/bi/alerts/{id}`、`POST /canvas/bi/alerts/{id}/run`、`GET /canvas/bi/delivery-logs`、`GET /canvas/bi/delivery-audit`、`POST /canvas/bi/delivery-logs/retry`、`GET /canvas/bi/delivery-attachments`、`GET /canvas/bi/delivery-attachments/{id}/download`、`POST /canvas/bi/delivery-attachments/cleanup`、`POST /canvas/bi/delivery-scheduler/run`、`POST /canvas/bi/self-service/exports/cleanup`、`POST /canvas/bi/query/compile`、`POST /canvas/bi/query/execute`。
- 嵌入 ticket：`POST /canvas/bi/embed-tickets` 签发短期 HMAC ticket，`POST /canvas/bi/embed-tickets/verify` 匿名校验 ticket，前端设计器可生成嵌入链接。
- 前端工作台：QuickBI-like 设计器布局、持久化/预置看板画布、门户查看态、订阅告警、资源位置移动、资源负责人转让、资源收藏、资源/组件评论、资源编辑锁、发布审批请求/审核、持久化门户资产列表、持久化数据集资产列表、持久化图表资产列表、看板复制/归档/导入/导出、资源包 JSON 文件下载/上传、看板/图表/数据集/门户发布历史和版本恢复、数据/样式/交互面板、组件库、查询控件、数据集清单；看板保存草稿以及看板/图表/数据集/门户版本恢复会在当前资源锁有效时携带锁 token；看板 widget 会按维度/指标自动执行查询，失败时降级为预置预览；工作台展示数据源健康、最近查询历史以及 widget 的实时/缓存状态；设计器资源区支持字段/图表/控件搜索，组件卡片支持查看 SQL、复制、删除、布局微调、Ctrl/Shift/Meta 多选、左/中/右/顶/垂直居中/底对齐、桌面/平板/手机预览模式、标题区拖拽移动、吸附到邻近边线或中心线、右下角拖拽缩放、真实 CSS 栅格定位、碰撞避让和顶部撤销/重做，右侧数据面板展示查询结果、SQL hash、明细预览和后端编译出的参数化 SQL。
- 前端嵌入页：`/bi/embed/:resourceType/:resourceKey` 在无登录态下校验 ticket 并渲染基础报表视图。
- Canvas 入口：画布统计页和画布列表可进入 `/bi?dashboard=canvas-effect&canvasId={id}`。

仍未完成的生产级能力：

- 生产级查询治理：分布式缓存、慢查询详情页、执行取消、执行超时策略外显和 per-dataset quota。
- 完整 BI 资源 lifecycle：发布审批流程、非管理员发布审批门禁、保存/发布前权限校验、资源移动/转让/收藏/评论/编辑锁前后端闭环、dataset/dashboard/chart/portal 保存与版本恢复写入链路编辑锁强制绑定、数据集草稿/发布/归档/发布历史/版本恢复、仪表板草稿/发布/复制/导出资源包/导入资源包/资源包文件上传下载/归档/发布历史/版本恢复、图表草稿/发布/归档/发布历史/版本恢复和门户草稿/发布/归档/发布历史/版本恢复基础流程已完成。
- 权限管理闭环：完整权限编辑器、权限申请、工作空间成员角色执行、导出审批、嵌入/真实投递/AI agents 复用同一权限路径，以及权限变更审计。
- 生产级外部嵌入：nonce 持久化校验、一次性消费、过期回收、来源白名单、访问次数限制和审计。
- 生产级自助取数：异步队列、对象存储、百万行分片、进度查询、失败重试和审计详情页。
- 电子表格、大屏、内置浏览器截图执行集群、对象存储保留策略、告警静默、更完整的日历窗口异常模型和 AI 问数/解读/搭建 Agent。

## 6. 后端模块设计

```text
org.chovy.canvas.domain.bi
  dataset
  metric
  query
  chart
  dashboard
  resource
  permission
  embed
  subscription
  ai

org.chovy.canvas.web.bi
  BiDatasetController
  BiQueryController
  BiChartController
  BiDashboardController
  BiResourceMovementController
  BiResourceTransferController
  BiResourceFavoriteController
  BiResourceCollaborationController
  BiPublishApprovalController
  BiPortalController
  BiEmbedController
```

第一批实现优先放在 `org.chovy.canvas.domain.bi` 和 `org.chovy.canvas.web.bi`，避免扩大现有 analytics controller 的职责。

## 7. 前端模块设计

```text
frontend/src/pages/bi
  workspace
  data-sources
  datasets
  dataset-editor
  chart-editor
  dashboard-editor
  portal
  self-service
  subscriptions
  ai-assistant

frontend/src/components/bi
  DatasetFieldTree
  MetricPicker
  ChartRenderer
  DashboardCanvas
  QueryControlBar
  InteractionRulesPanel
  PermissionPanel
  EmbedPreview
```

UI 风格参考 Quick BI 的工作台结构，但保持当前平台 Ant Design 风格。页面应偏数据工具，而不是营销落地页：

- 左侧导航 + 资源列表。
- 中间表格/画布主工作区。
- 右侧属性配置面板。
- 顶部固定操作栏。
- 图表卡片 8px 圆角以内。
- 查询控件使用熟悉的表单、选择器、日期范围、分段控件。

Quick BI 展示、UI 和交互参考落点：

- 仪表板制作参考 `create-a-dashboard-2` 和仪表板目录：顶部固定操作区承载保存、预览、发布、订阅、嵌入；左侧承载数据集字段、图表组件和控件组件；中间是栅格画布；右侧用数据、样式、高级/交互、权限页签配置选中组件。
- 查询控件参考 `overview-of-the-filter-bar-widget`：日期区间、枚举选择、搜索选择、数字区间、树形选择都应是画布上的一等组件，支持默认值、必填、级联和影响范围。
- 钻取、联动和跳转参考 `drilling-filter-interaction-and-hyperlink-1`、`filter-interaction-ui` 和 `hyperlink`：支持全局自动联动、单组件手动联动、点击后只下钻或下钻并联动、内部报表跳转、外部链接跳转和参数透传。
- 数据集编辑参考 `create-and-manage-datasets`、`detail-processing` 和 `manage-dataset`：字段大纲、数据预览、字段详情、维度/度量切换、展示格式、语义类型、默认聚合、文件夹分组、行列权限状态和影响分析必须在一个编辑上下文中可见。
- 数据门户参考 `bi-portals`、`create-a-pc-bi-portal-2` 和 `bi-portal-menu-permissions`：门户不是单报表页面，而是菜单化的数据产品；需支持顶部导航、左导航、双导航、LOGO、标题、页脚、菜单搜索、默认主页、全屏和菜单级授权。
- 订阅参考 `subscriptions`、`create-a-subscription-task`、`faq-about-subscriptions`：发送内容按截图、报表链接、数据附件拆分；渠道覆盖邮件、钉钉、企业微信、飞书和自定义渠道；数据附件优先邮件渠道，非邮件渠道通过报表链接访问并下载。
- 嵌入参考 `embed-a-report-into-a-third-party-system` 和 `CreateTicket`：ticket 默认短有效期、可限制使用次数、绑定用户或账号、绑定资源和筛选参数；Canvas 的实现用 HMAC ticket 替代阿里云 OpenAPI，但安全语义保持一致。

## 8. 数据模型

核心表：

- `bi_workspace`
- `bi_workspace_member`
- `bi_data_source_ref`
- `bi_dataset`
- `bi_dataset_version`
- `bi_dataset_field`
- `bi_dataset_relation`
- `bi_metric`
- `bi_chart`
- `bi_chart_version`
- `bi_dashboard`
- `bi_dashboard_widget`
- `bi_dashboard_version`
- `bi_portal`
- `bi_portal_version`
- `bi_portal_menu`
- `bi_resource_location`
- `bi_query_history`
- `bi_export_job`
- `bi_subscription`
- `bi_alert_rule`
- `bi_delivery_attachment`
- `bi_resource_permission`
- `bi_row_permission`
- `bi_column_permission`
- `bi_embed_token`
- `bi_audit_log`

## 9. 非功能要求

- 查询默认超时 30 秒。
- 查询结果默认最多 1000 行，导出任务最多 100 万行且异步。
- 所有外部嵌入 token 默认 10 分钟有效。
- 查询历史保留 180 天。
- 导出文件保留 7 天。
- Dashboard 查询支持缓存，默认 5 分钟。
- 数据源凭证必须加密。
- 多租户必须强制隔离。
- SQL 生成必须使用白名单字段和参数绑定。

## 10. 分阶段实施

### 阶段 1：BI 内核

- BI 元数据表。
- 营销数据集种子。
- 字段和指标模型。
- 安全查询编译器。
- 查询 API。
- 查询历史。

### 阶段 2：图表与仪表板

- 图表定义。
- 仪表板布局。
- 图表渲染器。
- 查询控件。
- 联动、钻取、跳转。
- 保存、预览、发布。

### 阶段 3：自助取数、导出、订阅

- 自助取数。
- CSV/Excel/PDF 导出。
- 异步导出任务。
- 订阅推送。
- 指标告警。

### 阶段 4：门户与嵌入

- 数据门户。
- 内部嵌入。
- 外部 embed token。
- 嵌入参数传递。
- 嵌入审计。

### 阶段 5：AI 分析

- 问数 Agent。
- 解读 Agent。
- 报告 Agent。
- 搭建 Agent。
- 洞察 Agent。

## 11. 第一批验收标准

第一批实现完成后，应满足：

1. 有完整 BI 元数据 schema。
2. 有营销数据集和指标种子。
3. 有安全查询编译器，能根据结构化查询生成参数化 SQL。
4. 查询编译器拒绝未知字段、未知指标、非法筛选、非法排序和超限 limit。
5. 查询编译器强制注入 `tenant_id` 条件。
6. 有单元测试覆盖正常查询、聚合查询、筛选、排序、limit、权限拒绝。
7. 文档说明完整 QuickBI-like 平台设计、UI 参考和交互参考。
