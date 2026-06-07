# QuickBI-Like Analytics Platform Design

## 1. 背景与目标

目标是在 Marketing Canvas 平台中建设一套完整的企业级 BI 能力包，并优先服务画布业务。它需要在能力语义上对齐阿里云 Quick BI 的主链路和企业治理边界，但不声明、复制或依赖阿里云 Quick BI 的内部实现；系统应成为平台内的 `BI Platform` 子系统，支持从画布运营分析逐步扩展为跨业务域的数据消费平台。

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
| 数据源、数据集、字段建模 | `bi_data_source_ref`、连接器能力注册、凭证加密、连接测试、schema 同步、查询/抽取/缓存模式、`bi_dataset`、`bi_dataset_field`、`bi_metric` 和内置数据集注册表 | 已有 `canvas_daily_stats` 语义模型、元数据表、BI 数据源连接器目录、租户内已配置数据源可见性、BI 数据源创建/编辑接入 API 与工作台入口、连接器类型和默认连接模式持久化、连接器支持模式的创建期约束、API HTTP/JSON 抽取接入配置持久化、CSV/Excel 文件上传、schema 同步、数据集草稿生成和 EXTRACT 物化刷新闭环、运行时预览和工作台数据预览入口、凭证加密和轮换、真实连接测试、schema 预览、持久化 schema 同步快照、API schema sync 多组 runtime 变量、基于成功 schema 快照生成单表和多行多表复合 Join 条件数据集草稿、API 响应 schema 到 EXTRACT 数据集草稿、SQL 模板参数后端绑定、前端 SQL 模板、参数、字段/指标精细编辑、样例预览和血缘影响分析入口、数据集抽取加速策略、手动刷新运行记录、API 抽取调度运行与逐策略观测、API 和文件数据集 EXTRACT 刷新写入 `bi_extract` 物化表，以及数据源 `USE`/`EDIT` 资源权限 foundation；更深的数据源能力差异约束仍需补齐 |
| 仪表板、查询控件、组件拖拽 | BI 工作台左侧资源区、中间 20 栅格画布、右侧属性面板、顶部工具栏、运行态参数和级联查询控件 | 已有 `canvas-effect` 预置看板和 QuickBI-like 设计器；URL 参数、全局参数 key/alias、控件默认值、记住查询条件、控件作用范围、条件级联候选查询和 embed 参数 claim 绑定已进入运行态查询链路；运行态编辑器已展示每个控件当前值来源（URL 覆盖、记住条件、默认值、已清除）和锁定状态；已保存图表资产支持基础编辑器、字段表单生成结构化查询、复制草稿和引用影响摘要 |
| 钻取、联动、跳转 | `BiDashboardInteraction` 保存组件关系和跳转目标 | 已在预置看板中表达联动、钻取、超链接 |
| 数据门户、自助取数、电子表格、大屏 | 门户菜单、导出任务、表格式分析和大屏资源类型 | 门户资源 lifecycle、自助取数/导出 foundation、电子表格和大屏资源 lifecycle、运行视图、工作台可视化编辑控件已完成；大屏布局已支持选中组件的方向移动、宽高步进缩放、多组件对齐、邻近组件参考线吸附和移动端单列/紧凑双列布局变体；电子表格运行态已支持单元格引用、`SUM`/`AVERAGE`/`MIN`/`MAX`/`COUNT` 区间求值和移动端列数适配，工作台支持单元格范围批量填充和单元格粗体/背景色/文字色编辑；高级表格分析能力仍需补齐 |
| 订阅推送、指标告警 | `bi_subscription`、`bi_alert_rule`、`bi_delivery_log`、`bi_delivery_attachment`、多渠道推送任务 | 订阅/告警管理、手动运行、阈值检测、投递日志、周期 due-check 调度、分布式调度租约、SMTP Email、Webhook/Lark/飞书/钉钉/企业微信 HTTP 投递、带退避和上限的重试、服务端快照/附件、可配置浏览器截图 renderer 和 renderer endpoint 集群故障转移 foundation 已完成 |
| 报表嵌入、CreateTicket | 短期 HMAC ticket、匿名 verify endpoint、嵌入渲染页、来源域名绑定和 replay 控制 | 已有 ticket 签发、校验、基础渲染页、外部 ticket 允许域名签名、匿名 HTTP 校验来源白名单、持久化 token hash、原子一次性消费、访问审计、过期 token 回收、访问次数限制、频率窗口限制、签名嵌入仪表板资源/运行态读取和签名嵌入查询执行 |
| 工作空间角色、行列权限 | 工作空间成员、资源权限、数据源权限、行权限、列权限、权限申请和审计 | 查询链路已接入资源/行/列权限、工作空间成员角色解析、脱敏和拒绝审计；资源/行/列权限已有管理 API 和工作台基础 UI；数据源 `USE` 权限已用于从数据源创建数据集，数据源 `USE`/`EDIT` 授权可通过权限 API 和工作台授予；权限申请已支持申请、列表、审批和审批通过后自动授予申请人资源权限；嵌入组件查询已复用同一查询执行权限/行列权限链路；导出/订阅/真实投递仍需进一步统一同一路径 |
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

- 第一批：Doris、MySQL、现有 `data_source_config` JDBC、API HTTP/JSON 抽取接入 foundation。
- 第二批：PostgreSQL、ClickHouse、CSV/Excel 上传。
- 第三批：第三方 SaaS 连接器和实时流式数据源。

当前数据源 onboarding foundation：

- `GET /canvas/bi/datasources/connectors` 暴露 QuickBI-like 连接器能力目录，覆盖 MySQL、Doris、PostgreSQL、ClickHouse、Hologres、AnalyticDB、Oracle、SQL Server、MaxCompute、CSV/Excel、API 和应用分析类数据源，并区分 `AVAILABLE` 与 `PLANNED`；连接器目录返回 `capacityCategory`/`capacityNote`，用于区分 `INTERACTIVE_QUERY` JDBC 直连/缓存容量、`HTTP_EXTRACT_SMALL` API 抽取容量、`APP_EXTRACT_SMALL` 应用数据源抽取容量、`FILE_EXTRACT_SMALL` 文件抽取容量和 `WAREHOUSE_EXTRACT` 规划中的云数仓抽取容量。API、APP_ANALYTICS 与 CSV/Excel 已开放为 `EXTRACT` 表数据集接入，MaxCompute 原生连接器仍保持计划状态。
- `GET /canvas/bi/datasources/onboarding` 读取当前租户在 `data_source_config` 中已经配置的数据源，映射为 BI 数据源接入视图。
- `POST /canvas/bi/datasources/onboarding` 通过 BI 数据源中心创建当前租户 JDBC、API、APP 或文件数据源，按连接器默认 driver、加密凭证、记录创建人，并拒绝尚未开放的计划连接器；API 和 APP_ANALYTICS 数据源默认走 `HTTP_JSON` driver、`API` source type 和 `api-{id}` source key，APP_ANALYTICS 保留自身 connectorType 以便容量治理和能力矩阵区分。
- `POST /canvas/bi/datasources/file-upload` 以 multipart 方式上传 CSV/XLS/XLSX 文件，按租户隔离保存上传对象，并创建 `CSV_EXCEL` / `FILE_UPLOAD` 抽取型数据源，持久化文件名、文件类型、sheet、分隔符、表头行和编码配置。
- `POST /canvas/bi/datasources/file-upload/materialize` 提供 QuickBI-like 一步式文件接入闭环：上传文件、同步 schema 快照、基于快照创建表数据集草稿、写入 `EXTRACT` 加速策略，并立即触发一次抽取刷新，响应包含数据源、schema snapshot、数据集资源、加速策略和刷新 run。
- `PUT /canvas/bi/datasources/onboarding/{id}` 在租户边界内更新 BI 数据源名称、连接器、URL、账号、driver、说明和启用状态；密码为空时保留原凭证，非空时重新加密保存；API 数据源会持久化经过规范化和脱敏的 `connectorConfigJson`。
- `POST /canvas/bi/datasources/{id}/credential-rotation` 在租户上下文中轮换数据源密码，响应只返回数据源 key 和操作人，不回显明文凭证。
- `POST /canvas/bi/datasources/{id}/connection-test` 对当前租户 JDBC 数据源执行真实连接测试，只读取 `DatabaseMetaData`，返回数据库产品、版本、耗时和脱敏错误摘要。
- `GET /canvas/bi/datasources/{id}/schema-preview` 对当前租户 JDBC 数据源读取表/视图和字段元数据，返回表类型、字段名、字段类型、是否可空和字段顺序，不读取业务表数据行。
- `POST /canvas/bi/datasources/{id}/api-preview` 对当前租户 API/HTTP_JSON 数据源执行真实 HTTP JSON 预览，支持 `{{var}}` 和 `${var}` 模板变量替换、按 `responseRowsPath` 抽取行、推断预览列类型，并按 Quick BI direct preview 约束限制 10MB 响应、1000 行和 100 列；工作台 API 预览控件可输入多组模板变量 key/value 和行数上限并随预览请求提交。
- `POST /canvas/bi/datasources/{id}/schema-sync` 对当前租户 JDBC 或 API 数据源执行元数据同步，将表/字段结构、同步状态、表数、字段数、同步人、同步时间和脱敏失败摘要写入 `bi_datasource_schema_snapshot`；JDBC 路径读取数据库元数据，API/HTTP_JSON 路径可接收与预览一致的可选请求体（模板变量和行数上限），执行真实 HTTP JSON 响应推断并持久化 `api_response` 表 schema。
- `GET /canvas/bi/datasources/{id}/schema-snapshot` 读取当前租户数据源的最新 schema 快照。
- `GET /canvas/bi/datasources/{id}/schema-snapshots` 读取当前租户数据源的 schema 同步历史，用于审计和回溯建模输入。
- 接入视图按 JDBC URL/driver/name/description 识别连接器类型，展示支持模式、连接模式、schema 同步状态和能力标签。
- 接入视图读取最新 schema 快照，将 `schemaSyncStatus`、`tableCount` 和 `lastSyncedAt` 展示给工作台，避免刷新后丢失同步结果。
- API 数据源 connector config 支持 `GET`/`POST`、`NONE`/`BASIC`/`BEARER`/`API_KEY`、请求头、查询参数、body template、JSON 响应行路径和 `JSON` 响应格式；服务端会遮蔽敏感 key、URL 中的 token/password/api key/access key 类参数，以及非变量占位符的敏感请求头/参数值。
- 接入视图不返回密码；URL 中的密码、token、authorization、api key 和 access key 类参数会被脱敏，用户名也只展示脱敏值。
- 数据源运行时兼容现有 `SecretCipher` 和 `DataSourceCredentialCipher` 两类历史凭证密文前缀，避免 BI 接入只对单一路径创建的数据源可用。
- 工作台治理 band 展示连接器目录和已接入数据源，提供连接器配置、连接凭证和接入复核三步数据源向导，支持创建 BI 数据源、选择连接器默认连接模式（直连查询、查询缓存或抽取加速，按连接器能力约束）、展示连接器容量分类、为 API/APP 类连接器配置 HTTP 方法、认证类型、响应行路径、请求头、查询参数和 body template、为 CSV/Excel 连接器选择本地 `.csv`、`.xls` 或 `.xlsx` 文件并一键完成上传、schema 同步、数据集草稿生成和 EXTRACT 刷新、编辑已接入数据源元数据、轮换凭证、对已接入 JDBC 数据源发起连接测试、schema 预览、schema 同步，对已接入 API/APP 数据源填写多组预览模板变量和行数上限后发起真实 HTTP 数据预览和 schema 同步，并能从成功 schema 快照中的单表或多表多 Join 模型生成数据集草稿；多表模型可在关系画布中拖拽表节点并随数据集创建提交 graph 坐标；API/APP schema 快照生成数据集草稿时会沿用当前多组预览变量，文件上传物化成功后会刷新接入列表、schema 快照、数据集资产和加速策略，和健康状态、健康 SLO、健康历史一起给分析师解释“能接什么、已接什么、当前是否可用、使用哪类容量桶”。
- 数据源已作为 `bi_resource_permission` 的 `DATASOURCE` 资源类型接入权限治理，JDBC source key 使用 `jdbc-{dataSourceConfigId}`，API source key 使用 `api-{dataSourceConfigId}`；从数据源 schema 创建数据集草稿前会校验当前用户对该数据源的 `USE` 授权，工作台权限治理可对数据源授予 `USE` 和 `EDIT`，但共享资源移动、转让、收藏、评论、锁定和发布审批仍限定在已支持的 BI 内容资源。

后续仍需补齐：更深的驱动包/认证方式/数据源能力差异约束。

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
- 建模方式已支持可视化表/视图建模、schema 快照驱动的多行多表关联建模，并新增 SQL 数据集草稿入口；SQL 草稿由服务端限制为单条只读 `SELECT`、拒绝危险 SQL token/注释/多语句、要求包含租户列，并支持 `{{parameter_key}}` 模板参数定义、默认值、必填、枚举允许值和 JDBC `?` 占位符绑定；前端工作台可输入 SQL 模板、自动解析模板参数、配置参数类型/必填/默认值/允许值，精细编辑 SQL 字段和指标，并通过既有草稿保存接口提交；SQL 样例预览和血缘影响分析入口已复用同一草稿资源模型。查询编译会按 `model.sqlParameterOrder` 在租户过滤和运行态筛选前绑定 `BiQueryRequest.sqlParameters`，缺少必填参数或传入非法枚举值时阻断。SQL 数据集发布强制走发布审批且管理员不能绕过。后续仍需补全更完整 SQL lint。
- 数据集编辑页需要包含字段大纲、数据预览、字段详情、字段批量配置、维度/度量角色切换、展示格式、语义类型、默认聚合、文件夹分组和数据过滤。
- 数据集管理页需要支持仅看我创建、名称搜索、创建者/创建时间筛选、授权、删除、重命名、转让、申请编辑/使用权限、行列权限配置状态标识。
- 数据集编辑需要有抢锁机制；未获得编辑锁时不能保存，抢锁后前一个编辑者的页面进入锁定状态。
- 删除或批量删除数据集前，需要提示受影响的仪表板、自助取数、电子表格、数据大屏、即席分析、数据填报和探索分析，并展示资源名称、所有者、修改人和修改时间。
- 数据集批量管理需要覆盖批量转让、批量移动、批量清除缓存和批量删除。
- Quick BI 官方限制单个数据集中的数据表/自定义 SQL 数量不能超过 100 个；Canvas BI 已开放 schema 快照驱动的多表 SQL 草稿，后续完整多表模型按同等上限治理。

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
- `POST /canvas/bi/datasets/resources/from-datasource-schema` 从当前租户某个数据源的最新成功 schema 快照生成单表/视图数据集草稿，执行前需通过该数据源的 `DATASOURCE USE` 授权，成功后自动带入字段、维度/度量角色、租户字段隐藏、行数指标和数值字段 SUM 指标；API 和上传文件 schema 快照允许响应列/文件列中没有租户列，建模时不把缺失的 synthetic tenant 字段强塞进 `selectedColumns`，但仍把 `tenantColumn` 设为统一租户列，并在 `modelJson` 中保留 `apiResponseVariables` 或文件源元数据供后续 EXTRACT 刷新复用。
- `POST /canvas/bi/datasets/resources/from-datasource-schema/multi-table` 从最新成功 schema 快照和前端提交的表/Join 模型生成 SQL 数据集草稿，执行前同样校验 `DATASOURCE USE` 授权；服务端只接受安全标识符、`INNER`/`LEFT`/`RIGHT`/`FULL` join 和白名单列间条件操作符（`=`、`<>`、`>`、`>=`、`<`、`<=`），生成只读派生 SQL、字段/指标、租户字段隐藏、`DATASOURCE_SCHEMA` 来源元数据、`modelType=MULTI_TABLE` 模型信息，并持久化 graph-canvas 节点坐标与由 Join 派生的关系边元数据。
- `POST /canvas/bi/datasets/resources/{datasetKey}/draft` 可接收 `datasetType=SQL` 的 SQL 数据集草稿，服务端会把通过 lint 的只读 SQL 包装为派生表表达式并写入 `model.sqlApprovalRequired=true`；SQL 模板参数会以 `model.sqlTemplate`、`model.sqlParameterOrder` 和规范化 `model.sqlParameters` 保存，派生表表达式只保留 JDBC `?` 占位符；发布 SQL 数据集必须存在覆盖当前资源更新时间的已通过审批，即使当前用户是管理员角色也不能跳过。
- 保存时校验 dataset key、表表达式、tenant column、字段表达式、指标表达式、字段重复、指标重复和指标允许维度。
- 已有数据集保存草稿需通过 `EDIT` 资源权限，并在保存 endpoint 携带当前用户有效的 `X-BI-LOCK-TOKEN` 编辑锁 token；发布需通过 `PUBLISH` 资源权限，非管理员发布还需存在通过评审且覆盖当前资源更新时间的发布审批。
- 前端工作台“数据集资产”表支持选择数据集资产，并在“数据集发布历史”表格查看发布版本、发布人、发布时间和恢复入口；数据集基础编辑器可选择多个字段批量写入字段文件夹 `folderKey`，并可复制当前数据集为 `*_copy` 草稿后复用 `POST /canvas/bi/datasets/resources/{datasetKey}/draft` 保存。数据源 schema 快照表支持从单表结构直接生成并选中数据集草稿，API 快照会把当前 API 多组预览变量写入数据集创建命令并在无响应租户列时仅设置统一 `tenant_id` 租户列配置；多表建模面板支持选择多张表、主表、添加多条关联行、配置每条 Join 的左右表和可重复字段条件，并在关系画布中显示可点击 Join 边、Join 类型、字段摘要/条件数和可拖拽表节点，分析师可选中关系边并从画布上下文设置 Join 左右端点表、Join 类型、查看、编辑字段和条件操作符、添加、移除多字段 Join 条件，并一键添加全部同名字段条件，也可直接交换关系方向以互换左右表、左右字段和复合条件字段对，生成星型或链式 `conditions[]` 与 `GRAPH_CANVAS` 节点布局的多表 SQL 数据集草稿，服务端会把复合条件、graph 节点和 Join 派生边持久化到模型并生成 `AND` 连接的 SQL `ON` 子句；数据源接入区域新增 SQL 数据集面板，支持输入 dataset key/name/tenant column/SQL 模板、解析 `{{parameter_key}}`、配置参数类型/必填/默认值/允许值，精细编辑字段 key、展示名、表达式、角色、类型、可见性、语义类型、默认聚合、格式、单位、敏感级别，以及指标 key、展示名、表达式、聚合、类型、允许维度、单位、格式、负责人和描述，并保存为 SQL 数据集草稿或发起样例预览与血缘影响分析。
- `BiDatasetSpecResolver` 将持久化数据集转换为查询语义模型；查询编译、查询执行和图表保存优先使用 resolver，找不到时回退内置营销数据集。
- 前端工作台底部展示“数据集资产”，并保留营销预置数据集展示。

后续仍需要补齐：完整数据集编辑器、字段文件夹树/拖拽式编排、数据集资源批量移动/批量转让、权限申请和权限管理 UI。资源移动/转让/收藏/评论/编辑锁 foundation、保存与版本恢复写入链路编辑锁强制校验、发布审批、保存/发布权限检查、SQL 参数后端绑定、前端 SQL 参数草稿配置、SQL 字段/指标精细编辑、SQL 样例预览/血缘影响分析、字段文件夹基础写入和数据集复制草稿、schema 快照驱动多表复合 Join 建模与行列权限的查询期执行基础已接入。

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
- `BiDatasetAccelerationService` 提供数据集级 `DIRECT_QUERY`、`CACHE`、`EXTRACT` 加速策略，保存策略会写入 before/after 审计；`EXTRACT` 模式可手动刷新，刷新运行记录状态、行数、耗时、物化表和错误摘要会持久化，成功刷新后的物化表会进入查询执行路由。
- `JdbcBiDatasetExtractMaterializer` 在可用 JDBC 数据源上创建租户隔离的 `bi_extract` 物化表，并按数据集租户列和行数上限抽取数据；运行环境缺少目标数据源或物化 schema 时会明确失败并记录刷新失败。
- API/HTTP_JSON 数据集的 `EXTRACT` 刷新通过数据集 `modelJson` 中的 `dataSourceConfigId`、`sourceKey` 和 `connectorType` 识别 API 源，并复用 `apiResponseVariables` 调用 `BiDatasourceRuntimeService.previewApiData(...)` 执行配置好的 HTTP JSON 请求，把返回行按数据集字段写入 `bi_extract` 物化表；若响应包含租户列则按当前租户过滤，若租户列缺失则用当前租户补齐目标租户列，确保后续查询仍走统一的租户条件。
- CSV/Excel 上传文件数据集的 `EXTRACT` 刷新通过 `CSV_EXCEL`、`FILE`、`FILE_UPLOAD` 或 `file-` source key 识别文件源，复用 `BiDatasourceRuntimeService.previewFileData(...)` 解析 CSV/XLS/XLSX 行数据，把文件列映射到数据集字段并写入 `bi_extract` 物化表；若文件包含租户列则按当前租户过滤，若文件不含租户列则注入当前租户，保持查询期统一租户隔离。
- `BiDatasetAccelerationSchedulerService` 提供数据集抽取加速的到期刷新 worker，自动调度默认通过 `canvas.bi.dataset.acceleration.scheduler.enabled:false` 关闭；它只处理启用的 `EXTRACT` + `SCHEDULED` 策略，支持基于 `refreshIntervalMinutes` 和 `cronExpression` 的 due-check，通过 `BI_DATASET_ACCELERATION_SCHEDULER` 租约避免多实例重复刷新，并在成功刷新后按数据集失效查询结果缓存；`POST /canvas/bi/datasets/resources/acceleration-scheduler/run` 允许当前租户运营侧手动运行一次 due-check，返回 `policiesChecked`、`refreshed`、`skipped`、`failed` 和逐策略 `items[]` 明细，明细包含 dataset key、`REFRESHED`/`SKIPPED`/`FAILED` 状态、原因、refresh run、行数、耗时、物化表和开始/结束时间，工作台数据集加速面板提供“运行抽取调度”按钮、结果摘要和调度明细表，用于观察 API/JDBC/文件 EXTRACT 刷新调度是否真正执行以及失败原因。
- 抽取保留治理默认保留最近 2 个成功物化表（`canvas.bi.dataset.acceleration.extract.retained-tables` 可配置），刷新成功后会安全 drop 旧物化表并把对应刷新 run 标记为 `DROPPED`；`GET /canvas/bi/datasets/resources/{datasetKey}/acceleration-capacity` 返回成功/失败刷新次数、活跃/已清理/待清理物化表数、保留行数、最近行数和耗时，`POST /canvas/bi/datasets/resources/{datasetKey}/acceleration-cleanup` 支持运营侧手动按保留数清理旧抽取表。
- API 数据源 runtime foundation 支持 `HTTP_JSON` / `API` 源在不走 JDBC 的情况下执行连接测试、schema preview、schema sync 和手动行数据预览：运行时从 `connector_config_json` 读取请求方法、认证类型、headers、query parameters、body template 和 `responseRowsPath`，使用加密 credential 生成运行时认证头，调用 HTTP JSON endpoint，支持 `{{var}}` 与 `${var}` 模板变量替换，从配置的 rows path 抽取样本行并推断 `BIGINT`、`DOUBLE`、`VARCHAR`、`BOOLEAN` 字段，返回 `api-{id}` source key 和 `api_response` 表预览。`POST /canvas/bi/datasources/{id}/api-preview` 支持传入多组模板变量和行数上限，工作台对 API 数据源展示可增删的预览变量名/变量值行和行数控件，并在提交前把行数归一到 1-1000；`POST /canvas/bi/datasources/{id}/schema-sync` 可接收同一请求体，使需要多组 templated headers/query/body 的 API 数据源可以先同步响应 schema，再直接生成 EXTRACT 数据集草稿；后端按 10MB 响应、1000 行和 100 列限制返回推断列、样本行、截断标记和耗时；持久化 connector config 仍保持敏感值脱敏，运行时错误会清理 URL/credential secret。
- Quick 引擎容量治理已新增租户级容量策略表 `bi_quick_engine_capacity_policy`，`GET /canvas/bi/capacity/quick-engine` 汇总当前租户成功且未被保留治理标记为 `DROPPED` 的抽取物化表容量，按数据集和用户排行返回使用行数、活跃表数、容量占比和 `NORMAL`/`WARNING`/`CRITICAL` 告警等级，并返回租户容量池策略与最近查询历史派生的运行中、排队、阻塞、失败、成功、缓存命中队列观测；`POST /canvas/bi/capacity/quick-engine/alert-policy` 可更新容量上限、告警阈值、通知渠道和接收人并写入审计，`POST /canvas/bi/capacity/quick-engine/tenant-pool-policy` 可更新 pool key、并发上限、队列上限、排队超时和权重并写入审计；`GET /canvas/bi/capacity/quick-engine/queue` 返回当前租户 durable queue 的 status count 和最近 job 列表，支持按 pool/status 过滤并限制返回条数，便于在 async worker 前观察队列积压、claim 和阻塞原因；`bi_quick_engine_queue_job` 和 `BiQuickEngineQueueService` 提供租户/pool 维度的持久化队列 job、过期转 `BLOCKED`、worker claim、跨租户/pool ready backlog 聚合、fair worker claim、claim 结果视图、claimed job 完成/阻塞、同步 queued admission 完成/阻塞、stale claim 恢复和队列快照 foundation；`BiQuickEngineQueueSchedulerService` 提供默认关闭的 stale claim 恢复与 fair worker claim 调度，启用后按租户和 pool key 在 `BI_QUICK_ENGINE_QUEUE_RECOVERY_{POOL}` 租约保护下执行过期 claimed job 阻塞、有效 stale claim 重新入队和跨实例 worker 批次唤醒；查询执行链路会在缓存未命中且进入真实数据源执行前执行租户容量池准入检查，准入成功会在配置租约服务时按容量池 slot key 获取跨实例运行槽租约，否则占用进程内租户运行槽，并在执行成功或失败后释放对应 slot/租约；并发已满但队列未满时会同步等待 release 唤醒/租约重试，等待成功后把 tenant、pool、SQL hash、数据集、请求人和排队超时持久化为 `QUEUED` 队列 job，并先写入 `QUEUED` 查询历史再执行；数据源执行成功会把该 durable queue job 标记为 `COMPLETED`，执行失败会标记为 `BLOCKED` 并记录失败摘要；队列已满、等待超时或等待中断时写入 `BLOCKED` 查询历史并拒绝执行。
- `JdbcBiQueryExecutor` 根据数据集表表达式路由 JDBC 数据源，`canvas_dws.*` 和 `canvas_ods.*` 走可选 Doris JDBC，其他数据集可走主库 JDBC。
- `JdbcBiQueryHistoryRecorder` 将请求 JSON、SQL hash、行数、耗时、状态和错误摘要写入 `bi_query_history`；历史写入失败不影响报表读取。
- `JdbcBiQueryHistoryReader` 提供租户内最近查询历史读取，并支持按历史 ID 读取单条详情；前端展示数据集、执行人、状态、行数、耗时和 SQL hash 摘要，可打开详情抽屉查看恢复后的结构化 `BiQueryRequest`、字段、过滤、排序、分页、错误和创建时间。
- `BiDatasourceHealthProvider` 暴露主库和 Doris 的健康状态，并提供最近健康检查快照历史；前端展示数据源可用性和最近健康历史，帮助解释实时数据和预览降级。
- `BiQueryResultCache` 提供按 SQL hash 的结果缓存，默认本地内存缓存、TTL 300 秒、最大 500 条，可通过 `canvas.bi.query.cache.*` 配置开关、TTL 和容量；缓存 provider 暴露 `BiQueryCacheStats` 快照，包含 provider、启停、当前条数、容量上限、TTL、命中/未命中、写入和驱逐计数。
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
- 查询控件运行态：URL/embed 参数优先，用户记住查询条件次之，控件默认值兜底；控件可配置作用范围，只影响目标组件；下级控件候选值按显式父控件级联规则执行 option query，同源级联直接复用父字段，非同源级联通过字段映射约束目标候选集，父控件隐藏时其默认值或全局参数仍可进入级联过滤。
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
- `GET /canvas/bi/dashboards/resources/{dashboardKey}/runtime-state` 读取当前租户、当前用户、当前看板的运行态参数，用于恢复 QuickBI-like 记住查询条件。
- `POST /canvas/bi/dashboards/resources/{dashboardKey}/runtime-state` 保存当前用户的看板运行态参数，后续看板打开时参与 URL 参数、记住条件和控件默认值的合成。
- 前端运行态编辑器已支持顶部控件栏和右侧交互面板同步编辑、显示参数数量、保存状态、每个控件的当前值来源（URL 覆盖、记住条件、默认值、已清除）和锁定状态，按默认值重置并剥离 URL 中的查询控件/全局参数覆盖项，同时保留 dashboard/canvasId 等上下文参数。
- 嵌入页复用工作台同一套运行态解析 helper，将已签名 ticket payload 中的 `filters` 与全局 `parameters` 合成为 dashboard runtime parameters，再用于组件查询和页面运行态标签展示，避免外部嵌入与认证工作台出现参数双轨。
- 前端工作台优先加载持久化资源，顶部展示来源、状态和版本，保存、发布、复制、导出、导入、归档按钮已接入上述 API；复制/导入后跳转到新看板，导出后显示当前资源包版本，资源包文件导出/导入使用后端文件端点，画布下方展示发布历史表格，发布成功后自动刷新快照列表，并可从历史版本恢复草稿。
- 已有仪表板保存草稿需通过 `EDIT` 资源权限，并在保存 endpoint 携带当前用户有效的 `X-BI-LOCK-TOKEN` 编辑锁 token；发布需通过 `PUBLISH` 资源权限，版本恢复回到草稿保存路径并继承相同编辑权限校验。

发布审批门禁已接入数据集、仪表板、图表和门户发布链路：非 `ADMIN`、`SUPER_ADMIN`、`TENANT_ADMIN` 角色发布时必须存在同资源、同工作空间的已通过审批，且审批时间不能早于资源最后更新时间；管理员角色可跳过审批。

资源移动/转让/收藏/评论/编辑锁 foundation、保存与版本恢复写入链路编辑锁强制校验、发布审批和保存/发布资源权限校验已接入统一资源管理与 lifecycle 路径，工作台可对当前仪表板、选中图表、选中数据集或选中门户执行移动、转让、收藏、评论、锁操作和发布审批请求/审核操作；仪表板 helper 已支持布局缩放与撤销/重做历史，画布卡片已提供标题区拖拽移动和右下角拖拽缩放手柄，顶部工具栏已接入撤销/重做、多选左/中/右/顶/垂直居中/底对齐以及桌面/平板/手机布局预览，前端渲染已使用真实 20 栅格 `gridX/gridY/gridW/gridH` 定位，并可把同一看板映射到 12 栅格平板布局或 1 栅格手机单列布局；移动/缩放时做基础碰撞避让；拖拽移动会在阈值内吸附到其它 widget 的边线或中心线，并返回 guide 元数据供后续视觉辅助线渲染；设计器键盘操作已支持方向键移动选中组件、Shift+方向键缩放、Delete/Backspace 删除、Ctrl/Cmd+D 复制、Ctrl/Cmd+Z 撤销和 Ctrl/Cmd+Y 或 Cmd+Shift+Z 重做。

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
- 前端工作台已接入图表基础编辑器：可编辑已保存图表的图表类型、数据集、维度字段、指标字段和取数 limit，并把字段表单转换为结构化 `BiQueryRequest` 后复用 `POST /canvas/bi/charts/resources/{chartKey}/draft` 保存草稿；复制图表会生成 `*-copy` 草稿并保留查询、样式和交互配置；编辑器展示数据集、维度数和指标数的引用影响摘要。

后续仍需要补齐：图表字段拖拽/拖放生成查询、更完整筛选/排序/样式/交互编辑表单、跨仪表板/门户/订阅的图表引用影响分析。图表资源可通过统一资源管理 band 进行移动/转让/收藏/评论/锁定，并通过统一 lifecycle guard 执行保存/发布权限、保存与版本恢复写入链路编辑锁与发布审批。

### 4.7 数据大屏

数据大屏用于值班室、战情室、活动大促。支持：

- 全屏展示。
- 大屏尺寸和背景。
- 自由定位组件。
- 自动刷新。
- 图表联动、钻取、跳转。
- 运营播报组件。

当前已落地大屏资源草稿/发布 lifecycle、运行态全屏栅格展示，以及工作台内对选中组件标题、资源绑定、x/y/w/h 坐标尺寸、方向移动、宽高步进缩放、多组件对齐、邻近组件参考线吸附和移动端单列/紧凑双列布局变体的可视化编辑与保存；工作台已接入大屏组件库基础模板，可添加指标卡、趋势折线、排行列表和文本面板组件并写入草稿 layout。

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

当前已落地电子表格资源草稿/发布 lifecycle、运行态网格渲染，以及工作台内 sheet/cell 选择、单元格值/公式编辑、空值清除、范围批量填充、单元格粗体/背景色/文字色编辑和保存；运行态支持单元格引用和 `SUM(A1:B2)`、`AVERAGE(A1:B2)`、`MIN(A1:B2)`、`MAX(A1:B2)`、`COUNT(A1:B2)` 区间公式求值，并对循环引用展示错误标记，会渲染已保存的单元格样式，且可按 sheet 级 `mobileLayout` 限制移动端列数；工作台已支持基于源单元格区域生成交叉表透视配置与汇总结果单元格。

Quick BI 自助取数参考落点：

- 自助取数可以从工作台、自助取数列表、数据集操作入口和数据集编辑页开始创建。
- 配置页需要先选择数据集，再配置行列字段、过滤器、查询控件和取数结果。
- 下载格式需要覆盖 CSV/Excel；大数据量场景需要异步任务、行数/文件大小限制和下载任务列表。
- 自助取数结果必须处在行级权限和列级权限控制范围内，导出还需要单独的导出权限。
- 企业导出治理需要覆盖事前导出开关/权限/审批、事中任务状态和下载渠道控制、事后下载审计和过期清理。

当前自助取数 foundation：

- `POST /canvas/bi/self-service/preview` 基于结构化 `BiQueryRequest` 进行预览，复用查询执行链路，因此自动继承租户隔离、行权限、列权限、脱敏、字段治理和查询历史。
- `POST /canvas/bi/self-service/exports` 创建异步导出任务并返回 `QUEUED`；任务处理器会从 `bi_export_job` 恢复原始结构化请求、执行查询，并通过 `BiFileStorage` 写入可下载对象；`BiFileStorage` 支持 byte array 写入和 streaming writer 写入，本地 provider 对 streaming writer 直接落盘，避免大 ZIP 先整体进入内存；默认 provider 是本地文件实现，表模型保存 `storage_provider`/`storage_key`；当 `canvas.bi.storage.provider=s3` 时，导出和订阅附件会通过 S3-compatible provider 写入 S3/MinIO/OSS S3 兼容端点，支持 endpoint、region、bucket、access key、secret key、key prefix、path-style、public base URL 和可选 bucket lifecycle 策略配置；敏感导出、显式 `approvalRequired` 或超过 `canvas.bi.export.approval.row-threshold` 的任务会进入 `PENDING_APPROVAL`，不会执行查询或生成文件。
- `GET /canvas/bi/self-service/exports` 返回最近导出任务。
- `GET /canvas/bi/self-service/exports/{id}` 返回当前租户内单个导出任务的审计详情，包含任务状态、存储、下载、审批、重试元数据以及恢复后的原始结构化导出请求。
- `POST /canvas/bi/self-service/exports/{id}/review` 由管理员角色审批或驳回待审批导出；批准后标记为 `QUEUED` 等待异步处理，驳回后标记 `REJECTED`。
- `POST /canvas/bi/self-service/exports/{id}/cancel` 允许当前租户操作人取消 `QUEUED`、`PENDING_APPROVAL` 或 `FAILED` 导出任务，任务会标记为 `CANCELED`、清空下载 URL 和重试时间，并记录取消操作者。
- `POST /canvas/bi/self-service/exports/queue/run` 手动处理当前租户的 `QUEUED` 导出任务，返回 checked/processed/completed/failed 计数和处理后的任务视图；生产环境也可启用 `canvas.bi.export.queue.*` 定时处理配置。
- `GET /canvas/bi/self-service/exports/{id}/download` 下载导出文件；可通过 `canvas.bi.export.download.rate-limit-per-minute` 启用租户内按用户的分钟级下载限流，成功下载写入 `BI_EXPORT_DOWNLOAD` 审计，被限流下载写入 `BI_EXPORT_DOWNLOAD_RATE_LIMITED` 审计；分片 ZIP 下载审计会包含 manifest 元数据和扁平化 `partStorageKeys`，已过期任务拒绝下载，并在标记 `EXPIRED` 前删除已记录的本地或 S3-compatible 存储对象，分片导出会先读取 ZIP manifest 并删除其中声明的 part 对象。
- `POST /canvas/bi/self-service/exports/cleanup` 清理当前租户已过期导出任务，删除已记录的本地或 S3-compatible 存储对象和分片 part 对象，并返回检查数、过期数、删除文件数和失败数。
- `POST /canvas/bi/self-service/exports/retry` 对当前租户到达 `next_retry_at` 的失败导出任务执行一次性重试，恢复原始结构化查询、复用 `BiFileStorage` 写文件，并返回 checked/retried/completed/failed 计数和重试后的任务视图。
- 导出格式支持 `CSV`、`JSON`、`XLSX` 和 `PDF`；导出前会校验数据集 `EXPORT` 资源权限，查询过程继续执行行列权限和脱敏。PDF 导出会写入可下载的 `application/pdf` 对象，包含标题、数据集、行数、列头、数据行和页脚。
- 大体量 `CSV` 导出支持对象级分片能力：请求行数最高限制为 1,000,000；超过 10,000 行时，处理器按 10,000 行一页执行结构化 `BiQueryRequest`，通过 `LIMIT`/`OFFSET` 拉取分页结果；每页先写入独立 storage part 对象，再用 streaming writer 写入 `application/zip` 下载对象；ZIP 内保持 `manifest.json` 和 `part-00001.csv`、`part-00002.csv` 等分片文件，manifest 记录 `storageLayout=OBJECT_PER_PART_ZIP`、数据集、请求行数、生成行数、分片数、分片大小、完整性标记，以及每个 part 的 `storageKey`、`rowCount`、`sizeBytes` 和 `sha256`；若最终 ZIP 写入失败，处理器会删除本次已生成的 part 对象后保留失败任务的重试状态。
- `BiSelfServiceExportService.restoreExportObjects` 支持对象级恢复演练：对已完成导出任务校验租户与对象存储 key，从备用 `BiFileStorage` provider 恢复缺失的根下载对象，并解析 ZIP manifest 中的 part `storageKey` 逐个恢复缺失分片，返回 primary/fallback provider、检查对象数、恢复对象数、缺失对象数、恢复 key 和缺失 key，用于跨 provider 分片恢复验证。
- 未启用全局对象存储时，默认本地 storage 根目录为 `${java.io.tmpdir}/canvas-bi-exports`，可通过 `canvas.bi.export.dir` 配置；导出任务默认保留 7 天，可通过 `canvas.bi.export.retention-days` 配置。
- 启用 `canvas.bi.storage.s3.lifecycle.enabled=true` 时，S3-compatible storage 启动时会向 bucket 写入 lifecycle policy：`{keyPrefix}exports/` 使用 `canvas.bi.export.retention-days`，`{keyPrefix}attachments/` 使用 `canvas.bi.delivery.attachment.retention-days`，作为应用层过期清理之外的 provider-native 兜底保留策略。
- 下载会累计 `download_count` 和 `last_downloaded_at`；已过期导出会拒绝下载并标记为 `EXPIRED`，清理接口会通过 `BiFileStorage` 删除根对象和 manifest 声明的 part 对象，同时保留旧本地路径行的兼容清理。
- 任务表保存并返回 `progress_percent`、`retry_count`、`max_retry_count`、`next_retry_at`、`last_retry_at` 和 `retry_exhausted_at`；失败任务按 `canvas.bi.export.retry.*` 配置写入可轮询的重试状态，默认最多 3 次、初始延迟 15 分钟、指数退避倍率 2、最大延迟 1440 分钟。
- 前端 BI 工作台展示字段拖拽取数构建器、自助取数预览、普通 CSV 导出、敏感导出申请、导出任务列表、审批状态、批准/驳回操作、可取消任务入口、保留期、下载次数、过期状态、进度条、失败重试状态、手动重试结果、清理结果、下载入口和导出审计详情抽屉。

当前自助取数导出硬化已覆盖流式分片写入、对象级分片保留、超大导出取消、下载限流、分片下载审计，以及跨 provider 根对象/分片对象恢复演练。

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
- 前端工作台已接入门户基础编辑器：可编辑顶部/左侧/双导航布局、默认主页、菜单搜索开关、全屏开关、移动端开关，并可选择菜单执行上移/下移重排；保存门户草稿复用 `POST /canvas/bi/portals/resources/{portalKey}/draft`，有效编辑锁会随请求携带。

后续仍需要补齐：门户 LOGO/标题/页脚/面包屑/缓存/别名等完整配置，菜单树拖拽式编排、多级菜单编辑和图标配置，门户预览页和嵌入渲染。

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
- `POST /canvas/bi/alerts/{id}/run` 手动检测告警，执行语义层指标查询；静态阈值按 `GT/GTE/LT/LTE/EQ/NEQ` 判断，异常型条件支持 `ANOMALY_DROP`、`ANOMALY_RISE` 或 `mode/type=ANOMALY`，默认 `model=POINT` 基于同一告警最近 `EVALUATION` 指标值计算当前点相对基线均值、标准差、偏离值和偏离百分比；`model=MOVING_AVERAGE`/`MOVING_AVG`/`ROLLING_AVERAGE` 支持 `comparisonWindow`、`minComparisonSamples` 和 `baselineWindow`，用当前值加最近历史样本组成对比窗口，并与更早历史基线窗口比较；`model=PERIOD_OVER_PERIOD`/`CALENDAR` 支持 `period=DAY_OVER_DAY`/`WEEK_OVER_WEEK`/`MONTH_OVER_MONTH`/`YEAR_OVER_YEAR`、`calendarWindowHours`、`baselineWindow` 和 `minSamples`，从同一告警历史 `EVALUATION` 日志中选取目标日历窗口内的样本作为同比/环比基线；payload 会写入 `model`、`baselineSampleCount`、`comparisonWindow`、`comparisonSampleCount`、`minComparisonSamples`、`comparisonAverage`、`period`、`calendarWindowHours`、`targetWindowStart`、`targetWindowEnd`、`baselineAverage`、`standardDeviation`、`delta`、`deltaPercent` 和 `threshold`；样本不足时只写 `SKIPPED` 检测日志，命中后生成渠道投递日志；若 condition 配置 `silence`/`mute`/`quietHours`/`silenceWindow`，或顶层配置 `silenceEnabled`、`muteUntil`、`silenceUntil`、时间窗口、星期和原因，则命中的告警会写入带 silence payload 的 `SKIPPED` 评估日志并抑制渠道投递。
- `GET /canvas/bi/delivery-logs` 查询订阅和告警投递流水。
- `GET /canvas/bi/delivery-audit` 按 jobType、status、channel、jobId 和 limit 返回投递审计窗口，汇总 total、delivered、triggered、skipped、pending、failed、retryable 和 retryExhausted，并返回对应明细。
- `GET /canvas/bi/delivery-attachments` 查询订阅投递附件，`GET /canvas/bi/delivery-attachments/{id}/download` 下载已生成文件，`POST /canvas/bi/delivery-attachments/cleanup` 清理过期附件对象并兼容旧本地路径。
- 订阅运行时会根据 `delivery.content` 和 `delivery.attachment(s)` 生成服务端快照/附件：`SNAPSHOT_LINK`/`SNAPSHOT` 默认生成 HTML 快照，`snapshotFormat`/`screenshotFormat` 设置为 `PNG` 或 `JPEG` 时会调用可配置 HTTP browser renderer 生成图片截图，`CSV`/`JSON`/`XLSX`/`PDF` 生成投递摘要附件，其中 PDF 会按摘要内容自动分页并生成多页 Page/Contents 对象，每页写入 `Page X of N` 页脚并转义 PDF literal 文本，附件元数据会写入 payload；附件通过 `BiFileStorage` 写入并记录 `storage_provider`/`storage_key`，默认本地 storage 根目录为 `${java.io.tmpdir}/canvas-bi-delivery-attachments`，启用 `canvas.bi.storage.provider=s3` 后复用同一 S3-compatible 对象存储 provider，并可通过 `canvas.bi.storage.s3.lifecycle.enabled=true` 把附件前缀保留期同步为 bucket lifecycle rule；附件默认留存 7 天，可通过 `canvas.bi.delivery.attachment.retention-days` 调整；下载会累计下载次数和最后下载时间，过期附件拒绝下载并在标记 `EXPIRED` 前删除已记录的本地或 S3-compatible 存储对象；清理任务也会删除过期附件对象；邮件渠道会读取生成文件并作为 MIME 附件发送，飞书、钉钉、企微和 Webhook 等非邮件渠道使用文本下载链接；失败邮件重试会从历史 payload 中的附件 ID 重新下载文件并回放为 MIME 附件。
- `BiSnapshotRenderer` 定义浏览器截图渲染 SPI；`HttpBiSnapshotRenderer` 默认关闭，通过 `canvas.bi.delivery.snapshot.renderer.enabled:true` 和 `canvas.bi.delivery.snapshot.renderer.url` 接入单个内部 Playwright/Browserless 渲染服务，也可通过 `canvas.bi.delivery.snapshot.renderer.urls` 配置逗号分隔的 renderer endpoint 集群。渲染时按轮转起点尝试 endpoint，当前 endpoint 网络、HTTP、JSON 或图片数据失败时自动故障转移到下一个 endpoint；协议为 JSON 请求和 base64 图片响应，支持 PNG/JPEG、宽高和缩放倍率。
- `BiDeliveryAdapterService` 提供外部投递 adapter foundation，支持 `EMAIL`、`WEBHOOK`、`LARK`、`FEISHU`、`DINGTALK`、`DING`、`WECOM`、`WECHAT_WORK` 和 `ENTERPRISE_WECHAT` 等渠道；Lark/飞书、钉钉和企业微信机器人采用文本消息 payload，通用 Webhook 采用 `BI_DELIVERY` 结构化 payload。
- `BiSmtpEmailDeliveryClient` 提供无第三方依赖的 SMTP 邮件 foundation，默认关闭，可通过 `canvas.bi.delivery.email.*` 配置启用，支持基本 SMTP、SSL、STARTTLS、AUTH LOGIN、纯文本正文和 `multipart/mixed` 附件。
- `POST /canvas/bi/delivery-logs/retry` 对到达 `next_retry_at` 且未耗尽的 `PENDING_ADAPTER` 和 `FAILED` 非检测日志进行一次性重试，复用同一 adapter 路径并写入新的重试日志；重试策略默认最多 4 次、初始延迟 30 分钟、指数退避倍率 2、最大延迟 1440 分钟，可通过 `canvas.bi.delivery.retry.*` 配置。
- `BiDeliverySchedulerService` 提供订阅/告警 due-check 调度能力，自动调度默认通过 `canvas.bi.delivery.scheduler.enabled:false` 关闭；支持 `HOURLY`、`DAILY`、`WEEKLY`、`MONTHLY`、`intervalMinutes`、`checkIntervalMinutes` 和 `cronExpression`，并基于 `bi_delivery_log` 最近运行时间避免同一周期重复投递；自动调度路径通过 `bi_delivery_scheduler_lease` 抢占租约，避免多实例重复触发同一租户的订阅/告警检查。
- `POST /canvas/bi/delivery-scheduler/run` 提供运营侧一次性调度检查，使用当前租户、用户和角色上下文，便于在后台调度关闭时手动补跑 due 任务。
- 创建订阅和告警时复用 `BiPermissionService.ACTION_SUBSCRIBE`，把订阅/告警纳入同一资源权限路径。
- 前端工作台新增“订阅推送”和“指标告警”区域，支持查看已有任务、创建当前看板日报订阅、为当前选中指标创建阈值告警或基线异常告警、手动执行/检测、触发一次调度检查、重试失败/待配置投递、查看投递记录与审计汇总、查看重试次数/下次重试/耗尽状态、下载投递附件、查看附件过期/下载次数并手动清理过期附件。
- 告警异常模型已支持点异常、移动平均和 `PERIOD_OVER_PERIOD` 同比/环比模型，覆盖日环比、周同比、月环比、年同比；周期模型支持可配置 calendar tolerance window、自然周期边界对齐，以及节假日 comparison date/name 映射，检测日志 payload 会写入 period、target window、naturalBoundary、holidayAdjusted、holidayComparisonDate、baseline sample 和 delta 细节。

后续仍需要补齐：内置浏览器截图执行集群。

### 4.11 嵌入分析

支持将仪表板、电子表格、大屏、自助取数和问数嵌入 Canvas 或第三方系统：

- 内部嵌入：登录态透传，使用当前用户权限。
- 外部嵌入：短期 embed token。
- token 绑定 tenant、user、resource、scope、筛选条件、全局参数、过期时间、nonce、访问次数和频率窗口。
- 支持 URL 参数传入筛选条件。
- token 不能暴露数据源凭证或 SQL。

当前实现：

- `POST /canvas/bi/embed-tickets` 签发 HMAC-SHA256 短期 ticket。
- `POST /canvas/bi/embed-tickets/verify` 匿名校验 ticket，用于第三方 iframe 打开嵌入报表。
- `POST /canvas/bi/embed/resources/dashboard` 匿名加载签名 ticket 绑定的仪表板资源元数据，先校验 ticket 资源范围，再按来源白名单扣减访问次数/频率窗口，并使用 ticket 的 `tenantId` 读取持久化仪表板资源或预置兜底。
- `POST /canvas/bi/embed/resources/dashboard/runtime-state` 匿名加载签名 ticket 绑定的仪表板运行态参数，先校验 ticket 资源范围，再按来源白名单扣减访问次数/频率窗口，并使用 ticket 的 `tenantId`、`username` 和 `resourceKey` 读取当前用户记住的运行态状态。
- `POST /canvas/bi/embed/query/execute` 匿名执行签名 ticket 绑定的仪表板组件查询，先校验 ticket 资源范围和 `query.dashboardKey`，再扣减访问次数/频率窗口，并以 ticket 的 `tenantId`/`username` 构造 `BiQueryContext` 复用查询执行、资源权限、行权限和列权限链路。
- `POST /canvas/bi/embed-tickets/cleanup` 按当前租户批量回收已过期且未消费的持久化 token。
- ticket payload 绑定 `tenantId`、`username`、`resourceType`、`resourceKey`、`scope`、`filters`、`parameters`、`allowedDomains`、`maxAccessCount`、`rateLimitPerMinute`、`nonce`、`issuedAt`、`expiresAt`。
- TTL 默认 10 分钟，最短 1 分钟，最长 30 分钟；外部 ticket 的前端默认申请 15 分钟。
- 前端 BI 设计器顶部“嵌入”按钮调用 ticket API，并在右侧交互面板展示短期 `embedUrl` 与过期时间。
- 匿名嵌入页 `/bi/embed/:resourceType/:resourceKey` 已具备基础路由、ticket 校验、资源匹配校验、票据化持久化看板资源加载、票据化运行态读取和逐组件签名查询渲染。
- 当前 ticket 已支持 `bi_embed_token` token hash 持久化、nonce/source 绑定、数据库原子访问次数与频率窗口扣减、访问审计、租户级过期回收、访问次数/频率窗口限制、embed 参数 claim 绑定、票据化仪表板资源元数据读取、票据化运行态读取、真实数据查询渲染和嵌入态查询权限链路复用；嵌入页运行态参数合成遵循 ticket filters/parameters 优先、记住查询条件次之、控件默认值兜底的 QuickBI-like 优先级。

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
- `BiPermissionService` 会在有 `bi_workspace_member` 记录时，把当前用户在该工作空间的 `role_key` 作为资源权限、行权限和列权限的有效角色；未配置成员记录时继续使用请求上下文中的全局角色。
- 查询编译和查询执行都会先经过权限准备：资源权限 `DENY` 会阻断数据集使用，行权限会追加为结构化 `BiFilter`，列权限 `DENY` 会在 SQL 生成前拒绝。
- 列权限 `MASK` 会在查询执行后对返回行做字段级脱敏，并把脱敏配置纳入查询缓存签名，避免不同权限用户共享错误缓存。
- 菜单 `visibility_json` 支持 `roles/users/denyRoles/denyUsers` 的服务级过滤方法，供后续门户查看态 API 复用。
- 权限拒绝会写入 `bi_audit_log`；现有 CDP 字段治理仍在权限准备之后执行，形成“BI 权限 + 字段治理”的叠加校验。
- `BiPermissionAdminService` 和 `BiPermissionController` 已提供资源授权、行权限、列权限的 list/upsert/delete 管理 API，支持通过数据集、仪表板、图表、门户、大屏、电子表格和数据源 key 解析资源 ID。
- 前端 BI 工作台已展示资源授权、行权限、列权限三类规则，资源授权目标与共享资源操作目标分离；权限治理可选择当前仪表板、图表、数据集、数据源、门户、大屏或电子表格授予 `USE`/`EDIT`，并保留数据集行权限和字段脱敏的模板化新增动作。
- `BiResourcePermissionGuard` 已接入数据集、仪表板、图表和门户 lifecycle：已有资源保存/恢复草稿执行 `EDIT`，发布执行 `PUBLISH`，首创资源因尚无资源 ID 暂不阻断创建。
- `BiPublishApprovalService.requireApprovedApproval(...)` 已接入数据集、仪表板、图表和门户发布路径，非管理员发布必须有覆盖当前资源更新时间的已批准发布审批；`ADMIN`、`SUPER_ADMIN` 和 `TENANT_ADMIN` 可绕过发布审批。
- 当前尚未完成完整权限编辑器，以及嵌入/真实外部投递统一鉴权。

### 4.13 AI 分析

参考 Quick BI 智能小Q，拆为五类 Agent：

- 问数 Agent：自然语言转结构化查询。
- 解读 Agent：解释图表、仪表板、异常波动。
- 报告 Agent：生成日报、周报、月报和管理层摘要。
- 搭建 Agent：根据用户意图生成图表和仪表板草稿。
- 洞察 Agent：自动发现趋势、异常、归因和机会。

AI 必须只使用数据集语义层，不直接访问数据库。生成查询前要走字段权限和行级权限校验。

当前 AI Agent 后端/API 已落地：

- `POST /canvas/bi/ai/ask`：问数 Agent 接收自然语言问题、可选数据集和 AI provider 参数，`BiAskDataAgentService` 只把数据集语义 catalog 暴露给 planner，planner 返回结构化 `BiQueryRequest` 后必须通过现有 `BiQueryExecutionService` 执行，因此复用租户注入、资源权限、行权限、列权限、字段治理、缓存和查询历史路径；planner 返回未知字段或 catalog 外数据集会在查询执行前被拒绝。
- `POST /canvas/bi/ai/interpret`：解读 Agent 接收已校验的语义查询和查询结果，先通过 `BiQueryCompiler` 校验字段、指标、维度组合和租户参数化 SQL 生成能力，再把语义 catalog、query、result 交给 planner。
- `POST /canvas/bi/ai/report`：报告 Agent 对每个报告 section 的 `BiQueryRequest` 与 `BiQueryResult` 做语义层校验和数据集一致性校验，再生成日报、周报、月报或管理摘要。
- `POST /canvas/bi/ai/dashboard-draft`：搭建 Agent 从语义 catalog 生成 `BiDashboardPreset` 和可选 `BiChartResource` 草稿；返回前会校验每个 widget/chart 的字段、指标、数据集和 query，拒绝未知字段。
- `POST /canvas/bi/ai/insights`：洞察 Agent 对当前/基线结果使用同一语义查询和数据集一致性校验，生成趋势、异常和机会列表。

默认 `LlmBi*Planner` 适配器复用 `AiLlmGateway`，内置模板 `BI Ask Data Planner`、`BI Interpretation Agent`、`BI Report Agent`、`BI Dashboard Draft Agent` 和 `BI Insight Agent` 提供 mock provider fallback。

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

截至 2026-06-06，第一阶段已落地：

- BI 元数据 Flyway 基础表：工作空间、数据源引用、数据集、字段、指标、图表、仪表板、门户、权限、订阅、告警、嵌入 token、审计等。
- 结构化查询编译器：白名单数据集、字段、指标、筛选、排序和租户条件注入，只生成参数化 SQL，不接受前端 SQL。
- 查询执行 foundation：`POST /canvas/bi/query/execute` 通过 JDBC/Doris 执行已编译查询，返回列、行、行数、耗时和 SQL hash，并基础写入 `bi_query_history`。
- 查询治理可见性：`GET /canvas/bi/query/history` 返回最近查询历史，`GET /canvas/bi/query/history/{id}` 返回当前租户内单条查询历史详情和恢复后的结构化请求，`GET /canvas/bi/query/governance-summary` 返回当前租户近期查询总量、慢查询、失败、缓存命中、可配置默认/数据集级超时策略、可配置默认/数据集级导出行数配额、按数据集聚合的诊断摘要和慢查询归因，归因包含慢/总次数、超阈时长、慢查询失败、慢查询未命中缓存和最大行数，`GET/POST /canvas/bi/query/governance-policy` 读取和更新当前租户持久化默认/数据集级 timeout/quota 策略，策略变更会写入 `bi_audit_log` 的 before/after 审计快照且审计写入失败不阻断策略落库，`GET /canvas/bi/query/governance-audit` 返回当前租户最近策略变更审计，`POST /canvas/bi/query/explain` 在不执行取数查询的情况下返回后端数据源执行计划，`POST /canvas/bi/query/cancel/{sqlHash}` 对运行中的 JDBC 查询 statement 发起取消请求并写入当前租户/操作人的取消审计，`GET /canvas/bi/datasources/connectors` 返回 BI 数据源连接器能力目录，`GET /canvas/bi/datasources/onboarding` 返回当前租户已配置数据源的脱敏接入视图，`POST /canvas/bi/datasources/{id}/connection-test` 执行当前租户数据源连接测试，`GET /canvas/bi/datasources/{id}/schema-preview` 返回当前租户数据源表/字段元数据预览，`GET /canvas/bi/datasources/health` 返回主库/Doris 健康状态，`GET /canvas/bi/datasources/health/history` 返回最近持久化健康检查快照，`GET /canvas/bi/datasources/health/slo` 返回最近健康窗口的整体和分数据源可用率。
- 查询结果缓存：同一租户、同一结构化查询编译出的 SQL hash 会复用短期结果缓存，降低 QuickBI-like 看板多组件刷新对 Doris 的压力；默认使用本地内存 provider，可通过 `canvas.bi.query.cache.provider=redis` 切换为 Redis 分布式 provider，使用 `canvas.bi.query.cache.redis.key-prefix`、`canvas.bi.query.cache.ttl-seconds` 和 `canvas.bi.query.cache.enabled` 控制 key 空间、TTL 和开关；`bi_query_cache_policy` 支持租户默认、数据集级和看板级缓存策略，widget 查询、查询控件候选值查询和自助取数查询都会携带当前 `dashboardKey`，后端执行会优先按看板级策略解析有效缓存模式/TTL，再回落到数据集级和租户默认策略；`bi_query_governance_policy.quota_rows` 会在查询编译和数据源执行前阻断超过数据集有效行数配额的请求，并记录 `BLOCKED` 查询历史；`GET/POST /canvas/bi/query/cache-policy` 提供策略读写，`GET /canvas/bi/query/cache-stats` 返回 provider、容量、TTL、命中/未命中、写入和驱逐计数，`POST /canvas/bi/query/cache/invalidate` 支持按 SQL hash、数据集或全量清除缓存，策略变更写入 `bi_audit_log` 且工作台可查看/更新默认缓存开关与 TTL，并可对当前仪表板或当前数据集写入覆盖策略的缓存模式、TTL 和开关，缓存容量/命中率/写入驱逐观测可见，并可从治理面板触发当前数据集或全量缓存清理；数据集抽取加速的手动和定时刷新成功后会按数据集清理旧查询结果，避免物化表已更新但 widget 仍读到旧缓存。
- 权限执行与管理 foundation：查询编译和查询执行接入 `BiPermissionService`，支持数据集资源拒绝、工作空间成员角色执行、行权限过滤追加、列权限拒绝、列脱敏、权限缓存签名和拒绝审计；`/canvas/bi/permissions/*` 提供资源、行、列权限管理 API，资源/行/列权限 create/update/delete 会写入 `bi_audit_log` 的操作者、权限类型、操作和 before/after 快照且审计写入失败不阻断权限变更，`GET /canvas/bi/permissions/audit` 返回当前租户最近权限变更审计；`GET/POST /canvas/bi/permissions/requests` 和 `POST /canvas/bi/permissions/requests/{id}/review` 支持权限申请、审批和审批通过后自动授予申请人资源权限；工作台展示并可创建常见授权规则，同时展示最近权限变更审计；门户 runtime API 已接入菜单可见性过滤。
- 自助取数/导出 foundation：`/canvas/bi/self-service/preview`、`/canvas/bi/self-service/exports`、`/canvas/bi/self-service/exports/{id}/review`、`/canvas/bi/self-service/exports/{id}/cancel`、`/canvas/bi/self-service/exports/{id}/download`、`/canvas/bi/self-service/exports/queue/run`、`/canvas/bi/self-service/exports/retry` 和 `/canvas/bi/self-service/exports/cleanup` 支持授权数据集预览、CSV/JSON/XLSX 异步导出任务、敏感/大批量导出审批、任务取消、下载、下载审计、下载限流审计、过期拒绝、失败重试、本地/S3-compatible 过期对象清理，以及大 CSV 的 streaming/object-per-part ZIP 导出 manifest 校验和分片下载审计，前端工作台已提供预览、普通导出、敏感导出、任务列表、待审批通过/驳回、可取消任务入口、进度/重试状态、过期/下载状态、手动重试和清理入口。
- 订阅/告警运行 foundation：`/canvas/bi/subscriptions` 和 `/canvas/bi/alerts` 支持任务列表、创建/更新和删除；`/canvas/bi/subscriptions/{id}/run`、`/canvas/bi/alerts/{id}/run`、`/canvas/bi/delivery-scheduler/run`、`/canvas/bi/delivery-logs/retry`、`/canvas/bi/delivery-logs`、`/canvas/bi/delivery-audit`、`/canvas/bi/delivery-attachments`、`/canvas/bi/delivery-attachments/{id}/download` 和 `/canvas/bi/delivery-attachments/cleanup` 支持手动运行、告警检测、一次性调度检查、到期失败/待配置重试、投递流水、投递审计摘要、服务端快照/附件生成、下载审计、过期拒绝和本地/S3-compatible 附件清理；自动调度具备 `bi_delivery_scheduler_lease` 分布式租约，避免多实例重复触发；创建时校验资源、数据集、指标、接收渠道，并执行 `SUBSCRIBE` 权限检查；投递 adapter 支持 SMTP Email、站内通知、Webhook、Lark/飞书、钉钉和企业微信，邮件渠道发送真实 MIME 附件，失败邮件重试会回放历史附件，失败/待配置投递按可配置指数退避写入 `next_retry_at`、`retry_count` 和 `retry_exhausted_at`，非邮件渠道在文本消息中带附件下载链接；截图格式支持 HTML 默认快照和可配置 HTTP renderer/renderer endpoint 集群的 PNG/JPEG；前端工作台展示订阅任务、指标告警、运行按钮、调度按钮、重试按钮、重试状态、过期附件清理、投递记录、投递审计摘要、附件过期/下载状态和附件下载入口。
- 营销画布内置数据集：`canvas_daily_stats`。
- 持久化数据集资源：支持数据集、字段、指标整包保存、发布、归档、发布快照历史和历史版本恢复，并能转换为查询语义模型供图表和查询引擎使用。
- 数据源 schema 到数据集：`POST /canvas/bi/datasets/resources/from-datasource-schema` 复用最新成功 schema 快照创建单表/视图数据集草稿，`POST /canvas/bi/datasets/resources/from-datasource-schema/multi-table` 复用最新成功 schema 快照和前端多行 Join/复合条件/graph 节点模型创建多表 SQL 数据集草稿；工作台关系画布可拖拽表节点、选择 Join 边、设置左右端点表、设置 Join 类型、查看字段摘要/条件数，并从选中边上下文查看、编辑字段和条件操作符、添加和移除 Join 条件、一键添加全部同名字段条件或交换关系方向，当前坐标和复合条件会提交给创建命令；字段、基础指标、租户列隐藏、Join 条件、graph-canvas 节点坐标和 Join 派生关系边、模型来源元数据随草稿一起落入数据集 lifecycle。
- 上传文件数据源闭环：`POST /canvas/bi/datasources/file-upload` 支持租户隔离的 CSV/XLS/XLSX 上传接入，`POST /canvas/bi/datasources/file-upload/materialize` 支持上传后立即 schema sync、生成数据集草稿、启用 EXTRACT 策略并刷新物化表；无租户列文件会在建模与抽取时保留统一 `tenant_id` 配置并在物化写入时注入当前租户，前端工作台会在上传成功后刷新数据源、schema、数据集资产和加速策略状态。
- 营销画布预置看板：`canvas-effect`。
- 持久化仪表板资源：支持看板资源列表、详情、保存草稿、发布、复制、JSON 资源包导入导出、资源包文件下载上传、归档、版本递增、发布快照历史和历史版本恢复；未保存时可从预置看板降级加载。
- 持久化图表资源：支持图表资源列表、详情、保存草稿、发布、归档、发布快照历史和历史版本恢复；保存时校验结构化查询，防止图表绕过语义层。
- 持久化门户资源：支持门户资源列表、详情、保存草稿、发布、归档、发布快照历史、历史版本恢复和按当前用户/角色过滤的 runtime 查看态；前端工作台支持门户基础编辑器，可保存导航布局、默认主页、菜单搜索、全屏、移动端和菜单上下移动排序。
- BI 资源移动 lifecycle：`bi_resource_location` 记录数据集、仪表板、图表和门户的租户/工作空间内文件夹位置与排序；移动接口会校验资源存在、拒绝归档资源、校验安全 folder key，并保留操作人；前端工作台可读取位置列表，选择当前仪表板、选中图表、选中数据集或选中门户并移动到目标文件夹，位置表展示 folder、sort、movedBy 和 movedAt。
- BI 资源转让 lifecycle：`bi_resource_ownership` 记录数据集、仪表板、图表和门户的当前负责人以及 transferredBy/transferredAt；转让接口会校验资源类型、资源 key、负责人、租户/工作空间内资源存在性并拒绝归档资源；前端工作台读取负责人列表，选择当前仪表板、选中图表、选中数据集或选中门户并转让给目标账号，负责人表展示 ownerUser、transferredBy 和 transferredAt。
- BI 资源协作 lifecycle：`bi_resource_favorite` 记录当前用户收藏，`bi_resource_comment` 记录资源级和 widget 级评论并支持创建者软删除，`bi_resource_lock` 记录资源编辑锁、锁持有人、token 和过期时间；收藏和评论会拒绝归档资源，锁获取通过资源唯一键条件 upsert 避免并发编辑冲突，释放锁要求 token 与当前用户匹配；保存和版本恢复 dataset/dashboard/chart/portal 写入链路时会用 `X-BI-LOCK-TOKEN` 强制校验当前用户持有的未过期锁，管理员角色可跳过该编辑锁门禁；前端工作台在统一资源管理 band 中展示当前资源收藏状态、评论流和编辑锁状态，并可执行收藏/取消收藏、发送/删除评论、获取/释放锁。
- BI 发布审批与 lifecycle 权限：`bi_publish_approval` 记录数据集、仪表板、图表和门户的发布审批请求、状态、申请人、审批人和审批意见；请求接口会校验资源类型/key、默认工作空间和资源存在性并拒绝归档资源；审核接口只允许 `PENDING` 审批流转到 `APPROVED` 或 `REJECTED`；非管理员发布需通过同资源、同工作空间且覆盖资源最后更新时间的已批准审批，管理员角色可跳过审批；已有资源保存/恢复草稿执行 `EDIT` 权限，发布执行 `PUBLISH` 权限；前端工作台在统一资源管理 band 中展示当前资源审批列表，并可创建审批、批准和驳回待审批请求。
- 后端接口：`GET /canvas/bi/datasets`、`GET /canvas/bi/datasets/{datasetKey}`、`GET /canvas/bi/datasets/resources`、`GET /canvas/bi/datasets/resources/{datasetKey}`、`POST /canvas/bi/datasets/resources/{datasetKey}/draft`、`POST /canvas/bi/datasets/resources/{datasetKey}/publish`、`GET /canvas/bi/datasets/resources/{datasetKey}/versions`、`POST /canvas/bi/datasets/resources/{datasetKey}/versions/{version}/restore`、`DELETE /canvas/bi/datasets/resources/{datasetKey}`、`GET /canvas/bi/dashboards/presets`、`GET /canvas/bi/dashboards/presets/{dashboardKey}`、`GET /canvas/bi/dashboards/resources`、`GET /canvas/bi/dashboards/resources/{dashboardKey}`、`POST /canvas/bi/dashboards/resources/{dashboardKey}/draft`、`POST /canvas/bi/dashboards/resources/{dashboardKey}/publish`、`POST /canvas/bi/dashboards/resources/{dashboardKey}/clone`、`GET /canvas/bi/dashboards/resources/{dashboardKey}/export`、`GET /canvas/bi/dashboards/resources/{dashboardKey}/export-file`、`POST /canvas/bi/dashboards/resources/import`、`POST /canvas/bi/dashboards/resources/import-file`、`DELETE /canvas/bi/dashboards/resources/{dashboardKey}`、`GET /canvas/bi/dashboards/resources/{dashboardKey}/versions`、`POST /canvas/bi/dashboards/resources/{dashboardKey}/versions/{version}/restore`、`GET /canvas/bi/charts/resources`、`GET /canvas/bi/charts/resources/{chartKey}`、`POST /canvas/bi/charts/resources/{chartKey}/draft`、`POST /canvas/bi/charts/resources/{chartKey}/publish`、`GET /canvas/bi/charts/resources/{chartKey}/versions`、`POST /canvas/bi/charts/resources/{chartKey}/versions/{version}/restore`、`DELETE /canvas/bi/charts/resources/{chartKey}`、`GET /canvas/bi/portals/resources`、`GET /canvas/bi/portals/resources/{portalKey}`、`POST /canvas/bi/portals/resources/{portalKey}/draft`、`POST /canvas/bi/portals/resources/{portalKey}/publish`、`GET /canvas/bi/portals/resources/{portalKey}/versions`、`POST /canvas/bi/portals/resources/{portalKey}/versions/{version}/restore`、`DELETE /canvas/bi/portals/resources/{portalKey}`、`GET /canvas/bi/portals/runtime`、`GET /canvas/bi/portals/runtime/{portalKey}`、`GET /canvas/bi/resources/locations`、`POST /canvas/bi/resources/move`、`POST /canvas/bi/resources/locations`、`GET /canvas/bi/resources/ownerships`、`POST /canvas/bi/resources/transfer`、`GET /canvas/bi/resources/favorites`、`POST /canvas/bi/resources/favorites`、`DELETE /canvas/bi/resources/favorites/{resourceType}/{resourceKey}`、`GET /canvas/bi/resources/comments`、`POST /canvas/bi/resources/comments`、`DELETE /canvas/bi/resources/comments/{commentId}`、`GET /canvas/bi/resources/locks`、`POST /canvas/bi/resources/locks/acquire`、`POST /canvas/bi/resources/locks/release`、`GET /canvas/bi/resources/publish-approvals`、`POST /canvas/bi/resources/publish-approvals`、`POST /canvas/bi/resources/publish-approvals/{approvalId}/review`、`GET /canvas/bi/permissions/audit`、`GET /canvas/bi/permissions/requests`、`POST /canvas/bi/permissions/requests`、`POST /canvas/bi/permissions/requests/{id}/review`、`GET /canvas/bi/subscriptions`、`POST /canvas/bi/subscriptions`、`DELETE /canvas/bi/subscriptions/{id}`、`POST /canvas/bi/subscriptions/{id}/run`、`GET /canvas/bi/alerts`、`POST /canvas/bi/alerts`、`DELETE /canvas/bi/alerts/{id}`、`POST /canvas/bi/alerts/{id}/run`、`GET /canvas/bi/delivery-logs`、`GET /canvas/bi/delivery-audit`、`POST /canvas/bi/delivery-logs/retry`、`GET /canvas/bi/delivery-attachments`、`GET /canvas/bi/delivery-attachments/{id}/download`、`POST /canvas/bi/delivery-attachments/cleanup`、`POST /canvas/bi/delivery-scheduler/run`、`POST /canvas/bi/self-service/exports/queue/run`、`POST /canvas/bi/self-service/exports/retry`、`POST /canvas/bi/self-service/exports/cleanup`、`GET /canvas/bi/query/history`、`GET /canvas/bi/query/history/{id}`、`GET /canvas/bi/query/governance-audit`、`GET /canvas/bi/query/cache-policy`、`POST /canvas/bi/query/cache-policy`、`GET /canvas/bi/query/cache-stats`、`POST /canvas/bi/query/cache/invalidate`、`GET /canvas/bi/datasources/connectors`、`GET /canvas/bi/datasources/onboarding`、`POST /canvas/bi/datasources/{id}/connection-test`、`GET /canvas/bi/datasources/{id}/schema-preview`、`GET /canvas/bi/datasources/health/slo`、`POST /canvas/bi/embed-tickets/cleanup`、`POST /canvas/bi/query/compile`、`POST /canvas/bi/query/explain`、`POST /canvas/bi/query/cancel/{sqlHash}`、`POST /canvas/bi/query/execute`。
- Quick 引擎容量接口：`GET /canvas/bi/capacity/quick-engine` 返回当前租户容量水位、分类、资源明细、用户排行、租户容量池策略和并发/队列观测，`POST /canvas/bi/capacity/quick-engine/alert-policy` 更新容量上限、告警阈值、通知渠道和接收人，`POST /canvas/bi/capacity/quick-engine/tenant-pool-policy` 更新 pool key、并发上限、队列上限、排队超时和权重；真实查询执行路径复用该租户容量池策略做准入硬门禁，缓存命中不会占用 Quick 引擎执行配额，非缓存执行会占用并最终释放跨实例运行槽租约（配置租约服务时）或进程内租户运行槽，并在并发满但队列未满时按租户排队超时等待 release 唤醒/租约重试，等待成功写入 `QUEUED` 历史，等待超时或中断按 `BLOCKED` 拒绝。
- 嵌入 ticket：`POST /canvas/bi/embed-tickets` 签发短期 HMAC ticket；外部 ticket 必须携带允许域名并把规范化 host/port 写入签名 payload；配置持久化 mapper 后会把 token hash、resource key、nonce、过期时间、签名 scope、全局参数、最大访问次数和频率窗口元数据落入 `bi_embed_token`；`POST /canvas/bi/embed-tickets/verify` 匿名 HTTP 校验会按 `Origin`/`Referer` 执行来源白名单并通过数据库原子更新完成访问次数与频率窗口扣减，成功和 replay/限流拒绝会写入 `bi_audit_log`；`POST /canvas/bi/embed/resources/dashboard` 匿名读取 ticket 绑定的 dashboard resource 元数据，并用 ticket 租户读取持久化资源；`POST /canvas/bi/embed/resources/dashboard/runtime-state` 匿名读取 ticket 绑定的 dashboard runtime state，并用 ticket 租户、用户和资源 key 读取记住查询条件；`POST /canvas/bi/embed/query/execute` 匿名执行签名 ticket 绑定的 dashboard widget query，并用 ticket 上的租户和用户进入同一查询权限/行列权限链路；`POST /canvas/bi/embed-tickets/cleanup` 会按当前租户批量回收过期 token；未配置 mapper 时仍保留进程内 nonce fallback；前端设计器可按 widget 数量加 verify、资源元数据加载和运行态读取生成可完成一轮渲染的访问次数上限。
- 前端工作台：QuickBI-like 设计器布局、持久化/预置看板画布、门户查看态、订阅告警、资源位置移动、资源负责人转让、资源收藏、资源/组件评论、资源编辑锁、发布审批请求/审核、持久化门户资产列表、持久化数据集资产列表、持久化图表资产列表、数据集基础编辑器、图表基础编辑器、看板复制/归档/导入/导出、资源包 JSON 文件下载/上传、看板/图表/数据集/门户发布历史和版本恢复、数据/样式/交互面板、组件库、查询控件、数据集清单；数据集基础编辑器可批量写入字段文件夹并复制数据集草稿，图表基础编辑器可保存图表类型、数据集、维度、指标和 limit，可复制草稿并展示引用影响摘要；看板保存草稿以及看板/图表/数据集/门户版本恢复会在当前资源锁有效时携带锁 token；看板 widget 会按维度/指标自动执行查询，失败时降级为预置预览；工作台展示数据源健康、数据源连接器目录、租户已接入数据源、数据源连接测试结果、API 数据源真实行预览、CSV/Excel 文件选择上传与一键物化、schema 预览、schema 同步快照、从 schema 快照生成单表/多表复合 Join 数据集草稿、最近健康历史、健康 SLO 摘要、最近查询历史、查询详情抽屉、当前查询治理策略、最近治理策略审计、最近权限变更审计、当前查询缓存策略、当前查询缓存 provider/容量/TTL/命中率/写入驱逐统计、Quick 引擎容量水位/分类/Top 资源/用户排行/容量告警策略/租户容量池策略/并发队列观测、数据集加速策略和 widget 的实时/缓存状态，可更新默认 timeout/quota、默认缓存开关/TTL、当前仪表板/当前数据集缓存覆盖策略的模式/TTL/开关、Quick 引擎容量上限/预警阈值/严重阈值/通知渠道/接收人、Quick 引擎 pool key/并发上限/队列上限/排队超时/容量池权重、数据集加速模式/刷新策略，可按当前数据集或全量发起清缓存并手动抽取刷新，并可对有 SQL hash 的查询记录发起取消请求；设计器资源区支持字段/图表/控件搜索，组件卡片支持查看 SQL、复制、删除、布局微调、Ctrl/Shift/Meta 多选、左/中/右/顶/垂直居中/底对齐、桌面/平板/手机预览模式、标题区拖拽移动、吸附到邻近边线或中心线、右下角拖拽缩放、真实 CSS 栅格定位、碰撞避让和顶部撤销/重做，右侧数据面板展示查询结果、SQL hash、明细预览、后端编译出的参数化 SQL 和执行计划诊断；URL 中以 filter key、字段名、全局参数 key 或全局参数 alias 传入的查询控件参数会绑定到 widget 执行、SQL 编译、执行计划、自助取数查询和 embed ticket claims，支持日期区间、枚举多选、数值区间和单值控件；运行态参数按 URL 参数、当前用户记住查询条件、控件默认值的优先级合成，`LAST_7_DAYS`/`LAST_30_DAYS` 等动态默认值会解析为具体日期区间，控件作用范围会限制只过滤目标 widget；查询控件候选值通过 `buildDashboardControlOptionQuery(...)` 按父控件级联、字段映射和当前运行态参数执行查询，并在交互面板展示当前候选值；画布工具栏和右侧交互面板可直接编辑、持久化并解释当前运行态参数来源。
- 前端嵌入页：`/bi/embed/:resourceType/:resourceKey` 在无登录态下校验 ticket，通过 `POST /canvas/bi/embed/resources/dashboard` 加载签名 ticket 绑定的持久化仪表板资源，通过 `POST /canvas/bi/embed/resources/dashboard/runtime-state` 加载签名 ticket 绑定用户的记住查询条件，并按 ticket filters/parameters、记住查询条件、控件默认值的优先级还原运行态参数，再逐 widget 调用签名嵌入查询接口渲染 KPI、折线、柱图和表格结果；没有查询结果时显示空状态，不再用静态假数据填充。
- Canvas 入口：画布统计页和画布列表可进入 `/bi?dashboard=canvas-effect&canvasId={id}`。

仍未完成的生产级能力：

- 数据源中心闭环：连接器目录、连接器类型、容量分类和默认连接模式持久化、支持模式创建期约束、多步骤接入向导、API/APP HTTP/JSON 抽取接入配置持久化、API/APP 数据源真实预览、API 数据集 EXTRACT 物化执行路径、API 抽取刷新调度 UI/观测、CSV/Excel 文件上传、schema 同步、数据集草稿生成和 EXTRACT 刷新闭环、已配置数据源脱敏可见性、真实 JDBC 连接测试、表/字段 schema 预览、schema 同步持久化快照、单表数据集草稿生成、多行多表复合 Join 条件建模草稿生成、SQL 模板参数后端绑定、前端 SQL 模板与参数配置草稿入口、SQL 字段/指标精细编辑、SQL 样例预览和血缘影响分析、凭证轮换、数据集抽取加速基础运行态和数据源 `USE`/`EDIT` 权限 foundation 已完成；更深的驱动包/认证方式/数据源能力差异约束和更完整连接健康治理仍需补齐。
- 动态报表运行态：URL filter key/字段名/全局参数 key/全局参数 alias 到 widget 查询、SQL 编译、执行计划、自助取数查询和 embed ticket claims 的绑定已完成；当前用户看板运行态状态保存/读取、记住查询条件、控件默认值解析、筛选影响范围、查询控件候选值条件级联、画布工具栏运行态编辑、运行态来源状态展示和嵌入态运行态复用已接入。
- 缓存设置与治理：租户默认、数据集级和看板级查询结果缓存策略、直连/缓存/抽取加速模式、TTL、widget/控件候选值/自助取数查询的看板级运行态绑定、工作台对当前仪表板/当前数据集缓存覆盖策略的模式/TTL/开关编辑、按 SQL hash/当前数据集/全量清除缓存、策略审计、缓存命中/容量/写入驱逐观测、数据集抽取刷新运行记录、抽取定时调度、抽取保留清理、抽取容量摘要、Quick 引擎租户容量摘要/资源明细/用户排行/告警策略、查询行数配额阻断、租户容量池策略、并发/队列观测、工作台租户容量池策略编辑、查询执行前容量池准入硬门禁、进程内运行槽释放、可选分布式运行槽租约、同步队列等待、等待超时/中断清理、release 唤醒、持久化队列 job 表/入队/过期/claim、查询准入链路持久化入队、跨实例队列公平性/调度唤醒和队列任务恢复 foundation 已完成。
- 权限管理闭环：完整权限编辑器、导出审批，以及真实投递复用同一权限路径。
- 生产级外部嵌入：来源白名单、持久化 token hash、数据库原子访问次数与频率窗口扣减、访问审计、过期回收、访问次数/频率限制、embed 参数 claim 绑定、签名嵌入仪表板资源元数据读取、签名嵌入运行态读取、签名嵌入查询执行、查询上下文绑定 ticket 租户/用户、查询权限/行列权限链路复用和嵌入页真实数据渲染硬化已完成。
- 生产级自助取数硬化：流式分片写入、对象级分片保留、超大导出取消、下载限流、分片下载审计和跨 provider 根对象/分片对象恢复演练已完成。
- 电子表格和大屏资源 lifecycle、运行视图与工作台可视化编辑控件已完成，大屏组件库基础模板、选中组件方向移动、宽高步进缩放、多组件对齐、邻近组件参考线吸附和移动端单列/紧凑双列布局变体、电子表格单元格引用、常见聚合区间公式求值、单元格粗体/背景色/文字色编辑、移动端列数适配、交叉表透视生成、节假日感知和自然周期边界对齐的同比/环比异常模型已接入。

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
- 数据集编辑参考 `create-and-manage-datasets`、`detail-processing` 和 `manage-dataset`：字段大纲、数据预览、字段详情、维度/度量切换、展示格式、语义类型、默认聚合、文件夹分组、行列权限状态和影响分析必须在一个编辑上下文中可见。SQL 数据集编辑器必须允许细粒度维护字段 key、显示名、表达式、角色、类型、可见性、敏感级别、语义类型、默认聚合、格式、单位，以及指标 key、显示名、表达式、聚合、类型、允许维度、单位、格式、负责人和描述，并把这些配置写入发布资源而不是只作为前端显示状态。
- SQL 数据集上线前必须提供样例预览、源表血缘和影响分析：预览应复用保存时的只读 SQL lint、参数模板绑定、租户字段校验和查询编译器，返回编译 SQL、样例列/行、数据源 ID、FROM/JOIN 源表、参数顺序、引用字段/指标、发布审批门禁、缓存和下游报表影响；执行失败时只返回诊断，不伪造样例行。
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
- SQL 数据集预览必须使用同一查询编译和参数绑定路径，不能用前端拼接 SQL 或绕过租户条件。

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
