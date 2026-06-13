package org.chovy.canvas.platform.adapter.persistence;

import org.chovy.canvas.platform.api.MarketingPlatformControlPlaneEvidenceProvider;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class JdbcMarketingPlatformControlPlaneEvidenceProvider
        implements MarketingPlatformControlPlaneEvidenceProvider {

    private static final long INTEGRATION_PROBE_FRESHNESS_HOURS = 24;
    private static final String INTEGRATION_PROBE_FAILURE_ALERT = "INTEGRATION_CONTRACT_PROBE_FAILURE";
    private static final String INTEGRATION_SLO_BURN_RATE_ALERT = "INTEGRATION_CONTRACT_SLO_BURN_RATE";

    private final JdbcTemplate jdbcTemplate;

    public JdbcMarketingPlatformControlPlaneEvidenceProvider(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public RuntimeEvidence evidence(Long tenantId) {
        Long scopedTenantId = tenantId == null || tenantId < 0 ? 0L : tenantId;
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

    private long count(String sql, Object... args) {
        try {
            Long value = jdbcTemplate.queryForObject(sql, Long.class, args);
            return value == null ? 0L : value;
        } catch (DataAccessException ignored) {
            return 0L;
        }
    }
}
