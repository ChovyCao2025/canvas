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

    public ConversationIngressService(ConversationSessionMapper sessionMapper,
                                      ConversationMessageMapper messageMapper,
                                      WaitResumeService waitResumeService,
                                      ObjectMapper objectMapper) {
        this(sessionMapper, messageMapper, waitResumeService, objectMapper, Clock.systemDefaultZone());
    }

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
    public ConversationIngressResp ingest(Long tenantId, ConversationIngressReq req) {
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
        return new ConversationIngressResp(session.getId(), message.getId(), STATUS_RECORDED, false, resumed);
    }

    public List<ConversationSessionView> listRecentSessions(Long tenantId, String userId, String channel, int limit) {
        Long normalizedTenantId = tenantId(tenantId);
        int boundedLimit = boundedLimit(limit);
        LambdaQueryWrapper<ConversationSessionDO> query = new LambdaQueryWrapper<ConversationSessionDO>()
                .eq(ConversationSessionDO::getTenantId, normalizedTenantId)
                .orderByDesc(ConversationSessionDO::getUpdatedAt)
                .last("LIMIT " + boundedLimit);
        if (!isBlank(userId)) {
            query.eq(ConversationSessionDO::getUserId, userId.trim());
        }
        if (!isBlank(channel)) {
            query.eq(ConversationSessionDO::getChannel, normalizeRequired(channel, "channel is required"));
        }
        return sessionMapper.selectList(query).stream()
                .filter(row -> normalizedTenantId.equals(row.getTenantId()))
                .map(this::toSessionView)
                .toList();
    }

    public List<ConversationMessageView> listMessages(Long tenantId, Long sessionId, int limit) {
        if (sessionId == null) {
            throw new IllegalArgumentException("sessionId is required");
        }
        Long normalizedTenantId = tenantId(tenantId);
        ConversationSessionDO session = sessionMapper.selectById(sessionId);
        if (session == null || !normalizedTenantId.equals(session.getTenantId())) {
            throw new IllegalArgumentException("Conversation session not found: " + sessionId);
        }
        return messageMapper.selectList(new LambdaQueryWrapper<ConversationMessageDO>()
                        .eq(ConversationMessageDO::getTenantId, normalizedTenantId)
                        .eq(ConversationMessageDO::getSessionId, sessionId)
                        .orderByAsc(ConversationMessageDO::getCreatedAt)
                        .last("LIMIT " + boundedLimit(limit)))
                .stream()
                .filter(row -> normalizedTenantId.equals(row.getTenantId()) && sessionId.equals(row.getSessionId()))
                .map(this::toMessageView)
                .toList();
    }

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

    private void updateSessionAfterMessage(ConversationSessionDO session,
                                           ConversationIngressReq req,
                                           ConversationMessageDO message,
                                           LocalDateTime occurredAt) {
        ConversationSessionDO update = new ConversationSessionDO();
        update.setId(session.getId());
        update.setStatus(STATUS_ACTIVE);
        update.setTurnCount((session.getTurnCount() == null ? 0 : session.getTurnCount()) + 1);
        update.setContextJson(writeJson(mergedContext(session, req, message)));
        update.setLastMessageAt(occurredAt);
        update.setUpdatedAt(now());
        sessionMapper.updateById(update);
    }

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

    private Map<String, Object> eventAttributes(ConversationIngressReq req,
                                                String channel,
                                                String provider,
                                                String messageType,
                                                Long sessionId,
                                                Long messageId) {
        Map<String, Object> attributes = new LinkedHashMap<>();
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
        return attributes;
    }

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

    private String idempotencyKey(String channel, String provider, ConversationIngressReq req) {
        if (!isBlank(req.externalMessageId())) {
            return channel + ":" + provider + ":" + req.externalMessageId().trim();
        }
        if (!isBlank(req.eventId())) {
            return channel + ":" + provider + ":EVENT:" + req.eventId().trim();
        }
        return channel + ":" + provider + ":FALLBACK:" + nullToBlank(req.executionId()).trim()
                + ":" + requireText(req.userId(), "userId is required")
                + ":" + nullToBlank(req.text()).trim();
    }

    private String eventId(ConversationIngressReq req, String idempotencyKey) {
        if (!isBlank(req.eventId())) {
            return req.eventId().trim();
        }
        return idempotencyKey;
    }

    private Map<String, Object> readMap(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, MAP_TYPE);
        } catch (Exception ignored) {
            return Map.of();
        }
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value == null ? Map.of() : value);
        } catch (Exception ex) {
            throw new IllegalArgumentException("conversation JSON serialization failed", ex);
        }
    }

    private static Long tenantId(Long tenantId) {
        return tenantId == null ? 0L : tenantId;
    }

    private static int boundedLimit(int limit) {
        if (limit < 1) {
            return 1;
        }
        return Math.min(limit, 100);
    }

    private static String normalizeRequired(String value, String message) {
        return requireText(value, message).toUpperCase(Locale.ROOT);
    }

    private static String normalizeOptional(String value, String defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static String nullToBlank(String value) {
        return value == null ? "" : value;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static void putIfPresent(Map<String, Object> target, String key, Object value) {
        if (value instanceof String text) {
            if (!text.isBlank()) {
                target.put(key, text.trim());
            }
        } else if (value != null) {
            target.put(key, value);
        }
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }
}
