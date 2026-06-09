package org.chovy.canvas.web;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.chovy.canvas.common.R;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
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

/**
 * MarketingPolicyAdminController 业务组件。
 */
@RestController
@RequestMapping("/canvas/policies")
@RequiredArgsConstructor
public class MarketingPolicyAdminController {

    private static final Long DEFAULT_TENANT_ID = 0L;
    private static final String ALL_CHANNELS = "ALL";

    private final MarketingConsentMapper consentMapper;
    private final MarketingSuppressionMapper suppressionMapper;
    private final CustomerChannelMapper channelMapper;
    private final TenantContextResolver tenantContextResolver;

    /**
     * ConsentReq 数据记录。
     */
    public record ConsentReq(String userId, String channel, String consentStatus, String source) {
    }

    /**
     * SuppressionReq 数据记录。
     */
    public record SuppressionReq(String userId,
                                 String channel,
                                 String reason,
                                 Boolean active,
                                 LocalDateTime expiresAt) {
    }

    /**
     * ChannelReq 数据记录。
     */
    public record ChannelReq(String userId,
                             String channel,
                             String address,
                             Integer enabled,
                             Integer verified,
                             String metadata) {
    }

    /**
     * PolicyState 数据记录。
     */
    public record PolicyState(String userId,
                              String channel,
                              MarketingConsentDO consent,
                              List<MarketingSuppressionDO> suppressions,
                              CustomerChannelDO customerChannel) {
    }

    /**
     * 执行 policyState 流程，围绕 policy state 完成校验、计算或结果组装。
     *
     * @param userId 业务对象 ID，用于定位具体记录。
     * @param channel channel 参数，用于 policyState 流程中的校验、计算或对象转换。
     * @return 返回 policyState 流程生成的业务结果。
     */
    @GetMapping("/state")
    public Mono<R<PolicyState>> policyState(@RequestParam String userId, @RequestParam String channel) {
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        return currentTenant().flatMap(context -> Mono.fromCallable(() -> {
            Long tenantId = tenantId(context);
            String normalizedUserId = required(userId, "userId");
            String normalizedChannel = normalizeRequired(channel, "channel");
            // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
            MarketingConsentDO consent = consentMapper.selectOne(new LambdaQueryWrapper<MarketingConsentDO>()
                    .eq(MarketingConsentDO::getTenantId, tenantId)
                    .eq(MarketingConsentDO::getUserId, normalizedUserId)
                    .eq(MarketingConsentDO::getChannel, normalizedChannel)
                    .last("LIMIT 1"));
            List<MarketingSuppressionDO> suppressions = suppressionMapper.selectList(
                    new LambdaQueryWrapper<MarketingSuppressionDO>()
                            .eq(MarketingSuppressionDO::getTenantId, tenantId)
                            .eq(MarketingSuppressionDO::getUserId, normalizedUserId)
                            .and(q -> q.eq(MarketingSuppressionDO::getChannel, normalizedChannel)
                                    .or()
                                    .eq(MarketingSuppressionDO::getChannel, ALL_CHANNELS)
                                    .or()
                                    .isNull(MarketingSuppressionDO::getChannel)));
            CustomerChannelDO customerChannel = channelMapper.selectOne(new LambdaQueryWrapper<CustomerChannelDO>()
                    .eq(CustomerChannelDO::getTenantId, tenantId)
                    .eq(CustomerChannelDO::getUserId, normalizedUserId)
                    .eq(CustomerChannelDO::getChannel, normalizedChannel)
                    .last("LIMIT 1"));
            // 汇总前面计算出的状态和明细，返回给调用方。
            return R.ok(new PolicyState(normalizedUserId, normalizedChannel,
                    consent, suppressions == null ? List.of() : suppressions, customerChannel));
        }).subscribeOn(Schedulers.boundedElastic()));
    }

    /**
     * 执行数据写入或状态变更。
     *
     * @param req 请求对象，承载本次操作的输入参数。
     * @return 返回流程执行后的业务结果。
     */
    @PostMapping("/consent")
    public Mono<R<MarketingConsentDO>> upsertConsent(@RequestBody ConsentReq req) {
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        return currentTenant().flatMap(context -> Mono.fromCallable(() -> {
            // 校验关键输入和前置条件，避免无效状态继续进入主流程。
            if (req == null) {
                throw new IllegalArgumentException("consent request is required");
            }
            Long tenantId = tenantId(context);
            String userId = required(req.userId(), "userId");
            String channel = normalizeRequired(req.channel(), "channel");
            // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
            MarketingConsentDO row = consentMapper.selectOne(new LambdaQueryWrapper<MarketingConsentDO>()
                    .eq(MarketingConsentDO::getTenantId, tenantId)
                    .eq(MarketingConsentDO::getUserId, userId)
                    .eq(MarketingConsentDO::getChannel, channel)
                    .last("LIMIT 1"));
            boolean insert = row == null;
            if (insert) {
                row = new MarketingConsentDO();
            }
            row.setTenantId(tenantId);
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
        }).subscribeOn(Schedulers.boundedElastic()));
    }

    /**
     * 执行数据写入或状态变更。
     *
     * @param req 请求对象，承载本次操作的输入参数。
     * @return 返回流程执行后的业务结果。
     */
    @PostMapping("/suppression")
    public Mono<R<MarketingSuppressionDO>> upsertSuppression(@RequestBody SuppressionReq req) {
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        return currentTenant().flatMap(context -> Mono.fromCallable(() -> {
            // 校验关键输入和前置条件，避免无效状态继续进入主流程。
            if (req == null) {
                throw new IllegalArgumentException("suppression request is required");
            }
            Long tenantId = tenantId(context);
            String userId = required(req.userId(), "userId");
            String channel = normalizeChannelOrAll(req.channel());
            String reason = required(req.reason(), "reason");
            // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
            MarketingSuppressionDO row = suppressionMapper.selectOne(new LambdaQueryWrapper<MarketingSuppressionDO>()
                    .eq(MarketingSuppressionDO::getTenantId, tenantId)
                    .eq(MarketingSuppressionDO::getUserId, userId)
                    .eq(MarketingSuppressionDO::getChannel, channel)
                    .eq(MarketingSuppressionDO::getReason, reason)
                    .last("LIMIT 1"));
            boolean insert = row == null;
            if (insert) {
                row = new MarketingSuppressionDO();
            }
            row.setTenantId(tenantId);
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
        }).subscribeOn(Schedulers.boundedElastic()));
    }

    /**
     * 执行数据写入或状态变更。
     *
     * @param req 请求对象，承载本次操作的输入参数。
     * @return 返回流程执行后的业务结果。
     */
    @PostMapping("/channel")
    public Mono<R<CustomerChannelDO>> upsertChannel(@RequestBody ChannelReq req) {
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        return currentTenant().flatMap(context -> Mono.fromCallable(() -> {
            // 校验关键输入和前置条件，避免无效状态继续进入主流程。
            if (req == null) {
                throw new IllegalArgumentException("channel request is required");
            }
            Long tenantId = tenantId(context);
            String userId = required(req.userId(), "userId");
            String channel = normalizeRequired(req.channel(), "channel");
            // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
            CustomerChannelDO row = channelMapper.selectOne(new LambdaQueryWrapper<CustomerChannelDO>()
                    .eq(CustomerChannelDO::getTenantId, tenantId)
                    .eq(CustomerChannelDO::getUserId, userId)
                    .eq(CustomerChannelDO::getChannel, channel)
                    .last("LIMIT 1"));
            boolean insert = row == null;
            if (insert) {
                row = new CustomerChannelDO();
            }
            row.setTenantId(tenantId);
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
        }).subscribeOn(Schedulers.boundedElastic()));
    }

    /**
     * 获取当前请求的登录上下文或租户信息。
     *
     * @return 返回 currentTenant 流程生成的业务结果。
     */
    private Mono<TenantContext> currentTenant() {
        return tenantContextResolver.currentOrError();
    }

    /**
     * 解析并规范化租户 ID。
     *
     * @param context 上下文对象，承载租户、身份或运行时信息。
     * @return 返回 tenant id 计算得到的数量、金额或指标值。
     */
    private Long tenantId(TenantContext context) {
        return context == null || context.tenantId() == null ? DEFAULT_TENANT_ID : context.tenantId();
    }

    /**
     * 规范化输入值。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private String normalizeChannelOrAll(String value) {
        return value == null || value.isBlank() ? ALL_CHANNELS : normalizeRequired(value, "channel");
    }

    /**
     * 规范化输入值。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param field 待处理业务值，用于规则计算、转换或外部调用。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private String normalizeRequired(String value, String field) {
        return required(value, field).toUpperCase(Locale.ROOT);
    }

    /**
     * 校验并获取必需参数、资源或权限。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param field 待处理业务值，用于规则计算、转换或外部调用。
     * @return 返回 required 生成的文本或业务键。
     */
    private String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }

    /**
     * 执行 optional 流程，围绕 optional 完成校验、计算或结果组装。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 optional 生成的文本或业务键。
     */
    private String optional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
