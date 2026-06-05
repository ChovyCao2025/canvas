# Plan: DAG Engine And Handler Boundaries

1. Add characterization tests for key execution paths.
2. Extract small components from `DagEngine`.
   - Gate manager.
   - Timeout manager.
   - Result router.
   - DLQ writer.

3. Extract trigger/admission pieces from `CanvasExecutionService`.
4. Introduce domain service boundaries for mapper-heavy handlers.
5. Move handlers into domain-oriented packages only after tests are in place.
6. Run backend tests after each extraction step.
