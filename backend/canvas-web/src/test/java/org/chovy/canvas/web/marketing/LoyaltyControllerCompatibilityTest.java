package org.chovy.canvas.web.marketing;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.chovy.canvas.marketing.api.LoyaltyFacade;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

class LoyaltyControllerCompatibilityTest {

    private static final Long DEFAULT_TENANT_ID = 0L;
    private static final Long HEADER_TENANT_ID = 42L;

    @Test
    void earnRoutePreservesPathTenantAndRequestBodyMapping() {
        RecordingLoyaltyFacade facade = new RecordingLoyaltyFacade();

        webClient(facade)
                .post()
                .uri("/canvas/loyalty/users/user-1/earn")
                .header("X-Tenant-Id", HEADER_TENANT_ID.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                          "transactionKey": "order-1",
                          "points": 120,
                          "pointsType": "BONUS",
                          "sourceType": "ORDER",
                          "sourceId": "ord-1",
                          "reason": "paid order",
                          "expiresAt": "2026-07-01T00:00:00"
                        }
                        """)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.message").isEqualTo("success")
                .jsonPath("$.errorCode").doesNotExist()
                .jsonPath("$.traceId").doesNotExist()
                .jsonPath("$.data.tenantId").isEqualTo(HEADER_TENANT_ID.intValue())
                .jsonPath("$.data.userId").isEqualTo("user-1")
                .jsonPath("$.data.pointsBalance").isEqualTo(120);

        assertThat(facade.operations).containsExactly("earn");
        assertThat(facade.lastTenantId).isEqualTo(HEADER_TENANT_ID);
        assertThat(facade.lastUserId).isEqualTo("user-1");
        assertThat(facade.lastEarnCommand.transactionKey()).isEqualTo("order-1");
        assertThat(facade.lastEarnCommand.points()).isEqualTo(120);
        assertThat(facade.lastEarnCommand.pointsType()).isEqualTo("BONUS");
        assertThat(facade.lastEarnCommand.expiresAt()).isEqualTo(LocalDateTime.parse("2026-07-01T00:00:00"));
    }

    @Test
    void accountBenefitsAndRedeemRoutesUseCompatibilityDefaultsAndEnvelope() {
        RecordingLoyaltyFacade facade = new RecordingLoyaltyFacade();
        WebTestClient client = webClient(facade);

        client.get()
                .uri("/canvas/loyalty/users/user-2/account")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.data.tenantId").isEqualTo(DEFAULT_TENANT_ID.intValue())
                .jsonPath("$.data.userId").isEqualTo("user-2");

        client.get()
                .uri("/canvas/loyalty/users/user-2/benefits")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.data").isArray()
                .jsonPath("$.data[0].benefitKey").isEqualTo("silver_coupon");

        client.post()
                .uri("/canvas/loyalty/users/user-2/redeem")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                          "redemptionKey": "redeem-1",
                          "rewardKey": "coupon-50",
                          "pointsCost": 200,
                          "reason": "checkout"
                        }
                        """)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.data.redemptionKey").isEqualTo("redeem-1")
                .jsonPath("$.data.status").isEqualTo("REDEEMED");

        assertThat(facade.operations).containsExactly("account", "benefits", "redeem");
        assertThat(facade.lastTenantId).isEqualTo(DEFAULT_TENANT_ID);
        assertThat(facade.lastRedemptionCommand.rewardKey()).isEqualTo("coupon-50");
        assertThat(facade.lastRedemptionCommand.pointsCost()).isEqualTo(200);
    }

    @Test
    void illegalArgumentMapsToApi001BadRequestEnvelope() {
        RecordingLoyaltyFacade facade = new RecordingLoyaltyFacade();
        facade.failEarn = true;

        webClient(facade)
                .post()
                .uri("/canvas/loyalty/users/user-1/earn")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"transactionKey": "", "points": 0}
                        """)
                .exchange()
                .expectStatus().isBadRequest()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.code").isEqualTo(400)
                .jsonPath("$.errorCode").isEqualTo("API_001")
                .jsonPath("$.message").isEqualTo("loyalty earn transaction key is required")
                .jsonPath("$.data").doesNotExist()
                .jsonPath("$.traceId").doesNotExist();
    }

    private static WebTestClient webClient(LoyaltyFacade facade) {
        return WebTestClient.bindToController(new LoyaltyController(facade)).build();
    }

    private static final class RecordingLoyaltyFacade implements LoyaltyFacade {
        private final List<String> operations = new ArrayList<>();
        private Long lastTenantId;
        private String lastUserId;
        private EarnCommand lastEarnCommand;
        private RedemptionCommand lastRedemptionCommand;
        private boolean failEarn;

        @Override
        public LoyaltyAccountView account(Long tenantId, String userId) {
            operations.add("account");
            lastTenantId = tenantId;
            lastUserId = userId;
            return accountView(tenantId, userId, 0);
        }

        @Override
        public LoyaltyAccountView earn(Long tenantId, String userId, EarnCommand command) {
            operations.add("earn");
            if (failEarn) {
                throw new IllegalArgumentException("loyalty earn transaction key is required");
            }
            lastTenantId = tenantId;
            lastUserId = userId;
            lastEarnCommand = command;
            return accountView(tenantId, userId, command.points());
        }

        @Override
        public RedemptionView redeem(Long tenantId, String userId, RedemptionCommand command) {
            operations.add("redeem");
            lastTenantId = tenantId;
            lastUserId = userId;
            lastRedemptionCommand = command;
            return new RedemptionView(99L, command.redemptionKey(), command.rewardKey(), command.pointsCost(),
                    "REDEEMED", null, LocalDateTime.parse("2026-06-14T10:00:00"));
        }

        @Override
        public List<BenefitEligibilityView> eligibleBenefits(Long tenantId, String userId) {
            operations.add("benefits");
            lastTenantId = tenantId;
            lastUserId = userId;
            return List.of(new BenefitEligibilityView("silver_coupon", "Silver Coupon", "SILVER", true,
                    "tier eligible"));
        }

        private static LoyaltyAccountView accountView(Long tenantId, String userId, int pointsBalance) {
            return new LoyaltyAccountView(1L, tenantId, userId, "M-" + tenantId + "-" + userId, "SILVER",
                    pointsBalance, pointsBalance, "ACTIVE");
        }
    }
}
