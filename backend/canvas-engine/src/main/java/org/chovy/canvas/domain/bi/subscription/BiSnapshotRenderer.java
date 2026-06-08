package org.chovy.canvas.domain.bi.subscription;

/**
 * BiSnapshotRenderer 定义 domain.bi.subscription 场景中的扩展契约。
 */
public interface BiSnapshotRenderer {

    /**
     * 执行 configured 流程，围绕 configured 完成校验、计算或结果组装。
     *
     * @return 返回 configured 的布尔判断结果。
     */
    boolean configured();

    /**
     * 组装输出结构或完成对象转换。
     *
     * @param request 请求对象，承载本次操作的输入参数。
     * @return 返回组装或转换后的结果对象。
     */
    BiSnapshotRenderResult render(BiSnapshotRenderRequest request);
}
