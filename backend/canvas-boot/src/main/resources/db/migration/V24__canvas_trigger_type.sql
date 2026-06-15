-- V24: Add trigger_type and cron_expression to canvas table.
-- trigger_type: REALTIME (default) | SCHEDULED
ALTER TABLE canvas
  ADD COLUMN trigger_type    VARCHAR(20)  NOT NULL DEFAULT 'REALTIME'  COMMENT 'REALTIME | SCHEDULED',
  ADD COLUMN cron_expression VARCHAR(100) NULL                          COMMENT 'Cron表达式，trigger_type=SCHEDULED时必填';
