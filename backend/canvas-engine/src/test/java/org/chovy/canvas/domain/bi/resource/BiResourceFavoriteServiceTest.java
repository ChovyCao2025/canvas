package org.chovy.canvas.domain.bi.resource;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.chovy.canvas.dal.dataobject.BiDashboardDO;
import org.chovy.canvas.dal.dataobject.BiResourceFavoriteDO;
import org.chovy.canvas.dal.dataobject.BiWorkspaceDO;
import org.chovy.canvas.dal.mapper.BiChartMapper;
import org.chovy.canvas.dal.mapper.BiDashboardMapper;
import org.chovy.canvas.dal.mapper.BiDatasetMapper;
import org.chovy.canvas.dal.mapper.BiPortalMapper;
import org.chovy.canvas.dal.mapper.BiResourceFavoriteMapper;
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

class BiResourceFavoriteServiceTest {

    @Test
    void favoriteMigrationCapturesUserScopedUniqueness() throws Exception {
        String migration = Files.readString(Path.of(
                "src/main/resources/db/migration/V245__bi_resource_favorite.sql"));

        assertThat(migration).contains("username VARCHAR(128) NOT NULL");
        assertThat(migration).contains("created_at DATETIME");
        assertThat(migration).contains("UNIQUE KEY uk_bi_resource_favorite");
    }

    @Test
    void favoriteDashboardPersistsTenantScopedUser() {
        BiWorkspaceMapper workspaceMapper = mock(BiWorkspaceMapper.class);
        BiDashboardMapper dashboardMapper = mock(BiDashboardMapper.class);
        BiResourceFavoriteMapper favoriteMapper = mock(BiResourceFavoriteMapper.class);
        BiWorkspaceDO workspace = new BiWorkspaceDO();
        workspace.setId(5L);
        BiDashboardDO dashboard = new BiDashboardDO();
        dashboard.setId(99L);
        dashboard.setStatus("PUBLISHED");
        when(workspaceMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(workspace);
        when(dashboardMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(dashboard);
        BiResourceFavoriteService service = new BiResourceFavoriteService(
                workspaceMapper,
                mock(BiDatasetMapper.class),
                dashboardMapper,
                mock(BiChartMapper.class),
                mock(BiPortalMapper.class),
                favoriteMapper);

        BiResourceFavoriteView favorite = service.favorite(7L, "alice",
                new BiResourceFavoriteCommand("DASHBOARD", "canvas-effect"));

        ArgumentCaptor<BiResourceFavoriteDO> favoriteCaptor = ArgumentCaptor.forClass(BiResourceFavoriteDO.class);
        verify(favoriteMapper).upsert(favoriteCaptor.capture());
        assertThat(favoriteCaptor.getValue().getTenantId()).isEqualTo(7L);
        assertThat(favoriteCaptor.getValue().getWorkspaceId()).isEqualTo(5L);
        assertThat(favoriteCaptor.getValue().getResourceType()).isEqualTo("DASHBOARD");
        assertThat(favoriteCaptor.getValue().getResourceKey()).isEqualTo("canvas-effect");
        assertThat(favoriteCaptor.getValue().getUsername()).isEqualTo("alice");
        assertThat(favorite.resourceType()).isEqualTo("DASHBOARD");
        assertThat(favorite.resourceKey()).isEqualTo("canvas-effect");
        assertThat(favorite.username()).isEqualTo("alice");
    }

    @Test
    void favoriteRejectsArchivedResource() {
        BiWorkspaceMapper workspaceMapper = mock(BiWorkspaceMapper.class);
        BiDashboardMapper dashboardMapper = mock(BiDashboardMapper.class);
        BiWorkspaceDO workspace = new BiWorkspaceDO();
        workspace.setId(5L);
        BiDashboardDO dashboard = new BiDashboardDO();
        dashboard.setId(99L);
        dashboard.setStatus("ARCHIVED");
        when(workspaceMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(workspace);
        when(dashboardMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(dashboard);
        BiResourceFavoriteService service = new BiResourceFavoriteService(
                workspaceMapper,
                mock(BiDatasetMapper.class),
                dashboardMapper,
                mock(BiChartMapper.class),
                mock(BiPortalMapper.class),
                mock(BiResourceFavoriteMapper.class));

        assertThatThrownBy(() -> service.favorite(7L, "alice",
                new BiResourceFavoriteCommand("DASHBOARD", "canvas-effect")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("BI resource is archived");
    }

    @Test
    void listFavoritesReturnsCurrentUserRows() {
        BiWorkspaceMapper workspaceMapper = mock(BiWorkspaceMapper.class);
        BiResourceFavoriteMapper favoriteMapper = mock(BiResourceFavoriteMapper.class);
        BiWorkspaceDO workspace = new BiWorkspaceDO();
        workspace.setId(5L);
        BiResourceFavoriteDO row = new BiResourceFavoriteDO();
        row.setId(42L);
        row.setTenantId(7L);
        row.setWorkspaceId(5L);
        row.setResourceType("DASHBOARD");
        row.setResourceKey("canvas-effect");
        row.setUsername("alice");
        when(workspaceMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(workspace);
        when(favoriteMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(row));
        BiResourceFavoriteService service = new BiResourceFavoriteService(
                workspaceMapper,
                mock(BiDatasetMapper.class),
                mock(BiDashboardMapper.class),
                mock(BiChartMapper.class),
                mock(BiPortalMapper.class),
                favoriteMapper);

        List<BiResourceFavoriteView> favorites = service.list(7L, "alice", "dashboard");

        assertThat(favorites).singleElement().satisfies(favorite -> {
            assertThat(favorite.workspaceId()).isEqualTo(5L);
            assertThat(favorite.resourceType()).isEqualTo("DASHBOARD");
            assertThat(favorite.resourceKey()).isEqualTo("canvas-effect");
            assertThat(favorite.username()).isEqualTo("alice");
        });
    }

    @Test
    void removeFavoriteDeletesCurrentUserRow() {
        BiWorkspaceMapper workspaceMapper = mock(BiWorkspaceMapper.class);
        BiResourceFavoriteMapper favoriteMapper = mock(BiResourceFavoriteMapper.class);
        BiWorkspaceDO workspace = new BiWorkspaceDO();
        workspace.setId(5L);
        when(workspaceMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(workspace);
        BiResourceFavoriteService service = new BiResourceFavoriteService(
                workspaceMapper,
                mock(BiDatasetMapper.class),
                mock(BiDashboardMapper.class),
                mock(BiChartMapper.class),
                mock(BiPortalMapper.class),
                favoriteMapper);

        service.unfavorite(7L, "alice", "dashboard", "canvas-effect");

        verify(favoriteMapper).deleteFavorite(7L, 5L, "DASHBOARD", "canvas-effect", "alice");
    }
}
