package org.chovy.canvas.web.bi;

import org.chovy.canvas.common.tenant.RoleNames;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.bi.permission.BiColumnPermissionCommand;
import org.chovy.canvas.domain.bi.permission.BiColumnPermissionView;
import org.chovy.canvas.domain.bi.permission.BiPermissionAdminService;
import org.chovy.canvas.domain.bi.permission.BiResourcePermissionCommand;
import org.chovy.canvas.domain.bi.permission.BiResourcePermissionView;
import org.chovy.canvas.domain.bi.permission.BiRowPermissionCommand;
import org.chovy.canvas.domain.bi.permission.BiRowPermissionView;
import org.chovy.canvas.domain.bi.query.BiFilter;
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

class BiPermissionControllerTest {

    @Test
    void upsertResourcePermissionUsesCurrentTenantAndUser() {
        TenantContextResolver resolver = resolver();
        BiPermissionAdminService service = mock(BiPermissionAdminService.class);
        BiResourcePermissionCommand command = new BiResourcePermissionCommand(
                "DATASET", "canvas_daily_stats", null, "ROLE", RoleNames.OPERATOR, "USE", "ALLOW");
        when(service.upsertResourcePermission(7L, "alice", command)).thenReturn(resourceView());
        BiPermissionController controller = new BiPermissionController(resolver, service);

        StepVerifier.create(controller.upsertResourcePermission(command))
                .assertNext(response -> assertThat(response.getData().resourceKey()).isEqualTo("canvas_daily_stats"))
                .verifyComplete();

        verify(service).upsertResourcePermission(7L, "alice", command);
    }

    @Test
    void listRowPermissionsUsesCurrentTenant() {
        TenantContextResolver resolver = resolver();
        BiPermissionAdminService service = mock(BiPermissionAdminService.class);
        when(service.listRowPermissions(7L, "canvas_daily_stats")).thenReturn(List.of(rowView()));
        BiPermissionController controller = new BiPermissionController(resolver, service);

        StepVerifier.create(controller.listRowPermissions("canvas_daily_stats"))
                .assertNext(response -> assertThat(response.getData()).singleElement()
                        .satisfies(row -> assertThat(row.ruleKey()).isEqualTo("operator-canvas")))
                .verifyComplete();
    }

    @Test
    void upsertColumnPermissionUsesCurrentTenant() {
        TenantContextResolver resolver = resolver();
        BiPermissionAdminService service = mock(BiPermissionAdminService.class);
        BiColumnPermissionCommand command = new BiColumnPermissionCommand(
                "canvas_daily_stats",
                "canvas_name",
                "ROLE",
                RoleNames.OPERATOR,
                "MASK",
                Map.of("strategy", "FIXED", "replacement", "MASKED"),
                true);
        when(service.upsertColumnPermission(7L, command)).thenReturn(columnView());
        BiPermissionController controller = new BiPermissionController(resolver, service);

        StepVerifier.create(controller.upsertColumnPermission(command))
                .assertNext(response -> assertThat(response.getData().policy()).isEqualTo("MASK"))
                .verifyComplete();
    }

    @Test
    void deleteResourcePermissionUsesCurrentTenant() {
        TenantContextResolver resolver = resolver();
        BiPermissionAdminService service = mock(BiPermissionAdminService.class);
        BiPermissionController controller = new BiPermissionController(resolver, service);

        StepVerifier.create(controller.deleteResourcePermission(99L))
                .assertNext(response -> assertThat(response.getCode()).isEqualTo(0))
                .verifyComplete();

        verify(service).deleteResourcePermission(7L, 99L);
    }

    private TenantContextResolver resolver() {
        TenantContextResolver resolver = mock(TenantContextResolver.class);
        when(resolver.current()).thenReturn(Mono.just(new TenantContext(7L, RoleNames.TENANT_ADMIN, "alice")));
        return resolver;
    }

    private BiResourcePermissionView resourceView() {
        return new BiResourcePermissionView(
                99L,
                7L,
                3L,
                "DATASET",
                "canvas_daily_stats",
                11L,
                "ROLE",
                RoleNames.OPERATOR,
                "USE",
                "ALLOW",
                "alice",
                LocalDateTime.parse("2026-06-05T04:30:00"));
    }

    private BiRowPermissionView rowView() {
        return new BiRowPermissionView(
                21L,
                7L,
                "canvas_daily_stats",
                11L,
                "operator-canvas",
                "ROLE",
                RoleNames.OPERATOR,
                "{\"filters\":[]}",
                true,
                LocalDateTime.parse("2026-06-05T04:30:00"));
    }

    private BiColumnPermissionView columnView() {
        return new BiColumnPermissionView(
                31L,
                7L,
                "canvas_daily_stats",
                11L,
                "canvas_name",
                "ROLE",
                RoleNames.OPERATOR,
                "MASK",
                "{\"strategy\":\"FIXED\"}",
                true,
                LocalDateTime.parse("2026-06-05T04:30:00"));
    }
}
