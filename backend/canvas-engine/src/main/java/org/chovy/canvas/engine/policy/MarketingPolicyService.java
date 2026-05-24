package org.chovy.canvas.engine.policy;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.chovy.canvas.dal.dataobject.CustomerChannelDO;
import org.chovy.canvas.dal.mapper.CustomerChannelMapper;
import org.chovy.canvas.dal.dataobject.CustomerProfileDO;
import org.chovy.canvas.dal.mapper.CustomerProfileMapper;
import org.chovy.canvas.dal.dataobject.MarketingConsentDO;
import org.chovy.canvas.dal.mapper.MarketingConsentMapper;
import org.chovy.canvas.dal.dataobject.MarketingSuppressionDO;
import org.chovy.canvas.dal.mapper.MarketingSuppressionMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class MarketingPolicyService {

    public static final String DEFAULT_TIMEZONE = "Asia/Shanghai";

    private final CustomerProfileMapper profileMapper;
    private final CustomerChannelMapper channelMapper;
    private final MarketingConsentMapper consentMapper;
    private final MarketingSuppressionMapper suppressionMapper;
    private final StringRedisTemplate redisTemplate;
    private final Clock clock = Clock.systemDefaultZone();

    public PolicyDecision consentAllowed(String userId, String channel, boolean requireExplicitConsent) {
        String normalized = normalize(channel);
        MarketingConsentDO consent = consentMapper.selectOne(new LambdaQueryWrapper<MarketingConsentDO>()
                .eq(MarketingConsentDO::getUserId, userId)
                .eq(MarketingConsentDO::getChannel, normalized)
                .last("LIMIT 1"));
        if (consent == null) {
            return requireExplicitConsent
                    ? PolicyDecision.blocked("NO_MARKETING_CONSENT", "用户未授权该渠道营销触达")
                    : PolicyDecision.allow();
        }
        if (MarketingConsentDO.OPT_OUT.equalsIgnoreCase(consent.getConsentStatus())) {
            return PolicyDecision.blocked("MARKETING_OPT_OUT", "用户已退订该渠道营销触达");
        }
        if (requireExplicitConsent && !MarketingConsentDO.OPT_IN.equalsIgnoreCase(consent.getConsentStatus())) {
            return PolicyDecision.blocked("NO_MARKETING_CONSENT", "用户未授权该渠道营销触达");
        }
        return PolicyDecision.allow();
    }

    public PolicyDecision suppressionAllowed(String userId, String channel) {
        String normalized = normalize(channel);
        LocalDateTime now = LocalDateTime.now(clock);
        Long count = suppressionMapper.selectCount(new LambdaQueryWrapper<MarketingSuppressionDO>()
                .eq(MarketingSuppressionDO::getUserId, userId)
                .eq(MarketingSuppressionDO::getActive, 1)
                .and(w -> w.isNull(MarketingSuppressionDO::getExpiresAt)
                        .or()
                        .gt(MarketingSuppressionDO::getExpiresAt, now))
                .and(w -> w.isNull(MarketingSuppressionDO::getChannel)
                        .or()
                        .eq(MarketingSuppressionDO::getChannel, normalized)
                        .or()
                        .eq(MarketingSuppressionDO::getChannel, "ALL")));
        return count != null && count > 0
                ? PolicyDecision.blocked("MARKETING_SUPPRESSED", "用户命中营销抑制名单")
                : PolicyDecision.allow();
    }

    public PolicyDecision channelAvailable(String userId, String channel) {
        String normalized = normalize(channel);
        CustomerChannelDO customerChannel = channelMapper.selectOne(new LambdaQueryWrapper<CustomerChannelDO>()
                .eq(CustomerChannelDO::getUserId, userId)
                .eq(CustomerChannelDO::getChannel, normalized)
                .eq(CustomerChannelDO::getEnabled, 1)
                .last("LIMIT 1"));
        if (customerChannel == null || customerChannel.getAddress() == null || customerChannel.getAddress().isBlank()) {
            return PolicyDecision.blocked("CHANNEL_UNAVAILABLE", "用户该渠道不可达");
        }
        return PolicyDecision.allow();
    }

    public ZoneId timezoneFor(String userId) {
        CustomerProfileDO profile = profileMapper.selectOne(new LambdaQueryWrapper<CustomerProfileDO>()
                .eq(CustomerProfileDO::getUserId, userId)
                .last("LIMIT 1"));
        String timezone = profile == null ? null : profile.getTimezone();
        if (timezone == null || timezone.isBlank()) {
            timezone = DEFAULT_TIMEZONE;
        }
        try {
            return ZoneId.of(timezone);
        } catch (Exception ignored) {
            return ZoneId.of(DEFAULT_TIMEZONE);
        }
    }

    public PolicyDecision quietHoursAllowed(String userId, String start, String end, String timezone) {
        LocalTime quietStart;
        LocalTime quietEnd;
        try {
            quietStart = LocalTime.parse(start == null || start.isBlank() ? "22:00" : start);
            quietEnd = LocalTime.parse(end == null || end.isBlank() ? "08:00" : end);
        } catch (DateTimeParseException e) {
            return PolicyDecision.blocked("INVALID_QUIET_HOURS", "静默时段格式不正确");
        }

        ZoneId zone = (timezone == null || timezone.isBlank() || "USER_LOCAL".equalsIgnoreCase(timezone))
                ? timezoneFor(userId)
                : ZoneId.of(timezone);
        LocalTime now = LocalTime.now(clock.withZone(zone));
        return insideWindow(now, quietStart, quietEnd)
                ? PolicyDecision.blocked("QUIET_HOURS", "当前处于用户静默时段")
                : PolicyDecision.allow();
    }

    public PolicyDecision consumeFrequency(
            String userId,
            Long canvasId,
            String nodeId,
            String scope,
            String channel,
            int maxCount,
            Duration window
    ) {
        if (maxCount <= 0) {
            return PolicyDecision.blocked("FREQUENCY_CAP_EXCEEDED", "频控上限为 0");
        }
        String key = frequencyKey(userId, canvasId, nodeId, scope, channel, window);
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1L) {
            redisTemplate.expire(key, window);
        }
        if (count != null && count > maxCount) {
            redisTemplate.opsForValue().decrement(key);
            return PolicyDecision.blocked("FREQUENCY_CAP_EXCEEDED", "用户触达频率超过限制");
        }
        return PolicyDecision.allow();
    }

    private String frequencyKey(
            String userId,
            Long canvasId,
            String nodeId,
            String scope,
            String channel,
            Duration window
    ) {
        long bucket = clock.millis() / Math.max(1L, window.toMillis());
        String normalizedScope = scope == null || scope.isBlank() ? "JOURNEY" : scope.toUpperCase(Locale.ROOT);
        String dimension = switch (normalizedScope) {
            case "GLOBAL" -> "global";
            case "CHANNEL" -> "channel:" + normalize(channel);
            case "NODE" -> "node:" + nodeId;
            default -> "journey:" + canvasId;
        };
        return "canvas:marketing:freq:" + normalizedScope + ":" + dimension + ":" + userId + ":" + bucket;
    }

    private boolean insideWindow(LocalTime current, LocalTime start, LocalTime end) {
        if (start.equals(end)) return true;
        if (start.isBefore(end)) {
            return !current.isBefore(start) && current.isBefore(end);
        }
        return !current.isBefore(start) || current.isBefore(end);
    }

    private String normalize(String channel) {
        return channel == null || channel.isBlank() ? "ALL" : channel.toUpperCase(Locale.ROOT);
    }

    public record PolicyDecision(boolean allowed, String reasonCode, String reasonMessage) {
        public static PolicyDecision allow() {
            return new PolicyDecision(true, null, null);
        }

        public static PolicyDecision blocked(String reasonCode, String reasonMessage) {
            return new PolicyDecision(false, reasonCode, reasonMessage);
        }
    }
}
