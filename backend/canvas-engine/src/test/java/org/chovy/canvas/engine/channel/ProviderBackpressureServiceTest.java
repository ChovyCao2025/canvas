package org.chovy.canvas.engine.channel;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ProviderBackpressureServiceTest {

    @Test
    void perSecondLimitIsIsolatedByTenantChannelProviderAndOperation() {
        ProviderBackpressureService.CounterStore counters = new ProviderBackpressureService.InMemoryCounterStore();
        ProviderBackpressureService service = new ProviderBackpressureService(counters, key ->
                new ProviderBackpressureService.ProviderLimit(1, 100L, true));

        assertThat(service.decide(0L, "SMS", "ALIYUN", "SEND", false).status()).isEqualTo("ALLOWED");
        assertThat(service.decide(0L, "SMS", "ALIYUN", "SEND", false).status()).isEqualTo("THROTTLED_RETRY");
        assertThat(service.decide(0L, "EMAIL", "ALIYUN", "SEND", false).status()).isEqualTo("ALLOWED");
    }

    @Test
    void counterUnavailableFailsClosedForRealModeAndBypassesSandbox() {
        ProviderBackpressureService.CounterStore broken = key -> {
            throw new IllegalStateException("counter down");
        };
        ProviderBackpressureService service = new ProviderBackpressureService(broken, key ->
                new ProviderBackpressureService.ProviderLimit(1, 100L, true));

        assertThat(service.decide(0L, "SMS", "ALIYUN", "SEND", false).status()).isEqualTo("REGISTRY_UNAVAILABLE");
        assertThat(service.decide(0L, "SMS", "SANDBOX", "SEND", true).status()).isEqualTo("ALLOWED");
    }
}
