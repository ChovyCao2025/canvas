package org.chovy.canvas.platform;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.chovy.canvas.dal.dataobject.BiDashboardDO;
import org.chovy.canvas.dal.dataobject.CanvasDO;
import org.chovy.canvas.dal.dataobject.ConversationWorkItemDO;
import org.chovy.canvas.dal.dataobject.CreatorCampaignDO;
import org.chovy.canvas.dal.dataobject.MarketingContentReleaseDO;
import org.chovy.canvas.dal.dataobject.GrowthActivityDO;
import org.chovy.canvas.dal.dataobject.GrowthRewardPoolDO;
import org.chovy.canvas.dal.dataobject.MarketingMonitorAlertChannelDO;
import org.chovy.canvas.dal.dataobject.MarketingMonitorAlertDO;
import org.chovy.canvas.dal.dataobject.MarketingMonitorProviderCredentialDO;
import org.chovy.canvas.dal.dataobject.MarketingMonitorSourceDO;
import org.chovy.canvas.dal.dataobject.MarketingIntegrationContractDO;
import org.chovy.canvas.dal.dataobject.PaidMediaAudienceDestinationDO;
import org.chovy.canvas.dal.dataobject.ProgrammaticDspSeatDO;
import org.chovy.canvas.dal.dataobject.ProgrammaticDspMutationDO;
import org.chovy.canvas.dal.dataobject.SearchMarketingSourceDO;
import org.chovy.canvas.dal.dataobject.SearchMarketingMutationDO;
import org.chovy.canvas.dal.dataobject.MarketingCampaignLinkDO;
import org.chovy.canvas.dal.dataobject.MarketingCampaignMasterDO;
import org.chovy.canvas.dal.mapper.BiDashboardMapper;
import org.chovy.canvas.dal.mapper.CanvasMapper;
import org.chovy.canvas.dal.mapper.ConversationWorkItemMapper;
import org.chovy.canvas.dal.mapper.CreatorCampaignMapper;
import org.chovy.canvas.dal.dataobject.CreatorProviderMutationDO;
import org.chovy.canvas.dal.mapper.GrowthActivityMapper;
import org.chovy.canvas.dal.mapper.GrowthRewardPoolMapper;
import org.chovy.canvas.dal.mapper.MarketingContentReleaseMapper;
import org.chovy.canvas.dal.mapper.MarketingMonitorAlertChannelMapper;
import org.chovy.canvas.dal.mapper.MarketingMonitorAlertMapper;
import org.chovy.canvas.dal.mapper.MarketingMonitorProviderCredentialMapper;
import org.chovy.canvas.dal.mapper.MarketingMonitorSourceMapper;
import org.chovy.canvas.dal.mapper.MarketingIntegrationContractMapper;
import org.chovy.canvas.dal.mapper.MarketingIntegrationContractProbeRunMapper;
import org.chovy.canvas.dal.mapper.PaidMediaAudienceDestinationMapper;
import org.chovy.canvas.dal.mapper.ProgrammaticDspMutationMapper;
import org.chovy.canvas.dal.mapper.ProgrammaticDspSeatMapper;
import org.chovy.canvas.dal.mapper.CreatorProviderMutationMapper;
import org.chovy.canvas.dal.mapper.SearchMarketingMutationMapper;
import org.chovy.canvas.dal.mapper.SearchMarketingSourceMapper;
import org.chovy.canvas.dal.mapper.MarketingCampaignLinkMapper;
import org.chovy.canvas.dal.mapper.MarketingCampaignMasterMapper;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.function.Supplier;

/**
 * JdbcMarketingPlatformControlPlaneEvidenceProvider 汇总 platform 场景的平台策略证据。
 */
@Component
public class JdbcMarketingPlatformControlPlaneEvidenceProvider
        implements MarketingPlatformControlPlaneEvidenceProvider {

    private static final long INTEGRATION_PROBE_FRESHNESS_HOURS = 24;
    private static final String INTEGRATION_PROBE_FAILURE_ALERT = "INTEGRATION_CONTRACT_PROBE_FAILURE";
    private static final String INTEGRATION_SLO_BURN_RATE_ALERT = "INTEGRATION_CONTRACT_SLO_BURN_RATE";

    private final CanvasMapper canvasMapper;
    private final MarketingContentReleaseMapper contentReleaseMapper;
    private final ConversationWorkItemMapper conversationWorkItemMapper;
    private final MarketingMonitorSourceMapper monitorSourceMapper;
    private final MarketingMonitorAlertChannelMapper alertChannelMapper;
    private final PaidMediaAudienceDestinationMapper paidMediaDestinationMapper;
    private final MarketingMonitorProviderCredentialMapper providerCredentialMapper;
    private final SearchMarketingSourceMapper searchSourceMapper;
    private final CreatorCampaignMapper creatorCampaignMapper;
    private final ProgrammaticDspSeatMapper dspSeatMapper;
    private final BiDashboardMapper biDashboardMapper;
    private final SearchMarketingMutationMapper searchMutationMapper;
    private final CreatorProviderMutationMapper creatorMutationMapper;
    private final ProgrammaticDspMutationMapper dspMutationMapper;
    private final MarketingCampaignMasterMapper campaignMasterMapper;
    private final MarketingCampaignLinkMapper campaignLinkMapper;
    private final MarketingIntegrationContractMapper integrationContractMapper;
    private final MarketingIntegrationContractProbeRunMapper integrationContractProbeRunMapper;
    private final MarketingMonitorAlertMapper alertMapper;
    private final GrowthActivityMapper growthActivityMapper;
    private final GrowthRewardPoolMapper growthRewardPoolMapper;

    /**
     * 创建 JdbcMarketingPlatformControlPlaneEvidenceProvider 实例并注入 platform 场景依赖。
     * @param canvasMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param contentReleaseMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param conversationWorkItemMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param monitorSourceMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param alertChannelMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param paidMediaDestinationMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param providerCredentialMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param searchSourceMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param creatorCampaignMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param dspSeatMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param biDashboardMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param searchMutationMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param creatorMutationMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param dspMutationMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param campaignMasterMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param campaignLinkMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param integrationContractMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param integrationContractProbeRunMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param alertMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param growthActivityMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param growthRewardPoolMapper 依赖组件，用于完成数据访问或外部能力调用。
     */
    public JdbcMarketingPlatformControlPlaneEvidenceProvider(
            CanvasMapper canvasMapper,
            MarketingContentReleaseMapper contentReleaseMapper,
            ConversationWorkItemMapper conversationWorkItemMapper,
            MarketingMonitorSourceMapper monitorSourceMapper,
            MarketingMonitorAlertChannelMapper alertChannelMapper,
            PaidMediaAudienceDestinationMapper paidMediaDestinationMapper,
            MarketingMonitorProviderCredentialMapper providerCredentialMapper,
            SearchMarketingSourceMapper searchSourceMapper,
            CreatorCampaignMapper creatorCampaignMapper,
            ProgrammaticDspSeatMapper dspSeatMapper,
            BiDashboardMapper biDashboardMapper,
            SearchMarketingMutationMapper searchMutationMapper,
            CreatorProviderMutationMapper creatorMutationMapper,
            ProgrammaticDspMutationMapper dspMutationMapper,
            MarketingCampaignMasterMapper campaignMasterMapper,
            MarketingCampaignLinkMapper campaignLinkMapper,
            MarketingIntegrationContractMapper integrationContractMapper,
            MarketingIntegrationContractProbeRunMapper integrationContractProbeRunMapper,
            MarketingMonitorAlertMapper alertMapper,
            GrowthActivityMapper growthActivityMapper,
            GrowthRewardPoolMapper growthRewardPoolMapper) {
        // 准备本次处理所需的上下文和中间变量。
        this.canvasMapper = canvasMapper;
        this.contentReleaseMapper = contentReleaseMapper;
        this.conversationWorkItemMapper = conversationWorkItemMapper;
        this.monitorSourceMapper = monitorSourceMapper;
        this.alertChannelMapper = alertChannelMapper;
        this.paidMediaDestinationMapper = paidMediaDestinationMapper;
        this.providerCredentialMapper = providerCredentialMapper;
        this.searchSourceMapper = searchSourceMapper;
        this.creatorCampaignMapper = creatorCampaignMapper;
        this.dspSeatMapper = dspSeatMapper;
        this.biDashboardMapper = biDashboardMapper;
        this.searchMutationMapper = searchMutationMapper;
        this.creatorMutationMapper = creatorMutationMapper;
        this.dspMutationMapper = dspMutationMapper;
        this.campaignMasterMapper = campaignMasterMapper;
        this.campaignLinkMapper = campaignLinkMapper;
        this.integrationContractMapper = integrationContractMapper;
        this.integrationContractProbeRunMapper = integrationContractProbeRunMapper;
        this.alertMapper = alertMapper;
        this.growthActivityMapper = growthActivityMapper;
        this.growthRewardPoolMapper = growthRewardPoolMapper;
    }

    /**
     * evidence 处理 platform 场景的业务逻辑。
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @return 返回 evidence 流程生成的业务结果。
     */
    @Override
    public RuntimeEvidence evidence(Long tenantId) {
        Long scopedTenantId = tenantId == null || tenantId < 0 ? 0L : tenantId;
        LocalDateTime freshProbeAfter = LocalDateTime.now().minusHours(INTEGRATION_PROBE_FRESHNESS_HOURS);
        // 每个计数独立归一化，单个看板指标降级为 0 时不改变证据结构。
        return new RuntimeEvidence(
                count(() -> canvasMapper.selectCount(new LambdaQueryWrapper<CanvasDO>()
                        .eq(CanvasDO::getTenantId, scopedTenantId)
                        .eq(CanvasDO::getStatus, 1))),
                count(() -> contentReleaseMapper.selectCount(new LambdaQueryWrapper<MarketingContentReleaseDO>()
                        .eq(MarketingContentReleaseDO::getTenantId, scopedTenantId)
                        .eq(MarketingContentReleaseDO::getStatus, "ACTIVE"))),
                count(() -> conversationWorkItemMapper.selectCount(new LambdaQueryWrapper<ConversationWorkItemDO>()
                        .eq(ConversationWorkItemDO::getTenantId, scopedTenantId))),
                count(() -> monitorSourceMapper.selectCount(new LambdaQueryWrapper<MarketingMonitorSourceDO>()
                        .eq(MarketingMonitorSourceDO::getTenantId, scopedTenantId)
                        .eq(MarketingMonitorSourceDO::getEnabled, 1))),
                count(() -> alertChannelMapper.selectCount(new LambdaQueryWrapper<MarketingMonitorAlertChannelDO>()
                        .eq(MarketingMonitorAlertChannelDO::getTenantId, scopedTenantId)
                        .eq(MarketingMonitorAlertChannelDO::getEnabled, 1))),
                count(() -> paidMediaDestinationMapper.selectCount(new LambdaQueryWrapper<PaidMediaAudienceDestinationDO>()
                        .eq(PaidMediaAudienceDestinationDO::getTenantId, scopedTenantId)
                        .eq(PaidMediaAudienceDestinationDO::getEnabled, 1))),
                count(() -> providerCredentialMapper.selectCount(new LambdaQueryWrapper<MarketingMonitorProviderCredentialDO>()
                        .eq(MarketingMonitorProviderCredentialDO::getTenantId, scopedTenantId)
                        .eq(MarketingMonitorProviderCredentialDO::getStatus, "ACTIVE"))),
                count(() -> searchSourceMapper.selectCount(new LambdaQueryWrapper<SearchMarketingSourceDO>()
                        .eq(SearchMarketingSourceDO::getTenantId, scopedTenantId)
                        .eq(SearchMarketingSourceDO::getEnabled, 1))),
                count(() -> creatorCampaignMapper.selectCount(new LambdaQueryWrapper<CreatorCampaignDO>()
                        .eq(CreatorCampaignDO::getTenantId, scopedTenantId)
                        .eq(CreatorCampaignDO::getStatus, "ACTIVE"))),
                count(() -> dspSeatMapper.selectCount(new LambdaQueryWrapper<ProgrammaticDspSeatDO>()
                        .eq(ProgrammaticDspSeatDO::getTenantId, scopedTenantId)
                        .eq(ProgrammaticDspSeatDO::getEnabled, 1))),
                count(() -> biDashboardMapper.selectCount(new LambdaQueryWrapper<BiDashboardDO>()
                        .eq(BiDashboardDO::getTenantId, scopedTenantId)
                        .eq(BiDashboardDO::getStatus, "PUBLISHED"))),
                // 按投放渠道族聚合变更计数，同时保持总量、待审批和失败槽位顺序一致。
                count(() -> searchMutationMapper.selectCount(new LambdaQueryWrapper<SearchMarketingMutationDO>()
                        .eq(SearchMarketingMutationDO::getTenantId, scopedTenantId))),
                count(() -> searchMutationMapper.selectCount(new LambdaQueryWrapper<SearchMarketingMutationDO>()
                        .eq(SearchMarketingMutationDO::getTenantId, scopedTenantId)
                        .eq(SearchMarketingMutationDO::getApprovalStatus, "PENDING"))),
                count(() -> searchMutationMapper.selectCount(new LambdaQueryWrapper<SearchMarketingMutationDO>()
                        .eq(SearchMarketingMutationDO::getTenantId, scopedTenantId)
                        .in(SearchMarketingMutationDO::getStatus, "FAILED", "DRY_RUN_FAILED"))),
                count(() -> creatorMutationMapper.selectCount(new LambdaQueryWrapper<CreatorProviderMutationDO>()
                        .eq(CreatorProviderMutationDO::getTenantId, scopedTenantId))),
                count(() -> creatorMutationMapper.selectCount(new LambdaQueryWrapper<CreatorProviderMutationDO>()
                        .eq(CreatorProviderMutationDO::getTenantId, scopedTenantId)
                        .eq(CreatorProviderMutationDO::getApprovalStatus, "PENDING"))),
                count(() -> creatorMutationMapper.selectCount(new LambdaQueryWrapper<CreatorProviderMutationDO>()
                        .eq(CreatorProviderMutationDO::getTenantId, scopedTenantId)
                        .in(CreatorProviderMutationDO::getStatus, "FAILED", "DRY_RUN_FAILED"))),
                count(() -> dspMutationMapper.selectCount(new LambdaQueryWrapper<ProgrammaticDspMutationDO>()
                        .eq(ProgrammaticDspMutationDO::getTenantId, scopedTenantId))),
                count(() -> dspMutationMapper.selectCount(new LambdaQueryWrapper<ProgrammaticDspMutationDO>()
                        .eq(ProgrammaticDspMutationDO::getTenantId, scopedTenantId)
                        .eq(ProgrammaticDspMutationDO::getApprovalStatus, "PENDING"))),
                count(() -> dspMutationMapper.selectCount(new LambdaQueryWrapper<ProgrammaticDspMutationDO>()
                        .eq(ProgrammaticDspMutationDO::getTenantId, scopedTenantId)
                        .in(ProgrammaticDspMutationDO::getStatus, "FAILED", "DRY_RUN_FAILED"))),
                count(() -> campaignMasterMapper.selectCount(new LambdaQueryWrapper<MarketingCampaignMasterDO>()
                        .eq(MarketingCampaignMasterDO::getTenantId, scopedTenantId)
                        .eq(MarketingCampaignMasterDO::getStatus, "ACTIVE"))),
                count(() -> campaignLinkMapper.selectCount(new LambdaQueryWrapper<MarketingCampaignLinkDO>()
                        .eq(MarketingCampaignLinkDO::getTenantId, scopedTenantId))),
                count(() -> campaignLinkMapper.selectCount(new LambdaQueryWrapper<MarketingCampaignLinkDO>()
                        .eq(MarketingCampaignLinkDO::getTenantId, scopedTenantId)
                        .eq(MarketingCampaignLinkDO::getRequiredForLaunch, 1))),
                count(() -> campaignLinkMapper.selectCount(new LambdaQueryWrapper<MarketingCampaignLinkDO>()
                        .eq(MarketingCampaignLinkDO::getTenantId, scopedTenantId)
                        .eq(MarketingCampaignLinkDO::getLinkStatus, "BLOCKED"))),
                count(() -> campaignLinkMapper.countActiveCampaignsWithInactiveRequiredLinks(scopedTenantId)),
                count(() -> campaignLinkMapper.countActiveCampaignsMissingPrimaryDependency(scopedTenantId)),
                count(() -> campaignLinkMapper.countActiveCampaignsMissingMeasurementDependency(scopedTenantId)),
                count(() -> integrationContractMapper.selectCount(
                        new LambdaQueryWrapper<MarketingIntegrationContractDO>()
                                .eq(MarketingIntegrationContractDO::getTenantId, scopedTenantId)
                                .eq(MarketingIntegrationContractDO::getStatus, "ACTIVE"))),
                // 使用新鲜探测窗口区分证据缺失、证据过期和当前生产集成失败。
                count(() -> integrationContractMapper.selectCount(
                        new LambdaQueryWrapper<MarketingIntegrationContractDO>()
                                .eq(MarketingIntegrationContractDO::getTenantId, scopedTenantId)
                                .eq(MarketingIntegrationContractDO::getEnvironment, "PRODUCTION")
                                .eq(MarketingIntegrationContractDO::getStatus, "ACTIVE"))),
                count(() -> integrationContractMapper.selectCount(
                        new LambdaQueryWrapper<MarketingIntegrationContractDO>()
                                .eq(MarketingIntegrationContractDO::getTenantId, scopedTenantId)
                                .eq(MarketingIntegrationContractDO::getStatus, "BLOCKED"))),
                count(() -> integrationContractMapper.selectCount(
                        new LambdaQueryWrapper<MarketingIntegrationContractDO>()
                                .eq(MarketingIntegrationContractDO::getTenantId, scopedTenantId)
                                .eq(MarketingIntegrationContractDO::getStatus, "DEGRADED"))),
                count(() -> integrationContractProbeRunMapper.countFreshPassingProductionContracts(
                        scopedTenantId,
                        freshProbeAfter)),
                count(() -> integrationContractProbeRunMapper.countFreshFailingProductionContracts(
                        scopedTenantId,
                        freshProbeAfter)),
                count(() -> alertMapper.selectCount(new LambdaQueryWrapper<MarketingMonitorAlertDO>()
                        .eq(MarketingMonitorAlertDO::getTenantId, scopedTenantId)
                        .eq(MarketingMonitorAlertDO::getStatus, "OPEN")
                        .eq(MarketingMonitorAlertDO::getAlertType, INTEGRATION_PROBE_FAILURE_ALERT))),
                count(() -> alertMapper.selectCount(new LambdaQueryWrapper<MarketingMonitorAlertDO>()
                        .eq(MarketingMonitorAlertDO::getTenantId, scopedTenantId)
                        .eq(MarketingMonitorAlertDO::getStatus, "OPEN")
                        .eq(MarketingMonitorAlertDO::getAlertType, INTEGRATION_SLO_BURN_RATE_ALERT))),
                count(() -> growthActivityMapper.selectCount(new LambdaQueryWrapper<GrowthActivityDO>()
                        .eq(GrowthActivityDO::getTenantId, scopedTenantId)
                        .eq(GrowthActivityDO::getStatus, "ACTIVE"))),
                count(() -> growthRewardPoolMapper.selectCount(new LambdaQueryWrapper<GrowthRewardPoolDO>()
                        .eq(GrowthRewardPoolDO::getTenantId, scopedTenantId)
                        .eq(GrowthRewardPoolDO::getStatus, "ACTIVE"))),
                count(() -> growthActivityMapper.selectCount(new LambdaQueryWrapper<GrowthActivityDO>()
                        .eq(GrowthActivityDO::getTenantId, scopedTenantId)
                        .eq(GrowthActivityDO::getStatus, "ACTIVE")
                        .isNotNull(GrowthActivityDO::getCampaignId)
                        .isNotNull(GrowthActivityDO::getDashboardRef))),
                count(() -> growthActivityMapper.selectCount(new LambdaQueryWrapper<GrowthActivityDO>()
                        .eq(GrowthActivityDO::getTenantId, scopedTenantId)
                        .eq(GrowthActivityDO::getStatus, "ACTIVE")
                        .and(wrapper -> wrapper.isNull(GrowthActivityDO::getCampaignId)
                                .or()
                                .isNull(GrowthActivityDO::getDashboardRef)))));
    }

    /**
     * 执行计数查询并把空结果归一化为 0。
     *
     * @param supplier 延迟执行的 Mapper 计数查询
     * @return 非空计数值，Mapper 返回 null 时为 0
     */
    private long count(Supplier<Long> supplier) {
        // Mapper 空返回统一归一化为 0，保证证据生成过程稳定且 DTO 可安全序列化。
        Long value = supplier.get();
        return value == null ? 0L : value;
    }
}
