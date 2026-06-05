package org.chovy.canvas.domain.warehouse;

import org.chovy.canvas.common.tenant.RoleNames;
import org.chovy.canvas.dal.dataobject.CdpWarehouseFieldAccessAuditDO;
import org.chovy.canvas.dal.dataobject.CdpWarehouseFieldPolicyDO;
import org.chovy.canvas.dal.mapper.CdpWarehouseFieldAccessAuditMapper;
import org.chovy.canvas.dal.mapper.CdpWarehouseFieldPolicyMapper;
import org.chovy.canvas.domain.bi.query.BiFilter;
import org.chovy.canvas.domain.bi.query.BiQueryContext;
import org.chovy.canvas.domain.bi.query.BiQueryRequest;
import org.chovy.canvas.domain.bi.query.BiSort;
import org.chovy.canvas.domain.bi.query.MarketingBiDatasetRegistry;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CdpWarehouseFieldGovernanceServiceTest {

    @Test
    void upsertPolicyNormalizesAndPersistsTenantPolicy() {
        CdpWarehouseFieldPolicyMapper policyMapper = mock(CdpWarehouseFieldPolicyMapper.class);
        CdpWarehouseFieldGovernanceService service = service(policyMapper,
                mock(CdpWarehouseFieldAccessAuditMapper.class));

        CdpWarehouseFieldGovernanceService.FieldPolicyView result = service.upsertPolicy(9L,
                new CdpWarehouseFieldGovernanceService.FieldPolicyCommand(
                        "canvas_daily_stats", "canvas_id", "canvas_dws.canvas_daily_stats",
                        "canvas_id", "number", "id", "normal", "allow",
                        "operator", "select,filter", null, null, "data-platform",
                        "tenant override"));

        assertThat(result.tenantId()).isEqualTo(9L);
        assertThat(result.valueType()).isEqualTo("NUMBER");
        assertThat(result.semanticType()).isEqualTo("ID");
        assertThat(result.accessPolicy()).isEqualTo("ALLOW");
        assertThat(result.lifecycleStatus()).isEqualTo("ACTIVE");

        ArgumentCaptor<CdpWarehouseFieldPolicyDO> row =
                ArgumentCaptor.forClass(CdpWarehouseFieldPolicyDO.class);
        verify(policyMapper).upsert(row.capture());
        assertThat(row.getValue().getAllowedUsages()).contains("SELECT", "FILTER");
        assertThat(row.getValue().getOwnerName()).isEqualTo("data-platform");
    }

    @Test
    void upsertPolicyRejectsMissingRequiredFields() {
        CdpWarehouseFieldGovernanceService service = service(mock(CdpWarehouseFieldPolicyMapper.class),
                mock(CdpWarehouseFieldAccessAuditMapper.class));

        assertThatThrownBy(() -> service.upsertPolicy(9L,
                new CdpWarehouseFieldGovernanceService.FieldPolicyCommand(
                        "canvas_daily_stats", "", "canvas_dws.canvas_daily_stats",
                        "canvas_id", "NUMBER", null, null, null, null,
                        null, null, null, null, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("fieldKey is required");
    }

    @Test
    void listPoliciesMergesBuiltInAndTenantOverride() {
        CdpWarehouseFieldPolicyMapper policyMapper = mock(CdpWarehouseFieldPolicyMapper.class);
        when(policyMapper.selectList(any())).thenReturn(List.of(
                policy(1L, 0L, "canvas_daily_stats", "stat_date", "ALLOW", RoleNames.OPERATOR,
                        "SELECT,FILTER,SORT,GROUP"),
                policy(2L, 0L, "canvas_daily_stats", "canvas_id", "ALLOW", RoleNames.OPERATOR,
                        "SELECT,FILTER,SORT,GROUP"),
                policy(3L, 9L, "canvas_daily_stats", "canvas_id", "MASK", RoleNames.TENANT_ADMIN,
                        "SELECT,FILTER")
        ));
        CdpWarehouseFieldGovernanceService service = service(policyMapper,
                mock(CdpWarehouseFieldAccessAuditMapper.class));

        List<CdpWarehouseFieldGovernanceService.FieldPolicyView> rows =
                service.listPolicies(9L, "canvas_daily_stats", "ACTIVE");

        assertThat(rows).extracting(CdpWarehouseFieldGovernanceService.FieldPolicyView::fieldKey)
                .containsExactly("stat_date", "canvas_id");
        assertThat(rows.get(1).tenantId()).isEqualTo(9L);
        assertThat(rows.get(1).accessPolicy()).isEqualTo("MASK");
        assertThat(rows.get(1).minRole()).isEqualTo(RoleNames.TENANT_ADMIN);
    }

    @Test
    void evaluateBiQueryDeniesMaskedFieldForOperatorAndAudits() {
        CdpWarehouseFieldPolicyMapper policyMapper = mock(CdpWarehouseFieldPolicyMapper.class);
        CdpWarehouseFieldAccessAuditMapper auditMapper = mock(CdpWarehouseFieldAccessAuditMapper.class);
        when(policyMapper.selectList(any())).thenReturn(List.of(
                policy(1L, 0L, "canvas_daily_stats", "canvas_id", "MASK", RoleNames.TENANT_ADMIN,
                        "SELECT,FILTER,GROUP")));
        CdpWarehouseFieldGovernanceService service = service(policyMapper, auditMapper);

        CdpWarehouseFieldGovernanceService.BiPolicyEvaluation evaluation = service.evaluateBiQuery(
                MarketingBiDatasetRegistry.dataset("canvas_daily_stats"),
                request(List.of("canvas_id"), List.of(), List.of(), List.of()),
                new BiQueryContext(9L, "alice", RoleNames.OPERATOR),
                CdpWarehouseFieldGovernanceService.ACTION_BI_EXECUTE);

        assertThat(evaluation.allowed()).isFalse();
        assertThat(evaluation.reason()).contains("below required TENANT_ADMIN");
        assertThat(evaluation.decisions()).anySatisfy(decision -> {
            assertThat(decision.fieldKey()).isEqualTo("canvas_id");
            assertThat(decision.decision()).isEqualTo("DENY");
        });
        ArgumentCaptor<CdpWarehouseFieldAccessAuditDO> audit =
                ArgumentCaptor.forClass(CdpWarehouseFieldAccessAuditDO.class);
        verify(auditMapper, atLeastOnce()).insert(audit.capture());
        assertThat(audit.getAllValues()).anySatisfy(row -> {
            assertThat(row.getTenantId()).isEqualTo(9L);
            assertThat(row.getFieldKey()).isEqualTo("canvas_id");
            assertThat(row.getActorId()).isEqualTo("alice");
            assertThat(row.getActionKey()).isEqualTo(CdpWarehouseFieldGovernanceService.ACTION_BI_EXECUTE);
        });
    }

    @Test
    void evaluateBiQueryAllowsMaskedFieldForTenantAdmin() {
        CdpWarehouseFieldPolicyMapper policyMapper = mock(CdpWarehouseFieldPolicyMapper.class);
        CdpWarehouseFieldAccessAuditMapper auditMapper = mock(CdpWarehouseFieldAccessAuditMapper.class);
        when(policyMapper.selectList(any())).thenReturn(List.of(
                policy(1L, 0L, "canvas_daily_stats", "canvas_id", "MASK", RoleNames.TENANT_ADMIN,
                        "SELECT,FILTER,GROUP")));
        CdpWarehouseFieldGovernanceService service = service(policyMapper, auditMapper);

        CdpWarehouseFieldGovernanceService.BiPolicyEvaluation evaluation = service.evaluateBiQuery(
                MarketingBiDatasetRegistry.dataset("canvas_daily_stats"),
                request(List.of("canvas_id"), List.of(), List.of(), List.of()),
                new BiQueryContext(9L, "alice", RoleNames.TENANT_ADMIN),
                CdpWarehouseFieldGovernanceService.ACTION_BI_EXECUTE);

        assertThat(evaluation.allowed()).isTrue();
        verify(auditMapper, never()).insert(any(CdpWarehouseFieldAccessAuditDO.class));
    }

    @Test
    void evaluateBiQueryDeniesUsageOutsideAllowedUsages() {
        CdpWarehouseFieldPolicyMapper policyMapper = mock(CdpWarehouseFieldPolicyMapper.class);
        CdpWarehouseFieldAccessAuditMapper auditMapper = mock(CdpWarehouseFieldAccessAuditMapper.class);
        when(policyMapper.selectList(any())).thenReturn(List.of(
                policy(1L, 0L, "canvas_daily_stats", "total_executions", "ALLOW", RoleNames.OPERATOR,
                        "SELECT")));
        CdpWarehouseFieldGovernanceService service = service(policyMapper, auditMapper);

        CdpWarehouseFieldGovernanceService.BiPolicyEvaluation evaluation = service.evaluateBiQuery(
                MarketingBiDatasetRegistry.dataset("canvas_daily_stats"),
                request(List.of(), List.of("total_executions"), List.of(),
                        List.of(new BiSort("total_executions", BiSort.Direction.DESC))),
                new BiQueryContext(9L, "alice", RoleNames.OPERATOR),
                CdpWarehouseFieldGovernanceService.ACTION_BI_EXECUTE);

        assertThat(evaluation.allowed()).isFalse();
        assertThat(evaluation.decisions()).anySatisfy(decision -> {
            assertThat(decision.fieldKey()).isEqualTo("total_executions");
            assertThat(decision.usage()).isEqualTo("SORT");
            assertThat(decision.reason()).contains("not allowed");
        });
        verify(auditMapper).insert(any(CdpWarehouseFieldAccessAuditDO.class));
    }

    private CdpWarehouseFieldGovernanceService service(CdpWarehouseFieldPolicyMapper policyMapper,
                                                       CdpWarehouseFieldAccessAuditMapper auditMapper) {
        return new CdpWarehouseFieldGovernanceService(policyMapper, auditMapper);
    }

    private BiQueryRequest request(List<String> dimensions,
                                   List<String> metrics,
                                   List<BiFilter> filters,
                                   List<BiSort> sorts) {
        return new BiQueryRequest("canvas_daily_stats", dimensions, metrics, filters, sorts, 100);
    }

    private CdpWarehouseFieldPolicyDO policy(Long id,
                                             Long tenantId,
                                             String datasetKey,
                                             String fieldKey,
                                             String accessPolicy,
                                             String minRole,
                                             String allowedUsages) {
        CdpWarehouseFieldPolicyDO row = new CdpWarehouseFieldPolicyDO();
        row.setId(id);
        row.setTenantId(tenantId);
        row.setDatasetKey(datasetKey);
        row.setFieldKey(fieldKey);
        row.setPhysicalName("canvas_dws." + datasetKey);
        row.setColumnName(fieldKey);
        row.setValueType("NUMBER");
        row.setSemanticType("ID");
        row.setPiiLevel("PII_RELATED");
        row.setAccessPolicy(accessPolicy);
        row.setMinRole(minRole);
        row.setAllowedUsages(allowedUsages);
        row.setLifecycleStatus("ACTIVE");
        row.setOwnerName("data-platform");
        row.setDescription("policy");
        return row;
    }
}
