package org.chovy.canvas.domain.demo;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

@Repository
public class JdbcDemoSandboxRepository implements DemoSandboxService.SandboxRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcDemoSandboxRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

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
        } catch (EmptyResultDataAccessException ignored) {
            return null;
        }
    }

    @Override
    public void recordReset(Long tenantId, String operator, Instant resetAt) {
        jdbcTemplate.update("""
                UPDATE demo_sandbox
                SET last_reset_at = ?, last_reset_by = ?, status = 'ACTIVE', updated_at = CURRENT_TIMESTAMP
                WHERE tenant_id = ?
                """, Timestamp.from(resetAt), operator, tenantId);
    }

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

    private static Timestamp toTimestamp(Instant instant) {
        return instant == null ? null : Timestamp.from(instant);
    }

    private static Instant toInstant(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant();
    }
}
