# 营销画布产品审核报告（2026-05-31）

> 基于项目代码扫描 + 6份已有审查文档 + 竞品对标，从产品/前端UX/后端API/竞品差距四个维度综合审核

---

## 一、产品功能缺项

### 1.1 三大系统性空白

| 空白层 | 现状 | 影响 |
|--------|------|------|
| **价值层** | 引擎只追踪"是否执行"，从不追踪"产生了什么价值" | 无法计算ROI、归因、转化贡献，营销投入无法衡量 |
| **跨画布协调层** | 每个画布是孤岛，无平台级疲劳度/优先级/去重/旅程追踪 | 同一用户可能被多个画布重复轰炸，无法做全局管控 |
| **高级营销抽象** | 只提供IF/DELAY/WAIT等底层积木，无组合模式 | 缺少滴灌营销、渠道瀑布升级、生命周期路由、VIP分层等高级模式 |

### 1.2 P0级缺项（合规红线 + 生产必须）

| # | 缺项 | 现状 | 影响 |
|---|------|------|------|
| 1 | 效果归因引擎 | 完全缺失 | 无法回答"哪条画布带来了转化" |
| 2 | 全局疲劳度控制 | 仅有节点级Handler，无跨画布/跨渠道UI管控 | 用户被重复轰炸 |
| 3 | 触达预览与试运行 | 后端有dry-run，前端无法预览受众规模和成本 | 发布前无法评估影响 |
| 4 | GDPR/PIPL删除权 | 缺失 | 合规红线 |
| 5 | /ops/端点公开暴露 | 无认证 | 安全漏洞 |
| 6 | MarketingPolicyService未接入发送链路 | 存在但从未被AbstractSendMessageHandler调用 | 合规检查形同虚设 |
| 7 | DLQ管理UI + 失败通知 | 后端有机制但前端不可见 | 运营人员故障感知延迟数小时 |
| 8 | 断路器可见性 | 熔断状态对运营不可见 | 无法感知系统降级 |
| 9 | 前端ErrorBoundary | 缺失，组件崩溃=白屏 | 用户无法恢复 |
| 10 | 画布执行状态增量持久化 | NodeGate锁/超时定时器全内存 | 崩溃丢状态，可能重发消息造成资金损失 |
| 11 | 8项非功能生产硬门 | K8s/Redis HA/优雅停机/备份/归档/CI/CD等 | 无法上生产 |

### 1.3 P1级缺项（运营必需 + 企业采购门槛）

| # | 缺项 | 现状 |
|---|------|------|
| 1 | 模板市场 | 后端SQL有20+模板，前端UI为零 |
| 2 | 画布版本管理 | 缺草稿/发布分离、diff对比可视化、回滚 |
| 3 | 用户旅程时间线 | 完全缺失 |
| 4 | 智能发送时机 | 被固定DelayHandler替代 |
| 5 | A/B实验深度 | 缺分层概念/结构化创建向导/指标管理/调试设备 |
| 6 | 渠道回执追踪 | 发完即忘，无送达/打开/点击/退回回调 |
| 7 | Webhook/事件订阅 | 零实现 |
| 8 | 画布导入导出 | 缺失 |
| 9 | SSO/OIDC | 缺失 |
| 10 | AI能力 | 192项中仅1项AI_LLM stub，竞品10+AI能力 |

---

## 二、前端UX缺项

### 2.1 严重问题

| # | 问题 | 证据 | 影响 |
|---|------|------|------|
| 1 | 编辑器/统计/用户页渲染在AppLayout外 | App.tsx:86-88 | 深链接用户无侧边栏，无法导航 |
| 2 | 无404路由 | App.tsx无catch-all | 无效URL显示空白页 |
| 3 | 6+页面required字段显示英文"Please input" | canvas-list:288, api-config:372-382, mq-config:195-201, event-config:211-214, tag-config:162-171, admin:133-139 | 用户体验不一致 |
| 4 | 403页面纯文本无样式无CTA | guards.tsx:46,54 | 权限拒绝页面不可用 |
| 5 | Axios仅拦截401，403/500无处理 | api.ts:36-41 | 大量错误无用户反馈 |
| 6 | 12+列表页无自定义空状态 | 几乎所有Table页 | 无引导无CTA |
| 7 | 状态映射不一致：编辑器只认3/5状态 | canvas-editor:1402-1406 vs canvas-list:27-33 | 归档/停止画布显示空标签 |

### 2.2 表单UX

| 问题 | 证据 |
|------|------|
| ParamSchemaEditor参数名零验证 | api-config:90 |
| Test run的userId无验证 | canvas-editor:1799 |
| audience-edit整页单滚动4个Card，无步骤向导 | audience-edit/index.tsx |
| 除canvas-editor外所有表单页无未保存警告 | audience-edit等 |
| 密码编辑字段无"已保存"视觉提示 | data-source-config:166 |
| 灰度发布失败显示"请稍后重试"无具体原因 | canvas-editor:1251 |
| 验证质量不一致：部分页面有中文提示，部分显示英文 | 全局 |

### 2.3 导航与寻路

| 问题 | 证据 |
|------|------|
| Logo点击跳/canvas而非/home | AppLayout:289 |
| 无面包屑导航 | 22个页面 |
| 无document.title/Helmet | 全局 |
| navigate(-1)回退不可控 | canvas-stats:199, canvas-users:61, cdp-user-detail:71 |
| 菜单key解析fallback为'canvas' | AppLayout:61-78 |

### 2.4 错误处理

| 问题 | 证据 |
|------|------|
| cdp-user-detail load()无catch | :31 |
| audience-list fetchList无catch | :55 |
| 大多数catch静默吞错误或显示"操作失败" | 全局 |
| 仅home和api-docs有error+retry模式 | home:149-158, api-docs |

### 2.5 数据展示

| 问题 | 证据 |
|------|------|
| 日期格式两种不兼容：内联replace丢时区 vs dayjs | canvas-list:183 vs cdpPresentation:53 |
| tenant-admin updatedAt渲染原始ISO字符串 | :83 |
| 数字格式不一致：仅audience-list用toLocaleString | :248 |
| 长文本截断不一致：部分有ellipsis+Tooltip，部分无 | canvas-list vs 其他 |

### 2.6 响应式/移动端

| 问题 | 证据 |
|------|------|
| 仅home页用antd响应式Grid | home/index.tsx |
| 侧边栏fixed 220px/64px，无移动端断点 | AppLayout:253-269 |
| canvas编辑器无触摸手势/视口缩放 | canvas-editor |
| api-config Modal width=1080px溢出 | api-config:368 |
| 无汉堡菜单/Drawer导航 | 全局 |

### 2.7 可访问性与设计系统

| 缺失项 | 现状 |
|--------|------|
| aria-label | 仅5处 |
| 主题/Token系统 | 颜色硬编码（SIDER_DARK, ACCENT等） |
| 组件库文档 | 无Storybook |
| 响应式设计 | 固定像素布局 |
| 深色模式 | 不支持 |
| 国际化 | 仅antd zh_CN，业务文本全硬编码中文 |

---

## 三、后端API缺项

### 3.1 CRITICAL：多租户隔离未强制执行

| 问题 | 证据 |
|------|------|
| 无TenantLineInnerInterceptor | MybatisPlusConfig:27-32仅有PaginationInnerInterceptor |
| V78加了tenant_id但DO文件无tenantId字段 | CanvasDO.java, CanvasExecutionDO.java |
| 仅4个Controller使用TenantContextResolver | AdminController, SystemOptionController, MetaController, TenantController |
| CanvasService查询无tenant_id过滤 | 任何租户用户可访问其他租户画布 |
| V78之后20+表无tenant_id | audience, cdp, tag, notification, async_task, data_source, customer_*, marketing_* |

### 3.2 业务逻辑Stub

| 文件 | 行 | 问题 |
|------|----|------|
| InAppNotifyHandler.java | :43 | `// TODO: 接入 MQTT 推送客户端`，站内通知只打日志不推送 |
| MetaService.java | :18 | "提供当前阶段 stub 选项数据" |
| CanvasExecutionService.java | :893-903 | FIXME: dedupTTL计算在运行期配置变更时不一致 |

### 3.3 表有Mapper无Controller（10+表）

| 表 | 迁移 | 问题 |
|----|------|------|
| canvas_audit_log | V3 | 审计日志表存在但无Mapper/Service/Controller |
| canvas_node_funnel_stats | V3 | 漏斗统计表存在但无API暴露 |
| marketing_frequency_counter | V60 | 频次计数器表无查看/重置API |
| customer_profile | V60 | 客户画像表有Mapper无Controller |
| customer_channel | V60 | 客户渠道表有Mapper无Controller |
| marketing_consent | V60 | 合规表有Mapper无Controller |
| marketing_suppression | V60 | 抑制表有Mapper无Controller |
| customer_tag | V64 | 客户标签表有Mapper无Controller |
| customer_points_ledger | V64 | 积分台账表有Mapper无Controller |
| customer_task_record | V64 | 任务记录表有Mapper无Controller |

### 3.4 API完整性缺失

| 类型 | 缺失 |
|------|------|
| 7个Controller缺DELETE | AsyncTask, CdpTagOperation, SystemOption, Notification, Admin, Tenant, Canvas |
| 5个Controller缺批量操作 | Canvas, EventDefinition, TagDefinition, Audience, ApiDefinition |
| 高级搜索/筛选 | 仅SystemOption和CdpUserController有关键词搜索 |
| 导出API | 执行数据/审计日志/用户活动均无导出 |
| 告警配置API | 无告警规则/通知偏好API |
| 定时任务管理API | CanvasVersionCleanupJob无手动触发/状态查看/配置API |
| 数据保留策略API | 无保留期/清理/归档生命周期API |

### 3.5 审计字段不一致

- 仅tenant表有updated_by，其余48+表只有created_by
- 无软删除（无deleted/del_flag列，无@TableLogic注解）

---

## 四、竞品功能差距（2025-2026对标）

对标平台：Braze / Iterable / Klaviyo / HubSpot / 神策 / CleverTap

### 4.1 Tier1: Table-stakes缺失（所有竞品都有，缺失=不可上市）

| # | 功能 | 竞品覆盖 | 本项目现状 | 影响 |
|---|------|----------|-----------|------|
| 1 | 动态内容/模板引擎(Liquid) | Braze/Iterable/Klaviyo | **缺失** — 无模板语言，消息内容静态 | 个性化不可能 |
| 2 | 智能发送时间优化(STO) | Braze/Iterable/Klaviyo/CleverTap | **缺失** — 无ML逐用户最优时间 | 参与率低 |
| 3 | Content Cards/持久化应用内信箱 | Braze/CleverTap/Iterable | **缺失** — 仅有瞬时通知 | 用户错过消息 |
| 4 | A/B实验统计报告 | 全部6家 | **部分** — 有AB_SPLIT但无显著性/置信区间 | 实验是摆设 |
| 5 | Webhook出站 | Braze/Iterable/Klaviyo/HubSpot | **缺失** | 无法集成外部系统 |
| 6 | 运营日历视图 | 全部6家 | **缺失** | 无法看到活动排期 |
| 7 | 多触点归因 | HubSpot/Klaviyo/Braze/神策 | **缺失** | 无法衡量ROI |
| 8 | 合规管理UI | 全部6家 | **部分** — 后端有DO无UI | 合规风险 |
| 9 | 产品目录 | Iterable/Klaviyo/Braze | **缺失** | 无法做动态推荐 |
| 10 | 实时分群评估 | 神策/CleverTap/Braze | **部分** — 批量+Tagger，无实时成员评估 | 触发延迟 |

### 4.2 Tier2: Differentiator缺失（Top3-4竞品有，缺失=落后）

| # | 功能 | 竞品覆盖 | 本项目现状 |
|---|------|----------|-----------|
| 1 | AI内容生成(LLM) | Braze/Iterable/HubSpot/Klaviyo | 缺失，无LLM集成 |
| 2 | 预测分析(流失/CLV/倾向) | Klaviyo/CleverTap/Braze/神策 | Stub：返回硬编码fallback |
| 3 | 生命周期可视化 | CleverTap/Braze/Iterable | 缺失 |
| 4 | 旅程分析(实际路径) | 神策/Braze/Iterable | 缺失，仅有设计路径漏斗 |
| 5 | RFM分析 | CleverTap/Klaviyo | 缺失 |
| 6 | A/B/n多变量测试 | Iterable/Braze/Klaviyo | 仅AB_SPLIT(2变体) |
| 7 | 身份解析/跨设备 | 神策/Braze/Klaviyo | 有IdentityTypeDO但无合并/解析逻辑 |
| 8 | 行业基准分析 | Klaviyo/HubSpot | 缺失 |
| 9 | 评价/UGC集成 | Klaviyo | 缺失 |
| 10 | WhatsApp渠道 | CleverTap/Braze/Iterable | 缺失 |

### 4.3 Tier3: Emerging缺失（1-2家竞品有，定义2026+方向）

| # | 功能 | 竞品覆盖 |
|---|------|----------|
| 1 | 自然语言旅程创建 | Braze(Sage AI) |
| 2 | AI旅程自动优化 | Iterable/Braze |
| 3 | 多臂老虎机测试 | Braze/Iterable |
| 4 | 隐私优先数据管理 | Iterable/神策 |
| 5 | 应用内调查/反馈 | CleverTap/HubSpot |
| 6 | Feature Flag/产品体验 | CleverTap |
| 7 | 对话AI/Chatbot集成 | HubSpot/Braze |
| 8 | 收入归因仪表盘 | Klaviyo/HubSpot |
| 9 | 跨空间营销编排 | Braze/神策 |
| 10 | AI异常检测 | 神策/CleverTap |

---

## 五、根因分析

| 根因 | 表现 |
|------|------|
| **平台定位偏差** | 作为"流程执行引擎"而非"营销运营平台"构建，缺少价值层和运营管控层 |
| **无PRD驱动** | 产品定义散落在12+文件中，无Epic/Story分解，PM Checklist完成度50% |
| **后端先行前端滞后** | 多项后端能力（模板API、diff API、DLQ机制、10+表有Mapper无Controller）无前端UI暴露 |
| **多租户架构半成品** | DB有tenant_id但Java层未强制执行，V78后20+表无tenant_id |
| **技术债阻碍迭代** | 2084行编辑器单体、1414行配置面板，任何UI改动都高风险 |
| **AI能力空白** | 192项中仅1项AI stub，竞品已将AI作为核心差异化 |

---

## 六、优先级建议

### 立即修复（1-2周）
- ErrorBoundary（防白屏）
- beforeunload未保存警告
- /ops/端点认证
- 多租户数据隔离强制执行（TenantLineInnerInterceptor + DO补字段）
- 404路由 + 403页面样式
- 编辑器页面纳入AppLayout（修复导航断裂）

### 短期补齐（1-2月）
- 全局疲劳度UI
- DLQ管理UI + 断路器可见性
- 列表页排序/筛选/批量操作
- 模板浏览页
- 表单验证中文化 + 未保存拦截
- 空状态组件 + 首次引导
- InApp通知接入真实推送（MQTT）

### 中期建设（2-4月）
- 效果归因引擎
- 画布版本管理（含diff可视化）
- 用户旅程时间线
- A/B实验重构（统计报告 + 创建向导）
- 设计系统（Token + Storybook）
- 动态内容/模板引擎（Liquid）
- 审计日志API + UI

### 长期演进（4-8月）
- AI能力建设（内容生成 → 预测分析 → NL旅程创建）
- 高级营销抽象（滴灌/瀑布/生命周期）
- 运营日历 + Content Cards + 产品目录
- 响应式 + 深色模式 + 国际化
- WhatsApp渠道 + Webhook出站
- 收入归因仪表盘

---

*审核人：John (PM Agent) | 审核日期：2026-05-31 | 数据来源：项目代码扫描 + 6份已有审查文档 + 6大竞品对标*
