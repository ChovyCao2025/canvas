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

class CdpWarehouseE2eCertificationGateSourceModeTest {

    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-06-06T04:00:00Z"),
            ZoneOffset.UTC);
    private static final LocalDateTime NOW = LocalDateTime.ofInstant(CLOCK.instant(), ZoneOffset.UTC);

    @Test
    void realtimeDataPathGatePassesOnlyForMysqlCdcProof() {
        CdpWarehouseE2eCertificationRunMapper mapper = mock(CdpWarehouseE2eCertificationRunMapper.class);
        when(mapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(
                run(101L, "MYSQL_CDC", "PASS", "SKIPPED", "PASS")));
        CdpWarehouseE2eCertificationGateService service =
                new CdpWarehouseE2eCertificationGateService(mapper, CLOCK);

        CdpWarehouseE2eCertificationGateService.GateDecision decision =
                service.evaluate(9L, "HYBRID", List.of("audience_12"), true, true, true, 60);

        assertThat(decision.status()).isEqualTo("PASS");
        assertThat(decision.reason()).contains("fresh PASS");
    }

    @Test
    void realtimeDataPathGateRejectsDirectSinkProof() {
        CdpWarehouseE2eCertificationRunMapper mapper = mock(CdpWarehouseE2eCertificationRunMapper.class);
        when(mapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(
                run(102L, "DIRECT_SINK", "SKIPPED", "PASS", "PASS")));
        CdpWarehouseE2eCertificationGateService service =
                new CdpWarehouseE2eCertificationGateService(mapper, CLOCK);

        CdpWarehouseE2eCertificationGateService.GateDecision decision =
                service.evaluate(9L, "HYBRID", List.of("audience_12"), true, true, true, 60);

        assertThat(decision.status()).isEqualTo("FAIL");
        assertThat(decision.reason()).contains("requires dataPathProof sourceMode MYSQL_CDC");
        assertThat(decision.matchedRunId()).isEqualTo(102L);
    }

    private CdpWarehouseE2eCertificationRunDO run(Long id,
                                                  String sourceMode,
                                                  String sourceStatus,
                                                  String sinkStatus,
                                                  String odsStatus) {
        CdpWarehouseE2eCertificationRunDO row = new CdpWarehouseE2eCertificationRunDO();
        row.setId(id);
        row.setTenantId(9L);
        row.setStatus("PASS");
        row.setMode("HYBRID");
        row.setRequirePhysical(1);
        row.setRequireRealtime(1);
        row.setRequireDataPathProof(1);
        row.setContractKeysJson("[\"audience_12\"]");
        row.setDataPathProofJson("""
                {
                  "sourceMode": "%s",
                  "sourceStatus": "%s",
                  "sinkStatus": "%s",
                  "odsStatus": "%s",
                  "status": "PASS",
                  "odsRowCount": 1
                }
                """.formatted(sourceMode, sourceStatus, sinkStatus, odsStatus));
        row.setFinishedAt(NOW.minusMinutes(5));
        return row;
    }
}
