package com.example.userlogin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.userlogin.model.Role;

import java.util.List;

public interface RoleService extends IService<Role> {
    List<String> getUserRoleName(List<Long> roleIds);
}
