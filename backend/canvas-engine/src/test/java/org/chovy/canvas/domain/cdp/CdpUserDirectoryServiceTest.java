package org.chovy.canvas.domain.cdp;

import org.chovy.canvas.dal.dataobject.CanvasExecutionDO;
import org.chovy.canvas.dal.mapper.CanvasExecutionMapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import org.chovy.canvas.dal.dataobject.CdpUserProfileDO;
import org.chovy.canvas.dal.mapper.CdpUserProfileMapper;

/**
 * CDP 用户 Directory 测试类。
 *
 * <p>覆盖该后端组件在典型输入、边界条件和异常场景下的行为，确保重构或性能优化不会改变既有契约。
 * <p>测试代码只构造必要的依赖与数据，断言重点放在可观察结果、状态变更和关键副作用上。
 */
class CdpUserDirectoryServiceTest {

    @Test
    void listUsersReturnsProfilesWithExecutionStatsAndTags() {
        CdpUserProfileMapper profileMapper = Mockito.mock(CdpUserProfileMapper.class);
        CanvasExecutionMapper executionMapper = Mockito.mock(CanvasExecutionMapper.class);
        CdpTagService tagService = Mockito.mock(CdpTagService.class);
        CdpUserDirectoryService service = new CdpUserDirectoryService(profileMapper, executionMapper, tagService);

        CdpUserProfileDO profile = new CdpUserProfileDO();
        profile.setUserId("u1");
        profile.setDisplayName("Alice");
        profile.setFirstSeenAt(LocalDateTime.parse("2026-05-23T10:00:00"));
        profile.setLastSeenAt(LocalDateTime.parse("2026-05-23T11:00:00"));
        when(profileMapper.selectList(any())).thenReturn(List.of(profile));

        CanvasExecutionDO success = new CanvasExecutionDO();
        success.setUserId("u1");
        success.setStatus(2);
        success.setCreatedAt(LocalDateTime.parse("2026-05-23T10:30:00"));
        CanvasExecutionDO failed = new CanvasExecutionDO();
        failed.setUserId("u1");
        failed.setStatus(3);
        failed.setCreatedAt(LocalDateTime.parse("2026-05-23T11:00:00"));
        when(executionMapper.selectList(any())).thenReturn(List.of(success, failed));
        when(tagService.listCurrentTags("u1")).thenReturn(List.of());

        var rows = service.listUsers(null);

        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).userId()).isEqualTo("u1");
        assertThat(rows.get(0).displayName()).isEqualTo("Alice");
        assertThat(rows.get(0).executionCount()).isEqualTo(2);
        assertThat(rows.get(0).successCount()).isEqualTo(1);
        assertThat(rows.get(0).failedCount()).isEqualTo(1);
        assertThat(rows.get(0).latestStatus()).isEqualTo("FAILED");
    }
}
