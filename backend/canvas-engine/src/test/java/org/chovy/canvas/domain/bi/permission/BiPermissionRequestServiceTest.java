package org.chovy.canvas.domain.bi.permission;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.chovy.canvas.dal.dataobject.BiPermissionRequestDO;
import org.chovy.canvas.dal.dataobject.BiWorkspaceDO;
import org.chovy.canvas.dal.mapper.BiPermissionRequestMapper;
import org.chovy.canvas.dal.mapper.BiWorkspaceMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BiPermissionRequestServiceTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-06-06T02:00:00Z"), ZoneOffset.UTC);

    @Test
    void permissionRequestMigrationCapturesReviewLifecycle() throws Exception {
        String migration = Files.readString(Path.of(
                "src/main/resources/db/migration/V336__bi_permission_requests.sql"));

        assertThat(migration).contains("CREATE TABLE IF NOT EXISTS bi_permission_request");
        assertThat(migration).contains("resource_type VARCHAR(64) NOT NULL");
        assertThat(migration).contains("requested_action VARCHAR(64) NOT NULL");
        assertThat(migration).contains("granted_permission_id BIGINT NULL");
        assertThat(migration).contains("idx_bi_permission_request_status");
    }

    @Test
    void requestResourcePermissionPersistsPendingRequesterGrant() {
        Fixture fixture = fixture();
        when(fixture.workspaceMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(workspace());

        BiPermissionRequestView view = fixture.service.requestPermission(
                7L,
                "alice",
                new BiPermissionRequestCommand(
                        "dashboard",
                        "canvas-effect",
                        "export",
                        "需要下载本周复盘数据"));

        ArgumentCaptor<BiPermissionRequestDO> captor = ArgumentCaptor.forClass(BiPermissionRequestDO.class);
        verify(fixture.requestMapper).insert(captor.capture());
        assertThat(captor.getValue().getTenantId()).isEqualTo(7L);
        assertThat(captor.getValue().getWorkspaceId()).isEqualTo(5L);
        assertThat(captor.getValue().getResourceType()).isEqualTo("DASHBOARD");
        assertThat(captor.getValue().getResourceKey()).isEqualTo("canvas-effect");
        assertThat(captor.getValue().getRequestedAction()).isEqualTo("EXPORT");
        assertThat(captor.getValue().getRequestedBy()).isEqualTo("alice");
        assertThat(captor.getValue().getStatus()).isEqualTo("PENDING");
        assertThat(captor.getValue().getRequestedAt()).isEqualTo(LocalDateTime.ofInstant(CLOCK.instant(), ZoneOffset.UTC));
        assertThat(view.status()).isEqualTo("PENDING");
        assertThat(view.reason()).isEqualTo("需要下载本周复盘数据");
    }

    @Test
    void approvalCreatesUserResourceGrantAndRecordsGrantedPermission() {
        Fixture fixture = fixture();
        BiPermissionRequestDO pending = request(31L, "PENDING");
        when(fixture.workspaceMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(workspace());
        when(fixture.requestMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(pending);
        when(fixture.adminService.upsertResourcePermission(any(), any(), any()))
                .thenReturn(new BiResourcePermissionView(
                        99L,
                        7L,
                        5L,
                        "DASHBOARD",
                        "canvas-effect",
                        12L,
                        "USER",
                        "alice",
                        "EXPORT",
                        "ALLOW",
                        "reviewer",
                        LocalDateTime.of(2026, 6, 6, 2, 1)));

        BiPermissionRequestView view = fixture.service.reviewPermissionRequest(
                7L,
                "reviewer",
                new BiPermissionRequestReviewCommand(31L, "approved", "同意临时导出"));

        ArgumentCaptor<BiResourcePermissionCommand> grant = ArgumentCaptor.forClass(BiResourcePermissionCommand.class);
        verify(fixture.adminService).upsertResourcePermission(any(), any(), grant.capture());
        assertThat(grant.getValue()).isEqualTo(new BiResourcePermissionCommand(
                "DASHBOARD",
                "canvas-effect",
                null,
                "USER",
                "alice",
                "EXPORT",
                "ALLOW"));
        ArgumentCaptor<BiPermissionRequestDO> updated = ArgumentCaptor.forClass(BiPermissionRequestDO.class);
        verify(fixture.requestMapper).updateById(updated.capture());
        assertThat(updated.getValue().getStatus()).isEqualTo("APPROVED");
        assertThat(updated.getValue().getReviewedBy()).isEqualTo("reviewer");
        assertThat(updated.getValue().getReviewedAt()).isEqualTo(LocalDateTime.ofInstant(CLOCK.instant(), ZoneOffset.UTC));
        assertThat(updated.getValue().getReviewComment()).isEqualTo("同意临时导出");
        assertThat(updated.getValue().getGrantedPermissionId()).isEqualTo(99L);
        assertThat(view.status()).isEqualTo("APPROVED");
        assertThat(view.grantedPermissionId()).isEqualTo(99L);
    }

    @Test
    void rejectionDoesNotCreateResourceGrant() {
        Fixture fixture = fixture();
        BiPermissionRequestDO pending = request(31L, "PENDING");
        when(fixture.workspaceMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(workspace());
        when(fixture.requestMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(pending);

        BiPermissionRequestView view = fixture.service.reviewPermissionRequest(
                7L,
                "reviewer",
                new BiPermissionRequestReviewCommand(31L, "rejected", "导出范围过大"));

        verify(fixture.adminService, never()).upsertResourcePermission(any(), any(), any());
        assertThat(view.status()).isEqualTo("REJECTED");
        assertThat(view.grantedPermissionId()).isNull();
    }

    @Test
    void listPermissionRequestsFiltersByResourceAndStatus() {
        Fixture fixture = fixture();
        when(fixture.workspaceMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(workspace());
        when(fixture.requestMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(request(31L, "APPROVED")));

        List<BiPermissionRequestView> views = fixture.service.listPermissionRequests(
                7L,
                "dashboard",
                "canvas-effect",
                "approved");

        assertThat(views).singleElement().satisfies(view -> {
            assertThat(view.resourceType()).isEqualTo("DASHBOARD");
            assertThat(view.resourceKey()).isEqualTo("canvas-effect");
            assertThat(view.status()).isEqualTo("APPROVED");
        });
    }

    @Test
    void reviewRejectsNonPendingRequests() {
        Fixture fixture = fixture();
        when(fixture.workspaceMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(workspace());
        when(fixture.requestMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(request(31L, "APPROVED"));

        assertThatThrownBy(() -> fixture.service.reviewPermissionRequest(
                7L,
                "reviewer",
                new BiPermissionRequestReviewCommand(31L, "approved", "重复审批")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not pending");
    }

    private Fixture fixture() {
        BiWorkspaceMapper workspaceMapper = mock(BiWorkspaceMapper.class);
        BiPermissionRequestMapper requestMapper = mock(BiPermissionRequestMapper.class);
        BiPermissionAdminService adminService = mock(BiPermissionAdminService.class);
        return new Fixture(
                workspaceMapper,
                requestMapper,
                adminService,
                new BiPermissionRequestService(workspaceMapper, requestMapper, adminService, CLOCK));
    }

    private BiWorkspaceDO workspace() {
        BiWorkspaceDO workspace = new BiWorkspaceDO();
        workspace.setId(5L);
        workspace.setTenantId(7L);
        workspace.setWorkspaceKey("marketing_canvas");
        return workspace;
    }

    private BiPermissionRequestDO request(Long id, String status) {
        BiPermissionRequestDO row = new BiPermissionRequestDO();
        row.setId(id);
        row.setTenantId(7L);
        row.setWorkspaceId(5L);
        row.setResourceType("DASHBOARD");
        row.setResourceKey("canvas-effect");
        row.setRequestedAction("EXPORT");
        row.setRequestedBy("alice");
        row.setRequestedAt(LocalDateTime.of(2026, 6, 6, 1, 50));
        row.setReason("需要下载本周复盘数据");
        row.setStatus(status);
        return row;
    }

    private record Fixture(
            BiWorkspaceMapper workspaceMapper,
            BiPermissionRequestMapper requestMapper,
            BiPermissionAdminService adminService,
            BiPermissionRequestService service) {
    }
}
