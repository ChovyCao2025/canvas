import http from './api'
import type { R } from '../types'

export interface BiQueryFilter {
  field: string
  operator: 'EQ' | 'NEQ' | 'GT' | 'GTE' | 'LT' | 'LTE' | 'IN' | 'BETWEEN' | 'CONTAINS'
  value: unknown
}

export interface BiQuerySort {
  field: string
  direction: 'ASC' | 'DESC'
}

export interface BiQueryRequest {
  datasetKey: string
  dashboardKey?: string | null
  dimensions: string[]
  metrics: string[]
  filters: BiQueryFilter[]
  sorts: BiQuerySort[]
  limit: number
  offset?: number
  sqlParameters?: Record<string, string>
}

export interface BiCompiledQuery {
  sql: string
  parameters: unknown[]
}

export interface BiQueryExplanation {
  datasetKey: string
  sqlHash: string
  parametersCount: number
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
  datasetKey: string
  columns: BiQueryColumn[]
  rows: Record<string, unknown>[]
  rowCount: number
  durationMs: number
  sqlHash: string
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
}

export interface BiDatasourceHealthSloSummary {
  totalChecks: number
  availableChecks: number
  unavailableChecks: number
  availabilityRate: number
  sources: BiDatasourceHealthSourceSlo[]
}

export interface BiDatasourceConnectorCapability {
  connectorType: string
  label: string
  sourceCategory: string
  supportedModes: string[]
  supportStatus: string
  capacityCategory?: string | null
  capacityNote?: string | null
  supportsConnectionTest: boolean
  supportsSchemaSync: boolean
  supportsSqlDataset: boolean
  supportsTableDataset: boolean
  supportsCredentials: boolean
  driverClassNames: string[]
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
  fieldKey: string
  displayName: string
  columnExpression: string
  role: 'DIMENSION' | 'MEASURE'
  dataType: string
  semanticType?: string | null
  defaultAggregation?: string | null
  formatPattern?: string | null
  unit?: string | null
  folderKey?: string | null
  visible: boolean
  sensitiveLevel: string
  sortOrder: number
}

export interface BiMetricResource {
  metricKey: string
  displayName: string
  expression: string
  aggregation: string
  dataType: string
  unit?: string | null
  formatPattern?: string | null
  allowedDimensions: string[]
  owner?: string | null
  description?: string | null
  status: string
}

export interface BiDatasetResource {
  datasetKey: string
  name: string
  datasetType: string
  tableExpression: string
  tenantColumn: string
  model: Record<string, unknown>
  fields: BiDatasetFieldResource[]
  metrics: BiMetricResource[]
  status: string
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
  dashboardKey: string
  title: string
  description: string
  datasetKey: string
  widgets: BiDashboardWidget[]
  filters: BiDashboardFilter[]
  globalParameters?: BiDashboardGlobalParameter[] | null
  interactions: BiDashboardInteraction[]
  subscriptionChannels: string[]
  embedScopes: string[]
}

export interface BiDashboardResource {
  preset: BiDashboardPreset
  status: string
  version: number
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
  chartKey: string
  name: string
  chartType: string
  datasetKey: string
  query: BiQueryRequest
  style: Record<string, unknown>
  interaction: Record<string, unknown>
  status: string
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

export interface BiPortalMenuResource {
  menuKey: string
  parentMenuKey?: string | null
  title: string
  resourceType: string
  resourceKey?: string | null
  resourceId?: number | null
  externalUrl?: string | null
  visibility: Record<string, unknown>
  sortOrder: number
}

export interface BiPortalResource {
  portalKey: string
  name: string
  theme: Record<string, unknown>
  menus: BiPortalMenuResource[]
  status: string
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
  resourceType: string
  resourceKey: string
  scope: string
  filters: Record<string, string>
  parameters?: Record<string, string>
  ttlSeconds: number
  allowedDomains?: string[]
  maxAccessCount?: number
  rateLimitPerMinute?: number
}

export interface BiEmbedTicket {
  ticket: string
  expiresAt: string
  embedUrl: string
}

export interface BiEmbedTicketPayload {
  tenantId: number
  username: string
  resourceType: string
  resourceKey: string
  scope: string
  filters: Record<string, string>
  parameters: Record<string, string>
  allowedDomains: string[]
  maxAccessCount?: number | null
  rateLimitPerMinute?: number | null
  nonce: string
  issuedAt: string
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
  resourceType: string
  resourceKey?: string | null
  resourceId?: number | null
  subjectType: string
  subjectId: string
  actionKey: string
  effect: string
}

export interface BiResourcePermissionView extends BiResourcePermissionCommand {
  id: number
  tenantId: number
  workspaceId: number
  resourceKey?: string | null
  resourceId: number
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
  datasetKey: string
  ruleKey: string
  subjectType: string
  subjectId: string
  filters: BiQueryFilter[]
  filter: Record<string, unknown>
  enabled: boolean
}

export interface BiRowPermissionView {
  id: number
  tenantId: number
  datasetKey: string
  datasetId: number
  ruleKey: string
  subjectType: string
  subjectId: string
  filterJson: string
  enabled: boolean
  createdAt?: string | null
}

export interface BiColumnPermissionCommand {
  datasetKey: string
  fieldKey: string
  subjectType: string
  subjectId: string
  policy: string
  mask: Record<string, unknown>
  enabled: boolean
}

export interface BiColumnPermissionView {
  id: number
  tenantId: number
  datasetKey: string
  datasetId: number
  fieldKey: string
  subjectType: string
  subjectId: string
  policy: string
  maskJson?: string | null
  enabled: boolean
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
  resourceType: string
  resourceKey?: string | null
  resourceId?: number | null
  exportFormat: string
  query: BiQueryRequest
  rowLimit: number
  approvalRequired?: boolean | null
  sensitive?: boolean | null
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
  subscriptionKey: string
  name: string
  resourceType: string
  resourceKey?: string | null
  resourceId?: number | null
  schedule: Record<string, unknown>
  receivers: Record<string, unknown>
  delivery: Record<string, unknown>
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

function permissionQuery(params: Record<string, string | number | null | undefined>) {
  const search = new URLSearchParams()
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== '') search.set(key, String(value))
  })
  const value = search.toString()
  return value ? `?${value}` : ''
}

function draftLockConfig(lockToken?: string | null) {
  return lockToken ? { headers: { 'X-BI-LOCK-TOKEN': lockToken } } : undefined
}

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
