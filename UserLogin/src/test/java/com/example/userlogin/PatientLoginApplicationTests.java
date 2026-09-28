package com.example.userlogin;

import com.example.userlogin.utils.DatabaseInitializer;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class PatientLoginApplicationTests {

    static {
        try {
            DatabaseInitializer.createDatabaseIfNotExists();
            System.out.println("测试启动前已完成数据库创建/检查");
        } catch (Exception e) {
            System.err.println("建库失败：" + e.getMessage());
            e.printStackTrace();
        }
    }

    @Test
    void contextLoads() {
    }

}
