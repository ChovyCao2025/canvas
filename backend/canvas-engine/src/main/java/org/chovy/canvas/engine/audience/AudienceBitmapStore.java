package org.chovy.canvas.engine.audience;

import com.google.common.hash.Hashing;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.roaringbitmap.RoaringBitmap;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * 人群 Bitmap 存储组件（Redis）。
 *
 * <p>职责：
 * 1) 把用户 ID 映射为稳定整数位图索引；
 * 2) 序列化/反序列化 RoaringBitmap；
 * 3) 提供 membership 查询能力。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AudienceBitmapStore {

    private static final String KEY_PREFIX = "audience:bitmap:";

    private final StringRedisTemplate redis;

    /**
     * 业务 userId -> 位图整数下标。
     *
     * <p>使用 murmur3_32 以获得稳定、分布较均匀的 hash。
     */
    public static int toUid(String userId) {
        int hash = Hashing.murmur3_32_fixed()
                .hashString(userId, StandardCharsets.UTF_8)
                .asInt();
        return hash == Integer.MIN_VALUE ? 0 : Math.abs(hash);
    }

    /** 保存人群 bitmap 到 Redis。 */
    public void save(Long audienceId, RoaringBitmap bitmap) throws IOException {
        bitmap.runOptimize();
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (DataOutputStream dos = new DataOutputStream(bos)) {
            bitmap.serialize(dos);
            dos.flush();
        }
        redis.opsForValue().set(KEY_PREFIX + audienceId, Base64.getEncoder().encodeToString(bos.toByteArray()));
        log.info("[AUDIENCE] bitmap saved audienceId={} sizeKB={}", audienceId, bos.size() / 1024);
    }

    /** 从 Redis 读取人群 bitmap，不存在时返回空 bitmap。 */
    public RoaringBitmap load(Long audienceId) throws IOException {
        String encoded = redis.opsForValue().get(KEY_PREFIX + audienceId);
        if (encoded == null) {
            return new RoaringBitmap();
        }
        byte[] bytes = Base64.getDecoder().decode(encoded);
        RoaringBitmap bitmap = new RoaringBitmap();
        bitmap.deserialize(new DataInputStream(new ByteArrayInputStream(bytes)));
        return bitmap;
    }

    /** 判断用户是否命中指定人群。 */
    public boolean isMember(Long audienceId, String userId) {
        try {
            return load(audienceId).contains(toUid(userId));
        } catch (IOException e) {
            log.error("[AUDIENCE] bitmap load failed audienceId={}: {}", audienceId, e.getMessage());
            return false;
        }
    }

    /** 删除指定人群 bitmap。 */
    public void delete(Long audienceId) {
        redis.delete(KEY_PREFIX + audienceId);
    }
}
