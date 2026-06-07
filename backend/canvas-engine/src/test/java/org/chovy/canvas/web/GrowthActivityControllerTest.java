package org.chovy.canvas.web;

import org.chovy.canvas.common.tenant.RoleNames;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.marketing.GrowthActivityCommand;
import org.chovy.canvas.domain.marketing.GrowthActivityReadinessCheckView;
import org.chovy.canvas.domain.marketing.GrowthActivityReadinessService;
import org.chovy.canvas.domain.marketing.GrowthActivityReadinessView;
import org.chovy.canvas.domain.marketing.GrowthActivityReportService;
import org.chovy.canvas.domain.marketing.GrowthActivityReportView;
import org.chovy.canvas.domain.marketing.GrowthActivityService;
import org.chovy.canvas.domain.marketing.GrowthActivityView;
import org.chovy.canvas.domain.marketing.GrowthReferralCodeView;
import org.chovy.canvas.domain.marketing.GrowthReferralQualificationCommand;
import org.chovy.canvas.domain.marketing.GrowthReferralRelationCommand;
import org.chovy.canvas.domain.marketing.GrowthReferralRelationView;
import org.chovy.canvas.domain.marketing.GrowthReferralService;
import org.chovy.canvas.domain.marketing.GrowthRewardPoolCommand;
import org.chovy.canvas.domain.marketing.GrowthRewardPoolService;
import org.chovy.canvas.domain.marketing.GrowthRewardPoolView;
import org.chovy.canvas.domain.marketing.GrowthRewardGrantCommand;
import org.chovy.canvas.domain.marketing.GrowthRewardGrantService;
import org.chovy.canvas.domain.marketing.GrowthRewardGrantView;
import org.chovy.canvas.domain.marketing.GrowthTaskDefinitionCommand;
import org.chovy.canvas.domain.marketing.GrowthTaskDefinitionView;
import org.chovy.canvas.domain.marketing.GrowthTaskProgressCommand;
import org.chovy.canvas.domain.marketing.GrowthTaskProgressView;
import org.chovy.canvas.domain.marketing.GrowthTaskService;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GrowthActivityControllerTest {

    @Test
    void upsertActivityUsesCurrentTenantAndOperator() {
        GrowthActivityService service = mock(GrowthActivityService.class);
        GrowthActivityReadinessService readinessService = mock(GrowthActivityReadinessService.class);
        GrowthActivityReportService reportService = mock(GrowthActivityReportService.class);
        GrowthActivityCommand command = command();
        when(service.upsertActivity(7L, command, "operator-1")).thenReturn(view("DRAFT"));
        GrowthActivityController controller = new GrowthActivityController(service, readinessService, reportService, resolver());

        StepVerifier.create(controller.upsertActivity(command))
                .assertNext(response -> {
                    assertThat(response.getCode()).isZero();
                    assertThat(response.getData().activityKey()).isEqualTo("invite-spring");
                })
                .verifyComplete();

        verify(service).upsertActivity(7L, command, "operator-1");
    }

    @Test
    void listAndDetailUseCurrentTenant() {
        GrowthActivityService service = mock(GrowthActivityService.class);
        GrowthActivityReadinessService readinessService = mock(GrowthActivityReadinessService.class);
        GrowthActivityReportService reportService = mock(GrowthActivityReportService.class);
        when(service.listActivities(7L, "REFERRAL_INVITE", "ACTIVE", 20)).thenReturn(List.of(view("ACTIVE")));
        when(service.getActivity(7L, 10L)).thenReturn(view("ACTIVE"));
        GrowthActivityController controller = new GrowthActivityController(service, readinessService, reportService, resolver());

        StepVerifier.create(controller.listActivities("REFERRAL_INVITE", "ACTIVE", 20))
                .assertNext(response -> assertThat(response.getData()).singleElement()
                        .satisfies(row -> assertThat(row.id()).isEqualTo(10L)))
                .verifyComplete();
        StepVerifier.create(controller.getActivity(10L))
                .assertNext(response -> assertThat(response.getData().status()).isEqualTo("ACTIVE"))
                .verifyComplete();

        verify(service).listActivities(7L, "REFERRAL_INVITE", "ACTIVE", 20);
        verify(service).getActivity(7L, 10L);
    }

    @Test
    void lifecycleEndpointsUseCurrentTenantAndOperator() {
        GrowthActivityService service = mock(GrowthActivityService.class);
        GrowthActivityReadinessService readinessService = mock(GrowthActivityReadinessService.class);
        GrowthActivityReportService reportService = mock(GrowthActivityReportService.class);
        when(service.publishActivity(7L, 10L, "operator-1")).thenReturn(view("ACTIVE"));
        when(service.pauseActivity(7L, 10L, "operator-1")).thenReturn(view("PAUSED"));
        when(service.closeActivity(7L, 10L, "operator-1")).thenReturn(view("CLOSED"));
        GrowthActivityController controller = new GrowthActivityController(service, readinessService, reportService, resolver());

        StepVerifier.create(controller.publishActivity(10L))
                .assertNext(response -> assertThat(response.getData().status()).isEqualTo("ACTIVE"))
                .verifyComplete();
        StepVerifier.create(controller.pauseActivity(10L))
                .assertNext(response -> assertThat(response.getData().status()).isEqualTo("PAUSED"))
                .verifyComplete();
        StepVerifier.create(controller.closeActivity(10L))
                .assertNext(response -> assertThat(response.getData().status()).isEqualTo("CLOSED"))
                .verifyComplete();

        verify(service).publishActivity(7L, 10L, "operator-1");
        verify(service).pauseActivity(7L, 10L, "operator-1");
        verify(service).closeActivity(7L, 10L, "operator-1");
    }

    @Test
    void reportEndpointUsesCurrentTenant() {
        GrowthActivityService service = mock(GrowthActivityService.class);
        GrowthActivityReadinessService readinessService = mock(GrowthActivityReadinessService.class);
        GrowthActivityReportService reportService = mock(GrowthActivityReportService.class);
        when(reportService.summarize(7L, 10L)).thenReturn(report());
        GrowthActivityController controller = new GrowthActivityController(service, readinessService, reportService, resolver());

        StepVerifier.create(controller.getReport(10L))
                .assertNext(response -> {
                    assertThat(response.getCode()).isZero();
                    assertThat(response.getData().activityId()).isEqualTo(10L);
                    assertThat(response.getData().conversion().roi()).isEqualByComparingTo("4.0000");
                })
                .verifyComplete();

        verify(reportService).summarize(7L, 10L);
    }

    @Test
    void readinessEndpointUsesCurrentTenant() {
        GrowthActivityService service = mock(GrowthActivityService.class);
        GrowthActivityReadinessService readinessService = mock(GrowthActivityReadinessService.class);
        GrowthActivityReportService reportService = mock(GrowthActivityReportService.class);
        when(readinessService.evaluate(7L, 10L)).thenReturn(readiness());
        GrowthActivityController controller = new GrowthActivityController(service, readinessService, reportService, resolver());

        StepVerifier.create(controller.getReadiness(10L))
                .assertNext(response -> {
                    assertThat(response.getCode()).isZero();
                    assertThat(response.getData().status()).isEqualTo("READY");
                    assertThat(response.getData().productionReady()).isTrue();
                })
                .verifyComplete();

        verify(readinessService).evaluate(7L, 10L);
    }

    @Test
    void rewardPoolEndpointsUseCurrentTenantAndOperator() {
        GrowthActivityService service = mock(GrowthActivityService.class);
        GrowthActivityReadinessService readinessService = mock(GrowthActivityReadinessService.class);
        GrowthActivityReportService reportService = mock(GrowthActivityReportService.class);
        GrowthRewardPoolService rewardPoolService = mock(GrowthRewardPoolService.class);
        GrowthRewardPoolCommand command = rewardPoolCommand();
        when(rewardPoolService.listPools(7L, 10L)).thenReturn(List.of(rewardPool()));
        when(rewardPoolService.upsertPool(7L, 10L, command, "operator-1")).thenReturn(rewardPool());
        GrowthActivityController controller = new GrowthActivityController(
                service, readinessService, reportService, rewardPoolService, resolver());

        StepVerifier.create(controller.listRewardPools(10L))
                .assertNext(response -> assertThat(response.getData()).singleElement()
                        .satisfies(pool -> {
                            assertThat(pool.poolKey()).isEqualTo("coupon-pool");
                            assertThat(pool.budgetAmount()).isEqualByComparingTo("1000.00");
                            assertThat(pool.inventoryLow()).isTrue();
                        }))
                .verifyComplete();
        StepVerifier.create(controller.upsertRewardPool(10L, command))
                .assertNext(response -> assertThat(response.getData().status()).isEqualTo("ACTIVE"))
                .verifyComplete();

        verify(rewardPoolService).listPools(7L, 10L);
        verify(rewardPoolService).upsertPool(7L, 10L, command, "operator-1");
    }

    @Test
    void grantEndpointsUseCurrentTenantAndOperator() {
        GrowthActivityService service = mock(GrowthActivityService.class);
        GrowthActivityReadinessService readinessService = mock(GrowthActivityReadinessService.class);
        GrowthActivityReportService reportService = mock(GrowthActivityReportService.class);
        GrowthRewardPoolService rewardPoolService = mock(GrowthRewardPoolService.class);
        GrowthReferralService referralService = mock(GrowthReferralService.class);
        GrowthTaskService taskService = mock(GrowthTaskService.class);
        GrowthRewardGrantService grantService = mock(GrowthRewardGrantService.class);
        GrowthRewardGrantCommand command = new GrowthRewardGrantCommand(
                20L, 200L, null, 900L, "TASK_COMPLETION", "task:900:completion",
                Map.of("provider", "coupon"), new BigDecimal("5.00"));
        GrowthActivityController.GrantReconcileRequest reconcileRequest = new GrowthActivityController.GrantReconcileRequest(
                "SUCCESS", Map.of("providerGrantId", "pg-1"));
        when(grantService.listGrants(7L, 10L)).thenReturn(List.of(rewardGrant()));
        when(grantService.createGrant(7L, 10L, command, "operator-1")).thenReturn(rewardGrant());
        when(grantService.retryGrant(7L, 300L, "operator-1")).thenReturn(rewardGrant());
        when(grantService.reconcileGrant(7L, 300L, "SUCCESS", Map.of("providerGrantId", "pg-1"), "operator-1"))
                .thenReturn(rewardGrant());
        when(grantService.cancelGrant(7L, 300L, "operator-1")).thenReturn(rewardGrant());
        GrowthActivityController controller = new GrowthActivityController(
                service, readinessService, reportService, rewardPoolService, grantService, referralService, taskService, resolver());

        StepVerifier.create(controller.listGrants(10L))
                .assertNext(response -> assertThat(response.getData()).singleElement()
                        .satisfies(grant -> assertThat(grant.idempotencyKey()).isEqualTo("task:900:completion")))
                .verifyComplete();
        StepVerifier.create(controller.createGrant(10L, command))
                .assertNext(response -> assertThat(response.getData().grantReason()).isEqualTo("TASK_COMPLETION"))
                .verifyComplete();
        StepVerifier.create(controller.retryGrant(10L, 300L))
                .assertNext(response -> assertThat(response.getData().status()).isEqualTo("FAILED"))
                .verifyComplete();
        StepVerifier.create(controller.reconcileGrant(10L, 300L, reconcileRequest))
                .assertNext(response -> assertThat(response.getData().providerResponse()).containsEntry("errorCode", "PROVIDER_TIMEOUT"))
                .verifyComplete();
        StepVerifier.create(controller.cancelGrant(10L, 300L))
                .assertNext(response -> assertThat(response.getData().id()).isEqualTo(300L))
                .verifyComplete();

        verify(grantService).listGrants(7L, 10L);
        verify(grantService).createGrant(7L, 10L, command, "operator-1");
        verify(grantService).retryGrant(7L, 300L, "operator-1");
        verify(grantService).reconcileGrant(7L, 300L, "SUCCESS", Map.of("providerGrantId", "pg-1"), "operator-1");
        verify(grantService).cancelGrant(7L, 300L, "operator-1");
    }

    @Test
    void referralEndpointsUseCurrentTenantAndOperator() {
        GrowthActivityService service = mock(GrowthActivityService.class);
        GrowthActivityReadinessService readinessService = mock(GrowthActivityReadinessService.class);
        GrowthActivityReportService reportService = mock(GrowthActivityReportService.class);
        GrowthRewardPoolService rewardPoolService = mock(GrowthRewardPoolService.class);
        GrowthReferralService referralService = mock(GrowthReferralService.class);
        GrowthReferralRelationCommand relationCommand = new GrowthReferralRelationCommand(
                "G10P200", "invitee-1", Map.of("ipRisk", "LOW"));
        GrowthReferralQualificationCommand qualificationCommand = new GrowthReferralQualificationCommand(
                100L, 101L, Map.of("riskDecision", "PASS"));
        when(referralService.listCodes(7L, 10L)).thenReturn(List.of(referralCode()));
        when(referralService.listRelations(7L, 10L)).thenReturn(List.of(referralRelation()));
        when(referralService.generateCode(7L, 10L, 200L, "operator-1")).thenReturn(referralCode());
        when(referralService.upsertRelation(7L, 10L, relationCommand, "operator-1")).thenReturn(referralRelation());
        when(referralService.qualifyRelation(7L, 700L, qualificationCommand, "operator-1")).thenReturn(referralRelation());
        GrowthActivityController controller = new GrowthActivityController(
                service, readinessService, reportService, rewardPoolService, referralService, resolver());

        StepVerifier.create(controller.listReferralCodes(10L))
                .assertNext(response -> assertThat(response.getData()).singleElement()
                        .satisfies(code -> assertThat(code.code()).isEqualTo("G10P200")))
                .verifyComplete();
        StepVerifier.create(controller.listReferralRelations(10L))
                .assertNext(response -> assertThat(response.getData()).singleElement()
                        .satisfies(relation -> {
                            assertThat(relation.inviteeUserId()).isEqualTo("invitee-1");
                            assertThat(relation.inviterRewardGrantId()).isEqualTo(900L);
                        }))
                .verifyComplete();
        StepVerifier.create(controller.generateReferralCode(10L, new GrowthActivityController.ReferralCodeRequest(200L)))
                .assertNext(response -> assertThat(response.getData().participantId()).isEqualTo(200L))
                .verifyComplete();
        StepVerifier.create(controller.upsertReferralRelation(10L, relationCommand))
                .assertNext(response -> assertThat(response.getData().status()).isEqualTo("QUALIFIED"))
                .verifyComplete();
        StepVerifier.create(controller.qualifyReferral(10L, 700L, qualificationCommand))
                .assertNext(response -> assertThat(response.getData().inviteeRewardGrantId()).isEqualTo(901L))
                .verifyComplete();

        verify(referralService).listCodes(7L, 10L);
        verify(referralService).listRelations(7L, 10L);
        verify(referralService).generateCode(7L, 10L, 200L, "operator-1");
        verify(referralService).upsertRelation(7L, 10L, relationCommand, "operator-1");
        verify(referralService).qualifyRelation(7L, 700L, qualificationCommand, "operator-1");
    }

    @Test
    void taskEndpointsUseCurrentTenantAndOperator() {
        GrowthActivityService service = mock(GrowthActivityService.class);
        GrowthActivityReadinessService readinessService = mock(GrowthActivityReadinessService.class);
        GrowthActivityReportService reportService = mock(GrowthActivityReportService.class);
        GrowthRewardPoolService rewardPoolService = mock(GrowthRewardPoolService.class);
        GrowthReferralService referralService = mock(GrowthReferralService.class);
        GrowthTaskService taskService = mock(GrowthTaskService.class);
        GrowthTaskDefinitionCommand definitionCommand = new GrowthTaskDefinitionCommand(
                "daily-login", "EVENT_COUNT", "EVENT", "DAILY", 20L, new BigDecimal("3"), "ACTIVE",
                Map.of("eventName", "login"));
        GrowthTaskProgressCommand progressCommand = new GrowthTaskProgressCommand(
                800L, 200L, new BigDecimal("1"), "login:2026-06-07", Map.of("source", "app"));
        when(taskService.listTaskDefinitions(7L, 10L)).thenReturn(List.of(taskDefinition()));
        when(taskService.listTaskProgress(7L, 10L)).thenReturn(List.of(taskProgress()));
        when(taskService.upsertTaskDefinition(7L, 10L, definitionCommand, "operator-1")).thenReturn(taskDefinition());
        when(taskService.recordProgress(7L, 10L, progressCommand, "operator-1")).thenReturn(taskProgress());
        when(taskService.resetProgress(7L, 900L, "operator-1")).thenReturn(taskProgress());
        GrowthActivityController controller = new GrowthActivityController(
                service, readinessService, reportService, rewardPoolService, referralService, taskService, resolver());

        StepVerifier.create(controller.listTaskDefinitions(10L))
                .assertNext(response -> assertThat(response.getData()).singleElement()
                        .satisfies(task -> assertThat(task.taskKey()).isEqualTo("daily-login")))
                .verifyComplete();
        StepVerifier.create(controller.listTaskProgress(10L))
                .assertNext(response -> assertThat(response.getData()).singleElement()
                        .satisfies(progress -> assertThat(progress.rewardGrantId()).isEqualTo(902L)))
                .verifyComplete();
        StepVerifier.create(controller.upsertTaskDefinition(10L, definitionCommand))
                .assertNext(response -> assertThat(response.getData().rewardPoolId()).isEqualTo(20L))
                .verifyComplete();
        StepVerifier.create(controller.recordTaskProgress(10L, progressCommand))
                .assertNext(response -> assertThat(response.getData().status()).isEqualTo("COMPLETED"))
                .verifyComplete();
        StepVerifier.create(controller.resetTaskProgress(10L, 900L))
                .assertNext(response -> assertThat(response.getData().participantId()).isEqualTo(200L))
                .verifyComplete();

        verify(taskService).listTaskDefinitions(7L, 10L);
        verify(taskService).listTaskProgress(7L, 10L);
        verify(taskService).upsertTaskDefinition(7L, 10L, definitionCommand, "operator-1");
        verify(taskService).recordProgress(7L, 10L, progressCommand, "operator-1");
        verify(taskService).resetProgress(7L, 900L, "operator-1");
    }

    private static TenantContextResolver resolver() {
        TenantContextResolver resolver = mock(TenantContextResolver.class);
        when(resolver.currentOrError()).thenReturn(Mono.just(new TenantContext(7L, RoleNames.OPERATOR, "operator-1")));
        return resolver;
    }

    private static GrowthActivityCommand command() {
        return new GrowthActivityCommand(
                "invite-spring",
                "Invite spring",
                "REFERRAL_INVITE",
                "DRAFT",
                10L,
                "ACQUISITION",
                "Growth",
                null,
                null,
                "PRIVATE_DOMAIN",
                Map.of(),
                null,
                null,
                null,
                Map.of());
    }

    private static GrowthActivityView view(String status) {
        return new GrowthActivityView(
                10L,
                7L,
                "invite-spring",
                "Invite spring",
                "REFERRAL_INVITE",
                status,
                10L,
                "ACQUISITION",
                "Growth",
                null,
                null,
                "PRIVATE_DOMAIN",
                Map.of(),
                null,
                null,
                null,
                Map.of(),
                "operator-1",
                "operator-1",
                null,
                null);
    }

    private static GrowthActivityReportView report() {
        return new GrowthActivityReportView(
                7L,
                10L,
                new GrowthActivityReportView.ParticipationMetrics(3, 2),
                new GrowthActivityReportView.ReferralMetrics(2, 1, 1, 0),
                new GrowthActivityReportView.GrantMetrics(2, 0, 1, 0, 0, 1, 0, new BigDecimal("25.00")),
                new GrowthActivityReportView.ConversionMetrics(1, new BigDecimal("100.00"), new BigDecimal("4.0000")),
                new GrowthActivityReportView.TaskMetrics(2, 1, new BigDecimal("0.5000")));
    }

    private static GrowthActivityReadinessView readiness() {
        return new GrowthActivityReadinessView(
                7L,
                10L,
                "invite-spring",
                "REFERRAL_INVITE",
                "2026-06-07T15:00:00",
                "READY",
                true,
                0,
                0,
                List.of(),
                List.of(),
                List.of(new GrowthActivityReadinessCheckView(
                        "PASS",
                        "CAMPAIGN_MASTER",
                        "campaign-master",
                        "Campaign master is linked",
                        "ok",
                        null)));
    }

    private static GrowthRewardPoolCommand rewardPoolCommand() {
        return new GrowthRewardPoolCommand(
                "coupon-pool",
                "COUPON",
                "COMMIT_ACTION",
                "spring-coupon",
                null,
                null,
                "coupon-contract",
                "LIMITED",
                100L,
                1,
                1,
                new BigDecimal("1000.00"),
                "CNY",
                "ACTIVE",
                Map.of());
    }

    private static GrowthRewardPoolView rewardPool() {
        return new GrowthRewardPoolView(
                20L,
                7L,
                10L,
                "coupon-pool",
                "COUPON",
                "COMMIT_ACTION",
                "spring-coupon",
                null,
                null,
                "coupon-contract",
                "LIMITED",
                100L,
                10L,
                40L,
                1,
                1,
                new BigDecimal("1000.00"),
                new BigDecimal("150.00"),
                new BigDecimal("400.00"),
                "CNY",
                "ACTIVE",
                true,
                Map.of(),
                "operator-1",
                "operator-1",
                null,
                null);
    }

    private static GrowthRewardGrantView rewardGrant() {
        return new GrowthRewardGrantView(
                300L,
                7L,
                10L,
                20L,
                200L,
                null,
                900L,
                "TASK_COMPLETION",
                "FAILED",
                "task:900:completion",
                Map.of("provider", "coupon"),
                Map.of("provider", "coupon-provider", "providerGrantId", "pg-1", "errorCode", "PROVIDER_TIMEOUT"),
                new BigDecimal("5.00"),
                "operator-1",
                "operator-1",
                null,
                null);
    }

    private static GrowthReferralCodeView referralCode() {
        return new GrowthReferralCodeView(
                500L,
                7L,
                10L,
                200L,
                "G10P200",
                "ACTIVE",
                "operator-1",
                null);
    }

    private static GrowthReferralRelationView referralRelation() {
        return new GrowthReferralRelationView(
                700L,
                7L,
                10L,
                500L,
                200L,
                "invitee-1",
                "QUALIFIED",
                Map.of("riskDecision", "PASS", "ipRisk", "LOW"),
                900L,
                901L,
                "operator-1",
                "operator-1",
                null,
                null);
    }

    private static GrowthTaskDefinitionView taskDefinition() {
        return new GrowthTaskDefinitionView(
                800L,
                7L,
                10L,
                "daily-login",
                "EVENT_COUNT",
                "EVENT",
                "DAILY",
                20L,
                new BigDecimal("3"),
                "ACTIVE",
                Map.of("eventName", "login"),
                "operator-1",
                "operator-1",
                null,
                null);
    }

    private static GrowthTaskProgressView taskProgress() {
        return new GrowthTaskProgressView(
                900L,
                7L,
                10L,
                200L,
                800L,
                new BigDecimal("3"),
                new BigDecimal("3"),
                "COMPLETED",
                "login:2026-06-07",
                Map.of("eventName", "login", "source", "app"),
                902L,
                "operator-1",
                null,
                null);
    }
}
