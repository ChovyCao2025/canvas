package org.chovy.canvas.dal.mapper;

import org.chovy.canvas.dal.dataobject.AudienceDefinitionDO;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 人群定义 Mapper（表：audience_definition）。
 *
 * <p>存储人群规则、计算策略与运行参数。
 */
@Mapper
public interface AudienceDefinitionMapper extends BaseMapper<AudienceDefinitionDO> {
    @Select("""
            SELECT id, tenant_id, name, description, rule_json, engine_type, data_source_type,
                   data_source_config, evaluation_strategy, default_snapshot_mode, cron_expression, enabled, created_by,
                   created_at, updated_at
            FROM audience_definition
            WHERE tenant_id = #{tenantId}
              AND enabled = 1
              AND evaluation_strategy IN ('OFFLINE_BATCH', 'HYBRID')
            ORDER BY updated_at ASC, id ASC
            LIMIT #{limit}
            """)
    List<AudienceDefinitionDO> selectMaterializationCandidates(@Param("tenantId") Long tenantId,
                                                               @Param("limit") int limit);

    // 人群规则 JSON 的解析和执行不在 Mapper 层处理。
    // 计算任务调度和状态更新在 AudienceBatchComputeService。
    // 删除人群时对应 bitmap 清理由业务服务触发。
}
