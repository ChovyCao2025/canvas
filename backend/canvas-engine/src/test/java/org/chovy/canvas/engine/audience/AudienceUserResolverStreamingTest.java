package org.chovy.canvas.engine.audience;

import org.chovy.canvas.dal.dataobject.AudienceDefinitionDO;
import org.chovy.canvas.dal.mapper.AudienceDefinitionMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AudienceUserResolverStreamingTest {

    @Test
    void resolveAndProcessStreamsJdbcRowsWithCursorFetchSettings() throws Exception {
        AudienceDefinitionMapper definitionMapper = mock(AudienceDefinitionMapper.class);
        CdpAudienceSourceService cdpAudienceSourceService = mock(CdpAudienceSourceService.class);
        JdbcConfigResolver jdbcConfigResolver = mock(JdbcConfigResolver.class);
        SqlWhereGenerator sqlWhereGenerator = mock(SqlWhereGenerator.class);
        DataSource dataSource = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        PreparedStatement statement = mock(PreparedStatement.class);
        ResultSet resultSet = mock(ResultSet.class);

        AudienceDefinitionDO definition = new AudienceDefinitionDO();
        definition.setId(7L);
        definition.setEnabled(1);
        definition.setDataSourceType("JDBC");
        definition.setDataSourceConfig("{\"id\":1}");
        definition.setRuleJson("{\"logic\":\"AND\"}");
        when(definitionMapper.selectById(7L)).thenReturn(definition);
        when(cdpAudienceSourceService.supports("JDBC")).thenReturn(false);
        when(jdbcConfigResolver.resolve(definition.getDataSourceConfig()))
                .thenReturn(new JdbcConfig(
                        1L,
                        "audience_users",
                        "jdbc:mysql://localhost:3306/canvas",
                        "user",
                        "password",
                        "user_id",
                        "com.mysql.cj.jdbc.Driver",
                        10_000));
        when(sqlWhereGenerator.generate(definition.getRuleJson()))
                .thenReturn(new SqlWhereGenerator.SqlWhere(
                        "city = :p1 AND score >= :p2",
                        new MapSqlParameterSource()
                                .addValue("p1", "Shanghai")
                                .addValue("p2", 10)));
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(
                "SELECT user_id FROM audience_users WHERE city = ? AND score >= ? LIMIT ?",
                ResultSet.TYPE_FORWARD_ONLY,
                ResultSet.CONCUR_READ_ONLY)).thenReturn(statement);
        when(statement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenAnswer(invocation -> {
            int index = rowIndex++;
            if (index >= 10_000) {
                return false;
            }
            currentUserId = "user-" + index;
            return true;
        });
        when(resultSet.getString(1)).thenAnswer(invocation -> currentUserId);

        AudienceUserResolver resolver = new AudienceUserResolver(
                definitionMapper,
                cdpAudienceSourceService,
                jdbcConfigResolver,
                sqlWhereGenerator,
                ignored -> dataSource);

        List<String> processed = new ArrayList<>();
        long count = resolver.resolveAndProcess(7L, processed::add);

        assertThat(count).isEqualTo(10_000);
        assertThat(processed).hasSize(10_000);
        assertThat(processed.getFirst()).isEqualTo("user-0");
        assertThat(processed.getLast()).isEqualTo("user-9999");
        verify(connection).prepareStatement(
                "SELECT user_id FROM audience_users WHERE city = ? AND score >= ? LIMIT ?",
                ResultSet.TYPE_FORWARD_ONLY,
                ResultSet.CONCUR_READ_ONLY);
        verify(statement).setFetchSize(Integer.MIN_VALUE);
        verify(statement).setObject(1, "Shanghai");
        verify(statement).setObject(2, 10);
        verify(statement).setObject(3, 10_000);
        verify(resultSet).close();
        verify(statement).close();
        verify(connection).close();
    }

    private int rowIndex;
    private String currentUserId;
}
