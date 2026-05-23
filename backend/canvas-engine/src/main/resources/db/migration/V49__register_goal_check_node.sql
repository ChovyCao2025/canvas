INSERT INTO node_type_registry
  (type_key, type_name, category, handler_class,
   config_schema, output_schema, outlet_schema, summary_template, runtime_policy_schema,
   risk_level, is_trigger, is_terminal, description, enabled)
VALUES (
  'GOAL_CHECK',
  '目标检测',
  '流程控制',
  'org.chovy.canvas.engine.handlers.GoalCheckHandler',
  '[
    {
      "key": "eventCode",
      "label": "目标事件",
      "type": "select",
      "dataSource": "/meta/event-definitions",
      "required": true
    },
    {
      "key": "mode",
      "label": "检测模式",
      "type": "select",
      "required": true,
      "defaultValue": "SYNC",
      "options": [
        {"label": "立即检查", "value": "SYNC"},
        {"label": "等待达成", "value": "ASYNC"}
      ]
    },
    {
      "key": "maxWait",
      "label": "最长等待",
      "type": "duration",
      "visible": "mode==ASYNC",
      "required": true
    }
  ]',
  '[
    {"name": "goalMet", "type": "BOOLEAN", "label": "是否达成目标"}
  ]',
  '[
    {"id": "goal_met", "label": "已达成", "color": "#52c41a", "targetField": "goalMetNodeId"},
    {"id": "goal_not_met", "label": "未达成", "color": "#8c8c8c", "targetField": "goalNotMetNodeId"},
    {"id": "timeout", "label": "超时", "color": "#faad14", "targetField": "timeoutNodeId"}
  ]',
  '目标检测（{{eventCode}}）',
  '[]',
  'LOW',
  0,
  0,
  '检测用户是否达成指定目标事件；支持立即检查和等待达成，未达成、已达成、等待超时分别走独立出口。',
  1
);
