-- V80: Link the Groovy transform example output to the following API_CALL input.

UPDATE canvas_template
SET graph_json = CAST(JSON_SET(
        graph_json,
        '$.nodes[2].config.inputParams', JSON_OBJECT('userId', '$${score}'),
        '$.nodes[2].bizConfig.inputParams', JSON_OBJECT('userId', '$${score}')
    ) AS CHAR)
WHERE template_key = 'component_groovy_transform';

UPDATE canvas_version cv
JOIN canvas c ON c.id = cv.canvas_id
SET cv.graph_json = CAST(JSON_SET(
        cv.graph_json,
        '$.nodes[2].config.inputParams', JSON_OBJECT('userId', '$${score}'),
        '$.nodes[2].bizConfig.inputParams', JSON_OBJECT('userId', '$${score}')
    ) AS CHAR)
WHERE c.source_template_key = 'component_groovy_transform'
  AND JSON_UNQUOTE(JSON_EXTRACT(cv.graph_json, '$.nodes[1].type')) = 'GROOVY'
  AND JSON_UNQUOTE(JSON_EXTRACT(cv.graph_json, '$.nodes[2].type')) = 'API_CALL';
