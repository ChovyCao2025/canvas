package org.chovy.canvas.domain.warehouse;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.chovy.canvas.dal.dataobject.CdpWarehouseE2eCertificationRunDO;
import org.chovy.canvas.dal.mapper.CdpWarehouseE2eCertificationRunMapper;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CdpWarehouseE2eCertificationGateServiceTest {

    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-06-05T04:00:00Z"),
            ZoneOffset.UTC);
    private static final LocalDateTime NOW = LocalDateTime.ofInstant(CLOCK.instant(), ZoneOffset.UTC);

    @Test
    void freshPassRunWithRequiredContractsPassesGate() {
        CdpWarehouseE2eCertificationRunMapper mapper = mock(CdpWarehouseE2eCertificationRunMapper.class);
        when(mapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(
                run(101L, "PASS", NOW.minusMinutes(10), "HYBRID", 1,
                        "[\"bi_daily_active_users\",\"audience_12\"]")));
        CdpWarehouseE2eCertificationGateService service =
                new CdpWarehouseE2eCertificationGateService(mapper, CLOCK);

        CdpWarehouseE2eCertificationGateService.GateDecision decision =
                service.evaluate(9L, "HYBRID",
                        List.of("audience_12", "bi_daily_active_users"), true, 60);

        assertThat(decision.status()).isEqualTo("PASS");
        assertThat(decision.reason()).contains("fresh PASS");
        assertThat(decision.matchedRunId()).isEqualTo(101L);
        assertThat(decision.expiresAt()).isEqualTo(NOW.minusMinutes(10).plusMinutes(60));
    }

    @Test
    void failedRunFailsGate() {
        CdpWarehouseE2eCertificationRunMapper mapper = mock(CdpWarehouseE2eCertificationRunMapper.class);
        when(mapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(
                run(102L, "FAIL", NOW.minusMinutes(5), "HYBRID", 1, "[\"audience_12\"]")));
        CdpWarehouseE2eCertificationGateService service =
                new CdpWarehouseE2eCertificationGateService(mapper, CLOCK);

        CdpWarehouseE2eCertificationGateService.GateDecision decision =
                service.evaluate(9L, "HYBRID", List.of("audience_12"), true, 60);

        assertThat(decision.status()).isEqualTo("FAIL");
        assertThat(decision.reason()).contains("latest matching run status is FAIL");
        assertThat(decision.matchedRunId()).isEqualTo(102L);
    }

    @Test
    void stalePassRunFailsGate() {
        CdpWarehouseE2eCertificationRunMapper mapper = mock(CdpWarehouseE2eCertificationRunMapper.class);
        when(mapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(
                run(103L, "PASS", NOW.minusMinutes(90), "HYBRID", 1, "[\"audience_12\"]")));
        CdpWarehouseE2eCertificationGateService service =
                new CdpWarehouseE2eCertificationGateService(mapper, CLOCK);

        CdpWarehouseE2eCertificationGateService.GateDecision decision =
                service.evaluate(9L, "HYBRID", List.of("audience_12"), true, 60);

        assertThat(decision.status()).isEqualTo("FAIL");
        assertThat(decision.reason()).contains("stale");
        assertThat(decision.expiresAt()).isEqualTo(NOW.minusMinutes(90).plusMinutes(60));
    }

    @Test
    void missingRequiredContractFailsGate() {
        CdpWarehouseE2eCertificationRunMapper mapper = mock(CdpWarehouseE2eCertificationRunMapper.class);
        when(mapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(
                run(104L, "PASS", NOW.minusMinutes(10), "HYBRID", 1, "[\"audience_12\"]")));
        CdpWarehouseE2eCertificationGateService service =
                new CdpWarehouseE2eCertificationGateService(mapper, CLOCK);

        CdpWarehouseE2eCertificationGateService.GateDecision decision =
                service.evaluate(9L, "HYBRID", List.of("audience_12", "bi_daily_active_users"), true, 60);

        assertThat(decision.status()).isEqualTo("FAIL");
        assertThat(decision.reason()).contains("missing contract keys");
        assertThat(decision.matchedRunId()).isEqualTo(104L);
    }

    @Test
    void realtimeRequiredGateRejectsRunWithoutRealtimeProof() {
        CdpWarehouseE2eCertificationRunMapper mapper = mock(CdpWarehouseE2eCertificationRunMapper.class);
        CdpWarehouseE2eCertificationRunDO row =
                run(105L, "PASS", NOW.minusMinutes(10), "HYBRID", 1, "[\"audience_12\"]");
        row.setRequireRealtime(0);
        when(mapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(row));
        CdpWarehouseE2eCertificationGateService service =
                new CdpWarehouseE2eCertificationGateService(mapper, CLOCK);

        CdpWarehouseE2eCertificationGateService.GateDecision decision =
                service.evaluate(9L, "HYBRID", List.of("audience_12"), true, true, 60);

        assertThat(decision.status()).isEqualTo("FAIL");
        assertThat(decision.reason()).contains("no matching certification run");
        assertThat(decision.requireRealtime()).isTrue();
    }

    @Test
    void dataPathProofRequiredGateRejectsRunWithoutDataPathProof() {
        CdpWarehouseE2eCertificationRunMapper mapper = mock(CdpWarehouseE2eCertificationRunMapper.class);
        CdpWarehouseE2eCertificationRunDO row =
                run(106L, "PASS", NOW.minusMinutes(10), "HYBRID", 1, "[\"audience_12\"]");
        row.setRequireDataPathProof(0);
        when(mapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(row));
        CdpWarehouseE2eCertificationGateService service =
                new CdpWarehouseE2eCertificationGateService(mapper, CLOCK);

        CdpWarehouseE2eCertificationGateService.GateDecision decision =
                service.evaluate(9L, "HYBRID", List.of("audience_12"), true, true, true, 60);

        assertThat(decision.status()).isEqualTo("FAIL");
        assertThat(decision.reason()).contains("no matching certification run");
        assertThat(decision.requireDataPathProof()).isTrue();
    }

    private CdpWarehouseE2eCertificationRunDO run(Long id,
                                                  String status,
                                                  LocalDateTime finishedAt,
                                                  String mode,
                                                  Integer requirePhysical,
                                                  String contractKeysJson) {
        CdpWarehouseE2eCertificationRunDO row = new CdpWarehouseE2eCertificationRunDO();
        row.setId(id);
        row.setTenantId(9L);
        row.setStatus(status);
        row.setMode(mode);
        row.setRequirePhysical(requirePhysical);
        row.setRequireRealtime(1);
        row.setRequireDataPathProof(1);
        row.setContractKeysJson(contractKeysJson);
        row.setFinishedAt(finishedAt);
        return row;
    }
}
