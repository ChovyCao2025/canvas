package org.chovy.canvas.domain.cdp;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.chovy.canvas.dal.dataobject.CdpTagOperationDO;
import org.chovy.canvas.dal.mapper.CdpTagOperationMapper;

/**
 * CDP 标签操作 Service Retry 测试类。
 *
 * <p>覆盖该后端组件在典型输入、边界条件和异常场景下的行为，确保重构或性能优化不会改变既有契约。
 * <p>测试代码只构造必要的依赖与数据，断言重点放在可观察结果、状态变更和关键副作用上。
 */
class CdpTagOperationServiceRetryTest {

    @Test
    void retryFailedRequeuesOnlyFailedUsers() {
        CdpTagOperationMapper mapper = Mockito.mock(CdpTagOperationMapper.class);
        CdpTagService tagService = Mockito.mock(CdpTagService.class);
        CdpTagOperationService service = new CdpTagOperationService(mapper, tagService);

        CdpTagOperationDO existing = new CdpTagOperationDO();
        existing.setId(7L);
        existing.setOperationType("BATCH_SET");
        existing.setTagCode("vip");
        existing.setTagValue("true");
        existing.setCreatedBy("admin");
        existing.setErrorMsg("u1: bad value; u2: timeout; ");
        when(mapper.selectById(7L)).thenReturn(existing);
        when(mapper.insert(any(CdpTagOperationDO.class))).thenAnswer(invocation -> {
            CdpTagOperationDO created = invocation.getArgument(0);
            created.setId(8L);
            return 1;
        });

        service.retryFailed(7L, null);

        ArgumentCaptor<CdpTagOperationDO> opCaptor = ArgumentCaptor.forClass(CdpTagOperationDO.class);
        verify(mapper).insert(opCaptor.capture());
        assertThat(opCaptor.getValue().getTagCode()).isEqualTo("vip");
        assertThat(opCaptor.getValue().getTotalCount()).isEqualTo(2);
    }
}
