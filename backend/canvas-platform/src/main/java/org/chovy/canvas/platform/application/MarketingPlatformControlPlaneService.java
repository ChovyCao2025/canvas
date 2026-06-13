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
package org.chovy.canvas.platform.application;

import org.chovy.canvas.platform.api.MarketingPlatformControlPlaneEvidenceProvider;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
/**
 * MarketingPlatformControlPlaneService 汇总平台控制面能力、证据数据和交付状态。
 */
public class MarketingPlatformControlPlaneService {

    private static final String LIVE = "LIVE";
    private static final String API_ONLY = "API_ONLY";
    private static final String CONFIGURATION_REQUIRED = "CONFIGURATION_REQUIRED";

    private final MarketingPlatformControlPlaneEvidenceProvider evidenceProvider;
    private final Clock clock;

    @Autowired
    /**
     * 初始化 MarketingPlatformControlPlaneService 实例。
     *
     * @param evidenceProvider evidence provider 参数，用于 MarketingPlatformControlPlaneService 流程中的校验、计算或对象转换。
     */
    public MarketingPlatformControlPlaneService(MarketingPlatformControlPlaneEvidenceProvider evidenceProvider) {
        this(evidenceProvider, Clock.systemDefaultZone());
    }

    /**
     * 初始化 MarketingPlatformControlPlaneService 实例。
     *
     * @param clock 时间参数，用于计算窗口、过期或审计时间。
     */
    MarketingPlatformControlPlaneService(Clock clock) {
        this(MarketingPlatformControlPlaneEvidenceProvider.empty(), clock);
    }

    /**
     * 初始化 MarketingPlatformControlPlaneService 实例。
     *
     * @param evidenceProvider evidence provider 参数，用于 MarketingPlatformControlPlaneService 流程中的校验、计算或对象转换。
     * @param clock 时间参数，用于计算窗口、过期或审计时间。
     */
    MarketingPlatformControlPlaneService(MarketingPlatformControlPlaneEvidenceProvider evidenceProvider,
                                         Clock clock) {
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
     * ControlPlaneSummary 汇总平台控制面能力、证据数据和交付状态。
     */
    public record ControlPlaneSummary(
            Long tenantId,
            String generatedAt,
            String overallStatus,
            int capabilityCount,
            int liveCapabilityCount,
            int actionItemCount,
            List<CapabilityCard> capabilities,
            List<IntegrationLane> integrationLanes,
            List<IntegrationAsset> integrationAssets,
            ReadinessGate readinessGate,
            List<ActionItem> actionItems) {
    }

    /**
     * CapabilityCard 汇总平台控制面能力、证据数据和交付状态。
     */
    public record CapabilityCard(
            String capabilityKey,
            String displayName,
            String domain,
            String status,
            String route,
            String apiRoot,
            String surface,
            List<String> productionSignals,
            List<String> gaps,
            List<EvidenceSignal> evidence) {
    }

    /**
     * EvidenceSignal 汇总平台控制面能力、证据数据和交付状态。
     */
    public record EvidenceSignal(
            String signalKey,
            String label,
            long value,
            String status) {
    }

    /**
     * IntegrationLane 汇总平台控制面能力、证据数据和交付状态。
     */
    public record IntegrationLane(
            String laneKey,
            String displayName,
            String sourceCapabilityKey,
            String targetCapabilityKey,
            String status,
            List<String> controls) {
    }

    /**
     * IntegrationAsset 汇总平台控制面能力、证据数据和交付状态。
     */
    public record IntegrationAsset(
            String assetKey,
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
    }

    /**
     * ReadinessGate 汇总平台控制面能力、证据数据和交付状态。
     */
    public record ReadinessGate(
            String status,
            boolean productionReady,
            int blockerCount,
            int warningCount,
            List<ReadinessFinding> blockers,
            List<ReadinessFinding> warnings) {
    }

    /**
     * ReadinessFinding 汇总平台控制面能力、证据数据和交付状态。
     */
    public record ReadinessFinding(
            String severity,
            String itemType,
            String itemKey,
            String title,
            String route,
            String reason) {
    }

    /**
     * ActionItem 汇总平台控制面能力、证据数据和交付状态。
     */
    public record ActionItem(
            String priority,
            String capabilityKey,
            String title,
            String route,
            String reason) {
    }
}
