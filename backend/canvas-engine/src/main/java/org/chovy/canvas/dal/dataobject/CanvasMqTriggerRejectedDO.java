package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * MQ 触发拒绝记录 数据对象，对应数据库表 {@code canvas_mq_trigger_rejected}。
 *
 * <p>该类是 MyBatis-Plus 与数据库之间的持久化模型，字段命名和类型需要与迁移脚本、Mapper XML 保持一致。
 * <p>业务层应通过 Service/Mapper 读写该对象，避免在控制器中直接暴露数据库结构。
 */
@Data
@TableName("canvas_mq_trigger_rejected")
public class CanvasMqTriggerRejectedDO {

    @TableId(type = IdType.AUTO)
    /** 拒绝记录主键 ID */
    private Long id;

    /** RocketMQ 消息 ID，用于定位被拒绝的原始消息 */
    private String msgId;

    /** 消息标签或触发优先级标签 */
    private String tag;

    /** 拒绝原因分类，如限流、解析失败或路由缺失 */
    private String reason;

    /** 详细错误信息 */
    private String errorMsg;

    /** 原始消息体内容，便于排查和人工补偿 */
    private String body;

    /** 拒绝记录创建时间 */
    private LocalDateTime createdAt;
}
