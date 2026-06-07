import axios, { type AxiosInstance, type AxiosRequestConfig } from 'axios'

export class ApiHttpError extends Error {
  constructor(
    message: string,
    public readonly status?: number,
    public readonly data?: unknown,
    public readonly code?: number,
    public readonly errorCode?: string,
    public readonly traceId?: string,
    public readonly retryable = false,
    public readonly canceled = false,
    public readonly original?: unknown,
  ) {
    super(message)
    this.name = 'ApiHttpError'
  }
}

export interface RequestOptions extends AxiosRequestConfig {
  requestKey?: string
  intendedPath?: string
}

export interface HttpClientOptions {
  maxGetRetries?: number
  onUnauthorized?: (intendedPath?: string) => void
}

type HttpMethod = 'get' | 'post' | 'put' | 'delete'

function isRecord(value: unknown): value is Record<string, unknown> {
  return Boolean(value) && typeof value === 'object'
}

function statusFrom(error: unknown): number | undefined {
  if (!isRecord(error)) return undefined
  if (typeof error.status === 'number') return error.status
  const response = isRecord(error.response) ? error.response : undefined
  return typeof response?.status === 'number' ? response.status : undefined
}

function dataFrom(error: unknown): unknown {
  if (!isRecord(error)) return undefined
  if ('data' in error) return error.data
  const response = isRecord(error.response) ? error.response : undefined
  return response?.data
}

function messageFrom(value: unknown): string | undefined {
  if (!isRecord(value)) return undefined
  return typeof value.message === 'string' && value.message.trim().length > 0
    ? value.message
    : undefined
}

function codeFrom(value: unknown): number | undefined {
  if (!isRecord(value)) return undefined
  return typeof value.code === 'number' ? value.code : undefined
}

function errorCodeFrom(value: unknown): string | undefined {
  if (!isRecord(value)) return undefined
  return typeof value.errorCode === 'string' ? value.errorCode : undefined
}

function traceIdFrom(value: unknown): string | undefined {
  if (!isRecord(value)) return undefined
  return typeof value.traceId === 'string' ? value.traceId : undefined
}

function isCanceled(error: unknown): boolean {
  if (axios.isCancel(error)) return true
  return isRecord(error) && error.code === 'ERR_CANCELED'
}

function isRetryableNetworkError(error: unknown): boolean {
  if (isCanceled(error)) return false
  if (!isRecord(error)) return false
  const status = statusFrom(error)
  if (status === 408 || status === 429) return true
  if (typeof status === 'number') return status >= 500
  return Boolean(error.request || error.code)
}

function normalizeError(error: unknown): ApiHttpError {
  if (error instanceof ApiHttpError) return error
  const status = statusFrom(error)
  const data = dataFrom(error)
  const code = codeFrom(data) ?? codeFrom(error)
  const errorCode = errorCodeFrom(data) ?? errorCodeFrom(error)
  const traceId = traceIdFrom(data) ?? traceIdFrom(error)
  const message = messageFrom(data)
    ?? messageFrom(error)
    ?? (error instanceof Error ? error.message : undefined)
    ?? 'Request failed'
  return new ApiHttpError(
    message,
    status,
    data,
    code,
    errorCode,
    traceId,
    isRetryableNetworkError(error),
    isCanceled(error),
    error,
  )
}

function unwrapPayload<T>(responseOrPayload: unknown): T {
  const payload = isRecord(responseOrPayload) && 'status' in responseOrPayload && 'data' in responseOrPayload
    ? responseOrPayload.data
    : responseOrPayload

  if (isRecord(payload) && 'code' in payload) {
    if (payload.code !== 0) {
      throw new ApiHttpError(
        messageFrom(payload) ?? 'Business error',
        undefined,
        payload.data,
        Number(payload.code),
        errorCodeFrom(payload),
        traceIdFrom(payload),
        false,
        false,
        payload,
      )
    }
    return payload.data as T
  }

  return payload as T
}

export function createHttpClient(instance: AxiosInstance, options: HttpClientOptions = {}) {
  const inFlight = new Map<string, Promise<unknown>>()

  async function request<T>(
    method: HttpMethod,
    url: string,
    data?: unknown,
    config: RequestOptions = {},
  ): Promise<T> {
    const { requestKey, intendedPath, ...axiosConfig } = config
    const key = method === 'get' ? requestKey : undefined
    if (key && inFlight.has(key)) return inFlight.get(key) as Promise<T>

    const run = async () => {
      const attempts = method === 'get' ? (options.maxGetRetries ?? 0) + 1 : 1
      for (let attempt = 1; attempt <= attempts; attempt += 1) {
        try {
          const response = await instance.request({
            ...axiosConfig,
            method,
            url,
            data,
          })
          return unwrapPayload<T>(response)
        } catch (error) {
          const normalized = normalizeError(error)
          if (normalized.status === 401) {
            options.onUnauthorized?.(intendedPath)
          }
          if (attempt < attempts && normalized.retryable) continue
          throw normalized
        }
      }
      throw new ApiHttpError('Request failed')
    }

    const promise = run()
    if (key) {
      inFlight.set(key, promise)
      promise.finally(() => inFlight.delete(key))
    }
    return promise
  }

  return {
    get: <T>(url: string, config?: RequestOptions) => request<T>('get', url, undefined, config),
    post: <T>(url: string, data?: unknown, config?: RequestOptions) => request<T>('post', url, data, config),
    put: <T>(url: string, data?: unknown, config?: RequestOptions) => request<T>('put', url, data, config),
    delete: <T>(url: string, config?: RequestOptions) => request<T>('delete', url, undefined, config),
  }
}
