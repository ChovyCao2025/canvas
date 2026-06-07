package org.chovy.canvas.platform;

public interface MarketingPlatformControlPlaneEvidenceProvider {

    RuntimeEvidence evidence(Long tenantId);

    static MarketingPlatformControlPlaneEvidenceProvider empty() {
        return tenantId -> RuntimeEvidence.empty();
    }

    record RuntimeEvidence(
            long publishedJourneyCount,
            long activeContentReleaseCount,
            long conversationWorkItemCount,
            long activeMonitoringSourceCount,
            long enabledAlertChannelCount,
            long enabledPaidMediaDestinationCount,
            long activeProviderCredentialCount,
            long enabledSearchSourceCount,
            long activeCreatorCampaignCount,
            long enabledProgrammaticDspSeatCount,
            long publishedBiDashboardCount,
            long searchProviderMutationCount,
            long searchPendingWriteCount,
            long searchFailedWriteCount,
            long creatorProviderMutationCount,
            long creatorPendingWriteCount,
            long creatorFailedWriteCount,
            long programmaticDspProviderMutationCount,
            long programmaticDspPendingWriteCount,
            long programmaticDspFailedWriteCount,
            long activeCampaignMasterCount,
            long campaignResourceLinkCount,
            long requiredCampaignResourceLinkCount,
            long blockedCampaignResourceLinkCount,
            long campaignsWithInactiveRequiredLinks,
            long campaignsMissingPrimaryDependency,
            long campaignsMissingMeasurementDependency,
            long activeIntegrationContractCount,
            long productionIntegrationContractCount,
            long blockedIntegrationContractCount,
            long degradedIntegrationContractCount,
            long freshPassingProductionIntegrationProbeCount,
            long freshFailingProductionIntegrationProbeCount,
            long openIntegrationContractProbeAlertCount,
            long openIntegrationContractSloAlertCount) {

        public static RuntimeEvidence empty() {
            return new RuntimeEvidence(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
        }
    }
}
