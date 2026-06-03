package org.chovy.canvas.domain.canvas;

import org.chovy.canvas.common.enums.ExecutionStatus;
import org.chovy.canvas.common.enums.VersionStatus;
import org.chovy.canvas.dal.dataobject.CanvasExecutionDO;
import org.chovy.canvas.dal.mapper.CanvasExecutionMapper;
import org.chovy.canvas.dal.mapper.CanvasWaitSubscriptionMapper;
import org.chovy.canvas.dal.dataobject.CanvasDO;
import org.chovy.canvas.dal.dataobject.CanvasVersionDO;
import org.chovy.canvas.dal.dataobject.CanvasWaitSubscriptionDO;
import org.chovy.canvas.dal.mapper.CanvasMapper;
import org.chovy.canvas.dal.mapper.CanvasVersionMapper;
import org.chovy.canvas.engine.wait.WaitSubscriptionService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CanvasVersionCleanupJobTest {

    @Test
    void cleanupCanvasKeepsVersionsReferencedByCanvasPointers() {
        CanvasVersionMapper versionMapper = mock(CanvasVersionMapper.class);
        CanvasMapper canvasMapper = mock(CanvasMapper.class);
        CanvasExecutionMapper executionMapper = mock(CanvasExecutionMapper.class);
        CanvasWaitSubscriptionMapper waitMapper = mock(CanvasWaitSubscriptionMapper.class);
        CanvasVersionCleanupJob job = new CanvasVersionCleanupJob(
                versionMapper, canvasMapper, executionMapper, waitMapper);
        ReflectionTestUtils.setField(job, "maxKeepCount", 1);

        CanvasDO canvas = new CanvasDO();
        canvas.setId(42L);
        canvas.setPublishedVersionId(2L);
        canvas.setPreviousVersionId(3L);
        canvas.setCanaryVersionId(4L);
        when(canvasMapper.selectById(42L)).thenReturn(canvas);

        CanvasVersionDO recent = version(5L, 5);
        CanvasVersionDO canary = version(4L, 4);
        CanvasVersionDO previous = version(3L, 3);
        CanvasVersionDO published = version(2L, 2);
        CanvasVersionDO stale = version(1L, 1);
        when(versionMapper.selectList(any())).thenReturn(List.of(recent, canary, previous, published, stale));
        when(executionMapper.selectList(any())).thenReturn(List.of());
        when(waitMapper.selectList(any())).thenReturn(List.of());

        int cleaned = job.cleanupCanvas(42L);

        assertThat(cleaned).isEqualTo(1);
        verify(versionMapper, never()).updateById(canary);
        verify(versionMapper, never()).updateById(previous);
        verify(versionMapper, never()).updateById(published);
        verify(versionMapper).updateById(stale);
        assertThat(stale.getGraphJson()).isNull();
    }

    @Test
    void cleanupCanvasKeepsVersionsReferencedByActiveExecutionAndWait() {
        CanvasVersionMapper versionMapper = mock(CanvasVersionMapper.class);
        CanvasMapper canvasMapper = mock(CanvasMapper.class);
        CanvasExecutionMapper executionMapper = mock(CanvasExecutionMapper.class);
        CanvasWaitSubscriptionMapper waitMapper = mock(CanvasWaitSubscriptionMapper.class);
        CanvasVersionCleanupJob job = new CanvasVersionCleanupJob(
                versionMapper, canvasMapper, executionMapper, waitMapper);
        ReflectionTestUtils.setField(job, "maxKeepCount", 1);

        CanvasDO canvas = new CanvasDO();
        canvas.setId(42L);
        canvas.setPublishedVersionId(4L);
        when(canvasMapper.selectById(42L)).thenReturn(canvas);

        CanvasVersionDO recent = version(4L, 4);
        CanvasVersionDO unreferenced = version(3L, 3);
        CanvasVersionDO runningVersion = version(2L, 2);
        CanvasVersionDO waitingVersion = version(1L, 1);
        when(versionMapper.selectList(any())).thenReturn(List.of(recent, unreferenced, runningVersion, waitingVersion));
        when(executionMapper.selectList(any())).thenReturn(List.of(execution(2L, ExecutionStatus.RUNNING)));
        when(waitMapper.selectList(any())).thenReturn(List.of(waitSubscription(1L)));

        int cleaned = job.cleanupCanvas(42L);

        assertThat(cleaned).isEqualTo(1);
        verify(versionMapper).updateById(unreferenced);
        verify(versionMapper, never()).updateById(runningVersion);
        verify(versionMapper, never()).updateById(waitingVersion);
        assertThat(unreferenced.getGraphJson()).isNull();
    }

    private static CanvasVersionDO version(Long id, int version) {
        CanvasVersionDO v = new CanvasVersionDO();
        v.setId(id);
        v.setCanvasId(42L);
        v.setVersion(version);
        v.setStatus(VersionStatus.PUBLISHED.getCode());
        v.setGraphJson("{\"nodes\":[]}");
        return v;
    }

    private static CanvasExecutionDO execution(Long versionId, ExecutionStatus status) {
        CanvasExecutionDO execution = new CanvasExecutionDO();
        execution.setCanvasId(42L);
        execution.setVersionId(versionId);
        execution.setStatus(status.getCode());
        return execution;
    }

    private static CanvasWaitSubscriptionDO waitSubscription(Long versionId) {
        CanvasWaitSubscriptionDO wait = new CanvasWaitSubscriptionDO();
        wait.setCanvasId(42L);
        wait.setVersionId(versionId);
        wait.setStatus(WaitSubscriptionService.STATUS_ACTIVE);
        return wait;
    }
}
