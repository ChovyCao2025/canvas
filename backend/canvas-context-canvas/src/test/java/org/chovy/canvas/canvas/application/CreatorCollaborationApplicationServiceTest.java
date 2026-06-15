package org.chovy.canvas.canvas.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;

import org.chovy.canvas.canvas.api.CreatorCollaborationFacade;
import org.junit.jupiter.api.Test;

class CreatorCollaborationApplicationServiceTest {

    @Test
    void creatorCollaborationLifecycleIsTenantScopedAndDeterministic() {
        CreatorCollaborationFacade service = new CreatorCollaborationApplicationService();

        Map<String, Object> creator = service.upsertCreator(7L, Map.of(
                "creatorKey", "creator-11",
                "displayName", "Creator One",
                "platform", "tiktok"), "operator-1");
        Map<String, Object> campaign = service.upsertCampaign(7L, Map.of(
                "campaignKey", "campaign-21",
                "name", "Summer Launch"), "operator-1");
        Map<String, Object> collaboration = service.upsertCollaboration(7L, Map.of(
                "collaborationKey", "collaboration-31",
                "campaignKey", "campaign-21",
                "creatorKey", "creator-11"), "operator-2");
        Map<String, Object> deliverable = service.upsertDeliverable(7L, Map.of(
                "deliverableKey", "deliverable-41",
                "collaborationKey", "collaboration-31",
                "title", "Launch video"), "operator-2");
        Map<String, Object> mutation = service.proposeMutation(7L, Map.of(
                "campaignId", 1L,
                "collaborationId", 1L,
                "mutationType", "RATE_CARD_UPDATE"), "operator-3");
        Map<String, Object> approved = service.approveMutation(7L, 1L, Map.of("comment", "ok"), "operator-4");
        Map<String, Object> executed = service.executeMutation(7L, 1L, Map.of("mode", "sync"), "operator-5");

        assertThat(creator).containsEntry("tenantId", 7L)
                .containsEntry("id", 1L)
                .containsEntry("creatorKey", "creator-11")
                .containsEntry("displayName", "Creator One")
                .containsEntry("updatedBy", "operator-1");
        assertThat(campaign).containsEntry("id", 1L)
                .containsEntry("campaignKey", "campaign-21")
                .containsEntry("name", "Summer Launch");
        assertThat(collaboration).containsEntry("id", 1L)
                .containsEntry("collaborationKey", "collaboration-31")
                .containsEntry("status", "ACTIVE")
                .containsEntry("updatedBy", "operator-2");
        assertThat(deliverable).containsEntry("id", 1L)
                .containsEntry("deliverableKey", "deliverable-41")
                .containsEntry("status", "PLANNED");
        assertThat(mutation).containsEntry("id", 1L)
                .containsEntry("status", "PROPOSED")
                .containsEntry("approvalStatus", "PENDING")
                .containsEntry("createdBy", "operator-3");
        assertThat(approved).containsEntry("approvalStatus", "APPROVED")
                .containsEntry("approvedBy", "operator-4");
        assertThat(executed).containsEntry("status", "EXECUTED")
                .containsEntry("executedBy", "operator-5");

        Map<String, Object> page = service.listMutations(7L, Map.of(
                "campaignId", 1L,
                "collaborationId", 1L,
                "status", "EXECUTED",
                "approvalStatus", "APPROVED",
                "limit", 10));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> mutations = (List<Map<String, Object>>) page.get("records");
        assertThat(mutations).singleElement()
                .satisfies(row -> assertThat(row).containsEntry("id", 1L));

        Map<String, Object> summary = service.summary(7L, Map.of(
                "campaignId", 1L,
                "creatorId", 1L,
                "collaborationId", 1L,
                "evaluatedAt", "2026-06-14T10:00:00"));
        assertThat(summary).containsEntry("tenantId", 7L)
                .containsEntry("campaignId", 1L)
                .containsEntry("creatorId", 1L)
                .containsEntry("collaborationId", 1L)
                .containsEntry("creatorCount", 1)
                .containsEntry("campaignCount", 1)
                .containsEntry("collaborationCount", 1)
                .containsEntry("deliverableCount", 1)
                .containsEntry("mutationCount", 1)
                .containsEntry("executedMutationCount", 1);

        assertThat(service.listMutations(8L, Map.of()).get("records")).asList().isEmpty();
    }

    @Test
    void defaultsAndValidationFollowCompatibilityRules() {
        CreatorCollaborationFacade service = new CreatorCollaborationApplicationService();

        Map<String, Object> creator = service.upsertCreator(null, Map.of("creatorKey", "defaulted"), "");

        assertThat(creator).containsEntry("tenantId", 0L)
                .containsEntry("id", 1L)
                .containsEntry("updatedBy", "system");

        assertThatThrownBy(() -> service.approveMutation(null, 999L, Map.of(), "operator-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Creator mutation not found: 999");
    }
}
