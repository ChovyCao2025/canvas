package org.chovy.canvas.domain.bi.dataset;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.dal.dataobject.BiDatasetDO;
import org.chovy.canvas.dal.dataobject.BiDatasetFieldDO;
import org.chovy.canvas.dal.dataobject.BiDatasetVersionDO;
import org.chovy.canvas.dal.dataobject.BiMetricDO;
import org.chovy.canvas.dal.dataobject.BiWorkspaceDO;
import org.chovy.canvas.dal.mapper.BiDatasetFieldMapper;
import org.chovy.canvas.dal.mapper.BiDatasetMapper;
import org.chovy.canvas.dal.mapper.BiDatasetVersionMapper;
import org.chovy.canvas.dal.mapper.BiMetricMapper;
import org.chovy.canvas.dal.mapper.BiWorkspaceMapper;
import org.chovy.canvas.domain.bi.permission.BiPermissionService;
import org.chovy.canvas.domain.bi.query.BiDatasetSpec;
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

class BiDatasetResourceServiceTest {

    @Test
    void saveDraftPersistsDatasetFieldsAndMetrics() {
        BiWorkspaceMapper workspaceMapper = mock(BiWorkspaceMapper.class);
        BiDatasetMapper datasetMapper = mock(BiDatasetMapper.class);
        BiDatasetFieldMapper fieldMapper = mock(BiDatasetFieldMapper.class);
        BiMetricMapper metricMapper = mock(BiMetricMapper.class);
        when(workspaceMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(workspace());
        when(datasetMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(dataset("DRAFT"));
        BiDatasetResourceService service = service(workspaceMapper, datasetMapper, fieldMapper, metricMapper);

        BiDatasetResource resource = service.saveDraft(7L, "alice", datasetResource());

        ArgumentCaptor<BiDatasetDO> datasetCaptor = ArgumentCaptor.forClass(BiDatasetDO.class);
        verify(datasetMapper).upsert(datasetCaptor.capture());
        assertThat(datasetCaptor.getValue().getTenantId()).isEqualTo(7L);
        assertThat(datasetCaptor.getValue().getWorkspaceId()).isEqualTo(5L);
        assertThat(datasetCaptor.getValue().getDatasetKey()).isEqualTo("channel_performance_daily");
        assertThat(datasetCaptor.getValue().getStatus()).isEqualTo("DRAFT");
        verify(fieldMapper).deleteByDataset(7L, 11L);
        verify(metricMapper).deleteByDataset(7L, 11L);
        verify(fieldMapper, org.mockito.Mockito.times(2)).insert(any(BiDatasetFieldDO.class));
        verify(metricMapper).insert(any(BiMetricDO.class));
        assertThat(resource.status()).isEqualTo("DRAFT");
        assertThat(resource.fields()).hasSize(2);
        assertThat(resource.metrics()).singleElement()
                .extracting(BiMetricResource::metricKey)
                .isEqualTo("send_count");
    }

    @Test
    void saveDraftRejectsUnsafeTableExpression() {
        BiDatasetResourceService service = service(
                mock(BiWorkspaceMapper.class),
                mock(BiDatasetMapper.class),
                mock(BiDatasetFieldMapper.class),
                mock(BiMetricMapper.class));
        BiDatasetResource resource = new BiDatasetResource(
                "unsafe_dataset",
                "Unsafe Dataset",
                "TABLE",
                "canvas_dws.safe_table;drop table users",
                "tenant_id",
                Map.of(),
                datasetResource().fields(),
                datasetResource().metrics(),
                "DRAFT",
                "CLIENT");

        assertThatThrownBy(() -> service.saveDraft(7L, "alice", resource))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("tableExpression must be a qualified table name");
    }

    @Test
    void saveDraftAcceptsSqlDatasetWithReadOnlyLintedSource() {
        BiWorkspaceMapper workspaceMapper = mock(BiWorkspaceMapper.class);
        BiDatasetMapper datasetMapper = mock(BiDatasetMapper.class);
        BiDatasetFieldMapper fieldMapper = mock(BiDatasetFieldMapper.class);
        BiMetricMapper metricMapper = mock(BiMetricMapper.class);
        when(workspaceMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(workspace());
        when(datasetMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(sqlDataset("DRAFT"));
        BiDatasetResourceService service = service(workspaceMapper, datasetMapper, fieldMapper, metricMapper);

        BiDatasetResource resource = service.saveDraft(7L, "alice", sqlDatasetResource());

        ArgumentCaptor<BiDatasetDO> datasetCaptor = ArgumentCaptor.forClass(BiDatasetDO.class);
        verify(datasetMapper).upsert(datasetCaptor.capture());
        assertThat(datasetCaptor.getValue().getDatasetType()).isEqualTo("SQL");
        assertThat(datasetCaptor.getValue().getTableExpression())
                .isEqualTo("(SELECT tenant_id, stat_date, total_cost FROM campaign_daily WHERE deleted = 0) sql_dataset");
        assertThat(datasetCaptor.getValue().getModelJson()).contains("\"sqlApprovalRequired\":true");
        assertThat(resource.datasetType()).isEqualTo("SQL");
        assertThat(resource.tableExpression())
                .isEqualTo("(SELECT tenant_id, stat_date, total_cost FROM campaign_daily WHERE deleted = 0) sql_dataset");
    }

    @Test
    void saveDraftAcceptsSqlDatasetWithDerivedTableSource() {
        BiWorkspaceMapper workspaceMapper = mock(BiWorkspaceMapper.class);
        BiDatasetMapper datasetMapper = mock(BiDatasetMapper.class);
        BiDatasetFieldMapper fieldMapper = mock(BiDatasetFieldMapper.class);
        BiMetricMapper metricMapper = mock(BiMetricMapper.class);
        when(workspaceMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(workspace());
        when(datasetMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(sqlDataset("DRAFT"));
        BiDatasetResourceService service = service(workspaceMapper, datasetMapper, fieldMapper, metricMapper);
        BiDatasetResource sql = sqlDatasetResource(
                "SELECT tenant_id, stat_date, total_cost FROM "
                        + "(SELECT tenant_id, stat_date, total_cost FROM campaign_daily WHERE deleted = 0) daily "
                        + "WHERE total_cost >= 0");

        service.saveDraft(7L, "alice", sql);

        ArgumentCaptor<BiDatasetDO> datasetCaptor = ArgumentCaptor.forClass(BiDatasetDO.class);
        verify(datasetMapper).upsert(datasetCaptor.capture());
        assertThat(datasetCaptor.getValue().getTableExpression())
                .isEqualTo("(SELECT tenant_id, stat_date, total_cost FROM "
                        + "(SELECT tenant_id, stat_date, total_cost FROM campaign_daily WHERE deleted = 0) daily "
                        + "WHERE total_cost >= 0) sql_dataset");
    }

    @Test
    void saveDraftAcceptsSqlDatasetWithQuotedSourceIdentifier() {
        BiWorkspaceMapper workspaceMapper = mock(BiWorkspaceMapper.class);
        BiDatasetMapper datasetMapper = mock(BiDatasetMapper.class);
        BiDatasetFieldMapper fieldMapper = mock(BiDatasetFieldMapper.class);
        BiMetricMapper metricMapper = mock(BiMetricMapper.class);
        when(workspaceMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(workspace());
        when(datasetMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(sqlDataset("DRAFT"));
        BiDatasetResourceService service = service(workspaceMapper, datasetMapper, fieldMapper, metricMapper);
        BiDatasetResource sql = sqlDatasetResource(
                "SELECT \"tenant_id\" AS tenant_id, \"stat_date\" AS stat_date, \"total_cost\" AS total_cost "
                        + "FROM \"campaign daily\" WHERE \"deleted\" = 0");

        service.saveDraft(7L, "alice", sql);

        ArgumentCaptor<BiDatasetDO> datasetCaptor = ArgumentCaptor.forClass(BiDatasetDO.class);
        verify(datasetMapper).upsert(datasetCaptor.capture());
        assertThat(datasetCaptor.getValue().getTableExpression())
                .isEqualTo("(SELECT \"tenant_id\" AS tenant_id, \"stat_date\" AS stat_date, "
                        + "\"total_cost\" AS total_cost FROM \"campaign daily\" WHERE \"deleted\" = 0) sql_dataset");
    }

    @Test
    void saveDraftAcceptsSqlDatasetWithBoundParametersAndStoresTemplateMetadata() {
        BiWorkspaceMapper workspaceMapper = mock(BiWorkspaceMapper.class);
        BiDatasetMapper datasetMapper = mock(BiDatasetMapper.class);
        BiDatasetFieldMapper fieldMapper = mock(BiDatasetFieldMapper.class);
        BiMetricMapper metricMapper = mock(BiMetricMapper.class);
        BiDatasetDO persisted = sqlDataset("DRAFT");
        persisted.setTableExpression("(SELECT tenant_id, stat_date, total_cost FROM campaign_daily WHERE stat_date >= ? AND channel = ?) sql_dataset");
        when(workspaceMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(workspace());
        when(datasetMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(sqlDataset("DRAFT"), persisted);
        BiDatasetResourceService service = service(workspaceMapper, datasetMapper, fieldMapper, metricMapper);

        BiDatasetResource resource = service.saveDraft(7L, "alice", parameterizedSqlDatasetResource());

        ArgumentCaptor<BiDatasetDO> datasetCaptor = ArgumentCaptor.forClass(BiDatasetDO.class);
        verify(datasetMapper).upsert(datasetCaptor.capture());
        assertThat(datasetCaptor.getValue().getTableExpression())
                .isEqualTo("(SELECT tenant_id, stat_date, total_cost FROM campaign_daily WHERE stat_date >= ? AND channel = ?) sql_dataset");
        assertThat(datasetCaptor.getValue().getModelJson())
                .contains("\"sqlApprovalRequired\":true")
                .contains("\"sqlTemplate\":\"SELECT tenant_id, stat_date, total_cost FROM campaign_daily WHERE stat_date >= {{start_date}} AND channel = {{channel}}\"")
                .contains("\"sqlParameterOrder\":[\"start_date\",\"channel\"]")
                .contains("\"key\":\"start_date\"")
                .contains("\"allowedValues\":[\"PAID\",\"EMAIL\"]");
        assertThat(resource.tableExpression())
                .isEqualTo("(SELECT tenant_id, stat_date, total_cost FROM campaign_daily WHERE stat_date >= ? AND channel = ?) sql_dataset");
    }

    @Test
    void saveDraftRejectsSqlDatasetWithUnboundTemplateParameter() {
        BiDatasetResourceService service = service(
                mock(BiWorkspaceMapper.class),
                mock(BiDatasetMapper.class),
                mock(BiDatasetFieldMapper.class),
                mock(BiMetricMapper.class));
        BiDatasetResource resource = new BiDatasetResource(
                "campaign_sql",
                "Campaign SQL",
                "SQL",
                "SELECT tenant_id, stat_date FROM campaign_daily WHERE stat_date >= {{start_date}}",
                "tenant_id",
                Map.of("sqlParameters", List.of()),
                sqlDatasetResource().fields(),
                sqlDatasetResource().metrics(),
                "DRAFT",
                "CLIENT");

        assertThatThrownBy(() -> service.saveDraft(7L, "alice", resource))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("SQL parameter definition is required: start_date");
    }

    @Test
    void saveDraftRejectsSqlDatasetWithUnsafeStatement() {
        BiDatasetResourceService service = service(
                mock(BiWorkspaceMapper.class),
                mock(BiDatasetMapper.class),
                mock(BiDatasetFieldMapper.class),
                mock(BiMetricMapper.class));
        BiDatasetResource resource = new BiDatasetResource(
                "campaign_sql",
                "Campaign SQL",
                "SQL",
                "SELECT tenant_id, stat_date FROM campaign_daily; DROP TABLE users",
                "tenant_id",
                Map.of(),
                sqlDatasetResource().fields(),
                sqlDatasetResource().metrics(),
                "DRAFT",
                "CLIENT");

        assertThatThrownBy(() -> service.saveDraft(7L, "alice", resource))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("SQL dataset query must be a single read-only SELECT");
    }

    @Test
    void saveDraftRejectsSqlDatasetWithoutSourceTable() {
        BiDatasetResourceService service = service(
                mock(BiWorkspaceMapper.class),
                mock(BiDatasetMapper.class),
                mock(BiDatasetFieldMapper.class),
                mock(BiMetricMapper.class));
        BiDatasetResource resource = new BiDatasetResource(
                "campaign_sql",
                "Campaign SQL",
                "SQL",
                "SELECT tenant_id, 1 AS total_cost",
                "tenant_id",
                Map.of(),
                sqlDatasetResource().fields(),
                sqlDatasetResource().metrics(),
                "DRAFT",
                "CLIENT");

        assertThatThrownBy(() -> service.saveDraft(7L, "alice", resource))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("SQL dataset query must include a FROM source");
    }

    @Test
    void resolvesPersistedDatasetSpecForQueryCompiler() {
        BiWorkspaceMapper workspaceMapper = mock(BiWorkspaceMapper.class);
        BiDatasetMapper datasetMapper = mock(BiDatasetMapper.class);
        BiDatasetFieldMapper fieldMapper = mock(BiDatasetFieldMapper.class);
        BiMetricMapper metricMapper = mock(BiMetricMapper.class);
        when(workspaceMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(workspace());
        when(datasetMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(dataset("PUBLISHED"));
        when(fieldMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(field("stat_date", "DIMENSION"), field("send_count", "MEASURE")));
        when(metricMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(metric("send_count", "SUM(send_count)")));
        BiDatasetResourceService service = service(workspaceMapper, datasetMapper, fieldMapper, metricMapper);

        BiDatasetSpec spec = service.dataset("channel_performance_daily", 7L);

        assertThat(spec.datasetKey()).isEqualTo("channel_performance_daily");
        assertThat(spec.tableExpression()).isEqualTo("canvas_dws.channel_performance_daily");
        assertThat(spec.fields()).containsKeys("stat_date", "send_count");
        assertThat(spec.metrics()).containsKey("send_count");
        assertThat(spec.metrics().get("send_count").allowedDimensions()).containsExactly("stat_date");
        assertThat(spec.model()).containsEntry("category", "CHANNEL");
    }

    @Test
    void builtInResourcePreservesMetricAllowedDimensions() {
        BiDatasetResourceService service = service(
                mock(BiWorkspaceMapper.class),
                mock(BiDatasetMapper.class),
                mock(BiDatasetFieldMapper.class),
                mock(BiMetricMapper.class));

        BiDatasetResource resource = service.getResource(7L, "canvas_daily_stats");

        assertThat(resource.source()).isEqualTo("PRESET");
        assertThat(resource.metrics()).filteredOn(metric -> "total_executions".equals(metric.metricKey()))
                .singleElement()
                .satisfies(metric -> assertThat(metric.allowedDimensions())
                        .contains("stat_date", "canvas_id", "canvas_name", "trigger_type"));
    }

    @Test
    void publishAndArchiveUpdateLifecycle() {
        BiWorkspaceMapper workspaceMapper = mock(BiWorkspaceMapper.class);
        BiDatasetMapper datasetMapper = mock(BiDatasetMapper.class);
        BiDatasetFieldMapper fieldMapper = mock(BiDatasetFieldMapper.class);
        BiMetricMapper metricMapper = mock(BiMetricMapper.class);
        when(workspaceMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(workspace());
        when(datasetMapper.selectOne(any(LambdaQueryWrapper.class)))
                .thenReturn(dataset("DRAFT"), dataset("PUBLISHED"), dataset("PUBLISHED"), dataset("ARCHIVED"));
        when(fieldMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(field("stat_date", "DIMENSION")));
        when(metricMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(metric("send_count", "SUM(send_count)")));
        BiDatasetResourceService service = service(workspaceMapper, datasetMapper, fieldMapper, metricMapper);

        BiDatasetResource published = service.publish(7L, "channel_performance_daily");
        BiDatasetResource archived = service.archive(7L, "channel_performance_daily");

        verify(datasetMapper).publish(7L, 5L, "channel_performance_daily");
        verify(datasetMapper).archive(7L, 5L, "channel_performance_daily");
        assertThat(published.status()).isEqualTo("PUBLISHED");
        assertThat(archived.status()).isEqualTo("ARCHIVED");
    }

    @Test
    void publishInsertsDatasetVersionSnapshot() {
        BiWorkspaceMapper workspaceMapper = mock(BiWorkspaceMapper.class);
        BiDatasetMapper datasetMapper = mock(BiDatasetMapper.class);
        BiDatasetFieldMapper fieldMapper = mock(BiDatasetFieldMapper.class);
        BiMetricMapper metricMapper = mock(BiMetricMapper.class);
        BiDatasetVersionMapper versionMapper = mock(BiDatasetVersionMapper.class);
        when(workspaceMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(workspace());
        when(datasetMapper.selectOne(any(LambdaQueryWrapper.class)))
                .thenReturn(dataset("DRAFT"), dataset("PUBLISHED"));
        when(fieldMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(field("stat_date", "DIMENSION")));
        when(metricMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(metric("send_count", "SUM(send_count)")));
        when(versionMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        BiDatasetResourceService service =
                service(workspaceMapper, datasetMapper, fieldMapper, metricMapper, versionMapper);

        BiDatasetResource published = service.publish(7L, "alice", "channel_performance_daily");

        ArgumentCaptor<BiDatasetVersionDO> versionCaptor = ArgumentCaptor.forClass(BiDatasetVersionDO.class);
        verify(versionMapper).insert(versionCaptor.capture());
        assertThat(published.status()).isEqualTo("PUBLISHED");
        assertThat(versionCaptor.getValue().getTenantId()).isEqualTo(7L);
        assertThat(versionCaptor.getValue().getWorkspaceId()).isEqualTo(5L);
        assertThat(versionCaptor.getValue().getDatasetId()).isEqualTo(11L);
        assertThat(versionCaptor.getValue().getDatasetKey()).isEqualTo("channel_performance_daily");
        assertThat(versionCaptor.getValue().getVersion()).isEqualTo(1);
        assertThat(versionCaptor.getValue().getPublishedBy()).isEqualTo("alice");
        assertThat(versionCaptor.getValue().getResourceJson()).contains("\"datasetKey\":\"channel_performance_daily\"");
    }

    @Test
    void saveDraftRequiresEditPermissionForExistingDataset() {
        BiWorkspaceMapper workspaceMapper = mock(BiWorkspaceMapper.class);
        BiDatasetMapper datasetMapper = mock(BiDatasetMapper.class);
        BiDatasetFieldMapper fieldMapper = mock(BiDatasetFieldMapper.class);
        BiMetricMapper metricMapper = mock(BiMetricMapper.class);
        BiResourcePermissionGuard permissionGuard = mock(BiResourcePermissionGuard.class);
        when(workspaceMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(workspace());
        when(datasetMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(dataset("DRAFT"));
        org.mockito.Mockito.doThrow(new BiPermissionService.BiPermissionDeniedException("missing edit"))
                .when(permissionGuard)
                .require(7L, 5L, "DATASET", 11L, "alice", "ANALYST", BiPermissionService.ACTION_EDIT);
        BiDatasetResourceService service = new BiDatasetResourceService(
                workspaceMapper,
                datasetMapper,
                fieldMapper,
                metricMapper,
                null,
                new ObjectMapper(),
                permissionGuard);

        assertThatThrownBy(() -> service.saveDraft(7L, "alice", "ANALYST", datasetResource()))
                .isInstanceOf(BiPermissionService.BiPermissionDeniedException.class)
                .hasMessageContaining("missing edit");

        verify(permissionGuard).require(7L, 5L, "DATASET", 11L, "alice", "ANALYST", BiPermissionService.ACTION_EDIT);
        verify(datasetMapper, org.mockito.Mockito.never()).upsert(any(BiDatasetDO.class));
    }

    @Test
    void saveDraftRequiresCurrentEditLockForExistingDataset() {
        BiWorkspaceMapper workspaceMapper = mock(BiWorkspaceMapper.class);
        BiDatasetMapper datasetMapper = mock(BiDatasetMapper.class);
        BiDatasetFieldMapper fieldMapper = mock(BiDatasetFieldMapper.class);
        BiMetricMapper metricMapper = mock(BiMetricMapper.class);
        BiResourceCollaborationService collaborationService = mock(BiResourceCollaborationService.class);
        when(workspaceMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(workspace());
        when(datasetMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(dataset("DRAFT"));
        org.mockito.Mockito.doThrow(new BiResourceCollaborationService.BiResourceLockRequiredException(
                        "active BI resource lock is required"))
                .when(collaborationService)
                .requireCurrentLock(7L, 5L, "DATASET", "channel_performance_daily", "alice", "bad-token");
        BiDatasetResourceService service = new BiDatasetResourceService(
                workspaceMapper,
                datasetMapper,
                fieldMapper,
                metricMapper,
                null,
                new ObjectMapper(),
                null,
                null,
                collaborationService);

        assertThatThrownBy(() -> service.saveDraft(
                7L, "alice", "ANALYST", datasetResource(), "bad-token"))
                .isInstanceOf(BiResourceCollaborationService.BiResourceLockRequiredException.class)
                .hasMessageContaining("active BI resource lock is required");

        verify(collaborationService).requireCurrentLock(
                7L, 5L, "DATASET", "channel_performance_daily", "alice", "bad-token");
        verify(datasetMapper, org.mockito.Mockito.never()).upsert(any(BiDatasetDO.class));
    }

    @Test
    void publishRequiresPublishPermission() {
        BiWorkspaceMapper workspaceMapper = mock(BiWorkspaceMapper.class);
        BiDatasetMapper datasetMapper = mock(BiDatasetMapper.class);
        BiDatasetFieldMapper fieldMapper = mock(BiDatasetFieldMapper.class);
        BiMetricMapper metricMapper = mock(BiMetricMapper.class);
        BiResourcePermissionGuard permissionGuard = mock(BiResourcePermissionGuard.class);
        when(workspaceMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(workspace());
        when(datasetMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(dataset("DRAFT"));
        org.mockito.Mockito.doThrow(new BiPermissionService.BiPermissionDeniedException("missing publish"))
                .when(permissionGuard)
                .require(7L, 5L, "DATASET", 11L, "alice", "ANALYST", BiPermissionService.ACTION_PUBLISH);
        BiDatasetResourceService service = new BiDatasetResourceService(
                workspaceMapper,
                datasetMapper,
                fieldMapper,
                metricMapper,
                null,
                new ObjectMapper(),
                permissionGuard);

        assertThatThrownBy(() -> service.publish(7L, "alice", "ANALYST", "channel_performance_daily"))
                .isInstanceOf(BiPermissionService.BiPermissionDeniedException.class)
                .hasMessageContaining("missing publish");

        verify(permissionGuard).require(7L, 5L, "DATASET", 11L, "alice", "ANALYST", BiPermissionService.ACTION_PUBLISH);
        verify(datasetMapper, org.mockito.Mockito.never()).publish(7L, 5L, "channel_performance_daily");
    }

    @Test
    void publishRequiresFreshApprovalForNonAdmin() {
        BiWorkspaceMapper workspaceMapper = mock(BiWorkspaceMapper.class);
        BiDatasetMapper datasetMapper = mock(BiDatasetMapper.class);
        BiDatasetFieldMapper fieldMapper = mock(BiDatasetFieldMapper.class);
        BiMetricMapper metricMapper = mock(BiMetricMapper.class);
        BiPublishApprovalService approvalService = mock(BiPublishApprovalService.class);
        LocalDateTime updatedAt = LocalDateTime.of(2026, 6, 5, 12, 0);
        BiDatasetDO draft = dataset("DRAFT");
        draft.setUpdatedAt(updatedAt);
        when(workspaceMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(workspace());
        when(datasetMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(draft);
        org.mockito.Mockito.doThrow(new BiPublishApprovalService.BiPublishApprovalRequiredException(
                        "approved BI publish approval is required"))
                .when(approvalService)
                .requireApprovedApproval(7L, 5L, "DATASET", "channel_performance_daily", updatedAt);
        BiDatasetResourceService service = new BiDatasetResourceService(
                workspaceMapper,
                datasetMapper,
                fieldMapper,
                metricMapper,
                null,
                new ObjectMapper(),
                null,
                approvalService);

        assertThatThrownBy(() -> service.publish(7L, "alice", "ANALYST", "channel_performance_daily"))
                .isInstanceOf(BiPublishApprovalService.BiPublishApprovalRequiredException.class)
                .hasMessageContaining("approved BI publish approval is required");

        verify(approvalService).requireApprovedApproval(
                7L, 5L, "DATASET", "channel_performance_daily", updatedAt);
        verify(datasetMapper, org.mockito.Mockito.never()).publish(7L, 5L, "channel_performance_daily");
    }

    @Test
    void tenantAdminPublishSkipsApprovalGate() {
        BiWorkspaceMapper workspaceMapper = mock(BiWorkspaceMapper.class);
        BiDatasetMapper datasetMapper = mock(BiDatasetMapper.class);
        BiDatasetFieldMapper fieldMapper = mock(BiDatasetFieldMapper.class);
        BiMetricMapper metricMapper = mock(BiMetricMapper.class);
        BiPublishApprovalService approvalService = mock(BiPublishApprovalService.class);
        when(workspaceMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(workspace());
        when(datasetMapper.selectOne(any(LambdaQueryWrapper.class)))
                .thenReturn(dataset("DRAFT"), dataset("PUBLISHED"));
        when(fieldMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(field("stat_date", "DIMENSION")));
        when(metricMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(metric("send_count", "SUM(send_count)")));
        BiDatasetResourceService service = new BiDatasetResourceService(
                workspaceMapper,
                datasetMapper,
                fieldMapper,
                metricMapper,
                null,
                new ObjectMapper(),
                null,
                approvalService);

        BiDatasetResource published = service.publish(
                7L, "admin", "TENANT_ADMIN", "channel_performance_daily");

        assertThat(published.status()).isEqualTo("PUBLISHED");
        verify(approvalService, org.mockito.Mockito.never()).requireApprovedApproval(
                any(), any(), any(), any(), any());
        verify(datasetMapper).publish(7L, 5L, "channel_performance_daily");
    }

    @Test
    void sqlDatasetPublishRequiresApprovalEvenForTenantAdmin() {
        BiWorkspaceMapper workspaceMapper = mock(BiWorkspaceMapper.class);
        BiDatasetMapper datasetMapper = mock(BiDatasetMapper.class);
        BiDatasetFieldMapper fieldMapper = mock(BiDatasetFieldMapper.class);
        BiMetricMapper metricMapper = mock(BiMetricMapper.class);
        BiPublishApprovalService approvalService = mock(BiPublishApprovalService.class);
        LocalDateTime updatedAt = LocalDateTime.of(2026, 6, 6, 9, 30);
        BiDatasetDO draft = sqlDataset("DRAFT");
        draft.setUpdatedAt(updatedAt);
        when(workspaceMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(workspace());
        when(datasetMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(draft);
        org.mockito.Mockito.doThrow(new BiPublishApprovalService.BiPublishApprovalRequiredException(
                        "approved BI publish approval is required"))
                .when(approvalService)
                .requireApprovedApproval(7L, 5L, "DATASET", "campaign_sql", updatedAt);
        BiDatasetResourceService service = new BiDatasetResourceService(
                workspaceMapper,
                datasetMapper,
                fieldMapper,
                metricMapper,
                null,
                new ObjectMapper(),
                null,
                approvalService);

        assertThatThrownBy(() -> service.publish(7L, "admin", "TENANT_ADMIN", "campaign_sql"))
                .isInstanceOf(BiPublishApprovalService.BiPublishApprovalRequiredException.class)
                .hasMessageContaining("approved BI publish approval is required");

        verify(approvalService).requireApprovedApproval(7L, 5L, "DATASET", "campaign_sql", updatedAt);
        verify(datasetMapper, org.mockito.Mockito.never()).publish(7L, 5L, "campaign_sql");
    }

    @Test
    void listAndRestoreDatasetVersionsUseSnapshots() {
        BiWorkspaceMapper workspaceMapper = mock(BiWorkspaceMapper.class);
        BiDatasetMapper datasetMapper = mock(BiDatasetMapper.class);
        BiDatasetFieldMapper fieldMapper = mock(BiDatasetFieldMapper.class);
        BiMetricMapper metricMapper = mock(BiMetricMapper.class);
        BiDatasetVersionMapper versionMapper = mock(BiDatasetVersionMapper.class);
        when(workspaceMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(workspace());
        when(datasetMapper.selectOne(any(LambdaQueryWrapper.class)))
                .thenReturn(dataset("PUBLISHED"), dataset("PUBLISHED"), dataset("DRAFT"));
        when(fieldMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(field("stat_date", "DIMENSION")));
        when(metricMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(metric("send_count", "SUM(send_count)")));
        BiDatasetVersionDO snapshot = datasetVersion(2, datasetResource());
        when(versionMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(snapshot));
        when(versionMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(snapshot);
        BiDatasetResourceService service =
                service(workspaceMapper, datasetMapper, fieldMapper, metricMapper, versionMapper);

        List<BiDatasetVersionView> versions = service.listVersions(7L, "channel_performance_daily", 5);
        BiDatasetResource restored = service.restoreVersion(7L, "alice", "channel_performance_daily", 2);

        assertThat(versions).singleElement()
                .satisfies(version -> {
                    assertThat(version.version()).isEqualTo(2);
                    assertThat(version.resource().datasetKey()).isEqualTo("channel_performance_daily");
                });
        assertThat(restored.status()).isEqualTo("DRAFT");
        verify(datasetMapper).upsert(any(BiDatasetDO.class));
    }

    private BiDatasetResourceService service(BiWorkspaceMapper workspaceMapper,
                                             BiDatasetMapper datasetMapper,
                                             BiDatasetFieldMapper fieldMapper,
                                             BiMetricMapper metricMapper) {
        return new BiDatasetResourceService(workspaceMapper, datasetMapper, fieldMapper, metricMapper, new ObjectMapper());
    }

    private BiDatasetResourceService service(BiWorkspaceMapper workspaceMapper,
                                             BiDatasetMapper datasetMapper,
                                             BiDatasetFieldMapper fieldMapper,
                                             BiMetricMapper metricMapper,
                                             BiDatasetVersionMapper versionMapper) {
        return new BiDatasetResourceService(
                workspaceMapper,
                datasetMapper,
                fieldMapper,
                metricMapper,
                versionMapper,
                new ObjectMapper());
    }

    private BiWorkspaceDO workspace() {
        BiWorkspaceDO row = new BiWorkspaceDO();
        row.setId(5L);
        row.setTenantId(0L);
        row.setWorkspaceKey("marketing_canvas");
        return row;
    }

    private BiDatasetDO dataset(String status) {
        BiDatasetDO row = new BiDatasetDO();
        row.setId(11L);
        row.setTenantId(7L);
        row.setWorkspaceId(5L);
        row.setDatasetKey("channel_performance_daily");
        row.setName("Channel Performance Daily");
        row.setDatasetType("TABLE");
        row.setTableExpression("canvas_dws.channel_performance_daily");
        row.setTenantColumn("tenant_id");
        row.setModelJson("{\"category\":\"CHANNEL\"}");
        row.setStatus(status);
        return row;
    }

    private BiDatasetDO sqlDataset(String status) {
        BiDatasetDO row = new BiDatasetDO();
        row.setId(12L);
        row.setTenantId(7L);
        row.setWorkspaceId(5L);
        row.setDatasetKey("campaign_sql");
        row.setName("Campaign SQL");
        row.setDatasetType("SQL");
        row.setTableExpression("(SELECT tenant_id, stat_date, total_cost FROM campaign_daily WHERE deleted = 0) sql_dataset");
        row.setTenantColumn("tenant_id");
        row.setModelJson("{\"sqlApprovalRequired\":true}");
        row.setStatus(status);
        return row;
    }

    private BiDatasetResource datasetResource() {
        return new BiDatasetResource(
                "channel_performance_daily",
                "Channel Performance Daily",
                "TABLE",
                "canvas_dws.channel_performance_daily",
                "tenant_id",
                Map.of("category", "CHANNEL"),
                List.of(
                        new BiDatasetFieldResource("stat_date", "Date", "stat_date", "DIMENSION", "DATE", "DATE", null, "yyyy-MM-dd", null, true, "NORMAL", 10),
                        new BiDatasetFieldResource("send_count", "Send Count", "send_count", "MEASURE", "NUMBER", "COUNT", "SUM", "#,##0", "次", true, "NORMAL", 20)),
                List.of(new BiMetricResource("send_count", "Send Count", "SUM(send_count)", "SUM", "NUMBER", "次", "#,##0", List.of("stat_date"), "alice", "Daily sends", "ACTIVE")),
                "DRAFT",
                "CLIENT");
    }

    private BiDatasetResource sqlDatasetResource() {
        return sqlDatasetResource("SELECT tenant_id, stat_date, total_cost FROM campaign_daily WHERE deleted = 0");
    }

    private BiDatasetResource sqlDatasetResource(String tableExpression) {
        return new BiDatasetResource(
                "campaign_sql",
                "Campaign SQL",
                "SQL",
                tableExpression,
                "tenant_id",
                Map.of("dataSourceConfigId", 7L),
                List.of(
                        new BiDatasetFieldResource("stat_date", "Date", "stat_date", "DIMENSION", "DATE", "DATE", null, "yyyy-MM-dd", null, true, "NORMAL", 10),
                        new BiDatasetFieldResource("total_cost", "Total Cost", "total_cost", "MEASURE", "NUMBER", null, "SUM", "#,##0.00", "元", true, "NORMAL", 20)),
                List.of(new BiMetricResource("total_cost", "Total Cost", "SUM(total_cost)", "SUM", "NUMBER", "元", "#,##0.00", List.of("stat_date"), "alice", "SQL metric", "ACTIVE")),
                "DRAFT",
                "CLIENT");
    }

    private BiDatasetResource parameterizedSqlDatasetResource() {
        return new BiDatasetResource(
                "campaign_sql",
                "Campaign SQL",
                "SQL",
                "SELECT tenant_id, stat_date, total_cost FROM campaign_daily WHERE stat_date >= {{start_date}} AND channel = {{channel}}",
                "tenant_id",
                Map.of(
                        "dataSourceConfigId", 7L,
                        "sqlParameters", List.of(
                                Map.of(
                                        "key", "start_date",
                                        "dataType", "DATE",
                                        "required", true),
                                Map.of(
                                        "key", "channel",
                                        "dataType", "STRING",
                                        "required", false,
                                        "defaultValue", "PAID",
                                        "allowedValues", List.of("PAID", "EMAIL")))),
                sqlDatasetResource().fields(),
                sqlDatasetResource().metrics(),
                "DRAFT",
                "CLIENT");
    }

    private BiDatasetFieldDO field(String fieldKey, String role) {
        BiDatasetFieldDO row = new BiDatasetFieldDO();
        row.setTenantId(7L);
        row.setDatasetId(11L);
        row.setFieldKey(fieldKey);
        row.setDisplayName(fieldKey);
        row.setColumnExpression(fieldKey);
        row.setRoleKey(role);
        row.setDataType("stat_date".equals(fieldKey) ? "DATE" : "NUMBER");
        row.setVisible(true);
        row.setSensitiveLevel("NORMAL");
        row.setSortOrder(10);
        return row;
    }

    private BiMetricDO metric(String metricKey, String expression) {
        BiMetricDO row = new BiMetricDO();
        row.setTenantId(7L);
        row.setWorkspaceId(5L);
        row.setDatasetId(11L);
        row.setMetricKey(metricKey);
        row.setDisplayName(metricKey);
        row.setExpression(expression);
        row.setAggregation("SUM");
        row.setDataType("NUMBER");
        row.setAllowedDimensionsJson("[\"stat_date\"]");
        row.setStatus("ACTIVE");
        return row;
    }

    private BiDatasetVersionDO datasetVersion(int version, BiDatasetResource resource) {
        BiDatasetVersionDO row = new BiDatasetVersionDO();
        row.setId(51L);
        row.setTenantId(7L);
        row.setWorkspaceId(5L);
        row.setDatasetId(11L);
        row.setDatasetKey("channel_performance_daily");
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
