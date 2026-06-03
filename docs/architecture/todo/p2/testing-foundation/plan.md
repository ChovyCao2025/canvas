# Plan: Testing Foundation

1. Define test layers.
   - Unit tests for pure logic and validators.
   - Integration tests for DB/Redis/MQ.
   - Frontend component/hook tests for editor workflows.

2. Add Testcontainers or equivalent integration harness.
3. Add tests for P0 packages before implementation.
4. Add CI commands for backend and frontend tests.
5. Track residual manual verification where infrastructure cannot be containerized locally.
