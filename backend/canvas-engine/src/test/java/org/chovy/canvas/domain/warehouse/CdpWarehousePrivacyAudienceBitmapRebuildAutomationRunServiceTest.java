package org.chovy.canvas.domain.warehouse;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.dal.dataobject.CdpWarehousePrivacyAudienceBitmapRebuildAutomationRunDO;
import org.chovy.canvas.dal.mapper.CdpWarehousePrivacyAudienceBitmapRebuildAutomationRunMapper;
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

class CdpWarehousePrivacyAudienceBitmapRebuildAutomationRunServiceTest {

    private static final Clock CLOCK =
            Clock.fixed(Instant.parse("2026-06-05T08:00:00Z"), ZoneId.of("Asia/Shanghai"));
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 6, 5, 16, 0);

    @Test
    void runAndRecordPersistsSuccessfulRun() {
        CdpWarehousePrivacyAudienceBitmapRebuildAutomationService automationService =
                mock(CdpWarehousePrivacyAudienceBitmapRebuildAutomationService.class);
        CdpWarehousePrivacyAudienceBitmapRebuildAutomationRunMapper mapper =
                mock(CdpWarehousePrivacyAudienceBitmapRebuildAutomationRunMapper.class);
        CdpWarehousePrivacyAudienceBitmapRebuildAutomationService.AutomationCommand command =
                new CdpWarehousePrivacyAudienceBitmapRebuildAutomationService.AutomationCommand(
                        "privacy-ops", 25, 100, true);
        CdpWarehousePrivacyAudienceBitmapRebuildAutomationService.AutomationResult result =
                new CdpWarehousePrivacyAudienceBitmapRebuildAutomationService.AutomationResult(
                        9L, "PASS", 2, 1, 1, 1, 0, List.of());
        when(automationService.run(9L, command)).thenReturn(result);
        when(mapper.insert(any(CdpWarehousePrivacyAudienceBitmapRebuildAutomationRunDO.class)))
                .thenAnswer(invocation -> {
                    CdpWarehousePrivacyAudienceBitmapRebuildAutomationRunDO row = invocation.getArgument(0);
                    row.setId(101L);
                    row.setCreatedAt(NOW);
                    row.setUpdatedAt(NOW);
                    return 1;
                });
        CdpWarehousePrivacyAudienceBitmapRebuildAutomationRunService service =
                new CdpWarehousePrivacyAudienceBitmapRebuildAutomationRunService(
                        automationService, mapper, new ObjectMapper(), CLOCK);

        CdpWarehousePrivacyAudienceBitmapRebuildAutomationRunService.AutomationRunView view =
                service.runAndRecord(9L, command, "MANUAL");

        assertThat(view.id()).isEqualTo(101L);
        assertThat(view.status()).isEqualTo("PASS");
        assertThat(view.result()).isSameAs(result);
        ArgumentCaptor<CdpWarehousePrivacyAudienceBitmapRebuildAutomationRunDO> insertCaptor =
                ArgumentCaptor.forClass(CdpWarehousePrivacyAudienceBitmapRebuildAutomationRunDO.class);
        ArgumentCaptor<CdpWarehousePrivacyAudienceBitmapRebuildAutomationRunDO> updateCaptor =
                ArgumentCaptor.forClass(CdpWarehousePrivacyAudienceBitmapRebuildAutomationRunDO.class);
        verify(mapper).insert(insertCaptor.capture());
        verify(mapper).updateById(updateCaptor.capture());
        CdpWarehousePrivacyAudienceBitmapRebuildAutomationRunDO inserted = insertCaptor.getValue();
        assertThat(inserted.getTenantId()).isEqualTo(9L);
        assertThat(inserted.getTriggerSource()).isEqualTo("MANUAL");
        assertThat(inserted.getStatus()).isEqualTo("PASS");
        assertThat(inserted.getActor()).isEqualTo("privacy-ops");
        assertThat(inserted.getScanLimit()).isEqualTo(25);
        assertThat(inserted.getAudienceLimit()).isEqualTo(100);
        assertThat(inserted.getRetryFailed()).isEqualTo(1);
        CdpWarehousePrivacyAudienceBitmapRebuildAutomationRunDO updated = updateCaptor.getValue();
        assertThat(updated.getScanned()).isEqualTo(2);
        assertThat(updated.getEligible()).isEqualTo(1);
        assertThat(updated.getTriggered()).isEqualTo(1);
        assertThat(updated.getSkipped()).isEqualTo(1);
        assertThat(updated.getFailed()).isZero();
        assertThat(updated.getResultJson()).contains("\"status\":\"PASS\"", "\"triggered\":1");
        assertThat(updated.getFinishedAt()).isEqualTo(NOW);
    }

    @Test
    void runAndRecordPersistsFailAndRethrowsWhenAutomationFails() {
        CdpWarehousePrivacyAudienceBitmapRebuildAutomationService automationService =
                mock(CdpWarehousePrivacyAudienceBitmapRebuildAutomationService.class);
        CdpWarehousePrivacyAudienceBitmapRebuildAutomationRunMapper mapper =
                mock(CdpWarehousePrivacyAudienceBitmapRebuildAutomationRunMapper.class);
        CdpWarehousePrivacyAudienceBitmapRebuildAutomationService.AutomationCommand command =
                new CdpWarehousePrivacyAudienceBitmapRebuildAutomationService.AutomationCommand(
                        "privacy-ops", 25, 100, false);
        when(automationService.run(9L, command)).thenThrow(new IllegalStateException("store unavailable"));
        when(mapper.insert(any(CdpWarehousePrivacyAudienceBitmapRebuildAutomationRunDO.class)))
                .thenAnswer(invocation -> {
                    CdpWarehousePrivacyAudienceBitmapRebuildAutomationRunDO row = invocation.getArgument(0);
                    row.setId(102L);
                    return 1;
                });
        CdpWarehousePrivacyAudienceBitmapRebuildAutomationRunService service =
                new CdpWarehousePrivacyAudienceBitmapRebuildAutomationRunService(
                        automationService, mapper, new ObjectMapper(), CLOCK);

        assertThatThrownBy(() -> service.runAndRecord(9L, command, "MANUAL"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("store unavailable");

        ArgumentCaptor<CdpWarehousePrivacyAudienceBitmapRebuildAutomationRunDO> updateCaptor =
                ArgumentCaptor.forClass(CdpWarehousePrivacyAudienceBitmapRebuildAutomationRunDO.class);
        verify(mapper).updateById(updateCaptor.capture());
        assertThat(updateCaptor.getValue().getStatus()).isEqualTo("FAIL");
        assertThat(updateCaptor.getValue().getErrorMessage()).contains("store unavailable");
        assertThat(updateCaptor.getValue().getFinishedAt()).isEqualTo(NOW);
    }

    @Test
    void recentReturnsTenantScopedRows() {
        CdpWarehousePrivacyAudienceBitmapRebuildAutomationRunMapper mapper =
                mock(CdpWarehousePrivacyAudienceBitmapRebuildAutomationRunMapper.class);
        when(mapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(row(101L, 9L, "PASS")));
        CdpWarehousePrivacyAudienceBitmapRebuildAutomationRunService service =
                new CdpWarehousePrivacyAudienceBitmapRebuildAutomationRunService(
                        mock(CdpWarehousePrivacyAudienceBitmapRebuildAutomationService.class),
                        mapper,
                        new ObjectMapper(),
                        CLOCK);

        List<CdpWarehousePrivacyAudienceBitmapRebuildAutomationRunService.AutomationRunView> runs =
                service.recent(9L, 20);

        assertThat(runs).singleElement().satisfies(run -> {
            assertThat(run.id()).isEqualTo(101L);
            assertThat(run.tenantId()).isEqualTo(9L);
            assertThat(run.status()).isEqualTo("PASS");
        });
    }

    @Test
    void getRejectsRowsOutsideTenantScope() {
        CdpWarehousePrivacyAudienceBitmapRebuildAutomationRunMapper mapper =
                mock(CdpWarehousePrivacyAudienceBitmapRebuildAutomationRunMapper.class);
        when(mapper.selectById(101L)).thenReturn(row(101L, 8L, "PASS"));
        CdpWarehousePrivacyAudienceBitmapRebuildAutomationRunService service =
                new CdpWarehousePrivacyAudienceBitmapRebuildAutomationRunService(
                        mock(CdpWarehousePrivacyAudienceBitmapRebuildAutomationService.class),
                        mapper,
                        new ObjectMapper(),
                        CLOCK);

        assertThatThrownBy(() -> service.get(9L, 101L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not found");
    }

    private CdpWarehousePrivacyAudienceBitmapRebuildAutomationRunDO row(Long id, Long tenantId, String status) {
        CdpWarehousePrivacyAudienceBitmapRebuildAutomationRunDO row =
                new CdpWarehousePrivacyAudienceBitmapRebuildAutomationRunDO();
        row.setId(id);
        row.setTenantId(tenantId);
        row.setTriggerSource("MANUAL");
        row.setStatus(status);
        row.setActor("privacy-ops");
        row.setScanLimit(25);
        row.setAudienceLimit(100);
        row.setRetryFailed(0);
        row.setScanned(2);
        row.setEligible(1);
        row.setTriggered(1);
        row.setSkipped(1);
        row.setFailed(0);
        row.setResultJson("{\"status\":\"" + status + "\"}");
        row.setStartedAt(NOW);
        row.setFinishedAt(NOW.plusMinutes(1));
        row.setCreatedAt(NOW);
        row.setUpdatedAt(NOW);
        return row;
    }
}
