package org.chovy.canvas.platform;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class JdbcPlatformWorkstreamRepository implements PlatformWorkstreamService.WorkstreamRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcPlatformWorkstreamRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

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
        } catch (EmptyResultDataAccessException ignored) {
            return null;
        }
    }
}
