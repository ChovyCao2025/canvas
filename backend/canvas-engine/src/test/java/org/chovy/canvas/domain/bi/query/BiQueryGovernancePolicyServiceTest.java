package org.chovy.canvas.domain.bi.query;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.dal.dataobject.BiAuditLogDO;
import org.chovy.canvas.dal.dataobject.BiQueryGovernancePolicyDO;
import org.chovy.canvas.dal.mapper.BiAuditLogMapper;
import org.chovy.canvas.dal.mapper.BiQueryGovernancePolicyMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BiQueryGovernancePolicyServiceTest {

    @Test
    void readsTenantPolicyWithDatasetOverrides() {
        BiQueryGovernancePolicyMapper mapper = mock(BiQueryGovernancePolicyMapper.class);
        when(mapper.selectList(any())).thenReturn(List.of(
                row(7L, BiQueryGovernancePolicyService.DEFAULT_DATASET_KEY, 12_000L, 900_000, "ops"),
                row(7L, "canvas_daily_stats", 15_000L, 120_000, "ops")
        ));
        BiQueryGovernancePolicyService service = new BiQueryGovernancePolicyService(mapper, mock(BiAuditLogMapper.class), new ObjectMapper());

        BiQueryGovernancePolicy policy = service.currentPolicy(7L);

        assertThat(policy.defaultTimeoutMs()).isEqualTo(12_000L);
        assertThat(policy.defaultQuotaRows()).isEqualTo(900_000);
        assertThat(policy.datasetPolicy("canvas_daily_stats").timeoutMs()).isEqualTo(15_000L);
        assertThat(policy.datasetPolicy("canvas_daily_stats").quotaRows()).isEqualTo(120_000);
    }

    @Test
    void upsertsDefaultAndDatasetPoliciesForTenant() {
        BiQueryGovernancePolicyMapper mapper = mock(BiQueryGovernancePolicyMapper.class);
        BiQueryGovernancePolicyDO existingDefault = row(7L, BiQueryGovernancePolicyService.DEFAULT_DATASET_KEY, 30_000L, 1_000_000, "system");
        existingDefault.setId(11L);
        when(mapper.selectList(any())).thenReturn(List.of(existingDefault));
        BiQueryGovernancePolicyService service = new BiQueryGovernancePolicyService(mapper, mock(BiAuditLogMapper.class), new ObjectMapper());

        service.upsertPolicy(7L, new BiQueryGovernancePolicyUpdateCommand(
                20_000L,
                500_000,
                List.of(new BiQueryGovernancePolicyUpdateCommand.DatasetPolicyCommand(
                        "canvas_daily_stats",
                        8_000L,
                        100_000))),
                "admin");

        ArgumentCaptor<BiQueryGovernancePolicyDO> updated = ArgumentCaptor.forClass(BiQueryGovernancePolicyDO.class);
        ArgumentCaptor<BiQueryGovernancePolicyDO> inserted = ArgumentCaptor.forClass(BiQueryGovernancePolicyDO.class);
        verify(mapper).updateById(updated.capture());
        verify(mapper).insert(inserted.capture());
        assertThat(updated.getValue().getId()).isEqualTo(11L);
        assertThat(updated.getValue().getTenantId()).isEqualTo(7L);
        assertThat(updated.getValue().getDatasetKey()).isEqualTo(BiQueryGovernancePolicyService.DEFAULT_DATASET_KEY);
        assertThat(updated.getValue().getTimeoutMs()).isEqualTo(20_000L);
        assertThat(updated.getValue().getQuotaRows()).isEqualTo(500_000);
        assertThat(updated.getValue().getUpdatedBy()).isEqualTo("admin");
        assertThat(inserted.getValue().getTenantId()).isEqualTo(7L);
        assertThat(inserted.getValue().getDatasetKey()).isEqualTo("canvas_daily_stats");
        assertThat(inserted.getValue().getTimeoutMs()).isEqualTo(8_000L);
        assertThat(inserted.getValue().getQuotaRows()).isEqualTo(100_000);
        assertThat(inserted.getValue().getUpdatedBy()).isEqualTo("admin");
    }

    @Test
    void auditsPolicyUpdateWithBeforeAndAfterSnapshots() throws Exception {
        BiQueryGovernancePolicyMapper mapper = mock(BiQueryGovernancePolicyMapper.class);
        BiAuditLogMapper auditLogMapper = mock(BiAuditLogMapper.class);
        BiQueryGovernancePolicyDO existingDefault = row(7L, BiQueryGovernancePolicyService.DEFAULT_DATASET_KEY, 30_000L, 1_000_000, "system");
        existingDefault.setId(11L);
        BiQueryGovernancePolicyDO existingDataset = row(7L, "canvas_daily_stats", 10_000L, 200_000, "system");
        existingDataset.setId(12L);
        when(mapper.selectList(any())).thenReturn(
                List.of(existingDefault, existingDataset),
                List.of(
                        row(7L, BiQueryGovernancePolicyService.DEFAULT_DATASET_KEY, 20_000L, 500_000, "admin"),
                        row(7L, "canvas_daily_stats", 8_000L, 100_000, "admin")
                ));
        ObjectMapper objectMapper = new ObjectMapper();
        BiQueryGovernancePolicyService service = new BiQueryGovernancePolicyService(mapper, auditLogMapper, objectMapper);

        service.upsertPolicy(7L, new BiQueryGovernancePolicyUpdateCommand(
                        20_000L,
                        500_000,
                        List.of(new BiQueryGovernancePolicyUpdateCommand.DatasetPolicyCommand(
                                "canvas_daily_stats",
                                8_000L,
                                100_000))),
                "admin");

        ArgumentCaptor<BiAuditLogDO> audit = ArgumentCaptor.forClass(BiAuditLogDO.class);
        verify(auditLogMapper).insert(audit.capture());
        assertThat(audit.getValue().getTenantId()).isEqualTo(7L);
        assertThat(audit.getValue().getActorId()).isEqualTo("admin");
        assertThat(audit.getValue().getActionKey()).isEqualTo("BI_QUERY_GOVERNANCE_POLICY_UPDATE");
        assertThat(audit.getValue().getResourceType()).isEqualTo("BI_QUERY_GOVERNANCE_POLICY");
        assertThat(audit.getValue().getCreatedAt()).isNotNull();
        JsonNode detail = objectMapper.readTree(audit.getValue().getDetailJson());
        assertThat(detail.path("before").path("defaultTimeoutMs").asLong()).isEqualTo(30_000L);
        assertThat(detail.path("after").path("defaultTimeoutMs").asLong()).isEqualTo(20_000L);
        assertThat(detail.path("before").path("datasets").path("canvas_daily_stats").path("quotaRows").asInt())
                .isEqualTo(200_000);
        assertThat(detail.path("after").path("datasets").path("canvas_daily_stats").path("quotaRows").asInt())
                .isEqualTo(100_000);
    }

    @Test
    void appliesPolicyUpdateWhenAuditStorageFails() {
        BiQueryGovernancePolicyMapper mapper = mock(BiQueryGovernancePolicyMapper.class);
        BiAuditLogMapper auditLogMapper = mock(BiAuditLogMapper.class);
        when(mapper.selectList(any())).thenReturn(List.of(), List.of(
                row(7L, BiQueryGovernancePolicyService.DEFAULT_DATASET_KEY, 20_000L, 500_000, "admin")
        ));
        when(auditLogMapper.insert(any(BiAuditLogDO.class))).thenThrow(new IllegalStateException("audit unavailable"));
        BiQueryGovernancePolicyService service = new BiQueryGovernancePolicyService(mapper, auditLogMapper, new ObjectMapper());

        assertThatCode(() -> service.upsertPolicy(7L, new BiQueryGovernancePolicyUpdateCommand(
                20_000L,
                500_000,
                List.of()),
                "admin")).doesNotThrowAnyException();
    }

    @Test
    void listsRecentTenantGovernanceAuditEntriesNewestFirst() {
        BiQueryGovernancePolicyMapper mapper = mock(BiQueryGovernancePolicyMapper.class);
        BiAuditLogMapper auditLogMapper = mock(BiAuditLogMapper.class);
        when(auditLogMapper.selectList(any())).thenReturn(List.of(
                auditRow(101L, 7L, "alice", "BI_QUERY_GOVERNANCE_POLICY_UPDATE",
                        "BI_QUERY_GOVERNANCE_POLICY", "{\"after\":{\"defaultTimeoutMs\":20000}}",
                        "2026-06-05T08:20:00"),
                auditRow(102L, 9L, "mallory", "BI_QUERY_GOVERNANCE_POLICY_UPDATE",
                        "BI_QUERY_GOVERNANCE_POLICY", "{}",
                        "2026-06-05T08:25:00"),
                auditRow(103L, 7L, "alice", "BI_QUERY_EXECUTE",
                        "BI_QUERY", "{}",
                        "2026-06-05T08:30:00"),
                auditRow(104L, 7L, "bob", "BI_QUERY_GOVERNANCE_POLICY_UPDATE",
                        "BI_QUERY_GOVERNANCE_POLICY", "{\"after\":{\"defaultQuotaRows\":500000}}",
                        "2026-06-05T08:10:00")
        ));
        BiQueryGovernancePolicyService service = new BiQueryGovernancePolicyService(mapper, auditLogMapper, new ObjectMapper());

        List<BiQueryGovernanceAuditEntry> entries = service.recentAudit(7L, 2);

        assertThat(entries).extracting(BiQueryGovernanceAuditEntry::id).containsExactly(101L, 104L);
        assertThat(entries).extracting(BiQueryGovernanceAuditEntry::actorId).containsExactly("alice", "bob");
        assertThat(entries).extracting(BiQueryGovernanceAuditEntry::detailJson)
                .containsExactly("{\"after\":{\"defaultTimeoutMs\":20000}}",
                        "{\"after\":{\"defaultQuotaRows\":500000}}");
    }

    private BiQueryGovernancePolicyDO row(Long tenantId, String datasetKey, Long timeoutMs, Integer quotaRows, String updatedBy) {
        BiQueryGovernancePolicyDO row = new BiQueryGovernancePolicyDO();
        row.setTenantId(tenantId);
        row.setDatasetKey(datasetKey);
        row.setTimeoutMs(timeoutMs);
        row.setQuotaRows(quotaRows);
        row.setUpdatedBy(updatedBy);
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
        row.setActorId(actorId);
        row.setActionKey(actionKey);
        row.setResourceType(resourceType);
        row.setDetailJson(detailJson);
        row.setCreatedAt(java.time.LocalDateTime.parse(createdAt));
        return row;
    }
}
