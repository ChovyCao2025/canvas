package org.chovy.canvas.domain.cdp;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CdpTagOperationServiceRetryTest {

    @Test
    void retryFailedRequeuesOnlyFailedUsers() {
        CdpTagOperationMapper mapper = Mockito.mock(CdpTagOperationMapper.class);
        CdpTagService tagService = Mockito.mock(CdpTagService.class);
        CdpTagOperationService service = new CdpTagOperationService(mapper, tagService);

        CdpTagOperation existing = new CdpTagOperation();
        existing.setId(7L);
        existing.setOperationType("BATCH_SET");
        existing.setTagCode("vip");
        existing.setTagValue("true");
        existing.setCreatedBy("admin");
        existing.setErrorMsg("u1: bad value; u2: timeout; ");
        when(mapper.selectById(7L)).thenReturn(existing);
        when(mapper.insert(any(CdpTagOperation.class))).thenAnswer(invocation -> {
            CdpTagOperation created = invocation.getArgument(0);
            created.setId(8L);
            return 1;
        });

        service.retryFailed(7L, null);

        ArgumentCaptor<CdpTagOperation> opCaptor = ArgumentCaptor.forClass(CdpTagOperation.class);
        verify(mapper).insert(opCaptor.capture());
        assertThat(opCaptor.getValue().getTagCode()).isEqualTo("vip");
        assertThat(opCaptor.getValue().getTotalCount()).isEqualTo(2);
    }
}
