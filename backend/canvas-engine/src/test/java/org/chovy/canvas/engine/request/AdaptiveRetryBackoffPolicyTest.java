package org.chovy.canvas.engine.request;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AdaptiveRetryBackoffPolicyTest {

    private final AdaptiveRetryBackoffPolicy policy = new AdaptiveRetryBackoffPolicy();

    @Test
    void usesDefaultExponentialRetry() {
        AdaptiveRetryBackoffPolicy.Decision decision = policy.evaluate(
                new AdaptiveRetryBackoffPolicy.Input(
                        2,
                        1_000,
                        60_000,
                        5,
                        AdaptiveRetryBackoffPolicy.LanePressureSnapshot.healthy(),
                        AdaptiveRetryBackoffPolicy.DownstreamErrorSnapshot.healthy(),
                        AdaptiveRetryBackoffPolicy.DlqGrowthSnapshot.healthy()));

        assertThat(decision.delayMs()).isEqualTo(2_000);
        assertThat(decision.reason()).isEqualTo("BASE_EXPONENTIAL");
        assertThat(decision.maxAttemptsExceeded()).isFalse();
    }

    @Test
    void increasesDelayWhenMainLaneP99IsAboveGate() {
        AdaptiveRetryBackoffPolicy.Decision decision = policy.evaluate(
                new AdaptiveRetryBackoffPolicy.Input(
                        2,
                        1_000,
                        60_000,
                        5,
                        new AdaptiveRetryBackoffPolicy.LanePressureSnapshot(2_500, 1_000),
                        AdaptiveRetryBackoffPolicy.DownstreamErrorSnapshot.healthy(),
                        AdaptiveRetryBackoffPolicy.DlqGrowthSnapshot.healthy()));

        assertThat(decision.delayMs()).isGreaterThan(2_000);
        assertThat(decision.reason()).contains("LANE_P99");
        assertThat(decision.pressureMultiplier()).isGreaterThan(1.0);
    }

    @Test
    void increasesDelayWhenDlqGrowthIsAboveGate() {
        AdaptiveRetryBackoffPolicy.Decision decision = policy.evaluate(
                new AdaptiveRetryBackoffPolicy.Input(
                        1,
                        1_000,
                        60_000,
                        5,
                        AdaptiveRetryBackoffPolicy.LanePressureSnapshot.healthy(),
                        AdaptiveRetryBackoffPolicy.DownstreamErrorSnapshot.healthy(),
                        new AdaptiveRetryBackoffPolicy.DlqGrowthSnapshot(30, 10)));

        assertThat(decision.delayMs()).isGreaterThan(1_000);
        assertThat(decision.reason()).contains("DLQ_GROWTH");
    }

    @Test
    void stopsAfterMaxAttempts() {
        AdaptiveRetryBackoffPolicy.Decision decision = policy.evaluate(
                new AdaptiveRetryBackoffPolicy.Input(
                        5,
                        1_000,
                        60_000,
                        5,
                        AdaptiveRetryBackoffPolicy.LanePressureSnapshot.healthy(),
                        AdaptiveRetryBackoffPolicy.DownstreamErrorSnapshot.healthy(),
                        AdaptiveRetryBackoffPolicy.DlqGrowthSnapshot.healthy()));

        assertThat(decision.maxAttemptsExceeded()).isTrue();
        assertThat(decision.delayMs()).isZero();
    }
}
