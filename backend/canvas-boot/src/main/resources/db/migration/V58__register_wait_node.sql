INSERT INTO node_type_registry
  (type_key, type_name, category, handler_class,
   config_schema, output_schema, outlet_schema, summary_template, runtime_policy_schema,
   risk_level, is_trigger, is_terminal, description, enabled)
VALUES (
  'WAIT',
  '等待',
  '流程控制',
  'org.chovy.canvas.engine.handlers.WaitHandler',
  '[
    {
      "key": "waitType",
      "label": "等待类型",
      "type": "select",
      "required": true,
      "defaultValue": "DURATION",
      "options": [
        {"label": "固定时长", "value": "DURATION"},
        {"label": "直到指定时间", "value": "UNTIL_DATE"},
        {"label": "直到相对时间", "value": "RELATIVE_TIME"},
        {"label": "时间窗口", "value": "TIME_WINDOW"},
        {"label": "直到事件发生", "value": "UNTIL_EVENT"}
      ]
    },
    {
      "key": "duration",
      "label": "等待时长",
      "type": "duration",
      "visible": "waitType==DURATION",
      "required": true
    },
    {
      "key": "untilDate",
      "label": "等待到",
      "type": "datetime",
      "visible": "waitType==UNTIL_DATE",
      "required": true
    },
    {
      "key": "time",
      "label": "相对时间",
      "type": "time",
      "visible": "waitType==RELATIVE_TIME",
      "required": true
    },
    {
      "key": "timeWindow",
      "label": "允许窗口",
      "type": "time-window",
      "visible": "waitType==TIME_WINDOW",
      "required": true
    },
    {
      "key": "eventCode",
      "label": "目标事件",
      "type": "select",
      "dataSource": "/meta/event-definitions",
      "visible": "waitType==UNTIL_EVENT",
      "required": true
    },
    {
      "key": "eventFilters",
      "label": "事件过滤",
      "type": "condition-builder",
      "visible": "waitType==UNTIL_EVENT"
    },
    {
      "key": "maxWait",
      "label": "最长等待",
      "type": "duration",
      "visible": "waitType==UNTIL_EVENT",
      "required": true
    }
  ]',
  '[
    {"name": "waitStatus", "type": "STRING", "label": "等待结果"}
  ]',
  '[
    {"id": "success", "label": "继续", "color": "#52c41a", "targetField": "nextNodeId"},
    {"id": "timeout", "label": "超时", "color": "#faad14", "targetField": "timeoutNodeId"}
  ]',
  '等待（{{waitType}}）',
  '[]',
  'LOW',
  0,
  0,
  '控制旅程节奏，支持固定时长、指定时间、相对时间、时间窗口和等待事件发生；事件等待会持久化订阅并支持超时出口。',
  1
);
