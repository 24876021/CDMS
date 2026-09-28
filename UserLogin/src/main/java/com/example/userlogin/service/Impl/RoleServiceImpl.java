package com.example.userlogin.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.userlogin.mapper.RoleMapper;
import com.example.userlogin.model.Role;
import com.example.userlogin.model.RoleAuthority;
import com.example.userlogin.service.RoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.swing.*;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class RoleServiceImpl extends ServiceImpl<RoleMapper, Role> implements RoleService {
    @Autowired
    RoleMapper roleMapper;
    public List<String> getUserRoleName(List<Long> roleIds) {
// 构建查询条件
        QueryWrapper<Role> queryWrapper = Wrappers.query();
        queryWrapper.in("role_id", roleIds);

        // 执行查询
        List<Role> roleAuthorities = roleMapper.selectList(queryWrapper);//返回一个满足条件的Role对象列表

        // 将 Role 对象列表中每个对象映射为其RoleName属性的值,形成角色信息列表
        return roleAuthorities.stream()
                .map(Role::getRoleName)
                .collect(Collectors.toList());
    }
}
