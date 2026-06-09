/** 监控来源保存载荷，定义品牌/竞品内容从哪里进入监控闭环。 */
export interface MarketingMonitorSourcePayload {
  /** 来源业务键，用于幂等保存和轮询定位。 */
  sourceKey: string
  /** 来源类型，例如 SOCIAL、WEB、REVIEW。 */
  sourceType: string
  /** 来源展示名称。 */
  displayName: string
  /** 是否启用该来源的入库和轮询。 */
  enabled: boolean
  /** 来源扩展配置。 */
  metadata: Record<string, unknown>
}

/** 已注册的监控来源。 */
export interface MarketingMonitorSource extends MarketingMonitorSourcePayload {
  /** 来源主键。 */
  id: number
  /** 租户 ID。 */
  tenantId: number
  /** 创建人。 */
  createdBy?: string
  /** 创建时间。 */
  createdAt?: string
  /** 更新时间。 */
  updatedAt?: string
}

/** 监控来源轮询配置载荷。 */
export interface MarketingMonitorSourcePollingPayload {
  /** 是否启用定时轮询。 */
  pollEnabled: boolean
  /** 轮询间隔分钟数。 */
  pollIntervalMinutes: number
  /** 当前供应商游标。 */
  pollCursor?: string
  /** 下一次计划轮询时间。 */
  nextPollAt?: string
}

/** 监控来源轮询状态，用于判断外部内容采集是否健康。 */
export interface MarketingMonitorSourcePolling {
  /** 租户 ID。 */
  tenantId: number
  /** 来源 ID。 */
  sourceId: number
  /** 来源业务键。 */
  sourceKey: string
  /** 来源类型。 */
  sourceType: string
  /** 是否启用轮询。 */
  pollEnabled: boolean
  /** 轮询间隔分钟数。 */
  pollIntervalMinutes: number
  /** 当前供应商游标。 */
  pollCursor?: string
  /** 最近一次轮询时间。 */
  lastPolledAt?: string
  /** 下一次计划轮询时间。 */
  nextPollAt?: string
  /** 最近轮询状态。 */
  lastPollStatus?: string
  /** 更新时间。 */
  updatedAt?: string
}

/** 手工或 webhook 入库的监控内容载荷。 */
export interface MarketingMonitorItemIngestPayload {
  /** 归属来源 ID。 */
  sourceId: number
  /** 外部平台内容 ID。 */
  externalItemId: string
  /** 原文链接。 */
  sourceUrl?: string
  /** 作者标识。 */
  authorKey?: string
  /** 品牌标识。 */
  brandKey?: string
  /** 待分析文本。 */
  text: string
  /** 文本语言。 */
  language?: string
  /** 外部发布时间。 */
  publishedAt?: string
  /** 竞品词表，key 为竞品，value 为命中词。 */
  competitors: Record<string, string[]>
  /** 原始供应商载荷。 */
  rawPayload: Record<string, unknown>
}

/** 情绪分析结果，服务于负面监控和趋势聚合。 */
export interface MarketingSentimentAnalysis {
  /** 分析记录主键。 */
  id: number
  /** 租户 ID。 */
  tenantId: number
  /** 内容项 ID。 */
  itemId: number
  /** 情绪标签。 */
  sentimentLabel: string
  /** 情绪分数。 */
  sentimentScore: number
  /** 模型置信度。 */
  confidence: number
  /** 模型键。 */
  modelKey: string
  /** 模型版本。 */
  modelVersion: string
  /** 关键词命中证据。 */
  keywordHits: Record<string, unknown>
  /** 创建时间。 */
  createdAt?: string
}

/** 竞品命中记录，用于竞品声量和情绪监控。 */
export interface MarketingCompetitorMention {
  /** 命中记录主键。 */
  id: number
  /** 租户 ID。 */
  tenantId: number
  /** 内容项 ID。 */
  itemId: number
  /** 竞品键。 */
  competitorKey: string
  /** 竞品展示名称。 */
  competitorName: string
  /** 命中的词项。 */
  matchedTerms: string[]
  /** 关联情绪标签。 */
  sentimentLabel: string
  /** 关联情绪分数。 */
  sentimentScore: number
  /** 创建时间。 */
  createdAt?: string
}

/** 监控内容项，承载品牌提及、情绪和竞品命中。 */
export interface MarketingMonitorItem {
  /** 内容项主键。 */
  id: number
  /** 租户 ID。 */
  tenantId: number
  /** 来源 ID。 */
  sourceId: number
  /** 外部内容 ID。 */
  externalItemId: string
  /** 来源类型。 */
  sourceType: string
  /** 外部链接。 */
  sourceUrl?: string
  /** 作者标识。 */
  authorKey?: string
  /** 品牌标识。 */
  brandKey?: string
  /** 正文文本。 */
  text: string
  /** 文本语言。 */
  language?: string
  /** 外部发布时间。 */
  publishedAt?: string
  /** 入库时间。 */
  ingestedAt?: string
  /** 原始供应商载荷。 */
  rawPayload: Record<string, unknown>
  /** 情绪标签。 */
  sentimentLabel?: string
  /** 情绪分数。 */
  sentimentScore?: number
  /** 情绪模型置信度。 */
  confidence?: number
  /** 命中的竞品键集合。 */
  competitorKeys: string[]
}

/** 监控告警，覆盖负面情绪、竞品异常和集成探针异常。 */
export interface MarketingMonitorAlert {
  /** 告警主键。 */
  id: number
  /** 租户 ID。 */
  tenantId: number
  /** 告警类型。 */
  alertType: string
  /** 严重度。 */
  severity: string
  /** 处理状态。 */
  status: string
  /** 告警范围键，例如品牌、竞品或集成契约。 */
  scopeKey?: string
  /** 告警标题。 */
  title: string
  /** 告警原因。 */
  reason?: string
  /** 告警窗口内命中数量。 */
  itemCount: number
  /** 告警窗口开始时间。 */
  windowStart?: string
  /** 告警窗口结束时间。 */
  windowEnd?: string
  /** 告警扩展证据。 */
  metadata: Record<string, unknown>
  /** 创建人。 */
  createdBy?: string
  /** 处理人。 */
  resolvedBy?: string
  /** 处理时间。 */
  resolvedAt?: string
  /** 创建时间。 */
  createdAt?: string
  /** 更新时间。 */
  updatedAt?: string
}

/** 手动触发来源轮询的请求载荷。 */
export interface MarketingMonitorPollPayload {
  /** 请求窗口开始。 */
  requestedFrom?: string
  /** 请求窗口结束。 */
  requestedUntil?: string
  /** 覆盖默认游标。 */
  cursorOverride?: string
  /** 最大采集条数。 */
  maxItems: number
  /** 是否强制忽略间隔限制。 */
  force: boolean
}

/** 来源轮询运行结果，用于排查采集质量和告警产出。 */
export interface MarketingMonitorPollRun {
  /** 轮询运行主键。 */
  id: number
  /** 租户 ID。 */
  tenantId: number
  /** 来源 ID。 */
  sourceId: number
  /** 来源业务键。 */
  sourceKey: string
  /** 来源类型。 */
  sourceType: string
  /** 运行状态。 */
  status: string
  /** 请求窗口开始。 */
  requestedFrom?: string
  /** 请求窗口结束。 */
  requestedUntil?: string
  /** 轮询前游标。 */
  cursorBefore?: string
  /** 轮询后游标。 */
  cursorAfter?: string
  /** 本次拉取条数。 */
  itemCount: number
  /** 新增入库条数。 */
  insertedCount: number
  /** 重复内容条数。 */
  duplicateCount: number
  /** 产生告警数量。 */
  alertCount: number
  /** 错误信息。 */
  errorMessage?: string
  /** 运行元数据。 */
  metadata: Record<string, unknown>
  /** 创建人。 */
  createdBy?: string
  /** 开始时间。 */
  startedAt?: string
  /** 结束时间。 */
  finishedAt?: string
  /** 创建时间。 */
  createdAt?: string
  /** 更新时间。 */
  updatedAt?: string
}

/** 生成趋势快照的请求载荷。 */
export interface MarketingMonitorTrendSnapshotPayload {
  /** 来源 ID。 */
  sourceId: number
  /** 聚合粒度，例如 HOUR、DAY。 */
  bucketGrain: string
  /** 聚合窗口开始。 */
  bucketStart: string
  /** 聚合窗口结束。 */
  bucketEnd: string
  /** 品牌筛选键。 */
  brandKey?: string
  /** 竞品筛选键。 */
  competitorKey?: string
  /** 趋势快照元数据。 */
  metadata: Record<string, unknown>
}

/** 趋势快照查询条件。 */
export interface MarketingMonitorTrendSnapshotQuery {
  /** 按来源筛选。 */
  sourceId?: number
  /** 按品牌筛选。 */
  brandKey?: string
  /** 按竞品筛选。 */
  competitorKey?: string
  /** 返回数量上限。 */
  limit?: number
}

/** 品牌/竞品监控趋势快照。 */
export interface MarketingMonitorTrendSnapshot {
  /** 快照主键。 */
  id: number
  /** 租户 ID。 */
  tenantId: number
  /** 来源 ID。 */
  sourceId: number
  /** 来源业务键。 */
  sourceKey: string
  /** 聚合粒度。 */
  bucketGrain: string
  /** 聚合窗口开始。 */
  bucketStart: string
  /** 聚合窗口结束。 */
  bucketEnd: string
  /** 品牌键。 */
  brandKey?: string
  /** 竞品键。 */
  competitorKey?: string
  /** 提及总数。 */
  mentionCount: number
  /** 正面数量。 */
  positiveCount: number
  /** 中性数量。 */
  neutralCount: number
  /** 负面数量。 */
  negativeCount: number
  /** 竞品命中数量。 */
  competitorCount: number
  /** 告警数量。 */
  alertCount: number
  /** 平均情绪分。 */
  avgSentimentScore?: number
  /** 聚合证据或扩展信息。 */
  metadata: Record<string, unknown>
  /** 创建人。 */
  createdBy?: string
  /** 创建时间。 */
  createdAt?: string
  /** 更新时间。 */
  updatedAt?: string
}

/** 外部监控 Provider 的访问凭据。 */
export interface MarketingMonitorProviderCredential {
  /** 凭据主键。 */
  id: number
  /** 租户 ID。 */
  tenantId: number
  /** 凭据业务键。 */
  credentialKey: string
  /** Provider 类型。 */
  providerType: string
  /** 认证类型。 */
  authType: string
  /** 凭据展示名称。 */
  displayName: string
  /** 启用或停用状态。 */
  status: string
  /** token 类型。 */
  tokenType?: string
  /** 授权 scope 集合。 */
  scopes: string[]
  /** access token 前缀，仅用于识别不暴露完整密钥。 */
  accessTokenPrefix?: string
  /** refresh token 前缀。 */
  refreshTokenPrefix?: string
  /** API key 前缀。 */
  apiKeyPrefix?: string
  /** 刷新端点。 */
  refreshEndpoint?: string
  /** 撤销端点。 */
  revokeEndpoint?: string
  /** access token 过期时间。 */
  expiresAt?: string
  /** refresh token 过期时间。 */
  refreshTokenExpiresAt?: string
  /** 撤销时间。 */
  revokedAt?: string
  /** 最近刷新时间。 */
  lastRefreshedAt?: string
  /** 刷新尝试次数。 */
  refreshAttemptCount: number
  /** 最近刷新状态。 */
  lastRefreshStatus?: string
  /** 最近刷新错误。 */
  lastRefreshError?: string
  /** 最近撤销状态。 */
  lastRevokeStatus?: string
  /** 最近撤销错误。 */
  lastRevokeError?: string
  /** 凭据扩展元数据。 */
  metadata: Record<string, unknown>
  /** 创建人。 */
  createdBy?: string
  /** 更新人。 */
  updatedBy?: string
  /** 创建时间。 */
  createdAt?: string
  /** 更新时间。 */
  updatedAt?: string
}

/** Provider 凭据查询条件。 */
export interface MarketingMonitorProviderCredentialQuery {
  /** 按 Provider 类型筛选。 */
  providerType?: string
  /** 按认证类型筛选。 */
  authType?: string
  /** 按凭据状态筛选。 */
  status?: string
  /** 返回数量上限。 */
  limit?: number
}

/** Provider 凭据刷新、撤销、停用等审计事件。 */
export interface MarketingMonitorProviderCredentialEvent {
  /** 事件主键。 */
  id: number
  /** 租户 ID。 */
  tenantId: number
  /** 凭据 ID。 */
  credentialId: number
  /** 凭据业务键。 */
  credentialKey: string
  /** 事件类型。 */
  eventType: string
  /** 事件状态。 */
  status: string
  /** 事件元数据。 */
  metadata: Record<string, unknown>
  /** 错误信息。 */
  errorMessage?: string
  /** 创建人。 */
  createdBy?: string
  /** 创建时间。 */
  createdAt?: string
}

/** Provider 凭据事件查询条件。 */
export interface MarketingMonitorProviderCredentialEventQuery {
  /** 按凭据键筛选。 */
  credentialKey?: string
  /** 按事件类型筛选。 */
  eventType?: string
  /** 按事件状态筛选。 */
  status?: string
  /** 返回数量上限。 */
  limit?: number
}

/** 批量刷新即将到期凭据的命令。 */
export interface MarketingMonitorProviderCredentialDueRefreshCommand {
  /** 到期检测窗口分钟数。 */
  windowMinutes?: number
  /** 本次刷新上限。 */
  limit?: number
}

/** 批量刷新到期凭据的执行结果。 */
export interface MarketingMonitorProviderCredentialDueRefreshResult {
  /** 租户 ID。 */
  tenantId: number
  /** 候选凭据数量。 */
  candidateCount: number
  /** 到期凭据数量。 */
  dueCount: number
  /** 成功刷新数量。 */
  refreshedCount: number
  /** 刷新失败数量。 */
  failedCount: number
  /** 跳过数量。 */
  skippedCount: number
  /** 到期判断截止时间。 */
  cutoffAt?: string
  /** 评估时间。 */
  evaluatedAt?: string
  /** 本次涉及的凭据。 */
  credentials: MarketingMonitorProviderCredential[]
}

/** 撤销 Provider 凭据的请求。 */
export interface MarketingMonitorProviderCredentialRevokeCommand {
  /** 覆盖默认撤销端点。 */
  revokeEndpoint?: string
  /** token 类型提示。 */
  tokenTypeHint?: string
  /** 是否同时撤销 refresh token。 */
  revokeRefreshToken?: boolean
  /** 撤销后是否停用本地凭据。 */
  disableAfterRevoke?: boolean
  /** 撤销元数据。 */
  metadata?: Record<string, unknown>
}

/** 发起 OAuth 授权的请求。 */
export interface MarketingMonitorProviderOAuthAuthorizationCommand {
  /** 待绑定凭据键。 */
  credentialKey: string
  /** Provider 类型。 */
  providerType: string
  /** 认证类型。 */
  authType: string
  /** 凭据展示名称。 */
  displayName: string
  /** Provider authorize 端点。 */
  authorizeEndpoint: string
  /** Provider token 端点。 */
  tokenEndpoint: string
  /** Provider revoke 端点。 */
  revokeEndpoint?: string
  /** OAuth 回调地址。 */
  redirectUri: string
  /** OAuth client id。 */
  clientId: string
  /** OAuth client secret。 */
  clientSecret?: string
  /** 请求的 scope。 */
  scopes: string[]
  /** 额外授权参数。 */
  authorizeParams: Record<string, unknown>
  /** state 有效分钟数。 */
  expiresInMinutes?: number
  /** 授权元数据。 */
  metadata: Record<string, unknown>
}

/** 完成 OAuth 回调的请求。 */
export interface MarketingMonitorProviderOAuthCallbackCommand {
  /** 授权 state。 */
  state: string
  /** Provider 返回的 code。 */
  code?: string
  /** Provider 返回的错误码。 */
  error?: string
  /** Provider 返回的错误描述。 */
  errorDescription?: string
  /** 回调元数据。 */
  metadata: Record<string, unknown>
}

/** OAuth 授权会话，用于换取并落库 Provider 凭据。 */
export interface MarketingMonitorProviderOAuthAuthorization {
  /** 授权会话主键。 */
  id: number
  /** 租户 ID。 */
  tenantId: number
  /** 授权 state。 */
  authState: string
  /** 凭据键。 */
  credentialKey: string
  /** Provider 类型。 */
  providerType: string
  /** 认证类型。 */
  authType: string
  /** 展示名称。 */
  displayName: string
  /** 授权状态。 */
  status: string
  /** Provider 授权 URL。 */
  authorizationUrl?: string
  /** authorize 端点。 */
  authorizeEndpoint: string
  /** token 端点。 */
  tokenEndpoint: string
  /** 回调地址。 */
  redirectUri: string
  /** scope 集合。 */
  scopes: string[]
  /** PKCE challenge 方法。 */
  codeChallengeMethod?: string
  /** 换 token 后生成的凭据 ID。 */
  credentialId?: number
  /** Provider 错误码。 */
  providerError?: string
  /** Provider 错误描述。 */
  providerErrorDescription?: string
  /** 最近 HTTP 状态。 */
  lastHttpStatus?: number
  /** 最近错误信息。 */
  lastErrorMessage?: string
  /** 授权会话过期时间。 */
  expiresAt?: string
  /** 授权完成时间。 */
  completedAt?: string
  /** 授权元数据。 */
  metadata: Record<string, unknown>
  /** 创建人。 */
  createdBy?: string
  /** 更新人。 */
  updatedBy?: string
  /** 创建时间。 */
  createdAt?: string
  /** 更新时间。 */
  updatedAt?: string
}

/** OAuth 授权会话查询条件。 */
export interface MarketingMonitorProviderOAuthAuthorizationQuery {
  /** 按凭据键筛选。 */
  credentialKey?: string
  /** 按 Provider 类型筛选。 */
  providerType?: string
  /** 按授权状态筛选。 */
  status?: string
  /** 返回数量上限。 */
  limit?: number
}

/** 内容入库后的联动结果：内容项、情绪、竞品和告警。 */
export interface MarketingMonitorIngestResult {
  /** 入库内容项。 */
  item: MarketingMonitorItem
  /** 情绪分析结果。 */
  sentiment: MarketingSentimentAnalysis
  /** 竞品命中结果。 */
  competitorMentions: MarketingCompetitorMention[]
  /** 入库触发的告警。 */
  alerts: MarketingMonitorAlert[]
}

/** 监控内容项查询条件。 */
export interface MarketingMonitorItemQuery {
  /** 按情绪标签筛选。 */
  sentimentLabel?: string
  /** 按竞品键筛选。 */
  competitorKey?: string
  /** 返回数量上限。 */
  limit?: number
}

/** 监控告警查询条件。 */
export interface MarketingMonitorAlertQuery {
  /** 按处理状态筛选。 */
  status?: string
  /** 返回数量上限。 */
  limit?: number
}

/** 监控工作台 KPI 展示模型。 */
export interface MonitoringKpis {
  /** 当前列表可见提及数量。 */
  visibleMentions: number
  /** 当前列表负面提及数量。 */
  negativeMentions: number
  /** 当前列表含竞品命中的提及数量。 */
  competitorMentions: number
  /** OPEN 状态告警数量。 */
  openAlerts: number
}

/** 归一化内容项查询参数，统一大小写、key 和 limit 边界。 */
export function normalizeItemQuery(filters: MarketingMonitorItemQuery = {}): MarketingMonitorItemQuery {
  return {
    sentimentLabel: normalizeOptionalUpper(filters.sentimentLabel),
    competitorKey: normalizeOptionalKey(filters.competitorKey),
    limit: boundedLimit(filters.limit),
  }
}

/** 归一化告警查询参数，保证状态筛选和 limit 符合后端预期。 */
export function normalizeAlertQuery(filters: MarketingMonitorAlertQuery = {}): MarketingMonitorAlertQuery {
  return {
    status: normalizeOptionalUpper(filters.status),
    limit: boundedLimit(filters.limit),
  }
}

/** 归一化趋势快照查询参数，过滤无效 sourceId 并规范品牌/竞品 key。 */
export function normalizeTrendQuery(
  filters: MarketingMonitorTrendSnapshotQuery = {},
): MarketingMonitorTrendSnapshotQuery {
  return {
    sourceId: normalizeOptionalPositiveNumber(filters.sourceId),
    brandKey: normalizeOptionalKey(filters.brandKey),
    competitorKey: normalizeOptionalKey(filters.competitorKey),
    limit: boundedLimit(filters.limit),
  }
}

/** 归一化 Provider 凭据查询参数。 */
export function normalizeCredentialQuery(
  filters: MarketingMonitorProviderCredentialQuery = {},
): MarketingMonitorProviderCredentialQuery {
  return {
    providerType: normalizeOptionalUpper(filters.providerType),
    authType: normalizeOptionalUpper(filters.authType),
    status: normalizeOptionalUpper(filters.status),
    limit: boundedLimit(filters.limit),
  }
}

/** 归一化 Provider 凭据事件查询参数。 */
export function normalizeCredentialEventQuery(
  filters: MarketingMonitorProviderCredentialEventQuery = {},
): MarketingMonitorProviderCredentialEventQuery {
  return {
    credentialKey: normalizeOptionalKey(filters.credentialKey),
    eventType: normalizeOptionalUpper(filters.eventType),
    status: normalizeOptionalUpper(filters.status),
    limit: boundedLimit(filters.limit),
  }
}

/** 归一化 OAuth 授权查询参数。 */
export function normalizeOAuthAuthorizationQuery(
  filters: MarketingMonitorProviderOAuthAuthorizationQuery = {},
): MarketingMonitorProviderOAuthAuthorizationQuery {
  return {
    credentialKey: normalizeOptionalKey(filters.credentialKey),
    providerType: normalizeOptionalUpper(filters.providerType),
    status: normalizeOptionalUpper(filters.status),
    limit: boundedLimit(filters.limit),
  }
}

/** 解析表单 JSON 文本，确保监控元数据和载荷均为对象。 */
export function parseJsonObject(value?: string): Record<string, unknown> {
  const text = value?.trim()
  if (!text) return {}
  const parsed = JSON.parse(text) as unknown
  if (!parsed || typeof parsed !== 'object' || Array.isArray(parsed)) {
    throw new Error('JSON 必须是对象')
  }
  return parsed as Record<string, unknown>
}

/** 将空格或逗号分隔的 OAuth scope 文本去重为数组。 */
export function parseScopes(value?: string): string[] {
  return (value ?? '')
    .split(/[\s,]+/)
    .map(scope => scope.trim())
    .filter((scope, index, all) => scope.length > 0 && all.indexOf(scope) === index)
}

/** 生成默认 OAuth 回调地址，服务端渲染时退回相对路径。 */
export function defaultOAuthRedirectUri(pathname = '/marketing-monitoring'): string {
  if (typeof window === 'undefined') return pathname
  return `${window.location.origin}${pathname}`
}

/** 解析竞品词表 JSON，并将竞品 key 和词项规整去重。 */
export function parseCompetitorMap(value?: string): Record<string, string[]> {
  const parsed = parseJsonObject(value)
  const result: Record<string, string[]> = {}
  // 每个竞品必须配置字符串数组，避免异常结构进入情绪/竞品识别链路。
  Object.entries(parsed).forEach(([key, terms]) => {
    if (!Array.isArray(terms)) {
      throw new Error('竞品词表必须是字符串数组')
    }
    const normalizedTerms = terms
      .filter((term): term is string => typeof term === 'string')
      .map(term => term.trim())
      .filter((term, index, all) => term.length > 0 && all.indexOf(term) === index)
    if (normalizedTerms.length > 0) {
      result[key.trim().toLowerCase()] = normalizedTerms
    }
  })
  return result
}

/** 将 Provider 凭据状态映射为监控工作台标签。 */
export function credentialStatusView(status?: string | null): { text: string; color: string } {
  const views: Record<string, { text: string; color: string }> = {
    ACTIVE: { text: '启用', color: 'green' },
    DISABLED: { text: '停用', color: 'default' },
  }
  const key = (status ?? '').toUpperCase()
  return views[key] ?? { text: status || '-', color: 'default' }
}

/** 将 OAuth 授权状态映射为监控工作台标签。 */
export function oauthAuthorizationStatusView(status?: string | null): { text: string; color: string } {
  const views: Record<string, { text: string; color: string }> = {
    PENDING: { text: '待回调', color: 'gold' },
    EXCHANGED: { text: '已换 token', color: 'green' },
    FAILED: { text: '失败', color: 'red' },
    EXPIRED: { text: '已过期', color: 'default' },
  }
  const key = (status ?? '').toUpperCase()
  return views[key] ?? { text: status || '-', color: 'default' }
}

/** 将情绪标签映射为中文和颜色。 */
export function sentimentView(status?: string | null): { text: string; color: string } {
  const views: Record<string, { text: string; color: string }> = {
    NEGATIVE: { text: '负面', color: 'red' },
    POSITIVE: { text: '正面', color: 'green' },
    NEUTRAL: { text: '中性', color: 'blue' },
  }
  const key = (status ?? '').toUpperCase()
  return views[key] ?? { text: status || '-', color: 'default' }
}

/** 将告警状态映射为中文和颜色。 */
export function alertStatusView(status?: string | null): { text: string; color: string } {
  const views: Record<string, { text: string; color: string }> = {
    OPEN: { text: '待处理', color: 'red' },
    RESOLVED: { text: '已处理', color: 'default' },
  }
  const key = (status ?? '').toUpperCase()
  return views[key] ?? { text: status || '-', color: 'default' }
}

/** 将异常严重度映射为告警标签颜色。 */
export function severityColor(severity?: string | null) {
  switch ((severity ?? '').toUpperCase()) {
    case 'CRITICAL':
    case 'HIGH':
      return 'red'
    case 'MEDIUM':
      return 'orange'
    case 'LOW':
      return 'blue'
    default:
      return 'default'
  }
}

/** 格式化监控时间，保留到秒用于列表展示。 */
export function formatMonitorDateTime(value?: string | null) {
  if (!value) return '-'
  return value.replace('T', ' ').slice(0, 19)
}

/** 聚合监控工作台 KPI：提及、负面、竞品命中和待处理告警。 */
export function calculateMonitoringKpis(
  items: MarketingMonitorItem[] = [],
  alerts: MarketingMonitorAlert[] = [],
): MonitoringKpis {
  return {
    visibleMentions: items.length,
    // 负面提及按情绪标签大小写归一后计数。
    negativeMentions: items.filter(item => (item.sentimentLabel ?? '').toUpperCase() === 'NEGATIVE').length,
    // 竞品提及以 competitorKeys 是否命中为准。
    competitorMentions: items.filter(item => item.competitorKeys.length > 0).length,
    // 告警只统计仍需运营处理的 OPEN 状态。
    openAlerts: alerts.filter(alert => (alert.status ?? '').toUpperCase() === 'OPEN').length,
  }
}

function normalizeOptionalUpper(value?: string) {
  const text = value?.trim()
  return text ? text.toUpperCase() : undefined
}

function normalizeOptionalKey(value?: string) {
  const text = value?.trim()
  return text ? text.toLowerCase() : undefined
}

function normalizeOptionalPositiveNumber(value?: number) {
  if (value === undefined || Number.isNaN(value) || value < 1) return undefined
  return value
}

function boundedLimit(limit?: number) {
  if (limit === undefined || Number.isNaN(limit)) return 50
  return Math.max(1, Math.min(limit, 100))
}
