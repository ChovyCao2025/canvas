export type SaveQueueStatus = 'idle' | 'saving' | 'retrying' | 'saved' | 'failed' | 'conflict'

export interface SaveQueueState<TPayload = unknown> {
  status: SaveQueueStatus
  attempts: number
  pending: boolean
  inFlight: boolean
  payload?: TPayload
  serverVersion?: number
  error?: unknown
}

export type SaveQueueResult<TPayload, TResponse = unknown> =
  | { status: 'saved'; attempts: number; payload: TPayload; response: TResponse }
  | { status: 'failed'; attempts: number; payload: TPayload; error: unknown }
  | { status: 'conflict'; attempts: number; payload: TPayload; serverVersion?: number; error: unknown }

export interface SaveQueueOptions<TPayload, TResponse = unknown> {
  save: (payload: TPayload, signal: AbortSignal) => Promise<TResponse>
  maxAttempts?: number
  baseDelayMs?: number
  maxDelayMs?: number
  sleep?: (delayMs: number, signal: AbortSignal) => Promise<void>
  onStateChange?: (state: SaveQueueState<TPayload>) => void
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return Boolean(value) && typeof value === 'object'
}

function statusFrom(error: unknown): number | undefined {
  if (!isRecord(error)) return undefined
  if (typeof error.status === 'number') return error.status
  const response = isRecord(error.response) ? error.response : undefined
  return typeof response?.status === 'number' ? response.status : undefined
}

function serverVersionFrom(error: unknown): number | undefined {
  if (!isRecord(error)) return undefined
  if (typeof error.serverVersion === 'number') return error.serverVersion
  const data = isRecord(error.data) ? error.data : undefined
  if (typeof data?.serverVersion === 'number') return data.serverVersion
  const response = isRecord(error.response) ? error.response : undefined
  const responseData = isRecord(response?.data) ? response.data : undefined
  if (typeof responseData?.serverVersion === 'number') return responseData.serverVersion
  const wrappedData = isRecord(responseData?.data) ? responseData.data : undefined
  return typeof wrappedData?.serverVersion === 'number' ? wrappedData.serverVersion : undefined
}

function isRetryable(error: unknown): boolean {
  if (!isRecord(error)) return false
  if (typeof error.retryable === 'boolean') return error.retryable
  const status = statusFrom(error)
  if (status === 409) return false
  return status === 408 || status === 429 || (typeof status === 'number' && status >= 500)
}

function defaultSleep(delayMs: number, signal: AbortSignal): Promise<void> {
  if (delayMs <= 0) return Promise.resolve()
  return new Promise((resolve, reject) => {
    if (signal.aborted) {
      reject(new Error('Save queue aborted'))
      return
    }
    const timer = setTimeout(resolve, delayMs)
    signal.addEventListener('abort', () => {
      clearTimeout(timer)
      reject(new Error('Save queue aborted'))
    }, { once: true })
  })
}

export function createSaveQueue<TPayload, TResponse = unknown>(
  options: SaveQueueOptions<TPayload, TResponse>,
) {
  const maxAttempts = Math.max(1, options.maxAttempts ?? 3)
  const baseDelayMs = Math.max(0, options.baseDelayMs ?? 300)
  const maxDelayMs = Math.max(baseDelayMs, options.maxDelayMs ?? 5_000)
  const sleep = options.sleep ?? defaultSleep

  let pending: { payload: TPayload } | undefined
  let active: Promise<SaveQueueResult<TPayload, TResponse>> | undefined
  let controller: AbortController | undefined
  let state: SaveQueueState<TPayload> = {
    status: 'idle',
    attempts: 0,
    pending: false,
    inFlight: false,
  }

  const publishState = (patch: Partial<SaveQueueState<TPayload>>) => {
    state = { ...state, ...patch }
    options.onStateChange?.({ ...state })
  }

  const retryDelay = (attempt: number) => Math.min(maxDelayMs, baseDelayMs * 2 ** (attempt - 1))

  const takePending = (): { payload: TPayload } | undefined => {
    const item = pending
    pending = undefined
    return item
  }

  const saveWithRetries = async (payload: TPayload): Promise<SaveQueueResult<TPayload, TResponse>> => {
    controller = new AbortController()
    for (let attempt = 1; attempt <= maxAttempts; attempt += 1) {
      publishState({
        status: attempt === 1 ? 'saving' : 'retrying',
        attempts: attempt,
        inFlight: true,
        pending: Boolean(pending),
        payload,
        error: undefined,
        serverVersion: undefined,
      })
      try {
        const response = await options.save(payload, controller.signal)
        publishState({
          status: 'saved',
          attempts: attempt,
          pending: Boolean(pending),
          inFlight: Boolean(pending),
          payload,
          error: undefined,
          serverVersion: undefined,
        })
        return { status: 'saved', attempts: attempt, payload, response }
      } catch (error) {
        const status = statusFrom(error)
        if (status === 409) {
          const serverVersion = serverVersionFrom(error)
          publishState({
            status: 'conflict',
            attempts: attempt,
            pending: false,
            inFlight: false,
            payload,
            serverVersion,
            error,
          })
          return { status: 'conflict', attempts: attempt, payload, serverVersion, error }
        }

        if (!isRetryable(error) || attempt === maxAttempts) {
          publishState({
            status: 'failed',
            attempts: attempt,
            pending: false,
            inFlight: false,
            payload,
            error,
            serverVersion: undefined,
          })
          return { status: 'failed', attempts: attempt, payload, error }
        }

        publishState({
          status: 'retrying',
          attempts: attempt,
          pending: Boolean(pending),
          inFlight: true,
          payload,
          error,
          serverVersion: undefined,
        })
        await sleep(retryDelay(attempt), controller.signal)
      }
    }

    throw new Error('unreachable save queue state')
  }

  const drain = async (): Promise<SaveQueueResult<TPayload, TResponse>> => {
    let lastResult: SaveQueueResult<TPayload, TResponse> | undefined
    while (pending) {
      const current = takePending()!.payload
      lastResult = await saveWithRetries(current)
      if (lastResult.status !== 'saved') {
        const queuedAfterFailure = takePending()
        if (queuedAfterFailure) {
          const preservedPayload = queuedAfterFailure.payload
          const result = { ...lastResult, payload: preservedPayload }
          publishState({ payload: preservedPayload, pending: false, inFlight: false })
          return result
        }
        return lastResult
      }
    }

    if (!lastResult) {
      throw new Error('save queue drained without a payload')
    }
    publishState({ pending: false, inFlight: false })
    return lastResult
  }

  return {
    enqueue(payload: TPayload) {
      pending = { payload }
      publishState({ pending: true, inFlight: Boolean(active), payload })
      if (!active) {
        active = Promise.resolve()
          .then(drain)
          .finally(() => {
            active = undefined
            controller = undefined
          })
      }
      return active
    },
    abort() {
      pending = undefined
      controller?.abort()
      publishState({ status: 'failed', pending: false, inFlight: false, error: new Error('Save queue aborted') })
    },
    getState() {
      return { ...state }
    },
  }
}
