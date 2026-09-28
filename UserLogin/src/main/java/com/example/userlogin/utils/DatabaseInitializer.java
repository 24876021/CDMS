package com.example.userlogin.utils;

import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.List;

/**
 * 数据库初始化工具类（Spring Boot启动前创建业务库）
 */
public class DatabaseInitializer {
    // 配置文件路径
    private static final String CONFIG_FILE = "application.yaml";
    // 配置项
    private static String TEMP_URL;
    private static String USERNAME;
    private static String PASSWORD;
    private static String DB_NAME; // 独立的数据库名配置

    // 静态代码块加载配置
    static {
        try {
            YamlPropertySourceLoader loader = new YamlPropertySourceLoader();
            ClassPathResource resource = new ClassPathResource(CONFIG_FILE);
            List<PropertySource<?>> propertySources = loader.load("yamlConfig", resource);

            if (propertySources.isEmpty()) {
                throw new RuntimeException("未加载到任何YAML配置");
            }

            PropertySource<?> propertySource = propertySources.get(0);
            // 读取独立的数据库名（用户直观修改的配置）
            DB_NAME = (String) propertySource.getProperty("database.init.name");
            // 读取建库用的临时连接配置
            TEMP_URL = (String) propertySource.getProperty("database.init.temp-url");
            USERNAME = (String) propertySource.getProperty("database.init.username");
            PASSWORD = (String) propertySource.getProperty("database.init.password");

            // 校验配置完整性
            if (DB_NAME == null || DB_NAME.isEmpty()
                    || TEMP_URL == null || TEMP_URL.isEmpty()
                    || USERNAME == null || USERNAME.isEmpty()
                    || PASSWORD == null || PASSWORD.isEmpty()) {
                throw new RuntimeException("数据库配置缺失！请检查application.yaml中database节点的配置");
            }
        } catch (Exception e) {
            throw new RuntimeException("加载数据库配置失败，项目无法启动", e);
        }
    }

    /**
     * 创建业务数据库（不存在则创建，存在则跳过）
     */
    public static void createDatabaseIfNotExists() {
        try (Connection conn = DriverManager.getConnection(TEMP_URL, USERNAME, PASSWORD);
             Statement stmt = conn.createStatement()) {
            // 使用独立配置的DB_NAME建库
            String createDbSql = String.format(
                    "CREATE DATABASE IF NOT EXISTS %s DEFAULT CHARACTER SET utf8mb4 DEFAULT COLLATE utf8mb4_unicode_ci",
                    DB_NAME
            );
            stmt.execute(createDbSql);
            System.out.println("业务库 " + DB_NAME + " 创建/检查完成（配置文件中database.init.name）");
        } catch (Exception e) {
            System.err.println("建库失败：" + e.getMessage());
            throw new RuntimeException("建库失败，项目无法启动", e);
        }
    }
}