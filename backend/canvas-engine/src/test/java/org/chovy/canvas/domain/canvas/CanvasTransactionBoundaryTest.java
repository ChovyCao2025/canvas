package org.chovy.canvas.domain.canvas;

import org.chovy.canvas.dal.mapper.CanvasExecutionMapper;
import org.chovy.canvas.dal.mapper.CanvasExecutionRequestMapper;
import org.chovy.canvas.dal.mapper.CanvasMapper;
import org.chovy.canvas.dal.mapper.CanvasVersionMapper;
import org.chovy.canvas.dal.dataobject.CanvasDO;
import org.chovy.canvas.dal.dataobject.CanvasVersionDO;
import org.chovy.canvas.common.enums.CanvasStatusEnum;
import org.chovy.canvas.engine.trigger.TriggerPreCheckService;
import org.chovy.canvas.infrastructure.redis.TriggerRouteService;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CanvasTransactionBoundaryTest {

    @Test
    void canvasTransactionServiceStaysDbOnly() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/org/chovy/canvas/domain/canvas/CanvasTransactionService.java"));

        assertThat(source)
                .doesNotContain("TriggerRouteService")
                .doesNotContain("CanvasSchedulerService")
                .doesNotContain("CanvasConfigCache")
                .doesNotContain("StringRedisTemplate")
                .doesNotContain("CanvasExecutionService");
    }

    @Test
    void killSwitchChangesDatabaseStateBeforeExternalSideEffects() {
        CanvasTransactionService transactionService = mock(CanvasTransactionService.class);
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        CanvasExecutionMapper executionMapper = mock(CanvasExecutionMapper.class);
        CanvasExecutionRequestMapper executionRequestMapper = mock(CanvasExecutionRequestMapper.class);
        CanvasService canvasService = mock(CanvasService.class);
        CanvasOpsService opsService = new CanvasOpsService(
                mock(CanvasMapper.class),
                mock(CanvasVersionMapper.class),
                executionMapper,
                executionRequestMapper,
                mock(TriggerRouteService.class),
                mock(TriggerPreCheckService.class),
                transactionService,
                new CanvasStateTransitionPolicy(),
                canvasService,
                redis);
        when(transactionService.killDb(7L)).thenReturn(55L);

        opsService.kill(7L, "FORCE");

        var inOrder = inOrder(transactionService, redis, executionMapper, executionRequestMapper, canvasService);
        inOrder.verify(transactionService).killDb(7L);
        inOrder.verify(redis).convertAndSend("canvas:kill:7", "FORCE");
        inOrder.verify(executionMapper).update(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        inOrder.verify(executionRequestMapper)
                .markForceCancelledByCanvas(org.mockito.ArgumentMatchers.eq(7L), org.mockito.ArgumentMatchers.any());
        inOrder.verify(canvasService).applyKillExternalCleanup(7L, 55L);
    }

    @Test
    void startCanaryInvalidatesRuntimeCanvasAfterDatabaseUpdate() {
        CanvasMapper canvasMapper = mock(CanvasMapper.class);
        CanvasVersionMapper versionMapper = mock(CanvasVersionMapper.class);
        CanvasService canvasService = mock(CanvasService.class);
        CanvasOpsService opsService = newOpsService(canvasMapper, versionMapper, canvasService);
        CanvasDO canvas = publishedCanvas(7L);
        CanvasVersionDO draft = version(11L, 3);
        CanvasVersionDO max = version(12L, 4);
        when(canvasMapper.selectById(7L)).thenReturn(canvas);
        when(versionMapper.selectOne(any())).thenReturn(draft, max);

        opsService.startCanary(7L, 25, "operator");

        var inOrder = inOrder(versionMapper, canvasMapper, canvasService);
        inOrder.verify(versionMapper).insert(any(CanvasVersionDO.class));
        inOrder.verify(canvasMapper).updateById(canvas);
        inOrder.verify(canvasService).invalidateRuntimeCanvas(7L);
    }

    @Test
    void canaryRuntimeInvalidationRunsAfterTransactionCommitWhenSynchronizationIsActive() {
        CanvasMapper canvasMapper = mock(CanvasMapper.class);
        CanvasVersionMapper versionMapper = mock(CanvasVersionMapper.class);
        CanvasService canvasService = mock(CanvasService.class);
        CanvasOpsService opsService = newOpsService(canvasMapper, versionMapper, canvasService);
        CanvasDO canvas = publishedCanvas(10L);
        CanvasVersionDO draft = version(21L, 3);
        CanvasVersionDO max = version(22L, 4);
        when(canvasMapper.selectById(10L)).thenReturn(canvas);
        when(versionMapper.selectOne(any())).thenReturn(draft, max);

        TransactionSynchronizationManager.initSynchronization();
        try {
            opsService.startCanary(10L, 25, "operator");

            verify(canvasService, never()).invalidateRuntimeCanvas(10L);
            TransactionSynchronizationManager.getSynchronizations()
                    .forEach(TransactionSynchronization::afterCommit);
            verify(canvasService).invalidateRuntimeCanvas(10L);
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void promoteCanaryInvalidatesRuntimeCanvasAfterDatabaseUpdate() {
        CanvasMapper canvasMapper = mock(CanvasMapper.class);
        CanvasVersionMapper versionMapper = mock(CanvasVersionMapper.class);
        CanvasService canvasService = mock(CanvasService.class);
        CanvasOpsService opsService = newOpsService(canvasMapper, versionMapper, canvasService);
        CanvasDO canvas = publishedCanvas(8L);
        canvas.setCanaryVersionId(88L);
        when(canvasMapper.selectById(8L)).thenReturn(canvas);

        opsService.promoteCanary(8L);

        var inOrder = inOrder(canvasMapper, canvasService);
        inOrder.verify(canvasMapper).updateById(canvas);
        inOrder.verify(canvasService).invalidateRuntimeCanvas(8L);
    }

    @Test
    void rollbackCanaryInvalidatesRuntimeCanvasAfterDatabaseUpdate() {
        CanvasMapper canvasMapper = mock(CanvasMapper.class);
        CanvasVersionMapper versionMapper = mock(CanvasVersionMapper.class);
        CanvasService canvasService = mock(CanvasService.class);
        CanvasOpsService opsService = newOpsService(canvasMapper, versionMapper, canvasService);
        CanvasDO canvas = publishedCanvas(9L);
        canvas.setCanaryVersionId(99L);
        canvas.setCanaryPercent(10);
        when(canvasMapper.selectById(9L)).thenReturn(canvas);

        opsService.rollbackCanary(9L);

        var inOrder = inOrder(canvasMapper, canvasService);
        inOrder.verify(canvasMapper).updateById(canvas);
        inOrder.verify(canvasService).invalidateRuntimeCanvas(9L);
    }

    private static CanvasOpsService newOpsService(CanvasMapper canvasMapper,
                                                  CanvasVersionMapper versionMapper,
                                                  CanvasService canvasService) {
        return new CanvasOpsService(
                canvasMapper,
                versionMapper,
                mock(CanvasExecutionMapper.class),
                mock(CanvasExecutionRequestMapper.class),
                mock(TriggerRouteService.class),
                mock(TriggerPreCheckService.class),
                mock(CanvasTransactionService.class),
                new CanvasStateTransitionPolicy(),
                canvasService,
                mock(StringRedisTemplate.class));
    }

    private static CanvasDO publishedCanvas(Long id) {
        CanvasDO canvas = new CanvasDO();
        canvas.setId(id);
        canvas.setTenantId(1L);
        canvas.setStatus(CanvasStatusEnum.PUBLISHED.getCode());
        canvas.setPublishedVersionId(1L);
        return canvas;
    }

    private static CanvasVersionDO version(Long id, int versionNumber) {
        CanvasVersionDO version = new CanvasVersionDO();
        version.setId(id);
        version.setTenantId(1L);
        version.setCanvasId(7L);
        version.setVersion(versionNumber);
        version.setGraphJson("{\"nodes\":[]}");
        return version;
    }
}
