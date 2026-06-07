import { afterEach, describe, expect, it, vi } from 'vitest'

import http from './api'
import { marketingMonitoringApi } from './marketingMonitoringApi'

describe('marketingMonitoringApi', () => {
  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('calls monitoring source, mention, alert, and resolve endpoints', async () => {
    const get = vi.spyOn(http, 'get').mockResolvedValue({ code: 0, message: 'success', data: [] })
    const post = vi.spyOn(http, 'post').mockResolvedValue({ code: 0, message: 'success', data: {} })

    await marketingMonitoringApi.upsertSource({
      sourceKey: 'manual-social-listening',
      sourceType: 'MANUAL',
      displayName: 'Manual Social Listening',
      enabled: true,
      metadata: { owner: 'brand-team' },
    })
    await marketingMonitoringApi.ingestItem({
      sourceId: 10,
      externalItemId: 'post-1',
      sourceUrl: 'https://example.com/post-1',
      authorKey: 'author-1',
      brandKey: 'our-brand',
      text: 'CompetitorX has bad support',
      language: 'en',
      publishedAt: '2026-06-06T10:00:00',
      competitors: { competitorx: ['CompetitorX'] },
      rawPayload: { provider: 'manual' },
    })
    await marketingMonitoringApi.listItems({
      sentimentLabel: 'NEGATIVE',
      competitorKey: 'competitorx',
      limit: 75,
    })
    await marketingMonitoringApi.listAlerts({ status: 'OPEN', limit: 50 })
    await marketingMonitoringApi.resolveAlert(401)
    await marketingMonitoringApi.configureSourcePolling(10, {
      pollEnabled: true,
      pollIntervalMinutes: 30,
      pollCursor: 'cursor-1',
      nextPollAt: '2026-06-06T11:00:00',
    })
    await marketingMonitoringApi.pollSource(10, {
      requestedFrom: '2026-06-06T10:00:00',
      requestedUntil: '2026-06-06T11:00:00',
      cursorOverride: 'cursor-1',
      maxItems: 80,
      force: true,
    })
    await marketingMonitoringApi.buildTrendSnapshot({
      sourceId: 10,
      bucketGrain: 'DAY',
      bucketStart: '2026-06-05T00:00:00',
      bucketEnd: '2026-06-06T00:00:00',
      brandKey: 'our-brand',
      competitorKey: 'competitorx',
      metadata: { source: 'manual' },
    })
    await marketingMonitoringApi.listTrendSnapshots({
      sourceId: 10,
      brandKey: 'our-brand',
      competitorKey: 'competitorx',
      limit: 50,
    })
    await marketingMonitoringApi.listProviderCredentials({
      providerType: 'X_RECENT_SEARCH',
      authType: 'OAUTH2_BEARER',
      status: 'ACTIVE',
      limit: 50,
    })
    await marketingMonitoringApi.refreshProviderCredential('x-prod')
    await marketingMonitoringApi.refreshDueProviderCredentials({ windowMinutes: 30, limit: 50 })
    await marketingMonitoringApi.revokeProviderCredential('x-prod', {
      revokeRefreshToken: true,
      tokenTypeHint: 'refresh_token',
      disableAfterRevoke: true,
      metadata: { source: 'test' },
    })
    await marketingMonitoringApi.disableProviderCredential('x-prod')
    await marketingMonitoringApi.listProviderCredentialEvents({
      credentialKey: 'x-prod',
      eventType: 'REFRESHED',
      status: 'SUCCESS',
      limit: 50,
    })
    await marketingMonitoringApi.startProviderOAuthAuthorization({
      credentialKey: 'x-prod',
      providerType: 'X_RECENT_SEARCH',
      authType: 'OAUTH2_BEARER',
      displayName: 'X Production',
      authorizeEndpoint: 'https://provider.example.test/oauth/authorize',
      tokenEndpoint: 'https://provider.example.test/oauth/token',
      revokeEndpoint: 'https://provider.example.test/oauth/revoke',
      redirectUri: 'https://canvas.example.test/marketing-monitoring',
      clientId: 'client-id',
      clientSecret: 'client-secret',
      scopes: ['tweet.read'],
      authorizeParams: { access_type: 'offline' },
      expiresInMinutes: 20,
      metadata: { owner: 'brand-team' },
    })
    await marketingMonitoringApi.completeProviderOAuthAuthorization({
      state: 'state-1',
      code: 'auth-code',
      metadata: { source: 'callback' },
    })
    await marketingMonitoringApi.listProviderOAuthAuthorizations({
      credentialKey: 'x-prod',
      providerType: 'X_RECENT_SEARCH',
      status: 'PENDING',
      limit: 50,
    })

    expect(post).toHaveBeenCalledWith('/canvas/marketing-monitoring/sources', {
      sourceKey: 'manual-social-listening',
      sourceType: 'MANUAL',
      displayName: 'Manual Social Listening',
      enabled: true,
      metadata: { owner: 'brand-team' },
    })
    expect(post).toHaveBeenCalledWith('/canvas/marketing-monitoring/items', expect.objectContaining({
      sourceId: 10,
      externalItemId: 'post-1',
      competitors: { competitorx: ['CompetitorX'] },
    }))
    expect(get).toHaveBeenCalledWith('/canvas/marketing-monitoring/items', {
      params: { sentimentLabel: 'NEGATIVE', competitorKey: 'competitorx', limit: 75 },
    })
    expect(get).toHaveBeenCalledWith('/canvas/marketing-monitoring/alerts', {
      params: { status: 'OPEN', limit: 50 },
    })
    expect(post).toHaveBeenCalledWith('/canvas/marketing-monitoring/alerts/401/resolve')
    expect(post).toHaveBeenCalledWith('/canvas/marketing-monitoring/sources/10/polling', {
      pollEnabled: true,
      pollIntervalMinutes: 30,
      pollCursor: 'cursor-1',
      nextPollAt: '2026-06-06T11:00:00',
    })
    expect(post).toHaveBeenCalledWith('/canvas/marketing-monitoring/sources/10/poll', {
      requestedFrom: '2026-06-06T10:00:00',
      requestedUntil: '2026-06-06T11:00:00',
      cursorOverride: 'cursor-1',
      maxItems: 80,
      force: true,
    })
    expect(post).toHaveBeenCalledWith('/canvas/marketing-monitoring/trends/snapshots/build', {
      sourceId: 10,
      bucketGrain: 'DAY',
      bucketStart: '2026-06-05T00:00:00',
      bucketEnd: '2026-06-06T00:00:00',
      brandKey: 'our-brand',
      competitorKey: 'competitorx',
      metadata: { source: 'manual' },
    })
    expect(get).toHaveBeenCalledWith('/canvas/marketing-monitoring/trends/snapshots', {
      params: {
        sourceId: 10,
        brandKey: 'our-brand',
        competitorKey: 'competitorx',
        limit: 50,
      },
    })
    expect(get).toHaveBeenCalledWith('/canvas/marketing-monitoring/provider-credentials', {
      params: {
        providerType: 'X_RECENT_SEARCH',
        authType: 'OAUTH2_BEARER',
        status: 'ACTIVE',
        limit: 50,
      },
    })
    expect(post).toHaveBeenCalledWith('/canvas/marketing-monitoring/provider-credentials/x-prod/refresh', {})
    expect(post).toHaveBeenCalledWith('/canvas/marketing-monitoring/provider-credentials/refresh-due', {
      windowMinutes: 30,
      limit: 50,
    })
    expect(post).toHaveBeenCalledWith('/canvas/marketing-monitoring/provider-credentials/x-prod/revoke', {
      revokeRefreshToken: true,
      tokenTypeHint: 'refresh_token',
      disableAfterRevoke: true,
      metadata: { source: 'test' },
    })
    expect(post).toHaveBeenCalledWith('/canvas/marketing-monitoring/provider-credentials/x-prod/disable')
    expect(get).toHaveBeenCalledWith('/canvas/marketing-monitoring/provider-credentials/events', {
      params: {
        credentialKey: 'x-prod',
        eventType: 'REFRESHED',
        status: 'SUCCESS',
        limit: 50,
      },
    })
    expect(post).toHaveBeenCalledWith('/canvas/marketing-monitoring/provider-credentials/oauth/authorizations',
      expect.objectContaining({
        credentialKey: 'x-prod',
        revokeEndpoint: 'https://provider.example.test/oauth/revoke',
      }),
    )
    expect(post).toHaveBeenCalledWith('/canvas/marketing-monitoring/provider-credentials/oauth/callback', {
      state: 'state-1',
      code: 'auth-code',
      metadata: { source: 'callback' },
    })
    expect(get).toHaveBeenCalledWith('/canvas/marketing-monitoring/provider-credentials/oauth/authorizations', {
      params: {
        credentialKey: 'x-prod',
        providerType: 'X_RECENT_SEARCH',
        status: 'PENDING',
        limit: 50,
      },
    })
  })
})
