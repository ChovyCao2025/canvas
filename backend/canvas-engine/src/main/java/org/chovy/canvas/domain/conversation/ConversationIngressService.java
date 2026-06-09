package org.chovy.canvas.domain.conversation;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.dal.dataobject.ConversationMessageDO;
import org.chovy.canvas.dal.dataobject.ConversationSessionDO;
import org.chovy.canvas.dal.mapper.ConversationMessageMapper;
import org.chovy.canvas.dal.mapper.ConversationSessionMapper;
import org.chovy.canvas.engine.wait.WaitResumeService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * ConversationIngressService 编排 domain.conversation 场景的领域业务规则。
 */
@Service
public class ConversationIngressService {

    public static final String EVENT_CODE = "CONVERSATION_REPLY";
    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_RECORDED = "RECORDED";
    public static final String DIRECTION_INBOUND = "INBOUND";

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final ConversationSessionMapper sessionMapper;
    private final ConversationMessageMapper messageMapper;
    private final WaitResumeService waitResumeService;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    /**
     * 创建 ConversationIngressService 实例并注入 domain.conversation 场景依赖。
     * @param sessionMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param messageMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param waitResumeService 依赖组件，用于完成数据访问或外部能力调用。
     * @param objectMapper 依赖组件，用于完成数据访问或外部能力调用。
     */
    public ConversationIngressService(ConversationSessionMapper sessionMapper,
                                      ConversationMessageMapper messageMapper,
                                      WaitResumeService waitResumeService,
                                      ObjectMapper objectMapper) {
        this(sessionMapper, messageMapper, waitResumeService, objectMapper, Clock.systemDefaultZone());
    }

    /**
     * 执行 ConversationIngressService 流程，围绕 conversation ingress service 完成校验、计算或结果组装。
     *
     * @param sessionMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param messageMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param waitResumeService 依赖组件，用于完成数据访问或外部能力调用。
     * @param objectMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param clock 时间参数，用于计算窗口、过期或审计时间。
     */
    ConversationIngressService(ConversationSessionMapper sessionMapper,
                               ConversationMessageMapper messageMapper,
                               WaitResumeService waitResumeService,
                               ObjectMapper objectMapper,
                               Clock clock) {
        this.sessionMapper = sessionMapper;
        this.messageMapper = messageMapper;
        this.waitResumeService = waitResumeService;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Transactional(rollbackFor = Exception.class)
    /**
     * 接收入站会话消息并写入会话流水。
     * 方法按租户、渠道、Provider 和外部消息生成幂等键，重复消息直接返回；新消息会更新会话上下文并恢复等待 CONVERSATION_REPLY 的执行。
     */
    public ConversationIngressResp ingest(Long tenantId, ConversationIngressReq req) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (req == null) {
            throw new IllegalArgumentException("conversation ingress request is required");
        }
        Long normalizedTenantId = tenantId(tenantId);
        String userId = requireText(req.userId(), "userId is required");
        String channel = normalizeRequired(req.channel(), "channel is required");
        String provider = normalizeOptional(req.provider(), "DEFAULT");
        String messageType = normalizeOptional(req.messageType(), "UNKNOWN");
        LocalDateTime occurredAt = req.occurredAt() == null ? now() : req.occurredAt();
        String idempotencyKey = idempotencyKey(channel, provider, req);

        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        ConversationMessageDO duplicate = messageMapper.selectOne(new LambdaQueryWrapper<ConversationMessageDO>()
                .eq(ConversationMessageDO::getTenantId, normalizedTenantId)
                .eq(ConversationMessageDO::getIdempotencyKey, idempotencyKey)
                .last("LIMIT 1"));
        if (duplicate != null) {
            return new ConversationIngressResp(duplicate.getSessionId(), duplicate.getId(), STATUS_RECORDED, true, 0);
        }

        ConversationSessionDO session = findActiveSession(normalizedTenantId, userId, channel, provider, req.executionId());
        if (session == null) {
            session = createSession(normalizedTenantId, req, userId, channel, provider, occurredAt);
        }

        ConversationMessageDO message = new ConversationMessageDO();
        message.setTenantId(normalizedTenantId);
        message.setSessionId(session.getId());
        message.setDirection(DIRECTION_INBOUND);
        message.setMessageType(messageType);
        message.setExternalMessageId(blankToNull(req.externalMessageId()));
        message.setIdempotencyKey(idempotencyKey);
        message.setContentJson(writeJson(content(req, channel, provider, messageType, occurredAt)));
        message.setTextContent(blankToNull(req.text()));
        message.setIntent(blankToNull(req.intent()));
        message.setProcessed(false);
        message.setCreatedAt(occurredAt);
        messageMapper.insert(message);

        updateSessionAfterMessage(session, req, message, occurredAt);
        int resumed = waitResumeService.resumeEventWaits(
                EVENT_CODE,
                userId,
                eventAttributes(req, channel, provider, messageType, session.getId(), message.getId()),
                eventId(req, idempotencyKey));
        // 汇总前面计算出的状态和明细，返回给调用方。
        return new ConversationIngressResp(session.getId(), message.getId(), STATUS_RECORDED, false, resumed);
    }

    /**
     * 查询租户内最近更新的会话列表。
     * 可按用户和渠道过滤，返回数量受上限保护，用于客服或私域会话侧栏。
     */
    public List<ConversationSessionView> listRecentSessions(Long tenantId, String userId, String channel, int limit) {
        Long normalizedTenantId = tenantId(tenantId);
        int boundedLimit = boundedLimit(limit);
        LambdaQueryWrapper<ConversationSessionDO> query = new LambdaQueryWrapper<ConversationSessionDO>()
                .eq(ConversationSessionDO::getTenantId, normalizedTenantId)
                // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
                .orderByDesc(ConversationSessionDO::getUpdatedAt)
                .last("LIMIT " + boundedLimit);
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (!isBlank(userId)) {
            query.eq(ConversationSessionDO::getUserId, userId.trim());
        }
        if (!isBlank(channel)) {
            query.eq(ConversationSessionDO::getChannel, normalizeRequired(channel, "channel is required"));
        }
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        return sessionMapper.selectList(query).stream()
                .filter(row -> normalizedTenantId.equals(row.getTenantId()))
                .map(this::toSessionView)
                .toList();
    }

    /**
     * 查询租户内指定会话的消息列表。
     * 会先校验会话归属租户，再按时间升序返回有限条消息，避免跨租户读取会话内容。
     */
    public List<ConversationMessageView> listMessages(Long tenantId, Long sessionId, int limit) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (sessionId == null) {
            throw new IllegalArgumentException("sessionId is required");
        }
        Long normalizedTenantId = tenantId(tenantId);
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        ConversationSessionDO session = sessionMapper.selectById(sessionId);
        if (session == null || !normalizedTenantId.equals(session.getTenantId())) {
            throw new IllegalArgumentException("Conversation session not found: " + sessionId);
        }
        return messageMapper.selectList(new LambdaQueryWrapper<ConversationMessageDO>()
                        .eq(ConversationMessageDO::getTenantId, normalizedTenantId)
                        .eq(ConversationMessageDO::getSessionId, sessionId)
                        .orderByAsc(ConversationMessageDO::getCreatedAt)
                        .last("LIMIT " + boundedLimit(limit)))
                // 遍历候选数据并按业务规则筛选、转换或聚合。
                .stream()
                .filter(row -> normalizedTenantId.equals(row.getTenantId()) && sessionId.equals(row.getSessionId()))
                .map(this::toMessageView)
                .toList();
    }

    /**
     * 查询或读取业务数据。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param userId 业务对象 ID，用于定位具体记录。
     * @param channel channel 参数，用于 findActiveSession 流程中的校验、计算或对象转换。
     * @param provider provider 参数，用于 findActiveSession 流程中的校验、计算或对象转换。
     * @param executionId 业务对象 ID，用于定位具体记录。
     * @return 返回符合条件的数据列表或视图。
     */
    private ConversationSessionDO findActiveSession(Long tenantId,
                                                   String userId,
                                                   String channel,
                                                   String provider,
                                                   String executionId) {
        LambdaQueryWrapper<ConversationSessionDO> query = new LambdaQueryWrapper<ConversationSessionDO>()
                .eq(ConversationSessionDO::getTenantId, tenantId)
                .eq(ConversationSessionDO::getUserId, userId)
                .eq(ConversationSessionDO::getChannel, channel)
                .eq(ConversationSessionDO::getProvider, provider)
                .eq(ConversationSessionDO::getStatus, STATUS_ACTIVE)
                .orderByDesc(ConversationSessionDO::getLastMessageAt)
                .last("LIMIT 1");
        if (isBlank(executionId)) {
            query.isNull(ConversationSessionDO::getExecutionId);
        } else {
            query.eq(ConversationSessionDO::getExecutionId, executionId.trim());
        }
        return sessionMapper.selectOne(query);
    }

    /**
     * 执行数据写入或状态变更。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param req 请求对象，承载本次操作的输入参数。
     * @param userId 业务对象 ID，用于定位具体记录。
     * @param channel channel 参数，用于 createSession 流程中的校验、计算或对象转换。
     * @param provider provider 参数，用于 createSession 流程中的校验、计算或对象转换。
     * @param occurredAt 时间参数，用于计算窗口、过期或审计时间。
     * @return 返回流程执行后的业务结果。
     */
    private ConversationSessionDO createSession(Long tenantId,
                                                ConversationIngressReq req,
                                                String userId,
                                                String channel,
                                                String provider,
                                                LocalDateTime occurredAt) {
        ConversationSessionDO session = new ConversationSessionDO();
        session.setTenantId(tenantId);
        session.setCanvasId(req.canvasId());
        session.setVersionId(req.versionId());
        session.setExecutionId(blankToNull(req.executionId()));
        session.setUserId(userId);
        session.setChannel(channel);
        session.setProvider(provider);
        session.setStatus(STATUS_ACTIVE);
        session.setTurnCount(0);
        session.setContextJson("{}");
        session.setLastMessageAt(occurredAt);
        session.setCreatedAt(now());
        session.setUpdatedAt(now());
        sessionMapper.insert(session);
        return session;
    }

    /**
     * 执行数据写入或状态变更。
     *
     * @param session session 参数，用于 updateSessionAfterMessage 流程中的校验、计算或对象转换。
     * @param req 请求对象，承载本次操作的输入参数。
     * @param message 原因或消息文本，用于记录状态变化的业务依据。
     * @param occurredAt 时间参数，用于计算窗口、过期或审计时间。
     */
    private void updateSessionAfterMessage(ConversationSessionDO session,
                                           ConversationIngressReq req,
                                           ConversationMessageDO message,
                                           LocalDateTime occurredAt) {
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        ConversationSessionDO update = new ConversationSessionDO();
        update.setId(session.getId());
        update.setStatus(STATUS_ACTIVE);
        update.setTurnCount((session.getTurnCount() == null ? 0 : session.getTurnCount()) + 1);
        update.setContextJson(writeJson(mergedContext(session, req, message)));
        update.setLastMessageAt(occurredAt);
        update.setUpdatedAt(now());
        sessionMapper.updateById(update);
    }

    /**
     * 处理集合、映射或字段拷贝逻辑。
     *
     * @param session session 参数，用于 mergedContext 流程中的校验、计算或对象转换。
     * @param req 请求对象，承载本次操作的输入参数。
     * @param message 原因或消息文本，用于记录状态变化的业务依据。
     * @return 返回 mergedContext 流程生成的业务结果。
     */
    private Map<String, Object> mergedContext(ConversationSessionDO session,
                                              ConversationIngressReq req,
                                              ConversationMessageDO message) {
        Map<String, Object> context = new LinkedHashMap<>(readMap(session.getContextJson()));
        putIfPresent(context, "intent", req.intent());
        putIfPresent(context, "lastText", req.text());
        context.put("lastMessageId", message.getId());
        if (req.attributes() != null && !req.attributes().isEmpty()) {
            context.put("lastAttributes", req.attributes());
        }
        return context;
    }

    /**
     * 执行 content 流程，围绕 content 完成校验、计算或结果组装。
     *
     * @param req 请求对象，承载本次操作的输入参数。
     * @param channel channel 参数，用于 content 流程中的校验、计算或对象转换。
     * @param provider provider 参数，用于 content 流程中的校验、计算或对象转换。
     * @param messageType 类型标识，用于选择对应处理分支。
     * @param occurredAt 时间参数，用于计算窗口、过期或审计时间。
     * @return 返回 content 流程生成的业务结果。
     */
    private Map<String, Object> content(ConversationIngressReq req,
                                        String channel,
                                        String provider,
                                        String messageType,
                                        LocalDateTime occurredAt) {
        Map<String, Object> content = new LinkedHashMap<>();
        content.put("channel", channel);
        content.put("provider", provider);
        content.put("messageType", messageType);
        putIfPresent(content, "externalMessageId", req.externalMessageId());
        putIfPresent(content, "eventId", req.eventId());
        putIfPresent(content, "executionId", req.executionId());
        putIfPresent(content, "text", req.text());
        putIfPresent(content, "intent", req.intent());
        content.put("attributes", req.attributes() == null ? Map.of() : req.attributes());
        content.put("occurredAt", occurredAt.toString());
        return content;
    }

    /**
     * 执行 eventAttributes 流程，围绕 event attributes 完成校验、计算或结果组装。
     *
     * @param req 请求对象，承载本次操作的输入参数。
     * @param channel channel 参数，用于 eventAttributes 流程中的校验、计算或对象转换。
     * @param provider provider 参数，用于 eventAttributes 流程中的校验、计算或对象转换。
     * @param messageType 类型标识，用于选择对应处理分支。
     * @param sessionId 业务对象 ID，用于定位具体记录。
     * @param messageId 业务对象 ID，用于定位具体记录。
     * @return 返回 eventAttributes 流程生成的业务结果。
     */
    private Map<String, Object> eventAttributes(ConversationIngressReq req,
                                                String channel,
                                                String provider,
                                                String messageType,
                                                Long sessionId,
                                                Long messageId) {
        // 准备本次处理所需的上下文和中间变量。
        Map<String, Object> attributes = new LinkedHashMap<>();
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (req.attributes() != null) {
            attributes.putAll(req.attributes());
        }
        attributes.put("channel", channel);
        attributes.put("provider", provider);
        attributes.put("messageType", messageType);
        putIfPresent(attributes, "text", req.text());
        putIfPresent(attributes, "intent", req.intent());
        putIfPresent(attributes, "externalMessageId", req.externalMessageId());
        putIfPresent(attributes, "eventId", req.eventId());
        putIfPresent(attributes, "executionId", req.executionId());
        attributes.put("canvasId", req.canvasId());
        attributes.put("versionId", req.versionId());
        attributes.put("sessionId", sessionId);
        attributes.put("messageId", messageId);
        attributes.put("conversationSessionId", sessionId);
        attributes.put("conversationMessageId", messageId);
        attributes.put("attributes", req.attributes() == null ? Map.of() : req.attributes());
        // 汇总前面计算出的状态和明细，返回给调用方。
        return attributes;
    }

    /**
     * 转换为接口返回或领域视图。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @return 返回组装或转换后的结果对象。
     */
    private ConversationSessionView toSessionView(ConversationSessionDO row) {
        return new ConversationSessionView(
                row.getId(),
                row.getTenantId(),
                row.getCanvasId(),
                row.getVersionId(),
                row.getExecutionId(),
                row.getUserId(),
                row.getChannel(),
                row.getProvider(),
                row.getStatus(),
                row.getTurnCount() == null ? 0 : row.getTurnCount(),
                readMap(row.getContextJson()),
                row.getLastMessageAt(),
                row.getExpiresAt(),
                row.getCreatedAt(),
                row.getUpdatedAt());
    }

    /**
     * 转换为接口返回或领域视图。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @return 返回组装或转换后的结果对象。
     */
    private ConversationMessageView toMessageView(ConversationMessageDO row) {
        return new ConversationMessageView(
                row.getId(),
                row.getTenantId(),
                row.getSessionId(),
                row.getDirection(),
                row.getMessageType(),
                row.getExternalMessageId(),
                row.getTextContent(),
                row.getIntent(),
                readMap(row.getContentJson()),
                row.getCreatedAt());
    }

    /**
     * 执行 idempotencyKey 流程，围绕 idempotency key 完成校验、计算或结果组装。
     *
     * @param channel channel 参数，用于 idempotencyKey 流程中的校验、计算或对象转换。
     * @param provider provider 参数，用于 idempotencyKey 流程中的校验、计算或对象转换。
     * @param req 请求对象，承载本次操作的输入参数。
     * @return 返回 idempotency key 生成的文本或业务键。
     */
    private String idempotencyKey(String channel, String provider, ConversationIngressReq req) {
        if (!isBlank(req.externalMessageId())) {
            return channel + ":" + provider + ":" + req.externalMessageId().trim();
        }
        if (!isBlank(req.eventId())) {
            return channel + ":" + provider + ":EVENT:" + req.eventId().trim();
        }
        return channel + ":" + provider + ":FALLBACK:" + nullToBlank(req.executionId()).trim()
                /**
                 * 校验并获取必需参数、资源或权限。
                 *
                 * @return 返回 requireText 流程生成的业务结果。
                 */
                + ":" + requireText(req.userId(), "userId is required")
                /**
                 * 按默认值规则处理输入值。
                 *
                 * @return 返回 nullToBlank 流程生成的业务结果。
                 */
                + ":" + nullToBlank(req.text()).trim();
    }

    /**
     * 执行 eventId 流程，围绕 event id 完成校验、计算或结果组装。
     *
     * @param req 请求对象，承载本次操作的输入参数。
     * @param idempotencyKey 业务键，用于在同一租户下定位资源。
     * @return 返回 event id 生成的文本或业务键。
     */
    private String eventId(ConversationIngressReq req, String idempotencyKey) {
        if (!isBlank(req.eventId())) {
            return req.eventId().trim();
        }
        return idempotencyKey;
    }

    /**
     * 查询或读取业务数据。
     *
     * @param json JSON 字符串，承载结构化配置或明细。
     * @return 返回 readMap 流程生成的业务结果。
     */
    private Map<String, Object> readMap(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, MAP_TYPE);
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (Exception ignored) {
            return Map.of();
        }
    }

    /**
     * 处理 JSON 序列化或反序列化。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 write json 生成的文本或业务键。
     */
    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value == null ? Map.of() : value);
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (Exception ex) {
            throw new IllegalArgumentException("conversation JSON serialization failed", ex);
        }
    }

    /**
     * 解析并规范化租户 ID。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @return 返回 tenant id 计算得到的数量、金额或指标值。
     */
    private static Long tenantId(Long tenantId) {
        return tenantId == null ? 0L : tenantId;
    }

    /**
     * 按安全边界裁剪或保护输入值。
     *
     * @param limit 分页或数量限制，避免一次处理过多数据。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private static int boundedLimit(int limit) {
        if (limit < 1) {
            return 1;
        }
        return Math.min(limit, 100);
    }

    /**
     * 规范化输入值。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param message 原因或消息文本，用于记录状态变化的业务依据。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private static String normalizeRequired(String value, String message) {
        return requireText(value, message).toUpperCase(Locale.ROOT);
    }

    /**
     * 规范化输入值。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param defaultValue 待处理值，用于规则计算或转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private static String normalizeOptional(String value, String defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }

    /**
     * 校验并获取必需参数、资源或权限。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param message 原因或消息文本，用于记录状态变化的业务依据。
     * @return 返回 require text 生成的文本或业务键。
     */
    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    /**
     * 按默认值规则处理输入值。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    /**
     * 按默认值规则处理输入值。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 null to blank 生成的文本或业务键。
     */
    private static String nullToBlank(String value) {
        return value == null ? "" : value;
    }

    /**
     * 判断业务条件是否成立。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回布尔判断结果。
     */
    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    /**
     * 处理集合、映射或字段拷贝逻辑。
     *
     * @param String string 参数，用于 putIfPresent 流程中的校验、计算或对象转换。
     * @param target target 参数，用于 putIfPresent 流程中的校验、计算或对象转换。
     * @param key 业务键，用于在同一租户下定位资源。
     * @param value 待处理值，用于规则计算或转换。
     */
    private static void putIfPresent(Map<String, Object> target, String key, Object value) {
        if (value instanceof String text) {
            if (!text.isBlank()) {
                target.put(key, text.trim());
            }
        // 根据前序判断结果进入后续条件分支。
        } else if (value != null) {
            target.put(key, value);
        }
    }

    /**
     * 执行 now 流程，围绕 now 完成校验、计算或结果组装。
     *
     * @return 返回 now 流程生成的业务结果。
     */
    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }
}
