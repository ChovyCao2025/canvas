package org.chovy.canvas.bi.domain;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
/**
 * BiPermissionPolicy 策略。
 */
public class BiPermissionPolicy {
    /**
     * 执行 evaluate 相关处理。
     */
    public BiAccessDecision evaluate(BiAccessRequest request, List<BiPermissionGrant> grants) {
        if (request == null) {
            throw new IllegalArgumentException("access request is required");
        }
        List<BiPermissionGrant> matching = grants == null ? List.of() : grants.stream()
                .filter(grant -> request.tenantId().equals(grant.tenantId()))
                .filter(grant -> request.workspaceId().equals(grant.workspaceId()))
                .filter(grant -> request.resourceType().equals(grant.resourceType()))
                .filter(grant -> request.resourceId().equals(grant.resourceId()))
                .filter(grant -> actionMatches(grant.actionKey(), request.actionKey()))
                .filter(grant -> subjectMatches(grant, request))
                .sorted(Comparator.comparingInt(this::subjectRank))
                .toList();
        // DENY 优先级高于 ALLOW，确保显式拒绝能够覆盖更宽泛的授权。
        BiPermissionGrant deny = matching.stream()
                .filter(grant -> "DENY".equals(grant.effect()))
                .findFirst()
                .orElse(null);
        if (deny != null) {
            return decision(request, deny, false, "explicit BI permission deny matched");
        }
        BiPermissionGrant allow = matching.stream()
                .filter(grant -> "ALLOW".equals(grant.effect()))
                .findFirst()
                .orElse(null);
        if (allow != null) {
            return decision(request, allow, true, "BI permission allow matched");
        }
        return new BiAccessDecision(false, "DENY", null, null, "no BI permission grant matched",
                "bi-permission:v1:%d:%d:%s:%d:%s:DENY:NONE:none".formatted(
                        request.tenantId(),
                        request.workspaceId(),
                        request.resourceType(),
                        request.resourceId(),
                        request.actionKey()));
    }
    /**
     * 执行 decision 相关处理。
     */
    private BiAccessDecision decision(BiAccessRequest request, BiPermissionGrant grant, boolean allowed, String reason) {
        return new BiAccessDecision(allowed, grant.effect(), grant.subjectType(), grant.subjectId(), reason,
                "bi-permission:v1:%d:%d:%s:%d:%s:%s:%s:%s".formatted(
                        request.tenantId(),
                        request.workspaceId(),
                        request.resourceType(),
                        request.resourceId(),
                        request.actionKey(),
                        grant.effect(),
                        grant.subjectType(),
                        grant.subjectId()));
    }
    /**
     * 执行 action Matches 相关处理。
     */
    private boolean actionMatches(String grantAction, String requestedAction) {
        return "*".equals(grantAction) || grantAction.equals(requestedAction);
    }
    /**
     * 执行 subject Matches 相关处理。
     */
    private boolean subjectMatches(BiPermissionGrant grant, BiAccessRequest request) {
        return switch (grant.subjectType()) {
            case "ALL" -> "*".equals(grant.subjectId()) || "ALL".equals(grant.subjectId());
            case "USER" -> grant.subjectId().equals(request.actor());
            case "ROLE" -> request.roles().contains(grant.subjectId().toUpperCase(Locale.ROOT));
            default -> false;
        };
    }
    /**
     * 执行 subject Rank 相关处理。
     */
    private int subjectRank(BiPermissionGrant grant) {
        return switch (grant.subjectType()) {
            case "USER" -> 0;
            case "ROLE" -> 1;
            case "ALL" -> 2;
            default -> 3;
        };
    }
}
