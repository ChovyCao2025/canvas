package org.chovy.canvas.bi.domain;

import org.chovy.canvas.bi.api.BiResourceFavoriteCommand;
import org.chovy.canvas.bi.api.BiResourceFavoriteView;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class BiResourceFavoriteCatalog {

    private final Map<FavoriteKey, BiResourceFavoriteView> favorites = new ConcurrentHashMap<>();

    public BiResourceFavoriteView favorite(Long tenantId,
                                           String actor,
                                           BiResourceFavoriteCommand command,
                                           LocalDateTime now) {
        if (command == null) {
            throw new IllegalArgumentException("favorite command is required");
        }
        FavoriteKey key = key(tenantId, actor, command.resourceType(), command.resourceKey());
        return favorites.computeIfAbsent(key, ignored -> new BiResourceFavoriteView(
                key.tenantId(),
                key.actor(),
                key.resourceType(),
                key.resourceKey(),
                titleOrDefault(command.title(), key.resourceKey()),
                now,
                now));
    }

    public java.util.List<BiResourceFavoriteView> list(Long tenantId, String actor, String resourceType) {
        FavoriteScope scope = scope(tenantId, actor);
        String normalizedType = resourceType == null || resourceType.isBlank() ? null : normalizeType(resourceType);
        return favorites.values().stream()
                .filter(favorite -> scope.tenantId().equals(favorite.tenantId()))
                .filter(favorite -> scope.actor().equals(favorite.actor()))
                .filter(favorite -> normalizedType == null || normalizedType.equals(favorite.resourceType()))
                .sorted(Comparator.comparing(BiResourceFavoriteView::resourceType)
                        .thenComparing(BiResourceFavoriteView::resourceKey))
                .toList();
    }

    public void remove(Long tenantId, String actor, String resourceType, String resourceKey) {
        favorites.remove(key(tenantId, actor, resourceType, resourceKey));
    }

    private static FavoriteKey key(Long tenantId, String actor, String resourceType, String resourceKey) {
        FavoriteScope scope = scope(tenantId, actor);
        return new FavoriteKey(
                scope.tenantId(),
                scope.actor(),
                normalizeType(resourceType),
                BiResourceKey.of(resourceKey, "resourceKey").value());
    }

    private static FavoriteScope scope(Long tenantId, String actor) {
        return new FavoriteScope(tenantId == null || tenantId < 0 ? 0L : tenantId, defaultActor(actor));
    }

    private static String normalizeType(String resourceType) {
        if (resourceType == null || resourceType.isBlank()) {
            throw new IllegalArgumentException("resourceType is required");
        }
        String normalized = resourceType.trim()
                .toUpperCase(Locale.ROOT)
                .replaceAll("[^A-Z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
        if (normalized.isBlank()) {
            throw new IllegalArgumentException("resourceType is invalid");
        }
        return normalized;
    }

    private static String defaultActor(String actor) {
        return actor == null || actor.isBlank() ? "system" : actor.trim();
    }

    private static String titleOrDefault(String title, String resourceKey) {
        return title == null || title.isBlank() ? resourceKey : title.trim();
    }

    private record FavoriteScope(Long tenantId, String actor) {
    }

    private record FavoriteKey(Long tenantId, String actor, String resourceType, String resourceKey) {
    }
}
