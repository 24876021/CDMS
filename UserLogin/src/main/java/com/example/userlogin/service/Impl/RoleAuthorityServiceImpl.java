package com.example.userlogin.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.userlogin.mapper.RoleAuthorityMapper;
import com.example.userlogin.model.RoleAuthority;
import com.example.userlogin.model.UserRole;
import com.example.userlogin.service.RoleAuthorityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class RoleAuthorityServiceImpl extends ServiceImpl<RoleAuthorityMapper, RoleAuthority> implements RoleAuthorityService {
    @Autowired
    RoleAuthorityMapper roleAuthorityMapper;
    public List<Long> getAuthoritysByRoleIds(List<Long> roleIds) {
        // 构建查询条件
        QueryWrapper<RoleAuthority> queryWrapper = Wrappers.query();
        queryWrapper.in("role_id", roleIds);

        // 执行查询
        List<RoleAuthority> roleAuthorities = roleAuthorityMapper.selectList(queryWrapper);//返回一个满足条件的RoleAuthority对象列表

        // 将 RoleAuthority 对象列表中每个对象映射为其AuthorityId属性的值,形成权限 ID 列表
        return roleAuthorities.stream()
                .map(RoleAuthority::getAuthorityId)
                .collect(Collectors.toList());
    }
    public List<Long> getAuthoritysByRoleIds(Long roleId) {
        // 构建查询条件
        QueryWrapper<RoleAuthority> queryWrapper = Wrappers.query();
        queryWrapper.in("role_id", roleId);

        // 执行查询
        List<RoleAuthority> roleAuthorities = roleAuthorityMapper.selectList(queryWrapper);//返回一个满足条件的RoleAuthority对象列表

        // 将 RoleAuthority 对象列表中每个对象映射为其AuthorityId属性的值,形成权限 ID 列表
        return roleAuthorities.stream()
                .map(RoleAuthority::getAuthorityId)
                .collect(Collectors.toList());
    }
    public int deleteByRoleIdAndAuthorityId(Long roleId, Long authorityId) {
        // 构建查询条件
        QueryWrapper<RoleAuthority> queryWrapper = Wrappers.query();
        queryWrapper.eq("role_id", roleId).eq("authority_id", authorityId);

        // 执行删除操作
        return roleAuthorityMapper.delete(queryWrapper);
    }
}
