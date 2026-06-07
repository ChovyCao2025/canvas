/* @vitest-environment jsdom */
import { render, screen } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { describe, expect, it, vi } from 'vitest'
import LiveRegion from '../accessibility/LiveRegion'
import { buildRouteAnnouncement } from '../accessibility/RouteA11y'
import AppLayout from './AppLayout'

vi.mock('../../context/AuthContext', () => ({
  useAuth: () => ({
    user: { displayName: 'Alice', role: 'TENANT_ADMIN' },
    isAdmin: true,
    canManageTenants: false,
    logout: vi.fn(),
  }),
}))

vi.mock('../notifications/NotificationBell', () => ({
  default: () => <button type="button">通知</button>,
}))

vi.mock('../../services/api', () => ({
  authApi: { logout: vi.fn() },
}))

describe('app shell accessibility', () => {
  it('renders live-region text for screen readers', () => {
    render(<LiveRegion message="已进入旅程管理" />)

    expect(screen.getByRole('status')).toHaveTextContent('已进入旅程管理')
  })

  it('builds route announcements from known paths', () => {
    expect(buildRouteAnnouncement('/canvas')).toBe('已进入旅程管理')
    expect(buildRouteAnnouncement('/admin/users')).toBe('已进入用户管理')
    expect(buildRouteAnnouncement('/marketing-preferences')).toBe('已进入偏好中心')
    expect(buildRouteAnnouncement('/marketing-forms')).toBe('已进入表单中心')
    expect(buildRouteAnnouncement('/growth-activities')).toBe('已进入增长活动中心')
    expect(buildRouteAnnouncement('/content-hub')).toBe('已进入内容中心')
    expect(buildRouteAnnouncement('/message-templates')).toBe('已进入模板中心')
    expect(buildRouteAnnouncement('/demo-sandbox')).toBe('已进入演示沙箱')
    expect(buildRouteAnnouncement('/conversations')).toBe('已进入会话工作台')
    expect(buildRouteAnnouncement('/channel-connectors')).toBe('已进入渠道连接器')
    expect(buildRouteAnnouncement('/public/forms/signup')).toBe('已进入公开表单')
  })

  it('renders skip link, main landmark, and named navigation', () => {
    render(
      <MemoryRouter initialEntries={['/demo-sandbox']}>
        <Routes>
          <Route element={<AppLayout />}>
            <Route path="/canvas" element={<div>旅程内容</div>} />
            <Route path="/channel-connectors" element={<div>渠道连接器内容</div>} />
            <Route path="/content-hub" element={<div>内容中心内容</div>} />
            <Route path="/growth-activities" element={<div>增长活动内容</div>} />
            <Route path="/message-templates" element={<div>模板中心内容</div>} />
            <Route path="/demo-sandbox" element={<div>演示沙箱内容</div>} />
          </Route>
        </Routes>
      </MemoryRouter>,
    )

    expect(screen.getByRole('link', { name: '跳到主要内容' })).toHaveAttribute('href', '#main-content')
    expect(screen.getByRole('navigation', { name: '主导航' })).toBeInTheDocument()
    expect(screen.getByText('内容中心')).toBeInTheDocument()
    expect(screen.getByText('增长活动')).toBeInTheDocument()
    expect(screen.getByText('演示沙箱')).toBeInTheDocument()
    expect(screen.getByRole('main')).toHaveAttribute('id', 'main-content')
  })

  it('renders conversation workspace navigation under operations', () => {
    render(
      <MemoryRouter initialEntries={['/conversations']}>
        <Routes>
          <Route element={<AppLayout />}>
            <Route path="/conversations" element={<div>会话工作台内容</div>} />
          </Route>
        </Routes>
      </MemoryRouter>,
    )

    expect(screen.getByText('会话工作台')).toBeInTheDocument()
  })
})
