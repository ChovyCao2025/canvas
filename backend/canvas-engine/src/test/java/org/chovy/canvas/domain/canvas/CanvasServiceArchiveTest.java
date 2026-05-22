package org.chovy.canvas.domain.canvas;

import org.chovy.canvas.domain.constant.CanvasStatusEnum;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CanvasServiceArchiveTest {

    @Mock
    private CanvasMapper canvasMapper;
    @Mock
    private CanvasTransactionService canvasTransactionService;

    @InjectMocks
    private CanvasService canvasService;

    private Canvas existingCanvas;

    @BeforeEach
    void setUp() {
        existingCanvas = new Canvas();
        existingCanvas.setId(1L);
        existingCanvas.setStatus(CanvasStatusEnum.DRAFT.getCode());
        existingCanvas.setName("测试画布");
    }

    @Test
    @DisplayName("归档草稿状态画布：调用 archiveDb 并成功")
    void archive_draft_canvas_succeeds() {
        when(canvasMapper.selectById(1L)).thenReturn(existingCanvas);

        canvasService.archive(1L, "user_001");

        verify(canvasTransactionService).archiveDb(1L);
    }

    @Test
    @DisplayName("画布不存在时抛出 IllegalArgumentException")
    void archive_nonexistent_canvas_throws() {
        when(canvasMapper.selectById(99L)).thenReturn(null);

        assertThatThrownBy(() -> canvasService.archive(99L, "user_001"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("99");
    }

    @Test
    @DisplayName("画布已是 ARCHIVED 状态时抛出 IllegalStateException")
    void archive_already_archived_canvas_throws() {
        existingCanvas.setStatus(CanvasStatusEnum.ARCHIVED.getCode());
        when(canvasMapper.selectById(1L)).thenReturn(existingCanvas);

        assertThatThrownBy(() -> canvasService.archive(1L, "user_001"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("已归档");
    }
}
