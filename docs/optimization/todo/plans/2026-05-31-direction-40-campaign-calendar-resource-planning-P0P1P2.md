# 方向㊵：营销日历与资源规划 — 功能清单

> 定位：从"单画布独立管理"升级为"全局Campaign日历+资源规划+冲突检测+容量管理"——多视图日历+资源分配+预算追踪+Campaign生命周期管理+AI排期建议
> 策略评估：2026年营销团队需要协调6+渠道的并行Campaign，InfluenceFlow报告指出有日历的团队lead转化率高32%+销售周期快27%，monday.com/Asana/Wrike已成熟
> 竞品对标：monday.com(营销日历+资源管理+15+视图+AI Blocks)、Asana(Work Graph+时间线)、Wrike(多层审批+资源容量)、InfluenceFlow(2026 Campaign日历指南)
> 建议：**P2建议做**，依赖⑰运营工作台+⑧审批流+⑯协作权限成熟后启动，营销日历是运营工作台的"时间维度"核心组件

---

## 参考出处

| 出处 | 核心观点 | 链接 |
|------|---------|------|
| InfluenceFlow: Campaign Calendar Complete Guide 2026 | 有日历的团队lead转化率高32%+销售周期快27%；80/20规划+滚动12月日历+AI排期 | https://influenceflow.io/resources/campaign-calendar-the-complete-2026-guide-to-marketing-planning-execution/ |
| monday.com: 15 Best Marketing Calendar Software 2026 | 营销日历核心：多视图+资源容量+AI规划+跨职能协作；15+视图(Gantt/Kanban/时间线/工作量) | https://monday.com/blog/project-management/marketing-calendar-software/ |
| Storyflow: 12 Best Marketing Campaign Planning Tools 2026 | Campaign叙事+日历+协作+AI支持+定价 | https://storyflow.so/blog/best-marketing-campaign-planning-tools-2026 |
| Teamwork: 10 Marketing Planning Software Tools | 营销规划软件：组织/追踪/执行营销策略和Campaign | https://www.teamwork.com/blog/marketing-planning-software/ |
| Guideflow: 15 Best Marketing Calendar Software 2026 | 15款营销日历对比：价格/G2评分/适用团队 | https://www.guideflow.com/blog/best-marketing-calendar-software-tools |

---

## 现状盘点

| 功能 | 实现程度 | 核心现有代码 | 差距 |
|------|----------|-------------|------|
| 画布管理 | **完整** | Canvas CRUD+状态管理(DRAFT/ACTIVE/PAUSED/ARCHIVED) | 单个画布独立管理，无全局日历视图 |
| 画布执行 | **完整** | DagEngine: 定时触发/事件触发 | 无Campaign间依赖关系+冲突检测 |
| 协作 | **部分** | RBAC(ADMIN/OPERATOR) | 无资源容量/工作负载视图 |
| Campaign日历 | **不存在** | — | 无法在统一日历中查看所有画布的排期 |
| 资源规划 | **不存在** | — | 无人员/预算/渠道容量规划 |
| 冲突检测 | **不存在** | — | 无法检测画布间的时间/受众/渠道/预算冲突 |
| 预算追踪 | **不存在** | — | 无Campaign预算设置+消耗追踪+预警 |

---

## 功能清单

### P0 — Campaign日历

#### 1. 全局营销日历 [中复杂度 | 1.5人月]

| 子功能 | 描述 | 参考出处 |
|--------|------|---------|
| 多视图日历 | 月/周/日/时间线/Gantt视图 | monday.com: 15+ board views |
| Campaign条目 | 每个画布在日历上显示为Campaign条目(名称/渠道/状态/负责人) | InfluenceFlow: campaign calendar components |
| 拖拽排期 | 日历上拖拽调整Campaign时间 | monday: drag-and-drop |
| 颜色编码 | 按状态/渠道/品牌/类型颜色编码 | 可视化区分 |
| 过滤与搜索 | 按渠道/状态/负责人/标签筛选 | Airtable: filter views |
| 订阅与导出 | iCal订阅+Google Calendar同步+PDF/CSV导出 | Guideflow: Google Calendar integration |

#### 2. Campaign生命周期管理 [中复杂度 | 1.0人月]

| 子功能 | 描述 |
|--------|------|
| Campaign阶段 | 规划→审核→就绪→执行中→已完成→归档 |
| 生产时间线 | 从发布日逆向推算生产时间(创意/文案/审核)→自动提醒 |
| 依赖关系 | Campaign间依赖标注(如"邮件Campaign依赖产品上线") |
| 里程碑 | Campaign关键里程碑设置+进度追踪 |

### P1 — 资源与预算

#### 3. 资源容量规划 [中复杂度 | 1.0人月]

| 子功能 | 描述 |
|--------|------|
| 工作负载视图 | 每个运营人员/设计师的Campaign负载可视化 |
| 容量预警 | 某人负载>80%→预警+建议调整排期 |
| 渠道容量 | 每个渠道的并行Campaign上限(邮件渠道: 3个/天) |
| 资源分配 | 为每个Campaign分配人员+预估工时 |

#### 4. 预算管理 [低复杂度 | 0.5人月]

| 子功能 | 描述 |
|--------|------|
| 预算设置 | 每个Campaign设置预算上限 |
| 消耗追踪 | 实际消耗vs预算(短信费用/邮件发送量/优惠券成本) |
| 预警 | 消耗>80%预算→预警 |
| ROI预估 | Campaign预算vs预估收入→ROI预估 |

### P2 — AI智能排期

#### 5. AI排期建议 [低复杂度 | 0.5人月]
AI分析历史效果数据→推荐最佳Campaign时间窗口

#### 6. 冲突检测 [低复杂度 | 0.3人月]
同受众+同时段→冲突告警; 同渠道超容量→自动建议替代时间

---

## 工作量估算

| 优先级 | 功能 | 总计 |
|--------|------|------|
| P0 | 全局营销日历 | 1.7人月 |
| P0 | Campaign生命周期管理 | 1.1人月 |
| P1 | 资源容量规划 | 1.1人月 |
| P1 | 预算管理 | 0.6人月 |
| P2 | AI排期+冲突检测 | 0.9人月 |
| | **合计** | **5.4人月** |

## 关键依赖

⑰运营工作台(日历是核心组件) + ⑧审批流(Campaign审批) + ⑯协作权限(资源可见性) + ⑨数据中台(效果数据驱动排期)
