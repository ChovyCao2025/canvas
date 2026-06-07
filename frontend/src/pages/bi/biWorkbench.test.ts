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
  buildDatasourceMultiTableDatasetCommand,
  buildDatasourceOnboardingCommand,
  buildSqlDatasetDraftResource,
  buildSqlDatasetParameterDrafts,
  buildDatasourceTableDatasetCommand,
  buildResourceMoveCommand,
  movePortalMenuItem,
  updatePortalNavigationConfig,
  buildResourceFavoriteCommand,
  buildResourceCommentCommand,
  buildResourceLockCommand,
  buildExportApprovalReviewCommand,
  buildPermissionRequestCommand,
  buildPermissionRequestReviewCommand,
  buildPublishApprovalRequestCommand,
  buildPublishApprovalReviewCommand,
  buildResourceTransferCommand,
  buildSelfServiceExtractionQuery,
  buildDashboardControlOptionQuery,
  buildDashboardInteractionTarget,
  dashboardPackageFileName,
  dashboardDefaultRuntimeParameters,
  dashboardRuntimeFilterLocked,
  dashboardRuntimeStateRows,
  dashboardRuntimeParametersFromEmbedPayload,
  dashboardRuntimeParametersFromSearchParams,
  dashboardRuntimeSearchParamKeys,
  dashboardRuntimeControlValue,
  dashboardDesignerKeyboardActionFromEventLike,
  stripDashboardRuntimeSearchParams,
  stripDashboardRuntimeSearchParam,
  dashboardLayoutColumns,
  dashboardResponsiveWidgets,
  resolveDashboardRuntimeParameters,
  updateDashboardRuntimeParameters,
  dropSelfServiceExtractionField,
  buildEmbedTicketRequest,
  buildBiPermissionResourceTargets,
  buildBiResourceTargets,
  buildBigScreenResourceOptions,
  buildBigScreenDraftResource,
  addBigScreenLibraryComponent,
  alignBigScreenLayoutItems,
  bigScreenResourceSummaryRows,
  moveBigScreenLayoutItem,
  resizeBigScreenLayoutItem,
  updateBigScreenLayoutItem,
  snapBigScreenLayoutItem,
  updateBigScreenMobileLayout,
  upsertBigScreenResource,
  biRuntimeRouteFromSearchParams,
  selectBigScreenRuntimeResource,
  buildSpreadsheetResourceOptions,
  buildSpreadsheetDraftResource,
  buildSpreadsheetPivotTable,
  evaluateSpreadsheetCells,
  spreadsheetResourceSummaryRows,
  updateSpreadsheetCellStyle,
  updateSpreadsheetCellRange,
  updateSpreadsheetCell,
  upsertSpreadsheetResource,
  selectSpreadsheetRuntimeResource,
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
  exportAuditDetailRows,
  isCancelableExportJob,
  publishApprovalStatusLabel,
  datasourceHealthHistoryRows,
  datasourceHealthSloRows,
  datasourceConnectorRows,
  datasourceConnectionTestRows,
  datasourceOnboardingRows,
  datasourceSchemaPreviewRows,
  datasourceSchemaSnapshotHistoryRows,
  datasourceSchemaSnapshotRows,
  datasetAccelerationPolicyRows,
  datasetAccelerationSchedulerRows,
  buildQuickEngineCapacityAlertPolicyCommand,
  buildQuickEngineTenantPoolPolicyCommand,
  permissionAuditRows,
  quickEngineCapacityDetailRows,
  quickEngineConcurrencyQueueRows,
  quickEngineCapacitySummaryRows,
  quickEngineCapacityUserRows,
  queryCacheInvalidationActionRows,
  queryCachePolicyRows,
  queryCacheStatsRows,
  buildQueryCachePolicyCommand,
  queryCancellationStatusLabel,
  queryExecutionPlanRows,
  queryGovernanceAuditRows,
  queryGovernancePolicyRows,
  queryGovernanceSummaryRows,
  queryHistoryDetailRows,
  removeSelfServiceExtractionField,
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

  test('maps dashboard designer keyboard shortcuts to widget actions', () => {
    expect(dashboardDesignerKeyboardActionFromEventLike({ key: 'ArrowLeft' })).toEqual({ type: 'move', direction: 'left' })
    expect(dashboardDesignerKeyboardActionFromEventLike({ key: 'ArrowDown', shiftKey: true })).toEqual({ type: 'resize', direction: 'down' })
    expect(dashboardDesignerKeyboardActionFromEventLike({ key: 'Delete' })).toEqual({ type: 'remove' })
    expect(dashboardDesignerKeyboardActionFromEventLike({ key: 'Backspace' })).toEqual({ type: 'remove' })
    expect(dashboardDesignerKeyboardActionFromEventLike({ key: 'd', ctrlKey: true })).toEqual({ type: 'duplicate' })
    expect(dashboardDesignerKeyboardActionFromEventLike({ key: 'D', metaKey: true })).toEqual({ type: 'duplicate' })
    expect(dashboardDesignerKeyboardActionFromEventLike({ key: 'z', metaKey: true })).toEqual({ type: 'undo' })
    expect(dashboardDesignerKeyboardActionFromEventLike({ key: 'z', metaKey: true, shiftKey: true })).toEqual({ type: 'redo' })
    expect(dashboardDesignerKeyboardActionFromEventLike({ key: 'y', ctrlKey: true })).toEqual({ type: 'redo' })
    expect(dashboardDesignerKeyboardActionFromEventLike({ key: 'a' })).toBeNull()
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
      maxAccessCount: getDefaultDashboardPreset().widgets.length + 3,
    })
    expect(buildEmbedTicketRequest(getDefaultDashboardPreset(), null, 'EXTERNAL_TICKET').ttlSeconds).toBe(900)
  })

  test('binds dashboard runtime parameters into embed ticket filters', () => {
    expect(buildEmbedTicketRequest(
      getDefaultDashboardPreset(),
      '12',
      'EXTERNAL_TICKET',
      {
        'filter-stat-date': ['2026-06-01', '2026-06-06'],
        'filter-trigger-type': 'TIME,MQ',
        empty: null,
      },
    )).toEqual({
      resourceType: 'DASHBOARD',
      resourceKey: 'canvas-effect',
      scope: 'EXTERNAL_TICKET',
      filters: {
        canvasId: '12',
        'filter-stat-date': '2026-06-01,2026-06-06',
        'filter-trigger-type': 'TIME,MQ',
      },
      ttlSeconds: 900,
      maxAccessCount: getDefaultDashboardPreset().widgets.length + 3,
    })
  })

  test('maps QuickBI-style global parameters into dashboard widget filters and embed claims', () => {
    const preset = {
      ...getDefaultDashboardPreset(),
      globalParameters: [
        {
          parameterKey: 'gpCanvas',
          fieldKey: 'canvas_name',
          filterKey: 'filter-canvas',
          aliases: ['canvasGlobal'],
          defaultValue: 'Default Canvas',
          locked: true,
        },
        {
          parameterKey: 'gpTriggerType',
          fieldKey: 'trigger_type',
          filterKey: 'filter-trigger-type',
          aliases: ['triggerGlobal'],
        },
      ],
    }
    const runtimeParameters = resolveDashboardRuntimeParameters(
      preset,
      new URLSearchParams('canvasGlobal=Welcome%20Journey&triggerGlobal=TIME,MQ'),
      { gpCanvas: 'Remembered Canvas' },
      new Date('2026-06-06T08:00:00Z'),
    )

    expect(runtimeParameters).toEqual(expect.objectContaining({
      'filter-canvas': 'Welcome Journey',
      gpCanvas: 'Welcome Journey',
      'filter-trigger-type': 'TIME,MQ',
      gpTriggerType: 'TIME,MQ',
    }))
    expect(buildWidgetQueryRequest(
      preset,
      getDashboardWidget(preset, 'detail-canvas'),
      '12',
      runtimeParameters,
    ).filters).toEqual(expect.arrayContaining([
      { field: 'canvas_name', operator: 'EQ', value: 'Welcome Journey' },
      { field: 'trigger_type', operator: 'IN', value: ['TIME', 'MQ'] },
    ]))
    expect(updateDashboardRuntimeParameters(preset, runtimeParameters, 'filter-canvas', 'Edited Canvas')).toEqual(expect.objectContaining({
      'filter-canvas': 'Welcome Journey',
      gpCanvas: 'Welcome Journey',
    }))
    expect(buildEmbedTicketRequest(preset, '12', 'EXTERNAL_TICKET', runtimeParameters)).toEqual(expect.objectContaining({
      filters: expect.objectContaining({
        'filter-canvas': 'Welcome Journey',
        'filter-trigger-type': 'TIME,MQ',
      }),
      parameters: expect.objectContaining({
        gpCanvas: 'Welcome Journey',
        gpTriggerType: 'TIME,MQ',
      }),
    }))
  })

  test('reuses embed ticket payload as normalized dashboard runtime parameters', () => {
    const preset = {
      ...getDefaultDashboardPreset(),
      globalParameters: [
        {
          parameterKey: 'gpCanvas',
          fieldKey: 'canvas_name',
          filterKey: 'filter-canvas',
          aliases: ['campaign'],
          locked: true,
        },
      ],
    }

    const runtimeParameters = dashboardRuntimeParametersFromEmbedPayload(
      preset,
      {
        tenantId: 7,
        username: 'viewer',
        resourceType: 'DASHBOARD',
        resourceKey: 'canvas-effect',
        scope: 'EXTERNAL_TICKET',
        filters: {
          canvasId: '12',
          'filter-stat-date': '2026-06-01,2026-06-06',
          'filter-trigger-type': 'TIME,MQ',
        },
        parameters: {
          gpCanvas: 'Welcome Journey',
        },
        allowedDomains: ['analytics.example.com'],
        nonce: 'nonce-1',
        issuedAt: '2026-06-06T08:00:00Z',
        expiresAt: '2026-06-06T08:15:00Z',
      },
      new Date('2026-06-06T08:00:00Z'),
    )

    expect(runtimeParameters).toEqual(expect.objectContaining({
      'filter-stat-date': '2026-06-01,2026-06-06',
      'filter-trigger-type': 'TIME,MQ',
      'filter-canvas': 'Welcome Journey',
      gpCanvas: 'Welcome Journey',
    }))
    expect(dashboardRuntimeFilterLocked(preset, preset.filters[1])).toBe(true)
    expect(buildWidgetQueryRequest(
      preset,
      getDashboardWidget(preset, 'detail-canvas'),
      '12',
      runtimeParameters,
    ).filters).toEqual(expect.arrayContaining([
      { field: 'canvas_id', operator: 'EQ', value: 12 },
      { field: 'trigger_type', operator: 'IN', value: ['TIME', 'MQ'] },
      { field: 'canvas_name', operator: 'EQ', value: 'Welcome Journey' },
    ]))
  })

  test('resets dashboard runtime parameters without dropping dashboard context', () => {
    const preset = getDefaultDashboardPreset()
    const stripped = stripDashboardRuntimeSearchParams(
      preset,
      new URLSearchParams('dashboard=canvas-effect&canvasId=12&filter-canvas=Welcome&stat_date=2026-06-01&unused=keep'),
    )
    const defaultParameters = dashboardDefaultRuntimeParameters(preset, new Date('2026-06-06T08:00:00Z'))

    expect(dashboardRuntimeSearchParamKeys(preset)).toEqual(expect.arrayContaining([
      'filter-stat-date',
      'stat_date',
      'filter-canvas',
      'canvas_name',
      'filter-trigger-type',
      'trigger_type',
    ]))
    expect(stripped.toString()).toBe('dashboard=canvas-effect&canvasId=12&unused=keep')
    expect(defaultParameters).toEqual({
      'filter-stat-date': ['2026-05-31', '2026-06-06'],
    })
  })

  test('strips a single dashboard runtime parameter without dropping other controls', () => {
    const preset = getDefaultDashboardPreset()
    const stripped = stripDashboardRuntimeSearchParam(
      preset,
      new URLSearchParams('dashboard=canvas-effect&canvasId=12&filter-canvas=Welcome&canvas_name=Alias&filter-trigger-type=TIME,MQ&unused=keep'),
      'filter-canvas',
    )

    expect(stripped.toString()).toBe('dashboard=canvas-effect&canvasId=12&filter-trigger-type=TIME%2CMQ&unused=keep')
  })

  test('propagates row and runtime parameters into dashboard drill targets', () => {
    const preset = getDefaultDashboardPreset()
    const interaction = preset.interactions.find(item => item.interactionKey === 'drill-rank-canvas')

    expect(buildDashboardInteractionTarget(
      preset,
      interaction!,
      { canvas_name: 'Welcome Journey' },
      {
        canvasId: '12',
        runtimeParameters: {
          'filter-stat-date': ['2026-06-01', '2026-06-06'],
          'filter-trigger-type': 'TIME,MQ',
        },
      },
    )).toBe('/bi?dashboard=canvas-effect&canvasId=12&widget=detail-canvas&filter-stat-date=2026-06-01%2C2026-06-06&filter-trigger-type=TIME%2CMQ&filter-canvas=Welcome+Journey')
  })

  test('propagates runtime parameters through hyperlink interaction templates', () => {
    const preset = getDefaultDashboardPreset()
    const interaction = preset.interactions.find(item => item.interactionKey === 'open-canvas-stats')

    expect(buildDashboardInteractionTarget(
      preset,
      interaction!,
      { canvas_id: 12 },
      {
        canvasId: '12',
        runtimeParameters: {
          'filter-stat-date': ['2026-06-01', '2026-06-06'],
          'filter-canvas': 'Welcome Journey',
          empty: null,
        },
      },
    )).toBe('/canvas/12/stats?canvasId=12&filter-stat-date=2026-06-01%2C2026-06-06&filter-canvas=Welcome+Journey')
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
      dashboardKey: 'canvas-effect',
      datasetKey: 'canvas_daily_stats',
      dimensions: ['stat_date'],
      metrics: ['total_executions', 'success_count', 'fail_count'],
      filters: [{ field: 'canvas_id', operator: 'EQ', value: 12 }],
      sorts: [{ field: 'stat_date', direction: 'ASC' }],
      limit: 500,
    })
  })

  test('binds dashboard runtime parameters into widget query filters', () => {
    const preset = getDefaultDashboardPreset()
    const widget = getDashboardWidget(preset, 'trend-executions')

    expect(buildWidgetQueryRequest(preset, widget, '12', {
      'filter-stat-date': '2026-06-01,2026-06-05',
      canvas_name: 'Welcome Journey',
      'filter-trigger-type': 'TIME,MQ',
    })).toEqual({
      dashboardKey: 'canvas-effect',
      datasetKey: 'canvas_daily_stats',
      dimensions: ['stat_date'],
      metrics: ['total_executions', 'success_count', 'fail_count'],
      filters: [
        { field: 'canvas_id', operator: 'EQ', value: 12 },
        { field: 'stat_date', operator: 'BETWEEN', value: ['2026-06-01', '2026-06-05'] },
        { field: 'canvas_name', operator: 'EQ', value: 'Welcome Journey' },
        { field: 'trigger_type', operator: 'IN', value: ['TIME', 'MQ'] },
      ],
      sorts: [{ field: 'stat_date', direction: 'ASC' }],
      limit: 500,
    })
  })

  test('extracts dashboard runtime parameters from URL search params', () => {
    const params = new URLSearchParams('dashboard=canvas-effect&canvasId=12&filter-trigger-type=TIME,MQ&canvas_name=Welcome')

    expect(dashboardRuntimeParametersFromSearchParams(getDefaultDashboardPreset(), params)).toEqual({
      'filter-trigger-type': 'TIME,MQ',
      canvas_name: 'Welcome',
    })
  })

  test('resolves dashboard runtime parameters from URL remembered state and defaults', () => {
    const preset = getDefaultDashboardPreset()

    expect(resolveDashboardRuntimeParameters(
      preset,
      new URLSearchParams('filter-trigger-type=TIME,MQ'),
      { 'filter-canvas': 'Remembered Canvas' },
      new Date('2026-06-06T08:00:00Z'),
    )).toEqual({
      'filter-stat-date': ['2026-05-31', '2026-06-06'],
      'filter-canvas': 'Remembered Canvas',
      'filter-trigger-type': 'TIME,MQ',
    })
  })

  test('summarizes dashboard runtime parameter sources for the state editor', () => {
    const preset = getDefaultDashboardPreset()
    const rows = dashboardRuntimeStateRows(
      preset,
      new URLSearchParams('filter-trigger-type=TIME,MQ&canvas_name=Url Canvas'),
      { 'filter-canvas': 'Remembered Canvas' },
      ['filter-trigger-type'],
      new Date('2026-06-06T08:00:00Z'),
    )

    expect(rows).toContainEqual(expect.objectContaining({
      key: 'filter-stat-date',
      label: '统计日期',
      source: 'DEFAULT',
      sourceLabel: '默认值',
      valueText: '2026-05-31,2026-06-06',
    }))
    expect(rows).toContainEqual(expect.objectContaining({
      key: 'filter-canvas',
      label: '画布名称',
      source: 'URL',
      sourceLabel: 'URL覆盖',
      valueText: 'Url Canvas',
    }))
    expect(rows).toContainEqual(expect.objectContaining({
      key: 'filter-trigger-type',
      label: '触发方式',
      source: 'CLEARED',
      sourceLabel: '已清除',
      valueText: '',
    }))
  })

  test('updates editable dashboard runtime control values by control type', () => {
    const preset = getDefaultDashboardPreset()
    const statDateFilter = preset.filters.find(filter => filter.filterKey === 'filter-stat-date')!
    const triggerTypeFilter = preset.filters.find(filter => filter.filterKey === 'filter-trigger-type')!

    const withDateRange = updateDashboardRuntimeParameters(
      preset,
      {},
      'filter-stat-date',
      '2026-06-01, 2026-06-06',
    )
    const withTriggerTypes = updateDashboardRuntimeParameters(
      preset,
      withDateRange,
      'filter-trigger-type',
      'TIME, MQ',
    )

    expect(withTriggerTypes).toEqual({
      'filter-stat-date': ['2026-06-01', '2026-06-06'],
      'filter-trigger-type': ['TIME', 'MQ'],
    })
    expect(dashboardRuntimeControlValue(withTriggerTypes, statDateFilter)).toBe('2026-06-01,2026-06-06')
    expect(dashboardRuntimeControlValue(withTriggerTypes, triggerTypeFilter)).toBe('TIME,MQ')
    expect(updateDashboardRuntimeParameters(preset, withTriggerTypes, 'filter-trigger-type', '')).toEqual({
      'filter-stat-date': ['2026-06-01', '2026-06-06'],
    })
  })

  test('applies scoped dashboard controls only to target widgets', () => {
    const preset = getDefaultDashboardPreset()
    const scopedPreset = {
      ...preset,
      filters: preset.filters.map(filter => filter.filterKey === 'filter-trigger-type'
        ? { ...filter, targetWidgetKeys: ['detail-canvas'] }
        : filter),
    }
    const runtimeParameters = {
      'filter-trigger-type': 'TIME,MQ',
    }

    expect(buildWidgetQueryRequest(
      scopedPreset,
      getDashboardWidget(scopedPreset, 'trend-executions'),
      null,
      runtimeParameters,
    ).filters).not.toContainEqual({ field: 'trigger_type', operator: 'IN', value: ['TIME', 'MQ'] })
    expect(buildWidgetQueryRequest(
      scopedPreset,
      getDashboardWidget(scopedPreset, 'detail-canvas'),
      null,
      runtimeParameters,
    ).filters).toContainEqual({ field: 'trigger_type', operator: 'IN', value: ['TIME', 'MQ'] })
  })

  test('builds cascaded option query for lower dashboard controls from parent values', () => {
    const preset = getDefaultDashboardPreset()
    const runtimeParameters = resolveDashboardRuntimeParameters(
      preset,
      new URLSearchParams(''),
      null,
      new Date('2026-06-06T08:00:00Z'),
    )

    expect(buildDashboardControlOptionQuery(
      preset,
      'filter-canvas',
      runtimeParameters,
      '12',
    )).toEqual({
      datasetKey: 'canvas_daily_stats',
      dashboardKey: 'canvas-effect',
      dimensions: ['canvas_name'],
      metrics: [],
      filters: [
        { field: 'canvas_id', operator: 'EQ', value: 12 },
        { field: 'stat_date', operator: 'BETWEEN', value: ['2026-05-31', '2026-06-06'] },
      ],
      sorts: [{ field: 'canvas_name', direction: 'ASC' }],
      limit: 100,
    })
  })

  test('excludes target control current value when building cascaded options', () => {
    const preset = getDefaultDashboardPreset()

    expect(buildDashboardControlOptionQuery(preset, 'filter-trigger-type', {
      'filter-stat-date': ['2026-06-01', '2026-06-06'],
      'filter-canvas': 'Welcome Journey',
      'filter-trigger-type': 'TIME',
    })).toEqual({
      datasetKey: 'canvas_daily_stats',
      dashboardKey: 'canvas-effect',
      dimensions: ['trigger_type'],
      metrics: [],
      filters: [
        { field: 'stat_date', operator: 'BETWEEN', value: ['2026-06-01', '2026-06-06'] },
        { field: 'canvas_name', operator: 'EQ', value: 'Welcome Journey' },
      ],
      sorts: [{ field: 'trigger_type', direction: 'ASC' }],
      limit: 100,
    })
  })

  test('does not apply reverse cascade from child controls to parent option queries', () => {
    const preset = getDefaultDashboardPreset()

    expect(buildDashboardControlOptionQuery(preset, 'filter-canvas', {
      'filter-trigger-type': 'TIME,MQ',
    }).filters).not.toContainEqual({ field: 'trigger_type', operator: 'IN', value: ['TIME', 'MQ'] })
  })

  test('builds mapped cascade option query for non-same-source controls', () => {
    const preset = getDefaultDashboardPreset()
    const mappedPreset = {
      ...preset,
      filters: preset.filters.map(filter => filter.filterKey === 'filter-canvas'
        ? {
            ...filter,
            optionDatasetKey: 'campaign_canvas_dim',
            optionFieldKey: 'campaign_title',
            cascade: {
              mode: 'MAPPED' as const,
              parentFilterKeys: ['filter-trigger-type'],
              parentFieldMapping: { 'filter-trigger-type': 'channel_code' },
            },
          }
        : filter),
    }

    expect(buildDashboardControlOptionQuery(mappedPreset, 'filter-canvas', {
      'filter-trigger-type': ['TIME', 'MQ'],
      'filter-canvas': 'Current value ignored',
    })).toEqual({
      datasetKey: 'campaign_canvas_dim',
      dashboardKey: 'canvas-effect',
      dimensions: ['campaign_title'],
      metrics: [],
      filters: [{ field: 'channel_code', operator: 'IN', value: ['TIME', 'MQ'] }],
      sorts: [{ field: 'campaign_title', direction: 'ASC' }],
      limit: 100,
    })
  })

  test('builds self-service extraction query from dropped fields', () => {
    const preset = getDefaultDashboardPreset()
    const droppedDimension = dropSelfServiceExtractionField({ dimensions: [], metrics: [] }, 'DIMENSION', ' stat_date ')
    const droppedMetric = dropSelfServiceExtractionField(droppedDimension, 'METRIC', ' total_executions ')
    const duplicateDrop = dropSelfServiceExtractionField(droppedMetric, 'METRIC', 'total_executions')

    expect(duplicateDrop).toEqual({
      dimensions: ['stat_date'],
      metrics: ['total_executions'],
    })
    expect(buildSelfServiceExtractionQuery(preset, duplicateDrop, '12', 250)).toEqual({
      datasetKey: 'canvas_daily_stats',
      dashboardKey: 'canvas-effect',
      dimensions: ['stat_date'],
      metrics: ['total_executions'],
      filters: [{ field: 'canvas_id', operator: 'EQ', value: 12 }],
      sorts: [{ field: 'stat_date', direction: 'ASC' }],
      limit: 250,
    })
    expect(removeSelfServiceExtractionField(duplicateDrop, 'DIMENSION', 'stat_date')).toEqual({
      dimensions: [],
      metrics: ['total_executions'],
    })
  })

  test('derives stable query history detail rows', () => {
    expect(queryHistoryDetailRows({
      id: 99,
      datasetKey: 'canvas_daily_stats',
      username: 'alice',
      request: {
        datasetKey: 'canvas_daily_stats',
        dimensions: ['stat_date'],
        metrics: ['total_executions'],
        filters: [{ field: 'canvas_id', operator: 'EQ', value: 12 }],
        sorts: [{ field: 'stat_date', direction: 'ASC' }],
        limit: 100,
        offset: 200,
      },
      rowCount: 3,
      durationMs: 1250,
      status: 'SUCCESS',
      sqlHash: 'abcdef123456',
      errorMessage: null,
      createdAt: '2026-06-05T02:30:00',
    })).toEqual([
      { label: '查询', value: '#99 · SUCCESS · alice' },
      { label: '数据集', value: 'canvas_daily_stats' },
      { label: '字段', value: 'stat_date / total_executions' },
      { label: '过滤', value: 'canvas_id EQ 12' },
      { label: '排序', value: 'stat_date ASC' },
      { label: '分页', value: 'limit 100 · offset 200' },
      { label: '结果', value: '3 行 · 1250 ms' },
      { label: 'SQL Hash', value: 'abcdef123456' },
      { label: '错误', value: '-' },
      { label: '创建', value: '2026-06-05 02:30' },
    ])
  })

  test('derives stable query governance summary rows', () => {
    expect(queryGovernanceSummaryRows({
      totalQueries: 20,
      slowQueries: 2,
      failedQueries: 3,
      cacheHits: 4,
      averageDurationMs: 1250,
      timeoutPolicyMs: 30000,
      datasetQuotaRows: 1000000,
      slowAttributions: [{
        datasetKey: 'canvas_daily_stats',
        slowQueries: 2,
        timeoutPolicyMs: 15000,
        maxDurationMs: 44000,
        maxOverPolicyMs: 29000,
      }],
      datasets: [{
        datasetKey: 'canvas_daily_stats',
        totalQueries: 12,
        slowQueries: 2,
        failedQueries: 3,
        cacheHits: 1,
        averageDurationMs: 1850,
        maxDurationMs: 44000,
        timeoutPolicyMs: 15000,
        quotaRows: 120000,
        slowFailures: 1,
        slowCacheMisses: 2,
        maxOverPolicyMs: 29000,
        maxRowCount: 220000,
      }],
    })).toEqual([
      { label: '查询总量', value: '20 次 · 平均 1250 ms' },
      { label: '慢查询', value: '2 次 · 超时策略 30000 ms' },
      { label: '失败查询', value: '3 次' },
      { label: '缓存命中', value: '4 次' },
      { label: '数据集配额', value: '1000000 行' },
      { label: '最慢数据集', value: 'canvas_daily_stats · 44000 ms · 12 次 · 15000 ms/120000 行' },
      { label: '慢查询归因', value: 'canvas_daily_stats · 慢 2/12 · 超阈 29000 ms · 失败 1 · 未命中缓存 2 · 最大 220000 行' },
    ])
  })

  test('derives stable query governance policy rows', () => {
    expect(queryGovernancePolicyRows({
      defaultTimeoutMs: 20000,
      defaultQuotaRows: 500000,
      datasets: [
        { datasetKey: 'canvas_daily_stats', timeoutMs: 8000, quotaRows: 100000 },
      ],
    })).toEqual([
      { label: '默认策略', value: '20000 ms · 500000 行' },
      { label: '数据集策略', value: 'canvas_daily_stats · 8000 ms · 100000 行' },
    ])
  })

  test('derives stable query governance audit rows', () => {
    expect(queryGovernanceAuditRows([
      {
        id: 101,
        actorId: 'alice',
        actionKey: 'BI_QUERY_GOVERNANCE_POLICY_UPDATE',
        resourceType: 'BI_QUERY_GOVERNANCE_POLICY',
        detailJson: '{"after":{"defaultTimeoutMs":20000}}',
        createdAt: '2026-06-05T08:20:00',
      },
      {
        id: 104,
        actorId: 'bob',
        actionKey: 'BI_QUERY_GOVERNANCE_POLICY_UPDATE',
        resourceType: 'BI_QUERY_GOVERNANCE_POLICY',
        detailJson: '{"after":{"defaultQuotaRows":500000}}',
        createdAt: '2026-06-05T08:10:00',
      },
    ])).toEqual([
      {
        key: '101',
        actor: 'alice',
        action: 'BI_QUERY_GOVERNANCE_POLICY_UPDATE',
        resource: 'BI_QUERY_GOVERNANCE_POLICY',
        detail: '{"after":{"defaultTimeoutMs":20000}}',
        createdAt: '2026-06-05T08:20:00',
      },
      {
        key: '104',
        actor: 'bob',
        action: 'BI_QUERY_GOVERNANCE_POLICY_UPDATE',
        resource: 'BI_QUERY_GOVERNANCE_POLICY',
        detail: '{"after":{"defaultQuotaRows":500000}}',
        createdAt: '2026-06-05T08:10:00',
      },
    ])
  })

  test('derives stable query cache policy rows', () => {
    expect(queryCachePolicyRows({
      defaultEnabled: true,
      defaultTtlSeconds: 300,
      defaultCacheMode: 'CACHE',
      resources: [
        {
          resourceType: 'DATASET',
          resourceKey: 'canvas_daily_stats',
          enabled: false,
          ttlSeconds: 60,
          cacheMode: 'DIRECT_QUERY',
        },
        {
          resourceType: 'DASHBOARD',
          resourceKey: 'canvas-effect',
          enabled: true,
          ttlSeconds: 120,
          cacheMode: 'CACHE',
        },
      ],
    })).toEqual([
      { label: '默认缓存', value: '启用 · CACHE · 300 秒' },
      { label: '资源策略', value: 'DATASET/canvas_daily_stats · 关闭 · DIRECT_QUERY · 60 秒 / DASHBOARD/canvas-effect · 启用 · CACHE · 120 秒' },
    ])
  })

  test('builds query cache policy command with scoped resource override', () => {
    expect(buildQueryCachePolicyCommand(
      {
        defaultEnabled: true,
        defaultTtlSeconds: 300,
        defaultCacheMode: 'CACHE',
        resources: [
          {
            resourceType: 'DASHBOARD',
            resourceKey: 'canvas-effect',
            enabled: true,
            ttlSeconds: 120,
            cacheMode: 'CACHE',
          },
          {
            resourceType: 'DATASET',
            resourceKey: 'node_daily_stats',
            enabled: true,
            ttlSeconds: 600,
            cacheMode: 'CACHE',
          },
        ],
      },
      {
        enabled: false,
        ttlSeconds: 90,
        cacheMode: 'DIRECT_QUERY',
      },
      {
        resourceType: 'dashboard',
        resourceKey: ' canvas-effect ',
        enabled: false,
        ttlSeconds: 45,
        cacheMode: 'direct_query',
      },
    )).toEqual({
      defaultEnabled: false,
      defaultTtlSeconds: 90,
      defaultCacheMode: 'DIRECT_QUERY',
      resources: [
        {
          resourceType: 'DATASET',
          resourceKey: 'node_daily_stats',
          enabled: true,
          ttlSeconds: 600,
          cacheMode: 'CACHE',
        },
        {
          resourceType: 'DASHBOARD',
          resourceKey: 'canvas-effect',
          enabled: false,
          ttlSeconds: 45,
          cacheMode: 'DIRECT_QUERY',
        },
      ],
    })
  })

  test('derives batch query cache invalidation actions for active resources', () => {
    expect(queryCacheInvalidationActionRows('canvas_daily_stats')).toEqual([
      {
        key: 'dataset',
        label: '清当前数据集',
        command: { scope: 'DATASET', datasetKey: 'canvas_daily_stats' },
      },
      {
        key: 'all',
        label: '清全部缓存',
        command: { scope: 'ALL' },
      },
    ])
  })

  test('derives stable query cache stats rows', () => {
    expect(queryCacheStatsRows({
      provider: 'memory',
      enabled: true,
      entryCount: 2,
      maxEntries: 500,
      ttlSeconds: 300,
      hitCount: 8,
      missCount: 2,
      putCount: 5,
      evictionCount: 1,
    })).toEqual([
      { label: '缓存 Provider', value: 'memory · 启用' },
      { label: '缓存容量', value: '2/500 条 · TTL 300 秒' },
      { label: '命中率', value: '80.0% · 命中 8 / 未命中 2' },
      { label: '写入/驱逐', value: '写入 5 · 驱逐 1' },
    ])
  })

  test('derives dataset acceleration policy rows with extract refresh status', () => {
    expect(datasetAccelerationPolicyRows({
      datasetKey: 'canvas_daily_stats',
      enabled: true,
      accelerationMode: 'EXTRACT',
      refreshMode: 'SCHEDULED',
      refreshIntervalMinutes: 30,
      ttlSeconds: 900,
      maxRows: 500000,
      cronExpression: '0 0/30 * * * ?',
      materializedTable: 'bi_extract.t7_canvas_daily_stats_20260606101530',
      lastStatus: 'SUCCESS',
      lastRunId: 31,
      lastRefreshedAt: '2026-06-06T10:15:30',
      recentRuns: [
        {
          id: 31,
          datasetKey: 'canvas_daily_stats',
          status: 'SUCCESS',
          rowCount: 42000,
          durationMs: 137,
          materializedTable: 'bi_extract.t7_canvas_daily_stats_20260606101530',
          requestedBy: 'alice',
          startedAt: '2026-06-06T10:15:30',
          finishedAt: '2026-06-06T10:15:30',
          errorSummary: null,
        },
      ],
    })).toEqual([
      { label: '加速模式', value: '启用 · EXTRACT · SCHEDULED' },
      { label: '刷新策略', value: '30 分钟 · TTL 900 秒 · 上限 500000 行 · 0 0/30 * * * ?' },
      { label: '最近刷新', value: 'SUCCESS · run#31 · 2026-06-06T10:15:30 · bi_extract.t7_canvas_daily_stats_20260606101530' },
      { label: '刷新记录', value: 'SUCCESS · 42000 行 · 137 ms · alice' },
    ])
  })

  test('derives dataset acceleration scheduler observability rows', () => {
    expect(datasetAccelerationSchedulerRows({
      policiesChecked: 3,
      refreshed: 1,
      skipped: 1,
      failed: 1,
      items: [
        {
          datasetKey: 'orders_api_extract',
          status: 'REFRESHED',
          reason: 'SUCCESS',
          refreshRunId: 91,
          rowCount: 42000,
          durationMs: 137,
          materializedTable: 'bi_extract.t7_orders_api_extract_20260606100500',
          startedAt: '2026-06-06T10:04:58',
          finishedAt: '2026-06-06T10:05:00',
        },
        {
          datasetKey: 'fresh_api_extract',
          status: 'SKIPPED',
          reason: 'not due',
          refreshRunId: null,
          rowCount: null,
          durationMs: null,
          materializedTable: null,
          startedAt: null,
          finishedAt: null,
        },
      ],
    })).toEqual([
      {
        key: 'orders_api_extract-0',
        datasetKey: 'orders_api_extract',
        status: 'REFRESHED',
        reason: 'SUCCESS',
        run: 'run#91 · 42000 行 · 137 ms',
        materializedTable: 'bi_extract.t7_orders_api_extract_20260606100500',
        window: '2026-06-06T10:04:58 -> 2026-06-06T10:05:00',
      },
      {
        key: 'fresh_api_extract-1',
        datasetKey: 'fresh_api_extract',
        status: 'SKIPPED',
        reason: 'not due',
        run: 'run#- · - 行 · - ms',
        materializedTable: '-',
        window: '- -> -',
      },
    ])
  })

  test('derives Quick Engine capacity rows and alert policy command', () => {
    const summary = {
      tenantId: 7,
      capacityLimitRows: 200000,
      usedRows: 173000,
      usagePercent: 86.5,
      alertLevel: 'WARNING',
      alertEnabled: true,
      alertPolicy: {
        enabled: true,
        capacityLimitRows: 200000,
        warningThresholdPercent: 80,
        criticalThresholdPercent: 95,
        notificationChannels: ['LARK', 'EMAIL'],
        notificationReceivers: ['bi-ops', 'alice'],
        updatedBy: 'ops',
        updatedAt: '2026-06-06T12:00:00',
      },
      tenantPoolPolicy: {
        poolKey: 'GOLD',
        maxConcurrentQueries: 4,
        queueLimit: 10,
        queueTimeoutSeconds: 180,
        poolWeight: 200,
        updatedBy: 'ops',
        updatedAt: '2026-06-06T12:00:00',
      },
      concurrencyQueue: {
        runningQueries: 2,
        queuedQueries: 1,
        blockedQueries: 1,
        successfulQueries: 2,
        failedQueries: 0,
        concurrencyUsagePercent: 50,
        queueUsagePercent: 10,
        state: 'NORMAL',
      },
      categories: [
        { type: 'DATASET_ACCELERATION', usedRows: 173000, resourceCount: 2 },
      ],
      details: [
        {
          type: 'DATASET_ACCELERATION',
          resourceKey: 'node_daily_stats',
          usedRows: 90000,
          activeTables: 1,
          latestRunId: 88,
          latestFinishedAt: '2026-06-06T10:00:00',
          latestRowCount: 90000,
          owner: 'bob',
        },
      ],
      userRankings: [
        { user: 'bob', usedRows: 90000, activeTables: 1, resourceCount: 1 },
      ],
    }

    expect(quickEngineCapacitySummaryRows(summary)).toEqual([
      { label: '容量水位', value: '173000/200000 行 · 86.5% · WARNING' },
      { label: '告警策略', value: '启用 · warning 80% · critical 95%' },
      { label: '通知策略', value: 'LARK, EMAIL · bi-ops, alice' },
      { label: '容量分类', value: 'DATASET_ACCELERATION 173000 行 / 2 个资源' },
    ])
    expect(quickEngineConcurrencyQueueRows(summary)).toEqual([
      { label: '租户容量池', value: 'GOLD · 并发 4 · 队列 10 · 等待 180 秒 · 权重 200' },
      { label: '并发队列', value: '2/4 并发 · 1/10 队列 · NORMAL' },
      { label: '最近结果', value: '成功 2 · 阻断 1 · 失败 0' },
    ])
    expect(quickEngineCapacityDetailRows(summary)).toEqual([
      {
        key: 'node_daily_stats',
        type: 'DATASET_ACCELERATION',
        resourceKey: 'node_daily_stats',
        usedRows: 90000,
        activeTables: 1,
        latest: 'run#88 · 2026-06-06T10:00:00 · 90000 行',
        owner: 'bob',
      },
    ])
    expect(quickEngineCapacityUserRows(summary)).toEqual([
      {
        key: 'bob',
        user: 'bob',
        usedRows: 90000,
        activeTables: 1,
        resourceCount: 1,
      },
    ])
    expect(buildQuickEngineCapacityAlertPolicyCommand({
      enabled: true,
      capacityLimitRows: 500000,
      warningThresholdPercent: 75,
      criticalThresholdPercent: 95,
      notificationChannels: ' lark, email ',
      notificationReceivers: ' bi-ops, alice ',
    })).toEqual({
      enabled: true,
      capacityLimitRows: 500000,
      warningThresholdPercent: 75,
      criticalThresholdPercent: 95,
      notificationChannels: ['LARK', 'EMAIL'],
      notificationReceivers: ['bi-ops', 'alice'],
    })
    expect(buildQuickEngineTenantPoolPolicyCommand({
      poolKey: ' gold ',
      maxConcurrentQueries: 16,
      queueLimit: 120,
      queueTimeoutSeconds: 300,
      poolWeight: 200,
    })).toEqual({
      poolKey: 'GOLD',
      maxConcurrentQueries: 16,
      queueLimit: 120,
      queueTimeoutSeconds: 300,
      poolWeight: 200,
    })
  })

  test('derives stable permission audit rows', () => {
    expect(permissionAuditRows([
      {
        id: 101,
        actorId: 'alice',
        actionKey: 'BI_PERMISSION_CHANGE',
        resourceType: 'BI_PERMISSION',
        detailJson: '{"permissionKind":"RESOURCE","operation":"CREATE"}',
        createdAt: '2026-06-05T09:20:00',
      },
      {
        id: 104,
        actorId: 'bob',
        actionKey: 'BI_PERMISSION_CHANGE',
        resourceType: 'BI_PERMISSION',
        detailJson: '{"permissionKind":"ROW","operation":"DELETE"}',
        createdAt: '2026-06-05T09:10:00',
      },
    ])).toEqual([
      {
        key: '101',
        actor: 'alice',
        action: 'BI_PERMISSION_CHANGE',
        resource: 'BI_PERMISSION',
        detail: '{"permissionKind":"RESOURCE","operation":"CREATE"}',
        createdAt: '2026-06-05T09:20:00',
      },
      {
        key: '104',
        actor: 'bob',
        action: 'BI_PERMISSION_CHANGE',
        resource: 'BI_PERMISSION',
        detail: '{"permissionKind":"ROW","operation":"DELETE"}',
        createdAt: '2026-06-05T09:10:00',
      },
    ])
  })

  test('derives stable datasource health history rows', () => {
    expect(datasourceHealthHistoryRows([
      {
        sourceKey: 'doris',
        sourceType: 'DORIS',
        available: false,
        message: 'connect timeout',
        checkedAt: '2026-06-05T08:10:00',
      },
      {
        sourceKey: 'primary',
        sourceType: 'MYSQL',
        available: true,
        message: 'available',
        checkedAt: '2026-06-05T08:09:00',
      },
    ])).toEqual([
      { key: 'doris-2026-06-05T08:10:00', source: 'doris / DORIS', status: '异常', message: 'connect timeout', checkedAt: '2026-06-05T08:10:00' },
      { key: 'primary-2026-06-05T08:09:00', source: 'primary / MYSQL', status: '正常', message: 'available', checkedAt: '2026-06-05T08:09:00' },
    ])
  })

  test('derives stable datasource health SLO rows', () => {
    expect(datasourceHealthSloRows({
      totalChecks: 4,
      availableChecks: 3,
      unavailableChecks: 1,
      availabilityRate: 75,
      sources: [
        {
          sourceKey: 'doris',
          sourceType: 'DORIS',
          totalChecks: 2,
          availableChecks: 1,
          unavailableChecks: 1,
          availabilityRate: 50,
          lastCheckedAt: '2026-06-05T08:10:00',
          lastMessage: 'timeout',
        },
      ],
    })).toEqual([
      { label: '整体可用率', value: '75% · 3/4 正常' },
      { label: '异常检查', value: '1 次' },
      { label: '最弱数据源', value: 'doris / DORIS · 50% · timeout' },
    ])
  })

  test('derives stable BI datasource connector rows', () => {
    expect(datasourceConnectorRows([
      {
        connectorType: 'MYSQL',
        label: 'MySQL',
        sourceCategory: 'JDBC',
        supportedModes: ['DIRECT_QUERY', 'CACHE'],
        supportStatus: 'AVAILABLE',
        capacityCategory: 'INTERACTIVE_QUERY',
        capacityNote: 'Interactive JDBC capacity',
        supportsConnectionTest: true,
        supportsSchemaSync: true,
        supportsSqlDataset: true,
        supportsTableDataset: true,
        supportsCredentials: true,
        driverClassNames: ['com.mysql.cj.jdbc.Driver'],
        note: 'JDBC connector is available',
      },
      {
        connectorType: 'API',
        label: 'API',
        sourceCategory: 'HTTP',
        supportedModes: ['EXTRACT'],
        supportStatus: 'PLANNED',
        capacityCategory: 'HTTP_EXTRACT_SMALL',
        capacityNote: 'HTTP extract capacity',
        supportsConnectionTest: false,
        supportsSchemaSync: false,
        supportsSqlDataset: false,
        supportsTableDataset: false,
        supportsCredentials: true,
        driverClassNames: [],
        note: 'Planned connector',
      },
    ])).toEqual([
      {
        key: 'MYSQL',
        connector: 'MySQL / MYSQL',
        category: 'JDBC',
        capacity: 'INTERACTIVE_QUERY · Interactive JDBC capacity',
        modes: 'DIRECT_QUERY / CACHE',
        status: 'AVAILABLE',
        capabilities: '连通性 · 元数据同步 · SQL 数据集 · 表数据集 · 凭证',
      },
      {
        key: 'API',
        connector: 'API / API',
        category: 'HTTP',
        capacity: 'HTTP_EXTRACT_SMALL · HTTP extract capacity',
        modes: 'EXTRACT',
        status: 'PLANNED',
        capabilities: '凭证',
      },
    ])
  })

  test('derives stable BI datasource onboarding rows', () => {
    expect(datasourceOnboardingRows([
      {
        id: 7,
        sourceKey: 'jdbc-7',
        name: 'production warehouse',
        type: 'JDBC',
        connectorType: 'MYSQL',
        enabled: true,
        driverClassName: 'com.mysql.cj.jdbc.Driver',
        maskedUrl: 'jdbc:mysql://db.example.com:3306/canvas?password=***',
        maskedUsername: 'ca***pp',
        connectionMode: 'DIRECT_QUERY',
        schemaSyncStatus: 'NOT_SYNCED',
        tableCount: 0,
        lastSyncedAt: null,
        supportedModes: ['DIRECT_QUERY', 'CACHE'],
        supportStatus: 'AVAILABLE',
        capabilities: ['CONNECTION_TEST', 'SCHEMA_SYNC', 'SQL_DATASET', 'TABLE_DATASET'],
      },
    ])).toEqual([
      {
        key: 'jdbc-7',
        id: 7,
        source: 'production warehouse · jdbc-7',
        connector: 'MYSQL / JDBC',
        status: '启用 · AVAILABLE',
        connection: 'DIRECT_QUERY · DIRECT_QUERY / CACHE',
        schema: 'NOT_SYNCED · 0 表',
        credential: 'ca***pp · jdbc:mysql://db.example.com:3306/canvas?password=***',
      },
    ])
  })

  test('derives stable BI datasource connection test rows', () => {
    expect(datasourceConnectionTestRows({
      id: 7,
      sourceKey: 'jdbc-7',
      connectorType: 'MYSQL',
      success: true,
      message: 'connection ok',
      databaseProductName: 'MySQL',
      databaseProductVersion: '8.0.35',
      checkedAt: '2026-06-06T01:40:00',
      durationMs: 12,
    })).toEqual([
      { label: '状态', value: '成功 · connection ok' },
      { label: '数据源', value: 'jdbc-7 / MYSQL' },
      { label: '数据库', value: 'MySQL 8.0.35' },
      { label: '耗时', value: '12 ms' },
      { label: '检查时间', value: '2026-06-06T01:40:00' },
    ])
  })

  test('derives stable BI datasource schema preview rows', () => {
    expect(datasourceSchemaPreviewRows({
      id: 7,
      sourceKey: 'jdbc-7',
      name: 'production warehouse',
      connectorType: 'MYSQL',
      checkedAt: '2026-06-06T01:45:00',
      tables: [
        {
          name: 'campaign_daily',
          tableType: 'TABLE',
          columns: [
            { name: 'stat_date', typeName: 'DATE', dataType: 91, nullable: false, ordinalPosition: 1 },
            { name: 'total_cost', typeName: 'DECIMAL', dataType: 3, nullable: true, ordinalPosition: 2 },
          ],
        },
      ],
    })).toEqual([
      {
        key: 'campaign_daily',
        table: 'campaign_daily / TABLE',
        columns: 'stat_date DATE 必填 · total_cost DECIMAL 可空',
        columnCount: '2 字段',
        checkedAt: '2026-06-06T01:45:00',
      },
    ])
  })

  test('derives stable BI datasource schema snapshot summary rows', () => {
    expect(datasourceSchemaSnapshotRows({
      id: 101,
      dataSourceConfigId: 7,
      sourceKey: 'jdbc-7',
      name: 'production warehouse',
      connectorType: 'MYSQL',
      syncStatus: 'SUCCESS',
      errorMessage: null,
      tableCount: 2,
      columnCount: 3,
      tables: [],
      syncedAt: '2026-06-06T02:15:00',
      syncedBy: 'alice',
    })).toEqual([
      { label: '状态', value: 'SUCCESS · 2 表 · 3 字段' },
      { label: '数据源', value: 'production warehouse · jdbc-7 / MYSQL' },
      { label: '同步人', value: 'alice' },
      { label: '同步时间', value: '2026-06-06T02:15:00' },
      { label: '错误', value: '-' },
    ])
  })

  test('derives stable BI datasource schema snapshot history rows', () => {
    expect(datasourceSchemaSnapshotHistoryRows([
      {
        id: 101,
        dataSourceConfigId: 7,
        sourceKey: 'jdbc-7',
        name: 'production warehouse',
        connectorType: 'MYSQL',
        syncStatus: 'SUCCESS',
        errorMessage: null,
        tableCount: 2,
        columnCount: 3,
        tables: [],
        syncedAt: '2026-06-06T02:15:00',
        syncedBy: 'alice',
      },
      {
        id: 102,
        dataSourceConfigId: 7,
        sourceKey: 'jdbc-7',
        name: 'production warehouse',
        connectorType: 'MYSQL',
        syncStatus: 'FAILED',
        errorMessage: 'connection refused',
        tableCount: 0,
        columnCount: 0,
        tables: [],
        syncedAt: '2026-06-06T02:10:00',
        syncedBy: 'bob',
      },
    ])).toEqual([
      {
        key: '101',
        source: 'production warehouse · jdbc-7',
        status: 'SUCCESS',
        schema: '2 表 · 3 字段',
        syncedAt: '2026-06-06T02:15:00',
        syncedBy: 'alice',
        error: '-',
      },
      {
        key: '102',
        source: 'production warehouse · jdbc-7',
        status: 'FAILED',
        schema: '0 表 · 0 字段',
        syncedAt: '2026-06-06T02:10:00',
        syncedBy: 'bob',
        error: 'connection refused',
      },
    ])
  })

  test('builds datasource table dataset command from schema snapshot table', () => {
    expect(buildDatasourceTableDatasetCommand({
      id: 101,
      dataSourceConfigId: 7,
      sourceKey: 'jdbc-7',
      name: 'production warehouse',
      connectorType: 'MYSQL',
      syncStatus: 'SUCCESS',
      errorMessage: null,
      tableCount: 1,
      columnCount: 4,
      syncedAt: '2026-06-06T02:15:00',
      syncedBy: 'alice',
      tables: [
        {
          name: 'campaign_daily',
          tableType: 'TABLE',
          columns: [
            { name: 'tenant_id', typeName: 'BIGINT', dataType: -5, nullable: false, ordinalPosition: 1 },
            { name: 'stat_date', typeName: 'DATE', dataType: 91, nullable: false, ordinalPosition: 2 },
            { name: 'campaign_name', typeName: 'VARCHAR', dataType: 12, nullable: true, ordinalPosition: 3 },
            { name: 'total_cost', typeName: 'DECIMAL', dataType: 3, nullable: true, ordinalPosition: 4 },
          ],
        },
      ],
    }, 'campaign_daily')).toEqual({
      dataSourceConfigId: 7,
      tableName: 'campaign_daily',
      datasetKey: 'jdbc_7_campaign_daily',
      name: 'production warehouse campaign_daily',
      tenantColumn: 'tenant_id',
      selectedColumns: ['tenant_id', 'stat_date', 'campaign_name', 'total_cost'],
    })
  })

  test('builds API datasource table dataset command with runtime variables and synthetic tenant column', () => {
    expect(buildDatasourceTableDatasetCommand({
      id: 202,
      dataSourceConfigId: 81,
      sourceKey: 'api-81',
      name: 'Orders API',
      connectorType: 'API',
      syncStatus: 'SUCCESS',
      errorMessage: null,
      tableCount: 1,
      columnCount: 3,
      syncedAt: '2026-06-06T03:25:00',
      syncedBy: 'alice',
      tables: [
        {
          name: 'api_response',
          tableType: 'HTTP_JSON',
          columns: [
            { name: 'order_id', typeName: 'VARCHAR', dataType: 12, nullable: false, ordinalPosition: 1 },
            { name: 'amount', typeName: 'DOUBLE', dataType: 8, nullable: true, ordinalPosition: 2 },
            { name: 'paid', typeName: 'BOOLEAN', dataType: 16, nullable: true, ordinalPosition: 3 },
          ],
        },
      ],
    }, 'api_response', { campaignId: 'cmp-1' })).toEqual({
      dataSourceConfigId: 81,
      tableName: 'api_response',
      datasetKey: 'api_81_api_response',
      name: 'Orders API api_response',
      tenantColumn: 'tenant_id',
      selectedColumns: ['order_id', 'amount', 'paid'],
      apiResponseVariables: { campaignId: 'cmp-1' },
    })
  })

  test('builds SQL dataset draft resource with ordered template parameters', () => {
    const sqlTemplate = [
      'SELECT tenant_id, stat_date, channel, total_cost',
      'FROM campaign_daily',
      'WHERE stat_date >= {{start_date}}',
      'AND stat_date <= {{end_date}}',
      'AND channel = {{channel}}',
      'AND channel <> {{channel}}',
    ].join('\n')

    const parameterDrafts = buildSqlDatasetParameterDrafts(sqlTemplate, [
      { key: 'channel', dataType: ' string ', required: false, defaultValue: 'PAID', allowedValuesText: 'PAID, EMAIL' },
      { key: 'unused', dataType: 'DATE', required: true, defaultValue: '2026-06-01', allowedValuesText: '' },
    ])

    expect(parameterDrafts).toEqual([
      { key: 'start_date', dataType: 'DATE', required: true, defaultValue: '', allowedValuesText: '' },
      { key: 'end_date', dataType: 'DATE', required: true, defaultValue: '', allowedValuesText: '' },
      { key: 'channel', dataType: 'STRING', required: false, defaultValue: 'PAID', allowedValuesText: 'PAID, EMAIL' },
    ])

    expect(buildSqlDatasetDraftResource({
      dataSourceConfigId: 7,
      datasetKey: ' Campaign SQL ',
      name: ' Campaign SQL ',
      sqlTemplate,
      tenantColumn: '',
      parameters: parameterDrafts,
      fields: [
        { fieldKey: 'tenant_id', displayName: 'Tenant', columnExpression: 'tenant_id', role: 'DIMENSION', dataType: 'NUMBER' },
        { fieldKey: 'stat_date', displayName: 'Date', columnExpression: 'stat_date', role: 'DIMENSION', dataType: 'DATE' },
        { fieldKey: 'channel', displayName: 'Channel', columnExpression: 'channel', role: 'DIMENSION', dataType: 'STRING' },
      ],
      metrics: [
        { metricKey: 'total_cost', displayName: 'Cost', expression: 'SUM(total_cost)', aggregation: 'SUM', dataType: 'NUMBER', allowedDimensions: ['stat_date', 'channel'] },
      ],
    })).toEqual({
      datasetKey: 'campaign_sql',
      name: 'Campaign SQL',
      datasetType: 'SQL',
      tableExpression: sqlTemplate,
      tenantColumn: 'tenant_id',
      model: {
        dataSourceConfigId: 7,
        sqlApprovalRequired: true,
        sqlTemplate,
        sqlParameterOrder: ['start_date', 'end_date', 'channel'],
        sqlParameters: [
          { key: 'start_date', dataType: 'DATE', required: true, allowedValues: [] },
          { key: 'end_date', dataType: 'DATE', required: true, allowedValues: [] },
          { key: 'channel', dataType: 'STRING', required: false, defaultValue: 'PAID', allowedValues: ['PAID', 'EMAIL'] },
        ],
      },
      fields: [
        {
          fieldKey: 'tenant_id',
          displayName: 'Tenant',
          columnExpression: 'tenant_id',
          role: 'DIMENSION',
          dataType: 'NUMBER',
          semanticType: null,
          defaultAggregation: null,
          formatPattern: null,
          unit: null,
          visible: true,
          sensitiveLevel: 'NORMAL',
          sortOrder: 0,
        },
        {
          fieldKey: 'stat_date',
          displayName: 'Date',
          columnExpression: 'stat_date',
          role: 'DIMENSION',
          dataType: 'DATE',
          semanticType: null,
          defaultAggregation: null,
          formatPattern: null,
          unit: null,
          visible: true,
          sensitiveLevel: 'NORMAL',
          sortOrder: 1,
        },
        {
          fieldKey: 'channel',
          displayName: 'Channel',
          columnExpression: 'channel',
          role: 'DIMENSION',
          dataType: 'STRING',
          semanticType: null,
          defaultAggregation: null,
          formatPattern: null,
          unit: null,
          visible: true,
          sensitiveLevel: 'NORMAL',
          sortOrder: 2,
        },
      ],
      metrics: [
        {
          metricKey: 'total_cost',
          displayName: 'Cost',
          expression: 'SUM(total_cost)',
          aggregation: 'SUM',
          dataType: 'NUMBER',
          unit: null,
          formatPattern: null,
          allowedDimensions: ['stat_date', 'channel'],
          owner: null,
          description: null,
          status: 'ACTIVE',
        },
      ],
      status: 'DRAFT',
      source: 'CLIENT',
    })
  })

  test('builds datasource onboarding command from QuickBI wizard draft', () => {
    expect(buildDatasourceOnboardingCommand(
      {
        connectorType: ' mysql ',
        name: ' Marketing Warehouse ',
        url: ' jdbc:mysql://warehouse.example.com:3306/marketing ',
        username: ' canvas_app ',
        password: 'plain-password',
        driverClassName: ' ',
        description: ' BI datasource center ',
        enabled: null,
        connectionMode: ' cache ',
      },
      [{
        connectorType: 'MYSQL',
        label: 'MySQL',
        sourceCategory: 'JDBC',
        supportedModes: ['DIRECT_QUERY', 'CACHE'],
        supportStatus: 'AVAILABLE',
        supportsConnectionTest: true,
        supportsSchemaSync: true,
        supportsSqlDataset: true,
        supportsTableDataset: true,
        supportsCredentials: true,
        driverClassNames: ['com.mysql.cj.jdbc.Driver'],
        note: '',
      }],
    )).toEqual({
      connectorType: 'MYSQL',
      name: 'Marketing Warehouse',
      url: 'jdbc:mysql://warehouse.example.com:3306/marketing',
      username: 'canvas_app',
      password: 'plain-password',
      driverClassName: 'com.mysql.cj.jdbc.Driver',
      description: 'BI datasource center',
      enabled: true,
      connectionMode: 'CACHE',
    })
  })

  test('builds API datasource onboarding connector config from HTTP extract draft', () => {
    expect(buildDatasourceOnboardingCommand(
      {
        connectorType: ' api ',
        name: ' Orders API ',
        url: ' https://api.example.com/orders?token=url-secret ',
        username: ' Authorization ',
        password: ' bearer-token ',
        connectionMode: ' direct_query ',
        apiRequestMethod: ' post ',
        apiAuthType: ' bearer ',
        apiHeaderName: ' X-Tenant ',
        apiHeaderValue: ' {{tenantId}} ',
        apiParameterName: ' page ',
        apiParameterValue: ' {{page}} ',
        apiBodyTemplate: ' {"campaign":"{{campaignId}}"} ',
        apiResponseRowsPath: ' $.data.items ',
      },
      [{
        connectorType: 'API',
        label: 'API',
        sourceCategory: 'HTTP',
        supportedModes: ['EXTRACT'],
        supportStatus: 'AVAILABLE',
        supportsConnectionTest: false,
        supportsSchemaSync: false,
        supportsSqlDataset: false,
        supportsTableDataset: true,
        supportsCredentials: true,
        driverClassNames: [],
        note: '',
      }],
    )).toEqual({
      connectorType: 'API',
      name: 'Orders API',
      url: 'https://api.example.com/orders?token=url-secret',
      username: 'Authorization',
      password: 'bearer-token',
      driverClassName: '',
      description: '',
      enabled: true,
      connectionMode: 'EXTRACT',
      connectorConfig: {
        requestMethod: 'POST',
        authType: 'BEARER',
        headers: [{ name: 'X-Tenant', value: '{{tenantId}}', variable: true }],
        parameters: [{ name: 'page', value: '{{page}}', variable: true }],
        bodyTemplate: '{"campaign":"{{campaignId}}"}',
        responseRowsPath: '$.data.items',
        responseFormat: 'JSON',
      },
    })
  })

  test('builds app datasource onboarding config through the HTTP extract path', () => {
    expect(buildDatasourceOnboardingCommand(
      {
        connectorType: ' APP_ANALYTICS ',
        name: ' Campaign App ',
        url: ' https://app.example.com/openapi/campaigns ',
        username: ' Authorization ',
        password: ' app-token ',
        connectionMode: ' direct_query ',
        apiRequestMethod: ' get ',
        apiAuthType: ' bearer ',
        apiResponseRowsPath: ' $.campaigns ',
      },
      [{
        connectorType: 'APP_ANALYTICS',
        label: 'Application Analytics',
        sourceCategory: 'APP',
        supportedModes: ['EXTRACT'],
        supportStatus: 'AVAILABLE',
        capacityCategory: 'APP_EXTRACT_SMALL',
        capacityNote: 'Application extract capacity',
        supportsConnectionTest: false,
        supportsSchemaSync: false,
        supportsSqlDataset: false,
        supportsTableDataset: true,
        supportsCredentials: true,
        driverClassNames: [],
        note: '',
      }],
    )).toEqual({
      connectorType: 'APP_ANALYTICS',
      name: 'Campaign App',
      url: 'https://app.example.com/openapi/campaigns',
      username: 'Authorization',
      password: 'app-token',
      driverClassName: '',
      description: '',
      enabled: true,
      connectionMode: 'EXTRACT',
      connectorConfig: {
        requestMethod: 'GET',
        authType: 'BEARER',
        headers: [],
        parameters: [],
        responseRowsPath: '$.campaigns',
        responseFormat: 'JSON',
      },
    })
  })

  test('builds CSV Excel datasource onboarding connector config from file draft', () => {
    expect(buildDatasourceOnboardingCommand(
      {
        connectorType: ' csv_excel ',
        name: ' Upload extract ',
        fileName: ' orders.xlsx ',
        fileType: ' xlsx ',
        fileSheetName: ' Orders ',
        fileDelimiter: ' | ',
        fileHeaderRow: true,
        fileEncoding: ' utf-8 ',
        connectionMode: ' direct_query ',
      },
      [{
        connectorType: 'CSV_EXCEL',
        label: 'CSV / Excel',
        sourceCategory: 'FILE',
        supportedModes: ['EXTRACT'],
        supportStatus: 'AVAILABLE',
        supportsConnectionTest: false,
        supportsSchemaSync: false,
        supportsSqlDataset: false,
        supportsTableDataset: true,
        supportsCredentials: false,
        driverClassNames: [],
        note: '',
      }],
    )).toEqual({
      connectorType: 'CSV_EXCEL',
      name: 'Upload extract',
      url: 'file://orders.xlsx',
      username: '',
      password: '',
      driverClassName: '',
      description: '',
      enabled: true,
      connectionMode: 'EXTRACT',
      connectorConfig: {
        fileName: 'orders.xlsx',
        fileType: 'XLSX',
        sheetName: 'Orders',
        delimiter: '|',
        headerRow: true,
        encoding: 'UTF-8',
      },
    })
  })

  test('builds datasource multi-table dataset command from schema snapshot joins', () => {
    expect(buildDatasourceMultiTableDatasetCommand({
      id: 101,
      dataSourceConfigId: 7,
      sourceKey: 'jdbc-7',
      name: 'production warehouse',
      connectorType: 'MYSQL',
      syncStatus: 'SUCCESS',
      errorMessage: null,
      tableCount: 2,
      columnCount: 7,
      syncedAt: '2026-06-06T02:15:00',
      syncedBy: 'alice',
      tables: [
        {
          name: 'campaign_daily',
          tableType: 'TABLE',
          columns: [
            { name: 'tenant_id', typeName: 'BIGINT', dataType: -5, nullable: false, ordinalPosition: 1 },
            { name: 'stat_date', typeName: 'DATE', dataType: 91, nullable: false, ordinalPosition: 2 },
            { name: 'campaign_id', typeName: 'BIGINT', dataType: -5, nullable: false, ordinalPosition: 3 },
            { name: 'total_cost', typeName: 'DECIMAL', dataType: 3, nullable: true, ordinalPosition: 4 },
          ],
        },
        {
          name: 'campaign_dim',
          tableType: 'TABLE',
          columns: [
            { name: 'campaign_id', typeName: 'BIGINT', dataType: -5, nullable: false, ordinalPosition: 1 },
            { name: 'campaign_name', typeName: 'VARCHAR', dataType: 12, nullable: true, ordinalPosition: 2 },
          ],
        },
      ],
    }, {
      baseTableName: 'campaign_daily',
      tableNames: ['campaign_daily', 'campaign_dim'],
      tenantColumn: 'tenant_id',
      joins: [
        {
          joinType: 'LEFT',
          leftTableName: 'campaign_daily',
          leftColumn: 'campaign_id',
          rightTableName: 'campaign_dim',
          rightColumn: 'campaign_id',
        },
      ],
    })).toEqual({
      dataSourceConfigId: 7,
      datasetKey: 'jdbc_7_campaign_daily_campaign_dim',
      name: 'production warehouse campaign_daily + campaign_dim',
      baseTableName: 'campaign_daily',
      tenantColumn: 'tenant_id',
      tables: [
        {
          tableName: 'campaign_daily',
          alias: 'campaign_daily',
          selectedColumns: ['tenant_id', 'stat_date', 'campaign_id', 'total_cost'],
        },
        {
          tableName: 'campaign_dim',
          alias: 'campaign_dim',
          selectedColumns: ['campaign_id', 'campaign_name'],
        },
      ],
      joins: [
        {
          joinType: 'LEFT',
          leftAlias: 'campaign_daily',
          leftColumn: 'campaign_id',
          rightAlias: 'campaign_dim',
          rightColumn: 'campaign_id',
          conditions: [
            { leftColumn: 'campaign_id', rightColumn: 'campaign_id' },
          ],
        },
      ],
      graph: {
        layoutMode: 'GRAPH_CANVAS',
        nodes: [
          { tableName: 'campaign_daily', alias: 'campaign_daily', x: 80, y: 80 },
          { tableName: 'campaign_dim', alias: 'campaign_dim', x: 360, y: 80 },
        ],
      },
    })
  })

  test('builds datasource multi-table dataset command with composite join conditions', () => {
    expect(buildDatasourceMultiTableDatasetCommand({
      id: 102,
      dataSourceConfigId: 8,
      sourceKey: 'jdbc-8',
      name: 'relationship warehouse',
      connectorType: 'MYSQL',
      syncStatus: 'SUCCESS',
      errorMessage: null,
      tableCount: 2,
      columnCount: 7,
      syncedAt: '2026-06-06T03:15:00',
      syncedBy: 'alice',
      tables: [
        {
          name: 'campaign_daily',
          tableType: 'TABLE',
          columns: [
            { name: 'tenant_id', typeName: 'BIGINT', dataType: -5, nullable: false, ordinalPosition: 1 },
            { name: 'campaign_id', typeName: 'BIGINT', dataType: -5, nullable: false, ordinalPosition: 2 },
            { name: 'stat_date', typeName: 'DATE', dataType: 91, nullable: false, ordinalPosition: 3 },
            { name: 'total_cost', typeName: 'DECIMAL', dataType: 3, nullable: true, ordinalPosition: 4 },
          ],
        },
        {
          name: 'campaign_budget',
          tableType: 'TABLE',
          columns: [
            { name: 'tenant_id', typeName: 'BIGINT', dataType: -5, nullable: false, ordinalPosition: 1 },
            { name: 'campaign_id', typeName: 'BIGINT', dataType: -5, nullable: false, ordinalPosition: 2 },
            { name: 'budget_amount', typeName: 'DECIMAL', dataType: 3, nullable: true, ordinalPosition: 3 },
          ],
        },
      ],
    }, {
      baseTableName: 'campaign_daily',
      tableNames: ['campaign_daily', 'campaign_budget'],
      tenantColumn: 'tenant_id',
      joins: [
        {
          joinType: 'INNER',
          leftTableName: 'campaign_daily',
          rightTableName: 'campaign_budget',
          conditions: [
            { leftColumn: 'tenant_id', rightColumn: 'tenant_id' },
            { leftColumn: 'campaign_id', rightColumn: 'campaign_id', operator: '<>', connector: 'OR' },
          ],
        },
      ],
    }).joins).toEqual([
      {
        joinType: 'INNER',
        leftAlias: 'campaign_daily',
        leftColumn: 'tenant_id',
        rightAlias: 'campaign_budget',
        rightColumn: 'tenant_id',
        conditions: [
          { leftColumn: 'tenant_id', rightColumn: 'tenant_id' },
          { leftColumn: 'campaign_id', rightColumn: 'campaign_id', operator: '<>', connector: 'OR' },
        ],
      },
    ])
  })

  test('builds datasource multi-table dataset command with graph canvas metadata', () => {
    expect(buildDatasourceMultiTableDatasetCommand({
      id: 103,
      dataSourceConfigId: 9,
      sourceKey: 'jdbc-9',
      name: 'graph warehouse',
      connectorType: 'MYSQL',
      syncStatus: 'SUCCESS',
      errorMessage: null,
      tableCount: 3,
      columnCount: 9,
      syncedAt: '2026-06-06T04:15:00',
      syncedBy: 'alice',
      tables: [
        {
          name: 'campaign_daily',
          tableType: 'TABLE',
          columns: [
            { name: 'tenant_id', typeName: 'BIGINT', dataType: -5, nullable: false, ordinalPosition: 1 },
            { name: 'campaign_id', typeName: 'BIGINT', dataType: -5, nullable: false, ordinalPosition: 2 },
            { name: 'total_cost', typeName: 'DECIMAL', dataType: 3, nullable: true, ordinalPosition: 3 },
          ],
        },
        {
          name: 'campaign_dim',
          tableType: 'TABLE',
          columns: [
            { name: 'tenant_id', typeName: 'BIGINT', dataType: -5, nullable: false, ordinalPosition: 1 },
            { name: 'campaign_id', typeName: 'BIGINT', dataType: -5, nullable: false, ordinalPosition: 2 },
            { name: 'campaign_name', typeName: 'VARCHAR', dataType: 12, nullable: true, ordinalPosition: 3 },
          ],
        },
        {
          name: 'campaign_budget',
          tableType: 'TABLE',
          columns: [
            { name: 'tenant_id', typeName: 'BIGINT', dataType: -5, nullable: false, ordinalPosition: 1 },
            { name: 'campaign_id', typeName: 'BIGINT', dataType: -5, nullable: false, ordinalPosition: 2 },
            { name: 'budget_amount', typeName: 'DECIMAL', dataType: 3, nullable: true, ordinalPosition: 3 },
          ],
        },
      ],
    }, {
      baseTableName: 'campaign_daily',
      tableNames: ['campaign_daily', 'campaign_dim', 'campaign_budget'],
      tenantColumn: 'tenant_id',
      joins: [
        {
          joinType: 'LEFT',
          leftTableName: 'campaign_daily',
          rightTableName: 'campaign_dim',
          conditions: [
            { leftColumn: 'tenant_id', rightColumn: 'tenant_id' },
            { leftColumn: 'campaign_id', rightColumn: 'campaign_id' },
          ],
        },
        {
          joinType: 'INNER',
          leftTableName: 'campaign_daily',
          leftColumn: 'campaign_id',
          rightTableName: 'campaign_budget',
          rightColumn: 'campaign_id',
        },
      ],
      graphNodes: [
        { tableName: 'campaign_daily', alias: 'campaign_daily', x: 140, y: 120 },
        { tableName: 'campaign_budget', alias: 'campaign_budget', x: 700, y: 220 },
      ],
    }).graph).toEqual({
      layoutMode: 'GRAPH_CANVAS',
      nodes: [
        { tableName: 'campaign_daily', alias: 'campaign_daily', x: 140, y: 120 },
        { tableName: 'campaign_dim', alias: 'campaign_dim', x: 360, y: 80 },
        { tableName: 'campaign_budget', alias: 'campaign_budget', x: 700, y: 220 },
      ],
    })
  })

  test('builds datasource edit command without overwriting a blank credential', () => {
    expect(buildDatasourceOnboardingCommand(
      {
        connectorType: 'DORIS',
        name: 'Doris Ads',
        url: 'jdbc:mysql://doris.example.com:9030/ads',
        username: 'doris_app',
        password: '   ',
        enabled: false,
      },
      [{
        connectorType: 'DORIS',
        label: 'Apache Doris',
        sourceCategory: 'JDBC',
        supportedModes: ['DIRECT_QUERY', 'CACHE'],
        supportStatus: 'AVAILABLE',
        supportsConnectionTest: true,
        supportsSchemaSync: true,
        supportsSqlDataset: true,
        supportsTableDataset: true,
        supportsCredentials: true,
        driverClassNames: ['com.mysql.cj.jdbc.Driver'],
        note: '',
      }],
    )).toMatchObject({
      connectorType: 'DORIS',
      password: '',
      driverClassName: 'com.mysql.cj.jdbc.Driver',
      enabled: false,
    })
  })

  test('derives stable query execution plan rows', () => {
    expect(queryExecutionPlanRows({
      datasetKey: 'canvas_daily_stats',
      sqlHash: 'abcdef123456',
      parametersCount: 2,
      steps: ['SCAN canvas_daily_stats', 'FILTER tenant_id = ?'],
    })).toEqual([
      { label: '数据集', value: 'canvas_daily_stats' },
      { label: 'SQL Hash', value: 'abcdef123456' },
      { label: '参数', value: '2 个' },
      { label: '执行计划', value: 'SCAN canvas_daily_stats\nFILTER tenant_id = ?' },
    ])
  })

  test('labels query cancellation status', () => {
    expect(queryCancellationStatusLabel({ sqlHash: 'abcdef123456', cancelled: true, message: 'cancellation requested' }))
      .toBe('abcdef123456 · 已请求取消 · cancellation requested')
    expect(queryCancellationStatusLabel({ sqlHash: 'missing', cancelled: false, message: 'not running' }))
      .toBe('missing · 未找到运行中查询 · not running')
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

  test('builds shared resource targets without datasource-only permission resources', () => {
    expect(buildBiResourceTargets({
      dashboardKey: 'canvas-effect',
      chartKey: 'trend-executions',
      datasetKey: 'canvas_daily_stats',
      dataSourceKey: 'jdbc-71',
      portalKey: 'executive-home',
      bigScreenKey: 'campaign-wall',
      spreadsheetKey: 'campaign-sheet',
    })).toEqual([
      {
        label: '当前仪表板',
        value: 'DASHBOARD',
        resourceType: 'DASHBOARD',
        resourceKey: 'canvas-effect',
        disabled: false,
      },
      {
        label: '选中图表',
        value: 'CHART',
        resourceType: 'CHART',
        resourceKey: 'trend-executions',
        disabled: false,
      },
      {
        label: '选中数据集',
        value: 'DATASET',
        resourceType: 'DATASET',
        resourceKey: 'canvas_daily_stats',
        disabled: false,
      },
      {
        label: '选中门户',
        value: 'PORTAL',
        resourceType: 'PORTAL',
        resourceKey: 'executive-home',
        disabled: false,
      },
      {
        label: '选中大屏',
        value: 'BIG_SCREEN',
        resourceType: 'BIG_SCREEN',
        resourceKey: 'campaign-wall',
        disabled: false,
      },
      {
        label: '选中电子表格',
        value: 'SPREADSHEET',
        resourceType: 'SPREADSHEET',
        resourceKey: 'campaign-sheet',
        disabled: false,
      },
    ])
  })

  test('marks unavailable shared resource targets disabled', () => {
    const targets = buildBiResourceTargets({
      dashboardKey: 'canvas-effect',
      chartKey: null,
      datasetKey: '',
      dataSourceKey: null,
      portalKey: undefined,
      bigScreenKey: null,
      spreadsheetKey: 'campaign-sheet',
    })

    expect(targets.map(target => `${target.value}:${target.disabled}`)).toEqual([
      'DASHBOARD:false',
      'CHART:true',
      'DATASET:true',
      'PORTAL:true',
      'BIG_SCREEN:true',
      'SPREADSHEET:false',
    ])
  })

  test('builds permission resource targets including datasource authorization', () => {
    expect(buildBiPermissionResourceTargets({
      dashboardKey: 'canvas-effect',
      chartKey: 'trend-executions',
      datasetKey: 'canvas_daily_stats',
      dataSourceKey: 'jdbc-71',
      portalKey: 'executive-home',
      bigScreenKey: 'campaign-wall',
      spreadsheetKey: 'campaign-sheet',
    }).map(target => `${target.value}:${target.resourceKey}:${target.disabled}`)).toEqual([
      'DASHBOARD:canvas-effect:false',
      'CHART:trend-executions:false',
      'DATASET:canvas_daily_stats:false',
      'DATASOURCE:jdbc-71:false',
      'PORTAL:executive-home:false',
      'BIG_SCREEN:campaign-wall:false',
      'SPREADSHEET:campaign-sheet:false',
    ])
  })

  test('marks unavailable datasource permission target disabled', () => {
    const targets = buildBiPermissionResourceTargets({
      dashboardKey: 'canvas-effect',
      chartKey: null,
      datasetKey: '',
      dataSourceKey: null,
      portalKey: undefined,
      bigScreenKey: null,
      spreadsheetKey: 'campaign-sheet',
    })

    expect(targets.map(target => `${target.value}:${target.disabled}`)).toEqual([
      'DASHBOARD:false',
      'CHART:true',
      'DATASET:true',
      'DATASOURCE:true',
      'PORTAL:true',
      'BIG_SCREEN:true',
      'SPREADSHEET:false',
    ])
  })

  test('builds big-screen and spreadsheet picker options', () => {
    expect(buildBigScreenResourceOptions([
      {
        screenKey: 'campaign-wall',
        name: 'Campaign Wall',
        status: 'PUBLISHED',
      },
      {
        screenKey: 'ops-wall',
        name: '',
        status: 'DRAFT',
      },
    ])).toEqual([
      {
        label: 'Campaign Wall · campaign-wall · PUBLISHED',
        value: 'campaign-wall',
        disabled: false,
      },
      {
        label: 'ops-wall · DRAFT',
        value: 'ops-wall',
        disabled: false,
      },
    ])

    expect(buildSpreadsheetResourceOptions([
      {
        spreadsheetKey: 'campaign-sheet',
        name: 'Campaign Sheet',
        status: 'DRAFT',
      },
      {
        spreadsheetKey: 'archived-sheet',
        name: 'Archived Sheet',
        status: 'ARCHIVED',
      },
    ])).toEqual([
      {
        label: 'Campaign Sheet · campaign-sheet · DRAFT',
        value: 'campaign-sheet',
        disabled: false,
      },
      {
        label: 'Archived Sheet · archived-sheet · ARCHIVED',
        value: 'archived-sheet',
        disabled: true,
      },
    ])
  })

  test('builds default big-screen and spreadsheet draft resources', () => {
    expect(buildBigScreenDraftResource({
      screenKey: ' campaign wall 2026 ',
      name: ' Campaign Launch Wall ',
      description: ' Launch monitor ',
      dashboardKey: 'canvas-effect',
      datasetKey: 'campaign_daily_stats',
    })).toEqual({
      screenKey: 'campaign-wall-2026',
      name: 'Campaign Launch Wall',
      description: 'Launch monitor',
      size: { width: 1920, height: 1080 },
      background: { type: 'SOLID', color: '#101820' },
      layout: [
        {
          widgetKey: 'canvas-effect-hero',
          title: 'Campaign Launch Wall',
          resourceType: 'DASHBOARD',
          resourceKey: 'canvas-effect',
          datasetKey: 'campaign_daily_stats',
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
    })

    expect(buildSpreadsheetDraftResource({
      spreadsheetKey: ' campaign sheet ',
      name: ' Campaign Planning ',
      description: '',
      datasetKey: 'campaign_daily_stats',
    })).toEqual({
      spreadsheetKey: 'campaign-sheet',
      name: 'Campaign Planning',
      description: null,
      sheets: [
        {
          sheetKey: 'summary',
          name: 'Summary',
          widgets: [
            {
              widgetKey: 'campaign-daily-stats-table',
              type: 'TABLE',
              datasetKey: 'campaign_daily_stats',
              range: 'A1:H20',
            },
          ],
        },
      ],
      dataBinding: { datasetKey: 'campaign_daily_stats', refreshMode: 'MANUAL' },
      style: { theme: 'default', density: 'compact' },
      status: 'DRAFT',
      version: 1,
      source: 'LOCAL',
    })
  })

  test('builds big-screen mobile layout variants from desktop layout', () => {
    const resource = {
      ...buildBigScreenDraftResource({
        screenKey: 'campaign-wall',
        name: 'Campaign Wall',
      }),
      layout: [
        { widgetKey: 'hero', title: 'Hero', x: 0, y: 0, w: 12, h: 4 },
        { widgetKey: 'detail', title: 'Detail', x: 12, y: 0, w: 12, h: 6 },
        { widgetKey: 'trend', title: 'Trend', x: 0, y: 5, w: 24, h: 5 },
      ],
    }

    const singleColumn = updateBigScreenMobileLayout(resource, 'single-column')
    const compact = updateBigScreenMobileLayout(resource, 'compact-grid')

    expect(singleColumn.mobileLayout).toEqual({
      variant: 'single-column',
      columns: 1,
      stack: true,
      items: [
        { widgetKey: 'hero', x: 0, y: 0, w: 1, h: 4 },
        { widgetKey: 'detail', x: 0, y: 4, w: 1, h: 6 },
        { widgetKey: 'trend', x: 0, y: 10, w: 1, h: 5 },
      ],
    })
    expect(compact.mobileLayout).toEqual({
      variant: 'compact-grid',
      columns: 2,
      stack: false,
      items: [
        { widgetKey: 'hero', x: 0, y: 0, w: 1, h: 4 },
        { widgetKey: 'detail', x: 1, y: 0, w: 1, h: 6 },
        { widgetKey: 'trend', x: 0, y: 6, w: 2, h: 5 },
      ],
    })
    expect(resource.mobileLayout).toEqual({ columns: 1, stack: true })
  })

  test('adds big-screen components from the component library immutably', () => {
    const resource = buildBigScreenDraftResource({
      screenKey: 'campaign-wall',
      name: 'Campaign Wall',
      dashboardKey: 'canvas-effect',
      datasetKey: 'campaign_daily_stats',
    })

    const withMetric = addBigScreenLibraryComponent(resource, 'metric-card')
    const withTrend = addBigScreenLibraryComponent(withMetric, 'trend-line')

    expect(resource.layout).toHaveLength(1)
    expect(withMetric.layout).toEqual([
      resource.layout[0],
      expect.objectContaining({
        widgetKey: 'campaign-wall-metric-card',
        title: '指标卡',
        componentType: 'METRIC_CARD',
        resourceType: 'DATASET',
        resourceKey: 'campaign_daily_stats',
        datasetKey: 'campaign_daily_stats',
        x: 0,
        y: 12,
        w: 6,
        h: 4,
      }),
    ])
    expect(withTrend.layout[2]).toEqual(expect.objectContaining({
      widgetKey: 'campaign-wall-trend-line',
      title: '趋势折线',
      componentType: 'TREND_LINE',
      x: 6,
      y: 12,
      w: 12,
      h: 5,
    }))
  })

  test('summarizes big-screen and spreadsheet editor resources', () => {
    expect(bigScreenResourceSummaryRows({
      screenKey: 'campaign-wall',
      name: 'Campaign Wall',
      description: 'Launch monitor',
      size: { width: 1920, height: 1080 },
      background: { type: 'SOLID', color: '#101820' },
      layout: [{ widgetKey: 'hero' }, { widgetKey: 'trend' }],
      refresh: { enabled: true, intervalSeconds: 60 },
      mobileLayout: { columns: 1 },
      status: 'PUBLISHED',
      version: 3,
      source: 'PERSISTED',
    })).toEqual([
      { label: '资源', value: 'Campaign Wall · campaign-wall' },
      { label: '状态', value: 'PUBLISHED · v3 · PERSISTED' },
      { label: '画布', value: '1920x1080 · 2 组件' },
      { label: '刷新', value: '启用 · 60 秒' },
      { label: '背景', value: 'SOLID · #101820' },
    ])

    expect(spreadsheetResourceSummaryRows({
      spreadsheetKey: 'campaign-sheet',
      name: 'Campaign Sheet',
      description: 'Planning workbook',
      sheets: [{ sheetKey: 'summary' }, { sheetKey: 'detail' }],
      dataBinding: { datasetKey: 'campaign_daily_stats', refreshMode: 'MANUAL' },
      style: { theme: 'default', density: 'compact' },
      status: 'DRAFT',
      version: 2,
      source: 'PERSISTED',
    })).toEqual([
      { label: '资源', value: 'Campaign Sheet · campaign-sheet' },
      { label: '状态', value: 'DRAFT · v2 · PERSISTED' },
      { label: '工作表', value: '2 张 · campaign_daily_stats' },
      { label: '刷新', value: 'MANUAL' },
      { label: '样式', value: 'default · compact' },
    ])
  })

  test('updates big-screen layout items and spreadsheet cells immutably', () => {
    const bigScreen = buildBigScreenDraftResource({
      screenKey: 'campaign-wall',
      name: 'Campaign Wall',
      dashboardKey: 'canvas-effect',
      datasetKey: 'campaign_daily_stats',
    })

    const editedBigScreen = updateBigScreenLayoutItem(bigScreen, 'canvas-effect-hero', {
      title: 'Executive KPI',
      resourceType: 'chart',
      resourceKey: 'chart-kpi',
      x: '25',
      y: '3',
      w: '6',
      h: '4',
    })

    expect(bigScreen.layout[0]).toMatchObject({ title: 'Campaign Wall', x: 0, y: 0, w: 24, h: 12 })
    expect(editedBigScreen.layout[0]).toEqual(expect.objectContaining({
      widgetKey: 'canvas-effect-hero',
      title: 'Executive KPI',
      resourceType: 'CHART',
      resourceKey: 'chart-kpi',
      x: 23,
      y: 3,
      w: 6,
      h: 4,
    }))

    const movedBigScreen = moveBigScreenLayoutItem(editedBigScreen, 'canvas-effect-hero', 'left')
    const resizedBigScreen = resizeBigScreenLayoutItem(movedBigScreen, 'canvas-effect-hero', 'right')

    expect(movedBigScreen.layout[0]).toEqual(expect.objectContaining({
      x: 22,
      y: 3,
      w: 6,
      h: 4,
    }))
    expect(resizedBigScreen.layout[0]).toEqual(expect.objectContaining({
      x: 22,
      y: 3,
      w: 2,
      h: 4,
    }))
    expect(editedBigScreen.layout[0]).toEqual(expect.objectContaining({
      x: 23,
      w: 6,
    }))

    const alignedBigScreen = alignBigScreenLayoutItems({
      ...editedBigScreen,
      layout: [
        editedBigScreen.layout[0],
        { widgetKey: 'detail', title: 'Detail', x: 4, y: 9, w: 8, h: 5 },
      ],
    }, ['canvas-effect-hero', 'detail'], 'left')

    expect(alignedBigScreen.layout).toEqual([
      expect.objectContaining({ widgetKey: 'canvas-effect-hero', x: 4, y: 3 }),
      expect.objectContaining({ widgetKey: 'detail', x: 4, y: 9 }),
    ])

    const snappedBigScreen = snapBigScreenLayoutItem({
      ...editedBigScreen,
      layout: [
        { widgetKey: 'hero', title: 'Hero', x: 0, y: 0, w: 8, h: 4 },
        { widgetKey: 'detail', title: 'Detail', x: 9, y: 5, w: 8, h: 5 },
      ],
    }, 'detail', 1)

    expect(snappedBigScreen.resource.layout).toEqual([
      expect.objectContaining({ widgetKey: 'hero', x: 0, y: 0 }),
      expect.objectContaining({ widgetKey: 'detail', x: 8, y: 4 }),
    ])
    expect(snappedBigScreen.guides).toEqual([
      { orientation: 'vertical', position: 8, widgetKey: 'hero', edge: 'right' },
      { orientation: 'horizontal', position: 4, widgetKey: 'hero', edge: 'bottom' },
    ])

    const spreadsheet = buildSpreadsheetDraftResource({
      spreadsheetKey: 'campaign-sheet',
      name: 'Campaign Sheet',
      datasetKey: 'campaign_daily_stats',
    })
    const editedSpreadsheet = updateSpreadsheetCell(spreadsheet, 'summary', 'b2', '=SUM(B3:B8)')
    const clearedSpreadsheet = updateSpreadsheetCell(editedSpreadsheet, 'summary', 'B2', '')

    expect(spreadsheet.sheets[0]).not.toHaveProperty('cells')
    expect(editedSpreadsheet.sheets[0].cells).toEqual({ B2: '=SUM(B3:B8)' })
    expect(clearedSpreadsheet.sheets[0].cells).toEqual({})
  })

  test('fills spreadsheet cell ranges immutably', () => {
    const spreadsheet = buildSpreadsheetDraftResource({
      spreadsheetKey: 'campaign-sheet',
      name: 'Campaign Sheet',
    })

    const filled = updateSpreadsheetCellRange(spreadsheet, 'summary', 'b2:c3', 'Ready')

    expect(filled.sheets[0].cells).toEqual({
      B2: 'Ready',
      B3: 'Ready',
      C2: 'Ready',
      C3: 'Ready',
    })
    expect(spreadsheet.sheets[0]).not.toHaveProperty('cells')
  })

  test('builds spreadsheet pivot tables from source cell ranges', () => {
    const spreadsheet = buildSpreadsheetDraftResource({
      spreadsheetKey: 'campaign-sheet',
      name: 'Campaign Sheet',
    })
    const source = updateSpreadsheetCellRange(spreadsheet, 'summary', 'A1:D1', '')
    const withRows: ReturnType<typeof buildSpreadsheetDraftResource> = {
      ...source,
      sheets: [{
        ...source.sheets[0],
        cells: {
          A1: '地区',
          B1: '渠道',
          C1: '消耗',
          D1: '转化',
          A2: '华东',
          B2: '搜索',
          C2: 100,
          D2: 4,
          A3: '华东',
          B3: '信息流',
          C3: 60,
          D3: 2,
          A4: '华南',
          B4: '搜索',
          C4: 40,
          D4: 1,
          A5: '华东',
          B5: '搜索',
          C5: 30,
          D5: 1,
        },
      }],
    }

    const pivoted = buildSpreadsheetPivotTable(withRows, 'summary', {
      sourceRange: 'A1:D5',
      targetCell: 'F1',
      rowField: '地区',
      columnField: '渠道',
      valueField: '消耗',
      aggregation: 'SUM',
    })

    expect(withRows.sheets[0]).not.toHaveProperty('pivotTables')
    expect((pivoted.sheets[0] as Record<string, unknown>).pivotTables).toEqual([{
      pivotKey: 'pivot-summary-f1',
      sourceRange: 'A1:D5',
      targetCell: 'F1',
      rowField: '地区',
      columnField: '渠道',
      valueField: '消耗',
      aggregation: 'SUM',
      rowLabels: ['华东', '华南'],
      columnLabels: ['搜索', '信息流'],
    }])
    expect(pivoted.sheets[0].cells).toEqual(expect.objectContaining({
      F1: '地区 / 渠道',
      G1: '搜索',
      H1: '信息流',
      F2: '华东',
      G2: 130,
      H2: 60,
      F3: '华南',
      G3: 40,
      H3: 0,
    }))
  })

  test('updates spreadsheet cell styles immutably', () => {
    const spreadsheet = buildSpreadsheetDraftResource({
      spreadsheetKey: 'campaign-sheet',
      name: 'Campaign Sheet',
      datasetKey: 'canvas_daily_stats',
    })

    const styled = updateSpreadsheetCellStyle(spreadsheet, 'summary', 'b2', {
      bold: true,
      backgroundColor: '#FEF3C7',
      textColor: '#111827',
    })
    const cleared = updateSpreadsheetCellStyle(styled, 'summary', 'B2', {
      bold: false,
      backgroundColor: '',
      textColor: '',
    })

    expect(spreadsheet.sheets[0]).not.toHaveProperty('cellStyles')
    expect(styled.sheets[0].cellStyles).toEqual({
      B2: {
        bold: true,
        backgroundColor: '#FEF3C7',
        textColor: '#111827',
      },
    })
    expect(cleared.sheets[0].cellStyles).toEqual({})
  })

  test('evaluates spreadsheet formulas across referenced cells', () => {
    expect(evaluateSpreadsheetCells({
      A1: '2',
      A2: 3,
      A3: '=SUM(A1:A2)',
      B1: '=A3',
      C1: '=C2',
      C2: '=C1',
    })).toEqual(expect.objectContaining({
      A1: '2',
      A2: 3,
      A3: 5,
      B1: 5,
      C1: '#CYCLE!',
      C2: '#CYCLE!',
    }))
  })

  test('evaluates common spreadsheet aggregate functions', () => {
    expect(evaluateSpreadsheetCells({
      A1: 2,
      A2: '4',
      A3: 'ignore',
      B1: '=AVERAGE(A1:A3)',
      B2: '=MIN(A1:A3)',
      B3: '=MAX(A1:A3)',
      B4: '=COUNT(A1:A3)',
      B5: '=sum(A1:A3)',
    })).toEqual(expect.objectContaining({
      B1: 2,
      B2: 0,
      B3: 4,
      B4: 2,
      B5: 6,
    }))
  })

  test('upserts active big-screen and spreadsheet resources and removes archived ones', () => {
    const bigScreens = [
      { screenKey: 'campaign-wall', name: 'Old', status: 'DRAFT' },
      { screenKey: 'ops-wall', name: 'Ops', status: 'PUBLISHED' },
    ]
    expect(upsertBigScreenResource(bigScreens, { screenKey: 'campaign-wall', name: 'New', status: 'PUBLISHED' })).toEqual([
      { screenKey: 'campaign-wall', name: 'New', status: 'PUBLISHED' },
      { screenKey: 'ops-wall', name: 'Ops', status: 'PUBLISHED' },
    ])
    expect(upsertBigScreenResource(bigScreens, { screenKey: 'new-wall', name: 'New Wall', status: 'DRAFT' })).toEqual([
      { screenKey: 'new-wall', name: 'New Wall', status: 'DRAFT' },
      { screenKey: 'campaign-wall', name: 'Old', status: 'DRAFT' },
      { screenKey: 'ops-wall', name: 'Ops', status: 'PUBLISHED' },
    ])
    expect(upsertBigScreenResource(bigScreens, { screenKey: 'campaign-wall', name: 'Old', status: 'ARCHIVED' })).toEqual([
      { screenKey: 'ops-wall', name: 'Ops', status: 'PUBLISHED' },
    ])

    const spreadsheets = [
      { spreadsheetKey: 'campaign-sheet', name: 'Old', status: 'DRAFT' },
      { spreadsheetKey: 'ops-sheet', name: 'Ops', status: 'PUBLISHED' },
    ]
    expect(upsertSpreadsheetResource(spreadsheets, { spreadsheetKey: 'campaign-sheet', name: 'New', status: 'PUBLISHED' })).toEqual([
      { spreadsheetKey: 'campaign-sheet', name: 'New', status: 'PUBLISHED' },
      { spreadsheetKey: 'ops-sheet', name: 'Ops', status: 'PUBLISHED' },
    ])
    expect(upsertSpreadsheetResource(spreadsheets, { spreadsheetKey: 'new-sheet', name: 'New Sheet', status: 'DRAFT' })).toEqual([
      { spreadsheetKey: 'new-sheet', name: 'New Sheet', status: 'DRAFT' },
      { spreadsheetKey: 'campaign-sheet', name: 'Old', status: 'DRAFT' },
      { spreadsheetKey: 'ops-sheet', name: 'Ops', status: 'PUBLISHED' },
    ])
    expect(upsertSpreadsheetResource(spreadsheets, { spreadsheetKey: 'campaign-sheet', name: 'Old', status: 'ARCHIVED' })).toEqual([
      { spreadsheetKey: 'ops-sheet', name: 'Ops', status: 'PUBLISHED' },
    ])
  })

  test('parses typed BI runtime routes and selects resources by id', () => {
    const bigScreenRoute = biRuntimeRouteFromSearchParams(new URLSearchParams('resourceType=BIG_SCREEN&resourceId=51&mode=big-screen'))
    expect(bigScreenRoute).toEqual({
      mode: 'big-screen',
      resourceType: 'BIG_SCREEN',
      resourceId: 51,
      resourceKey: null,
    })
    expect(selectBigScreenRuntimeResource([
      { id: 50, screenKey: 'ops-wall', name: 'Ops', status: 'PUBLISHED' },
      { id: 51, screenKey: 'campaign-wall', name: 'Campaign', status: 'PUBLISHED' },
    ], bigScreenRoute)).toEqual({
      id: 51,
      screenKey: 'campaign-wall',
      name: 'Campaign',
      status: 'PUBLISHED',
    })

    const spreadsheetRoute = biRuntimeRouteFromSearchParams(new URLSearchParams('resourceType=SPREADSHEET&resourceKey=budget-sheet&mode=spreadsheet'))
    expect(spreadsheetRoute).toEqual({
      mode: 'spreadsheet',
      resourceType: 'SPREADSHEET',
      resourceId: null,
      resourceKey: 'budget-sheet',
    })
    expect(selectSpreadsheetRuntimeResource([
      { id: 60, spreadsheetKey: 'campaign-sheet', name: 'Campaign', status: 'PUBLISHED' },
      { id: 61, spreadsheetKey: 'budget-sheet', name: 'Budget', status: 'PUBLISHED' },
    ], spreadsheetRoute)).toEqual({
      id: 61,
      spreadsheetKey: 'budget-sheet',
      name: 'Budget',
      status: 'PUBLISHED',
    })
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

  test('updates portal navigation config and menu order immutably', () => {
    const portal = {
      portalKey: 'executive-home',
      name: 'Executive Home',
      theme: { theme: 'light' },
      menus: [
        { menuKey: 'overview', title: 'Overview', resourceType: 'DASHBOARD', resourceKey: 'canvas-effect', sortOrder: 1 },
        { menuKey: 'sales', title: 'Sales', resourceType: 'DASHBOARD', resourceKey: 'sales-dashboard', sortOrder: 2 },
        { menuKey: 'docs', title: 'Docs', resourceType: 'EXTERNAL_LINK', externalUrl: 'https://example.test', sortOrder: 3 },
      ],
      status: 'DRAFT',
      source: 'PERSISTED',
    }

    const configured = updatePortalNavigationConfig(portal, {
      navigationLayout: 'dual',
      defaultMenuKey: 'sales',
      menuSearchEnabled: true,
      fullScreenEnabled: true,
      mobileEnabled: true,
    })
    const reordered = movePortalMenuItem(configured, 'sales', 'up')

    expect(portal.theme).toEqual({ theme: 'light' })
    expect(configured.theme).toEqual(expect.objectContaining({
      theme: 'light',
      navigationLayout: 'dual',
      defaultMenuKey: 'sales',
      menuSearchEnabled: true,
      fullScreenEnabled: true,
      mobileEnabled: true,
    }))
    expect(reordered.menus.map(menu => `${menu.menuKey}:${menu.sortOrder}`)).toEqual([
      'sales:1',
      'overview:2',
      'docs:3',
    ])
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

  test('detects self-service export jobs that can be canceled', () => {
    expect(isCancelableExportJob({ status: 'QUEUED' })).toBe(true)
    expect(isCancelableExportJob({ status: 'PENDING_APPROVAL' })).toBe(true)
    expect(isCancelableExportJob({ status: 'FAILED' })).toBe(true)
    expect(isCancelableExportJob({ status: 'COMPLETED' })).toBe(false)
    expect(isCancelableExportJob({ status: 'EXPIRED' })).toBe(false)
    expect(isCancelableExportJob({ status: 'CANCELED' })).toBe(false)
    expect(isCancelableExportJob(null)).toBe(false)
  })

  test('builds permission request and review commands', () => {
    expect(buildPermissionRequestCommand('dashboard', ' canvas-effect ', ' export ', '  下载周报数据  ')).toEqual({
      resourceType: 'DASHBOARD',
      resourceKey: 'canvas-effect',
      requestedAction: 'EXPORT',
      reason: '下载周报数据',
    })
    expect(buildPermissionRequestReviewCommand('approved', ' 已复核 ', 31)).toEqual({
      requestId: 31,
      status: 'APPROVED',
      reviewComment: '已复核',
    })
  })

  test('builds self-service export audit detail rows from job and request', () => {
    const rows = exportAuditDetailRows({
      job: {
        id: 76,
        resourceKey: 'canvas_daily_stats',
        exportFormat: 'PDF',
        status: 'COMPLETED',
        progressPercent: 100,
        storageProvider: 'S3',
        storageKey: 'exports/tenant-7/export-76.pdf',
        downloadCount: 3,
        lastDownloadedAt: '2026-06-05T10:15:00',
        approvalStatus: 'APPROVED',
        requestedBy: 'alice',
        reviewedBy: 'admin',
        retryCount: 1,
        maxRetryCount: 3,
        createdBy: 'alice',
        createdAt: '2026-06-05T09:55:00',
      },
      request: {
        exportFormat: 'PDF',
        rowLimit: 250,
        approvalRequired: true,
        sensitive: true,
        approvalReason: 'finance audit',
        query: {
          datasetKey: 'canvas_daily_stats',
          dimensions: ['stat_date'],
          metrics: ['total_executions'],
          filters: [],
          sorts: [],
          limit: 250,
        },
      },
    })

    expect(rows).toContainEqual({ label: '任务', value: '#76 · COMPLETED · PDF' })
    expect(rows).toContainEqual({ label: '数据集', value: 'canvas_daily_stats' })
    expect(rows).toContainEqual({ label: '字段', value: 'stat_date / total_executions' })
    expect(rows).toContainEqual({ label: '审批', value: '已通过 · alice -> admin · finance audit' })
    expect(rows).toContainEqual({ label: '存储', value: 'S3 · exports/tenant-7/export-76.pdf' })
    expect(rows).toContainEqual({ label: '下载', value: '3 次 · 2026-06-05 10:15' })
    expect(rows).toContainEqual({ label: '重试', value: '1/3' })
  })
})
