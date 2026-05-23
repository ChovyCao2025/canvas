# System Options Configuration Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace all audited hardcoded dropdown options with a maintainable system-option configuration model and add AB experiment group management.

**Architecture:** Add a backend `system_option` dictionary for global controlled options and an `ab_experiment_group` table for experiment-owned groups. Frontend pages consume options through shared APIs/hooks, while canvas schema rendering supports `optionCategory` in addition to existing `dataSource` and `options` fallbacks.

**Tech Stack:** Spring Boot WebFlux, MyBatis-Plus, Flyway SQL migrations, React 18, Vite, Ant Design, Vitest, JUnit 5.

---

## Execution Prerequisites

The repository is currently in a merge state and has existing staged and unstaged changes unrelated to this plan. Before implementing, either finish the merge in the main workspace or create an isolated worktree through `superpowers:using-git-worktrees`. Do not include unrelated existing changes in commits for this plan.

Use these verification commands throughout:

```bash
cd /Users/photonpay/project/canvas
mvn -pl backend/canvas-engine test
cd frontend && npm test
cd frontend && npm run build
```

---

## File Structure

Backend files to create:

- `backend/canvas-engine/src/main/resources/db/migration/V53__system_options_and_ab_groups.sql`: tables, seed options, schema migration, AB default groups.
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/meta/SystemOption.java`: MyBatis entity.
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/meta/SystemOptionMapper.java`: mapper.
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/meta/SystemOptionService.java`: lookup, admin update, label fallback, validation.
- `backend/canvas-engine/src/main/java/org/chovy/canvas/controller/SystemOptionController.java`: admin list and update endpoints.
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/meta/AbExperimentGroup.java`: MyBatis entity.
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/meta/AbExperimentGroupMapper.java`: mapper.
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/meta/AbExperimentGroupService.java`: group CRUD and default A/B creation.
- `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/meta/SystemOptionServiceTest.java`: dictionary behavior tests.
- `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/meta/AbExperimentGroupServiceTest.java`: AB group behavior tests.

Backend files to modify:

- `backend/canvas-engine/src/main/java/org/chovy/canvas/controller/MetaController.java`: add `/meta/options`, `/meta/options/batch`, replace stub endpoints, use AB groups from table.
- `backend/canvas-engine/src/main/java/org/chovy/canvas/controller/AbExperimentController.java`: create default groups and expose group endpoints.
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/meta/MetaService.java`: remove stub `List.of(...)` option providers or delegate to `SystemOptionService`.

Frontend files to create:

- `frontend/src/services/systemOptions.ts`: typed APIs and option categories.
- `frontend/src/hooks/useSystemOptions.ts`: shared hook with batch loading and current-value merge.
- `frontend/src/hooks/useSystemOptions.test.ts`: normalization and fallback tests.
- `frontend/src/pages/system-options/index.tsx`: system option management page.
- `frontend/src/pages/system-options/systemOptionsPage.test.ts`: presentation/helper tests.

Frontend files to modify:

- `frontend/src/services/api.ts`: export system option and AB group APIs.
- `frontend/src/types/index.ts`: add `SystemOption` and `OptionCategory` types, add `optionCategory` to `SchemaField`.
- `frontend/src/App.tsx`: add `/system-options` admin route.
- `frontend/src/components/layout/AppLayout.tsx`: add menu item.
- `frontend/src/components/config-panel/index.tsx`: load `optionCategory`, replace hardcoded condition/operator/value-type/unit options, update AB group list.
- `frontend/src/components/config-panel/displayValues.ts`: support disabled/current option labels.
- `frontend/src/components/config-panel/CronBuilder.tsx`: load frequency and weekday labels from system options, keep numeric ranges generated.
- `frontend/src/pages/api-config/index.tsx`: replace `PARAM_TYPES` and method constants with system options.
- `frontend/src/pages/mq-config/index.tsx`: replace parameter type constants.
- `frontend/src/pages/event-config/index.tsx`: replace event attribute type constants.
- `frontend/src/pages/tag-config/index.tsx`: replace tag type constants.
- `frontend/src/pages/audience-edit/index.tsx`: replace data source, operator, combinator, strategy, engine constants.
- `frontend/src/pages/admin/index.tsx`: replace role constants.
- `frontend/src/pages/ab-experiment/index.tsx`: add group management drawer.
- Existing tests under `frontend/src/components/config-panel/*.test.ts` and page tests: update expected option labels where needed.

---

## Seed Categories

Use these rows in the migration and frontend category constants. Labels preserve current UI text.

```text
condition_operator: EQ=等于, NEQ=不等于, CONTAINS=包含, GT=大于, LT=小于, GTE=大于等于, LTE=小于等于
audience_condition_operator: "="=等于, "!="=不等于, ">"=大于, ">="=大于等于, "<"=小于, "<="=小于等于, "in"=包含于
logic_relation: AND=且(AND), OR=或(OR)
query_combinator: and=且（AND）, or=或（OR）
param_type: STRING=字符型, NUMBER=数值型, TEXT=文本型, DATE=日期型, STRING_PARAM=字符型（参数调用）, BOOLEAN=布尔型, LIST=列表
event_attr_type: STRING=字符型, NUMBER=数值型, DATE=日期型
http_method: GET=GET, POST=POST
tag_type: offline=离线标签, realtime=实时标签
audience_data_source_type: TAGGER_API=Tagger API, JDBC=JDBC
audience_evaluation_strategy: OFFLINE_BATCH=离线批量, ONLINE=实时计算, HYBRID=混合
audience_engine_type: AVIATOR=AviatorScript, QL=QLExpress
user_role: ADMIN=ADMIN（管理员）, OPERATOR=OPERATOR（运营）
context_value_type: CUSTOM=自定义, CONTEXT=上下文
delay_unit: SECOND=秒, MINUTE=分钟, HOUR=小时
cron_frequency: daily=每天, weekly=每周, monthly=每月, hourly=每小时
weekday: 1=周一, 2=周二, 3=周三, 4=周四, 5=周五, 6=周六, 0=周日
schedule_type: ONCE=指定时间(ONCE), CRON=周期(CRON)
tagger_mode: realtime=实时触发（监听 MQ 事件）, offline=离线打标（流程内执行）, audience=人群圈选
threshold_mode: min_success=成功数 ≥ N（K-of-N 投票）, min_done=完成数 ≥ N（SUCCESS+FAILED 均计）, any_fail=任意上游失败立刻触发
aggregate_evaluate_mode: count=成功数 ≥ N, rate=成功率 ≥ N%, script=自定义脚本
approval_timeout_action: REJECT=拒绝, APPROVE=通过, KEEP_WAITING=持续等待
canvas_invoke_mode: SYNC=同步等待, ASYNC=异步触发
direct_return_build_type: CUSTOM=自定义
coupon_type: flight_coupon=机票代金券, hotel_coupon=酒店代金券, train_coupon=火车票代金券
reach_scene: quick_booking_push=急速预订Push, hotel_recommend_push=酒店推荐Push, coupon_reminder_sms=领券提醒短信
biz_line: FLIGHT=机票, HOTEL=酒店, TRAIN_TICKET=火车票
biz_line_api: check_good_seat=查询好坐席, query_user_info=查询用户信息, query_order_detail=查询订单详情
behavior_strategy_type: BROWSE_DURATION=浏览时长, BROWSE_COUNT=浏览次数, CLICK_COUNT=点击次数
message_code_in_app: international_hotel_coupon_popup=国际酒店领券弹窗, flight_coupon_banner=机票优惠Banner
message_code_mq: ivr_project=IVR项目消息, reward_notify=奖励通知消息
mq_topic_legacy: flight_order_status_change=机票订单状态变化, hotel_order_status_change=酒店订单状态变化, train_order_status_change=火车票订单状态变化
canvas_trigger_type: REALTIME=实时触发, SCHEDULED=定时触发
start_trigger_type: DIRECT=手动直调, EVENT=事件触发, SCHEDULED=定时触发, MQ=MQ消息
behavior_trigger_type: inapp=端内行为事件（监听 MQ）, direct=业务直调（HTTP 推送）
```

---

### Task 1: Database Migration

**Files:**
- Create: `backend/canvas-engine/src/main/resources/db/migration/V53__system_options_and_ab_groups.sql`

- [ ] **Step 1: Write migration with tables**

Create the migration with these table definitions:

```sql
CREATE TABLE IF NOT EXISTS system_option (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  category VARCHAR(80) NOT NULL,
  option_key VARCHAR(120) NOT NULL,
  label VARCHAR(200) NOT NULL,
  description VARCHAR(500) NULL,
  sort_order INT NOT NULL DEFAULT 0,
  enabled TINYINT NOT NULL DEFAULT 1,
  system_builtin TINYINT NOT NULL DEFAULT 1,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_system_option_category_key (category, option_key),
  KEY idx_system_option_category_enabled_sort (category, enabled, sort_order, id)
);

CREATE TABLE IF NOT EXISTS ab_experiment_group (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  experiment_id BIGINT NOT NULL,
  group_key VARCHAR(64) NOT NULL,
  label VARCHAR(200) NOT NULL,
  sort_order INT NOT NULL DEFAULT 0,
  enabled TINYINT NOT NULL DEFAULT 1,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_ab_experiment_group_key (experiment_id, group_key),
  KEY idx_ab_experiment_group_enabled_sort (experiment_id, enabled, sort_order, id)
);
```

- [ ] **Step 2: Seed `system_option`**

Use one `INSERT ... VALUES ... ON DUPLICATE KEY UPDATE` statement containing every row from the Seed Categories section. The SQL shape must be:

```sql
INSERT INTO system_option
  (category, option_key, label, description, sort_order, enabled, system_builtin)
VALUES
  ('condition_operator', 'EQ', '等于', '条件规则操作符', 10, 1, 1),
  ('condition_operator', 'NEQ', '不等于', '条件规则操作符', 20, 1, 1),
  ('condition_operator', 'CONTAINS', '包含', '条件规则操作符', 30, 1, 1),
  ('condition_operator', 'GT', '大于', '条件规则操作符', 40, 1, 1),
  ('condition_operator', 'LT', '小于', '条件规则操作符', 50, 1, 1),
  ('condition_operator', 'GTE', '大于等于', '条件规则操作符', 60, 1, 1),
  ('condition_operator', 'LTE', '小于等于', '条件规则操作符', 70, 1, 1),
  ('query_combinator', 'and', '且（AND）', '人群规则组合关系', 10, 1, 1),
  ('query_combinator', 'or', '或（OR）', '人群规则组合关系', 20, 1, 1),
  ('http_method', 'GET', 'GET', 'API 请求方法', 10, 1, 1),
  ('http_method', 'POST', 'POST', 'API 请求方法', 20, 1, 1);
```

The Seed Categories section is the complete row source. Each `category:key=label` pair becomes exactly one `VALUES` tuple, with sort orders increasing by 10 within each category. End the statement with `ON DUPLICATE KEY UPDATE label = VALUES(label), description = VALUES(description), sort_order = VALUES(sort_order), system_builtin = VALUES(system_builtin)` and do not overwrite `enabled`.

- [ ] **Step 3: Seed default AB groups**

Append this SQL so existing experiments get A/B groups:

```sql
INSERT INTO ab_experiment_group (experiment_id, group_key, label, sort_order, enabled)
SELECT id, 'A', 'A组', 10, 1 FROM ab_experiment
ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order);

INSERT INTO ab_experiment_group (experiment_id, group_key, label, sort_order, enabled)
SELECT id, 'B', 'B组', 20, 1 FROM ab_experiment
ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order);
```

- [ ] **Step 4: Migrate schema inline options**

Append `UPDATE node_type_registry` statements for known schema options. Use compact JSON and keep existing non-option fields unchanged. Create one statement for each mapping in this table:

| `type_key` | Old inline option fragment | New fragment |
| --- | --- | --- |
| `MANUAL_APPROVAL` | `"options":[{"label":"拒绝","value":"REJECT"},{"label":"通过","value":"APPROVE"},{"label":"持续等待","value":"KEEP_WAITING"}]` | `"optionCategory":"approval_timeout_action"` |
| `SCHEDULED_TRIGGER` | `"options":[{"label":"指定时间(ONCE)","value":"ONCE"},{"label":"周期(CRON)","value":"CRON"}]` | `"optionCategory":"schedule_type"` |
| `TAGGER` | `"options":[{"label":"实时触发（监听 MQ 事件）","value":"realtime"},{"label":"离线打标（流程内执行）","value":"offline"},{"label":"人群圈选","value":"audience"}]` | `"optionCategory":"tagger_mode"` |
| `THRESHOLD` | `"options":[{"label":"成功数 ≥ N（K-of-N 投票）","value":"min_success"},{"label":"完成数 ≥ N（SUCCESS+FAILED 均计）","value":"min_done"},{"label":"任意上游失败立刻触发","value":"any_fail"}]` | `"optionCategory":"threshold_mode"` |
| `AGGREGATE` | `"options":[{"label":"成功数 ≥ N","value":"count"},{"label":"成功率 ≥ N%","value":"rate"},{"label":"自定义脚本","value":"script"}]` | `"optionCategory":"aggregate_evaluate_mode"` |
| `CANVAS_TRIGGER` | `"options":[{"label":"同步等待","value":"SYNC"},{"label":"异步触发","value":"ASYNC"}]` | `"optionCategory":"canvas_invoke_mode"` |
| `LOGIC_RELATION` | `"options":[{"label":"且(AND)","value":"AND"},{"label":"或(OR)","value":"OR"}]` | `"optionCategory":"logic_relation"` |
| `DIRECT_RETURN` | `"options":[{"label":"自定义","value":"CUSTOM"}]` | `"optionCategory":"direct_return_build_type"` |
| `DELAY` | `"options":[{"label":"秒","value":"SECOND"},{"label":"分钟","value":"MINUTE"},{"label":"小时","value":"HOUR"}]` | `"optionCategory":"delay_unit"` |

```sql
UPDATE node_type_registry
SET config_schema = REPLACE(config_schema,
  '"options":[{"label":"拒绝","value":"REJECT"},{"label":"通过","value":"APPROVE"},{"label":"持续等待","value":"KEEP_WAITING"}]',
  '"optionCategory":"approval_timeout_action"')
WHERE type_key = 'MANUAL_APPROVAL';
```

- [ ] **Step 5: Run migration syntax validation through backend tests**

Run:

```bash
mvn -pl backend/canvas-engine test -DskipTests
```

Expected: Maven compiles resources without XML or SQL resource parsing errors.

- [ ] **Step 6: Commit migration**

```bash
git add backend/canvas-engine/src/main/resources/db/migration/V53__system_options_and_ab_groups.sql
git commit -m "feat: add system option migration"
```

---

### Task 2: Backend System Option Domain and Service

**Files:**
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/meta/SystemOption.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/meta/SystemOptionMapper.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/meta/SystemOptionService.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/meta/SystemOptionServiceTest.java`

- [ ] **Step 1: Write service unit tests**

Create `SystemOptionServiceTest` using Mockito:

```java
package org.chovy.canvas.domain.meta;

import org.chovy.canvas.domain.meta.StubOption;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SystemOptionServiceTest {
    @Test
    void activeOptionsReturnsStubOptionsInMapperOrder() {
        SystemOptionMapper mapper = mock(SystemOptionMapper.class);
        SystemOption option = new SystemOption();
        option.setOptionKey("EQ");
        option.setLabel("等于");
        when(mapper.selectList(any())).thenReturn(List.of(option));

        SystemOptionService service = new SystemOptionService(mapper);

        List<StubOption> options = service.activeOptions("condition_operator");

        assertThat(options).extracting(StubOption::getKey).containsExactly("EQ");
        assertThat(options).extracting(StubOption::getLabel).containsExactly("等于");
    }

    @Test
    void updateEditableRejectsMissingOption() {
        SystemOptionMapper mapper = mock(SystemOptionMapper.class);
        when(mapper.selectById(99L)).thenReturn(null);
        SystemOptionService service = new SystemOptionService(mapper);

        SystemOption patch = new SystemOption();
        patch.setLabel("新标签");

        assertThatThrownBy(() -> service.updateEditable(99L, patch))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("系统选项不存在");
    }

    @Test
    void updateEditableOnlyWritesEditableFields() {
        SystemOptionMapper mapper = mock(SystemOptionMapper.class);
        SystemOption existing = new SystemOption();
        existing.setId(1L);
        existing.setCategory("condition_operator");
        existing.setOptionKey("EQ");
        existing.setLabel("等于");
        existing.setEnabled(1);
        existing.setSystemBuiltin(1);
        when(mapper.selectById(1L)).thenReturn(existing);
        SystemOptionService service = new SystemOptionService(mapper);

        SystemOption patch = new SystemOption();
        patch.setCategory("other");
        patch.setOptionKey("BAD");
        patch.setLabel("等于（改）");
        patch.setDescription("描述");
        patch.setSortOrder(90);
        patch.setEnabled(0);

        service.updateEditable(1L, patch);

        verify(mapper).updateById(argThat(updated ->
                updated.getId().equals(1L)
                        && updated.getCategory().equals("condition_operator")
                        && updated.getOptionKey().equals("EQ")
                        && updated.getLabel().equals("等于（改）")
                        && updated.getEnabled() == 0));
    }
}
```

- [ ] **Step 2: Run tests and verify failure**

Run:

```bash
mvn -pl backend/canvas-engine -Dtest=SystemOptionServiceTest test
```

Expected: FAIL because `SystemOption`, `SystemOptionMapper`, and `SystemOptionService` do not exist.

- [ ] **Step 3: Implement entity and mapper**

Create `SystemOption.java`:

```java
package org.chovy.canvas.domain.meta;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("system_option")
public class SystemOption {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String category;
    private String optionKey;
    private String label;
    private String description;
    private Integer sortOrder;
    private Integer enabled;
    private Integer systemBuiltin;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
```

Create `SystemOptionMapper.java`:

```java
package org.chovy.canvas.domain.meta;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SystemOptionMapper extends BaseMapper<SystemOption> {
}
```

- [ ] **Step 4: Implement service**

Create `SystemOptionService.java`:

```java
package org.chovy.canvas.domain.meta;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SystemOptionService {
    private final SystemOptionMapper mapper;

    public List<SystemOption> listForAdmin(String category, Integer enabled, String keyword) {
        LambdaQueryWrapper<SystemOption> wrapper = new LambdaQueryWrapper<SystemOption>()
                .eq(category != null && !category.isBlank(), SystemOption::getCategory, category)
                .eq(enabled != null, SystemOption::getEnabled, enabled)
                .and(keyword != null && !keyword.isBlank(), w -> w
                        .like(SystemOption::getOptionKey, keyword)
                        .or()
                        .like(SystemOption::getLabel, keyword))
                .orderByAsc(SystemOption::getCategory)
                .orderByAsc(SystemOption::getSortOrder)
                .orderByAsc(SystemOption::getId);
        return mapper.selectList(wrapper);
    }

    public List<StubOption> activeOptions(String category) {
        return activeSystemOptions(category).stream()
                .map(option -> new StubOption(option.getOptionKey(), option.getLabel()))
                .toList();
    }

    public List<SystemOption> activeSystemOptions(String category) {
        if (category == null || category.isBlank()) {
            throw new IllegalArgumentException("category is required");
        }
        return mapper.selectList(new LambdaQueryWrapper<SystemOption>()
                .eq(SystemOption::getCategory, category)
                .eq(SystemOption::getEnabled, 1)
                .orderByAsc(SystemOption::getSortOrder)
                .orderByAsc(SystemOption::getId));
    }

    public void updateEditable(Long id, SystemOption patch) {
        SystemOption existing = mapper.selectById(id);
        if (existing == null) {
            throw new IllegalArgumentException("系统选项不存在: " + id);
        }
        existing.setLabel(patch.getLabel());
        existing.setDescription(patch.getDescription());
        existing.setSortOrder(patch.getSortOrder());
        existing.setEnabled(patch.getEnabled());
        mapper.updateById(existing);
    }
}
```

- [ ] **Step 5: Run backend tests**

Run:

```bash
mvn -pl backend/canvas-engine -Dtest=SystemOptionServiceTest test
```

Expected: PASS.

- [ ] **Step 6: Commit backend system option domain**

```bash
git add backend/canvas-engine/src/main/java/org/chovy/canvas/domain/meta/SystemOption.java \
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/meta/SystemOptionMapper.java \
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/meta/SystemOptionService.java \
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/meta/SystemOptionServiceTest.java
git commit -m "feat: add system option service"
```

---

### Task 3: Backend System Option Admin APIs

**Files:**
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/controller/SystemOptionController.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/controller/SystemOptionControllerTest.java`

- [ ] **Step 1: Write controller tests**

Create `SystemOptionControllerTest.java`:

```java
package org.chovy.canvas.controller;

import org.chovy.canvas.domain.meta.SystemOption;
import org.chovy.canvas.domain.meta.SystemOptionService;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class SystemOptionControllerTest {
    @Test
    void listReturnsPageResultShape() {
        SystemOptionService service = mock(SystemOptionService.class);
        SystemOption option = new SystemOption();
        option.setId(1L);
        option.setCategory("http_method");
        option.setOptionKey("POST");
        option.setLabel("POST");
        when(service.listForAdmin("http_method", null, null)).thenReturn(List.of(option));
        SystemOptionController controller = new SystemOptionController(service);

        StepVerifier.create(controller.list("http_method", null, null))
                .assertNext(result -> {
                    assertThat(result.getData().getTotal()).isEqualTo(1);
                    assertThat(result.getData().getList()).extracting(SystemOption::getOptionKey).containsExactly("POST");
                })
                .verifyComplete();
    }

    @Test
    void updateDelegatesToService() {
        SystemOptionService service = mock(SystemOptionService.class);
        SystemOptionController controller = new SystemOptionController(service);
        SystemOption patch = new SystemOption();
        patch.setLabel("POST（改）");

        StepVerifier.create(controller.update(1L, patch))
                .assertNext(result -> assertThat(result.getCode()).isEqualTo(0))
                .verifyComplete();

        verify(service).updateEditable(1L, patch);
    }
}
```

- [ ] **Step 2: Run test and verify failure**

Run:

```bash
mvn -pl backend/canvas-engine -Dtest=SystemOptionControllerTest test
```

Expected: FAIL because `SystemOptionController` does not exist.

- [ ] **Step 3: Implement controller**

Create `SystemOptionController.java`:

```java
package org.chovy.canvas.controller;

import lombok.RequiredArgsConstructor;
import org.chovy.canvas.common.PageResult;
import org.chovy.canvas.common.R;
import org.chovy.canvas.domain.meta.SystemOption;
import org.chovy.canvas.domain.meta.SystemOptionService;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import java.util.List;

@RestController
@RequestMapping("/admin/system-options")
@RequiredArgsConstructor
public class SystemOptionController {
    private final SystemOptionService service;

    @GetMapping
    public Mono<R<PageResult<SystemOption>>> list(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Integer enabled,
            @RequestParam(required = false) String keyword) {
        return Mono.fromCallable(() -> {
                    List<SystemOption> rows = service.listForAdmin(category, enabled, keyword);
                    return PageResult.of(rows.size(), rows);
                })
                .subscribeOn(Schedulers.boundedElastic())
                .map(R::ok);
    }

    @PutMapping("/{id}")
    public Mono<R<Void>> update(@PathVariable Long id, @RequestBody SystemOption body) {
        return Mono.<Void>fromRunnable(() -> service.updateEditable(id, body))
                .subscribeOn(Schedulers.boundedElastic())
                .thenReturn(R.ok());
    }
}
```

- [ ] **Step 4: Run controller tests**

Run:

```bash
mvn -pl backend/canvas-engine -Dtest=SystemOptionControllerTest,SystemOptionServiceTest test
```

Expected: PASS.

- [ ] **Step 5: Commit admin APIs**

```bash
git add backend/canvas-engine/src/main/java/org/chovy/canvas/controller/SystemOptionController.java \
  backend/canvas-engine/src/test/java/org/chovy/canvas/controller/SystemOptionControllerTest.java
git commit -m "feat: add system option admin api"
```

---

### Task 4: Backend Option Metadata APIs and Meta Stub Replacement

**Files:**
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/controller/MetaController.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/meta/MetaService.java`
- Create or modify tests under `backend/canvas-engine/src/test/java/org/chovy/canvas/controller/MetaControllerTest.java`

- [ ] **Step 1: Add failing tests for meta option lookup**

Create `MetaControllerTest.java`. Test through controller methods directly:

```java
package org.chovy.canvas.controller;

import org.chovy.canvas.common.R;
import org.chovy.canvas.domain.meta.*;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class MetaControllerTest {
    @Test
    void getOptionsReturnsSystemOptions() {
        SystemOptionService options = mock(SystemOptionService.class);
        when(options.activeOptions("http_method"))
                .thenReturn(List.of(new StubOption("POST", "POST")));
        MetaController controller = MetaControllerTestFactory.withSystemOptions(options);

        StepVerifier.create(controller.getOptions("http_method"))
                .assertNext(result -> {
                    assertThat(result.getCode()).isEqualTo(0);
                    assertThat(result.getData()).extracting(StubOption::getKey).containsExactly("POST");
                })
                .verifyComplete();
    }

    private static final class MetaControllerTestFactory {
        static MetaController withSystemOptions(SystemOptionService systemOptions) {
            return new MetaController(
                    mock(MetaService.class),
                    mock(ApiDefinitionMapper.class),
                    mock(AbExperimentMapper.class),
                    mock(TagDefinitionMapper.class),
                    mock(MqMessageDefinitionMapper.class),
                    mock(EventDefinitionMapper.class),
                    new ObjectMapper(),
                    systemOptions,
                    mock(AbExperimentGroupService.class));
        }
    }
}
```

- [ ] **Step 2: Run test and verify failure**

Run:

```bash
mvn -pl backend/canvas-engine -Dtest=MetaControllerTest test
```

Expected: FAIL because `getOptions` and the new constructor dependency do not exist.

- [ ] **Step 3: Modify `MetaController`**

Inject `SystemOptionService` and add endpoints. When Task 5 adds AB group metadata, `MetaController` will also inject `AbExperimentGroupService`; keep constructor field order aligned with `MetaControllerTestFactory`.

```java
private final SystemOptionService systemOptionService;

@GetMapping("/options")
public Mono<R<List<StubOption>>> getOptions(@RequestParam String category) {
    return Mono.fromCallable(() -> systemOptionService.activeOptions(category))
            .subscribeOn(Schedulers.boundedElastic())
            .map(R::ok);
}

@GetMapping("/options/batch")
public Mono<R<Map<String, List<StubOption>>>> getOptionsBatch(@RequestParam List<String> categories) {
    return Mono.fromCallable(() -> categories.stream()
                    .distinct()
                    .collect(Collectors.toMap(c -> c, systemOptionService::activeOptions)))
            .subscribeOn(Schedulers.boundedElastic())
            .map(R::ok);
}
```

Replace these endpoints to use `systemOptionService.activeOptions(...)`:

```java
@GetMapping("/mq-topics")
public Mono<R<List<StubOption>>> getMqTopics() {
    return Mono.just(R.ok(systemOptionService.activeOptions("mq_topic_legacy")));
}

@GetMapping("/coupon-types")
public Mono<R<List<StubOption>>> getCouponTypes() {
    return Mono.just(R.ok(systemOptionService.activeOptions("coupon_type")));
}

@GetMapping("/reach-scenes")
public Mono<R<List<StubOption>>> getReachScenes() {
    return Mono.just(R.ok(systemOptionService.activeOptions("reach_scene")));
}

@GetMapping("/biz-lines")
public Mono<R<List<StubOption>>> getBizLines() {
    return Mono.just(R.ok(systemOptionService.activeOptions("biz_line")));
}

@GetMapping("/biz-lines/{key}/apis")
public Mono<R<List<StubOption>>> getBizLineApis(@PathVariable String key) {
    return Mono.just(R.ok(systemOptionService.activeOptions("biz_line_api")));
}

@GetMapping("/behavior-strategy-types")
public Mono<R<List<StubOption>>> getBehaviorStrategyTypes() {
    return Mono.just(R.ok(systemOptionService.activeOptions("behavior_strategy_type")));
}

@GetMapping("/message-codes")
public Mono<R<List<StubOption>>> getMessageCodes(@RequestParam(defaultValue = "IN_APP") String type) {
    String category = "MQ".equals(type) ? "message_code_mq" : "message_code_in_app";
    return Mono.just(R.ok(systemOptionService.activeOptions(category)));
}
```

- [ ] **Step 4: Reduce `MetaService` stubs**

Remove or stop using these methods from `MetaService`: `getMqTopics`, `getCouponTypes`, `getReachScenes`, `getBizLines`, `getBizLineApis`, `getBehaviorStrategyTypes`, `getMessageCodes`. Keep methods still used by other classes until references are removed.

- [ ] **Step 5: Run tests**

Run:

```bash
mvn -pl backend/canvas-engine -Dtest=MetaControllerTest,SystemOptionServiceTest test
```

Expected: PASS.

- [ ] **Step 6: Commit API replacement**

```bash
git add backend/canvas-engine/src/main/java/org/chovy/canvas/controller/MetaController.java \
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/meta/MetaService.java \
  backend/canvas-engine/src/test/java/org/chovy/canvas/controller/MetaControllerTest.java
git commit -m "feat: expose system option metadata"
```

---

### Task 5: Backend AB Experiment Groups

**Files:**
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/meta/AbExperimentGroup.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/meta/AbExperimentGroupMapper.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/meta/AbExperimentGroupService.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/controller/AbExperimentController.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/controller/MetaController.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/meta/AbExperimentGroupServiceTest.java`

- [ ] **Step 1: Write service tests**

Create tests:

```java
package org.chovy.canvas.domain.meta;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AbExperimentGroupServiceTest {
    @Test
    void createDefaultsCreatesAAndB() {
        AbExperimentGroupMapper mapper = mock(AbExperimentGroupMapper.class);
        AbExperimentGroupService service = new AbExperimentGroupService(mapper);

        service.ensureDefaultGroups(12L);

        verify(mapper, times(2)).insert(argThat(group ->
                group.getExperimentId().equals(12L)
                        && List.of("A", "B").contains(group.getGroupKey())
                        && group.getEnabled() == 1));
    }

    @Test
    void activeOptionsMapsGroupKeyToLabel() {
        AbExperimentGroupMapper mapper = mock(AbExperimentGroupMapper.class);
        AbExperimentGroup group = new AbExperimentGroup();
        group.setGroupKey("A");
        group.setLabel("A组");
        when(mapper.selectList(any())).thenReturn(List.of(group));
        AbExperimentGroupService service = new AbExperimentGroupService(mapper);

        assertThat(service.activeGroupOptions(1L))
                .extracting(StubOption::getKey)
                .containsExactly("A");
    }
}
```

- [ ] **Step 2: Run test and verify failure**

Run:

```bash
mvn -pl backend/canvas-engine -Dtest=AbExperimentGroupServiceTest test
```

Expected: FAIL because AB group classes do not exist.

- [ ] **Step 3: Implement AB group entity and mapper**

Create `AbExperimentGroup.java`:

```java
package org.chovy.canvas.domain.meta;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("ab_experiment_group")
public class AbExperimentGroup {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long experimentId;
    private String groupKey;
    private String label;
    private Integer sortOrder;
    private Integer enabled;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
```

Create `AbExperimentGroupMapper.java`:

```java
package org.chovy.canvas.domain.meta;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AbExperimentGroupMapper extends BaseMapper<AbExperimentGroup> {
}
```

- [ ] **Step 4: Implement AB group service**

Create `AbExperimentGroupService.java`:

```java
package org.chovy.canvas.domain.meta;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AbExperimentGroupService {
    private final AbExperimentGroupMapper mapper;

    public List<AbExperimentGroup> list(Long experimentId, boolean includeDisabled) {
        return mapper.selectList(new LambdaQueryWrapper<AbExperimentGroup>()
                .eq(AbExperimentGroup::getExperimentId, experimentId)
                .eq(!includeDisabled, AbExperimentGroup::getEnabled, 1)
                .orderByAsc(AbExperimentGroup::getSortOrder)
                .orderByAsc(AbExperimentGroup::getId));
    }

    public List<StubOption> activeGroupOptions(Long experimentId) {
        return list(experimentId, false).stream()
                .map(group -> new StubOption(group.getGroupKey(), group.getLabel()))
                .toList();
    }

    public void ensureDefaultGroups(Long experimentId) {
        insertDefault(experimentId, "A", "A组", 10);
        insertDefault(experimentId, "B", "B组", 20);
    }

    private void insertDefault(Long experimentId, String groupKey, String label, int sortOrder) {
        AbExperimentGroup group = new AbExperimentGroup();
        group.setExperimentId(experimentId);
        group.setGroupKey(groupKey);
        group.setLabel(label);
        group.setSortOrder(sortOrder);
        group.setEnabled(1);
        mapper.insert(group);
    }

    public AbExperimentGroup create(Long experimentId, AbExperimentGroup body) {
        validateGroupKey(body.getGroupKey());
        body.setExperimentId(experimentId);
        if (body.getEnabled() == null) body.setEnabled(1);
        mapper.insert(body);
        return body;
    }

    public void update(Long groupId, AbExperimentGroup body) {
        AbExperimentGroup existing = mapper.selectById(groupId);
        if (existing == null) throw new IllegalArgumentException("AB 分组不存在: " + groupId);
        existing.setLabel(body.getLabel());
        existing.setSortOrder(body.getSortOrder());
        existing.setEnabled(body.getEnabled());
        mapper.updateById(existing);
    }

    public void disable(Long groupId) {
        AbExperimentGroup existing = mapper.selectById(groupId);
        if (existing == null) throw new IllegalArgumentException("AB 分组不存在: " + groupId);
        existing.setEnabled(0);
        mapper.updateById(existing);
    }

    private void validateGroupKey(String key) {
        if (key == null || !key.matches("[A-Za-z0-9_-]{1,64}")) {
            throw new IllegalArgumentException("groupKey 只能包含字母、数字、下划线和中划线，长度 1-64");
        }
    }
}
```

- [ ] **Step 5: Wire controller methods**

Inject `AbExperimentGroupService` into `AbExperimentController`. After experiment insert, call:

```java
abExperimentGroupService.ensureDefaultGroups(body.getId());
```

Add group endpoints:

```java
@GetMapping("/{id}/groups")
public Mono<R<List<AbExperimentGroup>>> listGroups(
        @PathVariable Long id,
        @RequestParam(defaultValue = "false") boolean includeDisabled) {
    return Mono.fromCallable(() -> abExperimentGroupService.list(id, includeDisabled))
            .subscribeOn(Schedulers.boundedElastic())
            .map(R::ok);
}

@PostMapping("/{id}/groups")
public Mono<R<AbExperimentGroup>> createGroup(@PathVariable Long id, @RequestBody AbExperimentGroup body) {
    return Mono.fromCallable(() -> abExperimentGroupService.create(id, body))
            .subscribeOn(Schedulers.boundedElastic())
            .map(R::ok);
}

@PutMapping("/{id}/groups/{groupId}")
public Mono<R<Void>> updateGroup(@PathVariable Long groupId, @RequestBody AbExperimentGroup body) {
    return Mono.fromRunnable(() -> abExperimentGroupService.update(groupId, body))
            .subscribeOn(Schedulers.boundedElastic())
            .thenReturn(R.ok());
}

@DeleteMapping("/{id}/groups/{groupId}")
public Mono<R<Void>> deleteGroup(@PathVariable Long groupId) {
    return Mono.fromRunnable(() -> abExperimentGroupService.disable(groupId))
            .subscribeOn(Schedulers.boundedElastic())
            .thenReturn(R.ok());
}
```

- [ ] **Step 6: Replace AB meta groups**

Modify `MetaController.getAbExperimentGroups(String key)` to resolve experiment by `experimentKey` and return `abExperimentGroupService.activeGroupOptions(experiment.getId())`. Return `R.ok(List.of())` if the experiment does not exist.

- [ ] **Step 7: Run backend tests**

Run:

```bash
mvn -pl backend/canvas-engine -Dtest=AbExperimentGroupServiceTest,MetaControllerTest test
```

Expected: PASS.

- [ ] **Step 8: Commit AB group backend**

```bash
git add backend/canvas-engine/src/main/java/org/chovy/canvas/domain/meta/AbExperimentGroup.java \
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/meta/AbExperimentGroupMapper.java \
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/meta/AbExperimentGroupService.java \
  backend/canvas-engine/src/main/java/org/chovy/canvas/controller/AbExperimentController.java \
  backend/canvas-engine/src/main/java/org/chovy/canvas/controller/MetaController.java \
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/meta/AbExperimentGroupServiceTest.java
git commit -m "feat: configure ab experiment groups"
```

---

### Task 6: Frontend System Option API and Hooks

**Files:**
- Create: `frontend/src/services/systemOptions.ts`
- Create: `frontend/src/hooks/useSystemOptions.ts`
- Create: `frontend/src/hooks/useSystemOptions.test.ts`
- Modify: `frontend/src/services/api.ts`
- Modify: `frontend/src/types/index.ts`

- [ ] **Step 1: Write hook tests**

Create helper tests that do not require React rendering:

```ts
import { describe, expect, it } from 'vitest'
import { mergeCurrentValueOption, toSelectOptions } from './useSystemOptions'

describe('system option helpers', () => {
  it('normalizes system options for Select', () => {
    expect(toSelectOptions([{ optionKey: 'POST', label: 'POST', enabled: 1 } as any]))
      .toEqual([{ value: 'POST', label: 'POST' }])
  })

  it('adds disabled current value when missing from active options', () => {
    expect(mergeCurrentValueOption([{ value: 'POST', label: 'POST' }], 'GET'))
      .toEqual([
        { value: 'GET', label: '已禁用：GET' },
        { value: 'POST', label: 'POST' },
      ])
  })
})
```

- [ ] **Step 2: Run test and verify failure**

Run:

```bash
cd frontend && npm test -- useSystemOptions.test.ts
```

Expected: FAIL because `useSystemOptions.ts` does not exist.

- [ ] **Step 3: Add shared types**

Modify `frontend/src/types/index.ts`:

```ts
export interface SystemOption {
  id: number
  category: string
  optionKey: string
  label: string
  description?: string
  sortOrder: number
  enabled: 0 | 1
  systemBuiltin: 0 | 1
  createdAt?: string
  updatedAt?: string
}

export type SelectOption = { label: string; value: string }
```

Add `optionCategory?: string` to both `SchemaField` interfaces in `frontend/src/types/index.ts` and `frontend/src/components/config-panel/index.tsx`.

- [ ] **Step 4: Create API module**

Create `frontend/src/services/systemOptions.ts`:

```ts
import http from './api'
import type { PageResult, R, StubOption, SystemOption } from '../types'

export const systemOptionsApi = {
  meta: (category: string) =>
    http.get<R<StubOption[]>, R<StubOption[]>>('/meta/options', { params: { category } }),
  metaBatch: (categories: string[]) =>
    http.get<R<Record<string, StubOption[]>>, R<Record<string, StubOption[]>>>('/meta/options/batch', {
      params: { categories },
      paramsSerializer: params => (params.categories as string[]).map(c => `categories=${encodeURIComponent(c)}`).join('&'),
    }),
  adminList: (params?: { category?: string; enabled?: number; keyword?: string }) =>
    http.get<R<PageResult<SystemOption>>, R<PageResult<SystemOption>>>('/admin/system-options', { params }),
  update: (id: number, body: Partial<SystemOption>) =>
    http.put<R<void>, R<void>>(`/admin/system-options/${id}`, body),
}
```

- [ ] **Step 5: Create hook helpers and hooks**

Create `frontend/src/hooks/useSystemOptions.ts`:

```ts
import { useEffect, useMemo, useState } from 'react'
import { systemOptionsApi } from '../services/systemOptions'
import type { StubOption, SystemOption } from '../types'

export function toSelectOptions(options: Array<SystemOption | StubOption>) {
  return options.map((option: any) => ({
    value: String(option.optionKey ?? option.key ?? option.value),
    label: String(option.label ?? option.optionKey ?? option.key ?? option.value),
  }))
}

export function mergeCurrentValueOption(options: { value: string; label: string }[], currentValue: unknown) {
  if (currentValue === undefined || currentValue === null || currentValue === '') return options
  const value = String(currentValue)
  if (options.some(option => String(option.value) === value)) return options
  return [{ value, label: `已禁用：${value}` }, ...options]
}

export function useSystemOptions(category: string | undefined, currentValue?: unknown) {
  const [raw, setRaw] = useState<StubOption[]>([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<Error | null>(null)

  useEffect(() => {
    if (!category) return
    setLoading(true)
    setError(null)
    systemOptionsApi.meta(category)
      .then(res => setRaw(res.data))
      .catch(err => setError(err))
      .finally(() => setLoading(false))
  }, [category])

  const options = useMemo(
    () => mergeCurrentValueOption(toSelectOptions(raw), currentValue),
    [raw, currentValue],
  )

  return { options, loading, error }
}
```

- [ ] **Step 6: Run frontend hook tests**

Run:

```bash
cd frontend && npm test -- useSystemOptions.test.ts
```

Expected: PASS.

- [ ] **Step 7: Commit frontend option infrastructure**

```bash
git add frontend/src/services/systemOptions.ts frontend/src/hooks/useSystemOptions.ts \
  frontend/src/hooks/useSystemOptions.test.ts frontend/src/types/index.ts frontend/src/services/api.ts
git commit -m "feat: add frontend system option hooks"
```

---

### Task 7: System Options Admin Page

**Files:**
- Create: `frontend/src/pages/system-options/index.tsx`
- Modify: `frontend/src/App.tsx`
- Modify: `frontend/src/components/layout/AppLayout.tsx`

- [ ] **Step 1: Add route and menu entry**

In `App.tsx`, import the page and add an admin route:

```tsx
import SystemOptionsPage from './pages/system-options'

<Route path="/system-options" element={<SystemOptionsPage />} />
```

In `AppLayout.tsx`, add selected key handling:

```ts
if (location.pathname.startsWith('/system-options')) return 'system-options'
```

Add a system settings menu item:

```tsx
{
  key: 'system-options',
  icon: <SettingOutlined />,
  label: '系统选项配置',
  onClick: () => navigate('/system-options'),
}
```

- [ ] **Step 2: Create management page**

Create `frontend/src/pages/system-options/index.tsx`:

```tsx
import { useEffect, useState } from 'react'
import { Button, Form, Input, InputNumber, Modal, Select, Space, Switch, Table, Tag, Typography, message } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import type { SystemOption } from '../../types'
import { systemOptionsApi } from '../../services/systemOptions'

const { Title } = Typography

const CATEGORY_OPTIONS = [
  { value: 'condition_operator', label: '条件操作符' },
  { value: 'audience_condition_operator', label: '人群规则操作符' },
  { value: 'logic_relation', label: '逻辑关系' },
  { value: 'param_type', label: '参数类型' },
  { value: 'http_method', label: 'HTTP 方法' },
  { value: 'user_role', label: '用户角色' },
]

export default function SystemOptionsPage() {
  const [data, setData] = useState<SystemOption[]>([])
  const [loading, setLoading] = useState(false)
  const [category, setCategory] = useState<string>()
  const [editing, setEditing] = useState<SystemOption | null>(null)
  const [form] = Form.useForm()

  const fetchList = async () => {
    setLoading(true)
    try {
      const res = await systemOptionsApi.adminList({ category })
      setData(res.data.list)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { fetchList() }, [category])

  const openEdit = (record: SystemOption) => {
    setEditing(record)
    form.setFieldsValue({ ...record, enabled: record.enabled === 1 })
  }

  const save = async () => {
    if (!editing) return
    const values = await form.validateFields()
    await systemOptionsApi.update(editing.id, {
      label: values.label,
      description: values.description,
      sortOrder: values.sortOrder,
      enabled: values.enabled ? 1 : 0,
    })
    message.success('保存成功')
    setEditing(null)
    fetchList()
  }

  const columns: ColumnsType<SystemOption> = [
    { title: '分类', dataIndex: 'category', width: 180 },
    { title: 'Key', dataIndex: 'optionKey', width: 160 },
    { title: '显示名', dataIndex: 'label' },
    { title: '排序', dataIndex: 'sortOrder', width: 80 },
    { title: '状态', dataIndex: 'enabled', width: 80, render: v => v === 1 ? <Tag color="green">启用</Tag> : <Tag>禁用</Tag> },
    { title: '内置', dataIndex: 'systemBuiltin', width: 80, render: v => v === 1 ? <Tag color="blue">内置</Tag> : <Tag>自定义</Tag> },
    { title: '操作', width: 90, render: (_, record) => <Button size="small" onClick={() => openEdit(record)}>编辑</Button> },
  ]

  return (
    <div style={{ padding: 24 }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 16 }}>
        <Title level={4} style={{ margin: 0 }}>系统选项配置</Title>
        <Select allowClear placeholder="筛选分类" style={{ width: 240 }} options={CATEGORY_OPTIONS} value={category} onChange={setCategory} />
      </div>
      <Table rowKey="id" dataSource={data} columns={columns} loading={loading} pagination={false} />
      <Modal title="编辑系统选项" open={!!editing} onOk={save} onCancel={() => setEditing(null)} okText="保存" cancelText="取消">
        <Form form={form} layout="vertical" style={{ marginTop: 16 }}>
          <Form.Item label="分类"><Input value={editing?.category} disabled /></Form.Item>
          <Form.Item label="Key"><Input value={editing?.optionKey} disabled /></Form.Item>
          <Form.Item name="label" label="显示名" rules={[{ required: true }]}><Input /></Form.Item>
          <Form.Item name="description" label="描述"><Input.TextArea rows={2} /></Form.Item>
          <Form.Item name="sortOrder" label="排序"><InputNumber style={{ width: '100%' }} /></Form.Item>
          <Form.Item name="enabled" label="启用" valuePropName="checked"><Switch /></Form.Item>
        </Form>
      </Modal>
    </div>
  )
}
```

- [ ] **Step 3: Run frontend build**

Run:

```bash
cd frontend && npm run build
```

Expected: PASS.

- [ ] **Step 4: Commit admin page**

```bash
git add frontend/src/pages/system-options/index.tsx frontend/src/App.tsx frontend/src/components/layout/AppLayout.tsx
git commit -m "feat: add system option admin page"
```

---

### Task 8: Replace Standalone Page Hardcoded Dropdowns

**Files:**
- Modify: `frontend/src/pages/api-config/index.tsx`
- Modify: `frontend/src/pages/mq-config/index.tsx`
- Modify: `frontend/src/pages/event-config/index.tsx`
- Modify: `frontend/src/pages/tag-config/index.tsx`
- Modify: `frontend/src/pages/audience-edit/index.tsx`
- Modify: `frontend/src/pages/admin/index.tsx`

- [ ] **Step 1: Replace API config constants**

In `api-config/index.tsx`, remove exported `PARAM_TYPES`. Load options:

```tsx
const { options: paramTypeOptions } = useSystemOptions('param_type')
const { options: methodOptions } = useSystemOptions('http_method')
```

Pass `paramTypeOptions` into `ParamSchemaEditor` as a prop and use `methodOptions` in the method `<Select>`.

- [ ] **Step 2: Replace MQ and event config types**

In `mq-config/index.tsx`, stop importing `PARAM_TYPES` from API config. Load `param_type` options and pass them to its `ParamSchemaEditor`.

In `event-config/index.tsx`, load `event_attr_type` options and pass them to `AttrSchemaEditor`.

- [ ] **Step 3: Replace tag config type options**

In `tag-config/index.tsx`, load:

```tsx
const { options: tagTypeOptions } = useSystemOptions('tag_type')
```

Use it in the tag type `<Select>`.

- [ ] **Step 4: Replace audience edit options**

In `audience-edit/index.tsx`, load:

```tsx
const { options: dataSourceOptions } = useSystemOptions('audience_data_source_type')
const { options: evaluationOptions } = useSystemOptions('audience_evaluation_strategy')
const { options: engineOptions } = useSystemOptions('audience_engine_type')
const { options: combinatorOptions } = useSystemOptions('query_combinator')
const { options: audienceOperatorOptions } = useSystemOptions('audience_condition_operator')
```

Map `audienceOperatorOptions` into `react-querybuilder` shape:

```tsx
const queryOperators = audienceOperatorOptions.map(option => ({ name: option.value, label: option.label }))
const queryCombinators = combinatorOptions.map(option => ({ name: option.value, label: option.label }))
```

Use these arrays for `operators` and `combinators`.

- [ ] **Step 5: Replace admin role options**

In `admin/index.tsx`, load `user_role` and use it in the role `<Select>`.

- [ ] **Step 6: Run frontend tests and build**

Run:

```bash
cd frontend && npm test
cd frontend && npm run build
```

Expected: PASS.

- [ ] **Step 7: Commit standalone page replacements**

```bash
git add frontend/src/pages/api-config/index.tsx frontend/src/pages/mq-config/index.tsx \
  frontend/src/pages/event-config/index.tsx frontend/src/pages/tag-config/index.tsx \
  frontend/src/pages/audience-edit/index.tsx frontend/src/pages/admin/index.tsx
git commit -m "feat: load page dropdowns from system options"
```

---

### Task 9: Config Panel OptionCategory and Hardcoded Control Replacement

**Files:**
- Modify: `frontend/src/components/config-panel/index.tsx`
- Modify: `frontend/src/components/config-panel/displayValues.ts`
- Modify: `frontend/src/components/config-panel/CronBuilder.tsx`
- Modify tests under `frontend/src/components/config-panel`

- [ ] **Step 1: Add optionCategory loading in ConfigPanel**

In `ConfigPanel`, after parsing schema fields, load both `dataSource` and `optionCategory` options into the existing `options` map. Use `/meta/options?category=...` and store by field key:

```tsx
fields.filter(f => f.optionCategory).forEach(f => {
  const category = f.optionCategory!
  systemOptionsApi.meta(category).then(res =>
    setOptions(prev => ({ ...prev, [f.key]: res.data.map(o => ({ key: o.key, label: o.label })) }))
  )
})
```

Keep existing `dataSource` loading unchanged.

- [ ] **Step 2: Replace condition operator arrays**

Load `condition_operator` options once in `ConfigPanel` and pass them to `ConditionRuleList` and `BranchList`:

```tsx
const [conditionOps, setConditionOps] = useState<StubOption[]>([])
useEffect(() => {
  systemOptionsApi.meta('condition_operator').then(res => setConditionOps(res.data))
}, [])
```

Use:

```tsx
options={conditionOps.map(o => ({ label: o.label, value: o.key }))}
```

Remove `const ops = ['EQ', ...]` from `ConditionRuleList` and `BranchList`.

- [ ] **Step 3: Replace relation, context value, param type, delay unit**

Load these categories in `ConfigPanel` and pass them to child controls:

```tsx
logic_relation
context_value_type
param_type
delay_unit
```

Replace hardcoded arrays in `BranchList`, `ContextValueList`, `ParamDefineList`, and `DelayInput`.

- [ ] **Step 4: Update CronBuilder**

Change props to accept option arrays:

```ts
interface Props {
  value?: string
  onChange?: (cron: string) => void
  frequencyOptions?: { label: string; value: string }[]
  weekdayOptions?: { label: string; value: number }[]
}
```

Keep generated hour/minute/day options. Default to current hardcoded labels only when props are not passed.

- [ ] **Step 5: Run config panel tests**

Run:

```bash
cd frontend && npm test -- config-panel
```

Expected: PASS.

- [ ] **Step 6: Commit config panel option replacement**

```bash
git add frontend/src/components/config-panel/index.tsx frontend/src/components/config-panel/displayValues.ts \
  frontend/src/components/config-panel/CronBuilder.tsx frontend/src/components/config-panel/*.test.ts
git commit -m "feat: load config panel options from metadata"
```

---

### Task 10: AB Experiment Group UI and AB_SPLIT Sync

**Files:**
- Modify: `frontend/src/services/api.ts`
- Modify: `frontend/src/pages/ab-experiment/index.tsx`
- Modify: `frontend/src/components/config-panel/index.tsx`
- Modify: `frontend/src/components/canvas/branchHandles.ts`

- [ ] **Step 1: Add AB group frontend API**

Extend `abExperimentApi`:

```ts
groups: (id: number, includeDisabled = false) =>
  http.get<R<any[]>, R<any[]>>(`/canvas/ab-experiments/${id}/groups`, { params: { includeDisabled } }),
createGroup: (id: number, body: any) =>
  http.post<R<any>, R<any>>(`/canvas/ab-experiments/${id}/groups`, body),
updateGroup: (id: number, groupId: number, body: any) =>
  http.put<R<void>, R<void>>(`/canvas/ab-experiments/${id}/groups/${groupId}`, body),
deleteGroup: (id: number, groupId: number) =>
  http.delete<R<void>, R<void>>(`/canvas/ab-experiments/${id}/groups/${groupId}`),
```

- [ ] **Step 2: Add group drawer to AB experiment page**

In `ab-experiment/index.tsx`, add a row button `分组`. It opens a drawer with table columns `groupKey`, `label`, `sortOrder`, `enabled`, actions edit/disable, and a small create form.

Use `abExperimentApi.groups(record.id, true)` for management.

- [ ] **Step 3: Sync AB_SPLIT groups in config panel**

In `AbGroupList`, watch `experimentKey`:

```tsx
const experimentKey = Form.useWatch('experimentKey', form)
```

Resolve the selected experiment from `/meta/ab-experiments`, then load `/meta/ab-experiments/{key}/groups`. Merge loaded groups with existing `groups` by `groupKey`, preserving `nextNodeId`.

- [ ] **Step 4: Stop manual group key creation in AB_SPLIT**

Replace the current `添加分组` behavior with a read-only list derived from experiment groups. If no experiment is selected, show `请先选择实验`. If no group is returned, show `该实验未配置启用分组`.

- [ ] **Step 5: Keep branch handles compatible**

In `branchHandles.ts`, keep existing `groupKey` based handles. Use group label only for display if present:

```ts
label: `${g.label ?? g.groupKey} ${percent}%`
```

- [ ] **Step 6: Run frontend tests**

Run:

```bash
cd frontend && npm test
cd frontend && npm run build
```

Expected: PASS.

- [ ] **Step 7: Commit AB group frontend**

```bash
git add frontend/src/services/api.ts frontend/src/pages/ab-experiment/index.tsx \
  frontend/src/components/config-panel/index.tsx frontend/src/components/canvas/branchHandles.ts
git commit -m "feat: manage ab experiment groups"
```

---

### Task 11: Final Hardcoded Option Audit and Regression

**Files:**
- Modify files found by audit only if they still contain option hardcoding.

- [ ] **Step 1: Run hardcoded dropdown audit**

Run:

```bash
rg -n "const operators|const ops|PARAM_TYPES|options=\\[|<option|List\\.of\\(|getCouponTypes|getReachScenes|getBizLines|getBehaviorStrategyTypes|getMessageCodes|getAbExperimentGroups" frontend/src backend/canvas-engine/src/main/java backend/canvas-engine/src/main/resources/db/migration -g '*.{ts,tsx,java,sql}'
```

Expected remaining matches:

- Test fixtures.
- Migration seed data in `V53__system_options_and_ab_groups.sql`.
- Generated numeric range options in `CronBuilder`.
- `field.options` fallback support in config panel and display helpers.
- Domain-specific table data sources such as API, MQ, event, tag, audience.

- [ ] **Step 2: Remove unexpected matches**

For each unexpected match, replace it with `useSystemOptions`, `optionCategory`, or a `systemOptionService.activeOptions(category)` lookup. Run the audit command again until only expected matches remain.

- [ ] **Step 3: Full backend verification**

Run:

```bash
mvn -pl backend/canvas-engine test
```

Expected: PASS.

- [ ] **Step 4: Full frontend verification**

Run:

```bash
cd frontend && npm test
cd frontend && npm run build
```

Expected: PASS.

- [ ] **Step 5: Manual smoke checklist**

Start the local frontend and backend the usual project way, then verify:

```text
系统选项配置页：能筛选、编辑显示名、调整排序、禁用/启用。
API 配置：参数类型和方法下拉可选，保存成功。
MQ 配置：参数类型下拉可选，保存成功。
事件配置：属性类型下拉可选，保存成功。
标签配置：标签类型下拉可选，保存成功。
人群编辑：规则操作符、组合关系、数据源、计算策略、规则引擎可选。
用户管理：角色下拉可选。
画布 IF/SELECTOR：条件操作符从字典展示。
画布 THRESHOLD/AGGREGATE/TAGGER/MANUAL_APPROVAL：schema optionCategory 下拉正常。
AB 实验管理：分组可维护。
AB_SPLIT：选择实验后按分组展示路由项。
```

- [ ] **Step 6: Final commit**

If Step 2 changed files, commit them:

```bash
git add frontend/src backend/canvas-engine/src/main/java backend/canvas-engine/src/main/resources/db/migration
git commit -m "chore: complete system option hardcoded audit"
```

---

## Self-Review Checklist

- Spec coverage: tasks cover system option table, AB group table, metadata endpoints, admin page, standalone pages, config panel, AB split, migration, and final audit.
- Placeholder scan: no task depends on unspecified files or deferred requirements.
- Type consistency: backend uses `optionKey`; frontend maps metadata `StubOption.key` to `Select.value`; schema uses `optionCategory`.
