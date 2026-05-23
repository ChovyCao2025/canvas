package org.chovy.canvas.domain.cdp;

import org.chovy.canvas.domain.canvas.Canvas;
import org.chovy.canvas.domain.canvas.CanvasMapper;
import org.chovy.canvas.domain.execution.CanvasExecution;
import org.chovy.canvas.domain.execution.CanvasExecutionMapper;
import org.chovy.canvas.dto.cdp.CdpUserDetailDTO;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class CdpUserInsightServiceTest {

    @Test
    void returnsCanvasSummariesForUser() {
        CdpUserService userService = Mockito.mock(CdpUserService.class);
        CdpTagService tagService = Mockito.mock(CdpTagService.class);
        CanvasExecutionMapper executionMapper = Mockito.mock(CanvasExecutionMapper.class);
        CanvasMapper canvasMapper = Mockito.mock(CanvasMapper.class);
        CdpUserInsightService service = new CdpUserInsightService(userService, tagService, executionMapper, canvasMapper);

        CdpUserProfile profile = new CdpUserProfile();
        profile.setUserId("u1");
        profile.setDisplayName("Alice");
        when(userService.getRequiredProfile("u1")).thenReturn(profile);
        when(userService.toDetail(profile)).thenReturn(new CdpUserDetailDTO("u1", "Alice", null, null, "ACTIVE", null, null, null));

        CanvasExecution execution = new CanvasExecution();
        execution.setCanvasId(7L);
        execution.setUserId("u1");
        execution.setStatus(2);
        execution.setCreatedAt(LocalDateTime.parse("2026-05-24T10:00:00"));
        when(executionMapper.selectList(any())).thenReturn(List.of(execution));

        Canvas canvas = new Canvas();
        canvas.setId(7L);
        canvas.setName("召回流程");
        when(canvasMapper.selectList(any())).thenReturn(List.of(canvas));
        when(tagService.listCurrentTags("u1")).thenReturn(List.of());

        var detail = service.getUserInsight("u1");

        assertThat(detail.userId()).isEqualTo("u1");
        assertThat(detail.canvasRows()).hasSize(1);
        assertThat(detail.canvasRows().get(0).canvasId()).isEqualTo(7L);
        assertThat(detail.canvasRows().get(0).canvasName()).isEqualTo("召回流程");
    }
}
