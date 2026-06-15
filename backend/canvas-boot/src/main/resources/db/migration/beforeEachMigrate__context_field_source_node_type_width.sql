-- Keep fresh MySQL installs compatible with early seed data before V2 runs.
SET @context_field_source_node_type_width := (
  SELECT CHARACTER_MAXIMUM_LENGTH
  FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'context_field'
    AND COLUMN_NAME = 'source_node_type'
);

SET @context_field_source_node_type_ddl := IF(
  @context_field_source_node_type_width IS NOT NULL
    AND @context_field_source_node_type_width < 128,
  'ALTER TABLE context_field MODIFY COLUMN source_node_type VARCHAR(128) NULL COMMENT ''由哪类节点产出''',
  'DO 0'
);

PREPARE context_field_source_node_type_stmt FROM @context_field_source_node_type_ddl;
EXECUTE context_field_source_node_type_stmt;
DEALLOCATE PREPARE context_field_source_node_type_stmt;
