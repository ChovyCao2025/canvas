package org.chovy.canvas.infrastructure.doris;

import org.chovy.canvas.domain.analytics.BehaviorAudienceRuleCompiler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DorisBehaviorAudienceOlapRepositoryTest {

    @Test
    void countRuleQueriesDwdFactWithBoundParametersAndLimit() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        when(jdbc.query(contains("FROM canvas_dwd.cdp_user_event_fact"),
                org.mockito.ArgumentMatchers.<RowMapper<String>>any(),
                eq(7L), eq("OrderPaid"), eq(30), eq("2"))).thenReturn(List.of("u1", "u2"));
        DorisBehaviorAudienceOlapRepository repository = repository(jdbc);
        BehaviorAudienceRuleCompiler.CompiledBehaviorAudienceQuery query =
                new BehaviorAudienceRuleCompiler.CompiledBehaviorAudienceQuery(
                        7L,
                        "OrderPaid",
                        30,
                        "COUNT",
                        ">=",
                        "2",
                        List.of(),
                        100);

        List<String> users = repository.findMatchingUsers(query);

        assertThat(users).containsExactly("u1", "u2");
        verify(jdbc).query(contains("GROUP BY user_id"),
                org.mockito.ArgumentMatchers.<RowMapper<String>>any(),
                eq(7L), eq("OrderPaid"), eq(30), eq("2"));
        verify(jdbc).query(contains("LIMIT 100"),
                org.mockito.ArgumentMatchers.<RowMapper<String>>any(),
                eq(7L), eq("OrderPaid"), eq(30), eq("2"));
    }

    @Test
    void filtersAreCompiledAsBoundParametersWithoutRawValuesInSql() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        when(jdbc.query(any(String.class),
                org.mockito.ArgumentMatchers.<RowMapper<String>>any(),
                eq(7L), eq("OrderPaid"), eq(30), eq("SMS"), eq("2"))).thenReturn(List.of("u1"));
        DorisBehaviorAudienceOlapRepository repository = repository(jdbc);
        BehaviorAudienceRuleCompiler.CompiledBehaviorAudienceQuery query =
                new BehaviorAudienceRuleCompiler.CompiledBehaviorAudienceQuery(
                        7L,
                        "OrderPaid",
                        30,
                        "COUNT",
                        ">=",
                        "2",
                        List.of(new BehaviorAudienceRuleCompiler.Filter("channel", "=", "SMS")),
                        100);

        List<String> users = repository.findMatchingUsers(query);

        assertThat(users).containsExactly("u1");
        verify(jdbc).query(contains("channel = ?"),
                org.mockito.ArgumentMatchers.<RowMapper<String>>any(),
                eq(7L), eq("OrderPaid"), eq(30), eq("SMS"), eq("2"));
    }

    @Test
    void disabledDorisFailsClosed() {
        DorisBehaviorAudienceOlapRepository repository = repository(null);
        BehaviorAudienceRuleCompiler.CompiledBehaviorAudienceQuery query =
                new BehaviorAudienceRuleCompiler.CompiledBehaviorAudienceQuery(
                        7L, "OrderPaid", 30, "COUNT", ">=", "2", List.of(), 100);

        assertThatThrownBy(() -> repository.findMatchingUsers(query))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Doris is disabled");
    }

    private DorisBehaviorAudienceOlapRepository repository(JdbcTemplate jdbc) {
        ObjectProvider<JdbcTemplate> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(jdbc);
        return new DorisBehaviorAudienceOlapRepository(provider);
    }
}
