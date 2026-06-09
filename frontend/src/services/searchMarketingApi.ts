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

/** 搜索营销 API，覆盖来源、关键词、快照、机会、provider 写入和影响评估。 */
export const searchMarketingApi = {
  /** 查询搜索营销来源。 */
  listSources: (params?: SearchMarketingSourceQuery) =>
    http.get<R<SearchMarketingSource[]>, R<SearchMarketingSource[]>>(
      '/canvas/search-marketing/sources',
      { params },
    ),

  /** 查询关键词主数据。 */
  listKeywords: (params?: SearchMarketingKeywordQuery) =>
    http.get<R<SearchMarketingKeyword[]>, R<SearchMarketingKeyword[]>>(
      '/canvas/search-marketing/keywords',
      { params },
    ),

  /** 查询搜索指标快照。 */
  listSnapshots: (params?: SearchMarketingSnapshotQuery) =>
    http.get<R<SearchMarketingSnapshot[]>, R<SearchMarketingSnapshot[]>>(
      '/canvas/search-marketing/snapshots',
      { params },
    ),

  /** 查询优化机会。 */
  listOpportunities: (params?: SearchMarketingOpportunityQuery) =>
    http.get<R<SearchMarketingOpportunity[]>, R<SearchMarketingOpportunity[]>>(
      '/canvas/search-marketing/opportunities',
      { params },
    ),

  /** 更新优化机会状态。 */
  updateOpportunityStatus: (opportunityId: number, payload: SearchMarketingOpportunityStatusCommand) =>
    http.post<R<SearchMarketingOpportunity>, R<SearchMarketingOpportunity>>(
      `/canvas/search-marketing/opportunities/${opportunityId}/status`,
      payload,
    ),

  /** 基于机会创建 provider 写入建议。 */
  proposeOpportunityMutation: (opportunityId: number, payload: SearchMarketingOpportunityMutationCommand) =>
    http.post<R<SearchMarketingMutation>, R<SearchMarketingMutation>>(
      `/canvas/search-marketing/opportunities/${opportunityId}/mutations`,
      payload,
    ),

  /** 查询 provider 写入队列。 */
  listMutations: (params?: SearchMarketingMutationQuery) =>
    http.get<R<SearchMarketingMutation[]>, R<SearchMarketingMutation[]>>(
      '/canvas/search-marketing/mutations',
      { params },
    ),

  /** 直接创建 provider 写入建议。 */
  proposeMutation: (payload: SearchMarketingMutationCommand) =>
    http.post<R<SearchMarketingMutation>, R<SearchMarketingMutation>>(
      '/canvas/search-marketing/mutations',
      payload,
    ),

  /** 审批 provider 写入。 */
  approveMutation: (mutationId: number, payload: SearchMarketingMutationApprovalCommand) =>
    http.post<R<SearchMarketingMutation>, R<SearchMarketingMutation>>(
      `/canvas/search-marketing/mutations/${mutationId}/approve`,
      payload,
    ),

  /** 执行 provider 写入或 dry-run。 */
  executeMutation: (mutationId: number, payload: SearchMarketingMutationExecuteCommand) =>
    http.post<R<SearchMarketingMutation>, R<SearchMarketingMutation>>(
      `/canvas/search-marketing/mutations/${mutationId}/execute`,
      payload,
    ),

  /** 查询 URL inspection 结果。 */
  listUrlInspections: (params?: SearchMarketingUrlInspectionQuery) =>
    http.get<R<SearchMarketingUrlInspection[]>, R<SearchMarketingUrlInspection[]>>(
      '/canvas/search-marketing/url-inspections',
      { params },
    ),

  /** 查询供应商侧变更。 */
  listProviderChanges: (params?: SearchMarketingProviderChangeQuery) =>
    http.get<R<SearchMarketingProviderChange[]>, R<SearchMarketingProviderChange[]>>(
      '/canvas/search-marketing/provider-changes',
      { params },
    ),

  /** 查询同步任务。 */
  listSyncRuns: (params?: SearchMarketingSyncRunQuery) =>
    http.get<R<SearchMarketingSyncRun[]>, R<SearchMarketingSyncRun[]>>(
      '/canvas/search-marketing/sync-runs',
      { params },
    ),

  /** 手动同步单个来源。 */
  syncSource: (sourceId: number, payload: SearchMarketingSyncRequest) =>
    http.post<R<SearchMarketingSyncRun>, R<SearchMarketingSyncRun>>(
      `/canvas/search-marketing/sources/${sourceId}/sync`,
      payload,
    ),

  /** 批量同步到期来源。 */
  syncDue: (payload?: SearchMarketingSyncDueRequest) =>
    http.post<R<SearchMarketingSyncRun[]>, R<SearchMarketingSyncRun[]>>(
      '/canvas/search-marketing/sources/sync-due',
      payload ?? {},
    ),

  /** 对 provider 写入进行供应商侧对账。 */
  reconcileMutation: (mutationId: number) =>
    http.post<R<SearchMarketingReconciliation>, R<SearchMarketingReconciliation>>(
      `/canvas/search-marketing/mutations/${mutationId}/reconcile`,
      {},
    ),

  /** 查询写入后的影响评估窗口。 */
  listImpactWindows: (params?: SearchMarketingImpactWindowQuery) =>
    http.get<R<SearchMarketingImpactWindow[]>, R<SearchMarketingImpactWindow[]>>(
      '/canvas/search-marketing/impact-windows',
      { params },
    ),

  /** 批量评估到期影响窗口。 */
  evaluateDueImpactWindows: (payload?: SearchMarketingSyncDueRequest) =>
    http.post<R<SearchMarketingImpactWindow[]>, R<SearchMarketingImpactWindow[]>>(
      '/canvas/search-marketing/impact-windows/evaluate-due',
      payload ?? {},
    ),

  /** 获取搜索营销生产就绪状态。 */
  readiness: () =>
    http.get<R<SearchMarketingReadiness>, R<SearchMarketingReadiness>>(
      '/canvas/search-marketing/readiness',
    ),
}
