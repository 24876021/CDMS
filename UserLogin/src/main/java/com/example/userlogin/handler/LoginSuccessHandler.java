package com.example.userlogin.handler;

import cn.hutool.json.JSONUtil;
import com.example.userlogin.model.Result;
import com.example.userlogin.model.SysUser;
import com.example.userlogin.service.SysUserService;
import com.example.userlogin.utils.CommonUtil;
import com.example.userlogin.utils.JwtUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * 登录成功处理器
 */
@Component
public class LoginSuccessHandler implements AuthenticationSuccessHandler {

    @Autowired
    private JwtUtils jwtUtils;
    @Autowired
    SysUserService sysUserService;

    /**
     *
     * @param authentication SpringSecurity中的接口Authentication继承了接口Principal，Principal接口表示主体的抽象概念，可用于表示任何实体，例如个人、公司和登录 ID，一般用来表示用户认证相关信息，调用其getName方法可以获得用户名
     * @throws IOException
     * @throws ServletException
     */
    @Override
    public void onAuthenticationSuccess(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Authentication authentication) throws IOException, ServletException {
        httpServletResponse.setContentType("application/json;charset=UTF-8");
        ServletOutputStream outputStream = httpServletResponse.getOutputStream();

        // 直接获取请求body中指定键的值
        String account = CommonUtil.getStringBodyParameterFromRequest(httpServletRequest,"account");

        if (account == null || account.isEmpty()) {
            throw new IllegalArgumentException("没有账户");
        }

        // 使用 getByAccount() 方法获取 userId
        SysUser sysUser = sysUserService.getByAccount(account);
        if (sysUser == null) {
            throw new IllegalArgumentException("没有找到账户: " + account);
        }
        Long userId = sysUser.getUserId();

        // 使用账户名生成JWT，并放置到响应头中
        String jwt = jwtUtils.generateToken(authentication.getName());
        httpServletResponse.setHeader(jwtUtils.getHeader(), jwt); // 响应头名，响应头数据
        System.out.println("生成的jwt: " + jwt);

        // 将 userId 和 jwt 一起传给前端
        Map<String, Object> responseData = new HashMap<>();
        responseData.put("userId", userId);
        responseData.put("jwt", jwt);


        Result result = Result.succ(responseData);

        outputStream.write(JSONUtil.toJsonStr(result).getBytes(StandardCharsets.UTF_8));
        outputStream.flush();
        outputStream.close();
    }
}
