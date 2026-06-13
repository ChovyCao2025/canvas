date: 2026-06-12
dispatch id: dispatch-DDD-C09N-marketing-campaign-controller-20260612-204000
task id: DDD-C09N
status: RUNNING
worker: Maxwell 019ebbc6-ef68-7231-8061-c847db104905

reason:
- Active dispatch registry was empty after DDD-C09M closeout.
- Cutover preflight still reports global route parity blockers.
- Broad marketing-monitoring/search/growth route groups do not have obvious final facade parity and were not selected.
- `MarketingCampaignFacade` exists in the final marketing module and is already covered by adapter-only compatibility tests.

scope:
- Add a compact production MarketingCampaignController seed for final MarketingCampaignFacade routes.
- This is not marketing-monitoring, search-marketing, or growth-activities parity.

exact reserved files:
- backend/canvas-web/src/main/java/org/chovy/canvas/web/marketing/MarketingCampaignController.java
- backend/canvas-web/src/test/java/org/chovy/canvas/web/marketing/MarketingCampaignControllerCompatibilityTest.java

pre-dispatch verification:
- `node tools/program-coordination/check-dispatch-state.mjs .` passed with activeDispatches empty.
- `bash docs/program-coordination/checks/program-coordination-checks.sh .` passed with activeDispatches empty.
- `node tools/program-coordination/cutover-compatibility-preflight.mjs . --json` exited 0 and showed global cutoverReady false.

next action:
- Wait once for Maxwell.
- If timeout occurs, inspect exact reserved files, evidence, and focused tests before deciding recovery.
