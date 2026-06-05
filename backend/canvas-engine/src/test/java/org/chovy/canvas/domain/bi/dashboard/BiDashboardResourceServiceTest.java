package org.chovy.canvas.domain.bi.dashboard;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.dal.dataobject.BiDashboardDO;
import org.chovy.canvas.dal.dataobject.BiDashboardVersionDO;
import org.chovy.canvas.dal.dataobject.BiDashboardWidgetDO;
import org.chovy.canvas.dal.dataobject.BiWorkspaceDO;
import org.chovy.canvas.dal.mapper.BiDashboardMapper;
import org.chovy.canvas.dal.mapper.BiDashboardVersionMapper;
import org.chovy.canvas.dal.mapper.BiDashboardWidgetMapper;
import org.chovy.canvas.dal.mapper.BiWorkspaceMapper;
import org.chovy.canvas.domain.bi.permission.BiPermissionService;
import org.chovy.canvas.domain.bi.resource.BiPublishApprovalService;
import org.chovy.canvas.domain.bi.resource.BiResourceCollaborationService;
import org.chovy.canvas.domain.bi.resource.BiResourcePermissionGuard;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BiDashboardResourceServiceTest {

    @Test
    void saveDraftPersistsDashboardAndWidgets() {
        BiWorkspaceMapper workspaceMapper = mock(BiWorkspaceMapper.class);
        BiDashboardMapper dashboardMapper = mock(BiDashboardMapper.class);
        BiDashboardWidgetMapper widgetMapper = mock(BiDashboardWidgetMapper.class);
        BiDashboardVersionMapper versionMapper = mock(BiDashboardVersionMapper.class);
        BiWorkspaceDO workspace = new BiWorkspaceDO();
        workspace.setId(5L);
        BiDashboardDO persisted = new BiDashboardDO();
        persisted.setId(99L);
        persisted.setTenantId(7L);
        persisted.setWorkspaceId(5L);
        persisted.setDashboardKey("canvas-effect");
        persisted.setName("画布效果分析");
        persisted.setStatus("DRAFT");
        persisted.setVersion(1);
        when(workspaceMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(workspace);
        when(dashboardMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null, persisted);
        when(widgetMapper.insert(any(BiDashboardWidgetDO.class))).thenReturn(1);
        BiDashboardResourceService service = new BiDashboardResourceService(
                workspaceMapper,
                dashboardMapper,
                widgetMapper,
                versionMapper,
                new ObjectMapper());

        BiDashboardResource resource = service.saveDraft(7L, "alice",
                MarketingBiDashboardPresetRegistry.preset("canvas-effect"));

        ArgumentCaptor<BiDashboardDO> dashboardCaptor = ArgumentCaptor.forClass(BiDashboardDO.class);
        verify(dashboardMapper).upsert(dashboardCaptor.capture());
        assertThat(dashboardCaptor.getValue().getTenantId()).isEqualTo(7L);
        assertThat(dashboardCaptor.getValue().getWorkspaceId()).isEqualTo(5L);
        assertThat(dashboardCaptor.getValue().getDashboardKey()).isEqualTo("canvas-effect");
        assertThat(dashboardCaptor.getValue().getStatus()).isEqualTo("DRAFT");
        verify(widgetMapper).deleteByDashboard(7L, 99L);
        verify(widgetMapper, org.mockito.Mockito.atLeastOnce()).insert(any(BiDashboardWidgetDO.class));
        assertThat(resource.status()).isEqualTo("DRAFT");
        assertThat(resource.version()).isEqualTo(1);
        assertThat(resource.source()).isEqualTo("PERSISTED");
    }

    @Test
    void getFallsBackToBuiltInPresetWhenNotPersisted() {
        BiWorkspaceMapper workspaceMapper = mock(BiWorkspaceMapper.class);
        BiDashboardMapper dashboardMapper = mock(BiDashboardMapper.class);
        BiDashboardWidgetMapper widgetMapper = mock(BiDashboardWidgetMapper.class);
        BiDashboardVersionMapper versionMapper = mock(BiDashboardVersionMapper.class);
        BiWorkspaceDO workspace = new BiWorkspaceDO();
        workspace.setId(5L);
        when(workspaceMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(workspace);
        when(dashboardMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        BiDashboardResourceService service = new BiDashboardResourceService(
                workspaceMapper,
                dashboardMapper,
                widgetMapper,
                versionMapper,
                new ObjectMapper());

        BiDashboardResource resource = service.get(7L, "canvas-effect");

        assertThat(resource.source()).isEqualTo("PRESET");
        assertThat(resource.status()).isEqualTo("PRESET");
        assertThat(resource.preset().dashboardKey()).isEqualTo("canvas-effect");
        assertThat(resource.preset().widgets()).hasSizeGreaterThan(1);
    }

    @Test
    void publishUpdatesDashboardLifecycle() {
        BiWorkspaceMapper workspaceMapper = mock(BiWorkspaceMapper.class);
        BiDashboardMapper dashboardMapper = mock(BiDashboardMapper.class);
        BiDashboardWidgetMapper widgetMapper = mock(BiDashboardWidgetMapper.class);
        BiDashboardVersionMapper versionMapper = mock(BiDashboardVersionMapper.class);
        BiWorkspaceDO workspace = new BiWorkspaceDO();
        workspace.setId(5L);
        BiDashboardDO draft = dashboard("DRAFT", 1);
        BiDashboardDO published = dashboard("PUBLISHED", 2);
        when(workspaceMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(workspace);
        when(dashboardMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(draft, published);
        when(widgetMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(widget()));
        BiDashboardResourceService service = new BiDashboardResourceService(
                workspaceMapper,
                dashboardMapper,
                widgetMapper,
                versionMapper,
                new ObjectMapper());

        BiDashboardResource resource = service.publish(7L, "alice", "canvas-effect");

        verify(dashboardMapper).publish(7L, 5L, "canvas-effect");
        ArgumentCaptor<BiDashboardVersionDO> versionCaptor = ArgumentCaptor.forClass(BiDashboardVersionDO.class);
        verify(versionMapper).insert(versionCaptor.capture());
        assertThat(versionCaptor.getValue().getTenantId()).isEqualTo(7L);
        assertThat(versionCaptor.getValue().getWorkspaceId()).isEqualTo(5L);
        assertThat(versionCaptor.getValue().getDashboardId()).isEqualTo(99L);
        assertThat(versionCaptor.getValue().getDashboardKey()).isEqualTo("canvas-effect");
        assertThat(versionCaptor.getValue().getVersion()).isEqualTo(2);
        assertThat(versionCaptor.getValue().getStatus()).isEqualTo("PUBLISHED");
        assertThat(versionCaptor.getValue().getPresetJson()).contains("\"dashboardKey\":\"canvas-effect\"");
        assertThat(versionCaptor.getValue().getPublishedBy()).isEqualTo("alice");
        assertThat(resource.status()).isEqualTo("PUBLISHED");
        assertThat(resource.version()).isEqualTo(2);
        assertThat(resource.preset().widgets()).singleElement()
                .extracting(BiDashboardWidget::widgetKey)
                .isEqualTo("kpi-total-executions");
    }

    @Test
    void publishRequiresPublishPermission() {
        BiWorkspaceMapper workspaceMapper = mock(BiWorkspaceMapper.class);
        BiDashboardMapper dashboardMapper = mock(BiDashboardMapper.class);
        BiDashboardWidgetMapper widgetMapper = mock(BiDashboardWidgetMapper.class);
        BiDashboardVersionMapper versionMapper = mock(BiDashboardVersionMapper.class);
        BiResourcePermissionGuard permissionGuard = mock(BiResourcePermissionGuard.class);
        BiWorkspaceDO workspace = new BiWorkspaceDO();
        workspace.setId(5L);
        BiDashboardDO draft = dashboard("DRAFT", 1);
        when(workspaceMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(workspace);
        when(dashboardMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(draft);
        org.mockito.Mockito.doThrow(new BiPermissionService.BiPermissionDeniedException("missing publish"))
                .when(permissionGuard)
                .require(7L, 5L, "DASHBOARD", 99L, "alice", "ANALYST", BiPermissionService.ACTION_PUBLISH);
        BiDashboardResourceService service = new BiDashboardResourceService(
                workspaceMapper,
                dashboardMapper,
                widgetMapper,
                versionMapper,
                new ObjectMapper(),
                permissionGuard);

        org.assertj.core.api.Assertions.assertThatThrownBy(
                        () -> service.publish(7L, "alice", "ANALYST", "canvas-effect"))
                .isInstanceOf(BiPermissionService.BiPermissionDeniedException.class)
                .hasMessageContaining("missing publish");

        verify(permissionGuard).require(7L, 5L, "DASHBOARD", 99L, "alice", "ANALYST", BiPermissionService.ACTION_PUBLISH);
        verify(dashboardMapper, org.mockito.Mockito.never()).publish(7L, 5L, "canvas-effect");
    }

    @Test
    void saveDraftRequiresCurrentEditLockForExistingDashboard() {
        BiWorkspaceMapper workspaceMapper = mock(BiWorkspaceMapper.class);
        BiDashboardMapper dashboardMapper = mock(BiDashboardMapper.class);
        BiDashboardWidgetMapper widgetMapper = mock(BiDashboardWidgetMapper.class);
        BiDashboardVersionMapper versionMapper = mock(BiDashboardVersionMapper.class);
        BiResourceCollaborationService collaborationService = mock(BiResourceCollaborationService.class);
        BiWorkspaceDO workspace = new BiWorkspaceDO();
        workspace.setId(5L);
        BiDashboardDO draft = dashboard("DRAFT", 1);
        when(workspaceMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(workspace);
        when(dashboardMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(draft);
        org.mockito.Mockito.doThrow(new BiResourceCollaborationService.BiResourceLockRequiredException(
                        "active BI resource lock is required"))
                .when(collaborationService)
                .requireCurrentLock(7L, 5L, "DASHBOARD", "canvas-effect", "alice", "bad-token");
        BiDashboardResourceService service = new BiDashboardResourceService(
                workspaceMapper,
                dashboardMapper,
                widgetMapper,
                versionMapper,
                new ObjectMapper(),
                null,
                null,
                collaborationService);

        assertThatThrownBy(() -> service.saveDraft(
                7L, "alice", "ANALYST", MarketingBiDashboardPresetRegistry.preset("canvas-effect"), "bad-token"))
                .isInstanceOf(BiResourceCollaborationService.BiResourceLockRequiredException.class)
                .hasMessageContaining("active BI resource lock is required");

        verify(collaborationService).requireCurrentLock(
                7L, 5L, "DASHBOARD", "canvas-effect", "alice", "bad-token");
        verify(dashboardMapper, org.mockito.Mockito.never()).upsert(any(BiDashboardDO.class));
    }

    @Test
    void publishRequiresFreshApprovalForNonAdmin() {
        BiWorkspaceMapper workspaceMapper = mock(BiWorkspaceMapper.class);
        BiDashboardMapper dashboardMapper = mock(BiDashboardMapper.class);
        BiDashboardWidgetMapper widgetMapper = mock(BiDashboardWidgetMapper.class);
        BiDashboardVersionMapper versionMapper = mock(BiDashboardVersionMapper.class);
        BiPublishApprovalService approvalService = mock(BiPublishApprovalService.class);
        LocalDateTime updatedAt = LocalDateTime.of(2026, 6, 5, 12, 0);
        BiDashboardDO draft = dashboard("DRAFT", 1);
        draft.setUpdatedAt(updatedAt);
        BiWorkspaceDO workspace = new BiWorkspaceDO();
        workspace.setId(5L);
        when(workspaceMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(workspace);
        when(dashboardMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(draft);
        org.mockito.Mockito.doThrow(new BiPublishApprovalService.BiPublishApprovalRequiredException(
                        "approved BI publish approval is required"))
                .when(approvalService)
                .requireApprovedApproval(7L, 5L, "DASHBOARD", "canvas-effect", updatedAt);
        BiDashboardResourceService service = new BiDashboardResourceService(
                workspaceMapper,
                dashboardMapper,
                widgetMapper,
                versionMapper,
                new ObjectMapper(),
                null,
                approvalService);

        org.assertj.core.api.Assertions.assertThatThrownBy(
                        () -> service.publish(7L, "alice", "ANALYST", "canvas-effect"))
                .isInstanceOf(BiPublishApprovalService.BiPublishApprovalRequiredException.class)
                .hasMessageContaining("approved BI publish approval is required");

        verify(approvalService).requireApprovedApproval(7L, 5L, "DASHBOARD", "canvas-effect", updatedAt);
        verify(dashboardMapper, org.mockito.Mockito.never()).publish(7L, 5L, "canvas-effect");
    }

    @Test
    void tenantAdminPublishSkipsApprovalGate() {
        BiWorkspaceMapper workspaceMapper = mock(BiWorkspaceMapper.class);
        BiDashboardMapper dashboardMapper = mock(BiDashboardMapper.class);
        BiDashboardWidgetMapper widgetMapper = mock(BiDashboardWidgetMapper.class);
        BiDashboardVersionMapper versionMapper = mock(BiDashboardVersionMapper.class);
        BiPublishApprovalService approvalService = mock(BiPublishApprovalService.class);
        BiWorkspaceDO workspace = new BiWorkspaceDO();
        workspace.setId(5L);
        when(workspaceMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(workspace);
        when(dashboardMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(dashboard("DRAFT", 1), dashboard("PUBLISHED", 2));
        when(widgetMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(widget()));
        BiDashboardResourceService service = new BiDashboardResourceService(
                workspaceMapper,
                dashboardMapper,
                widgetMapper,
                versionMapper,
                new ObjectMapper(),
                null,
                approvalService);

        BiDashboardResource published = service.publish(7L, "admin", "TENANT_ADMIN", "canvas-effect");

        assertThat(published.status()).isEqualTo("PUBLISHED");
        verify(approvalService, org.mockito.Mockito.never()).requireApprovedApproval(
                any(), any(), any(), any(), any());
        verify(dashboardMapper).publish(7L, 5L, "canvas-effect");
    }

    @Test
    void cloneResourceCopiesDashboardAsDraft() {
        BiWorkspaceMapper workspaceMapper = mock(BiWorkspaceMapper.class);
        BiDashboardMapper dashboardMapper = mock(BiDashboardMapper.class);
        BiDashboardWidgetMapper widgetMapper = mock(BiDashboardWidgetMapper.class);
        BiDashboardVersionMapper versionMapper = mock(BiDashboardVersionMapper.class);
        BiWorkspaceDO workspace = new BiWorkspaceDO();
        workspace.setId(5L);
        BiDashboardDO source = dashboard("canvas-effect", "PUBLISHED", 2, 99L);
        BiDashboardDO cloned = dashboard("canvas-effect-copy", "DRAFT", 1, 100L);
        cloned.setName("画布效果分析 副本");
        when(workspaceMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(workspace);
        when(dashboardMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null, source, null, cloned);
        when(widgetMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(widget()));
        BiDashboardResourceService service = new BiDashboardResourceService(
                workspaceMapper,
                dashboardMapper,
                widgetMapper,
                versionMapper,
                new ObjectMapper());

        BiDashboardResource resource = service.cloneResource(7L, "alice", "canvas-effect",
                new BiDashboardCloneCommand("canvas-effect-copy", "画布效果分析 副本", "复制用于运营复盘"));

        ArgumentCaptor<BiDashboardDO> dashboardCaptor = ArgumentCaptor.forClass(BiDashboardDO.class);
        verify(dashboardMapper).upsert(dashboardCaptor.capture());
        assertThat(dashboardCaptor.getValue().getDashboardKey()).isEqualTo("canvas-effect-copy");
        assertThat(dashboardCaptor.getValue().getName()).isEqualTo("画布效果分析 副本");
        assertThat(dashboardCaptor.getValue().getStatus()).isEqualTo("DRAFT");
        verify(widgetMapper).deleteByDashboard(7L, 100L);
        verify(widgetMapper, org.mockito.Mockito.atLeastOnce()).insert(any(BiDashboardWidgetDO.class));
        assertThat(resource.status()).isEqualTo("DRAFT");
        assertThat(resource.version()).isEqualTo(1);
        assertThat(resource.preset().dashboardKey()).isEqualTo("canvas-effect-copy");
    }

    @Test
    void exportResourceRequiresPublishedDashboardPackage() {
        BiWorkspaceMapper workspaceMapper = mock(BiWorkspaceMapper.class);
        BiDashboardMapper dashboardMapper = mock(BiDashboardMapper.class);
        BiDashboardWidgetMapper widgetMapper = mock(BiDashboardWidgetMapper.class);
        BiDashboardVersionMapper versionMapper = mock(BiDashboardVersionMapper.class);
        BiWorkspaceDO workspace = new BiWorkspaceDO();
        workspace.setId(5L);
        when(workspaceMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(workspace);
        when(dashboardMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(dashboard("PUBLISHED", 2));
        when(widgetMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(widget()));
        BiDashboardResourceService service = new BiDashboardResourceService(
                workspaceMapper,
                dashboardMapper,
                widgetMapper,
                versionMapper,
                new ObjectMapper());

        BiDashboardExportPackage exported = service.exportResource(7L, "alice", "canvas-effect");

        assertThat(exported.resourceType()).isEqualTo("DASHBOARD");
        assertThat(exported.schemaVersion()).isEqualTo(1);
        assertThat(exported.sourceDashboardKey()).isEqualTo("canvas-effect");
        assertThat(exported.sourceVersion()).isEqualTo(2);
        assertThat(exported.exportedBy()).isEqualTo("alice");
        assertThat(exported.preset().widgets()).singleElement()
                .extracting(BiDashboardWidget::widgetKey)
                .isEqualTo("kpi-total-executions");
    }

    @Test
    void exportResourceFileSerializesDownloadableJsonPackage() {
        BiWorkspaceMapper workspaceMapper = mock(BiWorkspaceMapper.class);
        BiDashboardMapper dashboardMapper = mock(BiDashboardMapper.class);
        BiDashboardWidgetMapper widgetMapper = mock(BiDashboardWidgetMapper.class);
        BiDashboardVersionMapper versionMapper = mock(BiDashboardVersionMapper.class);
        BiWorkspaceDO workspace = new BiWorkspaceDO();
        workspace.setId(5L);
        when(workspaceMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(workspace);
        when(dashboardMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(dashboard("PUBLISHED", 2));
        when(widgetMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(widget()));
        BiDashboardResourceService service = new BiDashboardResourceService(
                workspaceMapper,
                dashboardMapper,
                widgetMapper,
                versionMapper,
                new ObjectMapper().findAndRegisterModules());

        BiDashboardResourceService.DashboardPackageFile file =
                service.exportResourceFile(7L, "alice", "canvas-effect");

        assertThat(file.filename()).isEqualTo("canvas-effect-v2.bi-dashboard.json");
        assertThat(file.contentType()).isEqualTo("application/json");
        String json = new String(file.content(), StandardCharsets.UTF_8);
        assertThat(json).contains("\"resourceType\" : \"DASHBOARD\"");
        assertThat(json).contains("\"sourceDashboardKey\" : \"canvas-effect\"");
        assertThat(json).contains("\"sourceVersion\" : 2");
    }

    @Test
    void importResourceWritesPackageAsDraft() {
        BiWorkspaceMapper workspaceMapper = mock(BiWorkspaceMapper.class);
        BiDashboardMapper dashboardMapper = mock(BiDashboardMapper.class);
        BiDashboardWidgetMapper widgetMapper = mock(BiDashboardWidgetMapper.class);
        BiDashboardVersionMapper versionMapper = mock(BiDashboardVersionMapper.class);
        BiWorkspaceDO workspace = new BiWorkspaceDO();
        workspace.setId(5L);
        BiDashboardDO persisted = dashboard("imported-canvas-effect", "DRAFT", 1, 120L);
        when(workspaceMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(workspace);
        when(dashboardMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null, persisted);
        BiDashboardResourceService service = new BiDashboardResourceService(
                workspaceMapper,
                dashboardMapper,
                widgetMapper,
                versionMapper,
                new ObjectMapper());
        BiDashboardExportPackage packagePayload = new BiDashboardExportPackage(
                "DASHBOARD",
                1,
                "canvas-effect",
                2,
                MarketingBiDashboardPresetRegistry.preset("canvas-effect"),
                "alice",
                java.time.LocalDateTime.parse("2026-06-05T09:30:00"));

        BiDashboardResource resource = service.importResource(7L, "bob",
                new BiDashboardImportCommand(packagePayload, "imported-canvas-effect", "导入画布效果", false));

        ArgumentCaptor<BiDashboardDO> dashboardCaptor = ArgumentCaptor.forClass(BiDashboardDO.class);
        verify(dashboardMapper).upsert(dashboardCaptor.capture());
        assertThat(dashboardCaptor.getValue().getDashboardKey()).isEqualTo("imported-canvas-effect");
        assertThat(dashboardCaptor.getValue().getName()).isEqualTo("导入画布效果");
        assertThat(dashboardCaptor.getValue().getStatus()).isEqualTo("DRAFT");
        verify(widgetMapper).deleteByDashboard(7L, 120L);
        verify(widgetMapper, org.mockito.Mockito.atLeastOnce()).insert(any(BiDashboardWidgetDO.class));
        assertThat(resource.status()).isEqualTo("DRAFT");
        assertThat(resource.preset().dashboardKey()).isEqualTo("imported-canvas-effect");
    }

    @Test
    void importResourceFileReadsPackageBytesAsDraft() throws Exception {
        BiWorkspaceMapper workspaceMapper = mock(BiWorkspaceMapper.class);
        BiDashboardMapper dashboardMapper = mock(BiDashboardMapper.class);
        BiDashboardWidgetMapper widgetMapper = mock(BiDashboardWidgetMapper.class);
        BiDashboardVersionMapper versionMapper = mock(BiDashboardVersionMapper.class);
        BiWorkspaceDO workspace = new BiWorkspaceDO();
        workspace.setId(5L);
        BiDashboardDO persisted = dashboard("uploaded-canvas-effect", "DRAFT", 1, 121L);
        when(workspaceMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(workspace);
        when(dashboardMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null, persisted);
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        BiDashboardResourceService service = new BiDashboardResourceService(
                workspaceMapper,
                dashboardMapper,
                widgetMapper,
                versionMapper,
                objectMapper);
        BiDashboardExportPackage packagePayload = new BiDashboardExportPackage(
                "DASHBOARD",
                1,
                "canvas-effect",
                2,
                MarketingBiDashboardPresetRegistry.preset("canvas-effect"),
                "alice",
                java.time.LocalDateTime.parse("2026-06-05T09:30:00"));
        byte[] content = objectMapper.writeValueAsBytes(packagePayload);

        BiDashboardResource resource = service.importResourceFile(
                7L,
                "bob",
                content,
                "uploaded-canvas-effect",
                "上传画布效果",
                false);

        ArgumentCaptor<BiDashboardDO> dashboardCaptor = ArgumentCaptor.forClass(BiDashboardDO.class);
        verify(dashboardMapper).upsert(dashboardCaptor.capture());
        assertThat(dashboardCaptor.getValue().getDashboardKey()).isEqualTo("uploaded-canvas-effect");
        assertThat(dashboardCaptor.getValue().getName()).isEqualTo("上传画布效果");
        assertThat(dashboardCaptor.getValue().getStatus()).isEqualTo("DRAFT");
        verify(widgetMapper).deleteByDashboard(7L, 121L);
        verify(widgetMapper, org.mockito.Mockito.atLeastOnce()).insert(any(BiDashboardWidgetDO.class));
        assertThat(resource.status()).isEqualTo("DRAFT");
        assertThat(resource.preset().dashboardKey()).isEqualTo("uploaded-canvas-effect");
    }

    @Test
    void archiveMarksDashboardAsArchived() {
        BiWorkspaceMapper workspaceMapper = mock(BiWorkspaceMapper.class);
        BiDashboardMapper dashboardMapper = mock(BiDashboardMapper.class);
        BiDashboardWidgetMapper widgetMapper = mock(BiDashboardWidgetMapper.class);
        BiDashboardVersionMapper versionMapper = mock(BiDashboardVersionMapper.class);
        BiWorkspaceDO workspace = new BiWorkspaceDO();
        workspace.setId(5L);
        BiDashboardDO published = dashboard("PUBLISHED", 2);
        BiDashboardDO archived = dashboard("ARCHIVED", 2);
        when(workspaceMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(workspace);
        when(dashboardMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(published, archived);
        when(widgetMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(widget()));
        BiDashboardResourceService service = new BiDashboardResourceService(
                workspaceMapper,
                dashboardMapper,
                widgetMapper,
                versionMapper,
                new ObjectMapper());

        BiDashboardResource resource = service.archive(7L, "canvas-effect");

        verify(dashboardMapper).archive(7L, 5L, "canvas-effect");
        assertThat(resource.status()).isEqualTo("ARCHIVED");
        assertThat(resource.version()).isEqualTo(2);
    }

    @Test
    void listVersionsReturnsPublishedSnapshots() throws Exception {
        BiWorkspaceMapper workspaceMapper = mock(BiWorkspaceMapper.class);
        BiDashboardMapper dashboardMapper = mock(BiDashboardMapper.class);
        BiDashboardWidgetMapper widgetMapper = mock(BiDashboardWidgetMapper.class);
        BiDashboardVersionMapper versionMapper = mock(BiDashboardVersionMapper.class);
        BiWorkspaceDO workspace = new BiWorkspaceDO();
        workspace.setId(5L);
        BiDashboardDO dashboard = dashboard("PUBLISHED", 2);
        BiDashboardVersionDO version = new BiDashboardVersionDO();
        version.setId(77L);
        version.setTenantId(7L);
        version.setWorkspaceId(5L);
        version.setDashboardId(99L);
        version.setDashboardKey("canvas-effect");
        version.setVersion(2);
        version.setStatus("PUBLISHED");
        version.setPresetJson(new ObjectMapper().writeValueAsString(MarketingBiDashboardPresetRegistry.preset("canvas-effect")));
        version.setPublishedBy("alice");
        when(workspaceMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(workspace);
        when(dashboardMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(dashboard);
        when(versionMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(version));
        BiDashboardResourceService service = new BiDashboardResourceService(
                workspaceMapper,
                dashboardMapper,
                widgetMapper,
                versionMapper,
                new ObjectMapper());

        List<BiDashboardVersionView> versions = service.listVersions(7L, "canvas-effect", 10);

        assertThat(versions).singleElement().satisfies(item -> {
            assertThat(item.id()).isEqualTo(77L);
            assertThat(item.dashboardKey()).isEqualTo("canvas-effect");
            assertThat(item.version()).isEqualTo(2);
            assertThat(item.status()).isEqualTo("PUBLISHED");
            assertThat(item.preset().widgets()).hasSizeGreaterThan(1);
            assertThat(item.publishedBy()).isEqualTo("alice");
        });
    }

    @Test
    void restoreVersionWritesSnapshotBackAsDraft() throws Exception {
        BiWorkspaceMapper workspaceMapper = mock(BiWorkspaceMapper.class);
        BiDashboardMapper dashboardMapper = mock(BiDashboardMapper.class);
        BiDashboardWidgetMapper widgetMapper = mock(BiDashboardWidgetMapper.class);
        BiDashboardVersionMapper versionMapper = mock(BiDashboardVersionMapper.class);
        ObjectMapper objectMapper = new ObjectMapper();
        BiWorkspaceDO workspace = new BiWorkspaceDO();
        workspace.setId(5L);
        BiDashboardDO dashboard = dashboard("PUBLISHED", 3);
        BiDashboardVersionDO snapshot = new BiDashboardVersionDO();
        snapshot.setId(78L);
        snapshot.setTenantId(7L);
        snapshot.setWorkspaceId(5L);
        snapshot.setDashboardId(99L);
        snapshot.setDashboardKey("canvas-effect");
        snapshot.setVersion(2);
        snapshot.setStatus("PUBLISHED");
        snapshot.setPresetJson(objectMapper.writeValueAsString(MarketingBiDashboardPresetRegistry.preset("canvas-effect")));
        when(workspaceMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(workspace);
        when(dashboardMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(dashboard, dashboard, dashboard);
        when(versionMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(snapshot);
        BiDashboardResourceService service = new BiDashboardResourceService(
                workspaceMapper,
                dashboardMapper,
                widgetMapper,
                versionMapper,
                objectMapper);

        BiDashboardResource resource = service.restoreVersion(7L, "alice", "canvas-effect", 2);

        ArgumentCaptor<BiDashboardDO> dashboardCaptor = ArgumentCaptor.forClass(BiDashboardDO.class);
        verify(dashboardMapper).upsert(dashboardCaptor.capture());
        assertThat(dashboardCaptor.getValue().getDashboardKey()).isEqualTo("canvas-effect");
        assertThat(dashboardCaptor.getValue().getStatus()).isEqualTo("DRAFT");
        assertThat(dashboardCaptor.getValue().getVersion()).isEqualTo(3);
        verify(widgetMapper).deleteByDashboard(7L, 99L);
        verify(widgetMapper, org.mockito.Mockito.atLeastOnce()).insert(any(BiDashboardWidgetDO.class));
        assertThat(resource.status()).isEqualTo("DRAFT");
        assertThat(resource.version()).isEqualTo(3);
    }

    private BiDashboardDO dashboard(String status, int version) {
        return dashboard("canvas-effect", status, version, 99L);
    }

    private BiDashboardDO dashboard(String dashboardKey, String status, int version, Long id) {
        BiDashboardDO row = new BiDashboardDO();
        row.setId(id);
        row.setTenantId(7L);
        row.setWorkspaceId(5L);
        row.setDashboardKey(dashboardKey);
        row.setName("画布效果分析");
        row.setDescription("desc");
        row.setThemeJson("{\"datasetKey\":\"canvas_daily_stats\",\"embedScopes\":[\"INTERNAL_CANVAS\"]}");
        row.setFilterJson("{\"filters\":[],\"interactions\":[],\"subscriptionChannels\":[]}");
        row.setStatus(status);
        row.setVersion(version);
        return row;
    }

    private BiDashboardWidgetDO widget() {
        BiDashboardWidgetDO row = new BiDashboardWidgetDO();
        row.setTenantId(7L);
        row.setDashboardId(99L);
        row.setWidgetKey("kpi-total-executions");
        row.setWidgetType("KPI_CARD");
        row.setTitle("执行次数");
        row.setLayoutJson("{\"x\":0,\"y\":0,\"w\":6,\"h\":3,\"stylePreset\":\"emphasis\"}");
        row.setQueryOverrideJson("{\"dimensions\":[],\"metrics\":[\"total_executions\"]}");
        return row;
    }
}
