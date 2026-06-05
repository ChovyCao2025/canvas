package org.chovy.canvas.domain.canvas;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.AbstractWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.chovy.canvas.common.tenant.RoleNames;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.dal.dataobject.CanvasDO;
import org.chovy.canvas.dal.mapper.CanvasMapper;
import org.chovy.canvas.dal.mapper.CanvasVersionMapper;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CanvasServiceTenantScopeTest {

    @BeforeAll
    static void initMyBatisPlusTableInfo() {
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), ""),
                CanvasDO.class);
    }

    @Test
    void listUsesCurrentTenantContextForTenantAdmin() {
        CanvasMapper canvasMapper = mock(CanvasMapper.class);
        when(canvasMapper.selectPage(any(), any())).thenReturn(new Page<>());
        CanvasService service = service(canvasMapper, mock(CanvasVersionMapper.class));
        CanvasListQuery query = new CanvasListQuery();

        service.list(query, new TenantContext(3L, RoleNames.TENANT_ADMIN, "alice"));

        ArgumentCaptor<Wrapper<CanvasDO>> captor = ArgumentCaptor.forClass(Wrapper.class);
        verify(canvasMapper).selectPage(any(), captor.capture());
        AbstractWrapper<?, ?, ?> wrapper = (AbstractWrapper<?, ?, ?>) captor.getValue();
        assertThat(wrapper.getSqlSegment()).contains("tenant_id");
        assertThat(wrapper.getParamNameValuePairs()).containsValue(3L);
    }

    @Test
    void listAllowsLegacyAdminWithoutTenantFilter() {
        CanvasMapper canvasMapper = mock(CanvasMapper.class);
        when(canvasMapper.selectPage(any(), any())).thenReturn(new Page<>());
        CanvasService service = service(canvasMapper, mock(CanvasVersionMapper.class));
        CanvasListQuery query = new CanvasListQuery();

        service.list(query, new TenantContext(null, RoleNames.ADMIN, "root"));

        ArgumentCaptor<Wrapper<CanvasDO>> captor = ArgumentCaptor.forClass(Wrapper.class);
        verify(canvasMapper).selectPage(any(), captor.capture());
        AbstractWrapper<?, ?, ?> wrapper = (AbstractWrapper<?, ?, ?>) captor.getValue();
        assertThat(wrapper.getSqlSegment()).doesNotContain("tenant_id");
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
