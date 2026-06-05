package org.chovy.canvas.domain.analytics;

import org.chovy.canvas.dal.dataobject.AudienceMaterializationRunDO;
import org.chovy.canvas.dal.mapper.AudienceBitmapRollbackMapper;
import org.chovy.canvas.dal.mapper.AudienceMaterializationRunMapper;
import org.chovy.canvas.domain.warehouse.CdpWarehouseAvailabilityService;
import org.chovy.canvas.domain.warehouse.CdpWarehouseConsumerAvailabilityService;
import org.chovy.canvas.engine.audience.VersionedAudienceBitmapStore;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AudienceMaterializationOperationsServiceTest {

    @Test
    void materializeDelegatesToExistingMaterializationService() {
        AudienceMaterializationService materializationService = mock(AudienceMaterializationService.class);
        AudienceMaterializationRunMapper runMapper = mock(AudienceMaterializationRunMapper.class);
        AudienceMaterializationService.MaterializationResult expected =
                new AudienceMaterializationService.MaterializationResult("SUCCESS", 42L);
        when(materializationService.materialize(9L, 12L, "qa")).thenReturn(expected);
        AudienceMaterializationOperationsService service =
                new AudienceMaterializationOperationsService(materializationService, runMapper);

        AudienceMaterializationService.MaterializationResult result =
                service.materialize(9L, 12L, "qa");

        assertThat(result).isSameAs(expected);
        verify(materializationService).materialize(9L, 12L, "qa");
    }

    @Test
    void materializeRejectsInvalidAudienceId() {
        AudienceMaterializationOperationsService service =
                new AudienceMaterializationOperationsService(
                        mock(AudienceMaterializationService.class),
                        mock(AudienceMaterializationRunMapper.class));

        assertThatThrownBy(() -> service.materialize(9L, 0L, "qa"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("audienceId must be positive");
    }

    @Test
    void gatedMaterializeRunsWhenAvailabilityPasses() {
        AudienceMaterializationService materializationService = mock(AudienceMaterializationService.class);
        AudienceMaterializationRunMapper runMapper = mock(AudienceMaterializationRunMapper.class);
        CdpWarehouseAvailabilityService availabilityService = mock(CdpWarehouseAvailabilityService.class);
        LocalDateTime from = LocalDateTime.parse("2026-06-05T03:00:00");
        LocalDateTime to = LocalDateTime.parse("2026-06-05T04:00:00");
        CdpWarehouseAvailabilityService.AvailabilityDecision availability = availability("PASS", from, to);
        AudienceMaterializationService.MaterializationResult materialization =
                new AudienceMaterializationService.MaterializationResult("SUCCESS", 42L);
        when(availabilityService.evaluate(9L, from, to, "HYBRID")).thenReturn(availability);
        when(materializationService.materialize(9L, 12L, "qa")).thenReturn(materialization);
        AudienceMaterializationOperationsService service =
                new AudienceMaterializationOperationsService(
                        materializationService, runMapper, null, null, availabilityService);

        AudienceMaterializationOperationsService.GatedMaterializationResult result =
                service.materializeWithAvailabilityGate(9L, 12L, from, to, "HYBRID", false, "qa");

        assertThat(result.status()).isEqualTo("TRIGGERED");
        assertThat(result.availability()).isSameAs(availability);
        assertThat(result.materialization()).isSameAs(materialization);
        verify(materializationService).materialize(9L, 12L, "qa");
    }

    @Test
    void gatedMaterializeBlocksFailingAvailability() {
        AudienceMaterializationService materializationService = mock(AudienceMaterializationService.class);
        CdpWarehouseAvailabilityService availabilityService = mock(CdpWarehouseAvailabilityService.class);
        LocalDateTime from = LocalDateTime.parse("2026-06-05T03:00:00");
        LocalDateTime to = LocalDateTime.parse("2026-06-05T04:00:00");
        CdpWarehouseAvailabilityService.AvailabilityDecision availability = availability("FAIL", from, to);
        when(availabilityService.evaluate(9L, from, to, "OFFLINE")).thenReturn(availability);
        AudienceMaterializationOperationsService service =
                new AudienceMaterializationOperationsService(
                        materializationService,
                        mock(AudienceMaterializationRunMapper.class),
                        null,
                        null,
                        availabilityService);

        AudienceMaterializationOperationsService.GatedMaterializationResult result =
                service.materializeWithAvailabilityGate(9L, 12L, from, to, "OFFLINE", true, "qa");

        assertThat(result.status()).isEqualTo("BLOCKED");
        assertThat(result.reason()).contains("FAIL");
        assertThat(result.materialization()).isNull();
        org.mockito.Mockito.verifyNoInteractions(materializationService);
    }

    @Test
    void gatedMaterializeBlocksWarnUnlessExplicitlyAllowed() {
        AudienceMaterializationService materializationService = mock(AudienceMaterializationService.class);
        CdpWarehouseAvailabilityService availabilityService = mock(CdpWarehouseAvailabilityService.class);
        LocalDateTime from = LocalDateTime.parse("2026-06-05T03:00:00");
        LocalDateTime to = LocalDateTime.parse("2026-06-05T04:00:00");
        CdpWarehouseAvailabilityService.AvailabilityDecision availability = availability("WARN", from, to);
        when(availabilityService.evaluate(9L, from, to, "REALTIME")).thenReturn(availability);
        when(materializationService.materialize(9L, 12L, "qa")).thenReturn(
                new AudienceMaterializationService.MaterializationResult("SUCCESS", 42L));
        AudienceMaterializationOperationsService service =
                new AudienceMaterializationOperationsService(
                        materializationService,
                        mock(AudienceMaterializationRunMapper.class),
                        null,
                        null,
                        availabilityService);

        AudienceMaterializationOperationsService.GatedMaterializationResult blocked =
                service.materializeWithAvailabilityGate(9L, 12L, from, to, "REALTIME", false, "qa");
        AudienceMaterializationOperationsService.GatedMaterializationResult allowed =
                service.materializeWithAvailabilityGate(9L, 12L, from, to, "REALTIME", true, "qa");

        assertThat(blocked.status()).isEqualTo("BLOCKED");
        assertThat(blocked.reason()).contains("allowWarn");
        assertThat(allowed.status()).isEqualTo("TRIGGERED");
        assertThat(allowed.reason()).contains("WARN accepted");
        verify(materializationService).materialize(9L, 12L, "qa");
    }

    @Test
    void contractGatedMaterializeBlocksDisallowedContract() {
        AudienceMaterializationService materializationService = mock(AudienceMaterializationService.class);
        CdpWarehouseConsumerAvailabilityService consumerAvailabilityService =
                mock(CdpWarehouseConsumerAvailabilityService.class);
        LocalDateTime from = LocalDateTime.parse("2026-06-05T03:00:00");
        LocalDateTime to = LocalDateTime.parse("2026-06-05T04:00:00");
        when(consumerAvailabilityService.evaluateContract(9L, "audience_12", from, to))
                .thenReturn(consumerAvailability("audience_12", "FAIL", false, from, to));
        AudienceMaterializationOperationsService service =
                new AudienceMaterializationOperationsService(
                        materializationService,
                        mock(AudienceMaterializationRunMapper.class),
                        null,
                        null,
                        null,
                        consumerAvailabilityService);

        AudienceMaterializationOperationsService.ContractGatedMaterializationResult result =
                service.materializeWithConsumerAvailabilityContract(
                        9L, 12L, "audience_12", from, to, "qa");

        assertThat(result.status()).isEqualTo("BLOCKED");
        assertThat(result.materialization()).isNull();
        assertThat(result.consumerAvailability().status()).isEqualTo("FAIL");
        org.mockito.Mockito.verifyNoInteractions(materializationService);
    }

    @Test
    void contractGatedMaterializeRunsWhenContractAllows() {
        AudienceMaterializationService materializationService = mock(AudienceMaterializationService.class);
        CdpWarehouseConsumerAvailabilityService consumerAvailabilityService =
                mock(CdpWarehouseConsumerAvailabilityService.class);
        LocalDateTime from = LocalDateTime.parse("2026-06-05T03:00:00");
        LocalDateTime to = LocalDateTime.parse("2026-06-05T04:00:00");
        AudienceMaterializationService.MaterializationResult materialization =
                new AudienceMaterializationService.MaterializationResult("SUCCESS", 42L);
        when(consumerAvailabilityService.evaluateContract(9L, "audience_12", from, to))
                .thenReturn(consumerAvailability("audience_12", "WARN", true, from, to));
        when(materializationService.materialize(9L, 12L, "qa")).thenReturn(materialization);
        AudienceMaterializationOperationsService service =
                new AudienceMaterializationOperationsService(
                        materializationService,
                        mock(AudienceMaterializationRunMapper.class),
                        null,
                        null,
                        null,
                        consumerAvailabilityService);

        AudienceMaterializationOperationsService.ContractGatedMaterializationResult result =
                service.materializeWithConsumerAvailabilityContract(
                        9L, 12L, "audience_12", from, to, "qa");

        assertThat(result.status()).isEqualTo("TRIGGERED");
        assertThat(result.materialization()).isSameAs(materialization);
        assertThat(result.consumerAvailability().allowed()).isTrue();
        verify(materializationService).materialize(9L, 12L, "qa");
    }

    @Test
    void rollbackMarksBitmapVersionsAndRecordsAudit() {
        VersionedAudienceBitmapStore bitmapStore = mock(VersionedAudienceBitmapStore.class);
        AudienceBitmapRollbackMapper rollbackMapper = mock(AudienceBitmapRollbackMapper.class);
        when(bitmapStore.rollbackToVersion(9L, 12L, 2L)).thenReturn(
                new VersionedAudienceBitmapStore.RollbackResult(
                        9L, 12L, 2L, "audience:bitmap:12:v:2", 3, "SUCCESS"));
        AudienceMaterializationOperationsService service =
                new AudienceMaterializationOperationsService(
                        mock(AudienceMaterializationService.class),
                        mock(AudienceMaterializationRunMapper.class),
                        bitmapStore,
                        rollbackMapper);

        AudienceMaterializationOperationsService.RollbackView view =
                service.rollback(9L, 12L, 2L, "qa", "bad audience version");

        assertThat(view.targetVersion()).isEqualTo(2L);
        assertThat(view.rolledBackVersions()).isEqualTo(3);
        assertThat(view.status()).isEqualTo("SUCCESS");
        ArgumentCaptor<org.chovy.canvas.dal.dataobject.AudienceBitmapRollbackDO> audit =
                ArgumentCaptor.forClass(org.chovy.canvas.dal.dataobject.AudienceBitmapRollbackDO.class);
        verify(rollbackMapper).insert(audit.capture());
        assertThat(audit.getValue().getTenantId()).isEqualTo(9L);
        assertThat(audit.getValue().getAudienceId()).isEqualTo(12L);
        assertThat(audit.getValue().getTargetVersion()).isEqualTo(2L);
        assertThat(audit.getValue().getRolledBackVersions()).isEqualTo(3L);
        assertThat(audit.getValue().getReason()).isEqualTo("bad audience version");
        assertThat(audit.getValue().getOperator()).isEqualTo("qa");
    }

    @Test
    void rollbackRequiresAuditReason() {
        AudienceMaterializationOperationsService service =
                new AudienceMaterializationOperationsService(
                        mock(AudienceMaterializationService.class),
                        mock(AudienceMaterializationRunMapper.class),
                        mock(VersionedAudienceBitmapStore.class),
                        mock(AudienceBitmapRollbackMapper.class));

        assertThatThrownBy(() -> service.rollback(9L, 12L, 2L, "qa", " "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("reason is required");
    }

    @Test
    void recentRunsMapsTenantRunsToViews() {
        AudienceMaterializationRunMapper runMapper = mock(AudienceMaterializationRunMapper.class);
        when(runMapper.selectList(any())).thenReturn(List.of(
                run(101L, 9L, 12L, 3L, "SUCCESS", 42L, "audience:bitmap:12:v:3", null),
                run(100L, 9L, 12L, 2L, "FAILED", null, null, "Doris disabled")
        ));
        AudienceMaterializationOperationsService service =
                new AudienceMaterializationOperationsService(mock(AudienceMaterializationService.class), runMapper);

        List<AudienceMaterializationOperationsService.RunView> runs =
                service.recentRuns(9L, 12L, "success", 200);

        assertThat(runs).hasSize(2);
        assertThat(runs.get(0).id()).isEqualTo(101L);
        assertThat(runs.get(0).tenantId()).isEqualTo(9L);
        assertThat(runs.get(0).matchedUsers()).isEqualTo(42L);
        assertThat(runs.get(0).bitmapKey()).isEqualTo("audience:bitmap:12:v:3");
        assertThat(runs.get(1).matchedUsers()).isZero();
        assertThat(runs.get(1).errorMessage()).isEqualTo("Doris disabled");
    }

    private AudienceMaterializationRunDO run(Long id,
                                             Long tenantId,
                                             Long audienceId,
                                             Long version,
                                             String status,
                                             Long matchedUsers,
                                             String bitmapKey,
                                             String errorMessage) {
        AudienceMaterializationRunDO row = new AudienceMaterializationRunDO();
        row.setId(id);
        row.setTenantId(tenantId);
        row.setAudienceId(audienceId);
        row.setVersion(version);
        row.setStatus(status);
        row.setMatchedUsers(matchedUsers);
        row.setBitmapKey(bitmapKey);
        row.setErrorMessage(errorMessage);
        row.setStartedAt(LocalDateTime.parse("2026-06-05T03:00:00"));
        row.setFinishedAt(LocalDateTime.parse("2026-06-05T03:01:00"));
        row.setCreatedBy("qa");
        return row;
    }

    private CdpWarehouseAvailabilityService.AvailabilityDecision availability(
            String status,
            LocalDateTime from,
            LocalDateTime to) {
        return new CdpWarehouseAvailabilityService.AvailabilityDecision(
                9L,
                "HYBRID",
                from,
                to,
                to.plusMinutes(1),
                status,
                List.of(new CdpWarehouseAvailabilityService.AvailabilityGate(
                        "offline_aggregate",
                        status,
                        "test",
                        to,
                        0L,
                        1)));
    }

    private CdpWarehouseConsumerAvailabilityService.ConsumerAvailabilityEvaluation consumerAvailability(
            String contractKey,
            String status,
            boolean allowed,
            LocalDateTime from,
            LocalDateTime to) {
        return new CdpWarehouseConsumerAvailabilityService.ConsumerAvailabilityEvaluation(
                9L,
                contractKey,
                "AUDIENCE",
                "12",
                "HYBRID",
                from,
                to,
                to.plusMinutes(1),
                status,
                allowed,
                allowed ? "BLOCK_ON_FAIL" : "BLOCK_ON_WARN",
                availability(status, from, to),
                List.of(),
                "consumer availability " + status + (allowed ? " allowed" : " blocked"));
    }
}
