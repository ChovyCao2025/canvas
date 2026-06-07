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

  it('saves edited chart query fields and copies chart drafts from the resource workbench', async () => {
    vi.mocked(biApi.saveChartDraft).mockImplementation((_chartKey: string, resource) => Promise.resolve(ok(resource)))

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
    fireEvent.change(screen.getByLabelText('图表取数上限'), {
      target: { value: '250' },
    })
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
          limit: 250,
        }),
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
    expect(screen.getByText('引用影响：数据集 campaign_daily_stats · 2 维度 · 2 指标')).toBeInTheDocument()
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
    fireEvent.change(screen.getByLabelText('门户菜单'), {
      target: { value: 'sales' },
    })
    fireEvent.click(screen.getByRole('button', { name: '门户菜单上移' }))
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
        }),
        menus: [
          expect.objectContaining({ menuKey: 'sales', sortOrder: 1 }),
          expect.objectContaining({ menuKey: 'overview', sortOrder: 2 }),
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
    fireEvent.click(screen.getByRole('button', { name: /嵌入/ }))

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
          'filter-trigger-type': 'TIME,MQ',
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
    expect(screen.getByText('Doris Campaign Mart')).toBeInTheDocument()
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
    expect(await screen.findByText('Orders API')).toBeInTheDocument()
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
    expect(screen.getByText('2026-06-01')).toBeInTheDocument()
  }, 30000)

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
    ]))
    vi.mocked(biApi.saveDatasetDraft).mockImplementation((_datasetKey: string, resource) => Promise.resolve(ok(resource)))

    renderBiRoute('/bi')

    fireEvent.change(await screen.findByLabelText('数据集字段选择'), {
      target: { value: 'stat_date,channel' },
    })
    fireEvent.change(screen.getByLabelText('数据集字段文件夹'), {
      target: { value: '时间与渠道' },
    })
    fireEvent.click(screen.getByRole('button', { name: '批量移动字段到文件夹' }))
    fireEvent.click(screen.getByRole('button', { name: '保存数据集草稿' }))

    await waitFor(() => expect(biApi.saveDatasetDraft).toHaveBeenCalledWith(
      'campaign_model',
      expect.objectContaining({
        datasetKey: 'campaign_model',
        fields: [
          expect.objectContaining({ fieldKey: 'stat_date', folderKey: '时间与渠道' }),
          expect.objectContaining({ fieldKey: 'channel', folderKey: '时间与渠道' }),
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
    expect(screen.getByText('字段文件夹：时间与渠道 · 2 字段')).toBeInTheDocument()
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
  }, 30000)

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
  }, 30000)
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
