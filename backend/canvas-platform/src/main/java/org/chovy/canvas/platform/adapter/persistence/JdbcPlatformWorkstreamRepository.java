package org.chovy.canvas.platform.adapter.persistence;

import org.chovy.canvas.platform.domain.PlatformWorkstream;
import org.chovy.canvas.platform.domain.PlatformWorkstreamRepository;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 使用 JDBC 读取平台工作流定义。
 */
@Repository
public class JdbcPlatformWorkstreamRepository implements PlatformWorkstreamRepository {

    /**
     * 执行平台工作流 SQL 的 JDBC 模板。
     */
    private final JdbcTemplate jdbcTemplate;

    /**
     * 使用 JDBC 模板创建仓储。
     *
     * @param jdbcTemplate 执行 SQL 的 JDBC 模板
     */
    public JdbcPlatformWorkstreamRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 按优先级和稳定键查询全部工作流。
     *
     * @return 平台工作流列表
     */
    @Override
    public List<PlatformWorkstream> list() {
        return jdbcTemplate.query("""
                SELECT workstream_key, display_name, priority, requires_child_spec, child_spec_path, summary
                FROM platform_workstream
                ORDER BY priority ASC, workstream_key ASC
                """, (rs, rowNum) -> new PlatformWorkstream(
                rs.getString("workstream_key"),
                rs.getString("display_name"),
                rs.getString("priority"),
                rs.getInt("requires_child_spec") == 1,
                rs.getString("child_spec_path"),
                rs.getString("summary")));
    }

    /**
     * 按稳定键查询工作流。
     *
     * @param workstreamKey 工作流稳定键
     * @return 匹配的工作流；没有记录时返回 null
     */
    @Override
    public PlatformWorkstream get(String workstreamKey) {
        try {
            return jdbcTemplate.queryForObject("""
                    SELECT workstream_key, display_name, priority, requires_child_spec, child_spec_path, summary
                    FROM platform_workstream
                    WHERE workstream_key = ?
                    """, (rs, rowNum) -> new PlatformWorkstream(
                    rs.getString("workstream_key"),
                    rs.getString("display_name"),
                    rs.getString("priority"),
                    rs.getInt("requires_child_spec") == 1,
                    rs.getString("child_spec_path"),
                    rs.getString("summary")), workstreamKey);
        } catch (EmptyResultDataAccessException ignored) {
            // 仓储接口以 null 表示未找到，应用层负责转换为业务异常。
            return null;
        }
    }
}
