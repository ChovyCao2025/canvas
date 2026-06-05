/* @vitest-environment jsdom */
import React, { act } from 'react'
import { createRoot, type Root } from 'react-dom/client'
import { MemoryRouter } from 'react-router-dom'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
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

let root: Root | null = null
let container: HTMLDivElement | null = null
;(globalThis as typeof globalThis & { IS_REACT_ACT_ENVIRONMENT: boolean }).IS_REACT_ACT_ENVIRONMENT = true

describe('HomePage', () => {
  beforeEach(() => {
    navigateMock.mockReset()
    overviewMock.mockReset()
    installBrowserShims()
  })

  afterEach(async () => {
    if (root) {
      await act(async () => {
        root?.unmount()
      })
    }
    root = null
    container = null
    document.body.innerHTML = ''
  })

  it('renders the material ops dashboard layout', async () => {
    overviewMock.mockResolvedValueOnce({ data: overview() })

    await renderHome()

    expect(text()).toContain('运营驾驶舱')
    expect(text()).toContain('沉睡用户召回')
    expect(text()).toContain('失败率 4.8%')
    expect(text()).toContain('异常队列')
    expect(text()).toContain('Top 旅程表现')
    expect(text()).toContain('常用动作')
    expect(text()).toContain('128,430')
  })

  it('filters loaded journey names locally without re-calling the API', async () => {
    overviewMock.mockResolvedValueOnce({ data: overview() })

    await renderHome()
    const search = inputByPlaceholder('搜索当前首页旅程')

    await act(async () => {
      search.value = '召回'
      search.dispatchEvent(new Event('input', { bubbles: true }))
      search.dispatchEvent(new Event('change', { bubbles: true }))
    })

    expect(text()).not.toContain('新人激活 7 日链路')
    expect(text()).toContain('沉睡用户召回')
    expect(overviewMock).toHaveBeenCalledTimes(1)
  })

  it('shows a healthy summary when there are no attention items', async () => {
    overviewMock.mockResolvedValueOnce({
      data: {
        ...overview(),
        attentionItems: [],
      },
    })

    await renderHome()

    expect(text()).toContain('当前暂无高优先级异常')
    expect(text()).toContain('近 7 天旅程运行稳定，可继续关注触达趋势和 Top 旅程表现')
  })

  it('retries after the first overview request fails', async () => {
    overviewMock
      .mockRejectedValueOnce(new Error('network down'))
      .mockResolvedValueOnce({ data: overview() })

    await renderHome({ waitForData: false })
    await waitFor(() => expect(text()).toContain('首页数据加载失败'))

    await act(async () => {
      buttonByText('重试').click()
    })

    await waitFor(() => expect(text()).toContain('运营驾驶舱'))
    await waitFor(() => expect(text()).toContain('沉睡用户召回'))
    expect(overviewMock).toHaveBeenCalledTimes(2)
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

    await renderHome()

    const action = buttonInListItem('全部旅程', '查看')
    await act(async () => {
      action.click()
    })

    expect(navigateMock).not.toHaveBeenCalledWith('/canvas/0/edit')
    for (const [path] of navigateMock.mock.calls) {
      expect(String(path)).not.toContain('/canvas/0')
    }
  })
})

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

async function renderHome({ waitForData = true } = {}) {
  container = document.createElement('div')
  document.body.appendChild(container)
  root = createRoot(container)

  await act(async () => {
    root?.render(
      <MemoryRouter>
        <HomePage />
      </MemoryRouter>,
    )
  })

  if (waitForData) {
    await waitFor(() => expect(overviewMock).toHaveBeenCalledTimes(1))
    await waitFor(() => expect(text()).toContain('运营驾驶舱'))
  }
}

function text() {
  return document.body.textContent ?? ''
}

function inputByPlaceholder(placeholder: string) {
  const input = document.querySelector<HTMLInputElement>(`input[placeholder="${placeholder}"]`)
  if (!input) throw new Error(`Input with placeholder "${placeholder}" not found`)
  return input
}

function buttonByText(label: string) {
  const button = Array.from(document.querySelectorAll('button'))
    .find(element => element.textContent?.includes(label))
    ?? document.querySelector<HTMLButtonElement>('.ant-alert-action button')
  if (!button) throw new Error(`Button "${label}" not found`)
  return button
}

function buttonInListItem(itemText: string, label: string) {
  const item = Array.from(document.querySelectorAll('li'))
    .find(element => element.textContent?.includes(itemText))
  const button = item
    ? Array.from(item.querySelectorAll('button')).find(element => element.textContent?.includes(label))
    : null
  if (!button) throw new Error(`Button "${label}" for "${itemText}" not found`)
  return button
}

async function waitFor(assertion: () => void | Promise<void>) {
  let lastError: unknown
  for (let attempt = 0; attempt < 40; attempt += 1) {
    try {
      await assertion()
      return
    } catch (error) {
      lastError = error
      await act(async () => {
        await new Promise(resolve => setTimeout(resolve, 25))
      })
    }
  }
  throw lastError
}

function installBrowserShims() {
  const getComputedStyle = window.getComputedStyle.bind(window)
  Object.defineProperty(window, 'getComputedStyle', {
    writable: true,
    value: (element: Element) => getComputedStyle(element),
  })

  Object.defineProperty(window, 'matchMedia', {
    writable: true,
    value: vi.fn().mockImplementation((query: string) => ({
      matches: false,
      media: query,
      onchange: null,
      addListener: vi.fn(),
      removeListener: vi.fn(),
      addEventListener: vi.fn(),
      removeEventListener: vi.fn(),
      dispatchEvent: vi.fn(),
    })),
  })

  class ResizeObserverMock {
    observe() {}
    unobserve() {}
    disconnect() {}
  }

  Object.defineProperty(window, 'ResizeObserver', {
    writable: true,
    value: ResizeObserverMock,
  })
  Object.defineProperty(globalThis, 'ResizeObserver', {
    writable: true,
    value: ResizeObserverMock,
  })
}
