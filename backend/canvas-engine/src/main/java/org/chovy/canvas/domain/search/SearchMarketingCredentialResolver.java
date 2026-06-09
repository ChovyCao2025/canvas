package org.chovy.canvas.domain.search;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.dal.dataobject.MarketingMonitorProviderCredentialDO;
import org.chovy.canvas.dal.mapper.MarketingMonitorProviderCredentialMapper;
import org.chovy.canvas.domain.providerwrite.ProviderWriteEvidenceSanitizer;
import org.chovy.canvas.domain.providerwrite.ProviderWriteSandboxSupport;
import org.chovy.canvas.security.SecretCipher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Map;

/**
 * SearchMarketingCredentialResolver 编排 domain.search 场景的领域业务规则。
 */
@Service
public class SearchMarketingCredentialResolver {

    private static final TypeReference<Map<String, Object>> OBJECT_MAP = new TypeReference<>() {
    };

    private final MarketingMonitorProviderCredentialMapper credentialMapper;
    private final SecretCipher secretCipher;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    /**
     * 创建 SearchMarketingCredentialResolver 实例并注入 domain.search 场景依赖。
     * @param credentialMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param secretCipher secret cipher 参数，用于 SearchMarketingCredentialResolver 流程中的校验、计算或对象转换。
     * @param objectMapper 依赖组件，用于完成数据访问或外部能力调用。
     */
    @Autowired
    public SearchMarketingCredentialResolver(MarketingMonitorProviderCredentialMapper credentialMapper,
                                             SecretCipher secretCipher,
                                             ObjectMapper objectMapper) {
        this(credentialMapper, secretCipher, objectMapper, Clock.systemDefaultZone());
    }

    /**
     * 查询或读取业务数据。
     *
     * @param credentialMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param secretCipher secret cipher 参数，用于 SearchMarketingCredentialResolver 流程中的校验、计算或对象转换。
     * @param objectMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param clock 时间参数，用于计算窗口、过期或审计时间。
     */
    SearchMarketingCredentialResolver(MarketingMonitorProviderCredentialMapper credentialMapper,
                                      SecretCipher secretCipher,
                                      ObjectMapper objectMapper,
                                      Clock clock) {
        this.credentialMapper = credentialMapper;
        this.secretCipher = secretCipher;
        this.objectMapper = objectMapper == null ? new ObjectMapper() : objectMapper;
        this.clock = clock == null ? Clock.systemDefaultZone() : clock;
    }

    /**
     * 解析搜索营销提供方的租户级凭据。
     *
     * <p>沙箱提供方直接返回沙箱凭据引用；真实提供方会按租户、provider 和 credentialKey 查询托管凭据，
     * 校验状态和过期时间，解密访问令牌、开发者令牌与刷新令牌，并对元数据做安全清洗。
     * 缺失、禁用、过期或密文为空时返回 unavailable 引用而不是抛出。</p>
     *
     * @param tenantId 租户 ID，用于隔离凭据
     * @param provider 搜索营销提供方编码
     * @param credentialKey 业务配置中引用的凭据 key
     * @return 可用于外部搜索营销 API 的凭据引用，或携带不可用原因的引用
     */
    public SearchMarketingCredentialRef resolve(Long tenantId, String provider, String credentialKey) {
        String normalizedProvider = normalize(provider);
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (ProviderWriteSandboxSupport.supportsSandboxProvider(normalizedProvider)) {
            return SearchMarketingCredentialRef.sandbox();
        }
        String normalizedKey = normalizeKey(credentialKey);
        if (normalizedKey == null) {
            return SearchMarketingCredentialRef.unavailable(null, normalizedProvider,
                    "SEARCH_PROVIDER_CREDENTIAL_KEY_REQUIRED",
                    "search provider credential key is required");
        }
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        MarketingMonitorProviderCredentialDO row = credentialMapper.selectOne(
                new LambdaQueryWrapper<MarketingMonitorProviderCredentialDO>()
                        .eq(MarketingMonitorProviderCredentialDO::getTenantId, normalizeTenant(tenantId))
                        .eq(MarketingMonitorProviderCredentialDO::getCredentialKey, normalizedKey)
                        .eq(MarketingMonitorProviderCredentialDO::getProviderType, normalizedProvider)
                        .last("LIMIT 1"));
        if (row == null || !normalizeTenant(tenantId).equals(row.getTenantId())) {
            return SearchMarketingCredentialRef.unavailable(normalizedKey, normalizedProvider,
                    "SEARCH_PROVIDER_CREDENTIAL_NOT_FOUND",
                    "search provider credential is not found");
        }
        if (!"ACTIVE".equals(normalize(row.getStatus()))) {
            return SearchMarketingCredentialRef.unavailable(normalizedKey, normalizedProvider,
                    "SEARCH_PROVIDER_CREDENTIAL_DISABLED",
                    "search provider credential is disabled");
        }
        LocalDateTime now = LocalDateTime.now(clock);
        if (row.getExpiresAt() != null && !row.getExpiresAt().isAfter(now)) {
            return SearchMarketingCredentialRef.unavailable(normalizedKey, normalizedProvider,
                    "SEARCH_PROVIDER_CREDENTIAL_EXPIRED",
                    "search provider credential is expired");
        }
        String accessToken = decrypt(row.getAccessTokenCiphertext());
        String developerToken = decrypt(row.getApiKeyCiphertext());
        if (accessToken == null && developerToken == null) {
            return SearchMarketingCredentialRef.unavailable(normalizedKey, normalizedProvider,
                    "SEARCH_PROVIDER_CREDENTIAL_SECRET_MISSING",
                    "search provider credential secret is missing");
        }
        // 汇总前面计算出的状态和明细，返回给调用方。
        return new SearchMarketingCredentialRef(
                row.getId(),
                row.getCredentialKey(),
                row.getProviderType(),
                row.getAuthType(),
                accessToken,
                developerToken,
                decrypt(row.getRefreshTokenCiphertext()),
                row.getExpiresAt(),
                safeMetadata(row.getMetadataJson()),
                true,
                null,
                null);
    }

    /**
     * 按安全边界裁剪或保护输入值。
     *
     * @param metadataJson JSON 字符串，承载结构化配置或明细。
     * @return 返回 safeMetadata 流程生成的业务结果。
     */
    private Map<String, Object> safeMetadata(String metadataJson) {
        if (metadataJson == null || metadataJson.isBlank()) {
            return Map.of();
        }
        try {
            return ProviderWriteEvidenceSanitizer.sanitizeMap(objectMapper.readValue(metadataJson, OBJECT_MAP));
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (JsonProcessingException ex) {
            return Map.of("metadataParseError", "invalid credential metadata JSON");
        }
    }

    /**
     * 处理安全、签名或敏感信息逻辑。
     *
     * @param ciphertext ciphertext 参数，用于 decrypt 流程中的校验、计算或对象转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private String decrypt(String ciphertext) {
        String value = secretCipher.decrypt(ciphertext);
        return value == null || value.isBlank() ? null : value;
    }

    /**
     * 解析并规范化租户 ID。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private static Long normalizeTenant(Long tenantId) {
        return tenantId == null || tenantId < 0 ? 0L : tenantId;
    }

    /**
     * 规范化输入值。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private static String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    /**
     * 规范化输入值。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private static String normalizeKey(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
