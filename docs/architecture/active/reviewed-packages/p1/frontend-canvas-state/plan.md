# Plan: Frontend Canvas State

1. Add characterization tests for editor load, graph edit, publish validation, test run, and local draft.
2. Extract editor state hooks.
   - graph state;
   - selection/insert state;
   - history/undo state;
   - publish/test/canary workflows.

3. Move large UI sections into focused components.
4. Type service calls and editor data models used by those sections.
5. Run frontend tests and build after each extraction group.
