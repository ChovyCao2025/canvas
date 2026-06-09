package org.chovy.canvas.domain.programmatic;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.dal.dataobject.ProgrammaticDspCampaignDO;
import org.chovy.canvas.dal.dataobject.ProgrammaticDspLineItemDO;
import org.chovy.canvas.dal.dataobject.ProgrammaticDspMutationDO;
import org.chovy.canvas.dal.dataobject.ProgrammaticDspSeatDO;
import org.chovy.canvas.dal.dataobject.ProgrammaticDspSupplyPathDO;
import org.chovy.canvas.dal.mapper.ProgrammaticDspCampaignMapper;
import org.chovy.canvas.dal.mapper.ProgrammaticDspLineItemMapper;
import org.chovy.canvas.dal.mapper.ProgrammaticDspMutationMapper;
import org.chovy.canvas.dal.mapper.ProgrammaticDspSeatMapper;
import org.chovy.canvas.dal.mapper.ProgrammaticDspSupplyPathMapper;
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
 * ProgrammaticDspMutationService 编排 domain.programmatic 场景的领域业务规则。
 */
@Service
public class ProgrammaticDspMutationService {

    private static final TypeReference<Map<String, Object>> OBJECT_MAP = new TypeReference<>() {
    };
    private static final Set<String> MUTATION_TYPES = Set.of(
            "CREATE_INSERTION_ORDER",
            "UPDATE_CAMPAIGN_BUDGET",
            "CREATE_LINE_ITEM",
            "UPDATE_LINE_ITEM_BID",
            "UPDATE_LINE_ITEM_BUDGET",
            "UPDATE_LINE_ITEM_STATUS",
            "ASSIGN_TARGETING",
            "ATTACH_DEAL",
            "SYNC_PROVIDER_STATUS");
    private static final Set<String> SECRET_KEYS = Set.of(
            "token",
            "access_token",
            "refresh_token",
            "client_secret",
            "api_key",
            "apikey",
            "password",
            "secret");

    private final ProgrammaticDspSeatMapper seatMapper;
    private final ProgrammaticDspCampaignMapper campaignMapper;
    private final ProgrammaticDspLineItemMapper lineItemMapper;
    private final ProgrammaticDspSupplyPathMapper supplyPathMapper;
    private final ProgrammaticDspMutationMapper mutationMapper;
    private final ObjectMapper objectMapper;
    private final ProgrammaticDspProviderWriteGateway gateway;
    private final Clock clock;

    /**
     * 创建 ProgrammaticDspMutationService 实例并注入 domain.programmatic 场景依赖。
     * @param seatMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param campaignMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param lineItemMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param supplyPathMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param mutationMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param objectMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param gateway gateway 参数，用于 ProgrammaticDspMutationService 流程中的校验、计算或对象转换。
     */
    @Autowired
    public ProgrammaticDspMutationService(ProgrammaticDspSeatMapper seatMapper,
                                          ProgrammaticDspCampaignMapper campaignMapper,
                                          ProgrammaticDspLineItemMapper lineItemMapper,
                                          ProgrammaticDspSupplyPathMapper supplyPathMapper,
                                          ProgrammaticDspMutationMapper mutationMapper,
                                          ObjectMapper objectMapper,
                                          ProgrammaticDspProviderWriteGateway gateway) {
        this(seatMapper, campaignMapper, lineItemMapper, supplyPathMapper, mutationMapper, objectMapper,
                gateway, Clock.systemDefaultZone());
    }

    /**
     * 执行 ProgrammaticDspMutationService 流程，围绕 programmatic dsp mutation service 完成校验、计算或结果组装。
     *
     * @param seatMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param campaignMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param lineItemMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param supplyPathMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param mutationMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param objectMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param gateway gateway 参数，用于 ProgrammaticDspMutationService 流程中的校验、计算或对象转换。
     * @param clock 时间参数，用于计算窗口、过期或审计时间。
     */
    ProgrammaticDspMutationService(ProgrammaticDspSeatMapper seatMapper,
                                   ProgrammaticDspCampaignMapper campaignMapper,
                                   ProgrammaticDspLineItemMapper lineItemMapper,
                                   ProgrammaticDspSupplyPathMapper supplyPathMapper,
                                   ProgrammaticDspMutationMapper mutationMapper,
                                   ObjectMapper objectMapper,
                                   ProgrammaticDspProviderWriteGateway gateway,
                                   Clock clock) {
        this.seatMapper = seatMapper;
        this.campaignMapper = campaignMapper;
        this.lineItemMapper = lineItemMapper;
        this.supplyPathMapper = supplyPathMapper;
        this.mutationMapper = mutationMapper;
        this.objectMapper = objectMapper == null ? new ObjectMapper() : objectMapper;
        this.gateway = gateway == null ? ProgrammaticDspProviderWriteGateway.unsupported() : gateway;
        this.clock = clock == null ? Clock.systemDefaultZone() : clock;
    }

    /**
     * 提交程序化 DSP Provider 写操作提案。
     * 方法校验 seat/campaign/line item/supply path 的租户关系和 payload，按 mutationKey 幂等创建草稿并记录请求 hash。
     */
    public ProgrammaticDspMutationView propose(Long tenantId,
                                               ProgrammaticDspMutationCommand command,
                                               String actor) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (command == null) {
            throw new IllegalArgumentException("programmatic DSP mutation command is required");
        }
        Long scopedTenantId = normalizeTenant(tenantId);
        ProgrammaticDspSeatDO seat = seat(scopedTenantId, command.seatId());
        ProgrammaticDspLineItemDO lineItem = lineItem(scopedTenantId, command.lineItemId());
        ProgrammaticDspCampaignDO campaign = campaign(scopedTenantId, campaignId(command.campaignId(), lineItem));
        ProgrammaticDspSupplyPathDO supplyPath = supplyPath(scopedTenantId, command.supplyPathId());
        validateRelationship(seat, campaign, lineItem, supplyPath);
        String mutationType = normalizeMutationType(command.mutationType());
        String entityType = normalizeUpper(command.entityType(), "entityType");
        Map<String, Object> payload = payload(command.payload());
        validatePayload(mutationType, entityType, campaign, lineItem, supplyPath, payload, command.externalEntityId());
        String payloadJson = json(payload);
        String requestHash = sha256(payloadJson);
        String mutationKey = required(command.mutationKey(), "mutationKey");
        String idempotencyKey = defaultString(command.idempotencyKey(), mutationKey);
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        ProgrammaticDspMutationDO existing = mutationMapper.selectOne(new LambdaQueryWrapper<ProgrammaticDspMutationDO>()
                .eq(ProgrammaticDspMutationDO::getTenantId, scopedTenantId)
                .eq(ProgrammaticDspMutationDO::getMutationKey, mutationKey)
                .last("LIMIT 1"));
        if (existing != null) {
            validateTenant(scopedTenantId, existing.getTenantId(), "mutation");
            if (!Objects.equals(requestHash, existing.getRequestHash())) {
                throw new IllegalArgumentException("programmatic DSP mutation request hash conflicts with existing mutation key");
            }
            return toView(existing);
        }
        LocalDateTime changedAt = now();
        ProgrammaticDspMutationDO row = new ProgrammaticDspMutationDO();
        row.setTenantId(scopedTenantId);
        row.setSeatId(seat.getId());
        row.setCampaignId(campaign == null ? null : campaign.getId());
        row.setLineItemId(lineItem == null ? null : lineItem.getId());
        row.setSupplyPathId(supplyPath == null ? null : supplyPath.getId());
        row.setProvider(seat.getProvider());
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
        row.setUpdatedAt(changedAt);
        mutationMapper.insert(row);
        // 汇总前面计算出的状态和明细，返回给调用方。
        return toView(row);
    }

    /**
     * 审批 DSP Provider 写操作提案。
     * 通过后进入 READY，拒绝后进入 CANCELLED，并记录审批人、时间和决策原因。
     */
    public ProgrammaticDspMutationView approve(Long tenantId,
                                               Long mutationId,
                                               ProgrammaticDspMutationApprovalCommand command,
                                               String actor) {
        ProgrammaticDspMutationDO row = mutation(normalizeTenant(tenantId), mutationId);
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
                "reason", command == null ? "" : defaultString(command.reason(), ""),
                "decidedAt", changedAt.toString())));
        row.setUpdatedAt(changedAt);
        mutationMapper.updateById(row);
        return toView(row);
    }

    /**
     * 执行或干跑已审批的 DSP Provider 写操作。
     * 会按 dry-run 要求控制正式执行，调用 Provider gateway 后保存脱敏请求、响应、错误码和状态。
     */
    public ProgrammaticDspMutationView execute(Long tenantId,
                                               Long mutationId,
                                               ProgrammaticDspMutationExecuteCommand command,
                                               String actor) {
        Long scopedTenantId = normalizeTenant(tenantId);
        ProgrammaticDspMutationDO row = mutation(scopedTenantId, mutationId);
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (!"APPROVED".equals(row.getApprovalStatus())) {
            throw new IllegalStateException("programmatic DSP mutation must be approved before execution");
        }
        boolean dryRun = command == null || !Boolean.FALSE.equals(command.dryRun());
        if (!dryRun && Integer.valueOf(1).equals(row.getDryRunRequired())
                && !"DRY_RUN_OK".equals(row.getStatus())) {
            throw new IllegalStateException("dry run must pass before applying programmatic DSP mutation");
        }
        ProgrammaticDspSeatDO seat = seat(scopedTenantId, row.getSeatId());
        ProgrammaticDspLineItemDO lineItem = lineItem(scopedTenantId, row.getLineItemId());
        ProgrammaticDspCampaignDO campaign = campaign(scopedTenantId, campaignId(row.getCampaignId(), lineItem));
        ProgrammaticDspSupplyPathDO supplyPath = supplyPath(scopedTenantId, row.getSupplyPathId());
        validateRelationship(seat, campaign, lineItem, supplyPath);
        ProgrammaticDspMutationRequest request = new ProgrammaticDspMutationRequest(
                scopedTenantId,
                row.getSeatId(),
                row.getCampaignId(),
                row.getLineItemId(),
                row.getSupplyPathId(),
                row.getProvider(),
                seat.getSeatKey(),
                seat.getAdvertiserAccountId(),
                row.getMutationType(),
                row.getEntityType(),
                row.getExternalEntityId(),
                row.getIdempotencyKey(),
                dryRun,
                command == null || !Boolean.FALSE.equals(command.partialFailure()),
                map(row.getPayloadJson()),
                command == null || command.metadata() == null ? Map.of() : command.metadata());
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        ProgrammaticDspMutationResult result = gateway.execute(request);
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
     * 查询租户内 DSP Provider 写操作提案。
     * 支持按 seat、campaign、line item、执行状态和审批状态过滤，按更新时间倒序返回。
     */
    public List<ProgrammaticDspMutationView> list(Long tenantId, ProgrammaticDspMutationQuery query) {
        Long scopedTenantId = normalizeTenant(tenantId);
        Long seatId = query == null ? null : query.seatId();
        Long campaignId = query == null ? null : query.campaignId();
        Long lineItemId = query == null ? null : query.lineItemId();
        String status = query == null ? null : normalizeOptionalUpper(query.status());
        String approvalStatus = query == null ? null : normalizeOptionalUpper(query.approvalStatus());
        int limit = Math.min(Math.max(query == null || query.limit() == null ? 50 : query.limit(), 1), 100);
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        return safeList(mutationMapper.selectList(new LambdaQueryWrapper<ProgrammaticDspMutationDO>()
                .eq(ProgrammaticDspMutationDO::getTenantId, scopedTenantId)
                .eq(seatId != null, ProgrammaticDspMutationDO::getSeatId, seatId)
                .eq(campaignId != null, ProgrammaticDspMutationDO::getCampaignId, campaignId)
                .eq(lineItemId != null, ProgrammaticDspMutationDO::getLineItemId, lineItemId)
                .eq(status != null, ProgrammaticDspMutationDO::getStatus, status)
                .eq(approvalStatus != null, ProgrammaticDspMutationDO::getApprovalStatus, approvalStatus)
                .orderByDesc(ProgrammaticDspMutationDO::getUpdatedAt)
                // 遍历候选数据并按业务规则筛选、转换或聚合。
                .last("LIMIT " + limit))).stream()
                .filter(row -> scopedTenantId.equals(row.getTenantId()))
                .filter(row -> seatId == null || Objects.equals(seatId, row.getSeatId()))
                .filter(row -> campaignId == null || Objects.equals(campaignId, row.getCampaignId()))
                .filter(row -> lineItemId == null || Objects.equals(lineItemId, row.getLineItemId()))
                .filter(row -> status == null || status.equals(row.getStatus()))
                .filter(row -> approvalStatus == null || approvalStatus.equals(row.getApprovalStatus()))
                .limit(limit)
                .map(this::toView)
                .toList();
    }

    /**
     * 执行 seat 流程，围绕 seat 完成校验、计算或结果组装。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param seatId 业务对象 ID，用于定位具体记录。
     * @return 返回 seat 流程生成的业务结果。
     */
    private ProgrammaticDspSeatDO seat(Long tenantId, Long seatId) {
        ProgrammaticDspSeatDO seat = seatMapper.selectById(requiredId(seatId, "seatId"));
        validateTenant(tenantId, seat == null ? null : seat.getTenantId(), "seat");
        if (!Integer.valueOf(1).equals(seat.getEnabled())) {
            throw new IllegalArgumentException("programmatic DSP seat is disabled");
        }
        return seat;
    }

    /**
     * 执行 campaign 流程，围绕 campaign 完成校验、计算或结果组装。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param campaignId 业务对象 ID，用于定位具体记录。
     * @return 返回 campaign 流程生成的业务结果。
     */
    private ProgrammaticDspCampaignDO campaign(Long tenantId, Long campaignId) {
        if (campaignId == null) {
            return null;
        }
        ProgrammaticDspCampaignDO campaign = campaignMapper.selectById(requiredId(campaignId, "campaignId"));
        validateTenant(tenantId, campaign == null ? null : campaign.getTenantId(), "campaign");
        return campaign;
    }

    /**
     * 执行 lineItem 流程，围绕 line item 完成校验、计算或结果组装。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param lineItemId 业务对象 ID，用于定位具体记录。
     * @return 返回 lineItem 流程生成的业务结果。
     */
    private ProgrammaticDspLineItemDO lineItem(Long tenantId, Long lineItemId) {
        if (lineItemId == null) {
            return null;
        }
        ProgrammaticDspLineItemDO lineItem = lineItemMapper.selectById(requiredId(lineItemId, "lineItemId"));
        validateTenant(tenantId, lineItem == null ? null : lineItem.getTenantId(), "line item");
        return lineItem;
    }

    /**
     * 执行 supplyPath 流程，围绕 supply path 完成校验、计算或结果组装。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param supplyPathId 业务对象 ID，用于定位具体记录。
     * @return 返回 supplyPath 流程生成的业务结果。
     */
    private ProgrammaticDspSupplyPathDO supplyPath(Long tenantId, Long supplyPathId) {
        if (supplyPathId == null) {
            return null;
        }
        ProgrammaticDspSupplyPathDO supplyPath = supplyPathMapper.selectById(requiredId(supplyPathId,
                "supplyPathId"));
        validateTenant(tenantId, supplyPath == null ? null : supplyPath.getTenantId(), "supply path");
        return supplyPath;
    }

    /**
     * 执行 mutation 流程，围绕 mutation 完成校验、计算或结果组装。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param mutationId 业务对象 ID，用于定位具体记录。
     * @return 返回 mutation 流程生成的业务结果。
     */
    private ProgrammaticDspMutationDO mutation(Long tenantId, Long mutationId) {
        ProgrammaticDspMutationDO row = mutationMapper.selectById(requiredId(mutationId, "mutationId"));
        validateTenant(tenantId, row == null ? null : row.getTenantId(), "mutation");
        return row;
    }

    /**
     * 执行 campaignId 流程，围绕 campaign id 完成校验、计算或结果组装。
     *
     * @param commandCampaignId 业务对象 ID，用于定位具体记录。
     * @param lineItem line item 参数，用于 campaignId 流程中的校验、计算或对象转换。
     * @return 返回 campaign id 计算得到的数量、金额或指标值。
     */
    private Long campaignId(Long commandCampaignId, ProgrammaticDspLineItemDO lineItem) {
        return commandCampaignId == null && lineItem != null ? lineItem.getCampaignId() : commandCampaignId;
    }

    /**
     * 校验输入、权限或业务前置条件。
     *
     * @param seat 时间参数，用于计算窗口、过期或审计时间。
     * @param campaign campaign 参数，用于 validateRelationship 流程中的校验、计算或对象转换。
     * @param lineItem line item 参数，用于 validateRelationship 流程中的校验、计算或对象转换。
     * @param supplyPath supply path 参数，用于 validateRelationship 流程中的校验、计算或对象转换。
     */
    private void validateRelationship(ProgrammaticDspSeatDO seat,
                                      ProgrammaticDspCampaignDO campaign,
                                      ProgrammaticDspLineItemDO lineItem,
                                      ProgrammaticDspSupplyPathDO supplyPath) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (lineItem != null && !Objects.equals(seat.getId(), lineItem.getSeatId())) {
            throw new IllegalArgumentException("line item does not belong to seat");
        }
        if (lineItem != null && campaign != null && !Objects.equals(campaign.getId(), lineItem.getCampaignId())) {
            throw new IllegalArgumentException("line item does not belong to campaign");
        }
        if (campaign != null && !Objects.equals(seat.getCurrency(), campaign.getCurrency())) {
            throw new IllegalArgumentException("campaign currency does not match seat");
        }
        if (supplyPath != null) {
            if (lineItem == null) {
                throw new IllegalArgumentException("supply path requires line item");
            }
            if (!Objects.equals(lineItem.getId(), supplyPath.getLineItemId())) {
                throw new IllegalArgumentException("supply path does not belong to line item");
            }
        }
    }

    /**
     * 校验输入、权限或业务前置条件。
     *
     * @param mutationType 类型标识，用于选择对应处理分支。
     * @param entityType 类型标识，用于选择对应处理分支。
     * @param campaign campaign 参数，用于 validatePayload 流程中的校验、计算或对象转换。
     * @param lineItem line item 参数，用于 validatePayload 流程中的校验、计算或对象转换。
     * @param supplyPath supply path 参数，用于 validatePayload 流程中的校验、计算或对象转换。
     * @param payload 待处理业务值，用于规则计算、转换或外部调用。
     * @param externalEntityId 业务对象 ID，用于定位具体记录。
     */
    private void validatePayload(String mutationType,
                                 String entityType,
                                 ProgrammaticDspCampaignDO campaign,
                                 ProgrammaticDspLineItemDO lineItem,
                                 ProgrammaticDspSupplyPathDO supplyPath,
                                 Map<String, Object> payload,
                                 String externalEntityId) {
        // 准备本次处理所需的上下文和中间变量。
        rejectProviderSecrets(payload);
        switch (mutationType) {
            case "CREATE_INSERTION_ORDER" -> {
                // 校验关键输入和前置条件，避免无效状态继续进入主流程。
                if (!"CAMPAIGN".equals(entityType) && !"INSERTION_ORDER".equals(entityType)) {
                    throw new IllegalArgumentException("insertion order mutation entity must be CAMPAIGN or INSERTION_ORDER");
                }
                requireAnyPayloadValue(payload, "insertionOrderName", "campaignName");
            }
            case "UPDATE_CAMPAIGN_BUDGET" -> {
                requireCampaign(campaign, mutationType);
                if (!"CAMPAIGN".equals(entityType) && !"INSERTION_ORDER".equals(entityType)) {
                    throw new IllegalArgumentException("campaign budget entity must be CAMPAIGN or INSERTION_ORDER");
                }
                requireAnyPayloadValue(payload, "budgetMicros", "budgetAmount");
            }
            case "CREATE_LINE_ITEM" -> {
                requireCampaign(campaign, mutationType);
                requireAnyPayloadValue(payload, "lineItemName", "lineItemKey");
            }
            case "UPDATE_LINE_ITEM_BID" -> {
                requireLineItem(lineItem, mutationType);
                if (!"LINE_ITEM".equals(entityType)) {
                    throw new IllegalArgumentException("line item bid entity must be LINE_ITEM");
                }
                requireAnyPayloadValue(payload, "bidCpmMicros", "maxBidCpm", "bidStrategy");
            }
            case "UPDATE_LINE_ITEM_BUDGET" -> {
                requireLineItem(lineItem, mutationType);
                if (!"LINE_ITEM".equals(entityType)) {
                    throw new IllegalArgumentException("line item budget entity must be LINE_ITEM");
                }
                requireAnyPayloadValue(payload, "dailyBudgetMicros", "totalBudgetMicros",
                        "dailyBudgetAmount", "totalBudgetAmount");
            }
            case "UPDATE_LINE_ITEM_STATUS" -> {
                requireLineItem(lineItem, mutationType);
                if (!"LINE_ITEM".equals(entityType)) {
                    throw new IllegalArgumentException("line item status entity must be LINE_ITEM");
                }
                requireAnyPayloadValue(payload, "status");
            }
            case "ASSIGN_TARGETING" -> {
                requireLineItem(lineItem, mutationType);
                if (!"LINE_ITEM".equals(entityType) && !"TARGETING".equals(entityType)) {
                    throw new IllegalArgumentException("targeting entity must be LINE_ITEM or TARGETING");
                }
                requireAnyPayloadValue(payload, "targeting", "targetingType", "assignedTargetingOptions");
            }
            case "ATTACH_DEAL" -> {
                requireLineItem(lineItem, mutationType);
                if (supplyPath == null) {
                    requireAnyPayloadValue(payload, "dealId");
                }
            }
            case "SYNC_PROVIDER_STATUS" -> {
                if (lineItem == null && campaign == null && trimToNull(externalEntityId) == null) {
                    throw new IllegalArgumentException("provider status sync requires campaign, line item, or external entity id");
                }
            }
            default -> throw new IllegalArgumentException("unsupported programmatic DSP mutation type");
        }
    }

    /**
     * 校验并获取必需参数、资源或权限。
     *
     * @param campaign campaign 参数，用于 requireCampaign 流程中的校验、计算或对象转换。
     * @param mutationType 类型标识，用于选择对应处理分支。
     */
    private void requireCampaign(ProgrammaticDspCampaignDO campaign, String mutationType) {
        if (campaign == null) {
            throw new IllegalArgumentException(mutationType + " requires campaign");
        }
    }

    /**
     * 校验并获取必需参数、资源或权限。
     *
     * @param lineItem line item 参数，用于 requireLineItem 流程中的校验、计算或对象转换。
     * @param mutationType 类型标识，用于选择对应处理分支。
     */
    private void requireLineItem(ProgrammaticDspLineItemDO lineItem, String mutationType) {
        if (lineItem == null) {
            throw new IllegalArgumentException(mutationType + " requires line item");
        }
    }

    /**
     * 校验并获取必需参数、资源或权限。
     *
     * @param String string 参数，用于 requireAnyPayloadValue 流程中的校验、计算或对象转换。
     * @param payload 待处理业务值，用于规则计算、转换或外部调用。
     * @param keys keys 参数，用于 requireAnyPayloadValue 流程中的校验、计算或对象转换。
     */
    private void requireAnyPayloadValue(Map<String, Object> payload, String... keys) {
        for (String key : keys) {
            Object value = payload.get(key);
            if (value instanceof String text) {
                if (trimToNull(text) != null) {
                    return;
                }
            // 根据前序判断结果进入后续条件分支。
            } else if (value != null) {
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
    private ProgrammaticDspMutationView toView(ProgrammaticDspMutationDO row) {
        // 汇总前面计算出的状态和明细，返回给调用方。
        return new ProgrammaticDspMutationView(
                row.getId(),
                row.getTenantId(),
                row.getSeatId(),
                row.getCampaignId(),
                row.getLineItemId(),
                row.getSupplyPathId(),
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
    private Map<String, Object> providerRequestEvidence(ProgrammaticDspMutationRequest request) {
        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("provider", request.provider());
        evidence.put("seatKey", request.seatKey());
        evidence.put("advertiserAccountId", request.advertiserAccountId());
        evidence.put("mutationType", request.mutationType());
        evidence.put("entityType", request.entityType());
        evidence.put("externalEntityId", defaultString(request.externalEntityId(), ""));
        evidence.put("idempotencyKey", request.idempotencyKey());
        evidence.put("dryRun", request.dryRun());
        evidence.put("partialFailure", request.partialFailure());
        evidence.put("seatId", request.seatId());
        evidence.put("campaignId", request.campaignId());
        evidence.put("lineItemId", request.lineItemId());
        evidence.put("supplyPathId", request.supplyPathId());
        evidence.put("payload", request.payload());
        evidence.put("metadata", request.metadata());
        return ProviderWriteEvidenceSanitizer.sanitizeMap(evidence);
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
            throw new IllegalArgumentException("programmatic DSP mutation payload is required");
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
                    throw new IllegalArgumentException("programmatic DSP mutation payload must not contain provider secrets");
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
            throw new IllegalArgumentException("unsupported programmatic DSP mutation type");
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
            throw new IllegalStateException("creator cannot approve live programmatic DSP provider mutation");
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
            throw new IllegalArgumentException("programmatic DSP mutation metadata is not JSON serializable", ex);
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
            throw new IllegalStateException("Could not hash programmatic DSP mutation request", ex);
        }
    }
}
