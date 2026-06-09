package org.chovy.canvas.engine.request;

public interface ExecutionRequestRetryPressureSource {

    Snapshot snapshot();

    static ExecutionRequestRetryPressureSource healthy() {
        return () -> Snapshot.healthy();
    }

    record Snapshot(
            AdaptiveRetryBackoffPolicy.LanePressureSnapshot lanePressure,
            AdaptiveRetryBackoffPolicy.DownstreamErrorSnapshot downstreamErrors,
            AdaptiveRetryBackoffPolicy.DlqGrowthSnapshot dlqGrowth) {

        public static Snapshot healthy() {
            return new Snapshot(
                    AdaptiveRetryBackoffPolicy.LanePressureSnapshot.healthy(),
                    AdaptiveRetryBackoffPolicy.DownstreamErrorSnapshot.healthy(),
                    AdaptiveRetryBackoffPolicy.DlqGrowthSnapshot.healthy());
        }
    }
}
