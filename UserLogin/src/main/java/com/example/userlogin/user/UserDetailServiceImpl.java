package com.example.userlogin.user;

import com.example.userlogin.controller.WsPushController;
import com.example.userlogin.model.SysUser;
import com.example.userlogin.service.*;
import com.example.userlogin.utils.RedisUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 从数据库中验证账户名、密码验证的过程将由框架帮我们完成，封装隐藏了
 */
@Service
public class UserDetailServiceImpl implements UserDetailsService {

    public static final int MAX_FAIL_COUNT = 5;
    public static final String LOGIN_FAIL_KEY = "login:fail:";
    public static final long LOCK_EXPIRE_SECONDS = 900;
    public static final long MANUAL_LOCK_EXPIRE_SECONDS = 1800;
    public static final String LOCK_KEY = "login:lock:";

    public static final String LOCK_TYPE_PWD_ERROR = "pwd";
    public static final String LOCK_TYPE_MANUAL = "manual";

    @Autowired
    private SysUserService sysUserService;
    @Autowired
    UserRoleService userRoleService;
    @Autowired
    AuthorityService authorityService;
    @Autowired
    RoleAuthorityService roleAuthorityService;
    @Autowired
    private RedisUtil redisUtil;
    @Autowired
    private WsPushController wsPushController;

    @Override
    public UserDetails loadUserByUsername(String account) throws UsernameNotFoundException {

        SysUser sysUser = sysUserService.getByAccount(account);
        if (sysUser == null) {
            throw new UsernameNotFoundException("账户名错误");
        }

        boolean isLocked = false;

        if (!sysUser.isStatus()) {
            String lockKey = LOCK_KEY + sysUser.getAccount();
            Object lockObj = redisUtil.get(lockKey);

            if (lockObj != null) {
                try {
                    Map<String, String> map = (Map<String, String>) lockObj;
                    String expireStr = map.get("expire");
                    long expireTime = Long.parseLong(expireStr);

                    if (System.currentTimeMillis() < expireTime) {
                        isLocked = true;
                    } else {
                        sysUser.setStatus(true);
                        sysUserService.updateById(sysUser);
                        redisUtil.del(lockKey);
                        //解锁推送
                        wsPushController.pushByRole(1L, "unlock:" + account);
                    }
                } catch (Exception e) {
                    // 过期自动解锁
                    redisUtil.del(lockKey);
                    sysUser.setStatus(true);
                    sysUserService.updateById(sysUser);
                }
            } else {
                // 数据库锁了但无Redis锁 → 判定为手动锁，存入30分钟
                Map<String, String> map = new HashMap<>();
                map.put("type", LOCK_TYPE_MANUAL);
                map.put("expire", String.valueOf(System.currentTimeMillis() + MANUAL_LOCK_EXPIRE_SECONDS * 1000));
                redisUtil.set(lockKey, map, MANUAL_LOCK_EXPIRE_SECONDS);
                isLocked = true;
            }
        }

        if (isLocked) {
            return new User(
                    sysUser.getAccount(),
                    sysUser.getPassword(),
                    true,
                    true,
                    true,
                    false,
                    Collections.emptyList()
            );
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
            if (roleIds == null || roleIds.isEmpty())
                // 如果没有角色，返回空列表
                return Collections.emptyList();
            // 获取角色的权限ID列表
            List<Long> authorityIds = roleAuthorityService.getAuthoritysByRoleIds(roleIds);
            if (authorityIds == null || authorityIds.isEmpty())
                // 如果没有权限，返回空列表
                return Collections.emptyList();

            List<String> authorityNames = authorityService.getAuthorityByAuthoritys(authorityIds);
            // 将权限信息列表中的每个字符串映射为SimpleGrantedAuthority对象,形成GrantedAuthority列表
            return authorityNames.stream().map(SimpleGrantedAuthority::new).collect(Collectors.toList());
        } catch (Exception e) {
            // 处理异常
            e.printStackTrace();
            return Collections.emptyList();
        }
    }
}