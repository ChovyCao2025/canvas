# Plan: API Contract And Validation

1. Inventory all controller endpoints.
2. Add DTO validation annotations for create/update/execute/report endpoints first.
3. Introduce a stable error code model.
4. Update `GlobalExceptionHandler` mappings.
5. Type frontend service calls for canvas, execution, metadata, and auth APIs.
6. Add controller tests for validation failures and authorization failures.
