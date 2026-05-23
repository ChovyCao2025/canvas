# ARCH-STD Backend Standardization Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Reorganize `backend/canvas-engine` into a large-company Java backend structure with service interfaces, `*DO` persistence objects, separated Mapper/XML packages, and clean controller-to-service dependencies without changing business behavior.

**Architecture:** Keep `canvas-engine` as a modular monolith. Use `web/service/service.impl/dal.dataobject/dal.mapper/dto/query/vo/engine/integration/infrastructure/auth/common/config` package boundaries, preserving the runtime engine as its own execution domain and leaving future control-plane/runtime-plane/insight-plane service decomposition as an evolution path.

**Tech Stack:** Java 21, Spring Boot 3, WebFlux controllers, MyBatis-Plus, MySQL/Flyway, Redis, RocketMQ, Maven.

---

## Pre-Execution Notes

- The working tree is currently dirty. Do not revert or overwrite unrelated user changes.
- Several active uncommitted files are part of the current backend surface, including `CanvasExecutionRequest`, `CanvasExecutionRequestMapper`, `CanvasExecutionRequestService`, `CanvasExecutionRequestManagementController`, `NotificationService`, and `AsyncTaskService`. Treat an item from that list as in-scope when the corresponding file is present during execution.
- Commit only the files touched by each task. Before each commit, run `git diff --cached --name-only` and verify it contains only the task's files.
- This plan intentionally does not change API paths, JSON field names, database tables, Flyway SQL, cache keys, MQ topics, or business branching.
- At the start of every terminal session that uses `move_file`, define this helper:

```bash
move_file() {
  src="$1"
  dst="$2"
  mkdir -p "$(dirname "$dst")"
  if [ ! -e "$src" ]; then
    echo "skip missing $src"
    return 0
  fi
  if git ls-files --error-unmatch "$src" >/dev/null 2>&1; then
    git mv "$src" "$dst"
  else
    mv "$src" "$dst"
  fi
}
```

## File Structure Map

Create or converge toward this structure:

```text
backend/canvas-engine/src/main/java/org/chovy/canvas/
  web/
  service/
  service/impl/
  dal/dataobject/
  dal/mapper/
  dto/
  query/
  vo/
  engine/
  integration/
  infrastructure/
  auth/
  common/
  config/
```

Resource XML target:

```text
backend/canvas-engine/src/main/resources/mapper/
  approval/
  audience/
  auth/
  canvas/
  execution/
  meta/
```

Primary data objects to rename if present:

```text
Canvas -> CanvasDO
CanvasVersion -> CanvasVersionDO
CanvasTemplate -> CanvasTemplateDO
CanvasManualApproval -> CanvasManualApprovalDO
AudienceDefinition -> AudienceDefinitionDO
AudienceStat -> AudienceStatDO
CanvasExecution -> CanvasExecutionDO
CanvasExecutionDlq -> CanvasExecutionDlqDO
CanvasExecutionRequest -> CanvasExecutionRequestDO
CanvasExecutionStats -> CanvasExecutionStatsDO
CanvasExecutionTrace -> CanvasExecutionTraceDO
CanvasUserQuota -> CanvasUserQuotaDO
AbExperiment -> AbExperimentDO
ApiDefinition -> ApiDefinitionDO
ContextField -> ContextFieldDO
EventDefinition -> EventDefinitionDO
EventLog -> EventLogDO
MqMessageDefinition -> MqMessageDefinitionDO
NodeTypeRegistry -> NodeTypeRegistryDO
TagDefinition -> TagDefinitionDO
SysUser -> SysUserDO
```

Primary Mapper files to move if present:

```text
CanvasMapper, CanvasVersionMapper, CanvasTemplateMapper
CanvasManualApprovalMapper
AudienceDefinitionMapper, AudienceStatMapper
CanvasExecutionMapper, CanvasExecutionDlqMapper, CanvasExecutionRequestMapper, CanvasExecutionStatsMapper, CanvasExecutionTraceMapper, CanvasUserQuotaMapper
AbExperimentMapper, ApiDefinitionMapper, ContextFieldMapper, EventDefinitionMapper, EventLogMapper, MqMessageDefinitionMapper, NodeTypeRegistryMapper, TagDefinitionMapper
SysUserMapper
```

---

### Task 1: Establish Baseline and Guardrails

**Files:**
- Read: `docs/superpowers/specs/2026-05-23-ARCH-STD-backend-standardization-design.md`
- Read: current `git status`
- No code changes

- [ ] **Step 1: Re-read the approved spec**

Run:

```bash
sed -n '1,360p' docs/superpowers/specs/2026-05-23-ARCH-STD-backend-standardization-design.md
```

Expected: The output includes `ARCH-STD`, `web/service/dal`, `*DO`, Mapper XML, and “不改业务逻辑”.

- [ ] **Step 2: Record current dirty state outside the repo**

Run:

```bash
git status --short > /tmp/arch-std-pre-status.txt
cat /tmp/arch-std-pre-status.txt
```

Expected: The output may include unrelated modified and untracked files. Keep this file only as a local guardrail; do not commit it.

- [ ] **Step 3: Run the backend baseline test**

Run:

```bash
mvn -pl canvas-engine -am test
```

Expected: Ideally `BUILD SUCCESS`. If it fails before refactoring, copy the failing test names into `/tmp/arch-std-baseline-failures.txt` and continue only after confirming the failures are unrelated to ARCH-STD.

- [ ] **Step 4: Verify no files are staged**

Run:

```bash
git diff --cached --name-only
```

Expected: no output.

---

### Task 2: Update MyBatis XML Scanning and Create Mapper Resource Layout

**Files:**
- Modify: `backend/canvas-engine/src/main/resources/application.yml`
- Move: `backend/canvas-engine/src/main/resources/mapper/CanvasMapper.xml`
- Move: `backend/canvas-engine/src/main/resources/mapper/CanvasExecutionTraceMapper.xml`
- Create: empty XML files under `backend/canvas-engine/src/main/resources/mapper/**`

- [ ] **Step 1: Change mapper scanning to recursive**

Apply this patch:

```diff
*** Begin Patch
*** Update File: backend/canvas-engine/src/main/resources/application.yml
@@
 mybatis-plus:
-  mapper-locations: classpath:mapper/*.xml
+  mapper-locations: classpath*:mapper/**/*.xml
*** End Patch
```

- [ ] **Step 2: Create Mapper XML directories**

Run:

```bash
mkdir -p \
  backend/canvas-engine/src/main/resources/mapper/approval \
  backend/canvas-engine/src/main/resources/mapper/audience \
  backend/canvas-engine/src/main/resources/mapper/auth \
  backend/canvas-engine/src/main/resources/mapper/canvas \
  backend/canvas-engine/src/main/resources/mapper/execution \
  backend/canvas-engine/src/main/resources/mapper/meta
```

Expected: directories exist.

- [ ] **Step 3: Move existing XML files into domain directories**

Run:

```bash
git mv backend/canvas-engine/src/main/resources/mapper/CanvasMapper.xml \
  backend/canvas-engine/src/main/resources/mapper/canvas/CanvasMapper.xml

git mv backend/canvas-engine/src/main/resources/mapper/CanvasExecutionTraceMapper.xml \
  backend/canvas-engine/src/main/resources/mapper/execution/CanvasExecutionTraceMapper.xml
```

Expected: both `git mv` commands succeed.

- [ ] **Step 4: Add XML files for every Mapper**

For each file that does not already exist, create it with `apply_patch` using this exact content pattern. Example for `CanvasVersionMapper.xml`:

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE mapper
        PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="org.chovy.canvas.dal.mapper.CanvasVersionMapper">
</mapper>
```

Create these files:

```text
backend/canvas-engine/src/main/resources/mapper/approval/CanvasManualApprovalMapper.xml
backend/canvas-engine/src/main/resources/mapper/audience/AudienceDefinitionMapper.xml
backend/canvas-engine/src/main/resources/mapper/audience/AudienceStatMapper.xml
backend/canvas-engine/src/main/resources/mapper/auth/SysUserMapper.xml
backend/canvas-engine/src/main/resources/mapper/canvas/CanvasTemplateMapper.xml
backend/canvas-engine/src/main/resources/mapper/canvas/CanvasVersionMapper.xml
backend/canvas-engine/src/main/resources/mapper/execution/CanvasExecutionDlqMapper.xml
backend/canvas-engine/src/main/resources/mapper/execution/CanvasExecutionMapper.xml
backend/canvas-engine/src/main/resources/mapper/execution/CanvasExecutionRequestMapper.xml
backend/canvas-engine/src/main/resources/mapper/execution/CanvasExecutionStatsMapper.xml
backend/canvas-engine/src/main/resources/mapper/execution/CanvasUserQuotaMapper.xml
backend/canvas-engine/src/main/resources/mapper/meta/AbExperimentMapper.xml
backend/canvas-engine/src/main/resources/mapper/meta/ApiDefinitionMapper.xml
backend/canvas-engine/src/main/resources/mapper/meta/ContextFieldMapper.xml
backend/canvas-engine/src/main/resources/mapper/meta/EventDefinitionMapper.xml
backend/canvas-engine/src/main/resources/mapper/meta/EventLogMapper.xml
backend/canvas-engine/src/main/resources/mapper/meta/MqMessageDefinitionMapper.xml
backend/canvas-engine/src/main/resources/mapper/meta/NodeTypeRegistryMapper.xml
backend/canvas-engine/src/main/resources/mapper/meta/TagDefinitionMapper.xml
```

Create `CanvasExecutionRequestMapper.xml` when `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/execution/CanvasExecutionRequestMapper.java` exists. When that Java file is absent, omit only `CanvasExecutionRequestMapper.xml` and keep the rest of the XML list unchanged.

- [ ] **Step 5: Update existing XML namespaces**

Modify:

```xml
<mapper namespace="org.chovy.canvas.domain.canvas.CanvasMapper">
```

to:

```xml
<mapper namespace="org.chovy.canvas.dal.mapper.CanvasMapper">
```

Modify:

```xml
<mapper namespace="org.chovy.canvas.domain.execution.CanvasExecutionTraceMapper">
```

to:

```xml
<mapper namespace="org.chovy.canvas.dal.mapper.CanvasExecutionTraceMapper">
```

- [ ] **Step 6: Verify XML inventory**

Run:

```bash
find backend/canvas-engine/src/main/resources/mapper -name '*Mapper.xml' | sort
rg 'namespace="org.chovy.canvas.domain' backend/canvas-engine/src/main/resources/mapper
```

Expected: all Mapper XML files are listed; second command returns no matches.

- [ ] **Step 7: Compile quick feedback**

Run:

```bash
mvn -pl canvas-engine -am -DskipTests compile
```

Expected: it may fail because Java packages are not moved yet. Only continue if failures are Java package/type errors related to the upcoming tasks.

- [ ] **Step 8: Commit XML layout**

Run:

```bash
git add backend/canvas-engine/src/main/resources/application.yml backend/canvas-engine/src/main/resources/mapper
git diff --cached --name-only
git commit -m "refactor: standardize mapper xml layout"
```

Expected: staged files are only `application.yml` and Mapper XML files.

---

### Task 3: Move Persistence Objects and Mappers into DAL

**Files:**
- Move: current `domain/**` table objects to `dal/dataobject/*DO.java`
- Move: current `domain/**Mapper.java` and `auth/domain/SysUserMapper.java` to `dal/mapper`
- Modify: moved Java package declarations and class names

- [ ] **Step 1: Create DAL directories**

Run:

```bash
mkdir -p \
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject \
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper
```

- [ ] **Step 2: Move files with tracked/untracked safety**

Run the `move_file` helper from Pre-Execution Notes, then run:

```bash
move_file backend/canvas-engine/src/main/java/org/chovy/canvas/domain/canvas/Canvas.java backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/CanvasDO.java
move_file backend/canvas-engine/src/main/java/org/chovy/canvas/domain/canvas/CanvasVersion.java backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/CanvasVersionDO.java
move_file backend/canvas-engine/src/main/java/org/chovy/canvas/domain/canvas/CanvasTemplate.java backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/CanvasTemplateDO.java
move_file backend/canvas-engine/src/main/java/org/chovy/canvas/domain/approval/CanvasManualApproval.java backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/CanvasManualApprovalDO.java
move_file backend/canvas-engine/src/main/java/org/chovy/canvas/domain/audience/AudienceDefinition.java backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/AudienceDefinitionDO.java
move_file backend/canvas-engine/src/main/java/org/chovy/canvas/domain/audience/AudienceStat.java backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/AudienceStatDO.java
move_file backend/canvas-engine/src/main/java/org/chovy/canvas/domain/execution/CanvasExecution.java backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/CanvasExecutionDO.java
move_file backend/canvas-engine/src/main/java/org/chovy/canvas/domain/execution/CanvasExecutionDlq.java backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/CanvasExecutionDlqDO.java
move_file backend/canvas-engine/src/main/java/org/chovy/canvas/domain/execution/CanvasExecutionRequest.java backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/CanvasExecutionRequestDO.java
move_file backend/canvas-engine/src/main/java/org/chovy/canvas/domain/execution/CanvasExecutionStats.java backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/CanvasExecutionStatsDO.java
move_file backend/canvas-engine/src/main/java/org/chovy/canvas/domain/execution/CanvasExecutionTrace.java backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/CanvasExecutionTraceDO.java
move_file backend/canvas-engine/src/main/java/org/chovy/canvas/domain/execution/CanvasUserQuota.java backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/CanvasUserQuotaDO.java
move_file backend/canvas-engine/src/main/java/org/chovy/canvas/domain/meta/AbExperiment.java backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/AbExperimentDO.java
move_file backend/canvas-engine/src/main/java/org/chovy/canvas/domain/meta/ApiDefinition.java backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/ApiDefinitionDO.java
move_file backend/canvas-engine/src/main/java/org/chovy/canvas/domain/meta/ContextField.java backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/ContextFieldDO.java
move_file backend/canvas-engine/src/main/java/org/chovy/canvas/domain/meta/EventDefinition.java backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/EventDefinitionDO.java
move_file backend/canvas-engine/src/main/java/org/chovy/canvas/domain/meta/EventLog.java backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/EventLogDO.java
move_file backend/canvas-engine/src/main/java/org/chovy/canvas/domain/meta/MqMessageDefinition.java backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/MqMessageDefinitionDO.java
move_file backend/canvas-engine/src/main/java/org/chovy/canvas/domain/meta/NodeTypeRegistry.java backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/NodeTypeRegistryDO.java
move_file backend/canvas-engine/src/main/java/org/chovy/canvas/domain/meta/TagDefinition.java backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/TagDefinitionDO.java
move_file backend/canvas-engine/src/main/java/org/chovy/canvas/auth/domain/SysUser.java backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/SysUserDO.java
```

- [ ] **Step 3: Move Mapper files**

Run:

```bash
move_file backend/canvas-engine/src/main/java/org/chovy/canvas/domain/canvas/CanvasMapper.java backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/CanvasMapper.java
move_file backend/canvas-engine/src/main/java/org/chovy/canvas/domain/canvas/CanvasVersionMapper.java backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/CanvasVersionMapper.java
move_file backend/canvas-engine/src/main/java/org/chovy/canvas/domain/canvas/CanvasTemplateMapper.java backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/CanvasTemplateMapper.java
move_file backend/canvas-engine/src/main/java/org/chovy/canvas/domain/approval/CanvasManualApprovalMapper.java backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/CanvasManualApprovalMapper.java
move_file backend/canvas-engine/src/main/java/org/chovy/canvas/domain/audience/AudienceDefinitionMapper.java backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/AudienceDefinitionMapper.java
move_file backend/canvas-engine/src/main/java/org/chovy/canvas/domain/audience/AudienceStatMapper.java backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/AudienceStatMapper.java
move_file backend/canvas-engine/src/main/java/org/chovy/canvas/domain/execution/CanvasExecutionMapper.java backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/CanvasExecutionMapper.java
move_file backend/canvas-engine/src/main/java/org/chovy/canvas/domain/execution/CanvasExecutionDlqMapper.java backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/CanvasExecutionDlqMapper.java
move_file backend/canvas-engine/src/main/java/org/chovy/canvas/domain/execution/CanvasExecutionRequestMapper.java backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/CanvasExecutionRequestMapper.java
move_file backend/canvas-engine/src/main/java/org/chovy/canvas/domain/execution/CanvasExecutionStatsMapper.java backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/CanvasExecutionStatsMapper.java
move_file backend/canvas-engine/src/main/java/org/chovy/canvas/domain/execution/CanvasExecutionTraceMapper.java backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/CanvasExecutionTraceMapper.java
move_file backend/canvas-engine/src/main/java/org/chovy/canvas/domain/execution/CanvasUserQuotaMapper.java backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/CanvasUserQuotaMapper.java
move_file backend/canvas-engine/src/main/java/org/chovy/canvas/domain/meta/AbExperimentMapper.java backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/AbExperimentMapper.java
move_file backend/canvas-engine/src/main/java/org/chovy/canvas/domain/meta/ApiDefinitionMapper.java backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/ApiDefinitionMapper.java
move_file backend/canvas-engine/src/main/java/org/chovy/canvas/domain/meta/ContextFieldMapper.java backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/ContextFieldMapper.java
move_file backend/canvas-engine/src/main/java/org/chovy/canvas/domain/meta/EventDefinitionMapper.java backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/EventDefinitionMapper.java
move_file backend/canvas-engine/src/main/java/org/chovy/canvas/domain/meta/EventLogMapper.java backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/EventLogMapper.java
move_file backend/canvas-engine/src/main/java/org/chovy/canvas/domain/meta/MqMessageDefinitionMapper.java backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/MqMessageDefinitionMapper.java
move_file backend/canvas-engine/src/main/java/org/chovy/canvas/domain/meta/NodeTypeRegistryMapper.java backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/NodeTypeRegistryMapper.java
move_file backend/canvas-engine/src/main/java/org/chovy/canvas/domain/meta/TagDefinitionMapper.java backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/TagDefinitionMapper.java
move_file backend/canvas-engine/src/main/java/org/chovy/canvas/auth/domain/SysUserMapper.java backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/SysUserMapper.java
```

- [ ] **Step 4: Rewrite package declarations**

Run:

```bash
perl -pi -e 's/^package org\\.chovy\\.canvas\\.(domain\\.[a-z]+|auth\\.domain);/package org.chovy.canvas.dal.dataobject;/' backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/*.java
perl -pi -e 's/^package org\\.chovy\\.canvas\\.(domain\\.[a-z]+|auth\\.domain);/package org.chovy.canvas.dal.mapper;/' backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/*.java
```

- [ ] **Step 5: Rename public class declarations inside DO files**

Run:

```bash
perl -pi -e 's/public class Canvas\\b/public class CanvasDO/' backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/CanvasDO.java
perl -pi -e 's/public class CanvasVersion\\b/public class CanvasVersionDO/' backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/CanvasVersionDO.java
perl -pi -e 's/public class CanvasTemplate\\b/public class CanvasTemplateDO/' backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/CanvasTemplateDO.java
perl -pi -e 's/public class CanvasManualApproval\\b/public class CanvasManualApprovalDO/' backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/CanvasManualApprovalDO.java
perl -pi -e 's/public class AudienceDefinition\\b/public class AudienceDefinitionDO/' backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/AudienceDefinitionDO.java
perl -pi -e 's/public class AudienceStat\\b/public class AudienceStatDO/' backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/AudienceStatDO.java
perl -pi -e 's/public class CanvasExecution\\b/public class CanvasExecutionDO/' backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/CanvasExecutionDO.java
perl -pi -e 's/public class CanvasExecutionDlq\\b/public class CanvasExecutionDlqDO/' backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/CanvasExecutionDlqDO.java
perl -pi -e 's/public class CanvasExecutionRequest\\b/public class CanvasExecutionRequestDO/' backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/CanvasExecutionRequestDO.java
perl -pi -e 's/public class CanvasExecutionStats\\b/public class CanvasExecutionStatsDO/' backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/CanvasExecutionStatsDO.java
perl -pi -e 's/public class CanvasExecutionTrace\\b/public class CanvasExecutionTraceDO/' backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/CanvasExecutionTraceDO.java
perl -pi -e 's/public class CanvasUserQuota\\b/public class CanvasUserQuotaDO/' backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/CanvasUserQuotaDO.java
perl -pi -e 's/public class AbExperiment\\b/public class AbExperimentDO/' backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/AbExperimentDO.java
perl -pi -e 's/public class ApiDefinition\\b/public class ApiDefinitionDO/' backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/ApiDefinitionDO.java
perl -pi -e 's/public class ContextField\\b/public class ContextFieldDO/' backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/ContextFieldDO.java
perl -pi -e 's/public class EventDefinition\\b/public class EventDefinitionDO/' backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/EventDefinitionDO.java
perl -pi -e 's/public class EventLog\\b/public class EventLogDO/' backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/EventLogDO.java
perl -pi -e 's/public class MqMessageDefinition\\b/public class MqMessageDefinitionDO/' backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/MqMessageDefinitionDO.java
perl -pi -e 's/public class NodeTypeRegistry\\b/public class NodeTypeRegistryDO/' backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/NodeTypeRegistryDO.java
perl -pi -e 's/public class TagDefinition\\b/public class TagDefinitionDO/' backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/TagDefinitionDO.java
perl -pi -e 's/public class SysUser\\b/public class SysUserDO/' backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/SysUserDO.java
```

- [ ] **Step 6: Update Mapper generic types**

Run:

```bash
perl -pi -e 's/BaseMapper<Canvas>/BaseMapper<CanvasDO>/g; s/BaseMapper<CanvasVersion>/BaseMapper<CanvasVersionDO>/g; s/BaseMapper<CanvasTemplate>/BaseMapper<CanvasTemplateDO>/g; s/BaseMapper<CanvasManualApproval>/BaseMapper<CanvasManualApprovalDO>/g; s/BaseMapper<AudienceDefinition>/BaseMapper<AudienceDefinitionDO>/g; s/BaseMapper<AudienceStat>/BaseMapper<AudienceStatDO>/g; s/BaseMapper<CanvasExecution>/BaseMapper<CanvasExecutionDO>/g; s/BaseMapper<CanvasExecutionDlq>/BaseMapper<CanvasExecutionDlqDO>/g; s/BaseMapper<CanvasExecutionRequest>/BaseMapper<CanvasExecutionRequestDO>/g; s/BaseMapper<CanvasExecutionStats>/BaseMapper<CanvasExecutionStatsDO>/g; s/BaseMapper<CanvasExecutionTrace>/BaseMapper<CanvasExecutionTraceDO>/g; s/BaseMapper<CanvasUserQuota>/BaseMapper<CanvasUserQuotaDO>/g; s/BaseMapper<AbExperiment>/BaseMapper<AbExperimentDO>/g; s/BaseMapper<ApiDefinition>/BaseMapper<ApiDefinitionDO>/g; s/BaseMapper<ContextField>/BaseMapper<ContextFieldDO>/g; s/BaseMapper<EventDefinition>/BaseMapper<EventDefinitionDO>/g; s/BaseMapper<EventLog>/BaseMapper<EventLogDO>/g; s/BaseMapper<MqMessageDefinition>/BaseMapper<MqMessageDefinitionDO>/g; s/BaseMapper<NodeTypeRegistry>/BaseMapper<NodeTypeRegistryDO>/g; s/BaseMapper<TagDefinition>/BaseMapper<TagDefinitionDO>/g; s/BaseMapper<SysUser>/BaseMapper<SysUserDO>/g' backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/*.java
```

Then add the single import line below `package org.chovy.canvas.dal.mapper;` in every mapper that needs a DO type:

```java
import org.chovy.canvas.dal.dataobject.*;
```

- [ ] **Step 7: Run package-wide type replacement**

Run:

```bash
find backend/canvas-engine/src/main/java backend/canvas-engine/src/test/java -name '*.java' -print0 | xargs -0 perl -pi -e '
s/org\\.chovy\\.canvas\\.domain\\.canvas\\.CanvasVersionMapper/org.chovy.canvas.dal.mapper.CanvasVersionMapper/g;
s/org\\.chovy\\.canvas\\.domain\\.canvas\\.CanvasTemplateMapper/org.chovy.canvas.dal.mapper.CanvasTemplateMapper/g;
s/org\\.chovy\\.canvas\\.domain\\.canvas\\.CanvasMapper/org.chovy.canvas.dal.mapper.CanvasMapper/g;
s/org\\.chovy\\.canvas\\.domain\\.approval\\.CanvasManualApprovalMapper/org.chovy.canvas.dal.mapper.CanvasManualApprovalMapper/g;
s/org\\.chovy\\.canvas\\.domain\\.audience\\.AudienceDefinitionMapper/org.chovy.canvas.dal.mapper.AudienceDefinitionMapper/g;
s/org\\.chovy\\.canvas\\.domain\\.audience\\.AudienceStatMapper/org.chovy.canvas.dal.mapper.AudienceStatMapper/g;
s/org\\.chovy\\.canvas\\.domain\\.execution\\.CanvasExecutionDlqMapper/org.chovy.canvas.dal.mapper.CanvasExecutionDlqMapper/g;
s/org\\.chovy\\.canvas\\.domain\\.execution\\.CanvasExecutionRequestMapper/org.chovy.canvas.dal.mapper.CanvasExecutionRequestMapper/g;
s/org\\.chovy\\.canvas\\.domain\\.execution\\.CanvasExecutionStatsMapper/org.chovy.canvas.dal.mapper.CanvasExecutionStatsMapper/g;
s/org\\.chovy\\.canvas\\.domain\\.execution\\.CanvasExecutionTraceMapper/org.chovy.canvas.dal.mapper.CanvasExecutionTraceMapper/g;
s/org\\.chovy\\.canvas\\.domain\\.execution\\.CanvasExecutionMapper/org.chovy.canvas.dal.mapper.CanvasExecutionMapper/g;
s/org\\.chovy\\.canvas\\.domain\\.execution\\.CanvasUserQuotaMapper/org.chovy.canvas.dal.mapper.CanvasUserQuotaMapper/g;
s/org\\.chovy\\.canvas\\.domain\\.meta\\.AbExperimentMapper/org.chovy.canvas.dal.mapper.AbExperimentMapper/g;
s/org\\.chovy\\.canvas\\.domain\\.meta\\.ApiDefinitionMapper/org.chovy.canvas.dal.mapper.ApiDefinitionMapper/g;
s/org\\.chovy\\.canvas\\.domain\\.meta\\.ContextFieldMapper/org.chovy.canvas.dal.mapper.ContextFieldMapper/g;
s/org\\.chovy\\.canvas\\.domain\\.meta\\.EventDefinitionMapper/org.chovy.canvas.dal.mapper.EventDefinitionMapper/g;
s/org\\.chovy\\.canvas\\.domain\\.meta\\.EventLogMapper/org.chovy.canvas.dal.mapper.EventLogMapper/g;
s/org\\.chovy\\.canvas\\.domain\\.meta\\.MqMessageDefinitionMapper/org.chovy.canvas.dal.mapper.MqMessageDefinitionMapper/g;
s/org\\.chovy\\.canvas\\.domain\\.meta\\.NodeTypeRegistryMapper/org.chovy.canvas.dal.mapper.NodeTypeRegistryMapper/g;
s/org\\.chovy\\.canvas\\.domain\\.meta\\.TagDefinitionMapper/org.chovy.canvas.dal.mapper.TagDefinitionMapper/g;
s/org\\.chovy\\.canvas\\.auth\\.domain\\.SysUserMapper/org.chovy.canvas.dal.mapper.SysUserMapper/g;
s/org\\.chovy\\.canvas\\.domain\\.canvas\\.CanvasVersion/org.chovy.canvas.dal.dataobject.CanvasVersionDO/g;
s/org\\.chovy\\.canvas\\.domain\\.canvas\\.CanvasTemplate/org.chovy.canvas.dal.dataobject.CanvasTemplateDO/g;
s/org\\.chovy\\.canvas\\.domain\\.canvas\\.Canvas/org.chovy.canvas.dal.dataobject.CanvasDO/g;
s/org\\.chovy\\.canvas\\.domain\\.approval\\.CanvasManualApproval/org.chovy.canvas.dal.dataobject.CanvasManualApprovalDO/g;
s/org\\.chovy\\.canvas\\.domain\\.audience\\.AudienceDefinition/org.chovy.canvas.dal.dataobject.AudienceDefinitionDO/g;
s/org\\.chovy\\.canvas\\.domain\\.audience\\.AudienceStat/org.chovy.canvas.dal.dataobject.AudienceStatDO/g;
s/org\\.chovy\\.canvas\\.domain\\.execution\\.CanvasExecutionDlq/org.chovy.canvas.dal.dataobject.CanvasExecutionDlqDO/g;
s/org\\.chovy\\.canvas\\.domain\\.execution\\.CanvasExecutionRequest/org.chovy.canvas.dal.dataobject.CanvasExecutionRequestDO/g;
s/org\\.chovy\\.canvas\\.domain\\.execution\\.CanvasExecutionStats/org.chovy.canvas.dal.dataobject.CanvasExecutionStatsDO/g;
s/org\\.chovy\\.canvas\\.domain\\.execution\\.CanvasExecutionTrace/org.chovy.canvas.dal.dataobject.CanvasExecutionTraceDO/g;
s/org\\.chovy\\.canvas\\.domain\\.execution\\.CanvasExecution/org.chovy.canvas.dal.dataobject.CanvasExecutionDO/g;
s/org\\.chovy\\.canvas\\.domain\\.execution\\.CanvasUserQuota/org.chovy.canvas.dal.dataobject.CanvasUserQuotaDO/g;
s/org\\.chovy\\.canvas\\.domain\\.meta\\.AbExperiment/org.chovy.canvas.dal.dataobject.AbExperimentDO/g;
s/org\\.chovy\\.canvas\\.domain\\.meta\\.ApiDefinition/org.chovy.canvas.dal.dataobject.ApiDefinitionDO/g;
s/org\\.chovy\\.canvas\\.domain\\.meta\\.ContextField/org.chovy.canvas.dal.dataobject.ContextFieldDO/g;
s/org\\.chovy\\.canvas\\.domain\\.meta\\.EventDefinition/org.chovy.canvas.dal.dataobject.EventDefinitionDO/g;
s/org\\.chovy\\.canvas\\.domain\\.meta\\.EventLog/org.chovy.canvas.dal.dataobject.EventLogDO/g;
s/org\\.chovy\\.canvas\\.domain\\.meta\\.MqMessageDefinition/org.chovy.canvas.dal.dataobject.MqMessageDefinitionDO/g;
s/org\\.chovy\\.canvas\\.domain\\.meta\\.NodeTypeRegistry/org.chovy.canvas.dal.dataobject.NodeTypeRegistryDO/g;
s/org\\.chovy\\.canvas\\.domain\\.meta\\.TagDefinition/org.chovy.canvas.dal.dataobject.TagDefinitionDO/g;
s/org\\.chovy\\.canvas\\.auth\\.domain\\.SysUser/org.chovy.canvas.dal.dataobject.SysUserDO/g;
'
```

- [ ] **Step 8: Replace simple type names after imports**

Run:

```bash
find backend/canvas-engine/src/main/java backend/canvas-engine/src/test/java -name '*.java' -print0 | xargs -0 perl -pi -e '
s/\\bCanvasVersion\\b/CanvasVersionDO/g;
s/\\bCanvasTemplate\\b/CanvasTemplateDO/g;
s/\\bCanvasManualApproval\\b/CanvasManualApprovalDO/g;
s/\\bAudienceDefinition\\b/AudienceDefinitionDO/g;
s/\\bAudienceStat\\b/AudienceStatDO/g;
s/\\bCanvasExecutionDlq\\b/CanvasExecutionDlqDO/g;
s/\\bCanvasExecutionRequest\\b/CanvasExecutionRequestDO/g;
s/\\bCanvasExecutionStats\\b/CanvasExecutionStatsDO/g;
s/\\bCanvasExecutionTrace\\b/CanvasExecutionTraceDO/g;
s/\\bCanvasUserQuota\\b/CanvasUserQuotaDO/g;
s/\\bAbExperiment\\b/AbExperimentDO/g;
s/\\bApiDefinition\\b/ApiDefinitionDO/g;
s/\\bContextField\\b/ContextFieldDO/g;
s/\\bEventDefinition\\b/EventDefinitionDO/g;
s/\\bEventLog\\b/EventLogDO/g;
s/\\bMqMessageDefinition\\b/MqMessageDefinitionDO/g;
s/\\bNodeTypeRegistry\\b/NodeTypeRegistryDO/g;
s/\\bTagDefinition\\b/TagDefinitionDO/g;
s/\\bSysUser\\b/SysUserDO/g;
s/\\bCanvasExecution\\b/CanvasExecutionDO/g;
s/\\bCanvas\\b/CanvasDO/g;
'
```

Immediately inspect and fix false positives in comments, enum names, strings, and DTO names. The most important false-positive check is:

```bash
rg 'CanvasDODO|CanvasStatusEnumDO|CanvasDetailDTODO|CanvasCreateReqDO|CanvasUpdateReqDO|CanvasListQueryDO|CanvasMetricsDO|CanvasEngineApplicationDO' backend/canvas-engine/src/main/java backend/canvas-engine/src/test/java
```

Expected: no output. If any output appears, revert only the incorrect token in that file.

- [ ] **Step 9: Compile and resolve import/type errors**

Run:

```bash
mvn -pl canvas-engine -am -DskipTests compile
```

Expected: compile may fail with missing imports or accidental replacements. Change only package names, imports, and renamed type identifiers. Do not alter method bodies beyond renamed types.

- [ ] **Step 10: Commit DAL move**

Run:

```bash
git add backend/canvas-engine/src/main/java backend/canvas-engine/src/test/java backend/canvas-engine/src/main/resources/mapper
git diff --cached --name-only
git commit -m "refactor: move persistence types into dal"
```

Expected: staged files are Java package/type updates and Mapper XML namespace updates.

---

### Task 4: Move Constants, DTO Helpers, Auth Utility, and Infrastructure Packages

**Files:**
- Move: `domain/constant/*.java` to `common/enums`
- Move: `domain/meta/StubOption.java` to `dto/StubOption.java`
- Move: `auth/domain/JwtUtil.java` to `auth/util/JwtUtil.java`
- Move: `infra/**` to `infrastructure/**`
- Update imports

- [ ] **Step 1: Move non-DO support classes**

Run:

```bash
mkdir -p backend/canvas-engine/src/main/java/org/chovy/canvas/common/enums
mkdir -p backend/canvas-engine/src/main/java/org/chovy/canvas/auth/util
mkdir -p backend/canvas-engine/src/main/java/org/chovy/canvas/infrastructure

move_file backend/canvas-engine/src/main/java/org/chovy/canvas/domain/constant/ApprovalOnTimeoutAction.java backend/canvas-engine/src/main/java/org/chovy/canvas/common/enums/ApprovalOnTimeoutAction.java
move_file backend/canvas-engine/src/main/java/org/chovy/canvas/domain/constant/ApprovalStatus.java backend/canvas-engine/src/main/java/org/chovy/canvas/common/enums/ApprovalStatus.java
move_file backend/canvas-engine/src/main/java/org/chovy/canvas/domain/constant/CanvasStatusEnum.java backend/canvas-engine/src/main/java/org/chovy/canvas/common/enums/CanvasStatusEnum.java
move_file backend/canvas-engine/src/main/java/org/chovy/canvas/domain/constant/ExecutionStatus.java backend/canvas-engine/src/main/java/org/chovy/canvas/common/enums/ExecutionStatus.java
move_file backend/canvas-engine/src/main/java/org/chovy/canvas/domain/constant/NodeType.java backend/canvas-engine/src/main/java/org/chovy/canvas/common/enums/NodeType.java
move_file backend/canvas-engine/src/main/java/org/chovy/canvas/domain/constant/ScheduleType.java backend/canvas-engine/src/main/java/org/chovy/canvas/common/enums/ScheduleType.java
move_file backend/canvas-engine/src/main/java/org/chovy/canvas/domain/constant/TriggerType.java backend/canvas-engine/src/main/java/org/chovy/canvas/common/enums/TriggerType.java
move_file backend/canvas-engine/src/main/java/org/chovy/canvas/domain/constant/VersionStatus.java backend/canvas-engine/src/main/java/org/chovy/canvas/common/enums/VersionStatus.java
move_file backend/canvas-engine/src/main/java/org/chovy/canvas/domain/meta/StubOption.java backend/canvas-engine/src/main/java/org/chovy/canvas/dto/StubOption.java
move_file backend/canvas-engine/src/main/java/org/chovy/canvas/auth/domain/JwtUtil.java backend/canvas-engine/src/main/java/org/chovy/canvas/auth/util/JwtUtil.java
```

- [ ] **Step 2: Move infrastructure package**

Run:

```bash
mkdir -p backend/canvas-engine/src/main/java/org/chovy/canvas/infrastructure
if [ -d backend/canvas-engine/src/main/java/org/chovy/canvas/infra ]; then
  find backend/canvas-engine/src/main/java/org/chovy/canvas/infra -type f -name '*.java' | while read -r src; do
    dst="${src/src\\/main\\/java\\/org\\/chovy\\/canvas\\/infra/src\\/main\\/java\\/org\\/chovy\\/canvas\\/infrastructure}"
    move_file "$src" "$dst"
  done
fi
```

- [ ] **Step 3: Rewrite package declarations and imports**

Run:

```bash
find backend/canvas-engine/src/main/java backend/canvas-engine/src/test/java -name '*.java' -print0 | xargs -0 perl -pi -e '
s/package org\\.chovy\\.canvas\\.domain\\.constant;/package org.chovy.canvas.common.enums;/g;
s/org\\.chovy\\.canvas\\.domain\\.constant\\./org.chovy.canvas.common.enums./g;
s/org\\.chovy\\.canvas\\.domain\\.meta\\.StubOption/org.chovy.canvas.dto.StubOption/g;
s/org\\.chovy\\.canvas\\.auth\\.domain\\.JwtUtil/org.chovy.canvas.auth.util.JwtUtil/g;
s/org\\.chovy\\.canvas\\.infra\\./org.chovy.canvas.infrastructure./g;
s/package org\\.chovy\\.canvas\\.infra\\./package org.chovy.canvas.infrastructure./g;
'
perl -pi -e 's/^package org\\.chovy\\.canvas\\.domain\\.meta;/package org.chovy.canvas.dto;/' backend/canvas-engine/src/main/java/org/chovy/canvas/dto/StubOption.java
perl -pi -e 's/^package org\\.chovy\\.canvas\\.auth\\.domain;/package org.chovy.canvas.auth.util;/' backend/canvas-engine/src/main/java/org/chovy/canvas/auth/util/JwtUtil.java
```

Inspect `backend/canvas-engine/src/main/java/org/chovy/canvas/dto/StubOption.java` and ensure its first line is exactly:

```java
package org.chovy.canvas.dto;
```

- [ ] **Step 4: Compile and resolve package errors**

Run:

```bash
mvn -pl canvas-engine -am -DskipTests compile
```

Expected: compile passes or reports package errors from moved classes. Change only package declarations and imports.

- [ ] **Step 5: Commit support package move**

Run:

```bash
git add backend/canvas-engine/src/main/java backend/canvas-engine/src/test/java
git diff --cached --name-only
git commit -m "refactor: align support package boundaries"
```

Expected: staged files are constant/helper/infrastructure package moves and import updates.

---

### Task 5: Split Existing Business Services into Interfaces and Implementations

**Files:**
- Move implementation classes to `service/impl`
- Create interfaces in `service`
- Update controller and cross-service injection imports

- [ ] **Step 1: Create service directories**

Run:

```bash
mkdir -p backend/canvas-engine/src/main/java/org/chovy/canvas/service/impl
```

- [ ] **Step 2: Move implementation files**

Run:

```bash
move_file backend/canvas-engine/src/main/java/org/chovy/canvas/domain/canvas/CanvasService.java backend/canvas-engine/src/main/java/org/chovy/canvas/service/impl/CanvasServiceImpl.java
move_file backend/canvas-engine/src/main/java/org/chovy/canvas/domain/canvas/CanvasOpsService.java backend/canvas-engine/src/main/java/org/chovy/canvas/service/impl/CanvasOpsServiceImpl.java
move_file backend/canvas-engine/src/main/java/org/chovy/canvas/domain/canvas/CanvasTransactionService.java backend/canvas-engine/src/main/java/org/chovy/canvas/service/impl/CanvasTransactionServiceImpl.java
move_file backend/canvas-engine/src/main/java/org/chovy/canvas/domain/meta/MetaService.java backend/canvas-engine/src/main/java/org/chovy/canvas/service/impl/MetaServiceImpl.java
move_file backend/canvas-engine/src/main/java/org/chovy/canvas/domain/meta/EventDefinitionCacheService.java backend/canvas-engine/src/main/java/org/chovy/canvas/service/impl/EventDefinitionCacheServiceImpl.java
move_file backend/canvas-engine/src/main/java/org/chovy/canvas/auth/domain/SysUserService.java backend/canvas-engine/src/main/java/org/chovy/canvas/service/impl/SysUserServiceImpl.java
move_file backend/canvas-engine/src/main/java/org/chovy/canvas/domain/notification/NotificationService.java backend/canvas-engine/src/main/java/org/chovy/canvas/service/NotificationService.java
move_file backend/canvas-engine/src/main/java/org/chovy/canvas/domain/task/AsyncTaskService.java backend/canvas-engine/src/main/java/org/chovy/canvas/service/AsyncTaskService.java
```

- [ ] **Step 3: Update implementation class names and packages**

In each moved implementation file, set the package and class declaration exactly:

```java
package org.chovy.canvas.service.impl;
```

```java
public class CanvasServiceImpl implements CanvasService
public class CanvasOpsServiceImpl implements CanvasOpsService
public class CanvasTransactionServiceImpl implements CanvasTransactionService
public class MetaServiceImpl implements MetaService
public class EventDefinitionCacheServiceImpl implements EventDefinitionCacheService
public class SysUserServiceImpl implements SysUserService
```

Add the corresponding import in each implementation:

```java
import org.chovy.canvas.service.CanvasService;
```

Use the matching interface name for each file.

- [ ] **Step 4: Create service interfaces**

Create `backend/canvas-engine/src/main/java/org/chovy/canvas/service/CanvasService.java`:

```java
package org.chovy.canvas.service;

import org.chovy.canvas.common.PageResult;
import org.chovy.canvas.dal.dataobject.CanvasDO;
import org.chovy.canvas.dal.dataobject.CanvasVersionDO;
import org.chovy.canvas.dto.CanvasCreateReq;
import org.chovy.canvas.dto.CanvasDetailDTO;
import org.chovy.canvas.dto.CanvasUpdateReq;
import org.chovy.canvas.query.CanvasListQuery;

import java.util.List;

public interface CanvasService {
    CanvasDO create(CanvasCreateReq req);
    CanvasDetailDTO getById(Long id);
    void updateDraft(Long id, CanvasUpdateReq req);
    PageResult<CanvasDO> list(CanvasListQuery q);
    CanvasVersionDO publish(Long id, String operator);
    void offline(Long id, String operator);
    void archive(Long id, String operator);
    PageResult<CanvasVersionDO> getVersions(Long canvasId, int page, int size);
    @Deprecated
    List<CanvasVersionDO> getVersions(Long canvasId);
    CanvasVersionDO getVersion(Long canvasId, Long versionId);
    void revertToVersion(Long canvasId, Long versionId);
}
```

Create `CanvasOpsService.java`:

```java
package org.chovy.canvas.service;

import org.chovy.canvas.dal.dataobject.CanvasDO;

import java.util.Map;

public interface CanvasOpsService {
    void saveWithOptimisticLock(Long id, String name, String description, String graphJson, int editVersion, String operator);
    void kill(Long id, String mode);
    void startCanary(Long id, int percent, String operator);
    void promoteCanary(Long id);
    void rollbackCanary(Long id);
    void rollback(Long id);
    CanvasDO clone(Long id, String operator);
    Map<String, Object> diff(Long canvasId, Long v1Id, Long v2Id);
}
```

Create `CanvasTransactionService.java`:

```java
package org.chovy.canvas.service;

public interface CanvasTransactionService {
    Long offlineDb(Long id);
    void archiveDb(Long id);
}
```

Create `MetaService.java`:

```java
package org.chovy.canvas.service;

import org.chovy.canvas.dal.dataobject.ContextFieldDO;
import org.chovy.canvas.dal.dataobject.NodeTypeRegistryDO;
import org.chovy.canvas.dto.StubOption;

import java.util.List;

public interface MetaService {
    List<NodeTypeRegistryDO> getAllNodeTypes();
    NodeTypeRegistryDO getNodeTypeSchema(String typeKey);
    List<ContextFieldDO> getAllContextFields();
    List<StubOption> getMqTopics();
    List<StubOption> getCouponTypes();
    List<StubOption> getReachScenes();
    List<StubOption> getAbExperiments();
    List<StubOption> getAbExperimentGroups(String experimentKey);
    List<StubOption> getTaggerTags(String type);
    List<StubOption> getBizLines();
    List<StubOption> getBizLineApis(String bizLineKey);
    List<StubOption> getBehaviorStrategyTypes();
    List<StubOption> getMessageCodes(String type);
}
```

Create `EventDefinitionCacheService.java`:

```java
package org.chovy.canvas.service;

import org.chovy.canvas.dal.dataobject.EventDefinitionDO;

public interface EventDefinitionCacheService {
    EventDefinitionDO getPublishedByCode(String eventCode);
    void invalidatePublishedByCode(String eventCode);
}
```

Create `SysUserService.java`:

```java
package org.chovy.canvas.service;

import org.chovy.canvas.dal.dataobject.SysUserDO;

import java.util.List;

public interface SysUserService {
    SysUserDO findByUsername(String username);
    SysUserDO findByUsernameForAuth(String username);
    SysUserDO findById(Long id);
    List<SysUserDO> listAll();
    SysUserDO create(String username, String rawPassword, String displayName, String role);
    void update(Long id, String displayName, String rawPassword, String role);
    void disable(Long id);
    boolean checkPassword(SysUserDO user, String rawPassword);
}
```

- [ ] **Step 5: Move query classes**

Run:

```bash
mkdir -p backend/canvas-engine/src/main/java/org/chovy/canvas/query
move_file backend/canvas-engine/src/main/java/org/chovy/canvas/dto/CanvasListQuery.java backend/canvas-engine/src/main/java/org/chovy/canvas/query/CanvasListQuery.java
perl -pi -e 's/package org\\.chovy\\.canvas\\.dto;/package org.chovy.canvas.query;/' backend/canvas-engine/src/main/java/org/chovy/canvas/query/CanvasListQuery.java
find backend/canvas-engine/src/main/java backend/canvas-engine/src/test/java -name '*.java' -print0 | xargs -0 perl -pi -e 's/org\\.chovy\\.canvas\\.dto\\.CanvasListQuery/org.chovy.canvas.query.CanvasListQuery/g'
```

- [ ] **Step 6: Rewrite old service imports**

Run:

```bash
find backend/canvas-engine/src/main/java backend/canvas-engine/src/test/java -name '*.java' -print0 | xargs -0 perl -pi -e '
s/org\\.chovy\\.canvas\\.domain\\.canvas\\.CanvasService/org.chovy.canvas.service.CanvasService/g;
s/org\\.chovy\\.canvas\\.domain\\.canvas\\.CanvasOpsService/org.chovy.canvas.service.CanvasOpsService/g;
s/org\\.chovy\\.canvas\\.domain\\.canvas\\.CanvasTransactionService/org.chovy.canvas.service.CanvasTransactionService/g;
s/org\\.chovy\\.canvas\\.domain\\.meta\\.MetaService/org.chovy.canvas.service.MetaService/g;
s/org\\.chovy\\.canvas\\.domain\\.meta\\.EventDefinitionCacheService/org.chovy.canvas.service.EventDefinitionCacheService/g;
s/org\\.chovy\\.canvas\\.auth\\.domain\\.SysUserService/org.chovy.canvas.service.SysUserService/g;
s/org\\.chovy\\.canvas\\.domain\\.notification\\.NotificationService/org.chovy.canvas.service.NotificationService/g;
s/org\\.chovy\\.canvas\\.domain\\.task\\.AsyncTaskService/org.chovy.canvas.service.AsyncTaskService/g;
'
```

- [ ] **Step 7: Compile and fix signature mismatches**

Run:

```bash
mvn -pl canvas-engine -am -DskipTests compile
```

Expected: compile passes. Fix only package/type names and interface signatures that do not match implementation public methods.

- [ ] **Step 8: Commit service split**

Run:

```bash
git add backend/canvas-engine/src/main/java backend/canvas-engine/src/test/java
git diff --cached --name-only
git commit -m "refactor: split application services into interfaces"
```

Expected: staged files are service interfaces, service implementations, query move, imports, and type rename corrections.

---

### Task 6: Move Controllers to `web` and Add Thin Services for Direct Mapper Controllers

**Files:**
- Move: `controller/*.java` and `auth/controller/*.java` to `web`
- Create: thin service interfaces and implementations for direct Mapper controller logic
- Modify: controllers to inject services

- [ ] **Step 1: Move controllers into web package**

Run:

```bash
mkdir -p backend/canvas-engine/src/main/java/org/chovy/canvas/web
for src in backend/canvas-engine/src/main/java/org/chovy/canvas/controller/*.java backend/canvas-engine/src/main/java/org/chovy/canvas/auth/controller/*.java; do
  [ -e "$src" ] || continue
  dst="backend/canvas-engine/src/main/java/org/chovy/canvas/web/$(basename "$src")"
  move_file "$src" "$dst"
done
perl -pi -e 's/^package org\\.chovy\\.canvas\\.(auth\\.)?controller;/package org.chovy.canvas.web;/' backend/canvas-engine/src/main/java/org/chovy/canvas/web/*.java
```

- [ ] **Step 2: Create thin service interfaces**

Create service interfaces for controllers that currently inject Mapper directly:

```text
AbExperimentService
ApiDefinitionService
AudienceService
CanvasExecutionManagementService
CanvasStatsService
DlqService
EventDefinitionService
MqDefinitionService
OpsApplicationService
TagDefinitionService
```

Each interface method must match one controller action's business operation. Example for `TagDefinitionService`:

```java
package org.chovy.canvas.service;

import org.chovy.canvas.common.PageResult;
import org.chovy.canvas.dal.dataobject.TagDefinitionDO;

public interface TagDefinitionService {
    PageResult<TagDefinitionDO> list(String name, int page, int size);
    TagDefinitionDO create(TagDefinitionDO body);
    void update(Long id, TagDefinitionDO body);
    void delete(Long id);
}
```

- [ ] **Step 3: Create thin service implementations by moving existing controller logic**

For each direct Mapper controller, create `service.impl.*ServiceImpl` and move only the existing mapper logic from controller methods into service methods. Preserve the exact query wrappers, pagination defaults, JSON parsing, Redis calls, and return map keys.

Example for `TagDefinitionServiceImpl`:

```java
package org.chovy.canvas.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.chovy.canvas.common.PageResult;
import org.chovy.canvas.dal.dataobject.TagDefinitionDO;
import org.chovy.canvas.dal.mapper.TagDefinitionMapper;
import org.chovy.canvas.service.TagDefinitionService;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TagDefinitionServiceImpl implements TagDefinitionService {

    private final TagDefinitionMapper mapper;

    @Override
    public PageResult<TagDefinitionDO> list(String name, int page, int size) {
        LambdaQueryWrapper<TagDefinitionDO> wrapper = new LambdaQueryWrapper<TagDefinitionDO>()
                .like(name != null && !name.isBlank(), TagDefinitionDO::getName, name)
                .orderByDesc(TagDefinitionDO::getCreatedAt);
        var result = mapper.selectPage(new Page<>(page, size), wrapper);
        return PageResult.of(result.getTotal(), result.getRecords());
    }

    @Override
    public TagDefinitionDO create(TagDefinitionDO body) {
        mapper.insert(body);
        return body;
    }

    @Override
    public void update(Long id, TagDefinitionDO body) {
        body.setId(id);
        mapper.updateById(body);
    }

    @Override
    public void delete(Long id) {
        mapper.deleteById(id);
    }
}
```

- [ ] **Step 4: Update controllers to call thin services**

Example target for `TagDefinitionController`:

```java
@RestController
@RequestMapping("/canvas/tag-definitions")
@RequiredArgsConstructor
public class TagDefinitionController {

    private final TagDefinitionService tagDefinitionService;

    @GetMapping
    public Mono<R<PageResult<TagDefinitionDO>>> list(
            @RequestParam(required = false) String name,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return Mono.fromCallable(() -> tagDefinitionService.list(name, page, size))
                .subscribeOn(Schedulers.boundedElastic())
                .map(R::ok);
    }
}
```

For `AbExperimentController`, `ApiDefinitionController`, `AudienceController`, `CanvasExecutionManagementController`, `CanvasStatsController`, `DlqController`, `EventDefinitionController`, `MqDefinitionController`, `OpsController`, and `TagDefinitionController`, make the same mechanical controller change: remove Mapper fields, inject the matching service interface, move the former Mapper code into that service implementation, and leave all `@RequestMapping`, `@GetMapping`, `@PostMapping`, `@PutMapping`, `@DeleteMapping`, request parameters, and response field names unchanged.

- [ ] **Step 5: Verify no controller injects Mapper**

Run:

```bash
rg 'private final .*Mapper|Mapper ' backend/canvas-engine/src/main/java/org/chovy/canvas/web
```

Expected: no output, except comments if any. Remove direct Mapper fields from controllers.

- [ ] **Step 6: Compile and run controller tests**

Run:

```bash
mvn -pl canvas-engine -am -DskipTests compile
mvn -pl canvas-engine -Dtest='*ControllerTest' test
```

Expected: compile succeeds. Existing controller tests pass or fail only for pre-existing baseline failures recorded in Task 1.

- [ ] **Step 7: Commit web/service controller separation**

Run:

```bash
git add backend/canvas-engine/src/main/java backend/canvas-engine/src/test/java
git diff --cached --name-only
git commit -m "refactor: route controllers through services"
```

Expected: staged files are web package moves, new thin services, and tests/imports touched by the move.

---

### Task 7: Split Runtime `*Service` Classes Where Safe

**Files:**
- Modify or move runtime services in `engine/**`, `infrastructure/**`
- Create interfaces only for Spring services that are injected by other classes

- [ ] **Step 1: Inventory runtime services**

Run:

```bash
find backend/canvas-engine/src/main/java/org/chovy/canvas -name '*Service.java' -print | sort
rg '@Service|public class .*Service' backend/canvas-engine/src/main/java/org/chovy/canvas/engine backend/canvas-engine/src/main/java/org/chovy/canvas/infrastructure -n
```

Expected: list includes runtime services such as `CanvasExecutionService`, `CanvasSchedulerService`, `TriggerPreCheckService`, `AudienceBatchComputeService`, `AudienceSchedulerService`, `CanvasDisruptorService`, `ContextPersistenceService`, `TriggerRouteService`, and `MqRouteRefreshService`.

- [ ] **Step 2: For each injected runtime service, create an interface next to the implementation package boundary**

Example for `engine.trigger.CanvasExecutionService`:

```java
package org.chovy.canvas.engine.trigger;

import reactor.core.publisher.Mono;

import java.util.Map;

public interface CanvasExecutionService {
    Mono<Map<String, Object>> triggerDryRun(Long canvasId, String userId, Map<String, Object> payload, String graphJson);
    Mono<Map<String, Object>> trigger(Long canvasId, String userId, String triggerType, String triggerNodeType, String matchKey, Map<String, Object> payload, String msgId, boolean dryRun);
    void invalidateCanvas(Long canvasId);
}
```

Then rename the current class to `CanvasExecutionServiceImpl` in the same package:

```java
@Service
@RequiredArgsConstructor
public class CanvasExecutionServiceImpl implements CanvasExecutionService {
}
```

- [ ] **Step 3: Create interfaces for the other injected runtime services**

Use `rg -n '^    public ' <file>` on each implementation file and copy the exact public method signatures into the interface before renaming the class to `*ServiceImpl`. These are the current signatures that must be represented when the files exist:

```java
// CanvasSchedulerService -> CanvasSchedulerServiceImpl
void registerScheduledTriggers(Long canvasId, DagGraph graph);
void cancelScheduledTriggers(Long canvasId, DagGraph graph);
void cancelAll();

// TriggerPreCheckService -> TriggerPreCheckServiceImpl
void check(CanvasDO canvas, String userId);

// AudienceBatchComputeService -> AudienceBatchComputeServiceImpl
AudienceComputeResult compute(Long audienceId);
AudienceDefinitionDO create(AudienceDefinitionDO definition);
void update(AudienceDefinitionDO definition);
void delete(Long audienceId);
List<AudienceDefinitionDO> listReadyDefinitions();

// AudienceSchedulerService -> AudienceSchedulerServiceImpl
void refreshAll();
void refresh(AudienceDefinitionDO definition, Runnable job);
void cancel(Long audienceId);

// CanvasDisruptorService -> CanvasDisruptorServiceImpl
void publish(Long canvasId, String userId, String triggerType, String triggerNodeType, String matchKey, Map<String, Object> payload, String msgId, boolean dryRun);
void publishRequest(String requestId);
void shutdown();

// ContextPersistenceService -> ContextPersistenceServiceImpl
void save(ExecutionContext ctx);
ExecutionContext load(Long canvasId, String userId);
void delete(Long canvasId, String userId);
boolean exists(Long canvasId, String userId);
boolean acquireResumeLock(Long canvasId, String userId, String instanceId, long ttlSec);
void releaseResumeLock(Long canvasId, String userId);
boolean acquireDedup(Long canvasId, String userId, String msgId, Duration ttl);
void releaseDedup(String fullDedupKey);
String buildDedupKey(Long canvasId, String userId, String msgId);

// TriggerRouteService -> TriggerRouteServiceImpl
void registerMq(Long canvasId, String topicKey);
void registerBehavior(Long canvasId, String eventCode);
void registerTagger(Long canvasId, String tagCodeKey);
void removeMq(Long canvasId, String topicKey);
void removeBehavior(Long canvasId, String eventCode);
void removeTagger(Long canvasId, String tagCodeKey);
Set<String> getCanvasByMqTopic(String topicKey);
Set<String> getCanvasByBehavior(String eventCode);
Set<String> getCanvasByTagger(String tagCodeKey);
void clearMqRoutes();
void replaceMqRoutes(Map<String, Set<String>> routes);
boolean isRouteTableEmpty();

// MqRouteRefreshService -> MqRouteRefreshServiceImpl
void rebuildMqRoutes();

// CanvasExecutionRequestService -> CanvasExecutionRequestServiceImpl
String enqueue(Long canvasId, String userId, String triggerType, String triggerNodeType, String matchKey, Map<String, Object> payload, String sourceMsgId);
```

Do not include constructors, nested classes, package-private test helpers, or private methods in these interfaces.

Do not split classes named `*Cache` in this task unless they are injected under a `*Service` abstraction. Cache classes can remain concrete infrastructure components.

- [ ] **Step 4: Compile and fix interface signatures**

Run:

```bash
mvn -pl canvas-engine -am -DskipTests compile
```

Expected: compile succeeds after adding all public methods used by call sites to their interfaces.

- [ ] **Step 5: Commit runtime service interfaces**

Run:

```bash
git add backend/canvas-engine/src/main/java backend/canvas-engine/src/test/java
git diff --cached --name-only
git commit -m "refactor: add runtime service interfaces"
```

Expected: staged files are runtime service interface/implementation splits and imports.

---

### Task 8: Remove Old Domain Package and Verify Naming Rules

**Files:**
- Modify: any remaining Java imports and package references
- Delete: empty `domain/**`, old `controller/**`, old `infra/**` directories if empty

- [ ] **Step 1: Search for forbidden package references**

Run:

```bash
rg 'org\.chovy\.canvas\.domain|package org\.chovy\.canvas\.domain|org\.chovy\.canvas\.controller|org\.chovy\.canvas\.infra' backend/canvas-engine/src/main/java backend/canvas-engine/src/test/java
```

Expected: no output. For each match, replace the package according to the target structure in this plan.

- [ ] **Step 2: Search for non-DO persistence class references**

Run:

```bash
rg '\b(Canvas|CanvasVersion|CanvasTemplate|CanvasManualApproval|AudienceDefinition|AudienceStat|CanvasExecution|CanvasExecutionDlq|CanvasExecutionRequest|CanvasExecutionStats|CanvasExecutionTrace|CanvasUserQuota|AbExperiment|ApiDefinition|ContextField|EventDefinition|EventLog|MqMessageDefinition|NodeTypeRegistry|TagDefinition|SysUser)\b' backend/canvas-engine/src/main/java/org/chovy/canvas backend/canvas-engine/src/test/java/org/chovy/canvas
```

Expected: matches may remain in mapper names, controller names, DTO names, comments, enum names, table names, and API paths. Persistence object types should use the `*DO` suffix.

- [ ] **Step 3: Verify all `@TableName` classes end with DO**

Run:

```bash
rg -n '@TableName|public class ' backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject
```

Expected: every public class in `dal/dataobject` ends with `DO`.

- [ ] **Step 4: Verify all Mapper generics use DO**

Run:

```bash
rg 'BaseMapper<[^>]*DO>' backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper
rg 'BaseMapper<(?!.*DO)' backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper
```

Expected: first command lists Mapper generics. Second command returns no output; if the negative lookahead is unsupported by local `rg`, inspect `rg 'BaseMapper<' backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper`.

- [ ] **Step 5: Remove empty directories**

Run:

```bash
find backend/canvas-engine/src/main/java/org/chovy/canvas/domain -type d -empty -delete 2>/dev/null || true
find backend/canvas-engine/src/main/java/org/chovy/canvas/controller -type d -empty -delete 2>/dev/null || true
find backend/canvas-engine/src/main/java/org/chovy/canvas/infra -type d -empty -delete 2>/dev/null || true
```

- [ ] **Step 6: Compile**

Run:

```bash
mvn -pl canvas-engine -am -DskipTests compile
```

Expected: `BUILD SUCCESS`.

- [ ] **Step 7: Commit cleanup**

Run:

```bash
git add backend/canvas-engine/src/main/java backend/canvas-engine/src/test/java
git diff --cached --name-only
git commit -m "refactor: remove legacy backend package references"
```

Expected: staged files are final import/package cleanup only.

---

### Task 9: Run Full Verification

**Files:**
- No planned source edits unless verification reveals package/type errors from the refactor

- [ ] **Step 1: Run backend test suite**

Run:

```bash
mvn -pl canvas-engine -am test
```

Expected: `BUILD SUCCESS`, or only baseline failures recorded in Task 1. If failures are caused by ARCH-STD package/type moves, fix them before continuing.

- [ ] **Step 2: Run required spec checks**

Run:

```bash
rg "org.chovy.canvas.domain" backend/canvas-engine/src/main/java backend/canvas-engine/src/test/java
rg "namespace=\"org.chovy.canvas.domain" backend/canvas-engine/src/main/resources/mapper
rg "private final .*Mapper|Mapper " backend/canvas-engine/src/main/java/org/chovy/canvas/web
find backend/canvas-engine/src/main/resources/mapper -name '*Mapper.xml' | sort
```

Expected:

- first command has no output;
- second command has no output;
- third command has no controller Mapper field output;
- fourth command lists every Mapper XML.

- [ ] **Step 3: Verify Spring service injection direction**

Run:

```bash
rg 'private final .*ServiceImpl|new .*ServiceImpl' backend/canvas-engine/src/main/java backend/canvas-engine/src/test/java
```

Expected: no output from production code. Tests may instantiate implementations directly only when testing that implementation in isolation.

- [ ] **Step 4: Verify no business resources changed unintentionally**

Run:

```bash
git diff HEAD~5 -- backend/canvas-engine/src/main/resources/db/migration backend/canvas-engine/src/main/resources/db/demo
```

Expected: no SQL migration or demo data changes caused by ARCH-STD, except unrelated user changes that were present before this plan.

- [ ] **Step 5: Commit verification fixes if any**

If verification required fixes, run:

```bash
git add backend/canvas-engine/src/main/java backend/canvas-engine/src/test/java backend/canvas-engine/src/main/resources/mapper backend/canvas-engine/src/main/resources/application.yml
git diff --cached --name-only
git commit -m "fix: finish backend standardization verification"
```

Expected: create this commit only when Step 1-4 required source fixes after Task 8.

---

### Task 10: Final Documentation and Handoff

**Files:**
- Modify: `docs/superpowers/specs/INDEX.md` when the lookup wording differs from the command below
- Read: `docs/superpowers/specs/2026-05-23-ARCH-STD-backend-standardization-design.md`

- [ ] **Step 1: Confirm lookup commands still work**

Run:

```bash
rg "ARCH-STD" docs/superpowers/specs
sed -n '/ARCH-STD/p' docs/superpowers/specs/INDEX.md
```

Expected: both commands point to `2026-05-23-ARCH-STD-backend-standardization-design.md`.

- [ ] **Step 2: Summarize final package layout**

Run:

```bash
find backend/canvas-engine/src/main/java/org/chovy/canvas -maxdepth 2 -type d | sort
```

Expected: output includes `web`, `service`, `service/impl`, `dal`, `dal/dataobject`, `dal/mapper`, `engine`, `infrastructure`, `auth`, `common`, and `config`.

- [ ] **Step 3: Final status check**

Run:

```bash
git status --short
```

Expected: no ARCH-STD files remain unstaged. Compare any remaining unrelated dirty files with `/tmp/arch-std-pre-status.txt`.

- [ ] **Step 4: Final response contents**

Report:

```text
Implemented ARCH-STD backend standardization.
Verification: mvn -pl canvas-engine -am test
Lookup: rg "ARCH-STD" docs/superpowers/specs
```

If tests were not clean, include the exact failing test names and whether they were baseline failures from Task 1.
