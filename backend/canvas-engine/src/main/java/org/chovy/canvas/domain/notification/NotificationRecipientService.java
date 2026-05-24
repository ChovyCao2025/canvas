package org.chovy.canvas.domain.notification;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.chovy.canvas.dal.dataobject.SysUserDO;
import org.chovy.canvas.dal.mapper.SysUserMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationRecipientService {

    private final SysUserMapper userMapper;

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

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
