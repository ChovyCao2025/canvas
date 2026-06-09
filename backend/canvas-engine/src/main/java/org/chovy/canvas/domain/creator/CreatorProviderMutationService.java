package org.chovy.canvas.domain.creator;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.dal.dataobject.CreatorCampaignDO;
import org.chovy.canvas.dal.dataobject.CreatorCollaborationDO;
import org.chovy.canvas.dal.dataobject.CreatorDeliverableDO;
import org.chovy.canvas.dal.dataobject.CreatorProfileDO;
import org.chovy.canvas.dal.dataobject.CreatorProviderMutationDO;
import org.chovy.canvas.dal.mapper.CreatorCampaignMapper;
import org.chovy.canvas.dal.mapper.CreatorCollaborationMapper;
import org.chovy.canvas.dal.mapper.CreatorDeliverableMapper;
import org.chovy.canvas.dal.mapper.CreatorProfileMapper;
import org.chovy.canvas.dal.mapper.CreatorProviderMutationMapper;
import org.chovy.canvas.domain.providerwrite.ProviderWriteEvidenceSanitizer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * CreatorProviderMutationService 编排 domain.creator 场景的领域业务规则。
 */
@Service
public class CreatorProviderMutationService {

    private static final TypeReference<Map<String, Object>> OBJECT_MAP = new TypeReference<>() {
    };
    private static final Set<String> MUTATION_TYPES = Set.of(
            "PUBLISH_BRIEF",
            "INVITE_CREATOR",
            "GENERATE_AFFILIATE_LINK",
            "CREATE_DISCOUNT_CODE",
            "REQUEST_CONTENT_AUTHORIZATION",
            "SYNC_DELIVERABLE_STATUS");
    private static final Set<String> SECRET_KEYS = Set.of(
            "token",
            "access_token",
            "refresh_token",
            "client_secret",
            "api_key",
            "apikey",
            "password");

    private final CreatorCampaignMapper campaignMapper;
    private final CreatorCollaborationMapper collaborationMapper;
    private final CreatorDeliverableMapper deliverableMapper;
    private final CreatorProfileMapper profileMapper;
    private final CreatorProviderMutationMapper mutationMapper;
    private final ObjectMapper objectMapper;
    private final CreatorProviderWriteGateway gateway;
    private final Clock clock;

    /**
     * 创建 CreatorProviderMutationService 实例并注入 domain.creator 场景依赖。
     * @param campaignMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param collaborationMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param deliverableMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param profileMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param mutationMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param objectMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param gateway gateway 参数，用于 CreatorProviderMutationService 流程中的校验、计算或对象转换。
     */
    @Autowired
    public CreatorProviderMutationService(CreatorCampaignMapper campaignMapper,
                                          CreatorCollaborationMapper collaborationMapper,
                                          CreatorDeliverableMapper deliverableMapper,
                                          CreatorProfileMapper profileMapper,
                                          CreatorProviderMutationMapper mutationMapper,
                                          ObjectMapper objectMapper,
                                          CreatorProviderWriteGateway gateway) {
        this(campaignMapper, collaborationMapper, deliverableMapper, profileMapper, mutationMapper, objectMapper,
                gateway, Clock.systemDefaultZone());
    }

    /**
     * 执行 CreatorProviderMutationService 流程，围绕 creator provider mutation service 完成校验、计算或结果组装。
     *
     * @param campaignMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param collaborationMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param deliverableMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param profileMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param mutationMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param objectMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param gateway gateway 参数，用于 CreatorProviderMutationService 流程中的校验、计算或对象转换。
     * @param clock 时间参数，用于计算窗口、过期或审计时间。
     */
    CreatorProviderMutationService(CreatorCampaignMapper campaignMapper,
                                   CreatorCollaborationMapper collaborationMapper,
                                   CreatorDeliverableMapper deliverableMapper,
                                   CreatorProfileMapper profileMapper,
                                   CreatorProviderMutationMapper mutationMapper,
                                   ObjectMapper objectMapper,
                                   CreatorProviderWriteGateway gateway,
                                   Clock clock) {
        this.campaignMapper = campaignMapper;
        this.collaborationMapper = collaborationMapper;
        this.deliverableMapper = deliverableMapper;
        this.profileMapper = profileMapper;
        this.mutationMapper = mutationMapper;
        this.objectMapper = objectMapper == null ? new ObjectMapper() : objectMapper;
        this.gateway = gateway == null ? CreatorProviderWriteGateway.unsupported() : gateway;
        this.clock = clock == null ? Clock.systemDefaultZone() : clock;
    }

    /**
     * 提交达人平台写操作提案。
     * 方法校验活动/合作/交付物租户关系和请求 payload，按 mutationKey 幂等保存草稿，并记录请求 hash 防止同 key 不同请求被覆盖。
     */
    public CreatorProviderMutationView propose(Long tenantId,
                                               CreatorProviderMutationCommand command,
                                               String actor) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (command == null) {
            throw new IllegalArgumentException("creator provider mutation command is required");
        }
        Long scopedTenantId = normalizeTenant(tenantId);
        CreatorCampaignDO campaign = campaign(scopedTenantId, command.campaignId());
        CreatorCollaborationDO collaboration = collaboration(scopedTenantId, command.collaborationId());
        CreatorDeliverableDO deliverable = deliverable(scopedTenantId, command.deliverableId());
        validateRelationship(campaign, collaboration, deliverable);
        CreatorProfileDO creator = creator(scopedTenantId, collaboration == null ? null : collaboration.getCreatorId());
        String mutationType = normalizeMutationType(command.mutationType());
        String entityType = normalizeUpper(command.entityType(), "entityType");
        Map<String, Object> payload = payload(command.payload());
        validatePayload(mutationType, entityType, collaboration, deliverable, payload);
        String payloadJson = json(payload);
        String requestHash = sha256(payloadJson);
        String mutationKey = required(command.mutationKey(), "mutationKey");
        String idempotencyKey = defaultString(command.idempotencyKey(), mutationKey);
        SearchExistingMutation existing = existingMutation(scopedTenantId, mutationKey);
        if (existing.row() != null) {
            if (!Objects.equals(requestHash, existing.row().getRequestHash())) {
                throw new IllegalArgumentException("creator provider mutation request hash conflicts with existing mutation key");
            }
            return toView(existing.row());
        }
        LocalDateTime changedAt = now();
        CreatorProviderMutationDO row = new CreatorProviderMutationDO();
        row.setTenantId(scopedTenantId);
        row.setCampaignId(campaign.getId());
        row.setCollaborationId(collaboration == null ? null : collaboration.getId());
        row.setDeliverableId(deliverable == null ? null : deliverable.getId());
        row.setCreatorId(creator == null ? null : creator.getId());
        row.setProvider(provider(creator, payload));
        row.setMutationKey(mutationKey);
        row.setMutationType(mutationType);
        row.setEntityType(entityType);
        row.setExternalEntityId(trimToNull(command.externalEntityId()));
        row.setRequestHash(requestHash);
        row.setIdempotencyKey(idempotencyKey);
        row.setStatus("DRAFT");
        row.setApprovalStatus("PENDING");
        row.setDryRunRequired(Boolean.FALSE.equals(command.dryRunRequired()) ? 0 : 1);
        row.setPayloadJson(payloadJson);
        row.setValidationJson(json(Map.of("validatedAt", changedAt.toString(), "mutationType", mutationType)));
        row.setCreatedBy(defaultString(actor, "system"));
        row.setCreatedAt(changedAt);
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        row.setUpdatedAt(changedAt);
        mutationMapper.insert(row);
        // 汇总前面计算出的状态和明细，返回给调用方。
        return toView(row);
    }

    /**
     * 审批达人平台写操作提案。
     * APPROVED 会把提案置为 READY，REJECTED 会取消提案，并记录审批人、时间和原因。
     */
    public CreatorProviderMutationView approve(Long tenantId,
                                               Long mutationId,
                                               CreatorProviderMutationApprovalCommand command,
                                               String actor) {
        CreatorProviderMutationDO row = mutation(normalizeTenant(tenantId), mutationId);
        String decision = normalizeUpper(command == null ? null : command.decision(), "decision");
        LocalDateTime changedAt = now();
        if ("APPROVED".equals(decision)) {
            rejectSelfApproval(row.getCreatedBy(), actor);
            row.setApprovalStatus("APPROVED");
            row.setStatus("READY");
        // 根据前序判断结果进入后续条件分支。
        } else if ("REJECTED".equals(decision)) {
            row.setApprovalStatus("REJECTED");
            row.setStatus("CANCELLED");
        } else {
            throw new IllegalArgumentException("decision must be APPROVED or REJECTED");
        }
        row.setApprovedBy(defaultString(actor, "system"));
        row.setApprovedAt(changedAt);
        row.setValidationJson(json(Map.of(
                "decision", decision,
                /**
                 * 按默认值规则处理输入值。
                 *
                 * @return 返回 defaultString 流程生成的业务结果。
                 */
                "reason", defaultString(command.reason(), ""),
                "decidedAt", changedAt.toString())));
        row.setUpdatedAt(changedAt);
        mutationMapper.updateById(row);
        return toView(row);
    }

    /**
     * 执行或干跑已审批的达人平台写操作。
     * 正式执行前可要求 DRY_RUN_OK；方法会调用 Provider gateway，并保存脱敏后的请求/响应证据和执行状态。
     */
    public CreatorProviderMutationView execute(Long tenantId,
                                               Long mutationId,
                                               CreatorProviderMutationExecuteCommand command,
                                               String actor) {
        Long scopedTenantId = normalizeTenant(tenantId);
        CreatorProviderMutationDO row = mutation(scopedTenantId, mutationId);
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (!"APPROVED".equals(row.getApprovalStatus())) {
            throw new IllegalStateException("creator provider mutation must be approved before execution");
        }
        boolean dryRun = command == null || !Boolean.FALSE.equals(command.dryRun());
        if (!dryRun && Integer.valueOf(1).equals(row.getDryRunRequired())
                && !"DRY_RUN_OK".equals(row.getStatus())) {
            throw new IllegalStateException("dry run must pass before applying creator provider mutation");
        }
        CreatorProviderMutationRequest request = new CreatorProviderMutationRequest(
                scopedTenantId,
                row.getCampaignId(),
                row.getCollaborationId(),
                row.getDeliverableId(),
                row.getCreatorId(),
                row.getProvider(),
                row.getMutationType(),
                row.getEntityType(),
                row.getExternalEntityId(),
                row.getIdempotencyKey(),
                dryRun,
                command == null || !Boolean.FALSE.equals(command.partialFailure()),
                map(row.getPayloadJson()),
                command == null || command.metadata() == null ? Map.of() : command.metadata());
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        CreatorProviderMutationResult result = gateway.execute(request);
        LocalDateTime changedAt = now();
        row.setProviderRequestJson(json(providerRequestEvidence(request)));
        row.setProviderResponseJson(json(ProviderWriteEvidenceSanitizer.sanitizeMap(result.response())));
        row.setErrorCode(result.errorCode());
        row.setErrorMessage(result.errorMessage());
        row.setExecutedBy(defaultString(actor, "system"));
        row.setExecutedAt(changedAt);
        row.setUpdatedAt(changedAt);
        if (result.success()) {
            row.setStatus(dryRun ? "DRY_RUN_OK" : "APPLIED");
        } else {
            row.setStatus(dryRun ? "DRY_RUN_FAILED" : "FAILED");
        }
        mutationMapper.updateById(row);
        // 汇总前面计算出的状态和明细，返回给调用方。
        return toView(row);
    }

    /**
     * 查询租户内达人平台写操作提案列表。
     * 可按活动、合作、执行状态和审批状态过滤，返回结果受 limit 限制。
     */
    public List<CreatorProviderMutationView> list(Long tenantId, CreatorProviderMutationQuery query) {
        Long scopedTenantId = normalizeTenant(tenantId);
        Long campaignId = query == null ? null : query.campaignId();
        Long collaborationId = query == null ? null : query.collaborationId();
        String status = query == null ? null : normalizeOptionalUpper(query.status());
        String approvalStatus = query == null ? null : normalizeOptionalUpper(query.approvalStatus());
        int limit = Math.min(Math.max(query == null || query.limit() == null ? 50 : query.limit(), 1), 100);
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        return safeList(mutationMapper.selectList(new LambdaQueryWrapper<CreatorProviderMutationDO>()
                .eq(CreatorProviderMutationDO::getTenantId, scopedTenantId)
                .eq(campaignId != null, CreatorProviderMutationDO::getCampaignId, campaignId)
                .eq(collaborationId != null, CreatorProviderMutationDO::getCollaborationId, collaborationId)
                .eq(status != null, CreatorProviderMutationDO::getStatus, status)
                .eq(approvalStatus != null, CreatorProviderMutationDO::getApprovalStatus, approvalStatus)
                .orderByDesc(CreatorProviderMutationDO::getUpdatedAt)
                // 遍历候选数据并按业务规则筛选、转换或聚合。
                .last("LIMIT " + limit))).stream()
                .filter(row -> scopedTenantId.equals(row.getTenantId()))
                .filter(row -> campaignId == null || Objects.equals(campaignId, row.getCampaignId()))
                .filter(row -> collaborationId == null || Objects.equals(collaborationId, row.getCollaborationId()))
                .filter(row -> status == null || status.equals(row.getStatus()))
                .filter(row -> approvalStatus == null || approvalStatus.equals(row.getApprovalStatus()))
                .limit(limit)
                .map(this::toView)
                .toList();
    }

    /**
     * 执行 campaign 流程，围绕 campaign 完成校验、计算或结果组装。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param campaignId 业务对象 ID，用于定位具体记录。
     * @return 返回 campaign 流程生成的业务结果。
     */
    private CreatorCampaignDO campaign(Long tenantId, Long campaignId) {
        CreatorCampaignDO campaign = campaignMapper.selectById(requiredId(campaignId, "campaignId"));
        validateTenant(tenantId, campaign == null ? null : campaign.getTenantId(), "campaign");
        return campaign;
    }

    /**
     * 执行 collaboration 流程，围绕 collaboration 完成校验、计算或结果组装。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param collaborationId 业务对象 ID，用于定位具体记录。
     * @return 返回 collaboration 流程生成的业务结果。
     */
    private CreatorCollaborationDO collaboration(Long tenantId, Long collaborationId) {
        if (collaborationId == null) {
            return null;
        }
        CreatorCollaborationDO collaboration = collaborationMapper.selectById(requiredId(collaborationId,
                "collaborationId"));
        validateTenant(tenantId, collaboration == null ? null : collaboration.getTenantId(), "collaboration");
        return collaboration;
    }

    /**
     * 执行 deliverable 流程，围绕 deliverable 完成校验、计算或结果组装。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param deliverableId 业务对象 ID，用于定位具体记录。
     * @return 返回 deliverable 流程生成的业务结果。
     */
    private CreatorDeliverableDO deliverable(Long tenantId, Long deliverableId) {
        if (deliverableId == null) {
            return null;
        }
        CreatorDeliverableDO deliverable = deliverableMapper.selectById(requiredId(deliverableId, "deliverableId"));
        validateTenant(tenantId, deliverable == null ? null : deliverable.getTenantId(), "deliverable");
        return deliverable;
    }

    /**
     * 执行 creator 流程，围绕 creator 完成校验、计算或结果组装。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param creatorId 业务对象 ID，用于定位具体记录。
     * @return 返回 creator 流程生成的业务结果。
     */
    private CreatorProfileDO creator(Long tenantId, Long creatorId) {
        if (creatorId == null) {
            return null;
        }
        CreatorProfileDO creator = profileMapper.selectById(requiredId(creatorId, "creatorId"));
        validateTenant(tenantId, creator == null ? null : creator.getTenantId(), "creator");
        return creator;
    }

    /**
     * 执行 mutation 流程，围绕 mutation 完成校验、计算或结果组装。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param mutationId 业务对象 ID，用于定位具体记录。
     * @return 返回 mutation 流程生成的业务结果。
     */
    private CreatorProviderMutationDO mutation(Long tenantId, Long mutationId) {
        CreatorProviderMutationDO row = mutationMapper.selectById(requiredId(mutationId, "mutationId"));
        validateTenant(tenantId, row == null ? null : row.getTenantId(), "mutation");
        return row;
    }

    /**
     * 校验输入、权限或业务前置条件。
     *
     * @param campaign campaign 参数，用于 validateRelationship 流程中的校验、计算或对象转换。
     * @param collaboration collaboration 参数，用于 validateRelationship 流程中的校验、计算或对象转换。
     * @param deliverable deliverable 参数，用于 validateRelationship 流程中的校验、计算或对象转换。
     */
    private void validateRelationship(CreatorCampaignDO campaign,
                                      CreatorCollaborationDO collaboration,
                                      CreatorDeliverableDO deliverable) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (collaboration != null && !Objects.equals(campaign.getId(), collaboration.getCampaignId())) {
            throw new IllegalArgumentException("collaboration does not belong to campaign");
        }
        if (deliverable != null) {
            if (!Objects.equals(campaign.getId(), deliverable.getCampaignId())) {
                throw new IllegalArgumentException("deliverable does not belong to campaign");
            }
            if (collaboration != null && !Objects.equals(collaboration.getId(), deliverable.getCollaborationId())) {
                throw new IllegalArgumentException("deliverable does not belong to collaboration");
            }
            if (collaboration != null && !Objects.equals(collaboration.getCreatorId(), deliverable.getCreatorId())) {
                throw new IllegalArgumentException("deliverable creator does not match collaboration");
            }
        }
    }

    /**
     * 校验输入、权限或业务前置条件。
     *
     * @param mutationType 类型标识，用于选择对应处理分支。
     * @param entityType 类型标识，用于选择对应处理分支。
     * @param collaboration collaboration 参数，用于 validatePayload 流程中的校验、计算或对象转换。
     * @param deliverable deliverable 参数，用于 validatePayload 流程中的校验、计算或对象转换。
     * @param payload 待处理业务值，用于规则计算、转换或外部调用。
     */
    private void validatePayload(String mutationType,
                                 String entityType,
                                 CreatorCollaborationDO collaboration,
                                 CreatorDeliverableDO deliverable,
                                 Map<String, Object> payload) {
        // 准备本次流程的上下文、默认值和中间结果。
        rejectProviderSecrets(payload);
        switch (mutationType) {
            case "PUBLISH_BRIEF" -> requireAnyPayloadText(payload, "briefUrl", "briefText");
            case "INVITE_CREATOR", "GENERATE_AFFILIATE_LINK" -> requireCollaboration(collaboration, mutationType);
            case "CREATE_DISCOUNT_CODE" -> {
                requireCollaboration(collaboration, mutationType);
                // 校验策略输入和默认值，避免无效配置进入持久化或查询流程。
                if (trimToNull(string(payload.get("discountCode"))) == null
                        && trimToNull(collaboration.getDiscountCode()) == null) {
                    throw new IllegalArgumentException("discountCode is required");
                }
            }
            case "REQUEST_CONTENT_AUTHORIZATION" -> {
                requireDeliverable(deliverable, mutationType);
                if (!"DELIVERABLE".equals(entityType)) {
                    throw new IllegalArgumentException("content authorization entity must be DELIVERABLE");
                }
                requireAnyPayloadText(payload, "sparkAuthorizationCode", "authorizationAccountId", "permissionToken");
            }
            case "SYNC_DELIVERABLE_STATUS" -> requireDeliverable(deliverable, mutationType);
            default -> throw new IllegalArgumentException("unsupported creator provider mutation type");
        }
    }

    /**
     * 校验并获取必需参数、资源或权限。
     *
     * @param collaboration collaboration 参数，用于 requireCollaboration 流程中的校验、计算或对象转换。
     * @param mutationType 类型标识，用于选择对应处理分支。
     */
    private void requireCollaboration(CreatorCollaborationDO collaboration, String mutationType) {
        if (collaboration == null) {
            throw new IllegalArgumentException(mutationType + " requires collaboration");
        }
    }

    /**
     * 校验并获取必需参数、资源或权限。
     *
     * @param deliverable deliverable 参数，用于 requireDeliverable 流程中的校验、计算或对象转换。
     * @param mutationType 类型标识，用于选择对应处理分支。
     */
    private void requireDeliverable(CreatorDeliverableDO deliverable, String mutationType) {
        if (deliverable == null) {
            throw new IllegalArgumentException(mutationType + " requires deliverable");
        }
    }

    /**
     * 校验并获取必需参数、资源或权限。
     *
     * @param String string 参数，用于 requireAnyPayloadText 流程中的校验、计算或对象转换。
     * @param payload 待处理业务值，用于规则计算、转换或外部调用。
     * @param keys keys 参数，用于 requireAnyPayloadText 流程中的校验、计算或对象转换。
     */
    private void requireAnyPayloadText(Map<String, Object> payload, String... keys) {
        for (String key : keys) {
            if (trimToNull(string(payload.get(key))) != null) {
                return;
            }
        }
        throw new IllegalArgumentException(String.join(" or ", keys) + " is required");
    }

    /**
     * 转换为接口返回或领域视图。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @return 返回组装或转换后的结果对象。
     */
    private CreatorProviderMutationView toView(CreatorProviderMutationDO row) {
        // 汇总前面计算出的状态和明细，返回给调用方。
        return new CreatorProviderMutationView(
                row.getId(),
                row.getTenantId(),
                row.getCampaignId(),
                row.getCollaborationId(),
                row.getDeliverableId(),
                row.getCreatorId(),
                row.getProvider(),
                row.getMutationKey(),
                row.getMutationType(),
                row.getEntityType(),
                row.getExternalEntityId(),
                row.getRequestHash(),
                row.getIdempotencyKey(),
                row.getStatus(),
                row.getApprovalStatus(),
                Integer.valueOf(1).equals(row.getDryRunRequired()),
                map(row.getPayloadJson()),
                map(row.getValidationJson()),
                map(row.getProviderRequestJson()),
                map(row.getProviderResponseJson()),
                row.getErrorCode(),
                row.getErrorMessage(),
                row.getCreatedBy(),
                row.getApprovedBy(),
                row.getApprovedAt(),
                // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
                row.getExecutedBy(),
                row.getExecutedAt(),
                row.getCreatedAt(),
                row.getUpdatedAt());
    }

    /**
     * 执行 providerRequestEvidence 流程，围绕 provider request evidence 完成校验、计算或结果组装。
     *
     * @param request 请求对象，承载本次操作的输入参数。
     * @return 返回 providerRequestEvidence 流程生成的业务结果。
     */
    private Map<String, Object> providerRequestEvidence(CreatorProviderMutationRequest request) {
        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("provider", request.provider());
        evidence.put("mutationType", request.mutationType());
        evidence.put("entityType", request.entityType());
        evidence.put("externalEntityId", defaultString(request.externalEntityId(), ""));
        evidence.put("idempotencyKey", request.idempotencyKey());
        evidence.put("dryRun", request.dryRun());
        evidence.put("partialFailure", request.partialFailure());
        evidence.put("campaignId", request.campaignId());
        evidence.put("collaborationId", request.collaborationId());
        evidence.put("deliverableId", request.deliverableId());
        evidence.put("creatorId", request.creatorId());
        evidence.put("payload", request.payload());
        evidence.put("metadata", request.metadata());
        return ProviderWriteEvidenceSanitizer.sanitizeMap(evidence);
    }

    /**
     * 执行 existingMutation 流程，围绕 existing mutation 完成校验、计算或结果组装。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param mutationKey 业务键，用于在同一租户下定位资源。
     * @return 返回 existingMutation 流程生成的业务结果。
     */
    private SearchExistingMutation existingMutation(Long tenantId, String mutationKey) {
        CreatorProviderMutationDO row = mutationMapper.selectOne(new LambdaQueryWrapper<CreatorProviderMutationDO>()
                .eq(CreatorProviderMutationDO::getTenantId, tenantId)
                .eq(CreatorProviderMutationDO::getMutationKey, mutationKey)
                .last("LIMIT 1"));
        return new SearchExistingMutation(row);
    }

    /**
     * 执行 provider 流程，围绕 provider 完成校验、计算或结果组装。
     *
     * @param creator creator 参数，用于 provider 流程中的校验、计算或对象转换。
     * @param String string 参数，用于 provider 流程中的校验、计算或对象转换。
     * @param payload 待处理业务值，用于规则计算、转换或外部调用。
     * @return 返回 provider 生成的文本或业务键。
     */
    private String provider(CreatorProfileDO creator, Map<String, Object> payload) {
        if (creator != null) {
            return normalizeUpper(creator.getProvider(), "provider");
        }
        return normalizeUpper(string(payload.get("provider")), "provider");
    }

    /**
     * 执行 payload 流程，围绕 payload 完成校验、计算或结果组装。
     *
     * @param String string 参数，用于 payload 流程中的校验、计算或对象转换。
     * @param payload 待处理业务值，用于规则计算、转换或外部调用。
     * @return 返回 payload 流程生成的业务结果。
     */
    private Map<String, Object> payload(Map<String, Object> payload) {
        if (payload == null || payload.isEmpty()) {
            throw new IllegalArgumentException("creator provider mutation payload is required");
        }
        return Map.copyOf(payload);
    }

    /**
     * 处理安全、签名或敏感信息逻辑。
     *
     * @param value 待处理值，用于规则计算或转换。
     */
    private void rejectProviderSecrets(Object value) {
        if (value instanceof Map<?, ?> values) {
            values.forEach((key, nestedValue) -> {
                if (key != null && SECRET_KEYS.contains(key.toString().toLowerCase(Locale.ROOT))) {
                    throw new IllegalArgumentException("creator provider mutation payload must not contain provider secrets");
                }
                rejectProviderSecrets(nestedValue);
            });
        // 根据前序判断结果进入后续条件分支。
        } else if (value instanceof Iterable<?> values) {
            values.forEach(this::rejectProviderSecrets);
        }
    }

    /**
     * 校验输入、权限或业务前置条件。
     *
     * @param expectedTenantId 业务对象 ID，用于定位具体记录。
     * @param actualTenantId 业务对象 ID，用于定位具体记录。
     * @param resource resource 参数，用于 validateTenant 流程中的校验、计算或对象转换。
     */
    private void validateTenant(Long expectedTenantId, Long actualTenantId, String resource) {
        if (!expectedTenantId.equals(actualTenantId)) {
            throw new IllegalArgumentException(resource + " does not belong to current tenant");
        }
    }

    /**
     * 解析并规范化租户 ID。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private Long normalizeTenant(Long tenantId) {
        if (tenantId == null || tenantId < 0) {
            return 0L;
        }
        return tenantId;
    }

    /**
     * 校验并获取必需参数、资源或权限。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param field 待处理业务值，用于规则计算、转换或外部调用。
     * @return 返回 required id 计算得到的数量、金额或指标值。
     */
    private Long requiredId(Long value, String field) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value;
    }

    /**
     * 校验并获取必需参数、资源或权限。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param field 待处理业务值，用于规则计算、转换或外部调用。
     * @return 返回 required 生成的文本或业务键。
     */
    private String required(String value, String field) {
        String trimmed = trimToNull(value);
        if (trimmed == null) {
            throw new IllegalArgumentException(field + " is required");
        }
        return trimmed;
    }

    /**
     * 规范化输入值。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private String normalizeMutationType(String value) {
        String type = normalizeUpper(value, "mutationType");
        if (!MUTATION_TYPES.contains(type)) {
            throw new IllegalArgumentException("unsupported creator provider mutation type");
        }
        return type;
    }

    /**
     * 规范化输入值。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param field 待处理业务值，用于规则计算、转换或外部调用。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private String normalizeUpper(String value, String field) {
        return required(value, field).toUpperCase(Locale.ROOT);
    }

    /**
     * 规范化输入值。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private String normalizeOptionalUpper(String value) {
        String trimmed = trimToNull(value);
        return trimmed == null ? null : trimmed.toUpperCase(Locale.ROOT);
    }

    /**
     * 按默认值规则处理输入值。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param fallback fallback 参数，用于 defaultString 流程中的校验、计算或对象转换。
     * @return 返回 default string 生成的文本或业务键。
     */
    private String defaultString(String value, String fallback) {
        String trimmed = trimToNull(value);
        return trimmed == null ? fallback : trimmed;
    }

    /**
     * 执行业务决策动作，并同步后续状态。
     *
     * @param createdBy created by 参数，用于 rejectSelfApproval 流程中的校验、计算或对象转换。
     * @param actor 操作人标识，用于审计和权限判断。
     */
    private void rejectSelfApproval(String createdBy, String actor) {
        if (Objects.equals(defaultString(createdBy, "system"), defaultString(actor, "system"))) {
            throw new IllegalStateException("creator cannot approve live creator provider mutation");
        }
    }

    /**
     * 解析、归一化或保护输入值，生成安全可用的中间结果。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    /**
     * 执行 string 流程，围绕 string 完成校验、计算或结果组装。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 string 生成的文本或业务键。
     */
    private String string(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    /**
     * 执行 now 流程，围绕 now 完成校验、计算或结果组装。
     *
     * @return 返回 now 流程生成的业务结果。
     */
    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }

    /**
     * 处理 JSON 序列化或反序列化。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 json 生成的文本或业务键。
     */
    private String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value == null ? Map.of() : value);
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("creator provider mutation metadata is not JSON serializable", ex);
        }
    }

    /**
     * 组装输出结构或完成对象转换。
     *
     * @param json JSON 字符串，承载结构化配置或明细。
     * @return 返回组装或转换后的结果对象。
     */
    private Map<String, Object> map(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            Map<String, Object> values = objectMapper.readValue(json, OBJECT_MAP);
            return values == null ? Map.of() : values;
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (JsonProcessingException ex) {
            return Map.of();
        }
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

    /**
     * 执行 sha256 流程，围绕 sha256 完成校验、计算或结果组装。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 sha256 生成的文本或业务键。
     */
    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (Exception ex) {
            throw new IllegalStateException("Could not hash creator provider mutation request", ex);
        }
    }

    /**
     * SearchExistingMutation 数据记录。
     */
    private record SearchExistingMutation(CreatorProviderMutationDO row) {
    }
}
