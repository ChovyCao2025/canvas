package org.chovy.canvas.engine.handlers;

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
            item.put("user_profile", userProfile(ctx));
        }
        item.put("params", new LinkedHashMap<>(params));
        if (includeContextPayload) {
            item.put("callback_params", callbackParams(ctx, nodeId, now));
            item.put("process_info", processInfo(ctx, nodeId, now));
        }
        return List.of(item);
    }

    private Map<String, Object> userProfile(ExecutionContext ctx) {
        String userId = value(ctx != null ? ctx.getUserId() : null);
        Map<String, Object> userProfile = new LinkedHashMap<>();
        userProfile.put("target_type", "OPEN_ID");
        userProfile.put("target_id", userId);
        userProfile.put("customer_id", userId);
        return userProfile;
    }

    private Map<String, Object> callbackParams(ExecutionContext ctx, String nodeId, String now) {
        String executionId = value(ctx != null ? ctx.getExecutionId() : null);
        String currentNodeId = value(nodeId);
        String userId = value(ctx != null ? ctx.getUserId() : null);

        Map<String, Object> callbackParams = new LinkedHashMap<>();
        callbackParams.put("webhook_id", "");
        callbackParams.put("send_time", now);
        callbackParams.put("nodeId", currentNodeId);
        callbackParams.put("instanceId", executionId);
        callbackParams.put("batchId", executionId);
        callbackParams.put("actionId", executionId + ":" + currentNodeId);
        callbackParams.put("customerId", userId);
        return callbackParams;
    }

    private Map<String, Object> processInfo(ExecutionContext ctx, String nodeId, String now) {
        String executionId = value(ctx != null ? ctx.getExecutionId() : null);
        String currentNodeId = value(nodeId);

        Map<String, Object> processInfo = new LinkedHashMap<>();
        processInfo.put("processInstanceId", executionId);
        processInfo.put("processInstanceStartTime", now);
        processInfo.put("processNodeInstanceId", executionId + ":" + currentNodeId);
        processInfo.put("processNodeInstanceStartTime", now);
        processInfo.put("groupName", "");
        return processInfo;
    }

    private static String value(String value) {
        return value == null ? "" : value;
    }
}
