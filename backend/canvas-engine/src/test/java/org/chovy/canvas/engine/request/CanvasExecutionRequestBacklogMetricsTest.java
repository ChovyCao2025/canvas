package org.chovy.canvas.engine.request;

import org.chovy.canvas.dal.mapper.CanvasExecutionRequestMapper;
import org.chovy.canvas.domain.execution.CanvasExecutionRequestStatusCount;
import org.chovy.canvas.engine.scheduler.CanvasMetrics;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.chovy.canvas.domain.execution.CanvasExecutionRequestStatusCount;

class CanvasExecutionRequestBacklogMetricsTest {

    @Test
    void refreshPublishesActiveStatusesAndZerosMissingActiveStatuses() {
        CanvasExecutionRequestMapper mapper = mock(CanvasExecutionRequestMapper.class);
        CanvasMetrics metrics = mock(CanvasMetrics.class);
        CanvasExecutionRequestBacklogMetrics backlogMetrics =
                new CanvasExecutionRequestBacklogMetrics(mapper, metrics);
        when(mapper.countByStatus()).thenReturn(List.of(
                new CanvasExecutionRequestStatusCount(CanvasExecutionRequestStatus.PENDING, 3L),
                new CanvasExecutionRequestStatusCount(CanvasExecutionRequestStatus.RETRY, 2L),
                new CanvasExecutionRequestStatusCount(CanvasExecutionRequestStatus.SUCCEEDED, 99L)
        ));

        backlogMetrics.refresh();

        verify(metrics).setExecutionRequestBacklog(CanvasExecutionRequestStatus.PENDING, 3L);
        verify(metrics).setExecutionRequestBacklog(CanvasExecutionRequestStatus.RETRY, 2L);
        verify(metrics).setExecutionRequestBacklog(CanvasExecutionRequestStatus.RUNNING, 0L);
        verify(metrics, never()).setExecutionRequestBacklog(CanvasExecutionRequestStatus.SUCCEEDED, 99L);
        verify(metrics, never()).setExecutionRequestBacklog(CanvasExecutionRequestStatus.FAILED, 0L);
    }
}
