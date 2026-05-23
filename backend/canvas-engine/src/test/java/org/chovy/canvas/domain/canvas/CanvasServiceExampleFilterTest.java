package org.chovy.canvas.domain.canvas;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.chovy.canvas.dto.CanvasListQuery;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CanvasServiceExampleFilterTest {

    @Mock CanvasMapper canvasMapper;
    @Mock CanvasVersionMapper canvasVersionMapper;
    @Mock org.chovy.canvas.engine.dag.DagParser dagParser;
    @Mock org.chovy.canvas.infra.redis.TriggerRouteService triggerRouteService;
    @Mock org.chovy.canvas.engine.trigger.CanvasSchedulerService schedulerService;
    @Mock org.chovy.canvas.infra.cache.CanvasConfigCache configCache;
    @Mock org.chovy.canvas.engine.trigger.CanvasExecutionService canvasExecutionService;
    @Mock org.chovy.canvas.engine.handlers.GroovyHandler groovyHandler;
    @Mock org.chovy.canvas.engine.handlers.MqTriggerHandler mqTriggerHandler;
    @Mock org.springframework.data.redis.core.StringRedisTemplate redis;
    @Mock CanvasTransactionService canvasTransactionService;

    CanvasExamplesProperties properties;
    CanvasService service;

    @BeforeAll
    static void initMyBatisPlusTableInfo() {
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), ""),
                Canvas.class);
    }

    @BeforeEach
    void setUp() {
        properties = new CanvasExamplesProperties();
        service = new CanvasService(
                canvasMapper, canvasVersionMapper, dagParser, triggerRouteService,
                schedulerService, configCache, canvasExecutionService, groovyHandler,
                mqTriggerHandler, redis, canvasTransactionService, properties);
        when(canvasMapper.selectPage(any(Page.class), any())).thenReturn(new Page<>());
    }

    @Test
    void listIncludesExamplesWhenToggleEnabled() {
        properties.setEnabled(true);

        service.list(new CanvasListQuery());

        ArgumentCaptor<LambdaQueryWrapper<Canvas>> captor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(canvasMapper).selectPage(any(Page.class), captor.capture());
        assertThat(captor.getValue().getSqlSegment()).doesNotContain("is_example");
    }

    @Test
    void listHidesExamplesWhenToggleDisabled() {
        properties.setEnabled(false);

        service.list(new CanvasListQuery());

        ArgumentCaptor<LambdaQueryWrapper<Canvas>> captor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(canvasMapper).selectPage(any(Page.class), captor.capture());
        assertThat(captor.getValue().getSqlSegment()).contains("is_example");
    }
}
