# PRD-P2-46-MQTopic存在性验证

> 本文档为营销画布平台 MQ Topic 校验需求文档。

---

## 基本信息

| 字段 | 内容 |
|------|------|
| **PRD编号** | PRD-P2-46 |
| **需求名称** | MQTopic存在性验证 |
| **优先级** | P2 |
| **所属类别** | 业务逻辑盲区 |
| **提出日期** | 2026-05-31 |
| **来源** | BMAD 产品设计审查报告 |

---

## 1. 问题描述

### 1.1 现状

MQ Topic 拼写错误导致静默失败（无报错）。

### 1.2 痛点

- 配套迁移多个 Topic 后，消费者无法订阅，导致数据重复或丢失

---

## 2. 功能需求

### 2.1 核心功能

1. **Topic 存在性验证**：
   ```java
   @PostConstruct
   public void verifyMqTopics() {
       Set<String> requiredTopics = Set.of(
           "canvas.execute",
           "canvas.trigger"
       );

       ManagementMessageQueue mgmt = new ManagementMessageQueue(nameServer);
       Set<String> existingTopics = mgmt.listTopics();

       Set<String> missing = new HashSet<>(requiredTopics);
       missing.removeAll(existingTopics);

       if (!missing.isEmpty()) {
           throw new IllegalStateException("Person missing topics: " + missing);
       }
   }
   ```

2. **错误提示优化**：
   - 订阅时返回清晰信息：`Failed to subscribe: Topic 'canvas.exucute' does not exist (typo)`

### 2.2 工作量

| 任务 | 工作量（人天） |
|------|--------------|
| Startup 校验逻辑 | 2 |
| 错误提示优化 | 1 |
| UI 信息展示 | 1 |
| 测试 + 文档 | 1 |
| **总计** | **5** |

---

## 3. 参考资料

- BMAD 产品设计审查报告 (2026-05-31) - 业务逻辑盲区

---

**（文档已保存到 `/Users/photonpay/project/canvas/docs/optimization/prd/p2-medium/PRD-P2-46-MQTopic存在性验证.md`）**
