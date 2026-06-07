package org.chovy.canvas.domain.bi.resource;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.chovy.canvas.dal.dataobject.BiDashboardDO;
import org.chovy.canvas.dal.dataobject.BiPublishApprovalDO;
import org.chovy.canvas.dal.dataobject.BiSpreadsheetDO;
import org.chovy.canvas.dal.dataobject.BiWorkspaceDO;
import org.chovy.canvas.dal.mapper.BiBigScreenMapper;
import org.chovy.canvas.dal.mapper.BiChartMapper;
import org.chovy.canvas.dal.mapper.BiDashboardMapper;
import org.chovy.canvas.dal.mapper.BiDatasetMapper;
import org.chovy.canvas.dal.mapper.BiPortalMapper;
import org.chovy.canvas.dal.mapper.BiPublishApprovalMapper;
import org.chovy.canvas.dal.mapper.BiSpreadsheetMapper;
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

class BiPublishApprovalServiceTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-06-05T05:00:00Z"), ZoneOffset.UTC);

    @Test
    void publishApprovalMigrationCapturesReviewLifecycle() throws Exception {
        String migration = Files.readString(migrationDir().resolve("V248__bi_publish_approval.sql"));

        assertThat(migration).contains("CREATE TABLE IF NOT EXISTS bi_publish_approval");
        assertThat(migration).contains("status VARCHAR(32) NOT NULL");
        assertThat(migration).contains("reviewed_by VARCHAR(128)");
        assertThat(migration).contains("review_comment VARCHAR(512)");
        assertThat(migration).contains("KEY idx_bi_publish_approval_resource");
    }

    @Test
    void requestDashboardPublishApprovalPersistsPendingReview() {
        BiWorkspaceMapper workspaceMapper = mock(BiWorkspaceMapper.class);
        BiDashboardMapper dashboardMapper = mock(BiDashboardMapper.class);
        BiPublishApprovalMapper approvalMapper = mock(BiPublishApprovalMapper.class);
        when(workspaceMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(workspace());
        when(dashboardMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(dashboard("DRAFT"));
        BiPublishApprovalService service = service(workspaceMapper, dashboardMapper, approvalMapper);

        BiPublishApprovalView view = service.requestApproval(7L, "alice",
                new BiPublishApprovalRequestCommand("dashboard", "canvas-effect", "准备发布周报看板"));

        ArgumentCaptor<BiPublishApprovalDO> captor = ArgumentCaptor.forClass(BiPublishApprovalDO.class);
        verify(approvalMapper).insert(captor.capture());
        assertThat(captor.getValue().getTenantId()).isEqualTo(7L);
        assertThat(captor.getValue().getWorkspaceId()).isEqualTo(5L);
        assertThat(captor.getValue().getResourceType()).isEqualTo("DASHBOARD");
        assertThat(captor.getValue().getResourceKey()).isEqualTo("canvas-effect");
        assertThat(captor.getValue().getStatus()).isEqualTo("PENDING");
        assertThat(captor.getValue().getRequestedBy()).isEqualTo("alice");
        assertThat(captor.getValue().getRequestedAt()).isEqualTo(LocalDateTime.ofInstant(CLOCK.instant(), ZoneOffset.UTC));
        assertThat(view.status()).isEqualTo("PENDING");
        assertThat(view.reason()).isEqualTo("准备发布周报看板");
    }

    @Test
    void requestSpreadsheetPublishApprovalPersistsPendingReview() {
        BiWorkspaceMapper workspaceMapper = mock(BiWorkspaceMapper.class);
        BiSpreadsheetMapper spreadsheetMapper = mock(BiSpreadsheetMapper.class);
        BiPublishApprovalMapper approvalMapper = mock(BiPublishApprovalMapper.class);
        BiSpreadsheetDO spreadsheet = new BiSpreadsheetDO();
        spreadsheet.setId(99L);
        spreadsheet.setStatus("DRAFT");
        when(workspaceMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(workspace());
        when(spreadsheetMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(spreadsheet);
        BiPublishApprovalService service = service(
                workspaceMapper,
                mock(BiDashboardMapper.class),
                mock(BiBigScreenMapper.class),
                spreadsheetMapper,
                approvalMapper);

        BiPublishApprovalView view = service.requestApproval(7L, "alice",
                new BiPublishApprovalRequestCommand("spreadsheet", "budget-sheet", "准备发布预算模型"));

        ArgumentCaptor<BiPublishApprovalDO> captor = ArgumentCaptor.forClass(BiPublishApprovalDO.class);
        verify(approvalMapper).insert(captor.capture());
        assertThat(captor.getValue().getTenantId()).isEqualTo(7L);
        assertThat(captor.getValue().getWorkspaceId()).isEqualTo(5L);
        assertThat(captor.getValue().getResourceType()).isEqualTo("SPREADSHEET");
        assertThat(captor.getValue().getResourceKey()).isEqualTo("budget-sheet");
        assertThat(captor.getValue().getStatus()).isEqualTo("PENDING");
        assertThat(view.resourceType()).isEqualTo("SPREADSHEET");
        assertThat(view.resourceKey()).isEqualTo("budget-sheet");
    }

    @Test
    void requestRejectsArchivedResource() {
        BiWorkspaceMapper workspaceMapper = mock(BiWorkspaceMapper.class);
        BiDashboardMapper dashboardMapper = mock(BiDashboardMapper.class);
        when(workspaceMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(workspace());
        when(dashboardMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(dashboard("ARCHIVED"));
        BiPublishApprovalService service = service(workspaceMapper, dashboardMapper, mock(BiPublishApprovalMapper.class));

        assertThatThrownBy(() -> service.requestApproval(7L, "alice",
                new BiPublishApprovalRequestCommand("dashboard", "canvas-effect", "归档资源不应发起审批")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("BI resource is archived");
    }

    @Test
    void approvingPendingRequestRecordsReviewer() {
        BiWorkspaceMapper workspaceMapper = mock(BiWorkspaceMapper.class);
        BiPublishApprovalMapper approvalMapper = mock(BiPublishApprovalMapper.class);
        BiPublishApprovalDO pending = approval(9L, "PENDING");
        when(workspaceMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(workspace());
        when(approvalMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(pending);
        BiPublishApprovalService service = service(workspaceMapper, mock(BiDashboardMapper.class), approvalMapper);

        BiPublishApprovalView view = service.reviewApproval(7L, "reviewer",
                new BiPublishApprovalReviewCommand(9L, "approved", "口径已复核"));

        ArgumentCaptor<BiPublishApprovalDO> captor = ArgumentCaptor.forClass(BiPublishApprovalDO.class);
        verify(approvalMapper).updateById(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo("APPROVED");
        assertThat(captor.getValue().getReviewedBy()).isEqualTo("reviewer");
        assertThat(captor.getValue().getReviewedAt()).isEqualTo(LocalDateTime.ofInstant(CLOCK.instant(), ZoneOffset.UTC));
        assertThat(captor.getValue().getReviewComment()).isEqualTo("口径已复核");
        assertThat(view.status()).isEqualTo("APPROVED");
    }

    @Test
    void listApprovalsFiltersByResourceAndStatus() {
        BiWorkspaceMapper workspaceMapper = mock(BiWorkspaceMapper.class);
        BiPublishApprovalMapper approvalMapper = mock(BiPublishApprovalMapper.class);
        when(workspaceMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(workspace());
        when(approvalMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(approval(9L, "APPROVED")));
        BiPublishApprovalService service = service(workspaceMapper, mock(BiDashboardMapper.class), approvalMapper);

        List<BiPublishApprovalView> approvals = service.listApprovals(7L, "dashboard", "canvas-effect", "approved");

        assertThat(approvals).singleElement().satisfies(approval -> {
            assertThat(approval.resourceType()).isEqualTo("DASHBOARD");
            assertThat(approval.resourceKey()).isEqualTo("canvas-effect");
            assertThat(approval.status()).isEqualTo("APPROVED");
        });
    }

    @Test
    void requireApprovedApprovalAcceptsFreshApprovedReview() {
        BiWorkspaceMapper workspaceMapper = mock(BiWorkspaceMapper.class);
        BiPublishApprovalMapper approvalMapper = mock(BiPublishApprovalMapper.class);
        BiPublishApprovalDO approved = approval(9L, "APPROVED");
        approved.setReviewedBy("reviewer");
        approved.setReviewedAt(LocalDateTime.of(2026, 6, 5, 5, 10));
        when(workspaceMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(workspace());
        when(approvalMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(approved);
        BiPublishApprovalService service = service(workspaceMapper, mock(BiDashboardMapper.class), approvalMapper);

        BiPublishApprovalView view = service.requireApprovedApproval(
                7L,
                5L,
                "dashboard",
                "canvas-effect",
                LocalDateTime.of(2026, 6, 5, 5, 0));

        assertThat(view.id()).isEqualTo(9L);
        assertThat(view.status()).isEqualTo("APPROVED");
        assertThat(view.reviewedBy()).isEqualTo("reviewer");
    }

    @Test
    void requireApprovedApprovalRejectsMissingOrStaleReview() {
        BiWorkspaceMapper workspaceMapper = mock(BiWorkspaceMapper.class);
        BiPublishApprovalMapper approvalMapper = mock(BiPublishApprovalMapper.class);
        BiPublishApprovalDO stale = approval(9L, "APPROVED");
        stale.setReviewedAt(LocalDateTime.of(2026, 6, 5, 4, 59));
        when(workspaceMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(workspace());
        when(approvalMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null, stale);
        BiPublishApprovalService service = service(workspaceMapper, mock(BiDashboardMapper.class), approvalMapper);

        assertThatThrownBy(() -> service.requireApprovedApproval(
                7L,
                5L,
                "dashboard",
                "canvas-effect",
                LocalDateTime.of(2026, 6, 5, 5, 0)))
                .isInstanceOf(BiPublishApprovalService.BiPublishApprovalRequiredException.class)
                .hasMessageContaining("approved BI publish approval is required");

        assertThatThrownBy(() -> service.requireApprovedApproval(
                7L,
                5L,
                "dashboard",
                "canvas-effect",
                LocalDateTime.of(2026, 6, 5, 5, 0)))
                .isInstanceOf(BiPublishApprovalService.BiPublishApprovalRequiredException.class)
                .hasMessageContaining("stale");
    }

    private BiPublishApprovalService service(BiWorkspaceMapper workspaceMapper,
                                             BiDashboardMapper dashboardMapper,
                                             BiPublishApprovalMapper approvalMapper) {
        return service(
                workspaceMapper,
                dashboardMapper,
                mock(BiBigScreenMapper.class),
                mock(BiSpreadsheetMapper.class),
                approvalMapper);
    }

    private BiPublishApprovalService service(BiWorkspaceMapper workspaceMapper,
                                             BiDashboardMapper dashboardMapper,
                                             BiBigScreenMapper bigScreenMapper,
                                             BiSpreadsheetMapper spreadsheetMapper,
                                             BiPublishApprovalMapper approvalMapper) {
        return new BiPublishApprovalService(
                workspaceMapper,
                mock(BiDatasetMapper.class),
                dashboardMapper,
                mock(BiChartMapper.class),
                mock(BiPortalMapper.class),
                bigScreenMapper,
                spreadsheetMapper,
                approvalMapper,
                CLOCK);
    }

    private BiWorkspaceDO workspace() {
        BiWorkspaceDO workspace = new BiWorkspaceDO();
        workspace.setId(5L);
        return workspace;
    }

    private static Path migrationDir() {
        Path modulePath = Path.of("src/main/resources/db/migration");
        if (Files.exists(modulePath)) {
            return modulePath;
        }
        return Path.of("canvas-engine/src/main/resources/db/migration");
    }

    private BiDashboardDO dashboard(String status) {
        BiDashboardDO dashboard = new BiDashboardDO();
        dashboard.setId(99L);
        dashboard.setStatus(status);
        return dashboard;
    }

    private BiPublishApprovalDO approval(Long id, String status) {
        BiPublishApprovalDO row = new BiPublishApprovalDO();
        row.setId(id);
        row.setTenantId(7L);
        row.setWorkspaceId(5L);
        row.setResourceType("DASHBOARD");
        row.setResourceKey("canvas-effect");
        row.setStatus(status);
        row.setReason("准备发布周报看板");
        row.setRequestedBy("alice");
        row.setRequestedAt(LocalDateTime.ofInstant(CLOCK.instant(), ZoneOffset.UTC));
        return row;
    }

}
