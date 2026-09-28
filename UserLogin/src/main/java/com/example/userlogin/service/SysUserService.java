package com.example.userlogin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.userlogin.model.SysUser;

import java.util.List;

/**
 *
 */
public interface SysUserService extends IService<SysUser> {
    List<SysUser> getByUsername(String username);

    SysUser getByAccount(String account);
}
