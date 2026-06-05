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
public class VersionedAudienceBitmapStore {

    private static final String KEY_PREFIX = "audience:bitmap:";
    private static final String STATUS_WRITING = "WRITING";
    private static final String STATUS_READY = "READY";
    private static final String ROLLBACK_SUCCESS = "SUCCESS";
    private static final String ROLLBACK_NOOP = "NOOP";

    private final StringRedisTemplate redis;
    private final AudienceBitmapVersionMapper mapper;

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

    public void markReady(Long tenantId, Long audienceId, Long version) {
        mapper.markReady(normalizeTenant(tenantId), audienceId, version);
        redis.opsForValue().set(latestKey(audienceId), String.valueOf(version));
    }

    public RollbackResult rollbackToVersion(Long tenantId, Long audienceId, Long targetVersion) {
        Long scopedTenantId = normalizeTenant(tenantId);
        if (audienceId == null || audienceId <= 0) {
            throw new IllegalArgumentException("audienceId must be positive");
        }
        if (targetVersion == null || targetVersion <= 0) {
            throw new IllegalArgumentException("targetVersion must be positive");
        }
        AudienceBitmapVersionDO target = mapper.selectReadyVersion(scopedTenantId, audienceId, targetVersion);
        if (target == null) {
            throw new IllegalArgumentException("target bitmap version is not READY");
        }
        int rolledBack = mapper.markReadyVersionsNewerThanRolledBack(scopedTenantId, audienceId, targetVersion);
        redis.opsForValue().set(latestKey(audienceId), String.valueOf(targetVersion));
        return new RollbackResult(
                scopedTenantId,
                audienceId,
                targetVersion,
                target.getBitmapKey(),
                rolledBack,
                rolledBack > 0 ? ROLLBACK_SUCCESS : ROLLBACK_NOOP);
    }

    public boolean containsLatest(Long tenantId, Long audienceId, Long userIndex) {
        if (userIndex == null || userIndex < 0 || userIndex > Integer.MAX_VALUE) {
            return false;
        }

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
            return false;
        }
    }

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

    private RoaringBitmap deserialize(byte[] bytes) throws IOException {
        RoaringBitmap bitmap = new RoaringBitmap();
        bitmap.deserialize(new DataInputStream(new ByteArrayInputStream(bytes)));
        return bitmap;
    }

    private String bitmapKey(Long audienceId, Long version) {
        return KEY_PREFIX + audienceId + ":v:" + version;
    }

    private String latestKey(Long audienceId) {
        return KEY_PREFIX + audienceId + ":latest";
    }

    private Long normalizeTenant(Long tenantId) {
        return tenantId == null ? 0L : tenantId;
    }

    public record RollbackResult(
            Long tenantId,
            Long audienceId,
            Long targetVersion,
            String targetBitmapKey,
            int rolledBackVersions,
            String status) {
    }
}
