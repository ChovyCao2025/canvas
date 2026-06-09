package org.chovy.canvas.platform;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * JdbcPlatformWorkstreamRepository 汇总 platform 场景的平台策略证据。
 */
@Repository
public class JdbcPlatformWorkstreamRepository implements PlatformWorkstreamService.WorkstreamRepository {

    private final JdbcTemplate jdbcTemplate;

    /**
     * 创建 JdbcPlatformWorkstreamRepository 实例并注入 platform 场景依赖。
     * @param jdbcTemplate jdbc template 参数，用于 JdbcPlatformWorkstreamRepository 流程中的校验、计算或对象转换。
     */
    public JdbcPlatformWorkstreamRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * list 查询 platform 场景的业务数据。
     * @return 返回符合条件的数据列表或视图。
     */
    @Override
    public List<PlatformWorkstreamService.Workstream> list() {
        return jdbcTemplate.query("""
                SELECT workstream_key, display_name, priority, requires_child_spec, child_spec_path, summary
                FROM platform_workstream
                ORDER BY priority ASC, workstream_key ASC
                """, (rs, rowNum) -> new PlatformWorkstreamService.Workstream(
                rs.getString("workstream_key"),
                rs.getString("display_name"),
                rs.getString("priority"),
                rs.getInt("requires_child_spec") == 1,
                rs.getString("child_spec_path"),
                rs.getString("summary")));
    }

    /**
     * get 查询 platform 场景的业务数据。
     * @param workstreamKey 业务键，用于在同一租户下定位资源。
     * @return 返回 get 流程生成的业务结果。
     */
    @Override
    public PlatformWorkstreamService.Workstream get(String workstreamKey) {
        try {
            return jdbcTemplate.queryForObject("""
                    SELECT workstream_key, display_name, priority, requires_child_spec, child_spec_path, summary
                    FROM platform_workstream
                    WHERE workstream_key = ?
                    """, (rs, rowNum) -> new PlatformWorkstreamService.Workstream(
                    rs.getString("workstream_key"),
                    rs.getString("display_name"),
                    rs.getString("priority"),
                    rs.getInt("requires_child_spec") == 1,
                    rs.getString("child_spec_path"),
                    rs.getString("summary")), workstreamKey);
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (EmptyResultDataAccessException ignored) {
            return null;
        }
    }
}
