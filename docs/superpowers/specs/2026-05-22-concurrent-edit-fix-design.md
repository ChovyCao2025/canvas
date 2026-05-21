# 画布版本并发覆盖修复设计（优化点 #7）

## 背景

两人同时编辑同一画布时，后提交者可能覆盖先提交者的内容。后端已实现乐观锁（`updateEditVersion` CAS + 409），但前端存在三处缺陷导致防护失效或体验差。

## 根因分析

### Bug 1：TypeScript 类型缺少 editVersion 字段

`frontend/src/types/index.ts` 的 `Canvas` interface 缺少 `editVersion?: number`，导致 TypeScript 认为该字段不存在，后续初始化代码无法安全引用。

### Bug 2：editVersion 初始化硬编码为 0

```typescript
const editVersion = useRef(0)  // 永远从 0 开始
```

后端 `CanvasDetailDTO.canvas` 已包含 `editVersion`，但前端从不读取它。结果：用户每次打开编辑页，都以 `editVersion=0` 发起第一次保存，若服务端已是 `editVersion=3`，第一次 CAS 就会错误地覆盖（若 0 恰好匹配）或误报冲突（若不匹配）。

### Bug 3：409 冲突提示 UX 不足

```typescript
if (err?.response?.status === 409)
  message.error('画布已被他人修改，请刷新后重试')
```

只有一条 Toast 消息，用户没有直接刷新的入口，体验差。

---

## 解决方案

### Fix 1：补充 TypeScript 类型

在 `Canvas` interface 中加入：
```typescript
editVersion?: number
```

### Fix 2：从 detail 正确初始化 editVersion

```typescript
// 改为
const editVersion = useRef(detail.canvas.editVersion ?? 0)
```

### Fix 3：409 冲突弹窗带刷新按钮

将 `message.error` 替换为 `Modal.confirm`：

```typescript
if (err?.response?.status === 409) {
  Modal.confirm({
    title: '画布已被他人修改',
    content: '当前画布已有新版本，刷新后你的未保存内容将丢失。是否立即刷新？',
    okText: '立即刷新',
    cancelText: '暂不刷新',
    onOk: () => window.location.reload(),
  })
}
```

---

## 不在范围内

- 差异对比/三路合并（复杂度高，留后续迭代）
- 多人协作实时感知（需 WebSocket，留后续迭代）
