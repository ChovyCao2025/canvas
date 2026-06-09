package org.chovy.canvas.domain.creator;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.dal.dataobject.CreatorCampaignDO;
import org.chovy.canvas.dal.dataobject.CreatorCollaborationDO;
import org.chovy.canvas.dal.dataobject.CreatorDeliverableDO;
import org.chovy.canvas.dal.dataobject.CreatorProfileDO;
import org.chovy.canvas.dal.mapper.CreatorCampaignMapper;
import org.chovy.canvas.dal.mapper.CreatorCollaborationMapper;
import org.chovy.canvas.dal.mapper.CreatorDeliverableMapper;
import org.chovy.canvas.dal.mapper.CreatorProfileMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * CreatorCollaborationService 编排 domain.creator 场景的领域业务规则。
 */
@Service
public class CreatorCollaborationService {

    private static final BigDecimal ZERO_MONEY = new BigDecimal("0.0000");
    private static final BigDecimal ZERO_RATE = new BigDecimal("0.000000");
    private static final Set<String> COMPLETE_STATUSES = Set.of("POSTED", "APPROVED", "CANCELLED", "REJECTED");
    private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {
    };
    private static final TypeReference<Map<String, Object>> OBJECT_MAP = new TypeReference<>() {
    };

    private final CreatorProfileMapper profileMapper;
    private final CreatorCampaignMapper campaignMapper;
    private final CreatorCollaborationMapper collaborationMapper;
    private final CreatorDeliverableMapper deliverableMapper;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    /**
     * 创建 CreatorCollaborationService 实例并注入 domain.creator 场景依赖。
     * @param profileMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param campaignMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param collaborationMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param deliverableMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param objectMapper 依赖组件，用于完成数据访问或外部能力调用。
     */
    @Autowired
    public CreatorCollaborationService(CreatorProfileMapper profileMapper,
                                       CreatorCampaignMapper campaignMapper,
                                       CreatorCollaborationMapper collaborationMapper,
                                       CreatorDeliverableMapper deliverableMapper,
                                       ObjectMapper objectMapper) {
        this(profileMapper, campaignMapper, collaborationMapper, deliverableMapper, objectMapper,
                Clock.systemDefaultZone());
    }

    /**
     * 执行 CreatorCollaborationService 流程，围绕 creator collaboration service 完成校验、计算或结果组装。
     *
     * @param profileMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param campaignMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param collaborationMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param deliverableMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param objectMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param clock 时间参数，用于计算窗口、过期或审计时间。
     */
    CreatorCollaborationService(CreatorProfileMapper profileMapper,
                                CreatorCampaignMapper campaignMapper,
                                CreatorCollaborationMapper collaborationMapper,
                                CreatorDeliverableMapper deliverableMapper,
                                ObjectMapper objectMapper,
                                Clock clock) {
        this.profileMapper = profileMapper;
        this.campaignMapper = campaignMapper;
        this.collaborationMapper = collaborationMapper;
        this.deliverableMapper = deliverableMapper;
        this.objectMapper = objectMapper == null ? new ObjectMapper() : objectMapper;
        this.clock = clock == null ? Clock.systemDefaultZone() : clock;
    }

    /**
     * 新增或更新租户内达人档案。
     * 以 Provider 和 handleKey 唯一定位，写入粉丝、互动率、标签、风险状态和元数据，返回最新档案视图。
     */
    public CreatorProfileView upsertCreator(Long tenantId, CreatorProfileCommand command, String actor) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (command == null) {
            throw new IllegalArgumentException("creator profile command is required");
        }
        Long scopedTenantId = normalizeTenant(tenantId);
        String provider = normalizeUpper(command.provider(), "provider");
        String handle = normalizeHandle(command.handle());
        String handleKey = handleKey(handle);
        LocalDateTime changedAt = now();
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        CreatorProfileDO row = profileMapper.selectOne(new LambdaQueryWrapper<CreatorProfileDO>()
                .eq(CreatorProfileDO::getTenantId, scopedTenantId)
                .eq(CreatorProfileDO::getProvider, provider)
                .eq(CreatorProfileDO::getHandleKey, handleKey)
                .last("LIMIT 1"));
        if (row == null) {
            row = new CreatorProfileDO();
            row.setTenantId(scopedTenantId);
            row.setProvider(provider);
            row.setHandleKey(handleKey);
            row.setCreatedBy(defaultString(actor, "system"));
            row.setCreatedAt(changedAt);
        }
        row.setHandle(handle);
        row.setDisplayName(defaultString(command.displayName(), handle));
        row.setCreatorTier(normalizeOptionalUpper(command.creatorTier()));
        row.setPrimaryChannel(normalizeOptionalUpper(command.primaryChannel()));
        row.setFollowerCount(nonNegative(command.followerCount()));
        row.setAvgEngagementRate(rate(command.avgEngagementRate()));
        row.setTagsJson(json(command.tags()));
        row.setStatus(normalizeUpper(defaultString(command.status(), "ACTIVE"), "status"));
        row.setRiskStatus(normalizeUpper(defaultString(command.riskStatus(), "NORMAL"), "riskStatus"));
        row.setMetadataJson(json(command.metadata()));
        row.setUpdatedAt(changedAt);
        if (row.getId() == null) {
            profileMapper.insert(row);
        } else {
            profileMapper.updateById(row);
        }
        // 汇总前面计算出的状态和明细，返回给调用方。
        return toProfileView(row);
    }

    /**
     * 新增或更新租户内达人营销活动。
     * 以 campaignKey 唯一定位，保存目标、预算、币种、周期和状态，不触发外部达人平台操作。
     */
    public CreatorCampaignView upsertCampaign(Long tenantId, CreatorCampaignCommand command, String actor) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (command == null) {
            throw new IllegalArgumentException("creator campaign command is required");
        }
        Long scopedTenantId = normalizeTenant(tenantId);
        String campaignKey = required(command.campaignKey(), "campaignKey");
        LocalDateTime changedAt = now();
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        CreatorCampaignDO row = campaignMapper.selectOne(new LambdaQueryWrapper<CreatorCampaignDO>()
                .eq(CreatorCampaignDO::getTenantId, scopedTenantId)
                .eq(CreatorCampaignDO::getCampaignKey, campaignKey)
                .last("LIMIT 1"));
        if (row == null) {
            row = new CreatorCampaignDO();
            row.setTenantId(scopedTenantId);
            row.setCampaignKey(campaignKey);
            row.setCreatedBy(defaultString(actor, "system"));
            row.setCreatedAt(changedAt);
        }
        row.setCampaignName(defaultString(command.campaignName(), campaignKey));
        row.setObjective(normalizeOptionalUpper(command.objective()));
        row.setBudgetAmount(money(command.budgetAmount()));
        row.setCurrency(normalizeUpper(defaultString(command.currency(), "CNY"), "currency"));
        row.setStartAt(command.startAt());
        row.setEndAt(command.endAt());
        row.setStatus(normalizeUpper(defaultString(command.status(), "DRAFT"), "status"));
        row.setMetadataJson(json(command.metadata()));
        row.setUpdatedAt(changedAt);
        if (row.getId() == null) {
            campaignMapper.insert(row);
        } else {
            campaignMapper.updateById(row);
        }
        // 汇总前面计算出的状态和明细，返回给调用方。
        return toCampaignView(row);
    }

    /**
     * 新增或更新活动与达人的合作关系。
     * 会校验活动和达人都属于当前租户，保存报价、佣金、追踪链接、折扣码、权限和状态。
     */
    public CreatorCollaborationView upsertCollaboration(Long tenantId,
                                                        CreatorCollaborationCommand command,
                                                        String actor) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (command == null) {
            throw new IllegalArgumentException("creator collaboration command is required");
        }
        Long scopedTenantId = normalizeTenant(tenantId);
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        CreatorCampaignDO campaign = campaignMapper.selectById(requiredId(command.campaignId(), "campaignId"));
        validateTenant(scopedTenantId, campaign == null ? null : campaign.getTenantId(), "campaign");
        CreatorProfileDO creator = profileMapper.selectById(requiredId(command.creatorId(), "creatorId"));
        validateTenant(scopedTenantId, creator == null ? null : creator.getTenantId(), "creator");
        LocalDateTime changedAt = now();
        CreatorCollaborationDO row = collaborationMapper.selectOne(new LambdaQueryWrapper<CreatorCollaborationDO>()
                .eq(CreatorCollaborationDO::getTenantId, scopedTenantId)
                .eq(CreatorCollaborationDO::getCampaignId, command.campaignId())
                .eq(CreatorCollaborationDO::getCreatorId, command.creatorId())
                .last("LIMIT 1"));
        if (row == null) {
            row = new CreatorCollaborationDO();
            row.setTenantId(scopedTenantId);
            row.setCampaignId(command.campaignId());
            row.setCreatorId(command.creatorId());
            row.setCreatedBy(defaultString(actor, "system"));
            row.setCreatedAt(changedAt);
        }
        row.setOfferType(normalizeOptionalUpper(command.offerType()));
        row.setFixedFeeAmount(money(command.fixedFeeAmount()));
        row.setCommissionRate(rate(command.commissionRate()));
        row.setTrackingLink(trimToNull(command.trackingLink()));
        row.setDiscountCode(trimToNull(command.discountCode()));
        row.setStatus(normalizeUpper(defaultString(command.status(), "NEGOTIATING"), "status"));
        row.setPermissionsMetadataJson(json(command.permissionsMetadata()));
        row.setMetadataJson(json(command.metadata()));
        row.setUpdatedAt(changedAt);
        if (row.getId() == null) {
            collaborationMapper.insert(row);
        } else {
            collaborationMapper.updateById(row);
        }
        // 汇总前面计算出的状态和明细，返回给调用方。
        return toCollaborationView(row);
    }

    /**
     * 新增或更新达人合作交付物。
     * 会校验合作归属租户，保存内容平台、发布时间、内容链接和曝光/互动/转化/收入指标。
     */
    public CreatorDeliverableView upsertDeliverable(Long tenantId, CreatorDeliverableCommand command, String actor) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (command == null) {
            throw new IllegalArgumentException("creator deliverable command is required");
        }
        Long scopedTenantId = normalizeTenant(tenantId);
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        CreatorCollaborationDO collaboration = collaborationMapper.selectById(requiredId(
                command.collaborationId(), "collaborationId"));
        validateTenant(scopedTenantId, collaboration == null ? null : collaboration.getTenantId(), "collaboration");
        String deliverableKey = required(command.deliverableKey(), "deliverableKey");
        LocalDateTime changedAt = now();
        CreatorDeliverableDO row = deliverableMapper.selectOne(new LambdaQueryWrapper<CreatorDeliverableDO>()
                .eq(CreatorDeliverableDO::getTenantId, scopedTenantId)
                .eq(CreatorDeliverableDO::getCollaborationId, command.collaborationId())
                .eq(CreatorDeliverableDO::getDeliverableKey, deliverableKey)
                .last("LIMIT 1"));
        if (row == null) {
            row = new CreatorDeliverableDO();
            row.setTenantId(scopedTenantId);
            row.setCollaborationId(command.collaborationId());
            row.setCampaignId(collaboration.getCampaignId());
            row.setCreatorId(collaboration.getCreatorId());
            row.setDeliverableKey(deliverableKey);
            row.setCreatedBy(defaultString(actor, "system"));
            row.setCreatedAt(changedAt);
        }
        row.setContentType(normalizeUpper(defaultString(command.contentType(), "CONTENT"), "contentType"));
        row.setPlatform(normalizeOptionalUpper(command.platform()));
        row.setDueAt(command.dueAt());
        row.setPostedAt(command.postedAt());
        row.setContentUrl(trimToNull(command.contentUrl()));
        row.setStatus(normalizeUpper(defaultString(command.status(), "PLANNED"), "status"));
        row.setImpressionCount(nonNegative(command.impressionCount()));
        row.setLikeCount(nonNegative(command.likeCount()));
        row.setCommentCount(nonNegative(command.commentCount()));
        row.setShareCount(nonNegative(command.shareCount()));
        row.setSaveCount(nonNegative(command.saveCount()));
        row.setClickCount(nonNegative(command.clickCount()));
        row.setConversionCount(nonNegative(command.conversionCount()));
        row.setRevenueAmount(money(command.revenueAmount()));
        row.setMetadataJson(json(command.metadata()));
        row.setUpdatedAt(changedAt);
        if (row.getId() == null) {
            deliverableMapper.insert(row);
        } else {
            deliverableMapper.updateById(row);
        }
        // 汇总前面计算出的状态和明细，返回给调用方。
        return toDeliverableView(row);
    }

    /**
     * 汇总租户内达人合作表现。
     * 可按活动、达人或合作过滤，聚合交付物指标并计算完成度、成本和转化表现。
     */
    public CreatorPerformanceSummaryView summary(Long tenantId, CreatorPerformanceSummaryQuery query) {
        if (query == null) {
            throw new IllegalArgumentException("creator performance summary query is required");
        }
        Long scopedTenantId = normalizeTenant(tenantId);
        LocalDateTime evaluatedAt = query.evaluatedAt() == null ? now() : query.evaluatedAt();
        List<CreatorCollaborationDO> collaborations = safeList(collaborationMapper.selectList(
                new LambdaQueryWrapper<CreatorCollaborationDO>()
                        .eq(CreatorCollaborationDO::getTenantId, scopedTenantId)
                        .eq(query.campaignId() != null, CreatorCollaborationDO::getCampaignId, query.campaignId())
                        .eq(query.creatorId() != null, CreatorCollaborationDO::getCreatorId, query.creatorId())
                        .eq(query.collaborationId() != null, CreatorCollaborationDO::getId, query.collaborationId())));
        List<CreatorCollaborationDO> scopedCollaborations = collaborations.stream()
                .filter(row -> scopedTenantId.equals(row.getTenantId()))
                .filter(row -> query.campaignId() == null || Objects.equals(query.campaignId(), row.getCampaignId()))
                .filter(row -> query.creatorId() == null || Objects.equals(query.creatorId(), row.getCreatorId()))
                .filter(row -> query.collaborationId() == null || Objects.equals(query.collaborationId(), row.getId()))
                .toList();
        Set<Long> collaborationIds = scopedCollaborations.stream()
                .map(CreatorCollaborationDO::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toUnmodifiableSet());
        Map<Long, CreatorCollaborationDO> collaborationsById = scopedCollaborations.stream()
                .filter(row -> row.getId() != null)
                .collect(Collectors.toMap(CreatorCollaborationDO::getId, Function.identity(), (left, right) -> left));
        List<CreatorDeliverableDO> deliverables = collaborationIds.isEmpty()
                ? List.of()
                /**
                 * 按安全边界裁剪或保护输入值。
                 *
                 * @return 返回 safeList 流程生成的业务结果。
                 */
                : safeList(deliverableMapper.selectList(new LambdaQueryWrapper<CreatorDeliverableDO>()
                .eq(CreatorDeliverableDO::getTenantId, scopedTenantId)
                .eq(query.campaignId() != null, CreatorDeliverableDO::getCampaignId, query.campaignId())
                .eq(query.creatorId() != null, CreatorDeliverableDO::getCreatorId, query.creatorId())
                .eq(query.collaborationId() != null, CreatorDeliverableDO::getCollaborationId,
                        query.collaborationId())));
        List<CreatorDeliverableDO> scopedDeliverables = deliverables.stream()
                .filter(row -> scopedTenantId.equals(row.getTenantId()))
                .filter(row -> collaborationIds.contains(row.getCollaborationId()))
                .filter(row -> query.campaignId() == null || Objects.equals(query.campaignId(), row.getCampaignId()))
                .filter(row -> query.creatorId() == null || Objects.equals(query.creatorId(), row.getCreatorId()))
                .filter(row -> query.collaborationId() == null
                        || Objects.equals(query.collaborationId(), row.getCollaborationId()))
                .toList();

        BigDecimal revenue = ZERO_MONEY;
        BigDecimal commission = ZERO_MONEY;
        long impressions = 0L;
        long engagement = 0L;
        long clicks = 0L;
        long conversions = 0L;
        int posted = 0;
        int overdue = 0;
        for (CreatorDeliverableDO deliverable : scopedDeliverables) {
            BigDecimal deliverableRevenue = money(deliverable.getRevenueAmount());
            revenue = revenue.add(deliverableRevenue);
            CreatorCollaborationDO collaboration = collaborationsById.get(deliverable.getCollaborationId());
            commission = commission.add(deliverableRevenue.multiply(rate(
                    collaboration == null ? null : collaboration.getCommissionRate())));
            impressions += nonNegative(deliverable.getImpressionCount());
            engagement += nonNegative(deliverable.getLikeCount())
                    /**
                     * 执行 nonNegative 流程，围绕 non negative 完成校验、计算或结果组装。
                     *
                     * @return 返回 nonNegative 流程生成的业务结果。
                     */
                    + nonNegative(deliverable.getCommentCount())
                    /**
                     * 执行 nonNegative 流程，围绕 non negative 完成校验、计算或结果组装。
                     *
                     * @return 返回 nonNegative 流程生成的业务结果。
                     */
                    + nonNegative(deliverable.getShareCount())
                    /**
                     * 执行 nonNegative 流程，围绕 non negative 完成校验、计算或结果组装。
                     *
                     * @return 返回 nonNegative 流程生成的业务结果。
                     */
                    + nonNegative(deliverable.getSaveCount());
            clicks += nonNegative(deliverable.getClickCount());
            conversions += nonNegative(deliverable.getConversionCount());
            String status = normalizeOptionalUpper(deliverable.getStatus());
            if ("POSTED".equals(status) || "APPROVED".equals(status)) {
                posted++;
            }
            if (deliverable.getDueAt() != null
                    && deliverable.getDueAt().isBefore(evaluatedAt)
                    && !COMPLETE_STATUSES.contains(defaultString(status, "PLANNED"))) {
                overdue++;
            }
        }
        BigDecimal fixedFee = scopedCollaborations.stream()
                .map(CreatorCollaborationDO::getFixedFeeAmount)
                .map(this::money)
                .reduce(ZERO_MONEY, BigDecimal::add);
        BigDecimal totalCost = fixedFee.add(commission);
        BigDecimal roi = totalCost.compareTo(BigDecimal.ZERO) > 0
                ? revenue.subtract(totalCost).divide(totalCost, 6, RoundingMode.HALF_UP)
                : ZERO_RATE;
        return new CreatorPerformanceSummaryView(
                scopedTenantId,
                query.campaignId(),
                query.creatorId(),
                query.collaborationId(),
                scopedDeliverables.size(),
                posted,
                overdue,
                impressions,
                engagement,
                clicks,
                conversions,
                revenue,
                fixedFee,
                commission,
                totalCost,
                roi,
                evaluatedAt);
    }

    /**
     * 转换为接口返回或领域视图。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @return 返回组装或转换后的结果对象。
     */
    private CreatorProfileView toProfileView(CreatorProfileDO row) {
        // 汇总前面计算出的状态和明细，返回给调用方。
        return new CreatorProfileView(
                row.getId(),
                row.getTenantId(),
                row.getProvider(),
                row.getHandle(),
                row.getHandleKey(),
                row.getDisplayName(),
                row.getCreatorTier(),
                row.getPrimaryChannel(),
                nonNegative(row.getFollowerCount()),
                rate(row.getAvgEngagementRate()),
                list(row.getTagsJson()),
                row.getStatus(),
                row.getRiskStatus(),
                map(row.getMetadataJson()),
                row.getCreatedBy(),
                row.getCreatedAt(),
                // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
                row.getUpdatedAt());
    }

    /**
     * 转换为接口返回或领域视图。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @return 返回组装或转换后的结果对象。
     */
    private CreatorCampaignView toCampaignView(CreatorCampaignDO row) {
        return new CreatorCampaignView(
                row.getId(),
                row.getTenantId(),
                row.getCampaignKey(),
                row.getCampaignName(),
                row.getObjective(),
                money(row.getBudgetAmount()),
                row.getCurrency(),
                row.getStartAt(),
                row.getEndAt(),
                row.getStatus(),
                map(row.getMetadataJson()),
                row.getCreatedBy(),
                row.getCreatedAt(),
                row.getUpdatedAt());
    }

    /**
     * 转换为接口返回或领域视图。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @return 返回组装或转换后的结果对象。
     */
    private CreatorCollaborationView toCollaborationView(CreatorCollaborationDO row) {
        return new CreatorCollaborationView(
                row.getId(),
                row.getTenantId(),
                row.getCampaignId(),
                row.getCreatorId(),
                row.getOfferType(),
                money(row.getFixedFeeAmount()),
                rate(row.getCommissionRate()),
                row.getTrackingLink(),
                row.getDiscountCode(),
                row.getStatus(),
                map(row.getPermissionsMetadataJson()),
                map(row.getMetadataJson()),
                row.getCreatedBy(),
                row.getCreatedAt(),
                row.getUpdatedAt());
    }

    /**
     * 转换为接口返回或领域视图。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @return 返回组装或转换后的结果对象。
     */
    private CreatorDeliverableView toDeliverableView(CreatorDeliverableDO row) {
        // 汇总前面计算出的状态和明细，返回给调用方。
        return new CreatorDeliverableView(
                row.getId(),
                row.getTenantId(),
                row.getCollaborationId(),
                row.getCampaignId(),
                row.getCreatorId(),
                row.getDeliverableKey(),
                row.getContentType(),
                row.getPlatform(),
                row.getDueAt(),
                row.getPostedAt(),
                row.getContentUrl(),
                row.getStatus(),
                nonNegative(row.getImpressionCount()),
                nonNegative(row.getLikeCount()),
                nonNegative(row.getCommentCount()),
                nonNegative(row.getShareCount()),
                // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
                nonNegative(row.getSaveCount()),
                nonNegative(row.getClickCount()),
                nonNegative(row.getConversionCount()),
                money(row.getRevenueAmount()),
                map(row.getMetadataJson()),
                row.getCreatedBy(),
                row.getCreatedAt(),
                row.getUpdatedAt());
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
     * 规范化输入值。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private String normalizeHandle(String value) {
        return required(value, "handle");
    }

    /**
     * 执行核心业务处理流程。
     *
     * @param handle handle 参数，用于 handleKey 流程中的校验、计算或对象转换。
     * @return 返回 handle key 生成的文本或业务键。
     */
    private String handleKey(String handle) {
        String value = handle.startsWith("@") ? handle.substring(1) : handle;
        String key = value.trim().toLowerCase(Locale.ROOT);
        if (key.isBlank()) {
            throw new IllegalArgumentException("handle is required");
        }
        return key;
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
     * 执行 nonNegative 流程，围绕 non negative 完成校验、计算或结果组装。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 non negative 计算得到的数量、金额或指标值。
     */
    private Long nonNegative(Long value) {
        return value == null || value < 0 ? 0L : value;
    }

    /**
     * 执行 money 流程，围绕 money 完成校验、计算或结果组装。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 money 计算得到的数量、金额或指标值。
     */
    private BigDecimal money(BigDecimal value) {
        return value == null || value.compareTo(BigDecimal.ZERO) < 0 ? ZERO_MONEY : value;
    }

    /**
     * 执行 rate 流程，围绕 rate 完成校验、计算或结果组装。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 rate 计算得到的数量、金额或指标值。
     */
    private BigDecimal rate(BigDecimal value) {
        return value == null || value.compareTo(BigDecimal.ZERO) < 0 ? ZERO_RATE : value;
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
            throw new IllegalArgumentException("creator collaboration metadata is not JSON serializable", ex);
        }
    }

    /**
     * 查询或读取业务数据。
     *
     * @param json JSON 字符串，承载结构化配置或明细。
     * @return 返回符合条件的数据列表或视图。
     */
    private List<String> list(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            List<String> values = objectMapper.readValue(json, STRING_LIST);
            return values == null ? List.of() : values;
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (JsonProcessingException ex) {
            return List.of();
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
}
