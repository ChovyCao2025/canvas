CREATE TABLE IF NOT EXISTS conversation_contact_profile (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT 'Tenant id',
    user_id VARCHAR(128) NOT NULL COMMENT 'Conversation user id',
    display_name VARCHAR(128) NULL COMMENT 'Operator-facing display name',
    external_contact_id VARCHAR(128) NULL COMMENT 'Private-domain or CRM external contact id',
    private_domain_source VARCHAR(64) NULL COMMENT 'Source such as WECOM/WEB_CHAT/WHATSAPP',
    owner VARCHAR(128) NULL COMMENT 'Primary relationship owner',
    lifecycle_stage VARCHAR(64) NULL COMMENT 'Private-domain lifecycle stage',
    tags_json JSON NULL COMMENT 'Profile tags',
    attributes_json JSON NULL COMMENT 'Additional contact attributes',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_conversation_contact_profile_user (tenant_id, user_id),
    INDEX idx_conversation_contact_profile_owner (tenant_id, owner),
    INDEX idx_conversation_contact_profile_external (tenant_id, external_contact_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='SCRM contact profiles';

CREATE TABLE IF NOT EXISTS conversation_work_item (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT 'Tenant id',
    session_id BIGINT NOT NULL COMMENT 'Conversation session id',
    contact_profile_id BIGINT NULL COMMENT 'SCRM contact profile id',
    user_id VARCHAR(128) NOT NULL COMMENT 'Conversation user id',
    channel VARCHAR(32) NOT NULL COMMENT 'Conversation channel',
    provider VARCHAR(64) NOT NULL DEFAULT 'DEFAULT' COMMENT 'Channel provider',
    subject VARCHAR(255) NOT NULL COMMENT 'Inbox subject',
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN' COMMENT 'OPEN/PENDING/SNOOZED/RESOLVED',
    priority VARCHAR(20) NOT NULL DEFAULT 'NORMAL' COMMENT 'LOW/NORMAL/HIGH/URGENT',
    assigned_to VARCHAR(128) NULL COMMENT 'Assigned operator',
    assigned_team VARCHAR(128) NULL COMMENT 'Assigned team',
    source VARCHAR(64) NOT NULL DEFAULT 'CONVERSATION' COMMENT 'Work item source',
    sla_due_at DATETIME NULL COMMENT 'SLA due time',
    next_follow_up_at DATETIME NULL COMMENT 'Next follow-up reminder',
    last_customer_message_at DATETIME NULL COMMENT 'Latest inbound customer message time',
    last_operator_activity_at DATETIME NULL COMMENT 'Latest operator activity time',
    tags_json JSON NULL COMMENT 'Work item tags',
    attributes_json JSON NULL COMMENT 'Additional work item attributes',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_conversation_work_item_session (tenant_id, session_id),
    INDEX idx_conversation_work_item_inbox (tenant_id, status, assigned_to, channel, priority, last_customer_message_at),
    INDEX idx_conversation_work_item_follow_up (tenant_id, next_follow_up_at, status),
    INDEX idx_conversation_work_item_contact (tenant_id, contact_profile_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='SCRM conversation work items';

CREATE TABLE IF NOT EXISTS conversation_sop_task (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT 'Tenant id',
    work_item_id BIGINT NOT NULL COMMENT 'Conversation work item id',
    task_key VARCHAR(128) NOT NULL COMMENT 'Stable SOP task key',
    title VARCHAR(255) NOT NULL COMMENT 'Task title',
    status VARCHAR(20) NOT NULL DEFAULT 'TODO' COMMENT 'TODO/DONE/CANCELLED',
    assignee VARCHAR(128) NULL COMMENT 'Task assignee',
    due_at DATETIME NULL COMMENT 'Task due time',
    completed_by VARCHAR(128) NULL COMMENT 'Completing operator',
    completed_at DATETIME NULL COMMENT 'Completion time',
    metadata_json JSON NULL COMMENT 'Task metadata',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_conversation_sop_task_work_item (tenant_id, work_item_id, status, due_at),
    INDEX idx_conversation_sop_task_assignee (tenant_id, assignee, status, due_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='SCRM SOP tasks';

CREATE TABLE IF NOT EXISTS conversation_work_item_audit (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT 'Tenant id',
    work_item_id BIGINT NOT NULL COMMENT 'Conversation work item id',
    event_type VARCHAR(64) NOT NULL COMMENT 'Audit event type',
    actor VARCHAR(128) NOT NULL DEFAULT 'system' COMMENT 'Operator or system actor',
    old_value_json JSON NULL COMMENT 'Previous values',
    new_value_json JSON NULL COMMENT 'New values',
    note VARCHAR(512) NULL COMMENT 'Operator note',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_conversation_work_item_audit_item (tenant_id, work_item_id, created_at),
    INDEX idx_conversation_work_item_audit_actor (tenant_id, actor, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='SCRM work item audit events';
