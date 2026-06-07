UPDATE marketing_content_release newer
JOIN marketing_content_release older
  ON older.tenant_id = newer.tenant_id
 AND older.release_key = newer.release_key
 AND older.status = 'ACTIVE'
 AND newer.status = 'ACTIVE'
 AND (
   newer.source_version > older.source_version
   OR (newer.source_version = older.source_version AND newer.id > older.id)
 )
SET older.status = 'SUPERSEDED';

ALTER TABLE marketing_content_release
  ADD COLUMN active_release_key VARCHAR(128)
    GENERATED ALWAYS AS (CASE WHEN status = 'ACTIVE' THEN release_key ELSE NULL END) STORED,
  ADD UNIQUE KEY uk_marketing_content_release_active (tenant_id, active_release_key);
