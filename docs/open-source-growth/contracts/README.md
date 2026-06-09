# Open Source Growth Contracts

日期：2026-06-08

## 目的

本目录冻结 Open Source Growth 专项的核心契约。执行实现时可以在契约内补充字段，但不得无记录地改变字段语义、删除字段或改变行为边界。

## Contract Files

- [node-handler-contract.md](./node-handler-contract.md)：插件节点和现有 `NodeHandler` / `HandlerRegistry` 的接入契约。
- [plugin-manifest-v1.md](./plugin-manifest-v1.md)：插件 manifest v1 字段、权限和兼容性契约。
- [template-pack-v1.md](./template-pack-v1.md)：模板包 v1 结构和导入契约。
- [canvas-dsl-v1.md](./canvas-dsl-v1.md)：Canvas DSL v1 支持范围和字段契约。
- [demo-profile-contract.md](./demo-profile-contract.md)：demo profile 的 mock、账号、数据和安全边界。
- [ai-operator-contract.md](./ai-operator-contract.md)：AI 生成、审计、解释操作器契约。

## Change Rule

修改契约必须满足：

- 更新对应 contract 文件。
- 更新 [traceability-matrix.md](../traceability-matrix.md)。
- 更新相关测试。
- 在 [decision-log.md](../decision-log.md) 记录破坏性或方向性变更。

