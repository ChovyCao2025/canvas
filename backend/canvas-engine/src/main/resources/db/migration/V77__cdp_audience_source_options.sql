-- V77: CDP audience source options.

INSERT INTO system_option
  (category, option_key, label, description, sort_order, enabled, system_builtin)
VALUES
  ('audience_data_source_type', 'CDP_TAG', 'CDP 标签', '基于 CDP 当前用户标签圈选人群', 30, 1, 1),
  ('audience_data_source_type', 'CDP_PROFILE', 'CDP 用户属性', '基于 CDP 用户档案属性圈选人群', 40, 1, 1),
  ('audience_data_source_type', 'CDP_IDENTITY', 'CDP 身份', '基于 CDP 用户身份映射圈选人群', 50, 1, 1)
ON DUPLICATE KEY UPDATE
  label = VALUES(label),
  description = VALUES(description),
  sort_order = VALUES(sort_order),
  enabled = VALUES(enabled),
  system_builtin = VALUES(system_builtin);
