package org.chovy.canvas.web;

import org.chovy.canvas.dal.dataobject.CdpTagOperationDO;
import org.chovy.canvas.domain.cdp.CdpTagOperationService;
import org.chovy.canvas.dto.cdp.CdpBatchTagReq;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * CDP 标签操作 测试类。
 *
 * <p>覆盖该后端组件在典型输入、边界条件和异常场景下的行为，确保重构或性能优化不会改变既有契约。
 * <p>测试代码只构造必要的依赖与数据，断言重点放在可观察结果、状态变更和关键副作用上。
 */
class CdpTagOperationControllerTest {

    @Test
    void createReturnsCreatedOperation() {
        CdpTagOperationService service = Mockito.mock(CdpTagOperationService.class);
        CdpTagOperationController controller = new CdpTagOperationController(service);
        CdpTagOperationDO op = new CdpTagOperationDO();
        op.setId(7L);
        when(service.create(new CdpBatchTagReq("BATCH_SET", "vip", "true", List.of("u1"), "reason", "admin")))
                .thenReturn(op);

        CdpBatchTagReq req = new CdpBatchTagReq("BATCH_SET", "vip", "true", List.of("u1"), "reason", "admin");
        assertThat(controller.create(req).block().getData().getId()).isEqualTo(7L);
    }

    @Test
    void listReturnsRecentOperations() {
        CdpTagOperationService service = Mockito.mock(CdpTagOperationService.class);
        CdpTagOperationController controller = new CdpTagOperationController(service);
        when(service.listRecent(10)).thenReturn(List.of());

        assertThat(controller.list(10).block().getData()).isEmpty();
    }

    @Test
    void retryFailedReturnsNewOperation() {
        CdpTagOperationService service = Mockito.mock(CdpTagOperationService.class);
        CdpTagOperationController controller = new CdpTagOperationController(service);
        CdpTagOperationDO op = new CdpTagOperationDO();
        op.setId(9L);
        when(service.retryFailed(7L, null)).thenReturn(op);

        assertThat(controller.retryFailed(7L).block().getData().getId()).isEqualTo(9L);
    }
}
