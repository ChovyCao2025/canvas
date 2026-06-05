package org.chovy.canvas.domain.warehouse;

import org.chovy.canvas.dal.dataobject.CdpWarehouseStreamJobActionDO;
import org.chovy.canvas.dal.dataobject.CdpWarehouseStreamJobInstanceDO;
import org.chovy.canvas.dal.mapper.CdpWarehouseStreamJobActionMapper;
import org.chovy.canvas.dal.mapper.CdpWarehouseStreamJobInstanceMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CdpWarehouseRealtimeJobControlServiceTest {

    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-06-05T00:00:00Z"), ZoneId.of("UTC"));

    @Test
    void heartbeatUpsertsJobInstanceAndPreservesExistingDesiredStatusWhenMissing() {
        CdpWarehouseStreamJobInstanceMapper instanceMapper = mock(CdpWarehouseStreamJobInstanceMapper.class);
        CdpWarehouseStreamJobActionMapper actionMapper = mock(CdpWarehouseStreamJobActionMapper.class);
        CdpWarehouseStreamJobInstanceDO existing = job("RUNNING", "PAUSED", now().minusSeconds(10));
        when(instanceMapper.findByKey(9L, "pipe", "job-a")).thenReturn(existing);
        CdpWarehouseRealtimeJobControlService service = service(instanceMapper, actionMapper);

        CdpWarehouseRealtimeJobControlService.JobInstanceView view = service.heartbeat(9L,
                new CdpWarehouseRealtimeJobControlService.HeartbeatCommand(
                        "pipe", "job-a", "flink", "flink-1", "deployment/flink-1",
                        "running", null, now(), "{\"parallelism\":4}", null, "data-platform"));

        assertThat(view.runtimeStatus()).isEqualTo("RUNNING");
        assertThat(view.desiredStatus()).isEqualTo("PAUSED");
        assertThat(view.healthStatus()).isEqualTo("WARN");
        assertThat(view.reasons()).contains("desired PAUSED but runtime is RUNNING");
        ArgumentCaptor<CdpWarehouseStreamJobInstanceDO> row =
                ArgumentCaptor.forClass(CdpWarehouseStreamJobInstanceDO.class);
        verify(instanceMapper).upsertHeartbeat(row.capture());
        assertThat(row.getValue().getEngineType()).isEqualTo("FLINK");
        assertThat(row.getValue().getDesiredStatus()).isEqualTo("PAUSED");
        assertThat(row.getValue().getHeartbeatPayloadJson()).isEqualTo("{\"parallelism\":4}");
    }

    @Test
    void statusEvaluatesPassWarnAndFailJobs() {
        CdpWarehouseStreamJobInstanceMapper instanceMapper = mock(CdpWarehouseStreamJobInstanceMapper.class);
        when(instanceMapper.listInstances(9L, "pipe", 50)).thenReturn(List.of(
                job("RUNNING", "RUNNING", now().minusSeconds(10)),
                job("RUNNING", "RUNNING", now().minusSeconds(600)),
                job("FAILED", "RUNNING", now().minusSeconds(10))
        ));
        CdpWarehouseRealtimeJobControlService service =
                service(instanceMapper, mock(CdpWarehouseStreamJobActionMapper.class));

        CdpWarehouseRealtimeJobControlService.JobStatusSummary status =
                service.status(9L, "pipe", 300, 50);

        assertThat(status.total()).isEqualTo(3);
        assertThat(status.passed()).isEqualTo(1);
        assertThat(status.warned()).isEqualTo(1);
        assertThat(status.failed()).isEqualTo(1);
        assertThat(status.jobs().get(1).reasons().get(0)).contains("heartbeat age");
    }

    @Test
    void requestActionUpdatesDesiredStatusAndInsertsPendingAudit() {
        CdpWarehouseStreamJobInstanceMapper instanceMapper = mock(CdpWarehouseStreamJobInstanceMapper.class);
        CdpWarehouseStreamJobActionMapper actionMapper = mock(CdpWarehouseStreamJobActionMapper.class);
        when(instanceMapper.findByKey(9L, "pipe", "job-a"))
                .thenReturn(job("RUNNING", "RUNNING", now().minusSeconds(10)));
        CdpWarehouseRealtimeJobControlService service = service(instanceMapper, actionMapper);

        CdpWarehouseRealtimeJobControlService.JobActionView action = service.requestAction(9L,
                new CdpWarehouseRealtimeJobControlService.ActionRequestCommand(
                        "pipe", "job-a", "pause", "maintenance window"), "alice");

        assertThat(action.action()).isEqualTo("PAUSE");
        assertThat(action.status()).isEqualTo("PENDING");
        verify(instanceMapper).updateDesiredStatus(9L, "pipe", "job-a", "PAUSED");
        ArgumentCaptor<CdpWarehouseStreamJobActionDO> row =
                ArgumentCaptor.forClass(CdpWarehouseStreamJobActionDO.class);
        verify(actionMapper).insert(row.capture());
        assertThat(row.getValue().getRequestedBy()).isEqualTo("alice");
        assertThat(row.getValue().getReason()).isEqualTo("maintenance window");
    }

    @Test
    void pendingActionsAreBoundedAndMapped() {
        CdpWarehouseStreamJobActionMapper actionMapper = mock(CdpWarehouseStreamJobActionMapper.class);
        when(actionMapper.selectPending(9L, "pipe", "job-a", 100))
                .thenReturn(List.of(actionRow(7L, "RESTART", "PENDING")));
        CdpWarehouseRealtimeJobControlService service =
                service(mock(CdpWarehouseStreamJobInstanceMapper.class), actionMapper);

        List<CdpWarehouseRealtimeJobControlService.JobActionView> actions =
                service.pendingActions(9L, "pipe", "job-a", 500);

        assertThat(actions).hasSize(1);
        assertThat(actions.get(0).id()).isEqualTo(7L);
        verify(actionMapper).selectPending(9L, "pipe", "job-a", 100);
    }

    @Test
    void acknowledgeAndCompleteActionUpdateAuditState() {
        CdpWarehouseStreamJobActionMapper actionMapper = mock(CdpWarehouseStreamJobActionMapper.class);
        when(actionMapper.findByTenantAndId(9L, 7L)).thenReturn(actionRow(7L, "RESTART", "PENDING"));
        CdpWarehouseRealtimeJobControlService service =
                service(mock(CdpWarehouseStreamJobInstanceMapper.class), actionMapper);

        CdpWarehouseRealtimeJobControlService.JobActionView acknowledged = service.acknowledge(9L, 7L);
        when(actionMapper.findByTenantAndId(9L, 7L)).thenReturn(actionRow(7L, "RESTART", "ACKNOWLEDGED"));
        CdpWarehouseRealtimeJobControlService.JobActionView completed =
                service.complete(9L, 7L, "COMPLETED", "job restarted");

        assertThat(acknowledged.status()).isEqualTo("ACKNOWLEDGED");
        assertThat(completed.status()).isEqualTo("COMPLETED");
        assertThat(completed.resultMessage()).isEqualTo("job restarted");
        verify(actionMapper).updateStatus(9L, 7L, "ACKNOWLEDGED", now(), null, null);
        verify(actionMapper).updateStatus(9L, 7L, "COMPLETED", now(), now(), "job restarted");
    }

    @Test
    void requestActionRejectsMissingJobInstance() {
        CdpWarehouseRealtimeJobControlService service =
                service(mock(CdpWarehouseStreamJobInstanceMapper.class), mock(CdpWarehouseStreamJobActionMapper.class));

        assertThatThrownBy(() -> service.requestAction(9L,
                new CdpWarehouseRealtimeJobControlService.ActionRequestCommand(
                        "pipe", "job-a", "pause", "reason"), "alice"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("stream job instance not found");
    }

    private CdpWarehouseRealtimeJobControlService service(CdpWarehouseStreamJobInstanceMapper instanceMapper,
                                                          CdpWarehouseStreamJobActionMapper actionMapper) {
        return new CdpWarehouseRealtimeJobControlService(instanceMapper, actionMapper, CLOCK);
    }

    private CdpWarehouseStreamJobInstanceDO job(String runtimeStatus,
                                                String desiredStatus,
                                                LocalDateTime lastHeartbeatAt) {
        CdpWarehouseStreamJobInstanceDO row = new CdpWarehouseStreamJobInstanceDO();
        row.setId(1L);
        row.setTenantId(9L);
        row.setPipelineKey("pipe");
        row.setJobKey("job-a");
        row.setEngineType("FLINK");
        row.setEngineJobId("flink-1");
        row.setDeploymentRef("deployment/flink-1");
        row.setRuntimeStatus(runtimeStatus);
        row.setDesiredStatus(desiredStatus);
        row.setLastHeartbeatAt(lastHeartbeatAt);
        row.setLastErrorMessage("job failed");
        row.setOwnerName("data-platform");
        return row;
    }

    private CdpWarehouseStreamJobActionDO actionRow(Long id, String action, String status) {
        CdpWarehouseStreamJobActionDO row = new CdpWarehouseStreamJobActionDO();
        row.setId(id);
        row.setTenantId(9L);
        row.setPipelineKey("pipe");
        row.setJobKey("job-a");
        row.setAction(action);
        row.setStatus(status);
        row.setRequestedBy("alice");
        row.setReason("maintenance");
        row.setRequestedAt(now());
        if ("ACKNOWLEDGED".equals(status)) {
            row.setAcknowledgedAt(now());
        }
        return row;
    }

    private LocalDateTime now() {
        return LocalDateTime.now(CLOCK);
    }
}
