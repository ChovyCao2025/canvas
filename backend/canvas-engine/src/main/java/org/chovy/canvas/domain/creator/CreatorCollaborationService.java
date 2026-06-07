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

    @Autowired
    public CreatorCollaborationService(CreatorProfileMapper profileMapper,
                                       CreatorCampaignMapper campaignMapper,
                                       CreatorCollaborationMapper collaborationMapper,
                                       CreatorDeliverableMapper deliverableMapper,
                                       ObjectMapper objectMapper) {
        this(profileMapper, campaignMapper, collaborationMapper, deliverableMapper, objectMapper,
                Clock.systemDefaultZone());
    }

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

    public CreatorProfileView upsertCreator(Long tenantId, CreatorProfileCommand command, String actor) {
        if (command == null) {
            throw new IllegalArgumentException("creator profile command is required");
        }
        Long scopedTenantId = normalizeTenant(tenantId);
        String provider = normalizeUpper(command.provider(), "provider");
        String handle = normalizeHandle(command.handle());
        String handleKey = handleKey(handle);
        LocalDateTime changedAt = now();
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
        return toProfileView(row);
    }

    public CreatorCampaignView upsertCampaign(Long tenantId, CreatorCampaignCommand command, String actor) {
        if (command == null) {
            throw new IllegalArgumentException("creator campaign command is required");
        }
        Long scopedTenantId = normalizeTenant(tenantId);
        String campaignKey = required(command.campaignKey(), "campaignKey");
        LocalDateTime changedAt = now();
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
        return toCampaignView(row);
    }

    public CreatorCollaborationView upsertCollaboration(Long tenantId,
                                                        CreatorCollaborationCommand command,
                                                        String actor) {
        if (command == null) {
            throw new IllegalArgumentException("creator collaboration command is required");
        }
        Long scopedTenantId = normalizeTenant(tenantId);
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
        return toCollaborationView(row);
    }

    public CreatorDeliverableView upsertDeliverable(Long tenantId, CreatorDeliverableCommand command, String actor) {
        if (command == null) {
            throw new IllegalArgumentException("creator deliverable command is required");
        }
        Long scopedTenantId = normalizeTenant(tenantId);
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
        return toDeliverableView(row);
    }

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
                    + nonNegative(deliverable.getCommentCount())
                    + nonNegative(deliverable.getShareCount())
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

    private CreatorProfileView toProfileView(CreatorProfileDO row) {
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
                row.getUpdatedAt());
    }

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

    private CreatorDeliverableView toDeliverableView(CreatorDeliverableDO row) {
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
                nonNegative(row.getSaveCount()),
                nonNegative(row.getClickCount()),
                nonNegative(row.getConversionCount()),
                money(row.getRevenueAmount()),
                map(row.getMetadataJson()),
                row.getCreatedBy(),
                row.getCreatedAt(),
                row.getUpdatedAt());
    }

    private void validateTenant(Long expectedTenantId, Long actualTenantId, String resource) {
        if (!expectedTenantId.equals(actualTenantId)) {
            throw new IllegalArgumentException(resource + " does not belong to current tenant");
        }
    }

    private Long normalizeTenant(Long tenantId) {
        if (tenantId == null || tenantId < 0) {
            return 0L;
        }
        return tenantId;
    }

    private Long requiredId(Long value, String field) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value;
    }

    private String required(String value, String field) {
        String trimmed = trimToNull(value);
        if (trimmed == null) {
            throw new IllegalArgumentException(field + " is required");
        }
        return trimmed;
    }

    private String normalizeUpper(String value, String field) {
        return required(value, field).toUpperCase(Locale.ROOT);
    }

    private String normalizeOptionalUpper(String value) {
        String trimmed = trimToNull(value);
        return trimmed == null ? null : trimmed.toUpperCase(Locale.ROOT);
    }

    private String normalizeHandle(String value) {
        return required(value, "handle");
    }

    private String handleKey(String handle) {
        String value = handle.startsWith("@") ? handle.substring(1) : handle;
        String key = value.trim().toLowerCase(Locale.ROOT);
        if (key.isBlank()) {
            throw new IllegalArgumentException("handle is required");
        }
        return key;
    }

    private String defaultString(String value, String fallback) {
        String trimmed = trimToNull(value);
        return trimmed == null ? fallback : trimmed;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private Long nonNegative(Long value) {
        return value == null || value < 0 ? 0L : value;
    }

    private BigDecimal money(BigDecimal value) {
        return value == null || value.compareTo(BigDecimal.ZERO) < 0 ? ZERO_MONEY : value;
    }

    private BigDecimal rate(BigDecimal value) {
        return value == null || value.compareTo(BigDecimal.ZERO) < 0 ? ZERO_RATE : value;
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }

    private String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value == null ? Map.of() : value);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("creator collaboration metadata is not JSON serializable", ex);
        }
    }

    private List<String> list(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            List<String> values = objectMapper.readValue(json, STRING_LIST);
            return values == null ? List.of() : values;
        } catch (JsonProcessingException ex) {
            return List.of();
        }
    }

    private Map<String, Object> map(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            Map<String, Object> values = objectMapper.readValue(json, OBJECT_MAP);
            return values == null ? Map.of() : values;
        } catch (JsonProcessingException ex) {
            return Map.of();
        }
    }

    private <T> List<T> safeList(List<T> rows) {
        return rows == null ? List.of() : rows;
    }
}
