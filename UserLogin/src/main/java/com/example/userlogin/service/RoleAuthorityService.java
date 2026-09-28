package com.example.userlogin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.userlogin.model.RoleAuthority;
import com.example.userlogin.model.UserRole;

import java.util.List;

public interface RoleAuthorityService extends IService<RoleAuthority> {
    public List<Long> getAuthoritysByRoleIds(List<Long> roleIds);
    public List<Long> getAuthoritysByRoleIds(Long roleId); //查询单个角色的权限
    public int deleteByRoleIdAndAuthorityId(Long roleId, Long authorityId);
}
