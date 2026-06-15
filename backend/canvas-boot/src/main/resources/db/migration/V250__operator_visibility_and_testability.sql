ALTER TABLE `message_send_record`
  ADD INDEX `idx_message_send_canvas_status_created` (`canvas_id`, `status`, `created_at`),
  ADD INDEX `idx_message_send_canvas_user_created` (`canvas_id`, `user_id`, `created_at`),
  ADD INDEX `idx_message_send_execution_created` (`execution_id`, `created_at`);
