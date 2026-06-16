package org.chovy.canvas.cdp.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;

import org.chovy.canvas.cdp.api.CdpWarehouseMetricChangeReviewFacade;
import org.chovy.canvas.cdp.api.CdpWarehouseMetricChangeReviewFacade.MetricChangeCommand;
import org.junit.jupiter.api.Test;

/**
 * 验证 CdpWarehouseMetricChangeReviewApplicationService 的核心行为。
 */
class CdpWarehouseMetricChangeReviewApplicationServiceTest {

    /**
     * 创建s Approves And Applies Metric Change With Tenant And User Defaults。
     */
    @Test
    void createsApprovesAndAppliesMetricChangeWithTenantAndUserDefaults() {
        CdpWarehouseMetricChangeReviewFacade service = new CdpWarehouseMetricChangeReviewApplicationService();

        Map<String, Object> created = service.create(null, null, command(
                " dwd_user_profile ",
                " profile_completeness ",
                "sum(completed_fields) / count(*)",
                List.of("country", "channel"),
                "improve numerator"));
        Long reviewId = (Long) created.get("id");

        assertThat(created)
                .containsEntry("tenantId", 0L)
                .containsEntry("datasetKey", "dwd_user_profile")
                .containsEntry("metricKey", "profile_completeness")
                .containsEntry("status", "PENDING_REVIEW")
                .containsEntry("requestedBy", "system")
                .containsEntry("requestReason", "improve numerator");

        Map<String, Object> approved = service.approve(null, null, reviewId, " looks correct ");
        assertThat(approved)
                .containsEntry("status", "APPROVED")
                .containsEntry("reviewedBy", "system")
                .containsEntry("reviewNote", "looks correct");

        Map<String, Object> applied = service.apply(null, null, reviewId);
        assertThat(applied)
                .containsEntry("status", "APPLIED");
        assertThat((Map<String, Object>) applied.get("currentMetric"))
                .containsEntry("expression", "count_if(profile_complete)");
        assertThat((Map<String, Object>) applied.get("proposedMetric"))
                .containsEntry("metricKey", "profile_completeness")
                .containsEntry("expression", "sum(completed_fields) / count(*)");
        assertThat(applied.get("appliedAt")).isNotNull();
    }

    /**
     * 执行 normalizesFiltersAndPreventsDuplicateOpenReviews 对应的 CDP 业务操作。
     */
    @Test
    void normalizesFiltersAndPreventsDuplicateOpenReviews() {
        CdpWarehouseMetricChangeReviewFacade service = new CdpWarehouseMetricChangeReviewApplicationService();

        service.create(42L, "alice", command(
                " dwd_user_profile ",
                " profile_completeness ",
                "count_if(profile_complete)",
                List.of("country"),
                "business request"));

        assertThat(service.list(42L, " dwd_user_profile ", " profile_completeness ", " pending_review "))
                .hasSize(1)
                .first()
                .extracting(row -> row.get("status"))
                .isEqualTo("PENDING_REVIEW");
        assertThat(service.list(42L, "DWD_USER_PROFILE", "PROFILE_COMPLETENESS", "PENDING_REVIEW")).isEmpty();
        assertThat(service.list(42L, "other", null, null)).isEmpty();

        assertThatThrownBy(() -> service.create(42L, "bob", command(
                "dwd_user_profile",
                "profile_completeness",
                "count(*)",
                List.of("country"),
                "duplicate")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("open metric change review already exists");
    }

    /**
     * 校验s Required Fields And Status Transitions。
     */
    @Test
    void validatesRequiredFieldsAndStatusTransitions() {
        CdpWarehouseMetricChangeReviewFacade service = new CdpWarehouseMetricChangeReviewApplicationService();

        assertThatThrownBy(() -> service.create(0L, "alice", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("metric change command is required");
        assertThatThrownBy(() -> service.create(0L, "alice", command("", "metric", "count(*)", List.of(), "reason")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("datasetKey is required");

        Map<String, Object> created = service.create(0L, "alice", command(
                "dwd_user_profile",
                "profile_completeness",
                "count(*)",
                List.of("country"),
                "reason"));
        Long reviewId = (Long) created.get("id");

        assertThatThrownBy(() -> service.reject(0L, "reviewer", reviewId, ""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("reviewNote is required");
        assertThatThrownBy(() -> service.apply(0L, "reviewer", reviewId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("APPROVED");
    }

    /**
     * 执行 command 对应的 CDP 业务操作。
     */
    private static MetricChangeCommand command(
            String datasetKey,
            String metricKey,
            String proposedExpression,
            List<String> proposedAllowedDimensions,
            String reason) {
        return new MetricChangeCommand(datasetKey, metricKey, proposedExpression, proposedAllowedDimensions, reason);
    }
}
