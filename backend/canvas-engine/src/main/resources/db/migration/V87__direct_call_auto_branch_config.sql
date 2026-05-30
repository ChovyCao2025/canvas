-- V87: Make DIRECT_CALL an API entry whose fan-out branches are created by canvas edges.
--
-- Users should not configure event metadata or pre-create branch rows for the
-- API entry. The editor exposes an append handle and persists connected routes
-- into config.branches automatically.

UPDATE node_type_registry
SET config_schema = '[]',
    outlet_schema = '[]',
    description = '外部系统通过直调接口触发画布；下游分支由画布连线自动生成。'
WHERE type_key = 'DIRECT_CALL';
