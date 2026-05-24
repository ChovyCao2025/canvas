package org.chovy.canvas.dal.mapper;

import org.chovy.canvas.dal.dataobject.SysUserDO;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 系统用户 Mapper（表：sys_user）。
 *
 * <p>用于登录鉴权与后台用户管理 CRUD。
 */
@Mapper
public interface SysUserMapper extends BaseMapper<SysUserDO> {
    // 复杂查询由 Service 层通过 QueryWrapper 组合，当前无需自定义 SQL 方法。
    // 这样可以保持 Mapper 层简单，业务条件集中在 Service 便于测试。
    // 角色权限判断（ADMIN/OPERATOR）也在 Service/鉴权层处理。
    // 若后续出现复杂联表查询，优先新增专用 Mapper 方法而不是在 Controller 拼 SQL。
}
