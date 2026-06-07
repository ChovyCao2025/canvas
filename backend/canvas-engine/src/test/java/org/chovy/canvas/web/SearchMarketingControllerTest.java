package org.chovy.canvas.web;

import org.chovy.canvas.common.tenant.RoleNames;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.search.SearchMarketingKeywordCommand;
import org.chovy.canvas.domain.search.SearchMarketingKeywordView;
import org.chovy.canvas.domain.search.SearchMarketingImpactWindowQuery;
import org.chovy.canvas.domain.search.SearchMarketingImpactWindowService;
import org.chovy.canvas.domain.search.SearchMarketingImpactWindowView;
import org.chovy.canvas.domain.search.SearchMarketingMutationApprovalCommand;
import org.chovy.canvas.domain.search.SearchMarketingMutationCommand;
import org.chovy.canvas.domain.search.SearchMarketingMutationExecuteCommand;
import org.chovy.canvas.domain.search.SearchMarketingMutationQuery;
import org.chovy.canvas.domain.search.SearchMarketingMutationService;
import org.chovy.canvas.domain.search.SearchMarketingMutationView;
import org.chovy.canvas.domain.search.SearchMarketingOpportunityEvaluationCommand;
import org.chovy.canvas.domain.search.SearchMarketingOpportunityMutationCommand;
import org.chovy.canvas.domain.search.SearchMarketingOpportunityView;
import org.chovy.canvas.domain.search.SearchMarketingKeywordQuery;
import org.chovy.canvas.domain.search.SearchMarketingOpportunityQuery;
import org.chovy.canvas.domain.search.SearchMarketingOpportunityStatusCommand;
import org.chovy.canvas.domain.search.SearchMarketingProviderChangeQuery;
import org.chovy.canvas.domain.search.SearchMarketingProviderChangeView;
import org.chovy.canvas.domain.search.SearchMarketingReadinessView;
import org.chovy.canvas.domain.search.SearchMarketingReconciliationService;
import org.chovy.canvas.domain.search.SearchMarketingReconciliationView;
import org.chovy.canvas.domain.search.SearchMarketingService;
import org.chovy.canvas.domain.search.SearchMarketingSnapshotCommand;
import org.chovy.canvas.domain.search.SearchMarketingSnapshotQuery;
import org.chovy.canvas.domain.search.SearchMarketingSnapshotView;
import org.chovy.canvas.domain.search.SearchMarketingSourceCommand;
import org.chovy.canvas.domain.search.SearchMarketingSourceQuery;
import org.chovy.canvas.domain.search.SearchMarketingSourceView;
import org.chovy.canvas.domain.search.SearchMarketingSummaryQuery;
import org.chovy.canvas.domain.search.SearchMarketingSummaryView;
import org.chovy.canvas.domain.search.SearchMarketingSyncRequest;
import org.chovy.canvas.domain.search.SearchMarketingSyncRunQuery;
import org.chovy.canvas.domain.search.SearchMarketingSyncRunService;
import org.chovy.canvas.domain.search.SearchMarketingSyncRunView;
import org.chovy.canvas.domain.search.SearchMarketingUrlInspectionQuery;
import org.chovy.canvas.domain.search.SearchMarketingUrlInspectionView;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SearchMarketingControllerTest {

    @Test
    void writeEndpointsPassCurrentTenantAndOperator() {
        SearchMarketingService service = mock(SearchMarketingService.class);
        SearchMarketingMutationService mutationService = mock(SearchMarketingMutationService.class);
        SearchMarketingSourceCommand sourceCommand = sourceCommand();
        SearchMarketingKeywordCommand keywordCommand = keywordCommand();
        SearchMarketingSnapshotCommand snapshotCommand = snapshotCommand();
        SearchMarketingOpportunityEvaluationCommand evaluationCommand = evaluationCommand();
        when(service.upsertSource(7L, sourceCommand, "operator-1")).thenReturn(sourceView());
        when(service.upsertKeyword(7L, keywordCommand, "operator-1")).thenReturn(keywordView());
        when(service.recordSnapshot(7L, snapshotCommand, "operator-1")).thenReturn(snapshotView());
        when(service.evaluateOpportunities(7L, evaluationCommand, "operator-1")).thenReturn(List.of(opportunityView()));
        SearchMarketingController controller = new SearchMarketingController(service, mutationService, resolver());

        StepVerifier.create(controller.upsertSource(sourceCommand))
                .assertNext(response -> assertThat(response.getData().provider()).isEqualTo("GOOGLE_ADS"))
                .verifyComplete();
        StepVerifier.create(controller.upsertKeyword(keywordCommand))
                .assertNext(response -> assertThat(response.getData().keywordKey()).isEqualTo("running shoes"))
                .verifyComplete();
        StepVerifier.create(controller.recordSnapshot(snapshotCommand))
                .assertNext(response -> assertThat(response.getData().clickCount()).isEqualTo(40L))
                .verifyComplete();
        StepVerifier.create(controller.evaluateOpportunities(evaluationCommand))
                .assertNext(response -> assertThat(response.getData()).singleElement()
                        .satisfies(item -> assertThat(item.opportunityType()).isEqualTo("LOW_CTR")))
                .verifyComplete();

        verify(service).upsertSource(7L, sourceCommand, "operator-1");
        verify(service).upsertKeyword(7L, keywordCommand, "operator-1");
        verify(service).recordSnapshot(7L, snapshotCommand, "operator-1");
        verify(service).evaluateOpportunities(7L, evaluationCommand, "operator-1");
    }

    @Test
    void summaryEndpointPassesFilters() {
        SearchMarketingService service = mock(SearchMarketingService.class);
        SearchMarketingMutationService mutationService = mock(SearchMarketingMutationService.class);
        SearchMarketingSummaryQuery query = new SearchMarketingSummaryQuery("SEM", 10L, 20L, date(), date().plusDays(1));
        when(service.summary(7L, query)).thenReturn(summaryView());
        SearchMarketingController controller = new SearchMarketingController(service, mutationService, resolver());

        StepVerifier.create(controller.summary("SEM", 10L, 20L, date(), date().plusDays(1)))
                .assertNext(response -> assertThat(response.getData().roas()).isEqualByComparingTo("3.333333"))
                .verifyComplete();

        verify(service).summary(7L, query);
    }

    @Test
    void mutationEndpointsPassCurrentTenantAndOperator() {
        SearchMarketingService service = mock(SearchMarketingService.class);
        SearchMarketingMutationService mutationService = mock(SearchMarketingMutationService.class);
        SearchMarketingMutationCommand proposeCommand = mutationCommand();
        SearchMarketingMutationApprovalCommand approvalCommand =
                new SearchMarketingMutationApprovalCommand("APPROVED", "ready");
        SearchMarketingMutationExecuteCommand executeCommand =
                new SearchMarketingMutationExecuteCommand(true, true, Map.of("validateOnly", true));
        SearchMarketingMutationQuery query = new SearchMarketingMutationQuery(10L, "READY", "APPROVED", 20);
        when(mutationService.propose(7L, proposeCommand, "operator-1")).thenReturn(mutationView("DRAFT"));
        when(mutationService.approve(7L, 50L, approvalCommand, "operator-1")).thenReturn(mutationView("READY"));
        when(mutationService.execute(7L, 50L, executeCommand, "operator-1")).thenReturn(mutationView("DRY_RUN_OK"));
        when(mutationService.list(7L, query)).thenReturn(List.of(mutationView("READY")));
        SearchMarketingController controller = new SearchMarketingController(service, mutationService, resolver());

        StepVerifier.create(controller.proposeMutation(proposeCommand))
                .assertNext(response -> assertThat(response.getData().mutationKey()).isEqualTo("bid-raise-1"))
                .verifyComplete();
        StepVerifier.create(controller.approveMutation(50L, approvalCommand))
                .assertNext(response -> assertThat(response.getData().status()).isEqualTo("READY"))
                .verifyComplete();
        StepVerifier.create(controller.executeMutation(50L, executeCommand))
                .assertNext(response -> assertThat(response.getData().status()).isEqualTo("DRY_RUN_OK"))
                .verifyComplete();
        StepVerifier.create(controller.listMutations(10L, "READY", "APPROVED", 20))
                .assertNext(response -> assertThat(response.getData()).singleElement()
                        .satisfies(item -> assertThat(item.approvalStatus()).isEqualTo("APPROVED")))
                .verifyComplete();

        verify(mutationService).propose(7L, proposeCommand, "operator-1");
        verify(mutationService).approve(7L, 50L, approvalCommand, "operator-1");
        verify(mutationService).execute(7L, 50L, executeCommand, "operator-1");
        verify(mutationService).list(7L, query);
    }

    @Test
    void opportunityWorkflowEndpointsPassCurrentTenantAndOperator() {
        SearchMarketingService service = mock(SearchMarketingService.class);
        SearchMarketingMutationService mutationService = mock(SearchMarketingMutationService.class);
        SearchMarketingOpportunityStatusCommand statusCommand =
                new SearchMarketingOpportunityStatusCommand("ACCEPTED", "ready for bid change");
        SearchMarketingOpportunityMutationCommand mutationCommand =
                new SearchMarketingOpportunityMutationCommand(
                        "bid-raise-from-opportunity",
                        "UPDATE_KEYWORD_BID",
                        "KEYWORD",
                        "customers/1/adGroupCriteria/2~3",
                        true,
                        "idem-from-opportunity",
                        Map.of("bidMicros", 1500000));
        SearchMarketingOpportunityView accepted = new SearchMarketingOpportunityView(40L, 7L, 10L, 20L,
                "SEM", "LOW_CTR", date(), "HIGH", "ACCEPTED", "Improve CTR",
                new BigDecimal("5.0000"), Map.of("statusReason", "ready for bid change"),
                "operator-1", now(), now());
        when(service.updateOpportunityStatus(7L, 40L, statusCommand, "operator-1")).thenReturn(accepted);
        when(mutationService.proposeFromOpportunity(7L, 40L, mutationCommand, "operator-1"))
                .thenReturn(mutationView("DRAFT"));
        SearchMarketingController controller = new SearchMarketingController(service, mutationService, resolver());

        StepVerifier.create(controller.updateOpportunityStatus(40L, statusCommand))
                .assertNext(response -> assertThat(response.getData().status()).isEqualTo("ACCEPTED"))
                .verifyComplete();
        StepVerifier.create(controller.proposeOpportunityMutation(40L, mutationCommand))
                .assertNext(response -> assertThat(response.getData().mutationKey()).isEqualTo("bid-raise-1"))
                .verifyComplete();

        verify(service).updateOpportunityStatus(7L, 40L, statusCommand, "operator-1");
        verify(mutationService).proposeFromOpportunity(7L, 40L, mutationCommand, "operator-1");
    }

    @Test
    void querySyncAndReadinessEndpointsPassCurrentTenantAndOperator() {
        SearchMarketingService service = mock(SearchMarketingService.class);
        SearchMarketingMutationService mutationService = mock(SearchMarketingMutationService.class);
        SearchMarketingSyncRunService syncRunService = mock(SearchMarketingSyncRunService.class);
        SearchMarketingSyncRequest syncCommand =
                new SearchMarketingSyncRequest("PERFORMANCE", date(), date(), null);
        SearchMarketingSourceQuery sourceQuery = new SearchMarketingSourceQuery("GOOGLE_ADS", "SEM", true, 50);
        SearchMarketingKeywordQuery keywordQuery = new SearchMarketingKeywordQuery("SEM", "ACTIVE", 50);
        SearchMarketingSnapshotQuery snapshotQuery =
                new SearchMarketingSnapshotQuery("SEM", 10L, 20L, date(), date(), 50);
        SearchMarketingOpportunityQuery opportunityQuery =
                new SearchMarketingOpportunityQuery("SEM", 10L, "OPEN", null, 50);
        SearchMarketingUrlInspectionQuery urlInspectionQuery =
                new SearchMarketingUrlInspectionQuery(10L, "INDEXED", date(), date(), 50);
        SearchMarketingSyncRunQuery syncRunQuery = new SearchMarketingSyncRunQuery(10L, "PERFORMANCE", "SUCCEEDED", 50);
        when(service.listSources(7L, sourceQuery)).thenReturn(List.of(sourceView()));
        when(service.listKeywords(7L, keywordQuery)).thenReturn(List.of(keywordView()));
        when(service.listSnapshots(7L, snapshotQuery)).thenReturn(List.of(snapshotView()));
        when(service.listOpportunities(7L, opportunityQuery)).thenReturn(List.of(opportunityView()));
        when(syncRunService.listUrlInspections(7L, urlInspectionQuery))
                .thenReturn(List.of(urlInspectionView()));
        when(syncRunService.list(7L, syncRunQuery))
                .thenReturn(List.of(syncRunView()));
        when(syncRunService.runManual(7L, 10L, "PERFORMANCE", date(), date(), null, "operator-1"))
                .thenReturn(syncRunView());
        when(syncRunService.runDue(7L, 10, "operator-1")).thenReturn(List.of(syncRunView()));
        when(syncRunService.readiness(7L)).thenReturn(readinessView());
        SearchMarketingController controller = new SearchMarketingController(
                service, mutationService, syncRunService, resolver());

        StepVerifier.create(controller.listSources("GOOGLE_ADS", "SEM", true, 50))
                .assertNext(response -> assertThat(response.getData()).singleElement()
                        .satisfies(item -> assertThat(item.provider()).isEqualTo("GOOGLE_ADS")))
                .verifyComplete();
        StepVerifier.create(controller.listKeywords("SEM", "ACTIVE", 50))
                .assertNext(response -> assertThat(response.getData()).singleElement()
                        .satisfies(item -> assertThat(item.keywordKey()).isEqualTo("running shoes")))
                .verifyComplete();
        StepVerifier.create(controller.listSnapshots("SEM", 10L, 20L, date(), date(), 50))
                .assertNext(response -> assertThat(response.getData()).singleElement()
                        .satisfies(item -> assertThat(item.clickCount()).isEqualTo(40L)))
                .verifyComplete();
        StepVerifier.create(controller.listOpportunities("SEM", 10L, "OPEN", null, 50))
                .assertNext(response -> assertThat(response.getData()).singleElement()
                        .satisfies(item -> assertThat(item.status()).isEqualTo("OPEN")))
                .verifyComplete();
        StepVerifier.create(controller.listUrlInspections(10L, "INDEXED", date(), date(), 50))
                .assertNext(response -> assertThat(response.getData()).singleElement()
                        .satisfies(item -> assertThat(item.indexedState()).isEqualTo("INDEXED")))
                .verifyComplete();
        StepVerifier.create(controller.listSyncRuns(10L, "PERFORMANCE", "SUCCEEDED", 50))
                .assertNext(response -> assertThat(response.getData()).singleElement()
                        .satisfies(item -> assertThat(item.status()).isEqualTo("SUCCEEDED")))
                .verifyComplete();
        StepVerifier.create(controller.syncSource(10L, syncCommand))
                .assertNext(response -> assertThat(response.getData().runType()).isEqualTo("PERFORMANCE"))
                .verifyComplete();
        StepVerifier.create(controller.syncDue(new org.chovy.canvas.domain.search.SearchMarketingSyncDueRequest(10)))
                .assertNext(response -> assertThat(response.getData()).singleElement()
                        .satisfies(item -> assertThat(item.provider()).isEqualTo("GOOGLE_ADS")))
                .verifyComplete();
        StepVerifier.create(controller.readiness())
                .assertNext(response -> assertThat(response.getData().status()).isEqualTo("DEGRADED"))
                .verifyComplete();

        verify(service).listSources(7L, sourceQuery);
        verify(service).listKeywords(7L, keywordQuery);
        verify(service).listSnapshots(7L, snapshotQuery);
        verify(service).listOpportunities(7L, opportunityQuery);
        verify(syncRunService).listUrlInspections(7L, urlInspectionQuery);
        verify(syncRunService).list(7L, syncRunQuery);
        verify(syncRunService).runManual(7L, 10L, "PERFORMANCE", date(), date(), null, "operator-1");
        verify(syncRunService).runDue(7L, 10, "operator-1");
        verify(syncRunService).readiness(7L);
    }

    @Test
    void reconciliationAndImpactEndpointsPassCurrentTenantAndOperator() {
        SearchMarketingService service = mock(SearchMarketingService.class);
        SearchMarketingMutationService mutationService = mock(SearchMarketingMutationService.class);
        SearchMarketingReconciliationService reconciliationService = mock(SearchMarketingReconciliationService.class);
        SearchMarketingImpactWindowService impactWindowService = mock(SearchMarketingImpactWindowService.class);
        SearchMarketingProviderChangeQuery providerChangeQuery =
                new SearchMarketingProviderChangeQuery(10L, 50L, "GOOGLE_ADS", "CONFIRMED", 25);
        SearchMarketingImpactWindowQuery impactWindowQuery =
                new SearchMarketingImpactWindowQuery(40L, 50L, 10L, "SCHEDULED", null, 25);
        when(reconciliationService.list(7L, providerChangeQuery)).thenReturn(List.of(providerChangeView()));
        when(reconciliationService.reconcile(7L, 50L, "operator-1")).thenReturn(reconciliationView());
        when(impactWindowService.list(7L, impactWindowQuery)).thenReturn(List.of(impactWindowView()));
        when(impactWindowService.evaluateDue(7L, 10, "operator-1")).thenReturn(List.of(impactWindowView()));
        SearchMarketingController controller = new SearchMarketingController(
                service, mutationService, null, null, reconciliationService, impactWindowService, resolver());

        StepVerifier.create(controller.listProviderChanges(10L, 50L, "GOOGLE_ADS", "CONFIRMED", 25))
                .assertNext(response -> assertThat(response.getData()).singleElement()
                        .satisfies(item -> assertThat(item.reconciliationStatus()).isEqualTo("CONFIRMED")))
                .verifyComplete();
        StepVerifier.create(controller.reconcileMutation(50L))
                .assertNext(response -> assertThat(response.getData().status()).isEqualTo("RECONCILED"))
                .verifyComplete();
        StepVerifier.create(controller.listImpactWindows(40L, 50L, 10L, "SCHEDULED", null, 25))
                .assertNext(response -> assertThat(response.getData()).singleElement()
                        .satisfies(item -> assertThat(item.status()).isEqualTo("SCHEDULED")))
                .verifyComplete();
        StepVerifier.create(controller.evaluateDueImpactWindows(10))
                .assertNext(response -> assertThat(response.getData()).singleElement()
                        .satisfies(item -> assertThat(item.mutationId()).isEqualTo(50L)))
                .verifyComplete();

        verify(reconciliationService).list(7L, providerChangeQuery);
        verify(reconciliationService).reconcile(7L, 50L, "operator-1");
        verify(impactWindowService).list(7L, impactWindowQuery);
        verify(impactWindowService).evaluateDue(7L, 10, "operator-1");
    }

    private TenantContextResolver resolver() {
        TenantContextResolver resolver = mock(TenantContextResolver.class);
        when(resolver.currentOrError()).thenReturn(Mono.just(new TenantContext(7L, RoleNames.OPERATOR, "operator-1")));
        return resolver;
    }

    private SearchMarketingSourceCommand sourceCommand() {
        return new SearchMarketingSourceCommand(
                "GOOGLE_ADS",
                "ads-main",
                "Google Ads Main",
                "SEM",
                "123-456",
                null,
                "USD",
                "Asia/Shanghai",
                true,
                Map.of());
    }

    private SearchMarketingKeywordCommand keywordCommand() {
        return new SearchMarketingKeywordCommand(
                "SEM",
                "Running Shoes",
                "PHRASE",
                "https://example.com/shoes",
                "COMMERCIAL",
                List.of("brand"),
                "ACTIVE",
                Map.of());
    }

    private SearchMarketingSnapshotCommand snapshotCommand() {
        return new SearchMarketingSnapshotCommand(
                10L,
                20L,
                date(),
                "ALL",
                "ALL",
                "DEFAULT",
                1000L,
                40L,
                new BigDecimal("100.0000"),
                4L,
                new BigDecimal("400.0000"),
                new BigDecimal("2.5000"),
                Map.of());
    }

    private SearchMarketingOpportunityEvaluationCommand evaluationCommand() {
        return new SearchMarketingOpportunityEvaluationCommand(
                "SEM",
                10L,
                20L,
                date(),
                date(),
                100L,
                new BigDecimal("0.010000"),
                new BigDecimal("10.0000"),
                new BigDecimal("100.0000"));
    }

    private SearchMarketingMutationCommand mutationCommand() {
        return new SearchMarketingMutationCommand(
                10L,
                40L,
                20L,
                "bid-raise-1",
                "UPDATE_KEYWORD_BID",
                "KEYWORD",
                "customers/1/adGroupCriteria/2~3",
                true,
                "idem-1",
                Map.of("bidMicros", 1500000));
    }

    private SearchMarketingSourceView sourceView() {
        return new SearchMarketingSourceView(10L, 7L, "GOOGLE_ADS", "ads-main", "Google Ads Main",
                "SEM", "123-456", null, "USD", "Asia/Shanghai", true, Map.of(),
                "operator-1", now(), now());
    }

    private SearchMarketingKeywordView keywordView() {
        return new SearchMarketingKeywordView(20L, 7L, "SEM", "Running Shoes", "running shoes",
                "PHRASE", "https://example.com/shoes", "hash", "COMMERCIAL", List.of("brand"),
                "ACTIVE", Map.of(), "operator-1", now(), now());
    }

    private SearchMarketingSnapshotView snapshotView() {
        return new SearchMarketingSnapshotView(30L, 7L, 10L, 20L, "SEM", date(), "ALL", "ALL",
                "DEFAULT", 1000L, 40L, new BigDecimal("100.0000"), 4L,
                new BigDecimal("400.0000"), new BigDecimal("2.5000"), Map.of(),
                "operator-1", now(), now());
    }

    private SearchMarketingOpportunityView opportunityView() {
        return new SearchMarketingOpportunityView(40L, 7L, 10L, 20L, "SEM", "LOW_CTR", date(),
                "HIGH", "OPEN", "Improve CTR", new BigDecimal("5.0000"), Map.of("ctr", "0.005000"),
                "operator-1", now(), now());
    }

    private SearchMarketingSummaryView summaryView() {
        return new SearchMarketingSummaryView(7L, "SEM", 10L, 20L, date(), date().plusDays(1),
                2, 1500L, 50L, new BigDecimal("150.0000"), 5L, new BigDecimal("500.0000"),
                new BigDecimal("0.033333"), new BigDecimal("3.000000"),
                new BigDecimal("0.100000"), new BigDecimal("3.333333"), new BigDecimal("2.666667"));
    }

    private SearchMarketingMutationView mutationView(String status) {
        return new SearchMarketingMutationView(
                50L,
                7L,
                10L,
                40L,
                20L,
                "GOOGLE_ADS",
                "SEM",
                "bid-raise-1",
                "UPDATE_KEYWORD_BID",
                "KEYWORD",
                "customers/1/adGroupCriteria/2~3",
                "hash",
                "idem-1",
                status,
                "READY".equals(status) || "DRY_RUN_OK".equals(status) ? "APPROVED" : "PENDING",
                true,
                Map.of("bidMicros", 1500000),
                Map.of(),
                Map.of(),
                Map.of(),
                null,
                null,
                "operator-1",
                "operator-1",
                now(),
                "operator-1",
                now(),
                now(),
                now());
    }

    private SearchMarketingSyncRunView syncRunView() {
        return new SearchMarketingSyncRunView(60L, 7L, 10L, "PERFORMANCE", "GOOGLE_ADS", "SEM",
                "idem", date(), date(), null, "SUCCEEDED", false, 1L, 1L, 0L,
                "provider-request-1", null, null, Map.of("status", "ok"),
                "operator-1", now(), now(), now());
    }

    private SearchMarketingUrlInspectionView urlInspectionView() {
        return new SearchMarketingUrlInspectionView(70L, 7L, 10L, "GOOGLE_ADS",
                "https://example.com/shoes", "hash", date(), "INDEXED", "CRAWLED",
                "https://example.com/shoes", "PRESENT", "PASS", now(), Map.of(),
                "operator-1", now(), now());
    }

    private SearchMarketingReadinessView readinessView() {
        return new SearchMarketingReadinessView(7L, "DEGRADED",
                List.of("failed sync runs require operator attention"),
                Map.of("reason", "sync freshness needs attention"), now());
    }

    private SearchMarketingProviderChangeView providerChangeView() {
        return new SearchMarketingProviderChangeView(80L, 7L, 10L, 50L, "GOOGLE_ADS",
                "customers/1/adGroupCriteria/2~3", "UPDATE_KEYWORD_BID", Map.of("bidMicros", 1500000),
                "operator-1", now(), "CONFIRMED", Map.of("providerOperationId", "operations/1"),
                now(), now());
    }

    private SearchMarketingReconciliationView reconciliationView() {
        return new SearchMarketingReconciliationView(7L, 50L, 80L, "RECONCILED",
                "operations/1", Map.of("confirmed", true), now());
    }

    private SearchMarketingImpactWindowView impactWindowView() {
        return new SearchMarketingImpactWindowView(90L, 7L, 40L, 50L, 10L, 20L, "hash",
                date().minusDays(7), date().minusDays(1), date().plusDays(1), date().plusDays(7),
                "SCHEDULED", null, new BigDecimal("0.0000"), Map.of(), Map.of(),
                now().plusDays(8), null, "operator-1", now(), now());
    }

    private LocalDate date() {
        return LocalDate.of(2026, 6, 6);
    }

    private LocalDateTime now() {
        return LocalDateTime.of(2026, 6, 6, 0, 0);
    }
}
