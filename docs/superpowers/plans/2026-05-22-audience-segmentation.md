# 标签人群圈选 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 实现端到端的人群圈选系统：规则存储（数据库） → 双引擎评估（AviatorScript/QLExpress） → Roaring Bitmap 存储（Redis） → 批量计算任务 → TAGGER 节点集成 → 前端可视化规则构建器。

**Architecture:** 两张新表（audience_definition + audience_stat） → 规则引擎 Strategy 模式 → 批量任务双路径（JDBC直查 + Tagger API分页） → Roaring Bitmap 序列化至 Redis → TAGGER 节点新增 audience 模式 → React QueryBuilder + Ant Design 前端。

**Tech Stack:** Java 21, AviatorScript, QLExpress, RoaringBitmap, Spring WebFlux, MyBatis-Plus, React 18, react-querybuilder, Ant Design

---

## File Map

**后端新增：**
- `V39__audience_definition.sql`
- `domain/audience/AudienceDefinition.java`
- `domain/audience/AudienceDefinitionMapper.java`
- `domain/audience/AudienceStat.java`
- `domain/audience/AudienceStatMapper.java`
- `engine/audience/RuleEvaluator.java`（接口）
- `engine/audience/AviatorRuleEvaluator.java`
- `engine/audience/QLExpressRuleEvaluator.java`
- `engine/audience/RuleEvaluatorRouter.java`
- `engine/audience/SqlWhereGenerator.java`
- `engine/audience/AudienceBitmapStore.java`
- `engine/audience/AudienceBatchComputeJob.java`
- `controller/AudienceController.java`

**后端修改：**
- `engine/handlers/TaggerHandler.java`（新增 audience 模式）
- `pom.xml`（AviatorScript + QLExpress + RoaringBitmap 依赖）
- `resources/db/migration/V39__audience_definition.sql`

**前端新增：**
- `frontend/src/pages/audience-list/index.tsx`
- `frontend/src/pages/audience-edit/index.tsx`
- `frontend/src/services/audienceApi.ts`

**前端修改：**
- `frontend/src/App.tsx`（路由）
- `frontend/src/components/config-panel/`（TAGGER 节点新增 audience 模式）

---

## Task 1：数据库 + 实体 + Mapper

- [ ] **Step 1：创建 V39 migration**

```sql
-- V39: 人群圈选功能
CREATE TABLE audience_definition (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    name                VARCHAR(100) NOT NULL,
    description         VARCHAR(500),
    rule_json           TEXT NOT NULL,
    engine_type         VARCHAR(20)  NOT NULL DEFAULT 'AVIATOR',
    data_source_type    VARCHAR(20)  NOT NULL DEFAULT 'TAGGER_API',
    data_source_config  TEXT,
    evaluation_strategy VARCHAR(20)  NOT NULL DEFAULT 'OFFLINE_BATCH',
    cron_expression     VARCHAR(100),
    enabled             TINYINT      NOT NULL DEFAULT 1,
    created_by          VARCHAR(100),
    created_at          DATETIME,
    updated_at          DATETIME
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE audience_stat (
    audience_id     BIGINT PRIMARY KEY,
    estimated_size  BIGINT,
    bitmap_size_kb  INT,
    computed_at     DATETIME,
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING'
                    COMMENT 'PENDING | COMPUTING | READY | FAILED',
    error_msg       VARCHAR(500)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

- [ ] **Step 2：创建 AudienceDefinition 实体**

```java
package org.chovy.canvas.domain.audience;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("audience_definition")
public class AudienceDefinition {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String description;
    private String ruleJson;
    private String engineType;        // AVIATOR | QL
    private String dataSourceType;    // TAGGER_API | JDBC
    private String dataSourceConfig;  // JSON
    private String evaluationStrategy; // ONLINE | OFFLINE_BATCH | HYBRID
    private String cronExpression;
    private Integer enabled;
    private String createdBy;
    @TableField(fill = FieldFill.INSERT)   private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE) private LocalDateTime updatedAt;
}
```

- [ ] **Step 3：创建 AudienceStat 实体**

```java
package org.chovy.canvas.domain.audience;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("audience_stat")
public class AudienceStat {
    @TableId
    private Long audienceId;
    private Long estimatedSize;
    private Integer bitmapSizeKb;
    private LocalDateTime computedAt;
    private String status;   // PENDING | COMPUTING | READY | FAILED
    private String errorMsg;
}
```

- [ ] **Step 4：创建 Mapper 接口（MyBatis-Plus 默认方法即可）**

```java
@Mapper public interface AudienceDefinitionMapper extends BaseMapper<AudienceDefinition> {}
@Mapper public interface AudienceStatMapper extends BaseMapper<AudienceStat> {}
```

- [ ] **Step 5：编译验证**

```bash
cd backend && mvn compile -pl canvas-engine -q
```

- [ ] **Step 6：Commit**

```bash
git add backend/canvas-engine/src/main/resources/db/migration/V39__audience_definition.sql \
        backend/canvas-engine/src/main/java/org/chovy/canvas/domain/audience/
git commit -m "feat: add audience_definition and audience_stat tables and entities"
```

---

## Task 2：依赖引入 + Bitmap 存储工具

- [ ] **Step 1：pom.xml 添加三个依赖**

```xml
<!-- AviatorScript 规则引擎 -->
<dependency>
    <groupId>com.googlecode.aviator</groupId>
    <artifactId>aviator</artifactId>
    <version>5.4.3</version>
</dependency>

<!-- QLExpress 规则引擎 -->
<dependency>
    <groupId>com.alibaba</groupId>
    <artifactId>QLExpress</artifactId>
    <version>3.3.3</version>
</dependency>

<!-- Roaring Bitmap -->
<dependency>
    <groupId>org.roaringbitmap</groupId>
    <artifactId>RoaringBitmap</artifactId>
    <version>1.0.6</version>
</dependency>
```

- [ ] **Step 2：创建 AudienceBitmapStore**

```java
package org.chovy.canvas.engine.audience;

import com.google.common.hash.Hashing;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.roaringbitmap.RoaringBitmap;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@RequiredArgsConstructor
public class AudienceBitmapStore {

    private final StringRedisTemplate redis;
    private static final String KEY_PREFIX = "audience:bitmap:";

    /** userId 字符串 → 稳定的非负整数（MurmurHash3） */
    public static int toUid(String userId) {
        int h = Hashing.murmur3_32_fixed()
                .hashString(userId, StandardCharsets.UTF_8).asInt();
        return h == Integer.MIN_VALUE ? 0 : Math.abs(h);
    }

    /** 将 Bitmap 序列化后存入 Redis */
    public void save(Long audienceId, RoaringBitmap bitmap) throws IOException {
        bitmap.runOptimize(); // 压缩优化
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(bos);
        bitmap.serialize(dos);
        dos.flush();
        redis.opsForValue().set(KEY_PREFIX + audienceId,
                java.util.Base64.getEncoder().encodeToString(bos.toByteArray()));
        log.info("[AUDIENCE] bitmap saved audienceId={} sizeKB={}", audienceId, bos.size() / 1024);
    }

    /** 从 Redis 反序列化 Bitmap；不存在返回 empty bitmap */
    public RoaringBitmap load(Long audienceId) throws IOException {
        String encoded = redis.opsForValue().get(KEY_PREFIX + audienceId);
        if (encoded == null) return new RoaringBitmap();
        byte[] bytes = java.util.Base64.getDecoder().decode(encoded);
        RoaringBitmap bitmap = new RoaringBitmap();
        bitmap.deserialize(new DataInputStream(new ByteArrayInputStream(bytes)));
        return bitmap;
    }

    /** O(1) 成员查询 */
    public boolean isMember(Long audienceId, String userId) {
        try {
            RoaringBitmap bitmap = load(audienceId);
            return bitmap.contains(toUid(userId));
        } catch (IOException e) {
            log.error("[AUDIENCE] bitmap load failed audienceId={}: {}", audienceId, e.getMessage());
            return false; // fail-open：查不到不拦截用户
        }
    }

    public void delete(Long audienceId) {
        redis.delete(KEY_PREFIX + audienceId);
    }
}
```

- [ ] **Step 3：编译验证**

```bash
cd backend && mvn compile -pl canvas-engine -q
```

- [ ] **Step 4：Commit**

```bash
git add backend/canvas-engine/pom.xml \
        backend/canvas-engine/src/main/java/org/chovy/canvas/engine/audience/AudienceBitmapStore.java
git commit -m "feat: add RoaringBitmap + AviatorScript + QLExpress deps and AudienceBitmapStore"
```

---

## Task 3：规则引擎双实现

- [ ] **Step 1：定义 RuleEvaluator 接口**

```java
package org.chovy.canvas.engine.audience;

import java.util.Map;

public interface RuleEvaluator {
    /**
     * 评估规则是否匹配给定上下文。
     * @param ruleJson 规则 JSON（包含 logic + conditions + groups）
     * @param context  用户标签值 Map，如 {"city":"Beijing","vip_level":2}
     * @return true=命中人群
     */
    boolean evaluate(String ruleJson, Map<String, Object> context);
}
```

- [ ] **Step 2：AviatorRuleEvaluator**

```java
package org.chovy.canvas.engine.audience;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.googlecode.aviator.AviatorEvaluator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

@Slf4j
@Component("AVIATOR")
@RequiredArgsConstructor
public class AviatorRuleEvaluator implements RuleEvaluator {

    private final ObjectMapper objectMapper;

    @Override
    public boolean evaluate(String ruleJson, Map<String, Object> context) {
        try {
            Map<?, ?> rule = objectMapper.readValue(ruleJson, Map.class);
            String expr = toExpression(rule);
            log.debug("[AVIATOR] expr={}", expr);
            Object result = AviatorEvaluator.execute(expr, new HashMap<>(context));
            return Boolean.TRUE.equals(result);
        } catch (Exception e) {
            log.error("[AVIATOR] evaluate failed: {}", e.getMessage());
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    private String toExpression(Map<?, ?> rule) {
        String logic = (String) rule.getOrDefault("logic", "AND");
        String joinOp = "AND".equals(logic) ? " && " : " || ";

        List<String> parts = new ArrayList<>();

        // 叶节点 conditions
        List<Map<?, ?>> conditions = (List<Map<?, ?>>) rule.get("conditions");
        if (conditions != null) {
            for (Map<?, ?> c : conditions) {
                parts.add(toConditionExpr(c));
            }
        }

        // 嵌套 groups
        List<Map<?, ?>> groups = (List<Map<?, ?>>) rule.get("groups");
        if (groups != null) {
            for (Map<?, ?> g : groups) {
                parts.add("(" + toExpression(g) + ")");
            }
        }

        return parts.isEmpty() ? "true" : String.join(joinOp, parts);
    }

    @SuppressWarnings("unchecked")
    private String toConditionExpr(Map<?, ?> c) {
        String field = (String) c.get("field");
        String op    = (String) c.get("op");
        Object value = c.get("value");

        return switch (op) {
            case "="  -> field + " == " + quoteIfString(value);
            case "!=" -> field + " != " + quoteIfString(value);
            case ">"  -> field + " > "  + value;
            case ">=" -> field + " >= " + value;
            case "<"  -> field + " < "  + value;
            case "<=" -> field + " <= " + value;
            case "IN" -> {
                // 使用 Aviator 内置 include 函数
                List<?> list = (List<?>) value;
                yield "include(" + field + "_list, " + field + ")";
                // 注意：调用方需在 context 中注入 "{field}_list" 变量
            }
            default -> "true";
        };
    }

    private String quoteIfString(Object v) {
        return v instanceof String ? "\"" + v + "\"" : String.valueOf(v);
    }
}
```

- [ ] **Step 3：QLExpressRuleEvaluator**

```java
package org.chovy.canvas.engine.audience;

import com.alibaba.QLExpress.ExpressRunner;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

@Slf4j
@Component("QL")
@RequiredArgsConstructor
public class QLExpressRuleEvaluator implements RuleEvaluator {

    private final ObjectMapper objectMapper;
    private final ExpressRunner runner = new ExpressRunner();

    @Override
    public boolean evaluate(String ruleJson, Map<String, Object> context) {
        try {
            Map<?, ?> rule = objectMapper.readValue(ruleJson, Map.class);
            String script = toScript(rule);
            log.debug("[QL] script={}", script);
            com.alibaba.QLExpress.DefaultContext<String, Object> ctx =
                    new com.alibaba.QLExpress.DefaultContext<>();
            ctx.putAll(context);
            Object result = runner.execute(script, ctx, null, true, false);
            return Boolean.TRUE.equals(result);
        } catch (Exception e) {
            log.error("[QL] evaluate failed: {}", e.getMessage());
            return false;
        }
    }

    // 与 AviatorRuleEvaluator 相同的 JSON→脚本转换逻辑，QL 语法略有差异
    @SuppressWarnings("unchecked")
    private String toScript(Map<?, ?> rule) {
        String logic = (String) rule.getOrDefault("logic", "AND");
        String joinOp = "AND".equals(logic) ? " && " : " || ";
        List<String> parts = new ArrayList<>();
        List<Map<?, ?>> conditions = (List<Map<?, ?>>) rule.get("conditions");
        if (conditions != null) {
            for (Map<?, ?> c : conditions) {
                parts.add(toConditionScript(c));
            }
        }
        List<Map<?, ?>> groups = (List<Map<?, ?>>) rule.get("groups");
        if (groups != null) {
            for (Map<?, ?> g : groups) {
                parts.add("(" + toScript(g) + ")");
            }
        }
        return parts.isEmpty() ? "true" : String.join(joinOp, parts);
    }

    @SuppressWarnings("unchecked")
    private String toConditionScript(Map<?, ?> c) {
        String field = (String) c.get("field");
        String op    = (String) c.get("op");
        Object value = c.get("value");
        return switch (op) {
            case "="  -> field + " == " + quoteIfString(value);
            case "!=" -> field + " != " + quoteIfString(value);
            case ">"  -> field + " > "  + value;
            case ">=" -> field + " >= " + value;
            case "<"  -> field + " < "  + value;
            case "<=" -> field + " <= " + value;
            case "IN" -> {
                List<?> list = (List<?>) value;
                // QLExpress 内联列表成员检查
                String listStr = list.stream()
                        .map(v -> v instanceof String ? "\"" + v + "\"" : v.toString())
                        .collect(java.util.stream.Collectors.joining(","));
                yield "containsInList([" + listStr + "], " + field + ")";
            }
            default -> "true";
        };
    }

    private String quoteIfString(Object v) {
        return v instanceof String ? "\"" + v + "\"" : String.valueOf(v);
    }
}
```

- [ ] **Step 4：RuleEvaluatorRouter**

```java
package org.chovy.canvas.engine.audience;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class RuleEvaluatorRouter {

    private final Map<String, RuleEvaluator> evaluators; // Spring 按 bean name 注入

    public boolean evaluate(String engineType, String ruleJson, Map<String, Object> context) {
        RuleEvaluator evaluator = evaluators.getOrDefault(engineType,
                evaluators.get("AVIATOR")); // 默认 Aviator
        return evaluator.evaluate(ruleJson, context);
    }
}
```

- [ ] **Step 5：编译 + 测试**

```bash
cd backend && mvn compile -pl canvas-engine -q
# 手动验证：AviatorEvaluator.execute("age >= 18 && city == \"Beijing\"", Map.of("age",20,"city","Beijing")) → true
```

- [ ] **Step 6：Commit**

```bash
git add backend/canvas-engine/src/main/java/org/chovy/canvas/engine/audience/
git commit -m "feat: add RuleEvaluator interface with Aviator and QL implementations"
```

---

## Task 4：批量计算任务（AudienceBatchComputeJob）

- [ ] **Step 1：创建批量计算服务**

```java
package org.chovy.canvas.engine.audience;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.chovy.canvas.domain.audience.AudienceDefinition;
import org.chovy.canvas.domain.audience.AudienceDefinitionMapper;
import org.chovy.canvas.domain.audience.AudienceStat;
import org.chovy.canvas.domain.audience.AudienceStatMapper;
import org.roaringbitmap.RoaringBitmap;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AudienceBatchComputeService {

    private final AudienceDefinitionMapper definitionMapper;
    private final AudienceStatMapper       statMapper;
    private final RuleEvaluatorRouter      ruleRouter;
    private final AudienceBitmapStore      bitmapStore;
    private final StringRedisTemplate      redis;
    private final ObjectMapper             objectMapper;

    @org.springframework.beans.factory.annotation.Value("${canvas.integration.tagger-service-url}")
    private String taggerUrl;

    private static final String COMPUTE_LOCK_PREFIX = "audience:compute:lock:";
    private static final int    PAGE_SIZE = 1000;

    /**
     * 计算指定人群，更新 Redis Bitmap 和 audience_stat。
     * 幂等：同一人群不允许并发计算（Redis SETNX 锁）。
     */
    public void compute(Long audienceId) {
        String lockKey = COMPUTE_LOCK_PREFIX + audienceId;
        boolean locked = Boolean.TRUE.equals(
                redis.opsForValue().setIfAbsent(lockKey, "1", Duration.ofHours(2)));
        if (!locked) {
            log.warn("[AUDIENCE] 人群 {} 正在计算中，跳过重复触发", audienceId);
            return;
        }

        updateStat(audienceId, "COMPUTING", null, null);

        try {
            AudienceDefinition def = definitionMapper.selectById(audienceId);
            if (def == null || def.getEnabled() == 0)
                throw new IllegalArgumentException("人群不存在或已禁用: " + audienceId);

            RoaringBitmap bitmap = switch (def.getDataSourceType()) {
                case "JDBC"        -> computeViaJdbc(def);
                case "TAGGER_API"  -> computeViaTaggerApi(def);
                default            -> throw new IllegalStateException("未知数据源类型: " + def.getDataSourceType());
            };

            bitmapStore.save(audienceId, bitmap);

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            bitmap.serialize(new DataOutputStream(bos));
            updateStat(audienceId, "READY", (long) bitmap.getCardinality(), bos.size() / 1024);
            log.info("[AUDIENCE] 计算完成 audienceId={} size={}", audienceId, bitmap.getCardinality());

        } catch (Exception e) {
            log.error("[AUDIENCE] 计算失败 audienceId={}: {}", audienceId, e.getMessage());
            updateStat(audienceId, "FAILED", null, null);
            // 更新错误信息
            AudienceStat stat = statMapper.selectById(audienceId);
            if (stat != null) {
                stat.setErrorMsg(e.getMessage().substring(0, Math.min(500, e.getMessage().length())));
                statMapper.updateById(stat);
            }
        } finally {
            redis.delete(lockKey);
        }
    }

    /** TAGGER_API 路径：分页拉取用户列表 + 批量查标签值 + 规则评估 */
    @SuppressWarnings("unchecked")
    private RoaringBitmap computeViaTaggerApi(AudienceDefinition def) throws Exception {
        Map<?, ?> config = objectMapper.readValue(
                def.getDataSourceConfig() != null ? def.getDataSourceConfig() : "{}", Map.class);
        String seedTagCode = (String) config.getOrDefault("seedTagCode", "");

        WebClient client = WebClient.builder().baseUrl(taggerUrl).build();
        RoaringBitmap bitmap = new RoaringBitmap();
        int page = 1;

        while (true) {
            final int p = page;
            List<String> userIds = client.get()
                    .uri(u -> u.path("/offline/users")
                            .queryParam("tagCode", seedTagCode)
                            .queryParam("page", p)
                            .queryParam("size", PAGE_SIZE)
                            .build())
                    .retrieve()
                    .bodyToMono(Map.class)
                    .map(resp -> (List<String>) resp.getOrDefault("userIds", List.of()))
                    .block();

            if (userIds == null || userIds.isEmpty()) break;

            for (String userId : userIds) {
                // 简化：对每个用户调标签 API 获取上下文
                // 生产中应批量获取以减少 HTTP 调用次数
                Map<String, Object> context = fetchUserContext(client, userId, def.getRuleJson());
                if (ruleRouter.evaluate(def.getEngineType(), def.getRuleJson(), context)) {
                    bitmap.add(AudienceBitmapStore.toUid(userId));
                }
            }
            page++;
        }
        return bitmap;
    }

    /** JDBC 路径：JSON 规则 → SQL WHERE → 直接查询（由 SqlWhereGenerator 生成） */
    private RoaringBitmap computeViaJdbc(AudienceDefinition def) throws Exception {
        // TODO: 接入配置的 DataSource，通过 SqlWhereGenerator 生成 WHERE 子句直查
        // 当前版本作为后续迭代，先抛出提示
        throw new UnsupportedOperationException("JDBC 数据源计算暂未实现，请使用 TAGGER_API");
    }

    private Map<String, Object> fetchUserContext(WebClient client, String userId, String ruleJson) {
        // 从规则中提取所有 tagCode，批量查询用户标签值
        // 简化实现，生产中应批量优化
        return Map.of(); // placeholder
    }

    private void updateStat(Long audienceId, String status, Long size, Integer sizeKb) {
        AudienceStat stat = statMapper.selectById(audienceId);
        if (stat == null) { stat = new AudienceStat(); stat.setAudienceId(audienceId); }
        stat.setStatus(status);
        stat.setComputedAt(LocalDateTime.now());
        if (size != null)   stat.setEstimatedSize(size);
        if (sizeKb != null) stat.setBitmapSizeKb(sizeKb);
        statMapper.insertOrUpdate(stat);
    }
}
```

- [ ] **Step 2：创建 AudienceController**

```java
package org.chovy.canvas.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.chovy.canvas.common.PageResult;
import org.chovy.canvas.common.R;
import org.chovy.canvas.domain.audience.*;
import org.chovy.canvas.engine.audience.AudienceBatchComputeService;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@RestController
@RequestMapping("/canvas/audiences")
@RequiredArgsConstructor
public class AudienceController {

    private final AudienceDefinitionMapper definitionMapper;
    private final AudienceStatMapper       statMapper;
    private final AudienceBatchComputeService computeService;

    @GetMapping
    public Mono<R<PageResult<AudienceDefinition>>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return Mono.fromCallable(() -> {
            Page<AudienceDefinition> p = definitionMapper.selectPage(
                    new Page<>(page, size),
                    new LambdaQueryWrapper<AudienceDefinition>().orderByDesc(AudienceDefinition::getId));
            return R.ok(PageResult.of(p.getTotal(), p.getRecords()));
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @GetMapping("/ready")
    public Mono<R<java.util.List<AudienceDefinition>>> listReady() {
        return Mono.fromCallable(() -> {
            var list = definitionMapper.selectList(
                    new LambdaQueryWrapper<AudienceDefinition>()
                            .eq(AudienceDefinition::getEnabled, 1));
            return R.ok(list);
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @PostMapping
    public Mono<R<AudienceDefinition>> create(@RequestBody AudienceDefinition body) {
        return Mono.fromCallable(() -> { definitionMapper.insert(body); return R.ok(body); })
                .subscribeOn(Schedulers.boundedElastic());
    }

    @PutMapping("/{id}")
    public Mono<R<Void>> update(@PathVariable Long id, @RequestBody AudienceDefinition body) {
        body.setId(id);
        return Mono.fromCallable(() -> { definitionMapper.updateById(body); return R.<Void>ok(); })
                .subscribeOn(Schedulers.boundedElastic());
    }

    @DeleteMapping("/{id}")
    public Mono<R<Void>> delete(@PathVariable Long id) {
        return Mono.fromRunnable(() -> definitionMapper.deleteById(id))
                .subscribeOn(Schedulers.boundedElastic())
                .then(Mono.just(R.<Void>ok()));
    }

    @PostMapping("/{id}/compute")
    public Mono<R<Void>> compute(@PathVariable Long id) {
        return Mono.fromRunnable(() ->
                Thread.ofVirtual().start(() -> computeService.compute(id)))
                .subscribeOn(Schedulers.boundedElastic())
                .then(Mono.just(R.<Void>ok()));
    }

    @GetMapping("/{id}/stat")
    public Mono<R<AudienceStat>> stat(@PathVariable Long id) {
        return Mono.fromCallable(() -> R.ok(statMapper.selectById(id)))
                .subscribeOn(Schedulers.boundedElastic());
    }
}
```

- [ ] **Step 3：编译 + Commit**

```bash
cd backend && mvn compile -pl canvas-engine -q
git add backend/canvas-engine/src/main/java/org/chovy/canvas/engine/audience/ \
        backend/canvas-engine/src/main/java/org/chovy/canvas/controller/AudienceController.java
git commit -m "feat: add AudienceBatchComputeService and AudienceController"
```

---

## Task 5：TAGGER 节点集成

- [ ] **Step 1：修改 TaggerHandler.executeAsync()**

在 `TaggerHandler` 中，在现有 `"realtime"` 分支之前新增 `"audience"` 分支：

```java
@Override
public Mono<NodeResult> executeAsync(Map<String, Object> config, ExecutionContext ctx) {
    String mode = (String) config.getOrDefault("mode", "offline");

    if ("audience".equals(mode)) {
        return handleAudienceMode(config, ctx);
    }
    if ("realtime".equals(mode)) {
        return realtimeHandler.executeAsync(config, ctx);
    }
    return offlineHandler.executeAsync(config, ctx);
}

private Mono<NodeResult> handleAudienceMode(Map<String, Object> config, ExecutionContext ctx) {
    Object audienceIdObj = config.get("audienceId");
    if (audienceIdObj == null) return Mono.just(NodeResult.fail("TAGGER[audience]: audienceId 未配置"));

    Long audienceId = Long.parseLong(audienceIdObj.toString());
    String hitNextNodeId  = (String) config.get("hitNextNodeId");
    String missNextNodeId = (String) config.get("missNextNodeId");

    boolean hit = bitmapStore.isMember(audienceId, ctx.getUserId());
    String nextNodeId = hit ? hitNextNodeId : missNextNodeId;

    return Mono.just(NodeResult.ok(nextNodeId, Map.of("audienceHit", hit, "audienceId", audienceId)));
}
```

注入 `AudienceBitmapStore bitmapStore`（构造器注入）。

- [ ] **Step 2：编译 + 全量测试**

```bash
cd backend && mvn test -pl canvas-engine -q
```

Expected: `BUILD SUCCESS`

- [ ] **Step 3：Commit**

```bash
git add backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handlers/TaggerHandler.java
git commit -m "feat: add audience mode to TaggerHandler with Roaring Bitmap membership check"
```

---

## Task 6：前端——依赖 + API + 路由

- [ ] **Step 1：安装 react-querybuilder**

```bash
cd frontend && npm install react-querybuilder @react-querybuilder/antd
```

- [ ] **Step 2：创建 audienceApi.ts**

```typescript
// frontend/src/services/audienceApi.ts
import { http } from './api'

export interface AudienceDefinition {
  id?: number
  name: string
  description?: string
  ruleJson: string
  engineType: 'AVIATOR' | 'QL'
  dataSourceType: 'TAGGER_API' | 'JDBC'
  dataSourceConfig?: string
  evaluationStrategy: 'ONLINE' | 'OFFLINE_BATCH' | 'HYBRID'
  cronExpression?: string
  enabled: number
}

export interface AudienceStat {
  audienceId: number
  estimatedSize: number
  status: 'PENDING' | 'COMPUTING' | 'READY' | 'FAILED'
  computedAt: string
  errorMsg?: string
}

export const audienceApi = {
  list: (page = 1, size = 20) =>
    http.get<any>('/canvas/audiences', { params: { page, size } }),
  listReady: () =>
    http.get<any>('/canvas/audiences/ready'),
  create: (body: AudienceDefinition) =>
    http.post<any>('/canvas/audiences', body),
  update: (id: number, body: AudienceDefinition) =>
    http.put<any>(`/canvas/audiences/${id}`, body),
  delete: (id: number) =>
    http.delete<any>(`/canvas/audiences/${id}`),
  compute: (id: number) =>
    http.post<any>(`/canvas/audiences/${id}/compute`),
  stat: (id: number) =>
    http.get<any>(`/canvas/audiences/${id}/stat`),
}
```

- [ ] **Step 3：添加路由（App.tsx）**

在现有路由中添加：

```tsx
import AudienceListPage from './pages/audience-list'
import AudienceEditPage from './pages/audience-edit'

// 在 <Routes> 内添加：
<Route path="/audiences" element={<AudienceListPage />} />
<Route path="/audiences/new" element={<AudienceEditPage />} />
<Route path="/audiences/:id/edit" element={<AudienceEditPage />} />
```

- [ ] **Step 4：Commit**

```bash
git add frontend/src/services/audienceApi.ts frontend/src/App.tsx
git commit -m "feat: add audienceApi and audience routes"
```

---

## Task 7：前端——人群列表页

- [ ] **Step 1：创建 audience-list/index.tsx**

```tsx
// frontend/src/pages/audience-list/index.tsx
import { useEffect, useState } from 'react'
import { Table, Button, Tag, Space, Popconfirm, message, Typography, Tooltip } from 'antd'
import { PlusOutlined, ThunderboltOutlined } from '@ant-design/icons'
import { useNavigate } from 'react-router-dom'
import { audienceApi, AudienceStat } from '../../services/audienceApi'
import type { ColumnsType } from 'antd/es/table'

const { Title } = Typography

const STATUS_MAP: Record<string, { label: string; color: string }> = {
  PENDING:   { label: '待计算', color: 'default' },
  COMPUTING: { label: '计算中', color: 'processing' },
  READY:     { label: '就绪',   color: 'success' },
  FAILED:    { label: '失败',   color: 'error' },
}

export default function AudienceListPage() {
  const navigate = useNavigate()
  const [data, setData]       = useState<any[]>([])
  const [stats, setStats]     = useState<Record<number, AudienceStat>>({})
  const [loading, setLoading] = useState(false)

  const fetchList = async () => {
    setLoading(true)
    try {
      const res = await audienceApi.list()
      const list = res.data.list
      setData(list)
      // 并行拉取所有人群的计算状态
      const statResults = await Promise.allSettled(list.map((a: any) => audienceApi.stat(a.id)))
      const statMap: Record<number, AudienceStat> = {}
      statResults.forEach((r, i) => {
        if (r.status === 'fulfilled') statMap[list[i].id] = r.value.data
      })
      setStats(statMap)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { fetchList() }, [])

  const handleCompute = async (id: number) => {
    await audienceApi.compute(id)
    message.success('已触发计算，请稍后刷新查看结果')
  }

  const handleDelete = async (id: number) => {
    await audienceApi.delete(id)
    message.success('已删除')
    fetchList()
  }

  const columns: ColumnsType<any> = [
    { title: '名称', dataIndex: 'name' },
    { title: '计算策略', dataIndex: 'evaluationStrategy', width: 120 },
    {
      title: '状态',
      width: 100,
      render: (_, record) => {
        const stat = stats[record.id]
        if (!stat) return <Tag>-</Tag>
        const { label, color } = STATUS_MAP[stat.status] ?? { label: '未知', color: 'default' }
        return <Tag color={color}>{label}</Tag>
      },
    },
    {
      title: '人群规模',
      width: 120,
      render: (_, record) => {
        const stat = stats[record.id]
        return stat?.estimatedSize != null
          ? stat.estimatedSize.toLocaleString()
          : '-'
      },
    },
    {
      title: '最后计算',
      width: 180,
      render: (_, record) => stats[record.id]?.computedAt?.replace('T', ' ').slice(0, 19) ?? '-',
    },
    {
      title: '操作',
      width: 200,
      render: (_, record) => (
        <Space>
          <Button size="small" onClick={() => navigate(`/audiences/${record.id}/edit`)}>编辑</Button>
          <Tooltip title="立即触发重新计算">
            <Button size="small" icon={<ThunderboltOutlined />}
              onClick={() => handleCompute(record.id)}>计算</Button>
          </Tooltip>
          <Popconfirm title="确认删除？" onConfirm={() => handleDelete(record.id)}>
            <Button size="small" danger>删除</Button>
          </Popconfirm>
        </Space>
      ),
    },
  ]

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 16 }}>
        <Title level={4} style={{ margin: 0 }}>人群管理</Title>
        <Button type="primary" icon={<PlusOutlined />} onClick={() => navigate('/audiences/new')}>
          新建人群
        </Button>
      </div>
      <Table rowKey="id" dataSource={data} columns={columns} loading={loading} />
    </div>
  )
}
```

- [ ] **Step 2：Commit**

```bash
git add frontend/src/pages/audience-list/
git commit -m "feat: add audience list page with status and compute trigger"
```

---

## Task 8：前端——人群编辑页（规则构建器）

- [ ] **Step 1：创建 audience-edit/index.tsx**

```tsx
// frontend/src/pages/audience-edit/index.tsx
import { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import {
  Form, Input, Select, Button, Switch, message, Typography, Divider, Space, Card,
} from 'antd'
import QueryBuilder, { RuleGroupType } from 'react-querybuilder'
import { QueryBuilderAntD } from '@react-querybuilder/antd'
import { audienceApi } from '../../services/audienceApi'
import { canvasApi } from '../../services/api'
import 'react-querybuilder/dist/query-builder.css'

const { Title } = Typography
const { Option } = Select

const OPERATORS = [
  { name: '=',  label: '等于' },
  { name: '!=', label: '不等于' },
  { name: '>',  label: '大于' },
  { name: '>=', label: '大于等于' },
  { name: '<',  label: '小于' },
  { name: '<=', label: '小于等于' },
  { name: 'IN', label: '包含于' },
]

export default function AudienceEditPage() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const [form] = Form.useForm()
  const [query, setQuery] = useState<RuleGroupType>({ combinator: 'and', rules: [] })
  const [tagFields, setTagFields] = useState<any[]>([])
  const [loading, setLoading] = useState(false)
  const isEdit = !!id

  // 加载标签定义作为规则字段
  useEffect(() => {
    fetch('/canvas/tag-definitions?size=100')
      .then(r => r.json())
      .then(res => {
        const fields = res.data.list.map((t: any) => ({
          name:  t.tagCode,
          label: t.name,
        }))
        setTagFields(fields)
      })
  }, [])

  // 编辑时加载现有数据
  useEffect(() => {
    if (!isEdit) return
    audienceApi.list().then(res => {
      const audience = res.data.list.find((a: any) => String(a.id) === id)
      if (audience) {
        form.setFieldsValue(audience)
        if (audience.ruleJson) {
          try { setQuery(JSON.parse(audience.ruleJson)) } catch {}
        }
      }
    })
  }, [id])

  const handleSave = async () => {
    const values = await form.validateFields()
    setLoading(true)
    try {
      const body = { ...values, ruleJson: JSON.stringify(query) }
      if (isEdit) {
        await audienceApi.update(Number(id), body)
        message.success('保存成功，将自动触发重新计算')
        audienceApi.compute(Number(id)) // 保存后自动触发计算
      } else {
        const res = await audienceApi.create(body)
        message.success('创建成功')
        navigate(`/audiences/${res.data.id}/edit`)
      }
    } finally {
      setLoading(false)
    }
  }

  return (
    <div style={{ maxWidth: 900, margin: '0 auto', padding: 24 }}>
      <Title level={4}>{isEdit ? '编辑人群' : '新建人群'}</Title>

      <Form form={form} layout="vertical"
        initialValues={{ engineType: 'AVIATOR', dataSourceType: 'TAGGER_API',
                         evaluationStrategy: 'OFFLINE_BATCH', enabled: 1 }}>

        <Card title="基本信息" style={{ marginBottom: 16 }}>
          <Form.Item name="name" label="人群名称" rules={[{ required: true }]}>
            <Input placeholder="例：近30天消费-一线城市VIP用户" />
          </Form.Item>
          <Form.Item name="description" label="描述">
            <Input.TextArea rows={2} />
          </Form.Item>
        </Card>

        <Card title="数据源配置" style={{ marginBottom: 16 }}>
          <Form.Item name="dataSourceType" label="数据源类型">
            <Select>
              <Option value="TAGGER_API">Tagger 服务（离线标签）</Option>
              <Option value="JDBC">JDBC 数据源</Option>
            </Select>
          </Form.Item>
        </Card>

        <Card title="圈选规则" style={{ marginBottom: 16 }}>
          <QueryBuilderAntD>
            <QueryBuilder
              fields={tagFields}
              operators={OPERATORS}
              query={query}
              onQueryChange={setQuery}
              combinators={[
                { name: 'and', label: '且（AND）' },
                { name: 'or',  label: '或（OR）'  },
              ]}
            />
          </QueryBuilderAntD>
        </Card>

        <Card title="计算配置">
          <Form.Item name="evaluationStrategy" label="计算策略">
            <Select>
              <Option value="OFFLINE_BATCH">离线批量（推荐）</Option>
              <Option value="ONLINE">实时计算</Option>
              <Option value="HYBRID">混合</Option>
            </Select>
          </Form.Item>
          <Form.Item name="engineType" label="规则引擎">
            <Select>
              <Option value="AVIATOR">AviatorScript（快速，适合简单条件）</Option>
              <Option value="QL">QLExpress（复杂业务规则）</Option>
            </Select>
          </Form.Item>
          <Form.Item name="cronExpression" label="定时计算（留空=仅手动触发）">
            <Input placeholder="例：0 2 * * * （每日凌晨2点）" />
          </Form.Item>
        </Card>
      </Form>

      <div style={{ marginTop: 16, textAlign: 'right' }}>
        <Space>
          <Button onClick={() => navigate('/audiences')}>取消</Button>
          <Button type="primary" loading={loading} onClick={handleSave}>
            {isEdit ? '保存并重新计算' : '创建'}
          </Button>
        </Space>
      </div>
    </div>
  )
}
```

- [ ] **Step 2：验证 TypeScript**

```bash
cd frontend && npx tsc --noEmit
```

Expected: 无报错

- [ ] **Step 3：手动测试**

1. 进入 `/audiences` → 空列表，"新建人群"按钮可见
2. 点击新建 → 填写名称 → 规则构建器选择标签字段 + 条件 → 保存
3. 列表页显示新人群，状态 PENDING
4. 点击"计算" → 状态变为 COMPUTING → 轮询后变为 READY + 规模数字
5. 编辑画布，添加 TAGGER 节点，选择 `audience` 模式，选择刚创建的人群

- [ ] **Step 4：Commit**

```bash
git add frontend/src/pages/audience-edit/ frontend/src/pages/audience-list/
git commit -m "feat: add audience edit page with react-querybuilder visual rule builder"
```
