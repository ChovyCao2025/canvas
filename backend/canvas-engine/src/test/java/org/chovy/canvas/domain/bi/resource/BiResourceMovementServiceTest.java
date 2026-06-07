package org.chovy.canvas.domain.bi.resource;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.chovy.canvas.dal.dataobject.BiBigScreenDO;
import org.chovy.canvas.dal.dataobject.BiDashboardDO;
import org.chovy.canvas.dal.dataobject.BiResourceLocationDO;
import org.chovy.canvas.dal.dataobject.BiWorkspaceDO;
import org.chovy.canvas.dal.mapper.BiBigScreenMapper;
import org.chovy.canvas.dal.mapper.BiChartMapper;
import org.chovy.canvas.dal.mapper.BiDashboardMapper;
import org.chovy.canvas.dal.mapper.BiDatasetMapper;
import org.chovy.canvas.dal.mapper.BiPortalMapper;
import org.chovy.canvas.dal.mapper.BiResourceLocationMapper;
import org.chovy.canvas.dal.mapper.BiSpreadsheetMapper;
import org.chovy.canvas.dal.mapper.BiWorkspaceMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BiResourceMovementServiceTest {

    @Test
    void movementMigrationMatchesMapperTimestampColumn() throws Exception {
        String migration = Files.readString(Path.of(
                "src/main/resources/db/migration/V243__bi_resource_location.sql"));

        assertThat(migration).contains("moved_at DATETIME");
    }

    @Test
    void moveDashboardPersistsTenantScopedFolderLocation() {
        BiWorkspaceMapper workspaceMapper = mock(BiWorkspaceMapper.class);
        BiDatasetMapper datasetMapper = mock(BiDatasetMapper.class);
        BiDashboardMapper dashboardMapper = mock(BiDashboardMapper.class);
        BiChartMapper chartMapper = mock(BiChartMapper.class);
        BiPortalMapper portalMapper = mock(BiPortalMapper.class);
        BiResourceLocationMapper locationMapper = mock(BiResourceLocationMapper.class);
        BiWorkspaceDO workspace = new BiWorkspaceDO();
        workspace.setId(5L);
        BiDashboardDO dashboard = new BiDashboardDO();
        dashboard.setId(99L);
        dashboard.setStatus("PUBLISHED");
        when(workspaceMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(workspace);
        when(dashboardMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(dashboard);
        BiResourceMovementService service = new BiResourceMovementService(
                workspaceMapper,
                datasetMapper,
                dashboardMapper,
                chartMapper,
                portalMapper,
                mock(BiBigScreenMapper.class),
                mock(BiSpreadsheetMapper.class),
                locationMapper);

        BiResourceLocationView moved = service.move(7L, "alice",
                new BiResourceMoveCommand("DASHBOARD", "canvas-effect", "operations/q2", 30));

        ArgumentCaptor<BiResourceLocationDO> locationCaptor = ArgumentCaptor.forClass(BiResourceLocationDO.class);
        verify(locationMapper).upsert(locationCaptor.capture());
        ArgumentCaptor<LambdaQueryWrapper<BiWorkspaceDO>> workspaceQueryCaptor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(workspaceMapper).selectOne(workspaceQueryCaptor.capture());
        assertThat(workspaceQueryCaptor.getValue().getParamNameValuePairs())
                .containsValue("marketing_canvas");
        assertThat(locationCaptor.getValue().getTenantId()).isEqualTo(7L);
        assertThat(locationCaptor.getValue().getWorkspaceId()).isEqualTo(5L);
        assertThat(locationCaptor.getValue().getResourceType()).isEqualTo("DASHBOARD");
        assertThat(locationCaptor.getValue().getResourceKey()).isEqualTo("canvas-effect");
        assertThat(locationCaptor.getValue().getFolderKey()).isEqualTo("operations/q2");
        assertThat(locationCaptor.getValue().getSortOrder()).isEqualTo(30);
        assertThat(locationCaptor.getValue().getMovedBy()).isEqualTo("alice");
        assertThat(moved.resourceType()).isEqualTo("DASHBOARD");
        assertThat(moved.resourceKey()).isEqualTo("canvas-effect");
        assertThat(moved.folderKey()).isEqualTo("operations/q2");
        assertThat(moved.sortOrder()).isEqualTo(30);
        assertThat(moved.movedBy()).isEqualTo("alice");
    }

    @Test
    void moveBigScreenPersistsTenantScopedFolderLocation() {
        BiWorkspaceMapper workspaceMapper = mock(BiWorkspaceMapper.class);
        BiBigScreenMapper bigScreenMapper = mock(BiBigScreenMapper.class);
        BiResourceLocationMapper locationMapper = mock(BiResourceLocationMapper.class);
        BiWorkspaceDO workspace = new BiWorkspaceDO();
        workspace.setId(5L);
        BiBigScreenDO screen = new BiBigScreenDO();
        screen.setId(99L);
        screen.setStatus("PUBLISHED");
        when(workspaceMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(workspace);
        when(bigScreenMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(screen);
        BiResourceMovementService service = new BiResourceMovementService(
                workspaceMapper,
                mock(BiDatasetMapper.class),
                mock(BiDashboardMapper.class),
                mock(BiChartMapper.class),
                mock(BiPortalMapper.class),
                bigScreenMapper,
                mock(BiSpreadsheetMapper.class),
                locationMapper);

        BiResourceLocationView moved = service.move(7L, "alice",
                new BiResourceMoveCommand("big_screen", "executive-wall", "operations/screens", 20));

        ArgumentCaptor<BiResourceLocationDO> locationCaptor = ArgumentCaptor.forClass(BiResourceLocationDO.class);
        verify(locationMapper).upsert(locationCaptor.capture());
        assertThat(locationCaptor.getValue().getTenantId()).isEqualTo(7L);
        assertThat(locationCaptor.getValue().getWorkspaceId()).isEqualTo(5L);
        assertThat(locationCaptor.getValue().getResourceType()).isEqualTo("BIG_SCREEN");
        assertThat(locationCaptor.getValue().getResourceKey()).isEqualTo("executive-wall");
        assertThat(locationCaptor.getValue().getFolderKey()).isEqualTo("operations/screens");
        assertThat(locationCaptor.getValue().getSortOrder()).isEqualTo(20);
        assertThat(moved.resourceType()).isEqualTo("BIG_SCREEN");
        assertThat(moved.resourceKey()).isEqualTo("executive-wall");
    }

    @Test
    void moveRejectsMissingResource() {
        BiWorkspaceMapper workspaceMapper = mock(BiWorkspaceMapper.class);
        BiWorkspaceDO workspace = new BiWorkspaceDO();
        workspace.setId(5L);
        when(workspaceMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(workspace);
        BiResourceMovementService service = new BiResourceMovementService(
                workspaceMapper,
                mock(BiDatasetMapper.class),
                mock(BiDashboardMapper.class),
                mock(BiChartMapper.class),
                mock(BiPortalMapper.class),
                mock(BiBigScreenMapper.class),
                mock(BiSpreadsheetMapper.class),
                mock(BiResourceLocationMapper.class));

        assertThatThrownBy(() -> service.move(7L, "alice",
                new BiResourceMoveCommand("CHART", "missing-chart", "operations", 1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("BI resource not found");
    }

    @Test
    void moveRejectsArchivedResource() {
        BiWorkspaceMapper workspaceMapper = mock(BiWorkspaceMapper.class);
        BiDashboardMapper dashboardMapper = mock(BiDashboardMapper.class);
        BiWorkspaceDO workspace = new BiWorkspaceDO();
        workspace.setId(5L);
        BiDashboardDO dashboard = new BiDashboardDO();
        dashboard.setId(99L);
        dashboard.setStatus("ARCHIVED");
        when(workspaceMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(workspace);
        when(dashboardMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(dashboard);
        BiResourceMovementService service = new BiResourceMovementService(
                workspaceMapper,
                mock(BiDatasetMapper.class),
                dashboardMapper,
                mock(BiChartMapper.class),
                mock(BiPortalMapper.class),
                mock(BiBigScreenMapper.class),
                mock(BiSpreadsheetMapper.class),
                mock(BiResourceLocationMapper.class));

        assertThatThrownBy(() -> service.move(7L, "alice",
                new BiResourceMoveCommand("DASHBOARD", "canvas-effect", "operations", 1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("BI resource is archived");
    }

    @Test
    void listLocationsReturnsWorkspaceScopedRows() {
        BiWorkspaceMapper workspaceMapper = mock(BiWorkspaceMapper.class);
        BiResourceLocationMapper locationMapper = mock(BiResourceLocationMapper.class);
        BiWorkspaceDO workspace = new BiWorkspaceDO();
        workspace.setId(5L);
        BiResourceLocationDO row = new BiResourceLocationDO();
        row.setId(42L);
        row.setTenantId(7L);
        row.setWorkspaceId(5L);
        row.setResourceType("DASHBOARD");
        row.setResourceKey("canvas-effect");
        row.setFolderKey("operations/q2");
        row.setSortOrder(30);
        row.setMovedBy("alice");
        when(workspaceMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(workspace);
        when(locationMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(row));
        BiResourceMovementService service = new BiResourceMovementService(
                workspaceMapper,
                mock(BiDatasetMapper.class),
                mock(BiDashboardMapper.class),
                mock(BiChartMapper.class),
                mock(BiPortalMapper.class),
                mock(BiBigScreenMapper.class),
                mock(BiSpreadsheetMapper.class),
                locationMapper);

        List<BiResourceLocationView> locations = service.list(7L, "dashboard");

        assertThat(locations).singleElement().satisfies(location -> {
            assertThat(location.id()).isEqualTo(42L);
            assertThat(location.workspaceId()).isEqualTo(5L);
            assertThat(location.resourceType()).isEqualTo("DASHBOARD");
            assertThat(location.resourceKey()).isEqualTo("canvas-effect");
            assertThat(location.folderKey()).isEqualTo("operations/q2");
        });
    }
}
