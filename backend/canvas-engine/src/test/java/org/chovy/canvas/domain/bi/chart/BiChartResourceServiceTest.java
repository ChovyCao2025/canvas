package org.chovy.canvas.domain.bi.chart;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.dal.dataobject.BiChartDO;
import org.chovy.canvas.dal.dataobject.BiChartVersionDO;
import org.chovy.canvas.dal.dataobject.BiDatasetDO;
import org.chovy.canvas.dal.dataobject.BiWorkspaceDO;
import org.chovy.canvas.dal.mapper.BiChartMapper;
import org.chovy.canvas.dal.mapper.BiChartVersionMapper;
import org.chovy.canvas.dal.mapper.BiDatasetMapper;
import org.chovy.canvas.dal.mapper.BiWorkspaceMapper;
import org.chovy.canvas.domain.bi.permission.BiPermissionService;
import org.chovy.canvas.domain.bi.query.BiDatasetSpecResolver;
import org.chovy.canvas.domain.bi.query.BiQueryRequest;
import org.chovy.canvas.domain.bi.query.BiSort;
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

class BiChartResourceServiceTest {

    @Test
    void saveDraftPersistsValidatedChartResource() {
        BiWorkspaceMapper workspaceMapper = mock(BiWorkspaceMapper.class);
        BiDatasetMapper datasetMapper = mock(BiDatasetMapper.class);
        BiChartMapper chartMapper = mock(BiChartMapper.class);
        when(workspaceMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(workspace());
        when(datasetMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(dataset());
        when(chartMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null, chart("DRAFT"));
        BiChartResourceService service = new BiChartResourceService(
                workspaceMapper,
                datasetMapper,
                chartMapper,
                new ObjectMapper());

        BiChartResource resource = service.saveDraft(7L, "alice", chartResource());

        ArgumentCaptor<BiChartDO> captor = ArgumentCaptor.forClass(BiChartDO.class);
        verify(chartMapper).upsert(captor.capture());
        assertThat(captor.getValue().getTenantId()).isEqualTo(7L);
        assertThat(captor.getValue().getWorkspaceId()).isEqualTo(5L);
        assertThat(captor.getValue().getDatasetId()).isEqualTo(11L);
        assertThat(captor.getValue().getChartKey()).isEqualTo("trend-executions");
        assertThat(captor.getValue().getStatus()).isEqualTo("DRAFT");
        assertThat(captor.getValue().getQueryJson()).contains("total_executions");
        assertThat(resource.status()).isEqualTo("DRAFT");
        assertThat(resource.source()).isEqualTo("PERSISTED");
        assertThat(resource.query().metrics()).containsExactly("total_executions");
    }

    @Test
    void saveDraftRejectsUnknownMetricBeforePersistence() {
        BiWorkspaceMapper workspaceMapper = mock(BiWorkspaceMapper.class);
        BiDatasetMapper datasetMapper = mock(BiDatasetMapper.class);
        BiChartMapper chartMapper = mock(BiChartMapper.class);
        when(workspaceMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(workspace());
        when(datasetMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(dataset());
        BiChartResourceService service = new BiChartResourceService(
                workspaceMapper,
                datasetMapper,
                chartMapper,
                new ObjectMapper());
        BiQueryRequest query = new BiQueryRequest(
                "canvas_daily_stats",
                List.of("stat_date"),
                List.of("missing_metric"),
                List.of(),
                List.of(),
                500);
        BiChartResource resource = new BiChartResource(
                "unsafe-query",
                "Unsafe",
                "LINE",
                "canvas_daily_stats",
                query,
                Map.of(),
                Map.of(),
                "DRAFT",
                "CLIENT");

        assertThatThrownBy(() -> service.saveDraft(7L, "alice", resource))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown metric");
    }

    @Test
    void publishAndArchiveUpdateLifecycle() {
        BiWorkspaceMapper workspaceMapper = mock(BiWorkspaceMapper.class);
        BiDatasetMapper datasetMapper = mock(BiDatasetMapper.class);
        BiChartMapper chartMapper = mock(BiChartMapper.class);
        when(workspaceMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(workspace());
        when(chartMapper.selectOne(any(LambdaQueryWrapper.class)))
                .thenReturn(chart("DRAFT"), chart("PUBLISHED"), chart("PUBLISHED"), chart("ARCHIVED"));
        BiChartResourceService service = new BiChartResourceService(
                workspaceMapper,
                datasetMapper,
                chartMapper,
                new ObjectMapper());

        BiChartResource published = service.publish(7L, "trend-executions");
        BiChartResource archived = service.archive(7L, "trend-executions");

        verify(chartMapper).publish(7L, 5L, "trend-executions");
        verify(chartMapper).archive(7L, 5L, "trend-executions");
        assertThat(published.status()).isEqualTo("PUBLISHED");
        assertThat(archived.status()).isEqualTo("ARCHIVED");
    }

    @Test
    void publishWritesVersionSnapshot() {
        BiWorkspaceMapper workspaceMapper = mock(BiWorkspaceMapper.class);
        BiDatasetMapper datasetMapper = mock(BiDatasetMapper.class);
        BiChartMapper chartMapper = mock(BiChartMapper.class);
        BiChartVersionMapper versionMapper = mock(BiChartVersionMapper.class);
        when(workspaceMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(workspace());
        when(chartMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(chart("DRAFT"), chart("PUBLISHED"));
        when(versionMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        BiChartResourceService service = new BiChartResourceService(
                workspaceMapper,
                datasetMapper,
                chartMapper,
                versionMapper,
                new ObjectMapper(),
                (BiDatasetSpecResolver) null);

        BiChartResource published = service.publish(7L, "alice", "trend-executions");

        verify(chartMapper).publish(7L, 5L, "trend-executions");
        ArgumentCaptor<BiChartVersionDO> versionCaptor = ArgumentCaptor.forClass(BiChartVersionDO.class);
        verify(versionMapper).insert(versionCaptor.capture());
        assertThat(versionCaptor.getValue().getTenantId()).isEqualTo(7L);
        assertThat(versionCaptor.getValue().getWorkspaceId()).isEqualTo(5L);
        assertThat(versionCaptor.getValue().getChartId()).isEqualTo(21L);
        assertThat(versionCaptor.getValue().getChartKey()).isEqualTo("trend-executions");
        assertThat(versionCaptor.getValue().getVersion()).isEqualTo(1);
        assertThat(versionCaptor.getValue().getStatus()).isEqualTo("PUBLISHED");
        assertThat(versionCaptor.getValue().getResourceJson()).contains("\"chartKey\":\"trend-executions\"");
        assertThat(versionCaptor.getValue().getPublishedBy()).isEqualTo("alice");
        assertThat(published.status()).isEqualTo("PUBLISHED");
    }

    @Test
    void publishRequiresPublishPermission() {
        BiWorkspaceMapper workspaceMapper = mock(BiWorkspaceMapper.class);
        BiDatasetMapper datasetMapper = mock(BiDatasetMapper.class);
        BiChartMapper chartMapper = mock(BiChartMapper.class);
        BiResourcePermissionGuard permissionGuard = mock(BiResourcePermissionGuard.class);
        when(workspaceMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(workspace());
        when(chartMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(chart("DRAFT"));
        org.mockito.Mockito.doThrow(new BiPermissionService.BiPermissionDeniedException("missing publish"))
                .when(permissionGuard)
                .require(7L, 5L, "CHART", 21L, "alice", "ANALYST", BiPermissionService.ACTION_PUBLISH);
        BiChartResourceService service = new BiChartResourceService(
                workspaceMapper,
                datasetMapper,
                chartMapper,
                null,
                new ObjectMapper(),
                (BiDatasetSpecResolver) null,
                permissionGuard);

        assertThatThrownBy(() -> service.publish(7L, "alice", "ANALYST", "trend-executions"))
                .isInstanceOf(BiPermissionService.BiPermissionDeniedException.class)
                .hasMessageContaining("missing publish");

        verify(permissionGuard).require(7L, 5L, "CHART", 21L, "alice", "ANALYST", BiPermissionService.ACTION_PUBLISH);
        verify(chartMapper, org.mockito.Mockito.never()).publish(7L, 5L, "trend-executions");
    }

    @Test
    void saveDraftRequiresCurrentEditLockForExistingChart() {
        BiWorkspaceMapper workspaceMapper = mock(BiWorkspaceMapper.class);
        BiDatasetMapper datasetMapper = mock(BiDatasetMapper.class);
        BiChartMapper chartMapper = mock(BiChartMapper.class);
        BiResourceCollaborationService collaborationService = mock(BiResourceCollaborationService.class);
        when(workspaceMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(workspace());
        when(datasetMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(dataset());
        when(chartMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(chart("DRAFT"));
        org.mockito.Mockito.doThrow(new BiResourceCollaborationService.BiResourceLockRequiredException(
                        "active BI resource lock is required"))
                .when(collaborationService)
                .requireCurrentLock(7L, 5L, "CHART", "trend-executions", "alice", "bad-token");
        BiChartResourceService service = new BiChartResourceService(
                workspaceMapper,
                datasetMapper,
                chartMapper,
                null,
                new ObjectMapper(),
                (BiDatasetSpecResolver) null,
                null,
                null,
                collaborationService);

        assertThatThrownBy(() -> service.saveDraft(
                7L, "alice", "ANALYST", chartResource(), "bad-token"))
                .isInstanceOf(BiResourceCollaborationService.BiResourceLockRequiredException.class)
                .hasMessageContaining("active BI resource lock is required");

        verify(collaborationService).requireCurrentLock(
                7L, 5L, "CHART", "trend-executions", "alice", "bad-token");
        verify(chartMapper, org.mockito.Mockito.never()).upsert(any(BiChartDO.class));
    }

    @Test
    void publishRequiresFreshApprovalForNonAdmin() {
        BiWorkspaceMapper workspaceMapper = mock(BiWorkspaceMapper.class);
        BiDatasetMapper datasetMapper = mock(BiDatasetMapper.class);
        BiChartMapper chartMapper = mock(BiChartMapper.class);
        BiPublishApprovalService approvalService = mock(BiPublishApprovalService.class);
        LocalDateTime updatedAt = LocalDateTime.of(2026, 6, 5, 12, 0);
        BiChartDO draft = chart("DRAFT");
        draft.setUpdatedAt(updatedAt);
        when(workspaceMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(workspace());
        when(chartMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(draft);
        org.mockito.Mockito.doThrow(new BiPublishApprovalService.BiPublishApprovalRequiredException(
                        "approved BI publish approval is required"))
                .when(approvalService)
                .requireApprovedApproval(7L, 5L, "CHART", "trend-executions", updatedAt);
        BiChartResourceService service = new BiChartResourceService(
                workspaceMapper,
                datasetMapper,
                chartMapper,
                null,
                new ObjectMapper(),
                (BiDatasetSpecResolver) null,
                null,
                approvalService);

        assertThatThrownBy(() -> service.publish(7L, "alice", "ANALYST", "trend-executions"))
                .isInstanceOf(BiPublishApprovalService.BiPublishApprovalRequiredException.class)
                .hasMessageContaining("approved BI publish approval is required");

        verify(approvalService).requireApprovedApproval(7L, 5L, "CHART", "trend-executions", updatedAt);
        verify(chartMapper, org.mockito.Mockito.never()).publish(7L, 5L, "trend-executions");
    }

    @Test
    void tenantAdminPublishSkipsApprovalGate() {
        BiWorkspaceMapper workspaceMapper = mock(BiWorkspaceMapper.class);
        BiDatasetMapper datasetMapper = mock(BiDatasetMapper.class);
        BiChartMapper chartMapper = mock(BiChartMapper.class);
        BiPublishApprovalService approvalService = mock(BiPublishApprovalService.class);
        when(workspaceMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(workspace());
        when(chartMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(chart("DRAFT"), chart("PUBLISHED"));
        BiChartResourceService service = new BiChartResourceService(
                workspaceMapper,
                datasetMapper,
                chartMapper,
                null,
                new ObjectMapper(),
                (BiDatasetSpecResolver) null,
                null,
                approvalService);

        BiChartResource published = service.publish(7L, "admin", "TENANT_ADMIN", "trend-executions");

        assertThat(published.status()).isEqualTo("PUBLISHED");
        verify(approvalService, org.mockito.Mockito.never()).requireApprovedApproval(
                any(), any(), any(), any(), any());
        verify(chartMapper).publish(7L, 5L, "trend-executions");
    }

    @Test
    void listVersionsReturnsPublishedSnapshots() throws Exception {
        BiWorkspaceMapper workspaceMapper = mock(BiWorkspaceMapper.class);
        BiDatasetMapper datasetMapper = mock(BiDatasetMapper.class);
        BiChartMapper chartMapper = mock(BiChartMapper.class);
        BiChartVersionMapper versionMapper = mock(BiChartVersionMapper.class);
        ObjectMapper objectMapper = new ObjectMapper();
        BiChartVersionDO snapshot = new BiChartVersionDO();
        snapshot.setId(88L);
        snapshot.setTenantId(7L);
        snapshot.setWorkspaceId(5L);
        snapshot.setChartId(21L);
        snapshot.setChartKey("trend-executions");
        snapshot.setVersion(2);
        snapshot.setStatus("PUBLISHED");
        snapshot.setResourceJson(objectMapper.writeValueAsString(chartResource()));
        snapshot.setPublishedBy("alice");
        when(workspaceMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(workspace());
        when(chartMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(chart("PUBLISHED"));
        when(versionMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(snapshot));
        BiChartResourceService service = new BiChartResourceService(
                workspaceMapper,
                datasetMapper,
                chartMapper,
                versionMapper,
                objectMapper,
                (BiDatasetSpecResolver) null);

        List<BiChartVersionView> versions = service.listVersions(7L, "trend-executions", 10);

        assertThat(versions).singleElement().satisfies(version -> {
            assertThat(version.id()).isEqualTo(88L);
            assertThat(version.chartKey()).isEqualTo("trend-executions");
            assertThat(version.version()).isEqualTo(2);
            assertThat(version.resource().chartType()).isEqualTo("LINE");
            assertThat(version.publishedBy()).isEqualTo("alice");
        });
    }

    @Test
    void restoreVersionWritesSnapshotBackAsDraft() throws Exception {
        BiWorkspaceMapper workspaceMapper = mock(BiWorkspaceMapper.class);
        BiDatasetMapper datasetMapper = mock(BiDatasetMapper.class);
        BiChartMapper chartMapper = mock(BiChartMapper.class);
        BiChartVersionMapper versionMapper = mock(BiChartVersionMapper.class);
        ObjectMapper objectMapper = new ObjectMapper();
        BiChartVersionDO snapshot = new BiChartVersionDO();
        snapshot.setId(88L);
        snapshot.setTenantId(7L);
        snapshot.setWorkspaceId(5L);
        snapshot.setChartId(21L);
        snapshot.setChartKey("trend-executions");
        snapshot.setVersion(2);
        snapshot.setStatus("PUBLISHED");
        snapshot.setResourceJson(objectMapper.writeValueAsString(chartResource()));
        when(workspaceMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(workspace());
        when(chartMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(chart("PUBLISHED"), chart("DRAFT"));
        when(versionMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(snapshot);
        when(datasetMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(dataset());
        BiChartResourceService service = new BiChartResourceService(
                workspaceMapper,
                datasetMapper,
                chartMapper,
                versionMapper,
                objectMapper,
                (BiDatasetSpecResolver) null);

        BiChartResource resource = service.restoreVersion(7L, "alice", "trend-executions", 2);

        ArgumentCaptor<BiChartDO> chartCaptor = ArgumentCaptor.forClass(BiChartDO.class);
        verify(chartMapper).upsert(chartCaptor.capture());
        assertThat(chartCaptor.getValue().getChartKey()).isEqualTo("trend-executions");
        assertThat(chartCaptor.getValue().getStatus()).isEqualTo("DRAFT");
        assertThat(chartCaptor.getValue().getQueryJson()).contains("total_executions");
        assertThat(resource.status()).isEqualTo("DRAFT");
    }

    private BiWorkspaceDO workspace() {
        BiWorkspaceDO row = new BiWorkspaceDO();
        row.setId(5L);
        row.setTenantId(0L);
        row.setWorkspaceKey("marketing_canvas");
        return row;
    }

    private BiDatasetDO dataset() {
        BiDatasetDO row = new BiDatasetDO();
        row.setId(11L);
        row.setTenantId(0L);
        row.setWorkspaceId(5L);
        row.setDatasetKey("canvas_daily_stats");
        return row;
    }

    private BiChartResource chartResource() {
        BiQueryRequest query = new BiQueryRequest(
                "canvas_daily_stats",
                List.of("stat_date"),
                List.of("total_executions"),
                List.of(),
                List.of(new BiSort("stat_date", BiSort.Direction.ASC)),
                500);
        return new BiChartResource(
                "trend-executions",
                "Execution Trend",
                "LINE",
                "canvas_daily_stats",
                query,
                Map.of("palette", "workbench"),
                Map.of("click", "FILTER_LINKAGE"),
                "DRAFT",
                "CLIENT");
    }

    private BiChartDO chart(String status) {
        BiChartDO row = new BiChartDO();
        row.setId(21L);
        row.setTenantId(7L);
        row.setWorkspaceId(5L);
        row.setChartKey("trend-executions");
        row.setName("Execution Trend");
        row.setChartType("LINE");
        row.setDatasetId(11L);
        row.setQueryJson("""
                {"datasetKey":"canvas_daily_stats","dimensions":["stat_date"],"metrics":["total_executions"],"filters":[],"sorts":[{"field":"stat_date","direction":"ASC"}],"limit":500}
                """);
        row.setStyleJson("{\"palette\":\"workbench\"}");
        row.setInteractionJson("{\"click\":\"FILTER_LINKAGE\"}");
        row.setStatus(status);
        return row;
    }
}
