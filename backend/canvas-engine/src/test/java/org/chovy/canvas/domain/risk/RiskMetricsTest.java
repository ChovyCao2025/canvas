package org.chovy.canvas.domain.risk;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.chovy.canvas.config.CanvasRuntimeMetrics;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class RiskMetricsTest {

    @Test
    void recordsRiskControlMeters() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        CanvasRuntimeMetrics metrics = new CanvasRuntimeMetrics(registry);

        metrics.recordRiskDecision("payment", "BLOCK", 42);
        metrics.recordRiskDecisionFailure("payment", "timeout");
        metrics.recordRiskRuleHit("payment", "velocity", "ip_velocity", "BLOCK");
        metrics.recordRiskFeatureMissing("payment", "user.fail_count_1d");
        metrics.recordRiskStrategyActivation("payment", "payment-risk", "APPROVED");
        metrics.recordRiskModelGatewayFailure("payment", "payment-risk", "timeout");
        metrics.recordRiskListImport("block_device", "accepted", 12);
        metrics.recordRiskSimulationRun("payment", "COMPLETED", 200, 120);

        assertThat(registry.get("risk_decision_requests_total")
                .tag("scene", "payment")
                .tag("action", "block")
                .counter().count()).isEqualTo(1d);
        assertThat(registry.get("risk_decision_latency_ms")
                .tag("scene", "payment")
                .tag("action", "block")
                .timer().totalTime(TimeUnit.MILLISECONDS)).isEqualTo(42d);
        assertThat(registry.get("risk_decision_failures_total")
                .tag("scene", "payment")
                .tag("reason", "timeout")
                .counter().count()).isEqualTo(1d);
        assertThat(registry.get("risk_rule_hits_total")
                .tag("scene", "payment")
                .tag("group", "velocity")
                .tag("rule", "ip_velocity")
                .tag("action", "block")
                .counter().count()).isEqualTo(1d);
        assertThat(registry.get("risk_feature_missing_total")
                .tag("scene", "payment")
                .tag("feature", "user.fail_count_1d")
                .counter().count()).isEqualTo(1d);
        assertThat(registry.get("risk_strategy_activations_total")
                .tag("scene", "payment")
                .tag("strategy", "payment-risk")
                .tag("status", "approved")
                .counter().count()).isEqualTo(1d);
        assertThat(registry.get("risk_model_gateway_failures_total")
                .tag("scene", "payment")
                .tag("model", "payment-risk")
                .tag("reason", "timeout")
                .counter().count()).isEqualTo(1d);
        assertThat(registry.get("risk_list_import_rows_total")
                .tag("list", "block_device")
                .tag("status", "accepted")
                .counter().count()).isEqualTo(12d);
        assertThat(registry.get("risk_simulation_runs_total")
                .tag("scene", "payment")
                .tag("status", "completed")
                .counter().count()).isEqualTo(1d);
        assertThat(registry.get("risk_simulation_samples_total")
                .tag("scene", "payment")
                .tag("status", "completed")
                .counter().count()).isEqualTo(200d);
        assertThat(registry.get("risk_simulation_latency_ms")
                .tag("scene", "payment")
                .tag("status", "completed")
                .timer().totalTime(TimeUnit.MILLISECONDS)).isEqualTo(120d);
    }
}
