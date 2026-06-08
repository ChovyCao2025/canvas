package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * BiDatasourceHealthSnapshotDO 映射 dal.dataobject 场景的持久化数据行。
 */
@Data
@TableName("bi_datasource_health_snapshot")
public class BiDatasourceHealthSnapshotDO {

    /** BI数据源健康快照主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** BI数据源健康快照来源业务键 */
    private String sourceKey;

    /** BI数据源健康快照来源类型 */
    private String sourceType;

    /** BI数据源健康快照可用 */
    private Boolean available;

    /** BI数据源健康快照消息 */
    private String message;

    /** BI数据源健康快照检查时间 */
    private LocalDateTime checkedAt;

    /** BI数据源健康快照创建时间 */
    private LocalDateTime createdAt;
}
