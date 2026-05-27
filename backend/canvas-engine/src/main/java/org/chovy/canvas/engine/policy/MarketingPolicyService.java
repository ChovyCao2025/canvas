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

/**
 * Marketing Policy 营销策略服务。
 *
 * <p>集中处理频控、静默时段、授权和抑制名单等营销合规判断，避免各节点重复实现策略。
 * <p>执行节点通过该服务获得是否允许触达的结果，并据此选择继续、跳过或失败。
 */
@Service
@RequiredArgsConstructor
public class MarketingPolicyService {

    /** 用户未配置时使用的默认时区。 */
    public static final String DEFAULT_TIMEZONE = "Asia/Shanghai";

    /** 用户画像 Mapper。 */
    private final CustomerProfileMapper profileMapper;
    /** 客户渠道 Mapper。 */
    private final CustomerChannelMapper channelMapper;
    /** 营销授权 Mapper。 */
    private final MarketingConsentMapper consentMapper;
    /** 营销抑制 Mapper。 */
    private final MarketingSuppressionMapper suppressionMapper;
    /** Redis 模板，用于频控计数。 */
    private final StringRedisTemplate redisTemplate;
    /** 可注入时钟，便于测试时间相关策略。 */
    private final Clock clock = Clock.systemDefaultZone();

    /** 判断用户在指定渠道是否满足营销授权要求。 */
    public PolicyDecision consentAllowed(String userId, String channel, boolean requireExplicitConsent) {
        String normalized = normalize(channel);
        MarketingConsentDO consent = consentMapper.selectOne(new LambdaQueryWrapper<MarketingConsentDO>()
                .eq(MarketingConsentDO::getUserId, userId)
                .eq(MarketingConsentDO::getChannel, normalized)
                .last("LIMIT 1"));
        if (consent == null) {
            // 未找到授权记录时是否放行取决于节点是否要求显式授权。
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

    /** 判断用户是否被营销抑制名单拦截。 */
    public PolicyDecision suppressionAllowed(String userId, String channel) {
        String normalized = normalize(channel);
        LocalDateTime now = LocalDateTime.now(clock);
        Long count = suppressionMapper.selectCount(new LambdaQueryWrapper<MarketingSuppressionDO>()
                .eq(MarketingSuppressionDO::getUserId, userId)
                .eq(MarketingSuppressionDO::getActive, 1)
                // 未设置过期时间表示长期抑制，设置过期时间则只在未来仍生效。
                .and(w -> w.isNull(MarketingSuppressionDO::getExpiresAt)
                        .or()
                        .gt(MarketingSuppressionDO::getExpiresAt, now))
                // channel 为空或 ALL 表示跨渠道抑制，否则只拦截当前渠道。
                .and(w -> w.isNull(MarketingSuppressionDO::getChannel)
                        .or()
                        .eq(MarketingSuppressionDO::getChannel, normalized)
                        .or()
                        .eq(MarketingSuppressionDO::getChannel, "ALL")));
        return count != null && count > 0
                ? PolicyDecision.blocked("MARKETING_SUPPRESSED", "用户命中营销抑制名单")
                : PolicyDecision.allow();
    }

    /** 判断用户指定触达渠道是否可用。 */
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

    /** 解析用户时区，缺失时返回默认时区。 */
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

    /** 判断当前时间是否落在用户静默时段之外。 */
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
        // insideWindow 为 true 表示当前在静默窗口内，因此策略返回 blocked。
        return insideWindow(now, quietStart, quietEnd)
                ? PolicyDecision.blocked("QUIET_HOURS", "当前处于用户静默时段")
                : PolicyDecision.allow();
    }

    /** 消费一次频控额度并返回是否允许触达。 */
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
            // 首次创建计数桶时设置过期时间，后续自增沿用同一窗口。
            redisTemplate.expire(key, window);
        }
        if (count != null && count > maxCount) {
            // 超限的这一次不应占用额度，回滚自增后再返回拦截。
            redisTemplate.opsForValue().decrement(key);
            return PolicyDecision.blocked("FREQUENCY_CAP_EXCEEDED", "用户触达频率超过限制");
        }
        return PolicyDecision.allow();
    }

    /** 根据频控作用域、渠道、节点和时间窗口生成 Redis 计数 key。 */
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
        // 不同 scope 决定频控维度：全局、渠道、节点或当前画布旅程。
        String dimension = switch (normalizedScope) {
            case "GLOBAL" -> "global";
            case "CHANNEL" -> "channel:" + normalize(channel);
            case "NODE" -> "node:" + nodeId;
            default -> "journey:" + canvasId;
        };
        return "canvas:marketing:freq:" + normalizedScope + ":" + dimension + ":" + userId + ":" + bucket;
    }

    /** 判断当前本地时间是否位于静默窗口内，支持跨午夜窗口。 */
    private boolean insideWindow(LocalTime current, LocalTime start, LocalTime end) {
        if (start.equals(end)) return true;
        if (start.isBefore(end)) {
            // 普通窗口：例如 09:00-18:00。
            return !current.isBefore(start) && current.isBefore(end);
        }
        // 跨午夜窗口：例如 22:00-08:00，命中晚间或次日凌晨任一段即可。
        return !current.isBefore(start) || current.isBefore(end);
    }

    /** 将渠道统一为大写编码，缺省表示全部渠道。 */
    private String normalize(String channel) {
        return channel == null || channel.isBlank() ? "ALL" : channel.toUpperCase(Locale.ROOT);
    }

    /** 策略判断结果，包含是否允许通过以及被拦截时的原因码和提示文案。 */
    public record PolicyDecision(boolean allowed, String reasonCode, String reasonMessage) {
        /** 构造允许通过的策略结果。 */
        public static PolicyDecision allow() {
            return new PolicyDecision(true, null, null);
        }

        /** 构造被策略拦截的结果。 */
        public static PolicyDecision blocked(String reasonCode, String reasonMessage) {
            return new PolicyDecision(false, reasonCode, reasonMessage);
        }
    }
}
