# 画布编辑器 Dirty 状态修复设计（优化点 #2）

## 背景

用户保存画布后，再次修改内容，保存按钮仍显示"已保存"而非"草稿*"。根因是三处变更操作缺少 `setIsDirty(true)` 调用。

## 根因分析

| 变更操作 | 当前状态 | 问题 |
|---------|---------|------|
| 修改节点配置（`onNodeDataChange` L637） | 只调 `setNodes`，无 dirty 标记 | 配置修改后按钮仍显示"已保存" |
| 添加/删除连线（`onEdgesChange` L924） | 使用原始 hook，无 dirty 标记 | 连线变化不触发 dirty |
| 修改画布名称（L718 `setCanvasName`） | 只更新 state，无 dirty 标记 | 改名后按钮仍显示"已保存" |

节点位置移动/删除已通过 `onNodesChangeWrapped` 正确处理，无需改动。

## 解决方案

### 1. 包装 onEdgesChange

新增 `onEdgesChangeWrapped`，过滤掉 `select`/`reset` 等无意义变更（鼠标点击不应触发 dirty），对 `add`/`remove` 变更调用 `setIsDirty(true)`。

```typescript
const onEdgesChangeWrapped = useCallback((changes: EdgeChange[]) => {
  onEdgesChange(changes)
  const significant = changes.some(c => c.type === 'add' || c.type === 'remove')
  if (significant) setIsDirty(true)
}, [onEdgesChange])
```

ReactFlow 的 `onEdgesChange` 传入改为 `onEdgesChangeWrapped`。

### 2. onNodeDataChange 补充 dirty 标记

```typescript
const onNodeDataChange = useCallback((nid: string, patch: Partial<CanvasNodeData>) => {
  setNodes(prev => prev.map(n =>
    n.id === nid ? { ...n, data: { ...n.data as CanvasNodeData, ...patch } } : n
  ))
  setIsDirty(true)
}, [setNodes])
```

### 3. 画布名称修改补充 dirty 标记

```typescript
onChange={e => { setCanvasName(e.target.value); setIsDirty(true) }}
```

## 不在范围内

- 自动保存逻辑（已有，无需改动）
- 节点位置变更的 dirty 逻辑（已正确处理）
