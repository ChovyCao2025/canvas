package org.chovy.canvas.domain.project;

import org.chovy.canvas.dal.dataobject.CanvasProjectDO;
import org.chovy.canvas.dal.dataobject.CanvasProjectFolderDO;
import org.chovy.canvas.dal.dataobject.CanvasExecutionStatsDO;
import org.chovy.canvas.dal.mapper.CanvasMapper;
import org.chovy.canvas.dal.mapper.CanvasExecutionStatsMapper;
import org.chovy.canvas.dal.mapper.CanvasProjectFolderMapper;
import org.chovy.canvas.dal.mapper.CanvasProjectMapper;
import org.chovy.canvas.dal.mapper.CanvasProjectMemberMapper;
import org.chovy.canvas.dto.project.ProjectCreateReq;
import org.chovy.canvas.dto.project.ProjectMemberUpdateReq;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CanvasProjectServiceTest {

    @Test
    void createNormalizesProjectKeyAndDefaultsStatus() {
        CanvasProjectMapper projectMapper = mock(CanvasProjectMapper.class);
        CanvasProjectService service = service(projectMapper);

        ProjectCreateReq req = new ProjectCreateReq(
                " Growth Team ", " Growth Team ", "member growth", null,
                0, null, "alice");
        var resp = service.create(9L, req);

        ArgumentCaptor<CanvasProjectDO> captor = ArgumentCaptor.forClass(CanvasProjectDO.class);
        verify(projectMapper).insert(captor.capture());
        assertThat(captor.getValue().getTenantId()).isEqualTo(9L);
        assertThat(captor.getValue().getProjectKey()).isEqualTo("growth-team");
        assertThat(captor.getValue().getStatus()).isEqualTo("ACTIVE");
        assertThat(resp.projectKey()).isEqualTo("growth-team");
    }

    @Test
    void setMemberRejectsUnknownRole() {
        CanvasProjectService service = service(mock(CanvasProjectMapper.class));

        assertThatThrownBy(() -> service.setMember(9L, 11L, 5L,
                new ProjectMemberUpdateReq("alice", "OWNER")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported project role");
    }

    @Test
    void statsAggregatesPublishedCanvasesAndSevenDayExecutionStatsForAssignedCanvases() {
        CanvasProjectMapper projectMapper = mock(CanvasProjectMapper.class);
        CanvasProjectFolderMapper folderMapper = mock(CanvasProjectFolderMapper.class);
        CanvasMapper canvasMapper = mock(CanvasMapper.class);
        CanvasExecutionStatsMapper statsMapper = mock(CanvasExecutionStatsMapper.class);
        CanvasProjectService service = new CanvasProjectService(
                projectMapper,
                mock(CanvasProjectMemberMapper.class),
                folderMapper,
                canvasMapper,
                statsMapper);
        CanvasProjectDO project = new CanvasProjectDO();
        project.setId(11L);
        project.setTenantId(9L);
        project.setProjectKey("growth");
        when(projectMapper.selectOne(any())).thenReturn(project);
        when(folderMapper.selectCount(any())).thenReturn(2L);
        CanvasProjectFolderDO first = assignment(101L);
        CanvasProjectFolderDO second = assignment(102L);
        when(folderMapper.selectList(any())).thenReturn(List.of(first, second));
        when(canvasMapper.selectCount(any())).thenReturn(1L);
        CanvasExecutionStatsDO dayOne = statsRow(10, 2, 100L);
        CanvasExecutionStatsDO dayTwo = statsRow(5, 1, 200L);
        when(statsMapper.selectList(any())).thenReturn(List.of(dayOne, dayTwo));

        var stats = service.stats(9L, 11L);

        assertThat(stats.canvasCount()).isEqualTo(2L);
        assertThat(stats.publishedCanvasCount()).isEqualTo(1L);
        assertThat(stats.executionCount7d()).isEqualTo(15L);
        assertThat(stats.failedExecutionCount7d()).isEqualTo(3L);
        assertThat(stats.avgDurationMs7d()).isEqualTo(133L);
    }

    private CanvasProjectService service(CanvasProjectMapper projectMapper) {
        return new CanvasProjectService(
                projectMapper,
                mock(CanvasProjectMemberMapper.class),
                mock(CanvasProjectFolderMapper.class),
                mock(CanvasMapper.class),
                mock(CanvasExecutionStatsMapper.class));
    }

    private CanvasProjectFolderDO assignment(Long canvasId) {
        CanvasProjectFolderDO row = new CanvasProjectFolderDO();
        row.setTenantId(9L);
        row.setProjectId(11L);
        row.setCanvasId(canvasId);
        return row;
    }

    private CanvasExecutionStatsDO statsRow(Integer totalCount, Integer failCount, Long avgDurationMs) {
        CanvasExecutionStatsDO row = new CanvasExecutionStatsDO();
        row.setTotalCount(totalCount);
        row.setFailCount(failCount);
        row.setAvgDurationMs(avgDurationMs);
        return row;
    }
}
