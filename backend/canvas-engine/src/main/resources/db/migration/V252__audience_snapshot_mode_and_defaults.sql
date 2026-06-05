ALTER TABLE audience_definition
    ADD COLUMN default_snapshot_mode VARCHAR(32) NOT NULL DEFAULT 'STATIC_LOCKED'
        COMMENT 'Default TAGGER audience send mode';

CREATE TABLE IF NOT EXISTS audience_snapshot (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    audience_id BIGINT NOT NULL,
    canvas_id BIGINT NULL,
    canvas_version_id BIGINT NULL,
    node_id VARCHAR(128) NULL,
    snapshot_mode VARCHAR(32) NOT NULL,
    user_count BIGINT NOT NULL DEFAULT 0,
    user_ids_json LONGTEXT NOT NULL,
    created_by VARCHAR(128) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_audience_snapshot_source (audience_id, created_at),
    KEY idx_audience_snapshot_canvas (canvas_id, canvas_version_id, node_id)
) COMMENT='Locked audience user lists for static scheduled sends';

UPDATE node_type_registry
SET config_schema = '[{"key":"mode","label":"标签模式","type":"radio","required":true,"options":[{"label":"实时标签","value":"realtime"},{"label":"离线标签","value":"offline"},{"label":"人群圈选","value":"audience"}]},{"key":"tagCodeKey","label":"标签","type":"select","dataSource":"/meta/tagger-tags","required":true,"showWhen":"mode!=audience"},{"key":"audienceId","label":"人群","type":"select","dataSource":"/canvas/audiences/ready","required":true,"showWhen":"mode==audience"},{"key":"audienceSnapshotMode","label":"发送人群","type":"radio","required":false,"showWhen":"mode==audience","options":[{"label":"发布时锁定","value":"STATIC_LOCKED"},{"label":"每次刷新","value":"DYNAMIC_REFRESH"}]},{"key":"audienceSnapshotId","label":"锁定快照","type":"hidden","required":false,"showWhen":"mode==audience"},{"key":"hitNextNodeId","label":"命中分支","type":"edge-hint","showWhen":"mode==audience","icon":"check"},{"key":"missNextNodeId","label":"未命中分支","type":"edge-hint","showWhen":"mode==audience","icon":"close"}]'
WHERE type_key = 'TAGGER';
