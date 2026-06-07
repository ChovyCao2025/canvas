package org.chovy.canvas.domain.search;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.dal.dataobject.SearchMarketingKeywordDO;
import org.chovy.canvas.dal.dataobject.SearchMarketingSnapshotDO;
import org.chovy.canvas.dal.dataobject.SearchMarketingSourceDO;
import org.chovy.canvas.dal.dataobject.SearchMarketingSyncRunDO;
import org.chovy.canvas.dal.dataobject.SearchMarketingUrlInspectionDO;
import org.chovy.canvas.dal.mapper.SearchMarketingKeywordMapper;
import org.chovy.canvas.dal.mapper.SearchMarketingOpportunityMapper;
import org.chovy.canvas.dal.mapper.SearchMarketingSnapshotMapper;
import org.chovy.canvas.dal.mapper.SearchMarketingSourceMapper;
import org.chovy.canvas.dal.mapper.SearchMarketingSyncRunMapper;
import org.chovy.canvas.dal.mapper.SearchMarketingUrlInspectionMapper;
import org.chovy.canvas.domain.providerwrite.ProviderWriteEvidenceSanitizer;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SearchMarketingSyncRunServiceTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-06-06T00:00:00Z"), ZoneId.of("UTC"));
    private static final TypeReference<Map<String, Object>> OBJECT_MAP = new TypeReference<>() {
    };

    @Test
    void manualSyncPersistsRunAndSnapshots() {
        Fixture fixture = fixture();
        when(fixture.readClient.sync(any(), any())).thenReturn(SearchMarketingProviderSyncResult.success(
                "provider-request-1",
                List.of(performanceRow()),
                List.of(),
                Map.of("requestId", "provider-request-1")));

        SearchMarketingSyncRunView view = fixture.service.runManual(7L, 10L, "PERFORMANCE",
                LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 2), "cursor-1", "operator-1");

        assertThat(view.status()).isEqualTo("SUCCEEDED");
        assertThat(view.successCount()).isEqualTo(1L);
        assertThat(view.providerRequestId()).isEqualTo("provider-request-1");
        ArgumentCaptor<SearchMarketingSyncRunDO> runCaptor = ArgumentCaptor.forClass(SearchMarketingSyncRunDO.class);
        verify(fixture.syncRunMapper).insert(runCaptor.capture());
        verify(fixture.syncRunMapper).updateById(runCaptor.getValue());
        assertThat(runCaptor.getValue().getStatus()).isEqualTo("SUCCEEDED");
        assertThat(runCaptor.getValue().getIdempotencyKey()).hasSize(64);
        assertThat(runCaptor.getValue().getCreatedBy()).isEqualTo("operator-1");

        ArgumentCaptor<SearchMarketingKeywordDO> keywordCaptor =
                ArgumentCaptor.forClass(SearchMarketingKeywordDO.class);
        verify(fixture.keywordMapper).insert(keywordCaptor.capture());
        assertThat(keywordCaptor.getValue().getKeywordText()).isEqualTo("Running Shoes");
        assertThat(keywordCaptor.getValue().getChannel()).isEqualTo("SEM");

        ArgumentCaptor<SearchMarketingSnapshotDO> snapshotCaptor =
                ArgumentCaptor.forClass(SearchMarketingSnapshotDO.class);
        verify(fixture.snapshotMapper).insert(snapshotCaptor.capture());
        assertThat(snapshotCaptor.getValue().getTenantId()).isEqualTo(7L);
        assertThat(snapshotCaptor.getValue().getSourceId()).isEqualTo(10L);
        assertThat(snapshotCaptor.getValue().getKeywordId()).isEqualTo(20L);
        assertThat(snapshotCaptor.getValue().getSnapshotDate()).isEqualTo(LocalDate.of(2026, 6, 2));
        assertThat(snapshotCaptor.getValue().getClickCount()).isEqualTo(50L);
    }

    @Test
    void sameSourceWindowAndCursorIsIdempotent() {
        Fixture fixture = fixture();
        SearchMarketingSyncRunDO existing = terminalRun();
        when(fixture.syncRunMapper.selectOne(any())).thenReturn(existing);

        SearchMarketingSyncRunView view = fixture.service.runManual(7L, 10L, "PERFORMANCE",
                LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 2), "cursor-1", "operator-1");

        assertThat(view.id()).isEqualTo(99L);
        assertThat(view.status()).isEqualTo("SUCCEEDED");
        verify(fixture.syncRunMapper, never()).insert(any(SearchMarketingSyncRunDO.class));
        verify(fixture.readClient, never()).sync(any(), any());
    }

    @Test
    void seoTechnicalSyncPersistsUrlInspection() {
        Fixture fixture = fixture();
        when(fixture.readClient.sync(any(), any())).thenReturn(SearchMarketingProviderSyncResult.success(
                "provider-request-2",
                List.of(),
                List.of(urlInspectionRow()),
                Map.of("indexedState", "INDEXED")));

        SearchMarketingSyncRunView view = fixture.service.runManual(7L, 10L, "SEO_TECHNICAL",
                LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 2), null, "operator-1");

        assertThat(view.status()).isEqualTo("SUCCEEDED");
        assertThat(view.successCount()).isEqualTo(1L);
        ArgumentCaptor<SearchMarketingUrlInspectionDO> captor =
                ArgumentCaptor.forClass(SearchMarketingUrlInspectionDO.class);
        verify(fixture.urlInspectionMapper).insert(captor.capture());
        assertThat(captor.getValue().getTenantId()).isEqualTo(7L);
        assertThat(captor.getValue().getSourceId()).isEqualTo(10L);
        assertThat(captor.getValue().getPageUrl()).isEqualTo("https://example.com/shoes");
        assertThat(captor.getValue().getPageUrlHash()).hasSize(64);
        assertThat(captor.getValue().getIndexedState()).isEqualTo("INDEXED");
    }

    @Test
    void providerAuthErrorMarksRunFailed() {
        Fixture fixture = fixture();
        when(fixture.credentialResolver.resolve(eq(7L), eq("GOOGLE_ADS"), eq("google-main")))
                .thenReturn(SearchMarketingCredentialRef.unavailable("google-main", "GOOGLE_ADS",
                        "SEARCH_PROVIDER_CREDENTIAL_EXPIRED", "search provider credential is expired"));

        SearchMarketingSyncRunView view = fixture.service.runManual(7L, 10L, "PERFORMANCE",
                LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 2), null, "operator-1");

        assertThat(view.status()).isEqualTo("FAILED");
        assertThat(view.errorCode()).isEqualTo("SEARCH_PROVIDER_CREDENTIAL_UNAVAILABLE");
        assertThat(view.errorMessage()).contains("expired");
        verify(fixture.readClient, never()).sync(any(), any());
        ArgumentCaptor<SearchMarketingSyncRunDO> captor = ArgumentCaptor.forClass(SearchMarketingSyncRunDO.class);
        verify(fixture.syncRunMapper).insert(captor.capture());
        verify(fixture.syncRunMapper).updateById(captor.getValue());
        assertThat(captor.getValue().getRetryable()).isZero();
    }

    @Test
    void syncDoesNotCrossTenant() {
        Fixture fixture = fixture();
        SearchMarketingSourceDO source = source();
        source.setTenantId(99L);
        when(fixture.sourceMapper.selectById(10L)).thenReturn(source);

        assertThatThrownBy(() -> fixture.service.runManual(7L, 10L, "PERFORMANCE",
                LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 2), null, "operator-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("source");

        verify(fixture.syncRunMapper, never()).insert(any(SearchMarketingSyncRunDO.class));
        verify(fixture.readClient, never()).sync(any(), any());
    }

    @Test
    void syncEvidenceRedactsSecretShapedKeys() {
        Fixture fixture = fixture();
        when(fixture.readClient.sync(any(), any())).thenReturn(SearchMarketingProviderSyncResult.success(
                "provider-request-3",
                List.of(),
                List.of(),
                Map.of(
                        "access_token", "raw-token",
                        "nested", Map.of("client_secret", "raw-secret", "status", "OK"))));

        SearchMarketingSyncRunView view = fixture.service.runManual(7L, 10L, "PROVIDER_STATE",
                LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 2), null, "operator-1");

        assertThat(view.evidence()).containsEntry("access_token", ProviderWriteEvidenceSanitizer.REDACTED);
        Map<?, ?> nested = (Map<?, ?>) view.evidence().get("nested");
        assertThat(nested.get("client_secret")).isEqualTo(ProviderWriteEvidenceSanitizer.REDACTED);
        assertThat(nested.get("status")).isEqualTo("OK");
        ArgumentCaptor<SearchMarketingSyncRunDO> captor = ArgumentCaptor.forClass(SearchMarketingSyncRunDO.class);
        verify(fixture.syncRunMapper).insert(captor.capture());
        verify(fixture.syncRunMapper).updateById(captor.getValue());
        assertThat(jsonMap(captor.getValue().getEvidenceJson()).toString())
                .doesNotContain("raw-token")
                .doesNotContain("raw-secret");
    }

    @Test
    void runDueScansEnabledStaleSourcesWithConfiguredRunTypesAndLimit() {
        Fixture fixture = fixture();
        SearchMarketingSourceDO second = source();
        second.setId(11L);
        second.setSourceKey("ads-secondary");
        when(fixture.sourceMapper.selectList(any())).thenReturn(List.of(source(), second));
        when(fixture.syncRunMapper.selectOne(any())).thenReturn(null, null);
        when(fixture.readClient.sync(any(), any())).thenReturn(SearchMarketingProviderSyncResult.success(
                "provider-request-due",
                List.of(),
                List.of(),
                Map.of("mode", "due")));

        List<SearchMarketingSyncRunView> runs = fixture.service.runDue(7L, 1, "scheduler");

        assertThat(runs).hasSize(1);
        assertThat(runs.getFirst().sourceId()).isEqualTo(10L);
        assertThat(runs.getFirst().runType()).isEqualTo("PERFORMANCE");
        ArgumentCaptor<SearchMarketingSyncRunDO> captor = ArgumentCaptor.forClass(SearchMarketingSyncRunDO.class);
        verify(fixture.syncRunMapper).insert(captor.capture());
        assertThat(captor.getValue().getCreatedBy()).isEqualTo("scheduler");
    }

    private Fixture fixture() {
        SearchMarketingSourceMapper sourceMapper = mock(SearchMarketingSourceMapper.class);
        SearchMarketingKeywordMapper keywordMapper = mock(SearchMarketingKeywordMapper.class);
        SearchMarketingSnapshotMapper snapshotMapper = mock(SearchMarketingSnapshotMapper.class);
        SearchMarketingSyncRunMapper syncRunMapper = mock(SearchMarketingSyncRunMapper.class);
        SearchMarketingUrlInspectionMapper urlInspectionMapper = mock(SearchMarketingUrlInspectionMapper.class);
        SearchMarketingProviderReadClient readClient = mock(SearchMarketingProviderReadClient.class);
        SearchMarketingCredentialResolver credentialResolver = mock(SearchMarketingCredentialResolver.class);
        when(sourceMapper.selectById(10L)).thenReturn(source());
        when(keywordMapper.selectById(20L)).thenReturn(keyword());
        when(readClient.supports(any(), any())).thenReturn(true);
        when(credentialResolver.resolve(eq(7L), eq("GOOGLE_ADS"), eq("google-main")))
                .thenReturn(SearchMarketingCredentialRef.sandbox());
        doAnswer(invocation -> {
            invocation.<SearchMarketingSyncRunDO>getArgument(0).setId(90L);
            return 1;
        }).when(syncRunMapper).insert(any(SearchMarketingSyncRunDO.class));
        doAnswer(invocation -> {
            invocation.<SearchMarketingKeywordDO>getArgument(0).setId(20L);
            return 1;
        }).when(keywordMapper).insert(any(SearchMarketingKeywordDO.class));
        doAnswer(invocation -> {
            invocation.<SearchMarketingSnapshotDO>getArgument(0).setId(30L);
            return 1;
        }).when(snapshotMapper).insert(any(SearchMarketingSnapshotDO.class));
        doAnswer(invocation -> {
            invocation.<SearchMarketingUrlInspectionDO>getArgument(0).setId(40L);
            return 1;
        }).when(urlInspectionMapper).insert(any(SearchMarketingUrlInspectionDO.class));
        SearchMarketingService searchMarketingService = new SearchMarketingService(
                sourceMapper,
                keywordMapper,
                snapshotMapper,
                mock(SearchMarketingOpportunityMapper.class),
                OBJECT_MAPPER,
                CLOCK);
        SearchMarketingSyncRunService service = new SearchMarketingSyncRunService(
                sourceMapper,
                syncRunMapper,
                urlInspectionMapper,
                searchMarketingService,
                credentialResolver,
                new SearchMarketingProviderReadGateway(List.of(readClient)),
                OBJECT_MAPPER,
                CLOCK);
        return new Fixture(sourceMapper, keywordMapper, snapshotMapper, syncRunMapper, urlInspectionMapper,
                readClient, credentialResolver, service);
    }

    private static SearchMarketingPerformanceRow performanceRow() {
        return new SearchMarketingPerformanceRow(
                "Running Shoes",
                "EXACT",
                "https://example.com/shoes",
                LocalDate.of(2026, 6, 2),
                "DESKTOP",
                "US",
                "brand",
                1000L,
                50L,
                new BigDecimal("100.0000"),
                5L,
                new BigDecimal("250.0000"),
                new BigDecimal("2.5000"),
                Map.of("providerRowId", "row-1"));
    }

    private static SearchMarketingUrlInspectionRow urlInspectionRow() {
        return new SearchMarketingUrlInspectionRow(
                "https://example.com/shoes",
                LocalDate.of(2026, 6, 2),
                "INDEXED",
                "CRAWLED",
                "https://example.com/shoes",
                "PRESENT",
                "PASS",
                LocalDateTime.of(2026, 6, 2, 12, 0),
                Map.of("coverage", "Submitted and indexed"));
    }

    private static SearchMarketingSourceDO source() {
        SearchMarketingSourceDO row = new SearchMarketingSourceDO();
        row.setId(10L);
        row.setTenantId(7L);
        row.setProvider("GOOGLE_ADS");
        row.setSourceKey("ads-main");
        row.setDisplayName("Google Ads Main");
        row.setChannel("SEM");
        row.setExternalAccountId("123-456");
        row.setCurrency("USD");
        row.setTimezone("UTC");
        row.setEnabled(1);
        row.setMetadataJson("{\"credentialKey\":\"google-main\"}");
        return row;
    }

    private static SearchMarketingKeywordDO keyword() {
        SearchMarketingKeywordDO row = new SearchMarketingKeywordDO();
        row.setId(20L);
        row.setTenantId(7L);
        row.setChannel("SEM");
        row.setKeywordText("Running Shoes");
        row.setKeywordKey("running shoes");
        row.setMatchType("EXACT");
        row.setLandingPageUrl("https://example.com/shoes");
        row.setLandingPageUrlHash("existing-hash");
        row.setStatus("ACTIVE");
        return row;
    }

    private static SearchMarketingSyncRunDO terminalRun() {
        SearchMarketingSyncRunDO row = new SearchMarketingSyncRunDO();
        row.setId(99L);
        row.setTenantId(7L);
        row.setSourceId(10L);
        row.setRunType("PERFORMANCE");
        row.setProvider("GOOGLE_ADS");
        row.setChannel("SEM");
        row.setIdempotencyKey("existing");
        row.setWindowStart(LocalDate.of(2026, 6, 1));
        row.setWindowEnd(LocalDate.of(2026, 6, 2));
        row.setCursorValue("cursor-1");
        row.setStatus("SUCCEEDED");
        row.setRetryable(0);
        row.setRequestedCount(1L);
        row.setSuccessCount(1L);
        row.setFailedCount(0L);
        row.setProviderRequestId("provider-request-existing");
        row.setEvidenceJson("{\"status\":\"cached\"}");
        row.setCreatedBy("operator-1");
        row.setStartedAt(LocalDateTime.of(2026, 6, 6, 0, 0));
        row.setFinishedAt(LocalDateTime.of(2026, 6, 6, 0, 0));
        row.setUpdatedAt(LocalDateTime.of(2026, 6, 6, 0, 0));
        return row;
    }

    private static Map<String, Object> jsonMap(String json) {
        try {
            return OBJECT_MAPPER.readValue(json, OBJECT_MAP);
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    private record Fixture(SearchMarketingSourceMapper sourceMapper,
                           SearchMarketingKeywordMapper keywordMapper,
                           SearchMarketingSnapshotMapper snapshotMapper,
                           SearchMarketingSyncRunMapper syncRunMapper,
                           SearchMarketingUrlInspectionMapper urlInspectionMapper,
                           SearchMarketingProviderReadClient readClient,
                           SearchMarketingCredentialResolver credentialResolver,
                           SearchMarketingSyncRunService service) {
    }
}
