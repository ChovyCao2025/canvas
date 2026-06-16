package org.chovy.canvas.platform.application;

import org.chovy.canvas.platform.api.MarketingPlatformControlPlaneEvidenceProvider;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 汇总营销平台控制面能力、集成资产、就绪门禁和后续动作。
 */
@Service
public class MarketingPlatformControlPlaneService {

    /**
     * 表示能力或资产已经具备生产可用条件。
     */
    private static final String LIVE = "LIVE";

    /**
     * 表示能力已有 API 基础但仍缺少面向运营的完整工作台。
     */
    private static final String API_ONLY = "API_ONLY";

    /**
     * 表示能力仍缺少上线所需配置或证据。
     */
    private static final String CONFIGURATION_REQUIRED = "CONFIGURATION_REQUIRED";

    /**
     * 提供控制面就绪判断所需的运行时证据。
     */
    private final MarketingPlatformControlPlaneEvidenceProvider evidenceProvider;

    /**
     * 生成汇总快照时间时使用的时钟。
     */
    private final Clock clock;

    /**
     * 使用运行时证据提供者创建控制面服务。
     *
     * @param evidenceProvider 控制面运行时证据提供者
     */
    @Autowired
    public MarketingPlatformControlPlaneService(MarketingPlatformControlPlaneEvidenceProvider evidenceProvider) {
        this(evidenceProvider, Clock.systemDefaultZone());
    }

    /**
     * 使用指定时钟和空证据提供者创建控制面服务。
     *
     * @param clock 生成汇总时间时使用的时钟
     */
    MarketingPlatformControlPlaneService(Clock clock) {
        this(MarketingPlatformControlPlaneEvidenceProvider.empty(), clock);
    }

    /**
     * 使用指定证据提供者和时钟创建控制面服务。
     *
     * @param evidenceProvider 控制面运行时证据提供者
     * @param clock 生成汇总时间时使用的时钟
     */
    MarketingPlatformControlPlaneService(MarketingPlatformControlPlaneEvidenceProvider evidenceProvider,
                                         Clock clock) {
        // 空依赖降级为空证据提供者，保证控制面汇总在未接入真实数据源时仍可返回稳定结构。
        this.evidenceProvider = evidenceProvider == null
                ? MarketingPlatformControlPlaneEvidenceProvider.empty()
                : evidenceProvider;
        this.clock = clock == null ? Clock.systemDefaultZone() : clock;
    }

    /**
     * 汇总租户营销平台控制面的运行证据和就绪状态。
     *
     * <p>方法只读取 evidence provider，不写数据库或外部系统；返回能力卡片、集成通道、资产、就绪门禁和待办项，用于管理端
     * 判断哪些能力已可用、哪些仍需配置。
     *
     * @param tenantId 租户 ID，空值按 0 号默认租户统计
     * @return 控制面汇总快照，包含生成时间和各能力状态
     */
    public ControlPlaneSummary summary(Long tenantId) {
        // 准备本次处理所需的上下文和中间变量。
        Long scopedTenantId = tenantId == null ? 0L : tenantId;
        MarketingPlatformControlPlaneEvidenceProvider.RuntimeEvidence evidence =
                this.evidenceProvider.evidence(scopedTenantId);
        List<CapabilityCard> capabilities = capabilities(evidence == null
                ? MarketingPlatformControlPlaneEvidenceProvider.RuntimeEvidence.empty()
                : evidence);
        List<IntegrationLane> lanes = integrationLanes(capabilities);
        List<IntegrationAsset> assets = integrationAssets(evidence == null
                ? MarketingPlatformControlPlaneEvidenceProvider.RuntimeEvidence.empty()
                : evidence);
        List<ActionItem> actions = actionItems(capabilities);
        ReadinessGate gate = readinessGate(capabilities, lanes, assets);
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        long liveCount = capabilities.stream()
                .filter(capability -> LIVE.equals(capability.status()))
                .count();
        // 汇总前面计算出的状态和明细，返回给调用方。
        return new ControlPlaneSummary(
                scopedTenantId,
                LocalDateTime.now(clock).withNano(0).toString(),
                overallStatus(capabilities),
                capabilities.size(),
                Math.toIntExact(liveCount),
                actions.size(),
                capabilities,
                lanes,
                assets,
                gate,
                actions);
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param capabilities capabilities 参数，用于 overallStatus 流程中的校验、计算或对象转换。
     * @return 返回 overall status 生成的文本或业务键。
     */
    private static String overallStatus(List<CapabilityCard> capabilities) {
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        boolean needsConfiguration = capabilities.stream()
                .anyMatch(capability -> CONFIGURATION_REQUIRED.equals(capability.status()));
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (needsConfiguration) {
            return CONFIGURATION_REQUIRED;
        }
        boolean apiOnly = capabilities.stream()
                .anyMatch(capability -> API_ONLY.equals(capability.status()));
        // 汇总前面计算出的状态和明细，返回给调用方。
        return apiOnly ? API_ONLY : "READY";
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param evidence evidence 参数，用于 capabilities 流程中的校验、计算或对象转换。
     * @return 返回 capabilities 汇总后的集合、分页或映射视图。
     */
    private static List<CapabilityCard> capabilities(
            MarketingPlatformControlPlaneEvidenceProvider.RuntimeEvidence evidence) {
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        boolean journeyReady = evidence.publishedJourneyCount() > 0;
        boolean contentReady = evidence.activeContentReleaseCount() > 0;
        boolean monitoringReady = evidence.activeMonitoringSourceCount() > 0
                && evidence.enabledAlertChannelCount() > 0;
        boolean paidMediaReady = evidence.enabledPaidMediaDestinationCount() > 0;
        boolean credentialReady = evidence.activeProviderCredentialCount() > 0;
        boolean measurementReady = evidence.publishedBiDashboardCount() > 0;
        boolean campaignReady = evidence.activeCampaignMasterCount() > 0
                && evidence.requiredCampaignResourceLinkCount() > 0
                && campaignLaunchCoverageReady(evidence);
        boolean growthActivityReady = growthActivityReady(evidence);
        boolean integrationContractReady = integrationContractReady(evidence);
        // 汇总前面计算出的状态和明细，返回给调用方。
        return List.of(
                capability(
                        "campaign-master-ledger",
                        "Campaign Master Ledger",
                        "campaign",
                        readyStatus(campaignReady),
                        "/marketing-platform",
                        "/canvas/marketing-campaigns",
                        "api-backed",
                        List.of("cross-domain campaign master", "resource dependency links",
                                "launch-required linkage", "blocked link evidence"),
                        campaignGaps(evidence),
                        evidence(
                                signal("activeCampaignMasters", "Active campaigns",
                                        evidence.activeCampaignMasterCount()),
                                signal("campaignResourceLinks", "Resource links",
                                        evidence.campaignResourceLinkCount()),
                                signal("requiredCampaignResourceLinks", "Required links",
                                        evidence.requiredCampaignResourceLinkCount()),
                                signal("blockedCampaignResourceLinks", "Blocked links",
                                        evidence.blockedCampaignResourceLinkCount()),
                                absenceSignal("campaignsWithInactiveRequiredLinks", "Inactive required campaigns",
                                        evidence.campaignsWithInactiveRequiredLinks()),
                                absenceSignal("campaignsMissingPrimaryDependency", "Missing primary campaigns",
                                        evidence.campaignsMissingPrimaryDependency()),
                                absenceSignal("campaignsMissingMeasurementDependency", "Missing measurement campaigns",
                                        evidence.campaignsMissingMeasurementDependency()))),
                capability(
                        "growth-activity-center",
                        "Growth Activity Center",
                        "growth",
                        readyStatus(growthActivityReady),
                        "/growth-activities",
                        "/canvas/growth-activities",
                        "operator-facing",
                        List.of("activity lifecycle", "reward pool governance",
                                "readiness blockers", "grant and referral closure"),
                        growthActivityGaps(evidence),
                        evidence(
                                signal("activeGrowthActivities", "Active activities",
                                        evidence.activeGrowthActivityCount()),
                                signal("growthActivityRewardPools", "Reward pools",
                                        evidence.growthActivityRewardPoolCount()),
                                signal("readyGrowthActivities", "Ready activities",
                                        evidence.readyGrowthActivityCount()),
                                absenceSignal("blockedGrowthActivityReadiness", "Blocked readiness",
                                        evidence.blockedGrowthActivityReadinessCount()))),
                capability(
                        "integration-contract-registry",
                        "Integration Contract Registry",
                        "integration",
                        readyStatus(integrationContractReady),
                        "/marketing-platform",
                        "/canvas/marketing-integrations/contracts",
                        "operator-facing",
                        List.of("tenant-scoped API contracts", "provider family ownership",
                                "credential dependency declaration", "SLA and retry metadata"),
                        integrationContractGaps(evidence),
                        evidence(
                                signal("activeIntegrationContracts", "Active contracts",
                                        evidence.activeIntegrationContractCount()),
                                signal("productionIntegrationContracts", "Production contracts",
                                        evidence.productionIntegrationContractCount()),
                                signal("blockedIntegrationContracts", "Blocked contracts",
                                        evidence.blockedIntegrationContractCount()),
                                signal("degradedIntegrationContracts", "Degraded contracts",
                                        evidence.degradedIntegrationContractCount()),
                                signal("freshPassingProductionIntegrationProbes", "Fresh passing probes",
                                        evidence.freshPassingProductionIntegrationProbeCount()),
                                absenceSignal("freshFailingProductionIntegrationProbes", "Fresh failing probes",
                                        evidence.freshFailingProductionIntegrationProbeCount()),
                                absenceSignal("openIntegrationContractProbeAlerts", "Open probe alerts",
                                        evidence.openIntegrationContractProbeAlertCount()),
                                absenceSignal("openIntegrationContractSloAlerts", "Open SLO alerts",
                                        evidence.openIntegrationContractSloAlertCount()))),
                capability(
                        "journey-orchestration",
                        "Journey Orchestration",
                        "automation",
                        readyStatus(journeyReady),
                        "/canvas",
                        "/canvas",
                        "operator-facing",
                        List.of("tenant-scoped journeys", "rate and frequency controls", "auditable execution"),
                        gaps(journeyReady, "publish at least one production journey"),
                        evidence(signal("publishedJourneys", "Published journeys", evidence.publishedJourneyCount()))),
                capability(
                        "content-lifecycle",
                        "Content Lifecycle",
                        "content",
                        readyStatus(contentReady),
                        "/content-hub",
                        "/marketing/content",
                        "operator-facing",
                        List.of("versioned entries", "release gate", "runtime snapshots", "rollback audit"),
                        gaps(contentReady, "publish at least one active content release"),
                        evidence(signal("activeContentReleases", "Active releases", evidence.activeContentReleaseCount()))),
                capability(
                        "scrm-workspace",
                        "SCRM Workspace",
                        "private-domain",
                        LIVE,
                        "/conversations",
                        "/canvas/conversations",
                        "operator-facing",
                        List.of("inbox work items", "routing and SLA", "AI reply review", "timeline audit"),
                        List.of(),
                        evidence(signal("conversationWorkItems", "Work items", evidence.conversationWorkItemCount()))),
                capability(
                        "marketing-monitoring",
                        "Marketing Monitoring",
                        "monitoring",
                        readyStatus(monitoringReady),
                        "/marketing-monitoring",
                        "/canvas/marketing-monitoring",
                        "operator-facing",
                        List.of("signed webhook ingestion", "polling scheduler", "alert fanout", "OAuth wizard"),
                        gaps(monitoringReady, "configure at least one enabled monitoring source and alert channel"),
                        evidence(
                                signal("activeMonitoringSources", "Enabled sources", evidence.activeMonitoringSourceCount()),
                                signal("enabledAlertChannels", "Enabled alert channels", evidence.enabledAlertChannelCount()))),
                capability(
                        "paid-media-activation",
                        "Paid Media Activation",
                        "activation",
                        readyStatus(paidMediaReady),
                        "/audiences",
                        "/canvas/paid-media/audience-sync",
                        "api-backed",
                        List.of("hashed audience member audit", "consent validation", "destination ledger"),
                        gaps(paidMediaReady, "configure at least one enabled paid-media destination per tenant"),
                        evidence(signal("enabledPaidMediaDestinations", "Enabled destinations",
                                evidence.enabledPaidMediaDestinationCount()))),
                capability(
                        "search-marketing-governance",
                        "Search Marketing Governance",
                        "search",
                        API_ONLY,
                        "/search-marketing",
                        "/canvas/search-marketing",
                        "api-only",
                        List.of("keyword portfolio", "performance snapshots", "opportunity ledger", "dry-run-first mutations"),
                        List.of("operator workbench and live SEM write clients remain provider-specific"),
                        evidence(
                                signal("enabledSearchSources", "Enabled sources", evidence.enabledSearchSourceCount()),
                                signal("searchProviderMutations", "Provider mutations",
                                        evidence.searchProviderMutationCount()))),
                capability(
                        "creator-collaboration-governance",
                        "Creator Collaboration Governance",
                        "creator",
                        API_ONLY,
                        "/marketing-platform",
                        "/canvas/creator-collaboration",
                        "api-only",
                        List.of("creator registry", "collaboration ledger", "deliverable evidence", "governed mutations"),
                        List.of("operator workbench and live creator provider clients remain provider-specific"),
                        evidence(
                                signal("activeCreatorCampaigns", "Active campaigns",
                                        evidence.activeCreatorCampaignCount()),
                                signal("creatorProviderMutations", "Provider mutations",
                                        evidence.creatorProviderMutationCount()))),
                capability(
                        "programmatic-dsp-governance",
                        "Programmatic DSP Governance",
                        "programmatic",
                        API_ONLY,
                        "/marketing-platform",
                        "/canvas/programmatic-dsp",
                        "api-only",
                        List.of("DSP seats", "line items", "supply paths", "pacing summaries", "governed mutations"),
                        List.of("operator workbench and live DSP provider clients remain provider-specific"),
                        evidence(
                                signal("enabledProgrammaticDspSeats", "Enabled seats",
                                        evidence.enabledProgrammaticDspSeatCount()),
                                signal("programmaticDspProviderMutations", "Provider mutations",
                                        evidence.programmaticDspProviderMutationCount()))),
                capability(
                        "measurement-bi",
                        "Measurement And BI",
                        "measurement",
                        readyStatus(measurementReady),
                        "/bi",
                        "/bi",
                        "operator-facing",
                        List.of("multi-touch attribution", "ROI foundation", "BI presets", "query SLO evidence"),
                        gaps(measurementReady, "publish at least one BI dashboard for marketing measurement"),
                        evidence(signal("publishedBiDashboards", "Published dashboards",
                                evidence.publishedBiDashboardCount()))),
                capability(
                        "provider-credential-governance",
                        "Provider Credential Governance",
                        "integration",
                        readyStatus(credentialReady),
                        "/marketing-monitoring",
                        "/canvas/marketing-monitoring/provider-credentials",
                        "operator-facing",
                        List.of("encrypted credentials", "OAuth authorization", "refresh and revocation evidence"),
                        gaps(credentialReady, "complete at least one active provider credential onboarding"),
                        evidence(signal("activeProviderCredentials", "Active credentials",
                                evidence.activeProviderCredentialCount()))));
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param key 业务键，用于在同一租户下定位资源。
     * @param displayName 名称文本，用于展示或唯一性校验。
     * @param domain domain 参数，用于 capability 流程中的校验、计算或对象转换。
     * @param status 业务状态，用于筛选或推进状态流转。
     * @param route route 参数，用于 capability 流程中的校验、计算或对象转换。
     * @param apiRoot api root 参数，用于 capability 流程中的校验、计算或对象转换。
     * @param surface surface 参数，用于 capability 流程中的校验、计算或对象转换。
     * @param productionSignals production signals 参数，用于 capability 流程中的校验、计算或对象转换。
     * @param gaps gaps 参数，用于 capability 流程中的校验、计算或对象转换。
     * @param evidence evidence 参数，用于 capability 流程中的校验、计算或对象转换。
     * @return 返回 capability 流程生成的业务结果。
     */
    private static CapabilityCard capability(String key,
                                             String displayName,
                                             String domain,
                                             String status,
                                             String route,
                                             String apiRoot,
                                             String surface,
                                             List<String> productionSignals,
                                             List<String> gaps,
                                             List<EvidenceSignal> evidence) {
        return new CapabilityCard(key, displayName, domain, status, route, apiRoot, surface, productionSignals, gaps,
                evidence);
    }

    /**
     * 根据输入和依赖数据计算业务判断结果。
     *
     * @param ready ready 参数，用于 readyStatus 流程中的校验、计算或对象转换。
     * @return 返回 ready status 生成的文本或业务键。
     */
    private static String readyStatus(boolean ready) {
        return ready ? LIVE : CONFIGURATION_REQUIRED;
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param ready ready 参数，用于 gaps 流程中的校验、计算或对象转换。
     * @param gap gap 参数，用于 gaps 流程中的校验、计算或对象转换。
     * @return 返回 gaps 汇总后的集合、分页或映射视图。
     */
    private static List<String> gaps(boolean ready, String gap) {
        return ready ? List.of() : List.of(gap);
    }

    /**
     * 解析、归一化或保护输入值，生成安全可用的中间结果。
     *
     * @param signalKey 业务键，用于在同一租户下定位资源。
     * @param label label 参数，用于 signal 流程中的校验、计算或对象转换。
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private static EvidenceSignal signal(String signalKey, String label, long value) {
        return new EvidenceSignal(signalKey, label, value, value > 0 ? "PRESENT" : "MISSING");
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param signalKey 业务键，用于在同一租户下定位资源。
     * @param label label 参数，用于 absenceSignal 流程中的校验、计算或对象转换。
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 absenceSignal 流程生成的业务结果。
     */
    private static EvidenceSignal absenceSignal(String signalKey, String label, long value) {
        return new EvidenceSignal(signalKey, label, value, value == 0 ? "PRESENT" : "MISSING");
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param signals signals 参数，用于 evidence 流程中的校验、计算或对象转换。
     * @return 返回 evidence 汇总后的集合、分页或映射视图。
     */
    private static List<EvidenceSignal> evidence(EvidenceSignal... signals) {
        return List.of(signals);
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param capabilities capabilities 参数，用于 integrationLanes 流程中的校验、计算或对象转换。
     * @return 返回 integration lanes 汇总后的集合、分页或映射视图。
     */
    private static List<IntegrationLane> integrationLanes(List<CapabilityCard> capabilities) {
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        Map<String, String> capabilityStatuses = capabilities.stream()
                .collect(Collectors.toMap(CapabilityCard::capabilityKey, CapabilityCard::status));
        // 汇总前面计算出的状态和明细，返回给调用方。
        return List.of(
                lane(
                        "campaign-to-growth-activities",
                        "Campaign Master To Growth Activities",
                        "campaign-master-ledger",
                        "growth-activity-center",
                        laneStatus(capabilityStatuses, "campaign-master-ledger", "growth-activity-center"),
                        List.of("campaign-owned activity launch", "readiness gate", "reward cost closure")),
                lane(
                        "content-to-journey",
                        "Content Releases To Journey Runtime",
                        "content-lifecycle",
                        "journey-orchestration",
                        laneStatus(capabilityStatuses, "content-lifecycle", "journey-orchestration"),
                        List.of("immutable release snapshot", "rollback audit", "connected-content resolver")),
                lane(
                        "monitoring-to-duty-alerts",
                        "Monitoring To Duty Alerts",
                        "marketing-monitoring",
                        "provider-credential-governance",
                        laneStatus(capabilityStatuses, "marketing-monitoring", "provider-credential-governance"),
                        List.of("signed webhooks", "alert channel delivery log", "manual resend")),
                lane(
                        "audience-to-paid-media",
                        "CDP Audiences To Paid Media",
                        "journey-orchestration",
                        "paid-media-activation",
                        laneStatus(capabilityStatuses, "journey-orchestration", "paid-media-activation"),
                        List.of("consent gate", "hashed identifier export", "destination eligibility")),
                lane(
                        "contracts-to-provider-credentials",
                        "Integration Contracts To Provider Credentials",
                        "integration-contract-registry",
                        "provider-credential-governance",
                        laneStatus(capabilityStatuses, "integration-contract-registry",
                                "provider-credential-governance"),
                        List.of("credential dependency declaration", "environment split", "SLA/retry metadata")),
                lane(
                        "search-to-provider-write",
                        "Search Opportunities To Provider Write",
                        "search-marketing-governance",
                        "provider-credential-governance",
                        laneStatus(capabilityStatuses, "search-marketing-governance",
                                "provider-credential-governance"),
                        List.of("approval gate", "idempotency key", "dry-run evidence")),
                lane(
                        "creator-to-provider-write",
                        "Creator Deliverables To Provider Write",
                        "creator-collaboration-governance",
                        "provider-credential-governance",
                        laneStatus(capabilityStatuses, "creator-collaboration-governance",
                                "provider-credential-governance"),
                        List.of("approval gate", "provider payload hash", "execution ledger")),
                lane(
                        "dsp-to-provider-write",
                        "DSP Line Items To Provider Write",
                        "programmatic-dsp-governance",
                        "provider-credential-governance",
                        laneStatus(capabilityStatuses, "programmatic-dsp-governance",
                                "provider-credential-governance"),
                        List.of("approval gate", "dry-run-first execution", "mutation audit")));
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param capabilityStatuses capability statuses 参数，用于 laneStatus 流程中的校验、计算或对象转换。
     * @param sourceCapabilityKey 业务键，用于在同一租户下定位资源。
     * @param targetCapabilityKey 业务键，用于在同一租户下定位资源。
     * @return 返回 lane status 生成的文本或业务键。
     */
    private static String laneStatus(Map<String, String> capabilityStatuses,
                                     String sourceCapabilityKey,
                                     String targetCapabilityKey) {
        String sourceStatus = capabilityStatuses.getOrDefault(sourceCapabilityKey, CONFIGURATION_REQUIRED);
        String targetStatus = capabilityStatuses.getOrDefault(targetCapabilityKey, CONFIGURATION_REQUIRED);
        return LIVE.equals(sourceStatus) && LIVE.equals(targetStatus) ? "GOVERNED" : CONFIGURATION_REQUIRED;
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param evidence evidence 参数，用于 integrationAssets 流程中的校验、计算或对象转换。
     * @return 返回 integration assets 汇总后的集合、分页或映射视图。
     */
    private static List<IntegrationAsset> integrationAssets(
            MarketingPlatformControlPlaneEvidenceProvider.RuntimeEvidence evidence) {
        // 准备本次处理所需的上下文和中间变量。
        boolean credentialReady = evidence.activeProviderCredentialCount() > 0;
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        boolean contentJourneyReady = evidence.activeContentReleaseCount() > 0 && evidence.publishedJourneyCount() > 0;
        boolean monitoringReady = evidence.activeMonitoringSourceCount() > 0
                && evidence.enabledAlertChannelCount() > 0
                && credentialReady;
        boolean paidMediaReady = evidence.enabledPaidMediaDestinationCount() > 0 && credentialReady;
        // 汇总前面计算出的状态和明细，返回给调用方。
        return List.of(
                asset(
                        "growth-activity-center",
                        "Growth Activity Center",
                        "INTERNAL_GOVERNANCE",
                        "growth-activity-center",
                        "GROWTH",
                        readyStatus(growthActivityReady(evidence)),
                        "/canvas/growth-activities",
                        "reward provider contract when configured",
                        0,
                        evidence.blockedGrowthActivityReadinessCount(),
                        List.of("campaign link", "activity readiness", "reward pool budget",
                                "grant ledger closure"),
                        growthActivityGaps(evidence),
                        evidence(
                                signal("activeGrowthActivities", "Active activities",
                                        evidence.activeGrowthActivityCount()),
                                signal("growthActivityRewardPools", "Reward pools",
                                        evidence.growthActivityRewardPoolCount()),
                                signal("readyGrowthActivities", "Ready activities",
                                        evidence.readyGrowthActivityCount()),
                                absenceSignal("blockedGrowthActivityReadiness", "Blocked readiness",
                                        evidence.blockedGrowthActivityReadinessCount()))),
                asset(
                        "marketing-integration-contract-registry",
                        "Marketing Integration Contract Registry",
                        "CONTRACT_REGISTRY",
                        "integration-contract-registry",
                        "MULTI_PROVIDER",
                        readyStatus(integrationContractReady(evidence)),
                        "/canvas/marketing-integrations/contracts",
                        "declared per contract",
                        0,
                        evidence.blockedIntegrationContractCount(),
                        List.of("contract key uniqueness", "provider family ownership",
                                "credential dependency declaration", "SLA/retry/schema metadata"),
                        integrationContractGaps(evidence),
                        evidence(
                                signal("activeIntegrationContracts", "Active contracts",
                                        evidence.activeIntegrationContractCount()),
                                signal("productionIntegrationContracts", "Production contracts",
                                        evidence.productionIntegrationContractCount()),
                                signal("blockedIntegrationContracts", "Blocked contracts",
                                        evidence.blockedIntegrationContractCount()),
                                signal("degradedIntegrationContracts", "Degraded contracts",
                                        evidence.degradedIntegrationContractCount()),
                                signal("freshPassingProductionIntegrationProbes", "Fresh passing probes",
                                        evidence.freshPassingProductionIntegrationProbeCount()),
                                absenceSignal("freshFailingProductionIntegrationProbes", "Fresh failing probes",
                                        evidence.freshFailingProductionIntegrationProbeCount()),
                                absenceSignal("openIntegrationContractProbeAlerts", "Open probe alerts",
                                        evidence.openIntegrationContractProbeAlertCount()),
                                absenceSignal("openIntegrationContractSloAlerts", "Open SLO alerts",
                                        evidence.openIntegrationContractSloAlertCount()))),
                asset(
                        "campaign-master-resource-ledger",
                        "Campaign Master Resource Ledger",
                        "INTERNAL_GOVERNANCE",
                        "campaign-master-ledger",
                        "CAMPAIGN",
                        readyStatus(evidence.activeCampaignMasterCount() > 0
                                && evidence.requiredCampaignResourceLinkCount() > 0
                                && campaignLaunchCoverageReady(evidence)),
                        "/canvas/marketing-campaigns",
                        "none",
                        0,
                        evidence.blockedCampaignResourceLinkCount(),
                        List.of("tenant-scoped campaign key", "cross-domain resource link",
                                "launch-required dependencies", "blocked link triage"),
                        campaignGaps(evidence),
                        evidence(
                                signal("activeCampaignMasters", "Active campaigns",
                                        evidence.activeCampaignMasterCount()),
                                signal("campaignResourceLinks", "Resource links",
                                        evidence.campaignResourceLinkCount()),
                                signal("requiredCampaignResourceLinks", "Required links",
                                        evidence.requiredCampaignResourceLinkCount()),
                                signal("blockedCampaignResourceLinks", "Blocked links",
                                        evidence.blockedCampaignResourceLinkCount()),
                                absenceSignal("campaignsWithInactiveRequiredLinks", "Inactive required campaigns",
                                        evidence.campaignsWithInactiveRequiredLinks()),
                                absenceSignal("campaignsMissingPrimaryDependency", "Missing primary campaigns",
                                        evidence.campaignsMissingPrimaryDependency()),
                                absenceSignal("campaignsMissingMeasurementDependency", "Missing measurement campaigns",
                                        evidence.campaignsMissingMeasurementDependency()))),
                asset(
                        "content-release-runtime-resolver",
                        "Content Release Runtime Resolver",
                        "INTERNAL_RUNTIME",
                        "content-lifecycle",
                        "CONTENT_JOURNEY",
                        readyStatus(contentJourneyReady),
                        "/marketing/content",
                        "none",
                        0,
                        0,
                        List.of("immutable release checksum", "published journey version", "rollback audit"),
                        gaps(contentJourneyReady, "publish content releases and journeys for the same tenant"),
                        evidence(
                                signal("activeContentReleases", "Active releases",
                                        evidence.activeContentReleaseCount()),
                                signal("publishedJourneys", "Published journeys",
                                        evidence.publishedJourneyCount()))),
                asset(
                        "monitoring-provider-ingestion",
                        "Monitoring Provider Ingestion",
                        "INBOUND_API",
                        "marketing-monitoring",
                        "SOCIAL_MONITORING",
                        readyStatus(monitoringReady),
                        "/canvas/marketing-monitoring",
                        "active provider credential",
                        0,
                        0,
                        List.of("signed webhook ingestion", "scheduled polling", "alert fanout"),
                        gaps(monitoringReady, "enable monitoring source, alert channel, and provider credential"),
                        evidence(
                                signal("activeMonitoringSources", "Enabled sources",
                                        evidence.activeMonitoringSourceCount()),
                                signal("enabledAlertChannels", "Enabled alert channels",
                                        evidence.enabledAlertChannelCount()),
                                signal("activeProviderCredentials", "Active credentials",
                                        evidence.activeProviderCredentialCount()))),
                asset(
                        "paid-media-audience-sync",
                        "Paid Media Audience Sync",
                        "OUTBOUND_SYNC",
                        "paid-media-activation",
                        "PAID_MEDIA",
                        readyStatus(paidMediaReady),
                        "/canvas/paid-media/audience-sync",
                        "active provider credential",
                        0,
                        0,
                        List.of("consent gate", "hashed identifier export", "destination ledger"),
                        gaps(paidMediaReady, "enable paid-media destination and provider credential"),
                        evidence(
                                signal("enabledPaidMediaDestinations", "Enabled destinations",
                                        evidence.enabledPaidMediaDestinationCount()),
                                signal("activeProviderCredentials", "Active credentials",
                                        evidence.activeProviderCredentialCount()))),
                asset(
                        "search-provider-write-gateway",
                        "Search Provider Write Gateway",
                        "OUTBOUND_WRITE",
                        "search-marketing-governance",
                        "SEM",
                        providerWriteStatus(evidence.enabledSearchSourceCount(), credentialReady),
                        "/canvas/search-marketing/mutations",
                        "active provider credential",
                        evidence.searchPendingWriteCount(),
                        evidence.searchFailedWriteCount(),
                        List.of("approval gate", "idempotency key", "dry-run evidence", "sanitized provider payload"),
                        providerWriteGaps(evidence.enabledSearchSourceCount(), credentialReady,
                                "wire live SEM write client"),
                        evidence(
                                signal("enabledSearchSources", "Enabled sources",
                                        evidence.enabledSearchSourceCount()),
                                signal("searchProviderMutations", "Provider mutations",
                                        evidence.searchProviderMutationCount()),
                                signal("searchPendingWrites", "Pending writes",
                                        evidence.searchPendingWriteCount()),
                                signal("searchFailedWrites", "Failed writes",
                                        evidence.searchFailedWriteCount()))),
                asset(
                        "creator-provider-write-gateway",
                        "Creator Provider Write Gateway",
                        "OUTBOUND_WRITE",
                        "creator-collaboration-governance",
                        "CREATOR",
                        providerWriteStatus(evidence.activeCreatorCampaignCount(), credentialReady),
                        "/canvas/creator-collaboration/mutations",
                        "active provider credential",
                        evidence.creatorPendingWriteCount(),
                        evidence.creatorFailedWriteCount(),
                        List.of("approval gate", "creator relationship validation", "dry-run evidence",
                                "provider payload hash"),
                        providerWriteGaps(evidence.activeCreatorCampaignCount(), credentialReady,
                                "wire live creator provider write client"),
                        evidence(
                                signal("activeCreatorCampaigns", "Active campaigns",
                                        evidence.activeCreatorCampaignCount()),
                                signal("creatorProviderMutations", "Provider mutations",
                                        evidence.creatorProviderMutationCount()),
                                signal("creatorPendingWrites", "Pending writes",
                                        evidence.creatorPendingWriteCount()),
                                signal("creatorFailedWrites", "Failed writes",
                                        evidence.creatorFailedWriteCount()))),
                asset(
                        "programmatic-dsp-provider-write-gateway",
                        "Programmatic DSP Provider Write Gateway",
                        "OUTBOUND_WRITE",
                        "programmatic-dsp-governance",
                        "PROGRAMMATIC_DSP",
                        providerWriteStatus(evidence.enabledProgrammaticDspSeatCount(), credentialReady),
                        "/canvas/programmatic-dsp/mutations",
                        "active provider credential",
                        evidence.programmaticDspPendingWriteCount(),
                        evidence.programmaticDspFailedWriteCount(),
                        List.of("approval gate", "line-item hierarchy validation", "dry-run-first execution",
                                "mutation audit"),
                        providerWriteGaps(evidence.enabledProgrammaticDspSeatCount(), credentialReady,
                                "wire live DSP provider write client"),
                        evidence(
                                signal("enabledProgrammaticDspSeats", "Enabled seats",
                                        evidence.enabledProgrammaticDspSeatCount()),
                                signal("programmaticDspProviderMutations", "Provider mutations",
                                        evidence.programmaticDspProviderMutationCount()),
                                signal("programmaticDspPendingWrites", "Pending writes",
                                        evidence.programmaticDspPendingWriteCount()),
                                signal("programmaticDspFailedWrites", "Failed writes",
                                        evidence.programmaticDspFailedWriteCount()))));
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param configuredSourceCount configured source count 参数，用于 providerWriteStatus 流程中的校验、计算或对象转换。
     * @param credentialReady credential ready 参数，用于 providerWriteStatus 流程中的校验、计算或对象转换。
     * @return 返回 provider write status 生成的文本或业务键。
     */
    private static String providerWriteStatus(long configuredSourceCount, boolean credentialReady) {
        return configuredSourceCount > 0 && credentialReady ? API_ONLY : CONFIGURATION_REQUIRED;
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param configuredSourceCount configured source count 参数，用于 providerWriteGaps 流程中的校验、计算或对象转换。
     * @param credentialReady credential ready 参数，用于 providerWriteGaps 流程中的校验、计算或对象转换。
     * @param liveClientGap 依赖组件，用于完成数据访问或外部能力调用。
     * @return 返回 provider write gaps 汇总后的集合、分页或映射视图。
     */
    private static List<String> providerWriteGaps(long configuredSourceCount, boolean credentialReady, String liveClientGap) {
        List<String> gaps = new ArrayList<>();
        if (configuredSourceCount <= 0) {
            gaps.add("configure provider source or seat");
        }
        if (!credentialReady) {
            gaps.add("complete provider credential onboarding");
        }
        gaps.add(liveClientGap);
        return gaps;
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param evidence evidence 参数，用于 campaignGaps 流程中的校验、计算或对象转换。
     * @return 返回 campaign gaps 汇总后的集合、分页或映射视图。
     */
    private static List<String> campaignGaps(MarketingPlatformControlPlaneEvidenceProvider.RuntimeEvidence evidence) {
        // 准备本次处理所需的上下文和中间变量。
        List<String> gaps = new ArrayList<>();
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (evidence.activeCampaignMasterCount() <= 0) {
            gaps.add("create at least one active campaign master record");
        }
        if (evidence.requiredCampaignResourceLinkCount() <= 0) {
            gaps.add("attach launch-required journey, content, activation, or measurement resources");
        }
        if (evidence.blockedCampaignResourceLinkCount() > 0) {
            gaps.add("resolve blocked campaign resource links");
        }
        if (evidence.campaignsWithInactiveRequiredLinks() > 0) {
            gaps.add("resolve launch-required campaign resources that are not active");
        }
        if (evidence.campaignsMissingPrimaryDependency() > 0) {
            gaps.add("attach an active PRIMARY launch dependency to every active campaign");
        }
        if (evidence.campaignsMissingMeasurementDependency() > 0) {
            gaps.add("attach an active MEASUREMENT or BI dashboard dependency to every active campaign");
        }
        // 汇总前面计算出的状态和明细，返回给调用方。
        return gaps;
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param evidence evidence 参数，用于 campaignLaunchCoverageReady 流程中的校验、计算或对象转换。
     * @return 返回 campaign launch coverage ready 的布尔判断结果。
     */
    private static boolean campaignLaunchCoverageReady(
            MarketingPlatformControlPlaneEvidenceProvider.RuntimeEvidence evidence) {
        return evidence.blockedCampaignResourceLinkCount() == 0
                && evidence.campaignsWithInactiveRequiredLinks() == 0
                && evidence.campaignsMissingPrimaryDependency() == 0
                && evidence.campaignsMissingMeasurementDependency() == 0;
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param evidence evidence 参数，用于 growthActivityReady 流程中的校验、计算或对象转换。
     * @return 返回 growth activity ready 的布尔判断结果。
     */
    private static boolean growthActivityReady(
            MarketingPlatformControlPlaneEvidenceProvider.RuntimeEvidence evidence) {
        return evidence.activeGrowthActivityCount() > 0
                && evidence.growthActivityRewardPoolCount() > 0
                && evidence.readyGrowthActivityCount() >= evidence.activeGrowthActivityCount()
                && evidence.blockedGrowthActivityReadinessCount() == 0;
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param evidence evidence 参数，用于 growthActivityGaps 流程中的校验、计算或对象转换。
     * @return 返回 growth activity gaps 汇总后的集合、分页或映射视图。
     */
    private static List<String> growthActivityGaps(
            MarketingPlatformControlPlaneEvidenceProvider.RuntimeEvidence evidence) {
        // 准备本次处理所需的上下文和中间变量。
        List<String> gaps = new ArrayList<>();
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (evidence.activeGrowthActivityCount() <= 0) {
            gaps.add("create at least one active growth activity");
        }
        if (evidence.growthActivityRewardPoolCount() <= 0) {
            gaps.add("configure reward pools for growth activities");
        }
        if (evidence.readyGrowthActivityCount() < evidence.activeGrowthActivityCount()) {
            gaps.add("complete readiness for every active growth activity");
        }
        if (evidence.blockedGrowthActivityReadinessCount() > 0) {
            gaps.add("resolve growth activity readiness blockers");
        }
        // 汇总前面计算出的状态和明细，返回给调用方。
        return gaps;
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param evidence evidence 参数，用于 integrationContractReady 流程中的校验、计算或对象转换。
     * @return 返回 integration contract ready 的布尔判断结果。
     */
    private static boolean integrationContractReady(
            MarketingPlatformControlPlaneEvidenceProvider.RuntimeEvidence evidence) {
        return evidence.productionIntegrationContractCount() > 0
                && evidence.blockedIntegrationContractCount() == 0
                && evidence.degradedIntegrationContractCount() == 0
                && evidence.freshPassingProductionIntegrationProbeCount()
                >= evidence.productionIntegrationContractCount()
                && evidence.freshFailingProductionIntegrationProbeCount() == 0
                && evidence.openIntegrationContractProbeAlertCount() == 0
                && evidence.openIntegrationContractSloAlertCount() == 0;
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param evidence evidence 参数，用于 integrationContractGaps 流程中的校验、计算或对象转换。
     * @return 返回 integration contract gaps 汇总后的集合、分页或映射视图。
     */
    private static List<String> integrationContractGaps(
            MarketingPlatformControlPlaneEvidenceProvider.RuntimeEvidence evidence) {
        // 准备本次处理所需的上下文和中间变量。
        List<String> gaps = new ArrayList<>();
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (evidence.productionIntegrationContractCount() <= 0) {
            gaps.add("register at least one active production integration contract");
        }
        if (evidence.blockedIntegrationContractCount() > 0) {
            gaps.add("resolve blocked integration contracts");
        }
        if (evidence.degradedIntegrationContractCount() > 0) {
            gaps.add("resolve degraded integration contracts");
        }
        if (evidence.productionIntegrationContractCount() > 0
                && evidence.freshPassingProductionIntegrationProbeCount()
                < evidence.productionIntegrationContractCount()) {
            gaps.add("record fresh PASS probes for every production integration contract");
        }
        if (evidence.freshFailingProductionIntegrationProbeCount() > 0) {
            gaps.add("resolve failing production integration probes");
        }
        if (evidence.openIntegrationContractProbeAlertCount() > 0) {
            gaps.add("resolve OPEN integration contract probe alerts");
        }
        if (evidence.openIntegrationContractSloAlertCount() > 0) {
            gaps.add("resolve OPEN integration contract SLO burn-rate alerts");
        }
        // 汇总前面计算出的状态和明细，返回给调用方。
        return gaps;
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param assetKey 业务键，用于在同一租户下定位资源。
     * @param displayName 名称文本，用于展示或唯一性校验。
     * @param assetType 类型标识，用于选择对应处理分支。
     * @param ownerCapabilityKey 业务键，用于在同一租户下定位资源。
     * @param providerFamily provider family 参数，用于 asset 流程中的校验、计算或对象转换。
     * @param status 业务状态，用于筛选或推进状态流转。
     * @param apiRoot api root 参数，用于 asset 流程中的校验、计算或对象转换。
     * @param credentialDependency credential dependency 参数，用于 asset 流程中的校验、计算或对象转换。
     * @param pendingWrites pending writes 参数，用于 asset 流程中的校验、计算或对象转换。
     * @param failedWrites failed writes 参数，用于 asset 流程中的校验、计算或对象转换。
     * @param controls controls 参数，用于 asset 流程中的校验、计算或对象转换。
     * @param gaps gaps 参数，用于 asset 流程中的校验、计算或对象转换。
     * @param evidence evidence 参数，用于 asset 流程中的校验、计算或对象转换。
     * @return 返回 asset 流程生成的业务结果。
     */
    private static IntegrationAsset asset(String assetKey,
                                          String displayName,
                                          String assetType,
                                          String ownerCapabilityKey,
                                          String providerFamily,
                                          String status,
                                          String apiRoot,
                                          String credentialDependency,
                                          long pendingWrites,
                                          long failedWrites,
                                          List<String> controls,
                                          List<String> gaps,
                                          List<EvidenceSignal> evidence) {
        return new IntegrationAsset(
                assetKey,
                displayName,
                assetType,
                ownerCapabilityKey,
                providerFamily,
                status,
                apiRoot,
                credentialDependency,
                pendingWrites,
                failedWrites,
                controls,
                gaps,
                evidence);
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param laneKey 业务键，用于在同一租户下定位资源。
     * @param displayName 名称文本，用于展示或唯一性校验。
     * @param sourceCapabilityKey 业务键，用于在同一租户下定位资源。
     * @param targetCapabilityKey 业务键，用于在同一租户下定位资源。
     * @param status 业务状态，用于筛选或推进状态流转。
     * @param controls controls 参数，用于 lane 流程中的校验、计算或对象转换。
     * @return 返回 lane 流程生成的业务结果。
     */
    private static IntegrationLane lane(String laneKey,
                                        String displayName,
                                        String sourceCapabilityKey,
                                        String targetCapabilityKey,
                                        String status,
                                        List<String> controls) {
        return new IntegrationLane(laneKey, displayName, sourceCapabilityKey, targetCapabilityKey, status, controls);
    }

    /**
     * 根据输入和依赖数据计算业务判断结果。
     *
     * @param capabilities capabilities 参数，用于 readinessGate 流程中的校验、计算或对象转换。
     * @param lanes lanes 参数，用于 readinessGate 流程中的校验、计算或对象转换。
     * @param assets assets 参数，用于 readinessGate 流程中的校验、计算或对象转换。
     * @return 返回 readinessGate 流程生成的业务结果。
     */
    private static ReadinessGate readinessGate(List<CapabilityCard> capabilities,
                                               List<IntegrationLane> lanes,
                                               List<IntegrationAsset> assets) {
        List<ReadinessFinding> blockers = new ArrayList<>();
        List<ReadinessFinding> warnings = new ArrayList<>();
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        for (CapabilityCard capability : capabilities) {
            // 校验关键输入和前置条件，避免无效状态继续进入主流程。
            if (CONFIGURATION_REQUIRED.equals(capability.status())) {
                blockers.add(new ReadinessFinding(
                        "BLOCKER",
                        "CAPABILITY",
                        capability.capabilityKey(),
                        "Configure " + capability.displayName(),
                        capability.route(),
                        reason(capability.gaps())));
            } else if (API_ONLY.equals(capability.status())) {
                warnings.add(new ReadinessFinding(
                        "WARNING",
                        "CAPABILITY",
                        capability.capabilityKey(),
                        capability.displayName() + " is API-only",
                        capability.route(),
                        reason(capability.gaps())));
            }
        }
        for (IntegrationAsset asset : assets) {
            if (CONFIGURATION_REQUIRED.equals(asset.status())) {
                blockers.add(new ReadinessFinding(
                        "BLOCKER",
                        "INTEGRATION_ASSET",
                        asset.assetKey(),
                        "Configure " + asset.displayName(),
                        asset.apiRoot(),
                        reason(asset.gaps())));
            } else if (API_ONLY.equals(asset.status())) {
                warnings.add(new ReadinessFinding(
                        "WARNING",
                        "INTEGRATION_ASSET",
                        asset.assetKey(),
                        asset.displayName() + " has no live adapter",
                        asset.apiRoot(),
                        reason(asset.gaps())));
            }
            if (asset.failedWrites() > 0) {
                blockers.add(new ReadinessFinding(
                        "BLOCKER",
                        "PROVIDER_WRITE_FAILURE",
                        asset.assetKey(),
                        "Resolve failed provider writes for " + asset.displayName(),
                        asset.apiRoot(),
                        asset.failedWrites() + " failed provider writes require triage"));
            }
        }
        for (IntegrationLane lane : lanes) {
            if (CONFIGURATION_REQUIRED.equals(lane.status())) {
                warnings.add(new ReadinessFinding(
                        "WARNING",
                        "INTEGRATION_LANE",
                        lane.laneKey(),
                        lane.displayName() + " is not governed",
                        "/marketing-platform",
                        "source=" + lane.sourceCapabilityKey() + "; target=" + lane.targetCapabilityKey()));
            }
        }
        String status = blockers.isEmpty()
                ? (warnings.isEmpty() ? "READY" : "DEGRADED")
                : "BLOCKED";
        // 汇总前面计算出的状态和明细，返回给调用方。
        return new ReadinessGate(
                status,
                blockers.isEmpty(),
                blockers.size(),
                warnings.size(),
                blockers,
                warnings);
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param gaps gaps 参数，用于 reason 流程中的校验、计算或对象转换。
     * @return 返回 reason 生成的文本或业务键。
     */
    private static String reason(List<String> gaps) {
        return gaps == null || gaps.isEmpty() ? "no additional detail" : String.join("; ", gaps);
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param capabilities capabilities 参数，用于 actionItems 流程中的校验、计算或对象转换。
     * @return 返回 action items 汇总后的集合、分页或映射视图。
     */
    private static List<ActionItem> actionItems(List<CapabilityCard> capabilities) {
        List<ActionItem> actions = new ArrayList<>();
        for (CapabilityCard capability : capabilities) {
            if (LIVE.equals(capability.status())) {
                continue;
            }
            boolean configurationRequired = CONFIGURATION_REQUIRED.equals(capability.status());
            actions.add(new ActionItem(
                    configurationRequired ? "HIGH" : "MEDIUM",
                    capability.capabilityKey(),
                    actionTitle(capability),
                    capability.route(),
                    String.join("; ", capability.gaps())));
        }
        return actions;
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param capability capability 参数，用于 actionTitle 流程中的校验、计算或对象转换。
     * @return 返回 action title 生成的文本或业务键。
     */
    private static String actionTitle(CapabilityCard capability) {
        return switch (capability.capabilityKey()) {
            case "journey-orchestration" -> "Publish a production journey";
            case "campaign-master-ledger" -> "Create campaign master records and required resource links";
            case "integration-contract-registry" -> "Register production integration contracts and health probes";
            case "content-lifecycle" -> "Publish active content releases";
            case "marketing-monitoring" -> "Configure monitoring sources and alert channels";
            case "paid-media-activation" -> "Configure paid-media provider destinations";
            case "search-marketing-governance" -> "Wire live SEM provider write clients";
            case "creator-collaboration-governance" -> "Wire creator provider write clients";
            case "programmatic-dsp-governance" -> "Wire DSP provider write clients";
            case "measurement-bi" -> "Publish marketing measurement dashboards";
            case "provider-credential-governance" -> "Complete tenant provider credential onboarding";
            default -> "Complete capability configuration";
        };
    }

    /**
     * 控制面汇总快照。
     *
     * @param tenantId 租户标识
     * @param generatedAt 汇总生成时间
     * @param overallStatus 整体控制面状态
     * @param capabilityCount 能力总数
     * @param liveCapabilityCount 已上线能力数量
     * @param actionItemCount 待办动作数量
     * @param capabilities 能力卡片列表
     * @param integrationLanes 集成链路列表
     * @param integrationAssets 集成资产列表
     * @param readinessGate 生产就绪门禁
     * @param actionItems 待办动作列表
     */
    public static final class ControlPlaneSummary {

        /**
         * 租户标识。
         */
        private final Long tenantId;

        /**
         * 汇总生成时间。
         */
        private final String generatedAt;

        /**
         * 整体控制面状态。
         */
        private final String overallStatus;

        /**
         * 能力总数。
         */
        private final int capabilityCount;

        /**
         * 已上线能力数量。
         */
        private final int liveCapabilityCount;

        /**
         * 待办动作数量。
         */
        private final int actionItemCount;

        /**
         * 能力卡片列表。
         */
        private final List<CapabilityCard> capabilities;

        /**
         * 集成链路列表。
         */
        private final List<IntegrationLane> integrationLanes;

        /**
         * 集成资产列表。
         */
        private final List<IntegrationAsset> integrationAssets;

        /**
         * 生产就绪门禁。
         */
        private final ReadinessGate readinessGate;

        /**
         * 待办动作列表。
         */
        private final List<ActionItem> actionItems;

        /**
         * 创建ControlPlaneSummary。
         *
         * @param tenantId 租户标识
         * @param generatedAt 汇总生成时间
         * @param overallStatus 整体控制面状态
         * @param capabilityCount 能力总数
         * @param liveCapabilityCount 已上线能力数量
         * @param actionItemCount 待办动作数量
         * @param capabilities 能力卡片列表
         * @param integrationLanes 集成链路列表
         * @param integrationAssets 集成资产列表
         * @param readinessGate 生产就绪门禁
         * @param actionItems 待办动作列表
         */
        public ControlPlaneSummary(
                Long tenantId, String generatedAt, String overallStatus, int capabilityCount,
                int liveCapabilityCount, int actionItemCount, List<CapabilityCard> capabilities,
                List<IntegrationLane> integrationLanes, List<IntegrationAsset> integrationAssets,
                ReadinessGate readinessGate, List<ActionItem> actionItems) {
            this.tenantId = tenantId;
            this.generatedAt = generatedAt;
            this.overallStatus = overallStatus;
            this.capabilityCount = capabilityCount;
            this.liveCapabilityCount = liveCapabilityCount;
            this.actionItemCount = actionItemCount;
            this.capabilities = capabilities;
            this.integrationLanes = integrationLanes;
            this.integrationAssets = integrationAssets;
            this.readinessGate = readinessGate;
            this.actionItems = actionItems;
        }

        /**
         * 返回租户标识。
         *
         * @return 租户标识
         */
        public Long tenantId() {
            return tenantId;
        }

        /**
         * 返回汇总生成时间。
         *
         * @return 汇总生成时间
         */
        public String generatedAt() {
            return generatedAt;
        }

        /**
         * 返回整体控制面状态。
         *
         * @return 整体控制面状态
         */
        public String overallStatus() {
            return overallStatus;
        }

        /**
         * 返回能力总数。
         *
         * @return 能力总数
         */
        public int capabilityCount() {
            return capabilityCount;
        }

        /**
         * 返回已上线能力数量。
         *
         * @return 已上线能力数量
         */
        public int liveCapabilityCount() {
            return liveCapabilityCount;
        }

        /**
         * 返回待办动作数量。
         *
         * @return 待办动作数量
         */
        public int actionItemCount() {
            return actionItemCount;
        }

        /**
         * 返回能力卡片列表。
         *
         * @return 能力卡片列表
         */
        public List<CapabilityCard> capabilities() {
            return capabilities;
        }

        /**
         * 返回集成链路列表。
         *
         * @return 集成链路列表
         */
        public List<IntegrationLane> integrationLanes() {
            return integrationLanes;
        }

        /**
         * 返回集成资产列表。
         *
         * @return 集成资产列表
         */
        public List<IntegrationAsset> integrationAssets() {
            return integrationAssets;
        }

        /**
         * 返回生产就绪门禁。
         *
         * @return 生产就绪门禁
         */
        public ReadinessGate readinessGate() {
            return readinessGate;
        }

        /**
         * 返回待办动作列表。
         *
         * @return 待办动作列表
         */
        public List<ActionItem> actionItems() {
            return actionItems;
        }

        /**
         * 判断两个 ControlPlaneSummary 值对象是否相同。
         *
         * @param object 待比较对象
         * @return 所有字段相同时返回 true
         */
        @Override
        public boolean equals(Object object) {
            if (this == object) {
                return true;
            }
            if (!(object instanceof ControlPlaneSummary that)) {
                return false;
            }
            return Objects.equals(tenantId, that.tenantId)
                    && Objects.equals(generatedAt, that.generatedAt)
                    && Objects.equals(overallStatus, that.overallStatus)
                    && capabilityCount == that.capabilityCount
                    && liveCapabilityCount == that.liveCapabilityCount
                    && actionItemCount == that.actionItemCount
                    && Objects.equals(capabilities, that.capabilities)
                    && Objects.equals(integrationLanes, that.integrationLanes)
                    && Objects.equals(integrationAssets, that.integrationAssets)
                    && Objects.equals(readinessGate, that.readinessGate)
                    && Objects.equals(actionItems, that.actionItems);
        }

        /**
         * 计算 ControlPlaneSummary 值对象哈希值。
         *
         * @return 哈希值
         */
        @Override
        public int hashCode() {
            return Objects.hash(tenantId, generatedAt, overallStatus, capabilityCount, liveCapabilityCount, actionItemCount, capabilities, integrationLanes, integrationAssets, readinessGate, actionItems);
        }

        /**
         * 返回与原 record 形态一致的字符串。
         *
         * @return 字符串表示
         */
        @Override
        public String toString() {
            return "ControlPlaneSummary[tenantId=" + tenantId + ", generatedAt=" + generatedAt + ", overallStatus=" + overallStatus + ", capabilityCount=" + capabilityCount + ", liveCapabilityCount=" + liveCapabilityCount + ", actionItemCount=" + actionItemCount + ", capabilities=" + capabilities + ", integrationLanes=" + integrationLanes + ", integrationAssets=" + integrationAssets + ", readinessGate=" + readinessGate + ", actionItems=" + actionItems + "]";
        }
    }

    /**
     * 描述单项控制面能力的卡片。
     *
     * @param capabilityKey 能力稳定键
     * @param displayName 能力展示名称
     * @param domain 能力所属业务域
     * @param status 能力状态
     * @param route 管理端路由
     * @param apiRoot API 根路径
     * @param surface 能力交付形态
     * @param productionSignals 生产可用信号
     * @param gaps 阻止能力上线的缺口
     * @param evidence 支撑能力判断的证据信号
     */
    public static final class CapabilityCard {

        /**
         * 能力稳定键。
         */
        private final String capabilityKey;

        /**
         * 能力展示名称。
         */
        private final String displayName;

        /**
         * 能力所属业务域。
         */
        private final String domain;

        /**
         * 能力状态。
         */
        private final String status;

        /**
         * 管理端路由。
         */
        private final String route;

        /**
         * API 根路径。
         */
        private final String apiRoot;

        /**
         * 能力交付形态。
         */
        private final String surface;

        /**
         * 生产可用信号。
         */
        private final List<String> productionSignals;

        /**
         * 阻止能力上线的缺口。
         */
        private final List<String> gaps;

        /**
         * 支撑能力判断的证据信号。
         */
        private final List<EvidenceSignal> evidence;

        /**
         * 创建CapabilityCard。
         *
         * @param capabilityKey 能力稳定键
         * @param displayName 能力展示名称
         * @param domain 能力所属业务域
         * @param status 能力状态
         * @param route 管理端路由
         * @param apiRoot API 根路径
         * @param surface 能力交付形态
         * @param productionSignals 生产可用信号
         * @param gaps 阻止能力上线的缺口
         * @param evidence 支撑能力判断的证据信号
         */
        public CapabilityCard(
                String capabilityKey, String displayName, String domain, String status, String route,
                String apiRoot, String surface, List<String> productionSignals, List<String> gaps,
                List<EvidenceSignal> evidence) {
            this.capabilityKey = capabilityKey;
            this.displayName = displayName;
            this.domain = domain;
            this.status = status;
            this.route = route;
            this.apiRoot = apiRoot;
            this.surface = surface;
            this.productionSignals = productionSignals;
            this.gaps = gaps;
            this.evidence = evidence;
        }

        /**
         * 返回能力稳定键。
         *
         * @return 能力稳定键
         */
        public String capabilityKey() {
            return capabilityKey;
        }

        /**
         * 返回能力展示名称。
         *
         * @return 能力展示名称
         */
        public String displayName() {
            return displayName;
        }

        /**
         * 返回能力所属业务域。
         *
         * @return 能力所属业务域
         */
        public String domain() {
            return domain;
        }

        /**
         * 返回能力状态。
         *
         * @return 能力状态
         */
        public String status() {
            return status;
        }

        /**
         * 返回管理端路由。
         *
         * @return 管理端路由
         */
        public String route() {
            return route;
        }

        /**
         * 返回API 根路径。
         *
         * @return API 根路径
         */
        public String apiRoot() {
            return apiRoot;
        }

        /**
         * 返回能力交付形态。
         *
         * @return 能力交付形态
         */
        public String surface() {
            return surface;
        }

        /**
         * 返回生产可用信号。
         *
         * @return 生产可用信号
         */
        public List<String> productionSignals() {
            return productionSignals;
        }

        /**
         * 返回阻止能力上线的缺口。
         *
         * @return 阻止能力上线的缺口
         */
        public List<String> gaps() {
            return gaps;
        }

        /**
         * 返回支撑能力判断的证据信号。
         *
         * @return 支撑能力判断的证据信号
         */
        public List<EvidenceSignal> evidence() {
            return evidence;
        }

        /**
         * 判断两个 CapabilityCard 值对象是否相同。
         *
         * @param object 待比较对象
         * @return 所有字段相同时返回 true
         */
        @Override
        public boolean equals(Object object) {
            if (this == object) {
                return true;
            }
            if (!(object instanceof CapabilityCard that)) {
                return false;
            }
            return Objects.equals(capabilityKey, that.capabilityKey)
                    && Objects.equals(displayName, that.displayName)
                    && Objects.equals(domain, that.domain)
                    && Objects.equals(status, that.status)
                    && Objects.equals(route, that.route)
                    && Objects.equals(apiRoot, that.apiRoot)
                    && Objects.equals(surface, that.surface)
                    && Objects.equals(productionSignals, that.productionSignals)
                    && Objects.equals(gaps, that.gaps)
                    && Objects.equals(evidence, that.evidence);
        }

        /**
         * 计算 CapabilityCard 值对象哈希值。
         *
         * @return 哈希值
         */
        @Override
        public int hashCode() {
            return Objects.hash(capabilityKey, displayName, domain, status, route, apiRoot, surface, productionSignals, gaps, evidence);
        }

        /**
         * 返回与原 record 形态一致的字符串。
         *
         * @return 字符串表示
         */
        @Override
        public String toString() {
            return "CapabilityCard[capabilityKey=" + capabilityKey + ", displayName=" + displayName + ", domain=" + domain + ", status=" + status + ", route=" + route + ", apiRoot=" + apiRoot + ", surface=" + surface + ", productionSignals=" + productionSignals + ", gaps=" + gaps + ", evidence=" + evidence + "]";
        }
    }

    /**
     * 表示一个控制面证据信号。
     *
     * @param signalKey 证据信号稳定键
     * @param label 证据信号展示名称
     * @param value 证据信号数值
     * @param status 证据信号状态
     */
    public static final class EvidenceSignal {

        /**
         * 证据信号稳定键。
         */
        private final String signalKey;

        /**
         * 证据信号展示名称。
         */
        private final String label;

        /**
         * 证据信号数值。
         */
        private final long value;

        /**
         * 证据信号状态。
         */
        private final String status;

        /**
         * 创建EvidenceSignal。
         *
         * @param signalKey 证据信号稳定键
         * @param label 证据信号展示名称
         * @param value 证据信号数值
         * @param status 证据信号状态
         */
        public EvidenceSignal(
                String signalKey, String label, long value, String status) {
            this.signalKey = signalKey;
            this.label = label;
            this.value = value;
            this.status = status;
        }

        /**
         * 返回证据信号稳定键。
         *
         * @return 证据信号稳定键
         */
        public String signalKey() {
            return signalKey;
        }

        /**
         * 返回证据信号展示名称。
         *
         * @return 证据信号展示名称
         */
        public String label() {
            return label;
        }

        /**
         * 返回证据信号数值。
         *
         * @return 证据信号数值
         */
        public long value() {
            return value;
        }

        /**
         * 返回证据信号状态。
         *
         * @return 证据信号状态
         */
        public String status() {
            return status;
        }

        /**
         * 判断两个 EvidenceSignal 值对象是否相同。
         *
         * @param object 待比较对象
         * @return 所有字段相同时返回 true
         */
        @Override
        public boolean equals(Object object) {
            if (this == object) {
                return true;
            }
            if (!(object instanceof EvidenceSignal that)) {
                return false;
            }
            return Objects.equals(signalKey, that.signalKey)
                    && Objects.equals(label, that.label)
                    && value == that.value
                    && Objects.equals(status, that.status);
        }

        /**
         * 计算 EvidenceSignal 值对象哈希值。
         *
         * @return 哈希值
         */
        @Override
        public int hashCode() {
            return Objects.hash(signalKey, label, value, status);
        }

        /**
         * 返回与原 record 形态一致的字符串。
         *
         * @return 字符串表示
         */
        @Override
        public String toString() {
            return "EvidenceSignal[signalKey=" + signalKey + ", label=" + label + ", value=" + value + ", status=" + status + "]";
        }
    }

    /**
     * 描述两个控制面能力之间的集成链路。
     *
     * @param laneKey 集成链路稳定键
     * @param displayName 集成链路展示名称
     * @param sourceCapabilityKey 源能力键
     * @param targetCapabilityKey 目标能力键
     * @param status 集成链路状态
     * @param controls 链路所需控制点
     */
    public static final class IntegrationLane {

        /**
         * 集成链路稳定键。
         */
        private final String laneKey;

        /**
         * 集成链路展示名称。
         */
        private final String displayName;

        /**
         * 源能力键。
         */
        private final String sourceCapabilityKey;

        /**
         * 目标能力键。
         */
        private final String targetCapabilityKey;

        /**
         * 集成链路状态。
         */
        private final String status;

        /**
         * 链路所需控制点。
         */
        private final List<String> controls;

        /**
         * 创建IntegrationLane。
         *
         * @param laneKey 集成链路稳定键
         * @param displayName 集成链路展示名称
         * @param sourceCapabilityKey 源能力键
         * @param targetCapabilityKey 目标能力键
         * @param status 集成链路状态
         * @param controls 链路所需控制点
         */
        public IntegrationLane(
                String laneKey, String displayName, String sourceCapabilityKey, String targetCapabilityKey,
                String status, List<String> controls) {
            this.laneKey = laneKey;
            this.displayName = displayName;
            this.sourceCapabilityKey = sourceCapabilityKey;
            this.targetCapabilityKey = targetCapabilityKey;
            this.status = status;
            this.controls = controls;
        }

        /**
         * 返回集成链路稳定键。
         *
         * @return 集成链路稳定键
         */
        public String laneKey() {
            return laneKey;
        }

        /**
         * 返回集成链路展示名称。
         *
         * @return 集成链路展示名称
         */
        public String displayName() {
            return displayName;
        }

        /**
         * 返回源能力键。
         *
         * @return 源能力键
         */
        public String sourceCapabilityKey() {
            return sourceCapabilityKey;
        }

        /**
         * 返回目标能力键。
         *
         * @return 目标能力键
         */
        public String targetCapabilityKey() {
            return targetCapabilityKey;
        }

        /**
         * 返回集成链路状态。
         *
         * @return 集成链路状态
         */
        public String status() {
            return status;
        }

        /**
         * 返回链路所需控制点。
         *
         * @return 链路所需控制点
         */
        public List<String> controls() {
            return controls;
        }

        /**
         * 判断两个 IntegrationLane 值对象是否相同。
         *
         * @param object 待比较对象
         * @return 所有字段相同时返回 true
         */
        @Override
        public boolean equals(Object object) {
            if (this == object) {
                return true;
            }
            if (!(object instanceof IntegrationLane that)) {
                return false;
            }
            return Objects.equals(laneKey, that.laneKey)
                    && Objects.equals(displayName, that.displayName)
                    && Objects.equals(sourceCapabilityKey, that.sourceCapabilityKey)
                    && Objects.equals(targetCapabilityKey, that.targetCapabilityKey)
                    && Objects.equals(status, that.status)
                    && Objects.equals(controls, that.controls);
        }

        /**
         * 计算 IntegrationLane 值对象哈希值。
         *
         * @return 哈希值
         */
        @Override
        public int hashCode() {
            return Objects.hash(laneKey, displayName, sourceCapabilityKey, targetCapabilityKey, status, controls);
        }

        /**
         * 返回与原 record 形态一致的字符串。
         *
         * @return 字符串表示
         */
        @Override
        public String toString() {
            return "IntegrationLane[laneKey=" + laneKey + ", displayName=" + displayName + ", sourceCapabilityKey=" + sourceCapabilityKey + ", targetCapabilityKey=" + targetCapabilityKey + ", status=" + status + ", controls=" + controls + "]";
        }
    }

    /**
     * 描述控制面依赖的集成资产。
     *
     * @param assetKey 集成资产稳定键
     * @param displayName 集成资产展示名称
     * @param assetType 集成资产类型
     * @param ownerCapabilityKey 归属能力键
     * @param providerFamily 供应方家族
     * @param status 集成资产状态
     * @param apiRoot API 根路径
     * @param credentialDependency 凭据依赖说明
     * @param pendingWrites 待写入数量
     * @param failedWrites 写入失败数量
     * @param controls 资产所需控制点
     * @param gaps 阻止资产上线的缺口
     * @param evidence 支撑资产判断的证据信号
     */
    public static final class IntegrationAsset {

        /**
         * 集成资产稳定键。
         */
        private final String assetKey;

        /**
         * 集成资产展示名称。
         */
        private final String displayName;

        /**
         * 集成资产类型。
         */
        private final String assetType;

        /**
         * 归属能力键。
         */
        private final String ownerCapabilityKey;

        /**
         * 供应方家族。
         */
        private final String providerFamily;

        /**
         * 集成资产状态。
         */
        private final String status;

        /**
         * API 根路径。
         */
        private final String apiRoot;

        /**
         * 凭据依赖说明。
         */
        private final String credentialDependency;

        /**
         * 待写入数量。
         */
        private final long pendingWrites;

        /**
         * 写入失败数量。
         */
        private final long failedWrites;

        /**
         * 资产所需控制点。
         */
        private final List<String> controls;

        /**
         * 阻止资产上线的缺口。
         */
        private final List<String> gaps;

        /**
         * 支撑资产判断的证据信号。
         */
        private final List<EvidenceSignal> evidence;

        /**
         * 创建IntegrationAsset。
         *
         * @param assetKey 集成资产稳定键
         * @param displayName 集成资产展示名称
         * @param assetType 集成资产类型
         * @param ownerCapabilityKey 归属能力键
         * @param providerFamily 供应方家族
         * @param status 集成资产状态
         * @param apiRoot API 根路径
         * @param credentialDependency 凭据依赖说明
         * @param pendingWrites 待写入数量
         * @param failedWrites 写入失败数量
         * @param controls 资产所需控制点
         * @param gaps 阻止资产上线的缺口
         * @param evidence 支撑资产判断的证据信号
         */
        public IntegrationAsset(
                String assetKey, String displayName, String assetType, String ownerCapabilityKey,
                String providerFamily, String status, String apiRoot, String credentialDependency,
                long pendingWrites, long failedWrites, List<String> controls, List<String> gaps,
                List<EvidenceSignal> evidence) {
            this.assetKey = assetKey;
            this.displayName = displayName;
            this.assetType = assetType;
            this.ownerCapabilityKey = ownerCapabilityKey;
            this.providerFamily = providerFamily;
            this.status = status;
            this.apiRoot = apiRoot;
            this.credentialDependency = credentialDependency;
            this.pendingWrites = pendingWrites;
            this.failedWrites = failedWrites;
            this.controls = controls;
            this.gaps = gaps;
            this.evidence = evidence;
        }

        /**
         * 返回集成资产稳定键。
         *
         * @return 集成资产稳定键
         */
        public String assetKey() {
            return assetKey;
        }

        /**
         * 返回集成资产展示名称。
         *
         * @return 集成资产展示名称
         */
        public String displayName() {
            return displayName;
        }

        /**
         * 返回集成资产类型。
         *
         * @return 集成资产类型
         */
        public String assetType() {
            return assetType;
        }

        /**
         * 返回归属能力键。
         *
         * @return 归属能力键
         */
        public String ownerCapabilityKey() {
            return ownerCapabilityKey;
        }

        /**
         * 返回供应方家族。
         *
         * @return 供应方家族
         */
        public String providerFamily() {
            return providerFamily;
        }

        /**
         * 返回集成资产状态。
         *
         * @return 集成资产状态
         */
        public String status() {
            return status;
        }

        /**
         * 返回API 根路径。
         *
         * @return API 根路径
         */
        public String apiRoot() {
            return apiRoot;
        }

        /**
         * 返回凭据依赖说明。
         *
         * @return 凭据依赖说明
         */
        public String credentialDependency() {
            return credentialDependency;
        }

        /**
         * 返回待写入数量。
         *
         * @return 待写入数量
         */
        public long pendingWrites() {
            return pendingWrites;
        }

        /**
         * 返回写入失败数量。
         *
         * @return 写入失败数量
         */
        public long failedWrites() {
            return failedWrites;
        }

        /**
         * 返回资产所需控制点。
         *
         * @return 资产所需控制点
         */
        public List<String> controls() {
            return controls;
        }

        /**
         * 返回阻止资产上线的缺口。
         *
         * @return 阻止资产上线的缺口
         */
        public List<String> gaps() {
            return gaps;
        }

        /**
         * 返回支撑资产判断的证据信号。
         *
         * @return 支撑资产判断的证据信号
         */
        public List<EvidenceSignal> evidence() {
            return evidence;
        }

        /**
         * 判断两个 IntegrationAsset 值对象是否相同。
         *
         * @param object 待比较对象
         * @return 所有字段相同时返回 true
         */
        @Override
        public boolean equals(Object object) {
            if (this == object) {
                return true;
            }
            if (!(object instanceof IntegrationAsset that)) {
                return false;
            }
            return Objects.equals(assetKey, that.assetKey)
                    && Objects.equals(displayName, that.displayName)
                    && Objects.equals(assetType, that.assetType)
                    && Objects.equals(ownerCapabilityKey, that.ownerCapabilityKey)
                    && Objects.equals(providerFamily, that.providerFamily)
                    && Objects.equals(status, that.status)
                    && Objects.equals(apiRoot, that.apiRoot)
                    && Objects.equals(credentialDependency, that.credentialDependency)
                    && pendingWrites == that.pendingWrites
                    && failedWrites == that.failedWrites
                    && Objects.equals(controls, that.controls)
                    && Objects.equals(gaps, that.gaps)
                    && Objects.equals(evidence, that.evidence);
        }

        /**
         * 计算 IntegrationAsset 值对象哈希值。
         *
         * @return 哈希值
         */
        @Override
        public int hashCode() {
            return Objects.hash(assetKey, displayName, assetType, ownerCapabilityKey, providerFamily, status, apiRoot, credentialDependency, pendingWrites, failedWrites, controls, gaps, evidence);
        }

        /**
         * 返回与原 record 形态一致的字符串。
         *
         * @return 字符串表示
         */
        @Override
        public String toString() {
            return "IntegrationAsset[assetKey=" + assetKey + ", displayName=" + displayName + ", assetType=" + assetType + ", ownerCapabilityKey=" + ownerCapabilityKey + ", providerFamily=" + providerFamily + ", status=" + status + ", apiRoot=" + apiRoot + ", credentialDependency=" + credentialDependency + ", pendingWrites=" + pendingWrites + ", failedWrites=" + failedWrites + ", controls=" + controls + ", gaps=" + gaps + ", evidence=" + evidence + "]";
        }
    }

    /**
     * 控制面生产就绪门禁。
     *
     * @param status 门禁状态
     * @param productionReady 是否生产就绪
     * @param blockerCount 阻塞项数量
     * @param warningCount 警告项数量
     * @param blockers 阻塞项列表
     * @param warnings 警告项列表
     */
    public static final class ReadinessGate {

        /**
         * 门禁状态。
         */
        private final String status;

        /**
         * 是否生产就绪。
         */
        private final boolean productionReady;

        /**
         * 阻塞项数量。
         */
        private final int blockerCount;

        /**
         * 警告项数量。
         */
        private final int warningCount;

        /**
         * 阻塞项列表。
         */
        private final List<ReadinessFinding> blockers;

        /**
         * 警告项列表。
         */
        private final List<ReadinessFinding> warnings;

        /**
         * 创建ReadinessGate。
         *
         * @param status 门禁状态
         * @param productionReady 是否生产就绪
         * @param blockerCount 阻塞项数量
         * @param warningCount 警告项数量
         * @param blockers 阻塞项列表
         * @param warnings 警告项列表
         */
        public ReadinessGate(
                String status, boolean productionReady, int blockerCount, int warningCount,
                List<ReadinessFinding> blockers, List<ReadinessFinding> warnings) {
            this.status = status;
            this.productionReady = productionReady;
            this.blockerCount = blockerCount;
            this.warningCount = warningCount;
            this.blockers = blockers;
            this.warnings = warnings;
        }

        /**
         * 返回门禁状态。
         *
         * @return 门禁状态
         */
        public String status() {
            return status;
        }

        /**
         * 返回是否生产就绪。
         *
         * @return 是否生产就绪
         */
        public boolean productionReady() {
            return productionReady;
        }

        /**
         * 返回阻塞项数量。
         *
         * @return 阻塞项数量
         */
        public int blockerCount() {
            return blockerCount;
        }

        /**
         * 返回警告项数量。
         *
         * @return 警告项数量
         */
        public int warningCount() {
            return warningCount;
        }

        /**
         * 返回阻塞项列表。
         *
         * @return 阻塞项列表
         */
        public List<ReadinessFinding> blockers() {
            return blockers;
        }

        /**
         * 返回警告项列表。
         *
         * @return 警告项列表
         */
        public List<ReadinessFinding> warnings() {
            return warnings;
        }

        /**
         * 判断两个 ReadinessGate 值对象是否相同。
         *
         * @param object 待比较对象
         * @return 所有字段相同时返回 true
         */
        @Override
        public boolean equals(Object object) {
            if (this == object) {
                return true;
            }
            if (!(object instanceof ReadinessGate that)) {
                return false;
            }
            return Objects.equals(status, that.status)
                    && productionReady == that.productionReady
                    && blockerCount == that.blockerCount
                    && warningCount == that.warningCount
                    && Objects.equals(blockers, that.blockers)
                    && Objects.equals(warnings, that.warnings);
        }

        /**
         * 计算 ReadinessGate 值对象哈希值。
         *
         * @return 哈希值
         */
        @Override
        public int hashCode() {
            return Objects.hash(status, productionReady, blockerCount, warningCount, blockers, warnings);
        }

        /**
         * 返回与原 record 形态一致的字符串。
         *
         * @return 字符串表示
         */
        @Override
        public String toString() {
            return "ReadinessGate[status=" + status + ", productionReady=" + productionReady + ", blockerCount=" + blockerCount + ", warningCount=" + warningCount + ", blockers=" + blockers + ", warnings=" + warnings + "]";
        }
    }

    /**
     * 控制面就绪检查发现项。
     *
     * @param severity 严重级别
     * @param itemType 发现项类型
     * @param itemKey 发现项稳定键
     * @param title 发现项标题
     * @param route 处理入口路由
     * @param reason 发现项原因
     */
    public static final class ReadinessFinding {

        /**
         * 严重级别。
         */
        private final String severity;

        /**
         * 发现项类型。
         */
        private final String itemType;

        /**
         * 发现项稳定键。
         */
        private final String itemKey;

        /**
         * 发现项标题。
         */
        private final String title;

        /**
         * 处理入口路由。
         */
        private final String route;

        /**
         * 发现项原因。
         */
        private final String reason;

        /**
         * 创建ReadinessFinding。
         *
         * @param severity 严重级别
         * @param itemType 发现项类型
         * @param itemKey 发现项稳定键
         * @param title 发现项标题
         * @param route 处理入口路由
         * @param reason 发现项原因
         */
        public ReadinessFinding(
                String severity, String itemType, String itemKey, String title, String route, String reason) {
            this.severity = severity;
            this.itemType = itemType;
            this.itemKey = itemKey;
            this.title = title;
            this.route = route;
            this.reason = reason;
        }

        /**
         * 返回严重级别。
         *
         * @return 严重级别
         */
        public String severity() {
            return severity;
        }

        /**
         * 返回发现项类型。
         *
         * @return 发现项类型
         */
        public String itemType() {
            return itemType;
        }

        /**
         * 返回发现项稳定键。
         *
         * @return 发现项稳定键
         */
        public String itemKey() {
            return itemKey;
        }

        /**
         * 返回发现项标题。
         *
         * @return 发现项标题
         */
        public String title() {
            return title;
        }

        /**
         * 返回处理入口路由。
         *
         * @return 处理入口路由
         */
        public String route() {
            return route;
        }

        /**
         * 返回发现项原因。
         *
         * @return 发现项原因
         */
        public String reason() {
            return reason;
        }

        /**
         * 判断两个 ReadinessFinding 值对象是否相同。
         *
         * @param object 待比较对象
         * @return 所有字段相同时返回 true
         */
        @Override
        public boolean equals(Object object) {
            if (this == object) {
                return true;
            }
            if (!(object instanceof ReadinessFinding that)) {
                return false;
            }
            return Objects.equals(severity, that.severity)
                    && Objects.equals(itemType, that.itemType)
                    && Objects.equals(itemKey, that.itemKey)
                    && Objects.equals(title, that.title)
                    && Objects.equals(route, that.route)
                    && Objects.equals(reason, that.reason);
        }

        /**
         * 计算 ReadinessFinding 值对象哈希值。
         *
         * @return 哈希值
         */
        @Override
        public int hashCode() {
            return Objects.hash(severity, itemType, itemKey, title, route, reason);
        }

        /**
         * 返回与原 record 形态一致的字符串。
         *
         * @return 字符串表示
         */
        @Override
        public String toString() {
            return "ReadinessFinding[severity=" + severity + ", itemType=" + itemType + ", itemKey=" + itemKey + ", title=" + title + ", route=" + route + ", reason=" + reason + "]";
        }
    }

    /**
     * 控制面待办动作。
     *
     * @param priority 待办优先级
     * @param capabilityKey 关联能力键
     * @param title 待办标题
     * @param route 处理入口路由
     * @param reason 待办原因
     */
    public static final class ActionItem {

        /**
         * 待办优先级。
         */
        private final String priority;

        /**
         * 关联能力键。
         */
        private final String capabilityKey;

        /**
         * 待办标题。
         */
        private final String title;

        /**
         * 处理入口路由。
         */
        private final String route;

        /**
         * 待办原因。
         */
        private final String reason;

        /**
         * 创建ActionItem。
         *
         * @param priority 待办优先级
         * @param capabilityKey 关联能力键
         * @param title 待办标题
         * @param route 处理入口路由
         * @param reason 待办原因
         */
        public ActionItem(
                String priority, String capabilityKey, String title, String route, String reason) {
            this.priority = priority;
            this.capabilityKey = capabilityKey;
            this.title = title;
            this.route = route;
            this.reason = reason;
        }

        /**
         * 返回待办优先级。
         *
         * @return 待办优先级
         */
        public String priority() {
            return priority;
        }

        /**
         * 返回关联能力键。
         *
         * @return 关联能力键
         */
        public String capabilityKey() {
            return capabilityKey;
        }

        /**
         * 返回待办标题。
         *
         * @return 待办标题
         */
        public String title() {
            return title;
        }

        /**
         * 返回处理入口路由。
         *
         * @return 处理入口路由
         */
        public String route() {
            return route;
        }

        /**
         * 返回待办原因。
         *
         * @return 待办原因
         */
        public String reason() {
            return reason;
        }

        /**
         * 判断两个 ActionItem 值对象是否相同。
         *
         * @param object 待比较对象
         * @return 所有字段相同时返回 true
         */
        @Override
        public boolean equals(Object object) {
            if (this == object) {
                return true;
            }
            if (!(object instanceof ActionItem that)) {
                return false;
            }
            return Objects.equals(priority, that.priority)
                    && Objects.equals(capabilityKey, that.capabilityKey)
                    && Objects.equals(title, that.title)
                    && Objects.equals(route, that.route)
                    && Objects.equals(reason, that.reason);
        }

        /**
         * 计算 ActionItem 值对象哈希值。
         *
         * @return 哈希值
         */
        @Override
        public int hashCode() {
            return Objects.hash(priority, capabilityKey, title, route, reason);
        }

        /**
         * 返回与原 record 形态一致的字符串。
         *
         * @return 字符串表示
         */
        @Override
        public String toString() {
            return "ActionItem[priority=" + priority + ", capabilityKey=" + capabilityKey + ", title=" + title + ", route=" + route + ", reason=" + reason + "]";
        }
    }
}
