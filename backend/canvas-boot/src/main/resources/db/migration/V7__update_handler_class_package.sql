-- V7 更新 handler_class 中的包名（com.photon.canvas → org.chovy.canvas）
-- 适用于已经运行过 V2 迁移的现有数据库
UPDATE node_type_registry
SET handler_class = REPLACE(handler_class, 'com.photon.canvas', 'org.chovy.canvas')
WHERE handler_class LIKE 'com.photon.canvas%';
