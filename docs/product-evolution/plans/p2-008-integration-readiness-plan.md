# Integration Readiness Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Package the first integration readiness workflow: tenant-scoped integration API keys with permissions, masked display, revoke behavior, and readiness summary.

**Architecture:** Store API key metadata in an additive table and keep secret material out of API responses by hashing the raw token and returning only a one-time plaintext value on creation. Inbound/outbound webhook delivery, SSO/OIDC configuration, and data sync execution remain child specs after the API-key foundation is verified.

**Tech Stack:** Java 21, Spring Boot WebFlux-style `Mono`, Flyway, JUnit 5, Mockito, AssertJ, React 18, TypeScript, Axios, Vitest.

---

## Spec Reference

- `docs/product-evolution/specs/p2-008-integration-readiness.md`
- Source item: `docs/product-evolution/todo/p2/product-opportunities-from-filtered-scope.md#integration-readiness`

## File Structure

**Backend**
- Create: `backend/canvas-engine/src/main/resources/db/migration/V167__integration_readiness.sql` - integration API key metadata table.
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/integration/IntegrationService.java` - create/list/revoke/readiness logic.
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/IntegrationController.java` - `/integrations`.
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/integration/IntegrationServiceTest.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/controller/IntegrationControllerTest.java`

**Frontend**
- Create: `frontend/src/services/integrationApi.ts`
- Create: `frontend/src/pages/integrations/integrationReadiness.ts`
- Create: `frontend/src/pages/integrations/integrationReadiness.test.ts`

### Task 1: API Key Registry Service

**Files:**
- Create: `backend/canvas-engine/src/main/resources/db/migration/V167__integration_readiness.sql`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/integration/IntegrationService.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/integration/IntegrationServiceTest.java`

- [ ] **Step 1: Write integration service tests**

Create `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/integration/IntegrationServiceTest.java`:

```java
package org.chovy.canvas.domain.integration;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class IntegrationServiceTest {

    @Test
    void migrationCreatesTenantScopedApiKeyTableWithoutPlainSecret() throws Exception {
        String sql = Files.readString(Path.of(
                "src/main/resources/db/migration/V167__integration_readiness.sql"));

        assertThat(sql)
                .contains("CREATE TABLE IF NOT EXISTS integration_api_key")
                .contains("tenant_id BIGINT NOT NULL")
                .contains("key_prefix VARCHAR(32) NOT NULL")
                .contains("secret_hash VARCHAR(128) NOT NULL")
                .contains("permissions_json JSON NOT NULL")
                .contains("revoked_at DATETIME NULL")
                .doesNotContain("plain_secret");
    }

    @Test
    void createApiKeyHashesSecretAndReturnsPlaintextOnlyOnce() {
        IntegrationService.ApiKeyRepository repository = mock(IntegrationService.ApiKeyRepository.class);
        IntegrationService.SecretGenerator generator = mock(IntegrationService.SecretGenerator.class);
        IntegrationService service = new IntegrationService(repository, generator);
        when(generator.generate()).thenReturn("ck_live_abcdef123456");
        when(generator.sha256("ck_live_abcdef123456")).thenReturn("hash-1");

        IntegrationService.CreatedApiKey created = service.createApiKey(8L, "Webhook Partner", Set.of("WEBHOOK_READ", "EVENT_WRITE"), "operator-1");

        assertThat(created.plaintext()).isEqualTo("ck_live_abcdef123456");
        assertThat(created.record().maskedKey()).isEqualTo("ck_live...3456");
        verify(repository).insert(argThat(record ->
                record.tenantId().equals(8L)
                        && record.name().equals("Webhook Partner")
                        && record.secretHash().equals("hash-1")
                        && record.permissions().contains("EVENT_WRITE")));
    }

    @Test
    void createApiKeyRejectsUnsupportedPermission() {
        IntegrationService.ApiKeyRepository repository = mock(IntegrationService.ApiKeyRepository.class);
        IntegrationService.SecretGenerator generator = mock(IntegrationService.SecretGenerator.class);
        IntegrationService service = new IntegrationService(repository, generator);

        assertThatThrownBy(() -> service.createApiKey(8L, "Bad", Set.of("ROOT"), "operator-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("unsupported integration permission ROOT");
    }

    @Test
    void revokeDelegatesTenantScopedKey() {
        IntegrationService.ApiKeyRepository repository = mock(IntegrationService.ApiKeyRepository.class);
        IntegrationService.SecretGenerator generator = mock(IntegrationService.SecretGenerator.class);
        IntegrationService service = new IntegrationService(repository, generator);

        service.revoke(8L, 10L, "operator-1");

        verify(repository).revoke(8L, 10L, "operator-1");
    }

    @Test
    void readinessSummaryCountsActiveKeysAndWebhookGaps() {
        IntegrationService.ApiKeyRepository repository = mock(IntegrationService.ApiKeyRepository.class);
        IntegrationService.SecretGenerator generator = mock(IntegrationService.SecretGenerator.class);
        IntegrationService service = new IntegrationService(repository, generator);
        when(repository.activeKeys(8L)).thenReturn(List.of(new IntegrationService.ApiKeyRecord(
                10L, 8L, "Webhook Partner", "ck_live", "hash-1", "ck_live...3456", Set.of("EVENT_WRITE"), "operator-1", Instant.parse("2026-06-03T00:00:00Z"), null)));

        IntegrationService.ReadinessSummary summary = service.readiness(8L);

        assertThat(summary.activeApiKeyCount()).isEqualTo(1);
        assertThat(summary.webhookSigningReady()).isFalse();
        assertThat(summary.nextRequiredSpec()).isEqualTo("p1-005b-webhook-subscription-schema-and-signing");
    }
}
```

- [ ] **Step 2: Run service tests and confirm red state**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=IntegrationServiceTest
```

Expected: FAIL because the migration and service do not exist.

- [ ] **Step 3: Add integration migration**

Create `backend/canvas-engine/src/main/resources/db/migration/V167__integration_readiness.sql`:

```sql
CREATE TABLE IF NOT EXISTS integration_api_key (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  name VARCHAR(128) NOT NULL,
  key_prefix VARCHAR(32) NOT NULL,
  secret_hash VARCHAR(128) NOT NULL,
  masked_key VARCHAR(64) NOT NULL,
  permissions_json JSON NOT NULL,
  created_by VARCHAR(128) NOT NULL,
  revoked_by VARCHAR(128) NULL,
  revoked_at DATETIME NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_integration_api_key_hash (secret_hash),
  INDEX idx_integration_api_key_tenant (tenant_id, revoked_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

- [ ] **Step 4: Implement integration service**

Create `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/integration/IntegrationService.java`:

```java
package org.chovy.canvas.domain.integration;

import java.time.Instant;
import java.util.List;
import java.util.Set;

public class IntegrationService {

    private static final Set<String> SUPPORTED_PERMISSIONS = Set.of("WEBHOOK_READ", "WEBHOOK_WRITE", "EVENT_WRITE", "DATA_SOURCE_READ");

    private final ApiKeyRepository repository;
    private final SecretGenerator secretGenerator;

    public IntegrationService(ApiKeyRepository repository, SecretGenerator secretGenerator) {
        this.repository = repository;
        this.secretGenerator = secretGenerator;
    }

    public CreatedApiKey createApiKey(Long tenantId, String name, Set<String> permissions, String operator) {
        for (String permission : permissions) {
            if (!SUPPORTED_PERMISSIONS.contains(permission)) {
                throw new IllegalArgumentException("unsupported integration permission " + permission);
            }
        }
        String plaintext = secretGenerator.generate();
        String prefix = plaintext.substring(0, Math.min(7, plaintext.length()));
        String masked = prefix + "..." + plaintext.substring(Math.max(0, plaintext.length() - 4));
        ApiKeyRecord record = new ApiKeyRecord(
                null,
                tenantId,
                name,
                prefix,
                secretGenerator.sha256(plaintext),
                masked,
                permissions,
                operator,
                Instant.now(),
                null);
        repository.insert(record);
        return new CreatedApiKey(record, plaintext);
    }

    public List<ApiKeyRecord> activeKeys(Long tenantId) {
        return repository.activeKeys(tenantId);
    }

    public void revoke(Long tenantId, Long keyId, String operator) {
        repository.revoke(tenantId, keyId, operator);
    }

    public ReadinessSummary readiness(Long tenantId) {
        int activeCount = repository.activeKeys(tenantId).size();
        return new ReadinessSummary(activeCount, false, "p1-005b-webhook-subscription-schema-and-signing");
    }

    public record ApiKeyRecord(Long id, Long tenantId, String name, String keyPrefix, String secretHash, String maskedKey, Set<String> permissions, String createdBy, Instant createdAt, Instant revokedAt) {}

    public record CreatedApiKey(ApiKeyRecord record, String plaintext) {}

    public record ReadinessSummary(int activeApiKeyCount, boolean webhookSigningReady, String nextRequiredSpec) {}

    public interface ApiKeyRepository {
        void insert(ApiKeyRecord record);
        List<ApiKeyRecord> activeKeys(Long tenantId);
        void revoke(Long tenantId, Long keyId, String operator);
    }

    public interface SecretGenerator {
        String generate();
        String sha256(String plaintext);
    }
}
```

- [ ] **Step 5: Run service tests**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=IntegrationServiceTest
```

Expected: PASS.

### Task 2: Integration API

**Files:**
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/IntegrationController.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/controller/IntegrationControllerTest.java`

- [ ] **Step 1: Write controller tests**

Create `backend/canvas-engine/src/test/java/org/chovy/canvas/controller/IntegrationControllerTest.java`:

```java
package org.chovy.canvas.web;

import org.chovy.canvas.common.tenant.RoleNames;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.integration.IntegrationService;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class IntegrationControllerTest {

    @Test
    void createApiKeyUsesTenantAndUsername() {
        IntegrationService service = mock(IntegrationService.class);
        TenantContextResolver resolver = mock(TenantContextResolver.class);
        when(resolver.current()).thenReturn(Mono.just(operator()));
        IntegrationController.CreateApiKeyRequest request = new IntegrationController.CreateApiKeyRequest(
                "Webhook Partner", Set.of("EVENT_WRITE"));
        IntegrationService.ApiKeyRecord record = new IntegrationService.ApiKeyRecord(
                10L, 8L, "Webhook Partner", "ck_live", "hash", "ck_live...3456", Set.of("EVENT_WRITE"),
                "operator-1", Instant.parse("2026-06-03T00:00:00Z"), null);
        when(service.createApiKey(8L, "Webhook Partner", Set.of("EVENT_WRITE"), "operator-1"))
                .thenReturn(new IntegrationService.CreatedApiKey(record, "ck_live_abcdef123456"));
        IntegrationController controller = new IntegrationController(service, resolver);

        StepVerifier.create(controller.createApiKey(request))
                .assertNext(response -> {
                    assertThat(response.getData().plaintext()).isEqualTo("ck_live_abcdef123456");
                    assertThat(response.getData().record().secretHash()).isEqualTo("hash");
                })
                .verifyComplete();
    }

    @Test
    void listApiKeysReturnsOnlyServiceDataForCurrentTenant() {
        IntegrationService service = mock(IntegrationService.class);
        TenantContextResolver resolver = mock(TenantContextResolver.class);
        when(resolver.current()).thenReturn(Mono.just(operator()));
        when(service.activeKeys(8L)).thenReturn(List.of());
        IntegrationController controller = new IntegrationController(service, resolver);

        StepVerifier.create(controller.activeKeys())
                .assertNext(response -> assertThat(response.getData()).isEmpty())
                .verifyComplete();
    }

    @Test
    void revokeUsesTenantAndOperator() {
        IntegrationService service = mock(IntegrationService.class);
        TenantContextResolver resolver = mock(TenantContextResolver.class);
        when(resolver.current()).thenReturn(Mono.just(operator()));
        IntegrationController controller = new IntegrationController(service, resolver);

        StepVerifier.create(controller.revoke(10L))
                .assertNext(response -> assertThat(response.getCode()).isEqualTo(0))
                .verifyComplete();

        verify(service).revoke(8L, 10L, "operator-1");
    }

    private TenantContext operator() {
        return new TenantContext(8L, RoleNames.OPERATOR, "operator-1");
    }
}
```

- [ ] **Step 2: Run controller tests and confirm red state**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=IntegrationControllerTest
```

Expected: FAIL because `IntegrationController` does not exist.

- [ ] **Step 3: Add controller**

Create `backend/canvas-engine/src/main/java/org/chovy/canvas/web/IntegrationController.java`:

```java
package org.chovy.canvas.web;

import lombok.RequiredArgsConstructor;
import org.chovy.canvas.common.R;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.integration.IntegrationService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/integrations")
@RequiredArgsConstructor
public class IntegrationController {

    private final IntegrationService service;
    private final TenantContextResolver tenantContextResolver;

    @PostMapping("/api-keys")
    public Mono<R<IntegrationService.CreatedApiKey>> createApiKey(@RequestBody CreateApiKeyRequest request) {
        return tenantContextResolver.current()
                .flatMap(context -> Mono.fromCallable(() -> service.createApiKey(
                                context.tenantId(), request.name(), request.permissions(), context.username()))
                        .subscribeOn(Schedulers.boundedElastic()))
                .map(R::ok);
    }

    @GetMapping("/api-keys")
    public Mono<R<List<IntegrationService.ApiKeyRecord>>> activeKeys() {
        return tenantContextResolver.current()
                .flatMap(context -> Mono.fromCallable(() -> service.activeKeys(context.tenantId()))
                        .subscribeOn(Schedulers.boundedElastic()))
                .map(R::ok);
    }

    @DeleteMapping("/api-keys/{keyId}")
    public Mono<R<Void>> revoke(@PathVariable Long keyId) {
        return tenantContextResolver.current()
                .flatMap(context -> Mono.<Void>fromRunnable(() -> service.revoke(context.tenantId(), keyId, context.username()))
                        .subscribeOn(Schedulers.boundedElastic())
                        .thenReturn(R.ok()));
    }

    @GetMapping("/readiness")
    public Mono<R<IntegrationService.ReadinessSummary>> readiness() {
        return tenantContextResolver.current()
                .flatMap(context -> Mono.fromCallable(() -> service.readiness(context.tenantId()))
                        .subscribeOn(Schedulers.boundedElastic()))
                .map(R::ok);
    }

    public record CreateApiKeyRequest(String name, Set<String> permissions) {}
}
```

- [ ] **Step 4: Run controller tests**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=IntegrationControllerTest
```

Expected: PASS.

### Task 3: Frontend Integration Helpers

**Files:**
- Create: `frontend/src/services/integrationApi.ts`
- Create: `frontend/src/pages/integrations/integrationReadiness.ts`
- Create: `frontend/src/pages/integrations/integrationReadiness.test.ts`

- [ ] **Step 1: Write frontend tests**

Create `frontend/src/pages/integrations/integrationReadiness.test.ts`:

```ts
import { describe, expect, it } from 'vitest'
import { apiKeyStatusText, maskedKeyWarning, readinessBadge, type IntegrationReadinessSummary } from './integrationReadiness'

describe('integrationReadiness helpers', () => {
  it('formats readiness badge from summary', () => {
    const summary: IntegrationReadinessSummary = {
      activeApiKeyCount: 2,
      webhookSigningReady: false,
      nextRequiredSpec: 'p1-005b-webhook-subscription-schema-and-signing',
    }

    expect(readinessBadge(summary)).toEqual({
      status: 'PARTIAL',
      text: '2 active keys, webhook signing pending p1-005b-webhook-subscription-schema-and-signing',
    })
  })

  it('formats key status and secret warnings', () => {
    expect(apiKeyStatusText(null)).toBe('Active')
    expect(apiKeyStatusText('2026-06-03T00:00:00Z')).toBe('Revoked')
    expect(maskedKeyWarning('ck_live...3456')).toBe('Store the plaintext now. Later views only show ck_live...3456.')
  })
})
```

- [ ] **Step 2: Run frontend tests and confirm red state**

Run:

```bash
cd frontend && npm test -- integrationReadiness.test.ts
```

Expected: FAIL because `integrationReadiness.ts` does not exist.

- [ ] **Step 3: Add API wrapper**

Create `frontend/src/services/integrationApi.ts`:

```ts
import http from './api'
import type { R } from '../types'
import type { CreatedIntegrationApiKey, IntegrationApiKey, IntegrationReadinessSummary } from '../pages/integrations/integrationReadiness'

export const integrationApi = {
  createApiKey: (payload: { name: string; permissions: string[] }) =>
    http.post<R<CreatedIntegrationApiKey>, R<CreatedIntegrationApiKey>>('/integrations/api-keys', payload),
  activeApiKeys: () =>
    http.get<R<IntegrationApiKey[]>, R<IntegrationApiKey[]>>('/integrations/api-keys'),
  revokeApiKey: (keyId: number) =>
    http.delete<R<void>, R<void>>(`/integrations/api-keys/${keyId}`),
  readiness: () =>
    http.get<R<IntegrationReadinessSummary>, R<IntegrationReadinessSummary>>('/integrations/readiness'),
}
```

- [ ] **Step 4: Add frontend helpers**

Create `frontend/src/pages/integrations/integrationReadiness.ts`:

```ts
export interface IntegrationApiKey {
  id: number
  tenantId: number
  name: string
  keyPrefix: string
  maskedKey: string
  permissions: string[]
  createdBy: string
  createdAt: string
  revokedAt: string | null
}

export interface CreatedIntegrationApiKey {
  record: IntegrationApiKey
  plaintext: string
}

export interface IntegrationReadinessSummary {
  activeApiKeyCount: number
  webhookSigningReady: boolean
  nextRequiredSpec: string
}

export function readinessBadge(summary: IntegrationReadinessSummary) {
  if (summary.activeApiKeyCount > 0 && summary.webhookSigningReady) {
    return { status: 'READY', text: `${summary.activeApiKeyCount} active keys, webhook signing ready` }
  }
  return {
    status: 'PARTIAL',
    text: `${summary.activeApiKeyCount} active keys, webhook signing pending ${summary.nextRequiredSpec}`,
  }
}

export function apiKeyStatusText(revokedAt: string | null) {
  return revokedAt ? 'Revoked' : 'Active'
}

export function maskedKeyWarning(maskedKey: string) {
  return `Store the plaintext now. Later views only show ${maskedKey}.`
}
```

- [ ] **Step 5: Run frontend tests**

Run:

```bash
cd frontend && npm test -- integrationReadiness.test.ts
```

Expected: PASS.

### Task 4: Verification And Commit

**Files:**
- Modify: `docs/product-evolution/specs/p2-008-integration-readiness.md`
- Modify: `docs/product-evolution/plans/p2-008-integration-readiness-plan.md`
- Create: `backend/canvas-engine/src/main/resources/db/migration/V167__integration_readiness.sql`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/integration/IntegrationService.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/IntegrationController.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/integration/IntegrationServiceTest.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/controller/IntegrationControllerTest.java`
- Create: `frontend/src/services/integrationApi.ts`
- Create: `frontend/src/pages/integrations/integrationReadiness.ts`
- Create: `frontend/src/pages/integrations/integrationReadiness.test.ts`

- [ ] **Step 1: Run focused backend tests**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=IntegrationServiceTest,IntegrationControllerTest
```

Expected: PASS.

- [ ] **Step 2: Run focused frontend tests**

Run:

```bash
cd frontend && npm test -- integrationReadiness.test.ts
```

Expected: PASS.

- [ ] **Step 3: Add rollout notes to the implementation PR**

Use this rollout note text:

```markdown
Rollout: run `V167__integration_readiness.sql`, then expose API key create/list/revoke and readiness summary to tenant admins. Plaintext API keys are returned only once. Rollback: hide the integrations route and revoke newly created integration keys.
```

- [ ] **Step 4: Commit the implementation slice**

Run:

```bash
git add \
  backend/canvas-engine/src/main/resources/db/migration/V167__integration_readiness.sql \
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/integration/IntegrationService.java \
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/IntegrationController.java \
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/integration/IntegrationServiceTest.java \
  backend/canvas-engine/src/test/java/org/chovy/canvas/controller/IntegrationControllerTest.java \
  frontend/src/services/integrationApi.ts \
  frontend/src/pages/integrations/integrationReadiness.ts \
  frontend/src/pages/integrations/integrationReadiness.test.ts \
  docs/product-evolution/specs/p2-008-integration-readiness.md \
  docs/product-evolution/plans/p2-008-integration-readiness-plan.md
git commit -m "feat: add integration readiness api key foundation"
```

Expected: commit contains only integration API-key foundation, readiness helpers, tests, spec, and plan files.
