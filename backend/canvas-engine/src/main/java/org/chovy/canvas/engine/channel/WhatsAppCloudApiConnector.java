package org.chovy.canvas.engine.channel;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component("WHATSAPP:CLOUD_API")
public class WhatsAppCloudApiConnector implements ChannelConnector {

    private final WhatsAppCloudApiClient client;
    private final String phoneNumberId;
    private final String accessToken;
    private final String defaultLanguage;

    public WhatsAppCloudApiConnector(WhatsAppCloudApiClient client,
                                     @Value("${canvas.conversation.whatsapp.cloud.phone-number-id:}")
                                     String phoneNumberId,
                                     @Value("${canvas.conversation.whatsapp.cloud.access-token:}")
                                     String accessToken,
                                     @Value("${canvas.conversation.whatsapp.cloud.default-language:en_US}")
                                     String defaultLanguage) {
        this.client = client;
        this.phoneNumberId = blankToNull(phoneNumberId);
        this.accessToken = blankToNull(accessToken);
        this.defaultLanguage = blankToDefault(defaultLanguage, "en_US");
    }

    @Override
    public ConnectorMode mode() {
        return ConnectorMode.REAL;
    }

    @Override
    public ConnectorHealth health() {
        if (!configured()) {
            return new ConnectorHealth("DOWN", "WhatsApp Cloud API connector not configured");
        }
        return new ConnectorHealth("UP", "WhatsApp Cloud API connector ready");
    }

    @Override
    public ConnectorCapabilities capabilities() {
        return new ConnectorCapabilities(true, true, Map.of(
                "provider", "CLOUD_API",
                "supportsTemplate", true,
                "supportsSessionText", true));
    }

    @Override
    public ConnectorSendResult send(ConnectorSendRequest request) {
        if (!configured()) {
            return new ConnectorSendResult(false, null, "DISABLED", "WhatsApp Cloud API connector not configured");
        }
        try {
            Map<String, Object> providerPayload = providerPayload(request);
            Map<String, Object> response = client.sendMessage(phoneNumberId, accessToken, providerPayload);
            String messageId = firstMessageId(response);
            return new ConnectorSendResult(true, messageId, "ACCEPTED", null);
        } catch (RuntimeException ex) {
            return new ConnectorSendResult(false, null, "FAILED", ex.getMessage());
        }
    }

    @Override
    public ConnectorReceiptResult parseReceipt(Map<String, Object> rawPayload) {
        return new ConnectorReceiptResult(null, "UNSUPPORTED", rawPayload == null ? Map.of() : rawPayload);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> providerPayload(ConnectorSendRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("WhatsApp send request is required");
        }
        Map<String, Object> payload = request.payload() == null ? Map.of() : request.payload();
        String to = normalizedPhone(firstText(request.userId(), string(payload.get("userId"))));
        if (isBlank(to)) {
            throw new IllegalArgumentException("WhatsApp recipient is required");
        }
        String templateId = firstText(string(payload.get("templateId")), string(payload.get("template_id")));
        Map<String, Object> content = map(payload.get("content"));
        Map<String, Object> variables = map(payload.get("variables"));
        Map<String, Object> providerPayload = new LinkedHashMap<>();
        providerPayload.put("messaging_product", "whatsapp");
        providerPayload.put("to", to);
        if (!isBlank(templateId)) {
            providerPayload.put("type", "template");
            providerPayload.put("template", template(templateId, content, variables));
        } else {
            providerPayload.put("type", "text");
            providerPayload.put("text", Map.of("body", requiredText(content)));
        }
        return providerPayload;
    }

    private Map<String, Object> template(String templateId,
                                         Map<String, Object> content,
                                         Map<String, Object> variables) {
        Map<String, Object> template = new LinkedHashMap<>();
        template.put("name", templateId.trim());
        template.put("language", Map.of("code", language(content)));
        if (!variables.isEmpty()) {
            List<Map<String, Object>> parameters = new ArrayList<>();
            variables.forEach((key, value) -> parameters.add(Map.of("type", "text", "text", String.valueOf(value))));
            template.put("components", List.of(Map.of("type", "body", "parameters", parameters)));
        }
        return template;
    }

    private String language(Map<String, Object> content) {
        String value = firstText(string(content.get("language")), string(content.get("languageCode")));
        return isBlank(value) ? defaultLanguage : value;
    }

    private String requiredText(Map<String, Object> content) {
        String body = firstText(string(content.get("body")), string(content.get("content")));
        if (isBlank(body)) {
            throw new IllegalArgumentException("WhatsApp text body is required when templateId is missing");
        }
        return body.trim();
    }

    @SuppressWarnings("unchecked")
    private String firstMessageId(Map<String, Object> response) {
        Object rawMessages = response == null ? null : response.get("messages");
        if (rawMessages instanceof List<?> messages && !messages.isEmpty()
                && messages.get(0) instanceof Map<?, ?> first) {
            Object id = ((Map<String, Object>) first).get("id");
            return string(id);
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> map(Object value) {
        return value instanceof Map<?, ?> map ? (Map<String, Object>) map : Map.of();
    }

    private boolean configured() {
        return client != null && !isBlank(phoneNumberId) && !isBlank(accessToken);
    }

    private static String normalizedPhone(String userId) {
        if (isBlank(userId)) {
            return null;
        }
        String value = userId.trim();
        if (value.toLowerCase(Locale.ROOT).startsWith("whatsapp:")) {
            value = value.substring("whatsapp:".length());
        }
        if (value.startsWith("+")) {
            value = value.substring(1);
        }
        return value;
    }

    private static String string(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static String firstText(String primary, String fallback) {
        return isBlank(primary) ? fallback : primary;
    }

    private static String blankToDefault(String value, String fallback) {
        return isBlank(value) ? fallback : value.trim();
    }

    private static String blankToNull(String value) {
        return isBlank(value) ? null : value.trim();
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
