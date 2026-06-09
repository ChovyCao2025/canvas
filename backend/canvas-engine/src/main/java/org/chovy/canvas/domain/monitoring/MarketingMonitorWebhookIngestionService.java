package org.chovy.canvas.domain.monitoring;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.dal.dataobject.MarketingMonitorSourceDO;
import org.chovy.canvas.dal.mapper.MarketingMonitorSourceMapper;
import org.chovy.canvas.security.SecretCipher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Map;

/**
 * MarketingMonitorWebhookIngestionService 编排 domain.monitoring 场景的领域业务规则。
 */
@Service
public class MarketingMonitorWebhookIngestionService {

    private static final int SECRET_BYTES = 32;
    private static final int SECRET_PREFIX_LENGTH = 12;
    private static final int DEFAULT_TOLERANCE_SECONDS = 300;
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final TypeReference<Map<String, Object>> OBJECT_MAP = new TypeReference<>() {
    };

    private final MarketingMonitorSourceMapper sourceMapper;
    private final MarketingMonitoringService monitoringService;
    private final MarketingMonitorWebhookPayloadMapper payloadMapper;
    private final MarketingMonitorWebhookSignatureService signatureService;
    private final SecretCipher secretCipher;
    private final BCryptPasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    /**
     * 创建 MarketingMonitorWebhookIngestionService 实例并注入 domain.monitoring 场景依赖。
     * @param sourceMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param monitoringService 依赖组件，用于完成数据访问或外部能力调用。
     * @param payloadMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param signatureService 依赖组件，用于完成数据访问或外部能力调用。
     * @param secretCipher secret cipher 参数，用于 MarketingMonitorWebhookIngestionService 流程中的校验、计算或对象转换。
     * @param passwordEncoder password encoder 参数，用于 MarketingMonitorWebhookIngestionService 流程中的校验、计算或对象转换。
     * @param objectMapper 依赖组件，用于完成数据访问或外部能力调用。
     */
    @Autowired
    public MarketingMonitorWebhookIngestionService(MarketingMonitorSourceMapper sourceMapper,
                                                   MarketingMonitoringService monitoringService,
                                                   MarketingMonitorWebhookPayloadMapper payloadMapper,
                                                   MarketingMonitorWebhookSignatureService signatureService,
                                                   SecretCipher secretCipher,
                                                   BCryptPasswordEncoder passwordEncoder,
                                                   ObjectMapper objectMapper) {
        this(sourceMapper, monitoringService, payloadMapper, signatureService, secretCipher,
                passwordEncoder, objectMapper, Clock.systemDefaultZone());
    }

    /**
     * 执行 MarketingMonitorWebhookIngestionService 流程，围绕 marketing monitor webhook ingestion service 完成校验、计算或结果组装。
     *
     * @param sourceMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param monitoringService 依赖组件，用于完成数据访问或外部能力调用。
     * @param payloadMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param signatureService 依赖组件，用于完成数据访问或外部能力调用。
     * @param secretCipher secret cipher 参数，用于 MarketingMonitorWebhookIngestionService 流程中的校验、计算或对象转换。
     * @param passwordEncoder password encoder 参数，用于 MarketingMonitorWebhookIngestionService 流程中的校验、计算或对象转换。
     * @param objectMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param clock 时间参数，用于计算窗口、过期或审计时间。
     */
    MarketingMonitorWebhookIngestionService(MarketingMonitorSourceMapper sourceMapper,
                                            MarketingMonitoringService monitoringService,
                                            MarketingMonitorWebhookPayloadMapper payloadMapper,
                                            MarketingMonitorWebhookSignatureService signatureService,
                                            SecretCipher secretCipher,
                                            BCryptPasswordEncoder passwordEncoder,
                                            ObjectMapper objectMapper,
                                            Clock clock) {
        this.sourceMapper = sourceMapper;
        this.monitoringService = monitoringService;
        this.payloadMapper = payloadMapper;
        this.signatureService = signatureService;
        this.secretCipher = secretCipher;
        this.passwordEncoder = passwordEncoder;
        this.objectMapper = objectMapper == null ? new ObjectMapper() : objectMapper;
        this.clock = clock == null ? Clock.systemDefaultZone() : clock;
    }

    /**
     * 执行业务操作 rotateSecret，作为营销监控的服务入口。
     * <p>调用方必须传入租户上下文或租户 ID，方法内的查询、写入和治理判断都限制在该租户范围内。
     * 会通过 Mapper 写入、更新或关闭持久化记录；可能与外部供应商、Webhook 或上传交接端点交互。
     * @param tenantId 租户 ID，所有查询和写入都限定在该租户数据范围内
     * @param sourceId 目标业务记录 ID，需与租户边界匹配
     * @param actor 操作人标识，用于审计字段、状态流转记录或治理追踪
     * @return 返回租户范围内的最新业务视图或持久化对象快照
     */
    public MarketingMonitorWebhookSecretView rotateSecret(Long tenantId, Long sourceId, String actor) {
        Long scopedTenantId = normalizeTenant(tenantId);
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (sourceId == null) {
            throw new IllegalArgumentException("sourceId is required");
        }
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        MarketingMonitorSourceDO source = sourceMapper.selectById(sourceId);
        if (source == null || !scopedTenantId.equals(source.getTenantId())) {
            throw new IllegalArgumentException("monitor source is not found");
        }
        String rawSecret = generateSecret();
        LocalDateTime rotatedAt = now();
        int tolerance = tolerance(source.getWebhookSignatureToleranceSeconds());
        source.setWebhookEnabled(1);
        source.setWebhookSecretPrefix(rawSecret.substring(0, Math.min(SECRET_PREFIX_LENGTH, rawSecret.length())));
        source.setWebhookSecretHash(passwordEncoder.encode(rawSecret));
        source.setWebhookSecretCiphertext(secretCipher.encrypt(rawSecret));
        source.setWebhookSignatureToleranceSeconds(tolerance);
        source.setUpdatedAt(rotatedAt);
        sourceMapper.updateById(source);
        // 汇总前面计算出的状态和明细，返回给调用方。
        return new MarketingMonitorWebhookSecretView(
                source.getId(),
                scopedTenantId,
                source.getSourceKey(),
                source.getWebhookSecretPrefix(),
                rawSecret,
                endpointPath(scopedTenantId, source.getSourceKey()),
                tolerance,
                defaultActor(actor),
                rotatedAt);
    }

    /**
     * 执行业务操作 ingestWebhook，作为营销监控的服务入口。
     * <p>调用方必须传入租户上下文或租户 ID，方法内的查询、写入和治理判断都限制在该租户范围内。
     * 可能与外部供应商、Webhook 或上传交接端点交互。
     * @param tenantId 租户 ID，所有查询和写入都限定在该租户数据范围内
     * @param sourceKey 业务键，用于定位租户内的配置、资产或治理对象
     * @param timestamp 请求时间戳，参与签名计算并用于回放治理
     * @param signature signature 参数，用于 ingestWebhook 流程中的校验、计算或对象转换。
     * @param rawBody 未经改写的请求正文，参与签名计算以避免回调被篡改
     * @return 返回租户范围内的最新业务视图或持久化对象快照
     */
    public MarketingMonitorWebhookIngestView ingestWebhook(Long tenantId,
                                                           String sourceKey,
                                                           String timestamp,
                                                           String signature,
                                                           String rawBody) {
        // 准备本次处理所需的上下文和中间变量。
        Long scopedTenantId = normalizeTenant(tenantId);
        String normalizedSourceKey = normalizeKey(sourceKey);
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        MarketingMonitorSourceDO source = sourceMapper.selectOne(
                new LambdaQueryWrapper<MarketingMonitorSourceDO>()
                        .eq(MarketingMonitorSourceDO::getTenantId, scopedTenantId)
                        .eq(MarketingMonitorSourceDO::getSourceKey, normalizedSourceKey)
                        .last("LIMIT 1"));
        validateWebhookSource(scopedTenantId, source);
        String secret = secretCipher.decrypt(source.getWebhookSecretCiphertext());
        signatureService.verifyOrThrow(secret, timestamp, rawBody, signature,
                tolerance(source.getWebhookSignatureToleranceSeconds()));
        MarketingMonitorItemIngestCommand command = payloadMapper.toIngestCommand(source, parsePayload(rawBody));
        MarketingMonitorIngestResult result = monitoringService.ingestItem(
                scopedTenantId,
                command,
                "monitor-webhook:" + source.getSourceKey());
        // 汇总前面计算出的状态和明细，返回给调用方。
        return new MarketingMonitorWebhookIngestView(
                scopedTenantId,
                source.getId(),
                source.getSourceKey(),
                result);
    }

    /**
     * 校验输入、权限或业务前置条件。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param source source 参数，用于 validateWebhookSource 流程中的校验、计算或对象转换。
     */
    private void validateWebhookSource(Long tenantId, MarketingMonitorSourceDO source) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (source == null || !tenantId.equals(source.getTenantId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "monitor source is not found");
        }
        if (!enabled(source.getEnabled())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "monitor source is disabled");
        }
        if (!enabled(source.getWebhookEnabled())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "monitoring webhook is not enabled");
        }
        if (isBlank(source.getWebhookSecretCiphertext())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "monitoring webhook secret is not configured");
        }
    }

    /**
     * 解析并校验输入数据。
     *
     * @param rawBody raw body 参数，用于 parsePayload 流程中的校验、计算或对象转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private Map<String, Object> parsePayload(String rawBody) {
        if (isBlank(rawBody)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "monitoring webhook payload is required");
        }
        try {
            return objectMapper.readValue(rawBody, OBJECT_MAP);
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (JsonProcessingException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "monitoring webhook payload must be JSON", ex);
        }
    }

    /**
     * 处理安全、签名或敏感信息逻辑。
     *
     * @return 返回 generate secret 生成的文本或业务键。
     */
    private String generateSecret() {
        byte[] bytes = new byte[SECRET_BYTES];
        RANDOM.nextBytes(bytes);
        return "monwhsec_" + HexFormat.of().formatHex(bytes);
    }

    /**
     * 执行 endpointPath 流程，围绕 endpoint path 完成校验、计算或结果组装。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param sourceKey 业务键，用于在同一租户下定位资源。
     * @return 返回 endpoint path 生成的文本或业务键。
     */
    private String endpointPath(Long tenantId, String sourceKey) {
        return "/public/marketing-monitoring/webhooks/" + tenantId + "/" + sourceKey;
    }

    /**
     * 转换为接口返回或领域视图。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回组装或转换后的结果对象。
     */
    private int tolerance(Integer value) {
        return value == null || value <= 0 ? DEFAULT_TOLERANCE_SECONDS : value;
    }

    /**
     * 解析并规范化租户 ID。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private Long normalizeTenant(Long tenantId) {
        return tenantId == null ? 0L : tenantId;
    }

    /**
     * 规范化输入值。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private String normalizeKey(String value) {
        if (isBlank(value)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "sourceKey is required");
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }

    /**
     * 执行数据写入或状态变更。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 enabled 的布尔判断结果。
     */
    private boolean enabled(Integer value) {
        return value != null && value == 1;
    }

    /**
     * 解析操作人标识。
     *
     * @param actor 操作人标识，用于审计和权限判断。
     * @return 返回 default actor 生成的文本或业务键。
     */
    private String defaultActor(String actor) {
        return isBlank(actor) ? "system" : actor.trim();
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
     * 判断业务条件是否成立。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回布尔判断结果。
     */
    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
