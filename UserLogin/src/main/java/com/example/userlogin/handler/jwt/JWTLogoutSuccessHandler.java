package com.example.userlogin.handler.jwt;

import cn.hutool.json.JSONUtil;
import com.example.userlogin.model.Result;
import com.example.userlogin.utils.JwtUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Component;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 登出处理器
 *通过创建SecurityContextLogoutHandler对象，调用它的logout方法将之前置入SecurityContext中的用户信息进行清除
 *定义LogoutSuccessHandler接口的实现类JWTLogoutSuccessHandler，重写其onLogoutSuccess方法
 * springSecurity 管理地址：/logout
 */
@Component
public class JWTLogoutSuccessHandler implements LogoutSuccessHandler {

    @Autowired
    JwtUtils jwtUtils;

    @Override
    public void onLogoutSuccess(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Authentication authentication) throws IOException, ServletException {

        if (authentication != null) {
            new SecurityContextLogoutHandler().logout(httpServletRequest, httpServletResponse, authentication);
        }

        httpServletResponse.setContentType("application/json;charset=UTF-8");
        ServletOutputStream outputStream = httpServletResponse.getOutputStream();

        //httpServletResponse.setHeader(jwtUtils.getHeader(), "");

        Result result = Result.succ("成功登出!");

        outputStream.write(JSONUtil.toJsonStr(result).getBytes(StandardCharsets.UTF_8));
        outputStream.flush();
        outputStream.close();
    }
}
