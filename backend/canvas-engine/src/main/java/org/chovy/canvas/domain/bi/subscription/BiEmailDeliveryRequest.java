package org.chovy.canvas.domain.bi.subscription;

import java.util.List;

/**
 * BiEmailDeliveryRequest 承载 domain.bi.subscription 场景中的不可变数据快照。
 * @param from from 字段。
 * @param to to 字段。
 * @param subject subject 字段。
 * @param text text 字段。
 * @param attachments attachments 字段。
 */
public record BiEmailDeliveryRequest(
        String from,
        List<String> to,
        String subject,
        String text,
        List<BiEmailAttachment> attachments
) {
    /**
     * 创建 BiEmailDeliveryRequest 实例并注入 domain.bi.subscription 场景依赖。
     * @param from 时间或范围边界，用于限定统计窗口。
     * @param to 时间或范围边界，用于限定统计窗口。
     * @param subject 待处理业务值，用于规则计算、转换或外部调用。
     * @param text text 参数，用于 BiEmailDeliveryRequest 流程中的校验、计算或对象转换。
     */
    public BiEmailDeliveryRequest(String from, List<String> to, String subject, String text) {
        this(from, to, subject, text, List.of());
    }

    public BiEmailDeliveryRequest {
        to = to == null ? List.of() : List.copyOf(to);
        attachments = attachments == null ? List.of() : List.copyOf(attachments);
    }
}
