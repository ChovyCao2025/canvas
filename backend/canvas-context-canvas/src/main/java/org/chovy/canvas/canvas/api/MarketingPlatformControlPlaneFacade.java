package org.chovy.canvas.canvas.api;

import java.util.List;

/**
 * 定义MarketingPlatformControlPlaneFacade对外提供的能力契约。
 */
public interface MarketingPlatformControlPlaneFacade {

    /**
     * 处理summary。
     */
    ControlPlaneSummaryView summary(Long tenantId);

    /**
     * 承载ControlPlaneSummaryView的数据快照。
     */
    record ControlPlaneSummaryView(
            /**
             * 记录租户标识。
             */
            Long tenantId,
            /**
             * 记录generated时间。
             */
            String generatedAt,
            /**
             * 记录overallStatus。
             */
            String overallStatus,
            /**
             * 记录capabilityCount。
             */
            int capabilityCount,
            /**
             * 记录liveCapabilityCount。
             */
            int liveCapabilityCount,
            /**
             * 记录actionItemCount。
             */
            int actionItemCount,
            /**
             * 记录capabilities。
             */
            List<CapabilityCardView> capabilities,
            /**
             * 记录integrationLanes。
             */
            List<IntegrationLaneView> integrationLanes,
            /**
             * 记录integrationAssets。
             */
            List<IntegrationAssetView> integrationAssets,
            /**
             * 记录readinessGate。
             */
            ReadinessGateView readinessGate,
            /**
             * 记录actionItems。
             */
            List<ActionItemView> actionItems) {
    }

    /**
     * 承载CapabilityCardView的数据快照。
     */
    record CapabilityCardView(
            /**
             * 记录capabilityKey。
             */
            String capabilityKey,
            /**
             * 记录displayName。
             */
            String displayName,
            /**
             * 记录domain。
             */
            String domain,
            /**
             * 记录状态。
             */
            String status,
            /**
             * 记录route。
             */
            String route,
            /**
             * 记录apiRoot。
             */
            String apiRoot,
            /**
             * 记录surface。
             */
            String surface,
            /**
             * 记录productionSignals。
             */
            List<String> productionSignals,
            /**
             * 记录gaps。
             */
            List<String> gaps,
            /**
             * 记录evidence。
             */
            List<EvidenceSignalView> evidence) {
    }

    /**
     * 承载EvidenceSignalView的数据快照。
     */
    record EvidenceSignalView(String signalKey, String label, long value, String status) {
    }

    /**
     * 承载IntegrationLaneView的数据快照。
     */
    record IntegrationLaneView(
            /**
             * 记录laneKey。
             */
            String laneKey,
            /**
             * 记录displayName。
             */
            String displayName,
            /**
             * 记录sourceCapabilityKey。
             */
            String sourceCapabilityKey,
            /**
             * 记录targetCapabilityKey。
             */
            String targetCapabilityKey,
            /**
             * 记录状态。
             */
            String status,
            /**
             * 记录controls。
             */
            List<String> controls) {
    }

    /**
     * 承载IntegrationAssetView的数据快照。
     */
    record IntegrationAssetView(
            /**
             * 记录assetKey。
             */
            String assetKey,
            /**
             * 记录displayName。
             */
            String displayName,
            /**
             * 记录assetType。
             */
            String assetType,
            /**
             * 记录ownerCapabilityKey。
             */
            String ownerCapabilityKey,
            /**
             * 记录providerFamily。
             */
            String providerFamily,
            /**
             * 记录状态。
             */
            String status,
            /**
             * 记录apiRoot。
             */
            String apiRoot,
            /**
             * 记录credentialDependency。
             */
            String credentialDependency,
            /**
             * 记录pendingWrites。
             */
            long pendingWrites,
            /**
             * 记录failedWrites。
             */
            long failedWrites,
            /**
             * 记录controls。
             */
            List<String> controls,
            /**
             * 记录gaps。
             */
            List<String> gaps,
            /**
             * 记录evidence。
             */
            List<EvidenceSignalView> evidence) {
    }

    /**
     * 承载ReadinessGateView的数据快照。
     */
    record ReadinessGateView(
            /**
             * 记录状态。
             */
            String status,
            /**
             * 记录productionReady。
             */
            boolean productionReady,
            /**
             * 记录blockerCount。
             */
            int blockerCount,
            /**
             * 记录warningCount。
             */
            int warningCount,
            /**
             * 记录blockers。
             */
            List<ReadinessFindingView> blockers,
            /**
             * 记录warnings。
             */
            List<ReadinessFindingView> warnings) {
    }

    /**
     * 承载ReadinessFindingView的数据快照。
     */
    record ReadinessFindingView(
            /**
             * 记录severity。
             */
            String severity,
            /**
             * 记录itemType。
             */
            String itemType,
            /**
             * 记录itemKey。
             */
            String itemKey,
            /**
             * 记录标题。
             */
            String title,
            /**
             * 记录route。
             */
            String route,
            /**
             * 记录reason。
             */
            String reason) {
    }

    /**
     * 承载ActionItemView的数据快照。
     */
    record ActionItemView(
            /**
             * 记录priority。
             */
            String priority,
            /**
             * 记录capabilityKey。
             */
            String capabilityKey,
            /**
             * 记录标题。
             */
            String title,
            /**
             * 记录route。
             */
            String route,
            /**
             * 记录reason。
             */
            String reason) {
    }
}
