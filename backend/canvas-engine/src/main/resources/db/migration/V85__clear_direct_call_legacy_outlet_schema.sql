-- V85: Clear stale per-node outletSchema from DIRECT_CALL API entry nodes.
--
-- DIRECT_CALL now renders fan-out handles from config.branches. Older saved
-- graph_json may still contain a single success -> nextNodeId outletSchema,
-- which makes the editor hide the branch handles and route through one outlet.

UPDATE canvas_template
SET graph_json = CAST(JSON_REMOVE(graph_json, '$.nodes[1].outletSchema') AS CHAR)
WHERE JSON_UNQUOTE(JSON_EXTRACT(graph_json, '$.nodes[1].type')) = 'DIRECT_CALL'
  AND JSON_EXTRACT(graph_json, '$.nodes[1].outletSchema') IS NOT NULL;

UPDATE canvas_version
SET graph_json = CAST(JSON_REMOVE(graph_json, '$.nodes[1].outletSchema') AS CHAR)
WHERE JSON_UNQUOTE(JSON_EXTRACT(graph_json, '$.nodes[1].type')) = 'DIRECT_CALL'
  AND JSON_EXTRACT(graph_json, '$.nodes[1].outletSchema') IS NOT NULL;
