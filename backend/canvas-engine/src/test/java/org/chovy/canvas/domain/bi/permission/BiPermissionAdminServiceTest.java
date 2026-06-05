package org.chovy.canvas.domain.bi.permission;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.common.tenant.RoleNames;
import org.chovy.canvas.dal.dataobject.BiColumnPermissionDO;
import org.chovy.canvas.dal.dataobject.BiDatasetDO;
import org.chovy.canvas.dal.dataobject.BiResourcePermissionDO;
import org.chovy.canvas.dal.dataobject.BiRowPermissionDO;
import org.chovy.canvas.dal.mapper.BiChartMapper;
import org.chovy.canvas.dal.mapper.BiColumnPermissionMapper;
import org.chovy.canvas.dal.mapper.BiDashboardMapper;
import org.chovy.canvas.dal.mapper.BiDatasetMapper;
import org.chovy.canvas.dal.mapper.BiPortalMapper;
import org.chovy.canvas.dal.mapper.BiResourcePermissionMapper;
import org.chovy.canvas.dal.mapper.BiRowPermissionMapper;
import org.chovy.canvas.domain.bi.query.BiFilter;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
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

    private Fixture fixture() {
        BiDatasetMapper datasetMapper = mock(BiDatasetMapper.class);
        BiDashboardMapper dashboardMapper = mock(BiDashboardMapper.class);
        BiChartMapper chartMapper = mock(BiChartMapper.class);
        BiPortalMapper portalMapper = mock(BiPortalMapper.class);
        BiResourcePermissionMapper resourcePermissionMapper = mock(BiResourcePermissionMapper.class);
        BiRowPermissionMapper rowPermissionMapper = mock(BiRowPermissionMapper.class);
        BiColumnPermissionMapper columnPermissionMapper = mock(BiColumnPermissionMapper.class);
        return new Fixture(
                datasetMapper,
                resourcePermissionMapper,
                rowPermissionMapper,
                columnPermissionMapper,
                new BiPermissionAdminService(
                        datasetMapper,
                        dashboardMapper,
                        chartMapper,
                        portalMapper,
                        resourcePermissionMapper,
                        rowPermissionMapper,
                        columnPermissionMapper,
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

    private BiResourcePermissionDO resourcePermission(Long id,
                                                      Long resourceId,
                                                      String subjectType,
                                                      String subjectId,
                                                      String actionKey,
                                                      String effect) {
        BiResourcePermissionDO row = new BiResourcePermissionDO();
        row.setId(id);
        row.setTenantId(7L);
        row.setWorkspaceId(3L);
        row.setResourceType("DATASET");
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

    private record Fixture(
            BiDatasetMapper datasetMapper,
            BiResourcePermissionMapper resourcePermissionMapper,
            BiRowPermissionMapper rowPermissionMapper,
            BiColumnPermissionMapper columnPermissionMapper,
            BiPermissionAdminService service) {
    }
}
