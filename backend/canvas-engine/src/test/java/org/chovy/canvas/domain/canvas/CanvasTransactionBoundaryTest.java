package org.chovy.canvas.domain.canvas;

import org.chovy.canvas.dal.mapper.CanvasExecutionMapper;
import org.chovy.canvas.dal.mapper.CanvasExecutionRequestMapper;
import org.chovy.canvas.dal.mapper.CanvasMapper;
import org.chovy.canvas.dal.mapper.CanvasVersionMapper;
import org.chovy.canvas.engine.trigger.TriggerPreCheckService;
import org.chovy.canvas.infrastructure.redis.TriggerRouteService;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
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
}
