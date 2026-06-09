package org.chovy.canvas.dal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.chovy.canvas.dal.dataobject.ChannelDedupeRecordDO;

/**
 * ChannelDedupeRecordMapper 定义 dal.mapper 场景中的扩展契约。
 */
@Mapper
public interface ChannelDedupeRecordMapper extends BaseMapper<ChannelDedupeRecordDO> {

    @Insert("""
            INSERT IGNORE INTO channel_dedupe_record
            (tenant_id, dedupe_group, content_hash, channel, user_id, expires_at, created_at)
            VALUES
            (#{tenantId}, #{dedupeGroup}, #{contentHash}, #{channel}, #{userId}, #{expiresAt}, NOW())
            """)
    /**
     * 执行数据写入或状态变更。
     *
     * @param record record 参数，用于 insertIgnore 流程中的校验、计算或对象转换。
     * @return 返回 insert ignore 计算得到的数量、金额或指标值。
     */
    int insertIgnore(ChannelDedupeRecordDO record);
}
