package org.chovy.canvas.domain.monitoring;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.dal.dataobject.MarketingMonitorProviderOAuthAuthorizationDO;
import org.chovy.canvas.dal.dataobject.MarketingMonitorProviderOAuthAuthorizationEventDO;
import org.chovy.canvas.dal.mapper.MarketingMonitorProviderOAuthAuthorizationEventMapper;
import org.chovy.canvas.dal.mapper.MarketingMonitorProviderOAuthAuthorizationMapper;
import org.chovy.canvas.security.SecretCipher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * MarketingMonitorProviderOAuthAuthorizationService 编排 domain.monitoring 场景的领域业务规则。
 */
@Service
public class MarketingMonitorProviderOAuthAuthorizationService {

    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_EXCHANGED = "EXCHANGED";
    private static final String STATUS_FAILED = "FAILED";
    private static final String STATUS_EXPIRED = "EXPIRED";
    private static final String CODE_CHALLENGE_METHOD = "S256";
    private static final Set<String> RESERVED_AUTHORIZE_PARAMS = Set.of(
            "response_type",
            "client_id",
            "redirect_uri",
            "scope",
            "state",
            "code_challenge",
            "code_challenge_method");
    private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {
    };
    private static final TypeReference<Map<String, Object>> OBJECT_MAP = new TypeReference<>() {
    };

    private final MarketingMonitorProviderOAuthAuthorizationMapper authorizationMapper;
    private final MarketingMonitorProviderOAuthAuthorizationEventMapper eventMapper;
    private final MarketingMonitorProviderCredentialService credentialService;
    private final MarketingMonitorProviderHttpTransport tokenTransport;
    private final ObjectMapper objectMapper;
    private final SecretCipher secretCipher;
    private final Clock clock;
    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * 创建 MarketingMonitorProviderOAuthAuthorizationService 实例并注入 domain.monitoring 场景依赖。
     * @param authorizationMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param eventMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param credentialService 依赖组件，用于完成数据访问或外部能力调用。
     * @param tokenTransport token transport 参数，用于 MarketingMonitorProviderOAuthAuthorizationService 流程中的校验、计算或对象转换。
     * @param objectMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param secretCipher secret cipher 参数，用于 MarketingMonitorProviderOAuthAuthorizationService 流程中的校验、计算或对象转换。
     */
    @Autowired
    public MarketingMonitorProviderOAuthAuthorizationService(
            MarketingMonitorProviderOAuthAuthorizationMapper authorizationMapper,
            MarketingMonitorProviderOAuthAuthorizationEventMapper eventMapper,
            MarketingMonitorProviderCredentialService credentialService,
            MarketingMonitorProviderHttpTransport tokenTransport,
            ObjectMapper objectMapper,
            SecretCipher secretCipher) {
        this(authorizationMapper, eventMapper, credentialService, tokenTransport, objectMapper, secretCipher,
                Clock.systemDefaultZone());
    }

    /**
     * 执行 MarketingMonitorProviderOAuthAuthorizationService 流程，围绕 marketing monitor provider oauth authorization service 完成校验、计算或结果组装。
     *
     * @param authorizationMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param eventMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param credentialService 依赖组件，用于完成数据访问或外部能力调用。
     * @param tokenTransport token transport 参数，用于 MarketingMonitorProviderOAuthAuthorizationService 流程中的校验、计算或对象转换。
     * @param objectMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param secretCipher secret cipher 参数，用于 MarketingMonitorProviderOAuthAuthorizationService 流程中的校验、计算或对象转换。
     * @param clock 时间参数，用于计算窗口、过期或审计时间。
     */
    MarketingMonitorProviderOAuthAuthorizationService(
            MarketingMonitorProviderOAuthAuthorizationMapper authorizationMapper,
            MarketingMonitorProviderOAuthAuthorizationEventMapper eventMapper,
            MarketingMonitorProviderCredentialService credentialService,
            MarketingMonitorProviderHttpTransport tokenTransport,
            ObjectMapper objectMapper,
            SecretCipher secretCipher,
            Clock clock) {
        this.authorizationMapper = authorizationMapper;
        this.eventMapper = eventMapper;
        this.credentialService = credentialService;
        this.tokenTransport = tokenTransport;
        this.objectMapper = objectMapper == null ? new ObjectMapper() : objectMapper;
        this.secretCipher = secretCipher;
        this.clock = clock == null ? Clock.systemDefaultZone() : clock;
    }

    /**
     * 执行业务操作 startAuthorization，作为营销监控的服务入口。
     * <p>调用方必须传入租户上下文或租户 ID，方法内的查询、写入和治理判断都限制在该租户范围内。
     * 会通过 Mapper 写入、更新或关闭持久化记录；可能与外部供应商、Webhook 或上传交接端点交互。
     * @param tenantId 租户 ID，所有查询和写入都限定在该租户数据范围内
     * @param command 本次操作的业务请求参数，包含目标对象、状态或外部回调载荷
     * @param actor 操作人标识，用于审计字段、状态流转记录或治理追踪
     * @return 返回租户范围内的最新业务视图或持久化对象快照
     */
    @Transactional(rollbackFor = Exception.class)
    public MarketingMonitorProviderOAuthAuthorizationView startAuthorization(
            Long tenantId,
            MarketingMonitorProviderOAuthAuthorizationCommand command,
            String actor) {
        // 校验策略输入和默认值，避免无效配置进入持久化或查询流程。
        if (command == null) {
            throw new IllegalArgumentException("oauth authorization command is required");
        }
        Long scopedTenantId = normalizeTenant(tenantId);
        String state = randomToken();
        String verifier = randomToken();
        String challenge = s256(verifier);
        LocalDateTime changedAt = now();
        MarketingMonitorProviderOAuthAuthorizationDO row = new MarketingMonitorProviderOAuthAuthorizationDO();
        row.setTenantId(scopedTenantId);
        row.setAuthState(state);
        row.setCredentialKey(normalizeKey(command.credentialKey(), "credentialKey"));
        row.setProviderType(normalizeType(command.providerType(), "providerType"));
        row.setAuthType(normalizeType(command.authType(), "authType"));
        row.setDisplayName(defaultString(command.displayName(), row.getCredentialKey()));
        row.setStatus(STATUS_PENDING);
        row.setAuthorizeEndpoint(validateHttpUrl(command.authorizeEndpoint(), "authorizeEndpoint"));
        row.setTokenEndpoint(validateHttpUrl(command.tokenEndpoint(), "tokenEndpoint"));
        row.setRevokeEndpoint(validateOptionalHttpUrl(command.revokeEndpoint(), "revokeEndpoint"));
        row.setRedirectUri(validateHttpUrl(command.redirectUri(), "redirectUri"));
        row.setClientIdCiphertext(secretCipher.encrypt(required(command.clientId(), "clientId")));
        row.setClientSecretCiphertext(hasText(command.clientSecret()) ? secretCipher.encrypt(command.clientSecret().trim()) : null);
        row.setScopesJson(json(normalizedScopes(command.scopes())));
        row.setCodeVerifierCiphertext(secretCipher.encrypt(verifier));
        row.setCodeChallenge(challenge);
        row.setCodeChallengeMethod(CODE_CHALLENGE_METHOD);
        row.setAuthorizeParamsJson(json(validatedAuthorizeParams(command.authorizeParams())));
        row.setExpiresAt(changedAt.plusMinutes(boundedExpires(command.expiresInMinutes())));
        row.setMetadataJson(json(safeMap(command.metadata())));
        row.setCreatedBy(defaultActor(actor));
        // 访问持久化数据，读取现有配置或写入本次变更。
        row.setUpdatedBy(defaultActor(actor));
        row.setCreatedAt(changedAt);
        row.setUpdatedAt(changedAt);
        authorizationMapper.insert(row);
        writeEvent(scopedTenantId, row, "STARTED", STATUS_PENDING, Map.of(
                "providerType", row.getProviderType(),
                "authType", row.getAuthType(),
                "expiresAt", string(row.getExpiresAt())), null, actor);
        return toView(row, authorizationUrl(row));
    }

    /**
     * 执行业务操作 completeAuthorization，作为营销监控的服务入口。
     * <p>调用方必须传入租户上下文或租户 ID，方法内的查询、写入和治理判断都限制在该租户范围内。
     * 会通过 Mapper 写入、更新或关闭持久化记录；可能与外部供应商、Webhook 或上传交接端点交互。
     * @param tenantId 租户 ID，所有查询和写入都限定在该租户数据范围内
     * @param command 本次操作的业务请求参数，包含目标对象、状态或外部回调载荷
     * @param actor 操作人标识，用于审计字段、状态流转记录或治理追踪
     * @return 返回租户范围内的最新业务视图或持久化对象快照
     */
    @Transactional(rollbackFor = Exception.class)
    public MarketingMonitorProviderOAuthAuthorizationView completeAuthorization(
            Long tenantId,
            MarketingMonitorProviderOAuthCallbackCommand command,
            String actor) {
        if (command == null) {
            throw new IllegalArgumentException("oauth callback command is required");
        }
        Long scopedTenantId = normalizeTenant(tenantId);
        MarketingMonitorProviderOAuthAuthorizationDO row =
                requiredAuthorization(scopedTenantId, command.state());
        if (!STATUS_PENDING.equals(row.getStatus())) {
            row.setLastErrorMessage("oauth authorization state is not pending");
            return fail(row, scopedTenantId, "DUPLICATE_CALLBACK", STATUS_FAILED, row.getLastErrorMessage(),
                    safeMap(command.metadata()), actor);
        }
        if (row.getExpiresAt() != null && row.getExpiresAt().isBefore(now())) {
            return fail(row, scopedTenantId, "EXPIRED", STATUS_EXPIRED,
                    /**
                     * 按安全边界裁剪或保护输入值。
                     *
                     * @param actor 操作人标识，用于审计和权限判断。
                     * @return 返回 safeMap 流程生成的业务结果。
                     */
                    "oauth authorization state expired", safeMap(command.metadata()), actor);
        }
        if (hasText(command.error())) {
            row.setProviderError(command.error().trim());
            row.setProviderErrorDescription(trimLength(command.errorDescription(), 1000));
            return fail(row, scopedTenantId, "CALLBACK_FAILED", STATUS_FAILED,
                    defaultString(command.errorDescription(), command.error()),
                    safeMap(command.metadata()), actor);
        }
        String code = required(command.code(), "code");
        try {
            MarketingMonitorProviderHttpResponse response = tokenTransport.execute(tokenRequest(row, code));
            row.setLastHttpStatus(response.statusCode());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return fail(row, scopedTenantId, "TOKEN_EXCHANGE_FAILED", STATUS_FAILED,
                        "provider token exchange failed with status " + response.statusCode(),
                        eventMetadata(command.metadata(), response, null), actor);
            }
            JsonNode body = jsonNode(response.body());
            String accessToken = text(body, "access_token");
            if (!hasText(accessToken)) {
                return fail(row, scopedTenantId, "TOKEN_EXCHANGE_FAILED", STATUS_FAILED,
                        "provider token response did not include access_token",
                        eventMetadata(command.metadata(), response, null), actor);
            }
            MarketingMonitorProviderCredentialCommand credentialCommand =
                    credentialCommand(row, body, accessToken, command.metadata());
            MarketingMonitorProviderCredentialView credential =
                    credentialService.upsert(scopedTenantId, credentialCommand, actor);
            LocalDateTime changedAt = now();
            row.setStatus(STATUS_EXCHANGED);
            row.setCredentialId(credential.id());
            row.setTokenType(credentialCommand.tokenType());
            row.setLastErrorMessage(null);
            row.setCompletedAt(changedAt);
            row.setUpdatedBy(defaultActor(actor));
            row.setUpdatedAt(changedAt);
            authorizationMapper.updateById(row);
            writeEvent(scopedTenantId, row, "EXCHANGED", "SUCCESS",
                    eventMetadata(command.metadata(), response, credential.id()), null, actor);
            return toView(row, null);
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (RuntimeException ex) {
            return fail(row, scopedTenantId, "TOKEN_EXCHANGE_FAILED", STATUS_FAILED,
                    message(ex), safeMap(command.metadata()), actor);
        }
    }

    /**
     * 查询业务列表，作为营销监控的服务入口。
     * <p>调用方必须传入租户上下文或租户 ID，方法内的查询、写入和治理判断都限制在该租户范围内。
     * 不直接修改业务状态，主要读取数据或执行本地规则计算。
     * @param tenantId 租户 ID，所有查询和写入都限定在该租户数据范围内
     * @param query query 参数，用于 list 流程中的校验、计算或对象转换。
     * @return 返回按租户、状态和数量限制过滤后的视图列表；无数据时返回空列表
     */
    public List<MarketingMonitorProviderOAuthAuthorizationView> list(
            Long tenantId,
            MarketingMonitorProviderOAuthAuthorizationQuery query) {
        // 准备本次流程的上下文、默认值和中间结果。
        Long scopedTenantId = normalizeTenant(tenantId);
        MarketingMonitorProviderOAuthAuthorizationQuery effectiveQuery = query == null
                ? new MarketingMonitorProviderOAuthAuthorizationQuery(null, null, null, 50)
                : query;
        int limit = boundedLimit(effectiveQuery.limit());
        String credentialKey = normalizeOptionalKey(effectiveQuery.credentialKey());
        String providerType = normalizeOptionalUpper(effectiveQuery.providerType());
        String status = normalizeOptionalUpper(effectiveQuery.status());
        // 访问持久化数据，读取现有配置或写入本次变更。
        return safeList(authorizationMapper.selectList(new LambdaQueryWrapper<MarketingMonitorProviderOAuthAuthorizationDO>()
                        .eq(MarketingMonitorProviderOAuthAuthorizationDO::getTenantId, scopedTenantId)
                        .eq(credentialKey != null,
                                MarketingMonitorProviderOAuthAuthorizationDO::getCredentialKey, credentialKey)
                        .eq(providerType != null,
                                MarketingMonitorProviderOAuthAuthorizationDO::getProviderType, providerType)
                        .eq(status != null, MarketingMonitorProviderOAuthAuthorizationDO::getStatus, status)
                        .orderByDesc(MarketingMonitorProviderOAuthAuthorizationDO::getCreatedAt)
                        // 遍历候选记录并转换为前端或服务层需要的视图。
                        .last("LIMIT " + limit))).stream()
                .filter(row -> scopedTenantId.equals(row.getTenantId()))
                .filter(row -> credentialKey == null || credentialKey.equals(row.getCredentialKey()))
                .filter(row -> providerType == null || providerType.equals(row.getProviderType()))
                .filter(row -> status == null || status.equals(row.getStatus()))
                .limit(limit)
                .map(row -> toView(row, null))
                .toList();
    }

    /**
     * 执行业务操作 events，作为营销监控的服务入口。
     * <p>调用方必须传入租户上下文或租户 ID，方法内的查询、写入和治理判断都限制在该租户范围内。
     * @param tenantId 租户 ID，所有查询和写入都限定在该租户数据范围内
     * @param query query 参数，用于 events 流程中的校验、计算或对象转换。
     * @return 返回按租户、状态和数量限制过滤后的视图列表；无数据时返回空列表
     */
    public List<MarketingMonitorProviderOAuthAuthorizationEventView> events(
            Long tenantId,
            MarketingMonitorProviderOAuthAuthorizationEventQuery query) {
        // 准备本次流程的上下文、默认值和中间结果。
        Long scopedTenantId = normalizeTenant(tenantId);
        MarketingMonitorProviderOAuthAuthorizationEventQuery effectiveQuery = query == null
                ? new MarketingMonitorProviderOAuthAuthorizationEventQuery(null, null, null, null, 50)
                : query;
        int limit = boundedLimit(effectiveQuery.limit());
        String authState = trimToNull(effectiveQuery.authState());
        String credentialKey = normalizeOptionalKey(effectiveQuery.credentialKey());
        String eventType = normalizeOptionalUpper(effectiveQuery.eventType());
        String status = normalizeOptionalUpper(effectiveQuery.status());
        // 访问持久化数据，读取现有配置或写入本次变更。
        return safeList(eventMapper.selectList(new LambdaQueryWrapper<MarketingMonitorProviderOAuthAuthorizationEventDO>()
                        .eq(MarketingMonitorProviderOAuthAuthorizationEventDO::getTenantId, scopedTenantId)
                        .eq(authState != null,
                                MarketingMonitorProviderOAuthAuthorizationEventDO::getAuthState, authState)
                        .eq(credentialKey != null,
                                MarketingMonitorProviderOAuthAuthorizationEventDO::getCredentialKey, credentialKey)
                        .eq(eventType != null,
                                MarketingMonitorProviderOAuthAuthorizationEventDO::getEventType, eventType)
                        .eq(status != null,
                                MarketingMonitorProviderOAuthAuthorizationEventDO::getStatus, status)
                        .orderByDesc(MarketingMonitorProviderOAuthAuthorizationEventDO::getCreatedAt)
                        // 遍历候选记录并转换为前端或服务层需要的视图。
                        .last("LIMIT " + limit))).stream()
                .filter(row -> scopedTenantId.equals(row.getTenantId()))
                .filter(row -> authState == null || authState.equals(row.getAuthState()))
                .filter(row -> credentialKey == null || credentialKey.equals(row.getCredentialKey()))
                .filter(row -> eventType == null || eventType.equals(row.getEventType()))
                .filter(row -> status == null || status.equals(row.getStatus()))
                .limit(limit)
                .map(this::toEventView)
                .toList();
    }

    /**
     * 推进状态流转并记录本次处理结果。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param eventType 类型标识，用于选择对应处理分支。
     * @param status 业务状态，用于筛选或推进状态流转。
     * @param error error 参数，用于 fail 流程中的校验、计算或对象转换。
     * @param metadata metadata 参数，用于 fail 流程中的校验、计算或对象转换。
     * @param actor 操作人标识，用于审计和权限判断。
     * @return 返回 fail 流程生成的业务结果。
     */
    private MarketingMonitorProviderOAuthAuthorizationView fail(
            MarketingMonitorProviderOAuthAuthorizationDO row,
            Long tenantId,
            String eventType,
            String status,
            String error,
            Map<String, Object> metadata,
            String actor) {
        // 准备本次处理所需的上下文和中间变量。
        LocalDateTime changedAt = now();
        row.setStatus(status);
        row.setLastErrorMessage(trimLength(error, 1000));
        row.setCompletedAt(changedAt);
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        row.setUpdatedBy(defaultActor(actor));
        row.setUpdatedAt(changedAt);
        authorizationMapper.updateById(row);
        writeEvent(tenantId, row, eventType, status, metadata, row.getLastErrorMessage(), actor);
        // 汇总前面计算出的状态和明细，返回给调用方。
        return toView(row, null);
    }

    /**
     * 执行 credentialCommand 流程，围绕 credential command 完成校验、计算或结果组装。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @param body 待处理业务值，用于规则计算、转换或外部调用。
     * @param accessToken 令牌或锁标识，用于鉴权、幂等或并发控制。
     * @param callbackMetadata callback metadata 参数，用于 credentialCommand 流程中的校验、计算或对象转换。
     * @return 返回 credentialCommand 流程生成的业务结果。
     */
    private MarketingMonitorProviderCredentialCommand credentialCommand(
            MarketingMonitorProviderOAuthAuthorizationDO row,
            JsonNode body,
            String accessToken,
            Map<String, Object> callbackMetadata) {
        List<String> scopes = tokenScopes(body);
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (scopes.isEmpty()) {
            scopes = stringList(row.getScopesJson());
        }
        Long expiresIn = longValue(body, "expires_in");
        Map<String, Object> metadata = new LinkedHashMap<>(map(row.getMetadataJson()));
        metadata.put("oauthAuthorizationId", row.getId());
        metadata.put("oauthState", row.getAuthState());
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        safeMap(callbackMetadata).forEach((key, value) -> metadata.put("callback_" + key, value));
        // 汇总前面计算出的状态和明细，返回给调用方。
        return new MarketingMonitorProviderCredentialCommand(
                row.getCredentialKey(),
                row.getProviderType(),
                row.getAuthType(),
                row.getDisplayName(),
                true,
                accessToken,
                text(body, "refresh_token"),
                null,
                normalizeTokenType(defaultString(text(body, "token_type"), "Bearer")),
                scopes,
                expiresIn == null || expiresIn <= 0 ? null : now().plusSeconds(expiresIn),
                null,
                row.getTokenEndpoint(),
                row.getRevokeEndpoint(),
                secretCipher.decrypt(row.getClientIdCiphertext()),
                secretCipher.decrypt(row.getClientSecretCiphertext()),
                metadata);
    }

    /**
     * 转换为接口返回或领域视图。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @param code 业务编码，用于匹配对应类型或状态。
     * @return 返回组装或转换后的结果对象。
     */
    private MarketingMonitorProviderHttpRequest tokenRequest(
            MarketingMonitorProviderOAuthAuthorizationDO row,
            String code) {
        Map<String, String> form = new LinkedHashMap<>();
        form.put("grant_type", "authorization_code");
        form.put("code", code);
        form.put("redirect_uri", row.getRedirectUri());
        form.put("client_id", secretCipher.decrypt(row.getClientIdCiphertext()));
        form.put("code_verifier", secretCipher.decrypt(row.getCodeVerifierCiphertext()));
        String clientSecret = secretCipher.decrypt(row.getClientSecretCiphertext());
        if (hasText(clientSecret)) {
            form.put("client_secret", clientSecret);
        }
        return new MarketingMonitorProviderHttpRequest(
                "POST",
                URI.create(row.getTokenEndpoint()),
                Map.of(
                        "Content-Type", "application/x-www-form-urlencoded",
                        "Accept", "application/json"),
                form(form));
    }

    /**
     * 校验并获取必需参数、资源或权限。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param state state 参数，用于 requiredAuthorization 流程中的校验、计算或对象转换。
     * @return 返回 requiredAuthorization 流程生成的业务结果。
     */
    private MarketingMonitorProviderOAuthAuthorizationDO requiredAuthorization(Long tenantId, String state) {
        String authState = required(state, "state");
        MarketingMonitorProviderOAuthAuthorizationDO row = authorizationMapper.selectOne(
                new LambdaQueryWrapper<MarketingMonitorProviderOAuthAuthorizationDO>()
                        .eq(MarketingMonitorProviderOAuthAuthorizationDO::getTenantId, tenantId)
                        .eq(MarketingMonitorProviderOAuthAuthorizationDO::getAuthState, authState)
                        .last("LIMIT 1"));
        if (row == null) {
            throw new IllegalArgumentException("oauth authorization state is not found");
        }
        return row;
    }

    /**
     * 执行 authorizationUrl 流程，围绕 authorization url 完成校验、计算或结果组装。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @return 返回 authorization url 生成的文本或业务键。
     */
    private String authorizationUrl(MarketingMonitorProviderOAuthAuthorizationDO row) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("response_type", "code");
        params.put("client_id", secretCipher.decrypt(row.getClientIdCiphertext()));
        params.put("redirect_uri", row.getRedirectUri());
        String scope = String.join(" ", stringList(row.getScopesJson()));
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (hasText(scope)) {
            params.put("scope", scope);
        }
        params.put("state", row.getAuthState());
        params.put("code_challenge", row.getCodeChallenge());
        params.put("code_challenge_method", CODE_CHALLENGE_METHOD);
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        map(row.getAuthorizeParamsJson()).forEach((key, value) -> {
            if (value != null && hasText(String.valueOf(value))) {
                params.put(key, String.valueOf(value));
            }
        });
        String separator = row.getAuthorizeEndpoint().contains("?") ? "&" : "?";
        // 汇总前面计算出的状态和明细，返回给调用方。
        return row.getAuthorizeEndpoint() + separator + form(params);
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
                            MarketingMonitorProviderOAuthAuthorizationDO row,
                            String eventType,
                            String status,
                            Map<String, Object> metadata,
                            String error,
                            String actor) {
        MarketingMonitorProviderOAuthAuthorizationEventDO event =
                new MarketingMonitorProviderOAuthAuthorizationEventDO();
        event.setTenantId(tenantId);
        event.setAuthorizationId(row.getId());
        event.setAuthState(row.getAuthState());
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
     * 转换为接口返回或领域视图。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @param authorizationUrl authorization url 参数，用于 toView 流程中的校验、计算或对象转换。
     * @return 返回组装或转换后的结果对象。
     */
    private MarketingMonitorProviderOAuthAuthorizationView toView(
            MarketingMonitorProviderOAuthAuthorizationDO row,
            String authorizationUrl) {
        // 汇总前面计算出的状态和明细，返回给调用方。
        return new MarketingMonitorProviderOAuthAuthorizationView(
                row.getId(),
                row.getTenantId(),
                row.getAuthState(),
                row.getCredentialKey(),
                row.getProviderType(),
                row.getAuthType(),
                row.getDisplayName(),
                row.getStatus(),
                authorizationUrl,
                row.getAuthorizeEndpoint(),
                row.getTokenEndpoint(),
                row.getRedirectUri(),
                stringList(row.getScopesJson()),
                row.getCodeChallengeMethod(),
                row.getCredentialId(),
                row.getProviderError(),
                row.getProviderErrorDescription(),
                row.getLastHttpStatus(),
                row.getLastErrorMessage(),
                row.getExpiresAt(),
                row.getCompletedAt(),
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
    private MarketingMonitorProviderOAuthAuthorizationEventView toEventView(
            MarketingMonitorProviderOAuthAuthorizationEventDO row) {
        return new MarketingMonitorProviderOAuthAuthorizationEventView(
                row.getId(),
                row.getTenantId(),
                row.getAuthorizationId(),
                row.getAuthState(),
                row.getCredentialKey(),
                row.getEventType(),
                row.getStatus(),
                map(row.getMetadataJson()),
                row.getErrorMessage(),
                row.getCreatedBy(),
                row.getCreatedAt());
    }

    /**
     * 执行 eventMetadata 流程，围绕 event metadata 完成校验、计算或结果组装。
     *
     * @param callbackMetadata callback metadata 参数，用于 eventMetadata 流程中的校验、计算或对象转换。
     * @param response response 参数，用于 eventMetadata 流程中的校验、计算或对象转换。
     * @param credentialId 业务对象 ID，用于定位具体记录。
     * @return 返回 eventMetadata 流程生成的业务结果。
     */
    private Map<String, Object> eventMetadata(Map<String, Object> callbackMetadata,
                                              MarketingMonitorProviderHttpResponse response,
                                              Long credentialId) {
        Map<String, Object> result = new LinkedHashMap<>();
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (response != null) {
            result.put("httpStatus", response.statusCode());
            String requestId = response.headers().get("x-request-id");
            if (hasText(requestId)) {
                result.put("providerRequestId", requestId);
            }
        }
        if (credentialId != null) {
            result.put("credentialId", credentialId);
        }
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        safeMap(callbackMetadata).forEach((key, value) -> result.put("callback_" + key, value));
        // 汇总前面计算出的状态和明细，返回给调用方。
        return result;
    }

    /**
     * 校验输入、权限或业务前置条件。
     *
     * @param String string 参数，用于 validatedAuthorizeParams 流程中的校验、计算或对象转换。
     * @param values values 参数，用于 validatedAuthorizeParams 流程中的校验、计算或对象转换。
     * @return 返回布尔判断结果。
     */
    private Map<String, Object> validatedAuthorizeParams(Map<String, Object> values) {
        Map<String, Object> result = new LinkedHashMap<>();
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        safeMap(values).forEach((key, value) -> {
            String normalized = normalizeKey(key, "authorizeParam").replace('-', '_');
            // 校验关键输入和前置条件，避免无效状态继续进入主流程。
            if (RESERVED_AUTHORIZE_PARAMS.contains(normalized)) {
                throw new IllegalArgumentException("authorizeParams cannot override reserved OAuth param: " + key);
            }
            if (value != null && hasText(String.valueOf(value))) {
                result.put(key.trim(), String.valueOf(value).trim());
            }
        });
        // 汇总前面计算出的状态和明细，返回给调用方。
        return result;
    }

    /**
     * 转换为接口返回或领域视图。
     *
     * @param body 待处理业务值，用于规则计算、转换或外部调用。
     * @return 返回组装或转换后的结果对象。
     */
    private List<String> tokenScopes(JsonNode body) {
        String scope = text(body, "scope");
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (!hasText(scope)) {
            return List.of();
        }
        List<String> scopes = new ArrayList<>();
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        for (String value : scope.trim().split("\\s+")) {
            if (hasText(value)) {
                scopes.add(value.trim());
            }
        }
        // 汇总前面计算出的状态和明细，返回给调用方。
        return scopes;
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
            throw new IllegalStateException("provider token response JSON parse failed", ex);
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
            throw new IllegalArgumentException("oauth authorization JSON serialization failed", ex);
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
     * 执行 s256 流程，围绕 s256 完成校验、计算或结果组装。
     *
     * @param verifier verifier 参数，用于 s256 流程中的校验、计算或对象转换。
     * @return 返回 s256 生成的文本或业务键。
     */
    private String s256(String verifier) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(verifier.getBytes(StandardCharsets.US_ASCII));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (Exception ex) {
            throw new IllegalStateException("failed to generate PKCE challenge", ex);
        }
    }

    /**
     * 执行 randomToken 流程，围绕 random token 完成校验、计算或结果组装。
     *
     * @return 返回 random token 生成的文本或业务键。
     */
    private String randomToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
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
    private String validateHttpUrl(String value, String field) {
        String trimmed = required(value, field);
        URI uri = URI.create(trimmed);
        String scheme = uri.getScheme();
        if (!"https".equalsIgnoreCase(scheme) && !"http".equalsIgnoreCase(scheme)) {
            throw new IllegalArgumentException(field + " must be an HTTP URL");
        }
        return trimmed;
    }

    /**
     * 校验输入、权限或业务前置条件。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param field 待处理业务值，用于规则计算、转换或外部调用。
     * @return 返回布尔判断结果。
     */
    private String validateOptionalHttpUrl(String value, String field) {
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
     * 校验并获取必需参数、资源或权限。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param field 待处理业务值，用于规则计算、转换或外部调用。
     * @return 返回 required 生成的文本或业务键。
     */
    private String required(String value, String field) {
        if (!hasText(value)) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
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
     * 解析操作人标识。
     *
     * @param actor 操作人标识，用于审计和权限判断。
     * @return 返回 default actor 生成的文本或业务键。
     */
    private String defaultActor(String actor) {
        return hasText(actor) ? actor.trim() : "system";
    }

    /**
     * 按安全边界裁剪或保护输入值。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private int boundedExpires(Integer value) {
        int minutes = value == null ? 15 : value;
        if (minutes < 5) {
            return 5;
        }
        return Math.min(minutes, 60);
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
}
