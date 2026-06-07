package org.chovy.canvas.domain.approval;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.dal.dataobject.ApprovalDefinitionDO;
import org.chovy.canvas.dal.dataobject.ApprovalInstanceDO;
import org.chovy.canvas.dal.dataobject.ApprovalTaskDO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class LarkApprovalProvider implements ApprovalExternalProvider {

    private static final ObjectMapper JSON = new ObjectMapper();

    private final LarkApprovalClient client;
    private final ApprovalLarkUserIdentityResolver identityResolver;

    public LarkApprovalProvider(LarkApprovalClient client) {
        this(client, null);
    }

    @Autowired
    public LarkApprovalProvider(LarkApprovalClient client, ApprovalLarkUserIdentityResolver identityResolver) {
        this.client = client;
        this.identityResolver = identityResolver;
    }

    @Override
    public boolean supports(String provider) {
        return provider != null && "LARK".equalsIgnoreCase(provider.trim());
    }

    @Override
    public ApprovalExternalSubmissionResult submit(ApprovalDefinitionDO definition,
                                                   ApprovalInstanceDO instance,
                                                   List<ApprovalTaskDO> tasks,
                                                   ApprovalSubmitCommand command) {
        JsonNode binding = larkBinding(command == null ? null : command.snapshotJson());
        if (binding == null || binding.isMissingNode()) {
            return null;
        }
        String instanceCode = text(binding, "instanceCode");
        Map<Long, String> taskIds = taskIds(binding.path("taskIdsByLocalTaskId"));
        if (instanceCode == null) {
            instanceCode = createInstanceIfRequested(definition, instance, binding.path("create"));
            if (instanceCode != null && taskIds.isEmpty()) {
                taskIds = taskIdsFromCreatedInstance(instance, tasks, instanceCode);
            }
        }
        if (instanceCode == null && taskIds.isEmpty()) {
            return null;
        }
        return new ApprovalExternalSubmissionResult(instanceCode, taskIds);
    }

    @Override
    public void decide(ApprovalDefinitionDO definition,
                       ApprovalInstanceDO instance,
                       ApprovalTaskDO task,
                       ApprovalDecisionCommand command,
                       boolean approve) {
        String instanceCode = required(instance == null ? null : instance.getExternalInstanceId(),
                "Lark approval external instance id is required before deciding a Lark-backed task");
        String taskId = required(task == null ? null : task.getExternalTaskId(),
                "Lark approval external task id is required before deciding a Lark-backed task");
        LarkApprovalTaskActionRequest request = new LarkApprovalTaskActionRequest(
                instance.getTenantId(),
                instanceCode,
                taskId,
                command == null ? null : command.actor(),
                command == null ? null : command.comment());
        if (approve) {
            client.approveTask(request);
        } else {
            client.rejectTask(request);
        }
    }

    @Override
    public ApprovalExternalSyncResult sync(ApprovalDefinitionDO definition, ApprovalInstanceDO instance) {
        String instanceCode = required(instance == null ? null : instance.getExternalInstanceId(),
                "Lark approval external instance id is required before syncing a Lark-backed instance");
        LarkApprovalInstanceSnapshot snapshot = client.getInstance(instance.getTenantId(), instanceCode);
        if (snapshot == null) {
            return null;
        }
        Map<String, String> taskStatuses = new LinkedHashMap<>();
        for (LarkApprovalTaskSnapshot task : safeList(snapshot.tasks())) {
            String taskId = trimToNull(task.taskId());
            String status = mapTaskStatus(task.status());
            if (taskId != null && status != null) {
                taskStatuses.put(taskId, status);
            }
        }
        return new ApprovalExternalSyncResult(mapInstanceStatus(snapshot.status()), taskStatuses);
    }

    private JsonNode larkBinding(String snapshotJson) {
        String value = trimToNull(snapshotJson);
        if (value == null) {
            return null;
        }
        try {
            JsonNode root = JSON.readTree(value);
            JsonNode lark = root.path("lark");
            if (!lark.isMissingNode()) {
                return lark;
            }
            if (root.has("larkInstanceCode") || root.has("larkTaskIdsByLocalTaskId")) {
                return root;
            }
            return null;
        } catch (Exception ex) {
            throw new IllegalArgumentException("invalid approval snapshot JSON for Lark binding", ex);
        }
    }

    private Map<Long, String> taskIds(JsonNode node) {
        if (node == null || !node.isObject()) {
            return Map.of();
        }
        Map<Long, String> values = new LinkedHashMap<>();
        node.fields().forEachRemaining(entry -> {
            String externalTaskId = trimToNull(entry.getValue().asText(null));
            if (externalTaskId == null) {
                return;
            }
            try {
                values.put(Long.parseLong(entry.getKey()), externalTaskId);
            } catch (NumberFormatException ignored) {
                // Ignore malformed local task ids from upstream binding payloads.
            }
        });
        return values;
    }

    private Map<Long, String> taskIdsFromCreatedInstance(ApprovalInstanceDO instance,
                                                         List<ApprovalTaskDO> localTasks,
                                                         String instanceCode) {
        if (identityResolver == null || instance == null || trimToNull(instanceCode) == null) {
            return Map.of();
        }
        LarkApprovalInstanceSnapshot snapshot = client.getInstance(instance.getTenantId(), instanceCode);
        if (snapshot == null || snapshot.tasks() == null || snapshot.tasks().isEmpty()) {
            return Map.of();
        }
        Map<String, String> taskIdsByOpenId = uniqueTaskIdsByUserId(snapshot.tasks());
        if (taskIdsByOpenId.isEmpty()) {
            return Map.of();
        }
        Map<Long, String> values = new LinkedHashMap<>();
        for (ApprovalTaskDO task : safeList(localTasks)) {
            if (task == null || task.getId() == null) {
                continue;
            }
            ApprovalLarkUserIdentity identity = identityResolver.resolve(instance.getTenantId(), task.getApprover());
            String openId = identity == null ? null : trimToNull(identity.openId());
            String externalTaskId = openId == null ? null : taskIdsByOpenId.get(openId);
            if (externalTaskId != null) {
                values.put(task.getId(), externalTaskId);
            }
        }
        return values;
    }

    private Map<String, String> uniqueTaskIdsByUserId(List<LarkApprovalTaskSnapshot> tasks) {
        Map<String, String> values = new LinkedHashMap<>();
        Map<String, Boolean> duplicates = new LinkedHashMap<>();
        for (LarkApprovalTaskSnapshot task : safeList(tasks)) {
            String userId = trimToNull(task.userId());
            String taskId = trimToNull(task.taskId());
            if (userId == null || taskId == null) {
                continue;
            }
            if (values.containsKey(userId)) {
                duplicates.put(userId, true);
                values.remove(userId);
                continue;
            }
            if (!duplicates.containsKey(userId)) {
                values.put(userId, taskId);
            }
        }
        return values;
    }

    private String createInstanceIfRequested(ApprovalDefinitionDO definition,
                                             ApprovalInstanceDO instance,
                                             JsonNode createNode) {
        if (createNode == null || createNode.isMissingNode() || createNode.isNull()) {
            return null;
        }
        String approvalCode = required(definition == null ? null : definition.getExternalDefinitionCode(),
                "Lark approval external definition code is required before creating an instance");
        String form = required(text(createNode, "form"),
                "Lark approval create payload form is required");
        String openId = text(createNode, "openId");
        String userId = text(createNode, "userId");
        if (openId == null && userId == null) {
            throw new IllegalStateException("Lark approval create payload requires openId or userId");
        }
        return trimToNull(client.createInstance(new LarkApprovalCreateInstanceRequest(
                instance == null ? null : instance.getTenantId(),
                approvalCode,
                createUuid(instance),
                openId,
                userId,
                text(createNode, "departmentId"),
                form)));
    }

    private String createUuid(ApprovalInstanceDO instance) {
        if (instance == null || instance.getId() == null) {
            return null;
        }
        return "canvas-approval-" + instance.getId();
    }

    private String text(JsonNode node, String field) {
        String direct = trimToNull(node.path(field).asText(null));
        if (direct != null) {
            return direct;
        }
        if ("instanceCode".equals(field)) {
            return trimToNull(node.path("larkInstanceCode").asText(null));
        }
        if ("openId".equals(field)) {
            return trimToNull(node.path("open_id").asText(null));
        }
        if ("userId".equals(field)) {
            return trimToNull(node.path("user_id").asText(null));
        }
        if ("departmentId".equals(field)) {
            return trimToNull(node.path("department_id").asText(null));
        }
        return null;
    }

    private String required(String raw, String message) {
        String value = trimToNull(raw);
        if (value == null) {
            throw new IllegalStateException(message);
        }
        return value;
    }

    private String mapInstanceStatus(String raw) {
        String status = normalize(raw);
        if (status == null) {
            return null;
        }
        return switch (status) {
            case "APPROVED" -> ApprovalWorkflowService.STATUS_APPROVED;
            case "REJECTED" -> ApprovalWorkflowService.STATUS_REJECTED;
            case "CANCELED", "DELETED" -> ApprovalWorkflowService.STATUS_CANCELLED;
            case "PENDING" -> ApprovalWorkflowService.STATUS_PENDING;
            default -> null;
        };
    }

    private String mapTaskStatus(String raw) {
        String status = normalize(raw);
        if (status == null) {
            return null;
        }
        return switch (status) {
            case "APPROVED", "DONE" -> ApprovalWorkflowService.STATUS_APPROVED;
            case "REJECTED" -> ApprovalWorkflowService.STATUS_REJECTED;
            case "TRANSFERRED" -> "TRANSFERRED";
            case "PENDING" -> ApprovalWorkflowService.STATUS_PENDING;
            default -> null;
        };
    }

    private String normalize(String raw) {
        String value = trimToNull(raw);
        return value == null ? null : value.toUpperCase(Locale.ROOT);
    }

    private String trimToNull(String raw) {
        if (raw == null) {
            return null;
        }
        String value = raw.trim();
        return value.isEmpty() ? null : value;
    }

    private <T> List<T> safeList(List<T> rows) {
        return rows == null ? List.of() : rows;
    }
}
