package org.chovy.canvas.domain.policy;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.chovy.canvas.dal.dataobject.CustomerChannelDO;
import org.chovy.canvas.dal.dataobject.MarketingConsentDO;
import org.chovy.canvas.dal.dataobject.MarketingSuppressionDO;
import org.chovy.canvas.dal.mapper.CustomerChannelMapper;
import org.chovy.canvas.dal.mapper.MarketingConsentMapper;
import org.chovy.canvas.dal.mapper.MarketingSuppressionMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

/**
 * Operator-facing write surface for existing marketing policy tables.
 */
@Service
@RequiredArgsConstructor
public class MarketingPreferenceCenterService {

    private final MarketingConsentMapper consentMapper;
    private final CustomerChannelMapper channelMapper;
    private final MarketingSuppressionMapper suppressionMapper;

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param userId 业务对象 ID，用于定位具体记录。
     * @return 返回 report 流程生成的业务结果。
     */
    public PreferenceReport report(Long tenantId, String userId) {
        Long scopedTenantId = safeTenantId(tenantId);
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        List<ConsentRow> consents = consentMapper.selectList(new LambdaQueryWrapper<MarketingConsentDO>()
                        .eq(MarketingConsentDO::getTenantId, scopedTenantId)
                        .eq(MarketingConsentDO::getUserId, userId)
                        .orderByAsc(MarketingConsentDO::getChannel))
                // 遍历候选数据并按业务规则筛选、转换或聚合。
                .stream()
                .map(this::toConsentRow)
                .toList();
        List<ChannelRow> channels = channelMapper.selectList(new LambdaQueryWrapper<CustomerChannelDO>()
                        .eq(CustomerChannelDO::getTenantId, scopedTenantId)
                        .eq(CustomerChannelDO::getUserId, userId)
                        .orderByAsc(CustomerChannelDO::getChannel))
                .stream()
                .map(this::toChannelRow)
                .toList();
        List<SuppressionRow> suppressions = suppressionMapper.selectList(new LambdaQueryWrapper<MarketingSuppressionDO>()
                        .eq(MarketingSuppressionDO::getTenantId, scopedTenantId)
                        .eq(MarketingSuppressionDO::getUserId, userId)
                        .orderByDesc(MarketingSuppressionDO::getCreatedAt))
                .stream()
                .map(this::toSuppressionRow)
                .toList();
        PreferenceSummary summary = new PreferenceSummary(
                channels.size(),
                (int) consents.stream().filter(row -> MarketingConsentDO.OPT_IN.equalsIgnoreCase(row.consentStatus())).count(),
                (int) consents.stream().filter(row -> MarketingConsentDO.OPT_OUT.equalsIgnoreCase(row.consentStatus())).count(),
                (int) suppressions.stream().filter(row -> "ACTIVE".equals(row.state())).count(),
                (int) channels.stream().filter(ChannelRow::reachable).count());
        // 汇总前面计算出的状态和明细，返回给调用方。
        return new PreferenceReport(userId, consents, channels, suppressions, summary);
    }

    /**
     * 写入或更新业务数据，并保持关联状态一致。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param userId 业务对象 ID，用于定位具体记录。
     * @param command 命令对象，描述本次业务动作及其参数。
     * @return 返回流程执行后的业务结果。
     */
    public ConsentRow upsertConsent(Long tenantId, String userId, ConsentUpdateCommand command) {
        Long scopedTenantId = safeTenantId(tenantId);
        String channel = normalize(command.channel());
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        MarketingConsentDO row = consentMapper.selectOne(new LambdaQueryWrapper<MarketingConsentDO>()
                .eq(MarketingConsentDO::getTenantId, scopedTenantId)
                .eq(MarketingConsentDO::getUserId, userId)
                .eq(MarketingConsentDO::getChannel, channel)
                .last("LIMIT 1"));
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (row == null) {
            row = new MarketingConsentDO();
            row.setTenantId(scopedTenantId);
            row.setUserId(userId);
            row.setChannel(channel);
            row.setConsentStatus(normalizeConsentStatus(command.consentStatus()));
            row.setSource(defaultString(command.source(), "operator"));
            consentMapper.insert(row);
        } else {
            row.setConsentStatus(normalizeConsentStatus(command.consentStatus()));
            row.setSource(defaultString(command.source(), row.getSource()));
            consentMapper.updateById(row);
        }
        // 汇总前面计算出的状态和明细，返回给调用方。
        return toConsentRow(row);
    }

    /**
     * 写入或更新业务数据，并保持关联状态一致。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param userId 业务对象 ID，用于定位具体记录。
     * @param command 命令对象，描述本次业务动作及其参数。
     * @return 返回流程执行后的业务结果。
     */
    public ChannelRow upsertChannel(Long tenantId, String userId, ChannelUpdateCommand command) {
        Long scopedTenantId = safeTenantId(tenantId);
        String channel = normalize(command.channel());
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        CustomerChannelDO row = channelMapper.selectOne(new LambdaQueryWrapper<CustomerChannelDO>()
                .eq(CustomerChannelDO::getTenantId, scopedTenantId)
                .eq(CustomerChannelDO::getUserId, userId)
                .eq(CustomerChannelDO::getChannel, channel)
                .last("LIMIT 1"));
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (row == null) {
            row = new CustomerChannelDO();
            row.setTenantId(scopedTenantId);
            row.setUserId(userId);
            row.setChannel(channel);
            applyChannelUpdate(row, command);
            channelMapper.insert(row);
        } else {
            applyChannelUpdate(row, command);
            channelMapper.updateById(row);
        }
        // 汇总前面计算出的状态和明细，返回给调用方。
        return toChannelRow(row);
    }

    /**
     * 创建业务对象并完成必要的初始化。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param userId 业务对象 ID，用于定位具体记录。
     * @param command 命令对象，描述本次业务动作及其参数。
     * @return 返回 addSuppression 流程生成的业务结果。
     */
    public SuppressionRow addSuppression(Long tenantId, String userId, SuppressionCreateCommand command) {
        MarketingSuppressionDO row = new MarketingSuppressionDO();
        row.setTenantId(safeTenantId(tenantId));
        row.setUserId(userId);
        row.setChannel(normalize(command.channel()));
        row.setReason(defaultString(command.reason(), "operator"));
        row.setActive(Boolean.FALSE.equals(command.active()) ? 0 : 1);
        row.setExpiresAt(command.expiresAt());
        suppressionMapper.insert(row);
        return toSuppressionRow(row);
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param suppressionId 业务对象 ID，用于定位具体记录。
     */
    public void deactivateSuppression(Long tenantId, Long suppressionId) {
        MarketingSuppressionDO row = suppressionMapper.selectOne(new LambdaQueryWrapper<MarketingSuppressionDO>()
                .eq(MarketingSuppressionDO::getTenantId, safeTenantId(tenantId))
                .eq(MarketingSuppressionDO::getId, suppressionId)
                .last("LIMIT 1"));
        if (row == null) {
            return;
        }
        row.setActive(0);
        suppressionMapper.updateById(row);
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @param command 命令对象，描述本次业务动作及其参数。
     */
    private void applyChannelUpdate(CustomerChannelDO row, ChannelUpdateCommand command) {
        row.setAddress(defaultString(command.address(), ""));
        row.setEnabled(Boolean.FALSE.equals(command.enabled()) ? 0 : 1);
        row.setVerified(Boolean.TRUE.equals(command.verified()) ? 1 : 0);
        row.setMetadata(command.metadata());
    }

    /**
     * 组装输出结构或完成对象转换。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @return 返回组装或转换后的结果对象。
     */
    private ConsentRow toConsentRow(MarketingConsentDO row) {
        return new ConsentRow(row.getChannel(), row.getConsentStatus(), row.getSource(), row.getUpdatedAt());
    }

    /**
     * 组装输出结构或完成对象转换。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @return 返回组装或转换后的结果对象。
     */
    private ChannelRow toChannelRow(CustomerChannelDO row) {
        boolean enabled = row.getEnabled() != null && row.getEnabled() == 1;
        boolean verified = row.getVerified() != null && row.getVerified() == 1;
        boolean reachable = enabled && row.getAddress() != null && !row.getAddress().isBlank();
        return new ChannelRow(row.getChannel(), row.getAddress(), enabled, verified, reachable,
                row.getMetadata(), row.getUpdatedAt());
    }

    /**
     * 组装输出结构或完成对象转换。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @return 返回组装或转换后的结果对象。
     */
    private SuppressionRow toSuppressionRow(MarketingSuppressionDO row) {
        boolean active = row.getActive() != null && row.getActive() == 1;
        String state = suppressionState(row);
        return new SuppressionRow(row.getId(), row.getChannel(), row.getReason(), active, state,
                row.getExpiresAt(), row.getCreatedAt(), row.getUpdatedAt());
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @return 返回 suppression state 生成的文本或业务键。
     */
    private String suppressionState(MarketingSuppressionDO row) {
        if (row.getActive() == null || row.getActive() != 1) {
            return "INACTIVE";
        }
        LocalDateTime expiresAt = row.getExpiresAt();
        if (expiresAt != null && !expiresAt.isAfter(LocalDateTime.now())) {
            return "EXPIRED";
        }
        return "ACTIVE";
    }

    /**
     * 解析、归一化或保护输入值，生成安全可用的中间结果。
     *
     * @param status 业务状态，用于筛选或推进状态流转。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private String normalizeConsentStatus(String status) {
        // 准备本次处理所需的上下文和中间变量。
        String normalized = defaultString(status, MarketingConsentDO.OPT_OUT).toUpperCase(Locale.ROOT);
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if ("OPTIN".equals(normalized)) {
            return MarketingConsentDO.OPT_IN;
        }
        if ("OPTOUT".equals(normalized)) {
            return MarketingConsentDO.OPT_OUT;
        }
        if (MarketingConsentDO.OPT_IN.equals(normalized) || MarketingConsentDO.OPT_OUT.equals(normalized)) {
            // 汇总前面计算出的状态和明细，返回给调用方。
            return normalized;
        }
        throw new IllegalArgumentException("Unsupported consent status: " + status);
    }

    /**
     * 解析、归一化或保护输入值，生成安全可用的中间结果。
     *
     * @param channel channel 参数，用于 normalize 流程中的校验、计算或对象转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private String normalize(String channel) {
        return defaultString(channel, "ALL").toUpperCase(Locale.ROOT);
    }

    /**
     * 生成默认值或兜底结果，保证调用链稳定。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param fallback fallback 参数，用于 defaultString 流程中的校验、计算或对象转换。
     * @return 返回 default string 生成的文本或业务键。
     */
    private String defaultString(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @return 返回 safe tenant id 计算得到的数量、金额或指标值。
     */
    private Long safeTenantId(Long tenantId) {
        return tenantId == null ? 0L : tenantId;
    }

    /**
     * PreferenceReport 承载对应领域的业务规则、流程编排和结果转换。
     */
    public record PreferenceReport(
            String userId,
            List<ConsentRow> consents,
            List<ChannelRow> channels,
            List<SuppressionRow> suppressions,
            PreferenceSummary summary) {
    }

    /**
     * ConsentRow 承载对应领域的业务规则、流程编排和结果转换。
     */
    public record ConsentRow(String channel, String consentStatus, String source, LocalDateTime updatedAt) {
    }

    /**
     * ChannelRow 承载对应领域的业务规则、流程编排和结果转换。
     */
    public record ChannelRow(
            String channel,
            String address,
            boolean enabled,
            boolean verified,
            boolean reachable,
            String metadata,
            LocalDateTime updatedAt) {
    }

    /**
     * SuppressionRow 承载对应领域的业务规则、流程编排和结果转换。
     */
    public record SuppressionRow(
            Long id,
            String channel,
            String reason,
            boolean active,
            String state,
            LocalDateTime expiresAt,
            LocalDateTime createdAt,
            LocalDateTime updatedAt) {
    }

    /**
     * PreferenceSummary 承载对应领域的业务规则、流程编排和结果转换。
     */
    public record PreferenceSummary(
            int totalChannels,
            int optInCount,
            int optOutCount,
            int activeSuppressionCount,
            int reachableChannelCount) {
    }

    /**
     * ConsentUpdateCommand 承载对应领域的业务规则、流程编排和结果转换。
     */
    public record ConsentUpdateCommand(String channel, String consentStatus, String source) {
    }

    /**
     * ChannelUpdateCommand 承载对应领域的业务规则、流程编排和结果转换。
     */
    public record ChannelUpdateCommand(String channel, String address, Boolean enabled, Boolean verified, String metadata) {
    }

    /**
     * SuppressionCreateCommand 承载对应领域的业务规则、流程编排和结果转换。
     */
    public record SuppressionCreateCommand(String channel, String reason, Boolean active, LocalDateTime expiresAt) {
    }
}
