package org.chovy.canvas.engine.handlers;

import org.chovy.canvas.dal.dataobject.ApiDefinitionDO;
import org.chovy.canvas.engine.context.ExecutionContext;
import org.chovy.canvas.engine.handler.NodeResult;
import org.chovy.canvas.infrastructure.cache.ApiDefinitionCache;
import org.chovy.canvas.infrastructure.redis.RedisKeyUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Api Call Handler Rate Limit 测试类。
 *
 * <p>覆盖该后端组件在典型输入、边界条件和异常场景下的行为，确保重构或性能优化不会改变既有契约。
 * <p>测试代码只构造必要的依赖与数据，断言重点放在可观察结果、状态变更和关键副作用上。
 */
@ExtendWith(MockitoExtension.class)
class ApiCallHandlerRateLimitTest {

    @Mock StringRedisTemplate redis;
    @Mock ValueOperations<String, String> valueOps;
    @Mock ApiDefinitionCache apiDefinitionCache;
    @Mock ApiCallPayloadBuilder payloadBuilder;
    @Mock WebClient.Builder webClientBuilder;
    @Mock ExecutionContext ctx;

    @BeforeEach
    void setUp() {
        lenient().when(redis.opsForValue()).thenReturn(valueOps);
        lenient().when(payloadBuilder.build(anyMap(), any(), anyString(), anyBoolean()))
                .thenReturn(List.of(Map.of("params", Map.of())));
        clearInvocations(redis);
    }

    @Test
    @DisplayName("未超限时返回 false（允许通过）")
    void under_limit_returns_false() {
        when(valueOps.increment(anyString())).thenReturn(1L);
        when(redis.expire(anyString(), any())).thenReturn(true);

        boolean exceeded = ApiCallHandler.isRateLimitExceeded(redis, "test_api", 10, fixedInstant());

        assertThat(exceeded).isFalse();
    }

    @Test
    @DisplayName("超限时返回 true（拒绝）")
    void over_limit_returns_true() {
        when(valueOps.increment(anyString())).thenReturn(11L);

        boolean exceeded = ApiCallHandler.isRateLimitExceeded(redis, "test_api", 10, fixedInstant());

        assertThat(exceeded).isTrue();
    }

    @Test
    @DisplayName("计数等于限制时允许通过")
    void count_equal_to_limit_returns_false() {
        when(valueOps.increment(anyString())).thenReturn(10L);

        boolean exceeded = ApiCallHandler.isRateLimitExceeded(redis, "test_api", 10, fixedInstant());

        assertThat(exceeded).isFalse();
    }

    @Test
    @DisplayName("第一次请求时设置 TTL")
    void first_request_sets_expire() {
        when(valueOps.increment(anyString())).thenReturn(1L);
        when(redis.expire(anyString(), any())).thenReturn(true);

        ApiCallHandler.isRateLimitExceeded(redis, "test_api", 10, fixedInstant());

        String expectedKey = "canvas:ratelimit:test_api:1700000000";
        verify(valueOps).increment(expectedKey);
        verify(redis).expire(expectedKey, java.time.Duration.ofSeconds(2));
    }

    @Test
    @DisplayName("非第一次请求不重置 TTL")
    void subsequent_request_does_not_reset_expire() {
        when(valueOps.increment(anyString())).thenReturn(5L);

        ApiCallHandler.isRateLimitExceeded(redis, "test_api", 10, fixedInstant());

        verify(redis, never()).expire(anyString(), any());
    }

    @Test
    @DisplayName("第一次请求 TTL 设置失败时失败关闭")
    void first_request_returns_true_when_expire_returns_false() {
        when(valueOps.increment(anyString())).thenReturn(1L);
        when(redis.expire(anyString(), any())).thenReturn(false);

        boolean exceeded = ApiCallHandler.isRateLimitExceeded(redis, "test_api", 10, fixedInstant());

        assertThat(exceeded).isTrue();
    }

    @Test
    @DisplayName("第一次请求 TTL 设置结果为空时失败关闭")
    void first_request_returns_true_when_expire_returns_null() {
        when(valueOps.increment(anyString())).thenReturn(1L);
        when(redis.expire(anyString(), any())).thenReturn(null);

        boolean exceeded = ApiCallHandler.isRateLimitExceeded(redis, "test_api", 10, fixedInstant());

        assertThat(exceeded).isTrue();
    }

    @Test
    @DisplayName("Redis 计数为空时失败关闭")
    void null_count_returns_true() {
        when(valueOps.increment(anyString())).thenReturn(null);

        boolean exceeded = ApiCallHandler.isRateLimitExceeded(redis, "test_api", 10, fixedInstant());

        assertThat(exceeded).isTrue();
    }

    @Test
    @DisplayName("非法限制值直接失败关闭且不访问 Redis")
    void invalid_limit_returns_true_without_touching_redis() {
        boolean exceeded = ApiCallHandler.isRateLimitExceeded(redis, "test_api", 0, fixedInstant());

        assertThat(exceeded).isTrue();
        verifyNoInteractions(redis);
    }

    @Test
    @DisplayName("Redis 限流检查异常时返回失败结果且不调用下游 HTTP")
    void executeAsync_returns_failure_when_redis_rate_limit_check_throws() {
        ApiDefinitionDO def = enabledDefinition(10);
        when(apiDefinitionCache.getEnabled("test_api")).thenReturn(def);
        when(valueOps.increment(anyString())).thenThrow(new RuntimeException("redis down"));
        ApiCallHandler handler = new ApiCallHandler(
                apiDefinitionCache, webClientBuilder, new com.fasterxml.jackson.databind.ObjectMapper(),
                payloadBuilder, redis, defaultKeys());

        NodeResult result = handler.executeAsync(Map.of("apiKey", "test_api"), ctx).block();

        assertThat(result.success()).isFalse();
        assertThat(result.errorMessage()).contains("速率限制检查失败");
        verify(webClientBuilder, never()).build();
    }

    @Test
    @DisplayName("请求准备异常不应被误报为 Redis 限流检查失败")
    void executeAsync_propagates_request_preparation_errors_after_rate_limit_passes() {
        ApiDefinitionDO def = enabledDefinition(10);
        when(apiDefinitionCache.getEnabled("test_api")).thenReturn(def);
        when(valueOps.increment(anyString())).thenReturn(2L);
        ApiCallHandler handler = new ApiCallHandler(
                apiDefinitionCache, webClientBuilder, new com.fasterxml.jackson.databind.ObjectMapper(),
                payloadBuilder, redis, defaultKeys());

        assertThatThrownBy(() -> handler.executeAsync(
                Map.of("apiKey", "test_api", "inputParams", "not-a-map"), ctx).block())
                .isInstanceOf(ClassCastException.class);
        verify(webClientBuilder, never()).build();
    }

    @Test
    @DisplayName("持久化非法限制值时返回失败结果且不调用 Redis 或下游 HTTP")
    void executeAsync_returns_failure_when_persisted_rate_limit_is_invalid() {
        ApiDefinitionDO def = enabledDefinition(0);
        when(apiDefinitionCache.getEnabled("test_api")).thenReturn(def);
        ApiCallHandler handler = new ApiCallHandler(
                apiDefinitionCache, webClientBuilder, new com.fasterxml.jackson.databind.ObjectMapper(),
                payloadBuilder, redis, defaultKeys());

        NodeResult result = handler.executeAsync(Map.of("apiKey", "test_api"), ctx).block();

        assertThat(result.success()).isFalse();
        assertThat(result.errorMessage()).contains("速率限制配置无效");
        verifyNoInteractions(redis);
        verify(webClientBuilder, never()).build();
    }

    @Test
    @DisplayName("未超限时执行下游 HTTP 并解析 JSON 输出")
    void executeAsync_calls_downstream_and_parses_json_when_under_limit() {
        ApiDefinitionDO def = enabledDefinition(10);
        when(apiDefinitionCache.getEnabled("test_api")).thenReturn(def);
        when(valueOps.increment(anyString())).thenReturn(1L);
        when(redis.expire(anyString(), any())).thenReturn(true);
        ExchangeFunction exchangeFunction = request -> Mono.just(ClientResponse.create(HttpStatus.OK)
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .body("{\"accepted\":true}")
                .build());
        ApiCallHandler handler = new ApiCallHandler(
                apiDefinitionCache, WebClient.builder().exchangeFunction(exchangeFunction),
                new com.fasterxml.jackson.databind.ObjectMapper(), payloadBuilder, redis, defaultKeys());

        NodeResult result = handler.executeAsync(Map.of("apiKey", "test_api"), ctx).block();

        assertThat(result.success()).isTrue();
        assertThat(result.output()).containsEntry("accepted", true);

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(valueOps).increment(keyCaptor.capture());
        String capturedKey = keyCaptor.getValue();
        assertThat(capturedKey).matches("canvas:ratelimit:test_api:\\d+");
        verify(redis).expire(capturedKey, Duration.ofSeconds(2));
    }

    @Test
    @DisplayName("开启 validateRules 时接口响应不满足规则则失败")
    void executeAsync_returns_failure_when_response_validation_rules_do_not_match() {
        ApiDefinitionDO def = enabledDefinition(null);
        when(apiDefinitionCache.getEnabled("test_api")).thenReturn(def);
        ExchangeFunction exchangeFunction = request -> Mono.just(ClientResponse.create(HttpStatus.OK)
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .body("{\"accepted\":false}")
                .build());
        ApiCallHandler handler = new ApiCallHandler(
                apiDefinitionCache, WebClient.builder().exchangeFunction(exchangeFunction),
                new com.fasterxml.jackson.databind.ObjectMapper(), payloadBuilder, redis, defaultKeys());

        NodeResult result = handler.executeAsync(Map.of(
                "apiKey", "test_api",
                "validateResult", true,
                "validateRules", List.of(Map.of(
                        "field", "accepted",
                        "operator", "EQ",
                        "value", "true"))), ctx).block();

        assertThat(result.success()).isFalse();
        assertThat(result.errorMessage()).contains("响应校验不通过");
    }

    @Test
    @DisplayName("Redis 计数为空时生产路径返回限流检查失败且不调用下游 HTTP")
    void executeAsync_returns_check_failed_when_redis_increment_returns_null() {
        ApiDefinitionDO def = enabledDefinition(10);
        when(apiDefinitionCache.getEnabled("test_api")).thenReturn(def);
        when(valueOps.increment(anyString())).thenReturn(null);
        ApiCallHandler handler = new ApiCallHandler(
                apiDefinitionCache, webClientBuilder, new com.fasterxml.jackson.databind.ObjectMapper(),
                payloadBuilder, redis, defaultKeys());

        NodeResult result = handler.executeAsync(Map.of("apiKey", "test_api"), ctx).block();

        assertThat(result.success()).isFalse();
        assertThat(result.errorMessage()).contains("速率限制检查失败");
        verify(webClientBuilder, never()).build();
    }

    @Test
    @DisplayName("第一次请求 TTL 设置失败时生产路径返回限流检查失败且不调用下游 HTTP")
    void executeAsync_returns_check_failed_when_first_expire_returns_false() {
        ApiDefinitionDO def = enabledDefinition(10);
        when(apiDefinitionCache.getEnabled("test_api")).thenReturn(def);
        when(valueOps.increment(anyString())).thenReturn(1L);
        when(redis.expire(anyString(), any())).thenReturn(false);
        ApiCallHandler handler = new ApiCallHandler(
                apiDefinitionCache, webClientBuilder, new com.fasterxml.jackson.databind.ObjectMapper(),
                payloadBuilder, redis, defaultKeys());

        NodeResult result = handler.executeAsync(Map.of("apiKey", "test_api"), ctx).block();

        assertThat(result.success()).isFalse();
        assertThat(result.errorMessage()).contains("速率限制检查失败");
        verify(webClientBuilder, never()).build();
    }

    @Test
    @DisplayName("第一次请求 TTL 设置结果为空时生产路径返回限流检查失败且不调用下游 HTTP")
    void executeAsync_returns_check_failed_when_first_expire_returns_null() {
        ApiDefinitionDO def = enabledDefinition(10);
        when(apiDefinitionCache.getEnabled("test_api")).thenReturn(def);
        when(valueOps.increment(anyString())).thenReturn(1L);
        when(redis.expire(anyString(), any())).thenReturn(null);
        ApiCallHandler handler = new ApiCallHandler(
                apiDefinitionCache, webClientBuilder, new com.fasterxml.jackson.databind.ObjectMapper(),
                payloadBuilder, redis, defaultKeys());

        NodeResult result = handler.executeAsync(Map.of("apiKey", "test_api"), ctx).block();

        assertThat(result.success()).isFalse();
        assertThat(result.errorMessage()).contains("速率限制检查失败");
        verify(webClientBuilder, never()).build();
    }

    @Test
    @DisplayName("生产路径使用配置的 Redis key 前缀")
    void executeAsync_uses_configured_redis_key_prefix_when_under_limit() {
        ApiDefinitionDO def = enabledDefinition(10);
        when(apiDefinitionCache.getEnabled("test_api")).thenReturn(def);
        when(valueOps.increment(anyString())).thenReturn(2L);
        ExchangeFunction exchangeFunction = request -> Mono.just(ClientResponse.create(HttpStatus.OK)
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .body("{\"accepted\":true}")
                .build());
        ApiCallHandler handler = new ApiCallHandler(
                apiDefinitionCache, WebClient.builder().exchangeFunction(exchangeFunction),
                new com.fasterxml.jackson.databind.ObjectMapper(), payloadBuilder, redis, keysWithPrefix("testenv"));

        NodeResult result = handler.executeAsync(Map.of("apiKey", "test_api"), ctx).block();

        assertThat(result.success()).isTrue();
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(valueOps).increment(keyCaptor.capture());
        assertThat(keyCaptor.getValue()).matches("testenv:ratelimit:test_api:\\d+");
    }

    private static Instant fixedInstant() {
        return Instant.ofEpochSecond(1_700_000_000L);
    }

    private static ApiDefinitionDO enabledDefinition(Integer rateLimitPerSec) {
        ApiDefinitionDO def = new ApiDefinitionDO();
        def.setApiKey("test_api");
        def.setEnabled(1);
        def.setUrl("https://example.test/api");
        def.setMethod("POST");
        def.setRateLimitPerSec(rateLimitPerSec);
        return def;
    }

    private static RedisKeyUtil defaultKeys() {
        return keysWithPrefix("canvas");
    }

    private static RedisKeyUtil keysWithPrefix(String prefix) {
        RedisKeyUtil keys = new RedisKeyUtil();
        ReflectionTestUtils.setField(keys, "prefix", prefix);
        return keys;
    }

}
