package com.example.userlogin.filter;

import cn.hutool.core.util.StrUtil;
import com.example.userlogin.model.SysUser;
import com.example.userlogin.service.SysUserService;
import com.example.userlogin.user.UserDetailServiceImpl;
import com.example.userlogin.utils.JwtUtils;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * jwt校验器：校验前端发送的token是否正确或过期，若校验成功，则获取用户信息并存入Spring Security以便后续调用
 */

public class JwtAuthenticationFilter extends BasicAuthenticationFilter {

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private UserDetailServiceImpl userDetailService;

    @Autowired
    private SysUserService sysUserService;

    public JwtAuthenticationFilter(AuthenticationManager authenticationManager) {
        super(authenticationManager);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws IOException, ServletException {
        String jwt = request.getHeader(jwtUtils.getHeader());
        System.out.println("接收的jwt:"+jwt);
        // 这里如果没有jwt，继续往后走，因为后面还有鉴权管理器等去判断是否拥有身份凭证，所以是可以放行的
        // 没有jwt相当于匿名访问，若有一些接口是需要权限的，则不能访问这些接口
        if (StrUtil.isBlankOrUndefined(jwt)) {
            chain.doFilter(request, response);
            return;
        }

        Claims claim = jwtUtils.getClaimsByToken(jwt);
        if (claim == null) {
            throw new JwtException("token 异常");
        }
        if (jwtUtils.isTokenExpired(claim)) {
            throw new JwtException("token 已过期");
        }

        String account = claim.getSubject();//获取jwt中的账户名

        // 查询account字段获取数据库中用户的所有信息(就是记录
        SysUser sysUser = sysUserService.getByAccount(account);//可能抛出异常


        //抛出任何异常都会中断当前方法的执行流程，后续的代码不会继续执行
        // 使用账户、密码、权限字段构建一个认证令牌(它是一个已认证的Authentication对象)，用于保存用户信息;这里密码为null，是因为提供了正确的JWT,实现自动登录
        UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(account, null, userDetailService.getUserAuthority(sysUser.getUserId()));

        //将该令牌交给SecurityContextHolder，set进它的context中，这样后续就能通过调用SecurityContextHolder.getContext().getAuthentication()的方法获取到当前登录的用户的各种信息
        SecurityContextHolder.getContext().setAuthentication(token);

        chain.doFilter(request, response);

    }
}
