package org.chovy.canvas.web;

import org.chovy.canvas.common.R;
import org.chovy.canvas.common.enums.CanvasStatusEnum;
import org.chovy.canvas.common.enums.ExecutionStatus;
import org.chovy.canvas.dal.dataobject.CanvasDO;
import org.chovy.canvas.dal.dataobject.CanvasExecutionDO;
import org.chovy.canvas.dal.mapper.CanvasExecutionMapper;
import org.chovy.canvas.dal.mapper.CanvasMapper;
import org.chovy.canvas.infrastructure.doris.DailyStatsDTO;
import org.chovy.canvas.infrastructure.doris.DorisQueryService;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HomeOverviewControllerTest {

    @Test
    void overviewFallsBackToMysqlAndFiltersUnpublishedCanvases() {
        CanvasMapper canvasMapper = mock(CanvasMapper.class);
        CanvasExecutionMapper executionMapper = mock(CanvasExecutionMapper.class);
        HomeOverviewController controller = new HomeOverviewController(canvasMapper, executionMapper);
        LocalDateTime now = LocalDateTime.now();
        when(canvasMapper.selectList(any())).thenReturn(List.of(
                canvas(10L, "Published canvas", CanvasStatusEnum.PUBLISHED.getCode()),
                canvas(20L, "Draft canvas", CanvasStatusEnum.DRAFT.getCode())
        ));
        when(executionMapper.selectList(any())).thenReturn(List.of(
                execution("exec-1", 10L, "u-1", ExecutionStatus.SUCCESS, now.minusDays(1)),
                execution("exec-2", 10L, "u-2", ExecutionStatus.FAILED, now),
                execution("exec-3", 20L, "u-3", ExecutionStatus.SUCCESS, now)
        ));

        R<HomeOverviewController.HomeOverviewDTO> response = controller.overview(7).block();

        assertThat(response).isNotNull();
        assertThat(response.getCode()).isZero();
        HomeOverviewController.HomeOverviewDTO data = response.getData();
        assertThat(data.summary().publishedCanvasCount()).isEqualTo(1);
        assertThat(data.summary().totalExecutions()).isEqualTo(2);
        assertThat(data.summary().failedExecutions()).isEqualTo(1);
        assertThat(data.summary().uniqueUsers()).isEqualTo(2);
        assertThat(data.summary().successRate()).isEqualTo("50.0%");
        assertThat(data.topCanvases()).hasSize(1);
        assertThat(data.topCanvases().getFirst().canvasId()).isEqualTo(10L);
        assertThat(data.topCanvases().getFirst().failed()).isEqualTo(1);
        assertThat(data.attentionItems())
                .extracting(HomeOverviewController.AttentionItemDTO::type)
                .contains("HAS_FAILURES");
    }

    @Test
    void overviewPrefersDorisStatsAndFiltersUnpublishedCanvases() {
        CanvasMapper canvasMapper = mock(CanvasMapper.class);
        CanvasExecutionMapper executionMapper = mock(CanvasExecutionMapper.class);
        DorisQueryService dorisQueryService = mock(DorisQueryService.class);
        HomeOverviewController controller = new HomeOverviewController(canvasMapper, executionMapper, dorisQueryService);
        LocalDate today = LocalDate.now();
        when(canvasMapper.selectList(any())).thenReturn(List.of(
                canvas(10L, "Published canvas", CanvasStatusEnum.PUBLISHED.getCode()),
                canvas(20L, "Draft canvas", CanvasStatusEnum.DRAFT.getCode())
        ));
        when(dorisQueryService.available()).thenReturn(true);
        when(dorisQueryService.getDailyStats(any(LocalDate.class), any(LocalDate.class))).thenReturn(List.of(
                new DailyStatsDTO(today, 10L, "Published canvas", "DIRECT_CALL", 5L, 4L, 1L, 0L, 3L, 100L),
                new DailyStatsDTO(today, 20L, "Draft canvas", "DIRECT_CALL", 7L, 7L, 0L, 0L, 7L, 100L)
        ));

        R<HomeOverviewController.HomeOverviewDTO> response = controller.overview(7).block();

        assertThat(response).isNotNull();
        assertThat(response.getCode()).isZero();
        HomeOverviewController.HomeOverviewDTO data = response.getData();
        assertThat(data.summary().publishedCanvasCount()).isEqualTo(1);
        assertThat(data.summary().totalExecutions()).isEqualTo(5);
        assertThat(data.summary().failedExecutions()).isEqualTo(1);
        assertThat(data.summary().uniqueUsers()).isEqualTo(3);
        assertThat(data.summary().successRate()).isEqualTo("80.0%");
        assertThat(data.topCanvases()).hasSize(1);
        assertThat(data.topCanvases().getFirst().canvasId()).isEqualTo(10L);
        verify(executionMapper, never()).selectList(any());
    }

    private CanvasDO canvas(Long id, String name, Integer status) {
        CanvasDO canvas = new CanvasDO();
        canvas.setId(id);
        canvas.setName(name);
        canvas.setStatus(status);
        return canvas;
    }

    private CanvasExecutionDO execution(String id,
                                        Long canvasId,
                                        String userId,
                                        ExecutionStatus status,
                                        LocalDateTime createdAt) {
        CanvasExecutionDO execution = new CanvasExecutionDO();
        execution.setId(id);
        execution.setCanvasId(canvasId);
        execution.setUserId(userId);
        execution.setStatus(status.getCode());
        execution.setCreatedAt(createdAt);
        return execution;
    }
}
