package org.chovy.canvas.engine.channel;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * WhatsAppCloudApiConnector 参与 engine.channel 场景的画布执行引擎处理。
 */
@Component("WHATSAPP:CLOUD_API")
public class WhatsAppCloudApiConnector implements ChannelConnector {

    private final WhatsAppCloudApiClient client;
    private final String phoneNumberId;
    private final String accessToken;
    private final String defaultLanguage;

    /**
     * 创建 WhatsAppCloudApiConnector 实例并注入 engine.channel 场景依赖。
     * @param client 依赖组件，用于完成数据访问或外部能力调用。
     * @param phoneNumberId 业务对象 ID，用于定位具体记录。
     * @param accessToken 令牌或锁标识，用于鉴权、幂等或并发控制。
     * @param defaultLanguage default language 参数，用于 WhatsAppCloudApiConnector 流程中的校验、计算或对象转换。
     */
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

    /**
     * mode 处理 engine.channel 场景的业务逻辑。
     * @return 返回 mode 流程生成的业务结果。
     */
    @Override
    public ConnectorMode mode() {
        return ConnectorMode.REAL;
    }

    /**
     * health 查询 engine.channel 场景的业务数据。
     * @return 返回 health 流程生成的业务结果。
     */
    @Override
    public ConnectorHealth health() {
        if (!configured()) {
            return new ConnectorHealth("DOWN", "WhatsApp Cloud API connector not configured");
        }
        return new ConnectorHealth("UP", "WhatsApp Cloud API connector ready");
    }

    /**
     * capabilities 处理 engine.channel 场景的业务逻辑。
     * @return 返回 capabilities 流程生成的业务结果。
     */
    @Override
    public ConnectorCapabilities capabilities() {
        return new ConnectorCapabilities(true, true, Map.of(
                "provider", "CLOUD_API",
                "supportsTemplate", true,
                "supportsSessionText", true));
    }

    /**
     * send 创建或触发 engine.channel 场景的业务处理。
     * @param request 请求对象，承载本次操作的输入参数。
     * @return 返回 send 流程生成的业务结果。
     */
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
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (RuntimeException ex) {
            return new ConnectorSendResult(false, null, "FAILED", ex.getMessage());
        }
    }

    /**
     * parseReceipt 校验或转换 engine.channel 场景的数据。
     * @param rawPayload raw payload 参数，用于 parseReceipt 流程中的校验、计算或对象转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    @Override
    public ConnectorReceiptResult parseReceipt(Map<String, Object> rawPayload) {
        return new ConnectorReceiptResult(null, "UNSUPPORTED", rawPayload == null ? Map.of() : rawPayload);
    }

    @SuppressWarnings("unchecked")
    /**
     * 组装 WhatsApp Cloud API 发送载荷。
     *
     * @param request 标准渠道发送请求
     * @return 可直接提交给供应商 API 的载荷
     */
    private Map<String, Object> providerPayload(ConnectorSendRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("WhatsApp send request is required");
        }
        Map<String, Object> payload = request.payload() == null ? Map.of() : request.payload();
        // Canvas 用户 ID 可能保存为 whatsapp:+E164；Cloud API 需要去掉渠道前缀和加号后的数字。
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
            // 模板消息必须使用 WhatsApp template 信封；会话消息退化为普通文本。
            providerPayload.put("type", "template");
            providerPayload.put("template", template(templateId, content, variables));
        } else {
            providerPayload.put("type", "text");
            providerPayload.put("text", Map.of("body", requiredText(content)));
        }
        return providerPayload;
    }

    /**
     * 构造 WhatsApp 模板消息结构。
     *
     * @param templateId 模板名称
     * @param content 内容配置
     * @param variables 模板变量
     * @return Cloud API template 对象
     */
    private Map<String, Object> template(String templateId,
                                         Map<String, Object> content,
                                         Map<String, Object> variables) {
        Map<String, Object> template = new LinkedHashMap<>();
        template.put("name", templateId.trim());
        template.put("language", Map.of("code", language(content)));
        if (!variables.isEmpty()) {
            List<Map<String, Object>> parameters = new ArrayList<>();
            // 保留变量迭代顺序，确保正文占位符与模板编排顺序一致。
            variables.forEach((key, value) -> parameters.add(Map.of("type", "text", "text", String.valueOf(value))));
            template.put("components", List.of(Map.of("type", "body", "parameters", parameters)));
        }
        return template;
    }

    /**
     * 解析模板语言，缺失时使用默认语言。
     *
     * @param content 内容配置
     * @return WhatsApp 模板语言代码
     */
    private String language(Map<String, Object> content) {
        String value = firstText(string(content.get("language")), string(content.get("languageCode")));
        return isBlank(value) ? defaultLanguage : value;
    }

    /**
     * 读取文本消息正文并校验不能为空。
     *
     * @param content 内容配置
     * @return 去除首尾空白后的文本正文
     */
    private String requiredText(Map<String, Object> content) {
        String body = firstText(string(content.get("body")), string(content.get("content")));
        if (isBlank(body)) {
            throw new IllegalArgumentException("WhatsApp text body is required when templateId is missing");
        }
        return body.trim();
    }

    @SuppressWarnings("unchecked")
    /**
     * 从供应商响应中读取首个消息 ID。
     *
     * @param response 供应商响应
     * @return 首个消息 ID，缺失时返回 null
     */
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
    /**
     * 将对象安全转换为 Map。
     *
     * @param value 原始对象
     * @return Map 值，类型不匹配时返回空 Map
     */
    private Map<String, Object> map(Object value) {
        return value instanceof Map<?, ?> map ? (Map<String, Object>) map : Map.of();
    }

    /**
     * 判断 WhatsApp Cloud API 连接器是否已完成必要配置。
     *
     * @return true 表示可真实发送消息
     */
    private boolean configured() {
        return client != null && !isBlank(phoneNumberId) && !isBlank(accessToken);
    }

    /**
     * 规范化 WhatsApp 收件人手机号。
     *
     * @param userId 用户 ID 或手机号
     * @return 去除渠道前缀和加号后的手机号
     */
    private static String normalizedPhone(String userId) {
        if (isBlank(userId)) {
            return null;
        }
        String value = userId.trim();
        // 内部渠道前缀和开头加号属于路由元数据，不是 Cloud API 收件人 ID 的一部分。
        if (value.toLowerCase(Locale.ROOT).startsWith("whatsapp:")) {
            value = value.substring("whatsapp:".length());
        }
        if (value.startsWith("+")) {
            value = value.substring(1);
        }
        return value;
    }

    /**
     * 将对象转换为字符串。
     *
     * @param value 原始对象
     * @return 字符串值，null 保持为 null
     */
    private static String string(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    /**
     * 在主值为空白时返回兜底文本。
     *
     * @param primary 主文本
     * @param fallback 兜底文本
     * @return 非空白优先文本
     */
    private static String firstText(String primary, String fallback) {
        return isBlank(primary) ? fallback : primary;
    }

    /**
     * 在配置为空白时返回默认值。
     *
     * @param value 原始配置
     * @param fallback 默认配置
     * @return 非空配置
     */
    private static String blankToDefault(String value, String fallback) {
        return isBlank(value) ? fallback : value.trim();
    }

    /**
     * 将空白字符串归一化为 null。
     *
     * @param value 原始字符串
     * @return 去除首尾空白后的字符串或 null
     */
    private static String blankToNull(String value) {
        return isBlank(value) ? null : value.trim();
    }

    /**
     * 判断字符串是否为空白。
     *
     * @param value 待判断字符串
     * @return true 表示 null 或空白
     */
    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
