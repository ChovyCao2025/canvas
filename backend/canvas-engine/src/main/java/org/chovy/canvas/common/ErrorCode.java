package org.chovy.canvas.common;

/**
 * 统一错误码（设计文档第二十二章 22.2 节）。
 *
 * 使用约定：
 * 1) code 稳定、message 可演进（便于前端做稳定分支）；
 * 2) 新增错误码时按领域分段，避免无序增长；
 * 3) 不复用已有 code 表达新语义，避免灰度期误判。
 */
public final class ErrorCode {

    /**
     * 构造 ErrorCode 实例。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     */
    private ErrorCode() {}

    // 画布相关：配置、状态、版本流转
    /** 画布不存在。 */
    public static final String CANVAS_001 = "CANVAS_001";
    /** 画布未发布。 */
    public static final String CANVAS_002 = "CANVAS_002";
    /** DAG 校验失败（环）。 */
    public static final String CANVAS_003 = "CANVAS_003";
    /** 缺少触发器节点。 */
    public static final String CANVAS_004 = "CANVAS_004";
    /** 节点必填参数缺失。 */
    public static final String CANVAS_005 = "CANVAS_005";
    /** 版本不存在。 */
    public static final String CANVAS_006 = "CANVAS_006";
    /** 无草稿可发布。 */
    public static final String CANVAS_007 = "CANVAS_007";
    /** 同层节点字段冲突。 */
    public static final String CANVAS_008 = "CANVAS_008";
    /** 画布已被 Kill。 */
    public static final String CANVAS_009 = "CANVAS_009";
    /** 并发编辑冲突（乐观锁）。 */
    public static final String CANVAS_010 = "CANVAS_010";

    // 执行相关：运行期定位、超时、调用链限制
    /** 触发器节点未找到。 */
    public static final String EXEC_001 = "EXEC_001";
    /** 节点执行超时。 */
    public static final String EXEC_002 = "EXEC_002";
    /** 节点 Handler 未注册。 */
    public static final String EXEC_003 = "EXEC_003";
    /** 子画布循环调用。 */
    public static final String EXEC_004 = "EXEC_004";

    // 节点相关：单节点内部失败场景
    /** Groovy 脚本执行失败。 */
    public static final String NODE_001 = "NODE_001";
    /** API 调用失败。 */
    public static final String NODE_002 = "NODE_002";
    /** 发券失败。 */
    public static final String NODE_003 = "NODE_003";
    /** 熔断器打开。 */
    public static final String NODE_004 = "NODE_004";

    // 配额相关：触发前置检查拒绝原因
    /** 用户今日触发上限。 */
    public static final String QUOTA_001 = "QUOTA_001";
    /** 用户总触发上限。 */
    public static final String QUOTA_002 = "QUOTA_002";
    /** 冷却期未到。 */
    public static final String QUOTA_003 = "QUOTA_003";
    /** 全局触发上限。 */
    public static final String QUOTA_004 = "QUOTA_004";
    /** 活动尚未开始。 */
    public static final String QUOTA_005 = "QUOTA_005";
    /** 活动已结束或未发布。 */
    public static final String QUOTA_006 = "QUOTA_006";

    // 认证相关：登录、令牌、权限、账号状态
    /** 用户名或密码错误。 */
    public static final String AUTH_001 = "AUTH_001";
    /** Token 已过期。 */
    public static final String AUTH_002 = "AUTH_002";
    /** 无权限执行此操作。 */
    public static final String AUTH_003 = "AUTH_003";
    /** 账号已锁定（暴力破解保护，15分钟后解锁）。 */
    public static final String AUTH_004 = "AUTH_004";
    /** 账号已禁用。 */
    public static final String AUTH_005 = "AUTH_005";
}
