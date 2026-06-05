package org.chovy.canvas.domain.warehouse;

import org.chovy.canvas.dal.dataobject.AudienceDefinitionDO;
import org.chovy.canvas.dal.mapper.AudienceDefinitionMapper;
import org.chovy.canvas.domain.analytics.AudienceMaterializationOperationsService;
import org.chovy.canvas.domain.analytics.AudienceMaterializationService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class CdpWarehousePrivacyAudienceBitmapRebuildServiceTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 6, 5, 12, 0);

    @Test
    void upstreamNonPassProofBlocksMaterializationAndRecordsWarn() {
        Fixture fixture = fixture();
        when(fixture.erasureService.get(9L, 101L)).thenReturn(request(
                proof("CDP_USER_PROFILE", "PASS"),
                proof("DORIS_ODS_CDP_EVENT_LOG", "WARN"),
                proof("AUDIENCE_BITMAP_VERSION", "PLANNED")));
        when(fixture.erasureService.recordAssetProof(eq(9L), eq(101L), any()))
                .thenReturn(request(proof("AUDIENCE_BITMAP_VERSION", "WARN")));
        CdpWarehousePrivacyAudienceBitmapRebuildService service = fixture.service();

        CdpWarehousePrivacyAudienceBitmapRebuildService.AudienceBitmapRebuildResult result =
                service.rebuild(9L, 101L, command("privacy-ops", 50, null));

        assertThat(result.status()).isEqualTo("WARN");
        assertThat(result.blocked()).isTrue();
        assertThat(result.selectedAudiences()).isZero();
        assertThat(result.rebuiltAudiences()).isZero();
        verifyNoInteractions(fixture.definitionMapper, fixture.operationsService);
        ArgumentCaptor<CdpWarehousePrivacyErasureService.AssetProofCommand> proofCaptor =
                ArgumentCaptor.forClass(CdpWarehousePrivacyErasureService.AssetProofCommand.class);
        verify(fixture.erasureService).recordAssetProof(eq(9L), eq(101L), proofCaptor.capture());
        assertThat(proofCaptor.getValue().assetKey()).isEqualTo("AUDIENCE_BITMAP_VERSION");
        assertThat(proofCaptor.getValue().actionType()).isEqualTo("REBUILD");
        assertThat(proofCaptor.getValue().status()).isEqualTo("WARN");
        assertThat(proofCaptor.getValue().proofMessage()).contains("blocked");
    }

    @Test
    void successfulRebuildRecordsPassProof() {
        Fixture fixture = fixture();
        when(fixture.erasureService.get(9L, 101L)).thenReturn(request(
                proof("CDP_USER_PROFILE", "PASS"),
                proof("DORIS_ODS_CDP_EVENT_LOG", "PASS"),
                proof("REALTIME_RETRY_BUFFER", "SKIPPED"),
                proof("AUDIENCE_BITMAP_VERSION", "PLANNED")));
        when(fixture.definitionMapper.selectList(any()))
                .thenReturn(List.of(definition(12L), definition(13L)));
        when(fixture.operationsService.materialize(9L, 12L, "privacy-ops"))
                .thenReturn(new AudienceMaterializationService.MaterializationResult("SUCCESS", 10));
        when(fixture.operationsService.materialize(9L, 13L, "privacy-ops"))
                .thenReturn(new AudienceMaterializationService.MaterializationResult("SUCCESS", 0));
        when(fixture.erasureService.recordAssetProof(eq(9L), eq(101L), any()))
                .thenReturn(request(proof("AUDIENCE_BITMAP_VERSION", "PASS")));
        CdpWarehousePrivacyAudienceBitmapRebuildService service = fixture.service();

        CdpWarehousePrivacyAudienceBitmapRebuildService.AudienceBitmapRebuildResult result =
                service.rebuild(9L, 101L, command("privacy-ops", 50, null));

        assertThat(result.status()).isEqualTo("PASS");
        assertThat(result.blocked()).isFalse();
        assertThat(result.selectedAudiences()).isEqualTo(2);
        assertThat(result.rebuiltAudiences()).isEqualTo(2);
        assertThat(result.failedAudiences()).isZero();
        assertThat(result.audienceResults()).extracting(
                        CdpWarehousePrivacyAudienceBitmapRebuildService.AudienceRebuildItem::audienceId)
                .containsExactly(12L, 13L);
        verify(fixture.operationsService).materialize(9L, 12L, "privacy-ops");
        verify(fixture.operationsService).materialize(9L, 13L, "privacy-ops");
        ArgumentCaptor<CdpWarehousePrivacyErasureService.AssetProofCommand> proofCaptor =
                ArgumentCaptor.forClass(CdpWarehousePrivacyErasureService.AssetProofCommand.class);
        verify(fixture.erasureService).recordAssetProof(eq(9L), eq(101L), proofCaptor.capture());
        assertThat(proofCaptor.getValue().status()).isEqualTo("PASS");
        assertThat(proofCaptor.getValue().matchedCount()).isEqualTo(2L);
        assertThat(proofCaptor.getValue().affectedCount()).isEqualTo(2L);
        assertThat(proofCaptor.getValue().proofMessage()).contains("rebuilt 2");
    }

    @Test
    void noCandidatesRecordsSkippedProof() {
        Fixture fixture = fixture();
        when(fixture.erasureService.get(9L, 101L)).thenReturn(request(
                proof("CDP_USER_PROFILE", "PASS"),
                proof("AUDIENCE_BITMAP_VERSION", "PLANNED")));
        when(fixture.definitionMapper.selectList(any())).thenReturn(List.of());
        when(fixture.erasureService.recordAssetProof(eq(9L), eq(101L), any()))
                .thenReturn(request(proof("AUDIENCE_BITMAP_VERSION", "SKIPPED")));
        CdpWarehousePrivacyAudienceBitmapRebuildService service = fixture.service();

        CdpWarehousePrivacyAudienceBitmapRebuildService.AudienceBitmapRebuildResult result =
                service.rebuild(9L, 101L, command("privacy-ops", 50, null));

        assertThat(result.status()).isEqualTo("SKIPPED");
        assertThat(result.selectedAudiences()).isZero();
        verify(fixture.operationsService, never()).materialize(any(), any(), any());
        ArgumentCaptor<CdpWarehousePrivacyErasureService.AssetProofCommand> proofCaptor =
                ArgumentCaptor.forClass(CdpWarehousePrivacyErasureService.AssetProofCommand.class);
        verify(fixture.erasureService).recordAssetProof(eq(9L), eq(101L), proofCaptor.capture());
        assertThat(proofCaptor.getValue().status()).isEqualTo("SKIPPED");
        assertThat(proofCaptor.getValue().proofMessage()).contains("no enabled");
    }

    @Test
    void failedMaterializationRecordsFailProof() {
        Fixture fixture = fixture();
        when(fixture.erasureService.get(9L, 101L)).thenReturn(request(
                proof("CDP_USER_PROFILE", "PASS"),
                proof("AUDIENCE_BITMAP_VERSION", "PLANNED")));
        when(fixture.definitionMapper.selectList(any()))
                .thenReturn(List.of(definition(12L), definition(13L)));
        when(fixture.operationsService.materialize(9L, 12L, "privacy-ops"))
                .thenReturn(new AudienceMaterializationService.MaterializationResult("SUCCESS", 10));
        when(fixture.operationsService.materialize(9L, 13L, "privacy-ops"))
                .thenReturn(new AudienceMaterializationService.MaterializationResult("FAILED", 0));
        when(fixture.erasureService.recordAssetProof(eq(9L), eq(101L), any()))
                .thenReturn(request(proof("AUDIENCE_BITMAP_VERSION", "FAIL")));
        CdpWarehousePrivacyAudienceBitmapRebuildService service = fixture.service();

        CdpWarehousePrivacyAudienceBitmapRebuildService.AudienceBitmapRebuildResult result =
                service.rebuild(9L, 101L, command("privacy-ops", 50, null));

        assertThat(result.status()).isEqualTo("FAIL");
        assertThat(result.selectedAudiences()).isEqualTo(2);
        assertThat(result.rebuiltAudiences()).isEqualTo(1);
        assertThat(result.failedAudiences()).isEqualTo(1);
        ArgumentCaptor<CdpWarehousePrivacyErasureService.AssetProofCommand> proofCaptor =
                ArgumentCaptor.forClass(CdpWarehousePrivacyErasureService.AssetProofCommand.class);
        verify(fixture.erasureService).recordAssetProof(eq(9L), eq(101L), proofCaptor.capture());
        assertThat(proofCaptor.getValue().status()).isEqualTo("FAIL");
        assertThat(proofCaptor.getValue().errorMessage()).contains("audience 13");
    }

    private CdpWarehousePrivacyAudienceBitmapRebuildService.AudienceBitmapRebuildCommand command(
            String actor,
            int limit,
            List<Long> audienceIds) {
        return new CdpWarehousePrivacyAudienceBitmapRebuildService.AudienceBitmapRebuildCommand(
                actor, limit, audienceIds);
    }

    private Fixture fixture() {
        return new Fixture(
                mock(CdpWarehousePrivacyErasureService.class),
                mock(AudienceDefinitionMapper.class),
                mock(AudienceMaterializationOperationsService.class));
    }

    private CdpWarehousePrivacyErasureService.ErasureRequestView request(
            CdpWarehousePrivacyErasureService.AssetProofView... proofs) {
        return new CdpWarehousePrivacyErasureService.ErasureRequestView(
                101L,
                9L,
                "dsr-101",
                "USER_ID",
                "hash-101",
                "us***23",
                "privacy request",
                "privacy-ops",
                "RUNNING",
                NOW.plusHours(1),
                NOW.minusMinutes(5),
                null,
                "[]",
                "[]",
                List.of(proofs),
                NOW.minusMinutes(5),
                NOW.minusMinutes(5));
    }

    private CdpWarehousePrivacyErasureService.AssetProofView proof(String assetKey, String status) {
        return new CdpWarehousePrivacyErasureService.AssetProofView(
                null,
                9L,
                101L,
                "dsr-101",
                assetKey,
                assetKey.startsWith("AUDIENCE") ? "ADS" : "CDP",
                "ERASURE_PROOF",
                status,
                "prove erasure propagation for " + assetKey,
                0,
                0,
                null,
                null,
                null,
                null,
                null,
                null);
    }

    private AudienceDefinitionDO definition(Long id) {
        AudienceDefinitionDO row = new AudienceDefinitionDO();
        row.setId(id);
        row.setTenantId(9L);
        row.setEnabled(1);
        row.setEvaluationStrategy("HYBRID");
        return row;
    }

    private record Fixture(
            CdpWarehousePrivacyErasureService erasureService,
            AudienceDefinitionMapper definitionMapper,
            AudienceMaterializationOperationsService operationsService) {

        CdpWarehousePrivacyAudienceBitmapRebuildService service() {
            return new CdpWarehousePrivacyAudienceBitmapRebuildService(
                    erasureService,
                    definitionMapper,
                    operationsService);
        }
    }
}
