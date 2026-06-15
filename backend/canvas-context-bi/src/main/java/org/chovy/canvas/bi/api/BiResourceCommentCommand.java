package org.chovy.canvas.bi.api;

public record BiResourceCommentCommand(
        String resourceType,
        String resourceKey,
        String widgetKey,
        String commentText) {
}
