import http from './api'
import type { R } from '../types'
import type {
  CreatorProviderMutation,
  CreatorProviderMutationQuery,
  MarketingCampaign,
  MarketingCampaignCommand,
  MarketingCampaignLink,
  MarketingCampaignLinkCommand,
  MarketingCampaignQuery,
  MarketingCampaignReadiness,
  MarketingIntegrationContractAuditEvent,
  MarketingIntegrationContract,
  MarketingIntegrationContractCommand,
  MarketingIntegrationContractProbe,
  MarketingIntegrationContractProbeAutomationSummary,
  MarketingIntegrationContractProbeCommand,
  MarketingIntegrationContractProbeRun,
  MarketingIntegrationContractProbeRunCommand,
  MarketingIntegrationContractProbeRunQuery,
  MarketingIntegrationContractProbeQuery,
  MarketingIntegrationContractQuery,
  MarketingIntegrationContractSloEvaluation,
  MarketingPlatformControlPlaneSummary,
  ProgrammaticDspMutation,
  ProgrammaticDspMutationQuery,
  ProviderMutationApprovalCommand,
  ProviderMutationExecuteCommand,
  SearchMarketingMutation,
  SearchMarketingMutationQuery,
} from '../pages/marketing-platform/marketingPlatformControlPlane'

/** 营销平台控制面 API，覆盖能力总览、集成契约、Campaign 和 provider 写入治理。 */
export const marketingPlatformApi = {
  /** 获取营销平台控制面总览。 */
  controlPlane: () =>
    http.get<R<MarketingPlatformControlPlaneSummary>, R<MarketingPlatformControlPlaneSummary>>(
      '/canvas/marketing-platform/control-plane',
    ),

  /** 查询集成契约注册表。 */
  listMarketingIntegrationContracts: (params?: MarketingIntegrationContractQuery) =>
    http.get<R<MarketingIntegrationContract[]>, R<MarketingIntegrationContract[]>>(
      '/canvas/marketing-integrations/contracts',
      { params },
    ),

  /** 新建或更新集成契约。 */
  upsertMarketingIntegrationContract: (payload: MarketingIntegrationContractCommand) =>
    http.post<R<MarketingIntegrationContract>, R<MarketingIntegrationContract>>(
      '/canvas/marketing-integrations/contracts',
      payload,
    ),

  /** 归档集成契约。 */
  archiveMarketingIntegrationContract: (contractId: number) =>
    http.delete<R<MarketingIntegrationContract>, R<MarketingIntegrationContract>>(
      `/canvas/marketing-integrations/contracts/${contractId}`,
    ),

  /** 查询集成契约审计事件。 */
  listMarketingIntegrationContractAuditEvents: (contractId: number, params?: { limit?: number }) =>
    http.get<R<MarketingIntegrationContractAuditEvent[]>, R<MarketingIntegrationContractAuditEvent[]>>(
      `/canvas/marketing-integrations/contracts/${contractId}/audit-events`,
      { params },
    ),

  /** 查询单个契约的探针记录。 */
  listMarketingIntegrationContractProbes: (
    contractId: number,
    params?: Pick<MarketingIntegrationContractProbeQuery, 'limit'>,
  ) =>
    http.get<R<MarketingIntegrationContractProbe[]>, R<MarketingIntegrationContractProbe[]>>(
      `/canvas/marketing-integrations/contracts/${contractId}/probes`,
      { params },
    ),

  /** 查询最近集成契约探针记录。 */
  listRecentMarketingIntegrationContractProbes: (params?: MarketingIntegrationContractProbeQuery) =>
    http.get<R<MarketingIntegrationContractProbe[]>, R<MarketingIntegrationContractProbe[]>>(
      '/canvas/marketing-integrations/probes',
      { params },
    ),

  /** 记录单个集成契约探针。 */
  recordMarketingIntegrationContractProbe: (
    contractId: number,
    payload: MarketingIntegrationContractProbeCommand,
  ) =>
    http.post<R<MarketingIntegrationContractProbe>, R<MarketingIntegrationContractProbe>>(
      `/canvas/marketing-integrations/contracts/${contractId}/probes`,
      payload,
    ),

  /** 查询探针运行记录。 */
  listMarketingIntegrationContractProbeRuns: (params?: MarketingIntegrationContractProbeRunQuery) =>
    http.get<R<MarketingIntegrationContractProbeRun[]>, R<MarketingIntegrationContractProbeRun[]>>(
      '/canvas/marketing-integrations/contract-probe-runs',
      { params },
    ),

  /** 查询集成契约 SLO burn-rate 评估。 */
  listMarketingIntegrationContractSloEvaluations: (params?: Pick<MarketingIntegrationContractProbeQuery, 'limit'>) =>
    http.get<R<MarketingIntegrationContractSloEvaluation[]>, R<MarketingIntegrationContractSloEvaluation[]>>(
      '/canvas/marketing-integrations/contract-slo-evaluations',
      { params },
    ),

  /** 记录探针运行结果。 */
  recordMarketingIntegrationContractProbeRun: (
    contractId: number,
    payload: MarketingIntegrationContractProbeRunCommand,
  ) =>
    http.post<R<MarketingIntegrationContractProbeRun>, R<MarketingIntegrationContractProbeRun>>(
      `/canvas/marketing-integrations/contracts/${contractId}/probe-runs`,
      payload,
    ),

  /** 触发自动探针扫描。 */
  scanMarketingIntegrationContractProbes: (params?: Pick<MarketingIntegrationContractProbeQuery, 'limit'>) =>
    http.post<
      R<MarketingIntegrationContractProbeAutomationSummary>,
      R<MarketingIntegrationContractProbeAutomationSummary>
    >(
      '/canvas/marketing-integrations/contract-probe-runs/scan',
      undefined,
      { params },
    ),

  /** 查询 Campaign 主账本。 */
  listMarketingCampaigns: (params?: MarketingCampaignQuery) =>
    http.get<R<MarketingCampaign[]>, R<MarketingCampaign[]>>(
      '/canvas/marketing-campaigns',
      { params },
    ),

  /** 新建或更新 Campaign 主记录。 */
  upsertMarketingCampaign: (payload: MarketingCampaignCommand) =>
    http.post<R<MarketingCampaign>, R<MarketingCampaign>>(
      '/canvas/marketing-campaigns',
      payload,
    ),

  /** 查询 Campaign 绑定资源。 */
  listMarketingCampaignLinks: (campaignId: number) =>
    http.get<R<MarketingCampaignLink[]>, R<MarketingCampaignLink[]>>(
      `/canvas/marketing-campaigns/${campaignId}/links`,
    ),

  /** 获取 Campaign 上线闸口评估。 */
  getMarketingCampaignReadiness: (campaignId: number) =>
    http.get<R<MarketingCampaignReadiness>, R<MarketingCampaignReadiness>>(
      `/canvas/marketing-campaigns/${campaignId}/readiness`,
    ),

  /** 绑定 Campaign 资源依赖。 */
  linkMarketingCampaignResource: (payload: MarketingCampaignLinkCommand) =>
    http.post<R<MarketingCampaignLink>, R<MarketingCampaignLink>>(
      '/canvas/marketing-campaigns/links',
      payload,
    ),

  /** 解绑 Campaign 资源依赖。 */
  unlinkMarketingCampaignResource: (linkId: number) =>
    http.delete<R<void>, R<void>>(
      `/canvas/marketing-campaigns/links/${linkId}`,
    ),

  /** 查询搜索营销 provider 写入。 */
  listSearchMarketingMutations: (params?: SearchMarketingMutationQuery) =>
    http.get<R<SearchMarketingMutation[]>, R<SearchMarketingMutation[]>>(
      '/canvas/search-marketing/mutations',
      { params },
    ),

  /** 审批搜索营销 provider 写入。 */
  approveSearchMarketingMutation: (mutationId: number, payload: ProviderMutationApprovalCommand) =>
    http.post<R<SearchMarketingMutation>, R<SearchMarketingMutation>>(
      `/canvas/search-marketing/mutations/${mutationId}/approve`,
      payload,
    ),

  /** 执行搜索营销 provider 写入或 dry-run。 */
  executeSearchMarketingMutation: (mutationId: number, payload: ProviderMutationExecuteCommand) =>
    http.post<R<SearchMarketingMutation>, R<SearchMarketingMutation>>(
      `/canvas/search-marketing/mutations/${mutationId}/execute`,
      payload,
    ),

  /** 查询达人协作 provider 写入。 */
  listCreatorProviderMutations: (params?: CreatorProviderMutationQuery) =>
    http.get<R<CreatorProviderMutation[]>, R<CreatorProviderMutation[]>>(
      '/canvas/creator-collaboration/mutations',
      { params },
    ),

  /** 审批达人协作 provider 写入。 */
  approveCreatorProviderMutation: (mutationId: number, payload: ProviderMutationApprovalCommand) =>
    http.post<R<CreatorProviderMutation>, R<CreatorProviderMutation>>(
      `/canvas/creator-collaboration/mutations/${mutationId}/approve`,
      payload,
    ),

  /** 执行达人协作 provider 写入或 dry-run。 */
  executeCreatorProviderMutation: (mutationId: number, payload: ProviderMutationExecuteCommand) =>
    http.post<R<CreatorProviderMutation>, R<CreatorProviderMutation>>(
      `/canvas/creator-collaboration/mutations/${mutationId}/execute`,
      payload,
    ),

  /** 查询程序化 DSP provider 写入。 */
  listProgrammaticDspMutations: (params?: ProgrammaticDspMutationQuery) =>
    http.get<R<ProgrammaticDspMutation[]>, R<ProgrammaticDspMutation[]>>(
      '/canvas/programmatic-dsp/mutations',
      { params },
    ),

  /** 审批程序化 DSP provider 写入。 */
  approveProgrammaticDspMutation: (mutationId: number, payload: ProviderMutationApprovalCommand) =>
    http.post<R<ProgrammaticDspMutation>, R<ProgrammaticDspMutation>>(
      `/canvas/programmatic-dsp/mutations/${mutationId}/approve`,
      payload,
    ),

  /** 执行程序化 DSP provider 写入或 dry-run。 */
  executeProgrammaticDspMutation: (mutationId: number, payload: ProviderMutationExecuteCommand) =>
    http.post<R<ProgrammaticDspMutation>, R<ProgrammaticDspMutation>>(
      `/canvas/programmatic-dsp/mutations/${mutationId}/execute`,
      payload,
    ),
}
