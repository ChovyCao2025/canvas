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

    public ProgrammaticDspSeatView upsertSeat(Long tenantId,
                                              ProgrammaticDspSeatCommand command,
                                              String actor) {
        if (command == null) {
            throw new IllegalArgumentException("programmatic DSP seat command is required");
        }
        Long scopedTenantId = normalizeTenant(tenantId);
        String provider = normalizeUpper(command.provider(), "provider");
        String seatKey = required(command.seatKey(), "seatKey");
        LocalDateTime changedAt = now();
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
        return toSeatView(row);
    }

    public ProgrammaticDspCampaignView upsertCampaign(Long tenantId,
                                                      ProgrammaticDspCampaignCommand command,
                                                      String actor) {
        if (command == null) {
            throw new IllegalArgumentException("programmatic DSP campaign command is required");
        }
        Long scopedTenantId = normalizeTenant(tenantId);
        String campaignKey = required(command.campaignKey(), "campaignKey");
        LocalDateTime changedAt = now();
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
        return toCampaignView(row);
    }

    public ProgrammaticDspLineItemView upsertLineItem(Long tenantId,
                                                      ProgrammaticDspLineItemCommand command,
                                                      String actor) {
        if (command == null) {
            throw new IllegalArgumentException("programmatic DSP line item command is required");
        }
        Long scopedTenantId = normalizeTenant(tenantId);
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
        return toLineItemView(row);
    }

    public ProgrammaticDspSupplyPathView upsertSupplyPath(Long tenantId,
                                                          ProgrammaticDspSupplyPathCommand command,
                                                          String actor) {
        if (command == null) {
            throw new IllegalArgumentException("programmatic DSP supply path command is required");
        }
        Long scopedTenantId = normalizeTenant(tenantId);
        ProgrammaticDspLineItemDO lineItem = tenantLineItem(scopedTenantId, command.lineItemId());
        String exchangeKey = normalizeUpper(command.exchangeKey(), "exchangeKey");
        String dealId = defaultString(command.dealId(), "");
        String sellerId = defaultString(command.sellerId(), "");
        LocalDateTime changedAt = now();
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
        return toSupplyPathView(row);
    }

    public ProgrammaticDspSnapshotView recordSnapshot(Long tenantId,
                                                      ProgrammaticDspSnapshotCommand command,
                                                      String actor) {
        if (command == null) {
            throw new IllegalArgumentException("programmatic DSP snapshot command is required");
        }
        Long scopedTenantId = normalizeTenant(tenantId);
        ProgrammaticDspLineItemDO lineItem = tenantLineItem(scopedTenantId, command.lineItemId());
        LocalDate snapshotDate = command.snapshotDate() == null ? LocalDate.now(clock) : command.snapshotDate();
        LocalDateTime changedAt = now();
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
        return toSnapshotView(row);
    }

    public ProgrammaticDspSummaryView summary(Long tenantId, ProgrammaticDspSummaryQuery query) {
        if (query == null) {
            throw new IllegalArgumentException("programmatic DSP summary query is required");
        }
        Long scopedTenantId = normalizeTenant(tenantId);
        ProgrammaticDspLineItemDO lineItem = query.lineItemId() == null
                ? null
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

    private List<ProgrammaticDspPerformanceSnapshotDO> scopedSnapshots(Long tenantId,
                                                                       ProgrammaticDspSummaryQuery query) {
        return safeList(snapshotMapper.selectList(new LambdaQueryWrapper<ProgrammaticDspPerformanceSnapshotDO>()
                .eq(ProgrammaticDspPerformanceSnapshotDO::getTenantId, tenantId)
                .eq(query.seatId() != null, ProgrammaticDspPerformanceSnapshotDO::getSeatId, query.seatId())
                .eq(query.campaignId() != null, ProgrammaticDspPerformanceSnapshotDO::getCampaignId, query.campaignId())
                .eq(query.lineItemId() != null, ProgrammaticDspPerformanceSnapshotDO::getLineItemId, query.lineItemId())
                .ge(query.startDate() != null, ProgrammaticDspPerformanceSnapshotDO::getSnapshotDate, query.startDate())
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

    private ProgrammaticDspLineItemDO tenantLineItem(Long tenantId, Long lineItemId) {
        ProgrammaticDspLineItemDO lineItem = lineItemMapper.selectById(requiredId(lineItemId, "lineItemId"));
        validateTenant(tenantId, lineItem == null ? null : lineItem.getTenantId(), "line item");
        return lineItem;
    }

    private BigDecimal budgetAmount(ProgrammaticDspLineItemDO lineItem, ProgrammaticDspCampaignDO campaign) {
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
        return campaign == null ? ZERO : money(campaign.getBudgetAmount());
    }

    private String pacingStatus(ProgrammaticDspCampaignDO campaign,
                                BigDecimal budget,
                                BigDecimal spend,
                                LocalDateTime evaluatedAt) {
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
        return "ON_TRACK";
    }

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

    private ProgrammaticDspLineItemView toLineItemView(ProgrammaticDspLineItemDO row) {
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
                row.getUpdatedAt());
    }

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

    private ProgrammaticDspSnapshotView toSnapshotView(ProgrammaticDspPerformanceSnapshotDO row) {
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

    private String defaultString(String value, String fallback) {
        String trimmed = trimToNull(value);
        return trimmed == null ? fallback : trimmed;
    }

    private String actor(String actor) {
        return defaultString(actor, "system");
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

    private Integer nonNegative(Integer value) {
        return value == null || value < 0 ? 0 : value;
    }

    private BigDecimal money(BigDecimal value) {
        return value == null || value.compareTo(ZERO) < 0 ? ZERO : value;
    }

    private BigDecimal divide(BigDecimal numerator, BigDecimal denominator) {
        if (numerator == null || denominator == null || denominator.compareTo(ZERO) == 0) {
            return ZERO.setScale(6, RoundingMode.HALF_UP);
        }
        return numerator.divide(denominator, 6, RoundingMode.HALF_UP);
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }

    private String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value == null ? Map.of() : value);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("programmatic DSP metadata is not JSON serializable", ex);
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
