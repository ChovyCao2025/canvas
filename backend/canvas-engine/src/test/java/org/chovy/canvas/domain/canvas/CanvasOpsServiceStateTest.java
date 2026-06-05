package org.chovy.canvas.domain.canvas;

import org.chovy.canvas.common.enums.CanvasStatusEnum;
import org.chovy.canvas.dal.dataobject.CanvasDO;
import org.chovy.canvas.dal.mapper.CanvasExecutionMapper;
import org.chovy.canvas.dal.mapper.CanvasExecutionRequestMapper;
import org.chovy.canvas.dal.mapper.CanvasMapper;
import org.chovy.canvas.dal.mapper.CanvasVersionMapper;
import org.chovy.canvas.engine.trigger.TriggerPreCheckService;
import org.chovy.canvas.infrastructure.redis.TriggerRouteService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CanvasOpsServiceStateTest {

    @Test
    void saveWithOptimisticLockRejectsKilledCanvasBeforeCasUpdate() {
        CanvasMapper canvasMapper = mock(CanvasMapper.class);
        CanvasVersionMapper versionMapper = mock(CanvasVersionMapper.class);
        CanvasDO canvas = killedCanvas(20L);
        when(canvasMapper.selectById(20L)).thenReturn(canvas);
        CanvasOpsService service = service(canvasMapper, versionMapper);

        assertThatThrownBy(() -> service.saveWithOptimisticLock(
                20L, "name", "desc", "{\"nodes\":[]}", 1, "operator"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("KILLED");

        verify(canvasMapper, never()).updateEditVersion(20L, 1, 2, "name", "desc");
    }

    @Test
    void promoteCanaryRejectsKilledCanvasBeforePublishedPointerUpdate() {
        CanvasMapper canvasMapper = mock(CanvasMapper.class);
        CanvasVersionMapper versionMapper = mock(CanvasVersionMapper.class);
        CanvasDO canvas = killedCanvas(21L);
        canvas.setCanaryVersionId(210L);
        when(canvasMapper.selectById(21L)).thenReturn(canvas);
        CanvasOpsService service = service(canvasMapper, versionMapper);

        assertThatThrownBy(() -> service.promoteCanary(21L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("KILLED");

        verify(canvasMapper, never()).updateById(canvas);
    }

    @Test
    void rollbackCanaryRejectsKilledCanvasBeforeCanaryPointerUpdate() {
        CanvasMapper canvasMapper = mock(CanvasMapper.class);
        CanvasVersionMapper versionMapper = mock(CanvasVersionMapper.class);
        CanvasDO canvas = killedCanvas(22L);
        canvas.setCanaryVersionId(220L);
        when(canvasMapper.selectById(22L)).thenReturn(canvas);
        CanvasOpsService service = service(canvasMapper, versionMapper);

        assertThatThrownBy(() -> service.rollbackCanary(22L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("KILLED");

        verify(canvasMapper, never()).updateById(canvas);
    }

    @Test
    void rollbackRejectsKilledCanvasBeforePublishedPointerUpdate() {
        CanvasMapper canvasMapper = mock(CanvasMapper.class);
        CanvasVersionMapper versionMapper = mock(CanvasVersionMapper.class);
        CanvasDO canvas = killedCanvas(23L);
        canvas.setPreviousVersionId(230L);
        when(canvasMapper.selectById(23L)).thenReturn(canvas);
        CanvasOpsService service = service(canvasMapper, versionMapper);

        assertThatThrownBy(() -> service.rollback(23L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("KILLED");

        verify(canvasMapper, never()).updateById(canvas);
    }

    private static CanvasOpsService service(CanvasMapper canvasMapper, CanvasVersionMapper versionMapper) {
        return new CanvasOpsService(
                canvasMapper,
                versionMapper,
                mock(CanvasExecutionMapper.class),
                mock(CanvasExecutionRequestMapper.class),
                mock(TriggerRouteService.class),
                mock(TriggerPreCheckService.class),
                mock(CanvasTransactionService.class),
                mock(CanvasService.class),
                mock(org.springframework.data.redis.core.StringRedisTemplate.class));
    }

    private static CanvasDO killedCanvas(Long id) {
        CanvasDO canvas = new CanvasDO();
        canvas.setId(id);
        canvas.setStatus(CanvasStatusEnum.KILLED.getCode());
        return canvas;
    }
}
