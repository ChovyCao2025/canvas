package org.chovy.canvas.marketing.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import org.chovy.canvas.marketing.api.LoyaltyFacade;
import org.junit.jupiter.api.Test;

class LoyaltyApplicationServiceTest {

    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-06-14T02:00:00Z"),
            ZoneId.of("Asia/Shanghai"));

    @Test
    void earnCreatesTenantScopedAccountAndIsIdempotentByTransactionKey() {
        LoyaltyFacade service = new LoyaltyApplicationService(CLOCK);

        LoyaltyFacade.LoyaltyAccountView first = service.earn(7L, "user-1", new LoyaltyFacade.EarnCommand(
                "order-1", 120, "BASE", "ORDER", "ord-1", "paid order", null));
        LoyaltyFacade.LoyaltyAccountView duplicate = service.earn(7L, "user-1", new LoyaltyFacade.EarnCommand(
                "order-1", 120, "BASE", "ORDER", "ord-1", "retry", null));
        LoyaltyFacade.LoyaltyAccountView otherTenant = service.account(8L, "user-1");

        assertThat(first).extracting(
                        LoyaltyFacade.LoyaltyAccountView::tenantId,
                        LoyaltyFacade.LoyaltyAccountView::userId,
                        LoyaltyFacade.LoyaltyAccountView::tierCode,
                        LoyaltyFacade.LoyaltyAccountView::pointsBalance,
                        LoyaltyFacade.LoyaltyAccountView::lifetimePoints,
                        LoyaltyFacade.LoyaltyAccountView::status)
                .containsExactly(7L, "user-1", "SILVER", 120, 120, "ACTIVE");
        assertThat(duplicate.pointsBalance()).isEqualTo(120);
        assertThat(otherTenant).extracting(
                        LoyaltyFacade.LoyaltyAccountView::tenantId,
                        LoyaltyFacade.LoyaltyAccountView::pointsBalance,
                        LoyaltyFacade.LoyaltyAccountView::tierCode)
                .containsExactly(8L, 0, "BASIC");
    }

    @Test
    void redeemRecordsRedeemedAndFailedStatesAgainstAccountBalance() {
        LoyaltyFacade service = new LoyaltyApplicationService(CLOCK);

        service.earn(7L, "user-1", new LoyaltyFacade.EarnCommand(
                "order-1", 500, "BASE", "ORDER", "ord-1", "paid order", null));

        LoyaltyFacade.RedemptionView redeemed = service.redeem(7L, "user-1", new LoyaltyFacade.RedemptionCommand(
                "redeem-1", "coupon-50", 200, "checkout"));
        LoyaltyFacade.RedemptionView failed = service.redeem(7L, "user-1", new LoyaltyFacade.RedemptionCommand(
                "redeem-2", "coupon-500", 400, "checkout"));
        LoyaltyFacade.RedemptionView duplicate = service.redeem(7L, "user-1", new LoyaltyFacade.RedemptionCommand(
                "redeem-2", "coupon-500", 400, "retry"));

        assertThat(redeemed).extracting(
                        LoyaltyFacade.RedemptionView::redemptionKey,
                        LoyaltyFacade.RedemptionView::rewardKey,
                        LoyaltyFacade.RedemptionView::pointsCost,
                        LoyaltyFacade.RedemptionView::status,
                        LoyaltyFacade.RedemptionView::failureReason)
                .containsExactly("redeem-1", "coupon-50", 200, "REDEEMED", null);
        assertThat(service.account(7L, "user-1").pointsBalance()).isEqualTo(300);
        assertThat(failed.status()).isEqualTo("FAILED");
        assertThat(failed.failureReason()).isEqualTo("insufficient loyalty points balance");
        assertThat(duplicate.redemptionId()).isEqualTo(failed.redemptionId());
    }

    @Test
    void benefitsFollowCurrentTierAndValidationMatchesLegacyMessages() {
        LoyaltyFacade service = new LoyaltyApplicationService(CLOCK);

        service.earn(null, "user-1", new LoyaltyFacade.EarnCommand(
                "order-1", 1_000, "BASE", "ORDER", "ord-1", "paid order", null));

        List<LoyaltyFacade.BenefitEligibilityView> benefits = service.eligibleBenefits(null, "user-1");

        assertThat(service.account(null, "user-1").tenantId()).isZero();
        assertThat(benefits).extracting(LoyaltyFacade.BenefitEligibilityView::benefitKey)
                .containsExactly("birthday_bonus", "silver_coupon", "gold_shipping");
        assertThat(benefits).allSatisfy(benefit -> assertThat(benefit.eligible()).isTrue());

        assertThatThrownBy(() -> service.earn(7L, " ", new LoyaltyFacade.EarnCommand(
                "order-2", 100, "BASE", "ORDER", "ord-2", "paid order", null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("loyalty user id is required");
        assertThatThrownBy(() -> service.earn(7L, "user-1", new LoyaltyFacade.EarnCommand(
                " ", 100, "BASE", "ORDER", "ord-2", "paid order", null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("loyalty earn transaction key is required");
        assertThatThrownBy(() -> service.redeem(7L, "user-1", new LoyaltyFacade.RedemptionCommand(
                "redeem-3", "coupon", 0, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("loyalty redemption points cost must be positive");
    }
}
