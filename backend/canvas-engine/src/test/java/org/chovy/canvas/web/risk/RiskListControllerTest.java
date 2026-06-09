package org.chovy.canvas.web.risk;

import org.chovy.canvas.common.tenant.RoleNames;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.risk.dsl.RiskSubjectType;
import org.chovy.canvas.domain.risk.governance.RiskListCommand;
import org.chovy.canvas.domain.risk.governance.RiskListEntryCommand;
import org.chovy.canvas.domain.risk.governance.RiskListImportCommand;
import org.chovy.canvas.domain.risk.governance.RiskListService;
import org.chovy.canvas.domain.risk.runtime.RiskListType;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RiskListControllerTest {

    private final RiskListService service = new RiskListService(new RecordingAuditSink(),
            raw -> "hash:" + raw,
            Clock.fixed(Instant.parse("2026-06-08T10:00:00Z"), ZoneOffset.UTC));
    private final TenantContextResolver tenantResolver = mock(TenantContextResolver.class);
    private final RiskListController controller = new RiskListController(service, tenantResolver);

    @Test
    void createsTenantScopedList() {
        tenant(RoleNames.OPERATOR, "alice");

        StepVerifier.create(controller.createList(command()).map(response -> response.getData()))
                .assertNext(view -> {
                    assertThat(view.tenantId()).isEqualTo(7L);
                    assertThat(view.listKey()).isEqualTo("blacklist.user");
                })
                .verifyComplete();
    }

    @Test
    void listsListsEntriesAndRemovesEntriesForWorkbenchReads() {
        tenant(RoleNames.OPERATOR, "alice");
        controller.createList(command()).block();
        long entryId = controller.addEntry("blacklist.user", entry("user-secret-123"))
                .map(response -> response.getData().id())
                .block();

        StepVerifier.create(controller.listLists().map(response -> response.getData()))
                .assertNext(lists -> {
                    assertThat(lists).hasSize(1);
                    assertThat(lists.getFirst().listKey()).isEqualTo("blacklist.user");
                })
                .verifyComplete();

        StepVerifier.create(controller.listEntries("blacklist.user").map(response -> response.getData()))
                .assertNext(entries -> {
                    assertThat(entries).hasSize(1);
                    assertThat(entries.getFirst().id()).isEqualTo(entryId);
                    assertThat(entries.getFirst().subjectMasked()).doesNotContain("user-secret-123");
                })
                .verifyComplete();

        StepVerifier.create(controller.removeEntry("blacklist.user", entryId))
                .assertNext(response -> assertThat(response.getCode()).isZero())
                .verifyComplete();

        StepVerifier.create(controller.listEntries("blacklist.user").map(response -> response.getData()))
                .assertNext(entries -> assertThat(entries).isEmpty())
                .verifyComplete();
    }

    @Test
    void preventsRawPiiInResponseBody() {
        tenant(RoleNames.OPERATOR, "alice");
        controller.createList(command()).block();

        StepVerifier.create(controller.addEntry("blacklist.user", entry("user-secret-123"))
                        .map(response -> response.getData()))
                .assertNext(entry -> assertThat(entry.toString()).doesNotContain("user-secret-123"))
                .verifyComplete();
    }

    @Test
    void importsEntriesAtomicallyWithRowLevelErrors() {
        tenant(RoleNames.TENANT_ADMIN, "alice");
        controller.createList(command()).block();

        StepVerifier.create(controller.importEntries("blacklist.user", new RiskListImportCommand(List.of(
                                entry("user-1"),
                                entry("device-1").withSubjectType(RiskSubjectType.DEVICE_ID))))
                        .map(response -> response.getData()))
                .assertNext(result -> {
                    assertThat(result.totalRows()).isEqualTo(2);
                    assertThat(result.acceptedRows()).isEqualTo(1);
                    assertThat(result.rejectedRows()).isEqualTo(1);
                    assertThat(result.rowErrors()).containsExactly("row 2: subject type mismatch");
                })
                .verifyComplete();
    }

    @Test
    void requiresListImportPermissionForBulkImport() {
        tenant(RoleNames.OPERATOR, "alice");

        StepVerifier.create(controller.importEntries("blacklist.user", new RiskListImportCommand(List.of())))
                .expectError(AccessDeniedException.class)
                .verify();
    }

    private RiskListCommand command() {
        return new RiskListCommand("blacklist.user", RiskListType.BLACK, RiskSubjectType.USER_ID, false);
    }

    private RiskListEntryCommand entry(String rawSubject) {
        return new RiskListEntryCommand(rawSubject, RiskSubjectType.USER_ID, "manual", "operator",
                Instant.parse("2026-06-01T00:00:00Z"), null);
    }

    private void tenant(String role, String username) {
        when(tenantResolver.current()).thenReturn(Mono.just(new TenantContext(7L, role, username)));
    }

    private static final class RecordingAuditSink implements RiskListAuditSink {
        private final List<String> events = new ArrayList<>();

        @Override
        public String record(Long tenantId, String eventType, String resourceKey, String resourceId, String actor) {
            events.add(eventType + ":" + resourceKey + ":" + resourceId + ":" + actor);
            return "audit-" + events.size();
        }
    }
}
