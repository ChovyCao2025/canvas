/* @vitest-environment jsdom */
import { render, screen } from '@testing-library/react'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
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

const originalMatchMedia = window.matchMedia

afterEach(() => {
  Object.defineProperty(window, 'matchMedia', {
    configurable: true,
    writable: true,
    value: originalMatchMedia,
  })
})

function setMobileViewport() {
  Object.defineProperty(window, 'matchMedia', {
    configurable: true,
    writable: true,
    value: vi.fn().mockImplementation((query: string) => ({
      matches: query === '(max-width: 767px)',
      media: query,
      onchange: null,
      addEventListener: vi.fn(),
      removeEventListener: vi.fn(),
      addListener: vi.fn(),
      removeListener: vi.fn(),
      dispatchEvent: vi.fn(),
    })),
  })
}

describe('app shell responsive layout', () => {
  it('constrains the mobile content layout to the viewport width', () => {
    setMobileViewport()

    render(
      <MemoryRouter initialEntries={['/marketing-monitoring']}>
        <Routes>
          <Route element={<AppLayout />}>
            <Route path="/marketing-monitoring" element={<div>监测工作台内容</div>} />
          </Route>
        </Routes>
      </MemoryRouter>,
    )

    const main = screen.getByRole('main')
    const contentLayout = main.parentElement
    const rootLayout = contentLayout?.parentElement

    expect(rootLayout).toHaveStyle({
      maxWidth: '100vw',
      overflowX: 'hidden',
    })
    expect(contentLayout).toHaveStyle({
      minWidth: '0px',
      maxWidth: '100vw',
      overflowX: 'hidden',
    })
  })
})
