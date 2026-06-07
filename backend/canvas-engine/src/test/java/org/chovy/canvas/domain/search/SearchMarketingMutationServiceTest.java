package org.chovy.canvas.domain.search;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.dal.dataobject.SearchMarketingKeywordDO;
import org.chovy.canvas.dal.dataobject.SearchMarketingMutationDO;
import org.chovy.canvas.dal.dataobject.SearchMarketingOpportunityDO;
import org.chovy.canvas.dal.dataobject.SearchMarketingSourceDO;
import org.chovy.canvas.dal.mapper.SearchMarketingKeywordMapper;
import org.chovy.canvas.dal.mapper.SearchMarketingMutationMapper;
import org.chovy.canvas.dal.mapper.SearchMarketingOpportunityMapper;
import org.chovy.canvas.dal.mapper.SearchMarketingSourceMapper;
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
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SearchMarketingMutationServiceTest {

    @Test
    void proposesApprovedSearchMutationWithTenantOwnedSourceKeywordAndOpportunity() {
        SearchMarketingSourceMapper sourceMapper = mock(SearchMarketingSourceMapper.class);
        SearchMarketingKeywordMapper keywordMapper = mock(SearchMarketingKeywordMapper.class);
        SearchMarketingOpportunityMapper opportunityMapper = mock(SearchMarketingOpportunityMapper.class);
        SearchMarketingMutationMapper mutationMapper = mock(SearchMarketingMutationMapper.class);
        when(sourceMapper.selectById(10L)).thenReturn(source());
        when(keywordMapper.selectById(20L)).thenReturn(keyword());
        when(opportunityMapper.selectById(30L)).thenReturn(opportunity());
        doAnswer(invocation -> {
            invocation.<SearchMarketingMutationDO>getArgument(0).setId(50L);
            return 1;
        }).when(mutationMapper).insert(any(SearchMarketingMutationDO.class));
        SearchMarketingMutationService service = service(sourceMapper, keywordMapper, opportunityMapper,
                mutationMapper, SearchMarketingProviderWriteGateway.unsupported());

        SearchMarketingMutationView view = service.propose(7L, mutationCommand(), "operator-1");

        assertThat(view.id()).isEqualTo(50L);
        assertThat(view.provider()).isEqualTo("GOOGLE_ADS");
        assertThat(view.channel()).isEqualTo("SEM");
        assertThat(view.status()).isEqualTo("DRAFT");
        assertThat(view.approvalStatus()).isEqualTo("PENDING");
        assertThat(view.requestHash()).hasSize(64);
        assertThat(view.payload()).containsEntry("bidMicros", 1500000);
        ArgumentCaptor<SearchMarketingMutationDO> captor =
                ArgumentCaptor.forClass(SearchMarketingMutationDO.class);
        verify(mutationMapper).insert(captor.capture());
        assertThat(captor.getValue().getTenantId()).isEqualTo(7L);
        assertThat(captor.getValue().getSourceId()).isEqualTo(10L);
        assertThat(captor.getValue().getKeywordId()).isEqualTo(20L);
        assertThat(captor.getValue().getOpportunityId()).isEqualTo(30L);
        assertThat(captor.getValue().getMutationType()).isEqualTo("UPDATE_KEYWORD_BID");
        assertThat(captor.getValue().getCreatedBy()).isEqualTo("operator-1");
    }

    @Test
    void rejectsProposalWhenKeywordBelongsToAnotherTenant() {
        SearchMarketingSourceMapper sourceMapper = mock(SearchMarketingSourceMapper.class);
        SearchMarketingKeywordMapper keywordMapper = mock(SearchMarketingKeywordMapper.class);
        SearchMarketingMutationMapper mutationMapper = mock(SearchMarketingMutationMapper.class);
        SearchMarketingKeywordDO keyword = keyword();
        keyword.setTenantId(99L);
        when(sourceMapper.selectById(10L)).thenReturn(source());
        when(keywordMapper.selectById(20L)).thenReturn(keyword);
        SearchMarketingMutationService service = service(sourceMapper, keywordMapper,
                mock(SearchMarketingOpportunityMapper.class), mutationMapper,
                SearchMarketingProviderWriteGateway.unsupported());

        assertThatThrownBy(() -> service.propose(7L, mutationCommand(), "operator-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("keyword");

        verify(mutationMapper, never()).insert(any(SearchMarketingMutationDO.class));
    }

    @Test
    void rejectsNestedProviderSecretsInPayload() {
        SearchMarketingSourceMapper sourceMapper = mock(SearchMarketingSourceMapper.class);
        SearchMarketingKeywordMapper keywordMapper = mock(SearchMarketingKeywordMapper.class);
        SearchMarketingMutationMapper mutationMapper = mock(SearchMarketingMutationMapper.class);
        when(sourceMapper.selectById(10L)).thenReturn(source());
        when(keywordMapper.selectById(20L)).thenReturn(keyword());
        SearchMarketingMutationService service = service(sourceMapper, keywordMapper,
                mock(SearchMarketingOpportunityMapper.class), mutationMapper,
                SearchMarketingProviderWriteGateway.unsupported());

        assertThatThrownBy(() -> service.propose(7L,
                new SearchMarketingMutationCommand(10L, null, 20L, "bid-raise-secret",
                        "UPDATE_KEYWORD_BID", "KEYWORD", "customers/1/adGroupCriteria/2~3",
                        true, "idem-secret", Map.of(
                        "bidMicros", 1500000,
                        "providerAuth", Map.of("client_secret", "secret-value"))), "operator-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("provider secrets");

        verify(mutationMapper, never()).insert(any(SearchMarketingMutationDO.class));
    }

    @Test
    void keepsMutationIdempotentByMutationKeyAndRequestHash() {
        SearchMarketingSourceMapper sourceMapper = mock(SearchMarketingSourceMapper.class);
        SearchMarketingKeywordMapper keywordMapper = mock(SearchMarketingKeywordMapper.class);
        SearchMarketingOpportunityMapper opportunityMapper = mock(SearchMarketingOpportunityMapper.class);
        SearchMarketingMutationMapper mutationMapper = mock(SearchMarketingMutationMapper.class);
        when(sourceMapper.selectById(10L)).thenReturn(source());
        when(keywordMapper.selectById(20L)).thenReturn(keyword());
        when(opportunityMapper.selectById(30L)).thenReturn(opportunity());
        SearchMarketingMutationDO existing = mutation();
        when(mutationMapper.selectOne(any())).thenReturn(existing);
        SearchMarketingMutationService service = service(sourceMapper, keywordMapper, opportunityMapper,
                mutationMapper, SearchMarketingProviderWriteGateway.unsupported());

        SearchMarketingMutationView idempotent = service.propose(7L, mutationCommand(), "operator-1");

        assertThat(idempotent.id()).isEqualTo(50L);
        verify(mutationMapper, never()).insert(any(SearchMarketingMutationDO.class));
        assertThatThrownBy(() -> service.propose(7L,
                new SearchMarketingMutationCommand(10L, 30L, 20L, "bid-raise-1", "UPDATE_KEYWORD_BID",
                        "KEYWORD", "customers/1/adGroupCriteria/2~3", true, "idem-1",
                        Map.of("bidMicros", 2000000)), "operator-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("request hash");
    }

    @Test
    void enforcesApprovalAndDryRunBeforeApplyAndFailsClosedWhenLiveClientUnavailable() {
        SearchMarketingSourceMapper sourceMapper = mock(SearchMarketingSourceMapper.class);
        SearchMarketingMutationMapper mutationMapper = mock(SearchMarketingMutationMapper.class);
        SearchMarketingMutationDO row = mutation();
        row.setStatus("DRAFT");
        row.setApprovalStatus("PENDING");
        when(sourceMapper.selectById(10L)).thenReturn(source());
        when(mutationMapper.selectById(50L)).thenReturn(row);
        SearchMarketingMutationService service = service(sourceMapper, mock(SearchMarketingKeywordMapper.class),
                mock(SearchMarketingOpportunityMapper.class), mutationMapper,
                SearchMarketingProviderWriteGateway.unsupported());

        assertThatThrownBy(() -> service.execute(7L, 50L,
                new SearchMarketingMutationExecuteCommand(false, false, Map.of()), "operator-1"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("approved");

        SearchMarketingMutationView approved = service.approve(7L, 50L,
                new SearchMarketingMutationApprovalCommand("APPROVED", "ready"), "lead-1");
        assertThat(approved.status()).isEqualTo("READY");
        assertThat(approved.approvalStatus()).isEqualTo("APPROVED");

        assertThatThrownBy(() -> service.execute(7L, 50L,
                new SearchMarketingMutationExecuteCommand(false, false, Map.of()), "operator-1"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("dry run");

        SearchMarketingMutationView dryRun = service.execute(7L, 50L,
                new SearchMarketingMutationExecuteCommand(true, true, Map.of("validateOnly", true)), "operator-1");
        assertThat(dryRun.status()).isEqualTo("DRY_RUN_OK");

        SearchMarketingMutationView live = service.execute(7L, 50L,
                new SearchMarketingMutationExecuteCommand(false, true, Map.of()), "operator-1");
        assertThat(live.status()).isEqualTo("FAILED");
        assertThat(live.errorCode()).isEqualTo("PROVIDER_CLIENT_UNAVAILABLE");
    }

    @Test
    void redactsProviderRequestMetadataAndResponseEvidence() {
        SearchMarketingSourceMapper sourceMapper = mock(SearchMarketingSourceMapper.class);
        SearchMarketingMutationMapper mutationMapper = mock(SearchMarketingMutationMapper.class);
        SearchMarketingMutationDO row = mutation();
        row.setStatus("READY");
        row.setApprovalStatus("APPROVED");
        when(sourceMapper.selectById(10L)).thenReturn(source());
        when(mutationMapper.selectById(50L)).thenReturn(row);
        SearchMarketingProviderWriteClient client = new SearchMarketingProviderWriteClient() {
            @Override
            public boolean supports(SearchMarketingProviderMutationRequest request) {
                return true;
            }

            @Override
            public SearchMarketingProviderMutationResult execute(SearchMarketingProviderMutationRequest request) {
                return SearchMarketingProviderMutationResult.success("provider-validate-1", Map.of(
                        "requestId", "provider-request-1",
                        "access_token", "raw-response-token",
                        "nested", Map.of("client_secret", "raw-secret", "status", "OK")));
            }
        };
        SearchMarketingMutationService service = service(sourceMapper, mock(SearchMarketingKeywordMapper.class),
                mock(SearchMarketingOpportunityMapper.class), mutationMapper,
                new SearchMarketingProviderWriteGateway(List.of(client)));

        SearchMarketingMutationView view = service.execute(7L, 50L,
                new SearchMarketingMutationExecuteCommand(true, true, Map.of(
                        "source", "test",
                        "authorization", "Bearer raw",
                        "nested", Map.of("refresh_token", "raw-refresh", "note", "safe"))),
                "operator-1");

        Map<?, ?> metadata = (Map<?, ?>) view.dryRunResult().get("metadata");
        assertThat(metadata.get("authorization"))
                .isEqualTo(org.chovy.canvas.domain.providerwrite.ProviderWriteEvidenceSanitizer.REDACTED);
        assertThat(metadata.get("source")).isEqualTo("test");
        Map<?, ?> requestNested = (Map<?, ?>) metadata.get("nested");
        assertThat(requestNested.get("refresh_token"))
                .isEqualTo(org.chovy.canvas.domain.providerwrite.ProviderWriteEvidenceSanitizer.REDACTED);
        assertThat(requestNested.get("note")).isEqualTo("safe");
        assertThat(view.providerResult())
                .containsEntry("access_token", org.chovy.canvas.domain.providerwrite.ProviderWriteEvidenceSanitizer.REDACTED)
                .containsEntry("requestId", "provider-request-1");
        Map<?, ?> responseNested = (Map<?, ?>) view.providerResult().get("nested");
        assertThat(responseNested.get("client_secret"))
                .isEqualTo(org.chovy.canvas.domain.providerwrite.ProviderWriteEvidenceSanitizer.REDACTED);
        assertThat(responseNested.get("status")).isEqualTo("OK");
    }

    @Test
    void acceptedSemOpportunityCanCreateMutationProposal() {
        SearchMarketingSourceMapper sourceMapper = mock(SearchMarketingSourceMapper.class);
        SearchMarketingKeywordMapper keywordMapper = mock(SearchMarketingKeywordMapper.class);
        SearchMarketingOpportunityMapper opportunityMapper = mock(SearchMarketingOpportunityMapper.class);
        SearchMarketingMutationMapper mutationMapper = mock(SearchMarketingMutationMapper.class);
        SearchMarketingOpportunityDO opportunity = opportunity();
        opportunity.setStatus("ACCEPTED");
        when(sourceMapper.selectById(10L)).thenReturn(source());
        when(keywordMapper.selectById(20L)).thenReturn(keyword());
        when(opportunityMapper.selectById(30L)).thenReturn(opportunity);
        doAnswer(invocation -> {
            invocation.<SearchMarketingMutationDO>getArgument(0).setId(51L);
            return 1;
        }).when(mutationMapper).insert(any(SearchMarketingMutationDO.class));
        SearchMarketingMutationService service = service(sourceMapper, keywordMapper, opportunityMapper,
                mutationMapper, SearchMarketingProviderWriteGateway.unsupported());

        SearchMarketingMutationView view = service.proposeFromOpportunity(7L, 30L,
                new SearchMarketingOpportunityMutationCommand(
                        "bid-raise-from-opportunity",
                        "UPDATE_KEYWORD_BID",
                        "KEYWORD",
                        "customers/1/adGroupCriteria/2~3",
                        true,
                        "idem-from-opportunity",
                        Map.of("bidMicros", 1500000)), "operator-1");

        assertThat(view.id()).isEqualTo(51L);
        assertThat(view.opportunityId()).isEqualTo(30L);
        assertThat(view.sourceId()).isEqualTo(10L);
        assertThat(view.keywordId()).isEqualTo(20L);
    }

    @Test
    void seoOnlyOpportunityCannotCreateSemProviderMutation() {
        SearchMarketingSourceMapper sourceMapper = mock(SearchMarketingSourceMapper.class);
        SearchMarketingKeywordMapper keywordMapper = mock(SearchMarketingKeywordMapper.class);
        SearchMarketingOpportunityMapper opportunityMapper = mock(SearchMarketingOpportunityMapper.class);
        SearchMarketingMutationMapper mutationMapper = mock(SearchMarketingMutationMapper.class);
        SearchMarketingSourceDO seoSource = source();
        seoSource.setChannel("SEO");
        SearchMarketingKeywordDO seoKeyword = keyword();
        seoKeyword.setChannel("SEO");
        SearchMarketingOpportunityDO seoOpportunity = opportunity();
        seoOpportunity.setChannel("SEO");
        seoOpportunity.setStatus("ACCEPTED");
        when(sourceMapper.selectById(10L)).thenReturn(seoSource);
        when(keywordMapper.selectById(20L)).thenReturn(seoKeyword);
        when(opportunityMapper.selectById(30L)).thenReturn(seoOpportunity);
        SearchMarketingMutationService service = service(sourceMapper, keywordMapper, opportunityMapper,
                mutationMapper, SearchMarketingProviderWriteGateway.unsupported());

        assertThatThrownBy(() -> service.proposeFromOpportunity(7L, 30L,
                new SearchMarketingOpportunityMutationCommand(
                        "seo-to-sem",
                        "UPDATE_KEYWORD_BID",
                        "KEYWORD",
                        "customers/1/adGroupCriteria/2~3",
                        true,
                        "idem-seo",
                        Map.of("bidMicros", 1500000)), "operator-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("SEM source");

        verify(mutationMapper, never()).insert(any(SearchMarketingMutationDO.class));
    }

    private SearchMarketingMutationService service(SearchMarketingSourceMapper sourceMapper,
                                                   SearchMarketingKeywordMapper keywordMapper,
                                                   SearchMarketingOpportunityMapper opportunityMapper,
                                                   SearchMarketingMutationMapper mutationMapper,
                                                   SearchMarketingProviderWriteGateway gateway) {
        return new SearchMarketingMutationService(
                sourceMapper,
                keywordMapper,
                opportunityMapper,
                mutationMapper,
                new ObjectMapper(),
                gateway,
                Clock.fixed(Instant.parse("2026-06-06T00:00:00Z"), ZoneId.of("UTC")));
    }

    private SearchMarketingMutationCommand mutationCommand() {
        return new SearchMarketingMutationCommand(
                10L,
                30L,
                20L,
                "bid-raise-1",
                "UPDATE_KEYWORD_BID",
                "KEYWORD",
                "customers/1/adGroupCriteria/2~3",
                true,
                "idem-1",
                Map.of("bidMicros", 1500000));
    }

    private SearchMarketingMutationDO mutation() {
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
        row.setRequestHash("0f96fdf8dca4205e90173aa9736965d85edc65bd549d0022a90258ef7cfeb239");
        row.setIdempotencyKey("idem-1");
        row.setStatus("DRAFT");
        row.setApprovalStatus("PENDING");
        row.setDryRunRequired(1);
        row.setPayloadJson("{\"bidMicros\":1500000}");
        row.setValidationJson("{}");
        row.setCreatedBy("operator-1");
        row.setCreatedAt(now());
        row.setUpdatedAt(now());
        return row;
    }

    private SearchMarketingSourceDO source() {
        SearchMarketingSourceDO row = new SearchMarketingSourceDO();
        row.setId(10L);
        row.setTenantId(7L);
        row.setProvider("GOOGLE_ADS");
        row.setSourceKey("ads-main");
        row.setDisplayName("Google Ads Main");
        row.setChannel("SEM");
        row.setExternalAccountId("123-456");
        row.setCurrency("USD");
        row.setEnabled(1);
        return row;
    }

    private SearchMarketingKeywordDO keyword() {
        SearchMarketingKeywordDO row = new SearchMarketingKeywordDO();
        row.setId(20L);
        row.setTenantId(7L);
        row.setChannel("SEM");
        row.setKeywordText("Running Shoes");
        row.setKeywordKey("running shoes");
        row.setStatus("ACTIVE");
        return row;
    }

    private SearchMarketingOpportunityDO opportunity() {
        SearchMarketingOpportunityDO row = new SearchMarketingOpportunityDO();
        row.setId(30L);
        row.setTenantId(7L);
        row.setSourceId(10L);
        row.setKeywordId(20L);
        row.setChannel("SEM");
        row.setOpportunityType("LOW_CTR");
        row.setSnapshotDate(LocalDate.of(2026, 6, 6));
        row.setStatus("OPEN");
        row.setImpactScore(new BigDecimal("5.0000"));
        return row;
    }

    private LocalDateTime now() {
        return LocalDateTime.of(2026, 6, 6, 0, 0);
    }
}
