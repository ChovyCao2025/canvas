package org.chovy.canvas.domain.search;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.dal.dataobject.SearchMarketingMutationDO;
import org.chovy.canvas.dal.dataobject.SearchMarketingProviderChangeDO;
import org.chovy.canvas.dal.mapper.SearchMarketingMutationMapper;
import org.chovy.canvas.dal.mapper.SearchMarketingProviderChangeMapper;
import org.chovy.canvas.domain.providerwrite.ProviderWriteEvidenceSanitizer;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SearchMarketingReconciliationServiceTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-06-06T00:00:00Z"), ZoneId.of("UTC"));

    @Test
    void confirmedProviderOperationInsertsChangeAndMarksMutationReconciled() {
        SearchMarketingMutationMapper mutationMapper = mock(SearchMarketingMutationMapper.class);
        SearchMarketingProviderChangeMapper providerChangeMapper = mock(SearchMarketingProviderChangeMapper.class);
        SearchMarketingMutationDO mutation = mutation("APPLIED");
        mutation.setProviderResponseJson("""
                {"providerOperationId":"google-operation-1","access_token":"raw-token"}
                """);
        when(mutationMapper.selectById(50L)).thenReturn(mutation);
        doAnswer(invocation -> {
            invocation.<SearchMarketingProviderChangeDO>getArgument(0).setId(90L);
            return 1;
        }).when(providerChangeMapper).insert(any(SearchMarketingProviderChangeDO.class));
        SearchMarketingReconciliationService service = service(mutationMapper, providerChangeMapper);

        SearchMarketingReconciliationView view = service.reconcile(7L, 50L, "operator-1");

        assertThat(view.status()).isEqualTo("RECONCILED");
        assertThat(view.providerChangeId()).isEqualTo(90L);
        assertThat(view.providerOperationId()).isEqualTo("google-operation-1");
        ArgumentCaptor<SearchMarketingProviderChangeDO> changeCaptor =
                ArgumentCaptor.forClass(SearchMarketingProviderChangeDO.class);
        verify(providerChangeMapper).insert(changeCaptor.capture());
        assertThat(changeCaptor.getValue().getReconciliationStatus()).isEqualTo("CONFIRMED");
        assertThat(changeCaptor.getValue().getEvidenceJson())
                .contains("google-operation-1")
                .doesNotContain("raw-token");
        verify(mutationMapper).updateById(mutation);
        assertThat(mutation.getStatus()).isEqualTo("RECONCILED");
    }

    @Test
    void missingProviderConfirmationMarksMutationReconcileFailed() {
        SearchMarketingMutationMapper mutationMapper = mock(SearchMarketingMutationMapper.class);
        SearchMarketingProviderChangeMapper providerChangeMapper = mock(SearchMarketingProviderChangeMapper.class);
        SearchMarketingMutationDO mutation = mutation("APPLIED");
        mutation.setProviderResponseJson("{\"status\":\"accepted\"}");
        when(mutationMapper.selectById(50L)).thenReturn(mutation);
        doAnswer(invocation -> {
            invocation.<SearchMarketingProviderChangeDO>getArgument(0).setId(91L);
            return 1;
        }).when(providerChangeMapper).insert(any(SearchMarketingProviderChangeDO.class));
        SearchMarketingReconciliationService service = service(mutationMapper, providerChangeMapper);

        SearchMarketingReconciliationView view = service.reconcile(7L, 50L, "operator-1");

        assertThat(view.status()).isEqualTo("RECONCILE_FAILED");
        assertThat(mutation.getStatus()).isEqualTo("RECONCILE_FAILED");
        assertThat(mutation.getErrorCode()).isEqualTo("SEARCH_RECONCILIATION_NOT_CONFIRMED");
    }

    private SearchMarketingReconciliationService service(SearchMarketingMutationMapper mutationMapper,
                                                        SearchMarketingProviderChangeMapper providerChangeMapper) {
        return new SearchMarketingReconciliationService(
                mutationMapper,
                providerChangeMapper,
                new ObjectMapper(),
                CLOCK);
    }

    private SearchMarketingMutationDO mutation(String status) {
        SearchMarketingMutationDO row = new SearchMarketingMutationDO();
        row.setId(50L);
        row.setTenantId(7L);
        row.setSourceId(10L);
        row.setOpportunityId(30L);
        row.setKeywordId(20L);
        row.setProvider("GOOGLE_ADS");
        row.setChannel("SEM");
        row.setMutationKey("bid-raise-1");
        row.setMutationType("UPDATE_KEYWORD_BID");
        row.setEntityType("KEYWORD");
        row.setExternalEntityId("customers/1/adGroupCriteria/2~3");
        row.setIdempotencyKey("idem-1");
        row.setStatus(status);
        row.setPayloadJson("{\"bidMicros\":1500000}");
        row.setExecutedBy("operator-1");
        row.setExecutedAt(LocalDateTime.of(2026, 6, 6, 0, 0));
        row.setUpdatedAt(LocalDateTime.of(2026, 6, 6, 0, 0));
        return row;
    }
}
