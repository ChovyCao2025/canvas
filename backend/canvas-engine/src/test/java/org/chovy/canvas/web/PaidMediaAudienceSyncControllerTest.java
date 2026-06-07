package org.chovy.canvas.web;

import org.chovy.canvas.common.tenant.RoleNames;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.paidmedia.PaidMediaAudienceDestinationView;
import org.chovy.canvas.domain.paidmedia.PaidMediaAudienceMemberQuery;
import org.chovy.canvas.domain.paidmedia.PaidMediaAudienceMemberView;
import org.chovy.canvas.domain.paidmedia.PaidMediaAudienceRunQuery;
import org.chovy.canvas.domain.paidmedia.PaidMediaAudienceSyncCommand;
import org.chovy.canvas.domain.paidmedia.PaidMediaAudienceSyncRunView;
import org.chovy.canvas.domain.paidmedia.PaidMediaAudienceSyncService;
import org.chovy.canvas.domain.paidmedia.PaidMediaDestinationCommand;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PaidMediaAudienceSyncControllerTest {

    @Test
    void upsertDestinationPassesCurrentTenantAndOperator() {
        PaidMediaAudienceSyncService service = mock(PaidMediaAudienceSyncService.class);
        PaidMediaDestinationCommand command = destinationCommand();
        when(service.upsertDestination(7L, command, "operator-1")).thenReturn(destinationView());
        PaidMediaAudienceSyncController controller = new PaidMediaAudienceSyncController(service, resolver());

        StepVerifier.create(controller.upsertDestination(command))
                .assertNext(response -> {
                    assertThat(response.getCode()).isZero();
                    assertThat(response.getData().provider()).isEqualTo("META");
                })
                .verifyComplete();

        verify(service).upsertDestination(7L, command, "operator-1");
    }

    @Test
    void syncAudiencePassesCurrentTenantAndOperator() {
        PaidMediaAudienceSyncService service = mock(PaidMediaAudienceSyncService.class);
        PaidMediaAudienceSyncCommand command = syncCommand();
        when(service.syncAudience(7L, command, "operator-1")).thenReturn(runView());
        PaidMediaAudienceSyncController controller = new PaidMediaAudienceSyncController(service, resolver());

        StepVerifier.create(controller.syncAudience(command))
                .assertNext(response -> assertThat(response.getData().id()).isEqualTo(900L))
                .verifyComplete();

        verify(service).syncAudience(7L, command, "operator-1");
    }

    @Test
    void runsEndpointPassesFiltersAndBoundedLimit() {
        PaidMediaAudienceSyncService service = mock(PaidMediaAudienceSyncService.class);
        PaidMediaAudienceRunQuery query = new PaidMediaAudienceRunQuery(10L, 20L, "SUCCESS", 100);
        when(service.runs(7L, query)).thenReturn(List.of(runView()));
        PaidMediaAudienceSyncController controller = new PaidMediaAudienceSyncController(service, resolver());

        StepVerifier.create(controller.runs(10L, 20L, "SUCCESS", 500))
                .assertNext(response -> assertThat(response.getData()).singleElement()
                        .satisfies(run -> assertThat(run.id()).isEqualTo(900L)))
                .verifyComplete();

        verify(service).runs(7L, query);
    }

    @Test
    void membersEndpointPassesRunStatusAndLowerBoundedLimit() {
        PaidMediaAudienceSyncService service = mock(PaidMediaAudienceSyncService.class);
        PaidMediaAudienceMemberQuery query = new PaidMediaAudienceMemberQuery(900L, "ELIGIBLE", 1);
        when(service.members(7L, query)).thenReturn(List.of(memberView()));
        PaidMediaAudienceSyncController controller = new PaidMediaAudienceSyncController(service, resolver());

        StepVerifier.create(controller.members(900L, "ELIGIBLE", 0))
                .assertNext(response -> assertThat(response.getData()).singleElement()
                        .satisfies(member -> assertThat(member.userId()).isEqualTo("u1")))
                .verifyComplete();

        verify(service).members(7L, query);
    }

    private TenantContextResolver resolver() {
        TenantContextResolver resolver = mock(TenantContextResolver.class);
        when(resolver.currentOrError()).thenReturn(Mono.just(new TenantContext(7L, RoleNames.OPERATOR, "operator-1")));
        return resolver;
    }

    private PaidMediaDestinationCommand destinationCommand() {
        return new PaidMediaDestinationCommand(
                "META",
                "vip-meta",
                "VIP Meta Audience",
                "act_123",
                "aud_456",
                List.of("EMAIL", "PHONE"),
                "PAID_MEDIA",
                true,
                true,
                Map.of());
    }

    private PaidMediaAudienceSyncCommand syncCommand() {
        return new PaidMediaAudienceSyncCommand(
                10L,
                20L,
                List.of("u1"),
                "sandbox-upload-1",
                Map.of());
    }

    private PaidMediaAudienceDestinationView destinationView() {
        return new PaidMediaAudienceDestinationView(
                10L,
                7L,
                "META",
                "vip-meta",
                "VIP Meta Audience",
                "act_123",
                "aud_456",
                List.of("EMAIL", "PHONE"),
                "PAID_MEDIA",
                true,
                true,
                Map.<String, Object>of(),
                "operator-1",
                now(),
                now());
    }

    private PaidMediaAudienceSyncRunView runView() {
        return new PaidMediaAudienceSyncRunView(
                900L,
                7L,
                10L,
                20L,
                "META",
                "SUCCESS",
                1,
                1,
                0,
                0,
                "sandbox-upload-1",
                null,
                Map.<String, Object>of(),
                "operator-1",
                now(),
                now());
    }

    private PaidMediaAudienceMemberView memberView() {
        return new PaidMediaAudienceMemberView(
                100L,
                7L,
                900L,
                10L,
                20L,
                "META",
                "u1",
                "EMAIL",
                "hash-1",
                "ELIGIBLE",
                null,
                now());
    }

    private LocalDateTime now() {
        return LocalDateTime.of(2026, 6, 6, 0, 0);
    }
}
