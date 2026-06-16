package org.chovy.canvas.web;

import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.chovy.canvas.common.MapFieldKeys;
import org.chovy.canvas.common.R;
import org.chovy.canvas.common.tenant.RoleNames;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.notification.NotificationService;
import org.chovy.canvas.domain.notification.NotificationWebSocketTicketService;
import org.chovy.canvas.dto.notification.NotificationDTO;
import org.chovy.canvas.dto.notification.NotificationWebSocketTicketDTO;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;
import java.util.Map;

/**
 * 通知消息 HTTP 控制器，根路由为 {@code /canvas/notifications}。
 *
 * <p>负责接收前端或外部系统请求，完成参数绑定、基础校验和统一响应包装。
 * <p>具体业务规则委托给领域服务处理，控制器层保持薄封装以减少重复逻辑。
 */
@RestController
@RequestMapping("/canvas/notifications")
@RequiredArgsConstructor
public class NotificationController {

    /** 通知服务，用于查询和更新站内通知。 */
    private final NotificationService notificationService;
    /** WebSocket 票据服务，用于签发通知连接票据。 */
    private final NotificationWebSocketTicketService ticketService;
    /** 租户上下文解析器，用于隔离通知查询和状态更新。 */
    private final TenantContextResolver tenantContextResolver;

    /**
     * 查询当前用户的通知列表。
     *
     * @param unreadOnly 是否只返回未读通知
     * @param category 通知分类，可为空
     * @param archived 是否只返回已归档通知
     * @param page 页码，从 1 开始
     * @param size 每页数量，最大 100
     * @return 异步返回统一响应，包含列表结果。
     */
    @GetMapping
    public Mono<R<List<NotificationDTO>>> list(
            @RequestParam(defaultValue = "false") boolean unreadOnly,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "false") boolean archived,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        int safePage = Math.max(1, page);
        int safeSize = clamp(size, 1, 100);
        return currentUser().zipWith(tenantContextResolver.currentOrError()).flatMap(tuple ->
                // 通知查询依赖阻塞存储，切到 boundedElastic 后保持 WebFlux 事件线程轻量。
                Mono.fromCallable(() -> notificationService.list(
                                        tenantId(tuple.getT2()),
                                        tuple.getT1(),
                                        unreadOnly,
                                        category,
                                        archived,
                                        safePage,
                                        safeSize)
                                .stream()
                                .map(NotificationDTO::from)
                                .toList())
                        .subscribeOn(Schedulers.boundedElastic())
                        .map(R::ok));
    }

    /**
     * 查询当前用户的未读通知数量。
     *
     * @return 异步返回统一响应，包含 count 字段。
     */
    @GetMapping("/unread-count")
    public Mono<R<Map<String, Long>>> unreadCount() {
        return currentUser().zipWith(tenantContextResolver.currentOrError()).flatMap(tuple ->
                Mono.fromCallable(() -> R.ok(Map.of(
                                MapFieldKeys.COUNT,
                                notificationService.unreadCount(tuple.getT1(), tenantId(tuple.getT2())))))
                        .subscribeOn(Schedulers.boundedElastic()));
    }

    /**
     * 将当前用户的一条通知标记为已读。
     *
     * @param notificationId 通知 ID
     * @return 异步返回统一响应。
     */
    @PutMapping("/{notificationId}/read")
    public Mono<R<Void>> markRead(@PathVariable String notificationId) {
        return currentUser().zipWith(tenantContextResolver.currentOrError()).flatMap(tuple ->
                Mono.<Void>fromRunnable(() -> notificationService.markRead(
                                tuple.getT1(), notificationId, tenantId(tuple.getT2())))
                        .subscribeOn(Schedulers.boundedElastic())
                        .thenReturn(R.<Void>ok()));
    }

    /**
     * 将当前用户的全部通知标记为已读。
     *
     * @return 异步返回统一响应。
     */
    @PutMapping("/read-all")
    public Mono<R<Void>> markAllRead() {
        return currentUser().zipWith(tenantContextResolver.currentOrError()).flatMap(tuple ->
                Mono.<Void>fromRunnable(() -> notificationService.markAllRead(tuple.getT1(), tenantId(tuple.getT2())))
                        .subscribeOn(Schedulers.boundedElastic())
                        .thenReturn(R.<Void>ok()));
    }

    /**
     * 归档当前用户的一条通知。
     *
     * @param notificationId 通知 ID
     * @return 异步返回统一响应。
     */
    @PutMapping("/{notificationId}/archive")
    public Mono<R<Void>> archive(@PathVariable String notificationId) {
        return currentUser().zipWith(tenantContextResolver.currentOrError()).flatMap(tuple ->
                Mono.<Void>fromRunnable(() -> notificationService.archive(
                                tuple.getT1(), notificationId, tenantId(tuple.getT2())))
                        .subscribeOn(Schedulers.boundedElastic())
                        .thenReturn(R.<Void>ok()));
    }

    /**
     * 为通知 WebSocket 握手签发一次性票据。
     *
     * @return 异步返回票据和剩余有效期。
     */
    @PostMapping("/ws-ticket")
    public Mono<R<NotificationWebSocketTicketDTO>> createWsTicket() {
        return currentUser().zipWith(tenantContextResolver.currentOrError()).flatMap(tuple ->
                // WebSocket 握手走公开路由，连接前必须先生成短 TTL 一次性票据。
                Mono.fromCallable(() -> ticketService.createTicket(tenantId(tuple.getT2()), tuple.getT1()))
                        .subscribeOn(Schedulers.boundedElastic())
                        .map(ticket -> R.ok(new NotificationWebSocketTicketDTO(
                                ticket,
                                NotificationWebSocketTicketService.TICKET_TTL_SECONDS))));
    }

    /**
     * 获取当前请求的登录上下文或租户信息。
     *
     * @return 返回 current user 生成的文本或业务键。
     */
    private Mono<String> currentUser() {
        return ReactiveSecurityContextHolder.getContext()
                .map(ctx -> ctx.getAuthentication().getPrincipal())
                .cast(Claims.class)
                // 通知接口始终按登录用户隔离，不接受调用方传 userId。
                .map(claims -> defaultIfBlank(claims.get("username", String.class), "system"))
                .defaultIfEmpty("system");
    }

    /**
     * 按安全边界裁剪或保护输入值。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param min min 参数，用于 clamp 流程中的校验、计算或对象转换。
     * @param max max 参数，用于 clamp 流程中的校验、计算或对象转换。
     * @return 返回 clamp 计算得到的数量、金额或指标值。
     */
    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    /**
     * 按默认值规则处理输入值。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param fallback fallback 参数，用于 defaultIfBlank 流程中的校验、计算或对象转换。
     * @return 返回 default if blank 生成的文本或业务键。
     */
    private String defaultIfBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    /**
     * 解析并规范化租户 ID。
     *
     * @param context 上下文对象，承载租户、身份或运行时信息。
     * @return 返回 tenant id 计算得到的数量、金额或指标值。
     */
    private Long tenantId(TenantContext context) {
        if (context.tenantId() == null && RoleNames.ADMIN.equals(context.role())) {
            return null;
        }
        if (context.tenantId() == null) {
            /**
             * 执行 securityexception 对应的内部处理流程。
             *
             * @param context" context"，由调用方提供
             * @return 返回内部处理结果
             */
            throw new SecurityException("AUTH_003: missing tenant context");
        }
        return context.tenantId();
    }
}
