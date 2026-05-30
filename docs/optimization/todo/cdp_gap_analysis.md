# CDP能力缺项全景（2026-05-30）

基于项目CDP模块现状（V74-V77迁移、6张核心表、12个API端点、4个Handler节点、7个前端页面）与全功能CDP标杆（Segment/mParticle/Tealium/神策CDP）对比，梳理出20项能力缺项。

---

## CDP能力总览

| CDP维度 | 我方对应 | 覆盖度 | 缺项 |
|---------|---------|--------|------|
| **用户画像** | cdp_user_profile + cdp-user-detail页面 | 40% | 缺计算画像、画像版本、画像分组 |
| **标签体系** | cdp_user_tag + cdp_tag_operation + tag-config/tag-import页面 | 50% | 缺计算标签引擎、标签依赖图谱、标签血缘 |
| **人群圈选** | audience_definition + audience-edit页面 + RoaringBitmap | 55% | 缺实时人群、人群排重、人群快照 |
| **数据采集** | API推送 + Excel导入 + API拉取 | 25% | 缺SDK、实时事件管线、事件自动发现 |
| **数据导出与激活** | 无 | 0% | 缺Webhook、广告平台对接、邮件工具同步 |
| **数据治理** | phone/email脱敏 + 标签TTL | 15% | 缺数据保留策略、数据质量、数据血缘、合规增强 |

---

## 一、用户画像增强

### 1. 计算画像属性

**现状**：`cdp_user_profile.properties_json` 只存静态属性（如注册来源、VIP等级），没有基于行为数据计算的动态画像属性（如"最近30天消费金额"、"首次购买距今天数"）。

**子能力拆解**：
- **表达式定义**：定义画像属性的计算表达式（如 `SUM(order.amount) WHERE order.created_at > NOW() - INTERVAL 30 DAY`）
- **定时计算**：ElasticJob定时执行表达式，将结果写回 `cdp_user_profile.properties_json`
- **实时计算**：行为事件到达时增量更新画像属性（如"下单"事件→累加消费金额）
- **画像属性版本**：每次计算生成快照，支持查看历史值变化
- **在画布中引用**：画布节点可读取计算画像属性作为条件/变量

**业界标杆**：Segment Computed Traits、Tealium EventStream Attributes、神策用户属性计算。

**技术要点**：新增 `computed_profile_attribute` 表（attr_id, name, expression, refresh_mode, refresh_cron, last_computed_at）。定时任务解析表达式，从 `event_log` 聚合计算结果，写回 `properties_json`。实时模式需消费MQ事件增量更新。

---

### 2. 画像属性分组

**现状**：`properties_json` 是一个大JSON，所有属性平铺，无分组概念。

**子能力拆解**：
- **属性分组**：将画像属性按业务域分组（基础信息、消费行为、出行偏好、会员等级）
- **分组展示**：用户详情页按分组折叠/展开
- **分组权限**：不同角色可看到不同分组的属性

**技术要点**：`properties_json` 内部结构改为 `{"basic": {...}, "consumption": {...}}`，或新增 `profile_attribute_meta` 表定义属性分组。

---

### 3. 画像属性版本与历史

**现状**：`cdp_user_profile` 只有 `last_seen_at`，无属性变更记录。运营无法回答"这个用户VIP等级什么时候从银卡变成金卡的"。

**子能力拆解**：
- **属性变更日志**：记录每次画像属性变更（old_value → new_value）
- **属性时间线**：用户详情页展示属性变更时间线
- **属性回溯**：查询用户在某个时间点的属性值（point-in-time query）

**业界标杆**：Segment Profile API 支持 point-in-time query。

**技术要点**：新增 `profile_change_log` 表（user_id, attr_name, old_value, new_value, changed_at, source）。复用标签历史（`cdp_user_tag_history`）的设计模式。

---

## 二、标签体系增强

### 4. 计算标签引擎

**现状**：标签只能通过API推送、Excel导入、API拉取写入。没有基于规则/SQL/表达式自动计算的标签。运营想建一个"高流失风险用户"标签，需要写代码或找数据团队。

**子能力拆解**：
- **规则标签**：基于用户属性/行为条件自动打标（如"最近30天无活跃"→"流失风险"标签）
- **SQL标签**：基于SQL查询结果批量打标（如 `SELECT user_id FROM orders GROUP BY user_id HAVING SUM(amount) > 10000`→"高消费"标签）
- **表达式标签**：基于SpEL/Groovy表达式计算标签值（如 `days_since_last_active > 30 ? "high" : "low"`）
- **标签依赖**：标签B依赖标签A的计算结果，需按依赖顺序执行
- **标签调度**：支持定时/事件触发计算

**业界标杆**：Tealium Attributes（计算属性）、神策标签管理（规则标签+SQL标签）、Convertlab标签体系。

**技术要点**：新增 `computed_tag_definition` 表（tag_code, compute_type[RULE/SQL/EXPR], expression, schedule_cron, depends_on JSON, status）。扩展 `CdpTagService` 支持计算标签写入。ElasticJob定时执行。

---

### 5. 标签依赖图谱

**现状**：标签之间无依赖关系，不知道哪个标签依赖于哪个标签的计算结果。

**子能力拆解**：
- **依赖定义**：定义标签间的计算依赖关系
- **DAG可视化**：前端展示标签依赖关系图
- **循环检测**：保存时检测循环依赖并拒绝
- **依赖调度**：按拓扑排序依次计算依赖链上的标签

**技术要点**：标签定义表中 `depends_on` 字段存储依赖的tag_code列表。计算时构建DAG，拓扑排序后依次执行。循环检测用DFS。

---

### 6. 标签血缘与影响分析

**现状**：改一个标签不知道会影响哪些人群和画布。

**子能力拆解**：
- **标签→人群**：标签被哪些人群规则引用
- **标签→画布**：标签被哪些画布节点（TAGGER/SELECTOR/IF_CONDITION）引用
- **影响分析**：删除/修改标签前，展示影响范围
- **血缘图**：标签→人群→画布的完整血缘链路

**技术要点**：构建引用关系索引（标签被引用的次数和位置）。修改/删除标签前查询引用关系。前端用 @xyflow/react（项目已有）展示血缘图。

---

## 三、人群圈选增强

### 7. 实时人群

**现状**：人群只能离线批计算（`AudienceBatchComputeService`），无实时人群。用户行为变化后，人群成员不会实时更新。

**子能力拆解**：
- **实时规则引擎**：用户行为事件到达时，实时评估是否匹配人群规则
- **实时人群成员变更**：事件触发的入组/出组实时更新bitmap
- **实时人群事件**：人群成员变更时发MQ事件，画布可订阅
- **实时人群规模**：实时显示当前人群规模

**业界标杆**：Tealium AudienceStream（实时人群）、mParticle Real-time Audiences。

**技术要点**：消费行为事件MQ，用Aviator表达式引擎实时评估规则，命中则更新Redis bitmap + 发人群变更事件。非命中则检查是否需要出组。

---

### 8. 人群排重与合并

**现状**：不同人群之间可能有大量重叠用户，运营无法知道。

**子能力拆解**：
- **人群重叠分析**：计算两个人群的交集大小和重叠率
- **韦恩图**：可视化展示多个人群的重叠关系
- **人群合并**：合并多个人群创建新人群（并集）
- **人群排除**：A人群排除B人群创建新人群（差集）

**业界标杆**：Facebook Custom Audience（排重提示）、Tealium Audience Composition。

**技术要点**：利用RoaringBitmap的AND/OR/ANDNOT运算，计算交集/并集/差集的基数。前端用ECharts Venn图。

---

### 9. 人群快照与历史

**现状**：人群只有当前状态，没有历史快照。运营无法回答"这个人群上周有多少人"。

**子能力拆解**：
- **定时快照**：每天自动保存人群规模快照
- **历史趋势**：展示人群规模随时间的变化曲线
- **快照对比**：对比不同时间点的人群成员差异
- **快照回溯**：使用历史快照的人群成员执行画布

**技术要点**：新增 `audience_snapshot` 表（audience_id, snapshot_date, estimated_size, bitmap_key）。定时任务（ElasticJob）每天保存快照。

---

## 四、数据采集增强

### 10. 实时事件采集管线

**现状**：无实时事件采集。行为数据只能通过 `TrackEventHandler` 在画布执行中手动上报，不是通用的数据采集层。

**子能力拆解**：
- **事件上报API**：标准化事件上报协议 `POST /cdp/events/track`（batch模式）
- **SDK数据格式**：定义通用事件模型（eventCode, userId, properties, timestamp, sessionId, platform, deviceId）
- **事件验证**：上报时验证事件代码是否已注册（`event_definition`表）
- **事件去重**：基于idempotency_key去重
- **事件分发**：写入 `event_log` + 发MQ供下游消费

**业界标杆**：Segment Tracking API、mParticle Event API、神策SDK数据格式。

**技术要点**：新增 `CdpEventIngestionController`，接收批量事件上报。校验→去重→写`event_log`→发MQ。扩展现有 `event_log` 表字段（增加session_id, platform, device_id）。

---

### 11. 事件属性自动发现

**现状**：`event_definition` 表存在但属性需手动定义。当新事件或新属性出现时，需运营手动添加。

**子能力拆解**：
- **属性自动发现**：事件上报时，自动检测新属性并注册
- **属性类型推断**：根据值自动推断类型（string/number/boolean/date）
- **发现审核**：新发现的属性标记为"待审核"，需人工确认
- **属性字典**：所有事件属性的统一字典

**业界标杆**：Segment Schema API（自动发现事件属性）、神策事件管理（属性自动识别）。

**技术要点**：事件上报时，对比 `event_definition` 中的已知属性，新属性自动写入 `event_attr_definition` 表（status=PENDING_REVIEW）。

---

### 12. Web/Mobile SDK

**现状**：无客户端SDK。无法自动采集页面浏览、点击、停留时长等行为数据。

**子能力拆解**：
- **Web SDK**：JavaScript SDK，自动采集 page_view, click, stay_duration 等事件
- **Mobile SDK**：iOS/Android SDK，自动采集 app_open, screen_view, tap 等事件
- **SDK配置**：埋点方案配置（全埋点 vs 可视化埋点 vs 代码埋点）
- **SDK版本管理**：多版本SDK兼容

**业界标杆**：Segment Analytics.js、神策JS SDK/Android SDK/iOS SDK。

**技术要点**：这是独立项目，不属于画布后端。建议作为独立npm库/CocoaPod发布。短期可先对接第三方SDK（神策/Segment）而非自建。

---

## 五、数据导出与激活

### 13. Webhook/回调机制

**现状**：无。标签变更、人群成员变更等事件无法通知外部系统。

**子能力拆解**：
- **Webhook注册**：注册回调URL和订阅事件类型（TAG_CHANGED/AUDIENCE_CHANGED/PROFILE_CHANGED）
- **事件推送**：事件触发时POST到回调URL，带签名验证
- **重试机制**：失败时指数退避重试（最多3次）
- **事件日志**：记录推送历史和状态

**业界标杆**：Segment Source/Destinations Webhooks、Tealium EventStream Webhooks。

**技术要点**：新增 `webhook_subscription` 表（id, url, event_types JSON, secret, status）。新增 `WebhookDispatcherService`。签名用HMAC-SHA256。

---

### 14. 广告平台对接

**现状**：无。无法将人群同步到Facebook Custom Audience、Google Ads Customer Match等广告平台。

**子能力拆解**：
- **Facebook Custom Audience**：同步人群到Facebook广告账户
- **Google Ads Customer Match**：同步人群到Google广告账户
- **字节巨量引擎**：同步人群到巨量引擎广告平台
- **腾讯广告**：同步人群到腾讯广告平台
- **同步状态追踪**：跟踪人群同步状态和同步数量

**业界标杆**：Segment Destinations（300+集成）、Tealium Connectors（1000+集成）。

**技术要点**：新增 `audience_destination` 表（audience_id, platform, account_id, audience_id_on_platform, sync_status）。每个平台一个Connector实现。

---

### 15. 邮件/短信工具同步

**现状**：`SendSmsHandler`/`SendEmailHandler` 通过 `ReachDeliveryService` 调用外部触达平台，但不是标准化的CDP出向对接。

**子能力拆解**：
- **Braze/SFMC 对接**：将人群/标签同步到邮件营销工具
- **标准字段映射**：CDP属性→目标平台字段的映射配置
- **同步频率**：实时/定时同步
- **同步日志**：记录同步操作和结果

**技术要点**：复用 Webhook 基础设施 + 各平台API适配层。新增 `destination_connector` 抽象接口。

---

## 六、数据治理

### 16. 数据保留策略

**现状**：标签有TTL（`expires_at`），但 `event_log` 和 `execution_trace` 无保留策略，会无限增长。

**子能力拆解**：
- **保留策略配置**：为每类数据配置保留天数（event_log 90天、execution_trace 180天）
- **自动清理**：ElasticJob定时删除过期数据
- **归档**：过期前归档到冷存储（OSS/S3）
- **策略告警**：数据量接近阈值时告警

**技术要点**：新增 `data_retention_policy` 表（table_name, retention_days, archive_enabled, last_cleaned_at）。清理任务用分页DELETE避免锁表。

---

### 17. 数据质量监控

**现状**：无。不知道标签空值率、事件属性缺失率、人群规则命中率等。

**子能力拆解**：
- **标签质量**：标签覆盖率、空值率、枚举值分布
- **事件质量**：必填属性缺失率、事件日活趋势、异常值检测
- **人群质量**：规则命中率、人群规模波动检测
- **质量仪表盘**：综合数据质量评分和趋势

**业界标杆**：Segment Schema Dashboard、Monte Carlo（数据可观测性）。

**技术要点**：新增 `data_quality_metric` 表（metric_type, target_id, metric_name, metric_value, computed_at）。ElasticJob定时计算。

---

### 18. 数据血缘追踪

**现状**：无。不知道一个标签的数据从哪来、经过哪些变换、输出到哪去。

**子能力拆解**：
- **标签血缘**：标签来源（API推送/计算标签/导入）→标签值→引用方（人群/画布）
- **人群血缘**：人群规则引用的标签/属性→人群→输出方（画布/广告平台）
- **事件血缘**：事件来源（SDK/API/MQ）→事件→消费方（计算标签/画布）
- **血缘可视化**：端到端数据流图

**业界标杆**：Apache Atlas（Hadoop血缘）、DataHub（LinkedIn开源血缘平台）。

**技术要点**：构建引用关系图谱。标签创建/修改时记录来源。人群保存时解析规则中的标签引用。画布保存时解析节点中的标签/人群引用。

---

### 19. PIPL合规增强

**现状**：phone/email有脱敏，但无完整的个人信息保护合规能力。

**子能力拆解**：
- **数据分类**：标记哪些字段是个人敏感信息（PII）
- **脱敏策略**：按角色配置不同的脱敏规则（运营看后4位、管理员看全部）
- **授权管理**：与 #25 用户授权管理 联动，未授权用户的数据不参与计算
- **数据删除**：与 #25 数据删除 联动，级联删除CDP数据
- **审计日志**：记录所有PII数据的访问和操作

**技术要点**：新增 `pii_field_config` 表（table_name, field_name, pii_level, mask_rule）。查询层AOP拦截，根据角色自动脱敏。

---

### 20. API限流与配额

**现状**：标签写入API无限流，大量写入可能压垮数据库。

**子能力拆解**：
- **每源限流**：每个API来源配置QPS上限
- **配额管理**：按租户配置每日标签写入配额
- **限流策略**：超限返回429或排队延迟
- **配额监控**：实时显示配额使用情况

**技术要点**：基于Redis令牌桶限流。新增 `api_quota_config` 表（tenant_id, source, daily_limit, used_today, reset_date）。

---

## 缺项优先级总结

| 优先级 | 编号 | 能力 | 核心依赖 | 与现有代码对接点 |
|--------|------|------|---------|----------------|
| **P0（基础设施）** | #10 | 实时事件采集管线 | event_log表扩展 | 新增CdpEventIngestionController |
| **P0** | #13 | Webhook/回调机制 | 无 | 新增webhook_subscription表+Dispatcher |
| **P0** | #16 | 数据保留策略 | ElasticJob | 新增retention_policy表+清理Job |
| **P0** | #19 | PIPL合规增强 | 与#25授权联动 | AOP拦截+脱敏配置表 |
| **P1（核心能力）** | #4 | 计算标签引擎 | event_log+CDP标签 | 扩展CdpTagService |
| **P1** | #1 | 计算画像属性 | event_log+CDP画像 | 扩展properties_json写入 |
| **P1** | #7 | 实时人群 | MQ+Aviator | 消费行为事件+实时规则评估 |
| **P1** | #8 | 人群排重与合并 | RoaringBitmap | 并集/交集/差集运算 |
| **P1** | #11 | 事件属性自动发现 | event_definition表 | 上报时自动注册新属性 |
| **P1** | #20 | API限流与配额 | Redis令牌桶 | 标签写入接口前置拦截 |
| **P2（深度能力）** | #2 | 画像属性分组 | properties_json结构化 | 前端分组展示 |
| **P2** | #3 | 画像属性版本与历史 | 新增变更日志表 | 复用标签历史设计模式 |
| **P2** | #5 | 标签依赖图谱 | computed_tag_definition.depends_on | DAG可视化+拓扑排序 |
| **P2** | #6 | 标签血缘与影响分析 | 引用关系索引 | @xyflow/react血缘图 |
| **P2** | #9 | 人群快照与历史 | 新增snapshot表 | ElasticJob定时快照 |
| **P2** | #14 | 广告平台对接 | 各平台SDK | Connector接口抽象 |
| **P2** | #15 | 邮件/短信工具同步 | 目标平台API | Connector接口抽象 |
| **P3（长期规划）** | #12 | Web/Mobile SDK | 独立项目 | 对接第三方SDK |
| **P3** | #17 | 数据质量监控 | 定时计算+仪表盘 | 质量评分系统 |
| **P3** | #18 | 数据血缘追踪 | 引用关系图谱 | 端到端血缘可视化 |

---

## 实施路线建议

### 第一阶段（1-2周）：数据基建

1. #10 实时事件采集管线 — 标准化事件上报协议
2. #13 Webhook/回调机制 — CDP数据可被外部消费
3. #16 数据保留策略 — 防止数据无限增长
4. #19 PIPL合规增强 — 合规底线

### 第二阶段（2-4周）：核心CDP能力

5. #4 计算标签引擎 — 自动化标签生产
6. #1 计算画像属性 — 动态用户画像
7. #7 实时人群 — 实时响应用户行为
8. #8 人群排重与合并 — 精细化人群管理
9. #11 事件属性自动发现 — 降低维护成本
10. #20 API限流与配额 — 系统保护

### 第三阶段（1-2月）：深度与集成

11. #2~#6 画像/标签增强
12. #9 人群快照
13. #14~#15 数据导出与激活
14. #17~#18 数据治理深度

### 第四阶段（长期）：生态建设

15. #12 SDK
16. 更多平台Connector