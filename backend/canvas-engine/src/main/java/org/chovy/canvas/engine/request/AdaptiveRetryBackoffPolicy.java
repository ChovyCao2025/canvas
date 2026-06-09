package org.chovy.canvas.engine.request;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class AdaptiveRetryBackoffPolicy {

    public Decision evaluate(Input input) {
        if (input.nextAttempt() >= input.maxAttempts()) {
            return new Decision(0, "MAX_ATTEMPTS_EXCEEDED", 0.0, true);
        }
        long baseDelay = exponentialDelay(input.nextAttempt(), input.baseDelayMs(), input.maxDelayMs());
        Multiplier multiplier = pressureMultiplier(
                input.lanePressure(),
                input.downstreamErrors(),
                input.dlqGrowth());
        long adjusted = Math.min(input.maxDelayMs(), Math.round(baseDelay * multiplier.value()));
        return new Decision(
                adjusted,
                multiplier.reason(),
                multiplier.value(),
                false);
    }

    private long exponentialDelay(int nextAttempt, long baseDelayMs, long maxDelayMs) {
        long base = Math.max(1L, baseDelayMs);
        long cap = Math.max(base, maxDelayMs);
        int exponent = Math.max(0, nextAttempt - 1);
        long delay = base;
        for (int i = 0; i < exponent; i++) {
            if (delay >= cap / 2) {
                return cap;
            }
            delay *= 2;
        }
        return Math.min(delay, cap);
    }

    private Multiplier pressureMultiplier(LanePressureSnapshot lanePressure,
                                          DownstreamErrorSnapshot downstreamErrors,
                                          DlqGrowthSnapshot dlqGrowth) {
        double multiplier = 1.0;
        List<String> reasons = new ArrayList<>();
        if (lanePressure != null && lanePressure.mainLaneP99Ms() > lanePressure.p99GateMs()) {
            multiplier *= 2.0;
            reasons.add("LANE_P99");
        }
        if (downstreamErrors != null && downstreamErrors.errorRatePercent() > downstreamErrors.errorRateGatePercent()) {
            multiplier *= 2.0;
            reasons.add("DOWNSTREAM_ERROR");
        }
        if (dlqGrowth != null && dlqGrowth.growthPerMinute() > dlqGrowth.growthGatePerMinute()) {
            multiplier *= 2.0;
            reasons.add("DLQ_GROWTH");
        }
        if (reasons.isEmpty()) {
            return new Multiplier(1.0, "BASE_EXPONENTIAL");
        }
        return new Multiplier(multiplier, String.join("+", reasons));
    }

    private record Multiplier(double value, String reason) {
    }

    public record Input(
            int nextAttempt,
            long baseDelayMs,
            long maxDelayMs,
            int maxAttempts,
            LanePressureSnapshot lanePressure,
            DownstreamErrorSnapshot downstreamErrors,
            DlqGrowthSnapshot dlqGrowth) {
    }

    public record LanePressureSnapshot(long mainLaneP99Ms, long p99GateMs) {
        public static LanePressureSnapshot healthy() {
            return new LanePressureSnapshot(0, Long.MAX_VALUE);
        }
    }

    public record DownstreamErrorSnapshot(double errorRatePercent, double errorRateGatePercent) {
        public static DownstreamErrorSnapshot healthy() {
            return new DownstreamErrorSnapshot(0.0, 100.0);
        }
    }

    public record DlqGrowthSnapshot(long growthPerMinute, long growthGatePerMinute) {
        public static DlqGrowthSnapshot healthy() {
            return new DlqGrowthSnapshot(0, Long.MAX_VALUE);
        }
    }

    public record Decision(
            long delayMs,
            String reason,
            double pressureMultiplier,
            boolean maxAttemptsExceeded) {
    }
}
