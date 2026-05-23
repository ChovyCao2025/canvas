package org.chovy.canvas.controller;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.domain.meta.ApiDefinition;
import org.chovy.canvas.domain.meta.ApiDefinitionMapper;
import org.chovy.canvas.infra.cache.ApiDefinitionCache;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class ApiDefinitionControllerTest {

    private ApiDefinitionMapper apiDefinitionMapper;
    private ApiDefinitionCache apiDefinitionCache;
    private ApiDefinitionController controller;
    private ObjectMapper objectMapper;

    @BeforeAll
    static void initMyBatisPlusTableInfo() {
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), ""),
                ApiDefinition.class);
    }

    @BeforeEach
    void setUp() {
        apiDefinitionMapper = Mockito.mock(ApiDefinitionMapper.class);
        apiDefinitionCache = Mockito.mock(ApiDefinitionCache.class);
        objectMapper = new ObjectMapper();
        controller = new ApiDefinitionController(apiDefinitionMapper, apiDefinitionCache, objectMapper);
    }

    @Test
    void create_rejects_zero_rateLimitPerSec_and_does_not_insert() {
        ApiDefinition body = new ApiDefinition();
        body.setRateLimitPerSec(0);

        assertThatThrownBy(() -> controller.create(body).block())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("rateLimitPerSec 必须大于 0");

        verify(apiDefinitionMapper, never()).insert(Mockito.any(ApiDefinition.class));
    }

    @Test
    void update_clears_rateLimitPerSec_when_explicit_null() throws Exception {
        JsonNode bodyNode = objectMapper.readTree("{\"rateLimitPerSec\":null}");

        controller.update(1L, bodyNode).block();

        verify(apiDefinitionMapper).update(any(ApiDefinition.class), any(LambdaUpdateWrapper.class));
        verify(apiDefinitionMapper, never()).updateById(any(ApiDefinition.class));
    }

    @Test
    void update_omitting_rateLimitPerSec_uses_updateById_without_clear() throws Exception {
        JsonNode bodyNode = objectMapper.readTree("{\"name\":\"Demo API\"}");

        controller.update(1L, bodyNode).block();

        verify(apiDefinitionMapper).updateById(any(ApiDefinition.class));
        verify(apiDefinitionMapper, never()).update(any(ApiDefinition.class), any(LambdaUpdateWrapper.class));
    }

    @Test
    void update_rejects_negative_rateLimitPerSec_and_does_not_update() throws Exception {
        JsonNode bodyNode = objectMapper.readTree("{\"rateLimitPerSec\":-1}");

        assertThatThrownBy(() -> controller.update(1L, bodyNode).block())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("rateLimitPerSec 必须大于 0");

        verify(apiDefinitionMapper, never()).updateById(any(ApiDefinition.class));
        verify(apiDefinitionMapper, never()).update(any(ApiDefinition.class), any(LambdaUpdateWrapper.class));
    }
}
