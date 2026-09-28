package com.example.userlogin.handler;

import cn.hutool.json.JSONUtil;
import com.example.userlogin.model.Result;
import com.example.userlogin.model.SysUser;
import com.example.userlogin.service.SysUserService;
import com.example.userlogin.user.UserDetailServiceImpl;
import com.example.userlogin.utils.CommonUtil;
import com.example.userlogin.utils.JwtUtils;
import com.example.userlogin.utils.RedisUtil;
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
    @Autowired
    RedisUtil redisUtil;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Authentication authentication) throws IOException, ServletException {
        httpServletResponse.setContentType("application/json;charset=UTF-8");
        ServletOutputStream outputStream = httpServletResponse.getOutputStream();

        String account = CommonUtil.getStringBodyParameterFromRequest(httpServletRequest,"account");

        if (account == null || account.isEmpty()) {
            throw new IllegalArgumentException("没有账户");
        }

        // 登录成功 → 清除所有登录相关锁
        String failKey = UserDetailServiceImpl.LOGIN_FAIL_KEY + account;
        String lockKey = UserDetailServiceImpl.LOCK_KEY + account;
        redisUtil.del(failKey);
        redisUtil.del(lockKey);

        SysUser sysUser = sysUserService.getByAccount(account);
        if (sysUser == null) {
            throw new IllegalArgumentException("没有找到账户: " + account);
        }
        // 使用账户名生成JWT，并放置到响应头中
        String jwt = jwtUtils.generateToken(authentication.getName());
        httpServletResponse.setHeader(jwtUtils.getHeader(), jwt);// 响应头名，响应头数据

        // 将 userId 和 jwt 一起传给前端
        Map<String, Object> map = new HashMap<>();
        map.put("userId", sysUser.getUserId());
        map.put("jwt", jwt);

        outputStream.write(JSONUtil.toJsonStr(Result.succ(map)).getBytes(StandardCharsets.UTF_8));
        outputStream.flush();
        outputStream.close();
    }
}