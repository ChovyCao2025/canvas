package org.chovy.canvas.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class RedisRoleConfiguration {

    private final boolean readinessEnabled;
    private final String executionStateUrl;
    private final String routeCacheUrl;
    private final String bitmapUrl;
    private final String rateLimitUrl;

    public RedisRoleConfiguration(
            @Value("${canvas.redis.roles.readiness-enabled:false}") boolean readinessEnabled,
            @Value("${canvas.redis.roles.execution-state-url:redis://localhost:6379/0}") String executionStateUrl,
            @Value("${canvas.redis.roles.route-cache-url:redis://localhost:6379/0}") String routeCacheUrl,
            @Value("${canvas.redis.roles.bitmap-url:redis://localhost:6379/0}") String bitmapUrl,
            @Value("${canvas.redis.roles.rate-limit-url:redis://localhost:6379/0}") String rateLimitUrl) {
        this.readinessEnabled = readinessEnabled;
        this.executionStateUrl = executionStateUrl;
        this.routeCacheUrl = routeCacheUrl;
        this.bitmapUrl = bitmapUrl;
        this.rateLimitUrl = rateLimitUrl;
    }

    @PostConstruct
    void validate() {
        if (!readinessEnabled) {
            return;
        }
        List<String> roles = List.of(
                normalize(executionStateUrl),
                normalize(routeCacheUrl),
                normalize(bitmapUrl),
                normalize(rateLimitUrl));
        Set<String> distinct = new HashSet<>(roles);
        if (distinct.size() != roles.size()) {
            throw new IllegalStateException(
                    "4000 readiness requires separated Redis role connections for execution-state, route-cache, bitmap, and rate-limit traffic");
        }
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? "" : value.trim();
    }
}
