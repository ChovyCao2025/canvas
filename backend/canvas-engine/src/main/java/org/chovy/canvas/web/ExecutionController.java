package org.chovy.canvas.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.chovy.canvas.common.R;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.common.validation.ApiRequestValidation;
import org.chovy.canvas.common.enums.NodeType;
import org.chovy.canvas.dal.dataobject.CanvasDO;
import org.chovy.canvas.domain.canvas.CanvasService;
import org.chovy.canvas.domain.project.CanvasProjectAction;
import org.chovy.canvas.domain.project.CanvasProjectPermissionService;
import org.chovy.canvas.engine.disruptor.CanvasDisruptorService;
import org.chovy.canvas.engine.trigger.CanvasExecutionService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.chovy.canvas.security.PublicTriggerAuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import io.jsonwebtoken.Claims;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;

/**
 * 画布执行控制器：
 * 提供直调执行、行为触发异步投递与 dry-run 调试入口。
 */
@RestController
@RequestMapping("/canvas")
@RequiredArgsConstructor
@Validated
@Tag(name = "Canvas Execution", description = "Direct, dry-run, and behavior trigger execution APIs.")
public class ExecutionController {

    /** 执行服务，用于直调和 dry-run 画布执行。 */
    private final CanvasExecutionService executionService;
    // 12.8节 Disruptor 分发
    /** Disruptor 投递服务，用于异步发布行为触发任务。 */
    private final CanvasDisruptorService disruptorService;
    /** 公开触发接口签名校验服务。 */
    private final PublicTriggerAuthService publicTriggerAuthService;
    /** JSON 转换器，用于先验签 raw body，再解析请求体。 */
    private final ObjectMapper objectMapper;
    /** 可选租户上下文解析器，用于 dry-run 控制台权限检查。 */
    private TenantContextResolver tenantContextResolver;
    /** 可选画布服务，用于 dry-run 前读取画布并校验租户。 */
    private CanvasService canvasService;
    /** 可选项目权限服务，用于 dry-run 项目角色校验。 */
    private CanvasProjectPermissionService projectPermissionService;

    /**
     * 执行 setTenantContextResolver 流程，围绕 set tenant context resolver 完成校验、计算或结果组装。
     *
     * @param tenantContextResolver 依赖组件，用于完成数据访问、计算或外部能力调用。
     */
    @Autowired(required = false)
    void setTenantContextResolver(TenantContextResolver tenantContextResolver) {
        this.tenantContextResolver = tenantContextResolver;
    }

    /**
     * 执行 setCanvasService 流程，围绕 set canvas service 完成校验、计算或结果组装。
     *
     * @param canvasService 依赖组件，用于完成数据访问或外部能力调用。
     */
    @Autowired(required = false)
    void setCanvasService(CanvasService canvasService) {
        this.canvasService = canvasService;
    }

    /**
     * 执行 setProjectPermissionService 流程，围绕 set project permission service 完成校验、计算或结果组装。
     *
     * @param projectPermissionService 依赖组件，用于完成数据访问或外部能力调用。
     */
    @Autowired(required = false)
    void setProjectPermissionService(CanvasProjectPermissionService projectPermissionService) {
        this.projectPermissionService = projectPermissionService;
    }

    /**
     * 业务直调接口：同步执行并等待结果
     *
     * @param canvasId 画布 ID
     * @param req      直调请求参数（包含用户 ID、输入参数、幂等 Key）
     * @return 执行结果
     */
    @PostMapping("/execute/direct/{canvasId}")
    @Operation(
            operationId = "executeCanvasDirect",
            summary = "Directly execute a published canvas",
            description = "Public machine-to-machine trigger protected by HMAC headers. Callers should provide idempotencyKey for retry safety.",
            security = @SecurityRequirement(name = "triggerHmac")
    )
    /**
     * 处理 Execution 请求控制器公开辅助方法，供同类 endpoint 复用。
     * 接口在控制器或服务层执行资源权限校验后再处理请求。
     * 主要委托 executionService.trigger 完成业务处理。
     * 副作用取决于下游服务实现。
     * 方法以 Mono 作为异步边界返回，实际执行由 Reactor 链路承载。
     *
     * @param request 请求体。
     * @param canvasId 画布 ID。
     * @param rawBody 请求体，包含本次操作所需字段。
     * @return 异步返回统一响应，包含键值结果。
     */
    public Mono<R<Map<String, Object>>> directCall(
            ServerHttpRequest request,
            @PathVariable Long canvasId,
            @RequestBody Mono<String> rawBody) {
        return parseSignedBody(request, rawBody, DirectCallReq.class)
                .flatMap(req -> {
                    // 设计文档 13.1节：调用方应提供 idempotencyKey，网络超时重试时保持相同值可防重复执行
                    // 未提供时生成随机 UUID（不保证幂等，业务方需知晓风险）
                    // 压测请求通过 inputParams.perfRunId 贯穿账本，通过 idempotencyKey 控制唯一输入键。
                    String dedupKey = (req.getIdempotencyKey() != null && !req.getIdempotencyKey().isBlank())
                            ? req.getIdempotencyKey()
                            : UUID.randomUUID().toString();
                    String userId = requireText(req.getUserId(), "userId");
                    return executionService.trigger(
                                    canvasId, userId, NodeType.DIRECT_CALL,
                                    NodeType.DIRECT_CALL, null,
                                    req.getInputParams(), dedupKey, false)
                            .map(R::ok);
                });
    }

    /**
     * 端内行为触发：异步，经过 Disruptor Ring Buffer 削峰（12.8节）。
     * 立即返回 200，Disruptor 消费者异步执行。
     *
     * @param req 行为触发请求参数
     * @return 成功响应
     */
    @PostMapping("/trigger/behavior")
    @Operation(
            operationId = "triggerCanvasBehavior",
            summary = "Asynchronously trigger a behavior event",
            description = "Public machine-to-machine behavior trigger protected by HMAC headers and admitted through the Disruptor buffer.",
            security = @SecurityRequirement(name = "triggerHmac")
    )
    /**
     * 处理 Execution 请求控制器公开辅助方法，供同类 endpoint 复用。
     * 接口在控制器或服务层执行资源权限校验后再处理请求。
     * 主要委托 disruptorService.publish 完成业务处理。
     * 副作用取决于下游服务实现。
     * 方法以 Mono 作为异步边界返回，实际执行由 Reactor 链路承载。
     *
     * @param request 请求体。
     * @param rawBody 请求体，包含本次操作所需字段。
     * @return 异步返回统一响应，表示操作完成。
     */
    public Mono<R<Void>> behaviorTrigger(
            ServerHttpRequest request,
            @RequestBody Mono<String> rawBody) {
        return parseSignedBody(request, rawBody, BehaviorTriggerReq.class)
                .map(req -> {
                    Long canvasId = requireValue(req.getCanvasId(), "canvasId");
                    String userId = requireText(req.getUserId(), "userId");
                    // 控制器只负责投递 Ring Buffer，实际画布执行由 Disruptor 消费线程异步完成。
                    disruptorService.publish(
                            canvasId, userId, "BEHAVIOR",
                            NodeType.EVENT_TRIGGER, req.getEventCode(),
                            req.getBehaviorData(), req.getEventId());
                    return R.ok();
                });
    }

    /**
     * 干运行：不走 Disruptor，直接同步执行（不产生真实副作用）
     *
     * @param canvasId 画布 ID
     * @param req      请求参数
     * @return 干运行执行结果
     */
    @PostMapping("/execute/dry-run/{canvasId}")
    @Operation(
            operationId = "dryRunCanvasExecution",
            summary = "Dry-run a canvas graph",
            description = "Bearer-authenticated debug execution that uses the supplied graph JSON and does not create real side effects.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    /**
     * 触发Execution运行控制器公开辅助方法，供同类 endpoint 复用。
     * 接口先解析当前租户上下文，按租户隔离处理数据。
     * 主要委托 executionService.triggerDryRun 完成业务处理。
     * 副作用：会触发一次运行流程。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param canvasId 画布 ID。
     * @param req 请求体。
     * @return 异步返回统一响应，包含键值结果。
     */
    public Mono<R<Map<String, Object>>> dryRun(
            @PathVariable Long canvasId,
            @Valid @RequestBody DirectCallReq req) {
        // dry-run 使用请求中的 graphJson，不读取线上发布版本，也不产生真实副作用。
        return currentTenant().flatMap(context ->
                Mono.fromRunnable(() -> requireDryRunAccess(canvasId, context))
                        .subscribeOn(Schedulers.boundedElastic())
                        .then(currentUserId())
                        .flatMap(userId -> executionService.triggerDryRun(
                                        canvasId, userId,
                                        req.getInputParams(), req.getGraphJson())
                                .map(R::ok)));
    }

    /**
     * 获取当前请求的登录上下文或租户信息。
     *
     * @return 返回 current user id 生成的文本或业务键。
     */
    private Mono<String> currentUserId() {
        return ReactiveSecurityContextHolder.getContext()
                .map(ctx -> ctx.getAuthentication().getPrincipal())
                .cast(Claims.class)
                .map(claims -> {
                    // 优先使用 subject 中的用户 ID；旧 token 缺失 subject 时兼容 username。
                    String subject = claims.getSubject();
                    if (subject != null && !subject.isBlank()) {
                        return subject;
                    }
                    String username = claims.get("username", String.class);
                    return username != null && !username.isBlank() ? username : "system";
                })
                .defaultIfEmpty("system");
    }

    /**
     * 获取当前请求的登录上下文或租户信息。
     *
     * @return 返回 currentTenant 流程生成的业务结果。
     */
    private Mono<TenantContext> currentTenant() {
        if (tenantContextResolver == null) {
            return Mono.just(new TenantContext(null, null, "system"));
        }
        return tenantContextResolver.current()
                .defaultIfEmpty(new TenantContext(null, null, "system"));
    }

    /**
     * 校验并获取必需参数、资源或权限。
     *
     * @param canvasId 业务对象 ID，用于定位具体记录。
     * @param context 上下文对象，承载租户、身份或运行时信息。
     */
    private void requireDryRunAccess(Long canvasId, TenantContext context) {
        if (canvasService == null || projectPermissionService == null) {
            return;
        }
        CanvasDO canvas = canvasService.requireTenantAccess(canvasId,
                context == null ? null : context.tenantId(),
                context != null && context.isSuperAdmin());
        projectPermissionService.requireCanvasAction(canvas, context, CanvasProjectAction.EXECUTE);
    }

    /**
     * 解析并校验输入数据。
     *
     * @param request 请求对象，承载本次操作的输入参数。
     * @param rawBody raw body 参数，用于 parseSignedBody 流程中的校验、计算或对象转换。
     * @param bodyType 类型标识，用于选择对应处理分支。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private <T> Mono<T> parseSignedBody(ServerHttpRequest request, Mono<String> rawBody, Class<T> bodyType) {
        return rawBody.defaultIfEmpty("")
                .flatMap(body -> Mono.fromCallable(() -> {
                            publicTriggerAuthService.verify(request.getHeaders(), body);
                            try {
                                return ApiRequestValidation.validate(objectMapper.readValue(body, bodyType));
                            // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
                            } catch (IOException e) {
                                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请求体 JSON 不合法", e);
                            }
                        })
                        .subscribeOn(Schedulers.boundedElastic()));
    }

    /**
     * 校验并获取必需参数、资源或权限。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param fieldName 名称文本，用于展示或唯一性校验。
     * @return 返回 require text 生成的文本或业务键。
     */
    private String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, fieldName + " is required");
        }
        return value;
    }

    /**
     * 校验并获取必需参数、资源或权限。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param fieldName 名称文本，用于展示或唯一性校验。
     * @return 返回 requireValue 流程生成的业务结果。
     */
    private <T> T requireValue(T value, String fieldName) {
        if (value == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, fieldName + " is required");
        }
        return value;
    }


    /**
     * 直调/干运行请求体。
     */
    @Data
    static class DirectCallReq {

        /** 触发用户 ID。 */
        @Schema(description = "Business user ID used as execution subject.", maxLength = 128)
        @Size(max = 128)
        private String userId;

        /** 输入参数（注入到执行上下文 triggerPayload）。 */
        @Schema(description = "Input parameters merged into triggerPayload.")
        private Map<String, Object> inputParams;

        /** 幂等键（建议调用方传入，便于重试去重）。 */
        @Schema(description = "Caller-provided idempotency key for retry safety.", maxLength = 128)
        @Size(max = 128)
        private String idempotencyKey;

        /** dry-run 时传入当前画布 graphJson，直接使用而不读 DB draft。 */
        @Schema(description = "Graph JSON used only by dry-run execution.", maxLength = 1_000_000)
        @Size(max = 1_000_000)
        private String graphJson;
    }

    /**
     * 行为触发请求体（异步投递 Disruptor）。
     */
    @Data
    static class BehaviorTriggerReq {

        /** 目标画布 ID。 */
        @Schema(description = "Target canvas ID.")
        @NotNull
        @Positive
        private Long canvasId;

        /** 触发用户 ID。 */
        @Schema(description = "Business user ID used as execution subject.", maxLength = 128)
        @NotBlank
        @Size(max = 128)
        private String userId;

        /** 行为事件编码。 */
        @Schema(description = "Behavior event code.", maxLength = 128)
        @NotBlank
        @Size(max = 128)
        private String eventCode;

        /** 事件唯一 ID（用于幂等去重）。 */
        @Schema(description = "Unique event ID used for deduplication.", maxLength = 128)
        @NotBlank
        @Size(max = 128)
        private String eventId;

        /** 行为事件载荷。 */
        @Schema(description = "Behavior event payload.")
        private Map<String, Object> behaviorData;
    }
}
