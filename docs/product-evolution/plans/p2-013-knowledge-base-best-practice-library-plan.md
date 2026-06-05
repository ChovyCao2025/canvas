# Knowledge Base And Best Practice Library Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the first best-practice library workflow: searchable articles, template-to-practice links, contextual help entries, and governed benchmark/case-study metadata.

**Architecture:** Store library content in additive Flyway tables and keep search, contextual help, and template linking in `KnowledgeBaseService`. Expose a thin tenant-aware controller, then add frontend API and presentation helpers for the knowledge-base page and contextual help component.

**Tech Stack:** Java 21, Spring Boot WebFlux-style `Mono`, Flyway, JUnit 5, Mockito, AssertJ, React 18, TypeScript, Axios, Ant Design, Vitest.

---

## Spec Reference

- `docs/product-evolution/specs/p2-013-knowledge-base-best-practice-library.md`
- Source item: `docs/product-evolution/todo/p2/product-opportunities-from-filtered-scope.md#knowledge-base-and-best-practice-library`

## File Structure

**Backend**
- Create: `backend/canvas-engine/src/main/resources/db/migration/V172__knowledge_base_best_practices.sql` - article, template link, contextual help, and benchmark tables.
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/knowledge/KnowledgeBaseService.java` - search, contextual help, and template-link logic.
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/KnowledgeBaseController.java` - `/knowledge-base` API.
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/knowledge/KnowledgeBaseServiceTest.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/controller/KnowledgeBaseControllerTest.java`

**Frontend**
- Create: `frontend/src/services/knowledgeBaseApi.ts`
- Create: `frontend/src/pages/knowledge-base/knowledgeBase.ts`
- Create: `frontend/src/pages/knowledge-base/knowledgeBase.test.tsx`
- Create: `frontend/src/pages/knowledge-base/index.tsx`
- Create: `frontend/src/components/help/ContextualHelp.tsx`

### Task 1: Knowledge Schema And Service

**Files:**
- Create: `backend/canvas-engine/src/main/resources/db/migration/V172__knowledge_base_best_practices.sql`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/knowledge/KnowledgeBaseService.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/knowledge/KnowledgeBaseServiceTest.java`

- [ ] **Step 1: Write service tests**

Create `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/knowledge/KnowledgeBaseServiceTest.java`:

```java
package org.chovy.canvas.domain.knowledge;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class KnowledgeBaseServiceTest {

    @Test
    void migrationCreatesKnowledgeTables() throws Exception {
        String sql = Files.readString(Path.of(
                "src/main/resources/db/migration/V172__knowledge_base_best_practices.sql"));

        assertThat(sql)
                .contains("CREATE TABLE IF NOT EXISTS knowledge_article")
                .contains("tenant_id BIGINT NULL")
                .contains("article_type VARCHAR(32) NOT NULL")
                .contains("CREATE TABLE IF NOT EXISTS knowledge_template_link")
                .contains("CREATE TABLE IF NOT EXISTS knowledge_contextual_help")
                .contains("CREATE TABLE IF NOT EXISTS knowledge_benchmark")
                .contains("INDEX idx_knowledge_article_search");
    }

    @Test
    void searchReturnsPublishedGlobalAndTenantArticlesSortedByUpdatedTime() {
        KnowledgeBaseService.KnowledgeRepository repository = mock(KnowledgeBaseService.KnowledgeRepository.class);
        KnowledgeBaseService service = new KnowledgeBaseService(repository);
        when(repository.search(8L, "coupon", Set.of("PLAYBOOK", "FAQ"))).thenReturn(List.of(
                new KnowledgeBaseService.Article(8L, "coupon_playbook", "Coupon Playbook", "PLAYBOOK", "Use coupons carefully", List.of("coupon"), "PUBLISHED", Instant.parse("2026-06-03T01:00:00Z")),
                new KnowledgeBaseService.Article(null, "coupon_faq", "Coupon FAQ", "FAQ", "Common questions", List.of("coupon"), "PUBLISHED", Instant.parse("2026-06-03T02:00:00Z"))));

        List<KnowledgeBaseService.Article> result = service.search(8L, new KnowledgeBaseService.SearchQuery("coupon", Set.of("PLAYBOOK", "FAQ")));

        assertThat(result).extracting(KnowledgeBaseService.Article::articleKey)
                .containsExactly("coupon_faq", "coupon_playbook");
    }

    @Test
    void searchRejectsUnsupportedArticleType() {
        KnowledgeBaseService service = new KnowledgeBaseService(mock(KnowledgeBaseService.KnowledgeRepository.class));

        assertThatThrownBy(() -> service.search(8L, new KnowledgeBaseService.SearchQuery("x", Set.of("BLOG"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("unsupported knowledge article type BLOG");
    }

    @Test
    void contextualHelpReturnsEntriesForSurfaceAndField() {
        KnowledgeBaseService.KnowledgeRepository repository = mock(KnowledgeBaseService.KnowledgeRepository.class);
        KnowledgeBaseService service = new KnowledgeBaseService(repository);
        when(repository.contextualHelp(8L, "canvas-editor", "couponCode")).thenReturn(List.of(
                new KnowledgeBaseService.HelpEntry("canvas-editor", "couponCode", "coupon_faq", "Coupon code FAQ")));

        List<KnowledgeBaseService.HelpEntry> result = service.contextualHelp(8L, "canvas-editor", "couponCode");

        assertThat(result).extracting(KnowledgeBaseService.HelpEntry::articleKey).containsExactly("coupon_faq");
    }

    @Test
    void templatePracticesCombinesPlaybooksAndCaseStudies() {
        KnowledgeBaseService.KnowledgeRepository repository = mock(KnowledgeBaseService.KnowledgeRepository.class);
        KnowledgeBaseService service = new KnowledgeBaseService(repository);
        when(repository.templateArticles(8L, "retail_welcome")).thenReturn(List.of(
                new KnowledgeBaseService.Article(null, "welcome_case", "Welcome Case Study", "CASE_STUDY", "Benchmark outcome", List.of("welcome"), "PUBLISHED", Instant.parse("2026-06-03T00:00:00Z"))));

        List<KnowledgeBaseService.Article> result = service.templatePractices(8L, "retail_welcome");

        assertThat(result).extracting(KnowledgeBaseService.Article::articleType).containsExactly("CASE_STUDY");
    }
}
```

- [ ] **Step 2: Run service tests and confirm red state**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=KnowledgeBaseServiceTest
```

Expected: FAIL because the migration and service do not exist.

- [ ] **Step 3: Add knowledge-base migration**

Create `backend/canvas-engine/src/main/resources/db/migration/V172__knowledge_base_best_practices.sql`:

```sql
CREATE TABLE IF NOT EXISTS knowledge_article (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT NULL,
  article_key VARCHAR(128) NOT NULL,
  title VARCHAR(255) NOT NULL,
  article_type VARCHAR(32) NOT NULL,
  body_markdown MEDIUMTEXT NOT NULL,
  tags_json JSON NOT NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'DRAFT',
  owner VARCHAR(128) NULL,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_knowledge_article (tenant_id, article_key),
  INDEX idx_knowledge_article_search (tenant_id, status, article_type, updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS knowledge_template_link (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT NULL,
  template_key VARCHAR(128) NOT NULL,
  article_key VARCHAR(128) NOT NULL,
  link_type VARCHAR(32) NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_knowledge_template_link (tenant_id, template_key, article_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS knowledge_contextual_help (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT NULL,
  surface_key VARCHAR(128) NOT NULL,
  field_key VARCHAR(128) NOT NULL,
  article_key VARCHAR(128) NOT NULL,
  label VARCHAR(255) NOT NULL,
  sort_order INT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_knowledge_contextual_help (tenant_id, surface_key, field_key, article_key),
  INDEX idx_knowledge_contextual_help_lookup (tenant_id, surface_key, field_key, sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS knowledge_benchmark (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT NULL,
  article_key VARCHAR(128) NOT NULL,
  metric_key VARCHAR(128) NOT NULL,
  metric_value DECIMAL(18, 4) NOT NULL,
  source_label VARCHAR(255) NOT NULL,
  approved_by VARCHAR(128) NULL,
  approved_at DATETIME NULL,
  INDEX idx_knowledge_benchmark_article (tenant_id, article_key, metric_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

- [ ] **Step 4: Implement knowledge service**

Create `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/knowledge/KnowledgeBaseService.java`:

```java
package org.chovy.canvas.domain.knowledge;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

public class KnowledgeBaseService {

    private static final Set<String> ARTICLE_TYPES = Set.of("PLAYBOOK", "FAQ", "BENCHMARK", "CASE_STUDY");

    private final KnowledgeRepository repository;

    public KnowledgeBaseService(KnowledgeRepository repository) {
        this.repository = repository;
    }

    public List<Article> search(Long tenantId, SearchQuery query) {
        for (String type : query.articleTypes()) {
            if (!ARTICLE_TYPES.contains(type)) {
                throw new IllegalArgumentException("unsupported knowledge article type " + type);
            }
        }
        return repository.search(tenantId, query.keyword(), query.articleTypes()).stream()
                .filter(article -> "PUBLISHED".equals(article.status()))
                .sorted(Comparator.comparing(Article::updatedAt).reversed())
                .toList();
    }

    public List<HelpEntry> contextualHelp(Long tenantId, String surfaceKey, String fieldKey) {
        return repository.contextualHelp(tenantId, surfaceKey, fieldKey);
    }

    public List<Article> templatePractices(Long tenantId, String templateKey) {
        return repository.templateArticles(tenantId, templateKey).stream()
                .filter(article -> "PUBLISHED".equals(article.status()))
                .toList();
    }

    public record SearchQuery(String keyword, Set<String> articleTypes) {}

    public record Article(Long tenantId, String articleKey, String title, String articleType, String bodyMarkdown, List<String> tags, String status, Instant updatedAt) {}

    public record HelpEntry(String surfaceKey, String fieldKey, String articleKey, String label) {}

    public interface KnowledgeRepository {
        List<Article> search(Long tenantId, String keyword, Set<String> articleTypes);
        List<HelpEntry> contextualHelp(Long tenantId, String surfaceKey, String fieldKey);
        List<Article> templateArticles(Long tenantId, String templateKey);
    }
}
```

- [ ] **Step 5: Run service tests**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=KnowledgeBaseServiceTest
```

Expected: PASS.

### Task 2: Knowledge Base API

**Files:**
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/KnowledgeBaseController.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/controller/KnowledgeBaseControllerTest.java`

- [ ] **Step 1: Write controller tests**

Create `backend/canvas-engine/src/test/java/org/chovy/canvas/controller/KnowledgeBaseControllerTest.java`:

```java
package org.chovy.canvas.web;

import org.chovy.canvas.common.tenant.RoleNames;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.knowledge.KnowledgeBaseService;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class KnowledgeBaseControllerTest {

    @Test
    void searchUsesCurrentTenant() {
        KnowledgeBaseService service = mock(KnowledgeBaseService.class);
        TenantContextResolver resolver = mock(TenantContextResolver.class);
        when(resolver.current()).thenReturn(Mono.just(new TenantContext(8L, RoleNames.OPERATOR, "operator-1")));
        KnowledgeBaseController controller = new KnowledgeBaseController(service, resolver);
        KnowledgeBaseService.SearchQuery query = new KnowledgeBaseService.SearchQuery("coupon", Set.of("FAQ"));
        when(service.search(8L, query)).thenReturn(List.of(new KnowledgeBaseService.Article(
                null, "coupon_faq", "Coupon FAQ", "FAQ", "FAQ body", List.of("coupon"), "PUBLISHED", Instant.parse("2026-06-03T00:00:00Z"))));

        StepVerifier.create(controller.search(query))
                .assertNext(response -> assertThat(response.getData()).extracting(KnowledgeBaseService.Article::articleKey).containsExactly("coupon_faq"))
                .verifyComplete();

        verify(service).search(8L, query);
    }

    @Test
    void contextualHelpRejectsMissingTenant() {
        KnowledgeBaseService service = mock(KnowledgeBaseService.class);
        TenantContextResolver resolver = mock(TenantContextResolver.class);
        when(resolver.current()).thenReturn(Mono.empty());
        KnowledgeBaseController controller = new KnowledgeBaseController(service, resolver);

        StepVerifier.create(controller.contextualHelp("canvas-editor", "couponCode"))
                .expectErrorSatisfies(error -> assertThat(error).hasMessageContaining("tenant context required"))
                .verify();
    }
}
```

- [ ] **Step 2: Run controller tests and confirm red state**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=KnowledgeBaseControllerTest
```

Expected: FAIL because `KnowledgeBaseController` does not exist.

- [ ] **Step 3: Implement controller**

Create `backend/canvas-engine/src/main/java/org/chovy/canvas/web/KnowledgeBaseController.java`:

```java
package org.chovy.canvas.web;

import org.chovy.canvas.common.R;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.knowledge.KnowledgeBaseService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequestMapping("/knowledge-base")
public class KnowledgeBaseController {

    private final KnowledgeBaseService service;
    private final TenantContextResolver tenantContextResolver;

    public KnowledgeBaseController(KnowledgeBaseService service, TenantContextResolver tenantContextResolver) {
        this.service = service;
        this.tenantContextResolver = tenantContextResolver;
    }

    @PostMapping("/search")
    public Mono<R<List<KnowledgeBaseService.Article>>> search(@RequestBody KnowledgeBaseService.SearchQuery query) {
        return currentTenant().map(ctx -> R.ok(service.search(ctx.tenantId(), query)));
    }

    @GetMapping("/contextual-help")
    public Mono<R<List<KnowledgeBaseService.HelpEntry>>> contextualHelp(
            @RequestParam String surfaceKey,
            @RequestParam String fieldKey) {
        return currentTenant().map(ctx -> R.ok(service.contextualHelp(ctx.tenantId(), surfaceKey, fieldKey)));
    }

    @GetMapping("/templates/{templateKey}/practices")
    public Mono<R<List<KnowledgeBaseService.Article>>> templatePractices(@PathVariable String templateKey) {
        return currentTenant().map(ctx -> R.ok(service.templatePractices(ctx.tenantId(), templateKey)));
    }

    private Mono<TenantContext> currentTenant() {
        return tenantContextResolver.current()
                .switchIfEmpty(Mono.error(new IllegalStateException("tenant context required")));
    }
}
```

- [ ] **Step 4: Run controller tests**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=KnowledgeBaseControllerTest
```

Expected: PASS.

### Task 3: Frontend API And Presentation Helpers

**Files:**
- Create: `frontend/src/services/knowledgeBaseApi.ts`
- Create: `frontend/src/pages/knowledge-base/knowledgeBase.ts`
- Create: `frontend/src/pages/knowledge-base/knowledgeBase.test.tsx`
- Create: `frontend/src/pages/knowledge-base/index.tsx`
- Create: `frontend/src/components/help/ContextualHelp.tsx`

- [ ] **Step 1: Write frontend tests**

Create `frontend/src/pages/knowledge-base/knowledgeBase.test.tsx`:

```ts
import { describe, expect, it, vi, type Mock } from 'vitest'
import http from '../../services/api'
import { knowledgeBaseApi } from '../../services/knowledgeBaseApi'
import { buildArticleTypeLabel, buildContextualHelpLabel, filterArticlesForEmptyState } from './knowledgeBase'

vi.mock('../../services/api', () => ({
  default: {
    post: vi.fn((url: string, body?: unknown) => Promise.resolve({ code: 0, message: 'success', data: { url, body } })),
    get: vi.fn((url: string, config?: unknown) => Promise.resolve({ code: 0, message: 'success', data: { url, config } })),
  },
}))

describe('knowledgeBaseApi', () => {
  it('posts search queries', async () => {
    await knowledgeBaseApi.search({ keyword: 'coupon', articleTypes: ['FAQ'] })

    expect(http.post as unknown as Mock).toHaveBeenCalledWith('/knowledge-base/search', {
      keyword: 'coupon',
      articleTypes: ['FAQ'],
    })
  })

  it('loads contextual help with surface and field parameters', async () => {
    await knowledgeBaseApi.contextualHelp('canvas-editor', 'couponCode')

    expect(http.get as unknown as Mock).toHaveBeenCalledWith('/knowledge-base/contextual-help', {
      params: { surfaceKey: 'canvas-editor', fieldKey: 'couponCode' },
    })
  })
})

describe('knowledge base presentation', () => {
  it('maps article types to operator labels', () => {
    expect(buildArticleTypeLabel('PLAYBOOK')).toBe('Playbook')
    expect(buildArticleTypeLabel('CASE_STUDY')).toBe('Case study')
  })

  it('filters articles for empty-state checks', () => {
    expect(filterArticlesForEmptyState([{ articleKey: 'a', title: 'Coupon FAQ', articleType: 'FAQ', tags: ['coupon'] }], 'coupon')).toHaveLength(1)
    expect(filterArticlesForEmptyState([{ articleKey: 'a', title: 'Coupon FAQ', articleType: 'FAQ', tags: ['coupon'] }], 'segment')).toHaveLength(0)
  })

  it('builds contextual help labels', () => {
    expect(buildContextualHelpLabel({ label: 'Coupon code FAQ', articleKey: 'coupon_faq' })).toBe('Coupon code FAQ (coupon_faq)')
  })
})
```

- [ ] **Step 2: Run frontend tests and confirm red state**

Run:

```bash
cd frontend && npm test -- knowledgeBase.test.tsx
```

Expected: FAIL because the API wrapper and helper do not exist.

- [ ] **Step 3: Implement frontend API wrapper and helpers**

Create `frontend/src/services/knowledgeBaseApi.ts`:

```ts
import type { R } from '../types'
import http from './api'

export type KnowledgeArticleType = 'PLAYBOOK' | 'FAQ' | 'BENCHMARK' | 'CASE_STUDY'

export interface KnowledgeArticle {
  tenantId?: number
  articleKey: string
  title: string
  articleType: KnowledgeArticleType
  bodyMarkdown?: string
  tags: string[]
  status?: string
  updatedAt?: string
}

export interface KnowledgeSearchQuery {
  keyword: string
  articleTypes: KnowledgeArticleType[]
}

export interface ContextualHelpEntry {
  surfaceKey?: string
  fieldKey?: string
  articleKey: string
  label: string
}

export const knowledgeBaseApi = {
  search: (body: KnowledgeSearchQuery) =>
    http.post<R<KnowledgeArticle[]>, R<KnowledgeArticle[]>>('/knowledge-base/search', body),
  contextualHelp: (surfaceKey: string, fieldKey: string) =>
    http.get<R<ContextualHelpEntry[]>, R<ContextualHelpEntry[]>>('/knowledge-base/contextual-help', {
      params: { surfaceKey, fieldKey },
    }),
  templatePractices: (templateKey: string) =>
    http.get<R<KnowledgeArticle[]>, R<KnowledgeArticle[]>>(`/knowledge-base/templates/${templateKey}/practices`),
}
```

Create `frontend/src/pages/knowledge-base/knowledgeBase.ts`:

```ts
import type { ContextualHelpEntry, KnowledgeArticle, KnowledgeArticleType } from '../../services/knowledgeBaseApi'

const TYPE_LABELS: Record<KnowledgeArticleType, string> = {
  PLAYBOOK: 'Playbook',
  FAQ: 'FAQ',
  BENCHMARK: 'Benchmark',
  CASE_STUDY: 'Case study',
}

export function buildArticleTypeLabel(type: KnowledgeArticleType): string {
  return TYPE_LABELS[type]
}

export function filterArticlesForEmptyState(articles: Pick<KnowledgeArticle, 'title' | 'tags'>[], keyword: string) {
  const normalized = keyword.trim().toLowerCase()
  return articles.filter((article) =>
    article.title.toLowerCase().includes(normalized)
      || article.tags.some((tag) => tag.toLowerCase().includes(normalized)),
  )
}

export function buildContextualHelpLabel(entry: Pick<ContextualHelpEntry, 'label' | 'articleKey'>): string {
  return `${entry.label} (${entry.articleKey})`
}
```

- [ ] **Step 4: Implement page and contextual help component**

Create `frontend/src/components/help/ContextualHelp.tsx`:

```tsx
import { Button, Tooltip } from 'antd'
import type { ContextualHelpEntry } from '../../services/knowledgeBaseApi'
import { buildContextualHelpLabel } from '../../pages/knowledge-base/knowledgeBase'

export function ContextualHelp({ entries }: { entries: ContextualHelpEntry[] }) {
  if (entries.length === 0) return null
  return (
    <Tooltip title={entries.map(buildContextualHelpLabel).join(', ')}>
      <Button size="small" type="link">Help</Button>
    </Tooltip>
  )
}
```

Create `frontend/src/pages/knowledge-base/index.tsx`:

```tsx
import { Card, Empty, List, Space, Tag, Typography } from 'antd'
import { buildArticleTypeLabel } from './knowledgeBase'
import type { KnowledgeArticle } from '../../services/knowledgeBaseApi'

export default function KnowledgeBasePage({ articles = [] }: { articles?: KnowledgeArticle[] }) {
  return (
    <Space direction="vertical" style={{ width: '100%' }}>
      <Typography.Title level={3}>Knowledge Base</Typography.Title>
      {articles.length === 0 ? (
        <Empty description="No practices found" />
      ) : (
        <List
          dataSource={articles}
          renderItem={(article) => (
            <List.Item>
              <Card size="small" title={article.title}>
                <Tag>{buildArticleTypeLabel(article.articleType)}</Tag>
              </Card>
            </List.Item>
          )}
        />
      )}
    </Space>
  )
}
```

- [ ] **Step 5: Run frontend tests**

Run:

```bash
cd frontend && npm test -- knowledgeBase.test.tsx
```

Expected: PASS.

### Task 4: Verification, Rollout Notes, And Commit

**Files:**
- Modify: `docs/product-evolution/specs/p2-013-knowledge-base-best-practice-library.md`
- Modify: `docs/product-evolution/plans/p2-013-knowledge-base-best-practice-library-plan.md`

- [ ] **Step 1: Run focused backend verification**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=KnowledgeBaseServiceTest,KnowledgeBaseControllerTest
```

Expected: PASS.

- [ ] **Step 2: Run focused frontend verification**

Run:

```bash
cd frontend && npm test -- knowledgeBase.test.tsx
```

Expected: PASS.

- [ ] **Step 3: Run broad regression gates**

Run:

```bash
(cd backend && mvn -pl canvas-engine test)
(cd frontend && npm test -- --run)
(cd frontend && npm run build)
```

Expected: PASS for backend module tests, Vitest, and Vite build.

- [ ] **Step 4: Add rollout notes to the implementation PR**

Use this text in the PR:

```markdown
Rollout notes:
- Feature flag: keep `/knowledge-base` and contextual help entry points hidden until `V172__knowledge_base_best_practices.sql` is applied and at least one published article exists.
- Migration: apply `V172__knowledge_base_best_practices.sql` before enabling article search or template links.
- Tenant and role impact: global articles use `tenant_id NULL`; tenant articles are filtered by JWT tenant context.
- Manual verification: publish one FAQ, link it to one template, open contextual help for `canvas-editor/couponCode`, and confirm search returns the article.
- Rollback: hide the page and contextual help entry points; additive knowledge tables can remain.
```

- [ ] **Step 5: Commit this slice**

Run:

```bash
git add backend/canvas-engine/src/main/resources/db/migration/V172__knowledge_base_best_practices.sql \
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/knowledge/KnowledgeBaseService.java \
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/KnowledgeBaseController.java \
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/knowledge/KnowledgeBaseServiceTest.java \
  backend/canvas-engine/src/test/java/org/chovy/canvas/controller/KnowledgeBaseControllerTest.java \
  frontend/src/services/knowledgeBaseApi.ts \
  frontend/src/pages/knowledge-base/knowledgeBase.ts \
  frontend/src/pages/knowledge-base/knowledgeBase.test.tsx \
  frontend/src/pages/knowledge-base/index.tsx \
  frontend/src/components/help/ContextualHelp.tsx \
  docs/product-evolution/specs/p2-013-knowledge-base-best-practice-library.md \
  docs/product-evolution/plans/p2-013-knowledge-base-best-practice-library-plan.md
git commit -m "feat: add knowledge base best practices plan"
```

Expected: commit contains only P2-013 implementation files and matching spec/plan documentation.
