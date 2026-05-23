-- V44: 为 api_definition 增加速率限制配置
ALTER TABLE api_definition
    ADD COLUMN rate_limit_per_sec INT DEFAULT NULL
        COMMENT '每秒最大调用次数，NULL 表示不限制';
