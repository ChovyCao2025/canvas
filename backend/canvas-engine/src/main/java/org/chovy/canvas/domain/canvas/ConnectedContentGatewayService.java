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

/**
 * ConnectedContentGatewayService 编排 domain.canvas 场景的领域业务规则。
 */
@Service
@RequiredArgsConstructor
public class ConnectedContentGatewayService implements ConnectedContentGateway {

    private static final String STATUS_SUCCESS = "SUCCESS";

    private final ConnectedContentCacheMapper cacheMapper;
    private final WebClient.Builder webClientBuilder;

    @Override
    /**
     * 查找租户内仍未过期的 Connected Content 缓存。
     * 只返回成功状态且 expiresAt 晚于当前时间的缓存体，未命中时返回空。
     */
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
    /**
     * 保存或覆盖租户内 Connected Content 响应缓存。
     * 同一 cacheKey 会被更新为最新 URL/request hash、响应体和过期时间，状态固定为成功。
     */
    public void save(Long tenantId,
                     String cacheKey,
                     String urlHash,
                     String requestHash,
                     String body,
                     LocalDateTime expiresAt) {
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        ConnectedContentCacheDO row = cacheMapper.selectOne(new LambdaQueryWrapper<ConnectedContentCacheDO>()
                .eq(ConnectedContentCacheDO::getTenantId, tenantId)
                .eq(ConnectedContentCacheDO::getCacheKey, cacheKey)
                .last("LIMIT 1"));
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
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
            // 汇总前面计算出的状态和明细，返回给调用方。
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
    /**
     * 按请求配置通过 WebClient 拉取外部 Connected Content。
     * 支持 GET 和 JSON POST，会透传请求头并限制响应体大小；返回 Mono 中的字符串为空时归一为空串。
     */
    public Mono<String> fetch(HttpRequest request) {
        WebClient client = webClientBuilder.clone()
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(request.maxBytes()))
                .build();
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        WebClient.RequestHeadersSpec<?> spec;
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
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
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        request.headers().forEach((name, value) -> spec.header(name, value));
        return spec.retrieve()
                .bodyToMono(String.class)
                .map(body -> body == null ? "" : body);
    }
}
