package org.chovy.canvas.domain.template;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Repository
public class JdbcMessageTemplateRepository implements MessageTemplateService.TemplateRepository {

    private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<>() {
    };

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public JdbcMessageTemplateRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

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

    @Override
    public List<MessageTemplateService.Template> search(Long tenantId, String keyword, String channel) {
        StringBuilder sql = new StringBuilder("""
                SELECT tenant_id, template_code, display_name, channel, body, variable_schema_json, status, created_by
                FROM message_template
                WHERE tenant_id = ?
                """);
        List<Object> args = new ArrayList<>();
        args.add(tenantId);
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
        } catch (EmptyResultDataAccessException ignored) {
            return null;
        }
    }

    private String toJson(List<String> variables) {
        try {
            return objectMapper.writeValueAsString(variables);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("invalid template variables", e);
        }
    }

    private List<String> fromJson(String json) {
        try {
            return objectMapper.readValue(json, STRING_LIST_TYPE);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("invalid stored template variables", e);
        }
    }
}
