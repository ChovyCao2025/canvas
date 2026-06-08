import type {
  BiDashboardRuntimeStateCommand,
  BiEmbedTicketPayload,
  BiQueryCacheInvalidationCommand,
  BiQueryCachePolicyCommand,
  BiQueryRequest,
  BiQuickEngineCapacityAlertPolicyCommand,
  BiQuickEngineTenantPoolPolicyCommand,
} from '../../services/biApi'

export type BiSectionKey =
  | 'overview'
  | 'data'
  | 'dataset'
  | 'chart'
  | 'dashboard'
  | 'portal'
  | 'self-service'
  | 'subscription'
  | 'embed'
  | 'ai'

export interface BiWorkbenchSection {
  /** 工作台导航 key，对应页面分区和 URL tab。 */
  key: BiSectionKey
  /** 导航展示名称。 */
  label: string
  /** 分区能力摘要，用于概览卡片。 */
  summary: string
  /** 该分区承载的 BI 产品能力说明。 */
  capability: string
}

export interface MarketingDatasetPreset {
  /** 数据集唯一 key，作为查询和资源引用的稳定标识。 */
  key: string
  /** 面向运营/分析人员展示的数据集名称。 */
  label: string
  /** 数据来源说明，例如 Doris 表或投递回执。 */
  source: string
  /** 是否平台内置的营销分析预置数据集。 */
  preset: boolean
  /** 数据集中对外展示的核心指标名称。 */
  metrics: string[]
}

export interface BiDatasetMetadataLike {
  /** 后端语义数据集 key。 */
  datasetKey: string
  /** 数据集指标元数据，用于转换为营销数据集预置项。 */
  metrics: Array<{ metricKey: string }>
}

export interface BiDashboardPresetLike {
  /** 仪表盘资源 key，用于发布、嵌入、订阅和导出定位。 */
  dashboardKey: string
  /** 仪表盘标题。 */
  title: string
  /** 仪表盘业务描述。 */
  description: string
  /** 默认查询数据集 key，组件未覆盖时使用该数据集。 */
  datasetKey: string
  /** 仪表盘组件布局和查询配置。 */
  widgets: BiDashboardWidgetPreset[]
  /** 仪表盘查询控件配置。 */
  filters: BiDashboardFilterPreset[]
  /** 跨控件共享的全局参数，用于 URL、嵌入票据和联动别名统一取值。 */
  globalParameters?: BiDashboardGlobalParameterPreset[] | null
  /** 组件之间的钻取、联动或超链接交互。 */
  interactions: BiDashboardInteractionPreset[]
  /** 允许订阅推送的渠道，例如 EMAIL、LARK。 */
  subscriptionChannels: string[]
  /** 允许生成嵌入 ticket 的作用域。 */
  embedScopes: string[]
}

export interface BiDashboardWidgetPreset {
  /** 组件唯一 key，也是查询结果和布局操作的索引。 */
  widgetKey: string
  /** 组件标题。 */
  title: string
  /** 图表类型，例如 KPI_CARD、LINE、BAR。 */
  chartType: string
  /** 参与查询的维度字段。 */
  dimensions: string[]
  /** 参与查询的指标字段。 */
  metrics: string[]
  /** 栅格布局横向起点。 */
  gridX: number
  /** 栅格布局纵向起点。 */
  gridY: number
  /** 栅格布局宽度。 */
  gridW: number
  /** 栅格布局高度。 */
  gridH: number
  /** 组件视觉样式预设。 */
  stylePreset: string
}

export interface BiDashboardFilterPreset {
  /** 控件 key，URL 参数、运行态状态和联动逻辑优先使用。 */
  filterKey: string
  /** 过滤字段 key，对应数据集维度字段。 */
  fieldKey: string
  /** 控件展示名称。 */
  label: string
  /** 控件类型，例如 SELECT、DATE_RANGE。 */
  controlType: string
  /** 是否必须提供过滤值。 */
  required: boolean
  /** 默认过滤值，支持相对日期等运行态解析。 */
  defaultValue?: string | null
  /** 控件只作用于指定组件；为空表示作用于全部组件。 */
  targetWidgetKeys?: string[] | null
  /** 级联配置，用于父控件约束子控件候选项。 */
  cascade?: BiDashboardFilterCascadePreset | null
  /** 候选项查询使用的数据集 key，缺省使用仪表盘数据集。 */
  optionDatasetKey?: string | null
  /** 候选项查询展示字段，缺省使用 fieldKey。 */
  optionFieldKey?: string | null
  /** 是否隐藏控件但保留参数过滤能力。 */
  hidden?: boolean | null
}

export interface BiDashboardFilterCascadePreset {
  /** 父级控件 key 列表，决定当前控件候选项的上游过滤条件。 */
  parentFilterKeys: string[]
  /** 父控件字段到当前候选项字段的映射，跨数据源级联时使用。 */
  parentFieldMapping?: Record<string, string> | null
  /** 级联匹配模式：同源字段或显式映射字段。 */
  mode?: 'SAME_SOURCE' | 'MAPPED' | null
}

export interface BiDashboardGlobalParameterPreset {
  /** 全局参数 key，作为运行态参数的主键。 */
  parameterKey: string
  /** 绑定的数据集字段，用于和查询控件互相镜像。 */
  fieldKey?: string | null
  /** 绑定的控件 key。 */
  filterKey?: string | null
  /** URL 或嵌入参数中的别名集合。 */
  aliases?: string[] | null
  /** 全局参数默认值。 */
  defaultValue?: string | null
  /** 是否锁定，锁定后已有值不会被普通控件修改覆盖。 */
  locked?: boolean | null
}

export interface BiDashboardInteractionPreset {
  /** 交互规则 key。 */
  interactionKey: string
  /** 触发交互的源组件 key。 */
  sourceWidgetKey: string
  /** 联动或钻取目标组件 key。 */
  targetWidgetKey?: string | null
  /** 交互类型，例如 DRILL_DOWN、FILTER_LINKAGE、HYPERLINK。 */
  interactionType: string
  /** 从点击行中读取的字段 key。 */
  fieldKey: string
  /** 超链接目标或自定义交互目标。 */
  target?: string | null
}

/** 仪表盘运行态参数值，覆盖 URL、嵌入 ticket、记住条件和默认值。 */
export type BiDashboardRuntimeParameterValue = string | string[] | number | boolean | null | undefined
/** 运行态参数字典，key 可能是 filterKey、fieldKey 或全局参数别名。 */
export type BiDashboardRuntimeParameters = Record<string, BiDashboardRuntimeParameterValue>
/** 运行态参数来源，用于 UI 展示当前值为何生效。 */
export type BiDashboardRuntimeStateSource = 'URL' | 'REMEMBERED' | 'DEFAULT' | 'CLEARED' | 'EMPTY'

export interface BiDashboardRuntimeStateRow {
  /** 控件 key。 */
  key: string
  /** 数据集字段 key。 */
  fieldKey: string
  /** 控件展示名称。 */
  label: string
  /** 已格式化的当前参数值。 */
  valueText: string
  /** 当前值来源。 */
  source: BiDashboardRuntimeStateSource
  /** 来源中文标签。 */
  sourceLabel: string
  /** 是否由全局参数锁定。 */
  locked: boolean
}

export interface BiEmbedTicketPreviewRow {
  /** 预览行稳定 key。 */
  key: string
  /** 面向用户展示的字段名。 */
  label: string
  /** 面向用户展示的字段值。 */
  value: string
}

export interface BiDesignerToolbarAction {
  key: string
  label: string
}

export interface BiDesignerPaletteItem {
  key: string
  label: string
  group: string
}

export interface DashboardPresetHistory {
  /** 撤销栈，存放当前版本之前的仪表盘快照。 */
  past: BiDashboardPresetLike[]
  /** 设计器当前正在编辑的仪表盘快照。 */
  present: BiDashboardPresetLike
  /** 重做栈，撤销后保留的后续快照。 */
  future: BiDashboardPresetLike[]
}

export interface BiDashboardPackageLike {
  /** 导入/导出的仪表盘预设内容。 */
  preset: BiDashboardPresetLike
  /** 来源包版本，用于兼容未来结构升级。 */
  sourceVersion?: number
}

export interface BiResourceLocationLike {
  /** 位置记录 ID。 */
  id?: number
  /** 租户 ID。 */
  tenantId?: number
  /** 工作区 ID。 */
  workspaceId?: number
  /** 资源类型，例如 DASHBOARD、DATASET、CHART。 */
  resourceType: string
  /** 资源 key。 */
  resourceKey: string
  /** 所属文件夹 key，空值表示根目录。 */
  folderKey?: string | null
  /** 文件夹内排序值。 */
  sortOrder?: number | null
  /** 最近移动操作者。 */
  movedBy?: string | null
  /** 最近移动时间。 */
  movedAt?: string | null
}

export interface BiResourceMoveCommandLike {
  resourceType: string
  resourceKey: string
  folderKey: string | null
  sortOrder: number
}

export interface BiResourceOwnershipLike {
  /** 归属记录 ID。 */
  id?: number
  /** 租户 ID。 */
  tenantId?: number
  /** 工作区 ID。 */
  workspaceId?: number
  /** 资源类型。 */
  resourceType: string
  /** 资源 key。 */
  resourceKey: string
  /** 当前负责人账号。 */
  ownerUser?: string | null
  /** 最近转交操作者。 */
  transferredBy?: string | null
  /** 最近转交时间。 */
  transferredAt?: string | null
}

export interface BiResourceFavoriteLike {
  id?: number | null
  tenantId?: number
  workspaceId?: number
  resourceType: string
  resourceKey: string
  username?: string | null
  favorite?: boolean | null
  createdAt?: string | null
}

export interface BiResourceCommentLike {
  id?: number | null
  tenantId?: number
  workspaceId?: number
  resourceType: string
  resourceKey: string
  widgetKey?: string | null
  commentText: string
  createdBy?: string | null
  createdAt?: string | null
  deletedAt?: string | null
}

export interface BiResourceLockLike {
  /** 锁记录 ID。 */
  id?: number | null
  /** 租户 ID。 */
  tenantId?: number
  /** 工作区 ID。 */
  workspaceId?: number
  /** 被锁资源类型。 */
  resourceType: string
  /** 被锁资源 key。 */
  resourceKey: string
  /** 草稿锁 token，保存草稿时用于乐观并发保护。 */
  lockToken?: string | null
  /** 持锁人。 */
  lockedBy?: string | null
  /** 加锁时间。 */
  lockedAt?: string | null
  /** 锁过期时间。 */
  expiresAt?: string | null
  /** 当前是否有效持锁。 */
  locked?: boolean | null
}

export interface BiPublishApprovalLike {
  /** 审批记录 ID。 */
  id?: number | null
  /** 租户 ID。 */
  tenantId?: number
  /** 工作区 ID。 */
  workspaceId?: number
  /** 待发布资源类型。 */
  resourceType: string
  /** 待发布资源 key。 */
  resourceKey: string
  /** 审批状态，例如 PENDING、APPROVED、REJECTED。 */
  status: string
  /** 发起发布审批的原因。 */
  reason?: string | null
  /** 申请人。 */
  requestedBy?: string | null
  /** 申请时间。 */
  requestedAt?: string | null
  /** 审核人。 */
  reviewedBy?: string | null
  /** 审核时间。 */
  reviewedAt?: string | null
  /** 审核意见。 */
  reviewComment?: string | null
}

export interface BiResourceTransferCommandLike {
  resourceType: string
  resourceKey: string
  ownerUser: string
}

export interface BiResourceFavoriteCommandLike {
  resourceType: string
  resourceKey: string
  favorite: boolean
}

export interface BiResourceCommentCommandLike {
  resourceType: string
  resourceKey: string
  widgetKey: string | null
  commentText: string
}

export interface BiResourceLockCommandLike {
  resourceType: string
  resourceKey: string
  lockToken: string
  ttlSeconds: number | null
}

export interface BiPublishApprovalRequestCommandLike {
  resourceType: string
  resourceKey: string
  reason: string | null
}

export interface BiPublishApprovalReviewCommandLike {
  approvalId: number | null
  status: string
  reviewComment: string | null
}

export interface BiPermissionRequestCommandLike {
  /** 申请访问的资源类型。 */
  resourceType: string
  /** 申请访问的资源 key。 */
  resourceKey: string
  /** 申请动作，例如 USE、EDIT、EXPORT、PUBLISH。 */
  requestedAction: string
  /** 申请理由。 */
  reason: string | null
}

export interface BiPermissionRequestReviewCommandLike {
  requestId: number | null
  status: string
  reviewComment: string | null
}

export interface BiResourceTargetLike {
  /** 下拉展示文案。 */
  label: string
  /** 下拉值，通常由资源类型和 key 拼接。 */
  value: string
  /** BI 资源类型。 */
  resourceType: string
  /** BI 资源 key。 */
  resourceKey: string
  /** 是否禁止选择，常用于缺少可操作资源时兜底展示。 */
  disabled: boolean
}

export interface BiResourceTargetInputLike {
  dashboardKey?: string | null
  chartKey?: string | null
  datasetKey?: string | null
  dataSourceKey?: string | null
  portalKey?: string | null
  bigScreenKey?: string | null
  spreadsheetKey?: string | null
}

export interface BiResourcePickerOptionLike {
  label: string
  value: string
  disabled: boolean
}

export interface BiBigScreenResourceOptionLike {
  id?: number | null
  screenKey?: string | null
  name?: string | null
  status?: string | null
}

export interface BiSpreadsheetResourceOptionLike {
  id?: number | null
  spreadsheetKey?: string | null
  name?: string | null
  status?: string | null
}

export interface BiRuntimeRouteLike {
  mode: 'big-screen' | 'spreadsheet' | null
  resourceType: string | null
  resourceId: number | null
  resourceKey: string | null
}

export interface BiBigScreenDraftInputLike {
  screenKey?: string | null
  name?: string | null
  description?: string | null
  dashboardKey?: string | null
  datasetKey?: string | null
}

export interface BiBigScreenLayoutPatchLike {
  widgetKey?: string | null
  title?: string | null
  resourceType?: string | null
  resourceKey?: string | null
  datasetKey?: string | null
  x?: string | number | null
  y?: string | number | null
  w?: string | number | null
  h?: string | number | null
}

export type BiPortalNavigationLayout = 'top' | 'left' | 'dual'

export interface BiPortalNavigationConfigPatchLike {
  /** 门户导航布局：顶部、左侧或双栏。 */
  navigationLayout?: string | null
  /** 默认打开的菜单 key。 */
  defaultMenuKey?: string | null
  /** 是否开启菜单搜索。 */
  menuSearchEnabled?: boolean | null
  /** 是否允许门户全屏展示。 */
  fullScreenEnabled?: boolean | null
  /** 是否启用移动端适配。 */
  mobileEnabled?: boolean | null
  /** 门户 Logo 地址。 */
  logoUrl?: string | null
  /** 门户主标题。 */
  title?: string | null
  /** 门户副标题。 */
  subtitle?: string | null
  /** 门户页脚文案。 */
  footerText?: string | null
  /** 门户访问别名。 */
  alias?: string | null
  /** 是否展示面包屑。 */
  breadcrumbEnabled?: boolean | null
  /** 是否缓存菜单结构。 */
  menuCacheEnabled?: boolean | null
  /** 菜单缓存 TTL，单位秒。 */
  menuCacheTtlSeconds?: string | number | null
}

export interface BiPortalMenuResourceLike {
  /** 菜单 key。 */
  menuKey?: unknown
  /** 父菜单 key，空值表示一级菜单。 */
  parentMenuKey?: unknown
  /** 菜单标题。 */
  title?: unknown
  /** 菜单可见性和扩展配置，例如 iconKey。 */
  visibility?: Record<string, unknown> | null
  /** 菜单排序值。 */
  sortOrder?: unknown
}

export interface BiPortalResourceLike {
  /** 门户主题、导航和品牌化配置。 */
  theme?: Record<string, unknown> | null
  /** 门户菜单资源树。 */
  menus?: BiPortalMenuResourceLike[] | null
}

export interface BiPortalMenuConfigPatchLike {
  /** 菜单标题补丁。 */
  title?: string | null
  /** 父菜单 key 补丁。 */
  parentMenuKey?: string | null
  /** 菜单图标 key 补丁，写入 visibility.iconKey。 */
  iconKey?: string | null
}

export type BiPortalMenuDropPosition = 'before' | 'after' | 'inside'

export interface BiChartReferenceResourceLike {
  /** 图表资源 key。 */
  chartKey?: string | null
  /** 图表绑定的数据集 key。 */
  datasetKey?: string | null
  /** 图表查询字段快照，用于展示引用影响摘要。 */
  query?: {
    dimensions?: string[] | null
    metrics?: string[] | null
  } | null
}

export interface BiChartReferenceDashboardLike {
  /** 仪表盘 key。 */
  dashboardKey: string
  /** 仪表盘标题。 */
  title?: string | null
  /** 仪表盘内可能引用图表的组件列表。 */
  widgets?: Array<{
    widgetKey: string
    title?: string | null
    chartKey?: string | null
    resourceType?: string | null
    resourceKey?: string | null
  }> | null
}

export interface BiChartReferencePortalLike {
  /** 门户 key。 */
  portalKey: string
  /** 门户名称。 */
  name?: string | null
  /** 门户内可能引用图表的菜单列表。 */
  menus?: Array<{
    menuKey: string
    title?: string | null
    resourceType?: string | null
    resourceKey?: string | null
  }> | null
}

export interface BiChartReferenceSubscriptionLike {
  /** 订阅 key。 */
  subscriptionKey: string
  /** 订阅名称。 */
  name?: string | null
  /** 订阅目标资源类型。 */
  resourceType?: string | null
  /** 订阅目标资源 key。 */
  resourceKey?: string | null
}

export interface BiChartReferenceImpactInputLike {
  dashboards?: BiChartReferenceDashboardLike[] | null
  portals?: BiChartReferencePortalLike[] | null
  subscriptions?: BiChartReferenceSubscriptionLike[] | null
}

export interface BiChartReferenceImpactViewLike {
  dashboards?: Array<{
    dashboardKey: string
    title?: string | null
    widgetKey: string
    widgetTitle?: string | null
  }> | null
  portals?: Array<{
    portalKey: string
    name?: string | null
    menuKey: string
    menuTitle?: string | null
  }> | null
  subscriptions?: Array<{
    subscriptionKey: string
    name?: string | null
  }> | null
}

export interface BiChartQueryDesignerInputLike {
  datasetKey: string
  selectedDimensions?: string[] | null
  selectedMetrics?: string[] | null
  filterField?: string | null
  filterOperator?: string | null
  filterValue?: string | null
  sortField?: string | null
  sortDirection?: string | null
  limit?: number | string | null
}

export interface BiChartQueryPatchLike {
  datasetKey: string
  dimensions: string[]
  metrics: string[]
  filters: BiQueryRequest['filters']
  sorts: BiQueryRequest['sorts']
  limit: number
}

export interface BiChartQueryFieldStateLike {
  dimensions: string[]
  metrics: string[]
}

export type BiBigScreenLayoutDirection = 'left' | 'right' | 'up' | 'down'
export type BiBigScreenMobileLayoutVariant = 'single-column' | 'compact-grid'

export interface BiBigScreenDraftResourceLike {
  screenKey: string
  name: string
  description: string | null
  size: Record<string, unknown>
  background: Record<string, unknown>
  layout: Record<string, unknown>[]
  refresh: Record<string, unknown>
  mobileLayout: Record<string, unknown>
  status: string
  version: number
  source: string
}

export interface BiBigScreenLibraryComponentLike {
  key: string
  title: string
  componentType: string
  w: number
  h: number
  resourceType: string
}

export interface BiSpreadsheetDraftInputLike {
  spreadsheetKey?: string | null
  name?: string | null
  description?: string | null
  datasetKey?: string | null
}

export interface BiSpreadsheetDraftResourceLike {
  spreadsheetKey: string
  name: string
  description: string | null
  sheets: Record<string, unknown>[]
  dataBinding: Record<string, unknown>
  style: Record<string, unknown>
  status: string
  version: number
  source: string
}

export type BiSpreadsheetPivotAggregation = 'SUM' | 'COUNT' | 'AVERAGE' | 'MIN' | 'MAX'

export interface BiSpreadsheetPivotValueFieldInputLike {
  field: string
  aggregation?: BiSpreadsheetPivotAggregation | string | null
  label?: string | null
}

export interface BiSpreadsheetPivotTableInputLike {
  sourceRange: string
  targetCell: string
  rowField: string
  columnField: string
  valueField: string
  aggregation?: BiSpreadsheetPivotAggregation | string | null
  valueFields?: BiSpreadsheetPivotValueFieldInputLike[] | null
}

export type BiSqlDatasetParameterDataType = 'STRING' | 'NUMBER' | 'DATE' | 'DATETIME' | 'BOOLEAN' | 'PERCENT'

export interface BiSqlDatasetParameterDraftLike {
  key?: string | null
  dataType?: string | null
  required?: boolean | null
  defaultValue?: string | null
  allowedValues?: string[] | null
  allowedValuesText?: string | null
}

export interface BiSqlDatasetParameterResolvedLike {
  key: string
  dataType: BiSqlDatasetParameterDataType
  required: boolean
  defaultValue: string
  allowedValuesText: string
}

export interface BiSqlDatasetFieldDraftLike {
  fieldKey?: string | null
  displayName?: string | null
  columnExpression?: string | null
  role?: 'DIMENSION' | 'MEASURE' | string | null
  dataType?: string | null
  semanticType?: string | null
  defaultAggregation?: string | null
  formatPattern?: string | null
  unit?: string | null
  visible?: boolean | null
  sensitiveLevel?: string | null
}

export interface BiSqlDatasetMetricDraftLike {
  metricKey?: string | null
  displayName?: string | null
  expression?: string | null
  aggregation?: string | null
  dataType?: string | null
  unit?: string | null
  formatPattern?: string | null
  allowedDimensions?: string[] | null
  owner?: string | null
  description?: string | null
  status?: string | null
}

export interface BiSqlDatasetDraftInputLike {
  dataSourceConfigId?: number | null
  datasetKey?: string | null
  name?: string | null
  sqlTemplate?: string | null
  tenantColumn?: string | null
  parameters?: BiSqlDatasetParameterDraftLike[] | null
  fields?: BiSqlDatasetFieldDraftLike[] | null
  metrics?: BiSqlDatasetMetricDraftLike[] | null
}

export interface BiSqlDatasetSampleColumnLike {
  key: string
  role?: string | null
  dataType?: string | null
}

export interface BiSqlDatasetSampleProfileInputLike {
  columns?: BiSqlDatasetSampleColumnLike[] | null
  rows?: Array<Record<string, unknown>> | null
  rowCount?: number | null
  sampleLimit?: number | null
  sampleExecuted?: boolean | null
}

export interface BiSqlDatasetSampleProfileRow {
  key: string
  field: string
  role: string
  dataType: string
  filled: string
  unique: string
  samples: string
}

export interface BiSqlDatasetLineageLike {
  dataSourceConfigId?: number | null
  sourceTables?: string[] | null
  parameterKeys?: string[] | null
  tenantColumn?: string | null
  referencedFields?: string[] | null
  referencedMetrics?: string[] | null
  approvalRequired?: boolean | null
}

export interface BiSqlDatasetImpactLike {
  impactedAssetTypes?: string[] | null
  governanceGates?: string[] | null
  warnings?: string[] | null
}

export interface BiSqlDatasetImpactInputLike {
  datasetKey?: string | null
  lineage?: BiSqlDatasetLineageLike | null
  impact?: BiSqlDatasetImpactLike | null
}

export interface BiSqlDatasetImpactRow {
  key: string
  label: string
  value: string
}

export type BiSqlDatasetReadinessStatus = 'pass' | 'warn' | 'block'

export interface BiSqlDatasetReadinessPreviewLike extends BiSqlDatasetSampleProfileInputLike {
  compiledSql?: string | null
  parameterCount?: number | null
  executionError?: string | null
  lineage?: BiSqlDatasetLineageLike | null
  impact?: BiSqlDatasetImpactLike | null
}

export interface BiSqlDatasetReadinessInputLike {
  draft?: {
    fields?: BiSqlDatasetFieldDraftLike[] | null
    metrics?: BiSqlDatasetMetricDraftLike[] | null
    tenantColumn?: string | null
  } | null
  parameters?: BiSqlDatasetParameterResolvedLike[] | null
  preview?: BiSqlDatasetReadinessPreviewLike | null
}

export interface BiSqlDatasetReadinessRow {
  key: string
  label: string
  status: BiSqlDatasetReadinessStatus
  statusLabel: string
  detail: string
}

export interface BiBigScreenResourceLike extends BiBigScreenResourceOptionLike {
  description?: string | null
  size?: Record<string, unknown> | null
  background?: Record<string, unknown> | null
  layout?: Record<string, unknown>[] | null
  refresh?: Record<string, unknown> | null
  mobileLayout?: Record<string, unknown> | null
  version?: number | null
  source?: string | null
}

export interface BiSpreadsheetResourceLike extends BiSpreadsheetResourceOptionLike {
  description?: string | null
  sheets?: Record<string, unknown>[] | null
  dataBinding?: Record<string, unknown> | null
  style?: Record<string, unknown> | null
  version?: number | null
  source?: string | null
}

export interface BiSpreadsheetCellStylePatchLike {
  bold?: boolean | null
  backgroundColor?: string | null
  textColor?: string | null
}

export interface BiBigScreenLayoutSnapResult<T extends BiBigScreenResourceLike> {
  resource: T
  guides: DashboardSnapGuide[]
}

export interface BiBigScreenMobileLayoutItemLike {
  widgetKey: string
  x: number
  y: number
  w: number
  h: number
}

export interface BiEditorSummaryRow {
  label: string
  value: string
}

export type BiVisualEditorDiagnosticStatus = 'pass' | 'warn' | 'block'

export interface BiVisualEditorDiagnosticRow {
  key: string
  label: string
  status: BiVisualEditorDiagnosticStatus
  statusLabel: string
  detail: string
}

interface BiSpreadsheetVisualStats {
  cellCount: number
  formulaCount: number
  errorCount: number
  styleCount: number
  pivotTableCount: number
  conditionalFormatCount: number
}

export interface BiExportApprovalReviewCommandLike {
  status: 'APPROVED' | 'REJECTED'
  reviewComment: string | null
}

export interface BiExportAuditJobLike {
  id?: number | null
  resourceKey?: string | null
  resourceId?: number | null
  exportFormat?: string | null
  rowLimit?: number | null
  status?: string | null
  progressPercent?: number | null
  storageProvider?: string | null
  storageKey?: string | null
  retentionDays?: number | null
  expiresAt?: string | null
  downloadCount?: number | null
  lastDownloadedAt?: string | null
  approvalStatus?: string | null
  approvalReason?: string | null
  requestedBy?: string | null
  reviewedBy?: string | null
  retryCount?: number | null
  maxRetryCount?: number | null
  retryExhaustedAt?: string | null
  createdBy?: string | null
  createdAt?: string | null
  storageLayout?: string | null
  requestedRows?: number | null
  generatedRows?: number | null
  partCount?: number | null
  partSize?: number | null
  partStorageKeys?: string[] | null
}

export interface BiExportAuditRequestLike {
  exportFormat?: string | null
  rowLimit?: number | null
  approvalRequired?: boolean | null
  sensitive?: boolean | null
  approvalReason?: string | null
  query?: BiQueryRequest | null
}

export interface BiExportAuditDetailLike {
  job?: BiExportAuditJobLike | null
  request?: BiExportAuditRequestLike | null
  partition?: BiExportAuditPartitionLike | null
}

export interface BiExportAuditDetailRow {
  label: string
  value: string
}

export type BiExportHardeningDiagnosticStatus = 'pass' | 'warn' | 'block'

export interface BiExportHardeningDiagnosticRow {
  key: string
  label: string
  status: BiExportHardeningDiagnosticStatus
  statusLabel: string
  detail: string
}

export type BiAlertAnomalyDiagnosticStatus = 'pass' | 'warn' | 'block'

export interface BiAlertAnomalyDiagnosticRuleLike {
  alertKey?: string | null
  name?: string | null
  enabled?: boolean | null
  condition?: Record<string, unknown> | null
}

export interface BiAlertAnomalyDiagnosticRow {
  key: string
  label: string
  status: BiAlertAnomalyDiagnosticStatus
  statusLabel: string
  detail: string
}

export interface BiExportAuditPartitionLike {
  storageLayout?: string | null
  requestedRows?: number | null
  generatedRows?: number | null
  partCount?: number | null
  partSize?: number | null
  partStorageKeys?: string[] | null
}

export interface BiQueryHistoryDetailLike {
  id?: number | null
  datasetKey?: string | null
  username?: string | null
  request?: BiQueryRequest | null
  rowCount?: number | null
  durationMs?: number | null
  status?: string | null
  sqlHash?: string | null
  errorMessage?: string | null
  createdAt?: string | null
}

export interface BiQueryHistoryDetailRow {
  label: string
  value: string
}

export interface BiQueryGovernanceDatasetStatsLike {
  datasetKey?: string | null
  totalQueries?: number | null
  slowQueries?: number | null
  failedQueries?: number | null
  cacheHits?: number | null
  averageDurationMs?: number | null
  maxDurationMs?: number | null
  timeoutPolicyMs?: number | null
  quotaRows?: number | null
  slowFailures?: number | null
  slowCacheMisses?: number | null
  maxOverPolicyMs?: number | null
  maxRowCount?: number | null
}

export interface BiQueryGovernanceSlowAttributionLike {
  datasetKey?: string | null
  slowQueries?: number | null
  maxDurationMs?: number | null
  timeoutPolicyMs?: number | null
  maxOverPolicyMs?: number | null
}

export interface BiQueryGovernanceSummaryLike {
  totalQueries?: number | null
  slowQueries?: number | null
  failedQueries?: number | null
  cacheHits?: number | null
  averageDurationMs?: number | null
  timeoutPolicyMs?: number | null
  datasetQuotaRows?: number | null
  datasets?: BiQueryGovernanceDatasetStatsLike[] | null
  slowAttributions?: BiQueryGovernanceSlowAttributionLike[] | null
}

export interface BiQueryGovernanceSummaryRow {
  label: string
  value: string
}

export interface BiQueryGovernancePolicyDatasetLike {
  datasetKey: string
  timeoutMs: number
  quotaRows: number
}

export interface BiQueryGovernancePolicyLike {
  defaultTimeoutMs: number
  defaultQuotaRows: number
  datasets: BiQueryGovernancePolicyDatasetLike[]
}

export interface BiQueryGovernancePolicyRow {
  label: string
  value: string
}

export interface BiQueryCachePolicyResourceLike {
  resourceType?: string | null
  resourceKey?: string | null
  enabled?: boolean | null
  ttlSeconds?: number | null
  cacheMode?: string | null
}

export interface BiQueryCachePolicyLike {
  defaultEnabled?: boolean | null
  defaultTtlSeconds?: number | null
  defaultCacheMode?: string | null
  resources?: BiQueryCachePolicyResourceLike[] | null
}

export interface BiQueryCachePolicyRow {
  label: string
  value: string
}

export interface BiQueryCachePolicyDefaultDraftLike {
  enabled?: boolean | null
  ttlSeconds?: number | null
  cacheMode?: string | null
}

export interface BiQueryCachePolicyResourceDraftLike {
  resourceType?: string | null
  resourceKey?: string | null
  enabled?: boolean | null
  ttlSeconds?: number | null
  cacheMode?: string | null
}

export interface BiQueryCacheInvalidationActionRow {
  key: string
  label: string
  command: BiQueryCacheInvalidationCommand
}

export interface BiQueryCacheStatsLike {
  provider?: string | null
  enabled?: boolean | null
  entryCount?: number | null
  maxEntries?: number | null
  ttlSeconds?: number | null
  hitCount?: number | null
  missCount?: number | null
  putCount?: number | null
  evictionCount?: number | null
}

export interface BiQueryCacheStatsRow {
  label: string
  value: string
}

export interface BiQuickEngineCapacityAlertPolicyLike {
  enabled?: boolean | null
  capacityLimitRows?: number | null
  warningThresholdPercent?: number | null
  criticalThresholdPercent?: number | null
  notificationChannels?: string[] | null
  notificationReceivers?: string[] | null
  updatedBy?: string | null
  updatedAt?: string | null
}

export interface BiQuickEngineTenantPoolPolicyLike {
  poolKey?: string | null
  maxConcurrentQueries?: number | null
  queueLimit?: number | null
  queueTimeoutSeconds?: number | null
  poolWeight?: number | null
  updatedBy?: string | null
  updatedAt?: string | null
}

export interface BiQuickEngineConcurrencyQueueLike {
  runningQueries?: number | null
  queuedQueries?: number | null
  blockedQueries?: number | null
  successfulQueries?: number | null
  failedQueries?: number | null
  concurrencyUsagePercent?: number | null
  queueUsagePercent?: number | null
  state?: string | null
}

export interface BiQuickEngineCapacityCategoryUsageLike {
  type?: string | null
  usedRows?: number | null
  resourceCount?: number | null
}

export interface BiQuickEngineCapacityUsageDetailLike {
  type?: string | null
  resourceKey?: string | null
  usedRows?: number | null
  activeTables?: number | null
  latestRunId?: number | null
  latestFinishedAt?: string | null
  latestRowCount?: number | null
  owner?: string | null
}

export interface BiQuickEngineCapacityUserUsageLike {
  user?: string | null
  usedRows?: number | null
  activeTables?: number | null
  resourceCount?: number | null
}

export interface BiQuickEngineCapacitySummaryLike {
  tenantId?: number | null
  capacityLimitRows?: number | null
  usedRows?: number | null
  usagePercent?: number | null
  alertLevel?: string | null
  alertEnabled?: boolean | null
  alertPolicy?: BiQuickEngineCapacityAlertPolicyLike | null
  tenantPoolPolicy?: BiQuickEngineTenantPoolPolicyLike | null
  concurrencyQueue?: BiQuickEngineConcurrencyQueueLike | null
  categories?: BiQuickEngineCapacityCategoryUsageLike[] | null
  details?: BiQuickEngineCapacityUsageDetailLike[] | null
  userRankings?: BiQuickEngineCapacityUserUsageLike[] | null
}

export interface BiQuickEngineCapacitySummaryRow {
  label: string
  value: string
}

export interface BiQuickEngineConcurrencyQueueRow {
  label: string
  value: string
}

export interface BiQuickEngineCapacityDetailRow {
  key: string
  type: string
  resourceKey: string
  usedRows: number
  activeTables: number
  latest: string
  owner: string
}

export interface BiQuickEngineCapacityUserRow {
  key: string
  user: string
  usedRows: number
  activeTables: number
  resourceCount: number
}

export interface BiQuickEngineCapacityAlertPolicyDraftLike {
  enabled?: boolean | null
  capacityLimitRows?: number | null
  warningThresholdPercent?: number | null
  criticalThresholdPercent?: number | null
  notificationChannels?: string[] | string | null
  notificationReceivers?: string[] | string | null
}

export interface BiQuickEngineTenantPoolPolicyDraftLike {
  poolKey?: string | null
  maxConcurrentQueries?: number | null
  queueLimit?: number | null
  queueTimeoutSeconds?: number | null
  poolWeight?: number | null
}

export interface BiDatasetExtractRefreshRunLike {
  id?: number | null
  datasetKey?: string | null
  status?: string | null
  rowCount?: number | null
  durationMs?: number | null
  materializedTable?: string | null
  requestedBy?: string | null
  startedAt?: string | null
  finishedAt?: string | null
  errorSummary?: string | null
}

export interface BiDatasetAccelerationPolicyLike {
  datasetKey?: string | null
  enabled?: boolean | null
  accelerationMode?: string | null
  refreshMode?: string | null
  refreshIntervalMinutes?: number | null
  ttlSeconds?: number | null
  maxRows?: number | null
  cronExpression?: string | null
  materializedTable?: string | null
  lastStatus?: string | null
  lastRunId?: number | null
  lastRefreshedAt?: string | null
  recentRuns?: BiDatasetExtractRefreshRunLike[] | null
}

export interface BiDatasetAccelerationPolicyRow {
  label: string
  value: string
}

export interface BiDatasetAccelerationSchedulerItemLike {
  datasetKey?: string | null
  status?: string | null
  reason?: string | null
  refreshRunId?: number | null
  rowCount?: number | null
  durationMs?: number | null
  materializedTable?: string | null
  startedAt?: string | null
  finishedAt?: string | null
}

export interface BiDatasetAccelerationSchedulerResultLike {
  policiesChecked?: number | null
  refreshed?: number | null
  skipped?: number | null
  failed?: number | null
  items?: BiDatasetAccelerationSchedulerItemLike[] | null
}

export interface BiDatasetAccelerationSchedulerRow {
  key: string
  datasetKey: string
  status: string
  reason: string
  run: string
  materializedTable: string
  window: string
}

export interface BiQueryGovernanceAuditEntryLike {
  id?: number | null
  actorId?: string | null
  actionKey?: string | null
  resourceType?: string | null
  detailJson?: string | null
  createdAt?: string | null
}

export interface BiQueryGovernanceAuditRow {
  key: string
  actor: string
  action: string
  resource: string
  detail: string
  createdAt: string
}

export interface BiPermissionAuditEntryLike {
  id?: number | null
  actorId?: string | null
  actionKey?: string | null
  resourceType?: string | null
  detailJson?: string | null
  createdAt?: string | null
}

export interface BiPermissionAuditRow {
  key: string
  actor: string
  action: string
  resource: string
  detail: string
  createdAt: string
}

export interface BiDatasourceHealthSnapshotLike {
  sourceKey: string
  sourceType: string
  available: boolean
  message: string
  checkedAt: string
}

export interface BiDatasourceHealthHistoryRow {
  key: string
  source: string
  status: string
  message: string
  checkedAt: string
}

export interface BiDatasourceHealthSourceSloLike {
  sourceKey?: string | null
  sourceType?: string | null
  totalChecks?: number | null
  availableChecks?: number | null
  unavailableChecks?: number | null
  availabilityRate?: number | null
  lastCheckedAt?: string | null
  lastMessage?: string | null
  riskLevel?: string | null
  recommendedAction?: string | null
}

export interface BiDatasourceHealthSloLike {
  totalChecks?: number | null
  availableChecks?: number | null
  unavailableChecks?: number | null
  availabilityRate?: number | null
  sources?: BiDatasourceHealthSourceSloLike[] | null
}

export interface BiDatasourceHealthSloRow {
  label: string
  value: string
}

export interface BiDatasourceConnectorCapabilityLike {
  connectorType?: string | null
  label?: string | null
  sourceCategory?: string | null
  supportedModes?: string[] | null
  supportStatus?: string | null
  capacityCategory?: string | null
  capacityNote?: string | null
  supportsConnectionTest?: boolean | null
  supportsSchemaSync?: boolean | null
  supportsSqlDataset?: boolean | null
  supportsTableDataset?: boolean | null
  supportsCredentials?: boolean | null
  driverClassNames?: string[] | null
  note?: string | null
}

export interface BiDatasourceConnectorRow {
  key: string
  connector: string
  category: string
  capacity: string
  modes: string
  status: string
  capabilities: string
}

export interface BiDatasourceCapacityPolicyRow {
  key: string
  connector: string
  capacityPool: string
  budget: string
  eligibility: string
  guardrails: string
}

export interface BiDatasourceAdvancedCapabilityRow {
  key: string
  connector: string
  quickEngine: string
  crossSourceModeling: string
  selfService: string
  semanticAuthoring: string
  risk: string
}

export interface BiDatasourceOnboardingLike {
  id?: number | null
  sourceKey?: string | null
  name?: string | null
  type?: string | null
  connectorType?: string | null
  enabled?: boolean | null
  driverClassName?: string | null
  maskedUrl?: string | null
  maskedUsername?: string | null
  connectionMode?: string | null
  schemaSyncStatus?: string | null
  tableCount?: number | null
  lastSyncedAt?: string | null
  supportedModes?: string[] | null
  supportStatus?: string | null
  capabilities?: string[] | null
}

export interface BiDatasourceOnboardingRow {
  key: string
  id?: number | null
  source: string
  connector: string
  status: string
  connection: string
  schema: string
  credential: string
}

export interface BiDatasourceNextActionRow {
  key: string
  source: string
  readiness: string
  nextAction: string
  limitations: string
}

export interface BiDatasourceOnboardingDraftInputLike {
  connectorType?: string | null
  name?: string | null
  url?: string | null
  username?: string | null
  password?: string | null
  driverClassName?: string | null
  description?: string | null
  enabled?: boolean | null
  connectionMode?: string | null
  apiRequestMethod?: string | null
  apiAuthType?: string | null
  apiHeaderName?: string | null
  apiHeaderValue?: string | null
  apiHeaderVariable?: boolean | null
  apiParameterName?: string | null
  apiParameterValue?: string | null
  apiParameterVariable?: boolean | null
  apiBodyTemplate?: string | null
  apiResponseRowsPath?: string | null
  apiResponseFormat?: string | null
  fileName?: string | null
  fileType?: string | null
  fileSheetName?: string | null
  fileDelimiter?: string | null
  fileHeaderRow?: boolean | null
  fileEncoding?: string | null
}

export interface BiDatasourceOnboardingCommandLike {
  connectorType: string
  name: string
  url: string
  username: string
  password: string
  driverClassName: string
  description: string
  enabled: boolean
  connectionMode: string
  connectorConfig?: Record<string, unknown>
}

export interface BiDatasourceConnectionTestLike {
  id?: number | null
  sourceKey?: string | null
  connectorType?: string | null
  success?: boolean | null
  message?: string | null
  databaseProductName?: string | null
  databaseProductVersion?: string | null
  checkedAt?: string | null
  durationMs?: number | null
}

export interface BiDatasourceConnectionTestRow {
  label: string
  value: string
}

export interface BiDatasourceColumnPreviewLike {
  name?: string | null
  typeName?: string | null
  dataType?: number | null
  nullable?: boolean | null
  ordinalPosition?: number | null
}

export interface BiDatasourceTablePreviewLike {
  name?: string | null
  tableType?: string | null
  columns?: BiDatasourceColumnPreviewLike[] | null
}

export interface BiDatasourceSchemaPreviewLike {
  id?: number | null
  sourceKey?: string | null
  name?: string | null
  connectorType?: string | null
  tables?: BiDatasourceTablePreviewLike[] | null
  checkedAt?: string | null
}

export interface BiDatasourceSchemaPreviewRow {
  key: string
  table: string
  columns: string
  columnCount: string
  checkedAt: string
}

export interface BiDatasourceSchemaSnapshotLike {
  id?: number | null
  dataSourceConfigId?: number | null
  sourceKey?: string | null
  name?: string | null
  connectorType?: string | null
  syncStatus?: string | null
  errorMessage?: string | null
  tableCount?: number | null
  columnCount?: number | null
  tables?: BiDatasourceTablePreviewLike[] | null
  syncedAt?: string | null
  syncedBy?: string | null
}

export interface BiDatasourceSchemaSnapshotRow {
  label: string
  value: string
}

export interface BiDatasourceSchemaSnapshotHistoryRow {
  key: string
  source: string
  status: string
  schema: string
  syncedAt: string
  syncedBy: string
  error: string
}

export interface BiDatasourceTableDatasetCommandLike {
  dataSourceConfigId: number
  tableName: string
  datasetKey: string
  name: string
  tenantColumn: string
  selectedColumns: string[]
  apiResponseVariables?: Record<string, string>
}

export interface BiDatasourceMultiTableJoinInputLike {
  joinType?: string | null
  leftTableName?: string | null
  leftColumn?: string | null
  rightTableName?: string | null
  rightColumn?: string | null
  conditions?: Array<{
    leftColumn?: string | null
    operator?: string | null
    connector?: string | null
    rightColumn?: string | null
    groupStart?: boolean | null
    groupEnd?: boolean | null
  }> | null
}

export interface BiDatasourceMultiTableModelInputLike {
  baseTableName?: string | null
  tableNames?: string[] | null
  tenantColumn?: string | null
  joins?: BiDatasourceMultiTableJoinInputLike[] | null
  graphNodes?: Array<{
    tableName?: string | null
    alias?: string | null
    x?: number | null
    y?: number | null
  }> | null
}

export interface BiDatasourceMultiTableDatasetCommandLike {
  dataSourceConfigId: number
  datasetKey: string
  name: string
  baseTableName: string
  tenantColumn: string
  tables: Array<{
    tableName: string
    alias: string
    selectedColumns: string[]
  }>
  joins: Array<{
    joinType: string
    leftAlias: string
    leftColumn: string
    rightAlias: string
    rightColumn: string
    conditions: Array<{
      leftColumn: string
      operator?: string
      connector?: string
      rightColumn: string
      groupStart?: boolean
      groupEnd?: boolean
    }>
  }>
  graph: {
    layoutMode: string
    nodes: Array<{
      tableName: string
      alias: string
      x: number
      y: number
    }>
  }
}

export type BiDatasourceRelationshipDiagnosticStatus = 'pass' | 'warn' | 'block'

export interface BiDatasourceRelationshipDiagnosticRow {
  key: string
  label: string
  status: BiDatasourceRelationshipDiagnosticStatus
  statusLabel: string
  detail: string
}

export interface BiQueryExecutionPlanLike {
  datasetKey?: string | null
  sqlHash?: string | null
  parametersCount?: number | null
  steps?: string[] | null
}

export interface BiQueryExecutionPlanRow {
  label: string
  value: string
}

export interface BiQueryCancellationResultLike {
  sqlHash?: string | null
  cancelled?: boolean | null
  message?: string | null
}

export interface SelfServiceExtractionState {
  dimensions: string[]
  metrics: string[]
}

export type DashboardWidgetMoveDirection = 'up' | 'down' | 'left' | 'right'
export type DashboardWidgetAlignment = 'left' | 'right' | 'top' | 'bottom' | 'center' | 'middle'
export type DashboardDesignerKeyboardAction =
  | { type: 'move'; direction: DashboardWidgetMoveDirection }
  | { type: 'resize'; direction: DashboardWidgetMoveDirection }
  | { type: 'remove' }
  | { type: 'duplicate' }
  | { type: 'undo' }
  | { type: 'redo' }
export type DashboardSnapGuideOrientation = 'vertical' | 'horizontal'
export type DashboardSnapGuideEdge = 'left' | 'right' | 'center' | 'top' | 'bottom' | 'middle'
export type DashboardLayoutMode = 'desktop' | 'tablet' | 'mobile'

export const DASHBOARD_GRID_COLUMNS = 20
export const DASHBOARD_TABLET_GRID_COLUMNS = 12
export const DASHBOARD_MOBILE_GRID_COLUMNS = 1
export const DASHBOARD_GRID_MIN_WIDTH = 2
export const DASHBOARD_GRID_MIN_HEIGHT = 2
export const DASHBOARD_GRID_RESIZE_COLUMN_WIDTH = 64
export const DASHBOARD_GRID_ROW_HEIGHT = 42

export interface DashboardWidgetPlacement {
  gridX: number
  gridY: number
  gridW: number
  gridH: number
}

export interface DashboardWidgetGridPlacement {
  gridColumn: string
  gridRow: string
  minHeight: number
}

export interface DashboardSnapGuide {
  orientation: DashboardSnapGuideOrientation
  position: number
  widgetKey: string
  edge: DashboardSnapGuideEdge
}

export interface DashboardWidgetSnapResult {
  placement: DashboardWidgetPlacement
  guides: DashboardSnapGuide[]
}

export interface DesignerSearchableItem {
  key?: string
  label?: string
  name?: string
  group?: string
  datasetKey?: string
  chartKey?: string
}

export const QUICKBI_DESIGNER_ACTIONS: BiDesignerToolbarAction[] = [
  { key: 'save', label: '保存' },
  { key: 'undo', label: '撤销' },
  { key: 'redo', label: '重做' },
  { key: 'preview', label: '预览' },
  { key: 'publish', label: '发布' },
  { key: 'clone', label: '复制' },
  { key: 'export', label: '导出' },
  { key: 'import', label: '导入' },
  { key: 'subscribe', label: '订阅' },
  { key: 'embed', label: '嵌入' },
  { key: 'archive', label: '归档' },
]

export const QUICKBI_CHART_PALETTE: BiDesignerPaletteItem[] = [
  { key: 'KPI_CARD', label: '指标卡', group: '基础图表' },
  { key: 'TABLE', label: '明细表', group: '基础图表' },
  { key: 'CROSS_TABLE', label: '交叉表', group: '基础图表' },
  { key: 'LINE', label: '折线图', group: '趋势分析' },
  { key: 'BAR', label: '柱状图', group: '趋势分析' },
  { key: 'PIE', label: '饼图', group: '占比分析' },
  { key: 'FUNNEL', label: '漏斗图', group: '转化分析' },
  { key: 'HEATMAP', label: '热力图', group: '分布分析' },
]

export const QUICKBI_CONTROL_PALETTE: BiDesignerPaletteItem[] = [
  { key: 'DATE_RANGE', label: '日期区间', group: '查询控件' },
  { key: 'SEARCH_SELECT', label: '搜索选择', group: '查询控件' },
  { key: 'ENUM_MULTI_SELECT', label: '枚举多选', group: '查询控件' },
  { key: 'NUMBER_RANGE', label: '数值区间', group: '查询控件' },
  { key: 'RICH_TEXT', label: '富文本', group: '布局组件' },
  { key: 'TAB_CONTAINER', label: 'Tab 容器', group: '布局组件' },
  { key: 'IFRAME', label: '内嵌页面', group: '布局组件' },
]

export const DEFAULT_DASHBOARD_PRESETS: BiDashboardPresetLike[] = [
  {
    dashboardKey: 'canvas-effect',
    title: '画布效果分析',
    description: '面向营销画布执行、成功率、趋势和排行的预置分析看板。',
    datasetKey: 'canvas_daily_stats',
    widgets: [
      {
        widgetKey: 'kpi-total-executions',
        title: '执行次数',
        chartType: 'KPI_CARD',
        dimensions: [],
        metrics: ['total_executions'],
        gridX: 0,
        gridY: 0,
        gridW: 6,
        gridH: 3,
        stylePreset: 'emphasis',
      },
      {
        widgetKey: 'kpi-success-rate',
        title: '执行成功率',
        chartType: 'KPI_CARD',
        dimensions: [],
        metrics: ['success_rate'],
        gridX: 6,
        gridY: 0,
        gridW: 6,
        gridH: 3,
        stylePreset: 'health',
      },
      {
        widgetKey: 'trend-executions',
        title: '执行趋势',
        chartType: 'LINE',
        dimensions: ['stat_date'],
        metrics: ['total_executions', 'success_count', 'fail_count'],
        gridX: 0,
        gridY: 3,
        gridW: 12,
        gridH: 6,
        stylePreset: 'time-series',
      },
      {
        widgetKey: 'rank-canvas',
        title: '画布排行',
        chartType: 'BAR',
        dimensions: ['canvas_name'],
        metrics: ['total_executions', 'success_rate'],
        gridX: 12,
        gridY: 3,
        gridW: 8,
        gridH: 6,
        stylePreset: 'ranking',
      },
      {
        widgetKey: 'detail-canvas',
        title: '画布明细',
        chartType: 'TABLE',
        dimensions: ['stat_date', 'canvas_name', 'trigger_type'],
        metrics: ['total_executions', 'success_count', 'fail_count', 'unique_users', 'avg_duration_ms'],
        gridX: 0,
        gridY: 9,
        gridW: 20,
        gridH: 7,
        stylePreset: 'detail',
      },
    ],
    filters: [
      { filterKey: 'filter-stat-date', fieldKey: 'stat_date', label: '统计日期', controlType: 'DATE_RANGE', required: true, defaultValue: 'LAST_7_DAYS' },
      {
        filterKey: 'filter-canvas',
        fieldKey: 'canvas_name',
        label: '画布名称',
        controlType: 'SEARCH_SELECT',
        required: false,
        cascade: { parentFilterKeys: ['filter-stat-date'], mode: 'SAME_SOURCE' },
      },
      {
        filterKey: 'filter-trigger-type',
        fieldKey: 'trigger_type',
        label: '触发方式',
        controlType: 'ENUM_MULTI_SELECT',
        required: false,
        cascade: { parentFilterKeys: ['filter-stat-date', 'filter-canvas'], mode: 'SAME_SOURCE' },
      },
    ],
    interactions: [
      { interactionKey: 'linkage-trend-to-detail', sourceWidgetKey: 'trend-executions', targetWidgetKey: 'detail-canvas', interactionType: 'FILTER_LINKAGE', fieldKey: 'stat_date' },
      { interactionKey: 'drill-rank-canvas', sourceWidgetKey: 'rank-canvas', targetWidgetKey: 'detail-canvas', interactionType: 'DRILL_DOWN', fieldKey: 'canvas_name', target: 'canvas_name' },
      { interactionKey: 'open-canvas-stats', sourceWidgetKey: 'detail-canvas', interactionType: 'HYPERLINK', fieldKey: 'canvas_id', target: '/canvas/{canvas_id}/stats' },
    ],
    subscriptionChannels: ['EMAIL', 'LARK', 'WECOM', 'WEBHOOK'],
    embedScopes: ['INTERNAL_CANVAS', 'EXTERNAL_TICKET'],
  },
]

const METRIC_LABELS: Record<string, string> = {
  total_executions: '执行次数',
  success_count: '成功次数',
  fail_count: '失败次数',
  running_count: '运行中次数',
  unique_users: '去重用户数',
  avg_duration_ms: '平均耗时',
  success_rate: '执行成功率',
}

export const BI_WORKBENCH_SECTIONS: BiWorkbenchSection[] = [
  {
    key: 'overview',
    label: 'BI 总览',
    summary: '统一查看工作空间、数据集、仪表板、订阅和嵌入资产。',
    capability: 'Workspace',
  },
  {
    key: 'data',
    label: '数据源',
    summary: '管理 Doris、MySQL、文件和 API 数据源，承接凭证、连通性和表结构同步。',
    capability: 'Data Source',
  },
  {
    key: 'dataset',
    label: '数据集',
    summary: '沉淀字段、维度、度量、计算字段、行列权限和指标口径。',
    capability: 'Dataset',
  },
  {
    key: 'chart',
    label: '图表',
    summary: '通过字段拖拽生成指标卡、表格、折线、柱状、饼图、漏斗和热力图。',
    capability: 'Chart',
  },
  {
    key: 'dashboard',
    label: '仪表板',
    summary: '以栅格布局组合图表、查询控件、联动、钻取和跳转。',
    capability: 'Dashboard',
  },
  {
    key: 'portal',
    label: '数据门户',
    summary: '把仪表板、电子表格、大屏、自助取数和外链组织成角色化入口。',
    capability: 'Portal',
  },
  {
    key: 'self-service',
    label: '自助取数',
    summary: '业务用户按授权数据集选择维度指标，预览并导出明细或聚合数据。',
    capability: 'Self Service',
  },
  {
    key: 'subscription',
    label: '订阅告警',
    summary: '配置日报、周报、阈值告警和飞书/企微/邮件/Webhook 推送。',
    capability: 'Subscription',
  },
  {
    key: 'embed',
    label: '嵌入分析',
    summary: '生成内部嵌入和外部短期 token，把报表放入 Canvas 或第三方系统。',
    capability: 'Embed',
  },
  {
    key: 'ai',
    label: '智能小Q',
    summary: '围绕问数、解读、报告、搭建和洞察 Agent 建立 AI 分析入口。',
    capability: 'AI',
  },
]

export const DEFAULT_MARKETING_DATASETS: MarketingDatasetPreset[] = [
  {
    key: 'canvas_daily_stats',
    label: '画布每日统计',
    source: 'Doris: canvas_dws.canvas_daily_stats',
    preset: true,
    metrics: ['执行次数', '成功次数', '失败次数', '去重用户数', '平均耗时', '执行成功率'],
  },
  {
    key: 'node_daily_stats',
    label: '节点每日漏斗',
    source: 'Doris: canvas_dws.node_daily_stats',
    preset: true,
    metrics: ['进入次数', '成功次数', '失败次数', '跳过次数', '节点成功率'],
  },
  {
    key: 'channel_performance',
    label: '渠道效果分析',
    source: 'Canvas delivery receipts',
    preset: true,
    metrics: ['发送量', '送达率', '打开率', '点击率', '转化率', '成本'],
  },
  {
    key: 'conversion_attribution',
    label: '转化归因',
    source: 'Canvas attribution events',
    preset: true,
    metrics: ['转化数', '转化率', '收入', 'ROI'],
  },
]

export function getBiSection(key: string | null | undefined): BiWorkbenchSection {
  return BI_WORKBENCH_SECTIONS.find(section => section.key === key) ?? BI_WORKBENCH_SECTIONS[0]
}

/** 生成从营销画布跳转到 BI 效果仪表盘的入口地址。 */
export function canvasBiEntrypoint(canvasId: number | string): string {
  return `/bi?dashboard=canvas-effect&canvasId=${canvasId}`
}

/** 根据仪表盘预设和运行态参数组装嵌入 ticket 申请载荷。 */
export function buildEmbedTicketRequest(
  preset: BiDashboardPresetLike,
  canvasId?: string | null,
  scope = 'INTERNAL_CANVAS',
  runtimeParameters?: BiDashboardRuntimeParameters | null,
): {
  resourceType: string
  resourceKey: string
  scope: string
  filters: Record<string, string>
  parameters?: Record<string, string>
  ttlSeconds: number
  maxAccessCount: number
} {
  // 将全局参数和控件值固化进 ticket，避免嵌入页依赖外部 URL 状态。
  const parameters = buildEmbedTicketParameters(preset, runtimeParameters)
  return {
    resourceType: 'DASHBOARD',
    resourceKey: preset.dashboardKey,
    scope,
    filters: buildEmbedTicketFilters(preset, canvasId, runtimeParameters),
    ...(Object.keys(parameters).length ? { parameters } : {}),
    ttlSeconds: scope === 'EXTERNAL_TICKET' ? 900 : 600,
    maxAccessCount: Math.min(100, Math.max(4, preset.widgets.length + 3)),
  }
}

/** 预览即将固化到嵌入 ticket 的资源、运行态过滤和全局参数。 */
export function buildEmbedTicketPreviewRows(
  preset: BiDashboardPresetLike,
  canvasId?: string | null,
  scope = 'INTERNAL_CANVAS',
  runtimeParameters?: BiDashboardRuntimeParameters | null,
): BiEmbedTicketPreviewRow[] {
  const request = buildEmbedTicketRequest(preset, canvasId, scope, runtimeParameters)
  return [
    { key: 'resource', label: '资源', value: `${request.resourceType} / ${request.resourceKey}` },
    { key: 'scope', label: '范围', value: request.scope },
    { key: 'ttl', label: '有效期', value: `${request.ttlSeconds} 秒` },
    ...Object.entries(request.filters).map(([key, value]) => ({
      key: `filter:${key}`,
      label: `过滤 ${key}`,
      value,
    })),
    ...Object.entries(request.parameters ?? {}).map(([key, value]) => ({
      key: `parameter:${key}`,
      label: `参数 ${key}`,
      value,
    })),
  ]
}

/** 将单个仪表盘组件转换为后端 BI 查询请求。 */
export function buildWidgetQueryRequest(
  preset: BiDashboardPresetLike,
  widget: BiDashboardWidgetPreset,
  canvasId?: string | null,
  runtimeParametersOrLimit?: BiDashboardRuntimeParameters | number | null,
  limit = 500,
): BiQueryRequest {
  // 兼容历史调用：第四个参数既可能是运行态参数，也可能直接是 limit。
  const runtimeParameters = typeof runtimeParametersOrLimit === 'number' ? null : runtimeParametersOrLimit
  const effectiveLimit = typeof runtimeParametersOrLimit === 'number' ? runtimeParametersOrLimit : limit
  // 合并画布过滤、控件参数和组件作用域，保证查询只取当前业务上下文。
  const filters = buildDashboardRuntimeFilters(preset, canvasId, runtimeParameters, widget.widgetKey)
  return {
    datasetKey: preset.datasetKey,
    dashboardKey: preset.dashboardKey,
    dimensions: widget.dimensions,
    metrics: widget.metrics,
    filters,
    sorts: widget.dimensions.includes('stat_date')
      ? [{ field: 'stat_date', direction: 'ASC' as const }]
      : [],
    limit: effectiveLimit,
  }
}

/** 根据组件交互规则和点击行数据生成钻取、联动或超链接目标。 */
export function buildDashboardInteractionTarget(
  preset: BiDashboardPresetLike,
  interaction: BiDashboardInteractionPreset,
  row?: Record<string, unknown> | null,
  options: {
    canvasId?: string | null
    runtimeParameters?: BiDashboardRuntimeParameters | null
  } = {},
): string | null {
  if (interaction.interactionType === 'HYPERLINK') {
    // 超链接允许使用行字段模板，并附加当前画布和运行态参数上下文。
    const target = interaction.target?.trim()
    if (!target) return null
    const templatedTarget = replaceInteractionTemplate(target, row)
    if (!templatedTarget) return null
    const params = new URLSearchParams()
    appendInteractionParameter(params, 'canvasId', options.canvasId)
    appendRuntimeParameters(params, options.runtimeParameters)
    return appendQueryString(templatedTarget, params)
  }

  if (interaction.interactionType !== 'DRILL_DOWN' && interaction.interactionType !== 'FILTER_LINKAGE') {
    return null
  }

  const params = new URLSearchParams()
  // 仪表盘内钻取统一回到 BI 页面，由 URL 参数恢复过滤状态。
  params.set('dashboard', preset.dashboardKey)
  appendInteractionParameter(params, 'canvasId', options.canvasId)
  appendInteractionParameter(params, 'widget', interaction.targetWidgetKey)
  appendRuntimeParameters(params, options.runtimeParameters)

  const filterKey = interactionFilterParameterKey(preset, interaction)
  const value = interactionRowValue(row, interaction.fieldKey)
  appendInteractionParameter(params, filterKey, value)
  return `/bi?${params.toString()}`
}

/** 构造查询控件候选项请求，包含父级级联控件的过滤条件。 */
export function buildDashboardControlOptionQuery(
  preset: BiDashboardPresetLike,
  filterKey: string,
  runtimeParameters?: BiDashboardRuntimeParameters | null,
  canvasId?: string | null,
  limit = 100,
): BiQueryRequest {
  const targetFilter = preset.filters.find(filter => filter.filterKey === filterKey || filter.fieldKey === filterKey)
  if (!targetFilter) {
    throw new Error(`Unknown dashboard filter: ${filterKey}`)
  }

  // 候选项查询只取控件字段，并把画布上下文作为基础过滤。
  const optionFieldKey = targetFilter.optionFieldKey ?? targetFilter.fieldKey
  const filters = canvasIdQueryFilter(canvasId)
    ? [canvasIdQueryFilter(canvasId) as BiQueryRequest['filters'][number]]
    : []
  const parentKeys = new Set(targetFilter.cascade?.parentFilterKeys ?? [])
  for (const parentKey of parentKeys) {
    const parentFilter = preset.filters.find(filter => filter.filterKey === parentKey || filter.fieldKey === parentKey)
    if (!parentFilter || parentFilter.filterKey === targetFilter.filterKey || parentFilter.fieldKey === targetFilter.fieldKey) {
      continue
    }
    const value = dashboardRuntimeValueForFilter(preset, runtimeParameters, parentFilter)
    // 级联父控件字段可能需要映射到候选项数据集中的字段。
    const parentOptionField = mappedCascadeParentField(targetFilter.cascade, parentFilter)
    const queryFilter = toRuntimeQueryFilter({ ...parentFilter, fieldKey: parentOptionField }, value)
    if (queryFilter) filters.push(queryFilter)
  }

  return {
    datasetKey: targetFilter.optionDatasetKey ?? preset.datasetKey,
    dashboardKey: preset.dashboardKey,
    dimensions: [optionFieldKey],
    metrics: [],
    filters,
    sorts: [{ field: optionFieldKey, direction: 'ASC' }],
    limit,
  }
}

/** 在自助分析字段槽中添加字段，并保证维度和指标互斥。 */
export function dropSelfServiceExtractionField(
  state: SelfServiceExtractionState,
  role: 'DIMENSION' | 'METRIC',
  fieldKey: string,
): SelfServiceExtractionState {
  // 先归一化已有状态，避免空字段或重复拖拽污染查询请求。
  const normalized = fieldKey.trim()
  if (!normalized) return normalizeExtractionState(state)
  const current = normalizeExtractionState(state)
  if (role === 'DIMENSION') {
    return {
      dimensions: appendUnique(current.dimensions, normalized),
      metrics: current.metrics.filter(metric => metric !== normalized),
    }
  }
  return {
    dimensions: current.dimensions.filter(dimension => dimension !== normalized),
    metrics: appendUnique(current.metrics, normalized),
  }
}

/** 从自助分析字段槽中移除指定字段。 */
export function removeSelfServiceExtractionField(
  state: SelfServiceExtractionState,
  role: 'DIMENSION' | 'METRIC',
  fieldKey: string,
): SelfServiceExtractionState {
  const normalized = fieldKey.trim()
  const current = normalizeExtractionState(state)
  return role === 'DIMENSION'
    ? { ...current, dimensions: current.dimensions.filter(field => field !== normalized) }
    : { ...current, metrics: current.metrics.filter(field => field !== normalized) }
}

/** 将自助分析字段选择转换为 BI 查询请求。 */
export function buildSelfServiceExtractionQuery(
  preset: BiDashboardPresetLike,
  state: SelfServiceExtractionState,
  canvasId?: string | null,
  runtimeParametersOrLimit?: BiDashboardRuntimeParameters | number | null,
  limit = 500,
): BiQueryRequest {
  // 自助分析与仪表盘组件共用运行态过滤，保证预览结果与仪表盘上下文一致。
  const current = normalizeExtractionState(state)
  const runtimeParameters = typeof runtimeParametersOrLimit === 'number' ? null : runtimeParametersOrLimit
  const effectiveLimit = typeof runtimeParametersOrLimit === 'number' ? runtimeParametersOrLimit : limit
  const filters = buildDashboardRuntimeFilters(preset, canvasId, runtimeParameters)
  return {
    datasetKey: preset.datasetKey,
    dashboardKey: preset.dashboardKey,
    dimensions: current.dimensions,
    metrics: current.metrics,
    filters,
    sorts: current.dimensions.includes('stat_date')
      ? [{ field: 'stat_date', direction: 'ASC' as const }]
      : [],
    limit: effectiveLimit,
  }
}

/** 从 URLSearchParams 中提取仪表盘运行态参数，兼容 filterKey、fieldKey 和全局别名。 */
export function dashboardRuntimeParametersFromSearchParams(
  preset: BiDashboardPresetLike,
  searchParams: URLSearchParams,
): BiDashboardRuntimeParameters {
  const result: BiDashboardRuntimeParameters = {}
  for (const filter of preset.filters) {
    // 控件 key 优先，字段 key 作为历史或外部链接兼容入口。
    const valueByFilterKey = searchParamValue(searchParams, filter.filterKey)
    if (valueByFilterKey != null) {
      result[filter.filterKey] = dashboardRuntimeControlParameterValue(filter, searchParamRuntimeRawValue(valueByFilterKey))
      continue
    }
    const valueByFieldKey = searchParamValue(searchParams, filter.fieldKey)
    if (valueByFieldKey != null) {
      result[filter.fieldKey] = dashboardRuntimeControlParameterValue(filter, searchParamRuntimeRawValue(valueByFieldKey))
    }
  }
  for (const parameter of dashboardGlobalParameters(preset)) {
    // 全局参数可能存在多个别名，需要镜像到所有候选 key。
    const value = searchParamValueByKeys(searchParams, globalParameterCandidateKeys(parameter))
    if (value != null) {
      mirrorGlobalParameterValue(result, parameter, value)
    }
  }
  return result
}

export function dashboardRuntimeParametersFromEmbedPayload(
  preset: BiDashboardPresetLike,
  payload: BiEmbedTicketPayload,
  today?: Date,
): BiDashboardRuntimeParameters
export function dashboardRuntimeParametersFromEmbedPayload(
  preset: BiDashboardPresetLike,
  payload: BiEmbedTicketPayload,
  rememberedParameters?: BiDashboardRuntimeParameters | null,
  today?: Date,
): BiDashboardRuntimeParameters
export function dashboardRuntimeParametersFromEmbedPayload(
  preset: BiDashboardPresetLike,
  payload: BiEmbedTicketPayload,
  rememberedParametersOrToday?: BiDashboardRuntimeParameters | Date | null,
  today = new Date(),
): BiDashboardRuntimeParameters {
  // 兼容旧签名：第三个参数可传 today，也可传记住的运行态参数。
  const rememberedParameters = rememberedParametersOrToday instanceof Date
    ? null
    : rememberedParametersOrToday ?? null
  const effectiveToday = rememberedParametersOrToday instanceof Date ? rememberedParametersOrToday : today
  return resolveDashboardRuntimeParameters(preset, embedTicketPayloadSearchParams(payload), rememberedParameters, effectiveToday)
}

/** 按 URL 显式值、记住条件、默认值、已有全局值的优先级解析运行态参数。 */
export function resolveDashboardRuntimeParameters(
  preset: BiDashboardPresetLike,
  searchParams: URLSearchParams,
  rememberedParameters?: BiDashboardRuntimeParameters | null,
  today = new Date(),
): BiDashboardRuntimeParameters {
  const explicitParameters = dashboardRuntimeParametersFromSearchParams(preset, searchParams)
  const result: BiDashboardRuntimeParameters = {}
  for (const filter of preset.filters) {
    // 单个控件值优先从 URL 恢复，其次使用用户记住条件，最后落默认值。
    const explicitValue = explicitParameters[filter.filterKey] ?? explicitParameters[filter.fieldKey]
    if (explicitValue != null) {
      result[filter.filterKey] = explicitValue
      continue
    }
    const rememberedValue = rememberedParameters?.[filter.filterKey] ?? rememberedParameters?.[filter.fieldKey]
    if (rememberedValue != null) {
      result[filter.filterKey] = rememberedValue
      continue
    }
    const defaultValue = defaultRuntimeParameterValue(filter.defaultValue, today)
    if (defaultValue != null) {
      result[filter.filterKey] = defaultValue
    }
  }
  for (const parameter of dashboardGlobalParameters(preset)) {
    // 全局参数需要与绑定控件双向镜像，避免别名和 filterKey 出现分裂。
    const explicitValue = dashboardRuntimeValueForGlobalParameter(parameter, explicitParameters)
    const rememberedValue = dashboardRuntimeValueForGlobalParameter(parameter, rememberedParameters)
    const existingFilterValue = dashboardRuntimeValueForGlobalParameter(parameter, result)
    const defaultValue = defaultRuntimeParameterValue(parameter.defaultValue, today)
    const value = explicitValue ?? rememberedValue ?? defaultValue ?? existingFilterValue
    if (value != null) {
      mirrorGlobalParameterValue(result, parameter, value)
    }
  }
  return result
}

export function dashboardDefaultRuntimeParameters(
  preset: BiDashboardPresetLike,
  today = new Date(),
): BiDashboardRuntimeParameters {
  return resolveDashboardRuntimeParameters(preset, new URLSearchParams(), null, today)
}

export function dashboardRuntimeStateRows(
  preset: BiDashboardPresetLike,
  searchParams: URLSearchParams,
  rememberedParameters?: BiDashboardRuntimeParameters | null,
  clearedParameterKeys: string[] = [],
  today = new Date(),
): BiDashboardRuntimeStateRow[] {
  // 被清除的参数要先从有效 URL 中剔除，同时保留原值用于展示“已清除”来源。
  const clearedAliases = new Set(clearedParameterKeys.flatMap(key => dashboardRuntimeSearchParamAliases(preset, key)))
  let effectiveSearchParams = new URLSearchParams(searchParams)
  for (const key of clearedParameterKeys) {
    effectiveSearchParams = stripDashboardRuntimeSearchParam(preset, effectiveSearchParams, key)
  }
  const explicitParameters = dashboardRuntimeParametersFromSearchParams(preset, effectiveSearchParams)
  const originalExplicitParameters = dashboardRuntimeParametersFromSearchParams(preset, searchParams)
  return preset.filters.map(filter => {
    // UI 行聚合当前控件最终生效值及其来源，便于用户判断覆盖关系。
    const explicitValue = dashboardRuntimeValueForFilter(preset, explicitParameters, filter)
    const rememberedValue = dashboardRuntimeValueForFilter(preset, rememberedParameters, filter)
    const defaultValue = defaultRuntimeParameterValue(filter.defaultValue, today)
    const originalExplicitValue = dashboardRuntimeValueForFilter(preset, originalExplicitParameters, filter)
    const cleared = dashboardRuntimeSearchParamAliases(preset, filter.filterKey).some(key => clearedAliases.has(key))
    let source: BiDashboardRuntimeStateSource = 'EMPTY'
    let value: BiDashboardRuntimeParameterValue = null
    if (explicitValue != null) {
      source = 'URL'
      value = explicitValue
    } else if (rememberedValue != null) {
      source = 'REMEMBERED'
      value = rememberedValue
    } else if (defaultValue != null) {
      source = 'DEFAULT'
      value = defaultValue
    } else if (cleared && originalExplicitValue != null) {
      source = 'CLEARED'
    }
    return {
      key: filter.filterKey,
      fieldKey: filter.fieldKey,
      label: filter.label,
      valueText: serializeInteractionValue(value) ?? '',
      source,
      sourceLabel: dashboardRuntimeStateSourceLabel(source),
      locked: dashboardRuntimeFilterLocked(preset, filter),
    }
  })
}

export function dashboardRuntimeSearchParamKeys(preset: BiDashboardPresetLike): string[] {
  return uniqueTrimmed([
    ...preset.filters.flatMap(filter => [filter.filterKey, filter.fieldKey]),
    ...dashboardGlobalParameters(preset).flatMap(globalParameterCandidateKeys),
  ])
}

export function stripDashboardRuntimeSearchParams(
  preset: BiDashboardPresetLike,
  searchParams: URLSearchParams,
): URLSearchParams {
  const next = new URLSearchParams(searchParams)
  for (const key of dashboardRuntimeSearchParamKeys(preset)) {
    next.delete(key)
  }
  return next
}

export function stripDashboardRuntimeSearchParam(
  preset: BiDashboardPresetLike,
  searchParams: URLSearchParams,
  key: string,
): URLSearchParams {
  const next = new URLSearchParams(searchParams)
  for (const runtimeKey of dashboardRuntimeSearchParamAliases(preset, key)) {
    next.delete(runtimeKey)
  }
  return next
}

function dashboardRuntimeSearchParamAliases(preset: BiDashboardPresetLike, key: string): string[] {
  const normalizedKey = trimValue(key)
  if (!normalizedKey) return []
  const filter = preset.filters.find(item => item.filterKey === normalizedKey || item.fieldKey === normalizedKey)
  if (filter) return uniqueTrimmed([filter.filterKey, filter.fieldKey])
  const globalParameter = dashboardGlobalParameters(preset)
    .find(item => globalParameterCandidateKeys(item).includes(normalizedKey))
  if (globalParameter) return globalParameterCandidateKeys(globalParameter)
  return [normalizedKey]
}

function dashboardRuntimeStateSourceLabel(source: BiDashboardRuntimeStateSource): string {
  if (source === 'URL') return 'URL覆盖'
  if (source === 'REMEMBERED') return '记住条件'
  if (source === 'DEFAULT') return '默认值'
  if (source === 'CLEARED') return '已清除'
  return '未设置'
}

export function dashboardRuntimeControlValue(
  runtimeParameters: BiDashboardRuntimeParameters | null | undefined,
  filter: BiDashboardFilterPreset,
): string {
  const value = runtimeParameters?.[filter.filterKey] ?? runtimeParameters?.[filter.fieldKey]
  return serializeInteractionValue(value) ?? ''
}

export function dashboardRuntimeFilterLocked(
  preset: BiDashboardPresetLike,
  filter: BiDashboardFilterPreset,
): boolean {
  return Boolean(dashboardGlobalParameterForFilter(preset, filter)?.locked)
}

export function updateDashboardRuntimeParameters(
  preset: BiDashboardPresetLike,
  currentParameters: BiDashboardRuntimeParameters | null | undefined,
  filterKey: string,
  rawValue: string,
): BiDashboardRuntimeParameters {
  const filter = preset.filters.find(item => item.filterKey === filterKey || item.fieldKey === filterKey)
  if (!filter) return { ...(currentParameters ?? {}) }
  const globalParameter = dashboardGlobalParameterForFilter(preset, filter)
  // 锁定的全局参数已有值时，普通控件不能覆盖它。
  if (globalParameter?.locked && dashboardRuntimeValueForGlobalParameter(globalParameter, currentParameters) != null) {
    return { ...(currentParameters ?? {}) }
  }
  const next = { ...(currentParameters ?? {}) }
  const value = dashboardRuntimeControlParameterValue(filter, rawValue)
  // 删除 fieldKey 旧值，统一以 filterKey 作为当前控件的主存储 key。
  delete next[filter.fieldKey]
  if (value == null) {
    delete next[filter.filterKey]
    if (globalParameter) deleteGlobalParameterValue(next, globalParameter)
    return next
  }
  next[filter.filterKey] = value
  if (globalParameter) mirrorGlobalParameterValue(next, globalParameter, value)
  return next
}

export function buildDashboardRuntimeStateCommand(
  preset: BiDashboardPresetLike,
  runtimeParameters: BiDashboardRuntimeParameters | null | undefined,
): BiDashboardRuntimeStateCommand {
  const parameters: Record<string, unknown> = {}
  for (const filter of preset.filters) {
    const value = persistedRuntimeParameterValue(dashboardRuntimeValueForFilter(preset, runtimeParameters, filter))
    if (value == null) continue
    parameters[filter.filterKey] = value
    const globalParameter = dashboardGlobalParameterForFilter(preset, filter)
    if (globalParameter?.parameterKey?.trim()) {
      parameters[globalParameter.parameterKey.trim()] = value
    }
  }
  for (const parameter of dashboardGlobalParameters(preset)) {
    const parameterKey = parameter.parameterKey.trim()
    if (!parameterKey || parameters[parameterKey] != null) continue
    const value = persistedRuntimeParameterValue(dashboardRuntimeValueForGlobalParameter(parameter, runtimeParameters))
    if (value != null) parameters[parameterKey] = value
  }
  return { parameters }
}

function buildDashboardRuntimeFilters(
  preset: BiDashboardPresetLike,
  canvasId?: string | null,
  runtimeParameters?: BiDashboardRuntimeParameters | null,
  widgetKey?: string | null,
): BiQueryRequest['filters'] {
  const canvasFilter = canvasIdQueryFilter(canvasId)
  const filters: BiQueryRequest['filters'] = canvasFilter ? [canvasFilter] : []
  for (const filter of preset.filters) {
    if (!filterAppliesToWidget(filter, widgetKey)) continue
    const value = dashboardRuntimeValueForFilter(preset, runtimeParameters, filter)
    const queryFilter = toRuntimeQueryFilter(filter, value)
    if (queryFilter) filters.push(queryFilter)
  }
  return filters
}

function canvasIdQueryFilter(canvasId?: string | null): BiQueryRequest['filters'][number] | null {
  return canvasId
    ? {
        field: 'canvas_id',
        operator: 'EQ',
        value: Number.isNaN(Number(canvasId)) ? canvasId : Number(canvasId),
      }
    : null
}

function buildEmbedTicketFilters(
  preset: BiDashboardPresetLike,
  canvasId?: string | null,
  runtimeParameters?: BiDashboardRuntimeParameters | null,
): Record<string, string> {
  const filters: Record<string, string> = {}
  if (canvasId) filters.canvasId = canvasId
  const globalParameterKeys = new Set(dashboardGlobalParameters(preset).flatMap(globalParameterRuntimeOnlyKeys))
  Object.entries(runtimeParameters ?? {}).forEach(([key, value]) => {
    if (globalParameterKeys.has(key)) return
    const serialized = serializeEmbedFilterValue(value)
    if (serialized) filters[key] = serialized
  })
  for (const filter of preset.filters) {
    const value = dashboardRuntimeValueForFilter(preset, runtimeParameters, filter)
    const serialized = serializeEmbedFilterValue(value)
    if (serialized) filters[filter.filterKey] = serialized
  }
  return filters
}

function buildEmbedTicketParameters(
  preset: BiDashboardPresetLike,
  runtimeParameters?: BiDashboardRuntimeParameters | null,
): Record<string, string> {
  const parameters: Record<string, string> = {}
  for (const parameter of dashboardGlobalParameters(preset)) {
    const serialized = serializeEmbedFilterValue(dashboardRuntimeValueForGlobalParameter(parameter, runtimeParameters))
    if (serialized) parameters[parameter.parameterKey] = serialized
  }
  return parameters
}

function embedTicketPayloadSearchParams(payload: BiEmbedTicketPayload): URLSearchParams {
  const searchParams = new URLSearchParams()
  Object.entries(payload.filters ?? {}).forEach(([key, value]) => {
    if (key && value != null) searchParams.set(key, String(value))
  })
  Object.entries(payload.parameters ?? {}).forEach(([key, value]) => {
    if (key && value != null) searchParams.set(key, String(value))
  })
  return searchParams
}

function serializeEmbedFilterValue(value: BiDashboardRuntimeParameterValue): string | null {
  if (value == null) return null
  if (Array.isArray(value)) {
    const serialized = value
      .flatMap(item => serializeEmbedFilterValue(item))
      .filter((item): item is string => Boolean(item))
    return serialized.length ? serialized.join(',') : null
  }
  const serialized = String(value).trim()
  return serialized || null
}

function dashboardGlobalParameters(preset: BiDashboardPresetLike): BiDashboardGlobalParameterPreset[] {
  return (preset.globalParameters ?? [])
    .filter(parameter => Boolean(parameter?.parameterKey?.trim()))
}

function dashboardGlobalParameterForFilter(
  preset: BiDashboardPresetLike,
  filter: BiDashboardFilterPreset,
): BiDashboardGlobalParameterPreset | null {
  return dashboardGlobalParameters(preset).find(parameter =>
    parameter.filterKey === filter.filterKey
    || parameter.filterKey === filter.fieldKey
    || parameter.fieldKey === filter.fieldKey
    || parameter.fieldKey === filter.filterKey) ?? null
}

function dashboardRuntimeValueForFilter(
  preset: BiDashboardPresetLike,
  runtimeParameters: BiDashboardRuntimeParameters | null | undefined,
  filter: BiDashboardFilterPreset,
): BiDashboardRuntimeParameterValue {
  const directValue = runtimeParameters?.[filter.filterKey] ?? runtimeParameters?.[filter.fieldKey]
  if (directValue != null) return directValue
  const parameter = dashboardGlobalParameterForFilter(preset, filter)
  return parameter ? dashboardRuntimeValueForGlobalParameter(parameter, runtimeParameters) : null
}

function dashboardRuntimeValueForGlobalParameter(
  parameter: BiDashboardGlobalParameterPreset,
  runtimeParameters: BiDashboardRuntimeParameters | null | undefined,
): BiDashboardRuntimeParameterValue {
  if (!runtimeParameters) return null
  for (const key of globalParameterCandidateKeys(parameter)) {
    const value = runtimeParameters[key]
    if (value != null) return value
  }
  return null
}

function mirrorGlobalParameterValue(
  runtimeParameters: BiDashboardRuntimeParameters,
  parameter: BiDashboardGlobalParameterPreset,
  value: BiDashboardRuntimeParameterValue,
) {
  const parameterKey = parameter.parameterKey.trim()
  if (!parameterKey) return
  runtimeParameters[parameterKey] = value
  const mappedKey = parameter.filterKey?.trim() || parameter.fieldKey?.trim()
  if (mappedKey) runtimeParameters[mappedKey] = value
}

function deleteGlobalParameterValue(
  runtimeParameters: BiDashboardRuntimeParameters,
  parameter: BiDashboardGlobalParameterPreset,
) {
  for (const key of globalParameterCandidateKeys(parameter)) {
    delete runtimeParameters[key]
  }
}

function globalParameterCandidateKeys(parameter: BiDashboardGlobalParameterPreset): string[] {
  return uniqueTrimmed([
    parameter.parameterKey,
    ...(parameter.aliases ?? []),
    parameter.filterKey ?? '',
    parameter.fieldKey ?? '',
  ])
}

function globalParameterRuntimeOnlyKeys(parameter: BiDashboardGlobalParameterPreset): string[] {
  return uniqueTrimmed([
    parameter.parameterKey,
    ...(parameter.aliases ?? []),
  ])
}

function searchParamValueByKeys(
  searchParams: URLSearchParams,
  keys: string[],
): string | string[] | null {
  for (const key of keys) {
    const value = searchParamValue(searchParams, key)
    if (value != null) return value
  }
  return null
}

function interactionFilterParameterKey(
  preset: BiDashboardPresetLike,
  interaction: BiDashboardInteractionPreset,
): string {
  const target = interaction.target?.trim()
  const filter = preset.filters.find(item =>
    item.filterKey === target
    || item.fieldKey === target
    || item.filterKey === interaction.fieldKey
    || item.fieldKey === interaction.fieldKey)
  return filter?.filterKey ?? interaction.fieldKey
}

function appendRuntimeParameters(
  params: URLSearchParams,
  runtimeParameters?: BiDashboardRuntimeParameters | null,
) {
  Object.entries(runtimeParameters ?? {}).forEach(([key, value]) => {
    appendInteractionParameter(params, key, value)
  })
}

function appendInteractionParameter(
  params: URLSearchParams,
  key: string | null | undefined,
  value: unknown,
) {
  const parameterKey = key?.trim()
  const serialized = serializeInteractionValue(value)
  if (parameterKey && serialized) params.set(parameterKey, serialized)
}

function dashboardRuntimeControlParameterValue(
  filter: BiDashboardFilterPreset,
  rawValue: string,
): BiDashboardRuntimeParameterValue {
  const trimmed = rawValue.trim()
  if (!trimmed) return null
  if (filter.controlType === 'DATE_RANGE' || filter.controlType === 'ENUM_MULTI_SELECT') {
    const values = trimmed
      .split(',')
      .map(item => item.trim())
      .filter(Boolean)
    return values.length ? values : null
  }
  return trimmed
}

function persistedRuntimeParameterValue(value: BiDashboardRuntimeParameterValue): unknown {
  if (value == null) return null
  if (Array.isArray(value)) {
    const items = value
      .map(item => persistedRuntimeScalarValue(item))
      .filter((item): item is string | number | boolean => item != null)
    return items.length > 0 ? items : null
  }
  return persistedRuntimeScalarValue(value)
}

function persistedRuntimeScalarValue(value: string | number | boolean | null | undefined): string | number | boolean | null {
  if (value == null) return null
  if (typeof value === 'string') {
    const trimmed = value.trim()
    return trimmed || null
  }
  return value
}

function replaceInteractionTemplate(
  target: string,
  row?: Record<string, unknown> | null,
): string | null {
  let missingValue = false
  const replaced = target.replace(/\{([^}]+)\}/g, (_placeholder, key: string) => {
    const serialized = serializeInteractionValue(interactionRowValue(row, key.trim()))
    if (!serialized) {
      missingValue = true
      return ''
    }
    return encodeURIComponent(serialized)
  })
  return missingValue ? null : replaced
}

function interactionRowValue(
  row: Record<string, unknown> | null | undefined,
  key: string | null | undefined,
): unknown {
  if (!row || !key) return null
  return Object.prototype.hasOwnProperty.call(row, key) ? row[key] : null
}

function serializeInteractionValue(value: unknown): string | null {
  if (Array.isArray(value)) {
    return serializeEmbedFilterValue(value.map(item => serializeInteractionValue(item) ?? ''))
  }
  if (typeof value === 'string' || typeof value === 'number' || typeof value === 'boolean') {
    return serializeEmbedFilterValue(value)
  }
  return null
}

function appendQueryString(target: string, params: URLSearchParams): string {
  const query = params.toString()
  if (!query) return target
  const [withoutHash, hash] = target.split('#', 2)
  const separator = withoutHash.includes('?') ? '&' : '?'
  return `${withoutHash}${separator}${query}${hash ? `#${hash}` : ''}`
}

function mappedCascadeParentField(
  cascade: BiDashboardFilterCascadePreset | null | undefined,
  parentFilter: BiDashboardFilterPreset,
): string {
  return cascade?.parentFieldMapping?.[parentFilter.filterKey]
    ?? cascade?.parentFieldMapping?.[parentFilter.fieldKey]
    ?? parentFilter.fieldKey
}

function filterAppliesToWidget(filter: BiDashboardFilterPreset, widgetKey?: string | null): boolean {
  const targets = filter.targetWidgetKeys ?? []
  return targets.length === 0 || !widgetKey || targets.includes(widgetKey)
}

function toRuntimeQueryFilter(
  filter: BiDashboardFilterPreset,
  value: BiDashboardRuntimeParameterValue,
): BiQueryRequest['filters'][number] | null {
  const values = normalizeRuntimeParameterValues(value)
  if (values.length === 0) return null
  if (filter.controlType === 'DATE_RANGE') {
    if (values.length < 2) return null
    return { field: filter.fieldKey, operator: 'BETWEEN', value: [String(values[0]), String(values[1])] }
  }
  if (filter.controlType === 'NUMBER_RANGE') {
    if (values.length < 2) return null
    return { field: filter.fieldKey, operator: 'BETWEEN', value: [numericRuntimeValue(values[0]), numericRuntimeValue(values[1])] }
  }
  if (filter.controlType === 'ENUM_MULTI_SELECT') {
    return { field: filter.fieldKey, operator: 'IN', value: values.map(value => String(value)) }
  }
  return { field: filter.fieldKey, operator: 'EQ', value: numericStringOrRaw(values[0]) }
}

function normalizeRuntimeParameterValues(value: BiDashboardRuntimeParameterValue): Array<string | number | boolean> {
  if (value == null) return []
  if (Array.isArray(value)) {
    return value.flatMap(item => normalizeRuntimeParameterValues(item))
  }
  if (typeof value === 'string') {
    return value
      .split(',')
      .map(item => item.trim())
      .filter(Boolean)
  }
  return [value]
}

function numericRuntimeValue(value: string | number | boolean): number | string | boolean {
  if (typeof value !== 'string') return value
  const numeric = Number(value)
  return Number.isNaN(numeric) ? value : numeric
}

function numericStringOrRaw(value: string | number | boolean): string | number | boolean {
  return numericRuntimeValue(value)
}

function searchParamValue(
  searchParams: URLSearchParams,
  key: string,
): string | string[] | null {
  const values = searchParams.getAll(key).filter(value => value !== '')
  if (values.length === 0) return null
  return values.length === 1 ? values[0] : values
}

function searchParamRuntimeRawValue(value: string | string[]): string {
  return Array.isArray(value) ? value.join(',') : value
}

function defaultRuntimeParameterValue(
  defaultValue: string | null | undefined,
  today: Date,
): BiDashboardRuntimeParameterValue {
  if (!defaultValue) return null
  const normalized = defaultValue.trim().toUpperCase()
  if (normalized === 'TODAY') return formatDate(today)
  if (normalized === 'YESTERDAY') return formatDate(addDays(today, -1))
  if (normalized === 'LAST_7_DAYS') return [formatDate(addDays(today, -6)), formatDate(today)]
  if (normalized === 'LAST_30_DAYS') return [formatDate(addDays(today, -29)), formatDate(today)]
  return defaultValue
}

function addDays(date: Date, days: number): Date {
  const next = new Date(Date.UTC(date.getUTCFullYear(), date.getUTCMonth(), date.getUTCDate()))
  next.setUTCDate(next.getUTCDate() + days)
  return next
}

function formatDate(date: Date): string {
  const year = date.getUTCFullYear()
  const month = String(date.getUTCMonth() + 1).padStart(2, '0')
  const day = String(date.getUTCDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

function normalizeExtractionState(state: SelfServiceExtractionState): SelfServiceExtractionState {
  return {
    dimensions: uniqueTrimmed(state.dimensions),
    metrics: uniqueTrimmed(state.metrics),
  }
}

function uniqueTrimmed(values: string[]): string[] {
  return values.reduce<string[]>((items, value) => appendUnique(items, value.trim()), [])
}

function appendUnique(values: string[], value: string): string[] {
  if (!value || values.includes(value)) return values
  return [...values, value]
}

export function toMarketingDatasetPreset(dataset: BiDatasetMetadataLike): MarketingDatasetPreset {
  const fallback = DEFAULT_MARKETING_DATASETS.find(preset => preset.key === dataset.datasetKey)
  return {
    key: dataset.datasetKey,
    label: fallback?.label ?? dataset.datasetKey,
    source: fallback?.source ?? 'BI semantic dataset',
    preset: fallback?.preset ?? false,
    metrics: dataset.metrics.map(metric => METRIC_LABELS[metric.metricKey] ?? metric.metricKey),
  }
}

export function chartLabel(chartType: string): string {
  return QUICKBI_CHART_PALETTE.find(chart => chart.key === chartType)?.label ?? chartType
}

/** 汇总图表被仪表盘、门户和订阅引用的影响范围，供发布/编辑前提示使用。 */
export function chartReferenceImpactSummary(
  chart: BiChartReferenceResourceLike | null | undefined,
  impact: BiChartReferenceImpactInputLike = {},
): string {
  // 先展示图表本身的数据集和字段规模，即使没有引用信息也能给出基础影响。
  const chartKey = chart?.chartKey?.trim()
  const datasetKey = chart?.datasetKey?.trim() || '-'
  const dimensionCount = chart?.query?.dimensions?.length ?? 0
  const metricCount = chart?.query?.metrics?.length ?? 0
  const parts = [`引用影响：数据集 ${datasetKey} · ${dimensionCount} 维度 · ${metricCount} 指标`]

  if (!chartKey) return parts[0]

  // 从本地或服务端影响数据中提取实际命中的资源引用。
  const dashboardReferences = (impact.dashboards ?? []).flatMap(dashboard =>
    (dashboard.widgets ?? [])
      .filter(widget => chartReferenceMatches(widget, chartKey))
      .map(widget => `${dashboard.title?.trim() || dashboard.dashboardKey}/${widget.title?.trim() || widget.widgetKey}`),
  )
  const portalReferences = (impact.portals ?? []).flatMap(portal =>
    (portal.menus ?? [])
      .filter(menu => chartReferenceMatches(menu, chartKey))
      .map(menu => `${portal.name?.trim() || portal.portalKey}/${menu.title?.trim() || menu.menuKey}`),
  )
  const subscriptionReferences = (impact.subscriptions ?? [])
    .filter(subscription => chartReferenceMatches(subscription, chartKey))
    .map(subscription => subscription.name?.trim() || subscription.subscriptionKey)

  if (dashboardReferences.length > 0) {
    parts.push(`仪表板 ${dashboardReferences.join('、')}`)
  }
  if (portalReferences.length > 0) {
    parts.push(`门户 ${portalReferences.join('、')}`)
  }
  if (subscriptionReferences.length > 0) {
    parts.push(`订阅 ${subscriptionReferences.join('、')}`)
  }

  return parts.join(' · ')
}

/** 将后端扁平化的图表引用影响结构转换为本地摘要函数可消费的形态。 */
export function chartReferenceImpactSummaryFromImpact(
  chart: BiChartReferenceResourceLike | null | undefined,
  impact: BiChartReferenceImpactViewLike | null | undefined,
): string {
  const chartKey = chart?.chartKey ?? ''
  if (!impact) {
    // 服务端影响接口失败时退化为只展示图表自身信息。
    return chartReferenceImpactSummary(chart)
  }
  return chartReferenceImpactSummary(chart, {
    dashboards: (impact.dashboards ?? []).map(reference => ({
      dashboardKey: reference.dashboardKey,
      title: reference.title,
      widgets: [{
        widgetKey: reference.widgetKey,
        title: reference.widgetTitle,
        chartKey,
      }],
    })),
    portals: (impact.portals ?? []).map(reference => ({
      portalKey: reference.portalKey,
      name: reference.name,
      menus: [{
        menuKey: reference.menuKey,
        title: reference.menuTitle,
        resourceType: 'CHART',
        resourceKey: chartKey,
      }],
    })),
    subscriptions: (impact.subscriptions ?? []).map(reference => ({
      subscriptionKey: reference.subscriptionKey,
      name: reference.name,
      resourceType: 'CHART',
      resourceKey: chartKey,
    })),
  })
}

/** 把图表设计器草稿转换为后端图表查询 patch。 */
export function chartQueryPatchFromDesigner(input: BiChartQueryDesignerInputLike): BiChartQueryPatchLike {
  // 对用户输入做最小归一化，确保请求中没有空字段和重复字段。
  const filterField = input.filterField?.trim() ?? ''
  const filterOperator = normalizeQueryFilterOperator(input.filterOperator)
  const filterValue = input.filterValue?.trim() ?? ''
  const sortField = input.sortField?.trim() ?? ''
  const sortDirection = input.sortDirection?.trim().toUpperCase() === 'ASC' ? 'ASC' : 'DESC'
  return {
    datasetKey: input.datasetKey.trim(),
    dimensions: uniqueTrimmed(input.selectedDimensions ?? []),
    metrics: uniqueTrimmed(input.selectedMetrics ?? []),
    // IN/BETWEEN 过滤值需要拆分为数组，其它运算符保持原始标量。
    filters: filterField && filterValue
      ? [{ field: filterField, operator: filterOperator, value: parseChartFilterValue(filterOperator, filterValue) }]
      : [],
    sorts: sortField ? [{ field: sortField, direction: sortDirection }] : [],
    limit: Math.max(1, Number(input.limit) || 100),
  }
}

/** 处理图表字段拖放后的维度/指标状态，保证字段唯一。 */
export function chartQueryFieldsAfterDrop(
  state: BiChartQueryFieldStateLike,
  role: 'DIMENSION' | 'METRIC',
  fieldKey: string,
): BiChartQueryFieldStateLike {
  // 空字段拖放直接忽略，避免生成不可执行的查询字段。
  const normalized = fieldKey.trim()
  if (!normalized) return state
  return role === 'DIMENSION'
    ? { ...state, dimensions: appendUnique(state.dimensions, normalized) }
    : { ...state, metrics: appendUnique(state.metrics, normalized) }
}

/** 将图表过滤输入文本转换为后端查询值。 */
function parseChartFilterValue(operator: string, value: string): unknown {
  if (operator === 'IN') {
    return uniqueTrimmed(value.split(','))
  }
  if (operator === 'BETWEEN') {
    return value.split(',').map(item => item.trim()).filter(Boolean).slice(0, 2)
  }
  return value
}

/** 归一化图表过滤运算符，避免任意字符串进入 BiQueryRequest。 */
function normalizeQueryFilterOperator(operator: string | null | undefined): BiQueryRequest['filters'][number]['operator'] {
  const normalized = operator?.trim().toUpperCase()
  if (
    normalized === 'NEQ'
    || normalized === 'GT'
    || normalized === 'GTE'
    || normalized === 'LT'
    || normalized === 'LTE'
    || normalized === 'IN'
    || normalized === 'BETWEEN'
    || normalized === 'CONTAINS'
  ) {
    return normalized
  }
  return 'EQ'
}

/** 判断仪表盘组件、门户菜单或订阅是否引用了指定图表。 */
function chartReferenceMatches(
  reference: { chartKey?: string | null, resourceType?: string | null, resourceKey?: string | null },
  chartKey: string,
): boolean {
  if (reference.chartKey?.trim() === chartKey) return true
  return reference.resourceType?.trim().toUpperCase() === 'CHART' && reference.resourceKey?.trim() === chartKey
}

export function controlLabel(controlType: string): string {
  return QUICKBI_CONTROL_PALETTE.find(control => control.key === controlType)?.label ?? controlType
}

export function interactionLabel(interactionType: string): string {
  const labels: Record<string, string> = {
    FILTER_LINKAGE: '联动',
    DRILL_DOWN: '钻取',
    HYPERLINK: '跳转',
  }
  return labels[interactionType] ?? interactionType
}

export function getDefaultDashboardPreset(dashboardKey = 'canvas-effect'): BiDashboardPresetLike {
  return DEFAULT_DASHBOARD_PRESETS.find(preset => preset.dashboardKey === dashboardKey) ?? DEFAULT_DASHBOARD_PRESETS[0]
}

export function buildDashboardCloneCommand(preset: BiDashboardPresetLike, suffix: string) {
  const baseKey = normalizeResourceKey(preset.dashboardKey || 'dashboard')
  const normalizedSuffix = normalizeResourceKey(suffix || 'copy')
  return {
    dashboardKey: `${baseKey}-${normalizedSuffix}`.slice(0, 128),
    title: `${preset.title} 副本`,
    description: preset.description,
  }
}

export function buildDashboardImportCommand<T extends BiDashboardPackageLike>(packagePayload: T, suffix: string) {
  const baseKey = normalizeResourceKey(packagePayload.preset.dashboardKey || 'dashboard')
  const normalizedSuffix = normalizeResourceKey(suffix || 'import')
  return {
    packagePayload,
    dashboardKey: `${baseKey}-${normalizedSuffix}`.slice(0, 128),
    title: `${packagePayload.preset.title} 导入副本`,
    overwrite: false,
  }
}

export function dashboardPackageFileName<T extends BiDashboardPackageLike>(packagePayload: T): string {
  const baseKey = normalizeResourceKey(packagePayload.preset.dashboardKey || 'dashboard')
  const version = Number.isFinite(packagePayload.sourceVersion) ? packagePayload.sourceVersion : 1
  return `${baseKey}-v${version}.bi-dashboard.json`
}

export function serializeDashboardPackage<T extends BiDashboardPackageLike>(packagePayload: T): string {
  return `${JSON.stringify(packagePayload, null, 2)}\n`
}

export function parseDashboardPackageText<T extends BiDashboardPackageLike = BiDashboardPackageLike>(text: string): T {
  let parsed: unknown
  try {
    parsed = JSON.parse(text)
  } catch (error) {
    throw new Error('dashboard package file is not valid JSON')
  }
  if (!parsed || typeof parsed !== 'object' || !('preset' in parsed)) {
    throw new Error('dashboard package preset is required')
  }
  if ('resourceType' in parsed && (parsed as { resourceType?: unknown }).resourceType !== 'DASHBOARD') {
    throw new Error('unsupported dashboard package resource type')
  }
  const preset = (parsed as { preset?: Partial<BiDashboardPresetLike> }).preset
  if (!preset?.dashboardKey || !preset.title) {
    throw new Error('dashboard package preset is required')
  }
  return parsed as T
}

export function getDashboardWidget(
  preset: BiDashboardPresetLike,
  widgetKey: string | null | undefined,
): BiDashboardWidgetPreset {
  return preset.widgets.find(widget => widget.widgetKey === widgetKey) ?? preset.widgets[0]
}

export function duplicateDashboardWidget(
  preset: BiDashboardPresetLike,
  widgetKey: string,
): BiDashboardPresetLike {
  const source = preset.widgets.find(widget => widget.widgetKey === widgetKey)
  if (!source) return preset
  const copyKey = uniqueWidgetKey(preset, `${source.widgetKey}-copy`)
  const copy: BiDashboardWidgetPreset = {
    ...source,
    widgetKey: copyKey,
    title: uniqueWidgetTitle(preset, `${source.title} 副本`),
    gridX: Math.min(Math.max(0, source.gridX + 1), Math.max(0, 20 - source.gridW)),
    gridY: source.gridY + 1,
  }
  return {
    ...preset,
    widgets: [...preset.widgets, copy],
  }
}

export function removeDashboardWidget(
  preset: BiDashboardPresetLike,
  widgetKey: string,
): BiDashboardPresetLike {
  if (preset.widgets.length <= 1 || !preset.widgets.some(widget => widget.widgetKey === widgetKey)) {
    return preset
  }
  return {
    ...preset,
    widgets: preset.widgets.filter(widget => widget.widgetKey !== widgetKey),
    interactions: preset.interactions.filter(interaction =>
      interaction.sourceWidgetKey !== widgetKey && interaction.targetWidgetKey !== widgetKey),
  }
}

function normalizeWidgetPlacement(widget: DashboardWidgetPlacement): DashboardWidgetPlacement {
  const gridW = clampInteger(widget.gridW, DASHBOARD_GRID_MIN_WIDTH, DASHBOARD_GRID_COLUMNS)
  return {
    gridX: clampInteger(widget.gridX, 0, Math.max(0, DASHBOARD_GRID_COLUMNS - gridW)),
    gridY: Math.max(0, Math.round(widget.gridY)),
    gridW,
    gridH: Math.max(DASHBOARD_GRID_MIN_HEIGHT, Math.round(widget.gridH)),
  }
}

function widgetPlacementsOverlap(left: DashboardWidgetPlacement, right: DashboardWidgetPlacement): boolean {
  return left.gridX < right.gridX + right.gridW
    && left.gridX + left.gridW > right.gridX
    && left.gridY < right.gridY + right.gridH
    && left.gridY + left.gridH > right.gridY
}

function isWidgetPlacementAvailable(
  preset: BiDashboardPresetLike,
  widgetKey: string,
  placement: DashboardWidgetPlacement,
): boolean {
  return preset.widgets.every(widget =>
    widget.widgetKey === widgetKey
    || !widgetPlacementsOverlap(placement, normalizeWidgetPlacement(widget)))
}

function findNearestAvailableWidgetPlacement(
  preset: BiDashboardPresetLike,
  widgetKey: string,
  desiredPlacement: DashboardWidgetPlacement,
): DashboardWidgetPlacement {
  const desired = normalizeWidgetPlacement(desiredPlacement)
  if (isWidgetPlacementAvailable(preset, widgetKey, desired)) return desired

  const maxBottom = Math.max(
    desired.gridY + desired.gridH,
    ...preset.widgets
      .filter(widget => widget.widgetKey !== widgetKey)
      .map(widget => widget.gridY + widget.gridH),
  )
  const maxY = maxBottom + desired.gridH + preset.widgets.length + 4
  let nearest: DashboardWidgetPlacement | null = null
  let nearestDistance = Number.POSITIVE_INFINITY

  for (let gridY = 0; gridY <= maxY; gridY += 1) {
    for (let gridX = 0; gridX <= DASHBOARD_GRID_COLUMNS - desired.gridW; gridX += 1) {
      const candidate = { ...desired, gridX, gridY }
      if (!isWidgetPlacementAvailable(preset, widgetKey, candidate)) continue
      const distance = Math.abs(gridX - desired.gridX) + Math.abs(gridY - desired.gridY)
      if (
        distance < nearestDistance
        || (distance === nearestDistance && nearest && gridY < nearest.gridY)
        || (distance === nearestDistance && nearest && gridY === nearest.gridY && gridX < nearest.gridX)
      ) {
        nearest = candidate
        nearestDistance = distance
      }
    }
  }

  return nearest ?? { ...desired, gridY: maxY + 1 }
}

export function dashboardWidgetGridPlacement(widget: BiDashboardWidgetPreset): DashboardWidgetGridPlacement {
  const placement = normalizeWidgetPlacementForColumns(widget, DASHBOARD_GRID_COLUMNS)
  return {
    gridColumn: `${placement.gridX + 1} / span ${placement.gridW}`,
    gridRow: `${placement.gridY + 1} / span ${placement.gridH}`,
    minHeight: Math.max(placement.gridH * DASHBOARD_GRID_ROW_HEIGHT, 126),
  }
}

export function dashboardWidgetGridPlacementForColumns(
  widget: BiDashboardWidgetPreset,
  columns: number,
): DashboardWidgetGridPlacement {
  const placement = normalizeWidgetPlacementForColumns(widget, columns)
  return {
    gridColumn: `${placement.gridX + 1} / span ${placement.gridW}`,
    gridRow: `${placement.gridY + 1} / span ${placement.gridH}`,
    minHeight: Math.max(placement.gridH * DASHBOARD_GRID_ROW_HEIGHT, 126),
  }
}

function normalizeWidgetPlacementForColumns(
  widget: DashboardWidgetPlacement,
  columns: number,
): DashboardWidgetPlacement {
  const cappedColumns = Math.max(1, Math.round(columns))
  const minWidth = Math.min(DASHBOARD_GRID_MIN_WIDTH, cappedColumns)
  const gridW = clampInteger(widget.gridW, minWidth, cappedColumns)
  return {
    gridX: clampInteger(widget.gridX, 0, Math.max(0, cappedColumns - gridW)),
    gridY: Math.max(0, Math.round(widget.gridY)),
    gridW,
    gridH: Math.max(DASHBOARD_GRID_MIN_HEIGHT, Math.round(widget.gridH)),
  }
}

export function dashboardLayoutColumns(mode: DashboardLayoutMode): number {
  if (mode === 'mobile') return DASHBOARD_MOBILE_GRID_COLUMNS
  if (mode === 'tablet') return DASHBOARD_TABLET_GRID_COLUMNS
  return DASHBOARD_GRID_COLUMNS
}

export function dashboardResponsiveWidgets(
  preset: BiDashboardPresetLike,
  mode: DashboardLayoutMode,
): BiDashboardWidgetPreset[] {
  if (mode === 'desktop') {
    return preset.widgets.map(widget => ({ ...widget, ...normalizeWidgetPlacement(widget) }))
  }
  const ordered = preset.widgets
    .map((widget, index) => ({ widget, index }))
    .sort((left, right) =>
      left.widget.gridY - right.widget.gridY
      || left.widget.gridX - right.widget.gridX
      || left.index - right.index)

  if (mode === 'mobile') {
    return ordered.map(({ widget }, index) => ({
      ...widget,
      gridX: 0,
      gridY: index * 4,
      gridW: DASHBOARD_MOBILE_GRID_COLUMNS,
      gridH: 4,
    }))
  }

  const placed: BiDashboardWidgetPreset[] = []
  for (const { widget } of ordered) {
    const scaled = scaleWidgetPlacement(widget, DASHBOARD_GRID_COLUMNS, DASHBOARD_TABLET_GRID_COLUMNS)
    const placement = findAvailablePlacementAgainstPlaced(placed, scaled)
    placed.push({ ...widget, ...placement })
  }
  const placedByKey = new Map(placed.map(widget => [widget.widgetKey, widget]))
  return preset.widgets.map(widget => placedByKey.get(widget.widgetKey) ?? widget)
}

export function dashboardDesignerKeyboardActionFromEventLike(event: {
  key: string
  shiftKey?: boolean
  ctrlKey?: boolean
  metaKey?: boolean
}): DashboardDesignerKeyboardAction | null {
  const modifier = Boolean(event.ctrlKey || event.metaKey)
  const key = event.key.length === 1 ? event.key.toLowerCase() : event.key

  if (modifier && key === 'z' && event.shiftKey) return { type: 'redo' }
  if (modifier && key === 'z') return { type: 'undo' }
  if (modifier && key === 'y') return { type: 'redo' }
  if (modifier && key === 'd') return { type: 'duplicate' }
  if (!modifier && (key === 'Delete' || key === 'Backspace')) return { type: 'remove' }

  const directionByKey: Record<string, DashboardWidgetMoveDirection> = {
    ArrowUp: 'up',
    ArrowDown: 'down',
    ArrowLeft: 'left',
    ArrowRight: 'right',
  }
  const direction = directionByKey[key]
  if (!direction) return null
  return event.shiftKey ? { type: 'resize', direction } : { type: 'move', direction }
}

function scaleWidgetPlacement(
  widget: DashboardWidgetPlacement,
  sourceColumns: number,
  targetColumns: number,
): DashboardWidgetPlacement {
  const ratio = targetColumns / Math.max(1, sourceColumns)
  const gridW = clampInteger(Math.round(widget.gridW * ratio), 1, targetColumns)
  return {
    gridX: clampInteger(Math.round(widget.gridX * ratio), 0, Math.max(0, targetColumns - gridW)),
    gridY: Math.max(0, Math.round(widget.gridY)),
    gridW,
    gridH: Math.max(DASHBOARD_GRID_MIN_HEIGHT, Math.round(widget.gridH)),
  }
}

function isPlacementAvailableAgainstPlaced(
  placed: BiDashboardWidgetPreset[],
  placement: DashboardWidgetPlacement,
): boolean {
  return placed.every(widget => !widgetPlacementsOverlap(placement, normalizeWidgetPlacement(widget)))
}

function findAvailablePlacementAgainstPlaced(
  placed: BiDashboardWidgetPreset[],
  desiredPlacement: DashboardWidgetPlacement,
): DashboardWidgetPlacement {
  const desired = normalizeWidgetPlacement(desiredPlacement)
  if (isPlacementAvailableAgainstPlaced(placed, desired)) return desired
  const maxBottom = Math.max(
    desired.gridY + desired.gridH,
    ...placed.map(widget => widget.gridY + widget.gridH),
  )
  for (let gridY = desired.gridY; gridY <= maxBottom + desired.gridH + placed.length + 4; gridY += 1) {
    const candidate = { ...desired, gridY }
    if (isPlacementAvailableAgainstPlaced(placed, candidate)) return candidate
  }
  return { ...desired, gridY: maxBottom + 1 }
}

function resolveDashboardWidgetOverlaps(
  widgets: BiDashboardWidgetPreset[],
  priorityWidgetKey: string,
): BiDashboardWidgetPreset[] {
  const indexedWidgets = widgets.map((widget, index) => ({ widget, index }))
  const priority = indexedWidgets.find(item => item.widget.widgetKey === priorityWidgetKey)
  if (!priority) return widgets

  const ordered = [
    priority,
    ...indexedWidgets
      .filter(item => item.widget.widgetKey !== priorityWidgetKey)
      .sort((left, right) =>
        left.widget.gridY - right.widget.gridY
        || left.widget.gridX - right.widget.gridX
        || left.index - right.index),
  ]

  const placed: BiDashboardWidgetPreset[] = []
  for (const item of ordered) {
    const placement = findAvailablePlacementAgainstPlaced(placed, normalizeWidgetPlacement(item.widget))
    placed.push({ ...item.widget, ...placement })
  }
  const placedByKey = new Map(placed.map(widget => [widget.widgetKey, widget]))
  return widgets.map(widget => placedByKey.get(widget.widgetKey) ?? widget)
}

export function moveDashboardWidget(
  preset: BiDashboardPresetLike,
  widgetKey: string,
  direction: DashboardWidgetMoveDirection,
): BiDashboardPresetLike {
  let changed = false
  const widgets = preset.widgets.map(widget => {
    if (widget.widgetKey !== widgetKey) return widget
    const next = { ...widget }
    if (direction === 'left') next.gridX = Math.max(0, widget.gridX - 1)
    if (direction === 'right') next.gridX = Math.min(Math.max(0, DASHBOARD_GRID_COLUMNS - widget.gridW), widget.gridX + 1)
    if (direction === 'up') next.gridY = Math.max(0, widget.gridY - 1)
    if (direction === 'down') next.gridY = widget.gridY + 1
    const placement = findNearestAvailableWidgetPlacement(preset, widgetKey, normalizeWidgetPlacement(next))
    changed = changed || placement.gridX !== widget.gridX || placement.gridY !== widget.gridY
    return { ...widget, gridX: placement.gridX, gridY: placement.gridY }
  })
  return changed ? { ...preset, widgets } : preset
}

export function moveDashboardWidgetByPixels(
  preset: BiDashboardPresetLike,
  widgetKey: string,
  deltaX: number,
  deltaY: number,
  columnWidth = DASHBOARD_GRID_RESIZE_COLUMN_WIDTH,
  rowHeight = DASHBOARD_GRID_ROW_HEIGHT,
): BiDashboardPresetLike {
  const gridXDelta = Math.round(deltaX / Math.max(1, columnWidth))
  const gridYDelta = Math.round(deltaY / Math.max(1, rowHeight))
  if (gridXDelta === 0 && gridYDelta === 0) return preset
  let changed = false
  const widgets = preset.widgets.map(widget => {
    if (widget.widgetKey !== widgetKey) return widget
    const maxX = Math.max(0, DASHBOARD_GRID_COLUMNS - widget.gridW)
    const desired = normalizeWidgetPlacement({
      ...widget,
      gridX: clampInteger(widget.gridX + gridXDelta, 0, maxX),
      gridY: Math.max(0, Math.round(widget.gridY + gridYDelta)),
    })
    const placement = findNearestAvailableWidgetPlacement(
      preset,
      widgetKey,
      snapDashboardWidgetPlacement(preset, widgetKey, desired).placement,
    )
    if (placement.gridX === widget.gridX && placement.gridY === widget.gridY) return widget
    changed = true
    return { ...widget, gridX: placement.gridX, gridY: placement.gridY }
  })
  return changed ? { ...preset, widgets } : preset
}

export function resizeDashboardWidget(
  preset: BiDashboardPresetLike,
  widgetKey: string,
  gridWDelta: number,
  gridHDelta: number,
): BiDashboardPresetLike {
  let changed = false
  const widgets = preset.widgets.map(widget => {
    if (widget.widgetKey !== widgetKey) return widget
    const maxWidth = Math.max(DASHBOARD_GRID_MIN_WIDTH, DASHBOARD_GRID_COLUMNS - Math.max(0, widget.gridX))
    const nextGridW = clampInteger(widget.gridW + gridWDelta, DASHBOARD_GRID_MIN_WIDTH, maxWidth)
    const nextGridH = Math.max(DASHBOARD_GRID_MIN_HEIGHT, Math.round(widget.gridH + gridHDelta))
    if (nextGridW === widget.gridW && nextGridH === widget.gridH) return widget
    changed = true
    return { ...widget, gridW: nextGridW, gridH: nextGridH }
  })
  return changed ? { ...preset, widgets: resolveDashboardWidgetOverlaps(widgets, widgetKey) } : preset
}

export function resizeDashboardWidgetByPixels(
  preset: BiDashboardPresetLike,
  widgetKey: string,
  deltaX: number,
  deltaY: number,
  columnWidth = DASHBOARD_GRID_RESIZE_COLUMN_WIDTH,
  rowHeight = DASHBOARD_GRID_ROW_HEIGHT,
): BiDashboardPresetLike {
  const gridWDelta = Math.round(deltaX / Math.max(1, columnWidth))
  const gridHDelta = Math.round(deltaY / Math.max(1, rowHeight))
  if (gridWDelta === 0 && gridHDelta === 0) return preset
  return resizeDashboardWidget(preset, widgetKey, gridWDelta, gridHDelta)
}

export function alignDashboardWidgets(
  preset: BiDashboardPresetLike,
  widgetKeys: string[],
  alignment: DashboardWidgetAlignment,
): BiDashboardPresetLike {
  const selectedKeys = new Set(widgetKeys)
  const selected = preset.widgets.filter(widget => selectedKeys.has(widget.widgetKey))
  if (selected.length < 2) return preset

  const placements = selected.map(normalizeWidgetPlacement)
  const left = Math.min(...placements.map(widget => widget.gridX))
  const right = Math.max(...placements.map(widget => widget.gridX + widget.gridW))
  const top = Math.min(...placements.map(widget => widget.gridY))
  const bottom = Math.max(...placements.map(widget => widget.gridY + widget.gridH))
  const center = Math.round((left + right) / 2)
  const middle = Math.round((top + bottom) / 2)
  let changed = false

  const widgets = preset.widgets.map(widget => {
    if (!selectedKeys.has(widget.widgetKey)) return widget
    const current = normalizeWidgetPlacement(widget)
    const next = { ...current }
    if (alignment === 'left') next.gridX = left
    if (alignment === 'right') next.gridX = right - current.gridW
    if (alignment === 'top') next.gridY = top
    if (alignment === 'bottom') next.gridY = bottom - current.gridH
    if (alignment === 'center') next.gridX = center - Math.round(current.gridW / 2)
    if (alignment === 'middle') next.gridY = middle - Math.round(current.gridH / 2)
    const normalized = normalizeWidgetPlacement(next)
    changed = changed
      || normalized.gridX !== widget.gridX
      || normalized.gridY !== widget.gridY
      || normalized.gridW !== widget.gridW
      || normalized.gridH !== widget.gridH
    return { ...widget, ...normalized }
  })

  return changed ? { ...preset, widgets } : preset
}

export function snapDashboardWidgetPlacement(
  preset: BiDashboardPresetLike,
  widgetKey: string,
  desiredPlacement: DashboardWidgetPlacement,
  threshold = 1,
): DashboardWidgetSnapResult {
  let placement = normalizeWidgetPlacement(desiredPlacement)
  const guides: DashboardSnapGuide[] = []
  const others = preset.widgets
    .filter(widget => widget.widgetKey !== widgetKey)
    .map(widget => ({ widget, placement: normalizeWidgetPlacement(widget) }))

  const vertical = nearestSnapGuide(
    others.map(({ widget, placement: other }) => ({
      widgetKey: widget.widgetKey,
      edge: 'left' as DashboardSnapGuideEdge,
      position: other.gridX,
      overlap: overlapLength(
        placement.gridY,
        placement.gridY + placement.gridH,
        other.gridY,
        other.gridY + other.gridH,
      ),
    })).concat(others.map(({ widget, placement: other }) => ({
      widgetKey: widget.widgetKey,
      edge: 'right' as DashboardSnapGuideEdge,
      position: other.gridX + other.gridW,
      overlap: overlapLength(
        placement.gridY,
        placement.gridY + placement.gridH,
        other.gridY,
        other.gridY + other.gridH,
      ),
    }))).concat(others.map(({ widget, placement: other }) => ({
      widgetKey: widget.widgetKey,
      edge: 'center' as DashboardSnapGuideEdge,
      position: Math.round(other.gridX + other.gridW / 2),
      overlap: overlapLength(
        placement.gridY,
        placement.gridY + placement.gridH,
        other.gridY,
        other.gridY + other.gridH,
      ),
    }))),
    [
      { edge: 'left', position: placement.gridX, offset: 0 },
      { edge: 'right', position: placement.gridX + placement.gridW, offset: placement.gridW },
      { edge: 'center', position: Math.round(placement.gridX + placement.gridW / 2), offset: Math.round(placement.gridW / 2) },
    ],
    threshold,
  )

  if (vertical) {
    placement = normalizeWidgetPlacement({ ...placement, gridX: vertical.targetPosition - vertical.sourceOffset })
    guides.push({
      orientation: 'vertical',
      position: vertical.targetPosition,
      widgetKey: vertical.widgetKey,
      edge: vertical.targetEdge,
    })
  }

  const horizontal = nearestSnapGuide(
    others.map(({ widget, placement: other }) => ({
      widgetKey: widget.widgetKey,
      edge: 'bottom' as DashboardSnapGuideEdge,
      position: other.gridY + other.gridH,
      overlap: overlapLength(
        placement.gridX,
        placement.gridX + placement.gridW,
        other.gridX,
        other.gridX + other.gridW,
      ),
    })).concat(others.map(({ widget, placement: other }) => ({
      widgetKey: widget.widgetKey,
      edge: 'top' as DashboardSnapGuideEdge,
      position: other.gridY,
      overlap: overlapLength(
        placement.gridX,
        placement.gridX + placement.gridW,
        other.gridX,
        other.gridX + other.gridW,
      ),
    }))).concat(others.map(({ widget, placement: other }) => ({
      widgetKey: widget.widgetKey,
      edge: 'middle' as DashboardSnapGuideEdge,
      position: Math.round(other.gridY + other.gridH / 2),
      overlap: overlapLength(
        placement.gridX,
        placement.gridX + placement.gridW,
        other.gridX,
        other.gridX + other.gridW,
      ),
    }))),
    [
      { edge: 'top', position: placement.gridY, offset: 0 },
      { edge: 'bottom', position: placement.gridY + placement.gridH, offset: placement.gridH },
      { edge: 'middle', position: Math.round(placement.gridY + placement.gridH / 2), offset: Math.round(placement.gridH / 2) },
    ],
    threshold,
  )

  if (horizontal) {
    placement = normalizeWidgetPlacement({ ...placement, gridY: horizontal.targetPosition - horizontal.sourceOffset })
    guides.push({
      orientation: 'horizontal',
      position: horizontal.targetPosition,
      widgetKey: horizontal.widgetKey,
      edge: horizontal.targetEdge,
    })
  }

  return { placement, guides }
}

interface SnapTargetCandidate {
  widgetKey: string
  edge: DashboardSnapGuideEdge
  position: number
  overlap: number
}

interface SnapSourceCandidate {
  edge: DashboardSnapGuideEdge
  position: number
  offset: number
}

interface NearestSnapGuide {
  widgetKey: string
  targetEdge: DashboardSnapGuideEdge
  targetPosition: number
  sourceOffset: number
  distance: number
  overlap: number
}

function nearestSnapGuide(
  targets: SnapTargetCandidate[],
  sources: SnapSourceCandidate[],
  threshold: number,
): NearestSnapGuide | null {
  let nearest: NearestSnapGuide | null = null
  const cappedThreshold = Math.max(0, threshold)
  for (const target of targets) {
    for (const source of sources) {
      if (isCenterSnapEdge(target.edge) !== isCenterSnapEdge(source.edge)) continue
      const distance = Math.abs(target.position - source.position)
      if (distance > cappedThreshold) continue
      if (
        !nearest
        || distance < nearest.distance
        || (distance === nearest.distance && target.overlap > nearest.overlap)
      ) {
        nearest = {
          widgetKey: target.widgetKey,
          targetEdge: target.edge,
          targetPosition: target.position,
          sourceOffset: source.offset,
          distance,
          overlap: target.overlap,
        }
      }
    }
  }
  return nearest
}

function isCenterSnapEdge(edge: DashboardSnapGuideEdge): boolean {
  return edge === 'center' || edge === 'middle'
}

function overlapLength(leftStart: number, leftEnd: number, rightStart: number, rightEnd: number): number {
  return Math.max(0, Math.min(leftEnd, rightEnd) - Math.max(leftStart, rightStart))
}

export function createDashboardPresetHistory(preset: BiDashboardPresetLike): DashboardPresetHistory {
  return { past: [], present: preset, future: [] }
}

export function pushDashboardPresetHistory(
  history: DashboardPresetHistory,
  next: BiDashboardPresetLike,
  limit = 30,
): DashboardPresetHistory {
  if (next === history.present) return history
  return {
    past: [...history.past.slice(Math.max(0, history.past.length - limit + 1)), history.present],
    present: next,
    future: [],
  }
}

export function undoDashboardPresetHistory(history: DashboardPresetHistory): DashboardPresetHistory {
  const previous = history.past[history.past.length - 1]
  if (!previous) return history
  return {
    past: history.past.slice(0, -1),
    present: previous,
    future: [history.present, ...history.future],
  }
}

export function redoDashboardPresetHistory(history: DashboardPresetHistory): DashboardPresetHistory {
  const next = history.future[0]
  if (!next) return history
  return {
    past: [...history.past, history.present],
    present: next,
    future: history.future.slice(1),
  }
}

export function filterDesignerItems<T extends DesignerSearchableItem>(
  items: T[],
  keyword: string,
): T[] {
  const value = keyword.trim().toLowerCase()
  if (!value) return items
  return items.filter(item => [
    item.key,
    item.label,
    item.name,
    item.group,
    item.datasetKey,
    item.chartKey,
  ].some(text => String(text ?? '').toLowerCase().includes(value)))
}

export function toResourceLocationIndex<T extends BiResourceLocationLike>(locations: T[]): Record<string, T> {
  return Object.fromEntries(locations.map(location => [
    resourceLocationIndexKey(location.resourceType, location.resourceKey),
    location,
  ]))
}

export function resourceFolderLabel(location: BiResourceLocationLike | undefined): string {
  return location?.folderKey?.trim() || '根目录'
}

export function toResourceOwnershipIndex<T extends BiResourceOwnershipLike>(ownerships: T[]): Record<string, T> {
  return Object.fromEntries(ownerships.map(ownership => [
    resourceLocationIndexKey(ownership.resourceType, ownership.resourceKey),
    ownership,
  ]))
}

export function resourceOwnerLabel(ownership: BiResourceOwnershipLike | undefined): string {
  return ownership?.ownerUser?.trim() || '未分配'
}

export function toResourceFavoriteIndex<T extends BiResourceFavoriteLike>(favorites: T[]): Record<string, T> {
  return Object.fromEntries(favorites.map(favorite => [
    resourceLocationIndexKey(favorite.resourceType, favorite.resourceKey),
    favorite,
  ]))
}

export function resourceFavoriteLabel(favorite: BiResourceFavoriteLike | undefined): string {
  return favorite?.favorite === true ? '已收藏' : '未收藏'
}

export function buildBiResourceTargets(input: BiResourceTargetInputLike): BiResourceTargetLike[] {
  return [
    resourceTarget('当前仪表板', 'DASHBOARD', input.dashboardKey),
    resourceTarget('选中图表', 'CHART', input.chartKey),
    resourceTarget('选中数据集', 'DATASET', input.datasetKey),
    resourceTarget('选中门户', 'PORTAL', input.portalKey),
    resourceTarget('选中大屏', 'BIG_SCREEN', input.bigScreenKey),
    resourceTarget('选中电子表格', 'SPREADSHEET', input.spreadsheetKey),
  ]
}

export function buildBiPermissionResourceTargets(input: BiResourceTargetInputLike): BiResourceTargetLike[] {
  return [
    resourceTarget('当前仪表板', 'DASHBOARD', input.dashboardKey),
    resourceTarget('选中图表', 'CHART', input.chartKey),
    resourceTarget('选中数据集', 'DATASET', input.datasetKey),
    resourceTarget('选中数据源', 'DATASOURCE', input.dataSourceKey),
    resourceTarget('选中门户', 'PORTAL', input.portalKey),
    resourceTarget('选中大屏', 'BIG_SCREEN', input.bigScreenKey),
    resourceTarget('选中电子表格', 'SPREADSHEET', input.spreadsheetKey),
  ]
}

export function buildBigScreenResourceOptions(resources: BiBigScreenResourceOptionLike[]): BiResourcePickerOptionLike[] {
  return resources
    .map(resource => resourcePickerOption(resource.screenKey, resource.name, resource.status))
    .filter(option => option.value)
}

export function buildSpreadsheetResourceOptions(resources: BiSpreadsheetResourceOptionLike[]): BiResourcePickerOptionLike[] {
  return resources
    .map(resource => resourcePickerOption(resource.spreadsheetKey, resource.name, resource.status))
    .filter(option => option.value)
}

export function biRuntimeRouteFromSearchParams(searchParams: URLSearchParams): BiRuntimeRouteLike {
  const mode = runtimeRouteMode(searchParams.get('mode'))
  return {
    mode,
    resourceType: searchParams.get('resourceType')?.trim().toUpperCase() || runtimeResourceTypeForMode(mode),
    resourceId: nullableRuntimeResourceId(searchParams.get('resourceId')),
    resourceKey: searchParams.get('resourceKey')?.trim() || null,
  }
}

export function selectBigScreenRuntimeResource<T extends BiBigScreenResourceOptionLike>(
  resources: T[],
  route: BiRuntimeRouteLike | null | undefined,
): T | null {
  if (route?.resourceType && route.resourceType !== 'BIG_SCREEN') return resources[0] ?? null
  if (route?.resourceId != null) {
    const byId = resources.find(resource => resource.id === route.resourceId)
    if (byId) return byId
  }
  if (route?.resourceKey) {
    const byKey = resources.find(resource => resource.screenKey === route.resourceKey)
    if (byKey) return byKey
  }
  return resources[0] ?? null
}

export function selectSpreadsheetRuntimeResource<T extends BiSpreadsheetResourceOptionLike>(
  resources: T[],
  route: BiRuntimeRouteLike | null | undefined,
): T | null {
  if (route?.resourceType && route.resourceType !== 'SPREADSHEET') return resources[0] ?? null
  if (route?.resourceId != null) {
    const byId = resources.find(resource => resource.id === route.resourceId)
    if (byId) return byId
  }
  if (route?.resourceKey) {
    const byKey = resources.find(resource => resource.spreadsheetKey === route.resourceKey)
    if (byKey) return byKey
  }
  return resources[0] ?? null
}

export function buildBigScreenDraftResource(input: BiBigScreenDraftInputLike): BiBigScreenDraftResourceLike {
  const screenKey = normalizeResourceKey(input.screenKey || input.name || 'big-screen')
  const name = input.name?.trim() || screenKey
  const description = input.description?.trim() || null
  const dashboardKey = input.dashboardKey?.trim() || null
  const datasetKey = input.datasetKey?.trim() || null
  const resourceKey = dashboardKey || datasetKey || screenKey
  const widgetBaseKey = normalizeResourceKey(resourceKey.replace(/_/g, '-'))

  return {
    screenKey,
    name,
    description,
    size: { width: 1920, height: 1080 },
    background: { type: 'SOLID', color: '#101820' },
    layout: [
      {
        widgetKey: `${widgetBaseKey}-hero`,
        title: name,
        resourceType: dashboardKey ? 'DASHBOARD' : 'DATASET',
        resourceKey,
        datasetKey,
        x: 0,
        y: 0,
        w: 24,
        h: 12,
      },
    ],
    refresh: { enabled: true, intervalSeconds: 60 },
    mobileLayout: { columns: 1, stack: true },
    status: 'DRAFT',
    version: 1,
    source: 'LOCAL',
  }
}

export const BIG_SCREEN_COMPONENT_LIBRARY: BiBigScreenLibraryComponentLike[] = [
  { key: 'metric-card', title: '指标卡', componentType: 'METRIC_CARD', w: 6, h: 4, resourceType: 'DATASET' },
  { key: 'trend-line', title: '趋势折线', componentType: 'TREND_LINE', w: 12, h: 5, resourceType: 'DATASET' },
  { key: 'rank-list', title: '排行列表', componentType: 'RANK_LIST', w: 8, h: 6, resourceType: 'DATASET' },
  { key: 'text-panel', title: '文本面板', componentType: 'TEXT_PANEL', w: 6, h: 3, resourceType: 'STATIC' },
]

export function addBigScreenLibraryComponent<T extends BiBigScreenResourceLike>(
  resource: T,
  componentKey: string | null | undefined,
): T {
  const component = BIG_SCREEN_COMPONENT_LIBRARY.find(item => item.key === componentKey) ?? BIG_SCREEN_COMPONENT_LIBRARY[0]
  const layout = (resource.layout ?? []).map(item => ({ ...item }))
  const placement = nextBigScreenLibraryPlacement(layout, component.w, component.h)
  const datasetKey = String(resource.layout?.[0]?.datasetKey ?? '').trim()
    || String((resource as unknown as { dataBinding?: Record<string, unknown> }).dataBinding?.datasetKey ?? '').trim()
    || null
  const widgetKey = uniqueBigScreenWidgetKey(resource, component.key, layout)
  const nextItem: Record<string, unknown> = {
    widgetKey,
    title: component.title,
    componentType: component.componentType,
    resourceType: component.resourceType,
    resourceKey: component.resourceType === 'DATASET' ? datasetKey ?? resource.screenKey : resource.screenKey,
    datasetKey,
    x: placement.x,
    y: placement.y,
    w: placement.w,
    h: placement.h,
  }
  return { ...resource, layout: [...layout, nextItem] }
}

export function buildSpreadsheetDraftResource(input: BiSpreadsheetDraftInputLike): BiSpreadsheetDraftResourceLike {
  const spreadsheetKey = normalizeResourceKey(input.spreadsheetKey || input.name || 'spreadsheet')
  const name = input.name?.trim() || spreadsheetKey
  const description = input.description?.trim() || null
  const datasetKey = input.datasetKey?.trim() || null
  const widgetBaseKey = normalizeResourceKey((datasetKey || spreadsheetKey).replace(/_/g, '-'))

  return {
    spreadsheetKey,
    name,
    description,
    sheets: [
      {
        sheetKey: 'summary',
        name: 'Summary',
        widgets: [
          {
            widgetKey: `${widgetBaseKey}-table`,
            type: 'TABLE',
            datasetKey,
            range: 'A1:H20',
          },
        ],
      },
    ],
    dataBinding: { datasetKey, refreshMode: 'MANUAL' },
    style: { theme: 'default', density: 'compact' },
    status: 'DRAFT',
    version: 1,
    source: 'LOCAL',
  }
}

export function updateBigScreenLayoutItem<T extends BiBigScreenResourceLike>(
  resource: T,
  widgetKey: string | null | undefined,
  patch: BiBigScreenLayoutPatchLike,
): T {
  const layout = (resource.layout ?? []).map(item => ({ ...item }))
  const requestedKey = patch.widgetKey?.trim() || widgetKey?.trim() || ''
  const targetIndex = layout.findIndex(item => String(item.widgetKey ?? '') === requestedKey)
  const index = targetIndex >= 0 ? targetIndex : 0
  const current = layout[index] ?? {
    widgetKey: requestedKey || `${resource.screenKey || 'screen'}-widget`,
    title: resource.name || requestedKey || 'Widget',
    resourceType: 'DASHBOARD',
    resourceKey: requestedKey || resource.screenKey || '',
    x: 0,
    y: 0,
    w: 8,
    h: 5,
  }
  const nextWidgetKey = normalizeResourceKey(patch.widgetKey ?? String(current.widgetKey ?? (requestedKey || 'widget')))
  const next = {
    ...current,
    widgetKey: nextWidgetKey || String(current.widgetKey ?? (requestedKey || 'widget')),
    title: patch.title != null ? patch.title.trim() : current.title,
    resourceType: patch.resourceType != null ? patch.resourceType.trim().toUpperCase() : current.resourceType,
    resourceKey: patch.resourceKey != null ? patch.resourceKey.trim() : current.resourceKey,
    datasetKey: patch.datasetKey != null ? patch.datasetKey.trim() : current.datasetKey,
    x: clampedInteger(patch.x, current.x, 0, 23),
    y: clampedInteger(patch.y, current.y, 0, 99),
    w: clampedInteger(patch.w, current.w, 1, 24),
    h: clampedInteger(patch.h, current.h, 1, 24),
  }
  const normalized = layout.length ? layout : [next]
  normalized[index] = next
  return { ...resource, layout: normalized }
}

export function moveBigScreenLayoutItem<T extends BiBigScreenResourceLike>(
  resource: T,
  widgetKey: string | null | undefined,
  direction: BiBigScreenLayoutDirection,
): T {
  const current = findBigScreenLayoutItem(resource, widgetKey)
  if (!current) return resource
  const x = clampedInteger(undefined, current.x, 0, 23)
  const y = clampedInteger(undefined, current.y, 0, 99)
  return updateBigScreenLayoutItem(resource, widgetKey, {
    x: direction === 'left' ? x - 1 : direction === 'right' ? x + 1 : x,
    y: direction === 'up' ? y - 1 : direction === 'down' ? y + 1 : y,
  })
}

export function resizeBigScreenLayoutItem<T extends BiBigScreenResourceLike>(
  resource: T,
  widgetKey: string | null | undefined,
  direction: BiBigScreenLayoutDirection,
): T {
  const current = findBigScreenLayoutItem(resource, widgetKey)
  if (!current) return resource
  const x = clampedInteger(undefined, current.x, 0, 23)
  const maxWidth = Math.max(1, 24 - x)
  const w = clampedInteger(undefined, current.w, 1, Math.max(1, 24 - x))
  const h = clampedInteger(undefined, current.h, 1, 24)
  const nextW = direction === 'left' ? w - 1 : direction === 'right' ? w + 1 : w
  const nextH = direction === 'up' ? h - 1 : direction === 'down' ? h + 1 : h
  return updateBigScreenLayoutItem(resource, widgetKey, {
    w: clampInteger(nextW, 1, maxWidth),
    h: clampInteger(nextH, 1, 24),
  })
}

export function alignBigScreenLayoutItems<T extends BiBigScreenResourceLike>(
  resource: T,
  widgetKeys: string[],
  alignment: DashboardWidgetAlignment,
): T {
  const selectedKeys = new Set(widgetKeys.map(key => key.trim()).filter(Boolean))
  const selected = (resource.layout ?? []).filter(item => selectedKeys.has(String(item.widgetKey ?? '')))
  if (selected.length < 2) return resource
  const placements = selected.map(bigScreenLayoutPlacement)
  const left = Math.min(...placements.map(item => item.x))
  const right = Math.max(...placements.map(item => item.x + item.w))
  const top = Math.min(...placements.map(item => item.y))
  const bottom = Math.max(...placements.map(item => item.y + item.h))
  const center = Math.round((left + right) / 2)
  const middle = Math.round((top + bottom) / 2)
  let changed = false

  const layout = (resource.layout ?? []).map(item => {
    if (!selectedKeys.has(String(item.widgetKey ?? ''))) return item
    const current = bigScreenLayoutPlacement(item)
    const next = { ...current }
    if (alignment === 'left') next.x = left
    if (alignment === 'right') next.x = right - current.w
    if (alignment === 'top') next.y = top
    if (alignment === 'bottom') next.y = bottom - current.h
    if (alignment === 'center') next.x = center - Math.round(current.w / 2)
    if (alignment === 'middle') next.y = middle - Math.round(current.h / 2)
    const normalized = normalizeBigScreenLayoutPlacement(next)
    changed = changed || normalized.x !== current.x || normalized.y !== current.y
    return { ...item, x: normalized.x, y: normalized.y }
  })

  return changed ? { ...resource, layout } : resource
}

export function snapBigScreenLayoutItem<T extends BiBigScreenResourceLike>(
  resource: T,
  widgetKey: string | null | undefined,
  threshold = 1,
): BiBigScreenLayoutSnapResult<T> {
  const current = findBigScreenLayoutItem(resource, widgetKey)
  if (!current) return { resource, guides: [] }
  const requestedKey = String(current.widgetKey ?? widgetKey ?? '')
  let placement = bigScreenLayoutPlacement(current)
  const guides: DashboardSnapGuide[] = []
  const others = (resource.layout ?? [])
    .filter(item => String(item.widgetKey ?? '') !== requestedKey)
    .map(item => ({
      widgetKey: String(item.widgetKey ?? ''),
      placement: bigScreenLayoutPlacement(item),
    }))
    .filter(item => item.widgetKey)

  const vertical = nearestSnapGuide(
    others.map(({ widgetKey: otherWidgetKey, placement: other }) => ({
      widgetKey: otherWidgetKey,
      edge: 'left' as DashboardSnapGuideEdge,
      position: other.x,
      overlap: overlapLength(placement.y, placement.y + placement.h, other.y, other.y + other.h),
    })).concat(others.map(({ widgetKey: otherWidgetKey, placement: other }) => ({
      widgetKey: otherWidgetKey,
      edge: 'right' as DashboardSnapGuideEdge,
      position: other.x + other.w,
      overlap: overlapLength(placement.y, placement.y + placement.h, other.y, other.y + other.h),
    }))).concat(others.map(({ widgetKey: otherWidgetKey, placement: other }) => ({
      widgetKey: otherWidgetKey,
      edge: 'center' as DashboardSnapGuideEdge,
      position: Math.round(other.x + other.w / 2),
      overlap: overlapLength(placement.y, placement.y + placement.h, other.y, other.y + other.h),
    }))),
    [
      { edge: 'left', position: placement.x, offset: 0 },
      { edge: 'right', position: placement.x + placement.w, offset: placement.w },
      { edge: 'center', position: Math.round(placement.x + placement.w / 2), offset: Math.round(placement.w / 2) },
    ],
    threshold,
  )

  if (vertical) {
    placement = normalizeBigScreenLayoutPlacement({ ...placement, x: vertical.targetPosition - vertical.sourceOffset })
    guides.push({
      orientation: 'vertical',
      position: vertical.targetPosition,
      widgetKey: vertical.widgetKey,
      edge: vertical.targetEdge,
    })
  }

  const horizontal = nearestSnapGuide(
    others.map(({ widgetKey: otherWidgetKey, placement: other }) => ({
      widgetKey: otherWidgetKey,
      edge: 'bottom' as DashboardSnapGuideEdge,
      position: other.y + other.h,
      overlap: overlapLength(placement.x, placement.x + placement.w, other.x, other.x + other.w),
    })).concat(others.map(({ widgetKey: otherWidgetKey, placement: other }) => ({
      widgetKey: otherWidgetKey,
      edge: 'top' as DashboardSnapGuideEdge,
      position: other.y,
      overlap: overlapLength(placement.x, placement.x + placement.w, other.x, other.x + other.w),
    }))).concat(others.map(({ widgetKey: otherWidgetKey, placement: other }) => ({
      widgetKey: otherWidgetKey,
      edge: 'middle' as DashboardSnapGuideEdge,
      position: Math.round(other.y + other.h / 2),
      overlap: overlapLength(placement.x, placement.x + placement.w, other.x, other.x + other.w),
    }))),
    [
      { edge: 'top', position: placement.y, offset: 0 },
      { edge: 'bottom', position: placement.y + placement.h, offset: placement.h },
      { edge: 'middle', position: Math.round(placement.y + placement.h / 2), offset: Math.round(placement.h / 2) },
    ],
    threshold,
  )

  if (horizontal) {
    placement = normalizeBigScreenLayoutPlacement({ ...placement, y: horizontal.targetPosition - horizontal.sourceOffset })
    guides.push({
      orientation: 'horizontal',
      position: horizontal.targetPosition,
      widgetKey: horizontal.widgetKey,
      edge: horizontal.targetEdge,
    })
  }

  if (guides.length === 0) return { resource, guides }
  return {
    resource: updateBigScreenLayoutItem(resource, requestedKey, {
      x: placement.x,
      y: placement.y,
      w: placement.w,
      h: placement.h,
    }),
    guides,
  }
}

export function updateBigScreenMobileLayout<T extends BiBigScreenResourceLike>(
  resource: T,
  variant: BiBigScreenMobileLayoutVariant,
): T {
  const columns = variant === 'compact-grid' ? 2 : 1
  const orderedLayout = [...(resource.layout ?? [])]
    .map(item => ({ item, placement: bigScreenLayoutPlacement(item) }))
    .sort((left, right) => left.placement.y - right.placement.y || left.placement.x - right.placement.x)
  let nextY = 0
  const columnHeights = Array.from({ length: columns }, () => 0)
  const items: BiBigScreenMobileLayoutItemLike[] = orderedLayout.map(({ item, placement }) => {
    const widgetKey = String(item.widgetKey ?? '')
    const height = Math.max(1, placement.h)
    if (columns === 1 || placement.w >= 18) {
      const y = columns === 1 ? nextY : Math.max(...columnHeights)
      const mobileItem = { widgetKey, x: 0, y, w: columns, h: height }
      if (columns === 1) {
        nextY += height
      } else {
        columnHeights.fill(y + height)
      }
      return mobileItem
    }
    const column = columnHeights[0] <= columnHeights[1] ? 0 : 1
    const y = columnHeights[column]
    columnHeights[column] = y + height
    return { widgetKey, x: column, y, w: 1, h: height }
  }).filter(item => item.widgetKey)

  return {
    ...resource,
    mobileLayout: {
      variant,
      columns,
      stack: columns === 1,
      items,
    },
  }
}

function bigScreenLayoutPlacement(item: Record<string, unknown>): { x: number; y: number; w: number; h: number } {
  return normalizeBigScreenLayoutPlacement({
    x: clampedInteger(undefined, item.x, 0, 23),
    y: clampedInteger(undefined, item.y, 0, 99),
    w: clampedInteger(undefined, item.w, 1, 24),
    h: clampedInteger(undefined, item.h, 1, 24),
  })
}

function nextBigScreenLibraryPlacement(
  layout: Record<string, unknown>[],
  width: number,
  height: number,
): { x: number; y: number; w: number; h: number } {
  const w = clampInteger(width, 1, 24)
  const h = clampInteger(height, 1, 24)
  const placements = layout.map(bigScreenLayoutPlacement)
  const maxBottom = placements.reduce((bottom, item) => Math.max(bottom, item.y + item.h), 0)
  const lastRow = placements.filter(item => item.y + item.h === maxBottom)
  const rowY = lastRow.length ? Math.min(...lastRow.map(item => item.y)) : maxBottom
  const rowRight = placements
    .filter(item => item.y === rowY)
    .reduce((right, item) => Math.max(right, item.x + item.w), 0)
  if (rowRight + w <= 24) {
    return { x: rowRight, y: rowY, w, h }
  }
  return { x: 0, y: maxBottom, w, h }
}

function uniqueBigScreenWidgetKey(
  resource: BiBigScreenResourceLike,
  componentKey: string,
  layout: Record<string, unknown>[],
): string {
  const base = normalizeResourceKey(`${resource.screenKey || 'screen'}-${componentKey}`)
  const existing = new Set(layout.map(item => String(item.widgetKey ?? '')))
  if (!existing.has(base)) return base
  let index = 2
  while (existing.has(`${base}-${index}`)) index += 1
  return `${base}-${index}`
}

function normalizeBigScreenLayoutPlacement(placement: { x: number; y: number; w: number; h: number }) {
  const x = clampInteger(placement.x, 0, 23)
  const w = clampInteger(placement.w, 1, Math.max(1, 24 - x))
  return {
    x,
    y: clampInteger(placement.y, 0, 99),
    w,
    h: clampInteger(placement.h, 1, 24),
  }
}

function findBigScreenLayoutItem(
  resource: BiBigScreenResourceLike,
  widgetKey: string | null | undefined,
): Record<string, unknown> | null {
  const layout = resource.layout ?? []
  const requestedKey = widgetKey?.trim() || ''
  return layout.find(item => String(item.widgetKey ?? '') === requestedKey) ?? layout[0] ?? null
}

export function updateSpreadsheetCell<T extends BiSpreadsheetResourceLike>(
  resource: T,
  sheetKey: string | null | undefined,
  cellKey: string,
  value: string,
): T {
  const normalizedSheetKey = sheetKey?.trim() || String(resource.sheets?.[0]?.sheetKey ?? 'summary')
  const normalizedCellKey = normalizeSpreadsheetCellKey(cellKey)
  const sheets = (resource.sheets ?? []).map(sheet => ({ ...sheet }))
  let sheetIndex = sheets.findIndex(sheet => String(sheet.sheetKey ?? '') === normalizedSheetKey)
  if (sheetIndex < 0) {
    sheetIndex = sheets.length
    sheets.push({ sheetKey: normalizedSheetKey, name: normalizedSheetKey, cells: {} })
  }
  const currentSheet = sheets[sheetIndex]
  const cells = {
    ...((currentSheet.cells && typeof currentSheet.cells === 'object') ? currentSheet.cells as Record<string, unknown> : {}),
  }
  const trimmedValue = value.trim()
  if (trimmedValue) {
    cells[normalizedCellKey] = trimmedValue
  } else {
    delete cells[normalizedCellKey]
  }
  sheets[sheetIndex] = { ...currentSheet, cells }
  return { ...resource, sheets }
}

export function updateSpreadsheetCellRange<T extends BiSpreadsheetResourceLike>(
  resource: T,
  sheetKey: string | null | undefined,
  rangeText: string,
  value: string,
): T {
  const [startCell, endCell] = rangeText.split(':').map(item => item?.trim()).filter(Boolean)
  const cellKeys = endCell ? spreadsheetCellRange(startCell, endCell) : [normalizeSpreadsheetCellKey(startCell ?? rangeText)]
  return cellKeys.reduce(
    (current, cellKey) => updateSpreadsheetCell(current, sheetKey, cellKey, value),
    resource,
  )
}

export function updateSpreadsheetCellStyle<T extends BiSpreadsheetResourceLike>(
  resource: T,
  sheetKey: string | null | undefined,
  cellKey: string,
  patch: BiSpreadsheetCellStylePatchLike,
): T {
  const normalizedSheetKey = sheetKey?.trim() || String(resource.sheets?.[0]?.sheetKey ?? 'summary')
  const normalizedCellKey = normalizeSpreadsheetCellKey(cellKey)
  const sheets = (resource.sheets ?? []).map(sheet => ({ ...sheet }))
  let sheetIndex = sheets.findIndex(sheet => String(sheet.sheetKey ?? '') === normalizedSheetKey)
  if (sheetIndex < 0) {
    sheetIndex = sheets.length
    sheets.push({ sheetKey: normalizedSheetKey, name: normalizedSheetKey, cells: {} })
  }
  const currentSheet = sheets[sheetIndex]
  const cellStyles = {
    ...((currentSheet.cellStyles && typeof currentSheet.cellStyles === 'object') ? currentSheet.cellStyles as Record<string, Record<string, unknown>> : {}),
  }
  const currentStyle = {
    ...((cellStyles[normalizedCellKey] && typeof cellStyles[normalizedCellKey] === 'object') ? cellStyles[normalizedCellKey] : {}),
  }
  const nextStyle: Record<string, unknown> = { ...currentStyle }
  if (patch.bold != null) {
    if (patch.bold) nextStyle.bold = true
    else delete nextStyle.bold
  }
  if (patch.backgroundColor != null) {
    const backgroundColor = patch.backgroundColor.trim()
    if (backgroundColor) nextStyle.backgroundColor = backgroundColor
    else delete nextStyle.backgroundColor
  }
  if (patch.textColor != null) {
    const textColor = patch.textColor.trim()
    if (textColor) nextStyle.textColor = textColor
    else delete nextStyle.textColor
  }

  if (Object.keys(nextStyle).length) {
    cellStyles[normalizedCellKey] = nextStyle
  } else {
    delete cellStyles[normalizedCellKey]
  }
  sheets[sheetIndex] = { ...currentSheet, cellStyles }
  return { ...resource, sheets }
}

export function buildSpreadsheetPivotTable<T extends BiSpreadsheetResourceLike>(
  resource: T,
  sheetKey: string | null | undefined,
  input: BiSpreadsheetPivotTableInputLike,
): T {
  const normalizedSheetKey = sheetKey?.trim() || String(resource.sheets?.[0]?.sheetKey ?? 'summary')
  const sheets = (resource.sheets ?? []).map(sheet => ({ ...sheet }))
  let sheetIndex = sheets.findIndex(sheet => String(sheet.sheetKey ?? '') === normalizedSheetKey)
  if (sheetIndex < 0) {
    sheetIndex = sheets.length
    sheets.push({ sheetKey: normalizedSheetKey, name: normalizedSheetKey, cells: {} })
  }

  const currentSheet = sheets[sheetIndex]
  const currentCells = {
    ...((currentSheet.cells && typeof currentSheet.cells === 'object') ? currentSheet.cells as Record<string, unknown> : {}),
  }
  const evaluatedCells = evaluateSpreadsheetCells(currentCells)
  const sourceCells = spreadsheetCellRangeFromText(input.sourceRange)
  const sourceRows = spreadsheetCellRows(sourceCells)
  const headerCells = sourceRows[0] ?? []
  const headers = headerCells.map(cellKey => String(evaluatedCells[cellKey] ?? '').trim())
  const rowFieldIndex = headers.findIndex(header => header === input.rowField.trim())
  const columnFieldIndex = headers.findIndex(header => header === input.columnField.trim())
  const valueFields = normalizeSpreadsheetPivotValueFields(input)
    .map(valueField => ({
      ...valueField,
      index: headers.findIndex(header => header === valueField.field),
    }))
    .filter((valueField): valueField is BiSpreadsheetPivotValueFieldInputLike & { field: string; aggregation: BiSpreadsheetPivotAggregation; label: string; index: number } => valueField.index >= 0)
  const target = parseSpreadsheetCellKey(input.targetCell)
  if (!target || rowFieldIndex < 0 || columnFieldIndex < 0 || valueFields.length === 0) {
    sheets[sheetIndex] = currentSheet
    return { ...resource, sheets }
  }

  const aggregation = normalizeSpreadsheetPivotAggregation(input.aggregation)
  const rowLabels: string[] = []
  const columnLabels: string[] = []
  const buckets = new Map<string, unknown[]>()

  for (const row of sourceRows.slice(1)) {
    const rowLabel = String(evaluatedCells[row[rowFieldIndex]] ?? '').trim()
    const columnLabel = String(evaluatedCells[row[columnFieldIndex]] ?? '').trim()
    if (!rowLabel || !columnLabel) continue
    if (!rowLabels.includes(rowLabel)) rowLabels.push(rowLabel)
    if (!columnLabels.includes(columnLabel)) columnLabels.push(columnLabel)
    const bucketKey = `${rowLabel}\u0000${columnLabel}`
    const bucketValues = valueFields.map(valueField => evaluatedCells[row[valueField.index]])
    buckets.set(bucketKey, [...(buckets.get(bucketKey) ?? []), bucketValues])
  }

  const cells = { ...currentCells }
  const targetColumn = target.column
  const targetRow = target.row
  cells[spreadsheetColumnName(targetColumn) + targetRow] = `${input.rowField.trim()} / ${input.columnField.trim()}`
  const multipleValueFields = valueFields.length > 1
  columnLabels.forEach((columnLabel, columnIndex) => {
    valueFields.forEach((valueField, valueIndex) => {
      const outputColumn = targetColumn + columnIndex * valueFields.length + valueIndex + 1
      cells[spreadsheetColumnName(outputColumn) + targetRow] = multipleValueFields ? `${columnLabel} ${valueField.label}` : columnLabel
    })
  })
  rowLabels.forEach((rowLabel, rowIndex) => {
    const outputRow = targetRow + rowIndex + 1
    cells[spreadsheetColumnName(targetColumn) + outputRow] = rowLabel
    columnLabels.forEach((columnLabel, columnIndex) => {
      const values = buckets.get(`${rowLabel}\u0000${columnLabel}`) ?? []
      valueFields.forEach((valueField, valueIndex) => {
        const outputColumn = targetColumn + columnIndex * valueFields.length + valueIndex + 1
        const metricValues = values.map(rowValues => Array.isArray(rowValues) ? rowValues[valueIndex] : rowValues)
        cells[spreadsheetColumnName(outputColumn) + outputRow] = evaluateSpreadsheetAggregate(valueField.aggregation, metricValues)
      })
    })
  })

  const pivotKey = `pivot-${normalizedSheetKey}-${normalizeSpreadsheetCellKey(input.targetCell).toLowerCase()}`
  const pivotTable = {
    pivotKey,
    sourceRange: normalizeSpreadsheetRangeText(input.sourceRange),
    targetCell: normalizeSpreadsheetCellKey(input.targetCell),
    rowField: input.rowField.trim(),
    columnField: input.columnField.trim(),
    valueField: input.valueField.trim(),
    aggregation,
    ...(multipleValueFields ? {
      valueFields: valueFields.map(valueField => ({
        field: valueField.field,
        aggregation: valueField.aggregation,
        label: valueField.label,
      })),
    } : {}),
    rowLabels,
    columnLabels,
  }
  const pivotTables = [
    ...(((currentSheet.pivotTables && Array.isArray(currentSheet.pivotTables)) ? currentSheet.pivotTables : []) as Record<string, unknown>[])
      .filter(item => String(item.pivotKey ?? '') !== pivotKey),
    pivotTable,
  ]

  sheets[sheetIndex] = { ...currentSheet, cells, pivotTables }
  return { ...resource, sheets }
}

export function evaluateSpreadsheetCells(cells: Record<string, unknown>): Record<string, unknown> {
  const normalizedCells = Object.fromEntries(
    Object.entries(cells).map(([key, value]) => [normalizeSpreadsheetCellKey(key), value]),
  )
  const evaluated: Record<string, unknown> = {}
  const visiting = new Set<string>()

  const evaluateCell = (cellKey: string): unknown => {
    const normalizedKey = normalizeSpreadsheetCellKey(cellKey)
    if (Object.prototype.hasOwnProperty.call(evaluated, normalizedKey)) return evaluated[normalizedKey]
    if (visiting.has(normalizedKey)) {
      evaluated[normalizedKey] = '#CYCLE!'
      return '#CYCLE!'
    }
    visiting.add(normalizedKey)
    const rawValue = normalizedCells[normalizedKey]
    const value = evaluateSpreadsheetValue(rawValue, evaluateCell)
    visiting.delete(normalizedKey)
    evaluated[normalizedKey] = value
    return value
  }

  for (const key of Object.keys(normalizedCells)) {
    evaluateCell(key)
  }
  return evaluated
}

function evaluateSpreadsheetValue(
  value: unknown,
  evaluateCell: (cellKey: string) => unknown,
): unknown {
  if (typeof value !== 'string' || !value.trim().startsWith('=')) return value
  const expression = value.trim().slice(1).trim()
  const directReference = expression.match(/^([A-Z]+[0-9]+)$/i)
  if (directReference) return evaluateCell(directReference[1])
  const aggregate = expression.match(/^(SUM|AVERAGE|MIN|MAX|COUNT)\((.*)\)$/i)
  if (aggregate) {
    // 聚合函数支持单元格、区域、数字和嵌套算术表达式作为参数。
    const values = spreadsheetFormulaArguments(aggregate[2], evaluateCell)
    return evaluateSpreadsheetAggregate(aggregate[1], values)
  }
  // 非聚合公式兜底尝试四则运算，无法解析时保留原公式文本。
  const arithmetic = evaluateSpreadsheetArithmeticExpression(expression, evaluateCell)
  return arithmetic ?? value
}

function evaluateSpreadsheetAggregate(functionName: string, values: unknown[]): number {
  const normalizedName = functionName.trim().toUpperCase()
  const numbers = values.map(spreadsheetNumberValue)
  if (normalizedName === 'COUNT') return values.filter(value => spreadsheetNumericValue(value) != null).length
  if (normalizedName === 'AVERAGE') return numbers.length ? numbers.reduce((sum, item) => sum + item, 0) / numbers.length : 0
  if (normalizedName === 'MIN') return numbers.length ? Math.min(...numbers) : 0
  if (normalizedName === 'MAX') return numbers.length ? Math.max(...numbers) : 0
  return numbers.reduce((sum, item) => sum + item, 0)
}

function normalizeSpreadsheetPivotAggregation(value: string | null | undefined): BiSpreadsheetPivotAggregation {
  const normalized = String(value ?? 'SUM').trim().toUpperCase()
  if (normalized === 'COUNT' || normalized === 'AVERAGE' || normalized === 'MIN' || normalized === 'MAX') return normalized
  return 'SUM'
}

function normalizeSpreadsheetPivotValueFields(
  input: BiSpreadsheetPivotTableInputLike,
): Array<{ field: string; aggregation: BiSpreadsheetPivotAggregation; label: string }> {
  const rawValueFields = (input.valueFields ?? [])
    .map(valueField => ({
      field: valueField.field.trim(),
      aggregation: normalizeSpreadsheetPivotAggregation(valueField.aggregation),
      label: valueField.label?.trim() || valueField.field.trim(),
    }))
    .filter(valueField => valueField.field)

  if (rawValueFields.length) return rawValueFields
  const field = input.valueField.trim()
  return field
    ? [{ field, aggregation: normalizeSpreadsheetPivotAggregation(input.aggregation), label: field }]
    : []
}

function spreadsheetNumberValue(value: unknown): number {
  return spreadsheetNumericValue(value) ?? 0
}

function spreadsheetNumericValue(value: unknown): number | null {
  if (typeof value === 'number' && Number.isFinite(value)) return value
  if (typeof value === 'string') {
    const parsed = Number(value)
    if (Number.isFinite(parsed)) return parsed
  }
  return null
}

function spreadsheetFormulaArguments(
  expression: string,
  evaluateCell: (cellKey: string) => unknown,
): unknown[] {
  // 参数拆分需要尊重括号层级，避免 SUM(A1,(B1+C1)) 被错误切开。
  return splitSpreadsheetFormulaArguments(expression).flatMap(argument => {
    const range = argument.match(/^([A-Z]+[0-9]+):([A-Z]+[0-9]+)$/i)
    if (range) return spreadsheetCellRange(range[1], range[2]).map(cellKey => evaluateCell(cellKey))
    const directReference = argument.match(/^([A-Z]+[0-9]+)$/i)
    if (directReference) return [evaluateCell(directReference[1])]
    const numberValue = spreadsheetNumericValue(argument)
    if (numberValue != null) return [numberValue]
    const arithmetic = evaluateSpreadsheetArithmeticExpression(argument, evaluateCell)
    return arithmetic == null ? [argument] : [arithmetic]
  })
}

function splitSpreadsheetFormulaArguments(expression: string): string[] {
  const args: string[] = []
  let depth = 0
  let start = 0
  for (let index = 0; index < expression.length; index += 1) {
    const char = expression[index]
    if (char === '(') depth += 1
    if (char === ')') depth = Math.max(0, depth - 1)
    if (char === ',' && depth === 0) {
      args.push(expression.slice(start, index).trim())
      start = index + 1
    }
  }
  const last = expression.slice(start).trim()
  if (last) args.push(last)
  return args.filter(Boolean)
}

function evaluateSpreadsheetArithmeticExpression(
  expression: string,
  evaluateCell: (cellKey: string) => unknown,
): number | string | null {
  const tokens = tokenizeSpreadsheetArithmeticExpression(expression)
  if (tokens.length === 0) return null
  const values: Array<number | string> = []
  const operators: string[] = []
  const applyOperator = (): boolean => {
    const operator = operators.pop()
    if (!operator || operator === '(') return false
    const right = values.pop()
    const left = values.pop()
    if (typeof left !== 'number' || typeof right !== 'number') return false
    if (operator === '/' && right === 0) {
      // 保持电子表格常见错误语义，避免 Infinity 进入后续展示。
      values.push('#DIV/0!')
      return true
    }
    const result = switchSpreadsheetOperator(operator, left, right)
    if (result == null) return false
    values.push(result)
    return true
  }
  for (const token of tokens) {
    if (token.type === 'number') {
      values.push(Number(token.value))
      continue
    }
    if (token.type === 'cell') {
      values.push(spreadsheetNumberValue(evaluateCell(token.value)))
      continue
    }
    if (token.value === '(') {
      operators.push(token.value)
      continue
    }
    if (token.value === ')') {
      while (operators.length && operators[operators.length - 1] !== '(') {
        if (!applyOperator()) return null
        if (values[values.length - 1] === '#DIV/0!') return '#DIV/0!'
      }
      if (operators.pop() !== '(') return null
      continue
    }
    while (
      operators.length
      && operators[operators.length - 1] !== '('
      && spreadsheetOperatorPrecedence(operators[operators.length - 1]) >= spreadsheetOperatorPrecedence(token.value)
    ) {
      // 使用运算符优先级归约，保证乘除先于加减。
      if (!applyOperator()) return null
      if (values[values.length - 1] === '#DIV/0!') return '#DIV/0!'
    }
    operators.push(token.value)
  }
  while (operators.length) {
    if (!applyOperator()) return null
    if (values[values.length - 1] === '#DIV/0!') return '#DIV/0!'
  }
  return values.length === 1 ? values[0] : null
}

function tokenizeSpreadsheetArithmeticExpression(expression: string): Array<{ type: 'number' | 'cell' | 'operator'; value: string }> {
  const tokens: Array<{ type: 'number' | 'cell' | 'operator'; value: string }> = []
  let index = 0
  let expectsValue = true
  while (index < expression.length) {
    const char = expression[index]
    if (/\s/.test(char)) {
      index += 1
      continue
    }
    const rest = expression.slice(index)
    const signedNumber = expectsValue ? rest.match(/^[+-]?(?:[0-9]+(?:\.[0-9]+)?|\.[0-9]+)/) : null
    const number = signedNumber ?? rest.match(/^(?:[0-9]+(?:\.[0-9]+)?|\.[0-9]+)/)
    if (number) {
      tokens.push({ type: 'number', value: number[0] })
      index += number[0].length
      expectsValue = false
      continue
    }
    const cell = rest.match(/^[A-Z]+[0-9]+/i)
    if (cell) {
      tokens.push({ type: 'cell', value: cell[0] })
      index += cell[0].length
      expectsValue = false
      continue
    }
    if ('+-*/()'.includes(char)) {
      tokens.push({ type: 'operator', value: char })
      index += 1
      expectsValue = char !== ')'
      continue
    }
    return []
  }
  return tokens
}

function switchSpreadsheetOperator(operator: string, left: number, right: number): number | null {
  if (operator === '+') return left + right
  if (operator === '-') return left - right
  if (operator === '*') return left * right
  if (operator === '/') return left / right
  return null
}

function spreadsheetOperatorPrecedence(operator: string): number {
  return operator === '*' || operator === '/' ? 2 : 1
}

function spreadsheetCellRange(startCell: string, endCell: string): string[] {
  const start = parseSpreadsheetCellKey(startCell)
  const end = parseSpreadsheetCellKey(endCell)
  if (!start || !end) return []
  const fromColumn = Math.min(start.column, end.column)
  const toColumn = Math.max(start.column, end.column)
  const fromRow = Math.min(start.row, end.row)
  const toRow = Math.max(start.row, end.row)
  const cells: string[] = []
  for (let column = fromColumn; column <= toColumn; column += 1) {
    for (let row = fromRow; row <= toRow; row += 1) {
      cells.push(`${spreadsheetColumnName(column)}${row}`)
    }
  }
  return cells
}

function spreadsheetCellRangeFromText(rangeText: string): string[] {
  const [startCell, endCell] = rangeText.split(':').map(item => item?.trim()).filter(Boolean)
  return endCell ? spreadsheetCellRange(startCell, endCell) : [normalizeSpreadsheetCellKey(startCell ?? rangeText)]
}

function spreadsheetCellRows(cellKeys: string[]): string[][] {
  const parsed = cellKeys
    .map(cellKey => ({ cellKey, parsed: parseSpreadsheetCellKey(cellKey) }))
    .filter((item): item is { cellKey: string; parsed: { column: number; row: number } } => item.parsed != null)
  const rows = [...new Set(parsed.map(item => item.parsed.row))].sort((left, right) => left - right)
  const columns = [...new Set(parsed.map(item => item.parsed.column))].sort((left, right) => left - right)
  return rows.map(row => columns.map(column => `${spreadsheetColumnName(column)}${row}`))
}

function normalizeSpreadsheetRangeText(rangeText: string): string {
  const [startCell, endCell] = rangeText.split(':').map(item => item?.trim()).filter(Boolean)
  if (!endCell) return normalizeSpreadsheetCellKey(startCell ?? rangeText)
  return `${normalizeSpreadsheetCellKey(startCell)}:${normalizeSpreadsheetCellKey(endCell)}`
}

function parseSpreadsheetCellKey(value: string): { column: number; row: number } | null {
  const match = normalizeSpreadsheetCellKey(value).match(/^([A-Z]+)([0-9]+)$/)
  if (!match) return null
  return {
    column: spreadsheetColumnIndex(match[1]),
    row: Number(match[2]),
  }
}

function spreadsheetColumnIndex(column: string): number {
  return column.split('').reduce((total, char) => total * 26 + char.charCodeAt(0) - 64, 0)
}

function spreadsheetColumnName(index: number): string {
  let remaining = index
  let name = ''
  while (remaining > 0) {
    const offset = (remaining - 1) % 26
    name = String.fromCharCode(65 + offset) + name
    remaining = Math.floor((remaining - offset - 1) / 26)
  }
  return name
}

export function bigScreenResourceSummaryRows(
  resource: BiBigScreenResourceLike | null | undefined,
): BiEditorSummaryRow[] {
  const key = resource?.screenKey?.trim() || '-'
  const name = resource?.name?.trim() || key
  const width = displayRecordValue(resource?.size, 'width')
  const height = displayRecordValue(resource?.size, 'height')
  const layoutCount = resource?.layout?.length ?? 0
  return [
    { label: '资源', value: `${name} · ${key}` },
    {
      label: '状态',
      value: `${resource?.status?.trim() || '-'} · v${resource?.version ?? 0} · ${resource?.source?.trim() || '-'}`,
    },
    { label: '画布', value: `${width}x${height} · ${layoutCount} 组件` },
    { label: '刷新', value: refreshSummary(resource?.refresh) },
    {
      label: '背景',
      value: `${displayRecordValue(resource?.background, 'type')} · ${displayRecordValue(resource?.background, 'color')}`,
    },
  ]
}

export function spreadsheetResourceSummaryRows(
  resource: BiSpreadsheetResourceLike | null | undefined,
): BiEditorSummaryRow[] {
  const key = resource?.spreadsheetKey?.trim() || '-'
  const name = resource?.name?.trim() || key
  return [
    { label: '资源', value: `${name} · ${key}` },
    {
      label: '状态',
      value: `${resource?.status?.trim() || '-'} · v${resource?.version ?? 0} · ${resource?.source?.trim() || '-'}`,
    },
    {
      label: '工作表',
      value: `${resource?.sheets?.length ?? 0} 张 · ${displayRecordValue(resource?.dataBinding, 'datasetKey')}`,
    },
    { label: '刷新', value: displayRecordValue(resource?.dataBinding, 'refreshMode') },
    {
      label: '样式',
      value: `${displayRecordValue(resource?.style, 'theme')} · ${displayRecordValue(resource?.style, 'density')}`,
    },
  ]
}

export function buildVisualEditorDiagnosticRows(input: {
  bigScreen?: BiBigScreenResourceLike | null
  spreadsheet?: BiSpreadsheetResourceLike | null
}): BiVisualEditorDiagnosticRow[] {
  const layoutItems = input.bigScreen?.layout ?? []
  const overlapCount = countBigScreenLayoutOverlaps(layoutItems)
  const overflowCount = layoutItems.filter(item => {
    const x = Number(item.x ?? 0)
    const w = Number(item.w ?? 0)
    return Number.isFinite(x) && Number.isFinite(w) && x + w > 24
  }).length
  const mobileLayout = input.bigScreen?.mobileLayout
  const mobileColumns = Number(isPlainRecord(mobileLayout) ? mobileLayout.columns : undefined)
  const mobileEnabled = isPlainRecord(mobileLayout) && (mobileLayout.enabled === true || Number.isFinite(mobileColumns))
  const spreadsheetStats = spreadsheetVisualStats(input.spreadsheet)
  return [
    {
      key: 'bigScreenLayout',
      label: '大屏布局',
      status: overlapCount > 0 || overflowCount > 0 ? 'block' : 'pass',
      statusLabel: overlapCount > 0 || overflowCount > 0 ? '需补齐' : '可发布',
      detail: `${layoutItems.length} 组件 · ${overlapCount} 重叠 · ${overflowCount} 越界`,
    },
    {
      key: 'bigScreenMobile',
      label: '移动布局',
      status: mobileEnabled ? 'pass' : 'warn',
      statusLabel: mobileEnabled ? '可发布' : '需复核',
      detail: mobileEnabled
        ? `${Number.isFinite(mobileColumns) ? mobileColumns : 1} 列 · ${layoutItems.length} 组件已覆盖`
        : '未配置移动端布局',
    },
    {
      key: 'spreadsheetCells',
      label: '电子表格单元格',
      status: spreadsheetStats.errorCount > 0 ? 'warn' : 'pass',
      statusLabel: spreadsheetStats.errorCount > 0 ? '需复核' : '可发布',
      detail: `${spreadsheetStats.cellCount} 单元格 · ${spreadsheetStats.formulaCount} 公式 · ${spreadsheetStats.errorCount} 错误值`,
    },
    {
      key: 'spreadsheetStyles',
      label: '单元格样式',
      status: 'pass',
      statusLabel: '可发布',
      detail: `${spreadsheetStats.styleCount} 样式 · ${spreadsheetStats.conditionalFormatCount} 条件格式 · ${spreadsheetStats.pivotTableCount} 透视表`,
    },
  ]
}

function countBigScreenLayoutOverlaps(layoutItems: Record<string, unknown>[]): number {
  let count = 0
  for (let leftIndex = 0; leftIndex < layoutItems.length; leftIndex += 1) {
    for (let rightIndex = leftIndex + 1; rightIndex < layoutItems.length; rightIndex += 1) {
      if (bigScreenLayoutItemsOverlap(layoutItems[leftIndex], layoutItems[rightIndex])) count += 1
    }
  }
  return count
}

function bigScreenLayoutItemsOverlap(left: Record<string, unknown>, right: Record<string, unknown>): boolean {
  const leftX = Number(left.x ?? 0)
  const leftY = Number(left.y ?? 0)
  const leftW = Number(left.w ?? 0)
  const leftH = Number(left.h ?? 0)
  const rightX = Number(right.x ?? 0)
  const rightY = Number(right.y ?? 0)
  const rightW = Number(right.w ?? 0)
  const rightH = Number(right.h ?? 0)
  return leftX < rightX + rightW
    && leftX + leftW > rightX
    && leftY < rightY + rightH
    && leftY + leftH > rightY
}

function spreadsheetVisualStats(resource: BiSpreadsheetResourceLike | null | undefined): BiSpreadsheetVisualStats {
  return (resource?.sheets ?? []).reduce<BiSpreadsheetVisualStats>((stats, sheet) => {
    const cells = isPlainRecord(sheet.cells) ? sheet.cells : {}
    const cellStyles = isPlainRecord(sheet.cellStyles) ? sheet.cellStyles : {}
    const pivotTables = Array.isArray(sheet.pivotTables) ? sheet.pivotTables : []
    const conditionalFormats = Array.isArray(sheet.conditionalFormats) ? sheet.conditionalFormats : []
    const values = Object.values(cells)
    return {
      cellCount: stats.cellCount + values.length,
      formulaCount: stats.formulaCount + values.filter(value => String(value).trim().startsWith('=')).length,
      errorCount: stats.errorCount + values.filter(value => /^#(REF|VALUE|DIV\/0|NAME|N\/A|NUM)!?$/i.test(String(value).trim())).length,
      styleCount: stats.styleCount + Object.keys(cellStyles).length,
      pivotTableCount: stats.pivotTableCount + pivotTables.length,
      conditionalFormatCount: stats.conditionalFormatCount + conditionalFormats.length,
    }
  }, {
    cellCount: 0,
    formulaCount: 0,
    errorCount: 0,
    styleCount: 0,
    pivotTableCount: 0,
    conditionalFormatCount: 0,
  })
}

function isPlainRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null && !Array.isArray(value)
}

export function upsertBigScreenResource<T extends BiBigScreenResourceOptionLike>(resources: T[], resource: T): T[] {
  return upsertResourceByKey(resources, resource, item => item.screenKey)
}

export function upsertSpreadsheetResource<T extends BiSpreadsheetResourceOptionLike>(resources: T[], resource: T): T[] {
  return upsertResourceByKey(resources, resource, item => item.spreadsheetKey)
}

export function toResourceLockIndex<T extends BiResourceLockLike>(locks: T[]): Record<string, T> {
  return Object.fromEntries(locks.map(lock => [
    resourceLocationIndexKey(lock.resourceType, lock.resourceKey),
    lock,
  ]))
}

export function resourceCommentScopeLabel(comment: BiResourceCommentLike): string {
  return comment.widgetKey?.trim() ? `组件 ${comment.widgetKey.trim()}` : '资源'
}

export function resourceLockLabel(lock: BiResourceLockLike | undefined): string {
  if (lock?.locked !== true) return '未锁定'
  return `${lock.lockedBy?.trim() || '其他用户'} 编辑中`
}

export function resourceLockTokenFor(
  lock: BiResourceLockLike | null | undefined,
  fallbackToken: string,
  resourceType: string,
  resourceKey: string,
): string | null {
  if (lock?.locked !== true) return null
  if (resourceLocationIndexKey(lock.resourceType, lock.resourceKey) !== resourceLocationIndexKey(resourceType, resourceKey)) {
    return null
  }
  return lock.lockToken?.trim() || fallbackToken.trim() || null
}

export function publishApprovalStatusLabel(status: string | null | undefined): string {
  switch (status?.trim().toUpperCase()) {
    case 'PENDING':
      return '待审批'
    case 'APPROVED':
      return '已通过'
    case 'REJECTED':
      return '已驳回'
    default:
      return '未申请'
  }
}

export function exportApprovalStatusLabel(status: string | null | undefined): string {
  switch (status?.trim().toUpperCase()) {
    case 'PENDING':
      return '待审批'
    case 'APPROVED':
      return '已通过'
    case 'REJECTED':
      return '已驳回'
    default:
      return '无需审批'
  }
}

export interface BiCancelableExportJobLike {
  status?: string | null
}

export function isCancelableExportJob(job: BiCancelableExportJobLike | null | undefined): boolean {
  return ['QUEUED', 'PENDING_APPROVAL', 'FAILED'].includes(job?.status?.trim().toUpperCase() || '')
}

export function exportAuditDetailRows(detail: BiExportAuditDetailLike | null | undefined): BiExportAuditDetailRow[] {
  const job = detail?.job
  const request = detail?.request
  const query = request?.query
  const dimensions = query?.dimensions ?? []
  const metrics = query?.metrics ?? []
  const fields = [...dimensions, ...metrics].filter(Boolean)
  const approvalReason = request?.approvalReason || job?.approvalReason || '-'
  const partition = detail?.partition
  const storageLayout = partition?.storageLayout ?? job?.storageLayout
  const partCount = partition?.partCount ?? job?.partCount
  const generatedRows = partition?.generatedRows ?? job?.generatedRows
  const requestedRows = partition?.requestedRows ?? job?.requestedRows
  const partSize = partition?.partSize ?? job?.partSize
  const partStorageKeys = partition?.partStorageKeys ?? job?.partStorageKeys ?? []
  const rows: BiExportAuditDetailRow[] = [
    {
      label: '任务',
      value: `#${job?.id ?? '-'} · ${job?.status ?? '-'} · ${request?.exportFormat || job?.exportFormat || '-'}`,
    },
    {
      label: '数据集',
      value: query?.datasetKey || job?.resourceKey || String(job?.resourceId ?? '-'),
    },
    {
      label: '字段',
      value: fields.length > 0 ? fields.join(' / ') : '-',
    },
    {
      label: '行数',
      value: String(request?.rowLimit ?? query?.limit ?? '-'),
    },
    {
      label: '审批',
      value: `${exportApprovalStatusLabel(job?.approvalStatus)} · ${job?.requestedBy ?? '-'} -> ${job?.reviewedBy ?? '-'} · ${approvalReason}`,
    },
    {
      label: '存储',
      value: `${job?.storageProvider ?? '-'} · ${job?.storageKey ?? '-'}`,
    },
    {
      label: '下载',
      value: `${job?.downloadCount ?? 0} 次 · ${formatExportAuditTime(job?.lastDownloadedAt)}`,
    },
    {
      label: '重试',
      value: `${job?.retryCount ?? 0}/${job?.maxRetryCount ?? 0}`,
    },
    {
      label: '创建',
      value: `${job?.createdBy ?? '-'} · ${formatExportAuditTime(job?.createdAt)}`,
    },
  ]
  if (storageLayout || partCount || generatedRows || requestedRows || partSize) {
    rows.push({
      label: '分片',
      value: [
        storageLayout || '-',
        partCount ? `${formatExportAuditNumber(partCount)} 片` : null,
        generatedRows || requestedRows
          ? `${formatExportAuditNumber(generatedRows ?? 0)}/${formatExportAuditNumber(requestedRows ?? 0)} 行`
          : null,
        partSize ? `每片 ${formatExportAuditNumber(partSize)}` : null,
      ].filter(Boolean).join(' · '),
    })
  }
  if (partStorageKeys.length > 0) {
    rows.push({
      label: '分片对象',
      value: partStorageKeys.map(formatExportAuditPartStorageKey).join(' / '),
    })
  }
  return rows
}

export function exportHardeningDiagnosticRows(
  jobs: BiExportAuditJobLike[] | null | undefined,
): BiExportHardeningDiagnosticRow[] {
  const exportJobs = jobs ?? []
  const pendingApprovalCount = exportJobs.filter(job => normalizedExportStatus(job.approvalStatus || job.status) === 'PENDING_APPROVAL' || normalizedExportStatus(job.approvalStatus) === 'PENDING').length
  const expiredCount = exportJobs.filter(isExpiredExportAuditJob).length
  const partitionJobs = exportJobs.filter(job => {
    const layout = String(job.storageLayout ?? '').trim().toUpperCase()
    return layout === 'OBJECT_PER_PART_ZIP' || Number(job.partCount ?? 0) > 0
  })
  const partitionCount = partitionJobs.reduce((total, job) => total + Number(job.partCount ?? 0), 0)
  const partitionRequestedRows = partitionJobs.reduce((total, job) => total + Number(job.requestedRows ?? job.rowLimit ?? 0), 0)
  const partitionGeneratedRows = partitionJobs.reduce((total, job) => total + Number(job.generatedRows ?? 0), 0)
  const retentionDays = exportJobs
    .map(job => Number(job.retentionDays ?? 0))
    .filter(value => Number.isFinite(value) && value > 0)
  const minRetentionDays = retentionDays.length ? Math.min(...retentionDays) : 0
  const downloadCount = exportJobs.reduce((total, job) => total + Number(job.downloadCount ?? 0), 0)
  const retryingCount = exportJobs.filter(job => Number(job.retryCount ?? 0) > 0).length
  const retryExhaustedCount = exportJobs.filter(job => Boolean(job.retryExhaustedAt) || (
    Number(job.maxRetryCount ?? 0) > 0 && Number(job.retryCount ?? 0) >= Number(job.maxRetryCount ?? 0)
  )).length
  return [
    {
      key: 'exportControl',
      label: '导出控制',
      status: pendingApprovalCount > 0 || expiredCount > 0 ? 'warn' : 'pass',
      statusLabel: pendingApprovalCount > 0 || expiredCount > 0 ? '需复核' : '可发布',
      detail: `${exportJobs.length} 任务 · ${pendingApprovalCount} 审批中 · ${expiredCount} 已过期`,
    },
    {
      key: 'partitionStorage',
      label: '分片存储',
      status: partitionJobs.length > 0 ? 'pass' : 'warn',
      statusLabel: partitionJobs.length > 0 ? '可发布' : '需复核',
      detail: `${partitionJobs.length} 分片任务 · ${formatExportAuditNumber(partitionCount)} 分片 · ${formatExportAuditNumber(partitionGeneratedRows)}/${formatExportAuditNumber(partitionRequestedRows)} 行`,
    },
    {
      key: 'retentionDownload',
      label: '留存下载',
      status: expiredCount > 0 ? 'warn' : 'pass',
      statusLabel: expiredCount > 0 ? '需复核' : '可发布',
      detail: `${minRetentionDays || '-'} 天留存 · ${formatExportAuditNumber(downloadCount)} 下载 · ${expiredCount} 过期清理`,
    },
    {
      key: 'retryRecovery',
      label: '重试恢复',
      status: retryExhaustedCount > 0 ? 'block' : retryingCount > 0 ? 'warn' : 'pass',
      statusLabel: retryExhaustedCount > 0 ? '需补齐' : retryingCount > 0 ? '需复核' : '可发布',
      detail: `${retryingCount} 重试中 · ${retryExhaustedCount} 已耗尽`,
    },
  ]
}

export function alertAnomalyDiagnosticRows(
  rules: BiAlertAnomalyDiagnosticRuleLike[] | null | undefined,
): BiAlertAnomalyDiagnosticRow[] {
  const alertRules = rules ?? []
  const anomalyRules = alertRules.filter(rule => isAnomalyCondition(rule.condition))
  const disabledCount = anomalyRules.filter(rule => rule.enabled === false).length
  const periodRules = anomalyRules.filter(rule => normalizedAlertConditionValue(rule.condition?.model) === 'PERIOD_OVER_PERIOD'
    || Boolean(normalizedAlertConditionValue(rule.condition?.period)))
  const naturalBoundaryCount = periodRules.filter(rule => rule.condition?.naturalBoundary === true).length
  const periodLabels = Array.from(new Set(periodRules
    .map(rule => normalizedAlertConditionValue(rule.condition?.period))
    .filter(Boolean)))
  const calendarWindows = Array.from(new Set(periodRules
    .map(rule => Number(rule.condition?.calendarWindowHours ?? 0))
    .filter(value => Number.isFinite(value) && value > 0)
    .map(value => `${value}h`)))
  const holidayRules = anomalyRules.filter(rule => Boolean(rule.condition?.holidayComparisonDate || rule.condition?.holidayName))
  const holidaySummary = holidayRules
    .map(rule => {
      const name = String(rule.condition?.holidayName ?? 'holiday')
      const date = String(rule.condition?.holidayComparisonDate ?? '-')
      return `${name} -> ${date}`
    })
    .slice(0, 3)
    .join(' / ')
  const minSamples = Array.from(new Set(anomalyRules
    .map(rule => Number(rule.condition?.minSamples ?? 0))
    .filter(value => Number.isFinite(value) && value > 0)))
    .sort((left, right) => left - right)
  const silenceCount = anomalyRules.filter(rule => hasAlertSilenceConfig(rule.condition)).length
  return [
    {
      key: 'anomalyCoverage',
      label: '异常覆盖',
      status: anomalyRules.length > 0 && periodRules.length > 0 ? 'pass' : anomalyRules.length > 0 ? 'warn' : 'block',
      statusLabel: anomalyRules.length > 0 && periodRules.length > 0 ? '可发布' : anomalyRules.length > 0 ? '需复核' : '需补齐',
      detail: `${anomalyRules.length} 异常 · ${periodRules.length} 同环比 · ${disabledCount} 停用`,
    },
    {
      key: 'periodBoundary',
      label: '周期边界',
      status: periodRules.length > 0 && naturalBoundaryCount > 0 ? 'pass' : periodRules.length > 0 ? 'warn' : 'block',
      statusLabel: periodRules.length > 0 && naturalBoundaryCount > 0 ? '可发布' : periodRules.length > 0 ? '需复核' : '需补齐',
      detail: `${periodLabels.length ? periodLabels.join(' / ') : '-'} · ${naturalBoundaryCount} 自然边界 · ${calendarWindows.length ? calendarWindows.join('/') : '-'} 窗口`,
    },
    {
      key: 'holidayComparison',
      label: '节假日映射',
      status: holidayRules.length > 0 ? 'pass' : periodRules.length > 0 ? 'warn' : 'block',
      statusLabel: holidayRules.length > 0 ? '可发布' : periodRules.length > 0 ? '需复核' : '需补齐',
      detail: `${holidayRules.length} 节假日 · ${holidaySummary || '-'}`,
    },
    {
      key: 'sampleSilence',
      label: '样本静默',
      status: disabledCount > 0 || silenceCount > 0 ? 'warn' : 'pass',
      statusLabel: disabledCount > 0 || silenceCount > 0 ? '需复核' : '可发布',
      detail: `最小样本 ${minSamples.length ? minSamples.join('/') : '-'} · ${silenceCount} 静默 · ${disabledCount} 停用`,
    },
  ]
}

export function queryHistoryDetailRows(detail: BiQueryHistoryDetailLike | null | undefined): BiQueryHistoryDetailRow[] {
  const request = detail?.request
  const dimensions = request?.dimensions ?? []
  const metrics = request?.metrics ?? []
  const fields = [...dimensions, ...metrics].filter(Boolean)
  const filters = request?.filters ?? []
  const sorts = request?.sorts ?? []
  return [
    {
      label: '查询',
      value: `#${detail?.id ?? '-'} · ${detail?.status ?? '-'} · ${detail?.username ?? '-'}`,
    },
    {
      label: '数据集',
      value: request?.datasetKey || detail?.datasetKey || '-',
    },
    {
      label: '字段',
      value: fields.length > 0 ? fields.join(' / ') : '-',
    },
    {
      label: '过滤',
      value: filters.length > 0 ? filters.map(formatQueryHistoryFilter).join(' / ') : '-',
    },
    {
      label: '排序',
      value: sorts.length > 0 ? sorts.map(sort => `${sort.field} ${sort.direction}`).join(' / ') : '-',
    },
    {
      label: '分页',
      value: `limit ${request?.limit ?? '-'} · offset ${request?.offset ?? 0}`,
    },
    {
      label: '结果',
      value: `${detail?.rowCount ?? 0} 行 · ${detail?.durationMs ?? 0} ms`,
    },
    {
      label: 'SQL Hash',
      value: detail?.sqlHash || '-',
    },
    {
      label: '错误',
      value: detail?.errorMessage || '-',
    },
    {
      label: '创建',
      value: formatExportAuditTime(detail?.createdAt),
    },
  ]
}

export function queryGovernanceSummaryRows(
  summary: BiQueryGovernanceSummaryLike | null | undefined,
): BiQueryGovernanceSummaryRow[] {
  const slowestDataset = [...(summary?.datasets ?? [])]
    .sort((left, right) => (right.maxDurationMs ?? 0) - (left.maxDurationMs ?? 0))[0]
  return [
    {
      label: '查询总量',
      value: `${summary?.totalQueries ?? 0} 次 · 平均 ${summary?.averageDurationMs ?? 0} ms`,
    },
    {
      label: '慢查询',
      value: `${summary?.slowQueries ?? 0} 次 · 超时策略 ${summary?.timeoutPolicyMs ?? 0} ms`,
    },
    {
      label: '失败查询',
      value: `${summary?.failedQueries ?? 0} 次`,
    },
    {
      label: '缓存命中',
      value: `${summary?.cacheHits ?? 0} 次`,
    },
    {
      label: '数据集配额',
      value: `${summary?.datasetQuotaRows ?? 0} 行`,
    },
    {
      label: '最慢数据集',
      value: slowestDataset
        ? `${slowestDataset.datasetKey ?? '-'} · ${slowestDataset.maxDurationMs ?? 0} ms · ${slowestDataset.totalQueries ?? 0} 次${formatDatasetGovernancePolicy(slowestDataset)}`
        : '-',
    },
    {
      label: '慢查询归因',
      value: slowestDataset
        ? `${slowestDataset.datasetKey ?? '-'} · 慢 ${slowestDataset.slowQueries ?? 0}/${slowestDataset.totalQueries ?? 0} · 超阈 ${slowestDataset.maxOverPolicyMs ?? 0} ms · 失败 ${slowestDataset.slowFailures ?? 0} · 未命中缓存 ${slowestDataset.slowCacheMisses ?? 0} · 最大 ${slowestDataset.maxRowCount ?? 0} 行`
        : '-',
    },
  ]
}

function formatDatasetGovernancePolicy(dataset: BiQueryGovernanceDatasetStatsLike): string {
  if (dataset.timeoutPolicyMs == null && dataset.quotaRows == null) {
    return ''
  }
  return ` · ${dataset.timeoutPolicyMs ?? '-'} ms/${dataset.quotaRows ?? '-'} 行`
}

export function datasourceHealthHistoryRows(
  snapshots: BiDatasourceHealthSnapshotLike[] | null | undefined,
): BiDatasourceHealthHistoryRow[] {
  return (snapshots ?? []).map(snapshot => ({
    key: `${snapshot.sourceKey}-${snapshot.checkedAt}`,
    source: `${snapshot.sourceKey} / ${snapshot.sourceType}`,
    status: snapshot.available ? '正常' : '异常',
    message: snapshot.message || '-',
    checkedAt: snapshot.checkedAt,
  }))
}

export function datasourceHealthSloRows(
  summary: BiDatasourceHealthSloLike | null | undefined,
): BiDatasourceHealthSloRow[] {
  const weakestSource = [...(summary?.sources ?? [])]
    .sort((left, right) => (left.availabilityRate ?? 100) - (right.availabilityRate ?? 100))[0]
  const rows = [
    {
      label: '整体可用率',
      value: `${summary?.availabilityRate ?? 100}% · ${summary?.availableChecks ?? 0}/${summary?.totalChecks ?? 0} 正常`,
    },
    {
      label: '异常检查',
      value: `${summary?.unavailableChecks ?? 0} 次`,
    },
    {
      label: '最弱数据源',
      value: weakestSource
        ? `${weakestSource.sourceKey ?? '-'} / ${weakestSource.sourceType ?? '-'} · ${weakestSource.availabilityRate ?? 100}% · ${weakestSource.lastMessage || '-'}`
        : '-',
    },
  ]
  if (weakestSource?.riskLevel || weakestSource?.recommendedAction) {
    rows.push({
      label: '治理建议',
      value: `${weakestSource.riskLevel || '-'} · ${weakestSource.recommendedAction || '-'}`,
    })
  }
  return rows
}

export function datasourceConnectorRows(
  connectors: BiDatasourceConnectorCapabilityLike[] | null | undefined,
): BiDatasourceConnectorRow[] {
  return (connectors ?? []).map(connector => {
    const connectorType = connector.connectorType || '-'
    return {
      key: connectorType,
      connector: `${connector.label || connectorType} / ${connectorType}`,
      category: connector.sourceCategory || '-',
      capacity: connector.capacityCategory
        ? `${connector.capacityCategory}${connector.capacityNote ? ` · ${connector.capacityNote}` : ''}`
        : '-',
      modes: formatSlashList(connector.supportedModes),
      status: connector.supportStatus || '-',
      capabilities: formatDatasourceConnectorCapabilities(connector),
    }
  })
}

export function datasourceCapacityPolicyRows(
  connectors: BiDatasourceConnectorCapabilityLike[] | null | undefined,
): BiDatasourceCapacityPolicyRow[] {
  return (connectors ?? []).map(connector => {
    const connectorType = connector.connectorType || '-'
    return {
      key: connectorType,
      connector: `${connector.label || connectorType} / ${connectorType}`,
      ...datasourceCapacityPolicy(connectorType, connector.sourceCategory, connector.capacityCategory),
    }
  })
}

export function datasourceAdvancedCapabilityRows(
  connectors: BiDatasourceConnectorCapabilityLike[] | null | undefined,
): BiDatasourceAdvancedCapabilityRow[] {
  return (connectors ?? []).map(connector => {
    const connectorType = connector.connectorType || '-'
    return {
      key: connectorType,
      connector: `${connector.label || connectorType} / ${connectorType}`,
      ...datasourceAdvancedCapability(connector),
    }
  })
}

function datasourceAdvancedCapability(
  connector: BiDatasourceConnectorCapabilityLike,
): Omit<BiDatasourceAdvancedCapabilityRow, 'key' | 'connector'> {
  const connectorType = (connector.connectorType || '').toUpperCase()
  const sourceCategory = (connector.sourceCategory || '').toUpperCase()
  const capacityCategory = (connector.capacityCategory || '').toUpperCase()
  const supportStatus = (connector.supportStatus || '').toUpperCase()
  const modes = (connector.supportedModes ?? []).map(mode => mode.toUpperCase())
  const supportsSql = connector.supportsSqlDataset === true
  const supportsTable = connector.supportsTableDataset === true
  if (supportStatus === 'PLANNED') {
    return {
      quickEngine: '规划中 · 仓库级抽取容量未开放',
      crossSourceModeling: '阻断 · 连接器未开放前不可发布跨源模型',
      selfService: '阻断 · connector status PLANNED',
      semanticAuthoring: '暂不可用',
      risk: '高 · 需要先完成原生连接器和容量治理',
    }
  }
  if (connectorType === 'API' || sourceCategory === 'HTTP' || capacityCategory.startsWith('HTTP_EXTRACT')) {
    return {
      quickEngine: '必需 · HTTP JSON 仅通过抽取物化分析',
      crossSourceModeling: '受限 · 需先抽取成物化表后参与关联',
      selfService: '不支持 · API/应用源不直接进入自助取数',
      semanticAuthoring: semanticAuthoringLabel(supportsSql, supportsTable),
      risk: '中 · 受 10MB/100 列/1000 行预览和源端限流约束',
    }
  }
  if (sourceCategory === 'APP' || capacityCategory.startsWith('APP_EXTRACT')) {
    return {
      quickEngine: '必需 · 应用数据源通过抽取物化分析',
      crossSourceModeling: '受限 · 需先抽取成物化表后参与关联',
      selfService: '不支持 · API/应用源不直接进入自助取数',
      semanticAuthoring: semanticAuthoringLabel(supportsSql, supportsTable),
      risk: '中 · 应用凭证、同步周期和字段漂移需治理',
    }
  }
  if (connectorType === 'CSV_EXCEL' || sourceCategory === 'FILE' || capacityCategory.startsWith('FILE_EXTRACT')) {
    return {
      quickEngine: '必需 · 文件上传后以抽取表参与分析',
      crossSourceModeling: '受限 · 探索空间文件需物化后再治理关联',
      selfService: '不支持 · 探索空间上传源不进入自助取数',
      semanticAuthoring: semanticAuthoringLabel(supportsSql, supportsTable),
      risk: '中 · 文件大小、Sheet 数和字段类型漂移需复核',
    }
  }
  if (modes.includes('EXTRACT') && !modes.includes('DIRECT_QUERY')) {
    return {
      quickEngine: '必需 · 仅抽取模式参与分析',
      crossSourceModeling: '受限 · 跨源关联需开启 Quick 引擎抽取',
      selfService: supportsTable ? '支持 · 物化为普通数据集后可进入自助取数' : '受限 · 需先开放表数据集',
      semanticAuthoring: semanticAuthoringLabel(supportsSql, supportsTable),
      risk: '中 · 需关注抽取容量、调度和物化保留',
    }
  }
  return {
    quickEngine: '可选 · 直连/缓存优先，跨源或大数据量时启用抽取',
    crossSourceModeling: '支持 · 跨源关联需开启 Quick 引擎抽取',
    selfService: '支持 · 普通数据集可进入自助取数',
    semanticAuthoring: semanticAuthoringLabel(supportsSql, supportsTable),
    risk: connector.supportsConnectionTest && connector.supportsSchemaSync && connector.supportsCredentials
      ? '低 · 连接测试、schema 同步和凭证治理齐全'
      : '中 · 需补齐连接测试、schema 同步或凭证治理',
  }
}

function semanticAuthoringLabel(supportsSql: boolean, supportsTable: boolean): string {
  if (supportsSql && supportsTable) return 'SQL + 表数据集'
  if (supportsSql) return 'SQL 数据集'
  if (supportsTable) return '表数据集'
  return '暂不可用'
}

function datasourceCapacityPolicy(
  connectorType: string | null | undefined,
  sourceCategory: string | null | undefined,
  capacityCategory: string | null | undefined,
): Omit<BiDatasourceCapacityPolicyRow, 'key' | 'connector'> {
  const normalizedConnector = (connectorType || '').toUpperCase()
  const normalizedSource = (sourceCategory || '').toUpperCase()
  const normalizedCapacity = (capacityCategory || '').toUpperCase()
  if (normalizedConnector === 'API' || normalizedSource === 'HTTP' || normalizedCapacity.startsWith('HTTP_EXTRACT')) {
    return {
      capacityPool: 'HTTP 抽取小流量池',
      budget: '直连预览上限 10MB / 100 列 / 1000 行；抽取分页默认每页 1000 行',
      eligibility: '不进入自助取数',
      guardrails: 'JSON 响应解析、模板变量、抽取刷新和源端限流保护',
    }
  }
  if (normalizedSource === 'APP' || normalizedCapacity.startsWith('APP_EXTRACT')) {
    return {
      capacityPool: '应用抽取小流量池',
      budget: 'SaaS/API 应用源走 HTTP JSON 抽取；按应用连接器独立计量',
      eligibility: '自助取数需落成普通数据集后评估',
      guardrails: '应用凭证、同步周期、字段选择和容量分类隔离',
    }
  }
  if (normalizedConnector === 'CSV_EXCEL' || normalizedSource === 'FILE' || normalizedCapacity.startsWith('FILE_EXTRACT')) {
    return {
      capacityPool: '探索空间文件池',
      budget: 'CSV/Excel 建议 50MB 内、100 列内；Excel 最多解析 5 个 Sheet',
      eligibility: '探索空间上传源不支持自助取数',
      guardrails: 'UTF-8 编码、字段类型校验、追加/替换文件需重新匹配',
    }
  }
  return {
    capacityPool: '交互查询池',
    budget: '直连/缓存查询 · 受租户并发池和查询行数配额控制',
    eligibility: '可用于自助取数',
    guardrails: '连接测试、schema 同步、SQL/表数据集建模',
  }
}

export function datasourceOnboardingRows(
  sources: BiDatasourceOnboardingLike[] | null | undefined,
): BiDatasourceOnboardingRow[] {
  return (sources ?? []).map((source, index) => {
    const sourceKey = source.sourceKey || `source-${source.id ?? index}`
    const type = source.type || '-'
    const supportedModes = formatSlashList(source.supportedModes)
    return {
      key: sourceKey,
      id: source.id,
      source: `${source.name || '-'} · ${sourceKey}`,
      connector: `${source.connectorType || '-'} / ${type}`,
      status: `${source.enabled === false ? '停用' : '启用'} · ${source.supportStatus || '-'}`,
      connection: `${source.connectionMode || '-'} · ${supportedModes}`,
      schema: `${source.schemaSyncStatus || '-'} · ${source.tableCount ?? 0} 表${source.lastSyncedAt ? ` · ${source.lastSyncedAt}` : ''}`,
      credential: `${source.maskedUsername || '-'} · ${source.maskedUrl || '-'}`,
    }
  })
}

export function datasourceNextActionRows(
  sources: BiDatasourceOnboardingLike[] | null | undefined,
): BiDatasourceNextActionRow[] {
  return (sources ?? []).map((source, index) => {
    const sourceKey = source.sourceKey || `source-${source.id ?? index}`
    const connectorType = (source.connectorType || source.type || '').toUpperCase()
    const tableCount = source.tableCount ?? 0
    const ready = (source.schemaSyncStatus || '').toUpperCase() === 'SUCCESS' && tableCount > 0
    const isApi = connectorType === 'API' || sourceKey.toLowerCase().startsWith('api-')
    const isFile = connectorType === 'CSV_EXCEL' || connectorType === 'FILE' || sourceKey.toLowerCase().startsWith('file-')
    return {
      key: sourceKey,
      source: source.name || sourceKey,
      readiness: ready ? `可建模 · ${tableCount} 张表` : '待同步 schema',
      nextAction: datasourceNextAction(isApi, isFile, ready),
      limitations: datasourceLimitations(isApi, isFile),
    }
  })
}

function datasourceNextAction(isApi: boolean, isFile: boolean, ready: boolean): string {
  if (isApi) {
    return ready ? '创建表数据集并配置抽取刷新' : '完成 API 预览、解析响应字段并同步 schema'
  }
  if (isFile) {
    return ready ? '创建表数据集并进入报表/门户分析' : '上传/预览文件后同步 schema，再创建表数据集'
  }
  return ready ? '创建 SQL/表数据集，按需开启缓存或抽取加速' : '完成连接测试并同步 schema'
}

function datasourceLimitations(isApi: boolean, isFile: boolean): string {
  if (isApi) {
    return 'API 数据源不进入自助取数；直连小数据量受 10MB/100 列/1000 行约束'
  }
  if (isFile) {
    return '文件数据源适合探索空间和报表分析；自助取数需使用非探索空间数据集'
  }
  return '可用于自助取数；跨源数据集和高级同环比导出仍需治理校验'
}

export function datasourceConnectionTestRows(
  result: BiDatasourceConnectionTestLike | null | undefined,
): BiDatasourceConnectionTestRow[] {
  return [
    {
      label: '状态',
      value: `${result?.success ? '成功' : '失败'} · ${result?.message || '-'}`,
    },
    {
      label: '数据源',
      value: `${result?.sourceKey || '-'} / ${result?.connectorType || '-'}`,
    },
    {
      label: '数据库',
      value: `${result?.databaseProductName || '-'} ${result?.databaseProductVersion || ''}`.trim(),
    },
    {
      label: '耗时',
      value: `${result?.durationMs ?? 0} ms`,
    },
    {
      label: '检查时间',
      value: result?.checkedAt || '-',
    },
  ]
}

export function datasourceSchemaPreviewRows(
  preview: BiDatasourceSchemaPreviewLike | null | undefined,
): BiDatasourceSchemaPreviewRow[] {
  return (preview?.tables ?? []).map((table, index) => {
    const tableName = table.name || `table-${index}`
    const columns = table.columns ?? []
    return {
      key: tableName,
      table: `${tableName} / ${table.tableType || '-'}`,
      columns: columns.length > 0
        ? columns.map(column => `${column.name || '-'} ${column.typeName || '-'} ${column.nullable ? '可空' : '必填'}`).join(' · ')
        : '-',
      columnCount: `${columns.length} 字段`,
      checkedAt: preview?.checkedAt || '-',
    }
  })
}

export function datasourceSchemaSnapshotRows(
  snapshot: BiDatasourceSchemaSnapshotLike | null | undefined,
): BiDatasourceSchemaSnapshotRow[] {
  return [
    {
      label: '状态',
      value: `${snapshot?.syncStatus || '-'} · ${snapshot?.tableCount ?? 0} 表 · ${snapshot?.columnCount ?? 0} 字段`,
    },
    {
      label: '数据源',
      value: `${snapshot?.name || '-'} · ${snapshot?.sourceKey || '-'} / ${snapshot?.connectorType || '-'}`,
    },
    {
      label: '同步人',
      value: snapshot?.syncedBy || '-',
    },
    {
      label: '同步时间',
      value: snapshot?.syncedAt || '-',
    },
    {
      label: '错误',
      value: snapshot?.errorMessage || '-',
    },
  ]
}

export function datasourceSchemaSnapshotHistoryRows(
  snapshots: BiDatasourceSchemaSnapshotLike[] | null | undefined,
): BiDatasourceSchemaSnapshotHistoryRow[] {
  return (snapshots ?? []).map((snapshot, index) => ({
    key: String(snapshot.id ?? `${snapshot.sourceKey ?? 'snapshot'}-${snapshot.syncedAt ?? index}`),
    source: `${snapshot.name || '-'} · ${snapshot.sourceKey || '-'}`,
    status: snapshot.syncStatus || '-',
    schema: `${snapshot.tableCount ?? 0} 表 · ${snapshot.columnCount ?? 0} 字段`,
    syncedAt: snapshot.syncedAt || '-',
    syncedBy: snapshot.syncedBy || '-',
    error: snapshot.errorMessage || '-',
  }))
}

export function buildDatasourceTableDatasetCommand(
  snapshot: BiDatasourceSchemaSnapshotLike,
  tableName: string,
  apiResponseVariables: Record<string, string> = {},
): BiDatasourceTableDatasetCommandLike {
  const table = (snapshot.tables ?? []).find(item => item.name === tableName)
  const columns = (table?.columns ?? [])
    .filter(column => !!column.name?.trim())
    .sort((a, b) => (a.ordinalPosition ?? 0) - (b.ordinalPosition ?? 0))
    .map(column => column.name?.trim() ?? '')
  const apiSnapshot = (snapshot.connectorType || '').toUpperCase() === 'API'
    || (snapshot.sourceKey || '').toLowerCase().startsWith('api-')
  const tenantColumn = columns.find(column => column.toLowerCase() === 'tenant_id') ?? 'tenant_id'
  const selectedColumns = columns.length > 0 ? columns : []
  const command: BiDatasourceTableDatasetCommandLike = {
    dataSourceConfigId: snapshot.dataSourceConfigId ?? 0,
    tableName,
    datasetKey: normalizeDatasetKey(`${snapshot.sourceKey || 'datasource'}_${tableName}`),
    name: `${snapshot.name || snapshot.sourceKey || 'Datasource'} ${tableName}`,
    tenantColumn,
    selectedColumns: apiSnapshot
      ? selectedColumns.filter(column => column.toLowerCase() !== tenantColumn.toLowerCase())
      : selectedColumns,
  }
  if (apiSnapshot && Object.keys(apiResponseVariables).length > 0) {
    command.apiResponseVariables = apiResponseVariables
  }
  return command
}

export function buildDatasourceMultiTableDatasetCommand(
  snapshot: BiDatasourceSchemaSnapshotLike,
  input: BiDatasourceMultiTableModelInputLike,
): BiDatasourceMultiTableDatasetCommandLike {
  const snapshotTables = snapshot.tables ?? []
  const requestedTableNames = uniqueStrings(input.tableNames ?? [])
    .filter(tableName => snapshotTables.some(table => table.name === tableName))
  const baseTableName = trimValue(input.baseTableName)
    || requestedTableNames[0]
    || snapshotTables[0]?.name
    || ''
  const tableNames = uniqueStrings([baseTableName, ...requestedTableNames])
    .filter(tableName => snapshotTables.some(table => table.name === tableName))
  const tables = tableNames.map(tableName => {
    const table = snapshotTables.find(item => item.name === tableName)
    return {
      tableName,
      alias: normalizeDatasourceTableAlias(tableName),
      selectedColumns: datasourceTableColumnNames(table),
    }
  })
  const aliasByTableName = new Map(tables.map(table => [table.tableName, table.alias]))
  const joins = (input.joins ?? [])
    .map(join => {
      const conditions = normalizeDatasourceJoinConditions(join)
      const firstCondition = conditions[0]
      return {
        joinType: trimValue(join.joinType).toUpperCase() || 'LEFT',
        leftAlias: aliasByTableName.get(trimValue(join.leftTableName)) ?? normalizeDatasourceTableAlias(join.leftTableName),
        leftColumn: firstCondition?.leftColumn ?? '',
        rightAlias: aliasByTableName.get(trimValue(join.rightTableName)) ?? normalizeDatasourceTableAlias(join.rightTableName),
        rightColumn: firstCondition?.rightColumn ?? '',
        conditions,
      }
    })
    .filter(join => join.leftAlias && join.rightAlias && join.conditions.length > 0)
  const graphNodeCoordinates = new Map<string, { x: number; y: number }>()
  for (const node of input.graphNodes ?? []) {
    const x = sanitizeDatasourceGraphCoordinate(node.x)
    const y = sanitizeDatasourceGraphCoordinate(node.y)
    if (x === null || y === null) continue
    const tableName = trimValue(node.tableName)
    const alias = trimValue(node.alias)
    if (tableName) graphNodeCoordinates.set(tableName, { x, y })
    if (alias) graphNodeCoordinates.set(alias, { x, y })
  }
  const graphNodes = tables.map((table, index) => {
    const coordinate = graphNodeCoordinates.get(table.tableName) ?? graphNodeCoordinates.get(table.alias)
    return {
      tableName: table.tableName,
      alias: table.alias,
      x: coordinate?.x ?? 80 + (index % 3) * 280,
      y: coordinate?.y ?? 80 + Math.floor(index / 3) * 180,
    }
  })
  return {
    dataSourceConfigId: snapshot.dataSourceConfigId ?? 0,
    datasetKey: normalizeDatasetKey(`${snapshot.sourceKey || 'datasource'}_${tableNames.join('_')}`),
    name: `${snapshot.name || snapshot.sourceKey || 'Datasource'} ${tableNames.join(' + ')}`,
    baseTableName,
    tenantColumn: trimValue(input.tenantColumn)
      || tables[0]?.selectedColumns.find(column => column.toLowerCase() === 'tenant_id')
      || 'tenant_id',
    tables,
    joins,
    graph: {
      layoutMode: 'GRAPH_CANVAS',
      nodes: graphNodes,
    },
  }
}

export function buildDatasourceRelationshipDiagnosticRows(
  input: BiDatasourceMultiTableModelInputLike | null | undefined,
): BiDatasourceRelationshipDiagnosticRow[] {
  const tableNames = uniqueStrings(input?.tableNames ?? [])
  const baseTableName = trimValue(input?.baseTableName) || tableNames[0] || '-'
  const joins = input?.joins ?? []
  const normalizedJoinTypes = uniqueStrings(joins.map(join => trimValue(join.joinType).toUpperCase() || 'LEFT'))
  const rawConditions = joins.flatMap(join => {
    const conditions = join.conditions?.length
      ? join.conditions
      : [{ leftColumn: join.leftColumn, operator: '=', rightColumn: join.rightColumn }]
    return conditions.map(condition => ({
      leftColumn: trimValue(condition.leftColumn),
      rightColumn: trimValue(condition.rightColumn),
      connector: normalizeDatasourceJoinConnector(condition.connector),
      groupStart: condition.groupStart === true,
      groupEnd: condition.groupEnd === true,
    }))
  }).filter(condition => condition.leftColumn && condition.rightColumn)
  const complexJoinCount = joins.filter(join => {
    const conditionCount = join.conditions?.length ?? (join.leftColumn && join.rightColumn ? 1 : 0)
    return conditionCount > 1
  }).length
  const orConditionCount = rawConditions.filter(condition => condition.connector === 'OR').length
  const groupedConditionCount = Math.max(
    rawConditions.filter(condition => condition.groupStart).length,
    rawConditions.filter(condition => condition.groupEnd).length,
  )
  const relatedTables = new Set<string>()
  if (baseTableName && baseTableName !== '-') relatedTables.add(baseTableName)
  joins.forEach(join => {
    const leftTableName = trimValue(join.leftTableName)
    const rightTableName = trimValue(join.rightTableName)
    if (leftTableName) relatedTables.add(leftTableName)
    if (rightTableName) relatedTables.add(rightTableName)
  })
  const missingCoverage = tableNames.filter(tableName => !relatedTables.has(tableName))
  const joinDepth = joins.length
  const overQuickBiDepth = joinDepth > 5
  const hasFullJoin = normalizedJoinTypes.includes('FULL')
  const conditionStatus: BiDatasourceRelationshipDiagnosticStatus = orConditionCount > 0 || groupedConditionCount > 0 ? 'warn' : 'pass'

  return [
    {
      key: 'tables',
      label: '建模表',
      status: tableNames.length >= 2 ? 'pass' : 'block',
      statusLabel: tableNames.length >= 2 ? '可建模' : '需补齐',
      detail: `${tableNames.length} 表 · 主表 ${baseTableName}`,
    },
    {
      key: 'joinDepth',
      label: '关联层级',
      status: overQuickBiDepth ? 'warn' : 'pass',
      statusLabel: overQuickBiDepth ? '需复核' : '可建模',
      detail: `${joinDepth} 层 · Quick BI 物理模型建议不超过 5 层`,
    },
    {
      key: 'joinTypes',
      label: 'Join 类型',
      status: hasFullJoin ? 'warn' : 'pass',
      statusLabel: hasFullJoin ? '需复核' : '可建模',
      detail: `${normalizedJoinTypes.join(' / ') || '-'}${hasFullJoin ? ' · 包含 FULL JOIN，需确认空值扩散' : ''}`,
    },
    {
      key: 'conditions',
      label: '关联条件',
      status: conditionStatus,
      statusLabel: conditionStatus === 'pass' ? '可建模' : '需复核',
      detail: `${rawConditions.length} 条件 · ${complexJoinCount} 复合关系 · ${orConditionCount} OR · ${groupedConditionCount} 分组`,
    },
    {
      key: 'coverage',
      label: '关系覆盖',
      status: missingCoverage.length ? 'block' : 'pass',
      statusLabel: missingCoverage.length ? '需补齐' : '可建模',
      detail: missingCoverage.length
        ? `未覆盖: ${missingCoverage.join(', ')}`
        : `已覆盖 ${relatedTables.size}/${tableNames.length} 表`,
    },
  ]
}

export function buildSqlDatasetParameterDrafts(
  sqlTemplate: string | null | undefined,
  existingParameters: BiSqlDatasetParameterDraftLike[] | null | undefined = [],
): BiSqlDatasetParameterResolvedLike[] {
  const existingByKey = new Map<string, BiSqlDatasetParameterDraftLike>()
  for (const parameter of existingParameters ?? []) {
    const key = normalizeSqlParameterKey(parameter.key)
    if (key) existingByKey.set(key, parameter)
  }
  return sqlTemplateParameterKeys(sqlTemplate).map(key => {
    const existing = existingByKey.get(key)
    return {
      key,
      dataType: normalizeSqlParameterDataType(existing?.dataType, key),
      required: existing?.required !== false,
      defaultValue: trimValue(existing?.defaultValue),
      allowedValuesText: trimValue(existing?.allowedValuesText)
        || (existing?.allowedValues ?? []).map(value => trimValue(value)).filter(Boolean).join(', '),
    }
  })
}

export function buildSqlDatasetDraftResource(input: BiSqlDatasetDraftInputLike) {
  const sqlTemplate = trimValue(input.sqlTemplate)
  const sqlParameterOrder = sqlTemplateParameterKeys(sqlTemplate)
  const parameters = buildSqlDatasetParameterDrafts(sqlTemplate, input.parameters)
  const sqlParameters = parameters.map(parameter => {
    const definition: Record<string, unknown> = {
      key: parameter.key,
      dataType: parameter.dataType,
      required: parameter.required,
      allowedValues: sqlAllowedValues(parameter),
    }
    if (parameter.defaultValue) definition.defaultValue = parameter.defaultValue
    return definition
  })
  const model: Record<string, unknown> = {
    sqlApprovalRequired: true,
    sqlTemplate,
    sqlParameterOrder,
    sqlParameters,
  }
  if (input.dataSourceConfigId != null) {
    model.dataSourceConfigId = input.dataSourceConfigId
  }
  return {
    datasetKey: normalizeDatasetKey(input.datasetKey || input.name || 'sql_dataset'),
    name: trimValue(input.name) || normalizeDatasetKey(input.datasetKey || 'sql_dataset'),
    datasetType: 'SQL',
    tableExpression: sqlTemplate,
    tenantColumn: trimValue(input.tenantColumn) || 'tenant_id',
    model,
    fields: (input.fields ?? [])
      .map((field, index) => normalizeSqlDatasetField(field, index))
      .filter(field => field.fieldKey && field.columnExpression),
    metrics: (input.metrics ?? [])
      .map(metric => normalizeSqlDatasetMetric(metric))
      .filter(metric => metric.metricKey && metric.expression),
    status: 'DRAFT',
    source: 'CLIENT',
  }
}

export function buildSqlDatasetSampleProfileRows(
  input: BiSqlDatasetSampleProfileInputLike | null | undefined,
): BiSqlDatasetSampleProfileRow[] {
  const columns = input?.columns ?? []
  const rows = input?.rows ?? []
  const totalRows = rows.length || Math.max(0, input?.rowCount ?? 0)
  return columns.map(column => {
    const values = rows.map(row => row[column.key])
    const filledValues = values.filter(value => !isEmptySampleProfileValue(value))
    const uniqueValues = uniqueSampleProfileValues(filledValues)
    return {
      key: column.key,
      field: column.key,
      role: trimValue(column.role) || '-',
      dataType: trimValue(column.dataType) || '-',
      filled: `${filledValues.length}/${totalRows}`,
      unique: String(uniqueValues.length),
      samples: uniqueValues.slice(0, 3).map(formatSampleProfileValue).join(' / ') || '-',
    }
  })
}

export function buildSqlDatasetImpactRows(input: BiSqlDatasetImpactInputLike | null | undefined): BiSqlDatasetImpactRow[] {
  const lineage = input?.lineage
  const impact = input?.impact
  const sourceParts = [
    lineage?.dataSourceConfigId != null ? `datasource #${lineage.dataSourceConfigId}` : null,
    sqlImpactList(lineage?.sourceTables),
    lineage?.tenantColumn ? `tenant ${lineage.tenantColumn}` : null,
  ].filter(Boolean)
  const referenceParts = [
    sqlImpactList(lineage?.referencedFields),
    sqlImpactList(lineage?.referencedMetrics),
  ].filter(value => value && value !== '-')
  const governanceGates = [
    ...(impact?.governanceGates ?? []),
    ...(lineage?.approvalRequired ? ['发布需审批'] : []),
  ]
  return [
    { key: 'assets', label: '影响资产', value: sqlImpactList(impact?.impactedAssetTypes) },
    { key: 'lineage', label: '血缘来源', value: sourceParts.join(' · ') || '-' },
    { key: 'parameters', label: '运行参数', value: sqlImpactList(lineage?.parameterKeys) },
    { key: 'references', label: '引用字段', value: referenceParts.join(' · ') || '-' },
    { key: 'governance', label: '治理门禁', value: sqlImpactList(governanceGates) },
    { key: 'warnings', label: '风险提示', value: sqlImpactList(impact?.warnings) },
  ]
}

export function buildSqlDatasetReadinessRows(
  input: BiSqlDatasetReadinessInputLike | null | undefined,
): BiSqlDatasetReadinessRow[] {
  const fields = input?.draft?.fields ?? []
  const metrics = input?.draft?.metrics ?? []
  const parameters = input?.parameters ?? []
  const preview = input?.preview
  const visibleFieldCount = fields.filter(field => field.visible !== false).length
  const dimensionConstrainedMetricCount = metrics.filter(metric => (metric.allowedDimensions ?? []).length > 0).length
  const metadataIssues = [
    fields.length === 0 ? '缺少字段' : '',
    metrics.length === 0 ? '缺少指标' : '',
    metrics.some(metric => !trimValue(metric.owner)) ? '指标缺少负责人' : '',
    metrics.some(metric => !trimValue(metric.description)) ? '指标缺少描述' : '',
  ].filter(Boolean)
  const missingDefaults = parameters
    .filter(parameter => parameter.required && !trimValue(parameter.defaultValue))
    .map(parameter => parameter.key)
  const sampleBlocked = !preview
    || preview.sampleExecuted !== true
    || !!trimValue(preview.executionError)
    || (preview.columns ?? []).length === 0
  const lineage = preview?.lineage
  const gates = preview?.impact?.governanceGates ?? []
  const sourceTables = lineage?.sourceTables ?? []
  const lineageIssues = [
    !lineage ? '未返回血缘' : '',
    lineage && sourceTables.length === 0 ? '未识别来源表' : '',
    lineage && lineage.approvalRequired !== true ? '未强制发布审批' : '',
  ].filter(Boolean)
  const warnings = [
    ...(preview?.impact?.warnings ?? []),
    ...(!preview ? ['尚未预览'] : []),
  ]

  return [
    {
      key: 'metadata',
      label: '字段与指标',
      status: metadataIssues.length ? 'warn' : 'pass',
      statusLabel: metadataIssues.length ? '需复核' : '可发布',
      detail: metadataIssues.length
        ? metadataIssues.join(' / ')
        : `${fields.length} 字段 / ${metrics.length} 指标 · ${visibleFieldCount} 可见字段 · ${dimensionConstrainedMetricCount} 指标维度约束`,
    },
    {
      key: 'parameters',
      label: '运行参数',
      status: missingDefaults.length ? 'block' : 'pass',
      statusLabel: missingDefaults.length ? '需补齐' : '可发布',
      detail: missingDefaults.length
        ? `${parameters.length} 参数 · 缺少默认值: ${missingDefaults.join(', ')}`
        : `${parameters.length} 参数 · 默认值已补齐`,
    },
    {
      key: 'sample',
      label: '样例预览',
      status: sampleBlocked ? 'block' : 'pass',
      statusLabel: sampleBlocked ? '需补齐' : '可发布',
      detail: sampleBlocked
        ? (trimValue(preview?.executionError) || '未执行样例预览')
        : `${preview?.rowCount ?? 0}/${preview?.sampleLimit ?? 0} 行 · ${(preview?.columns ?? []).length} 列`,
    },
    {
      key: 'lineage',
      label: '血缘与审批',
      status: lineageIssues.length ? 'warn' : 'pass',
      statusLabel: lineageIssues.length ? '需复核' : '可发布',
      detail: [
        sourceTables.length ? sourceTables.join(' / ') : '无来源表',
        gates.length ? `门禁 ${gates.join(' / ')}` : '无门禁',
        ...lineageIssues,
      ].join(' · '),
    },
    {
      key: 'warnings',
      label: '风险提示',
      status: warnings.length ? 'warn' : 'pass',
      statusLabel: warnings.length ? '需复核' : '可发布',
      detail: warnings.length ? warnings.join(' / ') : '暂无风险提示',
    },
  ]
}

export function buildDatasourceOnboardingCommand(
  draft: BiDatasourceOnboardingDraftInputLike,
  connectors: BiDatasourceConnectorCapabilityLike[] | null | undefined = [],
): BiDatasourceOnboardingCommandLike {
  const normalizedDraftConnector = normalizeDatasourceConnectorType(draft.connectorType)
  const availableConnectors = (connectors ?? []).filter(connector => connector.supportStatus === 'AVAILABLE')
  const selectedConnector = availableConnectors.find(connector =>
    normalizeDatasourceConnectorType(connector.connectorType) === normalizedDraftConnector,
  ) ?? availableConnectors[0]
  const connectorType = normalizedDraftConnector || normalizeDatasourceConnectorType(selectedConnector?.connectorType) || 'MYSQL'
  const sourceCategory = selectedConnector?.sourceCategory ?? ''
  const isFileConnector = connectorType === 'CSV_EXCEL' || sourceCategory === 'FILE'
  const isHttpJsonConnector = connectorType === 'API' || sourceCategory === 'HTTP' || sourceCategory === 'APP'
  const driverClassName = isFileConnector
    ? ''
    : trimValue(draft.driverClassName)
    || selectedConnector?.driverClassNames?.find(driver => !!driver?.trim())?.trim()
    || ''
  const password = trimValue(draft.password)
  const supportedModes = (selectedConnector?.supportedModes ?? [])
    .map(normalizeDatasourceConnectionMode)
    .filter((mode): mode is string => Boolean(mode))
  const requestedMode = normalizeDatasourceConnectionMode(draft.connectionMode)
  const connectionMode = requestedMode && supportedModes.includes(requestedMode)
    ? requestedMode
    : supportedModes.includes('DIRECT_QUERY')
      ? 'DIRECT_QUERY'
      : supportedModes[0] ?? 'DIRECT_QUERY'
  const command: BiDatasourceOnboardingCommandLike = {
    connectorType,
    name: trimValue(draft.name),
    url: isFileConnector ? fileDatasourceUrl(draft) : trimValue(draft.url),
    username: isFileConnector ? '' : trimValue(draft.username),
    password: isFileConnector ? '' : password,
    driverClassName,
    description: trimValue(draft.description),
    enabled: draft.enabled !== false,
    connectionMode,
  }
  if (isHttpJsonConnector) {
    command.connectorConfig = buildApiDatasourceConnectorConfig(draft)
  }
  if (isFileConnector) {
    command.connectorConfig = buildFileDatasourceConnectorConfig(draft)
  }
  return command
}

function normalizeDatasourceConnectionMode(connectionMode: string | null | undefined): string {
  return trimValue(connectionMode).toUpperCase()
}

function buildApiDatasourceConnectorConfig(draft: BiDatasourceOnboardingDraftInputLike): Record<string, unknown> {
  const bodyTemplate = trimValue(draft.apiBodyTemplate)
  const config: Record<string, unknown> = {
    requestMethod: normalizeDatasourceOption(draft.apiRequestMethod, ['GET', 'POST'], 'GET'),
    authType: normalizeDatasourceOption(draft.apiAuthType, ['NONE', 'BASIC', 'BEARER', 'API_KEY'], 'NONE'),
    headers: [apiConnectorNameValue(draft.apiHeaderName, draft.apiHeaderValue, draft.apiHeaderVariable)].filter(Boolean),
    parameters: [apiConnectorNameValue(draft.apiParameterName, draft.apiParameterValue, draft.apiParameterVariable)].filter(Boolean),
    responseRowsPath: trimValue(draft.apiResponseRowsPath) || '$',
    responseFormat: normalizeDatasourceOption(draft.apiResponseFormat, ['JSON'], 'JSON'),
  }
  if (bodyTemplate) {
    config.bodyTemplate = bodyTemplate
  }
  return config
}

function fileDatasourceUrl(draft: BiDatasourceOnboardingDraftInputLike): string {
  const url = trimValue(draft.url)
  if (url) {
    return url
  }
  const fileName = trimValue(draft.fileName)
  return fileName ? `file://${fileName}` : ''
}

function buildFileDatasourceConnectorConfig(draft: BiDatasourceOnboardingDraftInputLike): Record<string, unknown> {
  const fileName = trimValue(draft.fileName) || fileNameFromUrl(draft.url)
  const fileType = normalizeFileDatasourceType(draft.fileType, fileName)
  const sheetName = trimValue(draft.fileSheetName)
  const delimiter = trimValue(draft.fileDelimiter)
  const config: Record<string, unknown> = {
    fileName,
    fileType,
    headerRow: draft.fileHeaderRow !== false,
    encoding: trimValue(draft.fileEncoding).toUpperCase() || 'UTF-8',
  }
  if (sheetName) {
    config.sheetName = sheetName
  }
  if (delimiter) {
    config.delimiter = delimiter
  }
  return config
}

function fileNameFromUrl(url: string | null | undefined): string {
  const normalized = trimValue(url)
  if (!normalized) {
    return ''
  }
  const slashIndex = normalized.lastIndexOf('/')
  return slashIndex >= 0 ? normalized.slice(slashIndex + 1).trim() : normalized
}

function normalizeFileDatasourceType(fileType: string | null | undefined, fileName: string): string {
  const requested = trimValue(fileType)
  const dotIndex = fileName.lastIndexOf('.')
  const extension = dotIndex >= 0 ? fileName.slice(dotIndex + 1) : ''
  return normalizeDatasourceOption(requested || extension, ['CSV', 'XLS', 'XLSX'], 'CSV')
}

function apiConnectorNameValue(
  name: string | null | undefined,
  value: string | null | undefined,
  variable: boolean | null | undefined,
): { name: string, value: string, variable: boolean } | null {
  const normalizedName = trimValue(name)
  if (!normalizedName) return null
  const normalizedValue = trimValue(value)
  return {
    name: normalizedName,
    value: normalizedValue,
    variable: variable ?? /\{\{[^}]+}}/.test(normalizedValue),
  }
}

function normalizeDatasourceOption(value: string | null | undefined, allowed: string[], fallback: string): string {
  const normalized = trimValue(value).toUpperCase()
  return allowed.includes(normalized) ? normalized : fallback
}

function formatSlashList(values: string[] | null | undefined): string {
  return values && values.length > 0 ? values.join(' / ') : '-'
}

function normalizeDatasourceConnectorType(value: string | null | undefined): string {
  return trimValue(value).toUpperCase()
}

function trimValue(value: string | null | undefined): string {
  return value?.trim() ?? ''
}

function uniqueStrings(values: Array<string | null | undefined>): string[] {
  const result: string[] = []
  const seen = new Set<string>()
  for (const value of values) {
    const trimmed = trimValue(value)
    if (!trimmed || seen.has(trimmed)) continue
    seen.add(trimmed)
    result.push(trimmed)
  }
  return result
}

function datasourceTableColumnNames(table: BiDatasourceTablePreviewLike | null | undefined): string[] {
  return (table?.columns ?? [])
    .filter(column => !!column.name?.trim())
    .sort((a, b) => (a.ordinalPosition ?? 0) - (b.ordinalPosition ?? 0))
    .map(column => column.name?.trim() ?? '')
}

const SQL_PARAMETER_PATTERN = /\{\{\s*([A-Za-z0-9][A-Za-z0-9_-]{0,127})\s*\}\}/g
const SQL_PARAMETER_DATA_TYPES = new Set<BiSqlDatasetParameterDataType>([
  'STRING',
  'NUMBER',
  'DATE',
  'DATETIME',
  'BOOLEAN',
  'PERCENT',
])

function sqlTemplateParameterKeys(sqlTemplate: string | null | undefined): string[] {
  const keys: string[] = []
  const seen = new Set<string>()
  for (const match of trimValue(sqlTemplate).matchAll(SQL_PARAMETER_PATTERN)) {
    const key = normalizeSqlParameterKey(match[1])
    if (!key || seen.has(key)) continue
    seen.add(key)
    keys.push(key)
  }
  return keys
}

function normalizeSqlParameterKey(value: string | null | undefined): string {
  const key = trimValue(value)
  return /^[A-Za-z0-9][A-Za-z0-9_-]{0,127}$/.test(key) ? key : ''
}

function normalizeSqlParameterDataType(
  dataType: string | null | undefined,
  key = '',
): BiSqlDatasetParameterDataType {
  const normalized = trimValue(dataType).toUpperCase()
  if (SQL_PARAMETER_DATA_TYPES.has(normalized as BiSqlDatasetParameterDataType)) {
    return normalized as BiSqlDatasetParameterDataType
  }
  const keyHint = key.toLowerCase()
  if (keyHint.includes('datetime') || keyHint.endsWith('_at') || keyHint.includes('time')) return 'DATETIME'
  if (keyHint.includes('date') || keyHint.endsWith('_day')) return 'DATE'
  if (keyHint.includes('percent') || keyHint.includes('rate')) return 'PERCENT'
  if (keyHint.endsWith('_id') || keyHint.includes('count') || keyHint.includes('amount') || keyHint.includes('cost')) return 'NUMBER'
  return 'STRING'
}

function sqlAllowedValues(parameter: BiSqlDatasetParameterDraftLike): string[] {
  const fromText = trimValue(parameter.allowedValuesText)
    .split(/[\n,]/)
    .map(value => trimValue(value))
    .filter(Boolean)
  const fromArray = (parameter.allowedValues ?? [])
    .map(value => trimValue(value))
    .filter(Boolean)
  return uniqueStrings(fromText.length > 0 ? fromText : fromArray)
}

function isEmptySampleProfileValue(value: unknown): boolean {
  return value === null || value === undefined || value === ''
}

function uniqueSampleProfileValues(values: unknown[]): unknown[] {
  const seen = new Set<string>()
  const result: unknown[] = []
  for (const value of values) {
    const key = formatSampleProfileValue(value)
    if (!seen.has(key)) {
      seen.add(key)
      result.push(value)
    }
  }
  return result
}

function formatSampleProfileValue(value: unknown): string {
  if (value === null || value === undefined) return '-'
  if (typeof value === 'string' || typeof value === 'number' || typeof value === 'boolean') return String(value)
  return JSON.stringify(value)
}

function sqlImpactList(values: string[] | null | undefined): string {
  const normalized = uniqueStrings((values ?? []).map(value => trimValue(value)).filter(Boolean))
  return normalized.length > 0 ? normalized.join(' / ') : '-'
}

function normalizeSqlDatasetField(field: BiSqlDatasetFieldDraftLike, index: number) {
  const fieldKey = normalizeDatasetKey(field.fieldKey || field.columnExpression || field.displayName || '')
  const displayName = trimValue(field.displayName) || fieldKey
  return {
    fieldKey,
    displayName,
    columnExpression: trimValue(field.columnExpression) || fieldKey,
    role: trimValue(field.role).toUpperCase() === 'MEASURE' ? 'MEASURE' : 'DIMENSION',
    dataType: normalizeSqlParameterDataType(field.dataType, fieldKey),
    semanticType: trimValue(field.semanticType) || null,
    defaultAggregation: trimValue(field.defaultAggregation) || null,
    formatPattern: trimValue(field.formatPattern) || null,
    unit: trimValue(field.unit) || null,
    visible: field.visible !== false,
    sensitiveLevel: trimValue(field.sensitiveLevel).toUpperCase() || 'NORMAL',
    sortOrder: index,
  }
}

function normalizeSqlDatasetMetric(metric: BiSqlDatasetMetricDraftLike) {
  const metricKey = normalizeDatasetKey(metric.metricKey || metric.displayName || '')
  return {
    metricKey,
    displayName: trimValue(metric.displayName) || metricKey,
    expression: trimValue(metric.expression) || metricKey,
    aggregation: trimValue(metric.aggregation).toUpperCase() || 'SUM',
    dataType: normalizeSqlParameterDataType(metric.dataType, metricKey),
    unit: trimValue(metric.unit) || null,
    formatPattern: trimValue(metric.formatPattern) || null,
    allowedDimensions: uniqueStrings(metric.allowedDimensions ?? []).map(normalizeDatasetKey).filter(Boolean),
    owner: trimValue(metric.owner) || null,
    description: trimValue(metric.description) || null,
    status: trimValue(metric.status).toUpperCase() || 'ACTIVE',
  }
}

function normalizeDatasourceTableAlias(value: string | null | undefined): string {
  const alias = trimValue(value)
    .toLowerCase()
    .replace(/[^a-z0-9_]+/g, '_')
    .replace(/^_+/, '')
    .replace(/_+$/, '')
  if (!alias) return 'table'
  return /^[a-z_]/.test(alias) ? alias : `t_${alias}`
}

function sanitizeDatasourceGraphCoordinate(value: number | null | undefined): number | null {
  if (typeof value !== 'number' || !Number.isFinite(value)) return null
  return Math.max(0, Math.round(value))
}

const DATASOURCE_JOIN_CONDITION_OPERATORS = ['=', '<>', '>', '>=', '<', '<=']

function normalizeDatasourceJoinConditionOperator(operator: string | null | undefined): string {
  const value = trimValue(operator)
  return DATASOURCE_JOIN_CONDITION_OPERATORS.includes(value) ? value : '='
}

function datasourceJoinConditionCommand(
  leftColumn: string,
  operator: string,
  rightColumn: string,
  connector?: string,
  groupStart = false,
  groupEnd = false,
): { leftColumn: string; operator?: string; connector?: string; rightColumn: string; groupStart?: boolean; groupEnd?: boolean } {
  const command: { leftColumn: string; operator?: string; connector?: string; rightColumn: string; groupStart?: boolean; groupEnd?: boolean } = {
    leftColumn,
    rightColumn,
  }
  if (operator !== '=') command.operator = operator
  if (connector) command.connector = connector
  if (groupStart) command.groupStart = true
  if (groupEnd) command.groupEnd = true
  return command
}

function normalizeDatasourceJoinConditions(join: BiDatasourceMultiTableJoinInputLike): Array<{ leftColumn: string; operator?: string; connector?: string; rightColumn: string; groupStart?: boolean; groupEnd?: boolean }> {
  const rawConditions = (join.conditions ?? []).length > 0
    ? join.conditions ?? []
    : [{ leftColumn: join.leftColumn, operator: '=', rightColumn: join.rightColumn }]
  const seen = new Set<string>()
  const conditions: Array<{ leftColumn: string; operator?: string; connector?: string; rightColumn: string; groupStart?: boolean; groupEnd?: boolean }> = []
  rawConditions.forEach(condition => {
    const leftColumn = trimValue(condition?.leftColumn)
    const operator = normalizeDatasourceJoinConditionOperator(condition?.operator)
    const connector = normalizeDatasourceJoinConnector(condition?.connector)
    const rightColumn = trimValue(condition?.rightColumn)
    const groupStart = condition?.groupStart === true
    const groupEnd = condition?.groupEnd === true
    const key = `${leftColumn}${operator}${connector}${rightColumn}${groupStart}${groupEnd}`
    if (!leftColumn || !rightColumn || seen.has(key)) return
    seen.add(key)
    conditions.push(datasourceJoinConditionCommand(leftColumn, operator, rightColumn, connector, groupStart, groupEnd))
  })
  return conditions
}

function normalizeDatasourceJoinConnector(connector: string | null | undefined): string | undefined {
  const value = trimValue(connector).toUpperCase()
  return value === 'OR' ? value : undefined
}

function formatDatasourceConnectorCapabilities(connector: BiDatasourceConnectorCapabilityLike): string {
  const labels = [
    connector.supportsConnectionTest ? '连通性' : '',
    connector.supportsSchemaSync ? '元数据同步' : '',
    connector.supportsSqlDataset ? 'SQL 数据集' : '',
    connector.supportsTableDataset ? '表数据集' : '',
    connector.supportsCredentials ? '凭证' : '',
  ].filter(Boolean)
  return labels.length > 0 ? labels.join(' · ') : '-'
}

export function queryGovernancePolicyRows(
  policy: BiQueryGovernancePolicyLike | null | undefined,
): BiQueryGovernancePolicyRow[] {
  const datasets = policy?.datasets ?? []
  return [
    {
      label: '默认策略',
      value: `${policy?.defaultTimeoutMs ?? 0} ms · ${policy?.defaultQuotaRows ?? 0} 行`,
    },
    {
      label: '数据集策略',
      value: datasets.length > 0
        ? datasets.map(dataset => `${dataset.datasetKey} · ${dataset.timeoutMs} ms · ${dataset.quotaRows} 行`).join(' / ')
        : '-',
    },
  ]
}

export function queryCachePolicyRows(
  policy: BiQueryCachePolicyLike | null | undefined,
): BiQueryCachePolicyRow[] {
  const resources = policy?.resources ?? []
  return [
    {
      label: '默认缓存',
      value: `${policy?.defaultEnabled === false ? '关闭' : '启用'} · ${policy?.defaultCacheMode || 'CACHE'} · ${policy?.defaultTtlSeconds ?? 0} 秒`,
    },
    {
      label: '资源策略',
      value: resources.length > 0
        ? resources
          .map(resource => {
            const resourceName = `${resource.resourceType || '-'}/${resource.resourceKey || '-'}`
            const enabledLabel = resource.enabled === false ? '关闭' : '启用'
            return `${resourceName} · ${enabledLabel} · ${resource.cacheMode || 'CACHE'} · ${resource.ttlSeconds ?? 0} 秒`
          })
          .join(' / ')
        : '-',
    },
  ]
}

export function buildQueryCachePolicyCommand(
  policy: BiQueryCachePolicyLike | null | undefined,
  defaultDraft: BiQueryCachePolicyDefaultDraftLike,
  resourceDraft?: BiQueryCachePolicyResourceDraftLike | null,
): BiQueryCachePolicyCommand {
  const normalizedResources = (policy?.resources ?? [])
    .map(normalizeQueryCachePolicyResource)
    .filter((resource): resource is BiQueryCachePolicyCommand['resources'][number] => Boolean(resource))
  const targetResource = normalizeQueryCachePolicyResource(resourceDraft)
  const resources = targetResource
    ? [
        ...normalizedResources.filter(resource =>
          resource.resourceType !== targetResource.resourceType || resource.resourceKey !== targetResource.resourceKey),
        targetResource,
      ]
    : normalizedResources
  const defaultEnabled = defaultDraft.enabled !== false
  return {
    defaultEnabled,
    defaultTtlSeconds: positiveInteger(defaultDraft.ttlSeconds, policy?.defaultTtlSeconds ?? 300),
    defaultCacheMode: normalizeCacheMode(defaultDraft.cacheMode ?? (defaultEnabled ? 'CACHE' : 'DIRECT_QUERY')),
    resources,
  }
}

function normalizeQueryCachePolicyResource(
  resource: BiQueryCachePolicyResourceDraftLike | null | undefined,
): BiQueryCachePolicyCommand['resources'][number] | null {
  if (!resource) {
    return null
  }
  const resourceType = resource.resourceType?.trim().toUpperCase()
  const resourceKey = resource.resourceKey?.trim()
  if (!resourceType || !resourceKey) {
    return null
  }
  const enabled = resource.enabled !== false
  return {
    resourceType,
    resourceKey,
    enabled,
    ttlSeconds: positiveInteger(resource.ttlSeconds, 300),
    cacheMode: normalizeCacheMode(resource.cacheMode ?? (enabled ? 'CACHE' : 'DIRECT_QUERY')),
  }
}

function normalizeCacheMode(cacheMode: string | null | undefined): string {
  return cacheMode?.trim().toUpperCase() || 'CACHE'
}

function positiveInteger(value: number | null | undefined, fallback: number): number {
  if (typeof value !== 'number' || !Number.isFinite(value) || value <= 0) {
    return Math.max(1, Math.trunc(fallback))
  }
  return Math.max(1, Math.trunc(value))
}

export function queryCacheInvalidationActionRows(
  datasetKey: string | null | undefined,
): BiQueryCacheInvalidationActionRow[] {
  const actions: BiQueryCacheInvalidationActionRow[] = []
  const normalizedDatasetKey = datasetKey?.trim()
  if (normalizedDatasetKey) {
    actions.push({
      key: 'dataset',
      label: '清当前数据集',
      command: { scope: 'DATASET', datasetKey: normalizedDatasetKey },
    })
  }
  actions.push({
    key: 'all',
    label: '清全部缓存',
    command: { scope: 'ALL' },
  })
  return actions
}

export function queryCacheStatsRows(
  stats: BiQueryCacheStatsLike | null | undefined,
): BiQueryCacheStatsRow[] {
  const hitCount = stats?.hitCount ?? 0
  const missCount = stats?.missCount ?? 0
  const totalReads = hitCount + missCount
  const hitRate = totalReads === 0 ? '0.0' : ((hitCount / totalReads) * 100).toFixed(1)
  const maxEntries = stats?.maxEntries ?? 0
  const capacity = maxEntries > 0 ? `${stats?.entryCount ?? 0}/${maxEntries}` : `${stats?.entryCount ?? 0}/-`
  return [
    {
      label: '缓存 Provider',
      value: `${stats?.provider ?? '-'} · ${stats?.enabled === false ? '关闭' : '启用'}`,
    },
    {
      label: '缓存容量',
      value: `${capacity} 条 · TTL ${stats?.ttlSeconds ?? 0} 秒`,
    },
    {
      label: '命中率',
      value: `${hitRate}% · 命中 ${hitCount} / 未命中 ${missCount}`,
    },
    {
      label: '写入/驱逐',
      value: `写入 ${stats?.putCount ?? 0} · 驱逐 ${stats?.evictionCount ?? 0}`,
    },
  ]
}

export function quickEngineCapacitySummaryRows(
  summary: BiQuickEngineCapacitySummaryLike | null | undefined,
): BiQuickEngineCapacitySummaryRow[] {
  const policy = summary?.alertPolicy
  const capacityLimitRows = summary?.capacityLimitRows ?? policy?.capacityLimitRows ?? 0
  const usedRows = summary?.usedRows ?? 0
  const usagePercent = summary?.usagePercent ?? 0
  const alertLevel = summary?.alertLevel || (policy?.enabled === false ? 'DISABLED' : 'NORMAL')
  const warningThreshold = policy?.warningThresholdPercent ?? 80
  const criticalThreshold = policy?.criticalThresholdPercent ?? 95
  const channels = listLabel(policy?.notificationChannels)
  const receivers = listLabel(policy?.notificationReceivers)
  const categories = (summary?.categories ?? [])
    .map(category =>
      `${category.type || '-'} ${category.usedRows ?? 0} 行 / ${category.resourceCount ?? 0} 个资源`)
    .join('；')
  return [
    {
      label: '容量水位',
      value: `${usedRows}/${capacityLimitRows} 行 · ${usagePercent}% · ${alertLevel}`,
    },
    {
      label: '告警策略',
      value: `${policy?.enabled === false ? '关闭' : '启用'} · warning ${warningThreshold}% · critical ${criticalThreshold}%`,
    },
    {
      label: '通知策略',
      value: `${channels} · ${receivers}`,
    },
    {
      label: '容量分类',
      value: categories || '-',
    },
  ]
}

export function quickEngineConcurrencyQueueRows(
  summary: BiQuickEngineCapacitySummaryLike | null | undefined,
): BiQuickEngineConcurrencyQueueRow[] {
  const policy = summary?.tenantPoolPolicy
  const queue = summary?.concurrencyQueue
  const poolKey = poolKeyLabel(policy?.poolKey)
  const maxConcurrentQueries = policy?.maxConcurrentQueries ?? 8
  const queueLimit = policy?.queueLimit ?? 50
  const queueTimeoutSeconds = policy?.queueTimeoutSeconds ?? 120
  const poolWeight = policy?.poolWeight ?? 100
  const runningQueries = queue?.runningQueries ?? 0
  const queuedQueries = queue?.queuedQueries ?? 0
  const blockedQueries = queue?.blockedQueries ?? 0
  const successfulQueries = queue?.successfulQueries ?? 0
  const failedQueries = queue?.failedQueries ?? 0
  const state = queue?.state || 'NORMAL'
  return [
    {
      label: '租户容量池',
      value: `${poolKey} · 并发 ${maxConcurrentQueries} · 队列 ${queueLimit} · 等待 ${queueTimeoutSeconds} 秒 · 权重 ${poolWeight}`,
    },
    {
      label: '并发队列',
      value: `${runningQueries}/${maxConcurrentQueries} 并发 · ${queuedQueries}/${queueLimit} 队列 · ${state}`,
    },
    {
      label: '最近结果',
      value: `成功 ${successfulQueries} · 阻断 ${blockedQueries} · 失败 ${failedQueries}`,
    },
  ]
}

export function quickEngineCapacityDetailRows(
  summary: BiQuickEngineCapacitySummaryLike | null | undefined,
): BiQuickEngineCapacityDetailRow[] {
  return (summary?.details ?? []).map((detail, index) => {
    const resourceKey = detail.resourceKey || `resource-${index}`
    const latestRun = detail.latestRunId ? `run#${detail.latestRunId}` : 'run#-'
    const latestFinishedAt = detail.latestFinishedAt || '-'
    const latestRowCount = detail.latestRowCount ?? 0
    return {
      key: resourceKey,
      type: detail.type || '-',
      resourceKey,
      usedRows: detail.usedRows ?? 0,
      activeTables: detail.activeTables ?? 0,
      latest: `${latestRun} · ${latestFinishedAt} · ${latestRowCount} 行`,
      owner: detail.owner || '-',
    }
  })
}

export function quickEngineCapacityUserRows(
  summary: BiQuickEngineCapacitySummaryLike | null | undefined,
): BiQuickEngineCapacityUserRow[] {
  return (summary?.userRankings ?? []).map((user, index) => {
    const username = user.user || `user-${index}`
    return {
      key: username,
      user: username,
      usedRows: user.usedRows ?? 0,
      activeTables: user.activeTables ?? 0,
      resourceCount: user.resourceCount ?? 0,
    }
  })
}

export function buildQuickEngineCapacityAlertPolicyCommand(
  draft: BiQuickEngineCapacityAlertPolicyDraftLike,
): BiQuickEngineCapacityAlertPolicyCommand {
  return {
    enabled: draft.enabled !== false,
    capacityLimitRows: positiveInteger(draft.capacityLimitRows, 1_000_000),
    warningThresholdPercent: positiveInteger(draft.warningThresholdPercent, 80),
    criticalThresholdPercent: positiveInteger(draft.criticalThresholdPercent, 95),
    notificationChannels: normalizedStringList(draft.notificationChannels, true),
    notificationReceivers: normalizedStringList(draft.notificationReceivers, false),
  }
}

export function buildQuickEngineTenantPoolPolicyCommand(
  draft: BiQuickEngineTenantPoolPolicyDraftLike,
): BiQuickEngineTenantPoolPolicyCommand {
  return {
    poolKey: poolKeyLabel(draft.poolKey),
    maxConcurrentQueries: positiveInteger(draft.maxConcurrentQueries, 8),
    queueLimit: positiveInteger(draft.queueLimit, 50),
    queueTimeoutSeconds: positiveInteger(draft.queueTimeoutSeconds, 120),
    poolWeight: positiveInteger(draft.poolWeight, 100),
  }
}

export function datasetAccelerationPolicyRows(
  policy: BiDatasetAccelerationPolicyLike | null | undefined,
): BiDatasetAccelerationPolicyRow[] {
  const latestRun = policy?.recentRuns?.[0]
  const enabledLabel = policy?.enabled === false ? '关闭' : '启用'
  const mode = policy?.accelerationMode || 'DIRECT_QUERY'
  const refreshMode = policy?.refreshMode || 'MANUAL'
  const intervalMinutes = policy?.refreshIntervalMinutes ?? 0
  const ttlSeconds = policy?.ttlSeconds ?? 0
  const maxRows = policy?.maxRows ?? 0
  const cronExpression = policy?.cronExpression || '-'
  const lastStatus = policy?.lastStatus || 'IDLE'
  const lastRun = policy?.lastRunId ? `run#${policy.lastRunId}` : 'run#-'
  const lastRefreshedAt = policy?.lastRefreshedAt || '-'
  const materializedTable = policy?.materializedTable || '-'
  const latestRunValue = latestRun
    ? `${latestRun.status || 'IDLE'} · ${latestRun.rowCount ?? 0} 行 · ${latestRun.durationMs ?? 0} ms · ${latestRun.requestedBy || '-'}`
    : '-'
  return [
    {
      label: '加速模式',
      value: `${enabledLabel} · ${mode} · ${refreshMode}`,
    },
    {
      label: '刷新策略',
      value: `${intervalMinutes} 分钟 · TTL ${ttlSeconds} 秒 · 上限 ${maxRows} 行 · ${cronExpression}`,
    },
    {
      label: '最近刷新',
      value: `${lastStatus} · ${lastRun} · ${lastRefreshedAt} · ${materializedTable}`,
    },
    {
      label: '刷新记录',
      value: latestRunValue,
    },
  ]
}

export function datasetAccelerationSchedulerRows(
  result: BiDatasetAccelerationSchedulerResultLike | null | undefined,
): BiDatasetAccelerationSchedulerRow[] {
  return (result?.items ?? []).map((item, index) => {
    const datasetKey = item.datasetKey || '-'
    const runId = item.refreshRunId == null ? '-' : String(item.refreshRunId)
    const rowCount = item.rowCount == null ? '-' : String(item.rowCount)
    const durationMs = item.durationMs == null ? '-' : String(item.durationMs)
    return {
      key: `${datasetKey}-${index}`,
      datasetKey,
      status: item.status || '-',
      reason: item.reason || '-',
      run: `run#${runId} · ${rowCount} 行 · ${durationMs} ms`,
      materializedTable: item.materializedTable || '-',
      window: `${item.startedAt || '-'} -> ${item.finishedAt || '-'}`,
    }
  })
}

function listLabel(values: string[] | null | undefined): string {
  const normalized = normalizedStringList(values, false)
  return normalized.length > 0 ? normalized.join(', ') : '-'
}

function poolKeyLabel(value: string | null | undefined): string {
  return value?.trim().toUpperCase() || 'STANDARD'
}

function normalizedStringList(values: string[] | string | null | undefined, uppercase: boolean): string[] {
  const source = Array.isArray(values) ? values : String(values ?? '').split(',')
  const result = new Set<string>()
  for (const value of source) {
    const trimmed = value.trim()
    if (trimmed) {
      result.add(uppercase ? trimmed.toUpperCase() : trimmed)
    }
  }
  return [...result]
}

export function queryGovernanceAuditRows(
  entries: BiQueryGovernanceAuditEntryLike[] | null | undefined,
): BiQueryGovernanceAuditRow[] {
  return (entries ?? []).map((entry, index) => ({
    key: String(entry.id ?? `${entry.actionKey ?? 'audit'}-${entry.createdAt ?? index}`),
    actor: entry.actorId || '-',
    action: entry.actionKey || '-',
    resource: entry.resourceType || '-',
    detail: entry.detailJson || '-',
    createdAt: entry.createdAt || '-',
  }))
}

export function permissionAuditRows(
  entries: BiPermissionAuditEntryLike[] | null | undefined,
): BiPermissionAuditRow[] {
  return (entries ?? []).map((entry, index) => ({
    key: String(entry.id ?? `${entry.actionKey ?? 'permission-audit'}-${entry.createdAt ?? index}`),
    actor: entry.actorId || '-',
    action: entry.actionKey || '-',
    resource: entry.resourceType || '-',
    detail: entry.detailJson || '-',
    createdAt: entry.createdAt || '-',
  }))
}

export function queryExecutionPlanRows(
  plan: BiQueryExecutionPlanLike | null | undefined,
): BiQueryExecutionPlanRow[] {
  return [
    {
      label: '数据集',
      value: plan?.datasetKey || '-',
    },
    {
      label: 'SQL Hash',
      value: plan?.sqlHash || '-',
    },
    {
      label: '参数',
      value: `${plan?.parametersCount ?? 0} 个`,
    },
    {
      label: '执行计划',
      value: (plan?.steps ?? []).length > 0 ? (plan?.steps ?? []).join('\n') : '-',
    },
  ]
}

export function queryCancellationStatusLabel(result: BiQueryCancellationResultLike | null | undefined): string {
  const status = result?.cancelled ? '已请求取消' : '未找到运行中查询'
  return `${result?.sqlHash || '-'} · ${status} · ${result?.message || '-'}`
}

function formatQueryHistoryFilter(filter: { field: string; operator: string; value: unknown }): string {
  return `${filter.field} ${filter.operator} ${formatQueryHistoryValue(filter.value)}`
}

function formatQueryHistoryValue(value: unknown): string {
  if (Array.isArray(value)) {
    return value.map(formatQueryHistoryValue).join(',')
  }
  if (value === null || value === undefined) {
    return '-'
  }
  if (typeof value === 'object') {
    return JSON.stringify(value)
  }
  return String(value)
}

function formatExportAuditNumber(value: number): string {
  return Number.isFinite(value) ? Math.trunc(value).toLocaleString('en-US') : '0'
}

function normalizedExportStatus(value: string | null | undefined): string {
  return value?.trim().toUpperCase() ?? ''
}

function isExpiredExportAuditJob(job: BiExportAuditJobLike): boolean {
  if (normalizedExportStatus(job.status) === 'EXPIRED') return true
  const expiresAt = job.expiresAt
  if (!expiresAt) return false
  const time = Date.parse(expiresAt)
  return Number.isFinite(time) && time <= Date.now()
}

function normalizedAlertConditionValue(value: unknown): string {
  return String(value ?? '').trim().toUpperCase()
}

function isAnomalyCondition(condition: Record<string, unknown> | null | undefined): boolean {
  if (!condition) return false
  const operator = normalizedAlertConditionValue(condition.operator)
  const mode = normalizedAlertConditionValue(condition.mode || condition.type)
  const model = normalizedAlertConditionValue(condition.model)
  return operator.startsWith('ANOMALY') || mode === 'ANOMALY' || model === 'PERIOD_OVER_PERIOD'
}

function hasAlertSilenceConfig(condition: Record<string, unknown> | null | undefined): boolean {
  if (!condition) return false
  return Boolean(
    condition.silence
    || condition.mute
    || condition.quietHours
    || condition.silenceWindow
    || condition.silenceEnabled
    || condition.muteUntil
    || condition.silenceUntil,
  )
}

function formatExportAuditPartStorageKey(value: string): string {
  const segments = value.split('/').filter(Boolean)
  return segments[segments.length - 1] || value
}

function formatExportAuditTime(value?: string | null): string {
  return value ? value.replace('T', ' ').slice(0, 16) : '-'
}

export function buildResourceMoveCommand(
  resourceType: string,
  resourceKey: string,
  folderKey: string | null | undefined,
  sortOrder: number | null | undefined,
): BiResourceMoveCommandLike {
  const normalizedSortOrder = typeof sortOrder === 'number' && Number.isFinite(sortOrder)
    ? Math.max(0, Math.trunc(sortOrder))
    : 0
  return {
    resourceType: resourceType.trim().toUpperCase(),
    resourceKey: resourceKey.trim(),
    folderKey: normalizeFolderKey(folderKey),
    sortOrder: normalizedSortOrder,
  }
}

export function updatePortalNavigationConfig<T extends BiPortalResourceLike>(
  portal: T,
  patch: BiPortalNavigationConfigPatchLike,
): T {
  // theme 可能来自后端任意 JSON，先克隆并保证是可写对象。
  const theme = {
    ...((portal.theme && typeof portal.theme === 'object') ? portal.theme : {}),
  }
  if (patch.navigationLayout != null) {
    theme.navigationLayout = normalizePortalNavigationLayout(patch.navigationLayout)
  }
  if (patch.defaultMenuKey != null) {
    const defaultMenuKey = patch.defaultMenuKey.trim()
    if (defaultMenuKey) theme.defaultMenuKey = defaultMenuKey
    else delete theme.defaultMenuKey
  }
  if (patch.menuSearchEnabled != null) theme.menuSearchEnabled = patch.menuSearchEnabled === true
  if (patch.fullScreenEnabled != null) theme.fullScreenEnabled = patch.fullScreenEnabled === true
  if (patch.mobileEnabled != null) theme.mobileEnabled = patch.mobileEnabled === true
  setTrimmedPortalThemeValue(theme, 'logoUrl', patch.logoUrl)
  setTrimmedPortalThemeValue(theme, 'title', patch.title)
  setTrimmedPortalThemeValue(theme, 'subtitle', patch.subtitle)
  setTrimmedPortalThemeValue(theme, 'footerText', patch.footerText)
  setTrimmedPortalThemeValue(theme, 'alias', patch.alias)
  if (patch.breadcrumbEnabled != null) theme.breadcrumbEnabled = patch.breadcrumbEnabled === true
  if (patch.menuCacheEnabled != null) theme.menuCacheEnabled = patch.menuCacheEnabled === true
  if (patch.menuCacheTtlSeconds != null) {
    // TTL 非正数视为清空配置，让后端或运行时使用默认缓存策略。
    const ttl = Number(patch.menuCacheTtlSeconds)
    if (Number.isFinite(ttl) && ttl > 0) theme.menuCacheTtlSeconds = Math.trunc(ttl)
    else delete theme.menuCacheTtlSeconds
  }
  return { ...portal, theme }
}

/** 写入门户主题字符串配置，空字符串表示删除该配置。 */
function setTrimmedPortalThemeValue(theme: Record<string, unknown>, key: string, value: string | null | undefined) {
  if (value == null) return
  const normalized = value.trim()
  if (normalized) theme[key] = normalized
  else delete theme[key]
}

/** 移动门户菜单排序，并重新归一化 sortOrder。 */
export function movePortalMenuItem<T extends BiPortalResourceLike>(
  portal: T,
  menuKey: string | null | undefined,
  direction: 'up' | 'down',
): Omit<T, 'menus'> & { menus: BiPortalMenuResourceLike[] } {
  const requestedKey = menuKey?.trim() || ''
  const menus = [...(portal.menus ?? [])].map(menu => ({ ...menu }))
  const index = menus.findIndex(menu => String(menu.menuKey ?? '') === requestedKey)
  if (index < 0) return { ...portal, menus }
  const targetIndex = direction === 'up' ? index - 1 : index + 1
  if (targetIndex < 0 || targetIndex >= menus.length) return { ...portal, menus: normalizePortalMenuSortOrder(menus) }
  const nextMenus = [...menus]
  const current = nextMenus[index]
  nextMenus[index] = nextMenus[targetIndex]
  nextMenus[targetIndex] = current
  return { ...portal, menus: normalizePortalMenuSortOrder(nextMenus) }
}

/** 更新门户菜单标题、父级和图标配置，保持菜单层级引用有效。 */
export function updatePortalMenuConfig<T extends BiPortalResourceLike>(
  portal: T,
  menuKey: string | null | undefined,
  patch: BiPortalMenuConfigPatchLike,
): Omit<T, 'menus'> & { menus: BiPortalMenuResourceLike[] } {
  const requestedKey = menuKey?.trim() || ''
  const menus = [...(portal.menus ?? [])].map(menu => ({ ...menu }))
  // 只允许设置为现存菜单且不能指向自身，避免形成无效父级。
  const menuKeys = new Set(menus.map(menu => String(menu.menuKey ?? '')).filter(Boolean))
  const nextMenus = menus.map(menu => {
    if (String(menu.menuKey ?? '') !== requestedKey) return menu
    const nextMenu = { ...menu }
    if (patch.title != null) {
      const title = patch.title.trim()
      if (title) nextMenu.title = title
    }
    if (patch.parentMenuKey != null) {
      const parentMenuKey = patch.parentMenuKey.trim()
      if (parentMenuKey && parentMenuKey !== requestedKey && menuKeys.has(parentMenuKey)) {
        nextMenu.parentMenuKey = parentMenuKey
      } else {
        delete nextMenu.parentMenuKey
      }
    }
    if (patch.iconKey != null) {
      // 图标属于菜单扩展配置，写入 visibility 以兼容后端 JSON 结构。
      const visibility = {
        ...((nextMenu.visibility && typeof nextMenu.visibility === 'object') ? nextMenu.visibility : {}),
      }
      const iconKey = patch.iconKey.trim()
      if (iconKey) visibility.iconKey = iconKey
      else delete visibility.iconKey
      nextMenu.visibility = visibility
    }
    return nextMenu
  })
  return { ...portal, menus: nextMenus }
}

/** 按树式拖放语义重排门户菜单。before/after 会提升为同级，inside 会挂到目标菜单下。 */
export function reorderPortalMenuTree<T extends BiPortalResourceLike>(
  portal: T,
  draggedMenuKey: string | null | undefined,
  targetMenuKey: string | null | undefined,
  position: BiPortalMenuDropPosition,
): Omit<T, 'menus'> & { menus: BiPortalMenuResourceLike[] } {
  const draggedKey = draggedMenuKey?.trim() || ''
  const targetKey = targetMenuKey?.trim() || ''
  if (!draggedKey || !targetKey || draggedKey === targetKey) {
    return { ...portal, menus: [...(portal.menus ?? [])] }
  }
  const menus = [...(portal.menus ?? [])].map(menu => ({ ...menu }))
  const draggedIndex = menus.findIndex(menu => String(menu.menuKey ?? '') === draggedKey)
  const targetIndex = menus.findIndex(menu => String(menu.menuKey ?? '') === targetKey)
  if (draggedIndex < 0 || targetIndex < 0) return { ...portal, menus }

  const draggedMenu = { ...menus[draggedIndex] }
  const remainingMenus = menus.filter((_, index) => index !== draggedIndex)
  const nextTargetIndex = remainingMenus.findIndex(menu => String(menu.menuKey ?? '') === targetKey)
  if (position === 'inside') {
    draggedMenu.parentMenuKey = targetKey
    remainingMenus.splice(nextTargetIndex + 1, 0, draggedMenu)
    return { ...portal, menus: normalizePortalMenuSortOrder(remainingMenus) }
  }
  delete draggedMenu.parentMenuKey
  remainingMenus.splice(position === 'before' ? nextTargetIndex : nextTargetIndex + 1, 0, draggedMenu)
  return { ...portal, menus: normalizePortalMenuSortOrder(remainingMenus) }
}

function normalizePortalNavigationLayout(value: string): BiPortalNavigationLayout {
  const normalized = value.trim().toLowerCase()
  if (normalized === 'left' || normalized === 'dual') return normalized
  return 'top'
}

function normalizePortalMenuSortOrder<T extends BiPortalMenuResourceLike>(menus: T[]): T[] {
  return menus.map((menu, index) => ({ ...menu, sortOrder: index + 1 }))
}

export function buildResourceTransferCommand(
  resourceType: string,
  resourceKey: string,
  ownerUser: string,
): BiResourceTransferCommandLike {
  return {
    resourceType: resourceType.trim().toUpperCase(),
    resourceKey: resourceKey.trim(),
    ownerUser: ownerUser.trim(),
  }
}

export function buildResourceFavoriteCommand(
  resourceType: string,
  resourceKey: string,
  favorite: boolean,
): BiResourceFavoriteCommandLike {
  return {
    resourceType: resourceType.trim().toUpperCase(),
    resourceKey: resourceKey.trim(),
    favorite,
  }
}

export function buildResourceCommentCommand(
  resourceType: string,
  resourceKey: string,
  widgetKey: string | null | undefined,
  commentText: string,
): BiResourceCommentCommandLike {
  return {
    resourceType: resourceType.trim().toUpperCase(),
    resourceKey: resourceKey.trim(),
    widgetKey: widgetKey?.trim() || null,
    commentText: commentText.trim(),
  }
}

export function buildResourceLockCommand(
  resourceType: string,
  resourceKey: string,
  lockToken: string,
  ttlSeconds: number | null | undefined,
): BiResourceLockCommandLike {
  return {
    resourceType: resourceType.trim().toUpperCase(),
    resourceKey: resourceKey.trim(),
    lockToken: lockToken.trim(),
    ttlSeconds: typeof ttlSeconds === 'number' && Number.isFinite(ttlSeconds)
      ? Math.max(30, Math.trunc(ttlSeconds))
      : null,
  }
}

export function buildPublishApprovalRequestCommand(
  resourceType: string,
  resourceKey: string,
  reason: string | null | undefined,
): BiPublishApprovalRequestCommandLike {
  return {
    resourceType: resourceType.trim().toUpperCase(),
    resourceKey: resourceKey.trim(),
    reason: reason?.trim() || null,
  }
}

export function buildPublishApprovalReviewCommand(
  status: string,
  reviewComment: string | null | undefined,
  approvalId: number | null = null,
): BiPublishApprovalReviewCommandLike {
  return {
    approvalId,
    status: status.trim().toUpperCase(),
    reviewComment: reviewComment?.trim() || null,
  }
}

export function buildPermissionRequestCommand(
  resourceType: string,
  resourceKey: string,
  requestedAction: string,
  reason: string | null | undefined,
): BiPermissionRequestCommandLike {
  return {
    resourceType: resourceType.trim().toUpperCase(),
    resourceKey: resourceKey.trim(),
    requestedAction: requestedAction.trim().toUpperCase(),
    reason: reason?.trim() || null,
  }
}

export function buildPermissionRequestReviewCommand(
  status: string,
  reviewComment: string | null | undefined,
  requestId: number | null = null,
): BiPermissionRequestReviewCommandLike {
  return {
    requestId,
    status: status.trim().toUpperCase(),
    reviewComment: reviewComment?.trim() || null,
  }
}

export function buildExportApprovalReviewCommand(
  status: string,
  reviewComment: string | null | undefined,
): BiExportApprovalReviewCommandLike {
  const normalized = status.trim().toUpperCase()
  if (normalized !== 'APPROVED' && normalized !== 'REJECTED') {
    throw new Error(`unsupported export approval status: ${status}`)
  }
  return {
    status: normalized,
    reviewComment: reviewComment?.trim() || null,
  }
}

export function resourceLocationIndexKey(resourceType: string, resourceKey: string): string {
  return `${resourceType.trim().toUpperCase()}/${resourceKey.trim()}`
}

function uniqueWidgetKey(preset: BiDashboardPresetLike, baseKey: string): string {
  const existing = new Set(preset.widgets.map(widget => widget.widgetKey))
  if (!existing.has(baseKey)) return baseKey
  let index = 2
  while (existing.has(`${baseKey}-${index}`)) {
    index += 1
  }
  return `${baseKey}-${index}`
}

function uniqueWidgetTitle(preset: BiDashboardPresetLike, baseTitle: string): string {
  const existing = new Set(preset.widgets.map(widget => widget.title))
  if (!existing.has(baseTitle)) return baseTitle
  let index = 2
  while (existing.has(`${baseTitle} ${index}`)) {
    index += 1
  }
  return `${baseTitle} ${index}`
}

function clampInteger(value: number, min: number, max: number): number {
  return Math.max(min, Math.min(max, Math.round(value)))
}

function clampedInteger(
  value: string | number | null | undefined,
  fallback: unknown,
  min: number,
  max: number,
): number {
  const source = value ?? fallback
  const parsed = typeof source === 'number' ? source : Number(source)
  return clampInteger(Number.isFinite(parsed) ? parsed : min, min, max)
}

function normalizeResourceKey(value: string): string {
  const normalized = value
    .trim()
    .replace(/[^A-Za-z0-9_-]+/g, '-')
    .replace(/^-+/, '')
    .replace(/-+$/, '')
  return normalized || 'copy'
}

function normalizeSpreadsheetCellKey(value: string): string {
  const normalized = value.trim().toUpperCase()
  return /^[A-Z]+[1-9][0-9]*$/.test(normalized) ? normalized : 'A1'
}

function normalizeDatasetKey(value: string): string {
  return value
    .trim()
    .toLowerCase()
    .replace(/[^a-z0-9_]+/g, '_')
    .replace(/^_+/, '')
    .replace(/_+$/, '') || 'dataset'
}

function normalizeFolderKey(value: string | null | undefined): string | null {
  if (!value?.trim()) return null
  return value
    .trim()
    .toLowerCase()
    .replace(/[^a-z0-9_-]+/g, '-')
    .replace(/^-+/, '')
    .replace(/-+$/, '') || null
}

function displayRecordValue(record: Record<string, unknown> | null | undefined, key: string): string {
  const value = record?.[key]
  return value == null || value === '' ? '-' : String(value)
}

function refreshSummary(refresh: Record<string, unknown> | null | undefined): string {
  if (refresh?.enabled === false) return '关闭'
  return `启用 · ${displayRecordValue(refresh, 'intervalSeconds')} 秒`
}

function upsertResourceByKey<T extends { status?: string | null }>(
  resources: T[],
  resource: T,
  keyOf: (resource: T) => string | null | undefined,
): T[] {
  const key = keyOf(resource)?.trim()
  if (!key) return resources
  const withoutResource = resources.filter(item => keyOf(item)?.trim() !== key)
  if (resource.status?.trim().toUpperCase() === 'ARCHIVED') {
    return withoutResource
  }
  return [resource, ...withoutResource]
}

function resourceTarget(label: string, resourceType: string, resourceKey: string | null | undefined): BiResourceTargetLike {
  const key = resourceKey?.trim() ?? ''
  return {
    label,
    value: resourceType,
    resourceType,
    resourceKey: key,
    disabled: !key,
  }
}

function resourcePickerOption(
  resourceKey: string | null | undefined,
  name: string | null | undefined,
  status: string | null | undefined,
): BiResourcePickerOptionLike {
  const key = resourceKey?.trim() ?? ''
  const title = name?.trim() ?? ''
  const state = status?.trim().toUpperCase() || 'DRAFT'
  const label = title && title !== key
    ? `${title} · ${key} · ${state}`
    : `${key} · ${state}`
  return {
    label,
    value: key,
    disabled: state === 'ARCHIVED',
  }
}

function runtimeRouteMode(mode: string | null): BiRuntimeRouteLike['mode'] {
  const normalized = mode?.trim().toLowerCase()
  if (normalized === 'big-screen' || normalized === 'spreadsheet') return normalized
  return null
}

function runtimeResourceTypeForMode(mode: BiRuntimeRouteLike['mode']): string | null {
  if (mode === 'big-screen') return 'BIG_SCREEN'
  if (mode === 'spreadsheet') return 'SPREADSHEET'
  return null
}

function nullableRuntimeResourceId(value: string | null): number | null {
  if (value == null || value.trim() === '') return null
  const id = Number(value)
  return Number.isInteger(id) && id >= 0 ? id : null
}
