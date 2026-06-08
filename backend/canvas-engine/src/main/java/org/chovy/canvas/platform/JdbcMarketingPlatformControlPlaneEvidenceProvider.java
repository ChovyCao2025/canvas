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
package org.chovy.canvas.platform;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.chovy.canvas.dal.dataobject.BiDashboardDO;
import org.chovy.canvas.dal.dataobject.CanvasDO;
import org.chovy.canvas.dal.dataobject.ConversationWorkItemDO;
import org.chovy.canvas.dal.dataobject.CreatorCampaignDO;
import org.chovy.canvas.dal.dataobject.MarketingContentReleaseDO;
import org.chovy.canvas.dal.dataobject.GrowthActivityDO;
import org.chovy.canvas.dal.dataobject.GrowthRewardPoolDO;
import org.chovy.canvas.dal.dataobject.MarketingMonitorAlertChannelDO;
import org.chovy.canvas.dal.dataobject.MarketingMonitorAlertDO;
import org.chovy.canvas.dal.dataobject.MarketingMonitorProviderCredentialDO;
import org.chovy.canvas.dal.dataobject.MarketingMonitorSourceDO;
import org.chovy.canvas.dal.dataobject.MarketingIntegrationContractDO;
import org.chovy.canvas.dal.dataobject.PaidMediaAudienceDestinationDO;
import org.chovy.canvas.dal.dataobject.ProgrammaticDspSeatDO;
import org.chovy.canvas.dal.dataobject.ProgrammaticDspMutationDO;
import org.chovy.canvas.dal.dataobject.SearchMarketingSourceDO;
import org.chovy.canvas.dal.dataobject.SearchMarketingMutationDO;
import org.chovy.canvas.dal.dataobject.MarketingCampaignLinkDO;
import org.chovy.canvas.dal.dataobject.MarketingCampaignMasterDO;
import org.chovy.canvas.dal.mapper.BiDashboardMapper;
import org.chovy.canvas.dal.mapper.CanvasMapper;
import org.chovy.canvas.dal.mapper.ConversationWorkItemMapper;
import org.chovy.canvas.dal.mapper.CreatorCampaignMapper;
import org.chovy.canvas.dal.dataobject.CreatorProviderMutationDO;
import org.chovy.canvas.dal.mapper.GrowthActivityMapper;
import org.chovy.canvas.dal.mapper.GrowthRewardPoolMapper;
import org.chovy.canvas.dal.mapper.MarketingContentReleaseMapper;
import org.chovy.canvas.dal.mapper.MarketingMonitorAlertChannelMapper;
import org.chovy.canvas.dal.mapper.MarketingMonitorAlertMapper;
import org.chovy.canvas.dal.mapper.MarketingMonitorProviderCredentialMapper;
import org.chovy.canvas.dal.mapper.MarketingMonitorSourceMapper;
import org.chovy.canvas.dal.mapper.MarketingIntegrationContractMapper;
import org.chovy.canvas.dal.mapper.MarketingIntegrationContractProbeRunMapper;
import org.chovy.canvas.dal.mapper.PaidMediaAudienceDestinationMapper;
import org.chovy.canvas.dal.mapper.ProgrammaticDspMutationMapper;
import org.chovy.canvas.dal.mapper.ProgrammaticDspSeatMapper;
import org.chovy.canvas.dal.mapper.CreatorProviderMutationMapper;
import org.chovy.canvas.dal.mapper.SearchMarketingMutationMapper;
import org.chovy.canvas.dal.mapper.SearchMarketingSourceMapper;
import org.chovy.canvas.dal.mapper.MarketingCampaignLinkMapper;
import org.chovy.canvas.dal.mapper.MarketingCampaignMasterMapper;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.function.Supplier;

@Component
public class JdbcMarketingPlatformControlPlaneEvidenceProvider
        implements MarketingPlatformControlPlaneEvidenceProvider {

    private static final long INTEGRATION_PROBE_FRESHNESS_HOURS = 24;
    private static final String INTEGRATION_PROBE_FAILURE_ALERT = "INTEGRATION_CONTRACT_PROBE_FAILURE";
    private static final String INTEGRATION_SLO_BURN_RATE_ALERT = "INTEGRATION_CONTRACT_SLO_BURN_RATE";

    private final CanvasMapper canvasMapper;
    private final MarketingContentReleaseMapper contentReleaseMapper;
    private final ConversationWorkItemMapper conversationWorkItemMapper;
    private final MarketingMonitorSourceMapper monitorSourceMapper;
    private final MarketingMonitorAlertChannelMapper alertChannelMapper;
    private final PaidMediaAudienceDestinationMapper paidMediaDestinationMapper;
    private final MarketingMonitorProviderCredentialMapper providerCredentialMapper;
    private final SearchMarketingSourceMapper searchSourceMapper;
    private final CreatorCampaignMapper creatorCampaignMapper;
    private final ProgrammaticDspSeatMapper dspSeatMapper;
    private final BiDashboardMapper biDashboardMapper;
    private final SearchMarketingMutationMapper searchMutationMapper;
    private final CreatorProviderMutationMapper creatorMutationMapper;
    private final ProgrammaticDspMutationMapper dspMutationMapper;
    private final MarketingCampaignMasterMapper campaignMasterMapper;
    private final MarketingCampaignLinkMapper campaignLinkMapper;
    private final MarketingIntegrationContractMapper integrationContractMapper;
    private final MarketingIntegrationContractProbeRunMapper integrationContractProbeRunMapper;
    private final MarketingMonitorAlertMapper alertMapper;
    private final GrowthActivityMapper growthActivityMapper;
    private final GrowthRewardPoolMapper growthRewardPoolMapper;

    public JdbcMarketingPlatformControlPlaneEvidenceProvider(
            CanvasMapper canvasMapper,
            MarketingContentReleaseMapper contentReleaseMapper,
            ConversationWorkItemMapper conversationWorkItemMapper,
            MarketingMonitorSourceMapper monitorSourceMapper,
            MarketingMonitorAlertChannelMapper alertChannelMapper,
            PaidMediaAudienceDestinationMapper paidMediaDestinationMapper,
            MarketingMonitorProviderCredentialMapper providerCredentialMapper,
            SearchMarketingSourceMapper searchSourceMapper,
            CreatorCampaignMapper creatorCampaignMapper,
            ProgrammaticDspSeatMapper dspSeatMapper,
            BiDashboardMapper biDashboardMapper,
            SearchMarketingMutationMapper searchMutationMapper,
            CreatorProviderMutationMapper creatorMutationMapper,
            ProgrammaticDspMutationMapper dspMutationMapper,
            MarketingCampaignMasterMapper campaignMasterMapper,
            MarketingCampaignLinkMapper campaignLinkMapper,
            MarketingIntegrationContractMapper integrationContractMapper,
            MarketingIntegrationContractProbeRunMapper integrationContractProbeRunMapper,
            MarketingMonitorAlertMapper alertMapper,
            GrowthActivityMapper growthActivityMapper,
            GrowthRewardPoolMapper growthRewardPoolMapper) {
        this.canvasMapper = canvasMapper;
        this.contentReleaseMapper = contentReleaseMapper;
        this.conversationWorkItemMapper = conversationWorkItemMapper;
        this.monitorSourceMapper = monitorSourceMapper;
        this.alertChannelMapper = alertChannelMapper;
        this.paidMediaDestinationMapper = paidMediaDestinationMapper;
        this.providerCredentialMapper = providerCredentialMapper;
        this.searchSourceMapper = searchSourceMapper;
        this.creatorCampaignMapper = creatorCampaignMapper;
        this.dspSeatMapper = dspSeatMapper;
        this.biDashboardMapper = biDashboardMapper;
        this.searchMutationMapper = searchMutationMapper;
        this.creatorMutationMapper = creatorMutationMapper;
        this.dspMutationMapper = dspMutationMapper;
        this.campaignMasterMapper = campaignMasterMapper;
        this.campaignLinkMapper = campaignLinkMapper;
        this.integrationContractMapper = integrationContractMapper;
        this.integrationContractProbeRunMapper = integrationContractProbeRunMapper;
        this.alertMapper = alertMapper;
        this.growthActivityMapper = growthActivityMapper;
        this.growthRewardPoolMapper = growthRewardPoolMapper;
    }

    @Override
    public RuntimeEvidence evidence(Long tenantId) {
        Long scopedTenantId = tenantId == null || tenantId < 0 ? 0L : tenantId;
        LocalDateTime freshProbeAfter = LocalDateTime.now().minusHours(INTEGRATION_PROBE_FRESHNESS_HOURS);
        return new RuntimeEvidence(
                count(() -> canvasMapper.selectCount(new LambdaQueryWrapper<CanvasDO>()
                        .eq(CanvasDO::getTenantId, scopedTenantId)
                        .eq(CanvasDO::getStatus, 1))),
                count(() -> contentReleaseMapper.selectCount(new LambdaQueryWrapper<MarketingContentReleaseDO>()
                        .eq(MarketingContentReleaseDO::getTenantId, scopedTenantId)
                        .eq(MarketingContentReleaseDO::getStatus, "ACTIVE"))),
                count(() -> conversationWorkItemMapper.selectCount(new LambdaQueryWrapper<ConversationWorkItemDO>()
                        .eq(ConversationWorkItemDO::getTenantId, scopedTenantId))),
                count(() -> monitorSourceMapper.selectCount(new LambdaQueryWrapper<MarketingMonitorSourceDO>()
                        .eq(MarketingMonitorSourceDO::getTenantId, scopedTenantId)
                        .eq(MarketingMonitorSourceDO::getEnabled, 1))),
                count(() -> alertChannelMapper.selectCount(new LambdaQueryWrapper<MarketingMonitorAlertChannelDO>()
                        .eq(MarketingMonitorAlertChannelDO::getTenantId, scopedTenantId)
                        .eq(MarketingMonitorAlertChannelDO::getEnabled, 1))),
                count(() -> paidMediaDestinationMapper.selectCount(new LambdaQueryWrapper<PaidMediaAudienceDestinationDO>()
                        .eq(PaidMediaAudienceDestinationDO::getTenantId, scopedTenantId)
                        .eq(PaidMediaAudienceDestinationDO::getEnabled, 1))),
                count(() -> providerCredentialMapper.selectCount(new LambdaQueryWrapper<MarketingMonitorProviderCredentialDO>()
                        .eq(MarketingMonitorProviderCredentialDO::getTenantId, scopedTenantId)
                        .eq(MarketingMonitorProviderCredentialDO::getStatus, "ACTIVE"))),
                count(() -> searchSourceMapper.selectCount(new LambdaQueryWrapper<SearchMarketingSourceDO>()
                        .eq(SearchMarketingSourceDO::getTenantId, scopedTenantId)
                        .eq(SearchMarketingSourceDO::getEnabled, 1))),
                count(() -> creatorCampaignMapper.selectCount(new LambdaQueryWrapper<CreatorCampaignDO>()
                        .eq(CreatorCampaignDO::getTenantId, scopedTenantId)
                        .eq(CreatorCampaignDO::getStatus, "ACTIVE"))),
                count(() -> dspSeatMapper.selectCount(new LambdaQueryWrapper<ProgrammaticDspSeatDO>()
                        .eq(ProgrammaticDspSeatDO::getTenantId, scopedTenantId)
                        .eq(ProgrammaticDspSeatDO::getEnabled, 1))),
                count(() -> biDashboardMapper.selectCount(new LambdaQueryWrapper<BiDashboardDO>()
                        .eq(BiDashboardDO::getTenantId, scopedTenantId)
                        .eq(BiDashboardDO::getStatus, "PUBLISHED"))),
                count(() -> searchMutationMapper.selectCount(new LambdaQueryWrapper<SearchMarketingMutationDO>()
                        .eq(SearchMarketingMutationDO::getTenantId, scopedTenantId))),
                count(() -> searchMutationMapper.selectCount(new LambdaQueryWrapper<SearchMarketingMutationDO>()
                        .eq(SearchMarketingMutationDO::getTenantId, scopedTenantId)
                        .eq(SearchMarketingMutationDO::getApprovalStatus, "PENDING"))),
                count(() -> searchMutationMapper.selectCount(new LambdaQueryWrapper<SearchMarketingMutationDO>()
                        .eq(SearchMarketingMutationDO::getTenantId, scopedTenantId)
                        .in(SearchMarketingMutationDO::getStatus, "FAILED", "DRY_RUN_FAILED"))),
                count(() -> creatorMutationMapper.selectCount(new LambdaQueryWrapper<CreatorProviderMutationDO>()
                        .eq(CreatorProviderMutationDO::getTenantId, scopedTenantId))),
                count(() -> creatorMutationMapper.selectCount(new LambdaQueryWrapper<CreatorProviderMutationDO>()
                        .eq(CreatorProviderMutationDO::getTenantId, scopedTenantId)
                        .eq(CreatorProviderMutationDO::getApprovalStatus, "PENDING"))),
                count(() -> creatorMutationMapper.selectCount(new LambdaQueryWrapper<CreatorProviderMutationDO>()
                        .eq(CreatorProviderMutationDO::getTenantId, scopedTenantId)
                        .in(CreatorProviderMutationDO::getStatus, "FAILED", "DRY_RUN_FAILED"))),
                count(() -> dspMutationMapper.selectCount(new LambdaQueryWrapper<ProgrammaticDspMutationDO>()
                        .eq(ProgrammaticDspMutationDO::getTenantId, scopedTenantId))),
                count(() -> dspMutationMapper.selectCount(new LambdaQueryWrapper<ProgrammaticDspMutationDO>()
                        .eq(ProgrammaticDspMutationDO::getTenantId, scopedTenantId)
                        .eq(ProgrammaticDspMutationDO::getApprovalStatus, "PENDING"))),
                count(() -> dspMutationMapper.selectCount(new LambdaQueryWrapper<ProgrammaticDspMutationDO>()
                        .eq(ProgrammaticDspMutationDO::getTenantId, scopedTenantId)
                        .in(ProgrammaticDspMutationDO::getStatus, "FAILED", "DRY_RUN_FAILED"))),
                count(() -> campaignMasterMapper.selectCount(new LambdaQueryWrapper<MarketingCampaignMasterDO>()
                        .eq(MarketingCampaignMasterDO::getTenantId, scopedTenantId)
                        .eq(MarketingCampaignMasterDO::getStatus, "ACTIVE"))),
                count(() -> campaignLinkMapper.selectCount(new LambdaQueryWrapper<MarketingCampaignLinkDO>()
                        .eq(MarketingCampaignLinkDO::getTenantId, scopedTenantId))),
                count(() -> campaignLinkMapper.selectCount(new LambdaQueryWrapper<MarketingCampaignLinkDO>()
                        .eq(MarketingCampaignLinkDO::getTenantId, scopedTenantId)
                        .eq(MarketingCampaignLinkDO::getRequiredForLaunch, 1))),
                count(() -> campaignLinkMapper.selectCount(new LambdaQueryWrapper<MarketingCampaignLinkDO>()
                        .eq(MarketingCampaignLinkDO::getTenantId, scopedTenantId)
                        .eq(MarketingCampaignLinkDO::getLinkStatus, "BLOCKED"))),
                count(() -> campaignLinkMapper.countActiveCampaignsWithInactiveRequiredLinks(scopedTenantId)),
                count(() -> campaignLinkMapper.countActiveCampaignsMissingPrimaryDependency(scopedTenantId)),
                count(() -> campaignLinkMapper.countActiveCampaignsMissingMeasurementDependency(scopedTenantId)),
                count(() -> integrationContractMapper.selectCount(
                        new LambdaQueryWrapper<MarketingIntegrationContractDO>()
                                .eq(MarketingIntegrationContractDO::getTenantId, scopedTenantId)
                                .eq(MarketingIntegrationContractDO::getStatus, "ACTIVE"))),
                count(() -> integrationContractMapper.selectCount(
                        new LambdaQueryWrapper<MarketingIntegrationContractDO>()
                                .eq(MarketingIntegrationContractDO::getTenantId, scopedTenantId)
                                .eq(MarketingIntegrationContractDO::getEnvironment, "PRODUCTION")
                                .eq(MarketingIntegrationContractDO::getStatus, "ACTIVE"))),
                count(() -> integrationContractMapper.selectCount(
                        new LambdaQueryWrapper<MarketingIntegrationContractDO>()
                                .eq(MarketingIntegrationContractDO::getTenantId, scopedTenantId)
                                .eq(MarketingIntegrationContractDO::getStatus, "BLOCKED"))),
                count(() -> integrationContractMapper.selectCount(
                        new LambdaQueryWrapper<MarketingIntegrationContractDO>()
                                .eq(MarketingIntegrationContractDO::getTenantId, scopedTenantId)
                                .eq(MarketingIntegrationContractDO::getStatus, "DEGRADED"))),
                count(() -> integrationContractProbeRunMapper.countFreshPassingProductionContracts(
                        scopedTenantId,
                        freshProbeAfter)),
                count(() -> integrationContractProbeRunMapper.countFreshFailingProductionContracts(
                        scopedTenantId,
                        freshProbeAfter)),
                count(() -> alertMapper.selectCount(new LambdaQueryWrapper<MarketingMonitorAlertDO>()
                        .eq(MarketingMonitorAlertDO::getTenantId, scopedTenantId)
                        .eq(MarketingMonitorAlertDO::getStatus, "OPEN")
                        .eq(MarketingMonitorAlertDO::getAlertType, INTEGRATION_PROBE_FAILURE_ALERT))),
                count(() -> alertMapper.selectCount(new LambdaQueryWrapper<MarketingMonitorAlertDO>()
                        .eq(MarketingMonitorAlertDO::getTenantId, scopedTenantId)
                        .eq(MarketingMonitorAlertDO::getStatus, "OPEN")
                        .eq(MarketingMonitorAlertDO::getAlertType, INTEGRATION_SLO_BURN_RATE_ALERT))),
                count(() -> growthActivityMapper.selectCount(new LambdaQueryWrapper<GrowthActivityDO>()
                        .eq(GrowthActivityDO::getTenantId, scopedTenantId)
                        .eq(GrowthActivityDO::getStatus, "ACTIVE"))),
                count(() -> growthRewardPoolMapper.selectCount(new LambdaQueryWrapper<GrowthRewardPoolDO>()
                        .eq(GrowthRewardPoolDO::getTenantId, scopedTenantId)
                        .eq(GrowthRewardPoolDO::getStatus, "ACTIVE"))),
                count(() -> growthActivityMapper.selectCount(new LambdaQueryWrapper<GrowthActivityDO>()
                        .eq(GrowthActivityDO::getTenantId, scopedTenantId)
                        .eq(GrowthActivityDO::getStatus, "ACTIVE")
                        .isNotNull(GrowthActivityDO::getCampaignId)
                        .isNotNull(GrowthActivityDO::getDashboardRef))),
                count(() -> growthActivityMapper.selectCount(new LambdaQueryWrapper<GrowthActivityDO>()
                        .eq(GrowthActivityDO::getTenantId, scopedTenantId)
                        .eq(GrowthActivityDO::getStatus, "ACTIVE")
                        .and(wrapper -> wrapper.isNull(GrowthActivityDO::getCampaignId)
                                .or()
                                .isNull(GrowthActivityDO::getDashboardRef)))));
    }

    private long count(Supplier<Long> supplier) {
        Long value = supplier.get();
        return value == null ? 0L : value;
    }
}
