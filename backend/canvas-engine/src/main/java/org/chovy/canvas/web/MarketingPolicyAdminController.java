package org.chovy.canvas.web;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.chovy.canvas.common.R;
import org.chovy.canvas.dal.dataobject.CustomerChannelDO;
import org.chovy.canvas.dal.dataobject.MarketingConsentDO;
import org.chovy.canvas.dal.dataobject.MarketingSuppressionDO;
import org.chovy.canvas.dal.mapper.CustomerChannelMapper;
import org.chovy.canvas.dal.mapper.MarketingConsentMapper;
import org.chovy.canvas.dal.mapper.MarketingSuppressionMapper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@RestController
@RequestMapping("/canvas/policies")
@RequiredArgsConstructor
public class MarketingPolicyAdminController {

    private static final Long DEFAULT_TENANT_ID = 0L;
    private static final String ALL_CHANNELS = "ALL";

    private final MarketingConsentMapper consentMapper;
    private final MarketingSuppressionMapper suppressionMapper;
    private final CustomerChannelMapper channelMapper;

    public record ConsentReq(String userId, String channel, String consentStatus, String source) {
    }

    public record SuppressionReq(String userId,
                                 String channel,
                                 String reason,
                                 Boolean active,
                                 LocalDateTime expiresAt) {
    }

    public record ChannelReq(String userId,
                             String channel,
                             String address,
                             Integer enabled,
                             Integer verified,
                             String metadata) {
    }

    public record PolicyState(String userId,
                              String channel,
                              MarketingConsentDO consent,
                              List<MarketingSuppressionDO> suppressions,
                              CustomerChannelDO customerChannel) {
    }

    @GetMapping("/state")
    public Mono<R<PolicyState>> policyState(@RequestParam String userId, @RequestParam String channel) {
        return Mono.fromCallable(() -> {
            String normalizedUserId = required(userId, "userId");
            String normalizedChannel = normalizeRequired(channel, "channel");
            MarketingConsentDO consent = consentMapper.selectOne(new LambdaQueryWrapper<MarketingConsentDO>()
                    .eq(MarketingConsentDO::getUserId, normalizedUserId)
                    .eq(MarketingConsentDO::getChannel, normalizedChannel)
                    .last("LIMIT 1"));
            List<MarketingSuppressionDO> suppressions = suppressionMapper.selectList(
                    new LambdaQueryWrapper<MarketingSuppressionDO>()
                            .eq(MarketingSuppressionDO::getUserId, normalizedUserId)
                            .and(q -> q.eq(MarketingSuppressionDO::getChannel, normalizedChannel)
                                    .or()
                                    .eq(MarketingSuppressionDO::getChannel, ALL_CHANNELS)
                                    .or()
                                    .isNull(MarketingSuppressionDO::getChannel)));
            CustomerChannelDO customerChannel = channelMapper.selectOne(new LambdaQueryWrapper<CustomerChannelDO>()
                    .eq(CustomerChannelDO::getUserId, normalizedUserId)
                    .eq(CustomerChannelDO::getChannel, normalizedChannel)
                    .last("LIMIT 1"));
            return R.ok(new PolicyState(normalizedUserId, normalizedChannel,
                    consent, suppressions == null ? List.of() : suppressions, customerChannel));
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @PostMapping("/consent")
    public Mono<R<MarketingConsentDO>> upsertConsent(@RequestBody ConsentReq req) {
        return Mono.fromCallable(() -> {
            if (req == null) {
                throw new IllegalArgumentException("consent request is required");
            }
            String userId = required(req.userId(), "userId");
            String channel = normalizeRequired(req.channel(), "channel");
            MarketingConsentDO row = consentMapper.selectOne(new LambdaQueryWrapper<MarketingConsentDO>()
                    .eq(MarketingConsentDO::getUserId, userId)
                    .eq(MarketingConsentDO::getChannel, channel)
                    .last("LIMIT 1"));
            boolean insert = row == null;
            if (insert) {
                row = new MarketingConsentDO();
                row.setTenantId(DEFAULT_TENANT_ID);
            }
            row.setUserId(userId);
            row.setChannel(channel);
            row.setConsentStatus(normalizeRequired(req.consentStatus(), "consentStatus"));
            row.setSource(optional(req.source()));
            if (insert) {
                consentMapper.insert(row);
            } else {
                consentMapper.updateById(row);
            }
            return R.ok(row);
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @PostMapping("/suppression")
    public Mono<R<MarketingSuppressionDO>> upsertSuppression(@RequestBody SuppressionReq req) {
        return Mono.fromCallable(() -> {
            if (req == null) {
                throw new IllegalArgumentException("suppression request is required");
            }
            String userId = required(req.userId(), "userId");
            String channel = normalizeChannelOrAll(req.channel());
            String reason = required(req.reason(), "reason");
            MarketingSuppressionDO row = suppressionMapper.selectOne(new LambdaQueryWrapper<MarketingSuppressionDO>()
                    .eq(MarketingSuppressionDO::getUserId, userId)
                    .eq(MarketingSuppressionDO::getChannel, channel)
                    .eq(MarketingSuppressionDO::getReason, reason)
                    .last("LIMIT 1"));
            boolean insert = row == null;
            if (insert) {
                row = new MarketingSuppressionDO();
                row.setTenantId(DEFAULT_TENANT_ID);
            }
            row.setUserId(userId);
            row.setChannel(channel);
            row.setReason(reason);
            row.setActive(Boolean.FALSE.equals(req.active()) ? 0 : 1);
            row.setExpiresAt(req.expiresAt());
            if (insert) {
                suppressionMapper.insert(row);
            } else {
                suppressionMapper.updateById(row);
            }
            return R.ok(row);
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @PostMapping("/channel")
    public Mono<R<CustomerChannelDO>> upsertChannel(@RequestBody ChannelReq req) {
        return Mono.fromCallable(() -> {
            if (req == null) {
                throw new IllegalArgumentException("channel request is required");
            }
            String userId = required(req.userId(), "userId");
            String channel = normalizeRequired(req.channel(), "channel");
            CustomerChannelDO row = channelMapper.selectOne(new LambdaQueryWrapper<CustomerChannelDO>()
                    .eq(CustomerChannelDO::getUserId, userId)
                    .eq(CustomerChannelDO::getChannel, channel)
                    .last("LIMIT 1"));
            boolean insert = row == null;
            if (insert) {
                row = new CustomerChannelDO();
                row.setTenantId(DEFAULT_TENANT_ID);
            }
            row.setUserId(userId);
            row.setChannel(channel);
            row.setAddress(optional(req.address()));
            row.setEnabled(req.enabled() == null ? 1 : req.enabled());
            row.setVerified(req.verified() == null ? 0 : req.verified());
            row.setMetadata(optional(req.metadata()));
            if (insert) {
                channelMapper.insert(row);
            } else {
                channelMapper.updateById(row);
            }
            return R.ok(row);
        }).subscribeOn(Schedulers.boundedElastic());
    }

    private String normalizeChannelOrAll(String value) {
        return value == null || value.isBlank() ? ALL_CHANNELS : normalizeRequired(value, "channel");
    }

    private String normalizeRequired(String value, String field) {
        return required(value, field).toUpperCase(Locale.ROOT);
    }

    private String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }

    private String optional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
