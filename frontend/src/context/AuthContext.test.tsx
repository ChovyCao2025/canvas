/**
 * 测试职责：验证认证上下文不会在初始化时泄露本地 token。
 *
 * 维护说明：认证初始化可以读取 localStorage，但不能把 token 或其片段写入日志。
 */
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { renderToString } from 'react-dom/server'
import { AuthProvider } from './AuthContext'

class MemoryStorage implements Storage {
  private readonly data = new Map<string, string>()

  get length() {
    return this.data.size
  }

  clear() {
    this.data.clear()
  }

  getItem(key: string) {
    return this.data.get(key) ?? null
  }

  key(index: number) {
    return Array.from(this.data.keys())[index] ?? null
  }

  removeItem(key: string) {
    this.data.delete(key)
  }

  setItem(key: string, value: string) {
    this.data.set(key, value)
  }
}

describe('AuthProvider', () => {
  beforeEach(() => {
    Object.defineProperty(globalThis, 'localStorage', {
      value: new MemoryStorage(),
      configurable: true,
    })
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('does not log token on auth init', () => {
    const spy = vi.spyOn(console, 'log').mockImplementation(() => undefined)
    localStorage.setItem('canvas_token', 'secret-token')
    localStorage.setItem('canvas_user', JSON.stringify({
      token: 'secret-token',
      userId: 1,
      tenantId: null,
      username: 'admin',
      displayName: 'Admin',
      role: 'ADMIN',
    }))

    renderToString(<AuthProvider><span /></AuthProvider>)

    const loggedText = spy.mock.calls.flat().map(value => String(value)).join(' ')
    expect(loggedText).not.toContain('secret-token')
  })
})
