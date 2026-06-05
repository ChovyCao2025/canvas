package org.chovy.canvas.web.bi;

import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.bi.subscription.BiAlertRuleCommand;
import org.chovy.canvas.domain.bi.subscription.BiAlertRuleView;
import org.chovy.canvas.domain.bi.subscription.BiDeliveryAttachmentCleanupResult;
import org.chovy.canvas.domain.bi.subscription.BiDeliveryAttachmentDownload;
import org.chovy.canvas.domain.bi.subscription.BiDeliveryAttachmentService;
import org.chovy.canvas.domain.bi.subscription.BiDeliveryAttachmentView;
import org.chovy.canvas.domain.bi.subscription.BiDeliveryAuditSummary;
import org.chovy.canvas.domain.bi.subscription.BiDeliveryRetryResult;
import org.chovy.canvas.domain.bi.subscription.BiDeliveryRunResult;
import org.chovy.canvas.domain.bi.subscription.BiDeliveryRuntimeService;
import org.chovy.canvas.domain.bi.subscription.BiDeliverySchedulerResult;
import org.chovy.canvas.domain.bi.subscription.BiDeliverySchedulerService;
import org.chovy.canvas.domain.bi.subscription.BiSubscriptionAdminService;
import org.chovy.canvas.domain.bi.subscription.BiSubscriptionCommand;
import org.chovy.canvas.domain.bi.subscription.BiSubscriptionView;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BiSubscriptionControllerTest {

    @Test
    void upsertSubscriptionPassesCurrentTenantUserAndRole() {
        TenantContextResolver resolver = mock(TenantContextResolver.class);
        when(resolver.current()).thenReturn(Mono.just(new TenantContext(7L, "OPERATOR", "alice")));
        BiSubscriptionAdminService service = mock(BiSubscriptionAdminService.class);
        BiDeliveryRuntimeService runtimeService = mock(BiDeliveryRuntimeService.class);
        BiSubscriptionCommand command = subscriptionCommand();
        when(service.upsertSubscription(7L, "alice", "OPERATOR", command)).thenReturn(subscriptionView());
        BiSubscriptionController controller = new BiSubscriptionController(resolver, service, runtimeService);

        StepVerifier.create(controller.upsertSubscription(command))
                .assertNext(response -> assertThat(response.getData().subscriptionKey()).isEqualTo("canvas-daily"))
                .verifyComplete();

        verify(service).upsertSubscription(7L, "alice", "OPERATOR", command);
    }

    @Test
    void listAlertsUsesCurrentTenant() {
        TenantContextResolver resolver = mock(TenantContextResolver.class);
        when(resolver.current()).thenReturn(Mono.just(new TenantContext(7L, "TENANT_ADMIN", "bob")));
        BiSubscriptionAdminService service = mock(BiSubscriptionAdminService.class);
        BiDeliveryRuntimeService runtimeService = mock(BiDeliveryRuntimeService.class);
        when(service.listAlerts(7L, 10)).thenReturn(List.of(alertView()));
        BiSubscriptionController controller = new BiSubscriptionController(resolver, service, runtimeService);

        StepVerifier.create(controller.listAlerts(10))
                .assertNext(response -> assertThat(response.getData()).singleElement()
                        .satisfies(alert -> assertThat(alert.metricKey()).isEqualTo("success_rate")))
                .verifyComplete();

        verify(service).listAlerts(7L, 10);
    }

    @Test
    void upsertAlertPassesCurrentTenantUserAndRole() {
        TenantContextResolver resolver = mock(TenantContextResolver.class);
        when(resolver.current()).thenReturn(Mono.just(new TenantContext(7L, "OPERATOR", "alice")));
        BiSubscriptionAdminService service = mock(BiSubscriptionAdminService.class);
        BiDeliveryRuntimeService runtimeService = mock(BiDeliveryRuntimeService.class);
        BiAlertRuleCommand command = alertCommand();
        when(service.upsertAlert(7L, "alice", "OPERATOR", command)).thenReturn(alertView());
        BiSubscriptionController controller = new BiSubscriptionController(resolver, service, runtimeService);

        StepVerifier.create(controller.upsertAlert(command))
                .assertNext(response -> assertThat(response.getData().alertKey()).isEqualTo("success-rate-alert"))
                .verifyComplete();

        verify(service).upsertAlert(7L, "alice", "OPERATOR", command);
    }

    @Test
    void runSubscriptionUsesCurrentTenantAndUser() {
        TenantContextResolver resolver = mock(TenantContextResolver.class);
        when(resolver.current()).thenReturn(Mono.just(new TenantContext(7L, "OPERATOR", "alice")));
        BiSubscriptionAdminService service = mock(BiSubscriptionAdminService.class);
        BiDeliveryRuntimeService runtimeService = mock(BiDeliveryRuntimeService.class);
        when(runtimeService.runSubscription(7L, 31L, "alice"))
                .thenReturn(new BiDeliveryRunResult("SUBSCRIPTION", 31L, "canvas-daily", "TRIGGERED", List.of()));
        BiSubscriptionController controller = new BiSubscriptionController(resolver, service, runtimeService);

        StepVerifier.create(controller.runSubscription(31L))
                .assertNext(response -> assertThat(response.getData().status()).isEqualTo("TRIGGERED"))
                .verifyComplete();

        verify(runtimeService).runSubscription(7L, 31L, "alice");
    }

    @Test
    void runAlertUsesCurrentTenantUserAndRole() {
        TenantContextResolver resolver = mock(TenantContextResolver.class);
        when(resolver.current()).thenReturn(Mono.just(new TenantContext(7L, "OPERATOR", "alice")));
        BiSubscriptionAdminService service = mock(BiSubscriptionAdminService.class);
        BiDeliveryRuntimeService runtimeService = mock(BiDeliveryRuntimeService.class);
        when(runtimeService.runAlert(7L, "alice", "OPERATOR", 41L))
                .thenReturn(new BiDeliveryRunResult("ALERT", 41L, "success-rate-alert", "TRIGGERED", List.of()));
        BiSubscriptionController controller = new BiSubscriptionController(resolver, service, runtimeService);

        StepVerifier.create(controller.runAlert(41L))
                .assertNext(response -> assertThat(response.getData().jobKey()).isEqualTo("success-rate-alert"))
                .verifyComplete();

        verify(runtimeService).runAlert(7L, "alice", "OPERATOR", 41L);
    }

    @Test
    void runDeliverySchedulerUsesCurrentTenantUserAndRole() {
        TenantContextResolver resolver = mock(TenantContextResolver.class);
        when(resolver.current()).thenReturn(Mono.just(new TenantContext(7L, "OPERATOR", "alice")));
        BiSubscriptionAdminService service = mock(BiSubscriptionAdminService.class);
        BiDeliveryRuntimeService runtimeService = mock(BiDeliveryRuntimeService.class);
        BiDeliverySchedulerService schedulerService = mock(BiDeliverySchedulerService.class);
        @SuppressWarnings("unchecked")
        ObjectProvider<BiDeliverySchedulerService> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(schedulerService);
        when(schedulerService.runDueOnce(7L, "alice", "OPERATOR", null))
                .thenReturn(new BiDeliverySchedulerResult(1, 1, 1, 0, 1, 0));
        BiSubscriptionController controller = new BiSubscriptionController(resolver, service, runtimeService, provider);

        StepVerifier.create(controller.runDeliveryScheduler())
                .assertNext(response -> {
                    assertThat(response.getData().subscriptionsTriggered()).isEqualTo(1);
                    assertThat(response.getData().alertsChecked()).isEqualTo(1);
                })
                .verifyComplete();

        verify(schedulerService).runDueOnce(7L, "alice", "OPERATOR", null);
    }

    @Test
    void retryDeliveryLogsUsesCurrentTenantAndUser() {
        TenantContextResolver resolver = mock(TenantContextResolver.class);
        when(resolver.current()).thenReturn(Mono.just(new TenantContext(7L, "OPERATOR", "alice")));
        BiSubscriptionAdminService service = mock(BiSubscriptionAdminService.class);
        BiDeliveryRuntimeService runtimeService = mock(BiDeliveryRuntimeService.class);
        when(runtimeService.retryPendingDeliveries(7L, "alice", 10))
                .thenReturn(new BiDeliveryRetryResult(2, 2, 1, 1, 0, List.of()));
        BiSubscriptionController controller = new BiSubscriptionController(resolver, service, runtimeService);

        StepVerifier.create(controller.retryDeliveryLogs(10))
                .assertNext(response -> {
                    assertThat(response.getData().checked()).isEqualTo(2);
                    assertThat(response.getData().delivered()).isEqualTo(1);
                })
                .verifyComplete();

        verify(runtimeService).retryPendingDeliveries(7L, "alice", 10);
    }

    @Test
    void auditDeliveryLogsUsesCurrentTenantAndFilters() {
        TenantContextResolver resolver = mock(TenantContextResolver.class);
        when(resolver.current()).thenReturn(Mono.just(new TenantContext(7L, "OPERATOR", "alice")));
        BiSubscriptionAdminService service = mock(BiSubscriptionAdminService.class);
        BiDeliveryRuntimeService runtimeService = mock(BiDeliveryRuntimeService.class);
        when(runtimeService.auditDeliveries(7L, "ALERT", "FAILED", "LARK", 41L, 25))
                .thenReturn(new BiDeliveryAuditSummary(2, 0, 0, 0, 0, 2, 1, 1, List.of()));
        BiSubscriptionController controller = new BiSubscriptionController(resolver, service, runtimeService);

        StepVerifier.create(controller.auditDeliveryLogs("ALERT", "FAILED", "LARK", 41L, 25))
                .assertNext(response -> {
                    assertThat(response.getData().total()).isEqualTo(2);
                    assertThat(response.getData().failed()).isEqualTo(2);
                    assertThat(response.getData().retryable()).isEqualTo(1);
                })
                .verifyComplete();

        verify(runtimeService).auditDeliveries(7L, "ALERT", "FAILED", "LARK", 41L, 25);
    }

    @Test
    void listDeliveryAttachmentsUsesCurrentTenant() {
        TenantContextResolver resolver = mock(TenantContextResolver.class);
        when(resolver.current()).thenReturn(Mono.just(new TenantContext(7L, "OPERATOR", "alice")));
        BiSubscriptionAdminService service = mock(BiSubscriptionAdminService.class);
        BiDeliveryRuntimeService runtimeService = mock(BiDeliveryRuntimeService.class);
        BiDeliveryAttachmentService attachmentService = mock(BiDeliveryAttachmentService.class);
        @SuppressWarnings("unchecked")
        ObjectProvider<BiDeliverySchedulerService> schedulerProvider = mock(ObjectProvider.class);
        @SuppressWarnings("unchecked")
        ObjectProvider<BiDeliveryAttachmentService> attachmentProvider = mock(ObjectProvider.class);
        when(attachmentProvider.getIfAvailable()).thenReturn(attachmentService);
        when(attachmentService.listAttachments(7L, "SUBSCRIPTION", 31L, null, 10))
                .thenReturn(List.of(attachmentView()));
        BiSubscriptionController controller = new BiSubscriptionController(
                resolver,
                service,
                runtimeService,
                schedulerProvider,
                attachmentProvider);

        StepVerifier.create(controller.listDeliveryAttachments("SUBSCRIPTION", 31L, null, 10))
                .assertNext(response -> assertThat(response.getData()).singleElement()
                        .satisfies(attachment -> assertThat(attachment.fileUrl()).contains("/download")))
                .verifyComplete();

        verify(attachmentService).listAttachments(7L, "SUBSCRIPTION", 31L, null, 10);
    }

    @Test
    void downloadDeliveryAttachmentUsesCurrentTenant() {
        TenantContextResolver resolver = mock(TenantContextResolver.class);
        when(resolver.current()).thenReturn(Mono.just(new TenantContext(7L, "OPERATOR", "alice")));
        BiSubscriptionAdminService service = mock(BiSubscriptionAdminService.class);
        BiDeliveryRuntimeService runtimeService = mock(BiDeliveryRuntimeService.class);
        BiDeliveryAttachmentService attachmentService = mock(BiDeliveryAttachmentService.class);
        @SuppressWarnings("unchecked")
        ObjectProvider<BiDeliverySchedulerService> schedulerProvider = mock(ObjectProvider.class);
        @SuppressWarnings("unchecked")
        ObjectProvider<BiDeliveryAttachmentService> attachmentProvider = mock(ObjectProvider.class);
        when(attachmentProvider.getIfAvailable()).thenReturn(attachmentService);
        when(attachmentService.download(7L, 71L))
                .thenReturn(new BiDeliveryAttachmentDownload("snapshot.html", "text/html; charset=UTF-8", "<html></html>".getBytes()));
        BiSubscriptionController controller = new BiSubscriptionController(
                resolver,
                service,
                runtimeService,
                schedulerProvider,
                attachmentProvider);

        StepVerifier.create(controller.downloadDeliveryAttachment(71L))
                .assertNext(response -> {
                    assertThat(response.getHeaders().getContentType().toString()).isEqualTo("text/html;charset=UTF-8");
                    assertThat(new String(response.getBody())).contains("html");
                })
                .verifyComplete();

        verify(attachmentService).download(7L, 71L);
    }

    @Test
    void cleanupDeliveryAttachmentsUsesCurrentTenant() {
        TenantContextResolver resolver = mock(TenantContextResolver.class);
        when(resolver.current()).thenReturn(Mono.just(new TenantContext(7L, "OPERATOR", "alice")));
        BiSubscriptionAdminService service = mock(BiSubscriptionAdminService.class);
        BiDeliveryRuntimeService runtimeService = mock(BiDeliveryRuntimeService.class);
        BiDeliveryAttachmentService attachmentService = mock(BiDeliveryAttachmentService.class);
        @SuppressWarnings("unchecked")
        ObjectProvider<BiDeliverySchedulerService> schedulerProvider = mock(ObjectProvider.class);
        @SuppressWarnings("unchecked")
        ObjectProvider<BiDeliveryAttachmentService> attachmentProvider = mock(ObjectProvider.class);
        when(attachmentProvider.getIfAvailable()).thenReturn(attachmentService);
        when(attachmentService.cleanupExpiredAttachments(7L, 50))
                .thenReturn(new BiDeliveryAttachmentCleanupResult(3, 2, 2, 1));
        BiSubscriptionController controller = new BiSubscriptionController(
                resolver,
                service,
                runtimeService,
                schedulerProvider,
                attachmentProvider);

        StepVerifier.create(controller.cleanupDeliveryAttachments(50))
                .assertNext(response -> {
                    assertThat(response.getData().checked()).isEqualTo(3);
                    assertThat(response.getData().failed()).isEqualTo(1);
                })
                .verifyComplete();

        verify(attachmentService).cleanupExpiredAttachments(7L, 50);
    }

    private BiSubscriptionCommand subscriptionCommand() {
        return new BiSubscriptionCommand(
                "canvas-daily",
                "Canvas Daily",
                "DASHBOARD",
                "canvas-effect",
                null,
                Map.of("frequency", "DAILY"),
                Map.of("channels", List.of("EMAIL")),
                Map.of("content", "SNAPSHOT_LINK"),
                true);
    }

    private BiSubscriptionView subscriptionView() {
        return new BiSubscriptionView(
                31L,
                7L,
                5L,
                "canvas-daily",
                "Canvas Daily",
                "DASHBOARD",
                "canvas-effect",
                21L,
                Map.of("frequency", "DAILY"),
                Map.of("channels", List.of("EMAIL")),
                Map.of("content", "SNAPSHOT_LINK"),
                true,
                "alice",
                null,
                null);
    }

    private BiAlertRuleCommand alertCommand() {
        return new BiAlertRuleCommand(
                "success-rate-alert",
                "Success Rate Alert",
                "canvas_daily_stats",
                "success_rate",
                Map.of("operator", "LT", "threshold", 0.9),
                Map.of("channels", List.of("LARK")),
                true);
    }

    private BiAlertRuleView alertView() {
        return new BiAlertRuleView(
                41L,
                7L,
                5L,
                "success-rate-alert",
                "Success Rate Alert",
                "canvas_daily_stats",
                11L,
                "success_rate",
                Map.of("operator", "LT", "threshold", 0.9),
                Map.of("channels", List.of("LARK")),
                true,
                "alice",
                null,
                null);
    }

    private BiDeliveryAttachmentView attachmentView() {
        return new BiDeliveryAttachmentView(
                71L,
                7L,
                5L,
                "SUBSCRIPTION",
                31L,
                "canvas-daily",
                null,
                "DASHBOARD",
                21L,
                "canvas-daily-html",
                "HTML",
                "canvas-daily.html",
                "text/html; charset=UTF-8",
                "/canvas/bi/delivery-attachments/71/download",
                null,
                null,
                512L,
                7,
                null,
                0,
                null,
                "COMPLETED",
                null,
                "alice",
                null,
                null);
    }
}
