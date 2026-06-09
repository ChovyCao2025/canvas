package org.chovy.canvas.engine.audience;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.chovy.canvas.engine.rule.RuleGroup;
import org.chovy.canvas.engine.rule.RuleParser;
import org.chovy.canvas.engine.rule.RuleSqlCompiler;
import org.chovy.canvas.engine.rule.RuleValidationException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 规则 JSON 转 SQL WHERE 片段生成器。
 *
 * <p>用于批处理人群计算，把规则下推给数据库执行。
 */
@Component
@RequiredArgsConstructor
public class SqlWhereGenerator {

    /** 规则 JSON 解析器。 */
    private final ObjectMapper objectMapper;
    private final ConcurrentMap<String, RuleGroup> ruleCache = new ConcurrentHashMap<>();

    /** 生成 SQL 片段与参数集合。 */
    public SqlWhere generate(String ruleJson) throws Exception {
        try {
            RuleSqlCompiler.SqlWhere where = new RuleSqlCompiler()
                    .compile(parseCached(ruleJson));
            return new SqlWhere(where.sql(), where.params());
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (RuleValidationException e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        }
    }

    /**
     * 解析并缓存规则 JSON。
     *
     * @param ruleJson 原始规则 JSON
     * @return 解析后的规则组
     * @throws Exception 规则解析失败时抛出
     */
    private RuleGroup parseCached(String ruleJson) throws Exception {
        String cacheKey = ruleJson == null ? "" : ruleJson;
        RuleGroup cached = ruleCache.get(cacheKey);
        if (cached != null) {
            return cached;
        }
        RuleGroup parsed = new RuleParser(objectMapper).parseAudienceJson(cacheKey);
        ruleCache.put(cacheKey, parsed);
        return parsed;
    }

    /**
     * SQL WHERE 片段及其命名参数。
     *
     * @param sql WHERE 片段，不包含 WHERE 关键字.
     * @param params 与命名参数占位符对应的参数集合.
     */
    public record SqlWhere(
        String sql,
        MapSqlParameterSource params
    ) {
    }
}
