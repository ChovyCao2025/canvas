package org.chovy.canvas.domain.programmatic;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.dal.dataobject.ProgrammaticDspCampaignDO;
import org.chovy.canvas.dal.dataobject.ProgrammaticDspLineItemDO;
import org.chovy.canvas.dal.dataobject.ProgrammaticDspPerformanceSnapshotDO;
import org.chovy.canvas.dal.dataobject.ProgrammaticDspSeatDO;
import org.chovy.canvas.dal.dataobject.ProgrammaticDspSupplyPathDO;
import org.chovy.canvas.dal.mapper.ProgrammaticDspCampaignMapper;
import org.chovy.canvas.dal.mapper.ProgrammaticDspLineItemMapper;
import org.chovy.canvas.dal.mapper.ProgrammaticDspPerformanceSnapshotMapper;
import org.chovy.canvas.dal.mapper.ProgrammaticDspSeatMapper;
import org.chovy.canvas.dal.mapper.ProgrammaticDspSupplyPathMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * ProgrammaticDspService 编排 domain.programmatic 场景的领域业务规则。
 */
@Service
public class ProgrammaticDspService {

    private static final BigDecimal ZERO = BigDecimal.ZERO;
    private static final TypeReference<Map<String, Object>> OBJECT_MAP = new TypeReference<>() {
    };

    private final ProgrammaticDspSeatMapper seatMapper;
    private final ProgrammaticDspCampaignMapper campaignMapper;
    private final ProgrammaticDspLineItemMapper lineItemMapper;
    private final ProgrammaticDspSupplyPathMapper supplyPathMapper;
    private final ProgrammaticDspPerformanceSnapshotMapper snapshotMapper;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    /**
     * 创建 ProgrammaticDspService 实例并注入 domain.programmatic 场景依赖。
     * @param seatMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param campaignMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param lineItemMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param supplyPathMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param snapshotMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param objectMapper 依赖组件，用于完成数据访问或外部能力调用。
     */
    @Autowired
    public ProgrammaticDspService(ProgrammaticDspSeatMapper seatMapper,
                                  ProgrammaticDspCampaignMapper campaignMapper,
                                  ProgrammaticDspLineItemMapper lineItemMapper,
                                  ProgrammaticDspSupplyPathMapper supplyPathMapper,
                                  ProgrammaticDspPerformanceSnapshotMapper snapshotMapper,
                                  ObjectMapper objectMapper) {
        this(seatMapper, campaignMapper, lineItemMapper, supplyPathMapper, snapshotMapper, objectMapper,
                Clock.systemDefaultZone());
    }

    /**
     * 执行 ProgrammaticDspService 流程，围绕 programmatic dsp service 完成校验、计算或结果组装。
     *
     * @param seatMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param campaignMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param lineItemMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param supplyPathMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param snapshotMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param objectMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param clock 时间参数，用于计算窗口、过期或审计时间。
     */
    ProgrammaticDspService(ProgrammaticDspSeatMapper seatMapper,
                           ProgrammaticDspCampaignMapper campaignMapper,
                           ProgrammaticDspLineItemMapper lineItemMapper,
                           ProgrammaticDspSupplyPathMapper supplyPathMapper,
                           ProgrammaticDspPerformanceSnapshotMapper snapshotMapper,
                           ObjectMapper objectMapper,
                           Clock clock) {
        this.seatMapper = seatMapper;
        this.campaignMapper = campaignMapper;
        this.lineItemMapper = lineItemMapper;
        this.supplyPathMapper = supplyPathMapper;
        this.snapshotMapper = snapshotMapper;
        this.objectMapper = objectMapper == null ? new ObjectMapper() : objectMapper;
        this.clock = clock == null ? Clock.systemDefaultZone() : clock;
    }

    /**
     * 新增或更新租户内 DSP seat。
     * 以 Provider 和 seatKey 唯一定位，保存账户、币种、时区、供应链策略和启用状态。
     */
    public ProgrammaticDspSeatView upsertSeat(Long tenantId,
                                              ProgrammaticDspSeatCommand command,
                                              String actor) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (command == null) {
            throw new IllegalArgumentException("programmatic DSP seat command is required");
        }
        Long scopedTenantId = normalizeTenant(tenantId);
        String provider = normalizeUpper(command.provider(), "provider");
        String seatKey = required(command.seatKey(), "seatKey");
        LocalDateTime changedAt = now();
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        ProgrammaticDspSeatDO row = seatMapper.selectOne(new LambdaQueryWrapper<ProgrammaticDspSeatDO>()
                .eq(ProgrammaticDspSeatDO::getTenantId, scopedTenantId)
                .eq(ProgrammaticDspSeatDO::getProvider, provider)
                .eq(ProgrammaticDspSeatDO::getSeatKey, seatKey)
                .last("LIMIT 1"));
        if (row == null) {
            row = new ProgrammaticDspSeatDO();
            row.setTenantId(scopedTenantId);
            row.setProvider(provider);
            row.setSeatKey(seatKey);
            row.setCreatedBy(actor(actor));
            row.setCreatedAt(changedAt);
        }
        row.setDisplayName(defaultString(command.displayName(), seatKey));
        row.setAdvertiserAccountId(trimToNull(command.advertiserAccountId()));
        row.setCurrency(normalizeUpper(defaultString(command.currency(), "CNY"), "currency"));
        row.setTimezone(defaultString(command.timezone(), "UTC"));
        row.setSupplyChainEnforcement(normalizeUpper(
                defaultString(command.supplyChainEnforcement(), "MONITOR"), "supplyChainEnforcement"));
        row.setEnabled(Boolean.FALSE.equals(command.enabled()) ? 0 : 1);
        row.setMetadataJson(json(command.metadata()));
        row.setUpdatedAt(changedAt);
        if (row.getId() == null) {
            seatMapper.insert(row);
        } else {
            seatMapper.updateById(row);
        }
        // 汇总前面计算出的状态和明细，返回给调用方。
        return toSeatView(row);
    }

    /**
     * 新增或更新租户内程序化广告活动。
     * 以 campaignKey 唯一定位，保存目标、预算、周期和状态，不调用外部 DSP。
     */
    public ProgrammaticDspCampaignView upsertCampaign(Long tenantId,
                                                      ProgrammaticDspCampaignCommand command,
                                                      String actor) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (command == null) {
            throw new IllegalArgumentException("programmatic DSP campaign command is required");
        }
        Long scopedTenantId = normalizeTenant(tenantId);
        String campaignKey = required(command.campaignKey(), "campaignKey");
        LocalDateTime changedAt = now();
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        ProgrammaticDspCampaignDO row = campaignMapper.selectOne(new LambdaQueryWrapper<ProgrammaticDspCampaignDO>()
                .eq(ProgrammaticDspCampaignDO::getTenantId, scopedTenantId)
                .eq(ProgrammaticDspCampaignDO::getCampaignKey, campaignKey)
                .last("LIMIT 1"));
        if (row == null) {
            row = new ProgrammaticDspCampaignDO();
            row.setTenantId(scopedTenantId);
            row.setCampaignKey(campaignKey);
            row.setCreatedBy(actor(actor));
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
     * 新增或更新租户内 DSP line item。
     * 会校验 seat 和 campaign 归属租户，保存出价、预算、频控、定向和状态。
     */
    public ProgrammaticDspLineItemView upsertLineItem(Long tenantId,
                                                      ProgrammaticDspLineItemCommand command,
                                                      String actor) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (command == null) {
            throw new IllegalArgumentException("programmatic DSP line item command is required");
        }
        Long scopedTenantId = normalizeTenant(tenantId);
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        ProgrammaticDspSeatDO seat = seatMapper.selectById(requiredId(command.seatId(), "seatId"));
        validateTenant(scopedTenantId, seat == null ? null : seat.getTenantId(), "seat");
        ProgrammaticDspCampaignDO campaign = campaignMapper.selectById(requiredId(command.campaignId(), "campaignId"));
        validateTenant(scopedTenantId, campaign == null ? null : campaign.getTenantId(), "campaign");
        String lineItemKey = required(command.lineItemKey(), "lineItemKey");
        LocalDateTime changedAt = now();
        ProgrammaticDspLineItemDO row = lineItemMapper.selectOne(new LambdaQueryWrapper<ProgrammaticDspLineItemDO>()
                .eq(ProgrammaticDspLineItemDO::getTenantId, scopedTenantId)
                .eq(ProgrammaticDspLineItemDO::getCampaignId, command.campaignId())
                .eq(ProgrammaticDspLineItemDO::getLineItemKey, lineItemKey)
                .last("LIMIT 1"));
        if (row == null) {
            row = new ProgrammaticDspLineItemDO();
            row.setTenantId(scopedTenantId);
            row.setCampaignId(command.campaignId());
            row.setLineItemKey(lineItemKey);
            row.setCreatedBy(actor(actor));
            row.setCreatedAt(changedAt);
        }
        row.setSeatId(command.seatId());
        row.setLineItemName(defaultString(command.lineItemName(), lineItemKey));
        row.setBidStrategy(normalizeUpper(defaultString(command.bidStrategy(), "MANUAL_CPM"), "bidStrategy"));
        row.setMaxBidCpm(money(command.maxBidCpm()));
        row.setDailyBudgetAmount(money(command.dailyBudgetAmount()));
        row.setTotalBudgetAmount(money(command.totalBudgetAmount()));
        row.setPacingMode(normalizeUpper(defaultString(command.pacingMode(), "EVEN"), "pacingMode"));
        row.setTargetingJson(json(command.targeting()));
        row.setFrequencyCap(nonNegative(command.frequencyCap()));
        row.setStatus(normalizeUpper(defaultString(command.status(), "DRAFT"), "status"));
        row.setMetadataJson(json(command.metadata()));
        row.setUpdatedAt(changedAt);
        if (row.getId() == null) {
            lineItemMapper.insert(row);
        } else {
            lineItemMapper.updateById(row);
        }
        // 汇总前面计算出的状态和明细，返回给调用方。
        return toLineItemView(row);
    }

    /**
     * 新增或更新 line item 的供应路径。
     * 以 exchange、dealId 和 sellerId 唯一定位，保存 ads.txt、sellers.json 和 schain 状态用于供应链治理。
     */
    public ProgrammaticDspSupplyPathView upsertSupplyPath(Long tenantId,
                                                          ProgrammaticDspSupplyPathCommand command,
                                                          String actor) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (command == null) {
            throw new IllegalArgumentException("programmatic DSP supply path command is required");
        }
        Long scopedTenantId = normalizeTenant(tenantId);
        ProgrammaticDspLineItemDO lineItem = tenantLineItem(scopedTenantId, command.lineItemId());
        String exchangeKey = normalizeUpper(command.exchangeKey(), "exchangeKey");
        String dealId = defaultString(command.dealId(), "");
        String sellerId = defaultString(command.sellerId(), "");
        LocalDateTime changedAt = now();
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        ProgrammaticDspSupplyPathDO row = supplyPathMapper.selectOne(new LambdaQueryWrapper<ProgrammaticDspSupplyPathDO>()
                .eq(ProgrammaticDspSupplyPathDO::getTenantId, scopedTenantId)
                .eq(ProgrammaticDspSupplyPathDO::getLineItemId, lineItem.getId())
                .eq(ProgrammaticDspSupplyPathDO::getExchangeKey, exchangeKey)
                .eq(ProgrammaticDspSupplyPathDO::getDealId, dealId)
                .eq(ProgrammaticDspSupplyPathDO::getSellerId, sellerId)
                .last("LIMIT 1"));
        if (row == null) {
            row = new ProgrammaticDspSupplyPathDO();
            row.setTenantId(scopedTenantId);
            row.setLineItemId(lineItem.getId());
            row.setExchangeKey(exchangeKey);
            row.setDealId(dealId);
            row.setSellerId(sellerId);
            row.setCreatedBy(actor(actor));
            row.setCreatedAt(changedAt);
        }
        row.setSellerDomain(trimToNull(command.sellerDomain()));
        row.setInventoryType(normalizeOptionalUpper(command.inventoryType()));
        row.setAdsTxtStatus(normalizeUpper(defaultString(command.adsTxtStatus(), "UNKNOWN"), "adsTxtStatus"));
        row.setSellersJsonStatus(normalizeUpper(defaultString(command.sellersJsonStatus(), "UNKNOWN"),
                "sellersJsonStatus"));
        row.setSchainComplete(Boolean.TRUE.equals(command.schainComplete()) ? 1 : 0);
        row.setStatus(normalizeUpper(defaultString(command.status(), "ACTIVE"), "status"));
        row.setMetadataJson(json(command.metadata()));
        row.setUpdatedAt(changedAt);
        if (row.getId() == null) {
            supplyPathMapper.insert(row);
        } else {
            supplyPathMapper.updateById(row);
        }
        // 汇总前面计算出的状态和明细，返回给调用方。
        return toSupplyPathView(row);
    }

    /**
     * 记录 DSP line item 的日度表现快照。
     * 同一租户、seat、campaign、line item 和日期会被 upsert，保存竞价、曝光、点击、转化、花费和收入指标。
     */
    public ProgrammaticDspSnapshotView recordSnapshot(Long tenantId,
                                                      ProgrammaticDspSnapshotCommand command,
                                                      String actor) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (command == null) {
            throw new IllegalArgumentException("programmatic DSP snapshot command is required");
        }
        Long scopedTenantId = normalizeTenant(tenantId);
        ProgrammaticDspLineItemDO lineItem = tenantLineItem(scopedTenantId, command.lineItemId());
        LocalDate snapshotDate = command.snapshotDate() == null ? LocalDate.now(clock) : command.snapshotDate();
        LocalDateTime changedAt = now();
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        ProgrammaticDspPerformanceSnapshotDO row = snapshotMapper.selectOne(
                new LambdaQueryWrapper<ProgrammaticDspPerformanceSnapshotDO>()
                        .eq(ProgrammaticDspPerformanceSnapshotDO::getTenantId, scopedTenantId)
                        .eq(ProgrammaticDspPerformanceSnapshotDO::getSeatId, lineItem.getSeatId())
                        .eq(ProgrammaticDspPerformanceSnapshotDO::getCampaignId, lineItem.getCampaignId())
                        .eq(ProgrammaticDspPerformanceSnapshotDO::getLineItemId, lineItem.getId())
                        .eq(ProgrammaticDspPerformanceSnapshotDO::getSnapshotDate, snapshotDate)
                        .last("LIMIT 1"));
        if (row == null) {
            row = new ProgrammaticDspPerformanceSnapshotDO();
            row.setTenantId(scopedTenantId);
            row.setSeatId(lineItem.getSeatId());
            row.setCampaignId(lineItem.getCampaignId());
            row.setLineItemId(lineItem.getId());
            row.setSnapshotDate(snapshotDate);
            row.setCreatedBy(actor(actor));
            row.setCreatedAt(changedAt);
        }
        row.setBidCount(nonNegative(command.bidCount()));
        row.setWinCount(nonNegative(command.winCount()));
        row.setImpressionCount(nonNegative(command.impressionCount()));
        row.setClickCount(nonNegative(command.clickCount()));
        row.setConversionCount(nonNegative(command.conversionCount()));
        row.setViewableImpressionCount(nonNegative(command.viewableImpressionCount()));
        row.setSpendAmount(money(command.spendAmount()));
        row.setRevenueAmount(money(command.revenueAmount()));
        row.setMetadataJson(json(command.metadata()));
        row.setUpdatedAt(changedAt);
        if (row.getId() == null) {
            snapshotMapper.insert(row);
        } else {
            snapshotMapper.updateById(row);
        }
        // 汇总前面计算出的状态和明细，返回给调用方。
        return toSnapshotView(row);
    }

    /**
     * 汇总租户内 DSP 表现。
     * 可按 seat、campaign、line item 和日期范围过滤，聚合快照并计算预算、花费、收入等派生指标。
     */
    public ProgrammaticDspSummaryView summary(Long tenantId, ProgrammaticDspSummaryQuery query) {
        if (query == null) {
            throw new IllegalArgumentException("programmatic DSP summary query is required");
        }
        Long scopedTenantId = normalizeTenant(tenantId);
        ProgrammaticDspLineItemDO lineItem = query.lineItemId() == null
                ? null
                /**
                 * 执行 tenantLineItem 流程，围绕 tenant line item 完成校验、计算或结果组装。
                 *
                 * @param scopedTenantId 业务对象 ID，用于定位具体记录。
                 * @return 返回 tenantLineItem 流程生成的业务结果。
                 */
                : tenantLineItem(scopedTenantId, query.lineItemId());
        ProgrammaticDspCampaignDO campaign = campaignForSummary(scopedTenantId, query, lineItem);
        List<ProgrammaticDspPerformanceSnapshotDO> snapshots = scopedSnapshots(scopedTenantId, query);
        long bids = 0L;
        long wins = 0L;
        long impressions = 0L;
        long clicks = 0L;
        long conversions = 0L;
        long viewableImpressions = 0L;
        BigDecimal spend = ZERO;
        BigDecimal revenue = ZERO;
        for (ProgrammaticDspPerformanceSnapshotDO snapshot : snapshots) {
            bids += nonNegative(snapshot.getBidCount());
            wins += nonNegative(snapshot.getWinCount());
            impressions += nonNegative(snapshot.getImpressionCount());
            clicks += nonNegative(snapshot.getClickCount());
            conversions += nonNegative(snapshot.getConversionCount());
            viewableImpressions += nonNegative(snapshot.getViewableImpressionCount());
            spend = spend.add(money(snapshot.getSpendAmount()));
            revenue = revenue.add(money(snapshot.getRevenueAmount()));
        }
        BigDecimal budget = budgetAmount(lineItem, campaign);
        return new ProgrammaticDspSummaryView(
                scopedTenantId,
                query.seatId(),
                query.campaignId(),
                query.lineItemId(),
                query.startDate(),
                query.endDate(),
                snapshots.size(),
                bids,
                wins,
                impressions,
                clicks,
                conversions,
                viewableImpressions,
                spend,
                revenue,
                budget,
                divide(BigDecimal.valueOf(wins), BigDecimal.valueOf(bids)),
                divide(BigDecimal.valueOf(clicks), BigDecimal.valueOf(impressions)),
                divide(BigDecimal.valueOf(conversions), BigDecimal.valueOf(clicks)),
                divide(spend, BigDecimal.valueOf(conversions)),
                divide(revenue, spend),
                divide(BigDecimal.valueOf(viewableImpressions), BigDecimal.valueOf(impressions)),
                divide(spend, budget),
                pacingStatus(campaign, budget, spend, query.evaluatedAt()),
                query.evaluatedAt());
    }

    /**
     * 执行 scopedSnapshots 流程，围绕 scoped snapshots 完成校验、计算或结果组装。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param query query 参数，用于 scopedSnapshots 流程中的校验、计算或对象转换。
     * @return 返回 scoped snapshots 汇总后的集合、分页或映射视图。
     */
    private List<ProgrammaticDspPerformanceSnapshotDO> scopedSnapshots(Long tenantId,
                                                                       ProgrammaticDspSummaryQuery query) {
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        return safeList(snapshotMapper.selectList(new LambdaQueryWrapper<ProgrammaticDspPerformanceSnapshotDO>()
                .eq(ProgrammaticDspPerformanceSnapshotDO::getTenantId, tenantId)
                .eq(query.seatId() != null, ProgrammaticDspPerformanceSnapshotDO::getSeatId, query.seatId())
                .eq(query.campaignId() != null, ProgrammaticDspPerformanceSnapshotDO::getCampaignId, query.campaignId())
                .eq(query.lineItemId() != null, ProgrammaticDspPerformanceSnapshotDO::getLineItemId, query.lineItemId())
                .ge(query.startDate() != null, ProgrammaticDspPerformanceSnapshotDO::getSnapshotDate, query.startDate())
                // 遍历候选数据并按业务规则筛选、转换或聚合。
                .le(query.endDate() != null, ProgrammaticDspPerformanceSnapshotDO::getSnapshotDate, query.endDate()))).stream()
                .filter(row -> tenantId.equals(row.getTenantId()))
                .filter(row -> query.seatId() == null || Objects.equals(query.seatId(), row.getSeatId()))
                .filter(row -> query.campaignId() == null || Objects.equals(query.campaignId(), row.getCampaignId()))
                .filter(row -> query.lineItemId() == null || Objects.equals(query.lineItemId(), row.getLineItemId()))
                .filter(row -> query.startDate() == null
                        || (row.getSnapshotDate() != null && !row.getSnapshotDate().isBefore(query.startDate())))
                .filter(row -> query.endDate() == null
                        || (row.getSnapshotDate() != null && !row.getSnapshotDate().isAfter(query.endDate())))
                .toList();
    }

    /**
     * 执行 campaignForSummary 流程，围绕 campaign for summary 完成校验、计算或结果组装。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param query query 参数，用于 campaignForSummary 流程中的校验、计算或对象转换。
     * @param lineItem line item 参数，用于 campaignForSummary 流程中的校验、计算或对象转换。
     * @return 返回 campaignForSummary 流程生成的业务结果。
     */
    private ProgrammaticDspCampaignDO campaignForSummary(Long tenantId,
                                                         ProgrammaticDspSummaryQuery query,
                                                         ProgrammaticDspLineItemDO lineItem) {
        Long campaignId = query.campaignId() == null && lineItem != null
                ? lineItem.getCampaignId()
                : query.campaignId();
        if (campaignId == null) {
            return null;
        }
        ProgrammaticDspCampaignDO campaign = campaignMapper.selectById(campaignId);
        validateTenant(tenantId, campaign == null ? null : campaign.getTenantId(), "campaign");
        return campaign;
    }

    /**
     * 执行 tenantLineItem 流程，围绕 tenant line item 完成校验、计算或结果组装。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param lineItemId 业务对象 ID，用于定位具体记录。
     * @return 返回 tenantLineItem 流程生成的业务结果。
     */
    private ProgrammaticDspLineItemDO tenantLineItem(Long tenantId, Long lineItemId) {
        ProgrammaticDspLineItemDO lineItem = lineItemMapper.selectById(requiredId(lineItemId, "lineItemId"));
        validateTenant(tenantId, lineItem == null ? null : lineItem.getTenantId(), "line item");
        return lineItem;
    }

    /**
     * 执行 budgetAmount 流程，围绕 budget amount 完成校验、计算或结果组装。
     *
     * @param lineItem line item 参数，用于 budgetAmount 流程中的校验、计算或对象转换。
     * @param campaign campaign 参数，用于 budgetAmount 流程中的校验、计算或对象转换。
     * @return 返回 budget amount 计算得到的数量、金额或指标值。
     */
    private BigDecimal budgetAmount(ProgrammaticDspLineItemDO lineItem, ProgrammaticDspCampaignDO campaign) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (lineItem != null) {
            BigDecimal totalBudget = money(lineItem.getTotalBudgetAmount());
            if (totalBudget.compareTo(ZERO) > 0) {
                return totalBudget;
            }
            BigDecimal dailyBudget = money(lineItem.getDailyBudgetAmount());
            if (dailyBudget.compareTo(ZERO) > 0) {
                return dailyBudget;
            }
        }
        // 汇总前面计算出的状态和明细，返回给调用方。
        return campaign == null ? ZERO : money(campaign.getBudgetAmount());
    }

    /**
     * 执行 pacingStatus 流程，围绕 pacing status 完成校验、计算或结果组装。
     *
     * @param campaign campaign 参数，用于 pacingStatus 流程中的校验、计算或对象转换。
     * @param budget budget 参数，用于 pacingStatus 流程中的校验、计算或对象转换。
     * @param spend spend 参数，用于 pacingStatus 流程中的校验、计算或对象转换。
     * @param evaluatedAt 时间参数，用于计算窗口、过期或审计时间。
     * @return 返回 pacing status 生成的文本或业务键。
     */
    private String pacingStatus(ProgrammaticDspCampaignDO campaign,
                                BigDecimal budget,
                                BigDecimal spend,
                                LocalDateTime evaluatedAt) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (campaign == null
                || campaign.getStartAt() == null
                || campaign.getEndAt() == null
                || evaluatedAt == null
                || budget == null
                || budget.compareTo(ZERO) <= 0
                || !campaign.getEndAt().isAfter(campaign.getStartAt())) {
            return "UNKNOWN";
        }
        double totalMillis = Duration.between(campaign.getStartAt(), campaign.getEndAt()).toMillis();
        if (totalMillis <= 0) {
            return "UNKNOWN";
        }
        double elapsedRatio = Duration.between(campaign.getStartAt(), evaluatedAt).toMillis() / totalMillis;
        elapsedRatio = Math.max(0.0, Math.min(1.0, elapsedRatio));
        double spendRatio = spend == null ? 0.0 : spend.divide(budget, 8, RoundingMode.HALF_UP).doubleValue();
        if (spendRatio < elapsedRatio - 0.10) {
            return "UNDER_PACING";
        }
        if (spendRatio > elapsedRatio + 0.10) {
            return "OVER_PACING";
        }
        // 汇总前面计算出的状态和明细，返回给调用方。
        return "ON_TRACK";
    }

    /**
     * 转换为接口返回或领域视图。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @return 返回组装或转换后的结果对象。
     */
    private ProgrammaticDspSeatView toSeatView(ProgrammaticDspSeatDO row) {
        return new ProgrammaticDspSeatView(
                row.getId(),
                row.getTenantId(),
                row.getProvider(),
                row.getSeatKey(),
                row.getDisplayName(),
                row.getAdvertiserAccountId(),
                row.getCurrency(),
                row.getTimezone(),
                row.getSupplyChainEnforcement(),
                Integer.valueOf(1).equals(row.getEnabled()),
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
    private ProgrammaticDspCampaignView toCampaignView(ProgrammaticDspCampaignDO row) {
        return new ProgrammaticDspCampaignView(
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
    private ProgrammaticDspLineItemView toLineItemView(ProgrammaticDspLineItemDO row) {
        // 汇总前面计算出的状态和明细，返回给调用方。
        return new ProgrammaticDspLineItemView(
                row.getId(),
                row.getTenantId(),
                row.getSeatId(),
                row.getCampaignId(),
                row.getLineItemKey(),
                row.getLineItemName(),
                row.getBidStrategy(),
                money(row.getMaxBidCpm()),
                money(row.getDailyBudgetAmount()),
                money(row.getTotalBudgetAmount()),
                row.getPacingMode(),
                map(row.getTargetingJson()),
                row.getFrequencyCap(),
                row.getStatus(),
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
    private ProgrammaticDspSupplyPathView toSupplyPathView(ProgrammaticDspSupplyPathDO row) {
        return new ProgrammaticDspSupplyPathView(
                row.getId(),
                row.getTenantId(),
                row.getLineItemId(),
                row.getExchangeKey(),
                row.getDealId(),
                row.getSellerId(),
                row.getSellerDomain(),
                row.getInventoryType(),
                row.getAdsTxtStatus(),
                row.getSellersJsonStatus(),
                Integer.valueOf(1).equals(row.getSchainComplete()),
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
    private ProgrammaticDspSnapshotView toSnapshotView(ProgrammaticDspPerformanceSnapshotDO row) {
        // 汇总前面计算出的状态和明细，返回给调用方。
        return new ProgrammaticDspSnapshotView(
                row.getId(),
                row.getTenantId(),
                row.getSeatId(),
                row.getCampaignId(),
                row.getLineItemId(),
                row.getSnapshotDate(),
                nonNegative(row.getBidCount()),
                nonNegative(row.getWinCount()),
                nonNegative(row.getImpressionCount()),
                nonNegative(row.getClickCount()),
                nonNegative(row.getConversionCount()),
                nonNegative(row.getViewableImpressionCount()),
                money(row.getSpendAmount()),
                money(row.getRevenueAmount()),
                map(row.getMetadataJson()),
                row.getCreatedBy(),
                row.getCreatedAt(),
                // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
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
     * 解析操作人标识。
     *
     * @param actor 操作人标识，用于审计和权限判断。
     * @return 返回 actor 生成的文本或业务键。
     */
    private String actor(String actor) {
        return defaultString(actor, "system");
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
     * 执行 nonNegative 流程，围绕 non negative 完成校验、计算或结果组装。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 non negative 计算得到的数量、金额或指标值。
     */
    private Integer nonNegative(Integer value) {
        return value == null || value < 0 ? 0 : value;
    }

    /**
     * 执行 money 流程，围绕 money 完成校验、计算或结果组装。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 money 计算得到的数量、金额或指标值。
     */
    private BigDecimal money(BigDecimal value) {
        return value == null || value.compareTo(ZERO) < 0 ? ZERO : value;
    }

    /**
     * 执行 divide 流程，围绕 divide 完成校验、计算或结果组装。
     *
     * @param numerator numerator 参数，用于 divide 流程中的校验、计算或对象转换。
     * @param denominator denominator 参数，用于 divide 流程中的校验、计算或对象转换。
     * @return 返回 divide 计算得到的数量、金额或指标值。
     */
    private BigDecimal divide(BigDecimal numerator, BigDecimal denominator) {
        if (numerator == null || denominator == null || denominator.compareTo(ZERO) == 0) {
            return ZERO.setScale(6, RoundingMode.HALF_UP);
        }
        return numerator.divide(denominator, 6, RoundingMode.HALF_UP);
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
            throw new IllegalArgumentException("programmatic DSP metadata is not JSON serializable", ex);
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
