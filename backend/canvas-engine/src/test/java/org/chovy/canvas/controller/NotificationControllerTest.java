package org.chovy.canvas.web;

import io.jsonwebtoken.Claims;
import org.chovy.canvas.common.tenant.RoleNames;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.dal.dataobject.NotificationDO;
import org.chovy.canvas.domain.notification.NotificationService;
import org.chovy.canvas.domain.notification.NotificationWebSocketTicketService;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import reactor.core.publisher.Mono;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 通知消息 测试类。
 *
 * <p>覆盖该后端组件在典型输入、边界条件和异常场景下的行为，确保重构或性能优化不会改变既有契约。
 * <p>测试代码只构造必要的依赖与数据，断言重点放在可观察结果、状态变更和关键副作用上。
 */
class NotificationControllerTest {

    @Test
    void unreadCount_returnsCurrentUserCountWhenNoSecurityContextExists() {
        NotificationService service = mock(NotificationService.class);
        NotificationWebSocketTicketService ticketService = mock(NotificationWebSocketTicketService.class);
        when(service.unreadCount("system", 7L)).thenReturn(2L);

        NotificationController controller = newController(service, ticketService);

        var response = controller.unreadCount().block();

        assertThat(response.getData().get("count")).isEqualTo(2L);
    }

    @Test
    void markRead_marksCurrentUserNotificationWhenNoSecurityContextExists() {
        NotificationService service = mock(NotificationService.class);
        NotificationWebSocketTicketService ticketService = mock(NotificationWebSocketTicketService.class);
        NotificationController controller = newController(service, ticketService);

        var response = controller.markRead("ntf_1").block();

        assertThat(response.getCode()).isZero();
        verify(service).markRead("system", "ntf_1", 7L);
    }

    @Test
    void unreadCount_usesClaimsUsername() {
        NotificationService service = mock(NotificationService.class);
        NotificationWebSocketTicketService ticketService = mock(NotificationWebSocketTicketService.class);
        when(service.unreadCount("alice", 7L)).thenReturn(3L);
        NotificationController controller = newController(service, ticketService);
        Claims claims = mock(Claims.class);
        when(claims.get("username", String.class)).thenReturn("alice");
        var auth = new UsernamePasswordAuthenticationToken(claims, null, List.of());

        var response = controller.unreadCount()
                .contextWrite(ReactiveSecurityContextHolder.withAuthentication(auth))
                .block();

        assertThat(response.getData().get("count")).isEqualTo(3L);
    }

    @Test
    void unreadCount_fallsBackWhenClaimsUsernameIsMissing() {
        NotificationService service = mock(NotificationService.class);
        NotificationWebSocketTicketService ticketService = mock(NotificationWebSocketTicketService.class);
        when(service.unreadCount("system", 7L)).thenReturn(2L);
        NotificationController controller = newController(service, ticketService);
        Claims claims = mock(Claims.class);
        var auth = new UsernamePasswordAuthenticationToken(claims, null, List.of());

        var response = controller.unreadCount()
                .contextWrite(ReactiveSecurityContextHolder.withAuthentication(auth))
                .block();

        assertThat(response.getData().get("count")).isEqualTo(2L);
    }

    @Test
    void list_capsLargeSizeAtOneHundred() {
        NotificationService service = mock(NotificationService.class);
        NotificationWebSocketTicketService ticketService = mock(NotificationWebSocketTicketService.class);
        NotificationDO notification = new NotificationDO();
        notification.setNotificationId("ntf_1");
        when(service.list(7L, "system", true, null, false, 1, 100)).thenReturn(List.of(notification));
        NotificationController controller = newController(service, ticketService);

        var response = controller.list(true, null, false, -5, 999).block();

        assertThat(response.getData()).hasSize(1);
        verify(service).list(7L, "system", true, null, false, 1, 100);
    }

    @Test
    void list_passesCategoryAndArchivedFilters() {
        NotificationService service = mock(NotificationService.class);
        NotificationWebSocketTicketService ticketService = mock(NotificationWebSocketTicketService.class);
        NotificationController controller = newController(service, ticketService);

        controller.list(false, "ALERT", true, 2, 30).block();

        verify(service).list(7L, "system", false, "ALERT", true, 2, 30);
    }

    @Test
    void createWsTicket_usesCurrentUserAndReturnsTicket() {
        NotificationService service = mock(NotificationService.class);
        NotificationWebSocketTicketService ticketService = mock(NotificationWebSocketTicketService.class);
        when(ticketService.createTicket(7L, "alice")).thenReturn("ntf_ws_1");
        NotificationController controller = newController(service, ticketService);
        Claims claims = mock(Claims.class);
        when(claims.get("username", String.class)).thenReturn("alice");
        var auth = new UsernamePasswordAuthenticationToken(claims, null, List.of());

        var response = controller.createWsTicket()
                .contextWrite(ReactiveSecurityContextHolder.withAuthentication(auth))
                .block();

        assertThat(response.getData().ticket()).isEqualTo("ntf_ws_1");
        assertThat(response.getData().expiresInSeconds()).isEqualTo(60);
        verify(ticketService).createTicket(7L, "alice");
    }

    @Test
    void archive_archivesCurrentUserNotification() {
        NotificationService service = mock(NotificationService.class);
        NotificationWebSocketTicketService ticketService = mock(NotificationWebSocketTicketService.class);
        NotificationController controller = newController(service, ticketService);

        var response = controller.archive("ntf_1").block();

        assertThat(response.getCode()).isZero();
        verify(service).archive("system", "ntf_1", 7L);
    }

    private NotificationController newController(NotificationService service,
                                                 NotificationWebSocketTicketService ticketService) {
        TenantContextResolver tenantContextResolver = mock(TenantContextResolver.class);
        when(tenantContextResolver.currentOrError())
                .thenReturn(Mono.just(new TenantContext(7L, RoleNames.TENANT_ADMIN, "admin")));
        return new NotificationController(service, ticketService, tenantContextResolver);
    }
}
