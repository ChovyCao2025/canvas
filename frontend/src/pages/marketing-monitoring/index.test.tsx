/* @vitest-environment jsdom */
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import MarketingMonitoringPage from './index'
import { marketingMonitoringApi } from '../../services/marketingMonitoringApi'

vi.mock('../../services/marketingMonitoringApi', () => ({
  marketingMonitoringApi: {
    buildTrendSnapshot: vi.fn(),
    completeProviderOAuthAuthorization: vi.fn(),
    configureSourcePolling: vi.fn(),
    disableProviderCredential: vi.fn(),
    ingestItem: vi.fn(),
    listAlerts: vi.fn(),
    listProviderCredentialEvents: vi.fn(),
    listProviderCredentials: vi.fn(),
    listProviderOAuthAuthorizations: vi.fn(),
    listItems: vi.fn(),
    listTrendSnapshots: vi.fn(),
    pollSource: vi.fn(),
    refreshDueProviderCredentials: vi.fn(),
    refreshProviderCredential: vi.fn(),
    revokeProviderCredential: vi.fn(),
    resolveAlert: vi.fn(),
    startProviderOAuthAuthorization: vi.fn(),
    upsertSource: vi.fn(),
  },
}))

const item = {
  id: 100,
  tenantId: 7,
  sourceId: 10,
  externalItemId: 'post-1',
  sourceType: 'MANUAL',
  sourceUrl: 'https://example.com/post-1',
  authorKey: 'author-1',
  brandKey: 'our-brand',
  text: 'CompetitorX has bad support',
  language: 'en',
  publishedAt: '2026-06-06T10:00:00',
  ingestedAt: '2026-06-06T10:01:00',
  rawPayload: { provider: 'manual' },
  sentimentLabel: 'NEGATIVE',
  sentimentScore: -1,
  confidence: 0.8,
  competitorKeys: ['competitorx'],
}

const alert = {
  id: 401,
  tenantId: 7,
  alertType: 'NEGATIVE_SENTIMENT',
  severity: 'HIGH',
  status: 'OPEN',
  scopeKey: 'our-brand',
  title: 'Negative sentiment detected',
  reason: 'Detected negative sentiment',
  itemCount: 1,
  windowStart: '2026-06-06T10:00:00',
  windowEnd: '2026-06-06T10:00:00',
  metadata: { itemId: 100 },
  createdBy: 'operator-1',
  createdAt: '2026-06-06T10:01:00',
  updatedAt: '2026-06-06T10:01:00',
}

const trendSnapshot = {
  id: 601,
  tenantId: 7,
  sourceId: 10,
  sourceKey: 'manual-social-listening',
  bucketGrain: 'DAY',
  bucketStart: '2026-06-05T00:00:00',
  bucketEnd: '2026-06-06T00:00:00',
  brandKey: 'our-brand',
  competitorKey: 'competitorx',
  mentionCount: 9,
  positiveCount: 3,
  neutralCount: 2,
  negativeCount: 4,
  competitorCount: 5,
  alertCount: 2,
  avgSentimentScore: -0.125,
  metadata: { source: 'manual' },
  createdBy: 'operator-1',
  createdAt: '2026-06-06T10:01:00',
  updatedAt: '2026-06-06T10:01:00',
}

const credential = {
  id: 700,
  tenantId: 7,
  credentialKey: 'x-prod',
  providerType: 'X_RECENT_SEARCH',
  authType: 'OAUTH2_BEARER',
  displayName: 'X Production',
  status: 'ACTIVE',
  tokenType: 'Bearer',
  scopes: ['tweet.read'],
  accessTokenPrefix: 'access-toke',
  refreshTokenPrefix: 'refresh-tok',
  apiKeyPrefix: undefined,
  refreshEndpoint: 'https://provider.example.test/oauth/token',
  revokeEndpoint: 'https://provider.example.test/oauth/revoke',
  expiresAt: '2026-06-06T11:00:00',
  refreshTokenExpiresAt: '2026-07-06T10:00:00',
  revokedAt: undefined,
  lastRefreshedAt: '2026-06-06T10:00:00',
  refreshAttemptCount: 1,
  lastRefreshStatus: 'SUCCESS',
  lastRefreshError: undefined,
  lastRevokeStatus: undefined,
  lastRevokeError: undefined,
  metadata: { owner: 'brand-team' },
  createdBy: 'operator-1',
  updatedBy: 'operator-1',
  createdAt: '2026-06-06T09:00:00',
  updatedAt: '2026-06-06T10:00:00',
}

const credentialEvent = {
  id: 701,
  tenantId: 7,
  credentialId: 700,
  credentialKey: 'x-prod',
  eventType: 'REFRESHED',
  status: 'SUCCESS',
  metadata: { httpStatus: 200 },
  createdBy: 'operator-1',
  createdAt: '2026-06-06T10:00:00',
}

const oauthAuthorization = {
  id: 800,
  tenantId: 7,
  authState: 'state-1',
  credentialKey: 'x-prod',
  providerType: 'X_RECENT_SEARCH',
  authType: 'OAUTH2_BEARER',
  displayName: 'X Production',
  status: 'PENDING',
  authorizationUrl: 'https://provider.example.test/oauth/authorize?state=state-1',
  authorizeEndpoint: 'https://provider.example.test/oauth/authorize',
  tokenEndpoint: 'https://provider.example.test/oauth/token',
  redirectUri: 'http://localhost:3000/marketing-monitoring',
  scopes: ['tweet.read'],
  codeChallengeMethod: 'S256',
  credentialId: undefined,
  providerError: undefined,
  providerErrorDescription: undefined,
  lastHttpStatus: undefined,
  lastErrorMessage: undefined,
  expiresAt: '2026-06-06T10:20:00',
  completedAt: undefined,
  metadata: { owner: 'brand-team' },
  createdBy: 'operator-1',
  updatedBy: 'operator-1',
  createdAt: '2026-06-06T10:00:00',
  updatedAt: '2026-06-06T10:00:00',
}

describe('MarketingMonitoringPage', () => {
  const openSpy = vi.fn()

  beforeEach(() => {
    vi.clearAllMocks()
    vi.stubGlobal('open', openSpy)
    vi.mocked(marketingMonitoringApi.listItems).mockResolvedValue({
      code: 0,
      message: 'success',
      data: [item],
    })
    vi.mocked(marketingMonitoringApi.listAlerts).mockResolvedValue({
      code: 0,
      message: 'success',
      data: [alert],
    })
    vi.mocked(marketingMonitoringApi.listTrendSnapshots).mockResolvedValue({
      code: 0,
      message: 'success',
      data: [trendSnapshot],
    })
    vi.mocked(marketingMonitoringApi.listProviderCredentials).mockResolvedValue({
      code: 0,
      message: 'success',
      data: [credential],
    })
    vi.mocked(marketingMonitoringApi.listProviderCredentialEvents).mockResolvedValue({
      code: 0,
      message: 'success',
      data: [credentialEvent],
    })
    vi.mocked(marketingMonitoringApi.listProviderOAuthAuthorizations).mockResolvedValue({
      code: 0,
      message: 'success',
      data: [oauthAuthorization],
    })
    vi.mocked(marketingMonitoringApi.buildTrendSnapshot).mockResolvedValue({
      code: 0,
      message: 'success',
      data: trendSnapshot,
    })
    vi.mocked(marketingMonitoringApi.startProviderOAuthAuthorization).mockResolvedValue({
      code: 0,
      message: 'success',
      data: oauthAuthorization,
    })
    vi.mocked(marketingMonitoringApi.completeProviderOAuthAuthorization).mockResolvedValue({
      code: 0,
      message: 'success',
      data: { ...oauthAuthorization, status: 'EXCHANGED', credentialId: 700 },
    })
    vi.mocked(marketingMonitoringApi.resolveAlert).mockResolvedValue({
      code: 0,
      message: 'success',
      data: { ...alert, status: 'RESOLVED', resolvedBy: 'operator-1', resolvedAt: '2026-06-06T10:05:00' },
    })
  }, 10000)

  it('loads mentions and alerts and resolves an open alert', async () => {
    render(<MarketingMonitoringPage />)

    expect(screen.getByRole('heading', { name: '监测工作台' })).toBeInTheDocument()
    await waitFor(() => expect(marketingMonitoringApi.listItems).toHaveBeenCalledWith({ limit: 50 }))
    await waitFor(() => expect(marketingMonitoringApi.listAlerts).toHaveBeenCalledWith({ status: 'OPEN', limit: 50 }))
    expect(await screen.findByText('CompetitorX has bad support')).toBeInTheDocument()
    expect(screen.getByText('Negative sentiment detected')).toBeInTheDocument()
    expect(screen.getByText('待处理告警')).toBeInTheDocument()
    await waitFor(() => expect(marketingMonitoringApi.listTrendSnapshots).toHaveBeenCalledWith({ limit: 50 }))
    expect(screen.getByText('趋势快照')).toBeInTheDocument()
    expect(screen.getByText('manual-social-listening / 10')).toBeInTheDocument()
    expect(screen.getByText('9')).toBeInTheDocument()
    expect(screen.getByText('-0.125')).toBeInTheDocument()
    await waitFor(() => expect(marketingMonitoringApi.listProviderCredentials).toHaveBeenCalledWith({
      status: 'ACTIVE',
      limit: 50,
    }))
    expect(screen.getByText('Provider 凭据')).toBeInTheDocument()
    expect(screen.getAllByText('X Production').length).toBeGreaterThan(0)
    expect(screen.getAllByText('x-prod').length).toBeGreaterThan(0)
    expect(screen.getByText('REFRESHED')).toBeInTheDocument()

    fireEvent.click(screen.getByRole('button', { name: '处理' }))

    await waitFor(() => expect(marketingMonitoringApi.resolveAlert).toHaveBeenCalledWith(401))
    await waitFor(() => expect(marketingMonitoringApi.listAlerts).toHaveBeenCalledTimes(2))
  })

  it('builds a trend snapshot from the workbench and refreshes trends', async () => {
    render(<MarketingMonitoringPage />)

    await waitFor(() => expect(marketingMonitoringApi.listTrendSnapshots).toHaveBeenCalledWith({ limit: 50 }))

    fireEvent.change(screen.getByLabelText('快照来源 ID'), { target: { value: '10' } })
    fireEvent.change(screen.getByLabelText('开始时间'), { target: { value: '2026-06-05T00:00:00' } })
    fireEvent.change(screen.getByLabelText('结束时间'), { target: { value: '2026-06-06T00:00:00' } })
    fireEvent.change(screen.getByLabelText('快照品牌 Key'), { target: { value: 'our-brand' } })
    fireEvent.change(screen.getByLabelText('快照竞品 Key'), { target: { value: 'competitorx' } })
    fireEvent.change(screen.getByLabelText('快照元数据 JSON'), { target: { value: '{"source":"manual"}' } })
    fireEvent.click(screen.getByRole('button', { name: '生成快照' }))

    await waitFor(() => expect(marketingMonitoringApi.buildTrendSnapshot).toHaveBeenCalledWith({
      sourceId: 10,
      bucketGrain: 'DAY',
      bucketStart: '2026-06-05T00:00:00',
      bucketEnd: '2026-06-06T00:00:00',
      brandKey: 'our-brand',
      competitorKey: 'competitorx',
      metadata: { source: 'manual' },
    }))
    await waitFor(() => expect(marketingMonitoringApi.listTrendSnapshots).toHaveBeenCalledTimes(2))
  })

  it('starts provider OAuth authorization and opens the returned URL', async () => {
    render(<MarketingMonitoringPage />)

    await waitFor(() => expect(marketingMonitoringApi.listProviderCredentials).toHaveBeenCalled())

    fireEvent.change(screen.getByLabelText('凭据 Key'), { target: { value: 'x-prod' } })
    fireEvent.change(screen.getByLabelText('显示名称'), { target: { value: 'X Production' } })
    fireEvent.change(screen.getByLabelText('Client ID'), { target: { value: 'client-id' } })
    fireEvent.change(screen.getByLabelText('Authorize Endpoint'), {
      target: { value: 'https://provider.example.test/oauth/authorize' },
    })
    fireEvent.change(screen.getByLabelText('Token Endpoint'), {
      target: { value: 'https://provider.example.test/oauth/token' },
    })
    fireEvent.change(screen.getByLabelText('Revoke Endpoint'), {
      target: { value: 'https://provider.example.test/oauth/revoke' },
    })
    fireEvent.click(screen.getByRole('button', { name: '创建授权' }))

    await waitFor(() => expect(marketingMonitoringApi.startProviderOAuthAuthorization).toHaveBeenCalledWith(
      expect.objectContaining({
        credentialKey: 'x-prod',
        providerType: 'X_RECENT_SEARCH',
        authType: 'OAUTH2_BEARER',
        displayName: 'X Production',
        clientId: 'client-id',
        revokeEndpoint: 'https://provider.example.test/oauth/revoke',
        scopes: ['tweet.read', 'users.read', 'offline.access'],
      }),
    ))
    expect(openSpy).toHaveBeenCalledWith(
      'https://provider.example.test/oauth/authorize?state=state-1',
      '_blank',
      'noopener,noreferrer',
    )
    expect(screen.getByDisplayValue('state-1')).toBeInTheDocument()
  })
})
