ALTER TABLE notification
    ADD UNIQUE KEY uk_notification_task_user_type (task_id, user_id, type);
