# RoaringBitmap Collision Fix Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development or superpowers:executing-plans.

**Goal:** Replace murmur3_32 hash with deterministic userId→integer mapping. Use Redis GETBIT for single user check, BITOP for set operations. Store bitmaps as raw bytes (no Base64).

**Architecture:** Redis INCR-based userId registry with Lua script for atomic get-or-assign. AudienceBitmapStore rewritten to use raw byte bitmaps with GETBIT/BITOP. Migration job converts existing Base64 bitmaps.

**Tech Stack:** Redis, Lua scripts, Java 21, Spring Boot

---

### Task 1: Implement Deterministic UserId Registry

**Files:**
- Create: `backend/canvas-engine/src/main/resources/scripts/userid_get_or_assign.lua`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/audience/UserIdRegistry.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/audience/UserIdRegistryTest.java`

- [ ] **Step 1: Write failing test**

```java
package org.chovy.canvas.engine.audience;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class UserIdRegistryTest {

    @Autowired
    private UserIdRegistry registry;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Test
    void getOrAssignId_sameUserId_returnsSameInteger() {
        int id1 = registry.getOrAssignId("user-123");
        int id2 = registry.getOrAssignId("user-123");
        assertThat(id1).isEqualTo(id2);
        assertThat(id1).isGreaterThan(0);
    }

    @Test
    void getOrAssignId_differentUserIds_returnsDifferentIntegers() {
        Set<Integer> ids = new HashSet<>();
        for (int i = 0; i < 1000; i++) {
            int id = registry.getOrAssignId("user-" + i);
            assertThat(ids).doesNotContain(id);
            ids.add(id);
        }
    }

    @Test
    void getOrAssignId_concurrentAccess_noCollision() throws Exception {
        int threadCount = 10;
        int usersPerThread = 100;
        Set<Integer> allIds = Collections.synchronizedSet(new HashSet<>());
        var executor = Executors.newFixedThreadPool(threadCount);

        var futures = new ArrayList<Future<Void>>();
        for (int t = 0; t < threadCount; t++) {
            int offset = t * usersPerThread;
            futures.add(executor.submit(() -> {
                for (int i = 0; i < usersPerThread; i++) {
                    int id = registry.getOrAssignId("concurrent-user-" + (offset + i));
                    assertThat(allIds.add(id)).isTrue();
                }
                return null;
            }));
        }

        for (var f : futures) f.get();
        executor.shutdown();
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd backend && mvn test -pl canvas-engine -Dtest=UserIdRegistryTest -v`
Expected: FAIL - UserIdRegistry class not found

- [ ] **Step 3: Create Lua script for atomic get-or-assign**

```lua
-- backend/canvas-engine/src/main/resources/scripts/userid_get_or_assign.lua
-- KEYS[1] = mapping key (canvas:uid:mapping:<userId>)
-- KEYS[2] = counter key (canvas:uid:counter)
-- Returns: integer ID assigned to this userId

local existing = redis.call('GET', KEYS[1])
if existing then
    return tonumber(existing)
end

local newId = redis.call('INCR', KEYS[2])
redis.call('SET', KEYS[1], tostring(newId))
return newId
```

- [ ] **Step 4: Implement UserIdRegistry**

```java
package org.chovy.canvas.engine.audience;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct;
import java.util.List;

@Slf4j
@Component
public class UserIdRegistry {

    private static final String COUNTER_KEY = "canvas:uid:counter";
    private static final String MAPPING_PREFIX = "canvas:uid:mapping:";

    @Autowired
    private StringRedisTemplate redisTemplate;

    private DefaultRedisScript<Long> getOrAssignScript;

    @PostConstruct
    void init() {
        getOrAssignScript = new DefaultRedisScript<>();
        getOrAssignScript.setScriptText(
            "local existing = redis.call('GET', KEYS[1]) " +
            "if existing then return tonumber(existing) end " +
            "local newId = redis.call('INCR', KEYS[2]) " +
            "redis.call('SET', KEYS[1], tostring(newId)) " +
            "return newId"
        );
        getOrAssignScript.setResultType(Long.class);
    }

    public int getOrAssignId(String userId) {
        String mappingKey = MAPPING_PREFIX + userId;
        Long id = redisTemplate.execute(
            getOrAssignScript,
            List.of(mappingKey, COUNTER_KEY)
        );
        if (id == null) {
            throw new IllegalStateException("Failed to get or assign userId: " + userId);
        }
        return id.intValue();
    }
}
```

- [ ] **Step 5: Run test to verify it passes**

Run: `cd backend && mvn test -pl canvas-engine -Dtest=UserIdRegistryTest -v`
Expected: PASS

- [ ] **Step 6: Commit**

```bash
git add backend/canvas-engine/src/main/resources/scripts/userid_get_or_assign.lua
git add backend/canvas-engine/src/main/java/org/chovy/canvas/engine/audience/UserIdRegistry.java
git add backend/canvas-engine/src/test/java/org/chovy/canvas/engine/audience/UserIdRegistryTest.java
git commit -m "feat: implement atomic userId→integer registry via Redis Lua script"
```

---

### Task 2: Rewrite AudienceBitmapStore with Raw Bytes + GETBIT + BITOP

**Files:**
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/audience/AudienceBitmapStore.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/audience/AudienceBitmapStoreTest.java`

- [ ] **Step 1: Write failing test**

```java
package org.chovy.canvas.engine.audience;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class AudienceBitmapStoreTest {

    @Autowired
    private AudienceBitmapStore bitmapStore;

    @Autowired
    private UserIdRegistry userIdRegistry;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Test
    void setMember_andIsMember_shouldReflectMembership() {
        String audienceKey = "canvas:audience:test-audience";
        String userId = "user-check-001";

        bitmapStore.addMember(audienceKey, userId);
        assertThat(bitmapStore.isMember(audienceKey, userId)).isTrue();
        assertThat(bitmapStore.isMember(audienceKey, "user-check-002")).isFalse();
    }

    @Test
    void setOperation_shouldPerformRedisBitop() {
        String keyA = "canvas:audience:audience-a";
        String keyB = "canvas:audience:audience-b";
        String resultKey = "canvas:audience:result";

        bitmapStore.addMember(keyA, "user-op-1");
        bitmapStore.addMember(keyA, "user-op-2");
        bitmapStore.addMember(keyB, "user-op-2");
        bitmapStore.addMember(keyB, "user-op-3");

        // BITOP OR gives union: {user-op-1, user-op-2, user-op-3}
        bitmapStore.setOperation(resultKey, "OR", keyA, keyB);

        assertThat(bitmapStore.isMember(resultKey, "user-op-1")).isTrue();  // in A
        assertThat(bitmapStore.isMember(resultKey, "user-op-2")).isTrue();  // in both A and B
        assertThat(bitmapStore.isMember(resultKey, "user-op-3")).isTrue();  // in B
    }

    @Test
    void addMember_shouldStoreRawBytes_notBase64() {
        String audienceKey = "canvas:audience:raw-check";
        bitmapStore.addMember(audienceKey, "user-raw-1");

        // Verify Redis key holds raw bytes (not Base64 encoded string)
        byte[] rawValue = redisTemplate.getConnectionFactory()
            .getConnection().stringCommands().get(audienceKey.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        // Raw bitmap bytes should not be valid Base64 for small bitmaps
        assertThat(rawValue).isNotNull();
        assertThat(rawValue.length).isGreaterThan(0);
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd backend && mvn test -pl canvas-engine -Dtest=AudienceBitmapStoreTest -v`
Expected: FAIL - AudienceBitmapStore does not have these methods

- [ ] **Step 3: Implement AudienceBitmapStore rewrite**

```java
// Rewrite AudienceBitmapStore.java - replace hash-based implementation

package org.chovy.canvas.engine.audience;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.connection.RedisCallback;
import org.springframework.stereotype.Component;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class AudienceBitmapStore {

    private static final String AUDIENCE_PREFIX = "canvas:audience:";

    @Autowired
    private UserIdRegistry userIdRegistry;

    @Autowired
    private StringRedisTemplate redisTemplate;

    /**
     * Add a user to the audience bitmap.
     * Uses deterministic userId→integer mapping. Stores as raw byte bitmap.
     */
    public void addMember(String audienceKey, String userId) {
        int bitOffset = userIdRegistry.getOrAssignId(userId);
        redisTemplate.opsForValue().setBit(audienceKey, bitOffset);
    }

    /**
     * Check if a user is in the audience bitmap.
     * Uses Redis GETBIT - O(1), no deserialization needed.
     */
    public boolean isMember(String audienceKey, String userId) {
        int bitOffset = userIdRegistry.getOrAssignId(userId);
        Boolean result = redisTemplate.opsForValue().getBit(audienceKey, bitOffset);
        return Boolean.TRUE.equals(result);
    }

    /**
     * Perform set operation on bitmaps using Redis BITOP.
     * operation: "AND", "OR", "XOR", "NOT"
     */
    public void setOperation(String destKey, String operation, String... sourceKeys) {
        redisTemplate.execute((RedisCallback<Void>) connection -> {
            byte[] destRaw = destKey.getBytes();
            List<byte[]> sourceRaws = Arrays.stream(sourceKeys)
                    .map(String::getBytes).toList();
            connection.stringCommands().bitOp(
                    org.springframework.data.redis.connection.BitOperation.valueOf(operation),
                    destRaw, sourceRaws.toArray(new byte[0][]));
            return null;
        });
    }

    /**
     * Get the count of members in the audience bitmap.
     * Uses Redis BITCOUNT.
     */
    public long countMembers(String audienceKey) {
        return redisTemplate.execute((RedisCallback<Long>) connection ->
                connection.stringCommands().bitCount(audienceKey.getBytes()));
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `cd backend && mvn test -pl canvas-engine -Dtest=AudienceBitmapStoreTest -v`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add backend/canvas-engine/src/main/java/org/chovy/canvas/engine/audience/AudienceBitmapStore.java
git add backend/canvas-engine/src/test/java/org/chovy/canvas/engine/audience/AudienceBitmapStoreTest.java
git commit -m "feat: rewrite AudienceBitmapStore with deterministic mapping, GETBIT, BITOP"
```

---

### Task 3: Adapt AudienceUserResolver and AudienceBatchComputeService

**Files:**
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/audience/AudienceUserResolver.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/audience/AudienceBatchComputeService.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/audience/AudienceUserResolverTest.java`

- [ ] **Step 1: Write failing test for AudienceUserResolver adaptation**

```java
package org.chovy.canvas.engine.audience;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class AudienceUserResolverTest {

    @Autowired
    private AudienceUserResolver resolver;

    @Autowired
    private AudienceBitmapStore bitmapStore;

    @Test
    void resolveAudience_shouldUseNewBitmapStore() {
        String audienceId = "test-audience-resolve";
        String bitmapKey = "canvas:audience:" + audienceId;

        // Add members via new store
        bitmapStore.addMember(bitmapKey, "user-resolve-1");
        bitmapStore.addMember(bitmapKey, "user-resolve-2");

        // Resolve should use GETBIT-based isMember
        var members = resolver.resolveAudience(audienceId);
        assertThat(members).contains("user-resolve-1", "user-resolve-2");
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd backend && mvn test -pl canvas-engine -Dtest=AudienceUserResolverTest -v`
Expected: FAIL - AudienceUserResolver still uses hash-based bitmap

- [ ] **Step 3: Modify AudienceUserResolver to use new bitmapStore.isMember()**

```java
// In AudienceUserResolver.java, replace the old hash-based resolution:

// BEFORE (hash-based, full deserialization):
// RoaringBitmap bitmap = RoaringBitmap.readFrom(redis.get(key));
// return bitmap.contains(murmur3(userId));

// AFTER (deterministic, GETBIT O(1)):
public boolean isUserInAudience(String audienceId, String userId) {
    String bitmapKey = AUDIENCE_PREFIX + audienceId;
    return audienceBitmapStore.isMember(bitmapKey, userId);
}
```

- [ ] **Step 4: Modify AudienceBatchComputeService to use new bitmapStore.addMember()**

```java
// In AudienceBatchComputeService.java, replace the old hash-based add:

// BEFORE (hash-based):
// int hash = murmur3(userId);
// bitmap.add(hash);
// redis.set(key, Base64.encode(bitmap.serialize()));

// AFTER (deterministic, raw bytes):
public void addUserToAudience(String audienceId, String userId) {
    String bitmapKey = AUDIENCE_PREFIX + audienceId;
    audienceBitmapStore.addMember(bitmapKey, userId);
}
```

- [ ] **Step 5: Run test to verify it passes**

Run: `cd backend && mvn test -pl canvas-engine -Dtest=AudienceUserResolverTest -v`
Expected: PASS

- [ ] **Step 6: Commit**

```bash
git add backend/canvas-engine/src/main/java/org/chovy/canvas/engine/audience/AudienceUserResolver.java
git add backend/canvas-engine/src/main/java/org/chovy/canvas/engine/audience/AudienceBatchComputeService.java
git add backend/canvas-engine/src/test/java/org/chovy/canvas/engine/audience/AudienceUserResolverTest.java
git commit -m "feat: adapt AudienceUserResolver and BatchComputeService to use deterministic bitmap"
```

---

### Task 4: Implement Existing Bitmap Migration Job

**Files:**
- Create: `backend/canvas-engine/src/main/resources/db/migration/V83__bitmap_migration_tracker.sql`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/audience/BitmapMigrationJob.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/audience/BitmapMigrationJobTest.java`

- [ ] **Step 1: Write failing test for migration job**

```java
package org.chovy.canvas.engine.audience;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class BitmapMigrationJobTest {

    @Autowired
    private BitmapMigrationJob migrationJob;

    @Autowired
    private AudienceBitmapStore bitmapStore;

    @Test
    void migrateAudience_shouldConvertBase64ToRawBytes() {
        // Setup: write a Base64-encoded RoaringBitmap to Redis (simulating old format)
        String audienceKey = "canvas:audience:migrate-test";

        // Create a RoaringBitmap with known integer IDs, serialize, Base64-encode, and write to Redis
        // This simulates the old AudienceBitmapStore.save() format
        RoaringBitmap oldBitmap = new RoaringBitmap();
        oldBitmap.add(1);
        oldBitmap.add(42);
        oldBitmap.add(1000);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (DataOutputStream dos = new DataOutputStream(bos)) {
            oldBitmap.serialize(dos);
            dos.flush();
        } catch (IOException e) { throw new RuntimeException(e); }
        String base64Encoded = Base64.getEncoder().encodeToString(bos.toByteArray());
        redisTemplate.opsForValue().set(audienceKey, base64Encoded);

        // Pre-register the reverse mapping: these integer IDs correspond to known userIds
        // In production, the migration job discovers this via SCAN of canvas:uid:mapping:* keys
        // For the test, we pre-populate the reverse index
        redisTemplate.opsForValue().set("canvas:uid:reverse:1", "known-user-1");
        redisTemplate.opsForValue().set("canvas:uid:reverse:42", "known-user-42");
        redisTemplate.opsForValue().set("canvas:uid:reverse:1000", "known-user-1000");

        migrationJob.migrateAudience(audienceKey);

        // After migration, isMember should work with new format
        assertThat(bitmapStore.isMember(audienceKey, "known-user-1")).isTrue();
        assertThat(bitmapStore.isMember(audienceKey, "known-user-42")).isTrue();
        assertThat(bitmapStore.isMember(audienceKey, "known-user-1000")).isTrue();
        assertThat(bitmapStore.isMember(audienceKey, "unknown-user")).isFalse();
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd backend && mvn test -pl canvas-engine -Dtest=BitmapMigrationJobTest -v`
Expected: FAIL - BitmapMigrationJob class not found

- [ ] **Step 3: Create migration tracker table**

```sql
-- backend/canvas-engine/src/main/resources/db/migration/V83__bitmap_migration_tracker.sql
CREATE TABLE canvas_bitmap_migration (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    audience_key VARCHAR(255) NOT NULL UNIQUE,
    old_size_bytes INT,
    new_size_bytes INT,
    member_count BIGINT,
    migrated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_audience_key (audience_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Track bitmap migration from Base64 to raw bytes';
```

- [ ] **Step 4: Implement BitmapMigrationJob**

```java
package org.chovy.canvas.engine.audience;

import lombok.extern.slf4j.Slf4j;
import org.roaringbitmap.RoaringBitmap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.util.Base64;

@Slf4j
@Component
public class BitmapMigrationJob {

    private static final String REVERSE_PREFIX = "canvas:uid:reverse:";

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private AudienceBitmapStore bitmapStore;

    @Autowired
    private UserIdRegistry userIdRegistry;

    /**
     * Migrate a single audience bitmap from Base64 RoaringBitmap to raw byte bitmap.
     *
     * <p>Steps:
     * 1. Read old Base64 value from Redis
     * 2. Decode Base64 → deserialize RoaringBitmap
     * 3. Iterate each integer ID in the old bitmap
     * 4. For each integer, look up the userId via the reverse index (canvas:uid:reverse:{integerId})
     * 5. For each resolved userId, call bitmapStore.addMember() to write to the new deterministic bitmap
     * 6. Delete the old Base64 key
     *
     * <p>The reverse index (canvas:uid:reverse:{integerId} → userId) must be populated
     * before migration. This is done automatically by UserIdRegistry.getOrAssignId():
     * whenever a new userId is assigned an integer ID, the reverse mapping is also written.
     * For pre-existing mappings, a one-time scan of canvas:uid:mapping:* keys is needed
     * to build the reverse index (see buildReverseIndex method below).
     */
    public void migrateAudience(String audienceKey) {
        log.info("[MIGRATE] Migrating bitmap: {}", audienceKey);

        String base64Value = redisTemplate.opsForValue().get(audienceKey);
        if (base64Value == null) {
            log.warn("[MIGRATE] No existing bitmap found for key: {}", audienceKey);
            return;
        }

        // Decode Base64 → RoaringBitmap
        byte[] rawBytes;
        try {
            rawBytes = Base64.getDecoder().decode(base64Value);
        } catch (IllegalArgumentException e) {
            log.error("[MIGRATE] Base64 decode failed for key: {}", audienceKey, e);
            return;
        }

        RoaringBitmap oldBitmap = new RoaringBitmap();
        try {
            oldBitmap.deserialize(new DataInputStream(new ByteArrayInputStream(rawBytes)));
        } catch (Exception e) {
            log.error("[MIGRATE] Failed to deserialize RoaringBitmap for key: {}", audienceKey, e);
            return;
        }

        // Iterate each integer in the old bitmap and resolve to userId via reverse index
        long migratedCount = 0;
        long skippedCount = 0;
        var it = oldBitmap.getIntIterator();
        while (it.hasNext()) {
            int integerId = it.next();
            String reverseKey = REVERSE_PREFIX + integerId;
            String userId = redisTemplate.opsForValue().get(reverseKey);
            if (userId != null && !userId.isBlank()) {
                bitmapStore.addMember(audienceKey, userId);
                migratedCount++;
            } else {
                skippedCount++;
                log.debug("[MIGRATE] No reverse mapping for integerId={}, skipping", integerId);
            }
        }

        // Delete the old Base64 key (replaced by raw byte bitmap written by addMember)
        redisTemplate.delete(audienceKey);

        log.info("[MIGRATE] Migration complete for key={} migrated={} skipped={} totalInOld={}",
                audienceKey, migratedCount, skippedCount, oldBitmap.getLongCardinality());
    }

    /**
     * One-time utility: build reverse index by scanning all canvas:uid:mapping:* keys.
     * For each mapping key (canvas:uid:mapping:{userId} → integerId), writes
     * canvas:uid:reverse:{integerId} → userId.
     *
     * <p>Must be run once before migrateAudience if the reverse index doesn't exist yet.
     * Uses Redis SCAN for efficiency (non-blocking).
     */
    public long buildReverseIndex() {
        long count = 0;
        try (var connection = redisTemplate.getConnectionFactory().getConnection()) {
            org.springframework.data.redis.core.ScanOptions scanOptions =
                    org.springframework.data.redis.core.ScanOptions.scanOptions()
                            .match("canvas:uid:mapping:*").count(1000).build();
            org.springframework.data.redis.core.Cursor<byte[]> cursor =
                    connection.keyCommands().scan(org.springframework.data.redis.core.CursorId.initial(), scanOptions);
            while (cursor.hasNext()) {
                byte[] keyBytes = cursor.next();
                String mappingKey = new String(keyBytes);
                String userId = mappingKey.substring("canvas:uid:mapping:".length());
                String integerIdStr = redisTemplate.opsForValue().get(mappingKey);
                if (integerIdStr != null) {
                    redisTemplate.opsForValue().set(REVERSE_PREFIX + integerIdStr, userId);
                    count++;
                }
            }
            cursor.close();
        }
        log.info("[MIGRATE] Built reverse index with {} entries", count);
        return count;
    }
}
```

- [ ] **Step 5: Run test to verify it passes**

Run: `cd backend && mvn test -pl canvas-engine -Dtest=BitmapMigrationJobTest -v`
Expected: PASS

- [ ] **Step 6: Commit**

```bash
git add backend/canvas-engine/src/main/resources/db/migration/V83__bitmap_migration_tracker.sql
git add backend/canvas-engine/src/main/java/org/chovy/canvas/engine/audience/BitmapMigrationJob.java
git add backend/canvas-engine/src/test/java/org/chovy/canvas/engine/audience/BitmapMigrationJobTest.java
git commit -m "feat: add bitmap migration job from Base64 RoaringBitmap to raw byte deterministic bitmap"
```
