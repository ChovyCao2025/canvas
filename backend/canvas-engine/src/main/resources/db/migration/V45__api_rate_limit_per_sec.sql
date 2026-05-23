-- V45: API_CALL 下游接口每秒限流配置
ALTER TABLE api_definition
    ADD COLUMN rate_limit_per_sec INT DEFAULT NULL
        COMMENT '每秒最大调用次数，NULL 表示不限制';
