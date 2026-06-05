package org.chovy.canvas.domain.compliance;

import org.chovy.canvas.dal.dataobject.CanvasAuditLogDO;
import org.chovy.canvas.dal.mapper.CanvasAuditLogMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class AuditEventServiceTest {

    @Test
    void writesMaskedAuditMetadataToCanvasAuditLog() {
        CanvasAuditLogMapper mapper = mock(CanvasAuditLogMapper.class);
        AuditEventService service = new AuditEventService(mapper, new PiiMaskingService());

        service.record(AuditEventService.AuditEventCommand.builder()
                .tenantId(7L)
                .actor("alice")
                .actorRole("ADMIN")
                .operation("canvas publish")
                .targetType("canvas")
                .targetId("42")
                .requestId("req-1")
                .ip("127.0.0.1")
                .metadata(Map.of("phone", "13812345678", "secret", "secret-token-123456"))
                .build());

        ArgumentCaptor<CanvasAuditLogDO> captor = ArgumentCaptor.forClass(CanvasAuditLogDO.class);
        verify(mapper).insert(captor.capture());
        CanvasAuditLogDO row = captor.getValue();
        assertThat(row.getCanvasId()).isEqualTo(42L);
        assertThat(row.getOperator()).isEqualTo("alice");
        assertThat(row.getOperatorRole()).isEqualTo("ADMIN");
        assertThat(row.getAction()).isEqualTo("canvas publish");
        assertThat(row.getIp()).isEqualTo("127.0.0.1");
        assertThat(row.getDetail()).contains("\"tenantId\":7");
        assertThat(row.getDetail()).contains("\"requestId\":\"req-1\"");
        assertThat(row.getDetail()).contains("138****5678");
        assertThat(row.getDetail()).contains("****3456");
        assertThat(row.getDetail()).doesNotContain("secret-token-123456");
    }
}
