package org.chovy.canvas.domain.warehouse;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.dal.dataobject.CdpWarehousePrivacyErasureAssetProofDO;
import org.chovy.canvas.dal.dataobject.CdpWarehousePrivacyErasureRequestDO;
import org.chovy.canvas.dal.mapper.CdpWarehousePrivacyErasureAssetProofMapper;
import org.chovy.canvas.dal.mapper.CdpWarehousePrivacyErasureRequestMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CdpWarehousePrivacyErasureServiceTest {

    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-06-05T04:00:00Z"),
            ZoneOffset.UTC);
    private static final LocalDateTime NOW = LocalDateTime.ofInstant(CLOCK.instant(), ZoneOffset.UTC);

    @Test
    void createHashesAndMasksSubjectAndCreatesDefaultAssetPlans() {
        CdpWarehousePrivacyErasureRequestMapper requestMapper =
                mock(CdpWarehousePrivacyErasureRequestMapper.class);
        CdpWarehousePrivacyErasureAssetProofMapper proofMapper =
                mock(CdpWarehousePrivacyErasureAssetProofMapper.class);
        doAnswer(invocation -> {
            CdpWarehousePrivacyErasureRequestDO row = invocation.getArgument(0);
            row.setId(101L);
            return 1;
        }).when(requestMapper).insert(any(CdpWarehousePrivacyErasureRequestDO.class));
        when(proofMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());
        CdpWarehousePrivacyErasureService service =
                new CdpWarehousePrivacyErasureService(requestMapper, proofMapper, new ObjectMapper(), CLOCK);

        CdpWarehousePrivacyErasureService.ErasureRequestView view = service.create(9L,
                new CdpWarehousePrivacyErasureService.ErasureRequestCommand(
                        "dsr-1001", "USER_ID", "user-123456",
                        "GDPR delete request", "privacy-ops", NOW.plusHours(2), null));

        assertThat(view.id()).isEqualTo(101L);
        assertThat(view.subjectHash()).hasSize(64);
        assertThat(view.subjectRefMasked()).isEqualTo("us***56");
        assertThat(view.subjectRefMasked()).doesNotContain("user-123456");
        assertThat(view.targetAssetsJson()).contains(
                "CDP_USER_PROFILE",
                "CDP_USER_IDENTITY",
                "CDP_USER_TAG",
                "CDP_EVENT_LOG",
                "DORIS_ODS_CDP_EVENT_LOG",
                "DORIS_DWD_CDP_USER_EVENT_FACT",
                "REALTIME_RETRY_BUFFER",
                "AUDIENCE_BITMAP_VERSION");
        ArgumentCaptor<CdpWarehousePrivacyErasureAssetProofDO> proofCaptor =
                ArgumentCaptor.forClass(CdpWarehousePrivacyErasureAssetProofDO.class);
        verify(proofMapper, org.mockito.Mockito.times(8)).insert(proofCaptor.capture());
        assertThat(proofCaptor.getAllValues())
                .extracting(CdpWarehousePrivacyErasureAssetProofDO::getStatus)
                .containsOnly("PLANNED");
        assertThat(proofCaptor.getAllValues())
                .extracting(CdpWarehousePrivacyErasureAssetProofDO::getAssetKey)
                .containsExactly(
                        "CDP_USER_PROFILE",
                        "CDP_USER_IDENTITY",
                        "CDP_USER_TAG",
                        "CDP_EVENT_LOG",
                        "DORIS_ODS_CDP_EVENT_LOG",
                        "DORIS_DWD_CDP_USER_EVENT_FACT",
                        "REALTIME_RETRY_BUFFER",
                        "AUDIENCE_BITMAP_VERSION");
        assertThat(proofCaptor.getAllValues())
                .extracting(CdpWarehousePrivacyErasureAssetProofDO::getRequestId)
                .containsOnly(101L);
    }

    @Test
    void recordAssetProofRollsRequestStatusToPassWhenAllProofsPass() {
        CdpWarehousePrivacyErasureRequestMapper requestMapper =
                mock(CdpWarehousePrivacyErasureRequestMapper.class);
        CdpWarehousePrivacyErasureAssetProofMapper proofMapper =
                mock(CdpWarehousePrivacyErasureAssetProofMapper.class);
        CdpWarehousePrivacyErasureRequestDO request = request(101L, "PENDING", NOW.plusHours(1));
        CdpWarehousePrivacyErasureAssetProofDO proof = proof(201L, "CDP_USER_PROFILE", "PLANNED");
        when(requestMapper.selectById(101L)).thenReturn(request);
        when(proofMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of(proof))
                .thenReturn(List.of(proof))
                .thenReturn(List.of(proof));
        CdpWarehousePrivacyErasureService service =
                new CdpWarehousePrivacyErasureService(requestMapper, proofMapper, new ObjectMapper(), CLOCK);

        CdpWarehousePrivacyErasureService.ErasureRequestView view = service.recordAssetProof(9L, 101L,
                new CdpWarehousePrivacyErasureService.AssetProofCommand(
                        "CDP_USER_PROFILE", "CDP", "DELETE", "PASS",
                        1L, 1L, "profile row removed", null, "privacy-ops", NOW));

        assertThat(view.status()).isEqualTo("PASS");
        assertThat(view.finishedAt()).isEqualTo(NOW);
        ArgumentCaptor<CdpWarehousePrivacyErasureRequestDO> requestCaptor =
                ArgumentCaptor.forClass(CdpWarehousePrivacyErasureRequestDO.class);
        verify(requestMapper).updateById(requestCaptor.capture());
        assertThat(requestCaptor.getValue().getStatus()).isEqualTo("PASS");
        assertThat(requestCaptor.getValue().getEvidenceJson()).contains("CDP_USER_PROFILE", "profile row removed");
        verify(proofMapper).updateById(proof);
    }

    @Test
    void recordAssetProofRedactsRawSubjectCompatibleWithMaskedReference() {
        CdpWarehousePrivacyErasureRequestMapper requestMapper =
                mock(CdpWarehousePrivacyErasureRequestMapper.class);
        CdpWarehousePrivacyErasureAssetProofMapper proofMapper =
                mock(CdpWarehousePrivacyErasureAssetProofMapper.class);
        CdpWarehousePrivacyErasureRequestDO request = request(101L, "PENDING", NOW.plusHours(1));
        request.setSubjectRefMasked("us***56");
        CdpWarehousePrivacyErasureAssetProofDO proof = proof(201L, "CDP_USER_PROFILE", "PLANNED");
        when(requestMapper.selectById(101L)).thenReturn(request);
        when(proofMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of(proof))
                .thenReturn(List.of(proof))
                .thenReturn(List.of(proof));
        CdpWarehousePrivacyErasureService service =
                new CdpWarehousePrivacyErasureService(requestMapper, proofMapper, new ObjectMapper(), CLOCK);

        CdpWarehousePrivacyErasureService.ErasureRequestView view = service.recordAssetProof(9L, 101L,
                new CdpWarehousePrivacyErasureService.AssetProofCommand(
                        "CDP_USER_PROFILE", "CDP", "DELETE", "PASS",
                        1L, 1L, "deleted profile rows for user-123456",
                        "executor output included user-123456", "privacy-ops", NOW));

        assertThat(view.assetProofs()).hasSize(1);
        assertThat(view.assetProofs().get(0).proofMessage())
                .contains("[REDACTED_SUBJECT]")
                .doesNotContain("user-123456");
        assertThat(view.assetProofs().get(0).errorMessage())
                .contains("[REDACTED_SUBJECT]")
                .doesNotContain("user-123456");
        ArgumentCaptor<CdpWarehousePrivacyErasureRequestDO> requestCaptor =
                ArgumentCaptor.forClass(CdpWarehousePrivacyErasureRequestDO.class);
        verify(requestMapper).updateById(requestCaptor.capture());
        assertThat(requestCaptor.getValue().getEvidenceJson())
                .contains("[REDACTED_SUBJECT]")
                .doesNotContain("user-123456");
        assertThat(proof.getProofMessage()).doesNotContain("user-123456");
        assertThat(proof.getErrorMessage()).doesNotContain("user-123456");
    }

    @Test
    void summaryFailsWhenActiveRequestIsOverdueOrFailed() {
        CdpWarehousePrivacyErasureRequestMapper requestMapper =
                mock(CdpWarehousePrivacyErasureRequestMapper.class);
        CdpWarehousePrivacyErasureAssetProofMapper proofMapper =
                mock(CdpWarehousePrivacyErasureAssetProofMapper.class);
        when(requestMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(
                request(101L, "RUNNING", NOW.minusMinutes(1)),
                request(102L, "PASS", NOW.minusHours(2))));
        CdpWarehousePrivacyErasureService service =
                new CdpWarehousePrivacyErasureService(requestMapper, proofMapper, new ObjectMapper(), CLOCK);

        CdpWarehousePrivacyErasureService.BacklogSummary summary = service.summary(9L);

        assertThat(summary.status()).isEqualTo("FAIL");
        assertThat(summary.activeCount()).isEqualTo(1);
        assertThat(summary.overdueCount()).isEqualTo(1);
        assertThat(summary.reason()).contains("failed or overdue");
    }

    @Test
    void summaryWarnsWhenRequestsAreActiveButNotOverdueOrFailed() {
        CdpWarehousePrivacyErasureRequestMapper requestMapper =
                mock(CdpWarehousePrivacyErasureRequestMapper.class);
        CdpWarehousePrivacyErasureAssetProofMapper proofMapper =
                mock(CdpWarehousePrivacyErasureAssetProofMapper.class);
        when(requestMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(
                request(101L, "RUNNING", NOW.plusMinutes(1)),
                request(102L, "PASS", NOW.minusHours(2))));
        CdpWarehousePrivacyErasureService service =
                new CdpWarehousePrivacyErasureService(requestMapper, proofMapper, new ObjectMapper(), CLOCK);

        CdpWarehousePrivacyErasureService.BacklogSummary summary = service.summary(9L);

        assertThat(summary.status()).isEqualTo("WARN");
        assertThat(summary.activeCount()).isEqualTo(1);
        assertThat(summary.overdueCount()).isZero();
        assertThat(summary.failedCount()).isZero();
        assertThat(summary.pendingCount()).isEqualTo(1);
        assertThat(summary.reason()).contains("active requests");
    }

    @Test
    void summaryPassesWhenThereIsNoActiveBacklog() {
        CdpWarehousePrivacyErasureRequestMapper requestMapper =
                mock(CdpWarehousePrivacyErasureRequestMapper.class);
        CdpWarehousePrivacyErasureAssetProofMapper proofMapper =
                mock(CdpWarehousePrivacyErasureAssetProofMapper.class);
        when(requestMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(
                request(101L, "PASS", NOW.minusHours(1)),
                request(102L, "PASS", NOW.minusHours(2))));
        CdpWarehousePrivacyErasureService service =
                new CdpWarehousePrivacyErasureService(requestMapper, proofMapper, new ObjectMapper(), CLOCK);

        CdpWarehousePrivacyErasureService.BacklogSummary summary = service.summary(9L);

        assertThat(summary.status()).isEqualTo("PASS");
        assertThat(summary.activeCount()).isZero();
        assertThat(summary.overdueCount()).isZero();
        assertThat(summary.failedCount()).isZero();
        assertThat(summary.pendingCount()).isZero();
        assertThat(summary.reason()).contains("clear");
    }

    private CdpWarehousePrivacyErasureRequestDO request(Long id, String status, LocalDateTime dueAt) {
        CdpWarehousePrivacyErasureRequestDO row = new CdpWarehousePrivacyErasureRequestDO();
        row.setId(id);
        row.setTenantId(9L);
        row.setRequestKey("dsr-" + id);
        row.setSubjectType("USER_ID");
        row.setSubjectHash("hash-" + id);
        row.setSubjectRefMasked("us***" + id);
        row.setReason("privacy request");
        row.setRequestedBy("privacy-ops");
        row.setStatus(status);
        row.setDueAt(dueAt);
        row.setStartedAt(NOW.minusHours(1));
        row.setTargetAssetsJson("[]");
        row.setEvidenceJson("[]");
        return row;
    }

    private CdpWarehousePrivacyErasureAssetProofDO proof(Long id, String assetKey, String status) {
        CdpWarehousePrivacyErasureAssetProofDO row = new CdpWarehousePrivacyErasureAssetProofDO();
        row.setId(id);
        row.setTenantId(9L);
        row.setRequestId(101L);
        row.setRequestKey("dsr-101");
        row.setAssetKey(assetKey);
        row.setAssetLayer("CDP");
        row.setActionType("ERASURE_PROOF");
        row.setStatus(status);
        row.setPlannedAction("prove erasure propagation for " + assetKey);
        row.setMatchedCount(0L);
        row.setAffectedCount(0L);
        return row;
    }
}
