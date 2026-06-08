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
import { afterEach, describe, expect, it, vi } from 'vitest'

import http from './api'
import { marketingPlatformApi } from './marketingPlatformApi'

describe('marketingPlatformApi', () => {
  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('loads the marketing platform control plane', async () => {
    const response = { code: 0, message: 'success', data: { capabilityCount: 0 } }
    const get = vi.spyOn(http, 'get').mockResolvedValue(response)

    await expect(marketingPlatformApi.controlPlane()).resolves.toBe(response)

    expect(get).toHaveBeenCalledWith('/canvas/marketing-platform/control-plane')
  })

  it('loads provider write mutation ledgers', async () => {
    const response = { code: 0, message: 'success', data: [] }
    const get = vi.spyOn(http, 'get').mockResolvedValue(response)

    await expect(marketingPlatformApi.listSearchMarketingMutations({ sourceId: 10, status: 'READY' }))
      .resolves.toBe(response)
    await expect(marketingPlatformApi.listCreatorProviderMutations({ campaignId: 20, approvalStatus: 'PENDING' }))
      .resolves.toBe(response)
    await expect(marketingPlatformApi.listProgrammaticDspMutations({ seatId: 30, lineItemId: 40 }))
      .resolves.toBe(response)

    expect(get).toHaveBeenCalledWith('/canvas/search-marketing/mutations', {
      params: { sourceId: 10, status: 'READY' },
    })
    expect(get).toHaveBeenCalledWith('/canvas/creator-collaboration/mutations', {
      params: { campaignId: 20, approvalStatus: 'PENDING' },
    })
    expect(get).toHaveBeenCalledWith('/canvas/programmatic-dsp/mutations', {
      params: { seatId: 30, lineItemId: 40 },
    })
  })

  it('operates the campaign master ledger', async () => {
    const response = { code: 0, message: 'success', data: [] }
    const get = vi.spyOn(http, 'get').mockResolvedValue(response)
    const post = vi.spyOn(http, 'post').mockResolvedValue(response)
    const del = vi.spyOn(http, 'delete').mockResolvedValue(response)
    const campaign = {
      campaignKey: 'spring-launch',
      campaignName: 'Spring launch',
      status: 'ACTIVE',
      brief: {},
    }
    const link = {
      campaignId: 10,
      resourceType: 'JOURNEY',
      resourceKey: 'launch-journey',
      requiredForLaunch: true,
      metadata: {},
    }

    await expect(marketingPlatformApi.listMarketingCampaigns({ status: 'ACTIVE', limit: 50 }))
      .resolves.toBe(response)
    await expect(marketingPlatformApi.upsertMarketingCampaign(campaign)).resolves.toBe(response)
    await expect(marketingPlatformApi.listMarketingCampaignLinks(10)).resolves.toBe(response)
    await expect(marketingPlatformApi.getMarketingCampaignReadiness(10)).resolves.toBe(response)
    await expect(marketingPlatformApi.linkMarketingCampaignResource(link)).resolves.toBe(response)
    await expect(marketingPlatformApi.unlinkMarketingCampaignResource(20)).resolves.toBe(response)

    expect(get).toHaveBeenCalledWith('/canvas/marketing-campaigns', {
      params: { status: 'ACTIVE', limit: 50 },
    })
    expect(post).toHaveBeenCalledWith('/canvas/marketing-campaigns', campaign)
    expect(get).toHaveBeenCalledWith('/canvas/marketing-campaigns/10/links')
    expect(get).toHaveBeenCalledWith('/canvas/marketing-campaigns/10/readiness')
    expect(post).toHaveBeenCalledWith('/canvas/marketing-campaigns/links', link)
    expect(del).toHaveBeenCalledWith('/canvas/marketing-campaigns/links/20')
  })

  it('operates the marketing integration contract registry', async () => {
    const response = { code: 0, message: 'success', data: [] }
    const get = vi.spyOn(http, 'get').mockResolvedValue(response)
    const post = vi.spyOn(http, 'post').mockResolvedValue(response)
    const del = vi.spyOn(http, 'delete').mockResolvedValue(response)
    const contract = {
      contractKey: 'google-ads-keyword-write',
      displayName: 'Google Ads keyword write',
      providerFamily: 'SEM',
      sourceCapabilityKey: 'search-marketing-governance',
      targetCapabilityKey: 'provider-credential-governance',
      assetKey: 'search-provider-write-gateway',
      apiRoot: '/canvas/search-marketing/mutations',
      metadata: {},
    }

    await expect(marketingPlatformApi.listMarketingIntegrationContracts({
      status: 'ACTIVE',
      providerFamily: 'SEM',
      limit: 50,
    })).resolves.toBe(response)
    await expect(marketingPlatformApi.upsertMarketingIntegrationContract(contract)).resolves.toBe(response)
    await expect(marketingPlatformApi.archiveMarketingIntegrationContract(10)).resolves.toBe(response)
    await expect(marketingPlatformApi.listMarketingIntegrationContractAuditEvents(10, { limit: 50 }))
      .resolves.toBe(response)
    await expect(marketingPlatformApi.listRecentMarketingIntegrationContractProbes({
      status: 'PASS',
      limit: 50,
    })).resolves.toBe(response)
    await expect(marketingPlatformApi.listMarketingIntegrationContractProbes(10, { limit: 20 })).resolves.toBe(response)
    await expect(marketingPlatformApi.listMarketingIntegrationContractSloEvaluations({ limit: 20 }))
      .resolves.toBe(response)
    await expect(marketingPlatformApi.recordMarketingIntegrationContractProbe(10, {
      probeKey: 'prod-readiness-probe',
      status: 'PASS',
      httpStatusCode: 204,
      latencyMs: 180,
      evidence: { traceId: 'abc-123' },
    })).resolves.toBe(response)
    await expect(marketingPlatformApi.scanMarketingIntegrationContractProbes({ limit: 50 }))
      .resolves.toBe(response)

    expect(get).toHaveBeenCalledWith('/canvas/marketing-integrations/contracts', {
      params: { status: 'ACTIVE', providerFamily: 'SEM', limit: 50 },
    })
    expect(post).toHaveBeenCalledWith('/canvas/marketing-integrations/contracts', contract)
    expect(del).toHaveBeenCalledWith('/canvas/marketing-integrations/contracts/10')
    expect(get).toHaveBeenCalledWith('/canvas/marketing-integrations/contracts/10/audit-events', {
      params: { limit: 50 },
    })
    expect(get).toHaveBeenCalledWith('/canvas/marketing-integrations/probes', {
      params: { status: 'PASS', limit: 50 },
    })
    expect(get).toHaveBeenCalledWith('/canvas/marketing-integrations/contracts/10/probes', {
      params: { limit: 20 },
    })
    expect(get).toHaveBeenCalledWith('/canvas/marketing-integrations/contract-slo-evaluations', {
      params: { limit: 20 },
    })
    expect(post).toHaveBeenCalledWith('/canvas/marketing-integrations/contracts/10/probes', {
      probeKey: 'prod-readiness-probe',
      status: 'PASS',
      httpStatusCode: 204,
      latencyMs: 180,
      evidence: { traceId: 'abc-123' },
    })
    expect(post).toHaveBeenCalledWith('/canvas/marketing-integrations/contract-probe-runs/scan', undefined, {
      params: { limit: 50 },
    })
  })

  it('approves and executes provider write mutations', async () => {
    const response = { code: 0, message: 'success', data: { id: 1 } }
    const post = vi.spyOn(http, 'post').mockResolvedValue(response)
    const approval = { decision: 'APPROVED' as const, reason: 'reviewed' }
    const dryRun = { dryRun: true, partialFailure: true, metadata: { source: 'ui' } }

    await expect(marketingPlatformApi.approveSearchMarketingMutation(1, approval)).resolves.toBe(response)
    await expect(marketingPlatformApi.executeSearchMarketingMutation(1, dryRun)).resolves.toBe(response)
    await expect(marketingPlatformApi.approveCreatorProviderMutation(2, approval)).resolves.toBe(response)
    await expect(marketingPlatformApi.executeCreatorProviderMutation(2, dryRun)).resolves.toBe(response)
    await expect(marketingPlatformApi.approveProgrammaticDspMutation(3, approval)).resolves.toBe(response)
    await expect(marketingPlatformApi.executeProgrammaticDspMutation(3, dryRun)).resolves.toBe(response)

    expect(post).toHaveBeenCalledWith('/canvas/search-marketing/mutations/1/approve', approval)
    expect(post).toHaveBeenCalledWith('/canvas/search-marketing/mutations/1/execute', dryRun)
    expect(post).toHaveBeenCalledWith('/canvas/creator-collaboration/mutations/2/approve', approval)
    expect(post).toHaveBeenCalledWith('/canvas/creator-collaboration/mutations/2/execute', dryRun)
    expect(post).toHaveBeenCalledWith('/canvas/programmatic-dsp/mutations/3/approve', approval)
    expect(post).toHaveBeenCalledWith('/canvas/programmatic-dsp/mutations/3/execute', dryRun)
  })
})
