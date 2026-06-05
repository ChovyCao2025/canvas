package org.chovy.canvas.domain.bi.resource;

public record BiResourceCommentCommand(
        String resourceType,
        String resourceKey,
        String widgetKey,
        String commentText) {
}
