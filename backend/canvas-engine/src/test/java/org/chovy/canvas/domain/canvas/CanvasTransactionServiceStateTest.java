package org.chovy.canvas.domain.canvas;

import org.chovy.canvas.common.enums.CanvasStatusEnum;
import org.chovy.canvas.dal.dataobject.CanvasDO;
import org.chovy.canvas.dal.dataobject.CanvasVersionDO;
import org.chovy.canvas.dal.mapper.CanvasMapper;
import org.chovy.canvas.dal.mapper.CanvasVersionMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CanvasTransactionServiceStateTest {

    @Test
    void publishDbRejectsKilledCanvas() {
        CanvasMapper canvasMapper = mock(CanvasMapper.class);
        CanvasVersionMapper versionMapper = mock(CanvasVersionMapper.class);
        CanvasDO canvas = canvas(7L, CanvasStatusEnum.KILLED);
        when(canvasMapper.selectById(7L)).thenReturn(canvas);
        CanvasTransactionService service = new CanvasTransactionService(canvasMapper, versionMapper);

        assertThatThrownBy(() -> service.publishDb(7L, "{\"nodes\":[]}", "operator"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("KILLED");

        verify(versionMapper, never()).insert(any(CanvasVersionDO.class));
        verify(canvasMapper, never()).updateById(any(CanvasDO.class));
    }

    @Test
    void archiveDbRejectsKilledCanvasBecauseKilledIsTerminal() {
        CanvasMapper canvasMapper = mock(CanvasMapper.class);
        CanvasVersionMapper versionMapper = mock(CanvasVersionMapper.class);
        CanvasDO canvas = canvas(8L, CanvasStatusEnum.KILLED);
        when(canvasMapper.selectById(8L)).thenReturn(canvas);
        CanvasTransactionService service = new CanvasTransactionService(canvasMapper, versionMapper);

        assertThatThrownBy(() -> service.archiveDb(8L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("KILLED");

        verify(canvasMapper, never()).updateById(any(CanvasDO.class));
    }

    @Test
    void offlineDbRejectsArchivedCanvas() {
        CanvasMapper canvasMapper = mock(CanvasMapper.class);
        CanvasVersionMapper versionMapper = mock(CanvasVersionMapper.class);
        CanvasDO canvas = canvas(9L, CanvasStatusEnum.ARCHIVED);
        when(canvasMapper.selectById(9L)).thenReturn(canvas);
        CanvasTransactionService service = new CanvasTransactionService(canvasMapper, versionMapper);

        assertThatThrownBy(() -> service.offlineDb(9L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ARCHIVED");

        verify(canvasMapper, never()).updateById(any(CanvasDO.class));
    }

    private static CanvasDO canvas(Long id, CanvasStatusEnum status) {
        CanvasDO canvas = new CanvasDO();
        canvas.setId(id);
        canvas.setStatus(status.getCode());
        return canvas;
    }
}
