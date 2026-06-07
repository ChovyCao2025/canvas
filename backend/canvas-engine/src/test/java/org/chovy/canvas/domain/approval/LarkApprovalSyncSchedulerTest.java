package org.chovy.canvas.domain.approval;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LarkApprovalSyncSchedulerTest {

    @Test
    void disabledSchedulerSkipsLarkSync() {
        ApprovalWorkflowService workflowService = mock(ApprovalWorkflowService.class);
        LarkApprovalSyncScheduler scheduler =
                new LarkApprovalSyncScheduler(workflowService, false, 7L, 100);

        LarkApprovalSyncScheduler.SyncRunResult result = scheduler.runScheduledOnce();

        assertThat(result.executed()).isFalse();
        assertThat(result.synced()).isZero();
        verify(workflowService, never()).syncPendingExternalInstances(7L, 100);
    }

    @Test
    void enabledSchedulerRunsTenantScopedBoundedSync() {
        ApprovalWorkflowService workflowService = mock(ApprovalWorkflowService.class);
        when(workflowService.syncPendingExternalInstances(7L, 25)).thenReturn(3);
        LarkApprovalSyncScheduler scheduler =
                new LarkApprovalSyncScheduler(workflowService, true, 7L, 25);

        LarkApprovalSyncScheduler.SyncRunResult result = scheduler.runScheduledOnce();

        assertThat(result.executed()).isTrue();
        assertThat(result.synced()).isEqualTo(3);
        verify(workflowService).syncPendingExternalInstances(7L, 25);
    }

    @Test
    void overlapGuardSkipsNestedSyncCycle() {
        ApprovalWorkflowService workflowService = mock(ApprovalWorkflowService.class);
        LarkApprovalSyncScheduler scheduler =
                new LarkApprovalSyncScheduler(workflowService, true, 7L, 25);
        AtomicBoolean nestedExecuted = new AtomicBoolean(true);
        doAnswer(invocation -> {
            nestedExecuted.set(scheduler.runScheduledOnce().executed());
            return 1;
        }).when(workflowService).syncPendingExternalInstances(7L, 25);

        LarkApprovalSyncScheduler.SyncRunResult result = scheduler.runScheduledOnce();

        assertThat(result.executed()).isTrue();
        assertThat(result.synced()).isEqualTo(1);
        assertThat(nestedExecuted).isFalse();
    }
}
