package org.chovy.canvas.domain.search;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.dal.dataobject.SearchMarketingKeywordDO;
import org.chovy.canvas.dal.dataobject.SearchMarketingMutationDO;
import org.chovy.canvas.dal.dataobject.SearchMarketingOpportunityDO;
import org.chovy.canvas.dal.dataobject.SearchMarketingSourceDO;
import org.chovy.canvas.dal.mapper.SearchMarketingKeywordMapper;
import org.chovy.canvas.dal.mapper.SearchMarketingMutationMapper;
import org.chovy.canvas.dal.mapper.SearchMarketingOpportunityMapper;
import org.chovy.canvas.dal.mapper.SearchMarketingSourceMapper;
import org.chovy.canvas.domain.providerwrite.ProviderWriteEvidenceSanitizer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
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
 * SearchMarketingMutationService 编排 domain.search 场景的领域业务规则。
 */
@Service
public class SearchMarketingMutationService {

    private static final TypeReference<Map<String, Object>> OBJECT_MAP = new TypeReference<>() {
    };
    private static final Set<String> MUTATION_TYPES = Set.of(
            "ADD_KEYWORD",
            "UPDATE_KEYWORD_BID",
            "ADD_NEGATIVE_KEYWORD",
            "UPDATE_CAMPAIGN_BUDGET",
            "PAUSE_KEYWORD");
    private static final Set<String> SECRET_KEYS = Set.of(
            "token",
            "access_token",
            "refresh_token",
            "client_secret",
            "api_key",
            "apikey",
            "password");

    private final SearchMarketingSourceMapper sourceMapper;
    private final SearchMarketingKeywordMapper keywordMapper;
    private final SearchMarketingOpportunityMapper opportunityMapper;
    private final SearchMarketingMutationMapper mutationMapper;
    private final ObjectMapper objectMapper;
    private final SearchMarketingProviderWriteGateway gateway;
    private final Clock clock;

    /**
     * 创建 SearchMarketingMutationService 实例并注入 domain.search 场景依赖。
     * @param sourceMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param keywordMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param opportunityMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param mutationMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param objectMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param gateway gateway 参数，用于 SearchMarketingMutationService 流程中的校验、计算或对象转换。
     */
    @Autowired
    public SearchMarketingMutationService(SearchMarketingSourceMapper sourceMapper,
                                          SearchMarketingKeywordMapper keywordMapper,
                                          SearchMarketingOpportunityMapper opportunityMapper,
                                          SearchMarketingMutationMapper mutationMapper,
                                          ObjectMapper objectMapper,
                                          SearchMarketingProviderWriteGateway gateway) {
        this(sourceMapper, keywordMapper, opportunityMapper, mutationMapper, objectMapper, gateway,
                Clock.systemDefaultZone());
    }

    /**
     * 查询或读取业务数据。
     *
     * @param sourceMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param keywordMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param opportunityMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param mutationMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param objectMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param gateway gateway 参数，用于 SearchMarketingMutationService 流程中的校验、计算或对象转换。
     * @param clock 时间参数，用于计算窗口、过期或审计时间。
     */
    SearchMarketingMutationService(SearchMarketingSourceMapper sourceMapper,
                                   SearchMarketingKeywordMapper keywordMapper,
                                   SearchMarketingOpportunityMapper opportunityMapper,
                                   SearchMarketingMutationMapper mutationMapper,
                                   ObjectMapper objectMapper,
                                   SearchMarketingProviderWriteGateway gateway,
                                   Clock clock) {
        this.sourceMapper = sourceMapper;
        this.keywordMapper = keywordMapper;
        this.opportunityMapper = opportunityMapper;
        this.mutationMapper = mutationMapper;
        this.objectMapper = objectMapper == null ? new ObjectMapper() : objectMapper;
        this.gateway = gateway == null ? SearchMarketingProviderWriteGateway.unsupported() : gateway;
        this.clock = clock == null ? Clock.systemDefaultZone() : clock;
    }

    /**
     * 执行业务操作 propose，作为搜索营销的服务入口。
     * <p>调用方必须传入租户上下文或租户 ID，方法内的查询、写入和治理判断都限制在该租户范围内。
     * 会通过 Mapper 写入、更新或关闭持久化记录。
     * @param tenantId 租户 ID，所有查询和写入都限定在该租户数据范围内
     * @param command 本次操作的业务请求参数，包含目标对象、状态或外部回调载荷
     * @param actor 操作人标识，用于审计字段、状态流转记录或治理追踪
     * @return 返回租户范围内的最新业务视图或持久化对象快照
     */
    public SearchMarketingMutationView propose(Long tenantId, SearchMarketingMutationCommand command, String actor) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (command == null) {
            throw new IllegalArgumentException("search marketing mutation command is required");
        }
        Long scopedTenantId = normalizeTenant(tenantId);
        SearchMarketingSourceDO source = source(scopedTenantId, command.sourceId());
        SearchMarketingKeywordDO keyword = keyword(scopedTenantId, command.keywordId());
        SearchMarketingOpportunityDO opportunity = opportunity(scopedTenantId, command.opportunityId());
        validateOpportunityScope(opportunity, command.sourceId(), command.keywordId());
        String mutationType = normalizeMutationType(command.mutationType());
        String entityType = normalizeUpper(command.entityType(), "entityType");
        Map<String, Object> payload = payload(command.payload());
        validatePayload(mutationType, entityType, command.externalEntityId(), command.keywordId(), payload);
        String payloadJson = json(payload);
        String requestHash = sha256(payloadJson);
        String mutationKey = required(command.mutationKey(), "mutationKey");
        String idempotencyKey = defaultString(command.idempotencyKey(), mutationKey);
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        SearchMarketingMutationDO existing = mutationMapper.selectOne(new LambdaQueryWrapper<SearchMarketingMutationDO>()
                .eq(SearchMarketingMutationDO::getTenantId, scopedTenantId)
                .eq(SearchMarketingMutationDO::getMutationKey, mutationKey)
                .last("LIMIT 1"));
        if (existing != null) {
            validateTenant(scopedTenantId, existing.getTenantId(), "mutation");
            if (!Objects.equals(requestHash, existing.getRequestHash())
                    && !Objects.equals(payloadJson, json(map(existing.getPayloadJson())))) {
                throw new IllegalArgumentException("mutation request hash conflicts with existing mutation key");
            }
            return toView(existing);
        }
        LocalDateTime changedAt = now();
        SearchMarketingMutationDO row = new SearchMarketingMutationDO();
        row.setTenantId(scopedTenantId);
        row.setSourceId(source.getId());
        row.setOpportunityId(opportunity == null ? null : opportunity.getId());
        row.setKeywordId(keyword == null ? null : keyword.getId());
        row.setProvider(source.getProvider());
        row.setChannel(source.getChannel());
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
     * 执行业务操作 proposeFromOpportunity，作为搜索营销的服务入口。
     * <p>调用方必须传入租户上下文或租户 ID，方法内的查询、写入和治理判断都限制在该租户范围内。
     * @param tenantId 租户 ID，所有查询和写入都限定在该租户数据范围内
     * @param opportunityId 目标业务记录 ID，需与租户边界匹配
     * @param command 本次操作的业务请求参数，包含目标对象、状态或外部回调载荷
     * @param actor 操作人标识，用于审计字段、状态流转记录或治理追踪
     * @return 返回租户范围内的最新业务视图或持久化对象快照
     */
    public SearchMarketingMutationView proposeFromOpportunity(Long tenantId,
                                                              Long opportunityId,
                                                              SearchMarketingOpportunityMutationCommand command,
                                                              String actor) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (command == null) {
            throw new IllegalArgumentException("search marketing opportunity mutation command is required");
        }
        Long scopedTenantId = normalizeTenant(tenantId);
        SearchMarketingOpportunityDO opportunity = opportunity(scopedTenantId, opportunityId);
        if (!"ACCEPTED".equals(opportunity.getStatus())) {
            throw new IllegalStateException("search marketing opportunity must be accepted before creating mutation");
        }
        // 汇总前面计算出的状态和明细，返回给调用方。
        return propose(scopedTenantId,
                new SearchMarketingMutationCommand(
                        opportunity.getSourceId(),
                        opportunity.getId(),
                        opportunity.getKeywordId(),
                        command.mutationKey(),
                        command.mutationType(),
                        command.entityType(),
                        command.externalEntityId(),
                        command.dryRunRequired(),
                        command.idempotencyKey(),
                        command.payload()),
                actor);
    }

    /**
     * 审批通过待审变更，作为搜索营销的服务入口。
     * <p>调用方必须传入租户上下文或租户 ID，方法内的查询、写入和治理判断都限制在该租户范围内。
     * 会通过 Mapper 写入、更新或关闭持久化记录。
     * @param tenantId 租户 ID，所有查询和写入都限定在该租户数据范围内
     * @param mutationId 目标业务记录 ID，需与租户边界匹配
     * @param command 本次操作的业务请求参数，包含目标对象、状态或外部回调载荷
     * @param actor 操作人标识，用于审计字段、状态流转记录或治理追踪
     * @return 返回租户范围内的最新业务视图或持久化对象快照
     */
    public SearchMarketingMutationView approve(Long tenantId,
                                               Long mutationId,
                                               SearchMarketingMutationApprovalCommand command,
                                               String actor) {
        SearchMarketingMutationDO row = mutation(normalizeTenant(tenantId), mutationId);
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
     * 执行隐私擦除请求，作为搜索营销的服务入口。
     * <p>调用方必须传入租户上下文或租户 ID，方法内的查询、写入和治理判断都限制在该租户范围内。
     * 会通过 Mapper 写入、更新或关闭持久化记录。
     * @param tenantId 租户 ID，所有查询和写入都限定在该租户数据范围内
     * @param mutationId 目标业务记录 ID，需与租户边界匹配
     * @param command 本次操作的业务请求参数，包含目标对象、状态或外部回调载荷
     * @param actor 操作人标识，用于审计字段、状态流转记录或治理追踪
     * @return 返回租户范围内的最新业务视图或持久化对象快照
     */
    public SearchMarketingMutationView execute(Long tenantId,
                                               Long mutationId,
                                               SearchMarketingMutationExecuteCommand command,
                                               String actor) {
        Long scopedTenantId = normalizeTenant(tenantId);
        SearchMarketingMutationDO row = mutation(scopedTenantId, mutationId);
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (!"APPROVED".equals(row.getApprovalStatus())) {
            throw new IllegalStateException("search marketing mutation must be approved before execution");
        }
        boolean dryRun = command == null || !Boolean.FALSE.equals(command.dryRun());
        if (!dryRun && Integer.valueOf(1).equals(row.getDryRunRequired())
                && !"DRY_RUN_OK".equals(row.getStatus())) {
            throw new IllegalStateException("dry run must pass before applying search marketing mutation");
        }
        SearchMarketingSourceDO source = source(scopedTenantId, row.getSourceId());
        Map<String, Object> metadata = command == null || command.metadata() == null ? Map.of() : command.metadata();
        boolean partialFailure = command == null || !Boolean.FALSE.equals(command.partialFailure());
        SearchMarketingProviderMutationRequest request = new SearchMarketingProviderMutationRequest(
                scopedTenantId,
                row.getSourceId(),
                row.getProvider(),
                source.getSourceKey(),
                source.getExternalAccountId(),
                row.getMutationType(),
                row.getEntityType(),
                row.getExternalEntityId(),
                row.getIdempotencyKey(),
                dryRun,
                partialFailure,
                map(row.getPayloadJson()),
                metadata);
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        SearchMarketingProviderMutationResult result = gateway.execute(request);
        LocalDateTime changedAt = now();
        row.setProviderRequestJson(json(providerRequestEvidence(request)));
        row.setProviderResponseJson(json(providerResponseEvidence(result)));
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
     * 查询业务列表，作为搜索营销的服务入口。
     * <p>调用方必须传入租户上下文或租户 ID，方法内的查询、写入和治理判断都限制在该租户范围内。
     * 不直接修改业务状态，主要读取数据或执行本地规则计算。
     * @param tenantId 租户 ID，所有查询和写入都限定在该租户数据范围内
     * @param query query 参数，用于 list 流程中的校验、计算或对象转换。
     * @return 返回按租户、状态和数量限制过滤后的视图列表；无数据时返回空列表
     */
    public List<SearchMarketingMutationView> list(Long tenantId, SearchMarketingMutationQuery query) {
        Long scopedTenantId = normalizeTenant(tenantId);
        Long sourceId = query == null ? null : query.sourceId();
        String status = query == null ? null : normalizeOptionalUpper(query.status());
        String approvalStatus = query == null ? null : normalizeOptionalUpper(query.approvalStatus());
        int limit = Math.min(Math.max(query == null || query.limit() == null ? 50 : query.limit(), 1), 100);
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        return safeList(mutationMapper.selectList(new LambdaQueryWrapper<SearchMarketingMutationDO>()
                .eq(SearchMarketingMutationDO::getTenantId, scopedTenantId)
                .eq(sourceId != null, SearchMarketingMutationDO::getSourceId, sourceId)
                .eq(status != null, SearchMarketingMutationDO::getStatus, status)
                .eq(approvalStatus != null, SearchMarketingMutationDO::getApprovalStatus, approvalStatus)
                .orderByDesc(SearchMarketingMutationDO::getUpdatedAt)
                // 遍历候选数据并按业务规则筛选、转换或聚合。
                .last("LIMIT " + limit))).stream()
                .filter(row -> scopedTenantId.equals(row.getTenantId()))
                .filter(row -> sourceId == null || Objects.equals(sourceId, row.getSourceId()))
                .filter(row -> status == null || status.equals(row.getStatus()))
                .filter(row -> approvalStatus == null || approvalStatus.equals(row.getApprovalStatus()))
                .limit(limit)
                .map(this::toView)
                .toList();
    }

    /**
     * 执行 source 流程，围绕 source 完成校验、计算或结果组装。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param sourceId 业务对象 ID，用于定位具体记录。
     * @return 返回 source 流程生成的业务结果。
     */
    private SearchMarketingSourceDO source(Long tenantId, Long sourceId) {
        SearchMarketingSourceDO source = sourceMapper.selectById(requiredId(sourceId, "sourceId"));
        validateTenant(tenantId, source == null ? null : source.getTenantId(), "source");
        if (!"SEM".equals(source.getChannel())) {
            throw new IllegalArgumentException("search marketing provider mutations require SEM source");
        }
        if (!Integer.valueOf(1).equals(source.getEnabled())) {
            throw new IllegalArgumentException("search marketing source is disabled");
        }
        return source;
    }

    /**
     * 执行 keyword 流程，围绕 keyword 完成校验、计算或结果组装。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param keywordId 业务对象 ID，用于定位具体记录。
     * @return 返回 keyword 流程生成的业务结果。
     */
    private SearchMarketingKeywordDO keyword(Long tenantId, Long keywordId) {
        if (keywordId == null) {
            return null;
        }
        SearchMarketingKeywordDO keyword = keywordMapper.selectById(requiredId(keywordId, "keywordId"));
        validateTenant(tenantId, keyword == null ? null : keyword.getTenantId(), "keyword");
        return keyword;
    }

    /**
     * 执行 opportunity 流程，围绕 opportunity 完成校验、计算或结果组装。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param opportunityId 业务对象 ID，用于定位具体记录。
     * @return 返回 opportunity 流程生成的业务结果。
     */
    private SearchMarketingOpportunityDO opportunity(Long tenantId, Long opportunityId) {
        if (opportunityId == null) {
            return null;
        }
        SearchMarketingOpportunityDO opportunity = opportunityMapper.selectById(requiredId(opportunityId,
                "opportunityId"));
        validateTenant(tenantId, opportunity == null ? null : opportunity.getTenantId(), "opportunity");
        return opportunity;
    }

    /**
     * 执行 mutation 流程，围绕 mutation 完成校验、计算或结果组装。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param mutationId 业务对象 ID，用于定位具体记录。
     * @return 返回 mutation 流程生成的业务结果。
     */
    private SearchMarketingMutationDO mutation(Long tenantId, Long mutationId) {
        SearchMarketingMutationDO row = mutationMapper.selectById(requiredId(mutationId, "mutationId"));
        validateTenant(tenantId, row == null ? null : row.getTenantId(), "mutation");
        return row;
    }

    /**
     * 校验输入、权限或业务前置条件。
     *
     * @param opportunity opportunity 参数，用于 validateOpportunityScope 流程中的校验、计算或对象转换。
     * @param sourceId 业务对象 ID，用于定位具体记录。
     * @param keywordId 业务对象 ID，用于定位具体记录。
     */
    private void validateOpportunityScope(SearchMarketingOpportunityDO opportunity, Long sourceId, Long keywordId) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (opportunity == null) {
            // 汇总前面计算出的状态和明细，返回给调用方。
            return;
        }
        if (!Objects.equals(sourceId, opportunity.getSourceId())) {
            throw new IllegalArgumentException("opportunity source does not match mutation source");
        }
        if (keywordId != null && !Objects.equals(keywordId, opportunity.getKeywordId())) {
            throw new IllegalArgumentException("opportunity keyword does not match mutation keyword");
        }
    }

    /**
     * 校验输入、权限或业务前置条件。
     *
     * @param mutationType 类型标识，用于选择对应处理分支。
     * @param entityType 类型标识，用于选择对应处理分支。
     * @param externalEntityId 业务对象 ID，用于定位具体记录。
     * @param keywordId 业务对象 ID，用于定位具体记录。
     * @param payload 待处理业务值，用于规则计算、转换或外部调用。
     */
    private void validatePayload(String mutationType,
                                 String entityType,
                                 String externalEntityId,
                                 Long keywordId,
                                 Map<String, Object> payload) {
        // 准备本次处理所需的上下文和中间变量。
        rejectProviderSecrets(payload);
        switch (mutationType) {
            case "ADD_KEYWORD" -> {
                requirePayloadText(payload, "text");
                requirePayloadText(payload, "matchType");
            }
            case "UPDATE_KEYWORD_BID" -> {
                requireEntityReference(keywordId, externalEntityId, "keyword bid update");
                requirePositive(payload, "bidMicros");
            }
            case "ADD_NEGATIVE_KEYWORD" -> {
                // 校验关键输入和前置条件，避免无效状态继续进入主流程。
                if (!"CAMPAIGN".equals(entityType) && !"AD_GROUP".equals(entityType)) {
                    throw new IllegalArgumentException("negative keyword mutation entity must be CAMPAIGN or AD_GROUP");
                }
                requirePayloadText(payload, "text");
            }
            case "UPDATE_CAMPAIGN_BUDGET" -> {
                if (!"CAMPAIGN".equals(entityType)) {
                    throw new IllegalArgumentException("campaign budget mutation entity must be CAMPAIGN");
                }
                requirePositive(payload, "budgetMicros");
            }
            case "PAUSE_KEYWORD" -> requireEntityReference(keywordId, externalEntityId, "keyword pause");
            default -> throw new IllegalArgumentException("unsupported search marketing mutation type");
        }
    }

    /**
     * 校验并获取必需参数、资源或权限。
     *
     * @param String string 参数，用于 requirePayloadText 流程中的校验、计算或对象转换。
     * @param payload 待处理业务值，用于规则计算、转换或外部调用。
     * @param key 业务键，用于在同一租户下定位资源。
     */
    private void requirePayloadText(Map<String, Object> payload, String key) {
        Object value = payload.get(key);
        if (!(value instanceof String text) || text.trim().isEmpty()) {
            throw new IllegalArgumentException(key + " is required");
        }
    }

    /**
     * 校验并获取必需参数、资源或权限。
     *
     * @param String string 参数，用于 requirePositive 流程中的校验、计算或对象转换。
     * @param payload 待处理业务值，用于规则计算、转换或外部调用。
     * @param key 业务键，用于在同一租户下定位资源。
     */
    private void requirePositive(Map<String, Object> payload, String key) {
        Object value = payload.get(key);
        BigDecimal number = BigDecimal.ZERO;
        if (value instanceof Number numeric) {
            number = new BigDecimal(numeric.toString());
        // 根据前序判断结果进入后续条件分支。
        } else if (value instanceof String text && !text.isBlank()) {
            number = new BigDecimal(text.trim());
        }
        if (number.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(key + " must be positive");
        }
    }

    /**
     * 校验并获取必需参数、资源或权限。
     *
     * @param keywordId 业务对象 ID，用于定位具体记录。
     * @param externalEntityId 业务对象 ID，用于定位具体记录。
     * @param operation 待调度任务或操作名称，用于封装阻塞工作。
     */
    private void requireEntityReference(Long keywordId, String externalEntityId, String operation) {
        if ((keywordId == null || keywordId <= 0) && trimToNull(externalEntityId) == null) {
            throw new IllegalArgumentException(operation + " requires keywordId or externalEntityId");
        }
    }

    /**
     * 转换为接口返回或领域视图。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @return 返回组装或转换后的结果对象。
     */
    private SearchMarketingMutationView toView(SearchMarketingMutationDO row) {
        // 汇总前面计算出的状态和明细，返回给调用方。
        return new SearchMarketingMutationView(
                row.getId(),
                row.getTenantId(),
                row.getSourceId(),
                row.getOpportunityId(),
                row.getKeywordId(),
                row.getProvider(),
                row.getChannel(),
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
    private Map<String, Object> providerRequestEvidence(SearchMarketingProviderMutationRequest request) {
        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("provider", request.provider());
        evidence.put("sourceKey", request.sourceKey());
        evidence.put("externalAccountId", defaultString(request.externalAccountId(), ""));
        evidence.put("mutationType", request.mutationType());
        evidence.put("entityType", request.entityType());
        evidence.put("externalEntityId", defaultString(request.externalEntityId(), ""));
        evidence.put("idempotencyKey", request.idempotencyKey());
        evidence.put("dryRun", request.dryRun());
        evidence.put("partialFailure", request.partialFailure());
        evidence.put("payload", request.payload());
        evidence.put("metadata", request.metadata());
        return ProviderWriteEvidenceSanitizer.sanitizeMap(evidence);
    }

    /**
     * 执行 providerResponseEvidence 流程，围绕 provider response evidence 完成校验、计算或结果组装。
     *
     * @param result result 参数，用于 providerResponseEvidence 流程中的校验、计算或对象转换。
     * @return 返回 providerResponseEvidence 流程生成的业务结果。
     */
    private Map<String, Object> providerResponseEvidence(SearchMarketingProviderMutationResult result) {
        Map<String, Object> evidence = new LinkedHashMap<>(
                ProviderWriteEvidenceSanitizer.sanitizeMap(result.response()));
        if (result.providerOperationId() != null) {
            evidence.put("providerOperationId", result.providerOperationId());
        }
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
            throw new IllegalArgumentException("mutation payload is required");
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
                    throw new IllegalArgumentException("mutation payload must not contain provider secrets");
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
            throw new IllegalArgumentException("unsupported search marketing mutation type");
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
            throw new IllegalStateException("creator cannot approve live search marketing provider mutation");
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
            throw new IllegalArgumentException("search marketing mutation metadata is not JSON serializable", ex);
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
            throw new IllegalStateException("Could not hash search marketing mutation request", ex);
        }
    }
}
