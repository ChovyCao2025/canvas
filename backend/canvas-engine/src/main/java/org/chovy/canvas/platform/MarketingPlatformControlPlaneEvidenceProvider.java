package org.chovy.canvas.platform;

/**
 * MarketingPlatformControlPlaneEvidenceProvider 定义 platform 场景中的扩展契约。
 */
public interface MarketingPlatformControlPlaneEvidenceProvider {

    /**
     * 汇总指定租户的平台控制面运行态证据。
     *
     * @param tenantId 租户 ID
     * @return 当前租户的平台运行态证据快照
     */
    RuntimeEvidence evidence(Long tenantId);

    /**
     * 创建一个始终返回空证据的 Provider。
     *
     * @return 空实现 Provider
     */
    static MarketingPlatformControlPlaneEvidenceProvider empty() {
        return tenantId -> RuntimeEvidence.empty();
    }

    /**
     * 平台控制面运行态证据快照。
     *
     * <p>字段按控制面模块和风险门禁顺序排列，用于策略计算时判断当前租户是否具备发布、
     * 监控、投放、集成和增长活动相关的基础能力。
     */
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
            long openIntegrationContractSloAlertCount,
            long activeGrowthActivityCount,
            long growthActivityRewardPoolCount,
            long readyGrowthActivityCount,
            long blockedGrowthActivityReadinessCount) {

        /**
         * empty 处理 platform 场景的业务逻辑。
         * @return 返回 empty 流程生成的业务结果。
         */
        public static RuntimeEvidence empty() {
            return new RuntimeEvidence(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
        }
    }
}
