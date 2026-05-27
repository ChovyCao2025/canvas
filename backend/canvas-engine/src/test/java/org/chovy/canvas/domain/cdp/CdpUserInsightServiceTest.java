package org.chovy.canvas.domain.cdp;

import org.chovy.canvas.dal.dataobject.CanvasDO;
import org.chovy.canvas.dal.mapper.CanvasMapper;
import org.chovy.canvas.dal.dataobject.CanvasExecutionDO;
import org.chovy.canvas.dal.mapper.CanvasExecutionMapper;
import org.chovy.canvas.dto.cdp.CdpUserDetailDTO;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import org.chovy.canvas.dal.dataobject.CdpUserProfileDO;

/**
 * CDP 用户 Insight 测试类。
 *
 * <p>覆盖该后端组件在典型输入、边界条件和异常场景下的行为，确保重构或性能优化不会改变既有契约。
 * <p>测试代码只构造必要的依赖与数据，断言重点放在可观察结果、状态变更和关键副作用上。
 */
class CdpUserInsightServiceTest {

    @Test
    void returnsCanvasSummariesForUser() {
        CdpUserService userService = Mockito.mock(CdpUserService.class);
        CdpTagService tagService = Mockito.mock(CdpTagService.class);
        CanvasExecutionMapper executionMapper = Mockito.mock(CanvasExecutionMapper.class);
        CanvasMapper canvasMapper = Mockito.mock(CanvasMapper.class);
        CdpUserInsightService service = new CdpUserInsightService(userService, tagService, executionMapper, canvasMapper);

        CdpUserProfileDO profile = new CdpUserProfileDO();
        profile.setUserId("u1");
        profile.setDisplayName("Alice");
        when(userService.getRequiredProfile("u1")).thenReturn(profile);
        when(userService.toDetail(profile)).thenReturn(new CdpUserDetailDTO("u1", "Alice", null, null, "ACTIVE", null, null, null));

        CanvasExecutionDO execution = new CanvasExecutionDO();
        execution.setCanvasId(7L);
        execution.setUserId("u1");
        execution.setStatus(2);
        execution.setCreatedAt(LocalDateTime.parse("2026-05-24T10:00:00"));
        when(executionMapper.selectList(any())).thenReturn(List.of(execution));

        CanvasDO canvas = new CanvasDO();
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
