# Frontend Resilience And A11y Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add recoverable route errors, classified API failures, offline/status announcements, accessible app-shell navigation, and dirty-editor unload protection.

**Architecture:** Keep existing pure Vitest tests on the `node` environment and opt component tests into jsdom per file. Add small reusable frontend primitives, then wire them into `App`, `guards`, `AppLayout`, `api.ts`, and the canvas editor.

**Tech Stack:** React 18, React Router 6, Ant Design, Axios, Vite, Vitest, jsdom, Testing Library React, TypeScript.

---

## Spec Reference

- `docs/product-evolution/specs/p0-002-frontend-resilience-and-a11y.md`
- Source item: `docs/product-evolution/todo/p0/frontend-resilience-and-a11y-stopgaps.md`

## File Structure

**Test Harness**
- Modify: `frontend/package.json`
- Modify: `frontend/package-lock.json`
- Modify: `frontend/vite.config.ts`
- Create: `frontend/src/test/setupTests.ts`

**Error And Route Fallbacks**
- Modify: `frontend/src/App.tsx`
- Modify: `frontend/src/auth/guards.tsx`
- Create: `frontend/src/components/errors/AppErrorBoundary.tsx`
- Create: `frontend/src/components/errors/ForbiddenPage.tsx`
- Create: `frontend/src/components/errors/NotFoundPage.tsx`
- Test: `frontend/src/components/errors/AppErrorBoundary.test.tsx`
- Test: `frontend/src/components/errors/routeFallbacks.test.tsx`

**API Resilience**
- Modify: `frontend/src/services/api.ts`
- Create: `frontend/src/services/apiError.ts`
- Test: `frontend/src/services/apiResilience.test.ts`

**Accessibility Shell**
- Modify: `frontend/src/components/layout/AppLayout.tsx`
- Create: `frontend/src/components/accessibility/LiveRegion.tsx`
- Create: `frontend/src/components/accessibility/RouteA11y.tsx`
- Test: `frontend/src/components/layout/AppLayout.a11y.test.tsx`

**Unsaved Editor Guard**
- Modify: `frontend/src/pages/canvas-editor/index.tsx`
- Create: `frontend/src/pages/canvas-editor/unsavedChangeGuard.ts`
- Test: `frontend/src/pages/canvas-editor/unsavedChangeGuard.test.ts`

### Task 1: Add jsdom Component Test Harness

**Files:**
- Modify: `frontend/package.json`
- Modify: `frontend/package-lock.json`
- Modify: `frontend/vite.config.ts`
- Create: `frontend/src/test/setupTests.ts`

- [ ] **Step 1: Install component-test dependencies**

Run: `cd frontend && npm install -D jsdom @testing-library/react @testing-library/jest-dom @testing-library/user-event`

Expected: `package.json` and `package-lock.json` include the four dev dependencies.

- [ ] **Step 2: Add Testing Library setup file**

Create `frontend/src/test/setupTests.ts`.

```ts
import '@testing-library/jest-dom/vitest'
```

- [ ] **Step 3: Wire setup without changing the default environment**

Modify `frontend/vite.config.ts`.

```ts
test: {
  environment: 'node',
  setupFiles: ['./src/test/setupTests.ts'],
},
```

Component tests must start with `/* @vitest-environment jsdom */`; pure helper tests keep the existing `node` behavior.

- [ ] **Step 4: Run an existing pure test**

Run: `cd frontend && npm test -- homeOverview.test.ts`

Expected: PASS; the setup file does not require jsdom for existing pure tests.

### Task 2: Add Recoverable Error Boundary And Route Fallbacks

**Files:**
- Create: `frontend/src/components/errors/AppErrorBoundary.test.tsx`
- Create: `frontend/src/components/errors/routeFallbacks.test.tsx`
- Create: `frontend/src/components/errors/AppErrorBoundary.tsx`
- Create: `frontend/src/components/errors/ForbiddenPage.tsx`
- Create: `frontend/src/components/errors/NotFoundPage.tsx`
- Modify: `frontend/src/auth/guards.tsx`
- Modify: `frontend/src/App.tsx`

- [ ] **Step 1: Write ErrorBoundary component tests**

Create `AppErrorBoundary.test.tsx`.

```tsx
/* @vitest-environment jsdom */
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'
import AppErrorBoundary from './AppErrorBoundary'

function Boom() {
  throw new Error('route failed')
}

describe('AppErrorBoundary', () => {
  it('renders a recoverable alert when a child throws', async () => {
    render(<AppErrorBoundary><Boom /></AppErrorBoundary>)

    expect(screen.getByRole('alert')).toHaveTextContent('页面加载失败')
    expect(screen.getByRole('button', { name: '重试' })).toBeInTheDocument()
  })

  it('resets the failure state when retry is clicked', async () => {
    let shouldThrow = true
    function Flaky() {
      if (shouldThrow) throw new Error('first render failed')
      return <div>已恢复</div>
    }

    render(<AppErrorBoundary><Flaky /></AppErrorBoundary>)
    shouldThrow = false
    await userEvent.click(screen.getByRole('button', { name: '重试' }))

    expect(screen.getByText('已恢复')).toBeInTheDocument()
  })
})
```

- [ ] **Step 2: Write route fallback tests**

Create `routeFallbacks.test.tsx`.

```tsx
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
```

- [ ] **Step 3: Run fallback tests and confirm red state**

Run: `cd frontend && npm test -- AppErrorBoundary.test.tsx routeFallbacks.test.tsx`

Expected: FAIL because the three components do not exist yet.

- [ ] **Step 4: Implement fallback components**

Create `AppErrorBoundary.tsx`.

```tsx
import { Component, type ErrorInfo, type ReactNode } from 'react'
import { Button, Result } from 'antd'
import { Link } from 'react-router-dom'

interface State { failed: boolean }

export default class AppErrorBoundary extends Component<{ children: ReactNode }, State> {
  state: State = { failed: false }

  static getDerivedStateFromError() {
    return { failed: true }
  }

  componentDidCatch(error: Error, info: ErrorInfo) {
    console.error('[APP_ERROR_BOUNDARY]', error, info.componentStack)
  }

  render() {
    if (!this.state.failed) return this.props.children
    return (
      <Result
        role="alert"
        status="error"
        title="页面加载失败"
        subTitle="当前页面渲染异常，重试后仍失败请返回首页。"
        extra={[
          <Button key="retry" type="primary" onClick={() => this.setState({ failed: false })}>重试</Button>,
          <Link key="home" to="/home">返回首页</Link>,
        ]}
      />
    )
  }
}
```

Create `ForbiddenPage.tsx` and `NotFoundPage.tsx` using Ant Design `Result`. Use `Link` for navigation so tests can assert `href`.

```tsx
export default function ForbiddenPage() {
  return <Result status="403" title="无权限访问" subTitle="当前账号没有访问该页面的权限。" extra={<Link to="/home">返回首页</Link>} />
}
```

```tsx
export default function NotFoundPage() {
  return <Result status="404" title="页面不存在" subTitle="当前地址没有匹配的页面。" extra={<Link to="/canvas">返回旅程管理</Link>} />
}
```

- [ ] **Step 5: Wire route guards and catch-all route**

In `guards.tsx`, replace inline forbidden `<div>` returns with `<ForbiddenPage />`.

```tsx
if (!isAdmin) return <ForbiddenPage />
```

In `App.tsx`, import `AppErrorBoundary` and `NotFoundPage`. Wrap the routed app content inside the boundary and add an authenticated catch-all route.

```tsx
<AppErrorBoundary>
  <Suspense fallback={<RouteFallback />}>
    <Routes>
      ...
      <Route element={<RequireAuth />}>
        <Route element={<AppLayout />}>
          ...
          <Route path="*" element={<NotFoundPage />} />
        </Route>
      </Route>
    </Routes>
  </Suspense>
</AppErrorBoundary>
```

- [ ] **Step 6: Run fallback tests and confirm green state**

Run: `cd frontend && npm test -- AppErrorBoundary.test.tsx routeFallbacks.test.tsx`

Expected: PASS.

### Task 3: Classify API Failures And Add Timeout

**Files:**
- Create: `frontend/src/services/apiResilience.test.ts`
- Create: `frontend/src/services/apiError.ts`
- Modify: `frontend/src/services/api.ts`

- [ ] **Step 1: Write error-classification tests**

Create `apiResilience.test.ts`.

```ts
import { describe, expect, it } from 'vitest'
import { classifyApiError } from './apiError'

describe('classifyApiError', () => {
  it('classifies timeout errors as retryable', () => {
    expect(classifyApiError({ code: 'ECONNABORTED', message: 'timeout' })).toMatchObject({
      kind: 'timeout',
      retryable: true,
    })
  })

  it('classifies forbidden responses', () => {
    expect(classifyApiError({ response: { status: 403, data: { message: 'AUTH_003' } } })).toMatchObject({
      kind: 'forbidden',
      status: 403,
      retryable: false,
    })
  })

  it('classifies offline network failures', () => {
    Object.defineProperty(globalThis.navigator, 'onLine', { value: false, configurable: true })
    expect(classifyApiError({ request: {}, message: 'Network Error' })).toMatchObject({
      kind: 'offline',
      retryable: true,
    })
  })
})
```

- [ ] **Step 2: Run API tests and confirm red state**

Run: `cd frontend && npm test -- apiResilience.test.ts`

Expected: FAIL because `apiError.ts` does not exist.

- [ ] **Step 3: Implement `apiError.ts`**

Create `frontend/src/services/apiError.ts`.

```ts
export type ApiErrorKind = 'timeout' | 'canceled' | 'offline' | 'unauthorized' | 'forbidden' | 'server' | 'unknown'

export interface ClassifiedApiError {
  kind: ApiErrorKind
  status?: number
  message: string
  retryable: boolean
  original: unknown
}

export function classifyApiError(error: any): ClassifiedApiError {
  const status = error?.response?.status
  const message = error?.response?.data?.message || error?.message || '请求失败'
  if (error?.code === 'ECONNABORTED') return { kind: 'timeout', message, retryable: true, original: error }
  if (error?.code === 'ERR_CANCELED') return { kind: 'canceled', message, retryable: false, original: error }
  if (typeof navigator !== 'undefined' && navigator.onLine === false) return { kind: 'offline', message: '网络已断开', retryable: true, original: error }
  if (status === 401) return { kind: 'unauthorized', status, message, retryable: false, original: error }
  if (status === 403) return { kind: 'forbidden', status, message, retryable: false, original: error }
  if (status >= 500) return { kind: 'server', status, message, retryable: true, original: error }
  return { kind: 'unknown', status, message, retryable: false, original: error }
}
```

- [ ] **Step 4: Update shared Axios client**

Modify the `http` client in `api.ts`.

```ts
import { classifyApiError } from './apiError'

const http = axios.create({ baseURL: '/', timeout: 15000 })
```

Keep 401 cleanup before rejecting the classified error.

```ts
const classified = classifyApiError(err)
if (classified.kind === 'unauthorized') {
  localStorage.removeItem('canvas_token')
  localStorage.removeItem('canvas_user')
  window.location.href = '/login'
}
return Promise.reject(classified)
```

- [ ] **Step 5: Run API tests and build**

Run: `cd frontend && npm test -- apiResilience.test.ts && npm run build`

Expected: PASS; TypeScript accepts the classified error export and Axios timeout option.

### Task 4: Add Accessibility Shell Primitives

**Files:**
- Create: `frontend/src/components/layout/AppLayout.a11y.test.tsx`
- Create: `frontend/src/components/accessibility/LiveRegion.tsx`
- Create: `frontend/src/components/accessibility/RouteA11y.tsx`
- Modify: `frontend/src/components/layout/AppLayout.tsx`
- Modify: `frontend/src/App.tsx`

- [ ] **Step 1: Write accessibility shell tests**

Create `AppLayout.a11y.test.tsx`.

```tsx
/* @vitest-environment jsdom */
import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { describe, expect, it } from 'vitest'
import LiveRegion from '../accessibility/LiveRegion'
import { buildRouteAnnouncement } from '../accessibility/RouteA11y'

describe('app shell accessibility', () => {
  it('renders live-region text for screen readers', () => {
    render(<LiveRegion message="已进入旅程管理" />)

    expect(screen.getByRole('status')).toHaveTextContent('已进入旅程管理')
  })

  it('builds route announcements from known paths', () => {
    expect(buildRouteAnnouncement('/canvas')).toBe('已进入旅程管理')
    expect(buildRouteAnnouncement('/admin/users')).toBe('已进入用户管理')
  })
})
```

- [ ] **Step 2: Run accessibility tests and confirm red state**

Run: `cd frontend && npm test -- AppLayout.a11y.test.tsx`

Expected: FAIL because `LiveRegion` and `RouteA11y` do not exist.

- [ ] **Step 3: Implement `LiveRegion`**

Create `LiveRegion.tsx`.

```tsx
const visuallyHidden: React.CSSProperties = {
  position: 'absolute',
  width: 1,
  height: 1,
  padding: 0,
  margin: -1,
  overflow: 'hidden',
  clip: 'rect(0, 0, 0, 0)',
  whiteSpace: 'nowrap',
  border: 0,
}

export default function LiveRegion({ message }: { message: string }) {
  return <div role="status" aria-live="polite" aria-atomic="true" style={visuallyHidden}>{message}</div>
}
```

- [ ] **Step 4: Implement route announcements and focus**

Create `RouteA11y.tsx`.

```tsx
import { useEffect, useState } from 'react'
import { useLocation } from 'react-router-dom'
import LiveRegion from './LiveRegion'

export function buildRouteAnnouncement(pathname: string) {
  if (pathname.startsWith('/admin/users')) return '已进入用户管理'
  if (pathname.startsWith('/admin/tenants')) return '已进入租户管理'
  if (pathname.startsWith('/canvas')) return '已进入旅程管理'
  if (pathname.startsWith('/cdp/users')) return '已进入用户中心'
  return '页面已更新'
}

export default function RouteA11y() {
  const location = useLocation()
  const [message, setMessage] = useState('')

  useEffect(() => {
    setMessage(buildRouteAnnouncement(location.pathname))
    document.getElementById('main-content')?.focus()
  }, [location.pathname])

  return <LiveRegion message={message} />
}
```

- [ ] **Step 5: Wire app shell landmarks**

In `App.tsx`, render `<RouteA11y />` inside `BrowserRouter` before `Routes`.

In `AppLayout.tsx`, add a skip link before the `Sider`, add `role="navigation"` and `aria-label="主导航"` to the menu container, and add a focusable `main` landmark to `Content`.

```tsx
<a href="#main-content" className="skip-link">跳到主要内容</a>
```

```tsx
<Content id="main-content" role="main" tabIndex={-1}>
  <Outlet />
</Content>
```

- [ ] **Step 6: Run accessibility tests and build**

Run: `cd frontend && npm test -- AppLayout.a11y.test.tsx && npm run build`

Expected: PASS.

### Task 5: Add Dirty Editor Unload Guard

**Files:**
- Create: `frontend/src/pages/canvas-editor/unsavedChangeGuard.test.ts`
- Create: `frontend/src/pages/canvas-editor/unsavedChangeGuard.ts`
- Modify: `frontend/src/pages/canvas-editor/index.tsx`

- [ ] **Step 1: Write unload guard tests**

Create `unsavedChangeGuard.test.ts`.

```ts
import { describe, expect, it, vi } from 'vitest'
import { shouldWarnBeforeUnload, bindBeforeUnloadGuard } from './unsavedChangeGuard'

describe('unsavedChangeGuard', () => {
  it('warns only for dirty writable drafts', () => {
    expect(shouldWarnBeforeUnload({ isDirty: true, readonly: false })).toBe(true)
    expect(shouldWarnBeforeUnload({ isDirty: false, readonly: false })).toBe(false)
    expect(shouldWarnBeforeUnload({ isDirty: true, readonly: true })).toBe(false)
  })

  it('registers and removes beforeunload listener', () => {
    const add = vi.spyOn(window, 'addEventListener')
    const remove = vi.spyOn(window, 'removeEventListener')

    const cleanup = bindBeforeUnloadGuard(() => true)
    cleanup()

    expect(add).toHaveBeenCalledWith('beforeunload', expect.any(Function))
    expect(remove).toHaveBeenCalledWith('beforeunload', expect.any(Function))
  })
})
```

- [ ] **Step 2: Run unload tests and confirm red state**

Run: `cd frontend && npm test -- unsavedChangeGuard.test.ts`

Expected: FAIL because `unsavedChangeGuard.ts` does not exist.

- [ ] **Step 3: Implement unload guard helpers**

Create `unsavedChangeGuard.ts`.

```ts
interface GuardState {
  isDirty: boolean
  readonly: boolean
}

export function shouldWarnBeforeUnload(state: GuardState) {
  return state.isDirty && !state.readonly
}

export function bindBeforeUnloadGuard(shouldWarn: () => boolean) {
  const handler = (event: BeforeUnloadEvent) => {
    if (!shouldWarn()) return
    event.preventDefault()
    event.returnValue = ''
  }
  window.addEventListener('beforeunload', handler)
  return () => window.removeEventListener('beforeunload', handler)
}
```

- [ ] **Step 4: Wire canvas editor dirty state**

In `frontend/src/pages/canvas-editor/index.tsx`, import the helpers and add an effect near the existing local-draft dirty-state effect.

```tsx
useEffect(() => {
  return bindBeforeUnloadGuard(() => shouldWarnBeforeUnload({ isDirty, readonly }))
}, [isDirty, readonly])
```

- [ ] **Step 5: Run unload tests and editor build**

Run: `cd frontend && npm test -- unsavedChangeGuard.test.ts && npm run build`

Expected: PASS; TypeScript accepts `BeforeUnloadEvent` and the editor registers the guard only while mounted.

### Task 6: Frontend Regression And Rollout Notes

**Files:**
- Modify: `docs/product-evolution/specs/p0-002-frontend-resilience-and-a11y.md`
- Modify: `docs/product-evolution/plans/p0-002-frontend-resilience-and-a11y-plan.md`

- [ ] **Step 1: Run focused frontend tests**

Run: `cd frontend && npm test -- AppErrorBoundary.test.tsx routeFallbacks.test.tsx apiResilience.test.ts AppLayout.a11y.test.tsx unsavedChangeGuard.test.ts`

Expected: PASS for all P0-002 tests.

- [ ] **Step 2: Run existing frontend suite**

Run: `cd frontend && npm test`

Expected: PASS for existing pure tests and new jsdom component tests.

- [ ] **Step 3: Run frontend production build**

Run: `cd frontend && npm run build`

Expected: PASS with TypeScript and Vite build success.

- [ ] **Step 4: Add rollout notes to the implementation PR**

Use this exact checklist in the PR body.

```markdown
Rollout notes:
- ErrorBoundary wraps lazy routes and can be verified by forcing a temporary throw in a local route component.
- 403 and 404 pages use normal router links and do not require backend changes.
- Axios timeout is 15 seconds; 401 redirect behavior is unchanged.
- Component tests opt into jsdom per file; default Vitest environment remains node.
- Dirty editor unload warning is active only when the canvas editor has unsaved writable changes.
```

- [ ] **Step 5: Commit the implementation slice**

Run: `git add frontend/package.json frontend/package-lock.json frontend/vite.config.ts frontend/src docs/product-evolution/specs/p0-002-frontend-resilience-and-a11y.md docs/product-evolution/plans/p0-002-frontend-resilience-and-a11y-plan.md && git commit -m "feat: add frontend resilience shell"`

Expected: commit contains the P0-002 frontend resilience and accessibility slice.
