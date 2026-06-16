package org.chovy.canvas.execution.adapter.external;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

import org.chovy.canvas.execution.application.CanvasTriggerApplicationService.TriggerRoute;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.ReactiveValueOperations;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * 定义 RedisTriggerRouteAdapterTest 的执行上下文数据结构或业务契约。
 */
class RedisTriggerRouteAdapterTest {

    /**
     * 执行 buildsStableTenantScopedRedisKeysAndRoundTripsRoutes 对应的业务处理。
     */
    @Test
    void buildsStableTenantScopedRedisKeysAndRoundTripsRoutes() {
        RedisTriggerRouteAdapter adapter = new RedisTriggerRouteAdapter(null);
        TriggerRoute route = new TriggerRoute(7L, 11L, 13L, "MQ", "orders.created");

        String key = adapter.routeKey(route.tenantId(), route.triggerType(), route.matchKey());
        String serialized = adapter.serialize(route);
        TriggerRoute restored = adapter.deserialize(serialized);

        assertThat(key).isEqualTo("canvas:execution:trigger-route:7:MQ:orders.created");
        assertThat(restored).isEqualTo(route);
    }

    /**
     * 执行 routesForReturnsRouteSavedForTriggerTypeAndMatchKey 对应的业务处理。
     */
    @Test
    void routesForReturnsRouteSavedForTriggerTypeAndMatchKey() {
        ReactiveStringRedisTemplate redisTemplate = mock(ReactiveStringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ReactiveValueOperations<String, String> values = mock(ReactiveValueOperations.class);
        Map<String, String> redisValues = new LinkedHashMap<>();
        when(redisTemplate.opsForValue()).thenReturn(values);
        when(values.set(anyString(), anyString())).thenAnswer(invocation -> {
            redisValues.put(invocation.getArgument(0), invocation.getArgument(1));
            return Mono.just(true);
        });
        when(values.get(anyString())).thenAnswer(invocation -> Mono.justOrEmpty(redisValues.get(invocation.getArgument(0))));
        when(redisTemplate.delete(anyString())).thenAnswer(invocation -> {
            redisValues.remove(invocation.getArgument(0, String.class));
            return Mono.just(true);
        });
        when(redisTemplate.keys(anyString())).thenAnswer(invocation -> {
            Pattern keyPattern = Pattern.compile(invocation.getArgument(0, String.class).replace("*", "[^:]+"));
            return Flux.fromIterable(redisValues.keySet().stream()
                    .filter(key -> keyPattern.matcher(key).matches())
                    .toList());
        });

        RedisTriggerRouteAdapter adapter = new RedisTriggerRouteAdapter(redisTemplate);
        TriggerRoute route = new TriggerRoute(7L, 11L, 13L, "MQ", "orders.created");

        adapter.save(route);

        assertThat(adapter.routesFor("mq", "orders.created")).containsExactly(route);
    }

    /**
     * 执行 removeDeletesOnlyMatchingTenantCanvasRoutes 对应的业务处理。
     */
    @Test
    void removeDeletesOnlyMatchingTenantCanvasRoutes() {
        ReactiveStringRedisTemplate redisTemplate = mock(ReactiveStringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ReactiveValueOperations<String, String> values = mock(ReactiveValueOperations.class);
        Map<String, String> redisValues = new LinkedHashMap<>();
        when(redisTemplate.opsForValue()).thenReturn(values);
        when(values.set(anyString(), anyString())).thenAnswer(invocation -> {
            redisValues.put(invocation.getArgument(0), invocation.getArgument(1));
            return Mono.just(true);
        });
        when(values.get(anyString())).thenAnswer(invocation -> Mono.justOrEmpty(redisValues.get(invocation.getArgument(0))));
        when(redisTemplate.delete(anyString())).thenAnswer(invocation -> {
            redisValues.remove(invocation.getArgument(0, String.class));
            return Mono.just(true);
        });
        when(redisTemplate.keys(anyString())).thenAnswer(invocation -> {
            Pattern keyPattern = Pattern.compile(invocation.getArgument(0, String.class).replace("*", "[^:]+"));
            return Flux.fromIterable(redisValues.keySet().stream()
                    .filter(key -> keyPattern.matcher(key).matches())
                    .toList());
        });
        RedisTriggerRouteAdapter adapter = new RedisTriggerRouteAdapter(redisTemplate);
        TriggerRoute route = new TriggerRoute(7L, 11L, 13L, "MQ", "orders.created");
        TriggerRoute otherCanvas = new TriggerRoute(7L, 12L, 14L, "MQ", "orders.updated");

        adapter.save(route);
        adapter.save(otherCanvas);
        adapter.remove(7L, 11L);

        assertThat(adapter.routesFor("MQ", "orders.created")).isEmpty();
        assertThat(adapter.routesFor("MQ", "orders.updated")).containsExactly(otherCanvas);
    }

    /**
     * 执行 routesForReturnsBlankMatchRoutesAsWildcardRoutes 对应的业务处理。
     */
    @Test
    void routesForReturnsBlankMatchRoutesAsWildcardRoutes() {
        ReactiveStringRedisTemplate redisTemplate = mock(ReactiveStringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ReactiveValueOperations<String, String> values = mock(ReactiveValueOperations.class);
        Map<String, String> redisValues = new LinkedHashMap<>();
        when(redisTemplate.opsForValue()).thenReturn(values);
        when(values.set(anyString(), anyString())).thenAnswer(invocation -> {
            redisValues.put(invocation.getArgument(0), invocation.getArgument(1));
            return Mono.just(true);
        });
        when(values.get(anyString())).thenAnswer(invocation -> Mono.justOrEmpty(redisValues.get(invocation.getArgument(0))));
        when(redisTemplate.keys(anyString())).thenAnswer(invocation -> {
            Pattern keyPattern = Pattern.compile(invocation.getArgument(0, String.class).replace("*", "[^:]+"));
            return Flux.fromIterable(redisValues.keySet().stream()
                    .filter(key -> keyPattern.matcher(key).matches())
                    .toList());
        });
        RedisTriggerRouteAdapter adapter = new RedisTriggerRouteAdapter(redisTemplate);
        TriggerRoute generic = new TriggerRoute(7L, 11L, 13L, "MQ", "");

        adapter.save(generic);

        assertThat(adapter.routesFor("MQ", "orders.created")).containsExactly(generic);
    }
}
