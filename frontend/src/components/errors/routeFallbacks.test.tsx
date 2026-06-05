/* @vitest-environment jsdom */
import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { describe, expect, it } from 'vitest'
import ForbiddenPage from './ForbiddenPage'
import NotFoundPage from './NotFoundPage'

describe('route fallback pages', () => {
  it('renders a stable forbidden page', () => {
    render(<MemoryRouter><ForbiddenPage /></MemoryRouter>)

    expect(screen.getByRole('heading', { name: '无权限访问' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: '返回首页' })).toHaveAttribute('href', '/home')
  })

  it('renders a stable not-found page', () => {
    render(<MemoryRouter><NotFoundPage /></MemoryRouter>)

    expect(screen.getByRole('heading', { name: '页面不存在' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: '返回旅程管理' })).toHaveAttribute('href', '/canvas')
  })
})
