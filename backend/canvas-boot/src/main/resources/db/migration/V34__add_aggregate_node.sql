-- V34: 新增 AGGREGATE 聚合评估节点
--
-- 与 HUB 的区别：
--   HUB      — 等待所有上游完成，不关心结果，直接路由到下游（纯等待）
--   AGGREGATE — 等待所有上游完成，基于上游执行结果评估条件，路由到成功或失败分支
--
-- 评估方式（evaluateMode）：
--   count  — 成功数 ≥ N
--   rate   — 成功率 ≥ N%
--   script — 自定义 Groovy 布尔表达式（可用变量: successCount / failCount / totalCount / successRate / outputs）

INSERT INTO node_type_registry
  (type_key, type_name, category, handler_class,
   config_schema, output_schema, is_trigger, is_terminal, description, enabled)
VALUES (
  'AGGREGATE',
  '聚合评估',
  '逻辑分支',
  'org.chovy.canvas.engine.handlers.AggregateHandler',
  '[
    {
      "key": "evaluateMode",
      "label": "评估方式",
      "type": "select",
      "required": true,
      "defaultValue": "count",
      "options": [
        {"label": "成功数 ≥ N", "value": "count"},
        {"label": "成功率 ≥ N%", "value": "rate"},
        {"label": "自定义脚本",  "value": "script"}
      ]
    },
    {
      "key": "minCount",
      "label": "最少成功数 N",
      "type": "number",
      "required": true,
      "defaultValue": 1,
      "visible": "evaluateMode==count"
    },
    {
      "key": "minRate",
      "label": "最低成功率 (0 ~ 100)",
      "type": "number",
      "required": true,
      "defaultValue": 50,
      "visible": "evaluateMode==rate"
    },
    {
      "key": "evaluateScript",
      "label": "评估脚本",
      "type": "multi-text",
      "required": true,
      "visible": "evaluateMode==script",
      "hint": "返回 true/false 的 Groovy 表达式。可用变量: successCount(成功数) / failCount(失败数) / totalCount(总数) / successRate(成功率 0~100) / outputs(Map<nodeId, Map>上游输出)"
    },
    {
      "key": "_routeHint",
      "label": "路由分支",
      "type": "edge-hint",
      "icon": "branch",
      "hint": "从节点底部拖出连线：success handle → 条件满足时的下游，fail handle → 条件不满足时的下游"
    },
    {
      "key": "timeout",
      "label": "等待超时 (秒)",
      "type": "number",
      "defaultValue": 600
    }
  ]',
  '[
    {"name": "successCount", "type": "NUMBER", "label": "成功上游数"},
    {"name": "failCount",    "type": "NUMBER", "label": "失败上游数"},
    {"name": "totalCount",   "type": "NUMBER", "label": "上游总数"},
    {"name": "successRate",  "type": "NUMBER", "label": "成功率(0~100)"},
    {"name": "passed",       "type": "BOOLEAN","label": "是否通过评估"}
  ]',
  0, 0,
  '等待所有并行上游分支完成，基于上游结果（成功数/成功率/自定义脚本）评估条件，路由到对应分支。适用于多渠道投票、K-of-N 决策等场景。',
  1
);
