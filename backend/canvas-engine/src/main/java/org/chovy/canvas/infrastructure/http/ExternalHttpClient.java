package org.chovy.canvas.infrastructure.http;

import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * Local boundary for outbound HTTP JSON calls to named external integrations.
 */
@FunctionalInterface
public interface ExternalHttpClient {

    String REACH_PLATFORM = "reach-platform";

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param integrationName 名称文本，用于展示或唯一性校验。
     * @param path path 参数，用于 postJson 流程中的校验、计算或对象转换。
     * @param payload 待处理业务值，用于规则计算、转换或外部调用。
     * @return 返回 postJson 流程生成的业务结果。
     */
    Mono<Map<String, Object>> postJson(String integrationName, String path, Map<String, Object> payload);
}
