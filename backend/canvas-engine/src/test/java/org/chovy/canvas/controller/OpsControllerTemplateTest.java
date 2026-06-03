package org.chovy.canvas.web;

import org.chovy.canvas.common.tenant.RoleNames;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.dal.mapper.CanvasManualApprovalMapper;
import org.chovy.canvas.dal.dataobject.CanvasDO;
import org.chovy.canvas.dal.mapper.CanvasMapper;
import org.chovy.canvas.dal.dataobject.CanvasTemplateDO;
import org.chovy.canvas.dal.mapper.CanvasTemplateMapper;
import org.chovy.canvas.dal.dataobject.CanvasVersionDO;
import org.chovy.canvas.dal.mapper.CanvasVersionMapper;
import org.chovy.canvas.common.enums.CanvasStatusEnum;
import org.chovy.canvas.common.enums.VersionStatus;
import org.chovy.canvas.infrastructure.cache.CanvasConfigCache;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Ops Controller Template 测试类。
 *
 * <p>覆盖该后端组件在典型输入、边界条件和异常场景下的行为，确保重构或性能优化不会改变既有契约。
 * <p>测试代码只构造必要的依赖与数据，断言重点放在可观察结果、状态变更和关键副作用上。
 */
@ExtendWith(MockitoExtension.class)
class OpsControllerTemplateTest {

    @Mock CanvasTemplateMapper templateMapper;
    @Mock CanvasMapper canvasMapper;
    @Mock CanvasVersionMapper canvasVersionMapper;
    @Mock CanvasManualApprovalMapper approvalMapper;
    @Mock CanvasConfigCache configCache;
    @Mock TenantContextResolver tenantContextResolver;

    @Test
    void createFromTemplateCreatesDraftVersionWithTemplateGraph() {
        CanvasTemplateDO template = new CanvasTemplateDO();
        template.setId(5L);
        template.setName("示例：事件触发新客领券");
        template.setDescription("事件触发后判断新客并发券");
        template.setGraphJson("{\"nodes\":[]}");
        template.setUseCount(3);
        when(templateMapper.selectById(5L)).thenReturn(template);
        doAnswer(invocation -> {
            CanvasDO canvas = invocation.getArgument(0);
            canvas.setId(88L);
            return 1;
        }).when(canvasMapper).insert(any(CanvasDO.class));
        when(tenantContextResolver.current())
                .thenReturn(Mono.just(new TenantContext(42L, RoleNames.TENANT_ADMIN, "operator")));

        OpsController controller = new OpsController(
                templateMapper, canvasMapper, canvasVersionMapper,
                approvalMapper, configCache, tenantContextResolver);
        OpsController.FromTemplateReq req = new OpsController.FromTemplateReq();
        req.setName("我的新客发券流程");

        CanvasDO created = controller.createFromTemplate(5L, req).block().getData();

        assertThat(created.getId()).isEqualTo(88L);
        assertThat(created.getTenantId()).isEqualTo(42L);
        assertThat(created.getName()).isEqualTo("我的新客发券流程");
        assertThat(created.getStatus()).isEqualTo(CanvasStatusEnum.DRAFT.getCode());
        assertThat(created.getIsExample()).isEqualTo(0);
        assertThat(created.getSourceTemplateKey()).isNull();

        ArgumentCaptor<CanvasVersionDO> versionCaptor = ArgumentCaptor.forClass(CanvasVersionDO.class);
        verify(canvasVersionMapper).insert(versionCaptor.capture());
        CanvasVersionDO version = versionCaptor.getValue();
        assertThat(version.getTenantId()).isEqualTo(42L);
        assertThat(version.getCanvasId()).isEqualTo(88L);
        assertThat(version.getVersion()).isEqualTo(1);
        assertThat(version.getGraphJson()).isEqualTo("{\"nodes\":[]}");
        assertThat(version.getStatus()).isEqualTo(VersionStatus.DRAFT.getCode());

        assertThat(template.getUseCount()).isEqualTo(4);
        verify(templateMapper).updateById(template);
    }
}
