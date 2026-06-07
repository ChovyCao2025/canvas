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

    public CanvasPublishApprovalService(CanvasMapper canvasMapper,
                                        CanvasVersionMapper canvasVersionMapper,
                                        CanvasProjectFolderMapper projectFolderMapper,
                                        CanvasProjectMapper projectMapper,
                                        CanvasProjectMemberMapper projectMemberMapper,
                                        ApprovalWorkflowService workflowService,
                                        CanvasService canvasService,
                                        ApprovalLarkUserIdentityResolver larkUserIdentityResolver) {
        this(canvasMapper, canvasVersionMapper, projectFolderMapper, projectMapper, projectMemberMapper,
                workflowService, canvasService, larkUserIdentityResolver, new ObjectMapper());
    }

    CanvasPublishApprovalService(CanvasMapper canvasMapper,
                                 CanvasVersionMapper canvasVersionMapper,
                                 CanvasProjectFolderMapper projectFolderMapper,
                                 CanvasProjectMapper projectMapper,
                                 CanvasProjectMemberMapper projectMemberMapper,
                                 ApprovalWorkflowService workflowService,
                                 CanvasService canvasService) {
        this(canvasMapper, canvasVersionMapper, projectFolderMapper, projectMapper, projectMemberMapper,
                workflowService, canvasService, null, new ObjectMapper());
    }

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

    public ApprovalInstanceView submitReview(Long tenantId,
                                             Long canvasId,
                                             String submitter,
                                             CanvasPublishApprovalRequest request) {
        Evaluation evaluation = evaluate(tenantId, canvasId);
        String actor = defaultActor(submitter);
        workflowService.cancelOpen(evaluation.canvas().getTenantId(), TARGET_TYPE,
                String.valueOf(evaluation.canvas().getId()), actor, "new canvas publish review submitted");
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
                : project(canvas.getTenantId(), assignment.getProjectId());
        List<String> reasons = riskReasons(canvas, draft, project);
        return new Evaluation(canvas, draft, assignment, project, reasons, !reasons.isEmpty());
    }

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

    private CanvasProjectFolderDO projectFolder(CanvasDO canvas) {
        if (projectFolderMapper == null) {
            return null;
        }
        return projectFolderMapper.selectOne(new LambdaQueryWrapper<CanvasProjectFolderDO>()
                .eq(CanvasProjectFolderDO::getTenantId, canvas.getTenantId())
                .eq(CanvasProjectFolderDO::getCanvasId, canvas.getId())
                .last("LIMIT 1"));
    }

    private CanvasProjectDO project(Long tenantId, Long projectId) {
        if (projectMapper == null) {
            return null;
        }
        return projectMapper.selectOne(new LambdaQueryWrapper<CanvasProjectDO>()
                .eq(CanvasProjectDO::getTenantId, tenantId)
                .eq(CanvasProjectDO::getId, projectId)
                .last("LIMIT 1"));
    }

    private List<String> riskReasons(CanvasDO canvas, CanvasVersionDO draft, CanvasProjectDO project) {
        List<String> reasons = new ArrayList<>();
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
        return List.copyOf(reasons);
    }

    private List<String> approvers(Evaluation evaluation) {
        Set<String> approvers = new LinkedHashSet<>();
        if (evaluation.assignment() != null && evaluation.assignment().getProjectId() != null && projectMemberMapper != null) {
            List<CanvasProjectMemberDO> members = projectMemberMapper.selectList(new LambdaQueryWrapper<CanvasProjectMemberDO>()
                    .eq(CanvasProjectMemberDO::getTenantId, evaluation.canvas().getTenantId())
                    .eq(CanvasProjectMemberDO::getProjectId, evaluation.assignment().getProjectId())
                    .eq(CanvasProjectMemberDO::getRole, CanvasProjectRole.PROJECT_ADMIN.name())
                    .orderByAsc(CanvasProjectMemberDO::getUsername));
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

    private String firstPresent(String primary, String fallback) {
        String value = trimToNull(primary);
        return value == null ? trimToNull(fallback) : value;
    }

    private void putIfPresent(Map<String, Object> target, String key, String raw) {
        String value = trimToNull(raw);
        if (value != null) {
            target.put(key, value);
        }
    }

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

    private Map<String, Object> larkFormField(String id, String type, String value) {
        Map<String, Object> field = new LinkedHashMap<>();
        field.put("id", id);
        field.put("type", type);
        field.put("value", value == null ? "" : value);
        return field;
    }

    private String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("failed to serialize approval payload", ex);
        }
    }

    private Long requireId(Long value, String field) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value;
    }

    private String defaultActor(String raw) {
        String value = trimToNull(raw);
        return value == null ? "system" : value;
    }

    private String trimToNull(String raw) {
        if (raw == null) {
            return null;
        }
        String value = raw.trim();
        return value.isEmpty() ? null : value;
    }

    private record Evaluation(CanvasDO canvas,
                              CanvasVersionDO draft,
                              CanvasProjectFolderDO assignment,
                              CanvasProjectDO project,
                              List<String> riskReasons,
                              boolean required) {
        private String riskLevel() {
            return riskReasons.isEmpty() ? "LOW" : "HIGH";
        }
    }

    public static class CanvasPublishApprovalRequiredException extends RuntimeException {
        public CanvasPublishApprovalRequiredException(String message) {
            super(message);
        }
    }
}
