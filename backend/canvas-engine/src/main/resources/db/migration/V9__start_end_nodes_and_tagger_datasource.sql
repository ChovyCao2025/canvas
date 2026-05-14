-- V9: 新增 START/END 节点类型 + TAGGER_OFFLINE 标签 dataSource 配置化

-- START 节点：流程图入口标记，is_trigger=1，无入边
INSERT INTO `node_type_registry`
    (`type_key`, `type_name`, `category`, `handler_class`, `config_schema`,
     `output_schema`, `is_trigger`, `is_terminal`, `description`, `enabled`)
VALUES (
    'START',
    '开始',
    '流程控制',
    'org.chovy.canvas.engine.handlers.StartHandler',
    '[]',
    '[]',
    1, 0,
    '流程图的起点，拖入画布后连接第一个执行节点即可，无需配置。',
    1
);

-- END 节点：流程图出口标记，is_terminal=1，无出边
INSERT INTO `node_type_registry`
    (`type_key`, `type_name`, `category`, `handler_class`, `config_schema`,
     `output_schema`, `is_trigger`, `is_terminal`, `description`, `enabled`)
VALUES (
    'END',
    '结束',
    '流程控制',
    'org.chovy.canvas.engine.handlers.EndHandler',
    '[]',
    '[]',
    0, 1,
    '流程图的终点，接在最后一个节点之后，明确标识流程结束位置，无需配置。',
    1
);

-- 更新 TAGGER_OFFLINE config_schema：tagCodeKey 从动态接口获取标签列表
UPDATE `node_type_registry`
SET `config_schema` = '[
  {"key":"tagCodeKey","label":"标签 Code","type":"select","dataSource":"/meta/tagger-tags?type=offline","required":true},
  {"key":"nextNodeId","label":"后继节点","type":"input","required":false}
]'
WHERE `type_key` = 'TAGGER_OFFLINE';

-- 更新 TAGGER_REALTIME config_schema：同步更新实时标签 dataSource
UPDATE `node_type_registry`
SET `config_schema` = '[
  {"key":"tagCodeKey","label":"标签 Code","type":"select","dataSource":"/meta/tagger-tags?type=realtime","required":true},
  {"key":"nextNodeId","label":"后继节点","type":"input","required":false}
]'
WHERE `type_key` = 'TAGGER_REALTIME';
