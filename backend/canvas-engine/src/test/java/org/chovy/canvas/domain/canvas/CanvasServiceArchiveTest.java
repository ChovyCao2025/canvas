package org.chovy.canvas.domain.canvas;

import org.chovy.canvas.common.enums.CanvasStatusEnum;
import org.chovy.canvas.engine.dag.DagGraph;
import org.chovy.canvas.engine.dag.DagParser;
import org.chovy.canvas.engine.trigger.CanvasExecutionService;
import org.chovy.canvas.engine.trigger.CanvasSchedulerService;
import org.chovy.canvas.infrastructure.cache.CanvasConfigCache;
import org.chovy.canvas.infrastructure.redis.TriggerRouteService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.chovy.canvas.dal.dataobject.CanvasDO;
import org.chovy.canvas.dal.mapper.CanvasMapper;
import org.chovy.canvas.dal.dataobject.CanvasVersionDO;
import org.chovy.canvas.dal.mapper.CanvasVersionMapper;

/**
 * Canvas Service Archive 测试类。
 *
 * <p>覆盖该后端组件在典型输入、边界条件和异常场景下的行为，确保重构或性能优化不会改变既有契约。
 * <p>测试代码只构造必要的依赖与数据，断言重点放在可观察结果、状态变更和关键副作用上。
 */
@ExtendWith(MockitoExtension.class)
class CanvasServiceArchiveTest {

    @Mock
    private CanvasMapper canvasMapper;
    @Mock
    private CanvasVersionMapper canvasVersionMapper;
    @Mock
    private CanvasTransactionService canvasTransactionService;
    @Mock
    private DagParser dagParser;
    @Mock
    private TriggerRouteService triggerRouteService;
    @Mock
    private CanvasSchedulerService schedulerService;
    @Mock
    private CanvasConfigCache configCache;
    @Mock
    private CanvasExecutionService canvasExecutionService;
    @Mock
    private org.chovy.canvas.engine.handlers.GroovyHandler groovyHandler;
    @Mock
    private org.chovy.canvas.engine.handlers.MqTriggerHandler mqTriggerHandler;
    @Mock
    private org.springframework.data.redis.core.StringRedisTemplate redis;
    @Mock
    private CanvasExamplesProperties examplesProperties;

    @InjectMocks
    private CanvasService canvasService;

    private CanvasDO existingCanvas;

    @BeforeEach
    void setUp() {
        existingCanvas = new CanvasDO();
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

    @Test
    @DisplayName("归档已发布画布：调用 archiveDb 并清理 Redis 路由、Scheduler、缓存")
    void archive_published_canvas_cleans_up_external_state() {
        Long publishedVersionId = 42L;
        existingCanvas.setStatus(CanvasStatusEnum.PUBLISHED.getCode());
        existingCanvas.setPublishedVersionId(publishedVersionId);
        when(canvasMapper.selectById(1L)).thenReturn(existingCanvas);

        CanvasVersionDO publishedVersion = new CanvasVersionDO();
        publishedVersion.setId(publishedVersionId);
        publishedVersion.setCanvasId(1L);
        publishedVersion.setGraphJson("{\"nodes\":[]}");
        when(canvasVersionMapper.selectById(publishedVersionId)).thenReturn(publishedVersion);

        DagGraph graph = mock(DagGraph.class);
        when(graph.allNodeIds()).thenReturn(Collections.emptySet());
        when(dagParser.parse("{\"nodes\":[]}")).thenReturn(graph);

        canvasService.archive(1L, "user_001");

        verify(canvasTransactionService).archiveDb(1L);
        verify(schedulerService).cancelScheduledTriggers(1L, graph);
        verify(configCache).invalidate(1L, publishedVersionId);
        verify(canvasExecutionService).invalidateCanvas(1L);
    }
}
