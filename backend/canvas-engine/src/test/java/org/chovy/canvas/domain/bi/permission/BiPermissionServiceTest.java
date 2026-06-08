package org.chovy.canvas.domain.bi.permission;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.common.tenant.RoleNames;
import org.chovy.canvas.dal.dataobject.BiAuditLogDO;
import org.chovy.canvas.dal.dataobject.BiColumnPermissionDO;
import org.chovy.canvas.dal.dataobject.BiDatasetDO;
import org.chovy.canvas.dal.dataobject.BiResourcePermissionDO;
import org.chovy.canvas.dal.dataobject.BiRowPermissionDO;
import org.chovy.canvas.dal.dataobject.BiWorkspaceMemberDO;
import org.chovy.canvas.dal.mapper.BiAuditLogMapper;
import org.chovy.canvas.dal.mapper.BiColumnPermissionMapper;
import org.chovy.canvas.dal.mapper.BiDatasetMapper;
import org.chovy.canvas.dal.mapper.BiResourcePermissionMapper;
import org.chovy.canvas.dal.mapper.BiRowPermissionMapper;
import org.chovy.canvas.dal.mapper.BiWorkspaceMemberMapper;
import org.chovy.canvas.domain.bi.portal.BiPortalMenuResource;
import org.chovy.canvas.domain.bi.query.BiDatasetSpec;
import org.chovy.canvas.domain.bi.query.BiFilter;
import org.chovy.canvas.domain.bi.query.BiQueryContext;
import org.chovy.canvas.domain.bi.query.BiQueryRequest;
import org.chovy.canvas.domain.bi.query.MarketingBiDatasetRegistry;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BiPermissionServiceTest {

    @Test
    void rowPermissionAddsScopedFiltersToQuery() {
        Fixtures fixtures = fixtures();
        when(fixtures.resourcePermissionMapper.selectList(any())).thenReturn(List.of());
        when(fixtures.rowPermissionMapper.selectList(any())).thenReturn(List.of(rowPermission(
                "operator-canvas",
                "ROLE",
                RoleNames.OPERATOR,
                "{\"filters\":[{\"field\":\"canvas_id\",\"operator\":\"IN\",\"value\":[12,13]}]}")));
        when(fixtures.columnPermissionMapper.selectList(any())).thenReturn(List.of());

        BiPermissionService.BiPreparedQuery prepared = fixtures.service.prepareQuery(
                dataset(),
                request(List.of("stat_date"), List.of("total_executions")),
                new BiQueryContext(7L, "alice", RoleNames.OPERATOR),
                BiPermissionService.ACTION_USE);

        assertThat(prepared.request().filters()).extracting(BiFilter::field)
                .containsExactly("canvas_id");
        assertThat(prepared.request().filters().get(0).operator()).isEqualTo(BiFilter.Operator.IN);
        assertThat(prepared.permissionSignature()).contains("operator-canvas");
    }

    @Test
    void resourcePermissionDenyBlocksQueryBeforeSqlCompilation() {
        Fixtures fixtures = fixtures();
        when(fixtures.resourcePermissionMapper.selectList(any())).thenReturn(List.of(resourcePermission(
                "ALL", "*", BiPermissionService.ACTION_USE, "DENY")));

        assertThatThrownBy(() -> fixtures.service.prepareQuery(
                dataset(),
                request(List.of("stat_date"), List.of("total_executions")),
                new BiQueryContext(7L, "alice", RoleNames.OPERATOR),
                BiPermissionService.ACTION_USE))
                .isInstanceOf(BiPermissionService.BiPermissionDeniedException.class)
                .hasMessageContaining("resource permission DENY");

        verify(fixtures.auditLogMapper).insert(any(BiAuditLogDO.class));
    }

    @Test
    void datasourceUseRequiresExplicitGrantForDatasetCreation() {
        Fixtures fixtures = fixtures();
        when(fixtures.resourcePermissionMapper.selectList(any())).thenReturn(List.of());

        assertThatThrownBy(() -> fixtures.service.enforceResourceAccess(
                7L,
                0L,
                "DATASOURCE",
                71L,
                new BiQueryContext(7L, "alice", RoleNames.OPERATOR),
                BiPermissionService.ACTION_USE))
                .isInstanceOf(BiPermissionService.BiPermissionDeniedException.class)
                .hasMessageContaining("permission is required");
    }

    @Test
    void datasourceUseGrantAllowsDatasetCreation() {
        Fixtures fixtures = fixtures();
        BiResourcePermissionDO grant = resourcePermission(
                "ROLE",
                RoleNames.OPERATOR,
                BiPermissionService.ACTION_USE,
                "ALLOW");
        grant.setResourceType("DATASOURCE");
        grant.setResourceId(71L);
        grant.setWorkspaceId(0L);
        when(fixtures.resourcePermissionMapper.selectList(any())).thenReturn(List.of(grant));

        fixtures.service.enforceResourceAccess(
                7L,
                0L,
                "DATASOURCE",
                71L,
                new BiQueryContext(7L, "alice", RoleNames.OPERATOR),
                BiPermissionService.ACTION_USE);
    }

    @Test
    void workspaceMemberRoleAllowsResourcePermissionActions() {
        Fixtures fixtures = fixtures();
        when(fixtures.workspaceMemberMapper.selectOne(any())).thenReturn(workspaceMember("BI_ANALYST"));
        when(fixtures.resourcePermissionMapper.selectList(any())).thenReturn(List.of(resourcePermission(
                "ROLE", "BI_ANALYST", BiPermissionService.ACTION_EXPORT, "ALLOW")));

        fixtures.service.enforceResourceAccess(
                7L,
                3L,
                "DATASET",
                11L,
                new BiQueryContext(7L, "alice", RoleNames.OPERATOR),
                BiPermissionService.ACTION_EXPORT);
    }

    @Test
    void systemRoleCanDeliverSubscriptionsWithoutExplicitGrant() {
        Fixtures fixtures = fixtures();
        when(fixtures.resourcePermissionMapper.selectList(any())).thenReturn(List.of());

        fixtures.service.enforceResourceAccess(
                7L,
                3L,
                "DASHBOARD",
                21L,
                new BiQueryContext(7L, "bi-delivery-scheduler", "SYSTEM"),
                BiPermissionService.ACTION_SUBSCRIBE);

        verify(fixtures.auditLogMapper).insert(any(BiAuditLogDO.class));
    }

    @Test
    void workspaceMemberRoleAppliesRowPermissionsDuringQuery() {
        Fixtures fixtures = fixtures();
        when(fixtures.workspaceMemberMapper.selectOne(any())).thenReturn(workspaceMember("BI_VIEWER"));
        when(fixtures.resourcePermissionMapper.selectList(any())).thenReturn(List.of());
        when(fixtures.rowPermissionMapper.selectList(any())).thenReturn(List.of(rowPermission(
                "viewer-canvas",
                "ROLE",
                "BI_VIEWER",
                "{\"filters\":[{\"field\":\"canvas_id\",\"operator\":\"EQ\",\"value\":42}]}")));
        when(fixtures.columnPermissionMapper.selectList(any())).thenReturn(List.of());

        BiPermissionService.BiPreparedQuery prepared = fixtures.service.prepareQuery(
                dataset(),
                request(List.of("stat_date"), List.of("total_executions")),
                new BiQueryContext(7L, "alice", RoleNames.OPERATOR),
                BiPermissionService.ACTION_USE);

        assertThat(prepared.request().filters()).extracting(BiFilter::field)
                .containsExactly("canvas_id");
        assertThat(prepared.permissionSignature()).contains("viewer-canvas");
    }

    @Test
    void columnPermissionDenyBlocksSensitiveFieldUsageAndAudits() {
        Fixtures fixtures = fixtures();
        when(fixtures.resourcePermissionMapper.selectList(any())).thenReturn(List.of());
        when(fixtures.rowPermissionMapper.selectList(any())).thenReturn(List.of());
        when(fixtures.columnPermissionMapper.selectList(any())).thenReturn(List.of(columnPermission(
                "canvas_id", "USER", "alice", "DENY", null)));

        assertThatThrownBy(() -> fixtures.service.prepareQuery(
                dataset(),
                request(List.of("canvas_id"), List.of("total_executions")),
                new BiQueryContext(7L, "alice", RoleNames.OPERATOR),
                BiPermissionService.ACTION_USE))
                .isInstanceOf(BiPermissionService.BiPermissionDeniedException.class)
                .hasMessageContaining("column permission DENY");

        verify(fixtures.auditLogMapper).insert(any(BiAuditLogDO.class));
    }

    @Test
    void columnPermissionMaskAppliesToReturnedRowsAndCacheSignature() {
        Fixtures fixtures = fixtures();
        when(fixtures.resourcePermissionMapper.selectList(any())).thenReturn(List.of());
        when(fixtures.rowPermissionMapper.selectList(any())).thenReturn(List.of());
        when(fixtures.columnPermissionMapper.selectList(any())).thenReturn(List.of(columnPermission(
                "canvas_name",
                "ROLE",
                RoleNames.OPERATOR,
                "MASK",
                "{\"strategy\":\"FIXED\",\"replacement\":\"MASKED\"}")));

        BiPermissionService.BiPreparedQuery prepared = fixtures.service.prepareQuery(
                dataset(),
                request(List.of("canvas_name"), List.of("total_executions")),
                new BiQueryContext(7L, "alice", RoleNames.OPERATOR),
                BiPermissionService.ACTION_USE);

        List<Map<String, Object>> masked = fixtures.service.applyMasks(
                List.of(Map.of("canvas_name", "Welcome Flow", "total_executions", 9L)),
                prepared.columnMasks());

        assertThat(prepared.columnMasks()).extracting(BiPermissionService.BiColumnMask::fieldKey)
                .containsExactly("canvas_name");
        assertThat(prepared.permissionSignature()).contains("canvas_name");
        assertThat(masked).singleElement()
                .satisfies(row -> assertThat(row).containsEntry("canvas_name", "MASKED"));
    }

    @Test
    void menuVisibilitySupportsAllowAndDenyLists() {
        Fixtures fixtures = fixtures();
        List<BiPortalMenuResource> menus = fixtures.service.visibleMenus(List.of(
                menu("ops", Map.of("roles", List.of(RoleNames.OPERATOR))),
                menu("admin", Map.of("roles", List.of(RoleNames.TENANT_ADMIN))),
                menu("blocked", Map.of("roles", List.of(RoleNames.OPERATOR), "denyUsers", List.of("alice")))),
                new BiQueryContext(7L, "alice", RoleNames.OPERATOR));

        assertThat(menus).extracting(BiPortalMenuResource::menuKey)
                .containsExactly("ops");
    }

    private Fixtures fixtures() {
        BiDatasetMapper datasetMapper = mock(BiDatasetMapper.class);
        BiResourcePermissionMapper resourcePermissionMapper = mock(BiResourcePermissionMapper.class);
        BiRowPermissionMapper rowPermissionMapper = mock(BiRowPermissionMapper.class);
        BiColumnPermissionMapper columnPermissionMapper = mock(BiColumnPermissionMapper.class);
        BiWorkspaceMemberMapper workspaceMemberMapper = mock(BiWorkspaceMemberMapper.class);
        BiAuditLogMapper auditLogMapper = mock(BiAuditLogMapper.class);
        when(datasetMapper.selectOne(any())).thenReturn(datasetRow());
        return new Fixtures(
                datasetMapper,
                resourcePermissionMapper,
                rowPermissionMapper,
                columnPermissionMapper,
                workspaceMemberMapper,
                auditLogMapper,
                new BiPermissionService(
                        datasetMapper,
                        resourcePermissionMapper,
                        rowPermissionMapper,
                        columnPermissionMapper,
                        workspaceMemberMapper,
                        auditLogMapper,
                        new ObjectMapper()));
    }

    private BiQueryRequest request(List<String> dimensions, List<String> metrics) {
        return new BiQueryRequest("canvas_daily_stats", dimensions, metrics, List.of(), List.of(), 100);
    }

    private BiDatasetSpec dataset() {
        return MarketingBiDatasetRegistry.dataset("canvas_daily_stats");
    }

    private BiDatasetDO datasetRow() {
        BiDatasetDO row = new BiDatasetDO();
        row.setId(11L);
        row.setTenantId(0L);
        row.setWorkspaceId(3L);
        row.setDatasetKey("canvas_daily_stats");
        row.setStatus("PUBLISHED");
        return row;
    }

    private BiResourcePermissionDO resourcePermission(String subjectType,
                                                      String subjectId,
                                                      String actionKey,
                                                      String effect) {
        BiResourcePermissionDO row = new BiResourcePermissionDO();
        row.setTenantId(0L);
        row.setWorkspaceId(3L);
        row.setResourceType("DATASET");
        row.setResourceId(11L);
        row.setSubjectType(subjectType);
        row.setSubjectId(subjectId);
        row.setActionKey(actionKey);
        row.setEffect(effect);
        return row;
    }

    private BiRowPermissionDO rowPermission(String ruleKey,
                                            String subjectType,
                                            String subjectId,
                                            String filterJson) {
        BiRowPermissionDO row = new BiRowPermissionDO();
        row.setTenantId(0L);
        row.setDatasetId(11L);
        row.setRuleKey(ruleKey);
        row.setSubjectType(subjectType);
        row.setSubjectId(subjectId);
        row.setFilterJson(filterJson);
        row.setEnabled(true);
        return row;
    }

    private BiWorkspaceMemberDO workspaceMember(String roleKey) {
        BiWorkspaceMemberDO row = new BiWorkspaceMemberDO();
        row.setId(41L);
        row.setTenantId(7L);
        row.setWorkspaceId(3L);
        row.setUserId("alice");
        row.setRoleKey(roleKey);
        return row;
    }

    private BiColumnPermissionDO columnPermission(String fieldKey,
                                                  String subjectType,
                                                  String subjectId,
                                                  String policy,
                                                  String maskJson) {
        BiColumnPermissionDO row = new BiColumnPermissionDO();
        row.setTenantId(0L);
        row.setDatasetId(11L);
        row.setFieldKey(fieldKey);
        row.setSubjectType(subjectType);
        row.setSubjectId(subjectId);
        row.setPolicy(policy);
        row.setMaskJson(maskJson);
        row.setEnabled(true);
        return row;
    }

    private BiPortalMenuResource menu(String menuKey, Map<String, Object> visibility) {
        return new BiPortalMenuResource(
                menuKey,
                null,
                menuKey,
                "DASHBOARD",
                "canvas-effect",
                21L,
                null,
                visibility,
                10);
    }

    private record Fixtures(
            BiDatasetMapper datasetMapper,
            BiResourcePermissionMapper resourcePermissionMapper,
            BiRowPermissionMapper rowPermissionMapper,
            BiColumnPermissionMapper columnPermissionMapper,
            BiWorkspaceMemberMapper workspaceMemberMapper,
            BiAuditLogMapper auditLogMapper,
            BiPermissionService service) {
    }
}
