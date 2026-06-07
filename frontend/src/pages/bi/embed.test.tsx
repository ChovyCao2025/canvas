/* @vitest-environment jsdom */
import { cleanup, render, screen, waitFor } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { MemoryRouter, Route, Routes } from 'react-router-dom'

import BiEmbedPage from './embed'
import { biApi } from '../../services/biApi'
import type { BiDashboardPreset } from '../../services/biApi'

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

const embeddedPreset: BiDashboardPreset = {
  dashboardKey: 'canvas-effect',
  title: 'Embedded Campaign Dashboard',
  description: 'real data embed',
  datasetKey: 'canvas_daily_stats',
  widgets: [
    {
      widgetKey: 'kpi-total',
      title: '总执行数',
      chartType: 'KPI_CARD',
      dimensions: [],
      metrics: ['total_executions'],
      gridX: 0,
      gridY: 0,
      gridW: 6,
      gridH: 4,
      stylePreset: 'primary',
    },
  ],
  filters: [
    {
      filterKey: 'filter-stat-date',
      fieldKey: 'stat_date',
      label: '统计日期',
      controlType: 'DATE_RANGE',
      required: true,
    },
    {
      filterKey: 'filter-trigger-type',
      fieldKey: 'trigger_type',
      label: '触发类型',
      controlType: 'ENUM_MULTI_SELECT',
      required: false,
    },
    {
      filterKey: 'filter-canvas',
      fieldKey: 'canvas_name',
      label: '画布名称',
      controlType: 'TEXT',
      required: false,
    },
  ],
  globalParameters: [
    {
      parameterKey: 'campaignParam',
      fieldKey: 'canvas_name',
      filterKey: 'filter-canvas',
      aliases: ['campaign'],
      locked: true,
    },
  ],
  interactions: [],
  subscriptionChannels: [],
  embedScopes: ['EXTERNAL_TICKET'],
}

describe('BiEmbedPage', () => {
  beforeEach(() => {
    Object.values(biApiMock).forEach(mock => {
      mock.mockReset()
      mock.mockResolvedValue(ok([]))
    })
    vi.mocked(biApi.verifyEmbedTicket).mockResolvedValue(ok({
      tenantId: 7,
      username: 'viewer',
      resourceType: 'DASHBOARD',
      resourceKey: 'canvas-effect',
      scope: 'EXTERNAL_TICKET',
      filters: {
        canvasId: '12',
        'filter-stat-date': '2026-06-01,2026-06-05',
        'filter-trigger-type': 'TIME,MQ',
      },
      parameters: {
        campaignParam: 'Welcome Journey',
      },
      allowedDomains: ['analytics.example.com'],
      nonce: 'nonce-1',
      issuedAt: '2026-06-06T07:40:00Z',
      expiresAt: '2026-06-06T07:55:00Z',
    }))
    vi.mocked(biApi.getEmbedDashboardResource).mockResolvedValue(ok({
      preset: embeddedPreset,
      status: 'PUBLISHED',
      version: 4,
      source: 'PERSISTED',
    }))
    vi.mocked(biApi.getEmbedDashboardRuntimeState).mockResolvedValue(ok({
      dashboardKey: 'canvas-effect',
      username: 'viewer',
      parameters: {
        'filter-canvas': 'Remembered Journey',
      },
      updatedAt: '2026-06-06T07:41:00',
    }))
    vi.mocked(biApi.executeEmbedQuery).mockResolvedValue(ok({
      datasetKey: 'canvas_daily_stats',
      columns: [{ key: 'total_executions', role: 'MEASURE', dataType: 'NUMBER' }],
      rows: [{ total_executions: 2400 }],
      rowCount: 1,
      durationMs: 42,
      sqlHash: 'hash-real-embed',
      cached: false,
    }))
  })

  afterEach(() => {
    cleanup()
    vi.clearAllMocks()
  })

  it('renders dashboard widgets from verified tickets using real query results', async () => {
    renderEmbedRoute('/bi/embed/DASHBOARD/canvas-effect?ticket=ticket-1')

    expect(await screen.findByText('Embedded Campaign Dashboard')).toBeInTheDocument()
    expect(await screen.findByText('2,400')).toBeInTheDocument()
    expect(screen.getByText('实时查询')).toBeInTheDocument()
    expect(screen.getByText('1 行 · 42ms')).toBeInTheDocument()
    expect(screen.getByText('filter-canvas: Welcome Journey')).toBeInTheDocument()
    expect(screen.getByText('campaignParam: Welcome Journey')).toBeInTheDocument()

    await waitFor(() => expect(biApi.executeEmbedQuery).toHaveBeenCalledWith({
      ticket: 'ticket-1',
      resourceType: 'DASHBOARD',
      resourceKey: 'canvas-effect',
      widgetKey: 'kpi-total',
      query: {
        dashboardKey: 'canvas-effect',
        datasetKey: 'canvas_daily_stats',
        dimensions: [],
        metrics: ['total_executions'],
        filters: [
          { field: 'canvas_id', operator: 'EQ', value: 12 },
          { field: 'stat_date', operator: 'BETWEEN', value: ['2026-06-01', '2026-06-05'] },
          { field: 'trigger_type', operator: 'IN', value: ['TIME', 'MQ'] },
          { field: 'canvas_name', operator: 'EQ', value: 'Welcome Journey' },
        ],
        sorts: [],
        limit: 500,
      },
    }))
    expect(biApi.executeQuery).not.toHaveBeenCalled()
    expect(biApi.getEmbedDashboardResource).toHaveBeenCalledWith({
      ticket: 'ticket-1',
      resourceType: 'DASHBOARD',
      resourceKey: 'canvas-effect',
    })
    expect(biApi.getEmbedDashboardRuntimeState).toHaveBeenCalledWith({
      ticket: 'ticket-1',
      resourceType: 'DASHBOARD',
      resourceKey: 'canvas-effect',
    })
    expect(biApi.getDashboardResource).not.toHaveBeenCalled()
  })

  it('reuses signed dashboard runtime state when ticket filters do not override it', async () => {
    vi.mocked(biApi.verifyEmbedTicket).mockResolvedValue(ok({
      tenantId: 7,
      username: 'viewer',
      resourceType: 'DASHBOARD',
      resourceKey: 'canvas-effect',
      scope: 'EXTERNAL_TICKET',
      filters: {
        canvasId: '12',
        'filter-stat-date': '2026-06-01,2026-06-05',
      },
      parameters: {},
      allowedDomains: ['analytics.example.com'],
      nonce: 'nonce-3',
      issuedAt: '2026-06-06T07:40:00Z',
      expiresAt: '2026-06-06T07:55:00Z',
    }))

    renderEmbedRoute('/bi/embed/DASHBOARD/canvas-effect?ticket=ticket-3')

    expect(await screen.findByText('filter-canvas: Remembered Journey')).toBeInTheDocument()
    await waitFor(() => expect(biApi.executeEmbedQuery).toHaveBeenCalledWith(expect.objectContaining({
      query: expect.objectContaining({
        filters: expect.arrayContaining([
          { field: 'canvas_name', operator: 'EQ', value: 'Remembered Journey' },
        ]),
      }),
    })))
  })

  it('does not execute dashboard queries when the ticket targets another resource', async () => {
    vi.mocked(biApi.verifyEmbedTicket).mockResolvedValue(ok({
      tenantId: 7,
      username: 'viewer',
      resourceType: 'DASHBOARD',
      resourceKey: 'other-dashboard',
      scope: 'EXTERNAL_TICKET',
      filters: {},
      parameters: {},
      allowedDomains: ['analytics.example.com'],
      nonce: 'nonce-2',
      issuedAt: '2026-06-06T07:40:00Z',
      expiresAt: '2026-06-06T07:55:00Z',
    }))

    renderEmbedRoute('/bi/embed/DASHBOARD/canvas-effect?ticket=ticket-2')

    expect(await screen.findByText('嵌入 ticket 与当前资源不匹配')).toBeInTheDocument()
    expect(biApi.getEmbedDashboardResource).not.toHaveBeenCalled()
    expect(biApi.getEmbedDashboardRuntimeState).not.toHaveBeenCalled()
    expect(biApi.getDashboardResource).not.toHaveBeenCalled()
    expect(biApi.executeEmbedQuery).not.toHaveBeenCalled()
    expect(biApi.executeQuery).not.toHaveBeenCalled()
  })
})

function renderEmbedRoute(initialEntry: string) {
  return render(
    <MemoryRouter initialEntries={[initialEntry]}>
      <Routes>
        <Route path="/bi/embed/:resourceType/:resourceKey" element={<BiEmbedPage />} />
      </Routes>
    </MemoryRouter>,
  )
}
