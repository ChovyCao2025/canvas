package org.chovy.canvas.domain.bi.resource;

public record BiResourceFavoriteCommand(
        String resourceType,
        String resourceKey,
        Boolean favorite) {

    public BiResourceFavoriteCommand(String resourceType, String resourceKey) {
        this(resourceType, resourceKey, true);
    }
}
