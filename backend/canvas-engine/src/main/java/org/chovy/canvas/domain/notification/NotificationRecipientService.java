package org.chovy.canvas.domain.notification;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.chovy.canvas.dal.dataobject.SysUserDO;
import org.chovy.canvas.dal.mapper.SysUserMapper;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 通知收件人解析服务。
 *
 * <p>当前负责查询可接收系统通知的管理员账号，并在查询异常时提供保底收件人。
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
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
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
