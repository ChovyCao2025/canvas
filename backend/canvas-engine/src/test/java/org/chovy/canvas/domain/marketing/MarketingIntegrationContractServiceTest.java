package org.chovy.canvas.domain.marketing;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.dal.dataobject.MarketingIntegrationContractAuditEventDO;
import org.chovy.canvas.dal.dataobject.MarketingIntegrationContractDO;
import org.chovy.canvas.dal.mapper.MarketingIntegrationContractAuditEventMapper;
import org.chovy.canvas.dal.mapper.MarketingIntegrationContractMapper;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MarketingIntegrationContractServiceTest {

    @Test
    void upsertContractNormalizesAndInsertsTenantScopedRecord() {
        Harness harness = harness();
        when(harness.contractMapper.selectOne(any())).thenReturn(null);
        when(harness.auditMapper.selectOne(any())).thenReturn(null);
        doAnswer(invocation -> {
            MarketingIntegrationContractDO row = invocation.getArgument(0);
            row.setId(100L);
            return 1;
        }).when(harness.contractMapper).insert(any(MarketingIntegrationContractDO.class));

        MarketingIntegrationContractView view = harness.service.upsertContract(7L, command(), "operator-1");

        assertThat(view.id()).isEqualTo(100L);
        assertThat(view.contractKey()).isEqualTo("google-ads-keyword-write");
        assertThat(view.providerFamily()).isEqualTo("SEM");
        assertThat(view.direction()).isEqualTo("OUTBOUND");
        assertThat(view.environment()).isEqualTo("PRODUCTION");
        assertThat(view.authMode()).isEqualTo("OAUTH");
        assertThat(view.status()).isEqualTo("ACTIVE");
        assertThat(view.retryPolicy()).containsEntry("maxAttempts", 3);
        assertThat(view.schemaContract()).containsEntry("request", "googleAdsMutate");
        verify(harness.contractMapper).insert(argThat((MarketingIntegrationContractDO row) ->
                row.getTenantId().equals(7L)
                        && row.getContractKey().equals("google-ads-keyword-write")
                        && row.getSourceCapabilityKey().equals("search-marketing-governance")
                        && row.getTargetCapabilityKey().equals("provider-credential-governance")
                        && row.getAssetKey().equals("search-provider-write-gateway")
                        && row.getTimeoutMs().equals(45000)
                        && row.getRetryPolicyJson().contains("maxAttempts")
                        && row.getSchemaContractJson().contains("googleAdsMutate")
                        && row.getCreatedBy().equals("operator-1")
                        && row.getUpdatedBy().equals("operator-1")));
        verify(harness.auditMapper).insert(argThat((MarketingIntegrationContractAuditEventDO row) ->
                row.getTenantId().equals(7L)
                        && row.getContractId().equals(100L)
                        && row.getContractKey().equals("google-ads-keyword-write")
                        && row.getRevision().equals(1)
                        && row.getEventType().equals("CREATED")
                        && row.getSnapshotJson().contains("google-ads-keyword-write")
                        && row.getChangedBy().equals("operator-1")));
    }

    @Test
    void upsertContractUpdatesExistingTenantKey() {
        Harness harness = harness();
        MarketingIntegrationContractDO existing = contract(10L, 7L, "google-ads-keyword-write", "DRAFT");
        when(harness.contractMapper.selectOne(any())).thenReturn(existing);
        when(harness.auditMapper.selectOne(any())).thenReturn(auditEvent(1, "CREATED"));

        MarketingIntegrationContractView view = harness.service.upsertContract(7L, command(), "operator-2");

        assertThat(view.id()).isEqualTo(10L);
        assertThat(view.status()).isEqualTo("ACTIVE");
        verify(harness.contractMapper).updateById(argThat((MarketingIntegrationContractDO row) ->
                row.getId().equals(10L)
                        && row.getContractKey().equals("google-ads-keyword-write")
                        && row.getUpdatedBy().equals("operator-2")));
        verify(harness.auditMapper).insert(argThat((MarketingIntegrationContractAuditEventDO row) ->
                row.getTenantId().equals(7L)
                        && row.getContractId().equals(10L)
                        && row.getRevision().equals(2)
                        && row.getEventType().equals("UPDATED")
                        && "DRAFT".equals(row.getPreviousStatus())
                        && "ACTIVE".equals(row.getNewStatus())
                        && row.getChangedFieldsJson().contains("status")
                        && row.getChangedBy().equals("operator-2")));
        verify(harness.contractMapper, never()).insert(any(MarketingIntegrationContractDO.class));
    }

    @Test
    void listContractsValidatesFiltersAndLimits() {
        Harness harness = harness();
        when(harness.contractMapper.selectList(any()))
                .thenReturn(List.of(contract(10L, 7L, "google-ads-keyword-write", "ACTIVE")));

        List<MarketingIntegrationContractView> rows =
                harness.service.listContracts(7L, "active", "sem", 500);

        assertThat(rows).singleElement()
                .satisfies(row -> {
                    assertThat(row.contractKey()).isEqualTo("google-ads-keyword-write");
                    assertThat(row.status()).isEqualTo("ACTIVE");
                });

        assertThatThrownBy(() -> harness.service.listContracts(7L, "launching", null, 10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("unsupported integration contract status");
    }

    @Test
    void upsertContractRejectsInvalidStatusDirectionAuthAndTimeout() {
        Harness harness = harness();
        when(harness.contractMapper.selectOne(any())).thenReturn(null);

        assertThatThrownBy(() -> harness.service.upsertContract(
                7L,
                new MarketingIntegrationContractCommand(
                        "contract",
                        "Contract",
                        "SEM",
                        "search",
                        "credential",
                        "asset",
                        "SIDEWAYS",
                        "PRODUCTION",
                        "OAUTH",
                        "credential",
                        "/canvas/search-marketing/mutations",
                        "Growth",
                        "ACTIVE",
                        "STANDARD",
                        30000,
                        Map.of(),
                        Map.of(),
                        Map.of()),
                "operator-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("unsupported integration direction");

        assertThatThrownBy(() -> harness.service.upsertContract(
                7L,
                commandWith("ACTIVE", "OUTBOUND", "PASSWORD", 30000),
                "operator-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("unsupported integration auth mode");

        assertThatThrownBy(() -> harness.service.upsertContract(
                7L,
                commandWith("LIVE", "OUTBOUND", "OAUTH", 30000),
                "operator-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("unsupported integration contract status");

        assertThatThrownBy(() -> harness.service.upsertContract(
                7L,
                commandWith("ACTIVE", "OUTBOUND", "OAUTH", 500),
                "operator-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("timeoutMs must be between");
    }

    @Test
    void archiveContractEnforcesTenantOwnership() {
        Harness harness = harness();
        when(harness.contractMapper.selectById(10L))
                .thenReturn(contract(10L, 7L, "google-ads-keyword-write", "ACTIVE"));
        when(harness.auditMapper.selectOne(any())).thenReturn(auditEvent(2, "UPDATED"));

        MarketingIntegrationContractView archived = harness.service.archiveContract(7L, 10L, "operator-1");

        assertThat(archived.status()).isEqualTo("ARCHIVED");
        verify(harness.contractMapper).updateById(argThat((MarketingIntegrationContractDO row) ->
                row.getId().equals(10L)
                        && row.getStatus().equals("ARCHIVED")
                        && row.getUpdatedBy().equals("operator-1")));
        verify(harness.auditMapper).insert(argThat((MarketingIntegrationContractAuditEventDO row) ->
                row.getTenantId().equals(7L)
                        && row.getContractId().equals(10L)
                        && row.getRevision().equals(3)
                        && row.getEventType().equals("ARCHIVED")
                        && "ACTIVE".equals(row.getPreviousStatus())
                        && "ARCHIVED".equals(row.getNewStatus())
                        && row.getChangedBy().equals("operator-1")));

        when(harness.contractMapper.selectById(11L))
                .thenReturn(contract(11L, 8L, "foreign-contract", "ACTIVE"));
        assertThatThrownBy(() -> harness.service.archiveContract(7L, 11L, "operator-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("integration contract does not belong to tenant");
    }

    @Test
    void listAuditEventsEnforcesTenantAndBoundsLimit() {
        Harness harness = harness();
        when(harness.contractMapper.selectById(10L))
                .thenReturn(contract(10L, 7L, "google-ads-keyword-write", "ACTIVE"));
        when(harness.auditMapper.selectList(any()))
                .thenReturn(List.of(auditEvent(3, "ARCHIVED"), auditEvent(2, "UPDATED")));

        List<MarketingIntegrationContractAuditEventView> events =
                harness.service.listAuditEvents(7L, 10L, 500);

        assertThat(events).hasSize(2);
        assertThat(events.get(0).revision()).isEqualTo(3);
        assertThat(events.get(0).eventType()).isEqualTo("ARCHIVED");

        when(harness.contractMapper.selectById(11L))
                .thenReturn(contract(11L, 8L, "foreign-contract", "ACTIVE"));
        assertThatThrownBy(() -> harness.service.listAuditEvents(7L, 11L, 10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("integration contract does not belong to tenant");
    }

    private static Harness harness() {
        MarketingIntegrationContractMapper contractMapper = mock(MarketingIntegrationContractMapper.class);
        MarketingIntegrationContractAuditEventMapper auditMapper =
                mock(MarketingIntegrationContractAuditEventMapper.class);
        return new Harness(
                contractMapper,
                auditMapper,
                new MarketingIntegrationContractService(contractMapper, auditMapper, new ObjectMapper()));
    }

    private static MarketingIntegrationContractCommand command() {
        return commandWith("ACTIVE", "outbound", "oauth", 45000);
    }

    private static MarketingIntegrationContractCommand commandWith(
            String status,
            String direction,
            String authMode,
            Integer timeoutMs) {
        return new MarketingIntegrationContractCommand(
                " Google Ads Keyword Write ",
                "Google Ads keyword write",
                "sem",
                "Search Marketing Governance",
                "Provider Credential Governance",
                "Search Provider Write Gateway",
                direction,
                "production",
                authMode,
                "active provider credential",
                "/canvas/search-marketing/mutations",
                "Growth",
                status,
                "standard",
                timeoutMs,
                Map.of("maxAttempts", 3),
                Map.of("request", "googleAdsMutate"),
                Map.of("provider", "GOOGLE_ADS"));
    }

    private static MarketingIntegrationContractDO contract(Long id, Long tenantId, String key, String status) {
        MarketingIntegrationContractDO row = new MarketingIntegrationContractDO();
        row.setId(id);
        row.setTenantId(tenantId);
        row.setContractKey(key);
        row.setDisplayName(key);
        row.setProviderFamily("SEM");
        row.setSourceCapabilityKey("search-marketing-governance");
        row.setTargetCapabilityKey("provider-credential-governance");
        row.setAssetKey("search-provider-write-gateway");
        row.setDirection("OUTBOUND");
        row.setEnvironment("PRODUCTION");
        row.setAuthMode("OAUTH");
        row.setCredentialDependency("active provider credential");
        row.setApiRoot("/canvas/search-marketing/mutations");
        row.setStatus(status);
        row.setSlaTier("STANDARD");
        row.setTimeoutMs(30000);
        row.setRetryPolicyJson("{}");
        row.setSchemaContractJson("{}");
        row.setMetadataJson("{}");
        row.setCreatedBy("operator-1");
        row.setUpdatedBy("operator-1");
        return row;
    }

    private static MarketingIntegrationContractAuditEventDO auditEvent(Integer revision, String eventType) {
        MarketingIntegrationContractAuditEventDO row = new MarketingIntegrationContractAuditEventDO();
        row.setId(100L + revision);
        row.setTenantId(7L);
        row.setContractId(10L);
        row.setContractKey("google-ads-keyword-write");
        row.setRevision(revision);
        row.setEventType(eventType);
        row.setPreviousStatus(revision <= 1 ? null : "DRAFT");
        row.setNewStatus(eventType.equals("ARCHIVED") ? "ARCHIVED" : "ACTIVE");
        row.setSnapshotJson("{\"contractKey\":\"google-ads-keyword-write\"}");
        row.setChangedFieldsJson("{\"changedFields\":[\"status\"]}");
        row.setChangedBy("operator-1");
        return row;
    }

    private record Harness(
            MarketingIntegrationContractMapper contractMapper,
            MarketingIntegrationContractAuditEventMapper auditMapper,
            MarketingIntegrationContractService service) {
    }
}
