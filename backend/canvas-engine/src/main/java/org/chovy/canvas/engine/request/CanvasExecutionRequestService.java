package org.chovy.canvas.engine.request;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.chovy.canvas.domain.execution.CanvasExecutionRequest;
import org.chovy.canvas.domain.execution.CanvasExecutionRequestMapper;
import org.chovy.canvas.perf.PerfRunContext;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CanvasExecutionRequestService {

    private final CanvasExecutionRequestMapper mapper;
    private final ObjectMapper objectMapper;

    public String enqueue(Long canvasId,
                          String userId,
                          String triggerType,
                          String triggerNodeType,
                          String matchKey,
                          Map<String, Object> payload,
                          String sourceMsgId) {
        String requestId = buildRequestId(canvasId, triggerType, sourceMsgId);
        CanvasExecutionRequest request = new CanvasExecutionRequest();
        request.setId(requestId);
        request.setCanvasId(canvasId);
        request.setUserId(userId);
        request.setPerfRunId(PerfRunContext.extract(payload));
        request.setTriggerType(triggerType);
        request.setTriggerNodeType(triggerNodeType);
        request.setMatchKey(matchKey);
        request.setPayloadJson(toJson(payload != null ? payload : Map.of()));
        request.setSourceMsgId(sourceMsgId);
        request.setStatus(CanvasExecutionRequestStatus.PENDING);
        request.setAttemptCount(0);
        mapper.insertIgnore(request);
        return requestId;
    }

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

    private String sha256(String raw) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(raw.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    private String toJson(Map<String, Object> payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Execution request payload serialize failed", e);
        }
    }
}
