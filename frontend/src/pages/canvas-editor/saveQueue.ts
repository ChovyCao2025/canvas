/** 自动保存队列状态，用于驱动编辑器保存提示。 */
export type SaveQueueStatus = 'idle' | 'saving' | 'retrying' | 'saved' | 'failed' | 'conflict'

/** 保存队列的当前快照状态。 */
export interface SaveQueueState<TPayload = unknown> {
  /** 当前保存状态。 */
  status: SaveQueueStatus

  /** 当前载荷已尝试保存的次数。 */
  attempts: number

  /** 是否有等待合并保存的最新载荷。 */
  pending: boolean

  /** 是否有保存请求正在执行。 */
  inFlight: boolean

  /** 最近一次入队或正在保存的载荷。 */
  payload?: TPayload

  /** 冲突时服务端返回的最新版本号。 */
  serverVersion?: number

  /** 最近一次保存错误。 */
  error?: unknown
}

/** 单次队列 drain 的最终结果。 */
export type SaveQueueResult<TPayload, TResponse = unknown> =
  | { status: 'saved'; attempts: number; payload: TPayload; response: TResponse }
  | { status: 'failed'; attempts: number; payload: TPayload; error: unknown }
  | { status: 'conflict'; attempts: number; payload: TPayload; serverVersion?: number; error: unknown }

/** 创建保存队列所需的策略和回调。 */
export interface SaveQueueOptions<TPayload, TResponse = unknown> {
  /** 实际保存函数；AbortSignal 用于取消进行中的重试等待。 */
  save: (payload: TPayload, signal: AbortSignal) => Promise<TResponse>

  /** 最大尝试次数，默认 3。 */
  maxAttempts?: number

  /** 指数退避基础延迟毫秒数。 */
  baseDelayMs?: number

  /** 指数退避最大延迟毫秒数。 */
  maxDelayMs?: number

  /** 可替换的 sleep 实现，测试中可注入无等待版本。 */
  sleep?: (delayMs: number, signal: AbortSignal) => Promise<void>

  /** 队列状态变化回调。 */
  onStateChange?: (state: SaveQueueState<TPayload>) => void
}

/** 判断未知错误对象是否可按 record 读取字段。 */
function isRecord(value: unknown): value is Record<string, unknown> {
  return Boolean(value) && typeof value === 'object'
}

/** 从多种 API 错误包装格式中提取 HTTP 状态码。 */
function statusFrom(error: unknown): number | undefined {
  if (!isRecord(error)) return undefined
  if (typeof error.status === 'number') return error.status
  const response = isRecord(error.response) ? error.response : undefined
  return typeof response?.status === 'number' ? response.status : undefined
}

/** 从冲突错误中提取服务端版本号，兼容 axios 和业务包装结构。 */
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

/** 判断错误是否值得重试；409 冲突必须立即交给用户处理。 */
function isRetryable(error: unknown): boolean {
  if (!isRecord(error)) return false
  if (typeof error.retryable === 'boolean') return error.retryable
  const status = statusFrom(error)
  if (status === 409) return false
  return status === 408 || status === 429 || (typeof status === 'number' && status >= 500)
}

/** 默认退避等待，支持保存队列 abort 时中断计时器。 */
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

/** 创建合并式自动保存队列：只保存最新 pending 载荷，并对临时错误重试。 */
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

  /** 按尝试次数计算指数退避延迟。 */
  const retryDelay = (attempt: number) => Math.min(maxDelayMs, baseDelayMs * 2 ** (attempt - 1))

  /** 取走最新 pending 载荷；队列只保留最后一次编辑结果。 */
  const takePending = (): { payload: TPayload } | undefined => {
    const item = pending
    pending = undefined
    return item
  }

  /** 保存单个载荷，遇到可重试错误按退避策略重试。 */
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
          // 版本冲突不能自动覆盖，保留本地 payload 交给编辑器提示恢复。
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
          // 非临时错误或重试耗尽后结束本轮 drain。
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
        // 临时错误等待后重试；期间如有新编辑，会体现在 pending 状态上。
        await sleep(retryDelay(attempt), controller.signal)
      }
    }

    throw new Error('unreachable save queue state')
  }

  /** 顺序消费 pending；保存失败时保留失败期间最新入队的载荷用于上层本地草稿。 */
  const drain = async (): Promise<SaveQueueResult<TPayload, TResponse>> => {
    let lastResult: SaveQueueResult<TPayload, TResponse> | undefined
    while (pending) {
      const current = takePending()!.payload
      lastResult = await saveWithRetries(current)
      if (lastResult.status !== 'saved') {
        const queuedAfterFailure = takePending()
        if (queuedAfterFailure) {
          // 保存失败期间发生的新编辑更重要，结果 payload 替换为最新载荷。
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
        // 没有活动 drain 时启动一轮；已有活动时仅覆盖 pending。
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
