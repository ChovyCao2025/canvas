ALTER TABLE data_source_config
  ADD COLUMN connector_type VARCHAR(64) NULL AFTER type,
  ADD COLUMN connection_mode VARCHAR(32) NULL AFTER connector_type;

UPDATE data_source_config
SET connector_type = CASE
    WHEN LOWER(CONCAT_WS(' ', driver_class_name, url, name, description)) LIKE '%doris%' THEN 'DORIS'
    WHEN LOWER(CONCAT_WS(' ', driver_class_name, url, name, description)) LIKE '%postgresql%'
      OR LOWER(CONCAT_WS(' ', driver_class_name, url, name, description)) LIKE '%:postgres:%' THEN 'POSTGRESQL'
    WHEN LOWER(CONCAT_WS(' ', driver_class_name, url, name, description)) LIKE '%clickhouse%' THEN 'CLICKHOUSE'
    WHEN LOWER(CONCAT_WS(' ', driver_class_name, url, name, description)) LIKE '%oracle%' THEN 'ORACLE'
    WHEN LOWER(CONCAT_WS(' ', driver_class_name, url, name, description)) LIKE '%sqlserver%'
      OR LOWER(CONCAT_WS(' ', driver_class_name, url, name, description)) LIKE '%microsoft%' THEN 'SQLSERVER'
    WHEN LOWER(CONCAT_WS(' ', driver_class_name, url, name, description)) LIKE '%mysql%' THEN 'MYSQL'
    ELSE 'JDBC'
  END,
  connection_mode = 'DIRECT_QUERY'
WHERE connector_type IS NULL
   OR connector_type = ''
   OR connection_mode IS NULL
   OR connection_mode = '';
