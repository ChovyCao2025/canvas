package org.chovy.canvas.domain.canvas;

import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

/**
 * ConnectedContentGateway 定义 domain.canvas 场景中的扩展契约。
 */
public interface ConnectedContentGateway {

    /**
     * 查询或读取业务数据。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param cacheKey 业务键，用于在同一租户下定位资源。
     * @param now 时间参数，用于计算窗口、过期或审计时间。
     * @return 返回符合条件的数据列表或视图。
     */
    Optional<CachedContent> findFresh(Long tenantId, String cacheKey, LocalDateTime now);

    /**
     * 执行数据写入或状态变更。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param cacheKey 业务键，用于在同一租户下定位资源。
     * @param urlHash url hash 参数，用于 save 流程中的校验、计算或对象转换。
     * @param requestHash request hash 参数，用于 save 流程中的校验、计算或对象转换。
     * @param body 待处理业务值，用于规则计算、转换或外部调用。
     * @param expiresAt 时间参数，用于计算窗口、过期或审计时间。
     */
    void save(Long tenantId, String cacheKey, String urlHash, String requestHash, String body, LocalDateTime expiresAt);

    /**
     * 执行 fetch 流程，围绕 fetch 完成校验、计算或结果组装。
     *
     * @param request 请求对象，承载本次操作的输入参数。
     * @return 返回 fetch 生成的文本或业务键。
     */
    Mono<String> fetch(HttpRequest request);

    /**
     * CachedContent 数据记录。
     */
    record CachedContent(String body) {
    }

    /**
     * HttpRequest 数据记录。
     */
    record HttpRequest(String url,
                       String method,
                       Map<String, String> headers,
                       String body,
                       int timeoutMs,
                       int maxBytes) {
    }
}
