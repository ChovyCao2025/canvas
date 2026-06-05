package org.chovy.canvas.dal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.chovy.canvas.dal.dataobject.ChannelDedupeRecordDO;

@Mapper
public interface ChannelDedupeRecordMapper extends BaseMapper<ChannelDedupeRecordDO> {

    @Insert("""
            INSERT IGNORE INTO channel_dedupe_record
            (tenant_id, dedupe_group, content_hash, channel, user_id, expires_at, created_at)
            VALUES
            (#{tenantId}, #{dedupeGroup}, #{contentHash}, #{channel}, #{userId}, #{expiresAt}, NOW())
            """)
    int insertIgnore(ChannelDedupeRecordDO record);
}
