package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * CdpProfileAttributeChangeLogDO 映射 dal.dataobject 场景的持久化数据行。
 */
@Data
@TableName("cdp_profile_attribute_change_log")
public class CdpProfileAttributeChangeLogDO {
    /** CDP画像属性变更日志主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 所属租户 ID */
    private Long tenantId;
    /** CDP画像属性变更日志属性编码 */
    private String attrCode;
    /** 关联的用户 ID */
    private String userId;
    /** CDP画像属性变更日志原值 */
    private String oldValue;
    /** CDP画像属性变更日志新值 */
    private String newValue;
    /** 关联的来源运行 ID */
    private Long sourceRunId;
    /** CDP画像属性变更日志变更时间 */
    private LocalDateTime changedAt;
}
