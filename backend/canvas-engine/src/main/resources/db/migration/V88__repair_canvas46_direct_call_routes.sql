-- V88: Normalize the SaaS expansion API entry after switching DIRECT_CALL to edge-created branches.
--
-- Canvas46 had a stale default nextNodeId and an empty third branch in the
-- draft. Keep only the two real API routes and remove legacy outlet metadata.

UPDATE canvas_template
SET graph_json = CAST(JSON_REMOVE(
        JSON_SET(
            graph_json,
            '$.nodes[1].config.branches', CAST('[{"label":"渠道 A","nextNodeId":"api_a"},{"label":"渠道 B","nextNodeId":"api_b"}]' AS JSON),
            '$.nodes[1].bizConfig.branches', CAST('[{"label":"渠道 A","nextNodeId":"api_a"},{"label":"渠道 B","nextNodeId":"api_b"}]' AS JSON)
        ),
        '$.nodes[1].config.nextNodeId',
        '$.nodes[1].bizConfig.nextNodeId',
        '$.nodes[1].outletSchema'
    ) AS CHAR)
WHERE template_key = 'saas_expansion_signal'
  AND JSON_UNQUOTE(JSON_EXTRACT(graph_json, '$.nodes[1].type')) = 'DIRECT_CALL'
  AND JSON_SEARCH(graph_json, 'one', 'api_a', NULL, '$.nodes[*].id') IS NOT NULL
  AND JSON_SEARCH(graph_json, 'one', 'api_b', NULL, '$.nodes[*].id') IS NOT NULL;

UPDATE canvas_version cv
JOIN canvas c ON c.id = cv.canvas_id
SET cv.graph_json = CAST(JSON_REMOVE(
        JSON_SET(
            cv.graph_json,
            '$.nodes[1].config.branches', CAST('[{"label":"渠道 A","nextNodeId":"api_a"},{"label":"渠道 B","nextNodeId":"api_b"}]' AS JSON),
            '$.nodes[1].bizConfig.branches', CAST('[{"label":"渠道 A","nextNodeId":"api_a"},{"label":"渠道 B","nextNodeId":"api_b"}]' AS JSON)
        ),
        '$.nodes[1].config.nextNodeId',
        '$.nodes[1].bizConfig.nextNodeId',
        '$.nodes[1].outletSchema'
    ) AS CHAR)
WHERE c.source_template_key = 'saas_expansion_signal'
  AND JSON_UNQUOTE(JSON_EXTRACT(cv.graph_json, '$.nodes[1].type')) = 'DIRECT_CALL'
  AND JSON_SEARCH(cv.graph_json, 'one', 'api_a', NULL, '$.nodes[*].id') IS NOT NULL
  AND JSON_SEARCH(cv.graph_json, 'one', 'api_b', NULL, '$.nodes[*].id') IS NOT NULL;
