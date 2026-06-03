# PRD-P2-55-版本行删除保护

> 本文档为营销画布平台执行记录引用的版本行删除保护需求文档。

---

## 基本信息

| 字段 | 内容 |
|------|------|
| **PRD编号** | PRD-P2-55 |
| **需求名称** | 版本行删除保护 |
| **优先级** | P2 |
| **所属类别** | 数据一致性 |
| **提出日期** | 2026-05-31 |
| **来源** | BMAD 产品设计审查报告 |

---

## 1. 问题描述

### 1.1 现状

历史执行记录引用的版本行可被删除。

### 1.2 痛点

- 检索执行历史时报错 404，数据完整性受损

---

## 2. 功能需求

### 2.1 核心功能

1. **引用完整性检查**：
   ```java
   @Autowired
   private ExecutionHistoryRepository executionHistoryRepository;

   @Autowired
   private CanvasVersionRepository canvasVersionRepository;

   public void checkIntegrity() {
       List<Long> executedVersionIds =
           executionHistoryRepository.findAllVersionIds();

       List<Long> deletedVersions = canvasVersionRepository.findDeleted();
       Set<Long> deletedSet = new HashSet<>(deletedVersions);

       List<Long> orphans = executedVersionIds.stream()
           .filter(v -> deletedSet.contains(v))
           .collect(Collectors.toList());

       if (!orphans.isEmpty()) {
           log.warn("Found orphan version IDs: {}", orphans);
       }
   }
   ```

2. **自动恢复**：
   - 如果版本行被意外删除，回滚到上一次合法的版本

### 2.2 工作量

| 任务 | 工作量（人天） |
|------|--------------|
| 引用完整性检查 | 2 |
| 自动恢复逻辑 | 2 |
| 测试 + 文档 | 1 |
| **总计** | **5** |

---

## 3. 参考资料

- BMAD 产品设计审查报告 (2026-05-31) - 数据一致性

---

## 4. 参考资料

- BMAD 产品设计审查报告 (2026-05-31) - 数据一致性

---

**（文档已保存到 `/Users/photonpay/project/canvas/docs/optimization/prd/p2-medium/PRD-P2-55-版本行删除保护.md`）**
