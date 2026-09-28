package com.example.userlogin.config;

import com.example.userlogin.filter.CaptchaFilter;
import com.example.userlogin.filter.CustomAuthenticationFilter;
import com.example.userlogin.filter.JwtAuthenticationFilter;
import com.example.userlogin.handler.LoginFailureHandler;
import com.example.userlogin.handler.LoginSuccessHandler;
import com.example.userlogin.handler.jwt.JWTLogoutSuccessHandler;
import com.example.userlogin.handler.jwt.JwtAccessDeniedHandler;
import com.example.userlogin.handler.jwt.JwtAuthenticationEntryPoint;
import com.example.userlogin.handler.password.PasswordEncoder;
import com.example.userlogin.user.UserDetailServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * 整合所有组件，进行Spring Security全局配置
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
@EnableGlobalMethodSecurity(prePostEnabled = true) //启用在方法前后以注解的方式进行权限检查的功能
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    @Autowired
    LoginFailureHandler loginFailureHandler;

    @Autowired
    LoginSuccessHandler loginSuccessHandler;

    @Autowired
    CaptchaFilter captchaFilter;

    @Autowired
    JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @Autowired
    JwtAccessDeniedHandler jwtAccessDeniedHandler;

    @Autowired
    UserDetailServiceImpl userDetailService;

    @Autowired
    JWTLogoutSuccessHandler jwtLogoutSuccessHandler;

    @Bean
    JwtAuthenticationFilter jwtAuthenticationFilter() throws Exception {
        JwtAuthenticationFilter jwtAuthenticationFilter = new JwtAuthenticationFilter(authenticationManager());
        return jwtAuthenticationFilter;
    }

//接口认证白名单，以下接口不需要登录即可访问
    private static final String[] URL_WHITELIST = {
      "/login",         //登录
      "/logout",        //登出
      "/captcha",        //验证码
      "/sysUser/register", //注册
      "/swagger-ui.html",
      "/webjars/**",
      "/swagger-resources/**",
      "/v2/api-docs/**",
      "/"
    };

    /**
     * 使用 Spring Security 自带的密码编译器
     */
    @Bean
    PasswordEncoder PasswordEncoder() {
        return new PasswordEncoder();
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.cors().and()
                .csrf().disable() //禁用默认的csrf保护

                // 登录配置
                .formLogin()
                //.successHandler(loginSuccessHandler) 使用自定义配置登录状态处理器，这里无需配置
                //.failureHandler(loginFailureHandler) 使用自定义配置登录状态处理器，这里无需配置

                //登出配置
                .and()             //连接配置块
                .logout()
                .logoutSuccessHandler(jwtLogoutSuccessHandler)

                // 禁用session存储用户认证信息，前端使用LocalStorage存储JWT来实现登录持久化
                .and()
                .sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)

                // 配置拦截规则
                .and()
                .authorizeRequests()
                .antMatchers(URL_WHITELIST).permitAll()//白名单中的接口不需要认证
                .anyRequest().authenticated()//所有其他请求必须通过登录认证

                // 异常处理器
                .and()
                .exceptionHandling()
                .authenticationEntryPoint(jwtAuthenticationEntryPoint)//未认证
                .accessDeniedHandler(jwtAccessDeniedHandler)//已认证无权限

                // 配置自定义的过滤器
                .and()
                //自定义的jwt过滤器
                .addFilter(jwtAuthenticationFilter())
                //自定义登录拦截 用customAuthenticationFilter 替换 UsernamePasswordAuthenticationFilter
                .addFilterAt(customAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class)
                // 验证码过滤器放在登录拦截过滤器之前(该默认过滤器已被上述代码替换
                .addFilterBefore(captchaFilter, UsernamePasswordAuthenticationFilter.class)
                ;
    }

    /**
     * 重写Spring Security获取当前用户的权限和状态的方法
     * */
    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth.userDetailsService(userDetailService);
    }

    /**
     * 登录配置，使用自定义的登录状态处理器
     */
    @Bean
    CustomAuthenticationFilter customAuthenticationFilter() throws Exception {
        CustomAuthenticationFilter filter = new CustomAuthenticationFilter();
        filter.setAuthenticationSuccessHandler(loginSuccessHandler);
        filter.setAuthenticationFailureHandler(loginFailureHandler);
        // 可自定义登录接口请求路径
        // filter.setFilterProcessesUrl("");
        filter.setAuthenticationManager(authenticationManagerBean());

        return filter;
    }
}
