package org.chovy.canvas.domain.monitoring;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.dal.dataobject.MarketingMonitorProviderCredentialDO;
import org.chovy.canvas.dal.dataobject.MarketingMonitorProviderCredentialEventDO;
import org.chovy.canvas.dal.mapper.MarketingMonitorProviderCredentialEventMapper;
import org.chovy.canvas.dal.mapper.MarketingMonitorProviderCredentialMapper;
import org.chovy.canvas.security.SecretCipher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * MarketingMonitorProviderCredentialService 编排 domain.monitoring 场景的领域业务规则。
 */
@Service
public class MarketingMonitorProviderCredentialService {

    private static final int SECRET_PREFIX_LENGTH = 12;
    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_DISABLED = "DISABLED";
    private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {
    };
    private static final TypeReference<Map<String, Object>> OBJECT_MAP = new TypeReference<>() {
    };

    private final MarketingMonitorProviderCredentialMapper credentialMapper;
    private final MarketingMonitorProviderCredentialEventMapper eventMapper;
    private final MarketingMonitorProviderHttpTransport refreshTransport;
    private final ObjectMapper objectMapper;
    private final SecretCipher secretCipher;
    private final BCryptPasswordEncoder passwordEncoder;
    private final Clock clock;

    /**
     * 创建 MarketingMonitorProviderCredentialService 实例并注入 domain.monitoring 场景依赖。
     * @param credentialMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param eventMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param refreshTransport refresh transport 参数，用于 MarketingMonitorProviderCredentialService 流程中的校验、计算或对象转换。
     * @param objectMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param secretCipher secret cipher 参数，用于 MarketingMonitorProviderCredentialService 流程中的校验、计算或对象转换。
     * @param passwordEncoder password encoder 参数，用于 MarketingMonitorProviderCredentialService 流程中的校验、计算或对象转换。
     */
    @Autowired
    public MarketingMonitorProviderCredentialService(
            MarketingMonitorProviderCredentialMapper credentialMapper,
            MarketingMonitorProviderCredentialEventMapper eventMapper,
            MarketingMonitorProviderHttpTransport refreshTransport,
            ObjectMapper objectMapper,
            SecretCipher secretCipher,
            BCryptPasswordEncoder passwordEncoder) {
        this(credentialMapper, eventMapper, refreshTransport, objectMapper, secretCipher, passwordEncoder,
                Clock.systemDefaultZone());
    }

    /**
     * 执行 MarketingMonitorProviderCredentialService 流程，围绕 marketing monitor provider credential service 完成校验、计算或结果组装。
     *
     * @param credentialMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param eventMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param refreshTransport refresh transport 参数，用于 MarketingMonitorProviderCredentialService 流程中的校验、计算或对象转换。
     * @param objectMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param secretCipher secret cipher 参数，用于 MarketingMonitorProviderCredentialService 流程中的校验、计算或对象转换。
     * @param passwordEncoder password encoder 参数，用于 MarketingMonitorProviderCredentialService 流程中的校验、计算或对象转换。
     * @param clock 时间参数，用于计算窗口、过期或审计时间。
     */
    MarketingMonitorProviderCredentialService(
            MarketingMonitorProviderCredentialMapper credentialMapper,
            MarketingMonitorProviderCredentialEventMapper eventMapper,
            MarketingMonitorProviderHttpTransport refreshTransport,
            ObjectMapper objectMapper,
            SecretCipher secretCipher,
            BCryptPasswordEncoder passwordEncoder,
            Clock clock) {
        this.credentialMapper = credentialMapper;
        this.eventMapper = eventMapper;
        this.refreshTransport = refreshTransport;
        this.objectMapper = objectMapper == null ? new ObjectMapper() : objectMapper;
        this.secretCipher = secretCipher;
        this.passwordEncoder = passwordEncoder == null ? new BCryptPasswordEncoder() : passwordEncoder;
        this.clock = clock == null ? Clock.systemDefaultZone() : clock;
    }

    /**
     * 创建或更新业务记录，作为营销监控的服务入口。
     * <p>调用方必须传入租户上下文或租户 ID，方法内的查询、写入和治理判断都限制在该租户范围内。
     * 会通过 Mapper 写入、更新或关闭持久化记录。
     * @param tenantId 租户 ID，所有查询和写入都限定在该租户数据范围内
     * @param command 本次操作的业务请求参数，包含目标对象、状态或外部回调载荷
     * @param actor 操作人标识，用于审计字段、状态流转记录或治理追踪
     * @return 返回租户范围内的最新业务视图或持久化对象快照
     */
    @Transactional(rollbackFor = Exception.class)
    public MarketingMonitorProviderCredentialView upsert(Long tenantId,
                                                         MarketingMonitorProviderCredentialCommand command,
                                                         String actor) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (command == null) {
            throw new IllegalArgumentException("provider credential command is required");
        }
        Long scopedTenantId = normalizeTenant(tenantId);
        String credentialKey = normalizeKey(command.credentialKey(), "credentialKey");
        MarketingMonitorProviderCredentialDO row = findCredential(scopedTenantId, credentialKey);
        LocalDateTime changedAt = now();
        if (row == null) {
            row = new MarketingMonitorProviderCredentialDO();
            row.setTenantId(scopedTenantId);
            row.setCredentialKey(credentialKey);
            row.setCreatedBy(defaultActor(actor));
            row.setCreatedAt(changedAt);
            row.setRefreshAttemptCount(0);
        }
        row.setProviderType(normalizeType(command.providerType(), "providerType"));
        row.setAuthType(normalizeType(command.authType(), "authType"));
        row.setDisplayName(defaultString(command.displayName(), credentialKey));
        row.setStatus(Boolean.FALSE.equals(command.enabled()) ? STATUS_DISABLED : STATUS_ACTIVE);
        row.setTokenType(trimToNull(command.tokenType()));
        row.setScopesJson(json(normalizedScopes(command.scopes())));
        applySecret(command.accessToken(), row::setAccessTokenPrefix, row::setAccessTokenHash,
                row::setAccessTokenCiphertext);
        applySecret(command.refreshToken(), row::setRefreshTokenPrefix, row::setRefreshTokenHash,
                row::setRefreshTokenCiphertext);
        applySecret(command.apiKey(), row::setApiKeyPrefix, row::setApiKeyHash, row::setApiKeyCiphertext);
        if (hasText(command.clientId())) {
            row.setClientIdCiphertext(secretCipher.encrypt(command.clientId().trim()));
        }
        if (hasText(command.clientSecret())) {
            row.setClientSecretCiphertext(secretCipher.encrypt(command.clientSecret().trim()));
        }
        row.setRefreshEndpoint(validateOptionalUri(command.refreshEndpoint(), "refreshEndpoint"));
        row.setRevokeEndpoint(validateOptionalUri(command.revokeEndpoint(), "revokeEndpoint"));
        row.setExpiresAt(command.expiresAt());
        row.setRefreshTokenExpiresAt(command.refreshTokenExpiresAt());
        row.setMetadataJson(json(safeMap(command.metadata())));
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        row.setUpdatedBy(defaultActor(actor));
        row.setUpdatedAt(changedAt);
        if (row.getId() == null) {
            credentialMapper.insert(row);
        } else {
            credentialMapper.updateById(row);
        }
        writeEvent(scopedTenantId, row, "UPSERTED", "SUCCESS", Map.of(
                "providerType", row.getProviderType(),
                "authType", row.getAuthType(),
                "status", row.getStatus()), null, actor);
        // 汇总前面计算出的状态和明细，返回给调用方。
        return toView(row);
    }

    /**
     * 查询业务列表，作为营销监控的服务入口。
     * <p>调用方必须传入租户上下文或租户 ID，方法内的查询、写入和治理判断都限制在该租户范围内。
     * 不直接修改业务状态，主要读取数据或执行本地规则计算。
     * @param tenantId 租户 ID，所有查询和写入都限定在该租户数据范围内
     * @param query query 参数，用于 list 流程中的校验、计算或对象转换。
     * @return 返回按租户、状态和数量限制过滤后的视图列表；无数据时返回空列表
     */
    public List<MarketingMonitorProviderCredentialView> list(Long tenantId,
                                                             MarketingMonitorProviderCredentialQuery query) {
        // 准备本次流程的上下文、默认值和中间结果。
        Long scopedTenantId = normalizeTenant(tenantId);
        MarketingMonitorProviderCredentialQuery effectiveQuery = query == null
                ? new MarketingMonitorProviderCredentialQuery(null, null, null, 50)
                : query;
        int limit = boundedLimit(effectiveQuery.limit());
        String providerType = normalizeOptionalUpper(effectiveQuery.providerType());
        String authType = normalizeOptionalUpper(effectiveQuery.authType());
        String status = normalizeOptionalUpper(effectiveQuery.status());
        // 访问持久化数据，读取现有配置或写入本次变更。
        return safeList(credentialMapper.selectList(new LambdaQueryWrapper<MarketingMonitorProviderCredentialDO>()
                        .eq(MarketingMonitorProviderCredentialDO::getTenantId, scopedTenantId)
                        .eq(providerType != null, MarketingMonitorProviderCredentialDO::getProviderType, providerType)
                        .eq(authType != null, MarketingMonitorProviderCredentialDO::getAuthType, authType)
                        .eq(status != null, MarketingMonitorProviderCredentialDO::getStatus, status)
                        .orderByDesc(MarketingMonitorProviderCredentialDO::getUpdatedAt)
                        // 遍历候选记录并转换为前端或服务层需要的视图。
                        .last("LIMIT " + limit))).stream()
                .filter(row -> scopedTenantId.equals(row.getTenantId()))
                .filter(row -> providerType == null || providerType.equals(row.getProviderType()))
                .filter(row -> authType == null || authType.equals(row.getAuthType()))
                .filter(row -> status == null || status.equals(row.getStatus()))
                .limit(limit)
                .map(this::toView)
                .toList();
    }

    /**
     * 执行业务操作 disable，作为营销监控的服务入口。
     * <p>调用方必须传入租户上下文或租户 ID，方法内的查询、写入和治理判断都限制在该租户范围内。
     * 会通过 Mapper 写入、更新或关闭持久化记录。
     * @param tenantId 租户 ID，所有查询和写入都限定在该租户数据范围内
     * @param credentialKey 业务键，用于定位租户内的配置、资产或治理对象
     * @param actor 操作人标识，用于审计字段、状态流转记录或治理追踪
     * @return 返回租户范围内的最新业务视图或持久化对象快照
     */
    @Transactional(rollbackFor = Exception.class)
    public MarketingMonitorProviderCredentialView disable(Long tenantId, String credentialKey, String actor) {
        // 准备本次处理所需的上下文和中间变量。
        Long scopedTenantId = normalizeTenant(tenantId);
        MarketingMonitorProviderCredentialDO row = requiredCredential(scopedTenantId, credentialKey);
        row.setStatus(STATUS_DISABLED);
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        row.setUpdatedBy(defaultActor(actor));
        row.setUpdatedAt(now());
        credentialMapper.updateById(row);
        writeEvent(scopedTenantId, row, "DISABLED", "SUCCESS", Map.of("status", STATUS_DISABLED), null, actor);
        // 汇总前面计算出的状态和明细，返回给调用方。
        return toView(row);
    }

    /**
     * 执行业务操作 refresh，作为营销监控的服务入口。
     * <p>调用方必须传入租户上下文或租户 ID，方法内的查询、写入和治理判断都限制在该租户范围内。
     * 会通过 Mapper 写入、更新或关闭持久化记录；可能与外部供应商、Webhook 或上传交接端点交互。
     * @param tenantId 租户 ID，所有查询和写入都限定在该租户数据范围内
     * @param credentialKey 业务键，用于定位租户内的配置、资产或治理对象
     * @param command 本次操作的业务请求参数，包含目标对象、状态或外部回调载荷
     * @param actor 操作人标识，用于审计字段、状态流转记录或治理追踪
     * @return 返回租户范围内的最新业务视图或持久化对象快照
     */
    @Transactional(rollbackFor = Exception.class)
    public MarketingMonitorProviderCredentialView refresh(Long tenantId,
                                                          String credentialKey,
                                                          MarketingMonitorProviderCredentialRefreshCommand command,
                                                          String actor) {
        Long scopedTenantId = normalizeTenant(tenantId);
        MarketingMonitorProviderCredentialDO row = requiredCredential(scopedTenantId, credentialKey);
        ensureEnabled(row);
        int nextAttempt = defaultInt(row.getRefreshAttemptCount()) + 1;
        try {
            MarketingMonitorProviderHttpResponse response = refreshTransport.execute(refreshRequest(row));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return refreshFailed(scopedTenantId, row, nextAttempt,
                        "provider refresh failed with status " + response.statusCode(), actor,
                        Map.of("httpStatus", response.statusCode()));
            }
            JsonNode body = jsonNode(response.body());
            String accessToken = text(body, "access_token");
            if (!hasText(accessToken)) {
                return refreshFailed(scopedTenantId, row, nextAttempt,
                        "provider refresh response did not include access_token", actor,
                        Map.of("httpStatus", response.statusCode()));
            }
            LocalDateTime changedAt = now();
            applySecret(accessToken, row::setAccessTokenPrefix, row::setAccessTokenHash,
                    row::setAccessTokenCiphertext);
            String refreshToken = text(body, "refresh_token");
            if (hasText(refreshToken)) {
                applySecret(refreshToken, row::setRefreshTokenPrefix, row::setRefreshTokenHash,
                        row::setRefreshTokenCiphertext);
            }
            String tokenType = text(body, "token_type");
            if (hasText(tokenType)) {
                row.setTokenType(normalizeTokenType(tokenType));
            }
            List<String> scopes = refreshScopes(body);
            if (!scopes.isEmpty()) {
                row.setScopesJson(json(scopes));
            }
            Long expiresIn = longValue(body, "expires_in");
            if (expiresIn != null && expiresIn > 0) {
                row.setExpiresAt(changedAt.plusSeconds(expiresIn));
            }
            row.setLastRefreshedAt(changedAt);
            row.setRefreshAttemptCount(nextAttempt);
            row.setLastRefreshStatus("SUCCESS");
            row.setLastRefreshError(null);
            row.setUpdatedBy(defaultActor(actor));
            row.setUpdatedAt(changedAt);
            credentialMapper.updateById(row);
            writeEvent(scopedTenantId, row, "REFRESHED", "SUCCESS", Map.of(
                    "httpStatus", response.statusCode(),
                    "tokenType", row.getTokenType(),
                    "expiresAt", string(row.getExpiresAt())), null, actor);
            return toView(row);
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (RuntimeException ex) {
            return refreshFailed(scopedTenantId, row, nextAttempt, message(ex), actor, Map.of());
        }
    }

    /**
     * 执行业务操作 refreshDue，作为营销监控的服务入口。
     * <p>调用方必须传入租户上下文或租户 ID，方法内的查询、写入和治理判断都限制在该租户范围内。
     * @param tenantId 租户 ID，所有查询和写入都限定在该租户数据范围内
     * @param command 本次操作的业务请求参数，包含目标对象、状态或外部回调载荷
     * @param actor 操作人标识，用于审计字段、状态流转记录或治理追踪
     * @return 返回本次处理的状态、计数、命中明细或治理结论，供控制器和调度任务判断后续动作
     */
    public MarketingMonitorProviderCredentialDueRefreshResult refreshDue(
            Long tenantId,
            MarketingMonitorProviderCredentialDueRefreshCommand command,
            String actor) {
        Long scopedTenantId = normalizeTenant(tenantId);
        MarketingMonitorProviderCredentialDueRefreshCommand effectiveCommand = command == null
                ? new MarketingMonitorProviderCredentialDueRefreshCommand(null, null)
                : command;
        int limit = boundedLimit(effectiveCommand.limit() == null ? 50 : effectiveCommand.limit());
        int windowMinutes = boundedWindowMinutes(effectiveCommand.windowMinutes());
        LocalDateTime evaluatedAt = now();
        LocalDateTime cutoffAt = evaluatedAt.plusMinutes(windowMinutes);
        List<MarketingMonitorProviderCredentialDO> candidates = safeList(credentialMapper.selectList(
                new LambdaQueryWrapper<MarketingMonitorProviderCredentialDO>()
                        .eq(MarketingMonitorProviderCredentialDO::getTenantId, scopedTenantId)
                        .eq(MarketingMonitorProviderCredentialDO::getStatus, STATUS_ACTIVE)
                        .isNotNull(MarketingMonitorProviderCredentialDO::getRefreshEndpoint)
                        .isNotNull(MarketingMonitorProviderCredentialDO::getRefreshTokenCiphertext)
                        .isNotNull(MarketingMonitorProviderCredentialDO::getExpiresAt)
                        .le(MarketingMonitorProviderCredentialDO::getExpiresAt, cutoffAt)
                        .orderByAsc(MarketingMonitorProviderCredentialDO::getExpiresAt)
                        .last("LIMIT " + limit)));
        List<MarketingMonitorProviderCredentialDO> due = candidates.stream()
                .filter(row -> scopedTenantId.equals(row.getTenantId()))
                .filter(row -> STATUS_ACTIVE.equals(normalizeOptionalUpper(row.getStatus())))
                .filter(row -> hasText(row.getRefreshEndpoint()))
                .filter(row -> hasText(row.getRefreshTokenCiphertext()))
                .filter(row -> row.getExpiresAt() != null && !row.getExpiresAt().isAfter(cutoffAt))
                .sorted(Comparator.comparing(
                        MarketingMonitorProviderCredentialDO::getExpiresAt,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .limit(limit)
                .toList();
        int refreshed = 0;
        int failed = 0;
        List<MarketingMonitorProviderCredentialView> credentials = new ArrayList<>();
        for (MarketingMonitorProviderCredentialDO row : due) {
            try {
                MarketingMonitorProviderCredentialView view = refresh(scopedTenantId, row.getCredentialKey(),
                        new MarketingMonitorProviderCredentialRefreshCommand(null), actor);
                credentials.add(view);
                if ("SUCCESS".equals(normalizeOptionalUpper(view.lastRefreshStatus()))) {
                    refreshed++;
                } else {
                    failed++;
                }
            // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
            } catch (RuntimeException ex) {
                failed++;
                credentials.add(refreshFailed(scopedTenantId, row,
                        defaultInt(row.getRefreshAttemptCount()) + 1,
                        message(ex),
                        actor,
                        Map.of("scheduled", true)));
            }
        }
        return new MarketingMonitorProviderCredentialDueRefreshResult(
                scopedTenantId,
                candidates.size(),
                due.size(),
                refreshed,
                failed,
                Math.max(0, candidates.size() - due.size()),
                cutoffAt,
                evaluatedAt,
                credentials);
    }

    /**
     * 执行业务操作 revoke，作为营销监控的服务入口。
     * <p>调用方必须传入租户上下文或租户 ID，方法内的查询、写入和治理判断都限制在该租户范围内。
     * 会通过 Mapper 写入、更新或关闭持久化记录；可能与外部供应商、Webhook 或上传交接端点交互。
     * @param tenantId 租户 ID，所有查询和写入都限定在该租户数据范围内
     * @param credentialKey 业务键，用于定位租户内的配置、资产或治理对象
     * @param command 本次操作的业务请求参数，包含目标对象、状态或外部回调载荷
     * @param actor 操作人标识，用于审计字段、状态流转记录或治理追踪
     * @return 返回租户范围内的最新业务视图或持久化对象快照
     */
    @Transactional(rollbackFor = Exception.class)
    public MarketingMonitorProviderCredentialView revoke(Long tenantId,
                                                         String credentialKey,
                                                         MarketingMonitorProviderCredentialRevokeCommand command,
                                                         String actor) {
        Long scopedTenantId = normalizeTenant(tenantId);
        MarketingMonitorProviderCredentialDO row = requiredCredential(scopedTenantId, credentialKey);
        MarketingMonitorProviderCredentialRevokeCommand effectiveCommand = command == null
                ? new MarketingMonitorProviderCredentialRevokeCommand(null, null, null, null, null)
                : command;
        try {
            String endpoint = validateOptionalUri(
                    defaultString(effectiveCommand.revokeEndpoint(), row.getRevokeEndpoint()), "revokeEndpoint");
            if (!hasText(endpoint)) {
                throw new IllegalStateException("revoke endpoint is required");
            }
            MarketingMonitorProviderHttpResponse response =
                    refreshTransport.execute(revokeRequest(row, endpoint, effectiveCommand));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return revokeFailed(scopedTenantId, row,
                        "provider revoke failed with status " + response.statusCode(),
                        actor,
                        revokeMetadata(effectiveCommand, response, null));
            }
            LocalDateTime changedAt = now();
            if (!Boolean.FALSE.equals(effectiveCommand.disableAfterRevoke())) {
                row.setStatus(STATUS_DISABLED);
            }
            row.setRevokeEndpoint(endpoint);
            row.setRevokedAt(changedAt);
            row.setLastRevokeStatus("SUCCESS");
            row.setLastRevokeError(null);
            row.setUpdatedBy(defaultActor(actor));
            row.setUpdatedAt(changedAt);
            credentialMapper.updateById(row);
            writeEvent(scopedTenantId, row, "REVOKED", "SUCCESS",
                    revokeMetadata(effectiveCommand, response, revokeTokenTypeHint(row, effectiveCommand)),
                    null,
                    actor);
            return toView(row);
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (RuntimeException ex) {
            return revokeFailed(scopedTenantId, row, message(ex), actor,
                    revokeMetadata(effectiveCommand, null, null));
        }
    }

    /**
     * 关闭或解析业务异常，作为营销监控的服务入口。
     * <p>调用方必须传入租户上下文或租户 ID，方法内的查询、写入和治理判断都限制在该租户范围内。
     * @param tenantId 租户 ID，所有查询和写入都限定在该租户数据范围内
     * @param reference reference 参数，用于 resolveValue 流程中的校验、计算或对象转换。
     * @return 返回业务计算后的字符串结果
     */
    public String resolveValue(Long tenantId, String reference) {
        CredentialReference parsed = parseReference(reference);
        MarketingMonitorProviderCredentialDO row = requiredCredential(normalizeTenant(tenantId), parsed.credentialKey());
        ensureEnabled(row);
        String value = switch (parsed.field()) {
            case "access_token" -> secretCipher.decrypt(row.getAccessTokenCiphertext());
            case "refresh_token" -> secretCipher.decrypt(row.getRefreshTokenCiphertext());
            case "api_key" -> secretCipher.decrypt(row.getApiKeyCiphertext());
            case "client_id" -> secretCipher.decrypt(row.getClientIdCiphertext());
            case "client_secret" -> secretCipher.decrypt(row.getClientSecretCiphertext());
            default -> throw new IllegalArgumentException("unsupported credential field: " + parsed.field());
        };
        if (!hasText(value)) {
            throw new IllegalStateException("credential value is not available: " + reference);
        }
        return value;
    }

    /**
     * 执行业务操作 events，作为营销监控的服务入口。
     * <p>调用方必须传入租户上下文或租户 ID，方法内的查询、写入和治理判断都限制在该租户范围内。
     * @param tenantId 租户 ID，所有查询和写入都限定在该租户数据范围内
     * @param query query 参数，用于 events 流程中的校验、计算或对象转换。
     * @return 返回按租户、状态和数量限制过滤后的视图列表；无数据时返回空列表
     */
    public List<MarketingMonitorProviderCredentialEventView> events(
            Long tenantId,
            MarketingMonitorProviderCredentialEventQuery query) {
        // 准备本次流程的上下文、默认值和中间结果。
        Long scopedTenantId = normalizeTenant(tenantId);
        MarketingMonitorProviderCredentialEventQuery effectiveQuery = query == null
                ? new MarketingMonitorProviderCredentialEventQuery(null, null, null, 50)
                : query;
        int limit = boundedLimit(effectiveQuery.limit());
        String credentialKey = normalizeOptionalKey(effectiveQuery.credentialKey());
        String eventType = normalizeOptionalUpper(effectiveQuery.eventType());
        String status = normalizeOptionalUpper(effectiveQuery.status());
        // 访问持久化数据，读取现有配置或写入本次变更。
        return safeList(eventMapper.selectList(new LambdaQueryWrapper<MarketingMonitorProviderCredentialEventDO>()
                        .eq(MarketingMonitorProviderCredentialEventDO::getTenantId, scopedTenantId)
                        .eq(credentialKey != null,
                                MarketingMonitorProviderCredentialEventDO::getCredentialKey, credentialKey)
                        .eq(eventType != null, MarketingMonitorProviderCredentialEventDO::getEventType, eventType)
                        .eq(status != null, MarketingMonitorProviderCredentialEventDO::getStatus, status)
                        .orderByDesc(MarketingMonitorProviderCredentialEventDO::getCreatedAt)
                        // 遍历候选记录并转换为前端或服务层需要的视图。
                        .last("LIMIT " + limit))).stream()
                .filter(row -> scopedTenantId.equals(row.getTenantId()))
                .filter(row -> credentialKey == null || credentialKey.equals(row.getCredentialKey()))
                .filter(row -> eventType == null || eventType.equals(row.getEventType()))
                .filter(row -> status == null || status.equals(row.getStatus()))
                .limit(limit)
                .map(this::toEventView)
                .toList();
    }

    /**
     * 执行核心业务流程，并协调依赖组件完成处理。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @return 返回 refreshRequest 流程生成的业务结果。
     */
    private MarketingMonitorProviderHttpRequest refreshRequest(MarketingMonitorProviderCredentialDO row) {
        // 准备本次处理所需的上下文和中间变量。
        String endpoint = validateOptionalUri(row.getRefreshEndpoint(), "refreshEndpoint");
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (!hasText(endpoint)) {
            throw new IllegalStateException("refresh endpoint is required");
        }
        String refreshToken = secretCipher.decrypt(row.getRefreshTokenCiphertext());
        String clientId = secretCipher.decrypt(row.getClientIdCiphertext());
        String clientSecret = secretCipher.decrypt(row.getClientSecretCiphertext());
        if (!hasText(refreshToken)) {
            throw new IllegalStateException("refresh token is required");
        }
        if (!hasText(clientId)) {
            throw new IllegalStateException("client id is required");
        }
        Map<String, String> form = new LinkedHashMap<>();
        form.put("grant_type", "refresh_token");
        form.put("refresh_token", refreshToken);
        form.put("client_id", clientId);
        if (hasText(clientSecret)) {
            form.put("client_secret", clientSecret);
        }
        // 汇总前面计算出的状态和明细，返回给调用方。
        return new MarketingMonitorProviderHttpRequest(
                "POST",
                URI.create(endpoint),
                Map.of(
                        "Content-Type", "application/x-www-form-urlencoded",
                        "Accept", "application/json"),
                form(form));
    }

    /**
     * 执行 revokeRequest 流程，围绕 revoke request 完成校验、计算或结果组装。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @param endpoint endpoint 参数，用于 revokeRequest 流程中的校验、计算或对象转换。
     * @param command 命令对象，描述本次业务动作及其参数。
     * @return 返回 revokeRequest 流程生成的业务结果。
     */
    private MarketingMonitorProviderHttpRequest revokeRequest(MarketingMonitorProviderCredentialDO row,
                                                              String endpoint,
                                                              MarketingMonitorProviderCredentialRevokeCommand command) {
        RevokeToken revokeToken = revokeToken(row, command);
        Map<String, String> form = new LinkedHashMap<>();
        form.put("token", revokeToken.token());
        form.put("token_type_hint", revokeToken.tokenTypeHint());
        String clientId = secretCipher.decrypt(row.getClientIdCiphertext());
        String clientSecret = secretCipher.decrypt(row.getClientSecretCiphertext());
        if (hasText(clientId)) {
            form.put("client_id", clientId);
        }
        if (hasText(clientSecret)) {
            form.put("client_secret", clientSecret);
        }
        return new MarketingMonitorProviderHttpRequest(
                "POST",
                URI.create(endpoint),
                Map.of(
                        "Content-Type", "application/x-www-form-urlencoded",
                        "Accept", "application/json"),
                form(form));
    }

    /**
     * 执行核心业务流程，并协调依赖组件完成处理。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param row 持久化行数据，承载数据库记录内容。
     * @param attemptCount attempt count 参数，用于 refreshFailed 流程中的校验、计算或对象转换。
     * @param error error 参数，用于 refreshFailed 流程中的校验、计算或对象转换。
     * @param actor 操作人标识，用于审计和权限判断。
     * @param metadata metadata 参数，用于 refreshFailed 流程中的校验、计算或对象转换。
     * @return 返回 refreshFailed 流程生成的业务结果。
     */
    private MarketingMonitorProviderCredentialView refreshFailed(Long tenantId,
                                                                 MarketingMonitorProviderCredentialDO row,
                                                                 int attemptCount,
                                                                 String error,
                                                                 String actor,
                                                                 Map<String, Object> metadata) {
        // 准备本次处理所需的上下文和中间变量。
        LocalDateTime changedAt = now();
        row.setRefreshAttemptCount(attemptCount);
        row.setLastRefreshStatus("FAILED");
        row.setLastRefreshError(trimLength(error, 1000));
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        row.setUpdatedBy(defaultActor(actor));
        row.setUpdatedAt(changedAt);
        credentialMapper.updateById(row);
        writeEvent(tenantId, row, "REFRESH_FAILED", "FAILED", metadata, row.getLastRefreshError(), actor);
        // 汇总前面计算出的状态和明细，返回给调用方。
        return toView(row);
    }

    /**
     * 执行 revokeFailed 流程，围绕 revoke failed 完成校验、计算或结果组装。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param row 持久化行数据，承载数据库记录内容。
     * @param error error 参数，用于 revokeFailed 流程中的校验、计算或对象转换。
     * @param actor 操作人标识，用于审计和权限判断。
     * @param metadata metadata 参数，用于 revokeFailed 流程中的校验、计算或对象转换。
     * @return 返回 revokeFailed 流程生成的业务结果。
     */
    private MarketingMonitorProviderCredentialView revokeFailed(Long tenantId,
                                                                MarketingMonitorProviderCredentialDO row,
                                                                String error,
                                                                String actor,
                                                                Map<String, Object> metadata) {
        // 准备本次处理所需的上下文和中间变量。
        LocalDateTime changedAt = now();
        row.setLastRevokeStatus("FAILED");
        row.setLastRevokeError(trimLength(error, 1000));
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        row.setUpdatedBy(defaultActor(actor));
        row.setUpdatedAt(changedAt);
        credentialMapper.updateById(row);
        writeEvent(tenantId, row, "REVOKE_FAILED", "FAILED", metadata, row.getLastRevokeError(), actor);
        // 汇总前面计算出的状态和明细，返回给调用方。
        return toView(row);
    }

    /**
     * 校验并获取必需参数、资源或权限。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param credentialKey 业务键，用于在同一租户下定位资源。
     * @return 返回 requiredCredential 流程生成的业务结果。
     */
    private MarketingMonitorProviderCredentialDO requiredCredential(Long tenantId, String credentialKey) {
        String normalizedKey = normalizeKey(credentialKey, "credentialKey");
        MarketingMonitorProviderCredentialDO row = findCredential(tenantId, normalizedKey);
        if (row == null) {
            throw new IllegalArgumentException("provider credential is not found: " + normalizedKey);
        }
        return row;
    }

    /**
     * 查询或读取业务数据。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param credentialKey 业务键，用于在同一租户下定位资源。
     * @return 返回符合条件的数据列表或视图。
     */
    private MarketingMonitorProviderCredentialDO findCredential(Long tenantId, String credentialKey) {
        return credentialMapper.selectOne(new LambdaQueryWrapper<MarketingMonitorProviderCredentialDO>()
                .eq(MarketingMonitorProviderCredentialDO::getTenantId, tenantId)
                .eq(MarketingMonitorProviderCredentialDO::getCredentialKey, credentialKey)
                .last("LIMIT 1"));
    }

    /**
     * 写入或更新业务数据，并保持关联状态一致。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param row 持久化行数据，承载数据库记录内容。
     * @param eventType 类型标识，用于选择对应处理分支。
     * @param status 业务状态，用于筛选或推进状态流转。
     * @param metadata metadata 参数，用于 writeEvent 流程中的校验、计算或对象转换。
     * @param error error 参数，用于 writeEvent 流程中的校验、计算或对象转换。
     * @param actor 操作人标识，用于审计和权限判断。
     */
    private void writeEvent(Long tenantId,
                            MarketingMonitorProviderCredentialDO row,
                            String eventType,
                            String status,
                            Map<String, Object> metadata,
                            String error,
                            String actor) {
        MarketingMonitorProviderCredentialEventDO event = new MarketingMonitorProviderCredentialEventDO();
        event.setTenantId(tenantId);
        event.setCredentialId(row.getId());
        event.setCredentialKey(row.getCredentialKey());
        event.setEventType(eventType);
        event.setStatus(status);
        event.setMetadataJson(json(safeMap(metadata)));
        event.setErrorMessage(trimLength(error, 1000));
        event.setCreatedBy(defaultActor(actor));
        event.setCreatedAt(now());
        eventMapper.insert(event);
    }

    /**
     * 应用请求中的业务字段或租户约束。
     *
     * @param raw raw 参数，用于 applySecret 流程中的校验、计算或对象转换。
     * @param prefixSetter prefix setter 参数，用于 applySecret 流程中的校验、计算或对象转换。
     * @param hashSetter hash setter 参数，用于 applySecret 流程中的校验、计算或对象转换。
     * @param ciphertextSetter ciphertext setter 参数，用于 applySecret 流程中的校验、计算或对象转换。
     */
    private void applySecret(String raw,
                             java.util.function.Consumer<String> prefixSetter,
                             java.util.function.Consumer<String> hashSetter,
                             java.util.function.Consumer<String> ciphertextSetter) {
        if (!hasText(raw)) {
            return;
        }
        String value = raw.trim();
        prefixSetter.accept(value.substring(0, Math.min(SECRET_PREFIX_LENGTH, value.length())));
        hashSetter.accept(passwordEncoder.encode(value));
        ciphertextSetter.accept(secretCipher.encrypt(value));
    }

    /**
     * 转换为接口返回或领域视图。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @return 返回组装或转换后的结果对象。
     */
    private MarketingMonitorProviderCredentialView toView(MarketingMonitorProviderCredentialDO row) {
        // 汇总前面计算出的状态和明细，返回给调用方。
        return new MarketingMonitorProviderCredentialView(
                row.getId(),
                row.getTenantId(),
                row.getCredentialKey(),
                row.getProviderType(),
                row.getAuthType(),
                row.getDisplayName(),
                row.getStatus(),
                row.getTokenType(),
                stringList(row.getScopesJson()),
                row.getAccessTokenPrefix(),
                row.getRefreshTokenPrefix(),
                row.getApiKeyPrefix(),
                row.getRefreshEndpoint(),
                row.getRevokeEndpoint(),
                row.getExpiresAt(),
                row.getRefreshTokenExpiresAt(),
                row.getRevokedAt(),
                row.getLastRefreshedAt(),
                defaultInt(row.getRefreshAttemptCount()),
                row.getLastRefreshStatus(),
                row.getLastRefreshError(),
                row.getLastRevokeStatus(),
                row.getLastRevokeError(),
                map(row.getMetadataJson()),
                row.getCreatedBy(),
                // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
                row.getUpdatedBy(),
                row.getCreatedAt(),
                row.getUpdatedAt());
    }

    /**
     * 转换为接口返回或领域视图。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @return 返回组装或转换后的结果对象。
     */
    private MarketingMonitorProviderCredentialEventView toEventView(MarketingMonitorProviderCredentialEventDO row) {
        return new MarketingMonitorProviderCredentialEventView(
                row.getId(),
                row.getTenantId(),
                row.getCredentialId(),
                row.getCredentialKey(),
                row.getEventType(),
                row.getStatus(),
                map(row.getMetadataJson()),
                row.getErrorMessage(),
                row.getCreatedBy(),
                row.getCreatedAt());
    }

    /**
     * 校验输入、权限或业务前置条件。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     */
    private void ensureEnabled(MarketingMonitorProviderCredentialDO row) {
        if (STATUS_DISABLED.equals(normalizeOptionalUpper(row.getStatus()))) {
            throw new IllegalStateException("provider credential is disabled: " + row.getCredentialKey());
        }
    }

    /**
     * 解析并校验输入数据。
     *
     * @param reference reference 参数，用于 parseReference 流程中的校验、计算或对象转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private CredentialReference parseReference(String reference) {
        if (!hasText(reference)) {
            throw new IllegalArgumentException("credential reference is required");
        }
        String[] parts = reference.trim().split(":", 3);
        if (parts.length != 3 || !"credential".equals(parts[0])) {
            throw new IllegalArgumentException("credential reference must use credential:<key>:<field>");
        }
        return new CredentialReference(normalizeKey(parts[1], "credentialKey"),
                normalizeKey(parts[2], "field").replace('-', '_'));
    }

    /**
     * 执行核心业务流程，并协调依赖组件完成处理。
     *
     * @param body 待处理业务值，用于规则计算、转换或外部调用。
     * @return 返回 refresh scopes 汇总后的集合、分页或映射视图。
     */
    private List<String> refreshScopes(JsonNode body) {
        String scope = text(body, "scope");
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (hasText(scope)) {
            List<String> scopes = new ArrayList<>();
            // 遍历候选数据并按业务规则筛选、转换或聚合。
            for (String value : scope.trim().split("\\s+")) {
                if (hasText(value)) {
                    scopes.add(value.trim());
                }
            }
            return scopes;
        }
        JsonNode scopes = body.path("scopes");
        if (scopes.isArray()) {
            List<String> values = new ArrayList<>();
            scopes.forEach(node -> {
                if (hasText(node.asText())) {
                    values.add(node.asText().trim());
                }
            });
            return values;
        }
        // 汇总前面计算出的状态和明细，返回给调用方。
        return List.of();
    }

    /**
     * 执行 revokeToken 流程，围绕 revoke token 完成校验、计算或结果组装。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @param command 命令对象，描述本次业务动作及其参数。
     * @return 返回 revokeToken 流程生成的业务结果。
     */
    private RevokeToken revokeToken(MarketingMonitorProviderCredentialDO row,
                                    MarketingMonitorProviderCredentialRevokeCommand command) {
        boolean preferRefreshToken = !Boolean.FALSE.equals(command.revokeRefreshToken());
        String token = preferRefreshToken ? secretCipher.decrypt(row.getRefreshTokenCiphertext()) : null;
        String defaultHint = "refresh_token";
        if (!hasText(token)) {
            token = secretCipher.decrypt(row.getAccessTokenCiphertext());
            defaultHint = "access_token";
        }
        if (!hasText(token)) {
            throw new IllegalStateException("credential token is required for revoke");
        }
        return new RevokeToken(token, defaultString(command.tokenTypeHint(), defaultHint));
    }

    /**
     * 执行 revokeTokenTypeHint 流程，围绕 revoke token type hint 完成校验、计算或结果组装。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @param command 命令对象，描述本次业务动作及其参数。
     * @return 返回 revoke token type hint 生成的文本或业务键。
     */
    private String revokeTokenTypeHint(MarketingMonitorProviderCredentialDO row,
                                       MarketingMonitorProviderCredentialRevokeCommand command) {
        return revokeToken(row, command).tokenTypeHint();
    }

    /**
     * 执行 revokeMetadata 流程，围绕 revoke metadata 完成校验、计算或结果组装。
     *
     * @param command 命令对象，描述本次业务动作及其参数。
     * @param response response 参数，用于 revokeMetadata 流程中的校验、计算或对象转换。
     * @param tokenTypeHint token type hint 参数，用于 revokeMetadata 流程中的校验、计算或对象转换。
     * @return 返回 revokeMetadata 流程生成的业务结果。
     */
    private Map<String, Object> revokeMetadata(MarketingMonitorProviderCredentialRevokeCommand command,
                                               MarketingMonitorProviderHttpResponse response,
                                               String tokenTypeHint) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (response != null) {
            metadata.put("httpStatus", response.statusCode());
            String requestId = response.headers().get("x-request-id");
            if (hasText(requestId)) {
                metadata.put("providerRequestId", requestId);
            }
        }
        if (hasText(tokenTypeHint)) {
            metadata.put("tokenTypeHint", tokenTypeHint);
        }
        sanitizedMetadata(command == null ? null : command.metadata())
                // 遍历候选数据并按业务规则筛选、转换或聚合。
                .forEach((key, value) -> metadata.put("request_" + key, value));
        // 汇总前面计算出的状态和明细，返回给调用方。
        return metadata;
    }

    /**
     * 执行 sanitizedMetadata 流程，围绕 sanitized metadata 完成校验、计算或结果组装。
     *
     * @param String string 参数，用于 sanitizedMetadata 流程中的校验、计算或对象转换。
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 sanitizedMetadata 流程生成的业务结果。
     */
    private Map<String, Object> sanitizedMetadata(Map<String, Object> value) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        safeMap(value).forEach((key, metadataValue) -> {
            String normalized = key == null ? "" : key.toLowerCase(Locale.ROOT);
            if (!normalized.contains("token")
                    && !normalized.contains("secret")
                    && !normalized.contains("password")
                    && !normalized.contains("api_key")) {
                metadata.put(key, metadataValue);
            }
        });
        return metadata;
    }

    /**
     * 规范化输入值。
     *
     * @param scopes scopes 参数，用于 normalizedScopes 流程中的校验、计算或对象转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private List<String> normalizedScopes(List<String> scopes) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (scopes == null) {
            return List.of();
        }
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        return scopes.stream()
                .filter(this::hasText)
                .map(String::trim)
                .distinct()
                .toList();
    }

    /**
     * 执行 form 流程，围绕 form 完成校验、计算或结果组装。
     *
     * @param String string 参数，用于 form 流程中的校验、计算或对象转换。
     * @param values values 参数，用于 form 流程中的校验、计算或对象转换。
     * @return 返回 form 生成的文本或业务键。
     */
    private String form(Map<String, String> values) {
        StringBuilder builder = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, String> entry : values.entrySet()) {
            if (!first) {
                builder.append("&");
            }
            first = false;
            builder.append(urlEncode(entry.getKey()))
                    .append("=")
                    .append(urlEncode(entry.getValue()));
        }
        return builder.toString();
    }

    /**
     * 处理 JSON 序列化或反序列化。
     *
     * @param body 待处理业务值，用于规则计算、转换或外部调用。
     * @return 返回 jsonNode 流程生成的业务结果。
     */
    private JsonNode jsonNode(String body) {
        try {
            return objectMapper.readTree(hasText(body) ? body : "{}");
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("provider refresh response JSON parse failed", ex);
        }
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
            throw new IllegalArgumentException("provider credential JSON serialization failed", ex);
        }
    }

    /**
     * 执行 stringList 流程，围绕 string list 完成校验、计算或结果组装。
     *
     * @param json JSON 字符串，承载结构化配置或明细。
     * @return 返回 string list 汇总后的集合、分页或映射视图。
     */
    private List<String> stringList(String json) {
        if (!hasText(json)) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, STRING_LIST);
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (JsonProcessingException ex) {
            return List.of();
        }
    }

    /**
     * 组装输出结构或完成对象转换。
     *
     * @param json JSON 字符串，承载结构化配置或明细。
     * @return 返回组装或转换后的结果对象。
     */
    private Map<String, Object> map(String json) {
        if (!hasText(json)) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, OBJECT_MAP);
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (JsonProcessingException ex) {
            return Map.of();
        }
    }

    /**
     * 按安全边界裁剪或保护输入值。
     *
     * @param String string 参数，用于 safeMap 流程中的校验、计算或对象转换。
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 safeMap 流程生成的业务结果。
     */
    private Map<String, Object> safeMap(Map<String, Object> value) {
        return value == null ? Map.of() : value;
    }

    /**
     * 按安全边界裁剪或保护输入值。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 safe list 汇总后的集合、分页或映射视图。
     */
    private <T> List<T> safeList(List<T> value) {
        return value == null ? List.of() : value;
    }

    /**
     * 执行 longValue 流程，围绕 long value 完成校验、计算或结果组装。
     *
     * @param node node 参数，用于 longValue 流程中的校验、计算或对象转换。
     * @param field 待处理业务值，用于规则计算、转换或外部调用。
     * @return 返回 long value 计算得到的数量、金额或指标值。
     */
    private Long longValue(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isNumber() ? value.asLong() : null;
    }

    /**
     * 执行 text 流程，围绕 text 完成校验、计算或结果组装。
     *
     * @param node node 参数，用于 text 流程中的校验、计算或对象转换。
     * @param field 待处理业务值，用于规则计算、转换或外部调用。
     * @return 返回 text 生成的文本或业务键。
     */
    private String text(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isMissingNode() || value.isNull() ? null : value.asText();
    }

    /**
     * 校验输入、权限或业务前置条件。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param field 待处理业务值，用于规则计算、转换或外部调用。
     * @return 返回布尔判断结果。
     */
    private String validateOptionalUri(String value, String field) {
        String trimmed = trimToNull(value);
        if (trimmed == null) {
            return null;
        }
        URI uri = URI.create(trimmed);
        String scheme = uri.getScheme();
        if (!"https".equalsIgnoreCase(scheme) && !"http".equalsIgnoreCase(scheme)) {
            throw new IllegalArgumentException(field + " must be an HTTP URL");
        }
        return trimmed;
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
     * @param field 待处理业务值，用于规则计算、转换或外部调用。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private String normalizeKey(String value, String field) {
        if (!hasText(value)) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }

    /**
     * 规范化输入值。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private String normalizeOptionalKey(String value) {
        return hasText(value) ? value.trim().toLowerCase(Locale.ROOT) : null;
    }

    /**
     * 规范化输入值。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param field 待处理业务值，用于规则计算、转换或外部调用。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private String normalizeType(String value, String field) {
        if (!hasText(value)) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }

    /**
     * 规范化输入值。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private String normalizeOptionalUpper(String value) {
        return hasText(value) ? value.trim().toUpperCase(Locale.ROOT) : null;
    }

    /**
     * 规范化输入值。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private String normalizeTokenType(String value) {
        String trimmed = trimToNull(value);
        if (trimmed == null) {
            return null;
        }
        return "bearer".equalsIgnoreCase(trimmed) ? "Bearer" : trimmed;
    }

    /**
     * 按默认值规则处理输入值。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param fallback fallback 参数，用于 defaultString 流程中的校验、计算或对象转换。
     * @return 返回 default string 生成的文本或业务键。
     */
    private String defaultString(String value, String fallback) {
        return hasText(value) ? value.trim() : fallback;
    }

    /**
     * 解析、归一化或保护输入值，生成安全可用的中间结果。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private String trimToNull(String value) {
        return hasText(value) ? value.trim() : null;
    }

    /**
     * 解析操作人标识。
     *
     * @param actor 操作人标识，用于审计和权限判断。
     * @return 返回 default actor 生成的文本或业务键。
     */
    private String defaultActor(String actor) {
        return hasText(actor) ? actor.trim() : "system";
    }

    /**
     * 按默认值规则处理输入值。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 default int 计算得到的数量、金额或指标值。
     */
    private int defaultInt(Integer value) {
        return value == null ? 0 : value;
    }

    /**
     * 按安全边界裁剪或保护输入值。
     *
     * @param limit 分页或数量限制，避免一次处理过多数据。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private int boundedLimit(int limit) {
        if (limit < 1) {
            return 1;
        }
        return Math.min(limit, 100);
    }

    /**
     * 按安全边界裁剪或保护输入值。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private int boundedWindowMinutes(Integer value) {
        int minutes = value == null ? 30 : value;
        if (minutes < 1) {
            return 1;
        }
        return Math.min(minutes, 24 * 60);
    }

    /**
     * 解析、归一化或保护输入值，生成安全可用的中间结果。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param maxLength max length 参数，用于 trimLength 流程中的校验、计算或对象转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private String trimLength(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    /**
     * 执行 message 流程，围绕 message 完成校验、计算或结果组装。
     *
     * @param ex ex 参数，用于 message 流程中的校验、计算或对象转换。
     * @return 返回 message 生成的文本或业务键。
     */
    private String message(RuntimeException ex) {
        return ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage();
    }

    /**
     * 执行 string 流程，围绕 string 完成校验、计算或结果组装。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 string 生成的文本或业务键。
     */
    private String string(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    /**
     * 执行 urlEncode 流程，围绕 url encode 完成校验、计算或结果组装。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 url encode 生成的文本或业务键。
     */
    private String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
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
    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    /**
     * CredentialReference 数据记录。
     */
    private record CredentialReference(String credentialKey, String field) {
    }

    /**
     * RevokeToken 数据记录。
     */
    private record RevokeToken(String token, String tokenTypeHint) {
    }
}
