UPDATE node_type_registry
SET config_schema = '[
  {"key":"waitType","label":"等待类型","type":"select","required":true,"defaultValue":"DURATION","options":[{"label":"固定时长","value":"DURATION"},{"label":"直到指定时间","value":"UNTIL_DATE"},{"label":"直到相对时间","value":"RELATIVE_TIME"},{"label":"时间窗口","value":"TIME_WINDOW"},{"label":"直到事件发生","value":"UNTIL_EVENT"}]},
  {"key":"duration","label":"等待时长","type":"duration","visible":"waitType==DURATION","required":true},
  {"key":"untilDate","label":"等待到","type":"datetime","visible":"waitType==UNTIL_DATE","required":true},
  {"key":"time","label":"相对时间","type":"time","visible":"waitType==RELATIVE_TIME","required":true},
  {"key":"timeWindow","label":"允许窗口","type":"time-window","visible":"waitType==TIME_WINDOW","required":true},
  {"key":"eventCode","label":"目标事件","type":"select","dataSource":"/meta/event-definitions","visible":"waitType==UNTIL_EVENT","required":true},
  {"key":"eventFilters","label":"事件过滤","type":"condition-builder","visible":"waitType==UNTIL_EVENT"},
  {"key":"maxWait","label":"最长等待","type":"duration","visible":"waitType==UNTIL_EVENT","required":true}
]'
WHERE type_key = 'WAIT';
