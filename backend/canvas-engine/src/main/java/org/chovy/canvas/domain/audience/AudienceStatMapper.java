package org.chovy.canvas.domain.audience;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 人群统计 Mapper（表：audience_stat）。
 *
 * <p>记录人群计算状态、估算规模与最近计算时间。
 */
@Mapper
public interface AudienceStatMapper extends BaseMapper<AudienceStat> {
    // 统计聚合任务由 AudienceBatchComputeService 负责触发和写入。
    // 列表页展示的人群状态和规模主要读取本表。
    // READY/FAILED 等状态流转由批处理服务统一维护。
}
