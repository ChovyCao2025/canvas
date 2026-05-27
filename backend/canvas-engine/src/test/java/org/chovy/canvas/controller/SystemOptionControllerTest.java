package org.chovy.canvas.web;

import org.chovy.canvas.dal.dataobject.SystemOptionDO;
import org.chovy.canvas.common.tenant.RoleNames;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.meta.SystemOptionService;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 系统选项 测试类。
 *
 * <p>覆盖该后端组件在典型输入、边界条件和异常场景下的行为，确保重构或性能优化不会改变既有契约。
 * <p>测试代码只构造必要的依赖与数据，断言重点放在可观察结果、状态变更和关键副作用上。
 */
class SystemOptionControllerTest {

    @Test
    void listReturnsPageResultShape() {
        SystemOptionService service = mock(SystemOptionService.class);
        TenantContextResolver resolver = mock(TenantContextResolver.class);
        when(resolver.current()).thenReturn(reactor.core.publisher.Mono.just(superAdmin()));
        SystemOptionDO option = new SystemOptionDO();
        option.setId(1L);
        option.setCategory("http_method");
        option.setOptionKey("POST");
        option.setLabel("POST");
        when(service.listForAdmin("http_method", null, null, null, true)).thenReturn(List.of(option));
        SystemOptionController controller = new SystemOptionController(service, resolver);

        StepVerifier.create(controller.list("http_method", null, null, null))
                .assertNext(result -> {
                    assertThat(result.getData().getTotal()).isEqualTo(1);
                    assertThat(result.getData().getList())
                            .extracting(SystemOptionDO::getOptionKey)
                            .containsExactly("POST");
                })
                .verifyComplete();
    }

    @Test
    void updateDelegatesToService() {
        SystemOptionService service = mock(SystemOptionService.class);
        TenantContextResolver resolver = mock(TenantContextResolver.class);
        when(resolver.current()).thenReturn(reactor.core.publisher.Mono.just(superAdmin()));
        SystemOptionController controller = new SystemOptionController(service, resolver);
        SystemOptionDO patch = new SystemOptionDO();
        patch.setLabel("POST（改）");

        StepVerifier.create(controller.update(1L, patch))
                .assertNext(result -> assertThat(result.getCode()).isEqualTo(0))
                .verifyComplete();

        verify(service).updateEditable(1L, patch, superAdmin());
    }

    private TenantContext superAdmin() {
        return new TenantContext(1L, RoleNames.SUPER_ADMIN, "root");
    }
}
