package com.example.userlogin.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 跨域配置
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // addMapping表示要处理的请求地址
        registry.addMapping("/**")
            .allowedMethods("*")
            .allowedOrigins("http://localhost:8080")
            .allowedHeaders("*")
            .allowCredentials(true)
            .exposedHeaders("")
            .maxAge(3600);
    }
}
