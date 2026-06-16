package org.chovy.canvas.execution.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;

import org.chovy.canvas.execution.api.ExecutionRerunFacade;
import org.junit.jupiter.api.Test;

/**
 * 定义 ExecutionRerunApplicationServiceTest 的执行上下文数据结构或业务契约。
 */
class ExecutionRerunApplicationServiceTest {

    /**
     * 执行 rerunCreatesSuccessfulAuditAndReturnsExecutionPayload 对应的业务处理。
     */
    @Test
    void rerunCreatesSuccessfulAuditAndReturnsExecutionPayload() {
        ExecutionRerunFacade service = new ExecutionRerunApplicationService();

        ExecutionRerunFacade.RerunResult result = service.rerun(7L, "operator-1", true, 42L,
                new ExecutionRerunFacade.RerunCommand("dry_run", "reproduce failed coupon path",
                        "user-1", null, "exec-1", Map.of("couponCode", "A10"), null));

        assertThat(result)
                .returns(1L, ExecutionRerunFacade.RerunResult::auditId)
                .returns("DRY_RUN", ExecutionRerunFacade.RerunResult::mode)
                .returns("SUCCESS", ExecutionRerunFacade.RerunResult::status);
        assertThat(result.execution()).containsEntry("canvasId", 42L)
                .containsEntry("userId", "user-1")
                .containsEntry("dryRun", true)
                .containsEntry("dedupKey", "rerun-1");

        assertThat(service.audit(7L, 1L))
                .returns(42L, ExecutionRerunFacade.AuditRow::canvasId)
                .returns("user-1", ExecutionRerunFacade.AuditRow::userId)
                .returns("exec-1", ExecutionRerunFacade.AuditRow::originalExecutionId)
                .returns("operator-1", ExecutionRerunFacade.AuditRow::operator)
                .returns("SUCCESS", ExecutionRerunFacade.AuditRow::status);
    }

    /**
     * 执行 auditsAreTenantScopedFilteredAndNewestFirst 对应的业务处理。
     */
    @Test
    void auditsAreTenantScopedFilteredAndNewestFirst() {
        ExecutionRerunFacade service = new ExecutionRerunApplicationService();

        service.rerun(7L, "operator-1", false, 42L,
                new ExecutionRerunFacade.RerunCommand("SKIP_SIDE_EFFECTS", "validate new segment safely",
                        "user-1", null, null, Map.of("segment", "gold"), null));
        service.rerun(7L, "operator-2", true, 99L,
                new ExecutionRerunFacade.RerunCommand("DRY_RUN", "check different canvas",
                        "user-2", null, null, Map.of(), null));
        service.rerun(8L, "operator-3", true, 42L,
                new ExecutionRerunFacade.RerunCommand("DRY_RUN", "tenant isolated rerun",
                        "user-3", null, null, Map.of(), null));

        assertThat(service.audits(7L, null))
                .extracting(ExecutionRerunFacade.AuditRow::id)
                .containsExactly(2L, 1L);
        assertThat(service.audits(7L, 42L))
                .singleElement()
                .returns(42L, ExecutionRerunFacade.AuditRow::canvasId)
                .returns("SKIP_SIDE_EFFECTS", ExecutionRerunFacade.AuditRow::mode);
    }

    /**
     * 执行 validationMatchesLegacyBusinessRules 对应的业务处理。
     */
    @Test
    void validationMatchesLegacyBusinessRules() {
        ExecutionRerunFacade service = new ExecutionRerunApplicationService();

        assertThatThrownBy(() -> service.rerun(7L, "operator-1", true, 42L,
                new ExecutionRerunFacade.RerunCommand("UNKNOWN", "reproduce failed coupon path",
                        "user-1", null, null, Map.of(), null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("unsupported rerun mode: UNKNOWN");
        assertThatThrownBy(() -> service.rerun(7L, "operator-1", true, 42L,
                new ExecutionRerunFacade.RerunCommand("DRY_RUN", "short",
                        "user-1", null, null, Map.of(), null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("reason must be at least 10 characters");
        assertThatThrownBy(() -> service.rerun(7L, "operator-1", true, 42L,
                new ExecutionRerunFacade.RerunCommand("DRY_RUN", "reproduce failed coupon path",
                        " ", null, null, Map.of(), null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("userId is required");
        assertThatThrownBy(() -> service.rerun(7L, "operator-1", false, 42L,
                new ExecutionRerunFacade.RerunCommand("ADMIN_REPLAY", "reproduce failed coupon path",
                        "user-1", null, null, Map.of(), null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ADMIN_REPLAY requires admin role");
        assertThatThrownBy(() -> service.audit(7L, 999L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("rerun audit not found");
    }
}
