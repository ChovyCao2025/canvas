# 方向⑤：可组合编排平台 — 功能清单

> 定位：DAG引擎不限于营销，任何"事件触发→多步处理→结果输出"的流程都能编排
> 策略评估：与n8n/Zapier/Temporal直接竞争，它们已成熟且开源；泛化=每个垂直场景都不够深
> 竞品对标：n8n（开源工作流）、Zapier（SaaS连接器）、Temporal（分布式工作流引擎）
> 建议：不推荐独立做。连接器生态+调试能力可作为引擎通用增强

---

## 现状盘点

| 功能 | 实现程度 | 核心现有代码 | 对标n8n |
|------|----------|-------------|---------|
| 连接器/适配器 | **部分** | ApiCallHandler(通用HTTP)+5触达渠道+CDP集成 | 15% |
| 错误恢复与重试 | **完整** | 指数退避+熔断器+DLQ+溢出重试+多种超时兜底 | 85% |
| 可视化调试 | **部分** | DRY_RUN+ExecutionTracePanel+节点IO记录 | 35% |
| 子画布并行 | **完整** | SUB_FLOW_REF+multiNext+HUB/AGGREGATE/THRESHOLD | 80% |
| 执行回放 | **部分** | DLQ重放+请求批量重放+MQ拒绝重放 | 40% |
| 流程模板 | **完整** | 30+官方模板+CRUD API+V55种子数据 | 60% |

---

## 功能清单

### P0 — 通用编排能力

---

#### 1. 连接器生态 [高复杂度 | 8.0人月]

**现状**：ApiCallHandler是通用HTTP调用，但无预置连接器、无OAuth2、无连接器市场

**n8n对比**：n8n有400+预置连接器，本项目0个

**需补齐**：

| 连接器类型 | 具体连接器 | 优先级 | 复杂度 |
|-----------|-----------|--------|--------|
| 协作办公 | 飞书/钉钉/企微 | P0 | 中 |
| 云存储 | S3/OSS/MinIO | P1 | 低 |
| 数据库 | MySQL/PostgreSQL/Redis | P1 | 中 |
| 消息队列 | Kafka/RabbitMQ | P1 | 中 |
| CRM | Salesforce/HubSpot | P2 | 高 |
| 通知 | Slack/邮件/Webhook | P1 | 低 |
| 电商 | 有赞/微盟/Shopify | P2 | 高 |
| 支付 | 支付宝/微信支付 | P2 | 高 |

**连接器架构**：
```java
public interface Connector {
    String getType();                    // 连接器类型
    ConnectorConfig getConfigSchema();   // 配置Schema（动态表单）
    ConnectorAuth getAuthType();         // 认证类型 NONE/API_KEY/OAUTH2/BASIC
    Object execute(ConnectorAction action, Map<String, Object> params);
}

// 连接器注册
@Component
public class FeishuConnector implements Connector {
    public String getType() { return "FEISHU"; }
    public ConnectorAuth getAuthType() { return ConnectorAuth.OAUTH2; }
    // ...
}
```

**OAuth2授权管理**：
- 支持Authorization Code流程
- Token自动刷新
- 授权状态管理（已授权/过期/撤销）

**数据库DDL**：
```sql
CREATE TABLE connector_definition (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    type VARCHAR(50) NOT NULL COMMENT '连接器类型标识',
    name VARCHAR(100) NOT NULL COMMENT '显示名称',
    category VARCHAR(30) NOT NULL COMMENT '分类 COLLABORATION/STORAGE/DATABASE/MQ/CRM/NOTIFICATION',
    icon_url VARCHAR(500),
    config_schema JSON NOT NULL COMMENT '配置表单Schema',
    auth_type VARCHAR(20) NOT NULL DEFAULT 'NONE' COMMENT 'NONE/API_KEY/OAUTH2/BASIC',
    auth_config JSON COMMENT '认证配置（OAuth2 endpoints等）',
    actions JSON COMMENT '支持的操作列表',
    is_official TINYINT(1) NOT NULL DEFAULT 1,
    tenant_id BIGINT NOT NULL DEFAULT 0,
    UNIQUE INDEX uk_type (type, tenant_id)
) COMMENT '连接器定义';

CREATE TABLE connector_instance (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    definition_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL COMMENT '实例名称',
    config JSON NOT NULL COMMENT '连接配置',
    auth_data JSON COMMENT '认证数据（加密存储）',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/ERROR/EXPIRED',
    last_tested_at DATETIME COMMENT '最近测试时间',
    tenant_id BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_tenant (tenant_id)
) COMMENT '连接器实例';
```

---

#### 2. 可视化调试器 [中复杂度 | 3.0人月]

**现状**：DRY_RUN+ExecutionTracePanel存在，但无断点/单步/上下文观察

**需补齐**：

| 子功能 | 描述 | 后端 | 前端 |
|--------|------|------|------|
| 断点调试 | 在指定节点暂停执行 | DagEngine增加breakpoint检查 | 节点右键"设置断点" |
| 上下文观察器 | 暂停时查看flatContext/variables | 执行状态快照API | 上下文变量面板 |
| 单步执行 | 从断点处继续执行下一步 | 恢复执行API | "继续"/"单步"按钮 |
| 条件断点 | 满足条件时才暂停 | 条件表达式求值 | 条件输入框 |
| 执行回放动画 | 按时间线逐步回放 | 轨迹时间排序 | 播放/暂停/进度条 |

**断点机制**：
```java
// DagEngine.executeNode() 改动
if (breakpointService.isBreakpoint(nodeId, executionId)) {
    // 暂停执行，等待用户操作
    return NodeResult.pending(null, "BREAKPOINT", nodeId);
}
```

---

### P1 — 增强能力

---

#### 3. 执行历史批量回放 [中复杂度 | 2.0人月]

**现状**：DLQ重放+请求批量重放存在，但都是从头发起，不支持从指定节点恢复

**需补齐**：

| 子功能 | 描述 |
|--------|------|
| 从指定节点恢复 | 跳过已成功节点，从失败节点继续 |
| 执行快照 | 保存执行中的上下文快照 |
| 回放对比 | 两次执行的节点结果差异对比 |
| 回放UI | 历史执行列表+回放操作+对比视图 |

---

#### 4. 表单设计器 [高复杂度 | 5.0人月]

**现状**：不存在

**描述**：为非营销场景提供表单输入能力（如IT工单、审批表单、数据录入）

**需从零构建**：
- 拖拽式表单设计器
- 表单字段类型：文本/数字/日期/选择/文件上传/富文本
- 表单验证规则
- 表单与画布联动（表单提交→触发画布）

---

#### 5. 流程模板市场 [中复杂度 | 2.0人月]

**现状**：30+官方模板存在但无市场UI、无社区分享

**需补齐**：
- 模板市场首页（分类+搜索+评分）
- 社区模板上传/分享
- 模板版本管理
- 模板使用统计

---

## 工作量估算

| 优先级 | 功能 | 后端人月 | 前端人月 | 测试人月 | 总计 |
|--------|------|---------|---------|---------|------|
| P0 | 连接器生态 | 5.0 | 3.0 | 1.0 | 9.0 |
| P0 | 可视化调试器 | 1.5 | 1.5 | 0.5 | 3.5 |
| P1 | 执行历史回放 | 1.5 | 0.5 | 0.3 | 2.3 |
| P1 | 表单设计器 | 3.0 | 2.0 | 0.5 | 5.5 |
| P1 | 流程模板市场 | 1.0 | 1.0 | 0.3 | 2.3 |
| | **合计** | **12.0** | **8.0** | **2.6** | **22.6** |

---

## 不推荐独立做的原因

| 风险 | 说明 |
|------|------|
| 强敌环伺 | n8n开源+400连接器+社区活跃；Zapier SaaS成熟；Temporal分布式工作流标杆 |
| 护城河浅 | 编排引擎本身无壁垒，n8n/Temporal可替代 |
| 场景泛化=不够深 | IT运维需要AIOps能力，数据管道需要Spark集成，每个场景都需要深度投入 |
| ROI低 | 22人月投入后仍无法与n8n匹敌 |

**建议**：将连接器生态和调试器作为引擎通用增强（服务于方向①②③），不作为独立方向。
