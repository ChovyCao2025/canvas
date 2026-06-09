package org.chovy.canvas.engine.llm;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.dal.dataobject.AiUsageAuditDO;
import org.chovy.canvas.dal.mapper.AiUsageAuditMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class AiUsageAuditServiceTest {

    @Test
    void recordsAuditEventToMapperWithOutputJsonAndRuntimeContext() {
        AiUsageAuditMapper mapper = mock(AiUsageAuditMapper.class);
        AiUsageAuditService service = new AiUsageAuditService(mapper, new ObjectMapper());

        service.record(new AiUsageAuditService.AiUsageAuditEvent(
                Instant.parse("2026-06-09T10:15:30Z"),
                42L,
                10L,
                "exec-1",
                "ai-1",
                101L,
                3L,
                "gpt-test",
                AiLlmGateway.STATUS_SUCCESS,
                false,
                123L,
                12,
                4,
                new ObjectMapper().createObjectNode().put("text", "hello"),
                null,
                null));

        ArgumentCaptor<AiUsageAuditDO> captor = ArgumentCaptor.forClass(AiUsageAuditDO.class);
        verify(mapper).insert(captor.capture());
        AiUsageAuditDO row = captor.getValue();
        assertThat(row.getTenantId()).isEqualTo(42L);
        assertThat(row.getCanvasId()).isEqualTo(10L);
        assertThat(row.getExecutionId()).isEqualTo("exec-1");
        assertThat(row.getNodeId()).isEqualTo("ai-1");
        assertThat(row.getProviderId()).isEqualTo(101L);
        assertThat(row.getTemplateId()).isEqualTo(3L);
        assertThat(row.getModelKey()).isEqualTo("gpt-test");
        assertThat(row.getStatus()).isEqualTo("SUCCESS");
        assertThat(row.getFallbackUsed()).isEqualTo(0);
        assertThat(row.getPromptTokens()).isEqualTo(12);
        assertThat(row.getCompletionTokens()).isEqualTo(4);
        assertThat(row.getOutputJson()).contains("\"text\":\"hello\"");
    }
}
