package com.example.userlogin.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.userlogin.mapper.AuthorityMapper;
import com.example.userlogin.model.Authority;
import com.example.userlogin.model.RoleAuthority;
import com.example.userlogin.model.UserRole;
import com.example.userlogin.service.AuthorityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service

public class AuthorityServiceImpl extends ServiceImpl<AuthorityMapper, Authority> implements AuthorityService {
    @Autowired
    AuthorityMapper authorityMapper;
    public List<String> getAuthorityByAuthoritys(List<Long> authorityIds) {

        // 构建查询条件
        QueryWrapper<Authority> queryWrapper = Wrappers.query();
        queryWrapper.in("authority_id", authorityIds);

        // 执行查询
        List<Authority> authorities = authorityMapper.selectList(queryWrapper);//返回一个满足条件的Authority对象列表

        // 将Authority对象列表中的每个对象映射为其 AuthorityName 属性的值,形成权限名称列表
        return authorities.stream()
                .map(Authority::getAuthorityName)
                .collect(Collectors.toList());
    }
}
