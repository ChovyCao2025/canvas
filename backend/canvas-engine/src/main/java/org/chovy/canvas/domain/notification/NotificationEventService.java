package org.chovy.canvas.domain.notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.chovy.canvas.dal.dataobject.CanvasManualApprovalDO;
import org.chovy.canvas.common.enums.ApprovalStatus;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.LinkedHashSet;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationEventService {

    private static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final NotificationService notificationService;
    private final NotificationRecipientService recipientService;

    public void approvalPending(CanvasManualApprovalDO approval, List<String> approvers) {
        if (approval == null || approvers == null || approvers.isEmpty()) {
            return;
        }
        String actionUrl = canvasUrl(approval.getCanvasId());
        for (String approver : distinctNonBlank(approvers)) {
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

    public void canvasChanged(
            String type,
            Long canvasId,
            String title,
            String content,
            String severity,
            String operator) {
        String actionUrl = canvasUrl(canvasId);
        for (String recipient : adminsPlusOperator(operator)) {
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

    private void createBestEffort(NotificationCreateCommand command) {
        try {
            notificationService.create(command);
        } catch (Exception e) {
            log.error("[NOTIFICATION] 创建通知失败 userId={} type={}: {}",
                    command.userId(), command.type(), e.getMessage(), e);
        }
    }

    private List<String> adminsPlusOperator(String operator) {
        LinkedHashSet<String> recipients = new LinkedHashSet<>(adminsOrDefault());
        if (hasText(operator)) {
            recipients.add(operator);
        }
        return List.copyOf(recipients);
    }

    private List<String> adminsOrDefault() {
        List<String> admins = recipientService.activeAdmins();
        if (admins == null || admins.isEmpty()) {
            return List.of("admin");
        }
        return admins;
    }

    private List<String> distinctNonBlank(List<String> values) {
        return values.stream()
                .filter(this::hasText)
                .distinct()
                .toList();
    }

    private String canvasUrl(Long canvasId) {
        return canvasId == null ? "/home" : "/canvas/" + canvasId + "/stats";
    }

    private String formatTime(java.time.LocalDateTime value) {
        return value == null ? "未设置" : TIME_FORMATTER.format(value);
    }

    private String defaultIfBlank(String value, String fallback) {
        return hasText(value) ? value : fallback;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
