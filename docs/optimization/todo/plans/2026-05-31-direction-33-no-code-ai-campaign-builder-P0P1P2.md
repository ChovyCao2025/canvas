# 方向㉝：无代码/Low-Code AI营销活动构建器 — 功能清单

> 定位：从"手动画布拖拽+配置"升级为"自然语言→智能生成画布+多渠道内容"——Prompt-driven Campaign Builder + AI画布生成 + 智能推荐 + 一键多渠道内容 + 对话式营销构建
> 策略评估：2026年AI Agent+No-Code趋势正重塑营销自动化交互范式，SuperMIA/Manus/Taskade已证明"一句话生成全渠道营销活动"可行
> 竞品对标：SuperMIA(自然语言→多渠道campaign+预估触达)、Manus(单Prompt→完整营销生命周期)、HubSpot Breeze(AI Agent Builder)、Gumloop/Vellum(No-Code AI Workflow)
> 建议：**P2建议做**，依赖④AI原生平台+⑮营销资源中心+㉘动态内容引擎成熟后启动，是营销平台的终极用户体验升级

---

## 参考出处

| 出处 | 核心观点 | 链接 |
|------|---------|------|
| SuperMIA: AI Multi-Channel Campaign Generator 2026 | 自然语言输入→6渠道(Email/Instagram/LinkedIn/Facebook/Google/Meta Ads)同时生成+预估触达仪表盘+品牌一致性+3分钟完成 | https://supermia.ai/ai-marketing-campaign/ |
| Manus: AI Agent for Marketing Automation 2026 | 单Prompt驱动完整营销生命周期：素材生成+视频+落地页+竞品分析+数据图表，100+AI Agent并行部署 | https://manus.im/solutions/marketing |
| HubSpot Breeze: AI Agent Builder 2026 | No-Code AI Agent Builder(Beta)，自然语言创建工作流+自动执行 | https://www.digitalapplied.com/blog/email-marketing-ai-agents-automation-guide-2026 |
| Vellum.ai: No-Code AI Workflow Guide 2026 | Natural-Language Agent Builder，描述需求→AI生成完整工作流，分钟级构建 | https://www.vellum.ai/blog/no-code-ai-workflow-automation-tools-guide |
| AITable.ai: No-Code Automation Platform 2026 | 业务用户通过可视化界面+拖拽+预建模板构建自动化，无需开发者 | https://aitable.ai/blog/no-code-automation-platform/ |
| Ubora Thrive AI: Campaign Generator 2026 | 完整多渠道campaign生成~90秒，Campaign Canvas™多阶段旅程+Smart Reply动态优化 | https://uborathrive.ai/ai-campaign-generators |
| Gumloop: Top No-Code AI Agent Builders 2026 | No-Code AI Agent，拖拽组合AI节点+外部工具+API，构建自动化工作流 | https://metaflow.life/blog/best-no-code-ai-agent-builders |
| Taskade Genesis 2026 | Prompt→完整工作应用，不限于文本生成，AI驱动生产力自动化 | https://www.taskade.com/blog/ai-prompt-generators |

---

## 现状盘点

| 功能 | 实现程度 | 核心现有代码 | 差距 |
|------|----------|-------------|------|
| DAG画布编辑器 | **完整** | React Flow(@xyflow/react) + dagre布局 + 拖拽节点 | 手工拖拽逐个节点，无AI辅助生成 |
| AI Next Best Action | **存根** | AiNextBestActionHandler.java — 仅返回fallbackAction | 硬编码回退，无AI模型集成，无推荐引擎 |
| 节点配置面板 | **完整** | MetaController: schema/上下文字段动态渲染 | 需手动填写字段，无智能填充 |
| 画布复制 | **部分** | Canvas复制API | 仅1:1复制，无智能变体生成 |
| AI画布生成 | **不存在** | — | 无自然语言→DAG画布自动生成 |
| 智能推荐 | **不存在** | — | 无节点推荐/下一步建议/最佳实践提示 |
| 多渠道内容生成 | **不存在** | — | 无一键生成Email/SMS/Push/社交内容 |
| 对话式构建 | **不存在** | — | 无Chat式交互创建画布 |
| 画布模板智能匹配 | **不存在** | — | 无基于目标的画布模板推荐 |
| Campaign预估 | **不存在** | — | 无触达预估/转化预估/收入预估 |

### 关键洞察

AiNextBestActionHandler vs AI Campaign Builder的本质差异：
- **AiNextBestActionHandler**：画布内执行节点（在运行时决定"给用户推什么"），但当前是存根——只返回fallback
- **AI Campaign Builder**：画布构建工具（在设计时回答"画布该怎么搭"），零实现

SuperMIA给营销平台的启示：
- **营销平台的终极UX不是更复杂的配置表单，而是去掉配置表单**
- "描述你的产品/目标/受众" → AI生成完整多渠道营销活动（邮件+社交+广告+落地页）
- 自然语言是最自然的交互界面，尤其在移动端和运营人员使用场景

Manus给Agent平台的启示：
- **单Prompt→完整营销生命周期**：不是生成一个节点配置，是生成整个画布+内容+投放
- **100+ Agent并行部署**：大规模竞品分析/线索增强/市场调研可分钟级完成
- **Canvas需要从"工具"进化为"Agent平台"**：画布不仅是人工编排工具，也是AI Agent的协作空间

Canvas画布编辑器的现状（强项）：
- React Flow拖拽画布+60+节点类型+条件分支+DAG自动布局
- 这些是AI生成画布的**最佳基础**：AI生成DAG JSON → React Flow渲染 → 人工微调

---

## 功能清单

### P0 — AI画布智能生成

---

#### 1. 自然语言→DAG画布生成 [高复杂度 | 2.5人月]

**现状**：零AI画布生成

**需补齐**：

| 子功能 | 描述 | 参考出处 |
|--------|------|---------|
| Prompt输入 | 自然语言描述营销目标→AI解析为画布结构 | SuperMIA: natural language brief→campaign |
| DAG生成引擎 | LLM→生成DAG JSON（节点类型/连接/配置）→React Flow渲染 | Canvas已有DAG JSON schema可复用 |
| 营销目标理解 | 识别目标(拉新/促活/转化/召回)+渠道偏好+受众约束 | SuperMIA: product+goal+audience+tone → campaign |
| 节点配置智能填充 | AI根据上下文自动填充节点参数(内容/时间/条件) | Ubora Thrive: Smart Reply |
| 多画布变体 | 同一目标生成3-5个画布变体(保守/激进/创新) | A/B测试联动 |
| 生成历史 | 生成历史记录+版本回退+重生成 | 对话历史 |
| 模板库学习 | 从现有画布模板学习→生成更符合业务习惯的画布 | Fine-tuning/In-context learning |

**AI画布生成流程**：

```
用户输入: "针对30天未活跃用户，在周二上午10点发一封'回流优惠'邮件，
         含20元优惠券，24小时后未打开则发短信提醒"

      ↓ LLM理解 (Prompt Engineering + Few-Shot Examples)
      
解析结果:
  目标: 用户回流
  受众: 30天未活跃用户 → Trigger: SEGMENT_TRIGGER + segmentId=inactive_30d
  渠道: Email + SMS
  时间: 周二10:00 → 时区感知
  内容: 回流优惠邮件 + 20元优惠券
  条件: 24h未打开 → Email Open Check + Wait 24h

      ↓ DAG生成 (Template + Rule-based + LLM)
      
生成DAG:
  [SEGMENT_TRIGGER: inactive_30d]
      ↓
  [SEND_EMAIL: 回流优惠邮件 - 含20元优惠券]
      ↓
  [WAIT: 24h]
      ↓
  [CONDITION: 邮件是否已打开?]
      ├─ YES → [END]
      └─ NO  → [SEND_SMS: 回流提醒短信]
                  ↓
               [END]

      ↓ React Flow渲染
      
用户在画布上看到生成的DAG，可手动微调任何节点配置
```

**AI画布生成数据库DDL**：

```sql
CREATE TABLE ai_canvas_generation (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_prompt TEXT NOT NULL COMMENT '用户自然语言输入',
    parsed_intent JSON COMMENT 'AI解析的用户意图(goal/channels/audience/timing)',
    generated_canvas_json JSON COMMENT '生成的DAG JSON',
    canvas_id BIGINT COMMENT '实际创建的画布ID(用户确认后)',
    generation_version INT NOT NULL DEFAULT 1 COMMENT '生成版本(同一prompt的第N次生成)',
    model_used VARCHAR(50) COMMENT '使用的LLM模型',
    model_temperature DECIMAL(3,2) COMMENT '生成温度',
    tokens_used INT COMMENT '消耗Token数',
    user_feedback VARCHAR(20) COMMENT 'APPROVED/MODIFIED/REJECTED',
    user_modifications JSON COMMENT '用户修改内容(用于模型改进)',
    created_by VARCHAR(64),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    tenant_id BIGINT NOT NULL DEFAULT 0,
    INDEX idx_user (created_by),
    INDEX idx_canvas (canvas_id),
    INDEX idx_tenant (tenant_id)
) COMMENT 'AI画布生成记录';

CREATE TABLE ai_canvas_conversation (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    session_id VARCHAR(64) NOT NULL COMMENT '对话会话ID',
    turn_number INT NOT NULL COMMENT '对话轮次',
    role VARCHAR(10) NOT NULL COMMENT 'USER/ASSISTANT/SYSTEM',
    content TEXT NOT NULL COMMENT '对话内容',
    actions JSON COMMENT '本轮触发的操作(generate/modify/explain)',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_session (session_id),
    INDEX idx_session_turn (session_id, turn_number)
) COMMENT 'AI画布对话历史';
```

---

#### 2. 智能节点与配置推荐 [中复杂度 | 1.5人月]

**现状**：零智能推荐

**需补齐**：

| 子功能 | 描述 |
|--------|------|
| 下一个节点推荐 | 在画布编辑时，AI推荐接下来可能需要的节点(Top 3) |
| 连接建议 | 选中节点→AI建议合理的下游连接 |
| 配置智能填充 | 选择节点→AI根据上下游上下文预填配置参数 |
| 受众智能匹配 | 输入营销目标→AI推荐合适的受众分群 |
| 时间智能建议 | AI根据行业最佳实践推荐发送时间（如电商: 周二10AM/周五8PM） |
| 内容智能生成 | 节点内邮件/SMS/推送内容根据目标自动生成初稿 |
| 风险提醒 | AI检测画布配置风险（如频控冲突/死循环/无出口）→提前告警 |

---

### P1 — 多渠道内容智能生成

---

#### 3. 一键多渠道内容生成 [高复杂度 | 2.0人月]

**现状**：每个渠道内容需手动填写

**需补齐**：

| 子功能 | 描述 | 参考出处 |
|--------|------|---------|
| 多渠道同步生成 | 一个brief→同时生成Email/SMS/Push/社交内容 | SuperMIA: 6 channel simultaneous |
| 渠道格式适配 | 自动适配各渠道格式(邮件标题20-50字符/SMS 70字符限制/社交hashtags) | SuperMIA: format per channel |
| 品牌语调一致性 | AI应用品牌语调参数→各渠道内容风格统一 | SuperMIA: brand consistency |
| 内容预览 | 各渠道内容在真实设备上的预览效果 | SuperMIA: preview on each platform |
| A/B变体生成 | 同一内容自动生成A/B测试变体(标题/CTA/图片) | ⑭A/B测试联动 |
| 多语言内容 | 一份brief→同时生成多语言版本 | ㉛i18n联动 |

**多渠道内容生成示例**：

```
Brief: "SaaS产品AI分析插件上线，面向SMB用户，引导免费试用，
       专业但不失亲和，突出'3分钟安装，立即见效'"

    ↓ AI内容生成

Email:
  标题: "3分钟，让你的数据开口说话 | AI分析插件上线"
  正文: "Hi {{user.name}}，我们刚刚上线了AI分析插件...只需3分钟安装..."
  
SMS:
  "【Canvas】AI分析插件上线！3分钟安装，立即见效。免费试用→ {{short_link}} 退订回T"

Push:
  标题: "AI分析插件已上线"
  正文: "3分钟安装，数据立即开口说话"

微信模板消息:
  模板ID: new_feature_launch
  first: "AI分析插件已上线，立即免费试用！"
  keyword1: "AI分析插件"
  keyword2: "3分钟安装，零配置"
  remark: "点击查看详情→"
```

---

#### 4. 对话式营销活动构建 [中复杂度 | 1.5人月]

**现状**：无对话式交互

**需补齐**：

| 子功能 | 描述 | 参考出处 |
|--------|------|---------|
| Chat Panel | 画布编辑器侧边Chat Panel，对话式创建/修改画布 | Manus: single prompt→workflow |
| 多轮对话 | AI可以反问澄清需求（目标用户？预算？渠道偏好？） | SuperMIA: natural conversation |
| 画布解释 | AI解释当前画布逻辑（"这个节点会在用户点击邮件后触发..."） | 画布可解释性 |
| 修改指令 | 自然语言修改画布（"把短信改为微信模板消息"） | 指令式编辑 |
| 错误修复 | AI检测画布问题→对话中建议修复方案 | 智能纠错 |
| 历史对话 | 完整对话历史+上下文记忆 | 会话持久化 |

---

### P2 — 高级AI能力

---

#### 5. Campaign智能预估与优化 [中复杂度 | 1.5人月]

**现状**：无预估能力

**需补齐**：

| 子功能 | 描述 | 参考出处 |
|--------|------|---------|
| 触达预估 | 根据受众规模+历史触达率→预估触达人数 | SuperMIA: estimated reach dashboard |
| 转化预估 | 根据历史转化率→预估点击/注册/购买数 | ML预测模型 |
| 收入预估 | 根据ARPU+预估转化→预估Campaign收入 | ROI预估 |
| 最佳发送时间 | ML分析用户打开/点击行为→推荐最佳发送时间 | CleverTap: Send Time Optimization |
| 预算优化 | 多Campaign预算分配优化（最大化总体ROI） | 线性规划/强化学习 |
| Campaign对比 | AI对比多个Campaign方案，推荐最优 | 方案评估 |

**预估仪表盘DDL**：

```sql
CREATE TABLE ai_campaign_estimation (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    canvas_id BIGINT NOT NULL,
    audience_size INT NOT NULL COMMENT '受众规模',
    estimated_reach INT COMMENT '预估触达',
    estimated_opens INT COMMENT '预估打开',
    estimated_clicks INT COMMENT '预估点击',
    estimated_conversions INT COMMENT '预估转化',
    estimated_revenue DECIMAL(15,2) COMMENT '预估收入',
    estimated_cost DECIMAL(15,2) COMMENT '预估成本',
    estimated_roi DECIMAL(5,2) COMMENT '预估ROI',
    confidence_level VARCHAR(10) COMMENT '置信度HIGH/MEDIUM/LOW',
    model_version VARCHAR(20),
    estimated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_canvas (canvas_id),
    INDEX idx_estimated_at (estimated_at)
) COMMENT 'AI Campaign预估数据';
```

---

#### 6. 智能画布优化建议 [中复杂度 | 1.0人月]

**描述**：AI持续分析画布效果→给出优化建议

| 子功能 | 描述 |
|--------|------|
| 效果归因 | AI分析哪些节点/步骤贡献了最多转化 |
| 瓶颈识别 | AI识别画布中的流失瓶颈（如第二步流失60%） |
| 优化建议 | AI给出具体优化建议（"将等待时间从24h缩短到6h可提升打开率15%"） |
| A/B建议 | AI建议哪些环节值得A/B测试 |
| 自动优化 | AI自动调整参数（频次/时间/内容）持续优化效果 |
| 效果报告 | AI生成自然语言Campaign效果分析报告 |

---

## 工作量估算

| 优先级 | 功能 | 后端人月 | 前端人月 | AI/ML人月 | 测试人月 | 总计 |
|--------|------|---------|---------|----------|---------|------|
| P0 | 自然语言→DAG生成 | 1.2 | 0.8 | 0.5 | 0.3 | 2.8 |
| P0 | 智能节点推荐 | 0.7 | 0.5 | 0.3 | 0.2 | 1.7 |
| P1 | 一键多渠道内容生成 | 0.8 | 0.5 | 0.5 | 0.3 | 2.1 |
| P1 | 对话式营销构建 | 0.7 | 0.5 | 0.3 | 0.2 | 1.7 |
| P2 | Campaign预估与优化 | 0.5 | 0.5 | 0.5 | 0.2 | 1.7 |
| P2 | 智能画布优化 | 0.5 | 0.3 | 0.3 | 0.1 | 1.2 |
| | **合计** | **4.4** | **3.1** | **2.4** | **1.3** | **11.2** |

---

## 执行顺序

```
Sprint 1 (P0-DAG): 自然语言→DAG画布生成 — 2.8人月
  → 产出：Prompt输入+LLM DAG生成+React Flow渲染+多画布变体+对话历史

Sprint 2 (P0-Recommend): 智能节点与配置推荐 — 1.7人月
  → 产出：下一节点推荐+连接建议+智能填充+风险提醒+内容初稿

Sprint 3 (P1-Content): 一键多渠道内容生成 — 2.1人月
  → 产出：多渠道同步生成+渠道格式适配+品牌语调一致+A/B变体+多语言

Sprint 4 (P1-Chat): 对话式营销活动构建 — 1.7人月
  → 产出：Chat Panel+多轮对话+画布解释+指令修改+错误修复

Sprint 5 (P2-高级): Campaign预估+画布优化 — 2.9人月
  → 产出：触达预估+转化预估+ROI预估+瓶颈识别+优化建议+自动优化
```

---

## 风险评估

| 风险 | 影响 | 缓解措施 |
|------|------|---------|
| LLM幻觉 | AI生成的DAG包含不存在的节点类型/参数→画布无法执行 | Schema约束生成+节点类型白名单+DAG验证器+人工审核 |
| 生成质量不稳定 | 同一prompt不同时间生成质量差异大 | Temperature控制+Few-Shot缓存+质量评分+版本对比 |
| 内容安全 | AI生成内容可能违规/不当 | 内容安全审核+品牌规范约束+敏感词过滤+人工审核 |
| Token成本 | LLM调用频繁→成本高 | 缓存常见生成+节点级增量生成+本地小模型做简单推荐 |
| 用户信任 | 运营人员不信任AI生成画布→使用率低 | 可解释性+人工可微调+效果对比+渐进式引入 |
| 画布复杂度 | LLM难以处理超复杂画布（50+节点） | 复杂画布分步生成+子画布组装+人工关键节点锚定 |

---

## 与其他方向的关系

| 方向 | 与㉝的关系 |
|------|----------|
| ④ AI原生平台 | AI画布生成+智能推荐+内容生成是AI平台的核心应用场景 |
| ⑮ 营销资源中心 | AI生成的内容/模板/素材存入资源中心 |
| ㉘ 动态内容引擎 | AI生成的内容通过Liquid模板引擎渲染 |
| ⑬ 实时用户画像 | 画像数据作为AI智能推荐的输入（用户标签→推荐节点/内容） |
| ⑨ 营销数据中台 | 历史Campaign效果数据作为AI预估和优化的训练数据 |
| ㉛ 国际化与多语言 | AI多语言内容生成+本地化适配 |
| ① 营销自动化深度 | AI生成画布→自动化执行→效果反馈→AI优化→闭环 |
| ⑭ A/B测试平台 | AI建议A/B测试方案+AI生成A/B变体 |
