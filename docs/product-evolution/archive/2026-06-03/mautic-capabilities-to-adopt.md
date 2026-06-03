# Mautic 值得借鉴能力清单

> **状态**：待落地，优先级 P0-P2
> **创建时间**：2026-06-02
> **参考文档**：[Mautic 对比分析报告](mautic-comparison-2026-06.md)

---

## 一、P0 级别能力（1-2 周快速落地）

### 1.1 动态人群发送（Segments-based Sending）

**Mautic 实现**：
> "Segment emails can now adapt to audience growth in real time. Choose whether to continue sending to contacts as they enter a segment, or lock the audience to those present when sending begins."

**两种模式**：
```yaml
Dynamic (Expand): [触发时圈子 1000人] → [执行时圈子 5000人]
  ✅ 适合漏斗场景（加购 → 下单）
  ❌ 可能超量

Static (Lock): [触发时圈子 1000人] → [执行时圈子固定 1000人]
  ✅ 控制成本
  ❌ 错过新增转化
```

**Canvas 现状**：
- ❌ 触发时人群是固定的，无法动态调整

**实施价值**：
- ✅ 周期性触达（每日/每周新闻稿）
- ✅ 漏斗优化（转化漏斗随人群增长自动扩大）
- ✅ 成本控制（静态模式避免超量）

**实施周期**：1-2 周

**技术要点**：
```java
// backend/canvas-engine/src/main/java/com/photon/canvas/handler/SegmentationHandler.java
@Service
public class SegmentationHandler implements NodeHandler {

    @Override
    public Mono<Void> executeAsync(CanvasContext context, NodeExecutionConfig node) {
        String segmentId = node.getParam("segment_id", String.class);
        boolean lockAudience = node.getParam("lock_audience", Boolean.class, false);

        // 获取触发时刻的人群快照
        long triggerTime = System.currentTimeMillis();
        List<Long> triggerAudienceIds = audienceResolver.resolveAudience(segmentId);

        if (lockAudience) {
            // 静态模式：触发时锁定人群
            audienceResolver.configureBatchSend(triggerAudienceIds);
        } else {
            // 动态模式：周期性重新计算人群
            long refreshInterval = node.getParam("refresh_interval", Long.class, 24 * 60 * 60 * 1000L);
            audienceResolver.schedulePeriodicResolution(
                segmentId,
                triggerTime + refreshInterval,
                (recalcAudience) -> audienceResolver.configureBatchSend(recalcAudience)
            );
        }

        return nextNodes.stream()
            .map(nodeId -> executeNodeAsync(context, nodeId))
            .reduce(Mono.empty(), Mono::concat);
    }
}
```

**前端 UI 参数**：
```tsx
// frontend/src/pages/Canvas/NodeConfig/SegmentationNodeConfig.tsx
<Form.Item label="人群刷新策略">
   <Radio.Group value={lockAudience} onChange={handleLockAudienceChange}>
      <Radio value={false}>不锁定（随人群增长自动扩大）</Radio>
      <Radio value={true}>锁定触发时人群（避免超量）</Radio>
   </Radio.Group>
   {lockAudience === false && (
      <Form.Item name="refresh_interval" label="刷新间隔">
         <Select>
            <Option value={60*60*1000}>1 小时后刷新</Option>
            <Option value={24*60*60*1000}>1 天后刷新</Option>
            <Option value={7*24*60*60*1000}>7 天后刷新</Option>
         </Select>
      </Form.Item>
   )}
</Form.Item>
```

---

### 1.2 智能预览（Preview Improvements）

**Mautic 7 改进**：
```yaml
Preview Features:
  - Text message previews (quick on-the-go review)
  - Web notification previews (visual design validation)
  - Email scheduling moved to email detail page (reduced context switching)
```

**Canvas 现状**：
- ✅ Email Preview（基础）- 使用占位变量
- ⚠️ 无 Web Push 预览
- ⚠️ 无移动端样式预览
- ⚠️ 无 Dry Run 模式

**实施价值**：
- ✅ 减少误发布频率
- ✅ 提升运营体验

**实施周期**：1 周

**技术要点**：
```java
// backend/canvas-engine/src/main/java/com/photon/canvas/service/PreviewService.java
@Service
public class PreviewService {

    /**
     * Preview message content with variable substitution
     */
    public String previewMessage(String template, CampaignContext context) {
        Map<String, Object> variables = extractVariables(context);
        String rendered = renderTemplate(template, variables);

        // 特殊处理：移除私密数据（手机号/身份证）
        rendered = maskSensitiveData(rendered);

        return rendered;
    }

    /**
     * Dry Run simulation without actual sending
     */
    public DryRunResult dryRun(Canvas canvas, String segmentId, int testSize) {
        // 1. Resolve test audience
        List<Long> testUserIds = audienceResolver.resolveAudience(segmentId)
            .stream()
            .limit(testSize)
            .collect(Collectors.toList());

        // 2. Mock execution
        ExecutionContext mockContext = new ExecutionContext();
        mockContext.setTargetUserIds(testUserIds);

        List<NodeExecutionTrace> traces = new ArrayList<>();
        for (CanvasNode node : canvas.getNodes()) {
            NodeExecutionTrace trace = executeNodeMock(canvas, node, mockContext);
            traces.add(trace);
        }

        // 3. Summary report
        DryRunResult result = new DryRunResult();
        result.setTraces(traces);
        result.setExecutionTime(calculateEstimatedTime(traces));
        result.setNodesExceedingThreshold(findThresholdViolations(traces));

        return result;
    }

    private String maskSensitiveData(String content) {
        // 手机号脱敏：13812345678 → 138****5678
        content = content.replaceAll("(\\d{3})\\d{4}(\\d{4})", "$1****$2");
        // 身份证脱敏：110105199001011234 → 110105********1234
        content = content.replaceAll("(\\d{6})\\d{8}(\\d{4})", "$1********$2");
        return content;
    }
}
```

**前端 UI Enhancement**：
```tsx
// frontend/src/pages/Canvas/Simulation/PushMessagePreview.tsx
<Modal title="Push 消息预览" visible={previewVisible} onCancel={onClose}>
   <div className="mobile-preview-frame">
      <PhoneFrame>
         <div className="device-status-bar">9:41 AM</div>
         <div className="notification-card">
            <NotificationIcon type={channelType} />
            <div className="notification-content">
               <div className="notification-title">智能回复预览</div>
               <div className="notification-body">
                  {previewContent || '点击"试跑"生成预览...'}
               </div>
               <div className="notification-timestamp">
                  {currentTime}
               </div>
            </div>
         </div>
      </PhoneFrame>
   </div>
   <div className="test-controls">
      <Button onClick={() => runDryRun(10)}>试跑 10 条</Button>
      <Button type="primary" onClick={onPublish}>发布画布</Button>
   </div>
</Modal>
```

---

## 二、P1 级别能力（2-4 周落地）

### 2.1 画布导入导出（Canvas Migration Service）

**Mautic 实现**：
- 支持 **完整 Campaign Export**（JSON）
- 支持 **Import 到新环境**
- 解决环境迁移的 **重复配置** 问题

**Canvas 现状**：
- ❌ 无 Export API
- ❌ 无 Import API
- 需手动复制节点/连线索引

**实施价值**：
- ✅ 快速环境搭建
- ✅ 团队间知识复用（模板市场底层）
- ✅ 容灾方案（跨机房复制）

**实施周期**：2-3 周

**技术要点**：
```java
// backend/canvas-engine/src/main/java/com/photon/canvas/service/CanvasMigrationService.java
@Service
public class CanvasMigrationService {

    /**
     * Export canvas with all node configurations
     */
    public byte[] exportCanvas(Long canvasId) {
        Canvas canvas = canvasRepository.findById(canvasId)
            .orElseThrow(() -> new CanvasNotFoundException(canvasId));

        CanvasExportDTO dto = new CanvasExportDTO();
        dto.setCanvas(canvas);
        dto.setNodes(canvasNodeService.getAllNodesByCanvas(canvasId));
        dto.setEdges(canvasEdgeService.getAllEdgesByCanvas(canvasId));

        return JacksonUtil.writeValueAsBytes(dto);
    }

    /**
     * Import from JSON, includes validation and conflict resolution
     */
    public Long importCanvas(byte[] exportData, Long targetProjectId) {
        CanvasImportDTO dto = JacksonUtil.readValue(exportData, CanvasImportDTO.class);

        // 1. Validation
        validateCanvasImport(dto);

        // 2. Create project if not exists
        Long projectId = dto.getProjectId() != null
            ? dto.getProjectId()
            : projectService.createProject(dto.getProjectName());

        // 3. Create Canvas
        Canvas canvas = dto.getCanvas();
        canvas.setProjectId(projectId);
        canvas.setVersion(1L);
        canvas = canvasRepository.save(canvas);

        // 4. Create Nodes
        for (CanvasNode node : canvas.getNodes()) {
            node.setCanvasId(canvas.getId());
            canvasNodeRepository.save(node);
        }

        // 5. Create Edges
        for (CanvasEdge edge : canvas.getEdges()) {
            canvasEdgeRepository.save(edge);
        }

        return canvas.getId();
    }

    private void validateCanvasImport(CanvasImportDTO dto) {
        // Validate required fields
        if (dto.getCanvas() == null || dto.getCanvas().getName() == null) {
            throw new ValidationException("Missing canvas name");
        }

        // Validate nodes
        for (CanvasNode node : dto.getNodes()) {
            if (node.getType() == null) {
                throw new ValidationException("Node type is required");
            }
        }

        // Validate edges
        for (CanvasEdge edge : dto.getEdges()) {
            if (edge.getSourceNodeId() == null || edge.getTargetNodeId() == null) {
                throw new ValidationException("Edge source/target nodes are required");
            }
        }
    }
}
```

**前端 UI**：
```tsx
// frontend/src/pages/Canvas/CanvasMenu.tsx
<Dropdown.Button
  menu={{ items: [
    { key: 'export', label: '导出画布', onClick: handleExport },
    { key: 'import', label: '导入画布', onClick: handleImport },
    { key: 'share', label: '生成分享链接', onClick: handleShare },
  ]}}
>
  更多操作
</Dropdown.Button>
```

---

### 2.2 画布版本管理（Canvas Versioning）

**Mautic 实现**：
```yaml
Version History:
  - Draft/发布版本分离
  - 版本快照（每次保存生成版本）
  - 版本对比（并排差异）
  - 一键回滚
```

**Canvas 现状**：
- ⚠️ 画布只有一个版本，修改即生效

**实施价值**：
- ✅ 多人协作冲突解决
- ✅ 快速回滚（发布后发现问题）
- ✅ 版本对比（了解迭代历史）

**实施周期**：1-2 周

**技术要点**：
```sql
-- 新增 canvas_version 表
CREATE TABLE canvas_version (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    canvas_id BIGINT NOT NULL COMMENT '画布ID',
    version_number INT NOT NULL COMMENT '版本号',
    name VARCHAR(255) COMMENT '版本名称',
    description TEXT COMMENT '版本描述',
    status VARCHAR(20) DEFAULT 'draft' COMMENT '状态 draft/published/archived',
    created_by BIGINT NOT NULL COMMENT '创建人ID',
    created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    last_modified_by BIGINT COMMENT '最后修改人ID',
    last_modified_time DATETIME COMMENT '最后修改时间',
    UNIQUE KEY uk_canvas_version (canvas_id, version_number),
    INDEX idx_canvas_id (canvas_id),
    INDEX idx_created_time (created_time)
) COMMENT='画布版本表';

-- 添加版本字段到 canvas 表
ALTER TABLE canvas ADD COLUMN active_version_id BIGINT COMMENT '当前活跃版本ID';
ALTER TABLE canvas ADD COLUMN current_version_number INT COMMENT '当前版本号';
```

```java
// backend/canvas-engine/src/main/java/com/photon/canvas/service/CanvasVersionService.java
@Service
public class CanvasVersionService {

    @Transactional
    public Long createVersion(Long canvasId, String name, String description) {
        Canvas canvas = canvasRepository.findById(canvasId)
            .orElseThrow(() -> new CanvasNotFoundException(canvasId));

        // 1. If editing current version, create new version first
        if ("published".equals(canvas.getStatus())) {
            Long newVersionNumber = getCurrentVersionNumber(canvasId) + 1;
            copyToVersion(canvasId, newVersionNumber, "自动备份");
        }

        // 2. Save current version
        CanvasVersion version = new CanvasVersion();
        version.setCanvasId(canvasId);
        version.setVersionNumber(getCurrentVersionNumber(canvasId));
        version.setName(name);
        version.setDescription(description);
        version.setStatus("draft");
        version.setCreatedBy(getCurrentUserId());
        version.setCreateTime(LocalDateTime.now());

        CanvasVersion saved = canvasVersionRepository.save(version);

        // 3. Update canvas to point to new version
        canvas.setActiveVersionId(saved.getId());
        canvas.setCurrentVersionNumber(saved.getVersionNumber());
        canvasRepository.save(canvas);

        return saved.getId();
    }

    @Transactional
    public void rollBackToVersion(Long canvasId, Long versionId) {
        CanvasVersion targetVersion = canvasVersionRepository.findById(versionId)
            .orElseThrow(() -> new VersionNotFoundException(versionId));

        // 1. Copy version content to canvas
        copyFromVersion(canvasId, targetVersion.getVersionNumber());

        // 2. Update canvas status to draft
        Canvas canvas = canvasRepository.findById(canvasId).orElseThrow();
        canvas.setStatus("draft");
        canvasRepository.save(canvas);
    }
}
```

---

## 三、P2 级别能力（3-4 周落地）

### 3.1 Projects 模块（项目治理）

**Mautic 实现**：
```yaml
Projects: 逻辑容器
├── Campaign Management
│   ├── Welcome Series
│   ├── Abandoned Cart Recovery
│   └── Re-engagement Flows
├── Email Templates
├── Landing Pages
└── Segments
```

**Canvas 现状**：
- ⚠️ 画布粒度是单项目，无 Projects 容器

**实施价值**：
- ✅ 多品牌/多业务线隔离（多租户）
- ✅ 环境管理（Dev/Staging/Prod 分离）
- ✅ 资产归管

**实施周期**：3-4 周

**技术要点**：
```sql
-- 新增 projects 表
CREATE TABLE projects (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    code VARCHAR(50) NOT NULL UNIQUE COMMENT '项目代码（唯一标识）',
    name VARCHAR(255) NOT NULL COMMENT '项目名称',
    description TEXT COMMENT '项目描述',
    status VARCHAR(20) DEFAULT 'active' COMMENT '状态 active/archived/draft',
    owner_id BIGINT COMMENT '项目负责人ID',
    created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_time DATETIME ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_code (code)
) COMMENT='营销项目表';

-- 关联 canvas_version
ALTER TABLE canvas_version ADD COLUMN project_id BIGINT COMMENT '所属项目ID';
CREATE INDEX idx_project_version ON canvas_version(project_id);

-- 添加环境字段
ALTER TABLE canvas_version ADD COLUMN environment VARCHAR(20)
  DEFAULT 'production'
  CHECK (environment IN ('development','staging','production'));

-- 关联 projects 到 canvas
ALTER TABLE canvas ADD COLUMN project_id BIGINT;
CREATE INDEX idx_canvas_project ON canvas(project_id);
```

**前端 UI**：
```tsx
// frontend/src/pages/Project/ProjectList.tsx
<CardList:
  cards={projects.map(p => ({
    id: p.id,
    title: p.name,
    description: p.description,
    status: p.status,
    memberCount: getTeamMemberCount(p.id),
  }))}
  onCreate={() => setProjectModalVisible(true)}
/>

<ProjectModal visible={projectModalVisible} onFinish={handleProjectCreate} />
<ProjectDashboard id={selectedProjectId} />
```

---

## 四、战略启示能力（3-6 个月规划）

### 4.1 AI 能力路线图

**Mautic AI Manifesto 策略**：
```yaml
Mautic AI Principles:
  - AI-agnostic（AI 代理选择）
  - Accessible（普惠）
  - Flexible（多云兼容）
  - Transparent（透明）
  - Ethical（隐私保护）
```

**Canvas 可借鉴动作**：

#### 4.1.1 制定 AI 能力白皮书

创建 `docs/optimization/ai-capability-roadmap-canvas.md`，包含：
- AI 能力愿景
- 隐私保护（数据本地化）
- AI 模型选择（开源 vs 云厂商）
- 第三方 AI 生态策略

#### 4.1.2 分阶段落地

| Phase | 时间 | 能力目标 | 实施方式 |
|-------|------|---------|---------|
| **Phase 1** | 2026 Q3 | AI 辅助分段生成 | NLP 输入（自然语言）→ SQL 条件 |
| **Phase 2** | 2026 Q4 | AI 内容生成 | 邮件/Push 模板填充 + 行业模板 |
| **Phase 3** | 2027 Q1 | AI 策略优化 | 自动调整触达时机/渠道比例 |
| **Phase 4** | 2027 Q2 | AI 预测建模 | LTV 预测/流失预警 |

#### 4.1.3 公开 AI 能力路线图

```markdown
# Canvas AI 能力路线图

## 第一年：AI 辅助与合规模范

**Q3 2026** - 基础 NLP 能力
- ✅ AI 辅助分段生成（输入"给购买过婴儿用品的用户发优惠"，自动生成 SQL）
- ✅ 数据清洗与脱敏
- ✅ 合规审计日志

**Q4 2026** - 内容与策略
- ✅ AI 辅助模板生成（邮件/Push 话术建议）
- ✅ 多渠道内容适配
- ✅ 效果预测分析

**Q1 2027** - 优化与预测
- ✅ AI 策略优化（自动调整触达时机）
- ✅ 渠道比例智能分配
- ✅ 成本预测

**Q2 2027** - 深度预测
- ✅ User LTV 预测
- ✅ 流失预警
- ✅ ROI 实时监控

## 第二年：生态与洞察

**Q3 2027 - AI 能力市场**
- ✅ 开源 AI_ImplicitSegmentBuilder（社区版本）
- ✅ 商业 AI_AudienceInsight（企业版）

**Q4 2027 - 数据洞察平台**
- ✅ 实时用户画像
- ✅ 渠道效果对比
- ✅ A/B 测试洞察

## 关键原则

1. **隐私优先**：所有 AI 处理在私有化环境完成
2. **开源优先**：核心 AI 模块开源，商业化服务隔离
3. **透明可控**：AI 决策可追溯，用户可调整
4. **不依赖单一 AI 提供商**：支持 OpenAI/Anthropic/本地模型
```

---

## 五、实施优先级建议

### 5.1 快速胜利（2-4 周）

| 能力 | 周期 | ROI | 依赖 |
|------|------|-----|------|
| **Segments-based Sending** | 1-2 周 | 极高 | AudienceResolver |
| **Preview Improvements** | 1 周 | 高 | 前端 UI |
| **Canvas Migration Service** | 2-3 周 | 高 | 无 |

### 5.2 核心功能（4-8 周）

| 能力 | 周期 | ROI | 依赖 |
|------|------|-----|------|
| **画布版本管理** | 1-2 周 | 中 | CanvasVersionService |
| **Projects 模块** | 3-4 周 | 中 | 多租户架构 |

### 5.3 战略布局（3-6 月）

| 能力 | 周期 | ROI | 依赖 |
|------|------|-----|------|
| **AI 能力白皮书** | 1 周 | 长期 | 无 |
| **AI 能力市场** | 3-6 月 | 中 | AI 模型集成 |

---

## 六、风险与注意事项

### 6.1 技术风险

| 风险项 | 影响 | 概率 | 缓解措施 |
|--------|------|------|---------|
| **动态人群刷新超时** | 人群计算超时 | 中 | 添加后台任务保障 + 用户可配置超时阈值 |
| **导入画布版本不兼容** | 旧版本无法导入 | 低 | 版本号校验 + 旧版本自动升级 |
| ** Projects 性能问题** | 查询变慢 | 中 | 索引优化 + 数据库分片 |

### 6.2 业务风险

| 风险项 | 影响 | 概率 | 缓解措施 |
|--------|------|------|---------|
| **Preview 隐私泄露** | 敏感数据暴露 | 低 | 强制定义占位符 + 审计日志 |
| **Dry Run 误发布** | 试跑内容实际发送 | 极低 | 强制二次确认 + 沙箱环境 |

---

## 七、成功指标

### 7.1 量化指标（落地后 1 个月）

| 指标 | 目标 | 定义 |
|------|------|------|
| **误发布率** | ↓ 60% | 条件触发后未实际发送 |
| **环境搭建时间** | ↓ 70% | 从画布导出到生产部署时长 |
| **用户满意度** | ≥ 4.5/5 | 运营用户对预览 Dry Run 的满意度问卷 |

### 7.2 定性指标

- **开发者体验**：导入导出 API 响应时间 < 5s
- **运营体验**：动态人群刷新成功率 ≥ 99%
- **合规体验**：数据脱敏率 100%

---

## 八、后续行动

- [ ] **Week 1-2**：Segments-based Sending + Preview Improvements（P0）
- [ ] **Week 3-6**：Canvas Migration Service + 画布版本管理（P1）
- [ ] **Month 3-4**：Projects 模块 + AI 能力白皮书（P2）

**下一步**：确认优先级后生成详细 PRD。
