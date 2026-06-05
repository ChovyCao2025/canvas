package org.chovy.canvas.domain.analytics;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BehaviorAudienceRuleCompilerTest {

    private final BehaviorAudienceRuleCompiler compiler = new BehaviorAudienceRuleCompiler(new ObjectMapper());

    @Test
    void compilesBoundedCdpEventMetricRule() {
        String ruleJson = """
                {
                  "source": "CDP_EVENT_METRIC",
                  "eventCode": "OrderPaid",
                  "windowDays": 30,
                  "metric": "COUNT",
                  "operator": ">=",
                  "value": 2,
                  "filters": [
                    {"field": "channel", "operator": "=", "value": "SMS"}
                  ]
                }
                """;

        BehaviorAudienceRuleCompiler.CompiledBehaviorAudienceQuery query =
                compiler.compile(7L, ruleJson, 5000);

        assertThat(query.tenantId()).isEqualTo(7L);
        assertThat(query.eventCode()).isEqualTo("OrderPaid");
        assertThat(query.windowDays()).isEqualTo(30);
        assertThat(query.metric()).isEqualTo("COUNT");
        assertThat(query.operator()).isEqualTo(">=");
        assertThat(query.threshold()).isEqualTo("2");
        assertThat(query.limit()).isEqualTo(5000);
        assertThat(query.filters()).singleElement()
                .satisfies(filter -> {
                    assertThat(filter.field()).isEqualTo("channel");
                    assertThat(filter.operator()).isEqualTo("=");
                    assertThat(filter.value()).isEqualTo("SMS");
                });
    }

    @Test
    void rejectsUnsupportedSourceAndOperator() {
        assertThatThrownBy(() -> compiler.compile(7L, """
                {"source":"RAW_SQL","eventCode":"OrderPaid","windowDays":1,"metric":"COUNT","operator":">=","value":1}
                """, 100))
                .hasMessageContaining("source");

        assertThatThrownBy(() -> compiler.compile(7L, """
                {"source":"CDP_EVENT_METRIC","eventCode":"OrderPaid","windowDays":1,"metric":"COUNT","operator":"LIKE","value":1}
                """, 100))
                .hasMessageContaining("operator");
    }

    @Test
    void rejectsUnsafeOrUnboundedRules() {
        assertThatThrownBy(() -> compiler.compile(null, """
                {"source":"CDP_EVENT_METRIC","eventCode":"OrderPaid","windowDays":1,"metric":"COUNT","operator":">=","value":1}
                """, 100))
                .hasMessageContaining("tenantId");

        assertThatThrownBy(() -> compiler.compile(7L, """
                {"source":"CDP_EVENT_METRIC","eventCode":"","windowDays":1,"metric":"COUNT","operator":">=","value":1}
                """, 100))
                .hasMessageContaining("eventCode");

        assertThatThrownBy(() -> compiler.compile(7L, """
                {"source":"CDP_EVENT_METRIC","eventCode":"OrderPaid","windowDays":0,"metric":"COUNT","operator":">=","value":1}
                """, 100))
                .hasMessageContaining("windowDays");

        assertThatThrownBy(() -> compiler.compile(7L, """
                {"source":"CDP_EVENT_METRIC","eventCode":"OrderPaid","windowDays":1,"metric":"COUNT","operator":">=","value":1,
                 "filters":[{"field":"properties['x']","operator":"=","value":"bad"}]}
                """, 100))
                .hasMessageContaining("filter field");
    }
}
