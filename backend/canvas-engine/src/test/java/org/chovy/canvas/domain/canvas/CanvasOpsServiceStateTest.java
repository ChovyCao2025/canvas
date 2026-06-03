package org.chovy.canvas.domain.canvas;

import org.chovy.canvas.common.enums.CanvasStatusEnum;
import org.chovy.canvas.dal.dataobject.CanvasDO;
import org.chovy.canvas.dal.mapper.CanvasMapper;
import org.chovy.canvas.dal.mapper.CanvasVersionMapper;
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
        CanvasDO canvas = new CanvasDO();
        canvas.setId(20L);
        canvas.setStatus(CanvasStatusEnum.KILLED.getCode());
        when(canvasMapper.selectById(20L)).thenReturn(canvas);
        CanvasOpsService service = new CanvasOpsService(
                canvasMapper,
                versionMapper,
                null,
                null,
                null,
                null,
                null,
                new CanvasStateTransitionPolicy(),
                null,
                null);

        assertThatThrownBy(() -> service.saveWithOptimisticLock(
                20L, "name", "desc", "{\"nodes\":[]}", 1, "operator"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("KILLED");

        verify(canvasMapper, never()).updateEditVersion(20L, 1, 2, "name", "desc");
    }
}
