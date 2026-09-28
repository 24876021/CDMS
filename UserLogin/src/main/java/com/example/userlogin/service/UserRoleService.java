package com.example.userlogin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.userlogin.model.UserRole;

import java.util.List;

public interface UserRoleService extends IService<UserRole> {

    public List<Long> getRolesByUserId(Long userId);
    public int deleteByUserIdAndRoleId(Long userId, Long roleId);
    List<Long> getUserIdsByRoleId(Long roleId);
}
