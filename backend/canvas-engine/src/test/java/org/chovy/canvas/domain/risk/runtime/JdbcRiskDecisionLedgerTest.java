package org.chovy.canvas.domain.risk.runtime;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.dal.dataobject.RiskDecisionRunDO;
import org.chovy.canvas.dal.dataobject.RiskRuleHitDO;
import org.chovy.canvas.dal.mapper.RiskDecisionRunMapper;
import org.chovy.canvas.dal.mapper.RiskRuleHitMapper;
import org.chovy.canvas.domain.risk.dsl.RiskRuntimeMode;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JdbcRiskDecisionLedgerTest {

    private final RiskDecisionRunMapper runMapper = mock(RiskDecisionRunMapper.class);
    private final RiskRuleHitMapper hitMapper = mock(RiskRuleHitMapper.class);
    private final JdbcRiskDecisionLedger ledger = new JdbcRiskDecisionLedger(runMapper, hitMapper, new ObjectMapper());

    @Test
    void findByRequestQueriesTenantScopedRequestAndMapsPersistedOutput() {
        RiskDecisionResponse response = response("req-1", "9001");
        RiskDecisionRunDO row = runRow(9001L, response);
        row.setInputSnapshotJson("{subject={userId=u***3, phone=***4567}}");
        when(runMapper.selectOne(any())).thenReturn(row);

        Optional<RiskDecisionRunRecord> found = ledger.findByRequest(10L, "req-1");

        assertThat(found).isPresent();
        assertThat(found.get().decisionRunId()).isEqualTo("9001");
        assertThat(found.get().tenantId()).isEqualTo(10L);
        assertThat(found.get().requestId()).isEqualTo("req-1");
        assertThat(found.get().requestHash()).isEqualTo("hash-1");
        assertThat(found.get().subjectHash()).isEqualTo("subject-hash-1");
        assertThat(found.get().inputSnapshotJson()).doesNotContain("user@example.com", "+15551234567");
        assertThat(found.get().response()).isEqualTo(response);
        ArgumentCaptor<Wrapper<RiskDecisionRunDO>> queryCaptor = ArgumentCaptor.forClass(Wrapper.class);
        verify(runMapper).selectOne(queryCaptor.capture());
        assertThat(queryCaptor.getValue().getSqlSegment()).contains("tenant_id", "request_id");
    }

    @Test
    void saveRunInsertsMaskedDecisionRunAndReturnsGeneratedDecisionRunId() {
        when(runMapper.insert(any(RiskDecisionRunDO.class))).thenAnswer(invocation -> {
            RiskDecisionRunDO row = invocation.getArgument(0);
            row.setId(7007L);
            return 1;
        });
        RiskDecisionResponse response = response("req-save", null);
        RiskDecisionRunRecord record = new RiskDecisionRunRecord(
                null,
                10L,
                "req-save",
                "hash-save",
                "subject-hash-save",
                "{subject={email=u***@example.com, phone=***4567}}",
                response);

        RiskDecisionRunRecord saved = ledger.saveRun(record);

        ArgumentCaptor<RiskDecisionRunDO> rowCaptor = ArgumentCaptor.forClass(RiskDecisionRunDO.class);
        verify(runMapper).insert(rowCaptor.capture());
        RiskDecisionRunDO row = rowCaptor.getValue();
        assertThat(row.getTenantId()).isEqualTo(10L);
        assertThat(row.getRequestId()).isEqualTo("req-save");
        assertThat(row.getRequestHash()).isEqualTo("hash-save");
        assertThat(row.getSubjectHash()).isEqualTo("subject-hash-save");
        assertThat(row.getSceneKey()).isEqualTo("BENEFIT");
        assertThat(row.getStrategyKey()).isEqualTo("benefit_default");
        assertThat(row.getStrategyVersion()).isEqualTo(3);
        assertThat(row.getDecision()).isEqualTo("REVIEW");
        assertThat(row.getScore()).isEqualTo(65);
        assertThat(row.getRiskBand()).isEqualTo("MEDIUM");
        assertThat(row.getMode()).isEqualTo("ENFORCE");
        assertThat(row.getLatencyMs()).isEqualTo(17);
        assertThat(row.getStatus()).isEqualTo("SUCCEEDED");
        assertThat(row.getInputSnapshotJson()).doesNotContain("user@example.com", "+15551234567");
        assertThat(row.getOutputJson()).contains("\"requestId\":\"req-save\"", "\"action\":\"REVIEW\"");
        assertThat(saved.decisionRunId()).isEqualTo("7007");
        assertThat(saved.response().decisionRunId()).isEqualTo("7007");
    }

    @Test
    void saveRuleHitsInsertsTenantScopedHitsFromSavedRunContext() {
        when(runMapper.insert(any(RiskDecisionRunDO.class))).thenAnswer(invocation -> {
            RiskDecisionRunDO row = invocation.getArgument(0);
            row.setId(42L);
            return 1;
        });
        RiskDecisionRunRecord saved = ledger.saveRun(new RiskDecisionRunRecord(
                null,
                10L,
                "req-hits",
                "hash-hits",
                "subject-hash-hits",
                "{subject={userId=u***3}}",
                response("req-hits", null)));

        ledger.saveRuleHits(saved.decisionRunId(), List.of(
                new RiskDecisionRuleHit("velocity", "score-high", RiskDecisionAction.BLOCK, 90, "score-high", false),
                new RiskDecisionRuleHit("velocity", "shadow-review", RiskDecisionAction.REVIEW, 20, "shadow-review", true)));

        ArgumentCaptor<RiskRuleHitDO> hitCaptor = ArgumentCaptor.forClass(RiskRuleHitDO.class);
        verify(hitMapper, org.mockito.Mockito.times(2)).insert(hitCaptor.capture());
        assertThat(hitCaptor.getAllValues()).extracting(RiskRuleHitDO::getTenantId).containsExactly(10L, 10L);
        assertThat(hitCaptor.getAllValues()).extracting(RiskRuleHitDO::getDecisionRunId).containsExactly(42L, 42L);
        assertThat(hitCaptor.getAllValues()).extracting(RiskRuleHitDO::getStrategyKey)
                .containsExactly("benefit_default", "benefit_default");
        assertThat(hitCaptor.getAllValues()).extracting(RiskRuleHitDO::getStrategyVersion).containsExactly(3, 3);
        assertThat(hitCaptor.getAllValues()).extracting(RiskRuleHitDO::getMode).containsExactly("ENFORCE", "ENFORCE");
        assertThat(hitCaptor.getAllValues()).extracting(RiskRuleHitDO::getRuleKey)
                .containsExactly("score-high", "shadow-review");
        assertThat(hitCaptor.getAllValues()).extracting(RiskRuleHitDO::getAction).containsExactly("BLOCK", "REVIEW");
        assertThat(hitCaptor.getAllValues()).extracting(RiskRuleHitDO::getScoreDelta).containsExactly(90, 20);
        assertThat(hitCaptor.getAllValues()).extracting(RiskRuleHitDO::getReasonCode)
                .containsExactly("score-high", "shadow-review");
        assertThat(hitCaptor.getAllValues()).extracting(RiskRuleHitDO::getEvidenceJson)
                .allSatisfy(json -> assertThat(json).doesNotContain("user@example.com", "+15551234567"));
        assertThat(hitCaptor.getAllValues().get(1).getEvidenceJson()).contains("\"shadow\":true");
    }

    private RiskDecisionRunDO runRow(Long id, RiskDecisionResponse response) {
        RiskDecisionRunDO row = new RiskDecisionRunDO();
        row.setId(id);
        row.setTenantId(10L);
        row.setRequestId(response.requestId());
        row.setRequestHash("hash-1");
        row.setSubjectHash("subject-hash-1");
        row.setSceneKey(response.sceneKey());
        row.setStrategyKey(response.strategyKey());
        row.setStrategyVersion(response.strategyVersion());
        row.setDecision(response.action().name());
        row.setScore(response.score());
        row.setRiskBand(response.riskBand().name());
        row.setMode(response.mode().name());
        row.setLatencyMs(response.latencyMs());
        row.setStatus("SUCCEEDED");
        row.setOutputJson(writeJson(response));
        return row;
    }

    private RiskDecisionResponse response(String requestId, String decisionRunId) {
        return new RiskDecisionResponse(
                requestId,
                decisionRunId,
                "BENEFIT",
                "benefit_default",
                3,
                RiskRuntimeMode.ENFORCE,
                RiskDecisionAction.REVIEW,
                65,
                RiskBand.MEDIUM,
                List.of("score-high"),
                List.of("velocity:score-high"),
                List.of("mode:ENFORCE"),
                List.of(),
                17,
                true);
    }

    private String writeJson(Object value) {
        try {
            return new ObjectMapper().writeValueAsString(value);
        } catch (Exception error) {
            throw new IllegalStateException(error);
        }
    }
}
