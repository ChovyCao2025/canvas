# Audience Data Source Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Extract JDBC connection settings into reusable audience-scoped data sources so JDBC audiences select a data source instead of re-entering database credentials.

**Architecture:** Add a first-class `audience_data_source` persistence model and CRUD API in the backend, migrate legacy JDBC audience records onto that model, then update the frontend to manage data sources and reference them from the audience editor. JDBC compute will resolve connection settings via `dataSourceId` and keep table-level query settings on the audience definition.

**Tech Stack:** Spring Boot WebFlux, MyBatis-Plus, Flyway, Jackson, React 18, React Router 6, Ant Design 5, Vitest, Maven

---

## File Structure

### Backend

- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/audience/AudienceDataSource.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/audience/AudienceDataSourceMapper.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/controller/AudienceDataSourceController.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/audience/AudienceDataSourceService.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/audience/AudienceDataSourceServiceTest.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/audience/AudienceBatchComputeServiceJdbcConfigTest.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/audience/AudienceDefinition.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/controller/AudienceController.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/audience/AudienceBatchComputeService.java`
- Modify: `backend/canvas-engine/src/main/resources/db/migration/V41__audience_demo_data.sql`
- Create: `backend/canvas-engine/src/main/resources/db/migration/V44__audience_data_source.sql`

### Frontend

- Create: `frontend/src/services/audienceDataSourceApi.ts`
- Create: `frontend/src/pages/audience-data-source/index.tsx`
- Create: `frontend/src/pages/audience-data-source/index.test.tsx`
- Modify: `frontend/src/services/audienceApi.ts`
- Modify: `frontend/src/pages/audience-list/index.tsx`
- Modify: `frontend/src/pages/audience-edit/index.tsx`
- Modify: `frontend/src/App.tsx`

## Task 1: Add backend data model and migration

**Files:**
- Create: `backend/canvas-engine/src/main/resources/db/migration/V44__audience_data_source.sql`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/audience/AudienceDataSource.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/audience/AudienceDataSourceMapper.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/audience/AudienceDefinition.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/audience/AudienceDataSourceServiceTest.java`

- [ ] **Step 1: Write the failing backend service test for delete protection and reference counting**

```java
@Test
void delete_rejects_when_definition_references_data_source() {
    AudienceDataSourceMapper dataSourceMapper = mock(AudienceDataSourceMapper.class);
    AudienceDefinitionMapper definitionMapper = mock(AudienceDefinitionMapper.class);
    AudienceDataSourceService service = new AudienceDataSourceService(dataSourceMapper, definitionMapper);

    when(definitionMapper.selectCount(any())).thenReturn(1L);

    assertThatThrownBy(() -> service.delete(7L))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("referenced");
}

@Test
void list_with_reference_count_returns_usage_count() {
    AudienceDataSourceMapper dataSourceMapper = mock(AudienceDataSourceMapper.class);
    AudienceDefinitionMapper definitionMapper = mock(AudienceDefinitionMapper.class);
    AudienceDataSourceService service = new AudienceDataSourceService(dataSourceMapper, definitionMapper);

    AudienceDataSource source = new AudienceDataSource();
    source.setId(9L);
    source.setName("Demo DS");
    when(dataSourceMapper.selectList(null)).thenReturn(List.of(source));
    when(definitionMapper.selectCount(any())).thenReturn(3L);

    assertThat(service.list().getFirst().getReferenceCount()).isEqualTo(3L);
}
```

- [ ] **Step 2: Run the targeted test to verify it fails**

Run:

```bash
/bin/zsh -lc 'JAVA_HOME=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home PATH=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home/bin:$PATH mvn test -pl canvas-engine -Dtest=AudienceDataSourceServiceTest -q'
```

Expected: FAIL because `AudienceDataSourceService` and related model classes do not exist yet.

- [ ] **Step 3: Add migration and persistence model**

```sql
CREATE TABLE audience_data_source (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    url VARCHAR(1000) NOT NULL,
    username VARCHAR(200) NOT NULL,
    password VARCHAR(500) NOT NULL,
    driver_class_name VARCHAR(255),
    enabled TINYINT NOT NULL DEFAULT 1,
    created_by VARCHAR(100),
    created_at DATETIME,
    updated_at DATETIME
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

ALTER TABLE audience_definition
    ADD COLUMN data_source_id BIGINT NULL AFTER data_source_type;
```

```java
@Data
@TableName("audience_data_source")
public class AudienceDataSource {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String description;
    private String url;
    private String username;
    private String password;
    private String driverClassName;
    private Integer enabled;
    @TableField(exist = false)
    private Long referenceCount;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
```

```java
public interface AudienceDataSourceMapper extends BaseMapper<AudienceDataSource> {
}
```

```java
public class AudienceDefinition {
    private String dataSourceType;
    private Long dataSourceId;
    private String dataSourceConfig;
}
```

- [ ] **Step 4: Implement the minimal service for list/create/update/delete**

```java
@Service
@RequiredArgsConstructor
public class AudienceDataSourceService {
    private final AudienceDataSourceMapper dataSourceMapper;
    private final AudienceDefinitionMapper definitionMapper;

    public List<AudienceDataSource> list() {
        return dataSourceMapper.selectList(null).stream().peek(item ->
                item.setReferenceCount(definitionMapper.selectCount(
                        new LambdaQueryWrapper<AudienceDefinition>()
                                .eq(AudienceDefinition::getDataSourceId, item.getId())
                                .eq(AudienceDefinition::getDataSourceType, "JDBC")
                ))
        ).toList();
    }

    public void delete(Long id) {
        Long count = definitionMapper.selectCount(new LambdaQueryWrapper<AudienceDefinition>()
                .eq(AudienceDefinition::getDataSourceId, id)
                .eq(AudienceDefinition::getDataSourceType, "JDBC"));
        if (count != null && count > 0) {
            throw new IllegalStateException("Audience data source is still referenced");
        }
        dataSourceMapper.deleteById(id);
    }
}
```

- [ ] **Step 5: Re-run the targeted backend test**

Run:

```bash
/bin/zsh -lc 'JAVA_HOME=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home PATH=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home/bin:$PATH mvn test -pl canvas-engine -Dtest=AudienceDataSourceServiceTest -q'
```

Expected: PASS.

- [ ] **Step 6: Commit the model and migration slice**

```bash
git add backend/canvas-engine/src/main/resources/db/migration/V44__audience_data_source.sql backend/canvas-engine/src/main/java/org/chovy/canvas/domain/audience/AudienceDataSource.java backend/canvas-engine/src/main/java/org/chovy/canvas/domain/audience/AudienceDataSourceMapper.java backend/canvas-engine/src/main/java/org/chovy/canvas/domain/audience/AudienceDefinition.java backend/canvas-engine/src/main/java/org/chovy/canvas/engine/audience/AudienceDataSourceService.java backend/canvas-engine/src/test/java/org/chovy/canvas/engine/audience/AudienceDataSourceServiceTest.java
git commit -m "feat: add audience data source model"
```

## Task 2: Wire backend CRUD API and JDBC compute resolution

**Files:**
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/controller/AudienceDataSourceController.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/audience/AudienceBatchComputeService.java`
- Modify: `backend/canvas-engine/src/main/resources/db/migration/V41__audience_demo_data.sql`
- Modify: `backend/canvas-engine/src/main/resources/db/migration/V44__audience_data_source.sql`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/audience/AudienceBatchComputeServiceJdbcConfigTest.java`

- [ ] **Step 1: Write the failing JDBC config resolution test**

```java
@Test
void parse_jdbc_config_merges_data_source_record_and_definition_config() throws Exception {
    AudienceDataSource dataSource = new AudienceDataSource();
    dataSource.setUrl("jdbc:mysql://127.0.0.1:3306/canvas_demo");
    dataSource.setUsername("root");
    dataSource.setPassword("root");
    dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");

    AudienceDefinition definition = new AudienceDefinition();
    definition.setDataSourceType("JDBC");
    definition.setDataSourceId(11L);
    definition.setDataSourceConfig("{\"baseTable\":\"audience_demo_user\",\"userIdColumn\":\"user_id\",\"maxRows\":100}");

    JdbcConfig config = service.parseJdbcConfig(definition, dataSource);

    assertThat(config.baseTable()).isEqualTo("audience_demo_user");
    assertThat(config.url()).contains("canvas_demo");
    assertThat(config.maxRows()).isEqualTo(100);
}
```

- [ ] **Step 2: Run the targeted test to verify it fails**

Run:

```bash
/bin/zsh -lc 'JAVA_HOME=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home PATH=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home/bin:$PATH mvn test -pl canvas-engine -Dtest=AudienceBatchComputeServiceJdbcConfigTest -q'
```

Expected: FAIL because the compute service still expects full JDBC config inside `dataSourceConfig`.

- [ ] **Step 3: Expose backend CRUD controller and data-source-aware compute**

```java
@RestController
@RequestMapping("/canvas/audience-data-sources")
@RequiredArgsConstructor
public class AudienceDataSourceController {
    private final AudienceDataSourceService service;

    @GetMapping
    public Mono<R<List<AudienceDataSource>>> list() {
        return Mono.fromCallable(() -> R.ok(service.list())).subscribeOn(Schedulers.boundedElastic());
    }
}
```

```java
private final AudienceDataSourceMapper dataSourceMapper;

private RoaringBitmap computeViaJdbc(AudienceDefinition definition) throws Exception {
    if (definition.getDataSourceId() == null) {
        throw new IllegalArgumentException("dataSourceId is required for JDBC");
    }
    AudienceDataSource dataSource = dataSourceMapper.selectById(definition.getDataSourceId());
    if (dataSource == null) {
        throw new IllegalArgumentException("Audience data source not found: " + definition.getDataSourceId());
    }
    JdbcConfig jdbcConfig = parseJdbcConfig(definition, dataSource);
    // existing SQL generation continues unchanged
}

JdbcConfig parseJdbcConfig(AudienceDefinition definition, AudienceDataSource dataSource) throws Exception {
    Map<String, Object> config = objectMapper.readValue(
            definition.getDataSourceConfig() == null || definition.getDataSourceConfig().isBlank() ? "{}" : definition.getDataSourceConfig(),
            new TypeReference<>() {}
    );
    String baseTable = stringValue(config, "baseTable");
    String userIdColumn = stringValue(config, "userIdColumn", "user_id");
    Integer maxRows = config.get("maxRows") instanceof Number number ? number.intValue() : null;
    return new JdbcConfig(baseTable, dataSource.getUrl(), dataSource.getUsername(), dataSource.getPassword(), userIdColumn, stringValue(Map.of("driverClassName", dataSource.getDriverClassName()), "driverClassName", "com.mysql.cj.jdbc.Driver"), maxRows);
}
```

- [ ] **Step 4: Extend migration to move legacy audience JDBC config into `audience_data_source`**

```sql
INSERT INTO audience_data_source (id, name, description, url, username, password, driver_class_name, enabled, created_by, created_at, updated_at)
VALUES
(90001, '演示 JDBC 数据源', '从历史 audience_definition 迁移而来', 'jdbc:mysql://127.0.0.1:3306/canvas_demo?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai', 'root', 'root', 'com.mysql.cj.jdbc.Driver', 1, 'system', NOW(), NOW())
ON DUPLICATE KEY UPDATE updated_at = VALUES(updated_at);

UPDATE audience_definition
SET data_source_id = 90001,
    data_source_config = '{"baseTable":"audience_demo_user","userIdColumn":"user_id","maxRows":10000}'
WHERE id IN (90001, 90002, 90003);
```

- [ ] **Step 5: Re-run focused backend tests**

Run:

```bash
/bin/zsh -lc 'JAVA_HOME=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home PATH=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home/bin:$PATH mvn test -pl canvas-engine -Dtest=AudienceDataSourceServiceTest,AudienceBatchComputeServiceJdbcConfigTest,SqlWhereGeneratorTest -q'
```

Expected: PASS.

- [ ] **Step 6: Commit the backend API and compute slice**

```bash
git add backend/canvas-engine/src/main/java/org/chovy/canvas/controller/AudienceDataSourceController.java backend/canvas-engine/src/main/java/org/chovy/canvas/engine/audience/AudienceBatchComputeService.java backend/canvas-engine/src/main/resources/db/migration/V41__audience_demo_data.sql backend/canvas-engine/src/main/resources/db/migration/V44__audience_data_source.sql backend/canvas-engine/src/test/java/org/chovy/canvas/engine/audience/AudienceBatchComputeServiceJdbcConfigTest.java
git commit -m "feat: wire audience data source backend APIs"
```

## Task 3: Add frontend API and data source management page

**Files:**
- Create: `frontend/src/services/audienceDataSourceApi.ts`
- Create: `frontend/src/pages/audience-data-source/index.tsx`
- Create: `frontend/src/pages/audience-data-source/index.test.tsx`
- Modify: `frontend/src/App.tsx`

- [ ] **Step 1: Write the failing page test for empty state and create modal**

```tsx
it('shows management title and create button', async () => {
  vi.mocked(audienceDataSourceApi.list).mockResolvedValue({ data: [] } as any)
  render(<MemoryRouter><AudienceDataSourcePage /></MemoryRouter>)

  expect(await screen.findByText('人群数据源')).toBeInTheDocument()
  expect(screen.getByRole('button', { name: '新建数据源' })).toBeInTheDocument()
})
```

- [ ] **Step 2: Run the targeted frontend test to verify it fails**

Run:

```bash
cd frontend && npm test -- src/pages/audience-data-source/index.test.tsx
```

Expected: FAIL because the API module and page do not exist yet.

- [ ] **Step 3: Add typed frontend API and route**

```ts
export interface AudienceDataSource {
  id?: number
  name: string
  description?: string
  url: string
  username: string
  password?: string
  driverClassName?: string
  enabled: number
  referenceCount?: number
}

export const audienceDataSourceApi = {
  list: () => http.get('/canvas/audience-data-sources'),
  create: (body: AudienceDataSource) => http.post('/canvas/audience-data-sources', body),
  update: (id: number, body: AudienceDataSource) => http.put(`/canvas/audience-data-sources/${id}`, body),
  delete: (id: number) => http.delete(`/canvas/audience-data-sources/${id}`),
}
```

```tsx
<Route path="/audiences/data-sources" element={<AudienceDataSourcePage />} />
```

- [ ] **Step 4: Implement the management page using the existing config-page pattern**

```tsx
<div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 20 }}>
  <Title level={4} style={{ margin: 0 }}>人群数据源</Title>
  <Button type="primary" icon={<PlusOutlined />} onClick={openCreate}>新建数据源</Button>
</div>

<Table
  rowKey="id"
  dataSource={data}
  columns={[
    { title: '名称', dataIndex: 'name' },
    { title: 'JDBC URL', dataIndex: 'url', ellipsis: true },
    { title: '驱动', dataIndex: 'driverClassName', width: 220 },
    { title: '引用数', dataIndex: 'referenceCount', width: 90 },
  ]}
/>
```

- [ ] **Step 5: Re-run the targeted frontend test and build**

Run:

```bash
cd frontend && npm test -- src/pages/audience-data-source/index.test.tsx
cd frontend && npm run build
```

Expected: test PASS; build PASS.

- [ ] **Step 6: Commit the frontend management page slice**

```bash
git add frontend/src/services/audienceDataSourceApi.ts frontend/src/pages/audience-data-source/index.tsx frontend/src/pages/audience-data-source/index.test.tsx frontend/src/App.tsx
git commit -m "feat: add audience data source management page"
```

## Task 4: Update audience editor to reference reusable data sources

**Files:**
- Modify: `frontend/src/services/audienceApi.ts`
- Modify: `frontend/src/pages/audience-edit/index.tsx`
- Modify: `frontend/src/pages/audience-list/index.tsx`
- Test: `frontend/src/pages/audience-data-source/index.test.tsx`

- [ ] **Step 1: Write the failing editor test for JDBC mode**

```tsx
it('shows data source select instead of raw jdbc credential inputs', async () => {
  vi.mocked(audienceDataSourceApi.list).mockResolvedValue({
    data: [{ id: 1, name: 'Demo DS', url: 'jdbc:mysql://demo', username: 'root', enabled: 1 }],
  } as any)

  render(<MemoryRouter><AudienceEditPage /></MemoryRouter>)
  await userEvent.selectOptions(screen.getByLabelText('数据源类型'), 'JDBC')

  expect(await screen.findByText('人群数据源')).toBeInTheDocument()
  expect(screen.queryByLabelText('JDBC URL')).not.toBeInTheDocument()
  expect(screen.queryByLabelText('用户名')).not.toBeInTheDocument()
})
```

- [ ] **Step 2: Run the targeted frontend test to verify it fails**

Run:

```bash
cd frontend && npm test -- src/pages/audience-data-source/index.test.tsx
```

Expected: FAIL because `AudienceEditPage` still renders raw JDBC credential fields.

- [ ] **Step 3: Extend audience DTO shape and editor serialization**

```ts
export interface AudienceDefinition {
  id?: number
  dataSourceType: 'TAGGER_API' | 'JDBC'
  dataSourceId?: number
  dataSourceConfig?: string
}
```

```ts
type AudienceEditFormValues = Omit<AudienceDefinition, 'enabled'> & {
  jdbcConfig?: {
    baseTable?: string
    userIdColumn?: string
    maxRows?: number
  }
}
```

```ts
const body: AudienceDefinition = {
  ...values,
  dataSourceId: values.dataSourceType === 'JDBC' ? values.dataSourceId : undefined,
  dataSourceConfig: JSON.stringify(
    values.dataSourceType === 'JDBC'
      ? values.jdbcConfig
      : values.taggerConfig ?? {}
  ),
}
```

- [ ] **Step 4: Replace raw JDBC fields with data source selection and add list-page entry**

```tsx
{dataSourceType === 'JDBC' && (
  <>
    <Form.Item name="dataSourceId" label="人群数据源" rules={[{ required: true, message: '请选择人群数据源' }]}>
      <Select
        options={jdbcSources.map(item => ({ value: item.id, label: item.name }))}
        placeholder={jdbcSources.length ? '选择数据源' : '暂无可用数据源，请先创建'}
      />
    </Form.Item>
    <Form.Item name={['jdbcConfig', 'baseTable']} label="基础表名" rules={[{ required: true, message: '请输入基础表名' }]}>
      <Input placeholder="audience_demo_user" />
    </Form.Item>
  </>
)}
```

```tsx
<Space>
  <Button onClick={() => navigate('/audiences/data-sources')}>数据源管理</Button>
  <Button type="primary" icon={<PlusOutlined />} onClick={() => navigate('/audiences/new')}>新建人群</Button>
</Space>
```

- [ ] **Step 5: Run frontend verification**

Run:

```bash
cd frontend && npm test -- src/pages/audience-data-source/index.test.tsx
cd frontend && npm run build
```

Expected: PASS, and the compiled UI contains the new route plus updated audience editor.

- [ ] **Step 6: Commit the audience editor slice**

```bash
git add frontend/src/services/audienceApi.ts frontend/src/pages/audience-edit/index.tsx frontend/src/pages/audience-list/index.tsx
git commit -m "feat: reference audience data sources from audience editor"
```

## Task 5: Final integration verification

**Files:**
- Modify if needed: any of the files above to resolve issues found during verification

- [ ] **Step 1: Run backend audience-focused regression tests**

Run:

```bash
/bin/zsh -lc 'JAVA_HOME=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home PATH=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home/bin:$PATH mvn test -pl canvas-engine -Dtest=AudienceDataSourceServiceTest,AudienceBatchComputeServiceJdbcConfigTest,SqlWhereGeneratorTest,TaggerHandlerTest -q'
```

Expected: PASS.

- [ ] **Step 2: Run frontend tests and production build**

Run:

```bash
cd frontend && npm test
cd frontend && npm run build
```

Expected: PASS.

- [ ] **Step 3: Smoke-check migrated demo data assumptions**

Run:

```bash
rg -n "audience_data_source|data_source_id|audience_demo_user" backend/canvas-engine/src/main/resources/db/migration/V4*.sql
```

Expected: `V41__audience_demo_data.sql` seeds simplified audience configs, and `V44__audience_data_source.sql` creates/populates the reusable JDBC source.

- [ ] **Step 4: Review changed API contracts before merge**

Check these concrete points:

```text
- AudienceDefinition JSON now includes dataSourceId for JDBC audiences.
- /canvas/audience-data-sources exposes enabled and referenceCount.
- Disabled data sources remain visible for existing references but are not offered for new selection.
```

- [ ] **Step 5: Commit any final fixups**

```bash
git add backend/canvas-engine/src/main/java/org/chovy/canvas/controller/AudienceDataSourceController.java backend/canvas-engine/src/main/java/org/chovy/canvas/engine/audience/AudienceBatchComputeService.java frontend/src/pages/audience-data-source/index.tsx frontend/src/pages/audience-edit/index.tsx
git commit -m "test: verify audience data source integration"
```

## Self-Review

Spec coverage check:
- Data model extraction is covered by Task 1.
- Backend CRUD, deletion protection, and JDBC compute lookup are covered by Task 2.
- Frontend data source management entry and page are covered by Task 3.
- Audience editor/list integration and no-raw-credentials flow are covered by Task 4.
- Regression verification is covered by Task 5.

Placeholder scan:
- No `TODO`, `TBD`, or “similar to previous task” markers remain.

Type consistency:
- `dataSourceId`, `referenceCount`, and `driverClassName` are named consistently across backend, frontend, and tests in this plan.
