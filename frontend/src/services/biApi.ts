import http from './api'
import type { R } from '../types'

export interface BiQueryFilter {
  /** 过滤字段 key，必须来自数据集维度或运行态参数映射。 */
  field: string
  /** 查询过滤运算符。 */
  operator: 'EQ' | 'NEQ' | 'GT' | 'GTE' | 'LT' | 'LTE' | 'IN' | 'BETWEEN' | 'CONTAINS'
  /** 过滤值；IN/BETWEEN 可传数组，其它场景通常为标量。 */
  value: unknown
}

export interface BiQuerySort {
  /** 排序字段 key。 */
  field: string
  /** 排序方向。 */
  direction: 'ASC' | 'DESC'
}

export interface BiQueryRequest {
  /** 查询的数据集 key。 */
  datasetKey: string
  /** 来源仪表盘 key，用于审计、缓存和权限上下文。 */
  dashboardKey?: string | null
  /** 查询维度字段。 */
  dimensions: string[]
  /** 查询指标字段。 */
  metrics: string[]
  /** 查询过滤条件。 */
  filters: BiQueryFilter[]
  /** 查询排序条件。 */
  sorts: BiQuerySort[]
  /** 最大返回行数。 */
  limit: number
  /** 分页偏移量。 */
  offset?: number
  /** SQL 数据集运行时参数。 */
  sqlParameters?: Record<string, string>
}

export interface BiCompiledQuery {
  /** 后端编译后的 SQL。 */
  sql: string
  /** SQL 绑定参数。 */
  parameters: unknown[]
}

export interface BiQueryExplanation {
  /** 被解释的数据集 key。 */
  datasetKey: string
  /** SQL 指纹，用于缓存、取消和审计关联。 */
  sqlHash: string
  /** 参数数量。 */
  parametersCount: number
  /** 查询规划和治理步骤说明。 */
  steps: string[]
}

export interface BiQueryCancellationResult {
  sqlHash: string
  cancelled: boolean
  message: string
}

export interface BiQueryColumn {
  key: string
  role: string
  dataType: string
}

export interface BiQueryResult {
  /** 实际查询的数据集 key。 */
  datasetKey: string
  /** 返回列元数据。 */
  columns: BiQueryColumn[]
  /** 查询结果行。 */
  rows: Record<string, unknown>[]
  /** 返回行数。 */
  rowCount: number
  /** 查询耗时，单位毫秒。 */
  durationMs: number
  /** SQL 指纹，用于缓存命中和历史追踪。 */
  sqlHash: string
  /** 是否命中查询缓存。 */
  cached: boolean
}

export interface BiQueryHistoryItem {
  id: number
  datasetKey: string
  username: string
  rowCount: number
  durationMs: number
  status: string
  sqlHash: string
  errorMessage?: string | null
  createdAt: string
}

export interface BiQueryHistoryDetail extends BiQueryHistoryItem {
  request?: BiQueryRequest | null
}

export interface BiDatasourceHealth {
  sourceKey: string
  sourceType: string
  available: boolean
  message: string
}

export interface BiDatasourceHealthSnapshot extends BiDatasourceHealth {
  checkedAt: string
}

export interface BiDatasourceHealthSourceSlo {
  sourceKey: string
  sourceType: string
  totalChecks: number
  availableChecks: number
  unavailableChecks: number
  availabilityRate: number
  lastCheckedAt: string
  lastMessage: string
  riskLevel?: string
  recommendedAction?: string
}

export interface BiDatasourceHealthSloSummary {
  totalChecks: number
  availableChecks: number
  unavailableChecks: number
  availabilityRate: number
  sources: BiDatasourceHealthSourceSlo[]
}

export interface BiDatasourceConnectorCapability {
  /** 连接器类型编码。 */
  connectorType: string
  /** 连接器展示名称。 */
  label: string
  /** 数据源类别，例如 DATABASE、FILE、API。 */
  sourceCategory: string
  /** 支持的接入模式。 */
  supportedModes: string[]
  /** 支持状态，例如 GA、BETA、DEPRECATED。 */
  supportStatus: string
  /** 容量归类，用于 QuickEngine 容量看板聚合。 */
  capacityCategory?: string | null
  /** 容量使用说明。 */
  capacityNote?: string | null
  /** 是否支持连接测试。 */
  supportsConnectionTest: boolean
  /** 是否支持 schema 同步。 */
  supportsSchemaSync: boolean
  /** 是否支持 SQL 数据集建模。 */
  supportsSqlDataset: boolean
  /** 是否支持表数据集建模。 */
  supportsTableDataset: boolean
  /** 是否需要凭据。 */
  supportsCredentials: boolean
  /** 可选 JDBC 驱动类名。 */
  driverClassNames: string[]
  /** 连接器说明或限制。 */
  note: string
}

export interface BiDatasourceOnboardingView {
  id: number
  sourceKey: string
  name: string
  type: string
  connectorType: string
  enabled: boolean
  driverClassName?: string | null
  maskedUrl: string
  maskedUsername: string
  connectionMode: string
  schemaSyncStatus: string
  tableCount: number
  lastSyncedAt?: string | null
  supportedModes: string[]
  supportStatus: string
  capabilities: string[]
}

export interface BiDatasourceOnboardingCommand {
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

export interface BiDatasourceCredentialRotationCommand {
  password: string
}

export interface BiDatasourceCredentialRotationView {
  id: number
  sourceKey: string
  rotatedBy: string
}

export interface BiDatasourceFileUploadOptions {
  name: string
  description?: string | null
  sheetName?: string | null
  delimiter?: string | null
  headerRow?: boolean
  encoding?: string | null
}

export interface BiDatasourceFileMaterializationOptions extends BiDatasourceFileUploadOptions {
  datasetKey?: string | null
  datasetName?: string | null
  tenantColumn?: string | null
  schemaLimit?: number | null
  maxRows?: number | null
}

export interface BiDatasourceConnectionTestResult {
  id: number
  sourceKey: string
  connectorType: string
  success: boolean
  message: string
  databaseProductName?: string | null
  databaseProductVersion?: string | null
  checkedAt: string
  durationMs: number
}

export interface BiDatasourceColumnPreview {
  name: string
  typeName: string
  dataType: number
  nullable: boolean
  ordinalPosition: number
}

export interface BiDatasourceTablePreview {
  name: string
  tableType: string
  columns: BiDatasourceColumnPreview[]
}

export interface BiDatasourceSchemaPreview {
  id: number
  sourceKey: string
  name: string
  connectorType: string
  tables: BiDatasourceTablePreview[]
  checkedAt: string
}

export interface BiDatasourceApiPreviewRequest {
  variables?: Record<string, string>
  limit?: number
}

export interface BiDatasourceApiPreview {
  id: number
  sourceKey: string
  name: string
  connectorType: string
  columns: BiQueryColumn[]
  rows: Record<string, unknown>[]
  rowCount: number
  truncated: boolean
  durationMs: number
  checkedAt: string
}

export interface BiDatasourceSchemaSnapshotView {
  id?: number | null
  dataSourceConfigId: number
  sourceKey: string
  name: string
  connectorType: string
  syncStatus: string
  errorMessage?: string | null
  tableCount: number
  columnCount: number
  tables: BiDatasourceTablePreview[]
  syncedAt: string
  syncedBy?: string | null
}

export interface BiQueryGovernanceDatasetStats {
  datasetKey: string
  totalQueries: number
  slowQueries: number
  failedQueries: number
  cacheHits: number
  averageDurationMs: number
  maxDurationMs: number
  timeoutPolicyMs: number
  quotaRows: number
  slowFailures: number
  slowCacheMisses: number
  maxOverPolicyMs: number
  maxRowCount: number
}

export interface BiQueryGovernanceSlowAttribution {
  datasetKey: string
  slowQueries: number
  maxDurationMs: number
  timeoutPolicyMs: number
  maxOverPolicyMs: number
}

export interface BiQueryGovernanceSummary {
  totalQueries: number
  slowQueries: number
  failedQueries: number
  cacheHits: number
  averageDurationMs: number
  timeoutPolicyMs: number
  datasetQuotaRows: number
  datasets: BiQueryGovernanceDatasetStats[]
  slowAttributions: BiQueryGovernanceSlowAttribution[]
}

export interface BiQueryGovernancePolicyDataset {
  datasetKey: string
  timeoutMs: number
  quotaRows: number
}

export interface BiQueryGovernancePolicyView {
  defaultTimeoutMs: number
  defaultQuotaRows: number
  datasets: BiQueryGovernancePolicyDataset[]
}

export interface BiQueryGovernancePolicyCommand {
  defaultTimeoutMs: number
  defaultQuotaRows: number
  datasets: BiQueryGovernancePolicyDataset[]
}

export interface BiQueryGovernanceAuditEntry {
  id: number
  actorId: string
  actionKey: string
  resourceType: string
  detailJson: string
  createdAt: string
}

export interface BiQueryCachePolicyResource {
  resourceType: string
  resourceKey: string
  enabled: boolean
  ttlSeconds: number
  cacheMode: string
}

export interface BiQueryCachePolicyView {
  defaultEnabled: boolean
  defaultTtlSeconds: number
  defaultCacheMode: string
  resources: BiQueryCachePolicyResource[]
}

export interface BiQueryCachePolicyCommand {
  defaultEnabled: boolean
  defaultTtlSeconds: number
  defaultCacheMode: string
  resources: BiQueryCachePolicyResource[]
}

export interface BiQueryCacheInvalidationCommand {
  scope: 'SQL_HASH' | 'DATASET' | 'ALL'
  sqlHash?: string | null
  datasetKey?: string | null
}

export interface BiQueryCacheInvalidationResult {
  scope: string
  deletedEntries: number
  message: string
}

export interface BiQueryCacheStats {
  provider: string
  enabled: boolean
  entryCount: number
  maxEntries: number
  ttlSeconds: number
  hitCount: number
  missCount: number
  putCount: number
  evictionCount: number
}

export interface BiQuickEngineCapacityAlertPolicyView {
  enabled: boolean
  capacityLimitRows: number
  warningThresholdPercent: number
  criticalThresholdPercent: number
  notificationChannels: string[]
  notificationReceivers: string[]
  updatedBy: string
  updatedAt?: string | null
}

export interface BiQuickEngineCapacityAlertPolicyCommand {
  enabled: boolean
  capacityLimitRows: number
  warningThresholdPercent: number
  criticalThresholdPercent: number
  notificationChannels: string[]
  notificationReceivers: string[]
}

export interface BiQuickEngineTenantPoolPolicyView {
  poolKey: string
  maxConcurrentQueries: number
  queueLimit: number
  queueTimeoutSeconds: number
  poolWeight: number
  updatedBy: string
  updatedAt?: string | null
}

export interface BiQuickEngineTenantPoolPolicyCommand {
  poolKey: string
  maxConcurrentQueries: number
  queueLimit: number
  queueTimeoutSeconds: number
  poolWeight: number
}

export interface BiQuickEngineConcurrencyQueue {
  runningQueries: number
  queuedQueries: number
  blockedQueries: number
  successfulQueries: number
  failedQueries: number
  concurrencyUsagePercent: number
  queueUsagePercent: number
  state: string
}

export interface BiQuickEngineCapacityCategoryUsage {
  type: string
  usedRows: number
  resourceCount: number
}

export interface BiQuickEngineCapacityUsageDetail {
  type: string
  resourceKey: string
  usedRows: number
  activeTables: number
  latestRunId?: number | null
  latestFinishedAt?: string | null
  latestRowCount?: number | null
  owner: string
}

export interface BiQuickEngineCapacityUserUsage {
  user: string
  usedRows: number
  activeTables: number
  resourceCount: number
}

export interface BiQuickEngineCapacitySummary {
  tenantId: number
  capacityLimitRows: number
  usedRows: number
  usagePercent: number
  alertLevel: string
  alertEnabled: boolean
  alertPolicy: BiQuickEngineCapacityAlertPolicyView
  tenantPoolPolicy: BiQuickEngineTenantPoolPolicyView
  concurrencyQueue: BiQuickEngineConcurrencyQueue
  categories: BiQuickEngineCapacityCategoryUsage[]
  details: BiQuickEngineCapacityUsageDetail[]
  userRankings: BiQuickEngineCapacityUserUsage[]
}

export interface BiDatasetExtractRefreshRunView {
  id?: number | null
  datasetKey: string
  status: string
  rowCount?: number | null
  durationMs?: number | null
  materializedTable?: string | null
  requestedBy?: string | null
  startedAt?: string | null
  finishedAt?: string | null
  errorSummary?: string | null
}

export interface BiDatasetAccelerationPolicyView {
  datasetKey: string
  enabled: boolean
  accelerationMode: string
  refreshMode: string
  refreshIntervalMinutes: number
  ttlSeconds: number
  maxRows: number
  cronExpression?: string | null
  materializedTable?: string | null
  lastStatus?: string | null
  lastRunId?: number | null
  lastRefreshedAt?: string | null
  recentRuns: BiDatasetExtractRefreshRunView[]
}

export interface BiDatasetAccelerationPolicyCommand {
  enabled: boolean
  accelerationMode: string
  refreshMode: string
  refreshIntervalMinutes: number
  ttlSeconds: number
  maxRows: number
  cronExpression?: string | null
}

export interface BiDatasetAccelerationSchedulerResult {
  policiesChecked: number
  refreshed: number
  skipped: number
  failed: number
  items: BiDatasetAccelerationSchedulerItem[]
}

export interface BiDatasetAccelerationSchedulerItem {
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

export interface BiDatasetField {
  fieldKey: string
  role: 'DIMENSION' | 'MEASURE'
  dataType: string
}

export interface BiDatasetMetric {
  metricKey: string
  dataType: string
}

export interface BiDatasetView {
  datasetKey: string
  fields: BiDatasetField[]
  metrics: BiDatasetMetric[]
}

export interface BiDatasetFieldResource {
  /** 字段 key，作为维度/度量查询引用标识。 */
  fieldKey: string
  /** 字段展示名。 */
  displayName: string
  /** 字段来源表达式，通常是表列或 SQL 表达式。 */
  columnExpression: string
  /** 字段角色：维度或度量。 */
  role: 'DIMENSION' | 'MEASURE'
  /** 字段数据类型。 */
  dataType: string
  /** 语义类型，例如日期、地区、用户标识。 */
  semanticType?: string | null
  /** 默认聚合方式。 */
  defaultAggregation?: string | null
  /** 前端格式化模式。 */
  formatPattern?: string | null
  /** 指标单位。 */
  unit?: string | null
  /** 字段分组文件夹 key。 */
  folderKey?: string | null
  /** 是否在自助分析和图表设计中可见。 */
  visible: boolean
  /** 敏感等级，用于列权限和脱敏策略。 */
  sensitiveLevel: string
  /** 字段排序值。 */
  sortOrder: number
}

export interface BiMetricResource {
  /** 指标 key，作为图表和查询引用标识。 */
  metricKey: string
  /** 指标展示名。 */
  displayName: string
  /** 指标计算表达式。 */
  expression: string
  /** 指标聚合方式。 */
  aggregation: string
  /** 指标数据类型。 */
  dataType: string
  /** 指标单位。 */
  unit?: string | null
  /** 指标格式化模式。 */
  formatPattern?: string | null
  /** 允许与该指标组合查询的维度 key。 */
  allowedDimensions: string[]
  /** 指标负责人。 */
  owner?: string | null
  /** 指标口径说明。 */
  description?: string | null
  /** 指标状态，例如 DRAFT、PUBLISHED。 */
  status: string
}

export interface BiDatasetResource {
  /** 数据集 key。 */
  datasetKey: string
  /** 数据集名称。 */
  name: string
  /** 数据集类型，例如 TABLE、SQL、API。 */
  datasetType: string
  /** 表达式或物化表名称。 */
  tableExpression: string
  /** 租户隔离字段。 */
  tenantColumn: string
  /** 数据集模型扩展配置。 */
  model: Record<string, unknown>
  /** 数据集字段列表。 */
  fields: BiDatasetFieldResource[]
  /** 数据集指标列表。 */
  metrics: BiMetricResource[]
  /** 发布状态。 */
  status: string
  /** 数据来源说明。 */
  source: string
}

export interface BiDatasetFromDatasourceCommand {
  dataSourceConfigId: number
  tableName: string
  datasetKey: string
  name: string
  tenantColumn: string
  selectedColumns: string[]
  apiResponseVariables?: Record<string, string>
}

export interface BiDatasetFromDatasourceTableCommand {
  tableName: string
  alias: string
  selectedColumns: string[]
}

export interface BiDatasetFromDatasourceJoinCommand {
  joinType: string
  leftAlias: string
  leftColumn: string
  rightAlias: string
  rightColumn: string
  conditions?: Array<{
    leftColumn: string
    operator?: string
    connector?: string
    rightColumn: string
    groupStart?: boolean
    groupEnd?: boolean
  }>
}

export interface BiDatasetFromDatasourceMultiTableCommand {
  dataSourceConfigId: number
  datasetKey: string
  name: string
  baseTableName: string
  tenantColumn: string
  tables: BiDatasetFromDatasourceTableCommand[]
  joins: BiDatasetFromDatasourceJoinCommand[]
  graph?: {
    layoutMode: string
    nodes: Array<{
      tableName: string
      alias: string
      x: number
      y: number
    }>
  }
}

export interface BiDatasourceFileMaterializationResult {
  source: BiDatasourceOnboardingView
  schemaSnapshot: BiDatasourceSchemaSnapshotView
  dataset: BiDatasetResource
  accelerationPolicy: BiDatasetAccelerationPolicyView
  refreshRun: BiDatasetExtractRefreshRunView
}

export interface BiSqlDatasetPreviewCommand {
  resource: BiDatasetResource
  sqlParameters?: Record<string, string>
  limit?: number
  executeSample?: boolean
}

export interface BiSqlDatasetLineageView {
  dataSourceConfigId?: number | null
  sourceTables: string[]
  parameterKeys: string[]
  tenantColumn: string
  referencedFields: string[]
  referencedMetrics: string[]
  approvalRequired: boolean
}

export interface BiSqlDatasetImpactView {
  impactedAssetTypes: string[]
  governanceGates: string[]
  warnings: string[]
}

export interface BiSqlDatasetPreviewResult {
  datasetKey: string
  normalizedSqlTemplate?: string | null
  compiledSql: string
  parameterCount: number
  columns: BiQueryColumn[]
  rows: Record<string, unknown>[]
  rowCount: number
  sampleLimit: number
  sampleExecuted: boolean
  executionError?: string | null
  lineage: BiSqlDatasetLineageView
  impact: BiSqlDatasetImpactView
}

export interface BiDatasetVersionView {
  id: number
  datasetKey: string
  version: number
  status: string
  resource: BiDatasetResource
  publishedBy?: string | null
  createdAt?: string | null
}

export interface BiDashboardWidget {
  widgetKey: string
  title: string
  chartType: string
  dimensions: string[]
  metrics: string[]
  gridX: number
  gridY: number
  gridW: number
  gridH: number
  stylePreset: string
}

export interface BiDashboardFilter {
  filterKey: string
  fieldKey: string
  label: string
  controlType: string
  required: boolean
  defaultValue?: string | null
  targetWidgetKeys?: string[] | null
  cascade?: BiDashboardFilterCascade | null
  optionDatasetKey?: string | null
  optionFieldKey?: string | null
  hidden?: boolean | null
}

export interface BiDashboardFilterCascade {
  parentFilterKeys: string[]
  parentFieldMapping?: Record<string, string> | null
  mode?: 'SAME_SOURCE' | 'MAPPED' | null
}

export interface BiDashboardInteraction {
  interactionKey: string
  sourceWidgetKey: string
  targetWidgetKey?: string | null
  interactionType: string
  fieldKey: string
  target?: string | null
}

export interface BiDashboardGlobalParameter {
  parameterKey: string
  fieldKey?: string | null
  filterKey?: string | null
  aliases?: string[] | null
  defaultValue?: string | null
  locked?: boolean | null
}

export interface BiDashboardPreset {
  /** 仪表盘 key。 */
  dashboardKey: string
  /** 仪表盘标题。 */
  title: string
  /** 仪表盘描述。 */
  description: string
  /** 默认数据集 key。 */
  datasetKey: string
  /** 组件配置。 */
  widgets: BiDashboardWidget[]
  /** 查询控件配置。 */
  filters: BiDashboardFilter[]
  /** 全局参数配置。 */
  globalParameters?: BiDashboardGlobalParameter[] | null
  /** 交互规则配置。 */
  interactions: BiDashboardInteraction[]
  /** 可订阅渠道。 */
  subscriptionChannels: string[]
  /** 可嵌入作用域。 */
  embedScopes: string[]
}

export interface BiDashboardResource {
  /** 仪表盘预设快照。 */
  preset: BiDashboardPreset
  /** 发布状态。 */
  status: string
  /** 当前版本号。 */
  version: number
  /** 来源说明，例如 SYSTEM、USER_IMPORT。 */
  source: string
}

export interface BiDashboardRuntimeStateView {
  dashboardKey: string
  username: string
  parameters: Record<string, unknown>
  updatedAt?: string | null
}

export interface BiDashboardRuntimeStateCommand {
  parameters: Record<string, unknown>
}

export interface BiDashboardCloneCommand {
  dashboardKey: string
  title: string
  description?: string | null
}

export interface BiDashboardExportPackage {
  resourceType: string
  schemaVersion: number
  sourceDashboardKey: string
  sourceVersion: number
  preset: BiDashboardPreset
  exportedBy?: string | null
  exportedAt?: string | null
}

export interface BiDashboardImportCommand {
  packagePayload: BiDashboardExportPackage
  dashboardKey?: string | null
  title?: string | null
  overwrite?: boolean | null
}

export interface BiDashboardVersionView {
  id: number
  dashboardKey: string
  version: number
  status: string
  preset: BiDashboardPreset
  publishedBy?: string | null
  createdAt?: string | null
}

export interface BiChartResource {
  /** 图表 key。 */
  chartKey: string
  /** 图表名称。 */
  name: string
  /** 图表类型。 */
  chartType: string
  /** 绑定数据集 key。 */
  datasetKey: string
  /** 图表查询定义。 */
  query: BiQueryRequest
  /** 图表样式配置。 */
  style: Record<string, unknown>
  /** 图表交互配置。 */
  interaction: Record<string, unknown>
  /** 图表发布状态。 */
  status: string
  /** 图表来源说明。 */
  source: string
}

export interface BiChartVersionView {
  id: number
  chartKey: string
  version: number
  status: string
  resource: BiChartResource
  publishedBy?: string | null
  createdAt?: string | null
}

export interface BiChartDashboardReference {
  /** 引用该图表的仪表盘 key。 */
  dashboardKey: string
  /** 仪表盘标题。 */
  title: string
  /** 引用该图表的组件 key。 */
  widgetKey: string
  /** 组件标题。 */
  widgetTitle: string
  /** 仪表盘状态。 */
  status: string
}

export interface BiChartPortalReference {
  /** 引用该图表的门户 key。 */
  portalKey: string
  /** 门户名称。 */
  name: string
  /** 引用该图表的菜单 key。 */
  menuKey: string
  /** 菜单标题。 */
  menuTitle: string
  /** 门户状态。 */
  status: string
}

export interface BiChartSubscriptionReference {
  /** 引用该图表的订阅 key。 */
  subscriptionKey: string
  /** 订阅名称。 */
  name: string
  /** 订阅是否启用。 */
  enabled?: boolean | null
}

export interface BiChartReferenceImpact {
  /** 被分析的图表 key。 */
  chartKey: string
  /** 被分析的图表名称。 */
  chartName: string
  /** 图表绑定的数据集 key。 */
  datasetKey: string
  /** 受影响的仪表盘引用。 */
  dashboards: BiChartDashboardReference[]
  /** 受影响的门户引用。 */
  portals: BiChartPortalReference[]
  /** 受影响的订阅引用。 */
  subscriptions: BiChartSubscriptionReference[]
}

export interface BiPortalMenuResource {
  /** 门户菜单 key。 */
  menuKey: string
  /** 父菜单 key，空值表示一级菜单。 */
  parentMenuKey?: string | null
  /** 菜单标题。 */
  title: string
  /** 菜单指向的资源类型，例如 DASHBOARD、CHART、EXTERNAL。 */
  resourceType: string
  /** 菜单指向的资源 key。 */
  resourceKey?: string | null
  /** 菜单指向的资源 ID。 */
  resourceId?: number | null
  /** 外部链接地址。 */
  externalUrl?: string | null
  /** 菜单可见性和扩展配置。 */
  visibility: Record<string, unknown>
  /** 菜单排序值。 */
  sortOrder: number
}

export interface BiPortalResource {
  /** 门户 key。 */
  portalKey: string
  /** 门户名称。 */
  name: string
  /** 门户主题、导航和品牌化配置。 */
  theme: Record<string, unknown>
  /** 门户菜单树。 */
  menus: BiPortalMenuResource[]
  /** 门户发布状态。 */
  status: string
  /** 门户来源说明。 */
  source: string
}

export interface BiPortalVersionView {
  id: number
  portalKey: string
  version: number
  status: string
  resource: BiPortalResource
  publishedBy?: string | null
  createdAt?: string | null
}

export interface BiBigScreenResource {
  id?: number | null
  screenKey: string
  name: string
  description?: string | null
  size: Record<string, unknown>
  background: Record<string, unknown>
  layout: Record<string, unknown>[]
  refresh: Record<string, unknown>
  mobileLayout: Record<string, unknown>
  status: string
  version: number
  source: string
}

export interface BiBigScreenVersionView {
  id: number
  screenKey: string
  version: number
  status: string
  resource: BiBigScreenResource
  publishedBy?: string | null
  createdAt?: string | null
}

export interface BiSpreadsheetResource {
  id?: number | null
  spreadsheetKey: string
  name: string
  description?: string | null
  sheets: Record<string, unknown>[]
  dataBinding: Record<string, unknown>
  style: Record<string, unknown>
  status: string
  version: number
  source: string
}

export interface BiSpreadsheetVersionView {
  id: number
  spreadsheetKey: string
  version: number
  status: string
  resource: BiSpreadsheetResource
  publishedBy?: string | null
  createdAt?: string | null
}

export interface BiEmbedTicketRequest {
  /** 嵌入资源类型。 */
  resourceType: string
  /** 嵌入资源 key。 */
  resourceKey: string
  /** 嵌入作用域，例如 INTERNAL_CANVAS、EXTERNAL_TICKET。 */
  scope: string
  /** ticket 内置过滤条件。 */
  filters: Record<string, string>
  /** ticket 内置运行态参数。 */
  parameters?: Record<string, string>
  /** ticket 有效期，单位秒。 */
  ttlSeconds: number
  /** 允许加载嵌入页的域名白名单。 */
  allowedDomains?: string[]
  /** ticket 最大访问次数。 */
  maxAccessCount?: number
  /** ticket 每分钟访问限制。 */
  rateLimitPerMinute?: number
}

export interface BiEmbedTicket {
  /** 一次性或短期有效的嵌入 ticket。 */
  ticket: string
  /** ticket 过期时间。 */
  expiresAt: string
  /** 可直接打开的嵌入 URL。 */
  embedUrl: string
}

export interface BiEmbedTicketPayload {
  /** ticket 所属租户。 */
  tenantId: number
  /** ticket 签发用户。 */
  username: string
  /** ticket 授权资源类型。 */
  resourceType: string
  /** ticket 授权资源 key。 */
  resourceKey: string
  /** ticket 授权作用域。 */
  scope: string
  /** ticket 固化过滤条件。 */
  filters: Record<string, string>
  /** ticket 固化运行态参数。 */
  parameters: Record<string, string>
  /** 域名白名单。 */
  allowedDomains: string[]
  /** 最大访问次数。 */
  maxAccessCount?: number | null
  /** 每分钟访问限制。 */
  rateLimitPerMinute?: number | null
  /** 防重放随机串。 */
  nonce: string
  /** 签发时间。 */
  issuedAt: string
  /** 过期时间。 */
  expiresAt: string
}

export interface BiEmbedQueryRequest {
  ticket: string
  resourceType: string
  resourceKey: string
  widgetKey?: string | null
  query: BiQueryRequest
}

export interface BiEmbedDashboardResourceRequest {
  ticket: string
  resourceType: string
  resourceKey: string
}

export interface BiEmbedTokenCleanupResult {
  checked: number
  revoked: number
  failed: number
}

export interface BiResourcePermissionCommand {
  /** 授权资源类型。 */
  resourceType: string
  /** 授权资源 key。 */
  resourceKey?: string | null
  /** 授权资源 ID，兼容仅有 ID 的资源。 */
  resourceId?: number | null
  /** 主体类型，例如 USER、ROLE、GROUP、ALL。 */
  subjectType: string
  /** 主体 ID。 */
  subjectId: string
  /** 授权动作，例如 USE、EDIT、EXPORT。 */
  actionKey: string
  /** 授权效果：ALLOW 或 DENY。 */
  effect: string
}

export interface BiResourcePermissionView extends BiResourcePermissionCommand {
  id: number
  tenantId: number
  workspaceId: number
  resourceKey?: string | null
  /** 资源 ID，部分 key 型资源可能为空。 */
  resourceId?: number | null
  createdBy?: string | null
  createdAt?: string | null
}

export interface BiResourceMoveCommand {
  resourceType: string
  resourceKey: string
  folderKey?: string | null
  sortOrder?: number | null
}

export interface BiResourceLocationView {
  id: number
  tenantId: number
  workspaceId: number
  resourceType: string
  resourceKey: string
  folderKey?: string | null
  sortOrder: number
  movedBy?: string | null
  movedAt?: string | null
}

export interface BiResourceTransferCommand {
  resourceType: string
  resourceKey: string
  ownerUser: string
}

export interface BiResourceOwnershipView {
  id: number
  tenantId: number
  workspaceId: number
  resourceType: string
  resourceKey: string
  ownerUser: string
  transferredBy?: string | null
  transferredAt?: string | null
}

export interface BiResourceFavoriteCommand {
  resourceType: string
  resourceKey: string
  favorite: boolean
}

export interface BiResourceFavoriteView {
  id?: number | null
  tenantId: number
  workspaceId: number
  resourceType: string
  resourceKey: string
  username: string
  favorite: boolean
  createdAt?: string | null
}

export interface BiResourceCommentCommand {
  resourceType: string
  resourceKey: string
  widgetKey?: string | null
  commentText: string
}

export interface BiResourceCommentView {
  id?: number | null
  tenantId: number
  workspaceId: number
  resourceType: string
  resourceKey: string
  widgetKey?: string | null
  commentText: string
  createdBy?: string | null
  createdAt?: string | null
  deletedAt?: string | null
}

export interface BiResourceLockCommand {
  resourceType: string
  resourceKey: string
  lockToken: string
  ttlSeconds?: number | null
}

export interface BiResourceLockView {
  id?: number | null
  tenantId: number
  workspaceId: number
  resourceType: string
  resourceKey: string
  lockToken?: string | null
  lockedBy?: string | null
  lockedAt?: string | null
  expiresAt?: string | null
  locked: boolean
}

export interface BiPublishApprovalRequestCommand {
  resourceType: string
  resourceKey: string
  reason?: string | null
}

export interface BiPublishApprovalReviewCommand {
  approvalId?: number | null
  status: string
  reviewComment?: string | null
}

export interface BiPublishApprovalView {
  id?: number | null
  tenantId: number
  workspaceId: number
  resourceType: string
  resourceKey: string
  status: string
  reason?: string | null
  requestedBy?: string | null
  requestedAt?: string | null
  reviewedBy?: string | null
  reviewedAt?: string | null
  reviewComment?: string | null
}

export interface BiPermissionRequestCommand {
  resourceType: string
  resourceKey: string
  requestedAction: string
  reason?: string | null
}

export interface BiPermissionRequestReviewCommand {
  requestId?: number | null
  status: string
  reviewComment?: string | null
}

export interface BiPermissionRequestView {
  id?: number | null
  tenantId: number
  workspaceId: number
  resourceType: string
  resourceKey: string
  requestedAction: string
  requestedBy?: string | null
  requestedAt?: string | null
  reason?: string | null
  status: string
  reviewedBy?: string | null
  reviewedAt?: string | null
  reviewComment?: string | null
  grantedPermissionId?: number | null
}

export interface BiRowPermissionCommand {
  /** 数据集 key。 */
  datasetKey: string
  /** 行权限规则 key。 */
  ruleKey: string
  /** 主体类型。 */
  subjectType: string
  /** 主体 ID。 */
  subjectId: string
  /** 查询过滤条件形式的行级约束。 */
  filters: BiQueryFilter[]
  /** 原始过滤 JSON，便于后端保存复杂表达式。 */
  filter: Record<string, unknown>
  /** 是否启用该规则。 */
  enabled: boolean
}

export interface BiRowPermissionView {
  id: number
  tenantId: number
  datasetKey: string
  /** 数据集 ID，部分测试或轻量接口只返回 datasetKey。 */
  datasetId?: number | null
  ruleKey: string
  subjectType: string
  subjectId: string
  filterJson: string
  enabled: boolean
  createdAt?: string | null
}

export interface BiColumnPermissionCommand {
  /** 数据集 key。 */
  datasetKey: string
  /** 字段 key。 */
  fieldKey: string
  /** 主体类型。 */
  subjectType: string
  /** 主体 ID。 */
  subjectId: string
  /** 字段策略：ALLOW、DENY 或 MASK。 */
  policy: string
  /** 脱敏配置。 */
  mask: Record<string, unknown>
  /** 是否启用该规则。 */
  enabled: boolean
}

export interface BiColumnPermissionView {
  id: number
  tenantId: number
  datasetKey: string
  /** 数据集 ID，部分测试或轻量接口只返回 datasetKey。 */
  datasetId?: number | null
  fieldKey: string
  subjectType: string
  subjectId: string
  policy: string
  maskJson?: string | null
  /** 是否启用该列权限规则，旧响应可能缺省。 */
  enabled?: boolean | null
  createdAt?: string | null
}

export interface BiPermissionAuditEntry {
  id: number
  actorId: string
  actionKey: string
  resourceType: string
  detailJson: string
  createdAt: string
}

export interface BiSelfServicePreviewRequest {
  query: BiQueryRequest
  previewLimit: number
}

export interface BiExportJobCommand {
  /** 导出资源类型。 */
  resourceType: string
  /** 导出资源 key。 */
  resourceKey?: string | null
  /** 导出资源 ID。 */
  resourceId?: number | null
  /** 导出格式，例如 CSV、XLSX、PDF。 */
  exportFormat: string
  /** 导出使用的查询请求。 */
  query: BiQueryRequest
  /** 导出行数上限。 */
  rowLimit: number
  /** 是否需要审批。 */
  approvalRequired?: boolean | null
  /** 是否包含敏感数据。 */
  sensitive?: boolean | null
  /** 导出审批理由。 */
  approvalReason?: string | null
}

export interface BiExportJobView {
  id: number
  tenantId: number
  workspaceId: number
  resourceType: string
  resourceKey?: string | null
  resourceId?: number | null
  exportFormat: string
  rowLimit: number
  status: string
  progressPercent?: number | null
  fileUrl?: string | null
  storageProvider?: string | null
  storageKey?: string | null
  retentionDays?: number | null
  expiresAt?: string | null
  downloadCount?: number | null
  lastDownloadedAt?: string | null
  approvalStatus?: string | null
  approvalReason?: string | null
  requestedBy?: string | null
  requestedAt?: string | null
  reviewedBy?: string | null
  reviewedAt?: string | null
  reviewComment?: string | null
  errorMessage?: string | null
  retryCount?: number | null
  maxRetryCount?: number | null
  nextRetryAt?: string | null
  lastRetryAt?: string | null
  retryExhaustedAt?: string | null
  createdBy?: string | null
  createdAt?: string | null
  updatedAt?: string | null
}

export interface BiExportJobDetailView {
  job: BiExportJobView
  request: BiExportJobCommand
  partition?: {
    storageLayout?: string | null
    requestedRows?: number | null
    generatedRows?: number | null
    partCount?: number | null
    partSize?: number | null
    partStorageKeys?: string[] | null
  } | null
}

export interface BiExportRetryResult {
  checked: number
  retried: number
  completed: number
  failed: number
  jobs: BiExportJobView[]
}

export interface BiExportApprovalReviewCommand {
  status: 'APPROVED' | 'REJECTED'
  reviewComment?: string | null
}

export interface BiSubscriptionCommand {
  /** 订阅 key。 */
  subscriptionKey: string
  /** 订阅名称。 */
  name: string
  /** 订阅资源类型。 */
  resourceType: string
  /** 订阅资源 key。 */
  resourceKey?: string | null
  /** 订阅资源 ID。 */
  resourceId?: number | null
  /** 调度配置，例如 cron、时区、频率。 */
  schedule: Record<string, unknown>
  /** 接收人配置。 */
  receivers: Record<string, unknown>
  /** 投递渠道和附件配置。 */
  delivery: Record<string, unknown>
  /** 是否启用订阅。 */
  enabled: boolean
}

export interface BiSubscriptionView extends BiSubscriptionCommand {
  id: number
  tenantId: number
  workspaceId: number
  resourceKey?: string | null
  resourceId: number
  createdBy?: string | null
  createdAt?: string | null
  updatedAt?: string | null
}

export interface BiAlertRuleCommand {
  alertKey: string
  name: string
  datasetKey: string
  metricKey: string
  condition: Record<string, unknown>
  receivers: Record<string, unknown>
  enabled: boolean
}

export interface BiAlertRuleView extends BiAlertRuleCommand {
  id: number
  tenantId: number
  workspaceId: number
  datasetId: number
  createdBy?: string | null
  createdAt?: string | null
  updatedAt?: string | null
}

export interface BiDeliveryLogView {
  id: number
  tenantId: number
  workspaceId: number
  jobType: string
  jobId: number
  jobKey: string
  resourceType?: string | null
  resourceId?: number | null
  channel: string
  receiver: Record<string, unknown>
  payload: Record<string, unknown>
  metricValue?: number | string | null
  status: string
  message?: string | null
  errorMessage?: string | null
  retryCount?: number | null
  maxRetryCount?: number | null
  nextRetryAt?: string | null
  lastRetryAt?: string | null
  retryExhaustedAt?: string | null
  triggeredBy?: string | null
  createdAt?: string | null
  updatedAt?: string | null
}

export interface BiDeliveryAttachmentView {
  id: number
  tenantId: number
  workspaceId: number
  jobType: string
  jobId: number
  jobKey: string
  deliveryLogId?: number | null
  resourceType?: string | null
  resourceId?: number | null
  attachmentKey: string
  attachmentType: string
  fileName: string
  contentType: string
  fileUrl?: string | null
  storageProvider?: string | null
  storageKey?: string | null
  sizeBytes?: number | null
  retentionDays?: number | null
  expiresAt?: string | null
  downloadCount?: number | null
  lastDownloadedAt?: string | null
  status: string
  errorMessage?: string | null
  createdBy?: string | null
  createdAt?: string | null
  updatedAt?: string | null
}

export interface BiDeliveryRunResult {
  jobType: string
  jobId: number
  jobKey: string
  status: string
  logs: BiDeliveryLogView[]
}

export interface BiDeliverySchedulerResult {
  subscriptionsChecked: number
  subscriptionsTriggered: number
  alertsChecked: number
  alertsTriggered: number
  skipped: number
  failed: number
}

export interface BiDeliveryRetryResult {
  checked: number
  retried: number
  delivered: number
  pending: number
  failed: number
  logs: BiDeliveryLogView[]
}

export interface BiDeliveryAuditSummary {
  total: number
  delivered: number
  triggered: number
  skipped: number
  pending: number
  failed: number
  retryable: number
  retryExhausted: number
  logs: BiDeliveryLogView[]
}

export interface BiDeliveryAttachmentCleanupResult {
  checked: number
  expired: number
  filesDeleted: number
  failed: number
}

export interface BiExportCleanupResult {
  checked: number
  expired: number
  filesDeleted: number
  failed: number
}

/** 将权限、导入导出和上传类接口的可选参数组装为查询串。 */
function permissionQuery(params: Record<string, string | number | null | undefined>) {
  const search = new URLSearchParams()
  // 空值不进入 URL，避免后端把空字符串误判为显式过滤条件。
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== '') search.set(key, String(value))
  })
  const value = search.toString()
  return value ? `?${value}` : ''
}

/** 草稿保存时附加锁 token，用于后端做 BI 资源编辑并发控制。 */
function draftLockConfig(lockToken?: string | null) {
  return lockToken ? { headers: { 'X-BI-LOCK-TOKEN': lockToken } } : undefined
}

/** BI 模块 API 封装，保持前端页面只依赖业务方法而不散落后端路径。 */
export const biApi = {
  listDatasets: () =>
    http.get<R<BiDatasetView[]>, R<BiDatasetView[]>>('/canvas/bi/datasets'),
  getDataset: (datasetKey: string) =>
    http.get<R<BiDatasetView>, R<BiDatasetView>>(`/canvas/bi/datasets/${datasetKey}`),
  listDatasetResources: () =>
    http.get<R<BiDatasetResource[]>, R<BiDatasetResource[]>>('/canvas/bi/datasets/resources'),
  getDatasetResource: (datasetKey: string) =>
    http.get<R<BiDatasetResource>, R<BiDatasetResource>>(`/canvas/bi/datasets/resources/${datasetKey}`),
  saveDatasetDraft: (datasetKey: string, resource: BiDatasetResource, lockToken?: string | null) =>
    http.post<R<BiDatasetResource>, R<BiDatasetResource>>(
      `/canvas/bi/datasets/resources/${datasetKey}/draft`,
      resource,
      draftLockConfig(lockToken),
    ),
  publishDataset: (datasetKey: string) =>
    http.post<R<BiDatasetResource>, R<BiDatasetResource>>(`/canvas/bi/datasets/resources/${datasetKey}/publish`, {}),
  archiveDataset: (datasetKey: string) =>
    http.delete<R<BiDatasetResource>, R<BiDatasetResource>>(`/canvas/bi/datasets/resources/${datasetKey}`),
  createDatasetFromDatasourceSchema: (command: BiDatasetFromDatasourceCommand) =>
    http.post<R<BiDatasetResource>, R<BiDatasetResource>>('/canvas/bi/datasets/resources/from-datasource-schema', command),
  createMultiTableDatasetFromDatasourceSchema: (command: BiDatasetFromDatasourceMultiTableCommand) =>
    http.post<R<BiDatasetResource>, R<BiDatasetResource>>('/canvas/bi/datasets/resources/from-datasource-schema/multi-table', command),
  previewSqlDataset: (command: BiSqlDatasetPreviewCommand) =>
    http.post<R<BiSqlDatasetPreviewResult>, R<BiSqlDatasetPreviewResult>>('/canvas/bi/datasets/resources/sql-preview', command),
  listDashboardPresets: () =>
    http.get<R<BiDashboardPreset[]>, R<BiDashboardPreset[]>>('/canvas/bi/dashboards/presets'),
  getDashboardPreset: (dashboardKey: string) =>
    http.get<R<BiDashboardPreset>, R<BiDashboardPreset>>(`/canvas/bi/dashboards/presets/${dashboardKey}`),
  listDashboardResources: () =>
    http.get<R<BiDashboardResource[]>, R<BiDashboardResource[]>>('/canvas/bi/dashboards/resources'),
  getDashboardResource: (dashboardKey: string) =>
    http.get<R<BiDashboardResource>, R<BiDashboardResource>>(`/canvas/bi/dashboards/resources/${dashboardKey}`),
  saveDashboardDraft: (dashboardKey: string, preset: BiDashboardPreset, lockToken?: string | null) =>
    http.post<R<BiDashboardResource>, R<BiDashboardResource>>(
      `/canvas/bi/dashboards/resources/${dashboardKey}/draft`,
      preset,
      draftLockConfig(lockToken),
    ),
  publishDashboard: (dashboardKey: string) =>
    http.post<R<BiDashboardResource>, R<BiDashboardResource>>(`/canvas/bi/dashboards/resources/${dashboardKey}/publish`, {}),
  cloneDashboard: (dashboardKey: string, command: BiDashboardCloneCommand) =>
    http.post<R<BiDashboardResource>, R<BiDashboardResource>>(`/canvas/bi/dashboards/resources/${dashboardKey}/clone`, command),
  exportDashboard: (dashboardKey: string) =>
    http.get<R<BiDashboardExportPackage>, R<BiDashboardExportPackage>>(`/canvas/bi/dashboards/resources/${dashboardKey}/export`),
  exportDashboardFile: (dashboardKey: string) =>
    http.get<Blob, Blob>(`/canvas/bi/dashboards/resources/${dashboardKey}/export-file`, { responseType: 'blob' }),
  importDashboard: (command: BiDashboardImportCommand) =>
    http.post<R<BiDashboardResource>, R<BiDashboardResource>>('/canvas/bi/dashboards/resources/import', command),
  importDashboardFile: (file: File, dashboardKey: string, title?: string | null, overwrite = false) => {
    const form = new FormData()
    form.append('file', file)
    return http.post<R<BiDashboardResource>, R<BiDashboardResource>>(
      `/canvas/bi/dashboards/resources/import-file${permissionQuery({ dashboardKey, title, overwrite: overwrite ? 'true' : 'false' })}`,
      form,
    )
  },
  archiveDashboard: (dashboardKey: string) =>
    http.delete<R<BiDashboardResource>, R<BiDashboardResource>>(`/canvas/bi/dashboards/resources/${dashboardKey}`),
  listDashboardVersions: (dashboardKey: string, limit = 20) =>
    http.get<R<BiDashboardVersionView[]>, R<BiDashboardVersionView[]>>(`/canvas/bi/dashboards/resources/${dashboardKey}/versions?limit=${limit}`),
  restoreDashboardVersion: (dashboardKey: string, version: number, lockToken?: string | null) =>
    http.post<R<BiDashboardResource>, R<BiDashboardResource>>(
      `/canvas/bi/dashboards/resources/${dashboardKey}/versions/${version}/restore`,
      {},
      draftLockConfig(lockToken),
    ),
  getDashboardRuntimeState: (dashboardKey: string) =>
    http.get<R<BiDashboardRuntimeStateView>, R<BiDashboardRuntimeStateView>>(`/canvas/bi/dashboards/resources/${dashboardKey}/runtime-state`),
  saveDashboardRuntimeState: (dashboardKey: string, command: BiDashboardRuntimeStateCommand) =>
    http.post<R<BiDashboardRuntimeStateView>, R<BiDashboardRuntimeStateView>>(`/canvas/bi/dashboards/resources/${dashboardKey}/runtime-state`, command),
  listChartResources: () =>
    http.get<R<BiChartResource[]>, R<BiChartResource[]>>('/canvas/bi/charts/resources'),
  getChartResource: (chartKey: string) =>
    http.get<R<BiChartResource>, R<BiChartResource>>(`/canvas/bi/charts/resources/${chartKey}`),
  getChartReferenceImpact: (chartKey: string) =>
    http.get<R<BiChartReferenceImpact>, R<BiChartReferenceImpact>>(`/canvas/bi/charts/resources/${chartKey}/impact`),
  saveChartDraft: (chartKey: string, resource: BiChartResource, lockToken?: string | null) =>
    http.post<R<BiChartResource>, R<BiChartResource>>(
      `/canvas/bi/charts/resources/${chartKey}/draft`,
      resource,
      draftLockConfig(lockToken),
    ),
  publishChart: (chartKey: string) =>
    http.post<R<BiChartResource>, R<BiChartResource>>(`/canvas/bi/charts/resources/${chartKey}/publish`, {}),
  archiveChart: (chartKey: string) =>
    http.delete<R<BiChartResource>, R<BiChartResource>>(`/canvas/bi/charts/resources/${chartKey}`),
  listChartVersions: (chartKey: string, limit = 20) =>
    http.get<R<BiChartVersionView[]>, R<BiChartVersionView[]>>(`/canvas/bi/charts/resources/${chartKey}/versions?limit=${limit}`),
  restoreChartVersion: (chartKey: string, version: number, lockToken?: string | null) =>
    http.post<R<BiChartResource>, R<BiChartResource>>(
      `/canvas/bi/charts/resources/${chartKey}/versions/${version}/restore`,
      {},
      draftLockConfig(lockToken),
    ),
  listDatasetVersions: (datasetKey: string, limit = 20) =>
    http.get<R<BiDatasetVersionView[]>, R<BiDatasetVersionView[]>>(`/canvas/bi/datasets/resources/${datasetKey}/versions?limit=${limit}`),
  restoreDatasetVersion: (datasetKey: string, version: number, lockToken?: string | null) =>
    http.post<R<BiDatasetResource>, R<BiDatasetResource>>(
      `/canvas/bi/datasets/resources/${datasetKey}/versions/${version}/restore`,
      {},
      draftLockConfig(lockToken),
    ),
  listPortalResources: () =>
    http.get<R<BiPortalResource[]>, R<BiPortalResource[]>>('/canvas/bi/portals/resources'),
  getPortalResource: (portalKey: string) =>
    http.get<R<BiPortalResource>, R<BiPortalResource>>(`/canvas/bi/portals/resources/${portalKey}`),
  listPortalRuntime: () =>
    http.get<R<BiPortalResource[]>, R<BiPortalResource[]>>('/canvas/bi/portals/runtime'),
  getPortalRuntime: (portalKey: string) =>
    http.get<R<BiPortalResource>, R<BiPortalResource>>(`/canvas/bi/portals/runtime/${portalKey}`),
  savePortalDraft: (portalKey: string, resource: BiPortalResource, lockToken?: string | null) =>
    http.post<R<BiPortalResource>, R<BiPortalResource>>(
      `/canvas/bi/portals/resources/${portalKey}/draft`,
      resource,
      draftLockConfig(lockToken),
    ),
  publishPortal: (portalKey: string) =>
    http.post<R<BiPortalResource>, R<BiPortalResource>>(`/canvas/bi/portals/resources/${portalKey}/publish`, {}),
  archivePortal: (portalKey: string) =>
    http.delete<R<BiPortalResource>, R<BiPortalResource>>(`/canvas/bi/portals/resources/${portalKey}`),
  listPortalVersions: (portalKey: string, limit = 20) =>
    http.get<R<BiPortalVersionView[]>, R<BiPortalVersionView[]>>(`/canvas/bi/portals/resources/${portalKey}/versions?limit=${limit}`),
  restorePortalVersion: (portalKey: string, version: number, lockToken?: string | null) =>
    http.post<R<BiPortalResource>, R<BiPortalResource>>(
      `/canvas/bi/portals/resources/${portalKey}/versions/${version}/restore`,
      {},
      draftLockConfig(lockToken),
    ),
  listBigScreenResources: () =>
    http.get<R<BiBigScreenResource[]>, R<BiBigScreenResource[]>>('/canvas/bi/big-screens/resources'),
  getBigScreenResource: (screenKey: string) =>
    http.get<R<BiBigScreenResource>, R<BiBigScreenResource>>(`/canvas/bi/big-screens/resources/${screenKey}`),
  saveBigScreenDraft: (screenKey: string, resource: BiBigScreenResource, lockToken?: string | null) =>
    http.post<R<BiBigScreenResource>, R<BiBigScreenResource>>(
      `/canvas/bi/big-screens/resources/${screenKey}/draft`,
      resource,
      draftLockConfig(lockToken),
    ),
  publishBigScreen: (screenKey: string) =>
    http.post<R<BiBigScreenResource>, R<BiBigScreenResource>>(`/canvas/bi/big-screens/resources/${screenKey}/publish`, {}),
  archiveBigScreen: (screenKey: string) =>
    http.delete<R<BiBigScreenResource>, R<BiBigScreenResource>>(`/canvas/bi/big-screens/resources/${screenKey}`),
  listBigScreenVersions: (screenKey: string, limit = 20) =>
    http.get<R<BiBigScreenVersionView[]>, R<BiBigScreenVersionView[]>>(`/canvas/bi/big-screens/resources/${screenKey}/versions?limit=${limit}`),
  restoreBigScreenVersion: (screenKey: string, version: number, lockToken?: string | null) =>
    http.post<R<BiBigScreenResource>, R<BiBigScreenResource>>(
      `/canvas/bi/big-screens/resources/${screenKey}/versions/${version}/restore`,
      {},
      draftLockConfig(lockToken),
    ),
  listSpreadsheetResources: () =>
    http.get<R<BiSpreadsheetResource[]>, R<BiSpreadsheetResource[]>>('/canvas/bi/spreadsheets/resources'),
  getSpreadsheetResource: (spreadsheetKey: string) =>
    http.get<R<BiSpreadsheetResource>, R<BiSpreadsheetResource>>(`/canvas/bi/spreadsheets/resources/${spreadsheetKey}`),
  saveSpreadsheetDraft: (spreadsheetKey: string, resource: BiSpreadsheetResource, lockToken?: string | null) =>
    http.post<R<BiSpreadsheetResource>, R<BiSpreadsheetResource>>(
      `/canvas/bi/spreadsheets/resources/${spreadsheetKey}/draft`,
      resource,
      draftLockConfig(lockToken),
    ),
  publishSpreadsheet: (spreadsheetKey: string) =>
    http.post<R<BiSpreadsheetResource>, R<BiSpreadsheetResource>>(`/canvas/bi/spreadsheets/resources/${spreadsheetKey}/publish`, {}),
  archiveSpreadsheet: (spreadsheetKey: string) =>
    http.delete<R<BiSpreadsheetResource>, R<BiSpreadsheetResource>>(`/canvas/bi/spreadsheets/resources/${spreadsheetKey}`),
  listSpreadsheetVersions: (spreadsheetKey: string, limit = 20) =>
    http.get<R<BiSpreadsheetVersionView[]>, R<BiSpreadsheetVersionView[]>>(`/canvas/bi/spreadsheets/resources/${spreadsheetKey}/versions?limit=${limit}`),
  restoreSpreadsheetVersion: (spreadsheetKey: string, version: number, lockToken?: string | null) =>
    http.post<R<BiSpreadsheetResource>, R<BiSpreadsheetResource>>(
      `/canvas/bi/spreadsheets/resources/${spreadsheetKey}/versions/${version}/restore`,
      {},
      draftLockConfig(lockToken),
    ),
  createEmbedTicket: (request: BiEmbedTicketRequest) =>
    http.post<R<BiEmbedTicket>, R<BiEmbedTicket>>('/canvas/bi/embed-tickets', request),
  verifyEmbedTicket: (ticket: string) =>
    http.post<R<BiEmbedTicketPayload>, R<BiEmbedTicketPayload>>('/canvas/bi/embed-tickets/verify', { ticket }),
  executeEmbedQuery: (request: BiEmbedQueryRequest) =>
    http.post<R<BiQueryResult>, R<BiQueryResult>>('/canvas/bi/embed/query/execute', request),
  getEmbedDashboardResource: (request: BiEmbedDashboardResourceRequest) =>
    http.post<R<BiDashboardResource>, R<BiDashboardResource>>('/canvas/bi/embed/resources/dashboard', request),
  getEmbedDashboardRuntimeState: (request: BiEmbedDashboardResourceRequest) =>
    http.post<R<BiDashboardRuntimeStateView>, R<BiDashboardRuntimeStateView>>('/canvas/bi/embed/resources/dashboard/runtime-state', request),
  getEmbedPortalResource: (request: BiEmbedDashboardResourceRequest) =>
    http.post<R<BiPortalResource>, R<BiPortalResource>>('/canvas/bi/embed/resources/portal', request),
  cleanupEmbedTickets: (limit = 100) =>
    http.post<R<BiEmbedTokenCleanupResult>, R<BiEmbedTokenCleanupResult>>(`/canvas/bi/embed-tickets/cleanup?limit=${limit}`, {}),
  compileQuery: (request: BiQueryRequest) =>
    http.post<R<BiCompiledQuery>, R<BiCompiledQuery>>('/canvas/bi/query/compile', request),
  executeQuery: (request: BiQueryRequest) =>
    http.post<R<BiQueryResult>, R<BiQueryResult>>('/canvas/bi/query/execute', request),
  explainQuery: (request: BiQueryRequest) =>
    http.post<R<BiQueryExplanation>, R<BiQueryExplanation>>('/canvas/bi/query/explain', request),
  cancelQuery: (sqlHash: string) =>
    http.post<R<BiQueryCancellationResult>, R<BiQueryCancellationResult>>(`/canvas/bi/query/cancel/${encodeURIComponent(sqlHash)}`, {}),
  listQueryHistory: (limit = 20) =>
    http.get<R<BiQueryHistoryItem[]>, R<BiQueryHistoryItem[]>>(`/canvas/bi/query/history?limit=${limit}`),
  getQueryHistoryDetail: (historyId: number) =>
    http.get<R<BiQueryHistoryDetail>, R<BiQueryHistoryDetail>>(`/canvas/bi/query/history/${historyId}`),
  getQueryGovernanceSummary: (limit = 100) =>
    http.get<R<BiQueryGovernanceSummary>, R<BiQueryGovernanceSummary>>(`/canvas/bi/query/governance-summary?limit=${limit}`),
  getQueryGovernancePolicy: () =>
    http.get<R<BiQueryGovernancePolicyView>, R<BiQueryGovernancePolicyView>>('/canvas/bi/query/governance-policy'),
  updateQueryGovernancePolicy: (command: BiQueryGovernancePolicyCommand) =>
    http.post<R<BiQueryGovernancePolicyView>, R<BiQueryGovernancePolicyView>>('/canvas/bi/query/governance-policy', command),
  listQueryGovernanceAudit: (limit = 20) =>
    http.get<R<BiQueryGovernanceAuditEntry[]>, R<BiQueryGovernanceAuditEntry[]>>(`/canvas/bi/query/governance-audit?limit=${limit}`),
  getQueryCachePolicy: () =>
    http.get<R<BiQueryCachePolicyView>, R<BiQueryCachePolicyView>>('/canvas/bi/query/cache-policy'),
  updateQueryCachePolicy: (command: BiQueryCachePolicyCommand) =>
    http.post<R<BiQueryCachePolicyView>, R<BiQueryCachePolicyView>>('/canvas/bi/query/cache-policy', command),
  invalidateQueryCache: (command: BiQueryCacheInvalidationCommand) =>
    http.post<R<BiQueryCacheInvalidationResult>, R<BiQueryCacheInvalidationResult>>('/canvas/bi/query/cache/invalidate', command),
  getQueryCacheStats: () =>
    http.get<R<BiQueryCacheStats>, R<BiQueryCacheStats>>('/canvas/bi/query/cache-stats'),
  getQuickEngineCapacity: (limit = 50) =>
    http.get<R<BiQuickEngineCapacitySummary>, R<BiQuickEngineCapacitySummary>>(`/canvas/bi/capacity/quick-engine?limit=${limit}`),
  upsertQuickEngineCapacityAlertPolicy: (command: BiQuickEngineCapacityAlertPolicyCommand) =>
    http.post<R<BiQuickEngineCapacityAlertPolicyView>, R<BiQuickEngineCapacityAlertPolicyView>>(
      '/canvas/bi/capacity/quick-engine/alert-policy',
      command,
    ),
  upsertQuickEngineTenantPoolPolicy: (command: BiQuickEngineTenantPoolPolicyCommand) =>
    http.post<R<BiQuickEngineTenantPoolPolicyView>, R<BiQuickEngineTenantPoolPolicyView>>(
      '/canvas/bi/capacity/quick-engine/tenant-pool-policy',
      command,
    ),
  getDatasetAccelerationPolicy: (datasetKey: string) =>
    http.get<R<BiDatasetAccelerationPolicyView>, R<BiDatasetAccelerationPolicyView>>(
      `/canvas/bi/datasets/resources/${encodeURIComponent(datasetKey)}/acceleration-policy`,
    ),
  updateDatasetAccelerationPolicy: (datasetKey: string, command: BiDatasetAccelerationPolicyCommand) =>
    http.post<R<BiDatasetAccelerationPolicyView>, R<BiDatasetAccelerationPolicyView>>(
      `/canvas/bi/datasets/resources/${encodeURIComponent(datasetKey)}/acceleration-policy`,
      command,
    ),
  refreshDatasetAcceleration: (datasetKey: string) =>
    http.post<R<BiDatasetExtractRefreshRunView>, R<BiDatasetExtractRefreshRunView>>(
      `/canvas/bi/datasets/resources/${encodeURIComponent(datasetKey)}/acceleration-refresh`,
      {},
    ),
  listDatasetAccelerationRuns: (datasetKey: string, limit = 10) =>
    http.get<R<BiDatasetExtractRefreshRunView[]>, R<BiDatasetExtractRefreshRunView[]>>(
      `/canvas/bi/datasets/resources/${encodeURIComponent(datasetKey)}/acceleration-runs?limit=${limit}`,
    ),
  runDatasetAccelerationScheduler: () =>
    http.post<R<BiDatasetAccelerationSchedulerResult>, R<BiDatasetAccelerationSchedulerResult>>(
      '/canvas/bi/datasets/resources/acceleration-scheduler/run',
      {},
    ),
  listDatasourceHealth: () =>
    http.get<R<BiDatasourceHealth[]>, R<BiDatasourceHealth[]>>('/canvas/bi/datasources/health'),
  listDatasourceHealthHistory: (limit = 20) =>
    http.get<R<BiDatasourceHealthSnapshot[]>, R<BiDatasourceHealthSnapshot[]>>(`/canvas/bi/datasources/health/history?limit=${limit}`),
  getDatasourceHealthSlo: (limit = 100) =>
    http.get<R<BiDatasourceHealthSloSummary>, R<BiDatasourceHealthSloSummary>>(`/canvas/bi/datasources/health/slo?limit=${limit}`),
  listDatasourceConnectors: () =>
    http.get<R<BiDatasourceConnectorCapability[]>, R<BiDatasourceConnectorCapability[]>>('/canvas/bi/datasources/connectors'),
  listDatasourceOnboarding: () =>
    http.get<R<BiDatasourceOnboardingView[]>, R<BiDatasourceOnboardingView[]>>('/canvas/bi/datasources/onboarding'),
  createDatasourceOnboarding: (command: BiDatasourceOnboardingCommand) =>
    http.post<R<BiDatasourceOnboardingView>, R<BiDatasourceOnboardingView>>('/canvas/bi/datasources/onboarding', command),
  updateDatasourceOnboarding: (id: number, command: BiDatasourceOnboardingCommand) =>
    http.put<R<BiDatasourceOnboardingView>, R<BiDatasourceOnboardingView>>(`/canvas/bi/datasources/onboarding/${id}`, command),
  uploadDatasourceFile: (file: File, options: BiDatasourceFileUploadOptions) => {
    const form = new FormData()
    form.append('file', file)
    return http.post<R<BiDatasourceOnboardingView>, R<BiDatasourceOnboardingView>>(
      `/canvas/bi/datasources/file-upload${permissionQuery({
        name: options.name,
        description: options.description,
        sheetName: options.sheetName,
        delimiter: options.delimiter,
        headerRow: options.headerRow === false ? 'false' : 'true',
        encoding: options.encoding,
      })}`,
      form,
    )
  },
  uploadAndMaterializeDatasourceFile: (file: File, options: BiDatasourceFileMaterializationOptions) => {
    const form = new FormData()
    form.append('file', file)
    return http.post<R<BiDatasourceFileMaterializationResult>, R<BiDatasourceFileMaterializationResult>>(
      `/canvas/bi/datasources/file-upload/materialize${permissionQuery({
        name: options.name,
        description: options.description,
        sheetName: options.sheetName,
        delimiter: options.delimiter,
        headerRow: options.headerRow === false ? 'false' : 'true',
        encoding: options.encoding,
        datasetKey: options.datasetKey,
        datasetName: options.datasetName,
        tenantColumn: options.tenantColumn,
        schemaLimit: options.schemaLimit,
        maxRows: options.maxRows,
      })}`,
      form,
    )
  },
  testDatasourceConnection: (id: number) =>
    http.post<R<BiDatasourceConnectionTestResult>, R<BiDatasourceConnectionTestResult>>(`/canvas/bi/datasources/${id}/connection-test`, {}),
  rotateDatasourceCredential: (id: number, command: BiDatasourceCredentialRotationCommand) =>
    http.post<R<BiDatasourceCredentialRotationView>, R<BiDatasourceCredentialRotationView>>(
      `/canvas/bi/datasources/${id}/credential-rotation`,
      command,
    ),
  previewDatasourceSchema: (id: number, limit = 100) =>
    http.get<R<BiDatasourceSchemaPreview>, R<BiDatasourceSchemaPreview>>(`/canvas/bi/datasources/${id}/schema-preview?limit=${limit}`),
  previewApiDatasource: (id: number, request: BiDatasourceApiPreviewRequest = { variables: {}, limit: 50 }) =>
    http.post<R<BiDatasourceApiPreview>, R<BiDatasourceApiPreview>>(`/canvas/bi/datasources/${id}/api-preview`, request),
  syncDatasourceSchema: (id: number, limit = 100, request?: BiDatasourceApiPreviewRequest) =>
    http.post<R<BiDatasourceSchemaSnapshotView>, R<BiDatasourceSchemaSnapshotView>>(
      `/canvas/bi/datasources/${id}/schema-sync?limit=${limit}`,
      request ?? {},
    ),
  getDatasourceSchemaSnapshot: (id: number) =>
    http.get<R<BiDatasourceSchemaSnapshotView>, R<BiDatasourceSchemaSnapshotView>>(`/canvas/bi/datasources/${id}/schema-snapshot`),
  listDatasourceSchemaSnapshots: (id: number, limit = 20) =>
    http.get<R<BiDatasourceSchemaSnapshotView[]>, R<BiDatasourceSchemaSnapshotView[]>>(`/canvas/bi/datasources/${id}/schema-snapshots?limit=${limit}`),
  listResourcePermissions: (params: { resourceType?: string; resourceKey?: string; resourceId?: number } = {}) =>
    http.get<R<BiResourcePermissionView[]>, R<BiResourcePermissionView[]>>(`/canvas/bi/permissions/resources${permissionQuery(params)}`),
  upsertResourcePermission: (command: BiResourcePermissionCommand) =>
    http.post<R<BiResourcePermissionView>, R<BiResourcePermissionView>>('/canvas/bi/permissions/resources', command),
  deleteResourcePermission: (id: number) =>
    http.delete<R<void>, R<void>>(`/canvas/bi/permissions/resources/${id}`),
  listResourceLocations: (resourceType?: string) =>
    http.get<R<BiResourceLocationView[]>, R<BiResourceLocationView[]>>(`/canvas/bi/resources/locations${permissionQuery({ resourceType })}`),
  moveResource: (command: BiResourceMoveCommand) =>
    http.post<R<BiResourceLocationView>, R<BiResourceLocationView>>('/canvas/bi/resources/move', command),
  listResourceOwnerships: (resourceType?: string) =>
    http.get<R<BiResourceOwnershipView[]>, R<BiResourceOwnershipView[]>>(`/canvas/bi/resources/ownerships${permissionQuery({ resourceType })}`),
  transferResource: (command: BiResourceTransferCommand) =>
    http.post<R<BiResourceOwnershipView>, R<BiResourceOwnershipView>>('/canvas/bi/resources/transfer', command),
  listResourceFavorites: (resourceType?: string) =>
    http.get<R<BiResourceFavoriteView[]>, R<BiResourceFavoriteView[]>>(`/canvas/bi/resources/favorites${permissionQuery({ resourceType })}`),
  favoriteResource: (command: BiResourceFavoriteCommand) =>
    http.post<R<BiResourceFavoriteView>, R<BiResourceFavoriteView>>('/canvas/bi/resources/favorites', command),
  listResourceComments: (resourceType: string, resourceKey: string) =>
    http.get<R<BiResourceCommentView[]>, R<BiResourceCommentView[]>>(
      `/canvas/bi/resources/comments${permissionQuery({ resourceType, resourceKey })}`,
    ),
  addResourceComment: (command: BiResourceCommentCommand) =>
    http.post<R<BiResourceCommentView>, R<BiResourceCommentView>>('/canvas/bi/resources/comments', command),
  deleteResourceComment: (commentId: number) =>
    http.delete<R<void>, R<void>>(`/canvas/bi/resources/comments/${commentId}`),
  getResourceLock: (resourceType: string, resourceKey: string) =>
    http.get<R<BiResourceLockView | null>, R<BiResourceLockView | null>>(
      `/canvas/bi/resources/locks${permissionQuery({ resourceType, resourceKey })}`,
    ),
  acquireResourceLock: (command: BiResourceLockCommand) =>
    http.post<R<BiResourceLockView>, R<BiResourceLockView>>('/canvas/bi/resources/locks/acquire', command),
  releaseResourceLock: (command: BiResourceLockCommand) =>
    http.post<R<void>, R<void>>('/canvas/bi/resources/locks/release', command),
  listPublishApprovals: (params: { resourceType?: string; resourceKey?: string; status?: string } = {}) =>
    http.get<R<BiPublishApprovalView[]>, R<BiPublishApprovalView[]>>(
      `/canvas/bi/resources/publish-approvals${permissionQuery(params)}`,
    ),
  requestPublishApproval: (command: BiPublishApprovalRequestCommand) =>
    http.post<R<BiPublishApprovalView>, R<BiPublishApprovalView>>('/canvas/bi/resources/publish-approvals', command),
  reviewPublishApproval: (approvalId: number, command: BiPublishApprovalReviewCommand) =>
    http.post<R<BiPublishApprovalView>, R<BiPublishApprovalView>>(
      `/canvas/bi/resources/publish-approvals/${approvalId}/review`,
      command,
    ),
  listRowPermissions: (datasetKey?: string) =>
    http.get<R<BiRowPermissionView[]>, R<BiRowPermissionView[]>>(`/canvas/bi/permissions/rows${permissionQuery({ datasetKey })}`),
  upsertRowPermission: (command: BiRowPermissionCommand) =>
    http.post<R<BiRowPermissionView>, R<BiRowPermissionView>>('/canvas/bi/permissions/rows', command),
  deleteRowPermission: (id: number) =>
    http.delete<R<void>, R<void>>(`/canvas/bi/permissions/rows/${id}`),
  listColumnPermissions: (datasetKey?: string) =>
    http.get<R<BiColumnPermissionView[]>, R<BiColumnPermissionView[]>>(`/canvas/bi/permissions/columns${permissionQuery({ datasetKey })}`),
  upsertColumnPermission: (command: BiColumnPermissionCommand) =>
    http.post<R<BiColumnPermissionView>, R<BiColumnPermissionView>>('/canvas/bi/permissions/columns', command),
  deleteColumnPermission: (id: number) =>
    http.delete<R<void>, R<void>>(`/canvas/bi/permissions/columns/${id}`),
  listPermissionAudit: (limit = 20) =>
    http.get<R<BiPermissionAuditEntry[]>, R<BiPermissionAuditEntry[]>>(`/canvas/bi/permissions/audit?limit=${limit}`),
  listPermissionRequests: (params: { resourceType?: string; resourceKey?: string; status?: string } = {}) =>
    http.get<R<BiPermissionRequestView[]>, R<BiPermissionRequestView[]>>(
      `/canvas/bi/permissions/requests${permissionQuery(params)}`,
    ),
  requestPermission: (command: BiPermissionRequestCommand) =>
    http.post<R<BiPermissionRequestView>, R<BiPermissionRequestView>>('/canvas/bi/permissions/requests', command),
  reviewPermissionRequest: (requestId: number, command: BiPermissionRequestReviewCommand) =>
    http.post<R<BiPermissionRequestView>, R<BiPermissionRequestView>>(
      `/canvas/bi/permissions/requests/${requestId}/review`,
      command,
    ),
  previewSelfService: (request: BiSelfServicePreviewRequest) =>
    http.post<R<BiQueryResult>, R<BiQueryResult>>('/canvas/bi/self-service/preview', request),
  createExport: (command: BiExportJobCommand) =>
    http.post<R<BiExportJobView>, R<BiExportJobView>>('/canvas/bi/self-service/exports', command),
  listExports: (limit = 20) =>
    http.get<R<BiExportJobView[]>, R<BiExportJobView[]>>(`/canvas/bi/self-service/exports?limit=${limit}`),
  getExportDetail: (id: number) =>
    http.get<R<BiExportJobDetailView>, R<BiExportJobDetailView>>(`/canvas/bi/self-service/exports/${id}`),
  reviewExport: (id: number, command: BiExportApprovalReviewCommand) =>
    http.post<R<BiExportJobView>, R<BiExportJobView>>(`/canvas/bi/self-service/exports/${id}/review`, command),
  cancelExport: (id: number) =>
    http.post<R<BiExportJobView>, R<BiExportJobView>>(`/canvas/bi/self-service/exports/${id}/cancel`, {}),
  cleanupExports: (limit = 100) =>
    http.post<R<BiExportCleanupResult>, R<BiExportCleanupResult>>(`/canvas/bi/self-service/exports/cleanup?limit=${limit}`, {}),
  retryExports: (limit = 20) =>
    http.post<R<BiExportRetryResult>, R<BiExportRetryResult>>(`/canvas/bi/self-service/exports/retry?limit=${limit}`, {}),
  listSubscriptions: (limit = 20) =>
    http.get<R<BiSubscriptionView[]>, R<BiSubscriptionView[]>>(`/canvas/bi/subscriptions?limit=${limit}`),
  upsertSubscription: (command: BiSubscriptionCommand) =>
    http.post<R<BiSubscriptionView>, R<BiSubscriptionView>>('/canvas/bi/subscriptions', command),
  deleteSubscription: (id: number) =>
    http.delete<R<void>, R<void>>(`/canvas/bi/subscriptions/${id}`),
  runSubscription: (id: number) =>
    http.post<R<BiDeliveryRunResult>, R<BiDeliveryRunResult>>(`/canvas/bi/subscriptions/${id}/run`, {}),
  listAlerts: (limit = 20) =>
    http.get<R<BiAlertRuleView[]>, R<BiAlertRuleView[]>>(`/canvas/bi/alerts?limit=${limit}`),
  upsertAlert: (command: BiAlertRuleCommand) =>
    http.post<R<BiAlertRuleView>, R<BiAlertRuleView>>('/canvas/bi/alerts', command),
  deleteAlert: (id: number) =>
    http.delete<R<void>, R<void>>(`/canvas/bi/alerts/${id}`),
  runAlert: (id: number) =>
    http.post<R<BiDeliveryRunResult>, R<BiDeliveryRunResult>>(`/canvas/bi/alerts/${id}/run`, {}),
  listDeliveryLogs: (limit = 20, params: { jobType?: string; jobId?: number } = {}) =>
    http.get<R<BiDeliveryLogView[]>, R<BiDeliveryLogView[]>>(`/canvas/bi/delivery-logs${permissionQuery({ ...params, limit })}`),
  auditDeliveryLogs: (limit = 50, params: { jobType?: string; status?: string; channel?: string; jobId?: number } = {}) =>
    http.get<R<BiDeliveryAuditSummary>, R<BiDeliveryAuditSummary>>(`/canvas/bi/delivery-audit${permissionQuery({ ...params, limit })}`),
  listDeliveryAttachments: (limit = 20, params: { jobType?: string; jobId?: number; deliveryLogId?: number } = {}) =>
    http.get<R<BiDeliveryAttachmentView[]>, R<BiDeliveryAttachmentView[]>>(`/canvas/bi/delivery-attachments${permissionQuery({ ...params, limit })}`),
  cleanupDeliveryAttachments: (limit = 100) =>
    http.post<R<BiDeliveryAttachmentCleanupResult>, R<BiDeliveryAttachmentCleanupResult>>(`/canvas/bi/delivery-attachments/cleanup?limit=${limit}`, {}),
  retryDeliveryLogs: (limit = 20) =>
    http.post<R<BiDeliveryRetryResult>, R<BiDeliveryRetryResult>>(`/canvas/bi/delivery-logs/retry?limit=${limit}`, {}),
  runDeliveryScheduler: () =>
    http.post<R<BiDeliverySchedulerResult>, R<BiDeliverySchedulerResult>>('/canvas/bi/delivery-scheduler/run', {}),
}
