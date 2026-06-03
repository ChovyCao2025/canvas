package org.chovy.canvas.domain.canvas;

import com.baomidou.mybatisplus.core.conditions.AbstractWrapper;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.chovy.canvas.dal.dataobject.CanvasDO;
import org.chovy.canvas.dal.dataobject.CanvasVersionDO;
import org.chovy.canvas.dal.mapper.CanvasMapper;
import org.chovy.canvas.dal.mapper.CanvasVersionMapper;
import org.chovy.canvas.dto.CanvasCreateReq;
import org.chovy.canvas.query.CanvasListQuery;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class CanvasServiceTenantIsolationTest {

    @BeforeAll
    static void initMyBatisPlusTableInfo() {
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), ""),
                CanvasDO.class);
    }

    @Test
    void createAssignsTenantToCanvasAndInitialDraft() {
        CanvasMapper canvasMapper = mock(CanvasMapper.class);
        CanvasVersionMapper versionMapper = mock(CanvasVersionMapper.class);
        doAnswer(invocation -> {
            CanvasDO canvas = invocation.getArgument(0);
            canvas.setId(9L);
            return 1;
        }).when(canvasMapper).insert(any(CanvasDO.class));
        CanvasService service = service(canvasMapper, versionMapper);
        CanvasCreateReq req = new CanvasCreateReq();
        req.setName("welcome");
        req.setGraphJson("{\"nodes\":[]}");
        req.setCreatedBy("operator");

        service.create(req, 42L);

        ArgumentCaptor<CanvasDO> canvasCaptor = ArgumentCaptor.forClass(CanvasDO.class);
        verify(canvasMapper).insert(canvasCaptor.capture());
        assertThat(canvasCaptor.getValue().getTenantId()).isEqualTo(42L);

        ArgumentCaptor<CanvasVersionDO> versionCaptor = ArgumentCaptor.forClass(CanvasVersionDO.class);
        verify(versionMapper).insert(versionCaptor.capture());
        assertThat(versionCaptor.getValue().getTenantId()).isEqualTo(42L);
        assertThat(versionCaptor.getValue().getCanvasId()).isEqualTo(9L);
    }

    @Test
    void listScopesByTenant() {
        CanvasMapper canvasMapper = mock(CanvasMapper.class);
        CanvasService service = service(canvasMapper, mock(CanvasVersionMapper.class));
        doAnswer(invocation -> new Page<CanvasDO>()).when(canvasMapper).selectPage(any(), any());

        service.list(new CanvasListQuery(), 42L);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Wrapper<CanvasDO>> captor = ArgumentCaptor.forClass(Wrapper.class);
        verify(canvasMapper).selectPage(any(), captor.capture());
        AbstractWrapper<CanvasDO, ?, ?> wrapper = (AbstractWrapper<CanvasDO, ?, ?>) captor.getValue();
        assertThat(wrapper.getSqlSegment()).contains("tenant_id");
        assertThat(wrapper.getParamNameValuePairs()).containsValue(42L);
    }

    private static CanvasService service(CanvasMapper canvasMapper, CanvasVersionMapper versionMapper) {
        return new CanvasService(
                canvasMapper,
                versionMapper,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                new CanvasStateTransitionPolicy(),
                new CanvasExamplesProperties(),
                null
        );
    }
}
