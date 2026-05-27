package org.chovy.canvas.engine.request;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.chovy.canvas.dal.dataobject.CanvasExecutionRequestDO;
import org.chovy.canvas.dal.mapper.CanvasExecutionRequestMapper;
import org.chovy.canvas.perf.PerfRunContext;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;

/**
 * 画布执行请求 执行请求治理组件。
 *
 * <p>负责画布执行请求的排队、派发、限流、重放或积压度量，削峰高并发触发流量。
 * <p>该层位于触发入口和 DAG 执行之间，核心目标是保护执行引擎稳定性。
 */
@Service
@RequiredArgsConstructor
public class CanvasExecutionRequestService {

    /** 执行请求 Mapper，用于落库排队请求并支持幂等插入。 */
    private final CanvasExecutionRequestMapper mapper;
    /** Jackson ObjectMapper，用于 JSON 序列化和反序列化。 */
    private final ObjectMapper objectMapper;

    /** 将触发请求写入执行请求表，供异步派发。 */
    public String enqueue(Long canvasId,
                          String userId,
                          String triggerType,
                          String triggerNodeType,
                          String matchKey,
                          Map<String, Object> payload,
                          String sourceMsgId) {
        String requestId = buildRequestId(canvasId, triggerType, sourceMsgId);
        CanvasExecutionRequestDO request = new CanvasExecutionRequestDO();
        request.setId(requestId);
        request.setCanvasId(canvasId);
        request.setUserId(userId);
        // 压测批次标识直接写入请求记录，后续执行和指标可沿着同一条链路关联。
        request.setPerfRunId(PerfRunContext.extract(payload));
        request.setTriggerType(triggerType);
        request.setTriggerNodeType(triggerNodeType);
        request.setMatchKey(matchKey);
        // 先序列化入库，再交给派发器异步消费，避免触发入口同步阻塞。
        request.setPayloadJson(toJson(payload != null ? payload : Map.of()));
        request.setSourceMsgId(sourceMsgId);
        // 初始态统一落为 PENDING，等待 dispatcher 抢占并推进到 RUNNING。
        request.setStatus(CanvasExecutionRequestStatus.PENDING);
        request.setAttemptCount(0);
        mapper.insertIgnore(request);
        return requestId;
    }

    /** 根据来源消息 ID 生成幂等请求 ID；无来源 ID 时生成随机请求 ID。 */
    private String buildRequestId(Long canvasId, String triggerType, String sourceMsgId) {
        String prefix = triggerType == null ? "request" : triggerType.toLowerCase();
        if (sourceMsgId == null || sourceMsgId.isBlank()) {
            return prefix + "-" + canvasId + "-" + UUID.randomUUID().toString()
                    .replace("-", "")
                    .substring(0, 24);
        }
        String raw = prefix + ":" + canvasId + ":" + sourceMsgId;
        return prefix + "-" + canvasId + "-" + sha256(raw).substring(0, 24);
    }

    /** 计算 SHA-256 十六进制摘要，用于稳定生成幂等请求后缀。 */
    private String sha256(String raw) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(raw.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    /** 将执行请求载荷序列化为数据库中的 JSON 文本。 */
    private String toJson(Map<String, Object> payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Execution request payload serialize failed", e);
        }
    }
}
