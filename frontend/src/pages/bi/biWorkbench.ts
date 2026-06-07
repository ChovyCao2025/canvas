import type {
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
  key: BiSectionKey
  label: string
  summary: string
  capability: string
}

export interface MarketingDatasetPreset {
  key: string
  label: string
  source: string
  preset: boolean
  metrics: string[]
}

export interface BiDatasetMetadataLike {
  datasetKey: string
  metrics: Array<{ metricKey: string }>
}

export interface BiDashboardPresetLike {
  dashboardKey: string
  title: string
  description: string
  datasetKey: string
  widgets: BiDashboardWidgetPreset[]
  filters: BiDashboardFilterPreset[]
  globalParameters?: BiDashboardGlobalParameterPreset[] | null
  interactions: BiDashboardInteractionPreset[]
  subscriptionChannels: string[]
  embedScopes: string[]
}

export interface BiDashboardWidgetPreset {
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

export interface BiDashboardFilterPreset {
  filterKey: string
  fieldKey: string
  label: string
  controlType: string
  required: boolean
  defaultValue?: string | null
  targetWidgetKeys?: string[] | null
  cascade?: BiDashboardFilterCascadePreset | null
  optionDatasetKey?: string | null
  optionFieldKey?: string | null
  hidden?: boolean | null
}

export interface BiDashboardFilterCascadePreset {
  parentFilterKeys: string[]
  parentFieldMapping?: Record<string, string> | null
  mode?: 'SAME_SOURCE' | 'MAPPED' | null
}

export interface BiDashboardGlobalParameterPreset {
  parameterKey: string
  fieldKey?: string | null
  filterKey?: string | null
  aliases?: string[] | null
  defaultValue?: string | null
  locked?: boolean | null
}

export interface BiDashboardInteractionPreset {
  interactionKey: string
  sourceWidgetKey: string
  targetWidgetKey?: string | null
  interactionType: string
  fieldKey: string
  target?: string | null
}

export type BiDashboardRuntimeParameterValue = string | string[] | number | boolean | null | undefined
export type BiDashboardRuntimeParameters = Record<string, BiDashboardRuntimeParameterValue>
export type BiDashboardRuntimeStateSource = 'URL' | 'REMEMBERED' | 'DEFAULT' | 'CLEARED' | 'EMPTY'

export interface BiDashboardRuntimeStateRow {
  key: string
  fieldKey: string
  label: string
  valueText: string
  source: BiDashboardRuntimeStateSource
  sourceLabel: string
  locked: boolean
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
  past: BiDashboardPresetLike[]
  present: BiDashboardPresetLike
  future: BiDashboardPresetLike[]
}

export interface BiDashboardPackageLike {
  preset: BiDashboardPresetLike
  sourceVersion?: number
}

export interface BiResourceLocationLike {
  id?: number
  tenantId?: number
  workspaceId?: number
  resourceType: string
  resourceKey: string
  folderKey?: string | null
  sortOrder?: number | null
  movedBy?: string | null
  movedAt?: string | null
}

export interface BiResourceMoveCommandLike {
  resourceType: string
  resourceKey: string
  folderKey: string | null
  sortOrder: number
}

export interface BiResourceOwnershipLike {
  id?: number
  tenantId?: number
  workspaceId?: number
  resourceType: string
  resourceKey: string
  ownerUser?: string | null
  transferredBy?: string | null
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
  id?: number | null
  tenantId?: number
  workspaceId?: number
  resourceType: string
  resourceKey: string
  lockToken?: string | null
  lockedBy?: string | null
  lockedAt?: string | null
  expiresAt?: string | null
  locked?: boolean | null
}

export interface BiPublishApprovalLike {
  id?: number | null
  tenantId?: number
  workspaceId?: number
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
  resourceType: string
  resourceKey: string
  requestedAction: string
  reason: string | null
}

export interface BiPermissionRequestReviewCommandLike {
  requestId: number | null
  status: string
  reviewComment: string | null
}

export interface BiResourceTargetLike {
  label: string
  value: string
  resourceType: string
  resourceKey: string
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
  navigationLayout?: string | null
  defaultMenuKey?: string | null
  menuSearchEnabled?: boolean | null
  fullScreenEnabled?: boolean | null
  mobileEnabled?: boolean | null
}

export interface BiPortalMenuResourceLike {
  menuKey?: unknown
  sortOrder?: unknown
}

export interface BiPortalResourceLike {
  theme?: Record<string, unknown> | null
  menus?: BiPortalMenuResourceLike[] | null
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

export interface BiSpreadsheetPivotTableInputLike {
  sourceRange: string
  targetCell: string
  rowField: string
  columnField: string
  valueField: string
  aggregation?: BiSpreadsheetPivotAggregation | string | null
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

export interface BiExportApprovalReviewCommandLike {
  status: 'APPROVED' | 'REJECTED'
  reviewComment: string | null
}

export interface BiExportAuditJobLike {
  id?: number | null
  resourceKey?: string | null
  resourceId?: number | null
  exportFormat?: string | null
  status?: string | null
  progressPercent?: number | null
  storageProvider?: string | null
  storageKey?: string | null
  downloadCount?: number | null
  lastDownloadedAt?: string | null
  approvalStatus?: string | null
  approvalReason?: string | null
  requestedBy?: string | null
  reviewedBy?: string | null
  retryCount?: number | null
  maxRetryCount?: number | null
  createdBy?: string | null
  createdAt?: string | null
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
}

export interface BiExportAuditDetailRow {
  label: string
  value: string
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

export function canvasBiEntrypoint(canvasId: number | string): string {
  return `/bi?dashboard=canvas-effect&canvasId=${canvasId}`
}

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

export function buildWidgetQueryRequest(
  preset: BiDashboardPresetLike,
  widget: BiDashboardWidgetPreset,
  canvasId?: string | null,
  runtimeParametersOrLimit?: BiDashboardRuntimeParameters | number | null,
  limit = 500,
): BiQueryRequest {
  const runtimeParameters = typeof runtimeParametersOrLimit === 'number' ? null : runtimeParametersOrLimit
  const effectiveLimit = typeof runtimeParametersOrLimit === 'number' ? runtimeParametersOrLimit : limit
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
  params.set('dashboard', preset.dashboardKey)
  appendInteractionParameter(params, 'canvasId', options.canvasId)
  appendInteractionParameter(params, 'widget', interaction.targetWidgetKey)
  appendRuntimeParameters(params, options.runtimeParameters)

  const filterKey = interactionFilterParameterKey(preset, interaction)
  const value = interactionRowValue(row, interaction.fieldKey)
  appendInteractionParameter(params, filterKey, value)
  return `/bi?${params.toString()}`
}

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

export function dropSelfServiceExtractionField(
  state: SelfServiceExtractionState,
  role: 'DIMENSION' | 'METRIC',
  fieldKey: string,
): SelfServiceExtractionState {
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

export function buildSelfServiceExtractionQuery(
  preset: BiDashboardPresetLike,
  state: SelfServiceExtractionState,
  canvasId?: string | null,
  runtimeParametersOrLimit?: BiDashboardRuntimeParameters | number | null,
  limit = 500,
): BiQueryRequest {
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

export function dashboardRuntimeParametersFromSearchParams(
  preset: BiDashboardPresetLike,
  searchParams: URLSearchParams,
): BiDashboardRuntimeParameters {
  const result: BiDashboardRuntimeParameters = {}
  for (const filter of preset.filters) {
    const valueByFilterKey = searchParamValue(searchParams, filter.filterKey)
    if (valueByFilterKey != null) {
      result[filter.filterKey] = valueByFilterKey
      continue
    }
    const valueByFieldKey = searchParamValue(searchParams, filter.fieldKey)
    if (valueByFieldKey != null) {
      result[filter.fieldKey] = valueByFieldKey
    }
  }
  for (const parameter of dashboardGlobalParameters(preset)) {
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
  const rememberedParameters = rememberedParametersOrToday instanceof Date
    ? null
    : rememberedParametersOrToday ?? null
  const effectiveToday = rememberedParametersOrToday instanceof Date ? rememberedParametersOrToday : today
  return resolveDashboardRuntimeParameters(preset, embedTicketPayloadSearchParams(payload), rememberedParameters, effectiveToday)
}

export function resolveDashboardRuntimeParameters(
  preset: BiDashboardPresetLike,
  searchParams: URLSearchParams,
  rememberedParameters?: BiDashboardRuntimeParameters | null,
  today = new Date(),
): BiDashboardRuntimeParameters {
  const explicitParameters = dashboardRuntimeParametersFromSearchParams(preset, searchParams)
  const result: BiDashboardRuntimeParameters = {}
  for (const filter of preset.filters) {
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
  const clearedAliases = new Set(clearedParameterKeys.flatMap(key => dashboardRuntimeSearchParamAliases(preset, key)))
  let effectiveSearchParams = new URLSearchParams(searchParams)
  for (const key of clearedParameterKeys) {
    effectiveSearchParams = stripDashboardRuntimeSearchParam(preset, effectiveSearchParams, key)
  }
  const explicitParameters = dashboardRuntimeParametersFromSearchParams(preset, effectiveSearchParams)
  const originalExplicitParameters = dashboardRuntimeParametersFromSearchParams(preset, searchParams)
  return preset.filters.map(filter => {
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
  if (globalParameter?.locked && dashboardRuntimeValueForGlobalParameter(globalParameter, currentParameters) != null) {
    return { ...(currentParameters ?? {}) }
  }
  const next = { ...(currentParameters ?? {}) }
  const value = dashboardRuntimeControlParameterValue(filter, rawValue)
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
  const sourceCells = spreadsheetCellRangeFromText(input.sourceRange)
  const sourceRows = spreadsheetCellRows(sourceCells)
  const headerCells = sourceRows[0] ?? []
  const headers = headerCells.map(cellKey => String(currentCells[cellKey] ?? '').trim())
  const rowFieldIndex = headers.findIndex(header => header === input.rowField.trim())
  const columnFieldIndex = headers.findIndex(header => header === input.columnField.trim())
  const valueFieldIndex = headers.findIndex(header => header === input.valueField.trim())
  const target = parseSpreadsheetCellKey(input.targetCell)
  if (!target || rowFieldIndex < 0 || columnFieldIndex < 0 || valueFieldIndex < 0) {
    sheets[sheetIndex] = currentSheet
    return { ...resource, sheets }
  }

  const aggregation = normalizeSpreadsheetPivotAggregation(input.aggregation)
  const rowLabels: string[] = []
  const columnLabels: string[] = []
  const buckets = new Map<string, unknown[]>()

  for (const row of sourceRows.slice(1)) {
    const rowLabel = String(currentCells[row[rowFieldIndex]] ?? '').trim()
    const columnLabel = String(currentCells[row[columnFieldIndex]] ?? '').trim()
    if (!rowLabel || !columnLabel) continue
    if (!rowLabels.includes(rowLabel)) rowLabels.push(rowLabel)
    if (!columnLabels.includes(columnLabel)) columnLabels.push(columnLabel)
    const bucketKey = `${rowLabel}\u0000${columnLabel}`
    buckets.set(bucketKey, [...(buckets.get(bucketKey) ?? []), currentCells[row[valueFieldIndex]]])
  }

  const cells = { ...currentCells }
  const targetColumn = target.column
  const targetRow = target.row
  cells[spreadsheetColumnName(targetColumn) + targetRow] = `${input.rowField.trim()} / ${input.columnField.trim()}`
  columnLabels.forEach((label, index) => {
    cells[spreadsheetColumnName(targetColumn + index + 1) + targetRow] = label
  })
  rowLabels.forEach((rowLabel, rowIndex) => {
    const outputRow = targetRow + rowIndex + 1
    cells[spreadsheetColumnName(targetColumn) + outputRow] = rowLabel
    columnLabels.forEach((columnLabel, columnIndex) => {
      const values = buckets.get(`${rowLabel}\u0000${columnLabel}`) ?? []
      cells[spreadsheetColumnName(targetColumn + columnIndex + 1) + outputRow] = evaluateSpreadsheetAggregate(aggregation, values)
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
  const aggregateRange = expression.match(/^(SUM|AVERAGE|MIN|MAX|COUNT)\(([A-Z]+[0-9]+):([A-Z]+[0-9]+)\)$/i)
  if (aggregateRange) {
    const values = spreadsheetCellRange(aggregateRange[2], aggregateRange[3])
      .map(cellKey => evaluateCell(cellKey))
    return evaluateSpreadsheetAggregate(aggregateRange[1], values)
  }
  return value
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
  return [
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
  return [
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
): { leftColumn: string; operator?: string; connector?: string; rightColumn: string } {
  const command: { leftColumn: string; operator?: string; connector?: string; rightColumn: string } = {
    leftColumn,
    rightColumn,
  }
  if (operator !== '=') command.operator = operator
  if (connector) command.connector = connector
  return command
}

function normalizeDatasourceJoinConditions(join: BiDatasourceMultiTableJoinInputLike): Array<{ leftColumn: string; operator?: string; connector?: string; rightColumn: string }> {
  const rawConditions = (join.conditions ?? []).length > 0
    ? join.conditions ?? []
    : [{ leftColumn: join.leftColumn, operator: '=', rightColumn: join.rightColumn }]
  const seen = new Set<string>()
  const conditions: Array<{ leftColumn: string; operator?: string; connector?: string; rightColumn: string }> = []
  rawConditions.forEach(condition => {
    const leftColumn = trimValue(condition?.leftColumn)
    const operator = normalizeDatasourceJoinConditionOperator(condition?.operator)
    const connector = normalizeDatasourceJoinConnector(condition?.connector)
    const rightColumn = trimValue(condition?.rightColumn)
    const key = `${leftColumn}${operator}${connector}${rightColumn}`
    if (!leftColumn || !rightColumn || seen.has(key)) return
    seen.add(key)
    conditions.push(datasourceJoinConditionCommand(leftColumn, operator, rightColumn, connector))
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
  return { ...portal, theme }
}

export function movePortalMenuItem<T extends BiPortalResourceLike>(
  portal: T,
  menuKey: string | null | undefined,
  direction: 'up' | 'down',
): T {
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
