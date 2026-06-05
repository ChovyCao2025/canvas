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
  dimensions: string[]
  metrics: string[]
  filters: BiQueryFilter[]
  sorts: BiQuerySort[]
  limit: number
}

export interface BiCompiledQuery {
  sql: string
  parameters: unknown[]
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

export interface BiDatasourceHealth {
  sourceKey: string
  sourceType: string
  available: boolean
  message: string
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
}

export interface BiDashboardInteraction {
  interactionKey: string
  sourceWidgetKey: string
  targetWidgetKey?: string | null
  interactionType: string
  fieldKey: string
  target?: string | null
}

export interface BiDashboardPreset {
  dashboardKey: string
  title: string
  description: string
  datasetKey: string
  widgets: BiDashboardWidget[]
  filters: BiDashboardFilter[]
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

export interface BiEmbedTicketRequest {
  resourceType: string
  resourceKey: string
  scope: string
  filters: Record<string, string>
  ttlSeconds: number
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
  nonce: string
  issuedAt: string
  expiresAt: string
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
  createdBy?: string | null
  createdAt?: string | null
  updatedAt?: string | null
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
  createEmbedTicket: (request: BiEmbedTicketRequest) =>
    http.post<R<BiEmbedTicket>, R<BiEmbedTicket>>('/canvas/bi/embed-tickets', request),
  verifyEmbedTicket: (ticket: string) =>
    http.post<R<BiEmbedTicketPayload>, R<BiEmbedTicketPayload>>('/canvas/bi/embed-tickets/verify', { ticket }),
  compileQuery: (request: BiQueryRequest) =>
    http.post<R<BiCompiledQuery>, R<BiCompiledQuery>>('/canvas/bi/query/compile', request),
  executeQuery: (request: BiQueryRequest) =>
    http.post<R<BiQueryResult>, R<BiQueryResult>>('/canvas/bi/query/execute', request),
  listQueryHistory: (limit = 20) =>
    http.get<R<BiQueryHistoryItem[]>, R<BiQueryHistoryItem[]>>(`/canvas/bi/query/history?limit=${limit}`),
  listDatasourceHealth: () =>
    http.get<R<BiDatasourceHealth[]>, R<BiDatasourceHealth[]>>('/canvas/bi/datasources/health'),
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
  previewSelfService: (request: BiSelfServicePreviewRequest) =>
    http.post<R<BiQueryResult>, R<BiQueryResult>>('/canvas/bi/self-service/preview', request),
  createExport: (command: BiExportJobCommand) =>
    http.post<R<BiExportJobView>, R<BiExportJobView>>('/canvas/bi/self-service/exports', command),
  listExports: (limit = 20) =>
    http.get<R<BiExportJobView[]>, R<BiExportJobView[]>>(`/canvas/bi/self-service/exports?limit=${limit}`),
  reviewExport: (id: number, command: BiExportApprovalReviewCommand) =>
    http.post<R<BiExportJobView>, R<BiExportJobView>>(`/canvas/bi/self-service/exports/${id}/review`, command),
  cleanupExports: (limit = 100) =>
    http.post<R<BiExportCleanupResult>, R<BiExportCleanupResult>>(`/canvas/bi/self-service/exports/cleanup?limit=${limit}`, {}),
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
