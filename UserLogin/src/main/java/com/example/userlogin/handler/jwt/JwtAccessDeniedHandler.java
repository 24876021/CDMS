package com.example.userlogin.handler.jwt;

import cn.hutool.json.JSONUtil;
import com.example.userlogin.model.Result;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 用户权限不足处理器
 */
@Component
public class JwtAccessDeniedHandler implements AccessDeniedHandler {

    @Override
    public void handle(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, AccessDeniedException e) throws IOException, ServletException {
        httpServletResponse.setContentType("application/json;charset=UTF-8");
        //httpServletResponse.setStatus(HttpServletResponse.SC_FORBIDDEN);//返回403
        ServletOutputStream outputStream = httpServletResponse.getOutputStream();//获取 HttpServletResponse 对象的输出流

        //Result result = Result.fail(e.getMessage());
        Result result = Result.fail("权限不足!");

        outputStream.write(JSONUtil.toJsonStr(result).getBytes(StandardCharsets.UTF_8));
        outputStream.flush();//刷新输出流
        outputStream.close();
    }
}
