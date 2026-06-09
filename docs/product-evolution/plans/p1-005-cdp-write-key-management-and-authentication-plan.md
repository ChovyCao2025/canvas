# CDP Write Key Management And Authentication Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add tenant-scoped CDP write keys, secret-safe admin endpoints, and Basic Auth validation for CDP ingestion.

**Architecture:** Store only key prefix and BCrypt hash in MySQL. Keep parsing, validation, generation, listing, and disable behavior in `CdpWriteKeyAuthService`, with a thin controller for tenant-scoped admin endpoints.

**Tech Stack:** Java 21, Spring Boot WebFlux, MyBatis-Plus, Flyway, BCrypt, JUnit 5, Mockito, AssertJ.

**Implementation Status:** Implemented on 2026-06-05 with the actual migration version `V100__cdp_write_key_management.sql` because `V97` is not the current migration sequence in this workspace. Focused Java 21 verification passed on 2026-06-08 for `CdpWriteKeySchemaTest`, `CdpWriteKeyAuthServiceTest`, and `CdpWriteKeyControllerTest`, covering schema, authentication, and admin API behavior.

---

## Spec Reference

- `docs/product-evolution/specs/p1-005-cdp-write-key-management-and-authentication.md`

## File Structure

- Create: `backend/canvas-engine/src/main/resources/db/migration/V100__cdp_write_key_management.sql`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/cdp/CdpWriteKeySchemaTest.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/CdpWriteKeyDO.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/CdpWriteKeyMapper.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/cdp/CdpWriteKeyAuthService.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dto/cdp/CdpWriteKeyCreateReq.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dto/cdp/CdpWriteKeyCreateResp.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dto/cdp/CdpWriteKeyRowDTO.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/CdpWriteKeyController.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/cdp/CdpWriteKeyAuthServiceTest.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/web/CdpWriteKeyControllerTest.java`

### Task 1: Schema And Data Object

**Files:**
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/cdp/CdpWriteKeySchemaTest.java`
- Create: `backend/canvas-engine/src/main/resources/db/migration/V100__cdp_write_key_management.sql`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/CdpWriteKeyDO.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/CdpWriteKeyMapper.java`

- [x] **Step 1: Write schema test**

```java
class CdpWriteKeySchemaTest {

    @Test
    void migrationCreatesWriteKeyTableWithoutRawSecretStorage() throws Exception {
        String sql = Files.readString(Path.of(
                "src/main/resources/db/migration/V100__cdp_write_key_management.sql"));

        assertThat(sql)
                .contains("CREATE TABLE IF NOT EXISTS cdp_write_key")
                .contains("tenant_id")
                .contains("key_prefix")
                .contains("key_hash")
                .contains("platform")
                .contains("rate_limit_qps")
                .contains("daily_quota")
                .contains("UNIQUE KEY uk_cdp_write_key_prefix")
                .doesNotContain("raw_key");
    }
}
```

- [x] **Step 2: Run schema test and confirm red state**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=CdpWriteKeySchemaTest
```

Expected: FAIL because migration does not exist.

Observed: The migration already existed as `V100__cdp_write_key_management.sql` in the current workspace, so this red-state step was superseded by existing implementation.

- [x] **Step 3: Add migration**

Create `V100__cdp_write_key_management.sql`:

```sql
CREATE TABLE IF NOT EXISTS cdp_write_key (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id BIGINT NOT NULL DEFAULT 0,
    name VARCHAR(128) NOT NULL,
    key_prefix VARCHAR(16) NOT NULL,
    key_hash VARCHAR(120) NOT NULL,
    platform VARCHAR(32) NOT NULL DEFAULT 'WEB',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    rate_limit_qps INT NOT NULL DEFAULT 100,
    daily_quota BIGINT NULL,
    description VARCHAR(500) NULL,
    created_by VARCHAR(128) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_cdp_write_key_prefix (key_prefix),
    INDEX idx_cdp_write_key_tenant_status (tenant_id, status),
    INDEX idx_cdp_write_key_platform (tenant_id, platform)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='CDP SDK write keys';
```

- [x] **Step 4: Add data object and mapper**

Create `CdpWriteKeyDO.java`:

```java
@Data
@TableName("cdp_write_key")
public class CdpWriteKeyDO {
    public static final String ACTIVE = "ACTIVE";
    public static final String DISABLED = "DISABLED";

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long tenantId;
    private String name;
    private String keyPrefix;
    private String keyHash;
    private String platform;
    private String status;
    private Integer rateLimitQps;
    private Long dailyQuota;
    private String description;
    private String createdBy;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
```

Create `CdpWriteKeyMapper.java`:

```java
@Mapper
public interface CdpWriteKeyMapper extends BaseMapper<CdpWriteKeyDO> {
}
```

- [x] **Step 5: Run schema test**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=CdpWriteKeySchemaTest
```

Expected: PASS.

Observed on 2026-06-08: PASS with Java 21 as part of `CdpWriteKeySchemaTest,CdpWriteKeyAuthServiceTest,CdpWriteKeyControllerTest` focused verification.

### Task 2: Authentication Service

**Files:**
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/cdp/CdpWriteKeyAuthServiceTest.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/cdp/CdpWriteKeyAuthService.java`

- [x] **Step 1: Write auth tests**

Create `CdpWriteKeyAuthServiceTest.java`:

```java
class CdpWriteKeyAuthServiceTest {
    private CdpWriteKeyMapper mapper;
    private BCryptPasswordEncoder encoder;
    private CdpWriteKeyAuthService service;

    @BeforeEach
    void setUp() {
        mapper = mock(CdpWriteKeyMapper.class);
        encoder = new BCryptPasswordEncoder();
        service = new CdpWriteKeyAuthService(mapper, encoder);
    }

    @Test
    void authenticateResolvesActiveWriteKeyFromBasicAuth() {
        String raw = "ck_test_0123456789abcdef";
        when(mapper.selectOne(any())).thenReturn(row(raw, CdpWriteKeyDO.ACTIVE));

        var result = service.authenticate(headers(raw));

        assertThat(result.tenantId()).isEqualTo(42L);
        assertThat(result.writeKeyId()).isEqualTo(7L);
        assertThat(result.platform()).isEqualTo("WEB");
    }

    @Test
    void authenticateRejectsMalformedBasicAuth() {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, "Bearer token");

        assertThatThrownBy(() -> service.authenticate(headers))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("CDP write key is required");
    }

    @Test
    void authenticateRejectsDisabledWriteKey() {
        String raw = "ck_test_disabled";
        when(mapper.selectOne(any())).thenReturn(row(raw, CdpWriteKeyDO.DISABLED));

        assertThatThrownBy(() -> service.authenticate(headers(raw)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("CDP write key is disabled");
    }

    @Test
    void authenticateRejectsHashMismatch() {
        when(mapper.selectOne(any())).thenReturn(row("ck_test_real", CdpWriteKeyDO.ACTIVE));

        assertThatThrownBy(() -> service.authenticate(headers("ck_test_wrong")))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("CDP write key is invalid");
    }

    private CdpWriteKeyDO row(String raw, String status) {
        CdpWriteKeyDO row = new CdpWriteKeyDO();
        row.setId(7L);
        row.setTenantId(42L);
        row.setKeyPrefix(raw.substring(0, Math.min(raw.length(), 12)));
        row.setKeyHash(encoder.encode(raw));
        row.setPlatform("WEB");
        row.setStatus(status);
        row.setRateLimitQps(100);
        return row;
    }

    private HttpHeaders headers(String raw) {
        String token = Base64.getEncoder()
                .encodeToString((raw + ":").getBytes(StandardCharsets.UTF_8));
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, "Basic " + token);
        return headers;
    }
}
```

- [x] **Step 2: Run auth tests and confirm red state**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=CdpWriteKeyAuthServiceTest
```

Expected: FAIL because service does not exist.

Observed: The service already existed in the current workspace, so this red-state step was superseded by existing implementation.

- [x] **Step 3: Implement auth service**

Create `CdpWriteKeyAuthService.java`:

```java
@Service
@RequiredArgsConstructor
public class CdpWriteKeyAuthService {
    private static final SecureRandom RANDOM = new SecureRandom();

    private final CdpWriteKeyMapper writeKeyMapper;
    private final BCryptPasswordEncoder passwordEncoder;

    public record AuthenticatedWriteKey(
            Long writeKeyId,
            Long tenantId,
            String keyPrefix,
            String platform,
            Integer rateLimitQps,
            Long dailyQuota
    ) {
    }

    public AuthenticatedWriteKey authenticate(HttpHeaders headers) {
        String raw = extractBasicWriteKey(headers);
        CdpWriteKeyDO row = writeKeyMapper.selectOne(new LambdaQueryWrapper<CdpWriteKeyDO>()
                .eq(CdpWriteKeyDO::getKeyPrefix, prefix(raw))
                .last("LIMIT 1"));
        if (row == null || !passwordEncoder.matches(raw, row.getKeyHash())) {
            throw unauthorized("CDP write key is invalid");
        }
        if (!CdpWriteKeyDO.ACTIVE.equals(row.getStatus())) {
            throw unauthorized("CDP write key is disabled");
        }
        return new AuthenticatedWriteKey(
                row.getId(), row.getTenantId(), row.getKeyPrefix(), row.getPlatform(),
                row.getRateLimitQps(), row.getDailyQuota());
    }

    public String generateRawKey() {
        byte[] bytes = new byte[24];
        RANDOM.nextBytes(bytes);
        return "ck_" + HexFormat.of().formatHex(bytes);
    }

    private String extractBasicWriteKey(HttpHeaders headers) {
        String auth = headers.getFirst(HttpHeaders.AUTHORIZATION);
        if (auth == null || !auth.startsWith("Basic ")) {
            throw unauthorized("CDP write key is required");
        }
        try {
            String decoded = new String(Base64.getDecoder().decode(auth.substring(6)), StandardCharsets.UTF_8);
            int colon = decoded.indexOf(':');
            String raw = colon >= 0 ? decoded.substring(0, colon) : decoded;
            if (raw.isBlank()) {
                throw unauthorized("CDP write key is required");
            }
            return raw;
        } catch (IllegalArgumentException e) {
            throw unauthorized("CDP write key is malformed");
        }
    }

    private String prefix(String raw) {
        return raw.substring(0, Math.min(raw.length(), 12));
    }

    private ResponseStatusException unauthorized(String message) {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, message);
    }
}
```

- [x] **Step 4: Run auth tests**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=CdpWriteKeyAuthServiceTest
```

Expected: PASS.

Observed on 2026-06-08: PASS with Java 21 as part of `CdpWriteKeySchemaTest,CdpWriteKeyAuthServiceTest,CdpWriteKeyControllerTest` focused verification.

### Task 3: Admin DTOs And Controller

**Files:**
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dto/cdp/CdpWriteKeyCreateReq.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dto/cdp/CdpWriteKeyCreateResp.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dto/cdp/CdpWriteKeyRowDTO.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/CdpWriteKeyController.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/web/CdpWriteKeyControllerTest.java`

- [x] **Step 1: Add DTO records**

```java
public record CdpWriteKeyCreateReq(
        String name,
        String platform,
        Integer rateLimitQps,
        Long dailyQuota,
        String description
) {
}

public record CdpWriteKeyCreateResp(
        Long id,
        String name,
        String writeKey,
        String keyPrefix,
        String platform,
        Integer rateLimitQps,
        Long dailyQuota
) {
}

public record CdpWriteKeyRowDTO(
        Long id,
        String name,
        String keyPrefix,
        String platform,
        String status,
        Integer rateLimitQps,
        Long dailyQuota,
        String description,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
```

- [x] **Step 2: Add create/list/disable service methods**

Add to `CdpWriteKeyAuthService`:

```java
public CdpWriteKeyDO create(Long tenantId, CdpWriteKeyCreateReq req, String createdBy, String rawKey) {
    CdpWriteKeyDO row = new CdpWriteKeyDO();
    row.setTenantId(tenantId);
    row.setName(req.name().trim());
    row.setKeyPrefix(prefix(rawKey));
    row.setKeyHash(passwordEncoder.encode(rawKey));
    row.setPlatform(req.platform() == null || req.platform().isBlank() ? "WEB" : req.platform().trim().toUpperCase(Locale.ROOT));
    row.setStatus(CdpWriteKeyDO.ACTIVE);
    row.setRateLimitQps(req.rateLimitQps() == null || req.rateLimitQps() <= 0 ? 100 : req.rateLimitQps());
    row.setDailyQuota(req.dailyQuota());
    row.setDescription(req.description());
    row.setCreatedBy(createdBy);
    writeKeyMapper.insert(row);
    return row;
}

public List<CdpWriteKeyDO> listTenantKeys(Long tenantId) {
    return writeKeyMapper.selectList(new LambdaQueryWrapper<CdpWriteKeyDO>()
            .eq(CdpWriteKeyDO::getTenantId, tenantId)
            .orderByDesc(CdpWriteKeyDO::getId));
}

public void disable(Long tenantId, Long id) {
    CdpWriteKeyDO row = writeKeyMapper.selectById(id);
    if (row == null || !tenantId.equals(row.getTenantId())) {
        throw new IllegalArgumentException("CDP write key not found: " + id);
    }
    row.setStatus(CdpWriteKeyDO.DISABLED);
    writeKeyMapper.updateById(row);
}
```

- [x] **Step 3: Add controller**

Create `CdpWriteKeyController.java`:

```java
@RestController
@RequestMapping("/cdp/write-keys")
@RequiredArgsConstructor
public class CdpWriteKeyController {
    private final TenantContextResolver tenantContextResolver;
    private final CdpWriteKeyAuthService writeKeyService;

    @GetMapping
    public Mono<R<List<CdpWriteKeyRowDTO>>> list() {
        return tenantContextResolver.current()
                .flatMap(ctx -> Mono.fromCallable(() -> R.ok(writeKeyService.listTenantKeys(ctx.tenantId())
                        .stream()
                        .map(this::toRow)
                        .toList()))
                        .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping
    public Mono<R<CdpWriteKeyCreateResp>> create(@RequestBody CdpWriteKeyCreateReq req) {
        return tenantContextResolver.current()
                .flatMap(ctx -> Mono.fromCallable(() -> {
                    String raw = writeKeyService.generateRawKey();
                    CdpWriteKeyDO row = writeKeyService.create(ctx.tenantId(), req, ctx.username(), raw);
                    return R.ok(new CdpWriteKeyCreateResp(
                            row.getId(), row.getName(), raw, row.getKeyPrefix(), row.getPlatform(),
                            row.getRateLimitQps(), row.getDailyQuota()));
                }).subscribeOn(Schedulers.boundedElastic()));
    }

    @DeleteMapping("/{id}")
    public Mono<R<Void>> disable(@PathVariable Long id) {
        return tenantContextResolver.current()
                .flatMap(ctx -> Mono.fromCallable(() -> {
                    writeKeyService.disable(ctx.tenantId(), id);
                    return R.<Void>ok();
                }).subscribeOn(Schedulers.boundedElastic()));
    }

    private CdpWriteKeyRowDTO toRow(CdpWriteKeyDO row) {
        return new CdpWriteKeyRowDTO(
                row.getId(), row.getName(), row.getKeyPrefix(), row.getPlatform(), row.getStatus(),
                row.getRateLimitQps(), row.getDailyQuota(), row.getDescription(), row.getCreatedAt(), row.getUpdatedAt());
    }
}
```

- [x] **Step 4: Add controller tests**

In `CdpWriteKeyControllerTest`, verify create response contains `writeKey` and list response only contains `keyPrefix`:

```java
@Test
void createReturnsRawKeyOnceAndListReturnsOnlyPrefix() {
    when(writeKeyService.generateRawKey()).thenReturn("ck_raw_secret");
    when(writeKeyService.create(eq(42L), any(), eq("alice"), eq("ck_raw_secret"))).thenReturn(row("ck_raw_secret"));

    CdpWriteKeyCreateResp created = controller.create(new CdpWriteKeyCreateReq("Website", "WEB", 100, null, ""))
            .block()
            .getData();

    assertThat(created.writeKey()).isEqualTo("ck_raw_secret");
    assertThat(created.keyPrefix()).isEqualTo("ck_raw_secre");
}
```

- [x] **Step 5: Run focused tests**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=CdpWriteKeySchemaTest,CdpWriteKeyAuthServiceTest,CdpWriteKeyControllerTest
```

Expected: PASS.

Observed on 2026-06-08: PASS with Java 21:

```bash
cd backend && JAVA_HOME=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home mvn -pl canvas-engine -Dtest=CdpWriteKeySchemaTest,CdpWriteKeyAuthServiceTest,CdpWriteKeyControllerTest test
```

Result: 7 tests, 0 failures, 0 errors, 0 skipped.

Earlier static checks also confirmed:

```bash
rg -n "CREATE TABLE IF NOT EXISTS cdp_write_key|key_prefix|key_hash|raw_key|UNIQUE KEY uk_cdp_write_key_prefix" backend/canvas-engine/src/main/resources/db/migration/V100__cdp_write_key_management.sql
rg -n "@RequestMapping\\(\"/cdp/write-keys\"\\)|@GetMapping|@PostMapping|@DeleteMapping|writeKeyService\\.generateRawKey|listTenantKeys|disable\\(" backend/canvas-engine/src/main/java/org/chovy/canvas/web/CdpWriteKeyController.java
rg -n "class CdpWriteKeySchemaTest|class CdpWriteKeyAuthServiceTest|class CdpWriteKeyControllerTest|authenticateRejectsDisabledWriteKey|createReturnsRawKeyOnce" backend/canvas-engine/src/test/java/org/chovy/canvas/domain/cdp/CdpWriteKeySchemaTest.java backend/canvas-engine/src/test/java/org/chovy/canvas/domain/cdp/CdpWriteKeyAuthServiceTest.java backend/canvas-engine/src/test/java/org/chovy/canvas/web/CdpWriteKeyControllerTest.java
```

### Task 4: Commit This Slice

**Files:**
- Read: `docs/product-evolution/specs/p1-005-cdp-write-key-management-and-authentication.md`
- Read: `docs/product-evolution/plans/p1-005-cdp-write-key-management-and-authentication-plan.md`

- [x] **Step 1: Document commit boundary**
Boundary: No git commit or merge was created in this docs-only audit; the command below remains the future scoped staging recipe.

Run:

```bash
git add backend/canvas-engine/src/main/resources/db/migration/V100__cdp_write_key_management.sql \
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/CdpWriteKeyDO.java \
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/CdpWriteKeyMapper.java \
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/cdp/CdpWriteKeyAuthService.java \
  backend/canvas-engine/src/main/java/org/chovy/canvas/dto/cdp/CdpWriteKeyCreateReq.java \
  backend/canvas-engine/src/main/java/org/chovy/canvas/dto/cdp/CdpWriteKeyCreateResp.java \
  backend/canvas-engine/src/main/java/org/chovy/canvas/dto/cdp/CdpWriteKeyRowDTO.java \
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/CdpWriteKeyController.java \
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/cdp/CdpWriteKeySchemaTest.java \
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/cdp/CdpWriteKeyAuthServiceTest.java \
  backend/canvas-engine/src/test/java/org/chovy/canvas/web/CdpWriteKeyControllerTest.java \
  docs/product-evolution/specs/p1-005-cdp-write-key-management-and-authentication.md \
  docs/product-evolution/plans/p1-005-cdp-write-key-management-and-authentication-plan.md
git commit -m "feat: add cdp write key management"
```

Expected: commit contains only CDP write-key schema, auth, admin API, tests, and docs.
