package org.chovy.canvas.domain.execution;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户触发配额 Mapper（表：canvas_user_quota）。
 *
 * <p>记录用户在不同窗口内的触发计数，供限流/频控判断。
 */
@Mapper
public interface CanvasUserQuotaMapper extends BaseMapper<CanvasUserQuota> {
    // 频控命中判断在 TriggerPreCheckService，Mapper 仅持久化计数状态。
    // 计数窗口的重置/过期通常依赖定时任务或 TTL 策略。
    // 多维配额（按场景/渠道）扩展时可在 Service 层封装 key 组合逻辑。
}
