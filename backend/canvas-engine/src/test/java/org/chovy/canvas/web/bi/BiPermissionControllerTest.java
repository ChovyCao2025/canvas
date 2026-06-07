package org.chovy.canvas.web.bi;

import org.chovy.canvas.common.tenant.RoleNames;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.bi.permission.BiColumnPermissionCommand;
import org.chovy.canvas.domain.bi.permission.BiColumnPermissionView;
import org.chovy.canvas.domain.bi.permission.BiPermissionAuditEntry;
import org.chovy.canvas.domain.bi.permission.BiPermissionAdminService;
import org.chovy.canvas.domain.bi.permission.BiPermissionRequestCommand;
import org.chovy.canvas.domain.bi.permission.BiPermissionRequestReviewCommand;
import org.chovy.canvas.domain.bi.permission.BiPermissionRequestService;
import org.chovy.canvas.domain.bi.permission.BiPermissionRequestView;
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
    void upsertRowPermissionUsesCurrentTenantAndUser() {
        TenantContextResolver resolver = resolver();
        BiPermissionAdminService service = mock(BiPermissionAdminService.class);
        BiRowPermissionCommand command = new BiRowPermissionCommand(
                "canvas_daily_stats",
                "operator-canvas",
                "ROLE",
                RoleNames.OPERATOR,
                List.of(new BiFilter("canvas_id", BiFilter.Operator.IN, List.of(12, 13))),
                Map.of(),
                true);
        when(service.upsertRowPermission(7L, "alice", command)).thenReturn(rowView());
        BiPermissionController controller = new BiPermissionController(resolver, service);

        StepVerifier.create(controller.upsertRowPermission(command))
                .assertNext(response -> assertThat(response.getData().ruleKey()).isEqualTo("operator-canvas"))
                .verifyComplete();

        verify(service).upsertRowPermission(7L, "alice", command);
    }

    @Test
    void upsertColumnPermissionUsesCurrentTenantAndUser() {
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
        when(service.upsertColumnPermission(7L, "alice", command)).thenReturn(columnView());
        BiPermissionController controller = new BiPermissionController(resolver, service);

        StepVerifier.create(controller.upsertColumnPermission(command))
                .assertNext(response -> assertThat(response.getData().policy()).isEqualTo("MASK"))
                .verifyComplete();

        verify(service).upsertColumnPermission(7L, "alice", command);
    }

    @Test
    void deleteResourcePermissionUsesCurrentTenant() {
        TenantContextResolver resolver = resolver();
        BiPermissionAdminService service = mock(BiPermissionAdminService.class);
        BiPermissionController controller = new BiPermissionController(resolver, service);

        StepVerifier.create(controller.deleteResourcePermission(99L))
                .assertNext(response -> assertThat(response.getCode()).isEqualTo(0))
                .verifyComplete();

        verify(service).deleteResourcePermission(7L, "alice", 99L);
    }

    @Test
    void deleteRowAndColumnPermissionsUseCurrentTenantAndUser() {
        TenantContextResolver resolver = resolver();
        BiPermissionAdminService service = mock(BiPermissionAdminService.class);
        BiPermissionController controller = new BiPermissionController(resolver, service);

        StepVerifier.create(controller.deleteRowPermission(21L))
                .assertNext(response -> assertThat(response.getCode()).isEqualTo(0))
                .verifyComplete();
        StepVerifier.create(controller.deleteColumnPermission(31L))
                .assertNext(response -> assertThat(response.getCode()).isEqualTo(0))
                .verifyComplete();

        verify(service).deleteRowPermission(7L, "alice", 21L);
        verify(service).deleteColumnPermission(7L, "alice", 31L);
    }

    @Test
    void exposesTenantScopedPermissionAudit() {
        TenantContextResolver resolver = resolver();
        BiPermissionAdminService service = mock(BiPermissionAdminService.class);
        when(service.recentAudit(7L, 3)).thenReturn(List.of(new BiPermissionAuditEntry(
                101L,
                "alice",
                "BI_PERMISSION_CHANGE",
                "BI_PERMISSION",
                "{\"permissionKind\":\"RESOURCE\",\"operation\":\"CREATE\"}",
                LocalDateTime.parse("2026-06-05T09:20:00"))));
        BiPermissionController controller = new BiPermissionController(resolver, service);

        StepVerifier.create(controller.permissionAudit(3))
                .assertNext(response -> assertThat(response.getData()).singleElement().satisfies(entry -> {
                    assertThat(entry.id()).isEqualTo(101L);
                    assertThat(entry.actorId()).isEqualTo("alice");
                    assertThat(entry.actionKey()).isEqualTo("BI_PERMISSION_CHANGE");
                    assertThat(entry.detailJson()).contains("RESOURCE");
                }))
                .verifyComplete();

        verify(service).recentAudit(7L, 3);
    }

    @Test
    void requestPermissionUsesCurrentTenantAndUser() {
        TenantContextResolver resolver = resolver();
        BiPermissionAdminService adminService = mock(BiPermissionAdminService.class);
        BiPermissionRequestService requestService = mock(BiPermissionRequestService.class);
        BiPermissionRequestCommand command = new BiPermissionRequestCommand(
                "DASHBOARD",
                "canvas-effect",
                "EXPORT",
                "需要下载周报数据");
        when(requestService.requestPermission(7L, "alice", command)).thenReturn(permissionRequestView());
        BiPermissionController controller = new BiPermissionController(resolver, adminService, requestService);

        StepVerifier.create(controller.requestPermission(command))
                .assertNext(response -> assertThat(response.getData().status()).isEqualTo("PENDING"))
                .verifyComplete();

        verify(requestService).requestPermission(7L, "alice", command);
    }

    @Test
    void reviewPermissionRequestUsesCurrentTenantAndUser() {
        TenantContextResolver resolver = resolver();
        BiPermissionAdminService adminService = mock(BiPermissionAdminService.class);
        BiPermissionRequestService requestService = mock(BiPermissionRequestService.class);
        BiPermissionRequestReviewCommand command = new BiPermissionRequestReviewCommand(
                31L,
                "APPROVED",
                "同意导出");
        when(requestService.reviewPermissionRequest(7L, "alice", command)).thenReturn(permissionRequestView("APPROVED"));
        BiPermissionController controller = new BiPermissionController(resolver, adminService, requestService);

        StepVerifier.create(controller.reviewPermissionRequest(31L, command))
                .assertNext(response -> assertThat(response.getData().status()).isEqualTo("APPROVED"))
                .verifyComplete();

        verify(requestService).reviewPermissionRequest(7L, "alice", command);
    }

    @Test
    void listPermissionRequestsUsesCurrentTenantAndFilters() {
        TenantContextResolver resolver = resolver();
        BiPermissionAdminService adminService = mock(BiPermissionAdminService.class);
        BiPermissionRequestService requestService = mock(BiPermissionRequestService.class);
        when(requestService.listPermissionRequests(7L, "DASHBOARD", "canvas-effect", "PENDING"))
                .thenReturn(List.of(permissionRequestView()));
        BiPermissionController controller = new BiPermissionController(resolver, adminService, requestService);

        StepVerifier.create(controller.listPermissionRequests("DASHBOARD", "canvas-effect", "PENDING"))
                .assertNext(response -> assertThat(response.getData()).singleElement()
                        .satisfies(request -> assertThat(request.resourceKey()).isEqualTo("canvas-effect")))
                .verifyComplete();

        verify(requestService).listPermissionRequests(7L, "DASHBOARD", "canvas-effect", "PENDING");
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

    private BiPermissionRequestView permissionRequestView() {
        return permissionRequestView("PENDING");
    }

    private BiPermissionRequestView permissionRequestView(String status) {
        return new BiPermissionRequestView(
                31L,
                7L,
                5L,
                "DASHBOARD",
                "canvas-effect",
                "EXPORT",
                "alice",
                LocalDateTime.parse("2026-06-06T02:00:00"),
                "需要下载周报数据",
                status,
                "reviewer",
                LocalDateTime.parse("2026-06-06T02:05:00"),
                "同意导出",
                99L);
    }
}
