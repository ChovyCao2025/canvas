package com.photon.canvas.common;

/**
 * 统一错误码（设计文档第二十二章 22.2 节）。
 */
public final class ErrorCode {

    private ErrorCode() {}

    // 画布相关
    public static final String CANVAS_001 = "CANVAS_001"; // 画布不存在
    public static final String CANVAS_002 = "CANVAS_002"; // 画布未发布
    public static final String CANVAS_003 = "CANVAS_003"; // DAG 校验失败（环）
    public static final String CANVAS_004 = "CANVAS_004"; // 缺少触发器节点
    public static final String CANVAS_005 = "CANVAS_005"; // 节点必填参数缺失
    public static final String CANVAS_006 = "CANVAS_006"; // 版本不存在
    public static final String CANVAS_007 = "CANVAS_007"; // 无草稿可发布
    public static final String CANVAS_008 = "CANVAS_008"; // 同层节点字段冲突
    public static final String CANVAS_009 = "CANVAS_009"; // 画布已被 Kill
    public static final String CANVAS_010 = "CANVAS_010"; // 并发编辑冲突（乐观锁）

    // 执行相关
    public static final String EXEC_001 = "EXEC_001"; // 触发器节点未找到
    public static final String EXEC_002 = "EXEC_002"; // 节点执行超时
    public static final String EXEC_003 = "EXEC_003"; // 节点 Handler 未注册
    public static final String EXEC_004 = "EXEC_004"; // 子画布循环调用

    // 节点相关
    public static final String NODE_001 = "NODE_001"; // Groovy 脚本执行失败
    public static final String NODE_002 = "NODE_002"; // API 调用失败
    public static final String NODE_003 = "NODE_003"; // 发券失败
    public static final String NODE_004 = "NODE_004"; // 熔断器打开

    // 配额相关
    public static final String QUOTA_001 = "QUOTA_001"; // 用户今日触发上限
    public static final String QUOTA_002 = "QUOTA_002"; // 用户总触发上限
    public static final String QUOTA_003 = "QUOTA_003"; // 冷却期未到
    public static final String QUOTA_004 = "QUOTA_004"; // 全局触发上限
    public static final String QUOTA_005 = "QUOTA_005"; // 活动尚未开始
    public static final String QUOTA_006 = "QUOTA_006"; // 活动已结束或未发布

    // 认证相关
    public static final String AUTH_001 = "AUTH_001"; // 用户名或密码错误
    public static final String AUTH_002 = "AUTH_002"; // Token 已过期
    public static final String AUTH_003 = "AUTH_003"; // 无权限执行此操作
}
