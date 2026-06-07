/* @vitest-environment jsdom */
import { act, fireEvent, render, screen, waitFor } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import SearchMarketingPage from './index'
import { searchMarketingApi } from '../../services/searchMarketingApi'

vi.mock('../../services/searchMarketingApi', () => ({
  searchMarketingApi: {
    executeMutation: vi.fn(),
    listKeywords: vi.fn(),
      listMutations: vi.fn(),
      listOpportunities: vi.fn(),
      listImpactWindows: vi.fn(),
      listProviderChanges: vi.fn(),
      listSnapshots: vi.fn(),
      listSources: vi.fn(),
      listSyncRuns: vi.fn(),
      listUrlInspections: vi.fn(),
      proposeOpportunityMutation: vi.fn(),
      readiness: vi.fn(),
      reconcileMutation: vi.fn(),
      syncSource: vi.fn(),
      evaluateDueImpactWindows: vi.fn(),
    },
}))

describe('SearchMarketingPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.mocked(searchMarketingApi.readiness).mockResolvedValue({
      code: 0,
      message: 'success',
      data: { tenantId: 7, status: 'LIVE', blockers: [], evidence: {}, evaluatedAt: '2026-06-06T10:00:00' },
    })
    vi.mocked(searchMarketingApi.listSources).mockResolvedValue({
      code: 0,
      message: 'success',
      data: [source],
    })
    vi.mocked(searchMarketingApi.listKeywords).mockResolvedValue({
      code: 0,
      message: 'success',
      data: [keyword],
    })
    vi.mocked(searchMarketingApi.listSnapshots).mockResolvedValue({
      code: 0,
      message: 'success',
      data: [seoSnapshot, semSnapshot],
    })
    vi.mocked(searchMarketingApi.listOpportunities).mockResolvedValue({
      code: 0,
      message: 'success',
      data: [opportunity],
    })
    vi.mocked(searchMarketingApi.listMutations).mockResolvedValue({
      code: 0,
      message: 'success',
      data: [readyMutation, dryRunOkMutation, appliedMutation],
    })
    vi.mocked(searchMarketingApi.listUrlInspections).mockResolvedValue({
      code: 0,
      message: 'success',
      data: [urlInspection],
    })
    vi.mocked(searchMarketingApi.listProviderChanges).mockResolvedValue({
      code: 0,
      message: 'success',
      data: [providerChange],
    })
    vi.mocked(searchMarketingApi.listImpactWindows).mockResolvedValue({
      code: 0,
      message: 'success',
      data: [impactWindow],
    })
    vi.mocked(searchMarketingApi.listSyncRuns).mockResolvedValue({
      code: 0,
      message: 'success',
      data: [syncRun],
    })
    vi.mocked(searchMarketingApi.syncSource).mockResolvedValue({
      code: 0,
      message: 'success',
      data: syncRun,
    })
    vi.mocked(searchMarketingApi.proposeOpportunityMutation).mockResolvedValue({
      code: 0,
      message: 'success',
      data: readyMutation,
    })
    vi.mocked(searchMarketingApi.executeMutation).mockResolvedValue({
      code: 0,
      message: 'success',
      data: dryRunOkMutation,
    })
    vi.mocked(searchMarketingApi.reconcileMutation).mockResolvedValue({
      code: 0,
      message: 'success',
      data: {
        tenantId: 7,
        mutationId: 52,
        providerChangeId: 80,
        status: 'RECONCILED',
        providerOperationId: 'operations/1',
        evidence: {},
        reconciledAt: '2026-06-06T10:00:00',
      },
    })
    vi.mocked(searchMarketingApi.evaluateDueImpactWindows).mockResolvedValue({
      code: 0,
      message: 'success',
      data: [impactWindow],
    })
  })

  it('renders the workbench tabs and redacts secret-shaped evidence', async () => {
    render(<SearchMarketingPage />)

    expect(screen.getByRole('heading', { name: 'SEO / SEM 管理' })).toBeInTheDocument()
    await waitFor(() => expect(searchMarketingApi.listSources).toHaveBeenCalledWith({ limit: 50 }))
    expect(screen.getByText('Overview')).toBeInTheDocument()
    expect(screen.getByText('Sources')).toBeInTheDocument()
    expect(screen.getByText('Keyword Portfolio')).toBeInTheDocument()
    expect(screen.getByText('Performance Evidence')).toBeInTheDocument()
    expect(screen.getByText('SEO Technical Evidence')).toBeInTheDocument()
    expect(screen.getByText('Opportunities')).toBeInTheDocument()
    expect(screen.getByText('Provider Writes and Impact')).toBeInTheDocument()
    expect(screen.getByText('Google Ads Main')).toBeInTheDocument()
    fireEvent.click(screen.getByRole('tab', { name: 'SEO Technical Evidence' }))
    await screen.findByText('INDEXED')
    expect(document.body.textContent).not.toContain('access_token')
    expect(document.body.textContent).not.toContain('refresh_token')
    expect(document.body.textContent).not.toContain('client_secret')
    expect(document.body.textContent).not.toContain('developer_token')
    expect(document.body.textContent).not.toContain('raw-secret')
  })

  it('shows a loading state while workbench requests are in flight', async () => {
    let resolveReadiness: (value: Awaited<ReturnType<typeof searchMarketingApi.readiness>>) => void = () => {}
    vi.mocked(searchMarketingApi.readiness).mockReturnValue(new Promise(resolve => {
      resolveReadiness = resolve
    }))

    render(<SearchMarketingPage />)

    expect(screen.getByText('加载搜索营销数据')).toBeInTheDocument()

    await act(async () => {
      resolveReadiness({
        code: 0,
        message: 'success',
        data: { tenantId: 7, status: 'LIVE', blockers: [], evidence: {}, evaluatedAt: '2026-06-06T10:00:00' },
      })
    })
  })

  it('shows retry and empty states when loading fails and then returns no rows', async () => {
    vi.mocked(searchMarketingApi.readiness)
      .mockRejectedValueOnce(new Error('network down'))
      .mockResolvedValueOnce({
        code: 0,
        message: 'success',
        data: { tenantId: 7, status: 'LIVE', blockers: [], evidence: {}, evaluatedAt: '2026-06-06T10:00:00' },
      })
    vi.mocked(searchMarketingApi.listSources).mockResolvedValue({ code: 0, message: 'success', data: [] })
    vi.mocked(searchMarketingApi.listKeywords).mockResolvedValue({ code: 0, message: 'success', data: [] })
    vi.mocked(searchMarketingApi.listSnapshots).mockResolvedValue({ code: 0, message: 'success', data: [] })
    vi.mocked(searchMarketingApi.listOpportunities).mockResolvedValue({ code: 0, message: 'success', data: [] })
    vi.mocked(searchMarketingApi.listMutations).mockResolvedValue({ code: 0, message: 'success', data: [] })
    vi.mocked(searchMarketingApi.listUrlInspections).mockResolvedValue({ code: 0, message: 'success', data: [] })
    vi.mocked(searchMarketingApi.listProviderChanges).mockResolvedValue({ code: 0, message: 'success', data: [] })
    vi.mocked(searchMarketingApi.listImpactWindows).mockResolvedValue({ code: 0, message: 'success', data: [] })
    vi.mocked(searchMarketingApi.listSyncRuns).mockResolvedValue({ code: 0, message: 'success', data: [] })

    render(<SearchMarketingPage />)

    await screen.findByText('搜索营销数据加载失败')
    fireEvent.click(screen.getByRole('button', { name: '重试' }))

    await screen.findByText('暂无搜索营销来源')
    expect(screen.getByText('暂无同步记录')).toBeInTheDocument()
    fireEvent.click(screen.getByRole('tab', { name: 'Sources' }))
    expect(screen.getByText('暂无 Sources 数据')).toBeInTheDocument()
  })

  it('syncs a source and reloads the workbench', async () => {
    render(<SearchMarketingPage />)

    await screen.findByText('Google Ads Main')
    fireEvent.click(screen.getByRole('button', { name: '同步 Google Ads Main' }))

    await waitFor(() => expect(searchMarketingApi.syncSource).toHaveBeenCalledWith(10, {
      runType: 'PERFORMANCE',
    }))
    await waitFor(() => expect(searchMarketingApi.listSources).toHaveBeenCalledTimes(2))
  })

  it('opens an opportunity proposal form and submits a mutation proposal', async () => {
    render(<SearchMarketingPage />)

    fireEvent.click(screen.getByRole('tab', { name: 'Opportunities' }))
    await screen.findByText('Improve CTR')
    fireEvent.click(screen.getByRole('button', { name: '创建提案 LOW_CTR' }))
    fireEvent.change(screen.getByLabelText('Mutation Key'), {
      target: { value: 'bid-raise-from-ui' },
    })
    fireEvent.click(screen.getByRole('button', { name: '提交提案' }))

    await waitFor(() => expect(searchMarketingApi.proposeOpportunityMutation).toHaveBeenCalledWith(40, {
      mutationKey: 'bid-raise-from-ui',
      mutationType: 'UPDATE_KEYWORD_BID',
      entityType: 'KEYWORD',
      externalEntityId: 'customers/1/adGroupCriteria/2~3',
      dryRunRequired: true,
      payload: { bidMicros: 1500000 },
    }))
  })

  it('disables apply until approval and dry-run success', async () => {
    render(<SearchMarketingPage />)

    fireEvent.click(screen.getByRole('tab', { name: 'Provider Writes and Impact' }))
    await screen.findByText('bid-ready')
    expect(screen.getByRole('button', { name: '应用 bid-ready' })).toBeDisabled()
    expect(screen.getByRole('button', { name: '应用 bid-dry-run-ok' })).toBeEnabled()
  })

  it('reconciles applied writes and evaluates due impact windows', async () => {
    render(<SearchMarketingPage />)

    fireEvent.click(screen.getByRole('tab', { name: 'Provider Writes and Impact' }))
    await screen.findByText('bid-applied')
    fireEvent.click(screen.getByRole('button', { name: '对账 bid-applied' }))
    fireEvent.click(screen.getByRole('button', { name: '评估到期影响窗口' }))

    await waitFor(() => expect(searchMarketingApi.reconcileMutation).toHaveBeenCalledWith(52))
    await waitFor(() => expect(searchMarketingApi.evaluateDueImpactWindows).toHaveBeenCalledWith({ limit: 50 }))
    expect(screen.getByText('CONFIRMED')).toBeInTheDocument()
    expect(screen.getByText('SCHEDULED')).toBeInTheDocument()
  })
})

const source = {
  id: 10,
  tenantId: 7,
  provider: 'GOOGLE_ADS',
  sourceKey: 'google-main',
  displayName: 'Google Ads Main',
  channel: 'SEM',
  externalAccountId: '123-456',
  currency: 'USD',
  timezone: 'Asia/Shanghai',
  enabled: true,
  metadata: { credentialKey: 'google-main' },
}

const keyword = {
  id: 20,
  tenantId: 7,
  channel: 'SEM',
  keywordText: 'Running Shoes',
  keywordKey: 'running shoes',
  matchType: 'PHRASE',
  landingPageUrl: 'https://example.com/shoes',
  landingPageUrlHash: 'page-hash',
  labels: ['brand'],
  status: 'ACTIVE',
  metadata: {},
}

const seoSnapshot = {
  id: 30,
  tenantId: 7,
  sourceId: 10,
  keywordId: 20,
  channel: 'SEO',
  snapshotDate: '2026-06-06',
  device: 'ALL',
  country: 'ALL',
  queryGroupKey: 'DEFAULT',
  impressionCount: 1000,
  clickCount: 100,
  costAmount: 0,
  conversionCount: 0,
  revenueAmount: 0,
  averagePosition: 4.2,
  metadata: {},
}

const semSnapshot = {
  ...seoSnapshot,
  id: 31,
  channel: 'SEM',
  clickCount: 30,
  costAmount: 120,
  conversionCount: 3,
  revenueAmount: 600,
}

const opportunity = {
  id: 40,
  tenantId: 7,
  sourceId: 10,
  keywordId: 20,
  channel: 'SEM',
  opportunityType: 'LOW_CTR',
  snapshotDate: '2026-06-06',
  severity: 'HIGH',
  status: 'ACCEPTED',
  recommendation: 'Improve CTR',
  impactScore: 5,
  evidence: { ctr: 0.01 },
}

const readyMutation = {
  id: 50,
  tenantId: 7,
  provider: 'GOOGLE_ADS',
  sourceId: 10,
  opportunityId: 40,
  keywordId: 20,
  channel: 'SEM',
  mutationKey: 'bid-ready',
  mutationType: 'UPDATE_KEYWORD_BID',
  entityType: 'KEYWORD',
  externalEntityId: 'customers/1/adGroupCriteria/2~3',
  requestHash: 'hash-1',
  idempotencyKey: 'idem-1',
  status: 'READY',
  approvalStatus: 'APPROVED',
  dryRunRequired: true,
  payload: { bidMicros: 1500000, access_token: 'raw-secret' },
  providerResponse: { client_secret: 'raw-secret' },
}

const dryRunOkMutation = {
  ...readyMutation,
  id: 51,
  mutationKey: 'bid-dry-run-ok',
  status: 'DRY_RUN_OK',
  providerResponse: { developer_token: 'raw-secret' },
}

const appliedMutation = {
  ...readyMutation,
  id: 52,
  mutationKey: 'bid-applied',
  status: 'APPLIED',
  providerResponse: { providerOperationId: 'operations/1', access_token: 'raw-secret' },
}

const urlInspection = {
  id: 70,
  tenantId: 7,
  sourceId: 10,
  provider: 'GOOGLE_SEARCH_CONSOLE',
  pageUrl: 'https://example.com/shoes',
  pageUrlHash: 'page-hash',
  inspectionDate: '2026-06-06',
  indexedState: 'INDEXED',
  crawlState: 'CRAWLED',
  canonicalUrl: 'https://example.com/shoes',
  sitemapState: 'PRESENT',
  mobileUsabilityState: 'PASS',
  lastCrawlAt: '2026-06-06T09:00:00',
  evidence: { refresh_token: 'raw-secret' },
}

const providerChange = {
  id: 80,
  tenantId: 7,
  sourceId: 10,
  mutationId: 52,
  provider: 'GOOGLE_ADS',
  externalResourceId: 'customers/1/adGroupCriteria/2~3',
  changeType: 'UPDATE_KEYWORD_BID',
  changedFields: { bidMicros: 1500000, developer_token: 'raw-secret' },
  providerActor: 'operator-1',
  providerChangedAt: '2026-06-06T10:00:00',
  reconciliationStatus: 'CONFIRMED',
  evidence: { client_secret: 'raw-secret' },
}

const impactWindow = {
  id: 90,
  tenantId: 7,
  opportunityId: 40,
  mutationId: 52,
  sourceId: 10,
  keywordId: 20,
  pageUrlHash: 'page-hash',
  baselineStartDate: '2026-05-24',
  baselineEndDate: '2026-05-30',
  postStartDate: '2026-06-07',
  postEndDate: '2026-06-13',
  status: 'SCHEDULED',
  decision: null,
  confidence: 0,
  metricDeltas: { apiKey: 'raw-secret' },
  evidence: { access_token: 'raw-secret' },
  dueAt: '2026-06-14T00:00:00',
}

const syncRun = {
  id: 60,
  tenantId: 7,
  sourceId: 10,
  runType: 'PERFORMANCE',
  provider: 'GOOGLE_ADS',
  channel: 'SEM',
  idempotencyKey: 'sync-1',
  status: 'SUCCEEDED',
  retryable: false,
  requestedCount: 2,
  successCount: 2,
  failedCount: 0,
  evidence: {},
}
