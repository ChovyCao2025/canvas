package org.chovy.canvas.domain.warehouse;

import org.chovy.canvas.dal.dataobject.CdpEventLogDO;
import org.chovy.canvas.dal.mapper.CdpEventLogMapper;
import org.chovy.canvas.dal.mapper.CdpUserIdentityMapper;
import org.chovy.canvas.dal.mapper.CdpUserProfileMapper;
import org.chovy.canvas.dal.mapper.CdpUserTagMapper;
import org.chovy.canvas.dal.mapper.CdpWarehouseRealtimeRetryMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CdpWarehousePrivacyErasureExecutionServiceTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 6, 5, 12, 0);

    @Test
    void hashMismatchRejectsBeforeMutationOrProofRecording() {
        Fixture fixture = fixture(null);
        when(fixture.erasureService.get(9L, 101L)).thenReturn(request("user-123", List.of("CDP_USER_PROFILE")));
        CdpWarehousePrivacyErasureExecutionService service = fixture.service();

        assertThatThrownBy(() -> service.execute(9L, 101L,
                new CdpWarehousePrivacyErasureExecutionService.ErasureExecutionCommand(
                        "other-user", false, "privacy-ops", null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("subjectValue does not match");

        verify(fixture.profileMapper, never()).delete(any());
        verify(fixture.erasureService, never()).recordAssetProof(any(), any(), any());
    }

    @Test
    void dryRunRecordsMatchedCountsWithoutDeletingRows() {
        Fixture fixture = fixture(null);
        when(fixture.erasureService.get(9L, 101L)).thenReturn(request("user-123",
                List.of("CDP_USER_PROFILE", "CDP_EVENT_LOG")));
        when(fixture.profileMapper.selectCount(any())).thenReturn(1L);
        when(fixture.eventLogMapper.selectList(any())).thenReturn(List.of(event(501L)));
        when(fixture.eventLogMapper.selectCount(any())).thenReturn(1L);
        when(fixture.erasureService.recordAssetProof(eq(9L), eq(101L), any()))
                .thenReturn(request("user-123", List.of("CDP_USER_PROFILE", "CDP_EVENT_LOG")));
        CdpWarehousePrivacyErasureExecutionService service = fixture.service();

        CdpWarehousePrivacyErasureExecutionService.ErasureExecutionResult result = service.execute(9L, 101L,
                new CdpWarehousePrivacyErasureExecutionService.ErasureExecutionCommand(
                        "user-123", true, "privacy-ops", null));

        assertThat(result.assetResults()).hasSize(2);
        assertThat(result.status()).isEqualTo("WARN");
        verify(fixture.profileMapper, never()).delete(any());
        verify(fixture.eventLogMapper, never()).delete(any());
        ArgumentCaptor<CdpWarehousePrivacyErasureService.AssetProofCommand> proofCaptor =
                ArgumentCaptor.forClass(CdpWarehousePrivacyErasureService.AssetProofCommand.class);
        verify(fixture.erasureService, org.mockito.Mockito.times(2))
                .recordAssetProof(eq(9L), eq(101L), proofCaptor.capture());
        assertThat(proofCaptor.getAllValues()).allSatisfy(proof -> {
            assertThat(proof.status()).isEqualTo("WARN");
            assertThat(proof.actionType()).isEqualTo("DRY_RUN");
            assertThat(proof.matchedCount()).isEqualTo(1L);
            assertThat(proof.affectedCount()).isZero();
            assertThat(proof.proofMessage()).contains("dry-run");
        });
    }

    @Test
    void executionDeletesCdpEventAndRetryAssetsAndRecordsPassProof() {
        Fixture fixture = fixture(null);
        when(fixture.erasureService.get(9L, 101L)).thenReturn(request("user-123",
                List.of("CDP_USER_PROFILE", "CDP_USER_IDENTITY", "CDP_USER_TAG", "CDP_EVENT_LOG", "REALTIME_RETRY_BUFFER")));
        when(fixture.profileMapper.selectCount(any())).thenReturn(1L);
        when(fixture.identityMapper.selectCount(any())).thenReturn(2L);
        when(fixture.tagMapper.selectCount(any())).thenReturn(3L);
        when(fixture.eventLogMapper.selectList(any())).thenReturn(List.of(event(501L), event(502L)));
        when(fixture.eventLogMapper.selectCount(any())).thenReturn(2L);
        when(fixture.retryMapper.selectCount(any())).thenReturn(2L);
        when(fixture.profileMapper.delete(any())).thenReturn(1);
        when(fixture.identityMapper.delete(any())).thenReturn(2);
        when(fixture.tagMapper.delete(any())).thenReturn(3);
        when(fixture.eventLogMapper.delete(any())).thenReturn(2);
        when(fixture.retryMapper.delete(any())).thenReturn(2);
        when(fixture.erasureService.recordAssetProof(eq(9L), eq(101L), any()))
                .thenReturn(request("user-123", List.of()));
        CdpWarehousePrivacyErasureExecutionService service = fixture.service();

        CdpWarehousePrivacyErasureExecutionService.ErasureExecutionResult result = service.execute(9L, 101L,
                new CdpWarehousePrivacyErasureExecutionService.ErasureExecutionCommand(
                        "user-123", false, "privacy-ops", null));

        assertThat(result.status()).isEqualTo("PASS");
        assertThat(result.assetResults()).extracting(CdpWarehousePrivacyErasureExecutionService.AssetExecutionResult::assetKey)
                .containsExactly("CDP_USER_PROFILE", "CDP_USER_IDENTITY", "CDP_USER_TAG", "CDP_EVENT_LOG", "REALTIME_RETRY_BUFFER");
        verify(fixture.profileMapper).delete(any());
        verify(fixture.identityMapper).delete(any());
        verify(fixture.tagMapper).delete(any());
        verify(fixture.eventLogMapper).delete(any());
        verify(fixture.retryMapper).delete(any());
        ArgumentCaptor<CdpWarehousePrivacyErasureService.AssetProofCommand> proofCaptor =
                ArgumentCaptor.forClass(CdpWarehousePrivacyErasureService.AssetProofCommand.class);
        verify(fixture.erasureService, org.mockito.Mockito.times(5))
                .recordAssetProof(eq(9L), eq(101L), proofCaptor.capture());
        assertThat(proofCaptor.getAllValues()).allSatisfy(proof -> {
            assertThat(proof.status()).isEqualTo("PASS");
            assertThat(proof.actionType()).isEqualTo("DELETE");
            assertThat(proof.affectedCount()).isEqualTo(proof.matchedCount());
        });
    }

    @Test
    void missingDorisExecutorRecordsFailForExecutedDorisAsset() {
        Fixture fixture = fixture(null);
        when(fixture.erasureService.get(9L, 101L)).thenReturn(request("user-123",
                List.of("DORIS_ODS_CDP_EVENT_LOG")));
        when(fixture.erasureService.recordAssetProof(eq(9L), eq(101L), any()))
                .thenReturn(request("user-123", List.of("DORIS_ODS_CDP_EVENT_LOG")));
        CdpWarehousePrivacyErasureExecutionService service = fixture.service();

        CdpWarehousePrivacyErasureExecutionService.ErasureExecutionResult result = service.execute(9L, 101L,
                new CdpWarehousePrivacyErasureExecutionService.ErasureExecutionCommand(
                        "user-123", false, "privacy-ops", null));

        assertThat(result.status()).isEqualTo("FAIL");
        assertThat(result.assetResults()).singleElement().satisfies(asset -> {
            assertThat(asset.assetKey()).isEqualTo("DORIS_ODS_CDP_EVENT_LOG");
            assertThat(asset.status()).isEqualTo("FAIL");
            assertThat(asset.errorMessage()).contains("Doris privacy erasure executor is not configured");
        });
        ArgumentCaptor<CdpWarehousePrivacyErasureService.AssetProofCommand> proofCaptor =
                ArgumentCaptor.forClass(CdpWarehousePrivacyErasureService.AssetProofCommand.class);
        verify(fixture.erasureService).recordAssetProof(eq(9L), eq(101L), proofCaptor.capture());
        assertThat(proofCaptor.getValue().status()).isEqualTo("FAIL");
        assertThat(proofCaptor.getValue().errorMessage()).contains("Doris privacy erasure executor is not configured");
    }

    private Fixture fixture(CdpWarehouseDorisPrivacyErasureExecutor dorisExecutor) {
        return new Fixture(
                mock(CdpWarehousePrivacyErasureService.class),
                mock(CdpUserProfileMapper.class),
                mock(CdpUserIdentityMapper.class),
                mock(CdpUserTagMapper.class),
                mock(CdpEventLogMapper.class),
                mock(CdpWarehouseRealtimeRetryMapper.class),
                dorisExecutor);
    }

    private CdpWarehousePrivacyErasureService.ErasureRequestView request(String subjectValue, List<String> assets) {
        return new CdpWarehousePrivacyErasureService.ErasureRequestView(
                101L,
                9L,
                "dsr-101",
                "USER_ID",
                hash(9L, "USER_ID", subjectValue),
                "us***23",
                "privacy request",
                "privacy-ops",
                "RUNNING",
                NOW.plusHours(1),
                NOW.minusMinutes(5),
                null,
                "[]",
                "[]",
                assets.stream()
                        .map(asset -> new CdpWarehousePrivacyErasureService.AssetProofView(
                                null, 9L, 101L, "dsr-101", asset, asset.startsWith("DORIS") ? "ODS" : "CDP",
                                "ERASURE_PROOF", "PLANNED", "prove erasure propagation for " + asset,
                                0, 0, null, null, null, null, null, null))
                        .toList(),
                NOW.minusMinutes(5),
                NOW.minusMinutes(5));
    }

    private CdpEventLogDO event(Long id) {
        CdpEventLogDO row = new CdpEventLogDO();
        row.setId(id);
        row.setTenantId(9L);
        row.setUserId("user-123");
        return row;
    }

    private String hash(Long tenantId, String subjectType, String subjectValue) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(
                    (tenantId + ":" + subjectType + ":" + subjectValue).getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private record Fixture(
            CdpWarehousePrivacyErasureService erasureService,
            CdpUserProfileMapper profileMapper,
            CdpUserIdentityMapper identityMapper,
            CdpUserTagMapper tagMapper,
            CdpEventLogMapper eventLogMapper,
            CdpWarehouseRealtimeRetryMapper retryMapper,
            CdpWarehouseDorisPrivacyErasureExecutor dorisExecutor) {

        CdpWarehousePrivacyErasureExecutionService service() {
            return new CdpWarehousePrivacyErasureExecutionService(
                    erasureService,
                    profileMapper,
                    identityMapper,
                    tagMapper,
                    eventLogMapper,
                    retryMapper,
                    dorisExecutor);
        }
    }
}
