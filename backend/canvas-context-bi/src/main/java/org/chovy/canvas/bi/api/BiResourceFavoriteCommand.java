package org.chovy.canvas.bi.api;

public record BiResourceFavoriteCommand(
        String resourceType,
        String resourceKey,
        String title) {
}
