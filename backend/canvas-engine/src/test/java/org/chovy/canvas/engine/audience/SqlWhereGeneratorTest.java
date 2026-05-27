package org.chovy.canvas.engine.audience;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SqlWhereGeneratorTest {

    private final SqlWhereGenerator generator = new SqlWhereGenerator(new com.fasterxml.jackson.databind.ObjectMapper());

    @Test
    void generates_nested_where_clause() throws Exception {
        String ruleJson = """
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
                """;

        SqlWhereGenerator.SqlWhere where = generator.generate(ruleJson);

        assertThat(where.sql()).isEqualTo("last_purchase_days <= :p1 AND vip_level >= :p2 AND (city IN (:p3) OR order_count > :p4)");
        MapSqlParameterSource params = where.params();
        assertThat(params.getValue("p1")).isEqualTo(30);
        assertThat(params.getValue("p2")).isEqualTo(2);
        java.util.List<?> inValues = (java.util.List<?>) params.getValue("p3");
        assertThat(inValues).hasSize(2);
        assertThat(inValues.stream().map(String::valueOf).toList()).contains("Beijing", "Shanghai");
        assertThat(params.getValue("p4")).isEqualTo(5);
    }

    @Test
    void rejects_illegal_identifier() {
        String ruleJson = """
                {
                  "logic":"AND",
                  "conditions":[
                    {"field":"user_id;drop table","op":"=","value":1}
                  ]
                }
                """;

        assertThatThrownBy(() -> generator.generate(ruleJson))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Illegal field name");
    }

    @Test
    void supports_lowercase_in_operator_from_ui_options() throws Exception {
        String ruleJson = """
                {
                  "logic":"AND",
                  "conditions":[
                    {"field":"city","op":"in","value":["Beijing","Shanghai"]}
                  ]
                }
                """;

        SqlWhereGenerator.SqlWhere where = generator.generate(ruleJson);

        assertThat(where.sql()).isEqualTo("city IN (:p1)");
    }
}
