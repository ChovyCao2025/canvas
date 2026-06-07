package org.chovy.canvas.platform;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class MarketingPlatformControlPlaneService {

    private static final String LIVE = "LIVE";
    private static final String API_ONLY = "API_ONLY";
    private static final String CONFIGURATION_REQUIRED = "CONFIGURATION_REQUIRED";

    private final MarketingPlatformControlPlaneEvidenceProvider evidenceProvider;
    private final Clock clock;

    @Autowired
    public MarketingPlatformControlPlaneService(MarketingPlatformControlPlaneEvidenceProvider evidenceProvider) {
        this(evidenceProvider, Clock.systemDefaultZone());
    }

    MarketingPlatformControlPlaneService(Clock clock) {
        this(MarketingPlatformControlPlaneEvidenceProvider.empty(), clock);
    }

    MarketingPlatformControlPlaneService(MarketingPlatformControlPlaneEvidenceProvider evidenceProvider,
                                         Clock clock) {
        this.evidenceProvider = evidenceProvider == null
                ? MarketingPlatformControlPlaneEvidenceProvider.empty()
                : evidenceProvider;
        this.clock = clock == null ? Clock.systemDefaultZone() : clock;
    }

    public ControlPlaneSummary summary(Long tenantId) {
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
        long liveCount = capabilities.stream()
                .filter(capability -> LIVE.equals(capability.status()))
                .count();
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

    private static String overallStatus(List<CapabilityCard> capabilities) {
        boolean needsConfiguration = capabilities.stream()
                .anyMatch(capability -> CONFIGURATION_REQUIRED.equals(capability.status()));
        if (needsConfiguration) {
            return CONFIGURATION_REQUIRED;
        }
        boolean apiOnly = capabilities.stream()
                .anyMatch(capability -> API_ONLY.equals(capability.status()));
        return apiOnly ? API_ONLY : "READY";
    }

    private static List<CapabilityCard> capabilities(
            MarketingPlatformControlPlaneEvidenceProvider.RuntimeEvidence evidence) {
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
        boolean integrationContractReady = integrationContractReady(evidence);
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

    private static String readyStatus(boolean ready) {
        return ready ? LIVE : CONFIGURATION_REQUIRED;
    }

    private static List<String> gaps(boolean ready, String gap) {
        return ready ? List.of() : List.of(gap);
    }

    private static EvidenceSignal signal(String signalKey, String label, long value) {
        return new EvidenceSignal(signalKey, label, value, value > 0 ? "PRESENT" : "MISSING");
    }

    private static EvidenceSignal absenceSignal(String signalKey, String label, long value) {
        return new EvidenceSignal(signalKey, label, value, value == 0 ? "PRESENT" : "MISSING");
    }

    private static List<EvidenceSignal> evidence(EvidenceSignal... signals) {
        return List.of(signals);
    }

    private static List<IntegrationLane> integrationLanes(List<CapabilityCard> capabilities) {
        Map<String, String> capabilityStatuses = capabilities.stream()
                .collect(Collectors.toMap(CapabilityCard::capabilityKey, CapabilityCard::status));
        return List.of(
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

    private static String laneStatus(Map<String, String> capabilityStatuses,
                                     String sourceCapabilityKey,
                                     String targetCapabilityKey) {
        String sourceStatus = capabilityStatuses.getOrDefault(sourceCapabilityKey, CONFIGURATION_REQUIRED);
        String targetStatus = capabilityStatuses.getOrDefault(targetCapabilityKey, CONFIGURATION_REQUIRED);
        return LIVE.equals(sourceStatus) && LIVE.equals(targetStatus) ? "GOVERNED" : CONFIGURATION_REQUIRED;
    }

    private static List<IntegrationAsset> integrationAssets(
            MarketingPlatformControlPlaneEvidenceProvider.RuntimeEvidence evidence) {
        boolean credentialReady = evidence.activeProviderCredentialCount() > 0;
        boolean contentJourneyReady = evidence.activeContentReleaseCount() > 0 && evidence.publishedJourneyCount() > 0;
        boolean monitoringReady = evidence.activeMonitoringSourceCount() > 0
                && evidence.enabledAlertChannelCount() > 0
                && credentialReady;
        boolean paidMediaReady = evidence.enabledPaidMediaDestinationCount() > 0 && credentialReady;
        return List.of(
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

    private static String providerWriteStatus(long configuredSourceCount, boolean credentialReady) {
        return configuredSourceCount > 0 && credentialReady ? API_ONLY : CONFIGURATION_REQUIRED;
    }

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

    private static List<String> campaignGaps(MarketingPlatformControlPlaneEvidenceProvider.RuntimeEvidence evidence) {
        List<String> gaps = new ArrayList<>();
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
        return gaps;
    }

    private static boolean campaignLaunchCoverageReady(
            MarketingPlatformControlPlaneEvidenceProvider.RuntimeEvidence evidence) {
        return evidence.blockedCampaignResourceLinkCount() == 0
                && evidence.campaignsWithInactiveRequiredLinks() == 0
                && evidence.campaignsMissingPrimaryDependency() == 0
                && evidence.campaignsMissingMeasurementDependency() == 0;
    }

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

    private static List<String> integrationContractGaps(
            MarketingPlatformControlPlaneEvidenceProvider.RuntimeEvidence evidence) {
        List<String> gaps = new ArrayList<>();
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
        return gaps;
    }

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

    private static IntegrationLane lane(String laneKey,
                                        String displayName,
                                        String sourceCapabilityKey,
                                        String targetCapabilityKey,
                                        String status,
                                        List<String> controls) {
        return new IntegrationLane(laneKey, displayName, sourceCapabilityKey, targetCapabilityKey, status, controls);
    }

    private static ReadinessGate readinessGate(List<CapabilityCard> capabilities,
                                               List<IntegrationLane> lanes,
                                               List<IntegrationAsset> assets) {
        List<ReadinessFinding> blockers = new ArrayList<>();
        List<ReadinessFinding> warnings = new ArrayList<>();
        for (CapabilityCard capability : capabilities) {
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
        return new ReadinessGate(
                status,
                blockers.isEmpty(),
                blockers.size(),
                warnings.size(),
                blockers,
                warnings);
    }

    private static String reason(List<String> gaps) {
        return gaps == null || gaps.isEmpty() ? "no additional detail" : String.join("; ", gaps);
    }

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

    public record EvidenceSignal(
            String signalKey,
            String label,
            long value,
            String status) {
    }

    public record IntegrationLane(
            String laneKey,
            String displayName,
            String sourceCapabilityKey,
            String targetCapabilityKey,
            String status,
            List<String> controls) {
    }

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

    public record ReadinessGate(
            String status,
            boolean productionReady,
            int blockerCount,
            int warningCount,
            List<ReadinessFinding> blockers,
            List<ReadinessFinding> warnings) {
    }

    public record ReadinessFinding(
            String severity,
            String itemType,
            String itemKey,
            String title,
            String route,
            String reason) {
    }

    public record ActionItem(
            String priority,
            String capabilityKey,
            String title,
            String route,
            String reason) {
    }
}
