-- V41: runnable audience demo data for three calculation modes
-- 1) OFFLINE_BATCH + AVIATOR + JDBC
-- 2) OFFLINE_BATCH + QL + JDBC
-- 3) HYBRID + AVIATOR + JDBC
--
-- This migration creates a demo user table plus three audience definitions so the
-- audience list/editor/TAGGER audience mode can be exercised immediately after Flyway runs.

CREATE TABLE IF NOT EXISTS audience_demo_user (
    user_id VARCHAR(64) PRIMARY KEY,
    city VARCHAR(64),
    vip_level INT,
    last_purchase_days INT,
    has_coupon TINYINT,
    order_count INT,
    total_amount DECIMAL(18,2),
    channel VARCHAR(32),
    active_days_30 INT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

DELETE FROM audience_demo_user;
INSERT INTO audience_demo_user
(user_id, city, vip_level, last_purchase_days, has_coupon, order_count, total_amount, channel, active_days_30, created_at)
VALUES
('u1001', 'Beijing', 3, 7, 1, 8, 2688.00, 'APP', 18, NOW()),
('u1002', 'Shanghai', 2, 15, 0, 6, 1888.00, 'APP', 14, NOW()),
('u1003', 'Beijing', 1, 45, 1, 2, 288.00, 'MINI_APP', 6, NOW()),
('u1004', 'Guangzhou', 4, 3, 1, 12, 5888.00, 'APP', 25, NOW()),
('u1005', 'Shenzhen', 0, 90, 0, 1, 99.00, 'WEB', 2, NOW()),
('u1006', 'Shanghai', 3, 21, 0, 10, 3299.00, 'APP', 19, NOW()),
('u1007', 'Beijing', 2, 28, 1, 5, 999.00, 'APP', 11, NOW()),
('u1008', 'Hangzhou', 1, 12, 0, 4, 699.00, 'MINI_APP', 8, NOW()),
('u1009', 'Shanghai', 5, 2, 1, 20, 9999.00, 'APP', 29, NOW()),
('u1010', 'Beijing', 2, 32, 0, 7, 1499.00, 'WEB', 10, NOW()),
('u1011', 'Chengdu', 1, 18, 1, 3, 399.00, 'APP', 7, NOW()),
('u1012', 'Shanghai', 2, 9, 1, 9, 2599.00, 'APP', 16, NOW());

DELETE FROM audience_stat WHERE audience_id IN (90001, 90002, 90003);
DELETE FROM audience_definition WHERE id IN (90001, 90002, 90003);

INSERT INTO audience_definition
(id, name, description, rule_json, engine_type, data_source_type, data_source_config, evaluation_strategy, cron_expression, enabled, created_by, created_at, updated_at)
VALUES
(
  90001,
  '演示：高价值近30天活跃用户',
  '体验 OFFLINE_BATCH + AVIATOR + JDBC。筛选近30天活跃、北上、VIP 且有券或高订单数用户。',
  '{"logic":"AND","conditions":[{"field":"last_purchase_days","op":"<=","value":30},{"field":"city","op":"IN","value":["Beijing","Shanghai"]},{"field":"vip_level","op":">=","value":2}],"groups":[{"logic":"OR","conditions":[{"field":"has_coupon","op":"=","value":1},{"field":"order_count","op":">","value":5}]}]}',
  'AVIATOR',
  'JDBC',
  '{"url":"jdbc:mysql://127.0.0.1:3306/canvas?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai","username":"root","password":"root","baseTable":"audience_demo_user","userIdColumn":"user_id","driverClassName":"com.mysql.cj.jdbc.Driver","maxRows":10000}',
  'OFFLINE_BATCH',
  '0 2 * * *',
  1,
  'system',
  NOW(),
  NOW()
),
(
  90002,
  '演示：高频消费城市用户',
  '体验 OFFLINE_BATCH + QL + JDBC。筛选上海/北京且订单数>=7或总消费>2500的用户。',
  '{"logic":"AND","conditions":[{"field":"city","op":"IN","value":["Beijing","Shanghai"]}],"groups":[{"logic":"OR","conditions":[{"field":"order_count","op":">=","value":7},{"field":"total_amount","op":">","value":2500}]}]}',
  'QL',
  'JDBC',
  '{"url":"jdbc:mysql://127.0.0.1:3306/canvas?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai","username":"root","password":"root","baseTable":"audience_demo_user","userIdColumn":"user_id","driverClassName":"com.mysql.cj.jdbc.Driver","maxRows":10000}',
  'OFFLINE_BATCH',
  '0 3 * * *',
  1,
  'system',
  NOW(),
  NOW()
),
(
  90003,
  '演示：多渠道高活跃用户',
  '体验 HYBRID + AVIATOR + JDBC。筛选近15天购买且30天活跃>=12的 APP/MINI_APP 用户。',
  '{"logic":"AND","conditions":[{"field":"last_purchase_days","op":"<=","value":15},{"field":"active_days_30","op":">=","value":12},{"field":"channel","op":"IN","value":["APP","MINI_APP"]}],"groups":[]}',
  'AVIATOR',
  'JDBC',
  '{"url":"jdbc:mysql://127.0.0.1:3306/canvas?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai","username":"root","password":"root","baseTable":"audience_demo_user","userIdColumn":"user_id","driverClassName":"com.mysql.cj.jdbc.Driver","maxRows":10000}',
  'HYBRID',
  '0 4 * * *',
  1,
  'system',
  NOW(),
  NOW()
);

INSERT INTO audience_stat
(audience_id, estimated_size, bitmap_size_kb, computed_at, status, error_msg)
VALUES
(90001, 0, 0, NOW(), 'PENDING', NULL),
(90002, 0, 0, NOW(), 'PENDING', NULL),
(90003, 0, 0, NOW(), 'PENDING', NULL);
