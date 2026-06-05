import type { BiQueryRequest } from '../../services/biApi'

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
}

export interface BiDashboardInteractionPreset {
  interactionKey: string
  sourceWidgetKey: string
  targetWidgetKey?: string | null
  interactionType: string
  fieldKey: string
  target?: string | null
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

export interface BiExportApprovalReviewCommandLike {
  status: 'APPROVED' | 'REJECTED'
  reviewComment: string | null
}

export type DashboardWidgetMoveDirection = 'up' | 'down' | 'left' | 'right'
export type DashboardWidgetAlignment = 'left' | 'right' | 'top' | 'bottom' | 'center' | 'middle'
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
      { filterKey: 'filter-canvas', fieldKey: 'canvas_name', label: '画布名称', controlType: 'SEARCH_SELECT', required: false },
      { filterKey: 'filter-trigger-type', fieldKey: 'trigger_type', label: '触发方式', controlType: 'ENUM_MULTI_SELECT', required: false },
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
): {
  resourceType: string
  resourceKey: string
  scope: string
  filters: Record<string, string>
  ttlSeconds: number
} {
  return {
    resourceType: 'DASHBOARD',
    resourceKey: preset.dashboardKey,
    scope,
    filters: canvasId ? { canvasId } : {},
    ttlSeconds: scope === 'EXTERNAL_TICKET' ? 900 : 600,
  }
}

export function buildWidgetQueryRequest(
  preset: BiDashboardPresetLike,
  widget: BiDashboardWidgetPreset,
  canvasId?: string | null,
  limit = 500,
): BiQueryRequest {
  const filters = canvasId
    ? [{
        field: 'canvas_id',
        operator: 'EQ' as const,
        value: Number.isNaN(Number(canvasId)) ? canvasId : Number(canvasId),
      }]
    : []
  return {
    datasetKey: preset.datasetKey,
    dimensions: widget.dimensions,
    metrics: widget.metrics,
    filters,
    sorts: widget.dimensions.includes('stat_date')
      ? [{ field: 'stat_date', direction: 'ASC' as const }]
      : [],
    limit,
  }
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

function normalizeResourceKey(value: string): string {
  const normalized = value
    .trim()
    .replace(/[^A-Za-z0-9_-]+/g, '-')
    .replace(/^-+/, '')
    .replace(/-+$/, '')
  return normalized || 'copy'
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
