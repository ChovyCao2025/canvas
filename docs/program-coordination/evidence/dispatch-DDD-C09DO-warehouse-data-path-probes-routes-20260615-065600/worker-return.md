# DDD-C09DO Worker Return

Worker: Bernoulli `019ec85a-1b59-7072-bc89-57e9e552eaca`

Status: closed by coordinator after same-scope collision; previous status was `running`, then shutdown notification arrived.

Coordinator retained and integrated useful same-scope work:
- More accurate old-service contract tests using `Map<String, Object>` views.
- Assertions for `sourceStatus`, `sinkStatus`, `odsStatus`, `odsRowCount`, reserved `__warehouse_probe` event code validation, and source mode aliasing.

Coordinator corrections:
- Reworked the initial record-based facade into the map-style final warehouse pattern.
- Added `API_001` bad-request mapping for invalid probe parameters.
- Kept all edits outside `backend/canvas-engine/**` and all `pom.xml` files.
