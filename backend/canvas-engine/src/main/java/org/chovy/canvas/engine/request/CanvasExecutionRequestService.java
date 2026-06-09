package org.chovy.canvas.engine.request;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.dal.dataobject.CanvasDO;
import org.chovy.canvas.dal.dataobject.CanvasExecutionRequestDO;
import org.chovy.canvas.dal.mapper.CanvasMapper;
import org.chovy.canvas.dal.mapper.CanvasExecutionRequestMapper;
import org.chovy.canvas.perf.PerfRunContext;
import org.springframework.beans.factory.annotation.Autowired;
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
public class CanvasExecutionRequestService {

    /** 执行请求 Mapper，用于落库排队请求并支持幂等插入。 */
    private final CanvasExecutionRequestMapper mapper;
    /** Jackson ObjectMapper，用于 JSON 序列化和反序列化。 */
    private final ObjectMapper objectMapper;
    /** 画布 Mapper，用于按目标画布解析租户归属。 */
    private final CanvasMapper canvasMapper;

    /**
     * 创建 CanvasExecutionRequestService 实例并注入 engine.request 场景依赖。
     * @param mapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param objectMapper 依赖组件，用于完成数据访问或外部能力调用。
     */
    public CanvasExecutionRequestService(CanvasExecutionRequestMapper mapper, ObjectMapper objectMapper) {
        this(mapper, objectMapper, null);
    }

    /**
     * 创建 CanvasExecutionRequestService 实例并注入 engine.request 场景依赖。
     * @param mapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param objectMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param canvasMapper 依赖组件，用于完成数据访问或外部能力调用。
     */
    @Autowired
    public CanvasExecutionRequestService(CanvasExecutionRequestMapper mapper,
                                         ObjectMapper objectMapper,
                                         CanvasMapper canvasMapper) {
        this.mapper = mapper;
        this.objectMapper = objectMapper;
        this.canvasMapper = canvasMapper;
    }

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
        request.setTenantId(resolveTenantId(canvasId));
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

    /**
     * 根据画布 ID 解析租户 ID。
     *
     * @param canvasId 画布 ID
     * @return 画布所属租户 ID，缺少 CanvasMapper 时返回 null
     */
    private Long resolveTenantId(Long canvasId) {
        if (canvasMapper == null) {
            return null;
        }
        CanvasDO canvas = canvasMapper.selectById(canvasId);
        if (canvas == null) {
            throw new IllegalArgumentException("Canvas not found: " + canvasId);
        }
        return canvas.getTenantId();
    }

    /** 根据来源消息 ID 生成幂等请求 ID；无来源 ID 时生成随机请求 ID。 */
    private String buildRequestId(Long canvasId, String triggerType, String sourceMsgId) {
        String prefix = triggerType == null ? "request" : triggerType.toLowerCase();
        if (sourceMsgId == null || sourceMsgId.isBlank()) {
            return prefix + "-" + canvasId + "-" + UUID.randomUUID().toString()
                    .replace("-", "")
                    .substring(0, 24);
        }
        // 幂等边界必须包含 canvasId：同一条 RocketMQ 消息可以合法 fan-out 到多个画布。
        // 因此不要用全局 UNIQUE(source_msg_id) 替代这个请求 ID。
        String raw = prefix + ":" + canvasId + ":" + sourceMsgId;
        return prefix + "-" + canvasId + "-" + sha256(raw).substring(0, 24);
    }

    /** 计算 SHA-256 十六进制摘要，用于稳定生成幂等请求后缀。 */
    private String sha256(String raw) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(raw.getBytes(StandardCharsets.UTF_8)));
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    /** 将执行请求载荷序列化为数据库中的 JSON 文本。 */
    private String toJson(Map<String, Object> payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Execution request payload serialize failed", e);
        }
    }
}
