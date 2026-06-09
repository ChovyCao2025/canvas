package org.chovy.canvas.engine.llm;

import com.fasterxml.jackson.databind.JsonNode;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * LlmClient 定义 engine.llm 场景中的扩展契约。
 */
public interface LlmClient {

    /**
     * 判断当前客户端是否支持指定供应商类型。
     *
     * @param providerType 供应商类型
     * @return true 表示可以处理该供应商请求
     */
    boolean supports(String providerType);

    /**
     * 调用大模型并返回结构化响应。
     *
     * @param request 大模型调用请求
     * @return 包含原始内容、结构化输出和 token 用量的响应
     */
    Mono<LlmResponse> complete(LlmRequest request);

    /**
     * 大模型调用请求。
     *
     * @param endpoint 供应商接口地址
     * @param modelKey 模型标识
     * @param prompt 用户提示词
     * @param outputSchema 期望输出 JSON schema
     * @param defaultValues 输出默认值
     * @param params 供应商扩展参数
     * @param timeoutMs 请求超时时间，单位毫秒
     */
    record LlmRequest(
            String endpoint,
            String modelKey,
            String prompt,
            JsonNode outputSchema,
            JsonNode defaultValues,
            Map<String, Object> params,
            int timeoutMs,
            String apiKey) {
    }

    /**
     * 大模型调用响应。
     *
     * @param rawContent 供应商返回的原始文本内容
     * @param output 解析后的 JSON 输出
     * @param promptTokens prompt token 用量
     * @param completionTokens completion token 用量
     */
    record LlmResponse(
            String rawContent,
            JsonNode output,
            Integer promptTokens,
            Integer completionTokens) {
    }
}
