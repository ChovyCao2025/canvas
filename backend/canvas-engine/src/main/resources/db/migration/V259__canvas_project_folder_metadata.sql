ALTER TABLE canvas
    ADD COLUMN project_key VARCHAR(128) NULL COMMENT 'Flat project grouping key',
    ADD COLUMN project_name VARCHAR(255) NULL COMMENT 'Flat project display name',
    ADD COLUMN folder_key VARCHAR(128) NULL COMMENT 'Flat folder grouping key',
    ADD COLUMN folder_name VARCHAR(255) NULL COMMENT 'Flat folder display name',
    ADD KEY idx_canvas_project_folder (project_key, folder_key, status, updated_at);
