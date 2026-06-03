# Publish-Time Audience Snapshot Locking Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Lock static TAGGER audience membership into durable snapshots during canvas publish without mutating draft graph JSON.

**Architecture:** Add `AudienceSnapshotService` as a pure graph transformation and persistence service. `CanvasService.publish` feeds the draft graph through the service before the existing publish transaction, so only the published graph receives snapshot IDs.

**Tech Stack:** Java 21, Spring Boot, MyBatis-Plus, Jackson, JUnit 5, Mockito, AssertJ.

---

## Spec Reference

- `docs/product-evolution/specs/p1-003b-publish-time-audience-snapshot-locking.md`
- Depends on `docs/product-evolution/specs/p1-003-audience-snapshot-mode-and-defaults.md`

## File Structure

- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/audience/AudienceSnapshotService.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/audience/AudienceSnapshotServiceTest.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/canvas/CanvasPublishAudienceSnapshotTest.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/canvas/CanvasService.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/canvas/CanvasTransactionService.java`

### Task 1: Snapshot Service Contract

**Files:**
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/audience/AudienceSnapshotServiceTest.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/audience/AudienceSnapshotService.java`

- [ ] **Step 1: Write snapshot service tests**

Create `AudienceSnapshotServiceTest.java`:

```java
package org.chovy.canvas.domain.audience;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.dal.dataobject.AudienceDefinitionDO;
import org.chovy.canvas.dal.dataobject.AudienceSnapshotDO;
import org.chovy.canvas.dal.mapper.AudienceDefinitionMapper;
import org.chovy.canvas.dal.mapper.AudienceSnapshotMapper;
import org.chovy.canvas.engine.audience.AudienceSnapshotService;
import org.chovy.canvas.engine.audience.AudienceUserResolver;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AudienceSnapshotServiceTest {

    @Test
    void lockSnapshotPersistsDistinctUsersAndMetadata() {
        AudienceUserResolver resolver = mock(AudienceUserResolver.class);
        AudienceSnapshotMapper snapshotMapper = mock(AudienceSnapshotMapper.class);
        when(resolver.resolve(101L)).thenReturn(List.of("u1", "u2", "u1", ""));
        AudienceSnapshotService service = new AudienceSnapshotService(
                resolver, snapshotMapper, mock(AudienceDefinitionMapper.class), new ObjectMapper(), 10_000);

        AudienceSnapshotDO snapshot = service.lockSnapshot(101L, 62L, 91L, "audience_node", "alice");

        ArgumentCaptor<AudienceSnapshotDO> captor = ArgumentCaptor.forClass(AudienceSnapshotDO.class);
        verify(snapshotMapper).insert(captor.capture());
        assertThat(captor.getValue().getAudienceId()).isEqualTo(101L);
        assertThat(captor.getValue().getCanvasId()).isEqualTo(62L);
        assertThat(captor.getValue().getCanvasVersionId()).isEqualTo(91L);
        assertThat(captor.getValue().getNodeId()).isEqualTo("audience_node");
        assertThat(captor.getValue().getUserCount()).isEqualTo(2L);
        assertThat(captor.getValue().getUserIdsJson()).contains("u1", "u2");
        assertThat(snapshot.getSnapshotMode()).isEqualTo("STATIC_LOCKED");
    }

    @Test
    void lockSnapshotRejectsOversizedAudience() {
        AudienceUserResolver resolver = mock(AudienceUserResolver.class);
        when(resolver.resolve(101L)).thenReturn(List.of("u1", "u2"));
        AudienceSnapshotService service = new AudienceSnapshotService(
                resolver, mock(AudienceSnapshotMapper.class), mock(AudienceDefinitionMapper.class), new ObjectMapper(), 1);

        assertThatThrownBy(() -> service.lockSnapshot(101L, 62L, 91L, "audience_node", "alice"))
                .hasMessageContaining("AUDIENCE_SNAPSHOT_LIMIT");
    }

    @Test
    void defaultModeFallsBackToStaticLockedWhenDefinitionIsBlank() {
        AudienceDefinitionMapper definitionMapper = mock(AudienceDefinitionMapper.class);
        AudienceDefinitionDO definition = new AudienceDefinitionDO();
        definition.setDefaultSnapshotMode("");
        when(definitionMapper.selectById(101L)).thenReturn(definition);
        AudienceSnapshotService service = new AudienceSnapshotService(
                mock(AudienceUserResolver.class),
                mock(AudienceSnapshotMapper.class),
                definitionMapper,
                new ObjectMapper(),
                10_000);

        assertThat(service.defaultModeForAudience(101L).name()).isEqualTo("STATIC_LOCKED");
    }
}
```

- [ ] **Step 2: Run service tests and confirm red state**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=AudienceSnapshotServiceTest
```

Expected: FAIL because `AudienceSnapshotService` does not exist.

- [ ] **Step 3: Implement snapshot service persistence**

Create `AudienceSnapshotService.java` with constructor injection for resolver, mappers, object mapper, and `maxSnapshotUsers`:

```java
@Service
public class AudienceSnapshotService {
    private final AudienceUserResolver userResolver;
    private final AudienceSnapshotMapper snapshotMapper;
    private final AudienceDefinitionMapper definitionMapper;
    private final ObjectMapper objectMapper;
    private final int maxSnapshotUsers;

    public AudienceSnapshotService(
            AudienceUserResolver userResolver,
            AudienceSnapshotMapper snapshotMapper,
            AudienceDefinitionMapper definitionMapper,
            ObjectMapper objectMapper,
            @Value("${canvas.audience.snapshot.max-users:100000}") int maxSnapshotUsers) {
        this.userResolver = userResolver;
        this.snapshotMapper = snapshotMapper;
        this.definitionMapper = definitionMapper;
        this.objectMapper = objectMapper;
        this.maxSnapshotUsers = maxSnapshotUsers;
    }

    public AudienceSnapshotDO lockSnapshot(Long audienceId, Long canvasId, Long canvasVersionId, String nodeId, String operator) {
        List<String> users = userResolver.resolve(audienceId).stream()
                .filter(userId -> userId != null && !userId.isBlank())
                .distinct()
                .toList();
        if (users.size() > maxSnapshotUsers) {
            throw new IllegalStateException("AUDIENCE_SNAPSHOT_LIMIT: audienceId=" + audienceId
                    + " size=" + users.size() + " max=" + maxSnapshotUsers);
        }
        AudienceSnapshotDO snapshot = new AudienceSnapshotDO();
        snapshot.setAudienceId(audienceId);
        snapshot.setCanvasId(canvasId);
        snapshot.setCanvasVersionId(canvasVersionId);
        snapshot.setNodeId(nodeId);
        snapshot.setSnapshotMode(AudienceSnapshotMode.STATIC_LOCKED.name());
        snapshot.setUserCount((long) users.size());
        snapshot.setUserIdsJson(writeJson(users));
        snapshot.setCreatedBy(operator);
        snapshot.setCreatedAt(LocalDateTime.now());
        snapshotMapper.insert(snapshot);
        return snapshot;
    }

    public AudienceSnapshotMode defaultModeForAudience(Long audienceId) {
        AudienceDefinitionDO definition = definitionMapper.selectById(audienceId);
        return AudienceSnapshotMode.normalize(definition == null ? null : definition.getDefaultSnapshotMode());
    }
}
```

Add a private `writeJson(Object value)` helper that wraps `objectMapper.writeValueAsString(value)` and throws `IllegalStateException("Audience snapshot serialization failed", e)`.

- [ ] **Step 4: Run service tests**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=AudienceSnapshotServiceTest
```

Expected: PASS.

### Task 2: Publish Graph Binding

**Files:**
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/canvas/CanvasPublishAudienceSnapshotTest.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/audience/AudienceSnapshotService.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/canvas/CanvasService.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/canvas/CanvasTransactionService.java`

- [ ] **Step 1: Write graph binding tests**

Add tests to `AudienceSnapshotServiceTest`:

```java
@Test
void bindGraphLocksStaticNodesAndRemovesDynamicStaleSnapshotIds() {
    AudienceUserResolver resolver = mock(AudienceUserResolver.class);
    AudienceSnapshotMapper snapshotMapper = mock(AudienceSnapshotMapper.class);
    when(resolver.resolve(101L)).thenReturn(List.of("u1"));
    AudienceSnapshotService service = new AudienceSnapshotService(
            resolver, snapshotMapper, mock(AudienceDefinitionMapper.class), new ObjectMapper(), 10_000);
    String graphJson = """
            {"nodes":[
              {"id":"static","type":"TAGGER","config":{"mode":"audience","audienceId":101,"audienceSnapshotMode":"STATIC_LOCKED"}},
              {"id":"dynamic","type":"TAGGER","config":{"mode":"audience","audienceId":102,"audienceSnapshotMode":"DYNAMIC_REFRESH","audienceSnapshotId":9}}
            ]}
            """;

    String bound = service.bindAudienceSnapshotsForPublish(62L, 91L, graphJson, "alice");

    assertThat(bound).contains("\"audienceSnapshotMode\":\"STATIC_LOCKED\"");
    assertThat(bound).contains("\"audienceSnapshotId\"");
    assertThat(bound).contains("\"audienceSnapshotMode\":\"DYNAMIC_REFRESH\"");
    assertThat(bound).doesNotContain("\"audienceSnapshotId\":9");
}
```

- [ ] **Step 2: Run graph binding test and confirm red state**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=AudienceSnapshotServiceTest
```

Expected: FAIL because `bindAudienceSnapshotsForPublish` does not exist.

- [ ] **Step 3: Implement graph binding**

Add to `AudienceSnapshotService`:

```java
@SuppressWarnings("unchecked")
public String bindAudienceSnapshotsForPublish(Long canvasId, Long versionId, String graphJson, String operator) {
    try {
        Map<String, Object> root = objectMapper.readValue(graphJson, new TypeReference<Map<String, Object>>() {});
        Object rawNodes = root.get("nodes");
        if (!(rawNodes instanceof List<?> nodes)) {
            return graphJson;
        }
        for (Object rawNode : nodes) {
            if (!(rawNode instanceof Map<?, ?> node)) {
                continue;
            }
            Map<String, Object> mutableNode = (Map<String, Object>) node;
            if (!"TAGGER".equals(String.valueOf(mutableNode.get("type")))) {
                continue;
            }
            Object rawConfig = mutableNode.get("config");
            if (!(rawConfig instanceof Map<?, ?> config)) {
                continue;
            }
            Map<String, Object> mutableConfig = (Map<String, Object>) config;
            if (!"audience".equals(String.valueOf(mutableConfig.get("mode")))) {
                continue;
            }
            Long audienceId = Long.valueOf(String.valueOf(mutableConfig.get("audienceId")));
            AudienceSnapshotMode mode = resolveNodeMode(mutableConfig, audienceId);
            mutableConfig.put("audienceSnapshotMode", mode.name());
            if (mode == AudienceSnapshotMode.STATIC_LOCKED) {
                AudienceSnapshotDO snapshot = lockSnapshot(audienceId, canvasId, versionId,
                        String.valueOf(mutableNode.get("id")), operator);
                mutableConfig.put("audienceSnapshotId", snapshot.getId());
            } else {
                mutableConfig.remove("audienceSnapshotId");
            }
        }
        return objectMapper.writeValueAsString(root);
    } catch (Exception e) {
        throw new IllegalStateException("Bind audience snapshots failed: " + e.getMessage(), e);
    }
}

private AudienceSnapshotMode resolveNodeMode(Map<String, Object> config, Long audienceId) {
    Object raw = config.get("audienceSnapshotMode");
    return raw == null || String.valueOf(raw).isBlank()
            ? defaultModeForAudience(audienceId)
            : AudienceSnapshotMode.normalize(String.valueOf(raw));
}
```

- [ ] **Step 4: Wire publish path**

In `CanvasService`, inject `AudienceSnapshotService` and transform draft graph JSON immediately before the publish transaction:

```java
String graphJsonForPublish = audienceSnapshotService.bindAudienceSnapshotsForPublish(
        id,
        draft.getId(),
        draft.getGraphJson(),
        operator);
DagGraph graph = dagParser.parse(graphJsonForPublish);
CanvasVersionDO version = canvasTransactionService.publishDb(id, graphJsonForPublish, operator);
```

Keep the existing draft version unchanged; only pass `graphJsonForPublish` into the published version creation.

- [ ] **Step 5: Add publish integration assertion**

Create `CanvasPublishAudienceSnapshotTest.java` with a mocked `CanvasTransactionService` that captures the graph passed to `publishDb`:

```java
@Test
void publishUsesBoundGraphWithoutWritingDraftGraph() {
    ArgumentCaptor<String> publishedGraph = ArgumentCaptor.forClass(String.class);
    when(snapshotService.bindAudienceSnapshotsForPublish(eq(62L), anyLong(), anyString(), eq("alice")))
            .thenReturn("{\"nodes\":[{\"id\":\"tag\",\"type\":\"TAGGER\",\"config\":{\"audienceSnapshotId\":501}}]}");
    when(transactionService.publishDb(eq(62L), publishedGraph.capture(), eq("alice")))
            .thenReturn(new CanvasVersionDO());

    canvasService.publish(62L, "alice");

    assertThat(publishedGraph.getValue()).contains("audienceSnapshotId");
    verify(versionMapper, never()).updateById(argThat(version -> version.getGraphJson() != null
            && version.getGraphJson().contains("audienceSnapshotId")));
}
```

Use the existing `CanvasService` test construction pattern in this repository for mappers and parser dependencies.

- [ ] **Step 6: Run publish snapshot tests**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=AudienceSnapshotServiceTest,CanvasPublishAudienceSnapshotTest
```

Expected: PASS.

### Task 3: Verification And Commit

**Files:**
- Read: `docs/product-evolution/specs/p1-003b-publish-time-audience-snapshot-locking.md`
- Read: `docs/product-evolution/plans/p1-003b-publish-time-audience-snapshot-locking-plan.md`

- [ ] **Step 1: Run focused backend tests**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=AudienceSnapshotServiceTest,CanvasPublishAudienceSnapshotTest
```

Expected: PASS.

- [ ] **Step 2: Commit this slice**

Run:

```bash
git add backend/canvas-engine/src/main/java/org/chovy/canvas/engine/audience/AudienceSnapshotService.java \
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/canvas/CanvasService.java \
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/canvas/CanvasTransactionService.java \
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/audience/AudienceSnapshotServiceTest.java \
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/canvas/CanvasPublishAudienceSnapshotTest.java \
  docs/product-evolution/specs/p1-003b-publish-time-audience-snapshot-locking.md \
  docs/product-evolution/plans/p1-003b-publish-time-audience-snapshot-locking-plan.md
git commit -m "feat: lock static audience snapshots on publish"
```

Expected: commit contains only publish-time snapshot locking behavior.
