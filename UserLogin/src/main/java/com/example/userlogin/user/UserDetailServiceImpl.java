package com.example.userlogin.user;


import com.example.userlogin.model.SysUser;
import com.example.userlogin.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 从数据库中验证账户名、密码验证的过程将由框架帮我们完成，封装隐藏了
 */
@Service
public class UserDetailServiceImpl implements UserDetailsService {

    @Autowired
    private SysUserService sysUserService;
    @Autowired
    UserRoleService userRoleService;
    @Autowired
    RoleService roleService;
    @Autowired
    AuthorityService authorityService;
    @Autowired
    RoleAuthorityService  roleAuthorityService;


    @Override
    public UserDetails loadUserByUsername(String account) throws UsernameNotFoundException {

        SysUser sysUser = sysUserService.getByAccount(account);
        if (sysUser == null) {
            throw new UsernameNotFoundException("账户名错误");
        }

        //传入构造函数的值不能为空
        return new AccountUser(
                sysUser.getUserId(),
                sysUser.getAccount(),
                sysUser.getPassword(),
                sysUser.isDisable(),
                sysUser.isStatus(),
                getUserAuthority(sysUser.getUserId())
        );

    }

    /**
     * 获取用户权限信息
     * @param userId 用户ID
     * @return 当前权限列表
     */
    public List<GrantedAuthority> getUserAuthority(Long userId) {
        try {
            // 获取用户的角色ID列表
            List<Long> roleIds = userRoleService.getRolesByUserId(userId);
            if (roleIds == null || roleIds.isEmpty()) {
                // 如果没有角色，返回空列表
                return Collections.emptyList();
            }
            System.out.println("用户角色ID："+roleIds);
            // 获取角色的权限ID列表
            List<Long> authorityIds = roleAuthorityService.getAuthoritysByRoleIds(roleIds);
            if (authorityIds == null || authorityIds.isEmpty()) {
                // 如果没有权限，返回空列表
                return Collections.emptyList();
            }
            System.out.println("角色权限ID："+authorityIds);
            // 获取权限信息
            List<String> authorityNames = authorityService.getAuthorityByAuthoritys(authorityIds);
            if (authorityNames == null || authorityNames.isEmpty()) {
                // 如果没有持有权限，返回空列表
                return Collections.emptyList();
            }
            System.out.println("持有权限："+authorityNames);
            // 将权限信息列表中的每个字符串映射为SimpleGrantedAuthority对象,形成GrantedAuthority列表
            return authorityNames.stream()
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            // 处理异常
            e.printStackTrace();
            return Collections.emptyList(); // 或者抛出自定义异常
        }
    }

    /*public List<GrantedAuthority> getUserAuthority(Long userId) {
        //将权限信息字符串转换为GrantedAuthority列表
        //return AuthorityUtils.commaSeparatedStringToAuthorityList(authority);
    }*/
}
