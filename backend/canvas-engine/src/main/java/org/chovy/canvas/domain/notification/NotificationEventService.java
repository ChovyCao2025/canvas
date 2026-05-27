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
 * 通知消息 Event 通知领域组件。
 *
 * <p>负责站内通知的创建、收件人解析、未读状态和实时推送封装。
 * <p>该组件连接异步任务、WebSocket 和通知持久化模型，保证消息中心口径一致。
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

    /** 封装通知创建的容错逻辑，避免通知失败影响主业务流程。 */
    private void createBestEffort(NotificationCreateCommand command) {
        try {
            notificationService.create(command);
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
