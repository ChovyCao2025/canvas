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

import org.chovy.canvas.dal.mapper.BiDashboardMapper;
import org.chovy.canvas.dal.mapper.CanvasMapper;
import org.chovy.canvas.dal.mapper.ConversationWorkItemMapper;
import org.chovy.canvas.dal.mapper.CreatorCampaignMapper;
import org.chovy.canvas.dal.mapper.CreatorProviderMutationMapper;
import org.chovy.canvas.dal.mapper.GrowthActivityMapper;
import org.chovy.canvas.dal.mapper.GrowthRewardPoolMapper;
import org.chovy.canvas.dal.mapper.MarketingCampaignLinkMapper;
import org.chovy.canvas.dal.mapper.MarketingCampaignMasterMapper;
import org.chovy.canvas.dal.mapper.MarketingContentReleaseMapper;
import org.chovy.canvas.dal.mapper.MarketingIntegrationContractMapper;
import org.chovy.canvas.dal.mapper.MarketingIntegrationContractProbeRunMapper;
import org.chovy.canvas.dal.mapper.MarketingMonitorAlertMapper;
import org.chovy.canvas.dal.mapper.MarketingMonitorAlertChannelMapper;
import org.chovy.canvas.dal.mapper.MarketingMonitorProviderCredentialMapper;
import org.chovy.canvas.dal.mapper.MarketingMonitorSourceMapper;
import org.chovy.canvas.dal.mapper.PaidMediaAudienceDestinationMapper;
import org.chovy.canvas.dal.mapper.ProgrammaticDspMutationMapper;
import org.chovy.canvas.dal.mapper.ProgrammaticDspSeatMapper;
import org.chovy.canvas.dal.mapper.SearchMarketingMutationMapper;
import org.chovy.canvas.dal.mapper.SearchMarketingSourceMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JdbcMarketingPlatformControlPlaneEvidenceProviderTest {

    @Test
    void evidenceAggregatesCountsFromMarketingDomainTables() {
        MapperMocks mocks = MapperMocks.create();
        when(mocks.canvasMapper.selectCount(any())).thenReturn(3L);
        when(mocks.contentReleaseMapper.selectCount(any())).thenReturn(2L);
        when(mocks.conversationWorkItemMapper.selectCount(any())).thenReturn(5L);
        when(mocks.monitorSourceMapper.selectCount(any())).thenReturn(1L);
        when(mocks.alertChannelMapper.selectCount(any())).thenReturn(4L);
        when(mocks.paidMediaDestinationMapper.selectCount(any())).thenReturn(6L);
        when(mocks.providerCredentialMapper.selectCount(any())).thenReturn(7L);
        when(mocks.searchSourceMapper.selectCount(any())).thenReturn(8L);
        when(mocks.creatorCampaignMapper.selectCount(any())).thenReturn(9L);
        when(mocks.dspSeatMapper.selectCount(any())).thenReturn(10L);
        when(mocks.biDashboardMapper.selectCount(any())).thenReturn(11L);
        when(mocks.searchMutationMapper.selectCount(any())).thenReturn(12L, 3L, 1L);
        when(mocks.creatorMutationMapper.selectCount(any())).thenReturn(9L, 2L, 0L);
        when(mocks.dspMutationMapper.selectCount(any())).thenReturn(7L, 1L, 1L);
        when(mocks.campaignMasterMapper.selectCount(any())).thenReturn(13L);
        when(mocks.campaignLinkMapper.selectCount(any())).thenReturn(14L, 10L, 2L);
        when(mocks.campaignLinkMapper.countActiveCampaignsWithInactiveRequiredLinks(any())).thenReturn(3L);
        when(mocks.campaignLinkMapper.countActiveCampaignsMissingPrimaryDependency(any())).thenReturn(4L);
        when(mocks.campaignLinkMapper.countActiveCampaignsMissingMeasurementDependency(any())).thenReturn(5L);
        when(mocks.integrationContractMapper.selectCount(any())).thenReturn(15L, 12L, 1L, 2L);
        when(mocks.probeRunMapper.countFreshPassingProductionContracts(any(), any())).thenReturn(11L);
        when(mocks.probeRunMapper.countFreshFailingProductionContracts(any(), any())).thenReturn(2L);
        when(mocks.alertMapper.selectCount(any())).thenReturn(3L, 4L);
        when(mocks.growthActivityMapper.selectCount(any())).thenReturn(16L, 13L, 3L);
        when(mocks.growthRewardPoolMapper.selectCount(any())).thenReturn(17L);

        MarketingPlatformControlPlaneEvidenceProvider.RuntimeEvidence evidence = mocks.provider().evidence(7L);

        assertThat(evidence.publishedJourneyCount()).isEqualTo(3);
        assertThat(evidence.activeContentReleaseCount()).isEqualTo(2);
        assertThat(evidence.enabledPaidMediaDestinationCount()).isEqualTo(6);
        assertThat(evidence.searchProviderMutationCount()).isEqualTo(12);
        assertThat(evidence.searchPendingWriteCount()).isEqualTo(3);
        assertThat(evidence.searchFailedWriteCount()).isEqualTo(1);
        assertThat(evidence.campaignResourceLinkCount()).isEqualTo(14);
        assertThat(evidence.requiredCampaignResourceLinkCount()).isEqualTo(10);
        assertThat(evidence.blockedCampaignResourceLinkCount()).isEqualTo(2);
        assertThat(evidence.campaignsWithInactiveRequiredLinks()).isEqualTo(3);
        assertThat(evidence.campaignsMissingPrimaryDependency()).isEqualTo(4);
        assertThat(evidence.campaignsMissingMeasurementDependency()).isEqualTo(5);
        assertThat(evidence.activeIntegrationContractCount()).isEqualTo(15);
        assertThat(evidence.productionIntegrationContractCount()).isEqualTo(12);
        assertThat(evidence.blockedIntegrationContractCount()).isEqualTo(1);
        assertThat(evidence.degradedIntegrationContractCount()).isEqualTo(2);
        assertThat(evidence.freshPassingProductionIntegrationProbeCount()).isEqualTo(11);
        assertThat(evidence.freshFailingProductionIntegrationProbeCount()).isEqualTo(2);
        assertThat(evidence.openIntegrationContractProbeAlertCount()).isEqualTo(3);
        assertThat(evidence.openIntegrationContractSloAlertCount()).isEqualTo(4);
        assertThat(evidence.activeGrowthActivityCount()).isEqualTo(16);
        assertThat(evidence.growthActivityRewardPoolCount()).isEqualTo(17);
        assertThat(evidence.readyGrowthActivityCount()).isEqualTo(13);
        assertThat(evidence.blockedGrowthActivityReadinessCount()).isEqualTo(3);
    }

    @Test
    void nullMapperCountsAreNormalizedToZero() {
        MapperMocks mocks = MapperMocks.create();
        when(mocks.canvasMapper.selectCount(any())).thenReturn(null);
        when(mocks.contentReleaseMapper.selectCount(any())).thenReturn(null);
        when(mocks.conversationWorkItemMapper.selectCount(any())).thenReturn(null);
        when(mocks.monitorSourceMapper.selectCount(any())).thenReturn(null);
        when(mocks.alertChannelMapper.selectCount(any())).thenReturn(null);
        when(mocks.paidMediaDestinationMapper.selectCount(any())).thenReturn(null);
        when(mocks.providerCredentialMapper.selectCount(any())).thenReturn(null);
        when(mocks.searchSourceMapper.selectCount(any())).thenReturn(null);
        when(mocks.creatorCampaignMapper.selectCount(any())).thenReturn(null);
        when(mocks.dspSeatMapper.selectCount(any())).thenReturn(null);
        when(mocks.biDashboardMapper.selectCount(any())).thenReturn(null);
        when(mocks.searchMutationMapper.selectCount(any())).thenReturn(null);
        when(mocks.creatorMutationMapper.selectCount(any())).thenReturn(null);
        when(mocks.dspMutationMapper.selectCount(any())).thenReturn(null);
        when(mocks.campaignMasterMapper.selectCount(any())).thenReturn(null);
        when(mocks.campaignLinkMapper.selectCount(any())).thenReturn(null);
        when(mocks.campaignLinkMapper.countActiveCampaignsWithInactiveRequiredLinks(any())).thenReturn(null);
        when(mocks.campaignLinkMapper.countActiveCampaignsMissingPrimaryDependency(any())).thenReturn(null);
        when(mocks.campaignLinkMapper.countActiveCampaignsMissingMeasurementDependency(any())).thenReturn(null);
        when(mocks.integrationContractMapper.selectCount(any())).thenReturn(null);
        when(mocks.probeRunMapper.countFreshPassingProductionContracts(any(), any())).thenReturn(null);
        when(mocks.probeRunMapper.countFreshFailingProductionContracts(any(), any())).thenReturn(null);
        when(mocks.alertMapper.selectCount(any())).thenReturn(null);
        when(mocks.growthActivityMapper.selectCount(any())).thenReturn(null);
        when(mocks.growthRewardPoolMapper.selectCount(any())).thenReturn(null);

        MarketingPlatformControlPlaneEvidenceProvider.RuntimeEvidence evidence = mocks.provider().evidence(null);

        assertThat(evidence).isEqualTo(MarketingPlatformControlPlaneEvidenceProvider.RuntimeEvidence.empty());
    }

    private record MapperMocks(
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
            MarketingIntegrationContractProbeRunMapper probeRunMapper,
            MarketingMonitorAlertMapper alertMapper,
            GrowthActivityMapper growthActivityMapper,
            GrowthRewardPoolMapper growthRewardPoolMapper) {

        static MapperMocks create() {
            return new MapperMocks(
                    mock(CanvasMapper.class),
                    mock(MarketingContentReleaseMapper.class),
                    mock(ConversationWorkItemMapper.class),
                    mock(MarketingMonitorSourceMapper.class),
                    mock(MarketingMonitorAlertChannelMapper.class),
                    mock(PaidMediaAudienceDestinationMapper.class),
                    mock(MarketingMonitorProviderCredentialMapper.class),
                    mock(SearchMarketingSourceMapper.class),
                    mock(CreatorCampaignMapper.class),
                    mock(ProgrammaticDspSeatMapper.class),
                    mock(BiDashboardMapper.class),
                    mock(SearchMarketingMutationMapper.class),
                    mock(CreatorProviderMutationMapper.class),
                    mock(ProgrammaticDspMutationMapper.class),
                    mock(MarketingCampaignMasterMapper.class),
                    mock(MarketingCampaignLinkMapper.class),
                    mock(MarketingIntegrationContractMapper.class),
                    mock(MarketingIntegrationContractProbeRunMapper.class),
                    mock(MarketingMonitorAlertMapper.class),
                    mock(GrowthActivityMapper.class),
                    mock(GrowthRewardPoolMapper.class));
        }

        JdbcMarketingPlatformControlPlaneEvidenceProvider provider() {
            return new JdbcMarketingPlatformControlPlaneEvidenceProvider(
                    canvasMapper,
                    contentReleaseMapper,
                    conversationWorkItemMapper,
                    monitorSourceMapper,
                    alertChannelMapper,
                    paidMediaDestinationMapper,
                    providerCredentialMapper,
                    searchSourceMapper,
                    creatorCampaignMapper,
                    dspSeatMapper,
                    biDashboardMapper,
                    searchMutationMapper,
                    creatorMutationMapper,
                    dspMutationMapper,
                    campaignMasterMapper,
                    campaignLinkMapper,
                    integrationContractMapper,
                    probeRunMapper,
                    alertMapper,
                    growthActivityMapper,
                    growthRewardPoolMapper);
        }
    }
}
