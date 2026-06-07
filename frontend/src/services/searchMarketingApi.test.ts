import { afterEach, describe, expect, it, vi } from 'vitest'

import http from './api'
import { searchMarketingApi } from './searchMarketingApi'

describe('searchMarketingApi', () => {
  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('calls search marketing read endpoints', async () => {
    const response = { code: 0, message: 'success', data: [] }
    const get = vi.spyOn(http, 'get').mockResolvedValue(response)

    await expect(searchMarketingApi.listSources({ provider: 'GOOGLE_ADS' })).resolves.toBe(response)
    await expect(searchMarketingApi.listKeywords({ channel: 'SEM' })).resolves.toBe(response)
    await expect(searchMarketingApi.listSnapshots({ channel: 'SEO' })).resolves.toBe(response)
    await expect(searchMarketingApi.listOpportunities({ status: 'OPEN' })).resolves.toBe(response)
    await expect(searchMarketingApi.listMutations({ status: 'READY' })).resolves.toBe(response)
    await expect(searchMarketingApi.listUrlInspections({ indexedState: 'INDEXED' })).resolves.toBe(response)
    await expect(searchMarketingApi.listProviderChanges({ reconciliationStatus: 'CONFIRMED' })).resolves.toBe(response)
    await expect(searchMarketingApi.listImpactWindows({ status: 'SCHEDULED' })).resolves.toBe(response)
    await expect(searchMarketingApi.listSyncRuns({ sourceId: 10 })).resolves.toBe(response)
    await expect(searchMarketingApi.readiness()).resolves.toBe(response)

    expect(get).toHaveBeenCalledWith('/canvas/search-marketing/sources', {
      params: { provider: 'GOOGLE_ADS' },
    })
    expect(get).toHaveBeenCalledWith('/canvas/search-marketing/keywords', {
      params: { channel: 'SEM' },
    })
    expect(get).toHaveBeenCalledWith('/canvas/search-marketing/snapshots', {
      params: { channel: 'SEO' },
    })
    expect(get).toHaveBeenCalledWith('/canvas/search-marketing/opportunities', {
      params: { status: 'OPEN' },
    })
    expect(get).toHaveBeenCalledWith('/canvas/search-marketing/mutations', {
      params: { status: 'READY' },
    })
    expect(get).toHaveBeenCalledWith('/canvas/search-marketing/url-inspections', {
      params: { indexedState: 'INDEXED' },
    })
    expect(get).toHaveBeenCalledWith('/canvas/search-marketing/provider-changes', {
      params: { reconciliationStatus: 'CONFIRMED' },
    })
    expect(get).toHaveBeenCalledWith('/canvas/search-marketing/impact-windows', {
      params: { status: 'SCHEDULED' },
    })
    expect(get).toHaveBeenCalledWith('/canvas/search-marketing/sync-runs', {
      params: { sourceId: 10 },
    })
    expect(get).toHaveBeenCalledWith('/canvas/search-marketing/readiness')
  })

  it('calls search marketing write endpoints', async () => {
    const response = { code: 0, message: 'success', data: { id: 1 } }
    const post = vi.spyOn(http, 'post').mockResolvedValue(response)
    const sync = { runType: 'PERFORMANCE', windowStart: '2026-06-01', windowEnd: '2026-06-02' }
    const proposal = {
      mutationKey: 'bid-raise-1',
      mutationType: 'UPDATE_KEYWORD_BID',
      entityType: 'KEYWORD',
      externalEntityId: 'customers/1/adGroupCriteria/2~3',
      dryRunRequired: true,
      payload: { bidMicros: 1500000 },
    }
    const execute = { dryRun: true, partialFailure: true, metadata: { source: 'ui' } }

    await expect(searchMarketingApi.syncSource(10, sync)).resolves.toBe(response)
    await expect(searchMarketingApi.proposeOpportunityMutation(40, proposal)).resolves.toBe(response)
    await expect(searchMarketingApi.executeMutation(50, execute)).resolves.toBe(response)
    await expect(searchMarketingApi.reconcileMutation(50)).resolves.toBe(response)
    await expect(searchMarketingApi.evaluateDueImpactWindows({ limit: 10 })).resolves.toBe(response)

    expect(post).toHaveBeenCalledWith('/canvas/search-marketing/sources/10/sync', sync)
    expect(post).toHaveBeenCalledWith('/canvas/search-marketing/opportunities/40/mutations', proposal)
    expect(post).toHaveBeenCalledWith('/canvas/search-marketing/mutations/50/execute', execute)
    expect(post).toHaveBeenCalledWith('/canvas/search-marketing/mutations/50/reconcile', {})
    expect(post).toHaveBeenCalledWith('/canvas/search-marketing/impact-windows/evaluate-due', { limit: 10 })
  })
})
