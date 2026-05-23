import { describe, expect, it, vi } from 'vitest'
import { MemoryRouter } from 'react-router-dom'
import { renderToStaticMarkup } from 'react-dom/server'
import AudienceDataSourcePage from './index'

vi.mock('../../services/audienceDataSourceApi', () => ({
  audienceDataSourceApi: {
    list: vi.fn().mockResolvedValue({ data: [] }),
    create: vi.fn(),
    update: vi.fn(),
    delete: vi.fn(),
  },
}))

describe('AudienceDataSourcePage', () => {
  it('shows management title and create button', () => {
    const html = renderToStaticMarkup(
      <MemoryRouter>
        <AudienceDataSourcePage />
      </MemoryRouter>,
    )

    expect(html).toContain('人群数据源')
    expect(html).toContain('新建数据源')
  })
})
