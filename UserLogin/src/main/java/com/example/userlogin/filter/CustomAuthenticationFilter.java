package com.example.userlogin.filter;

import com.example.userlogin.utils.CommonUtil;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

/**
 * 用户首次登录校验器(登录拦截
 */

public class CustomAuthenticationFilter  extends UsernamePasswordAuthenticationFilter  {

    private static final String APPLICATION_JSON_UTF8_VALUE_WITH_SPACE = "application/json; charset=UTF-8";
    private static final String ACCOUNT = "account";
    private static final String PASSWORD = "password";

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) throws AuthenticationException {
        // 通过 ContentType 判断是否是 JSON 登录
        String contentType = request.getContentType();
        if (MediaType.APPLICATION_JSON_VALUE.equalsIgnoreCase(contentType) ||
                APPLICATION_JSON_UTF8_VALUE_WITH_SPACE.equalsIgnoreCase(contentType)) {

            // 获取 body 参数 Map
            Map<String, Object> obj = CommonUtil.getBodyParametersFromRequest(request);

            System.out.println("登录数据: " + obj);

            // 使用账户和密码构建 Token
            UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(
                    obj.get(ACCOUNT), obj.get(PASSWORD));
            setDetails(request, token); // 将请求的详细信息传入到 UsernamePasswordAuthenticationToken 对象中
                return this.getAuthenticationManager().authenticate(token);//认证方法由Spring Security实现
                //抛出 AuthenticationException 异常，则表示认证失败
        } else {
            return super.attemptAuthentication(request, response);
        }
    }
}
