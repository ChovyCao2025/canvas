package org.chovy.canvas.domain.demo;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

/**
 * JdbcDemoSandboxRepository 编排 domain.demo 场景的领域业务规则。
 */
@Repository
public class JdbcDemoSandboxRepository implements DemoSandboxService.SandboxRepository {

    private final JdbcTemplate jdbcTemplate;

    /**
     * 创建 JdbcDemoSandboxRepository 实例并注入 domain.demo 场景依赖。
     * @param jdbcTemplate jdbc template 参数，用于 JdbcDemoSandboxRepository 流程中的校验、计算或对象转换。
     */
    public JdbcDemoSandboxRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * upsert 更新 domain.demo 场景的业务状态。
     * @param sandbox sandbox 参数，用于 upsert 流程中的校验、计算或对象转换。
     */
    @Override
    public void upsert(DemoSandboxService.Sandbox sandbox) {
        jdbcTemplate.update("""
                INSERT INTO demo_sandbox (
                    tenant_id,
                    demo_name,
                    demo_marker,
                    status,
                    expires_at,
                    last_reset_at
                ) VALUES (?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                    demo_name = VALUES(demo_name),
                    demo_marker = VALUES(demo_marker),
                    status = VALUES(status),
                    expires_at = VALUES(expires_at),
                    updated_at = CURRENT_TIMESTAMP
                """,
                sandbox.tenantId(),
                sandbox.demoName(),
                sandbox.demoMarker(),
                sandbox.status(),
                Timestamp.from(sandbox.expiresAt()),
                toTimestamp(sandbox.lastResetAt()));
    }

    /**
     * get 查询 domain.demo 场景的业务数据。
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @return 返回 get 流程生成的业务结果。
     */
    @Override
    public DemoSandboxService.Sandbox get(Long tenantId) {
        try {
            return jdbcTemplate.queryForObject("""
                    SELECT tenant_id, demo_name, demo_marker, status, expires_at, last_reset_at
                    FROM demo_sandbox
                    WHERE tenant_id = ?
                    """, (rs, rowNum) -> new DemoSandboxService.Sandbox(
                    rs.getLong("tenant_id"),
                    rs.getString("demo_name"),
                    rs.getString("demo_marker"),
                    rs.getString("status"),
                    rs.getTimestamp("expires_at").toInstant(),
                    toInstant(rs.getTimestamp("last_reset_at"))), tenantId);
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (EmptyResultDataAccessException ignored) {
            return null;
        }
    }

    /**
     * recordReset 处理 domain.demo 场景的业务逻辑。
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param operator 操作人标识，用于审计和权限判断。
     * @param resetAt 时间参数，用于计算窗口、过期或审计时间。
     */
    @Override
    public void recordReset(Long tenantId, String operator, Instant resetAt) {
        jdbcTemplate.update("""
                UPDATE demo_sandbox
                SET last_reset_at = ?, last_reset_by = ?, status = 'ACTIVE', updated_at = CURRENT_TIMESTAMP
                WHERE tenant_id = ?
                """, Timestamp.from(resetAt), operator, tenantId);
    }

    /**
     * findExpired 查询 domain.demo 场景的业务数据。
     * @param now 时间参数，用于计算窗口、过期或审计时间。
     * @return 返回符合条件的数据列表或视图。
     */
    @Override
    public List<DemoSandboxService.Sandbox> findExpired(Instant now) {
        return jdbcTemplate.query("""
                SELECT tenant_id, demo_name, demo_marker, status, expires_at, last_reset_at
                FROM demo_sandbox
                WHERE expires_at <= ?
                ORDER BY expires_at ASC, tenant_id ASC
                """, (rs, rowNum) -> new DemoSandboxService.Sandbox(
                rs.getLong("tenant_id"),
                rs.getString("demo_name"),
                rs.getString("demo_marker"),
                rs.getString("status"),
                rs.getTimestamp("expires_at").toInstant(),
                toInstant(rs.getTimestamp("last_reset_at"))), Timestamp.from(now));
    }

    /**
     * 转换为接口返回或领域视图。
     *
     * @param instant instant 参数，用于 toTimestamp 流程中的校验、计算或对象转换。
     * @return 返回组装或转换后的结果对象。
     */
    private static Timestamp toTimestamp(Instant instant) {
        return instant == null ? null : Timestamp.from(instant);
    }

    /**
     * 转换为接口返回或领域视图。
     *
     * @param timestamp 时间参数，用于计算窗口、过期或审计时间。
     * @return 返回组装或转换后的结果对象。
     */
    private static Instant toInstant(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant();
    }
}
