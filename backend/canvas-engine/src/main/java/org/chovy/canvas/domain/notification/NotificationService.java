package org.chovy.canvas.domain.notification;

public interface NotificationService {

    void createForTask(
            String operator,
            String type,
            String title,
            String content,
            String url,
            String taskId
    );
}
