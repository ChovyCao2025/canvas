package org.chovy.canvas.domain.canvas;

import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

public interface ConnectedContentGateway {

    Optional<CachedContent> findFresh(Long tenantId, String cacheKey, LocalDateTime now);

    void save(Long tenantId, String cacheKey, String urlHash, String requestHash, String body, LocalDateTime expiresAt);

    Mono<String> fetch(HttpRequest request);

    record CachedContent(String body) {
    }

    record HttpRequest(String url,
                       String method,
                       Map<String, String> headers,
                       String body,
                       int timeoutMs,
                       int maxBytes) {
    }
}
