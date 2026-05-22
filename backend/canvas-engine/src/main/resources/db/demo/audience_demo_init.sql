CREATE DATABASE IF NOT EXISTS canvas_demo DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE canvas_demo;

DROP TABLE IF EXISTS audience_demo_user;
CREATE TABLE audience_demo_user (
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
