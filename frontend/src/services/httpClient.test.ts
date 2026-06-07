import axios, { CanceledError } from 'axios'
import { describe, expect, it, vi } from 'vitest'
import { ApiHttpError, createHttpClient } from './httpClient'

function ok(data: unknown) {
  return {
    status: 200,
    statusText: 'OK',
    headers: {},
    config: {},
    data: { code: 0, data },
  }
}

describe('httpClient', () => {
  it('dedupes identical in-flight GET requests by requestKey', async () => {
    const adapter = vi.fn().mockResolvedValue(ok({ ok: true }))
    const client = createHttpClient(axios.create({ adapter }))

    const first = client.get('/api/canvas/1', { requestKey: 'canvas:1' })
    const second = client.get('/api/canvas/1', { requestKey: 'canvas:1' })

    await expect(Promise.all([first, second])).resolves.toEqual([{ ok: true }, { ok: true }])
    expect(adapter).toHaveBeenCalledTimes(1)
  })

  it('retries idempotent GET and does not retry POST by default', async () => {
    const adapter = vi.fn()
      .mockRejectedValueOnce({ code: 'ECONNRESET' })
      .mockResolvedValueOnce(ok({ ok: true }))
    const client = createHttpClient(axios.create({ adapter }), { maxGetRetries: 1 })

    await expect(client.get('/api/retry')).resolves.toEqual({ ok: true })
    expect(adapter).toHaveBeenCalledTimes(2)

    adapter.mockReset()
    adapter.mockRejectedValueOnce({ code: 'ECONNRESET' })

    await expect(client.post('/api/no-retry', {})).rejects.toMatchObject({
      name: 'ApiHttpError',
      retryable: true,
    })
    expect(adapter).toHaveBeenCalledTimes(1)
  })

  it('normalizes business errors and 401 unauthorized callbacks', async () => {
    const unauthorized = vi.fn()
    const adapter = vi.fn()
      .mockResolvedValueOnce({
        status: 200,
        statusText: 'OK',
        headers: {},
        config: {},
        data: { code: 40001, message: 'bad request', data: { field: 'name' }, traceId: 'trace-1' },
      })
      .mockRejectedValueOnce({ response: { status: 401, data: { message: 'expired' } } })
    const client = createHttpClient(axios.create({ adapter }), { onUnauthorized: unauthorized })

    await expect(client.get('/api/business')).rejects.toMatchObject({
      code: 40001,
      message: 'bad request',
      data: { field: 'name' },
      traceId: 'trace-1',
    })
    await expect(client.get('/api/protected', { intendedPath: '/canvas/1' })).rejects.toMatchObject({ status: 401 })
    expect(unauthorized).toHaveBeenCalledWith('/canvas/1')
  })

  it('passes AbortSignal through and normalizes cancellation', async () => {
    const adapter = vi.fn(async config => {
      expect(config.signal).toBeInstanceOf(AbortSignal)
      throw new CanceledError('aborted', undefined, config)
    })
    const client = createHttpClient(axios.create({ adapter }))
    const controller = new AbortController()
    controller.abort()

    await expect(client.get('/api/cancel', { signal: controller.signal })).rejects.toMatchObject({
      name: 'ApiHttpError',
      canceled: true,
      retryable: false,
    })
  })

  it('normalizes network errors', async () => {
    const adapter = vi.fn().mockRejectedValue({ request: {}, message: 'network down' })
    const client = createHttpClient(axios.create({ adapter }))

    await expect(client.get('/api/network')).rejects.toBeInstanceOf(ApiHttpError)
    await expect(client.get('/api/network')).rejects.toMatchObject({
      message: 'network down',
      retryable: true,
    })
  })
})
