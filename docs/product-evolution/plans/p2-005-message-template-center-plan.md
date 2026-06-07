# Message Template Center Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the first message template center workflow: tenant-scoped template creation, search, variable extraction, and preview rendering.

**Architecture:** Store templates in an additive table and keep rendering in a small domain service that extracts `{{variable}}` placeholders and previews against supplied context. Approval integration and channel adaptation beyond a simple channel field are deferred to child specs after this CRUD/preview foundation is verified.

**Tech Stack:** Java 21, Spring Boot WebFlux-style `Mono`, Flyway, JUnit 5, Mockito, AssertJ, React 18, TypeScript, Axios, Vitest.

**Implementation status (2026-06-05):** Completed. The actual migration is `V268__message_template_center.sql` because this workspace already uses `V267` for the technical migration evidence registry. The backend uses `TenantContextResolver.currentOrError()` for tenant isolation, and the frontend now exposes a visible `/message-templates` page plus menu entry. Commit was intentionally skipped because the user did not request one.

---

## Spec Reference

- `docs/product-evolution/specs/p2-005-message-template-center.md`
- Source item: `docs/product-evolution/todo/p2/product-opportunities-from-filtered-scope.md#message-template-center`

## File Structure

**Backend**
- Create: `backend/canvas-engine/src/main/resources/db/migration/V268__message_template_center.sql` - message template table.
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/template/MessageTemplateService.java` - tenant-scoped create/search/preview logic.
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/template/JdbcMessageTemplateRepository.java` - production persistence adapter.
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/MessageTemplateController.java` - `/message-templates` API.
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/template/MessageTemplateServiceTest.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/controller/MessageTemplateControllerTest.java`

**Frontend**
- Create: `frontend/src/services/messageTemplateApi.ts`
- Create: `frontend/src/services/messageTemplateApi.test.ts`
- Create: `frontend/src/pages/message-templates/messageTemplateCenter.ts`
- Create: `frontend/src/pages/message-templates/messageTemplateCenter.test.ts`
- Create: `frontend/src/pages/message-templates/index.tsx`
- Create: `frontend/src/pages/message-templates/index.test.tsx`
- Modify: `frontend/src/App.tsx`
- Modify: `frontend/src/components/layout/AppLayout.tsx`
- Modify: `frontend/src/components/layout/AppLayout.a11y.test.tsx`
- Modify: `frontend/src/components/accessibility/RouteA11y.tsx`

### Task 1: Template Schema And Rendering Service

**Files:**
- Create: `backend/canvas-engine/src/main/resources/db/migration/V268__message_template_center.sql`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/template/MessageTemplateService.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/template/JdbcMessageTemplateRepository.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/template/MessageTemplateServiceTest.java`

- [x] **Step 1: Write service tests**

Create `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/template/MessageTemplateServiceTest.java`:

```java
package org.chovy.canvas.domain.template;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MessageTemplateServiceTest {

    @Test
    void migrationCreatesTenantScopedTemplateTable() throws Exception {
        String sql = Files.readString(Path.of(
                "src/main/resources/db/migration/V268__message_template_center.sql"));

        assertThat(sql)
                .contains("CREATE TABLE IF NOT EXISTS message_template")
                .contains("tenant_id BIGINT NOT NULL")
                .contains("template_code VARCHAR(128) NOT NULL")
                .contains("channel VARCHAR(64) NOT NULL")
                .contains("body TEXT NOT NULL")
                .contains("variable_schema_json JSON NOT NULL")
                .contains("UNIQUE KEY uk_message_template_code");
    }

    @Test
    void createExtractsVariablesAndPersistsTenantScopedTemplate() {
        MessageTemplateService.TemplateRepository repository = mock(MessageTemplateService.TemplateRepository.class);
        MessageTemplateService service = new MessageTemplateService(repository);

        MessageTemplateService.Template saved = service.create(8L, new MessageTemplateService.TemplateDraft(
                "welcome_sms", "Welcome SMS", "SMS", "Hi {{firstName}}, your level is {{tier}}."));

        assertThat(saved.variables()).containsExactly("firstName", "tier");
        verify(repository).insert(argThat(template ->
                template.tenantId().equals(8L)
                        && template.templateCode().equals("welcome_sms")
                        && template.variables().equals(List.of("firstName", "tier"))));
    }

    @Test
    void searchDelegatesTenantAndKeyword() {
        MessageTemplateService.TemplateRepository repository = mock(MessageTemplateService.TemplateRepository.class);
        MessageTemplateService service = new MessageTemplateService(repository);
        when(repository.search(8L, "welcome", "SMS")).thenReturn(List.of(new MessageTemplateService.Template(
                8L, "welcome_sms", "Welcome SMS", "SMS", "Hi {{firstName}}", List.of("firstName"), "DRAFT")));

        List<MessageTemplateService.Template> result = service.search(8L, "welcome", "SMS");

        assertThat(result).extracting(MessageTemplateService.Template::templateCode).containsExactly("welcome_sms");
    }

    @Test
    void previewReplacesKnownVariablesAndReportsMissingVariables() {
        MessageTemplateService.TemplateRepository repository = mock(MessageTemplateService.TemplateRepository.class);
        MessageTemplateService service = new MessageTemplateService(repository);
        when(repository.get(8L, "welcome_sms")).thenReturn(new MessageTemplateService.Template(
                8L, "welcome_sms", "Welcome SMS", "SMS", "Hi {{firstName}}, use {{couponCode}}.", List.of("firstName", "couponCode"), "DRAFT"));

        MessageTemplateService.PreviewResult result = service.preview(8L, "welcome_sms", Map.of("firstName", "Alice"));

        assertThat(result.renderedBody()).isEqualTo("Hi Alice, use {{couponCode}}.");
        assertThat(result.missingVariables()).containsExactly("couponCode");
    }

    @Test
    void createRejectsUnsupportedChannel() {
        MessageTemplateService.TemplateRepository repository = mock(MessageTemplateService.TemplateRepository.class);
        MessageTemplateService service = new MessageTemplateService(repository);

        assertThatThrownBy(() -> service.create(8L, new MessageTemplateService.TemplateDraft(
                "bad", "Bad", "FAX", "Hi")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("unsupported template channel FAX");
    }
}
```

- [x] **Step 2: Run service tests and confirm red state**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=MessageTemplateServiceTest
```

Expected: FAIL because the migration and service do not exist.

- [x] **Step 3: Add template migration**

Create `backend/canvas-engine/src/main/resources/db/migration/V268__message_template_center.sql`:

```sql
CREATE TABLE IF NOT EXISTS message_template (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  template_code VARCHAR(128) NOT NULL,
  display_name VARCHAR(128) NOT NULL,
  channel VARCHAR(64) NOT NULL,
  body TEXT NOT NULL,
  variable_schema_json JSON NOT NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'DRAFT',
  created_by VARCHAR(128) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_message_template_code (tenant_id, template_code),
  INDEX idx_message_template_search (tenant_id, channel, status, updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

- [x] **Step 4: Implement message template service**

Create `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/template/MessageTemplateService.java`:

```java
package org.chovy.canvas.domain.template;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MessageTemplateService {

    private static final Set<String> SUPPORTED_CHANNELS = Set.of("SMS", "EMAIL", "IN_APP", "WEBHOOK");
    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{\\{\\s*([A-Za-z][A-Za-z0-9_]*)\\s*}}");

    private final TemplateRepository repository;

    public MessageTemplateService(TemplateRepository repository) {
        this.repository = repository;
    }

    public Template create(Long tenantId, TemplateDraft draft) {
        if (!SUPPORTED_CHANNELS.contains(draft.channel())) {
            throw new IllegalArgumentException("unsupported template channel " + draft.channel());
        }
        Template template = new Template(
                tenantId,
                draft.templateCode(),
                draft.displayName(),
                draft.channel(),
                draft.body(),
                extractVariables(draft.body()),
                "DRAFT");
        repository.insert(template);
        return template;
    }

    public List<Template> search(Long tenantId, String keyword, String channel) {
        return repository.search(tenantId, keyword, channel);
    }

    public PreviewResult preview(Long tenantId, String templateCode, Map<String, Object> context) {
        Template template = repository.get(tenantId, templateCode);
        String rendered = template.body();
        List<String> missing = new ArrayList<>();
        for (String variable : template.variables()) {
            Object value = context.get(variable);
            if (value == null) {
                missing.add(variable);
            } else {
                rendered = rendered.replaceAll("\\{\\{\\s*" + Pattern.quote(variable) + "\\s*}}", Matcher.quoteReplacement(String.valueOf(value)));
            }
        }
        return new PreviewResult(rendered, missing);
    }

    public static List<String> extractVariables(String body) {
        LinkedHashSet<String> variables = new LinkedHashSet<>();
        Matcher matcher = VARIABLE_PATTERN.matcher(body);
        while (matcher.find()) {
            variables.add(matcher.group(1));
        }
        return List.copyOf(variables);
    }

    public record TemplateDraft(String templateCode, String displayName, String channel, String body) {}

    public record Template(Long tenantId, String templateCode, String displayName, String channel, String body, List<String> variables, String status) {}

    public record PreviewResult(String renderedBody, List<String> missingVariables) {}

    public interface TemplateRepository {
        void insert(Template template);
        List<Template> search(Long tenantId, String keyword, String channel);
        Template get(Long tenantId, String templateCode);
    }
}
```

- [x] **Step 5: Run service tests**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=MessageTemplateServiceTest
```

Expected: PASS.

### Task 2: Template API

**Files:**
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/MessageTemplateController.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/controller/MessageTemplateControllerTest.java`

- [x] **Step 1: Write controller tests**

Create `backend/canvas-engine/src/test/java/org/chovy/canvas/controller/MessageTemplateControllerTest.java`:

```java
package org.chovy.canvas.web;

import org.chovy.canvas.common.tenant.RoleNames;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.template.MessageTemplateService;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MessageTemplateControllerTest {

    @Test
    void createUsesTenantFromContext() {
        MessageTemplateService service = mock(MessageTemplateService.class);
        TenantContextResolver resolver = mock(TenantContextResolver.class);
        when(resolver.current()).thenReturn(Mono.just(operator()));
        MessageTemplateService.TemplateDraft draft = new MessageTemplateService.TemplateDraft("welcome_sms", "Welcome SMS", "SMS", "Hi {{firstName}}");
        when(service.create(8L, draft)).thenReturn(new MessageTemplateService.Template(
                8L, "welcome_sms", "Welcome SMS", "SMS", "Hi {{firstName}}", List.of("firstName"), "DRAFT"));
        MessageTemplateController controller = new MessageTemplateController(service, resolver);

        StepVerifier.create(controller.create(draft))
                .assertNext(response -> assertThat(response.getData().templateCode()).isEqualTo("welcome_sms"))
                .verifyComplete();
    }

    @Test
    void previewUsesTenantAndTemplateCode() {
        MessageTemplateService service = mock(MessageTemplateService.class);
        TenantContextResolver resolver = mock(TenantContextResolver.class);
        when(resolver.current()).thenReturn(Mono.just(operator()));
        when(service.preview(8L, "welcome_sms", Map.of("firstName", "Alice")))
                .thenReturn(new MessageTemplateService.PreviewResult("Hi Alice", List.of()));
        MessageTemplateController controller = new MessageTemplateController(service, resolver);

        StepVerifier.create(controller.preview("welcome_sms", Map.of("firstName", "Alice")))
                .assertNext(response -> assertThat(response.getData().renderedBody()).isEqualTo("Hi Alice"))
                .verifyComplete();

        verify(service).preview(8L, "welcome_sms", Map.of("firstName", "Alice"));
    }

    private TenantContext operator() {
        return new TenantContext(8L, RoleNames.OPERATOR, "operator-1");
    }
}
```

- [x] **Step 2: Run controller tests and confirm red state**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=MessageTemplateControllerTest
```

Expected: FAIL because `MessageTemplateController` does not exist.

- [x] **Step 3: Add controller**

Create `backend/canvas-engine/src/main/java/org/chovy/canvas/web/MessageTemplateController.java`:

```java
package org.chovy.canvas.web;

import lombok.RequiredArgsConstructor;
import org.chovy.canvas.common.R;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.template.MessageTemplateService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/message-templates")
@RequiredArgsConstructor
public class MessageTemplateController {

    private final MessageTemplateService service;
    private final TenantContextResolver tenantContextResolver;

    @GetMapping
    public Mono<R<List<MessageTemplateService.Template>>> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String channel) {
        return tenantContextResolver.current()
                .flatMap(context -> Mono.fromCallable(() -> service.search(context.tenantId(), keyword, channel))
                        .subscribeOn(Schedulers.boundedElastic()))
                .map(R::ok);
    }

    @PostMapping
    public Mono<R<MessageTemplateService.Template>> create(@RequestBody MessageTemplateService.TemplateDraft draft) {
        return tenantContextResolver.current()
                .flatMap(context -> Mono.fromCallable(() -> service.create(context.tenantId(), draft))
                        .subscribeOn(Schedulers.boundedElastic()))
                .map(R::ok);
    }

    @PostMapping("/{templateCode}/preview")
    public Mono<R<MessageTemplateService.PreviewResult>> preview(
            @PathVariable String templateCode,
            @RequestBody Map<String, Object> context) {
        return tenantContextResolver.current()
                .flatMap(tenant -> Mono.fromCallable(() -> service.preview(tenant.tenantId(), templateCode, context))
                        .subscribeOn(Schedulers.boundedElastic()))
                .map(R::ok);
    }
}
```

- [x] **Step 4: Run controller tests**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=MessageTemplateControllerTest
```

Expected: PASS.

### Task 3: Frontend Template Helpers

**Files:**
- Create: `frontend/src/services/messageTemplateApi.ts`
- Create: `frontend/src/services/messageTemplateApi.test.ts`
- Create: `frontend/src/pages/message-templates/messageTemplateCenter.ts`
- Create: `frontend/src/pages/message-templates/messageTemplateCenter.test.ts`
- Create: `frontend/src/pages/message-templates/index.tsx`
- Create: `frontend/src/pages/message-templates/index.test.tsx`
- Modify: `frontend/src/App.tsx`
- Modify: `frontend/src/components/layout/AppLayout.tsx`
- Modify: `frontend/src/components/layout/AppLayout.a11y.test.tsx`
- Modify: `frontend/src/components/accessibility/RouteA11y.tsx`

- [x] **Step 1: Write frontend tests**

Create `frontend/src/pages/message-templates/messageTemplateCenter.test.ts`:

```ts
import { describe, expect, it } from 'vitest'
import { channelLabel, formatMissingVariables, templatePreviewState, variablesFromBody } from './messageTemplateCenter'

describe('messageTemplateCenter', () => {
  it('extracts variables in display order without duplicates', () => {
    expect(variablesFromBody('Hi {{firstName}}, {{firstName}} uses {{couponCode}}')).toEqual(['firstName', 'couponCode'])
  })

  it('formats missing variables and channel labels', () => {
    expect(formatMissingVariables(['couponCode', 'tier'])).toBe('Missing: couponCode, tier')
    expect(formatMissingVariables([])).toBe('All variables resolved')
    expect(channelLabel('IN_APP')).toBe('In-app')
  })

  it('maps preview state for UI rendering', () => {
    expect(templatePreviewState({ renderedBody: 'Hi Alice', missingVariables: [] })).toEqual({
      status: 'READY',
      text: 'Hi Alice',
    })
    expect(templatePreviewState({ renderedBody: 'Hi {{couponCode}}', missingVariables: ['couponCode'] }).status).toBe('MISSING_VARIABLES')
  })
})
```

- [x] **Step 2: Run frontend tests and confirm red state**

Run:

```bash
cd frontend && npm test -- messageTemplateCenter.test.ts
```

Expected: FAIL because `messageTemplateCenter.ts` does not exist.

- [x] **Step 3: Add API wrapper**

Create `frontend/src/services/messageTemplateApi.ts`:

```ts
import http from './api'
import type { R } from '../types'
import type { MessageTemplate, MessageTemplateDraft, TemplatePreviewResult } from '../pages/message-templates/messageTemplateCenter'

export const messageTemplateApi = {
  search: (params?: { keyword?: string; channel?: string }) =>
    http.get<R<MessageTemplate[]>, R<MessageTemplate[]>>('/message-templates', { params }),
  create: (payload: MessageTemplateDraft) =>
    http.post<R<MessageTemplate>, R<MessageTemplate>>('/message-templates', payload),
  preview: (templateCode: string, context: Record<string, unknown>) =>
    http.post<R<TemplatePreviewResult>, R<TemplatePreviewResult>>(`/message-templates/${templateCode}/preview`, context),
}
```

- [x] **Step 4: Add presentation helpers**

Create `frontend/src/pages/message-templates/messageTemplateCenter.ts`:

```ts
export interface MessageTemplateDraft {
  templateCode: string
  displayName: string
  channel: string
  body: string
}

export interface MessageTemplate extends MessageTemplateDraft {
  tenantId: number
  variables: string[]
  status: string
}

export interface TemplatePreviewResult {
  renderedBody: string
  missingVariables: string[]
}

export function variablesFromBody(body: string): string[] {
  const variables: string[] = []
  for (const match of body.matchAll(/\{\{\s*([A-Za-z][A-Za-z0-9_]*)\s*}}/g)) {
    if (!variables.includes(match[1])) variables.push(match[1])
  }
  return variables
}

export function formatMissingVariables(missing: string[]) {
  return missing.length === 0 ? 'All variables resolved' : `Missing: ${missing.join(', ')}`
}

export function channelLabel(channel: string) {
  if (channel === 'IN_APP') return 'In-app'
  return channel.charAt(0) + channel.slice(1).toLowerCase()
}

export function templatePreviewState(result: TemplatePreviewResult) {
  return {
    status: result.missingVariables.length === 0 ? 'READY' : 'MISSING_VARIABLES',
    text: result.renderedBody,
  }
}
```

- [x] **Step 5: Run frontend tests**

Run:

```bash
cd frontend && npm test -- messageTemplateCenter.test.ts
```

Expected: PASS.

### Task 4: Verification And Rollout Notes

**Files:**
- Modify: `docs/product-evolution/specs/p2-005-message-template-center.md`
- Modify: `docs/product-evolution/plans/p2-005-message-template-center-plan.md`
- Create: `backend/canvas-engine/src/main/resources/db/migration/V268__message_template_center.sql`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/template/MessageTemplateService.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/template/JdbcMessageTemplateRepository.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/MessageTemplateController.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/template/MessageTemplateServiceTest.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/controller/MessageTemplateControllerTest.java`
- Create: `frontend/src/services/messageTemplateApi.ts`
- Create: `frontend/src/services/messageTemplateApi.test.ts`
- Create: `frontend/src/pages/message-templates/messageTemplateCenter.ts`
- Create: `frontend/src/pages/message-templates/messageTemplateCenter.test.ts`
- Create: `frontend/src/pages/message-templates/index.tsx`
- Create: `frontend/src/pages/message-templates/index.test.tsx`
- Modify: `frontend/src/App.tsx`
- Modify: `frontend/src/components/layout/AppLayout.tsx`
- Modify: `frontend/src/components/layout/AppLayout.a11y.test.tsx`
- Modify: `frontend/src/components/accessibility/RouteA11y.tsx`

- [x] **Step 1: Run focused backend tests**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=MessageTemplateServiceTest,MessageTemplateControllerTest
```

Expected: PASS.

- [x] **Step 2: Run focused frontend tests**

Run:

```bash
cd frontend && npm test -- src/pages/message-templates/messageTemplateCenter.test.ts src/services/messageTemplateApi.test.ts src/pages/message-templates/index.test.tsx src/components/layout/AppLayout.a11y.test.tsx
```

Expected: PASS.

- [x] **Step 3: Add rollout notes to the implementation PR**

Use this rollout note text:

```markdown
Rollout: run `V268__message_template_center.sql`, then expose template search/create/preview entry points to operators. Rollback: hide the template center route; template rows are additive and do not affect runtime sends until a child approval/channel-adaptation spec wires them into canvas nodes.
```

- [x] **Step 4: Commit skipped by operator instruction**

Run:

```bash
Commit was not created because the operator did not request one.
```

Expected: no commit is created; changes remain in the working tree for operator review.

## Acceptance Checklist

- [x] Tenant-scoped message template table exists as additive migration `V268__message_template_center.sql`.
- [x] Backend create/search/preview preserves tenant context and rejects unsupported channels.
- [x] Variables are extracted in stable display order, persisted as JSON, and used by preview rendering.
- [x] `/message-templates` read/write/preview endpoints require authenticated tenant context.
- [x] Frontend API wrapper, helper functions, page, route, navigation entry, empty/loading/error/saving states, and route announcement are implemented.
- [x] Rollout and rollback notes are documented.

## Verification Evidence

- [x] Backend red state on 2026-06-05: `JAVA_HOME=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home PATH="/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home/bin:$PATH" mvn -pl canvas-engine -Dtest=MessageTemplateServiceTest,MessageTemplateControllerTest test` from `backend` failed in `testCompile` because `MessageTemplateService` did not exist.
- [x] Backend focused tests pass on 2026-06-05: `JAVA_HOME=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home PATH="/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home/bin:$PATH" mvn -pl canvas-engine -Dtest=MessageTemplateServiceTest,MessageTemplateControllerTest test` from `backend` (8 tests, 0 failures, 0 errors, 0 skipped).
- [x] Frontend red state on 2026-06-05: `PATH="/opt/homebrew/bin:$PATH" npm run test -- messageTemplateCenter.test.ts messageTemplateApi.test.ts index.test.tsx` from `frontend` failed because `messageTemplateApi`, `messageTemplateCenter`, and `message-templates/index.tsx` did not exist.
- [x] Frontend focused tests pass on 2026-06-05: `PATH="/opt/homebrew/bin:$PATH" npm run test -- src/pages/message-templates/messageTemplateCenter.test.ts src/services/messageTemplateApi.test.ts src/pages/message-templates/index.test.tsx src/components/layout/AppLayout.a11y.test.tsx` from `frontend` (9 tests, 0 failures).
- [x] Frontend production build passes on 2026-06-05: `PATH="/opt/homebrew/bin:$PATH" npm run build` from `frontend`.
