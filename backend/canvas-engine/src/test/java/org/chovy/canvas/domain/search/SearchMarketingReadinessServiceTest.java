package org.chovy.canvas.domain.search;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.dal.dataobject.MarketingMonitorProviderCredentialDO;
import org.chovy.canvas.dal.dataobject.SearchMarketingImpactWindowDO;
import org.chovy.canvas.dal.dataobject.SearchMarketingMutationDO;
import org.chovy.canvas.dal.dataobject.SearchMarketingSourceDO;
import org.chovy.canvas.dal.dataobject.SearchMarketingSyncRunDO;
import org.chovy.canvas.dal.mapper.MarketingMonitorProviderCredentialMapper;
import org.chovy.canvas.dal.mapper.SearchMarketingImpactWindowMapper;
import org.chovy.canvas.dal.mapper.SearchMarketingMutationMapper;
import org.chovy.canvas.dal.mapper.SearchMarketingSourceMapper;
import org.chovy.canvas.dal.mapper.SearchMarketingSyncRunMapper;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SearchMarketingReadinessServiceTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-06-20T00:00:00Z"), ZoneId.of("UTC"));

    @Test
    void liveWhenEnabledSourcesHaveActiveCredentialsFreshSyncsAdaptersAndClosedLoops() {
        Fixtures fixtures = fixtures();
        when(fixtures.sourceMapper.selectList(any())).thenReturn(List.of(source("SEM")));
        when(fixtures.credentialMapper.selectList(any())).thenReturn(List.of(credential("ACTIVE",
                LocalDateTime.of(2026, 7, 1, 0, 0))));
        when(fixtures.syncRunMapper.selectList(any())).thenReturn(List.of(successfulSync(2)));
        when(fixtures.mutationMapper.selectList(any())).thenReturn(List.of());
        when(fixtures.impactWindowMapper.selectList(any())).thenReturn(List.of());
        when(fixtures.writeGateway.supportsLiveApply("GOOGLE_ADS")).thenReturn(true);

        SearchMarketingReadinessView readiness = fixtures.service.readiness(7L);

        assertThat(readiness.status()).isEqualTo("LIVE");
        assertThat(readiness.blockers()).isEmpty();
        assertThat(readiness.evidence()).containsEntry("enabledSourceCount", 1L);
    }

    @Test
    void expiredCredentialBlocksReadiness() {
        Fixtures fixtures = fixtures();
        when(fixtures.sourceMapper.selectList(any())).thenReturn(List.of(source("SEM")));
        when(fixtures.credentialMapper.selectList(any())).thenReturn(List.of(credential("ACTIVE",
                LocalDateTime.of(2026, 6, 1, 0, 0))));
        when(fixtures.syncRunMapper.selectList(any())).thenReturn(List.of(successfulSync(2)));
        when(fixtures.mutationMapper.selectList(any())).thenReturn(List.of());
        when(fixtures.impactWindowMapper.selectList(any())).thenReturn(List.of());
        when(fixtures.writeGateway.supportsLiveApply("GOOGLE_ADS")).thenReturn(true);

        SearchMarketingReadinessView readiness = fixtures.service.readiness(7L);

        assertThat(readiness.status()).isEqualTo("BLOCKED");
        assertThat(readiness.blockers()).anySatisfy(blocker ->
                assertThat(blocker).contains("credential google-main is expired"));
    }

    @Test
    void staleSuccessfulSyncBlocksReadiness() {
        Fixtures fixtures = fixtures();
        when(fixtures.sourceMapper.selectList(any())).thenReturn(List.of(source("SEM")));
        when(fixtures.credentialMapper.selectList(any())).thenReturn(List.of(credential("ACTIVE",
                LocalDateTime.of(2026, 7, 1, 0, 0))));
        when(fixtures.syncRunMapper.selectList(any())).thenReturn(List.of(successfulSync(30)));
        when(fixtures.mutationMapper.selectList(any())).thenReturn(List.of());
        when(fixtures.impactWindowMapper.selectList(any())).thenReturn(List.of());
        when(fixtures.writeGateway.supportsLiveApply("GOOGLE_ADS")).thenReturn(true);

        SearchMarketingReadinessView readiness = fixtures.service.readiness(7L);

        assertThat(readiness.status()).isEqualTo("BLOCKED");
        assertThat(readiness.blockers()).anySatisfy(blocker ->
                assertThat(blocker).contains("latest successful PERFORMANCE sync is stale"));
    }

    @Test
    void authClassFailedSyncBlocksReadiness() {
        Fixtures fixtures = fixtures();
        when(fixtures.sourceMapper.selectList(any())).thenReturn(List.of(source("SEM")));
        when(fixtures.credentialMapper.selectList(any())).thenReturn(List.of(credential("ACTIVE",
                LocalDateTime.of(2026, 7, 1, 0, 0))));
        when(fixtures.syncRunMapper.selectList(any())).thenReturn(List.of(successfulSync(2), failedSync("AUTH_EXPIRED")));
        when(fixtures.mutationMapper.selectList(any())).thenReturn(List.of());
        when(fixtures.impactWindowMapper.selectList(any())).thenReturn(List.of());
        when(fixtures.writeGateway.supportsLiveApply("GOOGLE_ADS")).thenReturn(true);

        SearchMarketingReadinessView readiness = fixtures.service.readiness(7L);

        assertThat(readiness.status()).isEqualTo("BLOCKED");
        assertThat(readiness.blockers()).anySatisfy(blocker ->
                assertThat(blocker).contains("blocking sync failure AUTH_EXPIRED"));
    }

    @Test
    void unreconciledWritePastSlaBlocksReadiness() {
        Fixtures fixtures = fixtures();
        when(fixtures.sourceMapper.selectList(any())).thenReturn(List.of(source("SEM")));
        when(fixtures.credentialMapper.selectList(any())).thenReturn(List.of(credential("ACTIVE",
                LocalDateTime.of(2026, 7, 1, 0, 0))));
        when(fixtures.syncRunMapper.selectList(any())).thenReturn(List.of(successfulSync(2)));
        when(fixtures.mutationMapper.selectList(any())).thenReturn(List.of(appliedMutation(30)));
        when(fixtures.impactWindowMapper.selectList(any())).thenReturn(List.of());
        when(fixtures.writeGateway.supportsLiveApply("GOOGLE_ADS")).thenReturn(true);

        SearchMarketingReadinessView readiness = fixtures.service.readiness(7L);

        assertThat(readiness.status()).isEqualTo("BLOCKED");
        assertThat(readiness.blockers()).anySatisfy(blocker ->
                assertThat(blocker).contains("unreconciled live mutation 50 exceeded reconciliation SLA"));
    }

    @Test
    void dueImpactWindowBlocksReadinessUntilEvaluated() {
        Fixtures fixtures = fixtures();
        when(fixtures.sourceMapper.selectList(any())).thenReturn(List.of(source("SEM")));
        when(fixtures.credentialMapper.selectList(any())).thenReturn(List.of(credential("ACTIVE",
                LocalDateTime.of(2026, 7, 1, 0, 0))));
        when(fixtures.syncRunMapper.selectList(any())).thenReturn(List.of(successfulSync(2)));
        when(fixtures.mutationMapper.selectList(any())).thenReturn(List.of());
        when(fixtures.impactWindowMapper.selectList(any())).thenReturn(List.of(dueImpactWindow()));
        when(fixtures.writeGateway.supportsLiveApply("GOOGLE_ADS")).thenReturn(true);

        SearchMarketingReadinessView readiness = fixtures.service.readiness(7L);

        assertThat(readiness.status()).isEqualTo("BLOCKED");
        assertThat(readiness.blockers()).anySatisfy(blocker ->
                assertThat(blocker).contains("impact window 70 is due but not evaluated"));
    }

    @Test
    void enabledSemProviderWithoutLiveAdapterBlocksReadiness() {
        Fixtures fixtures = fixtures();
        when(fixtures.sourceMapper.selectList(any())).thenReturn(List.of(source("SEM")));
        when(fixtures.credentialMapper.selectList(any())).thenReturn(List.of(credential("ACTIVE",
                LocalDateTime.of(2026, 7, 1, 0, 0))));
        when(fixtures.syncRunMapper.selectList(any())).thenReturn(List.of(successfulSync(2)));
        when(fixtures.mutationMapper.selectList(any())).thenReturn(List.of());
        when(fixtures.impactWindowMapper.selectList(any())).thenReturn(List.of());
        when(fixtures.writeGateway.supportsLiveApply("GOOGLE_ADS")).thenReturn(false);

        SearchMarketingReadinessView readiness = fixtures.service.readiness(7L);

        assertThat(readiness.status()).isEqualTo("BLOCKED");
        assertThat(readiness.blockers()).anySatisfy(blocker ->
                assertThat(blocker).contains("live write adapter is unavailable for GOOGLE_ADS"));
    }

    private Fixtures fixtures() {
        SearchMarketingSourceMapper sourceMapper = mock(SearchMarketingSourceMapper.class);
        MarketingMonitorProviderCredentialMapper credentialMapper =
                mock(MarketingMonitorProviderCredentialMapper.class);
        SearchMarketingSyncRunMapper syncRunMapper = mock(SearchMarketingSyncRunMapper.class);
        SearchMarketingMutationMapper mutationMapper = mock(SearchMarketingMutationMapper.class);
        SearchMarketingImpactWindowMapper impactWindowMapper = mock(SearchMarketingImpactWindowMapper.class);
        SearchMarketingProviderWriteGateway writeGateway = mock(SearchMarketingProviderWriteGateway.class);
        SearchMarketingReadinessService service = new SearchMarketingReadinessService(
                sourceMapper,
                credentialMapper,
                syncRunMapper,
                mutationMapper,
                impactWindowMapper,
                writeGateway,
                new ObjectMapper(),
                CLOCK);
        return new Fixtures(sourceMapper, credentialMapper, syncRunMapper, mutationMapper, impactWindowMapper,
                writeGateway, service);
    }

    private SearchMarketingSourceDO source(String channel) {
        SearchMarketingSourceDO row = new SearchMarketingSourceDO();
        row.setId(10L);
        row.setTenantId(7L);
        row.setProvider("GOOGLE_ADS");
        row.setSourceKey("google-main");
        row.setChannel(channel);
        row.setEnabled(1);
        row.setMetadataJson("{\"credentialKey\":\"google-main\",\"freshnessHours\":24,\"reconciliationSlaHours\":24}");
        return row;
    }

    private MarketingMonitorProviderCredentialDO credential(String status, LocalDateTime expiresAt) {
        MarketingMonitorProviderCredentialDO row = new MarketingMonitorProviderCredentialDO();
        row.setId(80L);
        row.setTenantId(7L);
        row.setCredentialKey("google-main");
        row.setProviderType("GOOGLE_ADS");
        row.setStatus(status);
        row.setExpiresAt(expiresAt);
        return row;
    }

    private SearchMarketingSyncRunDO successfulSync(int hoursAgo) {
        SearchMarketingSyncRunDO row = new SearchMarketingSyncRunDO();
        row.setId(90L + hoursAgo);
        row.setTenantId(7L);
        row.setSourceId(10L);
        row.setRunType("PERFORMANCE");
        row.setStatus("SUCCEEDED");
        row.setFinishedAt(LocalDateTime.of(2026, 6, 20, 0, 0).minusHours(hoursAgo));
        return row;
    }

    private SearchMarketingSyncRunDO failedSync(String errorCode) {
        SearchMarketingSyncRunDO row = new SearchMarketingSyncRunDO();
        row.setId(91L);
        row.setTenantId(7L);
        row.setSourceId(10L);
        row.setRunType("PERFORMANCE");
        row.setStatus("FAILED");
        row.setErrorCode(errorCode);
        row.setFinishedAt(LocalDateTime.of(2026, 6, 20, 0, 0).minusHours(1));
        return row;
    }

    private SearchMarketingMutationDO appliedMutation(int hoursAgo) {
        SearchMarketingMutationDO row = new SearchMarketingMutationDO();
        row.setId(50L);
        row.setTenantId(7L);
        row.setSourceId(10L);
        row.setStatus("APPLIED");
        row.setExecutedAt(LocalDateTime.of(2026, 6, 20, 0, 0).minusHours(hoursAgo));
        return row;
    }

    private SearchMarketingImpactWindowDO dueImpactWindow() {
        SearchMarketingImpactWindowDO row = new SearchMarketingImpactWindowDO();
        row.setId(70L);
        row.setTenantId(7L);
        row.setStatus("SCHEDULED");
        row.setDueAt(LocalDateTime.of(2026, 6, 19, 0, 0));
        return row;
    }

    private record Fixtures(
            SearchMarketingSourceMapper sourceMapper,
            MarketingMonitorProviderCredentialMapper credentialMapper,
            SearchMarketingSyncRunMapper syncRunMapper,
            SearchMarketingMutationMapper mutationMapper,
            SearchMarketingImpactWindowMapper impactWindowMapper,
            SearchMarketingProviderWriteGateway writeGateway,
            SearchMarketingReadinessService service) {
    }
}
