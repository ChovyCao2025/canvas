package org.chovy.canvas.platform.adapter.persistence;

import org.chovy.canvas.platform.api.MarketingPlatformControlPlaneEvidenceProvider;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 使用 JDBC 从平台表中汇总营销控制面运行时证据。
 */
@Component
public class JdbcMarketingPlatformControlPlaneEvidenceProvider
        implements MarketingPlatformControlPlaneEvidenceProvider {

    /**
     * 生产集成探针被认为新鲜的时间窗口，单位为小时。
     */
    private static final long INTEGRATION_PROBE_FRESHNESS_HOURS = 24;

    /**
     * 集成探针失败告警类型。
     */
    private static final String INTEGRATION_PROBE_FAILURE_ALERT = "INTEGRATION_CONTRACT_PROBE_FAILURE";

    /**
     * 集成 SLO 燃尽率告警类型。
     */
    private static final String INTEGRATION_SLO_BURN_RATE_ALERT = "INTEGRATION_CONTRACT_SLO_BURN_RATE";

    /**
     * 执行控制面证据统计 SQL 的 JDBC 模板。
     */
    private final JdbcTemplate jdbcTemplate;

    /**
     * 使用 JDBC 模板创建证据提供者。
     *
     * @param jdbcTemplate 执行 SQL 的 JDBC 模板
     */
    public JdbcMarketingPlatformControlPlaneEvidenceProvider(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 查询指定租户的控制面运行时证据。
     *
     * @param tenantId 租户标识
     * @return 控制面运行时证据
     */
    @Override
    public RuntimeEvidence evidence(Long tenantId) {
        Long scopedTenantId = tenantId == null || tenantId < 0 ? 0L : tenantId;
        // 只统计最近 24 小时的生产探针，避免陈旧 PASS 掩盖当前集成风险。
        LocalDateTime freshProbeAfter = LocalDateTime.now().minusHours(INTEGRATION_PROBE_FRESHNESS_HOURS);
        return new RuntimeEvidence(
                count("SELECT COUNT(*) FROM canvas WHERE tenant_id = ? AND status = 1", scopedTenantId),
                count("SELECT COUNT(*) FROM marketing_content_release WHERE tenant_id = ? AND status = 'ACTIVE'", scopedTenantId),
                count("SELECT COUNT(*) FROM conversation_work_item WHERE tenant_id = ?", scopedTenantId),
                count("SELECT COUNT(*) FROM marketing_monitor_source WHERE tenant_id = ? AND enabled = 1", scopedTenantId),
                count("SELECT COUNT(*) FROM marketing_monitor_alert_channel WHERE tenant_id = ? AND enabled = 1", scopedTenantId),
                count("SELECT COUNT(*) FROM paid_media_audience_destination WHERE tenant_id = ? AND enabled = 1", scopedTenantId),
                count("SELECT COUNT(*) FROM marketing_monitor_provider_credential WHERE tenant_id = ? AND status = 'ACTIVE'", scopedTenantId),
                count("SELECT COUNT(*) FROM search_marketing_source WHERE tenant_id = ? AND enabled = 1", scopedTenantId),
                count("SELECT COUNT(*) FROM creator_campaign WHERE tenant_id = ? AND status = 'ACTIVE'", scopedTenantId),
                count("SELECT COUNT(*) FROM programmatic_dsp_seat WHERE tenant_id = ? AND enabled = 1", scopedTenantId),
                count("SELECT COUNT(*) FROM bi_dashboard WHERE tenant_id = ? AND status = 'PUBLISHED'", scopedTenantId),
                count("SELECT COUNT(*) FROM search_marketing_mutation WHERE tenant_id = ?", scopedTenantId),
                count("SELECT COUNT(*) FROM search_marketing_mutation WHERE tenant_id = ? AND approval_status = 'PENDING'", scopedTenantId),
                count("SELECT COUNT(*) FROM search_marketing_mutation WHERE tenant_id = ? AND status IN ('FAILED', 'DRY_RUN_FAILED')", scopedTenantId),
                count("SELECT COUNT(*) FROM creator_provider_mutation WHERE tenant_id = ?", scopedTenantId),
                count("SELECT COUNT(*) FROM creator_provider_mutation WHERE tenant_id = ? AND approval_status = 'PENDING'", scopedTenantId),
                count("SELECT COUNT(*) FROM creator_provider_mutation WHERE tenant_id = ? AND status IN ('FAILED', 'DRY_RUN_FAILED')", scopedTenantId),
                count("SELECT COUNT(*) FROM programmatic_dsp_mutation WHERE tenant_id = ?", scopedTenantId),
                count("SELECT COUNT(*) FROM programmatic_dsp_mutation WHERE tenant_id = ? AND approval_status = 'PENDING'", scopedTenantId),
                count("SELECT COUNT(*) FROM programmatic_dsp_mutation WHERE tenant_id = ? AND status IN ('FAILED', 'DRY_RUN_FAILED')", scopedTenantId),
                count("SELECT COUNT(*) FROM marketing_campaign_master WHERE tenant_id = ? AND status = 'ACTIVE'", scopedTenantId),
                count("SELECT COUNT(*) FROM marketing_campaign_link WHERE tenant_id = ?", scopedTenantId),
                count("SELECT COUNT(*) FROM marketing_campaign_link WHERE tenant_id = ? AND required_for_launch = 1", scopedTenantId),
                count("SELECT COUNT(*) FROM marketing_campaign_link WHERE tenant_id = ? AND link_status = 'BLOCKED'", scopedTenantId),
                count("""
                        SELECT COUNT(DISTINCT c.id)
                        FROM marketing_campaign_master c
                        JOIN marketing_campaign_link l
                          ON l.tenant_id = c.tenant_id
                         AND l.campaign_id = c.id
                        WHERE c.tenant_id = ?
                          AND c.status = 'ACTIVE'
                          AND l.required_for_launch = 1
                          AND l.link_status <> 'ACTIVE'
                        """, scopedTenantId),
                count("""
                        SELECT COUNT(*)
                        FROM marketing_campaign_master c
                        WHERE c.tenant_id = ?
                          AND c.status = 'ACTIVE'
                          AND NOT EXISTS (
                            SELECT 1
                            FROM marketing_campaign_link l
                            WHERE l.tenant_id = c.tenant_id
                              AND l.campaign_id = c.id
                              AND l.required_for_launch = 1
                              AND l.link_status = 'ACTIVE'
                              AND l.dependency_role = 'PRIMARY'
                          )
                        """, scopedTenantId),
                count("""
                        SELECT COUNT(*)
                        FROM marketing_campaign_master c
                        WHERE c.tenant_id = ?
                          AND c.status = 'ACTIVE'
                          AND NOT EXISTS (
                            SELECT 1
                            FROM marketing_campaign_link l
                            WHERE l.tenant_id = c.tenant_id
                              AND l.campaign_id = c.id
                              AND l.required_for_launch = 1
                              AND l.link_status = 'ACTIVE'
                              AND (l.dependency_role = 'MEASUREMENT' OR l.resource_type = 'BI_DASHBOARD')
                          )
                        """, scopedTenantId),
                count("SELECT COUNT(*) FROM marketing_integration_contract WHERE tenant_id = ? AND status = 'ACTIVE'", scopedTenantId),
                count("""
                        SELECT COUNT(*)
                        FROM marketing_integration_contract
                        WHERE tenant_id = ?
                          AND environment = 'PRODUCTION'
                          AND status = 'ACTIVE'
                        """, scopedTenantId),
                count("SELECT COUNT(*) FROM marketing_integration_contract WHERE tenant_id = ? AND status = 'BLOCKED'", scopedTenantId),
                count("SELECT COUNT(*) FROM marketing_integration_contract WHERE tenant_id = ? AND status = 'DEGRADED'", scopedTenantId),
                count("""
                        SELECT COUNT(DISTINCT probe.contract_id)
                        FROM marketing_integration_contract_probe_run probe
                        INNER JOIN marketing_integration_contract contract
                          ON contract.id = probe.contract_id
                         AND contract.tenant_id = probe.tenant_id
                        WHERE probe.tenant_id = ?
                          AND contract.environment = 'PRODUCTION'
                          AND contract.status = 'ACTIVE'
                          AND probe.environment = 'PRODUCTION'
                          AND probe.status = 'PASS'
                          AND probe.observed_at >= ?
                        """, scopedTenantId, freshProbeAfter),
                count("""
                        SELECT COUNT(DISTINCT probe.contract_id)
                        FROM marketing_integration_contract_probe_run probe
                        INNER JOIN marketing_integration_contract contract
                          ON contract.id = probe.contract_id
                         AND contract.tenant_id = probe.tenant_id
                        WHERE probe.tenant_id = ?
                          AND contract.environment = 'PRODUCTION'
                          AND contract.status = 'ACTIVE'
                          AND probe.environment = 'PRODUCTION'
                          AND probe.status IN ('WARN', 'FAIL')
                          AND probe.observed_at >= ?
                        """, scopedTenantId, freshProbeAfter),
                count("""
                        SELECT COUNT(*)
                        FROM marketing_monitor_alert
                        WHERE tenant_id = ?
                          AND status = 'OPEN'
                          AND alert_type = ?
                        """, scopedTenantId, INTEGRATION_PROBE_FAILURE_ALERT),
                count("""
                        SELECT COUNT(*)
                        FROM marketing_monitor_alert
                        WHERE tenant_id = ?
                          AND status = 'OPEN'
                          AND alert_type = ?
                        """, scopedTenantId, INTEGRATION_SLO_BURN_RATE_ALERT),
                count("SELECT COUNT(*) FROM growth_activity WHERE tenant_id = ? AND status = 'ACTIVE'", scopedTenantId),
                count("SELECT COUNT(*) FROM growth_reward_pool WHERE tenant_id = ? AND status = 'ACTIVE'", scopedTenantId),
                count("""
                        SELECT COUNT(*)
                        FROM growth_activity
                        WHERE tenant_id = ?
                          AND status = 'ACTIVE'
                          AND campaign_id IS NOT NULL
                          AND dashboard_ref IS NOT NULL
                        """, scopedTenantId),
                count("""
                        SELECT COUNT(*)
                        FROM growth_activity
                        WHERE tenant_id = ?
                          AND status = 'ACTIVE'
                          AND (campaign_id IS NULL OR dashboard_ref IS NULL)
                        """, scopedTenantId));
    }

    /**
     * 执行单值 COUNT 查询。
     *
     * @param sql COUNT 查询语句
     * @param args 查询参数
     * @return 查询结果；表或列缺失等数据访问失败时返回 0
     */
    private long count(String sql, Object... args) {
        try {
            Long value = jdbcTemplate.queryForObject(sql, Long.class, args);
            return value == null ? 0L : value;
        } catch (DataAccessException ignored) {
            // 控制面要能在部分营销表尚未迁移时启动，缺失证据按 0 处理。
            return 0L;
        }
    }
}
