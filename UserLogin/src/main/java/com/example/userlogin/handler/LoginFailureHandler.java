package com.example.userlogin.handler;

import cn.hutool.json.JSONUtil;
import com.example.userlogin.controller.WsPushController;
import com.example.userlogin.handler.error.CaptchaException;
import com.example.userlogin.model.Result;
import com.example.userlogin.model.SysUser;
import com.example.userlogin.service.SysUserService;
import com.example.userlogin.utils.CommonUtil;
import com.example.userlogin.utils.RedisUtil;
import com.example.userlogin.user.UserDetailServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
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
 * 登录失败处理器
 */
@Component
public class LoginFailureHandler implements AuthenticationFailureHandler {

    @Autowired
    private RedisUtil redisUtil;
    @Autowired
    private SysUserService sysUserService;
    @Autowired
    private WsPushController wsPushController;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException e)
            throws IOException, ServletException {
        response.setContentType("application/json;charset=UTF-8");
        ServletOutputStream out = response.getOutputStream();
        String account = CommonUtil.getStringBodyParameterFromRequest(request, "account");
        String msg = "登录失败";

        // 账户锁定提示，区分手动锁 / 密码错锁
        if (e instanceof LockedException) {
            String lockType = null;
            String lockKey = UserDetailServiceImpl.LOCK_KEY + account;
            Object lockObj = redisUtil.get(lockKey);

            if (lockObj != null) {
                Map<String, String> lockMap = (Map<String, String>) lockObj;
                lockType = lockMap.get("type");
            }

            if (UserDetailServiceImpl.LOCK_TYPE_MANUAL.equals(lockType)) {
                msg = "账户已被管理员锁定，30分钟后自动解锁";
            } else if (UserDetailServiceImpl.LOCK_TYPE_PWD_ERROR.equals(lockType)) {
                msg = "连续输错密码过多，账户已锁定15分钟";
            } else {
                msg = "账户已锁定，请稍后重试";
            }
        }
        // 验证码错误
        else if (e instanceof CaptchaException) {
            msg = "验证码错误";
        }
        // 密码错误
        else if (e instanceof BadCredentialsException) {
            String key = UserDetailServiceImpl.LOGIN_FAIL_KEY + account;
            int curr = redisUtil.hasKey(key) ? Integer.parseInt(redisUtil.get(key).toString()) : 0;
            int next = curr + 1;

            redisUtil.set(key, next, 600);
            int left = UserDetailServiceImpl.MAX_FAIL_COUNT - next;

            if (left > 0) {
                msg = "密码错误，还能尝试 " + left + " 次";
            } else {
                // 输错5次 → 锁定15分钟（Java 8 兼容写法）
                SysUser user = sysUserService.getByAccount(account);
                if (user != null) {
                    user.setStatus(false);
                    sysUserService.updateById(user);

                    String lockKey = UserDetailServiceImpl.LOCK_KEY + account;
                    Map<String, String> map = new HashMap<>();
                    map.put("type", UserDetailServiceImpl.LOCK_TYPE_PWD_ERROR);
                    map.put("expire", String.valueOf(System.currentTimeMillis() + UserDetailServiceImpl.LOCK_EXPIRE_SECONDS * 1000));

                    redisUtil.set(lockKey, map, UserDetailServiceImpl.LOCK_EXPIRE_SECONDS);
                    //锁定推送
                    wsPushController.pushByRole(1L, "lock:" + account);
                }
                redisUtil.del(key);
                msg = "连续输错5次密码，账户已锁定15分钟";
            }
        }
        // 账户禁用
        else if (e instanceof DisabledException) {
            msg = "账户已禁用";
        }
        // 用户名不存在
        else if (e instanceof UsernameNotFoundException) {
            msg = "用户名不存在";
        }

        out.write(JSONUtil.toJsonStr(Result.fail(msg)).getBytes(StandardCharsets.UTF_8));
        out.flush();
        out.close();
    }
}