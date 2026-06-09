package org.chovy.canvas.web;

import com.fasterxml.jackson.core.type.TypeReference;
import org.chovy.canvas.common.MapFieldKeys;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import org.chovy.canvas.common.R;
import org.chovy.canvas.dal.dataobject.CanvasManualApprovalDO;
import org.chovy.canvas.dal.mapper.CanvasManualApprovalMapper;
import org.chovy.canvas.common.enums.ApprovalStatus;
import org.chovy.canvas.domain.approval.ApprovalWorkflowService;
import org.chovy.canvas.domain.notification.NotificationEventService;
import org.chovy.canvas.engine.context.ExecutionContext;
import org.chovy.canvas.engine.trigger.CanvasExecutionService;
import org.chovy.canvas.infrastructure.redis.ContextPersistenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 执行相关 API：
 * - 人工审批 approve / reject（设计文档 18.2节）
 * - 死信重放（设计文档 13.3节）
 */
@Slf4j
@RestController
@RequestMapping("/canvas/execution")
@RequiredArgsConstructor
public class CanvasExecutionManagementController {

    /** 人工审批 Mapper，用于查询和更新审批单。 */
    private final CanvasManualApprovalMapper approvalMapper;
    /** 上下文持久化服务，用于读取执行上下文快照。 */
    private final ContextPersistenceService ctxStore;
    /** 执行服务，用于审批后继续推进画布执行。 */
    private final CanvasExecutionService executionService;
    /** JSON 转换器，用于解析审批上下文。 */
    private final ObjectMapper objectMapper;
    /** 通知事件服务，用于发送审批结果通知。 */
    private final NotificationEventService notificationEventService;
    /** 统一审批工作流服务；存在时执行审批决策优先写入统一审批任务。 */
    private ApprovalWorkflowService approvalWorkflowService;

    /**
     * 执行 setApprovalWorkflowService 流程，围绕 set approval workflow service 完成校验、计算或结果组装。
     *
     * @param approvalWorkflowService 依赖组件，用于完成数据访问或外部能力调用。
     */
    @Autowired(required = false)
    void setApprovalWorkflowService(ApprovalWorkflowService approvalWorkflowService) {
        this.approvalWorkflowService = approvalWorkflowService;
    }

    /**
     * 人工审批通过（设计文档 18.2节）。
     * 安全校验：当前用户必须在 approvers 列表中（设计文档 18.2节补充说明）。
     *
     * @param executionId 执行实例 ID
     * @return 成功响应
     */
    @PostMapping("/{executionId}/approve")
    public Mono<R<Void>> approve(@PathVariable String executionId) {
        return currentActor()
                .flatMap(actor ->
                        Mono.<Void>fromRunnable(() -> resumeWithResult(executionId, ApprovalStatus.APPROVED, actor,
                                        null, /* watchdog */ false))
                                .subscribeOn(Schedulers.boundedElastic())
                                .thenReturn(R.<Void>ok()));
    }

    /**
     * 人工审批拒绝（设计文档 18.2节）
     *
     * @param executionId 执行实例 ID
     * @param reason      拒绝原因
     * @return 成功响应
     */
    @PostMapping("/{executionId}/reject")
    public Mono<R<Void>> reject(@PathVariable String executionId,
                                @RequestParam(required = false) String reason) {
        return currentActor()
                .flatMap(actor ->
                        Mono.<Void>fromRunnable(() -> resumeWithResult(executionId, ApprovalStatus.REJECTED, actor,
                                        reason, /* watchdog */ false))
                                .subscribeOn(Schedulers.boundedElastic())
                                .thenReturn(R.<Void>ok()));
    }


    /**
     * 执行 resume With Result 对应的业务逻辑。
     *
     * <p>实现会读写 Redis 中的缓存、锁、路由或运行态数据。
     *
     * @param executionId executionId 对应的业务主键或标识
     * @param result result 方法执行所需的业务参数
     * @param approver approver 方法执行所需的业务参数
     * @param isWatchdog isWatchdog 方法执行所需的业务参数
     */
// ── private ──────────────────────────────────────────────────

    private void resumeWithResult(String executionId, String result, RuntimeActor actor,
                                  String comment, boolean isWatchdog) {
        String approver = actor == null ? "system" : actor.username();
        if (!isWatchdog && approvalWorkflowService != null) {
            var decided = approvalWorkflowService.decideTargetTask(
                    actor == null ? 0L : actor.tenantId(),
                    "EXECUTION_NODE",
                    executionId,
                    approver,
                    actor == null ? null : actor.role(),
                    comment,
                    ApprovalStatus.APPROVED.equals(result));
            if (decided != null) {
                log.info("[APPROVAL] 统一审批任务已处理 executionId={} result={}", executionId, result);
                return;
            }
        }
        // 只查 PENDING 记录，审批结果一旦落库后重复请求会自然变成幂等 no-op。
        CanvasManualApprovalDO approval = approvalMapper.selectList(
                        new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<CanvasManualApprovalDO>()
                                .eq(CanvasManualApprovalDO::getExecutionId, executionId)
                                .eq(CanvasManualApprovalDO::getStatus, ApprovalStatus.PENDING)
                                .last("LIMIT 1"))
                .stream().findFirst().orElse(null);

        if (approval == null) {
            log.warn("[APPROVAL] 找不到 PENDING 审批记录 executionId={}", executionId);
            return;
        }

        if (!isWatchdog) {
            ensureApprovalTenant(actor == null ? 0L : actor.tenantId(), approval);
        }

        // 审批人身份校验（设计文档 18.2节安全要求；Watchdog 超时处理时跳过）
        if (!isWatchdog) {
            try {
                List<String> approvers = objectMapper.readValue(
                        approval.getApprovers(), new TypeReference<>() {
                        });
                if (!approvers.contains(approver)) {
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                            "AUTH_003: 当前用户不在审批人列表中，无权操作此审批");
                }
            // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
            } catch (ResponseStatusException e) {
                throw e;
            // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
            } catch (Exception ignored) {
            }
        }

        // 2. 更新审批记录
        approval.setStatus(result);
        approval.setResultBy(approver);
        approval.setResultAt(LocalDateTime.now());
        // 条件更新再次限定 PENDING，防止两个审批人并发通过/拒绝造成双写。
        int updated = approvalMapper.update(approval,
                new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<CanvasManualApprovalDO>()
                        .eq(CanvasManualApprovalDO::getId, approval.getId())
                        .eq(CanvasManualApprovalDO::getStatus, ApprovalStatus.PENDING));
        if (updated <= 0) {
            log.info("[APPROVAL] 审批已被其他请求处理 approvalId={}", approval.getId());
            return;
        }
        notificationEventService.approvalResult(approval, result, approver);

        log.info("[APPROVAL] 审批记录已更新 executionId={} result={}", executionId, result);
    }

    /**
     * 校验输入、权限或业务前置条件。
     *
     * @param requestTenantId 业务对象 ID，用于定位具体记录。
     * @param approval approval 参数，用于 ensureApprovalTenant 流程中的校验、计算或对象转换。
     */
    private void ensureApprovalTenant(Long requestTenantId, CanvasManualApprovalDO approval) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (approval.getTenantId() != null) {
            if (!approval.getTenantId().equals(requestTenantId)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "current tenant cannot access approval");
            }
            // 汇总前面计算出的状态和明细，返回给调用方。
            return;
        }

        ExecutionContext ctx = ctxStore.load(approval.getCanvasId(), approval.getUserId());
        if (ctx == null
                || ctx.getTenantId() == null
                || !ctx.getTenantId().equals(requestTenantId)
                || !approval.getExecutionId().equals(ctx.getExecutionId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "current tenant cannot access approval");
        }
    }

    /**
     * 从 JWT SecurityContext 获取当前登录用户名
     */
    private Mono<RuntimeActor> currentActor() {
        return ReactiveSecurityContextHolder.getContext()
                .map(ctx -> ctx.getAuthentication().getPrincipal())
                .cast(Claims.class)
                .map(c -> new RuntimeActor(
                        readLong(c.get("tenantId")),
                        c.get("role", String.class),
                        defaultIfBlank(c.get("username", String.class), "system")))
                .defaultIfEmpty(new RuntimeActor(0L, null, "system"));
    }

    /**
     * 查询或读取业务数据。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 read long 计算得到的数量、金额或指标值。
     */
    private Long readLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value == null || String.valueOf(value).isBlank()) {
            return 0L;
        }
        try {
            return Long.valueOf(String.valueOf(value));
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (NumberFormatException ignored) {
            return 0L;
        }
    }

    /**
     * 按默认值规则处理输入值。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param fallback fallback 参数，用于 defaultIfBlank 流程中的校验、计算或对象转换。
     * @return 返回 default if blank 生成的文本或业务键。
     */
    private String defaultIfBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    /**
     * RuntimeActor 数据记录。
     */
    private record RuntimeActor(Long tenantId, String role, String username) {
    }
}
