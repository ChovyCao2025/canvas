/**
 * 页面职责：画布连线交互常量，调大 React Flow 连接命中半径。
 *
 * 维护说明：节点 handle 较小，较大的半径能降低拖线连接失败概率。
 */
// React Flow defaults to 20px. The canvas nodes use compact handles, so a
// larger radius makes dropping onto the target handle less brittle.
export const CANVAS_CONNECTION_RADIUS = 48
