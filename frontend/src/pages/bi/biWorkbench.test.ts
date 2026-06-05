import { describe, expect, test } from 'vitest'
import {
  BI_WORKBENCH_SECTIONS,
  DEFAULT_DASHBOARD_PRESETS,
  DEFAULT_MARKETING_DATASETS,
  QUICKBI_CHART_PALETTE,
  QUICKBI_CONTROL_PALETTE,
  QUICKBI_DESIGNER_ACTIONS,
  alignDashboardWidgets,
  buildDashboardCloneCommand,
  buildDashboardImportCommand,
  buildResourceMoveCommand,
  buildResourceFavoriteCommand,
  buildResourceCommentCommand,
  buildResourceLockCommand,
  buildExportApprovalReviewCommand,
  buildPublishApprovalRequestCommand,
  buildPublishApprovalReviewCommand,
  buildResourceTransferCommand,
  dashboardPackageFileName,
  dashboardLayoutColumns,
  dashboardResponsiveWidgets,
  buildEmbedTicketRequest,
  buildWidgetQueryRequest,
  canvasBiEntrypoint,
  chartLabel,
  controlLabel,
  dashboardWidgetGridPlacement,
  duplicateDashboardWidget,
  filterDesignerItems,
  getDashboardWidget,
  getDefaultDashboardPreset,
  getBiSection,
  interactionLabel,
  moveDashboardWidget,
  moveDashboardWidgetByPixels,
  parseDashboardPackageText,
  removeDashboardWidget,
  resizeDashboardWidget,
  resizeDashboardWidgetByPixels,
  resourceFolderLabel,
  resourceFavoriteLabel,
  resourceCommentScopeLabel,
  resourceLockLabel,
  resourceLockTokenFor,
  exportApprovalStatusLabel,
  publishApprovalStatusLabel,
  createDashboardPresetHistory,
  pushDashboardPresetHistory,
  redoDashboardPresetHistory,
  undoDashboardPresetHistory,
  toResourceLocationIndex,
  toResourceFavoriteIndex,
  toResourceLockIndex,
  resourceOwnerLabel,
  toResourceOwnershipIndex,
  serializeDashboardPackage,
  snapDashboardWidgetPlacement,
  toMarketingDatasetPreset,
} from './biWorkbench'

describe('biWorkbench', () => {
  test('exposes QuickBI-like workbench sections in product order', () => {
    expect(BI_WORKBENCH_SECTIONS.map(section => section.key)).toEqual([
      'overview',
      'data',
      'dataset',
      'chart',
      'dashboard',
      'portal',
      'self-service',
      'subscription',
      'embed',
      'ai',
    ])
  })

  test('marks Canvas marketing datasets as built-in presets', () => {
    expect(DEFAULT_MARKETING_DATASETS.map(dataset => dataset.key)).toContain('canvas_daily_stats')
    expect(DEFAULT_MARKETING_DATASETS.every(dataset => dataset.preset)).toBe(true)
  })

  test('returns stable Canvas BI entrypoint for embedded dashboard', () => {
    expect(canvasBiEntrypoint(12)).toEqual('/bi?dashboard=canvas-effect&canvasId=12')
  })

  test('finds section by key and falls back to overview', () => {
    expect(getBiSection('dashboard')?.label).toBe('仪表板')
    expect(getBiSection('missing')?.key).toBe('overview')
  })

  test('maps backend dataset metadata into marketing table presets', () => {
    expect(toMarketingDatasetPreset({
      datasetKey: 'canvas_daily_stats',
      metrics: [{ metricKey: 'total_executions' }, { metricKey: 'success_rate' }],
    })).toMatchObject({
      key: 'canvas_daily_stats',
      label: '画布每日统计',
      metrics: ['执行次数', '执行成功率'],
    })
  })

  test('exposes QuickBI-like designer actions and palettes', () => {
    expect(QUICKBI_DESIGNER_ACTIONS.map(action => action.key)).toEqual([
      'save',
      'undo',
      'redo',
      'preview',
      'publish',
      'clone',
      'export',
      'import',
      'subscribe',
      'embed',
      'archive',
    ])
    expect(QUICKBI_CHART_PALETTE.map(chart => chart.key)).toContain('FUNNEL')
    expect(QUICKBI_CONTROL_PALETTE.map(control => control.key)).toContain('TAB_CONTAINER')
  })

  test('ships a Canvas dashboard preset with interactions and embedding scopes', () => {
    const preset = getDefaultDashboardPreset()
    expect(DEFAULT_DASHBOARD_PRESETS.map(item => item.dashboardKey)).toContain('canvas-effect')
    expect(preset.widgets.map(widget => widget.chartType)).toContain('LINE')
    expect(preset.filters.map(filter => filter.controlType)).toContain('DATE_RANGE')
    expect(preset.interactions.map(interaction => interaction.interactionType)).toEqual([
      'FILTER_LINKAGE',
      'DRILL_DOWN',
      'HYPERLINK',
    ])
    expect(preset.embedScopes).toContain('INTERNAL_CANVAS')
  })

  test('selects dashboard widget and maps interaction labels', () => {
    const preset = getDefaultDashboardPreset()
    expect(getDashboardWidget(preset, 'rank-canvas').title).toBe('画布排行')
    expect(getDashboardWidget(preset, 'missing').widgetKey).toBe('kpi-total-executions')
    expect(chartLabel('KPI_CARD')).toBe('指标卡')
    expect(controlLabel('SEARCH_SELECT')).toBe('搜索选择')
    expect(interactionLabel('DRILL_DOWN')).toBe('钻取')
  })

  test('builds embed ticket request from dashboard and canvas context', () => {
    expect(buildEmbedTicketRequest(getDefaultDashboardPreset(), '12')).toEqual({
      resourceType: 'DASHBOARD',
      resourceKey: 'canvas-effect',
      scope: 'INTERNAL_CANVAS',
      filters: { canvasId: '12' },
      ttlSeconds: 600,
    })
    expect(buildEmbedTicketRequest(getDefaultDashboardPreset(), null, 'EXTERNAL_TICKET').ttlSeconds).toBe(900)
  })

  test('builds safe dashboard clone command', () => {
    expect(buildDashboardCloneCommand(getDefaultDashboardPreset(), 'copy 2026')).toEqual({
      dashboardKey: 'canvas-effect-copy-2026',
      title: '画布效果分析 副本',
      description: '面向营销画布执行、成功率、趋势和排行的预置分析看板。',
    })
  })

  test('builds safe dashboard import command from package', () => {
    const preset = getDefaultDashboardPreset()

    expect(buildDashboardImportCommand({ preset }, 'import 2026')).toEqual({
      packagePayload: { preset },
      dashboardKey: 'canvas-effect-import-2026',
      title: '画布效果分析 导入副本',
      overwrite: false,
    })
  })

  test('serializes dashboard resource package files with stable names', () => {
    const packagePayload = {
      resourceType: 'DASHBOARD',
      schemaVersion: 1,
      sourceVersion: 3,
      preset: getDefaultDashboardPreset(),
    }

    expect(dashboardPackageFileName(packagePayload)).toBe('canvas-effect-v3.bi-dashboard.json')
    expect(parseDashboardPackageText(serializeDashboardPackage(packagePayload))).toEqual(packagePayload)
  })

  test('rejects invalid dashboard resource package files', () => {
    expect(() => parseDashboardPackageText('{"resourceType":"CHART"}')).toThrow('dashboard package preset is required')
    expect(() => parseDashboardPackageText(JSON.stringify({
      resourceType: 'CHART',
      preset: getDefaultDashboardPreset(),
    }))).toThrow('unsupported dashboard package resource type')
    expect(() => parseDashboardPackageText('not-json')).toThrow('dashboard package file is not valid JSON')
  })

  test('builds executable widget query from dashboard preset', () => {
    const preset = getDefaultDashboardPreset()
    const widget = getDashboardWidget(preset, 'trend-executions')

    expect(buildWidgetQueryRequest(preset, widget, '12')).toEqual({
      datasetKey: 'canvas_daily_stats',
      dimensions: ['stat_date'],
      metrics: ['total_executions', 'success_count', 'fail_count'],
      filters: [{ field: 'canvas_id', operator: 'EQ', value: 12 }],
      sorts: [{ field: 'stat_date', direction: 'ASC' }],
      limit: 500,
    })
  })

  test('duplicates dashboard widgets with stable unique layout metadata', () => {
    const preset = getDefaultDashboardPreset()
    const copied = duplicateDashboardWidget(preset, 'rank-canvas')

    expect(copied.widgets).toHaveLength(preset.widgets.length + 1)
    expect(copied.widgets[copied.widgets.length - 1]).toMatchObject({
      widgetKey: 'rank-canvas-copy',
      title: '画布排行 副本',
      chartType: 'BAR',
      gridX: 12,
      gridY: 4,
    })
  })

  test('removes dashboard widgets and dependent interactions', () => {
    const preset = getDefaultDashboardPreset()
    const removed = removeDashboardWidget(preset, 'rank-canvas')

    expect(removed.widgets.map(widget => widget.widgetKey)).not.toContain('rank-canvas')
    expect(removed.interactions.some(interaction =>
      interaction.sourceWidgetKey === 'rank-canvas' || interaction.targetWidgetKey === 'rank-canvas')).toBe(false)
  })

  test('keeps at least one widget when deleting from a dashboard', () => {
    const preset = { ...getDefaultDashboardPreset(), widgets: [getDashboardWidget(getDefaultDashboardPreset(), 'rank-canvas')], interactions: [] }

    expect(removeDashboardWidget(preset, 'rank-canvas')).toBe(preset)
  })

  test('moves dashboard widgets within the 20-column canvas', () => {
    const preset = getDefaultDashboardPreset()
    const blockedLeft = moveDashboardWidget(preset, 'rank-canvas', 'left')
    const movedDown = moveDashboardWidget(preset, 'detail-canvas', 'down')
    const blockedUp = moveDashboardWidget(preset, 'trend-executions', 'up')
    const movedByDrag = moveDashboardWidgetByPixels(preset, 'detail-canvas', 0, 90)
    const clampedByDrag = moveDashboardWidgetByPixels(preset, 'trend-executions', -1000, -1000)

    expect(getDashboardWidget(blockedLeft, 'rank-canvas')).toMatchObject({ gridX: 12, gridY: 3 })
    expect(getDashboardWidget(movedDown, 'detail-canvas').gridY).toBe(10)
    expect(getDashboardWidget(blockedUp, 'trend-executions').gridY).toBe(3)
    expect(getDashboardWidget(movedByDrag, 'detail-canvas')).toMatchObject({ gridX: 0, gridY: 11 })
    expect(getDashboardWidget(clampedByDrag, 'trend-executions')).toMatchObject({ gridX: 0, gridY: 3 })
  })

  test('maps widget grid metadata to explicit CSS grid placement', () => {
    const preset = getDefaultDashboardPreset()

    expect(dashboardWidgetGridPlacement(getDashboardWidget(preset, 'rank-canvas'))).toEqual({
      gridColumn: '13 / span 8',
      gridRow: '4 / span 6',
      minHeight: 252,
    })
  })

  test('maps dashboard widgets into desktop tablet and mobile layouts', () => {
    const preset = getDefaultDashboardPreset()
    const desktop = dashboardResponsiveWidgets(preset, 'desktop')
    const tablet = dashboardResponsiveWidgets(preset, 'tablet')
    const mobile = dashboardResponsiveWidgets(preset, 'mobile')

    expect(dashboardLayoutColumns('desktop')).toBe(20)
    expect(dashboardLayoutColumns('tablet')).toBe(12)
    expect(dashboardLayoutColumns('mobile')).toBe(1)
    expect(getDashboardWidget({ ...preset, widgets: desktop }, 'trend-executions')).toMatchObject({ gridX: 0, gridW: 12 })
    expect(tablet.every(widget => widget.gridX >= 0 && widget.gridX + widget.gridW <= 12)).toBe(true)
    expect(getDashboardWidget({ ...preset, widgets: tablet }, 'rank-canvas')).toMatchObject({ gridX: 7, gridW: 5 })
    expect(mobile.map(widget => widget.widgetKey)).toEqual([
      'kpi-total-executions',
      'kpi-success-rate',
      'trend-executions',
      'rank-canvas',
      'detail-canvas',
    ])
    expect(mobile).toEqual(mobile.map((widget, index) => ({
      ...widget,
      gridX: 0,
      gridY: index * 4,
      gridW: 1,
      gridH: 4,
    })))
  })

  test('resizes dashboard widgets within grid bounds', () => {
    const preset = getDefaultDashboardPreset()
    const wider = resizeDashboardWidget(preset, 'trend-executions', 8, 2)
    const clamped = resizeDashboardWidget(wider, 'trend-executions', 100, -100)
    const fromPixels = resizeDashboardWidgetByPixels(preset, 'rank-canvas', -130, 90)

    expect(getDashboardWidget(wider, 'trend-executions')).toMatchObject({ gridW: 20, gridH: 8 })
    expect(getDashboardWidget(wider, 'rank-canvas').gridY).toBe(11)
    expect(getDashboardWidget(wider, 'detail-canvas').gridY).toBe(17)
    expect(getDashboardWidget(clamped, 'trend-executions')).toMatchObject({ gridW: 20, gridH: 2 })
    expect(getDashboardWidget(fromPixels, 'rank-canvas')).toMatchObject({ gridW: 6, gridH: 8 })
    expect(getDashboardWidget(fromPixels, 'detail-canvas').gridY).toBe(11)
  })

  test('aligns multiple dashboard widgets on shared grid edges and centers', () => {
    const preset = getDefaultDashboardPreset()
    const alignedLeft = alignDashboardWidgets(preset, ['rank-canvas', 'detail-canvas'], 'left')
    const alignedBottom = alignDashboardWidgets(preset, ['kpi-total-executions', 'trend-executions'], 'bottom')
    const alignedCenter = alignDashboardWidgets(preset, ['kpi-total-executions', 'detail-canvas'], 'center')
    const ignoredInvalidSelection = alignDashboardWidgets(preset, ['missing', 'rank-canvas'], 'left')

    expect(getDashboardWidget(alignedLeft, 'rank-canvas').gridX).toBe(0)
    expect(getDashboardWidget(alignedLeft, 'detail-canvas').gridX).toBe(0)
    expect(getDashboardWidget(alignedBottom, 'kpi-total-executions').gridY).toBe(6)
    expect(getDashboardWidget(alignedBottom, 'trend-executions').gridY).toBe(3)
    expect(getDashboardWidget(alignedCenter, 'kpi-total-executions').gridX).toBe(7)
    expect(getDashboardWidget(alignedCenter, 'detail-canvas').gridX).toBe(0)
    expect(ignoredInvalidSelection).toBe(preset)
  })

  test('snaps dashboard widget placement to nearby widget guide lines', () => {
    const preset = getDefaultDashboardPreset()

    const result = snapDashboardWidgetPlacement(preset, 'rank-canvas', {
      gridX: 11,
      gridY: 2,
      gridW: 8,
      gridH: 6,
    })

    expect(result.placement).toMatchObject({ gridX: 12, gridY: 3, gridW: 8, gridH: 6 })
    expect(result.guides).toEqual([
      { orientation: 'vertical', position: 12, widgetKey: 'trend-executions', edge: 'right' },
      { orientation: 'horizontal', position: 9, widgetKey: 'detail-canvas', edge: 'top' },
    ])
  })

  test('tracks dashboard layout history for undo and redo', () => {
    const preset = getDefaultDashboardPreset()
    const moved = moveDashboardWidget(preset, 'detail-canvas', 'down')
    const history = pushDashboardPresetHistory(createDashboardPresetHistory(preset), moved)
    const undone = undoDashboardPresetHistory(history)
    const redone = redoDashboardPresetHistory(undone)

    expect(getDashboardWidget(history.present, 'detail-canvas').gridY).toBe(10)
    expect(undone.present).toBe(preset)
    expect(redone.present).toBe(moved)
  })

  test('filters designer assets by key label group and dataset metadata', () => {
    expect(filterDesignerItems(QUICKBI_CHART_PALETTE, '漏斗').map(item => item.key)).toEqual(['FUNNEL'])
    expect(filterDesignerItems([
      { key: 'a', name: '渠道图表', datasetKey: 'channel_performance' },
      { key: 'b', name: '画布图表', datasetKey: 'canvas_daily_stats' },
    ], 'canvas')).toEqual([{ key: 'b', name: '画布图表', datasetKey: 'canvas_daily_stats' }])
  })

  test('indexes resource locations by type and key with root fallback', () => {
    const index = toResourceLocationIndex([
      {
        id: 1,
        tenantId: 1,
        workspaceId: 2,
        resourceType: 'DASHBOARD',
        resourceKey: 'canvas-effect',
        folderKey: 'marketing',
        sortOrder: 10,
        movedBy: 'analyst',
        movedAt: '2026-06-05T09:00:00',
      },
    ])

    expect(index['DASHBOARD/canvas-effect']).toMatchObject({
      folderKey: 'marketing',
      sortOrder: 10,
    })
    expect(resourceFolderLabel(index['DASHBOARD/canvas-effect'])).toBe('marketing')
    expect(resourceFolderLabel(undefined)).toBe('根目录')
  })

  test('builds normalized resource move commands', () => {
    expect(buildResourceMoveCommand('dashboard', 'canvas-effect', ' Marketing Reports ', 12)).toEqual({
      resourceType: 'DASHBOARD',
      resourceKey: 'canvas-effect',
      folderKey: 'marketing-reports',
      sortOrder: 12,
    })
    expect(buildResourceMoveCommand('dataset', 'canvas_daily_stats', '', null)).toEqual({
      resourceType: 'DATASET',
      resourceKey: 'canvas_daily_stats',
      folderKey: null,
      sortOrder: 0,
    })
  })

  test('indexes ownerships and builds normalized transfer commands', () => {
    const index = toResourceOwnershipIndex([
      {
        id: 1,
        tenantId: 1,
        workspaceId: 2,
        resourceType: 'dashboard',
        resourceKey: 'canvas-effect',
        ownerUser: 'alice',
        transferredBy: 'system',
        transferredAt: '2026-06-05T11:30:00',
      },
    ])

    expect(index['DASHBOARD/canvas-effect']).toMatchObject({
      ownerUser: 'alice',
      transferredBy: 'system',
    })
    expect(resourceOwnerLabel(index['DASHBOARD/canvas-effect'])).toBe('alice')
    expect(resourceOwnerLabel(undefined)).toBe('未分配')
    expect(buildResourceTransferCommand('chart', ' weekly-trend ', ' bob@example.com ')).toEqual({
      resourceType: 'CHART',
      resourceKey: 'weekly-trend',
      ownerUser: 'bob@example.com',
    })
  })

  test('indexes favorites and builds normalized favorite commands', () => {
    const index = toResourceFavoriteIndex([
      {
        id: 1,
        tenantId: 1,
        workspaceId: 2,
        resourceType: 'dashboard',
        resourceKey: 'canvas-effect',
        username: 'alice',
        favorite: true,
        createdAt: '2026-06-05T12:00:00',
      },
    ])

    expect(index['DASHBOARD/canvas-effect']).toMatchObject({
      username: 'alice',
      favorite: true,
    })
    expect(resourceFavoriteLabel(index['DASHBOARD/canvas-effect'])).toBe('已收藏')
    expect(resourceFavoriteLabel(undefined)).toBe('未收藏')
    expect(buildResourceFavoriteCommand('portal', ' executive-home ', false)).toEqual({
      resourceType: 'PORTAL',
      resourceKey: 'executive-home',
      favorite: false,
    })
  })

  test('builds resource comment commands and labels widget comments', () => {
    expect(buildResourceCommentCommand('dashboard', ' canvas-effect ', ' kpi-total ', '  已确认口径  ')).toEqual({
      resourceType: 'DASHBOARD',
      resourceKey: 'canvas-effect',
      widgetKey: 'kpi-total',
      commentText: '已确认口径',
    })
    expect(buildResourceCommentCommand('dataset', 'canvas_daily_stats', '', '字段已复核')).toEqual({
      resourceType: 'DATASET',
      resourceKey: 'canvas_daily_stats',
      widgetKey: null,
      commentText: '字段已复核',
    })
    expect(resourceCommentScopeLabel({ resourceType: 'DASHBOARD', resourceKey: 'canvas-effect', widgetKey: 'kpi-total', commentText: 'ok' })).toBe('组件 kpi-total')
    expect(resourceCommentScopeLabel({ resourceType: 'DASHBOARD', resourceKey: 'canvas-effect', widgetKey: null, commentText: 'ok' })).toBe('资源')
  })

  test('indexes resource locks and builds normalized lock commands', () => {
    const index = toResourceLockIndex([
      {
        id: 3,
        tenantId: 1,
        workspaceId: 2,
        resourceType: 'dashboard',
        resourceKey: 'canvas-effect',
        lockToken: 'token-1',
        lockedBy: 'alice',
        lockedAt: '2026-06-05T12:00:00',
        expiresAt: '2026-06-05T12:05:00',
        locked: true,
      },
    ])

    expect(index['DASHBOARD/canvas-effect']).toMatchObject({
      lockToken: 'token-1',
      lockedBy: 'alice',
    })
    expect(resourceLockLabel(index['DASHBOARD/canvas-effect'])).toBe('alice 编辑中')
    expect(resourceLockLabel({ resourceType: 'DASHBOARD', resourceKey: 'canvas-effect', locked: false })).toBe('未锁定')
    expect(resourceLockTokenFor(index['DASHBOARD/canvas-effect'], 'fallback-token', 'dashboard', 'canvas-effect')).toBe('token-1')
    expect(resourceLockTokenFor(index['DASHBOARD/canvas-effect'], 'fallback-token', 'chart', 'trend-executions')).toBeNull()
    expect(buildResourceLockCommand('dashboard', ' canvas-effect ', ' token-1 ', 120)).toEqual({
      resourceType: 'DASHBOARD',
      resourceKey: 'canvas-effect',
      lockToken: 'token-1',
      ttlSeconds: 120,
    })
  })

  test('builds publish approval commands and maps review status labels', () => {
    expect(buildPublishApprovalRequestCommand('dashboard', ' canvas-effect ', '  发布到运营门户  ')).toEqual({
      resourceType: 'DASHBOARD',
      resourceKey: 'canvas-effect',
      reason: '发布到运营门户',
    })
    expect(buildPublishApprovalReviewCommand('approved', ' 已复核 ', 9)).toEqual({
      approvalId: 9,
      status: 'APPROVED',
      reviewComment: '已复核',
    })
    expect(publishApprovalStatusLabel('pending')).toBe('待审批')
    expect(publishApprovalStatusLabel('APPROVED')).toBe('已通过')
    expect(publishApprovalStatusLabel('rejected')).toBe('已驳回')
    expect(buildExportApprovalReviewCommand('approved', ' 同意导出 ')).toEqual({
      status: 'APPROVED',
      reviewComment: '同意导出',
    })
    expect(exportApprovalStatusLabel('PENDING')).toBe('待审批')
    expect(exportApprovalStatusLabel('APPROVED')).toBe('已通过')
    expect(exportApprovalStatusLabel('REJECTED')).toBe('已驳回')
  })
})
