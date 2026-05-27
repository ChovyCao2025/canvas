package org.chovy.canvas.web;

import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.chovy.canvas.common.MapFieldKeys;
import org.chovy.canvas.common.R;
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
        return currentUser().flatMap(userId ->
                // 通知查询依赖阻塞存储，切到 boundedElastic 后保持 WebFlux 事件线程轻量。
                Mono.fromCallable(() -> notificationService.list(userId, unreadOnly, category, archived, safePage, safeSize)
                                .stream()
                                .map(NotificationDTO::from)
                                .toList())
                        .subscribeOn(Schedulers.boundedElastic())
                        .map(R::ok));
    }

    /**
     * 处理 unread Count 对应的 HTTP 接口请求。
     *
     * <p>方法负责接收控制层参数、调用领域服务并封装统一响应。
     *
     * @return 异步执行结果，订阅后产生节点结果或业务响应
     */
    @GetMapping("/unread-count")
    public Mono<R<Map<String, Long>>> unreadCount() {
        return currentUser().flatMap(userId ->
                Mono.fromCallable(() -> R.ok(Map.of(MapFieldKeys.COUNT, notificationService.unreadCount(userId))))
                        .subscribeOn(Schedulers.boundedElastic()));
    }

    /**
     * 处理 mark Read 对应的 HTTP 接口请求。
     *
     * <p>方法负责接收控制层参数、调用领域服务并封装统一响应。
     *
     * @param notificationId notificationId 对应的业务主键或标识
     * @return 异步执行结果，订阅后产生节点结果或业务响应
     */
    @PutMapping("/{notificationId}/read")
    public Mono<R<Void>> markRead(@PathVariable String notificationId) {
        return currentUser().flatMap(userId ->
                Mono.<Void>fromRunnable(() -> notificationService.markRead(userId, notificationId))
                        .subscribeOn(Schedulers.boundedElastic())
                        .thenReturn(R.<Void>ok()));
    }

    /**
     * 处理 mark All Read 对应的 HTTP 接口请求。
     *
     * <p>方法负责接收控制层参数、调用领域服务并封装统一响应。
     *
     * @return 异步执行结果，订阅后产生节点结果或业务响应
     */
    @PutMapping("/read-all")
    public Mono<R<Void>> markAllRead() {
        return currentUser().flatMap(userId ->
                Mono.<Void>fromRunnable(() -> notificationService.markAllRead(userId))
                        .subscribeOn(Schedulers.boundedElastic())
                        .thenReturn(R.<Void>ok()));
    }

    /**
     * 处理 archive 对应的 HTTP 接口请求。
     *
     * <p>方法负责接收控制层参数、调用领域服务并封装统一响应。
     *
     * @param notificationId notificationId 对应的业务主键或标识
     * @return 异步执行结果，订阅后产生节点结果或业务响应
     */
    @PutMapping("/{notificationId}/archive")
    public Mono<R<Void>> archive(@PathVariable String notificationId) {
        return currentUser().flatMap(userId ->
                Mono.<Void>fromRunnable(() -> notificationService.archive(userId, notificationId))
                        .subscribeOn(Schedulers.boundedElastic())
                        .thenReturn(R.<Void>ok()));
    }

    /**
     * 处理 create Ws Ticket 对应的 HTTP 接口请求。
     *
     * <p>方法负责接收控制层参数、调用领域服务并封装统一响应。
     *
     * @return 异步执行结果，订阅后产生节点结果或业务响应
     */
    @PostMapping("/ws-ticket")
    public Mono<R<NotificationWebSocketTicketDTO>> createWsTicket() {
        return currentUser().flatMap(userId ->
                // WebSocket 握手走公开路由，连接前必须先生成短 TTL 一次性票据。
                Mono.fromCallable(() -> ticketService.createTicket(userId))
                        .subscribeOn(Schedulers.boundedElastic())
                        .map(ticket -> R.ok(new NotificationWebSocketTicketDTO(
                                ticket,
                                NotificationWebSocketTicketService.TICKET_TTL_SECONDS))));
    }

    /**
     * 执行 current User 对应的业务逻辑。
     *
     * <p>返回值采用 Reactor 异步模型，调用方可继续组合后续处理。
     *
     * @return 异步执行结果，订阅后产生节点结果或业务响应
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
     * 执行 clamp 对应的业务逻辑。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param value value 待写入、比较或转换的业务值
     * @param min min 方法执行所需的业务参数
     * @param max max 方法执行所需的业务参数
     * @return 计算得到的数值结果
     */
    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    /**
     * 执行 default If Blank 对应的业务逻辑。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param value value 待写入、比较或转换的业务值
     * @param fallback fallback 方法执行所需的业务参数
     * @return 转换或查询得到的字符串结果
     */
    private String defaultIfBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
