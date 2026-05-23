ALTER TABLE `canvas_execution_request`
  ADD KEY `idx_execution_request_status_updated` (`status`, `updated_at`),
  ADD KEY `idx_execution_request_user_status_updated` (`user_id`, `status`, `updated_at`),
  ADD KEY `idx_execution_request_source_status_updated` (`source_msg_id`, `status`, `updated_at`);
