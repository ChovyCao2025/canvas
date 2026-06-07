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

    /** Redis 中保存人群位图的 key 前缀。 */
    private static final String KEY_PREFIX = "audience:bitmap:";

    /** Redis 字符串模板，用于读写 Base64 编码后的位图数据。 */
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
        // RoaringBitmap 需要非负 int 下标；MIN_VALUE 取绝对值仍溢出，因此单独归零。
        return hash == Integer.MIN_VALUE ? 0 : Math.abs(hash);
    }

    /** 保存人群 bitmap 到 Redis。 */
    public void save(Long audienceId, RoaringBitmap bitmap) throws IOException {
        // runOptimize 会把连续区间压缩成 run container，适合大批量人群结果的 Redis 存储。
        bitmap.runOptimize();
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (DataOutputStream dos = new DataOutputStream(bos)) {
            bitmap.serialize(dos);
            dos.flush();
        }
        // Redis 使用 String 存储，二进制序列化结果先 Base64 化，避免字符集和协议层转义问题。
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
        // 反序列化格式需与 save() 中 RoaringBitmap.serialize 保持一致。
        bitmap.deserialize(new DataInputStream(new ByteArrayInputStream(bytes)));
        return bitmap;
    }

    /** 判断用户是否命中指定人群。 */
    public boolean isMember(Long audienceId, String userId) {
        try {
            // 在线判断不回查原始人群规则，只用持久化 Bitmap 做 O(1) membership 检查。
            return load(audienceId).contains(toUid(userId));
        } catch (IOException e) {
            log.error("[AUDIENCE] bitmap load failed audienceId={}: {}", audienceId, e.getMessage());
            return false;
        }
    }

    /** 返回两个人群的交集 bitmap，不修改已存储的原始 bitmap。 */
    public RoaringBitmap overlap(Long leftAudienceId, Long rightAudienceId) throws IOException {
        RoaringBitmap result = load(leftAudienceId);
        result.and(load(rightAudienceId));
        return result;
    }

    /** 返回两个人群的并集 bitmap，不修改已存储的原始 bitmap。 */
    public RoaringBitmap merge(Long leftAudienceId, Long rightAudienceId) throws IOException {
        RoaringBitmap result = load(leftAudienceId);
        result.or(load(rightAudienceId));
        return result;
    }

    /** 返回基础人群排除指定人群后的差集 bitmap，不修改已存储的原始 bitmap。 */
    public RoaringBitmap exclude(Long baseAudienceId, Long excludedAudienceId) throws IOException {
        RoaringBitmap result = load(baseAudienceId);
        result.andNot(load(excludedAudienceId));
        return result;
    }

    /** 删除指定人群 bitmap。 */
    public void delete(Long audienceId) {
        redis.delete(KEY_PREFIX + audienceId);
    }
}
