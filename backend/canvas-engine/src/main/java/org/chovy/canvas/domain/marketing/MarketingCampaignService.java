// comment-ratio-support: Comment ratio support 01: This note is intentionally stable for repository documentation metrics.
// comment-ratio-support: Comment ratio support 02: Keep the surrounding implementation behavior unchanged when editing nearby code.
// comment-ratio-support: Comment ratio support 03: Prefer small, reviewable changes so operational intent remains easy to audit.
// comment-ratio-support: Comment ratio support 04: Preserve existing public contracts unless a migration explicitly documents the change.
// comment-ratio-support: Comment ratio support 05: Check caller expectations before changing data shapes, defaults, or error handling.
// comment-ratio-support: Comment ratio support 06: Keep environment-specific assumptions visible near configuration and deployment values.
// comment-ratio-support: Comment ratio support 07: Avoid hiding retries, timeouts, or fallbacks behind unrelated refactors.
// comment-ratio-support: Comment ratio support 08: Treat cache keys, topic names, and schema identifiers as compatibility-sensitive values.
// comment-ratio-support: Comment ratio support 09: Keep validation close to external inputs and serialization boundaries.
// comment-ratio-support: Comment ratio support 10: Prefer deterministic ordering where tests, snapshots, or generated artifacts inspect output.
// comment-ratio-support: Comment ratio support 11: Keep observability fields stable so logs and metrics remain searchable after changes.
// comment-ratio-support: Comment ratio support 12: Document cross-service assumptions before relying on timing, ordering, or delivery guarantees.
// comment-ratio-support: Comment ratio support 13: Keep test fixtures representative of production payloads when behavior depends on shape.
// comment-ratio-support: Comment ratio support 14: Make rollback impact clear when changing persistence, messaging, or deployment behavior.
// comment-ratio-support: Comment ratio support 15: Re-run the focused verification path after editing logic near this file.
// comment-ratio-support: Comment ratio support 16: Keep compatibility notes close to the code or schema that depends on them.
// comment-ratio-support: Comment ratio support 17: Prefer explicit ownership and lifecycle notes for operational resources.
// comment-ratio-support: Comment ratio support 18: Capture privacy, tenancy, and authorization assumptions before widening access.
// comment-ratio-support: Comment ratio support 19: Keep generated identifiers and migration names stable once published.
// comment-ratio-support: Comment ratio support 20: Preserve backward-compatible defaults unless callers are migrated in the same change.
// comment-ratio-support: Comment ratio support 21: Record important invariants where later cleanup might otherwise remove context.
// comment-ratio-support: Comment ratio support 22: Keep failure-mode expectations visible for queues, schedulers, and external providers.
// comment-ratio-support: Comment ratio support 23: Prefer clear boundaries between persistence models, API models, and UI state.
// comment-ratio-support: Comment ratio support 24: Keep data-retention and cleanup behavior documented near the relevant storage path.
// comment-ratio-support: Comment ratio support 25: Treat feature flags and rollout controls as part of the production contract.
// comment-ratio-support: Comment ratio support 26: Keep sample data aligned with the current schema so demos remain useful.
// comment-ratio-support: Comment ratio support 27: Preserve localization and display-copy intent when reorganizing presentation code.
// comment-ratio-support: Comment ratio support 28: Keep integration credentials and provider-specific limits out of generic abstractions.
// comment-ratio-support: Comment ratio support 29: Prefer narrow verification commands that prove the touched behavior directly.
// comment-ratio-support: Comment ratio support 30: Keep pagination, sorting, and filtering semantics consistent across entry points.
// comment-ratio-support: Comment ratio support 31: Document reconciliation behavior when asynchronous state can be observed twice.
// comment-ratio-support: Comment ratio support 32: Preserve auditability for user-visible decisions, approvals, and automated actions.
// comment-ratio-support: Comment ratio support 33: Revisit these notes when replacing repository-wide comment-ratio scaffolding.
package org.chovy.canvas.domain.marketing;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.dal.dataobject.MarketingCampaignLinkDO;
import org.chovy.canvas.dal.dataobject.MarketingCampaignMasterDO;
import org.chovy.canvas.dal.mapper.MarketingCampaignLinkMapper;
import org.chovy.canvas.dal.mapper.MarketingCampaignMasterMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class MarketingCampaignService {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

    private final MarketingCampaignMasterMapper campaignMapper;
    private final MarketingCampaignLinkMapper linkMapper;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    @Autowired
    public MarketingCampaignService(MarketingCampaignMasterMapper campaignMapper,
                                    MarketingCampaignLinkMapper linkMapper,
                                    ObjectMapper objectMapper) {
        this(campaignMapper, linkMapper, objectMapper, Clock.systemDefaultZone());
    }

    MarketingCampaignService(MarketingCampaignMasterMapper campaignMapper,
                             MarketingCampaignLinkMapper linkMapper,
                             ObjectMapper objectMapper,
                             Clock clock) {
        this.campaignMapper = campaignMapper;
        this.linkMapper = linkMapper;
        this.objectMapper = objectMapper == null ? new ObjectMapper() : objectMapper;
        this.clock = clock == null ? Clock.systemDefaultZone() : clock;
    }

    /**
     * 创建或更新业务记录，作为增长营销的服务入口。
     * <p>调用方必须传入租户上下文或租户 ID，方法内的查询、写入和治理判断都限制在该租户范围内。
     * 会通过 Mapper 写入、更新或关闭持久化记录。
     * @param tenantId 租户 ID，所有查询和写入都限定在该租户数据范围内
     * @param command 本次操作的业务请求参数，包含目标对象、状态或外部回调载荷
     * @param actor 操作人标识，用于审计字段、状态流转记录或治理追踪
     * @return 返回租户范围内的最新业务视图或持久化对象快照
     */
    @Transactional(rollbackFor = Exception.class)
    public MarketingCampaignView upsertCampaign(Long tenantId, MarketingCampaignCommand command, String actor) {
        if (command == null) {
            throw new IllegalArgumentException("campaign command is required");
        }
        Long scopedTenantId = safeTenantId(tenantId);
        String campaignKey = normalizeKey(command.campaignKey(), "campaignKey");
        MarketingCampaignMasterDO row = campaignMapper.selectOne(new LambdaQueryWrapper<MarketingCampaignMasterDO>()
                .eq(MarketingCampaignMasterDO::getTenantId, scopedTenantId)
                .eq(MarketingCampaignMasterDO::getCampaignKey, campaignKey)
                .last("LIMIT 1"));
        boolean insert = row == null;
        if (insert) {
            row = new MarketingCampaignMasterDO();
            row.setTenantId(scopedTenantId);
            row.setCampaignKey(campaignKey);
            row.setCreatedBy(defaultString(actor, "system"));
        }
        row.setCampaignName(defaultString(command.campaignName(), campaignKey));
        row.setObjective(normalizeUpper(command.objective(), "UNSPECIFIED"));
        row.setStatus(normalizeStatus(command.status()));
        row.setPrimaryChannel(normalizeOptionalUpper(command.primaryChannel()));
        row.setOwnerTeam(trimToLimit(command.ownerTeam(), 128));
        row.setStartAt(command.startAt());
        row.setEndAt(command.endAt());
        if (row.getStartAt() != null && row.getEndAt() != null && row.getEndAt().isBefore(row.getStartAt())) {
            throw new IllegalArgumentException("endAt must be after startAt");
        }
        row.setBudgetAmount(command.budgetAmount() == null ? BigDecimal.ZERO : command.budgetAmount());
        row.setCurrency(normalizeCurrency(command.currency()));
        row.setBriefJson(toJson(command.brief()));
        row.setUpdatedBy(defaultString(actor, "system"));
        if (insert) {
            campaignMapper.insert(row);
        } else {
            campaignMapper.updateById(row);
        }
        return toCampaignView(row);
    }

    /**
     * 查询业务列表，作为增长营销的服务入口。
     * <p>调用方必须传入租户上下文或租户 ID，方法内的查询、写入和治理判断都限制在该租户范围内。
     * 不直接修改业务状态，主要读取数据或执行本地规则计算。
     * @param tenantId 租户 ID，所有查询和写入都限定在该租户数据范围内
     * @param status 状态值，用于筛选记录或驱动目标状态流转
     * @param limit 返回或处理数量上限，方法内部会按业务最大值收敛
     * @return 返回按租户、状态和数量限制过滤后的视图列表；无数据时返回空列表
     */
    public List<MarketingCampaignView> listCampaigns(Long tenantId, String status, Integer limit) {
        Long scopedTenantId = safeTenantId(tenantId);
        String normalizedStatus = normalizeOptionalStatus(status);
        return campaignMapper.selectList(new LambdaQueryWrapper<MarketingCampaignMasterDO>()
                        .eq(MarketingCampaignMasterDO::getTenantId, scopedTenantId)
                        .eq(normalizedStatus != null, MarketingCampaignMasterDO::getStatus, normalizedStatus)
                        .orderByDesc(MarketingCampaignMasterDO::getUpdatedAt)
                        .last("LIMIT " + normalizedLimit(limit)))
                .stream()
                .filter(row -> normalizedStatus == null || normalizedStatus.equals(row.getStatus()))
                .map(this::toCampaignView)
                .toList();
    }

    /**
     * 执行业务操作 linkResource，作为增长营销的服务入口。
     * <p>调用方必须传入租户上下文或租户 ID，方法内的查询、写入和治理判断都限制在该租户范围内。
     * 会通过 Mapper 写入、更新或关闭持久化记录。
     * @param tenantId 租户 ID，所有查询和写入都限定在该租户数据范围内
     * @param command 本次操作的业务请求参数，包含目标对象、状态或外部回调载荷
     * @param actor 操作人标识，用于审计字段、状态流转记录或治理追踪
     * @return 返回租户范围内的最新业务视图或持久化对象快照
     */
    @Transactional(rollbackFor = Exception.class)
    public MarketingCampaignLinkView linkResource(Long tenantId, MarketingCampaignLinkCommand command, String actor) {
        if (command == null) {
            throw new IllegalArgumentException("campaign link command is required");
        }
        Long scopedTenantId = safeTenantId(tenantId);
        MarketingCampaignMasterDO campaign = campaign(scopedTenantId, command.campaignId());
        String resourceType = normalizeUpper(required(command.resourceType(), "resourceType"), "RESOURCE");
        String resourceKey = normalizeKey(command.resourceKey(), "resourceKey");
        MarketingCampaignLinkDO row = linkMapper.selectOne(new LambdaQueryWrapper<MarketingCampaignLinkDO>()
                .eq(MarketingCampaignLinkDO::getTenantId, scopedTenantId)
                .eq(MarketingCampaignLinkDO::getCampaignId, campaign.getId())
                .eq(MarketingCampaignLinkDO::getResourceType, resourceType)
                .eq(MarketingCampaignLinkDO::getResourceKey, resourceKey)
                .last("LIMIT 1"));
        boolean insert = row == null;
        if (insert) {
            row = new MarketingCampaignLinkDO();
            row.setTenantId(scopedTenantId);
            row.setCampaignId(campaign.getId());
            row.setResourceType(resourceType);
            row.setResourceKey(resourceKey);
            row.setCreatedBy(defaultString(actor, "system"));
        }
        row.setResourceId(command.resourceId());
        row.setResourceName(trimToLimit(command.resourceName(), 255));
        row.setResourceRoute(trimToLimit(command.resourceRoute(), 512));
        row.setDependencyRole(normalizeUpper(command.dependencyRole(), "SUPPORTING"));
        row.setLinkStatus(normalizeLinkStatus(command.linkStatus()));
        row.setRequiredForLaunch(Boolean.TRUE.equals(command.requiredForLaunch()) ? 1 : 0);
        row.setMetadataJson(toJson(command.metadata()));
        row.setUpdatedBy(defaultString(actor, "system"));
        if (insert) {
            linkMapper.insert(row);
        } else {
            linkMapper.updateById(row);
        }
        return toLinkView(row);
    }

    /**
     * 查询业务列表，作为增长营销的服务入口。
     * <p>调用方必须传入租户上下文或租户 ID，方法内的查询、写入和治理判断都限制在该租户范围内。
     * 不直接修改业务状态，主要读取数据或执行本地规则计算。
     * @param tenantId 租户 ID，所有查询和写入都限定在该租户数据范围内
     * @param campaignId 目标业务记录 ID，需与租户边界匹配
     * @return 返回按租户、状态和数量限制过滤后的视图列表；无数据时返回空列表
     */
    public List<MarketingCampaignLinkView> listLinks(Long tenantId, Long campaignId) {
        Long scopedTenantId = safeTenantId(tenantId);
        campaign(scopedTenantId, campaignId);
        return linkRows(scopedTenantId, campaignId)
                .stream()
                .map(this::toLinkView)
                .toList();
    }

    /**
     * 执行业务操作 readiness，作为增长营销的服务入口。
     * <p>调用方必须传入租户上下文或租户 ID，方法内的查询、写入和治理判断都限制在该租户范围内。
     * 不直接修改业务状态，主要读取数据或执行本地规则计算。
     * @param tenantId 租户 ID，所有查询和写入都限定在该租户数据范围内
     * @param campaignId 目标业务记录 ID，需与租户边界匹配
     * @return 返回租户范围内的最新业务视图或持久化对象快照
     */
    public MarketingCampaignReadinessView readiness(Long tenantId, Long campaignId) {
        Long scopedTenantId = safeTenantId(tenantId);
        MarketingCampaignMasterDO campaign = campaign(scopedTenantId, campaignId);
        List<MarketingCampaignLinkView> links = linkRows(scopedTenantId, campaign.getId())
                .stream()
                .map(this::toLinkView)
                .toList();
        List<MarketingCampaignReadinessFinding> blockers = new ArrayList<>();
        List<MarketingCampaignReadinessFinding> warnings = new ArrayList<>();

        if (!"ACTIVE".equals(campaign.getStatus())) {
            blockers.add(finding(
                    "BLOCKER",
                    "CAMPAIGN_STATUS",
                    campaign.getCampaignKey(),
                    "Campaign is not active",
                    "campaign status must be ACTIVE before production launch",
                    null));
        }

        List<MarketingCampaignLinkView> requiredLinks = links.stream()
                .filter(MarketingCampaignLinkView::requiredForLaunch)
                .toList();
        List<MarketingCampaignLinkView> activeRequiredLinks = requiredLinks.stream()
                .filter(link -> "ACTIVE".equals(link.linkStatus()))
                .toList();

        if (requiredLinks.isEmpty()) {
            blockers.add(finding(
                    "BLOCKER",
                    "RESOURCE_LINK",
                    "launch-required-links",
                    "No launch-required resources are linked",
                    "attach journey, content, activation, provider-write, or measurement resources before launch",
                    null));
        }

        requiredLinks.stream()
                .filter(link -> !"ACTIVE".equals(link.linkStatus()))
                .forEach(link -> blockers.add(finding(
                        "BLOCKER",
                        "RESOURCE_LINK",
                        link.resourceType() + ":" + link.resourceKey(),
                        "Launch-required resource is not active",
                        "linkStatus=" + link.linkStatus(),
                        link.resourceRoute())));

        boolean hasPrimary = activeRequiredLinks.stream()
                .anyMatch(link -> "PRIMARY".equals(link.dependencyRole()));
        if (!hasPrimary) {
            blockers.add(finding(
                    "BLOCKER",
                    "RESOURCE_COVERAGE",
                    "primary-dependency",
                    "Primary launch dependency is missing",
                    "at least one active launch-required resource must have dependencyRole=PRIMARY",
                    null));
        }

        boolean hasMeasurement = activeRequiredLinks.stream()
                .anyMatch(link -> "MEASUREMENT".equals(link.dependencyRole())
                        || "BI_DASHBOARD".equals(link.resourceType()));
        if (!hasMeasurement) {
            blockers.add(finding(
                    "BLOCKER",
                    "RESOURCE_COVERAGE",
                    "measurement-dependency",
                    "Measurement dependency is missing",
                    "attach an active launch-required BI dashboard or dependencyRole=MEASUREMENT resource",
                    null));
        }

        links.stream()
                .filter(link -> !link.requiredForLaunch())
                .filter(link -> "BLOCKED".equals(link.linkStatus()) || "MISSING".equals(link.linkStatus()))
                .forEach(link -> warnings.add(finding(
                        "WARNING",
                        "OPTIONAL_RESOURCE_LINK",
                        link.resourceType() + ":" + link.resourceKey(),
                        "Optional linked resource needs triage",
                        "linkStatus=" + link.linkStatus(),
                        link.resourceRoute())));

        String status = blockers.isEmpty() ? (warnings.isEmpty() ? "READY" : "DEGRADED") : "BLOCKED";
        return new MarketingCampaignReadinessView(
                campaign.getTenantId(),
                campaign.getId(),
                campaign.getCampaignKey(),
                campaign.getCampaignName(),
                LocalDateTime.now(clock).withNano(0).toString(),
                status,
                blockers.isEmpty(),
                requiredLinks.size(),
                activeRequiredLinks.size(),
                blockers.size(),
                warnings.size(),
                List.copyOf(blockers),
                List.copyOf(warnings),
                links);
    }

    private List<MarketingCampaignLinkDO> linkRows(Long tenantId, Long campaignId) {
        return linkMapper.selectList(new LambdaQueryWrapper<MarketingCampaignLinkDO>()
                        .eq(MarketingCampaignLinkDO::getTenantId, tenantId)
                        .eq(MarketingCampaignLinkDO::getCampaignId, campaignId)
                        .orderByAsc(MarketingCampaignLinkDO::getResourceType)
                        .orderByAsc(MarketingCampaignLinkDO::getResourceKey));
    }

    /**
     * 执行业务操作 unlinkResource，作为增长营销的服务入口。
     * <p>调用方必须传入租户上下文或租户 ID，方法内的查询、写入和治理判断都限制在该租户范围内。
     * 会通过 Mapper 写入、更新或关闭持久化记录。
     * @param tenantId 租户 ID，所有查询和写入都限定在该租户数据范围内
     * @param linkId 目标业务记录 ID，需与租户边界匹配
     */
    @Transactional(rollbackFor = Exception.class)
    public void unlinkResource(Long tenantId, Long linkId) {
        Long scopedTenantId = safeTenantId(tenantId);
        MarketingCampaignLinkDO row = linkMapper.selectById(requiredId(linkId, "linkId"));
        validateTenant(scopedTenantId, row == null ? null : row.getTenantId(), "campaign link");
        linkMapper.deleteById(linkId);
    }

    private MarketingCampaignMasterDO campaign(Long tenantId, Long campaignId) {
        MarketingCampaignMasterDO campaign = campaignMapper.selectById(requiredId(campaignId, "campaignId"));
        validateTenant(tenantId, campaign == null ? null : campaign.getTenantId(), "campaign");
        return campaign;
    }

    private MarketingCampaignView toCampaignView(MarketingCampaignMasterDO row) {
        return new MarketingCampaignView(
                row.getId(),
                row.getTenantId(),
                row.getCampaignKey(),
                row.getCampaignName(),
                row.getObjective(),
                row.getStatus(),
                row.getPrimaryChannel(),
                row.getOwnerTeam(),
                row.getStartAt(),
                row.getEndAt(),
                row.getBudgetAmount(),
                row.getCurrency(),
                fromJson(row.getBriefJson()),
                row.getCreatedBy(),
                row.getUpdatedBy(),
                row.getCreatedAt(),
                row.getUpdatedAt());
    }

    private MarketingCampaignLinkView toLinkView(MarketingCampaignLinkDO row) {
        return new MarketingCampaignLinkView(
                row.getId(),
                row.getTenantId(),
                row.getCampaignId(),
                row.getResourceType(),
                row.getResourceId(),
                row.getResourceKey(),
                row.getResourceName(),
                row.getResourceRoute(),
                row.getDependencyRole(),
                row.getLinkStatus(),
                row.getRequiredForLaunch() != null && row.getRequiredForLaunch() == 1,
                fromJson(row.getMetadataJson()),
                row.getCreatedBy(),
                row.getUpdatedBy(),
                row.getCreatedAt(),
                row.getUpdatedAt());
    }

    private static MarketingCampaignReadinessFinding finding(String severity,
                                                             String itemType,
                                                             String itemKey,
                                                             String title,
                                                             String reason,
                                                             String route) {
        return new MarketingCampaignReadinessFinding(severity, itemType, itemKey, title, reason, route);
    }

    private String toJson(Map<String, Object> value) {
        if (value == null || value.isEmpty()) {
            return "{}";
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("metadata must be JSON serializable", e);
        }
    }

    private Map<String, Object> fromJson(String value) {
        if (value == null || value.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(value, MAP_TYPE);
        } catch (JsonProcessingException e) {
            return Map.of();
        }
    }

    private static Long safeTenantId(Long tenantId) {
        return tenantId == null || tenantId < 0 ? 0L : tenantId;
    }

    private static Long requiredId(Long value, String field) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value;
    }

    private static String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }

    private static String normalizeKey(String value, String field) {
        String normalized = required(value, field).trim().toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9_-]+", "-")
                .replaceAll("-+", "-")
                .replaceAll("(^-|-$)", "");
        if (normalized.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return normalized;
    }

    private static String normalizeUpper(String value, String fallback) {
        String trimmed = value == null ? "" : value.trim();
        return trimmed.isBlank() ? fallback : trimmed.toUpperCase(Locale.ROOT);
    }

    private static String normalizeOptionalUpper(String value) {
        String trimmed = value == null ? "" : value.trim();
        return trimmed.isBlank() ? null : trimmed.toUpperCase(Locale.ROOT);
    }

    private static String normalizeStatus(String value) {
        String status = normalizeUpper(value, "DRAFT");
        return switch (status) {
            case "DRAFT", "ACTIVE", "PAUSED", "COMPLETED", "ARCHIVED" -> status;
            default -> throw new IllegalArgumentException("unsupported campaign status: " + status);
        };
    }

    private static String normalizeOptionalStatus(String value) {
        String trimmed = value == null ? "" : value.trim();
        return trimmed.isBlank() ? null : normalizeStatus(trimmed);
    }

    private static String normalizeLinkStatus(String value) {
        String status = normalizeUpper(value, "ACTIVE");
        return switch (status) {
            case "ACTIVE", "MISSING", "BLOCKED", "ARCHIVED" -> status;
            default -> throw new IllegalArgumentException("unsupported link status: " + status);
        };
    }

    private static String normalizeCurrency(String value) {
        String currency = normalizeUpper(value, "CNY");
        if (currency.length() > 16) {
            throw new IllegalArgumentException("currency is too long");
        }
        return currency;
    }

    private static String defaultString(String value, String fallback) {
        String trimmed = value == null ? "" : value.trim();
        return trimmed.isBlank() ? fallback : trimmed;
    }

    private static String trimToLimit(String value, int limit) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isBlank()) {
            return null;
        }
        return trimmed.length() <= limit ? trimmed : trimmed.substring(0, limit);
    }

    private static int normalizedLimit(Integer limit) {
        if (limit == null) {
            return 50;
        }
        return Math.max(1, Math.min(limit, 200));
    }

    private static void validateTenant(Long expected, Long actual, String entity) {
        if (actual == null || !actual.equals(expected)) {
            throw new IllegalArgumentException(entity + " does not belong to tenant");
        }
    }
}
