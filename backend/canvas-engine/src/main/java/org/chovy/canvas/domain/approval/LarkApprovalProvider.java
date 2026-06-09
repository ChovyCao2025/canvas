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

/**
 * LarkApprovalProvider 编排 domain.approval 场景的领域业务规则。
 */
@Service
public class LarkApprovalProvider implements ApprovalExternalProvider {

    private static final ObjectMapper JSON = new ObjectMapper();

    private final LarkApprovalClient client;
    private final ApprovalLarkUserIdentityResolver identityResolver;

    /**
     * 创建 LarkApprovalProvider 实例并注入 domain.approval 场景依赖。
     * @param client 依赖组件，用于完成数据访问或外部能力调用。
     */
    public LarkApprovalProvider(LarkApprovalClient client) {
        this(client, null);
    }

    /**
     * 创建 LarkApprovalProvider 实例并注入 domain.approval 场景依赖。
     * @param client 依赖组件，用于完成数据访问或外部能力调用。
     * @param identityResolver 依赖组件，用于完成数据访问、计算或外部能力调用。
     */
    @Autowired
    public LarkApprovalProvider(LarkApprovalClient client, ApprovalLarkUserIdentityResolver identityResolver) {
        this.client = client;
        this.identityResolver = identityResolver;
    }

    /**
     * 判断外部审批提供方标识是否由飞书实现处理。
     *
     * @param provider 审批定义或运行时命令中的提供方编码
     * @return {@code true} 表示编码去空白后等于 LARK，大小写不敏感
     */
    @Override
    public boolean supports(String provider) {
        return provider != null && "LARK".equalsIgnoreCase(provider.trim());
    }

    /**
     * submit 创建或触发 domain.approval 场景的业务处理。
     * @param definition definition 参数，用于 submit 流程中的校验、计算或对象转换。
     * @param instance instance 参数，用于 submit 流程中的校验、计算或对象转换。
     * @param tasks tasks 参数，用于 submit 流程中的校验、计算或对象转换。
     * @param command 命令对象，描述本次业务动作及其参数。
     * @return 返回 submit 流程生成的业务结果。
     */
    @Override
    public ApprovalExternalSubmissionResult submit(ApprovalDefinitionDO definition,
                                                   ApprovalInstanceDO instance,
                                                   List<ApprovalTaskDO> tasks,
                                                   ApprovalSubmitCommand command) {
        // 准备本次处理所需的上下文和中间变量。
        JsonNode binding = larkBinding(command == null ? null : command.snapshotJson());
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
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
        // 汇总前面计算出的状态和明细，返回给调用方。
        return new ApprovalExternalSubmissionResult(instanceCode, taskIds);
    }

    /**
     * decide 处理 domain.approval 场景的业务逻辑。
     * @param definition definition 参数，用于 decide 流程中的校验、计算或对象转换。
     * @param instance instance 参数，用于 decide 流程中的校验、计算或对象转换。
     * @param task task 参数，用于 decide 流程中的校验、计算或对象转换。
     * @param command 命令对象，描述本次业务动作及其参数。
     * @param approve approve 参数，用于 decide 流程中的校验、计算或对象转换。
     */
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

    /**
     * 从飞书审批实例拉取最新状态并映射为本地审批状态变更。
     *
     * <p>方法要求本地实例已保存飞书 instance code，会通过客户端读取实例快照，将飞书实例状态映射为本地实例状态，
     * 并按外部 task id 汇总任务状态。该方法只返回同步结果，不直接落库；实际更新由审批工作流服务完成。</p>
     *
     * @param definition 本地审批定义，用于提供外部审批配置上下文
     * @param instance 已提交到飞书的本地审批实例
     * @return 可应用到本地实例和任务的状态快照；飞书无快照时返回 {@code null}
     */
    @Override
    public ApprovalExternalSyncResult sync(ApprovalDefinitionDO definition, ApprovalInstanceDO instance) {
        String instanceCode = required(instance == null ? null : instance.getExternalInstanceId(),
                "Lark approval external instance id is required before syncing a Lark-backed instance");
        LarkApprovalInstanceSnapshot snapshot = client.getInstance(instance.getTenantId(), instanceCode);
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (snapshot == null) {
            return null;
        }
        Map<String, String> taskStatuses = new LinkedHashMap<>();
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        for (LarkApprovalTaskSnapshot task : safeList(snapshot.tasks())) {
            String taskId = trimToNull(task.taskId());
            String status = mapTaskStatus(task.status());
            if (taskId != null && status != null) {
                taskStatuses.put(taskId, status);
            }
        }
        // 汇总前面计算出的状态和明细，返回给调用方。
        return new ApprovalExternalSyncResult(mapInstanceStatus(snapshot.status()), taskStatuses);
    }

    /**
     * 执行 larkBinding 流程，围绕 lark binding 完成校验、计算或结果组装。
     *
     * @param snapshotJson JSON 字符串，承载结构化配置或明细。
     * @return 返回 larkBinding 流程生成的业务结果。
     */
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
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (Exception ex) {
            throw new IllegalArgumentException("invalid approval snapshot JSON for Lark binding", ex);
        }
    }

    /**
     * 执行 taskIds 流程，围绕 task ids 完成校验、计算或结果组装。
     *
     * @param node node 参数，用于 taskIds 流程中的校验、计算或对象转换。
     * @return 返回 task ids 生成的文本或业务键。
     */
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
            // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
            } catch (NumberFormatException ignored) {
                // Ignore malformed local task ids from upstream binding payloads.
            }
        });
        return values;
    }

    /**
     * 执行 taskIdsFromCreatedInstance 流程，围绕 task ids from created instance 完成校验、计算或结果组装。
     *
     * @param instance instance 参数，用于 taskIdsFromCreatedInstance 流程中的校验、计算或对象转换。
     * @param localTasks local tasks 参数，用于 taskIdsFromCreatedInstance 流程中的校验、计算或对象转换。
     * @param instanceCode 业务编码，用于匹配对应类型或状态。
     * @return 返回 task ids from created instance 生成的文本或业务键。
     */
    private Map<Long, String> taskIdsFromCreatedInstance(ApprovalInstanceDO instance,
                                                         List<ApprovalTaskDO> localTasks,
                                                         String instanceCode) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
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
        // 遍历候选数据并按业务规则筛选、转换或聚合。
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
        // 汇总前面计算出的状态和明细，返回给调用方。
        return values;
    }

    /**
     * 执行 uniqueTaskIdsByUserId 流程，围绕 unique task ids by user id 完成校验、计算或结果组装。
     *
     * @param tasks tasks 参数，用于 uniqueTaskIdsByUserId 流程中的校验、计算或对象转换。
     * @return 返回 unique task ids by user id 生成的文本或业务键。
     */
    private Map<String, String> uniqueTaskIdsByUserId(List<LarkApprovalTaskSnapshot> tasks) {
        Map<String, String> values = new LinkedHashMap<>();
        Map<String, Boolean> duplicates = new LinkedHashMap<>();
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        for (LarkApprovalTaskSnapshot task : safeList(tasks)) {
            String userId = trimToNull(task.userId());
            String taskId = trimToNull(task.taskId());
            // 校验关键输入和前置条件，避免无效状态继续进入主流程。
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
        // 汇总前面计算出的状态和明细，返回给调用方。
        return values;
    }

    /**
     * 执行数据写入或状态变更。
     *
     * @param definition definition 参数，用于 createInstanceIfRequested 流程中的校验、计算或对象转换。
     * @param instance instance 参数，用于 createInstanceIfRequested 流程中的校验、计算或对象转换。
     * @param createNode create node 参数，用于 createInstanceIfRequested 流程中的校验、计算或对象转换。
     * @return 返回流程执行后的业务结果。
     */
    private String createInstanceIfRequested(ApprovalDefinitionDO definition,
                                             ApprovalInstanceDO instance,
                                             JsonNode createNode) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
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
        // 汇总前面计算出的状态和明细，返回给调用方。
        return trimToNull(client.createInstance(new LarkApprovalCreateInstanceRequest(
                instance == null ? null : instance.getTenantId(),
                approvalCode,
                createUuid(instance),
                openId,
                userId,
                text(createNode, "departmentId"),
                form)));
    }

    /**
     * 执行数据写入或状态变更。
     *
     * @param instance instance 参数，用于 createUuid 流程中的校验、计算或对象转换。
     * @return 返回流程执行后的业务结果。
     */
    private String createUuid(ApprovalInstanceDO instance) {
        if (instance == null || instance.getId() == null) {
            return null;
        }
        return "canvas-approval-" + instance.getId();
    }

    /**
     * 执行 text 流程，围绕 text 完成校验、计算或结果组装。
     *
     * @param node node 参数，用于 text 流程中的校验、计算或对象转换。
     * @param field 待处理业务值，用于规则计算、转换或外部调用。
     * @return 返回 text 生成的文本或业务键。
     */
    private String text(JsonNode node, String field) {
        // 准备本次处理所需的上下文和中间变量。
        String direct = trimToNull(node.path(field).asText(null));
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
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
        // 汇总前面计算出的状态和明细，返回给调用方。
        return null;
    }

    /**
     * 校验并获取必需参数、资源或权限。
     *
     * @param raw raw 参数，用于 required 流程中的校验、计算或对象转换。
     * @param message 原因或消息文本，用于记录状态变化的业务依据。
     * @return 返回 required 生成的文本或业务键。
     */
    private String required(String raw, String message) {
        String value = trimToNull(raw);
        if (value == null) {
            throw new IllegalStateException(message);
        }
        return value;
    }

    /**
     * 组装输出结构或完成对象转换。
     *
     * @param raw raw 参数，用于 mapInstanceStatus 流程中的校验、计算或对象转换。
     * @return 返回组装或转换后的结果对象。
     */
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

    /**
     * 组装输出结构或完成对象转换。
     *
     * @param raw raw 参数，用于 mapTaskStatus 流程中的校验、计算或对象转换。
     * @return 返回组装或转换后的结果对象。
     */
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

    /**
     * 规范化输入值。
     *
     * @param raw raw 参数，用于 normalize 流程中的校验、计算或对象转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private String normalize(String raw) {
        String value = trimToNull(raw);
        return value == null ? null : value.toUpperCase(Locale.ROOT);
    }

    /**
     * 解析、归一化或保护输入值，生成安全可用的中间结果。
     *
     * @param raw raw 参数，用于 trimToNull 流程中的校验、计算或对象转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private String trimToNull(String raw) {
        if (raw == null) {
            return null;
        }
        String value = raw.trim();
        return value.isEmpty() ? null : value;
    }

    /**
     * 按安全边界裁剪或保护输入值。
     *
     * @param rows rows 参数，用于 safeList 流程中的校验、计算或对象转换。
     * @return 返回 safe list 汇总后的集合、分页或映射视图。
     */
    private <T> List<T> safeList(List<T> rows) {
        return rows == null ? List.of() : rows;
    }
}
