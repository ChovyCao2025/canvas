package org.chovy.canvas.platform.api;

import java.util.Objects;

/**
 * 为营销平台控制面汇总运行时证据。
 */
public interface MarketingPlatformControlPlaneEvidenceProvider {

    /**
     * 查询指定租户的控制面运行时证据。
     *
     * @param tenantId 租户标识
     * @return 控制面运行时证据
     */
    RuntimeEvidence evidence(Long tenantId);

    /**
     * 创建始终返回空证据的提供者。
     *
     * @return 空证据提供者
     */
    static MarketingPlatformControlPlaneEvidenceProvider empty() {
        return tenantId -> RuntimeEvidence.empty();
    }

    /**
     * 控制面用于计算就绪状态的运行时指标快照。
     *
     * @param publishedJourneyCount 已发布旅程数量
     * @param activeContentReleaseCount 活跃内容发布数量
     * @param conversationWorkItemCount 会话工作项数量
     * @param activeMonitoringSourceCount 活跃监控来源数量
     * @param enabledAlertChannelCount 已启用告警渠道数量
     * @param enabledPaidMediaDestinationCount 已启用付费媒体投放目的地数量
     * @param activeProviderCredentialCount 活跃供应方凭据数量
     * @param enabledSearchSourceCount 已启用搜索来源数量
     * @param activeCreatorCampaignCount 活跃达人活动数量
     * @param enabledProgrammaticDspSeatCount 已启用程序化 DSP 席位数量
     * @param publishedBiDashboardCount 已发布 BI 看板数量
     * @param searchProviderMutationCount 搜索供应方变更数量
     * @param searchPendingWriteCount 搜索待写入数量
     * @param searchFailedWriteCount 搜索写入失败数量
     * @param creatorProviderMutationCount 达人供应方变更数量
     * @param creatorPendingWriteCount 达人待写入数量
     * @param creatorFailedWriteCount 达人写入失败数量
     * @param programmaticDspProviderMutationCount 程序化 DSP 供应方变更数量
     * @param programmaticDspPendingWriteCount 程序化 DSP 待写入数量
     * @param programmaticDspFailedWriteCount 程序化 DSP 写入失败数量
     * @param activeCampaignMasterCount 活跃活动主记录数量
     * @param campaignResourceLinkCount 活动资源关联总数
     * @param requiredCampaignResourceLinkCount 必需活动资源关联数量
     * @param blockedCampaignResourceLinkCount 被阻塞活动资源关联数量
     * @param campaignsWithInactiveRequiredLinks 存在非活跃必需关联的活动数量
     * @param campaignsMissingPrimaryDependency 缺少主依赖的活动数量
     * @param campaignsMissingMeasurementDependency 缺少度量依赖的活动数量
     * @param activeIntegrationContractCount 活跃集成契约数量
     * @param productionIntegrationContractCount 生产集成契约数量
     * @param blockedIntegrationContractCount 被阻塞集成契约数量
     * @param degradedIntegrationContractCount 已降级集成契约数量
     * @param freshPassingProductionIntegrationProbeCount 新近通过的生产集成探针数量
     * @param freshFailingProductionIntegrationProbeCount 新近失败的生产集成探针数量
     * @param openIntegrationContractProbeAlertCount 未关闭的集成探针告警数量
     * @param openIntegrationContractSloAlertCount 未关闭的集成 SLO 告警数量
     * @param activeGrowthActivityCount 活跃增长活动数量
     * @param growthActivityRewardPoolCount 增长活动奖池数量
     * @param readyGrowthActivityCount 已就绪增长活动数量
     * @param blockedGrowthActivityReadinessCount 被阻塞增长活动就绪项数量
     */
    final class RuntimeEvidence {

        /**
         * 已发布旅程数量。
         */
        private final long publishedJourneyCount;

        /**
         * 活跃内容发布数量。
         */
        private final long activeContentReleaseCount;

        /**
         * 会话工作项数量。
         */
        private final long conversationWorkItemCount;

        /**
         * 活跃监控来源数量。
         */
        private final long activeMonitoringSourceCount;

        /**
         * 已启用告警渠道数量。
         */
        private final long enabledAlertChannelCount;

        /**
         * 已启用付费媒体投放目的地数量。
         */
        private final long enabledPaidMediaDestinationCount;

        /**
         * 活跃供应方凭据数量。
         */
        private final long activeProviderCredentialCount;

        /**
         * 已启用搜索来源数量。
         */
        private final long enabledSearchSourceCount;

        /**
         * 活跃达人活动数量。
         */
        private final long activeCreatorCampaignCount;

        /**
         * 已启用程序化 DSP 席位数量。
         */
        private final long enabledProgrammaticDspSeatCount;

        /**
         * 已发布 BI 看板数量。
         */
        private final long publishedBiDashboardCount;

        /**
         * 搜索供应方变更数量。
         */
        private final long searchProviderMutationCount;

        /**
         * 搜索待写入数量。
         */
        private final long searchPendingWriteCount;

        /**
         * 搜索写入失败数量。
         */
        private final long searchFailedWriteCount;

        /**
         * 达人供应方变更数量。
         */
        private final long creatorProviderMutationCount;

        /**
         * 达人待写入数量。
         */
        private final long creatorPendingWriteCount;

        /**
         * 达人写入失败数量。
         */
        private final long creatorFailedWriteCount;

        /**
         * 程序化 DSP 供应方变更数量。
         */
        private final long programmaticDspProviderMutationCount;

        /**
         * 程序化 DSP 待写入数量。
         */
        private final long programmaticDspPendingWriteCount;

        /**
         * 程序化 DSP 写入失败数量。
         */
        private final long programmaticDspFailedWriteCount;

        /**
         * 活跃活动主记录数量。
         */
        private final long activeCampaignMasterCount;

        /**
         * 活动资源关联总数。
         */
        private final long campaignResourceLinkCount;

        /**
         * 必需活动资源关联数量。
         */
        private final long requiredCampaignResourceLinkCount;

        /**
         * 被阻塞活动资源关联数量。
         */
        private final long blockedCampaignResourceLinkCount;

        /**
         * 存在非活跃必需关联的活动数量。
         */
        private final long campaignsWithInactiveRequiredLinks;

        /**
         * 缺少主依赖的活动数量。
         */
        private final long campaignsMissingPrimaryDependency;

        /**
         * 缺少度量依赖的活动数量。
         */
        private final long campaignsMissingMeasurementDependency;

        /**
         * 活跃集成契约数量。
         */
        private final long activeIntegrationContractCount;

        /**
         * 生产集成契约数量。
         */
        private final long productionIntegrationContractCount;

        /**
         * 被阻塞集成契约数量。
         */
        private final long blockedIntegrationContractCount;

        /**
         * 已降级集成契约数量。
         */
        private final long degradedIntegrationContractCount;

        /**
         * 新近通过的生产集成探针数量。
         */
        private final long freshPassingProductionIntegrationProbeCount;

        /**
         * 新近失败的生产集成探针数量。
         */
        private final long freshFailingProductionIntegrationProbeCount;

        /**
         * 未关闭的集成探针告警数量。
         */
        private final long openIntegrationContractProbeAlertCount;

        /**
         * 未关闭的集成 SLO 告警数量。
         */
        private final long openIntegrationContractSloAlertCount;

        /**
         * 活跃增长活动数量。
         */
        private final long activeGrowthActivityCount;

        /**
         * 增长活动奖池数量。
         */
        private final long growthActivityRewardPoolCount;

        /**
         * 已就绪增长活动数量。
         */
        private final long readyGrowthActivityCount;

        /**
         * 被阻塞增长活动就绪项数量。
         */
        private final long blockedGrowthActivityReadinessCount;

        /**
         * 创建RuntimeEvidence。
         *
         * @param publishedJourneyCount 已发布旅程数量
         * @param activeContentReleaseCount 活跃内容发布数量
         * @param conversationWorkItemCount 会话工作项数量
         * @param activeMonitoringSourceCount 活跃监控来源数量
         * @param enabledAlertChannelCount 已启用告警渠道数量
         * @param enabledPaidMediaDestinationCount 已启用付费媒体投放目的地数量
         * @param activeProviderCredentialCount 活跃供应方凭据数量
         * @param enabledSearchSourceCount 已启用搜索来源数量
         * @param activeCreatorCampaignCount 活跃达人活动数量
         * @param enabledProgrammaticDspSeatCount 已启用程序化 DSP 席位数量
         * @param publishedBiDashboardCount 已发布 BI 看板数量
         * @param searchProviderMutationCount 搜索供应方变更数量
         * @param searchPendingWriteCount 搜索待写入数量
         * @param searchFailedWriteCount 搜索写入失败数量
         * @param creatorProviderMutationCount 达人供应方变更数量
         * @param creatorPendingWriteCount 达人待写入数量
         * @param creatorFailedWriteCount 达人写入失败数量
         * @param programmaticDspProviderMutationCount 程序化 DSP 供应方变更数量
         * @param programmaticDspPendingWriteCount 程序化 DSP 待写入数量
         * @param programmaticDspFailedWriteCount 程序化 DSP 写入失败数量
         * @param activeCampaignMasterCount 活跃活动主记录数量
         * @param campaignResourceLinkCount 活动资源关联总数
         * @param requiredCampaignResourceLinkCount 必需活动资源关联数量
         * @param blockedCampaignResourceLinkCount 被阻塞活动资源关联数量
         * @param campaignsWithInactiveRequiredLinks 存在非活跃必需关联的活动数量
         * @param campaignsMissingPrimaryDependency 缺少主依赖的活动数量
         * @param campaignsMissingMeasurementDependency 缺少度量依赖的活动数量
         * @param activeIntegrationContractCount 活跃集成契约数量
         * @param productionIntegrationContractCount 生产集成契约数量
         * @param blockedIntegrationContractCount 被阻塞集成契约数量
         * @param degradedIntegrationContractCount 已降级集成契约数量
         * @param freshPassingProductionIntegrationProbeCount 新近通过的生产集成探针数量
         * @param freshFailingProductionIntegrationProbeCount 新近失败的生产集成探针数量
         * @param openIntegrationContractProbeAlertCount 未关闭的集成探针告警数量
         * @param openIntegrationContractSloAlertCount 未关闭的集成 SLO 告警数量
         * @param activeGrowthActivityCount 活跃增长活动数量
         * @param growthActivityRewardPoolCount 增长活动奖池数量
         * @param readyGrowthActivityCount 已就绪增长活动数量
         * @param blockedGrowthActivityReadinessCount 被阻塞增长活动就绪项数量
         */
        public RuntimeEvidence(
                long publishedJourneyCount, long activeContentReleaseCount, long conversationWorkItemCount,
                long activeMonitoringSourceCount, long enabledAlertChannelCount,
                long enabledPaidMediaDestinationCount, long activeProviderCredentialCount,
                long enabledSearchSourceCount, long activeCreatorCampaignCount,
                long enabledProgrammaticDspSeatCount, long publishedBiDashboardCount,
                long searchProviderMutationCount, long searchPendingWriteCount, long searchFailedWriteCount,
                long creatorProviderMutationCount, long creatorPendingWriteCount, long creatorFailedWriteCount,
                long programmaticDspProviderMutationCount, long programmaticDspPendingWriteCount,
                long programmaticDspFailedWriteCount, long activeCampaignMasterCount,
                long campaignResourceLinkCount, long requiredCampaignResourceLinkCount,
                long blockedCampaignResourceLinkCount, long campaignsWithInactiveRequiredLinks,
                long campaignsMissingPrimaryDependency, long campaignsMissingMeasurementDependency,
                long activeIntegrationContractCount, long productionIntegrationContractCount,
                long blockedIntegrationContractCount, long degradedIntegrationContractCount,
                long freshPassingProductionIntegrationProbeCount,
                long freshFailingProductionIntegrationProbeCount, long openIntegrationContractProbeAlertCount,
                long openIntegrationContractSloAlertCount, long activeGrowthActivityCount,
                long growthActivityRewardPoolCount, long readyGrowthActivityCount,
                long blockedGrowthActivityReadinessCount) {
            this.publishedJourneyCount = publishedJourneyCount;
            this.activeContentReleaseCount = activeContentReleaseCount;
            this.conversationWorkItemCount = conversationWorkItemCount;
            this.activeMonitoringSourceCount = activeMonitoringSourceCount;
            this.enabledAlertChannelCount = enabledAlertChannelCount;
            this.enabledPaidMediaDestinationCount = enabledPaidMediaDestinationCount;
            this.activeProviderCredentialCount = activeProviderCredentialCount;
            this.enabledSearchSourceCount = enabledSearchSourceCount;
            this.activeCreatorCampaignCount = activeCreatorCampaignCount;
            this.enabledProgrammaticDspSeatCount = enabledProgrammaticDspSeatCount;
            this.publishedBiDashboardCount = publishedBiDashboardCount;
            this.searchProviderMutationCount = searchProviderMutationCount;
            this.searchPendingWriteCount = searchPendingWriteCount;
            this.searchFailedWriteCount = searchFailedWriteCount;
            this.creatorProviderMutationCount = creatorProviderMutationCount;
            this.creatorPendingWriteCount = creatorPendingWriteCount;
            this.creatorFailedWriteCount = creatorFailedWriteCount;
            this.programmaticDspProviderMutationCount = programmaticDspProviderMutationCount;
            this.programmaticDspPendingWriteCount = programmaticDspPendingWriteCount;
            this.programmaticDspFailedWriteCount = programmaticDspFailedWriteCount;
            this.activeCampaignMasterCount = activeCampaignMasterCount;
            this.campaignResourceLinkCount = campaignResourceLinkCount;
            this.requiredCampaignResourceLinkCount = requiredCampaignResourceLinkCount;
            this.blockedCampaignResourceLinkCount = blockedCampaignResourceLinkCount;
            this.campaignsWithInactiveRequiredLinks = campaignsWithInactiveRequiredLinks;
            this.campaignsMissingPrimaryDependency = campaignsMissingPrimaryDependency;
            this.campaignsMissingMeasurementDependency = campaignsMissingMeasurementDependency;
            this.activeIntegrationContractCount = activeIntegrationContractCount;
            this.productionIntegrationContractCount = productionIntegrationContractCount;
            this.blockedIntegrationContractCount = blockedIntegrationContractCount;
            this.degradedIntegrationContractCount = degradedIntegrationContractCount;
            this.freshPassingProductionIntegrationProbeCount = freshPassingProductionIntegrationProbeCount;
            this.freshFailingProductionIntegrationProbeCount = freshFailingProductionIntegrationProbeCount;
            this.openIntegrationContractProbeAlertCount = openIntegrationContractProbeAlertCount;
            this.openIntegrationContractSloAlertCount = openIntegrationContractSloAlertCount;
            this.activeGrowthActivityCount = activeGrowthActivityCount;
            this.growthActivityRewardPoolCount = growthActivityRewardPoolCount;
            this.readyGrowthActivityCount = readyGrowthActivityCount;
            this.blockedGrowthActivityReadinessCount = blockedGrowthActivityReadinessCount;
        }

        /**
         * 返回已发布旅程数量。
         *
         * @return 已发布旅程数量
         */
        public long publishedJourneyCount() {
            return publishedJourneyCount;
        }

        /**
         * 返回活跃内容发布数量。
         *
         * @return 活跃内容发布数量
         */
        public long activeContentReleaseCount() {
            return activeContentReleaseCount;
        }

        /**
         * 返回会话工作项数量。
         *
         * @return 会话工作项数量
         */
        public long conversationWorkItemCount() {
            return conversationWorkItemCount;
        }

        /**
         * 返回活跃监控来源数量。
         *
         * @return 活跃监控来源数量
         */
        public long activeMonitoringSourceCount() {
            return activeMonitoringSourceCount;
        }

        /**
         * 返回已启用告警渠道数量。
         *
         * @return 已启用告警渠道数量
         */
        public long enabledAlertChannelCount() {
            return enabledAlertChannelCount;
        }

        /**
         * 返回已启用付费媒体投放目的地数量。
         *
         * @return 已启用付费媒体投放目的地数量
         */
        public long enabledPaidMediaDestinationCount() {
            return enabledPaidMediaDestinationCount;
        }

        /**
         * 返回活跃供应方凭据数量。
         *
         * @return 活跃供应方凭据数量
         */
        public long activeProviderCredentialCount() {
            return activeProviderCredentialCount;
        }

        /**
         * 返回已启用搜索来源数量。
         *
         * @return 已启用搜索来源数量
         */
        public long enabledSearchSourceCount() {
            return enabledSearchSourceCount;
        }

        /**
         * 返回活跃达人活动数量。
         *
         * @return 活跃达人活动数量
         */
        public long activeCreatorCampaignCount() {
            return activeCreatorCampaignCount;
        }

        /**
         * 返回已启用程序化 DSP 席位数量。
         *
         * @return 已启用程序化 DSP 席位数量
         */
        public long enabledProgrammaticDspSeatCount() {
            return enabledProgrammaticDspSeatCount;
        }

        /**
         * 返回已发布 BI 看板数量。
         *
         * @return 已发布 BI 看板数量
         */
        public long publishedBiDashboardCount() {
            return publishedBiDashboardCount;
        }

        /**
         * 返回搜索供应方变更数量。
         *
         * @return 搜索供应方变更数量
         */
        public long searchProviderMutationCount() {
            return searchProviderMutationCount;
        }

        /**
         * 返回搜索待写入数量。
         *
         * @return 搜索待写入数量
         */
        public long searchPendingWriteCount() {
            return searchPendingWriteCount;
        }

        /**
         * 返回搜索写入失败数量。
         *
         * @return 搜索写入失败数量
         */
        public long searchFailedWriteCount() {
            return searchFailedWriteCount;
        }

        /**
         * 返回达人供应方变更数量。
         *
         * @return 达人供应方变更数量
         */
        public long creatorProviderMutationCount() {
            return creatorProviderMutationCount;
        }

        /**
         * 返回达人待写入数量。
         *
         * @return 达人待写入数量
         */
        public long creatorPendingWriteCount() {
            return creatorPendingWriteCount;
        }

        /**
         * 返回达人写入失败数量。
         *
         * @return 达人写入失败数量
         */
        public long creatorFailedWriteCount() {
            return creatorFailedWriteCount;
        }

        /**
         * 返回程序化 DSP 供应方变更数量。
         *
         * @return 程序化 DSP 供应方变更数量
         */
        public long programmaticDspProviderMutationCount() {
            return programmaticDspProviderMutationCount;
        }

        /**
         * 返回程序化 DSP 待写入数量。
         *
         * @return 程序化 DSP 待写入数量
         */
        public long programmaticDspPendingWriteCount() {
            return programmaticDspPendingWriteCount;
        }

        /**
         * 返回程序化 DSP 写入失败数量。
         *
         * @return 程序化 DSP 写入失败数量
         */
        public long programmaticDspFailedWriteCount() {
            return programmaticDspFailedWriteCount;
        }

        /**
         * 返回活跃活动主记录数量。
         *
         * @return 活跃活动主记录数量
         */
        public long activeCampaignMasterCount() {
            return activeCampaignMasterCount;
        }

        /**
         * 返回活动资源关联总数。
         *
         * @return 活动资源关联总数
         */
        public long campaignResourceLinkCount() {
            return campaignResourceLinkCount;
        }

        /**
         * 返回必需活动资源关联数量。
         *
         * @return 必需活动资源关联数量
         */
        public long requiredCampaignResourceLinkCount() {
            return requiredCampaignResourceLinkCount;
        }

        /**
         * 返回被阻塞活动资源关联数量。
         *
         * @return 被阻塞活动资源关联数量
         */
        public long blockedCampaignResourceLinkCount() {
            return blockedCampaignResourceLinkCount;
        }

        /**
         * 返回存在非活跃必需关联的活动数量。
         *
         * @return 存在非活跃必需关联的活动数量
         */
        public long campaignsWithInactiveRequiredLinks() {
            return campaignsWithInactiveRequiredLinks;
        }

        /**
         * 返回缺少主依赖的活动数量。
         *
         * @return 缺少主依赖的活动数量
         */
        public long campaignsMissingPrimaryDependency() {
            return campaignsMissingPrimaryDependency;
        }

        /**
         * 返回缺少度量依赖的活动数量。
         *
         * @return 缺少度量依赖的活动数量
         */
        public long campaignsMissingMeasurementDependency() {
            return campaignsMissingMeasurementDependency;
        }

        /**
         * 返回活跃集成契约数量。
         *
         * @return 活跃集成契约数量
         */
        public long activeIntegrationContractCount() {
            return activeIntegrationContractCount;
        }

        /**
         * 返回生产集成契约数量。
         *
         * @return 生产集成契约数量
         */
        public long productionIntegrationContractCount() {
            return productionIntegrationContractCount;
        }

        /**
         * 返回被阻塞集成契约数量。
         *
         * @return 被阻塞集成契约数量
         */
        public long blockedIntegrationContractCount() {
            return blockedIntegrationContractCount;
        }

        /**
         * 返回已降级集成契约数量。
         *
         * @return 已降级集成契约数量
         */
        public long degradedIntegrationContractCount() {
            return degradedIntegrationContractCount;
        }

        /**
         * 返回新近通过的生产集成探针数量。
         *
         * @return 新近通过的生产集成探针数量
         */
        public long freshPassingProductionIntegrationProbeCount() {
            return freshPassingProductionIntegrationProbeCount;
        }

        /**
         * 返回新近失败的生产集成探针数量。
         *
         * @return 新近失败的生产集成探针数量
         */
        public long freshFailingProductionIntegrationProbeCount() {
            return freshFailingProductionIntegrationProbeCount;
        }

        /**
         * 返回未关闭的集成探针告警数量。
         *
         * @return 未关闭的集成探针告警数量
         */
        public long openIntegrationContractProbeAlertCount() {
            return openIntegrationContractProbeAlertCount;
        }

        /**
         * 返回未关闭的集成 SLO 告警数量。
         *
         * @return 未关闭的集成 SLO 告警数量
         */
        public long openIntegrationContractSloAlertCount() {
            return openIntegrationContractSloAlertCount;
        }

        /**
         * 返回活跃增长活动数量。
         *
         * @return 活跃增长活动数量
         */
        public long activeGrowthActivityCount() {
            return activeGrowthActivityCount;
        }

        /**
         * 返回增长活动奖池数量。
         *
         * @return 增长活动奖池数量
         */
        public long growthActivityRewardPoolCount() {
            return growthActivityRewardPoolCount;
        }

        /**
         * 返回已就绪增长活动数量。
         *
         * @return 已就绪增长活动数量
         */
        public long readyGrowthActivityCount() {
            return readyGrowthActivityCount;
        }

        /**
         * 返回被阻塞增长活动就绪项数量。
         *
         * @return 被阻塞增长活动就绪项数量
         */
        public long blockedGrowthActivityReadinessCount() {
            return blockedGrowthActivityReadinessCount;
        }

        /**
         * 判断两个 RuntimeEvidence 值对象是否相同。
         *
         * @param object 待比较对象
         * @return 所有字段相同时返回 true
         */
        @Override
        public boolean equals(Object object) {
            if (this == object) {
                return true;
            }
            if (!(object instanceof RuntimeEvidence that)) {
                return false;
            }
            return publishedJourneyCount == that.publishedJourneyCount
                    && activeContentReleaseCount == that.activeContentReleaseCount
                    && conversationWorkItemCount == that.conversationWorkItemCount
                    && activeMonitoringSourceCount == that.activeMonitoringSourceCount
                    && enabledAlertChannelCount == that.enabledAlertChannelCount
                    && enabledPaidMediaDestinationCount == that.enabledPaidMediaDestinationCount
                    && activeProviderCredentialCount == that.activeProviderCredentialCount
                    && enabledSearchSourceCount == that.enabledSearchSourceCount
                    && activeCreatorCampaignCount == that.activeCreatorCampaignCount
                    && enabledProgrammaticDspSeatCount == that.enabledProgrammaticDspSeatCount
                    && publishedBiDashboardCount == that.publishedBiDashboardCount
                    && searchProviderMutationCount == that.searchProviderMutationCount
                    && searchPendingWriteCount == that.searchPendingWriteCount
                    && searchFailedWriteCount == that.searchFailedWriteCount
                    && creatorProviderMutationCount == that.creatorProviderMutationCount
                    && creatorPendingWriteCount == that.creatorPendingWriteCount
                    && creatorFailedWriteCount == that.creatorFailedWriteCount
                    && programmaticDspProviderMutationCount == that.programmaticDspProviderMutationCount
                    && programmaticDspPendingWriteCount == that.programmaticDspPendingWriteCount
                    && programmaticDspFailedWriteCount == that.programmaticDspFailedWriteCount
                    && activeCampaignMasterCount == that.activeCampaignMasterCount
                    && campaignResourceLinkCount == that.campaignResourceLinkCount
                    && requiredCampaignResourceLinkCount == that.requiredCampaignResourceLinkCount
                    && blockedCampaignResourceLinkCount == that.blockedCampaignResourceLinkCount
                    && campaignsWithInactiveRequiredLinks == that.campaignsWithInactiveRequiredLinks
                    && campaignsMissingPrimaryDependency == that.campaignsMissingPrimaryDependency
                    && campaignsMissingMeasurementDependency == that.campaignsMissingMeasurementDependency
                    && activeIntegrationContractCount == that.activeIntegrationContractCount
                    && productionIntegrationContractCount == that.productionIntegrationContractCount
                    && blockedIntegrationContractCount == that.blockedIntegrationContractCount
                    && degradedIntegrationContractCount == that.degradedIntegrationContractCount
                    && freshPassingProductionIntegrationProbeCount == that.freshPassingProductionIntegrationProbeCount
                    && freshFailingProductionIntegrationProbeCount == that.freshFailingProductionIntegrationProbeCount
                    && openIntegrationContractProbeAlertCount == that.openIntegrationContractProbeAlertCount
                    && openIntegrationContractSloAlertCount == that.openIntegrationContractSloAlertCount
                    && activeGrowthActivityCount == that.activeGrowthActivityCount
                    && growthActivityRewardPoolCount == that.growthActivityRewardPoolCount
                    && readyGrowthActivityCount == that.readyGrowthActivityCount
                    && blockedGrowthActivityReadinessCount == that.blockedGrowthActivityReadinessCount;
        }

        /**
         * 计算 RuntimeEvidence 值对象哈希值。
         *
         * @return 哈希值
         */
        @Override
        public int hashCode() {
            return Objects.hash(publishedJourneyCount, activeContentReleaseCount, conversationWorkItemCount, activeMonitoringSourceCount, enabledAlertChannelCount, enabledPaidMediaDestinationCount, activeProviderCredentialCount, enabledSearchSourceCount, activeCreatorCampaignCount, enabledProgrammaticDspSeatCount, publishedBiDashboardCount, searchProviderMutationCount, searchPendingWriteCount, searchFailedWriteCount, creatorProviderMutationCount, creatorPendingWriteCount, creatorFailedWriteCount, programmaticDspProviderMutationCount, programmaticDspPendingWriteCount, programmaticDspFailedWriteCount, activeCampaignMasterCount, campaignResourceLinkCount, requiredCampaignResourceLinkCount, blockedCampaignResourceLinkCount, campaignsWithInactiveRequiredLinks, campaignsMissingPrimaryDependency, campaignsMissingMeasurementDependency, activeIntegrationContractCount, productionIntegrationContractCount, blockedIntegrationContractCount, degradedIntegrationContractCount, freshPassingProductionIntegrationProbeCount, freshFailingProductionIntegrationProbeCount, openIntegrationContractProbeAlertCount, openIntegrationContractSloAlertCount, activeGrowthActivityCount, growthActivityRewardPoolCount, readyGrowthActivityCount, blockedGrowthActivityReadinessCount);
        }

        /**
         * 返回与原 record 形态一致的字符串。
         *
         * @return 字符串表示
         */
        @Override
        public String toString() {
            return "RuntimeEvidence[publishedJourneyCount=" + publishedJourneyCount + ", activeContentReleaseCount=" + activeContentReleaseCount + ", conversationWorkItemCount=" + conversationWorkItemCount + ", activeMonitoringSourceCount=" + activeMonitoringSourceCount + ", enabledAlertChannelCount=" + enabledAlertChannelCount + ", enabledPaidMediaDestinationCount=" + enabledPaidMediaDestinationCount + ", activeProviderCredentialCount=" + activeProviderCredentialCount + ", enabledSearchSourceCount=" + enabledSearchSourceCount + ", activeCreatorCampaignCount=" + activeCreatorCampaignCount + ", enabledProgrammaticDspSeatCount=" + enabledProgrammaticDspSeatCount + ", publishedBiDashboardCount=" + publishedBiDashboardCount + ", searchProviderMutationCount=" + searchProviderMutationCount + ", searchPendingWriteCount=" + searchPendingWriteCount + ", searchFailedWriteCount=" + searchFailedWriteCount + ", creatorProviderMutationCount=" + creatorProviderMutationCount + ", creatorPendingWriteCount=" + creatorPendingWriteCount + ", creatorFailedWriteCount=" + creatorFailedWriteCount + ", programmaticDspProviderMutationCount=" + programmaticDspProviderMutationCount + ", programmaticDspPendingWriteCount=" + programmaticDspPendingWriteCount + ", programmaticDspFailedWriteCount=" + programmaticDspFailedWriteCount + ", activeCampaignMasterCount=" + activeCampaignMasterCount + ", campaignResourceLinkCount=" + campaignResourceLinkCount + ", requiredCampaignResourceLinkCount=" + requiredCampaignResourceLinkCount + ", blockedCampaignResourceLinkCount=" + blockedCampaignResourceLinkCount + ", campaignsWithInactiveRequiredLinks=" + campaignsWithInactiveRequiredLinks + ", campaignsMissingPrimaryDependency=" + campaignsMissingPrimaryDependency + ", campaignsMissingMeasurementDependency=" + campaignsMissingMeasurementDependency + ", activeIntegrationContractCount=" + activeIntegrationContractCount + ", productionIntegrationContractCount=" + productionIntegrationContractCount + ", blockedIntegrationContractCount=" + blockedIntegrationContractCount + ", degradedIntegrationContractCount=" + degradedIntegrationContractCount + ", freshPassingProductionIntegrationProbeCount=" + freshPassingProductionIntegrationProbeCount + ", freshFailingProductionIntegrationProbeCount=" + freshFailingProductionIntegrationProbeCount + ", openIntegrationContractProbeAlertCount=" + openIntegrationContractProbeAlertCount + ", openIntegrationContractSloAlertCount=" + openIntegrationContractSloAlertCount + ", activeGrowthActivityCount=" + activeGrowthActivityCount + ", growthActivityRewardPoolCount=" + growthActivityRewardPoolCount + ", readyGrowthActivityCount=" + readyGrowthActivityCount + ", blockedGrowthActivityReadinessCount=" + blockedGrowthActivityReadinessCount + "]";
        }

        /**
         * 创建所有指标为零的运行时证据。
         *
         * @return 空运行时证据
         */
        public static RuntimeEvidence empty() {
            return new RuntimeEvidence(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
        }
    }
}
