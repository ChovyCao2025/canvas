package org.chovy.canvas.domain.bi.resource;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.chovy.canvas.dal.dataobject.BiDashboardDO;
import org.chovy.canvas.dal.dataobject.BiResourceCommentDO;
import org.chovy.canvas.dal.dataobject.BiResourceLockDO;
import org.chovy.canvas.dal.dataobject.BiWorkspaceDO;
import org.chovy.canvas.dal.mapper.BiChartMapper;
import org.chovy.canvas.dal.mapper.BiDashboardMapper;
import org.chovy.canvas.dal.mapper.BiDatasetMapper;
import org.chovy.canvas.dal.mapper.BiPortalMapper;
import org.chovy.canvas.dal.mapper.BiResourceCommentMapper;
import org.chovy.canvas.dal.mapper.BiResourceLockMapper;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BiResourceCollaborationServiceTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-06-05T04:00:00Z"), ZoneOffset.UTC);

    @Test
    void collaborationMigrationCreatesCommentAndLockTables() throws Exception {
        String migration = Files.readString(Path.of(
                "src/main/resources/db/migration/V246__bi_resource_collaboration.sql"));

        assertThat(migration).contains("CREATE TABLE IF NOT EXISTS bi_resource_comment");
        assertThat(migration).contains("widget_key VARCHAR(128)");
        assertThat(migration).contains("deleted_at DATETIME");
        assertThat(migration).contains("CREATE TABLE IF NOT EXISTS bi_resource_lock");
        assertThat(migration).contains("UNIQUE KEY uk_bi_resource_lock");
    }

    @Test
    void addCommentPersistsWidgetScopedComment() {
        BiWorkspaceMapper workspaceMapper = mock(BiWorkspaceMapper.class);
        BiDashboardMapper dashboardMapper = mock(BiDashboardMapper.class);
        BiResourceCommentMapper commentMapper = mock(BiResourceCommentMapper.class);
        BiWorkspaceDO workspace = workspace();
        BiDashboardDO dashboard = dashboard("PUBLISHED");
        when(workspaceMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(workspace);
        when(dashboardMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(dashboard);
        BiResourceCollaborationService service = service(
                workspaceMapper,
                dashboardMapper,
                commentMapper,
                mock(BiResourceLockMapper.class));

        BiResourceCommentView comment = service.addComment(7L, "alice",
                new BiResourceCommentCommand("dashboard", "canvas-effect", "kpi-total", "本周转化率需要复核"));

        ArgumentCaptor<BiResourceCommentDO> captor = ArgumentCaptor.forClass(BiResourceCommentDO.class);
        verify(commentMapper).insert(captor.capture());
        assertThat(captor.getValue().getTenantId()).isEqualTo(7L);
        assertThat(captor.getValue().getWorkspaceId()).isEqualTo(5L);
        assertThat(captor.getValue().getResourceType()).isEqualTo("DASHBOARD");
        assertThat(captor.getValue().getResourceKey()).isEqualTo("canvas-effect");
        assertThat(captor.getValue().getWidgetKey()).isEqualTo("kpi-total");
        assertThat(captor.getValue().getCommentText()).isEqualTo("本周转化率需要复核");
        assertThat(captor.getValue().getCreatedBy()).isEqualTo("alice");
        assertThat(comment.resourceType()).isEqualTo("DASHBOARD");
        assertThat(comment.widgetKey()).isEqualTo("kpi-total");
        assertThat(comment.commentText()).isEqualTo("本周转化率需要复核");
    }

    @Test
    void listCommentsReturnsCurrentResourceRows() {
        BiWorkspaceMapper workspaceMapper = mock(BiWorkspaceMapper.class);
        BiResourceCommentMapper commentMapper = mock(BiResourceCommentMapper.class);
        BiWorkspaceDO workspace = workspace();
        BiResourceCommentDO row = new BiResourceCommentDO();
        row.setId(12L);
        row.setTenantId(7L);
        row.setWorkspaceId(5L);
        row.setResourceType("DASHBOARD");
        row.setResourceKey("canvas-effect");
        row.setWidgetKey("kpi-total");
        row.setCommentText("已确认口径");
        row.setCreatedBy("alice");
        when(workspaceMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(workspace);
        when(commentMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(row));
        BiResourceCollaborationService service = service(
                workspaceMapper,
                mock(BiDashboardMapper.class),
                commentMapper,
                mock(BiResourceLockMapper.class));

        List<BiResourceCommentView> comments = service.listComments(7L, "dashboard", "canvas-effect");

        assertThat(comments).singleElement().satisfies(comment -> {
            assertThat(comment.workspaceId()).isEqualTo(5L);
            assertThat(comment.resourceType()).isEqualTo("DASHBOARD");
            assertThat(comment.resourceKey()).isEqualTo("canvas-effect");
            assertThat(comment.commentText()).isEqualTo("已确认口径");
        });
    }

    @Test
    void commentRejectsArchivedResource() {
        BiWorkspaceMapper workspaceMapper = mock(BiWorkspaceMapper.class);
        BiDashboardMapper dashboardMapper = mock(BiDashboardMapper.class);
        when(workspaceMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(workspace());
        when(dashboardMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(dashboard("ARCHIVED"));
        BiResourceCollaborationService service = service(
                workspaceMapper,
                dashboardMapper,
                mock(BiResourceCommentMapper.class),
                mock(BiResourceLockMapper.class));

        assertThatThrownBy(() -> service.addComment(7L, "alice",
                new BiResourceCommentCommand("dashboard", "canvas-effect", null, "归档资源不应评论")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("BI resource is archived");
    }

    @Test
    void acquireLockPersistsTokenAndExpiry() {
        BiWorkspaceMapper workspaceMapper = mock(BiWorkspaceMapper.class);
        BiDashboardMapper dashboardMapper = mock(BiDashboardMapper.class);
        BiResourceLockMapper lockMapper = mock(BiResourceLockMapper.class);
        when(workspaceMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(workspace());
        when(dashboardMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(dashboard("DRAFT"));
        when(lockMapper.acquire(any(BiResourceLockDO.class))).thenReturn(1);
        BiResourceCollaborationService service = service(
                workspaceMapper,
                dashboardMapper,
                mock(BiResourceCommentMapper.class),
                lockMapper);

        BiResourceLockView lock = service.acquireLock(7L, "alice",
                new BiResourceLockCommand("dashboard", "canvas-effect", "token-1", 120));

        ArgumentCaptor<BiResourceLockDO> captor = ArgumentCaptor.forClass(BiResourceLockDO.class);
        verify(lockMapper).acquire(captor.capture());
        assertThat(captor.getValue().getTenantId()).isEqualTo(7L);
        assertThat(captor.getValue().getWorkspaceId()).isEqualTo(5L);
        assertThat(captor.getValue().getLockToken()).isEqualTo("token-1");
        assertThat(captor.getValue().getLockedBy()).isEqualTo("alice");
        assertThat(captor.getValue().getLockedAt()).isEqualTo(LocalDateTime.ofInstant(CLOCK.instant(), ZoneOffset.UTC));
        assertThat(captor.getValue().getExpiresAt()).isEqualTo(LocalDateTime.ofInstant(CLOCK.instant(), ZoneOffset.UTC).plusSeconds(120));
        assertThat(lock.locked()).isTrue();
    }

    @Test
    void acquireLockRejectsActiveForeignLock() {
        BiWorkspaceMapper workspaceMapper = mock(BiWorkspaceMapper.class);
        BiDashboardMapper dashboardMapper = mock(BiDashboardMapper.class);
        BiResourceLockMapper lockMapper = mock(BiResourceLockMapper.class);
        BiResourceLockDO current = new BiResourceLockDO();
        current.setResourceType("DASHBOARD");
        current.setResourceKey("canvas-effect");
        current.setLockToken("bob-token");
        current.setLockedBy("bob");
        current.setLockedAt(LocalDateTime.ofInstant(CLOCK.instant(), ZoneOffset.UTC).minusSeconds(30));
        current.setExpiresAt(LocalDateTime.ofInstant(CLOCK.instant(), ZoneOffset.UTC).plusSeconds(90));
        when(workspaceMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(workspace());
        when(dashboardMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(dashboard("DRAFT"));
        when(lockMapper.acquire(any(BiResourceLockDO.class))).thenReturn(0);
        when(lockMapper.selectCurrent(7L, 5L, "DASHBOARD", "canvas-effect")).thenReturn(current);
        BiResourceCollaborationService service = service(
                workspaceMapper,
                dashboardMapper,
                mock(BiResourceCommentMapper.class),
                lockMapper);

        assertThatThrownBy(() -> service.acquireLock(7L, "alice",
                new BiResourceLockCommand("dashboard", "canvas-effect", "alice-token", 120)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("BI resource is locked by bob");
    }

    @Test
    void requireCurrentLockRejectsMissingOrMismatchedToken() {
        BiWorkspaceMapper workspaceMapper = mock(BiWorkspaceMapper.class);
        BiResourceLockMapper lockMapper = mock(BiResourceLockMapper.class);
        BiResourceLockDO current = new BiResourceLockDO();
        current.setResourceType("DASHBOARD");
        current.setResourceKey("canvas-effect");
        current.setLockToken("alice-token");
        current.setLockedBy("alice");
        current.setLockedAt(LocalDateTime.ofInstant(CLOCK.instant(), ZoneOffset.UTC).minusSeconds(30));
        current.setExpiresAt(LocalDateTime.ofInstant(CLOCK.instant(), ZoneOffset.UTC).plusSeconds(90));
        when(workspaceMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(workspace());
        when(lockMapper.selectCurrent(7L, 5L, "DASHBOARD", "canvas-effect")).thenReturn(null, current, current);
        BiResourceCollaborationService service = service(
                workspaceMapper,
                mock(BiDashboardMapper.class),
                mock(BiResourceCommentMapper.class),
                lockMapper);

        assertThatThrownBy(() -> service.requireCurrentLock(
                7L, 5L, "dashboard", "canvas-effect", "alice", "alice-token"))
                .isInstanceOf(BiResourceCollaborationService.BiResourceLockRequiredException.class)
                .hasMessageContaining("active BI resource lock is required");

        assertThatThrownBy(() -> service.requireCurrentLock(
                7L, 5L, "dashboard", "canvas-effect", "alice", "wrong-token"))
                .isInstanceOf(BiResourceCollaborationService.BiResourceLockRequiredException.class)
                .hasMessageContaining("lock token does not match");

        BiResourceLockView accepted = service.requireCurrentLock(
                7L, 5L, "dashboard", "canvas-effect", "alice", "alice-token");

        assertThat(accepted.locked()).isTrue();
        assertThat(accepted.lockedBy()).isEqualTo("alice");
    }

    @Test
    void releaseLockDeletesOnlyMatchingToken() {
        BiWorkspaceMapper workspaceMapper = mock(BiWorkspaceMapper.class);
        BiResourceLockMapper lockMapper = mock(BiResourceLockMapper.class);
        when(workspaceMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(workspace());
        BiResourceCollaborationService service = service(
                workspaceMapper,
                mock(BiDashboardMapper.class),
                mock(BiResourceCommentMapper.class),
                lockMapper);

        service.releaseLock(7L, "alice",
                new BiResourceLockCommand("dashboard", "canvas-effect", "alice-token", null));

        verify(lockMapper).release(7L, 5L, "DASHBOARD", "canvas-effect", "alice-token", "alice");
    }

    private BiResourceCollaborationService service(BiWorkspaceMapper workspaceMapper,
                                                   BiDashboardMapper dashboardMapper,
                                                   BiResourceCommentMapper commentMapper,
                                                   BiResourceLockMapper lockMapper) {
        return new BiResourceCollaborationService(
                workspaceMapper,
                mock(BiDatasetMapper.class),
                dashboardMapper,
                mock(BiChartMapper.class),
                mock(BiPortalMapper.class),
                commentMapper,
                lockMapper,
                CLOCK);
    }

    private BiWorkspaceDO workspace() {
        BiWorkspaceDO workspace = new BiWorkspaceDO();
        workspace.setId(5L);
        return workspace;
    }

    private BiDashboardDO dashboard(String status) {
        BiDashboardDO dashboard = new BiDashboardDO();
        dashboard.setId(99L);
        dashboard.setStatus(status);
        return dashboard;
    }
}
