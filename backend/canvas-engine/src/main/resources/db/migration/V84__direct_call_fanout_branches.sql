-- V84: Allow API entry (DIRECT_CALL) to fan out to multiple downstream nodes.

UPDATE node_type_registry
SET config_schema = '[
      {"key":"eventCode","label":"关联事件（可选）","type":"select","dataSource":"/meta/event-definitions","required":false},
      {"key":"eventParams","label":"入参说明","type":"api-input-params","apiKeyField":"eventCode","defsSource":"/meta/event-definitions","required":false},
      {"key":"branches","label":"下游分支","type":"broadcast-branch-list","required":false}
    ]',
    outlet_schema = '[]'
WHERE type_key = 'DIRECT_CALL';

UPDATE canvas_template
SET graph_json = CAST(JSON_SET(
        graph_json,
        '$.nodes[1].config.branches', CAST('[{"label":"渠道 A","nextNodeId":"api_a"},{"label":"渠道 B","nextNodeId":"api_b"}]' AS JSON),
        '$.nodes[1].bizConfig.branches', CAST('[{"label":"渠道 A","nextNodeId":"api_a"},{"label":"渠道 B","nextNodeId":"api_b"}]' AS JSON)
    ) AS CHAR)
WHERE template_key = 'saas_expansion_signal'
  AND JSON_UNQUOTE(JSON_EXTRACT(graph_json, '$.nodes[1].type')) = 'DIRECT_CALL'
  AND JSON_SEARCH(graph_json, 'one', 'api_b', NULL, '$.nodes[*].id') IS NOT NULL;

UPDATE canvas_version cv
JOIN canvas c ON c.id = cv.canvas_id
SET cv.graph_json = CAST(JSON_SET(
        cv.graph_json,
        '$.nodes[1].config.branches', CAST('[{"label":"渠道 A","nextNodeId":"api_a"},{"label":"渠道 B","nextNodeId":"api_b"}]' AS JSON),
        '$.nodes[1].bizConfig.branches', CAST('[{"label":"渠道 A","nextNodeId":"api_a"},{"label":"渠道 B","nextNodeId":"api_b"}]' AS JSON)
    ) AS CHAR)
WHERE c.source_template_key = 'saas_expansion_signal'
  AND JSON_UNQUOTE(JSON_EXTRACT(cv.graph_json, '$.nodes[1].type')) = 'DIRECT_CALL'
  AND JSON_SEARCH(cv.graph_json, 'one', 'api_b', NULL, '$.nodes[*].id') IS NOT NULL;
