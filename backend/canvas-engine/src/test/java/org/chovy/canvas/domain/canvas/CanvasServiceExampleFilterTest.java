package org.chovy.canvas.domain.canvas;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.chovy.canvas.query.CanvasListQuery;
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
import org.chovy.canvas.dal.dataobject.CanvasDO;
import org.chovy.canvas.dal.mapper.CanvasMapper;
import org.chovy.canvas.dal.mapper.CanvasVersionMapper;

/**
 * Canvas Service Example Filter 测试类。
 *
 * <p>覆盖该后端组件在典型输入、边界条件和异常场景下的行为，确保重构或性能优化不会改变既有契约。
 * <p>测试代码只构造必要的依赖与数据，断言重点放在可观察结果、状态变更和关键副作用上。
 */
@ExtendWith(MockitoExtension.class)
class CanvasServiceExampleFilterTest {

    @Mock CanvasMapper canvasMapper;
    @Mock CanvasVersionMapper canvasVersionMapper;
    @Mock org.chovy.canvas.engine.dag.DagParser dagParser;
    @Mock org.chovy.canvas.infrastructure.redis.TriggerRouteService triggerRouteService;
    @Mock org.chovy.canvas.engine.trigger.CanvasSchedulerService schedulerService;
    @Mock org.chovy.canvas.infrastructure.cache.CanvasConfigCache configCache;
    @Mock org.chovy.canvas.engine.trigger.CanvasExecutionService canvasExecutionService;
    @Mock org.chovy.canvas.engine.trigger.TriggerPreCheckService preCheckService;
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
                CanvasDO.class);
    }

    @BeforeEach
    void setUp() {
        properties = new CanvasExamplesProperties();
        service = new CanvasService(
                canvasMapper, canvasVersionMapper, dagParser, triggerRouteService,
                schedulerService, configCache, canvasExecutionService, preCheckService, groovyHandler,
                mqTriggerHandler, redis, canvasTransactionService, properties);
        when(canvasMapper.selectPage(any(Page.class), any())).thenReturn(new Page<>());
    }

    @Test
    void listIncludesExamplesWhenToggleEnabled() {
        properties.setEnabled(true);

        service.list(new CanvasListQuery());

        ArgumentCaptor<LambdaQueryWrapper<CanvasDO>> captor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(canvasMapper).selectPage(any(Page.class), captor.capture());
        assertThat(captor.getValue().getSqlSegment()).doesNotContain("is_example");
    }

    @Test
    void listHidesExamplesWhenToggleDisabled() {
        properties.setEnabled(false);

        service.list(new CanvasListQuery());

        ArgumentCaptor<LambdaQueryWrapper<CanvasDO>> captor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(canvasMapper).selectPage(any(Page.class), captor.capture());
        assertThat(captor.getValue().getSqlSegment()).contains("is_example");
    }
}
