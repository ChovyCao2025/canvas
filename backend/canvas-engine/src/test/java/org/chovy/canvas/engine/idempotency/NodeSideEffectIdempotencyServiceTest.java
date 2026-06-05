package org.chovy.canvas.engine.idempotency;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.engine.context.ExecutionContext;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class NodeSideEffectIdempotencyServiceTest {

    @Test
    void buildKeyIsStableForSameExecutionNodeAndOperation() {
        NodeSideEffectIdempotencyService service = service(jdbc(), 3);
        ExecutionContext ctx = context(7L, "exec-1", 42L);

        String first = service.buildKey(ctx, "node-1", "SEND_SMS", "user-9");
        String second = service.buildKey(ctx, "node-1", "SEND_SMS", "user-9");

        assertThat(first)
                .isEqualTo(second)
                .hasSize(64);
    }

    @Test
    void buildKeySeparatesTenantsAndOperations() {
        NodeSideEffectIdempotencyService service = service(jdbc(), 3);

        String tenantA = service.buildKey(context(7L, "exec-1", 42L), "node-1", "SEND_SMS", "user-9");
        String tenantB = service.buildKey(context(8L, "exec-1", 42L), "node-1", "SEND_SMS", "user-9");
        String otherOperation = service.buildKey(context(7L, "exec-1", 42L), "node-1", "SEND_SMS", "user-10");

        assertThat(tenantA).isNotEqualTo(tenantB);
        assertThat(tenantA).isNotEqualTo(otherOperation);
    }

    @Test
    void reserveCreatesRunningRecordForUniqueOperation() {
        JdbcTemplate jdbc = jdbc();
        NodeSideEffectIdempotencyService service = service(jdbc, 3);
        ExecutionContext ctx = context(7L, "exec-1", 42L);

        NodeSideEffectIdempotencyService.ReserveResult result =
                service.reserve(ctx, "node-1", "SEND_MQ", "user-1:welcome");

        assertThat(result.reserved()).isTrue();
        assertThat(result.shouldExecute()).isTrue();
        assertThat(result.record().getStatus()).isEqualTo(NodeSideEffectIdempotencyService.STATUS_RUNNING);
        assertThat(result.record().getAttemptCount()).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM node_side_effect_idempotency", Integer.class))
                .isEqualTo(1);
    }

    @Test
    void reserveReturnsCompletedRecordForDuplicateOperation() {
        NodeSideEffectIdempotencyService service = service(jdbc(), 3);
        ExecutionContext ctx = context(7L, "exec-1", 42L);

        var first = service.reserve(ctx, "node-1", "API_CALL", "user-1:profile-sync");
        service.complete(first.record().getId(), Map.of("httpStatus", 200, "requestId", "r-1"));

        var duplicate = service.reserve(ctx, "node-1", "API_CALL", "user-1:profile-sync");

        assertThat(duplicate.completed()).isTrue();
        assertThat(duplicate.shouldExecute()).isFalse();
        assertThat(duplicate.cachedOutput())
                .containsEntry("httpStatus", 200)
                .containsEntry("requestId", "r-1");
    }

    @Test
    void completeStoresStableOutputSnapshot() {
        NodeSideEffectIdempotencyService service = service(jdbc(), 3);
        ExecutionContext ctx = context(7L, "exec-1", 42L);

        var reservation = service.reserve(ctx, "node-1", "POINTS", "user-1:points:100");
        service.complete(reservation.record().getId(), Map.of("ledgerId", 99L));

        String key = service.buildKey(ctx, "node-1", "POINTS", "user-1:points:100");
        assertThat(service.cachedOutput(key)).hasValueSatisfying(output ->
                assertThat(output).containsEntry("ledgerId", 99));
    }

    @Test
    void failAllowsRetryUntilMaxAttempts() {
        NodeSideEffectIdempotencyService service = service(jdbc(), 2);
        ExecutionContext ctx = context(7L, "exec-1", 42L);

        var first = service.reserve(ctx, "node-1", "COUPON", "user-1:coupon:a");
        service.fail(first.record().getId(), "timeout");
        var second = service.reserve(ctx, "node-1", "COUPON", "user-1:coupon:a");
        service.fail(second.record().getId(), "timeout again");
        var exhausted = service.reserve(ctx, "node-1", "COUPON", "user-1:coupon:a");

        assertThat(first.reserved()).isTrue();
        assertThat(second.reserved()).isTrue();
        assertThat(second.record().getAttemptCount()).isEqualTo(2);
        assertThat(exhausted.exhausted()).isTrue();
        assertThat(exhausted.shouldExecute()).isFalse();
    }

    @Test
    void duplicateOperationDoesNotReexecuteExternalSideEffect() {
        NodeSideEffectIdempotencyService service = service(jdbc(), 3);
        ExecutionContext ctx = context(7L, "exec-1", 42L);
        AtomicInteger externalCalls = new AtomicInteger();

        runExternalSideEffectOnce(service, ctx, externalCalls);
        NodeSideEffectIdempotencyService.ReserveResult duplicate =
                runExternalSideEffectOnce(service, ctx, externalCalls);

        assertThat(externalCalls).hasValue(1);
        assertThat(duplicate.completed()).isTrue();
        assertThat(duplicate.cachedOutput()).containsEntry("messageId", "m-1");
    }

    private NodeSideEffectIdempotencyService.ReserveResult runExternalSideEffectOnce(
            NodeSideEffectIdempotencyService service,
            ExecutionContext ctx,
            AtomicInteger externalCalls) {
        var reservation = service.reserve(ctx, "node-1", "SEND_MQ", "user-1:welcome");
        if (reservation.shouldExecute()) {
            externalCalls.incrementAndGet();
            service.complete(reservation.record().getId(), Map.of("messageId", "m-1"));
        }
        return reservation;
    }

    private NodeSideEffectIdempotencyService service(JdbcTemplate jdbc, int maxAttempts) {
        return new NodeSideEffectIdempotencyService(jdbc, new ObjectMapper(), maxAttempts);
    }

    private JdbcTemplate jdbc() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.h2.Driver");
        dataSource.setUrl("jdbc:h2:mem:" + UUID.randomUUID()
                + ";MODE=MySQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1");
        dataSource.setUsername("sa");
        dataSource.setPassword("");
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        jdbc.execute("""
                CREATE TABLE node_side_effect_idempotency (
                  id BIGINT NOT NULL AUTO_INCREMENT,
                  tenant_id BIGINT NOT NULL DEFAULT 1,
                  execution_id VARCHAR(64) NOT NULL,
                  canvas_id BIGINT NOT NULL,
                  node_id VARCHAR(64) NOT NULL,
                  node_type VARCHAR(64) NOT NULL,
                  operation_key VARCHAR(255) NOT NULL,
                  idempotency_key VARCHAR(512) NOT NULL,
                  status VARCHAR(32) NOT NULL,
                  attempt_count INT NOT NULL DEFAULT 0,
                  output_json TEXT NULL,
                  error_message VARCHAR(500) NULL,
                  created_at DATETIME NOT NULL,
                  updated_at DATETIME NOT NULL,
                  PRIMARY KEY (id),
                  UNIQUE KEY uk_node_side_effect_tenant_key (tenant_id, idempotency_key)
                )
                """);
        return jdbc;
    }

    private ExecutionContext context(Long tenantId, String executionId, Long canvasId) {
        ExecutionContext ctx = new ExecutionContext();
        ctx.setTenantId(tenantId);
        ctx.setExecutionId(executionId);
        ctx.setCanvasId(canvasId);
        ctx.setUserId("user-1");
        return ctx;
    }
}
