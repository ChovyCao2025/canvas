package org.chovy.canvas.domain.warehouse;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CdpWarehousePrivacyAudienceBitmapRebuildAutomationServiceTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 6, 5, 12, 0);

    @Test
    void eligibleRequestInvokesAudienceBitmapRebuild() {
        CdpWarehousePrivacyErasureService erasureService = mock(CdpWarehousePrivacyErasureService.class);
        CdpWarehousePrivacyAudienceBitmapRebuildService rebuildService =
                mock(CdpWarehousePrivacyAudienceBitmapRebuildService.class);
        CdpWarehousePrivacyErasureService.ErasureRequestView request = request(101L, "WARN",
                proof("CDP_USER_PROFILE", "PASS"),
                proof("DORIS_ODS_CDP_EVENT_LOG", "SKIPPED"),
                proof("AUDIENCE_BITMAP_VERSION", "WARN"));
        when(erasureService.recent(9L, null, 20)).thenReturn(List.of(request));
        CdpWarehousePrivacyAudienceBitmapRebuildService.AudienceBitmapRebuildCommand rebuildCommand =
                new CdpWarehousePrivacyAudienceBitmapRebuildService.AudienceBitmapRebuildCommand(
                        "privacy-rebuild-scheduler", 50, null);
        when(rebuildService.rebuild(9L, 101L, rebuildCommand)).thenReturn(rebuildResult(101L, "PASS"));
        CdpWarehousePrivacyAudienceBitmapRebuildAutomationService service =
                new CdpWarehousePrivacyAudienceBitmapRebuildAutomationService(erasureService, rebuildService);

        CdpWarehousePrivacyAudienceBitmapRebuildAutomationService.AutomationResult result =
                service.run(9L, new CdpWarehousePrivacyAudienceBitmapRebuildAutomationService.AutomationCommand(
                        "privacy-rebuild-scheduler", 20, 50, false));

        assertThat(result.status()).isEqualTo("PASS");
        assertThat(result.scanned()).isEqualTo(1);
        assertThat(result.eligible()).isEqualTo(1);
        assertThat(result.triggered()).isEqualTo(1);
        assertThat(result.failed()).isZero();
        verify(rebuildService).rebuild(9L, 101L, rebuildCommand);
    }

    @Test
    void upstreamNonPassRequestIsSkipped() {
        CdpWarehousePrivacyErasureService erasureService = mock(CdpWarehousePrivacyErasureService.class);
        CdpWarehousePrivacyAudienceBitmapRebuildService rebuildService =
                mock(CdpWarehousePrivacyAudienceBitmapRebuildService.class);
        CdpWarehousePrivacyErasureService.ErasureRequestView request = request(101L, "WARN",
                proof("CDP_USER_PROFILE", "WARN"),
                proof("AUDIENCE_BITMAP_VERSION", "WARN"));
        when(erasureService.recent(9L, null, 20)).thenReturn(List.of(request));
        CdpWarehousePrivacyAudienceBitmapRebuildAutomationService service =
                new CdpWarehousePrivacyAudienceBitmapRebuildAutomationService(erasureService, rebuildService);

        CdpWarehousePrivacyAudienceBitmapRebuildAutomationService.AutomationResult result =
                service.run(9L, new CdpWarehousePrivacyAudienceBitmapRebuildAutomationService.AutomationCommand(
                        "privacy-rebuild-scheduler", 20, 50, false));

        assertThat(result.status()).isEqualTo("SKIPPED");
        assertThat(result.scanned()).isEqualTo(1);
        assertThat(result.eligible()).isZero();
        assertThat(result.skipped()).isEqualTo(1);
        assertThat(result.requestResults()).singleElement().satisfies(item -> {
            assertThat(item.status()).isEqualTo("SKIPPED");
            assertThat(item.reason()).contains("upstream");
        });
        verify(rebuildService, never()).rebuild(org.mockito.Mockito.any(), org.mockito.Mockito.any(), org.mockito.Mockito.any());
    }

    @Test
    void failedAudienceProofIsSkippedByDefault() {
        CdpWarehousePrivacyErasureService erasureService = mock(CdpWarehousePrivacyErasureService.class);
        CdpWarehousePrivacyAudienceBitmapRebuildService rebuildService =
                mock(CdpWarehousePrivacyAudienceBitmapRebuildService.class);
        CdpWarehousePrivacyErasureService.ErasureRequestView request = request(101L, "FAIL",
                proof("CDP_USER_PROFILE", "PASS"),
                proof("AUDIENCE_BITMAP_VERSION", "FAIL"));
        when(erasureService.recent(9L, null, 20)).thenReturn(List.of(request));
        CdpWarehousePrivacyAudienceBitmapRebuildAutomationService service =
                new CdpWarehousePrivacyAudienceBitmapRebuildAutomationService(erasureService, rebuildService);

        CdpWarehousePrivacyAudienceBitmapRebuildAutomationService.AutomationResult result =
                service.run(9L, new CdpWarehousePrivacyAudienceBitmapRebuildAutomationService.AutomationCommand(
                        "privacy-rebuild-scheduler", 20, 50, false));

        assertThat(result.status()).isEqualTo("SKIPPED");
        assertThat(result.eligible()).isZero();
        assertThat(result.requestResults()).singleElement().satisfies(item -> {
            assertThat(item.status()).isEqualTo("SKIPPED");
            assertThat(item.reason()).contains("retryFailed");
        });
        verify(rebuildService, never()).rebuild(org.mockito.Mockito.any(), org.mockito.Mockito.any(), org.mockito.Mockito.any());
    }

    @Test
    void failedAudienceProofRetriesWhenConfigured() {
        CdpWarehousePrivacyErasureService erasureService = mock(CdpWarehousePrivacyErasureService.class);
        CdpWarehousePrivacyAudienceBitmapRebuildService rebuildService =
                mock(CdpWarehousePrivacyAudienceBitmapRebuildService.class);
        CdpWarehousePrivacyErasureService.ErasureRequestView request = request(101L, "FAIL",
                proof("CDP_USER_PROFILE", "PASS"),
                proof("AUDIENCE_BITMAP_VERSION", "FAIL"));
        when(erasureService.recent(9L, null, 20)).thenReturn(List.of(request));
        CdpWarehousePrivacyAudienceBitmapRebuildService.AudienceBitmapRebuildCommand rebuildCommand =
                new CdpWarehousePrivacyAudienceBitmapRebuildService.AudienceBitmapRebuildCommand(
                        "privacy-rebuild-scheduler", 50, null);
        when(rebuildService.rebuild(9L, 101L, rebuildCommand)).thenReturn(rebuildResult(101L, "PASS"));
        CdpWarehousePrivacyAudienceBitmapRebuildAutomationService service =
                new CdpWarehousePrivacyAudienceBitmapRebuildAutomationService(erasureService, rebuildService);

        CdpWarehousePrivacyAudienceBitmapRebuildAutomationService.AutomationResult result =
                service.run(9L, new CdpWarehousePrivacyAudienceBitmapRebuildAutomationService.AutomationCommand(
                        "privacy-rebuild-scheduler", 20, 50, true));

        assertThat(result.status()).isEqualTo("PASS");
        assertThat(result.eligible()).isEqualTo(1);
        verify(rebuildService).rebuild(9L, 101L, rebuildCommand);
    }

    private CdpWarehousePrivacyAudienceBitmapRebuildService.AudienceBitmapRebuildResult rebuildResult(
            Long requestId,
            String status) {
        return new CdpWarehousePrivacyAudienceBitmapRebuildService.AudienceBitmapRebuildResult(
                9L, requestId, status, false, false, 1, 1, 0, 10, List.of());
    }

    private CdpWarehousePrivacyErasureService.ErasureRequestView request(
            Long id,
            String status,
            CdpWarehousePrivacyErasureService.AssetProofView... proofs) {
        return new CdpWarehousePrivacyErasureService.ErasureRequestView(
                id,
                9L,
                "dsr-" + id,
                "USER_ID",
                "hash-" + id,
                "us***23",
                "privacy request",
                "privacy-ops",
                status,
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
}
