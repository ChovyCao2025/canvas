package org.chovy.canvas.domain.marketing;

import org.chovy.canvas.dal.dataobject.GrowthRewardGrantDO;
import org.chovy.canvas.dal.dataobject.GrowthRewardPoolDO;
import org.chovy.canvas.domain.loyalty.LoyaltyService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GrowthLoyaltyAdapterTest {

    @Test
    void earnPointsDelegatesToExistingLoyaltyEarnApi() {
        LoyaltyService loyaltyService = mock(LoyaltyService.class);
        when(loyaltyService.earn(7L, "user-1", new LoyaltyService.EarnCommand(
                "growth:grant:300:earn",
                120,
                "BONUS",
                "GROWTH_ACTIVITY",
                "10",
                "growth activity reward grant 300",
                null))).thenReturn(accountView(120));
        GrowthLoyaltyAdapter adapter = new GrowthLoyaltyAdapter(loyaltyService);

        GrowthLoyaltyResult result = adapter.earnPoints(7L, 10L, pointsPool(), grant(), "user-1");

        assertThat(result.status()).isEqualTo("EARNED");
        assertThat(result.payload()).containsEntry("pointsBalance", 120);
        verify(loyaltyService).earn(7L, "user-1", new LoyaltyService.EarnCommand(
                "growth:grant:300:earn",
                120,
                "BONUS",
                "GROWTH_ACTIVITY",
                "10",
                "growth activity reward grant 300",
                null));
    }

    @Test
    void redeemBenefitDelegatesToExistingLoyaltyRedeemApi() {
        LoyaltyService loyaltyService = mock(LoyaltyService.class);
        when(loyaltyService.redeem(7L, "user-1", new LoyaltyService.RedemptionCommand(
                "growth:grant:300:redeem",
                "birthday-coupon",
                200,
                "growth activity redemption 300"))).thenReturn(redemptionView("REDEEMED"));
        GrowthLoyaltyAdapter adapter = new GrowthLoyaltyAdapter(loyaltyService);

        GrowthLoyaltyResult result = adapter.redeemBenefit(7L, loyaltyPool(), grant(), "user-1");

        assertThat(result.status()).isEqualTo("REDEEMED");
        assertThat(result.payload()).containsEntry("rewardKey", "birthday-coupon");
        verify(loyaltyService).redeem(7L, "user-1", new LoyaltyService.RedemptionCommand(
                "growth:grant:300:redeem",
                "birthday-coupon",
                200,
                "growth activity redemption 300"));
    }

    @Test
    void exposesEligibleBenefitsAndAccountWithoutDuplicatingState() {
        LoyaltyService loyaltyService = mock(LoyaltyService.class);
        when(loyaltyService.account(7L, "user-1")).thenReturn(accountView(300));
        when(loyaltyService.eligibleBenefits(7L, "user-1")).thenReturn(List.of(
                new LoyaltyService.BenefitEligibilityView("birthday-coupon", "Birthday Coupon", "BASIC", true, "tier eligible")));
        GrowthLoyaltyAdapter adapter = new GrowthLoyaltyAdapter(loyaltyService);

        assertThat(adapter.account(7L, "user-1").payload()).containsEntry("pointsBalance", 300);
        assertThat(adapter.eligibleBenefits(7L, "user-1"))
                .singleElement()
                .satisfies(benefit -> {
                    assertThat(benefit.benefitKey()).isEqualTo("birthday-coupon");
                    assertThat(benefit.eligible()).isTrue();
                });
    }

    @Test
    void rejectsUnsupportedPoolForLoyaltyAdapter() {
        GrowthLoyaltyAdapter adapter = new GrowthLoyaltyAdapter(mock(LoyaltyService.class));
        GrowthRewardPoolDO pool = pointsPool();
        pool.setRewardType("COUPON");

        assertThatThrownBy(() -> adapter.redeemBenefit(7L, pool, grant(), "user-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("growth loyalty adapter requires LOYALTY reward type");
    }

    private static GrowthRewardPoolDO pointsPool() {
        GrowthRewardPoolDO row = new GrowthRewardPoolDO();
        row.setId(100L);
        row.setTenantId(7L);
        row.setActivityId(10L);
        row.setRewardType("POINTS");
        row.setPointsType("BONUS");
        row.setMetadataJson("{\"points\":120}");
        return row;
    }

    private static GrowthRewardPoolDO loyaltyPool() {
        GrowthRewardPoolDO row = new GrowthRewardPoolDO();
        row.setId(101L);
        row.setTenantId(7L);
        row.setActivityId(10L);
        row.setRewardType("LOYALTY");
        row.setLoyaltyRewardKey("birthday-coupon");
        row.setMetadataJson("{\"pointsCost\":200}");
        return row;
    }

    private static GrowthRewardGrantDO grant() {
        GrowthRewardGrantDO row = new GrowthRewardGrantDO();
        row.setId(300L);
        row.setTenantId(7L);
        row.setActivityId(10L);
        row.setPoolId(100L);
        row.setStatus("RESERVED");
        row.setIdempotencyKey("idem");
        row.setCostAmount(BigDecimal.ZERO);
        return row;
    }

    private static LoyaltyService.LoyaltyAccountView accountView(int pointsBalance) {
        return new LoyaltyService.LoyaltyAccountView(
                900L,
                7L,
                "user-1",
                "M-7-user-1",
                "SILVER",
                pointsBalance,
                pointsBalance,
                "ACTIVE");
    }

    private static LoyaltyService.RedemptionView redemptionView(String status) {
        return new LoyaltyService.RedemptionView(
                901L,
                "growth:grant:300:redeem",
                "birthday-coupon",
                200,
                status,
                null,
                null);
    }
}
