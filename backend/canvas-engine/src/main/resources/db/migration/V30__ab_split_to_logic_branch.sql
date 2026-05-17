-- V30: Move AB_SPLIT from 人群圈选 to 逻辑分支 category.
-- 人群圈选 category is now empty and will no longer appear in the node panel.
UPDATE node_type_registry
SET category = '逻辑分支'
WHERE type_key = 'AB_SPLIT';
