package org.chovy.canvas.domain.canvas;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.AbstractWrapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.chovy.canvas.dal.dataobject.CanvasDO;
import org.chovy.canvas.dal.dataobject.CanvasVersionDO;
import org.chovy.canvas.dal.mapper.CanvasMapper;
import org.chovy.canvas.dal.mapper.CanvasVersionMapper;
import org.chovy.canvas.dto.CanvasCreateReq;
import org.chovy.canvas.engine.dag.DagParser;
import org.chovy.canvas.engine.handlers.GroovyHandler;
import org.chovy.canvas.engine.rule.CanvasRuleGraphValidator;
import org.chovy.canvas.engine.trigger.CanvasExecutionService;
import org.chovy.canvas.engine.trigger.CanvasSchedulerService;
import org.chovy.canvas.engine.trigger.TriggerPreCheckService;
import org.chovy.canvas.infrastructure.cache.CanvasConfigCache;
import org.chovy.canvas.infrastructure.redis.TriggerRouteService;
import org.chovy.canvas.query.CanvasListQuery;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.access.AccessDeniedException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CanvasTenantIsolationTest {

    @BeforeAll
    static void initMyBatisPlusTableInfo() {
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), ""),
                CanvasDO.class);
    }

    @Test
    void createPersistsTenantOnCanvasAndInitialDraftVersion() {
        CanvasMapper canvasMapper = mock(CanvasMapper.class);
        CanvasVersionMapper versionMapper = mock(CanvasVersionMapper.class);
        doAnswer(invocation -> {
            CanvasDO canvas = invocation.getArgument(0);
            canvas.setId(100L);
            return 1;
        }).when(canvasMapper).insert(any(CanvasDO.class));
        CanvasService service = service(canvasMapper, versionMapper);
        CanvasCreateReq req = new CanvasCreateReq();
        req.setTenantId(9L);
        req.setName("tenant canvas");
        req.setGraphJson("{\"nodes\":[]}");

        service.create(req);

        ArgumentCaptor<CanvasDO> canvasCaptor = ArgumentCaptor.forClass(CanvasDO.class);
        ArgumentCaptor<CanvasVersionDO> versionCaptor = ArgumentCaptor.forClass(CanvasVersionDO.class);
        verify(canvasMapper).insert(canvasCaptor.capture());
        verify(versionMapper).insert(versionCaptor.capture());
        assertThat(canvasCaptor.getValue().getTenantId()).isEqualTo(9L);
        assertThat(versionCaptor.getValue().getTenantId()).isEqualTo(9L);
        assertThat(versionCaptor.getValue().getCanvasId()).isEqualTo(100L);
    }

    @Test
    void listAddsTenantPredicateWhenTenantIdIsPresent() {
        CanvasMapper canvasMapper = mock(CanvasMapper.class);
        when(canvasMapper.selectPage(any(), any())).thenReturn(new Page<>());
        CanvasService service = service(canvasMapper, mock(CanvasVersionMapper.class));
        CanvasListQuery query = new CanvasListQuery();
        query.setTenantId(9L);

        service.list(query);

        ArgumentCaptor<Wrapper<CanvasDO>> wrapperCaptor = ArgumentCaptor.forClass(Wrapper.class);
        verify(canvasMapper).selectPage(any(), wrapperCaptor.capture());
        AbstractWrapper<?, ?, ?> wrapper = (AbstractWrapper<?, ?, ?>) wrapperCaptor.getValue();
        assertThat(wrapper.getSqlSegment()).contains("tenant_id");
        assertThat(wrapper.getParamNameValuePairs()).containsValue(9L);
    }

    @Test
    void requireTenantAccessRejectsCrossTenantCanvasAndAllowsSuperAdmin() {
        CanvasMapper canvasMapper = mock(CanvasMapper.class);
        CanvasDO canvas = new CanvasDO();
        canvas.setId(100L);
        canvas.setTenantId(9L);
        when(canvasMapper.selectById(100L)).thenReturn(canvas);
        CanvasService service = service(canvasMapper, mock(CanvasVersionMapper.class));

        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                        service.requireTenantAccess(100L, 10L, false))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("跨租户");

        assertThat(service.requireTenantAccess(100L, 10L, true)).isSameAs(canvas);
        assertThat(service.requireTenantAccess(100L, 9L, false)).isSameAs(canvas);
    }

    private CanvasService service(CanvasMapper canvasMapper, CanvasVersionMapper versionMapper) {
        return new CanvasService(
                canvasMapper,
                versionMapper,
                mock(DagParser.class),
                mock(TriggerRouteService.class),
                mock(CanvasSchedulerService.class),
                mock(CanvasConfigCache.class),
                mock(CanvasExecutionService.class),
                mock(TriggerPreCheckService.class),
                mock(GroovyHandler.class),
                mock(org.chovy.canvas.engine.handlers.MqTriggerHandler.class),
                mock(CanvasRuleGraphValidator.class),
                mock(org.springframework.data.redis.core.StringRedisTemplate.class),
                mock(CanvasTransactionService.class),
                new CanvasExamplesProperties());
    }
}
