package org.chovy.canvas.domain.bi.permission;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.common.tenant.RoleNames;
import org.chovy.canvas.dal.dataobject.BiAuditLogDO;
import org.chovy.canvas.dal.dataobject.BiBigScreenDO;
import org.chovy.canvas.dal.dataobject.BiColumnPermissionDO;
import org.chovy.canvas.dal.dataobject.BiDatasetDO;
import org.chovy.canvas.dal.dataobject.BiResourcePermissionDO;
import org.chovy.canvas.dal.dataobject.BiRowPermissionDO;
import org.chovy.canvas.dal.dataobject.BiSpreadsheetDO;
import org.chovy.canvas.dal.dataobject.DataSourceConfigDO;
import org.chovy.canvas.dal.mapper.BiAuditLogMapper;
import org.chovy.canvas.dal.mapper.BiBigScreenMapper;
import org.chovy.canvas.dal.mapper.BiChartMapper;
import org.chovy.canvas.dal.mapper.BiColumnPermissionMapper;
import org.chovy.canvas.dal.mapper.BiDashboardMapper;
import org.chovy.canvas.dal.mapper.BiDatasetMapper;
import org.chovy.canvas.dal.mapper.BiPortalMapper;
import org.chovy.canvas.dal.mapper.BiResourcePermissionMapper;
import org.chovy.canvas.dal.mapper.BiRowPermissionMapper;
import org.chovy.canvas.dal.mapper.BiSpreadsheetMapper;
import org.chovy.canvas.dal.mapper.DataSourceConfigMapper;
import org.chovy.canvas.domain.bi.query.BiFilter;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BiPermissionAdminServiceTest {

    @Test
    void upsertsDatasetResourcePermissionByResourceKey() {
        Fixture fixture = fixture();
        BiDatasetDO dataset = dataset(11L, "canvas_daily_stats");
        BiResourcePermissionDO persisted = resourcePermission(99L, 11L, "ROLE", RoleNames.OPERATOR, "USE", "ALLOW");
        when(fixture.datasetMapper.selectOne(any())).thenReturn(dataset);
        when(fixture.datasetMapper.selectById(11L)).thenReturn(dataset);
        when(fixture.resourcePermissionMapper.selectOne(any())).thenReturn(null, persisted);

        BiResourcePermissionView view = fixture.service.upsertResourcePermission(
                7L,
                "alice",
                new BiResourcePermissionCommand(
                        "DATASET",
                        "canvas_daily_stats",
                        null,
                        "ROLE",
                        RoleNames.OPERATOR,
                        "USE",
                        "ALLOW"));

        assertThat(view.id()).isEqualTo(99L);
        assertThat(view.resourceKey()).isEqualTo("canvas_daily_stats");
        assertThat(view.subjectType()).isEqualTo("ROLE");
        verify(fixture.resourcePermissionMapper).insert(any(BiResourcePermissionDO.class));
    }

    @Test
    void upsertsBigScreenResourcePermissionByResourceKey() {
        Fixture fixture = fixture();
        BiBigScreenDO screen = bigScreen(51L, "executive-wall");
        BiResourcePermissionDO persisted = resourcePermission(
                199L, 51L, "BIG_SCREEN", "ROLE", RoleNames.OPERATOR, "VIEW", "ALLOW");
        when(fixture.bigScreenMapper.selectOne(any())).thenReturn(screen);
        when(fixture.bigScreenMapper.selectById(51L)).thenReturn(screen);
        when(fixture.resourcePermissionMapper.selectOne(any())).thenReturn(null, persisted);

        BiResourcePermissionView view = fixture.service.upsertResourcePermission(
                7L,
                "alice",
                new BiResourcePermissionCommand(
                        "big_screen",
                        "executive-wall",
                        null,
                        "ROLE",
                        RoleNames.OPERATOR,
                        "VIEW",
                        "ALLOW"));

        ArgumentCaptor<BiResourcePermissionDO> captor = ArgumentCaptor.forClass(BiResourcePermissionDO.class);
        verify(fixture.resourcePermissionMapper).insert(captor.capture());
        assertThat(captor.getValue().getWorkspaceId()).isEqualTo(5L);
        assertThat(captor.getValue().getResourceType()).isEqualTo("BIG_SCREEN");
        assertThat(captor.getValue().getResourceId()).isEqualTo(51L);
        assertThat(view.resourceType()).isEqualTo("BIG_SCREEN");
        assertThat(view.resourceKey()).isEqualTo("executive-wall");
        assertThat(view.actionKey()).isEqualTo("VIEW");
    }

    @Test
    void upsertsSpreadsheetResourcePermissionByResourceKey() {
        Fixture fixture = fixture();
        BiSpreadsheetDO spreadsheet = spreadsheet(61L, "budget-sheet");
        BiResourcePermissionDO persisted = resourcePermission(
                299L, 61L, "SPREADSHEET", "USER", "bob", "EDIT", "ALLOW");
        when(fixture.spreadsheetMapper.selectOne(any())).thenReturn(spreadsheet);
        when(fixture.spreadsheetMapper.selectById(61L)).thenReturn(spreadsheet);
        when(fixture.resourcePermissionMapper.selectOne(any())).thenReturn(null, persisted);

        BiResourcePermissionView view = fixture.service.upsertResourcePermission(
                7L,
                "alice",
                new BiResourcePermissionCommand(
                        "spreadsheet",
                        "budget-sheet",
                        null,
                        "USER",
                        "bob",
                        "EDIT",
                        "ALLOW"));

        ArgumentCaptor<BiResourcePermissionDO> captor = ArgumentCaptor.forClass(BiResourcePermissionDO.class);
        verify(fixture.resourcePermissionMapper).insert(captor.capture());
        assertThat(captor.getValue().getWorkspaceId()).isEqualTo(5L);
        assertThat(captor.getValue().getResourceType()).isEqualTo("SPREADSHEET");
        assertThat(captor.getValue().getResourceId()).isEqualTo(61L);
        assertThat(view.resourceType()).isEqualTo("SPREADSHEET");
        assertThat(view.resourceKey()).isEqualTo("budget-sheet");
        assertThat(view.actionKey()).isEqualTo("EDIT");
    }

    @Test
    void upsertsDatasourceUsePermissionBySourceKey() {
        Fixture fixture = fixture();
        DataSourceConfigDO datasource = datasource(71L, "Marketing Doris");
        BiResourcePermissionDO persisted = resourcePermission(
                399L, 71L, "DATASOURCE", "ROLE", RoleNames.OPERATOR, "USE", "ALLOW");
        when(fixture.dataSourceConfigMapper.selectOne(any())).thenReturn(datasource);
        when(fixture.dataSourceConfigMapper.selectById(71L)).thenReturn(datasource);
        when(fixture.resourcePermissionMapper.selectOne(any())).thenReturn(null, persisted);

        BiResourcePermissionView view = fixture.service.upsertResourcePermission(
                7L,
                "alice",
                new BiResourcePermissionCommand(
                        "datasource",
                        "jdbc-71",
                        null,
                        "ROLE",
                        RoleNames.OPERATOR,
                        "USE",
                        "ALLOW"));

        ArgumentCaptor<BiResourcePermissionDO> captor = ArgumentCaptor.forClass(BiResourcePermissionDO.class);
        verify(fixture.resourcePermissionMapper).insert(captor.capture());
        assertThat(captor.getValue().getWorkspaceId()).isEqualTo(0L);
        assertThat(captor.getValue().getResourceType()).isEqualTo("DATASOURCE");
        assertThat(captor.getValue().getResourceId()).isEqualTo(71L);
        assertThat(view.resourceType()).isEqualTo("DATASOURCE");
        assertThat(view.resourceKey()).isEqualTo("jdbc-71");
        assertThat(view.actionKey()).isEqualTo("USE");
    }

    @Test
    void updatesExistingResourcePermissionEffect() {
        Fixture fixture = fixture();
        BiDatasetDO dataset = dataset(11L, "canvas_daily_stats");
        BiResourcePermissionDO existing = resourcePermission(99L, 11L, "USER", "bob", "EXPORT", "ALLOW");
        when(fixture.datasetMapper.selectOne(any())).thenReturn(dataset);
        when(fixture.datasetMapper.selectById(11L)).thenReturn(dataset);
        when(fixture.resourcePermissionMapper.selectOne(any())).thenReturn(existing);

        BiResourcePermissionView view = fixture.service.upsertResourcePermission(
                7L,
                "alice",
                new BiResourcePermissionCommand(
                        "DATASET",
                        "canvas_daily_stats",
                        null,
                        "USER",
                        "bob",
                        "EXPORT",
                        "DENY"));

        assertThat(view.effect()).isEqualTo("DENY");
        verify(fixture.resourcePermissionMapper).updateById(existing);
    }

    @Test
    void upsertsRowPermissionWithStructuredFilters() {
        Fixture fixture = fixture();
        BiDatasetDO dataset = dataset(11L, "canvas_daily_stats");
        BiRowPermissionDO persisted = rowPermission(21L, 11L, "operator-canvas", "ROLE", RoleNames.OPERATOR);
        when(fixture.datasetMapper.selectOne(any())).thenReturn(dataset);
        when(fixture.rowPermissionMapper.selectOne(any())).thenReturn(null, persisted);

        BiRowPermissionView view = fixture.service.upsertRowPermission(
                7L,
                new BiRowPermissionCommand(
                        "canvas_daily_stats",
                        "operator-canvas",
                        "ROLE",
                        RoleNames.OPERATOR,
                        List.of(new BiFilter("canvas_id", BiFilter.Operator.IN, List.of(12, 13))),
                        Map.of(),
                        true));

        assertThat(view.ruleKey()).isEqualTo("operator-canvas");
        assertThat(view.filterJson()).contains("canvas_id", "IN");
        verify(fixture.rowPermissionMapper).insert(any(BiRowPermissionDO.class));
    }

    @Test
    void upsertsColumnPermissionWithMaskStrategy() {
        Fixture fixture = fixture();
        BiDatasetDO dataset = dataset(11L, "canvas_daily_stats");
        BiColumnPermissionDO persisted = columnPermission(31L, 11L, "canvas_name", "ROLE", RoleNames.OPERATOR, "MASK");
        when(fixture.datasetMapper.selectOne(any())).thenReturn(dataset);
        when(fixture.columnPermissionMapper.selectOne(any())).thenReturn(null, persisted);

        BiColumnPermissionView view = fixture.service.upsertColumnPermission(
                7L,
                new BiColumnPermissionCommand(
                        "canvas_daily_stats",
                        "canvas_name",
                        "ROLE",
                        RoleNames.OPERATOR,
                        "MASK",
                        Map.of("strategy", "FIXED", "replacement", "MASKED"),
                        true));

        assertThat(view.fieldKey()).isEqualTo("canvas_name");
        assertThat(view.policy()).isEqualTo("MASK");
        assertThat(view.maskJson()).contains("FIXED");
        verify(fixture.columnPermissionMapper).insert(any(BiColumnPermissionDO.class));
    }

    @Test
    void listsRowPermissionsWithDatasetKey() {
        Fixture fixture = fixture();
        BiDatasetDO dataset = dataset(11L, "canvas_daily_stats");
        when(fixture.datasetMapper.selectList(any())).thenReturn(List.of(dataset));
        when(fixture.rowPermissionMapper.selectList(any())).thenReturn(List.of(
                rowPermission(21L, 11L, "operator-canvas", "ROLE", RoleNames.OPERATOR)));

        List<BiRowPermissionView> views = fixture.service.listRowPermissions(7L, null);

        assertThat(views).singleElement().satisfies(view -> {
            assertThat(view.datasetKey()).isEqualTo("canvas_daily_stats");
            assertThat(view.ruleKey()).isEqualTo("operator-canvas");
        });
    }

    @Test
    void auditsResourcePermissionCreateWithBeforeAndAfterSnapshots() throws Exception {
        BiAuditLogMapper auditLogMapper = mock(BiAuditLogMapper.class);
        Fixture fixture = fixture(auditLogMapper);
        BiDatasetDO dataset = dataset(11L, "canvas_daily_stats");
        BiResourcePermissionDO persisted = resourcePermission(99L, 11L, "ROLE", RoleNames.OPERATOR, "USE", "ALLOW");
        when(fixture.datasetMapper.selectOne(any())).thenReturn(dataset);
        when(fixture.datasetMapper.selectById(11L)).thenReturn(dataset);
        when(fixture.resourcePermissionMapper.selectOne(any())).thenReturn(null, persisted);

        fixture.service.upsertResourcePermission(
                7L,
                "alice",
                new BiResourcePermissionCommand(
                        "DATASET",
                        "canvas_daily_stats",
                        null,
                        "ROLE",
                        RoleNames.OPERATOR,
                        "USE",
                        "ALLOW"));

        ArgumentCaptor<BiAuditLogDO> audit = ArgumentCaptor.forClass(BiAuditLogDO.class);
        verify(auditLogMapper).insert(audit.capture());
        assertThat(audit.getValue().getTenantId()).isEqualTo(7L);
        assertThat(audit.getValue().getWorkspaceId()).isEqualTo(3L);
        assertThat(audit.getValue().getActorId()).isEqualTo("alice");
        assertThat(audit.getValue().getActionKey()).isEqualTo("BI_PERMISSION_CHANGE");
        assertThat(audit.getValue().getResourceType()).isEqualTo("BI_PERMISSION");
        assertThat(audit.getValue().getResourceId()).isEqualTo(99L);
        assertThat(audit.getValue().getCreatedAt()).isNotNull();
        JsonNode detail = new ObjectMapper().readTree(audit.getValue().getDetailJson());
        assertThat(detail.path("permissionKind").asText()).isEqualTo("RESOURCE");
        assertThat(detail.path("operation").asText()).isEqualTo("CREATE");
        assertThat(detail.path("before").isNull()).isTrue();
        assertThat(detail.path("after").path("resourceType").asText()).isEqualTo("DATASET");
        assertThat(detail.path("after").path("subjectId").asText()).isEqualTo(RoleNames.OPERATOR);
        assertThat(detail.path("after").path("effect").asText()).isEqualTo("ALLOW");
    }

    @Test
    void auditsRowPermissionUpdateAndDeleteWithActor() throws Exception {
        BiAuditLogMapper auditLogMapper = mock(BiAuditLogMapper.class);
        Fixture fixture = fixture(auditLogMapper);
        BiDatasetDO dataset = dataset(11L, "canvas_daily_stats");
        BiRowPermissionDO existing = rowPermission(21L, 11L, "operator-canvas", "ROLE", RoleNames.OPERATOR);
        when(fixture.datasetMapper.selectOne(any())).thenReturn(dataset);
        when(fixture.rowPermissionMapper.selectOne(any())).thenReturn(existing);
        when(fixture.rowPermissionMapper.selectById(21L)).thenReturn(existing);

        fixture.service.upsertRowPermission(
                7L,
                "alice",
                new BiRowPermissionCommand(
                        "canvas_daily_stats",
                        "operator-canvas",
                        "USER",
                        "bob",
                        List.of(new BiFilter("canvas_id", BiFilter.Operator.EQ, 12)),
                        Map.of(),
                        false));
        fixture.service.deleteRowPermission(7L, "alice", 21L);

        ArgumentCaptor<BiAuditLogDO> audit = ArgumentCaptor.forClass(BiAuditLogDO.class);
        verify(auditLogMapper, times(2)).insert(audit.capture());
        JsonNode update = new ObjectMapper().readTree(audit.getAllValues().get(0).getDetailJson());
        assertThat(update.path("permissionKind").asText()).isEqualTo("ROW");
        assertThat(update.path("operation").asText()).isEqualTo("UPDATE");
        assertThat(update.path("before").path("subjectId").asText()).isEqualTo(RoleNames.OPERATOR);
        assertThat(update.path("after").path("subjectId").asText()).isEqualTo("bob");
        assertThat(update.path("after").path("enabled").asBoolean()).isFalse();
        JsonNode delete = new ObjectMapper().readTree(audit.getAllValues().get(1).getDetailJson());
        assertThat(delete.path("permissionKind").asText()).isEqualTo("ROW");
        assertThat(delete.path("operation").asText()).isEqualTo("DELETE");
        assertThat(delete.path("before").path("id").asLong()).isEqualTo(21L);
        assertThat(delete.path("after").isNull()).isTrue();
    }

    @Test
    void appliesPermissionChangeWhenAuditStorageFails() {
        BiAuditLogMapper auditLogMapper = mock(BiAuditLogMapper.class);
        doThrow(new IllegalStateException("audit unavailable")).when(auditLogMapper).insert(any(BiAuditLogDO.class));
        Fixture fixture = fixture(auditLogMapper);
        BiDatasetDO dataset = dataset(11L, "canvas_daily_stats");
        BiColumnPermissionDO persisted = columnPermission(31L, 11L, "canvas_name", "ROLE", RoleNames.OPERATOR, "MASK");
        when(fixture.datasetMapper.selectOne(any())).thenReturn(dataset);
        when(fixture.columnPermissionMapper.selectOne(any())).thenReturn(null, persisted);

        assertThatCode(() -> fixture.service.upsertColumnPermission(
                7L,
                "alice",
                new BiColumnPermissionCommand(
                        "canvas_daily_stats",
                        "canvas_name",
                        "ROLE",
                        RoleNames.OPERATOR,
                        "MASK",
                        Map.of("strategy", "FIXED", "replacement", "MASKED"),
                        true))).doesNotThrowAnyException();

        verify(fixture.columnPermissionMapper).insert(any(BiColumnPermissionDO.class));
    }

    @Test
    void listsRecentPermissionAuditEntriesForTenant() {
        BiAuditLogMapper auditLogMapper = mock(BiAuditLogMapper.class);
        when(auditLogMapper.selectList(any())).thenReturn(List.of(
                auditRow(101L, 7L, "alice", "BI_PERMISSION_CHANGE", "BI_PERMISSION",
                        "{\"permissionKind\":\"RESOURCE\",\"operation\":\"CREATE\"}",
                        "2026-06-05T09:20:00"),
                auditRow(102L, 9L, "mallory", "BI_PERMISSION_CHANGE", "BI_PERMISSION",
                        "{}",
                        "2026-06-05T09:25:00"),
                auditRow(103L, 7L, "alice", "BI_QUERY_EXECUTE", "BI_QUERY",
                        "{}",
                        "2026-06-05T09:30:00"),
                auditRow(104L, 7L, "bob", "BI_PERMISSION_CHANGE", "BI_PERMISSION",
                        "{\"permissionKind\":\"ROW\",\"operation\":\"DELETE\"}",
                        "2026-06-05T09:10:00")
        ));
        Fixture fixture = fixture(auditLogMapper);

        List<BiPermissionAuditEntry> entries = fixture.service.recentAudit(7L, 2);

        assertThat(entries).extracting(BiPermissionAuditEntry::id).containsExactly(101L, 104L);
        assertThat(entries).extracting(BiPermissionAuditEntry::actorId).containsExactly("alice", "bob");
        assertThat(entries).extracting(BiPermissionAuditEntry::detailJson)
                .containsExactly("{\"permissionKind\":\"RESOURCE\",\"operation\":\"CREATE\"}",
                        "{\"permissionKind\":\"ROW\",\"operation\":\"DELETE\"}");
    }

    private Fixture fixture() {
        return fixture(mock(BiAuditLogMapper.class));
    }

    private Fixture fixture(BiAuditLogMapper auditLogMapper) {
        BiDatasetMapper datasetMapper = mock(BiDatasetMapper.class);
        BiDashboardMapper dashboardMapper = mock(BiDashboardMapper.class);
        BiChartMapper chartMapper = mock(BiChartMapper.class);
        BiPortalMapper portalMapper = mock(BiPortalMapper.class);
        BiBigScreenMapper bigScreenMapper = mock(BiBigScreenMapper.class);
        BiSpreadsheetMapper spreadsheetMapper = mock(BiSpreadsheetMapper.class);
        DataSourceConfigMapper dataSourceConfigMapper = mock(DataSourceConfigMapper.class);
        BiResourcePermissionMapper resourcePermissionMapper = mock(BiResourcePermissionMapper.class);
        BiRowPermissionMapper rowPermissionMapper = mock(BiRowPermissionMapper.class);
        BiColumnPermissionMapper columnPermissionMapper = mock(BiColumnPermissionMapper.class);
        return new Fixture(
                datasetMapper,
                bigScreenMapper,
                spreadsheetMapper,
                dataSourceConfigMapper,
                resourcePermissionMapper,
                rowPermissionMapper,
                columnPermissionMapper,
                new BiPermissionAdminService(
                        datasetMapper,
                        dashboardMapper,
                        chartMapper,
                        portalMapper,
                        bigScreenMapper,
                        spreadsheetMapper,
                        dataSourceConfigMapper,
                        resourcePermissionMapper,
                        rowPermissionMapper,
                        columnPermissionMapper,
                        auditLogMapper,
                        new ObjectMapper()));
    }

    private BiDatasetDO dataset(Long id, String datasetKey) {
        BiDatasetDO row = new BiDatasetDO();
        row.setId(id);
        row.setTenantId(0L);
        row.setWorkspaceId(3L);
        row.setDatasetKey(datasetKey);
        row.setStatus("PUBLISHED");
        return row;
    }

    private DataSourceConfigDO datasource(Long id, String name) {
        DataSourceConfigDO row = new DataSourceConfigDO();
        row.setId(id);
        row.setTenantId(7L);
        row.setName(name);
        row.setType("JDBC");
        row.setEnabled(1);
        return row;
    }

    private BiBigScreenDO bigScreen(Long id, String screenKey) {
        BiBigScreenDO row = new BiBigScreenDO();
        row.setId(id);
        row.setTenantId(7L);
        row.setWorkspaceId(5L);
        row.setScreenKey(screenKey);
        row.setStatus("PUBLISHED");
        return row;
    }

    private BiSpreadsheetDO spreadsheet(Long id, String spreadsheetKey) {
        BiSpreadsheetDO row = new BiSpreadsheetDO();
        row.setId(id);
        row.setTenantId(7L);
        row.setWorkspaceId(5L);
        row.setSpreadsheetKey(spreadsheetKey);
        row.setStatus("PUBLISHED");
        return row;
    }

    private BiResourcePermissionDO resourcePermission(Long id,
                                                      Long resourceId,
                                                      String subjectType,
                                                      String subjectId,
                                                      String actionKey,
                                                      String effect) {
        return resourcePermission(id, resourceId, "DATASET", subjectType, subjectId, actionKey, effect);
    }

    private BiResourcePermissionDO resourcePermission(Long id,
                                                      Long resourceId,
                                                      String resourceType,
                                                      String subjectType,
                                                      String subjectId,
                                                      String actionKey,
                                                      String effect) {
        BiResourcePermissionDO row = new BiResourcePermissionDO();
        row.setId(id);
        row.setTenantId(7L);
        row.setWorkspaceId(3L);
        row.setResourceType(resourceType);
        row.setResourceId(resourceId);
        row.setSubjectType(subjectType);
        row.setSubjectId(subjectId);
        row.setActionKey(actionKey);
        row.setEffect(effect);
        return row;
    }

    private BiRowPermissionDO rowPermission(Long id,
                                            Long datasetId,
                                            String ruleKey,
                                            String subjectType,
                                            String subjectId) {
        BiRowPermissionDO row = new BiRowPermissionDO();
        row.setId(id);
        row.setTenantId(7L);
        row.setDatasetId(datasetId);
        row.setRuleKey(ruleKey);
        row.setSubjectType(subjectType);
        row.setSubjectId(subjectId);
        row.setFilterJson("{\"filters\":[{\"field\":\"canvas_id\",\"operator\":\"IN\",\"value\":[12,13]}]}");
        row.setEnabled(true);
        return row;
    }

    private BiColumnPermissionDO columnPermission(Long id,
                                                  Long datasetId,
                                                  String fieldKey,
                                                  String subjectType,
                                                  String subjectId,
                                                  String policy) {
        BiColumnPermissionDO row = new BiColumnPermissionDO();
        row.setId(id);
        row.setTenantId(7L);
        row.setDatasetId(datasetId);
        row.setFieldKey(fieldKey);
        row.setSubjectType(subjectType);
        row.setSubjectId(subjectId);
        row.setPolicy(policy);
        row.setMaskJson("{\"strategy\":\"FIXED\",\"replacement\":\"MASKED\"}");
        row.setEnabled(true);
        return row;
    }

    private BiAuditLogDO auditRow(Long id,
                                  Long tenantId,
                                  String actorId,
                                  String actionKey,
                                  String resourceType,
                                  String detailJson,
                                  String createdAt) {
        BiAuditLogDO row = new BiAuditLogDO();
        row.setId(id);
        row.setTenantId(tenantId);
        row.setWorkspaceId(3L);
        row.setActorId(actorId);
        row.setActionKey(actionKey);
        row.setResourceType(resourceType);
        row.setResourceId(id + 1000);
        row.setDetailJson(detailJson);
        row.setCreatedAt(LocalDateTime.parse(createdAt));
        return row;
    }

    private record Fixture(
            BiDatasetMapper datasetMapper,
            BiBigScreenMapper bigScreenMapper,
            BiSpreadsheetMapper spreadsheetMapper,
            DataSourceConfigMapper dataSourceConfigMapper,
            BiResourcePermissionMapper resourcePermissionMapper,
            BiRowPermissionMapper rowPermissionMapper,
            BiColumnPermissionMapper columnPermissionMapper,
            BiPermissionAdminService service) {
    }
}
