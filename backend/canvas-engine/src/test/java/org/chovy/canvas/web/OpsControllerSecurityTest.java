package org.chovy.canvas.web;

import org.chovy.canvas.common.tenant.RoleNames;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.dal.dataobject.CanvasDO;
import org.chovy.canvas.dal.mapper.CanvasManualApprovalMapper;
import org.chovy.canvas.dal.mapper.CanvasMapper;
import org.chovy.canvas.dal.mapper.CanvasTemplateMapper;
import org.chovy.canvas.dal.mapper.CanvasVersionMapper;
import org.chovy.canvas.domain.canvas.CanvasOpsService;
import org.chovy.canvas.domain.canvas.CanvasService;
import org.chovy.canvas.domain.notification.NotificationEventService;
import org.chovy.canvas.domain.ops.OpsAuditEventService;
import org.chovy.canvas.infrastructure.cache.CanvasConfigCache;
import org.chovy.canvas.infrastructure.redis.TriggerRouteRecoveryService;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OpsControllerSecurityTest {

    @Test
    void rejectsUnauthenticatedRequests() {
        TenantContextResolver resolver = mock(TenantContextResolver.class);
        when(resolver.currentOrError()).thenReturn(Mono.error(new SecurityException("missing tenant context")));
        OpsController controller = controller(resolver);

        StepVerifier.create(controller.runtimeStatus())
                .expectError(SecurityException.class)
                .verify();
    }

    @Test
    void allowsOperatorReadOnlyAccess() {
        OpsController controller = controller(context(RoleNames.OPERATOR, 1L, "operator"));

        var response = controller.runtimeStatus().block();

        assertThat(response.getCode()).isZero();
        assertThat(response.getData().role()).isEqualTo(RoleNames.OPERATOR);
    }

    @Test
    void limitsTenantAdminEmergencyActionsToTenantScope() {
        TenantContextResolver resolver = resolver(context(RoleNames.TENANT_ADMIN, 1L, "tenant-admin"));
        CanvasService canvasService = mock(CanvasService.class);
        when(canvasService.requireTenantAccess(20L, 1L, false))
                .thenThrow(new AccessDeniedException("跨租户访问被拒绝"));
        OpsController controller = controller(resolver, canvasService, mock(CanvasOpsService.class),
                new OpsAuditEventService(), mock(NotificationEventService.class));

        StepVerifier.create(controller.killCanvas(20L, request("tenant scoped kill")))
                .expectError(AccessDeniedException.class)
                .verify();
    }

    @Test
    void allowsSystemAdminGlobalActions() {
        TenantContextResolver resolver = resolver(context(RoleNames.SUPER_ADMIN, null, "root"));
        CanvasService canvasService = mock(CanvasService.class);
        CanvasOpsService opsService = mock(CanvasOpsService.class);
        OpsAuditEventService auditService = new OpsAuditEventService();
        when(canvasService.requireTenantAccess(20L, null, true)).thenReturn(canvas(20L, 99L));
        OpsController controller = controller(resolver, canvasService, opsService,
                auditService, mock(NotificationEventService.class));

        var response = controller.killCanvas(20L, request("incident mitigation")).block();

        assertThat(response.getCode()).isZero();
        verify(opsService).kill(20L, "GRACEFUL");
        assertThat(auditService.recent(null, 10))
                .extracting(OpsAuditEventService.OpsAuditEvent::action)
                .contains("KILL");
    }

    @Test
    void requiresReasonText() {
        TenantContextResolver resolver = resolver(context(RoleNames.TENANT_ADMIN, 1L, "tenant-admin"));
        CanvasService canvasService = mock(CanvasService.class);
        CanvasOpsService opsService = mock(CanvasOpsService.class);
        OpsController controller = controller(resolver, canvasService, opsService,
                new OpsAuditEventService(), mock(NotificationEventService.class));

        StepVerifier.create(controller.pauseCanvas(20L, request(" ")))
                .expectError(IllegalArgumentException.class)
                .verify();
        verify(canvasService, never()).offline(eq(20L), eq("tenant-admin"));
    }

    @Test
    void createsAuditEvent() {
        TenantContextResolver resolver = resolver(context(RoleNames.TENANT_ADMIN, 1L, "tenant-admin"));
        CanvasService canvasService = mock(CanvasService.class);
        OpsAuditEventService auditService = new OpsAuditEventService();
        when(canvasService.requireTenantAccess(20L, 1L, false)).thenReturn(canvas(20L, 1L));
        OpsController controller = controller(resolver, canvasService, mock(CanvasOpsService.class),
                auditService, mock(NotificationEventService.class));

        var response = controller.pauseCanvas(20L, request("pause during incident")).block();

        assertThat(response.getCode()).isZero();
        verify(canvasService).offline(20L, "tenant-admin");
        assertThat(auditService.recent(1L, 10))
                .extracting(OpsAuditEventService.OpsAuditEvent::action,
                        OpsAuditEventService.OpsAuditEvent::reason)
                .contains(org.assertj.core.groups.Tuple.tuple("PAUSE", "pause during incident"));
    }

    private OpsController controller(TenantContext context) {
        return controller(resolver(context));
    }

    private OpsController controller(TenantContextResolver resolver) {
        return controller(resolver, mock(CanvasService.class), mock(CanvasOpsService.class),
                new OpsAuditEventService(), mock(NotificationEventService.class));
    }

    private OpsController controller(TenantContextResolver resolver,
                                     CanvasService canvasService,
                                     CanvasOpsService opsService,
                                     OpsAuditEventService auditService,
                                     NotificationEventService notificationEventService) {
        return new OpsController(
                mock(CanvasTemplateMapper.class),
                mock(CanvasMapper.class),
                mock(CanvasVersionMapper.class),
                mock(CanvasManualApprovalMapper.class),
                mock(CanvasConfigCache.class),
                mock(TriggerRouteRecoveryService.class),
                resolver,
                canvasService,
                opsService,
                auditService,
                notificationEventService);
    }

    private TenantContextResolver resolver(TenantContext context) {
        TenantContextResolver resolver = mock(TenantContextResolver.class);
        when(resolver.currentOrError()).thenReturn(Mono.just(context));
        return resolver;
    }

    private TenantContext context(String role, Long tenantId, String username) {
        return new TenantContext(tenantId, role, username);
    }

    private OpsController.EmergencyActionReq request(String reason) {
        OpsController.EmergencyActionReq request = new OpsController.EmergencyActionReq();
        request.setReason(reason);
        request.setMode("GRACEFUL");
        return request;
    }

    private CanvasDO canvas(Long id, Long tenantId) {
        CanvasDO canvas = new CanvasDO();
        canvas.setId(id);
        canvas.setTenantId(tenantId);
        return canvas;
    }
}
