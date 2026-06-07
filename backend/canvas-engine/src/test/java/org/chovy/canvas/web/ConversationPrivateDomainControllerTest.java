package org.chovy.canvas.web;

import org.chovy.canvas.common.tenant.RoleNames;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.conversation.ConversationPrivateDomainSyncService;
import org.chovy.canvas.domain.conversation.PrivateDomainContactQuery;
import org.chovy.canvas.domain.conversation.PrivateDomainContactSnapshot;
import org.chovy.canvas.domain.conversation.PrivateDomainContactView;
import org.chovy.canvas.domain.conversation.PrivateDomainGroupQuery;
import org.chovy.canvas.domain.conversation.PrivateDomainGroupView;
import org.chovy.canvas.domain.conversation.PrivateDomainSyncCommand;
import org.chovy.canvas.domain.conversation.PrivateDomainSyncRunView;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.PostMapping;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ConversationPrivateDomainControllerTest {

    @Test
    void ingestEndpointUsesDocumentedSyncRunsPath() throws Exception {
        Method method = ConversationPrivateDomainController.class.getMethod("ingest", PrivateDomainSyncCommand.class);

        assertThat(Arrays.asList(method.getAnnotation(PostMapping.class).value()))
                .contains("/sync-runs");
    }

    @Test
    void ingestSnapshotPassesCurrentTenantAndOperator() {
        ConversationPrivateDomainSyncService service = mock(ConversationPrivateDomainSyncService.class);
        PrivateDomainSyncCommand command = command();
        when(service.ingestSnapshot(7L, command, "operator-1")).thenReturn(run());
        ConversationPrivateDomainController controller = new ConversationPrivateDomainController(service, resolver());

        StepVerifier.create(controller.ingest(command))
                .assertNext(response -> {
                    assertThat(response.getCode()).isZero();
                    assertThat(response.getData().provider()).isEqualTo("WECOM");
                })
                .verifyComplete();

        verify(service).ingestSnapshot(7L, command, "operator-1");
    }

    @Test
    void contactsEndpointPassesFiltersAndBoundedLimit() {
        ConversationPrivateDomainSyncService service = mock(ConversationPrivateDomainSyncService.class);
        PrivateDomainContactQuery query = new PrivateDomainContactQuery("WECOM", "sales-1", "Alice", 100);
        when(service.contacts(7L, query)).thenReturn(List.of(contact()));
        ConversationPrivateDomainController controller = new ConversationPrivateDomainController(service, resolver());

        StepVerifier.create(controller.contacts("WECOM", "sales-1", "Alice", 500))
                .assertNext(response -> assertThat(response.getData()).singleElement()
                        .satisfies(contact -> assertThat(contact.externalContactId()).isEqualTo("wm-001")))
                .verifyComplete();

        verify(service).contacts(7L, query);
    }

    @Test
    void groupsEndpointPassesFiltersAndLowerBoundedLimit() {
        ConversationPrivateDomainSyncService service = mock(ConversationPrivateDomainSyncService.class);
        PrivateDomainGroupQuery query = new PrivateDomainGroupQuery("WECOM", "sales-1", 1);
        when(service.groups(7L, query)).thenReturn(List.of(group()));
        ConversationPrivateDomainController controller = new ConversationPrivateDomainController(service, resolver());

        StepVerifier.create(controller.groups("WECOM", "sales-1", 0))
                .assertNext(response -> assertThat(response.getData()).singleElement()
                        .satisfies(group -> assertThat(group.externalGroupId()).isEqualTo("wr-001")))
                .verifyComplete();

        verify(service).groups(7L, query);
    }

    @Test
    void syncRunsEndpointPassesProviderAndBoundedLimit() {
        ConversationPrivateDomainSyncService service = mock(ConversationPrivateDomainSyncService.class);
        when(service.syncRuns(7L, "WECOM", 100)).thenReturn(List.of(run()));
        ConversationPrivateDomainController controller = new ConversationPrivateDomainController(service, resolver());

        StepVerifier.create(controller.syncRuns("WECOM", 500))
                .assertNext(response -> assertThat(response.getData()).singleElement()
                        .satisfies(run -> assertThat(run.id()).isEqualTo(900L)))
                .verifyComplete();

        verify(service).syncRuns(7L, "WECOM", 100);
    }

    private TenantContextResolver resolver() {
        TenantContextResolver resolver = mock(TenantContextResolver.class);
        when(resolver.currentOrError()).thenReturn(Mono.just(new TenantContext(7L, RoleNames.OPERATOR, "operator-1")));
        return resolver;
    }

    private PrivateDomainSyncCommand command() {
        return new PrivateDomainSyncCommand(
                "WECOM",
                "CONTACTS",
                "cursor-1",
                "cursor-2",
                List.of(new PrivateDomainContactSnapshot(
                        "wm-001",
                        "Alice Zhang",
                        null,
                        null,
                        null,
                        null,
                        "sales-1",
                        null,
                        null,
                        null,
                        List.of("vip"),
                        Map.of(),
                        Map.of())),
                List.of(),
                Map.of());
    }

    private PrivateDomainContactView contact() {
        return new PrivateDomainContactView(
                100L,
                7L,
                "WECOM",
                "wm-001",
                "WECOM:wm-001",
                "Alice Zhang",
                "sales-1",
                "VIP",
                null,
                null,
                List.of("vip"),
                Map.of(),
                now());
    }

    private PrivateDomainGroupView group() {
        return new PrivateDomainGroupView(
                200L,
                7L,
                "WECOM",
                "wr-001",
                "VIP Leads",
                "sales-1",
                "ACTIVE",
                1,
                now(),
                List.of(),
                now());
    }

    private PrivateDomainSyncRunView run() {
        return new PrivateDomainSyncRunView(
                900L,
                7L,
                "WECOM",
                "CONTACTS",
                "SUCCESS",
                "operator-1",
                "cursor-1",
                "cursor-2",
                1,
                1,
                0,
                0,
                0,
                0,
                0,
                null,
                Map.of(),
                now(),
                now());
    }

    private LocalDateTime now() {
        return LocalDateTime.of(2026, 6, 6, 11, 0);
    }
}
