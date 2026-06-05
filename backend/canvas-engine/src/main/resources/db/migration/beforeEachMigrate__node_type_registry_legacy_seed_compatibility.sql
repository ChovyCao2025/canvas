-- Keep fresh installs compatible with legacy node seed migrations that predate
-- the consolidated V2 node catalog.
SET @latest_flyway_version := (
  SELECT version
  FROM flyway_schema_history
  WHERE success = 1
  ORDER BY installed_rank DESC
  LIMIT 1
);

SET @node_type_registry_seed_cleanup := CASE
  WHEN @latest_flyway_version = '33' THEN
    'DELETE FROM node_type_registry WHERE type_key = ''AGGREGATE'''
  WHEN @latest_flyway_version = '34' THEN
    'DELETE FROM node_type_registry WHERE type_key = ''THRESHOLD'''
  WHEN @latest_flyway_version = '57' THEN
    'DELETE FROM node_type_registry WHERE type_key = ''WAIT'''
  ELSE
    'DO 0'
END;

PREPARE node_type_registry_seed_cleanup_stmt FROM @node_type_registry_seed_cleanup;
EXECUTE node_type_registry_seed_cleanup_stmt;
DEALLOCATE PREPARE node_type_registry_seed_cleanup_stmt;
