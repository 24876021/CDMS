package com.example.userlogin.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.userlogin.mapper.UserRoleMapper;
import com.example.userlogin.model.UserRole;
import com.example.userlogin.service.UserRoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserRoleServiceImpl extends ServiceImpl<UserRoleMapper, UserRole> implements UserRoleService {
    @Autowired
    UserRoleMapper userRoleMapper;
    public List<Long> getRolesByUserId(Long userId) {
        // 构建查询条件
        QueryWrapper<UserRole> queryWrapper = Wrappers.query();
        queryWrapper.eq("user_id", userId);

        // 执行查询
        List<UserRole> userRoles = userRoleMapper.selectList(queryWrapper); // 返回一个满足条件的UserRole对象列表

        // 将 UserRole 对象列表中每个对象映射为其 RoleId 属性的值，形成角色 ID 列表
        return userRoles.stream()
                .map(UserRole::getRoleId)
                .collect(Collectors.toList());
    }
    /**
     * 根据 userId 和 roleId 删除记录
     *
     * @param userId 用户ID
     * @param roleId 角色ID
     * @return 删除的记录数
     */
    public int deleteByUserIdAndRoleId(Long userId, Long roleId) {
        // 构建查询条件
        QueryWrapper<UserRole> queryWrapper = Wrappers.query();
        queryWrapper.eq("user_id", userId).eq("role_id", roleId);

        // 执行删除操作
        return userRoleMapper.delete(queryWrapper);
    }

    @Override
    public List<Long> getUserIdsByRoleId(Long roleId) {
        QueryWrapper<UserRole> queryWrapper = Wrappers.query();
        queryWrapper.eq("role_id", roleId);
        List<UserRole> userRoles = userRoleMapper.selectList(queryWrapper);
        return userRoles.stream()
                .map(UserRole::getUserId)
                .collect(Collectors.toList());
    }
}

