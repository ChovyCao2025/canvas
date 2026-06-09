package org.chovy.canvas.domain.risk;

import org.chovy.canvas.domain.risk.dsl.RiskRuntimeMode;
import org.chovy.canvas.domain.risk.dsl.RiskSubjectType;
import org.chovy.canvas.domain.risk.governance.RiskListCommand;
import org.chovy.canvas.domain.risk.governance.RiskListEntryCommand;
import org.chovy.canvas.domain.risk.governance.RiskListEntryView;
import org.chovy.canvas.domain.risk.governance.RiskListService;
import org.chovy.canvas.domain.risk.governance.RiskStrategyCommand;
import org.chovy.canvas.domain.risk.governance.RiskStrategyService;
import org.chovy.canvas.domain.risk.runtime.RiskActiveStrategyReader;
import org.chovy.canvas.domain.risk.runtime.RiskCompiledStrategy;
import org.chovy.canvas.domain.risk.runtime.RiskDecisionLedger;
import org.chovy.canvas.domain.risk.runtime.RiskDecisionMerger;
import org.chovy.canvas.domain.risk.runtime.RiskDecisionRequest;
import org.chovy.canvas.domain.risk.runtime.RiskDecisionResponse;
import org.chovy.canvas.domain.risk.runtime.RiskDecisionRunRecord;
import org.chovy.canvas.domain.risk.runtime.RiskDecisionRuleHit;
import org.chovy.canvas.domain.risk.runtime.RiskFailPolicy;
import org.chovy.canvas.domain.risk.runtime.RiskListType;
import org.chovy.canvas.domain.risk.runtime.RiskResolvedValue;
import org.chovy.canvas.domain.risk.runtime.RiskRuleEvaluator;
import org.chovy.canvas.web.risk.RiskStrategyAuditSink;
import org.chovy.canvas.web.risk.RiskStrategyRuntimeCache;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class RiskProductionReadinessAuditTest {

    @Test
    void foundationSchemaTenantScopesEveryRiskTableAndRequiresCompiledHash() throws Exception {
        String sql = migrationSql();

        for (String table : List.of("risk_scene", "risk_strategy", "risk_strategy_version",
                "risk_list", "risk_list_entry", "risk_decision_run", "risk_rule_hit")) {
            String definition = tableDefinition(sql, table);
            assertThat(definition).contains("tenant_id BIGINT NOT NULL");
        }
        assertThat(tableDefinition(sql, "risk_strategy_version")).contains("compiled_hash VARCHAR(128) NOT NULL");
    }

    @Test
    void listEntriesPersistOnlyHashAndMaskedSubjectEvidence() {
        RiskListService service = new RiskListService((tenantId, eventType, resourceKey, resourceId, actor) -> "audit-1",
                rawSubject -> "hash:" + rawSubject,
                Clock.fixed(Instant.parse("2026-06-08T10:00:00Z"), ZoneOffset.UTC));
        service.createList(7L, new RiskListCommand("blacklist.email", RiskListType.BLACK,
                RiskSubjectType.EMAIL, true), "alice");

        RiskListEntryView entry = service.addEntry(7L, "blacklist.email",
                new RiskListEntryCommand("user@example.com", RiskSubjectType.EMAIL, "fraud", "ops",
                        Instant.parse("2026-06-01T00:00:00Z"), null),
                "alice");

        assertThat(entry.subjectHash()).isEqualTo("hash:user@example.com");
        assertThat(entry.subjectMasked()).isEqualTo("u***@example.com");
        assertThat(entry.toString()).doesNotContain("user@example.com");
    }

    @Test
    void decisionTraceMasksRawPiiAcrossSubjectEventContextAndFeatures() {
        FakeLedger ledger = new FakeLedger();
        RiskDecisionServiceHarness service = new RiskDecisionServiceHarness(ledger);

        service.evaluate(new RiskDecisionRequest(
                7L,
                "req-pii",
                "payment",
                Instant.parse("2026-06-08T10:00:00Z"),
                orderedMap("email", "event@example.com", "phone", "+15550001111", "amount", 199),
                orderedMap("userId", "user-123", "email", "subject@example.com", "phone", "+15551234567"),
                orderedMap("operatorEmail", "operator@example.com", "ip", "10.0.0.1"),
                orderedMap("risk.email", "feature@example.com", "risk.phone", "+15552223333"),
                50));

        String snapshot = ledger.savedRuns.getFirst().inputSnapshotJson();
        assertThat(snapshot)
                .doesNotContain("event@example.com")
                .doesNotContain("+15550001111")
                .doesNotContain("subject@example.com")
                .doesNotContain("+15551234567")
                .doesNotContain("operator@example.com")
                .doesNotContain("feature@example.com")
                .doesNotContain("+15552223333")
                .contains("***4567")
                .contains("***1111")
                .contains("***3333");
    }

    @Test
    void highRiskActivationRequiresApproval() {
        RiskStrategyService service = new RiskStrategyService(noopStrategyAuditSink(), noopRuntimeCache());
        service.createDraft(7L, new RiskStrategyCommand("payment", "payment-risk", "Payment risk",
                "HIGH", "{\"rules\":[{\"action\":\"ALLOW\"}]}"), "alice");
        service.validate(7L, "payment-risk", 1, "alice");
        service.markSimulated(7L, "payment-risk", 1, "alice");

        assertThatExceptionOfType(IllegalStateException.class)
                .isThrownBy(() -> service.activate(7L, "payment-risk", 1, "alice"))
                .withMessageContaining("approval");
    }

    private String migrationSql() throws Exception {
        InputStream resource = getClass().getResourceAsStream("/db/migration/V357__risk_control_rule_engine_foundation.sql");
        assertThat(resource).isNotNull();
        return new String(resource.readAllBytes(), StandardCharsets.UTF_8);
    }

    private String tableDefinition(String sql, String tableName) {
        int start = sql.indexOf("CREATE TABLE IF NOT EXISTS " + tableName);
        assertThat(start).isGreaterThanOrEqualTo(0);
        int end = sql.indexOf(";\n", start);
        return sql.substring(start, end);
    }

    private Map<String, Object> orderedMap(Object... pairs) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            map.put((String) pairs[i], pairs[i + 1]);
        }
        return map;
    }

    private RiskStrategyAuditSink noopStrategyAuditSink() {
        return (tenantId, eventType, strategyKey, version, actor) -> {
        };
    }

    private RiskStrategyRuntimeCache noopRuntimeCache() {
        return (tenantId, strategyKey) -> {
        };
    }

    private static final class RiskDecisionServiceHarness {
        private final org.chovy.canvas.domain.risk.runtime.RiskDecisionService service;

        private RiskDecisionServiceHarness(FakeLedger ledger) {
            this.service = new org.chovy.canvas.domain.risk.runtime.RiskDecisionService(
                    (tenantId, sceneKey) -> new RiskCompiledStrategy(sceneKey, "payment-risk", 1,
                            RiskRuntimeMode.ENFORCE, RiskFailPolicy.FAIL_REVIEW, List.of(), List.of()),
                    ledger,
                    (request, operand) -> RiskResolvedValue.missing(),
                    new RiskRuleEvaluator(),
                    new RiskDecisionMerger(),
                    Clock.fixed(Instant.parse("2026-06-08T10:00:00Z"), ZoneOffset.UTC));
        }

        private RiskDecisionResponse evaluate(RiskDecisionRequest request) {
            return service.evaluate(request);
        }
    }

    private static final class FakeLedger implements RiskDecisionLedger {
        private final List<RiskDecisionRunRecord> savedRuns = new ArrayList<>();

        @Override
        public Optional<RiskDecisionRunRecord> findByRequest(Long tenantId, String requestId) {
            return savedRuns.stream()
                    .filter(run -> run.tenantId().equals(tenantId) && run.requestId().equals(requestId))
                    .findFirst();
        }

        @Override
        public RiskDecisionRunRecord saveRun(RiskDecisionRunRecord run) {
            RiskDecisionRunRecord saved = run.withDecisionRunId("run-" + (savedRuns.size() + 1));
            savedRuns.add(saved);
            return saved;
        }

        @Override
        public void saveRuleHits(String decisionRunId, List<RiskDecisionRuleHit> hits) {
        }
    }
}
