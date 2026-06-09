package org.chovy.canvas.domain.template;

import org.chovy.canvas.common.tenant.TenantContext;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
/**
 * MessageTemplateService 承载对应领域的业务规则、流程编排和结果转换。
 */
public class MessageTemplateService {

    private static final Set<String> SUPPORTED_CHANNELS = Set.of("SMS", "EMAIL", "PUSH", "WECHAT", "IN_APP", "WEBHOOK");
    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{\\{\\s*([A-Za-z][A-Za-z0-9_]*)\\s*}}");

    private final TemplateRepository repository;

    /**
     * 初始化 MessageTemplateService 实例。
     *
     * @param repository 依赖组件，用于完成数据访问或外部能力调用。
     */
    public MessageTemplateService(TemplateRepository repository) {
        this.repository = repository;
    }

    /**
     * 创建业务对象并完成必要的初始化。
     *
     * @param tenant tenant 参数，用于 create 流程中的校验、计算或对象转换。
     * @param draft draft 参数，用于 create 流程中的校验、计算或对象转换。
     * @return 返回流程执行后的业务结果。
     */
    public Template create(TenantContext tenant, TemplateDraft draft) {
        Long tenantId = requireTenantId(tenant);
        String templateCode = normalizeTemplateCode(draft.templateCode());
        String channel = normalizeChannel(draft.channel());
        requireText(draft.displayName(), "display name is required");
        requireText(draft.body(), "template body is required");

        Template template = new Template(
                tenantId,
                templateCode,
                draft.displayName().trim(),
                channel,
                draft.body(),
                extractVariables(draft.body()),
                "DRAFT",
                operator(tenant));
        repository.insert(template);
        return template;
    }

    /**
     * 查询并组装符合条件的业务数据。
     *
     * @param tenant tenant 参数，用于 search 流程中的校验、计算或对象转换。
     * @param keyword keyword 参数，用于 search 流程中的校验、计算或对象转换。
     * @param channel channel 参数，用于 search 流程中的校验、计算或对象转换。
     * @return 返回符合条件的数据列表或视图。
     */
    public List<Template> search(TenantContext tenant, String keyword, String channel) {
        Long tenantId = requireTenantId(tenant);
        return repository.search(tenantId, normalizeKeyword(keyword), normalizeOptionalChannel(channel));
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param tenant tenant 参数，用于 preview 流程中的校验、计算或对象转换。
     * @param templateCode 业务编码，用于匹配对应类型或状态。
     * @param MapString map string 参数，用于 preview 流程中的校验、计算或对象转换。
     * @param context 上下文对象，承载租户、身份或运行时信息。
     * @return 返回 preview 流程生成的业务结果。
     */
    public PreviewResult preview(TenantContext tenant, String templateCode, Map<String, Object> context) {
        Long tenantId = requireTenantId(tenant);
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        Template template = repository.get(tenantId, normalizeTemplateCode(templateCode));
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (template == null) {
            throw new IllegalArgumentException("template not found: " + templateCode);
        }
        Map<String, Object> safeContext = context == null ? Map.of() : context;
        LinkedHashSet<String> missing = new LinkedHashSet<>();
        Matcher matcher = VARIABLE_PATTERN.matcher(template.body());
        StringBuffer rendered = new StringBuffer();
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        while (matcher.find()) {
            String variable = matcher.group(1);
            Object value = safeContext.get(variable);
            if (value == null) {
                missing.add(variable);
                matcher.appendReplacement(rendered, Matcher.quoteReplacement(matcher.group(0)));
            } else {
                matcher.appendReplacement(rendered, Matcher.quoteReplacement(String.valueOf(value)));
            }
        }
        matcher.appendTail(rendered);
        return new PreviewResult(rendered.toString(), List.copyOf(missing));
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param body 待处理业务值，用于规则计算、转换或外部调用。
     * @return 返回 extract variables 汇总后的集合、分页或映射视图。
     */
    public static List<String> extractVariables(String body) {
        if (body == null || body.isBlank()) {
            return List.of();
        }
        LinkedHashSet<String> variables = new LinkedHashSet<>();
        Matcher matcher = VARIABLE_PATTERN.matcher(body);
        while (matcher.find()) {
            variables.add(matcher.group(1));
        }
        return List.copyOf(variables);
    }

    /**
     * 校验输入、权限或业务前置条件。
     *
     * @param tenant tenant 参数，用于 requireTenantId 流程中的校验、计算或对象转换。
     * @return 返回 require tenant id 计算得到的数量、金额或指标值。
     */
    private static Long requireTenantId(TenantContext tenant) {
        if (tenant == null || tenant.tenantId() == null) {
            throw new SecurityException("AUTH_003: missing tenant context");
        }
        return tenant.tenantId();
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param tenant tenant 参数，用于 operator 流程中的校验、计算或对象转换。
     * @return 返回 operator 生成的文本或业务键。
     */
    private static String operator(TenantContext tenant) {
        if (tenant.username() == null || tenant.username().isBlank()) {
            return "unknown";
        }
        return tenant.username().trim();
    }

    /**
     * 解析、归一化或保护输入值，生成安全可用的中间结果。
     *
     * @param templateCode 业务编码，用于匹配对应类型或状态。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private static String normalizeTemplateCode(String templateCode) {
        requireText(templateCode, "template code is required");
        String normalized = Objects.requireNonNull(templateCode).trim().toLowerCase(Locale.ROOT);
        if (!normalized.matches("[a-z0-9][a-z0-9_-]{0,127}")) {
            throw new IllegalArgumentException("invalid template code: " + templateCode);
        }
        return normalized;
    }

    /**
     * 解析、归一化或保护输入值，生成安全可用的中间结果。
     *
     * @param channel channel 参数，用于 normalizeChannel 流程中的校验、计算或对象转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private static String normalizeChannel(String channel) {
        requireText(channel, "template channel is required");
        String normalized = channel.trim().toUpperCase(Locale.ROOT);
        if (!SUPPORTED_CHANNELS.contains(normalized)) {
            throw new IllegalArgumentException("unsupported template channel " + normalized);
        }
        return normalized;
    }

    /**
     * 解析、归一化或保护输入值，生成安全可用的中间结果。
     *
     * @param channel channel 参数，用于 normalizeOptionalChannel 流程中的校验、计算或对象转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private static String normalizeOptionalChannel(String channel) {
        if (channel == null || channel.isBlank()) {
            return null;
        }
        return normalizeChannel(channel);
    }

    /**
     * 解析、归一化或保护输入值，生成安全可用的中间结果。
     *
     * @param keyword keyword 参数，用于 normalizeKeyword 流程中的校验、计算或对象转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private static String normalizeKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        return keyword.trim();
    }

    /**
     * 校验输入、权限或业务前置条件。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param message 原因或消息文本，用于记录状态变化的业务依据。
     */
    private static void requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
    }

    /**
     * TemplateDraft 承载对应领域的业务规则、流程编排和结果转换。
     */
    public record TemplateDraft(String templateCode, String displayName, String channel, String body) {
    }

    /**
     * Template 承载对应领域的业务规则、流程编排和结果转换。
     */
    public record Template(
            Long tenantId,
            String templateCode,
            String displayName,
            String channel,
            String body,
            List<String> variables,
            String status,
            String createdBy) {
    }

    /**
     * PreviewResult 承载对应领域的业务规则、流程编排和结果转换。
     */
    public record PreviewResult(String renderedBody, List<String> missingVariables) {
    }

    /**
     * TemplateRepository 承载对应领域的业务规则、流程编排和结果转换。
     */
    public interface TemplateRepository {
        /**
         * 写入或更新业务数据，并保持关联状态一致。
         *
         * @param template template 参数，用于 insert 流程中的校验、计算或对象转换。
         */
        void insert(Template template);

        /**
         * 查询并组装符合条件的业务数据。
         *
         * @param tenantId 租户 ID，用于限定数据隔离范围。
         * @param keyword keyword 参数，用于 search 流程中的校验、计算或对象转换。
         * @param channel channel 参数，用于 search 流程中的校验、计算或对象转换。
         * @return 返回符合条件的数据列表或视图。
         */
        List<Template> search(Long tenantId, String keyword, String channel);

        /**
         * 根据方法职责完成对应的业务处理流程。
         *
         * @param tenantId 租户 ID，用于限定数据隔离范围。
         * @param templateCode 业务编码，用于匹配对应类型或状态。
         * @return 返回 get 流程生成的业务结果。
         */
        Template get(Long tenantId, String templateCode);
    }
}
