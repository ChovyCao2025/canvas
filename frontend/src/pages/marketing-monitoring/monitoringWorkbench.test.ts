import { describe, expect, it } from 'vitest'

import {
  alertStatusView,
  calculateMonitoringKpis,
  credentialStatusView,
  defaultOAuthRedirectUri,
  formatMonitorDateTime,
  normalizeAlertQuery,
  normalizeCredentialEventQuery,
  normalizeCredentialQuery,
  normalizeItemQuery,
  normalizeOAuthAuthorizationQuery,
  normalizeTrendQuery,
  parseCompetitorMap,
  parseJsonObject,
  parseScopes,
  oauthAuthorizationStatusView,
  sentimentView,
  severityColor,
  type MarketingMonitorAlert,
  type MarketingMonitorItem,
} from './monitoringWorkbench'

describe('monitoringWorkbench', () => {
  it('normalizes item and alert query limits before backend calls', () => {
    expect(normalizeItemQuery({
      sentimentLabel: ' negative ',
      competitorKey: ' CompetitorX ',
      limit: 500,
    })).toEqual({
      sentimentLabel: 'NEGATIVE',
      competitorKey: 'competitorx',
      limit: 100,
    })
    expect(normalizeAlertQuery({ status: ' open ', limit: 0 })).toEqual({
      status: 'OPEN',
      limit: 1,
    })
    expect(normalizeTrendQuery({
      sourceId: 10,
      brandKey: ' Our-Brand ',
      competitorKey: ' CompetitorX ',
      limit: 500,
    })).toEqual({
      sourceId: 10,
      brandKey: 'our-brand',
      competitorKey: 'competitorx',
      limit: 100,
    })
    expect(normalizeCredentialQuery({
      providerType: ' x_recent_search ',
      authType: ' oauth2_bearer ',
      status: ' active ',
      limit: 500,
    })).toEqual({
      providerType: 'X_RECENT_SEARCH',
      authType: 'OAUTH2_BEARER',
      status: 'ACTIVE',
      limit: 100,
    })
    expect(normalizeCredentialEventQuery({
      credentialKey: ' X-Prod ',
      eventType: ' refreshed ',
      status: ' success ',
      limit: 0,
    })).toEqual({
      credentialKey: 'x-prod',
      eventType: 'REFRESHED',
      status: 'SUCCESS',
      limit: 1,
    })
    expect(normalizeOAuthAuthorizationQuery({
      credentialKey: ' X-Prod ',
      providerType: ' x_recent_search ',
      status: ' pending ',
      limit: 75,
    })).toEqual({
      credentialKey: 'x-prod',
      providerType: 'X_RECENT_SEARCH',
      status: 'PENDING',
      limit: 75,
    })
  })

  it('parses JSON objects, scopes, and competitor maps with local validation', () => {
    expect(parseJsonObject('{"owner":"brand"}')).toEqual({ owner: 'brand' })
    expect(parseJsonObject('')).toEqual({})
    expect(() => parseJsonObject('[]')).toThrow('JSON 必须是对象')
    expect(parseScopes('tweet.read users.read,offline.access tweet.read')).toEqual([
      'tweet.read',
      'users.read',
      'offline.access',
    ])
    expect(parseCompetitorMap('{"competitorx":["CompetitorX","CX"],"empty":[]}')).toEqual({
      competitorx: ['CompetitorX', 'CX'],
    })
    expect(() => parseCompetitorMap('{"bad":"CompetitorX"}')).toThrow('竞品词表必须是字符串数组')
  })

  it('formats sentiment, severity, status, and dates for operator tables', () => {
    expect(sentimentView('NEGATIVE')).toEqual({ text: '负面', color: 'red' })
    expect(sentimentView('POSITIVE')).toEqual({ text: '正面', color: 'green' })
    expect(sentimentView('UNKNOWN')).toEqual({ text: 'UNKNOWN', color: 'default' })
    expect(alertStatusView('RESOLVED')).toEqual({ text: '已处理', color: 'default' })
    expect(credentialStatusView('ACTIVE')).toEqual({ text: '启用', color: 'green' })
    expect(oauthAuthorizationStatusView('PENDING')).toEqual({ text: '待回调', color: 'gold' })
    expect(severityColor('CRITICAL')).toBe('red')
    expect(severityColor('LOW')).toBe('blue')
    expect(formatMonitorDateTime('2026-06-06T10:30:12')).toBe('2026-06-06 10:30:12')
    expect(defaultOAuthRedirectUri('/oauth/callback')).toContain('/oauth/callback')
  })

  it('calculates visible monitoring KPIs from current rows', () => {
    expect(calculateMonitoringKpis([
      item(1, 'NEGATIVE', ['competitorx']),
      item(2, 'POSITIVE', []),
      item(3, 'NEGATIVE', ['competitory']),
    ], [
      alert(401, 'OPEN'),
      alert(402, 'RESOLVED'),
    ])).toEqual({
      visibleMentions: 3,
      negativeMentions: 2,
      competitorMentions: 2,
      openAlerts: 1,
    })
  })
})

function item(id: number, sentimentLabel: string, competitorKeys: string[]): MarketingMonitorItem {
  return {
    id,
    tenantId: 7,
    sourceId: 10,
    externalItemId: `post-${id}`,
    sourceType: 'MANUAL',
    brandKey: 'our-brand',
    text: 'mention text',
    rawPayload: {},
    sentimentLabel,
    sentimentScore: sentimentLabel === 'NEGATIVE' ? -1 : 1,
    confidence: 0.8,
    competitorKeys,
  }
}

function alert(id: number, status: string): MarketingMonitorAlert {
  return {
    id,
    tenantId: 7,
    alertType: 'NEGATIVE_SENTIMENT',
    severity: 'HIGH',
    status,
    title: 'Negative sentiment detected',
    itemCount: 1,
    metadata: {},
  }
}
