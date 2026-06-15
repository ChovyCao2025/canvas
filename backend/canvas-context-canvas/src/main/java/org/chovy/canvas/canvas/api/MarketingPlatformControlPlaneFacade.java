package org.chovy.canvas.canvas.api;

import java.util.List;

public interface MarketingPlatformControlPlaneFacade {

    ControlPlaneSummaryView summary(Long tenantId);

    record ControlPlaneSummaryView(
            Long tenantId,
            String generatedAt,
            String overallStatus,
            int capabilityCount,
            int liveCapabilityCount,
            int actionItemCount,
            List<CapabilityCardView> capabilities,
            List<IntegrationLaneView> integrationLanes,
            List<IntegrationAssetView> integrationAssets,
            ReadinessGateView readinessGate,
            List<ActionItemView> actionItems) {
    }

    record CapabilityCardView(
            String capabilityKey,
            String displayName,
            String domain,
            String status,
            String route,
            String apiRoot,
            String surface,
            List<String> productionSignals,
            List<String> gaps,
            List<EvidenceSignalView> evidence) {
    }

    record EvidenceSignalView(String signalKey, String label, long value, String status) {
    }

    record IntegrationLaneView(
            String laneKey,
            String displayName,
            String sourceCapabilityKey,
            String targetCapabilityKey,
            String status,
            List<String> controls) {
    }

    record IntegrationAssetView(
            String assetKey,
            String displayName,
            String assetType,
            String ownerCapabilityKey,
            String providerFamily,
            String status,
            String apiRoot,
            String credentialDependency,
            long pendingWrites,
            long failedWrites,
            List<String> controls,
            List<String> gaps,
            List<EvidenceSignalView> evidence) {
    }

    record ReadinessGateView(
            String status,
            boolean productionReady,
            int blockerCount,
            int warningCount,
            List<ReadinessFindingView> blockers,
            List<ReadinessFindingView> warnings) {
    }

    record ReadinessFindingView(
            String severity,
            String itemType,
            String itemKey,
            String title,
            String route,
            String reason) {
    }

    record ActionItemView(
            String priority,
            String capabilityKey,
            String title,
            String route,
            String reason) {
    }
}
