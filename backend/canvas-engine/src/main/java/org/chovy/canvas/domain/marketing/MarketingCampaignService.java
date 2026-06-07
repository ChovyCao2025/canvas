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

    public List<MarketingCampaignLinkView> listLinks(Long tenantId, Long campaignId) {
        Long scopedTenantId = safeTenantId(tenantId);
        campaign(scopedTenantId, campaignId);
        return linkRows(scopedTenantId, campaignId)
                .stream()
                .map(this::toLinkView)
                .toList();
    }

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
