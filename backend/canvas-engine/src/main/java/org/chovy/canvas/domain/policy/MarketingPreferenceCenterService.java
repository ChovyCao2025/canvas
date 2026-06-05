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

    public PreferenceReport report(Long tenantId, String userId) {
        Long scopedTenantId = safeTenantId(tenantId);
        List<ConsentRow> consents = consentMapper.selectList(new LambdaQueryWrapper<MarketingConsentDO>()
                        .eq(MarketingConsentDO::getTenantId, scopedTenantId)
                        .eq(MarketingConsentDO::getUserId, userId)
                        .orderByAsc(MarketingConsentDO::getChannel))
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
        return new PreferenceReport(userId, consents, channels, suppressions, summary);
    }

    public ConsentRow upsertConsent(Long tenantId, String userId, ConsentUpdateCommand command) {
        Long scopedTenantId = safeTenantId(tenantId);
        String channel = normalize(command.channel());
        MarketingConsentDO row = consentMapper.selectOne(new LambdaQueryWrapper<MarketingConsentDO>()
                .eq(MarketingConsentDO::getTenantId, scopedTenantId)
                .eq(MarketingConsentDO::getUserId, userId)
                .eq(MarketingConsentDO::getChannel, channel)
                .last("LIMIT 1"));
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
        return toConsentRow(row);
    }

    public ChannelRow upsertChannel(Long tenantId, String userId, ChannelUpdateCommand command) {
        Long scopedTenantId = safeTenantId(tenantId);
        String channel = normalize(command.channel());
        CustomerChannelDO row = channelMapper.selectOne(new LambdaQueryWrapper<CustomerChannelDO>()
                .eq(CustomerChannelDO::getTenantId, scopedTenantId)
                .eq(CustomerChannelDO::getUserId, userId)
                .eq(CustomerChannelDO::getChannel, channel)
                .last("LIMIT 1"));
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
        return toChannelRow(row);
    }

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

    private void applyChannelUpdate(CustomerChannelDO row, ChannelUpdateCommand command) {
        row.setAddress(defaultString(command.address(), ""));
        row.setEnabled(Boolean.FALSE.equals(command.enabled()) ? 0 : 1);
        row.setVerified(Boolean.TRUE.equals(command.verified()) ? 1 : 0);
        row.setMetadata(command.metadata());
    }

    private ConsentRow toConsentRow(MarketingConsentDO row) {
        return new ConsentRow(row.getChannel(), row.getConsentStatus(), row.getSource(), row.getUpdatedAt());
    }

    private ChannelRow toChannelRow(CustomerChannelDO row) {
        boolean enabled = row.getEnabled() != null && row.getEnabled() == 1;
        boolean verified = row.getVerified() != null && row.getVerified() == 1;
        boolean reachable = enabled && row.getAddress() != null && !row.getAddress().isBlank();
        return new ChannelRow(row.getChannel(), row.getAddress(), enabled, verified, reachable,
                row.getMetadata(), row.getUpdatedAt());
    }

    private SuppressionRow toSuppressionRow(MarketingSuppressionDO row) {
        boolean active = row.getActive() != null && row.getActive() == 1;
        String state = suppressionState(row);
        return new SuppressionRow(row.getId(), row.getChannel(), row.getReason(), active, state,
                row.getExpiresAt(), row.getCreatedAt(), row.getUpdatedAt());
    }

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

    private String normalizeConsentStatus(String status) {
        String normalized = defaultString(status, MarketingConsentDO.OPT_OUT).toUpperCase(Locale.ROOT);
        if ("OPTIN".equals(normalized)) {
            return MarketingConsentDO.OPT_IN;
        }
        if ("OPTOUT".equals(normalized)) {
            return MarketingConsentDO.OPT_OUT;
        }
        if (MarketingConsentDO.OPT_IN.equals(normalized) || MarketingConsentDO.OPT_OUT.equals(normalized)) {
            return normalized;
        }
        throw new IllegalArgumentException("Unsupported consent status: " + status);
    }

    private String normalize(String channel) {
        return defaultString(channel, "ALL").toUpperCase(Locale.ROOT);
    }

    private String defaultString(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private Long safeTenantId(Long tenantId) {
        return tenantId == null ? 0L : tenantId;
    }

    public record PreferenceReport(
            String userId,
            List<ConsentRow> consents,
            List<ChannelRow> channels,
            List<SuppressionRow> suppressions,
            PreferenceSummary summary) {
    }

    public record ConsentRow(String channel, String consentStatus, String source, LocalDateTime updatedAt) {
    }

    public record ChannelRow(
            String channel,
            String address,
            boolean enabled,
            boolean verified,
            boolean reachable,
            String metadata,
            LocalDateTime updatedAt) {
    }

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

    public record PreferenceSummary(
            int totalChannels,
            int optInCount,
            int optOutCount,
            int activeSuppressionCount,
            int reachableChannelCount) {
    }

    public record ConsentUpdateCommand(String channel, String consentStatus, String source) {
    }

    public record ChannelUpdateCommand(String channel, String address, Boolean enabled, Boolean verified, String metadata) {
    }

    public record SuppressionCreateCommand(String channel, String reason, Boolean active, LocalDateTime expiresAt) {
    }
}
