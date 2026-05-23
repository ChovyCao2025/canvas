package org.chovy.canvas.dto;

import lombok.Data;

/**
 * 安全更新请求（带乐观锁版本号）。
 *
 * <p>编辑器保存草稿时提交，用于避免并发编辑覆盖。
 * 当 `editVersion` 与数据库不一致时，服务层会拒绝本次更新。
 */
@Data
public class SafeUpdateReq {

    /** 画布名称（用于列表展示和检索）。 */
    private String name;

    /** 画布描述（可为空，主要用于运营备注）。 */
    private String description;

    /** 最新草稿 graph JSON（完整节点图快照）。 */
    private String graphJson;

    /** 当前编辑版本号（用于 CAS 乐观锁）。 */
    private int editVersion;
}
