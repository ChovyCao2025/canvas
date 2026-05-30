-- V83: Make the SaaS expansion example executable through the public direct-call endpoint.
--
-- The original template starts with API_CALL nodes, so publish-time validation
-- rejects canvases created from it because no trigger node exists. Insert a
-- DIRECT_CALL/API entry node between START and the first API_CALL.

UPDATE canvas_template
SET graph_json = CAST(JSON_ARRAY_INSERT(
        JSON_SET(
            graph_json,
            '$.nodes[0].config.nextNodeId', 'direct',
            '$.nodes[0].bizConfig.nextNodeId', 'direct'
        ),
        '$.nodes[1]',
        CAST('{
          "id":"direct",
          "type":"DIRECT_CALL",
          "name":"API入口",
          "category":"入口节点",
          "x":400,
          "y":120,
          "config":{"nextNodeId":"api_a"},
          "bizConfig":{"nextNodeId":"api_a"}
        }' AS JSON)
    ) AS CHAR),
    covered_node_types = 'DIRECT_CALL,API_CALL,AGGREGATE,SEND_MQ'
WHERE template_key = 'saas_expansion_signal'
  AND JSON_SEARCH(graph_json, 'one', 'DIRECT_CALL', NULL, '$.nodes[*].type') IS NULL;

UPDATE canvas_version cv
JOIN canvas c ON c.id = cv.canvas_id
SET cv.graph_json = CAST(JSON_ARRAY_INSERT(
        JSON_SET(
            cv.graph_json,
            '$.nodes[0].config.nextNodeId', 'direct',
            '$.nodes[0].bizConfig.nextNodeId', 'direct'
        ),
        '$.nodes[1]',
        CAST('{
          "id":"direct",
          "type":"DIRECT_CALL",
          "name":"API入口",
          "category":"入口节点",
          "x":400,
          "y":120,
          "config":{"nextNodeId":"api_a"},
          "bizConfig":{"nextNodeId":"api_a"}
        }' AS JSON)
    ) AS CHAR)
WHERE c.source_template_key = 'saas_expansion_signal'
  AND JSON_SEARCH(cv.graph_json, 'one', 'DIRECT_CALL', NULL, '$.nodes[*].type') IS NULL;
