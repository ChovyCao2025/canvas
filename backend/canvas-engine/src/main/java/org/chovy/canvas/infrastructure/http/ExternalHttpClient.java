package org.chovy.canvas.infrastructure.http;

import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * Local boundary for outbound HTTP JSON calls to named external integrations.
 */
@FunctionalInterface
public interface ExternalHttpClient {

    String REACH_PLATFORM = "reach-platform";

    Mono<Map<String, Object>> postJson(String integrationName, String path, Map<String, Object> payload);
}
