package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 人群计算运行记录 数据对象，对应数据库表 {@code audience_compute_run}。
 *
 * <p>该类是 MyBatis-Plus 与数据库之间的持久化模型，字段命名和类型需要与迁移脚本、Mapper XML 保持一致。
 * <p>业务层应通过 Service/Mapper 读写该对象，避免在控制器中直接暴露数据库结构。
 */
@Data
@TableName("audience_compute_run")
public class AudienceComputeRunDO {

    @TableId(type = IdType.AUTO)
    /** 人群计算运行记录主键 ID */
    private Long id;

    /** 被计算的人群 ID，对应 audience_definition.id */
    private Long audienceId;

    /** 压测运行批次 ID，普通计算为空 */
    private String perfRunId;

    /** 压测输入样本 ID，用于关联性能测试数据 */
    private String perfInputId;

    /** 本次计算状态，如 RUNNING、SUCCEEDED、FAILED */
    private String status;

    /** 本次计算得到的预估人群规模 */
    private Long estimatedSize;

    /** 本次计算生成或引用的 Bitmap 大小（KB） */
    private Integer bitmapSizeKb;

    /** 计算失败原因，成功时为空 */
    private String errorMsg;

    /** 运行记录创建时间 */
    private LocalDateTime createdAt;

    /** 运行记录最后更新时间 */
    private LocalDateTime updatedAt;
}
