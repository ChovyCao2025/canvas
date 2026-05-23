package org.chovy.canvas.domain.notification;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.chovy.canvas.auth.domain.SysUser;
import org.chovy.canvas.auth.domain.SysUserMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationRecipientService {

    private final SysUserMapper userMapper;

    public List<String> activeAdmins() {
        try {
            return userMapper.selectList(new LambdaQueryWrapper<SysUser>()
                            .eq(SysUser::getEnabled, 1)
                            .eq(SysUser::getRole, "ADMIN")
                            .orderByAsc(SysUser::getUsername))
                    .stream()
                    .map(SysUser::getUsername)
                    .filter(this::hasText)
                    .distinct()
                    .toList();
        } catch (Exception e) {
            log.error("[NOTIFICATION] 查询管理员收件人失败: {}", e.getMessage(), e);
            return List.of("admin");
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
