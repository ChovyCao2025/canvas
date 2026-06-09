package org.chovy.canvas.engine.audience;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.chovy.canvas.dal.dataobject.AudienceBitmapVersionDO;
import org.chovy.canvas.dal.mapper.AudienceBitmapVersionMapper;
import org.roaringbitmap.RoaringBitmap;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Base64;

@Slf4j
@Component
@RequiredArgsConstructor
/**
 * VersionedAudienceBitmapStore 参与画布执行引擎流程，封装节点、调度或运行时处理能力。
 */
public class VersionedAudienceBitmapStore {

    private static final String KEY_PREFIX = "audience:bitmap:";
    private static final String STATUS_WRITING = "WRITING";
    private static final String STATUS_READY = "READY";
    private static final String ROLLBACK_SUCCESS = "SUCCESS";
    private static final String ROLLBACK_NOOP = "NOOP";

    private final StringRedisTemplate redis;
    private final AudienceBitmapVersionMapper mapper;

    /**
     * 写入或更新业务数据，并保持关联状态一致。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param audienceId 业务对象 ID，用于定位具体记录。
     * @param version version 参数，用于 saveVersion 流程中的校验、计算或对象转换。
     * @param bitmap bitmap 参数，用于 saveVersion 流程中的校验、计算或对象转换。
     * @param source source 参数，用于 saveVersion 流程中的校验、计算或对象转换。
     * @return 返回流程执行后的业务结果。
     */
    public String saveVersion(Long tenantId, Long audienceId, Long version, RoaringBitmap bitmap, String source) {
        String key = bitmapKey(audienceId, version);
        byte[] bytes = serialize(bitmap);
        redis.opsForValue().set(key, Base64.getEncoder().encodeToString(bytes));

        AudienceBitmapVersionDO row = new AudienceBitmapVersionDO();
        row.setTenantId(normalizeTenant(tenantId));
        row.setAudienceId(audienceId);
        row.setVersion(version);
        row.setBitmapKey(key);
        row.setEstimatedSize(bitmap == null ? 0L : bitmap.getLongCardinality());
        row.setBitmapSizeKb(Math.max(1L, bytes.length / 1024L));
        row.setSource(source);
        row.setStatus(STATUS_WRITING);
        mapper.insert(row);
        return key;
    }

    /**
     * 推进状态流转并记录本次处理结果。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param audienceId 业务对象 ID，用于定位具体记录。
     * @param version version 参数，用于 markReady 流程中的校验、计算或对象转换。
     */
    public void markReady(Long tenantId, Long audienceId, Long version) {
        mapper.markReady(normalizeTenant(tenantId), audienceId, version);
        redis.opsForValue().set(latestKey(audienceId), String.valueOf(version));
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param audienceId 业务对象 ID，用于定位具体记录。
     * @param targetVersion target version 参数，用于 rollbackToVersion 流程中的校验、计算或对象转换。
     * @return 返回 rollbackToVersion 流程生成的业务结果。
     */
    public RollbackResult rollbackToVersion(Long tenantId, Long audienceId, Long targetVersion) {
        Long scopedTenantId = normalizeTenant(tenantId);
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (audienceId == null || audienceId <= 0) {
            throw new IllegalArgumentException("audienceId must be positive");
        }
        if (targetVersion == null || targetVersion <= 0) {
            throw new IllegalArgumentException("targetVersion must be positive");
        }
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        AudienceBitmapVersionDO target = mapper.selectReadyVersion(scopedTenantId, audienceId, targetVersion);
        if (target == null) {
            throw new IllegalArgumentException("target bitmap version is not READY");
        }
        int rolledBack = mapper.markReadyVersionsNewerThanRolledBack(scopedTenantId, audienceId, targetVersion);
        redis.opsForValue().set(latestKey(audienceId), String.valueOf(targetVersion));
        // 汇总前面计算出的状态和明细，返回给调用方。
        return new RollbackResult(
                scopedTenantId,
                audienceId,
                targetVersion,
                target.getBitmapKey(),
                rolledBack,
                rolledBack > 0 ? ROLLBACK_SUCCESS : ROLLBACK_NOOP);
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param audienceId 业务对象 ID，用于定位具体记录。
     * @param userIndex user index 参数，用于 containsLatest 流程中的校验、计算或对象转换。
     * @return 返回 contains latest 的布尔判断结果。
     */
    public boolean containsLatest(Long tenantId, Long audienceId, Long userIndex) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (userIndex == null || userIndex < 0 || userIndex > Integer.MAX_VALUE) {
            return false;
        }

        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        AudienceBitmapVersionDO latest = mapper.selectLatestReady(normalizeTenant(tenantId), audienceId);
        if (latest == null || !STATUS_READY.equals(latest.getStatus()) || latest.getBitmapKey() == null) {
            return false;
        }

        String encoded = redis.opsForValue().get(latest.getBitmapKey());
        if (encoded == null || encoded.isBlank()) {
            return false;
        }

        try {
            RoaringBitmap bitmap = deserialize(Base64.getDecoder().decode(encoded));
            return bitmap.contains(userIndex.intValue());
        } catch (IllegalArgumentException | IOException ex) {
            log.warn("[AUDIENCE] latest bitmap decode failed tenantId={} audienceId={} key={}: {}",
                    tenantId, audienceId, latest.getBitmapKey(), ex.getMessage());
            // 汇总前面计算出的状态和明细，返回给调用方。
            return false;
        }
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param bitmap bitmap 参数，用于 serialize 流程中的校验、计算或对象转换。
     * @return 返回 serialize 流程生成的业务结果。
     */
    public byte[] serialize(RoaringBitmap bitmap) {
        RoaringBitmap safeBitmap = bitmap == null ? new RoaringBitmap() : bitmap;
        safeBitmap.runOptimize();
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (DataOutputStream dos = new DataOutputStream(bos)) {
            safeBitmap.serialize(dos);
            dos.flush();
            return bos.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("failed to serialize audience bitmap", ex);
        }
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param bytes bytes 参数，用于 deserialize 流程中的校验、计算或对象转换。
     * @return 返回 deserialize 流程生成的业务结果。
     */
    private RoaringBitmap deserialize(byte[] bytes) throws IOException {
        RoaringBitmap bitmap = new RoaringBitmap();
        bitmap.deserialize(new DataInputStream(new ByteArrayInputStream(bytes)));
        return bitmap;
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param audienceId 业务对象 ID，用于定位具体记录。
     * @param version version 参数，用于 bitmapKey 流程中的校验、计算或对象转换。
     * @return 返回 bitmap key 生成的文本或业务键。
     */
    private String bitmapKey(Long audienceId, Long version) {
        return KEY_PREFIX + audienceId + ":v:" + version;
    }

    /**
     * 查询并组装符合条件的业务数据。
     *
     * @param audienceId 业务对象 ID，用于定位具体记录。
     * @return 返回 latest key 生成的文本或业务键。
     */
    private String latestKey(Long audienceId) {
        return KEY_PREFIX + audienceId + ":latest";
    }

    /**
     * 解析、归一化或保护输入值，生成安全可用的中间结果。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private Long normalizeTenant(Long tenantId) {
        return tenantId == null ? 0L : tenantId;
    }

    /**
     * RollbackResult 参与画布执行引擎流程，封装节点、调度或运行时处理能力。
     */
    public record RollbackResult(
            Long tenantId,
            Long audienceId,
            Long targetVersion,
            String targetBitmapKey,
            int rolledBackVersions,
            String status) {
    }
}
