package org.chovy.canvas.infra.cache;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.RequiredArgsConstructor;
import org.chovy.canvas.domain.meta.ApiDefinition;
import org.chovy.canvas.domain.meta.ApiDefinitionMapper;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ApiDefinitionCache {

    private final ApiDefinitionMapper mapper;
    private final Cache<String, Optional<ApiDefinition>> cache = Caffeine.newBuilder()
            .maximumSize(1_000)
            .expireAfterWrite(Duration.ofMinutes(5))
            .build();

    public ApiDefinition getEnabled(String apiKey) {
        if (apiKey == null || apiKey.isBlank()) return null;
        return cache.get(apiKey, this::load).orElse(null);
    }

    public void invalidate(String apiKey) {
        if (apiKey != null && !apiKey.isBlank()) {
            cache.invalidate(apiKey);
        }
    }

    public void invalidateAll() {
        cache.invalidateAll();
    }

    private Optional<ApiDefinition> load(String apiKey) {
        return Optional.ofNullable(mapper.selectOne(
                new LambdaQueryWrapper<ApiDefinition>()
                        .eq(ApiDefinition::getApiKey, apiKey)
                        .eq(ApiDefinition::getEnabled, 1)
        ));
    }
}
