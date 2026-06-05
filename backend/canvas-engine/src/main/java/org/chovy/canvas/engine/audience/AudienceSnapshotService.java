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

@Service
public class AudienceSnapshotService {

    private final AudienceUserResolver userResolver;
    private final AudienceSnapshotMapper snapshotMapper;
    private final AudienceDefinitionMapper definitionMapper;
    private final ObjectMapper objectMapper;
    private final int maxSnapshotUsers;

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

    public AudienceSnapshotDO lockSnapshot(Long audienceId,
                                           Long canvasId,
                                           Long canvasVersionId,
                                           String nodeId,
                                           String operator) {
        List<String> users = userResolver.resolve(audienceId).stream()
                .filter(userId -> userId != null && !userId.isBlank())
                .distinct()
                .toList();
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
        snapshotMapper.insert(snapshot);
        return snapshot;
    }

    public AudienceSnapshotMode defaultModeForAudience(Long audienceId) {
        AudienceDefinitionDO definition = definitionMapper.selectById(audienceId);
        return AudienceSnapshotMode.normalize(definition == null ? null : definition.getDefaultSnapshotMode());
    }

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
        } catch (Exception e) {
            throw new IllegalStateException("Audience snapshot parse failed: " + snapshotId, e);
        }
    }

    public boolean contains(Long snapshotId, String userId) {
        return users(snapshotId).contains(userId);
    }

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
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Bind audience snapshots failed: " + e.getMessage(), e);
        }
    }

    private AudienceSnapshotMode resolveNodeMode(Map<String, Object> config, Long audienceId) {
        Object raw = config.get("audienceSnapshotMode");
        return raw == null || String.valueOf(raw).isBlank()
                ? defaultModeForAudience(audienceId)
                : AudienceSnapshotMode.normalize(String.valueOf(raw));
    }

    private Long parseAudienceId(Object rawAudienceId) {
        if (rawAudienceId == null || String.valueOf(rawAudienceId).isBlank()) {
            throw new IllegalStateException("Audience TAGGER node missing audienceId");
        }
        try {
            return Long.valueOf(String.valueOf(rawAudienceId));
        } catch (NumberFormatException e) {
            throw new IllegalStateException("Audience TAGGER node has invalid audienceId: " + rawAudienceId, e);
        }
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Audience snapshot serialization failed", e);
        }
    }
}
