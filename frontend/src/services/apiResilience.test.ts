import { describe, expect, it } from 'vitest'
import { http } from './api'
import { classifyApiError, userFacingApiErrorMessage } from './apiError'

describe('api error classification', () => {
  it('configures the shared client with a bounded timeout', () => {
    expect(http.defaults.timeout).toBe(15000)
  })

  it('classifies authorization and conflict failures as non-retryable', () => {
    expect(classifyApiError({ response: { status: 401, data: { errorCode: 'AUTH_002', message: 'expired' } } })).toMatchObject({
      kind: 'unauthorized',
      errorCode: 'AUTH_002',
      message: 'expired',
      retryable: false,
    })
    expect(classifyApiError({ response: { status: 403, data: { message: 'forbidden' } } })).toMatchObject({
      kind: 'forbidden',
      status: 403,
      retryable: false,
    })
    expect(classifyApiError({ response: { status: 409, data: { message: 'conflict' } } })).toMatchObject({
      kind: 'conflict',
      retryable: false,
    })
  })

  it('classifies transport failures as retryable with user-facing copy', () => {
    const error = { code: 'ECONNRESET', message: 'socket closed' }

    expect(classifyApiError(error)).toMatchObject({ kind: 'network', retryable: true })
    expect(userFacingApiErrorMessage(error)).toBe('网络连接异常，请检查网络后重试')
    expect(classifyApiError({ code: 'ECONNABORTED', message: 'timeout' })).toMatchObject({
      kind: 'timeout',
      retryable: true,
    })
    expect(classifyApiError({ code: 'ERR_CANCELED', message: 'canceled' })).toMatchObject({
      kind: 'canceled',
      retryable: false,
    })
  })

  it('classifies offline, server, and unknown failures distinctly', () => {
    Object.defineProperty(globalThis, 'navigator', {
      value: { onLine: false },
      configurable: true,
    })
    expect(classifyApiError({ request: {}, message: 'Network Error' })).toMatchObject({
      kind: 'offline',
      retryable: true,
    })
    Object.defineProperty(globalThis, 'navigator', {
      value: { onLine: true },
      configurable: true,
    })
    expect(classifyApiError({ response: { status: 503, data: { message: 'down' } } })).toMatchObject({
      kind: 'server',
      status: 503,
      retryable: true,
    })
    expect(classifyApiError(new Error('plain'))).toMatchObject({
      kind: 'unknown',
      retryable: false,
    })
  })

  it('classifies wrapped business errors', () => {
    expect(classifyApiError({ code: 40001, message: 'bad request' })).toMatchObject({
      kind: 'business',
      code: 40001,
      retryable: false,
    })
  })
})
