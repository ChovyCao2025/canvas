package org.chovy.canvas.platform.api;

import java.util.List;
import java.util.Map;

public interface ApprovalFacade {

    List<Map<String, Object>> tasks(Long tenantId, String actor, String role, String status);

    List<Map<String, Object>> instances(Long tenantId, String targetType, String targetId, String status);

    Map<String, Object> approve(Long tenantId, Long taskId, Map<String, Object> payload, String actor, String role);

    Map<String, Object> reject(Long tenantId, Long taskId, Map<String, Object> payload, String actor, String role);

    Map<String, Object> syncLarkApprovals(Long tenantId, Integer limit, String actor, String role);

    Map<String, Object> syncLarkApprovalInstance(Long tenantId, Long instanceId, String actor, String role);
}
