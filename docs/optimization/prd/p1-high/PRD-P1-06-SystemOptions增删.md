# PRD-P1-06-SystemOptions增删

> 本文档为营销画布平台产品需求文档标准模板，每项缺项对应一份独立 PRD。

---

## 基本信息

| 字段 | 内容 |
|------|------|
| **PRD编号** | PRD-P1-06 |
| **需求名称** | SystemOptions增删 |
| **优先级** | P1 |
| **所属类别** | 数据库设计 |
| **提出日期** | 2026-05-31 |
| **来源** | BMAD 产品设计审查报告 |
| **竞品对标** | Braze 配置 via Dashboard: 支持配置级别全局和 App 级; Iterable 允许 config master 调整 |

---

## 1. 问题描述

### 1.1 现状

- `system_options` 表仅支持通过数据库直接插入或 Flyway 初始化
- 缺乏运行时增删配置项的 API 或管理界面
- 配置项类型限制固定，缺少灵活性

### 1.2 痛点

- 新增节点类型或新节点属性时需修改数据库表或依赖数据迁移代码
- 紧急配置调整时流程长，需协调 DBA
- 无法针对不同租户/环境使用不同配置模板

### 1.3 竞品对标

| 竞品 | 能力 |
|------|------|
| Braze | 支持配置级别全局和 App 级 |
| Iterable | 配置项可通过 master config 调整 |
| HubSpot | Settings 支持自定义属性 |

---

## 2. 目标与价值

### 2.1 用户故事

> 作为 系统管理员，我希望 通过前端界面或 API 运行时增删 `system_options` 配置项，以便 灵活适应业务变化与个性化场景。

### 2.2 成功指标

- 管理界面可按 key/value 类型创建、编辑、删除配置项
- 新增配置项评估参数配置后实时生效（如节点阈值配置）
- 系统对不同租户的环境支持不同配置集

### 2.3 不做会怎样

- 初始设计不合理导致后续架构调整成本高
- 紧急配置生效困难，依赖低效的数据迁移
- 多租户场景下通用配置管理混乱

---

## 3. 功能需求

### 3.1 核心功能

1. **配置 CRUD** — 支持按 key 增加/删除/修改配置项
2. **类型管理** — 支持配置项类型（String/Number/Boolean/JSON）
3. **权限控制** — 只有 ADMIN 角色可修改 `system_options`
4. **环境隔离** — 支持按环境（dev/test/prod）区分配置
5. **审计日志** — 记录配置变更历史

### 3.2 详细描述

- API 端点：
  - `GET /api/v3/system/options` — 查询配置（支持筛选 key）
  - `POST /api/v3/system/options` — 创建配置（key 唯一）
  - `PUT /api/v3/system/options/{key}` — 更新配置
  - `DELETE /api/v3/system/options/{key}` — 删除配置

- 数据结构：
  ```json
  {
    "key": "string",
    "value": "string",  // 任意 JSON 序列化后存入
    "valueType": "STRING|NUMBER|BOOLEAN|JSON",
    "description": "string",
    "env": "ALL|DEV|TEST|PROD"
  }
  ```

### 3.3 交互流程

1. Admin 登录管理后台 → 进入「系统配置」页面
2. 点击「新增配置」→ 填写 key/type/description/value
3. 选择生效环境（可选择多个或 ALL）
4. 保存后前端或原生应用通过读取配置实时生效

---

## 4. 非功能需求

- **性能要求**: 配置读取支持缓存，避免每次 DB 查询
- **安全要求**: 敏感配置（密码、密钥）通过加密字段存储
- **可用性要求**: 配置变更后无需重启服务

---

## 5. 验收标准

- [ ] 管理界面可按 key/value 类型创建配置
- [ ] 非 ADMIN 用户无法访问配置管理接口
- [ ] 配置变更时间戳、操作人记录在审计日志
- [ ] 配置支持多环境隔离，不同环境可设置不同值

---

## 6. 技术建议

### 6.1 涉及模块

- 后端 API（新增 SystemOptionController）
- 前端 Admin 界面（新增配置管理页面）
- 审计模块（auditing system_options 变更）

### 6.2 技术要点

- 使用 Spring Cache 缓存 `system_options`，更新时清除缓存
- 敏感值通过 `@Encrypt` 注解加密
- 类型转化根据 `valueType` 执行（如 Number 转 long）

### 6.3 预估工作量

- 配置 CRUD API 开发：5 人天
- 前端管理界面：3 人天
- 缓存与审计集成：1 人天

---

## 7. 依赖与风险

### 7.1 前置依赖

- 文库支持多租户里的分层对象设计

### 7.2 风险

- key 值的键控冲突（如 keyword 命名替换）
- 配置项被不恰当改写导致服务不可用

---

## 8. 参考资料

- BMAD 产品设计审查报告 (2026-05-31)
- 当前 `system_options` 表结构
