/* @vitest-environment jsdom */
import { render, screen, waitFor } from '@testing-library/react'
import { afterEach, describe, expect, it, vi } from 'vitest'

import MessageTemplatesPage from './index'
import { messageTemplateApi } from '../../services/messageTemplateApi'

vi.mock('../../services/messageTemplateApi', () => ({
  messageTemplateApi: {
    search: vi.fn(),
    create: vi.fn(),
    preview: vi.fn(),
  },
}))

describe('MessageTemplatesPage', () => {
  afterEach(() => {
    vi.clearAllMocks()
  })

  it('loads templates and renders empty state with editor controls', async () => {
    vi.mocked(messageTemplateApi.search).mockResolvedValue({
      code: 0,
      message: 'success',
      data: [],
    })

    render(<MessageTemplatesPage />)

    await waitFor(() => expect(messageTemplateApi.search).toHaveBeenCalled())
    expect(screen.getByText('暂无消息模板')).toBeInTheDocument()
    expect(screen.getByLabelText('模板编码')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /保存模板/ })).toBeInTheDocument()
  })
})
