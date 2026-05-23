// @vitest-environment jsdom

import { act } from 'react'
import { createRoot } from 'react-dom/client'
import { MemoryRouter } from 'react-router-dom'
import { describe, expect, it, vi } from 'vitest'
import AudienceDataSourcePage from './index'

const { list } = vi.hoisted(() => ({
  list: vi.fn().mockResolvedValue({ data: [] }),
}))

vi.mock('../../services/audienceDataSourceApi', () => ({
  audienceDataSourceApi: {
    list,
    create: vi.fn(),
    update: vi.fn(),
    delete: vi.fn(),
  },
}))

describe('AudienceDataSourcePage', () => {
  it('shows management title and create button', async () => {
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
    ;(globalThis as typeof globalThis & { IS_REACT_ACT_ENVIRONMENT?: boolean }).IS_REACT_ACT_ENVIRONMENT = true

    const container = document.createElement('div')
    document.body.appendChild(container)
    const root = createRoot(container)

    try {
      await act(async () => {
        root.render(
          <MemoryRouter>
            <AudienceDataSourcePage />
          </MemoryRouter>,
        )
      })
      await act(async () => {
        await Promise.resolve()
      })

      expect(list).toHaveBeenCalledTimes(1)
      expect(container.textContent).toContain('人群数据源')
      expect(container.textContent).toContain('新建数据源')
    } finally {
      root.unmount()
      container.remove()
    }
  })
})
