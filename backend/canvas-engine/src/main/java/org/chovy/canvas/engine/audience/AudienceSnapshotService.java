package org.chovy.canvas.engine.audience;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.common.enums.AudienceSnapshotMode;
import org.chovy.canvas.common.enums.NodeType;
import org.chovy.canvas.dal.dataobject.AudienceDefinitionDO;
import org.chovy.canvas.dal.dataobject.AudienceSnapshotDO;
import org.chovy.canvas.dal.mapper.AudienceDefinitionMapper;
import org.chovy.canvas.dal.mapper.AudienceSnapshotMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * AudienceSnapshotService 参与 engine.audience 场景的画布执行引擎处理。
 */
@Service
public class AudienceSnapshotService {

    private final AudienceUserResolver userResolver;
    private final AudienceSnapshotMapper snapshotMapper;
    private final AudienceDefinitionMapper definitionMapper;
    private final ObjectMapper objectMapper;
    private final int maxSnapshotUsers;

    /**
     * 创建 AudienceSnapshotService 实例并注入 engine.audience 场景依赖。
     * @param userResolver 依赖组件，用于完成数据访问、计算或外部能力调用。
     * @param snapshotMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param definitionMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param objectMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param maxSnapshotUsers max snapshot users 参数，用于 AudienceSnapshotService 流程中的校验、计算或对象转换。
     */
    public AudienceSnapshotService(AudienceUserResolver userResolver,
                                   AudienceSnapshotMapper snapshotMapper,
                                   AudienceDefinitionMapper definitionMapper,
                                   ObjectMapper objectMapper,
                                   @Value("${canvas.audience.snapshot.max-users:100000}") int maxSnapshotUsers) {
        this.userResolver = userResolver;
        this.snapshotMapper = snapshotMapper;
        this.definitionMapper = definitionMapper;
        this.objectMapper = objectMapper;
        this.maxSnapshotUsers = maxSnapshotUsers;
    }

    /**
     * 锁定人群在发布时刻的静态用户快照。
     *
     * <p>方法通过 resolver 计算用户、去重并校验最大用户数，随后把用户列表 JSON 写入 audience_snapshot。返回的快照 ID
     * 可绑定到发布版本中的节点配置，保证后续执行使用固定人群。
     *
     * @param audienceId 人群 ID
     * @param canvasId 画布 ID
     * @param canvasVersionId 画布版本 ID
     * @param nodeId 绑定该快照的节点 ID
     * @param operator 操作人
     * @return 新建的静态快照记录
     */
    public AudienceSnapshotDO lockSnapshot(Long audienceId,
                                           Long canvasId,
                                           Long canvasVersionId,
                                           String nodeId,
                                           String operator) {
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        List<String> users = userResolver.resolve(audienceId).stream()
                .filter(userId -> userId != null && !userId.isBlank())
                .distinct()
                .toList();
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (users.size() > maxSnapshotUsers) {
            throw new IllegalStateException("AUDIENCE_SNAPSHOT_LIMIT: audienceId=" + audienceId
                    + " size=" + users.size() + " max=" + maxSnapshotUsers);
        }

        AudienceSnapshotDO snapshot = new AudienceSnapshotDO();
        snapshot.setAudienceId(audienceId);
        snapshot.setCanvasId(canvasId);
        snapshot.setCanvasVersionId(canvasVersionId);
        snapshot.setNodeId(nodeId);
        snapshot.setSnapshotMode(AudienceSnapshotMode.STATIC_LOCKED.name());
        snapshot.setUserCount((long) users.size());
        snapshot.setUserIdsJson(writeJson(users));
        snapshot.setCreatedBy(operator);
        snapshot.setCreatedAt(LocalDateTime.now());
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        snapshotMapper.insert(snapshot);
        return snapshot;
    }

    /**
     * 读取人群默认快照模式。
     *
     * <p>方法只查询人群定义并按枚举默认值归一化；人群不存在时返回系统默认模式。
     *
     * @param audienceId 人群 ID
     * @return 归一化后的快照模式
     */
    public AudienceSnapshotMode defaultModeForAudience(Long audienceId) {
        AudienceDefinitionDO definition = definitionMapper.selectById(audienceId);
        return AudienceSnapshotMode.normalize(definition == null ? null : definition.getDefaultSnapshotMode());
    }

    /**
     * 解析指定人群快照中的用户 ID 列表。
     *
     * <p>方法读取快照表中的 JSON 用户列表，快照不存在或 JSON 非法时抛异常；不重新计算人群。
     *
     * @param snapshotId 快照 ID
     * @return 快照锁定的用户 ID 列表
     */
    public List<String> users(Long snapshotId) {
        AudienceSnapshotDO snapshot = snapshotMapper.selectById(snapshotId);
        if (snapshot == null) {
            throw new IllegalArgumentException("Audience snapshot not found: " + snapshotId);
        }
        if (snapshot.getUserIdsJson() == null || snapshot.getUserIdsJson().isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(
                    snapshot.getUserIdsJson(),
                    new TypeReference<List<String>>() {
                    });
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (Exception e) {
            throw new IllegalStateException("Audience snapshot parse failed: " + snapshotId, e);
        }
    }

    /**
     * 判断用户是否包含在指定快照中。
     *
     * <p>方法基于 {@link #users(Long)} 的静态列表判断，不读取实时人群规则。
     *
     * @param snapshotId 快照 ID
     * @param userId 用户 ID
     * @return {@code true} 表示用户属于该快照
     */
    public boolean contains(Long snapshotId, String userId) {
        return users(snapshotId).contains(userId);
    }

    /**
     * 发布画布时为需要静态锁定的人群节点绑定快照。
     *
     * <p>方法解析 graph JSON，扫描 TAGGER audience 节点，根据节点或人群默认模式决定是否创建静态快照，并把
     * audienceSnapshotId/audienceSnapshotMode 写回节点配置；输入图无法解析时抛序列化异常。
     *
     * @param canvasId 画布 ID
     * @param canvasVersionId 发布版本 ID
     * @param graphJson 原始画布图 JSON
     * @param operator 操作人
     * @return 已绑定快照信息的画布图 JSON
     */
    @SuppressWarnings("unchecked")
    public String bindAudienceSnapshotsForPublish(Long canvasId,
                                                  Long canvasVersionId,
                                                  String graphJson,
                                                  String operator) {
        try {
            Map<String, Object> root = objectMapper.readValue(
                    graphJson,
                    new TypeReference<Map<String, Object>>() {
                    });
            Object rawNodes = root.get("nodes");
            if (!(rawNodes instanceof List<?> nodes)) {
                return graphJson;
            }

            for (Object rawNode : nodes) {
                if (!(rawNode instanceof Map<?, ?> node)) {
                    continue;
                }
                Map<String, Object> mutableNode = (Map<String, Object>) node;
                if (!NodeType.TAGGER.equals(String.valueOf(mutableNode.get("type")))) {
                    continue;
                }

                Object rawConfig = mutableNode.get("config");
                if (!(rawConfig instanceof Map<?, ?> config)) {
                    continue;
                }
                Map<String, Object> mutableConfig = (Map<String, Object>) config;
                if (!"audience".equals(String.valueOf(mutableConfig.get("mode")))) {
                    continue;
                }

                Long audienceId = parseAudienceId(mutableConfig.get("audienceId"));
                AudienceSnapshotMode mode = resolveNodeMode(mutableConfig, audienceId);
                mutableConfig.put("audienceSnapshotMode", mode.name());
                if (mode == AudienceSnapshotMode.STATIC_LOCKED) {
                    AudienceSnapshotDO snapshot = lockSnapshot(
                            audienceId,
                            canvasId,
                            canvasVersionId,
                            String.valueOf(mutableNode.get("id")),
                            operator);
                    mutableConfig.put("audienceSnapshotId", snapshot.getId());
                } else {
                    mutableConfig.remove("audienceSnapshotId");
                }
            }
            return objectMapper.writeValueAsString(root);
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (RuntimeException e) {
            throw e;
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (Exception e) {
            throw new IllegalStateException("Bind audience snapshots failed: " + e.getMessage(), e);
        }
    }

    /**
     * 解析节点级人群快照模式。
     *
     * @param config 节点配置
     * @param audienceId 人群 ID
     * @return 节点显式配置的模式，缺失时使用人群默认模式
     */
    private AudienceSnapshotMode resolveNodeMode(Map<String, Object> config, Long audienceId) {
        Object raw = config.get("audienceSnapshotMode");
        return raw == null || String.valueOf(raw).isBlank()
                /**
                 * 按默认值规则处理输入值。
                 *
                 * @return 返回 defaultModeForAudience 流程生成的业务结果。
                 */
                ? defaultModeForAudience(audienceId)
                : AudienceSnapshotMode.normalize(String.valueOf(raw));
    }

    /**
     * 从节点配置值中解析人群 ID。
     *
     * @param rawAudienceId 原始人群 ID 值
     * @return Long 类型人群 ID
     */
    private Long parseAudienceId(Object rawAudienceId) {
        if (rawAudienceId == null || String.valueOf(rawAudienceId).isBlank()) {
            throw new IllegalStateException("Audience TAGGER node missing audienceId");
        }
        try {
            return Long.valueOf(String.valueOf(rawAudienceId));
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (NumberFormatException e) {
            throw new IllegalStateException("Audience TAGGER node has invalid audienceId: " + rawAudienceId, e);
        }
    }

    /**
     * 将对象序列化为 JSON 字符串。
     *
     * @param value 待序列化对象
     * @return JSON 字符串
     */
    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Audience snapshot serialization failed", e);
        }
    }
}
