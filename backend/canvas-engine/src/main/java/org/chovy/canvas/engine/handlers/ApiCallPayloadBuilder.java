package org.chovy.canvas.engine.handlers;

import org.chovy.canvas.common.MapFieldKeys;
import org.chovy.canvas.engine.context.ExecutionContext;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.LongSupplier;

@Component
public class ApiCallPayloadBuilder {

    private final LongSupplier nowMillis;

    public ApiCallPayloadBuilder() {
        this(System::currentTimeMillis);
    }

    ApiCallPayloadBuilder(LongSupplier nowMillis) {
        this.nowMillis = nowMillis;
    }

    public List<Map<String, Object>> build(Map<String, Object> params,
                                           ExecutionContext ctx,
                                           String nodeId,
                                           boolean includeContextPayload) {
        Map<String, Object> item = new LinkedHashMap<>();
        String now = String.valueOf(nowMillis.getAsLong());
        if (includeContextPayload) {
            item.put(MapFieldKeys.USER_PROFILE, userProfile(ctx));
        }
        item.put(MapFieldKeys.PARAMS, new LinkedHashMap<>(params));
        if (includeContextPayload) {
            item.put(MapFieldKeys.CALLBACK_PARAMS, callbackParams(ctx, nodeId, now));
            item.put(MapFieldKeys.PROCESS_INFO, processInfo(ctx, nodeId, now));
        }
        return List.of(item);
    }

    private Map<String, Object> userProfile(ExecutionContext ctx) {
        String userId = value(ctx != null ? ctx.getUserId() : null);
        Map<String, Object> userProfile = new LinkedHashMap<>();
        userProfile.put(MapFieldKeys.TARGET_TYPE, "OPEN_ID");
        userProfile.put(MapFieldKeys.TARGET_ID, userId);
        userProfile.put(MapFieldKeys.CUSTOMER_ID, userId);
        return userProfile;
    }

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

    private static String value(String value) {
        return value == null ? "" : value;
    }
}
