package org.chovy.canvas.domain.bi.resource;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.chovy.canvas.dal.dataobject.BiDashboardDO;
import org.chovy.canvas.dal.dataobject.BiResourceOwnershipDO;
import org.chovy.canvas.dal.dataobject.BiWorkspaceDO;
import org.chovy.canvas.dal.mapper.BiChartMapper;
import org.chovy.canvas.dal.mapper.BiDashboardMapper;
import org.chovy.canvas.dal.mapper.BiDatasetMapper;
import org.chovy.canvas.dal.mapper.BiPortalMapper;
import org.chovy.canvas.dal.mapper.BiResourceOwnershipMapper;
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

class BiResourceTransferServiceTest {

    @Test
    void ownershipMigrationCapturesTransferAuditColumns() throws Exception {
        String migration = Files.readString(Path.of(
                "src/main/resources/db/migration/V244__bi_resource_ownership.sql"));

        assertThat(migration).contains("owner_user VARCHAR(128) NOT NULL");
        assertThat(migration).contains("transferred_by VARCHAR(128)");
        assertThat(migration).contains("transferred_at DATETIME");
    }

    @Test
    void transferDashboardPersistsTenantScopedOwner() {
        BiWorkspaceMapper workspaceMapper = mock(BiWorkspaceMapper.class);
        BiDashboardMapper dashboardMapper = mock(BiDashboardMapper.class);
        BiResourceOwnershipMapper ownershipMapper = mock(BiResourceOwnershipMapper.class);
        BiWorkspaceDO workspace = new BiWorkspaceDO();
        workspace.setId(5L);
        BiDashboardDO dashboard = new BiDashboardDO();
        dashboard.setId(99L);
        dashboard.setStatus("PUBLISHED");
        when(workspaceMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(workspace);
        when(dashboardMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(dashboard);
        BiResourceTransferService service = new BiResourceTransferService(
                workspaceMapper,
                mock(BiDatasetMapper.class),
                dashboardMapper,
                mock(BiChartMapper.class),
                mock(BiPortalMapper.class),
                ownershipMapper);

        BiResourceOwnershipView transferred = service.transfer(7L, "alice",
                new BiResourceTransferCommand("DASHBOARD", "canvas-effect", "bob@example.com"));

        ArgumentCaptor<BiResourceOwnershipDO> ownershipCaptor = ArgumentCaptor.forClass(BiResourceOwnershipDO.class);
        verify(ownershipMapper).upsert(ownershipCaptor.capture());
        assertThat(ownershipCaptor.getValue().getTenantId()).isEqualTo(7L);
        assertThat(ownershipCaptor.getValue().getWorkspaceId()).isEqualTo(5L);
        assertThat(ownershipCaptor.getValue().getResourceType()).isEqualTo("DASHBOARD");
        assertThat(ownershipCaptor.getValue().getResourceKey()).isEqualTo("canvas-effect");
        assertThat(ownershipCaptor.getValue().getOwnerUser()).isEqualTo("bob@example.com");
        assertThat(ownershipCaptor.getValue().getTransferredBy()).isEqualTo("alice");
        assertThat(transferred.ownerUser()).isEqualTo("bob@example.com");
        assertThat(transferred.transferredBy()).isEqualTo("alice");
    }

    @Test
    void transferRejectsArchivedResource() {
        BiWorkspaceMapper workspaceMapper = mock(BiWorkspaceMapper.class);
        BiDashboardMapper dashboardMapper = mock(BiDashboardMapper.class);
        BiWorkspaceDO workspace = new BiWorkspaceDO();
        workspace.setId(5L);
        BiDashboardDO dashboard = new BiDashboardDO();
        dashboard.setId(99L);
        dashboard.setStatus("ARCHIVED");
        when(workspaceMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(workspace);
        when(dashboardMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(dashboard);
        BiResourceTransferService service = new BiResourceTransferService(
                workspaceMapper,
                mock(BiDatasetMapper.class),
                dashboardMapper,
                mock(BiChartMapper.class),
                mock(BiPortalMapper.class),
                mock(BiResourceOwnershipMapper.class));

        assertThatThrownBy(() -> service.transfer(7L, "alice",
                new BiResourceTransferCommand("DASHBOARD", "canvas-effect", "bob")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("BI resource is archived");
    }

    @Test
    void listOwnershipsReturnsWorkspaceScopedRows() {
        BiWorkspaceMapper workspaceMapper = mock(BiWorkspaceMapper.class);
        BiResourceOwnershipMapper ownershipMapper = mock(BiResourceOwnershipMapper.class);
        BiWorkspaceDO workspace = new BiWorkspaceDO();
        workspace.setId(5L);
        BiResourceOwnershipDO row = new BiResourceOwnershipDO();
        row.setId(42L);
        row.setTenantId(7L);
        row.setWorkspaceId(5L);
        row.setResourceType("DASHBOARD");
        row.setResourceKey("canvas-effect");
        row.setOwnerUser("bob");
        row.setTransferredBy("alice");
        when(workspaceMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(workspace);
        when(ownershipMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(row));
        BiResourceTransferService service = new BiResourceTransferService(
                workspaceMapper,
                mock(BiDatasetMapper.class),
                mock(BiDashboardMapper.class),
                mock(BiChartMapper.class),
                mock(BiPortalMapper.class),
                ownershipMapper);

        List<BiResourceOwnershipView> ownerships = service.list(7L, "dashboard");

        assertThat(ownerships).singleElement().satisfies(ownership -> {
            assertThat(ownership.workspaceId()).isEqualTo(5L);
            assertThat(ownership.resourceType()).isEqualTo("DASHBOARD");
            assertThat(ownership.resourceKey()).isEqualTo("canvas-effect");
            assertThat(ownership.ownerUser()).isEqualTo("bob");
            assertThat(ownership.transferredBy()).isEqualTo("alice");
        });
    }
}
