package org.chovy.canvas.domain.notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.chovy.canvas.dal.dataobject.CanvasManualApprovalDO;
import org.chovy.canvas.common.enums.ApprovalStatus;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * 业务事件通知编排服务。
 *
 * <p>将审批、画布变更和系统告警等业务事件转换为消息中心通知。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationEventService {

    /** 通知内容中展示审批截止时间的格式。 */
    private static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    /** 通知服务，用于创建消息中心通知。 */
    private final NotificationService notificationService;
    /** 通知收件人服务，用于解析管理员或目标用户。 */
    private final NotificationRecipientService recipientService;

    /** 创建人工审批待处理通知。 */
    public void approvalPending(CanvasManualApprovalDO approval, List<String> approvers) {
        if (approval == null || approvers == null || approvers.isEmpty()) {
            return;
        }
        String actionUrl = canvasUrl(approval.getCanvasId());
        for (String approver : distinctNonBlank(approvers)) {
            // 每个审批人使用独立 dedupKey，避免多人审批任务只生成一条通知。
            createBestEffort(NotificationCreateCommand.builder()
                    .userId(approver)
                    .category("APPROVAL")
                    .severity("WARNING")
                    .type("APPROVAL_PENDING")
                    .title("待审批：画布执行需要确认")
                    .content("执行 " + approval.getExecutionId() + " 已挂起，截止时间 "
                            /**
                             * 执行 formatTime 流程，围绕 format time 完成校验、计算或结果组装。
                             *
                             * @return 返回 formatTime 流程生成的业务结果。
                             */
                            + formatTime(approval.getTimeoutAt()))
                    .targetUrl(actionUrl)
                    .actionLabel("查看审批")
                    .actionUrl(actionUrl)
                    .bizType("APPROVAL")
                    .bizId(approval.getId())
                    .dedupKey("approval:pending:" + approval.getId() + ":" + approver)
                    .build());
        }
    }

    /** 创建人工审批结果通知。 */
    public void approvalResult(CanvasManualApprovalDO approval, String result, String approver) {
        if (approval == null || !hasText(approval.getUserId())) {
            return;
        }
        boolean approved = ApprovalStatus.APPROVED.equals(result);
        String actionUrl = canvasUrl(approval.getCanvasId());
        createBestEffort(NotificationCreateCommand.builder()
                .userId(approval.getUserId())
                .category("APPROVAL")
                .severity(approved ? "SUCCESS" : "ERROR")
                .type(approved ? "APPROVAL_APPROVED" : "APPROVAL_REJECTED")
                .title(approved ? "审批已通过" : "审批已拒绝")
                .content("执行 " + approval.getExecutionId() + " 由 "
                        /**
                         * 按默认值规则处理输入值。
                         *
                         * @param approver approver 参数，用于 defaultIfBlank 流程中的校验、计算或对象转换。
                         * @return 返回 defaultIfBlank 流程生成的业务结果。
                         */
                        + defaultIfBlank(approver, "system")
                        + (approved ? " 通过" : " 拒绝"))
                .targetUrl(actionUrl)
                .actionLabel("查看执行")
                .actionUrl(actionUrl)
                .bizType("APPROVAL")
                .bizId(approval.getId())
                .dedupKey("approval:result:" + approval.getId())
                .build());
    }

    /** 创建画布变更类通知。 */
    public void canvasChanged(
            String type,
            Long canvasId,
            String title,
            String content,
            String severity,
            String operator) {
        String actionUrl = canvasUrl(canvasId);
        for (String recipient : adminsPlusOperator(operator)) {
            // 画布变更通知面向管理员和操作者，通知失败不影响主业务状态切换。
            createBestEffort(NotificationCreateCommand.builder()
                    .userId(recipient)
                    .category("CHANGE")
                    .severity(defaultIfBlank(severity, "INFO"))
                    .type(type)
                    .title(title)
                    .content(content)
                    .targetUrl(actionUrl)
                    .actionLabel("查看画布")
                    .actionUrl(actionUrl)
                    .bizType("CANVAS")
                    .bizId(canvasId == null ? null : String.valueOf(canvasId))
                    .build());
        }
    }

    /** 创建系统告警通知。 */
    public void systemAlert(
            String type,
            String title,
            String content,
            String targetUrl,
            String bizType,
            String bizId,
            String dedupKey,
            String payloadJson) {
        for (String recipient : adminsOrDefault()) {
            createBestEffort(NotificationCreateCommand.builder()
                    .userId(recipient)
                    .category("ALERT")
                    .severity(type != null && type.contains("NO_ROUTE") ? "WARNING" : "ERROR")
                    .type(type)
                    .title(title)
                    .content(content)
                    .targetUrl(defaultIfBlank(targetUrl, "/home"))
                    .actionLabel("查看配置")
                    .actionUrl(defaultIfBlank(targetUrl, "/home"))
                    .bizType(bizType)
                    .bizId(bizId)
                    .dedupKey(dedupKey)
                    .payloadJson(payloadJson)
                    .build());
        }
    }

    /**
     * 发送租户执行失败激增告警。
     * 告警会按租户和时间窗口生成去重键，通知管理员查看运行态异常。
     */
    public void failedExecutionSpike(Long tenantId, long failedCount, String window) {
        runtimeAlert(
                tenantId,
                "RUNTIME_FAILED_EXECUTION_SPIKE",
                "ERROR",
                "执行失败率异常",
                /**
                 * 按默认值规则处理输入值。
                 *
                 * @param window window 参数，用于 defaultIfBlank 流程中的校验、计算或对象转换。
                 * @param failedCount failed count 参数，用于 defaultIfBlank 流程中的校验、计算或对象转换。
                 * @return 返回 defaultIfBlank 流程生成的业务结果。
                 */
                "最近 " + defaultIfBlank(window, "unknown") + " 失败执行数=" + failedCount,
                /**
                 * 按默认值规则处理输入值。
                 *
                 * @param window window 参数，用于 defaultIfBlank 流程中的校验、计算或对象转换。
                 * @return 返回 defaultIfBlank 流程生成的业务结果。
                 */
                "runtime:failed-execution-spike:" + tenantId + ":" + defaultIfBlank(window, "unknown"));
    }

    /**
     * 发送 DLQ 积压增长告警。
     * 告警面向管理员，按队列名去重，提示处理失败消息堆积。
     */
    public void dlqGrowth(String queue, long count) {
        runtimeAlert(
                null,
                "RUNTIME_DLQ_GROWTH",
                "WARNING",
                "DLQ 积压增长",
                /**
                 * 按默认值规则处理输入值。
                 *
                 * @param queue queue 参数，用于 defaultIfBlank 流程中的校验、计算或对象转换。
                 * @param count 分页、数量或序号参数，用于控制处理规模。
                 * @return 返回 defaultIfBlank 流程生成的业务结果。
                 */
                "队列 " + defaultIfBlank(queue, "unknown") + " 待处理数量=" + count,
                /**
                 * 按默认值规则处理输入值。
                 *
                 * @param queue queue 参数，用于 defaultIfBlank 流程中的校验、计算或对象转换。
                 * @return 返回 defaultIfBlank 流程生成的业务结果。
                 */
                "runtime:dlq-growth:" + defaultIfBlank(queue, "unknown"));
    }

    /**
     * 发送投递 outbox 死信告警。
     * 用于提示投递链路存在不可自动恢复的 DEAD 记录，通知管理员介入处理。
     */
    public void deliveryOutboxDeadRows(long count) {
        runtimeAlert(
                null,
                "RUNTIME_DELIVERY_OUTBOX_DEAD_ROWS",
                "ERROR",
                "投递 outbox 存在死信",
                "投递 outbox DEAD 数量=" + count,
                "runtime:delivery-outbox-dead");
    }

    /**
     * 发送执行 trace buffer 溢出告警。
     * 告警包含丢弃数量，提示排查 trace 采集容量或消费速度。
     */
    public void traceBufferOverflow(long droppedCount) {
        runtimeAlert(
                null,
                "RUNTIME_TRACE_BUFFER_OVERFLOW",
                "WARNING",
                "执行 trace buffer 溢出",
                "trace dropped 数量=" + droppedCount,
                "runtime:trace-buffer-overflow");
    }

    /**
     * 通知管理员运维应急动作已经完成。
     * 消息会包含动作、Canvas、操作者和原因，并指向相关 Canvas 或首页，作为变更告知而非审批。
     */
    public void emergencyActionCompleted(String action, Long canvasId, String operator, String reason) {
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        for (String recipient : adminsOrDefault()) {
            String actionUrl = canvasUrl(canvasId);
            createBestEffort(NotificationCreateCommand.builder()
                    .userId(recipient)
                    .category("CHANGE")
                    .severity("WARNING")
                    .type("OPS_EMERGENCY_ACTION_COMPLETED")
                    .title("运维应急动作已执行")
                    .content("action=" + defaultIfBlank(action, "UNKNOWN")
                            + ", operator=" + defaultIfBlank(operator, "system")
                            + ", reason=" + defaultIfBlank(reason, "未填写"))
                    .targetUrl(actionUrl)
                    .actionLabel("查看画布")
                    .actionUrl(actionUrl)
                    .bizType("CANVAS")
                    .bizId(canvasId == null ? null : String.valueOf(canvasId))
                    .dedupKey("ops:emergency:" + action + ":" + canvasId + ":" + System.currentTimeMillis())
                    .build());
        }
    }

    /**
     * 执行核心业务处理流程。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param type 类型标识，用于选择对应处理分支。
     * @param severity severity 参数，用于 runtimeAlert 流程中的校验、计算或对象转换。
     * @param title title 参数，用于 runtimeAlert 流程中的校验、计算或对象转换。
     * @param content content 参数，用于 runtimeAlert 流程中的校验、计算或对象转换。
     * @param dedupKey 业务键，用于在同一租户下定位资源。
     */
    private void runtimeAlert(Long tenantId,
                              String type,
                              String severity,
                              String title,
                              String content,
                              String dedupKey) {
        for (String recipient : adminsOrDefault()) {
            createBestEffort(NotificationCreateCommand.builder()
                    .tenantId(tenantId)
                    .userId(recipient)
                    .category("ALERT")
                    .severity(severity)
                    .type(type)
                    .title(title)
                    .content(content)
                    .targetUrl("/ops/runtime")
                    .actionLabel("查看运行状态")
                    .actionUrl("/ops/runtime")
                    .bizType("RUNTIME")
                    .bizId(tenantId == null ? null : String.valueOf(tenantId))
                    .dedupKey(dedupKey)
                    .build());
        }
    }

    /** 封装通知创建的容错逻辑，避免通知失败影响主业务流程。 */
    private void createBestEffort(NotificationCreateCommand command) {
        try {
            notificationService.create(command);
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (Exception e) {
            // 通知是旁路能力，异常只记录日志，不能反向中断审批或画布生命周期主流程。
            log.error("[NOTIFICATION] 创建通知失败 userId={} type={}: {}",
                    command.userId(), command.type(), e.getMessage(), e);
        }
    }

    /** 组合管理员和操作人作为画布变更类通知收件人，并保持去重顺序。 */
    private List<String> adminsPlusOperator(String operator) {
        LinkedHashSet<String> recipients = new LinkedHashSet<>(adminsOrDefault());
        if (hasText(operator)) {
            recipients.add(operator);
        }
        return List.copyOf(recipients);
    }

    /** 查询管理员收件人，系统未配置管理员时使用默认 admin。 */
    private List<String> adminsOrDefault() {
        List<String> admins = recipientService.activeAdmins();
        if (admins == null || admins.isEmpty()) {
            return List.of("admin");
        }
        return admins;
    }

    /** 过滤空白收件人并去重，避免同一审批人收到重复通知。 */
    private List<String> distinctNonBlank(List<String> values) {
        return values.stream()
                .filter(this::hasText)
                .distinct()
                .toList();
    }

    /** 生成通知跳转的画布统计页地址。 */
    private String canvasUrl(Long canvasId) {
        return canvasId == null ? "/home" : "/canvas/" + canvasId + "/stats";
    }

    /** 格式化通知中的时间字段，空值显示为未设置。 */
    private String formatTime(java.time.LocalDateTime value) {
        return value == null ? "未设置" : TIME_FORMATTER.format(value);
    }

    /** 返回非空白文本，否则使用默认值。 */
    private String defaultIfBlank(String value, String fallback) {
        return hasText(value) ? value : fallback;
    }

    /** 判断字符串是否包含非空白字符。 */
    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
