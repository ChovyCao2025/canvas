# Plugin And Integration Foundations Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Create the first safe internal plugin foundation: a built-in plugin registry with compatibility checks, enable state, and API docs presentation helpers.

**Architecture:** Store built-in plugin metadata in an additive table while keeping runtime loading closed to third-party code. Expose a read-only operator API for plugin catalog and a controlled enable/disable service path that checks compatibility before state changes.

**Tech Stack:** Java 21, Spring Boot WebFlux-style `Mono`, MyBatis-Plus, Flyway, JUnit 5, Mockito, AssertJ, React 18, TypeScript, Axios, Vitest.

---

## Spec Reference

- `docs/product-evolution/specs/p2-002-plugin-and-integration-foundations.md`
- Source item: `docs/product-evolution/todo/p2/plugin-and-integration-foundations.md`

## File Structure

**Backend**
- Create: `backend/canvas-engine/src/main/resources/db/migration/V161__plugin_integration_foundations.sql` - built-in plugin metadata table.
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/plugin/PluginRegistryService.java` - built-in plugin catalog and compatibility state transition logic.
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/PluginRegistryController.java` - `/canvas/plugins` catalog and enable-state endpoint.
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/plugin/PluginRegistryServiceTest.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/controller/PluginRegistryControllerTest.java`

**Frontend**
- Create: `frontend/src/services/pluginRegistryApi.ts`
- Create: `frontend/src/pages/api-docs/pluginIntegrationDocs.ts`
- Create: `frontend/src/pages/api-docs/pluginIntegrationDocs.test.ts`

### Task 1: Registry Schema And Service

**Files:**
- Create: `backend/canvas-engine/src/main/resources/db/migration/V161__plugin_integration_foundations.sql`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/plugin/PluginRegistryService.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/plugin/PluginRegistryServiceTest.java`

- [ ] **Step 1: Write registry service tests**

Create `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/plugin/PluginRegistryServiceTest.java`:

```java
package org.chovy.canvas.engine.plugin;

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

class PluginRegistryServiceTest {

    @Test
    void migrationCreatesPluginRegistryTableWithoutRuntimeCodeLoadingFields() throws Exception {
        String sql = Files.readString(Path.of(
                "src/main/resources/db/migration/V161__plugin_integration_foundations.sql"));

        assertThat(sql)
                .contains("CREATE TABLE IF NOT EXISTS built_in_plugin_registry")
                .contains("plugin_key VARCHAR(128) NOT NULL")
                .contains("extension_point VARCHAR(64) NOT NULL")
                .contains("compatibility_json JSON NOT NULL")
                .contains("enabled TINYINT NOT NULL DEFAULT 0")
                .doesNotContain("jar_url")
                .doesNotContain("classloader");
    }

    @Test
    void listReturnsBuiltInPluginsGroupedByExtensionPoint() {
        PluginRegistryService.PluginRepository repository = mock(PluginRegistryService.PluginRepository.class);
        PluginRegistryService service = new PluginRegistryService(repository);
        when(repository.list()).thenReturn(List.of(
                new PluginRegistryService.Plugin("wecom-channel", "CHANNEL_ADAPTER", "WeCom", true, Map.of("minCanvasVersion", "1.0.0")),
                new PluginRegistryService.Plugin("csv-export", "DATA_EXPORTER", "CSV Export", false, Map.of("minCanvasVersion", "1.0.0"))));

        Map<String, List<PluginRegistryService.Plugin>> grouped = service.groupedCatalog();

        assertThat(grouped).containsKeys("CHANNEL_ADAPTER", "DATA_EXPORTER");
        assertThat(grouped.get("CHANNEL_ADAPTER")).extracting(PluginRegistryService.Plugin::pluginKey)
                .containsExactly("wecom-channel");
    }

    @Test
    void enableRejectsIncompatiblePluginBeforePersisting() {
        PluginRegistryService.PluginRepository repository = mock(PluginRegistryService.PluginRepository.class);
        PluginRegistryService service = new PluginRegistryService(repository);
        when(repository.get("ai-gateway")).thenReturn(new PluginRegistryService.Plugin(
                "ai-gateway", "AI_GATEWAY", "AI Gateway", false, Map.of("minCanvasVersion", "9.9.9")));

        assertThatThrownBy(() -> service.setEnabled("ai-gateway", true, "1.0.0"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("plugin ai-gateway requires canvas version 9.9.9");
    }

    @Test
    void enablePersistsCompatiblePlugin() {
        PluginRegistryService.PluginRepository repository = mock(PluginRegistryService.PluginRepository.class);
        PluginRegistryService service = new PluginRegistryService(repository);
        when(repository.get("csv-export")).thenReturn(new PluginRegistryService.Plugin(
                "csv-export", "DATA_EXPORTER", "CSV Export", false, Map.of("minCanvasVersion", "1.0.0")));

        service.setEnabled("csv-export", true, "1.2.0");

        verify(repository).setEnabled(argThat(command ->
                command.pluginKey().equals("csv-export") && command.enabled()));
    }
}
```

- [ ] **Step 2: Run service tests and confirm red state**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=PluginRegistryServiceTest
```

Expected: FAIL because the migration and service do not exist.

- [ ] **Step 3: Add registry migration**

Create `backend/canvas-engine/src/main/resources/db/migration/V161__plugin_integration_foundations.sql`:

```sql
CREATE TABLE IF NOT EXISTS built_in_plugin_registry (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  plugin_key VARCHAR(128) NOT NULL,
  display_name VARCHAR(128) NOT NULL,
  extension_point VARCHAR(64) NOT NULL,
  compatibility_json JSON NOT NULL,
  config_schema_json JSON NULL,
  enabled TINYINT NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_builtin_plugin_key (plugin_key),
  INDEX idx_builtin_plugin_extension (extension_point, enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO built_in_plugin_registry(plugin_key, display_name, extension_point, compatibility_json, config_schema_json, enabled)
VALUES
  ('wecom-channel', 'WeCom Channel Adapter', 'CHANNEL_ADAPTER', JSON_OBJECT('minCanvasVersion', '1.0.0'), JSON_OBJECT('corpId', 'string'), 0),
  ('csv-export', 'CSV Data Exporter', 'DATA_EXPORTER', JSON_OBJECT('minCanvasVersion', '1.0.0'), JSON_OBJECT('delimiter', 'string'), 1),
  ('batch-operations', 'Batch Operation Pack', 'RULE_TEMPLATE_PACK', JSON_OBJECT('minCanvasVersion', '1.0.0'), JSON_OBJECT(), 1),
  ('ai-gateway', 'AI Gateway Adapter', 'AI_GATEWAY', JSON_OBJECT('minCanvasVersion', '1.0.0'), JSON_OBJECT('provider', 'string'), 0);
```

- [ ] **Step 4: Implement plugin registry service**

Create `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/plugin/PluginRegistryService.java`:

```java
package org.chovy.canvas.engine.plugin;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class PluginRegistryService {

    private final PluginRepository repository;

    public PluginRegistryService(PluginRepository repository) {
        this.repository = repository;
    }

    public Map<String, List<Plugin>> groupedCatalog() {
        return repository.list().stream()
                .sorted(Comparator.comparing(Plugin::pluginKey))
                .collect(Collectors.groupingBy(Plugin::extensionPoint));
    }

    public void setEnabled(String pluginKey, boolean enabled, String currentCanvasVersion) {
        Plugin plugin = repository.get(pluginKey);
        String minVersion = String.valueOf(plugin.compatibility().getOrDefault("minCanvasVersion", "1.0.0"));
        if (compareVersion(currentCanvasVersion, minVersion) < 0) {
            throw new IllegalStateException("plugin " + pluginKey + " requires canvas version " + minVersion);
        }
        repository.setEnabled(new EnableCommand(pluginKey, enabled));
    }

    private static int compareVersion(String left, String right) {
        String[] l = left.split("\\.");
        String[] r = right.split("\\.");
        for (int i = 0; i < Math.max(l.length, r.length); i++) {
            int lv = i < l.length ? Integer.parseInt(l[i]) : 0;
            int rv = i < r.length ? Integer.parseInt(r[i]) : 0;
            if (lv != rv) return Integer.compare(lv, rv);
        }
        return 0;
    }

    public record Plugin(String pluginKey, String extensionPoint, String displayName, boolean enabled, Map<String, Object> compatibility) {}

    public record EnableCommand(String pluginKey, boolean enabled) {}

    public interface PluginRepository {
        List<Plugin> list();
        Plugin get(String pluginKey);
        void setEnabled(EnableCommand command);
    }
}
```

- [ ] **Step 5: Run service tests**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=PluginRegistryServiceTest
```

Expected: PASS.

### Task 2: Operator API

**Files:**
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/PluginRegistryController.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/controller/PluginRegistryControllerTest.java`

- [ ] **Step 1: Write controller tests**

Create `backend/canvas-engine/src/test/java/org/chovy/canvas/controller/PluginRegistryControllerTest.java`:

```java
package org.chovy.canvas.web;

import org.chovy.canvas.engine.plugin.PluginRegistryService;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PluginRegistryControllerTest {

    @Test
    void catalogReturnsGroupedPlugins() {
        PluginRegistryService service = mock(PluginRegistryService.class);
        when(service.groupedCatalog()).thenReturn(Map.of("DATA_EXPORTER", List.of(
                new PluginRegistryService.Plugin("csv-export", "DATA_EXPORTER", "CSV Export", true, Map.of()))));
        PluginRegistryController controller = new PluginRegistryController(service);

        StepVerifier.create(controller.catalog())
                .assertNext(response -> {
                    assertThat(response.getCode()).isEqualTo(0);
                    assertThat(response.getData().get("DATA_EXPORTER")).hasSize(1);
                })
                .verifyComplete();
    }

    @Test
    void setEnabledDelegatesWithCurrentVersionHeader() {
        PluginRegistryService service = mock(PluginRegistryService.class);
        PluginRegistryController controller = new PluginRegistryController(service);

        StepVerifier.create(controller.setEnabled("csv-export", "1.2.0", new PluginRegistryController.EnableRequest(true)))
                .assertNext(response -> assertThat(response.getCode()).isEqualTo(0))
                .verifyComplete();

        verify(service).setEnabled("csv-export", true, "1.2.0");
    }
}
```

- [ ] **Step 2: Run controller tests and confirm red state**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=PluginRegistryControllerTest
```

Expected: FAIL because `PluginRegistryController` does not exist.

- [ ] **Step 3: Add controller**

Create `backend/canvas-engine/src/main/java/org/chovy/canvas/web/PluginRegistryController.java`:

```java
package org.chovy.canvas.web;

import lombok.RequiredArgsConstructor;
import org.chovy.canvas.common.R;
import org.chovy.canvas.engine.plugin.PluginRegistryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/canvas/plugins")
@RequiredArgsConstructor
public class PluginRegistryController {

    private final PluginRegistryService service;

    @GetMapping
    public Mono<R<Map<String, List<PluginRegistryService.Plugin>>>> catalog() {
        return Mono.fromCallable(service::groupedCatalog)
                .subscribeOn(Schedulers.boundedElastic())
                .map(R::ok);
    }

    @PutMapping("/{pluginKey}/enabled")
    public Mono<R<Void>> setEnabled(
            @PathVariable String pluginKey,
            @RequestHeader(name = "X-Canvas-Version", defaultValue = "1.0.0") String canvasVersion,
            @RequestBody EnableRequest request) {
        return Mono.<Void>fromRunnable(() -> service.setEnabled(pluginKey, request.enabled(), canvasVersion))
                .subscribeOn(Schedulers.boundedElastic())
                .thenReturn(R.ok());
    }

    public record EnableRequest(boolean enabled) {}
}
```

- [ ] **Step 4: Run controller tests**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=PluginRegistryControllerTest
```

Expected: PASS.

### Task 3: Frontend Plugin Docs Helpers

**Files:**
- Create: `frontend/src/services/pluginRegistryApi.ts`
- Create: `frontend/src/pages/api-docs/pluginIntegrationDocs.ts`
- Create: `frontend/src/pages/api-docs/pluginIntegrationDocs.test.ts`

- [ ] **Step 1: Write frontend helper tests**

Create `frontend/src/pages/api-docs/pluginIntegrationDocs.test.ts`:

```ts
import { describe, expect, it } from 'vitest'
import { formatPluginCompatibility, pluginStatusText, sortPluginCatalog, type PluginCatalog } from './pluginIntegrationDocs'

describe('pluginIntegrationDocs', () => {
  it('sorts extension groups and plugins for API docs display', () => {
    const catalog: PluginCatalog = {
      DATA_EXPORTER: [{ pluginKey: 'csv-export', extensionPoint: 'DATA_EXPORTER', displayName: 'CSV Export', enabled: true, compatibility: { minCanvasVersion: '1.0.0' } }],
      CHANNEL_ADAPTER: [{ pluginKey: 'wecom-channel', extensionPoint: 'CHANNEL_ADAPTER', displayName: 'WeCom', enabled: false, compatibility: { minCanvasVersion: '1.0.0' } }],
    }

    expect(sortPluginCatalog(catalog).map(group => group.extensionPoint)).toEqual(['CHANNEL_ADAPTER', 'DATA_EXPORTER'])
  })

  it('formats status and compatibility copy', () => {
    expect(pluginStatusText(true)).toBe('Enabled')
    expect(pluginStatusText(false)).toBe('Disabled')
    expect(formatPluginCompatibility({ minCanvasVersion: '1.2.0' })).toBe('Requires Canvas 1.2.0 or newer')
  })
})
```

- [ ] **Step 2: Run frontend helper tests and confirm red state**

Run:

```bash
cd frontend && npm test -- pluginIntegrationDocs.test.ts
```

Expected: FAIL because `pluginIntegrationDocs.ts` does not exist.

- [ ] **Step 3: Add API wrapper**

Create `frontend/src/services/pluginRegistryApi.ts`:

```ts
import http from './api'
import type { R } from '../types'
import type { PluginCatalog } from '../pages/api-docs/pluginIntegrationDocs'

export const pluginRegistryApi = {
  catalog: () => http.get<R<PluginCatalog>, R<PluginCatalog>>('/canvas/plugins'),
  setEnabled: (pluginKey: string, enabled: boolean) =>
    http.put<R<void>, R<void>>(`/canvas/plugins/${pluginKey}/enabled`, { enabled }),
}
```

- [ ] **Step 4: Add presentation helpers**

Create `frontend/src/pages/api-docs/pluginIntegrationDocs.ts`:

```ts
export interface PluginItem {
  pluginKey: string
  extensionPoint: string
  displayName: string
  enabled: boolean
  compatibility: Record<string, unknown>
}

export type PluginCatalog = Record<string, PluginItem[]>

export function sortPluginCatalog(catalog: PluginCatalog) {
  return Object.entries(catalog)
    .sort(([left], [right]) => left.localeCompare(right))
    .map(([extensionPoint, plugins]) => ({
      extensionPoint,
      plugins: [...plugins].sort((left, right) => left.pluginKey.localeCompare(right.pluginKey)),
    }))
}

export function pluginStatusText(enabled: boolean) {
  return enabled ? 'Enabled' : 'Disabled'
}

export function formatPluginCompatibility(compatibility: Record<string, unknown>) {
  return `Requires Canvas ${String(compatibility.minCanvasVersion ?? '1.0.0')} or newer`
}
```

- [ ] **Step 5: Run frontend helper tests**

Run:

```bash
cd frontend && npm test -- pluginIntegrationDocs.test.ts
```

Expected: PASS.

### Task 4: Verification And Commit

**Files:**
- Modify: `docs/product-evolution/specs/p2-002-plugin-and-integration-foundations.md`
- Modify: `docs/product-evolution/plans/p2-002-plugin-and-integration-foundations-plan.md`
- Create: `backend/canvas-engine/src/main/resources/db/migration/V161__plugin_integration_foundations.sql`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/plugin/PluginRegistryService.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/PluginRegistryController.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/plugin/PluginRegistryServiceTest.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/controller/PluginRegistryControllerTest.java`
- Create: `frontend/src/services/pluginRegistryApi.ts`
- Create: `frontend/src/pages/api-docs/pluginIntegrationDocs.ts`
- Create: `frontend/src/pages/api-docs/pluginIntegrationDocs.test.ts`

- [ ] **Step 1: Run focused backend tests**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=PluginRegistryServiceTest,PluginRegistryControllerTest
```

Expected: PASS.

- [ ] **Step 2: Run focused frontend tests**

Run:

```bash
cd frontend && npm test -- pluginIntegrationDocs.test.ts
```

Expected: PASS.

- [ ] **Step 3: Add rollout notes to the implementation PR**

Use this rollout note text:

```markdown
Rollout: run `V161__plugin_integration_foundations.sql`, then expose `/canvas/plugins` in the API docs page. Third-party runtime loading remains disabled; only built-in plugin rows can be enabled. Rollback: hide the plugin catalog entry point and set affected rows to `enabled=0`.
```

- [ ] **Step 4: Commit the implementation slice**

Run:

```bash
git add \
  backend/canvas-engine/src/main/resources/db/migration/V161__plugin_integration_foundations.sql \
  backend/canvas-engine/src/main/java/org/chovy/canvas/engine/plugin/PluginRegistryService.java \
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/PluginRegistryController.java \
  backend/canvas-engine/src/test/java/org/chovy/canvas/engine/plugin/PluginRegistryServiceTest.java \
  backend/canvas-engine/src/test/java/org/chovy/canvas/controller/PluginRegistryControllerTest.java \
  frontend/src/services/pluginRegistryApi.ts \
  frontend/src/pages/api-docs/pluginIntegrationDocs.ts \
  frontend/src/pages/api-docs/pluginIntegrationDocs.test.ts \
  docs/product-evolution/specs/p2-002-plugin-and-integration-foundations.md \
  docs/product-evolution/plans/p2-002-plugin-and-integration-foundations-plan.md
git commit -m "feat: add built-in plugin registry foundation"
```

Expected: commit contains only built-in plugin registry, operator API, API docs helpers, tests, spec, and plan files.
