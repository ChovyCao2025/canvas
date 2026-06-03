package org.chovy.canvas.engine.trigger;

import org.chovy.canvas.common.enums.CanvasStatusEnum;
import org.chovy.canvas.dal.dataobject.CanvasDO;
import org.chovy.canvas.dal.mapper.CanvasMapper;
import org.chovy.canvas.dal.mapper.CanvasUserQuotaMapper;
import org.chovy.canvas.engine.concurrent.BackgroundTaskExecutor;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TriggerPreCheckServiceQuotaReconciliationTest {

    @Test
    void reconcileInactiveCanvasQuotasCleansPermanentQuotaKeysForInactiveCanvases() {
        CanvasMapper canvasMapper = mock(CanvasMapper.class);
        CanvasUserQuotaMapper quotaMapper = mock(CanvasUserQuotaMapper.class);
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        BackgroundTaskExecutor backgroundTaskExecutor = mock(BackgroundTaskExecutor.class);
        TriggerPreCheckService service = new TriggerPreCheckService(
                canvasMapper, quotaMapper, redis, backgroundTaskExecutor);

        when(canvasMapper.selectList(any())).thenReturn(List.of(
                canvas(10L, CanvasStatusEnum.OFFLINE),
                canvas(11L, CanvasStatusEnum.KILLED)));

        int reconciled = service.reconcileInactiveCanvasQuotas();

        assertThat(reconciled).isEqualTo(2);
        verify(redis).delete("canvas:global_count:10");
        verify(redis).delete("canvas:global_count:11");
        verify(backgroundTaskExecutor).submitBestEffort(org.mockito.ArgumentMatchers.eq("quota-cleanup-10"), any());
        verify(backgroundTaskExecutor).submitBestEffort(org.mockito.ArgumentMatchers.eq("quota-cleanup-11"), any());
    }

    private static CanvasDO canvas(Long id, CanvasStatusEnum status) {
        CanvasDO canvas = new CanvasDO();
        canvas.setId(id);
        canvas.setStatus(status.getCode());
        return canvas;
    }
}
