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
            //.allowedOrigins("http://localhost:8080","http://192.168.10.56:8080")//使通过本机端口和让局域网内指定设备使用本机IP+端口能够访问
                .allowedOriginPatterns("http://*:8081","http://*:8083")//使本机和局域网内其他设备都能通过端口访问,添加nginx使用的8083端口
            .allowedHeaders("*")
            .allowCredentials(true)
            .exposedHeaders("")
            .maxAge(3600);
    }
}
