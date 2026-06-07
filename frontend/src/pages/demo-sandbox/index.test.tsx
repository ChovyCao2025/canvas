/* @vitest-environment jsdom */
import { render, screen, waitFor } from '@testing-library/react'
import { afterEach, describe, expect, it, vi } from 'vitest'

import DemoSandboxPage from './index'
import { demoSandboxApi } from '../../services/demoSandboxApi'

vi.mock('../../services/demoSandboxApi', () => ({
  demoSandboxApi: {
    install: vi.fn(),
    reset: vi.fn(),
    reply: vi.fn(),
    expired: vi.fn(),
  },
}))

describe('DemoSandboxPage', () => {
  afterEach(() => {
    vi.clearAllMocks()
  })

  it('loads expired sandboxes and renders empty state with install controls', async () => {
    vi.mocked(demoSandboxApi.expired).mockResolvedValue({
      code: 0,
      message: 'success',
      data: [],
    })

    render(<DemoSandboxPage />)

    await waitFor(() => expect(demoSandboxApi.expired).toHaveBeenCalled())
    expect(screen.getByText('暂无过期沙箱')).toBeInTheDocument()
    expect(screen.getByLabelText('租户 ID')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /安装演示沙箱/ })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /重置沙箱/ })).toBeInTheDocument()
  })

  it('renders sandbox conversation reply simulator controls', async () => {
    vi.mocked(demoSandboxApi.expired).mockResolvedValue({
      code: 0,
      message: 'success',
      data: [],
    })

    render(<DemoSandboxPage />)

    await waitFor(() => expect(demoSandboxApi.expired).toHaveBeenCalled())
    expect(screen.getByText('模拟会话回复')).toBeInTheDocument()
    expect(screen.getByLabelText('用户 ID')).toBeInTheDocument()
    expect(screen.getByLabelText('回复内容')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /发送模拟回复/ })).toBeInTheDocument()
  })
})
