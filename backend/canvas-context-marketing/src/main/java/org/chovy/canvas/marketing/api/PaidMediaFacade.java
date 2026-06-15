package org.chovy.canvas.marketing.api;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface PaidMediaFacade {

    DestinationView upsertDestination(Long tenantId, DestinationCommand command, String actor);

    SyncRunView syncAudience(Long tenantId, SyncCommand command, String actor);

    List<SyncRunView> runs(Long tenantId, RunQuery query);

    List<MemberView> members(Long tenantId, MemberQuery query);

    default void registerAudience(Long tenantId, Long audienceId, boolean active) {
    }

    default void registerProfile(Long tenantId, String userId, String email, String phone) {
    }

    default void grantConsent(Long tenantId, String userId, String channel) {
    }

    record DestinationCommand(
            String provider,
            String destinationKey,
            String displayName,
            String accountId,
            String externalAudienceId,
            List<String> identifierTypes,
            String consentChannel,
            Boolean enforceConsent,
            Boolean enabled,
            Map<String, Object> metadata) {
    }

    record SyncCommand(
            Long destinationId,
            Long audienceId,
            List<String> userIds,
            String externalOperationId,
            Map<String, Object> metadata) {
    }

    record RunQuery(Long destinationId, Long audienceId, String status, int limit) {
    }

    record MemberQuery(Long runId, String status, int limit) {
    }

    record DestinationView(
            Long id,
            Long tenantId,
            String provider,
            String destinationKey,
            String displayName,
            String accountId,
            String externalAudienceId,
            List<String> identifierTypes,
            String consentChannel,
            boolean enforceConsent,
            boolean enabled,
            Map<String, Object> metadata,
            String createdBy,
            LocalDateTime createdAt,
            LocalDateTime updatedAt) {
    }

    record SyncRunView(
            Long id,
            Long tenantId,
            Long destinationId,
            Long audienceId,
            String provider,
            String status,
            int requestedCount,
            int eligibleCount,
            int skippedCount,
            int failedCount,
            String externalOperationId,
            String failureReason,
            Map<String, Object> metadata,
            String createdBy,
            LocalDateTime createdAt,
            LocalDateTime completedAt) {
    }

    record MemberView(
            Long id,
            Long tenantId,
            Long runId,
            Long destinationId,
            Long audienceId,
            String provider,
            String userId,
            String identifierType,
            String identifierHash,
            String status,
            String reason,
            LocalDateTime createdAt) {
    }
}
