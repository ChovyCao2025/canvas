# Frontend Accessibility Audit

Status date: 2026-06-05

This audit records keyboard order, focus target, visible focus, label source, screen-reader name, and remediation path for the core frontend workflows covered by P2-06.

| Workflow | keyboard order | focus target | visible focus | label source | screen-reader name | remediation path |
| --- | --- | --- | --- | --- | --- | --- |
| login | username, password, submit | login form controls | Ant Design input/button focus ring | form label and button text | 用户名、密码、登录 | Keep form item labels and submit text visible. |
| home | sidebar navigation, notification bell, dashboard actions | navigation menu and dashboard action buttons | menu item and button focus styles | menu text and button text | 首页导航和运营动作 | Preserve visible menu text; icon-only actions need `aria-label`. |
| canvas list | sidebar, search/filter controls, create/template actions, table actions | search input, primary actions, row actions | input/button/table action focus styles | visible labels and button text | 旅程管理筛选和行操作 | Add labels to icon-only row actions before hiding visible text. |
| canvas editor | toolbar, node library, graph canvas, config panel, trace/history/settings drawers | node library items, edge actions, toolbar buttons, config inputs | button focus ring and focused node-library group outline | tooltips, `aria-label`, visible button text, config field labels | 画布编辑器节点库、画布区、配置面板和执行轨迹 | Keep `HoverEdge` actions as buttons and node library entries focusable. |
| api docs | sidebar, endpoint search/filter, endpoint cards, copy/test actions | endpoint filters and action buttons | input/button focus styles | visible endpoint names and action button text | API 文档筛选和接口操作 | Add labels to icon-only copy/test buttons. |
| tenant admin | sidebar, tenant table, create/disable/activate controls | tenant action buttons and dialogs | button and modal focus management | button text, modal title, form labels | 租户管理操作 | Preserve modal titles and confirmation button labels. |
| notification bell | bell button, drawer filter, view toggle, mark-read action, notification rows | notification bell, segmented filter, notification row buttons | button and list row focus styles | `aria-label` and visible notification title | 打开消息中心、通知分类筛选、查看通知 | Keep notification rows keyboard-activatable with Enter and Space. |

## Static Checks

- `frontend/src/pages/canvas-editor/canvasEditorAccessibility.ts` mirrors these rows for Node/Vitest checks.
- `frontend/src/components/canvas/HoverEdge.tsx` exposes insert/delete edge actions as named buttons.
- `frontend/src/components/node-panel/NodeLibraryItem.tsx` exposes node library entries as focusable labelled groups.
- `frontend/src/components/notifications/NotificationBell.tsx` labels the bell, category filter, archive action, and keyboard-activatable notification rows.

## Verification

```bash
cd frontend
npm test -- canvasEditorAccessibility notificationPresentation nodeLibrary
test -f docs/architecture/frontend/accessibility-audit.md
rg "keyboard order|visible focus|screen-reader name|canvas editor|notification bell" docs/architecture/frontend/accessibility-audit.md
```
