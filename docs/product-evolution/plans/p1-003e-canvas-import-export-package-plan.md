# Canvas Import Export Package Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a sanitized, versioned canvas export package and import it as a new draft canvas with one draft version.

**Architecture:** Implement a `CanvasImportExportService` beside existing canvas domain services. Export reads one canvas/version, sanitizes graph JSON recursively, and returns package version 1; import validates the package and writes a new draft canvas/version without reusing runtime state.

**Tech Stack:** Java 21, Spring Boot, MyBatis-Plus, Jackson, JUnit 5, Mockito, AssertJ, React 18, TypeScript, Ant Design, Vitest.

---

## Spec Reference

- `docs/product-evolution/specs/p1-003e-canvas-import-export-package.md`

## File Structure

- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/canvas/CanvasImportExportService.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dto/canvas/CanvasExportPackage.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dto/canvas/CanvasImportReq.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dto/canvas/CanvasImportResp.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/canvas/CanvasImportExportServiceTest.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/CanvasController.java`
- Modify: `frontend/src/services/api.ts`
- Create: `frontend/src/pages/canvas-list/canvasImportExport.ts`
- Create: `frontend/src/pages/canvas-list/importExportFlow.test.ts`
- Modify: `frontend/src/pages/canvas-list/index.tsx`

### Task 1: Backend Package Service

**Files:**
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/canvas/CanvasImportExportServiceTest.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dto/canvas/CanvasExportPackage.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dto/canvas/CanvasImportReq.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dto/canvas/CanvasImportResp.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/canvas/CanvasImportExportService.java`

- [x] **Step 1: Write import/export tests**

Create `CanvasImportExportServiceTest.java`:

```java
package org.chovy.canvas.domain.canvas;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.common.enums.CanvasStatusEnum;
import org.chovy.canvas.dal.dataobject.CanvasDO;
import org.chovy.canvas.dal.dataobject.CanvasVersionDO;
import org.chovy.canvas.dal.mapper.CanvasMapper;
import org.chovy.canvas.dal.mapper.CanvasVersionMapper;
import org.chovy.canvas.dto.canvas.CanvasImportReq;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CanvasImportExportServiceTest {

    @Test
    void exportSanitizesRuntimeSnapshotAndSecrets() {
        CanvasMapper canvasMapper = mock(CanvasMapper.class);
        CanvasVersionMapper versionMapper = mock(CanvasVersionMapper.class);
        CanvasDO canvas = new CanvasDO();
        canvas.setId(62L);
        canvas.setName("旅程");
        canvas.setProjectKey("growth");
        when(canvasMapper.selectById(62L)).thenReturn(canvas);
        CanvasVersionDO version = new CanvasVersionDO();
        version.setId(91L);
        version.setCanvasId(62L);
        version.setGraphJson("""
                {"nodes":[{"id":"tag","type":"TAGGER","config":{
                  "audienceSnapshotId":501,
                  "idempotencyKey":"idem-1",
                  "apiKey":"live-key",
                  "password":"secret"
                }}]}
                """);
        when(versionMapper.selectById(91L)).thenReturn(version);
        CanvasImportExportService service = new CanvasImportExportService(
                canvasMapper, versionMapper, new ObjectMapper());

        var exported = service.exportCanvas(62L, 91L);

        String graph = exported.graph().toString();
        assertThat(graph).doesNotContain("audienceSnapshotId", "idempotencyKey", "live-key", "secret");
        assertThat(exported.packageVersion()).isEqualTo(1);
        assertThat(exported.canvas().get("projectKey")).isEqualTo("growth");
    }

    @Test
    void importCreatesDraftCanvasAndDraftVersion() {
        CanvasMapper canvasMapper = mock(CanvasMapper.class);
        CanvasVersionMapper versionMapper = mock(CanvasVersionMapper.class);
        CanvasImportExportService service = new CanvasImportExportService(
                canvasMapper, versionMapper, new ObjectMapper());
        String packageJson = """
                {"packageVersion":1,"canvas":{"name":"导入旅程","projectKey":"growth"},
                 "graph":{"nodes":[{"id":"start","type":"START","config":{}}]}}
                """;

        var resp = service.importCanvas(new CanvasImportReq(packageJson, "alice"));

        ArgumentCaptor<CanvasDO> canvasCaptor = ArgumentCaptor.forClass(CanvasDO.class);
        ArgumentCaptor<CanvasVersionDO> versionCaptor = ArgumentCaptor.forClass(CanvasVersionDO.class);
        verify(canvasMapper).insert(canvasCaptor.capture());
        verify(versionMapper).insert(versionCaptor.capture());
        assertThat(canvasCaptor.getValue().getStatus()).isEqualTo(CanvasStatusEnum.DRAFT.getCode());
        assertThat(canvasCaptor.getValue().getName()).isEqualTo("导入旅程");
        assertThat(canvasCaptor.getValue().getProjectKey()).isEqualTo("growth");
        assertThat(versionCaptor.getValue().getCanvasId()).isEqualTo(canvasCaptor.getValue().getId());
        assertThat(resp.canvas()).isSameAs(canvasCaptor.getValue());
    }

    @Test
    void importRejectsUnsupportedPackageVersion() {
        CanvasImportExportService service = new CanvasImportExportService(
                mock(CanvasMapper.class), mock(CanvasVersionMapper.class), new ObjectMapper());

        assertThatThrownBy(() -> service.importCanvas(new CanvasImportReq(
                "{\"packageVersion\":2,\"canvas\":{},\"graph\":{\"nodes\":[]}}", "alice")))
                .hasMessageContaining("Unsupported canvas package version");
    }
}
```

- [x] **Step 2: Run tests and confirm red state**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=CanvasImportExportServiceTest
```

Expected: FAIL because service and DTOs do not exist.

- [x] **Step 3: Add DTOs**

Create DTO records:

```java
public record CanvasExportPackage(
        int packageVersion,
        LocalDateTime exportedAt,
        Map<String, Object> source,
        Map<String, Object> canvas,
        Map<String, Object> graph
) {
}

public record CanvasImportReq(String packageJson, String operator) {
}

public record CanvasImportResp(CanvasDO canvas, Long draftVersionId) {
}
```

- [x] **Step 4: Implement service**

Create `CanvasImportExportService.java`:

```java
@Service
@RequiredArgsConstructor
public class CanvasImportExportService {
    private static final int PACKAGE_VERSION = 1;
    private static final Set<String> RUNTIME_KEYS = Set.of(
            "audienceSnapshotId", "idempotencyKey", "publishedVersionId",
            "canaryVersionId", "previousVersionId", "routeState");

    private final CanvasMapper canvasMapper;
    private final CanvasVersionMapper versionMapper;
    private final ObjectMapper objectMapper;

    public CanvasExportPackage exportCanvas(Long canvasId, Long versionId) {
        CanvasDO canvas = requireCanvas(canvasId);
        CanvasVersionDO version = versionMapper.selectById(versionId);
        if (version == null || !canvasId.equals(version.getCanvasId())) {
            throw new IllegalArgumentException("Canvas version not found: " + versionId);
        }
        Map<String, Object> graph = sanitizeGraph(parseMap(version.getGraphJson()));
        return new CanvasExportPackage(
                PACKAGE_VERSION,
                LocalDateTime.now(),
                Map.of("canvasId", canvas.getId(), "canvasName", canvas.getName(), "versionId", version.getId()),
                canvasMeta(canvas),
                graph);
    }

    @Transactional
    public CanvasImportResp importCanvas(CanvasImportReq req) {
        Map<String, Object> pkg = parseMap(req.packageJson());
        if (!Integer.valueOf(PACKAGE_VERSION).equals(asInt(pkg.get("packageVersion")))) {
            throw new IllegalArgumentException("Unsupported canvas package version: " + pkg.get("packageVersion"));
        }
        Map<String, Object> canvasMeta = objectMap(pkg.get("canvas"), "canvas");
        Map<String, Object> graph = objectMap(pkg.get("graph"), "graph");
        validateGraph(graph);

        CanvasDO canvas = new CanvasDO();
        canvas.setName(string(canvasMeta, "name", "Imported Canvas"));
        canvas.setDescription(string(canvasMeta, "description", null));
        canvas.setStatus(CanvasStatusEnum.DRAFT.getCode());
        canvas.setCreatedBy(req.operator());
        canvas.setProjectKey(string(canvasMeta, "projectKey", null));
        canvas.setProjectName(string(canvasMeta, "projectName", null));
        canvas.setFolderKey(string(canvasMeta, "folderKey", null));
        canvas.setFolderName(string(canvasMeta, "folderName", null));
        canvas.setTriggerType(string(canvasMeta, "triggerType", null));
        canvas.setCronExpression(string(canvasMeta, "cronExpression", null));
        canvasMapper.insert(canvas);

        CanvasVersionDO version = new CanvasVersionDO();
        version.setCanvasId(canvas.getId());
        version.setVersion(1);
        version.setStatus(VersionStatus.DRAFT.getCode());
        version.setCreatedBy(req.operator());
        version.setGraphJson(writeJson(graph));
        versionMapper.insert(version);
        return new CanvasImportResp(canvas, version.getId());
    }
}
```

Add private helpers `requireCanvas`, `canvasMeta`, `sanitizeGraph`, `removeRuntimeKeys`, `parseMap`, `objectMap`, `validateGraph`, `asInt`, `string`, and `writeJson`. `sanitizeGraph` must call `removeRuntimeKeys` before `DataMaskingUtil.maskObject`.

- [x] **Step 5: Run service tests**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=CanvasImportExportServiceTest
```

Expected: PASS.

### Task 2: API And Frontend Helpers

**Files:**
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/CanvasController.java`
- Modify: `frontend/src/services/api.ts`
- Create: `frontend/src/pages/canvas-list/canvasImportExport.ts`
- Create: `frontend/src/pages/canvas-list/importExportFlow.test.ts`

- [x] **Step 1: Add controller endpoints**

Inject service:

```java
private final CanvasImportExportService importExportService;
```

Add endpoints:

```java
@GetMapping("/{id}/export")
public Mono<R<CanvasExportPackage>> exportCanvas(
        @PathVariable Long id,
        @RequestParam Long versionId) {
    return Mono.fromCallable(() -> importExportService.exportCanvas(id, versionId))
            .subscribeOn(Schedulers.boundedElastic())
            .map(R::ok);
}

@PostMapping("/import")
public Mono<R<CanvasImportResp>> importCanvas(@RequestBody CanvasImportReq req) {
    CanvasImportReq normalized = new CanvasImportReq(req.packageJson(), req.operator() == null ? "system" : req.operator());
    return Mono.fromCallable(() -> importExportService.importCanvas(normalized))
            .subscribeOn(Schedulers.boundedElastic())
            .map(R::ok);
}
```

- [x] **Step 2: Add frontend helper tests**

Create `importExportFlow.test.ts`:

```ts
import { describe, expect, it } from 'vitest'
import { buildImportPayload, exportedFileName } from './canvasImportExport'

describe('canvas import export helpers', () => {
  it('builds stable export file names', () => {
    expect(exportedFileName({ id: 62, name: '新客旅程' })).toBe('canvas-62.json')
  })

  it('trims package text', () => {
    expect(buildImportPayload('  {"packageVersion":1}  ')).toEqual({
      packageJson: '{"packageVersion":1}',
    })
  })
})
```

- [x] **Step 3: Add helper and API methods**

Create `canvasImportExport.ts`:

```ts
import type { Canvas } from '../../types'

export function exportedFileName(canvas: Pick<Canvas, 'id' | 'name'>) {
  return `canvas-${canvas.id}.json`
}

export function buildImportPayload(packageText: string) {
  return { packageJson: packageText.trim() }
}
```

In `api.ts`, add types and methods:

```ts
export interface CanvasExportPackage {
  packageVersion: number
  exportedAt: string
  source: Record<string, unknown>
  canvas: Record<string, unknown>
  graph: Record<string, unknown>
}

export interface CanvasImportResp {
  canvas: Canvas
  draftVersionId: number
}

export const canvasApi = {
  exportCanvas: (id: number, versionId: number) =>
    http.get<R<CanvasExportPackage>, R<CanvasExportPackage>>(`/canvas/${id}/export`, { params: { versionId } }),
  importCanvas: (body: { packageJson: string }) =>
    http.post<R<CanvasImportResp>, R<CanvasImportResp>>('/canvas/import', body),
}
```

Merge these methods into the existing `canvasApi` object.

- [x] **Step 4: Run frontend helper tests**

Run:

```bash
cd frontend && npm test -- importExportFlow.test.ts
```

Expected: PASS.

### Task 3: List Page Actions

**Files:**
- Modify: `frontend/src/pages/canvas-list/index.tsx`

- [x] **Step 1: Add import state and export handler**

Add state:

```tsx
const [importVisible, setImportVisible] = useState(false)
const [importText, setImportText] = useState('')
```

Add handlers:

```tsx
const handleExport = async (record: Canvas) => {
  const versionId = record.publishedVersionId
  if (!versionId) {
    message.warning('请先发布或选择可导出的版本')
    return
  }
  const res = await canvasApi.exportCanvas(record.id, versionId)
  const blob = new Blob([JSON.stringify(res.data, null, 2)], { type: 'application/json' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = exportedFileName(record)
  link.click()
  URL.revokeObjectURL(url)
}

const handleImport = async () => {
  await canvasApi.importCanvas(buildImportPayload(importText))
  message.success('导入成功')
  setImportVisible(false)
  setImportText('')
  fetchList(1)
}
```

- [x] **Step 2: Add buttons and modal**

Add an import button near the existing create button:

```tsx
<Button onClick={() => setImportVisible(true)}>导入</Button>
```

Add an export action in the row action menu:

```tsx
{
  key: 'export',
  label: '导出',
  onClick: () => handleExport(record),
}
```

Add modal:

```tsx
<Modal
  title="导入画布"
  open={importVisible}
  onOk={handleImport}
  onCancel={() => setImportVisible(false)}
  okText="导入"
  cancelText="取消"
>
  <Input.TextArea
    rows={10}
    value={importText}
    onChange={event => setImportText(event.target.value)}
  />
</Modal>
```

- [x] **Step 3: Run focused verification**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=CanvasImportExportServiceTest
cd frontend && npm test -- importExportFlow.test.ts
```

Expected: PASS.

### Verification Evidence

- Backend import/export service and controller operator loop suite:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=CanvasImportExportServiceTest,CanvasControllerOperatorLoopTest
```

Result: 7 tests, 0 failures, 0 errors, 0 skipped.

- Frontend import/export helper suite:

```bash
cd frontend && npm test -- importExportFlow.test.ts
```

Result: 1 test file, 2 tests passed.

- Frontend production build:

```bash
cd frontend && npm run build
```

Result: TypeScript and Vite build passed.

### Task 4: Commit This Slice

**Files:**
- Read: `docs/product-evolution/specs/p1-003e-canvas-import-export-package.md`
- Read: `docs/product-evolution/plans/p1-003e-canvas-import-export-package-plan.md`

- [ ] **Step 1: Commit**

Run:

```bash
git add backend/canvas-engine/src/main/java/org/chovy/canvas/domain/canvas/CanvasImportExportService.java \
  backend/canvas-engine/src/main/java/org/chovy/canvas/dto/canvas/CanvasExportPackage.java \
  backend/canvas-engine/src/main/java/org/chovy/canvas/dto/canvas/CanvasImportReq.java \
  backend/canvas-engine/src/main/java/org/chovy/canvas/dto/canvas/CanvasImportResp.java \
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/CanvasController.java \
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/canvas/CanvasImportExportServiceTest.java \
  frontend/src/services/api.ts \
  frontend/src/pages/canvas-list/canvasImportExport.ts \
  frontend/src/pages/canvas-list/importExportFlow.test.ts \
  frontend/src/pages/canvas-list/index.tsx \
  docs/product-evolution/specs/p1-003e-canvas-import-export-package.md \
  docs/product-evolution/plans/p1-003e-canvas-import-export-package-plan.md
git commit -m "feat: add canvas import export packages"
```

Expected: commit contains only import/export package behavior.
