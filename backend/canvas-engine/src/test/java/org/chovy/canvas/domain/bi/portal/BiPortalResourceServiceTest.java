package org.chovy.canvas.domain.bi.portal;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.dal.dataobject.BiChartDO;
import org.chovy.canvas.dal.dataobject.BiDashboardDO;
import org.chovy.canvas.dal.dataobject.BiPortalDO;
import org.chovy.canvas.dal.dataobject.BiPortalMenuDO;
import org.chovy.canvas.dal.dataobject.BiPortalVersionDO;
import org.chovy.canvas.dal.dataobject.BiWorkspaceDO;
import org.chovy.canvas.dal.mapper.BiChartMapper;
import org.chovy.canvas.dal.mapper.BiDashboardMapper;
import org.chovy.canvas.dal.mapper.BiPortalMapper;
import org.chovy.canvas.dal.mapper.BiPortalMenuMapper;
import org.chovy.canvas.dal.mapper.BiPortalVersionMapper;
import org.chovy.canvas.dal.mapper.BiWorkspaceMapper;
import org.chovy.canvas.domain.bi.permission.BiPermissionService;
import org.chovy.canvas.domain.bi.resource.BiPublishApprovalService;
import org.chovy.canvas.domain.bi.resource.BiResourceCollaborationService;
import org.chovy.canvas.domain.bi.resource.BiResourcePermissionGuard;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BiPortalResourceServiceTest {

    @Test
    void saveDraftPersistsPortalAndMenus() {
        BiWorkspaceMapper workspaceMapper = mock(BiWorkspaceMapper.class);
        BiPortalMapper portalMapper = mock(BiPortalMapper.class);
        BiPortalMenuMapper menuMapper = mock(BiPortalMenuMapper.class);
        BiDashboardMapper dashboardMapper = mock(BiDashboardMapper.class);
        BiChartMapper chartMapper = mock(BiChartMapper.class);
        when(workspaceMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(workspace());
        when(portalMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(portal("DRAFT"));
        when(dashboardMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(dashboard());
        when(chartMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(chart());
        BiPortalResourceService service = service(workspaceMapper, portalMapper, menuMapper, dashboardMapper, chartMapper);

        BiPortalResource resource = service.saveDraft(7L, "alice", portalResource());

        ArgumentCaptor<BiPortalDO> portalCaptor = ArgumentCaptor.forClass(BiPortalDO.class);
        ArgumentCaptor<BiPortalMenuDO> menuCaptor = ArgumentCaptor.forClass(BiPortalMenuDO.class);
        verify(portalMapper).upsert(portalCaptor.capture());
        verify(menuMapper).deleteByPortal(7L, 31L);
        verify(menuMapper, org.mockito.Mockito.times(3)).insert(menuCaptor.capture());
        assertThat(portalCaptor.getValue().getTenantId()).isEqualTo(7L);
        assertThat(portalCaptor.getValue().getPortalKey()).isEqualTo("canvas-ops-portal");
        assertThat(menuCaptor.getAllValues()).extracting(BiPortalMenuDO::getResourceType)
                .containsExactly("DASHBOARD", "CHART", "EXTERNAL_LINK");
        assertThat(menuCaptor.getAllValues().get(0).getResourceId()).isEqualTo(21L);
        assertThat(menuCaptor.getAllValues().get(1).getResourceId()).isEqualTo(22L);
        assertThat(resource.status()).isEqualTo("DRAFT");
        assertThat(resource.menus()).hasSize(3);
    }

    @Test
    void saveDraftRejectsUnsafeExternalUrl() {
        BiPortalResourceService service = service(
                mock(BiWorkspaceMapper.class),
                mock(BiPortalMapper.class),
                mock(BiPortalMenuMapper.class),
                mock(BiDashboardMapper.class),
                mock(BiChartMapper.class));
        BiPortalResource resource = new BiPortalResource(
                "unsafe-portal",
                "Unsafe Portal",
                Map.of(),
                List.of(new BiPortalMenuResource(
                        "bad-link",
                        null,
                        "Bad",
                        "EXTERNAL_LINK",
                        null,
                        null,
                        "javascript:alert(1)",
                        Map.of(),
                        10)),
                "DRAFT",
                "CLIENT");

        assertThatThrownBy(() -> service.saveDraft(7L, "alice", resource))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("externalUrl must be http(s) or an internal path");
    }

    @Test
    void publishAndArchiveUpdateLifecycle() {
        BiWorkspaceMapper workspaceMapper = mock(BiWorkspaceMapper.class);
        BiPortalMapper portalMapper = mock(BiPortalMapper.class);
        BiPortalMenuMapper menuMapper = mock(BiPortalMenuMapper.class);
        BiDashboardMapper dashboardMapper = mock(BiDashboardMapper.class);
        BiChartMapper chartMapper = mock(BiChartMapper.class);
        when(workspaceMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(workspace());
        when(portalMapper.selectOne(any(LambdaQueryWrapper.class)))
                .thenReturn(portal("DRAFT"), portal("PUBLISHED"), portal("PUBLISHED"), portal("ARCHIVED"));
        when(menuMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(menu()));
        when(dashboardMapper.selectById(21L)).thenReturn(dashboard());
        BiPortalResourceService service = service(workspaceMapper, portalMapper, menuMapper, dashboardMapper, chartMapper);

        BiPortalResource published = service.publish(7L, "canvas-ops-portal");
        BiPortalResource archived = service.archive(7L, "canvas-ops-portal");

        verify(portalMapper).publish(7L, 5L, "canvas-ops-portal");
        verify(portalMapper).archive(7L, 5L, "canvas-ops-portal");
        assertThat(published.status()).isEqualTo("PUBLISHED");
        assertThat(archived.status()).isEqualTo("ARCHIVED");
        assertThat(published.menus()).singleElement()
                .extracting(BiPortalMenuResource::resourceKey)
                .isEqualTo("canvas-effect");
    }

    @Test
    void publishInsertsPortalVersionSnapshot() {
        BiWorkspaceMapper workspaceMapper = mock(BiWorkspaceMapper.class);
        BiPortalMapper portalMapper = mock(BiPortalMapper.class);
        BiPortalMenuMapper menuMapper = mock(BiPortalMenuMapper.class);
        BiDashboardMapper dashboardMapper = mock(BiDashboardMapper.class);
        BiChartMapper chartMapper = mock(BiChartMapper.class);
        BiPortalVersionMapper versionMapper = mock(BiPortalVersionMapper.class);
        when(workspaceMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(workspace());
        when(portalMapper.selectOne(any(LambdaQueryWrapper.class)))
                .thenReturn(portal("DRAFT"), portal("PUBLISHED"));
        when(menuMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(menu()));
        when(dashboardMapper.selectById(21L)).thenReturn(dashboard());
        when(versionMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        BiPortalResourceService service =
                service(workspaceMapper, portalMapper, menuMapper, dashboardMapper, chartMapper, versionMapper);

        BiPortalResource published = service.publish(7L, "alice", "canvas-ops-portal");

        ArgumentCaptor<BiPortalVersionDO> versionCaptor = ArgumentCaptor.forClass(BiPortalVersionDO.class);
        verify(versionMapper).insert(versionCaptor.capture());
        assertThat(published.status()).isEqualTo("PUBLISHED");
        assertThat(versionCaptor.getValue().getTenantId()).isEqualTo(7L);
        assertThat(versionCaptor.getValue().getWorkspaceId()).isEqualTo(5L);
        assertThat(versionCaptor.getValue().getPortalId()).isEqualTo(31L);
        assertThat(versionCaptor.getValue().getPortalKey()).isEqualTo("canvas-ops-portal");
        assertThat(versionCaptor.getValue().getVersion()).isEqualTo(1);
        assertThat(versionCaptor.getValue().getPublishedBy()).isEqualTo("alice");
        assertThat(versionCaptor.getValue().getResourceJson()).contains("\"portalKey\":\"canvas-ops-portal\"");
    }

    @Test
    void publishRequiresPublishPermission() {
        BiWorkspaceMapper workspaceMapper = mock(BiWorkspaceMapper.class);
        BiPortalMapper portalMapper = mock(BiPortalMapper.class);
        BiPortalMenuMapper menuMapper = mock(BiPortalMenuMapper.class);
        BiDashboardMapper dashboardMapper = mock(BiDashboardMapper.class);
        BiChartMapper chartMapper = mock(BiChartMapper.class);
        BiResourcePermissionGuard permissionGuard = mock(BiResourcePermissionGuard.class);
        when(workspaceMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(workspace());
        when(portalMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(portal("DRAFT"));
        org.mockito.Mockito.doThrow(new BiPermissionService.BiPermissionDeniedException("missing publish"))
                .when(permissionGuard)
                .require(7L, 5L, "PORTAL", 31L, "alice", "ANALYST", BiPermissionService.ACTION_PUBLISH);
        BiPortalResourceService service = new BiPortalResourceService(
                workspaceMapper,
                portalMapper,
                menuMapper,
                dashboardMapper,
                chartMapper,
                null,
                new ObjectMapper(),
                permissionGuard);

        assertThatThrownBy(() -> service.publish(7L, "alice", "ANALYST", "canvas-ops-portal"))
                .isInstanceOf(BiPermissionService.BiPermissionDeniedException.class)
                .hasMessageContaining("missing publish");

        verify(permissionGuard).require(7L, 5L, "PORTAL", 31L, "alice", "ANALYST", BiPermissionService.ACTION_PUBLISH);
        verify(portalMapper, org.mockito.Mockito.never()).publish(7L, 5L, "canvas-ops-portal");
    }

    @Test
    void saveDraftRequiresCurrentEditLockForExistingPortal() {
        BiWorkspaceMapper workspaceMapper = mock(BiWorkspaceMapper.class);
        BiPortalMapper portalMapper = mock(BiPortalMapper.class);
        BiPortalMenuMapper menuMapper = mock(BiPortalMenuMapper.class);
        BiDashboardMapper dashboardMapper = mock(BiDashboardMapper.class);
        BiChartMapper chartMapper = mock(BiChartMapper.class);
        BiResourceCollaborationService collaborationService = mock(BiResourceCollaborationService.class);
        when(workspaceMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(workspace());
        when(portalMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(portal("DRAFT"));
        when(dashboardMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(dashboard());
        when(chartMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(chart());
        org.mockito.Mockito.doThrow(new BiResourceCollaborationService.BiResourceLockRequiredException(
                        "active BI resource lock is required"))
                .when(collaborationService)
                .requireCurrentLock(7L, 5L, "PORTAL", "canvas-ops-portal", "alice", "bad-token");
        BiPortalResourceService service = new BiPortalResourceService(
                workspaceMapper,
                portalMapper,
                menuMapper,
                dashboardMapper,
                chartMapper,
                null,
                new ObjectMapper(),
                null,
                null,
                collaborationService);

        assertThatThrownBy(() -> service.saveDraft(
                7L, "alice", "ANALYST", portalResource(), "bad-token"))
                .isInstanceOf(BiResourceCollaborationService.BiResourceLockRequiredException.class)
                .hasMessageContaining("active BI resource lock is required");

        verify(collaborationService).requireCurrentLock(
                7L, 5L, "PORTAL", "canvas-ops-portal", "alice", "bad-token");
        verify(portalMapper, org.mockito.Mockito.never()).upsert(any(BiPortalDO.class));
    }

    @Test
    void publishRequiresFreshApprovalForNonAdmin() {
        BiWorkspaceMapper workspaceMapper = mock(BiWorkspaceMapper.class);
        BiPortalMapper portalMapper = mock(BiPortalMapper.class);
        BiPortalMenuMapper menuMapper = mock(BiPortalMenuMapper.class);
        BiDashboardMapper dashboardMapper = mock(BiDashboardMapper.class);
        BiChartMapper chartMapper = mock(BiChartMapper.class);
        BiPublishApprovalService approvalService = mock(BiPublishApprovalService.class);
        LocalDateTime updatedAt = LocalDateTime.of(2026, 6, 5, 12, 0);
        BiPortalDO draft = portal("DRAFT");
        draft.setUpdatedAt(updatedAt);
        when(workspaceMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(workspace());
        when(portalMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(draft);
        org.mockito.Mockito.doThrow(new BiPublishApprovalService.BiPublishApprovalRequiredException(
                        "approved BI publish approval is required"))
                .when(approvalService)
                .requireApprovedApproval(7L, 5L, "PORTAL", "canvas-ops-portal", updatedAt);
        BiPortalResourceService service = new BiPortalResourceService(
                workspaceMapper,
                portalMapper,
                menuMapper,
                dashboardMapper,
                chartMapper,
                null,
                new ObjectMapper(),
                null,
                approvalService);

        assertThatThrownBy(() -> service.publish(7L, "alice", "ANALYST", "canvas-ops-portal"))
                .isInstanceOf(BiPublishApprovalService.BiPublishApprovalRequiredException.class)
                .hasMessageContaining("approved BI publish approval is required");

        verify(approvalService).requireApprovedApproval(7L, 5L, "PORTAL", "canvas-ops-portal", updatedAt);
        verify(portalMapper, org.mockito.Mockito.never()).publish(7L, 5L, "canvas-ops-portal");
    }

    @Test
    void tenantAdminPublishSkipsApprovalGate() {
        BiWorkspaceMapper workspaceMapper = mock(BiWorkspaceMapper.class);
        BiPortalMapper portalMapper = mock(BiPortalMapper.class);
        BiPortalMenuMapper menuMapper = mock(BiPortalMenuMapper.class);
        BiDashboardMapper dashboardMapper = mock(BiDashboardMapper.class);
        BiChartMapper chartMapper = mock(BiChartMapper.class);
        BiPublishApprovalService approvalService = mock(BiPublishApprovalService.class);
        when(workspaceMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(workspace());
        when(portalMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(portal("DRAFT"), portal("PUBLISHED"));
        when(menuMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(menu()));
        when(dashboardMapper.selectById(21L)).thenReturn(dashboard());
        BiPortalResourceService service = new BiPortalResourceService(
                workspaceMapper,
                portalMapper,
                menuMapper,
                dashboardMapper,
                chartMapper,
                null,
                new ObjectMapper(),
                null,
                approvalService);

        BiPortalResource published = service.publish(7L, "admin", "TENANT_ADMIN", "canvas-ops-portal");

        assertThat(published.status()).isEqualTo("PUBLISHED");
        verify(approvalService, org.mockito.Mockito.never()).requireApprovedApproval(
                any(), any(), any(), any(), any());
        verify(portalMapper).publish(7L, 5L, "canvas-ops-portal");
    }

    @Test
    void listAndRestorePortalVersionsUseSnapshots() {
        BiWorkspaceMapper workspaceMapper = mock(BiWorkspaceMapper.class);
        BiPortalMapper portalMapper = mock(BiPortalMapper.class);
        BiPortalMenuMapper menuMapper = mock(BiPortalMenuMapper.class);
        BiDashboardMapper dashboardMapper = mock(BiDashboardMapper.class);
        BiChartMapper chartMapper = mock(BiChartMapper.class);
        BiPortalVersionMapper versionMapper = mock(BiPortalVersionMapper.class);
        when(workspaceMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(workspace());
        when(portalMapper.selectOne(any(LambdaQueryWrapper.class)))
                .thenReturn(portal("PUBLISHED"), portal("PUBLISHED"), portal("DRAFT"));
        when(menuMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(menu()));
        when(dashboardMapper.selectById(21L)).thenReturn(dashboard());
        when(dashboardMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(dashboard());
        when(chartMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(chart());
        BiPortalVersionDO snapshot = portalVersion(2, portalResource());
        when(versionMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(snapshot));
        when(versionMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(snapshot);
        BiPortalResourceService service =
                service(workspaceMapper, portalMapper, menuMapper, dashboardMapper, chartMapper, versionMapper);

        List<BiPortalVersionView> versions = service.listVersions(7L, "canvas-ops-portal", 5);
        BiPortalResource restored = service.restoreVersion(7L, "alice", "canvas-ops-portal", 2);

        assertThat(versions).singleElement()
                .satisfies(version -> {
                    assertThat(version.version()).isEqualTo(2);
                    assertThat(version.resource().portalKey()).isEqualTo("canvas-ops-portal");
                });
        assertThat(restored.status()).isEqualTo("DRAFT");
        verify(portalMapper).upsert(any(BiPortalDO.class));
    }

    private BiPortalResourceService service(BiWorkspaceMapper workspaceMapper,
                                            BiPortalMapper portalMapper,
                                            BiPortalMenuMapper menuMapper,
                                            BiDashboardMapper dashboardMapper,
                                            BiChartMapper chartMapper) {
        return new BiPortalResourceService(
                workspaceMapper,
                portalMapper,
                menuMapper,
                dashboardMapper,
                chartMapper,
                new ObjectMapper());
    }

    private BiPortalResourceService service(BiWorkspaceMapper workspaceMapper,
                                            BiPortalMapper portalMapper,
                                            BiPortalMenuMapper menuMapper,
                                            BiDashboardMapper dashboardMapper,
                                            BiChartMapper chartMapper,
                                            BiPortalVersionMapper versionMapper) {
        return new BiPortalResourceService(
                workspaceMapper,
                portalMapper,
                menuMapper,
                dashboardMapper,
                chartMapper,
                versionMapper,
                new ObjectMapper());
    }

    private BiPortalResource portalResource() {
        return new BiPortalResource(
                "canvas-ops-portal",
                "Canvas Operations Portal",
                Map.of("theme", "light", "defaultMenuKey", "overview"),
                List.of(
                        new BiPortalMenuResource("overview", null, "经营总览", "DASHBOARD", "canvas-effect", null, null, Map.of("roles", List.of("OPERATOR")), 10),
                        new BiPortalMenuResource("trend", null, "执行趋势", "CHART", "trend-executions", null, null, Map.of("roles", List.of("OPERATOR")), 20),
                        new BiPortalMenuResource("canvas-list", null, "画布列表", "EXTERNAL_LINK", null, null, "/canvas", Map.of("roles", List.of("OPERATOR")), 30)),
                "DRAFT",
                "CLIENT");
    }

    private BiWorkspaceDO workspace() {
        BiWorkspaceDO row = new BiWorkspaceDO();
        row.setId(5L);
        row.setTenantId(0L);
        row.setWorkspaceKey("marketing_canvas");
        return row;
    }

    private BiPortalDO portal(String status) {
        BiPortalDO row = new BiPortalDO();
        row.setId(31L);
        row.setTenantId(7L);
        row.setWorkspaceId(5L);
        row.setPortalKey("canvas-ops-portal");
        row.setName("Canvas Operations Portal");
        row.setThemeJson("{\"theme\":\"light\"}");
        row.setStatus(status);
        return row;
    }

    private BiPortalMenuDO menu() {
        BiPortalMenuDO row = new BiPortalMenuDO();
        row.setId(41L);
        row.setTenantId(7L);
        row.setPortalId(31L);
        row.setMenuKey("overview");
        row.setTitle("经营总览");
        row.setResourceType("DASHBOARD");
        row.setResourceId(21L);
        row.setVisibilityJson("{\"roles\":[\"OPERATOR\"]}");
        row.setSortOrder(10);
        return row;
    }

    private BiDashboardDO dashboard() {
        BiDashboardDO row = new BiDashboardDO();
        row.setId(21L);
        row.setTenantId(7L);
        row.setWorkspaceId(5L);
        row.setDashboardKey("canvas-effect");
        row.setName("画布效果分析");
        row.setStatus("PUBLISHED");
        return row;
    }

    private BiChartDO chart() {
        BiChartDO row = new BiChartDO();
        row.setId(22L);
        row.setTenantId(7L);
        row.setWorkspaceId(5L);
        row.setChartKey("trend-executions");
        row.setName("Execution Trend");
        row.setChartType("LINE");
        row.setStatus("PUBLISHED");
        return row;
    }

    private BiPortalVersionDO portalVersion(int version, BiPortalResource resource) {
        BiPortalVersionDO row = new BiPortalVersionDO();
        row.setId(61L);
        row.setTenantId(7L);
        row.setWorkspaceId(5L);
        row.setPortalId(31L);
        row.setPortalKey("canvas-ops-portal");
        row.setVersion(version);
        row.setStatus("PUBLISHED");
        try {
            row.setResourceJson(new ObjectMapper().writeValueAsString(resource));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
        row.setPublishedBy("alice");
        return row;
    }
}
