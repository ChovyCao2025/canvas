package org.chovy.canvas.platform.application;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.chovy.canvas.platform.api.ApprovalFacade;
import org.chovy.canvas.platform.domain.ApprovalCatalog;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ApprovalApplicationService implements ApprovalFacade {

    private static final Long DEFAULT_TENANT_ID = 7L;
    private static final String DEFAULT_ACTOR = "operator-1";
    private static final String DEFAULT_ROLE = "OPERATOR";

    private final ApprovalCatalog catalog;

    public ApprovalApplicationService() {
        this(new ApprovalCatalog());
    }

    public ApprovalApplicationService(ApprovalCatalog catalog) {
        this.catalog = catalog;
    }

    @Override
    public List<Map<String, Object>> tasks(Long tenantId, String actor, String role, String status) {
        return catalog.tasks(safeTenantId(tenantId), actorOrDefault(actor), roleOrDefault(role), statusOrPending(status));
    }

    @Override
    public List<Map<String, Object>> instances(Long tenantId, String targetType, String targetId, String status) {
        return catalog.instances(safeTenantId(tenantId), targetType, targetId, normalizedStatus(status));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> approve(Long tenantId, Long taskId, Map<String, Object> payload, String actor,
                                       String role) {
        return catalog.decide(safeTenantId(tenantId), taskId, actorOrDefault(actor), roleOrDefault(role),
                stringValue(payload, "comment"),
                "APPROVED");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> reject(Long tenantId, Long taskId, Map<String, Object> payload, String actor,
                                      String role) {
        return catalog.decide(safeTenantId(tenantId), taskId, actorOrDefault(actor), roleOrDefault(role),
                stringValue(payload, "comment"),
                "REJECTED");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> syncLarkApprovals(Long tenantId, Integer limit, String actor, String role) {
        requireAdmin(roleOrDefault(role));
        return catalog.syncLarkApprovals(safeTenantId(tenantId), normalizedLimit(limit), actorOrDefault(actor));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> syncLarkApprovalInstance(Long tenantId, Long instanceId, String actor, String role) {
        requireAdmin(roleOrDefault(role));
        return catalog.syncLarkApprovalInstance(safeTenantId(tenantId), instanceId, actorOrDefault(actor));
    }

    private static Long safeTenantId(Long tenantId) {
        return tenantId == null || tenantId <= 0 ? DEFAULT_TENANT_ID : tenantId;
    }

    private static String actorOrDefault(String actor) {
        return actor == null || actor.isBlank() ? DEFAULT_ACTOR : actor.trim();
    }

    private static String roleOrDefault(String role) {
        return role == null || role.isBlank() ? DEFAULT_ROLE : role.trim().toUpperCase(Locale.ROOT);
    }

    private static String statusOrPending(String status) {
        String normalized = normalizedStatus(status);
        return normalized == null ? "PENDING" : normalized;
    }

    private static String normalizedStatus(String status) {
        return status == null || status.isBlank() ? null : status.trim().toUpperCase(Locale.ROOT);
    }

    private static int normalizedLimit(Integer limit) {
        if (limit == null) {
            return 100;
        }
        if (limit <= 0) {
            throw new IllegalArgumentException("approval sync limit must be positive");
        }
        return Math.min(limit, 500);
    }

    private static void requireAdmin(String role) {
        if ("ADMIN".equals(role) || "TENANT_ADMIN".equals(role) || "SUPER_ADMIN".equals(role)) {
            return;
        }
        throw new SecurityException("Lark approval sync requires admin role");
    }

    private static String stringValue(Map<String, Object> payload, String key) {
        if (payload == null) {
            return null;
        }
        Object value = payload.get(key);
        return value == null ? null : String.valueOf(value);
    }
}
