-- V40: extend TAGGER node with audience mode
UPDATE node_type_registry
SET config_schema = '[{"key":"mode","label":"标签模式","type":"radio","required":true,"options":[{"label":"实时触发（监听 MQ 事件）","value":"realtime"},{"label":"离线打标（流程内执行）","value":"offline"},{"label":"人群圈选","value":"audience"}]},{"key":"tagCodeKey","label":"标签","type":"select","dataSource":"/meta/tagger-tags","required":true,"showWhen":"mode!=audience"},{"key":"audienceId","label":"人群","type":"select","dataSource":"/canvas/audiences/ready","required":true,"showWhen":"mode==audience"},{"key":"hitNextNodeId","label":"命中分支","type":"edge-hint","showWhen":"mode==audience","icon":"check"},{"key":"missNextNodeId","label":"未命中分支","type":"edge-hint","showWhen":"mode==audience","icon":"close"}]'
WHERE type_key = 'TAGGER';
