package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AudienceMaterializationRunDO 映射 dal.dataobject 场景的持久化数据行。
 */
@Data
@TableName("audience_materialization_run")
public class AudienceMaterializationRunDO {

    /** 人群物化运行主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属租户 ID */
    private Long tenantId;

    /** 关联的人群 ID */
    private Long audienceId;

    /** 人群物化运行版本号 */
    private Long version;

    /** 人群物化运行当前状态 */
    private String status;

    /** 人群物化运行规则配置 JSON */
    private String ruleJson;

    /** 人群物化运行匹配用户 */
    private Long matchedUsers;

    /** 人群物化运行位图业务键 */
    private String bitmapKey;

    /** 人群物化运行错误信息 */
    private String errorMessage;

    /** 人群物化运行开始时间 */
    private LocalDateTime startedAt;

    /** 人群物化运行结束时间 */
    private LocalDateTime finishedAt;

    /** 人群物化运行创建人 */
    private String createdBy;
}
