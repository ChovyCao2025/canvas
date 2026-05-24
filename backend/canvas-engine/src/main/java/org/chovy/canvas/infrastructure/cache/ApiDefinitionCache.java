package org.chovy.canvas.infrastructure.cache;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.RequiredArgsConstructor;
import org.chovy.canvas.dal.dataobject.ApiDefinitionDO;
import org.chovy.canvas.dal.mapper.ApiDefinitionMapper;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ApiDefinitionCache {

    private final ApiDefinitionMapper mapper;
    private final Cache<String, Optional<ApiDefinitionDO>> cache = Caffeine.newBuilder()
            .maximumSize(1_000)
            .expireAfterWrite(Duration.ofMinutes(5))
            .build();

    public ApiDefinitionDO getEnabled(String apiKey) {
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

    private Optional<ApiDefinitionDO> load(String apiKey) {
        return Optional.ofNullable(mapper.selectOne(
                new LambdaQueryWrapper<ApiDefinitionDO>()
                        .eq(ApiDefinitionDO::getApiKey, apiKey)
                        .eq(ApiDefinitionDO::getEnabled, 1)
        ));
    }
}
