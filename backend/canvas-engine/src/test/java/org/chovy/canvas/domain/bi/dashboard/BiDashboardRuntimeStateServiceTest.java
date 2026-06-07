package org.chovy.canvas.domain.bi.dashboard;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.dal.dataobject.BiDashboardRuntimeStateDO;
import org.chovy.canvas.dal.dataobject.BiWorkspaceDO;
import org.chovy.canvas.dal.mapper.BiDashboardRuntimeStateMapper;
import org.chovy.canvas.dal.mapper.BiWorkspaceMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BiDashboardRuntimeStateServiceTest {

    @Test
    void savesDashboardRuntimeParametersForCurrentTenantAndUser() {
        BiWorkspaceMapper workspaceMapper = mock(BiWorkspaceMapper.class);
        BiDashboardRuntimeStateMapper stateMapper = mock(BiDashboardRuntimeStateMapper.class);
        when(workspaceMapper.selectOne(any())).thenReturn(workspace());
        BiDashboardRuntimeStateService service =
                new BiDashboardRuntimeStateService(workspaceMapper, stateMapper, new ObjectMapper());
        BiDashboardRuntimeStateCommand command = new BiDashboardRuntimeStateCommand(Map.of(
                "filter-stat-date", List.of("2026-06-01", "2026-06-06"),
                "filter-trigger-type", List.of("TIME", "MQ")));

        BiDashboardRuntimeStateView view = service.save(7L, "alice", "canvas-effect", command);

        ArgumentCaptor<BiDashboardRuntimeStateDO> captor =
                ArgumentCaptor.forClass(BiDashboardRuntimeStateDO.class);
        verify(stateMapper).upsert(captor.capture());
        assertThat(captor.getValue().getTenantId()).isEqualTo(7L);
        assertThat(captor.getValue().getWorkspaceId()).isEqualTo(99L);
        assertThat(captor.getValue().getDashboardKey()).isEqualTo("canvas-effect");
        assertThat(captor.getValue().getUsername()).isEqualTo("alice");
        assertThat(captor.getValue().getParameterJson()).contains("filter-stat-date");
        assertThat(captor.getValue().getUpdatedAt()).isNotNull();
        assertThat(view.dashboardKey()).isEqualTo("canvas-effect");
        assertThat(view.parameters()).containsEntry("filter-trigger-type", List.of("TIME", "MQ"));
    }

    @Test
    void readsRememberedDashboardRuntimeParametersForCurrentTenantAndUser() {
        BiWorkspaceMapper workspaceMapper = mock(BiWorkspaceMapper.class);
        BiDashboardRuntimeStateMapper stateMapper = mock(BiDashboardRuntimeStateMapper.class);
        when(workspaceMapper.selectOne(any())).thenReturn(workspace());
        BiDashboardRuntimeStateDO row = new BiDashboardRuntimeStateDO();
        row.setTenantId(7L);
        row.setWorkspaceId(99L);
        row.setDashboardKey("canvas-effect");
        row.setUsername("alice");
        row.setParameterJson("""
                {"filter-stat-date":["2026-06-01","2026-06-06"],"filter-canvas":"Welcome"}
                """);
        row.setUpdatedAt(LocalDateTime.parse("2026-06-06T03:20:00"));
        when(stateMapper.selectOne(any())).thenReturn(row);
        BiDashboardRuntimeStateService service =
                new BiDashboardRuntimeStateService(workspaceMapper, stateMapper, new ObjectMapper());

        BiDashboardRuntimeStateView view = service.get(7L, "alice", "canvas-effect");

        assertThat(view.dashboardKey()).isEqualTo("canvas-effect");
        assertThat(view.username()).isEqualTo("alice");
        assertThat(view.parameters()).containsEntry("filter-canvas", "Welcome");
        assertThat(view.parameters()).containsEntry("filter-stat-date", List.of("2026-06-01", "2026-06-06"));
        assertThat(view.updatedAt()).isEqualTo(LocalDateTime.parse("2026-06-06T03:20:00"));
    }

    private BiWorkspaceDO workspace() {
        BiWorkspaceDO workspace = new BiWorkspaceDO();
        workspace.setId(99L);
        workspace.setTenantId(7L);
        workspace.setWorkspaceKey("marketing_canvas");
        workspace.setName("Marketing Canvas");
        workspace.setStatus("ACTIVE");
        return workspace;
    }
}
