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

export const marketingPlatformApi = {
  controlPlane: () =>
    http.get<R<MarketingPlatformControlPlaneSummary>, R<MarketingPlatformControlPlaneSummary>>(
      '/canvas/marketing-platform/control-plane',
    ),

  listMarketingIntegrationContracts: (params?: MarketingIntegrationContractQuery) =>
    http.get<R<MarketingIntegrationContract[]>, R<MarketingIntegrationContract[]>>(
      '/canvas/marketing-integrations/contracts',
      { params },
    ),

  upsertMarketingIntegrationContract: (payload: MarketingIntegrationContractCommand) =>
    http.post<R<MarketingIntegrationContract>, R<MarketingIntegrationContract>>(
      '/canvas/marketing-integrations/contracts',
      payload,
    ),

  archiveMarketingIntegrationContract: (contractId: number) =>
    http.delete<R<MarketingIntegrationContract>, R<MarketingIntegrationContract>>(
      `/canvas/marketing-integrations/contracts/${contractId}`,
    ),

  listMarketingIntegrationContractAuditEvents: (contractId: number, params?: { limit?: number }) =>
    http.get<R<MarketingIntegrationContractAuditEvent[]>, R<MarketingIntegrationContractAuditEvent[]>>(
      `/canvas/marketing-integrations/contracts/${contractId}/audit-events`,
      { params },
    ),

  listMarketingIntegrationContractProbes: (
    contractId: number,
    params?: Pick<MarketingIntegrationContractProbeQuery, 'limit'>,
  ) =>
    http.get<R<MarketingIntegrationContractProbe[]>, R<MarketingIntegrationContractProbe[]>>(
      `/canvas/marketing-integrations/contracts/${contractId}/probes`,
      { params },
    ),

  listRecentMarketingIntegrationContractProbes: (params?: MarketingIntegrationContractProbeQuery) =>
    http.get<R<MarketingIntegrationContractProbe[]>, R<MarketingIntegrationContractProbe[]>>(
      '/canvas/marketing-integrations/probes',
      { params },
    ),

  recordMarketingIntegrationContractProbe: (
    contractId: number,
    payload: MarketingIntegrationContractProbeCommand,
  ) =>
    http.post<R<MarketingIntegrationContractProbe>, R<MarketingIntegrationContractProbe>>(
      `/canvas/marketing-integrations/contracts/${contractId}/probes`,
      payload,
    ),

  listMarketingIntegrationContractProbeRuns: (params?: MarketingIntegrationContractProbeRunQuery) =>
    http.get<R<MarketingIntegrationContractProbeRun[]>, R<MarketingIntegrationContractProbeRun[]>>(
      '/canvas/marketing-integrations/contract-probe-runs',
      { params },
    ),

  listMarketingIntegrationContractSloEvaluations: (params?: Pick<MarketingIntegrationContractProbeQuery, 'limit'>) =>
    http.get<R<MarketingIntegrationContractSloEvaluation[]>, R<MarketingIntegrationContractSloEvaluation[]>>(
      '/canvas/marketing-integrations/contract-slo-evaluations',
      { params },
    ),

  recordMarketingIntegrationContractProbeRun: (
    contractId: number,
    payload: MarketingIntegrationContractProbeRunCommand,
  ) =>
    http.post<R<MarketingIntegrationContractProbeRun>, R<MarketingIntegrationContractProbeRun>>(
      `/canvas/marketing-integrations/contracts/${contractId}/probe-runs`,
      payload,
    ),

  scanMarketingIntegrationContractProbes: (params?: Pick<MarketingIntegrationContractProbeQuery, 'limit'>) =>
    http.post<
      R<MarketingIntegrationContractProbeAutomationSummary>,
      R<MarketingIntegrationContractProbeAutomationSummary>
    >(
      '/canvas/marketing-integrations/contract-probe-runs/scan',
      undefined,
      { params },
    ),

  listMarketingCampaigns: (params?: MarketingCampaignQuery) =>
    http.get<R<MarketingCampaign[]>, R<MarketingCampaign[]>>(
      '/canvas/marketing-campaigns',
      { params },
    ),

  upsertMarketingCampaign: (payload: MarketingCampaignCommand) =>
    http.post<R<MarketingCampaign>, R<MarketingCampaign>>(
      '/canvas/marketing-campaigns',
      payload,
    ),

  listMarketingCampaignLinks: (campaignId: number) =>
    http.get<R<MarketingCampaignLink[]>, R<MarketingCampaignLink[]>>(
      `/canvas/marketing-campaigns/${campaignId}/links`,
    ),

  getMarketingCampaignReadiness: (campaignId: number) =>
    http.get<R<MarketingCampaignReadiness>, R<MarketingCampaignReadiness>>(
      `/canvas/marketing-campaigns/${campaignId}/readiness`,
    ),

  linkMarketingCampaignResource: (payload: MarketingCampaignLinkCommand) =>
    http.post<R<MarketingCampaignLink>, R<MarketingCampaignLink>>(
      '/canvas/marketing-campaigns/links',
      payload,
    ),

  unlinkMarketingCampaignResource: (linkId: number) =>
    http.delete<R<void>, R<void>>(
      `/canvas/marketing-campaigns/links/${linkId}`,
    ),

  listSearchMarketingMutations: (params?: SearchMarketingMutationQuery) =>
    http.get<R<SearchMarketingMutation[]>, R<SearchMarketingMutation[]>>(
      '/canvas/search-marketing/mutations',
      { params },
    ),

  approveSearchMarketingMutation: (mutationId: number, payload: ProviderMutationApprovalCommand) =>
    http.post<R<SearchMarketingMutation>, R<SearchMarketingMutation>>(
      `/canvas/search-marketing/mutations/${mutationId}/approve`,
      payload,
    ),

  executeSearchMarketingMutation: (mutationId: number, payload: ProviderMutationExecuteCommand) =>
    http.post<R<SearchMarketingMutation>, R<SearchMarketingMutation>>(
      `/canvas/search-marketing/mutations/${mutationId}/execute`,
      payload,
    ),

  listCreatorProviderMutations: (params?: CreatorProviderMutationQuery) =>
    http.get<R<CreatorProviderMutation[]>, R<CreatorProviderMutation[]>>(
      '/canvas/creator-collaboration/mutations',
      { params },
    ),

  approveCreatorProviderMutation: (mutationId: number, payload: ProviderMutationApprovalCommand) =>
    http.post<R<CreatorProviderMutation>, R<CreatorProviderMutation>>(
      `/canvas/creator-collaboration/mutations/${mutationId}/approve`,
      payload,
    ),

  executeCreatorProviderMutation: (mutationId: number, payload: ProviderMutationExecuteCommand) =>
    http.post<R<CreatorProviderMutation>, R<CreatorProviderMutation>>(
      `/canvas/creator-collaboration/mutations/${mutationId}/execute`,
      payload,
    ),

  listProgrammaticDspMutations: (params?: ProgrammaticDspMutationQuery) =>
    http.get<R<ProgrammaticDspMutation[]>, R<ProgrammaticDspMutation[]>>(
      '/canvas/programmatic-dsp/mutations',
      { params },
    ),

  approveProgrammaticDspMutation: (mutationId: number, payload: ProviderMutationApprovalCommand) =>
    http.post<R<ProgrammaticDspMutation>, R<ProgrammaticDspMutation>>(
      `/canvas/programmatic-dsp/mutations/${mutationId}/approve`,
      payload,
    ),

  executeProgrammaticDspMutation: (mutationId: number, payload: ProviderMutationExecuteCommand) =>
    http.post<R<ProgrammaticDspMutation>, R<ProgrammaticDspMutation>>(
      `/canvas/programmatic-dsp/mutations/${mutationId}/execute`,
      payload,
    ),
}
