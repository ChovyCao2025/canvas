package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * CdpWarehouseSyntheticDataPathProbeRunDO 映射 dal.dataobject 场景的持久化数据行。
 */
@Data
@TableName("cdp_warehouse_synthetic_data_path_probe_run")
public class CdpWarehouseSyntheticDataPathProbeRunDO {

    /** CDP数仓合成数据路径探测运行主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属租户 ID */
    private Long tenantId;

    /** CDP数仓合成数据路径探测运行探测业务键 */
    private String probeKey;

    /** CDP数仓合成数据路径探测运行来源模式 */
    private String sourceMode;

    /** 关联的消息 ID */
    private String messageId;

    /** CDP数仓合成数据路径探测运行事件编码 */
    private String eventCode;

    /** 关联的用户 ID */
    private String userId;

    /** CDP数仓合成数据路径探测运行是否严格校验 */
    private Integer strictMode;

    /** CDP数仓合成数据路径探测运行当前状态 */
    private String status;

    /** CDP数仓合成数据路径探测运行来源状态 */
    private String sourceStatus;

    /** CDP数仓合成数据路径探测运行落地状态 */
    private String sinkStatus;

    /** CDP数仓合成数据路径探测运行ODS 状态 */
    private String odsStatus;

    /** CDP数仓合成数据路径探测运行ODSROWCOUNT数量 */
    private Long odsRowCount;

    /** CDP数仓合成数据路径探测运行开始时间 */
    private LocalDateTime startedAt;

    /** CDP数仓合成数据路径探测运行结束时间 */
    private LocalDateTime finishedAt;

    /** CDP数仓合成数据路径探测运行错误信息 */
    private String errorMessage;

    /** CDP数仓合成数据路径探测运行证据明细 JSON */
    private String evidenceJson;

    /** CDP数仓合成数据路径探测运行创建时间 */
    private LocalDateTime createdAt;

    /** CDP数仓合成数据路径探测运行最后更新时间 */
    private LocalDateTime updatedAt;
}
