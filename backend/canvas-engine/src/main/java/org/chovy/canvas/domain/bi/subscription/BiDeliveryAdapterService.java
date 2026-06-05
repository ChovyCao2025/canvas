package org.chovy.canvas.domain.bi.subscription;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

@Service
public class BiDeliveryAdapterService {

    private static final Duration HTTP_TIMEOUT = Duration.ofSeconds(10);
    private static final Pattern EMAIL_ADDRESS = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private final WebClient.Builder webClientBuilder;
    private final BiEmailDeliveryClient emailDeliveryClient;
    private final String mailFrom;
    private final ObjectMapper objectMapper;

    public BiDeliveryAdapterService(WebClient.Builder webClientBuilder, ObjectMapper objectMapper) {
        this(webClientBuilder, (BiEmailDeliveryClient) null, "", objectMapper);
    }

    @Autowired
    public BiDeliveryAdapterService(WebClient.Builder webClientBuilder,
                                    ObjectProvider<BiEmailDeliveryClient> emailDeliveryClientProvider,
                                    @Value("${canvas.bi.delivery.email.from:}") String mailFrom,
                                    ObjectMapper objectMapper) {
        this(webClientBuilder,
                emailDeliveryClientProvider == null ? null : emailDeliveryClientProvider.getIfAvailable(),
                mailFrom,
                objectMapper);
    }

    public BiDeliveryAdapterService(WebClient.Builder webClientBuilder,
                                    BiEmailDeliveryClient emailDeliveryClient,
                                    String mailFrom,
                                    ObjectMapper objectMapper) {
        this.webClientBuilder = webClientBuilder;
        this.emailDeliveryClient = emailDeliveryClient;
        this.mailFrom = mailFrom == null ? "" : mailFrom.trim();
        this.objectMapper = objectMapper;
    }

    public BiDeliveryAdapterResult deliver(BiDeliveryAdapterRequest request) {
        String channel = normalizeChannel(request.channel());
        if (isWebhookLike(channel)) {
            return deliverWebhook(channel, request);
        }
        if ("EMAIL".equals(channel)) {
            return deliverEmail(request);
        }
        return BiDeliveryAdapterResult.pending("Delivery adapter is not configured yet");
    }

    private BiDeliveryAdapterResult deliverEmail(BiDeliveryAdapterRequest request) {
        if (emailDeliveryClient == null || !emailDeliveryClient.configured()) {
            return BiDeliveryAdapterResult.pending("Email adapter is not configured yet");
        }
        List<String> recipients = emailRecipients(request.receiver());
        if (recipients.isEmpty()) {
            return BiDeliveryAdapterResult.pending("Email recipients are not configured");
        }
        String from = emailFrom(request.receiver());
        if (!hasText(from)) {
            return BiDeliveryAdapterResult.pending("Email sender is not configured");
        }
        try {
            emailDeliveryClient.send(new BiEmailDeliveryRequest(
                    from,
                    recipients,
                    String.valueOf(request.payload().getOrDefault("title", "BI 通知")),
                    messageText(request),
                    request.attachments()));
            String attachmentSummary = request.attachments().isEmpty()
                    ? ""
                    : " with " + request.attachments().size() + " attachment(s)";
            return BiDeliveryAdapterResult.delivered("Email delivered to " + recipients.size() + " recipient(s)" + attachmentSummary);
        } catch (RuntimeException e) {
            return BiDeliveryAdapterResult.failed("Email delivery failed", truncate(e.getMessage()));
        }
    }

    private BiDeliveryAdapterResult deliverWebhook(String channel, BiDeliveryAdapterRequest request) {
        String url = webhookUrl(channel, request.receiver());
        if (!hasText(url)) {
            return BiDeliveryAdapterResult.pending(channel + " webhook URL is not configured");
        }
        Map<String, Object> body = webhookBody(channel, request);
        try {
            ResponseEntity<String> response = postJson(url, body);
            int httpStatus = response == null ? 0 : response.getStatusCode().value();
            if (httpStatus >= 200 && httpStatus < 300) {
                return BiDeliveryAdapterResult.delivered(channel + " webhook delivered: HTTP " + httpStatus);
            }
            return BiDeliveryAdapterResult.failed(channel + " webhook returned HTTP " + httpStatus,
                    truncate(response == null ? null : response.getBody()));
        } catch (RuntimeException e) {
            return BiDeliveryAdapterResult.failed(channel + " webhook delivery failed", truncate(e.getMessage()));
        }
    }

    protected ResponseEntity<String> postJson(String url, Map<String, Object> body) {
        return webClientBuilder.build()
                .post()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchangeToMono(response -> response.toEntity(String.class))
                .block(HTTP_TIMEOUT);
    }

    private Map<String, Object> webhookBody(String channel, BiDeliveryAdapterRequest request) {
        if ("LARK".equals(channel) || "FEISHU".equals(channel)) {
            return Map.of(
                    "msg_type", "text",
                    "content", Map.of("text", messageText(request)));
        }
        if ("DINGTALK".equals(channel) || "DING".equals(channel)) {
            return Map.of(
                    "msgtype", "text",
                    "text", Map.of("content", messageText(request)));
        }
        if (isWeCom(channel)) {
            return Map.of(
                    "msgtype", "text",
                    "text", Map.of("content", messageText(request)));
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("event", "BI_DELIVERY");
        body.put("tenantId", request.tenantId());
        body.put("workspaceId", request.workspaceId());
        body.put("jobType", request.jobType());
        body.put("jobId", request.jobId());
        body.put("jobKey", request.jobKey());
        body.put("resourceType", request.resourceType());
        body.put("resourceId", request.resourceId());
        body.put("channel", channel);
        body.put("metricValue", request.metricValue());
        body.put("triggeredBy", request.triggeredBy());
        body.put("payload", request.payload());
        return body;
    }

    private String messageText(BiDeliveryAdapterRequest request) {
        String title = String.valueOf(request.payload().getOrDefault("title", "BI 通知"));
        String message = String.valueOf(request.payload().getOrDefault("message", ""));
        String url = String.valueOf(request.payload().getOrDefault("url", "/bi"));
        String metric = request.metricValue() == null ? "" : "\n指标值: " + request.metricValue();
        String attachments = attachmentText(request.payload());
        return title + "\n" + message + metric + "\n查看: " + url + attachments;
    }

    private String attachmentText(Map<String, Object> payload) {
        Object extra = payload.get("extra");
        if (!(extra instanceof Map<?, ?> extraMap)) {
            return "";
        }
        Object attachments = extraMap.get("attachments");
        if (!(attachments instanceof List<?> values) || values.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder("\n附件:");
        for (Object value : values) {
            if (value instanceof Map<?, ?> attachment) {
                Object name = attachment.get("fileName");
                Object type = attachment.get("attachmentType");
                Object link = attachment.get("fileUrl");
                builder.append("\n- ")
                        .append(hasText(String.valueOf(type)) ? String.valueOf(type) + " " : "")
                        .append(hasText(String.valueOf(name)) ? String.valueOf(name) : "attachment");
                if (hasText(String.valueOf(link))) {
                    builder.append(" ").append(link);
                }
            }
        }
        return builder.toString();
    }

    private String webhookUrl(String channel, Map<String, Object> receiver) {
        String lower = channel.toLowerCase(Locale.ROOT);
        Object nested = firstValue(receiver, lower, "webhook", "bot", "robot");
        if (nested instanceof Map<?, ?> nestedMap) {
            String nestedUrl = urlFromMap(nestedMap, lower);
            if (hasText(nestedUrl)) {
                return nestedUrl;
            }
        }
        return urlFromMap(receiver, lower);
    }

    private String urlFromMap(Map<?, ?> values, String lowerChannel) {
        String[] keys = {
                lowerChannel + "WebhookUrl",
                lowerChannel + "Webhook",
                lowerChannel + "Url",
                "wecomWebhookUrl",
                "wechatWorkWebhookUrl",
                "enterpriseWechatWebhookUrl",
                "qyWechatWebhookUrl",
                "webhookUrl",
                "webhook",
                "callbackUrl",
                "url"
        };
        for (String key : keys) {
            Object value = values.get(key);
            if (hasText(String.valueOf(value))) {
                return String.valueOf(value).trim();
            }
        }
        Object urls = values.get("webhookUrls");
        if (urls instanceof List<?> list && !list.isEmpty()) {
            return String.valueOf(list.get(0)).trim();
        }
        return "";
    }

    private List<String> emailRecipients(Map<String, Object> receiver) {
        List<String> recipients = new ArrayList<>();
        addEmails(recipients, receiver.get("emails"));
        addEmails(recipients, receiver.get("email"));
        addEmails(recipients, receiver.get("to"));
        addEmails(recipients, receiver.get("recipients"));
        addEmails(recipients, receiver.get("users"));
        return recipients.stream()
                .map(String::trim)
                .filter(value -> EMAIL_ADDRESS.matcher(value).matches())
                .distinct()
                .toList();
    }

    private void addEmails(List<String> recipients, Object value) {
        if (value instanceof List<?> list) {
            list.forEach(item -> addEmails(recipients, item));
            return;
        }
        if (value == null) {
            return;
        }
        String raw = String.valueOf(value);
        for (String token : raw.split("[,;\\s]+")) {
            if (hasText(token)) {
                recipients.add(token);
            }
        }
    }

    private String emailFrom(Map<String, Object> receiver) {
        Object configured = receiver.get("from");
        if (hasText(String.valueOf(configured))) {
            return String.valueOf(configured).trim();
        }
        return mailFrom;
    }

    private Object firstValue(Map<String, Object> values, String... keys) {
        for (String key : keys) {
            if (values.containsKey(key)) {
                return values.get(key);
            }
        }
        return null;
    }

    private boolean isWebhookLike(String channel) {
        return "WEBHOOK".equals(channel)
                || "LARK".equals(channel)
                || "FEISHU".equals(channel)
                || "DINGTALK".equals(channel)
                || "DING".equals(channel)
                || isWeCom(channel);
    }

    private boolean isWeCom(String channel) {
        return "WECOM".equals(channel)
                || "WE_COM".equals(channel)
                || "WECHAT_WORK".equals(channel)
                || "WECHATWORK".equals(channel)
                || "ENTERPRISE_WECHAT".equals(channel)
                || "QYWX".equals(channel);
    }

    private String normalizeChannel(String channel) {
        return channel == null || channel.isBlank() ? "IN_APP" : channel.trim().toUpperCase(Locale.ROOT);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank() && !"null".equalsIgnoreCase(value);
    }

    private String truncate(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value;
        if (normalized.length() > 1000) {
            normalized = normalized.substring(0, 1000);
        }
        try {
            objectMapper.readTree(normalized);
            return normalized;
        } catch (Exception ignored) {
            return normalized;
        }
    }
}
