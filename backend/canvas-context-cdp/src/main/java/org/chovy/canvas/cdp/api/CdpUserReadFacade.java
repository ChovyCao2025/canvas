package org.chovy.canvas.cdp.api;

import java.time.LocalDateTime;
import java.util.List;

public interface CdpUserReadFacade {

    List<CdpUserRowView> listUsers(Long tenantId, String keyword);

    CdpUserProfileView getUser(Long tenantId, String userId);

    CdpUserInsightView getInsight(Long tenantId, String userId);

    record CdpUserRowView(
            String userId,
            String displayName,
            long executionCount,
            long successCount,
            long failedCount,
            String latestStatus,
            LocalDateTime firstEnteredAt,
            LocalDateTime lastEnteredAt,
            List<CdpUserTagSummaryView> tags) {
    }

    record CdpUserProfileView(
            String userId,
            String displayName,
            String phone,
            String email,
            String status,
            String propertiesJson,
            LocalDateTime firstSeenAt,
            LocalDateTime lastSeenAt) {
    }

    record CdpUserInsightView(
            String userId,
            CdpUserProfileView profile,
            List<CdpUserTagSummaryView> tags,
            List<CdpUserCanvasSummaryView> canvasRows) {
    }

    record CdpUserTagSummaryView(
            String tagCode,
            String tagName,
            String tagValue,
            String valueType,
            String sourceType,
            String status,
            LocalDateTime effectiveAt,
            LocalDateTime expiresAt,
            LocalDateTime updatedAt) {
    }

    record CdpUserCanvasSummaryView(
            Long canvasId,
            String canvasName,
            long executionCount,
            long successCount,
            long failedCount,
            String latestStatus,
            LocalDateTime firstEnteredAt,
            LocalDateTime lastEnteredAt) {
    }
}
