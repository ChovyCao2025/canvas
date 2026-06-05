package org.chovy.canvas.domain.canvas;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.chovy.canvas.dal.dataobject.ConnectedContentCacheDO;
import org.chovy.canvas.dal.mapper.ConnectedContentCacheMapper;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ConnectedContentGatewayService implements ConnectedContentGateway {

    private static final String STATUS_SUCCESS = "SUCCESS";

    private final ConnectedContentCacheMapper cacheMapper;
    private final WebClient.Builder webClientBuilder;

    @Override
    public Optional<CachedContent> findFresh(Long tenantId, String cacheKey, LocalDateTime now) {
        ConnectedContentCacheDO row = cacheMapper.selectOne(new LambdaQueryWrapper<ConnectedContentCacheDO>()
                .eq(ConnectedContentCacheDO::getTenantId, tenantId)
                .eq(ConnectedContentCacheDO::getCacheKey, cacheKey)
                .eq(ConnectedContentCacheDO::getStatus, STATUS_SUCCESS)
                .gt(ConnectedContentCacheDO::getExpiresAt, now)
                .last("LIMIT 1"));
        return row == null ? Optional.empty() : Optional.of(new CachedContent(row.getResponseJson()));
    }

    @Override
    public void save(Long tenantId,
                     String cacheKey,
                     String urlHash,
                     String requestHash,
                     String body,
                     LocalDateTime expiresAt) {
        ConnectedContentCacheDO row = cacheMapper.selectOne(new LambdaQueryWrapper<ConnectedContentCacheDO>()
                .eq(ConnectedContentCacheDO::getTenantId, tenantId)
                .eq(ConnectedContentCacheDO::getCacheKey, cacheKey)
                .last("LIMIT 1"));
        if (row == null) {
            row = new ConnectedContentCacheDO();
            row.setTenantId(tenantId);
            row.setCacheKey(cacheKey);
            row.setUrlHash(urlHash);
            row.setRequestHash(requestHash);
            row.setResponseJson(body);
            row.setStatus(STATUS_SUCCESS);
            row.setExpiresAt(expiresAt);
            cacheMapper.insert(row);
            return;
        }
        row.setUrlHash(urlHash);
        row.setRequestHash(requestHash);
        row.setResponseJson(body);
        row.setStatus(STATUS_SUCCESS);
        row.setExpiresAt(expiresAt);
        cacheMapper.updateById(row);
    }

    @Override
    public Mono<String> fetch(HttpRequest request) {
        WebClient client = webClientBuilder.clone()
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(request.maxBytes()))
                .build();
        WebClient.RequestHeadersSpec<?> spec;
        if ("GET".equals(request.method())) {
            spec = client.get().uri(request.url());
        } else {
            spec = client.post()
                    .uri(request.url())
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request.body() == null || request.body().isBlank()
                            ? "{}"
                            : request.body());
        }
        request.headers().forEach((name, value) -> spec.header(name, value));
        return spec.retrieve()
                .bodyToMono(String.class)
                .map(body -> body == null ? "" : body);
    }
}
