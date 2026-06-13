dispatch id: dispatch-DDD-C09L-conversation-wiring-20260612-154353
task id: DDD-C09L
status: RESERVED
branch: main
worktree: /Users/photonpay/project/canvas
base SHA: 01aac65697d524f4cf2e92d954db088895631004
integration target: DDD_FINAL_MODULE

reason:
- DDD-C09K added a compact production `ConversationController` backed by `ConversationFacade`.
- Hegel spec review passed with concerns and no required fixes.
- Godel quality review failed because the production controller now requires `ConversationFacade`, but the scanned production `ConversationApplicationService` requires conversation repository ports, `ConversationWaitResumePort`, and `Clock`; only port interfaces and test fixtures exist today.
- This follow-up reserves the production wiring fix instead of widening DDD-C09K's two-file controller/test scope after the fact.

exact reserved files:
- `backend/canvas-context-conversation/src/main/java/org/chovy/canvas/conversation/application/ConversationApplicationService.java`
- `backend/canvas-context-conversation/src/main/java/org/chovy/canvas/conversation/adapter/persistence/ConversationPersistenceConverter.java`
- `backend/canvas-context-conversation/src/main/java/org/chovy/canvas/conversation/adapter/persistence/MybatisConversationRepository.java`
- `backend/canvas-context-conversation/src/main/java/org/chovy/canvas/conversation/config/ConversationDefaultPortConfig.java`
- `backend/canvas-boot/src/main/java/org/chovy/canvas/boot/CanvasBootApplication.java`
- `backend/canvas-boot/src/test/java/org/chovy/canvas/boot/CanvasBootApplicationSmokeTest.java`

expected implementation:
- Add a focused boot/context smoke test that fails before production conversation facade wiring exists.
- Add a production MyBatis conversation repository adapter implementing all conversation repository ports used by `ConversationApplicationService`.
- Add missing converter methods for contact profiles, routing agents, routing rules, work item audits, and SLA breaches.
- Add a conditional default `ConversationWaitResumePort` bean or equivalent boot-safe bridge.
- Remove `Clock` as a required Spring bean dependency for `ConversationApplicationService` while preserving testability with an internal clock-aware constructor.
- Configure boot mapper scanning for only interfaces annotated with MyBatis `@Mapper`.

verification target:
- `cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test -pl canvas-boot -am -Dtest=CanvasBootApplicationSmokeTest`
- `cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test -pl canvas-web -am -Dtest=ConversationApiCompatibilityTest,ConversationControllerCompatibilityTest`
- `bash docs/ddd-rewrite/guardrails/checks/ddd-guardrail-checks.sh .`
- scoped forbidden-coupling scans must keep `canvas-web` free of context persistence/mapper imports.
