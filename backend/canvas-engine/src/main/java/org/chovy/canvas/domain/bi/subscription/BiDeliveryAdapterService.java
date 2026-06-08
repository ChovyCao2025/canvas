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
/**
 * BiDeliveryAdapterService 承载对应领域的业务规则、流程编排和结果转换。
 */
public class BiDeliveryAdapterService {

    private static final Duration HTTP_TIMEOUT = Duration.ofSeconds(10);
    private static final Pattern EMAIL_ADDRESS = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private final WebClient.Builder webClientBuilder;
    private final BiEmailDeliveryClient emailDeliveryClient;
    private final String mailFrom;
    private final ObjectMapper objectMapper;

    /**
     * 初始化 BiDeliveryAdapterService 实例。
     *
     * @param webClientBuilder 依赖组件，用于完成数据访问或外部能力调用。
     * @param objectMapper 依赖组件，用于完成数据访问或外部能力调用。
     */
    public BiDeliveryAdapterService(WebClient.Builder webClientBuilder, ObjectMapper objectMapper) {
        this(webClientBuilder, (BiEmailDeliveryClient) null, "", objectMapper);
    }

    @Autowired
    /**
     * 初始化 BiDeliveryAdapterService 实例。
     *
     * @param webClientBuilder 依赖组件，用于完成数据访问或外部能力调用。
     * @param emailDeliveryClientProvider 依赖组件，用于完成数据访问或外部能力调用。
     * @param mailFrom 时间或范围边界，用于限定统计窗口。
     * @param objectMapper 依赖组件，用于完成数据访问或外部能力调用。
     */
    public BiDeliveryAdapterService(WebClient.Builder webClientBuilder,
                                    ObjectProvider<BiEmailDeliveryClient> emailDeliveryClientProvider,
                                    @Value("${canvas.bi.delivery.email.from:}") String mailFrom,
                                    ObjectMapper objectMapper) {
        this(webClientBuilder,
                emailDeliveryClientProvider == null ? null : emailDeliveryClientProvider.getIfAvailable(),
                mailFrom,
                objectMapper);
    }

    /**
     * 初始化 BiDeliveryAdapterService 实例。
     *
     * @param webClientBuilder 依赖组件，用于完成数据访问或外部能力调用。
     * @param emailDeliveryClient 依赖组件，用于完成数据访问或外部能力调用。
     * @param mailFrom 时间或范围边界，用于限定统计窗口。
     * @param objectMapper 依赖组件，用于完成数据访问或外部能力调用。
     */
    public BiDeliveryAdapterService(WebClient.Builder webClientBuilder,
                                    BiEmailDeliveryClient emailDeliveryClient,
                                    String mailFrom,
                                    ObjectMapper objectMapper) {
        this.webClientBuilder = webClientBuilder;
        this.emailDeliveryClient = emailDeliveryClient;
        this.mailFrom = mailFrom == null ? "" : mailFrom.trim();
        this.objectMapper = objectMapper;
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param request 请求对象，承载本次操作的输入参数。
     * @return 返回 deliver 流程生成的业务结果。
     */
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

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param request 请求对象，承载本次操作的输入参数。
     * @return 返回 deliverEmail 流程生成的业务结果。
     */
    private BiDeliveryAdapterResult deliverEmail(BiDeliveryAdapterRequest request) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
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
            // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
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
            // 汇总前面计算出的状态和明细，返回给调用方。
            return BiDeliveryAdapterResult.failed("Email delivery failed", truncate(e.getMessage()));
        }
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param channel channel 参数，用于 deliverWebhook 流程中的校验、计算或对象转换。
     * @param request 请求对象，承载本次操作的输入参数。
     * @return 返回 deliverWebhook 流程生成的业务结果。
     */
    private BiDeliveryAdapterResult deliverWebhook(String channel, BiDeliveryAdapterRequest request) {
        // 准备本次处理所需的上下文和中间变量。
        String url = webhookUrl(channel, request.receiver());
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
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
            // 汇总前面计算出的状态和明细，返回给调用方。
            return BiDeliveryAdapterResult.failed(channel + " webhook delivery failed", truncate(e.getMessage()));
        }
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param url url 参数，用于 postJson 流程中的校验、计算或对象转换。
     * @param MapString map string 参数，用于 postJson 流程中的校验、计算或对象转换。
     * @param body 待处理业务值，用于规则计算、转换或外部调用。
     * @return 返回 post json 生成的文本或业务键。
     */
    protected ResponseEntity<String> postJson(String url, Map<String, Object> body) {
        return webClientBuilder.build()
                .post()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchangeToMono(response -> response.toEntity(String.class))
                .block(HTTP_TIMEOUT);
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param channel channel 参数，用于 webhookBody 流程中的校验、计算或对象转换。
     * @param request 请求对象，承载本次操作的输入参数。
     * @return 返回 webhookBody 流程生成的业务结果。
     */
    private Map<String, Object> webhookBody(String channel, BiDeliveryAdapterRequest request) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
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
        // 汇总前面计算出的状态和明细，返回给调用方。
        return body;
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param request 请求对象，承载本次操作的输入参数。
     * @return 返回 message text 生成的文本或业务键。
     */
    private String messageText(BiDeliveryAdapterRequest request) {
        String title = String.valueOf(request.payload().getOrDefault("title", "BI 通知"));
        String message = String.valueOf(request.payload().getOrDefault("message", ""));
        String url = String.valueOf(request.payload().getOrDefault("url", "/bi"));
        String metric = request.metricValue() == null ? "" : "\n指标值: " + request.metricValue();
        String attachments = attachmentText(request.payload());
        return title + "\n" + message + metric + "\n查看: " + url + attachments;
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param MapString map string 参数，用于 attachmentText 流程中的校验、计算或对象转换。
     * @param payload 待处理业务值，用于规则计算、转换或外部调用。
     * @return 返回 attachment text 生成的文本或业务键。
     */
    private String attachmentText(Map<String, Object> payload) {
        Object extra = payload.get("extra");
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (!(extra instanceof Map<?, ?> extraMap)) {
            return "";
        }
        Object attachments = extraMap.get("attachments");
        if (!(attachments instanceof List<?> values) || values.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder("\n附件:");
        // 遍历候选数据并按业务规则筛选、转换或聚合。
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
        // 汇总前面计算出的状态和明细，返回给调用方。
        return builder.toString();
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param channel channel 参数，用于 webhookUrl 流程中的校验、计算或对象转换。
     * @param MapString map string 参数，用于 webhookUrl 流程中的校验、计算或对象转换。
     * @param receiver receiver 参数，用于 webhookUrl 流程中的校验、计算或对象转换。
     * @return 返回 webhook url 生成的文本或业务键。
     */
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

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param Map map 参数，用于 urlFromMap 流程中的校验、计算或对象转换。
     * @param values values 参数，用于 urlFromMap 流程中的校验、计算或对象转换。
     * @param lowerChannel lower channel 参数，用于 urlFromMap 流程中的校验、计算或对象转换。
     * @return 返回 url from map 生成的文本或业务键。
     */
    private String urlFromMap(Map<?, ?> values, String lowerChannel) {
        // 准备本次处理所需的上下文和中间变量。
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
        // 遍历候选数据并按业务规则筛选、转换或聚合。
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
        // 汇总前面计算出的状态和明细，返回给调用方。
        return "";
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param MapString map string 参数，用于 emailRecipients 流程中的校验、计算或对象转换。
     * @param receiver receiver 参数，用于 emailRecipients 流程中的校验、计算或对象转换。
     * @return 返回 email recipients 汇总后的集合、分页或映射视图。
     */
    private List<String> emailRecipients(Map<String, Object> receiver) {
        // 准备本次处理所需的上下文和中间变量。
        List<String> recipients = new ArrayList<>();
        addEmails(recipients, receiver.get("emails"));
        addEmails(recipients, receiver.get("email"));
        addEmails(recipients, receiver.get("to"));
        addEmails(recipients, receiver.get("recipients"));
        addEmails(recipients, receiver.get("users"));
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        return recipients.stream()
                .map(String::trim)
                .filter(value -> EMAIL_ADDRESS.matcher(value).matches())
                .distinct()
                .toList();
    }

    /**
     * 创建业务对象并完成必要的初始化。
     *
     * @param recipients recipients 参数，用于 addEmails 流程中的校验、计算或对象转换。
     * @param value 待处理值，用于规则计算或转换。
     */
    private void addEmails(List<String> recipients, Object value) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (value instanceof List<?> list) {
            // 遍历候选数据并按业务规则筛选、转换或聚合。
            list.forEach(item -> addEmails(recipients, item));
            return;
        }
        if (value == null) {
            // 汇总前面计算出的状态和明细，返回给调用方。
            return;
        }
        String raw = String.valueOf(value);
        for (String token : raw.split("[,;\\s]+")) {
            if (hasText(token)) {
                recipients.add(token);
            }
        }
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param MapString map string 参数，用于 emailFrom 流程中的校验、计算或对象转换。
     * @param receiver receiver 参数，用于 emailFrom 流程中的校验、计算或对象转换。
     * @return 返回 email from 生成的文本或业务键。
     */
    private String emailFrom(Map<String, Object> receiver) {
        Object configured = receiver.get("from");
        if (hasText(String.valueOf(configured))) {
            return String.valueOf(configured).trim();
        }
        return mailFrom;
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param MapString map string 参数，用于 firstValue 流程中的校验、计算或对象转换。
     * @param values values 参数，用于 firstValue 流程中的校验、计算或对象转换。
     * @param keys keys 参数，用于 firstValue 流程中的校验、计算或对象转换。
     * @return 返回 firstValue 流程生成的业务结果。
     */
    private Object firstValue(Map<String, Object> values, String... keys) {
        for (String key : keys) {
            if (values.containsKey(key)) {
                return values.get(key);
            }
        }
        return null;
    }

    /**
     * 校验输入、权限或业务前置条件。
     *
     * @param channel channel 参数，用于 isWebhookLike 流程中的校验、计算或对象转换。
     * @return 返回布尔判断结果。
     */
    private boolean isWebhookLike(String channel) {
        return "WEBHOOK".equals(channel)
                || "LARK".equals(channel)
                || "FEISHU".equals(channel)
                || "DINGTALK".equals(channel)
                || "DING".equals(channel)
                || isWeCom(channel);
    }

    /**
     * 校验输入、权限或业务前置条件。
     *
     * @param channel channel 参数，用于 isWeCom 流程中的校验、计算或对象转换。
     * @return 返回布尔判断结果。
     */
    private boolean isWeCom(String channel) {
        return "WECOM".equals(channel)
                || "WE_COM".equals(channel)
                || "WECHAT_WORK".equals(channel)
                || "WECHATWORK".equals(channel)
                || "ENTERPRISE_WECHAT".equals(channel)
                || "QYWX".equals(channel);
    }

    /**
     * 解析、归一化或保护输入值，生成安全可用的中间结果。
     *
     * @param channel channel 参数，用于 normalizeChannel 流程中的校验、计算或对象转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private String normalizeChannel(String channel) {
        return channel == null || channel.isBlank() ? "IN_APP" : channel.trim().toUpperCase(Locale.ROOT);
    }

    /**
     * 校验输入、权限或业务前置条件。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回布尔判断结果。
     */
    private boolean hasText(String value) {
        return value != null && !value.isBlank() && !"null".equalsIgnoreCase(value);
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 truncate 生成的文本或业务键。
     */
    private String truncate(String value) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (value == null) {
            return null;
        }
        String normalized = value;
        if (normalized.length() > 1000) {
            normalized = normalized.substring(0, 1000);
        }
        try {
            // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
            objectMapper.readTree(normalized);
            return normalized;
        } catch (Exception ignored) {
            // 汇总前面计算出的状态和明细，返回给调用方。
            return normalized;
        }
    }
}
