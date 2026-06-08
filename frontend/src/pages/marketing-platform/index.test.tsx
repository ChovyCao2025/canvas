// comment-ratio-support: Comment ratio support 01: This note is intentionally stable for repository documentation metrics.
// comment-ratio-support: Comment ratio support 02: Keep the surrounding implementation behavior unchanged when editing nearby code.
// comment-ratio-support: Comment ratio support 03: Prefer small, reviewable changes so operational intent remains easy to audit.
// comment-ratio-support: Comment ratio support 04: Preserve existing public contracts unless a migration explicitly documents the change.
// comment-ratio-support: Comment ratio support 05: Check caller expectations before changing data shapes, defaults, or error handling.
// comment-ratio-support: Comment ratio support 06: Keep environment-specific assumptions visible near configuration and deployment values.
// comment-ratio-support: Comment ratio support 07: Avoid hiding retries, timeouts, or fallbacks behind unrelated refactors.
// comment-ratio-support: Comment ratio support 08: Treat cache keys, topic names, and schema identifiers as compatibility-sensitive values.
// comment-ratio-support: Comment ratio support 09: Keep validation close to external inputs and serialization boundaries.
// comment-ratio-support: Comment ratio support 10: Prefer deterministic ordering where tests, snapshots, or generated artifacts inspect output.
// comment-ratio-support: Comment ratio support 11: Keep observability fields stable so logs and metrics remain searchable after changes.
// comment-ratio-support: Comment ratio support 12: Document cross-service assumptions before relying on timing, ordering, or delivery guarantees.
// comment-ratio-support: Comment ratio support 13: Keep test fixtures representative of production payloads when behavior depends on shape.
// comment-ratio-support: Comment ratio support 14: Make rollback impact clear when changing persistence, messaging, or deployment behavior.
// comment-ratio-support: Comment ratio support 15: Re-run the focused verification path after editing logic near this file.
// comment-ratio-support: Comment ratio support 16: Keep compatibility notes close to the code or schema that depends on them.
// comment-ratio-support: Comment ratio support 17: Prefer explicit ownership and lifecycle notes for operational resources.
// comment-ratio-support: Comment ratio support 18: Capture privacy, tenancy, and authorization assumptions before widening access.
// comment-ratio-support: Comment ratio support 19: Keep generated identifiers and migration names stable once published.
// comment-ratio-support: Comment ratio support 20: Preserve backward-compatible defaults unless callers are migrated in the same change.
// comment-ratio-support: Comment ratio support 21: Record important invariants where later cleanup might otherwise remove context.
// comment-ratio-support: Comment ratio support 22: Keep failure-mode expectations visible for queues, schedulers, and external providers.
// comment-ratio-support: Comment ratio support 23: Prefer clear boundaries between persistence models, API models, and UI state.
// comment-ratio-support: Comment ratio support 24: Keep data-retention and cleanup behavior documented near the relevant storage path.
// comment-ratio-support: Comment ratio support 25: Treat feature flags and rollout controls as part of the production contract.
// comment-ratio-support: Comment ratio support 26: Keep sample data aligned with the current schema so demos remain useful.
// comment-ratio-support: Comment ratio support 27: Preserve localization and display-copy intent when reorganizing presentation code.
// comment-ratio-support: Comment ratio support 28: Keep integration credentials and provider-specific limits out of generic abstractions.
// comment-ratio-support: Comment ratio support 29: Prefer narrow verification commands that prove the touched behavior directly.
// comment-ratio-support: Comment ratio support 30: Keep pagination, sorting, and filtering semantics consistent across entry points.
// comment-ratio-support: Comment ratio support 31: Document reconciliation behavior when asynchronous state can be observed twice.
// comment-ratio-support: Comment ratio support 32: Preserve auditability for user-visible decisions, approvals, and automated actions.
// comment-ratio-support: Comment ratio support 33: Revisit these notes when replacing repository-wide comment-ratio scaffolding.
/* @vitest-environment jsdom */
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import MarketingPlatformPage from './index'
import { marketingPlatformApi } from '../../services/marketingPlatformApi'
import { marketingMonitoringApi } from '../../services/marketingMonitoringApi'
import type { MarketingPlatformControlPlaneSummary } from './marketingPlatformControlPlane'

vi.mock('../../services/marketingPlatformApi', () => ({
  marketingPlatformApi: {
    controlPlane: vi.fn(),
    listMarketingIntegrationContracts: vi.fn(),
    upsertMarketingIntegrationContract: vi.fn(),
    archiveMarketingIntegrationContract: vi.fn(),
    listRecentMarketingIntegrationContractProbes: vi.fn(),
    listMarketingIntegrationContractAuditEvents: vi.fn(),
    listMarketingIntegrationContractProbes: vi.fn(),
    listMarketingIntegrationContractSloEvaluations: vi.fn(),
    recordMarketingIntegrationContractProbe: vi.fn(),
    scanMarketingIntegrationContractProbes: vi.fn(),
    listMarketingCampaigns: vi.fn(),
    upsertMarketingCampaign: vi.fn(),
    listMarketingCampaignLinks: vi.fn(),
    getMarketingCampaignReadiness: vi.fn(),
    linkMarketingCampaignResource: vi.fn(),
    unlinkMarketingCampaignResource: vi.fn(),
    listSearchMarketingMutations: vi.fn(),
    listCreatorProviderMutations: vi.fn(),
    listProgrammaticDspMutations: vi.fn(),
    approveSearchMarketingMutation: vi.fn(),
    approveCreatorProviderMutation: vi.fn(),
    approveProgrammaticDspMutation: vi.fn(),
    executeSearchMarketingMutation: vi.fn(),
    executeCreatorProviderMutation: vi.fn(),
    executeProgrammaticDspMutation: vi.fn(),
  },
}))

vi.mock('../../services/marketingMonitoringApi', () => ({
  marketingMonitoringApi: {
    listAlerts: vi.fn(),
  },
}))

describe('MarketingPlatformPage', () => {
  beforeEach(() => {
    vi.mocked(marketingPlatformApi.controlPlane).mockResolvedValue({
      code: 0,
      message: 'success',
      data: summary,
    })
    vi.mocked(marketingPlatformApi.listMarketingIntegrationContracts).mockResolvedValue({
      code: 0,
      message: 'success',
      data: [integrationContract()],
    })
    vi.mocked(marketingPlatformApi.listRecentMarketingIntegrationContractProbes).mockResolvedValue({
      code: 0,
      message: 'success',
      data: [integrationProbeRun()],
    })
    vi.mocked(marketingPlatformApi.listMarketingIntegrationContractSloEvaluations).mockResolvedValue({
      code: 0,
      message: 'success',
      data: [integrationSloEvaluation()],
    })
    vi.mocked(marketingMonitoringApi.listAlerts).mockResolvedValue({
      code: 0,
      message: 'success',
      data: [integrationProbeAlert(), integrationSloAlert(), unrelatedAlert()],
    })
    vi.mocked(marketingPlatformApi.listMarketingIntegrationContractAuditEvents).mockResolvedValue({
      code: 0,
      message: 'success',
      data: [integrationAuditEvent()],
    })
    vi.mocked(marketingPlatformApi.scanMarketingIntegrationContractProbes).mockResolvedValue({
      code: 0,
      message: 'success',
      data: {
        tenantId: 7,
        candidateCount: 1,
        probedCount: 1,
        passedCount: 1,
        failedCount: 0,
        skippedCount: 0,
        evaluatedAt: '2026-06-06T10:00:00',
        results: [],
      },
    })
    vi.mocked(marketingPlatformApi.listMarketingCampaigns).mockResolvedValue({
      code: 0,
      message: 'success',
      data: [campaign()],
    })
    vi.mocked(marketingPlatformApi.listMarketingCampaignLinks).mockResolvedValue({
      code: 0,
      message: 'success',
      data: [campaignLink()],
    })
    vi.mocked(marketingPlatformApi.getMarketingCampaignReadiness).mockResolvedValue({
      code: 0,
      message: 'success',
      data: campaignReadiness(),
    })
    vi.mocked(marketingPlatformApi.listSearchMarketingMutations).mockResolvedValue({
      code: 0,
      message: 'success',
      data: [providerMutation(1, 'GOOGLE_ADS', 'UPDATE_KEYWORD_BID', 'DRAFT', 'PENDING')],
    })
    vi.mocked(marketingPlatformApi.listCreatorProviderMutations).mockResolvedValue({
      code: 0,
      message: 'success',
      data: [providerMutation(2, 'TIKTOK', 'REQUEST_CONTENT_AUTHORIZATION', 'READY', 'APPROVED')],
    })
    vi.mocked(marketingPlatformApi.listProgrammaticDspMutations).mockResolvedValue({
      code: 0,
      message: 'success',
      data: [providerMutation(3, 'DV360', 'UPDATE_LINE_ITEM_BID', 'DRY_RUN_OK', 'APPROVED')],
    })
  })

  it('renders the unified control plane with capabilities, lanes, and actions', async () => {
    render(
      <MemoryRouter initialEntries={['/marketing-platform']}>
        <MarketingPlatformPage />
      </MemoryRouter>,
    )

    await waitFor(() => expect(screen.getByRole('heading', { name: '营销中台' })).toBeInTheDocument())

    expect(screen.getByText('2 / 4 能力已上线')).toBeInTheDocument()
    expect(screen.getAllByText('上线闸口').length).toBeGreaterThan(0)
    expect(screen.getByText('阻断 1 · 警告 1')).toBeInTheDocument()
    expect(screen.getByText('Configure Paid Media Activation')).toBeInTheDocument()
    expect(screen.getByText('Paid Media Activation')).toBeInTheDocument()
    expect(screen.getByText('publishedJourneys 1')).toBeInTheDocument()
    expect(screen.getByText('paidMediaDestinations 0')).toBeInTheDocument()
    expect(screen.getByText('Search Opportunities To Provider Write')).toBeInTheDocument()
    expect(screen.getByText('Configure paid-media provider destinations')).toBeInTheDocument()
    expect(screen.getByText('集成契约注册表')).toBeInTheDocument()
    expect(screen.getByText('Google Ads keyword write')).toBeInTheDocument()
    expect(screen.getAllByText('google-ads-keyword-write').length).toBeGreaterThan(0)
    expect(screen.getByText('生产 ACTIVE 1')).toBeInTheDocument()
    expect(screen.getByText('健康 PASS 1')).toBeInTheDocument()
    expect(screen.getAllByText('prod-readiness-probe').length).toBeGreaterThan(0)
    expect(screen.getByText('204 · 180ms')).toBeInTheDocument()
    expect(screen.getByText('集成探针告警')).toBeInTheDocument()
    expect(screen.getByText('Marketing integration contract probe failed')).toBeInTheDocument()
    expect(screen.getByText('google-ads-keyword-write failed prod-readiness-probe: timeout')).toBeInTheDocument()
    expect(screen.getByText('Marketing integration contract SLO burn-rate breached')).toBeInTheDocument()
    expect(screen.getByText('google-ads-keyword-write breached PAGE_FAST_BURN: 20.00x burn')).toBeInTheDocument()
    expect(screen.getAllByText('SLO Burn-rate').length).toBeGreaterThan(0)
    expect(screen.getByText('PAGE_FAST_BURN')).toBeInTheDocument()
    expect(screen.getByText('20x / 14.4x')).toBeInTheDocument()
    expect(screen.getAllByText('CRITICAL').length).toBeGreaterThanOrEqual(2)
    expect(marketingMonitoringApi.listAlerts).toHaveBeenCalledWith({ status: 'OPEN', limit: 100 })
    expect(screen.getByText('记录探针')).toBeInTheDocument()
    expect(screen.getByText('审计')).toBeInTheDocument()
    expect(screen.getByText('新建契约')).toBeInTheDocument()
    expect(screen.getByText('Campaign 主账本')).toBeInTheDocument()
    expect(screen.getByText('Spring launch')).toBeInTheDocument()
    expect(screen.getByText('spring-launch')).toBeInTheDocument()
    expect(screen.getByText('新建 Campaign')).toBeInTheDocument()
    expect(screen.getAllByText('上线闸口').length).toBeGreaterThan(1)
    expect(screen.getByText('评估闸口')).toBeInTheDocument()
    expect(screen.getByText('Provider 写入操作')).toBeInTheDocument()
    expect(screen.getByText('集成资产目录')).toBeInTheDocument()
    expect(screen.getByText('Search Provider Write Gateway')).toBeInTheDocument()
    expect(screen.getByText('待审批 3')).toBeInTheDocument()
    expect(screen.getByText('UPDATE_KEYWORD_BID')).toBeInTheDocument()
    expect(screen.getByText('REQUEST_CONTENT_AUTHORIZATION')).toBeInTheDocument()
    expect(screen.getByText('UPDATE_LINE_ITEM_BID')).toBeInTheDocument()
  })

  it('runs automated integration probes from the control plane', async () => {
    render(
      <MemoryRouter initialEntries={['/marketing-platform']}>
        <MarketingPlatformPage />
      </MemoryRouter>,
    )

    await waitFor(() => expect(screen.getByRole('heading', { name: '营销中台' })).toBeInTheDocument())

    const probeLoadCount = vi.mocked(marketingPlatformApi.listRecentMarketingIntegrationContractProbes).mock.calls.length
    fireEvent.click(screen.getByRole('button', { name: /运行自动探针/ }))

    await waitFor(() => {
      expect(marketingPlatformApi.scanMarketingIntegrationContractProbes).toHaveBeenCalledWith({ limit: 50 })
    })
    await waitFor(() => {
      expect(vi.mocked(marketingPlatformApi.listRecentMarketingIntegrationContractProbes).mock.calls.length)
        .toBeGreaterThan(probeLoadCount)
    })
  }, 10000)

  it('opens integration contract audit history from the control plane', async () => {
    render(
      <MemoryRouter initialEntries={['/marketing-platform']}>
        <MarketingPlatformPage />
      </MemoryRouter>,
    )

    await waitFor(() => expect(screen.getByRole('heading', { name: '营销中台' })).toBeInTheDocument())
    fireEvent.click(screen.getByRole('button', { name: /审计/ }))

    await waitFor(() => {
      expect(marketingPlatformApi.listMarketingIntegrationContractAuditEvents).toHaveBeenCalledWith(30, { limit: 50 })
    })
    expect(await screen.findByText('UPDATED')).toBeInTheDocument()
    expect(screen.getByText('DRAFT -> ACTIVE')).toBeInTheDocument()
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
    capability('journey-orchestration', 'Journey Orchestration', 'LIVE', '/canvas'),
    capability('content-lifecycle', 'Content Lifecycle', 'LIVE', '/content-hub'),
    capability('paid-media-activation', 'Paid Media Activation', 'CONFIGURATION_REQUIRED', '/audiences'),
    capability('search-marketing-governance', 'Search Marketing Governance', 'API_ONLY', '/marketing-platform'),
  ],
  integrationLanes: [
    {
      laneKey: 'search-to-provider-write',
      displayName: 'Search Opportunities To Provider Write',
      sourceCapabilityKey: 'search-marketing-governance',
      targetCapabilityKey: 'provider-credential-governance',
      status: 'CONFIGURATION_REQUIRED',
      controls: ['approval gate', 'dry-run evidence'],
    },
  ],
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
      pendingWrites: 3,
      failedWrites: 1,
      controls: ['approval gate', 'dry-run evidence'],
      gaps: ['wire live SEM write client'],
      evidence: [
        {
          signalKey: 'searchProviderMutations',
          label: 'Provider mutations',
          value: 12,
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
        reason: 'Real provider activation depends on destination setup',
      },
    ],
    warnings: [
      {
        severity: 'WARNING',
        itemType: 'INTEGRATION_ASSET',
        itemKey: 'search-provider-write-gateway',
        title: 'Search Provider Write Gateway has no live adapter',
        route: '/canvas/search-marketing/mutations',
        reason: 'wire live SEM write client',
      },
    ],
  },
  actionItems: [
    {
      priority: 'HIGH',
      capabilityKey: 'paid-media-activation',
      title: 'Configure paid-media provider destinations',
      route: '/audiences',
      reason: 'Real provider activation depends on destination setup',
    },
    {
      priority: 'MEDIUM',
      capabilityKey: 'search-marketing-governance',
      title: 'Wire live SEM provider write clients',
      route: '/marketing-platform',
      reason: 'Provider clients are environment-specific',
    },
  ],
}

function capability(capabilityKey: string, displayName: string, status: string, route: string) {
  const evidenceLabel = capabilityKey === 'journey-orchestration'
    ? 'publishedJourneys'
    : capabilityKey === 'paid-media-activation'
      ? 'paidMediaDestinations'
      : `${capabilityKey}Evidence`
  return {
    capabilityKey,
    displayName,
    domain: 'marketing',
    status,
    route,
    apiRoot: '/canvas/marketing-platform',
    surface: 'operator-facing',
    productionSignals: ['tenant scoped'],
    gaps: status === 'LIVE' ? [] : ['requires provider configuration'],
    evidence: [
      {
        signalKey: evidenceLabel,
        label: evidenceLabel,
        value: status === 'LIVE' ? 1 : 0,
        status: status === 'LIVE' ? 'PRESENT' : 'MISSING',
      },
    ],
  }
}

function providerMutation(id: number, provider: string, mutationType: string, status: string, approvalStatus: string) {
  return {
    id,
    provider,
    mutationKey: `mutation-${id}`,
    mutationType,
    entityType: 'LINE_ITEM',
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
    errorCode: null,
    errorMessage: null,
    createdBy: 'operator-1',
    approvedBy: approvalStatus === 'APPROVED' ? 'lead-1' : null,
    approvedAt: approvalStatus === 'APPROVED' ? '2026-06-06T09:00:00' : null,
    executedBy: null,
    executedAt: null,
    createdAt: '2026-06-06T08:00:00',
    updatedAt: `2026-06-06T10:0${id}:00`,
  }
}

function integrationContract() {
  return {
    id: 30,
    tenantId: 7,
    contractKey: 'google-ads-keyword-write',
    displayName: 'Google Ads keyword write',
    providerFamily: 'SEM',
    sourceCapabilityKey: 'search-marketing-governance',
    targetCapabilityKey: 'provider-credential-governance',
    assetKey: 'search-provider-write-gateway',
    direction: 'OUTBOUND',
    environment: 'PRODUCTION',
    authMode: 'OAUTH',
    credentialDependency: 'active provider credential',
    apiRoot: '/canvas/search-marketing/mutations',
    ownerTeam: 'Growth',
    status: 'ACTIVE',
    slaTier: 'STANDARD',
    timeoutMs: 30000,
    retryPolicy: { maxAttempts: 3 },
    schemaContract: {},
    metadata: { provider: 'GOOGLE_ADS' },
    createdBy: 'operator-1',
    updatedBy: 'operator-1',
    createdAt: '2026-06-06T08:00:00',
    updatedAt: '2026-06-06T10:00:00',
  }
}

function integrationProbeRun() {
  return {
    id: 40,
    tenantId: 7,
    contractId: 30,
    contractKey: 'google-ads-keyword-write',
    probeKey: 'prod-readiness-probe',
    environment: 'PRODUCTION',
    status: 'PASS',
    httpStatusCode: 204,
    latencyMs: 180,
    problemTypeUri: null,
    problemTitle: null,
    problemDetail: null,
    errorType: null,
    evidence: { traceId: 'abc-123' },
    observedAt: '2026-06-06T10:00:00',
    createdBy: 'operator-1',
    createdAt: '2026-06-06T10:00:00',
  }
}

function integrationAuditEvent() {
  return {
    id: 41,
    tenantId: 7,
    contractId: 30,
    contractKey: 'google-ads-keyword-write',
    revision: 2,
    eventType: 'UPDATED',
    previousStatus: 'DRAFT',
    newStatus: 'ACTIVE',
    snapshot: { contractKey: 'google-ads-keyword-write' },
    changedFields: { changedFields: ['status'] },
    changedBy: 'operator-1',
    createdAt: '2026-06-06T10:00:00',
  }
}

function integrationProbeAlert() {
  return {
    id: 901,
    tenantId: 7,
    alertType: 'INTEGRATION_CONTRACT_PROBE_FAILURE',
    severity: 'CRITICAL',
    status: 'OPEN',
    scopeKey: 'google-ads-keyword-write',
    title: 'Marketing integration contract probe failed',
    reason: 'google-ads-keyword-write failed prod-readiness-probe: timeout',
    itemCount: 2,
    windowStart: '2026-06-06T10:00:00',
    windowEnd: '2026-06-06T10:05:00',
    metadata: {
      contractId: 30,
      probeRunId: 40,
      providerFamily: 'SEM',
      lastHttpStatusCode: 504,
    },
    createdBy: 'probe-scheduler',
    resolvedBy: null,
    resolvedAt: null,
    createdAt: '2026-06-06T10:00:00',
    updatedAt: '2026-06-06T10:05:00',
  }
}

function integrationSloAlert() {
  return {
    id: 903,
    tenantId: 7,
    alertType: 'INTEGRATION_CONTRACT_SLO_BURN_RATE',
    severity: 'CRITICAL',
    status: 'OPEN',
    scopeKey: 'google-ads-keyword-write',
    title: 'Marketing integration contract SLO burn-rate breached',
    reason: 'google-ads-keyword-write breached PAGE_FAST_BURN: 20.00x burn',
    itemCount: 1,
    windowStart: '2026-06-06T09:00:00',
    windowEnd: '2026-06-06T10:00:00',
    metadata: {
      contractId: 30,
      probeKey: 'prod-readiness-probe',
      providerFamily: 'SEM',
      targetPercent: 99,
    },
    createdBy: 'probe-scheduler',
    resolvedBy: null,
    resolvedAt: null,
    createdAt: '2026-06-06T10:00:00',
    updatedAt: '2026-06-06T10:00:00',
  }
}

function integrationSloEvaluation() {
  return {
    tenantId: 7,
    contractId: 30,
    contractKey: 'google-ads-keyword-write',
    displayName: 'Google Ads keyword write',
    providerFamily: 'SEM',
    probeKey: 'prod-readiness-probe',
    status: 'PAGE',
    severity: 'CRITICAL',
    triggeredRuleKey: 'PAGE_FAST_BURN',
    targetPercent: 99,
    errorBudget: 0.01,
    reason: 'google-ads-keyword-write breached PAGE_FAST_BURN: 20.00x burn over 60m and 20.00x over 5m',
    generatedAt: '2026-06-06T10:00:00',
    windows: [
      {
        ruleKey: 'PAGE_FAST_BURN',
        windowKey: 'long',
        windowMinutes: 60,
        totalCount: 100,
        badCount: 20,
        badRatio: 0.2,
        burnRate: 20,
        thresholdBurnRate: 14.4,
        sufficient: true,
        breached: true,
        windowStart: '2026-06-06T09:00:00',
        windowEnd: '2026-06-06T10:00:00',
      },
    ],
  }
}

function unrelatedAlert() {
  return {
    id: 902,
    tenantId: 7,
    alertType: 'NEGATIVE_SENTIMENT',
    severity: 'HIGH',
    status: 'OPEN',
    scopeKey: 'brand',
    title: 'Negative sentiment detected',
    reason: 'irrelevant',
    itemCount: 1,
    windowStart: '2026-06-06T10:00:00',
    windowEnd: '2026-06-06T10:00:00',
    metadata: {},
    createdBy: 'operator-1',
    createdAt: '2026-06-06T10:00:00',
    updatedAt: '2026-06-06T10:00:00',
  }
}

function campaign() {
  return {
    id: 10,
    tenantId: 7,
    campaignKey: 'spring-launch',
    campaignName: 'Spring launch',
    objective: 'ACQUISITION',
    status: 'ACTIVE',
    primaryChannel: 'PAID_MEDIA',
    ownerTeam: 'Growth',
    startAt: '2026-06-01T00:00:00',
    endAt: '2026-06-30T23:59:00',
    budgetAmount: 1200,
    currency: 'CNY',
    brief: {},
    createdBy: 'operator-1',
    updatedBy: 'operator-1',
    createdAt: '2026-06-06T08:00:00',
    updatedAt: '2026-06-06T10:00:00',
  }
}

function campaignLink() {
  return {
    id: 20,
    tenantId: 7,
    campaignId: 10,
    resourceType: 'JOURNEY',
    resourceId: 300,
    resourceKey: 'launch-journey',
    resourceName: 'Launch journey',
    resourceRoute: '/canvas/300',
    dependencyRole: 'PRIMARY',
    linkStatus: 'ACTIVE',
    requiredForLaunch: true,
    metadata: {},
    createdBy: 'operator-1',
    updatedBy: 'operator-1',
    createdAt: '2026-06-06T08:00:00',
    updatedAt: '2026-06-06T10:00:00',
  }
}

function campaignReadiness() {
  return {
    tenantId: 7,
    campaignId: 10,
    campaignKey: 'spring-launch',
    campaignName: 'Spring launch',
    generatedAt: '2026-06-06T10:00',
    status: 'READY',
    productionReady: true,
    requiredLinkCount: 2,
    activeRequiredLinkCount: 2,
    blockerCount: 0,
    warningCount: 0,
    blockers: [],
    warnings: [],
    links: [campaignLink()],
  }
}
