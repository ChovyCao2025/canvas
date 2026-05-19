-- V35: 新增 THRESHOLD 阈值触发节点
--
-- 与 AGGREGATE / HUB 的区别：
--   HUB / AGGREGATE — 等所有上游完成后评估（ctx 已完整，repeat 无实际语义价值）
--   THRESHOLD       — 不等，每个上游完成都触发一次评估，达到阈值立刻路由
--                     这是 repeat 机制真正有语义价值的节点类型：
--                     handler 持锁期间到来的上游信号通过 repeatPending 被保存，
--                     repeat 重新评估时可能因新上游的到来而改变路由决策

INSERT INTO node_type_registry
  (type_key, type_name, category, handler_class,
   config_schema, output_schema, is_trigger, is_terminal, description, enabled)
VALUES (
  'THRESHOLD',
  '阈值触发',
  '逻辑分支',
  'org.chovy.canvas.engine.handlers.ThresholdHandler',
  '[
    {
      "key": "thresholdMode",
      "label": "触发条件",
      "type": "select",
      "required": true,
      "defaultValue": "min_success",
      "options": [
        {"label": "成功数 ≥ N（K-of-N 投票）",      "value": "min_success"},
        {"label": "完成数 ≥ N（SUCCESS+FAILED 均计）","value": "min_done"},
        {"label": "任意上游失败立刻触发",              "value": "any_fail"}
      ]
    },
    {
      "key": "threshold",
      "label": "阈值 N",
      "type": "number",
      "required": true,
      "defaultValue": 1,
      "visible": "thresholdMode==min_success||thresholdMode==min_done",
      "hint": "any_fail 模式无需配置此项"
    },
    {
      "key": "_routeHint",
      "label": "路由分支",
      "type": "edge-hint",
      "hint": "success → 达到阈值时路由；fail → 全部完成但未达阈值时路由"
    },
    {
      "key": "timeout",
      "label": "等待超时 (秒)",
      "type": "number",
      "defaultValue": 600,
      "hint": "超过此时间仍未达阈值则标记失败"
    }
  ]',
  '[
    {"name": "successCount", "type": "NUMBER",  "label": "成功上游数"},
    {"name": "doneCount",    "type": "NUMBER",  "label": "已完成上游数"},
    {"name": "totalCount",   "type": "NUMBER",  "label": "上游总数"}
  ]',
  0, 0,
  '每个上游完成都触发一次评估，达到阈值（如成功数≥K）立刻路由，不等待其余上游。适用于 K-of-N 投票、任意失败快速响应等场景。',
  1
);
