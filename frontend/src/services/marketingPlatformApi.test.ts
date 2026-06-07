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
