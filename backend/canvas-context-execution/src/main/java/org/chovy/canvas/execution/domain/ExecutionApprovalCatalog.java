package org.chovy.canvas.execution.domain;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.chovy.canvas.execution.api.ExecutionApprovalFacade.ExecutionApprovalDecision;

/**
 * 定义 ExecutionApprovalCatalog 的执行上下文数据结构或业务契约。
 */
public class ExecutionApprovalCatalog {

    /**
     * 保存 PENDING 对应的状态或配置。
     */
    private static final String PENDING = "PENDING";

    /**
     * 保存 APPROVED 对应的状态或配置。
     */
    private static final String APPROVED = "APPROVED";

    /**
     * 保存 REJECTED 对应的状态或配置。
     */
    private static final String REJECTED = "REJECTED";

    /**
     * 保存 NOOP 对应的状态或配置。
     */
    private static final String NOOP = "NOOP";

    /**
     * 保存 clock 对应的状态或配置。
     */
    private final Clock clock;
    private final List<ApprovalEntry> approvals = new ArrayList<>();

    /**
     * 执行 ExecutionApprovalCatalog 对应的业务处理。
     */
    public ExecutionApprovalCatalog() {
        this(Clock.systemDefaultZone());
    }

    /**
     * 执行 ExecutionApprovalCatalog 对应的业务处理。
     * @param clock clock 参数
     */
    ExecutionApprovalCatalog(Clock clock) {
        this.clock = clock;
        approvals.add(new ApprovalEntry(7L, "exec-pending", PENDING, List.of("operator-1"), null, null, null));
        approvals.add(new ApprovalEntry(7L, "exec-reject", PENDING, List.of("operator-2"), null, null, null));
        approvals.add(new ApprovalEntry(7L, "exec-already-approved", APPROVED, List.of("operator-1"), "operator-1", null,
                LocalDateTime.parse("2026-06-15T06:00:00")));
    }

    public ExecutionApprovalDecision decide(
            Long tenantId,
            String executionId,
            String actor,
            String role,
            String comment,
            boolean approved) {
        String normalizedExecutionId = requireText(executionId, "executionId");
        String normalizedActor = actor == null || actor.isBlank() ? "system" : actor.trim();
        ApprovalEntry approval = approvals.stream()
                .filter(entry -> Objects.equals(entry.executionId(), normalizedExecutionId))
                .filter(entry -> PENDING.equals(entry.status()))
                .findFirst()
                .orElse(null);
        if (approval == null) {
            return new ExecutionApprovalDecision(normalizedExecutionId, NOOP, normalizedActor, comment, LocalDateTime.now(clock));
        }
        if (approval.tenantId() != null && !Objects.equals(approval.tenantId(), tenantId)) {
            throw new SecurityException("current tenant cannot access approval");
        }
        if (!approval.approvers().isEmpty() && !approval.approvers().contains(normalizedActor)) {
            throw new SecurityException("AUTH_003: current user is not an approver");
        }
        String result = approved ? APPROVED : REJECTED;
        LocalDateTime resultAt = LocalDateTime.now(clock);
        approval.status(result);
        approval.resultBy(normalizedActor);
        approval.comment(comment == null || comment.isBlank() ? null : comment.trim());
        approval.resultAt(resultAt);
        return new ExecutionApprovalDecision(
                normalizedExecutionId,
                result,
                normalizedActor,
                approval.comment(),
                resultAt);
    }

    /**
     * 执行 requireText 对应的业务处理。
     * @param value value 参数
     * @param field field 参数
     */
    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }

    /**
     * 定义 ApprovalEntry 的执行上下文数据结构或业务契约。
     */
    private static final class ApprovalEntry {
        /**
         * 保存 tenantId 对应的状态或配置。
         */
        private final Long tenantId;

        /**
         * 保存 executionId 对应的状态或配置。
         */
        private final String executionId;

        /**
         * 保存 approvers 对应的状态或配置。
         */
        private final List<String> approvers;

        /**
         * 保存 status 对应的状态或配置。
         */
        private String status;

        /**
         * 保存 resultBy 对应的状态或配置。
         */
        private String resultBy;

        /**
         * 保存 comment 对应的状态或配置。
         */
        private String comment;

        /**
         * 保存 resultAt 对应的状态或配置。
         */
        private LocalDateTime resultAt;

        private ApprovalEntry(
                Long tenantId,
                String executionId,
                String status,
                List<String> approvers,
                String resultBy,
                String comment,
                LocalDateTime resultAt) {
            this.tenantId = tenantId;
            this.executionId = executionId;
            this.status = status;
            this.approvers = approvers;
            this.resultBy = resultBy;
            this.comment = comment;
            this.resultAt = resultAt;
        }

        /**
         * 执行 tenantId 对应的业务处理。
         */
        private Long tenantId() {
            return tenantId;
        }

        /**
         * 执行 executionId 对应的业务处理。
         */
        private String executionId() {
            return executionId;
        }

        /**
         * 执行 status 对应的业务处理。
         * @return 处理后的结果
         */
        private String status() {
            return status;
        }

        /**
         * 执行 status 对应的业务处理。
         * @param status status 参数
         */
        private void status(String status) {
            this.status = status;
        }

        /**
         * 执行 approvers 对应的业务处理。
         * @return 处理后的结果
         */
        private List<String> approvers() {
            return approvers;
        }

        /**
         * 执行 resultBy 对应的业务处理。
         * @param resultBy resultBy 参数
         */
        private void resultBy(String resultBy) {
            this.resultBy = resultBy;
        }

        /**
         * 执行 comment 对应的业务处理。
         * @return 处理后的结果
         */
        private String comment() {
            return comment;
        }

        /**
         * 执行 comment 对应的业务处理。
         * @param comment comment 参数
         */
        private void comment(String comment) {
            this.comment = comment;
        }

        /**
         * 执行 resultAt 对应的业务处理。
         * @param resultAt resultAt 参数
         */
        private void resultAt(LocalDateTime resultAt) {
            this.resultAt = resultAt;
        }
    }
}
