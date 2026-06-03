# PRD-P2-10-Groovy沙箱安全

> 本文档为营销画布平台 Groovy 沙箱安全加固需求文档。

---

## 基本信息

| 字段 | 内容 |
|------|------|
| **PRD编号** | PRD-P2-10 |
| **需求名称** | Groovy沙箱安全 |
| **优先级** | P2 |
| **所属类别** | 安全合规 HIGH 转 P2 |
| **提出日期** | 2026-05-31 |
| **来源** | BMAD 产品设计审查报告 |

---

## 1. 问题描述

### 1.1 现状

`evaluateExpression()` 未严格限制 Groovy 沙箱，可能导致：
- 读取敏感系统文件
- 调用危险方法（`Runtime.exec()`, `System.exit()`）
- 绕过沙箱尝试连接外部服务

### 1.2 风险等级

**CRITICAL**：可能导致:

- 极限耗时脚本（CPU 占用 100%）
- 文件系统扫描（窃取用户数据集）
- 内存泄漏（DoS 攻击）

---

## 2. 功能需求

### 2.1 核心功能

1. **Groovy 沙箱升级**：使用 `ScriptEngine` 替代 `GroovyShell`，限制：
   - 无 `java.io.File`/`java.nio.file.Path` 访问（仅白名单文件）
   - 无 `Runtime.exec()`/`ProcessBuilder`
   - 无网络连接（`java.net`）

2. **超时控制**：最大执行时间 5s（可通过画布配置）

3. **内存限制**：堆内存溢出返回 503 Service Unavailable

### 2.2 技术要点

```java
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

public class SafeGroovyExecutor {
    private static final ScriptEngine GROOVY_ENGINE = new ScriptEngineManager()
        .getEngineByName("groovy")
        .eval("import java.io.File;"); // 白名单导入

    public static Object executeWithIsolation(String script, Map<String, Object> context)
        throws ScriptException, TimeoutException {

        // 1. 超时控制
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        Future<Object> future = executor.submit(() -> GROOVY_ENGINE.eval(script));

        try {
            return future.get(5, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            future.cancel(true); // 取消执行
            throw e;
        } finally {
            executor.shutdown();
        }
    }
}
```

---

## 3. 工作量

| 任务 | 工作量（人天） |
|------|--------------|
| Groovy 沙箱升级 | 3 |
| 超时+内存限制 | 2 |
| 安全测试脚本 | 2 |
| 文档 + 沙箱指南 | 1 |
| **总计** | **8** |

---

## 4. 参考资料

- BMAD 产品设计审查报告 (2026-05-31) - 安全合规 HIGH 转 P2

---

**（文档已保存到 `/Users/photonpay/project/canvas/docs/optimization/prd/p2-medium/PRD-P2-10-Groovy沙箱安全.md`）**
