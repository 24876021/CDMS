package com.example.userlogin.user;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.util.Assert;

import java.util.Collection;


/**
账户认证管理器(自定义用户信息对象)
 */
public class AccountUser implements UserDetails {

    private Long userId;

    private static final long serialVersionUID = 540L;
    private static final Log logger = LogFactory.getLog(User.class);
    private String password;
    private final String account;
    private final Collection<? extends GrantedAuthority> authorities;
    private final boolean accountNonExpired;
    private final boolean accountNonLocked;
    private final boolean credentialsNonExpired;
    private final boolean enabled;

    public AccountUser(Long userId, String account, String password,boolean enabled, boolean accountNonLocked,Collection<? extends GrantedAuthority> authorities) {
        this(userId, account, password, enabled, true, true, accountNonLocked, authorities);
    }

    /**
     * @param userId 用户ID
     * @param account 用户账号
     * @param password 用户密码
     * @param enabled 用户是否启用
     * @param accountNonExpired 账号是否未过期
     * @param credentialsNonExpired 凭证是否未过期
     * @param accountNonLocked 账号是否未锁定
     * @param authorities 用户的权限集合
     */
    public AccountUser(Long userId, String account, String password, boolean enabled, boolean accountNonExpired, boolean credentialsNonExpired, boolean accountNonLocked, Collection<? extends GrantedAuthority> authorities) {
        Assert.hasText(account, "账号不能为空");
        Assert.hasText(password, "密码不能为空");
        //若为空或空字符将抛出IllegalArgumentException异常
        //其余参数若为false会抛出框架设置的标准异常
        //Assert.isTrue(account != null && !"".equals(account) && password != null, "账户和密码不能为null,且账户不为空字符串");
        this.userId = userId;
        this.account = account;
        this.password = password;
        this.enabled = enabled;
        this.accountNonExpired = accountNonExpired;
        this.credentialsNonExpired = credentialsNonExpired;
        this.accountNonLocked = accountNonLocked;
        this.authorities = authorities;
    }

    //在用户调用接口时，scrutiny会通过UserDetails对象调用以下方法认证用户的权限和状态
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return this.authorities;
    }

    @Override
    public String getPassword() {return this.password;}

    @Override
    public String getUsername() {
        return this.account;
    }

    @Override
    public boolean isAccountNonExpired() {
        return this.accountNonExpired;
    }

    @Override
    public boolean isAccountNonLocked() {
        return this.accountNonLocked;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return this.credentialsNonExpired;
    }

    @Override
    public boolean isEnabled() {
        return this.enabled;
    }
}
