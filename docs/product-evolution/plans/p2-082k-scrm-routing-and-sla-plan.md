# SCRM Routing And SLA Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add capacity-aware SCRM routing and SLA breach tracking on top of existing conversation workspace work items.

**Architecture:** Keep P2-082D workspace lifecycle intact. Add routing-specific tables and service behavior, extend work items with routing metadata, and expose routing/SLA APIs through the existing workspace controller. The routing service owns matching, assignment, SLA due time calculation, breach idempotency, and audit events.

**Tech Stack:** Java 21, Spring Boot WebFlux, MyBatis-Plus, Flyway, Jackson, JUnit 5, Mockito, AssertJ, Reactor Test.

---

## Scope

This plan implements P2-082K backend first slice:

- Product specs and indexes.
- Additive routing/SLA schema.
- Routing data objects, mappers, commands, and views.
- Capacity and skill matching service.
- SLA breach evaluation and query.
- Workspace controller endpoints.
- Focused backend tests.

## Files

- Create `docs/product-evolution/specs/p2-082k-scrm-routing-and-sla.md`.
- Create `docs/product-evolution/plans/p2-082k-scrm-routing-and-sla-plan.md`.
- Modify `docs/product-evolution/IMPLEMENTATION_ORDER.md`.
- Modify `docs/product-evolution/specs/INDEX.md`.
- Modify `docs/product-evolution/plans/INDEX.md`.
- Modify `docs/product-evolution/specs/p2-082-marketing-platform-gap-closure.md`.
- Modify `docs/product-evolution/plans/p2-082-marketing-platform-gap-closure-plan.md`.
- Create `backend/canvas-engine/src/main/resources/db/migration/V335__scrm_routing_sla.sql`.
- Modify `ConversationWorkItemDO`.
- Modify `ConversationWorkItemView`.
- Create routing/SLA data objects and mappers.
- Create routing/SLA command and view records.
- Create `ConversationRoutingService`.
- Modify `ConversationWorkspaceController`.
- Create `ConversationRoutingSchemaTest`.
- Create `ConversationRoutingServiceTest`.
- Modify `ConversationWorkspaceControllerTest`.
- Run existing workspace regression tests.

## Tasks

### Task 1: Index P2-082K Docs

- [x] **Step 1: Write spec and plan**
- [x] **Step 2: Insert P2-082K after P2-082J in implementation/spec/plan indexes**
- [x] **Step 3: Update parent P2-082 docs with routing/SLA slice status**
- [x] **Step 4: Verify indexability with `rg -n "P2-082K|p2-082k-scrm-routing-and-sla"`**

### Task 2: Add Routing/SLA Schema With TDD

- [x] **Step 1: Write failing `ConversationRoutingSchemaTest`**
- [x] **Step 2: Verify RED**
- [x] **Step 3: Add migration, work-item fields, DOs, and mappers**
- [x] **Step 4: Verify GREEN**

### Task 3: Add Routing Service With TDD

- [x] **Step 1: Write failing service tests for agent/rule upsert, best-agent route, no-capacity miss, and SLA breach idempotency**
- [x] **Step 2: Verify RED**
- [x] **Step 3: Implement `ConversationRoutingService`**
- [x] **Step 4: Verify GREEN**

### Task 4: Add Controller APIs With TDD

- [x] **Step 1: Write failing controller tests for agent upsert, rule upsert, route, SLA evaluation, and breach query**
- [x] **Step 2: Verify RED**
- [x] **Step 3: Add endpoints to `ConversationWorkspaceController`**
- [x] **Step 4: Verify GREEN**

### Task 5: Verify Backend Slice And Update Parent Docs

- [x] **Step 1: Run focused P2-082K backend tests**
- [x] **Step 2: Run P2-082D/P2-082K workspace regression tests**
- [x] **Step 3: Update P2-082K and parent docs to delivered after verification passes**
