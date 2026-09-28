package com.example.userlogin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.userlogin.model.Authority;
import com.example.userlogin.model.RoleAuthority;

import java.util.List;

public interface AuthorityService extends IService<Authority> {
    public List<String> getAuthorityByAuthoritys(List<Long> authorityIds);
}
