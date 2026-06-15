package org.chovy.canvas.bi.api;

public record BiEmbedQueryCommand(
        String ticket,
        String resourceType,
        String resourceKey,
        String widgetKey,
        BiQueryCommand query) {
}
