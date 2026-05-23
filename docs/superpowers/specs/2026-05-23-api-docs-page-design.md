# API 说明页面设计

## 背景

当前项目后端已经暴露了完整 REST API，README 只说明可访问 Swagger UI。前端侧边栏中已有「系统设置」和「API 接口配置」，但没有面向外部调用方或平台集成方的内置 API 文档页。用户希望新增一个 API 说明入口，把开放 API 列出来，并给出可直接参考的调用示例。

本设计覆盖“外部调用 API”和“内部管理 API”。页面默认只展示外部系统更可能调用的接口，内部管理接口通过页面开关展示。

## 参考基准

视觉和交互参考成熟开发者文档站，而不是普通后台 CRUD 页：

- Stripe API Reference：资源分类清楚，接口条目与代码示例并排展示。
- OpenAI API Reference：概念说明与端点说明分层，示例可快速复制。
- Twilio Docs：产品域分组明确，适合多能力平台。
- Shopify API Docs：左侧资源导航、主体说明、示例区域的三栏阅读模型。

落地时不复制任何品牌视觉。页面仍使用当前项目的 Ant Design、侧边栏和后台色彩体系，但信息架构、留白、代码块、标签和示例阅读体验按开发者文档产品来做。

## 目标

- 新增「API 说明」页面，展示项目可调用 API。
- 页面默认展示外部调用接口。
- 通过「显示内部管理 API」开关展示后台页面能力对应的内部接口。
- 每个接口提供方法、路径、说明、认证要求、请求参数、请求示例和响应示例。
- 按业务能力分类，方便调用方快速定位。
- 保持静态文档配置，避免依赖 Swagger 运行态。

## 非目标

- 不实现 Swagger/OpenAPI 自动解析。
- 不实现在线调试请求发送。
- 不实现接口权限动态识别。
- 不改后端 Controller。
- 不改现有 Swagger 配置。

## 信息架构

侧边栏新增一个菜单分组：

- 开发者文档
  - API 说明

不把 API 说明放入现有「API 接口配置」页。原因是「API 接口配置」是业务节点调用外部 API 的配置管理页，而「API 说明」是本系统对外提供能力的开发者文档，两者语义不同。

页面路径建议为：

- `/api-docs`

权限沿用后台管理类页面：

- 初期放在 `RequireAdmin` 下。
- 后续如果要给普通操作员查看，可以单独调整路由权限。

## API 分类

页面按以下分类展示：

- 认证：登录、登出、当前用户。
- 外部触发：事件上报、行为触发、画布直调、dry-run。
- 审批回调：审批通过、审批拒绝。
- 画布管理：创建、查询、更新、发布、下线、归档、克隆、版本、灰度、回滚。
- 配置管理：API 定义、事件定义、MQ 定义、标签、人群、AB 实验。
- 元数据：节点类型、上下文字段、业务线、可选项。
- 运行观测：执行记录、轨迹、统计、漏斗、趋势、DLQ、执行请求重放。
- 运维与模板：缓存失效、模板列表、另存模板、基于模板创建、待审批发布请求。
- 用户管理：用户列表、创建、更新、禁用。

外部调用接口默认展示：

- `POST /auth/login`
- `POST /auth/logout`
- `GET /auth/me`
- `POST /canvas/events/report`
- `POST /canvas/trigger/behavior`
- `POST /canvas/execute/direct/{canvasId}`
- `POST /canvas/execute/dry-run/{canvasId}`
- `POST /canvas/execution/{executionId}/approve`
- `POST /canvas/execution/{executionId}/reject`
- 常用只读元数据接口，例如节点类型、业务线、API 定义选项、事件定义选项。

内部管理接口标记为 `internal: true`，默认隐藏。打开「显示内部管理 API」后展示，并用「内部」标签标记。

## 页面布局

采用开发者文档三段式布局：

- 顶部工具条：标题、说明、搜索框、内部接口开关。
- 左侧文档导航：分类列表和接口数量。
- 中间主体：接口说明卡片列表。
- 右侧辅助区：当前选中接口的请求/响应示例，或页面内目录。

在当前后台内容区宽度下，具体布局为：

- 宽屏：左侧分类导航 220px，中间内容自适应，右侧示例面板 360px。
- 中屏：左侧分类导航和内容双栏，示例跟随接口卡片折叠在下方。
- 小屏：分类导航变成顶部 Tabs，示例在接口详情内展示。

页面不使用营销式 hero，不使用大面积渐变或装饰背景。整体应安静、专业、便于扫描。

## 组件设计

数据模型：

```ts
interface ApiDocEndpoint {
  id: string
  title: string
  method: 'GET' | 'POST' | 'PUT' | 'DELETE'
  path: string
  category: string
  summary: string
  auth: 'none' | 'bearer'
  internal?: boolean
  params?: Array<{ name: string; in: 'path' | 'query' | 'body'; required?: boolean; desc: string }>
  requestExample?: unknown
  responseExample?: unknown
}
```

主要组件：

- `ApiDocsPage`：页面容器，负责搜索、内部接口开关和当前分类状态。
- `apiDocs.ts`：静态接口文档数据。
- `ApiCategoryNav`：分类导航，展示分类名称和接口数量。
- `ApiEndpointCard`：单个接口说明，展示方法、路径、说明、认证、参数。
- `CodeExamplePanel`：请求和响应 JSON 示例面板。

## 视觉规范

接口方法使用颜色标签，但克制使用：

- `GET`：绿色。
- `POST`：蓝色。
- `PUT`：橙色。
- `DELETE`：红色。

接口路径使用等宽字体，支持长路径换行。卡片圆角不超过 8px。代码块使用深色背景或浅色边框均可，但必须保证可读、可复制、移动端不溢出。

页面重点是阅读效率：

- 搜索框支持按路径、标题、说明过滤。
- 外部/内部标签必须明显。
- 认证要求用 `Bearer Token` 或 `无需认证` 标签展示。
- 示例 JSON 格式化为 2 空格缩进。
- 每个接口卡片默认展示核心信息，参数和示例可折叠展开。

## 数据来源

第一版从项目 Controller 手工整理静态文档：

- `backend/canvas-engine/src/main/java/org/chovy/canvas/auth/controller`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/controller`

静态方式的理由：

- 可以补充中文业务说明和真实使用场景。
- 不依赖后端服务运行或 Swagger 配置。
- 可以明确标记外部/内部接口。
- 当前接口规模可控，手工整理成本低。

## 示例策略

示例使用业务可读值，不使用空对象作为主要示例。

事件上报示例：

```json
{
  "eventCode": "ORDER_PAID",
  "userId": "user_10001",
  "attributes": {
    "orderId": "ord_202605230001",
    "amount": 199.00
  }
}
```

直调执行示例：

```json
{
  "userId": "user_10001",
  "idempotencyKey": "journey-direct-202605230001",
  "inputParams": {
    "couponId": "coupon_001"
  }
}
```

统一响应示例：

```json
{
  "code": 0,
  "message": "success",
  "data": {}
}
```

实际字段名以项目 `R<T>` 响应模型和 Controller 请求 DTO 为准。

## 测试

前端：

- API 文档数据存在外部接口和内部接口。
- 默认不展示 `internal: true` 的接口。
- 打开开关后展示内部接口。
- 搜索能匹配路径和标题。
- `npm run build`

如果新增测试文件，优先测试过滤逻辑，不为纯展示样式写脆弱快照。

## 风险与处理

- 手工文档可能与 Controller 演进不同步。处理方式：把接口数据集中在 `apiDocs.ts`，后续改接口时同文件更新。
- 全量内部接口太多会压低阅读效率。处理方式：默认隐藏内部接口，并按分类计数。
- API 配置页和 API 说明页命名容易混淆。处理方式：菜单分组使用「开发者文档」，页面标题使用「API 说明」，避免放入「API 接口配置」页。
- 示例可能和真实 DTO 有偏差。处理方式：实现时对照 Controller 和 DTO 文件，不凭记忆写字段。
