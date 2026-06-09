# Product Led Growth And Community Evidence Plan

Status: Historical plan evidence records implementation and verification; commit and merge status was not verified in this docs-only audit.

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a product-led growth and community evidence gate so trial journeys, activation milestones, proficiency levels, referrals, public examples, case studies, community templates, and customer story loops cannot proceed without metric, consent, risk, proof, and rollback evidence.

**Architecture:** Implement an additive evidence registry and domain service. This slice records growth governance decisions only; it does not launch onboarding, referral, public gallery, community publishing, customer-story, or template workflows.

**Tech Stack:** Java 21, Spring Boot project layout, Flyway, JUnit 5, AssertJ, Mockito.

---

## Spec Reference

- `docs/product-evolution/specs/p3-012-product-led-growth-and-community.md`
- Source item: `docs/product-evolution/todo/p3/strategic-opportunities-from-filtered-scope.md#product-led-growth-and-community`

## File Structure

**Backend**
- Create: `backend/canvas-engine/src/main/resources/db/migration/V184__product_led_growth_evidence.sql`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/strategy/growth/ProductLedGrowthEvidenceService.java`

**Tests**
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/strategy/growth/ProductLedGrowthEvidenceServiceTest.java`

**Documentation**
- Modify: `docs/product-evolution/specs/p3-012-product-led-growth-and-community.md`
- Modify: `docs/product-evolution/plans/p3-012-product-led-growth-and-community-plan.md`

**No Frontend**
- No frontend files are part of this slice because growth and community surfaces require accepted activation, consent, and risk evidence first.

### Task 1: Evidence Contract Tests

**Files:**
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/strategy/growth/ProductLedGrowthEvidenceServiceTest.java`

- [x] **Step 1: Write migration and service tests**

Create `ProductLedGrowthEvidenceServiceTest`:

```java
package org.chovy.canvas.strategy.growth;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ProductLedGrowthEvidenceServiceTest {

    @Test
    void migrationCreatesProductLedGrowthEvidenceGate() throws Exception {
        String sql = Files.readString(Path.of(
                "src/main/resources/db/migration/V184__product_led_growth_evidence.sql"));

        assertThat(sql)
                .contains("CREATE TABLE IF NOT EXISTS product_led_growth_evidence")
                .contains("opportunity_key VARCHAR(128) NOT NULL")
                .contains("funnel_stage VARCHAR(64) NOT NULL")
                .contains("activation_metric VARCHAR(255) NOT NULL")
                .contains("consent_requirement TEXT NOT NULL")
                .contains("content_risk_notes TEXT NOT NULL")
                .contains("proof_command VARCHAR(1000) NOT NULL")
                .contains("rollback_note VARCHAR(1000) NOT NULL")
                .contains("decision_status VARCHAR(32) NOT NULL");
    }

    @Test
    void registerRejectsMissingMetricConsentRiskOrRollback() {
        ProductLedGrowthEvidenceService.EvidenceRepository repository = mock(ProductLedGrowthEvidenceService.EvidenceRepository.class);
        ProductLedGrowthEvidenceService service = new ProductLedGrowthEvidenceService(repository);

        assertThatThrownBy(() -> service.register(new ProductLedGrowthEvidenceService.EvidenceRequest(
                "public-template-gallery", "growth-1", "activation", "marketing operator",
                "", "explicit publisher consent", "public content review",
                "increase activated workspaces", "npm test", "hide gallery route")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("activation metric is required");

        assertThatThrownBy(() -> service.register(new ProductLedGrowthEvidenceService.EvidenceRequest(
                "customer-story-loop", "growth-1", "expansion", "customer success",
                "case study opt-in rate", "", "public content review",
                "increase qualified references", "npm test", "hide story route")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("consent requirement is required");
    }

    @Test
    void registerStoresBlockedDecisionUntilGrowthReview() {
        ProductLedGrowthEvidenceService.EvidenceRepository repository = mock(ProductLedGrowthEvidenceService.EvidenceRepository.class);
        ProductLedGrowthEvidenceService service = new ProductLedGrowthEvidenceService(repository);

        service.register(new ProductLedGrowthEvidenceService.EvidenceRequest(
                "referral-invite", "growth-1", "activation", "workspace admin",
                "invited workspace activation rate", "recipient consent and anti-spam review",
                "low risk with rate limits", "increase activated referred workspaces",
                "cd frontend && npm test -- --run", "disable product_led_growth.registry.enabled"));

        verify(repository).insert(argThat(record ->
                record.opportunityKey().equals("referral-invite")
                        && record.decisionStatus().equals("BLOCKED_PENDING_REVIEW")
                        && record.activationMetric().contains("activation rate")));
    }
}
```

- [x] **Step 2: Run tests and confirm red state**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=ProductLedGrowthEvidenceServiceTest
```

Expected: FAIL because the migration and service do not exist.

### Task 2: Migration And Service

**Files:**
- Create: `backend/canvas-engine/src/main/resources/db/migration/V184__product_led_growth_evidence.sql`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/strategy/growth/ProductLedGrowthEvidenceService.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/strategy/growth/ProductLedGrowthEvidenceServiceTest.java`

- [x] **Step 1: Add the additive migration**

Create `V184__product_led_growth_evidence.sql`:

```sql
CREATE TABLE IF NOT EXISTS product_led_growth_evidence (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  opportunity_key VARCHAR(128) NOT NULL,
  owner_id VARCHAR(128) NOT NULL,
  funnel_stage VARCHAR(64) NOT NULL,
  target_persona VARCHAR(128) NOT NULL,
  activation_metric VARCHAR(255) NOT NULL,
  consent_requirement TEXT NOT NULL,
  content_risk_notes TEXT NOT NULL,
  experiment_hypothesis TEXT NOT NULL,
  proof_command VARCHAR(1000) NOT NULL,
  rollback_note VARCHAR(1000) NOT NULL,
  decision_status VARCHAR(32) NOT NULL DEFAULT 'BLOCKED_PENDING_REVIEW',
  reviewed_by VARCHAR(128) NULL,
  reviewed_at DATETIME NULL,
  child_spec VARCHAR(255) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_plg_evidence_opportunity_status (opportunity_key, decision_status),
  INDEX idx_plg_evidence_funnel_stage (funnel_stage, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

- [x] **Step 2: Implement the evidence gate**

Create `ProductLedGrowthEvidenceService`:

```java
package org.chovy.canvas.strategy.growth;

public class ProductLedGrowthEvidenceService {

    private final EvidenceRepository repository;

    public ProductLedGrowthEvidenceService(EvidenceRepository repository) {
        this.repository = repository;
    }

    public EvidenceRecord register(EvidenceRequest request) {
        requireText(request.opportunityKey(), "opportunity key is required");
        requireText(request.ownerId(), "owner is required");
        requireText(request.funnelStage(), "funnel stage is required");
        requireText(request.targetPersona(), "target persona is required");
        requireText(request.activationMetric(), "activation metric is required");
        requireText(request.consentRequirement(), "consent requirement is required");
        requireText(request.contentRiskNotes(), "content risk notes are required");
        requireText(request.experimentHypothesis(), "experiment hypothesis is required");
        requireText(request.proofCommand(), "proof command is required");
        requireText(request.rollbackNote(), "rollback note is required");

        EvidenceRecord record = new EvidenceRecord(
                request.opportunityKey(), request.ownerId(), request.funnelStage(),
                request.targetPersona(), request.activationMetric(), request.consentRequirement(),
                request.contentRiskNotes(), request.experimentHypothesis(), request.proofCommand(),
                request.rollbackNote(), "BLOCKED_PENDING_REVIEW");
        repository.insert(record);
        return record;
    }

    public void approve(String opportunityKey, String reviewerId, String childSpec) {
        requireText(opportunityKey, "opportunity key is required");
        requireText(reviewerId, "reviewer is required");
        requireText(childSpec, "child spec is required");
        repository.approve(opportunityKey, reviewerId, childSpec);
    }

    private static void requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
    }

    public record EvidenceRequest(
            String opportunityKey,
            String ownerId,
            String funnelStage,
            String targetPersona,
            String activationMetric,
            String consentRequirement,
            String contentRiskNotes,
            String experimentHypothesis,
            String proofCommand,
            String rollbackNote) {}

    public record EvidenceRecord(
            String opportunityKey,
            String ownerId,
            String funnelStage,
            String targetPersona,
            String activationMetric,
            String consentRequirement,
            String contentRiskNotes,
            String experimentHypothesis,
            String proofCommand,
            String rollbackNote,
            String decisionStatus) {}

    public interface EvidenceRepository {
        void insert(EvidenceRecord record);
        void approve(String opportunityKey, String reviewerId, String childSpec);
    }
}
```

- [x] **Step 3: Run focused tests**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=ProductLedGrowthEvidenceServiceTest
```

Expected: PASS for migration shape and service gate behavior.

### Task 3: Experiment Approval Gate

**Files:**
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/strategy/growth/ProductLedGrowthEvidenceService.java`
- Modify: `backend/canvas-engine/src/test/java/org/chovy/canvas/strategy/growth/ProductLedGrowthEvidenceServiceTest.java`

- [x] **Step 1: Add approval test**

Add this test:

```java
@Test
void approvalRequiresReviewerAndChildSpec() {
    ProductLedGrowthEvidenceService.EvidenceRepository repository = mock(ProductLedGrowthEvidenceService.EvidenceRepository.class);
    ProductLedGrowthEvidenceService service = new ProductLedGrowthEvidenceService(repository);

    assertThatThrownBy(() -> service.approve("referral-invite", "", "docs/product-evolution/specs/p3-012a-referral-invite.md"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("reviewer is required");

    assertThatThrownBy(() -> service.approve("referral-invite", "growth-lead-1", ""))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("child spec is required");

    service.approve("referral-invite", "growth-lead-1", "docs/product-evolution/specs/p3-012a-referral-invite.md");

    verify(repository).approve("referral-invite", "growth-lead-1", "docs/product-evolution/specs/p3-012a-referral-invite.md");
}
```

- [x] **Step 2: Implement approval method**

Add this method to `ProductLedGrowthEvidenceService` and add the matching method to `EvidenceRepository`:

```java
public void approve(String opportunityKey, String reviewerId, String childSpec) {
    requireText(opportunityKey, "opportunity key is required");
    requireText(reviewerId, "reviewer is required");
    requireText(childSpec, "child spec is required");
    repository.approve(opportunityKey, reviewerId, childSpec);
}

public interface EvidenceRepository {
    void insert(EvidenceRecord record);
    void approve(String opportunityKey, String reviewerId, String childSpec);
}
```

- [x] **Step 3: Run focused tests**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=ProductLedGrowthEvidenceServiceTest
```

Expected: PASS with registration and approval gate coverage.

### Task 4: Verification, Rollout Notes, And Commit

**Files:**
- Modify: `docs/product-evolution/specs/p3-012-product-led-growth-and-community.md`
- Modify: `docs/product-evolution/plans/p3-012-product-led-growth-and-community-plan.md`

- [x] **Step 1: Run focused verification**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=ProductLedGrowthEvidenceServiceTest
```

Expected: PASS.

- [x] **Step 2: Run migration naming check**

Run:

```bash
test -f backend/canvas-engine/src/main/resources/db/migration/V184__product_led_growth_evidence.sql
```

Expected: command exits 0.

- [x] **Step 3: Rollout notes**

Rollout: run `V184__product_led_growth_evidence.sql`, then allow growth owners to register opportunity evidence. Keep public, referral, case-study, and community workflows unavailable until reviewed child specs exist. Rollback: disable evidence registration or hide the admin entry point; no runtime growth workflow depends on this additive table.

Commit boundary: no commit was created in this docs-only audit; commit and merge status remains unverified.

Run:

```bash
git add backend/canvas-engine/src/main/resources/db/migration/V184__product_led_growth_evidence.sql \
  backend/canvas-engine/src/main/java/org/chovy/canvas/strategy/growth/ProductLedGrowthEvidenceService.java \
  backend/canvas-engine/src/test/java/org/chovy/canvas/strategy/growth/ProductLedGrowthEvidenceServiceTest.java \
  docs/product-evolution/specs/p3-012-product-led-growth-and-community.md \
  docs/product-evolution/plans/p3-012-product-led-growth-and-community-plan.md
git commit -m "docs: add product led growth evidence gate"
```

Expected: commit contains only the PLG evidence migration, service, test, spec, and plan.
