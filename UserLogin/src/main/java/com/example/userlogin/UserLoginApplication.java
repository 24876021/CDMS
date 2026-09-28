package com.example.userlogin;

import com.example.userlogin.utils.DatabaseInitializer; // 导入建库工具类
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 启动类
 * @author tian
 */
@MapperScan(basePackages = "com.example.userlogin.mapper")
@SpringBootApplication
public class UserLoginApplication {
    public static void main(String[] args) {
        // 初始化数据库（优先MySQL，失败则降级到SQLite）
        DatabaseInitializer.initDatabase();//本地测试用，手动建库的话可以将这行代码和测试类中的static代码块和DatabaseInitializer.java一起删除掉
        // 第二步：再启动 Spring Boot 应用
        SpringApplication.run(UserLoginApplication.class, args);
    }
}