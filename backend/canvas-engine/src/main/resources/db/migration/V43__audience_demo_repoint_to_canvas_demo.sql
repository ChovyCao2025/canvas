-- V43: repoint audience demo JDBC definitions to isolated canvas_demo database

UPDATE audience_definition
SET data_source_config = '{"url":"jdbc:mysql://127.0.0.1:3306/canvas_demo?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai","username":"root","password":"root","baseTable":"audience_demo_user","userIdColumn":"user_id","driverClassName":"com.mysql.cj.jdbc.Driver","maxRows":10000}'
WHERE id IN (90001, 90002, 90003);
