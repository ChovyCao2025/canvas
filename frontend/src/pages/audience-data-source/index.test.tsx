// @vitest-environment jsdom

import { act } from 'react'
import { createRoot } from 'react-dom/client'
import { MemoryRouter } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import AudienceDataSourcePage, { toAudienceDataSourceBody } from './index'

const { list, update } = vi.hoisted(() => ({
  list: vi.fn().mockResolvedValue({ data: [] }),
  update: vi.fn().mockResolvedValue({ data: {} }),
}))

vi.mock('../../services/audienceDataSourceApi', () => ({
  audienceDataSourceApi: {
    list,
    create: vi.fn(),
    update,
    delete: vi.fn(),
  },
}))

describe('AudienceDataSourcePage', () => {
  beforeEach(() => {
    list.mockReset()
    list.mockResolvedValue({ data: [] })
    update.mockReset()
    update.mockResolvedValue({ data: {} })
  })

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
      expect(container.textContent).toContain('暂无数据源')

      const createButton = container.querySelector('button')
      expect(createButton).not.toBeNull()

      await act(async () => {
        createButton?.dispatchEvent(new MouseEvent('click', { bubbles: true }))
      })

      expect(document.body.textContent).toContain('JDBC URL')
    } finally {
      root.unmount()
      container.remove()
    }
  })

  it('disables delete action when the data source is referenced', async () => {
    list.mockResolvedValueOnce({
      data: [
        {
          id: 1,
          name: '已引用数据源',
          url: 'jdbc:mysql://demo',
          username: 'root',
          enabled: 1,
          referenceCount: 2,
        },
      ],
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

      expect(container.textContent).toContain('已引用数据源')
      expect(container.querySelector('button[disabled]')).not.toBeNull()
    } finally {
      root.unmount()
      container.remove()
    }
  })

  it('omits password from the submit payload when the field is blank in edit mode', () => {
    const body = toAudienceDataSourceBody({
      name: '编辑中的数据源',
      description: '历史描述',
      url: 'jdbc:mysql://demo',
      username: 'root',
      password: '',
      enabled: true,
    })

    expect(body.password).toBeUndefined()
  })
})
