package org.chovy.canvas.engine.rule;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;

import static org.assertj.core.api.Assertions.assertThat;

class RuleSqlCompilerTest {

    private final RuleParser parser = new RuleParser(new ObjectMapper());
    private final RuleSqlCompiler compiler = new RuleSqlCompiler();

    @Test
    void compilesNestedAudienceRuleToParameterizedSql() throws Exception {
        RuleGroup rule = parser.parseAudienceJson("""
                {
                  "logic":"AND",
                  "conditions":[
                    {"field":"last_purchase_days","op":"<=","value":30},
                    {"field":"vip_level","op":">=","value":2}
                  ],
                  "groups":[
                    {
                      "logic":"OR",
                      "conditions":[
                        {"field":"city","op":"IN","value":["Beijing","Shanghai"]},
                        {"field":"order_count","op":">","value":5}
                      ]
                    }
                  ]
                }
                """);

        RuleSqlCompiler.SqlWhere where = compiler.compile(rule);

        assertThat(where.sql()).isEqualTo("last_purchase_days <= :p1 AND vip_level >= :p2 AND (city IN (:p3) OR order_count > :p4)");
        MapSqlParameterSource params = where.params();
        assertThat(params.getValue("p1")).isEqualTo(30);
        assertThat(params.getValue("p2")).isEqualTo(2);
        assertThat(params.getValue("p3")).asList().containsExactly("Beijing", "Shanghai");
        assertThat(params.getValue("p4")).isEqualTo(5);
    }
}
