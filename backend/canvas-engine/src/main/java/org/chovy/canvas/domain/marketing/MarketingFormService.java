package org.chovy.canvas.domain.marketing;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.common.enums.NodeType;
import org.chovy.canvas.dal.dataobject.CdpUserProfileDO;
import org.chovy.canvas.dal.dataobject.CustomerChannelDO;
import org.chovy.canvas.dal.dataobject.MarketingConsentDO;
import org.chovy.canvas.dal.dataobject.MarketingFormDefinitionDO;
import org.chovy.canvas.dal.dataobject.MarketingFormSubmissionDO;
import org.chovy.canvas.dal.mapper.CdpUserProfileMapper;
import org.chovy.canvas.dal.mapper.CustomerChannelMapper;
import org.chovy.canvas.dal.mapper.MarketingConsentMapper;
import org.chovy.canvas.dal.mapper.MarketingFormDefinitionMapper;
import org.chovy.canvas.dal.mapper.MarketingFormSubmissionMapper;
import org.chovy.canvas.domain.cdp.CdpUserService;
import org.chovy.canvas.engine.disruptor.CanvasDisruptorService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class MarketingFormService {

    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_INACTIVE = "INACTIVE";

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};
    private static final String DEFAULT_FIELD_SCHEMA = """
            [{"key":"email","label":"邮箱","type":"email","required":true},{"key":"name","label":"姓名","type":"text","required":false},{"key":"marketingConsent","label":"同意接收营销消息","type":"checkbox","required":false}]
            """;

    private final MarketingFormDefinitionMapper definitionMapper;
    private final MarketingFormSubmissionMapper submissionMapper;
    private final CdpUserService cdpUserService;
    private final CdpUserProfileMapper profileMapper;
    private final CustomerChannelMapper channelMapper;
    private final MarketingConsentMapper consentMapper;
    private final ObjectMapper objectMapper;
    private final ObjectProvider<CanvasDisruptorService> disruptorService;

    public MarketingFormService(MarketingFormDefinitionMapper definitionMapper,
                                MarketingFormSubmissionMapper submissionMapper,
                                CdpUserService cdpUserService,
                                CdpUserProfileMapper profileMapper,
                                CustomerChannelMapper channelMapper,
                                MarketingConsentMapper consentMapper,
                                ObjectMapper objectMapper) {
        this(definitionMapper, submissionMapper, cdpUserService, profileMapper, channelMapper,
                consentMapper, objectMapper, null);
    }

    @Autowired
    public MarketingFormService(MarketingFormDefinitionMapper definitionMapper,
                                MarketingFormSubmissionMapper submissionMapper,
                                CdpUserService cdpUserService,
                                CdpUserProfileMapper profileMapper,
                                CustomerChannelMapper channelMapper,
                                MarketingConsentMapper consentMapper,
                                ObjectMapper objectMapper,
                                ObjectProvider<CanvasDisruptorService> disruptorService) {
        this.definitionMapper = definitionMapper;
        this.submissionMapper = submissionMapper;
        this.cdpUserService = cdpUserService;
        this.profileMapper = profileMapper;
        this.channelMapper = channelMapper;
        this.consentMapper = consentMapper;
        this.objectMapper = objectMapper == null ? new ObjectMapper() : objectMapper;
        this.disruptorService = disruptorService;
    }

    public List<FormDefinitionView> list(Long tenantId) {
        return definitionMapper.selectList(new LambdaQueryWrapper<MarketingFormDefinitionDO>()
                        .eq(MarketingFormDefinitionDO::getTenantId, safeTenantId(tenantId))
                        .orderByDesc(MarketingFormDefinitionDO::getUpdatedAt)
                        .orderByDesc(MarketingFormDefinitionDO::getId))
                .stream()
                .map(this::toDefinitionView)
                .toList();
    }

    public FormDefinitionView get(Long tenantId, Long id) {
        return toDefinitionView(getRequiredById(safeTenantId(tenantId), id));
    }

    @Transactional(rollbackFor = Exception.class)
    public FormDefinitionView create(Long tenantId, FormDefinitionCommand command) {
        Long scopedTenantId = safeTenantId(tenantId);
        MarketingFormDefinitionDO row = new MarketingFormDefinitionDO();
        row.setTenantId(scopedTenantId);
        row.setPublicKey(normalizePublicKey(command.publicKey()));
        row.setName(requireText(command.name(), "name"));
        row.setDescription(trimToLimit(command.description(), 500));
        row.setStatus(Boolean.FALSE.equals(command.active()) ? STATUS_INACTIVE : STATUS_ACTIVE);
        row.setFieldSchemaJson(normalizeJson(command.fieldSchemaJson(), DEFAULT_FIELD_SCHEMA, "fieldSchemaJson"));
        row.setSubmitActionJson(normalizeJson(command.submitActionJson(), "{}", "submitActionJson"));
        row.setSuccessMessage(defaultString(command.successMessage(), "提交成功"));
        row.setCreatedBy(defaultString(command.createdBy(), "operator"));
        definitionMapper.insert(row);
        return toDefinitionView(row);
    }

    @Transactional(rollbackFor = Exception.class)
    public FormDefinitionView update(Long tenantId, Long id, FormDefinitionCommand command) {
        Long scopedTenantId = safeTenantId(tenantId);
        MarketingFormDefinitionDO row = getRequiredById(scopedTenantId, id);
        if (hasText(command.publicKey())) {
            row.setPublicKey(normalizePublicKey(command.publicKey()));
        }
        if (hasText(command.name())) {
            row.setName(requireText(command.name(), "name"));
        }
        row.setDescription(trimToLimit(command.description(), 500));
        row.setFieldSchemaJson(normalizeJson(command.fieldSchemaJson(), row.getFieldSchemaJson(), "fieldSchemaJson"));
        row.setSubmitActionJson(normalizeJson(command.submitActionJson(), row.getSubmitActionJson(), "submitActionJson"));
        row.setSuccessMessage(defaultString(command.successMessage(), row.getSuccessMessage()));
        if (command.active() != null) {
            row.setStatus(Boolean.TRUE.equals(command.active()) ? STATUS_ACTIVE : STATUS_INACTIVE);
        }
        definitionMapper.updateById(row);
        return toDefinitionView(row);
    }

    @Transactional(rollbackFor = Exception.class)
    public FormDefinitionView setStatus(Long tenantId, Long id, boolean active) {
        MarketingFormDefinitionDO row = getRequiredById(safeTenantId(tenantId), id);
        row.setStatus(active ? STATUS_ACTIVE : STATUS_INACTIVE);
        definitionMapper.updateById(row);
        return toDefinitionView(row);
    }

    public List<SubmissionView> submissions(Long tenantId, Long formId, int limit) {
        Long scopedTenantId = safeTenantId(tenantId);
        int pageSize = Math.max(1, Math.min(limit <= 0 ? 50 : limit, 200));
        LambdaQueryWrapper<MarketingFormSubmissionDO> query = new LambdaQueryWrapper<MarketingFormSubmissionDO>()
                .eq(MarketingFormSubmissionDO::getTenantId, scopedTenantId)
                .orderByDesc(MarketingFormSubmissionDO::getCreatedAt)
                .orderByDesc(MarketingFormSubmissionDO::getId);
        if (formId != null) {
            query.eq(MarketingFormSubmissionDO::getFormId, formId);
        }
        return submissionMapper.selectPage(new Page<>(1, pageSize), query)
                .getRecords()
                .stream()
                .map(this::toSubmissionView)
                .toList();
    }

    public PublicFormView publicForm(String publicKey) {
        MarketingFormDefinitionDO row = getRequiredByPublicKey(publicKey);
        return new PublicFormView(
                row.getPublicKey(),
                row.getName(),
                row.getDescription(),
                row.getStatus(),
                row.getFieldSchemaJson(),
                row.getSuccessMessage());
    }

    @Transactional(rollbackFor = Exception.class)
    public SubmitResult submit(String publicKey, PublicSubmitCommand command) {
        MarketingFormDefinitionDO definition = getRequiredByPublicKey(publicKey);
        if (!STATUS_ACTIVE.equalsIgnoreCase(definition.getStatus())) {
            throw new IllegalStateException("marketing form is not active: " + definition.getPublicKey());
        }
        Map<String, Object> response = command.response() == null ? Map.of() : command.response();
        Map<String, Object> utm = command.utm() == null ? Map.of() : command.utm();
        String email = normalizeEmail(firstText(response, "email", "mail"));
        String phone = firstText(response, "phone", "mobile", "mobilePhone", "tel");
        String name = firstText(response, "name", "fullName", "displayName");
        CdpUserProfileDO profile = resolveProfile(definition, command, response, email, phone);
        if (profile != null) {
            updateProfile(definition, profile, response, email, phone, name);
            upsertChannels(definition, profile.getUserId(), email, phone);
        }
        ConsentDecision consent = resolveConsent(command, response, email, phone);
        if (profile != null && consent != null) {
            for (String channel : consent.channels()) {
                upsertConsent(definition, profile.getUserId(), channel, consent.status());
            }
        }

        MarketingFormSubmissionDO row = new MarketingFormSubmissionDO();
        row.setTenantId(definition.getTenantId());
        row.setFormId(definition.getId());
        row.setPublicKey(definition.getPublicKey());
        row.setUserId(profile == null ? null : profile.getUserId());
        row.setAnonymousId(trimToLimit(command.anonymousId(), 128));
        row.setResponseJson(toJson(response));
        row.setUtmJson(utm.isEmpty() ? null : toJson(utm));
        row.setConsentChannel(consent == null ? null : String.join(",", consent.channels()));
        row.setConsentStatus(consent == null ? null : consent.status());
        row.setIdempotencyKey(defaultString(command.idempotencyKey(), "form:" + UUID.randomUUID()));
        row.setUserAgent(trimToLimit(command.userAgent(), 512));
        row.setSubmitIpHash(trimToLimit(command.submitIpHash(), 128));
        row.setTriggerEventCode(triggerEventCode(definition));
        try {
            submissionMapper.insert(row);
        } catch (DuplicateKeyException duplicate) {
            MarketingFormSubmissionDO existing = findSubmissionByIdempotency(
                    definition.getTenantId(), definition.getId(), row.getIdempotencyKey());
            if (existing != null) {
                return toSubmitResult(existing, definition, false);
            }
            throw duplicate;
        }
        boolean triggered = maybePublishTrigger(definition, row, response, utm);
        return toSubmitResult(row, definition, triggered);
    }

    private CdpUserProfileDO resolveProfile(MarketingFormDefinitionDO definition,
                                            PublicSubmitCommand command,
                                            Map<String, Object> response,
                                            String email,
                                            String phone) {
        String userId = firstText(response, "userId", "user_id", "uid");
        if (hasText(userId)) {
            return cdpUserService.ensureUser(definition.getTenantId(), userId, "MARKETING_FORM", definition.getPublicKey());
        }
        if (hasText(email)) {
            return cdpUserService.ensureUserByIdentity(
                    definition.getTenantId(), "EMAIL", email, "MARKETING_FORM", definition.getPublicKey());
        }
        if (hasText(phone)) {
            return cdpUserService.ensureUserByIdentity(
                    definition.getTenantId(), "PHONE", phone, "MARKETING_FORM", definition.getPublicKey());
        }
        if (hasText(command.anonymousId())) {
            return cdpUserService.ensureUser(
                    definition.getTenantId(), "anonymous:" + command.anonymousId().trim(),
                    "MARKETING_FORM", definition.getPublicKey());
        }
        return null;
    }

    private void updateProfile(MarketingFormDefinitionDO definition,
                               CdpUserProfileDO profile,
                               Map<String, Object> response,
                               String email,
                               String phone,
                               String name) {
        if (hasText(name)) {
            profile.setDisplayName(name);
        } else if (!hasText(profile.getDisplayName()) && hasText(email)) {
            profile.setDisplayName(email);
        }
        if (hasText(email)) {
            profile.setEmail(email);
        }
        if (hasText(phone)) {
            profile.setPhone(phone.trim());
        }
        if (!hasText(profile.getStatus())) {
            profile.setStatus("ACTIVE");
        }
        profile.setLastSeenAt(LocalDateTime.now());
        profile.setPropertiesJson(mergeProfileProperties(profile.getPropertiesJson(), definition, response));
        profileMapper.updateById(profile);
    }

    private void upsertChannels(MarketingFormDefinitionDO definition, String userId, String email, String phone) {
        if (hasText(email)) {
            upsertChannel(definition.getTenantId(), userId, "EMAIL", email,
                    Map.of("source", "marketing_form", "publicKey", definition.getPublicKey()));
        }
        if (hasText(phone)) {
            upsertChannel(definition.getTenantId(), userId, "SMS", phone.trim(),
                    Map.of("source", "marketing_form", "publicKey", definition.getPublicKey()));
        }
    }

    private void upsertChannel(Long tenantId, String userId, String channel, String address, Map<String, Object> metadata) {
        CustomerChannelDO row = channelMapper.selectOne(new LambdaQueryWrapper<CustomerChannelDO>()
                .eq(CustomerChannelDO::getTenantId, tenantId)
                .eq(CustomerChannelDO::getUserId, userId)
                .eq(CustomerChannelDO::getChannel, channel)
                .last("LIMIT 1"));
        if (row == null) {
            row = new CustomerChannelDO();
            row.setTenantId(tenantId);
            row.setUserId(userId);
            row.setChannel(channel);
            row.setEnabled(1);
            row.setVerified(0);
            row.setAddress(address);
            row.setMetadata(toJson(metadata));
            channelMapper.insert(row);
        } else {
            row.setAddress(address);
            row.setEnabled(1);
            row.setMetadata(toJson(metadata));
            channelMapper.updateById(row);
        }
    }

    private ConsentDecision resolveConsent(PublicSubmitCommand command,
                                           Map<String, Object> response,
                                           String email,
                                           String phone) {
        String explicitStatus = normalizeConsentStatusOrNull(command.consentStatus());
        String requestedChannel = normalizeChannelOrNull(command.consentChannel());
        if (explicitStatus != null) {
            return new ConsentDecision(channelsForConsent(requestedChannel, email, phone), explicitStatus);
        }
        Object rawConsent = firstRaw(response, "marketingConsent", "consent", "optIn", "subscribe");
        if (rawConsent == null) {
            return null;
        }
        String status = truthy(rawConsent) ? MarketingConsentDO.OPT_IN : MarketingConsentDO.OPT_OUT;
        return new ConsentDecision(channelsForConsent(requestedChannel, email, phone), status);
    }

    private List<String> channelsForConsent(String requestedChannel, String email, String phone) {
        if (hasText(requestedChannel) && !"ALL".equals(requestedChannel)) {
            return List.of(requestedChannel);
        }
        List<String> channels = new ArrayList<>();
        if (hasText(email)) {
            channels.add("EMAIL");
        }
        if (hasText(phone)) {
            channels.add("SMS");
        }
        return channels.isEmpty() ? List.of("ALL") : channels;
    }

    private void upsertConsent(MarketingFormDefinitionDO definition, String userId, String channel, String status) {
        MarketingConsentDO row = consentMapper.selectOne(new LambdaQueryWrapper<MarketingConsentDO>()
                .eq(MarketingConsentDO::getTenantId, definition.getTenantId())
                .eq(MarketingConsentDO::getUserId, userId)
                .eq(MarketingConsentDO::getChannel, channel)
                .last("LIMIT 1"));
        if (row == null) {
            row = new MarketingConsentDO();
            row.setTenantId(definition.getTenantId());
            row.setUserId(userId);
            row.setChannel(channel);
            row.setConsentStatus(status);
            row.setSource("marketing_form:" + definition.getPublicKey());
            consentMapper.insert(row);
        } else {
            row.setConsentStatus(status);
            row.setSource("marketing_form:" + definition.getPublicKey());
            consentMapper.updateById(row);
        }
    }

    private boolean maybePublishTrigger(MarketingFormDefinitionDO definition,
                                        MarketingFormSubmissionDO submission,
                                        Map<String, Object> response,
                                        Map<String, Object> utm) {
        Map<String, Object> action = submitAction(definition);
        Long canvasId = longValue(action.get("canvasId"));
        String eventCode = cleanText(action.get("triggerEventCode"));
        if (canvasId == null || !hasText(eventCode) || !hasText(submission.getUserId())) {
            return false;
        }
        CanvasDisruptorService publisher = disruptorService == null ? null : disruptorService.getIfAvailable();
        if (publisher == null) {
            return false;
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("formPublicKey", definition.getPublicKey());
        payload.put("submissionId", submission.getId());
        payload.put("response", response);
        payload.put("utm", utm);
        payload.put("anonymousId", submission.getAnonymousId());
        publisher.publish(canvasId, submission.getUserId(), "BEHAVIOR", NodeType.EVENT_TRIGGER,
                eventCode, payload, submission.getIdempotencyKey());
        return true;
    }

    private String triggerEventCode(MarketingFormDefinitionDO definition) {
        return cleanText(submitAction(definition).get("triggerEventCode"));
    }

    private Map<String, Object> submitAction(MarketingFormDefinitionDO definition) {
        String json = definition.getSubmitActionJson();
        if (!hasText(json)) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, MAP_TYPE);
        } catch (JsonProcessingException ignored) {
            return Map.of();
        }
    }

    private MarketingFormDefinitionDO getRequiredById(Long tenantId, Long id) {
        MarketingFormDefinitionDO row = definitionMapper.selectOne(new LambdaQueryWrapper<MarketingFormDefinitionDO>()
                .eq(MarketingFormDefinitionDO::getTenantId, tenantId)
                .eq(MarketingFormDefinitionDO::getId, id)
                .last("LIMIT 1"));
        if (row == null) {
            throw new IllegalArgumentException("marketing form does not exist: " + id);
        }
        return row;
    }

    private MarketingFormDefinitionDO getRequiredByPublicKey(String publicKey) {
        MarketingFormDefinitionDO row = definitionMapper.selectOne(new LambdaQueryWrapper<MarketingFormDefinitionDO>()
                .eq(MarketingFormDefinitionDO::getPublicKey, normalizePublicKey(publicKey))
                .last("LIMIT 1"));
        if (row == null) {
            throw new IllegalArgumentException("marketing form does not exist: " + publicKey);
        }
        return row;
    }

    private MarketingFormSubmissionDO findSubmissionByIdempotency(Long tenantId, Long formId, String idempotencyKey) {
        return submissionMapper.selectOne(new LambdaQueryWrapper<MarketingFormSubmissionDO>()
                .eq(MarketingFormSubmissionDO::getTenantId, tenantId)
                .eq(MarketingFormSubmissionDO::getFormId, formId)
                .eq(MarketingFormSubmissionDO::getIdempotencyKey, idempotencyKey)
                .last("LIMIT 1"));
    }

    private FormDefinitionView toDefinitionView(MarketingFormDefinitionDO row) {
        return new FormDefinitionView(
                row.getId(),
                row.getPublicKey(),
                row.getName(),
                row.getDescription(),
                row.getStatus(),
                row.getFieldSchemaJson(),
                row.getSubmitActionJson(),
                row.getSuccessMessage(),
                row.getCreatedAt(),
                row.getUpdatedAt());
    }

    private SubmissionView toSubmissionView(MarketingFormSubmissionDO row) {
        return new SubmissionView(
                row.getId(),
                row.getFormId(),
                row.getPublicKey(),
                row.getUserId(),
                row.getAnonymousId(),
                row.getResponseJson(),
                row.getUtmJson(),
                row.getConsentChannel(),
                row.getConsentStatus(),
                row.getIdempotencyKey(),
                row.getTriggerEventCode(),
                row.getCreatedAt());
    }

    private SubmitResult toSubmitResult(MarketingFormSubmissionDO row, MarketingFormDefinitionDO definition, boolean triggered) {
        return new SubmitResult(
                row.getId(),
                row.getUserId(),
                definition.getSuccessMessage(),
                row.getTriggerEventCode(),
                triggered);
    }

    private String mergeProfileProperties(String existingJson,
                                          MarketingFormDefinitionDO definition,
                                          Map<String, Object> response) {
        Map<String, Object> properties = new LinkedHashMap<>();
        if (hasText(existingJson)) {
            try {
                JsonNode node = objectMapper.readTree(existingJson);
                if (node != null && node.isObject()) {
                    properties.putAll(objectMapper.convertValue(node, MAP_TYPE));
                }
            } catch (RuntimeException | JsonProcessingException ignored) {
                properties.put("previousPropertiesJson", existingJson);
            }
        }
        properties.put("lastMarketingFormKey", definition.getPublicKey());
        properties.put("lastMarketingFormSubmittedAt", LocalDateTime.now().toString());
        properties.put("lastMarketingFormFields", response);
        return toJson(properties);
    }

    private String normalizeJson(String value, String fallback, String fieldName) {
        String json = defaultString(value, fallback);
        try {
            objectMapper.readTree(json);
            return json;
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException(fieldName + " must be valid JSON");
        }
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value == null ? Map.of() : value);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("JSON serialization failed");
        }
    }

    private String normalizePublicKey(String value) {
        String text = hasText(value) ? value.trim() : "mf_" + UUID.randomUUID().toString().replace("-", "");
        String normalized = text.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9_-]", "-")
                .replaceAll("-+", "-");
        if (!hasText(normalized)) {
            normalized = "mf_" + UUID.randomUUID().toString().replace("-", "");
        }
        return trimToLimit(normalized, 96);
    }

    private String normalizeConsentStatusOrNull(String status) {
        if (!hasText(status)) {
            return null;
        }
        String normalized = status.trim().toUpperCase(Locale.ROOT);
        if ("OPTIN".equals(normalized)) {
            return MarketingConsentDO.OPT_IN;
        }
        if ("OPTOUT".equals(normalized)) {
            return MarketingConsentDO.OPT_OUT;
        }
        if (MarketingConsentDO.OPT_IN.equals(normalized) || MarketingConsentDO.OPT_OUT.equals(normalized)) {
            return normalized;
        }
        throw new IllegalArgumentException("unsupported consent status: " + status);
    }

    private String normalizeChannelOrNull(String channel) {
        if (!hasText(channel)) {
            return null;
        }
        return channel.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeEmail(String email) {
        return hasText(email) ? email.trim().toLowerCase(Locale.ROOT) : null;
    }

    private Object firstRaw(Map<String, Object> map, String... keys) {
        if (map == null || map.isEmpty()) {
            return null;
        }
        for (String key : keys) {
            if (map.containsKey(key)) {
                return map.get(key);
            }
        }
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            for (String key : keys) {
                if (entry.getKey().equalsIgnoreCase(key)) {
                    return entry.getValue();
                }
            }
        }
        return null;
    }

    private String firstText(Map<String, Object> map, String... keys) {
        return cleanText(firstRaw(map, keys));
    }

    private String cleanText(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        return text.isBlank() ? null : text;
    }

    private Long longValue(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        String text = cleanText(value);
        if (!hasText(text)) {
            return null;
        }
        try {
            return Long.parseLong(text);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private boolean truthy(Object value) {
        if (value instanceof Boolean bool) {
            return bool;
        }
        if (value instanceof Number number) {
            return number.intValue() != 0;
        }
        String text = cleanText(value);
        if (!hasText(text)) {
            return false;
        }
        return switch (text.toLowerCase(Locale.ROOT)) {
            case "true", "1", "yes", "y", "on", "opt_in" -> true;
            default -> false;
        };
    }

    private String requireText(String value, String fieldName) {
        if (!hasText(value)) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }

    private String defaultString(String value, String fallback) {
        return hasText(value) ? value.trim() : fallback;
    }

    private String trimToLimit(String value, int limit) {
        if (!hasText(value)) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.length() <= limit ? trimmed : trimmed.substring(0, limit);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private Long safeTenantId(Long tenantId) {
        return tenantId == null ? 0L : tenantId;
    }

    private record ConsentDecision(List<String> channels, String status) {
    }

    public record FormDefinitionCommand(
            String publicKey,
            String name,
            String description,
            String fieldSchemaJson,
            String submitActionJson,
            String successMessage,
            Boolean active,
            String createdBy) {
    }

    public record PublicSubmitCommand(
            Map<String, Object> response,
            Map<String, Object> utm,
            String anonymousId,
            String idempotencyKey,
            String consentChannel,
            String consentStatus,
            String userAgent,
            String submitIpHash) {
    }

    public record FormDefinitionView(
            Long id,
            String publicKey,
            String name,
            String description,
            String status,
            String fieldSchemaJson,
            String submitActionJson,
            String successMessage,
            LocalDateTime createdAt,
            LocalDateTime updatedAt) {
    }

    public record PublicFormView(
            String publicKey,
            String name,
            String description,
            String status,
            String fieldSchemaJson,
            String successMessage) {
    }

    public record SubmissionView(
            Long id,
            Long formId,
            String publicKey,
            String userId,
            String anonymousId,
            String responseJson,
            String utmJson,
            String consentChannel,
            String consentStatus,
            String idempotencyKey,
            String triggerEventCode,
            LocalDateTime createdAt) {
    }

    public record SubmitResult(
            Long submissionId,
            String userId,
            String successMessage,
            String triggerEventCode,
            boolean triggered) {
    }
}
