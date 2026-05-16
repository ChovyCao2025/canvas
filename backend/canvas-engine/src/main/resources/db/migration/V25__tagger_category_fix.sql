-- V25: Move TAGGER_OFFLINE from 人群圈选 to 行为策略 category.
-- TAGGER_REALTIME was already in 行为策略; this aligns TAGGER_OFFLINE with it.
-- Removes the 人群圈选 category from the node panel entirely.
UPDATE node_type_registry
SET category = '行为策略'
WHERE type_key = 'TAGGER_OFFLINE';
