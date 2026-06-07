package org.chovy.canvas.domain.marketing;

import org.chovy.canvas.common.MapFieldKeys;
import org.chovy.canvas.dal.dataobject.GrowthRewardGrantDO;
import org.chovy.canvas.dal.dataobject.GrowthRewardPoolDO;
import org.chovy.canvas.engine.context.ExecutionContext;
import org.chovy.canvas.engine.handler.NodeHandler;
import org.chovy.canvas.engine.handler.NodeResult;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GrowthBenefitPromotionGrantAdapterTest {

    @Test
    void grantsCouponThroughCommitActionHandlerAndMarksGrantSuccess() {
        CapturingCommitActionHandler commitAction = new CapturingCommitActionHandler(
                NodeResult.ok(null, Map.of("couponId", "c-1")));
        GrowthRewardGrantService grantService = mock(GrowthRewardGrantService.class);
        when(grantService.markSuccess(7L, 300L, Map.of("couponId", "c-1"), "operator-1"))
                .thenReturn(view("SUCCESS", Map.of("couponId", "c-1")));
        GrowthBenefitPromotionGrantAdapter adapter = new GrowthBenefitPromotionGrantAdapter(commitAction, grantService);

        GrowthBenefitGrantResult result = adapter.grantBenefit(
                7L,
                10L,
                couponPool(),
                grant("idem-coupon", new BigDecimal("20.00")),
                "user-1",
                "operator-1");

        assertThat(result.status()).isEqualTo("SUCCESS");
        assertThat(result.providerResponse()).containsEntry("couponId", "c-1");
        assertThat(commitAction.lastConfig()).containsEntry(MapFieldKeys.ACTION_TYPE, "ISSUE_COUPON")
                .containsEntry(MapFieldKeys.COUPON_TYPE_KEY, "new-user-20")
                .containsEntry(MapFieldKeys.IDEMPOTENCY_KEY, "idem-coupon")
                .containsEntry(MapFieldKeys.NODE_ID_INTERNAL, "growth-grant-300");
        assertThat(commitAction.lastContext().getTenantId()).isEqualTo(7L);
        assertThat(commitAction.lastContext().getUserId()).isEqualTo("user-1");
        verify(grantService).markSuccess(7L, 300L, Map.of("couponId", "c-1"), "operator-1");
    }

    @Test
    void grantsPointsThroughCommitActionHandlerAndMarksGrantSuccess() {
        CapturingCommitActionHandler commitAction = new CapturingCommitActionHandler(
                NodeResult.ok(null, Map.of(MapFieldKeys.POINTS_LEDGER_ID, 900L)));
        GrowthRewardGrantService grantService = mock(GrowthRewardGrantService.class);
        when(grantService.markSuccess(7L, 300L, Map.of(MapFieldKeys.POINTS_LEDGER_ID, 900L), "operator-2"))
                .thenReturn(view("SUCCESS", Map.of(MapFieldKeys.POINTS_LEDGER_ID, 900L)));
        GrowthBenefitPromotionGrantAdapter adapter = new GrowthBenefitPromotionGrantAdapter(commitAction, grantService);

        GrowthRewardPoolDO pool = pointsPool();
        GrowthBenefitGrantResult result = adapter.grantBenefit(
                7L,
                10L,
                pool,
                grant("idem-points", BigDecimal.ZERO),
                "user-2",
                "operator-2");

        assertThat(result.status()).isEqualTo("SUCCESS");
        assertThat(commitAction.lastConfig()).containsEntry(MapFieldKeys.ACTION_TYPE, "POINTS")
                .containsEntry("operation", "GRANT")
                .containsEntry("points", 50)
                .containsEntry("pointsType", "BONUS")
                .containsEntry("reason", "growth activity 10 reward grant 300");
    }

    @Test
    void marksGrantFailureWhenCommitActionFails() {
        CapturingCommitActionHandler commitAction = new CapturingCommitActionHandler(
                NodeResult.fail("provider down", Map.of("message", "provider down")));
        GrowthRewardGrantService grantService = mock(GrowthRewardGrantService.class);
        when(grantService.markFailure(7L, 300L, Map.of("message", "provider down"), "operator-3"))
                .thenReturn(view("FAILED", Map.of("message", "provider down")));
        GrowthBenefitPromotionGrantAdapter adapter = new GrowthBenefitPromotionGrantAdapter(commitAction, grantService);

        GrowthBenefitGrantResult result = adapter.grantBenefit(
                7L,
                10L,
                couponPool(),
                grant("idem-fail", BigDecimal.ONE),
                "user-3",
                "operator-3");

        assertThat(result.status()).isEqualTo("FAILED");
        verify(grantService).markFailure(7L, 300L, Map.of("message", "provider down"), "operator-3");
    }

    @Test
    void rejectsUnsupportedRewardTypeBeforeProviderCall() {
        CapturingCommitActionHandler commitAction = new CapturingCommitActionHandler(NodeResult.ok(null, Map.of()));
        GrowthBenefitPromotionGrantAdapter adapter =
                new GrowthBenefitPromotionGrantAdapter(commitAction, mock(GrowthRewardGrantService.class));
        GrowthRewardPoolDO pool = couponPool();
        pool.setRewardType("LOYALTY");

        assertThatThrownBy(() -> adapter.grantBenefit(7L, 10L, pool, grant("idem-loyalty", BigDecimal.ONE),
                "user-1", "operator-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("unsupported benefit promotion reward type");
        assertThat(commitAction.lastConfig()).isNull();
    }

    private static GrowthRewardPoolDO couponPool() {
        GrowthRewardPoolDO row = new GrowthRewardPoolDO();
        row.setId(100L);
        row.setTenantId(7L);
        row.setActivityId(10L);
        row.setPoolKey("coupon-pool");
        row.setRewardType("COUPON");
        row.setGrantChannel("COMMIT_ACTION");
        row.setCouponTypeKey("new-user-20");
        return row;
    }

    private static GrowthRewardPoolDO pointsPool() {
        GrowthRewardPoolDO row = couponPool();
        row.setRewardType("POINTS");
        row.setPointsType("BONUS");
        row.setMetadataJson("{\"points\":50}");
        return row;
    }

    private static GrowthRewardGrantDO grant(String idempotencyKey, BigDecimal costAmount) {
        GrowthRewardGrantDO row = new GrowthRewardGrantDO();
        row.setId(300L);
        row.setTenantId(7L);
        row.setActivityId(10L);
        row.setPoolId(100L);
        row.setParticipantId(200L);
        row.setGrantReason("QUALIFIED_REFERRAL");
        row.setStatus("RESERVED");
        row.setIdempotencyKey(idempotencyKey);
        row.setCostAmount(costAmount);
        return row;
    }

    private static GrowthRewardGrantView view(String status, Map<String, Object> response) {
        return new GrowthRewardGrantView(
                300L,
                7L,
                10L,
                100L,
                200L,
                null,
                null,
                "QUALIFIED_REFERRAL",
                status,
                "idem",
                Map.of(),
                response,
                BigDecimal.ZERO,
                "operator",
                "operator",
                null,
                null);
    }

    private static class CapturingCommitActionHandler implements NodeHandler {
        private final NodeResult result;
        private final AtomicReference<Map<String, Object>> config = new AtomicReference<>();
        private final AtomicReference<ExecutionContext> context = new AtomicReference<>();

        private CapturingCommitActionHandler(NodeResult result) {
            this.result = result;
        }

        @Override
        public Mono<NodeResult> executeAsync(Map<String, Object> config, ExecutionContext ctx) {
            this.config.set(config);
            this.context.set(ctx);
            return Mono.just(result);
        }

        private Map<String, Object> lastConfig() {
            return config.get();
        }

        private ExecutionContext lastContext() {
            return context.get();
        }
    }
}
