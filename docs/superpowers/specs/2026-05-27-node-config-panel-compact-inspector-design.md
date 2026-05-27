# 节点配置面板紧凑 Inspector 设计

## 背景

当前右侧节点配置面板已经从默认 Ant Design 表单升级为 inspector / iOS 风格，但实际使用中出现了新的视觉问题：

- 控件高度约 `60px`，在右侧窄栏中显得过大。
- 卡片圆角约 `22px - 26px`，和后台画布工具的密集配置场景不够协调。
- 复杂节点的条件、分支、参数映射被大控件撑高，阅读效率下降。
- 视觉强化主要服务少数节点，缺少一套能覆盖所有节点类型的通用组织方式。

本次选择已确认的 **方案 B：全节点兼容分组版**。目标是保留右侧配置面板的清晰识别能力，同时把控件和卡片收敛为更克制、更适合后台配置工具的视觉密度。

## 范围

### 本次只修改

- `frontend/src/components/config-panel/index.tsx`
- `frontend/src/components/config-panel/InspectorCards.tsx`
- `frontend/src/components/config-panel/controlChrome.ts`
- `frontend/src/components/config-panel/presentation.ts`
- 上述模块对应的前端单元测试

### 明确不修改

- 左侧节点库 `frontend/src/components/node-panel/*`
- 中间 React Flow 画布区域 `frontend/src/pages/canvas-editor/index.tsx`
- 画布节点卡片 `frontend/src/components/canvas/CanvasNode.tsx`
- 三栏整体布局、右侧栏宽度、工具栏、MiniMap、连线交互
- 后端接口、节点 schema 来源、保存逻辑和图结构格式

## 目标

- 降低右侧配置面板“大框突兀感”。
- 让简单节点保持清爽表单，复杂节点自动形成更易扫读的配置分组。
- 兼容所有节点类型，不为单一节点类型设计不可复用的特殊结构。
- 保持现有 schema 驱动、表单联动、保存回写和只读态行为不变。
- 提升条件规则、参数映射、出口路由等高频复杂配置的密度和可读性。

## 非目标

- 不做左侧节点库视觉改版。
- 不做中间画布、节点卡片、连线或占位点改版。
- 不新增拖拽调整右栏宽度。
- 不引入新的 Tab、Drawer、Modal 或二次编辑路径。
- 不修改节点配置字段语义，不新增后端字段。

## 视觉方向

右侧配置面板采用克制的后台 inspector 风格：

- 面板背景使用浅灰蓝 `#f8fafc` 或接近值。
- 卡片背景保持白色。
- 卡片圆角统一收敛到约 `8px`。
- 主输入控件高度收敛到约 `40px`。
- 复杂行内控件高度约 `30px - 34px`。
- 输入框和下拉框使用白底、浅边框，不再使用厚重内阴影。
- 分类色只用于小胶囊、轻描边和语义状态，不做大面积彩色头部。
- 删除、添加、排序等动作优先使用紧凑图标按钮或轻量整行按钮。

## 信息结构

右侧面板由以下区域组成：

1. 顶部节点识别区
2. 自动字段分组区
3. 出口路由摘要区
4. 可选预览或只读信息区

### 顶部节点识别区

顶部卡片保留，但降低高度和视觉重量。

展示内容：

- 节点类型，例如 `IF_CONDITION`
- 节点分类，例如 `逻辑分支`
- 出口数量，例如 `2 出口`
- 节点名称
- 一句弱说明，优先来自节点展示模型或根据节点能力生成

顶部不展示大图标、不使用大面积渐变、不放浮夸状态块。

### 自动字段分组区

字段按 schema 类型和展示意图自动归类。简单节点可能只有一个分组，复杂节点会拆成多个分组。

分组规则：

- `基础配置`：节点名称，以及普通 `input`、`select`、`radio`、`number`、`toggle`、`datetime`、`canvas-select`、`node-select` 字段。
- `条件规则`：`condition-rule-list`、`branch-list`、`priority-list`、`ab-group-list`、`cron`、`delay-input` 等决定判断、调度或流程分配的复杂控件。
- `参数映射`：`context-value-list`、`param-define-list`、`key-value`、`api-input-params`。
- `预览信息`：`event-attr-preview`、只读提示、上下文字段预览。
- `出口路由`：`edge-hint` 和由节点 `bizConfig`、`outletSchema` 推导出来的后继节点摘要。
- `高级配置`：保留给后续运行策略、重试、超时、限流等配置；本次不主动新增字段。

每个分组卡片标题行左侧显示分组名，右侧显示数量或模式，例如 `2 条`、`AND`、`3 个参数`。

## 复杂控件表现

### 条件规则

条件规则使用紧凑行内结构：

- 一行包含 `字段 / 操作符 / 值 / 删除`。
- 行高约 `32px`。
- 字段、操作符、值仍保持可编辑。
- 新增条件使用轻量整行按钮。

### 分支和优先级

分支类控件使用紧凑列表：

- 每条分支展示分支名、条件摘要、目标节点。
- 已连接目标显示节点名称。
- 未连接目标使用弱黄色状态，不留空。
- 删除按钮使用小图标按钮。

### 参数映射

参数映射使用两列或三列紧凑行：

- 参数名
- 来源值或上下文引用
- 删除按钮

复杂参数较多时优先保证纵向扫读，不把每个参数撑成完整大表单块。

### 代码编辑器

代码编辑器保留等宽字体和清晰边框：

- 不套大圆角卡片。
- 高度根据现有字段渲染逻辑保持稳定。
- 聚焦态只加强边框，不引入强阴影。

## 数据流

本次不改变数据流。

- `ConfigPanel` 继续加载节点 schema。
- `parseSchema` 继续把 `configSchema` 转为字段列表。
- `evaluateVisible` 和 `showWhen` 继续控制字段可见性。
- `handleValuesChange` 继续将 `name` 和 `bizConfig` 拆开回传。
- 子控件继续通过 `applyFormPatch` 写入多个字段。
- `onChange` 仍由画布编辑器写回 React Flow 节点数据。

新增的字段分组只影响渲染组织，不改变表单字段名、字段值或保存结构。

## 组件边界

建议保持现有模块边界：

- `presentation.ts`：生成节点头部信息、分组元信息、出口摘要所需的展示模型。
- `InspectorCards.tsx`：承载顶部卡片、分组卡片、摘要行、路由行等纯展示组件。
- `controlChrome.ts`：集中维护输入框、下拉框、行内控件和弹层的视觉尺寸。
- `index.tsx`：负责 schema 解析、表单渲染、字段分组和复杂控件组合。

如果新增 helper，优先放在 `presentation.ts` 或局部纯函数中，并配套单元测试。

## 只读态和错误处理

- 只读态继续依赖 Ant Design `Form disabled={readonly}`。
- 远程 dataSource 加载失败行为不在本次改变范围内。
- dataSource 依赖字段缺失时继续清空对应下拉选项。
- 未连接出口明确显示 `未连接`。
- 无节点选中时仍显示原有空态文案，可仅做右侧面板内的轻视觉收敛。

## 测试

需要补充或调整以下前端测试：

- `presentation.test.ts`：验证字段按类型归类到正确分组。
- `presentation.test.ts`：验证 `outletSchema` 或已知分支字段能生成出口路由摘要。
- `controlChrome.test.ts`：验证主控件和行内控件尺寸从大控件收敛到新规格。
- `displayValues.test.ts` 或现有相关测试：确保分组展示不改变 select 展示值解析。
- `formValues.test.ts`：确保分组渲染不改变 name / bizConfig 回写行为。

手工验证：

- 简单节点只出现基础配置，视觉不空。
- `IF_CONDITION` 条件规则行不拥挤。
- `AB_SPLIT`、`PRIORITY`、`SELECTOR` 等多分支节点路由清晰。
- `API_CALL`、`SEND_MQ` 等参数映射节点能保持可读密度。
- 只读画布中右侧控件不可编辑。

## 验收标准

- 只改右侧节点配置面板相关文件。
- 左侧节点库和中间画布视觉不发生变化。
- 主输入控件高度约 `40px`，卡片圆角约 `8px`。
- 复杂控件不再被 `60px` 大控件样式撑高。
- 所有节点类型仍通过 schema 驱动渲染。
- 保存、自动保存、节点切换、只读态和 dataSource 联动行为保持不变。
