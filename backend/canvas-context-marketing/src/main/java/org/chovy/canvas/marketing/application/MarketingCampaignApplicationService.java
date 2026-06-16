package org.chovy.canvas.marketing.application;

import org.chovy.canvas.marketing.api.MarketingCampaignCommand;
import org.chovy.canvas.marketing.api.MarketingCampaignFacade;
import org.chovy.canvas.marketing.api.MarketingCampaignLinkCommand;
import org.chovy.canvas.marketing.api.MarketingCampaignLinkView;
import org.chovy.canvas.marketing.api.MarketingCampaignReadinessFinding;
import org.chovy.canvas.marketing.api.MarketingCampaignReadinessView;
import org.chovy.canvas.marketing.api.MarketingCampaignView;
import org.chovy.canvas.marketing.domain.CampaignBudget;
import org.chovy.canvas.marketing.domain.CampaignDateRange;
import org.chovy.canvas.marketing.domain.CampaignKey;
import org.chovy.canvas.marketing.domain.CampaignLinkStatus;
import org.chovy.canvas.marketing.domain.CampaignStatus;
import org.chovy.canvas.marketing.domain.MarketingCampaign;
import org.chovy.canvas.marketing.domain.MarketingCampaignLink;
import org.chovy.canvas.marketing.domain.MarketingCampaignReadinessIssue;
import org.chovy.canvas.marketing.domain.MarketingCampaignReadinessPolicy;
import org.chovy.canvas.marketing.domain.MarketingCampaignReadinessReport;
import org.chovy.canvas.marketing.domain.MarketingCampaignRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 编排MarketingCampaign相关的应用层用例。
 */
@Service
public class MarketingCampaignApplicationService implements MarketingCampaignFacade {

    /**
     * 用于读写营销活动聚合的仓储。
     */
    private final MarketingCampaignRepository repository;

    /**
     * 用于评估活动上线准备度的领域策略。
     */
    private final MarketingCampaignReadinessPolicy readinessPolicy;

    /**
     * 用于生成确定性业务时间的时钟。
     */
    private final Clock clock;

    /**
     * 创建MarketingCampaignApplicationService实例。
     */
    @Autowired
    public MarketingCampaignApplicationService(MarketingCampaignRepository repository) {
        this(repository, Clock.systemDefaultZone());
    }

    MarketingCampaignApplicationService(MarketingCampaignRepository repository, Clock clock) {
        this.repository = repository;
        this.readinessPolicy = new MarketingCampaignReadinessPolicy();
        this.clock = clock == null ? Clock.systemDefaultZone() : clock;
    }

    /**
     * 执行upsertCampaign业务操作。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public MarketingCampaignView upsertCampaign(Long tenantId, MarketingCampaignCommand command, String actor) {
        if (command == null) {
            throw new IllegalArgumentException("campaign command is required");
        }
        Long scopedTenantId = safeTenantId(tenantId);
        CampaignKey campaignKey = CampaignKey.of(command.campaignKey(), "campaignKey");
        MarketingCampaign existing = repository.findByTenantAndKey(scopedTenantId, campaignKey);
        MarketingCampaign campaign = new MarketingCampaign(
                existing == null ? null : existing.id(),
                scopedTenantId,
                campaignKey,
                defaultString(command.campaignName(), campaignKey.value()),
                normalizeUpper(command.objective(), "UNSPECIFIED"),
                CampaignStatus.from(command.status()),
                normalizeOptionalUpper(command.primaryChannel()),
                trimToLimit(command.ownerTeam(), 128),
                CampaignDateRange.of(command.startAt(), command.endAt()),
                CampaignBudget.of(command.budgetAmount(), command.currency()),
                safeMap(command.brief()),
                existing == null ? defaultString(actor, "system") : existing.createdBy(),
                defaultString(actor, "system"),
                existing == null ? null : existing.createdAt(),
                existing == null ? null : existing.updatedAt());
        return toCampaignView(repository.save(campaign));
    }

    /**
     * 查询campaigns列表。
     */
    @Override
    public List<MarketingCampaignView> listCampaigns(Long tenantId, String status, Integer limit) {
        Long scopedTenantId = safeTenantId(tenantId);
        CampaignStatus normalizedStatus = CampaignStatus.optional(status);
        return repository.list(scopedTenantId, normalizedStatus, normalizedLimit(limit)).stream()
                .map(this::toCampaignView)
                .toList();
    }

    /**
     * 执行linkResource业务操作。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public MarketingCampaignLinkView linkResource(Long tenantId, MarketingCampaignLinkCommand command, String actor) {
        if (command == null) {
            throw new IllegalArgumentException("campaign link command is required");
        }
        Long scopedTenantId = safeTenantId(tenantId);
        MarketingCampaign campaign = campaign(scopedTenantId, command.campaignId());
        String resourceType = normalizeUpper(required(command.resourceType(), "resourceType"), "RESOURCE");
        CampaignKey resourceKey = CampaignKey.of(command.resourceKey(), "resourceKey");
        MarketingCampaignLink existing = repository.findLink(
                scopedTenantId,
                campaign.id(),
                resourceType,
                resourceKey);
        MarketingCampaignLink link = new MarketingCampaignLink(
                existing == null ? null : existing.id(),
                scopedTenantId,
                campaign.id(),
                resourceType,
                command.resourceId(),
                resourceKey,
                trimToLimit(command.resourceName(), 255),
                trimToLimit(command.resourceRoute(), 512),
                normalizeUpper(command.dependencyRole(), "SUPPORTING"),
                CampaignLinkStatus.from(command.linkStatus()),
                Boolean.TRUE.equals(command.requiredForLaunch()),
                safeMap(command.metadata()),
                existing == null ? defaultString(actor, "system") : existing.createdBy(),
                defaultString(actor, "system"),
                existing == null ? null : existing.createdAt(),
                existing == null ? null : existing.updatedAt());
        return toLinkView(repository.saveLink(link));
    }

    /**
     * 查询links列表。
     */
    @Override
    public List<MarketingCampaignLinkView> listLinks(Long tenantId, Long campaignId) {
        Long scopedTenantId = safeTenantId(tenantId);
        MarketingCampaign campaign = campaign(scopedTenantId, campaignId);
        return repository.listLinks(scopedTenantId, campaign.id()).stream()
                .map(this::toLinkView)
                .toList();
    }

    /**
     * 执行readiness业务操作。
     */
    @Override
    public MarketingCampaignReadinessView readiness(Long tenantId, Long campaignId) {
        Long scopedTenantId = safeTenantId(tenantId);
        MarketingCampaign campaign = campaign(scopedTenantId, campaignId);
        MarketingCampaignReadinessReport report = readinessPolicy.evaluate(
                campaign,
                repository.listLinks(scopedTenantId, campaign.id()),
                clock);
        return toReadinessView(report);
    }

    /**
     * 执行unlinkResource业务操作。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unlinkResource(Long tenantId, Long linkId) {
        Long scopedTenantId = safeTenantId(tenantId);
        Long requiredLinkId = requiredId(linkId, "linkId");
        MarketingCampaignLink link = repository.findLinkById(scopedTenantId, requiredLinkId);
        if (link == null) {
            throw new IllegalArgumentException("campaign link does not belong to tenant");
        }
        repository.deleteLink(scopedTenantId, requiredLinkId);
    }

    /**
     * 执行campaign业务操作。
     */
    private MarketingCampaign campaign(Long tenantId, Long campaignId) {
        MarketingCampaign campaign = repository.findById(tenantId, requiredId(campaignId, "campaignId"));
        if (campaign == null) {
            throw new IllegalArgumentException("campaign does not belong to tenant");
        }
        return campaign;
    }

    /**
     * 转换为campaignView对象。
     */
    private MarketingCampaignView toCampaignView(MarketingCampaign campaign) {
        return new MarketingCampaignView(
                campaign.id(),
                campaign.tenantId(),
                campaign.campaignKey().value(),
                campaign.campaignName(),
                campaign.objective(),
                campaign.status().name(),
                campaign.primaryChannel(),
                campaign.ownerTeam(),
                campaign.dateRange().startAt(),
                campaign.dateRange().endAt(),
                campaign.budget().amount(),
                campaign.budget().currency(),
                campaign.brief(),
                campaign.createdBy(),
                campaign.updatedBy(),
                campaign.createdAt(),
                campaign.updatedAt());
    }

    /**
     * 转换为linkView对象。
     */
    private MarketingCampaignLinkView toLinkView(MarketingCampaignLink link) {
        return new MarketingCampaignLinkView(
                link.id(),
                link.tenantId(),
                link.campaignId(),
                link.resourceType(),
                link.resourceId(),
                link.resourceKey().value(),
                link.resourceName(),
                link.resourceRoute(),
                link.dependencyRole(),
                link.linkStatus().name(),
                link.requiredForLaunch(),
                link.metadata(),
                link.createdBy(),
                link.updatedBy(),
                link.createdAt(),
                link.updatedAt());
    }

    /**
     * 转换为readinessView对象。
     */
    private MarketingCampaignReadinessView toReadinessView(MarketingCampaignReadinessReport report) {
        return new MarketingCampaignReadinessView(
                report.tenantId(),
                report.campaignId(),
                report.campaignKey(),
                report.campaignName(),
                report.generatedAt(),
                report.status(),
                report.productionReady(),
                report.requiredLinkCount(),
                report.activeRequiredLinkCount(),
                report.blockerCount(),
                report.warningCount(),
                report.blockers().stream().map(this::toFinding).toList(),
                report.warnings().stream().map(this::toFinding).toList(),
                report.links().stream().map(this::toLinkView).toList());
    }

    /**
     * 转换为finding对象。
     */
    private MarketingCampaignReadinessFinding toFinding(MarketingCampaignReadinessIssue issue) {
        return new MarketingCampaignReadinessFinding(
                issue.severity(),
                issue.itemType(),
                issue.itemKey(),
                issue.title(),
                issue.reason(),
                issue.route());
    }

    /**
     * 执行safeTenantId业务操作。
     */
    private static Long safeTenantId(Long tenantId) {
        return tenantId == null || tenantId < 0 ? 0L : tenantId;
    }

    /**
     * 校验并返回dId必填值。
     */
    private static Long requiredId(Long value, String field) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value;
    }

    /**
     * 校验并返回d必填值。
     */
    private static String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }

    /**
     * 规范化upper输入值。
     */
    private static String normalizeUpper(String value, String fallback) {
        String trimmed = value == null ? "" : value.trim();
        return trimmed.isBlank() ? fallback : trimmed.toUpperCase(Locale.ROOT);
    }

    /**
     * 规范化optionalUpper输入值。
     */
    private static String normalizeOptionalUpper(String value) {
        String trimmed = value == null ? "" : value.trim();
        return trimmed.isBlank() ? null : trimmed.toUpperCase(Locale.ROOT);
    }

    /**
     * 执行defaultString业务操作。
     */
    private static String defaultString(String value, String fallback) {
        String trimmed = value == null ? "" : value.trim();
        return trimmed.isBlank() ? fallback : trimmed;
    }

    /**
     * 执行trimToLimit业务操作。
     */
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

    /**
     * 规范化dLimit输入值。
     */
    private static int normalizedLimit(Integer limit) {
        if (limit == null) {
            return 50;
        }
        return Math.max(1, Math.min(limit, 200));
    }

    /**
     * 执行safeMap业务操作。
     */
    private static Map<String, Object> safeMap(Map<String, Object> value) {
        if (value == null || value.isEmpty()) {
            return Map.of();
        }
        return Collections.unmodifiableMap(new LinkedHashMap<>(value));
    }
}
