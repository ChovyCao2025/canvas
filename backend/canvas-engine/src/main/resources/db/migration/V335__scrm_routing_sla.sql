ALTER TABLE conversation_work_item
    ADD COLUMN routing_status VARCHAR(32) NOT NULL DEFAULT 'UNROUTED' AFTER attributes_json,
    ADD COLUMN required_skills_json JSON NULL AFTER routing_status,
    ADD COLUMN routing_reason VARCHAR(1000) NULL AFTER required_skills_json,
    ADD COLUMN routed_at DATETIME NULL AFTER routing_reason,
    ADD COLUMN sla_policy_key VARCHAR(128) NULL AFTER routed_at,
    ADD INDEX idx_conversation_work_item_routing (tenant_id, routing_status, assigned_team, priority),
    ADD INDEX idx_conversation_work_item_sla_due (tenant_id, sla_due_at, status);

CREATE TABLE IF NOT EXISTS conversation_routing_agent (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL DEFAULT 0,
    agent_key VARCHAR(128) NOT NULL,
    display_name VARCHAR(128) NOT NULL,
    team_key VARCHAR(128) NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'AVAILABLE',
    max_capacity INT NOT NULL DEFAULT 1,
    current_load INT NOT NULL DEFAULT 0,
    skills_json JSON NULL,
    metadata_json JSON NULL,
    created_by VARCHAR(128) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_conversation_routing_agent (tenant_id, agent_key),
    INDEX idx_conversation_routing_agent_match (tenant_id, status, team_key, current_load),
    INDEX idx_conversation_routing_agent_team (tenant_id, team_key, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='SCRM routing agent capacity and skills';

CREATE TABLE IF NOT EXISTS conversation_routing_rule (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL DEFAULT 0,
    rule_key VARCHAR(128) NOT NULL,
    channel VARCHAR(32) NULL,
    min_priority VARCHAR(32) NOT NULL DEFAULT 'NORMAL',
    required_skills_json JSON NULL,
    target_team VARCHAR(128) NULL,
    sla_minutes INT NOT NULL DEFAULT 60,
    enabled TINYINT NOT NULL DEFAULT 1,
    sort_order INT NOT NULL DEFAULT 1000,
    metadata_json JSON NULL,
    created_by VARCHAR(128) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_conversation_routing_rule (tenant_id, rule_key),
    INDEX idx_conversation_routing_rule_match (tenant_id, enabled, channel, min_priority, sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='SCRM routing skill and SLA rules';

CREATE TABLE IF NOT EXISTS conversation_sla_breach (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL DEFAULT 0,
    work_item_id BIGINT NOT NULL,
    breach_type VARCHAR(64) NOT NULL,
    severity VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'OPEN',
    escalation_target VARCHAR(128) NULL,
    reason VARCHAR(1000) NULL,
    due_at DATETIME NOT NULL,
    breached_at DATETIME NOT NULL,
    resolved_by VARCHAR(128) NULL,
    resolved_at DATETIME NULL,
    metadata_json JSON NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_conversation_sla_breach_open (tenant_id, work_item_id, breach_type, status),
    INDEX idx_conversation_sla_breach_status (tenant_id, status, severity, breached_at),
    INDEX idx_conversation_sla_breach_work_item (tenant_id, work_item_id, breached_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='SCRM SLA breach escalation records';
