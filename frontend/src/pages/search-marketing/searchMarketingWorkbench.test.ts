import { describe, expect, it } from 'vitest'

import {
  calculateSearchMarketingKpis,
  canApplyMutation,
  canDryRunMutation,
  opportunitySeverityView,
  opportunityStatusView,
  readinessStatusView,
  redactSearchMarketingSecrets,
  syncRunStatusView,
  type SearchMarketingMutation,
  type SearchMarketingOpportunity,
  type SearchMarketingSnapshot,
} from './searchMarketingWorkbench'

describe('searchMarketingWorkbench', () => {
  it('formats readiness, sync run, opportunity severity, and status labels', () => {
    expect(readinessStatusView('LIVE')).toEqual({ text: '生产就绪', color: 'green' })
    expect(readinessStatusView('DEGRADED')).toEqual({ text: '降级', color: 'gold' })
    expect(readinessStatusView('BLOCKED')).toEqual({ text: '阻断', color: 'red' })
    expect(syncRunStatusView('PARTIAL')).toEqual({ text: '部分成功', color: 'gold' })
    expect(syncRunStatusView('FAILED')).toEqual({ text: '失败', color: 'red' })
    expect(opportunitySeverityView('HIGH')).toEqual({ text: '高', color: 'red' })
    expect(opportunityStatusView('ROLLBACK_REQUIRED')).toEqual({ text: '需要回滚', color: 'red' })
  })

  it('gates dry-run and live apply from approval, dry-run, and readiness state', () => {
    expect(canDryRunMutation(mutation({ approvalStatus: 'PENDING', status: 'READY' }))).toBe(false)
    expect(canDryRunMutation(mutation({ approvalStatus: 'APPROVED', status: 'READY' }))).toBe(true)
    expect(canApplyMutation(mutation({ approvalStatus: 'APPROVED', status: 'READY' }), 'LIVE')).toBe(false)
    expect(canApplyMutation(mutation({ approvalStatus: 'APPROVED', status: 'DRY_RUN_OK' }), 'BLOCKED')).toBe(false)
    expect(canApplyMutation(mutation({ approvalStatus: 'APPROVED', status: 'DRY_RUN_OK' }), 'LIVE')).toBe(true)
    expect(canApplyMutation(mutation({
      approvalStatus: 'APPROVED',
      status: 'READY',
      dryRunRequired: false,
    }), 'LIVE')).toBe(true)
  })

  it('recursively redacts secret-shaped fields', () => {
    expect(redactSearchMarketingSecrets({
      access_token: 'raw-access',
      nested: {
        client_secret: 'raw-secret',
        apiKey: 'raw-key',
        safe: 'visible',
      },
      errors: [
        { developer_token: 'raw-dev-token', message: 'invalid bid' },
      ],
    })).toEqual({
      access_token: '[REDACTED]',
      nested: {
        client_secret: '[REDACTED]',
        apiKey: '[REDACTED]',
        safe: 'visible',
      },
      errors: [
        { developer_token: '[REDACTED]', message: 'invalid bid' },
      ],
    })
  })

  it('calculates operational KPIs from visible rows', () => {
    expect(calculateSearchMarketingKpis({
      snapshots: [
        snapshot('SEO', 100, 0, 0, 0),
        snapshot('SEM', 30, 120, 3, 600),
        snapshot('SEM', 20, 80, 1, 120),
      ],
      opportunities: [
        opportunity('OPEN'),
        opportunity('ACCEPTED'),
        opportunity('MUTED'),
      ],
      mutations: [
        mutation({ status: 'FAILED' }),
        mutation({ status: 'DRY_RUN_FAILED' }),
        mutation({ status: 'APPLIED' }),
        mutation({ status: 'RECONCILED' }),
      ],
    })).toEqual({
      seoClicks: 100,
      semSpend: 200,
      conversions: 4,
      roas: 3.6,
      openOpportunities: 2,
      failedWrites: 2,
      unreconciledWrites: 1,
    })
  })
})

function mutation(overrides: Partial<SearchMarketingMutation>): SearchMarketingMutation {
  return {
    id: 50,
    tenantId: 7,
    provider: 'GOOGLE_ADS',
    mutationKey: 'bid-raise-1',
    mutationType: 'UPDATE_KEYWORD_BID',
    entityType: 'KEYWORD',
    requestHash: 'hash',
    idempotencyKey: 'idem-1',
    status: 'READY',
    approvalStatus: 'APPROVED',
    dryRunRequired: true,
    payload: {},
    ...overrides,
  }
}

function snapshot(
  channel: string,
  clickCount: number,
  costAmount: number,
  conversionCount: number,
  revenueAmount: number,
): SearchMarketingSnapshot {
  return {
    id: 30,
    tenantId: 7,
    sourceId: 10,
    keywordId: 20,
    channel,
    snapshotDate: '2026-06-06',
    device: 'ALL',
    country: 'ALL',
    queryGroupKey: 'DEFAULT',
    impressionCount: 1000,
    clickCount,
    costAmount,
    conversionCount,
    revenueAmount,
    metadata: {},
  }
}

function opportunity(status: string): SearchMarketingOpportunity {
  return {
    id: 40,
    tenantId: 7,
    sourceId: 10,
    keywordId: 20,
    channel: 'SEM',
    opportunityType: 'LOW_CTR',
    snapshotDate: '2026-06-06',
    severity: 'HIGH',
    status,
    recommendation: 'Improve CTR',
    impactScore: 5,
    evidence: {},
  }
}
