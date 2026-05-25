package org.chovy.canvas.service.impl;

import cn.hutool.core.lang.Snowflake;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.chovy.canvas.common.MapFieldKeys;
import org.chovy.canvas.common.enums.NodeType;
import org.chovy.canvas.common.enums.TriggerType;
import org.chovy.canvas.dal.dataobject.EventDefinitionDO;
import org.chovy.canvas.dal.dataobject.EventLogDO;
import org.chovy.canvas.dal.mapper.EventLogMapper;
import org.chovy.canvas.domain.meta.EventDefinitionCacheService;
import org.chovy.canvas.dto.EventReportReq;
import org.chovy.canvas.engine.disruptor.CanvasDisruptorService;
import org.chovy.canvas.engine.wait.WaitResumeService;
import org.chovy.canvas.infrastructure.redis.TriggerRouteService;
import org.chovy.canvas.perf.PerfRunContext;
import org.chovy.canvas.service.EventDefinitionService;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;


@Slf4j
@RequiredArgsConstructor
@Service
public class EventDefinitionServiceImpl implements EventDefinitionService {

    private final EventLogMapper logMapper;
    private final CanvasDisruptorService disruptorService;
    private final TriggerRouteService triggerRouteService;
    private final ObjectMapper objectMapper;
    private final WaitResumeService waitResumeService;
    private final EventDefinitionCacheService eventDefinitionCacheService;
    private final Snowflake snowflake;

    @Override
    public Map<String, Object> doReportEvent(EventReportReq req) {
        // 1. 校验请求参数以及事件定义是否存在
        validateReportReq(req);

        String eventId = "evt-" + snowflake.nextIdStr();
        Map<String, Object> payload = req.getAttributes() != null ? req.getAttributes() : Map.of();

        // 2. 从路由表查所有监听此事件的已发布画布，逐一触发
        Set<String> canvasIds = triggerAllCanvas(req, payload, eventId);

        // 3. 记录事件日志（在获取 canvasIds 之后，以写入真实触发数量）
        EventLogDO eventLog = writeEventLog(req, payload, canvasIds);

        // 4. 构建返回结果
        return buildReturnMap(req, payload, eventId, eventLog, canvasIds);
    }

    private Map<String, Object> buildReturnMap(EventReportReq req, Map<String, Object> payload, String eventId, EventLogDO eventLog, Set<String> canvasIds) {
        int waitsResumed = waitResumeService.resumeEventWaits(
                req.getEventCode(),
                req.getUserId(),
                payload,
                eventId
        );

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put(MapFieldKeys.EVENT_LOG_ID, eventLog.getId());
        resp.put(MapFieldKeys.EVENT_CODE, req.getEventCode());
        resp.put(MapFieldKeys.USER_ID, req.getUserId());
        resp.put(MapFieldKeys.CANVAS_TRIGGERED, canvasIds.size());
        resp.put(MapFieldKeys.WAITS_RESUMED, waitsResumed);
        resp.put(MapFieldKeys.STATUS, "ACCEPTED");
        return resp;
    }

    private Set<String> triggerAllCanvas(EventReportReq req, Map<String, Object> payload, String eventId) {
        Set<String> canvasIds = triggerRouteService.getCanvasByBehavior(req.getEventCode());
        canvasIds.forEach(cidStr -> {
            try {
                Long cid = Long.parseLong(cidStr);
                // 事件实际发布
                // 具体的消费逻辑: org.chovy.canvas.engine.disruptor.CanvasDisruptorService.CanvasDisruptorService
                disruptorService.publish(cid, req.getUserId(), TriggerType.EVENT,
                        NodeType.EVENT_TRIGGER, req.getEventCode(), payload, eventId + "-" + cidStr);
                log.info("[EVENT] 触发画布 canvasId={} eventCode={} userId={}",
                        cid, req.getEventCode(), req.getUserId());
            } catch (Exception e) {
                log.warn("[EVENT] 触发画布失败 canvasId={}: {}", cidStr, e.getMessage());
            }
        });

        if (canvasIds.isEmpty()) {
            log.info("[EVENT] 无已发布画布订阅事件 eventCode={}", req.getEventCode());
        }
        return canvasIds;
    }

    private EventLogDO writeEventLog(EventReportReq req, Map<String, Object> payload, Set<String> canvasIds) {
        EventLogDO eventLog = new EventLogDO();
        eventLog.setEventCode(req.getEventCode());
        eventLog.setUserId(req.getUserId());
        eventLog.setPerfRunId(PerfRunContext.extract(payload));
        try {
            eventLog.setAttributes(req.getAttributes() != null
                    ? objectMapper.writeValueAsString(req.getAttributes()) : null);
        } catch (Exception ignored) {
        }
        eventLog.setCanvasTriggered(canvasIds.size());
        eventLog.setCanvasCount(canvasIds.size());
        logMapper.insert(eventLog);
        return eventLog;
    }

    private void validateReportReq(EventReportReq req) {
        // 校验事件编码以及用户ID是否为空
        if (req.getEventCode() == null || req.getEventCode().isBlank())
            throw new IllegalArgumentException("eventCode 不能为空");
        if (req.getUserId() == null || req.getUserId().isBlank())
            throw new IllegalArgumentException("userId 不能为空");

        // 验证事件定义存在（走 cache SDK，TTL=10min）
        EventDefinitionDO def = eventDefinitionCacheService.getPublishedByCode(req.getEventCode());
        if (def == null)
            throw new IllegalArgumentException("事件未定义或已禁用: " + req.getEventCode());
    }
}
