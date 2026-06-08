import { beforeEach, describe, expect, it, vi } from 'vitest'
import http from './api'
import { biApi, type BiBigScreenResource, type BiDatasetResource, type BiDatasourceApiPreviewRequest, type BiDatasourceOnboardingCommand, type BiSpreadsheetResource } from './biApi'

vi.mock('./api', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    delete: vi.fn(),
  },
}))

const mockedHttp = vi.mocked(http)

describe('biApi', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mockedHttp.get.mockResolvedValue({ data: [] })
    mockedHttp.post.mockResolvedValue({ data: {} })
    mockedHttp.put.mockResolvedValue({ data: {} })
    mockedHttp.delete.mockResolvedValue({ data: {} })
  })

  it('calls big-screen lifecycle endpoints', async () => {
    const resource: BiBigScreenResource = {
      screenKey: 'campaign-wall',
      name: 'Campaign Wall',
      description: 'Executive launch monitor',
      size: { width: 1920, height: 1080 },
      background: { color: '#101820' },
      layout: [{ widgetKey: 'revenue-kpi' }],
      refresh: { intervalSeconds: 60 },
      mobileLayout: {},
      status: 'DRAFT',
      version: 1,
      source: 'PERSISTED',
    }

    await biApi.getBigScreenResource('campaign-wall')
    await biApi.saveBigScreenDraft('campaign-wall', resource, 'lock-1')
    await biApi.publishBigScreen('campaign-wall')
    await biApi.archiveBigScreen('campaign-wall')
    await biApi.listBigScreenVersions('campaign-wall', 5)
    await biApi.restoreBigScreenVersion('campaign-wall', 3, 'lock-2')

    expect(mockedHttp.get).toHaveBeenCalledWith('/canvas/bi/big-screens/resources/campaign-wall')
    expect(mockedHttp.post).toHaveBeenCalledWith(
      '/canvas/bi/big-screens/resources/campaign-wall/draft',
      resource,
      { headers: { 'X-BI-LOCK-TOKEN': 'lock-1' } },
    )
    expect(mockedHttp.post).toHaveBeenCalledWith('/canvas/bi/big-screens/resources/campaign-wall/publish', {})
    expect(mockedHttp.delete).toHaveBeenCalledWith('/canvas/bi/big-screens/resources/campaign-wall')
    expect(mockedHttp.get).toHaveBeenCalledWith('/canvas/bi/big-screens/resources/campaign-wall/versions?limit=5')
    expect(mockedHttp.post).toHaveBeenCalledWith(
      '/canvas/bi/big-screens/resources/campaign-wall/versions/3/restore',
      {},
      { headers: { 'X-BI-LOCK-TOKEN': 'lock-2' } },
    )
  })

  it('calls spreadsheet lifecycle endpoints', async () => {
    const resource: BiSpreadsheetResource = {
      spreadsheetKey: 'campaign-sheet',
      name: 'Campaign Sheet',
      description: 'Planning workbook',
      sheets: [{ sheetKey: 'summary', name: 'Summary' }],
      dataBinding: { datasetKey: 'campaign_daily_stats' },
      style: { theme: 'light' },
      status: 'DRAFT',
      version: 1,
      source: 'PERSISTED',
    }

    await biApi.getSpreadsheetResource('campaign-sheet')
    await biApi.saveSpreadsheetDraft('campaign-sheet', resource, null)
    await biApi.publishSpreadsheet('campaign-sheet')
    await biApi.archiveSpreadsheet('campaign-sheet')
    await biApi.listSpreadsheetVersions('campaign-sheet')
    await biApi.restoreSpreadsheetVersion('campaign-sheet', 2)

    expect(mockedHttp.get).toHaveBeenCalledWith('/canvas/bi/spreadsheets/resources/campaign-sheet')
    expect(mockedHttp.post).toHaveBeenCalledWith(
      '/canvas/bi/spreadsheets/resources/campaign-sheet/draft',
      resource,
      undefined,
    )
    expect(mockedHttp.post).toHaveBeenCalledWith('/canvas/bi/spreadsheets/resources/campaign-sheet/publish', {})
    expect(mockedHttp.delete).toHaveBeenCalledWith('/canvas/bi/spreadsheets/resources/campaign-sheet')
    expect(mockedHttp.get).toHaveBeenCalledWith('/canvas/bi/spreadsheets/resources/campaign-sheet/versions?limit=20')
    expect(mockedHttp.post).toHaveBeenCalledWith(
      '/canvas/bi/spreadsheets/resources/campaign-sheet/versions/2/restore',
      {},
      undefined,
    )
  })

  it('loads chart reference impact from chart resource endpoint', async () => {
    await biApi.getChartReferenceImpact('trend-executions')

    expect(mockedHttp.get).toHaveBeenCalledWith('/canvas/bi/charts/resources/trend-executions/impact')
  })

  it('calls datasource credential rotation endpoint without exposing credentials in the path', async () => {
    await biApi.rotateDatasourceCredential(11, { password: 'rotated-password' })

    expect(mockedHttp.post).toHaveBeenCalledWith(
      '/canvas/bi/datasources/11/credential-rotation',
      { password: 'rotated-password' },
    )
  })

  it('calls datasource onboarding create and update endpoints', async () => {
    const command: BiDatasourceOnboardingCommand = {
      connectorType: 'MYSQL',
      name: 'Marketing Warehouse',
      url: 'jdbc:mysql://warehouse.example.com:3306/marketing',
      username: 'canvas_app',
      password: 'plain-password',
      driverClassName: 'com.mysql.cj.jdbc.Driver',
      description: 'QuickBI datasource center',
      enabled: true,
      connectionMode: 'DIRECT_QUERY',
      connectorConfig: {
        requestMethod: 'GET',
        authType: 'NONE',
        headers: [],
        parameters: [],
        responseRowsPath: '$',
        responseFormat: 'JSON',
      },
    }

    await biApi.createDatasourceOnboarding(command)
    await biApi.updateDatasourceOnboarding(21, { ...command, password: '' })

    expect(mockedHttp.post).toHaveBeenCalledWith('/canvas/bi/datasources/onboarding', command)
    expect(mockedHttp.put).toHaveBeenCalledWith('/canvas/bi/datasources/onboarding/21', { ...command, password: '' })
  })

  it('executes signed embed queries through the anonymous ticket-bound endpoint', async () => {
    const request = {
      ticket: 'ticket-1',
      resourceType: 'DASHBOARD',
      resourceKey: 'canvas-effect',
      widgetKey: 'kpi-total',
      query: {
        datasetKey: 'canvas_daily_stats',
        dashboardKey: 'canvas-effect',
        dimensions: [],
        metrics: ['total_executions'],
        filters: [],
        sorts: [],
        limit: 500,
      },
    }

    await biApi.executeEmbedQuery(request)

    expect(mockedHttp.post).toHaveBeenCalledWith('/canvas/bi/embed/query/execute', request)
  })

  it('loads embedded dashboard metadata through the anonymous ticket-bound endpoint', async () => {
    const request = {
      ticket: 'ticket-1',
      resourceType: 'DASHBOARD',
      resourceKey: 'canvas-effect',
    }

    await biApi.getEmbedDashboardResource(request)

    expect(mockedHttp.post).toHaveBeenCalledWith('/canvas/bi/embed/resources/dashboard', request)
  })

  it('loads embedded dashboard runtime state through the anonymous ticket-bound endpoint', async () => {
    const request = {
      ticket: 'ticket-1',
      resourceType: 'DASHBOARD',
      resourceKey: 'canvas-effect',
    }

    await biApi.getEmbedDashboardRuntimeState(request)

    expect(mockedHttp.post).toHaveBeenCalledWith('/canvas/bi/embed/resources/dashboard/runtime-state', request)
  })

  it('loads embedded portal metadata through the anonymous ticket-bound endpoint', async () => {
    const request = {
      ticket: 'ticket-portal',
      resourceType: 'PORTAL',
      resourceKey: 'executive-home',
    }

    await biApi.getEmbedPortalResource(request)

    expect(mockedHttp.post).toHaveBeenCalledWith('/canvas/bi/embed/resources/portal', request)
  })

  it('uploads datasource files through multipart transport', async () => {
    const file = new File(['order_id,amount\nO-1,12.5\n'], 'orders.csv', { type: 'text/csv' })

    await biApi.uploadDatasourceFile(file, {
      name: 'Uploaded Orders',
      description: 'Browser CSV upload',
      delimiter: ',',
      headerRow: true,
      encoding: 'UTF-8',
    })

    expect(mockedHttp.post).toHaveBeenCalledWith(
      '/canvas/bi/datasources/file-upload?name=Uploaded+Orders&description=Browser+CSV+upload&delimiter=%2C&headerRow=true&encoding=UTF-8',
      expect.any(FormData),
    )
    const calls = mockedHttp.post.mock.calls
    const form = calls[calls.length - 1]?.[1] as FormData
    expect(form.get('file')).toBe(file)
  })

  it('uploads and materializes datasource files through multipart transport', async () => {
    const file = new File(['order_id,amount\nO-1,12.5\n'], 'orders.csv', { type: 'text/csv' })

    await biApi.uploadAndMaterializeDatasourceFile(file, {
      name: 'Uploaded Orders',
      description: 'Browser CSV upload',
      delimiter: ',',
      headerRow: true,
      encoding: 'UTF-8',
      datasetKey: 'file_91_orders',
      datasetName: 'Uploaded Orders Dataset',
      tenantColumn: 'tenant_id',
      schemaLimit: 200,
      maxRows: 5000,
    })

    expect(mockedHttp.post).toHaveBeenCalledWith(
      '/canvas/bi/datasources/file-upload/materialize?name=Uploaded+Orders&description=Browser+CSV+upload&delimiter=%2C&headerRow=true&encoding=UTF-8&datasetKey=file_91_orders&datasetName=Uploaded+Orders+Dataset&tenantColumn=tenant_id&schemaLimit=200&maxRows=5000',
      expect.any(FormData),
    )
    const calls = mockedHttp.post.mock.calls
    const form = calls[calls.length - 1]?.[1] as FormData
    expect(form.get('file')).toBe(file)
  })

  it('calls SQL dataset preview endpoint with draft resource and parameters', async () => {
    const resource: BiDatasetResource = {
      datasetKey: 'campaign_sql',
      name: 'Campaign SQL',
      datasetType: 'SQL',
      tableExpression: 'SELECT tenant_id, stat_date FROM campaign_daily WHERE stat_date >= {{start_date}}',
      tenantColumn: 'tenant_id',
      model: {
        sqlApprovalRequired: true,
        sqlTemplate: 'SELECT tenant_id, stat_date FROM campaign_daily WHERE stat_date >= {{start_date}}',
        sqlParameterOrder: ['start_date'],
        sqlParameters: [{ key: 'start_date', dataType: 'DATE', required: true, allowedValues: [] }],
      },
      fields: [{
        fieldKey: 'stat_date',
        displayName: 'Date',
        columnExpression: 'stat_date',
        role: 'DIMENSION',
        dataType: 'DATE',
        visible: true,
        sensitiveLevel: 'NORMAL',
        sortOrder: 10,
      }],
      metrics: [{
        metricKey: 'row_count',
        displayName: 'Rows',
        expression: 'COUNT(*)',
        aggregation: 'COUNT',
        dataType: 'NUMBER',
        allowedDimensions: ['stat_date'],
        status: 'ACTIVE',
      }],
      status: 'DRAFT',
      source: 'CLIENT',
    }

    await biApi.previewSqlDataset({
      resource,
      sqlParameters: { start_date: '2026-06-01' },
      limit: 20,
      executeSample: true,
    })

    expect(mockedHttp.post).toHaveBeenCalledWith(
      '/canvas/bi/datasets/resources/sql-preview',
      {
        resource,
        sqlParameters: { start_date: '2026-06-01' },
        limit: 20,
        executeSample: true,
      },
    )
  })

  it('calls API datasource runtime preview endpoint', async () => {
    const request: BiDatasourceApiPreviewRequest = {
      variables: {
        tenantId: 't-7',
      },
      limit: 50,
    }

    await biApi.previewApiDatasource(81, request)

    expect(mockedHttp.post).toHaveBeenCalledWith('/canvas/bi/datasources/81/api-preview', request)
  })

  it('passes API runtime variables to datasource schema sync', async () => {
    const request: BiDatasourceApiPreviewRequest = {
      variables: {
        campaignId: 'cmp-1',
      },
      limit: 20,
    }

    await biApi.syncDatasourceSchema(81, 100, request)

    expect(mockedHttp.post).toHaveBeenCalledWith('/canvas/bi/datasources/81/schema-sync?limit=100', request)
  })

  it('calls dataset acceleration policy and extract refresh endpoints', async () => {
    const command = {
      enabled: true,
      accelerationMode: 'EXTRACT',
      refreshMode: 'SCHEDULED',
      refreshIntervalMinutes: 30,
      ttlSeconds: 900,
      maxRows: 500000,
      cronExpression: '0 0/30 * * * ?',
    }

    await biApi.getDatasetAccelerationPolicy('canvas_daily_stats')
    await biApi.updateDatasetAccelerationPolicy('canvas_daily_stats', command)
    await biApi.refreshDatasetAcceleration('canvas_daily_stats')
    await biApi.listDatasetAccelerationRuns('canvas_daily_stats', 5)
    await biApi.runDatasetAccelerationScheduler()

    expect(mockedHttp.get).toHaveBeenCalledWith('/canvas/bi/datasets/resources/canvas_daily_stats/acceleration-policy')
    expect(mockedHttp.post).toHaveBeenCalledWith(
      '/canvas/bi/datasets/resources/canvas_daily_stats/acceleration-policy',
      command,
    )
    expect(mockedHttp.post).toHaveBeenCalledWith(
      '/canvas/bi/datasets/resources/canvas_daily_stats/acceleration-refresh',
      {},
    )
    expect(mockedHttp.get).toHaveBeenCalledWith('/canvas/bi/datasets/resources/canvas_daily_stats/acceleration-runs?limit=5')
    expect(mockedHttp.post).toHaveBeenCalledWith(
      '/canvas/bi/datasets/resources/acceleration-scheduler/run',
      {},
    )
  })

  it('calls embed ticket cleanup endpoint', async () => {
    await biApi.cleanupEmbedTickets(50)

    expect(mockedHttp.post).toHaveBeenCalledWith('/canvas/bi/embed-tickets/cleanup?limit=50', {})
  })

  it('calls self-service export cancel endpoint', async () => {
    await biApi.cancelExport(55)

    expect(mockedHttp.post).toHaveBeenCalledWith('/canvas/bi/self-service/exports/55/cancel', {})
  })

  it('calls query cache stats endpoint', async () => {
    await biApi.getQueryCacheStats()

    expect(mockedHttp.get).toHaveBeenCalledWith('/canvas/bi/query/cache-stats')
  })

  it('calls Quick Engine capacity governance endpoints', async () => {
    const command = {
      enabled: true,
      capacityLimitRows: 500000,
      warningThresholdPercent: 75,
      criticalThresholdPercent: 95,
      notificationChannels: ['LARK', 'EMAIL'],
      notificationReceivers: ['bi-ops', 'alice'],
    }
    const poolCommand = {
      poolKey: 'GOLD',
      maxConcurrentQueries: 16,
      queueLimit: 120,
      queueTimeoutSeconds: 300,
      poolWeight: 200,
    }

    await biApi.getQuickEngineCapacity(20)
    await biApi.upsertQuickEngineCapacityAlertPolicy(command)
    await biApi.upsertQuickEngineTenantPoolPolicy(poolCommand)

    expect(mockedHttp.get).toHaveBeenCalledWith('/canvas/bi/capacity/quick-engine?limit=20')
    expect(mockedHttp.post).toHaveBeenCalledWith('/canvas/bi/capacity/quick-engine/alert-policy', command)
    expect(mockedHttp.post).toHaveBeenCalledWith('/canvas/bi/capacity/quick-engine/tenant-pool-policy', poolCommand)
  })

  it('calls BI permission request lifecycle endpoints', async () => {
    const command = {
      resourceType: 'DASHBOARD',
      resourceKey: 'canvas-effect',
      requestedAction: 'EXPORT',
      reason: 'download weekly report data',
    }
    const review = {
      requestId: 31,
      status: 'APPROVED',
      reviewComment: 'approved',
    }

    await biApi.listPermissionRequests({ resourceType: 'DASHBOARD', resourceKey: 'canvas-effect', status: 'PENDING' })
    await biApi.requestPermission(command)
    await biApi.reviewPermissionRequest(31, review)

    expect(mockedHttp.get).toHaveBeenCalledWith(
      '/canvas/bi/permissions/requests?resourceType=DASHBOARD&resourceKey=canvas-effect&status=PENDING',
    )
    expect(mockedHttp.post).toHaveBeenCalledWith('/canvas/bi/permissions/requests', command)
    expect(mockedHttp.post).toHaveBeenCalledWith('/canvas/bi/permissions/requests/31/review', review)
  })
})
