import { describe, expect, it } from 'vitest'

import {
  buildProviderWriteQueue,
  calculateControlPlaneKpis,
  calculateProviderWriteKpis,
  evidenceStatusColor,
  evidenceStatusText,
  laneStatusColor,
  providerWriteActionState,
  readinessPercent,
  statusText,
  type MarketingPlatformControlPlaneSummary,
} from './marketingPlatformControlPlane'

describe('marketingPlatformControlPlane', () => {
  it('calculates readiness KPIs from backend summary counts', () => {
    expect(calculateControlPlaneKpis(summary)).toEqual({
      capabilityCount: 4,
      liveCapabilities: 2,
      configurationRequired: 2,
      actionCount: 2,
      readinessPercent: 50,
      readinessGateStatus: 'BLOCKED',
      blockerCount: 1,
      warningCount: 1,
    })
  })

  it('formats readiness and lane statuses for stable operator labels', () => {
    expect(statusText('LIVE')).toBe('已上线')
    expect(statusText('CONFIGURATION_REQUIRED')).toBe('需配置')
    expect(statusText('API_ONLY')).toBe('API 就绪')
    expect(statusText('BLOCKED')).toBe('阻断')
    expect(statusText('DEGRADED')).toBe('降级可上线')
    expect(laneStatusColor('GOVERNED')).toBe('green')
    expect(laneStatusColor('CONFIGURATION_REQUIRED')).toBe('gold')
    expect(evidenceStatusText('PRESENT')).toBe('有证据')
    expect(evidenceStatusColor('MISSING')).toBe('gold')
    expect(readinessPercent(7, 10)).toBe(70)
    expect(readinessPercent(0, 0)).toBe(0)
  })

  it('normalizes provider write mutations and calculates action gates', () => {
    const queue = buildProviderWriteQueue({
      search: [mutation('SEARCH_MARKETING', 1, 'DRAFT', 'PENDING', '2026-06-06T10:00:00')],
      creator: [mutation('CREATOR', 2, 'DRY_RUN_OK', 'APPROVED', '2026-06-06T11:00:00')],
      dsp: [mutation('PROGRAMMATIC_DSP', 3, 'FAILED', 'APPROVED', '2026-06-06T09:00:00')],
    })

    expect(queue.map(item => item.gateway)).toEqual(['CREATOR', 'SEARCH_MARKETING', 'PROGRAMMATIC_DSP'])
    expect(calculateProviderWriteKpis(queue)).toEqual({
      total: 3,
      pendingApproval: 1,
      ready: 0,
      dryRunOk: 1,
      failed: 1,
    })
    expect(providerWriteActionState(queue[1])).toMatchObject({
      canApprove: true,
      canDryRun: false,
      canApply: false,
    })
    expect(providerWriteActionState(queue[0])).toMatchObject({
      canApprove: false,
      canDryRun: true,
      canApply: true,
    })
  })
})

const summary: MarketingPlatformControlPlaneSummary = {
  tenantId: 7,
  generatedAt: '2026-06-06T10:00',
  overallStatus: 'CONFIGURATION_REQUIRED',
  capabilityCount: 4,
  liveCapabilityCount: 2,
  actionItemCount: 2,
  capabilities: [
    capability('journey-orchestration', 'LIVE'),
    capability('content-lifecycle', 'LIVE'),
    capability('paid-media-activation', 'CONFIGURATION_REQUIRED'),
    capability('search-marketing-governance', 'API_ONLY'),
  ],
  integrationLanes: [],
  integrationAssets: [
    {
      assetKey: 'search-provider-write-gateway',
      displayName: 'Search Provider Write Gateway',
      assetType: 'OUTBOUND_WRITE',
      ownerCapabilityKey: 'search-marketing-governance',
      providerFamily: 'SEM',
      status: 'API_ONLY',
      apiRoot: '/canvas/search-marketing/mutations',
      credentialDependency: 'active provider credential',
      pendingWrites: 1,
      failedWrites: 0,
      controls: ['approval gate'],
      gaps: ['wire live SEM write client'],
      evidence: [
        {
          signalKey: 'searchProviderMutations',
          label: 'Provider mutations',
          value: 1,
          status: 'PRESENT',
        },
      ],
    },
  ],
  readinessGate: {
    status: 'BLOCKED',
    productionReady: false,
    blockerCount: 1,
    warningCount: 1,
    blockers: [
      {
        severity: 'BLOCKER',
        itemType: 'CAPABILITY',
        itemKey: 'paid-media-activation',
        title: 'Configure Paid Media Activation',
        route: '/audiences',
        reason: 'requires provider configuration',
      },
    ],
    warnings: [
      {
        severity: 'WARNING',
        itemType: 'CAPABILITY',
        itemKey: 'search-marketing-governance',
        title: 'Search Marketing Governance is API-only',
        route: '/marketing-platform',
        reason: 'wire live SEM write client',
      },
    ],
  },
  actionItems: [
    {
      priority: 'HIGH',
      capabilityKey: 'paid-media-activation',
      title: 'Configure provider destinations',
      route: '/audiences',
      reason: 'Real provider activation depends on destination credentials',
    },
    {
      priority: 'MEDIUM',
      capabilityKey: 'search-marketing-governance',
      title: 'Wire live SEM write clients',
      route: '/canvas/search-marketing',
      reason: 'The gateway is governed but provider clients are environment-specific',
    },
  ],
}

function capability(capabilityKey: string, status: string) {
  return {
    capabilityKey,
    displayName: capabilityKey,
    domain: 'marketing',
    status,
    route: '/marketing-platform',
    apiRoot: '/canvas/marketing-platform',
    surface: 'operator-facing',
    productionSignals: ['tenant scoped'],
    gaps: status === 'LIVE' ? [] : ['requires provider configuration'],
    evidence: [
      {
        signalKey: `${capabilityKey}-count`,
        label: `${capabilityKey} count`,
        value: status === 'LIVE' ? 1 : 0,
        status: status === 'LIVE' ? 'PRESENT' : 'MISSING',
      },
    ],
  }
}

function mutation(gateway: string, id: number, status: string, approvalStatus: string, updatedAt: string) {
  return {
    id,
    provider: gateway === 'PROGRAMMATIC_DSP' ? 'DV360' : 'GOOGLE_ADS',
    mutationKey: `mutation-${id}`,
    mutationType: gateway === 'CREATOR' ? 'REQUEST_CONTENT_AUTHORIZATION' : 'UPDATE_LINE_ITEM_BID',
    entityType: gateway === 'CREATOR' ? 'DELIVERABLE' : 'LINE_ITEM',
    externalEntityId: `external-${id}`,
    requestHash: `hash-${id}`,
    idempotencyKey: `idem-${id}`,
    status,
    approvalStatus,
    dryRunRequired: true,
    payload: { value: id },
    validation: {},
    providerRequest: {},
    providerResponse: {},
    createdBy: 'operator-1',
    createdAt: '2026-06-06T08:00:00',
    updatedAt,
  }
}
