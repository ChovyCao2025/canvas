package org.chovy.canvas.domain.cdp;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import org.chovy.canvas.dal.dataobject.CanvasExecutionDO;
import org.chovy.canvas.dal.mapper.CanvasExecutionMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 画布用户 Query 测试类。
 *
 * <p>覆盖该后端组件在典型输入、边界条件和异常场景下的行为，确保重构或性能优化不会改变既有契约。
 * <p>测试代码只构造必要的依赖与数据，断言重点放在可观察结果、状态变更和关键副作用上。
 */
class CanvasUserQueryServiceTest {

    @Test
    void listUsersAggregatesExecutionsByUser() {
        CanvasExecutionMapper executionMapper = Mockito.mock(CanvasExecutionMapper.class);
        CdpTagService tagService = Mockito.mock(CdpTagService.class);
        CdpUserService userService = Mockito.mock(CdpUserService.class);
        CanvasUserQueryService service = new CanvasUserQueryService(executionMapper, tagService, userService);

        CanvasExecutionDO success = exec("e1", "u1", 2, LocalDateTime.parse("2026-05-23T10:00:00"));
        CanvasExecutionDO failed = exec("e2", "u1", 3, LocalDateTime.parse("2026-05-23T11:00:00"));
        when(executionMapper.selectList(any(Wrapper.class))).thenReturn(List.of(success, failed));
        when(tagService.listCurrentTags("u1")).thenReturn(List.of());

        var rows = service.listUsers(7L);

        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).userId()).isEqualTo("u1");
        assertThat(rows.get(0).executionCount()).isEqualTo(2);
        assertThat(rows.get(0).successCount()).isEqualTo(1);
        assertThat(rows.get(0).failedCount()).isEqualTo(1);
        assertThat(rows.get(0).latestStatus()).isEqualTo("FAILED");
    }

    @Test
    void getUserInCanvasFailsWhenUserHasNoExecutionsInCanvas() {
        CanvasExecutionMapper executionMapper = Mockito.mock(CanvasExecutionMapper.class);
        CdpTagService tagService = Mockito.mock(CdpTagService.class);
        CdpUserService userService = Mockito.mock(CdpUserService.class);
        CanvasUserQueryService service = new CanvasUserQueryService(executionMapper, tagService, userService);

        when(executionMapper.selectList(any(Wrapper.class))).thenReturn(List.of());

        assertThatThrownBy(() -> service.getUserInCanvas(7L, "u404"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("u404");
    }

    @Test
    void listExecutionsDelegatesToExecutionMapper() {
        CanvasExecutionMapper executionMapper = Mockito.mock(CanvasExecutionMapper.class);
        CdpTagService tagService = Mockito.mock(CdpTagService.class);
        CdpUserService userService = Mockito.mock(CdpUserService.class);
        CanvasUserQueryService service = new CanvasUserQueryService(executionMapper, tagService, userService);
        when(executionMapper.selectList(any(Wrapper.class))).thenReturn(List.of());

        service.listExecutions(7L, "u1");

        ArgumentCaptor<Wrapper<CanvasExecutionDO>> captor = wrapperCaptor();
        verify(executionMapper).selectList(captor.capture());
        assertThat(captor.getValue()).isNotNull();
    }

    private CanvasExecutionDO exec(String id, String userId, int status, LocalDateTime createdAt) {
        CanvasExecutionDO exec = new CanvasExecutionDO();
        exec.setId(id);
        exec.setCanvasId(7L);
        exec.setUserId(userId);
        exec.setStatus(status);
        exec.setCreatedAt(createdAt);
        return exec;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static <T> ArgumentCaptor<Wrapper<T>> wrapperCaptor() {
        return (ArgumentCaptor) ArgumentCaptor.forClass(Wrapper.class);
    }

}
