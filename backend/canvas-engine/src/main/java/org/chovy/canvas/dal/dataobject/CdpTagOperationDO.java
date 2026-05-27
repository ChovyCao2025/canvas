package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * CDP 标签操作 数据对象，对应数据库表 {@code cdp_tag_operation}。
 *
 * <p>该类是 MyBatis-Plus 与数据库之间的持久化模型，字段命名和类型需要与迁移脚本、Mapper XML 保持一致。
 * <p>业务层应通过 Service/Mapper 读写该对象，避免在控制器中直接暴露数据库结构。
 */
@Data
@TableName("cdp_tag_operation")
public class CdpTagOperationDO {

    @TableId(type = IdType.AUTO)
    /** 标签操作记录主键 ID */
    private Long id;

    /** 操作类型，如批量新增、批量更新或批量删除标签 */
    private String operationType;

    /** 被操作的标签编码，对应 tag_definition.tag_code */
    private String tagCode;

    /** 本次操作写入或删除的标签值 */
    private String tagValue;

    /** 本次操作涉及的用户总数 */
    private Integer totalCount;

    /** 本次操作成功处理的用户数 */
    private Integer successCount;

    /** 本次操作失败的用户数 */
    private Integer failCount;

    /** 操作状态，如 RUNNING、SUCCEEDED、FAILED */
    private String status;

    /** 操作失败或部分失败时的错误摘要 */
    private String errorMsg;

    /** 操作发起人 */
    private String createdBy;

    @TableField(fill = FieldFill.INSERT)
    /** 记录创建时间，由 MyBatis-Plus 自动填充 */
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    /** 记录最后更新时间，由 MyBatis-Plus 自动填充 */
    private LocalDateTime updatedAt;
}
