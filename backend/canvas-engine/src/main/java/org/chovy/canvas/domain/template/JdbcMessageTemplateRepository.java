package org.chovy.canvas.domain.template;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

/**
 * JdbcMessageTemplateRepository 编排 domain.template 场景的领域业务规则。
 */
@Repository
public class JdbcMessageTemplateRepository implements MessageTemplateService.TemplateRepository {

    private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<>() {
    };

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    /**
     * 创建 JdbcMessageTemplateRepository 实例并注入 domain.template 场景依赖。
     * @param jdbcTemplate jdbc template 参数，用于 JdbcMessageTemplateRepository 流程中的校验、计算或对象转换。
     * @param objectMapper 依赖组件，用于完成数据访问或外部能力调用。
     */
    public JdbcMessageTemplateRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * insert 处理 domain.template 场景的业务逻辑。
     * @param template template 参数，用于 insert 流程中的校验、计算或对象转换。
     */
    @Override
    public void insert(MessageTemplateService.Template template) {
        jdbcTemplate.update("""
                INSERT INTO message_template (
                    tenant_id,
                    template_code,
                    display_name,
                    channel,
                    body,
                    variable_schema_json,
                    status,
                    created_by
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """,
                template.tenantId(),
                template.templateCode(),
                template.displayName(),
                template.channel(),
                template.body(),
                toJson(template.variables()),
                template.status(),
                template.createdBy());
    }

    /**
     * search 查询 domain.template 场景的业务数据。
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param keyword keyword 参数，用于 search 流程中的校验、计算或对象转换。
     * @param channel channel 参数，用于 search 流程中的校验、计算或对象转换。
     * @return 返回符合条件的数据列表或视图。
     */
    @Override
    public List<MessageTemplateService.Template> search(Long tenantId, String keyword, String channel) {
        StringBuilder sql = new StringBuilder("""
                SELECT tenant_id, template_code, display_name, channel, body, variable_schema_json, status, created_by
                FROM message_template
                WHERE tenant_id = ?
                """);
        List<Object> args = new ArrayList<>();
        args.add(tenantId);
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (keyword != null) {
            sql.append(" AND (template_code LIKE ? OR display_name LIKE ?)");
            String pattern = "%" + keyword + "%";
            args.add(pattern);
            args.add(pattern);
        }
        if (channel != null) {
            sql.append(" AND channel = ?");
            args.add(channel);
        }
        sql.append(" ORDER BY updated_at DESC, id DESC LIMIT 100");
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        return jdbcTemplate.query(sql.toString(), (rs, rowNum) -> new MessageTemplateService.Template(
                rs.getLong("tenant_id"),
                rs.getString("template_code"),
                rs.getString("display_name"),
                rs.getString("channel"),
                rs.getString("body"),
                fromJson(rs.getString("variable_schema_json")),
                rs.getString("status"),
                rs.getString("created_by")), args.toArray());
    }

    /**
     * get 查询 domain.template 场景的业务数据。
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param templateCode 业务编码，用于匹配对应类型或状态。
     * @return 返回 get 流程生成的业务结果。
     */
    @Override
    public MessageTemplateService.Template get(Long tenantId, String templateCode) {
        try {
            return jdbcTemplate.queryForObject("""
                    SELECT tenant_id, template_code, display_name, channel, body, variable_schema_json, status, created_by
                    FROM message_template
                    WHERE tenant_id = ? AND template_code = ?
                    """, (rs, rowNum) -> new MessageTemplateService.Template(
                    rs.getLong("tenant_id"),
                    rs.getString("template_code"),
                    rs.getString("display_name"),
                    rs.getString("channel"),
                    rs.getString("body"),
                    fromJson(rs.getString("variable_schema_json")),
                    rs.getString("status"),
                    rs.getString("created_by")), tenantId, templateCode);
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (EmptyResultDataAccessException ignored) {
            return null;
        }
    }

    /**
     * 转换为接口返回或领域视图。
     *
     * @param variables variables 参数，用于 toJson 流程中的校验、计算或对象转换。
     * @return 返回组装或转换后的结果对象。
     */
    private String toJson(List<String> variables) {
        try {
            return objectMapper.writeValueAsString(variables);
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("invalid template variables", e);
        }
    }

    /**
     * 处理 JSON 序列化或反序列化。
     *
     * @param json JSON 字符串，承载结构化配置或明细。
     * @return 返回组装或转换后的结果对象。
     */
    private List<String> fromJson(String json) {
        try {
            return objectMapper.readValue(json, STRING_LIST_TYPE);
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("invalid stored template variables", e);
        }
    }
}
