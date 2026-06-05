export interface CanvasEditorAccessibilityRow {
  workflow: string
  keyboardOrder: string
  visibleFocus: string
  screenReaderName: string
  focusTarget: string
}

export interface CanvasEditorFocusableControl {
  id: string
  ariaName: string
  focusable: boolean
}

export function buildCanvasEditorAccessibilityRows(): CanvasEditorAccessibilityRow[] {
  return [
    row('login', 'email -> password -> submit', 'input and button outline', '登录表单', 'login submit'),
    row('home', 'primary navigation -> dashboard cards', 'navigation item outline', '首页导航', 'home overview'),
    row('canvas list', 'filters -> table -> actions', 'table row and action outline', '画布列表', 'canvas table'),
    row('canvas editor', 'toolbar -> node library -> canvas -> config panel', 'node and edge outline', '画布编辑器', 'node library and canvas actions'),
    row('api docs', 'search -> category -> endpoint detail', 'search and endpoint outline', '接口文档', 'api endpoint'),
    row('tenant admin', 'tabs -> forms -> save action', 'form control outline', '租户管理', 'tenant admin form'),
    row('notification bell', 'bell -> popover -> notification item', 'bell and item outline', '消息中心', 'notification bell'),
  ]
}

export function getCanvasEditorFocusableControls(): CanvasEditorFocusableControl[] {
  return [
    control('node-library-item', '节点库条目'),
    control('edge-insert-action', '插入到连线'),
    control('edge-delete-action', '删除连线'),
    control('notification-bell', '打开消息中心'),
    control('canvas-save-action', '保存画布'),
    control('canvas-publish-action', '发布画布'),
  ]
}

function row(
  workflow: string,
  keyboardOrder: string,
  visibleFocus: string,
  screenReaderName: string,
  focusTarget: string,
): CanvasEditorAccessibilityRow {
  return { workflow, keyboardOrder, visibleFocus, screenReaderName, focusTarget }
}

function control(id: string, ariaName: string): CanvasEditorFocusableControl {
  return { id, ariaName, focusable: true }
}
