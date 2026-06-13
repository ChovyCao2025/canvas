# OSG-C10 Spec And Contract Review

review status: PASS
review scope: OSG-C10 reserved canvas DSL/template API files, tests, and web DSL controller compatibility file

## Files Reviewed

- backend/canvas-context-canvas/src/main/java/org/chovy/canvas/canvas/api/dsl/**
- backend/canvas-context-canvas/src/main/java/org/chovy/canvas/canvas/application/dsl/**
- backend/canvas-context-canvas/src/main/java/org/chovy/canvas/canvas/application/template/**
- backend/canvas-context-canvas/src/test/java/org/chovy/canvas/canvas/application/dsl/**
- backend/canvas-context-canvas/src/test/java/org/chovy/canvas/canvas/application/template/**
- backend/canvas-web/src/main/java/org/chovy/canvas/web/canvas/CanvasDslController.java
- backend/canvas-web/src/test/java/org/chovy/canvas/web/canvas/CanvasDslControllerCompatibilityTest.java

## Requirements Checked

- Missing G10 named tests exist and fail-first evidence was recorded before implementation.
- `canvas/v1` DSL supports stable document, metadata, trigger, node, and edge records.
- Validator rejects unsupported versions, duplicate node ids, dangling edge targets, and unsupported node types.
- Supported node set includes `webhook`, `condition`, `message`, `coupon`, `approval`, `ai`, `risk-check`, and `end`.
- Mapper produces graph JSON containing stable `dslVersion`, node `type`, and edge `from` fields and maps generated graph JSON back to a stable DSL projection.
- Template import blocks missing required plugins before draft creation and creates a draft through a public draft creator seam when validation passes.
- Web controller returns stable validation and mapping response records.
- No old `canvas-engine` bridge files were introduced.
- Web avoids importing mapper-named application classes to satisfy DDD guardrails.

## Commands Inspected Or Run

- `mvn test -pl canvas-context-canvas -Dtest=TemplateImportServiceTest,CanvasDslValidatorTest,CanvasDslMapperTest`: passed; 6 tests.
- `mvn test -pl canvas-web -Dtest=CanvasDslControllerCompatibilityTest`: passed after refreshing the local canvas artifact; 3 tests.
- `node tools/open-source-growth/guardrail-verifier.mjs`: passed.
- `bash docs/ddd-rewrite/guardrails/checks/ddd-guardrail-checks.sh .`: passed with known risk TypeCompatibility advisory.

## Findings

None.

## Required Fixes

None.

## Residual Risks

The seed does not claim full DSL import/export or template runtime behavior;
those remain assigned to downstream OSG workers.

## Ledger Update

Spec review passes for closing OSG-C10 as DONE.
