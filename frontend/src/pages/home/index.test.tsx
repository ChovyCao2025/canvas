/* @vitest-environment jsdom */
import { fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { homeApi } from '../../services/api'
import type { HomeOverview } from './homeOverview'
import HomePage from './index'

const navigateMock = vi.hoisted(() => vi.fn())

vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual<typeof import('react-router-dom')>('react-router-dom')
  return {
    ...actual,
    useNavigate: () => navigateMock,
  }
})

vi.mock('../../services/api', () => ({
  homeApi: {
    overview: vi.fn(),
  },
}))

const overviewMock = vi.mocked(homeApi.overview)

describe('HomePage', () => {
  beforeEach(() => {
    navigateMock.mockReset()
    overviewMock.mockReset()
  })

  it('renders the material ops dashboard layout', async () => {
    overviewMock.mockResolvedValueOnce({ data: overview() })

    renderHome()

    expect(await screen.findByText('运营驾驶舱')).toBeInTheDocument()
    expect(screen.getAllByText('沉睡用户召回').length).toBeGreaterThan(0)
    expect(screen.getByText('失败率 4.8%')).toBeInTheDocument()
    expect(screen.getByText('异常队列')).toBeInTheDocument()
    expect(screen.getByText('Top 旅程表现')).toBeInTheDocument()
    expect(screen.getByText('常用动作')).toBeInTheDocument()
    expect(screen.getByText('128,430')).toBeInTheDocument()
  })

  it('filters loaded journey names locally without re-calling the API', async () => {
    const user = userEvent.setup()
    overviewMock.mockResolvedValueOnce({ data: overview() })

    renderHome()
    await screen.findByText('新人激活 7 日链路')

    await user.type(screen.getByPlaceholderText('搜索当前首页旅程'), '召回')

    expect(screen.queryByText('新人激活 7 日链路')).not.toBeInTheDocument()
    expect(screen.getAllByText('沉睡用户召回').length).toBeGreaterThan(0)
    expect(overviewMock).toHaveBeenCalledTimes(1)
  })

  it('shows a healthy summary when there are no attention items', async () => {
    overviewMock.mockResolvedValueOnce({
      data: {
        ...overview(),
        attentionItems: [],
      },
    })

    renderHome()

    expect(await screen.findByText('当前暂无高优先级异常')).toBeInTheDocument()
    expect(screen.getByText('近 7 天旅程运行稳定，可继续关注触达趋势和 Top 旅程表现')).toBeInTheDocument()
  })

  it('retries after the first overview request fails', async () => {
    const user = userEvent.setup()
    overviewMock
      .mockRejectedValueOnce(new Error('network down'))
      .mockResolvedValueOnce({ data: overview() })

    renderHome()
    expect(await screen.findByText('首页数据加载失败')).toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: /重\s*试/ }))

    await waitFor(() => expect(overviewMock).toHaveBeenCalledTimes(2))
    expect(await screen.findByText('运营驾驶舱')).toBeInTheDocument()
    expect(screen.getAllByText('沉睡用户召回').length).toBeGreaterThan(0)
  })

  it('does not navigate aggregate no-execution attention to canvas 0 edit', async () => {
    overviewMock.mockResolvedValueOnce({
      data: {
        ...overview(),
        topCanvases: [],
        attentionItems: [
          {
            canvasId: 0,
            name: '全部旅程',
            type: 'NO_RECENT_EXECUTIONS',
            message: '最近暂无执行记录',
            severity: 'info',
          },
        ],
      },
    })

    renderHome()

    const aggregateItem = (await screen.findByText('全部旅程')).closest('li')
    expect(aggregateItem).not.toBeNull()
    fireEvent.click(within(aggregateItem as HTMLElement).getByRole('button', { name: '查看' }))

    expect(navigateMock).not.toHaveBeenCalledWith('/canvas/0/edit')
    for (const [path] of navigateMock.mock.calls) {
      expect(String(path)).not.toContain('/canvas/0')
    }
  })
})

function renderHome() {
  render(
    <MemoryRouter>
      <HomePage />
    </MemoryRouter>,
  )
}

function overview(): HomeOverview {
  return {
    range: { days: 7, since: '2026-05-29', until: '2026-06-05' },
    summary: {
      publishedCanvasCount: 42,
      totalExecutions: 246880,
      uniqueUsers: 128430,
      successRate: '98.6%',
      failedExecutions: 318,
    },
    trend: [
      { date: '2026-06-01', total: 12000, failed: 12 },
      { date: '2026-06-02', total: 18000, failed: 20 },
    ],
    topCanvases: [
      { canvasId: 1, name: '新人激活 7 日链路', total: 84000, uniqueUsers: 43000, successRate: '99.2%', failed: 28 },
      { canvasId: 2, name: '沉睡用户召回', total: 52000, uniqueUsers: 31000, successRate: '95.2%', failed: 141 },
    ],
    attentionItems: [
      { canvasId: 2, name: '沉睡用户召回', type: 'HIGH_FAILURE_RATE', message: '失败率 4.8%', severity: 'warning' },
      { canvasId: 3, name: '生日礼遇活动', type: 'NO_RECENT_EXECUTIONS', message: '近 7 天无执行记录', severity: 'info' },
    ],
  }
}
