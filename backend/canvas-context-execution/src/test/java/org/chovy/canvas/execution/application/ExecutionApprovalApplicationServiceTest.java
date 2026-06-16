package org.chovy.canvas.execution.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.chovy.canvas.execution.api.ExecutionApprovalFacade;
import org.junit.jupiter.api.Test;

/**
 * 定义 ExecutionApprovalApplicationServiceTest 的执行上下文数据结构或业务契约。
 */
class ExecutionApprovalApplicationServiceTest {

    /**
     * 执行 approveAndRejectRecordPendingApprovalDecisionWithActorAndComment 对应的业务处理。
     */
    @Test
    void approveAndRejectRecordPendingApprovalDecisionWithActorAndComment() {
        ExecutionApprovalFacade service = new ExecutionApprovalApplicationService();

        ExecutionApprovalFacade.ExecutionApprovalDecision approved =
                service.approve(7L, "exec-pending", "operator-1", "OPS");
        ExecutionApprovalFacade.ExecutionApprovalDecision rejected =
                service.reject(7L, "exec-reject", "operator-2", "risk mismatch", "OPS");

        assertThat(approved.executionId()).isEqualTo("exec-pending");
        assertThat(approved.result()).isEqualTo("APPROVED");
        assertThat(approved.resultBy()).isEqualTo("operator-1");
        assertThat(approved.comment()).isNull();
        assertThat(rejected.executionId()).isEqualTo("exec-reject");
        assertThat(rejected.result()).isEqualTo("REJECTED");
        assertThat(rejected.resultBy()).isEqualTo("operator-2");
        assertThat(rejected.comment()).isEqualTo("risk mismatch");
    }

    /**
     * 执行 enforcesTenantAndApproverButTreatsMissingPendingApprovalAsNoop 对应的业务处理。
     */
    @Test
    void enforcesTenantAndApproverButTreatsMissingPendingApprovalAsNoop() {
        ExecutionApprovalFacade service = new ExecutionApprovalApplicationService();

        ExecutionApprovalFacade.ExecutionApprovalDecision noop =
                service.approve(7L, "exec-already-approved", "operator-1", "OPS");

        assertThat(noop.result()).isEqualTo("NOOP");
        assertThat(noop.resultBy()).isEqualTo("operator-1");

        assertThatThrownBy(() -> service.approve(8L, "exec-pending", "operator-1", "OPS"))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("current tenant cannot access approval");
        assertThatThrownBy(() -> service.approve(7L, "exec-pending", "outsider", "OPS"))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("AUTH_003: current user is not an approver");
    }
}
