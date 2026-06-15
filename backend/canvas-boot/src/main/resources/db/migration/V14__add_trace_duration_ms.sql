-- V14: duration_ms 列已在数据库中存在，此 migration 为空操作
-- （列在早期手动 ALTER 中已添加，IF NOT EXISTS 语法和 DELIMITER 存储过程
--  均不兼容当前 Flyway+MySQL 组合，故以 SELECT 1 作占位保持版本号连续）
SELECT 1;
