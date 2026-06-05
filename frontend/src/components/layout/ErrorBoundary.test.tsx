/* @vitest-environment jsdom */
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, describe, expect, it, vi } from 'vitest'
import ErrorBoundary from './ErrorBoundary'

function Boom({ message = 'route failed' }: { message?: string }): null {
  throw new Error(message)
}

function installMemoryStorage() {
  const data = new Map<string, string>()
  const storage = {
    get length() {
      return data.size
    },
    clear: () => data.clear(),
    getItem: (key: string) => data.get(key) ?? null,
    key: (index: number) => Array.from(data.keys())[index] ?? null,
    removeItem: (key: string) => data.delete(key),
    setItem: (key: string, value: string) => data.set(key, value),
  } as Storage
  Object.defineProperty(globalThis, 'localStorage', {
    value: storage,
    configurable: true,
  })
  return storage
}

describe('ErrorBoundary', () => {
  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('renders children when no error is thrown', () => {
    render(
      <ErrorBoundary routeName="首页">
        <main>正常内容</main>
      </ErrorBoundary>,
    )

    expect(screen.getByText('正常内容')).toBeInTheDocument()
  })

  it('renders an accessible fallback without leaking secrets', () => {
    vi.spyOn(console, 'error').mockImplementation(() => {})
    const localStorage = installMemoryStorage()
    localStorage.setItem('canvas_token', 'local-storage-token')

    render(
      <ErrorBoundary routeName="画布路由 token=secret-token">
        <Boom message="database failure" />
      </ErrorBoundary>,
    )

    const alert = screen.getByRole('alert')
    expect(alert).toHaveTextContent('画布路由 token=****加载失败')
    expect(screen.getByRole('button', { name: '重试画布路由 token=****' })).toBeInTheDocument()
    expect(document.body).not.toHaveTextContent('secret-token')
    expect(document.body).not.toHaveTextContent('local-storage-token')
    expect(document.body).not.toHaveTextContent('stack trace')
  })

  it('resets the failed state when retry is clicked', async () => {
    vi.spyOn(console, 'error').mockImplementation(() => {})
    let shouldThrow = true
    function Flaky() {
      if (shouldThrow) throw new Error('first render failed')
      return <div>已恢复</div>
    }

    render(
      <ErrorBoundary routeName="配置页">
        <Flaky />
      </ErrorBoundary>,
    )
    shouldThrow = false
    await userEvent.click(screen.getByRole('button', { name: '重试配置页' }))

    expect(screen.getByText('已恢复')).toBeInTheDocument()
  })

  it('resets the failed state when resetKey changes', () => {
    vi.spyOn(console, 'error').mockImplementation(() => {})
    let shouldThrow = true
    function Flaky() {
      if (shouldThrow) throw new Error('route render failed')
      return <div>路由已恢复</div>
    }

    const { rerender } = render(
      <ErrorBoundary routeName="管理员路由" resetKey="admin-v1">
        <Flaky />
      </ErrorBoundary>,
    )

    expect(screen.getByRole('alert')).toHaveTextContent('管理员路由加载失败')
    shouldThrow = false
    rerender(
      <ErrorBoundary routeName="管理员路由" resetKey="admin-v2">
        <Flaky />
      </ErrorBoundary>,
    )

    expect(screen.getByText('路由已恢复')).toBeInTheDocument()
  })
})
