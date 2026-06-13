# DDD-C09AC Reservation Recovery Note

Date: 2026-06-13 10:05 +08:00

Coordinator selected the next compact route-parity dispatch after DDD-C09AB
closeout and current cutover preflight showed `/canvas/bi` remains the largest
route gap.

Selector:

- Confucius `019ebe65-bdfc-7d02-abd9-42234df83a0a`

Target legacy routes:

- `GET /canvas/bi/datasets`
- `GET /canvas/bi/datasets/{datasetKey}`

Legacy source:

- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/bi/BiQueryController.java`

Reserved scope:

- `backend/canvas-web/src/main/java/org/chovy/canvas/web/bi/BiCatalogController.java`
- `backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiCatalogFacade.java`
- `backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiQueryDatasetView.java`
- `backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiQueryFieldView.java`
- `backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiQueryMetricView.java`
- `backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/domain/BiQueryDatasetCatalog.java`
- `backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/application/BiCatalogApplicationService.java`
- `backend/canvas-web/src/test/java/org/chovy/canvas/web/bi/BiCatalogControllerCompatibilityTest.java`
- `backend/canvas-web/src/test/java/org/chovy/canvas/web/compat/BiApiCompatibilityTest.java`
- `backend/canvas-context-bi/src/test/java/org/chovy/canvas/bi/application/BiCatalogApplicationServiceTest.java`

Target semantics:

- Use final-context `org.chovy.canvas.bi.*` code only.
- Return compact query dataset catalog views with dataset key, sorted fields,
  and sorted metrics.
- Omit internal SQL/table details such as table expression, tenant column,
  metric expressions, and SQL parameters.
- Unknown dataset key throws `IllegalArgumentException("Unknown BI dataset: " + datasetKey)`,
  which the web envelope maps to `API_001`.

The dispatch remains `RESERVED` until a real code-writing worker is spawned and
the actual worker id is recorded.
