package org.chovy.canvas.web;

import com.fasterxml.jackson.core.type.TypeReference;
import org.chovy.canvas.common.MapFieldKeys;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import org.chovy.canvas.common.R;
import org.chovy.canvas.dal.dataobject.CanvasManualApprovalDO;
import org.chovy.canvas.dal.mapper.CanvasManualApprovalMapper;
import org.chovy.canvas.common.enums.ApprovalStatus;
import org.chovy.canvas.domain.notification.NotificationEventService;
import org.chovy.canvas.engine.trigger.CanvasExecutionService;
import org.chovy.canvas.infrastructure.redis.ContextPersistenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
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

    /**
     * 人工审批通过（设计文档 18.2节）。
     * 安全校验：当前用户必须在 approvers 列表中（设计文档 18.2节补充说明）。
     *
     * @param executionId 执行实例 ID
     * @return 成功响应
     */
    @PostMapping("/{executionId}/approve")
    public Mono<R<Void>> approve(@PathVariable String executionId) {
        return currentUsername()
                .flatMap(username ->
                        Mono.<Void>fromRunnable(() -> resumeWithResult(executionId, ApprovalStatus.APPROVED, username,
                                        /* watchdog */ false))
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
        return currentUsername()
                .flatMap(username ->
                        Mono.<Void>fromRunnable(() -> resumeWithResult(executionId, ApprovalStatus.REJECTED, username,
                                        /* watchdog */ false))
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

    private void resumeWithResult(String executionId, String result, String approver,
                                  boolean isWatchdog) {
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
            } catch (ResponseStatusException e) {
                throw e;
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
     * 从 JWT SecurityContext 获取当前登录用户名
     */
    private Mono<String> currentUsername() {
        return ReactiveSecurityContextHolder.getContext()
                .map(ctx -> ctx.getAuthentication().getPrincipal())
                .cast(Claims.class)
                .map(c -> c.get("username", String.class))
                .defaultIfEmpty("system");
    }
}
