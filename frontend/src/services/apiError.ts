import axios from 'axios'

export type ApiErrorKind =
  | 'business'
  | 'unauthorized'
  | 'forbidden'
  | 'network'
  | 'offline'
  | 'timeout'
  | 'canceled'
  | 'server'
  | 'conflict'
  | 'unknown'

export interface ClassifiedApiError {
  kind: ApiErrorKind
  message: string
  status?: number
  code?: number
  errorCode?: string
  retryable: boolean
  data?: unknown
  original: unknown
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return Boolean(value) && typeof value === 'object'
}

function messageFrom(value: unknown): string | undefined {
  if (!isRecord(value)) return undefined
  const record = value
  return typeof record.message === 'string' && record.message.trim().length > 0
    ? record.message
    : undefined
}

function codeFrom(value: unknown): number | undefined {
  if (!isRecord(value)) return undefined
  const code = value.code
  return typeof code === 'number' ? code : undefined
}

function errorCodeFrom(value: unknown): string | undefined {
  if (!isRecord(value)) return undefined
  return typeof value.errorCode === 'string' ? value.errorCode : undefined
}

function isOffline() {
  return typeof navigator !== 'undefined' && navigator.onLine === false
}

export function classifyApiError(error: unknown): ClassifiedApiError {
  const errorRecord: Record<string, unknown> = isRecord(error) ? error : {}
  const response = axios.isAxiosError(error)
    ? error.response
    : errorRecord.response as { status?: number; data?: unknown } | undefined
  const status = response?.status
  const data = response?.data
  const code = codeFrom(data) ?? codeFrom(error)
  const errorCode = errorCodeFrom(data) ?? errorCodeFrom(error)
  const message = messageFrom(data) ?? (error instanceof Error ? error.message : undefined) ?? '请求失败，请稍后重试'

  const transportCode = errorRecord.code
  if (transportCode === 'ERR_CANCELED' || axios.isCancel(error)) {
    return { kind: 'canceled', status, message, retryable: false, data, original: error }
  }
  if (transportCode === 'ECONNABORTED' || transportCode === 'ETIMEDOUT') {
    return { kind: 'timeout', status, message, retryable: true, data, original: error }
  }
  if (!status && isOffline()) {
    return { kind: 'offline', status, message: '网络已断开', retryable: true, data, original: error }
  }

  if (status === 401) {
    return { kind: 'unauthorized', status, code, errorCode, message, retryable: false, data, original: error }
  }
  if (status === 403) {
    return { kind: 'forbidden', status, code, errorCode, message, retryable: false, data, original: error }
  }
  if (status === 409) return { kind: 'conflict', status, code, errorCode, message, retryable: false, data, original: error }
  if (status && status >= 500) return { kind: 'server', status, code, errorCode, message, retryable: true, data, original: error }
  if (typeof code === 'number') return { kind: 'business', status, code, errorCode, message, retryable: false, data, original: error }

  if (!status && (axios.isAxiosError(error) || transportCode || errorRecord.request)) {
    return { kind: 'network', status, message, retryable: true, data, original: error }
  }
  return { kind: 'unknown', status, message, retryable: false, data, original: error }
}

export function userFacingApiErrorMessage(error: unknown): string {
  const classified = classifyApiError(error)
  if (classified.kind === 'offline') return '网络已断开，请恢复连接后重试'
  if (classified.kind === 'network') return '网络连接异常，请检查网络后重试'
  if (classified.kind === 'timeout') return '请求超时，请稍后重试'
  if (classified.kind === 'canceled') return '请求已取消'
  if (classified.kind === 'unauthorized') return '登录已过期，请重新登录'
  if (classified.kind === 'forbidden') return '当前账号无权限执行此操作'
  if (classified.kind === 'server') return '服务暂时不可用，请稍后重试'
  if (classified.kind === 'conflict') return '内容已被更新，请刷新后重试'
  return classified.message
}
