-- V86: Repair stale SaaS expansion drafts that still have a single API entry branch.
--
-- The editor loads the latest draft (status = 0) before the published version.
-- Some Canvas46 drafts were saved after the DIRECT_CALL fan-out migration with
-- only one branch, so the canvas still rendered a single "分支 1" outlet.

UPDATE canvas_template
SET graph_json = CAST(JSON_SET(
        JSON_REMOVE(graph_json, '$.nodes[1].outletSchema'),
        '$.nodes[1].config.branches', CAST('[{"label":"渠道 A","nextNodeId":"api_a"},{"label":"渠道 B","nextNodeId":"api_b"}]' AS JSON),
        '$.nodes[1].bizConfig.branches', CAST('[{"label":"渠道 A","nextNodeId":"api_a"},{"label":"渠道 B","nextNodeId":"api_b"}]' AS JSON)
    ) AS CHAR)
WHERE template_key = 'saas_expansion_signal'
  AND JSON_UNQUOTE(JSON_EXTRACT(graph_json, '$.nodes[1].type')) = 'DIRECT_CALL'
  AND JSON_SEARCH(graph_json, 'one', 'api_b', NULL, '$.nodes[*].id') IS NOT NULL
  AND COALESCE(JSON_LENGTH(JSON_EXTRACT(graph_json, '$.nodes[1].config.branches')), 0) < 2;

UPDATE canvas_version cv
JOIN canvas c ON c.id = cv.canvas_id
SET cv.graph_json = CAST(JSON_SET(
        JSON_REMOVE(cv.graph_json, '$.nodes[1].outletSchema'),
        '$.nodes[1].config.branches', CAST('[{"label":"渠道 A","nextNodeId":"api_a"},{"label":"渠道 B","nextNodeId":"api_b"}]' AS JSON),
        '$.nodes[1].bizConfig.branches', CAST('[{"label":"渠道 A","nextNodeId":"api_a"},{"label":"渠道 B","nextNodeId":"api_b"}]' AS JSON)
    ) AS CHAR)
WHERE c.source_template_key = 'saas_expansion_signal'
  AND JSON_UNQUOTE(JSON_EXTRACT(cv.graph_json, '$.nodes[1].type')) = 'DIRECT_CALL'
  AND JSON_SEARCH(cv.graph_json, 'one', 'api_b', NULL, '$.nodes[*].id') IS NOT NULL
  AND COALESCE(JSON_LENGTH(JSON_EXTRACT(cv.graph_json, '$.nodes[1].config.branches')), 0) < 2;
