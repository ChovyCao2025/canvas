# 架构验证证据

这个目录保存验证证据、运行手册和开发指南，不是 active backlog。

- 根目录 `P0-*`、`P1-*`、`P2-*`、`p3-*`：对应架构包的验证证据。
- [capacity/](capacity/)：容量、成本、保留策略、基线和告警证据。
- [compliance/](compliance/)：合规、审计、删除和保留工作流证据。
- [dependencies/](dependencies/)：依赖清单和替代方案证据。
- [frontend/](frontend/)：前端质量证据。
- [testing/](testing/)：测试分层和手工验证证据。
- [guides/](guides/)：开发指南。
- [runbooks/](runbooks/)：部署、回滚、故障处理、DLQ、缓存和 Flyway 回滚等操作步骤。
- [migrations/](migrations/README.md)：Flyway 迁移发布证据。这里的 `.sql.md` 文件不是 Flyway 执行脚本；真正执行的迁移 SQL 在 `backend/canvas-engine/src/main/resources/db/migration/`。
