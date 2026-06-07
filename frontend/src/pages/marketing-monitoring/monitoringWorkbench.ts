export interface MarketingMonitorSourcePayload {
  sourceKey: string
  sourceType: string
  displayName: string
  enabled: boolean
  metadata: Record<string, unknown>
}

export interface MarketingMonitorSource extends MarketingMonitorSourcePayload {
  id: number
  tenantId: number
  createdBy?: string
  createdAt?: string
  updatedAt?: string
}

export interface MarketingMonitorSourcePollingPayload {
  pollEnabled: boolean
  pollIntervalMinutes: number
  pollCursor?: string
  nextPollAt?: string
}

export interface MarketingMonitorSourcePolling {
  tenantId: number
  sourceId: number
  sourceKey: string
  sourceType: string
  pollEnabled: boolean
  pollIntervalMinutes: number
  pollCursor?: string
  lastPolledAt?: string
  nextPollAt?: string
  lastPollStatus?: string
  updatedAt?: string
}

export interface MarketingMonitorItemIngestPayload {
  sourceId: number
  externalItemId: string
  sourceUrl?: string
  authorKey?: string
  brandKey?: string
  text: string
  language?: string
  publishedAt?: string
  competitors: Record<string, string[]>
  rawPayload: Record<string, unknown>
}

export interface MarketingSentimentAnalysis {
  id: number
  tenantId: number
  itemId: number
  sentimentLabel: string
  sentimentScore: number
  confidence: number
  modelKey: string
  modelVersion: string
  keywordHits: Record<string, unknown>
  createdAt?: string
}

export interface MarketingCompetitorMention {
  id: number
  tenantId: number
  itemId: number
  competitorKey: string
  competitorName: string
  matchedTerms: string[]
  sentimentLabel: string
  sentimentScore: number
  createdAt?: string
}

export interface MarketingMonitorItem {
  id: number
  tenantId: number
  sourceId: number
  externalItemId: string
  sourceType: string
  sourceUrl?: string
  authorKey?: string
  brandKey?: string
  text: string
  language?: string
  publishedAt?: string
  ingestedAt?: string
  rawPayload: Record<string, unknown>
  sentimentLabel?: string
  sentimentScore?: number
  confidence?: number
  competitorKeys: string[]
}

export interface MarketingMonitorAlert {
  id: number
  tenantId: number
  alertType: string
  severity: string
  status: string
  scopeKey?: string
  title: string
  reason?: string
  itemCount: number
  windowStart?: string
  windowEnd?: string
  metadata: Record<string, unknown>
  createdBy?: string
  resolvedBy?: string
  resolvedAt?: string
  createdAt?: string
  updatedAt?: string
}

export interface MarketingMonitorPollPayload {
  requestedFrom?: string
  requestedUntil?: string
  cursorOverride?: string
  maxItems: number
  force: boolean
}

export interface MarketingMonitorPollRun {
  id: number
  tenantId: number
  sourceId: number
  sourceKey: string
  sourceType: string
  status: string
  requestedFrom?: string
  requestedUntil?: string
  cursorBefore?: string
  cursorAfter?: string
  itemCount: number
  insertedCount: number
  duplicateCount: number
  alertCount: number
  errorMessage?: string
  metadata: Record<string, unknown>
  createdBy?: string
  startedAt?: string
  finishedAt?: string
  createdAt?: string
  updatedAt?: string
}

export interface MarketingMonitorTrendSnapshotPayload {
  sourceId: number
  bucketGrain: string
  bucketStart: string
  bucketEnd: string
  brandKey?: string
  competitorKey?: string
  metadata: Record<string, unknown>
}

export interface MarketingMonitorTrendSnapshotQuery {
  sourceId?: number
  brandKey?: string
  competitorKey?: string
  limit?: number
}

export interface MarketingMonitorTrendSnapshot {
  id: number
  tenantId: number
  sourceId: number
  sourceKey: string
  bucketGrain: string
  bucketStart: string
  bucketEnd: string
  brandKey?: string
  competitorKey?: string
  mentionCount: number
  positiveCount: number
  neutralCount: number
  negativeCount: number
  competitorCount: number
  alertCount: number
  avgSentimentScore?: number
  metadata: Record<string, unknown>
  createdBy?: string
  createdAt?: string
  updatedAt?: string
}

export interface MarketingMonitorProviderCredential {
  id: number
  tenantId: number
  credentialKey: string
  providerType: string
  authType: string
  displayName: string
  status: string
  tokenType?: string
  scopes: string[]
  accessTokenPrefix?: string
  refreshTokenPrefix?: string
  apiKeyPrefix?: string
  refreshEndpoint?: string
  revokeEndpoint?: string
  expiresAt?: string
  refreshTokenExpiresAt?: string
  revokedAt?: string
  lastRefreshedAt?: string
  refreshAttemptCount: number
  lastRefreshStatus?: string
  lastRefreshError?: string
  lastRevokeStatus?: string
  lastRevokeError?: string
  metadata: Record<string, unknown>
  createdBy?: string
  updatedBy?: string
  createdAt?: string
  updatedAt?: string
}

export interface MarketingMonitorProviderCredentialQuery {
  providerType?: string
  authType?: string
  status?: string
  limit?: number
}

export interface MarketingMonitorProviderCredentialEvent {
  id: number
  tenantId: number
  credentialId: number
  credentialKey: string
  eventType: string
  status: string
  metadata: Record<string, unknown>
  errorMessage?: string
  createdBy?: string
  createdAt?: string
}

export interface MarketingMonitorProviderCredentialEventQuery {
  credentialKey?: string
  eventType?: string
  status?: string
  limit?: number
}

export interface MarketingMonitorProviderCredentialDueRefreshCommand {
  windowMinutes?: number
  limit?: number
}

export interface MarketingMonitorProviderCredentialDueRefreshResult {
  tenantId: number
  candidateCount: number
  dueCount: number
  refreshedCount: number
  failedCount: number
  skippedCount: number
  cutoffAt?: string
  evaluatedAt?: string
  credentials: MarketingMonitorProviderCredential[]
}

export interface MarketingMonitorProviderCredentialRevokeCommand {
  revokeEndpoint?: string
  tokenTypeHint?: string
  revokeRefreshToken?: boolean
  disableAfterRevoke?: boolean
  metadata?: Record<string, unknown>
}

export interface MarketingMonitorProviderOAuthAuthorizationCommand {
  credentialKey: string
  providerType: string
  authType: string
  displayName: string
  authorizeEndpoint: string
  tokenEndpoint: string
  revokeEndpoint?: string
  redirectUri: string
  clientId: string
  clientSecret?: string
  scopes: string[]
  authorizeParams: Record<string, unknown>
  expiresInMinutes?: number
  metadata: Record<string, unknown>
}

export interface MarketingMonitorProviderOAuthCallbackCommand {
  state: string
  code?: string
  error?: string
  errorDescription?: string
  metadata: Record<string, unknown>
}

export interface MarketingMonitorProviderOAuthAuthorization {
  id: number
  tenantId: number
  authState: string
  credentialKey: string
  providerType: string
  authType: string
  displayName: string
  status: string
  authorizationUrl?: string
  authorizeEndpoint: string
  tokenEndpoint: string
  redirectUri: string
  scopes: string[]
  codeChallengeMethod?: string
  credentialId?: number
  providerError?: string
  providerErrorDescription?: string
  lastHttpStatus?: number
  lastErrorMessage?: string
  expiresAt?: string
  completedAt?: string
  metadata: Record<string, unknown>
  createdBy?: string
  updatedBy?: string
  createdAt?: string
  updatedAt?: string
}

export interface MarketingMonitorProviderOAuthAuthorizationQuery {
  credentialKey?: string
  providerType?: string
  status?: string
  limit?: number
}

export interface MarketingMonitorIngestResult {
  item: MarketingMonitorItem
  sentiment: MarketingSentimentAnalysis
  competitorMentions: MarketingCompetitorMention[]
  alerts: MarketingMonitorAlert[]
}

export interface MarketingMonitorItemQuery {
  sentimentLabel?: string
  competitorKey?: string
  limit?: number
}

export interface MarketingMonitorAlertQuery {
  status?: string
  limit?: number
}

export interface MonitoringKpis {
  visibleMentions: number
  negativeMentions: number
  competitorMentions: number
  openAlerts: number
}

export function normalizeItemQuery(filters: MarketingMonitorItemQuery = {}): MarketingMonitorItemQuery {
  return {
    sentimentLabel: normalizeOptionalUpper(filters.sentimentLabel),
    competitorKey: normalizeOptionalKey(filters.competitorKey),
    limit: boundedLimit(filters.limit),
  }
}

export function normalizeAlertQuery(filters: MarketingMonitorAlertQuery = {}): MarketingMonitorAlertQuery {
  return {
    status: normalizeOptionalUpper(filters.status),
    limit: boundedLimit(filters.limit),
  }
}

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

export function parseJsonObject(value?: string): Record<string, unknown> {
  const text = value?.trim()
  if (!text) return {}
  const parsed = JSON.parse(text) as unknown
  if (!parsed || typeof parsed !== 'object' || Array.isArray(parsed)) {
    throw new Error('JSON 必须是对象')
  }
  return parsed as Record<string, unknown>
}

export function parseScopes(value?: string): string[] {
  return (value ?? '')
    .split(/[\s,]+/)
    .map(scope => scope.trim())
    .filter((scope, index, all) => scope.length > 0 && all.indexOf(scope) === index)
}

export function defaultOAuthRedirectUri(pathname = '/marketing-monitoring'): string {
  if (typeof window === 'undefined') return pathname
  return `${window.location.origin}${pathname}`
}

export function parseCompetitorMap(value?: string): Record<string, string[]> {
  const parsed = parseJsonObject(value)
  const result: Record<string, string[]> = {}
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

export function credentialStatusView(status?: string | null): { text: string; color: string } {
  const views: Record<string, { text: string; color: string }> = {
    ACTIVE: { text: '启用', color: 'green' },
    DISABLED: { text: '停用', color: 'default' },
  }
  const key = (status ?? '').toUpperCase()
  return views[key] ?? { text: status || '-', color: 'default' }
}

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

export function sentimentView(status?: string | null): { text: string; color: string } {
  const views: Record<string, { text: string; color: string }> = {
    NEGATIVE: { text: '负面', color: 'red' },
    POSITIVE: { text: '正面', color: 'green' },
    NEUTRAL: { text: '中性', color: 'blue' },
  }
  const key = (status ?? '').toUpperCase()
  return views[key] ?? { text: status || '-', color: 'default' }
}

export function alertStatusView(status?: string | null): { text: string; color: string } {
  const views: Record<string, { text: string; color: string }> = {
    OPEN: { text: '待处理', color: 'red' },
    RESOLVED: { text: '已处理', color: 'default' },
  }
  const key = (status ?? '').toUpperCase()
  return views[key] ?? { text: status || '-', color: 'default' }
}

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

export function formatMonitorDateTime(value?: string | null) {
  if (!value) return '-'
  return value.replace('T', ' ').slice(0, 19)
}

export function calculateMonitoringKpis(
  items: MarketingMonitorItem[] = [],
  alerts: MarketingMonitorAlert[] = [],
): MonitoringKpis {
  return {
    visibleMentions: items.length,
    negativeMentions: items.filter(item => (item.sentimentLabel ?? '').toUpperCase() === 'NEGATIVE').length,
    competitorMentions: items.filter(item => item.competitorKeys.length > 0).length,
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
