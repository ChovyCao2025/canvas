export interface AnalyticsConfig {
  /** 后端事件接入写入密钥，用于 Basic Auth 认证。 */
  writeKey: string
  /** 批量上报服务地址，通常指向 Canvas 事件采集网关。 */
  serverUrl: string
  /** 队列达到该数量后自动 flush；未配置时默认 20 条。 */
  flushAt?: number
  /** 是否禁用 localStorage/sessionStorage 持久化，适合隐私敏感页面或无状态测试。 */
  disableClientPersistence?: boolean
  /** 是否启用合规同意门禁；启用后 optIn 前事件只入内存队列且不持久化。 */
  isComplianceEnabled?: boolean
}

/** 允许测试环境注入 fetch 替身，同时只依赖 SDK 需要的最小响应字段。 */
export type FetchLike = (input: string, init?: RequestInit) => Promise<{ ok: boolean; status: number }>

export interface AnalyticsRuntimeOptions {
  /** 自定义 fetch 实现，便于 SSR、测试或小程序容器替换网络层。 */
  fetchImpl?: FetchLike
  /** 自定义持久化存储，默认使用浏览器 localStorage。 */
  storage?: Storage
}

export interface OptOutOptions {
  /** 退出追踪时是否同时清理匿名 ID 和用户 ID。 */
  clearPersistence?: boolean
}

/** Canvas 采集协议支持的事件类型。 */
export type EventType = 'track' | 'identify' | 'page' | 'group' | 'alias'

export interface AnalyticsEvent {
  /** 单条消息唯一 ID，同时作为默认幂等键。 */
  messageId: string
  /** 事件类别，决定后端如何解释业务载荷。 */
  type: EventType
  /** track/page 事件名称。 */
  event?: string
  /** 已识别用户 ID；匿名访客或 optOut 状态下可能为空。 */
  userId?: string | null
  /** 浏览器侧匿名 ID，用于用户登录前后的行为归并。 */
  anonymousId: string
  /** 后端去重使用的幂等键。 */
  idempotencyKey: string
  /** group 事件关联的组织、租户或账号组 ID。 */
  groupId?: string
  /** alias 事件中的旧 ID，用于身份合并。 */
  previousId?: string
  /** 业务事件属性。 */
  properties?: Record<string, unknown>
  /** identify/group 的画像属性。 */
  traits?: Record<string, unknown>
  /** SDK 自动附加的页面、活动参数、会话和库版本上下文。 */
  context: Record<string, unknown>
  /** 客户端生成事件的时间。 */
  timestamp: string
  /** 本批次真正发送到服务端的时间。 */
  sentAt?: string
}

interface EventInput {
  /** 入队事件类型。 */
  type: EventType
  /** 入队事件名称。 */
  event?: string
  /** 本次事件显式绑定的用户 ID。 */
  userId?: string | null
  /** 本次事件显式绑定的 group ID。 */
  groupId?: string
  /** alias 事件中的旧 ID。 */
  previousId?: string
  /** 入队时提供的业务属性。 */
  properties?: Record<string, unknown>
  /** 入队时提供的用户或 group 画像属性。 */
  traits?: Record<string, unknown>
}

const SDK_VERSION = '0.1.0'
const QUEUE_KEY = 'canvas_sdk_queue'
const ANON_KEY = 'canvas_anonymous_id'
const USER_KEY = 'canvas_user_id'
const OPTOUT_KEY = 'canvas_opted_out'

export class CanvasAnalytics {
  /** 加载后的运行配置；未 load 前禁止 flush。 */
  private config: AnalyticsConfig | null = null
  /** 待发送事件队列，失败时保留以便下次 flush。 */
  private queue: AnalyticsEvent[] = []
  /** 网络层实现，可由测试或非浏览器运行时注入。 */
  private fetchImpl?: FetchLike
  /** 客户端持久化存储；不可用时 SDK 自动退化为内存队列。 */
  private storage: Storage | null
  /** 匿名访客 ID，贯穿登录前行为。 */
  private anonymous = ''
  /** 当前已识别用户 ID。 */
  private user: string | null = null
  /** 用户是否已退出追踪。 */
  private optedOut = false
  /** 合规门禁是否已获得同意。 */
  private consentGranted = true

  /**
   * 初始化 SDK 运行时依赖并恢复本地队列。
   *
   * <p>构造阶段不校验 writeKey/serverUrl，允许调用方先注入依赖再显式 load 配置。
   */
  constructor(options: AnalyticsRuntimeOptions = {}) {
    this.fetchImpl = options.fetchImpl || defaultFetch()
    this.storage = options.storage || defaultStorage()
    this.optedOut = this.storage?.getItem(OPTOUT_KEY) === '1'
    this.anonymous = this.storage?.getItem(ANON_KEY) || makeId('anon')
    this.user = this.storage?.getItem(USER_KEY) ?? null
    this.restoreQueue()
  }

  /** 创建并加载一个可立即使用的 SDK 实例。 */
  static load(config: AnalyticsConfig, options: AnalyticsRuntimeOptions = {}) {
    const analytics = new CanvasAnalytics(options)
    analytics.load(config)
    return analytics
  }

  /**
   * 加载采集配置并刷新身份/队列持久化状态。
   *
   * @param config 写入密钥、采集地址和合规/持久化策略
   * @returns 当前 SDK 实例，支持链式调用
   */
  load(config: AnalyticsConfig) {
    if (!config.writeKey || !config.serverUrl) {
      throw new Error('writeKey and serverUrl are required')
    }
    this.config = { flushAt: 20, ...config }
    this.consentGranted = !this.config.isComplianceEnabled
    this.persistIdentity()
    this.persistQueue()
    return this
  }

  /** 记录业务行为事件。 */
  track(event: string, properties: Record<string, unknown> = {}) {
    return this.enqueue({ type: 'track', event, properties })
  }

  /** 绑定登录用户并上报用户画像属性。 */
  identify(userId: string, traits: Record<string, unknown> = {}) {
    if (this.optedOut) return this
    this.user = requireText(userId, 'userId')
    this.persistIdentity()
    return this.enqueue({ type: 'identify', userId: this.user, traits, properties: traits })
  }

  /** 记录页面浏览事件，默认使用当前 document.title 作为页面名称。 */
  page(name = documentTitle(), properties: Record<string, unknown> = {}) {
    return this.enqueue({ type: 'page', event: name, properties })
  }

  /** 记录用户与组织、租户或账号组的关系。 */
  group(groupId: string, traits: Record<string, unknown> = {}) {
    return this.enqueue({
      type: 'group',
      groupId: requireText(groupId, 'groupId'),
      traits,
      properties: { groupId, ...traits },
    })
  }

  /** 将匿名 ID 或旧用户 ID 合并到新的用户 ID。 */
  alias(newId: string, previousId = this.anonymous) {
    if (this.optedOut) return this
    const nextUserId = requireText(newId, 'newId')
    this.user = nextUserId
    this.persistIdentity()
    return this.enqueue({
      type: 'alias',
      userId: nextUserId,
      previousId,
      properties: { previousId },
    })
  }

  /** 恢复追踪并允许合规门禁下的队列持久化。 */
  optIn() {
    this.optedOut = false
    this.consentGranted = true
    this.storage?.removeItem(OPTOUT_KEY)
    this.persistIdentity()
    this.persistQueue()
    return this
  }

  /**
   * 退出追踪并清空待发送队列。
   *
   * @param options 控制是否同时清理匿名 ID 和用户 ID
   */
  optOut(options: OptOutOptions = {}) {
    this.optedOut = true
    this.queue = []
    this.user = null
    this.storage?.setItem(OPTOUT_KEY, '1')
    this.storage?.removeItem(QUEUE_KEY)
    if (options.clearPersistence) {
      this.storage?.removeItem(ANON_KEY)
      this.storage?.removeItem(USER_KEY)
      this.anonymous = makeId('anon')
    }
    return this
  }

  /** 返回当前用户是否已退出追踪。 */
  hasOptedOut() {
    return this.optedOut
  }

  /**
   * 将内存队列批量发送到采集网关。
   *
   * <p>仅在未 optOut、已满足合规同意且队列非空时发送；发送失败会抛错并保留队列，
   * 发送成功后按 messageId 从本地队列删除已确认批次。
   */
  async flush() {
    if (this.optedOut || this.waitingForConsent() || this.queue.length === 0) {
      return
    }
    if (!this.config) {
      throw new Error('Canvas analytics must be loaded before flush')
    }
    if (!this.fetchImpl) {
      throw new Error('fetch is not available')
    }
    const sentAt = new Date().toISOString()
    const batch = this.queue.map(event => ({ ...event, sentAt }))
    const response = await this.fetchImpl(this.config.serverUrl, {
      method: 'POST',
      headers: {
        Authorization: `Basic ${base64Encode(`${this.config.writeKey}:`)}`,
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({ batch, sentAt }),
    })
    if (!response.ok) {
      throw new Error(`Canvas analytics flush failed with status ${response.status}`)
    }
    const sentIds = new Set(batch.map(event => event.messageId))
    this.queue = this.queue.filter(event => !sentIds.has(event.messageId))
    this.persistQueue()
  }

  /** 清空队列并重置用户/匿名身份，常用于退出登录或测试隔离。 */
  reset() {
    this.queue = []
    this.user = null
    this.anonymous = makeId('anon')
    this.storage?.removeItem(QUEUE_KEY)
    this.storage?.removeItem(USER_KEY)
    this.persistIdentity()
    return this
  }

  /** 返回当前待发送事件数量。 */
  queueSize() {
    return this.queue.length
  }

  /** 返回队列快照，避免调用方直接修改内部队列。 */
  peekQueue() {
    return this.queue.map(event => ({ ...event }))
  }

  /** 返回当前已识别用户 ID。 */
  userId() {
    return this.user
  }

  /** 返回当前匿名 ID。 */
  anonymousId() {
    return this.anonymous
  }

  /** 将单条事件补齐身份、上下文和幂等键后放入队列。 */
  private enqueue(input: EventInput) {
    if (this.optedOut) return this
    const messageId = makeId('msg')
    const event: AnalyticsEvent = {
      messageId,
      type: input.type,
      event: input.event,
      userId: input.userId ?? this.user,
      anonymousId: this.anonymous,
      idempotencyKey: messageId,
      groupId: input.groupId,
      previousId: input.previousId,
      properties: input.properties || {},
      traits: input.traits,
      context: buildContext(),
      timestamp: new Date().toISOString(),
    }
    this.queue.push(stripUndefined(event))
    this.persistQueue()
    // 达到批量阈值后异步 flush；失败留给调用方后续显式 flush 或下一次阈值触发。
    if (this.config?.flushAt && this.queue.length >= this.config.flushAt) {
      void this.flush()
    }
    return this
  }

  /** 从持久化存储恢复未发送队列，解析失败时丢弃损坏内容。 */
  private restoreQueue() {
    const raw = this.storage?.getItem(QUEUE_KEY)
    if (!raw) return
    try {
      const parsed = JSON.parse(raw)
      this.queue = Array.isArray(parsed) ? parsed : []
    } catch {
      this.queue = []
    }
  }

  /** 持久化匿名 ID 和已识别用户 ID，受 optOut/合规门禁控制。 */
  private persistIdentity() {
    if (this.persistenceDisabled()) return
    this.storage?.setItem(ANON_KEY, this.anonymous)
    if (this.user) {
      this.storage?.setItem(USER_KEY, this.user)
    }
  }

  /** 持久化待发送队列，避免页面刷新导致未上报事件丢失。 */
  private persistQueue() {
    if (this.persistenceDisabled()) return
    this.storage?.setItem(QUEUE_KEY, JSON.stringify(this.queue))
  }

  /** 判断当前是否禁止写入本地存储。 */
  private persistenceDisabled() {
    return this.optedOut
      || this.config?.disableClientPersistence === true
      || this.waitingForConsent()
  }

  /** 合规模式下尚未 optIn 时，队列不可持久化也不可发送。 */
  private waitingForConsent() {
    return this.config?.isComplianceEnabled === true && !this.consentGranted
  }
}

/** 创建未加载配置的 SDK 实例，适合延迟注入 writeKey 的应用。 */
export function createAnalytics(options: AnalyticsRuntimeOptions = {}) {
  return new CanvasAnalytics(options)
}

/** 构造每条事件共享的自动上下文。 */
function buildContext() {
  return {
    library: {
      name: '@canvas/analytics-web',
      version: SDK_VERSION,
    },
    page: pageContext(),
    campaign: campaignContext(),
    sessionId: sessionId(),
  }
}

/** 读取浏览器页面上下文；SSR 环境返回空对象。 */
function pageContext() {
  if (typeof window === 'undefined') {
    return {}
  }
  return {
    url: window.location.href,
    path: window.location.pathname,
    search: window.location.search,
    title: documentTitle(),
    referrer: typeof document === 'undefined' ? '' : document.referrer,
  }
}

/** 提取当前 URL 中的 UTM 活动参数。 */
function campaignContext() {
  if (typeof window === 'undefined') {
    return {}
  }
  const params = new URLSearchParams(window.location.search)
  const campaign: Record<string, string> = {}
  // 只采集标准 UTM 字段，避免把任意查询参数带入事件上下文。
  for (const key of ['utm_source', 'utm_medium', 'utm_campaign', 'utm_term', 'utm_content']) {
    const value = params.get(key)
    if (value) campaign[key] = value
  }
  return campaign
}

/** 获取或创建浏览器会话 ID，用于同一 tab 会话内事件串联。 */
function sessionId() {
  const storage = defaultStorage('session')
  const key = 'canvas_session_id'
  const existing = storage?.getItem(key)
  if (existing) return existing
  const next = makeId('sess')
  storage?.setItem(key, next)
  return next
}

/** 安全读取文档标题，兼容非浏览器运行时。 */
function documentTitle() {
  return typeof document === 'undefined' ? '' : document.title
}

/** 生成带业务前缀的随机 ID，优先使用 crypto.randomUUID。 */
function makeId(prefix: string) {
  const random = globalThis.crypto?.randomUUID?.()
    || `${Date.now().toString(36)}_${Math.random().toString(36).slice(2)}`
  return `${prefix}_${random}`
}

/** 将 Basic Auth 明文转换为 base64。 */
function base64Encode(value: string) {
  return btoa(value)
}

/** 校验必填文本字段并返回去除首尾空白后的值。 */
function requireText(value: string, fieldName: string) {
  if (!value || !value.trim()) {
    throw new Error(`${fieldName} is required`)
  }
  return value.trim()
}

/** 获取浏览器存储；禁用存储或 SSR 时返回 null。 */
function defaultStorage(kind: 'local' | 'session' = 'local') {
  if (typeof window === 'undefined') return null
  try {
    return kind === 'session' ? window.sessionStorage : window.localStorage
  } catch {
    return null
  }
}

/** 获取全局 fetch，并绑定 globalThis 保持浏览器实现的 this 语义。 */
function defaultFetch() {
  if (typeof fetch === 'undefined') return undefined
  return fetch.bind(globalThis) as FetchLike
}

/** 删除 undefined 字段，避免 JSON 载荷出现无意义键。 */
function stripUndefined<T extends object>(value: T): T {
  return Object.fromEntries(Object.entries(value).filter(([, entry]) => entry !== undefined)) as T
}
