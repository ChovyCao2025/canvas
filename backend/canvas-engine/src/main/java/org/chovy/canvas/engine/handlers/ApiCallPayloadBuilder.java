package org.chovy.canvas.engine.handlers;

import org.chovy.canvas.common.MapFieldKeys;
import org.chovy.canvas.engine.context.ExecutionContext;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.LongSupplier;

/**
 * Api Call Payload Builder节点处理器。
 *
 * <p>由 DAG 执行器在运行画布节点时调用，读取节点 config 与执行上下文，产出 NodeResult 决定后续路由。
 * <p>处理器应保持单节点职责，跨节点编排、重试和状态持久化由执行引擎统一管理。
 */
@Component
public class ApiCallPayloadBuilder {

    /** 当前时间供应器，用于生成回调和流程时间戳。 */
    private final LongSupplier nowMillis;

    /**
     * 构造 ApiCallPayloadBuilder 实例。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     */
    public ApiCallPayloadBuilder() {
        this(System::currentTimeMillis);
    }

    /**
     * 构造 ApiCallPayloadBuilder 实例，并根据入参初始化依赖、配置或内部状态。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param nowMillis nowMillis 方法执行所需的业务参数
     */
    ApiCallPayloadBuilder(LongSupplier nowMillis) {
        this.nowMillis = nowMillis;
    }

    /**
     * 构建、解析或转换 build 相关的业务数据。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param params params 方法执行所需的业务参数
     * @param ctx 执行上下文，提供当前画布、用户和节点运行态数据
     * @param nodeId nodeId 对应的业务主键或标识
     * @param includeContextPayload includeContextPayload 请求体、消息体或事件载荷
     * @return 查询、转换或计算得到的结果集合
     */
    public List<Map<String, Object>> build(Map<String, Object> params,
                                           ExecutionContext ctx,
                                           String nodeId,
                                           boolean includeContextPayload) {
        Map<String, Object> item = new LinkedHashMap<>();
        String now = String.valueOf(nowMillis.getAsLong());
        if (includeContextPayload) {
            // includeContextPayload 打开时，按外部触达平台协议补齐用户画像与流程追踪字段。
            item.put(MapFieldKeys.USER_PROFILE, userProfile(ctx));
        }
        item.put(MapFieldKeys.PARAMS, new LinkedHashMap<>(params));
        if (includeContextPayload) {
            item.put(MapFieldKeys.CALLBACK_PARAMS, callbackParams(ctx, nodeId, now));
            item.put(MapFieldKeys.PROCESS_INFO, processInfo(ctx, nodeId, now));
        }
        return List.of(item);
    }

    /**
     * 执行 user Profile 对应的业务逻辑。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param ctx 执行上下文，提供当前画布、用户和节点运行态数据
     * @return 按业务键组织的映射结果
     */
    private Map<String, Object> userProfile(ExecutionContext ctx) {
        String userId = value(ctx != null ? ctx.getUserId() : null);
        Map<String, Object> userProfile = new LinkedHashMap<>();
        userProfile.put(MapFieldKeys.TARGET_TYPE, "OPEN_ID");
        userProfile.put(MapFieldKeys.TARGET_ID, userId);
        userProfile.put(MapFieldKeys.CUSTOMER_ID, userId);
        return userProfile;
    }

    /**
     * 执行 callback Params 对应的业务逻辑。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param ctx 执行上下文，提供当前画布、用户和节点运行态数据
     * @param nodeId nodeId 对应的业务主键或标识
     * @param now now 方法执行所需的业务参数
     * @return 按业务键组织的映射结果
     */
    private Map<String, Object> callbackParams(ExecutionContext ctx, String nodeId, String now) {
        String executionId = value(ctx != null ? ctx.getExecutionId() : null);
        String currentNodeId = value(nodeId);
        String userId = value(ctx != null ? ctx.getUserId() : null);

        Map<String, Object> callbackParams = new LinkedHashMap<>();
        callbackParams.put(MapFieldKeys.WEBHOOK_ID, "");
        callbackParams.put(MapFieldKeys.SEND_TIME, now);
        callbackParams.put(MapFieldKeys.NODE_ID, currentNodeId);
        callbackParams.put(MapFieldKeys.INSTANCE_ID, executionId);
        callbackParams.put(MapFieldKeys.BATCH_ID, executionId);
        callbackParams.put(MapFieldKeys.ACTION_ID, executionId + ":" + currentNodeId);
        callbackParams.put(MapFieldKeys.CUSTOMER_ID_CAMEL, userId);
        return callbackParams;
    }

    /**
     * 执行 process Info 对应的业务逻辑。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param ctx 执行上下文，提供当前画布、用户和节点运行态数据
     * @param nodeId nodeId 对应的业务主键或标识
     * @param now now 方法执行所需的业务参数
     * @return 按业务键组织的映射结果
     */
    private Map<String, Object> processInfo(ExecutionContext ctx, String nodeId, String now) {
        String executionId = value(ctx != null ? ctx.getExecutionId() : null);
        String currentNodeId = value(nodeId);

        Map<String, Object> processInfo = new LinkedHashMap<>();
        processInfo.put(MapFieldKeys.PROCESS_INSTANCE_ID, executionId);
        processInfo.put(MapFieldKeys.PROCESS_INSTANCE_START_TIME, now);
        processInfo.put(MapFieldKeys.PROCESS_NODE_INSTANCE_ID, executionId + ":" + currentNodeId);
        processInfo.put(MapFieldKeys.PROCESS_NODE_INSTANCE_START_TIME, now);
        processInfo.put(MapFieldKeys.GROUP_NAME, "");
        return processInfo;
    }

    /**
     * 执行 value 对应的业务逻辑。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param value value 待写入、比较或转换的业务值
     * @return 转换或查询得到的字符串结果
     */
    private static String value(String value) {
        return value == null ? "" : value;
    }
}
