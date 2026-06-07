import type { R } from '../types'
import type {
  SearchMarketingKeyword,
  SearchMarketingKeywordQuery,
  SearchMarketingMutation,
  SearchMarketingMutationApprovalCommand,
  SearchMarketingMutationCommand,
  SearchMarketingMutationExecuteCommand,
  SearchMarketingMutationQuery,
  SearchMarketingOpportunity,
  SearchMarketingOpportunityMutationCommand,
  SearchMarketingOpportunityQuery,
  SearchMarketingOpportunityStatusCommand,
  SearchMarketingProviderChange,
  SearchMarketingProviderChangeQuery,
  SearchMarketingReadiness,
  SearchMarketingReconciliation,
  SearchMarketingImpactWindow,
  SearchMarketingImpactWindowQuery,
  SearchMarketingSnapshot,
  SearchMarketingSnapshotQuery,
  SearchMarketingSource,
  SearchMarketingSourceQuery,
  SearchMarketingSyncDueRequest,
  SearchMarketingSyncRequest,
  SearchMarketingSyncRun,
  SearchMarketingSyncRunQuery,
  SearchMarketingUrlInspection,
  SearchMarketingUrlInspectionQuery,
} from '../pages/search-marketing/searchMarketingWorkbench'
import http from './api'

export const searchMarketingApi = {
  listSources: (params?: SearchMarketingSourceQuery) =>
    http.get<R<SearchMarketingSource[]>, R<SearchMarketingSource[]>>(
      '/canvas/search-marketing/sources',
      { params },
    ),

  listKeywords: (params?: SearchMarketingKeywordQuery) =>
    http.get<R<SearchMarketingKeyword[]>, R<SearchMarketingKeyword[]>>(
      '/canvas/search-marketing/keywords',
      { params },
    ),

  listSnapshots: (params?: SearchMarketingSnapshotQuery) =>
    http.get<R<SearchMarketingSnapshot[]>, R<SearchMarketingSnapshot[]>>(
      '/canvas/search-marketing/snapshots',
      { params },
    ),

  listOpportunities: (params?: SearchMarketingOpportunityQuery) =>
    http.get<R<SearchMarketingOpportunity[]>, R<SearchMarketingOpportunity[]>>(
      '/canvas/search-marketing/opportunities',
      { params },
    ),

  updateOpportunityStatus: (opportunityId: number, payload: SearchMarketingOpportunityStatusCommand) =>
    http.post<R<SearchMarketingOpportunity>, R<SearchMarketingOpportunity>>(
      `/canvas/search-marketing/opportunities/${opportunityId}/status`,
      payload,
    ),

  proposeOpportunityMutation: (opportunityId: number, payload: SearchMarketingOpportunityMutationCommand) =>
    http.post<R<SearchMarketingMutation>, R<SearchMarketingMutation>>(
      `/canvas/search-marketing/opportunities/${opportunityId}/mutations`,
      payload,
    ),

  listMutations: (params?: SearchMarketingMutationQuery) =>
    http.get<R<SearchMarketingMutation[]>, R<SearchMarketingMutation[]>>(
      '/canvas/search-marketing/mutations',
      { params },
    ),

  proposeMutation: (payload: SearchMarketingMutationCommand) =>
    http.post<R<SearchMarketingMutation>, R<SearchMarketingMutation>>(
      '/canvas/search-marketing/mutations',
      payload,
    ),

  approveMutation: (mutationId: number, payload: SearchMarketingMutationApprovalCommand) =>
    http.post<R<SearchMarketingMutation>, R<SearchMarketingMutation>>(
      `/canvas/search-marketing/mutations/${mutationId}/approve`,
      payload,
    ),

  executeMutation: (mutationId: number, payload: SearchMarketingMutationExecuteCommand) =>
    http.post<R<SearchMarketingMutation>, R<SearchMarketingMutation>>(
      `/canvas/search-marketing/mutations/${mutationId}/execute`,
      payload,
    ),

  listUrlInspections: (params?: SearchMarketingUrlInspectionQuery) =>
    http.get<R<SearchMarketingUrlInspection[]>, R<SearchMarketingUrlInspection[]>>(
      '/canvas/search-marketing/url-inspections',
      { params },
    ),

  listProviderChanges: (params?: SearchMarketingProviderChangeQuery) =>
    http.get<R<SearchMarketingProviderChange[]>, R<SearchMarketingProviderChange[]>>(
      '/canvas/search-marketing/provider-changes',
      { params },
    ),

  listSyncRuns: (params?: SearchMarketingSyncRunQuery) =>
    http.get<R<SearchMarketingSyncRun[]>, R<SearchMarketingSyncRun[]>>(
      '/canvas/search-marketing/sync-runs',
      { params },
    ),

  syncSource: (sourceId: number, payload: SearchMarketingSyncRequest) =>
    http.post<R<SearchMarketingSyncRun>, R<SearchMarketingSyncRun>>(
      `/canvas/search-marketing/sources/${sourceId}/sync`,
      payload,
    ),

  syncDue: (payload?: SearchMarketingSyncDueRequest) =>
    http.post<R<SearchMarketingSyncRun[]>, R<SearchMarketingSyncRun[]>>(
      '/canvas/search-marketing/sources/sync-due',
      payload ?? {},
    ),

  reconcileMutation: (mutationId: number) =>
    http.post<R<SearchMarketingReconciliation>, R<SearchMarketingReconciliation>>(
      `/canvas/search-marketing/mutations/${mutationId}/reconcile`,
      {},
    ),

  listImpactWindows: (params?: SearchMarketingImpactWindowQuery) =>
    http.get<R<SearchMarketingImpactWindow[]>, R<SearchMarketingImpactWindow[]>>(
      '/canvas/search-marketing/impact-windows',
      { params },
    ),

  evaluateDueImpactWindows: (payload?: SearchMarketingSyncDueRequest) =>
    http.post<R<SearchMarketingImpactWindow[]>, R<SearchMarketingImpactWindow[]>>(
      '/canvas/search-marketing/impact-windows/evaluate-due',
      payload ?? {},
    ),

  readiness: () =>
    http.get<R<SearchMarketingReadiness>, R<SearchMarketingReadiness>>(
      '/canvas/search-marketing/readiness',
    ),
}
