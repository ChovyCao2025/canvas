package org.chovy.canvas.cdp.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;

import org.junit.jupiter.api.Test;

/**
 * 验证 CdpWarehouseAudienceApplicationService 的核心行为。
 */
class CdpWarehouseAudienceApplicationServiceTest {

    /**
     * 执行 materializationOperationsCreateFilterableRunsWithDeterministicFields 对应的 CDP 业务操作。
     */
    @Test
    void materializationOperationsCreateFilterableRunsWithDeterministicFields() {
        CdpWarehouseAudienceApplicationService service = service();

        Map<String, Object> materialized = service.materialize(9L, 42L, "ada");
        Map<String, Object> gated = service.materializeGated(9L, 42L,
                Map.of("mode", "strict", "allowWarn", true), "ada");
        Map<String, Object> rollback = service.rollback(9L, 42L,
                Map.of("targetVersion", 1L, "reason", "bad segment"), "ada");

        assertThat(materialized)
                .containsEntry("tenantId", 9L)
                .containsEntry("audienceId", 42L)
                .containsEntry("operation", "MATERIALIZE")
                .containsEntry("status", "SUCCEEDED")
                .containsEntry("operator", "ada")
                .containsEntry("startedAt", "2026-06-14T00:00:00Z");
        assertThat(gated).containsEntry("gateMode", "STRICT").containsEntry("gateDecision", "WARN_ALLOWED");
        assertThat(rollback).containsEntry("status", "ROLLED_BACK").containsEntry("targetVersion", 1L);
        assertThat(rollback).containsEntry("reason", "bad segment");
        assertThat(service.recentRuns(9L, 42L, "SUCCEEDED", 10))
                .extracting(run -> run.get("operation"))
                .containsExactly("MATERIALIZE", "MATERIALIZE_GATED");
    }

    /**
     * 校验s Contract And Rollback Inputs Without Ceremonial Coverage。
     */
    @Test
    void validatesContractAndRollbackInputsWithoutCeremonialCoverage() {
        CdpWarehouseAudienceApplicationService service = service();

        assertThatThrownBy(() -> service.materializeContractGated(9L, 42L, Map.of(), "ada"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("contractKey is required");
        assertThatThrownBy(() -> service.rollback(9L, 42L, Map.of("reason", "bad segment"), "ada"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("targetVersion is required");
        assertThatThrownBy(() -> service.materialize(9L, 0L, "ada"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("audienceId is required");
    }

    /**
     * 执行 refreshDueCreatesBoundedRunsAndUsesDefaultActor 对应的 CDP 业务操作。
     */
    @Test
    void refreshDueCreatesBoundedRunsAndUsesDefaultActor() {
        CdpWarehouseAudienceApplicationService service = service();

        Map<String, Object> defaultRefresh = service.refreshDue(null, Map.of(), "");
        Map<String, Object> result = service.refreshDueGated(null, Map.of("limit", 2, "mode", "hybrid"), "");

        assertThat(defaultRefresh)
                .containsEntry("limit", 0)
                .containsEntry("refreshedCount", 0);
        assertThat(result)
                .containsEntry("tenantId", 0L)
                .containsEntry("gated", true)
                .containsEntry("refreshedCount", 2)
                .containsEntry("operator", "system");
        assertThat(service.recentRuns(0L, null, null, 10))
                .extracting(run -> run.get("operation"))
                .containsExactly("REFRESH_DUE_GATED", "REFRESH_DUE_GATED");
    }

    /**
     * 执行 service 对应的 CDP 业务操作。
     */
    private static CdpWarehouseAudienceApplicationService service() {
        return new CdpWarehouseAudienceApplicationService(
                Clock.fixed(Instant.parse("2026-06-14T00:00:00Z"), ZoneOffset.UTC));
    }
}
