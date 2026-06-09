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

/**
 * MarketingCampaignService 编排 domain.marketing 场景的领域业务规则。
 */
@Service
public class MarketingCampaignService {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

    private final MarketingCampaignMasterMapper campaignMapper;
    private final MarketingCampaignLinkMapper linkMapper;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    /**
     * 创建 MarketingCampaignService 实例并注入 domain.marketing 场景依赖。
     * @param campaignMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param linkMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param objectMapper 依赖组件，用于完成数据访问或外部能力调用。
     */
    @Autowired
    public MarketingCampaignService(MarketingCampaignMasterMapper campaignMapper,
                                    MarketingCampaignLinkMapper linkMapper,
                                    ObjectMapper objectMapper) {
        this(campaignMapper, linkMapper, objectMapper, Clock.systemDefaultZone());
    }

    /**
     * 执行 MarketingCampaignService 流程，围绕 marketing campaign service 完成校验、计算或结果组装。
     *
     * @param campaignMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param linkMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param objectMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param clock 时间参数，用于计算窗口、过期或审计时间。
     */
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
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (command == null) {
            throw new IllegalArgumentException("campaign command is required");
        }
        Long scopedTenantId = safeTenantId(tenantId);
        String campaignKey = normalizeKey(command.campaignKey(), "campaignKey");
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
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
        // 汇总前面计算出的状态和明细，返回给调用方。
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
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        return campaignMapper.selectList(new LambdaQueryWrapper<MarketingCampaignMasterDO>()
                        .eq(MarketingCampaignMasterDO::getTenantId, scopedTenantId)
                        .eq(normalizedStatus != null, MarketingCampaignMasterDO::getStatus, normalizedStatus)
                        .orderByDesc(MarketingCampaignMasterDO::getUpdatedAt)
                        .last("LIMIT " + normalizedLimit(limit)))
                // 遍历候选数据并按业务规则筛选、转换或聚合。
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
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (command == null) {
            throw new IllegalArgumentException("campaign link command is required");
        }
        Long scopedTenantId = safeTenantId(tenantId);
        MarketingCampaignMasterDO campaign = campaign(scopedTenantId, command.campaignId());
        String resourceType = normalizeUpper(required(command.resourceType(), "resourceType"), "RESOURCE");
        String resourceKey = normalizeKey(command.resourceKey(), "resourceKey");
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
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
        // 汇总前面计算出的状态和明细，返回给调用方。
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
                // 遍历候选数据并按业务规则筛选、转换或聚合。
                .stream()
                .map(this::toLinkView)
                .toList();
        List<MarketingCampaignReadinessFinding> blockers = new ArrayList<>();
        List<MarketingCampaignReadinessFinding> warnings = new ArrayList<>();

        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
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
        // 汇总前面计算出的状态和明细，返回给调用方。
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

    /**
     * 执行 linkRows 流程，围绕 link rows 完成校验、计算或结果组装。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param campaignId 业务对象 ID，用于定位具体记录。
     * @return 返回 link rows 汇总后的集合、分页或映射视图。
     */
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

    /**
     * 执行 campaign 流程，围绕 campaign 完成校验、计算或结果组装。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param campaignId 业务对象 ID，用于定位具体记录。
     * @return 返回 campaign 流程生成的业务结果。
     */
    private MarketingCampaignMasterDO campaign(Long tenantId, Long campaignId) {
        MarketingCampaignMasterDO campaign = campaignMapper.selectById(requiredId(campaignId, "campaignId"));
        validateTenant(tenantId, campaign == null ? null : campaign.getTenantId(), "campaign");
        return campaign;
    }

    /**
     * 转换为接口返回或领域视图。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @return 返回组装或转换后的结果对象。
     */
    private MarketingCampaignView toCampaignView(MarketingCampaignMasterDO row) {
        // 汇总前面计算出的状态和明细，返回给调用方。
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
                // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
                row.getUpdatedBy(),
                row.getCreatedAt(),
                row.getUpdatedAt());
    }

    /**
     * 转换为接口返回或领域视图。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @return 返回组装或转换后的结果对象。
     */
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

    /**
     * 查询或读取业务数据。
     *
     * @param severity severity 参数，用于 finding 流程中的校验、计算或对象转换。
     * @param itemType 类型标识，用于选择对应处理分支。
     * @param itemKey 业务键，用于在同一租户下定位资源。
     * @param title title 参数，用于 finding 流程中的校验、计算或对象转换。
     * @param reason 原因说明，用于记录状态变化的业务依据。
     * @param route route 参数，用于 finding 流程中的校验、计算或对象转换。
     * @return 返回符合条件的数据列表或视图。
     */
    private static MarketingCampaignReadinessFinding finding(String severity,
                                                             String itemType,
                                                             String itemKey,
                                                             String title,
                                                             String reason,
                                                             String route) {
        return new MarketingCampaignReadinessFinding(severity, itemType, itemKey, title, reason, route);
    }

    /**
     * 转换为接口返回或领域视图。
     *
     * @param String string 参数，用于 toJson 流程中的校验、计算或对象转换。
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回组装或转换后的结果对象。
     */
    private String toJson(Map<String, Object> value) {
        if (value == null || value.isEmpty()) {
            return "{}";
        }
        try {
            return objectMapper.writeValueAsString(value);
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("metadata must be JSON serializable", e);
        }
    }

    /**
     * 处理 JSON 序列化或反序列化。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回组装或转换后的结果对象。
     */
    private Map<String, Object> fromJson(String value) {
        if (value == null || value.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(value, MAP_TYPE);
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (JsonProcessingException e) {
            return Map.of();
        }
    }

    /**
     * 解析并规范化租户 ID。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @return 返回 safe tenant id 计算得到的数量、金额或指标值。
     */
    private static Long safeTenantId(Long tenantId) {
        return tenantId == null || tenantId < 0 ? 0L : tenantId;
    }

    /**
     * 校验并获取必需参数、资源或权限。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param field 待处理业务值，用于规则计算、转换或外部调用。
     * @return 返回 required id 计算得到的数量、金额或指标值。
     */
    private static Long requiredId(Long value, String field) {
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
    private static String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }

    /**
     * 规范化输入值。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param field 待处理业务值，用于规则计算、转换或外部调用。
     * @return 返回解析、归一化或安全处理后的值。
     */
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

    /**
     * 规范化输入值。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param fallback fallback 参数，用于 normalizeUpper 流程中的校验、计算或对象转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private static String normalizeUpper(String value, String fallback) {
        String trimmed = value == null ? "" : value.trim();
        return trimmed.isBlank() ? fallback : trimmed.toUpperCase(Locale.ROOT);
    }

    /**
     * 规范化输入值。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private static String normalizeOptionalUpper(String value) {
        String trimmed = value == null ? "" : value.trim();
        return trimmed.isBlank() ? null : trimmed.toUpperCase(Locale.ROOT);
    }

    /**
     * 规范化输入值。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private static String normalizeStatus(String value) {
        String status = normalizeUpper(value, "DRAFT");
        return switch (status) {
            case "DRAFT", "ACTIVE", "PAUSED", "COMPLETED", "ARCHIVED" -> status;
            default -> throw new IllegalArgumentException("unsupported campaign status: " + status);
        };
    }

    /**
     * 规范化输入值。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private static String normalizeOptionalStatus(String value) {
        String trimmed = value == null ? "" : value.trim();
        return trimmed.isBlank() ? null : normalizeStatus(trimmed);
    }

    /**
     * 规范化输入值。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private static String normalizeLinkStatus(String value) {
        String status = normalizeUpper(value, "ACTIVE");
        return switch (status) {
            case "ACTIVE", "MISSING", "BLOCKED", "ARCHIVED" -> status;
            default -> throw new IllegalArgumentException("unsupported link status: " + status);
        };
    }

    /**
     * 规范化输入值。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private static String normalizeCurrency(String value) {
        String currency = normalizeUpper(value, "CNY");
        if (currency.length() > 16) {
            throw new IllegalArgumentException("currency is too long");
        }
        return currency;
    }

    /**
     * 按默认值规则处理输入值。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param fallback fallback 参数，用于 defaultString 流程中的校验、计算或对象转换。
     * @return 返回 default string 生成的文本或业务键。
     */
    private static String defaultString(String value, String fallback) {
        String trimmed = value == null ? "" : value.trim();
        return trimmed.isBlank() ? fallback : trimmed;
    }

    /**
     * 按安全边界裁剪或保护输入值。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param limit 分页或数量限制，避免一次处理过多数据。
     * @return 返回解析、归一化或安全处理后的值。
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
     * 规范化输入值。
     *
     * @param limit 分页或数量限制，避免一次处理过多数据。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private static int normalizedLimit(Integer limit) {
        if (limit == null) {
            return 50;
        }
        return Math.max(1, Math.min(limit, 200));
    }

    /**
     * 校验输入、权限或业务前置条件。
     *
     * @param expected 待处理业务值，用于规则计算、转换或外部调用。
     * @param actual actual 参数，用于 validateTenant 流程中的校验、计算或对象转换。
     * @param entity entity 参数，用于 validateTenant 流程中的校验、计算或对象转换。
     */
    private static void validateTenant(Long expected, Long actual, String entity) {
        if (actual == null || !actual.equals(expected)) {
            throw new IllegalArgumentException(entity + " does not belong to tenant");
        }
    }
}
