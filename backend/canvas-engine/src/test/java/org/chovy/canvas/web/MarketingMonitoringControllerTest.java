package org.chovy.canvas.web;

import org.chovy.canvas.common.tenant.RoleNames;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.monitoring.MarketingCompetitorMentionView;
import org.chovy.canvas.domain.monitoring.MarketingMonitorAlertChannelCommand;
import org.chovy.canvas.domain.monitoring.MarketingMonitorAlertChannelView;
import org.chovy.canvas.domain.monitoring.MarketingMonitorAlertDeliveryView;
import org.chovy.canvas.domain.monitoring.MarketingMonitorAlertDispatchView;
import org.chovy.canvas.domain.monitoring.MarketingMonitorAlertFanoutService;
import org.chovy.canvas.domain.monitoring.MarketingMonitorAlertQuery;
import org.chovy.canvas.domain.monitoring.MarketingMonitorAlertView;
import org.chovy.canvas.domain.monitoring.MarketingMonitorIngestResult;
import org.chovy.canvas.domain.monitoring.MarketingMonitorInferenceCommand;
import org.chovy.canvas.domain.monitoring.MarketingMonitorInferenceQuery;
import org.chovy.canvas.domain.monitoring.MarketingMonitorInferenceService;
import org.chovy.canvas.domain.monitoring.MarketingMonitorInferenceView;
import org.chovy.canvas.domain.monitoring.MarketingMonitorItemIngestCommand;
import org.chovy.canvas.domain.monitoring.MarketingMonitorItemQuery;
import org.chovy.canvas.domain.monitoring.MarketingMonitorItemView;
import org.chovy.canvas.domain.monitoring.MarketingMonitorPollCommand;
import org.chovy.canvas.domain.monitoring.MarketingMonitorPollRunView;
import org.chovy.canvas.domain.monitoring.MarketingMonitorPollingService;
import org.chovy.canvas.domain.monitoring.MarketingMonitorProviderCredentialCommand;
import org.chovy.canvas.domain.monitoring.MarketingMonitorProviderCredentialDueRefreshCommand;
import org.chovy.canvas.domain.monitoring.MarketingMonitorProviderCredentialDueRefreshResult;
import org.chovy.canvas.domain.monitoring.MarketingMonitorProviderCredentialEventQuery;
import org.chovy.canvas.domain.monitoring.MarketingMonitorProviderCredentialEventView;
import org.chovy.canvas.domain.monitoring.MarketingMonitorProviderCredentialQuery;
import org.chovy.canvas.domain.monitoring.MarketingMonitorProviderCredentialRefreshCommand;
import org.chovy.canvas.domain.monitoring.MarketingMonitorProviderCredentialRevokeCommand;
import org.chovy.canvas.domain.monitoring.MarketingMonitorProviderCredentialService;
import org.chovy.canvas.domain.monitoring.MarketingMonitorProviderCredentialView;
import org.chovy.canvas.domain.monitoring.MarketingMonitorProviderOAuthAuthorizationCommand;
import org.chovy.canvas.domain.monitoring.MarketingMonitorProviderOAuthAuthorizationEventQuery;
import org.chovy.canvas.domain.monitoring.MarketingMonitorProviderOAuthAuthorizationEventView;
import org.chovy.canvas.domain.monitoring.MarketingMonitorProviderOAuthAuthorizationQuery;
import org.chovy.canvas.domain.monitoring.MarketingMonitorProviderOAuthAuthorizationService;
import org.chovy.canvas.domain.monitoring.MarketingMonitorProviderOAuthAuthorizationView;
import org.chovy.canvas.domain.monitoring.MarketingMonitorProviderOAuthCallbackCommand;
import org.chovy.canvas.domain.monitoring.MarketingMonitorSourceCommand;
import org.chovy.canvas.domain.monitoring.MarketingMonitorSourcePollingCommand;
import org.chovy.canvas.domain.monitoring.MarketingMonitorSourcePollingView;
import org.chovy.canvas.domain.monitoring.MarketingMonitorSourceView;
import org.chovy.canvas.domain.monitoring.MarketingMonitorTrendSnapshotCommand;
import org.chovy.canvas.domain.monitoring.MarketingMonitorTrendSnapshotQuery;
import org.chovy.canvas.domain.monitoring.MarketingMonitorTrendSnapshotView;
import org.chovy.canvas.domain.monitoring.MarketingMonitoringService;
import org.chovy.canvas.domain.monitoring.MarketingSentimentAnalysisView;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MarketingMonitoringControllerTest {

    @Test
    void upsertSourcePassesCurrentTenantAndOperator() {
        MarketingMonitoringService service = mock(MarketingMonitoringService.class);
        MarketingMonitorSourceCommand command = sourceCommand();
        when(service.upsertSource(7L, command, "operator-1")).thenReturn(sourceView());
        MarketingMonitoringController controller = new MarketingMonitoringController(
                service, fanoutService(), resolver());

        StepVerifier.create(controller.upsertSource(command))
                .assertNext(response -> {
                    assertThat(response.getCode()).isZero();
                    assertThat(response.getData().sourceType()).isEqualTo("MANUAL");
                })
                .verifyComplete();

        verify(service).upsertSource(7L, command, "operator-1");
    }

    @Test
    void ingestItemPassesCurrentTenantAndOperator() {
        MarketingMonitoringService service = mock(MarketingMonitoringService.class);
        MarketingMonitorItemIngestCommand command = ingestCommand();
        when(service.ingestItem(7L, command, "operator-1")).thenReturn(ingestResult());
        MarketingMonitoringController controller = new MarketingMonitoringController(
                service, fanoutService(), resolver());

        StepVerifier.create(controller.ingestItem(command))
                .assertNext(response -> assertThat(response.getData().sentiment().sentimentLabel())
                        .isEqualTo("NEGATIVE"))
                .verifyComplete();

        verify(service).ingestItem(7L, command, "operator-1");
    }

    @Test
    void itemEndpointPassesFiltersAndUpperBoundedLimit() {
        MarketingMonitoringService service = mock(MarketingMonitoringService.class);
        MarketingMonitorItemQuery query = new MarketingMonitorItemQuery("NEGATIVE", "competitorx", 100);
        when(service.items(7L, query)).thenReturn(List.of(itemView()));
        MarketingMonitoringController controller = new MarketingMonitoringController(
                service, fanoutService(), resolver());

        StepVerifier.create(controller.items("NEGATIVE", "competitorx", 500))
                .assertNext(response -> assertThat(response.getData()).singleElement()
                        .satisfies(item -> assertThat(item.id()).isEqualTo(100L)))
                .verifyComplete();

        verify(service).items(7L, query);
    }

    @Test
    void alertEndpointPassesStatusAndLowerBoundedLimit() {
        MarketingMonitoringService service = mock(MarketingMonitoringService.class);
        MarketingMonitorAlertQuery query = new MarketingMonitorAlertQuery("OPEN", 1);
        when(service.alerts(7L, query)).thenReturn(List.of(alertView()));
        MarketingMonitoringController controller = new MarketingMonitoringController(
                service, fanoutService(), resolver());

        StepVerifier.create(controller.alerts("OPEN", 0))
                .assertNext(response -> assertThat(response.getData()).singleElement()
                        .satisfies(alert -> assertThat(alert.status()).isEqualTo("OPEN")))
                .verifyComplete();

        verify(service).alerts(7L, query);
    }

    @Test
    void resolveAlertPassesCurrentTenantAlertAndOperator() {
        MarketingMonitoringService service = mock(MarketingMonitoringService.class);
        when(service.resolveAlert(7L, 401L, "operator-1")).thenReturn(resolvedAlertView());
        MarketingMonitoringController controller = new MarketingMonitoringController(
                service, fanoutService(), resolver());

        StepVerifier.create(controller.resolveAlert(401L))
                .assertNext(response -> assertThat(response.getData().status()).isEqualTo("RESOLVED"))
                .verifyComplete();

        verify(service).resolveAlert(7L, 401L, "operator-1");
    }

    @Test
    void upsertAlertChannelPassesCurrentTenantAndOperator() {
        MarketingMonitoringService service = mock(MarketingMonitoringService.class);
        MarketingMonitorAlertFanoutService fanoutService = mock(MarketingMonitorAlertFanoutService.class);
        MarketingMonitorAlertChannelCommand command = alertChannelCommand();
        when(fanoutService.upsertChannel(7L, command, "operator-1")).thenReturn(alertChannelView());
        MarketingMonitoringController controller = new MarketingMonitoringController(
                service, fanoutService, resolver());

        StepVerifier.create(controller.upsertAlertChannel(command))
                .assertNext(response -> assertThat(response.getData().channelKey()).isEqualTo("brand-duty"))
                .verifyComplete();

        verify(fanoutService).upsertChannel(7L, command, "operator-1");
    }

    @Test
    void dispatchAlertPassesCurrentTenantAlertAndOperator() {
        MarketingMonitoringService service = mock(MarketingMonitoringService.class);
        MarketingMonitorAlertFanoutService fanoutService = mock(MarketingMonitorAlertFanoutService.class);
        when(fanoutService.dispatchAlert(7L, 401L, "operator-1")).thenReturn(dispatchView());
        MarketingMonitoringController controller = new MarketingMonitoringController(
                service, fanoutService, resolver());

        StepVerifier.create(controller.dispatchAlert(401L))
                .assertNext(response -> assertThat(response.getData().delivered()).isEqualTo(1))
                .verifyComplete();

        verify(fanoutService).dispatchAlert(7L, 401L, "operator-1");
    }

    @Test
    void alertDeliveryEndpointPassesFiltersAndBoundedLimit() {
        MarketingMonitoringService service = mock(MarketingMonitoringService.class);
        MarketingMonitorAlertFanoutService fanoutService = mock(MarketingMonitorAlertFanoutService.class);
        when(fanoutService.deliveries(7L, 401L, "SUCCESS", 100))
                .thenReturn(List.of(deliveryView()));
        MarketingMonitoringController controller = new MarketingMonitoringController(
                service, fanoutService, resolver());

        StepVerifier.create(controller.alertDeliveries(401L, "SUCCESS", 500))
                .assertNext(response -> assertThat(response.getData()).singleElement()
                        .satisfies(delivery -> assertThat(delivery.status()).isEqualTo("SUCCESS")))
                .verifyComplete();

        verify(fanoutService).deliveries(7L, 401L, "SUCCESS", 100);
    }

    @Test
    void configurePollingPassesCurrentTenantSourceAndOperator() {
        MarketingMonitoringService service = mock(MarketingMonitoringService.class);
        MarketingMonitorPollingService pollingService = mock(MarketingMonitorPollingService.class);
        MarketingMonitorSourcePollingCommand command = pollingCommand();
        when(pollingService.configurePolling(7L, 10L, command, "operator-1")).thenReturn(pollingView());
        MarketingMonitoringController controller = new MarketingMonitoringController(
                service, fanoutService(), pollingService, resolver());

        StepVerifier.create(controller.configureSourcePolling(10L, command))
                .assertNext(response -> {
                    assertThat(response.getCode()).isZero();
                    assertThat(response.getData().pollEnabled()).isTrue();
                    assertThat(response.getData().pollIntervalMinutes()).isEqualTo(15);
                })
                .verifyComplete();

        verify(pollingService).configurePolling(7L, 10L, command, "operator-1");
    }

    @Test
    void pollSourcePassesCurrentTenantSourceCommandAndOperator() {
        MarketingMonitoringService service = mock(MarketingMonitoringService.class);
        MarketingMonitorPollingService pollingService = mock(MarketingMonitorPollingService.class);
        MarketingMonitorPollCommand command = pollCommand();
        when(pollingService.pollSource(7L, 10L, command, "operator-1")).thenReturn(pollRunView());
        MarketingMonitoringController controller = new MarketingMonitoringController(
                service, fanoutService(), pollingService, resolver());

        StepVerifier.create(controller.pollSource(10L, command))
                .assertNext(response -> {
                    assertThat(response.getCode()).isZero();
                    assertThat(response.getData().status()).isEqualTo("COMPLETED");
                    assertThat(response.getData().insertedCount()).isEqualTo(2);
                })
                .verifyComplete();

        verify(pollingService).pollSource(7L, 10L, command, "operator-1");
    }

    @Test
    void buildTrendSnapshotPassesCurrentTenantCommandAndOperator() {
        MarketingMonitoringService service = mock(MarketingMonitoringService.class);
        MarketingMonitorPollingService pollingService = mock(MarketingMonitorPollingService.class);
        MarketingMonitorTrendSnapshotCommand command = trendCommand();
        when(pollingService.buildTrendSnapshot(7L, command, "operator-1")).thenReturn(trendView());
        MarketingMonitoringController controller = new MarketingMonitoringController(
                service, fanoutService(), pollingService, resolver());

        StepVerifier.create(controller.buildTrendSnapshot(command))
                .assertNext(response -> {
                    assertThat(response.getCode()).isZero();
                    assertThat(response.getData().bucketGrain()).isEqualTo("DAY");
                    assertThat(response.getData().mentionCount()).isEqualTo(9);
                })
                .verifyComplete();

        verify(pollingService).buildTrendSnapshot(7L, command, "operator-1");
    }

    @Test
    void trendSnapshotEndpointPassesFiltersAndBoundedLimit() {
        MarketingMonitoringService service = mock(MarketingMonitoringService.class);
        MarketingMonitorPollingService pollingService = mock(MarketingMonitorPollingService.class);
        MarketingMonitorTrendSnapshotQuery query =
                new MarketingMonitorTrendSnapshotQuery(10L, "our-brand", "competitorx", 100);
        when(pollingService.trendSnapshots(7L, query)).thenReturn(List.of(trendView()));
        MarketingMonitoringController controller = new MarketingMonitoringController(
                service, fanoutService(), pollingService, resolver());

        StepVerifier.create(controller.trendSnapshots(10L, "our-brand", "competitorx", 500))
                .assertNext(response -> assertThat(response.getData()).singleElement()
                        .satisfies(snapshot -> assertThat(snapshot.alertCount()).isEqualTo(2)))
                .verifyComplete();

        verify(pollingService).trendSnapshots(7L, query);
    }

    @Test
    void inferenceEndpointUsesPathItemIdAndPassesCurrentTenantAndOperator() {
        MarketingMonitoringService service = mock(MarketingMonitoringService.class);
        MarketingMonitorPollingService pollingService = mock(MarketingMonitorPollingService.class);
        MarketingMonitorInferenceService inferenceService = mock(MarketingMonitorInferenceService.class);
        MarketingMonitorInferenceCommand body = new MarketingMonitorInferenceCommand(
                null,
                88L,
                99L,
                "gpt-monitor",
                "2026-06",
                true,
                Map.of("temperature", 0.1),
                1000,
                Map.of("reason", "operator-check"));
        MarketingMonitorInferenceCommand expected = new MarketingMonitorInferenceCommand(
                100L,
                88L,
                99L,
                "gpt-monitor",
                "2026-06",
                true,
                Map.of("temperature", 0.1),
                1000,
                Map.of("reason", "operator-check"));
        when(inferenceService.analyze(7L, expected, "operator-1")).thenReturn(inferenceView());
        MarketingMonitoringController controller = new MarketingMonitoringController(
                service, fanoutService(), pollingService, inferenceService, resolver());

        StepVerifier.create(controller.analyzeInference(100L, body))
                .assertNext(response -> {
                    assertThat(response.getCode()).isZero();
                    assertThat(response.getData().sentimentLabel()).isEqualTo("NEGATIVE");
                    assertThat(response.getData().fallbackUsed()).isTrue();
                })
                .verifyComplete();

        verify(inferenceService).analyze(7L, expected, "operator-1");
    }

    @Test
    void inferenceListEndpointPassesFiltersAndBoundedLimit() {
        MarketingMonitoringService service = mock(MarketingMonitoringService.class);
        MarketingMonitorPollingService pollingService = mock(MarketingMonitorPollingService.class);
        MarketingMonitorInferenceService inferenceService = mock(MarketingMonitorInferenceService.class);
        MarketingMonitorInferenceQuery query =
                new MarketingMonitorInferenceQuery(100L, "negative", "gpt-monitor", "success", false, 100);
        when(inferenceService.list(7L, query)).thenReturn(List.of(inferenceView()));
        MarketingMonitoringController controller = new MarketingMonitoringController(
                service, fanoutService(), pollingService, inferenceService, resolver());

        StepVerifier.create(controller.inferences(100L, "negative", "gpt-monitor", "success", false, 500))
                .assertNext(response -> assertThat(response.getData()).singleElement()
                        .satisfies(inference -> assertThat(inference.modelKey()).isEqualTo("gpt-monitor")))
                .verifyComplete();

        verify(inferenceService).list(7L, query);
    }

    @Test
    void upsertProviderCredentialPassesCurrentTenantAndOperator() {
        MarketingMonitoringService service = mock(MarketingMonitoringService.class);
        MarketingMonitorProviderCredentialService credentialService =
                mock(MarketingMonitorProviderCredentialService.class);
        MarketingMonitorProviderCredentialCommand command = credentialCommand();
        when(credentialService.upsert(7L, command, "operator-1")).thenReturn(credentialView("ACTIVE"));
        MarketingMonitoringController controller = new MarketingMonitoringController(
                service, fanoutService(), null, null, credentialService, resolver());

        StepVerifier.create(controller.upsertProviderCredential(command))
                .assertNext(response -> {
                    assertThat(response.getCode()).isZero();
                    assertThat(response.getData().credentialKey()).isEqualTo("x-prod");
                })
                .verifyComplete();

        verify(credentialService).upsert(7L, command, "operator-1");
    }

    @Test
    void providerCredentialListEndpointPassesFiltersAndBoundedLimit() {
        MarketingMonitoringService service = mock(MarketingMonitoringService.class);
        MarketingMonitorProviderCredentialService credentialService =
                mock(MarketingMonitorProviderCredentialService.class);
        MarketingMonitorProviderCredentialQuery query =
                new MarketingMonitorProviderCredentialQuery("x_recent_search", "oauth2_bearer", "active", 100);
        when(credentialService.list(7L, query)).thenReturn(List.of(credentialView("ACTIVE")));
        MarketingMonitoringController controller = new MarketingMonitoringController(
                service, fanoutService(), null, null, credentialService, resolver());

        StepVerifier.create(controller.providerCredentials("x_recent_search", "oauth2_bearer", "active", 500))
                .assertNext(response -> assertThat(response.getData()).singleElement()
                        .satisfies(credential -> assertThat(credential.providerType()).isEqualTo("X_RECENT_SEARCH")))
                .verifyComplete();

        verify(credentialService).list(7L, query);
    }

    @Test
    void refreshProviderCredentialPassesPathKeyCommandAndOperator() {
        MarketingMonitoringService service = mock(MarketingMonitoringService.class);
        MarketingMonitorProviderCredentialService credentialService =
                mock(MarketingMonitorProviderCredentialService.class);
        MarketingMonitorProviderCredentialRefreshCommand command =
                new MarketingMonitorProviderCredentialRefreshCommand(true);
        when(credentialService.refresh(7L, "x-prod", command, "operator-1"))
                .thenReturn(credentialView("ACTIVE"));
        MarketingMonitoringController controller = new MarketingMonitoringController(
                service, fanoutService(), null, null, credentialService, resolver());

        StepVerifier.create(controller.refreshProviderCredential("x-prod", command))
                .assertNext(response -> assertThat(response.getData().lastRefreshStatus()).isEqualTo("SUCCESS"))
                .verifyComplete();

        verify(credentialService).refresh(7L, "x-prod", command, "operator-1");
    }

    @Test
    void refreshDueProviderCredentialsPassesCurrentTenantCommandAndOperator() {
        MarketingMonitoringService service = mock(MarketingMonitoringService.class);
        MarketingMonitorProviderCredentialService credentialService =
                mock(MarketingMonitorProviderCredentialService.class);
        MarketingMonitorProviderCredentialDueRefreshCommand command =
                new MarketingMonitorProviderCredentialDueRefreshCommand(10, 500);
        MarketingMonitorProviderCredentialDueRefreshResult result =
                new MarketingMonitorProviderCredentialDueRefreshResult(
                        7L,
                        2,
                        1,
                        1,
                        0,
                        1,
                        now().plusMinutes(10),
                        now(),
                        List.of(credentialView("ACTIVE")));
        when(credentialService.refreshDue(7L,
                new MarketingMonitorProviderCredentialDueRefreshCommand(10, 100),
                "operator-1")).thenReturn(result);
        MarketingMonitoringController controller = new MarketingMonitoringController(
                service, fanoutService(), null, null, credentialService, resolver());

        StepVerifier.create(controller.refreshDueProviderCredentials(command))
                .assertNext(response -> {
                    assertThat(response.getCode()).isZero();
                    assertThat(response.getData().refreshedCount()).isEqualTo(1);
                })
                .verifyComplete();

        verify(credentialService).refreshDue(7L,
                new MarketingMonitorProviderCredentialDueRefreshCommand(10, 100),
                "operator-1");
    }

    @Test
    void revokeProviderCredentialPassesPathKeyCommandAndOperator() {
        MarketingMonitoringService service = mock(MarketingMonitoringService.class);
        MarketingMonitorProviderCredentialService credentialService =
                mock(MarketingMonitorProviderCredentialService.class);
        MarketingMonitorProviderCredentialRevokeCommand command =
                new MarketingMonitorProviderCredentialRevokeCommand(null, "refresh_token", true, true,
                        Map.of("ticket", "SR-1"));
        when(credentialService.revoke(7L, "x-prod", command, "operator-1"))
                .thenReturn(credentialView("DISABLED"));
        MarketingMonitoringController controller = new MarketingMonitoringController(
                service, fanoutService(), null, null, credentialService, resolver());

        StepVerifier.create(controller.revokeProviderCredential("x-prod", command))
                .assertNext(response -> assertThat(response.getData().status()).isEqualTo("DISABLED"))
                .verifyComplete();

        verify(credentialService).revoke(7L, "x-prod", command, "operator-1");
    }

    @Test
    void disableProviderCredentialPassesPathKeyAndOperator() {
        MarketingMonitoringService service = mock(MarketingMonitoringService.class);
        MarketingMonitorProviderCredentialService credentialService =
                mock(MarketingMonitorProviderCredentialService.class);
        when(credentialService.disable(7L, "x-prod", "operator-1")).thenReturn(credentialView("DISABLED"));
        MarketingMonitoringController controller = new MarketingMonitoringController(
                service, fanoutService(), null, null, credentialService, resolver());

        StepVerifier.create(controller.disableProviderCredential("x-prod"))
                .assertNext(response -> assertThat(response.getData().status()).isEqualTo("DISABLED"))
                .verifyComplete();

        verify(credentialService).disable(7L, "x-prod", "operator-1");
    }

    @Test
    void providerCredentialEventEndpointPassesFiltersAndBoundedLimit() {
        MarketingMonitoringService service = mock(MarketingMonitoringService.class);
        MarketingMonitorProviderCredentialService credentialService =
                mock(MarketingMonitorProviderCredentialService.class);
        MarketingMonitorProviderCredentialEventQuery query =
                new MarketingMonitorProviderCredentialEventQuery("x-prod", "refreshed", "success", 100);
        when(credentialService.events(7L, query)).thenReturn(List.of(credentialEventView()));
        MarketingMonitoringController controller = new MarketingMonitoringController(
                service, fanoutService(), null, null, credentialService, resolver());

        StepVerifier.create(controller.providerCredentialEvents("x-prod", "refreshed", "success", 500))
                .assertNext(response -> assertThat(response.getData()).singleElement()
                        .satisfies(event -> assertThat(event.eventType()).isEqualTo("REFRESHED")))
                .verifyComplete();

        verify(credentialService).events(7L, query);
    }

    @Test
    void startProviderOAuthAuthorizationPassesCurrentTenantAndOperator() {
        MarketingMonitoringService service = mock(MarketingMonitoringService.class);
        MarketingMonitorProviderCredentialService credentialService =
                mock(MarketingMonitorProviderCredentialService.class);
        MarketingMonitorProviderOAuthAuthorizationService oauthService =
                mock(MarketingMonitorProviderOAuthAuthorizationService.class);
        MarketingMonitorProviderOAuthAuthorizationCommand command = oauthCommand();
        when(oauthService.startAuthorization(7L, command, "operator-1")).thenReturn(oauthView("PENDING"));
        MarketingMonitoringController controller = new MarketingMonitoringController(
                service, fanoutService(), null, null, credentialService, oauthService, resolver());

        StepVerifier.create(controller.startProviderOAuthAuthorization(command))
                .assertNext(response -> {
                    assertThat(response.getCode()).isZero();
                    assertThat(response.getData().authorizationUrl()).contains("state=state-1");
                })
                .verifyComplete();

        verify(oauthService).startAuthorization(7L, command, "operator-1");
    }

    @Test
    void completeProviderOAuthAuthorizationPassesCurrentTenantCommandAndOperator() {
        MarketingMonitoringService service = mock(MarketingMonitoringService.class);
        MarketingMonitorProviderCredentialService credentialService =
                mock(MarketingMonitorProviderCredentialService.class);
        MarketingMonitorProviderOAuthAuthorizationService oauthService =
                mock(MarketingMonitorProviderOAuthAuthorizationService.class);
        MarketingMonitorProviderOAuthCallbackCommand command = oauthCallback();
        when(oauthService.completeAuthorization(7L, command, "operator-1")).thenReturn(oauthView("EXCHANGED"));
        MarketingMonitoringController controller = new MarketingMonitoringController(
                service, fanoutService(), null, null, credentialService, oauthService, resolver());

        StepVerifier.create(controller.completeProviderOAuthAuthorization(command))
                .assertNext(response -> assertThat(response.getData().status()).isEqualTo("EXCHANGED"))
                .verifyComplete();

        verify(oauthService).completeAuthorization(7L, command, "operator-1");
    }

    @Test
    void providerOAuthAuthorizationListEndpointPassesFiltersAndBoundedLimit() {
        MarketingMonitoringService service = mock(MarketingMonitoringService.class);
        MarketingMonitorProviderCredentialService credentialService =
                mock(MarketingMonitorProviderCredentialService.class);
        MarketingMonitorProviderOAuthAuthorizationService oauthService =
                mock(MarketingMonitorProviderOAuthAuthorizationService.class);
        MarketingMonitorProviderOAuthAuthorizationQuery query =
                new MarketingMonitorProviderOAuthAuthorizationQuery("x-prod", "x_recent_search", "pending", 100);
        when(oauthService.list(7L, query)).thenReturn(List.of(oauthView("PENDING")));
        MarketingMonitoringController controller = new MarketingMonitoringController(
                service, fanoutService(), null, null, credentialService, oauthService, resolver());

        StepVerifier.create(controller.providerOAuthAuthorizations("x-prod", "x_recent_search", "pending", 500))
                .assertNext(response -> assertThat(response.getData()).singleElement()
                        .satisfies(auth -> assertThat(auth.authState()).isEqualTo("state-1")))
                .verifyComplete();

        verify(oauthService).list(7L, query);
    }

    @Test
    void providerOAuthAuthorizationEventEndpointPassesFiltersAndBoundedLimit() {
        MarketingMonitoringService service = mock(MarketingMonitoringService.class);
        MarketingMonitorProviderCredentialService credentialService =
                mock(MarketingMonitorProviderCredentialService.class);
        MarketingMonitorProviderOAuthAuthorizationService oauthService =
                mock(MarketingMonitorProviderOAuthAuthorizationService.class);
        MarketingMonitorProviderOAuthAuthorizationEventQuery query =
                new MarketingMonitorProviderOAuthAuthorizationEventQuery("state-1", "x-prod", "exchanged", "success", 100);
        when(oauthService.events(7L, query)).thenReturn(List.of(oauthEventView()));
        MarketingMonitoringController controller = new MarketingMonitoringController(
                service, fanoutService(), null, null, credentialService, oauthService, resolver());

        StepVerifier.create(controller.providerOAuthAuthorizationEvents(
                        "state-1", "x-prod", "exchanged", "success", 500))
                .assertNext(response -> assertThat(response.getData()).singleElement()
                        .satisfies(event -> assertThat(event.eventType()).isEqualTo("EXCHANGED")))
                .verifyComplete();

        verify(oauthService).events(7L, query);
    }

    private TenantContextResolver resolver() {
        TenantContextResolver resolver = mock(TenantContextResolver.class);
        when(resolver.currentOrError()).thenReturn(Mono.just(new TenantContext(7L, RoleNames.OPERATOR, "operator-1")));
        return resolver;
    }

    private MarketingMonitorAlertFanoutService fanoutService() {
        return mock(MarketingMonitorAlertFanoutService.class);
    }

    private MarketingMonitorSourcePollingCommand pollingCommand() {
        return new MarketingMonitorSourcePollingCommand(
                true,
                15,
                "cursor-1",
                now().plusMinutes(15));
    }

    private MarketingMonitorPollCommand pollCommand() {
        return new MarketingMonitorPollCommand(
                now().minusHours(1),
                now(),
                "cursor-override",
                25,
                true);
    }

    private MarketingMonitorTrendSnapshotCommand trendCommand() {
        return new MarketingMonitorTrendSnapshotCommand(
                10L,
                "DAY",
                now().minusDays(1),
                now(),
                "our-brand",
                "competitorx",
                Map.of("source", "manual"));
    }

    private MarketingMonitorProviderCredentialCommand credentialCommand() {
        return new MarketingMonitorProviderCredentialCommand(
                "x-prod",
                "x_recent_search",
                "oauth2_bearer",
                "X Production",
                true,
                "access-token",
                "refresh-token",
                null,
                "Bearer",
                List.of("tweet.read"),
                now().plusHours(1),
                now().plusDays(30),
                "https://api.x.com/2/oauth2/token",
                "https://api.x.com/2/oauth2/revoke",
                "client-id",
                "client-secret",
                Map.of("owner", "brand-team"));
    }

    private MarketingMonitorProviderOAuthAuthorizationCommand oauthCommand() {
        return new MarketingMonitorProviderOAuthAuthorizationCommand(
                "x-prod",
                "x_recent_search",
                "oauth2_bearer",
                "X Production",
                "https://provider.example.test/oauth/authorize",
                "https://provider.example.test/oauth/token",
                "https://provider.example.test/oauth/revoke",
                "https://canvas.example.test/oauth/callback",
                "client-id",
                "client-secret",
                List.of("tweet.read"),
                Map.of("access_type", "offline"),
                20,
                Map.of("owner", "brand-team"));
    }

    private MarketingMonitorProviderOAuthCallbackCommand oauthCallback() {
        return new MarketingMonitorProviderOAuthCallbackCommand(
                "state-1",
                "auth-code",
                null,
                null,
                Map.of("providerRequestId", "cb-1"));
    }

    private MarketingMonitorSourceCommand sourceCommand() {
        return new MarketingMonitorSourceCommand(
                "manual-social-listening",
                "MANUAL",
                "Manual Social Listening",
                true,
                Map.of("owner", "brand-team"));
    }

    private MarketingMonitorAlertChannelCommand alertChannelCommand() {
        return new MarketingMonitorAlertChannelCommand(
                "brand-duty",
                "WEBHOOK",
                "Brand Duty",
                "https://hooks.example.test/brand-duty",
                true,
                "HIGH",
                List.of("NEGATIVE_SENTIMENT"),
                "CANVAS_HMAC",
                "fanout-secret",
                Map.of("owner", "brand-team"),
                3);
    }

    private MarketingMonitorItemIngestCommand ingestCommand() {
        return new MarketingMonitorItemIngestCommand(
                10L,
                "post-1",
                "https://example.com/post-1",
                "author-1",
                "our-brand",
                "CompetitorX has bad support",
                "en",
                now(),
                Map.of("competitorx", List.of("CompetitorX")),
                Map.of("provider", "manual"));
    }

    private MarketingMonitorSourceView sourceView() {
        return new MarketingMonitorSourceView(
                10L,
                7L,
                "manual-social-listening",
                "MANUAL",
                "Manual Social Listening",
                true,
                Map.of("owner", "brand-team"),
                "operator-1",
                now(),
                now());
    }

    private MarketingMonitorSourcePollingView pollingView() {
        return new MarketingMonitorSourcePollingView(
                7L,
                10L,
                "manual-social-listening",
                "MANUAL",
                true,
                15,
                "cursor-1",
                now().minusMinutes(15),
                now().plusMinutes(15),
                "COMPLETED",
                now());
    }

    private MarketingMonitorPollRunView pollRunView() {
        return new MarketingMonitorPollRunView(
                501L,
                7L,
                10L,
                "manual-social-listening",
                "MANUAL",
                "COMPLETED",
                now().minusHours(1),
                now(),
                "cursor-override",
                "cursor-2",
                3,
                2,
                1,
                1,
                null,
                Map.of("providerRunId", "run-1"),
                "operator-1",
                now().minusMinutes(1),
                now(),
                now().minusMinutes(1),
                now());
    }

    private MarketingMonitorTrendSnapshotView trendView() {
        return new MarketingMonitorTrendSnapshotView(
                601L,
                7L,
                10L,
                "manual-social-listening",
                "DAY",
                now().minusDays(1),
                now(),
                "our-brand",
                "competitorx",
                9,
                3,
                2,
                4,
                5,
                2,
                new BigDecimal("-0.12500"),
                Map.of("source", "manual"),
                "operator-1",
                now(),
                now());
    }

    private MarketingMonitorInferenceView inferenceView() {
        return new MarketingMonitorInferenceView(
                501L,
                7L,
                100L,
                10L,
                88L,
                99L,
                "gpt-monitor",
                "2026-06",
                "LOCAL_FALLBACK",
                true,
                "a".repeat(64),
                "b".repeat(64),
                "NEGATIVE",
                new BigDecimal("-0.50000"),
                new BigDecimal("0.80000"),
                List.of(Map.of("name", "CompetitorX")),
                List.of("support"),
                List.of("SENSITIVE_REFUND"),
                Map.of("summary", "sample"),
                12L,
                "operator-1",
                now(),
                now());
    }

    private MarketingMonitorProviderCredentialView credentialView(String status) {
        return new MarketingMonitorProviderCredentialView(
                700L,
                7L,
                "x-prod",
                "X_RECENT_SEARCH",
                "OAUTH2_BEARER",
                "X Production",
                status,
                "Bearer",
                List.of("tweet.read"),
                "access-toke",
                "refresh-tok",
                null,
                "https://api.x.com/2/oauth2/token",
                "https://api.x.com/2/oauth2/revoke",
                now().plusHours(1),
                now().plusDays(30),
                null,
                now(),
                1,
                "SUCCESS",
                null,
                null,
                null,
                Map.of("owner", "brand-team"),
                "operator-1",
                "operator-1",
                now(),
                now());
    }

    private MarketingMonitorProviderCredentialEventView credentialEventView() {
        return new MarketingMonitorProviderCredentialEventView(
                701L,
                7L,
                700L,
                "x-prod",
                "REFRESHED",
                "SUCCESS",
                Map.of("httpStatus", 200),
                null,
                "operator-1",
                now());
    }

    private MarketingMonitorProviderOAuthAuthorizationView oauthView(String status) {
        return new MarketingMonitorProviderOAuthAuthorizationView(
                801L,
                7L,
                "state-1",
                "x-prod",
                "X_RECENT_SEARCH",
                "OAUTH2_BEARER",
                "X Production",
                status,
                "https://provider.example.test/oauth/authorize?state=state-1",
                "https://provider.example.test/oauth/authorize",
                "https://provider.example.test/oauth/token",
                "https://canvas.example.test/oauth/callback",
                List.of("tweet.read"),
                "S256",
                "EXCHANGED".equals(status) ? 700L : null,
                null,
                null,
                "EXCHANGED".equals(status) ? 200 : null,
                null,
                now().plusMinutes(20),
                "EXCHANGED".equals(status) ? now() : null,
                Map.of("owner", "brand-team"),
                "operator-1",
                "operator-1",
                now(),
                now());
    }

    private MarketingMonitorProviderOAuthAuthorizationEventView oauthEventView() {
        return new MarketingMonitorProviderOAuthAuthorizationEventView(
                802L,
                7L,
                801L,
                "state-1",
                "x-prod",
                "EXCHANGED",
                "SUCCESS",
                Map.of("httpStatus", 200),
                null,
                "operator-1",
                now());
    }

    private MarketingMonitorIngestResult ingestResult() {
        return new MarketingMonitorIngestResult(
                itemView(),
                sentimentView(),
                List.of(competitorView()),
                List.of(alertView()));
    }

    private MarketingMonitorItemView itemView() {
        return new MarketingMonitorItemView(
                100L,
                7L,
                10L,
                "post-1",
                "MANUAL",
                "https://example.com/post-1",
                "author-1",
                "our-brand",
                "CompetitorX has bad support",
                "en",
                now(),
                now(),
                Map.of("provider", "manual"),
                "NEGATIVE",
                new BigDecimal("-1.00000"),
                new BigDecimal("0.80000"),
                List.of("competitorx"));
    }

    private MarketingSentimentAnalysisView sentimentView() {
        return new MarketingSentimentAnalysisView(
                200L,
                7L,
                100L,
                "NEGATIVE",
                new BigDecimal("-1.00000"),
                new BigDecimal("0.80000"),
                MarketingMonitoringService.SENTIMENT_MODEL_KEY,
                "lexicon_v1",
                Map.of("negative", List.of("bad"), "positive", List.of()),
                now());
    }

    private MarketingCompetitorMentionView competitorView() {
        return new MarketingCompetitorMentionView(
                300L,
                7L,
                100L,
                "competitorx",
                "CompetitorX",
                List.of("CompetitorX"),
                "NEGATIVE",
                new BigDecimal("-1.00000"),
                now());
    }

    private MarketingMonitorAlertView alertView() {
        return new MarketingMonitorAlertView(
                401L,
                7L,
                "NEGATIVE_SENTIMENT",
                "HIGH",
                "OPEN",
                "our-brand",
                "Negative sentiment detected",
                "Detected negative sentiment",
                1,
                now(),
                now(),
                Map.of("itemId", 100L),
                "operator-1",
                null,
                null,
                now(),
                now());
    }

    private MarketingMonitorAlertView resolvedAlertView() {
        return new MarketingMonitorAlertView(
                401L,
                7L,
                "NEGATIVE_SENTIMENT",
                "HIGH",
                "RESOLVED",
                "our-brand",
                "Negative sentiment detected",
                "Detected negative sentiment",
                1,
                now(),
                now(),
                Map.of("itemId", 100L),
                "operator-1",
                "operator-1",
                now(),
                now(),
                now());
    }

    private MarketingMonitorAlertChannelView alertChannelView() {
        return new MarketingMonitorAlertChannelView(
                11L,
                7L,
                "brand-duty",
                "WEBHOOK",
                "Brand Duty",
                "https://hooks.example.test/brand-duty",
                true,
                "HIGH",
                List.of("NEGATIVE_SENTIMENT"),
                "CANVAS_HMAC",
                "fanout-secre",
                Map.of("owner", "brand-team"),
                3,
                "operator-1",
                now(),
                now());
    }

    private MarketingMonitorAlertDispatchView dispatchView() {
        return new MarketingMonitorAlertDispatchView(
                7L,
                401L,
                1,
                1,
                0,
                List.of(deliveryView()));
    }

    private MarketingMonitorAlertDeliveryView deliveryView() {
        return new MarketingMonitorAlertDeliveryView(
                900L,
                7L,
                401L,
                11L,
                "brand-duty",
                "WEBHOOK",
                "delivery-1",
                1,
                202,
                "SUCCESS",
                null,
                null,
                null,
                now(),
                now());
    }

    private LocalDateTime now() {
        return LocalDateTime.of(2026, 6, 6, 8, 0);
    }
}
