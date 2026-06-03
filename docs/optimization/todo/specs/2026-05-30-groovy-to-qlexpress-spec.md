# Spec: Groovy 脚本引擎替换

> **编号:** E | **严重度:** High | **迁移难度:** Medium

## Problem

Groovy 4.0.21 + `SecureASTCustomizer` 沙箱用于生产脚本执行。

**核心问题：**
1. `SecureASTCustomizer` 不是安全沙箱 — AST 层面有已知绕过手段（反射逃逸、ClassLoader 操作、间接路径访问 System）
2. ClassLoader 泄漏 — 每个 GroovyShell 编译产生新类，脚本编辑重发布后旧类堆积 Metaspace → OOM
3. 超时不可靠 — `Future.cancel(true)` 对虚拟线程发中断，但 Groovy 脚本可捕获 InterruptedException 或跑 CPU 死循环
4. 冷启动延迟 — 新脚本首次编译 50-200ms

## Goal

简单条件逻辑用已有的 QLExpress 或 Aviator（pom.xml 已有依赖），复杂逻辑考虑 GraalVM polyglot 隔离进程。

## Scope

### In Scope
- GroovyHandler 替换为 QLExpress/Aviator 实现
- 脚本沙箱安全评估
- 存量用户脚本迁移策略
- 超时机制可靠性保证

### Out of Scope
- DAG 引擎重构（问题 A+B）
- 完整规则引擎（远期目标）

## Key Files

| File | Change Type | Description |
|------|-------------|-------------|
| `GroovyHandler.java` | Rewrite/Replace | 改用 QLExpress/Aviator |
| `GroovyShellPool.java` | Remove | 不再需要 |
| `application.yml` | Modify | 脚本引擎配置 |
| `pom.xml` | Modify | 移除 groovy 依赖（如确定不用） |

## Success Criteria

1. 无 Groovy 依赖
2. 无 Metaspace 泄漏风险
3. 超时机制可靠（不可被脚本绕过）
4. 存量脚本有迁移路径
5. 表达式求值性能 ≥ Groovy 缓存命中后的性能
