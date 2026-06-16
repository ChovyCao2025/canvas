package org.chovy.canvas.execution.domain;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.chovy.canvas.execution.api.ExecutionApprovalFacade.ExecutionApprovalDecision;

public class ExecutionApprovalCatalog {

    private static final String PENDING = "PENDING";
    private static final String APPROVED = "APPROVED";
    private static final String REJECTED = "REJECTED";
    private static final String NOOP = "NOOP";

    private final Clock clock;
    private final List<ApprovalEntry> approvals = new ArrayList<>();

    public ExecutionApprovalCatalog() {
        this(Clock.systemDefaultZone());
    }

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

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }

    private static final class ApprovalEntry {
        private final Long tenantId;
        private final String executionId;
        private final List<String> approvers;
        private String status;
        private String resultBy;
        private String comment;
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

        private Long tenantId() {
            return tenantId;
        }

        private String executionId() {
            return executionId;
        }

        private String status() {
            return status;
        }

        private void status(String status) {
            this.status = status;
        }

        private List<String> approvers() {
            return approvers;
        }

        private void resultBy(String resultBy) {
            this.resultBy = resultBy;
        }

        private String comment() {
            return comment;
        }

        private void comment(String comment) {
            this.comment = comment;
        }

        private void resultAt(LocalDateTime resultAt) {
            this.resultAt = resultAt;
        }
    }
}
