package org.chovy.canvas.domain.meta;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * API 定义 Mapper（表：api_definition）。
 *
 * <p>供 API_CALL 节点按 apiKey 查询 URL 与方法等元信息。
 */
@Mapper
public interface ApiDefinitionMapper extends BaseMapper<ApiDefinition> {
    // API 调用重试与错误处理策略在 ApiCallHandler 实现。
    // 这里存储的是“逻辑 API key -> 实际 URL/方法”的映射。
    // API 鉴权 token/签名等动态参数一般通过节点配置或网关注入。
}
