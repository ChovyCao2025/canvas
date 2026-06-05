/* @vitest-environment jsdom */
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { describe, expect, it, vi } from 'vitest'
import AppErrorBoundary from './AppErrorBoundary'

function Boom(): null {
  throw new Error('route failed')
}

describe('AppErrorBoundary', () => {
  it('renders a recoverable alert when a child throws', () => {
    vi.spyOn(console, 'error').mockImplementation(() => {})

    render(
      <MemoryRouter>
        <AppErrorBoundary><Boom /></AppErrorBoundary>
      </MemoryRouter>,
    )

    expect(screen.getByRole('alert')).toHaveTextContent('页面加载失败')
    expect(screen.getByRole('button', { name: '重试' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: '返回首页' })).toHaveAttribute('href', '/home')
  })

  it('resets the failure state when retry is clicked', async () => {
    vi.spyOn(console, 'error').mockImplementation(() => {})
    let shouldThrow = true
    function Flaky() {
      if (shouldThrow) throw new Error('first render failed')
      return <div>已恢复</div>
    }

    render(
      <MemoryRouter>
        <AppErrorBoundary><Flaky /></AppErrorBoundary>
      </MemoryRouter>,
    )
    shouldThrow = false
    await userEvent.click(screen.getByRole('button', { name: '重试' }))

    expect(screen.getByText('已恢复')).toBeInTheDocument()
  })
})
