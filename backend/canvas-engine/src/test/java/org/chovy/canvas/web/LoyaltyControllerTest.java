package org.chovy.canvas.web;

import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.loyalty.LoyaltyService;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LoyaltyControllerTest {

    @Test
    void earnUsesCurrentTenantAndReturnsAccountView() {
        TenantContextResolver resolver = mock(TenantContextResolver.class);
        when(resolver.current()).thenReturn(Mono.just(new TenantContext(7L, "TENANT_ADMIN", "alice")));
        LoyaltyService service = mock(LoyaltyService.class);
        AtomicReference<Long> tenantSeen = new AtomicReference<>();
        when(service.earn(org.mockito.ArgumentMatchers.eq(7L), org.mockito.ArgumentMatchers.eq("user-1"),
                org.mockito.ArgumentMatchers.any())).thenAnswer(invocation -> {
            tenantSeen.set(invocation.getArgument(0));
            return account();
        });
        LoyaltyController controller = new LoyaltyController(resolver, service);

        StepVerifier.create(controller.earn("user-1", new LoyaltyService.EarnCommand(
                        "earn-1", 100, "BASE", "ORDER", "order-1", "paid order", null)))
                .assertNext(response -> {
                    assertThat(response.getCode()).isEqualTo(0);
                    assertThat(tenantSeen.get()).isEqualTo(7L);
                    assertThat(response.getData().pointsBalance()).isEqualTo(100);
                })
                .verifyComplete();
    }

    @Test
    void benefitsUseCurrentTenant() {
        TenantContextResolver resolver = mock(TenantContextResolver.class);
        when(resolver.current()).thenReturn(Mono.just(new TenantContext(7L, "TENANT_ADMIN", "alice")));
        LoyaltyService service = mock(LoyaltyService.class);
        when(service.eligibleBenefits(7L, "user-1")).thenReturn(List.of(new LoyaltyService.BenefitEligibilityView(
                "gold_shipping",
                "Gold Shipping",
                "GOLD",
                true,
                "tier eligible")));
        LoyaltyController controller = new LoyaltyController(resolver, service);

        StepVerifier.create(controller.benefits("user-1"))
                .assertNext(response -> assertThat(response.getData()).singleElement()
                        .satisfies(benefit -> assertThat(benefit.benefitKey()).isEqualTo("gold_shipping")))
                .verifyComplete();
    }

    private LoyaltyService.LoyaltyAccountView account() {
        return new LoyaltyService.LoyaltyAccountView(
                101L,
                7L,
                "user-1",
                "M-1",
                "BASIC",
                100,
                100,
                "ACTIVE");
    }
}
