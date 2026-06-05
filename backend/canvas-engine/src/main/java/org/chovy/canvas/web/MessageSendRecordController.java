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

@RestController
@RequestMapping("/canvas/message-send-records")
@RequiredArgsConstructor
public class MessageSendRecordController {

    private final MessageSendRecordMapper mapper;

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

    private static int clamp(int size) {
        return Math.max(1, Math.min(size, 100));
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static String normalize(String value) {
        return value == null ? null : value.trim().toUpperCase();
    }
}
