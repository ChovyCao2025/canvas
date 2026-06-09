package org.chovy.canvas.domain.approval;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.common.enums.VersionStatus;
import org.chovy.canvas.dal.dataobject.CanvasDO;
import org.chovy.canvas.dal.dataobject.CanvasProjectDO;
import org.chovy.canvas.dal.dataobject.CanvasProjectFolderDO;
import org.chovy.canvas.dal.dataobject.CanvasProjectMemberDO;
import org.chovy.canvas.dal.dataobject.CanvasVersionDO;
import org.chovy.canvas.dal.mapper.CanvasMapper;
import org.chovy.canvas.dal.mapper.CanvasProjectFolderMapper;
import org.chovy.canvas.dal.mapper.CanvasProjectMapper;
import org.chovy.canvas.dal.mapper.CanvasProjectMemberMapper;
import org.chovy.canvas.dal.mapper.CanvasVersionMapper;
import org.chovy.canvas.domain.canvas.CanvasService;
import org.chovy.canvas.domain.project.CanvasProjectRole;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * CanvasPublishApprovalService 编排 domain.approval 场景的领域业务规则。
 */
@Service
public class CanvasPublishApprovalService {

    public static final String DEFINITION_KEY = "CANVAS_PUBLISH_DEFAULT";
    public static final String DOMAIN = "CANVAS";
    public static final String TARGET_TYPE = "CANVAS";
    public static final String AUTO_ACTION = "PUBLISH_CANVAS";

    private static final String ROLE_TENANT_ADMIN = "tenant_admin";

    private final CanvasMapper canvasMapper;
    private final CanvasVersionMapper canvasVersionMapper;
    private final CanvasProjectFolderMapper projectFolderMapper;
    private final CanvasProjectMapper projectMapper;
    private final CanvasProjectMemberMapper projectMemberMapper;
    private final ApprovalWorkflowService workflowService;
    private final CanvasService canvasService;
    private final ApprovalLarkUserIdentityResolver larkUserIdentityResolver;
    private final ObjectMapper objectMapper;

    /**
     * 创建 CanvasPublishApprovalService 实例并注入 domain.approval 场景依赖。
     * @param canvasMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param canvasVersionMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param projectFolderMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param projectMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param projectMemberMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param workflowService 依赖组件，用于完成数据访问或外部能力调用。
     * @param canvasService 依赖组件，用于完成数据访问或外部能力调用。
     * @param larkUserIdentityResolver 依赖组件，用于完成数据访问、计算或外部能力调用。
     */
    public CanvasPublishApprovalService(CanvasMapper canvasMapper,
                                        CanvasVersionMapper canvasVersionMapper,
                                        CanvasProjectFolderMapper projectFolderMapper,
                                        CanvasProjectMapper projectMapper,
                                        CanvasProjectMemberMapper projectMemberMapper,
                                        ApprovalWorkflowService workflowService,
                                        CanvasService canvasService,
                                        ApprovalLarkUserIdentityResolver larkUserIdentityResolver) {
        this(canvasMapper, canvasVersionMapper, projectFolderMapper, projectMapper, projectMemberMapper,
                /**
                 * 执行 ObjectMapper 流程，围绕 object mapper 完成校验、计算或结果组装。
                 *
                 * @return 返回 ObjectMapper 流程生成的业务结果。
                 */
                workflowService, canvasService, larkUserIdentityResolver, new ObjectMapper());
    }

    /**
     * 判断业务条件是否成立。
     *
     * @param canvasMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param canvasVersionMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param projectFolderMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param projectMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param projectMemberMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param workflowService 依赖组件，用于完成数据访问或外部能力调用。
     * @param canvasService 依赖组件，用于完成数据访问或外部能力调用。
     */
    CanvasPublishApprovalService(CanvasMapper canvasMapper,
                                 CanvasVersionMapper canvasVersionMapper,
                                 CanvasProjectFolderMapper projectFolderMapper,
                                 CanvasProjectMapper projectMapper,
                                 CanvasProjectMemberMapper projectMemberMapper,
                                 ApprovalWorkflowService workflowService,
                                 CanvasService canvasService) {
        this(canvasMapper, canvasVersionMapper, projectFolderMapper, projectMapper, projectMemberMapper,
                /**
                 * 执行 ObjectMapper 流程，围绕 object mapper 完成校验、计算或结果组装。
                 *
                 * @return 返回 ObjectMapper 流程生成的业务结果。
                 */
                workflowService, canvasService, null, new ObjectMapper());
    }

    /**
     * 判断业务条件是否成立。
     *
     * @param canvasMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param canvasVersionMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param projectFolderMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param projectMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param projectMemberMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param workflowService 依赖组件，用于完成数据访问或外部能力调用。
     * @param canvasService 依赖组件，用于完成数据访问或外部能力调用。
     * @param larkUserIdentityResolver 依赖组件，用于完成数据访问、计算或外部能力调用。
     * @param objectMapper 依赖组件，用于完成数据访问或外部能力调用。
     */
    CanvasPublishApprovalService(CanvasMapper canvasMapper,
                                 CanvasVersionMapper canvasVersionMapper,
                                 CanvasProjectFolderMapper projectFolderMapper,
                                 CanvasProjectMapper projectMapper,
                                 CanvasProjectMemberMapper projectMemberMapper,
                                 ApprovalWorkflowService workflowService,
                                 CanvasService canvasService,
                                 ApprovalLarkUserIdentityResolver larkUserIdentityResolver,
                                 ObjectMapper objectMapper) {
        this.canvasMapper = canvasMapper;
        this.canvasVersionMapper = canvasVersionMapper;
        this.projectFolderMapper = projectFolderMapper;
        this.projectMapper = projectMapper;
        this.projectMemberMapper = projectMemberMapper;
        this.workflowService = workflowService;
        this.canvasService = canvasService;
        this.larkUserIdentityResolver = larkUserIdentityResolver;
        this.objectMapper = objectMapper;
    }

    /**
     * 为 Canvas 当前草稿提交发布审批。
     * 方法会校验 Canvas 租户归属和草稿存在性，关闭同目标旧的待审流程，生成风险快照、审批人列表和自动发布动作后提交审批。
     */
    public ApprovalInstanceView submitReview(Long tenantId,
                                             Long canvasId,
                                             String submitter,
                                             CanvasPublishApprovalRequest request) {
        // 准备本次处理所需的上下文和中间变量。
        Evaluation evaluation = evaluate(tenantId, canvasId);
        String actor = defaultActor(submitter);
        workflowService.cancelOpen(evaluation.canvas().getTenantId(), TARGET_TYPE,
                String.valueOf(evaluation.canvas().getId()), actor, "new canvas publish review submitted");
        // 汇总前面计算出的状态和明细，返回给调用方。
        return workflowService.submit(new ApprovalSubmitCommand(
                evaluation.canvas().getTenantId(),
                DEFINITION_KEY,
                DOMAIN,
                TARGET_TYPE,
                String.valueOf(evaluation.canvas().getId()),
                evaluation.draft().getId(),
                actor,
                request == null ? null : request.reason(),
                evaluation.riskLevel(),
                json(evaluation.riskReasons()),
                snapshot(evaluation, actor, request),
                approvers(evaluation),
                null,
                AUTO_ACTION));
    }

    /**
     * 查询 Canvas 当前草稿的发布审批状态。
     * 返回是否需要审批、风险原因以及当前草稿最近一次通过审批记录，用于发布按钮和审批提示判断。
     */
    public CanvasPublishApprovalStatusView approvalStatus(Long tenantId, Long canvasId) {
        Evaluation evaluation = evaluate(tenantId, canvasId);
        ApprovalInstanceView latestApproved = workflowService.latestApproved(
                evaluation.canvas().getTenantId(),
                DEFINITION_KEY,
                TARGET_TYPE,
                String.valueOf(evaluation.canvas().getId()),
                evaluation.draft().getId());
        return new CanvasPublishApprovalStatusView(
                evaluation.canvas().getId(),
                evaluation.draft().getId(),
                evaluation.required(),
                evaluation.riskLevel(),
                evaluation.riskReasons(),
                latestApproved);
    }

    /**
     * 在发布前执行审批门禁并发布 Canvas。
     * 如果当前草稿存在风险且没有对应版本的已通过审批会拒绝发布；通过门禁后调用 Canvas 发布逻辑并产生版本状态变更。
     */
    public CanvasVersionDO publishWithApprovalGate(Long tenantId, Long canvasId, String operator) {
        Evaluation evaluation = evaluate(tenantId, canvasId);
        if (evaluation.required()) {
            ApprovalInstanceView approved = workflowService.latestApproved(
                    evaluation.canvas().getTenantId(),
                    DEFINITION_KEY,
                    TARGET_TYPE,
                    String.valueOf(evaluation.canvas().getId()),
                    evaluation.draft().getId());
            if (approved == null) {
                throw new CanvasPublishApprovalRequiredException(
                        "canvas publish approval is required for current draft " + evaluation.draft().getId());
            }
        }
        return canvasService.publish(canvasId, defaultActor(operator));
    }

    /**
     * 根据输入和依赖数据计算业务判断结果。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param canvasId 业务对象 ID，用于定位具体记录。
     * @return 返回 evaluate 流程生成的业务结果。
     */
    private Evaluation evaluate(Long tenantId, Long canvasId) {
        CanvasDO canvas = canvasMapper.selectById(requireId(canvasId, "canvasId"));
        if (canvas == null) {
            throw new IllegalArgumentException("canvas not found: " + canvasId);
        }
        Long scopedTenantId = tenantId == null ? canvas.getTenantId() : tenantId;
        if (scopedTenantId != null && canvas.getTenantId() != null && !Objects.equals(scopedTenantId, canvas.getTenantId())) {
            throw new AccessDeniedException("canvas tenant access denied");
        }
        CanvasVersionDO draft = latestDraft(canvas);
        CanvasProjectFolderDO assignment = projectFolder(canvas);
        CanvasProjectDO project = assignment == null || assignment.getProjectId() == null
                ? null
                /**
                 * 执行 project 流程，围绕 project 完成校验、计算或结果组装。
                 *
                 * @return 返回 project 流程生成的业务结果。
                 */
                : project(canvas.getTenantId(), assignment.getProjectId());
        List<String> reasons = riskReasons(canvas, draft, project);
        return new Evaluation(canvas, draft, assignment, project, reasons, !reasons.isEmpty());
    }

    /**
     * 查询并组装符合条件的业务数据。
     *
     * @param canvas canvas 参数，用于 latestDraft 流程中的校验、计算或对象转换。
     * @return 返回 latestDraft 流程生成的业务结果。
     */
    private CanvasVersionDO latestDraft(CanvasDO canvas) {
        CanvasVersionDO draft = canvasVersionMapper.selectOne(new LambdaQueryWrapper<CanvasVersionDO>()
                .eq(CanvasVersionDO::getCanvasId, canvas.getId())
                .eq(canvas.getTenantId() != null, CanvasVersionDO::getTenantId, canvas.getTenantId())
                .eq(CanvasVersionDO::getStatus, VersionStatus.DRAFT.getCode())
                .orderByDesc(CanvasVersionDO::getVersion)
                .orderByDesc(CanvasVersionDO::getId)
                .last("LIMIT 1"));
        if (draft == null || draft.getId() == null) {
            throw new IllegalStateException("canvas has no draft to approve: " + canvas.getId());
        }
        return draft;
    }

    /**
     * 执行 projectFolder 流程，围绕 project folder 完成校验、计算或结果组装。
     *
     * @param canvas canvas 参数，用于 projectFolder 流程中的校验、计算或对象转换。
     * @return 返回 projectFolder 流程生成的业务结果。
     */
    private CanvasProjectFolderDO projectFolder(CanvasDO canvas) {
        if (projectFolderMapper == null) {
            return null;
        }
        return projectFolderMapper.selectOne(new LambdaQueryWrapper<CanvasProjectFolderDO>()
                .eq(CanvasProjectFolderDO::getTenantId, canvas.getTenantId())
                .eq(CanvasProjectFolderDO::getCanvasId, canvas.getId())
                .last("LIMIT 1"));
    }

    /**
     * 执行 project 流程，围绕 project 完成校验、计算或结果组装。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param projectId 业务对象 ID，用于定位具体记录。
     * @return 返回 project 流程生成的业务结果。
     */
    private CanvasProjectDO project(Long tenantId, Long projectId) {
        if (projectMapper == null) {
            return null;
        }
        return projectMapper.selectOne(new LambdaQueryWrapper<CanvasProjectDO>()
                .eq(CanvasProjectDO::getTenantId, tenantId)
                .eq(CanvasProjectDO::getId, projectId)
                .last("LIMIT 1"));
    }

    /**
     * 执行 riskReasons 流程，围绕 risk reasons 完成校验、计算或结果组装。
     *
     * @param canvas canvas 参数，用于 riskReasons 流程中的校验、计算或对象转换。
     * @param draft draft 参数，用于 riskReasons 流程中的校验、计算或对象转换。
     * @param project project 参数，用于 riskReasons 流程中的校验、计算或对象转换。
     * @return 返回 risk reasons 汇总后的集合、分页或映射视图。
     */
    private List<String> riskReasons(CanvasDO canvas, CanvasVersionDO draft, CanvasProjectDO project) {
        // 准备本次处理所需的上下文和中间变量。
        List<String> reasons = new ArrayList<>();
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (project != null && Integer.valueOf(1).equals(project.getRequireReviewBeforePublish())) {
            reasons.add("PROJECT_REQUIRES_REVIEW");
        }
        if (canvas.getMaxTotalExecutions() == null) {
            reasons.add("UNLIMITED_TOTAL_CAP");
        }
        String graph = draft.getGraphJson() == null ? "" : draft.getGraphJson();
        String normalized = graph.toLowerCase(Locale.ROOT);
        if (normalized.contains("groovy") || normalized.contains("script")) {
            reasons.add("CUSTOM_SCRIPT_NODE");
        }
        if (normalized.contains("coupon")
                || normalized.contains("benefit")
                || normalized.contains("voucher")
                || normalized.contains("权益")) {
            reasons.add("COUPON_OR_BENEFIT_NODE");
        }
        // 汇总前面计算出的状态和明细，返回给调用方。
        return List.copyOf(reasons);
    }

    /**
     * 执行业务决策动作，并同步后续状态。
     *
     * @param evaluation evaluation 参数，用于 approvers 流程中的校验、计算或对象转换。
     * @return 返回流程执行后的业务结果。
     */
    private List<String> approvers(Evaluation evaluation) {
        Set<String> approvers = new LinkedHashSet<>();
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (evaluation.assignment() != null && evaluation.assignment().getProjectId() != null && projectMemberMapper != null) {
            // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
            List<CanvasProjectMemberDO> members = projectMemberMapper.selectList(new LambdaQueryWrapper<CanvasProjectMemberDO>()
                    .eq(CanvasProjectMemberDO::getTenantId, evaluation.canvas().getTenantId())
                    .eq(CanvasProjectMemberDO::getProjectId, evaluation.assignment().getProjectId())
                    .eq(CanvasProjectMemberDO::getRole, CanvasProjectRole.PROJECT_ADMIN.name())
                    .orderByAsc(CanvasProjectMemberDO::getUsername));
            // 遍历候选数据并按业务规则筛选、转换或聚合。
            for (CanvasProjectMemberDO member : members == null ? List.<CanvasProjectMemberDO>of() : members) {
                String username = trimToNull(member.getUsername());
                if (username != null) {
                    approvers.add(username);
                }
            }
        }
        if (approvers.isEmpty()) {
            approvers.add(ROLE_TENANT_ADMIN);
        }
        return List.copyOf(approvers);
    }

    /**
     * 查询并组装符合条件的业务数据。
     *
     * @param evaluation evaluation 参数，用于 snapshot 流程中的校验、计算或对象转换。
     * @param submitter submitter 参数，用于 snapshot 流程中的校验、计算或对象转换。
     * @param request 请求对象，承载本次操作的输入参数。
     * @return 返回 snapshot 生成的文本或业务键。
     */
    private String snapshot(Evaluation evaluation, String submitter, CanvasPublishApprovalRequest request) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("canvasId", evaluation.canvas().getId());
        snapshot.put("canvasName", evaluation.canvas().getName());
        snapshot.put("tenantId", evaluation.canvas().getTenantId());
        snapshot.put("draftVersionId", evaluation.draft().getId());
        snapshot.put("draftVersion", evaluation.draft().getVersion());
        snapshot.put("projectId", evaluation.assignment() == null ? null : evaluation.assignment().getProjectId());
        snapshot.put("projectKey", evaluation.project() == null ? null : evaluation.project().getProjectKey());
        snapshot.put("riskReasons", evaluation.riskReasons());
        snapshot.put("graphJson", evaluation.draft().getGraphJson());
        snapshot.put("lark", larkBinding(evaluation, submitter, request));
        return json(snapshot);
    }

    /**
     * 执行 larkBinding 流程，围绕 lark binding 完成校验、计算或结果组装。
     *
     * @param evaluation evaluation 参数，用于 larkBinding 流程中的校验、计算或对象转换。
     * @param submitter submitter 参数，用于 larkBinding 流程中的校验、计算或对象转换。
     * @param request 请求对象，承载本次操作的输入参数。
     * @return 返回 larkBinding 流程生成的业务结果。
     */
    private Map<String, Object> larkBinding(Evaluation evaluation,
                                            String submitter,
                                            CanvasPublishApprovalRequest request) {
        ApprovalLarkUserIdentity mapped = larkUserIdentityResolver == null
                ? null
                : larkUserIdentityResolver.resolve(evaluation.canvas().getTenantId(), submitter);
        Map<String, Object> create = new LinkedHashMap<>();
        create.put("form", larkForm(evaluation, request == null ? null : request.reason()));
        putIfPresent(create, "openId", firstPresent(
                request == null ? null : request.larkOpenId(),
                mapped == null ? null : mapped.openId()));
        putIfPresent(create, "userId", firstPresent(
                request == null ? null : request.larkUserId(),
                mapped == null ? null : mapped.userId()));
        putIfPresent(create, "departmentId", firstPresent(
                request == null ? null : request.larkDepartmentId(),
                mapped == null ? null : mapped.departmentId()));

        Map<String, Object> lark = new LinkedHashMap<>();
        lark.put("create", create);
        return lark;
    }

    /**
     * 执行 firstPresent 流程，围绕 first present 完成校验、计算或结果组装。
     *
     * @param primary primary 参数，用于 firstPresent 流程中的校验、计算或对象转换。
     * @param fallback fallback 参数，用于 firstPresent 流程中的校验、计算或对象转换。
     * @return 返回 first present 生成的文本或业务键。
     */
    private String firstPresent(String primary, String fallback) {
        String value = trimToNull(primary);
        return value == null ? trimToNull(fallback) : value;
    }

    /**
     * 处理集合、映射或字段拷贝逻辑。
     *
     * @param String string 参数，用于 putIfPresent 流程中的校验、计算或对象转换。
     * @param target target 参数，用于 putIfPresent 流程中的校验、计算或对象转换。
     * @param key 业务键，用于在同一租户下定位资源。
     * @param raw raw 参数，用于 putIfPresent 流程中的校验、计算或对象转换。
     */
    private void putIfPresent(Map<String, Object> target, String key, String raw) {
        String value = trimToNull(raw);
        if (value != null) {
            target.put(key, value);
        }
    }

    /**
     * 执行 larkForm 流程，围绕 lark form 完成校验、计算或结果组装。
     *
     * @param evaluation evaluation 参数，用于 larkForm 流程中的校验、计算或对象转换。
     * @param reason 原因说明，用于记录状态变化的业务依据。
     * @return 返回 lark form 生成的文本或业务键。
     */
    private String larkForm(Evaluation evaluation, String reason) {
        List<Map<String, Object>> fields = new ArrayList<>();
        fields.add(larkFormField("canvas_name", "input", evaluation.canvas().getName()));
        fields.add(larkFormField("canvas_id", "input", String.valueOf(evaluation.canvas().getId())));
        fields.add(larkFormField("draft_version", "input", String.valueOf(evaluation.draft().getVersion())));
        fields.add(larkFormField("submit_reason", "textarea", trimToNull(reason)));
        fields.add(larkFormField("project_key", "input",
                evaluation.project() == null ? null : evaluation.project().getProjectKey()));
        fields.add(larkFormField("risk_level", "input", evaluation.riskLevel()));
        fields.add(larkFormField("risk_reasons", "textarea", String.join("\n", evaluation.riskReasons())));
        fields.add(larkFormField("graph_json", "textarea", evaluation.draft().getGraphJson()));
        return json(fields);
    }

    /**
     * 执行 larkFormField 流程，围绕 lark form field 完成校验、计算或结果组装。
     *
     * @param id 业务对象 ID，用于定位具体记录。
     * @param type 类型标识，用于选择对应处理分支。
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 larkFormField 流程生成的业务结果。
     */
    private Map<String, Object> larkFormField(String id, String type, String value) {
        Map<String, Object> field = new LinkedHashMap<>();
        field.put("id", id);
        field.put("type", type);
        field.put("value", value == null ? "" : value);
        return field;
    }

    /**
     * 处理 JSON 序列化或反序列化。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 json 生成的文本或业务键。
     */
    private String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("failed to serialize approval payload", ex);
        }
    }

    /**
     * 校验并获取必需参数、资源或权限。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param field 待处理业务值，用于规则计算、转换或外部调用。
     * @return 返回 require id 计算得到的数量、金额或指标值。
     */
    private Long requireId(Long value, String field) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value;
    }

    /**
     * 解析操作人标识。
     *
     * @param raw raw 参数，用于 defaultActor 流程中的校验、计算或对象转换。
     * @return 返回 default actor 生成的文本或业务键。
     */
    private String defaultActor(String raw) {
        String value = trimToNull(raw);
        return value == null ? "system" : value;
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
     * Evaluation 数据记录。
     */
    private record Evaluation(CanvasDO canvas,
                              CanvasVersionDO draft,
                              CanvasProjectFolderDO assignment,
                              CanvasProjectDO project,
                              List<String> riskReasons,
                              boolean required) {
        /**
         * 执行 riskLevel 流程，围绕 risk level 完成校验、计算或结果组装。
         *
         * @return 返回 risk level 生成的文本或业务键。
         */
        private String riskLevel() {
            return riskReasons.isEmpty() ? "LOW" : "HIGH";
        }
    }

    /**
     * CanvasPublishApprovalRequiredException 承载对应领域的业务规则、流程编排和结果转换。
     */
    public static class CanvasPublishApprovalRequiredException extends RuntimeException {
        /**
         * 创建 CanvasPublishApprovalRequiredException 实例并注入 domain.approval 场景依赖。
         * @param message 原因或消息文本，用于记录状态变化的业务依据。
         */
        public CanvasPublishApprovalRequiredException(String message) {
            super(message);
        }
    }
}
