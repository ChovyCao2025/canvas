package org.chovy.canvas.web;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.chovy.canvas.common.PageResult;
import org.chovy.canvas.common.R;
import org.chovy.canvas.dal.dataobject.MessageSendRecordDO;
import org.chovy.canvas.dal.mapper.MessageSendRecordMapper;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.LocalDateTime;

/**
 * MessageSendRecordController 暴露 web 场景的 HTTP 接口。
 */
@RestController
@RequestMapping("/canvas/message-send-records")
@RequiredArgsConstructor
public class MessageSendRecordController {

    /**
     * 数据访问组件，用于访问和持久化对应数据。
     */
    private final MessageSendRecordMapper mapper;
    /**
     * 查询消息 Send Record列表接口，对应 GET 请求。
     * 接口不直接解析租户上下文，访问边界由路由鉴权和下游服务约束。
     * 该接口只读取数据，不主动触发业务写入。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param canvasId 画布 ID，可选。
     * @param executionId execution ID，可选。
     * @param userId user ID，可选。
     * @param channel 渠道过滤条件，可选。
     * @param status 状态过滤条件，可选。
     * @param startAt 请求参数，可选。
     * @param endAt 请求参数，可选。
     * @param page 请求参数，默认值为 1。
     * @param size 请求参数，默认值为 20。
     * @return 异步返回统一响应，包含分页结果。
     */
    @GetMapping
    public Mono<R<PageResult<MessageSendRecordDO>>> list(
            @RequestParam(required = false) Long canvasId,
            @RequestParam(required = false) String executionId,
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String channel,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startAt,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endAt,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return Mono.fromCallable(() -> {
            LambdaQueryWrapper<MessageSendRecordDO> wrapper = new LambdaQueryWrapper<MessageSendRecordDO>()
                    .eq(canvasId != null, MessageSendRecordDO::getCanvasId, canvasId)
                    .eq(hasText(executionId), MessageSendRecordDO::getExecutionId, executionId)
                    .eq(hasText(userId), MessageSendRecordDO::getUserId, userId)
                    .eq(hasText(channel), MessageSendRecordDO::getChannel, normalize(channel))
                    .eq(hasText(status), MessageSendRecordDO::getStatus, normalize(status))
                    .ge(startAt != null, MessageSendRecordDO::getCreatedAt, startAt)
                    .le(endAt != null, MessageSendRecordDO::getCreatedAt, endAt)
                    .orderByDesc(MessageSendRecordDO::getCreatedAt);
            Page<MessageSendRecordDO> result = mapper.selectPage(new Page<>(Math.max(1, page), clamp(size)), wrapper);
            return R.ok(PageResult.of(result.getTotal(), result.getRecords()));
        }).subscribeOn(Schedulers.boundedElastic());
    }
    /**
     * 获取消息 Send Record详情接口，对应 GET /{id}。
     * 接口不直接解析租户上下文，访问边界由路由鉴权和下游服务约束。
     * 该接口只读取数据，不主动触发业务写入。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param id 资源 ID。
     * @return 异步返回统一响应，包含获取消息 Send Record详情后的业务数据。
     */
    @GetMapping("/{id}")
    public Mono<R<MessageSendRecordDO>> detail(@PathVariable Long id) {
        return Mono.fromCallable(() -> {
            MessageSendRecordDO record = mapper.selectById(id);
            if (record == null) {
                return R.<MessageSendRecordDO>fail("发送记录不存在: " + id);
            }
            return R.ok(record);
        }).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * 按安全边界裁剪或保护输入值。
     *
     * @param size 分页或数量限制，避免一次处理过多数据。
     * @return 返回 clamp 计算得到的数量、金额或指标值。
     */
    private static int clamp(int size) {
        return Math.max(1, Math.min(size, 100));
    }

    /**
     * 判断业务条件是否成立。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回布尔判断结果。
     */
    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    /**
     * 规范化输入值。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private static String normalize(String value) {
        return value == null ? null : value.trim().toUpperCase();
    }
}
