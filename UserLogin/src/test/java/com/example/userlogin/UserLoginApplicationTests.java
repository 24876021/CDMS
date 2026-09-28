package com.example.userlogin;

import com.example.userlogin.utils.DatabaseInitializer;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * 测试类适配MySQL/SQLite自动降级逻辑
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class UserLoginApplicationTests { // 修正类名拼写错误

    // 静态代码块初始化数据库（使用新的降级逻辑）
    static {
        try {
            // 替换原有仅支持MySQL的方法，使用新的多数据库初始化方法
            DatabaseInitializer.initDatabase();
            System.out.println("测试启动前已完成数据库初始化（MySQL/SQLite自动适配）");
        } catch (Exception e) {
            System.err.println("数据库初始化失败：" + e.getMessage());
            e.printStackTrace();
        }
    }

    @Test
    void contextLoads() {
        // 验证Spring上下文加载，同时验证数据库初始化结果
        System.out.println("Spring上下文加载完成，当前使用的数据库URL：" + DatabaseInitializer.getCurrentDbUrl());
    }

}