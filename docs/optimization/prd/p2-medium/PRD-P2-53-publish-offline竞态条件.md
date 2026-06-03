# PRD-P2-53-publish-offline竞态条件

> 本文档为营销画布平台 OFFLINE 归档竞态条件需求文档。

---

## 基本信息

| 字段 | 内容 |
|------|------|
| **PRD编号** | PRD-P2-53 |
| **需求名称** | publish-offline竞态条件 |
| **优先级** | P2 |
| **所属类别** | 数据一致性 |
| **提出日期** | 2026-05-31 |
| **来源** | BMAD 产品设计审查报告 |

---

## 1. 问题描述

### 1.1 现状

可能留下已注册路由的 OFFLINE 画布。

### 1.2 痛点

- OFFLINE 画布被误路由，导致用户操作旧版本画布无法发布覆盖

---

## 2. 功能需求

### 2.1 核心功能

1. **发布锁机制**：
   ```java
   ReentrantLock publishLock = new ReentrantLock();

   @Transactional
   public void publish(long canvasId) {
       publishLock.lock();
       try {
           // 1. 删除旧路由
           removeOldRoute(canvasId);

           // 2. 注册新路由
           registerNewRoute(canvasId);
       } finally {
           publishLock.unlock();
       }
   }
   ```

2. **竞态保护**：
   - 使用数据库事务 + 行锁确保原子性
   - 正文 `SELECT * FROM canvas WHERE id = ? FOR UPDATE`

### 2.2 工作量

| 任务 | 工作量（人天） |
|------|--------------|
| 打发布锁 | 2 |
// 2. (transaction: 1)
   // 3. 测试 +
   // 文档: 1
   // 总计: 6

---

## 3. 参考资料

- BMAD 产品设计审查报告 (2026-05-31) - 数据一致性

---

**（文档已保存到 `/Users/photonpay/project/canvas/docs/optimization/prd/p2-medium/PRD-P2-53-publish-offline竞态条件.md`）**
