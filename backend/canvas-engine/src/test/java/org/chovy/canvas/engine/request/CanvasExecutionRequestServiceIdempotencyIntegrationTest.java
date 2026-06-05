package org.chovy.canvas.engine.request;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.common.enums.NodeType;
import org.chovy.canvas.common.enums.TriggerType;
import org.chovy.canvas.dal.dataobject.CanvasExecutionRequestDO;
import org.chovy.canvas.dal.mapper.CanvasExecutionRequestMapper;
import org.chovy.canvas.testsupport.CanvasIntegrationTestBase;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Tag("integration")
class CanvasExecutionRequestServiceIdempotencyIntegrationTest extends CanvasIntegrationTestBase {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void duplicateMqSourceMessageIsPersistedOnceInMySql() {
        JdbcTemplate jdbc = jdbcTemplate();
        jdbc.update("DELETE FROM canvas_execution_request");

        Long defaultTenantId = jdbc.queryForObject(
                "SELECT id FROM tenant WHERE tenant_key = 'default'",
                Long.class);

        CanvasExecutionRequestMapper mapper = mock(CanvasExecutionRequestMapper.class);
        when(mapper.insertIgnore(any(CanvasExecutionRequestDO.class))).thenAnswer(invocation ->
                insertIgnore(jdbc, invocation.getArgument(0), defaultTenantId));

        CanvasExecutionRequestService service = new CanvasExecutionRequestService(mapper, objectMapper);

        String first = service.enqueue(10L, "user-1", TriggerType.MQ, NodeType.MQ_TRIGGER,
                "ORDER_PAID", Map.of("orderId", "O-1"), "MSG-1");
        String second = service.enqueue(10L, "user-1", TriggerType.MQ, NodeType.MQ_TRIGGER,
                "ORDER_PAID", Map.of("orderId", "O-1"), "MSG-1");

        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM canvas_execution_request WHERE id = ?",
                Integer.class,
                first);

        assertThat(second).isEqualTo(first);
        assertThat(count).isEqualTo(1);
    }

    private int insertIgnore(JdbcTemplate jdbc, CanvasExecutionRequestDO request, Long defaultTenantId) {
        return jdbc.update("""
                        INSERT IGNORE INTO canvas_execution_request
                        (id, tenant_id, canvas_id, user_id, trigger_type, trigger_node_type, match_key,
                         payload_json, source_msg_id, status, attempt_count, created_at, updated_at)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())
                        """,
                request.getId(),
                request.getTenantId() != null ? request.getTenantId() : defaultTenantId,
                request.getCanvasId(),
                request.getUserId(),
                request.getTriggerType(),
                request.getTriggerNodeType(),
                request.getMatchKey(),
                request.getPayloadJson(),
                request.getSourceMsgId(),
                request.getStatus(),
                request.getAttemptCount());
    }
}
