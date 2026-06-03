/**
 * 测试职责：验证统一 API 客户端会解包 R<T> 并拒绝业务错误码。
 *
 * 维护说明：后端 HTTP 200 但 code 非 0 时，调用方必须进入异常路径。
 */
import { beforeEach, describe, expect, it } from 'vitest'
import { ApiBusinessError, http } from './api'

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

describe('api http interceptor', () => {
  beforeEach(() => {
    Object.defineProperty(globalThis, 'localStorage', {
      value: new MemoryStorage(),
      configurable: true,
    })
  })

  it('unwraps successful business responses', async () => {
    await expect(http.get('/ok', {
      adapter: async config => ({
        data: { code: 0, message: 'ok', data: { id: 1 } },
        status: 200,
        statusText: 'OK',
        headers: {},
        config,
      }),
    })).resolves.toEqual({ code: 0, message: 'ok', data: { id: 1 } })
  })

  it('rejects non-zero business code responses', async () => {
    const promise = http.get('/business-fail', {
      adapter: async config => ({
        data: { code: 40001, message: 'bad request', data: { field: 'name' } },
        status: 200,
        statusText: 'OK',
        headers: {},
        config,
      }),
    })

    await expect(promise).rejects.toBeInstanceOf(ApiBusinessError)
    await expect(promise).rejects.toMatchObject({
      code: 40001,
      message: 'bad request',
      data: { field: 'name' },
    })
  })
})
