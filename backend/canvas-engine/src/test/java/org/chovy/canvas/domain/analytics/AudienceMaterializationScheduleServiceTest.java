package org.chovy.canvas.domain.analytics;

import org.chovy.canvas.dal.dataobject.AudienceDefinitionDO;
import org.chovy.canvas.dal.dataobject.AudienceMaterializationRunDO;
import org.chovy.canvas.dal.mapper.AudienceDefinitionMapper;
import org.chovy.canvas.dal.mapper.AudienceMaterializationRunMapper;
import org.chovy.canvas.domain.warehouse.CdpWarehouseAvailabilityService;
import org.chovy.canvas.domain.warehouse.CdpWarehouseConsumerAvailabilityService;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AudienceMaterializationScheduleServiceTest {

    @Test
    void refreshDueRunsDueAudiencesAndSkipsInvalidScheduleWithoutBlockingFailures() {
        AudienceDefinitionMapper definitionMapper = mock(AudienceDefinitionMapper.class);
        AudienceMaterializationRunMapper runMapper = mock(AudienceMaterializationRunMapper.class);
        AudienceMaterializationService materializationService = mock(AudienceMaterializationService.class);
        LocalDateTime now = LocalDateTime.parse("2026-06-05T05:00:00");
        AudienceDefinitionDO hourly = definition(12L, "0 0 * * * *");
        AudienceDefinitionDO noCron = definition(13L, null);
        AudienceDefinitionDO neverRun = definition(14L, "0 0 * * * *");
        when(definitionMapper.selectMaterializationCandidates(9L, 100))
                .thenReturn(List.of(hourly, noCron, neverRun));
        when(runMapper.latestSuccessfulRun(9L, 12L)).thenReturn(run("2026-06-05T03:00:00"));
        when(runMapper.latestSuccessfulRun(9L, 13L)).thenReturn(run("2026-06-05T03:00:00"));
        when(runMapper.latestSuccessfulRun(9L, 14L)).thenReturn(null);
        when(materializationService.materialize(9L, 12L, "scheduler"))
                .thenReturn(new AudienceMaterializationService.MaterializationResult("SUCCESS", 10));
        when(materializationService.materialize(9L, 14L, "scheduler"))
                .thenReturn(new AudienceMaterializationService.MaterializationResult("FAILED", 0));
        AudienceMaterializationScheduleService service =
                new AudienceMaterializationScheduleService(definitionMapper, runMapper, materializationService);

        AudienceMaterializationScheduleService.ScheduledRefreshResult result =
                service.refreshDue(9L, now, 500, "");

        assertThat(result.scanned()).isEqualTo(3);
        assertThat(result.due()).isEqualTo(2);
        assertThat(result.succeeded()).isEqualTo(1);
        assertThat(result.failed()).isEqualTo(1);
        assertThat(result.skipped()).isEqualTo(1);
        verify(definitionMapper).selectMaterializationCandidates(9L, 100);
        verify(materializationService).materialize(9L, 12L, "scheduler");
        verify(materializationService).materialize(9L, 14L, "scheduler");
    }

    @Test
    void fiveFieldCronExpressionIsSupportedForDueChecks() {
        AudienceMaterializationScheduleService service =
                new AudienceMaterializationScheduleService(
                        mock(AudienceDefinitionMapper.class),
                        mock(AudienceMaterializationRunMapper.class),
                        mock(AudienceMaterializationService.class));

        boolean due = service.isDue(
                definition(12L, "0 * * * *"),
                run("2026-06-05T03:00:00"),
                LocalDateTime.parse("2026-06-05T04:00:00"));

        assertThat(due).isTrue();
    }

    @Test
    void refreshDueWithAvailabilityGateRunsDueRefreshWhenPasses() {
        AudienceDefinitionMapper definitionMapper = mock(AudienceDefinitionMapper.class);
        AudienceMaterializationRunMapper runMapper = mock(AudienceMaterializationRunMapper.class);
        AudienceMaterializationService materializationService = mock(AudienceMaterializationService.class);
        CdpWarehouseAvailabilityService availabilityService = mock(CdpWarehouseAvailabilityService.class);
        LocalDateTime now = LocalDateTime.parse("2026-06-05T05:00:00");
        LocalDateTime from = LocalDateTime.parse("2026-06-05T04:00:00");
        AudienceDefinitionDO due = definition(12L, "0 0 * * * *");
        when(availabilityService.evaluate(9L, from, now, "HYBRID"))
                .thenReturn(availability("PASS", from, now));
        when(definitionMapper.selectMaterializationCandidates(9L, 100)).thenReturn(List.of(due));
        when(runMapper.latestSuccessfulRun(9L, 12L)).thenReturn(null);
        when(materializationService.materialize(9L, 12L, "scheduler"))
                .thenReturn(new AudienceMaterializationService.MaterializationResult("SUCCESS", 10));
        AudienceMaterializationScheduleService service =
                new AudienceMaterializationScheduleService(
                        definitionMapper, runMapper, materializationService, availabilityService);

        AudienceMaterializationScheduleService.GatedScheduledRefreshResult result =
                service.refreshDueWithAvailabilityGate(9L, now, 100, "scheduler", from, now, "HYBRID", false);

        assertThat(result.status()).isEqualTo("EXECUTED");
        assertThat(result.reason()).isEqualTo("warehouse availability PASS");
        assertThat(result.refreshResult()).isNotNull();
        assertThat(result.refreshResult().succeeded()).isEqualTo(1);
        verify(materializationService).materialize(9L, 12L, "scheduler");
    }

    @Test
    void refreshDueWithAvailabilityGateBlocksFailBeforeScanningCandidates() {
        AudienceDefinitionMapper definitionMapper = mock(AudienceDefinitionMapper.class);
        AudienceMaterializationRunMapper runMapper = mock(AudienceMaterializationRunMapper.class);
        AudienceMaterializationService materializationService = mock(AudienceMaterializationService.class);
        CdpWarehouseAvailabilityService availabilityService = mock(CdpWarehouseAvailabilityService.class);
        LocalDateTime now = LocalDateTime.parse("2026-06-05T05:00:00");
        LocalDateTime from = LocalDateTime.parse("2026-06-05T04:00:00");
        when(availabilityService.evaluate(9L, from, now, "OFFLINE"))
                .thenReturn(availability("FAIL", from, now));
        AudienceMaterializationScheduleService service =
                new AudienceMaterializationScheduleService(
                        definitionMapper, runMapper, materializationService, availabilityService);

        AudienceMaterializationScheduleService.GatedScheduledRefreshResult result =
                service.refreshDueWithAvailabilityGate(9L, now, 100, "scheduler", from, now, "OFFLINE", true);

        assertThat(result.status()).isEqualTo("BLOCKED");
        assertThat(result.reason()).isEqualTo("warehouse availability FAIL");
        assertThat(result.refreshResult()).isNull();
        verifyNoInteractions(definitionMapper, runMapper, materializationService);
    }

    @Test
    void refreshDueWithAvailabilityGateBlocksWarnUnlessAllowed() {
        AudienceDefinitionMapper definitionMapper = mock(AudienceDefinitionMapper.class);
        AudienceMaterializationRunMapper runMapper = mock(AudienceMaterializationRunMapper.class);
        AudienceMaterializationService materializationService = mock(AudienceMaterializationService.class);
        CdpWarehouseAvailabilityService availabilityService = mock(CdpWarehouseAvailabilityService.class);
        LocalDateTime now = LocalDateTime.parse("2026-06-05T05:00:00");
        LocalDateTime from = LocalDateTime.parse("2026-06-05T04:00:00");
        AudienceDefinitionDO due = definition(12L, "0 0 * * * *");
        when(availabilityService.evaluate(9L, from, now, "REALTIME"))
                .thenReturn(availability("WARN", from, now));
        when(definitionMapper.selectMaterializationCandidates(9L, 100)).thenReturn(List.of(due));
        when(runMapper.latestSuccessfulRun(9L, 12L)).thenReturn(null);
        when(materializationService.materialize(9L, 12L, "scheduler"))
                .thenReturn(new AudienceMaterializationService.MaterializationResult("SUCCESS", 10));
        AudienceMaterializationScheduleService service =
                new AudienceMaterializationScheduleService(
                        definitionMapper, runMapper, materializationService, availabilityService);

        AudienceMaterializationScheduleService.GatedScheduledRefreshResult blocked =
                service.refreshDueWithAvailabilityGate(9L, now, 100, "scheduler", from, now, "REALTIME", false);
        AudienceMaterializationScheduleService.GatedScheduledRefreshResult executed =
                service.refreshDueWithAvailabilityGate(9L, now, 100, "scheduler", from, now, "REALTIME", true);

        assertThat(blocked.status()).isEqualTo("BLOCKED");
        assertThat(blocked.reason()).contains("allowWarn=true");
        assertThat(executed.status()).isEqualTo("EXECUTED");
        assertThat(executed.reason()).isEqualTo("warehouse availability WARN accepted by operator");
        assertThat(executed.refreshResult().succeeded()).isEqualTo(1);
        verify(materializationService).materialize(9L, 12L, "scheduler");
    }

    @Test
    void refreshDueWithConsumerAvailabilityContractsMaterializesAllowedDueAudience() {
        AudienceDefinitionMapper definitionMapper = mock(AudienceDefinitionMapper.class);
        AudienceMaterializationRunMapper runMapper = mock(AudienceMaterializationRunMapper.class);
        AudienceMaterializationService materializationService = mock(AudienceMaterializationService.class);
        CdpWarehouseConsumerAvailabilityService consumerAvailabilityService =
                mock(CdpWarehouseConsumerAvailabilityService.class);
        LocalDateTime now = LocalDateTime.parse("2026-06-05T05:00:00");
        LocalDateTime from = LocalDateTime.parse("2026-06-05T04:00:00");
        AudienceDefinitionDO due = definition(12L, "0 0 * * * *");
        when(definitionMapper.selectMaterializationCandidates(9L, 100)).thenReturn(List.of(due));
        when(runMapper.latestSuccessfulRun(9L, 12L)).thenReturn(null);
        when(consumerAvailabilityService.evaluateContract(9L, "audience_12", from, now))
                .thenReturn(consumerEvaluation("audience_12", "PASS", true, from, now));
        when(materializationService.materialize(9L, 12L, "scheduler"))
                .thenReturn(new AudienceMaterializationService.MaterializationResult("SUCCESS", 10));
        AudienceMaterializationScheduleService service =
                new AudienceMaterializationScheduleService(
                        definitionMapper,
                        runMapper,
                        materializationService,
                        null,
                        consumerAvailabilityService);

        AudienceMaterializationScheduleService.ContractGatedScheduledRefreshResult result =
                service.refreshDueWithConsumerAvailabilityContracts(
                        9L, now, 100, "scheduler", from, now, "audience_");

        assertThat(result.scanned()).isEqualTo(1);
        assertThat(result.due()).isEqualTo(1);
        assertThat(result.succeeded()).isEqualTo(1);
        assertThat(result.failed()).isZero();
        assertThat(result.blocked()).isZero();
        assertThat(result.skipped()).isZero();
        verify(consumerAvailabilityService).evaluateContract(9L, "audience_12", from, now);
        verify(materializationService).materialize(9L, 12L, "scheduler");
    }

    @Test
    void refreshDueWithConsumerAvailabilityContractsBlocksDisallowedAudience() {
        AudienceDefinitionMapper definitionMapper = mock(AudienceDefinitionMapper.class);
        AudienceMaterializationRunMapper runMapper = mock(AudienceMaterializationRunMapper.class);
        AudienceMaterializationService materializationService = mock(AudienceMaterializationService.class);
        CdpWarehouseConsumerAvailabilityService consumerAvailabilityService =
                mock(CdpWarehouseConsumerAvailabilityService.class);
        LocalDateTime now = LocalDateTime.parse("2026-06-05T05:00:00");
        LocalDateTime from = LocalDateTime.parse("2026-06-05T04:00:00");
        AudienceDefinitionDO due = definition(12L, "0 0 * * * *");
        when(definitionMapper.selectMaterializationCandidates(9L, 100)).thenReturn(List.of(due));
        when(runMapper.latestSuccessfulRun(9L, 12L)).thenReturn(null);
        when(consumerAvailabilityService.evaluateContract(9L, "audience_12", from, now))
                .thenReturn(consumerEvaluation("audience_12", "FAIL", false, from, now));
        AudienceMaterializationScheduleService service =
                new AudienceMaterializationScheduleService(
                        definitionMapper,
                        runMapper,
                        materializationService,
                        null,
                        consumerAvailabilityService);

        AudienceMaterializationScheduleService.ContractGatedScheduledRefreshResult result =
                service.refreshDueWithConsumerAvailabilityContracts(
                        9L, now, 100, "scheduler", from, now, "audience_");

        assertThat(result.scanned()).isEqualTo(1);
        assertThat(result.due()).isEqualTo(1);
        assertThat(result.succeeded()).isZero();
        assertThat(result.failed()).isZero();
        assertThat(result.blocked()).isEqualTo(1);
        assertThat(result.skipped()).isZero();
        verify(consumerAvailabilityService).evaluateContract(9L, "audience_12", from, now);
        verifyNoInteractions(materializationService);
    }

    @Test
    void refreshDueWithConsumerAvailabilityContractsCountsEvaluationExceptionAsFailure() {
        AudienceDefinitionMapper definitionMapper = mock(AudienceDefinitionMapper.class);
        AudienceMaterializationRunMapper runMapper = mock(AudienceMaterializationRunMapper.class);
        AudienceMaterializationService materializationService = mock(AudienceMaterializationService.class);
        CdpWarehouseConsumerAvailabilityService consumerAvailabilityService =
                mock(CdpWarehouseConsumerAvailabilityService.class);
        LocalDateTime now = LocalDateTime.parse("2026-06-05T05:00:00");
        LocalDateTime from = LocalDateTime.parse("2026-06-05T04:00:00");
        AudienceDefinitionDO due = definition(12L, "0 0 * * * *");
        when(definitionMapper.selectMaterializationCandidates(9L, 100)).thenReturn(List.of(due));
        when(runMapper.latestSuccessfulRun(9L, 12L)).thenReturn(null);
        when(consumerAvailabilityService.evaluateContract(9L, "audience_12", from, now))
                .thenThrow(new IllegalStateException("contract store unavailable"));
        AudienceMaterializationScheduleService service =
                new AudienceMaterializationScheduleService(
                        definitionMapper,
                        runMapper,
                        materializationService,
                        null,
                        consumerAvailabilityService);

        AudienceMaterializationScheduleService.ContractGatedScheduledRefreshResult result =
                service.refreshDueWithConsumerAvailabilityContracts(
                        9L, now, 100, "scheduler", from, now, "audience_");

        assertThat(result.scanned()).isEqualTo(1);
        assertThat(result.due()).isEqualTo(1);
        assertThat(result.succeeded()).isZero();
        assertThat(result.failed()).isEqualTo(1);
        assertThat(result.blocked()).isZero();
        assertThat(result.skipped()).isZero();
        verifyNoInteractions(materializationService);
    }

    @Test
    void refreshDueWithConsumerAvailabilityContractsHonorsConfigOverride() {
        AudienceDefinitionMapper definitionMapper = mock(AudienceDefinitionMapper.class);
        AudienceMaterializationRunMapper runMapper = mock(AudienceMaterializationRunMapper.class);
        AudienceMaterializationService materializationService = mock(AudienceMaterializationService.class);
        CdpWarehouseConsumerAvailabilityService consumerAvailabilityService =
                mock(CdpWarehouseConsumerAvailabilityService.class);
        LocalDateTime now = LocalDateTime.parse("2026-06-05T05:00:00");
        LocalDateTime from = LocalDateTime.parse("2026-06-05T04:00:00");
        AudienceDefinitionDO due = definition(12L, "0 0 * * * *");
        due.setDataSourceConfig("{\"warehouseAvailabilityContractKey\":\"critical_audience_12\"}");
        when(definitionMapper.selectMaterializationCandidates(9L, 100)).thenReturn(List.of(due));
        when(runMapper.latestSuccessfulRun(9L, 12L)).thenReturn(null);
        when(consumerAvailabilityService.evaluateContract(9L, "critical_audience_12", from, now))
                .thenReturn(consumerEvaluation("critical_audience_12", "PASS", true, from, now));
        when(materializationService.materialize(9L, 12L, "scheduler"))
                .thenReturn(new AudienceMaterializationService.MaterializationResult("SUCCESS", 10));
        AudienceMaterializationScheduleService service =
                new AudienceMaterializationScheduleService(
                        definitionMapper,
                        runMapper,
                        materializationService,
                        null,
                        consumerAvailabilityService);

        AudienceMaterializationScheduleService.ContractGatedScheduledRefreshResult result =
                service.refreshDueWithConsumerAvailabilityContracts(
                        9L, now, 100, "scheduler", from, now, "audience_");

        assertThat(result.succeeded()).isEqualTo(1);
        verify(consumerAvailabilityService).evaluateContract(9L, "critical_audience_12", from, now);
    }

    @Test
    void refreshDueWithConsumerAvailabilityContractsRequiresConsumerAvailabilityService() {
        AudienceDefinitionMapper definitionMapper = mock(AudienceDefinitionMapper.class);
        AudienceMaterializationRunMapper runMapper = mock(AudienceMaterializationRunMapper.class);
        AudienceMaterializationService materializationService = mock(AudienceMaterializationService.class);
        CdpWarehouseAvailabilityService availabilityService = null;
        CdpWarehouseConsumerAvailabilityService consumerAvailabilityService = null;
        AudienceMaterializationScheduleService service =
                new AudienceMaterializationScheduleService(
                        definitionMapper,
                        runMapper,
                        materializationService,
                        availabilityService,
                        consumerAvailabilityService);

        assertThatThrownBy(() -> service.refreshDueWithConsumerAvailabilityContracts(
                9L,
                LocalDateTime.parse("2026-06-05T05:00:00"),
                100,
                "scheduler",
                LocalDateTime.parse("2026-06-05T04:00:00"),
                LocalDateTime.parse("2026-06-05T05:00:00"),
                "audience_"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("warehouse consumer availability service is not configured");
        verifyNoInteractions(definitionMapper, runMapper, materializationService);
    }

    private AudienceDefinitionDO definition(Long id, String cron) {
        AudienceDefinitionDO definition = new AudienceDefinitionDO();
        definition.setId(id);
        definition.setTenantId(9L);
        definition.setEnabled(1);
        definition.setEvaluationStrategy("OFFLINE_BATCH");
        definition.setCronExpression(cron);
        return definition;
    }

    private AudienceMaterializationRunDO run(String finishedAt) {
        AudienceMaterializationRunDO run = new AudienceMaterializationRunDO();
        run.setTenantId(9L);
        run.setAudienceId(12L);
        run.setStatus("SUCCESS");
        run.setStartedAt(LocalDateTime.parse(finishedAt).minusMinutes(1));
        run.setFinishedAt(LocalDateTime.parse(finishedAt));
        return run;
    }

    private CdpWarehouseAvailabilityService.AvailabilityDecision availability(String status,
                                                                             LocalDateTime from,
                                                                             LocalDateTime to) {
        return new CdpWarehouseAvailabilityService.AvailabilityDecision(
                9L,
                "HYBRID",
                from,
                to,
                to,
                status,
                List.of(new CdpWarehouseAvailabilityService.AvailabilityGate(
                        "offline_aggregate",
                        status,
                        "test availability " + status,
                        to,
                        0L,
                        1)));
    }

    private CdpWarehouseConsumerAvailabilityService.ConsumerAvailabilityEvaluation consumerEvaluation(
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
                to,
                status,
                allowed,
                "BLOCK_ON_WARN",
                null,
                List.of(),
                "consumer availability " + status);
    }
}
