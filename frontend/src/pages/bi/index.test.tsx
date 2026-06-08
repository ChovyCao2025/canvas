/* @vitest-environment jsdom */
import { fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { MemoryRouter, Route, Routes } from 'react-router-dom'

import BiWorkbenchPage from './index'
import { biApi } from '../../services/biApi'

const biApiMock = vi.hoisted(() => {
  const mocks: Record<string, ReturnType<typeof vi.fn>> = {}
  return new Proxy(mocks, {
    get(target, property: string) {
      if (!target[property]) {
        target[property] = vi.fn(() => Promise.resolve({ code: 0, message: 'success', data: [] }))
      }
      return target[property]
    },
  })
})

vi.mock('../../services/biApi', async importOriginal => {
  const actual = await importOriginal<typeof import('../../services/biApi')>()
  return {
    ...actual,
    biApi: biApiMock,
  }
})

const ok = <T,>(data: T) => ({ code: 0, message: 'success', data })

describe('BiWorkbenchPage runtime routes', () => {
  beforeEach(() => {
    Object.values(biApiMock).forEach(mock => {
      mock.mockReset()
      mock.mockResolvedValue(ok([]))
    })
    vi.mocked(biApi.executeQuery).mockResolvedValue(ok({
      datasetKey: 'canvas_daily_stats',
      columns: [],
      rows: [],
      rowCount: 0,
      durationMs: 0,
      sqlHash: 'test-sql',
      cached: false,
    }))
    vi.mocked(biApi.getDashboardResource).mockResolvedValue(ok(null as never))
    vi.mocked(biApi.getDashboardRuntimeState).mockResolvedValue(ok(null as never))
    vi.mocked(biApi.getQueryGovernanceSummary).mockResolvedValue(ok(null as never))
    vi.mocked(biApi.getQueryGovernancePolicy).mockResolvedValue(ok(null as never))
    vi.mocked(biApi.getQueryCachePolicy).mockResolvedValue(ok(null as never))
    vi.mocked(biApi.getQueryCacheStats).mockResolvedValue(ok(null as never))
    vi.mocked(biApi.getDatasetAccelerationPolicy).mockResolvedValue(ok(null as never))
    vi.mocked(biApi.getDatasourceHealthSlo).mockResolvedValue(ok(null as never))
    vi.mocked(biApi.auditDeliveryLogs).mockResolvedValue(ok(null as never))
    vi.mocked(biApi.createEmbedTicket).mockResolvedValue(ok({
      ticket: 'embed-ticket',
      expiresAt: '2026-06-06T10:00:00Z',
      embedUrl: '/bi/embed/DASHBOARD/canvas-effect?ticket=embed-ticket',
    }))
    vi.mocked(biApi.listBigScreenResources).mockResolvedValue(ok([
      {
        id: 50,
        screenKey: 'ops-wall',
        name: 'Ops Wall',
        description: null,
        size: { width: 1920, height: 1080 },
        background: { color: '#101820' },
        layout: [{ widgetKey: 'ops-kpi', title: 'Ops KPI', x: 0, y: 0, w: 6, h: 4 }],
        refresh: { intervalSeconds: 60 },
        mobileLayout: {},
        status: 'PUBLISHED',
        version: 2,
        source: 'PERSISTED',
      },
      {
        id: 51,
        screenKey: 'campaign-wall',
        name: 'Campaign Command Wall',
        description: 'Campaign delivery wall',
        size: { width: 1920, height: 1080 },
        background: { color: '#0f172a' },
        layout: [{ widgetKey: 'campaign-kpi', title: 'Campaign KPI', x: 0, y: 0, w: 8, h: 5 }],
        refresh: { intervalSeconds: 30 },
        mobileLayout: {},
        status: 'PUBLISHED',
        version: 3,
        source: 'PERSISTED',
      },
    ]))
    vi.mocked(biApi.listSpreadsheetResources).mockResolvedValue(ok([
      {
        id: 60,
        spreadsheetKey: 'campaign-sheet',
        name: 'Campaign Sheet',
        description: null,
        sheets: [{ sheetKey: 'summary', cells: { A1: 'Campaign' } }],
        dataBinding: { datasetKey: 'canvas_daily_stats' },
        style: { theme: 'default' },
        status: 'PUBLISHED',
        version: 1,
        source: 'PERSISTED',
      },
      {
        id: 61,
        spreadsheetKey: 'budget-sheet',
        name: 'Budget Sheet',
        description: 'Budget rollup',
        sheets: [{
          sheetKey: 'summary',
          cells: { A1: 'Budget', B2: '=SUM(B3:B8)', B3: '2', B4: 4 },
          cellStyles: { B2: { bold: true, backgroundColor: '#FEF3C7', textColor: '#0F172A' } },
          mobileLayout: { enabled: true, columns: 4 },
        }],
        dataBinding: { datasetKey: 'budget_daily_stats' },
        style: { theme: 'compact' },
        status: 'PUBLISHED',
        version: 4,
        source: 'PERSISTED',
      },
    ]))
    vi.mocked(biApi.listChartResources).mockResolvedValue(ok([
      {
        chartKey: 'trend-executions',
        name: 'Trend Executions',
        chartType: 'LINE',
        datasetKey: 'canvas_daily_stats',
        query: {
          datasetKey: 'canvas_daily_stats',
          dimensions: ['stat_date'],
          metrics: ['total_executions'],
          filters: [],
          sorts: [{ field: 'stat_date', direction: 'ASC' }],
          limit: 100,
        },
        style: { palette: 'blue' },
        interaction: { drill: true },
        status: 'DRAFT',
        source: 'PERSISTED',
      },
    ]))
    vi.mocked(biApi.listPortalResources).mockResolvedValue(ok([
      {
        portalKey: 'executive-home',
        name: 'Executive Home',
        theme: { theme: 'light', navigationLayout: 'top', defaultMenuKey: 'overview' },
        menus: [
          { menuKey: 'overview', title: 'Overview', resourceType: 'DASHBOARD', resourceKey: 'canvas-effect', visibility: {}, sortOrder: 1 },
          { menuKey: 'sales', title: 'Sales', resourceType: 'DASHBOARD', resourceKey: 'sales-dashboard', visibility: {}, sortOrder: 2 },
        ],
        status: 'DRAFT',
        source: 'PERSISTED',
      },
    ]))
  })

  afterEach(() => {
    vi.clearAllMocks()
  })

  it('renders the big-screen runtime view selected by backend resource id', async () => {
    renderBiRoute('/bi?resourceType=BIG_SCREEN&resourceId=51&mode=big-screen')

    await waitFor(() => expect(biApi.listBigScreenResources).toHaveBeenCalled())
    expect(await screen.findByRole('heading', { name: 'Campaign Command Wall' })).toBeInTheDocument()
    expect(screen.getByText('campaign-kpi')).toBeInTheDocument()
  })

  it('renders the spreadsheet runtime view selected by backend resource id', async () => {
    renderBiRoute('/bi?resourceType=SPREADSHEET&resourceId=61&mode=spreadsheet')

    await waitFor(() => expect(biApi.listSpreadsheetResources).toHaveBeenCalled())
    expect(await screen.findByRole('heading', { name: 'Budget Sheet' })).toBeInTheDocument()
    expect(screen.queryByText('=SUM(B3:B8)')).not.toBeInTheDocument()
    expect(screen.getAllByText('6').length).toBeGreaterThan(0)
    expect(screen.getByLabelText('电子表格单元格 B2')).toHaveStyle({
      backgroundColor: '#FEF3C7',
      color: '#0F172A',
      fontWeight: '600',
    })
    expect(screen.getByLabelText('电子表格 summary 移动端视图')).toBeInTheDocument()
    expect(screen.queryByLabelText('电子表格单元格 H1')).not.toBeInTheDocument()
  })

  it('cancels queued self-service export jobs from the task list', async () => {
    vi.mocked(biApi.listExports).mockResolvedValue(ok([
      {
        id: 880,
        tenantId: 7,
        workspaceId: 3,
        resourceType: 'DATASET',
        resourceKey: 'canvas_daily_stats',
        resourceId: 11,
        exportFormat: 'CSV',
        rowLimit: 1000,
        status: 'QUEUED',
        progressPercent: 0,
        downloadCount: 0,
        retryCount: 0,
        maxRetryCount: 3,
        createdBy: 'alice',
        createdAt: '2026-06-05T09:55:00',
      },
    ]))
    vi.mocked(biApi.cancelExport).mockResolvedValue(ok({
      id: 880,
      status: 'CANCELED',
    } as never))

    renderBiRoute('/bi')

    expect(await screen.findByText('#880')).toBeInTheDocument()
    fireEvent.click(screen.getByLabelText('取消导出 #880'))

    await waitFor(() => expect(biApi.cancelExport).toHaveBeenCalledWith(880))
  }, 30000)

  it('reviews pending self-service export approvals from the task list', async () => {
    vi.mocked(biApi.listExports).mockResolvedValue(ok([
      {
        id: 881,
        tenantId: 7,
        workspaceId: 3,
        resourceType: 'DATASET',
        resourceKey: 'canvas_daily_stats',
        resourceId: 11,
        exportFormat: 'CSV',
        rowLimit: 6000,
        status: 'PENDING_APPROVAL',
        progressPercent: 0,
        downloadCount: 0,
        approvalStatus: 'PENDING',
        approvalReason: 'Sensitive export',
        requestedBy: 'alice',
        requestedAt: '2026-06-07T09:20:00',
        createdBy: 'alice',
        createdAt: '2026-06-07T09:20:00',
      },
    ]))
    vi.mocked(biApi.reviewExport).mockResolvedValue(ok({
      id: 881,
      status: 'QUEUED',
      approvalStatus: 'APPROVED',
      reviewComment: 'Approved after audit',
    } as never))

    renderBiRoute('/bi')

    expect(await screen.findByText('#881')).toBeInTheDocument()
    fireEvent.change(screen.getByLabelText('导出审批意见'), {
      target: { value: 'Approved after audit' },
    })
    fireEvent.click(screen.getByLabelText('批准导出 #881'))

    await waitFor(() => expect(biApi.reviewExport).toHaveBeenCalledWith(881, {
      status: 'APPROVED',
      reviewComment: 'Approved after audit',
    }))
  }, 30000)

  it('renders self-service export hardening diagnostics', async () => {
    vi.mocked(biApi.listExports).mockResolvedValue(ok([
      {
        id: 891,
        tenantId: 7,
        workspaceId: 3,
        resourceType: 'DATASET',
        resourceKey: 'canvas_daily_stats',
        exportFormat: 'CSV',
        rowLimit: 1000000,
        status: 'COMPLETED',
        progressPercent: 100,
        storageProvider: 'S3',
        storageKey: 'exports/tenant-7/export-891.zip',
        retentionDays: 7,
        expiresAt: '2099-06-12T00:00:00',
        downloadCount: 3,
        retryCount: 0,
        maxRetryCount: 3,
        storageLayout: 'OBJECT_PER_PART_ZIP',
        requestedRows: 1000000,
        generatedRows: 1000000,
        partCount: 100,
        partSize: 10000,
        partStorageKeys: ['exports/tenant-7/export-891/parts/part-00001.csv'],
      },
      {
        id: 892,
        tenantId: 7,
        workspaceId: 3,
        resourceType: 'DATASET',
        resourceKey: 'canvas_daily_stats',
        exportFormat: 'XLSX',
        rowLimit: 50000,
        status: 'PENDING_APPROVAL',
        progressPercent: 0,
        approvalStatus: 'PENDING',
        approvalReason: 'Sensitive customer list',
        requestedBy: 'alice',
        retryCount: 1,
        maxRetryCount: 3,
      },
      {
        id: 893,
        tenantId: 7,
        workspaceId: 3,
        resourceType: 'DATASET',
        resourceKey: 'canvas_daily_stats',
        exportFormat: 'CSV',
        rowLimit: 1000,
        status: 'EXPIRED',
        retentionDays: 7,
        downloadCount: 1,
        retryCount: 3,
        maxRetryCount: 3,
        retryExhaustedAt: '2026-06-08T10:00:00',
      },
    ]))

    renderBiRoute('/bi')

    expect(await screen.findByText('导出硬化诊断')).toBeInTheDocument()
    expect(screen.getByText('3 任务 · 1 审批中 · 1 已过期')).toBeInTheDocument()
    expect(screen.getByText('1 分片任务 · 100 分片 · 1,000,000/1,000,000 行')).toBeInTheDocument()
    expect(screen.getByText('7 天留存 · 4 下载 · 1 过期清理')).toBeInTheDocument()
    expect(screen.getByText('2 重试中 · 1 已耗尽')).toBeInTheDocument()
  }, 30000)

  it('renders holiday-aware period-over-period anomaly diagnostics', async () => {
    vi.mocked(biApi.listAlerts).mockResolvedValue(ok([
      {
        id: 901,
        tenantId: 7,
        workspaceId: 3,
        alertKey: 'gmv-yoy-holiday',
        name: 'GMV 春节同比异常',
        datasetId: 11,
        datasetKey: 'canvas_daily_stats',
        metricKey: 'total_executions',
        condition: {
          operator: 'ANOMALY_DROP',
          model: 'PERIOD_OVER_PERIOD',
          period: 'YEAR_OVER_YEAR',
          naturalBoundary: true,
          holidayComparisonDate: '2025-02-10',
          holidayName: 'spring-festival',
          minSamples: 3,
          calendarWindowHours: 6,
          silenceWindow: { minutes: 60 },
        },
        receivers: { channels: ['LARK'], users: ['CURRENT_USER'] },
        enabled: true,
      },
      {
        id: 902,
        tenantId: 7,
        workspaceId: 3,
        alertKey: 'revenue-mom',
        name: '收入月环比异常',
        datasetId: 11,
        datasetKey: 'canvas_daily_stats',
        metricKey: 'success_rate',
        condition: {
          operator: 'ANOMALY_RISE',
          model: 'PERIOD_OVER_PERIOD',
          period: 'MONTH_OVER_MONTH',
          naturalBoundary: true,
          minSamples: 4,
          calendarWindowHours: 12,
        },
        receivers: { channels: ['WEBHOOK'], users: ['CURRENT_USER'] },
        enabled: true,
      },
      {
        id: 903,
        tenantId: 7,
        workspaceId: 3,
        alertKey: 'success-rate-point',
        name: '成功率点异常',
        datasetId: 11,
        datasetKey: 'canvas_daily_stats',
        metricKey: 'success_rate',
        condition: {
          operator: 'ANOMALY_DROP',
          model: 'POINT',
          minSamples: 2,
        },
        receivers: { channels: ['EMAIL'], users: ['CURRENT_USER'] },
        enabled: false,
      },
    ]))

    renderBiRoute('/bi')

    expect(await screen.findByText('异常诊断')).toBeInTheDocument()
    expect(screen.getByText('3 异常 · 2 同环比 · 1 停用')).toBeInTheDocument()
    expect(screen.getByText('YEAR_OVER_YEAR / MONTH_OVER_MONTH · 2 自然边界 · 6h/12h 窗口')).toBeInTheDocument()
    expect(screen.getByText('1 节假日 · spring-festival -> 2025-02-10')).toBeInTheDocument()
    expect(screen.getByText('最小样本 2/3/4 · 1 静默 · 1 停用')).toBeInTheDocument()
  }, 30000)

  it('saves edited big-screen layout controls from the resource workbench', async () => {
    vi.mocked(biApi.saveBigScreenDraft).mockImplementation((_screenKey: string, resource) => Promise.resolve(ok(resource)))

    renderBiRoute('/bi')

    fireEvent.change(await screen.findByLabelText('大屏组件标题'), {
      target: { value: 'Executive Wall KPI' },
    })
    fireEvent.change(screen.getByLabelText('大屏组件x'), {
      target: { value: '3' },
    })
    fireEvent.change(screen.getByLabelText('大屏组件w'), {
      target: { value: '7' },
    })
    fireEvent.click(screen.getByRole('button', { name: '保存大屏草稿' }))

    await waitFor(() => expect(biApi.saveBigScreenDraft).toHaveBeenCalledWith(
      'ops-wall',
      expect.objectContaining({
        screenKey: 'ops-wall',
        layout: [
          expect.objectContaining({
            widgetKey: 'ops-kpi',
            title: 'Executive Wall KPI',
            x: 3,
            w: 7,
          }),
        ],
      }),
      null,
    ))
  }, 30000)

  it('renders visual editor diagnostics for big-screen and spreadsheet resources', async () => {
    renderBiRoute('/bi')

    expect(await screen.findByText('视觉诊断')).toBeInTheDocument()
    expect(screen.getByText('1 组件 · 0 重叠 · 0 越界')).toBeInTheDocument()
    expect(screen.getByText('未配置移动端布局')).toBeInTheDocument()
    expect(screen.getByText('1 单元格 · 0 公式 · 0 错误值')).toBeInTheDocument()
    expect(screen.getByText('0 样式 · 0 条件格式 · 0 透视表')).toBeInTheDocument()
  }, 30000)

  it('moves and resizes the selected big-screen layout item from the workbench', async () => {
    vi.mocked(biApi.saveBigScreenDraft).mockImplementation((_screenKey: string, resource) => Promise.resolve(ok(resource)))

    renderBiRoute('/bi')

    await screen.findByLabelText('大屏组件标题')
    fireEvent.click(screen.getByRole('button', { name: '大屏组件右移' }))
    fireEvent.click(screen.getByRole('button', { name: '大屏组件放宽' }))
    fireEvent.click(screen.getByRole('button', { name: '保存大屏草稿' }))

    await waitFor(() => expect(biApi.saveBigScreenDraft).toHaveBeenCalledWith(
      'ops-wall',
      expect.objectContaining({
        layout: [
          expect.objectContaining({
            widgetKey: 'ops-kpi',
            x: 1,
            w: 7,
          }),
        ],
      }),
      null,
    ))
  }, 30000)

  it('adds big-screen component library items from the workbench', async () => {
    vi.mocked(biApi.saveBigScreenDraft).mockImplementation((_screenKey: string, resource) => Promise.resolve(ok(resource)))

    renderBiRoute('/bi')

    await screen.findByLabelText('大屏组件标题')
    fireEvent.change(screen.getByLabelText('大屏组件库'), {
      target: { value: 'trend-line' },
    })
    fireEvent.click(screen.getByRole('button', { name: '添加大屏组件' }))
    fireEvent.click(screen.getByRole('button', { name: '保存大屏草稿' }))

    await waitFor(() => expect(biApi.saveBigScreenDraft).toHaveBeenCalledWith(
      'ops-wall',
      expect.objectContaining({
        layout: expect.arrayContaining([
          expect.objectContaining({
            widgetKey: 'ops-wall-trend-line',
            title: '趋势折线',
            componentType: 'TREND_LINE',
            resourceType: 'DATASET',
            x: 6,
            y: 0,
            w: 12,
            h: 5,
          }),
        ]),
      }),
      null,
    ))
  }, 30000)

  it('aligns big-screen layout items from the workbench', async () => {
    vi.mocked(biApi.listBigScreenResources).mockResolvedValue(ok([
      {
        id: 50,
        screenKey: 'ops-wall',
        name: 'Ops Wall',
        description: null,
        size: { width: 1920, height: 1080 },
        background: { color: '#101820' },
        layout: [
          { widgetKey: 'ops-kpi', title: 'Ops KPI', x: 0, y: 0, w: 6, h: 4 },
          { widgetKey: 'ops-detail', title: 'Ops Detail', x: 5, y: 6, w: 8, h: 4 },
        ],
        refresh: { intervalSeconds: 60 },
        mobileLayout: {},
        status: 'PUBLISHED',
        version: 2,
        source: 'PERSISTED',
      },
    ]))
    vi.mocked(biApi.saveBigScreenDraft).mockImplementation((_screenKey: string, resource) => Promise.resolve(ok(resource)))

    renderBiRoute('/bi')

    await screen.findByLabelText('大屏组件标题')
    fireEvent.click(screen.getByRole('button', { name: '大屏组件左对齐' }))
    fireEvent.click(screen.getByRole('button', { name: '保存大屏草稿' }))

    await waitFor(() => expect(biApi.saveBigScreenDraft).toHaveBeenCalledWith(
      'ops-wall',
      expect.objectContaining({
        layout: [
          expect.objectContaining({ widgetKey: 'ops-kpi', x: 0 }),
          expect.objectContaining({ widgetKey: 'ops-detail', x: 0 }),
        ],
      }),
      null,
    ))
  }, 30000)

  it('snaps the selected big-screen layout item from the workbench', async () => {
    vi.mocked(biApi.listBigScreenResources).mockResolvedValue(ok([
      {
        id: 50,
        screenKey: 'ops-wall',
        name: 'Ops Wall',
        description: null,
        size: { width: 1920, height: 1080 },
        background: { color: '#101820' },
        layout: [
          { widgetKey: 'ops-detail', title: 'Ops Detail', x: 9, y: 5, w: 8, h: 4 },
          { widgetKey: 'ops-kpi', title: 'Ops KPI', x: 0, y: 0, w: 8, h: 4 },
        ],
        refresh: { intervalSeconds: 60 },
        mobileLayout: {},
        status: 'PUBLISHED',
        version: 2,
        source: 'PERSISTED',
      },
    ]))
    vi.mocked(biApi.saveBigScreenDraft).mockImplementation((_screenKey: string, resource) => Promise.resolve(ok(resource)))

    renderBiRoute('/bi')

    await screen.findByLabelText('大屏组件标题')
    fireEvent.click(screen.getByRole('button', { name: '吸附大屏组件' }))
    fireEvent.click(screen.getByRole('button', { name: '保存大屏草稿' }))

    await waitFor(() => expect(biApi.saveBigScreenDraft).toHaveBeenCalledWith(
      'ops-wall',
      expect.objectContaining({
        layout: [
          expect.objectContaining({ widgetKey: 'ops-detail', x: 8, y: 4 }),
          expect.objectContaining({ widgetKey: 'ops-kpi', x: 0, y: 0 }),
        ],
      }),
      null,
    ))
  }, 30000)

  it('saves big-screen mobile layout variants from the workbench', async () => {
    vi.mocked(biApi.listBigScreenResources).mockResolvedValue(ok([
      {
        id: 50,
        screenKey: 'ops-wall',
        name: 'Ops Wall',
        description: null,
        size: { width: 1920, height: 1080 },
        background: { color: '#101820' },
        layout: [
          { widgetKey: 'ops-kpi', title: 'Ops KPI', x: 0, y: 0, w: 8, h: 4 },
          { widgetKey: 'ops-detail', title: 'Ops Detail', x: 8, y: 0, w: 8, h: 5 },
          { widgetKey: 'ops-trend', title: 'Ops Trend', x: 0, y: 6, w: 24, h: 6 },
        ],
        refresh: { intervalSeconds: 60 },
        mobileLayout: {},
        status: 'PUBLISHED',
        version: 2,
        source: 'PERSISTED',
      },
    ]))
    vi.mocked(biApi.saveBigScreenDraft).mockImplementation((_screenKey: string, resource) => Promise.resolve(ok(resource)))

    renderBiRoute('/bi')

    await screen.findByLabelText('大屏组件标题')
    fireEvent.change(screen.getByLabelText('大屏移动端布局'), {
      target: { value: 'compact-grid' },
    })
    fireEvent.click(screen.getByRole('button', { name: '保存大屏草稿' }))

    await waitFor(() => expect(biApi.saveBigScreenDraft).toHaveBeenCalledWith(
      'ops-wall',
      expect.objectContaining({
        mobileLayout: expect.objectContaining({
          variant: 'compact-grid',
          columns: 2,
          items: [
            { widgetKey: 'ops-kpi', x: 0, y: 0, w: 1, h: 4 },
            { widgetKey: 'ops-detail', x: 1, y: 0, w: 1, h: 5 },
            { widgetKey: 'ops-trend', x: 0, y: 5, w: 2, h: 6 },
          ],
        }),
      }),
      null,
    ))
  }, 30000)

  it('saves edited spreadsheet cells from the resource workbench', async () => {
    vi.mocked(biApi.saveSpreadsheetDraft).mockImplementation((_spreadsheetKey: string, resource) => Promise.resolve(ok(resource)))

    renderBiRoute('/bi')

    fireEvent.change(await screen.findByLabelText('电子表格单元格'), {
      target: { value: 'B2' },
    })
    fireEvent.change(screen.getByLabelText('电子表格单元格内容'), {
      target: { value: '=SUM(B3:B8)' },
    })
    fireEvent.click(screen.getByRole('button', { name: '保存电子表格草稿' }))

    await waitFor(() => expect(biApi.saveSpreadsheetDraft).toHaveBeenCalledWith(
      'campaign-sheet',
      expect.objectContaining({
        spreadsheetKey: 'campaign-sheet',
        sheets: [
          expect.objectContaining({
            sheetKey: 'summary',
            cells: expect.objectContaining({
              A1: 'Campaign',
              B2: '=SUM(B3:B8)',
            }),
          }),
        ],
      }),
      null,
    ))
  }, 30000)

  it('fills spreadsheet cell ranges from the resource workbench', async () => {
    vi.mocked(biApi.saveSpreadsheetDraft).mockImplementation((_spreadsheetKey: string, resource) => Promise.resolve(ok(resource)))

    renderBiRoute('/bi')

    fireEvent.change(await screen.findByLabelText('电子表格单元格内容'), {
      target: { value: 'Ready' },
    })
    fireEvent.change(screen.getByLabelText('电子表格填充范围'), {
      target: { value: 'B2:C3' },
    })
    fireEvent.click(screen.getByRole('button', { name: '批量填充电子表格单元格' }))
    fireEvent.click(screen.getByRole('button', { name: '保存电子表格草稿' }))

    await waitFor(() => expect(biApi.saveSpreadsheetDraft).toHaveBeenCalledWith(
      'campaign-sheet',
      expect.objectContaining({
        sheets: [
          expect.objectContaining({
            cells: expect.objectContaining({
              B2: 'Ready',
              B3: 'Ready',
              C2: 'Ready',
              C3: 'Ready',
            }),
          }),
        ],
      }),
      null,
    ))
  }, 30000)

  it('saves spreadsheet cell styles from the resource workbench', async () => {
    vi.mocked(biApi.saveSpreadsheetDraft).mockImplementation((_spreadsheetKey: string, resource) => Promise.resolve(ok(resource)))

    renderBiRoute('/bi')

    fireEvent.change(await screen.findByLabelText('电子表格单元格'), {
      target: { value: 'B2' },
    })
    fireEvent.click(screen.getByRole('button', { name: '加粗单元格' }))
    fireEvent.change(screen.getByLabelText('电子表格背景色'), {
      target: { value: '#FEF3C7' },
    })
    fireEvent.change(screen.getByLabelText('电子表格文字色'), {
      target: { value: '#0F172A' },
    })
    fireEvent.click(screen.getByRole('button', { name: '保存电子表格草稿' }))

    await waitFor(() => expect(biApi.saveSpreadsheetDraft).toHaveBeenCalledWith(
      'campaign-sheet',
      expect.objectContaining({
        sheets: [
          expect.objectContaining({
            cellStyles: expect.objectContaining({
              B2: {
                bold: true,
                backgroundColor: '#FEF3C7',
                textColor: '#0F172A',
              },
            }),
          }),
        ],
      }),
      null,
    ))
  }, 30000)

  it('generates spreadsheet pivot tables from the resource workbench', async () => {
    vi.mocked(biApi.saveSpreadsheetDraft).mockImplementation((_spreadsheetKey: string, resource) => Promise.resolve(ok(resource)))
    vi.mocked(biApi.listSpreadsheetResources).mockResolvedValue(ok([
      {
        id: 60,
        spreadsheetKey: 'campaign-sheet',
        name: 'Campaign Sheet',
        description: null,
        sheets: [{
          sheetKey: 'summary',
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
        dataBinding: { datasetKey: 'canvas_daily_stats' },
        style: { theme: 'default' },
        status: 'PUBLISHED',
        version: 1,
        source: 'PERSISTED',
      },
    ]))

    renderBiRoute('/bi')

    fireEvent.click(await screen.findByRole('button', { name: '生成交叉表透视' }))
    fireEvent.click(screen.getByRole('button', { name: '保存电子表格草稿' }))

    await waitFor(() => expect(biApi.saveSpreadsheetDraft).toHaveBeenCalledWith(
      'campaign-sheet',
      expect.objectContaining({
        sheets: [
          expect.objectContaining({
            pivotTables: [
              expect.objectContaining({
                pivotKey: 'pivot-summary-f1',
                sourceRange: 'A1:D5',
                targetCell: 'F1',
                rowField: '地区',
                columnField: '渠道',
                valueField: '消耗',
                aggregation: 'SUM',
              }),
            ],
            cells: expect.objectContaining({
              F1: '地区 / 渠道',
              G1: '搜索',
              H1: '信息流',
              F2: '华东',
              G2: 130,
              H2: 60,
              F3: '华南',
              G3: 40,
              H3: 0,
            }),
          }),
        ],
      }),
      null,
    ))
  }, 30000)

  it('generates multi-metric spreadsheet pivot tables from the resource workbench', async () => {
    vi.mocked(biApi.saveSpreadsheetDraft).mockImplementation((_spreadsheetKey: string, resource) => Promise.resolve(ok(resource)))
    vi.mocked(biApi.listSpreadsheetResources).mockResolvedValue(ok([
      {
        id: 60,
        spreadsheetKey: 'campaign-sheet',
        name: 'Campaign Sheet',
        description: null,
        sheets: [{
          sheetKey: 'summary',
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
        dataBinding: { datasetKey: 'canvas_daily_stats' },
        style: { theme: 'default' },
        status: 'PUBLISHED',
        version: 1,
        source: 'PERSISTED',
      },
    ]))

    renderBiRoute('/bi')

    fireEvent.click(await screen.findByRole('button', { name: '生成多指标透视' }))
    fireEvent.click(screen.getByRole('button', { name: '保存电子表格草稿' }))

    await waitFor(() => expect(biApi.saveSpreadsheetDraft).toHaveBeenCalledWith(
      'campaign-sheet',
      expect.objectContaining({
        sheets: [
          expect.objectContaining({
            pivotTables: [
              expect.objectContaining({
                pivotKey: 'pivot-summary-f1',
                valueFields: [
                  { field: '消耗', aggregation: 'SUM', label: '消耗' },
                  { field: '转化', aggregation: 'COUNT', label: '转化次数' },
                ],
              }),
            ],
            cells: expect.objectContaining({
              G1: '搜索 消耗',
              H1: '搜索 转化次数',
              I1: '信息流 消耗',
              J1: '信息流 转化次数',
              G2: 130,
              H2: 2,
              I2: 60,
              J2: 1,
            }),
          }),
        ],
      }),
      null,
    ))
  }, 30000)

  it('generates configurable multi-value spreadsheet pivot tables from the resource workbench', async () => {
    vi.mocked(biApi.saveSpreadsheetDraft).mockImplementation((_spreadsheetKey: string, resource) => Promise.resolve(ok(resource)))
    vi.mocked(biApi.listSpreadsheetResources).mockResolvedValue(ok([
      {
        id: 60,
        spreadsheetKey: 'campaign-sheet',
        name: 'Campaign Sheet',
        description: null,
        sheets: [{
          sheetKey: 'summary',
          cells: {
            A1: '区域',
            B1: '平台',
            C1: '花费',
            D1: '转化',
            E1: '收入',
            A2: '华东',
            B2: '搜索',
            C2: 100,
            D2: 4,
            E2: 700,
            A3: '华东',
            B3: '信息流',
            C3: 60,
            D3: 2,
            E3: 260,
            A4: '华南',
            B4: '搜索',
            C4: 40,
            D4: 1,
            E4: 120,
            A5: '华东',
            B5: '搜索',
            C5: 30,
            D5: 1,
            E5: 180,
          },
        }],
        dataBinding: { datasetKey: 'canvas_daily_stats' },
        style: { theme: 'default' },
        status: 'PUBLISHED',
        version: 1,
        source: 'PERSISTED',
      },
    ]))

    renderBiRoute('/bi')

    fireEvent.change(await screen.findByLabelText('透视源范围'), { target: { value: 'A1:E5' } })
    fireEvent.change(screen.getByLabelText('透视输出单元格'), { target: { value: 'K2' } })
    fireEvent.change(screen.getByLabelText('透视行字段'), { target: { value: '区域' } })
    fireEvent.change(screen.getByLabelText('透视列字段'), { target: { value: '平台' } })
    fireEvent.change(screen.getByLabelText('透视指标字段 1'), { target: { value: '花费' } })
    fireEvent.change(screen.getByLabelText('透视指标标签 1'), { target: { value: '成本' } })
    fireEvent.change(screen.getByLabelText('透视指标字段 2'), { target: { value: '转化' } })
    fireEvent.change(screen.getByLabelText('透视指标标签 2'), { target: { value: '转化数' } })
    fireEvent.click(screen.getByRole('button', { name: '添加透视指标' }))
    fireEvent.change(screen.getByLabelText('透视指标字段 3'), { target: { value: '收入' } })
    fireEvent.mouseDown(screen.getAllByLabelText('透视指标聚合 3')[0].querySelector('.ant-select-selector')!)
    const maxAggregationOptions = await screen.findAllByText('最大值')
    fireEvent.click(maxAggregationOptions[maxAggregationOptions.length - 1])
    fireEvent.change(screen.getByLabelText('透视指标标签 3'), { target: { value: '最高收入' } })

    fireEvent.click(screen.getByRole('button', { name: '生成多指标透视' }))
    fireEvent.click(screen.getByRole('button', { name: '保存电子表格草稿' }))

    await waitFor(() => expect(biApi.saveSpreadsheetDraft).toHaveBeenCalledWith(
      'campaign-sheet',
      expect.objectContaining({
        sheets: [
          expect.objectContaining({
            pivotTables: [
              expect.objectContaining({
                pivotKey: 'pivot-summary-k2',
                sourceRange: 'A1:E5',
                targetCell: 'K2',
                rowField: '区域',
                columnField: '平台',
                valueField: '花费',
                aggregation: 'SUM',
                valueFields: [
                  { field: '花费', aggregation: 'SUM', label: '成本' },
                  { field: '转化', aggregation: 'COUNT', label: '转化数' },
                  { field: '收入', aggregation: 'MAX', label: '最高收入' },
                ],
              }),
            ],
            cells: expect.objectContaining({
              K2: '区域 / 平台',
              L2: '搜索 成本',
              M2: '搜索 转化数',
              N2: '搜索 最高收入',
              O2: '信息流 成本',
              P2: '信息流 转化数',
              Q2: '信息流 最高收入',
              K3: '华东',
              L3: 130,
              M3: 2,
              N3: 700,
              O3: 60,
              P3: 1,
              Q3: 260,
              K4: '华南',
              L4: 40,
              M4: 1,
              N4: 120,
              O4: 0,
              P4: 0,
              Q4: 0,
            }),
          }),
        ],
      }),
      null,
    ))
  }, 30000)

  it('assigns spreadsheet pivot fields from detected source headers by drag and drop', async () => {
    vi.mocked(biApi.saveSpreadsheetDraft).mockImplementation((_spreadsheetKey: string, resource) => Promise.resolve(ok(resource)))
    vi.mocked(biApi.listSpreadsheetResources).mockResolvedValue(ok([
      {
        id: 60,
        spreadsheetKey: 'campaign-sheet',
        name: 'Campaign Sheet',
        description: null,
        sheets: [{
          sheetKey: 'summary',
          cells: {
            A1: '区域',
            B1: '平台',
            C1: '花费',
            D1: '转化',
            E1: '收入',
            A2: '华东',
            B2: '搜索',
            C2: 100,
            D2: 4,
            E2: 700,
            A3: '华东',
            B3: '信息流',
            C3: 60,
            D3: 2,
            E3: 260,
            A4: '华南',
            B4: '搜索',
            C4: 40,
            D4: 1,
            E4: 120,
            A5: '华东',
            B5: '搜索',
            C5: 30,
            D5: 1,
            E5: 180,
          },
        }],
        dataBinding: { datasetKey: 'canvas_daily_stats' },
        style: { theme: 'default' },
        status: 'PUBLISHED',
        version: 1,
        source: 'PERSISTED',
      },
    ]))
    const dragPayload = new Map<string, string>()
    const dataTransfer = {
      effectAllowed: '',
      dropEffect: '',
      setData: (type: string, value: string) => dragPayload.set(type, value),
      getData: (type: string) => dragPayload.get(type) ?? '',
      clearData: () => dragPayload.clear(),
    }

    renderBiRoute('/bi')

    fireEvent.change(await screen.findByLabelText('透视源范围'), { target: { value: 'A1:E5' } })
    fireEvent.change(screen.getByLabelText('透视输出单元格'), { target: { value: 'K2' } })

    fireEvent.dragStart(await screen.findByLabelText('透视字段 区域'), { dataTransfer })
    fireEvent.drop(screen.getByLabelText('透视行字段放置区'), { dataTransfer })
    fireEvent.dragStart(screen.getByLabelText('透视字段 平台'), { dataTransfer })
    fireEvent.drop(screen.getByLabelText('透视列字段放置区'), { dataTransfer })
    fireEvent.dragStart(screen.getByLabelText('透视字段 花费'), { dataTransfer })
    fireEvent.drop(screen.getByLabelText('透视指标放置区'), { dataTransfer })
    fireEvent.click(screen.getByRole('button', { name: '删除透视指标 2' }))
    fireEvent.dragStart(screen.getByLabelText('透视字段 收入'), { dataTransfer })
    fireEvent.drop(screen.getByLabelText('透视指标放置区'), { dataTransfer })
    fireEvent.change(screen.getByLabelText('透视指标标签 1'), { target: { value: '成本' } })
    fireEvent.change(screen.getByLabelText('透视指标标签 2'), { target: { value: '最高收入' } })
    fireEvent.mouseDown(screen.getAllByLabelText('透视指标聚合 2')[0].querySelector('.ant-select-selector')!)
    const maxAggregationOptions = await screen.findAllByText('最大值')
    fireEvent.click(maxAggregationOptions[maxAggregationOptions.length - 1])

    fireEvent.click(screen.getByRole('button', { name: '生成多指标透视' }))
    fireEvent.click(screen.getByRole('button', { name: '保存电子表格草稿' }))

    await waitFor(() => expect(biApi.saveSpreadsheetDraft).toHaveBeenCalledWith(
      'campaign-sheet',
      expect.objectContaining({
        sheets: [
          expect.objectContaining({
            pivotTables: [
              expect.objectContaining({
                sourceRange: 'A1:E5',
                targetCell: 'K2',
                rowField: '区域',
                columnField: '平台',
                valueField: '花费',
                valueFields: [
                  { field: '花费', aggregation: 'SUM', label: '成本' },
                  { field: '收入', aggregation: 'MAX', label: '最高收入' },
                ],
              }),
            ],
            cells: expect.objectContaining({
              L2: '搜索 成本',
              M2: '搜索 最高收入',
              N2: '信息流 成本',
              O2: '信息流 最高收入',
              L3: 130,
              M3: 700,
              N3: 60,
              O3: 260,
            }),
          }),
        ],
      }),
      null,
    ))
  }, 30000)

  it('reorders spreadsheet pivot metrics and previews output columns before saving', async () => {
    vi.mocked(biApi.saveSpreadsheetDraft).mockImplementation((_spreadsheetKey: string, resource) => Promise.resolve(ok(resource)))
    vi.mocked(biApi.listSpreadsheetResources).mockResolvedValue(ok([
      {
        id: 60,
        spreadsheetKey: 'campaign-sheet',
        name: 'Campaign Sheet',
        description: null,
        sheets: [{
          sheetKey: 'summary',
          cells: {
            A1: '区域',
            B1: '平台',
            C1: '花费',
            D1: '转化',
            E1: '收入',
            A2: '华东',
            B2: '搜索',
            C2: 100,
            D2: 4,
            E2: 700,
            A3: '华东',
            B3: '信息流',
            C3: 60,
            D3: 2,
            E3: 260,
          },
        }],
        dataBinding: { datasetKey: 'canvas_daily_stats' },
        style: { theme: 'default' },
        status: 'PUBLISHED',
        version: 1,
        source: 'PERSISTED',
      },
    ]))

    renderBiRoute('/bi')

    fireEvent.change(await screen.findByLabelText('透视源范围'), { target: { value: 'A1:E3' } })
    fireEvent.change(screen.getByLabelText('透视输出单元格'), { target: { value: 'K2' } })
    fireEvent.change(screen.getByLabelText('透视行字段'), { target: { value: '区域' } })
    fireEvent.change(screen.getByLabelText('透视列字段'), { target: { value: '平台' } })
    fireEvent.change(screen.getByLabelText('透视指标字段 1'), { target: { value: '花费' } })
    fireEvent.change(screen.getByLabelText('透视指标标签 1'), { target: { value: '成本' } })
    fireEvent.change(screen.getByLabelText('透视指标字段 2'), { target: { value: '转化' } })
    fireEvent.change(screen.getByLabelText('透视指标标签 2'), { target: { value: '转化数' } })
    fireEvent.click(screen.getByRole('button', { name: '添加透视指标' }))
    fireEvent.change(screen.getByLabelText('透视指标字段 3'), { target: { value: '收入' } })
    fireEvent.change(screen.getByLabelText('透视指标标签 3'), { target: { value: '营收' } })
    fireEvent.click(screen.getByRole('button', { name: '上移透视指标 3' }))
    fireEvent.click(screen.getByRole('button', { name: '上移透视指标 2' }))

    expect(screen.getByLabelText('透视输出列预览')).toHaveTextContent('搜索 营收 / 搜索 成本 / 搜索 转化数 / 信息流 营收')
    expect(screen.getByLabelText('透视预览单元格 K3')).toHaveTextContent('华东')
    expect(screen.getByLabelText('透视预览单元格 L3')).toHaveTextContent('700')
    expect(screen.getByLabelText('透视预览单元格 M3')).toHaveTextContent('100')
    expect(screen.getByLabelText('透视预览单元格 N3')).toHaveTextContent('1')
    expect(screen.getByLabelText('透视预览单元格 O3')).toHaveTextContent('260')

    fireEvent.click(screen.getByRole('button', { name: '生成多指标透视' }))
    fireEvent.click(screen.getByRole('button', { name: '保存电子表格草稿' }))

    await waitFor(() => expect(biApi.saveSpreadsheetDraft).toHaveBeenCalledWith(
      'campaign-sheet',
      expect.objectContaining({
        sheets: [
          expect.objectContaining({
            pivotTables: [
              expect.objectContaining({
                valueFields: [
                  { field: '收入', aggregation: 'SUM', label: '营收' },
                  { field: '花费', aggregation: 'SUM', label: '成本' },
                  { field: '转化', aggregation: 'COUNT', label: '转化数' },
                ],
              }),
            ],
            cells: expect.objectContaining({
              L2: '搜索 营收',
              M2: '搜索 成本',
              N2: '搜索 转化数',
              O2: '信息流 营收',
              P2: '信息流 成本',
              Q2: '信息流 转化数',
            }),
          }),
        ],
      }),
      null,
    ))
  }, 30000)

  it('saves edited chart query fields and copies chart drafts from the resource workbench', async () => {
    vi.mocked(biApi.saveChartDraft).mockImplementation((_chartKey: string, resource) => Promise.resolve(ok(resource)))
    vi.mocked(biApi.getChartReferenceImpact).mockResolvedValue(ok({
      chartKey: 'trend-executions',
      chartName: 'Trend Executions',
      datasetKey: 'campaign_daily_stats',
      dashboards: [
        {
          dashboardKey: 'server-board',
          title: 'Server Board',
          widgetKey: 'server-widget',
          widgetTitle: 'Server Widget',
          status: 'PUBLISHED',
        },
      ],
      portals: [
        {
          portalKey: 'server-portal',
          name: 'Server Portal',
          menuKey: 'server-menu',
          menuTitle: 'Server Menu',
          status: 'PUBLISHED',
        },
      ],
      subscriptions: [
        {
          subscriptionKey: 'server-daily',
          name: 'Server Daily',
          enabled: true,
        },
      ],
    }))
    vi.mocked(biApi.listPortalResources).mockResolvedValue(ok([
      {
        portalKey: 'executive-home',
        name: 'Executive Home',
        theme: { theme: 'light', navigationLayout: 'top', defaultMenuKey: 'kpi-menu' },
        menus: [
          { menuKey: 'kpi-menu', title: 'KPI Menu', resourceType: 'CHART', resourceKey: 'trend-executions', visibility: {}, sortOrder: 1 },
        ],
        status: 'DRAFT',
        source: 'PERSISTED',
      },
    ]))
    vi.mocked(biApi.listSubscriptions).mockResolvedValue(ok([
      {
        id: 71,
        tenantId: 1,
        workspaceId: 1,
        subscriptionKey: 'trend-daily',
        name: 'Trend Daily',
        resourceType: 'CHART',
        resourceKey: 'trend-executions',
        resourceId: 0,
        schedule: {},
        receivers: {},
        delivery: {},
        enabled: true,
      },
    ]))

    renderBiRoute('/bi')

    fireEvent.click(await screen.findByRole('tab', { name: '图表' }))
    fireEvent.change(await screen.findByLabelText('图表类型'), {
      target: { value: 'BAR' },
    })
    fireEvent.change(screen.getByLabelText('图表数据集'), {
      target: { value: 'campaign_daily_stats' },
    })
    fireEvent.change(screen.getByLabelText('图表维度字段'), {
      target: { value: 'channel,stat_date' },
    })
    fireEvent.change(screen.getByLabelText('图表指标字段'), {
      target: { value: 'send_count,conversion_count' },
    })
    fireEvent.change(screen.getByLabelText('图表筛选字段'), {
      target: { value: 'channel' },
    })
    fireEvent.change(screen.getByLabelText('图表筛选值'), {
      target: { value: 'SMS,Email' },
    })
    fireEvent.change(screen.getByLabelText('图表排序字段'), {
      target: { value: 'stat_date' },
    })
    fireEvent.change(screen.getByLabelText('图表排序方向'), {
      target: { value: 'DESC' },
    })
    fireEvent.change(screen.getByLabelText('图表取数上限'), {
      target: { value: '250' },
    })
    fireEvent.change(screen.getByLabelText('图表主题'), {
      target: { value: 'screen-dark' },
    })
    fireEvent.change(screen.getByLabelText('图表密度'), {
      target: { value: 'spacious' },
    })
    fireEvent.change(screen.getByLabelText('图表调色板'), {
      target: { value: 'green' },
    })
    fireEvent.click(screen.getByLabelText('图表图例'))
    fireEvent.click(screen.getByLabelText('图表数据标签'))
    fireEvent.change(screen.getByLabelText('图表X轴标题'), {
      target: { value: '统计日期' },
    })
    fireEvent.change(screen.getByLabelText('图表Y轴标题'), {
      target: { value: '发送与转化' },
    })
    fireEvent.change(screen.getByLabelText('图表标签位置'), {
      target: { value: 'inside' },
    })
    fireEvent.change(screen.getByLabelText('图表数字格式'), {
      target: { value: '0,0.0' },
    })
    fireEvent.change(screen.getByLabelText('图表条件格式字段'), {
      target: { value: 'conversion_count' },
    })
    fireEvent.change(screen.getByLabelText('图表条件格式操作符'), {
      target: { value: 'GTE' },
    })
    fireEvent.change(screen.getByLabelText('图表条件格式阈值'), {
      target: { value: '100' },
    })
    fireEvent.change(screen.getByLabelText('图表条件格式颜色'), {
      target: { value: '#16a34a' },
    })
    fireEvent.click(screen.getByLabelText('图表钻取'))
    fireEvent.change(screen.getByLabelText('图表联动目标'), {
      target: { value: 'detail-canvas' },
    })
    fireEvent.change(screen.getByLabelText('图表跳转模板'), {
      target: { value: '/canvas/{canvas_id}/stats' },
    })
    expect(await screen.findByText('引用影响：数据集 campaign_daily_stats · 2 维度 · 2 指标 · 仪表板 Server Board/Server Widget · 门户 Server Portal/Server Menu · 订阅 Server Daily')).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: '保存图表草稿' }))

    await waitFor(() => expect(biApi.saveChartDraft).toHaveBeenCalledWith(
      'trend-executions',
      expect.objectContaining({
        chartKey: 'trend-executions',
        name: 'Trend Executions',
        chartType: 'BAR',
        datasetKey: 'campaign_daily_stats',
        query: expect.objectContaining({
          datasetKey: 'campaign_daily_stats',
          dimensions: ['channel', 'stat_date'],
          metrics: ['send_count', 'conversion_count'],
          filters: [{ field: 'channel', operator: 'IN', value: ['SMS', 'Email'] }],
          sorts: [{ field: 'stat_date', direction: 'DESC' }],
          limit: 250,
        }),
        style: {
          theme: 'screen-dark',
          density: 'spacious',
          palette: 'green',
          legendVisible: false,
          dataLabelsVisible: true,
          axis: {
            xTitle: '统计日期',
            yTitle: '发送与转化',
          },
          labels: {
            position: 'inside',
            numberFormat: '0,0.0',
          },
          conditionalFormats: [
            {
              field: 'conversion_count',
              operator: 'GTE',
              value: '100',
              color: '#16a34a',
            },
          ],
        },
        interaction: {
          drillEnabled: false,
          linkageTarget: 'detail-canvas',
          hyperlinkTemplate: '/canvas/{canvas_id}/stats',
        },
      }),
      null,
    ))

    fireEvent.click(screen.getByRole('button', { name: '复制图表草稿' }))
    fireEvent.click(screen.getByRole('button', { name: '保存图表草稿' }))

    await waitFor(() => expect(biApi.saveChartDraft).toHaveBeenLastCalledWith(
      'trend-executions-copy',
      expect.objectContaining({
        chartKey: 'trend-executions-copy',
        name: 'Trend Executions 副本',
        status: 'DRAFT',
        source: 'PERSISTED',
      }),
      null,
    ))
    expect(screen.getByText('引用影响：数据集 campaign_daily_stats · 2 维度 · 2 指标 · 仪表板 Server Board/Server Widget · 门户 Server Portal/Server Menu · 订阅 Server Daily')).toBeInTheDocument()
  }, 30000)

  it('adds chart query fields from the field drop builder before saving draft', async () => {
    vi.mocked(biApi.listDatasets).mockResolvedValue(ok([
      {
        datasetKey: 'canvas_daily_stats',
        fields: [
          { fieldKey: 'stat_date', role: 'DIMENSION', dataType: 'DATE' },
          { fieldKey: 'channel', role: 'DIMENSION', dataType: 'STRING' },
          { fieldKey: 'send_count', role: 'MEASURE', dataType: 'NUMBER' },
        ],
        metrics: [
          { metricKey: 'total_executions', dataType: 'NUMBER' },
          { metricKey: 'send_count', dataType: 'NUMBER' },
        ],
      },
    ]))
    vi.mocked(biApi.saveChartDraft).mockImplementation((_chartKey: string, resource) => Promise.resolve(ok(resource)))

    renderBiRoute('/bi')

    fireEvent.click(await screen.findByRole('tab', { name: '图表' }))
    fireEvent.click(await screen.findByLabelText('添加图表维度字段 channel'))
    fireEvent.click(screen.getByLabelText('添加图表指标字段 send_count'))
    fireEvent.click(screen.getByRole('button', { name: '保存图表草稿' }))

    await waitFor(() => expect(biApi.saveChartDraft).toHaveBeenCalledWith(
      'trend-executions',
      expect.objectContaining({
        query: expect.objectContaining({
          dimensions: ['stat_date', 'channel'],
          metrics: ['total_executions', 'send_count'],
        }),
      }),
      null,
    ))
  }, 30000)

  it('saves portal navigation settings and menu order from the resource workbench', async () => {
    vi.mocked(biApi.savePortalDraft).mockImplementation((_portalKey: string, resource) => Promise.resolve(ok(resource)))

    renderBiRoute('/bi')

    fireEvent.change(await screen.findByLabelText('门户导航布局'), {
      target: { value: 'dual' },
    })
    fireEvent.change(screen.getByLabelText('门户默认主页'), {
      target: { value: 'sales' },
    })
    fireEvent.click(screen.getByLabelText('门户菜单搜索'))
    fireEvent.click(screen.getByLabelText('门户全屏'))
    fireEvent.click(screen.getByLabelText('门户移动端'))
    fireEvent.change(screen.getByLabelText('门户LOGO'), {
      target: { value: 'https://cdn.example.test/logo.svg' },
    })
    fireEvent.change(screen.getByLabelText('门户主标题'), {
      target: { value: 'Executive Portal' },
    })
    fireEvent.change(screen.getByLabelText('门户副标题'), {
      target: { value: 'Daily growth cockpit' },
    })
    fireEvent.change(screen.getByLabelText('门户页脚'), {
      target: { value: 'Data Ops 2026' },
    })
    fireEvent.change(screen.getByLabelText('门户别名'), {
      target: { value: 'exec-growth' },
    })
    fireEvent.click(screen.getByLabelText('门户面包屑'))
    fireEvent.click(screen.getByLabelText('门户菜单缓存'))
    fireEvent.change(screen.getByLabelText('门户缓存TTL'), {
      target: { value: '900' },
    })
    fireEvent.change(screen.getByLabelText('门户菜单'), {
      target: { value: 'sales' },
    })
    fireEvent.change(screen.getByLabelText('门户菜单标题'), {
      target: { value: 'Sales Performance' },
    })
    fireEvent.change(screen.getByLabelText('门户父级菜单'), {
      target: { value: 'overview' },
    })
    fireEvent.change(screen.getByLabelText('门户菜单图标'), {
      target: { value: 'line-chart' },
    })
    fireEvent.click(screen.getByRole('button', { name: '门户菜单上移' }))
    fireEvent.change(screen.getByLabelText('门户拖拽菜单'), {
      target: { value: 'overview' },
    })
    fireEvent.change(screen.getByLabelText('门户拖放目标菜单'), {
      target: { value: 'sales' },
    })
    fireEvent.change(screen.getByLabelText('门户拖放位置'), {
      target: { value: 'inside' },
    })
    fireEvent.click(screen.getByRole('button', { name: '应用门户菜单拖放' }))
    expect(screen.getByText('门户预览')).toBeInTheDocument()
    expect(screen.getByText('Executive Portal')).toBeInTheDocument()
    expect(screen.getAllByText(/Sales Performance/).length).toBeGreaterThan(0)
    fireEvent.click(screen.getByRole('button', { name: '生成门户嵌入' }))
    await waitFor(() => expect(biApi.createEmbedTicket).toHaveBeenCalledWith(expect.objectContaining({
      resourceType: 'PORTAL',
      resourceKey: 'executive-home',
      scope: 'INTERNAL_CANVAS',
      ttlSeconds: 900,
    })))
    fireEvent.click(screen.getByRole('button', { name: '保存门户草稿' }))

    await waitFor(() => expect(biApi.savePortalDraft).toHaveBeenCalledWith(
      'executive-home',
      expect.objectContaining({
        portalKey: 'executive-home',
        theme: expect.objectContaining({
          theme: 'light',
          navigationLayout: 'dual',
          defaultMenuKey: 'sales',
          menuSearchEnabled: true,
          fullScreenEnabled: true,
          mobileEnabled: true,
          logoUrl: 'https://cdn.example.test/logo.svg',
          title: 'Executive Portal',
          subtitle: 'Daily growth cockpit',
          footerText: 'Data Ops 2026',
          alias: 'exec-growth',
          breadcrumbEnabled: true,
          menuCacheEnabled: true,
          menuCacheTtlSeconds: 900,
        }),
        menus: [
          expect.objectContaining({
            menuKey: 'sales',
            title: 'Sales Performance',
            parentMenuKey: 'overview',
            sortOrder: 1,
            visibility: expect.objectContaining({ iconKey: 'line-chart' }),
          }),
          expect.objectContaining({ menuKey: 'overview', parentMenuKey: 'sales', sortOrder: 2 }),
        ],
      }),
      null,
    ))
  }, 30000)

  it('duplicates the selected dashboard widget from the keyboard', async () => {
    renderBiRoute('/bi?dashboard=canvas-effect&canvasId=12')

    const canvas = await screen.findByLabelText('QuickBI 仪表板画布')
    canvas.focus()
    fireEvent.keyDown(canvas, { key: 'd', ctrlKey: true })

    expect((await screen.findAllByText(/副本/)).length).toBeGreaterThan(0)
  }, 30000)

  it('creates embed tickets with the current dashboard runtime parameters', async () => {
    renderBiRoute('/bi?dashboard=canvas-effect&canvasId=12&filter-trigger-type=TIME,MQ&filter-canvas=Welcome%20Journey')

    expect(await screen.findByRole('heading', { name: '数据分析工作台' })).toBeInTheDocument()
    fireEvent.click(screen.getByRole('tab', { name: '交互' }))
    expect(await screen.findByText('嵌入参数预览')).toBeInTheDocument()
    expect(screen.getByText('过滤 filter-canvas：Welcome Journey')).toBeInTheDocument()
    expect(screen.getByText('过滤 filter-trigger-type：TIME,MQ')).toBeInTheDocument()
    fireEvent.click(screen.getAllByRole('button', { name: /嵌入/ })[0])

    await waitFor(() => expect(biApi.createEmbedTicket).toHaveBeenCalledWith(expect.objectContaining({
      resourceType: 'DASHBOARD',
      resourceKey: 'canvas-effect',
      scope: 'INTERNAL_CANVAS',
      filters: expect.objectContaining({
        canvasId: '12',
        'filter-stat-date': expect.stringMatching(/^\d{4}-\d{2}-\d{2},\d{4}-\d{2}-\d{2}$/),
        'filter-canvas': 'Welcome Journey',
        'filter-trigger-type': 'TIME,MQ',
      }),
      ttlSeconds: 600,
    })))
  }, 30000)

  it('saves edited dashboard runtime controls from the interaction panel', async () => {
    renderBiRoute('/bi?dashboard=canvas-effect&canvasId=12')

    expect(await screen.findByRole('heading', { name: '数据分析工作台' })).toBeInTheDocument()
    fireEvent.click(screen.getByRole('tab', { name: '交互' }))
    fireEvent.change(await screen.findByLabelText('画布名称运行参数'), {
      target: { value: 'Welcome Journey' },
    })

    await waitFor(() => expect(biApi.saveDashboardRuntimeState).toHaveBeenCalledWith('canvas-effect', {
      parameters: expect.objectContaining({
        'filter-stat-date': expect.any(Array),
        'filter-canvas': 'Welcome Journey',
      }),
    }))
  }, 30000)

  it('edits typed dashboard runtime controls from the interaction panel', async () => {
    vi.mocked(biApi.executeQuery).mockImplementation(request => {
      const dimension = request.dimensions?.[0]
      return Promise.resolve(ok({
        datasetKey: request.datasetKey,
        columns: dimension ? [{ key: dimension, role: 'DIMENSION', dataType: 'STRING' }] : [],
        rows: dimension === 'trigger_type'
          ? [{ trigger_type: 'TIME' }, { trigger_type: 'MQ' }]
          : [],
        rowCount: dimension === 'trigger_type' ? 2 : 0,
        durationMs: 1,
        sqlHash: `option-${dimension ?? 'widget'}`,
        cached: false,
      }))
    })

    renderBiRoute('/bi?dashboard=canvas-effect&canvasId=12&filter-stat-date=2026-06-01,2026-06-06')

    expect(await screen.findByRole('heading', { name: '数据分析工作台' })).toBeInTheDocument()
    fireEvent.click(screen.getByRole('tab', { name: '交互' }))
    fireEvent.change(await screen.findByLabelText('统计日期开始日期'), {
      target: { value: '2026-06-02' },
    })
    fireEvent.change(await screen.findByLabelText('统计日期结束日期'), {
      target: { value: '2026-06-08' },
    })
    fireEvent.click(await screen.findByRole('button', { name: '选择触发方式 TIME' }))
    fireEvent.click(await screen.findByRole('button', { name: '选择触发方式 MQ' }))

    await waitFor(() => expect(biApi.saveDashboardRuntimeState).toHaveBeenCalledWith('canvas-effect', {
      parameters: expect.objectContaining({
        'filter-stat-date': ['2026-06-02', '2026-06-08'],
        'filter-trigger-type': ['TIME', 'MQ'],
      }),
    }))
  }, 30000)

  it('edits dashboard runtime controls from the canvas toolbar', async () => {
    renderBiRoute('/bi?dashboard=canvas-effect&canvasId=12')

    expect(await screen.findByRole('heading', { name: '数据分析工作台' })).toBeInTheDocument()
    fireEvent.change(await screen.findByLabelText('运行态画布名称'), {
      target: { value: 'Toolbar Journey' },
    })

    await waitFor(() => expect(biApi.saveDashboardRuntimeState).toHaveBeenCalledWith('canvas-effect', {
      parameters: expect.objectContaining({
        'filter-stat-date': expect.any(Array),
        'filter-canvas': 'Toolbar Journey',
      }),
    }))
  })

  it('shows dashboard runtime parameter source status in the editor', async () => {
    renderBiRoute('/bi?dashboard=canvas-effect&canvasId=12&canvas_name=Url%20Canvas')

    expect(await screen.findByText('画布名称：URL覆盖')).toBeInTheDocument()
    expect(await screen.findByText('统计日期：默认值')).toBeInTheDocument()
  })

  it('resets dashboard runtime controls to defaults from the canvas toolbar', async () => {
    renderBiRoute('/bi?dashboard=canvas-effect&canvasId=12&filter-canvas=Welcome%20Journey&filter-trigger-type=TIME,MQ')

    expect(await screen.findByRole('heading', { name: '数据分析工作台' })).toBeInTheDocument()
    vi.mocked(biApi.saveDashboardRuntimeState).mockClear()
    fireEvent.click(await screen.findByRole('button', { name: '重置运行态参数' }))

    await waitFor(() => expect(biApi.saveDashboardRuntimeState).toHaveBeenCalled())
    const saveCalls = vi.mocked(biApi.saveDashboardRuntimeState).mock.calls
    const lastCall = saveCalls[saveCalls.length - 1]
    expect(lastCall).toEqual([
      'canvas-effect',
      {
        parameters: expect.objectContaining({
          'filter-stat-date': expect.any(Array),
        }),
      },
    ])
    expect(lastCall?.[1].parameters).not.toHaveProperty('filter-canvas')
    expect(lastCall?.[1].parameters).not.toHaveProperty('filter-trigger-type')
  })

  it('clears one dashboard runtime control from the canvas toolbar', async () => {
    renderBiRoute('/bi?dashboard=canvas-effect&canvasId=12&filter-canvas=Welcome%20Journey&filter-trigger-type=TIME,MQ')

    expect(await screen.findByRole('heading', { name: '数据分析工作台' })).toBeInTheDocument()
    vi.mocked(biApi.saveDashboardRuntimeState).mockClear()
    fireEvent.click(await screen.findByRole('button', { name: '清除运行态画布名称' }))

    await waitFor(() => expect(screen.getByLabelText('运行态画布名称')).toHaveValue(''))
    await waitFor(() => expect(biApi.saveDashboardRuntimeState).toHaveBeenCalled())
    const saveCalls = vi.mocked(biApi.saveDashboardRuntimeState).mock.calls
    const lastCall = saveCalls[saveCalls.length - 1]
    expect(lastCall).toEqual([
      'canvas-effect',
      {
        parameters: expect.objectContaining({
          'filter-stat-date': expect.any(Array),
          'filter-trigger-type': ['TIME', 'MQ'],
        }),
      },
    ])
    expect(lastCall?.[1].parameters).not.toHaveProperty('filter-canvas')
  })

  it('clears the active dataset query cache from the governance panel', async () => {
    vi.mocked(biApi.getQueryCacheStats).mockResolvedValue(ok({
      provider: 'memory',
      enabled: true,
      entryCount: 2,
      maxEntries: 500,
      ttlSeconds: 300,
      hitCount: 8,
      missCount: 2,
      putCount: 5,
      evictionCount: 1,
    }))
    vi.mocked(biApi.invalidateQueryCache).mockResolvedValue(ok({
      scope: 'DATASET',
      deletedEntries: 3,
      message: 'cleared dataset canvas_daily_stats',
    }))

    renderBiRoute('/bi?dashboard=canvas-effect&canvasId=12')

    expect(await screen.findByRole('heading', { name: '数据分析工作台' })).toBeInTheDocument()
    fireEvent.click(await screen.findByRole('button', { name: /清当前数据集/ }))

    await waitFor(() => expect(biApi.invalidateQueryCache).toHaveBeenCalledWith({
      scope: 'DATASET',
      datasetKey: 'canvas_daily_stats',
    }))
    expect(await screen.findByText('DATASET · 3 条')).toBeInTheDocument()
    expect(await screen.findByText('2/500 条 · TTL 300 秒')).toBeInTheDocument()
    expect(screen.getByText('80.0% · 命中 8 / 未命中 2')).toBeInTheDocument()
  })

  it('saves the active dashboard cache policy override from the governance panel', async () => {
    vi.mocked(biApi.updateQueryCachePolicy).mockResolvedValue(ok({
      defaultEnabled: true,
      defaultTtlSeconds: 300,
      defaultCacheMode: 'CACHE',
      resources: [
        {
          resourceType: 'DASHBOARD',
          resourceKey: 'canvas-effect',
          enabled: true,
          ttlSeconds: 300,
          cacheMode: 'CACHE',
        },
      ],
    }))

    renderBiRoute('/bi?dashboard=canvas-effect&canvasId=12')

    fireEvent.click(await screen.findByText('保存缓存'))

    await waitFor(() => expect(biApi.updateQueryCachePolicy).toHaveBeenCalledWith({
      defaultEnabled: true,
      defaultTtlSeconds: 300,
      defaultCacheMode: 'CACHE',
      resources: [
        {
          resourceType: 'DASHBOARD',
          resourceKey: 'canvas-effect',
          enabled: true,
          ttlSeconds: 300,
          cacheMode: 'CACHE',
        },
      ],
    }))
  })

  it('creates a datasource from the BI onboarding panel', async () => {
    vi.mocked(biApi.listDatasourceConnectors).mockResolvedValue(ok([
      {
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
        note: 'JDBC connector is available',
      },
    ]))
    vi.mocked(biApi.listDatasourceOnboarding)
      .mockResolvedValueOnce(ok([]))
      .mockResolvedValue(ok([
        {
          id: 77,
          sourceKey: 'jdbc-77',
          name: 'Marketing Warehouse',
          type: 'JDBC',
          connectorType: 'MYSQL',
          enabled: true,
          driverClassName: 'com.mysql.cj.jdbc.Driver',
          maskedUrl: 'jdbc:mysql://warehouse.example.com:3306/marketing',
          maskedUsername: 'ca***pp',
          connectionMode: 'DIRECT_QUERY',
          schemaSyncStatus: 'NOT_SYNCED',
          tableCount: 0,
          lastSyncedAt: null,
          supportedModes: ['DIRECT_QUERY', 'CACHE'],
          supportStatus: 'AVAILABLE',
          capabilities: ['CONNECTION_TEST', 'SCHEMA_SYNC'],
        },
      ]))
    vi.mocked(biApi.createDatasourceOnboarding).mockResolvedValue(ok({
      id: 77,
      sourceKey: 'jdbc-77',
      name: 'Marketing Warehouse',
      type: 'JDBC',
      connectorType: 'MYSQL',
      enabled: true,
      driverClassName: 'com.mysql.cj.jdbc.Driver',
      maskedUrl: 'jdbc:mysql://warehouse.example.com:3306/marketing',
      maskedUsername: 'ca***pp',
      connectionMode: 'DIRECT_QUERY',
      schemaSyncStatus: 'NOT_SYNCED',
      tableCount: 0,
      lastSyncedAt: null,
      supportedModes: ['DIRECT_QUERY', 'CACHE'],
      supportStatus: 'AVAILABLE',
      capabilities: ['CONNECTION_TEST', 'SCHEMA_SYNC'],
    }))

    renderBiRoute('/bi')

    const modeSelect = screen.getAllByLabelText('BI数据源连接模式')[0]
    fireEvent.mouseDown(modeSelect.querySelector('.ant-select-selector') ?? modeSelect)
    const cacheOptions = await screen.findAllByText('查询缓存')
    fireEvent.click(cacheOptions[cacheOptions.length - 1])
    fireEvent.click(screen.getByRole('button', { name: '下一步' }))

    fireEvent.change(await screen.findByLabelText('BI数据源名称'), {
      target: { value: 'Marketing Warehouse' },
    })
    fireEvent.change(screen.getByLabelText('BI数据源URL'), {
      target: { value: 'jdbc:mysql://warehouse.example.com:3306/marketing' },
    })
    fireEvent.change(screen.getByLabelText('BI数据源账号'), {
      target: { value: 'canvas_app' },
    })
    fireEvent.change(screen.getByLabelText('BI数据源密码'), {
      target: { value: 'plain-password' },
    })
    fireEvent.click(screen.getByRole('button', { name: '下一步' }))
    const onboardingCallsBeforeCreate = vi.mocked(biApi.listDatasourceOnboarding).mock.calls.length
    fireEvent.click(screen.getByRole('button', { name: '保存数据源' }))

    await waitFor(() => expect(biApi.createDatasourceOnboarding).toHaveBeenCalledWith({
      connectorType: 'MYSQL',
      name: 'Marketing Warehouse',
      url: 'jdbc:mysql://warehouse.example.com:3306/marketing',
      username: 'canvas_app',
      password: 'plain-password',
      driverClassName: 'com.mysql.cj.jdbc.Driver',
      description: '',
      enabled: true,
      connectionMode: 'CACHE',
    }))
    await waitFor(() => expect(vi.mocked(biApi.listDatasourceOnboarding).mock.calls.length)
      .toBeGreaterThan(onboardingCallsBeforeCreate))
  }, 60000)

  it('renders QuickBI-style datasource next actions for API and file sources', async () => {
    vi.mocked(biApi.listDatasourceOnboarding).mockResolvedValue(ok([
      {
        id: 81,
        sourceKey: 'api-81',
        name: 'Orders API',
        type: 'API',
        connectorType: 'API',
        enabled: true,
        driverClassName: null,
        maskedUrl: 'https://api.example.com/orders',
        maskedUsername: 'Authorization',
        connectionMode: 'EXTRACT',
        schemaSyncStatus: 'SUCCESS',
        tableCount: 1,
        lastSyncedAt: '2026-06-09T10:00:00',
        supportedModes: ['EXTRACT'],
        supportStatus: 'AVAILABLE',
        capabilities: ['TABLE_DATASET'],
      },
      {
        id: 91,
        sourceKey: 'file-91',
        name: 'Orders Upload',
        type: 'FILE',
        connectorType: 'CSV_EXCEL',
        enabled: true,
        driverClassName: null,
        maskedUrl: 'file://orders.csv',
        maskedUsername: '',
        connectionMode: 'EXTRACT',
        schemaSyncStatus: 'PENDING',
        tableCount: 0,
        lastSyncedAt: null,
        supportedModes: ['EXTRACT'],
        supportStatus: 'AVAILABLE',
        capabilities: ['TABLE_DATASET'],
      },
    ]))

    renderBiRoute('/bi')

    const datasourceSectionLabels = await screen.findAllByText('数据源')
    fireEvent.click(datasourceSectionLabels[0])

    expect(await screen.findByText('创建表数据集并配置抽取刷新')).toBeInTheDocument()
    expect(screen.getByText('上传/预览文件后同步 schema，再创建表数据集')).toBeInTheDocument()
    expect(screen.getByText('API 数据源不进入自助取数；直连小数据量受 10MB/100 列/1000 行约束')).toBeInTheDocument()
    expect(screen.getByText('文件数据源适合探索空间和报表分析；自助取数需使用非探索空间数据集')).toBeInTheDocument()
  }, 60000)

  it('renders datasource capacity policies for API app and exploration-space file connectors', async () => {
    vi.mocked(biApi.listDatasourceConnectors).mockResolvedValue(ok([
      {
        connectorType: 'API',
        label: 'API',
        sourceCategory: 'HTTP',
        supportedModes: ['EXTRACT'],
        supportStatus: 'AVAILABLE',
        capacityCategory: 'HTTP_EXTRACT_SMALL',
        capacityNote: 'HTTP extract capacity',
        supportsConnectionTest: false,
        supportsSchemaSync: false,
        supportsSqlDataset: false,
        supportsTableDataset: true,
        supportsCredentials: true,
        driverClassNames: [],
        note: 'HTTP API extract connector',
      },
      {
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
        note: 'Application datasource',
      },
      {
        connectorType: 'CSV_EXCEL',
        label: 'CSV / Excel',
        sourceCategory: 'FILE',
        supportedModes: ['EXTRACT'],
        supportStatus: 'AVAILABLE',
        capacityCategory: 'FILE_EXTRACT_SMALL',
        capacityNote: 'Uploaded-file extract capacity',
        supportsConnectionTest: false,
        supportsSchemaSync: false,
        supportsSqlDataset: false,
        supportsTableDataset: true,
        supportsCredentials: false,
        driverClassNames: [],
        note: 'File upload connector',
      },
    ]))

    renderBiRoute('/bi')

    const datasourceSectionLabels = await screen.findAllByText('数据源')
    fireEvent.click(datasourceSectionLabels[0])

    expect(await screen.findByText('HTTP 抽取小流量池')).toBeInTheDocument()
    expect(screen.getByText('应用抽取小流量池')).toBeInTheDocument()
    expect(screen.getByText('探索空间文件池')).toBeInTheDocument()
    expect(screen.getByText('直连预览上限 10MB / 100 列 / 1000 行；抽取分页默认每页 1000 行')).toBeInTheDocument()
    expect(screen.getByText('探索空间上传源不支持自助取数')).toBeInTheDocument()
  }, 60000)

  it('renders datasource advanced capability support matrix', async () => {
    vi.mocked(biApi.listDatasourceConnectors).mockResolvedValue(ok([
      {
        connectorType: 'API',
        label: 'API',
        sourceCategory: 'HTTP',
        supportedModes: ['EXTRACT'],
        supportStatus: 'AVAILABLE',
        capacityCategory: 'HTTP_EXTRACT_SMALL',
        supportsConnectionTest: false,
        supportsSchemaSync: false,
        supportsSqlDataset: false,
        supportsTableDataset: true,
        supportsCredentials: true,
        driverClassNames: [],
        note: 'HTTP API extract connector',
      },
      {
        connectorType: 'CSV_EXCEL',
        label: 'CSV / Excel',
        sourceCategory: 'FILE',
        supportedModes: ['EXTRACT'],
        supportStatus: 'AVAILABLE',
        capacityCategory: 'FILE_EXTRACT_SMALL',
        supportsConnectionTest: false,
        supportsSchemaSync: false,
        supportsSqlDataset: false,
        supportsTableDataset: true,
        supportsCredentials: false,
        driverClassNames: [],
        note: 'File upload connector',
      },
      {
        connectorType: 'MAXCOMPUTE',
        label: 'MaxCompute',
        sourceCategory: 'ALIBABA_CLOUD',
        supportedModes: ['EXTRACT'],
        supportStatus: 'PLANNED',
        capacityCategory: 'WAREHOUSE_EXTRACT',
        supportsConnectionTest: false,
        supportsSchemaSync: false,
        supportsSqlDataset: false,
        supportsTableDataset: false,
        supportsCredentials: true,
        driverClassNames: [],
        note: 'Native connector planned',
      },
    ]))

    renderBiRoute('/bi')

    const datasourceSectionLabels = await screen.findAllByText('数据源')
    fireEvent.click(datasourceSectionLabels[0])

    expect(await screen.findByText('必需 · HTTP JSON 仅通过抽取物化分析')).toBeInTheDocument()
    expect(screen.getByText('受限 · 探索空间文件需物化后再治理关联')).toBeInTheDocument()
    expect(screen.getByText('阻断 · 连接器未开放前不可发布跨源模型')).toBeInTheDocument()
    expect(screen.getByText('中 · 受 10MB/100 列/1000 行预览和源端限流约束')).toBeInTheDocument()
  }, 60000)

  it('guides datasource onboarding through connector, connection, and review steps', async () => {
    vi.mocked(biApi.listDatasourceConnectors).mockResolvedValue(ok([
      {
        connectorType: 'DORIS',
        label: 'Apache Doris',
        sourceCategory: 'JDBC',
        supportedModes: ['DIRECT_QUERY', 'EXTRACT'],
        supportStatus: 'AVAILABLE',
        supportsConnectionTest: true,
        supportsSchemaSync: true,
        supportsSqlDataset: true,
        supportsTableDataset: true,
        supportsCredentials: true,
        driverClassNames: ['com.mysql.cj.jdbc.Driver'],
        note: 'Doris connector is available',
      },
    ]))
    vi.mocked(biApi.listDatasourceOnboarding)
      .mockResolvedValueOnce(ok([]))
      .mockResolvedValue(ok([
        {
          id: 88,
          sourceKey: 'jdbc-88',
          name: 'Doris Campaign Mart',
          type: 'JDBC',
          connectorType: 'DORIS',
          enabled: true,
          driverClassName: 'com.mysql.cj.jdbc.Driver',
          maskedUrl: 'jdbc:mysql://doris.example.com:9030/ads',
          maskedUsername: 'bi***op',
          connectionMode: 'EXTRACT',
          schemaSyncStatus: 'NOT_SYNCED',
          tableCount: 0,
          lastSyncedAt: null,
          supportedModes: ['DIRECT_QUERY', 'EXTRACT'],
          supportStatus: 'AVAILABLE',
          capabilities: ['CONNECTION_TEST', 'SCHEMA_SYNC', 'TABLE_DATASET'],
        },
      ]))
    vi.mocked(biApi.createDatasourceOnboarding).mockResolvedValue(ok({
      id: 88,
      sourceKey: 'jdbc-88',
      name: 'Doris Campaign Mart',
      type: 'JDBC',
      connectorType: 'DORIS',
      enabled: true,
      driverClassName: 'com.mysql.cj.jdbc.Driver',
      maskedUrl: 'jdbc:mysql://doris.example.com:9030/ads',
      maskedUsername: 'bi***op',
      connectionMode: 'EXTRACT',
      schemaSyncStatus: 'NOT_SYNCED',
      tableCount: 0,
      lastSyncedAt: null,
      supportedModes: ['DIRECT_QUERY', 'EXTRACT'],
      supportStatus: 'AVAILABLE',
      capabilities: ['CONNECTION_TEST', 'SCHEMA_SYNC', 'TABLE_DATASET'],
    }))

    renderBiRoute('/bi')

    expect(await screen.findByText('连接器配置')).toBeInTheDocument()
    const modeSelect = screen.getAllByLabelText('BI数据源连接模式')[0]
    fireEvent.mouseDown(modeSelect.querySelector('.ant-select-selector') ?? modeSelect)
    const extractModeOptions = await screen.findAllByText('抽取加速')
    fireEvent.click(extractModeOptions[extractModeOptions.length - 1])
    fireEvent.click(screen.getByRole('button', { name: '下一步' }))

    expect(await screen.findByText('连接凭证')).toBeInTheDocument()
    fireEvent.change(screen.getByLabelText('BI数据源名称'), {
      target: { value: 'Doris Campaign Mart' },
    })
    fireEvent.change(screen.getByLabelText('BI数据源URL'), {
      target: { value: 'jdbc:mysql://doris.example.com:9030/ads' },
    })
    fireEvent.change(screen.getByLabelText('BI数据源账号'), {
      target: { value: 'bi_operator' },
    })
    fireEvent.change(screen.getByLabelText('BI数据源密码'), {
      target: { value: 'plain-password' },
    })
    fireEvent.click(screen.getByRole('button', { name: '下一步' }))

    expect(await screen.findByText('接入复核')).toBeInTheDocument()
    expect(screen.getAllByText('Doris Campaign Mart').length).toBeGreaterThan(0)
    fireEvent.click(screen.getByRole('button', { name: '保存数据源' }))

    await waitFor(() => expect(biApi.createDatasourceOnboarding).toHaveBeenCalledWith({
      connectorType: 'DORIS',
      name: 'Doris Campaign Mart',
      url: 'jdbc:mysql://doris.example.com:9030/ads',
      username: 'bi_operator',
      password: 'plain-password',
      driverClassName: 'com.mysql.cj.jdbc.Driver',
      description: '',
      enabled: true,
      connectionMode: 'EXTRACT',
    }))
  }, 60000)

  it('saves an API datasource with HTTP extract connector config', async () => {
    vi.mocked(biApi.listDatasourceConnectors).mockResolvedValue(ok([
      {
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
        note: 'HTTP API extract connector',
      },
    ]))
    vi.mocked(biApi.listDatasourceOnboarding)
      .mockResolvedValueOnce(ok([]))
      .mockResolvedValue(ok([
        {
          id: 81,
          sourceKey: 'api-81',
          name: 'Orders API',
          type: 'API',
          connectorType: 'API',
          enabled: true,
          driverClassName: 'HTTP_JSON',
          maskedUrl: 'https://api.example.com/orders?token=***',
          maskedUsername: 'Au***on',
          connectionMode: 'EXTRACT',
          schemaSyncStatus: 'NOT_SYNCED',
          tableCount: 0,
          lastSyncedAt: null,
          supportedModes: ['EXTRACT'],
          supportStatus: 'AVAILABLE',
          capabilities: ['TABLE_DATASET', 'CREDENTIALS'],
        },
      ]))
    vi.mocked(biApi.createDatasourceOnboarding).mockResolvedValue(ok({
      id: 81,
      sourceKey: 'api-81',
      name: 'Orders API',
      type: 'API',
      connectorType: 'API',
      enabled: true,
      driverClassName: 'HTTP_JSON',
      maskedUrl: 'https://api.example.com/orders?token=***',
      maskedUsername: 'Au***on',
      connectionMode: 'EXTRACT',
      schemaSyncStatus: 'NOT_SYNCED',
      tableCount: 0,
      lastSyncedAt: null,
      supportedModes: ['EXTRACT'],
      supportStatus: 'AVAILABLE',
      capabilities: ['TABLE_DATASET', 'CREDENTIALS'],
    }))

    renderBiRoute('/bi')

    await waitFor(() => expect(screen.getAllByLabelText('BI API请求方法')[0]).toBeInTheDocument(), { timeout: 5000 })
    expect(screen.getAllByLabelText('BI API认证类型')[0]).toBeInTheDocument()
    fireEvent.change(screen.getByLabelText('BI API响应行路径'), {
      target: { value: '$.data.items' },
    })
    fireEvent.click(screen.getByRole('button', { name: '下一步' }))

    fireEvent.change(await screen.findByLabelText('BI数据源名称'), {
      target: { value: 'Orders API' },
    })
    fireEvent.change(screen.getByLabelText('BI数据源URL'), {
      target: { value: 'https://api.example.com/orders?token=url-secret' },
    })
    fireEvent.change(screen.getByLabelText('BI数据源账号'), {
      target: { value: 'Authorization' },
    })
    fireEvent.change(screen.getByLabelText('BI数据源密码'), {
      target: { value: 'bearer-token' },
    })
    fireEvent.change(screen.getByLabelText('BI API请求头名称'), {
      target: { value: 'X-Tenant' },
    })
    fireEvent.change(screen.getByLabelText('BI API请求头值'), {
      target: { value: '{{tenantId}}' },
    })
    fireEvent.change(screen.getByLabelText('BI API参数名称'), {
      target: { value: 'page' },
    })
    fireEvent.change(screen.getByLabelText('BI API参数值'), {
      target: { value: '{{page}}' },
    })
    fireEvent.change(screen.getByLabelText('BI API请求体模板'), {
      target: { value: '{"campaign":"{{campaignId}}"}' },
    })
    fireEvent.click(screen.getByRole('button', { name: '下一步' }))
    await waitFor(() => expect(screen.getAllByText('Orders API').length).toBeGreaterThan(0))
    fireEvent.click(screen.getByRole('button', { name: '保存数据源' }))

    await waitFor(() => expect(biApi.createDatasourceOnboarding).toHaveBeenCalledWith({
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
        requestMethod: 'GET',
        authType: 'NONE',
        headers: [{ name: 'X-Tenant', value: '{{tenantId}}', variable: true }],
        parameters: [{ name: 'page', value: '{{page}}', variable: true }],
        bodyTemplate: '{"campaign":"{{campaignId}}"}',
        responseRowsPath: '$.data.items',
        responseFormat: 'JSON',
      },
    }))
  }, 30000)

  it('saves an app analytics datasource with app capacity category', async () => {
    vi.mocked(biApi.listDatasourceConnectors).mockResolvedValue(ok([
      {
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
        note: 'Application datasource',
      },
    ]))
    vi.mocked(biApi.listDatasourceOnboarding).mockResolvedValue(ok([]))
    vi.mocked(biApi.createDatasourceOnboarding).mockResolvedValue(ok({
      id: 82,
      sourceKey: 'api-82',
      name: 'Campaign App',
      type: 'API',
      connectorType: 'APP_ANALYTICS',
      enabled: true,
      driverClassName: 'HTTP_JSON',
      maskedUrl: 'https://app.example.com/openapi/campaigns',
      maskedUsername: 'Au***on',
      connectionMode: 'EXTRACT',
      schemaSyncStatus: 'NOT_SYNCED',
      tableCount: 0,
      lastSyncedAt: null,
      supportedModes: ['EXTRACT'],
      supportStatus: 'AVAILABLE',
      capabilities: ['TABLE_DATASET', 'CREDENTIALS'],
    }))

    renderBiRoute('/bi')

    expect(await screen.findByText('APP_EXTRACT_SMALL')).toBeInTheDocument()
    await waitFor(() => expect(screen.getAllByLabelText('BI API请求方法')[0]).toBeInTheDocument(), { timeout: 5000 })
    fireEvent.change(screen.getByLabelText('BI API响应行路径'), {
      target: { value: '$.campaigns' },
    })
    fireEvent.click(screen.getByRole('button', { name: '下一步' }))
    fireEvent.change(await screen.findByLabelText('BI数据源名称'), {
      target: { value: 'Campaign App' },
    })
    fireEvent.change(screen.getByLabelText('BI数据源URL'), {
      target: { value: 'https://app.example.com/openapi/campaigns' },
    })
    fireEvent.change(screen.getByLabelText('BI数据源账号'), {
      target: { value: 'Authorization' },
    })
    fireEvent.change(screen.getByLabelText('BI数据源密码'), {
      target: { value: 'app-token' },
    })
    fireEvent.click(screen.getByRole('button', { name: '下一步' }))
    expect(await screen.findByText('Campaign App')).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: '保存数据源' }))

    await waitFor(() => expect(biApi.createDatasourceOnboarding).toHaveBeenCalledWith({
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
        authType: 'NONE',
        headers: [],
        parameters: [],
        responseRowsPath: '$.campaigns',
        responseFormat: 'JSON',
      },
    }))
  }, 30000)

  it('saves a CSV Excel datasource without credential fields', async () => {
    vi.mocked(biApi.listDatasourceConnectors).mockResolvedValue(ok([
      {
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
        note: 'File upload onboarding',
      },
    ]))
    vi.mocked(biApi.listDatasourceOnboarding).mockResolvedValue(ok([]))
    vi.mocked(biApi.createDatasourceOnboarding).mockResolvedValue(ok({
      id: 91,
      sourceKey: 'file-91',
      name: 'Upload extract',
      type: 'FILE',
      connectorType: 'CSV_EXCEL',
      enabled: true,
      driverClassName: 'FILE_UPLOAD',
      maskedUrl: 'file://orders.xlsx',
      maskedUsername: 'fi***ad',
      connectionMode: 'EXTRACT',
      schemaSyncStatus: 'NOT_SYNCED',
      tableCount: 0,
      lastSyncedAt: null,
      supportedModes: ['EXTRACT'],
      supportStatus: 'AVAILABLE',
      capabilities: ['TABLE_DATASET'],
    }))

    renderBiRoute('/bi')

    await waitFor(() => expect(screen.getAllByLabelText('BI数据源连接模式')[0]).toBeInTheDocument(), { timeout: 5000 })
    const nextButton = screen.getByRole('button', { name: '下一步' })
    await waitFor(() => expect(nextButton).toBeEnabled())
    fireEvent.click(nextButton)
    fireEvent.change(await screen.findByLabelText('BI数据源名称'), {
      target: { value: 'Upload extract' },
    })
    expect(screen.queryByLabelText('BI数据源账号')).not.toBeInTheDocument()
    expect(screen.queryByLabelText('BI数据源密码')).not.toBeInTheDocument()
    fireEvent.change(screen.getByLabelText('BI文件名称'), {
      target: { value: 'orders.xlsx' },
    })
    const fileTypeSelect = screen.getAllByLabelText('BI文件类型')[0]
    fireEvent.mouseDown(fileTypeSelect.querySelector('.ant-select-selector') ?? fileTypeSelect)
    const xlsxOptions = await screen.findAllByText('Excel xlsx')
    fireEvent.click(xlsxOptions[xlsxOptions.length - 1])
    fireEvent.change(screen.getByLabelText('BI文件工作表'), {
      target: { value: 'Orders' },
    })
    fireEvent.change(screen.getByLabelText('BI文件分隔符'), {
      target: { value: '|' },
    })
    fireEvent.click(screen.getByRole('button', { name: '下一步' }))
    expect(await screen.findByText('Upload extract')).toBeInTheDocument()
    expect(screen.getByText('orders.xlsx')).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: '保存数据源' }))

    await waitFor(() => expect(biApi.createDatasourceOnboarding).toHaveBeenCalledWith({
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
    }))
  }, 30000)

  it('uploads and materializes a selected CSV Excel file datasource', async () => {
    vi.mocked(biApi.listDatasourceConnectors).mockResolvedValue(ok([
      {
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
        note: 'File upload onboarding',
      },
    ]))
    vi.mocked(biApi.listDatasourceOnboarding).mockResolvedValue(ok([]))
    vi.mocked(biApi.uploadAndMaterializeDatasourceFile).mockResolvedValue(ok({
      source: {
        id: 91,
        sourceKey: 'file-91',
        name: 'Upload extract',
        type: 'FILE',
        connectorType: 'CSV_EXCEL',
        enabled: true,
        driverClassName: 'FILE_UPLOAD',
        maskedUrl: 'file:///tmp/bi-datasource-uploads/tenant-7/orders.csv',
        maskedUsername: 'fi***ad',
        connectionMode: 'EXTRACT',
        schemaSyncStatus: 'SUCCESS',
        tableCount: 1,
        lastSyncedAt: '2026-06-06T10:15:30',
        supportedModes: ['EXTRACT'],
        supportStatus: 'AVAILABLE',
        capabilities: ['TABLE_DATASET'],
      },
      schemaSnapshot: {
        id: 202,
        dataSourceConfigId: 91,
        sourceKey: 'file-91',
        name: 'Upload extract',
        connectorType: 'CSV_EXCEL',
        syncStatus: 'SUCCESS',
        errorMessage: null,
        tableCount: 1,
        columnCount: 2,
        tables: [{
          name: 'orders',
          tableType: 'CSV',
          columns: [
            { name: 'order_id', typeName: 'VARCHAR', dataType: 12, nullable: true, ordinalPosition: 1 },
            { name: 'amount', typeName: 'DOUBLE', dataType: 8, nullable: true, ordinalPosition: 2 },
          ],
        }],
        syncedAt: '2026-06-06T10:15:30',
        syncedBy: 'alice',
      },
      dataset: {
        datasetKey: 'file_91_orders',
        name: 'Upload extract orders',
        datasetType: 'TABLE',
        tableExpression: 'orders',
        tenantColumn: 'tenant_id',
        model: { sourceKey: 'file-91', connectorType: 'CSV_EXCEL' },
        fields: [],
        metrics: [],
        status: 'DRAFT',
        source: 'DATASOURCE_SCHEMA',
      },
      accelerationPolicy: {
        datasetKey: 'file_91_orders',
        enabled: true,
        accelerationMode: 'EXTRACT',
        refreshMode: 'MANUAL',
        refreshIntervalMinutes: 60,
        ttlSeconds: 300,
        maxRows: 100000,
        cronExpression: null,
        materializedTable: 'bi_extract.t7_file_91_orders_20260606101530',
        lastStatus: 'SUCCESS',
        lastRunId: 31,
        lastRefreshedAt: '2026-06-06T10:15:31',
        recentRuns: [],
      },
      refreshRun: {
        id: 31,
        datasetKey: 'file_91_orders',
        status: 'SUCCESS',
        rowCount: 1,
        durationMs: 12,
        materializedTable: 'bi_extract.t7_file_91_orders_20260606101530',
        requestedBy: 'alice',
        startedAt: '2026-06-06T10:15:30',
        finishedAt: '2026-06-06T10:15:31',
        errorMessage: null,
      },
    }))
    const file = new File(['order_id,amount\nO-1,12.5\n'], 'orders.csv', { type: 'text/csv' })

    renderBiRoute('/bi')

    await waitFor(() => expect(screen.getAllByLabelText('BI数据源连接模式')[0]).toBeInTheDocument(), { timeout: 5000 })
    const nextButton = screen.getByRole('button', { name: '下一步' })
    await waitFor(() => expect(nextButton).toBeEnabled())
    fireEvent.click(nextButton)
    fireEvent.change(await screen.findByLabelText('BI数据源名称'), {
      target: { value: 'Upload extract' },
    })
    fireEvent.change(screen.getByLabelText('BI上传文件'), {
      target: { files: [file] },
    })
    fireEvent.click(screen.getByRole('button', { name: '下一步' }))
    expect(await screen.findByText('orders.csv')).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: '保存数据源' }))

    await waitFor(() => expect(biApi.uploadAndMaterializeDatasourceFile).toHaveBeenCalledWith(file, expect.objectContaining({
      name: 'Upload extract',
      description: '',
      delimiter: ',',
      headerRow: true,
      encoding: 'UTF-8',
      tenantColumn: 'tenant_id',
      schemaLimit: 200,
      maxRows: 100000,
    })))
    expect(biApi.createDatasourceOnboarding).not.toHaveBeenCalled()
  }, 30000)

  it('previews API datasource rows with request variables and limit from the datasource action', async () => {
    vi.mocked(biApi.listDatasourceOnboarding).mockResolvedValue(ok([
      {
        id: 81,
        sourceKey: 'api-81',
        name: 'Orders API',
        type: 'API',
        connectorType: 'API',
        enabled: true,
        driverClassName: 'HTTP_JSON',
        maskedUrl: 'https://api.example.com/orders',
        maskedUsername: 'Au***on',
        connectionMode: 'EXTRACT',
        schemaSyncStatus: 'NOT_SYNCED',
        tableCount: 0,
        lastSyncedAt: null,
        supportedModes: ['EXTRACT'],
        supportStatus: 'AVAILABLE',
        capabilities: ['TABLE_DATASET', 'CREDENTIALS'],
      },
    ]))
    vi.mocked(biApi.previewApiDatasource).mockResolvedValue(ok({
      id: 81,
      sourceKey: 'api-81',
      name: 'Orders API',
      connectorType: 'API',
      columns: [
        { key: 'order_id', role: 'DIMENSION', dataType: 'STRING' },
        { key: 'amount', role: 'MEASURE', dataType: 'NUMBER' },
      ],
      rows: [
        { order_id: 'O-1', amount: 12.5 },
      ],
      rowCount: 1,
      truncated: false,
      durationMs: 18,
      checkedAt: '2026-06-06T03:20:00',
    }))

    renderBiRoute('/bi')

    const datasourceSectionLabels = await screen.findAllByText('数据源')
    fireEvent.click(datasourceSectionLabels[0])
    fireEvent.change(await screen.findByLabelText('BI API预览变量名'), {
      target: { value: 'campaignId' },
    })
    fireEvent.change(screen.getByLabelText('BI API预览变量值'), {
      target: { value: 'cmp-1' },
    })
    fireEvent.click(screen.getByRole('button', { name: '新增BI API预览变量' }))
    fireEvent.change(screen.getByLabelText('BI API预览变量名 2'), {
      target: { value: 'tenantId' },
    })
    fireEvent.change(screen.getByLabelText('BI API预览变量值 2'), {
      target: { value: 'tenant-7' },
    })
    fireEvent.change(screen.getByLabelText('BI API预览行数'), {
      target: { value: '20' },
    })
    fireEvent.click(await screen.findByRole('button', { name: '预览数据' }))

    await waitFor(() => expect(biApi.previewApiDatasource).toHaveBeenCalledWith(81, {
      variables: { campaignId: 'cmp-1', tenantId: 'tenant-7' },
      limit: 20,
    }))
    expect(await screen.findByText('O-1')).toBeInTheDocument()
    expect(screen.getByText('1 行')).toBeInTheDocument()
  }, 20000)

  it('creates an API extract dataset from synced API schema using preview variables', async () => {
    const apiSchemaSnapshot = {
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
    }
    vi.mocked(biApi.listDatasourceOnboarding).mockResolvedValue(ok([
      {
        id: 81,
        sourceKey: 'api-81',
        name: 'Orders API',
        type: 'API',
        connectorType: 'API',
        enabled: true,
        driverClassName: 'HTTP_JSON',
        maskedUrl: 'https://api.example.com/orders',
        maskedUsername: 'Au***on',
        connectionMode: 'EXTRACT',
        schemaSyncStatus: 'NOT_SYNCED',
        tableCount: 0,
        lastSyncedAt: null,
        supportedModes: ['EXTRACT'],
        supportStatus: 'AVAILABLE',
        capabilities: ['TABLE_DATASET', 'SCHEMA_SYNC', 'CREDENTIALS'],
      },
    ]))
    vi.mocked(biApi.syncDatasourceSchema).mockResolvedValue(ok(apiSchemaSnapshot))
    vi.mocked(biApi.listDatasourceSchemaSnapshots).mockResolvedValue(ok([apiSchemaSnapshot]))
    vi.mocked(biApi.createDatasetFromDatasourceSchema).mockResolvedValue(ok({
      datasetKey: 'api_81_api_response',
      name: 'Orders API api_response',
      datasetType: 'TABLE',
      tableExpression: 'api_response',
      tenantColumn: 'tenant_id',
      model: { connectorType: 'API', apiResponseVariables: { campaignId: 'cmp-1', pageToken: 'page-2' } },
      fields: [],
      metrics: [],
      status: 'DRAFT',
      source: 'DATASOURCE_SCHEMA',
    }))

    renderBiRoute('/bi')

    const datasourceSectionLabels = await screen.findAllByText('数据源')
    fireEvent.click(datasourceSectionLabels[0])
    fireEvent.change(await screen.findByLabelText('BI API预览变量名'), {
      target: { value: 'campaignId' },
    })
    fireEvent.change(screen.getByLabelText('BI API预览变量值'), {
      target: { value: 'cmp-1' },
    })
    fireEvent.click(screen.getByRole('button', { name: '新增BI API预览变量' }))
    fireEvent.change(screen.getByLabelText('BI API预览变量名 2'), {
      target: { value: 'pageToken' },
    })
    fireEvent.change(screen.getByLabelText('BI API预览变量值 2'), {
      target: { value: 'page-2' },
    })
    fireEvent.change(screen.getByLabelText('BI API预览行数'), {
      target: { value: '20' },
    })
    fireEvent.click(screen.getByRole('button', { name: '同步 schema' }))
    await screen.findByText('api_response')
    fireEvent.click(screen.getByRole('button', { name: '生成数据集草稿 api_response' }))

    await waitFor(() => expect(biApi.syncDatasourceSchema).toHaveBeenCalledWith(81, 100, {
      variables: { campaignId: 'cmp-1', pageToken: 'page-2' },
      limit: 20,
    }))
    await waitFor(() => expect(biApi.createDatasetFromDatasourceSchema).toHaveBeenCalledWith({
      dataSourceConfigId: 81,
      tableName: 'api_response',
      datasetKey: 'api_81_api_response',
      name: 'Orders API api_response',
      tenantColumn: 'tenant_id',
      selectedColumns: ['order_id', 'amount', 'paid'],
      apiResponseVariables: { campaignId: 'cmp-1', pageToken: 'page-2' },
    }))
  }, 60000)

  it('creates a multi-table datasource dataset from the synced schema modeler', async () => {
    const schemaSnapshot = {
      id: 101,
      dataSourceConfigId: 22,
      sourceKey: 'jdbc-22',
      name: 'Campaign Warehouse',
      connectorType: 'MYSQL',
      syncStatus: 'SUCCESS',
      errorMessage: null,
      tableCount: 2,
      columnCount: 8,
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
            { name: 'tenant_id', typeName: 'BIGINT', dataType: -5, nullable: false, ordinalPosition: 1 },
            { name: 'campaign_id', typeName: 'BIGINT', dataType: -5, nullable: false, ordinalPosition: 2 },
            { name: 'campaign_name', typeName: 'VARCHAR', dataType: 12, nullable: true, ordinalPosition: 3 },
            { name: 'owner_user', typeName: 'VARCHAR', dataType: 12, nullable: true, ordinalPosition: 4 },
          ],
        },
      ],
    }
    vi.mocked(biApi.listDatasourceOnboarding).mockResolvedValue(ok([
      {
        id: 22,
        sourceKey: 'jdbc-22',
        name: 'Campaign Warehouse',
        type: 'JDBC',
        connectorType: 'MYSQL',
        enabled: true,
        driverClassName: 'com.mysql.cj.jdbc.Driver',
        maskedUrl: 'jdbc:mysql://warehouse.example.com:3306/marketing',
        maskedUsername: 'bi***op',
        connectionMode: 'DIRECT_QUERY',
        schemaSyncStatus: 'SUCCESS',
        tableCount: 2,
        lastSyncedAt: '2026-06-06T02:15:00',
        supportedModes: ['DIRECT_QUERY', 'CACHE'],
        supportStatus: 'AVAILABLE',
        capabilities: ['CONNECTION_TEST', 'SCHEMA_SYNC', 'TABLE_DATASET'],
      },
    ]))
    vi.mocked(biApi.syncDatasourceSchema).mockResolvedValue(ok(schemaSnapshot))
    vi.mocked(biApi.listDatasourceSchemaSnapshots).mockResolvedValue(ok([schemaSnapshot]))
    vi.mocked(biApi.createMultiTableDatasetFromDatasourceSchema).mockResolvedValue(ok({
      datasetKey: 'jdbc_22_campaign_daily_campaign_dim',
      name: 'Campaign Warehouse campaign_daily + campaign_dim',
      datasetType: 'SQL',
      tableExpression: '(SELECT ...) sql_dataset',
      tenantColumn: 'tenant_id',
      model: { modelType: 'MULTI_TABLE' },
      fields: [],
      metrics: [],
      status: 'DRAFT',
      source: 'DATASOURCE_SCHEMA',
    }))

    renderBiRoute('/bi')

    await screen.findByText('Campaign Warehouse · jdbc-22', {}, { timeout: 3000 })
    fireEvent.click(screen.getByRole('button', { name: '同步 schema' }))
    expect(await screen.findByText('多表建模')).toBeInTheDocument()
    const graphCanvas = await screen.findByLabelText('多表关系画布')
    const dailyNode = await screen.findByRole('button', { name: '关系节点 campaign_daily' })
    fireEvent.pointerDown(dailyNode, { pointerId: 1, clientX: 100, clientY: 100 })
    fireEvent.pointerMove(graphCanvas, { pointerId: 1, clientX: 150, clientY: 125 })
    fireEvent.pointerUp(graphCanvas, { pointerId: 1, clientX: 150, clientY: 125 })
    fireEvent.click(screen.getByRole('button', { name: '关系边 campaign_daily 到 campaign_dim LEFT campaign_id = campaign_id' }))
    const graphOperatorSelect = screen.getAllByLabelText('从画布设置操作符 campaign_daily 到 campaign_dim 条件 1')[0]
    fireEvent.mouseDown(graphOperatorSelect.querySelector('.ant-select-selector') ?? graphOperatorSelect)
    const notEqualOptions = await screen.findAllByText('<>')
    fireEvent.click(notEqualOptions[notEqualOptions.length - 1])
    expect(await screen.findByRole('button', { name: '关系边 campaign_daily 到 campaign_dim LEFT campaign_id <> campaign_id' })).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: '从画布添加全部同名字段条件 campaign_daily 到 campaign_dim' }))
    expect(await screen.findByRole('button', { name: '关系边 campaign_daily 到 campaign_dim LEFT campaign_id <> campaign_id 且 tenant_id = tenant_id' })).toBeInTheDocument()
    const graphConnectorSelect = screen.getAllByLabelText('从画布设置连接符 campaign_daily 到 campaign_dim 条件 2')[0]
    fireEvent.change(graphConnectorSelect, { target: { value: 'OR' } })
    expect(await screen.findByRole('button', { name: '关系边 campaign_daily 到 campaign_dim LEFT campaign_id <> campaign_id 或 tenant_id = tenant_id' })).toBeInTheDocument()
    expect(await screen.findByText('关系诊断')).toBeInTheDocument()
    expect(screen.getByText('2 表 · 主表 campaign_daily')).toBeInTheDocument()
    expect(screen.getByText('1 层 · Quick BI 物理模型建议不超过 5 层')).toBeInTheDocument()
    expect(screen.getByText('2 条件 · 1 复合关系 · 1 OR · 0 分组')).toBeInTheDocument()
    const graphJoinTypeSelect = screen.getAllByLabelText('从画布设置 Join 类型 campaign_daily 到 campaign_dim')[0]
    fireEvent.mouseDown(graphJoinTypeSelect.querySelector('.ant-select-selector') ?? graphJoinTypeSelect)
    const innerJoinOptions = await screen.findAllByText('INNER')
    fireEvent.click(innerJoinOptions[innerJoinOptions.length - 1])
    const graphLeftFieldSelect = screen.getAllByLabelText('从画布设置左字段 campaign_daily 到 campaign_dim 条件 1')[0]
    fireEvent.mouseDown(graphLeftFieldSelect.querySelector('.ant-select-selector') ?? graphLeftFieldSelect)
    const leftTenantOptions = await screen.findAllByText('tenant_id')
    fireEvent.click(leftTenantOptions[leftTenantOptions.length - 1])
    const graphRightFieldSelect = screen.getAllByLabelText('从画布设置右字段 campaign_daily 到 campaign_dim 条件 1')[0]
    fireEvent.mouseDown(graphRightFieldSelect.querySelector('.ant-select-selector') ?? graphRightFieldSelect)
    const rightTenantOptions = await screen.findAllByText('tenant_id')
    fireEvent.click(rightTenantOptions[rightTenantOptions.length - 1])
    expect(await screen.findByRole('button', { name: '关系边 campaign_daily 到 campaign_dim INNER tenant_id <> tenant_id' })).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: '从画布添加关联条件 campaign_daily 到 campaign_dim' }))
    fireEvent.click(screen.getByRole('button', { name: '从画布移除关联条件 campaign_daily 到 campaign_dim 条件 2' }))
    fireEvent.click(screen.getByRole('button', { name: '从画布交换关联方向 campaign_daily 到 campaign_dim' }))
    expect(await screen.findByRole('button', { name: '关系边 campaign_dim 到 campaign_daily INNER tenant_id <> tenant_id' })).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: '生成多表数据集' }))

    await waitFor(() => expect(biApi.createMultiTableDatasetFromDatasourceSchema).toHaveBeenCalledWith({
      dataSourceConfigId: 22,
      datasetKey: 'jdbc_22_campaign_daily_campaign_dim',
      name: 'Campaign Warehouse campaign_daily + campaign_dim',
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
          selectedColumns: ['tenant_id', 'campaign_id', 'campaign_name', 'owner_user'],
        },
      ],
      joins: [
        {
          joinType: 'INNER',
          leftAlias: 'campaign_dim',
          leftColumn: 'tenant_id',
          rightAlias: 'campaign_daily',
          rightColumn: 'tenant_id',
          conditions: [
            { leftColumn: 'tenant_id', rightColumn: 'tenant_id', operator: '<>' },
          ],
        },
      ],
      graph: {
        layoutMode: 'GRAPH_CANVAS',
        nodes: [
          { tableName: 'campaign_daily', alias: 'campaign_daily', x: 130, y: 105 },
          { tableName: 'campaign_dim', alias: 'campaign_dim', x: 360, y: 80 },
        ],
      },
    }))
  }, 60000)

  it('saves a parameterized SQL dataset draft from the datasource modeler', async () => {
    vi.mocked(biApi.listDatasourceOnboarding).mockResolvedValue(ok([
      {
        id: 22,
        sourceKey: 'jdbc-22',
        name: 'Campaign Warehouse',
        type: 'JDBC',
        connectorType: 'MYSQL',
        enabled: true,
        driverClassName: 'com.mysql.cj.jdbc.Driver',
        maskedUrl: 'jdbc:mysql://warehouse.example.com:3306/marketing',
        maskedUsername: 'bi***op',
        connectionMode: 'DIRECT_QUERY',
        schemaSyncStatus: 'SUCCESS',
        tableCount: 2,
        lastSyncedAt: '2026-06-06T02:15:00',
        supportedModes: ['DIRECT_QUERY', 'CACHE'],
        supportStatus: 'AVAILABLE',
        capabilities: ['CONNECTION_TEST', 'SCHEMA_SYNC', 'SQL_DATASET', 'TABLE_DATASET'],
      },
    ]))
    vi.mocked(biApi.saveDatasetDraft).mockResolvedValue(ok({
      datasetKey: 'campaign_sql',
      name: 'Campaign SQL',
      datasetType: 'SQL',
      tableExpression: '(SELECT tenant_id, stat_date, channel, total_cost FROM campaign_daily WHERE stat_date >= ? AND channel = ?) sql_dataset',
      tenantColumn: 'tenant_id',
      model: { sqlApprovalRequired: true },
      fields: [],
      metrics: [],
      status: 'DRAFT',
      source: 'PERSISTED',
    }))

    renderBiRoute('/bi')

    fireEvent.change(await screen.findByLabelText('BI SQL数据集Key'), {
      target: { value: 'campaign_sql' },
    })
    fireEvent.change(screen.getByLabelText('BI SQL数据集名称'), {
      target: { value: 'Campaign SQL' },
    })
    const sqlTemplate = 'SELECT tenant_id, stat_date, channel, total_cost FROM campaign_daily WHERE stat_date >= {{start_date}} AND channel = {{channel}}'
    fireEvent.change(screen.getByLabelText('BI SQL模板'), {
      target: { value: sqlTemplate },
    })
    fireEvent.change(await screen.findByLabelText('SQL参数start_date默认值'), {
      target: { value: '2026-06-01' },
    })
    fireEvent.change(screen.getByLabelText('SQL参数channel默认值'), {
      target: { value: 'PAID' },
    })
    fireEvent.change(screen.getByLabelText('SQL参数channel允许值'), {
      target: { value: 'PAID, EMAIL' },
    })
    fireEvent.change(screen.getByLabelText('SQL字段1显示名'), {
      target: { value: 'Business Date' },
    })
    fireEvent.change(screen.getByLabelText('SQL字段1格式'), {
      target: { value: 'yyyy-MM-dd' },
    })
    fireEvent.change(screen.getByLabelText('SQL字段2语义类型'), {
      target: { value: 'TRAFFIC_CHANNEL' },
    })
    fireEvent.click(screen.getByLabelText('SQL字段2可见'))
    fireEvent.change(screen.getByLabelText('SQL指标0显示名'), {
      target: { value: 'Total Spend' },
    })
    fireEvent.change(screen.getByLabelText('SQL指标0表达式'), {
      target: { value: 'SUM(ad_cost)' },
    })
    fireEvent.change(screen.getByLabelText('SQL指标0允许维度'), {
      target: { value: 'stat_date' },
    })
    fireEvent.change(screen.getByLabelText('SQL指标0单位'), {
      target: { value: 'USD' },
    })
    fireEvent.change(screen.getByLabelText('SQL指标0描述'), {
      target: { value: 'Paid media spend from SQL dataset' },
    })
    fireEvent.click(screen.getByRole('button', { name: '保存 SQL 数据集' }))

    await waitFor(() => expect(biApi.saveDatasetDraft).toHaveBeenCalledWith('campaign_sql', expect.objectContaining({
      datasetKey: 'campaign_sql',
      name: 'Campaign SQL',
      datasetType: 'SQL',
      tableExpression: sqlTemplate,
      tenantColumn: 'tenant_id',
      model: expect.objectContaining({
        dataSourceConfigId: 22,
        sqlApprovalRequired: true,
        sqlTemplate,
        sqlParameterOrder: ['start_date', 'channel'],
        sqlParameters: [
          { key: 'start_date', dataType: 'DATE', required: true, defaultValue: '2026-06-01', allowedValues: [] },
          { key: 'channel', dataType: 'STRING', required: true, defaultValue: 'PAID', allowedValues: ['PAID', 'EMAIL'] },
        ],
      }),
      fields: expect.arrayContaining([
        expect.objectContaining({ fieldKey: 'tenant_id', role: 'DIMENSION', dataType: 'NUMBER' }),
        expect.objectContaining({ fieldKey: 'stat_date', displayName: 'Business Date', role: 'DIMENSION', dataType: 'DATE', formatPattern: 'yyyy-MM-dd' }),
        expect.objectContaining({ fieldKey: 'channel', semanticType: 'TRAFFIC_CHANNEL', visible: false }),
      ]),
      metrics: expect.arrayContaining([
        expect.objectContaining({
          metricKey: 'total_cost',
          displayName: 'Total Spend',
          expression: 'SUM(ad_cost)',
          allowedDimensions: ['stat_date'],
          unit: 'USD',
          description: 'Paid media spend from SQL dataset',
        }),
      ]),
    })))
  }, 30000)

  it('previews a parameterized SQL dataset sample and lineage from the datasource modeler', async () => {
    vi.mocked(biApi.listDatasourceOnboarding).mockResolvedValue(ok([
      {
        id: 22,
        sourceKey: 'jdbc-22',
        name: 'Campaign Warehouse',
        type: 'JDBC',
        connectorType: 'MYSQL',
        enabled: true,
        driverClassName: 'com.mysql.cj.jdbc.Driver',
        maskedUrl: 'jdbc:mysql://warehouse.example.com:3306/marketing',
        maskedUsername: 'bi***op',
        connectionMode: 'DIRECT_QUERY',
        schemaSyncStatus: 'SUCCESS',
        tableCount: 2,
        lastSyncedAt: '2026-06-06T02:15:00',
        supportedModes: ['DIRECT_QUERY', 'CACHE'],
        supportStatus: 'AVAILABLE',
        capabilities: ['CONNECTION_TEST', 'SCHEMA_SYNC', 'SQL_DATASET', 'TABLE_DATASET'],
      },
    ]))
    vi.mocked(biApi.previewSqlDataset).mockResolvedValue(ok({
      datasetKey: 'campaign_sql',
      normalizedSqlTemplate: 'SELECT tenant_id, stat_date, channel, total_cost FROM campaign_daily WHERE stat_date >= {{start_date}} AND channel = {{channel}}',
      compiledSql: 'SELECT stat_date AS stat_date, SUM(total_cost) AS total_cost FROM (SELECT tenant_id, stat_date, channel, total_cost FROM campaign_daily WHERE stat_date >= ? AND channel = ?) sql_dataset WHERE tenant_id = ? GROUP BY stat_date LIMIT 20',
      parameterCount: 3,
      columns: [
        { key: 'stat_date', role: 'DIMENSION', dataType: 'DATE' },
        { key: 'total_cost', role: 'METRIC', dataType: 'NUMBER' },
      ],
      rows: [{ stat_date: '2026-06-01', total_cost: 18.5 }],
      rowCount: 1,
      sampleLimit: 20,
      sampleExecuted: true,
      executionError: null,
      lineage: {
        dataSourceConfigId: 22,
        sourceTables: ['campaign_daily'],
        parameterKeys: ['start_date', 'channel'],
        tenantColumn: 'tenant_id',
        referencedFields: ['stat_date', 'total_cost'],
        referencedMetrics: ['SUM(total_cost)'],
        approvalRequired: true,
      },
      impact: {
        impactedAssetTypes: ['DATASET_DRAFT', 'PUBLISH_APPROVAL', 'QUERY_CACHE', 'DOWNSTREAM_REPORTS'],
        governanceGates: ['READ_ONLY_SQL_LINT', 'TENANT_COLUMN_REQUIRED', 'SQL_PARAMETER_BINDING', 'PUBLISH_APPROVAL_REQUIRED'],
        warnings: [],
      },
    }))

    renderBiRoute('/bi')

    fireEvent.change(await screen.findByLabelText('BI SQL数据集Key'), {
      target: { value: 'campaign_sql' },
    })
    const sqlTemplate = 'SELECT tenant_id, stat_date, channel, total_cost FROM campaign_daily WHERE stat_date >= {{start_date}} AND channel = {{channel}}'
    fireEvent.change(screen.getByLabelText('BI SQL模板'), {
      target: { value: sqlTemplate },
    })
    fireEvent.change(await screen.findByLabelText('SQL参数start_date默认值'), {
      target: { value: '2026-06-01' },
    })
    fireEvent.change(screen.getByLabelText('SQL参数channel默认值'), {
      target: { value: 'PAID' },
    })
    fireEvent.change(screen.getByLabelText('SQL指标0负责人'), {
      target: { value: 'bi-owner' },
    })
    fireEvent.change(screen.getByLabelText('SQL指标0描述'), {
      target: { value: 'Paid media spend' },
    })
    fireEvent.click(screen.getByRole('button', { name: '预览 SQL 数据集' }))

    await waitFor(() => expect(biApi.previewSqlDataset).toHaveBeenCalledWith({
      resource: expect.objectContaining({
        datasetKey: 'campaign_sql',
        datasetType: 'SQL',
        tableExpression: sqlTemplate,
        model: expect.objectContaining({
          dataSourceConfigId: 22,
          sqlParameterOrder: ['start_date', 'channel'],
        }),
      }),
      sqlParameters: {
        start_date: '2026-06-01',
        channel: 'PAID',
      },
      limit: 20,
      executeSample: true,
    }))
    expect(await screen.findByText('SQL 预览 1 行')).toBeInTheDocument()
    expect(screen.getByText('campaign_daily')).toBeInTheDocument()
    expect(screen.getByText('PUBLISH_APPROVAL_REQUIRED')).toBeInTheDocument()
    expect(screen.getAllByText('2026-06-01').length).toBeGreaterThanOrEqual(1)
    expect(screen.getByText('样本剖析')).toBeInTheDocument()
    expect(screen.getByText('stat_date · DIMENSION · DATE')).toBeInTheDocument()
    expect(screen.getByText('total_cost · METRIC · NUMBER')).toBeInTheDocument()
    expect(screen.getAllByText('填充 1/1').length).toBeGreaterThanOrEqual(2)
    expect(screen.getAllByText('唯一 1').length).toBeGreaterThanOrEqual(2)
    expect(screen.getByText('影响分析')).toBeInTheDocument()
    expect(screen.getByText('DATASET_DRAFT / PUBLISH_APPROVAL / QUERY_CACHE / DOWNSTREAM_REPORTS')).toBeInTheDocument()
    expect(screen.getByText('datasource #22 · campaign_daily · tenant tenant_id')).toBeInTheDocument()
    expect(screen.getByText('发布诊断')).toBeInTheDocument()
    expect(screen.getByText('字段与指标')).toBeInTheDocument()
    expect(screen.getByText('3 字段 / 1 指标 · 3 可见字段 · 1 指标维度约束')).toBeInTheDocument()
    expect(screen.getAllByText('运行参数').length).toBeGreaterThanOrEqual(1)
    expect(screen.getByText('2 参数 · 默认值已补齐')).toBeInTheDocument()
    expect(screen.getByText('样例预览')).toBeInTheDocument()
    expect(screen.getByText('1/20 行 · 2 列')).toBeInTheDocument()
    expect(screen.getByText('血缘与审批')).toBeInTheDocument()
    expect(screen.getByText('campaign_daily · 门禁 READ_ONLY_SQL_LINT / TENANT_COLUMN_REQUIRED / SQL_PARAMETER_BINDING / PUBLISH_APPROVAL_REQUIRED')).toBeInTheDocument()
  }, 60000)

  it('creates a three-table datasource dataset from the schema relationship modeler', async () => {
    const schemaSnapshot = {
      id: 102,
      dataSourceConfigId: 22,
      sourceKey: 'jdbc-22',
      name: 'Campaign Warehouse',
      connectorType: 'MYSQL',
      syncStatus: 'SUCCESS',
      errorMessage: null,
      tableCount: 3,
      columnCount: 9,
      syncedAt: '2026-06-06T02:25:00',
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
        {
          name: 'campaign_budget',
          tableType: 'TABLE',
          columns: [
            { name: 'campaign_id', typeName: 'BIGINT', dataType: -5, nullable: false, ordinalPosition: 1 },
            { name: 'budget_amount', typeName: 'DECIMAL', dataType: 3, nullable: true, ordinalPosition: 2 },
            { name: 'budget_owner', typeName: 'VARCHAR', dataType: 12, nullable: true, ordinalPosition: 3 },
          ],
        },
      ],
    }
    vi.mocked(biApi.listDatasourceOnboarding).mockResolvedValue(ok([
      {
        id: 22,
        sourceKey: 'jdbc-22',
        name: 'Campaign Warehouse',
        type: 'JDBC',
        connectorType: 'MYSQL',
        enabled: true,
        driverClassName: 'com.mysql.cj.jdbc.Driver',
        maskedUrl: 'jdbc:mysql://warehouse.example.com:3306/marketing',
        maskedUsername: 'bi***op',
        connectionMode: 'DIRECT_QUERY',
        schemaSyncStatus: 'SUCCESS',
        tableCount: 3,
        lastSyncedAt: '2026-06-06T02:25:00',
        supportedModes: ['DIRECT_QUERY', 'CACHE'],
        supportStatus: 'AVAILABLE',
        capabilities: ['CONNECTION_TEST', 'SCHEMA_SYNC', 'TABLE_DATASET'],
      },
    ]))
    vi.mocked(biApi.syncDatasourceSchema).mockResolvedValue(ok(schemaSnapshot))
    vi.mocked(biApi.listDatasourceSchemaSnapshots).mockResolvedValue(ok([schemaSnapshot]))
    vi.mocked(biApi.createMultiTableDatasetFromDatasourceSchema).mockResolvedValue(ok({
      datasetKey: 'jdbc_22_campaign_daily_campaign_dim_campaign_budget',
      name: 'Campaign Warehouse campaign_daily + campaign_dim + campaign_budget',
      datasetType: 'SQL',
      tableExpression: '(SELECT ...) sql_dataset',
      tenantColumn: 'tenant_id',
      model: { modelType: 'MULTI_TABLE' },
      fields: [],
      metrics: [],
      status: 'DRAFT',
      source: 'DATASOURCE_SCHEMA',
    }))

    renderBiRoute('/bi')

    await screen.findByText('Campaign Warehouse · jdbc-22', {}, { timeout: 3000 })
    fireEvent.click(screen.getByRole('button', { name: '同步 schema' }))
    expect(await screen.findByText('多表建模')).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: '添加关联表' }))
    fireEvent.click(screen.getByRole('button', { name: '关系边 campaign_daily 到 campaign_budget LEFT campaign_id = campaign_id' }))
    const graphLeftTableSelect = (await screen.findAllByLabelText('从画布设置左表 campaign_daily 到 campaign_budget'))[0]
    fireEvent.mouseDown(graphLeftTableSelect.querySelector('.ant-select-selector') ?? graphLeftTableSelect)
    const graphLeftTableOptions = await screen.findAllByText('campaign_dim')
    fireEvent.click(graphLeftTableOptions[graphLeftTableOptions.length - 1])
    expect(await screen.findByRole('button', { name: '关系边 campaign_dim 到 campaign_budget LEFT campaign_id = campaign_id' })).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: '生成多表数据集' }))

    await waitFor(() => expect(biApi.createMultiTableDatasetFromDatasourceSchema).toHaveBeenCalledWith({
      dataSourceConfigId: 22,
      datasetKey: 'jdbc_22_campaign_daily_campaign_dim_campaign_budget',
      name: 'Campaign Warehouse campaign_daily + campaign_dim + campaign_budget',
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
        {
          tableName: 'campaign_budget',
          alias: 'campaign_budget',
          selectedColumns: ['campaign_id', 'budget_amount', 'budget_owner'],
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
        {
          joinType: 'LEFT',
          leftAlias: 'campaign_dim',
          leftColumn: 'campaign_id',
          rightAlias: 'campaign_budget',
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
          { tableName: 'campaign_budget', alias: 'campaign_budget', x: 640, y: 80 },
        ],
      },
    }))
  }, 60000)

  it('rotates credentials from the datasource onboarding actions', async () => {
    vi.mocked(biApi.listDatasourceOnboarding).mockResolvedValue(ok([
      {
        id: 11,
        sourceKey: 'jdbc-11',
        name: 'warehouse',
        type: 'JDBC',
        connectorType: 'MYSQL',
        enabled: true,
        driverClassName: 'com.mysql.cj.jdbc.Driver',
        maskedUrl: 'jdbc:mysql://db.example.com/canvas',
        maskedUsername: 'ca***pp',
        connectionMode: 'DIRECT_QUERY',
        schemaSyncStatus: 'NOT_SYNCED',
        tableCount: 0,
        lastSyncedAt: null,
        supportedModes: ['DIRECT_QUERY', 'CACHE'],
        supportStatus: 'AVAILABLE',
        capabilities: ['CONNECTION_TEST', 'SCHEMA_SYNC', 'SQL_DATASET', 'TABLE_DATASET', 'CREDENTIALS'],
      },
    ]))
    vi.mocked(biApi.rotateDatasourceCredential).mockResolvedValue(ok({
      id: 11,
      sourceKey: 'jdbc-11',
      rotatedBy: 'alice',
    }))

    renderBiRoute('/bi?dashboard=canvas-effect&canvasId=12')

    expect(await screen.findByText('warehouse · jdbc-11')).toBeInTheDocument()
    const onboardingCallsBeforeRotation = vi.mocked(biApi.listDatasourceOnboarding).mock.calls.length
    fireEvent.click(screen.getByRole('button', { name: /轮换凭证/ }))
    fireEvent.change(await screen.findByLabelText('数据源新密码'), {
      target: { value: 'rotated-password' },
    })
    const dialog = await screen.findByRole('dialog', { name: /轮换凭证/ })
    fireEvent.click(within(dialog).getByRole('button', { name: /轮\s*换/ }))

    await waitFor(() => expect(biApi.rotateDatasourceCredential).toHaveBeenCalledWith(11, {
      password: 'rotated-password',
    }))
    await waitFor(() => expect(vi.mocked(biApi.listDatasourceOnboarding).mock.calls.length)
      .toBeGreaterThan(onboardingCallsBeforeRotation))
  }, 30000)

  it('marks SQL dataset assets as approval gated', async () => {
    vi.mocked(biApi.listDatasetResources).mockResolvedValue(ok([
      {
        datasetKey: 'campaign_sql',
        name: 'Campaign SQL',
        datasetType: 'SQL',
        tableExpression: '(SELECT tenant_id, stat_date, total_cost FROM campaign_daily) sql_dataset',
        tenantColumn: 'tenant_id',
        model: { sqlApprovalRequired: true },
        fields: [],
        metrics: [],
        status: 'DRAFT',
        source: 'PERSISTED',
      },
    ]))

    renderBiRoute('/bi?dashboard=canvas-effect&canvasId=12')

    expect(await screen.findByText('Campaign SQL')).toBeInTheDocument()
    expect(screen.getByText('SQL 审批')).toBeInTheDocument()
    expect(screen.getByText('发布前必须审批')).toBeInTheDocument()
  }, 30000)

  it('saves dataset field folders and copies dataset drafts from the resource workbench', async () => {
    const consoleErrorSpy = vi.spyOn(console, 'error').mockImplementation(() => undefined)
    vi.mocked(biApi.listDatasetResources).mockResolvedValue(ok([
      {
        datasetKey: 'campaign_model',
        name: 'Campaign Model',
        datasetType: 'TABLE',
        tableExpression: 'dws.campaign_model',
        tenantColumn: 'tenant_id',
        model: {},
        fields: [
          {
            fieldKey: 'stat_date',
            displayName: 'Stat Date',
            columnExpression: 'stat_date',
            role: 'DIMENSION',
            dataType: 'DATE',
            visible: true,
            sensitiveLevel: 'PUBLIC',
            sortOrder: 1,
          },
          {
            fieldKey: 'channel',
            displayName: 'Channel',
            columnExpression: 'channel',
            role: 'DIMENSION',
            dataType: 'STRING',
            visible: true,
            sensitiveLevel: 'PUBLIC',
            sortOrder: 2,
          },
        ],
        metrics: [{
          metricKey: 'send_count',
          displayName: 'Send Count',
          expression: 'SUM(send_count)',
          aggregation: 'SUM',
          dataType: 'NUMBER',
          allowedDimensions: ['stat_date', 'channel'],
          status: 'ACTIVE',
        }],
        status: 'DRAFT',
        source: 'PERSISTED',
      },
      {
        datasetKey: 'campaign_orders',
        name: 'Campaign Orders',
        datasetType: 'TABLE',
        tableExpression: 'dws.campaign_orders',
        tenantColumn: 'tenant_id',
        model: {},
        fields: [],
        metrics: [],
        status: 'DRAFT',
        source: 'PERSISTED',
      },
    ]))
    vi.mocked(biApi.saveDatasetDraft).mockImplementation((_datasetKey: string, resource) => Promise.resolve(ok(resource)))
    vi.mocked(biApi.moveResource).mockResolvedValue(ok({
      id: 1,
      tenantId: 1,
      workspaceId: 1,
      resourceType: 'DATASET',
      resourceKey: 'campaign_model',
      folderKey: 'ops-datasets',
      sortOrder: 0,
    }))
    vi.mocked(biApi.transferResource).mockResolvedValue(ok({
      id: 2,
      tenantId: 1,
      workspaceId: 1,
      resourceType: 'DATASET',
      resourceKey: 'campaign_model',
      ownerUser: 'owner@example.com',
    }))

    renderBiRoute('/bi')

    fireEvent.change(await screen.findByLabelText('数据集字段选择'), {
      target: { value: 'stat_date,channel' },
    })
    fireEvent.change(screen.getByLabelText('数据集字段文件夹'), {
      target: { value: '时间与渠道' },
    })
    fireEvent.click(screen.getByRole('button', { name: '批量移动字段到文件夹' }))
    expect(screen.getByText('时间与渠道')).toBeInTheDocument()
    fireEvent.change(screen.getByLabelText('数据集拖拽字段'), {
      target: { value: 'channel' },
    })
    fireEvent.change(screen.getByLabelText('数据集拖放目标字段'), {
      target: { value: 'stat_date' },
    })
    fireEvent.change(screen.getByLabelText('数据集字段拖放位置'), {
      target: { value: 'before' },
    })
    fireEvent.change(screen.getByLabelText('数据集字段拖放文件夹'), {
      target: { value: '核心字段' },
    })
    fireEvent.click(screen.getByRole('button', { name: '应用数据集字段拖放' }))
    fireEvent.click(screen.getByRole('button', { name: '字段详情 stat_date' }))
    fireEvent.change(screen.getByLabelText('数据集字段显示名'), {
      target: { value: '统计日期' },
    })
    fireEvent.change(screen.getByLabelText('数据集字段角色'), {
      target: { value: 'DIMENSION' },
    })
    fireEvent.change(screen.getByLabelText('数据集字段语义类型'), {
      target: { value: 'DATE' },
    })
    fireEvent.change(screen.getByLabelText('数据集字段默认聚合'), {
      target: { value: 'NONE' },
    })
    fireEvent.change(screen.getByLabelText('数据集字段格式'), {
      target: { value: 'yyyy-MM-dd' },
    })
    fireEvent.click(screen.getByLabelText('数据集字段可见'))
    fireEvent.change(screen.getByLabelText('数据集批量角色'), {
      target: { value: 'DIMENSION' },
    })
    fireEvent.change(screen.getByLabelText('数据集批量格式'), {
      target: { value: 'text' },
    })
    fireEvent.click(screen.getByLabelText('数据集批量可见'))
    fireEvent.click(screen.getByRole('button', { name: '应用数据集字段批量配置' }))
    fireEvent.click(screen.getByRole('button', { name: '保存数据集草稿' }))

    await waitFor(() => expect(biApi.saveDatasetDraft).toHaveBeenCalledWith(
      'campaign_model',
      expect.objectContaining({
        datasetKey: 'campaign_model',
        fields: [
          expect.objectContaining({
            fieldKey: 'channel',
            folderKey: '核心字段',
            role: 'DIMENSION',
            formatPattern: 'text',
            visible: false,
            sortOrder: 1,
          }),
          expect.objectContaining({
            fieldKey: 'stat_date',
            displayName: '统计日期',
            role: 'DIMENSION',
            semanticType: 'DATE',
            defaultAggregation: 'NONE',
            formatPattern: 'text',
            folderKey: '时间与渠道',
            visible: false,
            sortOrder: 2,
          }),
        ],
      }),
      null,
    ))

    fireEvent.click(screen.getByRole('button', { name: '复制数据集草稿' }))
    fireEvent.click(screen.getByRole('button', { name: '保存数据集草稿' }))

    await waitFor(() => expect(biApi.saveDatasetDraft).toHaveBeenLastCalledWith(
      'campaign_model_copy',
      expect.objectContaining({
        datasetKey: 'campaign_model_copy',
        name: 'Campaign Model 副本',
        status: 'DRAFT',
        source: 'PERSISTED',
        fields: expect.arrayContaining([
          expect.objectContaining({ fieldKey: 'stat_date', folderKey: '时间与渠道' }),
        ]),
      }),
      null,
    ))
    expect(screen.getByText('字段文件夹：核心字段 · 1 字段；字段文件夹：时间与渠道 · 1 字段')).toBeInTheDocument()

    fireEvent.change(screen.getByLabelText('数据集批量资源选择'), {
      target: { value: 'campaign_model,campaign_orders' },
    })
    fireEvent.change(screen.getByLabelText('数据集批量移动文件夹'), {
      target: { value: 'ops datasets' },
    })
    fireEvent.click(screen.getByRole('button', { name: '批量移动数据集资源' }))

    await waitFor(() => expect(biApi.moveResource).toHaveBeenCalledTimes(2))
    expect(biApi.moveResource).toHaveBeenCalledWith({
      resourceType: 'DATASET',
      resourceKey: 'campaign_model',
      folderKey: 'ops-datasets',
      sortOrder: 0,
    })
    expect(biApi.moveResource).toHaveBeenCalledWith({
      resourceType: 'DATASET',
      resourceKey: 'campaign_orders',
      folderKey: 'ops-datasets',
      sortOrder: 1,
    })

    fireEvent.change(screen.getByLabelText('数据集批量转让人'), {
      target: { value: ' owner@example.com ' },
    })
    fireEvent.click(screen.getByRole('button', { name: '批量转让数据集资源' }))

    await waitFor(() => expect(biApi.transferResource).toHaveBeenCalledTimes(2))
    expect(biApi.transferResource).toHaveBeenCalledWith({
      resourceType: 'DATASET',
      resourceKey: 'campaign_model',
      ownerUser: 'owner@example.com',
    })
    expect(biApi.transferResource).toHaveBeenCalledWith({
      resourceType: 'DATASET',
      resourceKey: 'campaign_orders',
      ownerUser: 'owner@example.com',
    })
    expect(consoleErrorSpy.mock.calls.some(call => String(call[0] ?? '').includes('Encountered two children with the same key'))).toBe(false)
    consoleErrorSpy.mockRestore()
  }, 60000)

  it('requests and reviews BI resource permissions from the governance workbench', async () => {
    vi.mocked(biApi.listDatasetResources).mockResolvedValue(ok([
      {
        datasetKey: 'canvas_daily_stats',
        name: 'Canvas Daily Stats',
        datasetType: 'TABLE',
        tableExpression: 'canvas_dws.canvas_daily_stats',
        tenantColumn: 'tenant_id',
        model: {},
        fields: [
          {
            fieldKey: 'revenue',
            displayName: 'Revenue',
            columnExpression: 'revenue',
            role: 'MEASURE',
            dataType: 'NUMBER',
            visible: true,
            sensitiveLevel: 'CONFIDENTIAL',
            sortOrder: 1,
          },
          {
            fieldKey: 'canvas_name',
            displayName: 'Canvas Name',
            columnExpression: 'canvas_name',
            role: 'DIMENSION',
            dataType: 'STRING',
            visible: true,
            sensitiveLevel: 'PUBLIC',
            sortOrder: 2,
          },
        ],
        metrics: [],
        status: 'PUBLISHED',
        source: 'PERSISTED',
      },
    ]))
    vi.mocked(biApi.listPermissionRequests).mockResolvedValue(ok([
      {
        id: 901,
        tenantId: 7,
        workspaceId: 3,
        resourceType: 'DATASET',
        resourceKey: 'canvas_daily_stats',
        requestedAction: 'USE',
        requestedBy: 'alice',
        requestedAt: '2026-06-07T09:00:00',
        reason: 'Need dashboard access',
        status: 'PENDING',
      },
    ]))
    vi.mocked(biApi.requestPermission).mockResolvedValue(ok({
      id: 902,
      tenantId: 7,
      workspaceId: 3,
      resourceType: 'DATASET',
      resourceKey: 'canvas_daily_stats',
      requestedAction: 'EDIT',
      requestedBy: 'bob',
      requestedAt: '2026-06-07T09:05:00',
      reason: 'Build derived dashboard',
      status: 'PENDING',
    }))
    vi.mocked(biApi.reviewPermissionRequest).mockResolvedValue(ok({
      id: 901,
      tenantId: 7,
      workspaceId: 3,
      resourceType: 'DATASET',
      resourceKey: 'canvas_daily_stats',
      requestedAction: 'USE',
      requestedBy: 'alice',
      requestedAt: '2026-06-07T09:00:00',
      reason: 'Need dashboard access',
      status: 'APPROVED',
      reviewedBy: 'admin',
      reviewedAt: '2026-06-07T09:10:00',
      reviewComment: 'Approved for reporting',
      grantedPermissionId: 1001,
    }))

    renderBiRoute('/bi')

    await waitFor(() => expect(screen.getAllByLabelText('权限治理资源').length).toBeGreaterThan(0))
    const permissionTargetSelect = screen.getAllByLabelText('权限治理资源')[0]
    fireEvent.mouseDown(permissionTargetSelect.querySelector('.ant-select-selector') ?? permissionTargetSelect)
    const datasetTargetOptions = await screen.findAllByText('选中数据集 · canvas_daily_stats')
    fireEvent.click(datasetTargetOptions[datasetTargetOptions.length - 1])
    fireEvent.change(await screen.findByLabelText('权限申请动作'), {
      target: { value: 'EDIT' },
    })
    fireEvent.change(screen.getByLabelText('权限申请理由'), {
      target: { value: 'Build derived dashboard' },
    })
    fireEvent.click(screen.getByRole('button', { name: '提交权限申请' }))

    await waitFor(() => expect(biApi.requestPermission).toHaveBeenCalledWith({
      resourceType: 'DATASET',
      resourceKey: 'canvas_daily_stats',
      requestedAction: 'EDIT',
      reason: 'Build derived dashboard',
    }))

    fireEvent.change(screen.getByLabelText('权限审核意见'), {
      target: { value: 'Approved for reporting' },
    })
    fireEvent.click(await screen.findByRole('button', { name: '批准权限申请 #901' }))

    await waitFor(() => expect(biApi.reviewPermissionRequest).toHaveBeenCalledWith(901, {
      requestId: 901,
      status: 'APPROVED',
      reviewComment: 'Approved for reporting',
    }))
  }, 30000)

  it('saves custom BI resource permission rules from the governance workbench', async () => {
    vi.mocked(biApi.upsertResourcePermission).mockResolvedValue(ok({
      id: 503,
      tenantId: 7,
      workspaceId: 3,
      resourceType: 'DASHBOARD',
      resourceKey: 'canvas-effect',
      resourceId: 21,
      subjectType: 'USER',
      subjectId: 'bob',
      actionKey: 'PUBLISH',
      effect: 'DENY',
    }))

    renderBiRoute('/bi')

    fireEvent.change(await screen.findByLabelText('资源权限主体类型'), {
      target: { value: 'USER' },
    })
    fireEvent.change(screen.getByLabelText('资源权限主体'), {
      target: { value: 'bob' },
    })
    fireEvent.change(screen.getByLabelText('资源权限动作'), {
      target: { value: 'PUBLISH' },
    })
    fireEvent.change(screen.getByLabelText('资源权限效果'), {
      target: { value: 'DENY' },
    })
    fireEvent.click(screen.getByRole('button', { name: '保存资源权限规则' }))

    await waitFor(() => expect(biApi.upsertResourcePermission).toHaveBeenCalledWith({
      resourceType: 'DASHBOARD',
      resourceKey: 'canvas-effect',
      subjectType: 'USER',
      subjectId: 'bob',
      actionKey: 'PUBLISH',
      effect: 'DENY',
    }))
  }, 30000)

  it('saves custom BI row permission rules from the governance workbench', async () => {
    vi.mocked(biApi.listDatasetResources).mockResolvedValue(ok([
      {
        datasetKey: 'canvas_daily_stats',
        name: 'Canvas Daily Stats',
        datasetType: 'TABLE',
        tableExpression: 'canvas_dws.canvas_daily_stats',
        tenantColumn: 'tenant_id',
        model: {},
        fields: [],
        metrics: [],
        status: 'PUBLISHED',
        source: 'PERSISTED',
      },
    ]))
    vi.mocked(biApi.upsertRowPermission).mockResolvedValue(ok({
      id: 602,
      tenantId: 7,
      workspaceId: 3,
      datasetKey: 'canvas_daily_stats',
      datasetId: 11,
      ruleKey: 'region_scope',
      subjectType: 'USER',
      subjectId: 'bob',
      filterJson: '{"region":"APAC"}',
      enabled: true,
    }))

    renderBiRoute('/bi')

    await waitFor(() => expect(screen.getAllByLabelText('权限治理资源').length).toBeGreaterThan(0))
    const permissionTargetSelect = screen.getAllByLabelText('权限治理资源')[0]
    fireEvent.mouseDown(permissionTargetSelect.querySelector('.ant-select-selector') ?? permissionTargetSelect)
    const datasetTargetOptions = await screen.findAllByText('选中数据集 · canvas_daily_stats')
    fireEvent.click(datasetTargetOptions[datasetTargetOptions.length - 1])
    fireEvent.change(await screen.findByLabelText('行权限规则Key'), {
      target: { value: 'region_scope' },
    })
    fireEvent.change(screen.getByLabelText('行权限主体类型'), {
      target: { value: 'USER' },
    })
    fireEvent.change(screen.getByLabelText('行权限主体'), {
      target: { value: 'bob' },
    })
    fireEvent.change(screen.getByLabelText('行权限过滤JSON'), {
      target: { value: '{"region":"APAC"}' },
    })
    fireEvent.click(screen.getByRole('button', { name: '保存行权限规则' }))

    await waitFor(() => expect(biApi.upsertRowPermission).toHaveBeenCalledWith({
      datasetKey: 'canvas_daily_stats',
      ruleKey: 'region_scope',
      subjectType: 'USER',
      subjectId: 'bob',
      filters: [],
      filter: { region: 'APAC' },
      enabled: true,
    }))
  }, 30000)

  it('saves custom BI column permission rules from the governance workbench', async () => {
    vi.mocked(biApi.listDatasetResources).mockResolvedValue(ok([
      {
        datasetKey: 'canvas_daily_stats',
        name: 'Canvas Daily Stats',
        datasetType: 'TABLE',
        tableExpression: 'canvas_dws.canvas_daily_stats',
        tenantColumn: 'tenant_id',
        model: {},
        fields: [],
        metrics: [],
        status: 'PUBLISHED',
        source: 'PERSISTED',
      },
    ]))
    vi.mocked(biApi.upsertColumnPermission).mockResolvedValue(ok({
      id: 702,
      tenantId: 7,
      workspaceId: 3,
      datasetKey: 'canvas_daily_stats',
      datasetId: 11,
      fieldKey: 'revenue',
      subjectType: 'USER',
      subjectId: 'bob',
      policy: 'DENY',
      maskJson: '{"strategy":"FIXED","replacement":"***"}',
      enabled: true,
    }))

    renderBiRoute('/bi')

    await waitFor(() => expect(screen.getAllByLabelText('权限治理资源').length).toBeGreaterThan(0))
    const permissionTargetSelect = screen.getAllByLabelText('权限治理资源')[0]
    fireEvent.mouseDown(permissionTargetSelect.querySelector('.ant-select-selector') ?? permissionTargetSelect)
    const datasetTargetOptions = await screen.findAllByText('选中数据集 · canvas_daily_stats')
    fireEvent.click(datasetTargetOptions[datasetTargetOptions.length - 1])
    fireEvent.change(await screen.findByLabelText('列权限字段'), {
      target: { value: 'revenue' },
    })
    fireEvent.change(screen.getByLabelText('列权限主体类型'), {
      target: { value: 'USER' },
    })
    fireEvent.change(screen.getByLabelText('列权限主体'), {
      target: { value: 'bob' },
    })
    fireEvent.click(await screen.findByRole('button', { name: '应用敏感字段列权限' }))

    await waitFor(() => expect(biApi.upsertColumnPermission).toHaveBeenCalledWith({
      datasetKey: 'canvas_daily_stats',
      fieldKey: 'revenue',
      subjectType: 'USER',
      subjectId: 'bob',
      policy: 'MASK',
      mask: { strategy: 'FIXED', replacement: 'MASKED' },
      enabled: true,
    }))

    fireEvent.change(screen.getByLabelText('列权限策略'), {
      target: { value: 'DENY' },
    })
    fireEvent.change(screen.getByLabelText('列权限Mask JSON'), {
      target: { value: '{"strategy":"FIXED","replacement":"***"}' },
    })
    fireEvent.click(screen.getByRole('button', { name: '保存列权限规则' }))

    await waitFor(() => expect(biApi.upsertColumnPermission).toHaveBeenCalledWith({
      datasetKey: 'canvas_daily_stats',
      fieldKey: 'revenue',
      subjectType: 'USER',
      subjectId: 'bob',
      policy: 'DENY',
      mask: { strategy: 'FIXED', replacement: '***' },
      enabled: true,
    }))
  }, 30000)

  it('deletes BI resource row and column permissions from the governance workbench', async () => {
    vi.mocked(biApi.listResourcePermissions).mockResolvedValue(ok([
      {
        id: 501,
        tenantId: 7,
        workspaceId: 3,
        resourceType: 'DASHBOARD',
        resourceKey: 'canvas-effect',
        resourceId: null,
        subjectType: 'USER',
        subjectId: 'alice',
        actionKey: 'USE',
        effect: 'ALLOW',
      },
    ]))
    vi.mocked(biApi.listRowPermissions).mockResolvedValue(ok([
      {
        id: 601,
        tenantId: 7,
        workspaceId: 3,
        datasetKey: 'canvas_daily_stats',
        ruleKey: 'canvas_scope',
        subjectType: 'ROLE',
        subjectId: 'analyst',
        filterJson: '{"canvas_id":12}',
        enabled: true,
      },
    ]))
    vi.mocked(biApi.listColumnPermissions).mockResolvedValue(ok([
      {
        id: 701,
        tenantId: 7,
        workspaceId: 3,
        datasetKey: 'canvas_daily_stats',
        fieldKey: 'canvas_name',
        subjectType: 'ROLE',
        subjectId: 'analyst',
        policy: 'MASK',
        maskJson: '{"type":"partial"}',
      },
    ]))
    vi.mocked(biApi.deleteResourcePermission).mockResolvedValue(ok(undefined))
    vi.mocked(biApi.deleteRowPermission).mockResolvedValue(ok(undefined))
    vi.mocked(biApi.deleteColumnPermission).mockResolvedValue(ok(undefined))

    renderBiRoute('/bi')

    fireEvent.click(await screen.findByLabelText('删除资源权限 #501'))
    fireEvent.click(await screen.findByLabelText('删除行权限 #601'))
    fireEvent.click(await screen.findByLabelText('删除列权限 #701'))

    await waitFor(() => expect(biApi.deleteResourcePermission).toHaveBeenCalledWith(501))
    await waitFor(() => expect(biApi.deleteRowPermission).toHaveBeenCalledWith(601))
    await waitFor(() => expect(biApi.deleteColumnPermission).toHaveBeenCalledWith(701))
  }, 30000)

  it('saves and refreshes dataset extract acceleration settings', async () => {
    vi.mocked(biApi.listDatasetResources).mockResolvedValue(ok([
      {
        datasetKey: 'canvas_daily_stats',
        name: 'Canvas Daily Stats',
        datasetType: 'TABLE',
        tableExpression: 'canvas_dws.canvas_daily_stats',
        tenantColumn: 'tenant_id',
        model: {},
        fields: [],
        metrics: [],
        status: 'PUBLISHED',
        source: 'PERSISTED',
      },
    ]))
    vi.mocked(biApi.getDatasetAccelerationPolicy).mockResolvedValue(ok({
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
      recentRuns: [],
    }))
    vi.mocked(biApi.updateDatasetAccelerationPolicy).mockResolvedValue(ok({
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
      recentRuns: [],
    }))
    vi.mocked(biApi.refreshDatasetAcceleration).mockResolvedValue(ok({
      id: 32,
      datasetKey: 'canvas_daily_stats',
      status: 'SUCCESS',
      rowCount: 42000,
      durationMs: 137,
      materializedTable: 'bi_extract.t7_canvas_daily_stats_20260606101630',
      requestedBy: 'alice',
      startedAt: '2026-06-06T10:16:30',
      finishedAt: '2026-06-06T10:16:30',
      errorSummary: null,
    }))
    vi.mocked(biApi.runDatasetAccelerationScheduler).mockResolvedValue(ok({
      policiesChecked: 3,
      refreshed: 2,
      skipped: 1,
      failed: 0,
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
    }))

    renderBiRoute('/bi?dashboard=canvas-effect&canvasId=12')

    expect(await screen.findByText('数据集加速')).toBeInTheDocument()
    await waitFor(() => expect(biApi.getDatasetAccelerationPolicy).toHaveBeenCalledWith('canvas_daily_stats'))
    fireEvent.click(screen.getByRole('button', { name: /保存加速/ }))
    await waitFor(() => expect(biApi.updateDatasetAccelerationPolicy).toHaveBeenCalledWith('canvas_daily_stats', {
      enabled: true,
      accelerationMode: 'EXTRACT',
      refreshMode: 'SCHEDULED',
      refreshIntervalMinutes: 30,
      ttlSeconds: 900,
      maxRows: 500000,
      cronExpression: '0 0/30 * * * ?',
    }))
    fireEvent.click(screen.getByRole('button', { name: /刷新抽取/ }))
    await waitFor(() => expect(biApi.refreshDatasetAcceleration).toHaveBeenCalledWith('canvas_daily_stats'))
    fireEvent.click(screen.getByRole('button', { name: /运行抽取调度/ }))
    await waitFor(() => expect(biApi.runDatasetAccelerationScheduler).toHaveBeenCalled())
    expect(await screen.findByText('抽取调度 policies 3 · refreshed 2 · skipped 1 · failed 0')).toBeInTheDocument()
    expect(await screen.findByText('orders_api_extract')).toBeInTheDocument()
    expect(screen.getByText('REFRESHED')).toBeInTheDocument()
    expect(screen.getByText('run#91 · 42000 行 · 137 ms')).toBeInTheDocument()
    expect(screen.getByText('fresh_api_extract')).toBeInTheDocument()
    expect(screen.getByText('not due')).toBeInTheDocument()
  }, 60000)

  it('loads and saves Quick Engine capacity alert policy from the governance panel', async () => {
    vi.mocked(biApi.getQuickEngineCapacity).mockResolvedValue(ok({
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
        notificationChannels: ['EMAIL'],
        notificationReceivers: ['bi-ops'],
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
    }))
    vi.mocked(biApi.upsertQuickEngineCapacityAlertPolicy).mockResolvedValue(ok({
      enabled: true,
      capacityLimitRows: 500000,
      warningThresholdPercent: 75,
      criticalThresholdPercent: 95,
      notificationChannels: ['LARK', 'EMAIL'],
      notificationReceivers: ['bi-ops', 'alice'],
      updatedBy: 'alice',
      updatedAt: '2026-06-06T12:30:00',
    }))
    vi.mocked(biApi.upsertQuickEngineTenantPoolPolicy).mockResolvedValue(ok({
      poolKey: 'GOLD',
      maxConcurrentQueries: 16,
      queueLimit: 120,
      queueTimeoutSeconds: 300,
      poolWeight: 200,
      updatedBy: 'alice',
      updatedAt: '2026-06-06T12:30:00',
    }))

    renderBiRoute('/bi?dashboard=canvas-effect&canvasId=12')

    expect(await screen.findByText('Quick 引擎容量')).toBeInTheDocument()
    expect(await screen.findByText('173000/200000 行 · 86.5% · WARNING')).toBeInTheDocument()
    expect(await screen.findByText('2/4 并发 · 1/10 队列 · NORMAL')).toBeInTheDocument()
    const capacityDetail = screen.getByRole('region', { name: 'Quick引擎容量明细' })
    expect(within(capacityDetail).getByText('node_daily_stats')).toBeInTheDocument()
    fireEvent.change(screen.getByLabelText('Quick引擎容量上限'), {
      target: { value: '500000' },
    })
    fireEvent.change(screen.getByLabelText('Quick引擎预警阈值'), {
      target: { value: '75' },
    })
    fireEvent.change(screen.getByLabelText('Quick引擎严重阈值'), {
      target: { value: '95' },
    })
    fireEvent.change(screen.getByLabelText('Quick引擎通知渠道'), {
      target: { value: 'LARK, EMAIL' },
    })
    fireEvent.change(screen.getByLabelText('Quick引擎通知接收人'), {
      target: { value: 'bi-ops, alice' },
    })
    fireEvent.click(screen.getByRole('button', { name: /保存容量告警/ }))

    await waitFor(() => expect(biApi.upsertQuickEngineCapacityAlertPolicy).toHaveBeenCalledWith({
      enabled: true,
      capacityLimitRows: 500000,
      warningThresholdPercent: 75,
      criticalThresholdPercent: 95,
      notificationChannels: ['LARK', 'EMAIL'],
      notificationReceivers: ['bi-ops', 'alice'],
    }))
    fireEvent.change(screen.getByLabelText('Quick引擎容量池'), {
      target: { value: 'gold' },
    })
    fireEvent.change(screen.getByLabelText('Quick引擎最大并发'), {
      target: { value: '16' },
    })
    fireEvent.change(screen.getByLabelText('Quick引擎队列上限'), {
      target: { value: '120' },
    })
    fireEvent.change(screen.getByLabelText('Quick引擎队列等待秒数'), {
      target: { value: '300' },
    })
    fireEvent.change(screen.getByLabelText('Quick引擎容量池权重'), {
      target: { value: '200' },
    })
    fireEvent.click(screen.getByRole('button', { name: /保存容量池/ }))

    await waitFor(() => expect(biApi.upsertQuickEngineTenantPoolPolicy).toHaveBeenCalledWith({
      poolKey: 'GOLD',
      maxConcurrentQueries: 16,
      queueLimit: 120,
      queueTimeoutSeconds: 300,
      poolWeight: 200,
    }))
  }, 60000)
})

function renderBiRoute(initialEntry: string) {
  render(
    <MemoryRouter initialEntries={[initialEntry]}>
      <Routes>
        <Route path="/bi" element={<BiWorkbenchPage />} />
      </Routes>
    </MemoryRouter>,
  )
}
