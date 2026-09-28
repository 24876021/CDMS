package com.example.userlogin.handler;

import cn.hutool.json.JSONUtil;
import com.example.userlogin.model.Result;
import com.example.userlogin.handler.error.CaptchaException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 登录失败处理器(重定向到登录页面并发送错误信息)
 */
@Component
public class LoginFailureHandler implements AuthenticationFailureHandler {

    @Override
    public void onAuthenticationFailure(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, AuthenticationException e) throws IOException, ServletException {
        httpServletResponse.setContentType("application/json;charset=UTF-8");
        ServletOutputStream outputStream = httpServletResponse.getOutputStream();
        String errorMessage = "用户名或密码错误"; //默认错误信息
        Result result;
        if (e instanceof DisabledException) {
            errorMessage = "账户已被禁用";
            result = Result.fail(errorMessage);
        } else if (e instanceof LockedException) {
            errorMessage = "账户已被锁定";
            result = Result.fail(errorMessage);
        } else if (e instanceof CaptchaException) {
            errorMessage = "验证码错误";
            result = Result.fail(errorMessage);
            //httpServletResponse.setStatus(302);
        } else {
            result = Result.fail(errorMessage);
            //httpServletResponse.setStatus(303);
        }
        outputStream.write(JSONUtil.toJsonStr(result).getBytes(StandardCharsets.UTF_8));
        outputStream.flush();
        outputStream.close();
    }
}
