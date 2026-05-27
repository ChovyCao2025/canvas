# CDP Audience Loop Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Enable audience definitions to use CDP tags, profiles, and identities as first-class data sources, generate the existing audience bitmap, and preview CDP audience matches before saving.

**Architecture:** Add a focused CDP audience source service beside the existing audience engine. Keep `AudienceBatchComputeService` as the orchestration point and route `CDP_TAG`, `CDP_PROFILE`, and `CDP_IDENTITY` to the new service, while preserving the existing `JDBC` and `TAGGER_API` paths. Extend the audience editor and API types so operators can configure CDP sources without changing the canvas TAGGER audience runtime.

**Tech Stack:** Java 21, Spring Boot WebFlux, MyBatis-Plus, Flyway, JUnit 5, Mockito, AssertJ, React 18, TypeScript, Ant Design, react-querybuilder, Vitest.

---

## Scope Check

This plan implements only stage 1 from `docs/superpowers/specs/2026-05-27-cdp-complete-enhancement-design.md`: CDP data sources for audience computation and preview.

It does not implement user 360, CDP events, property history, operation audit, batch task itemization, or canvas user journey materialization. Those belong in later plans.

## Worktree Requirement

All implementation work must happen in an isolated git worktree.

```bash
cd /Users/photonpay/project/canvas
git worktree add ../canvas-cdp-complete -b feat/cdp-complete
cd ../canvas-cdp-complete
```

Expected:

- `../canvas-cdp-complete` exists.
- `git branch --show-current` prints `feat/cdp-complete`.
- `test -f frontend/src/services/cdpApi.ts && echo ok` prints `ok`.

If `feat/cdp-complete` already exists, use:

```bash
cd /Users/photonpay/project/canvas
git worktree add ../canvas-cdp-complete feat/cdp-complete
cd ../canvas-cdp-complete
```

Do not copy unstaged files from `/Users/photonpay/project/canvas` into the worktree.

## File Structure

### Backend Create

- `backend/canvas-engine/src/main/resources/db/migration/V77__cdp_audience_source_options.sql`  
  Adds CDP audience data source options to `system_option`.

- `backend/canvas-engine/src/main/java/org/chovy/canvas/dto/audience/AudienceSourceFieldDTO.java`  
  Response item for fields available in a CDP audience source.

- `backend/canvas-engine/src/main/java/org/chovy/canvas/dto/audience/AudiencePreviewReq.java`  
  Request body for previewing an audience rule without saving.

- `backend/canvas-engine/src/main/java/org/chovy/canvas/dto/audience/AudiencePreviewResp.java`  
  Preview response containing estimated size and sample user IDs.

- `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/audience/CdpAudienceSourceService.java`  
  Reads CDP tags, profiles, and identities and returns matching user IDs.

- `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/audience/CdpAudienceSourceServiceTest.java`  
  Unit tests for CDP rule matching and field metadata.

- `backend/canvas-engine/src/test/java/org/chovy/canvas/controller/AudienceControllerCdpSourceTest.java`  
  Controller tests for source fields and preview endpoints.

### Backend Modify

- `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/audience/AudienceBatchComputeService.java`  
  Route `CDP_TAG`, `CDP_PROFILE`, and `CDP_IDENTITY` to `CdpAudienceSourceService`.

- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/AudienceController.java`  
  Add source fields and preview endpoints.

- `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/AudienceDefinitionDO.java`  
  Update comments only to include CDP source types.

- `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/audience/AudienceBatchComputeServiceTest.java`  
  Add coverage that CDP source computation saves a bitmap.

### Frontend Create

- `frontend/src/pages/audience-edit/cdpAudienceFields.ts`  
  Pure helpers for mapping backend CDP field metadata to query-builder fields.

- `frontend/src/pages/audience-edit/cdpAudienceFields.test.ts`  
  Vitest coverage for the helpers.

### Frontend Modify

- `frontend/src/services/audienceApi.ts`  
  Add CDP data source types, source field API, and preview API.

- `frontend/src/pages/audience-edit/index.tsx`  
  Load CDP source fields, render CDP-specific config, and add preview action.

---

### Task 1: Create Worktree and Baseline Check

**Files:**
- No code files changed.

- [ ] **Step 1: Create the worktree**

Run:

```bash
cd /Users/photonpay/project/canvas
git worktree add ../canvas-cdp-complete -b feat/cdp-complete
cd ../canvas-cdp-complete
```

Expected:

```text
Preparing worktree (new branch 'feat/cdp-complete')
HEAD is now at dd68854 docs: add cdp complete enhancement design
```

If the branch already exists, run:

```bash
cd /Users/photonpay/project/canvas
git worktree add ../canvas-cdp-complete feat/cdp-complete
cd ../canvas-cdp-complete
```

Expected:

```text
Preparing worktree (checking out 'feat/cdp-complete')
HEAD is now at dd68854 docs: add cdp complete enhancement design
```

- [ ] **Step 2: Verify the implementation baseline**

Run:

```bash
git branch --show-current
test -f frontend/src/services/cdpApi.ts && echo cdp-api-ok
test -f backend/canvas-engine/src/main/java/org/chovy/canvas/domain/cdp/CdpTagService.java && echo cdp-backend-ok
```

Expected:

```text
feat/cdp-complete
cdp-api-ok
cdp-backend-ok
```

- [ ] **Step 3: Verify Java 21 is available**

Run:

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
export PATH="$JAVA_HOME/bin:$PATH"
java -version
```

Expected: output contains `version "21`.

- [ ] **Step 4: Commit checkpoint**

No commit is required for this task because it only creates the worktree.

---

### Task 2: Add CDP Audience Source Options

**Files:**
- Create: `backend/canvas-engine/src/main/resources/db/migration/V77__cdp_audience_source_options.sql`

- [ ] **Step 1: Write the migration**

Create `backend/canvas-engine/src/main/resources/db/migration/V77__cdp_audience_source_options.sql`:

```sql
-- V77: CDP audience source options.

INSERT INTO system_option
  (category, option_key, label, description, sort_order, enabled, system_builtin)
VALUES
  ('audience_data_source_type', 'CDP_TAG', 'CDP 标签', '基于 CDP 当前用户标签圈选人群', 30, 1, 1),
  ('audience_data_source_type', 'CDP_PROFILE', 'CDP 用户属性', '基于 CDP 用户档案属性圈选人群', 40, 1, 1),
  ('audience_data_source_type', 'CDP_IDENTITY', 'CDP 身份', '基于 CDP 用户身份映射圈选人群', 50, 1, 1)
ON DUPLICATE KEY UPDATE
  label = VALUES(label),
  description = VALUES(description),
  sort_order = VALUES(sort_order),
  enabled = VALUES(enabled),
  system_builtin = VALUES(system_builtin);
```

- [ ] **Step 2: Check migration filename ordering**

Run:

```bash
ls backend/canvas-engine/src/main/resources/db/migration/V7*.sql | sort
```

Expected: output includes:

```text
backend/canvas-engine/src/main/resources/db/migration/V76__tagger_tag_value_schema.sql
backend/canvas-engine/src/main/resources/db/migration/V77__cdp_audience_source_options.sql
```

- [ ] **Step 3: Commit**

Run:

```bash
git add backend/canvas-engine/src/main/resources/db/migration/V77__cdp_audience_source_options.sql
git commit -m "feat: add cdp audience source options"
```

Expected: commit succeeds.

---

### Task 3: Add Audience DTOs

**Files:**
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dto/audience/AudienceSourceFieldDTO.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dto/audience/AudiencePreviewReq.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dto/audience/AudiencePreviewResp.java`

- [ ] **Step 1: Create `AudienceSourceFieldDTO`**

Create `backend/canvas-engine/src/main/java/org/chovy/canvas/dto/audience/AudienceSourceFieldDTO.java`:

```java
package org.chovy.canvas.dto.audience;

public record AudienceSourceFieldDTO(
        String name,
        String label,
        String valueType
) {}
```

- [ ] **Step 2: Create `AudiencePreviewReq`**

Create `backend/canvas-engine/src/main/java/org/chovy/canvas/dto/audience/AudiencePreviewReq.java`:

```java
package org.chovy.canvas.dto.audience;

public record AudiencePreviewReq(
        String dataSourceType,
        String ruleJson,
        Integer sampleLimit
) {}
```

- [ ] **Step 3: Create `AudiencePreviewResp`**

Create `backend/canvas-engine/src/main/java/org/chovy/canvas/dto/audience/AudiencePreviewResp.java`:

```java
package org.chovy.canvas.dto.audience;

import java.util.List;

public record AudiencePreviewResp(
        long estimatedSize,
        List<String> sampleUserIds
) {}
```

- [ ] **Step 4: Compile DTOs**

Run:

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
export PATH="$JAVA_HOME/bin:$PATH"
mvn -f backend/pom.xml -pl canvas-engine -DskipTests compile
```

Expected: `BUILD SUCCESS`.

- [ ] **Step 5: Commit**

Run:

```bash
git add backend/canvas-engine/src/main/java/org/chovy/canvas/dto/audience
git commit -m "feat: add audience cdp dto contracts"
```

Expected: commit succeeds.

---

### Task 4: Implement CDP Audience Source Service

**Files:**
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/audience/CdpAudienceSourceService.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/audience/CdpAudienceSourceServiceTest.java`

- [ ] **Step 1: Write failing tests**

Create `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/audience/CdpAudienceSourceServiceTest.java`:

```java
package org.chovy.canvas.engine.audience;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import org.chovy.canvas.dal.dataobject.CdpUserIdentityDO;
import org.chovy.canvas.dal.dataobject.CdpUserProfileDO;
import org.chovy.canvas.dal.dataobject.CdpUserTagDO;
import org.chovy.canvas.dal.dataobject.IdentityTypeDO;
import org.chovy.canvas.dal.dataobject.TagDefinitionDO;
import org.chovy.canvas.dal.mapper.CdpUserIdentityMapper;
import org.chovy.canvas.dal.mapper.CdpUserProfileMapper;
import org.chovy.canvas.dal.mapper.CdpUserTagMapper;
import org.chovy.canvas.dal.mapper.IdentityTypeMapper;
import org.chovy.canvas.dal.mapper.TagDefinitionMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CdpAudienceSourceServiceTest {

    private CdpUserTagMapper userTagMapper;
    private CdpUserProfileMapper profileMapper;
    private CdpUserIdentityMapper identityMapper;
    private TagDefinitionMapper tagDefinitionMapper;
    private IdentityTypeMapper identityTypeMapper;
    private CdpAudienceSourceService service;

    @BeforeEach
    void setUp() {
        userTagMapper = mock(CdpUserTagMapper.class);
        profileMapper = mock(CdpUserProfileMapper.class);
        identityMapper = mock(CdpUserIdentityMapper.class);
        tagDefinitionMapper = mock(TagDefinitionMapper.class);
        identityTypeMapper = mock(IdentityTypeMapper.class);
        service = new CdpAudienceSourceService(
                userTagMapper,
                profileMapper,
                identityMapper,
                tagDefinitionMapper,
                identityTypeMapper,
                new com.fasterxml.jackson.databind.ObjectMapper()
        );
    }

    @Test
    void listSourceFieldsReturnsEnabledTagDefinitionsForCdpTag() {
        TagDefinitionDO tag = new TagDefinitionDO();
        tag.setTagCode("high_value");
        tag.setName("高价值用户");
        tag.setValueType("STRING");
        when(tagDefinitionMapper.selectList(any(Wrapper.class))).thenReturn(List.of(tag));

        var fields = service.listSourceFields("CDP_TAG");

        assertThat(fields).hasSize(1);
        assertThat(fields.get(0).name()).isEqualTo("high_value");
        assertThat(fields.get(0).label()).isEqualTo("高价值用户");
        assertThat(fields.get(0).valueType()).isEqualTo("STRING");
    }

    @Test
    void listSourceFieldsDiscoversProfilePropertyKeys() {
        when(profileMapper.selectList(any(Wrapper.class))).thenReturn(List.of(
                profile("u1", "{\"city\":\"Shanghai\",\"level\":3}")
        ));

        var fields = service.listSourceFields("CDP_PROFILE");

        assertThat(fields).extracting("name")
                .contains("displayName", "status", "city", "level");
    }

    @Test
    void resolveUserIdsByCdpTagEvaluatesMultipleTagsPerUser() {
        CdpUserTagDO vip = tag("u1", "high_value", "VIP");
        CdpUserTagDO risk = tag("u1", "churn_risk", "HIGH");
        CdpUserTagDO normal = tag("u2", "high_value", "NORMAL");
        CdpUserTagDO otherRisk = tag("u3", "churn_risk", "HIGH");
        when(userTagMapper.selectList(any(Wrapper.class))).thenReturn(List.of(vip, risk, normal, otherRisk));

        String ruleJson = """
                {
                  "logic":"AND",
                  "conditions":[
                    {"field":"high_value","op":"=","value":"VIP"},
                    {"field":"churn_risk","op":"IN","value":["HIGH","MEDIUM"]}
                  ]
                }
                """;

        assertThat(service.resolveUserIds("CDP_TAG", ruleJson))
                .containsExactly("u1");
    }

    @Test
    void resolveUserIdsByCdpIdentityMatchesTypeAndValue() {
        CdpUserIdentityDO mobile = new CdpUserIdentityDO();
        mobile.setUserId("u1");
        mobile.setIdentityType("MOBILE");
        mobile.setIdentityValue("13812345678");
        CdpUserIdentityDO email = new CdpUserIdentityDO();
        email.setUserId("u2");
        email.setIdentityType("EMAIL");
        email.setIdentityValue("user@example.com");
        when(identityMapper.selectList(any(Wrapper.class))).thenReturn(List.of(mobile, email));

        String ruleJson = """
                {
                  "logic":"AND",
                  "conditions":[
                    {"field":"MOBILE","op":"=","value":"13812345678"}
                  ]
                }
                """;

        assertThat(service.resolveUserIds("CDP_IDENTITY", ruleJson))
                .containsExactly("u1");
    }

    @Test
    void resolveUserIdsByCdpProfileMatchesPropertiesJson() {
        CdpUserProfileDO shanghai = profile("u1", "{\"city\":\"Shanghai\",\"level\":3}");
        CdpUserProfileDO beijing = profile("u2", "{\"city\":\"Beijing\",\"level\":1}");
        when(profileMapper.selectList(any(Wrapper.class))).thenReturn(List.of(shanghai, beijing));

        String ruleJson = """
                {
                  "logic":"AND",
                  "conditions":[
                    {"field":"city","op":"=","value":"Shanghai"},
                    {"field":"level","op":">=","value":2}
                  ]
                }
                """;

        assertThat(service.resolveUserIds("CDP_PROFILE", ruleJson))
                .containsExactly("u1");
    }

    private CdpUserTagDO tag(String userId, String tagCode, String tagValue) {
        CdpUserTagDO tag = new CdpUserTagDO();
        tag.setUserId(userId);
        tag.setTagCode(tagCode);
        tag.setTagValue(tagValue);
        tag.setStatus("ACTIVE");
        return tag;
    }

    private CdpUserProfileDO profile(String userId, String propertiesJson) {
        CdpUserProfileDO profile = new CdpUserProfileDO();
        profile.setUserId(userId);
        profile.setPropertiesJson(propertiesJson);
        profile.setStatus("ACTIVE");
        return profile;
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run:

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
export PATH="$JAVA_HOME/bin:$PATH"
mvn -f backend/pom.xml -pl canvas-engine -Dtest=CdpAudienceSourceServiceTest test
```

Expected: compilation fails because `CdpAudienceSourceService` does not exist.

- [ ] **Step 3: Implement the service**

Create `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/audience/CdpAudienceSourceService.java`:

```java
package org.chovy.canvas.engine.audience;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.chovy.canvas.dal.dataobject.CdpUserIdentityDO;
import org.chovy.canvas.dal.dataobject.CdpUserProfileDO;
import org.chovy.canvas.dal.dataobject.CdpUserTagDO;
import org.chovy.canvas.dal.dataobject.IdentityTypeDO;
import org.chovy.canvas.dal.dataobject.TagDefinitionDO;
import org.chovy.canvas.dal.mapper.CdpUserIdentityMapper;
import org.chovy.canvas.dal.mapper.CdpUserProfileMapper;
import org.chovy.canvas.dal.mapper.CdpUserTagMapper;
import org.chovy.canvas.dal.mapper.IdentityTypeMapper;
import org.chovy.canvas.dal.mapper.TagDefinitionMapper;
import org.chovy.canvas.dto.audience.AudienceSourceFieldDTO;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CdpAudienceSourceService {

    private final CdpUserTagMapper userTagMapper;
    private final CdpUserProfileMapper profileMapper;
    private final CdpUserIdentityMapper identityMapper;
    private final TagDefinitionMapper tagDefinitionMapper;
    private final IdentityTypeMapper identityTypeMapper;
    private final ObjectMapper objectMapper;

    public List<AudienceSourceFieldDTO> listSourceFields(String dataSourceType) {
        return switch (normalizeSourceType(dataSourceType)) {
            case "CDP_TAG" -> tagDefinitionMapper.selectList(new LambdaQueryWrapper<TagDefinitionDO>()
                            .eq(TagDefinitionDO::getEnabled, 1)
                            .orderByAsc(TagDefinitionDO::getId))
                    .stream()
                    .map(tag -> new AudienceSourceFieldDTO(
                            tag.getTagCode(),
                            blankToFallback(tag.getName(), tag.getTagCode()),
                            blankToFallback(tag.getValueType(), "STRING")))
                    .toList();
            case "CDP_PROFILE" -> profileSourceFields();
            case "CDP_IDENTITY" -> identityTypeMapper.selectList(new LambdaQueryWrapper<IdentityTypeDO>()
                            .eq(IdentityTypeDO::getEnabled, 1)
                            .orderByAsc(IdentityTypeDO::getPriority)
                            .orderByAsc(IdentityTypeDO::getId))
                    .stream()
                    .map(type -> new AudienceSourceFieldDTO(
                            type.getCode().toUpperCase(Locale.ROOT),
                            blankToFallback(type.getName(), type.getCode()),
                            "STRING"))
                    .toList();
            default -> throw new IllegalArgumentException("Unsupported CDP audience source: " + dataSourceType);
        };
    }

    private List<AudienceSourceFieldDTO> profileSourceFields() {
        java.util.LinkedHashMap<String, AudienceSourceFieldDTO> fields = new java.util.LinkedHashMap<>();
        fields.put("displayName", new AudienceSourceFieldDTO("displayName", "展示名", "STRING"));
        fields.put("phone", new AudienceSourceFieldDTO("phone", "手机号", "STRING"));
        fields.put("email", new AudienceSourceFieldDTO("email", "邮箱", "STRING"));
        fields.put("status", new AudienceSourceFieldDTO("status", "状态", "STRING"));
        fields.put("firstSeenAt", new AudienceSourceFieldDTO("firstSeenAt", "首次出现时间", "STRING"));
        fields.put("lastSeenAt", new AudienceSourceFieldDTO("lastSeenAt", "最近活跃时间", "STRING"));
        profileMapper.selectList(new LambdaQueryWrapper<CdpUserProfileDO>()
                        .eq(CdpUserProfileDO::getStatus, "ACTIVE"))
                .stream()
                .map(CdpUserProfileDO::getPropertiesJson)
                .map(this::parseProperties)
                .flatMap(props -> props.keySet().stream())
                .forEach(key -> fields.putIfAbsent(key, new AudienceSourceFieldDTO(key, key, "STRING")));
        return List.copyOf(fields.values());
    }

    public List<String> resolveUserIds(String dataSourceType, String ruleJson) {
        RuleGroup rule = parseRule(ruleJson);
        Set<String> userIds = new LinkedHashSet<>();
        switch (normalizeSourceType(dataSourceType)) {
            case "CDP_TAG" -> {
                Map<String, Map<String, Object>> factsByUser = new java.util.LinkedHashMap<>();
                userTagMapper.selectList(new LambdaQueryWrapper<CdpUserTagDO>()
                                .eq(CdpUserTagDO::getStatus, "ACTIVE"))
                        .forEach(tag -> {
                            if (tag.getUserId() != null && tag.getTagCode() != null) {
                                factsByUser.computeIfAbsent(tag.getUserId(), ignored -> new java.util.LinkedHashMap<>())
                                        .put(tag.getTagCode(), tag.getTagValue());
                            }
                        });
                factsByUser.entrySet().stream()
                        .filter(entry -> matchesFacts(rule, entry.getValue()))
                        .map(Map.Entry::getKey)
                        .forEach(userIds::add);
            }
            case "CDP_PROFILE" -> profileMapper.selectList(new LambdaQueryWrapper<CdpUserProfileDO>()
                            .eq(CdpUserProfileDO::getStatus, "ACTIVE"))
                    .stream()
                    .filter(profile -> matchesProfile(rule, profile))
                    .map(CdpUserProfileDO::getUserId)
                    .filter(Objects::nonNull)
                    .forEach(userIds::add);
            case "CDP_IDENTITY" -> {
                Map<String, Map<String, Object>> factsByUser = new java.util.LinkedHashMap<>();
                identityMapper.selectList(new LambdaQueryWrapper<CdpUserIdentityDO>())
                        .forEach(identity -> {
                            if (identity.getUserId() != null && identity.getIdentityType() != null) {
                                factsByUser.computeIfAbsent(identity.getUserId(), ignored -> new java.util.LinkedHashMap<>())
                                        .put(identity.getIdentityType().toUpperCase(Locale.ROOT), identity.getIdentityValue());
                            }
                        });
                factsByUser.entrySet().stream()
                        .filter(entry -> matchesFacts(rule, entry.getValue()))
                        .map(Map.Entry::getKey)
                        .forEach(userIds::add);
            }
            default -> throw new IllegalArgumentException("Unsupported CDP audience source: " + dataSourceType);
        }
        return List.copyOf(userIds);
    }

    private boolean matchesProfile(RuleGroup rule, CdpUserProfileDO profile) {
        Map<String, Object> props = parseProperties(profile.getPropertiesJson());
        java.util.Map<String, Object> facts = new java.util.LinkedHashMap<>(props);
        facts.put("displayName", profile.getDisplayName());
        facts.put("phone", profile.getPhone());
        facts.put("email", profile.getEmail());
        facts.put("status", profile.getStatus());
        facts.put("firstSeenAt", profile.getFirstSeenAt() == null ? null : profile.getFirstSeenAt().toString());
        facts.put("lastSeenAt", profile.getLastSeenAt() == null ? null : profile.getLastSeenAt().toString());
        return matchesFacts(rule, facts);
    }

    private boolean matchesFacts(RuleGroup rule, Map<String, Object> facts) {
        return evaluateGroup(rule, condition -> compare(facts.get(condition.field()), condition.op(), condition.value()));
    }

    private boolean evaluateGroup(RuleGroup group, java.util.function.Predicate<RuleCondition> predicate) {
        boolean or = "OR".equalsIgnoreCase(group.logic());
        boolean result = or ? false : true;
        for (RuleCondition condition : group.conditions()) {
            boolean current = predicate.test(condition);
            result = or ? result || current : result && current;
        }
        for (RuleGroup child : group.groups()) {
            boolean current = evaluateGroup(child, predicate);
            result = or ? result || current : result && current;
        }
        return result;
    }

    private boolean compare(Object actual, String op, Object expected) {
        return switch (op) {
            case "=" -> Objects.equals(stringValue(actual), stringValue(expected));
            case "!=" -> !Objects.equals(stringValue(actual), stringValue(expected));
            case ">" -> number(actual).compareTo(number(expected)) > 0;
            case ">=" -> number(actual).compareTo(number(expected)) >= 0;
            case "<" -> number(actual).compareTo(number(expected)) < 0;
            case "<=" -> number(actual).compareTo(number(expected)) <= 0;
            case "IN", "in" -> expected instanceof List<?> list
                    && list.stream().map(this::stringValue).anyMatch(item -> item.equals(stringValue(actual)));
            default -> throw new IllegalArgumentException("Unsupported operator: " + op);
        };
    }

    private RuleGroup parseRule(String ruleJson) {
        if (ruleJson == null || ruleJson.isBlank()) {
            return new RuleGroup("AND", List.of(), List.of());
        }
        try {
            Map<String, Object> raw = objectMapper.readValue(ruleJson, new TypeReference<>() {});
            return toGroup(raw);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid audience ruleJson", e);
        }
    }

    @SuppressWarnings("unchecked")
    private RuleGroup toGroup(Map<String, Object> raw) {
        String logic = String.valueOf(raw.getOrDefault("logic", "AND")).toUpperCase(Locale.ROOT);
        List<RuleCondition> conditions = ((List<Object>) raw.getOrDefault("conditions", List.of()))
                .stream()
                .filter(Map.class::isInstance)
                .map(item -> {
                    Map<String, Object> condition = (Map<String, Object>) item;
                    return new RuleCondition(
                            String.valueOf(condition.get("field")),
                            String.valueOf(condition.get("op")),
                            condition.get("value"));
                })
                .toList();
        List<RuleGroup> groups = ((List<Object>) raw.getOrDefault("groups", List.of()))
                .stream()
                .filter(Map.class::isInstance)
                .map(item -> toGroup((Map<String, Object>) item))
                .toList();
        return new RuleGroup(logic, conditions, groups);
    }

    private Map<String, Object> parseProperties(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            return Map.of();
        }
    }

    private BigDecimal number(Object value) {
        try {
            return new BigDecimal(stringValue(value));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Audience condition value must be numeric: " + value);
        }
    }

    private String normalizeSourceType(String dataSourceType) {
        return dataSourceType == null ? "" : dataSourceType.trim().toUpperCase(Locale.ROOT);
    }

    private String blankToFallback(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private record RuleGroup(String logic, List<RuleCondition> conditions, List<RuleGroup> groups) {}

    private record RuleCondition(String field, String op, Object value) {}
}
```

- [ ] **Step 4: Run tests**

Run:

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
export PATH="$JAVA_HOME/bin:$PATH"
mvn -f backend/pom.xml -pl canvas-engine -Dtest=CdpAudienceSourceServiceTest test
```

Expected: `BUILD SUCCESS`.

- [ ] **Step 5: Commit**

Run:

```bash
git add backend/canvas-engine/src/main/java/org/chovy/canvas/engine/audience/CdpAudienceSourceService.java backend/canvas-engine/src/test/java/org/chovy/canvas/engine/audience/CdpAudienceSourceServiceTest.java
git commit -m "feat: resolve cdp audience source users"
```

Expected: commit succeeds.

---

### Task 5: Route CDP Sources Through Audience Compute

**Files:**
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/audience/AudienceBatchComputeService.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/audience/AudienceBatchComputeServiceTest.java`

- [ ] **Step 1: Add failing compute test**

Append this test method to `AudienceBatchComputeServiceTest`:

```java
@Test
void computeViaCdpTagSavesBitmap() {
    AudienceDefinitionMapper definitionMapper = mock(AudienceDefinitionMapper.class);
    AudienceStatMapper statMapper = mock(AudienceStatMapper.class);
    AudienceComputeRunMapper computeRunMapper = mock(AudienceComputeRunMapper.class);
    StringRedisTemplate redis = mock(StringRedisTemplate.class);
    ValueOperations<String, String> valueOps = mock(ValueOperations.class);
    AudienceBitmapStore bitmapStore = mock(AudienceBitmapStore.class);
    CdpAudienceSourceService cdpAudienceSourceService = mock(CdpAudienceSourceService.class);

    org.chovy.canvas.dal.dataobject.AudienceDefinitionDO definition = new org.chovy.canvas.dal.dataobject.AudienceDefinitionDO();
    definition.setId(9L);
    definition.setName("CDP VIP");
    definition.setEnabled(1);
    definition.setDataSourceType("CDP_TAG");
    definition.setRuleJson("{\"logic\":\"AND\",\"conditions\":[{\"field\":\"high_value\",\"op\":\"=\",\"value\":\"VIP\"}]}");

    when(definitionMapper.selectById(9L)).thenReturn(definition);
    when(redis.opsForValue()).thenReturn(valueOps);
    when(valueOps.setIfAbsent(eq("audience:compute:lock:9"), eq("1"), any(Duration.class))).thenReturn(true);
    when(cdpAudienceSourceService.resolveUserIds("CDP_TAG", definition.getRuleJson())).thenReturn(java.util.List.of("u1", "u2"));

    AudienceBatchComputeService service = new AudienceBatchComputeService(
            definitionMapper,
            statMapper,
            computeRunMapper,
            mock(RuleEvaluatorRouter.class),
            bitmapStore,
            redis,
            new ObjectMapper(),
            mock(SqlWhereGenerator.class),
            mock(AudienceEvaluationContextFetcher.class),
            mock(JdbcConfigResolver.class),
            cdpAudienceSourceService
    );

    AudienceComputeResult result = service.compute(9L);

    assertThat(result.success()).isTrue();
    assertThat(result.status()).isEqualTo("READY");
    var bitmapCaptor = org.mockito.ArgumentCaptor.forClass(org.roaringbitmap.RoaringBitmap.class);
    verify(bitmapStore).save(eq(9L), bitmapCaptor.capture());
    assertThat(bitmapCaptor.getValue().getCardinality()).isEqualTo(2);
}
```

- [ ] **Step 2: Run test to verify it fails**

Run:

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
export PATH="$JAVA_HOME/bin:$PATH"
mvn -f backend/pom.xml -pl canvas-engine -Dtest=AudienceBatchComputeServiceTest#computeViaCdpTagSavesBitmap test
```

Expected: compilation fails because the `AudienceBatchComputeService` constructor does not yet accept `CdpAudienceSourceService`.

- [ ] **Step 3: Modify `AudienceBatchComputeService` constructor dependency**

Add a field:

```java
private final CdpAudienceSourceService cdpAudienceSourceService;
```

The constructor is Lombok-generated by `@RequiredArgsConstructor`, so adding this final field updates the constructor.

- [ ] **Step 4: Add CDP routing cases**

In the switch inside `compute`, replace:

```java
RoaringBitmap bitmap = switch (definition.getDataSourceType()) {
    case "JDBC" -> computeViaJdbc(definition);
    case "TAGGER_API" -> computeViaTaggerApi(definition);
    default -> throw new IllegalStateException("Unsupported data source: " + definition.getDataSourceType());
};
```

with:

```java
RoaringBitmap bitmap = switch (definition.getDataSourceType()) {
    case "JDBC" -> computeViaJdbc(definition);
    case "TAGGER_API" -> computeViaTaggerApi(definition);
    case "CDP_TAG", "CDP_PROFILE", "CDP_IDENTITY" -> computeViaCdp(definition);
    default -> throw new IllegalStateException("Unsupported data source: " + definition.getDataSourceType());
};
```

- [ ] **Step 5: Add `computeViaCdp`**

Add this private method to `AudienceBatchComputeService`:

```java
private RoaringBitmap computeViaCdp(AudienceDefinitionDO definition) {
    List<String> userIds = cdpAudienceSourceService.resolveUserIds(
            definition.getDataSourceType(),
            definition.getRuleJson());
    RoaringBitmap bitmap = new RoaringBitmap();
    for (String userId : userIds) {
        if (userId != null && !userId.isBlank()) {
            bitmap.add(AudienceBitmapStore.toUid(userId));
        }
    }
    return bitmap;
}
```

Ensure `java.util.List` is already imported. If not, add:

```java
import java.util.List;
```

- [ ] **Step 6: Update existing tests using the constructor**

In `AudienceBatchComputeServiceTest`, every `new AudienceBatchComputeService(...)` call must add:

```java
mock(CdpAudienceSourceService.class)
```

as the last constructor argument.

- [ ] **Step 7: Run focused tests**

Run:

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
export PATH="$JAVA_HOME/bin:$PATH"
mvn -f backend/pom.xml -pl canvas-engine -Dtest=AudienceBatchComputeServiceTest test
```

Expected: `BUILD SUCCESS`.

- [ ] **Step 8: Commit**

Run:

```bash
git add backend/canvas-engine/src/main/java/org/chovy/canvas/engine/audience/AudienceBatchComputeService.java backend/canvas-engine/src/test/java/org/chovy/canvas/engine/audience/AudienceBatchComputeServiceTest.java
git commit -m "feat: compute audiences from cdp sources"
```

Expected: commit succeeds.

---

### Task 6: Add Source Fields and Preview APIs

**Files:**
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/AudienceController.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/AudienceDefinitionDO.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/controller/AudienceControllerCdpSourceTest.java`

- [ ] **Step 1: Write controller tests**

Create `backend/canvas-engine/src/test/java/org/chovy/canvas/controller/AudienceControllerCdpSourceTest.java`:

```java
package org.chovy.canvas.controller;

import org.chovy.canvas.domain.notification.NotificationService;
import org.chovy.canvas.domain.task.AsyncTaskService;
import org.chovy.canvas.dto.audience.AudienceSourceFieldDTO;
import org.chovy.canvas.engine.audience.AudienceBatchComputeService;
import org.chovy.canvas.engine.audience.AudienceComputeTaskRunner;
import org.chovy.canvas.engine.audience.AudienceSchedulerService;
import org.chovy.canvas.engine.audience.CdpAudienceSourceService;
import org.chovy.canvas.web.AudienceController;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AudienceControllerCdpSourceTest {

    @Test
    void sourceFieldsReturnsCdpFields() {
        CdpAudienceSourceService cdpService = mock(CdpAudienceSourceService.class);
        when(cdpService.listSourceFields("CDP_TAG")).thenReturn(List.of(
                new AudienceSourceFieldDTO("high_value", "高价值用户", "STRING")
        ));
        AudienceController controller = controller(cdpService);

        StepVerifier.create(controller.sourceFields("CDP_TAG"))
                .assertNext(resp -> {
                    assertThat(resp.getData()).hasSize(1);
                    assertThat(resp.getData().get(0).name()).isEqualTo("high_value");
                })
                .verifyComplete();
    }

    @Test
    void previewReturnsEstimatedSizeAndLimitedSamples() {
        CdpAudienceSourceService cdpService = mock(CdpAudienceSourceService.class);
        when(cdpService.resolveUserIds("CDP_TAG", "{\"logic\":\"AND\"}"))
                .thenReturn(List.of("u1", "u2", "u3"));
        AudienceController controller = controller(cdpService);

        StepVerifier.create(controller.preview(new org.chovy.canvas.dto.audience.AudiencePreviewReq(
                        "CDP_TAG",
                        "{\"logic\":\"AND\"}",
                        2
                )))
                .assertNext(resp -> {
                    assertThat(resp.getData().estimatedSize()).isEqualTo(3);
                    assertThat(resp.getData().sampleUserIds()).containsExactly("u1", "u2");
                })
                .verifyComplete();
    }

    private AudienceController controller(CdpAudienceSourceService cdpService) {
        return new AudienceController(
                mock(org.chovy.canvas.dal.mapper.AudienceDefinitionMapper.class),
                mock(org.chovy.canvas.dal.mapper.AudienceStatMapper.class),
                mock(AudienceBatchComputeService.class),
                mock(AudienceSchedulerService.class),
                mock(AsyncTaskService.class),
                mock(AudienceComputeTaskRunner.class),
                mock(NotificationService.class),
                cdpService
        );
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run:

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
export PATH="$JAVA_HOME/bin:$PATH"
mvn -f backend/pom.xml -pl canvas-engine -Dtest=AudienceControllerCdpSourceTest test
```

Expected: compilation fails because `AudienceController` does not yet accept `CdpAudienceSourceService` or expose `sourceFields` and `preview`.

- [ ] **Step 3: Add controller dependency**

In `AudienceController`, add:

```java
private final CdpAudienceSourceService cdpAudienceSourceService;
```

Add imports:

```java
import org.chovy.canvas.dto.audience.AudiencePreviewReq;
import org.chovy.canvas.dto.audience.AudiencePreviewResp;
import org.chovy.canvas.dto.audience.AudienceSourceFieldDTO;
import org.chovy.canvas.engine.audience.CdpAudienceSourceService;
```

- [ ] **Step 4: Add source fields endpoint**

Add this method to `AudienceController`:

```java
@GetMapping("/source-fields")
public Mono<R<List<AudienceSourceFieldDTO>>> sourceFields(@RequestParam String dataSourceType) {
    return Mono.fromCallable(() -> R.ok(cdpAudienceSourceService.listSourceFields(dataSourceType)))
            .subscribeOn(Schedulers.boundedElastic());
}
```

- [ ] **Step 5: Add preview endpoint**

Add this method to `AudienceController`:

```java
@PostMapping("/preview")
public Mono<R<AudiencePreviewResp>> preview(@RequestBody AudiencePreviewReq req) {
    return Mono.fromCallable(() -> {
        List<String> userIds = cdpAudienceSourceService.resolveUserIds(req.dataSourceType(), req.ruleJson());
        int limit = req.sampleLimit() == null ? 10 : Math.max(1, Math.min(req.sampleLimit(), 100));
        return R.ok(new AudiencePreviewResp(userIds.size(), userIds.stream().limit(limit).toList()));
    }).subscribeOn(Schedulers.boundedElastic());
}
```

- [ ] **Step 6: Update `AudienceDefinitionDO` comment**

Change:

```java
/** 数据源类型：TAGGER_API | JDBC。 */
private String dataSourceType;
```

to:

```java
/** 数据源类型：TAGGER_API | JDBC | CDP_TAG | CDP_PROFILE | CDP_IDENTITY。 */
private String dataSourceType;
```

- [ ] **Step 7: Run controller tests**

Run:

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
export PATH="$JAVA_HOME/bin:$PATH"
mvn -f backend/pom.xml -pl canvas-engine -Dtest=AudienceControllerCdpSourceTest test
```

Expected: `BUILD SUCCESS`.

- [ ] **Step 8: Commit**

Run:

```bash
git add backend/canvas-engine/src/main/java/org/chovy/canvas/web/AudienceController.java backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/AudienceDefinitionDO.java backend/canvas-engine/src/test/java/org/chovy/canvas/controller/AudienceControllerCdpSourceTest.java
git commit -m "feat: add cdp audience preview api"
```

Expected: commit succeeds.

---

### Task 7: Add Frontend API Contracts and Helpers

**Files:**
- Modify: `frontend/src/services/audienceApi.ts`
- Create: `frontend/src/pages/audience-edit/cdpAudienceFields.ts`
- Test: `frontend/src/pages/audience-edit/cdpAudienceFields.test.ts`

- [ ] **Step 1: Write helper tests**

Create `frontend/src/pages/audience-edit/cdpAudienceFields.test.ts`:

```ts
import { describe, expect, it } from 'vitest'
import { toQueryBuilderFields, isCdpAudienceSource } from './cdpAudienceFields'

describe('cdpAudienceFields', () => {
  it('detects cdp data source types', () => {
    expect(isCdpAudienceSource('CDP_TAG')).toBe(true)
    expect(isCdpAudienceSource('CDP_PROFILE')).toBe(true)
    expect(isCdpAudienceSource('CDP_IDENTITY')).toBe(true)
    expect(isCdpAudienceSource('JDBC')).toBe(false)
  })

  it('maps source fields to query builder fields', () => {
    expect(toQueryBuilderFields([
      { name: 'high_value', label: '高价值用户', valueType: 'STRING' },
      { name: 'score', label: '分数', valueType: 'NUMBER' },
    ])).toEqual([
      { name: 'high_value', label: '高价值用户', value: 'high_value' },
      { name: 'score', label: '分数', value: 'score' },
    ])
  })
})
```

- [ ] **Step 2: Run tests to verify they fail**

Run:

```bash
npm --prefix frontend test -- cdpAudienceFields.test.ts
```

Expected: fails because `cdpAudienceFields.ts` does not exist.

- [ ] **Step 3: Create helper implementation**

Create `frontend/src/pages/audience-edit/cdpAudienceFields.ts`:

```ts
import type { AudienceDataSourceType, AudienceSourceField } from '../../services/audienceApi'

export function isCdpAudienceSource(type?: string): type is Extract<AudienceDataSourceType, 'CDP_TAG' | 'CDP_PROFILE' | 'CDP_IDENTITY'> {
  return type === 'CDP_TAG' || type === 'CDP_PROFILE' || type === 'CDP_IDENTITY'
}

export function toQueryBuilderFields(fields: AudienceSourceField[]) {
  return fields.map(field => ({
    name: field.name,
    label: field.label || field.name,
    value: field.name,
  }))
}
```

- [ ] **Step 4: Extend `audienceApi.ts` types and methods**

In `frontend/src/services/audienceApi.ts`, add before `AudienceDefinition`:

```ts
export type AudienceDataSourceType = 'TAGGER_API' | 'JDBC' | 'CDP_TAG' | 'CDP_PROFILE' | 'CDP_IDENTITY'

export interface AudienceSourceField {
  name: string
  label: string
  valueType: string
}

export interface AudiencePreviewPayload {
  dataSourceType: AudienceDataSourceType
  ruleJson: string
  sampleLimit?: number
}

export interface AudiencePreviewResp {
  estimatedSize: number
  sampleUserIds: string[]
}
```

Change the `dataSourceType` field in `AudienceDefinition` from:

```ts
dataSourceType: 'TAGGER_API' | 'JDBC'
```

to:

```ts
dataSourceType: AudienceDataSourceType
```

Add methods to `audienceApi`:

```ts
sourceFields: (dataSourceType: AudienceDataSourceType) =>
  http.get<R<AudienceSourceField[]>, R<AudienceSourceField[]>>('/canvas/audiences/source-fields', { params: { dataSourceType } }),
preview: (body: AudiencePreviewPayload) =>
  http.post<R<AudiencePreviewResp>, R<AudiencePreviewResp>>('/canvas/audiences/preview', body),
```

- [ ] **Step 5: Run helper tests**

Run:

```bash
npm --prefix frontend test -- cdpAudienceFields.test.ts
```

Expected: `2 tests` pass.

- [ ] **Step 6: Commit**

Run:

```bash
git add frontend/src/services/audienceApi.ts frontend/src/pages/audience-edit/cdpAudienceFields.ts frontend/src/pages/audience-edit/cdpAudienceFields.test.ts
git commit -m "feat: add cdp audience frontend contracts"
```

Expected: commit succeeds.

---

### Task 8: Upgrade Audience Editor for CDP Sources

**Files:**
- Modify: `frontend/src/pages/audience-edit/index.tsx`
- Test: `frontend/src/pages/audience-edit/cdpAudienceFields.test.ts`

- [ ] **Step 1: Add imports**

In `frontend/src/pages/audience-edit/index.tsx`, change:

```ts
import { Button, Card, Form, Input, InputNumber, Select, Space, Switch, Typography, message } from 'antd'
```

to:

```ts
import { Alert, Button, Card, Form, Input, InputNumber, Select, Space, Switch, Typography, message } from 'antd'
```

Change:

```ts
import { audienceApi, type AudienceDefinition } from '../../services/audienceApi'
```

to:

```ts
import { audienceApi, type AudienceDefinition, type AudienceDataSourceType, type AudiencePreviewResp, type AudienceSourceField } from '../../services/audienceApi'
```

Add:

```ts
import { isCdpAudienceSource, toQueryBuilderFields } from './cdpAudienceFields'
```

- [ ] **Step 2: Add state**

After `tagFields` state, add:

```ts
const [cdpFields, setCdpFields] = useState<AudienceSourceField[]>([])
const [previewResult, setPreviewResult] = useState<AudiencePreviewResp | null>(null)
const [previewLoading, setPreviewLoading] = useState(false)
```

- [ ] **Step 3: Update fields memo**

Replace:

```ts
const fields = useMemo(() => {
  if (dataSourceType === 'JDBC' && selectedJdbcTable) {
    return selectedJdbcTable.columns.map(column => ({ name: column, label: column, value: column }))
  }
  return tagFields
}, [dataSourceType, selectedJdbcTable, tagFields])
```

with:

```ts
const fields = useMemo(() => {
  if (dataSourceType === 'JDBC' && selectedJdbcTable) {
    return selectedJdbcTable.columns.map(column => ({ name: column, label: column, value: column }))
  }
  if (isCdpAudienceSource(dataSourceType)) {
    return toQueryBuilderFields(cdpFields)
  }
  return tagFields
}, [cdpFields, dataSourceType, selectedJdbcTable, tagFields])
```

- [ ] **Step 4: Load CDP fields when source changes**

Add this effect after the JDBC table-loading effect:

```ts
useEffect(() => {
  if (!isCdpAudienceSource(dataSourceType)) {
    setCdpFields([])
    setPreviewResult(null)
    return
  }
  audienceApi.sourceFields(dataSourceType as AudienceDataSourceType)
    .then(res => setCdpFields(res.data ?? []))
    .catch(error => {
      setCdpFields([])
      message.error(error?.message || '读取 CDP 字段失败')
    })
}, [dataSourceType])
```

- [ ] **Step 5: Update save config selection**

Replace:

```ts
const config = values.dataSourceType === 'JDBC'
  ? values.jdbcConfig
  : values.taggerConfig
```

with:

```ts
const config = values.dataSourceType === 'JDBC'
  ? values.jdbcConfig
  : values.dataSourceType === 'TAGGER_API'
    ? values.taggerConfig
    : { sourceType: values.dataSourceType }
```

- [ ] **Step 6: Add preview handler**

Add before `return (`:

```ts
const handlePreview = async () => {
  if (!isCdpAudienceSource(dataSourceType)) return
  setPreviewLoading(true)
  try {
    const ruleJson = JSON.stringify(serializeGroup(query))
    const res = await audienceApi.preview({
      dataSourceType: dataSourceType as AudienceDataSourceType,
      ruleJson,
      sampleLimit: 10,
    })
    setPreviewResult(res.data)
  } finally {
    setPreviewLoading(false)
  }
}
```

- [ ] **Step 7: Render CDP source hint**

Inside the data source card, after the JDBC block, add:

```tsx
{isCdpAudienceSource(dataSourceType) && (
  <Alert
    type="info"
    showIcon
    message="CDP 数据源将使用当前规则直接圈选 CDP 用户，保存后仍生成现有 audience bitmap。"
  />
)}
```

- [ ] **Step 8: Add preview button and result**

Inside the `圈选规则` card, after the `QueryBuilderAntD` block and before the JSON preview block, add:

```tsx
{isCdpAudienceSource(dataSourceType) && (
  <div style={{ marginTop: 12 }}>
    <Space>
      <Button loading={previewLoading} onClick={handlePreview}>
        预览命中
      </Button>
      {previewResult && (
        <Typography.Text type="secondary">
          命中 {previewResult.estimatedSize.toLocaleString()} 人，样例：{previewResult.sampleUserIds.join(', ') || '-'}
        </Typography.Text>
      )}
    </Space>
  </div>
)}
```

- [ ] **Step 9: Run frontend tests**

Run:

```bash
npm --prefix frontend test -- cdpAudienceFields.test.ts
```

Expected: tests pass.

- [ ] **Step 10: Run frontend build**

Run:

```bash
npm --prefix frontend run build
```

Expected: `vite build` completes successfully.

- [ ] **Step 11: Commit**

Run:

```bash
git add frontend/src/pages/audience-edit/index.tsx
git commit -m "feat: support cdp sources in audience editor"
```

Expected: commit succeeds.

---

### Task 9: Full Verification

**Files:**
- No new files.

- [ ] **Step 1: Run backend focused tests**

Run:

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
export PATH="$JAVA_HOME/bin:$PATH"
mvn -f backend/pom.xml -pl canvas-engine -Dtest='CdpAudienceSourceServiceTest,AudienceBatchComputeServiceTest,AudienceControllerCdpSourceTest' test
```

Expected: `BUILD SUCCESS`.

- [ ] **Step 2: Run frontend focused tests**

Run:

```bash
npm --prefix frontend test -- cdpAudienceFields.test.ts
```

Expected: all tests pass.

- [ ] **Step 3: Run frontend build**

Run:

```bash
npm --prefix frontend run build
```

Expected: build succeeds.

- [ ] **Step 4: Check git status**

Run:

```bash
git status --short
```

Expected: no unstaged or uncommitted files from this plan.

- [ ] **Step 5: Record known repository-wide test state**

Run:

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
export PATH="$JAVA_HOME/bin:$PATH"
mvn -f backend/pom.xml -pl canvas-engine -Dtest='*Cdp*,*CanvasUser*' test
```

Expected in the current repository baseline: this may fail during `testCompile` because several non-CDP tests instantiate constructors with stale argument lists. If it fails, copy the first constructor mismatch into the final implementation notes and do not treat it as a CDP audience-loop regression.

- [ ] **Step 6: Commit verification note only if files changed**

If Step 5 required editing no files, do not commit.

If a verification note file is added, use:

```bash
git add docs/superpowers/plans/2026-05-27-cdp-audience-loop.md
git commit -m "docs: note cdp audience verification state"
```

Expected: commit succeeds only if the plan file changed.
