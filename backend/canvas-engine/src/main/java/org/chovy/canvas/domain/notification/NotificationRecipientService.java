package org.chovy.canvas.domain.notification;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.chovy.canvas.dal.dataobject.SysUserDO;
import org.chovy.canvas.dal.mapper.SysUserMapper;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 通知消息 Recipient 通知领域组件。
 *
 * <p>负责站内通知的创建、收件人解析、未读状态和实时推送封装。
 * <p>该组件连接异步任务、WebSocket 和通知持久化模型，保证消息中心口径一致。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationRecipientService {

    /** 系统用户 Mapper，用于查询可接收通知的后台管理员。 */
    private final SysUserMapper userMapper;

    /** 查询启用状态的管理员用户 ID 列表。 */
    public List<String> activeAdmins() {
        try {
            return userMapper.selectList(new LambdaQueryWrapper<SysUserDO>()
                            .eq(SysUserDO::getEnabled, 1)
                            .eq(SysUserDO::getRole, "ADMIN")
                            .orderByAsc(SysUserDO::getUsername))
                    .stream()
                    .map(SysUserDO::getUsername)
                    .filter(this::hasText)
                    .distinct()
                    .toList();
        } catch (Exception e) {
            log.error("[NOTIFICATION] 查询管理员收件人失败: {}", e.getMessage(), e);
            return List.of("admin");
        }
    }

    /** 判断字符串是否包含非空白字符。 */
    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
